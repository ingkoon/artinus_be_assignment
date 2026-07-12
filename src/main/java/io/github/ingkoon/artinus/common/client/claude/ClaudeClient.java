package io.github.ingkoon.artinus.common.client.claude;

import io.github.ingkoon.artinus.common.client.claude.request.ClaudeMessageRequest;
import io.github.ingkoon.artinus.common.client.claude.response.ClaudeMessageResponse;
import io.github.ingkoon.artinus.common.client.csrng.exception.ExternalApiUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Anthropic Messages API 클라이언트. 이력 요약(자연어 생성) 전용.
 * 통신 장애/HTTP 오류는 ExternalApiUnavailableException으로 변환하여,
 * 서비스 계층에서 graceful degradation(요약 없이 목록 반환) 하도록 한다.
 */
@Component
@Slf4j
public class ClaudeClient {

    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient claudeRestClient;
    private final String apiKey;
    private final String model;
    private final int maxTokens;

    public ClaudeClient(@Qualifier("claudeRestClient") RestClient claudeRestClient,
                        @Value("${claude.api-key:}") String apiKey,
                        @Value("${claude.model}") String model,
                        @Value("${claude.max-tokens:1024}") int maxTokens) {
        this.claudeRestClient = claudeRestClient;
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    public String summarize(String prompt) {
        ClaudeMessageRequest request = ClaudeMessageRequest.of(model, maxTokens, prompt);
        try {
            ClaudeMessageResponse response = claudeRestClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ClaudeMessageResponse.class);

            if (response == null || response.content() == null || response.content().isEmpty()) {
                throw new ExternalApiUnavailableException();
            }

            // type == "text" 블록을 모두 이어붙인다 (첫 블록만 꺼내지 않는다)
            String text = response.content().stream()
                    .filter(block -> "text".equals(block.type()))
                    .map(ClaudeMessageResponse.ContentBlock::text)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining());

            if (text.isBlank()) {
                throw new ExternalApiUnavailableException();
            }
            return text;

        } catch (ResourceAccessException e) {
            log.warn("Claude 통신 장애", e);
            throw new ExternalApiUnavailableException(e);
        } catch (RestClientResponseException e) {
            log.warn("Claude HTTP 오류: {}", e.getStatusCode(), e);
            throw new ExternalApiUnavailableException(e);
        }
    }
}
