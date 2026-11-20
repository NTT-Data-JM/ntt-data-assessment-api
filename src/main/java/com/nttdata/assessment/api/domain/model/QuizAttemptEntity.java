package com.nttdata.assessment.api.domain.model;

import com.nttdata.assessment.api.api.dto.QuizSetupRequest;
import com.nttdata.assessment.api.api.dto.SaveAnswerRequest;

import java.util.List;
import java.util.Map;

/**
 * In-memory / persisted attempt aggregate.
 */
public record QuizAttemptEntity(
        String id,
        String profileId,
        String createdAt,
        String completedAt,
        String status,
        QuizSetupRequest setup,
        List<String> resolvedTopicIds,
        List<String> questionIds,
        String substitutionsNote,
        Map<String, SaveAnswerRequest> answers,
        Map<String, Integer> questionScores,
        Map<String, Integer> topicScores,
        Integer generalScore,
        String generalLevel,
        Map<String, String> topicNotes,
        String overallNotes
) {
}
