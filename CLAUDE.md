# CLAUDE.md

ARTINUS 백엔드 과제 — 구독 서비스 API. 이 문서는 Claude Code가 이 저장소에서 작업할 때 따라야 할 코드 컨벤션과 설계 지침을 정의한다.

## 프로젝트 개요

구독 서비스 백엔드 API. 회원은 여러 채널(홈페이지, 네이버, 콜센터 등)을 통해 구독/해지를 수행하며, 각 행위는 외부 API(csrng) 응답에 따라 커밋/롤백된다. 구독 이력은 불변 로그로 남고, 이력 조회 시 LLM(Claude)이 자연어 요약을 생성한다.

- 언어/런타임: Java 17
- 프레임워크: Spring Boot 3.5.x (Spring Framework 6.2 계열). **4.x로 올리지 말 것** — API 호환성 문제로 3.5로 확정했다.
- 영속성: Spring Data JPA + Hibernate, H2 (in-memory, MySQL 모드)
- 장애 대응: Resilience4j (resilience4j-spring-boot3)
- HTTP 클라이언트: RestClient (RestTemplate 아님)

## 아키텍처 원칙

### 계층 구조

```
Controller → Service → (Reader / Appender / Manager) → Repository / Entity
                     ↘ Client (csrng, Claude) — 외부 API
```

- **Controller**: 요청 DTO 수신, `@Valid` 형식 검증, Service 호출. 비즈니스 로직 없음.
- **Service**: 유스케이스 흐름 조율(orchestration)만. 도메인 규칙을 직접 판단하지 않고 엔티티·하위 컴포넌트에 위임한다. 트랜잭션 경계를 소유한다.
- **Reader / Appender / Manager**: 도메인별 영속성 접근을 행위별로 분리한 컴포넌트.
    - **Reader**: 조회 전담. "없으면 예외"(`getByXxx`)와 "없을 수 있음"(`findByXxx`)을 네이밍으로 구분. `@Transactional(readOnly = true)`.
    - **Appender**: 신규 엔티티 생성·저장 전담. append-only 성격(신규 User, 이력).
    - **Manager**: 기존 엔티티의 상태 변경 조율 전담. **변경 규칙 판단은 엔티티가 하고, Manager는 호출만 한다.**
- **Client**: 외부 API 호출 캡슐화. 통신 장애/비즈니스 실패를 예외로 변환.

### 핵심 규칙: 도메인 로직은 엔티티에 둔다 (Rich Domain Model)

- 상태 전이 규칙(구독/해지 가능 여부)은 **엔티티와 enum**이 소유한다. Service나 Reader/Appender/Manager로 옮기지 않는다.
- Reader/Appender/Manager는 "영속성 관점의 행위"만 담당한다. 도메인 규칙을 넣지 말 것.
- **조회에 검증을 섞지 않는다.** 예: `ChannelReader.getById()`에 "구독 가능한가" 검증을 넣지 말 것. 조회는 조회만, 검증은 엔티티+Service가 순서대로 엮는다. (단 "존재하지 않으면 예외"는 조회의 일부이므로 Reader에 둬도 됨.)

## 도메인 규칙 (명세 기준)

### 구독 상태 (UserStatus)

`NONE`(구독 안함), `BASIC`(일반 구독), `PREMIUM`(프리미엄 구독). 회원은 항상 이 셋 중 하나. **`NONE`은 null이 아니라 명시적 상태다.** 상태를 null로 표현하지 말 것.

- 구독 전이: `NONE → {BASIC, PREMIUM}`, `BASIC → {PREMIUM}`, `PREMIUM → {}`(불가)
- 해지 전이: `PREMIUM → {BASIC, NONE}`, `BASIC → {NONE}`, `NONE → {}`(불가)
- 최초 가입: `NONE`에서 시작하며 `BASIC` 또는 `PREMIUM`으로만 가입 가능. `NONE`으로는 가입 불가.

### 채널 (Channel)

구독/해지 창구. 두 능력을 **별도 boolean 컬럼**으로 표현한다 (enum으로 뭉치지 말 것 — 두 능력은 직교하는 개념).

