# External API 명세

## 개요
이 문서는 FinVibe Investment 서비스의 외부 노출 API 명세를 기술합니다.

---

## 1. 지갑 (Wallet)

### 지갑 잔액 조회
`GET /wallets/balance`

**설명**: 로그인한 사용자의 지갑 잔액을 조회합니다.  
**인증**: 필요

#### 응답 (200 OK)
| 필드명 | 타입 | 설명 |
| :--- | :--- | :--- |
| `walletId` | Number | 지갑 식별자 |
| `userId` | String (UUID) | 사용자 식별자 |
| `balance` | Number | 지갑 잔액 |

**응답 예시**:
```json
{
  "walletId": 0,
  "userId": "00000000-0000-0000-0000-000000000000",
  "balance": 0
}
```

---

## 2. 포트폴리오 그룹 (Portfolio Group)

### 사용자 포트폴리오 그룹 조회
`GET /portfolios`

**설명**: 로그인한 사용자의 포트폴리오 그룹 목록을 조회합니다.  
**인증**: 필요

#### 응답 (200 OK)
| 필드명 | 타입 | 설명 |
| :--- | :--- | :--- |
| `id` | Number | 포트폴리오 그룹 식별자 |
| `name` | String | 그룹 이름 |
| `iconCode` | String | 아이콘 코드 |

**응답 예시**:
```json
[
  {
    "id": 0,
    "name": "내 포트폴리오",
    "iconCode": "ICON_01"
  }
]
```

### 포트폴리오 그룹 생성
`POST /portfolios`

**설명**: 로그인한 사용자를 위해 새로운 포트폴리오 그룹을 생성합니다.  
**인증**: 필요

#### 요청 바디
| 필드명 | 타입 | 설명 | 필수 여부 |
| :--- | :--- | :--- | :--- |
| `name` | String | 그룹 이름 | Y |
| `iconCode` | String | 아이콘 코드 | Y |

**요청 예시**:
```json
{
  "name": "성장주 포트폴리오",
  "iconCode": "ICON_02"
}
```

#### 응답 (201 Created)
- 본문 없음

### 포트폴리오 그룹 수정
`PATCH /portfolios/{portfolioGroupId}`

**설명**: 특정 포트폴리오 그룹의 이름 또는 아이콘을 수정합니다.  
**인증**: 필요

#### 경로 파라미터
| 이름 | 타입 | 설명 | 필수 여부 |
| :--- | :--- | :--- | :--- |
| `portfolioGroupId` | Number | 수정할 포트폴리오 그룹 식별자 | Y |

#### 요청 바디
| 필드명 | 타입 | 설명 | 필수 여부 |
| :--- | :--- | :--- | :--- |
| `name` | String | 변경할 그룹 이름 | Y |
| `iconCode` | String | 변경할 아이콘 코드 | Y |

**요청 예시**:
```json
{
  "name": "수정된 포트폴리오",
  "iconCode": "ICON_03"
}
```

#### 응답 (200 OK)
- 본문 없음

### 포트폴리오 그룹 삭제
`DELETE /portfolios/{portfolioGroupId}`

**설명**: 특정 포트폴리오 그룹을 삭제합니다.  
**인증**: 필요

#### 경로 파라미터
| 이름 | 타입 | 설명 | 필수 여부 |
| :--- | :--- | :--- | :--- |
| `portfolioGroupId` | Number | 삭제할 포트폴리오 그룹 식별자 | Y |

#### 응답 (204 No Content)
- 본문 없음

---

## 3. 자산 (Asset)

### 포트폴리오별 자산 조회
`GET /portfolios/{portfolioId}/assets`

**설명**: 특정 포트폴리오에 속한 자산 목록을 조회합니다.  
**인증**: 필요

#### 경로 파라미터
| 이름 | 타입 | 설명 | 필수 여부 |
| :--- | :--- | :--- | :--- |
| `portfolioId` | Number | 포트폴리오 식별자 | Y |

#### 응답 (200 OK)
| 필드명 | 타입 | 설명 |
| :--- | :--- | :--- |
| `id` | Number | 자산 식별자 |
| `name` | String | 자산명 (종목명) |
| `amount` | Number | 보유 수량 |
| `totalPrice` | Number | 총 평가금액 |
| `currency` | String | 통화 (`USD`, `KRW`) |
| `stockId` | Number | 종목 식별자 |

