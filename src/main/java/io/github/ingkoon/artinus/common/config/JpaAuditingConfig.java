package io.github.ingkoon.artinus.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing(@CreatedDate/@LastModifiedDate) 활성화.
 * ArtinusApplication에 직접 붙이면 @WebMvcTest 슬라이스가 JPA 없이 로드에 실패하므로 분리한다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
