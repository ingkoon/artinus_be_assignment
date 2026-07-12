package io.github.ingkoon.artinus.common.client.csrng.response;

public record CsrngResponse(
        String status,
        int min,
        int max,
        int random
) {}