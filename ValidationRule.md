# Validation Rule

수집 API로 수신되는 데이터에 대한 Validation 규칙입니다.

---

## 공통 필드 (Common)

| 필드 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| `service_id` | String | ✅ | 비어있지 않을 것, 최대 50자 |
| `user_id` | String | ✅ | UUID 형식, 비어있지 않을 것 |
| `device_id` | String | ✅ | UUID 형식, 비어있지 않을 것 |

---

## 이벤트 공통 필드 (Event)

| 필드 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| `event_log_id` | String | ✅ | `{UUID}:{timestamp}` 형식, 비어있지 않을 것 |
| `event_name` | String | ✅ | 아래 허용된 이벤트명 중 하나 |
| `event_datetime` | String | ✅ | ISO 8601 형식 (`yyyy-MM-dd'T'HH:mm:ss'Z'`), 미래 시간 불가 |
| `event_properties` | Map | ❌ | 이벤트별 규칙 참조 |

### 허용된 이벤트명

`df_start_session`, `df_end_session`, `df_login`, `df_logout`, `df_purchase`, `df_view_product`, `df_add_to_cart`, `df_search`, `df_sign_up`, `df_add_payment_info`

---

## 이벤트별 event_properties 규칙

### df_start_session

| 프로퍼티 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| `df_session_id` | String | ✅ | 비어있지 않을 것, 최대 100자 |

### df_end_session

| 프로퍼티 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| `df_session_id` | String | ✅ | 비어있지 않을 것, 최대 100자 |
| `df_session_duration` | Number | ✅ | 0 이상의 정수 (단위: 초) |

### df_login

| 프로퍼티 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| `df_login_method` | String | ✅ | `Email`, `Google`, `Apple`, `Kakao`, `Naver` 중 하나 |

### df_logout

`event_properties` 없음

### df_purchase

| 프로퍼티 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| `df_order_id` | String | ✅ | UUID 형식 |
| `df_total_purchase_amount` | Number | ✅ | 0보다 큰 실수 |
| `df_payment_method` | String | ✅ | `Card`, `BankTransfer`, `Cash` 중 하나 |

### df_view_product

| 프로퍼티 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| `df_product_id` | String | ✅ | 비어있지 않을 것, 최대 100자 |
| `df_product_name` | String | ✅ | 비어있지 않을 것, 최대 200자 |
| `df_price` | Number | ✅ | 0 이상의 실수 |

### df_add_to_cart

| 프로퍼티 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| `df_product_id` | String | ✅ | 비어있지 않을 것, 최대 100자 |
| `df_product_name` | String | ✅ | 비어있지 않을 것, 최대 200자 |
| `df_price` | Number | ✅ | 0 이상의 실수 |
| `df_quantity` | Number | ✅ | 1 이상의 정수 |

### df_search

| 프로퍼티 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| `df_search_keyword` | String | ✅ | 비어있지 않을 것, 최대 500자 |

### df_sign_up

| 프로퍼티 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| `df_sign_up_method` | String | ✅ | `Email`, `Google`, `Apple`, `Kakao`, `Naver` 중 하나 |

### df_add_payment_info

| 프로퍼티 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| `df_payment_method` | String | ✅ | `Card`, `BankTransfer`, `Cash` 중 하나 |
