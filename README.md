# ARTINUS 백엔드 과제 — 구독 서비스 API

회원이 여러 채널(홈페이지·네이버·콜센터 등)을 통해 구독/해지를 수행하는 백엔드 API.
각 행위는 외부 API(csrng) 응답에 따라 커밋/롤백되며, 구독 이력은 불변 로그로 남는다.
이력 조회 시 LLM(Claude)이 이력을 자연어로 요약해 함께 반환한다.

---

## 1. 기술 스택 & 선택 근거

| 항목 | 선택 | 근거                                                   |
|---|---|------------------------------------------------------|
| 언어/런타임 | **Java 17** | LTS, 과제 요건                                           |
| 프레임워크 | **Spring Boot 3.5.16** (Spring 6.2) | X                                                    |
| 영속성 | **Spring Data JPA + Hibernate** | 도메인 중심 설계 및 보일러플레이트 최소화                              |
| DB | **H2 (in-memory, MySQL 모드)** | 과제 실행 편의. `ddl-auto: create` + `data.sql` 시드         |
| HTTP 클라이언트 | **RestClient** | Spring 6.1+ 동기 클라이언트. RestTemplate 대체, 선언적·타임아웃 제어 용이 |
| 장애 대응 | **Resilience4j 2.3.0** (spring-boot3) | 재시도 + 서킷브레이커를 애노테이션으로 선언                             |
| API 문서 | **springdoc-openapi 2.8.6** | 코드에서 OpenAPI 3 / Swagger UI 자동 생성                    |
| LLM | **Claude (Anthropic) — `claude-sonnet-5`** | 이력 자연어 요약                                            |

> RestClient를 선택한 이유: 
> 이 서비스의 외부 호출은 단순 동기 요청이라 WebClient(리액티브)의 복잡성이 불필요했으며,
> RestTemplate은 유지보수에 더 적합합니다. RestClient는 `ClientHttpRequestFactorySettings`로 connect/read 타임아웃을
> 명시적으로 줄 수 있어 장애 대응(타임아웃) 설계에 적합했습니다.

---

## 2. 실행 방법

### 요구사항
- **JDK 17** (빌드/실행). 시스템 기본이 구버전이면 `JAVA_HOME`을 JDK 17로 지정합니다.
- (선택) Claude API Key — 이력 요약 기능 사용 시. **없어도 앱은 정상 기동**하며, 요약만 대체 문구로 대체됩니다(graceful degradation).

### 환경변수
```bash
export CLAUDE_API_KEY="sk-ant-..."   
```

### 빌드 & 실행
```bash
./gradlew bootRun
# → http://localhost:8080
```

### 주요 엔드포인트
| 용도 | URL |
|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| H2 콘솔 | `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:artinus`) |

### 테스트
```bash
./gradlew test    # 57개 (도메인 단위 / 서비스 / 컨트롤러 / 리포지토리)
```

---

## 3. API 명세

| Method | Path | 설명 |
|---|---|---|
| `POST` | `/api/user/subscribe` | 구독 (최초 가입 / 등급 상승) |
| `POST` | `/api/user/unsubscribe` | 해지 (등급 하향 / 구독 종료) |
| `GET`  | `/api/user/subscribe?phone={phone}` | 이력 조회 + LLM 요약 |

### 요청 예시 — 구독
```http
POST /api/user/subscribe
Content-Type: application/json

{ "phone": "01011112222", "channelId": 1, "target": "BASIC" }
```
성공 시 `200 OK` 을 반환합니다. (본문 x).

### 응답 예시 — 이력 조회
```http
GET /api/user/subscribe?phone=01044440002
```
```json
{
  "history": [
    { "channelName": "홈페이지", "fromStatus": "NONE",    "toStatus": "BASIC",   "changedAt": "2026-07-12T12:24:37" },
    { "channelName": "네이버",   "fromStatus": "BASIC",   "toStatus": "PREMIUM", "changedAt": "2026-07-12T12:24:39" },
    { "channelName": "이메일",   "fromStatus": "PREMIUM", "toStatus": "NONE",    "changedAt": "2026-07-12T12:24:40" }
  ],
  "summary": "2026년 7월 12일, 해당 회원은 홈페이지를 통해 구독하지 않은 상태에서 일반 구독으로 전환한 뒤, 같은 날 네이버를 통해 프리미엄 구독으로 등급을 상향했으나, 이후 이메일을 통해 해지하며 구독하지 않는 상태로 최종 변경되었습니다."
}
```

