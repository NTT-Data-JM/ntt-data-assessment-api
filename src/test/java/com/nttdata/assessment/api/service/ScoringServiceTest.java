package com.nttdata.assessment.api.service;

import com.nttdata.assessment.api.api.dto.SaveAnswerRequest;
import com.nttdata.assessment.api.domain.model.QuestionEntity;
import com.nttdata.assessment.api.infrastructure.webclient.OllamaGradingWebClient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ScoringServiceTest {

    @Test
    void scoresSingleChoice() {
        ScoringService service = new ScoringService(mock(OllamaGradingWebClient.class));
        QuestionEntity question = new QuestionEntity(
                "q1", "solid", "SINGLE_CHOICE", "prompt", "NOVATO", true, null, "expl",
                List.of("JUNIOR"),
                List.of(new QuestionEntity.OptionEntity("q1:a", "A", true),
                        new QuestionEntity.OptionEntity("q1:b", "B", false)),
                List.of()
        );
        assertEquals(100, service.scoreDeterministic(question, new SaveAnswerRequest("SINGLE_CHOICE", "q1:a", null, null)));
        assertEquals(0, service.scoreDeterministic(question, new SaveAnswerRequest("SINGLE_CHOICE", "q1:b", null, null)));
    }

    @Test
    void scoresKeywords() {
        ScoringService service = new ScoringService(mock(OllamaGradingWebClient.class));
        QuestionEntity question = new QuestionEntity(
                "q2", "solid", "TEXT", "prompt", "NOVATO", true, "ref", "expl",
                List.of("JUNIOR"), List.of(), List.of("solid", "srp")
        );
        int score = service.scoreDeterministic(question, new SaveAnswerRequest("TEXT", null, null, "SOLID and SRP matter"));
        assertEquals(100, score);
    }
}
