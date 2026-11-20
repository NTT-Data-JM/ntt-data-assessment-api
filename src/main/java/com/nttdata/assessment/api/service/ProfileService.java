package com.nttdata.assessment.api.service;

import com.nttdata.assessment.api.api.dto.BaselineResponse;
import com.nttdata.assessment.api.api.dto.ProfileResponse;
import com.nttdata.assessment.api.api.dto.TopicResponse;
import com.nttdata.assessment.api.domain.repository.AssessmentRepository;
import com.nttdata.assessment.api.mapper.AssessmentMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * Profile and baseline use cases.
 */
@Service
public class ProfileService {

    private final AssessmentRepository repository;
    private final AssessmentMapper mapper;

    /**
     * @param repository repo
     * @param mapper mapper
     */
    public ProfileService(AssessmentRepository repository, AssessmentMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * @return active profile
     */
    public Mono<ProfileResponse> activeProfile() {
        return Mono.fromCallable(() -> mapper.toProfile(
                        repository.require(repository.findProfile(CatalogService.PROFILE_ID), CatalogService.PROFILE_ID)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * @return baseline bundle
     */
    public Mono<BaselineResponse> baseline() {
        return Mono.fromCallable(() -> {
            var profile = mapper.toProfile(
                    repository.require(repository.findProfile(CatalogService.PROFILE_ID), CatalogService.PROFILE_ID));
            List<TopicResponse> priority = repository.listTopics(CatalogService.PROFILE_ID).stream()
                    .filter(t -> t.priorityToLevel())
                    .map(mapper::toTopic)
                    .toList();
            return new BaselineResponse(
                    profile,
                    repository.listExperiences(CatalogService.PROFILE_ID),
                    repository.listStrengths(CatalogService.PROFILE_ID),
                    repository.listWeaknesses(CatalogService.PROFILE_ID),
                    priority
            );
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
