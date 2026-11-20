package com.nttdata.assessment.api.api.dto;

/**
 * Submit flags.
 */
public record SubmitAttemptRequest(Boolean allowIncomplete, Boolean useAiTextGrading) {
}
