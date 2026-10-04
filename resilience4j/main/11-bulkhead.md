# Bulkhead — 두 가지 격리와 스레드풀의 함정

Resilience4j의 Bulkhead는 이름이 하나지만 구현이 둘입니다. `SemaphoreBulkhead`는 동시 실행 수만 세고, `ThreadPoolBulkhead`는 호출을 다른 스레드로 넘깁니다. 후자를 고르는 순간 MDC·SecurityContext·트랜잭션이 끊기고 반환 타입이 `CompletionStage`로 바뀝니다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
  - [🖼 그림으로 보기 — 칸막이, 그리고 같은 스레드냐 갈아타냐](#-그림으로-보기--칸막이-그리고-같은-스레드냐-갈아타냐)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
  - [4-5. 🔢 숫자로 따라가기](#4-5--숫자로-따라가기)
  - [4-6. 🔥 시나리오 — 외부 원장 서버가 2초로 느려진 날](#4-6--시나리오--외부-원장-서버가-2초로-느려진-날)
  - [4-7. 🔥 시나리오 — 마케팅 푸시로 요청이 10배 몰린 날](#4-7--시나리오--마케팅-푸시로-요청이-10배-몰린-날)
- [5. 정리](#5-정리)
- [6. ❓ 헷갈리는 포인트](#6--헷갈리는-포인트)
- [7. 🧾 한 장 요약](#7--한-장-요약)

---

## 1. 핵심 개념 (What)

Bulkhead는 선박의 **격벽**입니다. 배 안을 여러 칸으로 나눠 놓으면 한 칸에 물이 들어와도 배 전체가 가라앉지 않습니다. 타이타닉이 가라앉은 이유가 격벽이 천장까지 닿지 않아 물이 칸을 넘어 번졌기 때문이라는 설명을 들어 봤을 겁니다. 소프트웨어에서 막으려는 것도 정확히 같습니다.

전형적인 장애 시나리오입니다.

```
Tomcat 스레드풀 200개
├── /orders        → 주문 DB (정상, 20ms)
├── /recommend     → 추천 API (장애, 30초 타임아웃)  ← 여기만 느려짐
└── /health        → 응답 없음

추천 API에 초당 10건 유입 × 30초 응답 = 동시 300건
→ Tomcat 스레드 200개 전부 추천 API 대기 중
→ 주문도, 헬스체크도 응답 못 함 → 전체 장애
```

**한 느린 의존성이 공유 스레드풀을 전부 잠식합니다.** 추천 기능만 죽어야 하는데 서비스가 죽습니다. Bulkhead는 "추천 API는 최대 20개까지만"이라고 칸을 나눠서, 나머지 180개를 지킵니다.

### 두 가지 격리

| | `SemaphoreBulkhead` | `ThreadPoolBulkhead` (`FixedThreadPoolBulkhead`) |
|---|---|---|
| 실행 스레드 | **호출 스레드 그대로** | 전용 `ThreadPoolExecutor`의 스레드 |
| 제한 수단 | `Semaphore`로 동시 진입 수 | 풀 크기 + 큐 용량 |
| 반환 타입 | 원래 타입(동기 가능) | **`CompletionStage`** |
| 인터페이스 | `Bulkhead` | `ThreadPoolBulkhead` (별개 인터페이스) |
| 컨텍스트 전파 | 자연스러움(같은 스레드) | **끊긴다.** `ContextPropagator` 필요 |
| 큐잉 | `maxWaitDuration` 동안 대기 | `queueCapacity`만큼 큐에 적재 |
| 거절 예외 | `BulkheadFullException` | `BulkheadFullException` |

핵심 차이는 "스레드가 바뀌는가"입니다. 그 한 줄에서 나머지 모든 차이가 파생됩니다.

### 🖼 그림으로 보기 — 칸막이, 그리고 같은 스레드냐 갈아타냐

먼저 "격벽"이 서버 안에서 어떻게 생겼는지부터 봅니다. Tomcat 스레드 200개는 모든 API가 같이 쓰는 공용 자원입니다. Bulkhead는 그중 "외부 원장 서버를 기다리는 데 쓸 수 있는 몫"에 칸을 칩니다.

```mermaid
flowchart LR
    REQ["유입 요청"] --> TP["Tomcat 스레드 200개<br/>(모든 API 공용)"]
    TP --> P["/points 포인트 지급"]
    TP --> B["/balance 잔액 조회"]
    TP --> H["/health"]
    P --> BH{"Bulkhead ledger<br/>maxConcurrentCalls=50"}
    BH -->|"permit 있음 (최대 50개)"| L["외부 원장 서버 호출"]
    BH -->|"50개 모두 사용 중"| X["BulkheadFullException<br/>→ 폴백 응답 (수 ms)"]
    B --> DB["DB"]
```

원장 서버가 아무리 느려져도 **원장을 기다리는 스레드는 50개를 넘지 못합니다.** 나머지 150개는 `/balance`, `/health`의 몫으로 남습니다.

이제 두 구현에서 한 번의 호출이 어떤 스레드 위에서 도는지 나란히 봅니다. 먼저 `SemaphoreBulkhead`입니다.

```mermaid
sequenceDiagram
    participant T as Tomcat 스레드 exec-7
    participant SB as SemaphoreBulkhead
    participant L as 외부 원장 서버

    T->>SB: tryAcquirePermission()<br/>semaphore.tryAcquire(maxWaitDuration)
    alt permit 있음
        SB-->>T: true
        T->>L: 같은 exec-7 위에서 호출 (MDC · 인증 · TX 그대로)
        L-->>T: 응답
        T->>SB: onComplete() → release()
    else permit 없음, maxWaitDuration 경과
        SB-->>T: false → BulkheadFullException
    end
```

다음은 `ThreadPoolBulkhead`(`FixedThreadPoolBulkhead`)입니다. 호출자 스레드는 일을 **맡기고 돌아오고**, 실제 호출은 `bulkhead-<name>-N` 스레드에서 일어납니다.

```mermaid
sequenceDiagram
    participant T as Tomcat 스레드 exec-7
    participant EX as ThreadPoolExecutor<br/>(core · queue · max)
    participant W as 워커 bulkhead-report-1
    participant L as 하류 작업

    T->>EX: submit(callable)
    alt 스레드 or 큐에 자리 있음
        EX-->>T: CompletableFuture 즉시 반환
        Note over T: exec-7 은 여기서 풀려남
        EX->>W: 큐에서 꺼내 실행
        W->>L: 호출 (MDC · 인증 · TX 없음,<br/>ContextPropagator 로 옮긴 것만)
        L-->>W: 응답
        W-->>T: future 완료 → whenComplete 콜백
    else 큐 full 이고 스레드 == max
        EX--xT: RejectedExecutionException
        Note over T: BulkheadFullException 이 submit() 에서<br/>동기적으로 던져짐 (future 로 오지 않음)
    end
```

### 설정값

`BulkheadConfig` (세마포어 방식):

| 옵션 | 기본값 | 의미 |
|---|---|---|
| `maxConcurrentCalls` | `25` | 동시 실행 허용 수. 0도 허용(전면 차단) |
| `maxWaitDuration` | `Duration.ofSeconds(0)` | 자리가 없을 때 기다릴 시간 |
| `writableStackTraceEnabled` | `true` | `false`면 `BulkheadFullException` 스택트레이스가 빈 배열 |
| `fairCallHandlingEnabled` | `true` | `Semaphore`의 fair/unfair. true면 FIFO 보장 |

`ThreadPoolBulkheadConfig` (스레드풀 방식):

| 옵션 | 기본값 | 의미 |
|---|---|---|
| `maxThreadPoolSize` | `availableProcessors()` | 최대 스레드 수 |
| `coreThreadPoolSize` | `availableProcessors() - 1` (최소 1) | 코어 스레드 수 |
| `queueCapacity` | `100` | 대기 큐 크기. `0`이면 `SynchronousQueue` |
| `keepAliveDuration` | `Duration.ofMillis(20)` | 코어 초과 유휴 스레드 생존 시간 |
| `writableStackTraceEnabled` | `true` | 위와 동일 |
| `contextPropagators` | `[]` | 스레드 간 컨텍스트 전파기 |
| `rejectedExecutionHandler` | `ThreadPoolExecutor.AbortPolicy` | 거절 정책 |

`build()`에서 `maxThreadPoolSize < coreThreadPoolSize`면 `IllegalArgumentException`입니다.

---

## 2. 왜 알아야 하는가 (Why)

**첫째, Bulkhead는 CircuitBreaker가 못 막는 걸 막습니다.** CircuitBreaker는 실패율/느린호출율이 임계치를 넘어야 열립니다. 그 임계치에 도달하기 **전까지의 구간** — 아직 실패는 아니고 그냥 느린 구간 — 에서 스레드는 이미 잠식됩니다. Bulkhead는 첫 번째 호출부터 상한을 걸어 이 구간을 지웁니다. 둘은 경쟁 관계가 아니라 보완 관계입니다.

**둘째, `ThreadPoolBulkhead`를 잘못 고르면 더 큰 사고가 납니다.** 스레드가 바뀌므로 MDC의 traceId가 사라져 로그 추적이 끊기고, `SecurityContextHolder`가 비어 인증이 날아가고, `@Transactional`의 커넥션이 전파되지 않아 트랜잭션 밖에서 DB를 건드립니다. 이건 "나중에 고치자"가 아니라 **아키텍처 결정**입니다.

**셋째, `queueCapacity`를 크게 잡는 게 왜 위험한지 아는 사람이 적습니다.** 큐가 길면 거절이 줄어드니 좋아 보입니다. 실제로는 요청이 큐에서 썩고, 클라이언트는 이미 타임아웃으로 떠났고, 서버는 아무도 기다리지 않는 응답을 만들고 있습니다. 거절을 큐로 바꾸면 **빠른 실패가 느린 실패로 바뀔 뿐**입니다.

**넷째, v2.4.0에서 바뀐 게 있습니다.** 커밋 7e3ab525(Issue #1592)가 `Bulkhead#acquirePermissionAsync()`를 추가하고 Reactor Bulkhead 연산자를 논블로킹으로 만들었습니다. 이전에는 이벤트 루프 스레드에서 `maxWaitDuration > 0`을 쓰면 루프가 멈췄습니다. 이 제약이 사라졌습니다. 이것도 뒤에서 소스로 봅니다.

---

## 3. 내부 구현 분석 (How)

### 3.1 `SemaphoreBulkhead` — 세마포어 하나가 전부

```java
public SemaphoreBulkhead(String name, @Nullable BulkheadConfig bulkheadConfig,
    Map<String, String> tags) {
    this.name = name;
    this.config = requireNonNull(bulkheadConfig, CONFIG_MUST_NOT_BE_NULL);
    this.tags = requireNonNull(tags, TAGS_MUST_NOTE_BE_NULL);
    // init semaphore
    this.semaphore = new Semaphore(config.getMaxConcurrentCalls(), config.isFairCallHandlingEnabled());
    ...
}

boolean tryEnterBulkhead() {
    long timeout = config.getMaxWaitDuration().toMillis();

    try {
        return semaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS);
    } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        return false;
    }
}
```

- `new Semaphore(maxConcurrentCalls, fair)` — permit이 곧 동시 실행 티켓입니다. `maxWaitDuration=0`이어도 `tryAcquire(0, MILLISECONDS)`는 permit이 있으면 즉시 성공합니다.
- **`toMillis()`에 주의하세요.** `maxWaitDuration(Duration.ofMicros(500))`처럼 1ms 미만을 주면 블로킹 경로에서는 **0으로 절삭**됩니다. 의도한 "0.5ms 대기"가 "대기 없음"이 됩니다. 비동기 경로는 뒤에서 보듯 나노초를 그대로 씁니다.
- `InterruptedException`은 플래그만 복원하고 `false`를 반환합니다. 그래서 `acquirePermission()`이 예외를 구분합니다.

```java
@Override
public void acquirePermission() {
    boolean permitted = tryAcquirePermission();
    if (permitted) {
        return;
    }
    if (Thread.currentThread().isInterrupted()) {
        throw new AcquirePermissionCancelledException();
    }
    throw BulkheadFullException.createBulkheadFullException(this);
}
```

**거절(`BulkheadFullException`)과 취소(`AcquirePermissionCancelledException`)를 구분합니다.** Retry 설정에서 `BulkheadFullException`을 재시도 대상에 넣을지는 논의할 가치가 있지만, `AcquirePermissionCancelledException`은 절대 아닙니다. 호출자가 떠난 것이니까요.

반납은 `onComplete()` 또는 `releasePermission()`입니다. 둘 다 `semaphore.release()` 후 `grantPendingPermissions()`를 호출합니다. 차이는 `onComplete()`가 `BulkheadOnCallFinishedEvent`를 추가로 발행한다는 것뿐입니다. **`decorate*`로 감싸지 않고 직접 획득했다면 `finally`에서 반납을 보장해야 합니다.** 빠뜨리면 permit이 영구 누수되어 Bulkhead가 서서히 잠깁니다.

### 3.2 v2.4.0의 논블로킹 획득 (커밋 7e3ab525, Issue #1592)

기존 `tryAcquirePermission()`은 `Semaphore.tryAcquire(timeout)`에서 **호출 스레드를 블로킹**합니다. 이벤트 루프에서 쓰면 안 되니, `BulkheadConfig.maxWaitDuration()` Javadoc은 "이벤트 루프에서는 0을 쓰라"고 안내해 왔습니다. 커밋 7e3ab525가 이 Javadoc까지 함께 고쳤습니다.

```java
// BulkheadConfig.Builder#maxWaitDuration Javadoc (v2.4.0)
 * Note: for threads running on an event-loop or equivalent (rx computation pool, etc),
 * blocking the thread must be avoided. Callers which use the blocking
 * {@link Bulkhead#acquirePermission()} or {@link Bulkhead#tryAcquirePermission()} on such
 * threads should set maxWaitDuration to 0. {@link Bulkhead#acquirePermissionAsync()} and
 * the Reactor Bulkhead operator wait for a permission without blocking the calling
 * thread, so they can be combined with a non-zero maxWaitDuration on an event-loop. The
 * RxJava 2 and RxJava 3 Bulkhead operators still acquire the permission with the blocking
 * {@link Bulkhead#tryAcquirePermission()}.
```

마지막 문장이 중요합니다. **Reactor 연산자만 논블로킹이 되었고, RxJava 2/3 연산자는 여전히 블로킹**입니다. 섞어 쓰는 코드베이스라면 구분해야 합니다.

새 메서드는 `Bulkhead`의 `default`로 추가되어 기존 커스텀 구현을 깨지 않습니다.

```java
default CompletableFuture<Void> acquirePermissionAsync() {
    CompletableFuture<Void> permission = new CompletableFuture<>();
    try {
        acquirePermission();
        permission.complete(null);
    } catch (Throwable throwable) {
        permission.completeExceptionally(throwable);
    }
    return permission;
}
```

Javadoc이 "The default implementation is a blocking bridge ... It only exists for backwards compatibility"라고 못 박습니다. **기본 구현은 논블로킹이 아닙니다.** 진짜 구현은 `SemaphoreBulkhead`의 오버라이드입니다.

```java
@Override
public CompletableFuture<Void> acquirePermissionAsync() {
    if (pendingPermissions.isEmpty() && semaphore.tryAcquire()) {
        publishBulkheadEvent(() -> new BulkheadOnCallPermittedEvent(name));
        return CompletableFuture.completedFuture(null);
    }
    Duration maxWaitDuration = config.getMaxWaitDuration();
    if (maxWaitDuration.isZero()) {
        publishBulkheadEvent(() -> new BulkheadOnCallRejectedEvent(name));
        return CompletableFuture
            .failedFuture(BulkheadFullException.createBulkheadFullException(this));
    }
    PendingPermission permission = new PendingPermission();
    ScheduledFuture<?> timeoutTask;
    try {
        timeoutTask = SchedulerFactory.getInstance().getScheduler().schedule(() -> {
            if (permission.tryExpire()) {
                permission.completeExceptionally(
                    BulkheadFullException.createBulkheadFullException(this));
            }
        }, toNanosSaturated(maxWaitDuration), TimeUnit.NANOSECONDS);
    } catch (RejectedExecutionException e) {
        // The scheduler was shut down concurrently. Fail before queueing the request, otherwise
        // a request nobody holds would be granted a permit later and leak it.
        return CompletableFuture.failedFuture(e);
    }
    pendingPermissions.offer(permission);
    ...
    grantPendingPermissions();
    return permission;
}
```

- 첫 분기 `pendingPermissions.isEmpty() && semaphore.tryAcquire()` — **대기 큐가 비어 있을 때만** 바로 집습니다. 큐에 사람이 있으면 새치기하지 않습니다. FIFO 공정성을 비동기 경로에서도 지키는 장치입니다.
- `maxWaitDuration.isZero()` → 큐에 넣지 않고 즉시 실패. 블로킹 경로와 의미가 일치합니다.
- 타임아웃은 `SchedulerFactory`의 공유 스케줄러에 `toNanosSaturated(maxWaitDuration)`로 예약합니다. **블로킹 경로의 `toMillis()` 절삭이 여기엔 없습니다.** 커밋 메시지의 "queue sub-millisecond wait durations"가 이걸 말합니다.
- `RejectedExecutionException` 처리 주석이 좋은 교보재입니다. 타임아웃을 못 걸면 **큐에 넣기 전에** 실패시킵니다. 넣어 버리면 아무도 소유하지 않는 요청이 나중에 permit을 받아 **영구 누수**가 됩니다.

`PendingPermission`은 네 상태의 작은 상태 기계입니다.

```java
private static final class PendingPermission extends CompletableFuture<Void> {

    private static final int PENDING = 0;
    private static final int GRANTED = 1;
    private static final int EXPIRED = 2;
    private static final int CANCELLED = 3;

    private final AtomicInteger state = new AtomicInteger(PENDING);

    boolean tryGrant() {
        return state.compareAndSet(PENDING, GRANTED);
    }

    boolean tryExpire() {
        return state.compareAndSet(PENDING, EXPIRED);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (state.compareAndSet(PENDING, CANCELLED)) {
            return super.cancel(mayInterruptIfRunning);
        }
        return isCancelled();
    }
}
```

부여·만료·취소가 모두 `PENDING`에서 CAS로 벗어나므로 **승자가 정확히 하나**입니다. 세 경로가 동시에 들어와도 permit이 두 번 나가거나 새어 나가지 않습니다.

그리고 드레인 루프입니다.

```java
private void grantPendingPermissions() {
    if (grantsInProgress.getAndIncrement() != 0) {
        return;
    }
    RuntimeException consumerFailure = null;
    int missed = 1;
    do {
        while (!pendingPermissions.isEmpty() && semaphore.tryAcquire()) {
            PendingPermission permission = pendingPermissions.poll();
            if (permission == null || !permission.tryGrant()) {
                semaphore.release();
                continue;
            }
            try {
                publishBulkheadEvent(() -> new BulkheadOnCallPermittedEvent(name));
            } catch (RuntimeException e) {
                if (consumerFailure == null) {
                    consumerFailure = e;
                } else {
                    consumerFailure.addSuppressed(e);
                }
            } finally {
                permission.complete(null);
            }
        }
        missed = grantsInProgress.addAndGet(-missed);
    } while (missed != 0);
    if (consumerFailure != null) {
        throw consumerFailure;
    }
}
```

- `grantsInProgress.getAndIncrement() != 0`이면 즉시 반환 — Reactive Streams 구현에서 흔한 **work-stealing 직렬화(missed 카운터)** 패턴입니다. 이미 누군가 루프를 돌고 있으면 카운터만 올리고 떠나고, 돌고 있는 쪽이 그만큼 더 돕니다. 재진입과 동시 호출이 재귀 없이 접힙니다.
- `!permission.tryGrant()`면 `semaphore.release()` — 이미 만료/취소된 요청에 잡아둔 permit을 **반드시 되돌립니다.** 누수 방지의 핵심 한 줄입니다.
- 이벤트 소비자가 던진 예외를 모아두고 **루프를 끝까지 돈 뒤** 던집니다. 중간에 탈출하면 큐가 멈춥니다. 소스 주석이 "A failing event consumer never leaves the loop early, otherwise the drain could get stuck"로 의도를 설명합니다.
- `BulkheadOnCallPermittedEvent`를 `permission.complete(null)` **전에** 발행합니다. `complete()`가 의존 액션을 부여 스레드에서 실행하므로, 그 액션이 호출을 끝내 버리면 Permitted가 Finished보다 늦게 나갈 수 있기 때문입니다.

### 3.3 `FixedThreadPoolBulkhead` — 스레드를 갈아탄다

```java
this.executorService = new ThreadPoolExecutor(config.getCoreThreadPoolSize(),
    config.getMaxThreadPoolSize(),
    config.getKeepAliveDuration().toMillis(), TimeUnit.MILLISECONDS,
    config.getQueueCapacity() == 0 ? new SynchronousQueue<>() : new ArrayBlockingQueue<>(config.getQueueCapacity()),
    threadFactory,
    config.getRejectedExecutionHandler());
```

```java
@Override
public <T> CompletableFuture<T> submit(Callable<T> callable) {
    final CompletableFuture<T> promise = new CompletableFuture<>();
    try {
        CompletableFuture.supplyAsync(ContextPropagator.decorateSupplier(config.getContextPropagator(),() -> {
            try {
                publishBulkheadEvent(() -> new BulkheadOnCallPermittedEvent(name));
                return callable.call();
            } catch (CompletionException e) {
                throw e;
            } catch (Exception e){
                throw new CompletionException(e);
            }
        }), executorService).whenComplete((result, throwable) -> {
            publishBulkheadEvent(() -> new BulkheadOnCallFinishedEvent(name));
            ...
        });
    } catch (RejectedExecutionException rejected) {
        publishBulkheadEvent(() -> new BulkheadOnCallRejectedEvent(name));
        throw BulkheadFullException.createBulkheadFullException(this);
    }
    return promise;
}
```

- 작업은 `ContextPropagator.decorateSupplier(...)`로 감싸집니다. **컨텍스트 전파가 여기서, 설정한 만큼만 일어납니다.** `contextPropagators`가 비어 있으면 아무것도 전파되지 않습니다.
- `BulkheadOnCallPermittedEvent`가 **풀 스레드 안에서** 발행됩니다. 즉 큐에서 꺼내져 실제로 실행이 시작된 시점입니다. 제출 시점이 아닙니다. 이 이벤트를 "진입"으로 읽으면 큐 대기 시간이 보이지 않습니다. 큐는 `Metrics.getQueueDepth()`로 보세요.
- `RejectedExecutionException`을 잡아 `BulkheadFullException`으로 바꿉니다. 단, **이 예외는 `submit()` 호출자에게 동기적으로 던져집니다.** `CompletionStage`로 전달되지 않습니다. `ThreadPoolBulkhead.executeSupplier(...)`를 `try/catch` 없이 쓰고 `.exceptionally(...)`만 붙여 두면 Bulkhead 포화를 놓칩니다. 리뷰에서 자주 보이는 실수입니다.

스레드 이름은 `BulkheadNamingThreadFactory`가 `"bulkhead-" + name` 접두사로 붙입니다(Javadoc: `bulkhead-$name-%d`). 가상 스레드 모드(`ExecutorServiceFactory.getThreadType() == ThreadType.VIRTUAL`)면 `bulkhead-<name>-v-N`이 됩니다. **스레드 덤프에서 어느 Bulkhead가 막혔는지 이름으로 바로 찾을 수 있습니다.** 운영에서 제일 요긴한 기능입니다.

### 3.4 큐 포화: `ThreadPoolExecutor`의 반직관적 동작

여기서 많이 틀립니다. `ThreadPoolExecutor`는 **큐가 꽉 차야 코어 이상으로 스레드를 늘립니다.**

```
작업 제출
 ├─ 실행 중 스레드 < corePoolSize      → 새 스레드 생성
 ├─ 아니면 큐에 offer 성공             → 큐에서 대기  ← 여기서 대부분 멈춤
 ├─ 큐 full & 스레드 < maxPoolSize     → 새 스레드 생성
 └─ 큐 full & 스레드 == maxPoolSize    → RejectedExecutionException
```

기본값이 `coreThreadPoolSize = CPU-1`, `maxThreadPoolSize = CPU`, `queueCapacity = 100`입니다. 8코어 서버라면 core 7, max 8, 큐 100입니다. **큐 100개가 다 차기 전에는 8번째 스레드가 절대 생기지 않습니다.** `maxThreadPoolSize`를 32로 올려도 결과가 거의 달라지지 않습니다 — 큐가 먼저 100개를 받으니까요.

그래서 `queueCapacity`를 크게 잡으면 이렇게 됩니다.

| `queueCapacity` | 동시 실행 | 큐 대기 | p99 지연 | 거절 |
|---|---|---|---|---|
| `0` (`SynchronousQueue`) | core~max | 없음 | 낮음 | 즉시, 많음 |
| `10` | core (큐 찰 때만 max) | 최대 10건 | 중간 | 중간 |
| `100` (기본값) | 대개 core 고정 | 최대 100건 | **매우 높음** | 드묾 |
| `10000` | core 고정 | 사실상 무제한 | 클라이언트 타임아웃 후에도 처리 | 거의 없음 |

응답이 1초 걸리는 의존성에 core 7, 큐 100이면 큐 끝 요청은 **14초를 기다립니다.** 클라이언트는 3초에 떠났습니다. 서버는 11초간 **아무도 받지 않을 응답**을 만듭니다. 그 사이 스레드는 계속 점유됩니다.

**지침: `queueCapacity`는 "순간 버스트를 흡수할 만큼"만 둡니다.** 상한 계산법은 `queueCapacity × 평균응답시간 / coreThreadPoolSize ≤ 클라이언트 타임아웃`입니다. 이 부등식을 못 지키는 큐 길이는 거절보다 나쁩니다. `rejectedExecutionHandler`로 `AbortPolicy`(기본) 대신 `CallerRunsPolicy`를 넣고 싶어지는데, 그건 **호출 스레드에서 실행**하므로 Bulkhead의 격리를 스스로 깨는 선택입니다. 하지 마세요.

### 3.5 두 방식의 호출 경로

```mermaid
flowchart TB
    subgraph SB["SemaphoreBulkhead — 스레드 전환 없음"]
        direction TB
        C1["호출 스레드<br/>(http-nio-8080-exec-7)<br/>MDC ✅ SecurityContext ✅ TX ✅"]
        C1 --> C2["tryAcquirePermission()<br/>semaphore.tryAcquire(maxWaitDuration)"]
        C2 -->|permit 획득| C3["같은 스레드에서 비즈니스 로직 실행<br/>MDC ✅ SecurityContext ✅ TX ✅"]
        C3 --> C4["onComplete() → release + grantPendingPermissions()"]
        C2 -->|"타임아웃"| C5["BulkheadFullException"]
        C2 -->|"인터럽트"| C6["AcquirePermissionCancelledException"]
    end

    subgraph TB2["ThreadPoolBulkhead — 스레드 전환 발생"]
        direction TB
        D1["호출 스레드<br/>(http-nio-8080-exec-7)<br/>MDC ✅ SecurityContext ✅ TX ✅"]
        D1 --> D2["submit(callable)"]
        D2 --> D3{"core 여유 / 큐 여유 / max 여유?"}
        D3 -->|"모두 없음"| D4["RejectedExecutionException<br/>→ BulkheadFullException (동기 throw)"]
        D3 -->|"큐 적재"| D5[("ArrayBlockingQueue<br/>queueCapacity")]
        D5 --> D6
        D3 -->|"즉시 실행"| D6
        D6["===== 스레드 경계 =====<br/>bulkhead-recommend-3<br/>MDC ❌ SecurityContext ❌ TX ❌<br/>ContextPropagator 가 복원한 것만 살아남음"]
        D6 --> D7["callable.call()"]
        D7 --> D8["CompletionStage 완료 → 호출자에게 반환"]
    end
```

`D6`의 점선이 모든 문제의 출발점입니다.

### 3.6 컨텍스트가 끊긴다

`ThreadPoolBulkhead`에서 스레드가 바뀌면 **ThreadLocal에 들어 있던 모든 것**이 사라집니다.

| 잃는 것 | 담긴 곳 | 증상 |
|---|---|---|
| traceId / requestId | `MDC` (`ThreadLocal<Map>`) | 로그에서 요청 추적 불가. 로그가 고아가 됨 |
| 인증 정보 | `SecurityContextHolder` | `getPrincipal()`이 `null` → 403 또는 NPE |
| 트랜잭션/커넥션 | `TransactionSynchronizationManager` | 트랜잭션 밖에서 DB 접근. 롤백 안 됨 |
| 테넌트/로케일 | 애플리케이션 `ThreadLocal` | 다른 테넌트 데이터 조회 — **보안 사고** |

해법은 `ThreadPoolBulkheadConfig.contextPropagator(...)`입니다. `FixedThreadPoolBulkhead.submit()`이 `ContextPropagator.decorateSupplier(config.getContextPropagator(), ...)`로 작업을 감싸므로, 등록한 전파기만 복원됩니다. 자세한 구현과 Spring 연동은 [advanced/10-context-propagation](../advanced/10-context-propagation.md)에서 다룹니다.

**트랜잭션은 `ContextPropagator`로 "전파"하면 안 됩니다.** JDBC 커넥션은 스레드에 묶인 자원이고, 두 스레드가 같은 커넥션을 쓰면 정의되지 않은 동작입니다. 트랜잭션 경계 안에서는 `ThreadPoolBulkhead`를 쓰지 않는 것이 정답입니다. `SemaphoreBulkhead`를 쓰세요.

---

## 4. 실전 예제

### 4-1. 의존성별로 칸 나누기

```java
@Configuration
public class BulkheadTopology {

    /**
     * DB: HikariCP 풀(20)보다 작게. 커넥션 고갈 전에 Bulkhead가 먼저 거절해야
     *     "커넥션 대기 30초" 대신 "즉시 거절"이 된다.
     *     트랜잭션 안에서 쓰므로 반드시 Semaphore 방식.
     */
    @Bean
    Bulkhead orderDbBulkhead() {
        return Bulkhead.of("order-db", BulkheadConfig.custom()
            .maxConcurrentCalls(16)                    // pool 20의 80%
            .maxWaitDuration(Duration.ofMillis(100))   // 짧은 버스트만 흡수
            .writableStackTraceEnabled(false)
            .build());
    }

    /**
     * 외부 추천 API: 느려질 것을 전제로 한다. 리틀의 법칙으로 산정(4-2 참고).
     *     응답을 기다리는 동안 호출 스레드를 잡고 있어도 되는 경로라면 Semaphore로 충분하다.
     */
    @Bean
    Bulkhead recommendApiBulkhead() {
        return Bulkhead.of("recommend-api", BulkheadConfig.custom()
            .maxConcurrentCalls(20)
            .maxWaitDuration(Duration.ZERO)            // 즉시 거절 → 폴백으로
            .writableStackTraceEnabled(false)
            .build());
    }

    /**
     * 캐시(Redis): 빠르고 많이 호출된다. 한도를 넉넉히, 대기는 0.
     *     fairCallHandling을 끄면 경합 시 처리량이 올라간다(순서 보장 포기).
     */
    @Bean
    Bulkhead cacheBulkhead() {
        return Bulkhead.of("cache", BulkheadConfig.custom()
            .maxConcurrentCalls(64)
            .maxWaitDuration(Duration.ZERO)
            .fairCallHandlingStrategyEnabled(false)
            .writableStackTraceEnabled(false)
            .build());
    }

    /**
     * 리포트 생성: 오래 걸리고, 호출 스레드를 점유하면 안 된다 → ThreadPool 방식.
     *     트랜잭션/보안 컨텍스트가 필요 없는 순수 계산 작업에만 쓴다.
     */
    @Bean
    ThreadPoolBulkhead reportBulkhead() {
        return ThreadPoolBulkhead.of("report", ThreadPoolBulkheadConfig.custom()
            .coreThreadPoolSize(4)
            .maxThreadPoolSize(8)
            .queueCapacity(8)                          // 100(기본)은 절대 쓰지 않는다
            .keepAliveDuration(Duration.ofSeconds(30))
            .writableStackTraceEnabled(false)
            .build());
    }
}
```

`queueCapacity(8)`에 주목하세요. core 4 + 큐 8 + max 8이면, 큐가 차는 순간 스레드가 8까지 늘고 그 다음은 거절입니다. 최악 대기 시간이 `8 × 평균응답 / 4`로 제한됩니다. 기본값 100을 그대로 두면 이 상한이 25배가 됩니다.

### 4-2. `maxConcurrentCalls`를 리틀의 법칙으로 산정하기

리틀의 법칙(Little's Law)은 `L = λW`입니다. `L` = 시스템 내 평균 건수, `λ` = 평균 도착률, `W` = 평균 체류 시간.

**절차:**

1. **측정한다.** 추측하지 않습니다. `http_server_requests_seconds_count` / `_sum`이나 APM에서 해당 의존성 호출의 `λ`(초당 호출 수)와 `W`(p95 응답 시간)를 꺼냅니다.
2. **정상 상태의 L을 구한다.** `L = λ × W`
3. **열화 시나리오의 L을 구한다.** 응답이 몇 배 느려질 때까지 버틸지 정합니다. `W_degraded = W × k` (보통 `k = 3~5`)
4. **상한을 확인한다.** 하류 자원(커넥션 풀, 상대 서비스의 동시 허용)과 내 스레드풀 여유를 넘지 않는지 봅니다.
5. **둘 중 작은 값을 고른다.** `maxConcurrentCalls = min(L_degraded, 자원상한)`
6. **거절률을 관찰하고 조정한다.** `BulkheadOnCallRejectedEvent`가 정상 트래픽에서 발생하면 과소 설정입니다.

**계산 예 — 추천 API:**

| 단계 | 값 | 근거 |
|---|---|---|
| λ (측정) | 40 req/s | APM 기준 피크 |
| W (측정, p95) | 250 ms | 같은 기간 |
| L (정상) | 40 × 0.25 = **10** | `L = λW` |
| W 열화 (k=4) | 1,000 ms | 1초까지는 버틴다고 결정 |
| L 열화 | 40 × 1.0 = **40** | 이만큼 동시에 쌓인다 |
| 자원 상한 | Tomcat 200 중 추천에 20% 할당 = **40** | 다른 엔드포인트 보호 |
| HTTP 커넥션 풀 | **20** | WebClient/Apache HC 설정값 |
| **결론** | `maxConcurrentCalls = 20` | `min(40, 40, 20)` |

여기서 커넥션 풀이 진짜 병목이라는 걸 알 수 있습니다. **Bulkhead 한도를 커넥션 풀보다 크게 잡으면 Bulkhead는 통과시키고 커넥션 풀에서 대기합니다.** 그 대기는 Bulkhead 지표에 안 보이므로 장애 원인을 못 찾습니다. 항상 **가장 희소한 자원에 맞추세요.**

검증 코드:

```java
@Test
void maxConcurrentCalls_should_not_exceed_http_connection_pool() {
    int bulkheadLimit = bulkheadRegistry.bulkhead("recommend-api")
        .getBulkheadConfig().getMaxConcurrentCalls();
    int connectionPoolSize = httpClientProperties.getMaxConnectionsPerRoute();

    // Bulkhead가 커넥션 풀보다 느슨하면 병목이 보이지 않는 곳으로 옮겨간다
    assertThat(bulkheadLimit).isLessThanOrEqualTo(connectionPoolSize);
}
```

이 테스트 하나가 설정 드리프트를 막습니다.

### 4-3. `ThreadPoolBulkhead`에서 MDC가 사라지는 걸 재현하기

```java
@SpringBootTest
class ThreadPoolBulkheadMdcLossTest {

    private static final Logger log =
        LoggerFactory.getLogger(ThreadPoolBulkheadMdcLossTest.class);

    @Test
    void mdc_is_lost_when_the_thread_changes() throws Exception {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("mdc-demo",
            ThreadPoolBulkheadConfig.custom()
                .coreThreadPoolSize(1)
                .maxThreadPoolSize(1)
                .queueCapacity(1)
                .build());   // contextPropagator 를 등록하지 않았다

        MDC.put("traceId", "trace-0001");
        log.info("caller thread={} traceId={}",
            Thread.currentThread().getName(), MDC.get("traceId"));
        // caller thread=main traceId=trace-0001

        CompletionStage<String> stage = bulkhead.executeSupplier(() -> {
            String insideThread = Thread.currentThread().getName();
            String insideTraceId = MDC.get("traceId");   // ← null
            log.info("bulkhead thread={} traceId={}", insideThread, insideTraceId);
            // bulkhead thread=bulkhead-mdc-demo-1 traceId=null
            return insideTraceId;
        });

        String traceIdInsideBulkhead = stage.toCompletableFuture().get(2, TimeUnit.SECONDS);

        assertThat(Thread.currentThread().getName()).isEqualTo("main");
        assertThat(MDC.get("traceId")).isEqualTo("trace-0001");  // 호출 스레드는 그대로
        assertThat(traceIdInsideBulkhead).isNull();              // 풀 스레드는 비어 있다
    }

    @Test
    void mdc_survives_with_a_context_propagator() throws Exception {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("mdc-propagated",
            ThreadPoolBulkheadConfig.custom()
                .coreThreadPoolSize(1)
                .maxThreadPoolSize(1)
                .queueCapacity(1)
                .contextPropagator(new MdcContextPropagator())   // 등록
                .build());

        MDC.put("traceId", "trace-0002");
        String inside = bulkhead
            .executeSupplier(() -> MDC.get("traceId"))
            .toCompletableFuture()
            .get(2, TimeUnit.SECONDS);

        assertThat(inside).isEqualTo("trace-0002");
    }
}
```

전파기 구현은 `ContextPropagator` 세 메서드(`retrieve` / `copy` / `clear`)를 채우면 됩니다 — 호출 스레드에서 꺼내고, 풀 스레드에서 심고, 끝나면 지웁니다. 지우지 않으면 **풀 스레드가 재사용되면서 이전 요청의 traceId가 다음 요청에 섞입니다.** 테넌트 ID라면 보안 사고입니다. 전체 구현은 [advanced/10-context-propagation](../advanced/10-context-propagation.md)을 보세요.

### 4-4. 포화를 눈으로 보기

```java
@Component
@RequiredArgsConstructor
public class BulkheadObservability {

    private final MeterRegistry meterRegistry;

    @PostConstruct
    void bindThreadPool(ThreadPoolBulkheadRegistry registry) {
        registry.getAllBulkheads().forEach(b -> {
            ThreadPoolBulkhead.Metrics m = b.getMetrics();
            Gauge.builder("app.bulkhead.queue.depth", m,
                    ThreadPoolBulkhead.Metrics::getQueueDepth)
                .tag("name", b.getName()).register(meterRegistry);
            Gauge.builder("app.bulkhead.queue.remaining", m,
                    ThreadPoolBulkhead.Metrics::getRemainingQueueCapacity)
                .tag("name", b.getName()).register(meterRegistry);
            Gauge.builder("app.bulkhead.thread.active", m,
                    ThreadPoolBulkhead.Metrics::getActiveThreadCount)
                .tag("name", b.getName()).register(meterRegistry);
        });
    }

    @PostConstruct
    void bindSemaphore(BulkheadRegistry registry) {
        registry.getAllBulkheads().forEach(b ->
            Gauge.builder("app.bulkhead.available.calls", b,
                    x -> x.getMetrics().getAvailableConcurrentCalls())
                .tag("name", b.getName()).register(meterRegistry));

        registry.getAllBulkheads().forEach(b ->
            b.getEventPublisher().onCallRejected(e ->
                meterRegistry.counter("app.bulkhead.rejected",
                    "name", e.getBulkheadName()).increment()));
    }
}
```

읽는 법:

- `available.calls`가 0에 붙어 있다 → 하류가 느려졌다는 **선행 지표**. CircuitBreaker가 열리기 전에 먼저 보입니다
- `queue.depth`가 계속 양수 → 큐가 지연을 쌓고 있음. `queueCapacity`를 줄이고 거절을 받아들이는 쪽이 낫습니다
- `rejected`가 평시에 발생 → 한도가 너무 낮거나, 하류가 이미 느려짐. 4-2로 돌아가 재계산
- `thread.active`가 `coreThreadPoolSize`에 붙어 있고 `queue.depth`도 양수 → 3.4에서 설명한 "큐가 차기 전엔 스레드가 안 늘어난다" 상태. 큐를 줄이거나 core를 늘려야 합니다

### 4-5. 🔢 숫자로 따라가기

**(A) `SemaphoreBulkhead` — `maxConcurrentCalls=3`, `maxWaitDuration=0`**

`maxWaitDuration=0`이면 `tryEnterBulkhead()`가 `semaphore.tryAcquire(0, MILLISECONDS)`를 부릅니다. 판단은 이것 하나입니다.

```mermaid
flowchart LR
    IN["호출 도착"] --> Q{"지금 남은 permit > 0 ?"}
    Q -->|yes| RUN["permit 1개 가져가고 즉시 실행<br/>(기다림 0)"]
    Q -->|no| REJ["기다리지 않고 바로 false<br/>→ BulkheadFullException"]
    RUN --> DONE["호출이 끝나야 onComplete() 로 반납"]
```

호출 7건이 겹쳐서 들어옵니다(시각·소요 시간 단위: 초).

| 호출 | 도착 | 소요 | 도착 순간 실행 중 | 결과 | 끝나는 시각 |
|---|---|---|---|---|---|
| A | 0.0 | 1.0 | 없음 | ✅ 진입 (사용 1/3) | 1.0 |
| B | 0.1 | 0.5 | A | ✅ 진입 (2/3) | 0.6 |
| C | 0.2 | 1.0 | A, B | ✅ 진입 (3/3) | 1.2 |
| D | 0.3 | 0.5 | A, B, C | ❌ **BulkheadFullException** (즉시) | — |
| E | 0.7 | 0.5 | A, C (B는 0.6에 반납) | ✅ 진입 (3/3) | 1.2 |
| F | 0.8 | 0.3 | A, C, E | ❌ **BulkheadFullException** (즉시) | — |
| G | 1.15 | 0.3 | C, E (A는 1.0에 반납) | ✅ 진입 (3/3) | 1.45 |

동시 실행은 어느 순간에도 3을 넘지 않고, 거절된 D·F는 **0ms 만에** 실패합니다. 호출 스레드를 붙잡지 않으니 그 스레드는 바로 폴백 응답을 만들러 갑니다.

**(B) 같은 호출, `maxWaitDuration=500ms`** (fair 세마포어라 먼저 기다린 쪽이 먼저 들어감)

| 호출 | 도착 | 기다림 | 진입 시각 | 끝나는 시각 |
|---|---|---|---|---|
| A, B, C | 0.0 / 0.1 / 0.2 | 0 | 즉시 | 1.0 / 0.6 / 1.2 |
| D | 0.3 | 0.3 (B 반납 0.6까지) | 0.6 | 1.1 |
| E | 0.7 | 0.3 (A 반납 1.0까지) | 1.0 | 1.5 |
| F | 0.8 | 0.3 (D 반납 1.1까지) | 1.1 | 1.4 |
| G | 1.15 | 0.05 (C 반납 1.2까지) | 1.2 | 1.5 |

이번엔 7건 모두 성공했지만 D·E·F·G가 **0.05~0.3초씩 호출 스레드를 잡고 기다렸습니다.** 짧은 버스트에는 대기가 이득이고, 하류가 계속 느린 상황에서는 이 대기가 그대로 스레드 점유가 됩니다. 그래서 "대기는 짧게, 0이 기본"입니다.

**(C) 리틀의 법칙 — 응답이 느려지면 동시 실행이 몇 개가 되나** (`L = λ × W`, 유입 20 req/s 고정)

| 하류 응답 시간 W | 동시 실행 L | `maxConcurrentCalls=10`이면 |
|---|---|---|
| 100ms (정상) | 20 × 0.1 = **2** | 여유. 거절 없음 |
| 500ms | 20 × 0.5 = **10** | 딱 한계 |
| 2s (열화) | 20 × 2 = **40** | 동시 10개까지만. 성공 처리량 최대 10 / 2s = **5 req/s**, 나머지 15 req/s는 즉시 거절 |
| 10s (사실상 장애) | 20 × 10 = **200** | 성공 1 req/s, 거절 19 req/s. Bulkhead가 없으면 Tomcat 200개가 전부 이 호출을 기다린다 |

유입은 그대로인데 **응답이 20배 느려지면 동시 실행도 20배**가 됩니다. RateLimiter(유입 속도 제한)로는 이 증가를 막을 수 없는 이유입니다. 그리고 Bulkhead를 건 순간 처리량의 상한은 **`maxConcurrentCalls / W`**가 됩니다 — 4-7 시나리오에서 이 식이 다시 나옵니다.

**(D) `ThreadPoolBulkhead` — core 4 · max 8 · queue 8, 작업당 1초, 20건이 동시에 도착**

3.4의 `ThreadPoolExecutor` 규칙(core → 큐 → max → 거절)을 그대로 적용합니다.

| 도착 순번 | 그 순간 상태 | 처리 | 실행 시간대 |
|---|---|---|---|
| 1~4 | 스레드 < core(4) | 코어 스레드 새로 생성, 즉시 실행 | 0~1s |
| 5~12 | 스레드 = core, 큐에 자리 있음 | **큐에 적재** (8건) | 1~2s |
| 13~16 | 큐 가득, 스레드 < max(8) | 비코어 스레드 생성, 즉시 실행 | 0~1s |
| 17~20 | 큐 가득, 스레드 = max | `RejectedExecutionException` → **`BulkheadFullException`을 `submit()`에서 동기 throw** | — |

먼저 온 5~12번이 나중에 온 13~16번보다 **1초 늦게** 끝납니다. 큐는 순서를 지켜 주지 않습니다. 같은 20건을 기본값 `queueCapacity=100`에 core 4·max 8로 넣으면 어떻게 될까요? 큐가 차지 않으니 스레드는 4개에서 늘지 않고, 20건이 4개씩 다섯 번에 걸쳐 0~5초 동안 처리됩니다. 거절은 0건이지만 마지막 요청은 **4초를 큐에서** 기다립니다.

### 4-6. 🔥 시나리오 — 외부 원장 서버가 2초로 느려진 날

**상황.** 포인트 지급 API가 외부 원장 서버를 호출합니다. 인스턴스당 유입 120 req/s, 원장 응답 평소 100ms → 동시 실행 `L = 120 × 0.1 = 12`. Tomcat 최대 스레드 200개. 같은 서버에 잔액 조회 API와 헬스체크도 있습니다.

**Bulkhead가 없을 때.**

| 시각 | 무슨 일 | 원장 대기 스레드 |
|---|---|---|
| 14:00:00.0 | 원장 서버 응답이 2초로 늘어남 (아직 실패는 아님 → CircuitBreaker 실패율은 0%) | 12 |
| 14:00:00.5 | 새 요청은 계속 들어오는데 아무도 끝나지 않음 | 12 + 60 = 72 |
| 14:00:01.0 | | 132 |
| 14:00:01.6 | **200개 소진.** 잔액 조회·헬스체크 요청도 Tomcat 대기열에서 멈춤 | 200 |
| 14:00:02~ | 정상 상태로 가려면 `120 × 2 = 240`개가 필요한데 200개뿐 → 계속 포화 | 200 |
| 14:00:10~ | 헬스체크 실패로 인스턴스가 재시작 → 남은 인스턴스로 부하가 몰려 연쇄 포화 | — |

느린호출 기반 CircuitBreaker를 걸어 뒀더라도 `minimumNumberOfCalls`만큼 호출이 **끝나야** 판정이 시작됩니다. 2초짜리 호출이 끝나기도 전에 1.6초 만에 스레드가 바닥났습니다.

**`SemaphoreBulkhead`(`maxConcurrentCalls=50`, `maxWaitDuration=0`)가 있을 때.**

| 시각 | 무슨 일 | 원장 대기 스레드 |
|---|---|---|
| 14:00:00.0 | 응답 2초로 늘어남 | 12 |
| 14:00:00.3 | 12 + 120 × 0.3 ≈ 50 → **permit 소진** | 50 |
| 14:00:00.3~ | 이후 원장 호출 성공 처리량 = 50 / 2s = **25 req/s**. 나머지 95 req/s는 `BulkheadFullException`으로 **수 ms 안에** 폴백("지급 요청이 접수되었습니다" + 재처리 큐 적재) | 50 고정 |
| 계속 | Tomcat 200개 중 150개는 잔액 조회·헬스체크가 그대로 사용. **포인트 지급 기능만 열화, 서비스는 생존** | 50 |
| 수십 초 뒤 | 끝난 느린 호출이 `minimumNumberOfCalls`를 채우면 느린호출 CircuitBreaker가 OPEN → 원장 호출 자체를 멈춤. Bulkhead는 그 판정이 나기 전 구간을 버텨 준 셈 | ↓ |

`maxConcurrentCalls=50`은 4-2 절차대로 `min(L_degraded, 자원 상한)`에서 나온 값이어야 합니다. 원장용 HTTP 커넥션 풀이 30이라면 50이 아니라 30입니다.

### 4-7. 🔥 시나리오 — 마케팅 푸시로 요청이 10배 몰린 날

**상황.** 위 서비스에 오후 7시 마케팅 푸시가 나갑니다. 원장 서버는 **멀쩡합니다**(응답 100ms). 유입만 120 → 1,200 req/s로 30초간 뜁니다.

| 항목 | 계산 |
|---|---|
| 필요한 동시 실행 | `L = 1,200 × 0.1 = 120` |
| Bulkhead 상한 50일 때 최대 처리량 | `50 / 0.1s = 500 req/s` |
| 거절 | 1,200 − 500 = **700 req/s** (하류가 건강한데도) |
| `maxWaitDuration=50ms`로 바꾸면 | 처리량 상한은 그대로 500 req/s. 대신 거절될 700 req/s가 50ms씩 스레드를 잡아 **35개**가 추가로 대기 |

Bulkhead는 "느려짐"뿐 아니라 "몰림"에도 똑같이 반응합니다. 처리량 상한이 `maxConcurrentCalls / W`이기 때문입니다. 이 상황에서 할 수 있는 선택은 셋입니다.

1. **상한을 피크 기준으로 다시 잡는다** — 원장 커넥션 풀과 원장 서버의 동시 허용치가 받쳐 준다면 `L_peak = 120` 이상으로.
2. **대기를 늘리지 않는다** — `maxWaitDuration`은 초 단위 피크를 흡수하지 못하고 스레드 점유만 늘립니다. 수십 ms 단위 순간 버스트용입니다.
3. **줄을 세울 곳을 바꾼다** — 푸시처럼 예고된 피크는 지급 요청을 큐에 적재하고 비동기로 소화하는 편이 낫습니다. 유입 자체를 고르게 하려면 RateLimiter([RateLimiter ②의 4-3](./10-ratelimiter-semaphore.md#4-3-ratelimiter-vs-bulkhead--혼동하지-않는-기준))를 앞에 둡니다.

---

## 5. 정리

| 질문 | 답 |
|---|---|
| Bulkhead가 막는 것 | 한 느린 의존성이 공유 스레드풀을 전부 잠식하는 상황 |
| CircuitBreaker와의 차이 | CB는 임계치 도달 후 차단. Bulkhead는 **첫 호출부터** 동시 실행 상한 |
| `SemaphoreBulkhead` 실행 스레드 | 호출 스레드 그대로. 컨텍스트 전파가 자연스러움 |
| `ThreadPoolBulkhead` 반환 타입 | `CompletionStage`. 동기 호출 체인에 그대로 끼울 수 없음 |
| `maxWaitDuration` 함정 | 블로킹 경로는 `toMillis()`로 **절삭**. 1ms 미만은 0이 됨 |
| v2.4.0 새 기능 | `Bulkhead#acquirePermissionAsync()` (커밋 7e3ab525, Issue #1592). `SemaphoreBulkhead`가 FIFO 대기 큐 + 공유 스케줄러로 논블로킹 구현 |
| 그 기본 구현 | `acquirePermission()`을 부르는 **블로킹 브리지**. 하위 호환용일 뿐 |
| Reactor / RxJava | Reactor 연산자는 논블로킹. **RxJava 2/3는 여전히 블로킹** |
| 큐가 차면 | `RejectedExecutionException` → `BulkheadFullException`을 **동기적으로 throw**. `CompletionStage`로 오지 않음 |
| `queueCapacity`를 크게 잡으면 | `ThreadPoolExecutor`는 큐가 꽉 차야 max까지 스레드를 늘린다 → 큐가 길면 스레드는 core에 고정되고 지연만 쌓임 |
| 큐 길이 상한 | `queueCapacity × 평균응답 / core ≤ 클라이언트 타임아웃` |
| `CallerRunsPolicy` | 호출 스레드에서 실행 = 격리 파괴. 쓰지 말 것 |
| 스레드 이름 | `bulkhead-<name>-N` (가상 스레드면 `bulkhead-<name>-v-N`). 덤프 분석의 핵심 |
| 끊기는 것 | MDC, `SecurityContextHolder`, 트랜잭션, 테넌트 ThreadLocal |
| 해법 | `ThreadPoolBulkheadConfig.contextPropagator(...)`. **트랜잭션은 전파 대상이 아님** — `SemaphoreBulkhead`를 쓸 것 |
| `maxConcurrentCalls` 산정 | `L = λW`로 열화 시 L을 구하고, 커넥션 풀 등 **가장 희소한 자원**과 min |
| permit 누수 | `decorate*` 없이 직접 획득했다면 `finally`에서 `onComplete()` 보장 |

---

## 6. ❓ 헷갈리는 포인트

- **Q. 벌크헤드랑 레이트 리미터는 뭐가 다른가요?** — RateLimiter는 **초당 몇 건 들어오나(λ)**를, Bulkhead는 **지금 동시에 몇 개 떠 있나(L)**를 셉니다. 하류가 느려지면 λ는 그대로인데 L이 폭증합니다(4-5 (C)). 느린 의존성에서 스레드를 지키는 건 Bulkhead의 일입니다.
- **Q. 세마포어 벌크헤드가 느린 호출을 중간에 끊어 주나요?** — 아닙니다. `SemaphoreBulkhead`는 **입구에서 permit을 나눠 줄 뿐**이고, 들어간 호출은 끝날 때까지(`onComplete()`) permit을 쥐고 있습니다. 끊는 건 HTTP 클라이언트 타임아웃이나 TimeLimiter의 몫입니다 — [TimeLimiter와 조합 순서](./12-timelimiter-and-composition.md).
- **Q. `maxWaitDuration=0`이면 자리가 있어도 거절되나요?** — 아닙니다. `tryAcquire(0, MILLISECONDS)`는 permit이 남아 있으면 즉시 성공하고, **없을 때만** 기다리지 않고 실패합니다.
- **Q. `ThreadPoolBulkhead`에 `maxThreadPoolSize`를 크게 주면 바쁠 때 스레드가 늘어나죠?** — 큐가 **가득 찬 뒤에만** 늘어납니다. 기본 큐 100이면 대부분 core에 머뭅니다(3.4, 4-5 (D)).
- **Q. 거절을 줄이려면 `maxConcurrentCalls`를 크게 잡으면 되나요?** — 상한을 커넥션 풀보다 크게 잡으면 대기가 Bulkhead 밖(커넥션 풀)으로 옮겨가 보이지 않게 될 뿐입니다. 처리량 상한은 `maxConcurrentCalls / W`이므로, 가장 희소한 자원 안에서 피크 L을 기준으로 정하세요(4-2, 4-7).

---

## 7. 🧾 한 장 요약

- Bulkhead는 **동시 실행 수(L)**에 칸을 쳐서, 느린 의존성 하나가 공용 스레드풀을 다 먹지 못하게 한다.
- `SemaphoreBulkhead` = 호출 스레드 그대로 + 세마포어 permit. 컨텍스트가 유지되고, 트랜잭션 경로에서는 이것만 쓴다.
- `ThreadPoolBulkhead` = 다른 스레드로 넘기고 `CompletionStage`를 받는다. MDC·인증·트랜잭션이 끊기고, 포화 시 `BulkheadFullException`이 `submit()`에서 **동기로** 던져진다.
- `maxWaitDuration=0`은 "자리가 있으면 즉시, 없으면 즉시 거절". 대기는 짧은 버스트용이고, 대기한 만큼 스레드를 잡는다.
- 리틀의 법칙 `L = λW`: 같은 유입에서 응답이 20배 느려지면 동시 실행도 20배. 상한은 열화 시 L과 커넥션 풀 등 **가장 희소한 자원**의 min으로.
- Bulkhead를 걸면 처리량 상한은 `maxConcurrentCalls / W`. 하류가 건강해도 피크 유입이 이를 넘으면 거절된다.
- `ThreadPoolExecutor`는 큐가 가득 차야 max까지 스레드를 늘린다. 큰 큐는 거절을 지연으로 바꿀 뿐이다.
- Bulkhead는 입구만 지킨다. 들어간 호출을 끊는 건 HTTP 타임아웃 · TimeLimiter다.

---

## 관련 문서
- 선행: [RateLimiter ② Semaphore 방식과 둘의 선택 기준](./10-ratelimiter-semaphore.md)
- 후행: [TimeLimiter와 조합 순서 — 데코레이터를 거꾸로 끼우면 안 되는 이유](./12-timelimiter-and-composition.md)

---
*Resilience4j v2.4.0 기준 · 소스 커밋 7e3ab525*

*마지막 업데이트: 2026년 10월*
