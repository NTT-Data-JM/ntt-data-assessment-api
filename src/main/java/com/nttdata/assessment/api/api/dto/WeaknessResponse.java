package com.nttdata.assessment.api.api.dto;

import java.util.List;

/**
 * Weakness item.
 */
public record WeaknessResponse(String id, String title, String description, List<String> relatedTopicIds) {
}
