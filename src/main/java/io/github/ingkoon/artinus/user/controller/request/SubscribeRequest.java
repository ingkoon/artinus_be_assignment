package io.github.ingkoon.artinus.user.controller.request;

import io.github.ingkoon.artinus.user.enums.UserStatus;
import io.github.ingkoon.artinus.user.service.param.SubscribeParam;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubscribeRequest(
        @NotBlank String phone,
        @NotNull Long channelId,
        @NotNull UserStatus target
) {
    public SubscribeParam toServiceParam() {
        return new SubscribeParam(phone, channelId, target);
    }
}
