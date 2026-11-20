package com.nttdata.assessment.api.api.dto;

import java.util.Map;

/**
 * Topic with relevance map.
 */
public record TopicResponse(
        String id,
        String profileId,
        String categoryId,
        String name,
        String roleBand,
        Map<String, String> relevanceByLevel,
        String baselineLevel,
        Integer currentScore,
        String currentLevel,
        boolean priorityToLevel,
        String baselineNotes,
        String generatedNotes
) {
}
