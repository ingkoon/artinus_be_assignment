package io.github.ingkoon.artinus.common.client.claude.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Anthropic Messages API 응답 바디.
 * content 는 블록 배열이며 type=="text" 블록이 여러 개일 수 있어 모두 이어붙인다.
 * (id/role/usage/stop_reason 등 나머지 필드는 무시)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClaudeMessageResponse(
        List<ContentBlock> content
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentBlock(String type, String text) {}
}
