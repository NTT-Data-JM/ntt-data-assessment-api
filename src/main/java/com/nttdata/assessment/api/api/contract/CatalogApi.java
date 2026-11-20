package com.nttdata.assessment.api.api.contract;

import com.nttdata.assessment.api.api.dto.CategoryResponse;
import com.nttdata.assessment.api.api.dto.QuestionResponse;
import com.nttdata.assessment.api.api.dto.ReadinessSummaryResponse;
import com.nttdata.assessment.api.api.dto.TopicResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Catalog contract.
 */
public interface CatalogApi {

    /**
     * @return categories
     */
    @GetMapping("/api/v1/categories")
    Mono<ResponseEntity<List<CategoryResponse>>> categories();

    /**
     * @return topics
     */
    @GetMapping("/api/v1/topics")
    Mono<ResponseEntity<List<TopicResponse>>> topics(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String roleBand,
            @RequestParam(required = false) Boolean priorityOnly,
            @RequestParam(required = false) Boolean belowTargetOnly,
            @RequestParam(required = false) Boolean coreOnly,
            @RequestParam(required = false) String search
    );

    /**
     * @param topicId id
     * @return topic
     */
    @GetMapping("/api/v1/topics/{topicId}")
    Mono<ResponseEntity<TopicResponse>> topic(@PathVariable String topicId);

    /**
     * @return questions
     */
    @GetMapping("/api/v1/questions")
    Mono<ResponseEntity<List<QuestionResponse>>> questions(
            @RequestParam(required = false) String topicId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String depth,
            @RequestParam(required = false) String roleBand,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) Boolean activeOnly
    );

    /**
     * @param questionId id
     * @return question
     */
    @GetMapping("/api/v1/questions/{questionId}")
    Mono<ResponseEntity<QuestionResponse>> question(@PathVariable String questionId);

    /**
     * @return readiness
     */
    @GetMapping("/api/v1/dashboard/readiness")
    Mono<ResponseEntity<ReadinessSummaryResponse>> readiness();
}