---

## 4. 도메인 규칙

### 구독 상태 (`UserStatus`)
다음과 같은 상태로 구분됩니다. 회원은 항상 이 셋 중 하나의 상태를 갖습니다.
- NONE (구독 안함)
- BASIC (일반 구독)
- PREMIUM(프리미엄 구독) 

**`NONE`은 null이 아니라 명시적 상태입니다.**

상태에 대해서는 다음과 같은 방향을 가지며 이를 `Map`기반의 자료구조로 로직을 구성했습니다.

| 행위 | 전이 |
|---|---|
| 구독 | `NONE → {BASIC, PREMIUM}`, `BASIC → {PREMIUM}`, `PREMIUM → {}`(불가) |
| 해지 | `PREMIUM → {BASIC, NONE}`, `BASIC → {NONE}`, `NONE → {}`(불가) |
| 최초 가입 | `NONE`에서 시작, `BASIC`/`PREMIUM`으로만 가능 |

### 채널 마스터 데이터 (`data.sql` 시드)
| ID | 이름 | 구독 | 해지 |
|---|---|:---:|:---:|
| 1 | 홈페이지 | ✅ | ✅ |
| 2 | 모바일앱 | ✅ | ✅ |
| 3 | 네이버 | ✅ | ❌ |
| 4 | SKT | ✅ | ❌ |
| 5 | 콜센터 | ❌ | ✅ |
| 6 | 이메일 | ❌ | ✅ |

---

## 5. 아키텍처

### 계층 구조
```
Controller → Service → (Reader / Appender / Manager) → Repository / Entity
                     ↘ Client (csrng, Claude) — 외부 API
```

- **Controller** — 요청 DTO 수신, `@Valid` 형식 검증, Service 호출. 비즈니스 로직 x.
- **Service** — 유스케이스 흐름을 조율하도록 설계했으며 실질적인 흐름은 비즈니스 로직을 전적으로 처리하는 객체에 위임했습니다.
- **Reader / Appender / Manager** — 도메인별 영속성 접근을 행위별로 분리했습니다.
  - Reader: 조회 전담(`@Transactional(readOnly=true)`). "없으면 예외"(`getByXxx`) / "없을 수 있음"(`findByXxx`) 네이밍 구분.
  - Appender: 신규 생성·저장 전담(append-only 성격 — 신규 User, 이력).
  - Manager: 기존 엔티티 상태 변경 조율. **변경 규칙 판단은 엔티티가, Manager는 호출만.**
- **Client** — 외부 API 호출 캡슐화. 통신 장애/비즈니스 실패를 예외로 변환.

### Rich Domain Model — 도메인 로직은 엔티티에 둔다
상태 전이 규칙(구독/해지 가능 여부)은 **엔티티와 enum**이 소유합니다.
- `UserStatus`가 전이 가능표(Map)를 갖고 `canSubscribeTo`/`canCancelTo` 판단하에 로직 수행을 진행했습니다..
- `User`가 `subscribeTo`/`cancelTo`(실행)와 `validateSubscribableTo`/`validateCancellableTo`(검증)를 제공합니다.
- Reader/Appender/Manager와 Service에는 도메인 규칙을 넣지 않았습니다.

---

## 6. 주요 설계 결정

### (1) 채널 능력 — enum이 아닌 boolean 2컬럼
"구독 가능"과 "해지 가능"은 **직교하는 별개 능력**입니다(둘 다 가능/구독만/해지만이 모두 존재).
하나의 enum으로 뭉치면 조합이 늘 때마다 값이 폭증하고 의미가 흐려집니다.
→ `CAN_SUBSCRIBE` / `CAN_CANCEL` 별도 boolean 컬럼으로 표현하고, `validateSubscribable()`/`validateCancellable()`로 검증.

### (2) 이력 모델 — from/to 둘 다 저장, append-only
`SubscriptionHistory`는 `fromStatus`/`toStatus`를 **둘 다** 저장합니다.
"프리미엄 → 일반으로 해지" 같은 전이를 완결적으로 서술하려면 이전 상태가 필요하기 때문입니다.
이력은 **불변 로그**라 수정·삭제가 없다 → Manager 없이 Appender/Reader만 존재합니다.
채널은 User가 아니라 **이력 테이블에 연결**했습니다(채널은 "행위에 딸린 정보"이지 회원의 속성이 아닙니다).
모든 참조·상태가 `nullable = false`.

