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
}