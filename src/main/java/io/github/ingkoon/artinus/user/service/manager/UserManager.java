package io.github.ingkoon.artinus.user.service.manager;

import io.github.ingkoon.artinus.user.domain.User;
import io.github.ingkoon.artinus.user.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserManager {

    public UserStatus subscribe(User user, UserStatus target) {
        return user.subscribeTo(target);   // 전이 규칙 = User 책임
    }

    public UserStatus cancel(User user, UserStatus target) {
        return user.cancelTo(target);
    }
}