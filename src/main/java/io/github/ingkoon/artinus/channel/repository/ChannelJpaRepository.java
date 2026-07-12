package io.github.ingkoon.artinus.channel.repository;

import io.github.ingkoon.artinus.channel.domain.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelJpaRepository extends JpaRepository<Channel, Long> {
}
