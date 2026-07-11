package io.github.ingkoon.artinus.common.entity;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
public class BaseEntity extends CreatedOnlyEntity {
    @LastModifiedDate
    private LocalDateTime updatedAt;
}