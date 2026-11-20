package com.nttdata.assessment.api.service.validation;

import com.nttdata.assessment.api.api.dto.QuizSetupRequest;

import java.util.Set;

/**
 * Quiz setup guards.
 */
public final class QuizValidation {

    private static final Set<String> MODES = Set.of("CHOICE", "TEXT", "BOTH");
    private static final Set<String> BANDS = Set.of("JUNIOR", "MID", "SENIOR", "LEADER");

    private QuizValidation() {
    }

    /**
     * @param request setup
     */
    public static void validateSetup(QuizSetupRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Quiz setup is required");
        }
        if (!BANDS.contains(request.roleBand())) {
            throw new IllegalArgumentException("Invalid roleBand");
        }
        if (!MODES.contains(request.questionMode())) {
            throw new IllegalArgumentException("Invalid questionMode");
        }
        if (request.depthFocus() == null || request.depthFocus().kind() == null) {
            throw new IllegalArgumentException("depthFocus is required");
        }
        if (!"EXACT".equals(request.depthFocus().kind()) && !"RECOMMENDED".equals(request.depthFocus().kind())) {
            throw new IllegalArgumentException("depthFocus.kind must be EXACT or RECOMMENDED");
        }
        if ("EXACT".equals(request.depthFocus().kind())
                && (request.depthFocus().depths() == null || request.depthFocus().depths().isEmpty())) {
            throw new IllegalArgumentException("EXACT depthFocus requires depths");
        }
    }
}
