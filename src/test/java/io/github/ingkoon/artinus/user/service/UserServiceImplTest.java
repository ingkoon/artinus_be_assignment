package io.github.ingkoon.artinus.user.service;

import io.github.ingkoon.artinus.channel.domain.Channel;
import io.github.ingkoon.artinus.common.client.claude.ClaudeClient;
import io.github.ingkoon.artinus.common.client.csrng.CsrngClient;
import io.github.ingkoon.artinus.common.client.csrng.exception.ExternalApiFailedException;
import io.github.ingkoon.artinus.common.client.csrng.exception.ExternalApiUnavailableException;
import io.github.ingkoon.artinus.common.exception.BusinessException;
import io.github.ingkoon.artinus.common.exception.ErrorCode;
import io.github.ingkoon.artinus.subscriptionhistory.domain.SubscriptionHistory;
import io.github.ingkoon.artinus.subscriptionhistory.service.appender.SubscriptionHistoryAppender;
import io.github.ingkoon.artinus.subscriptionhistory.service.reader.SubscriptionHistoryReader;
import io.github.ingkoon.artinus.user.domain.User;
import io.github.ingkoon.artinus.user.enums.UserStatus;
import io.github.ingkoon.artinus.user.exception.InvalidStatusTransitionException;
import io.github.ingkoon.artinus.user.service.appender.UserAppender;
import io.github.ingkoon.artinus.user.service.manager.UserManager;
import io.github.ingkoon.artinus.user.service.param.CancelParam;
import io.github.ingkoon.artinus.user.service.param.ReadHistoryParam;
import io.github.ingkoon.artinus.user.service.param.SubscribeParam;
import io.github.ingkoon.artinus.user.service.reader.UserReader;
import io.github.ingkoon.artinus.user.service.result.ReadHistorySummaryResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static io.github.ingkoon.artinus.user.enums.UserStatus.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String PHONE = "01011112222";

    @Mock UserReader userReader;
    @Mock UserAppender userAppender;
    @Mock UserManager userManager;
    @Mock io.github.ingkoon.artinus.channel.service.reader.ChannelReader channelReader;
    @Mock SubscriptionHistoryAppender historyAppender;
    @Mock SubscriptionHistoryReader historyReader;
    @Mock CsrngClient csrngClient;
    @Mock ClaudeClient claudeClient;

    @InjectMocks UserServiceImpl service;

    @Nested
    @DisplayName("subscribe")
    class Subscribe {
        @Test
        void 최초가입_none에서_basic() {
            Channel channel = mock(Channel.class);
            User user = mock(User.class);
            when(channelReader.getById(1L)).thenReturn(channel);
            when(userReader.findByPhone(PHONE)).thenReturn(Optional.empty());
            when(userAppender.append(PHONE, BASIC)).thenReturn(user);

            service.subscribe(new SubscribeParam(PHONE, 1L, BASIC));

            verify(channel).validateSubscribable();
            verify(csrngClient).verifyOrThrow();
            verify(userAppender).append(PHONE, BASIC);
            verify(historyAppender).append(user, channel, NONE, BASIC);
            verify(userManager, never()).subscribe(any(), any());
        }

        @Test
        void 기존회원_등급상승_basic에서_premium() {
            Channel channel = mock(Channel.class);
            User user = mock(User.class);
            when(channelReader.getById(1L)).thenReturn(channel);
            when(userReader.findByPhone(PHONE)).thenReturn(Optional.of(user));
            when(userManager.subscribe(user, PREMIUM)).thenReturn(BASIC);

            service.subscribe(new SubscribeParam(PHONE, 1L, PREMIUM));

            verify(user).validateSubscribableTo(PREMIUM);
            verify(csrngClient).verifyOrThrow();
            verify(userManager).subscribe(user, PREMIUM);
            verify(historyAppender).append(user, channel, BASIC, PREMIUM);
            verify(userAppender, never()).append(any(), any());
        }

        @Test
        void 구독불가_채널이면_csrng_호출전_차단() {
            Channel channel = mock(Channel.class);
            when(channelReader.getById(5L)).thenReturn(channel);
            doThrow(new BusinessException(ErrorCode.CHANNEL_NOT_SUBSCRIBABLE))
                    .when(channel).validateSubscribable();

            assertThatThrownBy(() -> service.subscribe(new SubscribeParam(PHONE, 5L, BASIC)))
                    .isInstanceOf(BusinessException.class);

            verify(csrngClient, never()).verifyOrThrow();
            verifyNoInteractions(userAppender, userManager, historyAppender);
        }

        @Test
        void 전이위반이면_csrng_호출전_차단() {
            Channel channel = mock(Channel.class);
            User user = mock(User.class);
            when(channelReader.getById(1L)).thenReturn(channel);
            when(userReader.findByPhone(PHONE)).thenReturn(Optional.of(user));
            doThrow(new InvalidStatusTransitionException(ErrorCode.INVALID_SUBSCRIBE_TRANSITION, PREMIUM, BASIC))
                    .when(user).validateSubscribableTo(BASIC);

            assertThatThrownBy(() -> service.subscribe(new SubscribeParam(PHONE, 1L, BASIC)))
                    .isInstanceOf(InvalidStatusTransitionException.class);

            verify(csrngClient, never()).verifyOrThrow();
            verifyNoInteractions(userManager, historyAppender);
        }

        @Test
        void csrng_실패시_상태전이_이력기록_안함() {
            Channel channel = mock(Channel.class);
            when(channelReader.getById(1L)).thenReturn(channel);
            when(userReader.findByPhone(PHONE)).thenReturn(Optional.empty());
            doThrow(new ExternalApiFailedException()).when(csrngClient).verifyOrThrow();

            assertThatThrownBy(() -> service.subscribe(new SubscribeParam(PHONE, 1L, BASIC)))
                    .isInstanceOf(ExternalApiFailedException.class);

            verify(userAppender, never()).append(any(), any());
            verifyNoInteractions(userManager, historyAppender);
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {
        @Test
        void 해지_흐름() {
            Channel channel = mock(Channel.class);
            User user = mock(User.class);
            when(channelReader.getById(1L)).thenReturn(channel);
            when(userReader.getByPhone(PHONE)).thenReturn(user);
            when(userManager.cancel(user, NONE)).thenReturn(PREMIUM);

            service.cancel(new CancelParam(PHONE, 1L, NONE));

            verify(channel).validateCancellable();
            verify(user).validateCancellableTo(NONE);
            verify(csrngClient).verifyOrThrow();
            verify(userManager).cancel(user, NONE);
            verify(historyAppender).append(user, channel, PREMIUM, NONE);
        }

        @Test
        void csrng_실패시_해지_이력기록_안함() {
            Channel channel = mock(Channel.class);
            User user = mock(User.class);
            when(channelReader.getById(1L)).thenReturn(channel);
            when(userReader.getByPhone(PHONE)).thenReturn(user);
            doThrow(new ExternalApiFailedException()).when(csrngClient).verifyOrThrow();

            assertThatThrownBy(() -> service.cancel(new CancelParam(PHONE, 1L, NONE)))
                    .isInstanceOf(ExternalApiFailedException.class);

            verify(userManager, never()).cancel(any(), any());
            verifyNoInteractions(historyAppender);
        }
    }

    @Nested
    @DisplayName("getHistory")
    class GetHistory {
        @Test
        void 이력없으면_claude_미호출() {
            when(historyReader.readAllByPhoneInTimeOrder(PHONE)).thenReturn(List.of());

            ReadHistorySummaryResult result = service.getHistory(new ReadHistoryParam(PHONE));

            assertThat(result.history()).isEmpty();
            assertThat(result.summary()).isEqualTo("구독 이력이 없습니다.");
            verifyNoInteractions(claudeClient);
        }

        @Test
        void 요약실패해도_목록은_반환() {
            SubscriptionHistory history = stubHistory();
            when(historyReader.readAllByPhoneInTimeOrder(PHONE)).thenReturn(List.of(history));
            when(claudeClient.summarize(anyString())).thenThrow(new ExternalApiUnavailableException());

            ReadHistorySummaryResult result = service.getHistory(new ReadHistoryParam(PHONE));

            assertThat(result.history()).hasSize(1);
            assertThat(result.history().get(0).channelName()).isEqualTo("홈페이지");
            assertThat(result.summary()).isEqualTo("요약을 생성하지 못했습니다.");
        }

        @Test
        void 프롬프트에_PII_전화번호_미포함_한글상태포함() {
            SubscriptionHistory history = stubHistory();
            when(historyReader.readAllByPhoneInTimeOrder(PHONE)).thenReturn(List.of(history));
            when(claudeClient.summarize(anyString())).thenReturn("요약 결과");

            ReadHistorySummaryResult result = service.getHistory(new ReadHistoryParam(PHONE));

            assertThat(result.summary()).isEqualTo("요약 결과");
            ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
            verify(claudeClient).summarize(prompt.capture());
            assertThat(prompt.getValue())
                    .doesNotContain(PHONE)          // PII 미포함
                    .contains("홈페이지")
                    .contains("일반 구독");          // 상태 한글 변환
        }

        private SubscriptionHistory stubHistory() {
            SubscriptionHistory history = mock(SubscriptionHistory.class);
            Channel channel = mock(Channel.class);
            when(history.getChannel()).thenReturn(channel);
            when(channel.getName()).thenReturn("홈페이지");
            when(history.getFromStatus()).thenReturn(NONE);
            when(history.getToStatus()).thenReturn(BASIC);
            when(history.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 1, 1, 10, 0));
            return history;
        }
    }
}
