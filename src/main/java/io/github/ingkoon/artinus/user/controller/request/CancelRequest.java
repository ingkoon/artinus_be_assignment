package io.github.ingkoon.artinus.user.controller.request;

import io.github.ingkoon.artinus.user.enums.UserStatus;
import io.github.ingkoon.artinus.user.service.param.CancelParam;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CancelRequest(
        @NotBlank String phone,
        @NotNull Long channelId,
        @NotNull UserStatus target
) {
    public CancelParam toServiceParam() {
        return new CancelParam(phone, channelId, target);
    }
}
