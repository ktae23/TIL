# JVM 클래스 로더와 모듈 시스템

JVM의 클래스 로딩 메커니즘, 모듈 시스템, 어노테이션 프로세싱에 대한 상세 분석입니다.

**기준 버전**: Java 8 → Java 9+ 모듈 시스템 변경사항 포함

## 목차

- [클래스 로더](#클래스-로더)
  - [Java 8: JAR 기반](#java-8-jar-기반-클래스-로딩)
  - [Java 9+: 모듈 기반](#java-9-모듈-시스템-jpms-기반-클래스-로딩)
- [클래스 로딩 과정](#클래스-로딩-과정-3단계)
- [어노테이션 프로세서](#어노테이션-프로세서)
- [참고 자료](#참고-자료)

---

## 클래스 로더

클래스 로더는 `.class` 파일을 JVM 메모리에 로딩하는 역할을 수행합니다.

### 클래스 로더 계층 구조

#### Java 8: JAR 기반 클래스 로딩

```
┌─────────────────────────────────────┐
│  Bootstrap ClassLoader              │
│  (Native 코드, null로 표현)         │
│  - rt.jar (java.lang.*, ...)        │
│  - 핵심 JDK 클래스 로딩              │
└─────────────────────────────────────┘
             ↓ 부모
┌─────────────────────────────────────┐
│  Extension ClassLoader              │
│  (sun.misc.Launcher$ExtClassLoader) │
│  - jre/lib/ext/*.jar                │
│  - 확장 라이브러리 로딩              │
└─────────────────────────────────────┘
             ↓ 부모
┌─────────────────────────────────────┐
│  Application ClassLoader            │
│  (sun.misc.Launcher$AppClassLoader) │
│  - classpath의 애플리케이션 클래스   │
│  - 사용자 작성 클래스 로딩           │
└─────────────────────────────────────┘
             ↓ 부모 (optional)
┌─────────────────────────────────────┐
│  Custom ClassLoader (사용자 정의)   │
│  - 특수 로딩 로직 (암호화, 네트워크) │
└─────────────────────────────────────┘
```

**Java 8 클래스 로더 특징:**
- **단일 거대 JAR**: rt.jar에 모든 핵심 클래스 포함 (60MB+)
- **전역 classpath**: 모든 클래스가 동일한 classpath 공유
- **캡슐화 부족**: internal API 접근 가능 (`sun.*`, `com.sun.*`)
- **의존성 충돌**: JAR Hell 문제 (같은 클래스 다른 버전)

#### Java 9+: 모듈 시스템 (JPMS) 기반 클래스 로딩

```
┌─────────────────────────────────────────────────────────┐
│  Bootstrap ClassLoader (BootLoader)                     │
│  (Native 코드, null로 표현)                             │
│  ┌─────────────────────────────────────────────────┐   │
│  │ 핵심 모듈 (rt.jar 제거, 모듈로 분리)             │   │
│  │  - java.base (필수, 자동 로드)                   │   │
│  │  - java.logging, java.xml, java.prefs           │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
             ↓ 부모
┌─────────────────────────────────────────────────────────┐
│  Platform ClassLoader (PlatformClassLoader)             │
│  (jdk.internal.loader.ClassLoaders$PlatformClassLoader) │
│  ┌─────────────────────────────────────────────────┐   │
│  │ Java SE 플랫폼 모듈 (Extension 대체)             │   │
│  │  - java.se (Java SE API)                        │   │
│  │  - java.sql, java.naming, java.management       │   │
│  │  - java.desktop, java.compiler                  │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
             ↓ 부모
┌─────────────────────────────────────────────────────────┐
│  Application ClassLoader (AppClassLoader)               │
│  (jdk.internal.loader.ClassLoaders$AppClassLoader)      │
│  ┌─────────────────────────────────────────────────┐   │
│  │ 애플리케이션 모듈 + classpath                    │   │
│  │  - 사용자 정의 모듈 (module-path)                │   │
│  │  - classpath의 클래스 (하위 호환)                │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
             ↓ 부모 (optional)
┌─────────────────────────────────────────────────────────┐
│  Custom ClassLoader (사용자 정의)                       │
│  - 동일 (모듈 시스템과 호환)                             │
└─────────────────────────────────────────────────────────┘
```

**Java 9+ 클래스 로더 변경사항:**

1. **Extension ClassLoader → Platform ClassLoader**
   - 이름 변경: `ExtClassLoader` → `PlatformClassLoader`
   - 역할 확대: Java SE 플랫폼 모듈 로딩
   - `jre/lib/ext` 디렉토리 제거

2. **rt.jar 제거, 모듈로 분리**
   - 단일 거대 JAR 제거
   - 90개 이상의 모듈로 분리 (`jmods/` 디렉토리)
   - 필요한 모듈만 선택적 로드 (경량화)

3. **모듈 경로 (module-path) 도입**
   - classpath와 별도로 module-path 존재
   - `--module-path` 또는 `-p` 옵션으로 지정

4. **강력한 캡슐화**
   - internal API 접근 차단 (`sun.*`, `jdk.internal.*`)
   - `--add-exports`, `--add-opens`로만 접근 가능

**Java 8 vs Java 9+ 클래스 로더 비교:**

| 항목 | Java 8 | Java 9+ | 변경 이유 |
|------|--------|---------|----------|
| **핵심 클래스 로더** | Bootstrap / Extension / Application | Bootstrap / Platform / Application | 모듈 시스템 지원 |
| **Extension CL 이름** | `sun.misc.Launcher$ExtClassLoader` | `jdk.internal.loader.ClassLoaders$PlatformClassLoader` | 역할 변경 (확장→플랫폼) |
| **Application CL 이름** | `sun.misc.Launcher$AppClassLoader` | `jdk.internal.loader.ClassLoaders$AppClassLoader` | 모듈 경로 지원 |
| **핵심 라이브러리** | rt.jar (단일 거대 JAR) | 모듈로 분리 (java.base 등) | 경량화, 선택적 로드 |
| **확장 라이브러리** | jre/lib/ext/*.jar | Platform 모듈 (java.se 등) | 모듈 시스템 통합 |
| **클래스 경로** | classpath만 존재 | classpath + module-path | 모듈과 JAR 공존 |
| **캡슐화** | 약함 (internal API 접근 가능) | 강함 (모듈 경계 강제) | 보안, API 안정성 |
| **로딩 방식** | JAR 전체 스캔 | 모듈 디스크립터 기반 | 성능 향상 |

**모듈 시스템 예시:**

```java
// module-info.java (모듈 디스크립터)
module com.example.myapp {
    requires java.base;      // 자동 포함 (명시 불필요)
    requires java.sql;       // Platform CL이 로드
    requires java.logging;   // Platform CL이 로드

    exports com.example.api; // 외부에 공개
    // com.example.internal은 캡슐화 (외부 접근 불가)
}
```

**클래스 로더 확인 코드 (Java 9+):**

```java
public class ModuleClassLoaderTest {
    public static void main(String[] args) {
        // Bootstrap ClassLoader (여전히 null)
        System.out.println("String: " + String.class.getClassLoader());
        // 출력: null

        // Platform ClassLoader (java.sql 모듈)
        System.out.println("java.sql.Connection: " +
            java.sql.Connection.class.getClassLoader());
        // 출력: jdk.internal.loader.ClassLoaders$PlatformClassLoader@...

        // Application ClassLoader
        System.out.println("MyClass: " + ModuleClassLoaderTest.class.getClassLoader());
        // 출력: jdk.internal.loader.ClassLoaders$AppClassLoader@...

        // 모듈 정보 확인
        Module stringModule = String.class.getModule();
        System.out.println("String 모듈: " + stringModule.getName());
        // 출력: java.base

        System.out.println("java.base는 모든 모듈에 자동 포함: " +
            stringModule.getDescriptor().requires());
    }
}
```

**주요 모듈 목록:**

```bash
# Java 9+ 모듈 확인
java --list-modules

# 핵심 모듈 (Bootstrap CL)
java.base                  # 필수 모듈 (자동 로드)
java.logging
java.xml
java.prefs

# 플랫폼 모듈 (Platform CL)
java.se                    # Java SE 전체 API
java.sql
java.naming
java.management
java.desktop
java.compiler

# JDK 전용 모듈
jdk.compiler               # javac 컴파일러
jdk.jshell                 # JShell REPL
jdk.httpserver             # 간단한 HTTP 서버
```

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

---

## 참고 자료

- [Java Class Loading Mechanism](https://docs.oracle.com/javase/8/docs/technotes/tools/findingclasses.html)
- [Java Platform Module System](https://openjdk.org/projects/jigsaw/)
- [Annotation Processing in Java](https://docs.oracle.com/javase/8/docs/api/javax/annotation/processing/package-summary.html)

---

*마지막 업데이트: 2025년 12월*
