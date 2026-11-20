package com.nttdata.assessment.api.service;

import com.nttdata.assessment.api.api.dto.CategoryResponse;
import com.nttdata.assessment.api.api.dto.QuestionResponse;
import com.nttdata.assessment.api.api.dto.ReadinessSummaryResponse;
import com.nttdata.assessment.api.api.dto.TopicResponse;
import com.nttdata.assessment.api.domain.model.QuestionEntity;
import com.nttdata.assessment.api.domain.model.TopicEntity;
import com.nttdata.assessment.api.domain.repository.AssessmentRepository;
import com.nttdata.assessment.api.mapper.AssessmentMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Locale;

/**
 * Catalog queries over SQLite.
 */
@Service
public class CatalogService {

    public static final String PROFILE_ID = "java-developer";

    private final AssessmentRepository repository;
    private final AssessmentMapper mapper;

    /**
     * @param repository JDBC repo
     * @param mapper mapper
     */
    public CatalogService(AssessmentRepository repository, AssessmentMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * @return categories
     */
    public Mono<List<CategoryResponse>> categories() {
        return Mono.fromCallable(() -> repository.listCategories(PROFILE_ID))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * @param categoryId filter
     * @param roleBand filter
     * @param priorityOnly filter
     * @param belowTargetOnly filter
     * @param coreOnly filter
     * @param search filter
     * @return topics
     */
    public Mono<List<TopicResponse>> topics(
            String categoryId,
            String roleBand,
            Boolean priorityOnly,
            Boolean belowTargetOnly,
            Boolean coreOnly,
            String search
    ) {
        return Mono.fromCallable(() -> repository.listTopics(PROFILE_ID).stream()
                        .filter(t -> categoryId == null || categoryId.isBlank() || t.categoryId().equals(categoryId))
                        .filter(t -> roleBand == null || roleBand.isBlank() || t.roleBand().equals(roleBand))
                        .filter(t -> priorityOnly == null || !priorityOnly || t.priorityToLevel())
                        .filter(t -> belowTargetOnly == null || !belowTargetOnly || LevelRules.order(t.currentLevel()) < 3)
                        .filter(t -> coreOnly == null || !coreOnly || t.relevanceByLevel().containsValue("CORE"))
                        .filter(t -> search == null || search.isBlank()
                                || t.name().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)))
                        .map(mapper::toTopic)
                        .toList())
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * @param topicId id
     * @return topic
     */
    public Mono<TopicResponse> topic(String topicId) {
        return Mono.fromCallable(() -> mapper.toTopic(repository.require(repository.findTopic(topicId), topicId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * @param topicId filter
     * @param type filter
     * @param depth filter
     * @param roleBand filter
     * @param mode filter
     * @param activeOnly filter
     * @return questions
     */
    public Mono<List<QuestionResponse>> questions(
            String topicId,
            String type,
            String depth,
            String roleBand,
            String mode,
            Boolean activeOnly
    ) {
        boolean onlyActive = !Boolean.FALSE.equals(activeOnly);
        return Mono.fromCallable(() -> repository.listQuestions(topicId, type, depth, roleBand, onlyActive).stream()
                        .filter(q -> matchesMode(q, mode))
                        .map(mapper::toQuestion)
                        .toList())
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * @param questionId id
     * @return question
     */
    public Mono<QuestionResponse> question(String questionId) {
        return Mono.fromCallable(() -> mapper.toQuestion(repository.require(repository.findQuestion(questionId), questionId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * @return readiness summary
     */
    public Mono<ReadinessSummaryResponse> readiness() {
        return Mono.fromCallable(() -> {
            List<TopicEntity> topics = repository.listTopics(PROFILE_ID);
            List<TopicEntity> priority = topics.stream().filter(TopicEntity::priorityToLevel).toList();
            int at = (int) priority.stream().filter(t -> LevelRules.order(t.currentLevel()) >= 3).count();
            int below = (int) topics.stream().filter(t -> LevelRules.order(t.currentLevel()) < 3).count();
            double pct = priority.isEmpty() ? 0.0 : (100.0 * at / priority.size());
            return new ReadinessSummaryResponse(priority.size(), at, pct, below);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private boolean matchesMode(QuestionEntity q, String mode) {
        if (mode == null || mode.isBlank() || "BOTH".equals(mode)) {
            return true;
        }
        if ("TEXT".equals(mode)) {
            return "TEXT".equals(q.type());
        }
        if ("CHOICE".equals(mode)) {
            return "SINGLE_CHOICE".equals(q.type()) || "MULTIPLE_CHOICE".equals(q.type());
        }
        return true;
    }
}
