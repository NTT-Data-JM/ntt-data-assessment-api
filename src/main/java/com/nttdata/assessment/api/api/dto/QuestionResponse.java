package com.nttdata.assessment.api.api.dto;

import java.util.List;

/**
 * Question bank item.
 */
public record QuestionResponse(
        String id,
        String topicId,
        String type,
        String prompt,
        String knowledgeDepth,
        List<String> roleBands,
        boolean active,
        List<QuestionOptionResponse> options,
        String referenceAnswer,
        List<String> keywords,
        String explanation
) {
}
