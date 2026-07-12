# 진행 상황 및 마일스톤

ARTINUS 백엔드 과제 — 구독 서비스 API. 최종 갱신 기준 진행 현황과 남은 작업.

## 확정된 기술 스택

| 항목 | 선택 | 비고 |
|---|---|---|
| 언어 | Java 17 | |
| 프레임워크 | Spring Boot **3.5.16** | 4.x에서 API 호환성 문제로 하향 확정. Spring Framework 6.2 계열. |
| 영속성 | Spring Data JPA + Hibernate | |
| DB | H2 (in-memory, MySQL 모드) | 과제용. `ddl-auto: create` + data.sql 시드. |
| HTTP 클라이언트 | RestClient (via RestClientFactory) | RestTemplate 아님. |
| 장애 대응 | Resilience4j (spring-boot3) | 서킷브레이커 + 재시도. |
| LLM | Claude (Anthropic API) | 이력 요약용. 모델명 미확정(docs 확인 필요). |

## 마일스톤

### M1. 도메인 설계 ✅ 완료

- [x] `UserStatus` enum — 상태(NONE/BASIC/PREMIUM) + 전이 규칙(Map 기반), 초기 상태 검증
- [x] `User` 엔티티 — 정적 팩토리, `subscribeTo`/`cancelTo`(실행), `validateSubscribableTo`/`validateCancellableTo`(검증)
- [x] `Channel` 엔티티 — boolean 2컬럼(subscribable/cancellable), `validateXxx` + `isXxx`
- [x] `SubscriptionHistory` 엔티티 — from/to 둘 다, append-only, 전 필드 non-null
- [x] 감사 필드 상속 체인 — `CreatedOnlyEntity`(이력) → `BaseEntity`(User/Channel)

**주요 결정:**
- 채널 능력을 enum이 아닌 **별도 boolean 컬럼**으로 (두 능력은 직교).
- 채널을 User가 아닌 **이력 테이블**에 연결 (채널은 행위에 딸린 정보).
- 이력에 **from/to 둘 다 저장** (해지 전이 서술 완결성).
- 상태는 null이 아닌 **명시적 NONE**.

### M2. 서비스 계층 ✅ 완료 (일부 점검 필요)

- [x] Reader/Appender/Manager 컴포넌트 분리 (User/Channel/History)
- [x] `UserServiceImpl.subscribe()` — 최초 가입/기존 회원 분기, 검증→csrng→전이→이력
- [x] `UserServiceImpl.cancel()` — 검증→csrng→전이→이력 (이중 전이 버그 수정 완료)
- [x] 트랜잭션 경계를 Service에 배치
- [ ] `getHistory()` — 미구현 (M5)

**주요 결정:**
- 검증(validate)과 실행(전이) 분리 → csrng 앞에서 검증, 뒤에서 전이.
- 싼 검증 먼저, 비싼 외부 호출 나중.

### M3. 예외 체계 ✅ 완료

- [x] `ErrorCode` enum (HttpStatus + 코드 + 메시지, 도메인별 접두사)
- [x] `BusinessException` 공통 부모 (RuntimeException)
- [x] 개별 예외 (`InvalidStatusTransitionException`, `ExternalApiFailedException`, `ExternalApiUnavailableException` 등)
- [x] `GlobalExceptionHandler` (`@RestControllerAdvice`)
- [ ] `HttpMessageNotReadableException` 핸들러 (잘못된 enum 입력 방어) — 점검 필요

### M4. 인프라/설정 ✅ 완료

- [x] `RestClientFactory` — 신 API(`ClientHttpRequestFactoryBuilder.detect()`)로 통일
- [x] `CsrngClientConfig` — 2s/2s 타임아웃
- [x] `application.yml` — datasource/jpa/h2/sql-init, resilience4j(최상위), 예외 FQCN
- [x] `data.sql` — 채널 6개 시드 + 테스트 유저 2명
- [x] 애플리케이션 정상 기동 확인

### M5. 이력 조회 + LLM 요약 ⬜ 미착수

> `.claude/skill/history-summary-feature.md` 참조. 서드파티 의존이라 마지막.

- [ ] `SubscriptionHistoryReader` fetch join + 시간순
- [ ] `HistoryItem` / `ReadHistorySummaryResult` DTO
- [ ] `ClaudeClient` + Config (read timeout 30s, 헤더, 모델명)
- [ ] 프롬프트 구성 (PII 제외)
- [ ] Graceful degradation (요약 실패해도 목록 반환)
- [ ] Controller 연결

### M6. Controller ⬜ 미착수

- [ ] `POST /subscriptions` (구독)
- [ ] `POST /subscriptions/cancel` (해지)
- [ ] `GET /users/{phone}/histories` (이력, M5 후)
- [ ] `@Valid` 형식 검증

### M7. csrng 클라이언트 최종 점검 ⬜ 진행 중

- [ ] `@Retry` + `@CircuitBreaker` 애노테이션 적용 확인
- [ ] fallback 오버로드 2개 (Failed 재전파 / 그 외 unavailable)
- [ ] 배열 응답·빈 본문 방어
- [ ] Resilience4j-Boot3.5 자동설정 동작 확인

### M8. 테스트 ⬜ 미착수

- [ ] 도메인 단위 (전이 규칙 전 조합)
- [ ] 서비스 통합 (흐름, 위반, 롤백)
- [ ] csrng 장애 시뮬레이션
- [ ] Controller MockMvc

## 다음 작업 권장 순서

1. **M6 Controller** — 구독/해지를 노출해 수동 테스트 가능하게
2. **M7 csrng 최종 점검** — 장애 대응 완성
3. **M8 테스트** — 핵심 로직 검증
4. **M5 이력+LLM** — 서드파티, 마지막

## 미해결/확인 필요

- Claude 모델 문자열·요금: docs.anthropic.com에서 현재 값 확인 후 `claude.model` 프로퍼티에.
- Resilience4j 2.3.0 ↔ Spring Boot 3.5.16 자동설정 호환: 실제 서킷브레이커 붙일 때 확인.
- data.sql 수동 ID 지정 ↔ IDENTITY 자동증가 충돌 가능성: 시드 후 신규 저장 시 확인.
- 동시성 처리(M 추가 후보): 낙관적 락 또는 readme 명시.
