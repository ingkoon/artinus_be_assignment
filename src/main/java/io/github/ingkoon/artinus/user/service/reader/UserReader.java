package io.github.ingkoon.artinus.user.service.reader;

import io.github.ingkoon.artinus.user.domain.User;
import io.github.ingkoon.artinus.user.exception.UserNotFoundException;
import io.github.ingkoon.artinus.user.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserReader {

    private final UserJpaRepository userRepository;

    @Transactional(readOnly = true)
    /** 없으면 null (최초 가입 판단용) */
    public Optional<User> findByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }

    /** 반드시 존재해야 하는 맥락(해지 등)에서 사용 */
    public User getByPhone(String phone) {
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new UserNotFoundException(phone));
    }
}