package io.github.ingkoon.artinus.common.client.csrng;

import io.github.ingkoon.artinus.common.client.csrng.exception.ExternalApiFailedException;
import io.github.ingkoon.artinus.common.client.csrng.exception.ExternalApiUnavailableException;
import io.github.ingkoon.artinus.common.client.csrng.response.CsrngResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
@Slf4j
public class CsrngClient {

    private final RestClient csrngRestClient;

    /**
     * csrng를 호출해 트랜잭션 진행 가능 여부를 검증한다.
     * - random == 1 : 정상 통과 (반환)
     * - random == 0 : ExternalApiFailedException (비즈니스 롤백, 재시도 X)
     * - 통신 장애    : ExternalApiUnavailableException (재시도/서킷 대상)
     */
    @CircuitBreaker(name = "csrng", fallbackMethod = "fallback")
    @Retry(name = "csrng")
    public void verifyOrThrow() {
        CsrngResponse response = call();

        if (response.random() == 0) {
            log.info("csrng 비즈니스 실패: random=0, 롤백 처리");
            throw new ExternalApiFailedException();
        }
        // random == 1 → 정상, 아무것도 안 하고 통과
    }

    private CsrngResponse call() {
        try {
            CsrngResponse[] body = csrngRestClient.get()
                    .uri("/csrng/csrng.php?min=0&max=1")
                    .retrieve()
                    .body(CsrngResponse[].class);

            if (body == null || body.length == 0) {
                // 200이지만 본문이 비정상일경우 인프라 실패로 취급
                throw new ExternalApiUnavailableException();
            }
            return body[0];   // csrng는 배열로 응답

        } catch (ResourceAccessException e) {
            // 타임아웃, 커넥션 거부 등
            log.warn("csrng 통신 장애", e);
            throw new ExternalApiUnavailableException(e);
        } catch (RestClientResponseException e) {
            // 4xx/5xx
            log.warn("csrng HTTP 오류: {}", e.getStatusCode(), e);
            throw new ExternalApiUnavailableException(e);
        }
    }

    /**
     * 서킷 open 상태이거나 재시도가 모두 소진됐을 때 호출된다.
     * 단, 비즈니스 실패(random=0)는 fallback으로 흘러오면 안 되므로 그대로 재전파한다.
     */
    private void fallback(ExternalApiFailedException e) {
        throw e;   // random=0은 fallback 대상이 아님 → 그대로 던짐
    }

    private void fallback(Throwable t) {
        log.error("csrng 사용 불가 (재시도 소진 또는 서킷 open)", t);
        throw new ExternalApiUnavailableException();
    }
}