package com.nttdata.assessment.api.api.dto;

import java.util.List;

/**
 * Baseline dashboard payload.
 */
public record BaselineResponse(
        ProfileResponse profile,
        List<ExperienceResponse> experiences,
        List<StrengthResponse> strengths,
        List<WeaknessResponse> weaknesses,
        List<TopicResponse> priorityTopics
) {
}
