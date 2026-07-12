package io.github.ingkoon.artinus.subscriptionhistory.service.reader;

import io.github.ingkoon.artinus.subscriptionhistory.domain.SubscriptionHistory;
import io.github.ingkoon.artinus.subscriptionhistory.repository.SubscriptionHistoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SubscriptionHistoryReader {

    private final SubscriptionHistoryJpaRepository historyRepository;

    /** 회원 이력을 시간순으로 조회 (fetch join). */
    @Transactional(readOnly = true)
    public List<SubscriptionHistory> readAllByPhoneInTimeOrder(String phone) {
        return historyRepository.findAllByUserPhoneOrderByCreatedAtAsc(phone);
    }
}
