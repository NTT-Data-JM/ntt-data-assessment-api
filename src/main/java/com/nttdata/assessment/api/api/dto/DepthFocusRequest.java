package com.nttdata.assessment.api.api.dto;

import java.util.List;

/**
 * Depth focus for quiz generation.
 */
public record DepthFocusRequest(String kind, List<String> depths) {
}
