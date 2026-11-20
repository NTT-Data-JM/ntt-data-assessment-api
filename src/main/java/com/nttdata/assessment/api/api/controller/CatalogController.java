package com.nttdata.assessment.api.api.controller;

import com.nttdata.assessment.api.api.contract.CatalogApi;
import com.nttdata.assessment.api.api.dto.CategoryResponse;
import com.nttdata.assessment.api.api.dto.QuestionResponse;
import com.nttdata.assessment.api.api.dto.ReadinessSummaryResponse;
import com.nttdata.assessment.api.api.dto.TopicResponse;
import com.nttdata.assessment.api.service.CatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Catalog endpoints.
 */
@RestController
public class CatalogController implements CatalogApi {

    private final CatalogService catalogService;

    /**
     * @param catalogService service
     */
    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Override
    public Mono<ResponseEntity<List<CategoryResponse>>> categories() {
        return catalogService.categories().map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<List<TopicResponse>>> topics(
            String categoryId,
            String roleBand,
            Boolean priorityOnly,
            Boolean belowTargetOnly,
            Boolean coreOnly,
            String search
    ) {
        return catalogService.topics(categoryId, roleBand, priorityOnly, belowTargetOnly, coreOnly, search)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TopicResponse>> topic(String topicId) {
        return catalogService.topic(topicId).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<List<QuestionResponse>>> questions(
            String topicId,
            String type,
            String depth,
            String roleBand,
            String mode,
            Boolean activeOnly
    ) {
        return catalogService.questions(topicId, type, depth, roleBand, mode, activeOnly).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<QuestionResponse>> question(String questionId) {
        return catalogService.question(questionId).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<ReadinessSummaryResponse>> readiness() {
        return catalogService.readiness().map(ResponseEntity::ok);
    }
}
