# 개발 과정 정리

작성일: 2026-04-10

## 1. 목표 이해

과제의 핵심 목표를 아래처럼 정리하고 작업을 시작했다.

- `data-producer`가 생성한 요청을 `data-ingestion`이 즉시 수신한다.
- HTTP 요청은 빠르게 `200 OK`를 반환하고, 실제 처리는 비동기로 분리한다.
- 수집된 데이터는 Validation Rule에 따라 검증한다.
- 처리 결과는 배치 단위로 적재한다.
- 필요하다면 Kafka 같은 외부 메시지 큐를 도입해 구조를 분리한다.
- 구현뿐 아니라, 문제를 어떻게 분석하고 AI와 협업했는지가 평가 포인트다.

## 2. 초기 점검

처음에는 실행 환경과 빌드 가능 여부부터 확인했다.

- Gradle wrapper를 설치/정비한 뒤 전체 빌드를 확인했다.
- 멀티 모듈 구조와 각 애플리케이션의 역할을 분리해서 파악했다.
- `README.md`, `ValidationRule.md`, 기존 소스 구조를 먼저 읽고 작업 범위를 정리했다.

이 단계에서 "기능 추가"보다 "실행 가능한 개발 환경 확보"를 우선순위로 두었다.

## 3. Kafka 실행 환경 정리

`docker compose up` 과정에서 Kafka 이미지 관련 문제가 먼저 발생했다.

### 문제

- 기존 `bitnami/kafka:3.7` 이미지가 존재하지 않아 컨테이너가 올라오지 않았다.

### 원인 판단

- 태그 자체가 잘못되었거나 더 이상 제공되지 않는 상태였다.
- 로컬 Docker Desktop 이슈와 별개로, 이미지 참조 자체가 유효하지 않았다.

### 조치

- `compose.yaml`에서 Kafka 이미지를 `apache/kafka:3.7.2`로 변경했다.
- KRaft 기준으로 리스너 및 브로커 설정을 다시 맞췄다.
- `kafka-init` 컨테이너가 기동 후 토픽을 만들도록 구성했다.

### 결과

- 로컬에서 Kafka 브로커와 초기화 컨테이너가 정상 기동되는 상태로 정리했다.
- 토픽 `ingestion.events.v1` 생성까지 확인했다.

## 4. data-ingestion 비동기 파이프라인 정리

과제 요구사항상 `data-ingestion`은 요청을 받은 뒤 오래 붙잡고 있으면 안 됐다.

### 기존 문제

- 컨트롤러가 Kafka 전송 완료 시점까지 기다리는 구조에 가까웠다.
- 이 구조는 "즉시 200 응답" 요구사항과 충돌할 가능성이 높았다.

### 조치

- `data-ingestion`에 bounded `ThreadPoolTaskExecutor` 기반 비동기 handoff 구조를 추가했다.
- 컨트롤러는 내부 큐에 작업 등록이 성공하면 빠르게 응답하도록 변경했다.
- 실제 Kafka publish는 별도 스레드에서 수행하도록 분리했다.

### 의도

- API 응답 지연을 줄이고,
- ingestion API를 "수집 진입점" 역할에 집중시키고,
- 이후 처리량 문제는 큐 크기와 executor 설정으로 조절할 수 있게 만들었다.

## 5. data-processor 신규 구성 및 실행 오류 해결

이후에는 수집 후처리를 담당할 `data-processor` 쪽을 본격적으로 정리했다.

### 실행 오류 1: `ObjectMapper` Bean 없음

#### 문제

- `JsonlBatchWriter` 생성자 주입 과정에서 `ObjectMapper` Bean을 찾지 못해 애플리케이션이 뜨지 않았다.

#### 조치

- `app/data-processor/build.gradle.kts`에 `spring-boot-starter-json`을 추가했다.

#### 결과

- Jackson 기반 Bean 구성이 정상화되었고 애플리케이션 실행 오류를 제거했다.

### 실행 오류 2: 로깅 충돌

#### 문제

- Parquet/Hadoop 계열 의존성 추가 후 `slf4j-reload4j`와 Spring Boot 기본 로깅(Logback)이 충돌했다.

#### 조치

- `data-processor` 의존성에서 `org.slf4j:slf4j-reload4j`를 제외했다.

#### 결과

- 로깅 충돌 없이 `data-processor`를 실행할 수 있게 정리했다.

## 6. 적재 포맷을 JSONL에서 Parquet로 전환

적재 포맷은 JSON보다 Parquet가 이후 분석/압축/컬럼 지향 처리 측면에서 더 적합하다고 판단했다.

### 변경 이유

- 파일 크기와 스캔 비용을 줄이기 쉽다.
- 추후 S3 적재 및 데이터 레이크 확장에 유리하다.
- "로컬 파일 적재 -> 이후 S3 연동" 경로와도 잘 맞는다.

### 조치

- 기존 JSONL writer를 제거하고 Parquet writer로 교체했다.
- `parquet-avro` 기반으로 `.parquet` 파일을 생성하도록 구현했다.
- Windows 환경에서 Hadoop 의존 문제를 피하기 위해 NIO 기반 `InputFile`/`OutputFile` 어댑터를 직접 구성했다.

### 결과

- 로컬 디스크에 Parquet 파일을 직접 생성하는 구조로 변경했다.
- 추후 저장소를 S3로 바꿀 때도 writer/저장 계층을 확장하기 쉬운 방향으로 정리했다.

