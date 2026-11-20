package com.nttdata.assessment.api.api.contract;

import com.nttdata.assessment.api.api.dto.AttemptResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * History contract.
 */
public interface HistoryApi {

    /**
     * @return attempts
     */
    @GetMapping("/api/v1/history/attempts")
    Mono<ResponseEntity<List<AttemptResponse>>> listAttempts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String roleBand
    );

    /**
     * @param attemptId id
     * @return attempt
     */
    @GetMapping("/api/v1/history/attempts/{attemptId}")
    Mono<ResponseEntity<AttemptResponse>> historyAttempt(@PathVariable String attemptId);
}