### (3) 검증(validate)과 실행(전이)의 분리
`subscribeTo()`/`cancelTo()`는 상태를 바꾸는 실행 메서드입니다.
외부 API(csrng) **앞**에서는 상태를 바꾸지 않는 `validateSubscribableTo()`/`validateCancellableTo()`만 호출하고,
실제 전이는 csrng 통과 **후** Manager로 수행합니다 → csrng 실패 시 롤백이 깔끔하고, 이중 전이 버그를 원천 차단.

### (4) 검증 순서 — 비용 측면을 고려해서 외부 API의 흐름을 가장 마지막으로 이관했습니다.
```
채널 능력 검증 → 회원 조회/존재 → 전이 가능성 검증(상태 변경 X) → csrng 호출 → 실제 전이 → 이력 기록
```

---

## 7. 외부 API 장애 대응 (csrng)

`GET https://csrng.net/csrng/csrng.php?min=0&max=1` 응답에 따라 트랜잭션 진행 여부를 결정합니다.
**두 종류의 실패를 반드시 구분합니다.**

| 구분 | 조건 | 예외 | 처리 |
|---|---|---|---|
| **비즈니스 실패** | `random == 0` | `ExternalApiFailedException` | 명세상 롤백 대상. **재시도·서킷 집계 제외.** → `502` |
| **인프라 실패** | 타임아웃 / 5xx / 커넥션 거부 / 빈 응답 | `ExternalApiUnavailableException` | 일시적 장애. **재시도·서킷브레이커 대상.** → `503` |

### 4겹 방어
1. **타임아웃** — RestClient connect/read 2s (`RestClientFactory`)
2. **재시도** — `@Retry(name="csrng")` — 인프라 실패만(3회, 지수 백오프). `random=0`은 재시도 하지 않습니다(GET 멱등이라 재시도 안전).
3. **서킷브레이커** — `@CircuitBreaker(name="csrng")` — 실패율 임계 초과 시 open → fast-fail
4. **트랜잭션 경계 분리** — csrng를 상태 변경 **전에** 호출해 실패 시 롤백이 깔끔하도록 순서를 잡았습니다.

> Resilience4j 애노테이션은 AOP 프록시로 동작하므로 self-invocation을 피해 **Service에서 호출**합니다.
> `fallbackMethod`은 `@Retry`에 두어, 서킷 open 시 `CallNotPermittedException`이 최외곽 Retry에 닿아
> **즉시 실패(fast-fail)**하도록 했습니다(fallback을 `@CircuitBreaker`에 두면 변환된 예외를 Retry가 헛되이 재시도).

---

## 8. LLM 이력 요약 (Claude)

- **모델** — `claude-sonnet-5` (프로퍼티 `claude.model`로 주입, 하드코딩 없음).
- **호출** — Anthropic Messages API(`POST /v1/messages`), read timeout **30s**(LLM은 느림 — csrng의 2s를 쓰면 정상 응답도 타임아웃).
- **응답 파싱** — `content`는 블록 배열이라 `type=="text"` 블록을 **모두 이어붙입니다**.
- **Graceful degradation** — 요약은 부가 기능이라, Claude가 실패해도 이력 목록은 정상 반환하고 요약 자리에만 대체 문구를 넣습니다. (csrng 실패와 다른 점: csrng는 비즈니스 롤백, LLM은 핵심 데이터 보존.)
- **트랜잭션 밖 호출** — 조회는 Reader의 readOnly 트랜잭션으로 끝내고, 느린 LLM 호출이 DB 커넥션을 30초씩 잡지 않게 합니다.

### 개인정보(PII) 취급
외부 LLM에 **전화번호 등 PII를 보내지 않습니다.** 요약에 필요한 건 채널·날짜·상태 전이뿐이므로,
프롬프트는 `- 날짜 / 채널 / 이전상태 → 변경상태` 형식으로만 구성하고 상태는 한글로 변환합니다.
API Key 등 인증 정보는 저장소에 커밋하지 않고 환경변수로 주입합니다.

---

## 9. 예외 체계 & HTTP 상태 매핑

- `ErrorCode` enum이 단일 정보원(HttpStatus + 코드 + 메시지). 도메인별 접두사(U/CH/S/E/C).
- `BusinessException`이 공통 부모(`RuntimeException` 상속 → 트랜잭션 롤백 보장).
- `@RestControllerAdvice` 전역 핸들러가 일관 응답(`{code, message}`)으로 변환.

