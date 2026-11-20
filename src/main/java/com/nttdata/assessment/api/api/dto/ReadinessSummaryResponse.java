package com.nttdata.assessment.api.api.dto;

/**
 * Readiness counters for priority topics.
 */
public record ReadinessSummaryResponse(
        int priorityTopicCount,
        int priorityAtOrAboveCompetente,
        double percentPriorityCompetente,
        int topicsBelowCompetente
) {
}
