# Virtual Thread vs WebFlux vs 코루틴 비교

> 출처: 카카오 제4회 Tech Meet — "JDK 21의 신기능 Virtual Thread 알아보기" (안정수 James)
> https://www.youtube.com/watch?v=vQP6Rs-ywlQ
> 참고: 카카오페이 기술블로그 — "코루틴과 Virtual Thread 비교와 사용"

---

## 목차

1. [세 가지 동시성 모델 한눈에 비교](#1-세-가지-동시성-모델-한눈에-비교)
2. [동작 원리 비교](#2-동작-원리-비교)
3. [성능 벤치마크](#3-성능-벤치마크)
4. [코드 스타일 비교](#4-코드-스타일-비교)
5. [주의사항](#5-주의사항)
6. [선택 기준 — 언제 무엇을 쓸까](#6-선택-기준--언제-무엇을-쓸까)
7. [면접용 핵심 정리](#7-면접용-핵심-정리)
8. [참고 자료](#8-참고-자료)

---

## 1. 세 가지 동시성 모델 한눈에 비교

| 항목 | Virtual Thread (Java) | WebFlux (Reactive) | 코루틴 (Kotlin) |
|-----|----------------------|-------------------|----------------|
| **언어** | Java (JDK 21+) | Java/Kotlin | Kotlin |
| **프로그래밍 모델** | **동기 (명령형)** | **비동기 (선언형/함수형)** | **비동기 (구조화된 동시성)** |
| **스레드 모델** | Virtual Thread per Request | 이벤트 루프 (소수 스레드) | 코루틴 on Dispatcher |
| **블로킹 처리** | 허용 (JVM 자동 Unmount) | 금지 (논블로킹 API 필수) | suspend로 일시 중단 |
| **DB 드라이버** | **JDBC** (기존 그대로) | **R2DBC** (리액티브 전용) | JDBC or R2DBC |
| **학습 곡선** | **매우 낮음** | 높음 | 중간 |
| **디버깅** | 일반 스택 트레이스 | 복잡 (비동기 체인) | 중간 (suspend 추적) |
| **스케줄링** | JVM이 자동 관리 | Netty 이벤트 루프 | 개발자가 Dispatcher 지정 |
| **태스킹 모델** | 선점형 (JVM 관리) | 이벤트 기반 | 협력적 멀티태스킹 |
| **Spring 지원** | Spring Boot 3.2+ | WebFlux starter | Spring + coroutine 통합 |

---

## 2. 동작 원리 비교

### Virtual Thread

```
요청 → Virtual Thread 생성 (경량, ~수KB)
  → Carrier Thread에 Mount
  → 블로킹 I/O 발생
  → JVM이 자동으로 Carrier Thread에서 Unmount
  → Carrier Thread는 다른 Virtual Thread 처리
  → I/O 완료 → 다시 Mount → 응답 반환
```

- ForkJoinPool 내의 Carrier Thread (OS 스레드와 1:1 매핑)를 공유
- 수십만~수백만 개 생성 가능
- **핵심: 기존 코드를 그대로 사용하면서 블로킹 비용을 JVM이 자동 처리**

### WebFlux (Reactive Streams)

```
요청 → 이벤트 루프에 등록
  → 논블로킹 I/O 호출 (Mono/Flux 반환)
  → 콜백 체인으로 결과 처리
  → 스레드 전환 없이 이벤트 루프가 다음 요청 처리
  → I/O 완료 이벤트 → 체인 실행 → 응답 반환
```

- Netty 기반 이벤트 루프 (CPU 코어 수만큼 스레드)
- **핵심: 스레드가 절대 블로킹되지 않으므로 소수 스레드로 높은 동시성 확보**

### 코루틴 (Kotlin Coroutines)

```
요청 → 코루틴 launch/async
  → Dispatcher에 의해 스레드에 배치
  → suspend 함수 호출 (일시 중단)
  → 해당 스레드는 다른 코루틴 처리
  → I/O 완료 → Continuation으로 재개 → 응답 반환
```

- `Dispatchers.IO`: I/O 전용 스레드풀 (기본 64개)
- `Dispatchers.Default`: CPU 연산용 (코어 수만큼)
- **핵심: suspend 키워드로 비동기를 동기처럼 작성, 개발자가 중단점 명시**

---

## 3. 성능 벤치마크

### 3.1 카카오 벤치마크 — I/O 블로킹 (Sleep 1초)

| 항목 | Platform Thread | Virtual Thread | 배수 |
|-----|----------------|----------------|------|
| **TPS** | ~200 (톰캣 기본) | **~3,000** | **15배** |

> Virtual Thread 적용만으로 I/O 바운드 작업의 처리량이 15배 향상

### 3.2 Virtual Thread vs WebFlux — DB + 외부 API (300ms 지연)

| 동시 사용자 | Virtual Thread (TPS) | WebFlux (TPS) | 차이 |
|-----------|---------------------|--------------|------|
| 100명 | 258 | 270 | WebFlux +4.5% |
| 200명 | 391 | 454 | WebFlux +16% |
| 400명 | 596 | 612 | WebFlux +2.6% |
| 800명 | 615 | 653 | WebFlux +6.2% |

> WebFlux가 처리량에서 약간 우위이지만, 격차가 크지 않음
> Virtual Thread는 기존 JDBC 코드를 그대로 유지할 수 있다는 이점

### 3.3 Virtual Thread vs 코루틴 — CPU 연산 (10만 개 소수 찾기)

| 항목 | Virtual Thread | 코루틴 | 비고 |
|-----|----------------|--------|------|
| **실행 시간** | ~1,914ms | ~2,261ms | VT가 **15% 빠름** |
| **메모리 사용** | 29.6MB | 67.2MB | VT가 **2.3배 적음** |
| **CPU 시간** | 174ms | 451ms | VT가 **2.6배 효율적** |

> 카카오페이 벤치마크 기준, Virtual Thread가 코루틴 대비 전반적으로 효율적

### 3.4 카카오 벤치마크 — DB 커넥션 제한 상황 (MySQL max_connections=150)

| 항목 | Platform Thread | Virtual Thread |
|-----|----------------|----------------|
| TPS | ~150 | 처음엔 높지만 → **커넥션 타임아웃 발생** |

> **핵심 교훈**: Virtual Thread를 켜기만 하면 안 됨. 뒷단 리소스(DB 커넥션) 관리가 필수

---

## 4. 코드 스타일 비교

### 4.1 동일한 작업: "사용자 조회 → 주문 조회 → 응답"

**Virtual Thread (Java — 기존 동기 코드 그대로)**

```java
@GetMapping("/orders")
public OrderResponse getOrders(@RequestParam Long userId) {
    User user = userRepository.findById(userId);     // 블로킹 OK
    List<Order> orders = orderRepository.findByUser(user); // 블로킹 OK
    return new OrderResponse(user, orders);
}
```

```properties
# application.properties — 이것만 추가하면 끝
spring.threads.virtual.enabled=true
```

**WebFlux (Reactive)**

```java
@GetMapping("/orders")
public Mono<OrderResponse> getOrders(@RequestParam Long userId) {
    return userRepository.findById(userId)           // R2DBC 필요
        .flatMap(user ->
            orderRepository.findByUser(user)
                .collectList()
                .map(orders -> new OrderResponse(user, orders))
        );
}
```

**코루틴 (Kotlin)**

```kotlin
@GetMapping("/orders")
suspend fun getOrders(@RequestParam userId: Long): OrderResponse {
    val user = userRepository.findById(userId)      // suspend 함수
    val orders = orderRepository.findByUser(user)   // suspend 함수
    return OrderResponse(user, orders)
}
```

### 4.2 병렬 실행 비교

**Virtual Thread**

```java
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    var userTask = scope.fork(() -> userService.getUser(id));
    var orderTask = scope.fork(() -> orderService.getOrders(id));
    scope.join().throwIfFailed();
    return new Result(userTask.get(), orderTask.get());
}
```

**WebFlux**

```java
Mono.zip(
    userService.getUser(id),
    orderService.getOrders(id)
).map(tuple -> new Result(tuple.getT1(), tuple.getT2()));
```

**코루틴**

```kotlin
coroutineScope {
    val user = async { userService.getUser(id) }
    val orders = async { orderService.getOrders(id) }
    Result(user.await(), orders.await())
}
```

---

## 5. 주의사항

### 5.1 Virtual Thread

| 주의사항 | 설명 | 해결 |
|---------|------|------|
| **Pinning** | `synchronized` 블록에서 Carrier Thread가 블로킹됨 | `ReentrantLock` 사용 |
| **DB 커넥션 고갈** | 요청 폭증 → HikariCP max-pool-size 초과 | `Semaphore`로 동시 접근 제한 |
| **ThreadLocal 메모리** | 수백만 VT × ThreadLocal = 메모리 폭발 | `ScopedValue` (Preview) 사용 |
| **Thread Pool 사용 금지** | VT를 풀링하면 의미 없음 (생성 비용이 매우 낮음) | `Executors.newVirtualThreadPerTaskExecutor()` |
| **CPU 바운드 부적합** | CPU 연산에는 이점 없음 | Platform Thread 사용 |

```java
// ❌ Pinning 발생
synchronized (lock) {
    jdbcCall();  // Carrier Thread 통째로 블로킹!
}

// ✅ ReentrantLock으로 해결
private final ReentrantLock lock = new ReentrantLock();

lock.lock();
try {
    jdbcCall();  // Virtual Thread만 블로킹, Carrier는 해제
} finally {
    lock.unlock();
}
```

```java
// ❌ DB 커넥션 고갈
// Virtual Thread가 수천 개 동시 실행 → DB 커넥션 동시 요청 폭증

// ✅ Semaphore로 제한
private final Semaphore dbSemaphore = new Semaphore(50); // 동시 50개 제한

dbSemaphore.acquire();
try {
    jdbcCall();
} finally {
    dbSemaphore.release();
}
```

### 5.2 WebFlux

| 주의사항 | 설명 |
|---------|------|
| **블로킹 코드 금지** | 이벤트 루프 스레드에서 블로킹하면 전체 성능 저하 |
| **JDBC 사용 불가** | R2DBC 등 논블로킹 드라이버 필요 |
| **디버깅 어려움** | 비동기 체인의 스택 트레이스가 복잡 |
| **학습 곡선** | Mono/Flux, 연산자 (flatMap, zip, retry 등) 숙지 필요 |

### 5.3 코루틴

| 주의사항 | 설명 |
|---------|------|
| **Dispatcher 관리** | I/O vs CPU 작업에 맞는 Dispatcher 선택 필수 |
| **구조화된 동시성** | CoroutineScope 관리, 예외 전파 규칙 숙지 필요 |
| **Java 라이브러리 호환** | 블로킹 Java 라이브러리 사용 시 `withContext(Dispatchers.IO)` 필요 |

---

## 6. 선택 기준 — 언제 무엇을 쓸까

| 상황 | 추천 | 이유 |
|-----|------|------|
| 기존 Java Spring MVC + I/O 많음 | **Virtual Thread** | 설정 한 줄로 15배 성능 향상, 코드 변경 없음 |
| 팀에 리액티브 경험 없음 | **Virtual Thread** | 학습 비용 최소 |
| Kotlin 프로젝트 | **코루틴** | 언어 네이티브 지원, suspend로 자연스러운 비동기 |
| WebSocket / SSE 스트리밍 | **WebFlux** | 이벤트 루프가 자연스러움 |
| 극한의 처리량 + 리소스 효율 | **WebFlux** | 벤치마크 상 처리량 약간 우위 |
| 결제 트랜잭션 처리 | **Virtual Thread** | JDBC/JPA + @Transactional 그대로 유지 |
| CPU 바운드 작업 | **Platform Thread** | 셋 다 이점 없음 |
| JDK 버전 제약 (< 21) | **WebFlux 또는 코루틴** | Virtual Thread는 JDK 21+ 필수 |
| Java + Kotlin 혼용 프로젝트 | **Virtual Thread** | 양쪽 모두 호환, 추가 러닝커브 없음 |

---

## 7. 면접용 핵심 정리

### "Virtual Thread, WebFlux, 코루틴의 차이를 설명해주세요"

> **세 기술 모두 I/O 바운드 작업의 동시 처리량을 높이는 방법이지만, 접근 방식이 다릅니다.**
>
> **WebFlux**는 논블로킹 I/O + 이벤트 루프 모델로, Mono/Flux 기반의 리액티브 코드를 요구합니다.
> 기존 JDBC를 사용할 수 없고 R2DBC 같은 리액티브 드라이버가 필요합니다.
> 처리량은 가장 높지만 학습 곡선이 가파르고 디버깅이 어렵습니다.
>
> **코루틴**은 Kotlin의 suspend 키워드로 비동기를 동기처럼 작성할 수 있습니다.
> WebFlux보다 코드가 직관적이지만, Dispatcher 관리와 구조화된 동시성 규칙을 알아야 합니다.
>
> **Virtual Thread**는 기존 동기 코드를 그대로 유지하면서, JVM이 블로킹 시점에 Carrier Thread를
> 자동으로 반환합니다. 카카오 벤치마크 기준 Platform Thread 대비 TPS가 15배 향상되며,
> WebFlux와의 처리량 격차도 크지 않습니다 (약 4~16%).
>
> 저는 Vertex AI 프로젝트에서 Virtual Thread를 선택했는데,
> 팀의 리액티브 경험이 없었고, 기존 JDBC/RestTemplate 기반 코드를 유지하면서
> 6배 성능 향상을 달성할 수 있었기 때문입니다.

### "Virtual Thread를 쓸 때 주의할 점은?"

> 1. **synchronized 대신 ReentrantLock** — Pinning으로 Carrier Thread가 블로킹됨
> 2. **DB 커넥션 풀 관리** — 요청 폭증 시 HikariCP 고갈 → Semaphore로 제한
> 3. **ThreadLocal 사용 주의** — 수백만 VT에서 메모리 폭발 가능

---

## 8. 참고 자료

- [카카오 Tech Meet — Virtual Thread 알아보기 (안정수)](https://www.youtube.com/watch?v=vQP6Rs-ywlQ)
- [카카오페이 — 코루틴과 Virtual Thread 비교와 사용](https://tech.kakaopay.com/post/coroutine_virtual_thread_wayne/)
- [카카오페이 — Virtual Thread에 봄(Spring)은 왔는가](https://tech.kakaopay.com/post/ro-spring-virtual-thread/)
- [Virtual Threads vs WebFlux 벤치마크 (Vincenzo Racca)](https://www.vincenzoracca.com/en/blog/framework/spring/virtual-threads-vs-webflux/)
- [우아한형제들 — Java의 미래, Virtual Thread](https://techblog.woowahan.com/15398/)
- [Baeldung — Reactor WebFlux vs Virtual Threads](https://www.baeldung.com/java-reactor-webflux-vs-virtual-threads)

*마지막 업데이트: 2026년 02월*