## 7. 배치 전 메모리 유실 위험 보완

사용자 관점에서 가장 중요한 질문은 이 부분이었다.

> "2000개가 다 차기 전에 프로세스가 종료되면 메모리에 들고 있던 정보들이 유실되는 것 아닌가?"

이 지적은 맞다. 단순 메모리 버퍼 방식은 배치 전 장애 시 유실 위험이 있다.

### 기존 위험

- 2,000건 단위로만 파일을 쓰면,
- 배치가 차기 전 프로세스 종료 시 메모리 내 결과가 사라질 수 있다.

### 조치

- Kafka consumer는 메시지를 읽은 뒤 바로 메모리 배치에만 올리지 않고,
- 먼저 로컬 spool 파일에 결과를 영속화하도록 변경했다.
- spool 저장이 끝난 뒤에만 Kafka ack를 수행하도록 바꿨다.
- 별도의 scheduled worker가 spool 파일을 읽어 Parquet 배치 파일로 병합/적재하게 구성했다.

### 현재 처리 흐름

1. Kafka에서 메시지 consume
2. Validation 수행
3. valid/invalid 결과를 `processor-spool` 아래 로컬 파일로 먼저 저장
4. 저장 성공 후 Kafka ack
5. 스케줄러가 spool을 읽어 `processor-output` 아래 Parquet 파일로 적재

### 기대 효과

- 프로세스 비정상 종료 시에도 이미 spool에 기록된 데이터는 남는다.
- 재기동 후 spool worker가 다시 읽어 적재를 이어갈 수 있다.
- "배치 효율"과 "장애 시 복구 가능성"을 동시에 챙길 수 있다.

## 8. 실제 확인하면서 정리한 오해/이슈

### 1) Kafka 메시지 수와 이벤트 수는 다르다

`README.md`의 예시 요청:

```bash
curl -X POST http://localhost:8081/push \
  -H "Content-Type: application/json" \
  -d '{
    "totalRequests": 100,
    "concurrency": 10,
    "eventsPerRequest": 5
  }'
```

이 경우 총 이벤트 수는 `100 x 5 = 500`개다.

하지만 Kafka에는 요청 DTO 단위로 publish하고 있으므로,

- Kafka 메시지 수: 100개
- 메시지 내부 이벤트 총합: 500개

로 해석하는 것이 맞다.

즉, "Kafka 메시지 500개"가 아니라 "Kafka 메시지 100개 안에 이벤트 500개" 구조다.

### 2) `.jsonl` 파일이 보였던 이유

- 코드상 Parquet로 전환한 뒤에도 `.jsonl`이 계속 생성되는 현상이 있었다.
- 원인을 확인해보니, 예전에 IntelliJ에서 띄워둔 구 버전 `data-processor` 프로세스가 살아 있었다.
- 즉, 새 코드가 아니라 이전 실행 프로세스가 계속 파일을 만들고 있었다.

이 경험을 통해 "코드 변경 확인"뿐 아니라 "실제로 어떤 프로세스가 떠 있는지"를 같이 봐야 한다는 점을 다시 확인했다.

## 9. 검증 방식

이번 작업에서는 아래처럼 검증했다.

- `.\gradlew build`로 전체 빌드 확인
- `.\gradlew :app:data-processor:test :app:data-processor:build`로 모듈 단위 확인
- Docker Compose로 Kafka 및 초기화 컨테이너 실행 확인
- 수동 API 호출로 ingestion/producer 동작 확인
- 로컬 `processor-spool`, `processor-output` 디렉터리 생성 여부 확인
- Parquet writer 및 spool store에 대해 테스트 코드 추가

## 10. AI 활용 방식

이번 과제에서 AI는 단순 코드 생성기가 아니라, 아래 역할로 활용했다.

- 에러 메시지 기반 원인 가설 정리
- Docker/Kafka 설정 문제를 빠르게 비교 검토
- Spring Boot Bean 구성 오류 원인 추적
- Parquet 전환 시 필요한 라이브러리/구조 탐색
- 메모리 유실 시나리오를 더 안전한 구조로 재설계
- 변경 후 테스트 포인트와 검증 순서를 정리

중요했던 점은 "AI가 제안한 내용을 그대로 반영"한 것이 아니라,

- 현재 과제 요구사항과 맞는지,
- 실제 로컬 환경에서 실행 가능한지,
- 장애 시 어떤 문제가 생길지를 기준으로 계속 걸러서 적용한 것이다.

## 11. 최종 정리

현재까지의 개발 과정은 단순 기능 추가보다 아래 흐름에 가까웠다.

- 실행 환경을 먼저 안정화하고,
- 요구사항과 다를 수 있는 처리 흐름을 수정하고,
- 적재 포맷을 목적에 맞게 재선정하고,
- 장애 시 유실 가능성을 줄이도록 구조를 고도화하고,
- 실제 실행 결과와 남은 리스크를 반복 검증하는 방식으로 진행했다.

특히 이번 과제에서는 "완벽한 기능 수"보다도,

- 어떤 문제가 먼저 해결되어야 하는지 우선순위를 잡은 점,
- AI를 디버깅/설계 보조 수단으로 사용한 점,
- 구현 이후에 운영 관점의 리스크까지 다시 점검한 점

을 보여주는 것이 중요하다고 판단했다.
