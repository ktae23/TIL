# 테스트 피라미드

## 목차
1. [테스트 피라미드 개념](#테스트-피라미드-개념)
2. [단위 테스트 (Unit Test)](#단위-테스트-unit-test)
3. [통합 테스트 (Integration Test)](#통합-테스트-integration-test)
4. [E2E 테스트 (End-to-End Test)](#e2e-테스트-end-to-end-test)
5. [비용 vs 효과 분석](#비용-vs-효과-분석)
6. [핵심 정리](#핵심-정리)

---

## 테스트 피라미드 개념

### 테스트 피라미드 구조

```
┌──────────────────────────────────────────────────────────────────┐
│                    테스트 피라미드                                │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│                         ▲                                        │
│                        ╱ ╲                                       │
│                       ╱   ╲                                      │
│                      ╱ E2E ╲        10% - 느림, 비쌈, 불안정     │
│                     ╱───────╲                                    │
│                    ╱         ╲                                   │
│                   ╱ Integration╲   20% - 중간 속도, 적당한 비용   │
│                  ╱─────────────╲                                 │
│                 ╱               ╲                                │
│                ╱   Unit Tests    ╲ 70% - 빠름, 저렴, 안정적      │
│               ╱───────────────────╲                              │
│              ╱                     ╲                             │
│             ▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔                            │
│                                                                   │
│  권장 비율: Unit 70% : Integration 20% : E2E 10%                 │
│                                                                   │
│  ⚠️ 아이스크림 콘 안티패턴 (피해야 함):                           │
│                        ╱───────────╲                             │
│                       ╱    E2E      ╲  ← 많은 E2E (느림, 불안정)  │
│                      ╱───────────────╲                           │
│                     ╱   Integration   ╲                          │
│                    ╱─────────────────╲                           │
│                   ▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔                           │
│                       Unit (적음)                                 │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 각 레벨의 특징

| 테스트 유형 | 범위 | 속도 | 비용 | 신뢰도 | 격리 |
|------------|------|------|------|--------|------|
| Unit | 클래스/메서드 | 밀리초 | 낮음 | 높음 | 완전 격리 |
| Integration | 컴포넌트 간 | 초 | 중간 | 중간 | 부분 격리 |
| E2E | 전체 시스템 | 분 | 높음 | 낮음 | 격리 없음 |

---

## 단위 테스트 (Unit Test)

### 특징

```
┌──────────────────────────────────────────────────────────────────┐
│                    단위 테스트 특징                               │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  테스트 대상:                                                    │
│  - 단일 클래스 또는 메서드                                       │
│  - 비즈니스 로직                                                 │
│  - 순수 함수                                                     │
│                                                                   │
│  격리 방법:                                                      │
│  - Mock 객체로 의존성 대체                                       │
│  - 외부 시스템 연결 없음                                         │
│                                                                   │
│  좋은 단위 테스트 조건 (FIRST):                                  │
│  - Fast: 밀리초 단위로 빠름                                      │
│  - Isolated: 다른 테스트와 독립                                  │
│  - Repeatable: 언제 실행해도 같은 결과                           │
│  - Self-validating: 성공/실패 자동 판단                          │
│  - Timely: 프로덕션 코드 작성 전/후 바로 작성                     │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 예제

```java
// 테스트 대상
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;

    public Order createOrder(OrderRequest request) {
        // 비즈니스 로직
        if (request.getItems().isEmpty()) {
            throw new InvalidOrderException("주문 항목이 비어있습니다");
        }

        BigDecimal total = calculateTotal(request.getItems());
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderException("주문 금액이 0원 이하입니다");
        }

        Order order = Order.create(request.getCustomerId(), request.getItems(), total);
        return orderRepository.save(order);
    }

    private BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

// 단위 테스트
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryClient inventoryClient;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("유효한 주문 요청 시 주문이 생성된다")
    void createOrder_ValidRequest_ReturnsOrder() {
        // Given
        OrderRequest request = OrderRequest.builder()
            .customerId(1L)
            .items(List.of(
                new OrderItem(1L, "상품A", BigDecimal.valueOf(10000), 2),
                new OrderItem(2L, "상품B", BigDecimal.valueOf(20000), 1)
            ))
            .build();

        Order expectedOrder = Order.create(1L, request.getItems(), BigDecimal.valueOf(40000));
        when(orderRepository.save(any(Order.class))).thenReturn(expectedOrder);

        // When
        Order result = orderService.createOrder(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(40000));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("빈 주문 항목으로 주문 시 예외가 발생한다")
    void createOrder_EmptyItems_ThrowsException() {
        // Given
        OrderRequest request = OrderRequest.builder()
            .customerId(1L)
            .items(Collections.emptyList())
            .build();

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request))
            .isInstanceOf(InvalidOrderException.class)
            .hasMessage("주문 항목이 비어있습니다");

        verify(orderRepository, never()).save(any());
    }
}
```

---

## 통합 테스트 (Integration Test)

### 특징

```
┌──────────────────────────────────────────────────────────────────┐
│                    통합 테스트 특징                               │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  테스트 대상:                                                    │
│  - 여러 컴포넌트 간의 상호작용                                   │
│  - 데이터베이스 연동                                             │
│  - 외부 API 연동 (필요 시 WireMock 등으로 대체)                  │
│                                                                   │
│  Spring Boot 통합 테스트 범위:                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                                                              │ │
│  │  @SpringBootTest (전체 컨텍스트)                             │ │
│  │  ┌─────────────────────────────────────────────────────────┐│ │
│  │  │  Controller → Service → Repository → Database          ││ │
│  │  └─────────────────────────────────────────────────────────┘│ │
│  │                                                              │ │
│  │  @DataJpaTest (JPA 레이어만)                                 │ │
│  │  ┌─────────────────────────────────────────────────────────┐│ │
│  │  │  Repository → Database                                  ││ │
│  │  └─────────────────────────────────────────────────────────┘│ │
│  │                                                              │ │
│  │  @WebMvcTest (Web 레이어만)                                  │ │
│  │  ┌─────────────────────────────────────────────────────────┐│ │
│  │  │  Controller (Service는 Mock)                            ││ │
│  │  └─────────────────────────────────────────────────────────┘│ │
│  │                                                              │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 예제

```java
// Repository 통합 테스트
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class OrderRepositoryTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("주문 저장 후 ID로 조회할 수 있다")
    void save_ThenFindById() {
        // Given
        Order order = Order.create(1L, createItems(), BigDecimal.valueOf(30000));

        // When
        Order saved = orderRepository.save(order);
        Order found = orderRepository.findById(saved.getId()).orElseThrow();

        // Then
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(30000));
    }

    @Test
    @DisplayName("고객 ID로 주문 목록을 조회할 수 있다")
    void findByCustomerId() {
        // Given
        Order order1 = Order.create(1L, createItems(), BigDecimal.valueOf(10000));
        Order order2 = Order.create(1L, createItems(), BigDecimal.valueOf(20000));
        Order order3 = Order.create(2L, createItems(), BigDecimal.valueOf(30000));

        orderRepository.saveAll(List.of(order1, order2, order3));

        // When
        List<Order> customer1Orders = orderRepository.findByCustomerId(1L);

        // Then
        assertThat(customer1Orders).hasSize(2);
    }
}

// 전체 통합 테스트
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("주문 생성 API 통합 테스트")
    void createOrder_Integration() {
        // Given
        OrderRequest request = OrderRequest.builder()
            .customerId(1L)
            .items(List.of(new OrderItemDto(1L, 10000, 2)))
            .build();

        // When
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
            "/api/orders",
            request,
            OrderResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getOrderId()).isNotNull();

        // DB 검증
        Order saved = orderRepository.findById(response.getBody().getOrderId()).orElseThrow();
        assertThat(saved.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(20000));
    }
}
```

---

## E2E 테스트 (End-to-End Test)

### 특징

```
┌──────────────────────────────────────────────────────────────────┐
│                    E2E 테스트 특징                                │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  테스트 대상:                                                    │
│  - 전체 시스템 (프론트엔드 → 백엔드 → DB)                        │
│  - 사용자 시나리오                                               │
│  - 크리티컬 비즈니스 플로우                                      │
│                                                                   │
│  특징:                                                           │
│  - 실제 환경과 가장 유사                                         │
│  - 느리고 비용이 높음                                            │
│  - 불안정할 수 있음 (Flaky Test)                                 │
│  - 최소한으로 유지                                               │
│                                                                   │
│  E2E 테스트 범위:                                                │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  Browser                                                     │ │
│  │     ↓                                                        │ │
│  │  Frontend (React, Vue)                                      │ │
│  │     ↓                                                        │ │
│  │  API Gateway                                                 │ │
│  │     ↓                                                        │ │
│  │  Backend Services                                            │ │
│  │     ↓                                                        │ │
│  │  Database, Redis, Kafka                                      │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### E2E 테스트 (REST Assured)

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderE2ETest {

    @LocalServerPort
    private int port;

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7")
        .withExposedPorts(6379);

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("주문 생성부터 결제까지 전체 플로우")
    void orderFlow_E2E() {
        // 1. 회원 가입
        String customerId = given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "email", "test@example.com",
                "password", "password123",
                "name", "테스트 사용자"
            ))
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(201)
            .extract()
            .path("customerId");

        // 2. 로그인
        String token = given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "email", "test@example.com",
                "password", "password123"
            ))
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .extract()
            .path("accessToken");

        // 3. 상품 목록 조회
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .body("$.size()", greaterThan(0));

        // 4. 주문 생성
        String orderId = given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "items", List.of(
                    Map.of("productId", 1, "quantity", 2)
                )
            ))
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201)
            .body("status", equalTo("CREATED"))
            .extract()
            .path("orderId");

        // 5. 결제
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "orderId", orderId,
                "paymentMethod", "CARD"
            ))
        .when()
            .post("/api/payments")
        .then()
            .statusCode(200)
            .body("status", equalTo("COMPLETED"));

        // 6. 주문 상태 확인
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/orders/" + orderId)
        .then()
            .statusCode(200)
            .body("status", equalTo("PAID"));
    }
}
```

---

## 비용 vs 효과 분석

### 테스트 유형별 비용-효과

```
┌──────────────────────────────────────────────────────────────────┐
│                    비용 vs 효과 분석                              │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ROI (투자 대비 효과)                                            │
│  ▲                                                               │
│  │                                                               │
│  │  ★ Unit Test                                                 │
│  │     (높은 ROI: 빠르고 저렴, 많이 작성 가능)                   │
│  │                                                               │
│  │              ★ Integration Test                               │
│  │                 (중간 ROI)                                    │
│  │                                                               │
│  │                          ★ E2E Test                           │
│  │                             (낮은 ROI: 느리고 비쌈)           │
│  │                                                               │
│  └────────────────────────────────────────────────────────────► │
│      저비용                                          고비용      │
│                                                                   │
│  비용 요소:                                                      │
│  - 작성 시간                                                     │
│  - 실행 시간                                                     │
│  - 유지보수 비용                                                 │
│  - 인프라 비용                                                   │
│  - 디버깅 시간                                                   │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 테스트 전략 결정

