package io.github.ingkoon.artinus.subscriptionhistory.domain;

import org.junit.jupiter.api.Test;

import static io.github.ingkoon.artinus.user.enums.UserStatus.*;
import static org.assertj.core.api.Assertions.*;

class SubscriptionHistoryTest {

    @Test
    void 상태변화가_있어야_이력_생성() {
        SubscriptionHistory history = SubscriptionHistory.create(null, null, NONE, BASIC);
        assertThat(history.getFromStatus()).isEqualTo(NONE);
        assertThat(history.getToStatus()).isEqualTo(BASIC);
    }

    @Test
    void 상태변화가_없으면_생성불가() {
        assertThatThrownBy(() -> SubscriptionHistory.create(null, null, BASIC, BASIC))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
