-- 마스터 데이터 시드 (H2, MySQL 모드). ddl-auto: create 로 스키마 생성 후 실행됨.
-- 감사 필드(created_at/updated_at)는 nullable 이므로 생략(NULL). data.sql 은 JPA 를 우회하므로 Auditing 리스너가 동작하지 않는다.

-- 채널 마스터 데이터: 구독/해지 능력은 직교하는 두 boolean 컬럼.
--   홈페이지·모바일앱 : 구독/해지 모두 가능
--   네이버·SKT       : 구독만 가능
--   콜센터·이메일     : 해지만 가능
INSERT INTO CHANNEL (ID, NAME, CAN_SUBSCRIBE, CAN_CANCEL) VALUES (1, '홈페이지',   TRUE,  TRUE);
INSERT INTO CHANNEL (ID, NAME, CAN_SUBSCRIBE, CAN_CANCEL) VALUES (2, '모바일앱',   TRUE,  TRUE);
INSERT INTO CHANNEL (ID, NAME, CAN_SUBSCRIBE, CAN_CANCEL) VALUES (3, '네이버',     TRUE,  FALSE);
INSERT INTO CHANNEL (ID, NAME, CAN_SUBSCRIBE, CAN_CANCEL) VALUES (4, 'SKT',       TRUE,  FALSE);
INSERT INTO CHANNEL (ID, NAME, CAN_SUBSCRIBE, CAN_CANCEL) VALUES (5, '콜센터',     FALSE, TRUE);
INSERT INTO CHANNEL (ID, NAME, CAN_SUBSCRIBE, CAN_CANCEL) VALUES (6, '이메일',     FALSE, TRUE);

-- 테스트 유저: 해지/등급전이 시나리오를 바로 시험할 수 있도록 이미 구독 중인 상태로 시드.
--   NONE 유저는 최초 구독 시 런타임에 생성되므로 시드하지 않는다.
-- VERSION은 낙관적 락 초기값(0). 시드 행에 명시하지 않으면 NULL이 되어 갱신 시 불안정.
INSERT INTO USERS (ID, PHONE, STATUS, VERSION) VALUES (1, '01011112222', 'BASIC',   0);
INSERT INTO USERS (ID, PHONE, STATUS, VERSION) VALUES (2, '01033334444', 'PREMIUM', 0);

-- 수동 지정한 ID 와 IDENTITY 자동증가 충돌 방지:
-- 구독 시 신규 유저가 IDENTITY 로 저장되므로, 다음 값을 시드 ID 이후로 재시작한다.
ALTER TABLE USERS ALTER COLUMN ID RESTART WITH 100;
