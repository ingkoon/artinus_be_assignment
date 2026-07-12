package io.github.ingkoon.artinus.user.domain;

import io.github.ingkoon.artinus.common.exception.BusinessException;
import io.github.ingkoon.artinus.user.enums.UserStatus;
import io.github.ingkoon.artinus.user.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.github.ingkoon.artinus.user.enums.UserStatus.*;
import static org.assertj.core.api.Assertions.*;

/**
 * 도메인 규칙(상태 전이)이 엔티티에 있는지 검증. (같은 패키지라 생성자로 임의 상태 유저를 만들 수 있음)
 */
class UserTest {

    private static final String PHONE = "01000000000";

    @Nested
    @DisplayName("정적 팩토리 create")
    class Create {
        @Test
        void basic_가입() {
            User user = User.create(PHONE, BASIC);
            assertThat(user.getStatus()).isEqualTo(BASIC);
            assertThat(user.getPhone()).isEqualTo(PHONE);
        }

        @Test
        void none으로_가입_불가() {
            assertThatThrownBy(() -> User.create(PHONE, NONE))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("subscribeTo (실행)")
    class SubscribeTo {
        @Test
        void none_to_basic_전이하고_from반환() {
            User user = new User(PHONE, NONE);
            UserStatus from = user.subscribeTo(BASIC);
            assertThat(from).isEqualTo(NONE);
            assertThat(user.getStatus()).isEqualTo(BASIC);
        }

        @Test
        void premium은_더이상_구독불가() {
            User user = new User(PHONE, PREMIUM);
            assertThatThrownBy(() -> user.subscribeTo(BASIC))
                    .isInstanceOf(InvalidStatusTransitionException.class);
            assertThat(user.getStatus()).isEqualTo(PREMIUM); // 실패 시 상태 불변
        }
    }

    @Nested
    @DisplayName("cancelTo (실행)")
    class CancelTo {
        @Test
        void premium_to_none_전이하고_from반환() {
            User user = new User(PHONE, PREMIUM);
            UserStatus from = user.cancelTo(NONE);
            assertThat(from).isEqualTo(PREMIUM);
            assertThat(user.getStatus()).isEqualTo(NONE);
        }

        @Test
        void none은_해지불가() {
            User user = new User(PHONE, NONE);
            assertThatThrownBy(() -> user.cancelTo(BASIC))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("validate 메서드는 상태를 바꾸지 않는다 (csrng 이전 사전검증용)")
    class ValidateOnly {
        @Test
        void validateSubscribableTo_상태불변() {
            User user = new User(PHONE, NONE);
            user.validateSubscribableTo(BASIC);
            assertThat(user.getStatus()).isEqualTo(NONE); // 변경 없음
        }

        @Test
        void validateSubscribableTo_위반시_예외() {
            User user = new User(PHONE, PREMIUM);
            assertThatThrownBy(() -> user.validateSubscribableTo(BASIC))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        void validateCancellableTo_상태불변() {
            User user = new User(PHONE, PREMIUM);
            user.validateCancellableTo(NONE);
            assertThat(user.getStatus()).isEqualTo(PREMIUM);
        }

        @Test
        void validateCancellableTo_위반시_예외() {
            User user = new User(PHONE, NONE);
            assertThatThrownBy(() -> user.validateCancellableTo(BASIC))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }
}