```java
/**
 * 테스트 전략 가이드라인
 *
 * 1. 단위 테스트로 테스트 가능한 것:
 *    - 비즈니스 로직
 *    - 유틸리티 함수
 *    - 도메인 객체
 *    - 알고리즘
 *
 * 2. 통합 테스트가 필요한 것:
 *    - Repository 쿼리
 *    - 외부 API 연동
 *    - 메시지 큐 처리
 *    - 캐시 로직
 *
 * 3. E2E 테스트가 필요한 것:
 *    - 핵심 비즈니스 플로우 (결제, 주문 등)
 *    - 중요한 사용자 시나리오
 *    - 배포 전 Smoke Test
 */

// 예: 주문 도메인 테스트 전략
class OrderTestStrategy {

    // Unit: 주문 생성 로직, 금액 계산, 상태 변경
    // → OrderTest, OrderItemTest, OrderCalculatorTest

    // Integration: 주문 저장, 재고 차감
    // → OrderRepositoryTest, InventoryClientTest

    // E2E: 주문 → 결제 → 배송 플로우
    // → OrderFlowE2ETest (1~2개)
}
```

---

## 핵심 정리

### 테스트 피라미드 비율

| 테스트 유형 | 권장 비율 | 실행 시간 | 커버리지 목표 |
|------------|----------|----------|--------------|
| Unit | 70% | ~수 밀리초 | 80%+ |
| Integration | 20% | ~수 초 | 핵심 경로 |
| E2E | 10% | ~수 분 | 크리티컬 플로우 |

