package io.github.ingkoon.artinus.user.domain;

import io.github.ingkoon.artinus.common.entity.BaseEntity;
import io.github.ingkoon.artinus.user.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "USERS")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_ID")
    private Long id;

    @Column(name = "PHONE", nullable = false, unique = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    private UserStatus status;

    public User(String phone, UserStatus status) {
        this.phone = phone;
        this.status = status;
    }

    public void changeStatus(UserStatus newStatus) {
        this.status = newStatus;
    }
}
