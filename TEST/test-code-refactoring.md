# 테스트 코드 리팩토링

테스트 픽스처 패턴, 빌더 패턴, 느린 테스트 개선 방법을 정리합니다.

## 목차

1. [테스트 코드 스멜](#1-테스트-코드-스멜)
2. [테스트 픽스처](#2-테스트-픽스처)
3. [테스트 빌더 패턴](#3-테스트-빌더-패턴)
4. [느린 테스트 개선](#4-느린-테스트-개선)
5. [테스트 유지보수](#5-테스트-유지보수)

---

## 1. 테스트 코드 스멜

### 흔한 문제들

```java
// ❌ Bad: 중복된 셋업 코드
@Test
void test1() {
    User user = new User();
    user.setId(1L);
    user.setName("kim");
    user.setEmail("kim@example.com");
    user.setAge(25);
    user.setStatus(UserStatus.ACTIVE);
    // ... 테스트
}

@Test
void test2() {
    User user = new User();
    user.setId(1L);
    user.setName("kim");
    user.setEmail("kim@example.com");
    user.setAge(25);
    user.setStatus(UserStatus.ACTIVE);
    // ... 테스트
}
```

```java
// ❌ Bad: 테스트와 무관한 데이터
@Test
void shouldCalculateDiscount() {
    User user = new User();
    user.setId(1L);
    user.setName("kim");           // 할인 계산과 무관
    user.setEmail("kim@test.com"); // 할인 계산과 무관
    user.setPhone("010-1234");     // 할인 계산과 무관
    user.setAddress("Seoul");      // 할인 계산과 무관
    user.setMembershipLevel(VIP);  // 이것만 필요!

    int discount = calculator.calculate(user);
    assertThat(discount).isEqualTo(20);
}
```

```java
// ❌ Bad: 마법의 숫자/문자열
@Test
void test() {
    Order order = createOrder(1L, 3, 15000, "CARD");
    // 각 파라미터가 뭘 의미하는지?
}
```

```java
// ❌ Bad: 과도한 Mock
@Test
void test() {
    given(userRepository.findById(any())).willReturn(Optional.of(user));
    given(orderRepository.findByUserId(any())).willReturn(orders);
    given(productRepository.findById(any())).willReturn(Optional.of(product));
    given(couponRepository.findValidCoupons(any())).willReturn(coupons);
    given(shippingService.calculate(any())).willReturn(shipping);
    given(taxService.calculate(any())).willReturn(tax);
    // 10개 이상의 Mock... 테스트 대상이 너무 많은 의존성을 가짐
}
```

---

## 2. 테스트 픽스처

### Object Mother 패턴

```java
// 공통 테스트 데이터 팩토리
public class UserMother {

    public static User aUser() {
        return User.builder()
                .id(1L)
                .name("김테스트")
                .email("test@example.com")
                .status(UserStatus.ACTIVE)
                .build();
    }

    public static User aVipUser() {
        return User.builder()
                .id(2L)
                .name("김VIP")
                .email("vip@example.com")
                .membershipLevel(MembershipLevel.VIP)
                .status(UserStatus.ACTIVE)
                .build();
    }

    public static User anInactiveUser() {
        return User.builder()
                .id(3L)
                .name("탈퇴회원")
                .email("inactive@example.com")
                .status(UserStatus.INACTIVE)
                .build();
    }
}

// 사용
@Test
void vipUserGetsDiscount() {
    User user = UserMother.aVipUser();
    int discount = calculator.calculate(user);
    assertThat(discount).isEqualTo(20);
}
```

### 픽스처 클래스

```java
// 관련 픽스처를 그룹화
public class OrderFixture {

    public static Order pending() {
        return Order.builder()
                .id(1L)
                .status(OrderStatus.PENDING)
                .items(List.of(OrderItemFixture.defaultItem()))
                .build();
    }

    public static Order completed() {
        return pending().toBuilder()
                .status(OrderStatus.COMPLETED)
                .completedAt(LocalDateTime.now())
                .build();
    }

    public static Order cancelled() {
        return pending().toBuilder()
                .status(OrderStatus.CANCELLED)
                .cancelledAt(LocalDateTime.now())
                .build();
    }
}

public class OrderItemFixture {

    public static OrderItem defaultItem() {
        return OrderItem.builder()
                .productId(100L)
                .quantity(1)
                .price(10000)
                .build();
    }
}
```

---

## 3. 테스트 빌더 패턴

### Test Data Builder

```java
public class UserBuilder {
    private Long id = 1L;
    private String name = "김테스트";
    private String email = "test@example.com";
    private MembershipLevel level = MembershipLevel.NORMAL;
    private UserStatus status = UserStatus.ACTIVE;

    public static UserBuilder aUser() {
        return new UserBuilder();
    }

    public UserBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public UserBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public UserBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserBuilder asVip() {
        this.level = MembershipLevel.VIP;
        return this;
    }

    public UserBuilder inactive() {
        this.status = UserStatus.INACTIVE;
        return this;
    }

    public User build() {
        return new User(id, name, email, level, status);
    }
}

// 사용: 테스트에 필요한 속성만 명시적으로 설정
@Test
void vipUserGetsExtraDiscount() {
    User user = aUser().asVip().build();
    // id, name, email은 기본값 사용
    // VIP 여부만 명시적으로 설정
}

@Test
void inactiveUserCannotOrder() {
    User user = aUser().inactive().build();
    // 비활성 상태만 명시적으로 설정
}
```

### 복잡한 객체 그래프

```java
public class OrderBuilder {
    private Long id = 1L;
    private User user = aUser().build();
    private List<OrderItem> items = new ArrayList<>();
    private OrderStatus status = OrderStatus.PENDING;
    private Address shippingAddress = AddressBuilder.aDefault().build();

    public static OrderBuilder anOrder() {
        return new OrderBuilder();
    }

    public OrderBuilder forUser(User user) {
        this.user = user;
        return this;
    }

    public OrderBuilder withItem(ProductBuilder product, int quantity) {
        items.add(OrderItem.builder()
                .product(product.build())
                .quantity(quantity)
                .build());
        return this;
    }

    public OrderBuilder shippingTo(Address address) {
        this.shippingAddress = address;
        return this;
    }

    public Order build() {
        return new Order(id, user, items, status, shippingAddress);
    }
}

// 사용: 읽기 쉬운 테스트
@Test
void shouldCalculateShipping() {
    Order order = anOrder()
            .forUser(aUser().asVip().build())
            .withItem(aProduct().withWeight(5), 2)
            .shippingTo(anAddress().inSeoul().build())
            .build();

    int shipping = shippingService.calculate(order);
    assertThat(shipping).isEqualTo(3000);
}
```

### 도메인 언어로 표현

```java
// 비즈니스 시나리오를 표현하는 빌더
public class OrderScenario {

    public static Order vipOrderWithHeavyItems() {
        return anOrder()
                .forUser(aUser().asVip().build())
                .withItem(aProduct().heavy(), 3)
                .build();
    }

    public static Order firstTimeUserWithCoupon() {
        return anOrder()
                .forUser(aUser().firstTime().build())
                .withCoupon(aCoupon().welcomeDiscount().build())
                .build();
    }
}

// 테스트에서 시나리오 사용
@Test
void heavyItemsGetExtraShippingFee() {
    Order order = OrderScenario.vipOrderWithHeavyItems();
    // ...
}
```

---

## 4. 느린 테스트 개선

### 느린 테스트 원인

```
원인 분석:
┌─────────────────────────────────────────────────────────────┐
│  1. Spring Context 로딩                                     │
│     - 불필요한 빈 로딩                                       │
│     - 컨텍스트 캐시 미스                                     │
│                                                             │
│  2. 데이터베이스                                             │
│     - 실제 DB 연결                                           │
│     - 대량 데이터 INSERT                                     │
│     - 트랜잭션 롤백 오버헤드                                 │
│                                                             │
│  3. 외부 서비스                                              │
│     - 실제 API 호출                                          │
│     - 네트워크 지연                                          │
│                                                             │
│  4. 테스트 설계                                              │
│     - @SpringBootTest 남용                                   │
│     - 불필요한 통합 테스트                                   │
└─────────────────────────────────────────────────────────────┘
```

### 슬라이스 테스트 활용

```java
// ❌ Bad: 전체 컨텍스트 로딩
@SpringBootTest
class UserServiceTest { ... }

// ✅ Good: 필요한 레이어만
@DataJpaTest  // Repository 테스트
class UserRepositoryTest { ... }

@WebMvcTest(UserController.class)  // Controller 테스트
class UserControllerTest { ... }

@JsonTest  // JSON 직렬화 테스트
class UserDtoTest { ... }
```

### 컨텍스트 캐싱 최적화

```java
// 동일한 설정의 테스트는 컨텍스트 공유
// 설정이 다르면 새 컨텍스트 생성

// 같은 컨텍스트 공유
@SpringBootTest
@ActiveProfiles("test")
class Test1 { ... }

@SpringBootTest
@ActiveProfiles("test")
class Test2 { ... }

// 다른 컨텍스트 (캐시 미스)
@SpringBootTest
@ActiveProfiles("test")
@MockBean
private SomeService someService;  // MockBean이 다르면 새 컨텍스트
class Test3 { ... }
```

### 병렬 실행

```properties
# junit-platform.properties
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=concurrent
junit.jupiter.execution.parallel.config.fixed.parallelism=4
```

```java
// 병렬 실행 시 주의: 테스트 격리
@Execution(ExecutionMode.CONCURRENT)
class ParallelTest {

    // 각 테스트가 독립적이어야 함
    // 공유 상태 금지
}

// 격리 필요한 테스트
@Execution(ExecutionMode.SAME_THREAD)
class SequentialTest {
    // 순차 실행
}
```

### 데이터베이스 테스트 최적화

```java
// 1. 테스트 데이터 최소화
@Test
void shouldFindUser() {
    // ❌ Bad: 100개 데이터 INSERT
    for (int i = 0; i < 100; i++) {
        repository.save(createUser(i));
    }

    // ✅ Good: 필요한 만큼만
    repository.save(createUser(1));
}

// 2. 벌크 INSERT 사용
@BeforeAll
static void setupData(@Autowired JdbcTemplate jdbc) {
    jdbc.batchUpdate(
        "INSERT INTO users (name, email) VALUES (?, ?)",
        testData,
        100  // batch size
    );
}

// 3. In-Memory DB 활용 (가능한 경우)
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.ANY)
class FastRepositoryTest { ... }
```

### Mock 전략

```java
// 외부 서비스는 기본적으로 Mock
@MockBean
private PaymentGateway paymentGateway;

@MockBean
private EmailService emailService;

// 빠른 응답 Mock
@BeforeEach
void setUp() {
    given(paymentGateway.process(any()))
            .willReturn(PaymentResult.success());

    doNothing().when(emailService).send(any());
}
```

---

## 5. 테스트 유지보수

### 테스트 명명 규칙

```java
// 패턴 1: should_ExpectedBehavior_When_StateUnderTest
@Test
void should_ReturnDiscount_When_UserIsVip() { ... }

// 패턴 2: given_When_Then
@Test
void givenVipUser_whenCalculateDiscount_thenReturn20Percent() { ... }

// 패턴 3: 한글 메서드명
@Test
void VIP_회원이면_20퍼센트_할인() { ... }

// 패턴 4: @DisplayName 활용
@Test
@DisplayName("VIP 회원은 20% 할인을 받는다")
void vipDiscount() { ... }
```

### 테스트 구조화

```java
@Nested
@DisplayName("사용자 생성")
class CreateUser {

    @Nested
    @DisplayName("성공 케이스")
    class Success {
        @Test
        void 유효한_정보로_생성() { ... }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failure {
        @Test
        void 이메일_중복시_예외() { ... }

        @Test
        void 필수값_누락시_예외() { ... }
    }
}
```

### Assertion 가독성

```java
// ❌ Bad: 실패 시 메시지 불명확
assertTrue(user.isActive());
assertEquals(expected, actual);

// ✅ Good: AssertJ 사용
assertThat(user.isActive())
        .as("사용자가 활성 상태여야 함")
        .isTrue();

assertThat(actual)
        .isEqualTo(expected)
        .describedAs("할인율이 20%여야 함");

// 복잡한 객체 검증
assertThat(user)
        .extracting("name", "email", "status")
        .containsExactly("kim", "kim@test.com", ACTIVE);

// 컬렉션 검증
assertThat(users)
        .hasSize(3)
        .extracting("name")
        .containsExactlyInAnyOrder("kim", "lee", "park");
```

### 테스트 커버리지 함정

```
커버리지 높음 ≠ 좋은 테스트

Bad:
- 단순히 코드 실행만 하고 검증 없음
- 해피 패스만 테스트
- 경계값 테스트 없음

Good:
- 의미 있는 assertion
- 엣지 케이스 포함
- 비즈니스 로직 검증
```

```java
// ❌ Bad: 커버리지만 높임
@Test
void test() {
    service.process(data);
    // assertion 없음
}

// ✅ Good: 의미 있는 검증
@Test
void shouldProcessDataCorrectly() {
    Result result = service.process(data);

    assertThat(result.getStatus()).isEqualTo(SUCCESS);
    assertThat(result.getProcessedCount()).isEqualTo(10);
    verify(repository).save(any());
}
```

---

## 리팩토링 체크리스트

```
□ 중복 코드 제거 (픽스처, 빌더 사용)
□ 테스트와 무관한 데이터 제거
□ 마법의 숫자/문자열 → 상수 또는 빌더
□ 과도한 Mock → 설계 개선 검토
□ @SpringBootTest → 슬라이스 테스트
□ 병렬 실행 가능하도록 격리
□ 테스트 이름으로 의도 표현
□ Given-When-Then 구조
□ AssertJ 활용
□ 경계값/엣지 케이스 테스트 추가
```

---

## 핵심 정리

| 문제 | 해결책 |
|------|--------|
| 중복 셋업 | Object Mother, Builder |
| 가독성 저하 | 도메인 언어 빌더 |
| 느린 테스트 | 슬라이스 테스트, 병렬 실행 |
| 유지보수 어려움 | 명확한 명명, 구조화 |
| 취약한 테스트 | 의미 있는 assertion |

```
좋은 테스트의 특성 (FIRST):
- Fast: 빠르게 실행
- Independent: 서로 독립적
- Repeatable: 반복 가능
- Self-validating: 자동 검증
- Timely: 적시에 작성
```

---

*마지막 업데이트: 2025년 01월*
