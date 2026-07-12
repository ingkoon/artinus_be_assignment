# 기능 개발 가이드: 남은 작업들

이력 조회(별도 스킬) 외에 남은 작업들. 우선순위 순.

## 1. Controller 레이어

구독/해지 서비스는 완성됐으나 이를 노출하는 Controller가 필요하다.

- `POST /subscriptions` — 구독. `@RequestBody SubscribeRequest` + `@Valid`. Request를 `toServiceParam()`으로 변환해 `userService.subscribe(param)` 호출.
- `POST /subscriptions/cancel` (또는 `DELETE`) — 해지. `CancelRequest` 동일 패턴.
- `GET /users/{phone}/histories` — 이력 조회(이력 스킬 완료 후 연결).

원칙:
- Controller는 얇게. 검증(`@Valid`)·변환·서비스 호출만.
- 성공 응답 형태 통일(단순 200 또는 결과 DTO).
- 예외는 던지지 말고 `GlobalExceptionHandler`가 처리하게 둔다.

## 2. 동시성 처리
같은 회원이 동시에 구독 요청을 보내면, `from` 상태를 읽는 시점과 반영 시점 사이에 경합이 생길 수 있다.

- 옵션 A: User에 `@Version` 낙관적 락. 충돌 시 재시도 또는 409 응답.
- 옵션 B: `findByPhone`에 비관적 락(`@Lock(PESSIMISTIC_WRITE)`, `SELECT ... FOR UPDATE`).
- 과제 규모에선 낙관적 락 + readme에 트레이드오프 설명 정도면 충분.
- 구현하지 않더라도 readme에 "인지하고 있으며 이렇게 대응 가능"을 명시.

## 3. csrng 클라이언트 완성

`CsrngClient.verifyOrThrow()`가 4겹 전략을 갖추었는지 확인:
- [ ] 타임아웃 (RestClientFactory로 2s/2s)
- [ ] `@Retry(name = "csrng")` — 인프라 실패만
- [ ] `@CircuitBreaker(name = "csrng", fallbackMethod = "fallback")`
- [ ] fallback 오버로드 2개: `ExternalApiFailedException`은 재전파, `Throwable`은 unavailable로
- [ ] 배열 응답 파싱(`CsrngResponse[]`), 빈 본문 방어
- [ ] `random==0` → `ExternalApiFailedException`, `random==1` → 통과
- [ ] self-invocation 아님 확인 (Service에서 호출)

## 4. 테스트

- 도메인 단위 테스트: `UserStatus` 전이 규칙(모든 전이 조합), `User.subscribeTo/cancelTo`, 최초 가입 검증.
- 서비스 테스트: 구독/해지 흐름, 채널 능력 위반, 전이 위반, csrng 실패 시 롤백.
- csrng 장애 시뮬레이션: `random=0`(롤백), 타임아웃(재시도→서킷).
- `@DataJpaTest`로 이력 fetch join·정렬 검증.
- MockMvc로 Controller 검증(형식 검증, 에러 응답 매핑).

## 5. 문서화 (readme.md) — 제출 필수

과제 평가 항목에 직결. 다음을 반드시 포함:
- 기술 선택 근거 (왜 Spring Boot 3.5, RestClient, Resilience4j, H2).
- 아키텍처 설계 (Reader/Appender/Manager 분리, Rich Domain Model).
- 채널 모델링 결정 (boolean 2컬럼 vs enum 트레이드오프).
- 이력 모델링 (from/to 둘 다 저장한 이유, append-only).
- 외부 API 장애 대응 (비즈니스/인프라 실패 구분, 4겹 전략).
- 트랜잭션 경계 판단 (csrng를 트랜잭션 밖/안 중 무엇으로, 왜).
- HTTP 상태 코드 매핑 근거.
- AI 활용 (Claude로 이력 요약, Claude Code로 개발).
- PII 최소 전송 등 개인정보 취급.

## 진행 순서 권장

1. Controller (구독/해지 노출 → 수동 테스트 가능)
2. csrng 클라이언트 최종 점검
3. 단위·통합 테스트
4. 이력 조회 + LLM 요약 (별도 스킬)
5. 동시성 처리
6. readme.md 작성