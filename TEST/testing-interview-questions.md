# 테스트 면접 핵심 질문 정리

5년차 백엔드 개발자 면접에서 자주 등장하는 테스트 관련 핵심 질문과 답변을 정리합니다.

## 목차

1. [FIRST 원칙](#1-first-원칙)
2. [TDD vs BDD](#2-tdd-vs-bdd)
3. [테스트 커버리지의 함정](#3-테스트-커버리지의-함정)
4. [테스트 더블 (Test Double)](#4-테스트-더블-test-double)
5. [단위 테스트 vs 통합 테스트](#5-단위-테스트-vs-통합-테스트)
6. [테스트 피라미드](#6-테스트-피라미드)

---

## 1. FIRST 원칙

### Q: 좋은 단위 테스트의 FIRST 원칙을 설명해주세요.

**F - Fast (빠르게)**
```java
// Bad: 실제 DB 연결로 느림
@Test
void createOrder_withRealDB() {
    Order order = orderService.create(request);  // DB I/O 발생
    assertThat(order).isNotNull();
}

// Good: Mock 사용으로 빠름
@Test
void createOrder_withMock() {
    when(orderRepository.save(any())).thenReturn(expectedOrder);
    Order order = orderService.create(request);
    assertThat(order).isNotNull();
}
```

**I - Independent/Isolated (독립적)**
```java
// Bad: 테스트 간 상태 공유
static int testCounter = 0;  // 공유 상태

@Test
void test1() {
    testCounter++;
    assertEquals(1, testCounter);  // 실행 순서에 따라 실패 가능
}

// Good: 각 테스트가 독립적
@BeforeEach
void setUp() {
    // 매 테스트마다 새로운 상태로 초기화
    orderService = new OrderService(new FakeOrderRepository());
}
```

**R - Repeatable (반복 가능)**
```java
// Bad: 시간 의존적
@Test
void checkExpiry() {
    Order order = new Order(LocalDateTime.now().plusDays(1));
    assertFalse(order.isExpired());  // 자정 근처에 실패 가능
}

// Good: 시간을 주입 가능하게
@Test
void checkExpiry() {
    Clock fixedClock = Clock.fixed(Instant.parse("2024-01-15T10:00:00Z"), ZoneId.UTC);
    Order order = new Order(LocalDateTime.now(fixedClock).plusDays(1));
    assertFalse(order.isExpired(fixedClock));
}
```

**S - Self-Validating (자체 검증)**
```java
// Bad: 수동 확인 필요
@Test
void createOrder() {
    Order order = orderService.create(request);
    System.out.println(order);  // 출력 보고 수동 확인?
}

// Good: 자동 검증
@Test
void createOrder() {
    Order order = orderService.create(request);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(order.getTotalAmount()).isEqualTo(10000);
}
```

**T - Timely (적시에)**
```java
// TDD 방식: 테스트 먼저 작성
// 1. 실패하는 테스트 작성
@Test
void should_calculate_discount_for_vip_customer() {
    Customer vip = Customer.vip();
    int discount = discountPolicy.calculate(vip, 10000);
    assertThat(discount).isEqualTo(1000);  // 10% 할인
}

// 2. 테스트 통과하는 최소 코드 작성
// 3. 리팩토링
```

---

## 2. TDD vs BDD

### Q: TDD와 BDD의 차이점은 무엇인가요?

**TDD (Test-Driven Development)**
```java
// 개발자 관점, 기술적 테스트
// Red → Green → Refactor

// Step 1: Red - 실패하는 테스트
@Test
void add_item_to_cart_increases_count() {
    Cart cart = new Cart();
    cart.add(new Item("book", 10000));
    assertEquals(1, cart.getItemCount());
}

// Step 2: Green - 통과하는 최소 구현
public class Cart {
    private List<Item> items = new ArrayList<>();

    public void add(Item item) {
        items.add(item);
    }

    public int getItemCount() {
        return items.size();
    }
}

// Step 3: Refactor - 코드 개선 (테스트는 유지)
```

**BDD (Behavior-Driven Development)**
```java
// 비즈니스 관점, 행동 명세
// Given-When-Then 형식

@DisplayName("장바구니 기능")
class CartBehaviorTest {

    @Test
    @DisplayName("고객이 상품을 장바구니에 담으면 상품 개수가 증가한다")
    void customer_adds_item_to_cart() {
        // Given: 빈 장바구니가 있을 때
        Cart cart = new Cart();
        Item book = new Item("클린코드", 20000);

        // When: 고객이 상품을 추가하면
        cart.add(book);

        // Then: 장바구니 상품 개수가 1개가 된다
        assertThat(cart.getItemCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("VIP 고객이 10만원 이상 구매시 10% 할인 받는다")
    void vip_customer_gets_discount_over_100000() {
        // Given
        Customer vip = Customer.builder().grade(Grade.VIP).build();
        Cart cart = new Cart();
        cart.add(new Item("노트북", 150000));

        // When
        int discount = discountPolicy.calculate(vip, cart);

        // Then
        assertThat(discount).isEqualTo(15000);
    }
}
```

**Cucumber (BDD 프레임워크) 예시**
```gherkin
# cart.feature
Feature: 장바구니 관리

  Scenario: 상품 추가
    Given 빈 장바구니가 있다
    When 고객이 "클린코드" 상품을 추가한다
    Then 장바구니에 1개의 상품이 있어야 한다

  Scenario: VIP 할인
    Given VIP 등급의 고객이 있다
    And 150,000원 상당의 상품이 장바구니에 있다
    When 결제를 진행한다
    Then 15,000원의 할인이 적용된다
```

| 구분 | TDD | BDD |
|------|-----|-----|
| 관점 | 개발자 | 비즈니스/사용자 |
| 언어 | 기술 용어 | 비즈니스 용어 |
| 형식 | assert 위주 | Given-When-Then |
| 목적 | 코드 설계 | 요구사항 명세화 |
| 참여자 | 개발자 | 개발자, 기획자, QA |

---

## 3. 테스트 커버리지의 함정

### Q: 높은 테스트 커버리지가 좋은 테스트를 보장하나요?

**커버리지의 한계**
```java
// 100% 라인 커버리지지만 의미 없는 테스트
public class Calculator {
    public int divide(int a, int b) {
        return a / b;  // b=0일 때 예외 발생
    }
}

@Test
void divide() {
    Calculator calc = new Calculator();
    int result = calc.divide(10, 2);
    // assertion 없음! 실행만 하면 커버리지 100%
}

// 또는
@Test
void divide() {
    Calculator calc = new Calculator();
    calc.divide(10, 2);
    assertTrue(true);  // 무의미한 assertion
}
```

**함정 1: 라인 커버리지 vs 분기 커버리지**
```java
public String getGrade(int score) {
    if (score >= 90) return "A";
    if (score >= 80) return "B";
    if (score >= 70) return "C";
    return "F";
}

// 라인 커버리지 100%가 되려면 4개 테스트 케이스 필요
// 하지만 경계값 테스트가 빠질 수 있음
@Test void gradeA() { assertEquals("A", getGrade(95)); }
@Test void gradeB() { assertEquals("B", getGrade(85)); }
@Test void gradeC() { assertEquals("C", getGrade(75)); }
@Test void gradeF() { assertEquals("F", getGrade(50)); }

// 경계값 테스트 추가 필요
@Test void gradeA_boundary() { assertEquals("A", getGrade(90)); }
@Test void gradeB_boundary() { assertEquals("B", getGrade(80)); }
@Test void gradeB_justBelow() { assertEquals("B", getGrade(89)); }
```

**함정 2: 커버리지가 높아도 놓치는 것들**
```java
// 동시성 문제
public class Counter {
    private int count = 0;

    public void increment() {
        count++;  // 라인 커버리지 100%, but 동시성 버그
    }
}

// 예외 케이스
public Order createOrder(Request request) {
    // null 체크, 유효성 검사 등이 없으면
    // 커버리지 높아도 프로덕션에서 NPE 발생
    return new Order(request.getItems());
}
```

**의미 있는 테스트 지표**
```java
// 1. Mutation Testing (변이 테스트)
// 코드를 변경했을 때 테스트가 실패하는지 확인
// PIT (Java), Stryker (JS) 등 사용

// 원본 코드
if (score >= 90) return "A";

// 변이된 코드 (테스트가 이걸 잡아야 함)
if (score > 90) return "A";   // >= 를 > 로 변경
if (score <= 90) return "A";  // >= 를 <= 로 변경
```

**권장 커버리지 전략**
| 레이어 | 권장 커버리지 | 설명 |
|--------|--------------|------|
| Domain/Core | 80-90% | 비즈니스 로직은 높게 |
| Service | 70-80% | 통합 로직 |
| Controller | 50-70% | 슬라이스 테스트 활용 |
| Repository | 낮음 | 프레임워크 신뢰 |

---

## 4. 테스트 더블 (Test Double)

### Q: Mock, Stub, Spy의 차이점을 설명해주세요.

**Dummy**
```java
// 전달되기만 하고 실제로 사용되지 않음
@Test
void createUser() {
    // EmailService는 사용되지 않지만 생성자에 필요
    EmailService dummyEmail = new DummyEmailService();
    UserService userService = new UserService(dummyEmail);

    User user = userService.create("kim", "kim@test.com");
    assertNotNull(user);
}
```

**Stub**
```java
// 미리 정의된 답변을 반환
public class StubStockRepository implements StockRepository {
    @Override
    public int getStock(Long productId) {
        return 100;  // 항상 100 반환
    }
}

@Test
void checkStock() {
    StockService service = new StockService(new StubStockRepository());
    assertTrue(service.isAvailable(1L, 50));  // 100 >= 50
}

// Mockito로 Stub
@Test
void checkStock_withMockito() {
    StockRepository stubRepo = mock(StockRepository.class);
    when(stubRepo.getStock(anyLong())).thenReturn(100);

    StockService service = new StockService(stubRepo);
    assertTrue(service.isAvailable(1L, 50));
}
```

**Mock**
```java
// 행동 검증 (verify) 목적
@Test
void createOrder_sendsEmail() {
    // Mock 생성
    EmailService mockEmail = mock(EmailService.class);
    OrderService orderService = new OrderService(mockEmail);

    // 실행
    orderService.createOrder(request);

    // 행동 검증 (이메일이 발송되었는지)
    verify(mockEmail, times(1)).send(any(Email.class));
    verify(mockEmail).send(argThat(email ->
        email.getSubject().contains("주문 확인")
    ));
}
```

**Spy**
```java
// 실제 객체를 감싸서 일부만 Stub
@Test
void processOrder_withSpy() {
    OrderService realService = new OrderService(repository);
    OrderService spyService = spy(realService);

    // 특정 메서드만 Stub
    doReturn(mockOrder).when(spyService).findById(anyLong());

    // 나머지는 실제 동작
    spyService.process(1L);  // findById만 Mock, 나머지는 실제 실행

    // 실제 메서드 호출 여부 검증
    verify(spyService).sendNotification(any());
}
```

**Fake**
```java
// 실제 구현의 단순화된 버전
public class FakeUserRepository implements UserRepository {
    private final Map<Long, User> database = new HashMap<>();
    private Long idSequence = 1L;

    @Override
    public User save(User user) {
        user.setId(idSequence++);
        database.put(user.getId(), user);
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(database.get(id));
    }
}
```

| 종류 | 목적 | 상태 검증 | 행동 검증 |
|------|------|----------|----------|
| Dummy | 파라미터 채우기 | X | X |
| Stub | 미리 정의된 값 반환 | O | X |
| Mock | 행동 검증 | O | O |
| Spy | 실제 객체 + 일부 Stub | O | O |
| Fake | 단순화된 실제 구현 | O | X |

---

## 5. 단위 테스트 vs 통합 테스트

### Q: 단위 테스트와 통합 테스트를 어떻게 구분하고 작성하나요?

**단위 테스트 (Unit Test)**
```java
// 특징: 빠름, 격리됨, 외부 의존성 없음
@ExtendWith(MockitoExtension.class)
class OrderServiceUnitTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StockService stockService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_success() {
        // Given
        OrderRequest request = OrderRequest.builder()
            .productId(1L)
            .quantity(2)
            .build();

        when(stockService.isAvailable(1L, 2)).thenReturn(true);
        when(orderRepository.save(any())).thenReturn(
            Order.builder().id(1L).status(PENDING).build()
        );

        // When
        Order order = orderService.create(request);

        // Then
        assertThat(order.getStatus()).isEqualTo(PENDING);
        verify(orderRepository).save(any());
    }
}
```

**통합 테스트 (Integration Test)**
```java
// 특징: 실제 인프라 사용, 느림, 여러 컴포넌트 통합
@SpringBootTest
@Transactional
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void createOrder_persistsToDatabase() {
        // Given
        OrderRequest request = OrderRequest.builder()
            .productId(1L)
            .quantity(2)
            .build();

        // When
        Order order = orderService.create(request);

        // Then
        Order found = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(PENDING);
    }
}
```

**슬라이스 테스트 (중간 형태)**
```java
// Repository 레이어만 테스트
@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void findByStatus() {
        // Given
        orderRepository.save(Order.builder().status(PENDING).build());
        orderRepository.save(Order.builder().status(COMPLETED).build());

        // When
        List<Order> pending = orderRepository.findByStatus(PENDING);

        // Then
        assertThat(pending).hasSize(1);
    }
}

// Controller 레이어만 테스트
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    void createOrder_returns201() throws Exception {
        when(orderService.create(any())).thenReturn(
            Order.builder().id(1L).build()
        );

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\": 1, \"quantity\": 2}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));
    }
}
```

| 구분 | 단위 테스트 | 통합 테스트 |
|------|------------|------------|
| 범위 | 단일 클래스/메서드 | 여러 컴포넌트 |
| 속도 | 빠름 (ms) | 느림 (초~분) |
| 의존성 | Mock 사용 | 실제 의존성 |
| 목적 | 로직 검증 | 통합 동작 검증 |
| 비중 | 70-80% | 20-30% |

---

## 6. 테스트 피라미드

### Q: 테스트 피라미드를 설명하고 각 레벨의 특징을 말해주세요.

**테스트 피라미드 구조**
```
                    /\
                   /  \
                  /    \
                 / E2E  \        ← 적게 (느림, 비쌈)
                /________\
               /          \
              / Integration \    ← 중간
             /______________\
            /                \
           /    Unit Tests    \  ← 많이 (빠름, 저렴)
          /____________________\
```

**각 레벨별 특징**

| 레벨 | 비중 | 속도 | 비용 | 신뢰도 | 유지보수 |
|------|------|------|------|--------|----------|
| E2E | 5-10% | 매우 느림 | 높음 | 높음 | 어려움 |
| Integration | 20-30% | 느림 | 중간 | 중간 | 중간 |
| Unit | 60-70% | 빠름 | 낮음 | 낮음 | 쉬움 |

**레벨별 예시**
```java
// Unit Test (많이)
@Test
void calculateDiscount_vipGets10Percent() {
    DiscountPolicy policy = new DiscountPolicy();
    int discount = policy.calculate(VIP, 10000);
    assertThat(discount).isEqualTo(1000);
}

// Integration Test (중간)
@SpringBootTest
@Test
void createOrder_savesToDatabaseAndSendsEvent() {
    Order order = orderService.create(request);

    // DB 저장 확인
    assertThat(orderRepository.findById(order.getId())).isPresent();

    // 이벤트 발행 확인
    verify(eventPublisher).publish(any(OrderCreatedEvent.class));
}

// E2E Test (적게)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class OrderE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void fullOrderFlow() {
        // 1. 로그인
        String token = login("user", "password");

        // 2. 주문 생성
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<Order> response = restTemplate.exchange(
            "/api/orders",
            HttpMethod.POST,
            new HttpEntity<>(orderRequest, headers),
            Order.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 3. 주문 조회
        Order order = restTemplate.getForObject(
            "/api/orders/" + response.getBody().getId(),
            Order.class
        );

        assertThat(order.getStatus()).isEqualTo(PENDING);
    }
}
```

**안티패턴: 아이스크림 콘 / 역피라미드**
```
    ______________
   /              \      ← E2E 많음 (느림, 깨지기 쉬움)
  /________________\
 /                  \
/    Integration     \   ← 중간
\____________________/
        |    |
        |    |           ← Unit 적음 (빠르지만...)
        |____|

문제점:
- 피드백 루프 느림
- 실패 원인 파악 어려움
- 유지보수 비용 높음
```

---

## 핵심 정리

| 주제 | 핵심 키워드 |
|------|-------------|
| FIRST 원칙 | Fast, Independent, Repeatable, Self-validating, Timely |
| TDD vs BDD | 기술 관점 vs 비즈니스 관점, Given-When-Then |
| 커버리지 함정 | 라인 vs 분기, Mutation Test, 의미 있는 assertion |
| 테스트 더블 | Mock(행동검증), Stub(상태), Spy(부분Mock), Fake(단순구현) |
| 단위 vs 통합 | 격리/빠름 vs 통합/느림, 슬라이스 테스트 활용 |
| 피라미드 | Unit(70%) > Integration(20%) > E2E(10%) |

---

*마지막 업데이트: 2025년 01월*
