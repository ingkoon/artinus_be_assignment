package io.github.ingkoon.artinus.user.controller.request;

import io.github.ingkoon.artinus.user.enums.UserStatus;
import io.github.ingkoon.artinus.user.service.param.CancelParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "해지 요청")
public record CancelRequest(
        @Schema(description = "회원 휴대폰번호", example = "01033334444")
        @NotBlank String phone,

        @Schema(description = "채널 ID (1:홈페이지, 2:모바일앱, 5:콜센터, 6:이메일)", example = "5")
        @NotNull Long channelId,

        @Schema(description = "전환할 구독 상태(BASIC/NONE)", example = "NONE")
        @NotNull UserStatus target
) {
    public CancelParam toServiceParam() {
        return new CancelParam(phone, channelId, target);
    }
}
