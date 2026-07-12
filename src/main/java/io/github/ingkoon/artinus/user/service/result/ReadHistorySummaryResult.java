package io.github.ingkoon.artinus.user.service.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 이력 조회 + LLM 요약 응답.
 * history: 시간순 이력 목록, summary: Claude가 생성한 자연어 요약(실패 시 대체 문구).
 */
@Schema(description = "이력 조회 + LLM 요약 응답")
public record ReadHistorySummaryResult(
        @Schema(description = "시간순 구독 변경 이력")
        List<HistoryItem> history,

        @Schema(description = "이력 자연어 요약(생성 실패 시 대체 문구)",
                example = "2026년 7월 12일 홈페이지를 통해 일반 구독으로 가입한 뒤, ...")
        String summary
) {}