**응답 예시**:
```json
[
  {
    "id": 1,
    "name": "애플",
    "amount": 10,
    "totalPrice": 1800,
    "currency": "USD",
    "stockId": 101
  }
]
```

### 자산 등록
`POST /portfolios/{portfolioId}/assets`

**설명**: 특정 포트폴리오에 자산을 직접 등록합니다.  
**인증**: 필요

#### 경로 파라미터
| 이름 | 타입 | 설명 | 필수 여부 |
| :--- | :--- | :--- | :--- |
| `portfolioId` | Number | 대상 포트폴리오 식별자 | Y |

#### 요청 바디
| 필드명 | 타입 | 설명 | 필수 여부 |
| :--- | :--- | :--- | :--- |
| `stockId` | Number | 종목 식별자 | Y |
| `amount` | Number | 수량 | Y |
| `stockPrice` | Number | 매수 주가 | Y |
| `name` | String | 자산명 | Y |
| `currency` | String | 통화 (`USD`, `KRW`) | Y |

**요청 예시**:
```json
{
  "stockId": 101,
  "amount": 5,
  "stockPrice": 150,
  "name": "애플",
  "currency": "USD"
}
```

#### 응답 (201 Created)
- 본문 없음

### 자산 등록 해제
`DELETE /portfolios/{portfolioId}/assets`

**설명**: 특정 포트폴리오에서 자산 보유 정보를 제거합니다.  
**인증**: 필요

#### 경로 파라미터
| 이름 | 타입 | 설명 | 필수 여부 |
| :--- | :--- | :--- | :--- |
| `portfolioId` | Number | 대상 포트폴리오 식별자 | Y |

#### 요청 바디
| 필드명 | 타입 | 설명 | 필수 여부 |
| :--- | :--- | :--- | :--- |
| `stockId` | Number | 종목 식별자 | Y |
| `amount` | Number | 제거할 수량 | Y |
| `stockPrice` | Number | 기준 주가 | Y |
| `currency` | String | 통화 (`USD`, `KRW`) | N |

**요청 예시**:
```json
{
  "stockId": 101,
  "amount": 2,
  "stockPrice": 160,
  "currency": "USD"
}
```

#### 응답 (204 No Content)
- 본문 없음

### 개인 보유 종목 TOP100 조회
`GET /assets/top-100`

**설명**: 로그인한 사용자의 보유 수량 기준 TOP100 종목을 조회합니다.  
**인증**: 필요

#### 응답 (200 OK)
| 필드명 | 타입 | 설명 |
| :--- | :--- | :--- |
| `totalElements` | Number | 전체 요소 수 |
| `items` | Array | 보유 종목 목록 |
| `items[].stockId` | Number | 종목 식별자 |
| `items[].name` | String | 종목명 |
| `items[].totalAmount` | Number | 총 보유 수량 |

**응답 예시**:
```json
{
  "totalElements": 2,
  "items": [
    {
      "stockId": 101,
      "name": "애플",
      "totalAmount": 12.5
    },
    {
      "stockId": 202,
      "name": "테슬라",
      "totalAmount": 8
    }
  ]
}
```

---

## 4. 거래 (Trade)

### 공통 열거형 (Enums)
| 구분 | 값 |
| :--- | :--- |
| `marketType` | `DOMESTIC` (국내), `INTERNATIONAL` (해외) |
| `tradeType` | `NORMAL` (일반), `RESERVED` (예약), `CANCELLED` (취소) |
| `transactionType` | `BUY` (매수), `SELL` (매도) |

### 거래 상태 조회
`GET /trades/{tradeId}`

**설명**: 개별 거래의 상세 상태를 조회합니다.  
**인증**: (컨트롤러 레벨 미강제)

#### 경로 파라미터
| 이름 | 타입 | 설명 | 필수 여부 |
| :--- | :--- | :--- | :--- |
| `tradeId` | Number | 거래 식별자 | Y |

