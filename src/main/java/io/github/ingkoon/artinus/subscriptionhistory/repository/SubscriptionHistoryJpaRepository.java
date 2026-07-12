package io.github.ingkoon.artinus.subscriptionhistory.repository;

import io.github.ingkoon.artinus.subscriptionhistory.domain.SubscriptionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubscriptionHistoryJpaRepository extends JpaRepository<SubscriptionHistory, Long> {

    /**
     * 회원 이력을 시간순(오름차순)으로 조회. User 및 Channel을 fetch join하여 N+1 회피.
     */
    @Query("""
            select h from SubscriptionHistory h
            join fetch h.channel c
            join fetch h.user u
            where u.phone = :phone
            order by h.createdAt asc
            """)
    List<SubscriptionHistory> findAllByUserPhoneOrderByCreatedAtAsc(@Param("phone") String phone);
}
