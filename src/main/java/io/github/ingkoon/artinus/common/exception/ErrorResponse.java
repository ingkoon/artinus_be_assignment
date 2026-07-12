package io.github.ingkoon.artinus.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "에러 응답")
public record ErrorResponse(
        @Schema(description = "도메인별 에러 코드", example = "CH002")
        String code,

        @Schema(description = "에러 메시지", example = "구독할 수 없는 채널입니다")
        String message
) {}