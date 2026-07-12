package io.github.ingkoon.artinus.user.service.appender;

import io.github.ingkoon.artinus.user.domain.User;
import io.github.ingkoon.artinus.user.enums.UserStatus;
import io.github.ingkoon.artinus.user.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAppender {

    private final UserJpaRepository userRepository;

    /** 최초 가입 회원 생성. 초기 상태 유효성은 User가 판단 */
    public User append(String phone, UserStatus initialStatus) {
        User user = User.create(phone, initialStatus);  // 정적 팩토리 + 검증
        return userRepository.save(user);
    }
}