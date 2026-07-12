package io.github.ingkoon.artinus.channel.exception;

import io.github.ingkoon.artinus.common.exception.BusinessException;
import io.github.ingkoon.artinus.common.exception.ErrorCode;

public class ChannelNotFoundException extends BusinessException {
    public ChannelNotFoundException(Long channelId) {
        super(ErrorCode.CHANNEL_NOT_FOUND, "channelId=" + channelId);
    }
}
