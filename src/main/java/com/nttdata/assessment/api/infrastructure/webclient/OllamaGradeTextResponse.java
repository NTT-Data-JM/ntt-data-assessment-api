package com.nttdata.assessment.api.infrastructure.webclient;

import java.util.List;

/**
 * Outbound grade response DTO.
 */
public record OllamaGradeTextResponse(
        int score,
        String feedback,
        List<String> matchedKeywords,
        List<String> missingKeywords,
        String model,
        String rawModelContent
) {
}
