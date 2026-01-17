# MSA 면접 핵심 질문 정리

5년차 백엔드 개발자 면접에서 자주 등장하는 MSA(Microservices Architecture) 핵심 질문과 답변을 정리합니다.

## 목차

1. [분산 트랜잭션](#1-분산-트랜잭션)
2. [CAP 이론](#2-cap-이론)
3. [이벤추얼 컨시스턴시](#3-이벤추얼-컨시스턴시-eventual-consistency)
4. [서비스 간 통신](#4-서비스-간-통신)
5. [서비스 디스커버리](#5-서비스-디스커버리)
6. [장애 격리 패턴](#6-장애-격리-패턴)

---

## 1. 분산 트랜잭션

### Q: MSA에서 분산 트랜잭션 문제는 무엇이고 어떻게 해결하나요?

**문제 상황**
```
모놀리식 시스템:
┌─────────────────────────────────────┐
│  @Transactional                     │
│  주문 생성 → 재고 차감 → 결제 처리  │  ← 하나의 DB 트랜잭션
└─────────────────────────────────────┘

MSA 시스템:
┌──────────┐   ┌──────────┐   ┌──────────┐
│ Order    │   │ Stock    │   │ Payment  │
│ Service  │ → │ Service  │ → │ Service  │  ← 각각 다른 DB
│ (DB1)    │   │ (DB2)    │   │ (DB3)    │
└──────────┘   └──────────┘   └──────────┘
     ↓              ↓              ↓
   주문 O        재고 O        결제 X  ← 부분 실패 발생!
```

**2PC (Two-Phase Commit) - 전통적 방식**
```
Phase 1 (Prepare):
Coordinator → 모든 참가자에게 "커밋 준비 됐나요?"
참가자들 → "Ready" 응답

Phase 2 (Commit/Rollback):
Coordinator → 모든 참가자에게 "Commit!" 또는 "Rollback!"

단점:
- 코디네이터 SPOF (Single Point of Failure)
- 락 대기로 인한 성능 저하
- MSA에서는 거의 사용 안 함
```

**Saga 패턴 - MSA 권장 방식**
```java
// Choreography (이벤트 기반)
Order Service: 주문 생성 → OrderCreated 이벤트 발행
                                     ↓
Stock Service: 재고 차감 → StockReserved 이벤트 발행
                                     ↓
Payment Service: 결제 처리 → PaymentCompleted 이벤트 발행

// 실패 시 보상 트랜잭션
Payment Service: 결제 실패 → PaymentFailed 이벤트 발행
                                     ↓
Stock Service: 재고 복구 → StockReleased 이벤트 발행
                                     ↓
Order Service: 주문 취소
```

```java
// Orchestration (중앙 제어)
@Service
public class OrderSagaOrchestrator {

    public void createOrder(OrderRequest request) {
        try {
            // Step 1: 주문 생성
            Order order = orderService.create(request);

            // Step 2: 재고 예약
            stockService.reserve(order.getItems());

            // Step 3: 결제 처리
            paymentService.process(order.getPayment());

            // Step 4: 주문 확정
            orderService.confirm(order.getId());

        } catch (StockException e) {
            // 보상: 주문 취소
            orderService.cancel(order.getId());

        } catch (PaymentException e) {
            // 보상: 재고 복구 → 주문 취소
            stockService.release(order.getItems());
            orderService.cancel(order.getId());
        }
    }
}
```

| 방식 | 장점 | 단점 |
|------|------|------|
| Choreography | 느슨한 결합, 단순한 구조 | 흐름 추적 어려움, 순환 의존 가능 |
| Orchestration | 명확한 흐름, 중앙 관리 | Orchestrator가 SPOF, 강한 결합 |

---

## 2. CAP 이론

### Q: CAP 이론을 설명하고 실제 시스템 예를 들어주세요.

**CAP 정의**
```
C (Consistency): 모든 노드가 동일한 데이터를 봄
A (Availability): 모든 요청이 응답을 받음 (성공/실패)
P (Partition Tolerance): 네트워크 분할에도 동작

분산 시스템에서 P는 필수 → C와 A 중 하나를 선택
```

**CP vs AP 시스템**
```
┌─────────────────────────────────────────────────────────────┐
│                    네트워크 파티션 발생 시                   │
├──────────────────────┬──────────────────────────────────────┤
│        CP 시스템      │           AP 시스템                  │
├──────────────────────┼──────────────────────────────────────┤
│ - 일관성 우선         │ - 가용성 우선                        │
│ - 일부 요청 거부      │ - 모든 요청 수락                     │
│ - 예: 은행 잔액       │ - 예: SNS 좋아요 수                  │
│                      │ - 나중에 데이터 동기화                │
└──────────────────────┴──────────────────────────────────────┘
```

**실제 시스템 분류**

| 시스템 | 유형 | 이유 |
|--------|------|------|
| MySQL (Single) | CA | 분산 아님, 파티션 없음 |
| MySQL Cluster | CP | 일관성 우선, 쓰기 불가 상황 |
| MongoDB | CP | 기본 설정에서 Primary 우선 |
| Cassandra | AP | 가용성 우선, 튜닝 가능 |
| DynamoDB | AP | 기본 Eventually Consistent |
| Redis Cluster | CP | 마스터 장애 시 대기 |
| Zookeeper | CP | 분산 코디네이션, 일관성 필수 |

```java
// DynamoDB - Consistent Read 옵션
// AP (기본) → CP로 전환 가능
GetItemRequest request = GetItemRequest.builder()
    .tableName("Orders")
    .key(Map.of("orderId", AttributeValue.builder().s(id).build()))
    .consistentRead(true)  // Strong Consistency
    .build();
```

---

## 3. 이벤추얼 컨시스턴시 (Eventual Consistency)

### Q: 이벤추얼 컨시스턴시란 무엇이고 어떻게 구현하나요?

**정의**: 일시적 불일치를 허용하되, 시간이 지나면 모든 노드가 일관된 상태에 도달

```
┌──────────┐     ┌──────────┐     ┌──────────┐
│  Node A  │     │  Node B  │     │  Node C  │
│ balance: │     │ balance: │     │ balance: │
│   100    │     │   100    │     │   100    │
└──────────┘     └──────────┘     └──────────┘
     │
     ↓ Write: balance = 150
┌──────────┐     ┌──────────┐     ┌──────────┐
│  Node A  │     │  Node B  │     │  Node C  │
│ balance: │     │ balance: │     │ balance: │
│   150    │     │   100    │     │   100    │  ← 일시적 불일치
└──────────┘     └──────────┘     └──────────┘
     │
     ↓ 복제 완료 (Eventually)
┌──────────┐     ┌──────────┐     ┌──────────┐
│  Node A  │     │  Node B  │     │  Node C  │
│ balance: │     │ balance: │     │ balance: │
│   150    │     │   150    │     │   150    │  ← 최종 일관성
└──────────┘     └──────────┘     └──────────┘
```

**구현 패턴**

```java
// 1. 이벤트 소싱으로 Eventually Consistent 구현
@Service
public class OrderEventHandler {

    @KafkaListener(topics = "order-events")
    public void handle(OrderEvent event) {
        switch (event.getType()) {
            case ORDER_CREATED:
                // 재고 서비스에서 주문 정보 동기화
                updateLocalOrderCache(event);
                break;
            case ORDER_CANCELLED:
                // 로컬 캐시 업데이트
                removeFromLocalCache(event.getOrderId());
                break;
        }
    }
}

// 2. 읽기/쓰기 분리 (CQRS)
// 쓰기: 마스터에 즉시 반영
// 읽기: 복제본에서 (약간의 지연 허용)

// 3. 멱등성 보장
@Transactional
public void processEvent(OrderEvent event) {
    // 이미 처리된 이벤트인지 확인
    if (eventLog.exists(event.getEventId())) {
        return;  // 중복 처리 방지
    }

    // 비즈니스 로직 실행
    orderRepository.save(event.toOrder());

    // 처리 완료 기록
    eventLog.save(event.getEventId());
}
```

**일관성 수준 선택 가이드**

| 사용 사례 | 일관성 수준 | 이유 |
|-----------|-------------|------|
| 계좌 잔액 | Strong | 정확성 필수 |
| 장바구니 | Session | 사용자 세션 내 일관성 |
| 좋아요 수 | Eventual | 정확도보다 성능 |
| 재고 수량 | Strong | 오버셀링 방지 |
| 추천 목록 | Eventual | 실시간 반영 불필요 |

---

## 4. 서비스 간 통신

### Q: 동기/비동기 통신의 차이와 선택 기준은?

**동기 통신 (Synchronous)**
```java
// REST API 호출
@Service
public class OrderService {

    private final RestTemplate restTemplate;

    public Order createOrder(OrderRequest request) {
        // 재고 서비스 동기 호출 (블로킹)
        StockResponse stock = restTemplate.getForObject(
            "http://stock-service/api/stocks/{productId}",
            StockResponse.class,
            request.getProductId()
        );

        if (stock.getQuantity() < request.getQuantity()) {
            throw new InsufficientStockException();
        }

        return orderRepository.save(request.toOrder());
    }
}

// gRPC (고성능 동기 통신)
@GrpcService
public class StockGrpcService extends StockServiceGrpc.StockServiceImplBase {
    @Override
    public void getStock(StockRequest request, StreamObserver<StockResponse> observer) {
        Stock stock = stockRepository.findById(request.getProductId());
        observer.onNext(StockResponse.newBuilder()
            .setQuantity(stock.getQuantity())
            .build());
        observer.onCompleted();
    }
}
```

**비동기 통신 (Asynchronous)**
```java
// 메시지 브로커 사용
@Service
public class OrderService {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public Order createOrder(OrderRequest request) {
        Order order = orderRepository.save(request.toOrder());

        // 이벤트 발행 (비블로킹)
        kafkaTemplate.send("order-events",
            new OrderCreatedEvent(order.getId(), order.getItems()));

        return order;
    }
}

@Service
public class StockEventConsumer {

    @KafkaListener(topics = "order-events", groupId = "stock-service")
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 비동기로 재고 처리
        stockService.reserveStock(event.getItems());
    }
}
```

**비교 및 선택 기준**

| 특성 | 동기 (REST/gRPC) | 비동기 (Message) |
|------|------------------|------------------|
| 응답 대기 | 필요 | 불필요 |
| 결합도 | 높음 (직접 의존) | 낮음 (브로커 통해) |
| 장애 전파 | O | X (버퍼링) |
| 순서 보장 | 자연스러움 | 별도 처리 필요 |
| 디버깅 | 쉬움 | 어려움 |
| 적합 사례 | 즉각 응답 필요 | 배경 처리, 알림 |

```java
// 실시간 필요 → 동기
GET /api/products/{id}/stock  // 재고 확인 후 구매 버튼 활성화

// 실시간 불필요 → 비동기
POST /api/orders  // 주문 접수 후 결제/배송은 비동기
```

---

## 5. 서비스 디스커버리

### Q: 서비스 디스커버리가 필요한 이유와 구현 방식은?

**문제**: 동적으로 변하는 서비스 인스턴스 주소
```
Order Service가 Stock Service를 호출하려면?

정적 환경:
stock-service: 192.168.1.10:8080  ← 고정 IP

동적 환경 (컨테이너/k8s):
stock-service-1: 10.0.1.5:8080   ← 매번 변경
stock-service-2: 10.0.2.8:8080
stock-service-3: 10.0.3.12:8080
```

**Client-Side Discovery**
```
┌──────────────┐      ┌─────────────────────┐
│ Order Service│─────→│   Service Registry  │
│  (Client)    │ 조회  │  (Eureka/Consul)    │
└──────────────┘      └─────────────────────┘
       │                        ↑
       ↓ 직접 호출               │ 등록
┌──────────────┐                │
│ Stock Service│────────────────┘
└──────────────┘
```

```java
// Spring Cloud Netflix Eureka 예시
// 1. Eureka Server
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication { }

// 2. Client 등록
@SpringBootApplication
@EnableEurekaClient
public class StockServiceApplication { }

// application.yml
eureka:
  client:
    service-url:
      defaultZone: http://eureka:8761/eureka/

// 3. 서비스 호출 (로드밸런싱 포함)
@FeignClient(name = "stock-service")
public interface StockClient {
    @GetMapping("/api/stocks/{productId}")
    StockResponse getStock(@PathVariable Long productId);
}
```

**Server-Side Discovery**
```
┌──────────────┐      ┌────────────────┐      ┌──────────────┐
│ Order Service│─────→│  Load Balancer │─────→│ Stock Service│
└──────────────┘      │  (Kubernetes)  │      └──────────────┘
                      └────────────────┘
                             ↑
                      Service Registry
                      (kube-dns, etc.)
```

```yaml
# Kubernetes Service (Server-Side Discovery)
apiVersion: v1
kind: Service
metadata:
  name: stock-service
spec:
  selector:
    app: stock
  ports:
    - port: 8080
  type: ClusterIP

# 다른 Pod에서 접근
# http://stock-service:8080/api/stocks/1
# Kubernetes가 자동으로 로드밸런싱
```

| 방식 | 장점 | 단점 |
|------|------|------|
| Client-Side | 클라이언트가 LB 제어, 유연함 | 클라이언트 복잡성 증가 |
| Server-Side | 클라이언트 단순, 언어 무관 | 추가 인프라 필요 |

---

## 6. 장애 격리 패턴

### Q: Circuit Breaker 패턴은 무엇이고 왜 필요한가요?

**문제: 장애 전파 (Cascading Failure)**
```
User → Order Service → Stock Service (장애 발생!)
                 ↓
        스레드 풀 고갈
        응답 지연 누적
                 ↓
        Order Service 장애
                 ↓
        전체 시스템 다운
```

**Circuit Breaker 상태 전이**
```
┌────────────────────────────────────────────────────────────┐
│                                                            │
│  ┌─────────┐   실패율 초과   ┌─────────┐   시간 경과       │
│  │ CLOSED  │───────────────→│  OPEN   │─────────────┐     │
│  │(정상 호출)│               │(호출 차단)│             ↓     │
│  └─────────┘               └─────────┘      ┌───────────┐ │
│       ↑                                     │ HALF_OPEN │ │
│       │         성공                        │(제한적 호출)│ │
│       └──────────────────────────────────────└───────────┘ │
│                                 실패 시 다시 OPEN           │
└────────────────────────────────────────────────────────────┘
```

**Resilience4j 구현**
```java
@Configuration
public class CircuitBreakerConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)           // 50% 실패 시 OPEN
            .slowCallRateThreshold(50)          // 50% 느린 호출 시 OPEN
            .slowCallDurationThreshold(Duration.ofSeconds(2))
            .waitDurationInOpenState(Duration.ofSeconds(30))  // OPEN 유지 시간
            .permittedNumberOfCallsInHalfOpenState(5)  // HALF_OPEN 시 허용 호출
            .slidingWindowSize(10)              // 최근 10개 요청 기준
            .build();

        return CircuitBreakerRegistry.of(config);
    }
}

@Service
public class OrderService {

    private final CircuitBreaker circuitBreaker;
    private final StockClient stockClient;

    public StockResponse getStock(Long productId) {
        return circuitBreaker.executeSupplier(() ->
            stockClient.getStock(productId)
        );
    }

    // 또는 어노테이션 방식
    @CircuitBreaker(name = "stockService", fallbackMethod = "getStockFallback")
    public StockResponse getStockWithAnnotation(Long productId) {
        return stockClient.getStock(productId);
    }

    public StockResponse getStockFallback(Long productId, Exception e) {
        // 기본값 반환 또는 캐시 사용
        return StockResponse.builder()
            .productId(productId)
            .available(false)
            .message("재고 확인 불가, 잠시 후 다시 시도해주세요")
            .build();
    }
}
```

**관련 패턴 비교**

| 패턴 | 목적 | 동작 |
|------|------|------|
| Circuit Breaker | 장애 전파 방지 | 실패 시 빠른 실패 반환 |
| Retry | 일시적 오류 복구 | 재시도 (with backoff) |
| Timeout | 무한 대기 방지 | 일정 시간 후 포기 |
| Bulkhead | 리소스 격리 | 스레드풀/세마포어 분리 |
| Rate Limiter | 과부하 방지 | 요청 수 제한 |

```java
// 패턴 조합 사용
@CircuitBreaker(name = "stock")
@Retry(name = "stock")
@RateLimiter(name = "stock")
@Bulkhead(name = "stock")
public StockResponse getStock(Long productId) {
    return stockClient.getStock(productId);
}
```

---

## 핵심 정리

| 주제 | 핵심 키워드 |
|------|-------------|
| 분산 트랜잭션 | Saga 패턴, Choreography/Orchestration, 보상 트랜잭션 |
| CAP | 네트워크 파티션 필수, CP vs AP, 트레이드오프 |
| Eventual Consistency | 일시적 불일치 허용, 멱등성, CQRS |
| 통신 | 동기(REST/gRPC) vs 비동기(Kafka), 용도에 맞게 선택 |
| 서비스 디스커버리 | Eureka/Consul, Client-Side vs Server-Side |
| 장애 격리 | Circuit Breaker, Fallback, Bulkhead |

---

*마지막 업데이트: 2025년 01월*
