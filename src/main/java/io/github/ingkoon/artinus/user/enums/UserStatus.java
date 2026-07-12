package io.github.ingkoon.artinus.user.enums;

import io.github.ingkoon.artinus.common.exception.BusinessException;
import io.github.ingkoon.artinus.common.exception.ErrorCode;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum UserStatus {
    NONE,
    BASIC,
    PREMIUM;

    private static final Map<UserStatus, Set<UserStatus>> SUBSCRIBABLE_TO = Map.of(
            NONE,    EnumSet.of(BASIC, PREMIUM),
            BASIC,   EnumSet.of(PREMIUM),
            PREMIUM, EnumSet.noneOf(UserStatus.class)
    );

    private static final Map<UserStatus, Set<UserStatus>> CANCELLABLE_TO = Map.of(
            PREMIUM, EnumSet.of(BASIC, NONE),
            BASIC,   EnumSet.of(NONE),
            NONE,    EnumSet.noneOf(UserStatus.class)
    );

    public boolean canSubscribeTo(UserStatus target) {
        return SUBSCRIBABLE_TO.get(this).contains(target);
    }

    public boolean canCancelTo(UserStatus target) {
        return CANCELLABLE_TO.get(this).contains(target);
    }

    public boolean isSubscribed() {
        return this != NONE;
    }

    public static void validateInitialStatus(UserStatus target) {
        if (target == null || !target.isSubscribed()) {
            throw new BusinessException(ErrorCode.INVALID_INITIAL_STATUS);
        }
    }

    /** 이력 요약 프롬프트 등 표현용 한글 라벨. */
    public String koreanLabel() {
        return switch (this) {
            case NONE -> "구독 안함";
            case BASIC -> "일반 구독";
            case PREMIUM -> "프리미엄 구독";
        };
    }
}