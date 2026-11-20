package com.nttdata.assessment.api.domain.model;

import java.util.Map;

/**
 * topics row + relevances.
 */
public record TopicEntity(
        String id,
        String profileId,
        String categoryId,
        String name,
        String roleBand,
        String baselineLevel,
        Integer currentScore,
        String currentLevel,
        boolean priorityToLevel,
        String baselineNotes,
        String generatedNotes,
        Map<String, String> relevanceByLevel
) {
}
