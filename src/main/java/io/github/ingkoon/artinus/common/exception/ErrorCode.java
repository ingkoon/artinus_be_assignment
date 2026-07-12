package io.github.ingkoon.artinus.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C001", "서버 내부 오류가 발생했습니다"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C002", "입력값이 올바르지 않습니다"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "회원을 찾을 수 없습니다"),

    // Channel
    CHANNEL_NOT_FOUND(HttpStatus.NOT_FOUND, "CH001", "채널을 찾을 수 없습니다"),
    CHANNEL_NOT_SUBSCRIBABLE(HttpStatus.BAD_REQUEST, "CH002", "구독할 수 없는 채널입니다"),
    CHANNEL_NOT_CANCELLABLE(HttpStatus.BAD_REQUEST, "CH003", "해지할 수 없는 채널입니다"),

    // 상태 전이
    INVALID_SUBSCRIBE_TRANSITION(HttpStatus.CONFLICT, "S001", "요청한 구독 상태로 변경할 수 없습니다"),
    INVALID_CANCEL_TRANSITION(HttpStatus.CONFLICT, "S002", "요청한 해지 상태로 변경할 수 없습니다"),
    INVALID_INITIAL_STATUS(HttpStatus.BAD_REQUEST, "S003", "가입 시 설정할 수 없는 구독 상태입니다"),

    // 외부 API
    EXTERNAL_API_FAILED(HttpStatus.BAD_GATEWAY, "E001", "외부 API 처리에 실패했습니다"),
    EXTERNAL_API_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "E002", "외부 API를 일시적으로 사용할 수 없습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}