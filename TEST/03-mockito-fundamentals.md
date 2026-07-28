# Mockito 기초

## 목차
1. [Mockito 개요](#mockito-개요)
2. [Mock vs Stub vs Spy](#mock-vs-stub-vs-spy)
3. [Mock 생성과 주입](#mock-생성과-주입)
4. [Stubbing](#stubbing)
5. [Verify 패턴](#verify-패턴)
6. [핵심 정리](#핵심-정리)

---

## Mockito 개요

### 테스트 더블 (Test Double)

```
┌──────────────────────────────────────────────────────────────────┐
│                    테스트 더블 종류                               │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  1. Dummy                                                        │
│     - 전달되기만 하고 실제로 사용되지 않음                        │
│     - 파라미터 채우기 용도                                       │
│                                                                   │
│  2. Fake                                                         │
│     - 동작하는 구현체이나 프로덕션에 적합하지 않음                │
│     - 예: InMemoryRepository                                     │
│                                                                   │
│  3. Stub                                                         │
│     - 미리 정해진 답변을 반환                                    │
│     - 상태 검증 (State Verification)                             │
│                                                                   │
│  4. Mock                                                         │
│     - 호출 기대치를 설정하고 검증                                │
│     - 행위 검증 (Behavior Verification)                          │
│                                                                   │
│  5. Spy                                                          │
│     - 실제 객체를 감싸서 일부 메서드만 가짜로                    │
│     - Partial Mock                                               │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 의존성 설정

```groovy
// build.gradle
dependencies {
    testImplementation 'org.mockito:mockito-core:5.8.0'
    testImplementation 'org.mockito:mockito-junit-jupiter:5.8.0'
}

// Spring Boot 사용 시 spring-boot-starter-test에 포함됨
```

---

## Mock vs Stub vs Spy

### Mock

```java
// Mock: 기대 행위를 설정하고 검증
@ExtendWith(MockitoExtension.class)
class MockExampleTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_SendsWelcomeEmail() {
        // Given
        UserRequest request = new UserRequest("test@example.com", "password");
        User user = new User(1L, "test@example.com");

        when(userRepository.save(any(User.class))).thenReturn(user);
        doNothing().when(emailService).sendWelcomeEmail(anyString());

        // When
        userService.createUser(request);

        // Then - 행위 검증
        verify(userRepository).save(any(User.class));
        verify(emailService).sendWelcomeEmail("test@example.com");
    }
}
```

### Stub

```java
// Stub: 미리 정해진 응답 반환
@Test
void getUser_ReturnsUser() {
    // Given - Stub 설정
    User stubUser = new User(1L, "test@example.com");
    when(userRepository.findById(1L)).thenReturn(Optional.of(stubUser));

    // When
    User result = userService.getUser(1L);

    // Then - 상태 검증
    assertThat(result.getEmail()).isEqualTo("test@example.com");
}

// 다양한 Stubbing 방법
@Test
void stubbingExamples() {
    // 단순 반환
    when(repository.findById(1L)).thenReturn(Optional.of(user));

    // 예외 발생
    when(repository.findById(999L)).thenThrow(new NotFoundException());

    // 여러 번 호출 시 다른 결과
    when(repository.count())
        .thenReturn(0L)
        .thenReturn(1L)
        .thenReturn(2L);

    // 인자에 따라 다른 결과 (Answer)
    when(repository.findById(anyLong())).thenAnswer(invocation -> {
        Long id = invocation.getArgument(0);
        return Optional.of(new User(id, "user" + id + "@example.com"));
    });
}
```

### Spy

```java
// Spy: 실제 객체의 일부 메서드만 가짜로
@ExtendWith(MockitoExtension.class)
class SpyExampleTest {

    @Spy
    private ArrayList<String> spyList = new ArrayList<>();

    @Test
    void spyExample() {
        // 실제 메서드 호출
        spyList.add("one");
        spyList.add("two");

        assertThat(spyList).hasSize(2);

        // 특정 메서드만 Stub
        doReturn(100).when(spyList).size();

        assertThat(spyList.size()).isEqualTo(100);
        assertThat(spyList.get(0)).isEqualTo("one");  // 실제 동작
    }

    @Spy
    private OrderService orderService = new OrderService(repository, client);

    @Test
    void partialMock() {
        // 일부 메서드만 Stub
        doReturn(new BigDecimal("100")).when(orderService).calculateDiscount(any());

        // 나머지는 실제 동작
        Order order = orderService.createOrder(request);

        assertThat(order.getDiscount()).isEqualByComparingTo(new BigDecimal("100"));
    }
}
```

### Mock vs Stub vs Spy 비교

| 특성 | Mock | Stub | Spy |
|------|------|------|-----|
| 실제 객체 | X | X | O (래핑) |
| 행위 검증 | O (verify) | X | O |
| 상태 검증 | O | O (주 목적) | O |
| 모든 메서드 가짜 | O | O | X (선택적) |
| 사용 시점 | 상호작용 검증 | 입력→출력 검증 | 부분 Mock |

---

## Mock 생성과 주입

### 어노테이션 기반

```java
@ExtendWith(MockitoExtension.class)
class AnnotationBasedTest {

    @Mock  // Mock 객체 생성
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Spy  // Spy 객체 생성
    private OrderCalculator orderCalculator = new OrderCalculator();

    @InjectMocks  // Mock들을 주입받는 객체
    private UserService userService;

    @Captor  // ArgumentCaptor 생성
    private ArgumentCaptor<User> userCaptor;

    @Test
    void test() {
        // ...
    }
}
```

### 프로그래밍 방식

```java
class ProgrammaticMockTest {

    @Test
    void createMockProgrammatically() {
        // Mock 생성
        UserRepository mockRepository = mock(UserRepository.class);

        // Spy 생성
        ArrayList<String> spyList = spy(new ArrayList<>());

        // lenient Mock (unused stubbing 경고 무시)
        UserRepository lenientMock = mock(UserRepository.class, withSettings().lenient());

        // 특정 설정으로 Mock 생성
        UserRepository strictMock = mock(UserRepository.class, withSettings()
            .strictness(Strictness.STRICT_STUBS)
            .name("userRepositoryMock"));
    }
}
```

### @InjectMocks 동작 방식

```java
// 주입 우선순위:
// 1. 생성자 주입
// 2. Setter 주입
// 3. 필드 주입

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;

    // 생성자 주입 (권장)
    public OrderService(OrderRepository orderRepository, PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
    }
}

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentService paymentService;

    @InjectMocks  // 생성자를 통해 Mock 주입
    private OrderService orderService;

    @Test
    void test() {
        // orderService는 Mock이 주입된 상태
    }
}
```

---

## Stubbing

### when-thenReturn

```java
class StubbingTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void whenThenReturn() {
        User user = new User(1L, "test@example.com");

        // 기본 stubbing
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        when(userRepository.count()).thenReturn(100L);

        // 결과 확인
        assertThat(userRepository.findById(1L)).contains(user);
        assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
        assertThat(userRepository.count()).isEqualTo(100L);
    }

    @Test
    void consecutiveStubbing() {
        // 연속 호출 시 다른 결과
        when(userRepository.count())
            .thenReturn(0L)
            .thenReturn(1L)
            .thenReturn(2L);

        assertThat(userRepository.count()).isEqualTo(0L);
        assertThat(userRepository.count()).isEqualTo(1L);
        assertThat(userRepository.count()).isEqualTo(2L);
        assertThat(userRepository.count()).isEqualTo(2L);  // 마지막 값 반복
    }

    @Test
    void thenThrow() {
        // 예외 발생
        when(userRepository.findById(999L))
            .thenThrow(new NotFoundException("User not found"));

        assertThatThrownBy(() -> userRepository.findById(999L))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void thenAnswer() {
        // 동적 응답
        when(userRepository.findById(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            if (id <= 0) {
                return Optional.empty();
            }
            return Optional.of(new User(id, "user" + id + "@example.com"));
        });

        assertThat(userRepository.findById(1L))
            .isPresent()
            .get()
            .extracting(User::getEmail)
            .isEqualTo("user1@example.com");
    }
}
```

### Argument Matchers

```java
class ArgumentMatchersTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void argumentMatchers() {
        // any() - 모든 값
        when(userRepository.findById(any())).thenReturn(Optional.of(new User()));

        // anyLong(), anyString(), anyInt() 등
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(new User()));

        // eq() - 특정 값
        when(userRepository.findById(eq(1L))).thenReturn(Optional.of(new User()));

        // null 처리
        when(userRepository.findByEmail(isNull())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(isNotNull())).thenReturn(Optional.of(new User()));

        // 조건 매칭
        when(userRepository.findById(argThat(id -> id > 0)))
            .thenReturn(Optional.of(new User()));

        // 문자열 매칭
        when(userRepository.findByEmail(contains("@example.com")))
            .thenReturn(Optional.of(new User()));

        when(userRepository.findByEmail(startsWith("admin")))
            .thenReturn(Optional.of(new User()));
    }

    @Test
    void mixedArgumentMatchers() {
        // ⚠️ Matcher 사용 시 모든 인자에 Matcher 사용 필요
        // 잘못된 예:
        // when(service.method(anyLong(), "fixed")).thenReturn(...);  // 에러!

        // 올바른 예:
        when(userRepository.findByNameAndAge(eq("홍길동"), anyInt()))
            .thenReturn(Optional.of(new User()));
    }
}
```

### doReturn-when (void 메서드, Spy)

```java
class DoReturnTest {

    @Mock
    private EmailService emailService;

    @Spy
    private ArrayList<String> spyList = new ArrayList<>();

    @Test
    void voidMethodStubbing() {
        // void 메서드 stubbing
        doNothing().when(emailService).sendEmail(anyString(), anyString());

        // void 메서드 예외 발생
        doThrow(new RuntimeException("메일 전송 실패"))
            .when(emailService).sendEmail(eq("invalid"), anyString());

        // 실행
        emailService.sendEmail("valid@example.com", "Hello");  // 정상
        assertThatThrownBy(() -> emailService.sendEmail("invalid", "Hello"))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void spyStubbing() {
        // Spy는 doReturn 사용 권장 (실제 메서드 호출 방지)

        // ⚠️ when-thenReturn은 실제 메서드를 먼저 호출
        // when(spyList.get(0)).thenReturn("stubbed");  // IndexOutOfBoundsException!

        // ✅ doReturn은 실제 메서드 호출 없이 stubbing
        doReturn("stubbed").when(spyList).get(0);

        spyList.add("real");
        assertThat(spyList.get(0)).isEqualTo("stubbed");  // stubbed 반환
        assertThat(spyList.size()).isEqualTo(1);  // 실제 동작
    }
}
```

---

## Verify 패턴

### 기본 검증

```java
class VerifyTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    @Test
    void verifyMethodCalls() {
        // Given
        when(userRepository.save(any())).thenReturn(new User(1L, "test@example.com"));

        // When
        userService.createUser(new UserRequest("test@example.com"));

        // Then - 호출 검증
        verify(userRepository).save(any(User.class));  // 1회 호출
        verify(emailService).sendWelcomeEmail("test@example.com");
    }

    @Test
    void verifyCallCount() {
        userService.processUsers(List.of(user1, user2, user3));

        // 호출 횟수 검증
        verify(userRepository, times(3)).save(any());
        verify(emailService, times(3)).sendEmail(anyString());

        // 최소/최대 횟수
        verify(userRepository, atLeast(1)).save(any());
        verify(userRepository, atMost(5)).save(any());
        verify(userRepository, atLeastOnce()).save(any());

        // 호출 안 됨
        verify(userRepository, never()).delete(any());
    }

    @Test
    void verifyNoMoreInteractions() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.getUser(1L);

        verify(userRepository).findById(1L);
        verifyNoMoreInteractions(userRepository);  // 다른 상호작용 없음
    }

    @Test
    void verifyNoInteractions() {
        userService.doSomethingWithoutRepository();

        verifyNoInteractions(userRepository);  // 어떤 상호작용도 없음
    }
}
```

### ArgumentCaptor

```java
class ArgumentCaptorTest {

