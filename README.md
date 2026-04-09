# Dfinery Backend 과제

## 개요

이벤트 데이터를 수집하고 비동기로 처리하는 파이프라인을 구현하는 과제입니다.

프로젝트는 멀티 모듈 구조로 구성되어 있으며, `data-producer`가 생성한 이벤트 데이터를 `data-ingestion`의 API로 전송합니다.
면접자는 `data-ingestion` 모듈의 수집 API에 비동기 처리 파이프라인을 구현해야 합니다.

---

## 프로젝트 구조

```
backend-test/
├── app/
│   ├── data-ingestion/          # 데이터 수집 서버 (port 8080) ← 구현 대상
│   │   └── controller/
│   │       └── DataIngestionController.kt
│   └── data-producer/           # 데이터 생성 & 전송 봇 (port 8081)
│       ├── controller/          # Push API 엔드포인트
│       ├── service/             # 병렬 전송 로직
│       └── client/              # data-ingestion API 호출 클라이언트
├── model/                       # 공유 데이터 클래스 (DTO, Event 등)
│   ├── ingestion/               # DataIngestionRequestDTO, DataIngestionResponseDTO
│   ├── producer/                # PushRequestDTO, PushResponseDTO
│   └── event/                   # Event
└── library/
    └── generator/               # 테스트 이벤트 데이터 생성기
```

---

## 기술 스택

- Kotlin 2.0
- Spring Boot 3.1.4
- Spring WebFlux (WebClient)
- Kotlin Coroutines
- Gradle 8.2 (멀티 모듈)
- Java 17

---

## 실행 방법

### 1. 빌드

```bash
./gradlew clean build -x test
```

### 2. data-ingestion 서버 실행

```bash
./gradlew :app:data-ingestion:bootRun
```

서버가 `http://localhost:8080`에서 시작됩니다.

### 3. data-producer 서버 실행 (별도 터미널)

```bash
./gradlew :app:data-producer:bootRun
```

서버가 `http://localhost:8081`에서 시작됩니다.

### 4. 데이터 전송

```bash
curl -X POST http://localhost:8081/push \
  -H "Content-Type: application/json" \
  -d '{
    "totalRequests": 100,
    "concurrency": 10,
    "eventsPerRequest": 5
  }'
```

| 파라미터 | 설명 |
|---|---|
| `totalRequests` | 총 API 요청 수 |
| `concurrency` | 동시 코루틴 워커 수 |
| `eventsPerRequest` | 요청당 이벤트 수 |

위 예시는 10개의 코루틴 워커가 병렬로 총 100번의 요청을 보내며, 각 요청에 5개의 이벤트가 포함됩니다. (총 500개 이벤트)

---

## 과제 내용

`DataIngestionController.kt`의 `collect()` 메서드에 비동기 처리 파이프라인을 구현하세요.

### 수집 API

- Endpoint: `POST /api/v1/collect`
- Request Body: `DataIngestionRequestDTO`

```json
{
  "common": {
    "service_id": "test-svc",
    "user_id": "uuid",
    "device_id": "uuid"
  },
  "events": [
    {
      "event_log_id": "uuid:timestamp",
      "event_name": "df_purchase",
      "event_datetime": "2026-03-20T08:00:00Z",
      "event_properties": {
        "df_order_id": "uuid",
        "df_total_purchase_amount": 50000.0,
        "df_payment_method": "Card"
      }
    }
  ]
}
```

### Required

- 수신된 데이터를 비동기 처리 파이프라인으로 전달
- 데이터 규칙에 의거한 Validation 처리
- Validation 실패 데이터에 대한 처리 설계
- 최대 2,000개 row 단위로 JSON 파일 로컬 적재

### Optional

- 병렬 처리
- Backpressure 처리
- 처리량 최적화

---

## 이벤트 종류

data-producer가 생성하는 이벤트 목록:

| 이벤트명 | 설명 |
|---|---|
| `df_start_session` | 세션 시작 |
| `df_end_session` | 세션 종료 |
| `df_login` | 로그인 |
| `df_logout` | 로그아웃 |
| `df_purchase` | 구매 (event_properties 포함) |
| `df_view_product` | 상품 조회 |
| `df_add_to_cart` | 장바구니 추가 |
| `df_search` | 검색 |
| `df_sign_up` | 회원가입 |
| `df_add_payment_info` | 결제 정보 추가 |

---

## 참고사항

- `data-producer`는 수정하지 않아도 됩니다.
- `library/generator` 모듈은 테스트용 이벤트 데이터 생성기로 자유롭게 수정 가능합니다.
- `model` 모듈에 필요한 데이터 클래스를 추가할 수 있습니다.
- `data-ingestion` 모듈에 자유롭게 패키지와 클래스를 추가하세요.
