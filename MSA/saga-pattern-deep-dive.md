# Saga 패턴 심층 분석

## 목차
1. [Saga 패턴 개요](#saga-패턴-개요)
2. [Choreography vs Orchestration](#choreography-vs-orchestration)
3. [보상 트랜잭션](#보상-트랜잭션)
4. [Saga 구현 예제](#saga-구현-예제)
5. [설계 고려사항](#설계-고려사항)
6. [핵심 정리](#핵심-정리)

---

## Saga 패턴 개요

### Saga 패턴이란?

Saga 패턴은 마이크로서비스 환경에서 분산 트랜잭션을 관리하는 패턴입니다. 각 서비스의 로컬 트랜잭션을 순차적으로 실행하고, 실패 시 보상 트랜잭션으로 롤백합니다.

```
┌──────────────────────────────────────────────────────────────────┐
│                    Saga 패턴 기본 개념                            │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  기존 ACID 트랜잭션 (2PC):                                       │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  BEGIN TRANSACTION                                         │  │
│  │    Update Service A                                        │  │
│  │    Update Service B                                        │  │
│  │    Update Service C                                        │  │
│  │  COMMIT (or ROLLBACK all)                                  │  │
│  └────────────────────────────────────────────────────────────┘  │
│  → 모든 서비스가 락을 유지 (블로킹)                               │
│                                                                   │
│  Saga 패턴:                                                      │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  T1: 주문 생성 → 완료                                       │  │
│  │  T2: 재고 차감 → 완료                                       │  │
│  │  T3: 결제 처리 → 실패!                                      │  │
│  │  C2: 재고 복구 (보상)                                       │  │
│  │  C1: 주문 취소 (보상)                                       │  │
│  └────────────────────────────────────────────────────────────┘  │
│  → 각 단계 독립적 커밋, 실패 시 보상 트랜잭션                     │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 언제 Saga를 사용하나?

```
✅ Saga가 적합한 경우:
   - 여러 마이크로서비스에 걸친 비즈니스 트랜잭션
   - 장시간 실행되는 트랜잭션
   - 높은 가용성이 필요한 경우
   - 최종 일관성(Eventual Consistency)을 허용하는 경우

❌ Saga가 부적합한 경우:
   - 강한 일관성이 필수인 경우 (금융 정산 등)
   - 단일 데이터베이스 내 트랜잭션
   - 롤백이 불가능한 외부 시스템 연동
```

---

## Choreography vs Orchestration

### Choreography (이벤트 기반)

```
┌──────────────────────────────────────────────────────────────────┐
│                    Choreography Saga                              │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  각 서비스가 이벤트를 발행하고 다른 서비스가 구독                   │
│                                                                   │
│  Order          Inventory        Payment         Shipping        │
│  Service        Service          Service         Service         │
│     │              │                │               │            │
│     │  OrderCreated │                │               │            │
│     │──────────────►│                │               │            │
│     │              │                │               │            │
│     │              │ InventoryReserved               │            │
│     │              │───────────────►│               │            │
│     │              │                │               │            │
│     │              │                │ PaymentCompleted            │
│     │              │                │──────────────►│            │
│     │              │                │               │            │
│     │              │                │               │ ShippingStarted
│     │◄──────────────────────────────────────────────│            │
│     │              │                │               │            │
│                                                                   │
│  장점:                                                           │
│  - 느슨한 결합                                                   │
│  - 단순한 서비스 (이벤트만 발행/구독)                            │
│  - 쉬운 확장                                                     │
│                                                                   │
│  단점:                                                           │
│  - 전체 흐름 파악 어려움                                         │
│  - 순환 의존성 위험                                              │
│  - 디버깅 복잡                                                   │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Orchestration (중앙 조정자)

```
┌──────────────────────────────────────────────────────────────────┐
│                    Orchestration Saga                             │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  중앙 Orchestrator가 전체 흐름을 제어                            │
│                                                                   │
│                    Order Saga                                     │
│                    Orchestrator                                   │
│                         │                                         │
│         ┌───────────────┼───────────────┐                        │
│         ▼               ▼               ▼                        │
│     ┌────────┐     ┌────────┐     ┌────────┐                    │
│     │Inventory│     │Payment │     │Shipping│                    │
│     │Service │     │Service │     │Service │                    │
│     └────────┘     └────────┘     └────────┘                    │
│                                                                   │
│  흐름:                                                           │
│  1. Orchestrator → Inventory: 재고 예약 요청                     │
│  2. Inventory → Orchestrator: 예약 완료                          │
│  3. Orchestrator → Payment: 결제 요청                            │
│  4. Payment → Orchestrator: 결제 완료                            │
│  5. Orchestrator → Shipping: 배송 요청                           │
│                                                                   │
│  장점:                                                           │
│  - 명확한 흐름 제어                                              │
│  - 디버깅 용이                                                   │
│  - 복잡한 비즈니스 로직 처리                                     │
│                                                                   │
│  단점:                                                           │
│  - Orchestrator에 로직 집중 (단일 장애점)                        │
│  - 서비스 간 결합도 증가                                         │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 비교 표

| 구분 | Choreography | Orchestration |
|------|--------------|---------------|
| 제어 방식 | 분산 (이벤트) | 중앙 (Orchestrator) |
| 결합도 | 느슨함 | 상대적으로 높음 |
| 복잡도 | 단순 (개별 서비스) | 복잡 (Orchestrator) |
| 가시성 | 낮음 | 높음 |
| 디버깅 | 어려움 | 쉬움 |
| 확장성 | 좋음 | 보통 |
| 적합 케이스 | 단순한 흐름, 적은 단계 | 복잡한 흐름, 많은 단계 |

---

## 보상 트랜잭션

### 보상 트랜잭션 설계

```
┌──────────────────────────────────────────────────────────────────┐
│                    보상 트랜잭션 설계 원칙                         │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  1. 멱등성 (Idempotency)                                         │
│     - 여러 번 실행해도 같은 결과                                  │
│     - 재시도 안전성 보장                                         │
│                                                                   │
│  2. 역순 실행                                                    │
│     - 마지막 성공 단계부터 첫 단계까지 역순으로 보상              │
│                                                                   │
│  3. 의미적 롤백                                                  │
│     - 물리적 롤백이 아닌 비즈니스적 되돌리기                     │
│     - 예: 재고 차감 → 재고 복구 (새 트랜잭션)                    │
│                                                                   │
│  정상 흐름:                                                      │
│  T1 ──► T2 ──► T3 ──► T4 ──► 완료                               │
│                                                                   │
│  T3 실패 시:                                                     │
│  T1 ──► T2 ──► T3(X) ──► C2 ──► C1 ──► 취소 완료                │
│                                                                   │
│                                                                   │
│  보상 트랜잭션 예시:                                             │
│  ┌─────────────────┬──────────────────────────────────────────┐ │
│  │ 원본 트랜잭션    │ 보상 트랜잭션                             │ │
│  ├─────────────────┼──────────────────────────────────────────┤ │
│  │ 주문 생성       │ 주문 상태를 CANCELLED로 변경              │ │
│  │ 재고 차감       │ 재고 수량 복구                            │ │
│  │ 결제 승인       │ 결제 취소/환불 요청                       │ │
│  │ 배송 요청       │ 배송 취소 요청                            │ │
│  └─────────────────┴──────────────────────────────────────────┘ │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 보상 불가능한 경우 처리

```java
// 보상 불가능한 액션을 마지막에 배치
public class OrderSaga {

    public void execute(OrderRequest request) {
        // 1. 주문 생성 (보상 가능)
        Order order = orderService.create(request);

        try {
            // 2. 재고 예약 (보상 가능)
            inventoryService.reserve(order);

            // 3. 결제 처리 (보상 가능)
            paymentService.process(order);

            // 4. 외부 알림 발송 (보상 불가능 - 마지막에 배치)
            notificationService.sendOrderConfirmation(order);

        } catch (Exception e) {
            compensate(order);
            throw e;
        }
    }

    // 또는 Pivot Transaction 패턴
    // 결제 승인을 Pivot으로 설정하여 그 이후는 무조건 진행
}
```

---

## Saga 구현 예제

### Orchestration Saga 구현 (Spring + Kafka)

```java
// Saga 상태
public enum OrderSagaState {
    STARTED,
    INVENTORY_RESERVED,
    PAYMENT_PROCESSED,
    SHIPPING_REQUESTED,
    COMPLETED,
    // 보상 상태
    PAYMENT_FAILED,
    INVENTORY_RELEASED,
    CANCELLED
}

// Saga 데이터
@Entity
@Table(name = "order_sagas")
public class OrderSaga {
    @Id
    private String sagaId;

    @Enumerated(EnumType.STRING)
    private OrderSagaState state;

    private Long orderId;
    private String failureReason;

    @Column(columnDefinition = "JSON")
    private String sagaData;  // 중간 데이터 저장

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// Saga Orchestrator
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderSagaOrchestrator {

    private final OrderSagaRepository sagaRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // 1. Saga 시작
    @Transactional
    public String startSaga(OrderRequest request) {
        String sagaId = UUID.randomUUID().toString();

        OrderSaga saga = new OrderSaga();
        saga.setSagaId(sagaId);
        saga.setState(OrderSagaState.STARTED);
        saga.setOrderId(request.getOrderId());
        saga.setCreatedAt(LocalDateTime.now());
        sagaRepository.save(saga);

        // 첫 번째 단계: 재고 예약 요청
        ReserveInventoryCommand command = new ReserveInventoryCommand(
            sagaId,
            request.getOrderId(),
            request.getItems()
        );
        kafkaTemplate.send("inventory-commands", sagaId, command);

        log.info("Saga started: {}", sagaId);
        return sagaId;
    }

    // 2. 재고 예약 완료 처리
    @KafkaListener(topics = "inventory-events", groupId = "order-saga")
    @Transactional
    public void handleInventoryEvent(InventoryEvent event) {
        OrderSaga saga = sagaRepository.findById(event.getSagaId())
            .orElseThrow();

        if (event instanceof InventoryReservedEvent) {
            saga.setState(OrderSagaState.INVENTORY_RESERVED);
            sagaRepository.save(saga);

            // 다음 단계: 결제 요청
            ProcessPaymentCommand command = new ProcessPaymentCommand(
                saga.getSagaId(),
                saga.getOrderId(),
                event.getTotalAmount()
            );
            kafkaTemplate.send("payment-commands", saga.getSagaId(), command);

        } else if (event instanceof InventoryReservationFailedEvent) {
            // 보상: 주문 취소
            saga.setState(OrderSagaState.CANCELLED);
            saga.setFailureReason("Inventory reservation failed");
            sagaRepository.save(saga);

            CancelOrderCommand command = new CancelOrderCommand(
                saga.getSagaId(),
                saga.getOrderId()
            );
            kafkaTemplate.send("order-commands", saga.getSagaId(), command);
        }
    }

    // 3. 결제 완료 처리
    @KafkaListener(topics = "payment-events", groupId = "order-saga")
    @Transactional
    public void handlePaymentEvent(PaymentEvent event) {
        OrderSaga saga = sagaRepository.findById(event.getSagaId())
            .orElseThrow();

        if (event instanceof PaymentCompletedEvent) {
            saga.setState(OrderSagaState.PAYMENT_PROCESSED);
            sagaRepository.save(saga);

            // 다음 단계: 배송 요청
            RequestShippingCommand command = new RequestShippingCommand(
                saga.getSagaId(),
                saga.getOrderId()
            );
            kafkaTemplate.send("shipping-commands", saga.getSagaId(), command);

        } else if (event instanceof PaymentFailedEvent) {
            saga.setState(OrderSagaState.PAYMENT_FAILED);
            saga.setFailureReason("Payment failed");
            sagaRepository.save(saga);

            // 보상: 재고 해제
            ReleaseInventoryCommand command = new ReleaseInventoryCommand(
                saga.getSagaId(),
                saga.getOrderId()
            );
            kafkaTemplate.send("inventory-commands", saga.getSagaId(), command);
        }
    }

    // 4. 배송 요청 완료 처리
    @KafkaListener(topics = "shipping-events", groupId = "order-saga")
    @Transactional
    public void handleShippingEvent(ShippingEvent event) {
        OrderSaga saga = sagaRepository.findById(event.getSagaId())
            .orElseThrow();

        if (event instanceof ShippingRequestedEvent) {
            saga.setState(OrderSagaState.COMPLETED);
            saga.setUpdatedAt(LocalDateTime.now());
            sagaRepository.save(saga);

            log.info("Saga completed successfully: {}", saga.getSagaId());
        }
    }

    // 5. 재고 해제 완료 처리 (보상 체인)
    @KafkaListener(topics = "inventory-compensation-events", groupId = "order-saga")
    @Transactional
    public void handleInventoryCompensation(InventoryReleasedEvent event) {
        OrderSaga saga = sagaRepository.findById(event.getSagaId())
            .orElseThrow();

        saga.setState(OrderSagaState.INVENTORY_RELEASED);
        sagaRepository.save(saga);

        // 다음 보상: 주문 취소
        CancelOrderCommand command = new CancelOrderCommand(
            saga.getSagaId(),
            saga.getOrderId()
        );
        kafkaTemplate.send("order-commands", saga.getSagaId(), command);
    }
}
```

### Choreography Saga 구현

```java
// Inventory Service - 이벤트 기반
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryEventHandler {

    private final InventoryRepository inventoryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "order-events", groupId = "inventory-service")
    @Transactional
    public void handleOrderEvent(OrderEvent event) {
        if (event instanceof OrderCreatedEvent orderCreated) {
            try {
                // 재고 예약
                for (OrderItem item : orderCreated.getItems()) {
                    Inventory inventory = inventoryRepository
                        .findByProductIdWithLock(item.getProductId())
                        .orElseThrow();

                    if (inventory.getQuantity() < item.getQuantity()) {
                        throw new InsufficientStockException();
                    }

                    inventory.decrease(item.getQuantity());
                    inventoryRepository.save(inventory);
                }

                // 성공 이벤트 발행
                kafkaTemplate.send("inventory-events",
                    new InventoryReservedEvent(orderCreated.getOrderId()));

            } catch (Exception e) {
                // 실패 이벤트 발행
                kafkaTemplate.send("inventory-events",
                    new InventoryReservationFailedEvent(
                        orderCreated.getOrderId(),
                        e.getMessage()
                    ));
            }
        }
    }

    // 보상 이벤트 처리
    @KafkaListener(topics = "payment-events", groupId = "inventory-service")
    @Transactional
    public void handlePaymentEvent(PaymentEvent event) {
        if (event instanceof PaymentFailedEvent paymentFailed) {
            // 재고 복구
            Order order = orderRepository.findById(paymentFailed.getOrderId())
                .orElseThrow();

            for (OrderItem item : order.getItems()) {
                Inventory inventory = inventoryRepository
                    .findByProductId(item.getProductId())
                    .orElseThrow();

                inventory.increase(item.getQuantity());
                inventoryRepository.save(inventory);
            }

            kafkaTemplate.send("inventory-events",
                new InventoryReleasedEvent(paymentFailed.getOrderId()));
        }
    }
}
```

---

## 설계 고려사항

### 멱등성 보장

```java
// Outbox 패턴 + 멱등성 키
@Entity
@Table(name = "processed_messages")
public class ProcessedMessage {
    @Id
    private String messageId;
    private LocalDateTime processedAt;
}

@Service
@RequiredArgsConstructor
public class IdempotentMessageHandler {

    private final ProcessedMessageRepository processedMessageRepository;

    @Transactional
    public <T> void handleMessage(String messageId, Supplier<T> handler) {
        // 이미 처리된 메시지인지 확인
        if (processedMessageRepository.existsById(messageId)) {
            log.info("Message already processed: {}", messageId);
            return;
        }

        // 메시지 처리
        handler.get();

        // 처리 완료 기록
        ProcessedMessage processed = new ProcessedMessage();
        processed.setMessageId(messageId);
        processed.setProcessedAt(LocalDateTime.now());
        processedMessageRepository.save(processed);
    }
}

// 사용 예시
@KafkaListener(topics = "order-events")
public void handleEvent(@Payload OrderEvent event,
                        @Header(KafkaHeaders.MESSAGE_KEY) String key) {
    idempotentHandler.handleMessage(key, () -> {
        processOrder(event);
        return null;
    });
}
```

### Saga 상태 모니터링

```java
// Saga 모니터링 서비스
@Service
@RequiredArgsConstructor
public class SagaMonitoringService {

    private final OrderSagaRepository sagaRepository;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedRate = 60000)  // 1분마다
    public void monitorSagas() {
        // 상태별 Saga 수 집계
        Map<OrderSagaState, Long> stateCounts = sagaRepository.countByState();

        stateCounts.forEach((state, count) ->
            meterRegistry.gauge("saga.count",
                Tags.of("state", state.name()),
                count));

        // 장시간 진행 중인 Saga 감지
        List<OrderSaga> stuckSagas = sagaRepository.findStuckSagas(
            LocalDateTime.now().minusMinutes(10)
        );

        if (!stuckSagas.isEmpty()) {
            log.warn("Found {} stuck sagas", stuckSagas.size());
            alertService.sendAlert("Stuck Sagas Detected", stuckSagas);
        }
    }

    // 실패한 Saga 재시도
    @Scheduled(fixedRate = 300000)  // 5분마다
    public void retryFailedSagas() {
        List<OrderSaga> failedSagas = sagaRepository.findByStateIn(
            List.of(OrderSagaState.PAYMENT_FAILED, OrderSagaState.INVENTORY_RELEASED)
        );

        for (OrderSaga saga : failedSagas) {
            if (saga.getRetryCount() < 3) {
                sagaOrchestrator.retryCompensation(saga);
            }
        }
    }
}
```

---

## 핵심 정리

### Saga 패턴 선택 기준

| 상황 | Choreography | Orchestration |
|------|--------------|---------------|
| 서비스 수 | 2-3개 | 4개 이상 |
| 흐름 복잡도 | 단순 선형 | 복잡한 분기 |
| 팀 구조 | 분산 팀 | 중앙 팀 |
| 모니터링 요구 | 낮음 | 높음 |
| 결합도 허용 | 낮음 필수 | 어느 정도 허용 |

### 설계 체크리스트

```
□ 각 단계의 보상 트랜잭션 정의
□ 멱등성 보장 (중복 처리 방지)
□ 실패 시나리오 모두 식별
□ 타임아웃 및 재시도 전략
□ Saga 상태 저장소 설계
□ 모니터링 및 알림 설정
□ 데드레터 큐 처리
□ 보상 불가능한 액션 마지막 배치
```

### 면접 대비 핵심 질문

1. **Q: Saga 패턴이 필요한 이유는?**
   - A: MSA에서 2PC(Two-Phase Commit)는 블로킹, 단일 장애점 문제. Saga는 로컬 트랜잭션 + 보상으로 최종 일관성 달성, 높은 가용성 유지

2. **Q: Choreography와 Orchestration의 차이점은?**
   - A: Choreography는 이벤트 기반 분산 제어, 느슨한 결합. Orchestration은 중앙 조정자가 흐름 제어, 명확한 가시성. 복잡도에 따라 선택

3. **Q: 보상 트랜잭션 설계 시 주의점은?**
   - A: 멱등성 보장, 역순 실행, 의미적 롤백(물리적 X), 보상 불가능한 액션은 마지막에 배치

4. **Q: Saga에서 일관성을 어떻게 보장하나요?**
   - A: 최종 일관성(Eventual Consistency)만 보장. 각 로컬 트랜잭션 커밋 후 이벤트 발행, 실패 시 보상 트랜잭션으로 비즈니스 일관성 유지

---

*마지막 업데이트: 2026년 01월*
