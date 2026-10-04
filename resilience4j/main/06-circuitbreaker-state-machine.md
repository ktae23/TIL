# CircuitBreaker 상태 머신 — 전이는 어떻게 일어나는가

`CircuitBreakerStateMachine` 은 6개 상태를 `AtomicReference` 하나로 관리하는 유한 상태 머신이다. 각 상태에서 무엇이 켜지고 꺼지는지, 전이가 어떤 원자적 연산으로 일어나는지, 여러 스레드가 동시에 임계값을 넘겼을 때 왜 전이가 한 번만 발생하는지를 소스로 확인한다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [🔢 숫자로 따라가기](#-숫자로-따라가기)
- [4. 실전 예제](#4-실전-예제)
- [🔥 시나리오](#-시나리오)
- [❓ 헷갈리는 포인트](#-헷갈리는-포인트)
- [5. 정리](#5-정리)
- [🧾 한 장 요약](#-한-장-요약)

---

## 1. 핵심 개념 (What)

`CircuitBreaker.State` enum 은 각 상태에 `order`(고정 정렬값)와 `allowPublish`(이벤트 발행 허용)를 준다 — `CLOSED(0, true)`, `OPEN(1, true)`, `HALF_OPEN(2, true)`, `DISABLED(3, false)`, `FORCED_OPEN(4, false)`, `METRICS_ONLY(5, true)`. `order` 주석이 "ordinal() 로도 되지만 enum 선언 순서를 바꾸면 값이 변하니 고정 정수를 쓴다"고 설명한다 — 지표 레이블로 노출되는 값이라 안정성이 필요했던 것이다.

| 상태 | 호출 허용 | 지표 기록 | 상태 전이 | 이벤트 발행 | 구현 클래스 |
|---|---|---|---|---|---|
| **CLOSED** | 전부 | O | O (→ OPEN) | O | `ClosedState` |
| **OPEN** | 전부 거부 | 거부 카운트 + 늦게 온 결과 | O (→ HALF_OPEN) | O | `OpenState` |
| **HALF_OPEN** | `permittedNumberOfCallsInHalfOpenState` 개만 | O | O (→ CLOSED/OPEN) | O | `HalfOpenState` |
| **DISABLED** | 전부 | **X (noOp)** | **X** | **X** (`allowPublish=false`) | `DisabledState` |
| **FORCED_OPEN** | 전부 거부 | 거부 카운트 | **X** | **X** | `ForcedOpenState` |
| **METRICS_ONLY** | 전부 | O | **X** | O (임계값 초과 포함) | `MetricsOnlyState` |

`DisabledState.onError()`/`onSuccess()` 는 진짜로 비어 있어서(`// noOp`) **지표가 멈춘다.** 반면 `MetricsOnlyState` 는 기록도 하고 임계값 초과 이벤트도 쏘지만 전이만 안 한다 — "새 서킷을 넣기 전에 실제로 열릴지 관찰하는" 모드다. 전이는 세 장치로 통제된다. ① 상태는 `AtomicReference<CircuitBreakerState> stateReference` 에 보관, ② 모든 전이는 `stateTransition(State, UnaryOperator<CircuitBreakerState>)` 단일 관문을 지남, ③ 미등록 전이는 `StateTransition.transitionBetween()` 이 `IllegalStateTransitionException` 으로 막는다.

---

## 2. 왜 알아야 하는가 (Why)

### 허용되지 않는 전이가 실제로 있다

`StateTransition` enum 에 없는 조합은 예외다. 그리고 빠진 조합이 있다.

- **`CLOSED → HALF_OPEN` 이 없다.** `CLOSED_TO_{CLOSED, OPEN, DISABLED, METRICS_ONLY, FORCED_OPEN}` 뿐이다.
- **`METRICS_ONLY → OPEN`, `METRICS_ONLY → HALF_OPEN` 이 없다.** `METRICS_ONLY_TO_{METRICS_ONLY, CLOSED, FORCED_OPEN, DISABLED}` 네 개뿐 — 관찰 모드에서 강제로 열려면 **FORCED_OPEN** 으로 가야 한다.
- 그래서 운영 API로 `/circuitbreaker/{name}/half-open` 을 만들어 두고 CLOSED 상태에서 호출했다가 500이 나는 사고가 생긴다.

### duration 은 어디로 가고, 왜 평균이 0으로 보이는가

데코레이터는 `StopWatch` 를 쓰지 않는다. `io.github.resilience4j.core.StopWatch` 클래스는 존재하지만 CircuitBreaker 경로에는 쓰이지 않고, `getCurrentTimestamp()` 로 직접 측정한다.

```java
circuitBreaker.acquirePermission();
final long start = circuitBreaker.getCurrentTimestamp();
try {
    T result = supplier.get();
    long duration = circuitBreaker.getCurrentTimestamp() - start;
    circuitBreaker.onResult(duration, circuitBreaker.getTimestampUnit(), result);
    return result;
} catch (Exception exception) {               // java.lang.Error 는 처리하지 않는다
    circuitBreaker.onError(circuitBreaker.getCurrentTimestamp() - start,
        circuitBreaker.getTimestampUnit(), exception);
    throw exception;
}
```

기본값은 `DEFAULT_TIMESTAMP_FUNCTION = clock -> System.nanoTime()`, `DEFAULT_TIMESTAMP_UNIT = NANOSECONDS` 다. 이 값의 쓰임이 두 갈래로 갈린다. **느린 호출 판정**은 `CircuitBreakerMetrics.onSuccess()` 에서 `durationUnit.toNanos(duration) > slowCallDurationThresholdInNanos` 로 나노 정밀도를 그대로 쓰고, **누적 duration**은 `AbstractAggregation.record()` 에서 `totalDurationInMillis += durationUnit.toMillis(duration)` — **밀리초로 내림된다.** 따라서 1ms 미만 호출은 누적 duration에 **0** 으로 들어간다. 평균 200µs 인 캐시 조회에서 `getMetrics().getAverageDuration()` 이 0ms 로 나오는 이유다. 느린 호출 판정은 정상이지만, 이 지표를 응답시간 모니터링으로 쓰면 틀린 그림을 본다. 레이턴시는 Micrometer 타이머로 따로 봐야 한다.

### `releasePermission()` 누락이 HALF_OPEN 을 영구 고착시킨다

허가 수는 `AtomicInteger` 하나이고 `releasePermission()` 만 복구시킨다. v2.4.0 에는 이 누수를 막는 방어 코드가 들어가 있다.

```java
/**
 * Evaluates a user-supplied predicate, making sure a permission that was previously acquired is
 * not leaked if the predicate throws. ... leaving the CircuitBreaker with a
 * permanently lost permit (most visibly wedging the HALF_OPEN state).
 */
private <T> boolean evaluatePredicate(Predicate<T> predicate, T value) {
    try {
        return predicate.test(value);
    } catch (Exception predicateException) {
        releasePermission();
        throw predicateException;
    }
}
```

`recordExceptionPredicate` 안의 NPE 하나로 HALF_OPEN 이 영구 고착될 수 있었다는 뜻이다. **predicate 는 절대 예외를 던지지 않게 작성하라**는 리뷰 포인트가 여기서 나온다.

---

## 3. 내부 구현 분석 (How)

### 3.1 전체 전이도

```mermaid
stateDiagram-v2
    [*] --> CLOSED: initialState (기본값 CLOSED)
    CLOSED --> OPEN: 임계값 초과 && isClosed.CAS(true,false)
    CLOSED --> OPEN: transitionOnResult → transitionToOpenAndWaitFor/Until
    OPEN --> HALF_OPEN: waitDuration 경과<br/>(자동: 스케줄러 / 수동: 다음 호출의 tryAcquirePermission)
    HALF_OPEN --> CLOSED: BELOW_THRESHOLDS && isHalfOpen.CAS(true,false)
    HALF_OPEN --> OPEN: 임계값 초과 && isHalfOpen.CAS(true,false)
    HALF_OPEN --> OPEN: maxWaitDurationInHalfOpenState 경과 (기본 OPEN)
    HALF_OPEN --> CLOSED: maxWaitDurationInHalfOpenState 경과 (=CLOSED 설정 시)
    CLOSED --> FORCED_OPEN: transitionToForcedOpenState()<br/>(OPEN, HALF_OPEN, METRICS_ONLY 에서도 가능)
    FORCED_OPEN --> CLOSED: transitionToClosedState() / reset()
    FORCED_OPEN --> HALF_OPEN: transitionToHalfOpenState()
    CLOSED --> DISABLED: transitionToDisabledState()<br/>(OPEN, HALF_OPEN, METRICS_ONLY 에서도 가능)
    DISABLED --> CLOSED: transitionToClosedState() / reset()
    CLOSED --> METRICS_ONLY: transitionToMetricsOnlyState()
    METRICS_ONLY --> CLOSED: transitionToClosedState()
    note right of METRICS_ONLY
        METRICS_ONLY → OPEN / HALF_OPEN, CLOSED → HALF_OPEN 은
        StateTransition 에 없음 → IllegalStateTransitionException
    end note
```

### 3.2 `stateTransition()` — 전이의 단일 관문

```java
private void stateTransition(State newState,
    UnaryOperator<CircuitBreakerState> newStateGenerator) {
    CircuitBreakerState previousState = stateReference.getAndUpdate(currentState -> {
        StateTransition.transitionBetween(getName(), currentState.getState(), newState);
        currentState.preTransitionHook();
        return newStateGenerator.apply(currentState);
    });
    publishStateTransitionEvent(
        StateTransition.transitionBetween(getName(), previousState.getState(), newState));
}
```

- **3행**: `getAndUpdate()` — CAS 실패 시 람다를 **재실행**한다.
- **4행**: 전이 유효성 검증. 반환값은 버리고 예외만 쓴다. 불법 전이면 예외가 `getAndUpdate` 밖으로 전파되고 **상태는 바뀌지 않는다.**
- **5~6행**: 떠나는 상태의 정리 훅(`OpenState` 는 예약된 HALF_OPEN 전이를, `HalfOpenState` 는 `maxWaitDuration` 타이머를 취소) 후 새 상태 객체 생성. `currentState.attempts()` 를 이어받아 백오프를 계산한다.
- **8~9행**: 같은 상태로의 전이(`CLOSED_TO_CLOSED` 등)는 `isInternalTransition()` 으로 걸러져 발행되지 않는다.

**리뷰 포인트:** 람다가 재실행될 수 있는데 그 안에 부수효과가 두 개 있다 — `preTransitionHook()` 과 새 상태 객체 생성. 특히 `OpenState` 생성자는 자동 전이가 켜져 있으면 **생성 시점에 스케줄러 작업을 등록한다**(`scheduledExecutorService.schedule(this::toHalfOpenState, waitDurationInMillis, MILLISECONDS)`). 따라서 두 스레드가 동시에 `transitionToOpenState()` 를 호출해 CAS 경쟁이 나면 버려진 `OpenState` 가 예약 작업을 남길 수 있다. 자동 전이의 진입점이 3.4절의 1회성 CAS로 보호되어 실무에서 잘 드러나지 않지만, **`transitionToOpenState()` 를 운영 API로 노출한다면 애플리케이션 레벨에서 직렬화**해두는 편이 안전하다.

### 3.3 호출 한 건의 생애

| 순서 | 호출 | 하는 일 |
|---|---|---|
| 1 | `acquirePermission()` / `tryAcquirePermission()` | 상태 객체에 위임. 거부 시 `CallNotPermittedException` 또는 `false` + `CallNotPermittedEvent` |
| 2~4 | `start = getCurrentTimestamp()` → 보호 대상 실행 → `duration = getCurrentTimestamp() - start` | 데코레이터가 직접 측정 |
| 5a | `onResult(duration, unit, result)` | `recordResultPredicate` 참이면 `state.onError()`, 아니면 `onSuccess()` + `handlePossibleTransition(Either.left(result))` |
| 5b | `onError(duration, unit, throwable)` | ignore → `releasePermission()` + 이벤트 후 return / record → `state.onError()` / 둘 다 아니면 `state.onSuccess()`, 마지막에 `handlePossibleTransition(Either.right(t))` |
| 6 | `CircuitBreakerMetrics` → `Result` | `ABOVE_THRESHOLDS` 등을 상태 객체에 반환 |
| 7 | 상태 객체의 `checkIfThresholdsExceeded()` | 1회성 CAS 후 `transitionToOpenState()` |

`tryAcquirePermission()` 은 `boolean` 을 돌려주고 거부 시 이벤트만 발행한다. `acquirePermission()` 은 예외를 잡아 이벤트를 발행한 뒤 다시 던진다. 전자가 필요한 이유는 **비동기/리액티브 경로**다 — 예외 대신 `promise.completeExceptionally()` 로 넘기려면 boolean 이 필요하다(`CircuitBreaker.decorateCompletionStage()` 참고). 각 상태의 `acquirePermission()` 구현은 대부분 `if (!tryAcquirePermission()) throw ...` 형태다.

#### 🖼 그림으로 보기 — 호출 한 건이 지나가는 길

```mermaid
sequenceDiagram
    participant App as 호출자
    participant Deco as decorateSupplier
    participant SM as CircuitBreakerStateMachine
    participant St as 현재 상태 객체
    participant M as CircuitBreakerMetrics
    App->>Deco: get()
    Deco->>SM: acquirePermission()
    SM->>St: acquirePermission()
    alt 거부 (OPEN / FORCED_OPEN / HALF_OPEN 허가 소진)
        St-->>SM: CallNotPermittedException
        SM-->>App: CallNotPermittedEvent 발행 후 예외 재throw (윈도 기록 없음)
    else 허가
        Deco->>Deco: start = getCurrentTimestamp()
        Deco->>Deco: supplier.get() 실행
        alt 정상 반환
            Deco->>SM: onResult(duration, unit, result)
        else 예외
            Deco->>SM: onError(duration, unit, e)
            SM->>SM: ignore 이면 releasePermission() 후 return
        end
        SM->>St: onSuccess() 또는 onError()
        St->>M: record → Snapshot
        M-->>St: Result (BELOW_MINIMUM_CALLS / BELOW / ABOVE ...)
        St->>St: checkIfThresholdsExceeded(result)
        opt 임계값 초과 && isClosed.CAS(true,false)
            St->>SM: transitionToOpenState()
        end
    end
```

### 3.4 OPEN → HALF_OPEN: 자동 전이 vs 지연 평가

`automaticTransitionFromOpenToHalfOpenEnabled` 기본값은 **false** 다. 끈 경우, 전이는 호출이 올 때 평가된다.

```java
@Override                                     // OpenState
public boolean tryAcquirePermission() {
    // Thread-safe
    if (clock.instant().isAfter(retryAfterWaitDuration)) {
        toHalfOpenState();
        // Check if the call is allowed to run in HALF_OPEN state after state transition
        boolean callPermitted = stateReference.get().tryAcquirePermission();
        if (!callPermitted) {
            publishCallNotPermittedEvent();
            circuitBreakerMetrics.onCallNotPermitted();
        }
        return callPermitted;
    }
    circuitBreakerMetrics.onCallNotPermitted();
    return false;
}
```

**호출이 와야 전이가 평가된다.** 트래픽이 0이면 OPEN 에 무한정 머문다 — "OPEN 상태가 설정한 60초보다 훨씬 길게 유지됨"은 버그가 아니라 설계다. 켠 경우에는 `SchedulerFactory` 싱글톤이 관리하는 **단일 스레드 스케줄러를 JVM 전체가 공유한다**(`newSingleThreadScheduledExecutor("CircuitBreakerAutoTransitionThread", ...)`). 전이 작업 자체는 가볍지만 `transitionToHalfOpenState()` → 이벤트 컨슈머 실행까지 이 스레드에서 돈다. **컨슈머에서 블로킹 I/O(알림 전송 등)를 하면 다른 모든 서킷의 자동 전이가 지연된다.**

자동/수동 양쪽은 같은 진입점을 공유하며 `ReentrantLock` + 1회성 CAS 로 보호된다 — `toHalfOpenState()` 안에서 `if (isOpen.compareAndSet(true, false)) transitionToHalfOpenState();`.

#### 🖼 그림으로 보기 — "20초 뒤 HALF_OPEN" 은 누가 실행하나

```mermaid
sequenceDiagram
    participant U as 사용자 요청
    participant O as OpenState
    participant S as 공유 스케줄러 스레드
    Note over O: t=0 OPEN 진입, retryAfterWaitDuration = t+20s
    alt automaticTransition = false (기본)
        U->>O: t=5s tryAcquirePermission()
        O-->>U: false (CallNotPermittedException)
        Note over O: t=20s~ 아무도 안 오면 계속 OPEN
        U->>O: t=95s tryAcquirePermission()
        O->>O: clock.instant().isAfter(t+20s) → toHalfOpenState()
        O-->>U: HALF_OPEN 허가 1개 차감 후 true → 이 요청이 첫 시험 호출
    else automaticTransition = true
        O->>S: 생성자에서 schedule(toHalfOpenState, 20s)
        S->>O: t=20s toHalfOpenState() → HALF_OPEN
        Note over S: 이벤트 컨슈머도 이 스레드에서 실행 → 블로킹 금지
    end
```

### 3.5 동시성: 1회성 `AtomicBoolean`

CLOSED 상태의 핵심.

```java
private void checkIfThresholdsExceeded(Result result) {
    if (Result.hasExceededThresholds(result) && isClosed.compareAndSet(true, false)) {
        publishCircuitThresholdsExceededEvent(result, circuitBreakerMetrics);
        transitionToOpenState();
    }
}
```

100개 스레드가 동시에 `ABOVE_THRESHOLDS` 를 받아도 `isClosed.compareAndSet(true, false)` 에 성공하는 스레드는 **정확히 하나**다. 전이도, 임계값 초과 이벤트도 1회만 발생한다. 같은 패턴이 모든 전이 지점에 복제돼 있다.

| 상태 | 플래그 | 보호 대상 |
|---|---|---|
| `ClosedState` | `isClosed` | CLOSED → OPEN 1회 |
| `OpenState` | `isOpen` | OPEN → HALF_OPEN 1회 (스케줄러 vs 사용자 스레드) |
| `HalfOpenState` | `isHalfOpen` | HALF_OPEN → OPEN/CLOSED 1회, 타이머 전이와의 경쟁 |
| `MetricsOnlyState` | `isFailureRateExceeded`, `isSlowCallRateExceeded` | 임계값 초과 이벤트를 종류별 1회 |

`MetricsOnlyState` 가 플래그를 둘로 나눈 이유는 전이가 없어 상태 객체가 교체되지 않기 때문이다 — 하나면 실패율 이벤트가 나간 뒤 느린호출율 이벤트가 영구히 막힌다.

**비대칭 주의:** `ClosedState.tryAcquirePermission()` 은 `isClosed.get()` 을 반환하지만 `acquirePermission()` 은 `// noOp` 이다. 정상 흐름에서는 `isClosed=false` 직후 상태 객체가 교체되므로 창이 짧다. 그런데 `ClosedState.handlePossibleTransition()` 은 `isClosed` 를 false 로 바꾼 **뒤에** `waitDuration`/`waitUntil` 이 둘 다 null 이면 `IllegalArgumentException` 을 던진다. 즉 `TransitionCheckResult.transitionToOpen()`(둘 다 null)을 쓰면 **상태는 CLOSED 인데 `isClosed=false` 로 남는다.** javadoc 은 "`transitionToOpenState()` 를 호출한다"고 적었지만 구현은 그렇지 않다 — **`transitionToOpenAndWaitFor(Duration)` 또는 `transitionToOpenAndWaitUntil(Instant)` 를 써야 한다**(상세: [07번 문서](./07-circuitbreaker-config.md) 3.5절).

### 3.6 HALF_OPEN 의 허가 관리

```java
@Override
public boolean tryAcquirePermission() {
    if (permittedNumberOfCalls.getAndUpdate(current -> current == 0 ? current : --current)
        > 0) {
        return true;
    }
    circuitBreakerMetrics.onCallNotPermitted();
    return false;
}
```

"0이면 그대로, 아니면 1 감소"를 원자적으로 수행하고 **감소 전 값**으로 판정한다. 음수로 내려가지 않으므로 초과 호출은 전부 거부되고 `onCallNotPermitted()` 로 집계된다. 호출자는 OPEN 때와 동일하게 `CallNotPermittedException` 을 받지만 메시지에 현재 상태가 들어가므로 구분은 가능하다. 복구는 `releasePermission()`(`incrementAndGet()`)뿐이다. 판정은 CLOSED와 달리 **양방향**이다 — `HalfOpenState.checkIfThresholdsExceeded()` 는 임계값 초과면 `isHalfOpen` CAS 후 `transitionToOpenState()`, `result == BELOW_THRESHOLDS` 면 같은 CAS 후 `transitionToClosedState()` 를 호출한다. `BELOW_MINIMUM_CALLS_THRESHOLD` 는 두 조건 모두에 안 걸린다 — **허가를 다 쓸 때까지 HALF_OPEN 에 머문다.** HALF_OPEN 지표는 항상 COUNT_BASED 이고 윈도 크기 = `permittedNumberOfCallsInHalfOpenState` 이므로(`CircuitBreakerMetrics.forHalfOpen()`) 최소 호출 수도 `min(minimumNumberOfCalls, permitted)` 로 축소된다. 그래도 교착이 가능한 경우의 탈출구가 `maxWaitDurationInHalfOpenState`(기본 0 = 비활성)다.

#### 🖼 그림으로 보기 — HALF_OPEN 시험 호출 흐름

```mermaid
flowchart TD
    REQ["요청 도착 (HALF_OPEN)"] --> P{"permittedNumberOfCalls<br/>getAndUpdate: 0이면 그대로, 아니면 -1<br/>감소 전 값 > 0 ?"}
    P -->|"아니오"| REJ["거부: CallNotPermittedException<br/>onCallNotPermitted() — 윈도 기록 없음"]
    P -->|"예"| RUN["시험 호출 실행"]
    RUN --> KIND{"결과 분류"}
    KIND -->|"ignore 예외"| REL["releasePermission()<br/>허가 +1 반환, 윈도 기록 없음"]
    KIND -->|"성공 / 실패 기록"| REC["HALF_OPEN 전용 윈도에 기록<br/>(COUNT_BASED, size = permitted)"]
    REC --> R{"Result"}
    R -->|"BELOW_MINIMUM_CALLS_THRESHOLD<br/>(기록 수 < min(minimumNumberOfCalls, permitted))"| WAIT["HALF_OPEN 유지<br/>남은 시험 호출을 기다림"]
    R -->|"BELOW_THRESHOLDS"| CL["isHalfOpen CAS → CLOSED<br/>(새 ClosedState, 새 윈도)"]
    R -->|"임계값 초과"| OP["isHalfOpen CAS → OPEN<br/>(attempts + 1)"]
    WAIT -.->|"maxWaitDurationInHalfOpenState 경과 (설정 시)"| TO["transitionToStateAfterWaitDuration<br/>(기본 OPEN)"]
```


## 🔢 숫자로 따라가기

설정: `slidingWindow(10, 5, COUNT_BASED)`, `failureRateThreshold=50`, `waitDurationInOpenState=20s`, `permittedNumberOfCallsInHalfOpenState=3`, `automaticTransitionFromOpenToHalfOpenEnabled=false`(기본). 느린 호출은 없다고 가정한다(기본 `slowCallDurationThreshold=60s`).

```mermaid
stateDiagram-v2
    [*] --> CLOSED
    CLOSED --> OPEN: 6번째 호출 후 실패율 3/6 = 50% (>= 50)
    OPEN --> HALF_OPEN: 20초 경과 후 첫 요청 (t=00:26)
    HALF_OPEN --> CLOSED: 시험 3건 실패율 0/3 = 0% (케이스 A)
    HALF_OPEN --> OPEN: 시험 3건 실패율 2/3 = 66.7% (케이스 B)
    note right of HALF_OPEN
        전용 윈도: COUNT_BASED size 3
        최소 호출 수 = min(5, 3) = 3
    end note
```

### CLOSED → OPEN

판정은 기록 **직후마다** 일어난다(`ClosedState.onError/onSuccess` → `checkIfThresholdsExceeded`). 실패율 `>=` 비교다(`CircuitBreakerMetrics` 148~157행).

| 시각 | # | 결과 | 윈도 (호출/실패) | 실패율 | `Result` | 상태 |
|---|---|---|---|---|---|---|
| 00:00 | 1 | S | 1/0 | - | BELOW_MINIMUM_CALLS_THRESHOLD | CLOSED |
| 00:01 | 2 | S | 2/0 | - | BELOW_MINIMUM_CALLS_THRESHOLD | CLOSED |
| 00:02 | 3 | F | 3/1 | - | BELOW_MINIMUM_CALLS_THRESHOLD | CLOSED |
| 00:03 | 4 | F | 4/2 | (산술 50%) | **BELOW_MINIMUM_CALLS_THRESHOLD** — 4 < 5 라서 계산 자체를 안 함 | CLOSED |
| 00:04 | 5 | S | 5/2 | 40% | BELOW_THRESHOLDS | CLOSED |
| 00:05 | 6 | F | 6/3 | **50%** | FAILURE_RATE_ABOVE_THRESHOLDS | **→ OPEN** (`attempts=1`, `retryAfterWaitDuration = 00:25`) |

### OPEN 체류

| 시각 | # | 일어난 일 |
|---|---|---|
| 00:05.3 | (6번 직전에 시작된 호출) | 늦게 끝난 결과는 `OpenState.onError/onSuccess` 가 **CLOSED 때 윈도**(그대로 물려받은 metrics)에 기록한다. 전이는 없다 |
| 00:10 | 7 | `isAfter(00:25)` 거짓 → `CallNotPermittedException`. `numberOfNotPermittedCalls` 만 +1, 실패율 분모에는 안 들어감 |
| 00:25.000 | 8 | 같은 Instant 면 `isAfter` 가 **거짓**(엄격한 비교) → 거부 |
| 00:26 | 9 | `isAfter` 참 → `toHalfOpenState()` → `HalfOpenState(attempts=1)` 생성, 허가 3 → **2**, 9번이 첫 시험 호출로 실행 |

자동 전이를 켜지 않았으므로 00:25~00:26 사이에 요청이 없었다면 전이도 없었다. 9번이 01:30 에 왔다면 HALF_OPEN 전이도 01:30 이다.

### HALF_OPEN 판정

00:26 에 요청 9~12가 거의 동시에 들어왔다.

| # | 허가 (감소 전 → 후) | 결과 |
|---|---|---|
| 9 | 3 → 2 | 실행 |
| 10 | 2 → 1 | 실행 |
| 11 | 1 → 0 | 실행 |
| 12 | 0 → 0 | **거부** (`CallNotPermittedException`, 메시지에 HALF_OPEN 표시) |

| 케이스 | 9 | 10 | 11 | 기록 2건 시점 | 3건 시점 | 결과 |
|---|---|---|---|---|---|---|
| A | S | S | S | 2/0 → BELOW_MINIMUM (2 < 3) | 3/0 = 0% → BELOW_THRESHOLDS | **CLOSED** — 새 `ClosedState`, 윈도 0건부터 다시 |
| B | S | F | F | 2/1 → BELOW_MINIMUM | 3/2 = 66.7% → 초과 | **OPEN** — `attempts=2`, 다시 20초 대기 |
| C | F | F | S | 2/2 → **여전히 BELOW_MINIMUM** | 3/2 = 66.7% | **OPEN** — 2건이 다 실패여도 3건째까지 기다린다 |
| D | S | F | S | 2/1 → BELOW_MINIMUM | 3/1 = 33.3% | **CLOSED** |

케이스 B 의 `attempts=2` 는 `transitionToOpenState()` 가 `new OpenState(currentState.attempts() + 1, ...)` 를 만들기 때문이다. `waitDurationInOpenState(20s)` 는 `IntervalFunction.of(20000)` 이라 attempts 와 무관하게 늘 20초지만, `waitIntervalFunctionInOpenState` 에 지수 백오프를 넣으면 이 값이 커진다([07번](./07-circuitbreaker-config.md) 3.6절).

---

## 4. 실전 예제

### 4.1 상태 전이를 로그와 지표로 남기기

```java
package com.example.resilience.observability;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 모든 CircuitBreaker 의 상태 전이를 구조화 로그 + 카운터로 남긴다.
 * - onEntryAdded/onEntryReplaced 를 함께 구독한다. 서킷은 lazy 생성이 기본이라 기동 시점
 *   순회만으로는 나중에 만들어진 것을 놓친다.
 * - 컨슈머는 절대 블로킹하지 않는다. automaticTransitionFromOpenToHalfOpen 이 켜져 있으면
 *   이 컨슈머가 JVM 공유 단일 스레드 스케줄러에서 실행될 수 있다(3.4절).
 */
@Component
public class CircuitBreakerTransitionRecorder {

    private static final Logger log =
        LoggerFactory.getLogger(CircuitBreakerTransitionRecorder.class);

    private final CircuitBreakerRegistry registry;
    private final MeterRegistry meterRegistry;

    public CircuitBreakerTransitionRecorder(CircuitBreakerRegistry registry,
                                            MeterRegistry meterRegistry) {
        this.registry = registry;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void init() {
        registry.getAllCircuitBreakers().forEach(this::subscribe);
        registry.getEventPublisher()
            .onEntryAdded(event -> subscribe(event.getAddedEntry()))
            .onEntryReplaced(event -> subscribe(event.getNewEntry()));
    }

    private void subscribe(CircuitBreaker cb) {
        cb.getEventPublisher().onStateTransition(this::record);
    }

    private void record(CircuitBreakerOnStateTransitionEvent event) {
        CircuitBreaker.State from = event.getStateTransition().getFromState();
        CircuitBreaker.State to = event.getStateTransition().getToState();
        String msg = "circuit_transition name={} from={} to={}";

        // OPEN 진입은 장애 신호다. WARN 으로 올려 알림 룰에 걸리게 한다.
        if (to == CircuitBreaker.State.OPEN || to == CircuitBreaker.State.FORCED_OPEN) {
            log.warn(msg, event.getCircuitBreakerName(), from, to);
        } else {
            log.info(msg, event.getCircuitBreakerName(), from, to);
        }
        Counter.builder("app.circuitbreaker.transition")
            .tag("name", event.getCircuitBreakerName())
            .tag("from", from.name()).tag("to", to.name())
            .register(meterRegistry)
            .increment();
    }
}
```

### 4.2 수동 전이를 운영 스위치로 쓰기 (배포 중 특정 의존성 차단)

3.1절의 불법 전이와 3.2절의 동시 호출 문제를 둘 다 막는 운영 레버다.

```java
package com.example.resilience.admin;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.IllegalStateTransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * 설계 근거:
 * - stateTransition() 의 getAndUpdate 람다는 CAS 실패 시 재실행되며 부수효과를 포함한다.
 *   운영 API 는 호출 빈도가 낮으니 이름별 락으로 직렬화해 경쟁 자체를 없애는 편이 싸다.
 * - FORCED_OPEN 은 어느 상태에서든 진입 가능하다. 반대로 HALF_OPEN 진입은
 *   CLOSED/METRICS_ONLY 에서 불법이므로 아예 제공하지 않는다.
 */
@Service
public class CircuitBreakerOpsSwitch {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerOpsSwitch.class);

    private final CircuitBreakerRegistry registry;
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public CircuitBreakerOpsSwitch(CircuitBreakerRegistry registry) {
        this.registry = registry;
    }

    /** 점검 중 전면 차단(FORCED_OPEN: 지표·이벤트 정지). 자동 복구되지 않는다. */
    public void block(String name, String reason) {
        apply(name, "block", reason, CircuitBreaker::transitionToForcedOpenState);
    }

    /** 차단 해제. CLOSED 로 복귀하며 지표 윈도는 새로 시작된다. */
    public void unblock(String name) {
        apply(name, "unblock", "-", CircuitBreaker::transitionToClosedState);
    }

    /** 오탐으로 서비스가 막힐 때의 비상 탈출구(DISABLED: 전부 통과, 지표 정지). */
    public void bypass(String name, String reason) {
        apply(name, "bypass", reason, CircuitBreaker::transitionToDisabledState);
    }

    private void apply(String name, String op, String reason, Consumer<CircuitBreaker> action) {
        CircuitBreaker cb = registry.circuitBreaker(name);
        ReentrantLock lock = locks.computeIfAbsent(name, k -> new ReentrantLock());
        lock.lock();
        try {
            log.warn("ops_circuit_{} name={} previousState={} reason={}",
                op, name, cb.getState(), reason);
            action.accept(cb);
        } catch (IllegalStateTransitionException e) {
            log.error("ops_circuit_illegal_transition name={} from={} to={}",   // 상태는 그대로다
                e.getName(), e.getFromState(), e.getToState());
            throw e;
        } finally {
            lock.unlock();
        }
    }
}
```

운영 체크리스트: `block()` → `unblock()` 쌍을 런북에 기록한다(FORCED_OPEN 은 시간 기반 탈출 경로가 없다). `bypass()`(DISABLED)는 지표가 멈춰 대시보드가 "평온해 보이는" 착시를 만들므로 DISABLED 상태 자체를 감지하는 알림 룰이 필요하다. 전이 이벤트는 발행되지만 대상이 DISABLED/FORCED_OPEN 이면 그 **이후** 이벤트는 `allowPublish=false` 로 막힌다.

---

## 🔥 시나리오

### 시나리오 A — 결제 승인 API, 외부 승인 서버가 배포 실수로 500을 뿜는다

> 결제 승인 API 가 외부 승인 서버를 호출한다. 평소 20 RPS, 정상 응답 150ms. 설정은 위 "숫자로 따라가기"와 비슷하게 `COUNT_BASED 20 / min 10 / 실패율 50% / OPEN 대기 30s / HALF_OPEN 시험 5건`, 자동 전이 꺼짐.

| 시각 | 사건 | 서킷 |
|---|---|---|
| 14:00:00 | 승인 서버 새 버전 배포. 요청의 80% 가 즉시 500 응답 | CLOSED |
| ≈ 14:00:00.65 | 배포 후 13번째 기록 시점에 최근 20건 중 실패가 10건에 도달 (새 기록의 80% 가 실패 → 0.8 × 13 ≈ 10) | **OPEN** — 이후 요청은 수 µs 만에 `CallNotPermittedException` → 폴백("잠시 후 다시 시도해 주세요") |
| 14:00:30.0 | 대기 만료. 하지만 자동 전이가 꺼져 있어 **다음 요청이 올 때까지** OPEN | OPEN |
| 14:00:30.05 | 첫 요청이 HALF_OPEN 전이를 일으키고 시험 호출 1번이 된다. 같은 순간 들어온 4건이 2~5번 시험 호출 | HALF_OPEN |
| 14:00:30.2 | 5건 중 4건 실패 (아직 롤백 전) → 80% | **OPEN** (`attempts=2`) |
| 14:01:00 ~ 14:02:30 | 30초마다 같은 일이 반복된다: 대기 만료 → 첫 요청이 HALF_OPEN → 시험 5건 대부분 실패 → OPEN (`attempts` 3, 4, …) | OPEN ↔ HALF_OPEN |
| 14:03:00 | 승인 서버 롤백 완료 | OPEN (마지막 대기 중) |
| 다음 대기 만료 후 | 첫 요청부터 시험 호출. 5건 모두 성공 → 0% | **CLOSED**, 새 윈도 · `attempts` 0 |

포인트: OPEN 동안 승인 서버는 30초에 시험 호출 5건 정도만 받아 **요청을 거의 받지 않는다.** 롤백 작업이 방해받지 않고, 호출자도 150ms 가 아니라 즉시 폴백을 받는다. 서킷이 없었다면 3분 동안 20 RPS × 80% = 약 2,880건이 그대로 500을 맞았다.

### 시나리오 B — 새벽에는 서킷이 "안 닫힌다"

같은 결제 승인 API 가 새벽 2시에 장애로 OPEN 됐고 2시 10분에 승인 서버는 정상화됐다. 그런데 새벽 트래픽은 10분에 1건이다.

- `automaticTransitionFromOpenToHalfOpenEnabled=false`(기본): 2시 10분 이후 첫 요청(예: 2시 17분)이 와야 HALF_OPEN 으로 바뀐다. 그 요청이 **시험 호출 1번**이 되고, 판정에는 `min(10, 5) = 5`건이 필요하다 — 10분에 1건이면 5번째 요청(약 2시 57분)까지 HALF_OPEN 에 머문다(`BELOW_MINIMUM_CALLS_THRESHOLD`).
- 대시보드에는 "2시~3시 서킷 비정상"으로 보이지만 실제 장애는 10분이었다.
- 해법: 저트래픽 경로는 `permittedNumberOfCallsInHalfOpenState` 를 1~3으로 낮추고, `maxWaitDurationInHalfOpenState` + `transitionToStateAfterWaitDuration` 로 HALF_OPEN 체류 상한과 그 뒤 갈 상태(기본 OPEN, 또는 CLOSED)를 정해 두거나, 자동 전이를 켜서 상태 표시라도 정확하게 만든다(자동 전이는 시험 호출 수를 줄여주지는 않는다).

---

## ❓ 헷갈리는 포인트

- **Q. HALF_OPEN 시험 호출 중 ignore 된 예외는 허가(permit)를 소모하나?**
  소모하지 않는다. `handleThrowable()` 은 ignore predicate 가 참이면 `releasePermission()` 을 부르고 바로 return 한다(`CircuitBreakerStateMachine` 206~212행). `HalfOpenState.releasePermission()` 은 `permittedNumberOfCalls.incrementAndGet()` 이므로 허가가 돌아오고, 윈도에도 기록되지 않는다. 그래서 **ignore 예외만 계속 나면 HALF_OPEN 은 판정 없이 머문다.**
- **Q. OPEN 에서 거부된 호출도 실패율을 올리나?**
  아니다. 거부는 `circuitBreakerMetrics.onCallNotPermitted()` 로 별도 카운터(`numberOfNotPermittedCalls`)만 올린다. 슬라이딩 윈도에는 들어가지 않는다.
- **Q. HALF_OPEN → CLOSED 가 되면 OPEN 직전의 실패 기록이 남아 있나?**
  안 남는다. `transitionToClosedState()` 는 `new ClosedState()` 를 만들고, 그 생성자가 `CircuitBreakerMetrics.forClosed(...)` 로 **빈 윈도**를 새로 만든다. `ClosedState.attempts()` 도 0이라 OPEN 대기 백오프도 초기화된다.
- **Q. `waitDurationInOpenState` 가 지나면 자동으로 HALF_OPEN 이 되나?**
  기본값(`automaticTransitionFromOpenToHalfOpenEnabled=false`)에서는 **아니다.** 다음 `tryAcquirePermission()` 이 `clock.instant().isAfter(retryAfterWaitDuration)` 를 확인할 때 전이된다. `getState()` 를 찍어보면 대기 시간이 지나도 OPEN 이 보이는 이유다.
- **Q. `permittedNumberOfCallsInHalfOpenState=10` 이고 `minimumNumberOfCalls=5` 면 5건만 보고 판정하나?**
  그렇다. HALF_OPEN 윈도의 최소 호출 수는 `min(minimumNumberOfCalls, permitted)` = 5 다(`CircuitBreakerMetrics.forHalfOpen` → 생성자 45~51행). 5건째 기록에서 판정이 나면 나머지 허가 5개는 쓰이지 않은 채 상태가 바뀐다.

---

## 5. 정리

| 상태 | 호출 | 지표 | 전이 | 이벤트 | 탈출 |
|---|---|---|---|---|---|
| CLOSED | 허용 | 기록 | → OPEN | O | 임계값 초과 시 자동 |
| OPEN | 거부 | 거부 카운트 + 늦게 온 결과 | → HALF_OPEN | O | `waitDuration` 후 다음 호출 또는 스케줄러 |
| HALF_OPEN | N개만 | 기록 | → CLOSED/OPEN | O | 허가 소진 후 판정 또는 `maxWaitDurationInHalfOpenState` |
| DISABLED / FORCED_OPEN | 허용 / 거부 | 없음 / 거부 카운트 | **없음** | **없음** | 수동 전이만 |
| METRICS_ONLY | 허용 | 기록 | **없음** | O | 수동 전이만 (OPEN/HALF_OPEN 은 불법) |

| 메커니즘 | 코드 | 역할 |
|---|---|---|
| 상태 보관 / 전이 관문 | `AtomicReference<CircuitBreakerState>` + `stateTransition()` | 상태 객체 통째 교체, 검증 + preTransitionHook + 이벤트 |
| 전이 검증 | `StateTransition.transitionBetween()` | 미등록 조합은 `IllegalStateTransitionException` |
| 중복 전이 방지 | 상태별 `AtomicBoolean` 1회성 CAS | 100스레드 동시 초과 → 전이 1회 |
| HALF_OPEN 허가 | `AtomicInteger.getAndUpdate(c -> c == 0 ? c : --c)` | 음수 방지 + 원자적 감소 |
| 자동 OPEN→HALF_OPEN | `SchedulerFactory` 싱글톤 단일 스레드 | JVM 전역 공유. 컨슈머 블로킹 금지 |
| duration 측정 | `getCurrentTimestamp()` (기본 `System.nanoTime()`) | 느린호출 판정은 나노, 누적은 ms 내림 |

리뷰에서 바로 지적할 것들:

1. **`automaticTransitionFromOpenToHalfOpenEnabled` 기본값은 false.** 트래픽이 없으면 OPEN 에서 안 나온다.
2. **`transitionOnResult` 에 `transitionToOpen()` 금지.** `transitionToOpenAndWaitFor(...)` 를 써야 한다.
3. **predicate 에서 예외를 던지지 말 것.** 허가 누수는 막아주지만 예외는 호출자에게 전파된다.
4. **CLOSED/METRICS_ONLY → HALF_OPEN 운영 API 는 만들 수 없다.** 그리고 **자동 전이 + 블로킹 이벤트 컨슈머 = 전역 장애 요인**이다.

---


## 🧾 한 장 요약

- 상태는 `AtomicReference` 안의 **상태 객체 하나**이고, 전이는 그 객체를 통째로 바꾸는 것이다(`stateTransition()` 단일 관문).
- 자동 전이 루프는 **CLOSED → OPEN → HALF_OPEN → CLOSED/OPEN** 뿐이다. DISABLED · FORCED_OPEN · METRICS_ONLY 는 수동 전이로만 드나든다.
- CLOSED 판정은 기록할 때마다: `minimumNumberOfCalls` 미만이면 계산 안 함 → 이상이면 실패율/느린호출율 `>=` 임계값이면 OPEN.
- OPEN 거부는 윈도에 안 들어간다. 대기 만료 후 **다음 요청**이 HALF_OPEN 을 연다(자동 전이를 켜지 않은 한).
- HALF_OPEN 은 허가 N개 + 전용 윈도(size N, 최소 `min(minimumNumberOfCalls, N)`)로 **양방향 판정**한다. ignore 예외는 허가를 돌려준다.
- 동시 임계값 초과에도 전이는 **1번**: 상태별 1회성 `AtomicBoolean` CAS.
- CLOSED 로 돌아오면 윈도와 `attempts` 가 초기화된다. 복구 실패가 반복될 때만 OPEN 대기가 길어진다.

---

## 관련 문서
- 선행: [슬라이딩 윈도 ② 시간 기반과 lock-free 구현](./05-sliding-window-time.md)
- 후행: [CircuitBreakerConfig 전 항목 — 숫자를 어떻게 정하나](./07-circuitbreaker-config.md)
- 참고: [EventProcessor 와 이벤트 발행](./03-event-processor.md), [Actuator 와 헬스 인디케이터](../advanced/05-actuator-and-health.md)

---
*Resilience4j v2.4.0 기준 · 소스 커밋 7e3ab525*

*마지막 업데이트: 2026년 10월*