    @Mock
    private UserRepository userRepository;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @InjectMocks
    private UserService userService;

    @Test
    void captureArgument() {
        // Given
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        userService.createUser(new UserRequest("test@example.com", "password123"));

        // Then - 인자 캡처 및 검증
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(capturedUser.getPassword()).isNotEqualTo("password123");  // 암호화됨
    }

    @Test
    void captureMultipleArguments() {
        // When
        userService.createUsers(List.of(
            new UserRequest("user1@example.com"),
            new UserRequest("user2@example.com"),
            new UserRequest("user3@example.com")
        ));

        // Then - 모든 호출의 인자 캡처
        verify(userRepository, times(3)).save(userCaptor.capture());

        List<User> capturedUsers = userCaptor.getAllValues();
        assertThat(capturedUsers).hasSize(3);
        assertThat(capturedUsers)
            .extracting(User::getEmail)
            .containsExactly("user1@example.com", "user2@example.com", "user3@example.com");
    }
}
```

### 순서 검증

```java
class VerifyOrderTest {

    @Mock
    private AuditService auditService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void verifyInOrder() {
        // When
        userService.createUser(request);

        // Then - 순서 검증
        InOrder inOrder = inOrder(userRepository, auditService);

        // 이 순서대로 호출되었는지 검증
        inOrder.verify(auditService).logAction("USER_CREATION_STARTED");
        inOrder.verify(userRepository).save(any());
        inOrder.verify(auditService).logAction("USER_CREATION_COMPLETED");
    }
}
```

---

## 핵심 정리

### Mock vs Stub vs Spy 선택

| 상황 | 선택 |
|------|------|
| 반환값만 필요 | Stub (when-thenReturn) |
| 호출 여부/횟수 검증 | Mock (verify) |
| 실제 객체 일부만 가짜 | Spy (doReturn-when) |
| void 메서드 처리 | doNothing/doThrow |

### Mockito 핵심 패턴

```java
// 기본 패턴
when(mock.method()).thenReturn(value);
verify(mock).method();

