package com.nttdata.assessment.api.api.controller;

import com.nttdata.assessment.api.api.contract.QuizApi;
import com.nttdata.assessment.api.api.dto.AttemptResponse;
import com.nttdata.assessment.api.api.dto.GenerationPreviewResponse;
import com.nttdata.assessment.api.api.dto.QuizSetupRequest;
import com.nttdata.assessment.api.api.dto.SaveAnswerRequest;
import com.nttdata.assessment.api.api.dto.SubmitAttemptRequest;
import com.nttdata.assessment.api.mapper.AssessmentMapper;
import com.nttdata.assessment.api.service.QuizService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Quiz endpoints.
 */
@RestController
public class QuizController implements QuizApi {

    private final QuizService quizService;
    private final AssessmentMapper mapper;

    /**
     * @param quizService service
     * @param mapper mapper
     */
    public QuizController(QuizService quizService, AssessmentMapper mapper) {
        this.quizService = quizService;
        this.mapper = mapper;
    }

    @Override
    public Mono<ResponseEntity<GenerationPreviewResponse>> preview(QuizSetupRequest request) {
        return quizService.preview(request).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<AttemptResponse>> createAttempt(QuizSetupRequest request) {
        return quizService.createAttempt(request)
                .map(mapper::toAttempt)
                .map(body -> ResponseEntity.status(HttpStatus.CREATED).body(body));
    }

    @Override
    public Mono<ResponseEntity<AttemptResponse>> getAttempt(String attemptId) {
        return quizService.getAttempt(attemptId).map(mapper::toAttempt).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<AttemptResponse>> saveAnswer(
            String attemptId,
            String questionId,
            SaveAnswerRequest request
    ) {
        return quizService.saveAnswer(attemptId, questionId, request).map(mapper::toAttempt).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<AttemptResponse>> submitAttempt(String attemptId, SubmitAttemptRequest request) {
        return quizService.submit(attemptId, request).map(mapper::toAttempt).map(ResponseEntity::ok);
    }
}
