package io.github.ingkoon.artinus.channel.domain;

import io.github.ingkoon.artinus.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ChannelTest {

    @Test
    void 구독전용_채널() {
        Channel naver = new Channel(3L, "네이버", true, false);
        assertThat(naver.isSubscribable()).isTrue();
        assertThat(naver.isCancellable()).isFalse();
        assertThatCode(naver::validateSubscribable).doesNotThrowAnyException();
        assertThatThrownBy(naver::validateCancellable).isInstanceOf(BusinessException.class);
    }

    @Test
    void 해지전용_채널() {
        Channel callCenter = new Channel(5L, "콜센터", false, true);
        assertThatThrownBy(callCenter::validateSubscribable).isInstanceOf(BusinessException.class);
        assertThatCode(callCenter::validateCancellable).doesNotThrowAnyException();
    }

    @Test
    void 구독_해지_모두가능_채널() {
        Channel homepage = new Channel(1L, "홈페이지", true, true);
        assertThatCode(homepage::validateSubscribable).doesNotThrowAnyException();
        assertThatCode(homepage::validateCancellable).doesNotThrowAnyException();
    }
}
