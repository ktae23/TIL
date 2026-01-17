# 이벤트 기반 아키텍처

## 목차
1. [이벤트 기반 아키텍처 개념](#이벤트-기반-아키텍처-개념)
2. [이벤트 소싱 (Event Sourcing)](#이벤트-소싱-event-sourcing)
3. [CQRS](#cqrs)
4. [멱등성 보장](#멱등성-보장)
5. [구현 패턴](#구현-패턴)
6. [핵심 정리](#핵심-정리)

---

## 이벤트 기반 아키텍처 개념

### EDA (Event-Driven Architecture)란?

```
┌──────────────────────────────────────────────────────────────────┐
│                    이벤트 기반 아키텍처                           │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  전통적인 요청-응답 방식:                                        │
│  ┌─────────┐  호출  ┌─────────┐  호출  ┌─────────┐              │
│  │Service A│ ─────► │Service B│ ─────► │Service C│              │
│  │         │ ◄───── │         │ ◄───── │         │              │
│  └─────────┘  응답  └─────────┘  응답  └─────────┘              │
│  → 동기, 강한 결합, 연쇄 장애                                    │
│                                                                   │
│  이벤트 기반 방식:                                               │
│  ┌─────────┐        ┌─────────────────┐                         │
│  │Service A│ ─────► │  Event Broker   │                         │
│  └─────────┘ 이벤트 │  (Kafka 등)     │                         │
│     발행           └────────┬────────┘                         │
│                             │                                    │
│              ┌──────────────┼──────────────┐                    │
│              ▼              ▼              ▼                    │
│         ┌─────────┐   ┌─────────┐   ┌─────────┐                │
│         │Service B│   │Service C│   │Service D│                │
│         └─────────┘   └─────────┘   └─────────┘                │
│  → 비동기, 느슨한 결합, 장애 격리                                │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 이벤트 유형

```
┌──────────────────────────────────────────────────────────────────┐
│                       이벤트 유형                                 │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  1. Domain Event (도메인 이벤트)                                 │
│     - 비즈니스적으로 중요한 사건                                 │
│     - 과거형으로 명명                                            │
│     - 예: OrderCreated, PaymentCompleted, UserRegistered        │
│                                                                   │
│  2. Integration Event (통합 이벤트)                              │
│     - 서비스 간 통신용 이벤트                                    │
│     - Bounded Context 경계를 넘어 발행                           │
│     - 예: OrderPlacedIntegrationEvent                            │
│                                                                   │
│  3. Event Notification (알림 이벤트)                             │
│     - 최소한의 정보만 포함                                       │
│     - 필요 시 API 호출로 상세 조회                               │
│     - 예: { "orderId": 123, "status": "SHIPPED" }               │
│                                                                   │
│  4. Event-Carried State Transfer (상태 전달 이벤트)              │
│     - 전체 상태 포함                                             │
│     - API 호출 없이 처리 가능                                    │
│     - 예: { "orderId": 123, "items": [...], "total": 50000 }    │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

---

## 이벤트 소싱 (Event Sourcing)

### 개념

```
┌──────────────────────────────────────────────────────────────────┐
│                    이벤트 소싱 vs 전통적 저장                     │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  전통적 방식 (State Sourcing):                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  Orders 테이블                                               │ │
│  │  ┌──────┬────────┬────────┬─────────────────────────────┐  │ │
│  │  │  id  │ status │ amount │ ... (현재 상태만 저장)       │  │ │
│  │  ├──────┼────────┼────────┼─────────────────────────────┤  │ │
│  │  │  1   │SHIPPED │ 50000  │ (어떻게 이 상태가 되었는지?) │  │ │
│  │  └──────┴────────┴────────┴─────────────────────────────┘  │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  이벤트 소싱:                                                    │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  Events 테이블 (이벤트 스토어)                               │ │
│  │  ┌──────┬───────────────────┬──────────────────────────────┐│ │
│  │  │  id  │ event_type        │ event_data                   ││ │
│  │  ├──────┼───────────────────┼──────────────────────────────┤│ │
│  │  │  1   │ OrderCreated      │ { "orderId": 1, ... }       ││ │
│  │  │  2   │ OrderPaid         │ { "orderId": 1, "amount": } ││ │
│  │  │  3   │ OrderShipped      │ { "orderId": 1, ... }       ││ │
│  │  └──────┴───────────────────┴──────────────────────────────┘│ │
│  │                                                              │ │
│  │  현재 상태 = Σ(이벤트) → 재생으로 상태 복원                   │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 이벤트 소싱 구현

```java
// 도메인 이벤트 기본 클래스
@Getter
public abstract class DomainEvent {
    private final String aggregateId;
    private final LocalDateTime occurredAt;
    private final int version;

    protected DomainEvent(String aggregateId, int version) {
        this.aggregateId = aggregateId;
        this.occurredAt = LocalDateTime.now();
        this.version = version;
    }
}

// 주문 이벤트들
public class OrderCreatedEvent extends DomainEvent {
    private final Long customerId;
    private final List<OrderItem> items;
    private final BigDecimal totalAmount;
    // ...
}

public class OrderPaidEvent extends DomainEvent {
    private final String paymentId;
    private final BigDecimal amount;
    // ...
}

public class OrderShippedEvent extends DomainEvent {
    private final String trackingNumber;
    // ...
}

// Aggregate (이벤트로부터 상태 복원)
public class Order {
    private Long id;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private List<OrderItem> items;
    private int version;

    private List<DomainEvent> uncommittedEvents = new ArrayList<>();

    // 팩토리 메서드 - 새 주문 생성
    public static Order create(Long customerId, List<OrderItem> items) {
        Order order = new Order();
        BigDecimal total = items.stream()
            .map(OrderItem::getPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 이벤트 발생
        order.apply(new OrderCreatedEvent(order.id.toString(), 0, customerId, items, total));

        return order;
    }

    // 이벤트 적용 (상태 변경)
    public void apply(DomainEvent event) {
        // 이벤트에 따라 상태 변경
        if (event instanceof OrderCreatedEvent e) {
            this.id = Long.parseLong(e.getAggregateId());
            this.status = OrderStatus.CREATED;
            this.items = e.getItems();
            this.totalAmount = e.getTotalAmount();
        } else if (event instanceof OrderPaidEvent e) {
            this.status = OrderStatus.PAID;
        } else if (event instanceof OrderShippedEvent e) {
            this.status = OrderStatus.SHIPPED;
        }

        this.version = event.getVersion();
        this.uncommittedEvents.add(event);
    }

    // 이벤트 히스토리로부터 상태 복원
    public static Order fromHistory(List<DomainEvent> events) {
        Order order = new Order();
        for (DomainEvent event : events) {
            order.apply(event);
        }
        order.uncommittedEvents.clear();  // 히스토리에서 복원 시 커밋된 이벤트
        return order;
    }

    // 명령 처리
    public void pay(String paymentId, BigDecimal amount) {
        if (this.status != OrderStatus.CREATED) {
            throw new IllegalStateException("Can only pay created orders");
        }
        apply(new OrderPaidEvent(this.id.toString(), this.version + 1, paymentId, amount));
    }
}

// 이벤트 스토어
@Repository
public class EventStore {

    private final JdbcTemplate jdbcTemplate;

    public void save(String aggregateId, List<DomainEvent> events, int expectedVersion) {
        // 낙관적 동시성 제어
        int currentVersion = getCurrentVersion(aggregateId);
        if (currentVersion != expectedVersion) {
            throw new ConcurrencyException("Aggregate modified by another transaction");
        }

        for (DomainEvent event : events) {
            jdbcTemplate.update(
                "INSERT INTO events (aggregate_id, event_type, event_data, version, occurred_at) VALUES (?, ?, ?, ?, ?)",
                aggregateId,
                event.getClass().getSimpleName(),
                objectMapper.writeValueAsString(event),
                event.getVersion(),
                event.getOccurredAt()
            );
        }
    }

    public List<DomainEvent> getEvents(String aggregateId) {
        return jdbcTemplate.query(
            "SELECT * FROM events WHERE aggregate_id = ? ORDER BY version",
            (rs, rowNum) -> deserializeEvent(rs),
            aggregateId
        );
    }
}
```

### 스냅샷 (Snapshot)

```java
// 이벤트가 많을 때 성능 최적화
@Service
public class OrderRepository {

    private final EventStore eventStore;
    private final SnapshotStore snapshotStore;

    private static final int SNAPSHOT_THRESHOLD = 100;

    public Order findById(Long orderId) {
        String aggregateId = orderId.toString();

        // 1. 스냅샷 조회
        Optional<Snapshot> snapshot = snapshotStore.getLatest(aggregateId);

        // 2. 스냅샷 이후 이벤트만 조회
        int fromVersion = snapshot.map(Snapshot::getVersion).orElse(0);
        List<DomainEvent> events = eventStore.getEventsAfter(aggregateId, fromVersion);

        // 3. 상태 복원
        Order order = snapshot
            .map(s -> objectMapper.readValue(s.getData(), Order.class))
            .orElse(new Order());

        for (DomainEvent event : events) {
            order.apply(event);
        }

        return order;
    }

    public void save(Order order) {
        List<DomainEvent> uncommittedEvents = order.getUncommittedEvents();
        eventStore.save(order.getId().toString(), uncommittedEvents, order.getVersion() - uncommittedEvents.size());

        // 스냅샷 생성 (일정 이벤트 수 초과 시)
        if (order.getVersion() % SNAPSHOT_THRESHOLD == 0) {
            snapshotStore.save(new Snapshot(
                order.getId().toString(),
                order.getVersion(),
                objectMapper.writeValueAsString(order)
            ));
        }
    }
}
```

---

## CQRS

### CQRS (Command Query Responsibility Segregation)

```
┌──────────────────────────────────────────────────────────────────┐
│                    CQRS 패턴                                      │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  명령(Command)과 조회(Query)를 분리                              │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                        Client                                │ │
│  └─────────────────────────────────────────────────────────────┘ │
│            │                                    │                 │
│            │ Commands                           │ Queries         │
│            │ (Create, Update, Delete)           │ (Read)          │
│            ▼                                    ▼                 │
│  ┌──────────────────────┐          ┌──────────────────────┐     │
│  │   Command Handler    │          │   Query Handler      │     │
│  │   (Write Model)      │          │   (Read Model)       │     │
│  └──────────┬───────────┘          └──────────┬───────────┘     │
│             │                                  │                  │
│             ▼                                  ▼                  │
│  ┌──────────────────────┐          ┌──────────────────────┐     │
│  │   Write Database     │ ──────►  │   Read Database      │     │
│  │   (정규화, 일관성)    │  동기화   │   (비정규화, 최적화)  │     │
│  │   PostgreSQL         │  이벤트   │   Elasticsearch,     │     │
│  │                      │          │   Redis, MongoDB     │     │
│  └──────────────────────┘          └──────────────────────┘     │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### CQRS 구현

```java
// Command Side
@Getter
@AllArgsConstructor
public class CreateOrderCommand {
    private final Long customerId;
    private final List<OrderItemDto> items;
}

@Service
@RequiredArgsConstructor
public class OrderCommandHandler {

    private final OrderRepository orderRepository;  // Write DB
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long handle(CreateOrderCommand command) {
        // 비즈니스 로직 수행
        Order order = Order.create(command.getCustomerId(), command.getItems());
        orderRepository.save(order);

        // 도메인 이벤트 발행 (Read 모델 동기화용)
        eventPublisher.publishEvent(new OrderCreatedEvent(order));

        return order.getId();
    }
}

// Query Side
@Getter
@Document(collection = "order_views")  // MongoDB
public class OrderView {
    @Id
    private String id;
    private Long orderId;
    private String customerName;
    private String status;
    private BigDecimal totalAmount;
    private List<OrderItemView> items;
    private LocalDateTime createdAt;
}

@Service
@RequiredArgsConstructor
public class OrderQueryHandler {

    private final OrderViewRepository orderViewRepository;  // Read DB

    public OrderView getOrder(Long orderId) {
        return orderViewRepository.findByOrderId(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    public Page<OrderView> searchOrders(OrderSearchCriteria criteria, Pageable pageable) {
        // 복잡한 검색은 Read 모델에서 최적화
        return orderViewRepository.search(criteria, pageable);
    }
}

// Read 모델 동기화 (Event Handler)
@Component
@RequiredArgsConstructor
public class OrderViewUpdater {

    private final OrderViewRepository orderViewRepository;
    private final CustomerClient customerClient;

    @EventListener
    @Async
    public void on(OrderCreatedEvent event) {
        Order order = event.getOrder();
        CustomerDto customer = customerClient.getCustomer(order.getCustomerId());

        OrderView view = new OrderView();
        view.setOrderId(order.getId());
        view.setCustomerName(customer.getName());
        view.setStatus(order.getStatus().name());
        view.setTotalAmount(order.getTotalAmount());
        view.setItems(mapItems(order.getItems()));
        view.setCreatedAt(order.getCreatedAt());

        orderViewRepository.save(view);
    }

    @EventListener
    @Async
    public void on(OrderStatusChangedEvent event) {
        orderViewRepository.findByOrderId(event.getOrderId())
            .ifPresent(view -> {
                view.setStatus(event.getNewStatus().name());
                orderViewRepository.save(view);
            });
    }
}
```

---

## 멱등성 보장

### 멱등성이 중요한 이유

```
┌──────────────────────────────────────────────────────────────────┐
│                    메시지 재처리 상황                             │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Producer          Broker          Consumer                       │
│      │                │                │                          │
│      │   메시지 발행   │                │                          │
│      │──────────────►│                │                          │
│      │                │   메시지 전달   │                          │
│      │                │──────────────►│                          │
│      │                │                │  처리 완료               │
│      │                │                │                          │
│      │                │   ACK (실패!)   │                          │
│      │                │◄──────────────│  ← 네트워크 오류         │
│      │                │                │                          │
│      │                │   재전달       │                          │
│      │                │──────────────►│  ← 중복 처리!            │
│      │                │                │                          │
│                                                                   │
│  해결책: 멱등성 보장 - 여러 번 처리해도 결과 동일                 │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 멱등성 구현 패턴

```java
// 1. Idempotency Key 패턴
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {
    @Id
    private String eventId;
    private LocalDateTime processedAt;
}

@Service
@RequiredArgsConstructor
public class IdempotentEventHandler {

    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public <T> void handleEvent(String eventId, Supplier<T> handler) {
        // 이미 처리된 이벤트인지 확인
        if (processedEventRepository.existsById(eventId)) {
            log.info("Event already processed: {}", eventId);
            return;
        }

        // 이벤트 처리
        handler.get();

        // 처리 완료 기록
        processedEventRepository.save(new ProcessedEvent(eventId, LocalDateTime.now()));
    }
}

// 사용 예시
@KafkaListener(topics = "order-events")
public void handleOrderEvent(@Payload OrderEvent event,
                             @Header(KafkaHeaders.RECEIVED_KEY) String key) {
    idempotentHandler.handleEvent(key, () -> {
        orderService.processOrder(event);
        return null;
    });
}

// 2. 자연 멱등성 - 상태 확인 후 처리
@Service
public class PaymentService {

    @Transactional
    public void processPayment(Long orderId, BigDecimal amount) {
        Order order = orderRepository.findById(orderId).orElseThrow();

        // 이미 결제된 주문이면 무시 (멱등성)
        if (order.getStatus() == OrderStatus.PAID) {
            log.info("Order already paid: {}", orderId);
            return;
        }

        // 결제 처리
        paymentGateway.charge(order.getPaymentInfo(), amount);
        order.markAsPaid();
        orderRepository.save(order);
    }
}

// 3. Optimistic Locking으로 멱등성
@Entity
public class Account {
    @Id
    private Long id;

    @Version
    private Long version;  // 낙관적 락

    private BigDecimal balance;

    public void withdraw(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }
        this.balance = this.balance.subtract(amount);
    }
}

// 동시에 같은 요청이 오면 OptimisticLockException 발생
// → 재시도 시 이미 처리됨을 확인
```

### Outbox 패턴

```java
// Transactional Outbox 패턴
// DB 트랜잭션과 메시지 발행의 원자성 보장

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String aggregateType;
    private String aggregateId;
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;  // PENDING, PUBLISHED

    private LocalDateTime createdAt;
}

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;

    @Transactional
    public Order createOrder(OrderRequest request) {
        // 1. 주문 생성
        Order order = Order.create(request);
        orderRepository.save(order);

        // 2. Outbox에 이벤트 저장 (같은 트랜잭션)
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setAggregateType("Order");
        outboxEvent.setAggregateId(order.getId().toString());
        outboxEvent.setEventType("OrderCreated");
        outboxEvent.setPayload(objectMapper.writeValueAsString(
            new OrderCreatedEvent(order)));
        outboxEvent.setStatus(OutboxStatus.PENDING);
        outboxRepository.save(outboxEvent);

        return order;
        // 트랜잭션 커밋 → 주문과 이벤트 모두 저장 또는 롤백
    }
}

// Outbox 폴링 (별도 스케줄러)
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedRate = 1000)  // 1초마다
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository
            .findByStatus(OutboxStatus.PENDING);

        for (OutboxEvent event : pendingEvents) {
            try {
                kafkaTemplate.send(
                    event.getAggregateType().toLowerCase() + "-events",
                    event.getAggregateId(),
                    event.getPayload()
                ).get();  // 동기 전송

                event.setStatus(OutboxStatus.PUBLISHED);
                outboxRepository.save(event);

            } catch (Exception e) {
                log.error("Failed to publish event: {}", event.getId(), e);
            }
        }
    }
}
```

---

## 구현 패턴

### Event Sourcing + CQRS 조합

```
┌──────────────────────────────────────────────────────────────────┐
│                Event Sourcing + CQRS                             │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Command                          Query                           │
│     │                                │                            │
│     ▼                                ▼                            │
│  ┌──────────────┐              ┌──────────────┐                  │
│  │   Command    │              │    Query     │                  │
│  │   Handler    │              │   Handler    │                  │
│  └──────┬───────┘              └──────┬───────┘                  │
│         │                             │                           │
│         ▼                             ▼                           │
│  ┌──────────────┐              ┌──────────────┐                  │
│  │   Domain     │              │   Read       │                  │
│  │  Aggregate   │              │   Model      │                  │
│  └──────┬───────┘              └──────────────┘                  │
│         │                             ▲                           │
│         │ 이벤트                      │ 구독                      │
│         ▼                             │                           │
│  ┌──────────────┐                     │                           │
│  │   Event      │─────────────────────┘                          │
│  │   Store      │                                                │
│  └──────────────┘                                                │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

---

## 핵심 정리

### 패턴 비교

| 패턴 | 목적 | 복잡도 | 사용 사례 |
|------|------|--------|----------|
| Event-Driven | 서비스 디커플링 | 중간 | MSA 통신 |
| Event Sourcing | 전체 히스토리 보존 | 높음 | 감사, 시간 여행 |
| CQRS | 읽기/쓰기 최적화 | 중간 | 고성능 조회 |
| Outbox | 메시지 전달 보장 | 중간 | 신뢰성 필요 |

### 설계 체크리스트

```
□ 이벤트 스키마 버전 관리
□ 멱등성 처리 메커니즘
□ 이벤트 순서 보장 (필요 시)
□ 실패 이벤트 재처리 (DLQ)
□ 스냅샷 전략 (Event Sourcing)
□ Read 모델 동기화 지연 허용
□ 모니터링 및 알림
```

### 면접 대비 핵심 질문

1. **Q: 이벤트 소싱의 장단점은?**
   - A: 장점은 전체 히스토리 보존, 감사 추적, 시간 여행 디버깅. 단점은 복잡도 증가, 이벤트 스키마 진화 어려움, 조회 성능 (스냅샷 필요)

2. **Q: CQRS를 사용하는 이유는?**
   - A: 읽기와 쓰기의 요구사항이 다를 때. 쓰기는 정규화/일관성, 읽기는 비정규화/성능 최적화. 각각 다른 DB 사용 가능

3. **Q: 멱등성을 보장하는 방법은?**
   - A: Idempotency Key로 중복 체크, 상태 확인 후 처리, Optimistic Locking. Outbox 패턴으로 정확히 한 번(Exactly-once) 유사 구현

4. **Q: 이벤트 기반 아키텍처의 데이터 일관성은?**
   - A: 최종 일관성(Eventual Consistency)만 보장. 이벤트 처리 지연 발생 가능. 즉시 일관성 필요 시 동기 호출 또는 Saga 패턴

---

*마지막 업데이트: 2025년 01월*
