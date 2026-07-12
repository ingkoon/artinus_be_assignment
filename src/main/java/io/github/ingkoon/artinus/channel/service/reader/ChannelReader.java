package io.github.ingkoon.artinus.channel.service.reader;

import io.github.ingkoon.artinus.channel.domain.Channel;
import io.github.ingkoon.artinus.channel.exception.ChannelNotFoundException;
import io.github.ingkoon.artinus.channel.repository.ChannelJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChannelReader {

    private final ChannelJpaRepository channelRepository;

    public Channel getById(Long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException(channelId));
    }
}
