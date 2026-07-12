package io.github.ingkoon.artinus.user.service.result;

import io.github.ingkoon.artinus.subscriptionhistory.domain.SubscriptionHistory;
import io.github.ingkoon.artinus.user.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 이력 조회 응답 항목. 생성 시각(createdAt)이 곧 구독/해지 날짜.
 */
@Schema(description = "구독 변경 이력 항목")
public record HistoryItem(
        @Schema(description = "행위 채널명", example = "홈페이지")
        String channelName,

        @Schema(description = "변경 전 상태", example = "NONE")
        UserStatus fromStatus,

        @Schema(description = "변경 후 상태", example = "BASIC")
        UserStatus toStatus,

        @Schema(description = "변경 시각", example = "2026-07-12T10:00:00")
        LocalDateTime changedAt
) {
    public static HistoryItem from(SubscriptionHistory history) {
        return new HistoryItem(
                history.getChannel().getName(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getCreatedAt()
        );
    }
}
