# Java 메모리 누수 탐지 및 분석 가이드

실무에서 Java 애플리케이션의 메모리 누수를 탐지하고 해결하는 방법을 정리합니다.

## 목차

1. [메모리 누수 증상](#1-메모리-누수-증상)
2. [힙 덤프 생성 및 분석](#2-힙-덤프-생성-및-분석)
3. [MAT (Memory Analyzer Tool) 활용](#3-mat-memory-analyzer-tool-활용)
4. [일반적인 메모리 누수 패턴](#4-일반적인-메모리-누수-패턴)
5. [실시간 모니터링](#5-실시간-모니터링)
6. [예방 및 Best Practices](#6-예방-및-best-practices)

---

## 1. 메모리 누수 증상

### 주요 증상

```
1. 점진적인 메모리 사용량 증가
   ┌─────────────────────────────────────────┐
   │ Memory                                  │
   │    ↗                                    │
   │   ↗                                     │
   │  ↗     GC 후에도 기준선이 계속 상승     │
   │ ↗                                       │
   └─────────────────────────────────────────┘

2. 빈번한 Full GC
   - GC 시간 증가
   - 애플리케이션 응답 지연

3. OutOfMemoryError 발생
   - java.lang.OutOfMemoryError: Java heap space
   - java.lang.OutOfMemoryError: GC overhead limit exceeded
```

### JVM 옵션으로 OOM 시 덤프 자동 생성

```bash
java -Xmx2g \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/heapdump.hprof \
     -XX:OnOutOfMemoryError="kill -9 %p" \
     -jar myapp.jar
```

---

## 2. 힙 덤프 생성 및 분석

### 힙 덤프 생성 방법

```bash
# 1. jmap 사용 (JDK 도구)
jmap -dump:live,format=b,file=heapdump.hprof <PID>

# live 옵션: GC 후 살아있는 객체만 덤프 (권장)

# 2. jcmd 사용 (JDK 8+)
jcmd <PID> GC.heap_dump heapdump.hprof

# 3. VisualVM에서 GUI로 생성

# 4. Spring Boot Actuator (운영 환경)
curl -X GET http://localhost:8080/actuator/heapdump -o heapdump.hprof
```

### PID 확인

```bash
# 방법 1: jps
jps -v | grep MyApp

# 방법 2: ps
ps aux | grep java

# 방법 3: pgrep
pgrep -f myapp.jar
```

### 덤프 파일 크기 주의

```
- 힙 덤프 크기 ≈ 힙 메모리 크기
- 2GB 힙 → 약 2GB 덤프 파일
- 충분한 디스크 공간 확보 필요
- 덤프 생성 중 애플리케이션 일시 중단 (STW)
```

---

## 3. MAT (Memory Analyzer Tool) 활용

### MAT 설치 및 설정

```bash
# Eclipse MAT 다운로드
# https://www.eclipse.org/mat/downloads.php

# 대용량 힙 분석을 위한 MAT 메모리 설정
# MemoryAnalyzer.ini 수정
-Xmx8g  # MAT 자체 힙 크기 증가
```

### 주요 분석 기능

**1. Histogram (히스토그램)**
```
클래스별 인스턴스 수와 메모리 사용량

┌─────────────────────────────────────────────────────────┐
│ Class Name            │ Objects │ Shallow Heap │ %     │
├───────────────────────┼─────────┼──────────────┼───────┤
│ byte[]                │ 100,000 │ 50,000,000   │ 25%   │
│ char[]                │ 80,000  │ 40,000,000   │ 20%   │
│ java.lang.String      │ 80,000  │ 2,560,000    │ 1.3%  │
│ com.example.Order     │ 50,000  │ 1,600,000    │ 0.8%  │ ← 의심!
└─────────────────────────────────────────────────────────┘

Shallow Heap: 객체 자체의 메모리
Retained Heap: 객체 + 참조하는 모든 객체 메모리
```

**2. Dominator Tree (지배자 트리)**
```
메모리를 가장 많이 점유하는 객체 계층

com.example.CacheManager
  └── java.util.HashMap (50MB) ← 캐시가 커지고 있음
       └── HashMap$Node[]
            └── com.example.Order (30MB)
            └── com.example.User (20MB)
```

**3. Leak Suspects Report (누수 의심 보고서)**
```
MAT가 자동으로 분석하여 의심 객체 제시

Problem Suspect 1:
  43.2% of the heap is occupied by one instance of
  "java.util.HashMap" loaded by "system class loader"

  The memory is accumulated in one instance of
  "com.example.SessionManager"
```

**4. OQL (Object Query Language)**
```sql
-- 특정 클래스 인스턴스 조회
SELECT * FROM com.example.Order

-- 조건부 조회
SELECT * FROM com.example.User u WHERE u.name = "admin"

-- 크기 조회
SELECT s.@retainedHeapSize FROM java.lang.String s
WHERE s.@retainedHeapSize > 10000

-- 참조 추적
SELECT * FROM OBJECTS (SELECT OBJECTS referrer(s)
FROM java.lang.String s WHERE toString(s) = "leaked")
```

---

## 4. 일반적인 메모리 누수 패턴

### 1. 정적 컬렉션

```java
// 누수 패턴
public class EventCache {
    // static 컬렉션이 계속 커짐
    private static final List<Event> events = new ArrayList<>();

    public void addEvent(Event event) {
        events.add(event);  // 제거 로직 없음!
    }
}

// 해결책
public class EventCache {
    private static final int MAX_SIZE = 1000;
    private static final LinkedList<Event> events = new LinkedList<>();

    public synchronized void addEvent(Event event) {
        if (events.size() >= MAX_SIZE) {
            events.removeFirst();
        }
        events.add(event);
    }
}

// 또는 WeakReference 사용
private static final List<WeakReference<Event>> events = new ArrayList<>();
```

### 2. 캐시 미관리

```java
// 누수 패턴
public class UserCache {
    private final Map<Long, User> cache = new HashMap<>();

    public User getUser(Long id) {
        return cache.computeIfAbsent(id,
            userId -> userRepository.findById(userId).orElse(null));
        // 만료 로직 없음!
    }
}

// 해결책 1: 크기 제한 (Guava Cache)
private final Cache<Long, User> cache = CacheBuilder.newBuilder()
    .maximumSize(10000)
    .expireAfterWrite(10, TimeUnit.MINUTES)
    .build();

// 해결책 2: WeakHashMap (키가 GC되면 자동 제거)
private final Map<Long, User> cache =
    Collections.synchronizedMap(new WeakHashMap<>());

// 해결책 3: Caffeine Cache
private final Cache<Long, User> cache = Caffeine.newBuilder()
    .maximumSize(10000)
    .expireAfterWrite(Duration.ofMinutes(10))
    .recordStats()
    .build();
```

### 3. 리스너/콜백 미해제

```java
// 누수 패턴
public class OrderService {
    public void processOrder(Order order) {
        // 익명 클래스가 외부 객체 참조 유지
        eventBus.register(new OrderEventListener() {
            @Override
            public void onEvent(Event event) {
                // order 객체를 계속 참조
                handleEvent(order, event);
            }
        });
        // unregister 호출 안 함!
    }
}

// 해결책
public class OrderService implements Closeable {
    private final List<OrderEventListener> listeners = new ArrayList<>();

    public void processOrder(Order order) {
        OrderEventListener listener = new OrderEventListener(order);
        listeners.add(listener);
        eventBus.register(listener);
    }

    @Override
    public void close() {
        listeners.forEach(eventBus::unregister);
        listeners.clear();
    }
}
```

### 4. 스레드 로컬 미정리

```java
// 누수 패턴
public class RequestContext {
    private static final ThreadLocal<UserSession> context = new ThreadLocal<>();

    public static void set(UserSession session) {
        context.set(session);
    }

    public static UserSession get() {
        return context.get();
    }
    // remove 호출 안 함 → 스레드 풀 환경에서 누수
}

// 해결책
public class RequestContext {
    private static final ThreadLocal<UserSession> context = new ThreadLocal<>();

    public static void set(UserSession session) {
        context.set(session);
    }

    public static void remove() {
        context.remove();  // 반드시 호출!
    }
}

// Filter에서 정리
@Override
public void doFilter(ServletRequest request, ServletResponse response,
                     FilterChain chain) throws IOException, ServletException {
    try {
        RequestContext.set(extractSession(request));
        chain.doFilter(request, response);
    } finally {
        RequestContext.remove();  // 요청 종료 시 정리
    }
}
```

### 5. 커넥션/스트림 미종료

```java
// 누수 패턴
public String readFile(String path) {
    BufferedReader reader = new BufferedReader(new FileReader(path));
    return reader.readLine();  // reader.close() 호출 안 함!
}

// 해결책: try-with-resources
public String readFile(String path) throws IOException {
    try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
        return reader.readLine();
    }  // 자동으로 close() 호출
}

// DB 커넥션도 동일
public User getUser(Long id) {
    try (Connection conn = dataSource.getConnection();
         PreparedStatement ps = conn.prepareStatement(SQL);
         ResultSet rs = ps.executeQuery()) {
        // ...
    }
}
```

### 6. 내부 클래스의 외부 참조

```java
// 누수 패턴
public class OuterClass {
    private byte[] largeData = new byte[10_000_000];  // 10MB

    public Runnable createTask() {
        // 비정적 내부 클래스는 외부 클래스 참조 유지
        return new Runnable() {
            @Override
            public void run() {
                // OuterClass.this를 암묵적으로 참조
                System.out.println("Task running");
            }
        };
    }
}

// 해결책: static 내부 클래스 또는 람다
public class OuterClass {
    private byte[] largeData = new byte[10_000_000];

    // static 내부 클래스
    private static class Task implements Runnable {
        @Override
        public void run() {
            System.out.println("Task running");
        }
    }

    // 또는 람다 (외부 변수 캡처 주의)
    public Runnable createTask() {
        return () -> System.out.println("Task running");
    }
}
```

---

## 5. 실시간 모니터링

### JConsole / VisualVM

```bash
# JConsole 실행
jconsole

# VisualVM 실행
jvisualvm

# 원격 모니터링을 위한 JMX 설정
java -Dcom.sun.management.jmxremote \
     -Dcom.sun.management.jmxremote.port=9999 \
     -Dcom.sun.management.jmxremote.authenticate=false \
     -Dcom.sun.management.jmxremote.ssl=false \
     -jar myapp.jar
```

### GC 로그 분석

```bash
# GC 로그 활성화 (Java 8)
-XX:+PrintGCDetails
-XX:+PrintGCTimeStamps
-Xloggc:/var/log/gc.log

# GC 로그 활성화 (Java 9+)
-Xlog:gc*:file=/var/log/gc.log:time,uptime,level,tags

# GC 로그 분석 도구
# - GCViewer
# - GCEasy (https://gceasy.io)
```

### Prometheus + Grafana

```yaml
# Spring Boot Actuator + Micrometer
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

```java
// 커스텀 메모리 메트릭
@Component
public class MemoryMetrics {

    public MemoryMetrics(MeterRegistry registry) {
        Gauge.builder("app.cache.size", cache::size)
            .description("Cache size")
            .register(registry);
    }
}
```

---

## 6. 예방 및 Best Practices

### 코드 레벨

```java
// 1. 컬렉션 크기 제한
private final Queue<Event> events = new ArrayBlockingQueue<>(1000);

// 2. 적절한 자료구조 선택
// WeakHashMap: 키가 더 이상 사용되지 않으면 자동 제거
// LinkedHashMap: removeEldestEntry 오버라이드로 크기 제한

// 3. try-with-resources 필수 사용
// 4. ThreadLocal은 반드시 remove()
// 5. 리스너/콜백 등록 시 해제 로직 필수
```

### 설정 레벨

```bash
# 적절한 힙 크기 설정
-Xms2g -Xmx2g  # 최소=최대로 설정하여 힙 크기 변동 방지

# GC 선택 (Java 11+)
-XX:+UseG1GC

# 메타스페이스 제한
-XX:MaxMetaspaceSize=256m

# 직접 메모리 제한
-XX:MaxDirectMemorySize=256m
```

### 테스트 레벨

```java
// 메모리 누수 테스트
@Test
void shouldNotLeakMemory() {
    WeakReference<Order> ref = new WeakReference<>(orderService.createOrder());
    orderService.clearOrder();

    System.gc();

    assertNull(ref.get(), "Order should be garbage collected");
}
```

### 모니터링 알림

```yaml
# Prometheus 알림 규칙
groups:
- name: memory
  rules:
  - alert: HighHeapUsage
    expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.9
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "JVM heap usage > 90%"

  - alert: FrequentFullGC
    expr: increase(jvm_gc_collection_seconds_count{gc="G1 Old Generation"}[5m]) > 5
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "Frequent Full GC detected"
```

---

## 분석 체크리스트

```
□ OOM 발생 시 힙 덤프 자동 생성 설정 확인
□ 힙 덤프 생성 (jmap 또는 jcmd)
□ MAT으로 Leak Suspects Report 확인
□ Dominator Tree에서 큰 객체 확인
□ Histogram에서 비정상적으로 많은 인스턴스 확인
□ GC Root까지 경로 추적 (Path to GC Roots)
□ 코드에서 누수 원인 수정
□ 수정 후 재테스트
```

---

*마지막 업데이트: 2026년 01월*
