package io.github.ingkoon.artinus.user.domain;

import io.github.ingkoon.artinus.common.entity.BaseEntity;
import io.github.ingkoon.artinus.common.exception.ErrorCode;
import io.github.ingkoon.artinus.user.enums.UserStatus;
import io.github.ingkoon.artinus.user.exception.InvalidStatusTransitionException;
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
    @Column(name = "ID")
    private Long id;

    @Column(name = "PHONE", nullable = false, unique = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    private UserStatus status;

    /** 낙관적 락: 같은 회원 동시 전이 시 나중 커밋을 충돌로 감지(→ 진 트랜잭션 롤백). */
    @Version
    @Column(name = "VERSION")
    private Long version;

    public User(String phone, UserStatus status) {
        this.phone = phone;
        this.status = status;
    }


    /**
     * 최초 가입 회원 생성.
     * 가입 가능한 초기 상태(BASIC/PREMIUM)인지 검증한 뒤 생성한다.
     */
    public static User create(String phone, UserStatus initialStatus) {
        UserStatus.validateInitialStatus(initialStatus);
        return new User(phone, initialStatus);
    }

    /**
     * 구독 전이를 수행하고 전이 직전 상태(from)를 반환한다.
     */
    public UserStatus subscribeTo(UserStatus target) {
        validateSubscribableTo(target);
        UserStatus from = this.status;
        this.status = target;
        return from;
    }

    /**
     * 해지 전이를 수행하고 전이 직전 상태(from)를 반환한다.
     */
    public UserStatus cancelTo(UserStatus target) {
        validateCancellableTo(target);
        UserStatus from = this.status;
        this.status = target;
        return from;
    }

    /**
     * 구독 전이 가능 여부만 검증한다. (상태 변경 없음 — 외부 API 호출 전 사전 검증용)
     */
    public void validateSubscribableTo(UserStatus target) {
        if (!this.status.canSubscribeTo(target)) {
            throw new InvalidStatusTransitionException(
                    ErrorCode.INVALID_SUBSCRIBE_TRANSITION, this.status, target);
        }
    }

    /**
     * 해지 전이 가능 여부만 검증한다. (상태 변경 없음 — 외부 API 호출 전 사전 검증용)
     */
    public void validateCancellableTo(UserStatus target) {
        if (!this.status.canCancelTo(target)) {
            throw new InvalidStatusTransitionException(
                    ErrorCode.INVALID_CANCEL_TRANSITION, this.status, target);
        }
    }
}
