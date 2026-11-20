package com.nttdata.assessment.api.api.dto;

import java.util.List;

/**
 * Answer payload for one question.
 */
public record SaveAnswerRequest(
        String type,
        String optionId,
        List<String> optionIds,
        String text
) {
}
