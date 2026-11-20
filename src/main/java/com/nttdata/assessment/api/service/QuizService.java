package com.nttdata.assessment.api.service;

import com.nttdata.assessment.api.api.dto.GenerationPreviewResponse;
import com.nttdata.assessment.api.api.dto.QuizSetupRequest;
import com.nttdata.assessment.api.api.dto.SaveAnswerRequest;
import com.nttdata.assessment.api.api.dto.SubmitAttemptRequest;
import com.nttdata.assessment.api.domain.model.QuestionEntity;
import com.nttdata.assessment.api.domain.model.QuizAttemptEntity;
import com.nttdata.assessment.api.domain.model.TopicEntity;
import com.nttdata.assessment.api.domain.repository.AssessmentRepository;
import com.nttdata.assessment.api.service.validation.QuizValidation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Quiz preview/create/answer/submit use cases.
 */
@Service
public class QuizService {

    public static final int QUESTIONS_PER_TOPIC = 5;
    public static final int AUTO_TOPIC_COUNT = 5;

    private final AssessmentRepository repository;
    private final ScoringService scoringService;

    /**
     * @param repository repo
     * @param scoringService scoring
     */
    public QuizService(AssessmentRepository repository, ScoringService scoringService) {
        this.repository = repository;
        this.scoringService = scoringService;
    }

