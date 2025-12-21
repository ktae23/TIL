# Java Virtual Threads 완벽 가이드

---

## 목차

1. [개요 및 역사](#1-개요-및-역사)
2. [핵심 개념](#2-핵심-개념)
3. [기본 사용법](#3-기본-사용법)
4. [Spring Boot에서의 활용](#4-spring-boot에서의-활용)
5. [Pinning 문제와 해결](#5-pinning-문제와-해결)
6. [베스트 프랙티스](#6-베스트-프랙티스)
7. [성능 및 모니터링](#7-성능-및-모니터링)
8. [참고 자료](#8-참고-자료)

---

## 1. 개요 및 역사

### 1.1 Virtual Threads란?

Virtual Threads(가상 스레드)는 **JDK가 제공하는 경량 스레드**로, 운영체제(OS)가 아닌 JVM에서 관리됩니다. 기존 Platform Thread(플랫폼 스레드)와 달리 수백만 개의 스레드를 생성해도 시스템 리소스를 거의 소모하지 않습니다.

### 1.2 릴리스 히스토리

| 버전 | JEP | 상태 | 주요 변경사항 |
|------|-----|------|---------------|
| **JDK 19** | JEP 425 | Preview 1 | 최초 도입 |
| **JDK 20** | JEP 436 | Preview 2 | 피드백 반영 개선 |
| **JDK 21 (LTS)** | JEP 444 | **정식 릴리스** | Thread-local 변수 완전 지원, JNI `IsVirtualThread` 함수 추가 |
| **JDK 24** | JEP 491 | 정식 릴리스 | synchronized 블록 Pinning 문제 해결 |

### 1.3 왜 Virtual Threads인가?

기존 Platform Thread의 한계:

```
Platform Thread = OS Thread (1:1 매핑)
- 스레드당 메모리: 2~10MB
- 100만 스레드 = 최소 2TB 메모리 필요
- 컨텍스트 스위칭 비용 높음
```

Virtual Thread의 해결책:

```
Virtual Thread = JVM 관리 경량 스레드 (M:N 매핑)
- 스레드당 메모리: 수 KB
- 100만 스레드도 수 GB로 가능
- Carrier Thread(Platform Thread) 위에서 실행
```

---

## 2. 핵심 개념

### 2.1 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                      Virtual Threads                        │
│  ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐    │
│  │VT1│ │VT2│ │VT3│ │VT4│ │VT5│ │VT6│ │VT7│ │VT8│ │...│    │
│  └─┬─┘ └─┬─┘ └─┬─┘ └─┬─┘ └─┬─┘ └─┬─┘ └─┬─┘ └─┬─┘ └───┘    │
│    │     │     │     │     │     │     │     │              │
│    └─────┴─────┼─────┴─────┴─────┼─────┴─────┘              │
│                │                 │                          │
│          ┌─────▼─────┐     ┌─────▼─────┐                    │
│          │ Carrier 1 │     │ Carrier 2 │  ← Platform Threads│
│          └─────┬─────┘     └─────┬─────┘                    │
│                │                 │                          │
└────────────────┼─────────────────┼──────────────────────────┘
                 │                 │
           ┌─────▼─────┐     ┌─────▼─────┐
           │ OS Thread │     │ OS Thread │
           └───────────┘     └───────────┘
```

### 2.2 주요 특징

| 특성 | Platform Thread | Virtual Thread |
|------|-----------------|----------------|
| 관리 주체 | OS | JVM |
| 생성 비용 | 높음 (수 ms) | 낮음 (수 μs) |
| 메모리 | 2~10MB | 수 KB |
| 최대 개수 | 수천 개 | 수백만 개 |
| Blocking I/O | 스레드 점유 | Carrier 반환 |
| 풀링 필요 | 필수 | 불필요 |

### 2.3 적합한 사용 케이스

**적합한 경우:**
- I/O 바운드 작업 (네트워크, 파일, DB 통신)
- 대량의 동시 요청 처리 (웹 서버)
- Blocking API 사용 시

**부적합한 경우:**
- CPU 집약적 작업 → Parallel Stream, ForkJoinPool 사용
- 이미 비동기로 잘 구현된 시스템

---

## 3. 기본 사용법

### 3.1 Virtual Thread 생성

```java
// 방법 1: Thread.startVirtualThread()
Thread vThread = Thread.startVirtualThread(() -> {
    System.out.println("Hello from Virtual Thread!");
});

// 방법 2: Thread.ofVirtual().start()
Thread vThread2 = Thread.ofVirtual()
    .name("my-virtual-thread")
    .start(() -> {
        System.out.println("Named Virtual Thread");
    });

// 방법 3: Thread.ofVirtual().unstarted()
Thread vThread3 = Thread.ofVirtual()
    .name("unstarted-vthread")
    .unstarted(() -> {
        System.out.println("Started later");
    });
vThread3.start();
```

### 3.2 ExecutorService 활용

```java
// Virtual Thread per Task Executor (권장)
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

    // 100만 개의 작업 제출 가능
    IntStream.range(0, 1_000_000).forEach(i -> {
        executor.submit(() -> {
            Thread.sleep(Duration.ofSeconds(1));
            return i;
        });
    });

} // try-with-resources로 자동 종료 대기
```

### 3.3 Virtual Thread 확인

```java
Thread current = Thread.currentThread();

// Virtual Thread 여부 확인
if (current.isVirtual()) {
    System.out.println("This is a Virtual Thread");
}

// 스레드 정보 출력
System.out.println(current); // VirtualThread[#21]/runnable@ForkJoinPool-1-worker-1
```

### 3.4 Structured Concurrency (Preview)

JDK 21에서 Preview로 도입된 구조적 동시성:

```java
// --enable-preview 필요
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

    Subtask<String> user = scope.fork(() -> fetchUser());
    Subtask<Integer> order = scope.fork(() -> fetchOrder());

    scope.join();           // 모든 subtask 완료 대기
    scope.throwIfFailed();  // 예외 발생 시 전파

    // 결과 사용
    return new Response(user.get(), order.get());
}
```

---

## 4. Spring Boot에서의 활용

### 4.1 기본 설정 (Spring Boot 3.2+)

```yaml
# application.yml
spring:
  threads:
    virtual:
      enabled: true  # 이 한 줄로 Virtual Thread 활성화!
  main:
    keep-alive: true  # 모든 스레드가 Virtual/Daemon일 때 JVM 유지
```

### 4.2 자동 설정되는 항목

`spring.threads.virtual.enabled=true` 설정 시 자동 적용:

| 컴포넌트 | 동작 |
|----------|------|
| **Tomcat/Jetty** | 요청 처리에 Virtual Thread 사용 |
| **@Async** | Virtual Thread Executor 사용 |
| **@Scheduled** | Virtual Thread로 실행 |
| **RabbitMQ Listener** | Virtual Thread Executor 사용 |
| **Kafka Listener** | Virtual Thread Executor 사용 |
| **Spring Data Redis** | ClusterCommandExecutor에 Virtual Thread 사용 |
| **Apache Pulsar** | VirtualThreadTaskExecutor 사용 |

### 4.3 커스텀 TaskExecutor 설정

```java
@Configuration
public class VirtualThreadConfig {

    @Bean
    public Executor taskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    // 또는 ThreadFactory 사용
    @Bean
    public Executor customVirtualExecutor() {
        ThreadFactory factory = Thread.ofVirtual()
            .name("custom-vt-", 0)
            .factory();
        return Executors.newThreadPerTaskExecutor(factory);
    }
}
```

### 4.4 @Async와 함께 사용

```java
@Service
public class AsyncService {

    @Async  // Virtual Thread에서 실행됨
    public CompletableFuture<String> asyncOperation() {
        // Blocking I/O도 안전하게 사용 가능
        String result = restTemplate.getForObject(url, String.class);
        return CompletableFuture.completedFuture(result);
    }
}
```

### 4.5 WebClient와의 비교

```java
// Virtual Thread 사용 시 - 동기 코드로 간단하게 작성
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) {
    // RestTemplate 사용 (Blocking이지만 Virtual Thread라 OK)
    return restTemplate.getForObject("/api/users/" + id, User.class);
}

// 기존 Reactive 방식 - 복잡한 체이닝 필요
@GetMapping("/users/{id}")
public Mono<User> getUserReactive(@PathVariable Long id) {
    return webClient.get()
        .uri("/api/users/" + id)
        .retrieve()
        .bodyToMono(User.class);
}
```

---

## 5. Pinning 문제와 해결

### 5.1 Pinning이란?

**Pinning(고정)**은 Virtual Thread가 Carrier Thread(Platform Thread)에 "고정"되어 다른 Virtual Thread가 해당 Carrier를 사용하지 못하는 상황입니다.

```
정상 동작:
VT1 → Carrier1 (I/O 대기) → Carrier1 반환 → 다른 VT 실행 가능

Pinning 발생:
VT1 → Carrier1 (synchronized 내 I/O) → Carrier1 점유 유지 ❌
→ Carrier Thread Pool 고갈 가능!
```

### 5.2 JDK 21에서의 Pinning 원인

1. **synchronized 블록/메서드 내 Blocking 작업**
2. **Native 코드 실행 (JNI, FFM API)**

```java
// Pinning 발생 예시 (JDK 21)
synchronized (lock) {
    // 이 안에서 Blocking I/O 수행 시 Pinning!
    Thread.sleep(1000);
    httpClient.send(request, handler);
}
```

### 5.3 JDK 21에서의 해결 방법

```java
// 해결책 1: ReentrantLock 사용
private final ReentrantLock lock = new ReentrantLock();

public void safeMethod() {
    lock.lock();
    try {
        // Blocking I/O 수행 - Pinning 발생 안 함
        Thread.sleep(1000);
    } finally {
        lock.unlock();
    }
}

// 해결책 2: synchronized 범위 최소화
public void optimizedMethod() {
    Data data;
    synchronized (lock) {
        data = prepareData();  // 빠른 작업만
    }
    // Blocking 작업은 synchronized 밖에서
    sendData(data);
}
```

### 5.4 Pinning 감지

```bash
# JVM 옵션으로 Pinning 감지
java -Djdk.tracePinnedThreads=full MyApplication

# 또는 short 모드
java -Djdk.tracePinnedThreads=short MyApplication
```

출력 예시:
```
Thread[#24,ForkJoinPool-1-worker-1,5,CarrierThreads]
    java.base/java.lang.VirtualThread$VThreadContinuation.onPinned(VirtualThread.java:185)
    java.base/java.lang.VirtualThread.park(VirtualThread.java:582)
    java.base/java.lang.System$2.parkVirtualThread(System.java:2643)
    java.base/jdk.internal.misc.VirtualThreads.park(VirtualThreads.java:54)
    java.base/java.util.concurrent.locks.LockSupport.park(LockSupport.java:219)
    ...
```

### 5.5 JDK 24: Pinning 문제 해결! (JEP 491)

**JDK 24부터 synchronized 블록에서 Pinning이 발생하지 않습니다!**

```java
// JDK 24+ : 이제 안전함!
synchronized (lock) {
    Thread.sleep(1000);  // Pinning 없음
    httpClient.send(request, handler);  // Pinning 없음
}
```

> **JEP 491 적용 후:**
> - Virtual Thread가 synchronized 내에서 block되면 Carrier Thread 반환
> - 다른 Platform Thread가 나중에 작업 재개 가능
> - **약 98% 성능 향상** (Pinning이 많던 워크로드 기준)

### 5.6 남아있는 Pinning 케이스 (JDK 24+)

```java
// 여전히 Pinning 발생
// 1. Native 메서드 호출
public native void nativeMethod();

// 2. Foreign Function & Memory API 사용
try (Arena arena = Arena.ofConfined()) {
    // FFM 작업 중 Pinning 가능
}
```

---

## 6. 베스트 프랙티스

### 6.1 DO (권장사항)

```java
// ✅ Virtual Thread per Task Executor 사용
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

// ✅ try-with-resources로 리소스 관리
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(task);
}

// ✅ I/O 바운드 작업에 활용
executor.submit(() -> {
    var response = httpClient.send(request, handler);
    repository.save(parseResponse(response));
});

// ✅ JDK 21에서는 ReentrantLock 고려 (JDK 24+는 불필요)
private final ReentrantLock lock = new ReentrantLock();
```

### 6.2 DON'T (주의사항)

```java
// ❌ Virtual Thread를 풀링하지 마세요
ExecutorService pool = Executors.newFixedThreadPool(100,
    Thread.ofVirtual().factory());  // 의미 없음!

// ❌ ThreadLocal 남용 금지 (메모리 누수 위험)
private static final ThreadLocal<ExpensiveObject> cache =
    ThreadLocal.withInitial(ExpensiveObject::new);
// 수백만 Virtual Thread × ExpensiveObject = 메모리 폭발!

// ❌ CPU 집약적 작업에 사용 금지
executor.submit(() -> {
    // CPU 바운드 작업 - 이점 없음
    return fibonacci(1000000);
});

// ❌ synchronized 내 장시간 Blocking (JDK 21)
synchronized (lock) {
    Thread.sleep(10_000);  // JDK 24 이전에서는 피하세요
}
```

### 6.3 점진적 마이그레이션 전략

```
1단계: 환경 준비
   └── JDK 21+ 업그레이드
   └── Spring Boot 3.2+ 업그레이드

2단계: 설정 활성화
   └── spring.threads.virtual.enabled=true

3단계: 모니터링
   └── Pinning 감지 로그 활성화
   └── Micrometer 메트릭 설정

4단계: 코드 최적화
   └── synchronized → ReentrantLock (JDK 21)
   └── ThreadLocal 사용 검토

5단계: JDK 24 업그레이드 (권장)
   └── Pinning 문제 완전 해결
   └── synchronized 코드 유지 가능
```

### 6.4 ThreadLocal 대안

```java
// ❌ 기존 방식 - Virtual Thread에서 비효율적
private static final ThreadLocal<SimpleDateFormat> formatter =
    ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

// ✅ 대안 1: Immutable 객체 사용
private static final DateTimeFormatter formatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd");

// ✅ 대안 2: 지역 변수로 사용
public String format(LocalDate date) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    return date.format(formatter);
}

// ✅ 대안 3: Scoped Values (Preview in JDK 21+)
private static final ScopedValue<User> CURRENT_USER = ScopedValue.newInstance();

ScopedValue.where(CURRENT_USER, user).run(() -> {
    // CURRENT_USER.get()으로 접근
});
```

---

## 7. 성능 및 모니터링

### 7.1 성능 벤치마크 (참고용)

| 시나리오 | Platform Thread | Virtual Thread | 개선율 |
|----------|-----------------|----------------|--------|
| 500 동시 사용자, 1000 요청 | 기준 | ~2배 향상 | 100% |
| I/O 대기 많은 워크로드 | 기준 | ~5-10배 향상 | 400-900% |
| CPU 집약적 워크로드 | 기준 | 동일 또는 약간 저하 | 0% |

### 7.2 Micrometer 메트릭 설정

```java
@Configuration
public class VirtualThreadMetricsConfig {

    @Bean
    public MeterBinder virtualThreadMetrics() {
        return registry -> {
            Gauge.builder("jvm.threads.virtual",
                    () -> Thread.getAllStackTraces().keySet().stream()
                        .filter(Thread::isVirtual)
                        .count())
                .description("Number of virtual threads")
                .register(registry);
        };
    }
}
```

### 7.3 JFR(Java Flight Recorder) 활용

```bash
# JFR 시작
java -XX:StartFlightRecording=filename=recording.jfr,duration=60s \
     -jar myapp.jar

# 주요 이벤트
# - jdk.VirtualThreadStart
# - jdk.VirtualThreadEnd
# - jdk.VirtualThreadPinned
# - jdk.VirtualThreadSubmitFailed
```

---

## 8. 참고 자료

### 공식 문서
- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [JEP 491: Synchronize Virtual Threads without Pinning](https://openjdk.org/jeps/491)
- [Oracle Virtual Threads Documentation](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html)

### Spring 관련
- [Spring Boot 3.2 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.2-Release-Notes)
- [Working with Virtual Threads in Spring - Baeldung](https://www.baeldung.com/spring-6-virtual-threads)

### 심화 학습
- [Java Virtual Threads: A Case Study - InfoQ](https://www.infoq.com/articles/java-virtual-threads-a-case-study/)
- [JDK 24's Major Improvement: Virtual Threads Without Pinning](https://www.danvega.dev/blog/jdk-24-virtual-threads-without-pinning)
- [Java 24 Thread Pinning Revisited](https://mikemybytes.com/2025/04/09/java24-thread-pinning-revisited/)

---

## 요약

| 항목 | 핵심 내용 |
|------|-----------|
| **정식 릴리스** | JDK 21 (LTS) - JEP 444 |
| **Spring 지원** | Spring Boot 3.2+ |
| **활성화 방법** | `spring.threads.virtual.enabled=true` |
| **적합한 케이스** | I/O 바운드 작업, 대량 동시 요청 |
| **주의사항** | ThreadLocal 남용, CPU 작업 사용 |
| **Pinning 해결** | JDK 24 (JEP 491)로 synchronized 문제 해결 |

---

*마지막 업데이트: 2025년 12월*
