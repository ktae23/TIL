# Java 버전별 주요 변경점

Java 8부터 최신 버전까지 주요 문법과 기능 변경사항을 체계적으로 정리한다. LTS 버전을 중심으로 각 버전별 핵심 기능과 실제 코드 예시를 포함한다.

## 목차

- [버전별 릴리스 개요](#버전별-릴리스-개요)
- [Java 8 (2014)](#java-8-2014)
- [Java 11 (2018, LTS)](#java-11-2018-lts)
- [Java 14 (2020)](#java-14-2020)
- [Java 15 (2020)](#java-15-2020)
- [Java 16 (2021)](#java-16-2021)
- [Java 17 (2021, LTS)](#java-17-2021-lts)
- [Java 21 (2023, LTS)](#java-21-2023-lts)
- [Java 22 (2024)](#java-22-2024)
- [Java 23 (2024)](#java-23-2024)
- [Java 24 (2025)](#java-24-2025)
- [버전별 기능 비교표](#버전별-기능-비교표)
- [마이그레이션 가이드](#마이그레이션-가이드)

## 버전별 릴리스 개요

| 버전 | 릴리스 | 유형 | 주요 특징 |
|------|--------|------|----------|
| **Java 8** | 2014-03 | **LTS** | Lambda, Stream API, Optional |
| **Java 11** | 2018-09 | **LTS** | var 키워드, String 개선, Files 개선 |
| **Java 14** | 2020-03 | Non-LTS | Switch Expression, NullPointerException 개선 |
| **Java 15** | 2020-09 | Non-LTS | Text Block |
| **Java 16** | 2021-03 | Non-LTS | Record, instanceof 패턴 매칭 |
| **Java 17** | 2021-09 | **LTS** | Sealed Class |
| **Java 21** | 2023-09 | **LTS** | Virtual Threads, Pattern Matching, Sequenced Collections |
| **Java 22** | 2024-03 | Non-LTS | Unnamed Variables, FFM API, Stream Gatherers (Preview) |
| **Java 23** | 2024-09 | Non-LTS | Primitive Patterns (Preview), Markdown Doc Comments |
| **Java 24** | 2025-03 | Non-LTS | Stream Gatherers 정식, Compact Object Headers |

---

## Java 8 (2014)

### Lambda Expression

```java
// 기존 방식
Runnable r1 = new Runnable() {
    @Override
    public void run() {
        System.out.println("Hello");
    }
};

// 람다 표현식
Runnable r2 = () -> System.out.println("Hello");

// 파라미터가 있는 경우
Comparator<String> comp = (s1, s2) -> s1.compareTo(s2);
```

### Stream API

```java
List<String> names = Arrays.asList("Kim", "Lee", "Park", "Choi");

// 필터링 + 변환 + 수집
List<String> result = names.stream()
    .filter(name -> name.length() > 2)
    .map(String::toUpperCase)
    .sorted()
    .collect(Collectors.toList());

// 집계
long count = names.stream()
    .filter(name -> name.startsWith("K"))
    .count();
```

### Optional

```java
// Optional 생성
Optional<String> opt1 = Optional.of("value");
Optional<String> opt2 = Optional.ofNullable(null);
Optional<String> opt3 = Optional.empty();

// 값 사용
String value = opt1
    .filter(s -> s.length() > 3)
    .map(String::toUpperCase)
    .orElse("default");

// null 체크 대체
Optional.ofNullable(user)
    .map(User::getAddress)
    .map(Address::getCity)
    .orElse("Unknown");
```

### Method Reference

```java
// 정적 메서드 참조
Function<String, Integer> parser = Integer::parseInt;

// 인스턴스 메서드 참조
Consumer<String> printer = System.out::println;

// 생성자 참조
Supplier<ArrayList<String>> listSupplier = ArrayList::new;
```

---

## Java 11 (2018, LTS)

### var 키워드 (Java 10에서 도입)

```java
// 타입 추론
var list = new ArrayList<String>();
var stream = list.stream();
var map = new HashMap<String, Integer>();

// 람다에서 var 사용 (Java 11)
Consumer<String> consumer = (var s) -> System.out.println(s);

// 어노테이션과 함께 사용
BiConsumer<String, String> bc = (@NonNull var a, @NonNull var b) -> {};
```

### String 새 메서드

```java
// isBlank() - 공백만 있는지 확인
"   ".isBlank();  // true

// lines() - 줄 단위로 스트림 생성
"line1\nline2\nline3".lines()
    .forEach(System.out::println);

// strip() - 유니코드 공백 제거 (trim보다 개선)
"  hello  ".strip();      // "hello"
"  hello  ".stripLeading();  // "hello  "
"  hello  ".stripTrailing(); // "  hello"

// repeat() - 문자열 반복
"ab".repeat(3);  // "ababab"
```

### Files 개선

```java
// 파일 읽기/쓰기 간소화
String content = Files.readString(Path.of("file.txt"));
Files.writeString(Path.of("file.txt"), "Hello World");
```

---

## Java 14 (2020)

### Switch Expression (표준)

```java
// 기존 switch 문
String result1;
switch (day) {
    case MONDAY:
    case FRIDAY:
        result1 = "Work";
        break;
    case SATURDAY:
    case SUNDAY:
        result1 = "Rest";
        break;
    default:
        result1 = "Unknown";
}

// switch 표현식 (화살표 사용)
String result2 = switch (day) {
    case MONDAY, FRIDAY -> "Work";
    case SATURDAY, SUNDAY -> "Rest";
    default -> "Unknown";
};

// yield로 값 반환
String result3 = switch (day) {
    case MONDAY, FRIDAY -> "Work";
    case SATURDAY, SUNDAY -> {
        System.out.println("Weekend!");
        yield "Rest";
    }
    default -> "Unknown";
};
```

### NullPointerException 메시지 개선

```java
// Java 14 이전
// Exception in thread "main" java.lang.NullPointerException

// Java 14 이후 - 어떤 변수가 null인지 명시
// Exception in thread "main" java.lang.NullPointerException:
//   Cannot invoke "String.length()" because "a.b.c" is null
```

---

## Java 15 (2020)

### Text Block (표준)

```java
// 기존 방식
String json1 = "{\n" +
    "  \"name\": \"John\",\n" +
    "  \"age\": 30\n" +
    "}";

// Text Block
String json2 = """
    {
      "name": "John",
      "age": 30
    }
    """;

// SQL 쿼리
String query = """
    SELECT id, name, email
    FROM users
    WHERE status = 'ACTIVE'
    ORDER BY created_at DESC
    """;
```

---

## Java 16 (2021)

### Record Class

```java
// 기존 데이터 클래스 (보일러플레이트 다수)
public class PersonOld {
    private final String name;
    private final int age;

    public PersonOld(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() { return name; }
    public int getAge() { return age; }

    @Override
    public boolean equals(Object o) { /* ... */ }
    @Override
    public int hashCode() { /* ... */ }
    @Override
    public String toString() { /* ... */ }
}

// Record (간결한 불변 데이터 클래스)
public record Person(String name, int age) {}

// 사용
Person person = new Person("Kim", 25);
String name = person.name();  // getter
int age = person.age();

// 커스텀 생성자 (유효성 검사)
public record Person(String name, int age) {
    public Person {
        if (age < 0) {
            throw new IllegalArgumentException("Age cannot be negative");
        }
    }
}

// 추가 메서드 정의
public record Person(String name, int age) {
    public String greeting() {
        return "Hello, I'm " + name;
    }
}
```

### instanceof Pattern Matching

```java
// 기존 방식
if (obj instanceof String) {
    String s = (String) obj;
    System.out.println(s.length());
}

// 패턴 매칭
if (obj instanceof String s) {
    System.out.println(s.length());
}

// 조건과 함께 사용
if (obj instanceof String s && s.length() > 5) {
    System.out.println(s.toUpperCase());
}
```

---

## Java 17 (2021, LTS)

### Sealed Class

```java
// sealed 클래스 - 상속 가능한 클래스 제한
public sealed class Shape
    permits Circle, Rectangle, Square {
}

// final - 더 이상 상속 불가
public final class Circle extends Shape {
    private final double radius;

    public Circle(double radius) {
        this.radius = radius;
    }
}

// sealed - 추가 제한적 상속 허용
public sealed class Rectangle extends Shape
    permits FilledRectangle {
    // ...
}

// non-sealed - 자유로운 상속 허용
public non-sealed class Square extends Shape {
    // ...
}

// record도 sealed의 하위 타입 가능
public sealed interface Result<T> {
    record Success<T>(T value) implements Result<T> {}
    record Failure<T>(String error) implements Result<T> {}
}
```

---

## Java 21 (2023, LTS)

JDK 17 이후 새로운 LTS 버전으로, Virtual Threads와 Pattern Matching이 정식 도입되었다.

### Virtual Threads (JEP 444) - 정식

기존 Platform Thread 대비 가벼운 경량 스레드로, 높은 동시성 처리가 가능하다.

```java
// Virtual Thread 생성
Thread vThread = Thread.ofVirtual().start(() -> {
    System.out.println("Running in virtual thread: " + Thread.currentThread());
});

// ExecutorService와 함께 사용
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    IntStream.range(0, 10_000).forEach(i -> {
        executor.submit(() -> {
            Thread.sleep(Duration.ofSeconds(1));
            return i;
        });
    });
}  // executor.close() 자동 호출, 모든 작업 완료 대기

// Thread.Builder를 통한 세밀한 제어
Thread.Builder builder = Thread.ofVirtual().name("worker-", 0);
Thread worker1 = builder.start(() -> doWork());
Thread worker2 = builder.start(() -> doWork());

// 기존 플랫폼 스레드
Thread pThread = Thread.ofPlatform().start(() -> {
    System.out.println("Running in platform thread");
});
```

#### Platform Thread vs Virtual Thread

| 구분 | Platform Thread | Virtual Thread |
|------|-----------------|----------------|
| **생성 비용** | 높음 (~1MB 스택) | 낮음 (~수 KB) |
| **개수 제한** | OS 레벨 제한 | 수백만 개 가능 |
| **스케줄링** | OS 스케줄러 | JVM 스케줄러 |
| **적합한 작업** | CPU 바운드 | I/O 바운드 |
| **동기화** | synchronized 사용 | synchronized 시 pinning 주의 |

### Pattern Matching for switch (JEP 441) - 정식

switch 표현식에서 패턴 매칭을 사용할 수 있다.

```java
// 타입 패턴 매칭
static String formatValue(Object obj) {
    return switch (obj) {
        case Integer i -> String.format("int %d", i);
        case Long l    -> String.format("long %d", l);
        case Double d  -> String.format("double %f", d);
        case String s  -> String.format("String %s", s);
        case null      -> "null";
        default        -> obj.toString();
    };
}

// 가드 패턴 (when 절)
static String categorize(Object obj) {
    return switch (obj) {
        case Integer i when i > 0 -> "positive integer";
        case Integer i when i < 0 -> "negative integer";
        case Integer i            -> "zero";
        case String s when s.isEmpty() -> "empty string";
        case String s             -> "string: " + s;
        default                   -> "unknown";
    };
}

// sealed 클래스와 함께 (exhaustive matching)
sealed interface Shape permits Circle, Rectangle {}
record Circle(double radius) implements Shape {}
record Rectangle(double w, double h) implements Shape {}

static double area(Shape shape) {
    return switch (shape) {
        case Circle(double r) -> Math.PI * r * r;
        case Rectangle(double w, double h) -> w * h;
        // default 불필요 - sealed이므로 모든 케이스 커버
    };
}
```

### Record Patterns (JEP 440) - 정식

Record의 구성 요소를 직접 분해할 수 있다.

```java
// 레코드 정의
record Point(int x, int y) {}
record Circle(Point center, int radius) {}

// 중첩 레코드 패턴 분해
static void printCenter(Object obj) {
    if (obj instanceof Circle(Point(int x, int y), int r)) {
        System.out.println("Center: (" + x + ", " + y + ")");
        System.out.println("Radius: " + r);
    }
}

// switch와 함께 사용
static String describe(Object obj) {
    return switch (obj) {
        case Circle(Point(int x, int y), int r) when r > 10
            -> "Large circle at (" + x + ", " + y + ")";
        case Circle(Point(int x, int y), int r)
            -> "Circle at (" + x + ", " + y + ") with radius " + r;
        case Point(int x, int y)
            -> "Point at (" + x + ", " + y + ")";
        default -> "Unknown shape";
    };
}

// 중첩 Record 분해 (Employee 예시)
record Person(String name, int age) {}
record Employee(Person person, String department) {}

static void process(Object obj) {
    if (obj instanceof Employee(Person(String name, int age), String dept)) {
        System.out.printf("%s (%d) works in %s%n", name, age, dept);
    }
}

// 향상된 for문에서 사용
record Pair<T, U>(T first, U second) {}

List<Pair<String, Integer>> pairs = List.of(
    new Pair<>("apple", 1),
    new Pair<>("banana", 2)
);

for (Pair<String, Integer>(var fruit, var count) : pairs) {
    System.out.println(fruit + ": " + count);
}
```

### Sequenced Collections (JEP 431) - 정식

순서가 있는 컬렉션을 위한 새로운 인터페이스가 추가되었다.

```java
// SequencedCollection 인터페이스
SequencedCollection<String> list = new ArrayList<>();
list.addFirst("first");
list.addLast("last");
String first = list.getFirst();
String last = list.getLast();
list.removeFirst();
list.removeLast();
SequencedCollection<String> reversed = list.reversed();

// SequencedSet 인터페이스
SequencedSet<String> set = new LinkedHashSet<>();
set.addFirst("a");
set.addLast("z");
SequencedSet<String> reversedSet = set.reversed();

// SequencedMap 인터페이스
SequencedMap<String, Integer> map = new LinkedHashMap<>();
map.putFirst("first", 1);
map.putLast("last", 99);
Map.Entry<String, Integer> firstEntry = map.firstEntry();
Map.Entry<String, Integer> lastEntry = map.lastEntry();
map.pollFirstEntry();
map.pollLastEntry();
SequencedMap<String, Integer> reversedMap = map.reversed();
```

#### 컬렉션 계층 구조 변경

```
                    Collection
                        │
              SequencedCollection (New)
               ╱                ╲
              ╱                  ╲
           List            SequencedSet (New)
                            ╱         ╲
                       SortedSet   LinkedHashSet
                           │
                      NavigableSet
```

### String Templates (JEP 430) - Preview

문자열 보간 기능 (Preview 단계).

```java
// STR 프로세서 (Preview)
String name = "World";
int year = 2024;
String message = STR."Hello, \{name}! Welcome to \{year}.";

// 표현식 사용
int x = 10, y = 20;
String result = STR."\{x} + \{y} = \{x + y}";

// 여러 줄 문자열
String json = STR."""
    {
        "name": "\{name}",
        "year": \{year}
    }
    """;

// FMT 프로세서 - 포맷팅 지원 (Preview)
double price = 19.99;
String formatted = FMT."Price: $%.2f\{price}";
```

> **주의**: String Templates는 JDK 23에서 Preview가 철회되었다.

### 기타 주요 변경사항

```java
// Unnamed Patterns and Variables (Preview) - JEP 443
// 사용하지 않는 변수를 _로 표시
record Box<T>(T content) {}

if (obj instanceof Box<?>(var _)) {
    System.out.println("It's a Box!");
}

try {
    // ...
} catch (Exception _) {
    System.out.println("Exception occurred");
}

// Key Encapsulation Mechanism API (JEP 452)
KeyPairGenerator g = KeyPairGenerator.getInstance("X25519");
KeyPair kp = g.generateKeyPair();
KEM kem = KEM.getInstance("DHKEM");
KEM.Encapsulator enc = kem.newEncapsulator(kp.getPublic());
KEM.Encapsulated encapsulated = enc.encapsulate();
```

---

## Java 22 (2024)

### Unnamed Variables & Patterns (JEP 456) - 정식

Preview였던 언더스코어 변수가 정식 기능이 되었다.

```java
// 사용하지 않는 변수
for (int _ = 0; _ < 5; _++) {
    System.out.println("Iteration");
}

// 람다에서 사용하지 않는 파라미터
map.forEach((_, value) -> System.out.println(value));

// try-with-resources에서 사용하지 않는 변수
try (var _ = ScopedValue.where(USER, user).call(() -> process())) {
    // USER가 설정된 상태로 실행
}

// 패턴 매칭에서
if (obj instanceof Point(int x, int _)) {
    System.out.println("x = " + x);  // y는 무시
}

// switch에서
String result = switch (obj) {
    case Integer _ -> "integer";
    case String _  -> "string";
    default        -> "other";
};
```

### Foreign Function & Memory API (JEP 454) - 정식

네이티브 코드 및 메모리와의 상호작용을 위한 API가 정식 도입되었다.

```java
// 네이티브 메모리 할당 및 사용
try (Arena arena = Arena.ofConfined()) {
    // 메모리 세그먼트 할당
    MemorySegment segment = arena.allocate(100);

    // 값 쓰기/읽기
    segment.set(ValueLayout.JAVA_INT, 0, 42);
    int value = segment.get(ValueLayout.JAVA_INT, 0);

    // 구조체 표현
    MemoryLayout pointLayout = MemoryLayout.structLayout(
        ValueLayout.JAVA_INT.withName("x"),
        ValueLayout.JAVA_INT.withName("y")
    );

    MemorySegment point = arena.allocate(pointLayout);
    VarHandle xHandle = pointLayout.varHandle(PathElement.groupElement("x"));
    VarHandle yHandle = pointLayout.varHandle(PathElement.groupElement("y"));

    xHandle.set(point, 0L, 10);
    yHandle.set(point, 0L, 20);
}

// 네이티브 함수 호출
Linker linker = Linker.nativeLinker();
SymbolLookup stdlib = linker.defaultLookup();

MethodHandle strlen = linker.downcallHandle(
    stdlib.find("strlen").orElseThrow(),
    FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
);

try (Arena arena = Arena.ofConfined()) {
    MemorySegment str = arena.allocateFrom("Hello");
    long len = (long) strlen.invoke(str);  // 5
}
```

### Statements before super() (JEP 447) - Preview

생성자에서 super() 호출 전에 문장을 실행할 수 있다.

```java
// JDK 21 이전 - 정적 메서드 우회 필요
class Before extends Parent {
    Before(int value) {
        super(validate(value));  // 정적 메서드로 우회
    }

    private static int validate(int value) {
        if (value < 0) throw new IllegalArgumentException();
        return value;
    }
}

// JDK 22+ - super() 전에 문장 허용 (Preview)
class After extends Parent {
    After(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Value must be positive");
        }
        super(value);
    }
}

// 복잡한 초기화 로직
class DatabaseConnection extends Connection {
    DatabaseConnection(String url) {
        var config = parseUrl(url);          // super 전에 실행
        validateConfig(config);               // super 전에 실행
        logger.info("Connecting to " + url);  // super 전에 실행
        super(config.host(), config.port());
    }
}
```

### Stream Gatherers (JEP 461) - Preview

중간 연산을 커스텀할 수 있는 Stream Gatherer API가 Preview로 추가되었다.

```java
// 기본 Gatherer 사용
List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

// windowFixed - 고정 크기 윈도우
List<List<Integer>> windows = numbers.stream()
    .gather(Gatherers.windowFixed(3))
    .toList();
// [[1, 2, 3], [4, 5, 6], [7, 8, 9], [10]]

// windowSliding - 슬라이딩 윈도우
List<List<Integer>> sliding = numbers.stream()
    .gather(Gatherers.windowSliding(3))
    .toList();
// [[1, 2, 3], [2, 3, 4], [3, 4, 5], ...]

// fold - 접기 연산 (reduce와 유사하지만 중간 결과 방출)
Stream<Integer> runningSum = numbers.stream()
    .gather(Gatherers.fold(() -> 0, Integer::sum));

// scan - 누적 연산
List<Integer> cumulative = numbers.stream()
    .gather(Gatherers.scan(() -> 0, Integer::sum))
    .toList();
// [1, 3, 6, 10, 15, 21, 28, 36, 45, 55]

// mapConcurrent - 병렬 매핑 (순서 유지)
List<String> results = urls.stream()
    .gather(Gatherers.mapConcurrent(4, this::fetchUrl))
    .toList();
```

### Class-File API (JEP 457) - Preview

ASM 같은 외부 라이브러리 없이 클래스 파일을 읽고 쓸 수 있다.

```java
// 클래스 파일 읽기
ClassModel cm = ClassFile.of().parse(bytes);
for (MethodModel mm : cm.methods()) {
    System.out.println(mm.methodName().stringValue());
}

// 클래스 파일 생성
byte[] bytes = ClassFile.of().build(
    ClassDesc.of("com.example.MyClass"),
    classBuilder -> classBuilder
        .withFlags(ClassFile.ACC_PUBLIC)
        .withMethod("hello",
            MethodTypeDesc.of(CD_void),
            ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC,
            methodBuilder -> methodBuilder
                .withCode(codeBuilder -> codeBuilder
                    .getstatic(CD_System, "out", CD_PrintStream)
                    .ldc("Hello, World!")
                    .invokevirtual(CD_PrintStream, "println",
                        MethodTypeDesc.of(CD_void, CD_String))
                    .return_()
                )
        )
);

// 클래스 파일 변환
ClassModel original = ClassFile.of().parse(originalBytes);
byte[] transformed = ClassFile.of().transformClass(original,
    (builder, element) -> {
        if (element instanceof MethodModel mm
            && mm.methodName().stringValue().equals("oldMethod")) {
            // 메서드 스킵 (삭제)
        } else {
            builder.accept(element);
        }
    });
```

### 기타 변경사항

```java
// Region Pinning for G1 (JEP 423)
// JNI 사용 시 전체 GC 대신 특정 영역만 고정

// Implicitly Declared Classes (JEP 463) - Preview
// main 메서드 단순화
void main() {
    System.out.println("Hello!");
}
```

---

## Java 23 (2024)

### Primitive Types in Patterns (JEP 455) - Preview

switch와 instanceof에서 primitive 타입을 직접 사용할 수 있다.

```java
// switch에서 primitive 패턴
int statusCode = getStatusCode();
String message = switch (statusCode) {
    case 200 -> "OK";
    case 404 -> "Not Found";
    case 500 -> "Internal Server Error";
    case int i when i >= 400 && i < 500 -> "Client Error: " + i;
    case int i when i >= 500 -> "Server Error: " + i;
    case int _ -> "Unknown";
};

// instanceof에서 primitive 패턴
Object value = getValue();
if (value instanceof int i && i > 0) {
    System.out.println("Positive int: " + i);
}

// Record 패턴과 결합
record Response(int code, String body) {}

void handle(Response response) {
    switch (response) {
        case Response(200, var body) -> processSuccess(body);
        case Response(int code, _) when code >= 400 -> handleError(code);
        case Response(int code, var body) -> handleOther(code, body);
    }
}

// 타입 변환 패턴
long bigValue = 10_000_000_000L;
if (bigValue instanceof int i) {
    // 손실 없이 int로 변환 가능한 경우만 매칭
    System.out.println("Fits in int: " + i);
}
```

### Markdown Documentation Comments (JEP 467) - Preview

JavaDoc 주석에 Markdown 문법을 사용할 수 있다.

```java
/// # User Service
///
/// 사용자 관련 비즈니스 로직을 처리하는 서비스.
///
/// ## 사용 예시
///
/// ```java
/// UserService service = new UserService();
/// User user = service.findById(1L);
/// ```
///
/// ## 주의사항
///
/// - 트랜잭션 범위에서 호출해야 함
/// - null 반환 가능
///
/// @param id 사용자 ID
/// @return 사용자 객체 또는 null
/// @throws IllegalArgumentException ID가 음수인 경우
public class UserService {

    /// 사용자를 ID로 조회한다.
    ///
    /// | 상태 | 반환값 |
    /// |------|--------|
    /// | 존재 | User 객체 |
    /// | 없음 | null |
    ///
    /// @param id 조회할 사용자 ID
    /// @return 사용자 객체
    public User findById(long id) {
        // ...
    }
}
```

### Flexible Constructor Bodies (JEP 482) - Second Preview

super() 전 문장 실행 기능이 2차 Preview로 계속된다.

```java
class PositiveInteger extends Number {
    private final int value;

    PositiveInteger(int value) {
        // super() 전에 유효성 검사
        if (value <= 0) {
            throw new IllegalArgumentException(
                "Value must be positive, got: " + value
            );
        }
        this.value = value;  // 필드 초기화도 super() 전에 가능
        super();
    }
}

// 복잡한 초기화
class ConfiguredService extends BaseService {
    private final Config config;

    ConfiguredService(String configPath) {
        // 설정 로드 및 검증
        var rawConfig = loadConfig(configPath);
        this.config = validateAndParse(rawConfig);

        // 로깅
        logger.info("Initializing with config: " + config);

        super(config.getServiceName());
    }
}
```

### Module Import Declarations (JEP 476) - Preview

모듈의 모든 패키지를 한 번에 import할 수 있다.

```java
// 기존 방식
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.function.Function;

// JDK 23+ (Preview)
import module java.base;  // java.base 모듈의 모든 public 타입

public class Example {
    public void demo() {
        List<String> list = List.of("a", "b", "c");
        Map<String, Integer> map = list.stream()
            .collect(Collectors.toMap(Function.identity(), String::length));
    }
}

// 여러 모듈 import
import module java.base;
import module java.sql;
import module java.logging;
```

### Implicitly Declared Classes and Instance Main Methods (JEP 477) - Third Preview

간단한 프로그램을 위한 축약 문법이 계속 발전한다.

```java
// 최소한의 Java 프로그램
void main() {
    println("Hello, World!");
}

// 인자 처리
void main(String[] args) {
    if (args.length > 0) {
        println("Hello, " + args[0] + "!");
    }
}

// 암시적 클래스 내 메서드
String greet(String name) {
    return "Hello, " + name;
}

void main() {
    println(greet("Java"));
}
```

### ZGC: Generational Mode by Default (JEP 474)

ZGC가 기본적으로 세대별 모드로 동작한다.

```bash
# JDK 23 이전 - 세대별 ZGC 명시적 활성화
java -XX:+UseZGC -XX:+ZGenerational MyApp

# JDK 23+ - 기본값이 세대별 ZGC
java -XX:+UseZGC MyApp

# 비세대별 ZGC 사용 (deprecated)
java -XX:+UseZGC -XX:-ZGenerational MyApp
```

---

## Java 24 (2025)

### Stream Gatherers (JEP 485) - 정식

Preview였던 Stream Gatherers가 정식 기능이 되었다.

```java
// 커스텀 Gatherer 구현
Gatherer<Integer, ?, Integer> distinctByMod3 = Gatherer.ofSequential(
    () -> new HashSet<Integer>(),  // 초기화
    (state, element, downstream) -> {
        int mod = element % 3;
        if (state.add(mod)) {
            downstream.push(element);
        }
        return true;
    }
);

List<Integer> result = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9)
    .gather(distinctByMod3)
    .toList();
// [1, 2, 3] - 각 mod 3 값당 첫 번째 요소만

// 복합 Gatherer
record Stats(long count, double sum, double avg) {}

Gatherer<Integer, ?, Stats> statistics = Gatherer.ofSequential(
    () -> new long[]{0, 0},  // [count, sum]
    (state, element, downstream) -> {
        state[0]++;
        state[1] += element;
        return true;
    },
    (state, downstream) -> {
        double avg = state[0] > 0 ? (double) state[1] / state[0] : 0;
        downstream.push(new Stats(state[0], state[1], avg));
    }
);
```

### Compact Object Headers (JEP 450) - Experimental

객체 헤더 크기를 줄여 메모리 효율성을 개선한다.

```bash
# 실험적 기능 활성화
java -XX:+UnlockExperimentalVMOptions -XX:+UseCompactObjectHeaders MyApp
```

| 구분 | 기존 헤더 | Compact 헤더 |
|------|----------|--------------|
| **64-bit JVM** | 12 bytes (압축 참조) / 16 bytes | 8 bytes |
| **메모리 절감** | - | 10-20% |

### Class-File API (JEP 484) - 정식

Preview였던 Class-File API가 정식 기능이 되었다.

```java
// 클래스 분석
ClassModel cm = ClassFile.of().parse(Files.readAllBytes(Path.of("MyClass.class")));

// 모든 메서드 출력
cm.methods().forEach(m -> {
    System.out.println(m.methodName() + " " + m.methodType());
});

// 모든 필드 출력
cm.fields().forEach(f -> {
    System.out.println(f.fieldName() + " " + f.fieldType());
});

// 바이트코드 분석
for (MethodModel mm : cm.methods()) {
    mm.code().ifPresent(code -> {
        for (CodeElement ce : code.elementList()) {
            if (ce instanceof Instruction inst) {
                System.out.println(inst.opcode());
            }
        }
    });
}
```

### Ahead-of-Time Class Loading & Linking (JEP 483)

애플리케이션 시작 시간을 단축하기 위한 AOT 클래스 로딩.

```bash
# 1단계: 트레이닝 실행 (클래스 로딩 기록)
java -XX:AOTMode=record -XX:AOTConfiguration=app.aotconf -jar myapp.jar

# 2단계: AOT 캐시 생성
java -XX:AOTMode=create -XX:AOTConfiguration=app.aotconf -XX:AOTCache=app.aot

# 3단계: AOT 캐시 사용
java -XX:AOTCache=app.aot -jar myapp.jar
```

### Late Barrier Expansion for G1 (JEP 475)

G1 GC의 성능이 개선된다.

### 기타 Preview/Incubator

```java
// Scoped Values (JEP 487) - Fourth Preview
final static ScopedValue<User> CURRENT_USER = ScopedValue.newInstance();

void handleRequest(User user) {
    ScopedValue.where(CURRENT_USER, user)
        .run(() -> processRequest());
}

void processRequest() {
    User user = CURRENT_USER.get();  // 상위에서 설정한 값 접근
}

// Structured Concurrency (JEP 488) - Fourth Preview
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Subtask<String> user = scope.fork(() -> fetchUser());
    Subtask<Integer> order = scope.fork(() -> fetchOrder());

    scope.join().throwIfFailed();

    return new Response(user.get(), order.get());
}
```

---

## 버전별 기능 비교표

### 주요 문법/API 도입 이력

| 기능 | 도입 버전 | 표준화 버전 | 설명 |
|------|----------|------------|------|
| Lambda Expression | 8 | 8 | 함수형 프로그래밍 지원 |
| Stream API | 8 | 8 | 컬렉션 처리 파이프라인 |
| Optional | 8 | 8 | null 안전 처리 |
| var 키워드 | 10 | 10 | 지역 변수 타입 추론 |
| Switch Expression | 12 (preview) | 14 | switch를 표현식으로 사용 |
| Text Block | 13 (preview) | 15 | 여러 줄 문자열 리터럴 |
| Record | 14 (preview) | 16 | 불변 데이터 클래스 |
| instanceof 패턴 | 14 (preview) | 16 | 타입 체크와 캐스팅 통합 |
| Sealed Class | 15 (preview) | 17 | 상속 계층 제한 |
| Record Pattern | 19 (preview) | 21 | 레코드 분해 패턴 |
| Switch 패턴 매칭 | 17 (preview) | 21 | switch에서 타입 패턴 |
| Virtual Thread | 19 (preview) | 21 | 경량 스레드 |
| Sequenced Collections | 21 | 21 | 순서 있는 컬렉션 API |
| Unnamed Variables | 21 (preview) | 22 | 사용하지 않는 변수 _ 표기 |
| FFM API | 19 (incubator) | 22 | 네이티브 코드/메모리 API |
| Stream Gatherers | 22 (preview) | 24 | 커스텀 중간 연산 |
| Class-File API | 22 (preview) | 24 | 클래스 파일 읽기/쓰기 |

### JDK 21~24 정식 기능 (GA) 추적

| 기능 | JDK 21 | JDK 22 | JDK 23 | JDK 24 |
|------|:------:|:------:|:------:|:------:|
| Virtual Threads | O | O | O | O |
| Pattern Matching for switch | O | O | O | O |
| Record Patterns | O | O | O | O |
| Sequenced Collections | O | O | O | O |
| Unnamed Variables | - | O | O | O |
| FFM API | - | O | O | O |
| Stream Gatherers | - | - | - | O |
| Class-File API | - | - | - | O |

### JDK 21~24 Preview/Incubator 기능 추적

| 기능 | JDK 21 | JDK 22 | JDK 23 | JDK 24 |
|------|:------:|:------:|:------:|:------:|
| String Templates | Preview | Preview | 철회 | - |
| Statements before super() | - | Preview | 2nd Preview | Preview |
| Primitive Patterns | - | - | Preview | Preview |
| Module Import | - | - | Preview | Preview |
| Scoped Values | Preview | 2nd Preview | 3rd Preview | 4th Preview |
| Structured Concurrency | Preview | 2nd Preview | 3rd Preview | 4th Preview |

---

## 마이그레이션 가이드

### JDK 17 -> JDK 21 마이그레이션

```java
// 1. Virtual Threads 활용
// Before: 스레드 풀 제한
ExecutorService executor = Executors.newFixedThreadPool(100);

// After: Virtual Threads
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

// 2. Pattern Matching 적용
// Before
if (obj instanceof String) {
    String s = (String) obj;
    process(s);
}

// After
if (obj instanceof String s) {
    process(s);
}

// 3. Sequenced Collections 사용
// Before
List<String> list = new ArrayList<>();
String first = list.get(0);
String last = list.get(list.size() - 1);

// After
SequencedCollection<String> list = new ArrayList<>();
String first = list.getFirst();
String last = list.getLast();
```

### JDK 21 -> JDK 24+ 마이그레이션

```java
// 1. Unnamed Variables 활용
// Before
try {
    // ...
} catch (Exception e) {
    log.error("Error occurred");
}

// After
try {
    // ...
} catch (Exception _) {
    log.error("Error occurred");
}

// 2. Stream Gatherers 활용
// Before: 별도 라이브러리 필요
List<List<Integer>> batches = Lists.partition(numbers, 10);

// After: 내장 Gatherer
List<List<Integer>> batches = numbers.stream()
    .gather(Gatherers.windowFixed(10))
    .toList();

// 3. FFM API로 네이티브 호출
// Before: JNI 또는 JNA
// After: FFM API (위 예시 참조)
```

### 주의사항

1. **String Templates 철회**: JDK 21-22의 Preview 기능이 JDK 23에서 철회됨
2. **synchronized와 Virtual Threads**: synchronized 블록에서 Virtual Thread가 pinning될 수 있음
3. **세대별 ZGC 기본값 변경**: JDK 23부터 기본 활성화

---

*마지막 업데이트: 2026년 02월*
