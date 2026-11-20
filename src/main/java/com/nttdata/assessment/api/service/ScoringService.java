package com.nttdata.assessment.api.service;

import com.nttdata.assessment.api.api.dto.SaveAnswerRequest;
import com.nttdata.assessment.api.domain.model.QuestionEntity;
import com.nttdata.assessment.api.infrastructure.webclient.OllamaGradeTextRequest;
import com.nttdata.assessment.api.infrastructure.webclient.OllamaGradeTextResponse;
import com.nttdata.assessment.api.infrastructure.webclient.OllamaGradingWebClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Deterministic choice scoring + keyword TEXT scoring, with optional AI override.
 */
@Service
public class ScoringService {

    private final OllamaGradingWebClient ollamaGradingWebClient;

    /**
     * @param ollamaGradingWebClient optional AI client
     */
    public ScoringService(OllamaGradingWebClient ollamaGradingWebClient) {
        this.ollamaGradingWebClient = ollamaGradingWebClient;
    }

    /**
     * @param question question
     * @param answer answer
     * @return 0..100
     */
    public int scoreDeterministic(QuestionEntity question, SaveAnswerRequest answer) {
        if (answer == null) {
            return 0;
        }
        return switch (question.type()) {
            case "SINGLE_CHOICE" -> scoreSingle(question, answer.optionId());
            case "MULTIPLE_CHOICE" -> scoreMulti(question, answer.optionIds());
            case "TEXT" -> scoreKeywords(question, answer.text());
            default -> 0;
        };
    }

    /**
     * @param question TEXT question
     * @param answer answer
     * @param useAi whether to call ollama-api
     * @return score mono
     */
    public Mono<Integer> scoreText(QuestionEntity question, SaveAnswerRequest answer, boolean useAi) {
        int keywordScore = scoreDeterministic(question, answer);
        if (!useAi || answer == null || answer.text() == null || answer.text().isBlank()) {
            return Mono.just(keywordScore);
        }
        OllamaGradeTextRequest request = new OllamaGradeTextRequest(
                question.prompt(),
                List.of(question.explanation() == null ? "" : question.explanation()),
                question.keywords(),
                answer.text(),
                null
        );
        return ollamaGradingWebClient.gradeText(request)
                .map(OllamaGradeTextResponse::score)
                .onErrorReturn(keywordScore);
    }

    private int scoreSingle(QuestionEntity question, String optionId) {
        if (optionId == null) {
            return 0;
        }
        return question.options().stream()
                .anyMatch(o -> o.correct() && (o.id().equals(optionId) || o.id().endsWith(":" + optionId)))
                ? 100 : 0;
    }

    private int scoreMulti(QuestionEntity question, List<String> optionIds) {
        Set<String> correct = new HashSet<>();
        for (QuestionEntity.OptionEntity option : question.options()) {
            if (option.correct()) {
                correct.add(option.id());
                int idx = option.id().lastIndexOf(':');
                if (idx >= 0) {
                    correct.add(option.id().substring(idx + 1));
                }
            }
        }
        Set<String> selected = optionIds == null ? Set.of() : new HashSet<>(optionIds);
        if (correct.isEmpty()) {
            return 0;
        }
        long hits = selected.stream().filter(correct::contains).count();
        long wrong = selected.stream().filter(id -> !correct.contains(id)).count();
        if (wrong > 0) {
            return 0;
        }
        return (int) Math.round(100.0 * hits / correct.size());
    }

    private int scoreKeywords(QuestionEntity question, String text) {
        if (text == null || text.isBlank() || question.keywords() == null || question.keywords().isEmpty()) {
            return 0;
        }
        String hay = text.toLowerCase(Locale.ROOT);
        long hits = question.keywords().stream()
                .filter(k -> k != null && !k.isBlank())
                .filter(k -> hay.contains(k.toLowerCase(Locale.ROOT)))
                .count();
        return (int) Math.round(100.0 * hits / question.keywords().size());
    }
}
