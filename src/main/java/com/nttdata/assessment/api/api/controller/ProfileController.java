package com.nttdata.assessment.api.api.controller;

import com.nttdata.assessment.api.api.contract.ProfileApi;
import com.nttdata.assessment.api.api.dto.BaselineResponse;
import com.nttdata.assessment.api.api.dto.ProfileResponse;
import com.nttdata.assessment.api.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Profile endpoints.
 */
@RestController
public class ProfileController implements ProfileApi {

    private final ProfileService profileService;

    /**
     * @param profileService service
     */
    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Override
    public Mono<ResponseEntity<ProfileResponse>> activeProfile() {
        return profileService.activeProfile().map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<BaselineResponse>> baseline() {
        return profileService.baseline().map(ResponseEntity::ok);
    }
}
