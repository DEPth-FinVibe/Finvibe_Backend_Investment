# Kafka Interfaces

이 문서는 finvibe-investment 내 Kafka 기반 인터페이스(토픽, 프로듀서/컨슈머, 이벤트 스키마)를 정리한다.

## Trade Module

### Topics

- `trade.trade-executed.v1` (publish)
- `trade.trade-reserved.v1` (publish)
- `trade.trade-cancelled.v1` (publish)
- `market.reservation-satisfied.v1` (subscribe)

### Producers

- trade.trade-executed.v1
  - Event: 체결 완료된 거래를 알리는 이벤트
  - Class: `src/main/java/depth/finvibe/investment/modules/trade/infra/messaging/TradeKafkaProducer.java`
  - Key: `userId.toString()`
  - Value: `TradeExecutedEvent`
  - Notes: 정상 체결 이벤트에 사용

- trade.trade-reserved.v1
  - Event: 예약 주문 거래 체결 정보를 알리는 이벤트
  - Class: `src/main/java/depth/finvibe/investment/modules/trade/infra/messaging/TradeKafkaProducer.java`
  - Key: `userId.toString()`
  - Value: `TradeExecutedEvent`

- trade.trade-cancelled.v1
  - Event: 예약 주문 취소를 알리는 이벤트
  - Class: `src/main/java/depth/finvibe/investment/modules/trade/infra/messaging/TradeKafkaProducer.java`
  - Key: `userId.toString()`
  - Value: `Long` (tradeId)

### Consumers

- market.reservation-satisfied.v1
  - Event: 예약 주문 체결 조건 충족을 수신해 예약 거래 실행을 트리거하는 이벤트
  - Group: `trade-group`
  - Class: `src/main/java/depth/finvibe/investment/modules/trade/infra/messaging/TradeKafkaConsumer.java`
  - Value type mapping: `spring.json.value.default.type=depth.finvibe.investment.shared.dto.ReservationSatisfiedEvent`

### Event Schemas

- TradeExecutedEvent
  - Source: `src/main/java/depth/finvibe/investment/shared/dto/TradeExecutedEvent.java`
  - Description: 체결 완료된 거래 정보를 전달
  - Fields:
    - `tradeId: Long`
    - `userId: String`
    - `type: String` ("BUY" | "SELL")
    - `amount: BigDecimal`
    - `price: Long`
    - `stockId: Long`
    - `name: String`
    - `currency: String` ("KRW" | "USD")
    - `portfolioId: Long`

- ReservationSatisfiedEvent
  - Source: `src/main/java/depth/finvibe/investment/shared/dto/ReservationSatisfiedEvent.java`
  - Description: 예약 주문이 체결 조건을 만족했음을 전달
  - Fields:
    - `tradeId: Long`
    - `type: String` ("BUY" | "SELL")
    - `price: Long`

## Market Module

### Topics

- `market.batch-price-updated.v1` (publish)
- `market.reservation-satisfied.v1` (publish)
- `trade.trade-reserved.v1` (subscribe)
- `trade.trade-cancelled.v1` (subscribe)

### Producers

- market.batch-price-updated.v1
  - Event: 배치 시세 갱신 완료를 알리는 이벤트
  - Class: `src/main/java/depth/finvibe/investment/modules/market/infra/messaging/MarketKafkaProducer.java`
  - Key: 없음 (null)
  - Value: `BatchPriceUpdatedEvent`

- market.reservation-satisfied.v1
  - Event: 예약 주문 체결 조건 만족을 알리는 이벤트
  - Class: `src/main/java/depth/finvibe/investment/modules/market/infra/messaging/MarketKafkaProducer.java`
  - Key: 없음 (null)
  - Value: `ReservationSatisfiedEvent`

### Consumers

- trade.trade-reserved.v1
  - Event: 예약 주문 거래를 수신해 예약 상태를 갱신하는 이벤트
  - Group: `market-group`
  - Class: `src/main/java/depth/finvibe/investment/modules/market/infra/messaging/MarketKafkaConsumer.java`
  - Value type mapping: `spring.json.value.default.type=depth.finvibe.investment.shared.dto.TradeExecutedEvent`

- trade.trade-cancelled.v1
  - Event: 예약 주문 취소를 수신해 예약 상태를 해제하는 이벤트
  - Group: `market-group`
  - Class: `src/main/java/depth/finvibe/investment/modules/market/infra/messaging/MarketKafkaConsumer.java`
  - Value type mapping: 없음 (Long)

### Event Schemas

- BatchPriceUpdatedEvent
  - Source: `src/main/java/depth/finvibe/investment/shared/dto/BatchPriceUpdatedEvent.java`
  - Description: 배치 시세 갱신 결과 요약 전달
  - Fields:
    - `batchExecutedAt: LocalDateTime`
    - `totalStockCount: Integer`
    - `updatedStockIds: List<Long>`

- ReservationSatisfiedEvent
  - Source: `src/main/java/depth/finvibe/investment/shared/dto/ReservationSatisfiedEvent.java`
  - Description: 예약 주문이 체결 조건을 만족했음을 전달
  - Fields:
    - `tradeId: Long`
    - `type: String` ("BUY" | "SELL")
    - `price: Long`

