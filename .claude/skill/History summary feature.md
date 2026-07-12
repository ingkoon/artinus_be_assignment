# 기능 개발 가이드: 구독 이력 조회 + LLM 요약

> 명세 요구사항 3번. 현재 미구현 상태(`UserServiceImpl.getHistory()`가 `return null`). 외부 API(Claude) 의존이라 **가장 마지막에 진행**한다.

## 목표

휴대폰번호로 회원의 구독/해지 이력을 시간순 조회하고, 그 이력을 Claude에 넘겨 자연어 요약을 함께 반환한다.

응답 형태:
```json
{
  "history": [ { "channelName": "...", "fromStatus": "...", "toStatus": "...", "changedAt": "..." } ],
  "summary": "2026년 1월 1일 홈페이지를 통해 일반 구독으로 가입한 뒤, ..."
}
```

## 구현 순서

### 1. SubscriptionHistoryReader — 시간순 조회

- 이력을 `createdAt` **오름차순**으로 조회(요약이 시간 흐름대로 나오도록).
- User·Channel을 함께 쓰므로 **fetch join으로 N+1 회피.**
- `@Transactional(readOnly = true)`.

```java
@Query("""
    select h from SubscriptionHistory h
    join fetch h.channel c
    join fetch h.user u
    where u.phone = :phone
    order by h.createdAt asc
    """)
List<SubscriptionHistory> findAllByUserPhoneOrderByCreatedAtAsc(String phone);
```

### 2. 응답 DTO (record)

- `HistoryItem`: channelName, fromStatus, toStatus, changedAt. `from(SubscriptionHistory)` 정적 변환 메서드.
- `ReadHistorySummaryResult`: `List<HistoryItem> history` + `String summary`.

### 3. ClaudeClient — 요약 생성

**설정 (ClaudeClientConfig)**
- RestClientFactory 재사용. baseUrl `https://api.anthropic.com`.
- 타임아웃: connect 3s, **read 30s** (LLM은 느림 — csrng의 2s를 쓰면 정상 응답도 타임아웃).
- 헤더: `x-api-key`(환경변수 주입), `anthropic-version: 2023-06-01`, `content-type: application/json`.
- 모델 문자열/요금은 **docs.anthropic.com에서 현재 값 확인 후** 프로퍼티(`claude.model`)로 주입. 코드에 하드코딩 금지.

**요청/응답 DTO**
- 요청: `model`, `max_tokens`(예 1024), `messages: [{role, content}]`. `@JsonProperty("max_tokens")` 주의(snake_case).
- 응답: `content`는 **블록 배열**. `type == "text"`인 블록을 **모두 이어붙인다**(첫 블록만 꺼내지 말 것).

**본체**
- 통신 장애(`ResourceAccessException`)·HTTP 오류(`RestClientResponseException`) → `ExternalApiUnavailableException`으로 변환.
- 응답이 null이거나 content가 비면 → `ExternalApiUnavailableException`.

### 4. 프롬프트 구성

- 이력을 `- 날짜 / 채널 / 이전상태 → 변경상태` 형식으로 나열.
- "시간 순서대로 자연스러운 한국어 한 문단으로 요약" 지시.
- **PII 금지: 전화번호를 프롬프트에 넣지 않는다.** 채널·날짜·상태만.
- 상태는 한글로 변환(NONE→구독 안함, BASIC→일반 구독, PREMIUM→프리미엄 구독).

### 5. 서비스 조립 — Graceful Degradation

**핵심 설계 판단:** 요약 실패가 이력 조회 전체를 실패시키면 안 된다. 이력 목록은 이미 DB에서 확보했으므로, Claude가 죽어도 목록은 정상 반환하고 요약 자리에만 대체 문구를 넣는다.

```java
private String summarizeSafely(List<HistoryItem> items) {
    if (items.isEmpty()) return "구독 이력이 없습니다.";
    try {
        return claudeClient.summarize(buildPrompt(items));
    } catch (ExternalApiUnavailableException e) {
        log.warn("이력 요약 생성 실패, 요약 없이 반환", e);
        return "요약을 생성하지 못했습니다.";
    }
}
```

- csrng와 다른 점: csrng 실패는 비즈니스 롤백이지만, LLM 요약은 부가 기능이라 실패해도 핵심 데이터는 살린다.
- **Claude 호출은 트랜잭션 밖에서.** 조회는 Reader의 readOnly 트랜잭션으로 끝내고, LLM 호출이 DB 커넥션을 30초씩 잡지 않게 한다.
- `getHistory()` 시그니처를 `ReadHistoryParam`(phone 포함) 받도록 변경.

## 체크리스트

- [ ] Reader fetch join, createdAt asc
- [ ] HistoryItem / ReadHistorySummaryResult record
- [ ] ClaudeClientConfig (read timeout 30s, 헤더)
- [ ] 모델명은 docs 확인 후 프로퍼티 주입
- [ ] content 블록 배열 전체 이어붙이기
- [ ] 프롬프트에 PII 미포함
- [ ] 요약 실패 시 graceful degradation
- [ ] Claude 호출은 트랜잭션 밖
- [ ] Controller `GET /users/{phone}/histories` 연결