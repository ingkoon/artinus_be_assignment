package io.github.ingkoon.artinus.common.client.claude.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Anthropic Messages API 요청 바디.
 * thinking=disabled 로 두어 단순 요약을 빠르고 깔끔하게(텍스트 블록만) 받는다.
 */
public record ClaudeMessageRequest(
        String model,
        @JsonProperty("max_tokens") int maxTokens,
        List<Message> messages,
        Thinking thinking
) {
    public record Message(String role, String content) {}

    public record Thinking(String type) {
        public static Thinking disabled() {
            return new Thinking("disabled");
        }
    }

    public static ClaudeMessageRequest of(String model, int maxTokens, String prompt) {
        return new ClaudeMessageRequest(
                model,
                maxTokens,
                List.of(new Message("user", prompt)),
                Thinking.disabled()
        );
    }
}