- 필드: `subscribable` / `cancellable` (getter: `isSubscribable()` / `isCancellable()`)
- 컬럼: `CAN_SUBSCRIBE` / `CAN_CANCEL`
- 검증 메서드: `validateSubscribable()` / `validateCancellable()` (위반 시 예외). 질의용 `isSubscribable()`도 함께 유지.
- 마스터 데이터 (data.sql로 시드): 홈페이지·모바일앱(둘 다), 네이버·SKT(구독만), 콜센터·이메일(해지만)

### 구독 이력 (SubscriptionHistory)

**불변 append-only 로그.** User·Channel을 각각 `@ManyToOne`으로 참조. `fromStatus`/`toStatus`를 **둘 다** 저장(해지 전이 서술을 위해). 수정·삭제 없음 → Manager 없이 Appender/Reader만 존재.

- 모든 참조·상태가 `nullable = false`. 채널·from·to 어디에도 null이 들어갈 자리가 없다.
- 생성 시각(`createdAt`)이 곧 구독/해지 날짜.

## 트랜잭션 규칙

- **트랜잭션 경계는 Service(유스케이스)에 둔다.** Appender/Manager에 독립 트랜잭션을 걸지 말 것 — 유스케이스 원자성이 깨진다(일부만 커밋되는 사고).
- Reader에는 `@Transactional(readOnly = true)`.
- Spring의 `org.springframework.transaction.annotation.Transactional`을 쓴다 (`jakarta.transaction.Transactional` 아님).
- **외부 API 호출을 트랜잭션과 어떻게 엮을지 주의.** LLM 등 느린 호출은 트랜잭션 밖에서. csrng는 상태 변경 전에 호출해 실패 시 롤백이 깔끔하도록 순서를 잡는다.

## 외부 API 장애 대응 (csrng)

**두 종류의 실패를 반드시 구분한다:**

- **비즈니스 실패** (`random=0`): API는 정상 작동, 명세상 롤백 대상. `ExternalApiFailedException`. **재시도·서킷 집계 제외.**
- **인프라 실패** (타임아웃/5xx/커넥션): API가 작동 못 함. `ExternalApiUnavailableException`. **재시도·서킷브레이커 대상.**

대응은 4겹: 타임아웃(RestClient) → 재시도(Resilience4j Retry) → 서킷브레이커(Resilience4j CircuitBreaker) → 트랜잭션 경계 분리. 두 예외의 구분이 "무엇을 재시도/롤백할지"를 결정한다.

- Resilience4j 애노테이션(`@CircuitBreaker`, `@Retry`)은 AOP 프록시로 동작 → **self-invocation 금지.** 반드시 외부 빈에서 호출.
- csrng는 GET(멱등)이라 재시도 안전.

## 검증 순서 원칙

싼 검증을 먼저, 비싼 외부 호출을 나중에. 구독/해지 서비스에서:
채널 능력 검증 → 회원 조회/존재 → 전이 가능성 검증(상태 변경 X) → **csrng 호출** → 실제 상태 전이 → 이력 기록.

**검증(validate)과 실행(전이)을 분리한다.** `subscribeTo()`/`cancelTo()`는 상태를 바꾸는 실행 메서드다. csrng **앞**에서는 상태를 바꾸지 않는 `validateSubscribableTo()`/`validateCancellableTo()`를 쓰고, 실제 전이는 csrng 통과 후 Manager로 수행한다. (이중 전이 버그 주의.)

## 예외 체계

- `ErrorCode` enum이 단일 정보원 (HttpStatus + 코드 + 메시지). 도메인별 접두사(U/CH/S/E).
- `BusinessException`이 공통 부모 (`RuntimeException` 상속 → 트랜잭션 롤백 보장).
- 개별 예외 클래스는 "자주 쓰이거나 생성 인자가 있는 것"에만. 나머지는 `throw new BusinessException(ErrorCode.XXX)`로 직접.
- `@RestControllerAdvice` 전역 핸들러가 `BusinessException`과 예상 밖 `Exception`을 일관 응답으로 변환.
- HTTP 매핑: 리소스 없음 404, 잘못된 입력 400, 상태 충돌 409, 외부 API 장애 502/503.

## 코딩 컨벤션

### 엔티티

