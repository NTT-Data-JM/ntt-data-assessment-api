package com.nttdata.assessment.api.infrastructure.webclient;

import com.nttdata.assessment.api.infrastructure.config.AssessmentProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Outbound client to ntt-data-assessment-ollama-api.
 */
@Component
public class OllamaGradingWebClient {

    private final WebClient webClient;
    private final AssessmentProperties properties;

    /**
     * @param ollamaApiWebClient client
     * @param properties settings
     */
    public OllamaGradingWebClient(WebClient ollamaApiWebClient, AssessmentProperties properties) {
        this.webClient = ollamaApiWebClient;
        this.properties = properties;
    }

    /**
     * @param request grade request
     * @return grade response
     */
    public Mono<OllamaGradeTextResponse> gradeText(OllamaGradeTextRequest request) {
        if (!properties.getOllama().isEnabled()) {
            return Mono.error(new IllegalStateException("Ollama grading is disabled"));
        }
        return webClient.post()
                .uri(properties.getOllama().getGradePath())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OllamaGradeTextResponse.class)
                .onErrorMap(ex -> new IllegalStateException("Ollama grading service is unavailable", ex));
    }
}
