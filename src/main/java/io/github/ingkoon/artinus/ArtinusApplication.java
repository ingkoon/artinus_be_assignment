package io.github.ingkoon.artinus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class ArtinusApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArtinusApplication.class, args);
    }

}