- `@NoArgsConstructor(access = PROTECTED)` — JPA용 기본 생성자만 열고, 실제 생성자는 private.
- 생성은 **정적 팩토리**(`create` / `register`)로 일원화하고, 그 안에서 불변식을 검증한다. 생성자를 public으로 열지 말 것.
- enum 필드는 반드시 `@Enumerated(EnumType.STRING)`. ordinal 금지(순서 변경 시 데이터 깨짐).
- boolean 필드명에 `is` 접두사를 붙이지 않는다 (getter에서 `is`가 붙음). `subscribable` (O) / `isSubscribable` (X).
- `@GeneratedValue`는 `IDENTITY` 사용 (AUTO 금지 — 벤더별 전략이 달라짐).

### 감사 필드 (BaseEntity)

- 상속 체인으로 분리: `CreatedOnlyEntity`(createdAt만) → `BaseEntity`(updatedAt 추가). 다단계 `@MappedSuperclass`.
- 이력 엔티티는 `CreatedOnlyEntity`를, User·Channel은 `BaseEntity`를 상속. 이력의 불변성을 상속 구조로 표현.
- 필드명은 `createdAt`/`updatedAt` (오타 `createAt` 금지).

### DTO

- 계층별 분리: `XxxRequest`(표현 계층, `@RequestBody`) → `XxxParam`(서비스 계층). Request에 `toServiceParam()` 변환 메서드.
- **record 사용** (불변, getter/생성자 자동). 접근자는 `phone()` 형태.
- Request에 Bean Validation(`@NotBlank`, `@NotNull`) → 형식 검증은 표현 계층에서 종료.
- `@RequestBody` JSON의 enum은 Jackson이 자동 변환. 잘못된 값 방어를 위해 `@JsonCreator` 또는 전역 핸들러에서 `HttpMessageNotReadableException` 처리.

### 생성 패턴

- 빌더는 "선택적 필드가 많을 때"만. **필드가 적고 전부 필수면 생성자 또는 정적 팩토리.** 빌더는 필수값 누락을 컴파일 타임에 못 막으므로 이력처럼 전 필드 필수인 엔티티엔 부적합.

### 포맷팅

- Lombok: `@Getter`, `@RequiredArgsConstructor` 적극 사용. 컴포넌트는 생성자 주입(`@RequiredArgsConstructor` + `final` 필드).
- 서비스 메서드는 "소리 내어 읽으면 비즈니스 시나리오가 되도록" — 조율 흐름만 담고 규칙은 위임.

## 개인정보 (PII)

- 외부 LLM(Claude)에 전화번호 등 PII를 보내지 않는다. 요약에 필요한 것은 채널·날짜·상태 전이뿐.
- API Key 등 인증 정보는 저장소에 커밋 금지. 환경변수로 주입(`${CLAUDE_API_KEY:}`).

## 패키지 구조

```
io.github.ingkoon.artinus
├── common
│   ├── entity        (BaseEntity, CreatedOnlyEntity)
│   ├── exception     (ErrorCode, BusinessException, GlobalExceptionHandler, 외부 API 예외)
│   └── client        (RestClientFactory, csrng·Claude 클라이언트)
├── user
│   ├── domain        (User)
│   ├── enums         (UserStatus)
│   ├── exception     (InvalidStatusTransitionException 등)
│   ├── service       (UserService, UserServiceImpl, reader/appender/manager/param/result)
│   └── controller
├── channel
│   ├── domain        (Channel)
│   └── service.reader (ChannelReader)
└── subscriptionhistory
    ├── domain        (SubscriptionHistory)
    └── service.appender / reader
```

## 하지 말 것 (안티패턴)

- Spring Boot를 4.x로 올리지 말 것.
- 상태를 null로 표현하지 말 것 (`NONE` 사용).
- 채널 능력을 enum으로 뭉치지 말 것 (별도 boolean 컬럼).
- Reader/Appender/Manager에 도메인 규칙을 넣지 말 것.
- Appender/Manager에 독립 트랜잭션을 걸지 말 것.
- csrng의 `random=0`을 재시도하지 말 것.
- 조회 메서드에 비즈니스 검증을 섞지 말 것.
- 이력 엔티티를 수정·삭제하지 말 것 (append-only).
- 엔티티 생성자를 public으로 열지 말 것 (정적 팩토리 사용).