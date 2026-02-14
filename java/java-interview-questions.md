# Java 면접 핵심 질문 정리

5년차 백엔드 개발자 면접에서 자주 등장하는 Java 핵심 질문과 답변을 정리합니다.

## 목차

1. [String 불변성](#1-string-불변성)
2. [equals()와 hashCode()](#2-equals와-hashcode)
3. [제네릭 (Generics)](#3-제네릭-generics)
4. [JVM 메모리 구조](#4-jvm-메모리-구조)
5. [가비지 컬렉션](#5-가비지-컬렉션)
6. [동시성 기초](#6-동시성-기초)

---

## 1. String 불변성

### Q: String이 불변(Immutable)인 이유와 장점은?

**불변인 이유**
```java
public final class String {
    private final char[] value;  // Java 8 이하
    private final byte[] value;  // Java 9 이상

    // setter 없음, 모든 조작 메서드는 새 객체 반환
}
```

**불변성의 장점**

1. **String Pool 캐싱 가능**
```java
String s1 = "hello";
String s2 = "hello";
System.out.println(s1 == s2);  // true (같은 객체 참조)

// Pool 우회
String s3 = new String("hello");
System.out.println(s1 == s3);  // false
System.out.println(s1.equals(s3));  // true
```

2. **Thread-Safe**
```java
// 불변이므로 동기화 없이 안전하게 공유 가능
public class UserService {
    private final String baseUrl = "https://api.example.com";  // 안전
}
```

3. **해시 코드 캐싱**
```java
// String.hashCode() 구현
private int hash;  // 캐시된 해시값

public int hashCode() {
    int h = hash;
    if (h == 0 && value.length > 0) {
        hash = h = /* 계산 */;  // 한 번만 계산
    }
    return h;
}
```

4. **보안성**
```java
// DB 연결, 네트워크 연결 등에서 파라미터 조작 방지
void connect(String url) {
    // url이 중간에 변경될 수 없음
}
```

### Q: String, StringBuilder, StringBuffer의 차이점은?

| 구분 | String | StringBuilder | StringBuffer |
|------|--------|---------------|--------------|
| 불변성 | 불변 | 가변 | 가변 |
| Thread-Safe | O | X | O (synchronized) |
| 성능 | 느림 (연결 시) | 빠름 | 중간 |
| 사용 시점 | 변경 적을 때 | 단일 스레드 | 멀티 스레드 |

```java
// 성능 비교
// Bad: O(n²)
String result = "";
for (int i = 0; i < 10000; i++) {
    result += i;  // 매번 새 String 객체 생성
}

// Good: O(n)
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 10000; i++) {
    sb.append(i);
}
String result = sb.toString();
```

---

## 2. equals()와 hashCode()

### Q: equals()와 hashCode()를 함께 재정의해야 하는 이유는?

**계약 (Contract)**
- `equals()`가 true인 두 객체는 반드시 같은 `hashCode()` 반환
- `hashCode()`가 같아도 `equals()`는 false일 수 있음

**HashMap 동작 원리**
```java
// HashMap.put() 간략화
public V put(K key, V value) {
    int hash = hash(key.hashCode());  // 1. 해시값으로 버킷 결정
    int index = hash & (table.length - 1);

    for (Node node : table[index]) {
        if (node.hash == hash &&
            (node.key == key || key.equals(node.key))) {  // 2. equals로 비교
            // 기존 값 교체
        }
    }
    // 새 노드 추가
}
```

**잘못된 구현 예시**
```java
public class User {
    private Long id;
    private String name;

    // equals만 재정의, hashCode 미구현
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    // hashCode를 재정의하지 않으면 Object.hashCode() 사용
    // → 객체 주소 기반이므로 같은 id여도 다른 해시값
}

// 문제 발생
User user1 = new User(1L, "Kim");
User user2 = new User(1L, "Kim");

Set<User> set = new HashSet<>();
set.add(user1);
set.add(user2);
System.out.println(set.size());  // 2 (1이어야 함!)
```

**올바른 구현**
```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof User)) return false;
    User user = (User) o;
    return Objects.equals(id, user.id);
}

@Override
public int hashCode() {
    return Objects.hash(id);  // equals에 사용된 필드와 동일
}

// 또는 Lombok 사용
@EqualsAndHashCode(of = "id")
public class User { }
```

---

## 3. 제네릭 (Generics)

### Q: 제네릭의 타입 소거(Type Erasure)란?

**타입 소거**: 컴파일 시 제네릭 타입 정보가 제거됨

```java
// 컴파일 전
List<String> strings = new ArrayList<>();
strings.add("hello");
String s = strings.get(0);

// 컴파일 후 (바이트코드)
List strings = new ArrayList();
strings.add("hello");
String s = (String) strings.get(0);  // 캐스팅 추가
```

**한계점**
```java
// 1. 런타임에 타입 정보 없음
if (list instanceof List<String>) { }  // 컴파일 에러

// 2. 제네릭 배열 생성 불가
T[] array = new T[10];  // 컴파일 에러

// 3. 원시 타입 사용 불가
List<int> numbers;  // 컴파일 에러
List<Integer> numbers;  // OK

// 4. static 멤버에 타입 파라미터 사용 불가
class Box<T> {
    static T value;  // 컴파일 에러
}
```

### Q: PECS (Producer Extends, Consumer Super)를 설명해주세요.

**공변성 (Covariance) - Producer Extends**
```java
// 데이터를 "읽기만" 할 때
public void processAnimals(List<? extends Animal> animals) {
    for (Animal animal : animals) {
        animal.eat();  // 읽기 OK
    }
    // animals.add(new Dog());  // 컴파일 에러! 쓰기 불가
}

List<Dog> dogs = new ArrayList<>();
processAnimals(dogs);  // OK
```

**반공변성 (Contravariance) - Consumer Super**
```java
// 데이터를 "쓰기만" 할 때
public void addDogs(List<? super Dog> dogs) {
    dogs.add(new Dog());  // 쓰기 OK
    dogs.add(new Puppy());  // OK (Puppy extends Dog)

    // Dog dog = dogs.get(0);  // 컴파일 에러! 읽기 시 타입 불확실
    Object obj = dogs.get(0);  // Object로만 읽기 가능
}

List<Animal> animals = new ArrayList<>();
addDogs(animals);  // OK
```

**실제 사용 예시**
```java
// Collections.copy 시그니처
public static <T> void copy(List<? super T> dest, List<? extends T> src) {
    for (int i = 0; i < src.size(); i++) {
        dest.set(i, src.get(i));  // src에서 읽어서 dest에 쓰기
    }
}
```

---

## 4. JVM 메모리 구조

### Q: JVM 메모리 영역을 설명해주세요.

```
┌─────────────────────────────────────────────────────┐
│                    JVM Memory                        │
├─────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────┐   │
│  │              Method Area (메서드 영역)        │   │
│  │   - 클래스 메타데이터                         │   │
│  │   - static 변수                              │   │
│  │   - 상수 풀 (Runtime Constant Pool)          │   │
│  └──────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────┐   │
│  │                 Heap (힙)                     │   │
│  │   - 객체 인스턴스                            │   │
│  │   - 배열                                     │   │
│  │   - GC 대상                                  │   │
│  └──────────────────────────────────────────────┘   │
│  ┌───────────┐ ┌───────────┐ ┌───────────┐        │
│  │  Stack 1  │ │  Stack 2  │ │  Stack 3  │ (스레드별)│
│  │  - 지역변수 │ │  - 지역변수 │ │  - 지역변수 │        │
│  │  - 메서드  │ │  - 메서드  │ │  - 메서드  │        │
│  │    프레임  │ │    프레임  │ │    프레임  │        │
│  └───────────┘ └───────────┘ └───────────┘        │
│  ┌───────────┐ ┌───────────┐ ┌───────────┐        │
│  │   PC 1    │ │   PC 2    │ │   PC 3    │ (스레드별)│
│  └───────────┘ └───────────┘ └───────────┘        │
│  ┌───────────────────────────────────────────────┐  │
│  │            Native Method Stack                │  │
│  └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

**영역별 특징**

| 영역 | 공유 범위 | 저장 내용 | GC 대상 |
|------|----------|----------|---------|
| Method Area | 모든 스레드 | 클래스 정보, static | △ (Metaspace) |
| Heap | 모든 스레드 | 객체 인스턴스 | O |
| Stack | 스레드별 | 지역변수, 메서드 호출 | X |
| PC Register | 스레드별 | 현재 실행 주소 | X |

### Q: 스택 프레임(Stack Frame)에 대해 설명해주세요.

```java
public class StackExample {
    public static void main(String[] args) {
        int a = 10;           // main 프레임의 지역 변수
        int result = add(a, 20);  // add 프레임 생성
    }

    public static int add(int x, int y) {
        int sum = x + y;      // add 프레임의 지역 변수
        return sum;
    }
}
```

```
[Stack]
┌──────────────────────┐
│   add() 프레임        │ ← 현재
│   - x: 10            │
│   - y: 20            │
│   - sum: 30          │
│   - return address   │
├──────────────────────┤
│   main() 프레임       │
│   - args: ref        │
│   - a: 10            │
│   - result: ?        │
└──────────────────────┘
```

---

## 5. 가비지 컬렉션

### Q: GC의 동작 원리와 종류를 설명해주세요.

**Heap 구조 (Generational GC)**
```
┌─────────────────────────────────────────────────────┐
│                      Heap                            │
├─────────────────────┬───────────────────────────────┤
│    Young Generation │        Old Generation         │
├──────┬──────┬───────┤                               │
│ Eden │ S0   │ S1    │         Tenured               │
│      │(From)│(To)   │                               │
└──────┴──────┴───────┴───────────────────────────────┘
        ↑              Minor GC                Major/Full GC
        새 객체 할당
```

**GC 동작 과정**
```
1. 새 객체는 Eden 영역에 할당
2. Eden이 가득 차면 Minor GC 발생
3. 살아남은 객체는 Survivor 영역으로 이동 (age +1)
4. age가 임계값 초과 시 Old Generation으로 이동 (Promotion)
5. Old Generation이 가득 차면 Major GC 발생
```

**GC 종류**

| GC | 특징 | 사용 시점 |
|----|------|----------|
| Serial GC | 단일 스레드, STW 길음 | 작은 애플리케이션 |
| Parallel GC | 멀티 스레드 Minor GC | Java 8 기본 |
| CMS GC | Concurrent Mark Sweep | 낮은 지연 (Deprecated) |
| G1 GC | Region 기반, 예측 가능 | Java 9+ 기본 |
| ZGC | 초저지연 (<10ms) | 대용량 힙 |
| Shenandoah | 동시 압축 | 낮은 지연 |

```bash
# GC 설정 예시
java -XX:+UseG1GC -Xms4g -Xmx4g -XX:MaxGCPauseMillis=200 MyApp
```

### Q: Stop-The-World(STW)란?

**정의**: GC 수행 시 모든 애플리케이션 스레드가 정지하는 현상

```java
// STW 발생 지점
1. Minor GC 시 (짧음)
2. Major/Full GC 시 (길 수 있음)
3. Young Gen → Old Gen 이동 시

// 모니터링
-XX:+PrintGCDetails
-Xlog:gc*  // Java 9+
```

---

## 6. 동시성 기초

### Q: synchronized 키워드의 동작 방식은?

**사용 방법**
```java
public class Counter {
    private int count = 0;

    // 1. 메서드 레벨 (this를 모니터로 사용)
    public synchronized void increment() {
        count++;
    }

    // 2. 블록 레벨
    public void incrementBlock() {
        synchronized (this) {
            count++;
        }
    }

    // 3. static 메서드 (클래스 객체를 모니터로 사용)
    private static int staticCount = 0;
    public static synchronized void staticIncrement() {
        staticCount++;
    }

    // 4. 별도 객체를 락으로 사용 (권장)
    private final Object lock = new Object();
    public void incrementWithLock() {
        synchronized (lock) {
            count++;
        }
    }
}
```

**모니터 락 동작**
```
┌─────────────────────────────────────┐
│            Object Header            │
├─────────────────────────────────────┤
│   Mark Word (lock 상태 정보)         │
│   - Unlocked                        │
│   - Biased (편향 락)                 │
│   - Lightweight Lock (경량 락)       │
│   - Heavyweight Lock (중량 락)       │
└─────────────────────────────────────┘
```

### Q: volatile 키워드는 언제 사용하나요?

**용도**: 변수의 가시성(Visibility) 보장

```java
public class VolatileExample {
    // volatile 없이
    private boolean running = true;  // 각 스레드가 캐시된 값 사용 가능

    // volatile 사용
    private volatile boolean running = true;  // 항상 메인 메모리에서 읽음

    public void stop() {
        running = false;  // 다른 스레드에서 즉시 확인 가능
    }

    public void run() {
        while (running) {
            // 작업 수행
        }
    }
}
```

**volatile의 한계**
```java
private volatile int count = 0;

// 원자성 보장 안됨!
count++;  // read → increment → write (3단계)

// 해결책: AtomicInteger 사용
private AtomicInteger atomicCount = new AtomicInteger(0);
atomicCount.incrementAndGet();  // 원자적 연산
```

| 구분 | synchronized | volatile |
|------|--------------|----------|
| 가시성 | O | O |
| 원자성 | O | X |
| 블로킹 | O (대기 발생) | X |
| 사용 범위 | 블록/메서드 | 변수 |

---

## 핵심 정리

| 주제 | 핵심 키워드 |
|------|-------------|
| String | 불변성, Pool, Thread-Safe, 해시 캐싱 |
| equals/hashCode | 계약 준수, HashMap 동작, Objects.hash() |
| 제네릭 | 타입 소거, PECS, 와일드카드 |
| JVM 메모리 | Heap, Stack, Method Area, 스택 프레임 |
| GC | Generational, STW, G1GC, ZGC |
| 동시성 | synchronized, volatile, 가시성/원자성 |

---

*마지막 업데이트: 2026년 01월*
