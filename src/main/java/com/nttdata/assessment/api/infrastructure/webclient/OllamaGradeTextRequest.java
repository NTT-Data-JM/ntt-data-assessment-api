package com.nttdata.assessment.api.infrastructure.webclient;

import java.util.List;

/**
 * Outbound grade request DTO.
 */
public record OllamaGradeTextRequest(
        String questionPrompt,
        List<String> expectedCriteria,
        List<String> keywords,
        String candidateAnswer,
        String model
) {
}
