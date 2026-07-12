package io.github.ingkoon.artinus.user.exception;

import io.github.ingkoon.artinus.common.exception.BusinessException;
import io.github.ingkoon.artinus.common.exception.ErrorCode;
import io.github.ingkoon.artinus.user.enums.UserStatus;

public class InvalidStatusTransitionException extends BusinessException {
    public InvalidStatusTransitionException(ErrorCode code,
                                            UserStatus from, UserStatus to) {
        super(code, "%s → %s".formatted(from, to));
    }
}