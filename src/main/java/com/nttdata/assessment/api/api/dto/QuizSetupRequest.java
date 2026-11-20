package com.nttdata.assessment.api.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Quiz setup mirroring the frontend QuizSetupInput.
 */
public record QuizSetupRequest(
        @NotBlank String profileId,
        @NotBlank String roleBand,
        @NotNull DepthFocusRequest depthFocus,
        @NotBlank String questionMode,
        List<String> selectedTopicIds
) {
}
