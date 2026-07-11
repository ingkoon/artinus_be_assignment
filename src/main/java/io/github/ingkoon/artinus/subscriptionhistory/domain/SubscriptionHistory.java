package io.github.ingkoon.artinus.subscriptionhistory.domain;

import io.github.ingkoon.artinus.channel.domain.Channel;
import io.github.ingkoon.artinus.common.entity.CreatedOnlyEntity;
import io.github.ingkoon.artinus.user.domain.User;
import io.github.ingkoon.artinus.user.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "SUBSCRIPTION_HISTORY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionHistory extends CreatedOnlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HISTORY_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CHANNEL_ID", nullable = false)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "FROM_STATUS", nullable = false)
    private UserStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "TO_STATUS", nullable = false)
    private UserStatus toStatus;

    public SubscriptionHistory(User user, Channel channel,
                               UserStatus fromStatus, UserStatus toStatus) {
        this.user = user;
        this.channel = channel;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    public static SubscriptionHistory create(User user, Channel channel,
                                             UserStatus fromStatus, UserStatus toStatus) {
        if (fromStatus == toStatus) {
            throw new IllegalArgumentException("상태 변화가 없는 이력은 생성할 수 없습니다");
        }
        return new SubscriptionHistory(user, channel, fromStatus, toStatus);
    }
}