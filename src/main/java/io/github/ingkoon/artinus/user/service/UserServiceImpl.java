package io.github.ingkoon.artinus.user.service;

import io.github.ingkoon.artinus.channel.domain.Channel;
import io.github.ingkoon.artinus.channel.service.reader.ChannelReader;
import io.github.ingkoon.artinus.common.client.claude.ClaudeClient;
import io.github.ingkoon.artinus.common.client.csrng.CsrngClient;
import io.github.ingkoon.artinus.common.client.csrng.exception.ExternalApiUnavailableException;
import io.github.ingkoon.artinus.subscriptionhistory.service.appender.SubscriptionHistoryAppender;
import io.github.ingkoon.artinus.subscriptionhistory.service.reader.SubscriptionHistoryReader;
import io.github.ingkoon.artinus.user.domain.User;
import io.github.ingkoon.artinus.user.enums.UserStatus;
import io.github.ingkoon.artinus.user.service.appender.UserAppender;
import io.github.ingkoon.artinus.user.service.manager.UserManager;
import io.github.ingkoon.artinus.user.service.param.CancelParam;
import io.github.ingkoon.artinus.user.service.param.ReadHistoryParam;
import io.github.ingkoon.artinus.user.service.param.SubscribeParam;
import io.github.ingkoon.artinus.user.service.reader.UserReader;
import io.github.ingkoon.artinus.user.service.result.HistoryItem;
import io.github.ingkoon.artinus.user.service.result.ReadHistorySummaryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy년 M월 d일");

    private final UserReader userReader;
    private final UserAppender userAppender;
    private final UserManager userManager;
    private final ChannelReader channelReader;
    private final SubscriptionHistoryAppender historyAppender;
    private final SubscriptionHistoryReader historyReader;
    private final CsrngClient csrngClient;
    private final ClaudeClient claudeClient;

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

    /**
     * 이력 조회 + LLM 요약.
     * 조회는 Reader의 readOnly 트랜잭션으로 끝내고, 느린 Claude 호출은 트랜잭션 밖에서 수행한다
     * (DB 커넥션을 30초씩 잡지 않도록). 요약 실패는 목록 반환을 막지 않는다(graceful degradation).
     */
    @Override
    public ReadHistorySummaryResult getHistory(ReadHistoryParam param) {
        List<HistoryItem> history = historyReader.readAllByPhoneInTimeOrder(param.phone())
                .stream()
                .map(HistoryItem::from)
                .toList();

        String summary = summarizeSafely(history);
        return new ReadHistorySummaryResult(history, summary);
    }

    private String summarizeSafely(List<HistoryItem> history) {
        if (history.isEmpty()) {
            return "구독 이력이 없습니다.";
        }
        try {
            return claudeClient.summarize(buildPrompt(history));
        } catch (ExternalApiUnavailableException e) {
            // 요약은 부가 기능이라 실패해도 핵심 데이터(목록)는 살린다
            log.warn("이력 요약 생성 실패, 요약 없이 반환", e);
            return "요약을 생성하지 못했습니다.";
        }
    }

    /**
     * PII(전화번호) 미포함 — 채널·날짜·상태 전이만 나열. 상태는 한글로 변환.
     */
    private String buildPrompt(List<HistoryItem> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("다음은 한 회원의 구독 변경 이력입니다. ")
          .append("시간 순서대로 자연스러운 한국어 한 문단으로 요약해 주세요.\n\n");
        for (HistoryItem item : history) {
            sb.append("- ")
              .append(item.changedAt().format(DATE_FORMAT))
              .append(" / ").append(item.channelName())
              .append(" / ").append(item.fromStatus().koreanLabel())
              .append(" → ").append(item.toStatus().koreanLabel())
              .append('\n');
        }
        return sb.toString();
    }
}