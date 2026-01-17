# Java 버전별 주요 변경점

Java 8부터 21까지 LTS 버전을 중심으로 새로 추가된 주요 문법과 기능을 정리합니다.

## 목차

- [Java 8 (2014)](#java-8-2014)
- [Java 11 (2018, LTS)](#java-11-2018-lts)
- [Java 14 (2020)](#java-14-2020)
- [Java 15 (2020)](#java-15-2020)
- [Java 16 (2021)](#java-16-2021)
- [Java 17 (2021, LTS)](#java-17-2021-lts)
- [Java 21 (2023, LTS)](#java-21-2023-lts)
- [버전별 기능 비교표](#버전별-기능-비교표)

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

### Record Pattern

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
        case Point(int x, int y) -> "Point at (" + x + ", " + y + ")";
        case Circle(Point p, int r) -> "Circle with radius " + r;
        default -> "Unknown shape";
    };
}
```

### Switch Pattern Matching

```java
// 타입 패턴
static String format(Object obj) {
    return switch (obj) {
        case Integer i -> "Integer: " + i;
        case Long l -> "Long: " + l;
        case Double d -> "Double: " + d;
        case String s -> "String: " + s;
        case null -> "null";
        default -> "Unknown";
    };
}

// 가드 조건 (when)
static String describeNumber(Integer i) {
    return switch (i) {
        case null -> "null";
        case Integer n when n < 0 -> "Negative";
        case Integer n when n == 0 -> "Zero";
        case Integer n when n > 0 -> "Positive";
        default -> "Unknown";  // 도달 불가능하지만 컴파일러를 위해
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

### Virtual Thread (Project Loom)

```java
// 가상 스레드 생성
Thread vThread = Thread.ofVirtual().start(() -> {
    System.out.println("Running in virtual thread");
});

// ExecutorService 사용
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    IntStream.range(0, 10_000).forEach(i -> {
        executor.submit(() -> {
            Thread.sleep(Duration.ofSeconds(1));
            return i;
        });
    });
}

// 기존 플랫폼 스레드
Thread pThread = Thread.ofPlatform().start(() -> {
    System.out.println("Running in platform thread");
});
```

### Sequenced Collections

```java
// SequencedCollection 인터페이스
SequencedCollection<String> list = new ArrayList<>();
list.addFirst("first");
list.addLast("last");
String first = list.getFirst();
String last = list.getLast();
list.removeFirst();
list.removeLast();

// 역순 뷰
SequencedCollection<String> reversed = list.reversed();

// SequencedMap
SequencedMap<String, Integer> map = new LinkedHashMap<>();
map.putFirst("a", 1);
map.putLast("z", 26);
Map.Entry<String, Integer> firstEntry = map.firstEntry();
Map.Entry<String, Integer> lastEntry = map.lastEntry();
```

---

## 버전별 기능 비교표

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

---

*마지막 업데이트: 2026년 01월*
