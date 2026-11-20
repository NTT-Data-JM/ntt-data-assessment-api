package com.nttdata.assessment.api.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Assessment API settings.
 */
@ConfigurationProperties(prefix = "assessment")
public class AssessmentProperties {

    private final Db db = new Db();
    private final Ollama ollama = new Ollama();

    public Db getDb() {
        return db;
    }

    public Ollama getOllama() {
        return ollama;
    }

    /**
     * SQLite path.
     */
    public static class Db {
        private String path = "../../database/sqlite/ntt-data-assessment-db.sqlite";

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    /**
     * Outbound ollama-api settings.
     */
    public static class Ollama {
        private boolean enabled = true;
        private String baseUrl = "http://localhost:8092";
        private String gradePath = "/api/v1/grades/text";
        private Duration connectTimeout = Duration.ofSeconds(2);
        private Duration responseTimeout = Duration.ofSeconds(60);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getGradePath() {
            return gradePath;
        }

        public void setGradePath(String gradePath) {
            this.gradePath = gradePath;
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getResponseTimeout() {
            return responseTimeout;
        }

        public void setResponseTimeout(Duration responseTimeout) {
            this.responseTimeout = responseTimeout;
        }
    }
}