- TradeExecutedEvent
  - Source: `src/main/java/depth/finvibe/investment/shared/dto/TradeExecutedEvent.java`
  - Description: 체결 완료된 거래 정보를 전달
  - Fields:
    - `tradeId: Long`
    - `userId: String`
    - `type: String` ("BUY" | "SELL")
    - `amount: BigDecimal`
    - `price: Long`
    - `stockId: Long`
    - `name: String`
    - `currency: String` ("KRW" | "USD")
    - `portfolioId: Long`

## Asset Module

### Topics

- `trade.trade-executed.v1` (subscribe)
- `user.signup.v1` (subscribe)
- `market.batch-price-updated.v1` (subscribe)

### Consumers

- trade.trade-executed.v1
  - Event: 체결 완료 거래를 수신해 자산 상태를 갱신하는 이벤트
  - Group: `asset-group`
  - Class: `src/main/java/depth/finvibe/investment/modules/asset/infra/messaging/KafkaConsumer.java`
  - Value type mapping: `spring.json.value.default.type=depth.finvibe.investment.shared.dto.TradeExecutedEvent`

- user.signup.v1
  - Event: 신규 가입 사용자를 수신해 초기 자산을 준비하는 이벤트
  - Group: `asset-group`
  - Class: `src/main/java/depth/finvibe/investment/modules/asset/infra/messaging/KafkaConsumer.java`
  - Value type mapping: `spring.json.value.default.type=depth.finvibe.investment.shared.dto.SignUpEvent`

- market.batch-price-updated.v1
  - Event: 배치 시세 갱신 결과를 수신해 자산 평가에 반영하는 이벤트
  - Group: `asset-group`
  - Class: `src/main/java/depth/finvibe/investment/modules/asset/infra/messaging/KafkaConsumer.java`
  - Value type mapping: `spring.json.value.default.type=depth.finvibe.investment.shared.dto.BatchPriceUpdatedEvent`

### Event Schemas

- TradeExecutedEvent
  - Source: `src/main/java/depth/finvibe/investment/shared/dto/TradeExecutedEvent.java`
  - Description: 체결 완료된 거래 정보를 전달
  - Fields:
    - `tradeId: Long`
    - `userId: String`
    - `type: String` ("BUY" | "SELL")
    - `amount: BigDecimal`
    - `price: Long`
    - `stockId: Long`
    - `name: String`
    - `currency: String` ("KRW" | "USD")
    - `portfolioId: Long`

- SignUpEvent
  - Source: `src/main/java/depth/finvibe/investment/shared/dto/SignUpEvent.java`
  - Description: 신규 가입 사용자 식별자 전달
  - Fields:
    - `userId: String`

- BatchPriceUpdatedEvent
  - Source: `src/main/java/depth/finvibe/investment/shared/dto/BatchPriceUpdatedEvent.java`
  - Description: 배치 시세 갱신 결과 요약 전달
  - Fields:
    - `batchExecutedAt: LocalDateTime`
    - `totalStockCount: Integer`
    - `updatedStockIds: List<Long>`

## Wallet Module

### Topics

- `trade.trade-executed.v1` (subscribe)
- `user.signup.v1` (subscribe)

### Consumers

- trade.trade-executed.v1
  - Event: 체결 완료 거래를 수신해 지갑 상태를 갱신하는 이벤트
  - Group: `wallet-group`
  - Class: `src/main/java/depth/finvibe/investment/modules/wallet/infra/messaging/WalletKafkaConsumer.java`
  - Value type mapping: `spring.json.value.default.type=depth.finvibe.investment.shared.dto.TradeExecutedEvent`

- user.signup.v1
  - Event: 신규 가입 사용자를 수신해 초기 지갑을 준비하는 이벤트
  - Group: `wallet-group`
  - Class: `src/main/java/depth/finvibe/investment/modules/wallet/infra/messaging/WalletKafkaConsumer.java`
  - Value type mapping: `spring.json.value.default.type=depth.finvibe.investment.shared.dto.SignUpEvent`

### Event Schemas

- TradeExecutedEvent
  - Source: `src/main/java/depth/finvibe/investment/shared/dto/TradeExecutedEvent.java`
  - Description: 체결 완료된 거래 정보를 전달
  - Fields:
    - `tradeId: Long`
    - `userId: String`
    - `type: String` ("BUY" | "SELL")
    - `amount: BigDecimal`
    - `price: Long`
    - `stockId: Long`
    - `name: String`
    - `currency: String` ("KRW" | "USD")
    - `portfolioId: Long`

- SignUpEvent
  - Source: `src/main/java/depth/finvibe/investment/shared/dto/SignUpEvent.java`
  - Description: 신규 가입 사용자 식별자 전달
  - Fields:
    - `userId: String`

## User Module (External)

### Topics

- `user.signup.v1` (publish)

### Producers

- user.signup.v1
  - Event: 신규 가입 사용자 식별자 전달
  - Producer: 외부 사용자 서비스
  - Value: `SignUpEvent`

### Event Schemas

- SignUpEvent
  - Source: `src/main/java/depth/finvibe/investment/shared/dto/SignUpEvent.java`
  - Description: 신규 가입 사용자 식별자 전달
  - Fields:
    - `userId: String`

## Serialization/Config Notes

- Producer uses `JacksonJsonSerializer` and disables type headers
  - Source: `src/main/java/depth/finvibe/investment/boot/config/KafkaConfig.java`
- Consumer uses `JacksonJsonDeserializer` with `spring.json.use.type.headers=false`
  - Source: `src/main/resources/application-kafka.yml`
- 각 `@KafkaListener`는 `spring.json.value.default.type`로 DTO 타입을 명시