    /**
     * @param setup setup
     * @return preview
     */
    public Mono<GenerationPreviewResponse> preview(QuizSetupRequest setup) {
        return Mono.fromCallable(() -> buildPreview(setup)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * @param setup setup
     * @return created attempt
     */
    public Mono<QuizAttemptEntity> createAttempt(QuizSetupRequest setup) {
        return Mono.fromCallable(() -> {
            QuizValidation.validateSetup(setup);
            GenerationPreviewResponse preview = buildPreview(setup);
            if (!preview.valid()) {
                throw new IllegalArgumentException(String.join(" ", preview.warnings()));
            }
            QuizAttemptEntity attempt = new QuizAttemptEntity(
                    "attempt-" + UUID.randomUUID(),
                    setup.profileId(),
                    Instant.now().toString(),
                    null,
                    "IN_PROGRESS",
                    setup,
                    preview.topicIds(),
                    preview.questionIds(),
                    preview.substitutionsNote(),
                    new HashMap<>(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            repository.saveAttempt(attempt);
            return attempt;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * @param attemptId id
     * @return attempt
     */
    public Mono<QuizAttemptEntity> getAttempt(String attemptId) {
        return Mono.fromCallable(() -> repository.require(repository.findAttempt(attemptId), attemptId))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * @param attemptId id
     * @param questionId question
     * @param answer answer
     * @return updated attempt
     */
    public Mono<QuizAttemptEntity> saveAnswer(String attemptId, String questionId, SaveAnswerRequest answer) {
        return Mono.fromCallable(() -> {
            QuizAttemptEntity attempt = repository.require(repository.findAttempt(attemptId), attemptId);
            if ("COMPLETED".equals(attempt.status())) {
                throw new IllegalArgumentException("El intento ya fue enviado");
            }
            if (!attempt.questionIds().contains(questionId)) {
                throw new IllegalArgumentException("Question not part of attempt");
            }
            Map<String, SaveAnswerRequest> answers = new HashMap<>(attempt.answers());
            answers.put(questionId, answer);
            QuizAttemptEntity updated = copy(attempt, answers, attempt.questionScores(), attempt.topicScores(),
                    attempt.generalScore(), attempt.generalLevel(), attempt.topicNotes(), attempt.overallNotes(),
                    attempt.status(), attempt.completedAt());
            repository.saveAttempt(updated);
            return updated;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * @param attemptId id
     * @param request submit flags
     * @return completed attempt
     */
    public Mono<QuizAttemptEntity> submit(String attemptId, SubmitAttemptRequest request) {
        boolean allowIncomplete = request == null || request.allowIncomplete() == null || request.allowIncomplete();
        boolean useAi = request != null && Boolean.TRUE.equals(request.useAiTextGrading());
        return Mono.fromCallable(() -> repository.require(repository.findAttempt(attemptId), attemptId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(attempt -> {
                    if ("COMPLETED".equals(attempt.status())) {
                        return Mono.just(attempt);
                    }
                    List<String> unanswered = attempt.questionIds().stream()
                            .filter(id -> !attempt.answers().containsKey(id))
                            .toList();
                    if (!unanswered.isEmpty() && !allowIncomplete) {
                        return Mono.error(new IllegalArgumentException("Quedan preguntas sin responder"));
                    }
                    return scoreAttempt(attempt, useAi);
                });
    }

    private Mono<QuizAttemptEntity> scoreAttempt(QuizAttemptEntity attempt, boolean useAi) {
        return Mono.fromCallable(repository::listActiveQuestions)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(allQuestions -> {
                    Map<String, QuestionEntity> byId = new HashMap<>();
                    for (QuestionEntity q : allQuestions) {
                        byId.put(q.id(), q);
                    }
                    Map<String, Integer> qScores = new HashMap<>();
                    List<Mono<Void>> textJobs = new ArrayList<>();
                    for (String qid : attempt.questionIds()) {
                        QuestionEntity question = byId.get(qid);
                        SaveAnswerRequest answer = attempt.answers().get(qid);
                        if (question == null) {
                            qScores.put(qid, 0);
                            continue;
                        }
                        if ("TEXT".equals(question.type()) && useAi) {
                            textJobs.add(scoringService.scoreText(question, answer, true)
                                    .doOnNext(score -> qScores.put(qid, score))
                                    .then());
                        } else {
                            qScores.put(qid, scoringService.scoreDeterministic(question, answer));
                        }
                    }
                    Mono<Void> wait = textJobs.isEmpty() ? Mono.empty() : Mono.when(textJobs);
                    return wait.then(Mono.fromCallable(() -> finalizeScores(attempt, qScores, byId))
                            .subscribeOn(Schedulers.boundedElastic()));
                });
    }

    private QuizAttemptEntity finalizeScores(
            QuizAttemptEntity attempt,
            Map<String, Integer> qScores,
            Map<String, QuestionEntity> byId
    ) {
        Map<String, List<Integer>> byTopic = new HashMap<>();
        for (String qid : attempt.questionIds()) {
            QuestionEntity q = byId.get(qid);
            if (q == null) {
                continue;
            }
            byTopic.computeIfAbsent(q.topicId(), key -> new ArrayList<>()).add(qScores.getOrDefault(qid, 0));
        }
        Map<String, Integer> topicScores = new HashMap<>();
        Map<String, String> topicNotes = new HashMap<>();
        for (Map.Entry<String, List<Integer>> entry : byTopic.entrySet()) {
            int avg = (int) Math.round(entry.getValue().stream().mapToInt(Integer::intValue).average().orElse(0));
            topicScores.put(entry.getKey(), avg);
            String level = LevelRules.fromScore(avg);
            String notes = "Score " + avg + " → " + level;
            topicNotes.put(entry.getKey(), notes);
            repository.updateTopicProgress(entry.getKey(), avg, level, notes);
        }
        int general = (int) Math.round(qScores.values().stream().mapToInt(Integer::intValue).average().orElse(0));
        String generalLevel = LevelRules.fromScore(general);
        String overall = "Foco sugerido: " + topicScores.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(3)
                .map(e -> e.getKey() + " (" + e.getValue() + ")")
                .reduce((a, b) -> a + ", " + b)
                .orElse("n/a");
        QuizAttemptEntity completed = copy(
                attempt,
                attempt.answers(),
                qScores,
                topicScores,
                general,
                generalLevel,
                topicNotes,
                overall,
                "COMPLETED",
                Instant.now().toString()
        );
        repository.saveAttempt(completed);
        return completed;
    }

    private GenerationPreviewResponse buildPreview(QuizSetupRequest setup) {
        QuizValidation.validateSetup(setup);
        List<TopicEntity> topics = repository.listTopics(CatalogService.PROFILE_ID);
        List<QuestionEntity> questions = repository.listActiveQuestions();
        List<String> topicIds = setup.selectedTopicIds() != null && !setup.selectedTopicIds().isEmpty()
                ? new ArrayList<>(setup.selectedTopicIds())
                : autoSelectTopics(setup, topics, questions);

        List<String> warnings = new ArrayList<>();
        List<String> questionIds = new ArrayList<>();
        List<String> substituted = new ArrayList<>();

        if (topicIds.isEmpty()) {
            return new GenerationPreviewResponse(List.of(), List.of(), QUESTIONS_PER_TOPIC, 0,
                    List.of("No hay temas con inventario suficiente para generar el quiz."), null, false);
        }

        for (String topicId : topicIds) {
            TopicEntity topic = topics.stream().filter(t -> t.id().equals(topicId)).findFirst().orElse(null);
            if (topic == null) {
                warnings.add("Tema no encontrado: " + topicId);
                continue;
            }
            PickResult pick = pickQuestionsForTopic(topic, setup, questions);
            if (pick.ids().size() < QUESTIONS_PER_TOPIC) {
                warnings.add(topic.name() + ": solo " + pick.ids().size() + "/" + QUESTIONS_PER_TOPIC
                        + " preguntas disponibles para el modo/nivel elegido.");
            }
            if (pick.substituted()) {
                substituted.add(topic.name());
            }
            questionIds.addAll(pick.ids());
        }

        String note = substituted.isEmpty()
                ? null
                : "Se usaron preguntas cercanas (fallback) en: " + String.join(", ", substituted) + ".";
        int expected = topicIds.size() * QUESTIONS_PER_TOPIC;
        boolean valid = questionIds.size() == expected;
        if (!valid && questionIds.size() < expected) {
            warnings.add("No se puede iniciar hasta completar 5 preguntas por tema.");
        }
        return new GenerationPreviewResponse(
                topicIds, questionIds, QUESTIONS_PER_TOPIC, questionIds.size(), warnings, note, valid);
    }

    private List<String> autoSelectTopics(
            QuizSetupRequest setup,
            List<TopicEntity> topics,
            List<QuestionEntity> questions
    ) {
        List<TopicEntity> sorted = topics.stream()
                .sorted((a, b) -> {
                    int belowA = LevelRules.order(a.currentLevel()) < 3 ? 0 : 1;
                    int belowB = LevelRules.order(b.currentLevel()) < 3 ? 0 : 1;
                    if (belowA != belowB) {
                        return Integer.compare(belowA, belowB);
                    }
                    if (a.priorityToLevel() != b.priorityToLevel()) {
                        return a.priorityToLevel() ? -1 : 1;
                    }
                    int coreA = a.relevanceByLevel().containsValue("CORE") ? 0 : 1;
                    int coreB = b.relevanceByLevel().containsValue("CORE") ? 0 : 1;
                    if (coreA != coreB) {
                        return Integer.compare(coreA, coreB);
                    }
                    return Integer.compare(a.currentScore() == null ? 0 : a.currentScore(),
                            b.currentScore() == null ? 0 : b.currentScore());
                })
                .toList();
        List<String> ids = new ArrayList<>();
        for (TopicEntity topic : sorted) {
            if (pickQuestionsForTopic(topic, setup, questions).ids().size() >= QUESTIONS_PER_TOPIC) {
                ids.add(topic.id());
            }
            if (ids.size() >= AUTO_TOPIC_COUNT) {
                break;
            }
        }
        return ids;
    }

    private PickResult pickQuestionsForTopic(
            TopicEntity topic,
            QuizSetupRequest setup,
            List<QuestionEntity> all
    ) {
        List<QuestionEntity> pool = all.stream()
                .filter(q -> q.topicId().equals(topic.id()))
                .filter(q -> matchesMode(q, setup.questionMode()))
                .toList();
        List<String> depths = desiredDepths(setup, topic.currentLevel());
        List<QuestionEntity> preferred = pool.stream()
                .filter(q -> q.roleBands().contains(setup.roleBand()) && depths.contains(q.knowledgeDepth()))
                .toList();
        List<QuestionEntity> bandOnly = pool.stream()
                .filter(q -> q.roleBands().contains(setup.roleBand()))
                .toList();
        List<QuestionEntity> depthOnly = pool.stream()
                .filter(q -> depths.contains(q.knowledgeDepth()))
                .toList();

        List<QuestionEntity> selected = new ArrayList<>();
        Set<String> used = new HashSet<>();
        take(selected, used, preferred);
        int beforeFallback = selected.size();
        take(selected, used, bandOnly);
        take(selected, used, depthOnly);
        take(selected, used, pool);
        List<String> ids = selected.stream().limit(QUESTIONS_PER_TOPIC).map(QuestionEntity::id).toList();
        boolean substituted = selected.size() > beforeFallback && beforeFallback < QUESTIONS_PER_TOPIC;
        return new PickResult(ids, substituted);
    }

    private void take(List<QuestionEntity> selected, Set<String> used, List<QuestionEntity> source) {
        for (QuestionEntity q : source) {
            if (selected.size() >= QUESTIONS_PER_TOPIC) {
                break;
            }
            if (used.add(q.id())) {
                selected.add(q);
            }
        }
    }

    private boolean matchesMode(QuestionEntity q, String mode) {
        if ("BOTH".equals(mode)) {
            return true;
        }
        if ("TEXT".equals(mode)) {
            return "TEXT".equals(q.type());
        }
        return "SINGLE_CHOICE".equals(q.type()) || "MULTIPLE_CHOICE".equals(q.type());
    }

    private List<String> desiredDepths(QuizSetupRequest setup, String topicLevel) {
        if ("EXACT".equals(setup.depthFocus().kind())) {
            return setup.depthFocus().depths().stream().filter(d -> !"N_A".equals(d)).toList();
        }
        int current = LevelRules.order(topicLevel);
        int target = LevelRules.order("COMPETENTE");
        return List.of("NOVATO", "PRINCIPIANTE_AVANZADO", "COMPETENTE", "PROFICIENTE", "MASTER").stream()
                .filter(d -> {
                    int o = LevelRules.order(d);
                    return o >= Math.max(1, current) && o <= Math.max(target, current + 1);
                })
                .toList();
    }

    private QuizAttemptEntity copy(
            QuizAttemptEntity base,
            Map<String, SaveAnswerRequest> answers,
            Map<String, Integer> questionScores,
            Map<String, Integer> topicScores,
            Integer generalScore,
            String generalLevel,
            Map<String, String> topicNotes,
            String overallNotes,
            String status,
            String completedAt
    ) {
        return new QuizAttemptEntity(
                base.id(),
                base.profileId(),
                base.createdAt(),
                completedAt,
                status,
                base.setup(),
                base.resolvedTopicIds(),
                base.questionIds(),
                base.substitutionsNote(),
                answers,
                questionScores,
                topicScores,
                generalScore,
                generalLevel,
                topicNotes,
                overallNotes
        );
    }

    private record PickResult(List<String> ids, boolean substituted) {
    }
}
