package com.nttdata.assessment.api.service;

import com.nttdata.assessment.api.domain.model.QuizAttemptEntity;
import com.nttdata.assessment.api.domain.repository.AssessmentRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Attempt history queries.
 */
@Service
public class HistoryService {

    private final AssessmentRepository repository;

    /**
     * @param repository repo
     */
    public HistoryService(AssessmentRepository repository) {
        this.repository = repository;
    }

    /**
     * @param status optional status filter
     * @param roleBand optional band filter
     * @return attempts
     */
    public Mono<java.util.List<QuizAttemptEntity>> list(String status, String roleBand) {
        return Mono.fromCallable(() -> repository.listAttempts().stream()
                        .filter(a -> status == null || status.isBlank() || status.equals(a.status()))
                        .filter(a -> roleBand == null || roleBand.isBlank()
                                || (a.setup() != null && roleBand.equals(a.setup().roleBand())))
                        .toList())
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * @param attemptId id
     * @return attempt
     */
    public Mono<QuizAttemptEntity> one(String attemptId) {
        return Mono.fromCallable(() -> repository.require(repository.findAttempt(attemptId), attemptId))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
