package io.github.ingkoon.artinus.subscriptionhistory.repository;

import io.github.ingkoon.artinus.channel.domain.Channel;
import io.github.ingkoon.artinus.subscriptionhistory.domain.SubscriptionHistory;
import io.github.ingkoon.artinus.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.test.context.TestConfiguration;

import java.util.List;

import static io.github.ingkoon.artinus.user.enums.UserStatus.*;
import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.*;

/**
 * 이력 조회 쿼리(fetch join + createdAt asc + phone 필터) 검증.
 * data.sql로 시드된 채널을 사용하고, createdAt 채번을 위해 Auditing을 활성화한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SubscriptionHistoryRepositoryTest.AuditingConfig.class)
class SubscriptionHistoryRepositoryTest {

    @TestConfiguration
    @EnableJpaAuditing
    static class AuditingConfig {}

    @Autowired TestEntityManager em;
    @Autowired SubscriptionHistoryJpaRepository repository;

    @Test
    void 회원_이력_시간순_fetchjoin_조회_다른회원_제외() {
        Channel homepage = em.find(Channel.class, 1L);   // data.sql 시드
        Channel naver = em.find(Channel.class, 3L);
        assertThat(homepage).as("data.sql 채널 시드").isNotNull();

        User userA = em.persist(User.create("01000000001", BASIC));
        User userB = em.persist(User.create("01099999999", PREMIUM));

        em.persist(SubscriptionHistory.create(userA, homepage, NONE, BASIC));
        em.persist(SubscriptionHistory.create(userA, naver, BASIC, PREMIUM));
        em.persist(SubscriptionHistory.create(userB, homepage, NONE, PREMIUM)); // 다른 회원 → 제외돼야
        em.flush();
        em.clear();

        List<SubscriptionHistory> result =
                repository.findAllByUserPhoneOrderByCreatedAtAsc("01000000001");

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(h ->
                assertThat(h.getUser().getPhone()).isEqualTo("01000000001"));
        // fetch join으로 채널이 함께 로드됨
        assertThat(result).extracting(h -> h.getChannel().getName())
                .containsExactlyInAnyOrder("홈페이지", "네이버");
        // createdAt 오름차순 정렬
        assertThat(result).isSortedAccordingTo(comparing(SubscriptionHistory::getCreatedAt));
    }
}
