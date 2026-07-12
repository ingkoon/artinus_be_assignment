package io.github.ingkoon.artinus.user.repository;

import io.github.ingkoon.artinus.user.domain.User;
import jakarta.persistence.OptimisticLockException;
import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static io.github.ingkoon.artinus.user.enums.UserStatus.*;
import static org.assertj.core.api.Assertions.*;

/**
 * 낙관적 락(@Version) 검증.
 * - 정상 갱신 시 version 증가
 * - 다른 트랜잭션이 먼저 갱신(version↑)한 뒤 stale 엔티티로 전이하면 충돌 발생
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserOptimisticLockTest {

    @Autowired
    TestEntityManager em;

    @Test
    void 정상_전이시_version_증가() {
        User user = em.persistFlushFind(User.create("01000000009", BASIC));
        assertThat(user.getVersion()).isZero();

        user.subscribeTo(PREMIUM);
        em.flush();

        assertThat(user.getVersion()).isEqualTo(1L);
    }

    @Test
    void 동시_전이_충돌시_예외() {
        User user = em.persistFlushFind(User.create("01000000008", BASIC));
        Long id = user.getId();
        em.clear();

        User stale = em.find(User.class, id);           // version 0 로드
        assertThat(stale.getVersion()).isZero();

        // 다른 트랜잭션이 먼저 커밋해 DB version 을 올린 상황을 모사(네이티브로 PC 우회)
        em.getEntityManager()
                .createNativeQuery("update USERS set VERSION = VERSION + 1 where ID = :id")
                .setParameter("id", id)
                .executeUpdate();

        // stale(version 0)로 전이 후 flush → UPDATE ... WHERE VERSION=0 이 0행 → 충돌
        stale.subscribeTo(PREMIUM);
        assertThatThrownBy(em::flush)
                .isInstanceOfAny(OptimisticLockException.class, StaleObjectStateException.class);
    }
}
