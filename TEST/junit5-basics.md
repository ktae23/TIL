# JUnit 5 기초

## 목차
1. [JUnit 5 개요](#junit-5-개요)
2. [주요 어노테이션](#주요-어노테이션)
3. [Assertions](#assertions)
4. [파라미터화 테스트](#파라미터화-테스트)
5. [테스트 생명주기](#테스트-생명주기)
6. [핵심 정리](#핵심-정리)

---

## JUnit 5 개요

### JUnit 5 아키텍처

```
┌──────────────────────────────────────────────────────────────────┐
│                    JUnit 5 아키텍처                               │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  JUnit 5 = JUnit Platform + JUnit Jupiter + JUnit Vintage        │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    JUnit Platform                            │ │
│  │  └── 테스트 실행 기반, IDE/빌드 도구 연동                    │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                              ▲                                    │
│              ┌───────────────┼───────────────┐                   │
│              │               │               │                   │
│  ┌───────────────────┐ ┌───────────────────┐ ┌─────────────────┐│
│  │   JUnit Jupiter   │ │   JUnit Vintage   │ │  Third Party    ││
│  │  └── JUnit 5 API  │ │  └── JUnit 3/4    │ │  └── TestNG 등  ││
│  └───────────────────┘ └───────────────────┘ └─────────────────┘│
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 의존성 설정

```groovy
// build.gradle
dependencies {
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    // 포함: junit-jupiter, mockito, assertj, hamcrest
}

// 개별 추가 시
testImplementation 'org.junit.jupiter:junit-jupiter:5.10.1'
testImplementation 'org.assertj:assertj-core:3.24.2'
testImplementation 'org.mockito:mockito-core:5.8.0'
```

---

## 주요 어노테이션

### 기본 어노테이션

```java
class JUnit5BasicTest {

    @BeforeAll
    static void beforeAll() {
        // 모든 테스트 전 1회 실행
        System.out.println("테스트 클래스 시작");
    }

    @AfterAll
    static void afterAll() {
        // 모든 테스트 후 1회 실행
        System.out.println("테스트 클래스 종료");
    }

    @BeforeEach
    void setUp() {
        // 각 테스트 전 실행
        System.out.println("테스트 시작");
    }

    @AfterEach
    void tearDown() {
        // 각 테스트 후 실행
        System.out.println("테스트 종료");
    }

    @Test
    @DisplayName("간단한 덧셈 테스트")
    void additionTest() {
        assertEquals(4, 2 + 2);
    }

    @Test
    @Disabled("이슈 #123 해결 후 활성화")
    void disabledTest() {
        // 비활성화된 테스트
    }

    @RepeatedTest(5)
    @DisplayName("반복 테스트")
    void repeatedTest(RepetitionInfo info) {
        System.out.println("반복 " + info.getCurrentRepetition() + "/" + info.getTotalRepetitions());
    }
}
```

### 조건부 테스트

```java
class ConditionalTest {

    @Test
    @EnabledOnOs(OS.MAC)
    void onMacOnly() {
        // macOS에서만 실행
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void onLinuxOrMac() {
        // Linux 또는 macOS에서만 실행
    }

    @Test
    @EnabledOnJre(JRE.JAVA_17)
    void onJava17Only() {
        // Java 17에서만 실행
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ENV", matches = "dev")
    void onDevEnv() {
        // ENV=dev 환경변수가 설정된 경우만 실행
    }

    @Test
    @EnabledIfSystemProperty(named = "ci", matches = "true")
    void onCIOnly() {
        // -Dci=true 시스템 프로퍼티가 설정된 경우만 실행
    }

    @Test
    @EnabledIf("customCondition")
    void customConditionTest() {
        // customCondition 메서드가 true 반환 시 실행
    }

    boolean customCondition() {
        return LocalDate.now().getDayOfWeek() != DayOfWeek.SUNDAY;
    }
}
```

### 테스트 그룹화

```java
@Nested
@DisplayName("주문 생성 테스트")
class OrderCreationTest {

    @Nested
    @DisplayName("유효한 주문")
    class ValidOrder {

        @Test
        @DisplayName("정상 주문이 생성된다")
        void createOrder_Success() {
            // ...
        }

        @Test
        @DisplayName("주문 총액이 올바르게 계산된다")
        void calculateTotal_Success() {
            // ...
        }
    }

    @Nested
    @DisplayName("무효한 주문")
    class InvalidOrder {

        @Test
        @DisplayName("빈 항목으로 주문 시 예외 발생")
        void createOrder_EmptyItems_ThrowsException() {
            // ...
        }
    }
}
```

### 태그와 필터링

```java
@Tag("slow")
@Tag("integration")
class SlowIntegrationTest {

    @Test
    @Tag("database")
    void databaseTest() {
        // ...
    }
}

// build.gradle - 태그로 필터링
test {
    useJUnitPlatform {
        includeTags 'fast'
        excludeTags 'slow'
    }
}

// 실행: ./gradlew test -PincludeTags=fast
```

---

## Assertions

### JUnit 5 Assertions

```java
import static org.junit.jupiter.api.Assertions.*;

class AssertionsTest {

    @Test
    void basicAssertions() {
        // 기본 단언
        assertEquals(4, 2 + 2);
        assertEquals(4, 2 + 2, "2 + 2는 4여야 함");

        assertNotEquals(5, 2 + 2);

        assertTrue(3 > 2);
        assertFalse(2 > 3);

        assertNull(null);
        assertNotNull("hello");

        // 동일 객체 참조 확인
        String s1 = "hello";
        String s2 = s1;
        assertSame(s1, s2);
    }

    @Test
    void arrayAssertions() {
        int[] expected = {1, 2, 3};
        int[] actual = {1, 2, 3};
        assertArrayEquals(expected, actual);
    }

    @Test
    void exceptionAssertions() {
        // 예외 발생 확인
        Exception exception = assertThrows(
            IllegalArgumentException.class,
            () -> validateAge(-1)
        );

        assertEquals("나이는 0 이상이어야 합니다", exception.getMessage());

        // 예외가 발생하지 않음 확인
        assertDoesNotThrow(() -> validateAge(20));
    }

    @Test
    void timeoutAssertions() {
        // 시간 제한 내 완료 확인
        assertTimeout(Duration.ofSeconds(2), () -> {
            Thread.sleep(1000);
            return "완료";
        });

        // 시간 초과 시 즉시 중단
        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            Thread.sleep(500);
            return "완료";
        });
    }

    @Test
    void groupedAssertions() {
        User user = new User("홍길동", 30, "hong@example.com");

        // 여러 단언을 그룹화 (하나가 실패해도 모두 실행)
        assertAll("user",
            () -> assertEquals("홍길동", user.getName()),
            () -> assertEquals(30, user.getAge()),
            () -> assertTrue(user.getEmail().contains("@"))
        );
    }

    private void validateAge(int age) {
        if (age < 0) {
            throw new IllegalArgumentException("나이는 0 이상이어야 합니다");
        }
    }
}
```

### AssertJ (권장)

```java
import static org.assertj.core.api.Assertions.*;

class AssertJTest {

    @Test
    void stringAssertions() {
        String name = "Hello World";

        assertThat(name)
            .isNotNull()
            .isNotEmpty()
            .startsWith("Hello")
            .endsWith("World")
            .contains("lo Wo")
            .hasSize(11);
    }

    @Test
    void numberAssertions() {
        int age = 25;

        assertThat(age)
            .isPositive()
            .isGreaterThan(20)
            .isLessThanOrEqualTo(30)
            .isBetween(20, 30);

        BigDecimal price = new BigDecimal("100.00");
        assertThat(price).isEqualByComparingTo(new BigDecimal("100"));
    }

    @Test
    void collectionAssertions() {
        List<String> names = List.of("Alice", "Bob", "Charlie");

        assertThat(names)
            .hasSize(3)
            .contains("Alice", "Bob")
            .containsExactly("Alice", "Bob", "Charlie")
            .doesNotContain("David")
            .first().isEqualTo("Alice");

        assertThat(names)
            .filteredOn(name -> name.length() > 4)
            .containsExactly("Alice", "Charlie");
    }

    @Test
    void objectAssertions() {
        User user = new User("홍길동", 30, "hong@example.com");

        assertThat(user)
            .hasFieldOrPropertyWithValue("name", "홍길동")
            .hasFieldOrPropertyWithValue("age", 30)
            .extracting("email")
            .isEqualTo("hong@example.com");
    }

    @Test
    void exceptionAssertions() {
        assertThatThrownBy(() -> divide(10, 0))
            .isInstanceOf(ArithmeticException.class)
            .hasMessageContaining("zero");

        assertThatCode(() -> divide(10, 2))
            .doesNotThrowAnyException();

        // Catchable Exception
        Throwable thrown = catchThrowable(() -> divide(10, 0));
        assertThat(thrown)
            .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void softAssertions() {
        User user = new User("홍길동", 30, "hong@example.com");

        // Soft assertions: 모든 단언을 실행하고 한번에 결과 보고
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(user.getName()).isEqualTo("홍길동");
        softly.assertThat(user.getAge()).isEqualTo(30);
        softly.assertThat(user.getEmail()).contains("@");
        softly.assertAll();

        // 또는 JUnit 5 Extension 사용
        // @ExtendWith(SoftAssertionsExtension.class)
    }

    private int divide(int a, int b) {
        return a / b;
    }
}
```

---

## 파라미터화 테스트

### 다양한 소스

```java
class ParameterizedTestExamples {

    // @ValueSource: 단일 타입 값 배열
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    @DisplayName("양수 테스트")
    void positiveNumbers(int number) {
        assertTrue(number > 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"hello", "world", "junit"})
    void notEmptyStrings(String value) {
        assertFalse(value.isEmpty());
    }

    // @NullSource, @EmptySource, @NullAndEmptySource
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    void nullEmptyAndBlankStrings(String text) {
        assertTrue(text == null || text.trim().isEmpty());
    }

    // @EnumSource
    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    void allOrderStatuses(OrderStatus status) {
        assertNotNull(status);
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"CREATED", "PAID"})
    void specificOrderStatuses(OrderStatus status) {
        assertTrue(status == OrderStatus.CREATED || status == OrderStatus.PAID);
    }

    // @CsvSource: CSV 형식 데이터
    @ParameterizedTest
    @CsvSource({
        "1, 2, 3",
        "5, 5, 10",
        "10, -5, 5"
    })
    void additionTest(int a, int b, int expected) {
        assertEquals(expected, a + b);
    }

    @ParameterizedTest
    @CsvSource({
        "apple, APPLE",
        "hello world, HELLO WORLD",
        "'foo, bar', 'FOO, BAR'"  // 쉼표 포함 시 따옴표
    })
    void toUpperCase(String input, String expected) {
        assertEquals(expected, input.toUpperCase());
    }

    // @CsvFileSource: CSV 파일에서 읽기
    @ParameterizedTest
    @CsvFileSource(resources = "/test-data.csv", numLinesToSkip = 1)
    void csvFileTest(String name, int age) {
        assertNotNull(name);
        assertTrue(age > 0);
    }

    // @MethodSource: 메서드에서 Arguments 제공
    @ParameterizedTest
    @MethodSource("provideStringsForIsBlank")
    void isBlankTest(String input, boolean expected) {
        assertEquals(expected, input == null || input.trim().isEmpty());
    }

    static Stream<Arguments> provideStringsForIsBlank() {
        return Stream.of(
            Arguments.of(null, true),
            Arguments.of("", true),
            Arguments.of("  ", true),
            Arguments.of("not blank", false)
        );
    }

    // 복잡한 객체 테스트
    @ParameterizedTest
    @MethodSource("provideOrderRequests")
    void orderCreationTest(OrderRequest request, boolean shouldSucceed) {
        if (shouldSucceed) {
            assertDoesNotThrow(() -> orderService.createOrder(request));
        } else {
            assertThrows(InvalidOrderException.class, () -> orderService.createOrder(request));
        }
    }

    static Stream<Arguments> provideOrderRequests() {
        return Stream.of(
            Arguments.of(validOrderRequest(), true),
            Arguments.of(emptyItemsOrderRequest(), false),
            Arguments.of(negativeAmountOrderRequest(), false)
        );
    }

    // @ArgumentsSource: 커스텀 ArgumentsProvider
    @ParameterizedTest
    @ArgumentsSource(CustomArgumentsProvider.class)
    void customArgumentsTest(String value, int expected) {
        assertEquals(expected, value.length());
    }
}

// 커스텀 ArgumentsProvider
class CustomArgumentsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return Stream.of(
            Arguments.of("hello", 5),
            Arguments.of("world", 5),
            Arguments.of("junit", 5)
        );
    }
}
```

### 파라미터 변환

```java
class ParameterConversionTest {

    // 암시적 변환
    @ParameterizedTest
    @ValueSource(strings = {"2025-01-15", "2025-12-31"})
    void implicitConversion(LocalDate date) {
        assertNotNull(date);
    }

    // 명시적 변환
    @ParameterizedTest
    @CsvSource({"CREATED", "PAID", "SHIPPED"})
    void enumConversion(@ConvertWith(OrderStatusConverter.class) OrderStatus status) {
        assertNotNull(status);
    }

    // 커스텀 Aggregator
    @ParameterizedTest
    @CsvSource({
        "홍길동, 30, hong@example.com",
        "김철수, 25, kim@example.com"
    })
    void customAggregator(@AggregateWith(UserAggregator.class) User user) {
        assertNotNull(user);
        assertNotNull(user.getName());
    }
}

class UserAggregator implements ArgumentsAggregator {
    @Override
    public Object aggregateArguments(ArgumentsAccessor accessor, ParameterContext context) {
        return new User(
            accessor.getString(0),
            accessor.getInteger(1),
            accessor.getString(2)
        );
    }
}
```

---

## 테스트 생명주기

### 생명주기 제어

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)  // 클래스당 하나의 인스턴스
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)  // 순서 지정
class LifecycleTest {

    private int counter = 0;

    @BeforeAll
    void beforeAll() {
        // PER_CLASS 모드에서는 static 불필요
        System.out.println("Before All");
    }

    @Test
    @Order(1)
    void firstTest() {
        counter++;
        assertEquals(1, counter);
    }

    @Test
    @Order(2)
    void secondTest() {
        counter++;
        assertEquals(2, counter);  // PER_CLASS이므로 상태 공유
    }

    @AfterAll
    void afterAll() {
        System.out.println("After All, counter = " + counter);
    }
}

// 기본 모드: PER_METHOD (각 테스트마다 새 인스턴스)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class DefaultLifecycleTest {

    @BeforeAll
    static void beforeAll() {
        // PER_METHOD 모드에서는 static 필수
    }
}
```

### 순서 지정 방법

```java
// @Order 어노테이션 기반
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderAnnotationTest {
    @Test @Order(1) void first() {}
    @Test @Order(2) void second() {}
    @Test @Order(3) void third() {}
}

// 메서드 이름 알파벳순
@TestMethodOrder(MethodOrderer.MethodName.class)
class AlphanumericTest {
    @Test void aTest() {}  // 1번
    @Test void bTest() {}  // 2번
    @Test void cTest() {}  // 3번
}

// DisplayName 기반
@TestMethodOrder(MethodOrderer.DisplayName.class)
class DisplayNameOrderTest {
    @Test @DisplayName("1. 첫번째") void first() {}
    @Test @DisplayName("2. 두번째") void second() {}
}

// 랜덤 순서
@TestMethodOrder(MethodOrderer.Random.class)
class RandomOrderTest {
    @Test void test1() {}
    @Test void test2() {}
    @Test void test3() {}
}
```

---

## 핵심 정리

### JUnit 5 주요 어노테이션

| 어노테이션 | 설명 |
|-----------|------|
| @Test | 테스트 메서드 |
| @DisplayName | 테스트 이름 |
| @BeforeEach/@AfterEach | 각 테스트 전/후 |
| @BeforeAll/@AfterAll | 전체 테스트 전/후 |
| @Disabled | 테스트 비활성화 |
| @Nested | 중첩 테스트 클래스 |
| @Tag | 테스트 태그 |
| @ParameterizedTest | 파라미터화 테스트 |

### 파라미터화 테스트 소스

| 소스 | 용도 |
|------|------|
| @ValueSource | 단일 타입 배열 |
| @CsvSource | CSV 형식 인라인 |
| @CsvFileSource | CSV 파일 |
| @MethodSource | 메서드에서 제공 |
| @EnumSource | Enum 값 |
| @ArgumentsSource | 커스텀 Provider |

### 실무 기반 핵심 질문

1. **Q: JUnit 4와 JUnit 5의 주요 차이점은?**
   - A: JUnit 5는 모듈화(Platform, Jupiter, Vintage), 람다 지원, @Nested, @DisplayName, @ParameterizedTest, Extension 모델 등 추가. @Before→@BeforeEach, @RunWith→@ExtendWith

2. **Q: @BeforeAll과 @BeforeEach의 차이점은?**
   - A: @BeforeAll은 클래스당 1회(static), @BeforeEach는 각 테스트 메서드 전 실행. TestInstance.Lifecycle.PER_CLASS 사용 시 @BeforeAll도 non-static 가능

3. **Q: 파라미터화 테스트의 장점은?**
   - A: 코드 중복 제거, 다양한 입력값 테스트 용이. @MethodSource로 복잡한 객체 테스트, @CsvSource로 간단한 케이스 테스트

4. **Q: assertAll()의 용도는?**
   - A: 여러 단언을 그룹화하여 하나가 실패해도 모두 실행. 실패한 모든 단언 결과를 한번에 확인 가능

---

*마지막 업데이트: 2026년 01월*
