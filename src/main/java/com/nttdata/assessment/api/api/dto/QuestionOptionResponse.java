package com.nttdata.assessment.api.api.dto;

/**
 * Choice option.
 */
public record QuestionOptionResponse(String id, String label, boolean isCorrect) {
}
