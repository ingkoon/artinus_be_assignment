package io.github.ingkoon.artinus.user.enums;

import io.github.ingkoon.artinus.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static io.github.ingkoon.artinus.user.enums.UserStatus.*;
import static org.assertj.core.api.Assertions.*;

class UserStatusTest {

    @Nested
    @DisplayName("구독 전이 규칙")
    class Subscribe {
        @ParameterizedTest(name = "{0} → {1} = {2}")
        @CsvSource({
                "NONE, BASIC, true",
                "NONE, PREMIUM, true",
                "NONE, NONE, false",
                "BASIC, PREMIUM, true",
                "BASIC, BASIC, false",
                "BASIC, NONE, false",
                "PREMIUM, BASIC, false",
                "PREMIUM, NONE, false",
                "PREMIUM, PREMIUM, false",
        })
        void canSubscribeTo(UserStatus from, UserStatus to, boolean expected) {
            assertThat(from.canSubscribeTo(to)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("해지 전이 규칙")
    class Cancel {
        @ParameterizedTest(name = "{0} → {1} = {2}")
        @CsvSource({
                "PREMIUM, BASIC, true",
                "PREMIUM, NONE, true",
                "PREMIUM, PREMIUM, false",
                "BASIC, NONE, true",
                "BASIC, BASIC, false",
                "BASIC, PREMIUM, false",
                "NONE, BASIC, false",
                "NONE, PREMIUM, false",
                "NONE, NONE, false",
        })
        void canCancelTo(UserStatus from, UserStatus to, boolean expected) {
            assertThat(from.canCancelTo(to)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("최초 가입 초기 상태 검증")
    class InitialStatus {
        @Test
        void basic_premium_허용() {
            assertThatCode(() -> UserStatus.validateInitialStatus(BASIC)).doesNotThrowAnyException();
            assertThatCode(() -> UserStatus.validateInitialStatus(PREMIUM)).doesNotThrowAnyException();
        }

        @Test
        void none_거부() {
            assertThatThrownBy(() -> UserStatus.validateInitialStatus(NONE))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void null_거부() {
            assertThatThrownBy(() -> UserStatus.validateInitialStatus(null))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Test
    void isSubscribed() {
        assertThat(NONE.isSubscribed()).isFalse();
        assertThat(BASIC.isSubscribed()).isTrue();
        assertThat(PREMIUM.isSubscribed()).isTrue();
    }

    @Test
    void koreanLabel() {
        assertThat(NONE.koreanLabel()).isEqualTo("구독 안함");
        assertThat(BASIC.koreanLabel()).isEqualTo("일반 구독");
        assertThat(PREMIUM.koreanLabel()).isEqualTo("프리미엄 구독");
    }
}