### 테스트 작성 원칙

```
1. 단위 테스트 우선
   - 빠른 피드백
   - 리팩토링 안전망
   - 문서 역할

2. 통합 테스트는 경계에서
   - DB, 외부 API 연동 검증
   - 슬라이스 테스트 활용

3. E2E는 최소한으로
   - 핵심 시나리오만
   - 안정적으로 유지

4. 피해야 할 패턴
   - 아이스크림 콘 (E2E 과다)
   - 모래시계 (Integration 과다)
   - Mock 지옥 (과도한 Mocking)
```

### 면접 대비 핵심 질문

1. **Q: 테스트 피라미드의 각 레벨별 특징은?**
   - A: Unit(70%)은 빠르고 격리됨, Integration(20%)은 컴포넌트 간 상호작용, E2E(10%)는 전체 시스템. 위로 갈수록 느리고 비용 높음

2. **Q: 단위 테스트의 좋은 특성은?**
   - A: FIRST - Fast, Isolated, Repeatable, Self-validating, Timely. 외부 의존성 Mock으로 격리

3. **Q: 통합 테스트와 단위 테스트의 경계는?**
   - A: 외부 시스템(DB, API) 연동이 필요하면 통합 테스트. 순수 비즈니스 로직은 단위 테스트. Testcontainers로 실제 환경과 유사하게

4. **Q: E2E 테스트를 최소화하는 이유는?**
   - A: 느림, 불안정(Flaky), 유지보수 비용 높음. 단위/통합 테스트로 대부분 커버하고 E2E는 크리티컬 플로우만

---

*마지막 업데이트: 2025년 01월*
