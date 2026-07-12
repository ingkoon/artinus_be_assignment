package io.github.ingkoon.artinus.channel.domain;

import io.github.ingkoon.artinus.common.entity.BaseEntity;
import io.github.ingkoon.artinus.common.exception.BusinessException;
import io.github.ingkoon.artinus.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "CHANNEL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Channel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "CAN_SUBSCRIBE", nullable = false)
    private boolean subscribable;

    @Column(name = "CAN_CANCEL", nullable = false)
    private boolean cancellable;

    public Channel(Long id, String name, boolean subscribable, boolean cancellable) {
        this.id = id;
        this.name = name;
        this.subscribable = subscribable;
        this.cancellable = cancellable;
    }

    public void validateSubscribable() {
        if (!this.subscribable) {
            throw new BusinessException(ErrorCode.CHANNEL_NOT_SUBSCRIBABLE);
        }
    }

    public void validateCancellable() {
        if (!this.cancellable) {
            throw new BusinessException(ErrorCode.CHANNEL_NOT_CANCELLABLE);
        }
    }
}