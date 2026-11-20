package com.nttdata.assessment.api.api.contract;

import com.nttdata.assessment.api.api.dto.BaselineResponse;
import com.nttdata.assessment.api.api.dto.ProfileResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

/**
 * Profile contract.
 */
public interface ProfileApi {

    /**
     * @return active profile
     */
    @GetMapping("/api/v1/profiles/active")
    Mono<ResponseEntity<ProfileResponse>> activeProfile();

    /**
     * @return baseline bundle
     */
    @GetMapping("/api/v1/baseline")
    Mono<ResponseEntity<BaselineResponse>> baseline();
}
