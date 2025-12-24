# JVM 실행 흐름과 클래스 로더 (Java 8 기준)

Java Virtual Machine(JVM)의 메모리 구조, 실행 흐름, 클래스 로딩 메커니즘, 그리고 어노테이션 프로세싱에 대한 핵심 개념을 정리합니다.

**기준 버전**: Java 8 (Java 9+ 변경사항 별도 표기)

## 목차

- [JVM 메모리 구조](#jvm-메모리-구조)
- [실행 흐름](#실행-흐름)
- [클래스 로더](#클래스-로더)
- [어노테이션 프로세서](#어노테이션-프로세서)

---

## JVM 메모리 구조

JVM은 다음과 같은 메모리 영역으로 구분됩니다:

### 메모리 영역 비교표 (Java 8)

| 영역 | 스레드 공유 | 생명주기 | 주요 저장 내용 | GC 대상 |
|------|------------|---------|--------------|---------|
| **Metaspace** | ✅ 공유 | JVM 시작~종료 | 클래스 메타데이터, static 변수, 상수 풀 | ✅ |
| **Heap** | ✅ 공유 | JVM 시작~종료 | 객체 인스턴스, 배열 | ✅ |
| **Stack** | ❌ 스레드별 | 스레드 시작~종료 | 지역 변수, 메서드 호출 정보 | ❌ |
| **PC Register** | ❌ 스레드별 | 스레드 시작~종료 | 현재 실행 중인 명령어 주소 | ❌ |
| **Native Method Stack** | ❌ 스레드별 | 스레드 시작~종료 | Native 메서드 호출 정보 | ❌ |

### 주요 특징

**1. Metaspace (Java 8의 핵심 변경사항)**

```
Java 7 이전              Java 8 이후
┌─────────────────┐      ┌─────────────────┐
│   Heap Memory   │      │   Heap Memory   │
│  ┌───────────┐  │      │  ┌───────────┐  │
│  │  PermGen  │  │  →   │  │ (PermGen  │  │
│  │ (고정크기) │  │      │  │   제거)   │  │
│  └───────────┘  │      │  └───────────┘  │
└─────────────────┘      └─────────────────┘
                         ┌─────────────────┐
                         │ Native Memory   │
                         │  ┌───────────┐  │
                         │  │Metaspace  │  │
                         │  │(동적확장)  │  │
                         │  └───────────┘  │
                         └─────────────────┘
```

**Java 8 이전 (PermGen)**
- Heap 내부에 위치
- 고정된 크기 (`-XX:MaxPermSize=128m`)
- 클래스 메타데이터, static 변수, 상수 풀 저장
- **문제점**: `OutOfMemoryError: PermGen space` 발생 빈번

**Java 8 이후 (Metaspace)**
- Native Memory에 위치 (Heap 외부)
- 동적 확장 가능 (`-XX:MaxMetaspaceSize` 기본값: 무제한)
- OS 메모리 한도까지 자동 확장
- **장점**: PermGen 공간 부족 문제 해결, 메모리 효율성 향상

**2. Heap (Java 8 기본 GC: Parallel GC)**

```
┌──────────────────────────────────────────┐
│              Heap Memory                 │
├──────────────────────┬───────────────────┤
│  Young Generation    │  Old Generation   │
│ ┌─────┬──────────┐   │                   │
│ │Eden │Survivor 0│   │   (장기 객체)      │
│ │     │Survivor 1│   │                   │
│ └─────┴──────────┘   │                   │
└──────────────────────┴───────────────────┘
```

- **Young Generation**: 새로 생성된 객체 저장 (Minor GC 대상)
- **Old Generation**: 오래 살아남은 객체 저장 (Major GC 대상)

> **Java 9+ 변경사항**: G1 GC가 기본 GC로 변경
> - Region 기반 메모리 관리
> - Young/Old 구분이 논리적으로만 존재
> - 예측 가능한 중지 시간

**3. Stack**

```
Thread-1 Stack       Thread-2 Stack
┌──────────────┐     ┌──────────────┐
│ Stack Frame  │     │ Stack Frame  │
│ ┌──────────┐ │     │ ┌──────────┐ │
│ │지역 변수  │ │     │ │지역 변수  │ │
│ │operand   │ │     │ │operand   │ │
│ │return    │ │     │ │return    │ │
│ │  addr    │ │     │ │  addr    │ │
│ └──────────┘ │     │ └──────────┘ │
└──────────────┘     └──────────────┘
```

- 메서드 호출마다 Stack Frame 생성
- 메서드 종료 시 자동 제거 (GC 불필요)
- `-Xss` 옵션으로 크기 조정 (기본: 1MB)

---

## 실행 흐름

### 전체 실행 과정 (상세)

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. 개발 단계: 소스 코드 작성                                      │
│    Example.java                                                  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. 컴파일: javac 컴파일러                                         │
│    - 소스 코드(.java) → 바이트코드(.class) 변환                   │
│    - 어노테이션 프로세서 실행 (컴파일 타임 코드 생성)              │
│    Example.class (바이트코드)                                     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. JVM 시작: java Example                                        │
│    - JVM 프로세스 생성                                            │
│    - 메모리 영역 초기화 (Heap, Metaspace, Stack 등)               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. 클래스 로딩: Class Loader Subsystem                            │
│    ┌──────────────────────────────────────────────────────┐     │
│    │ 4-1. Loading (로딩)                                   │     │
│    │      - .class 파일을 메모리로 로드                     │     │
│    │      - 위임 모델: Bootstrap → Extension → Application │     │
│    └──────────────────────────────────────────────────────┘     │
│                           ↓                                      │
│    ┌──────────────────────────────────────────────────────┐     │
│    │ 4-2. Linking (링킹)                                   │     │
│    │      - Verification: 바이트코드 검증                   │     │
│    │      - Preparation: static 변수 기본값 초기화          │     │
│    │      - Resolution: 심볼릭 참조 → 실제 주소 변환        │     │
│    └──────────────────────────────────────────────────────┘     │
│                           ↓                                      │
│    ┌──────────────────────────────────────────────────────┐     │
│    │ 4-3. Initialization (초기화)                          │     │
│    │      - static 변수 실제 값 할당                        │     │
│    │      - static 블록 실행                                │     │
│    └──────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5. 실행 엔진: Execution Engine                                    │
│    ┌──────────────────────────────────────────────────────┐     │
│    │ 바이트코드 → 기계어 변환                              │     │
│    │                                                       │     │
│    │  ┌─────────────┐          ┌──────────────┐          │     │
│    │  │ 인터프리터   │          │ JIT 컴파일러  │          │     │
│    │  │             │          │              │          │     │
│    │  │ - 한줄씩 해석│ ──────→  │ - Hot Spot   │          │     │
│    │  │ - 초기 실행  │  빈번한   │   감지       │          │     │
│    │  │             │  코드     │ - 네이티브   │          │     │
│    │  │             │  발견시   │   코드 생성  │          │     │
│    │  └─────────────┘          └──────────────┘          │     │
│    └──────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 6. 런타임 실행                                                    │
│    ┌──────────────────────────────────────────────────────┐     │
│    │ 메서드 호출                                           │     │
│    │  ↓                                                    │     │
│    │ Stack에 Frame 생성                                    │     │
│    │  - 지역 변수 할당                                      │     │
│    │  - Operand Stack (연산 스택)                          │     │
│    │  - Return Address                                     │     │
│    └──────────────────────────────────────────────────────┘     │
│                           ↓                                      │
│    ┌──────────────────────────────────────────────────────┐     │
│    │ 객체 생성 (new 키워드)                                │     │
│    │  ↓                                                    │     │
│    │ Heap의 Eden 영역에 메모리 할당                         │     │
│    │  - 생성자 실행                                         │     │
│    │  - 참조값을 Stack의 지역 변수에 저장                   │     │
│    └──────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 7. 가비지 컬렉션 (GC)                                             │
│    ┌──────────────────────────────────────────────────────┐     │
│    │ Minor GC (Young Generation)                           │     │
│    │  - Eden 영역 가득 차면 실행                            │     │
│    │  - 살아있는 객체 → Survivor로 이동                     │     │
│    │  - 일정 횟수 생존 → Old Generation으로 이동 (Promotion)│     │
│    └──────────────────────────────────────────────────────┘     │
│                           ↓                                      │
│    ┌──────────────────────────────────────────────────────┐     │
│    │ Major GC (Old Generation)                             │     │
│    │  - Old 영역 가득 차면 실행                             │     │
│    │  - STW (Stop-The-World) 발생                          │     │
│    │  - 애플리케이션 일시 중지                              │     │
│    └──────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────┘
```

### 메서드 호출 예시 (Stack Frame 생성 과정)

```java
public class Example {
    public static void main(String[] args) {  // Frame 1
        int result = calculate(5);             // Frame 2 생성 예정
        System.out.println(result);
    }

    public static int calculate(int num) {     // Frame 2
        int temp = num * 2;                    // Frame 2의 지역 변수
        return temp;
    }
}
```

**Stack 상태 변화:**

```
1. main() 호출 시
┌──────────────┐
│  Frame 1     │
│  main()      │
│ ─────────    │
│ args: [...]  │
└──────────────┘

2. calculate(5) 호출 시
┌──────────────┐
│  Frame 2     │ ← 현재 실행
│ calculate()  │
│ ─────────    │
│ num: 5       │
│ temp: 10     │
├──────────────┤
│  Frame 1     │
│  main()      │
│ ─────────    │
│ args: [...]  │
│ result: ?    │
└──────────────┘

3. calculate() 종료 후
┌──────────────┐
│  Frame 1     │
│  main()      │
│ ─────────    │
│ args: [...]  │
│ result: 10   │ ← 반환값 저장
└──────────────┘
```

### 실행 엔진 비교 (Java 8)

| 방식 | 동작 원리 | 장점 | 단점 | 사용 시점 |
|------|----------|------|------|---------|
| **인터프리터** | 바이트코드를 한 줄씩 읽어 즉시 실행 | 즉시 실행 가능, 메모리 효율적 | 느린 실행 속도 | 프로그램 초기 실행, 한 번만 실행되는 코드 |
| **JIT 컴파일러 (C1)** | Hot Spot 코드를 네이티브 코드로 컴파일 | 빠른 실행 속도 (인터프리터 대비) | 컴파일 오버헤드 | 반복 실행되는 코드 (임계값 도달) |
| **JIT 컴파일러 (C2)** | 고도로 최적화된 네이티브 코드 생성 | 매우 빠른 실행 속도 | 긴 컴파일 시간 | 매우 자주 실행되는 코드 (서버 애플리케이션) |

> **Java 9+ 변경사항**: AOT (Ahead-of-Time) 컴파일러 추가
> - 실행 전에 네이티브 코드로 미리 컴파일
> - 시작 시간 단축 (JIT 워밍업 불필요)

---

## 클래스 로더

클래스 로더는 `.class` 파일을 JVM 메모리에 로딩하는 역할을 수행합니다.

### 클래스 로더 계층 구조 (Java 8)

```
┌─────────────────────────────────────┐
│  Bootstrap ClassLoader              │
│  (Native 코드, null로 표현)         │
│  - rt.jar (java.lang.*, ...)        │
└─────────────────────────────────────┘
             ↓ 부모
┌─────────────────────────────────────┐
│  Extension ClassLoader              │
│  (sun.misc.Launcher$ExtClassLoader) │
│  - jre/lib/ext/*.jar                │
└─────────────────────────────────────┘
             ↓ 부모
┌─────────────────────────────────────┐
│  Application ClassLoader            │
│  (sun.misc.Launcher$AppClassLoader) │
│  - classpath의 애플리케이션 클래스   │
└─────────────────────────────────────┘
             ↓ 부모 (optional)
┌─────────────────────────────────────┐
│  Custom ClassLoader (사용자 정의)   │
│  - 특수 로딩 로직 (암호화, 네트워크) │
└─────────────────────────────────────┘
```

> **Java 9+ 변경사항**: 모듈 시스템 도입으로 클래스 로더 구조 변경
> - Extension ClassLoader → **Platform ClassLoader**
> - rt.jar 제거, 모듈로 재구성 (java.base 등)
> - 각 모듈이 자체 ClassLoader 보유

### 클래스 로더 종류 (Java 8)

| 클래스 로더 | 로딩 대상 | 구현 언어 | 확인 방법 |
|-----------|---------|---------|----------|
| **Bootstrap** | JDK 핵심 라이브러리<br>`rt.jar` (java.lang.*, java.util.*) | Native 코드 (C/C++) | `String.class.getClassLoader()` → null |
| **Extension** | 확장 라이브러리<br>`jre/lib/ext/*.jar` | Java | `DNSNameService.class.getClassLoader()` |
| **Application** | 애플리케이션 클래스<br>`classpath`의 모든 클래스 | Java | `MyClass.class.getClassLoader()` |
| **Custom** | 사용자 정의 로딩 로직 | Java | 직접 구현 (`extends ClassLoader`) |

### 클래스 로딩 과정 (3단계)

```
┌──────────────────────────────────────────────────────┐
│ 1. Loading (로딩)                                     │
│    - 클래스 로더가 .class 파일을 찾아 바이트로 읽음    │
│    - Method Area(Metaspace)에 클래스 정보 저장        │
│    - Heap에 Class 객체 생성 (java.lang.Class)         │
│                                                       │
│    위임 모델 (Delegation Model):                      │
│    ┌───────────────────────────────────────┐         │
│    │ 1. Application CL이 요청 받음         │         │
│    │ 2. Extension CL에 위임                │         │
│    │ 3. Bootstrap CL에 위임                │         │
│    │ 4. Bootstrap이 로드 실패 → Ext 시도   │         │
│    │ 5. Extension도 실패 → App 시도        │         │
│    │ 6. 모두 실패 → ClassNotFoundException │         │
│    └───────────────────────────────────────┘         │
└──────────────────────────────────────────────────────┘
                       ↓
┌──────────────────────────────────────────────────────┐
│ 2. Linking (링킹)                                     │
│    ┌──────────────────────────────────────┐          │
│    │ 2-1. Verification (검증)             │          │
│    │      - 바이트코드 형식 검사           │          │
│    │      - 타입 안전성 검사               │          │
│    │      - final 클래스 상속 여부 확인    │          │
│    └──────────────────────────────────────┘          │
│                    ↓                                  │
│    ┌──────────────────────────────────────┐          │
│    │ 2-2. Preparation (준비)              │          │
│    │      - static 변수 메모리 할당        │          │
│    │      - 기본값으로 초기화              │          │
│    │        int → 0, boolean → false, ... │          │
│    └──────────────────────────────────────┘          │
│                    ↓                                  │
│    ┌──────────────────────────────────────┐          │
│    │ 2-3. Resolution (해석) - Optional    │          │
│    │      - 심볼릭 참조→실제 메모리 주소   │          │
│    │      - 클래스, 필드, 메서드 참조 확정 │          │
│    │      - Lazy Resolution (필요시 수행)  │          │
│    └──────────────────────────────────────┘          │
└──────────────────────────────────────────────────────┘
                       ↓
┌──────────────────────────────────────────────────────┐
│ 3. Initialization (초기화)                            │
│    - static 변수에 실제 값 할당                        │
│    - static 블록 실행 (위에서 아래로 순차 실행)        │
│    - <clinit> 메서드 호출                             │
│    - Thread-safe 보장 (동시 초기화 방지)              │
└──────────────────────────────────────────────────────┘
```

### 클래스 로딩 예시 코드

```java
public class Example {
    static int count = 10;       // Preparation: 0 → Initialization: 10
    static String name;          // Preparation: null (그대로 유지)

    static {
        System.out.println("Static block 1");  // Initialization 시 실행
        count = 20;
    }

    static {
        System.out.println("Static block 2");  // 순차 실행
        name = "Example";
    }

    public static void main(String[] args) {
        System.out.println(count);  // 출력: 20
        System.out.println(name);   // 출력: Example
    }
}

// 실행 결과:
// Static block 1
// Static block 2
// 20
// Example
```

**클래스 로더 확인 코드:**

```java
public class ClassLoaderTest {
    public static void main(String[] args) {
        // Bootstrap ClassLoader (null 반환)
        System.out.println("String: " + String.class.getClassLoader());
        // 출력: String: null

        // Extension ClassLoader (Java 8)
        System.out.println("DNSNameService: " +
            com.sun.jndi.dns.DnsClient.class.getClassLoader());
        // 출력: sun.misc.Launcher$ExtClassLoader@...

        // Application ClassLoader
        System.out.println("MyClass: " + ClassLoaderTest.class.getClassLoader());
        // 출력: sun.misc.Launcher$AppClassLoader@...

        // 부모 확인
        ClassLoader appCL = ClassLoaderTest.class.getClassLoader();
        System.out.println("Parent: " + appCL.getParent());
        // 출력: sun.misc.Launcher$ExtClassLoader@...
        System.out.println("Parent's Parent: " + appCL.getParent().getParent());
        // 출력: null (Bootstrap)
    }
}
```

---

## 어노테이션 프로세서

어노테이션 프로세서는 **컴파일 타임**에 어노테이션을 분석하고 코드를 생성하거나 검증하는 도구입니다.

### 처리 시점 비교

| 시점 | 도구 | 예시 | 특징 | 성능 영향 |
|------|------|------|------|---------|
| **컴파일 타임** | Annotation Processor | Lombok, QueryDSL, AutoValue | 소스 코드 생성, 컴파일 오류 검출 | 런타임 오버헤드 없음 |
| **런타임** | Reflection API | Spring AOP, JPA, Jackson | 동적 프록시, 메타데이터 읽기 | 리플렉션 오버헤드 있음 |

### 주요 사용 사례

**1. 코드 생성 (Boilerplate 제거)**

```java
// Lombok 사용 전 (Java 8 기준)
public class User {
    private Long id;
    private String name;
    private String email;

    public User() {}

    public User(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) &&
               Objects.equals(name, user.name) &&
               Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, email);
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', email='" + email + "'}";
    }
}
```

```java
// Lombok 사용 후 (컴파일 타임에 위 코드가 자동 생성됨)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class User {
    private Long id;
    private String name;
    private String email;
}
```

**2. QueryDSL - 타입 안전 쿼리 생성**

```java
// JPA 엔티티
@Entity
public class Product {
    @Id private Long id;
    private String name;
    private int price;
}

// 컴파일 타임에 QProduct.java 자동 생성
// 생성된 코드 (일부):
public class QProduct extends EntityPathBase<Product> {
    public final NumberPath<Long> id = createNumber("id", Long.class);
    public final StringPath name = createString("name");
    public final NumberPath<Integer> price = createNumber("price", Integer.class);
}

// 타입 안전 쿼리 사용
QProduct product = QProduct.product;
List<Product> result = queryFactory
    .selectFrom(product)
    .where(product.price.gt(10000))
    .fetch();
```

**3. 컴파일 타임 검증**

```java
// AutoValue - 불변 객체 생성 및 검증
@AutoValue
public abstract class Money {
    public abstract String currency();
    public abstract long amount();

    public static Money create(String currency, long amount) {
        if (amount < 0) {
            // 컴파일 타임에 오류 검출
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        return new AutoValue_Money(currency, amount);
    }
}
```

### 동작 원리 (Java 8 Annotation Processing API)

```
┌───────────────────────────────────────────────────┐
│ 1. 소스 코드 작성                                  │
│    User.java (@Getter @Setter 어노테이션 포함)    │
└───────────────────────────────────────────────────┘
                     ↓
┌───────────────────────────────────────────────────┐
│ 2. javac 컴파일러 실행                             │
│    javac -processor LombokProcessor User.java    │
└───────────────────────────────────────────────────┘
                     ↓
┌───────────────────────────────────────────────────┐
│ 3. Annotation Processor 초기화                    │
│    - javax.annotation.processing.Processor 로드   │
│    - META-INF/services에 등록된 프로세서 탐색      │
└───────────────────────────────────────────────────┘
                     ↓
┌───────────────────────────────────────────────────┐
│ 4. AST (Abstract Syntax Tree) 생성                │
│    - 소스 코드를 트리 구조로 파싱                  │
│    - 컴파일러 내부 표현으로 변환                   │
└───────────────────────────────────────────────────┘
                     ↓
┌───────────────────────────────────────────────────┐
│ 5. Annotation Processing Rounds                   │
│    ┌───────────────────────────────────┐          │
│    │ Round 1:                          │          │
│    │  - @Getter, @Setter 어노테이션 발견│          │
│    │  - getter/setter 메서드 코드 생성  │          │
│    │  - 새로운 소스 파일/바이트코드 생성│          │
│    └───────────────────────────────────┘          │
│                   ↓                                │
│    ┌───────────────────────────────────┐          │
│    │ Round 2:                          │          │
│    │  - 새로 생성된 파일 검사           │          │
│    │  - 추가 어노테이션 없으면 종료     │          │
│    └───────────────────────────────────┘          │
└───────────────────────────────────────────────────┘
                     ↓
┌───────────────────────────────────────────────────┐
│ 6. 최종 컴파일                                     │
│    - 생성된 코드 포함하여 .class 파일 생성         │
│    User.class (getter/setter 메서드 포함)         │
└───────────────────────────────────────────────────┘
```

### Annotation Processor vs Reflection

```java
// Annotation Processor (컴파일 타임)
@Retention(RetentionPolicy.SOURCE)  // .class 파일에 포함되지 않음
public @interface Getter {}

// 컴파일 후:
// - Getter 어노테이션은 제거됨
// - 생성된 getter 메서드만 .class에 존재
// - 런타임 오버헤드 없음
```

```java
// Reflection (런타임)
@Retention(RetentionPolicy.RUNTIME)  // .class 파일에 포함
public @interface Entity {}

// 런타임에:
if (User.class.isAnnotationPresent(Entity.class)) {
    // 리플렉션으로 메타데이터 읽기
    Entity entity = User.class.getAnnotation(Entity.class);
    // 동적 처리 (프록시 생성, 테이블 매핑 등)
}
// - 런타임 오버헤드 발생
// - 동적 처리 가능
```

**비교 표:**

| 특성 | Annotation Processor | Reflection |
|------|---------------------|------------|
| **실행 시점** | 컴파일 타임 | 런타임 |
| **Retention** | SOURCE, CLASS | RUNTIME |
| **성능** | 런타임 오버헤드 없음 | 리플렉션 비용 발생 |
| **타입 안전성** | 컴파일 타임 검증 | 런타임 오류 가능 |
| **코드 생성** | 가능 | 불가능 (프록시만 가능) |
| **사용 예** | Lombok, QueryDSL | Spring, JPA, Jackson |

---

## Java 버전별 주요 변경사항 요약

### Java 8 → Java 9+

| 영역 | Java 8 | Java 9+ | 변경 이유 |
|------|--------|---------|----------|
| **모듈 시스템** | 없음 (monolithic JDK) | JPMS (Java Platform Module System) | 캡슐화 강화, 경량화 |
| **Class Loader** | Bootstrap / Extension / Application | Bootstrap / Platform / Application | 모듈 시스템 지원 |
| **rt.jar** | 단일 거대 jar | 모듈로 분리 (java.base 등) | 필요한 모듈만 로드 |
| **기본 GC** | Parallel GC | G1 GC | 예측 가능한 중지 시간 |
| **컴파일러** | JIT (C1, C2) | JIT + AOT | 시작 시간 단축 |
| **메모리** | Metaspace | Metaspace (동일) | - |

### Java 11+ 추가 변경사항

- **Epsilon GC**: No-Op GC (메모리 할당만, 회수 없음)
- **ZGC**: 초저지연 GC (10ms 이하 중지 시간)
- **Flight Recorder**: JVM 프로파일링 도구 오픈소스화

---

## 참고 자료

- [Oracle Java 8 JVM Specification](https://docs.oracle.com/javase/specs/jvms/se8/html/)
- [Java Class Loading Mechanism](https://docs.oracle.com/javase/8/docs/technotes/tools/findingclasses.html)
- [Annotation Processing in Java](https://docs.oracle.com/javase/8/docs/api/javax/annotation/processing/package-summary.html)
- [From PermGen to Metaspace](https://www.baeldung.com/java-permgen-metaspace)

---

*마지막 업데이트: 2025년 12월*
