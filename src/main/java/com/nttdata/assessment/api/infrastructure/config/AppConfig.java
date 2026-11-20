package com.nttdata.assessment.api.infrastructure.config;

import com.zaxxer.hikari.HikariDataSource;
import io.netty.channel.ChannelOption;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.List;

/**
 * Shared beans: CORS, WebClient, SQLite DataSource, properties.
 */
@Configuration
@EnableConfigurationProperties(AssessmentProperties.class)
public class AppConfig {

    /**
     * @return CORS filter for Vite demo
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // Vite may bind 5173, 5174, … when the default port is taken
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("*"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsWebFilter(source);
    }

    /**
     * Resolves relative SQLite paths against the process working directory.
     *
     * @param properties settings
     * @return SQLite datasource
     */
    @Bean
    public DataSource dataSource(AssessmentProperties properties) {
        Path path = Path.of(properties.getDb().getPath());
        if (!path.isAbsolute()) {
            path = Path.of("").toAbsolutePath().resolve(path).normalize();
        }
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:sqlite:" + path + "?busy_timeout=10000");
        dataSource.setDriverClassName("org.sqlite.JDBC");
        // Concurrent GETs from the SPA need more than 1; WAL allows multiple readers.
        dataSource.setMaximumPoolSize(5);
        dataSource.setMinimumIdle(1);
        dataSource.setConnectionTimeout(15_000);
        dataSource.setConnectionInitSql("PRAGMA journal_mode=WAL; PRAGMA busy_timeout=10000;");
        return dataSource;
    }

    /**
     * @param builder builder
     * @param properties settings
     * @return ollama-api client
     */
    @Bean
    public WebClient ollamaApiWebClient(WebClient.Builder builder, AssessmentProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) properties.getOllama().getConnectTimeout().toMillis())
                .responseTimeout(properties.getOllama().getResponseTimeout());
        return builder
                .baseUrl(properties.getOllama().getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
