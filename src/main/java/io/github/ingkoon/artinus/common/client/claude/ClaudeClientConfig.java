package io.github.ingkoon.artinus.common.client.claude;

import io.github.ingkoon.artinus.common.client.RestClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class ClaudeClientConfig {

    @Value("${claude.base-url:https://api.anthropic.com}")
    private String baseUrl;

    /**
     * LLM은 느리므로 read timeout을 넉넉히(30s) 준다.
     * csrng의 2s를 쓰면 정상 응답도 타임아웃난다.
     */
    @Bean
    public RestClient claudeRestClient(RestClientFactory factory) {
        return factory.create(
                baseUrl,
                Duration.ofSeconds(3),
                Duration.ofSeconds(30)
        );
    }
}
