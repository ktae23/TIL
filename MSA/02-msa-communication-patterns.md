# MSA 통신 패턴

## 목차
1. [동기 vs 비동기 통신](#동기-vs-비동기-통신)
2. [동기 통신 패턴](#동기-통신-패턴)
3. [비동기 통신 패턴](#비동기-통신-패턴)
4. [Saga 패턴 개요](#saga-패턴-개요)
5. [통신 패턴 선택 가이드](#통신-패턴-선택-가이드)
6. [핵심 정리](#핵심-정리)

---

## 동기 vs 비동기 통신

### 통신 방식 비교

```
┌──────────────────────────────────────────────────────────────────┐
│                    동기 vs 비동기 통신                            │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  동기 통신 (Synchronous):                                        │
│  ┌─────────┐         ┌─────────┐                                │
│  │ Service │ ──────► │ Service │                                │
│  │    A    │  요청   │    B    │                                │
│  │         │ ◄────── │         │                                │
│  │ (대기중) │  응답   │         │                                │
│  └─────────┘         └─────────┘                                │
│  → A는 B의 응답을 받을 때까지 대기                               │
│                                                                   │
│  비동기 통신 (Asynchronous):                                     │
│  ┌─────────┐  메시지  ┌─────────┐         ┌─────────┐           │
│  │ Service │ ───────► │ Message │ ───────► │ Service │           │
│  │    A    │         │  Broker │         │    B    │           │
│  │ (계속   │         │         │         │         │           │
│  │  진행)  │         └─────────┘         └─────────┘           │
│  └─────────┘                                                     │
│  → A는 메시지 발행 후 즉시 다른 작업 진행                        │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 비교 표

| 구분 | 동기 | 비동기 |
|------|------|--------|
| 응답 대기 | 필수 | 불필요 |
| 결합도 | 높음 (런타임 의존) | 낮음 |
| 장애 전파 | 연쇄 장애 가능 | 격리됨 |
| 구현 복잡도 | 낮음 | 높음 |
| 디버깅 | 쉬움 | 어려움 |
| 일관성 | 즉시 확인 | 최종 일관성 |
| 사용 사례 | 즉시 응답 필요 | 백그라운드 처리 |

---

## 동기 통신 패턴

### REST API

```java
// RestTemplate 사용 (전통적 방식)
@Service
@RequiredArgsConstructor
public class OrderService {

    private final RestTemplate restTemplate;

    public Order createOrder(OrderRequest request) {
        // 재고 서비스 호출
        InventoryResponse inventory = restTemplate.getForObject(
            "http://inventory-service/api/inventory/{productId}",
            InventoryResponse.class,
            request.getProductId()
        );

        if (inventory.getQuantity() < request.getQuantity()) {
            throw new InsufficientStockException();
        }

        // 주문 생성
        return orderRepository.save(Order.create(request));
    }
}

// WebClient 사용 (Reactive, 권장)
@Service
@RequiredArgsConstructor
public class OrderService {

    private final WebClient.Builder webClientBuilder;

    public Mono<Order> createOrder(OrderRequest request) {
        return webClientBuilder.build()
            .get()
            .uri("http://inventory-service/api/inventory/{id}", request.getProductId())
            .retrieve()
            .bodyToMono(InventoryResponse.class)
            .flatMap(inventory -> {
                if (inventory.getQuantity() < request.getQuantity()) {
                    return Mono.error(new InsufficientStockException());
                }
                return Mono.just(orderRepository.save(Order.create(request)));
            });
    }
}

// OpenFeign 사용 (선언적 방식)
@FeignClient(name = "inventory-service")
public interface InventoryClient {

    @GetMapping("/api/inventory/{productId}")
    InventoryResponse getInventory(@PathVariable Long productId);

    @PutMapping("/api/inventory/{productId}/decrease")
    void decreaseStock(@PathVariable Long productId, @RequestParam int quantity);
}

@Service
@RequiredArgsConstructor
public class OrderService {

    private final InventoryClient inventoryClient;

    public Order createOrder(OrderRequest request) {
        InventoryResponse inventory = inventoryClient.getInventory(request.getProductId());

        if (inventory.getQuantity() < request.getQuantity()) {
            throw new InsufficientStockException();
        }

        inventoryClient.decreaseStock(request.getProductId(), request.getQuantity());
        return orderRepository.save(Order.create(request));
    }
}
```

### gRPC

```protobuf
// inventory.proto
syntax = "proto3";

package inventory;

service InventoryService {
    rpc GetInventory (GetInventoryRequest) returns (InventoryResponse);
    rpc DecreaseStock (DecreaseStockRequest) returns (DecreaseStockResponse);
}

message GetInventoryRequest {
    int64 product_id = 1;
}

message InventoryResponse {
    int64 product_id = 1;
    int32 quantity = 2;
}

message DecreaseStockRequest {
    int64 product_id = 1;
    int32 quantity = 2;
}

message DecreaseStockResponse {
    bool success = 1;
}
```

```java
// gRPC 클라이언트
@Service
@RequiredArgsConstructor
public class InventoryGrpcClient {

    private final InventoryServiceGrpc.InventoryServiceBlockingStub blockingStub;

    public InventoryResponse getInventory(Long productId) {
        GetInventoryRequest request = GetInventoryRequest.newBuilder()
            .setProductId(productId)
            .build();

        return blockingStub.getInventory(request);
    }
}

// gRPC 서버
@GrpcService
public class InventoryGrpcService extends InventoryServiceGrpc.InventoryServiceImplBase {

    private final InventoryService inventoryService;

    @Override
    public void getInventory(GetInventoryRequest request,
                             StreamObserver<InventoryResponse> responseObserver) {
        Inventory inventory = inventoryService.findByProductId(request.getProductId());

        InventoryResponse response = InventoryResponse.newBuilder()
            .setProductId(inventory.getProductId())
            .setQuantity(inventory.getQuantity())
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
```

### REST vs gRPC

| 구분 | REST | gRPC |
|------|------|------|
| 프로토콜 | HTTP/1.1, HTTP/2 | HTTP/2 |
| 직렬화 | JSON, XML | Protocol Buffers |
| 성능 | 상대적 낮음 | 높음 |
| 브라우저 지원 | 직접 | gRPC-Web 필요 |
| 스트리밍 | 제한적 | 양방향 스트리밍 |
| 코드 생성 | 선택적 | 필수 |
| 학습 곡선 | 낮음 | 높음 |

---

## 비동기 통신 패턴

### 메시지 브로커 패턴

```
┌──────────────────────────────────────────────────────────────────┐
│                    메시지 패턴 종류                               │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  1. Point-to-Point (Queue):                                      │
│     Producer ──► [Queue] ──► Consumer                            │
│     - 하나의 메시지는 하나의 소비자만 처리                        │
│     - 예: 주문 처리                                              │
│                                                                   │
│  2. Publish-Subscribe (Topic):                                   │
│     Publisher ──► [Topic] ──┬► Subscriber 1                      │
│                             ├► Subscriber 2                      │
│                             └► Subscriber 3                      │
│     - 하나의 메시지를 여러 소비자가 수신                          │
│     - 예: 주문 완료 이벤트 브로드캐스트                           │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Apache Kafka 구현

```java
// 의존성
// implementation 'org.springframework.kafka:spring-kafka'

// Kafka 설정
@Configuration
public class KafkaConfig {

    @Bean
    public ProducerFactory<String, OrderEvent> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");  // 모든 복제본 확인
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, OrderEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}

// 이벤트 발행 (Producer)
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public void publishOrderCreated(Order order) {
        OrderEvent event = OrderEvent.builder()
            .eventType("ORDER_CREATED")
            .orderId(order.getId())
            .customerId(order.getCustomerId())
            .totalAmount(order.getTotalAmount())
            .timestamp(LocalDateTime.now())
            .build();

        kafkaTemplate.send("order-events", order.getId().toString(), event)
            .addCallback(
                result -> log.info("Order event published: {}", event.getOrderId()),
                ex -> log.error("Failed to publish order event", ex)
            );
    }
}

// 이벤트 구독 (Consumer)
@Service
@Slf4j
public class InventoryEventListener {

    private final InventoryService inventoryService;

    @KafkaListener(
        topics = "order-events",
        groupId = "inventory-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderEvent(OrderEvent event) {
        log.info("Received order event: {}", event);

        if ("ORDER_CREATED".equals(event.getEventType())) {
            try {
                inventoryService.reserveStock(event.getOrderId(), event.getItems());
            } catch (Exception e) {
                log.error("Failed to reserve stock for order: {}", event.getOrderId(), e);
                // 보상 이벤트 발행
            }
        }
    }
}

// Consumer 설정
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, OrderEvent> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "inventory-service");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderEvent>
            kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);  // 병렬 처리
        return factory;
    }
}
```

### RabbitMQ 구현

```java
// 의존성
// implementation 'org.springframework.boot:spring-boot-starter-amqp'

// RabbitMQ 설정
@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable("order.queue")
            .withArgument("x-dead-letter-exchange", "order.dlx")
            .withArgument("x-dead-letter-routing-key", "order.dlq")
            .build();
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange("order.exchange");
    }

    @Bean
    public Binding orderBinding(Queue orderQueue, TopicExchange orderExchange) {
        return BindingBuilder
            .bind(orderQueue)
            .to(orderExchange)
            .with("order.#");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

// 메시지 발행
@Service
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishOrderCreated(Order order) {
        OrderEvent event = OrderEvent.builder()
            .eventType("ORDER_CREATED")
            .orderId(order.getId())
            .build();

        rabbitTemplate.convertAndSend(
            "order.exchange",
            "order.created",
            event
        );
    }
}

// 메시지 구독
@Service
@Slf4j
public class OrderEventListener {

    @RabbitListener(queues = "order.queue")
    public void handleOrderEvent(OrderEvent event) {
        log.info("Received order event: {}", event);
        // 처리 로직
    }
}
```

---

## Saga 패턴 개요

### Saga 패턴 필요성

```
┌──────────────────────────────────────────────────────────────────┐
│                    분산 트랜잭션 문제                             │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  전통적인 2PC (Two-Phase Commit):                                │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  Coordinator                                                 │ │
│  │      │                                                       │ │
│  │      ├─ Prepare ──► Service A ✓                             │ │
│  │      ├─ Prepare ──► Service B ✓                             │ │
│  │      ├─ Prepare ──► Service C ✗ (실패)                      │ │
│  │      │                                                       │ │
│  │      └─ Rollback All                                        │ │
│  │                                                              │ │
│  │  문제점:                                                     │ │
│  │  - 동기적, 블로킹                                           │ │
│  │  - 단일 장애점 (Coordinator)                                │ │
│  │  - 성능 저하, 가용성 감소                                    │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  Saga 패턴:                                                      │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  T1 ──► T2 ──► T3 (실패)                                    │ │
│  │                   │                                          │ │
│  │                   ▼                                          │ │
│  │        C3 ◄── C2 ◄── C1 (보상 트랜잭션)                      │ │
│  │                                                              │ │
│  │  장점:                                                       │ │
│  │  - 비동기, 논블로킹                                         │ │
│  │  - 높은 가용성                                              │ │
│  │  - 각 서비스 자율성 유지                                     │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 주문 Saga 예시

```
┌──────────────────────────────────────────────────────────────────┐
│                    주문 처리 Saga                                 │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  정상 흐름:                                                      │
│                                                                   │
│  ┌─────────┐    ┌───────────┐    ┌─────────┐    ┌──────────┐   │
│  │  주문   │───►│   재고    │───►│  결제   │───►│   배송   │   │
│  │  생성   │    │   차감    │    │  처리   │    │   요청   │   │
│  └─────────┘    └───────────┘    └─────────┘    └──────────┘   │
│                                                                   │
│  실패 시 보상:                                                   │
│                                                                   │
│  ┌─────────┐    ┌───────────┐    ┌─────────┐                    │
│  │  주문   │◄───│   재고    │◄───│  결제   │ ✗ 실패            │
│  │  취소   │    │   복구    │    │  실패   │                    │
│  └─────────┘    └───────────┘    └─────────┘                    │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

```java
// 주문 상태 머신
public enum OrderStatus {
    PENDING,
    INVENTORY_RESERVED,
    PAYMENT_COMPLETED,
    SHIPPING_REQUESTED,
    COMPLETED,
    // 보상 상태
    PAYMENT_FAILED,
    INVENTORY_RELEASED,
    CANCELLED
}

// Saga 이벤트
@Getter
public abstract class OrderSagaEvent {
    private final Long orderId;
    private final LocalDateTime timestamp;

    protected OrderSagaEvent(Long orderId) {
        this.orderId = orderId;
        this.timestamp = LocalDateTime.now();
    }
}

public class OrderCreatedEvent extends OrderSagaEvent {
    private final List<OrderItem> items;
    private final Long customerId;
    // ...
}

public class InventoryReservedEvent extends OrderSagaEvent {
    private final Long orderId;
    // ...
}

public class PaymentCompletedEvent extends OrderSagaEvent {
    private final String transactionId;
    // ...
}

// 보상 이벤트
public class PaymentFailedEvent extends OrderSagaEvent {
    private final String reason;
    // ...
}

public class InventoryReleaseRequestedEvent extends OrderSagaEvent {
    // ...
}
```

---

## 통신 패턴 선택 가이드

### 선택 기준

```
┌──────────────────────────────────────────────────────────────────┐
│                    통신 패턴 선택 가이드                          │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  동기 통신 사용:                                                 │
│  ✅ 즉시 응답이 필요한 경우                                      │
│  ✅ 간단한 요청-응답 패턴                                        │
│  ✅ 데이터 일관성이 중요한 경우                                   │
│  ✅ 호출 체인이 짧은 경우 (2~3개 서비스)                         │
│                                                                   │
│  비동기 통신 사용:                                               │
│  ✅ 긴 처리 시간이 예상되는 경우                                 │
│  ✅ 여러 서비스에 알림이 필요한 경우                             │
│  ✅ 서비스 간 결합도를 낮추고 싶은 경우                          │
│  ✅ 일시적 장애에 대한 내성이 필요한 경우                        │
│  ✅ 피크 부하 처리가 필요한 경우                                 │
│                                                                   │
│  REST vs gRPC:                                                   │
│  REST: 외부 API, 브라우저 클라이언트, 간단한 CRUD               │
│  gRPC: 내부 서비스 간 통신, 고성능 필요, 스트리밍                │
│                                                                   │
│  Kafka vs RabbitMQ:                                              │
│  Kafka: 대용량, 순서 보장, 이벤트 소싱, 로그                     │
│  RabbitMQ: 복잡한 라우팅, 낮은 지연, 전통적 메시징              │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 하이브리드 접근

```java
// 동기 + 비동기 조합 예시
@Service
@RequiredArgsConstructor
public class OrderService {

    private final InventoryClient inventoryClient;  // 동기 (Feign)
    private final PaymentClient paymentClient;      // 동기 (Feign)
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;  // 비동기

    @Transactional
    public Order createOrder(OrderRequest request) {
        // 1. 재고 확인 - 동기 (즉시 확인 필요)
        InventoryResponse inventory = inventoryClient.checkAndReserve(
            request.getProductId(),
            request.getQuantity()
        );

        // 2. 결제 처리 - 동기 (결과 즉시 필요)
        PaymentResponse payment = paymentClient.processPayment(
            request.getPaymentInfo()
        );

        // 3. 주문 생성
        Order order = orderRepository.save(Order.create(request, payment.getTransactionId()));

        // 4. 알림 발송 - 비동기 (즉시 응답 불필요)
        kafkaTemplate.send("order-events", new OrderCreatedEvent(order));

        // 5. 분석/로깅 - 비동기 (백그라운드 처리)
        kafkaTemplate.send("analytics-events", new OrderAnalyticsEvent(order));

        return order;
    }
}
```

---

## 핵심 정리

### 통신 패턴 비교

| 패턴 | 결합도 | 가용성 | 복잡도 | 일관성 |
|------|--------|--------|--------|--------|
| REST | 높음 | 낮음 | 낮음 | 강함 |
| gRPC | 높음 | 낮음 | 중간 | 강함 |
| 메시지 큐 | 낮음 | 높음 | 높음 | 최종적 |
| 이벤트 | 낮음 | 높음 | 높음 | 최종적 |

### 패턴 선택 체크리스트

```
□ 즉시 응답 필요? → 동기
□ 장애 격리 필요? → 비동기
□ 여러 소비자 필요? → Pub/Sub
□ 순서 보장 필요? → Kafka
□ 복잡한 라우팅? → RabbitMQ
□ 고성능 내부 통신? → gRPC
□ 외부 API? → REST
```

### 실무 기반 핵심 질문

1. **Q: 동기와 비동기 통신의 장단점은?**
   - A: 동기는 즉시 응답, 간단한 구현이 장점이나 높은 결합도, 연쇄 장애 위험. 비동기는 낮은 결합도, 높은 가용성이 장점이나 복잡한 구현, 최종 일관성만 보장

2. **Q: Kafka와 RabbitMQ의 차이점은?**
   - A: Kafka는 분산 로그, 대용량, 순서 보장, 이벤트 재처리 가능. RabbitMQ는 전통적 메시지 브로커, 복잡한 라우팅, 메시지 확인(ACK) 중심

3. **Q: Saga 패턴이 필요한 이유는?**
   - A: MSA에서는 분산 트랜잭션(2PC)이 어려움. Saga는 로컬 트랜잭션 + 보상 트랜잭션으로 최종 일관성 달성. 비동기적이고 확장성 좋음

4. **Q: 서비스 간 통신에서 장애를 어떻게 처리하나요?**
   - A: 동기: Circuit Breaker, Retry, Timeout 적용. 비동기: 메시지 큐의 재시도, DLQ(Dead Letter Queue) 활용, 멱등성 보장

---

*마지막 업데이트: 2026년 01월*
