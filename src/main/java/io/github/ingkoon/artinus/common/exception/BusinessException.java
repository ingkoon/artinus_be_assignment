package io.github.ingkoon.artinus.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // 상세 맥락을 메시지에 덧붙이고 싶을 때
    public BusinessException(ErrorCode errorCode, String detail) {
        super("%s (%s)".formatted(errorCode.getMessage(), detail));
        this.errorCode = errorCode;
    }
}