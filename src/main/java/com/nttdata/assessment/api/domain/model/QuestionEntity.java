package com.nttdata.assessment.api.domain.model;

import java.util.List;

/**
 * questions row with options/keywords/bands.
 */
public record QuestionEntity(
        String id,
        String topicId,
        String type,
        String prompt,
        String knowledgeDepth,
        boolean active,
        String referenceAnswer,
        String explanation,
        List<String> roleBands,
        List<OptionEntity> options,
        List<String> keywords
) {
    /**
     * Choice option.
     */
    public record OptionEntity(String id, String label, boolean correct) {
    }
}
