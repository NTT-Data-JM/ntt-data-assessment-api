package com.nttdata.assessment.api.domain.model;

/**
 * assessment_profiles row.
 */
public record AssessmentProfileEntity(
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
