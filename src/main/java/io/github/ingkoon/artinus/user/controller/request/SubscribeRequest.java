package io.github.ingkoon.artinus.user.controller.request;

import io.github.ingkoon.artinus.user.enums.UserStatus;
import io.github.ingkoon.artinus.user.service.param.SubscribeParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "구독 요청")
public record SubscribeRequest(
        @Schema(description = "회원 휴대폰번호", example = "01011112222")
        @NotBlank String phone,

        @Schema(description = "채널 ID (1:홈페이지, 2:모바일앱, 3:네이버, 4:SKT)", example = "1")
        @NotNull Long channelId,

        @Schema(description = "전환할 구독 상태(BASIC/PREMIUM)", example = "BASIC")
        @NotNull UserStatus target
) {
    public SubscribeParam toServiceParam() {
        return new SubscribeParam(phone, channelId, target);
    }
}
