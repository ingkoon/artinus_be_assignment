package io.github.ingkoon.artinus.user.exception;

import io.github.ingkoon.artinus.common.exception.BusinessException;
import io.github.ingkoon.artinus.common.exception.ErrorCode;

public class UserNotFoundException extends BusinessException {
    public UserNotFoundException(String phone) {
        super(ErrorCode.USER_NOT_FOUND, "phone=" + phone);
    }
}