package com.nttdata.assessment.api.api.controller;

import com.nttdata.assessment.api.api.contract.HistoryApi;
import com.nttdata.assessment.api.api.dto.AttemptResponse;
import com.nttdata.assessment.api.mapper.AssessmentMapper;
import com.nttdata.assessment.api.service.HistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * History endpoints.
 */
@RestController
public class HistoryController implements HistoryApi {

    private final HistoryService historyService;
    private final AssessmentMapper mapper;

    /**
     * @param historyService service
     * @param mapper mapper
     */
    public HistoryController(HistoryService historyService, AssessmentMapper mapper) {
        this.historyService = historyService;
        this.mapper = mapper;
    }

    @Override
    public Mono<ResponseEntity<List<AttemptResponse>>> listAttempts(String status, String roleBand) {
        return historyService.list(status, roleBand)
                .map(list -> list.stream().map(mapper::toAttempt).toList())
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<AttemptResponse>> historyAttempt(String attemptId) {
        return historyService.one(attemptId).map(mapper::toAttempt).map(ResponseEntity::ok);
    }
}
