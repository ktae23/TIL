# 모듈러 모놀리스(Modular Monolith)와 Spring Modulith

모듈러 모놀리스는 모놀리스의 단순함과 마이크로서비스의 모듈화 장점을 결합한 아키텍처 패턴이다. Spring Modulith는 이를 Spring 생태계에서 쉽게 구현할 수 있도록 지원하는 프레임워크다.

## 목차

- [아키텍처 비교](#아키텍처-비교)
- [모듈러 모놀리스란?](#모듈러-모놀리스란)
- [Spring Modulith 소개](#spring-modulith-소개)
- [핵심 원리](#핵심-원리)
- [실전 예제](#실전-예제)
- [베스트 프랙티스](#베스트-프랙티스)
- [참고 자료](#참고-자료)

---

## 아키텍처 비교

| 특성 | 모놀리스 | 모듈러 모놀리스 | 마이크로서비스 |
|------|----------|-----------------|----------------|
| **배포 단위** | 단일 | 단일 | 서비스별 독립 |
| **모듈 경계** | 없거나 약함 | 명확함 | 물리적 분리 |
| **통신 방식** | 메서드 호출 | 이벤트/명시적 API | 네트워크(HTTP, gRPC) |
| **데이터베이스** | 공유 | 논리적 분리 가능 | 서비스별 분리 |
| **복잡도** | 낮음 | 중간 | 높음 |
| **운영 비용** | 낮음 | 낮음 | 높음 |
| **확장성** | 전체 확장 | 전체 확장 | 서비스별 확장 |
| **팀 독립성** | 낮음 | 중간 | 높음 |
| **트랜잭션** | ACID 용이 | ACID 용이 | 분산 트랜잭션 필요 |
| **장애 격리** | 어려움 | 부분적 | 용이 |
| **마이크로서비스 전환** | 어려움 | 용이 | - |

### 언제 모듈러 모놀리스를 선택하는가?

- 마이크로서비스의 운영 복잡도를 감당하기 어려울 때
- 팀 규모가 작거나 중간 규모일 때
- 도메인 경계가 아직 명확하지 않을 때
- 향후 마이크로서비스 전환 가능성을 열어두고 싶을 때

---

## 모듈러 모놀리스란?

모듈러 모놀리스는 **단일 배포 단위**를 유지하면서 내부적으로 **명확한 모듈 경계**를 가지는 아키텍처다.

### 핵심 특징

1. **논리적 모듈 분리**: 각 모듈은 자체 도메인을 담당
2. **명시적 의존성**: 모듈 간 의존성이 명확하게 정의됨
3. **캡슐화**: 각 모듈은 공개 API만 외부에 노출
4. **이벤트 기반 통신**: 모듈 간 결합도를 낮추기 위해 이벤트 사용

```
┌─────────────────────────────────────────────────────────┐
│                    Application                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐      │
│  │   Order     │  │  Inventory  │  │   Payment   │      │
│  │   Module    │──│   Module    │──│   Module    │      │
│  │             │  │             │  │             │      │
│  │ - OrderAPI  │  │ - StockAPI  │  │ - PayAPI    │      │
│  │ - internal  │  │ - internal  │  │ - internal  │      │
│  └─────────────┘  └─────────────┘  └─────────────┘      │
│         │                │                │              │
│         └────────────────┴────────────────┘              │
│                    Event Bus                             │
└─────────────────────────────────────────────────────────┘
```

---

## Spring Modulith 소개

Spring Modulith는 Spring 팀에서 제공하는 모듈러 모놀리스 구현 프레임워크다.

### 의존성 추가

```xml
<!-- Maven -->
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-core</artifactId>
    <version>1.2.0</version>
</dependency>

<!-- 테스트 지원 -->
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-test</artifactId>
    <version>1.2.0</version>
    <scope>test</scope>
</dependency>

<!-- 이벤트 외부화 (Kafka 예시) -->
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-events-kafka</artifactId>
    <version>1.2.0</version>
</dependency>
```

```groovy
// Gradle
implementation 'org.springframework.modulith:spring-modulith-starter-core:1.2.0'
testImplementation 'org.springframework.modulith:spring-modulith-starter-test:1.2.0'
```

### 주요 기능

| 기능 | 설명 |
|------|------|
| **모듈 구조 검증** | 모듈 간 의존성 규칙 위반 감지 |
| **모듈 문서화** | 자동 문서 생성 (PlantUML, Asciidoc) |
| **이벤트 기반 통신** | ApplicationEvent를 활용한 비동기 통신 |
| **이벤트 발행 보장** | 트랜잭션 아웃박스 패턴 지원 |
| **통합 테스트 지원** | 모듈 단위 슬라이스 테스트 |

---

## 핵심 원리

### 1. 패키지 구조 기반 모듈 인식

Spring Modulith는 메인 애플리케이션 클래스의 하위 패키지를 자동으로 모듈로 인식한다.

```
com.example.shop/
├── ShopApplication.java          # 메인 클래스
├── order/                         # Order 모듈
│   ├── Order.java                # 공개 API (패키지 루트)
│   ├── OrderService.java         # 공개 API
│   └── internal/                 # 내부 구현 (외부 접근 불가)
│       ├── OrderRepository.java
│       └── OrderValidator.java
├── inventory/                     # Inventory 모듈
│   ├── InventoryService.java
│   └── internal/
│       └── StockRepository.java
└── payment/                       # Payment 모듈
    ├── PaymentService.java
    └── internal/
        └── PaymentGateway.java
```

### 2. 공개 API vs 내부 구현

```java
// order/OrderService.java - 공개 API (다른 모듈에서 접근 가능)
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher events;

    public Order createOrder(CreateOrderRequest request) {
        Order order = Order.create(request);
        orderRepository.save(order);

        // 이벤트 발행으로 다른 모듈과 통신
        events.publishEvent(new OrderCreatedEvent(order.getId()));

        return order;
    }
}
```

```java
// order/internal/OrderRepository.java - 내부 구현 (외부 접근 불가)
@Repository
interface OrderRepository extends JpaRepository<Order, Long> {
    // 이 인터페이스는 order 모듈 외부에서 접근할 수 없음
}
```

### 3. 모듈 명시적 정의 (선택적)

복잡한 모듈 구조가 필요한 경우 `@ApplicationModule`을 사용한다.

```java
@ApplicationModule(
    allowedDependencies = {"inventory", "payment"},  // 허용된 의존성
    displayName = "주문 관리"
)
package com.example.shop.order;

import org.springframework.modulith.ApplicationModule;
```

### 4. Named Interface를 통한 세밀한 API 제어

```java
@ApplicationModule(
    allowedDependencies = {
        "inventory :: stock-api",  // inventory 모듈의 stock-api만 접근
        "payment"
    }
)
package com.example.shop.order;
```

```java
// inventory 모듈 내 Named Interface 정의
@NamedInterface("stock-api")
package com.example.shop.inventory.api;

import org.springframework.modulith.NamedInterface;
```

---

## 실전 예제

### 이벤트 기반 모듈 간 통신

```java
// 1. 이벤트 정의 (order 모듈)
package com.example.shop.order;

public record OrderCreatedEvent(
    Long orderId,
    Long productId,
    int quantity
) {}
```

```java
// 2. 이벤트 발행 (order 모듈)
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher events;

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Order order = Order.create(request);
        order = orderRepository.save(order);

        // 트랜잭션 커밋 후 이벤트 발행
        events.publishEvent(new OrderCreatedEvent(
            order.getId(),
            request.productId(),
            request.quantity()
        ));

        return order;
    }
}
```

```java
// 3. 이벤트 수신 (inventory 모듈)
package com.example.shop.inventory.internal;

@Service
@RequiredArgsConstructor
class InventoryEventHandler {

    private final StockRepository stockRepository;

    @ApplicationModuleListener  // Spring Modulith 제공
    void onOrderCreated(OrderCreatedEvent event) {
        stockRepository.decreaseStock(event.productId(), event.quantity());
    }
}
```

### 이벤트 발행 보장 (Transactional Outbox)

```java
// 설정
@Configuration
@EnableTransactionManagement
class ModulithConfig {

    @Bean
    ApplicationModuleInitializer modulesInitializer(ApplicationModules modules) {
        return modules::verify;
    }
}
```

```java
// 이벤트 발행 with 아웃박스 패턴
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher events;

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Order order = Order.create(request);
        order = orderRepository.save(order);

        // 이벤트가 DB에 저장되고, 트랜잭션 커밋 후 발행됨
        events.publishEvent(new OrderCreatedEvent(order.getId()));

        return order;
    }
}
```

```properties
# application.properties
spring.modulith.events.jdbc.schema-initialization.enabled=true
spring.modulith.republish-outstanding-events-on-restart=true
```

### 모듈 구조 검증 테스트

```java
@Test
void verifyModularStructure() {
    ApplicationModules modules = ApplicationModules.of(ShopApplication.class);

    // 모듈 구조 출력
    modules.forEach(System.out::println);

    // 의존성 규칙 검증 (위반 시 테스트 실패)
    modules.verify();
}
```

### 모듈 문서 생성

```java
@Test
void createModuleDocumentation() {
    ApplicationModules modules = ApplicationModules.of(ShopApplication.class);

    // PlantUML 다이어그램 생성
    new Documenter(modules)
        .writeDocumentation()
        .writeIndividualModulesAsPlantUml();
}
```

### 모듈 통합 테스트

```java
@ApplicationModuleTest  // order 모듈만 로드
class OrderModuleIntegrationTest {

    @Autowired
    OrderService orderService;

    @Autowired
    AssertablePublishedEvents events;  // 발행된 이벤트 검증

    @Test
    void createOrderPublishesEvent() {
        // given
        var request = new CreateOrderRequest(1L, 10);

        // when
        orderService.createOrder(request);

        // then
        assertThat(events)
            .contains(OrderCreatedEvent.class)
            .matching(e -> e.productId().equals(1L));
    }
}
```

### 시나리오 테스트 (Scenario API)

```java
@ApplicationModuleTest
class OrderScenarioTest {

    @Autowired
    OrderService orderService;

    @Test
    void orderCreationTriggersInventoryUpdate(Scenario scenario) {
        scenario.stimulate(() -> orderService.createOrder(new CreateOrderRequest(1L, 5)))
            .andWaitForEventOfType(OrderCreatedEvent.class)
            .toArriveAndVerify((event, dto) -> {
                assertThat(event.quantity()).isEqualTo(5);
            });
    }
}
```

---

## 베스트 프랙티스

### 1. 모듈 경계 설계

```
✅ 좋은 예: 도메인 중심 모듈 분리
com.example.shop/
├── order/          # 주문 도메인
├── catalog/        # 상품 카탈로그 도메인
├── customer/       # 고객 도메인
└── shipping/       # 배송 도메인

❌ 나쁜 예: 기술 계층 기반 분리
com.example.shop/
├── controller/     # 모든 컨트롤러
├── service/        # 모든 서비스
├── repository/     # 모든 레포지토리
└── entity/         # 모든 엔티티
```

### 2. 순환 의존성 방지

```java
// ❌ 나쁜 예: 직접 의존으로 인한 순환 참조 위험
@Service
class OrderService {
    private final InventoryService inventoryService;  // Order -> Inventory
}

@Service
class InventoryService {
    private final OrderService orderService;  // Inventory -> Order (순환!)
}
```

```java
// ✅ 좋은 예: 이벤트로 의존성 역전
@Service
class OrderService {
    private final ApplicationEventPublisher events;

    @Transactional
    public void createOrder(CreateOrderRequest request) {
        // 이벤트 발행 (Inventory를 직접 참조하지 않음)
        events.publishEvent(new OrderCreatedEvent(...));
    }
}

@Service
class InventoryEventHandler {
    @ApplicationModuleListener
    void on(OrderCreatedEvent event) {
        // 재고 차감
    }
}
```

### 3. 공유 커널 패턴

여러 모듈에서 공통으로 사용하는 타입은 별도 모듈로 분리한다.

```
com.example.shop/
├── shared/              # 공유 커널
│   ├── Money.java
│   ├── Address.java
│   └── DomainEvent.java
├── order/
├── payment/
└── shipping/
```

### 4. API 우선 설계

```java
// order/OrderApi.java - 모듈의 공개 계약
public interface OrderApi {
    Order createOrder(CreateOrderRequest request);
    Optional<Order> findById(Long id);
    void cancelOrder(Long orderId);
}

// order/internal/DefaultOrderApi.java - 구현
@Service
class DefaultOrderApi implements OrderApi {
    // 구현...
}
```

### 5. 점진적 마이크로서비스 전환

```java
// 1단계: 모듈러 모놀리스에서 이벤트 기반 통신 확립
@ApplicationModuleListener
void onOrderCreated(OrderCreatedEvent event) {
    // 로컬 처리
}

// 2단계: 이벤트를 외부 메시지 브로커로 발행
// spring-modulith-events-kafka 의존성 추가 시 자동 적용

// 3단계: 모듈을 별도 서비스로 분리
// - 해당 모듈 코드를 새 프로젝트로 이동
// - Kafka 컨슈머로 이벤트 수신
// - 기존 모놀리스에서 해당 모듈 제거
```

### 6. 테스트 전략

```java
// 단위 테스트: 도메인 로직
@Test
void orderTotalCalculation() {
    Order order = Order.create(...);
    assertThat(order.getTotal()).isEqualTo(Money.of(100));
}

// 모듈 통합 테스트: 모듈 내 협력
@ApplicationModuleTest
class OrderModuleTest { ... }

// 시스템 통합 테스트: 전체 모듈 협력
@SpringBootTest
class SystemIntegrationTest { ... }
```

### 7. 모니터링과 관찰 가능성

```java
// Actuator 통합
@Bean
ApplicationModuleInitializer moduleHealthIndicator(ApplicationModules modules) {
    return () -> {
        modules.forEach(module ->
            log.info("Module loaded: {}", module.getName()));
    };
}
```

---

## 참고 자료

- [Spring Modulith 공식 문서](https://docs.spring.io/spring-modulith/reference/)
- [Spring Modulith GitHub](https://github.com/spring-projects/spring-modulith)
- [Modular Monolith with DDD - Kamil Grzybek](https://github.com/kgrzybek/modular-monolith-with-ddd)
- [Spring I/O 2023 - Spring Modulith](https://www.youtube.com/watch?v=VGhg6Tfxb60)

---

*마지막 업데이트: 2025년 12월*
