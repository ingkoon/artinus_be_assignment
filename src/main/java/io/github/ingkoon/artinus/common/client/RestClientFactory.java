package io.github.ingkoon.artinus.common.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class RestClientFactory {

    private final RestClient.Builder builder;

    public RestClientFactory(RestClient.Builder builder) {
        this.builder = builder;
    }

    public RestClient create(String baseUrl, Duration connectTimeout, Duration readTimeout) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(connectTimeout)
                .withReadTimeout(readTimeout);

        return builder.clone()
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }
}