| 상황 | 코드 | HTTP |
|---|---|---|
| 잘못된 입력 / 형식 검증 실패 / 정의되지 않은 enum | C002 | **400** |
| 구독/해지 불가 채널 | CH002 / CH003 | **400** |
| 회원/채널 없음 | U001 / CH001 | **404** |
| 허용되지 않는 상태 전이 | S001 / S002 | **409** |
| csrng 비즈니스 실패(롤백) | E001 | **502** |
| csrng 인프라 장애 | E002 | **503** |
| 예상 밖 서버 오류 | C001 | **500** |

---

## 10. 테스트 (57개)

| 대상 | 방식 | 커버리지 |
|---|---|---|
| `UserStatus` | 단위(파라미터) | 구독/해지 전이 전 조합, 초기상태 검증 |
| `User` / `Channel` / `SubscriptionHistory` | 단위 | 전이 실행/검증 분리, 채널 능력, 이력 불변식 |
| `UserServiceImpl` | Mockito | 흐름·검증순서, csrng 실패 시 전이/이력 미기록, 요약 graceful degradation, **프롬프트 PII 미포함** |
| `UserRestController` | `@WebMvcTest` | 형식 검증·에러 상태코드 매핑 |
| `SubscriptionHistory` 조회 | `@DataJpaTest` | fetch join · createdAt asc · 회원 필터 |

---

## 11. 동시성 (낙관적 락)

같은 회원이 동시에 요청을 보내면(더블클릭·재시도 등) `from` 상태를 읽는 시점과 반영 시점 사이에 경합이 생깁니다.
DB 기본 격리수준(READ_COMMITTED)에서는 두 트랜잭션이 같은 행을 각자 읽고 각자 갱신하는 것(lost update)을 막지 못해,
**이력이 중복 기록되거나(유령 전이) 상태가 덮어써질(lost update)** 수 있습니다.

**대응 — `User`에 `@Version` 낙관적 락 적용.**
- 커밋 시 `UPDATE ... WHERE id=? AND version=?`로 검증하고, 다른 트랜잭션이 먼저 version을 올렸으면 충돌 → **진 트랜잭션 전체 롤백**(전이·이력 기록이 통째로 취소되어 유령 이력·lost update가 남지 않음).
- 충돌(`OptimisticLockingFailureException`)은 **`409`(U002)** 로 매핑 → 클라이언트가 재시도.
- 최초 가입 동시 요청(회원 행이 없어 버전으로 못 막는 구간)은 **`phone` unique 제약**이 막고, 이때의 `DataIntegrityViolationException`도 **`409`(U002)** 로 매핑.

**낙관적 락에 대한 사용 근거**
csrng 외부 호출이 트랜잭션 **안**에 있어, 비관적 락(`SELECT ... FOR UPDATE`)을 쓰면 행 잠금을 네트워크 호출(최대 2s + 재시도) 내내 붙잡게 된다.
낙관적 락은 락을 잡지 않아 이 구조에 적합하고, 경합이 드문 도메인 특성과도 맞는다.

---

## 12. AI 활용

- **기능** — 구독 이력 조회 시 Claude(`claude-sonnet-5`)가 이력을 자연어 한 문단으로 요약하며 PII는 전송하지 않습니다.
- **개발** — 본 저장소는 Claude Code를 활용해 설계·구현·테스트를 진행했습니다.

---

## 13. 패키지 구조
```
io.github.ingkoon.artinus
├── common
│   ├── config        (JpaAuditingConfig, OpenApiConfig)
│   ├── entity        (CreatedOnlyEntity → BaseEntity)
│   ├── exception     (ErrorCode, BusinessException, GlobalExceptionHandler)
│   └── client        (RestClientFactory, csrng·claude 클라이언트/설정/예외)
├── user
│   ├── domain        (User)
│   ├── enums         (UserStatus)
│   ├── exception     (InvalidStatusTransitionException 등)
│   ├── service       (UserService(Impl), reader/appender/manager/param/result)
│   └── controller    (UserRestController, request)
├── channel
│   ├── domain        (Channel)
│   └── service.reader
└── subscriptionhistory
    ├── domain        (SubscriptionHistory)
    └── service       (appender/reader)
```
