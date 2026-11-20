package com.nttdata.assessment.api.api.dto;

/**
 * Active assessment profile.
 */
public record ProfileResponse(
        String id,
        String name,
        String roleFamily,
        String technology,
        String candidateName,
        String evaluationDate,
        String baselineSeniority,
        String targetKnowledge
) {
}
