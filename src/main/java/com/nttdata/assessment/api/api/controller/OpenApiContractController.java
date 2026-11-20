package com.nttdata.assessment.api.api.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Serves packaged OpenAPI YAML.
 */
@RestController
public class OpenApiContractController {

    /**
     * @return openapi.yaml
     */
    @GetMapping(value = "/openapi.yaml", produces = "application/yaml")
    public Mono<String> openapi() {
        return Mono.fromCallable(() -> new ClassPathResource("openapi.yaml")
                .getContentAsString(StandardCharsets.UTF_8));
    }
}
