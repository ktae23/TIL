# Virtual Thread vs WebFlux vs Kotlin Coroutine 비교

---

## 목차

1. [개요](#1-개요)
2. [핵심 개념 비교](#2-핵심-개념-비교)
3. [아키텍처 비교](#3-아키텍처-비교)
4. [코드 비교: 동일한 작업, 세 가지 방식](#4-코드-비교-동일한-작업-세-가지-방식)
5. [성능 특성 비교](#5-성능-특성-비교)
6. [Spring Boot 통합 비교](#6-spring-boot-통합-비교)
7. [언제 무엇을 선택할까?](#7-언제-무엇을-선택할까)
8. [마이그레이션 고려사항](#8-마이그레이션-고려사항)
9. [참고 자료](#9-참고-자료)

---

## 1. 개요

Java/Kotlin 생태계에서 높은 동시성을 처리하는 대표적인 세 가지 접근 방식을 비교한다.

| 기술 | 등장 시점 | 언어 | 핵심 아이디어 |
|------|-----------|------|---------------|
| **WebFlux** | Spring 5 (2017) | Java/Kotlin | Reactive Streams 기반 비동기 논블로킹 |
| **Kotlin Coroutine** | Kotlin 1.3 (2018) | Kotlin | suspend 함수 기반 경량 비동기 |
| **Virtual Thread** | JDK 21 (2023) | Java | JVM 레벨 경량 스레드 |

---

## 2. 핵심 개념 비교

| 비교 항목 | Virtual Thread | WebFlux (Reactor) | Kotlin Coroutine |
|-----------|---------------|-------------------|------------------|
| **동시성 모델** | 스레드 기반 (M:N 스케줄링) | 이벤트 루프 + Reactive Streams | suspend/resume (CPS 변환) |
| **코드 스타일** | 동기식 (기존 코드 그대로) | `Mono`/`Flux` 체이닝 | `suspend fun` + 구조적 동시성 |
| **블로킹 I/O** | ✅ 자연스럽게 지원 (unmount) | ❌ 사용 금지 (스레드 점유) | ⚠️ `Dispatchers.IO`로 위임 |
| **학습 곡선** | 낮음 (기존 Java 그대로) | 높음 (Reactive 패러다임) | 중간 (suspend 개념 이해 필요) |
| **디버깅** | 쉬움 (스택 트레이스 자연스러움) | 어려움 (비동기 체인 추적 난해) | 중간 (coroutine debugger 지원) |
| **백프레셔** | ❌ 별도 구현 필요 | ✅ Reactive Streams 내장 | ✅ Channel/Flow로 지원 |
| **취소 전파** | `interrupt()` 기반 | `Disposable.dispose()` | 구조적 동시성 (자동 전파) |
| **생태계** | 기존 Java 라이브러리 모두 호환 | Reactive 전용 드라이버 필요 | Kotlin 전용, Java 상호운용 가능 |

---

## 3. 아키텍처 비교

### Virtual Thread

```
요청 → Virtual Thread 생성 (수 KB)
        ↓
     비즈니스 로직 (동기 코드)
        ↓
     Blocking I/O 발생
        ↓
     VT unmount (Carrier Thread 반환)
        ↓
     I/O 완료 → VT remount → 이어서 실행
```

### WebFlux (Reactor)

```
요청 → Event Loop Thread (소수 고정)
        ↓
     Mono/Flux 파이프라인 구성
        ↓
     Non-blocking I/O 호출
        ↓
     콜백 등록 → Event Loop 다른 요청 처리
        ↓
     I/O 완료 이벤트 → 콜백 실행 → 응답
```

### Kotlin Coroutine

```
요청 → Coroutine 생성 (수백 바이트)
        ↓
     suspend fun 호출
        ↓
     suspension point에서 중단
        ↓
     Dispatcher가 스레드 반환
        ↓
     재개 시 Dispatcher가 스레드 할당 → 이어서 실행
```

---

## 4. 코드 비교: 동일한 작업, 세 가지 방식

**시나리오**: 사용자 ID로 사용자 조회 → 주문 목록 조회 → 결과 조합 반환

### Virtual Thread (Spring MVC + JDK 21)

```java
@RestController
public class OrderController {

    @GetMapping("/users/{id}/orders")
    public UserOrderResponse getUserOrders(@PathVariable Long id) {
        // 동기 코드 — Virtual Thread 위에서 실행
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        List<Order> orders = orderRepository.findByUserId(id);

        return new UserOrderResponse(user.getName(), orders);
    }
}
```

**병렬 호출이 필요한 경우:**

```java
@GetMapping("/users/{id}/dashboard")
public DashboardResponse getDashboard(@PathVariable Long id) {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        Subtask<User> userTask = scope.fork(() -> userService.getUser(id));
        Subtask<List<Order>> ordersTask = scope.fork(() -> orderService.getOrders(id));
        Subtask<Integer> pointsTask = scope.fork(() -> pointService.getPoints(id));

        scope.join().throwIfFailed();

        return new DashboardResponse(
            userTask.get(), ordersTask.get(), pointsTask.get()
        );
    }
}
```

### WebFlux (Reactor)

```java
@RestController
public class OrderController {

    @GetMapping("/users/{id}/orders")
    public Mono<UserOrderResponse> getUserOrders(@PathVariable Long id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException(id)))
                .flatMap(user ->
                    orderRepository.findByUserId(id)
                        .collectList()
                        .map(orders -> new UserOrderResponse(user.getName(), orders))
                );
    }
}
```

**병렬 호출이 필요한 경우:**

```java
@GetMapping("/users/{id}/dashboard")
public Mono<DashboardResponse> getDashboard(@PathVariable Long id) {
    Mono<User> userMono = userService.getUser(id);
    Mono<List<Order>> ordersMono = orderService.getOrders(id).collectList();
    Mono<Integer> pointsMono = pointService.getPoints(id);

    return Mono.zip(userMono, ordersMono, pointsMono)
            .map(tuple -> new DashboardResponse(
                tuple.getT1(), tuple.getT2(), tuple.getT3()
            ));
}
```

### Kotlin Coroutine (Spring WebFlux + Coroutine)

```kotlin
@RestController
class OrderController(
    private val userRepository: UserRepository,
    private val orderRepository: OrderRepository
) {

    @GetMapping("/users/{id}/orders")
    suspend fun getUserOrders(@PathVariable id: Long): UserOrderResponse {
        val user = userRepository.findById(id)
            ?: throw UserNotFoundException(id)

        val orders = orderRepository.findByUserId(id)

        return UserOrderResponse(user.name, orders)
    }
}
```

**병렬 호출이 필요한 경우:**

```kotlin
@GetMapping("/users/{id}/dashboard")
suspend fun getDashboard(@PathVariable id: Long): DashboardResponse =
    coroutineScope {
        val userDeferred = async { userService.getUser(id) }
        val ordersDeferred = async { orderService.getOrders(id) }
        val pointsDeferred = async { pointService.getPoints(id) }

        DashboardResponse(
            userDeferred.await(),
            ordersDeferred.await(),
            pointsDeferred.await()
        )
    }
```

---

## 5. 성능 특성 비교

| 특성 | Virtual Thread | WebFlux | Kotlin Coroutine |
|------|---------------|---------|------------------|
| **스레드/코루틴 생성 비용** | 수 KB (힙 할당) | N/A (이벤트 루프 공유) | 수백 바이트 |
| **컨텍스트 스위칭** | JVM 내 스택 swap | 없음 (콜백 기반) | CPS 상태머신 전환 |
| **I/O Bound 처리량** | 매우 높음 | 매우 높음 | 매우 높음 |
| **CPU Bound 성능** | Platform Thread와 동일 | Event Loop 블로킹 위험 | `Dispatchers.Default` 활용 |
| **메모리 효율** | 높음 | 매우 높음 | 매우 높음 |
| **Cold Start** | 빠름 | 느림 (Netty 초기화) | 보통 |
| **최대 동시 연결** | 수백만 | 수십만 | 수백만 |

### 벤치마크 참고 (I/O Bound, 1만 동시 요청)

```
┌─────────────────────┬──────────┬──────────┬──────────────┐
│ Metric              │ VThread  │ WebFlux  │ Coroutine    │
├─────────────────────┼──────────┼──────────┼──────────────┤
│ Throughput (req/s)  │ ~45,000  │ ~48,000  │ ~47,000      │
│ Avg Latency (ms)    │ ~12      │ ~11      │ ~11          │
│ P99 Latency (ms)    │ ~35      │ ~30      │ ~32          │
│ Memory Usage (MB)   │ ~350     │ ~280     │ ~290         │
└─────────────────────┴──────────┴──────────┴──────────────┘
* 수치는 대략적인 참고 값이며 환경/워크로드에 따라 다름
```

> 세 가지 모두 I/O Bound 워크로드에서는 **비슷한 수준의 처리량**을 보인다. 차이는 주로 **개발 생산성, 코드 복잡도, 생태계 호환성**에서 발생한다.

---

## 6. Spring Boot 통합 비교

| 항목 | Virtual Thread | WebFlux | Kotlin Coroutine |
|------|---------------|---------|------------------|
| **기반 서버** | Tomcat (기본) | Netty (기본) | Netty (WebFlux 위) |
| **Spring 설정** | `spring.threads.virtual.enabled=true` | `spring-boot-starter-webflux` | WebFlux + `kotlinx-coroutines-reactor` |
| **Controller 반환 타입** | 일반 객체 | `Mono<T>` / `Flux<T>` | `suspend fun` → 일반 객체 |
| **Repository** | Spring Data JPA (그대로) | Spring Data R2DBC | Spring Data R2DBC + Coroutine 확장 |
| **DB 드라이버** | JDBC (기존 드라이버) | R2DBC (Reactive 전용) | R2DBC |
| **Transaction** | `@Transactional` (그대로) | `@Transactional` (Reactive) | `@Transactional` + coroutine 지원 |
| **기존 코드 마이그레이션** | 거의 없음 | 전면 재작성 | 부분 재작성 |

### Spring Boot 설정 예시

**Virtual Thread:**

```yaml
# application.yml — 이게 전부
spring:
  threads:
    virtual:
      enabled: true
```

**WebFlux:**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-r2dbc</artifactId>
</dependency>
```

**Kotlin Coroutine:**

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
}
```

---

## 7. 언제 무엇을 선택할까?

### Virtual Thread 선택 ✅

- 기존 Spring MVC + JDBC 프로젝트의 **동시성 개선**
- 팀이 **Java 중심**이고 Reactive 경험이 없을 때
- **블로킹 라이브러리**(JDBC, HttpClient 등)를 그대로 사용해야 할 때
- 코드 가독성과 디버깅 편의성이 중요할 때
- JDK 21+ 사용 가능한 환경

### WebFlux 선택 ✅

- **백프레셔**가 핵심 요구사항일 때 (대량 스트리밍 처리)
- **SSE, WebSocket** 등 스트리밍 API가 많을 때
- 팀이 이미 Reactive 패러다임에 익숙할 때
- 극한의 메모리 효율이 필요할 때

### Kotlin Coroutine 선택 ✅

- **Kotlin을 주 언어**로 사용하는 팀
- WebFlux의 성능 + 동기식 코드 가독성을 모두 원할 때
- **구조적 동시성**(Structured Concurrency)이 필요할 때
- Flow 기반 **스트리밍 처리**가 필요할 때

### 선택 플로우차트

```
Q1. JDK 21+ 사용 가능한가?
├── NO → WebFlux 또는 Coroutine
└── YES
    Q2. 백프레셔/스트리밍이 핵심인가?
    ├── YES → WebFlux 또는 Coroutine Flow
    └── NO
        Q3. 팀 주 언어는?
        ├── Java → Virtual Thread ✅
        └── Kotlin → Coroutine ✅
```

---

## 8. 마이그레이션 고려사항

### 기존 Spring MVC → Virtual Thread

```diff
  # application.yml
+ spring:
+   threads:
+     virtual:
+       enabled: true
```

> **가장 간단하다.** 코드 변경 없이 설정 한 줄로 적용 가능. 단, `synchronized` 블록 내 I/O가 있으면 Pinning 발생 → JDK 24에서 해결됨.

### 기존 WebFlux → Virtual Thread

주의: **전환 비용이 크다.** WebFlux를 쓰는 이유(백프레셔, 스트리밍)가 여전히 필요하다면 전환할 필요 없음.

```
Mono/Flux 체이닝  →  동기 코드로 풀어서 재작성
R2DBC             →  JDBC로 교체
Reactive 드라이버 →  블로킹 드라이버로 교체
```

### WebFlux → Kotlin Coroutine (점진적 마이그레이션)

```kotlin
// WebFlux Mono → suspend fun으로 변환
// 기존
fun getUser(id: Long): Mono<User> = userRepository.findById(id)

// 변환 후
suspend fun getUser(id: Long): User? = userRepository.findById(id).awaitSingleOrNull()
```

> Reactor의 `kotlinx-coroutines-reactor` 브릿지를 통해 **점진적 마이그레이션** 가능.

---

## 9. 참고 자료

- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [JEP 491: Synchronize Virtual Threads without Pinning](https://openjdk.org/jeps/491)
- [Spring Framework - Virtual Threads Support](https://spring.io/blog/2023/09/09/all-together-now-spring-boot-3-2-graalvm-native-images-java-21-virtual)
- [Project Reactor Reference Guide](https://projectreactor.io/docs/core/release/reference/)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Spring WebFlux + Coroutine](https://docs.spring.io/spring-framework/reference/languages/kotlin/coroutines.html)

---

*마지막 업데이트: 2026년 04월*
