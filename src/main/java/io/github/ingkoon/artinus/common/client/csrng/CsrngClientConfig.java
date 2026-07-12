package io.github.ingkoon.artinus.common.client.csrng;

import io.github.ingkoon.artinus.common.client.RestClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class CsrngClientConfig {

    @Bean
    public RestClient csrngRestClient(RestClientFactory factory) {
        return factory.create(
                "https://csrng.net",
                Duration.ofSeconds(2),
                Duration.ofSeconds(2)
        );
    }
}