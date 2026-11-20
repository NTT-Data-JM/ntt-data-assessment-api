package com.nttdata.assessment.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Assessment catalog / quiz / history WebFlux API.
 */
@SpringBootApplication
public class AssessmentApiApplication {

    /**
     * @param args CLI args
     */
    public static void main(String[] args) {
        SpringApplication.run(AssessmentApiApplication.class, args);
    }
}
