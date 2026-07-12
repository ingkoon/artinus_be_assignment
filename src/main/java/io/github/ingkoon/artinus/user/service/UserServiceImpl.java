package io.github.ingkoon.artinus.user.service;

import io.github.ingkoon.artinus.channel.domain.Channel;
import io.github.ingkoon.artinus.channel.service.reader.ChannelReader;
import io.github.ingkoon.artinus.common.client.csrng.CsrngClient;
import io.github.ingkoon.artinus.subscriptionhistory.service.appender.SubscriptionHistoryAppender;
import io.github.ingkoon.artinus.user.domain.User;
import io.github.ingkoon.artinus.user.enums.UserStatus;
import io.github.ingkoon.artinus.user.service.appender.UserAppender;
import io.github.ingkoon.artinus.user.service.manager.UserManager;
import io.github.ingkoon.artinus.user.service.param.CancelParam;
import io.github.ingkoon.artinus.user.service.param.SubscribeParam;
import io.github.ingkoon.artinus.user.service.reader.UserReader;
import io.github.ingkoon.artinus.user.service.result.ReadHistorySummaryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserReader userReader;
    private final UserAppender userAppender;
    private final UserManager userManager;
    private final ChannelReader channelReader;
    private final SubscriptionHistoryAppender historyAppender;
    private final CsrngClient csrngClient;

    @Override
    @Transactional
    public void subscribe(SubscribeParam param) {
        Channel channel = channelReader.getById(param.channelId());
        channel.validateSubscribable();

        User user = userReader.findByPhone(param.phone()).orElse(null);

        // 외부 API 호출 전 전이 가능성 검증 (상태 변경 없음)
        if (user == null) {
            UserStatus.validateInitialStatus(param.target());   // 최초 가입
        } else {
            user.validateSubscribableTo(param.target());        // 기존 회원 등급 상승
        }

        csrngClient.verifyOrThrow();

        // csrng 통과 후 실제 반영
        UserStatus from;
        if (user == null) {
            user = userAppender.append(param.phone(), param.target());
            from = UserStatus.NONE;
        } else {
            from = userManager.subscribe(user, param.target());
        }

        historyAppender.append(user, channel, from, param.target());
    }

    @Override
    @Transactional
    public void cancel(CancelParam param) {
        Channel channel = channelReader.getById(param.channelId());
        channel.validateCancellable();

        User user = userReader.getByPhone(param.phone());
        user.validateCancellableTo(param.target());   // 검증 전용 (상태 변경 없음)

        csrngClient.verifyOrThrow();

        UserStatus from = userManager.cancel(user, param.target());   // 실제 전이
        historyAppender.append(user, channel, from, param.target());
    }

    @Override
    public ReadHistorySummaryResult getHistory() {
        return null;
    }
}