#### 응답 (200 OK)
| 필드명 | 타입 | 설명 |
| :--- | :--- | :--- |
| `tradeId` | Number | 거래 식별자 |
| `marketType` | String | 시장 구분 (`DOMESTIC`, `INTERNATIONAL`) |
| `stockId` | Number | 종목 식별자 |
| `amount` | Number | 주문 수량 |
| `price` | Number | 주문 가격 |
| `portfolioId` | Number | 포트폴리오 식별자 |
| `userId` | String (UUID) | 사용자 식별자 |
| `tradeType` | String | 거래 유형 (`NORMAL`, `RESERVED`, `CANCELLED`) |
| `transactionType` | String | 매수/매도 구분 (`BUY`, `SELL`) |

**응답 예시**:
```json
{
  "tradeId": 123,
  "marketType": "DOMESTIC",
  "stockId": 10,
  "amount": 100,
  "price": 50000,
  "portfolioId": 1,
  "userId": "00000000-0000-0000-0000-000000000000",
  "tradeType": "NORMAL",
  "transactionType": "BUY"
}
```

### 거래 생성
`POST /trades`

**설명**: 신규 거래를 생성(매수/매도 주문)합니다.  
**인증**: (컨트롤러 레벨 미강제)

#### 요청 바디
| 필드명 | 타입 | 설명 | 필수 여부 |
| :--- | :--- | :--- | :--- |
| `marketType` | String | 시장 구분 (`DOMESTIC`, `INTERNATIONAL`) | Y |
| `stockId` | Number | 종목 식별자 | Y |
| `amount` | Number | 주문 수량 | Y |
| `price` | Number | 주문 가격 | Y |
| `portfolioId` | Number | 포트폴리오 식별자 | Y |
| `userId` | String (UUID) | 사용자 식별자 | Y |
| `tradeType` | String | 거래 유형 (`NORMAL`, `RESERVED`) | Y |
| `transactionType` | String | 매수/매도 구분 (`BUY`, `SELL`) | Y |

**요청 예시**:
```json
{
  "marketType": "DOMESTIC",
  "stockId": 10,
  "amount": 5,
  "price": 60000,
  "portfolioId": 1,
  "userId": "00000000-0000-0000-0000-000000000000",
  "tradeType": "NORMAL",
  "transactionType": "BUY"
}
```

#### 응답 (200 OK)
- 응답 바디는 [거래 상태 조회](#거래-상태-조회)와 동일합니다.

### 거래 취소
`DELETE /trades/{tradeId}`

**설명**: 진행 중인 거래를 취소합니다.  
**인증**: (컨트롤러 레벨 미강제)

#### 경로 파라미터
| 이름 | 타입 | 설명 | 필수 여부 |
| :--- | :--- | :--- | :--- |
| `tradeId` | Number | 취소할 거래 식별자 | Y |

#### 응답 (200 OK)
- 응답 바디는 [거래 상태 조회](#거래-상태-조회)와 동일하며, `tradeType`이 `CANCELLED`로 변경됩니다.

---

## 5. 시장 (Market)

### 장 상태 조회
`GET /market/status`

**설명**: 국내 시장의 개장/폐장 상태를 조회합니다.

#### 응답 (200 OK)
| 필드명 | 타입 | 설명 |
| :--- | :--- | :--- |
| `status` | String | 장 상태 (`OPEN`, `CLOSED`) |

**응답 예시**:
```json
{
  "status": "OPEN"
}
```

### 종가 조회
`GET /market/stocks/closing-prices`

**설명**: 국내 종목의 최신 종가(마지막 일봉)를 조회합니다.

#### 요청 파라미터
| 이름 | 타입 | 설명 | 필수 여부 |
| :--- | :--- | :--- | :--- |
| `stockIds` | Number[] | 종목 ID 리스트 | Y |

#### 응답 (200 OK)
| 필드명 | 타입 | 설명 |
| :--- | :--- | :--- |
| `stockId` | Number | 종목 식별자 |
| `stockName` | String | 종목명 |
| `at` | String (DateTime) | 종가 기준 시각 |
| `close` | Number | 종가 |
| `prevDayChangePct` | Number | 전일 대비 등락률 |
| `volume` | Number | 거래량 |
| `value` | Number | 거래대금 |

**응답 예시**:
```json
[
  {
    "stockId": 1,
    "stockName": "삼성전자",
    "at": "2024-01-02T15:30:00",
    "close": 70000,
    "prevDayChangePct": 0.5,
    "volume": 12000000,
    "value": 840000000000
  }
]
```
