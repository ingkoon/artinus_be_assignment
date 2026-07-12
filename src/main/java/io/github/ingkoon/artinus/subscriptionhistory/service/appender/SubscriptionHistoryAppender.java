package io.github.ingkoon.artinus.subscriptionhistory.service.appender;

import io.github.ingkoon.artinus.channel.domain.Channel;
import io.github.ingkoon.artinus.subscriptionhistory.domain.SubscriptionHistory;
import io.github.ingkoon.artinus.subscriptionhistory.repository.SubscriptionHistoryJpaRepository;
import io.github.ingkoon.artinus.user.domain.User;
import io.github.ingkoon.artinus.user.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionHistoryAppender {

    private final SubscriptionHistoryJpaRepository historyRepository;

    public void append(User user, Channel channel,
                       UserStatus from, UserStatus to) {
        historyRepository.save(
                SubscriptionHistory.create(user, channel, from, to));
    }
}
