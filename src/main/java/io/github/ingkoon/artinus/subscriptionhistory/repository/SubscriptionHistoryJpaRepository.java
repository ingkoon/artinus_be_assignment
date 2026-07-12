package io.github.ingkoon.artinus.subscriptionhistory.repository;

import io.github.ingkoon.artinus.subscriptionhistory.domain.SubscriptionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionHistoryJpaRepository extends JpaRepository<SubscriptionHistory, Long> {
}
