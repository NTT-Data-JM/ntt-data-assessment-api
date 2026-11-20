package com.nttdata.assessment.api.api.dto;

import java.util.List;

/**
 * Quiz generation preview.
 */
public record GenerationPreviewResponse(
        List<String> topicIds,
        List<String> questionIds,
        int questionsPerTopic,
        int totalQuestions,
        List<String> warnings,
        String substitutionsNote,
        boolean valid
) {
}
