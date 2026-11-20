package com.nttdata.assessment.api.api.contract;

import com.nttdata.assessment.api.api.dto.AttemptResponse;
import com.nttdata.assessment.api.api.dto.GenerationPreviewResponse;
import com.nttdata.assessment.api.api.dto.QuizSetupRequest;
import com.nttdata.assessment.api.api.dto.SaveAnswerRequest;
import com.nttdata.assessment.api.api.dto.SubmitAttemptRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;

/**
 * Quiz contract.
 */
public interface QuizApi {

    /**
     * @param request setup
     * @return preview
     */
    @PostMapping("/api/v1/quizzes/preview")
    Mono<ResponseEntity<GenerationPreviewResponse>> preview(@Valid @RequestBody QuizSetupRequest request);

    /**
     * @param request setup
     * @return created attempt
     */
    @PostMapping("/api/v1/quizzes/attempts")
    Mono<ResponseEntity<AttemptResponse>> createAttempt(@Valid @RequestBody QuizSetupRequest request);

    /**
     * @param attemptId id
     * @return attempt
     */
    @GetMapping("/api/v1/quizzes/attempts/{attemptId}")
    Mono<ResponseEntity<AttemptResponse>> getAttempt(@PathVariable String attemptId);

    /**
     * @param attemptId id
     * @param questionId question
     * @param request answer
     * @return updated attempt
     */
    @PutMapping("/api/v1/quizzes/attempts/{attemptId}/answers/{questionId}")
    Mono<ResponseEntity<AttemptResponse>> saveAnswer(
            @PathVariable String attemptId,
            @PathVariable String questionId,
            @RequestBody SaveAnswerRequest request
    );

    /**
     * @param attemptId id
     * @param request flags
     * @return completed attempt
     */
    @PostMapping("/api/v1/quizzes/attempts/{attemptId}/submit")
    Mono<ResponseEntity<AttemptResponse>> submitAttempt(
            @PathVariable String attemptId,
            @RequestBody(required = false) SubmitAttemptRequest request
    );
}