// void 메서드
doNothing().when(mock).voidMethod();
doThrow(exception).when(mock).voidMethod();

// Spy
doReturn(value).when(spy).method();

// 인자 검증
verify(mock).method(argCaptor.capture());
assertThat(argCaptor.getValue()).isEqualTo(expected);
```

### 실무 기반 핵심 질문

1. **Q: Mock과 Stub의 차이점은?**
   - A: Stub은 미리 정해진 응답 반환(상태 검증), Mock은 기대 행위 설정 후 검증(행위 검증). Mockito에서는 when으로 Stub, verify로 Mock 패턴 구현

2. **Q: @Mock과 @Spy의 차이점은?**
   - A: @Mock은 모든 메서드가 가짜, @Spy는 실제 객체를 래핑하여 일부만 가짜. Spy는 doReturn-when 사용 권장

3. **Q: ArgumentCaptor는 언제 사용하나요?**
   - A: 메서드에 전달된 인자를 캡처하여 검증할 때. 복잡한 객체의 특정 필드 값 검증, 여러 호출의 인자 수집 등

4. **Q: verify와 when의 차이점은?**
   - A: when은 Stubbing(응답 설정), verify는 호출 검증. when은 테스트 실행 전 설정, verify는 실행 후 검증

---

*마지막 업데이트: 2026년 01월*
