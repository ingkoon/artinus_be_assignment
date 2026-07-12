package io.github.ingkoon.artinus.user.repository;

import io.github.ingkoon.artinus.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);
}
