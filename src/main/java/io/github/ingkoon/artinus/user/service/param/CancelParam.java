package io.github.ingkoon.artinus.user.service.param;

import io.github.ingkoon.artinus.user.enums.UserStatus;

public record CancelParam(
        String phone,
        Long channelId,
        UserStatus target
) {}
