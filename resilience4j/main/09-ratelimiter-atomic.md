# RateLimiter ① AtomicRateLimiter — 락 없이 나노초로 센다

`AtomicRateLimiter`는 토큰을 실제로 저장하지 않습니다. "지금이 몇 번째 사이클의 몇 나노초인지"만 계산해서 허용 여부를 결정하고, 불변 `State` 객체를 `AtomicReference`로 교체하는 CAS 루프로 동시성을 처리합니다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
  - [🖼 그림으로 보기 — 시간축 위의 사이클과 예약](#-그림으로-보기--시간축-위의-사이클과-예약)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
  - [3.9 🖼 `acquirePermission()` 한 번의 전체 경로](#39--그림으로-보기--acquirepermission-한-번의-전체-경로)
  - [3.10 🔢 숫자로 따라가기](#310--숫자로-따라가기--limitforperiod2-limitrefreshperiod1s)
- [4. 실전 예제](#4-실전-예제)
  - [4-6. 🔥 시나리오 — 기본 timeoutDuration(5s)가 웹 서버를 멈춘 날](#4-6--시나리오--기본-timeoutduration5s가-웹-서버를-멈춘-날)
- [5. 정리](#5-정리)
- [6. ❓ 헷갈리는 포인트](#6--헷갈리는-포인트)
- [7. 🧾 한 장 요약](#7--한-장-요약)

---

## 1. 핵심 개념 (What)

토큰 버킷(token bucket)을 구현하라고 하면 대부분 이렇게 씁니다. 버킷 객체를 만들고, 스케줄러가 주기마다 토큰을 채우고, 요청이 오면 하나 꺼낸다. `AtomicRateLimiter`는 그 중 **채우는 과정을 아예 없앱니다.**

`AtomicRateLimiter`의 전체 상태는 숫자 3개입니다. `AtomicRateLimiter.State`를 보세요.

```java
private static class State {

    private final RateLimiterConfig config;

    private final long activeCycle;
    private final int activePermissions;
    private final long nanosToWait;
    ...
}
```

- `activeCycle` — 마지막으로 계산에 쓰인 사이클 번호
- `activePermissions` — 그 시점의 남은 허용량. **음수가 될 수 있습니다**
- `nanosToWait` — 마지막 호출자가 기다려야 했던 나노초

사이클(cycle)은 `nanoTimeStart` 이후의 경과 시간을 `limitRefreshPeriod`로 나눈 몫입니다. `limitRefreshPeriod=1s`면 1초마다 사이클 번호가 1 증가하고, 사이클이 바뀐 것을 발견하면 "그 사이에 몇 사이클 지났는지 × `limitForPeriod`"만큼을 한 번에 더해버립니다. 스케줄러도, 백그라운드 스레드도 없습니다. 아무도 호출하지 않는 동안에는 아무 일도 일어나지 않고, 다음 호출자가 계산으로 따라잡습니다.

클래스 Javadoc이 그 설명을 직접 하고 있습니다.

> By contract on start of each cycle `AtomicRateLimiter` should set `State#activePermissions` to `RateLimiterConfig#getLimitForPeriod`. For the `AtomicRateLimiter` callers it is really looks so, but under the hood there is some optimisations that will skip this refresh if `AtomicRateLimiter` is not used actively.

### 설정값 (`RateLimiterConfig`)

| 옵션 | 기본값 | 의미 |
|---|---|---|
| `limitForPeriod` | `50` | 한 주기에 허용할 호출 수. 1 미만이면 `IllegalArgumentException` |
| `limitRefreshPeriod` | `Duration.ofNanos(500)` | 주기 길이. 1ns 미만이면 `IllegalArgumentException` |
| `timeoutDuration` | `Duration.ofSeconds(5)` | 허용량이 없을 때 **기다려줄** 최대 시간. 음수 금지 |
| `drainPermissionsOnResult` | `any -> false` | 호출 결과를 보고 남은 허용량을 전부 버릴지 결정하는 `Predicate<Either<? extends Throwable, ?>>` |
| `writableStackTraceEnabled` | `true` | `false`면 `RequestNotPermitted`의 스택트레이스가 빈 배열 |

기본값 조합을 계산해 보세요. 500ns에 50개면 **초당 1억 회**입니다. `RateLimiterConfig.ofDefaults()`는 사실상 제한이 없는 설정입니다. 프로덕션에서 `limitForPeriod`만 바꾸고 `limitRefreshPeriod`를 그대로 두면 아무것도 제한하지 않습니다. 코드 리뷰에서 이 둘은 항상 같이 보세요.

### 음수 permission = 예약

`timeoutDuration`이 0보다 크면 `AtomicRateLimiter`는 허용량을 **미리 당겨씁니다.** 지금 남은 게 0인데 다음 사이클까지 300ms 남았고 `timeoutDuration`이 1초라면, `activePermissions`를 `-1`로 만들고 호출자에게 "300ms 기다려"라고 알려줍니다. 호출자는 그 시간 동안 `parkNanos`로 자고, 깨면 그냥 진행합니다. 다시 확인하지 않습니다. 자리를 **이미 예약해 두었기** 때문입니다.

이게 `AtomicRateLimiter`가 "대기 가능한(waiting) rate limiter"가 되는 방식이고, `SemaphoreBasedRateLimiter`가 `reservePermission()`에서 `UnsupportedOperationException`을 던지는 이유입니다.

### 🖼 그림으로 보기 — 시간축 위의 사이클과 예약

말로만 보면 헷갈리니 그림으로 고정해 둡니다. 이 절과 [3.10](#310--숫자로-따라가기--limitforperiod2-limitrefreshperiod1s)의 예시 설정은 모두 같습니다 — `limitForPeriod=2`, `limitRefreshPeriod=1s`, `timeoutDuration=500ms`. 리미터를 만든 순간을 0초로 둡니다.

```text
경과 시간(s)   0.0        0.5        1.0        1.5        2.0
               |------ cycle 0 ------|------ cycle 1 ------|------ cycle 2 ...
그 사이클 첫 계산  2                    min(남은 값 + 2, 2)    min(남은 값 + 2, 2)

 · 경계는 "생성 시점 + 1초의 배수"다. 벽시계의 정각 초가 아니다
 · 경계에서 누가 채워 주지 않는다. 경계를 넘은 뒤 처음 온 호출자가 계산으로 더한다
 · 남은 값이 음수(예약)면 새 사이클 몫에서 그만큼 먼저 빠진다
```

`activePermissions` 숫자 하나가 요청마다 어떻게 변하는지 따라가 봅니다. 요청 시각은 [3.10](#310--숫자로-따라가기--limitforperiod2-limitrefreshperiod1s)의 표와 같습니다.

```mermaid
flowchart LR
    S0["생성 직후<br/>cycle 0 · 2"] -->|"A t=0.0 통과"| S1["cycle 0 · 1"]
    S1 -->|"B t=0.1 통과"| S2["cycle 0 · 0"]
    S2 -->|"C t=0.6 예약<br/>0.4s 대기"| S3["cycle 0 · -1"]
    S3 -->|"D t=0.7 예약<br/>0.3s 대기"| S4["cycle 0 · -2"]
    S4 -->|"E t=0.9 거절<br/>대기 1.1s 필요"| S4
    S4 -->|"F t=1.05 가 계산<br/>min(-2 + 2, 2) = 0"| S5["cycle 1 · 0<br/>F도 거절"]
```

- `-1`, `-2`는 "C와 D가 cycle 1의 몫을 이미 가져갔다"는 뜻입니다. 그래서 새 사이클이 시작된 직후인 t=1.05의 F가 거절됩니다. 새 사이클이라고 무조건 자리가 있는 게 아닙니다.
- E는 거절됐기 때문에 숫자를 깎지 않습니다(`reservePermissions()`의 `canAcquireInTime=false` 분기).

같은 장면을 스레드 입장에서 보면 이렇습니다.

```mermaid
sequenceDiagram
    participant C as 요청 C (t=0.6)
    participant D as 요청 D (t=0.7)
    participant E as 요청 E (t=0.9)
    participant RL as AtomicRateLimiter

    C->>RL: acquirePermission()
    RL-->>C: nanosToWait 0.4s, 0.5s 이하라 예약 (0 → -1)
    D->>RL: acquirePermission()
    RL-->>D: nanosToWait 0.3s, 예약 (-1 → -2)
    E->>RL: acquirePermission()
    RL-->>E: nanosToWait 1.1s, 0.5s 초과라 예약 안 함 (-2 유지)
    Note over C,D: 각자 parkNanos 로 자다가 t=1.0 에 깨어나<br/>다시 확인하지 않고 바로 실행
    Note over E: 거절이 확정됐는데도 0.5s park<br/>t=1.4 에 false → RequestNotPermitted
```

---

## 2. 왜 알아야 하는가 (Why)

**첫째, 이게 기본 구현입니다.** `RateLimiter.of(...)`, `RateLimiter.ofDefaults(...)` 전부 `new AtomicRateLimiter(...)`를 반환합니다. `InMemoryRateLimiterRegistry`의 `computeIfAbsent`도 `new AtomicRateLimiter(name, ...)`입니다. Spring Boot 설정으로 `resilience4j.ratelimiter`를 쓰면 여러분이 돌리고 있는 건 이 클래스입니다. `SemaphoreBasedRateLimiter`는 직접 `new`로 만들지 않으면 쓰이지 않습니다.

**둘째, 외부 API 쿼터는 돈과 계정 정지로 이어집니다.** "초당 10회" 같은 제약은 지키지 못하면 429를 받고, 반복되면 키가 막힙니다. Retry와 CircuitBreaker는 *실패한 뒤에* 동작하지만 RateLimiter는 *실패하기 전에* 막습니다. 성격이 다릅니다.

**셋째, lock-free라는 선택의 비용과 이득을 알아야 합니다.** `SemaphoreBasedRateLimiter`는 `Semaphore.tryAcquire(timeout)`에서 스레드를 블로킹하고, 별도 스케줄러 스레드가 주기마다 permit을 반납합니다. `AtomicRateLimiter`는 CAS 한 번으로 상태를 갱신하고 끝냅니다. 허용량이 남아 있으면 **스레드가 전혀 멈추지 않습니다.** 대신 경합이 심하면 CAS가 실패해 재시도합니다 — 그래서 `compareAndSet()` 안에 `parkNanos(1)` back-off가 들어 있습니다. 즉 고TPS·짧은 주기에 유리하고, 코어 수를 크게 넘는 스레드가 동시에 몰리면 CAS 재시도 비용이 생깁니다.

**넷째, 인스턴스 로컬입니다.** 서버 N대면 실제 한도는 N배입니다. 이건 두 구현의 공통 한계이고, [RateLimiter ② Semaphore 방식과 둘의 선택 기준](./10-ratelimiter-semaphore.md)에서 계산까지 다룹니다.

---

## 3. 내부 구현 분석 (How)

### 3.1 시간 → 사이클 → 허용량

핵심은 `AtomicRateLimiter.calculateNextState()`입니다. 이름대로 **부수효과가 없는(side-effect-free)** 순수 함수이고, 그래서 CAS 루프 안에서 몇 번이고 다시 호출해도 안전합니다.

```java
private State calculateNextState(final int permits, final long timeoutInNanos,
                                 final State activeState) {
    long cyclePeriodInNanos = activeState.config.getLimitRefreshPeriod().toNanos();
    int permissionsPerCycle = activeState.config.getLimitForPeriod();

    long currentNanos = currentNanoTime();
    long currentCycle = currentNanos / cyclePeriodInNanos;

    long nextCycle = activeState.activeCycle;
    int nextPermissions = activeState.activePermissions;
    if (nextCycle != currentCycle) {
        long elapsedCycles = currentCycle - nextCycle;
        long accumulatedPermissions = elapsedCycles * permissionsPerCycle;
        nextCycle = currentCycle;
        nextPermissions = (int) min(nextPermissions + accumulatedPermissions,
            permissionsPerCycle);
    }
    long nextNanosToWait = nanosToWaitForPermission(
        permits, cyclePeriodInNanos, permissionsPerCycle, nextPermissions, currentNanos,
        currentCycle
    );
    State nextState = reservePermissions(activeState.config, permits, timeoutInNanos, nextCycle,
        nextPermissions, nextNanosToWait);
    return nextState;
}
```

줄 단위로 읽어 봅니다.

- `currentNanoTime()`은 `nanoTimeSupplier.get() - nanoTimeStart`입니다. **벽시계가 아니라 생성 시점 기준 상대 시간**이고, `nanoTimeStart`는 생성자에서 한 번 찍습니다. NTP가 시계를 당겨도, 서버 시간을 바꿔도 영향이 없습니다. `System.nanoTime()`을 주입 가능하게 만든 생성자(`Supplier<Long> nanoTimeSupplier`)가 있어서 테스트에서 시간을 직접 조작할 수 있습니다.
- `currentCycle = currentNanos / cyclePeriodInNanos` — 정수 나눗셈 한 번이 전부입니다. 이게 "토큰을 채우지 않는다"의 실체입니다.
- `if (nextCycle != currentCycle)` — 사이클이 바뀌었으면 `elapsedCycles × permissionsPerCycle`을 더합니다. 10초 동안 아무도 호출하지 않았다면 `elapsedCycles`가 10이지만...
- `min(nextPermissions + accumulatedPermissions, permissionsPerCycle)` — **상한이 `limitForPeriod`로 잘립니다.** 쉰 만큼 쌓아두는 구조가 아닙니다. 버스트 허용량은 언제나 최대 `limitForPeriod`입니다. 토큰 버킷의 버킷 크기가 `limitForPeriod`로 고정된 것과 같습니다.
- `nanosToWaitForPermission(...)` — 지금 당장 허용 못 하면 얼마나 기다려야 하는지 계산
- `reservePermissions(...)` — 기다릴 수 있는 범위면 허용량을 깎아서(=예약) 새 `State`를 만듭니다

```mermaid
flowchart TB
    A["currentNanoTime()<br/>= nanoTime() - nanoTimeStart"] --> B["currentCycle<br/>= currentNanos / cyclePeriodInNanos"]
    B --> C{"activeCycle<br/>!= currentCycle ?"}
    C -->|"같다 (같은 사이클)"| E["nextPermissions 그대로"]
    C -->|"다르다 (사이클 넘어감)"| D["elapsedCycles x limitForPeriod 더하고<br/>min(..., limitForPeriod) 로 상한 절단"]
    D --> E
    E --> F["nanosToWaitForPermission()"]
    F --> G{"timeoutInNanos<br/>≥ nanosToWait ?"}
    G -->|yes| H["permissions -= permits<br/>(음수 허용 = 예약)"]
    G -->|no| I["permissions 그대로<br/>(예약하지 않음)"]
    H --> J["new State(config, cycle, permissions, nanosToWait)"]
    I --> J
```

### 3.2 얼마나 기다려야 하는가

`AtomicRateLimiter.nanosToWaitForPermission()`입니다.

```java
private long nanosToWaitForPermission(final int permits, final long cyclePeriodInNanos,
                                      final int permissionsPerCycle,
                                      final int availablePermissions, final long currentNanos, final long currentCycle) {
    if (availablePermissions >= permits) {
        return 0L;
    }
    long nextCycleTimeInNanos = (currentCycle + 1) * cyclePeriodInNanos;
    long nanosToNextCycle = nextCycleTimeInNanos - currentNanos;
    int permissionsAtTheStartOfNextCycle = availablePermissions + permissionsPerCycle;
    int fullCyclesToWait = divCeil(-(permissionsAtTheStartOfNextCycle - permits), permissionsPerCycle);
    return (fullCyclesToWait * cyclePeriodInNanos) + nanosToNextCycle;
}
```

- `availablePermissions >= permits`면 0. 흔한 경로가 분기 하나로 끝납니다.
- `nanosToNextCycle` — 다음 사이클 시작까지 남은 시간. **사이클 경계는 절대 좌표**라서, 사이클 중간에 들어온 호출자는 다음 경계까지만 기다립니다.
- `permissionsAtTheStartOfNextCycle = availablePermissions + permissionsPerCycle` — 다음 경계에서 받게 될 허용량. `availablePermissions`가 이미 음수(예약이 쌓인 상태)면 이 값도 작아집니다.
- `fullCyclesToWait = divCeil(-(permissionsAtTheStartOfNextCycle - permits), permissionsPerCycle)` — 다음 경계 허용량으로도 부족하면 **추가로 몇 사이클을 더 기다려야 하는지**를 올림 나눗셈으로 구합니다. 예약이 많이 쌓이면 대기 시간이 선형으로 늘어나고, 결국 `timeoutDuration`을 넘겨 거절됩니다. 큐가 무한히 자라지 않게 막는 장치가 바로 `timeoutDuration`입니다.

`divCeil(x, y)`는 `(x + y - 1) / y`입니다. Javadoc에 `x`, `y` 모두 0보다 커야 한다고 명시돼 있고, 위 호출 지점에서는 `availablePermissions < permits`가 보장된 뒤라 그 전제가 성립합니다.

### 3.3 예약은 "깎아두는 것"이 전부

```java
private State reservePermissions(final RateLimiterConfig config, final int permits,
                                 final long timeoutInNanos,
                                 final long cycle, final int permissions, final long nanosToWait) {
    boolean canAcquireInTime = timeoutInNanos >= nanosToWait;
    int permissionsWithReservation = permissions;
    if (canAcquireInTime) {
        permissionsWithReservation -= permits;
    }
    return new State(config, cycle, permissionsWithReservation, nanosToWait);
}
```

세 줄이지만 이 메서드가 설계의 중심입니다.

- 기다려서 받을 수 있으면(`canAcquireInTime`) 허용량을 깎습니다. 음수가 되어도 그대로 둡니다. **음수는 "이미 미래 몫을 가져간 사람이 있다"는 뜻**입니다.
- 못 받을 상황이면 깎지 않습니다. 거절될 호출이 다른 호출자의 자리를 먹으면 안 되니까요.
- `nanosToWait`는 두 경우 모두 그대로 담깁니다. 호출자는 이 값으로 "기다릴까 / 거절할까"를 판단합니다.

### 3.4 CAS 루프와 back-off

```java
private State updateStateWithBackOff(final int permits, final long timeoutInNanos) {
    AtomicRateLimiter.State prev;
    AtomicRateLimiter.State next;
    do {
        prev = state.get();
        next = calculateNextState(permits, timeoutInNanos, prev);
    } while (!compareAndSet(prev, next));
    return next;
}

private boolean compareAndSet(final State current, final State next) {
    if (state.compareAndSet(current, next)) {
        return true;
    }
    parkNanos(1); // back-off
    return false;
}
```

- `prev = state.get()` → 계산 → CAS. 실패하면 **처음부터 다시 계산합니다.** `calculateNextState()`가 순수 함수라서 재계산이 안전하고, 시간도 다시 읽으므로 재시도 중 사이클이 넘어가면 그 사실이 자연스럽게 반영됩니다.
- `AtomicReference.updateAndGet()`을 쓰지 않은 이유는 `compareAndSet()`의 `parkNanos(1)`입니다. CAS가 실패했다는 건 다른 스레드와 부딪혔다는 뜻이고, 곧바로 재시도하면 또 부딪힙니다. 1나노초만 양보해도 경합이 크게 줄어듭니다. Javadoc이 근거 논문(arXiv 1305.5800)과 "showed great results ... in benchmark tests"를 명시하고 있습니다.
- `parkNanos(1)`은 실제로 1ns를 자는 게 아니라 OS 타이머 해상도만큼(보통 수십 µs) 멈춥니다. 양보 시간이 생각보다 길다는 걸 알고 보세요. 극단적 경합에서 지연 꼬리(tail latency)로 나타납니다.

```mermaid
sequenceDiagram
    participant T1 as Thread A
    participant T2 as Thread B
    participant S as AtomicReference(State)

    T1->>S: state.get() → S0
    T2->>S: state.get() → S0
    T1->>T1: calculateNextState(S0) → S1
    T2->>T2: calculateNextState(S0) → S1'
    T1->>S: compareAndSet(S0, S1) ✅
    T2->>S: compareAndSet(S0, S1') ❌
    T2->>T2: parkNanos(1) back-off
    T2->>S: state.get() → S1 (재조회)
    T2->>T2: calculateNextState(S1) → S2
    T2->>S: compareAndSet(S1, S2) ✅
```

### 3.5 대기: `parkNanos`와 인터럽트

```java
private boolean waitForPermission(final long nanosToWait) {
    waitingThreads.incrementAndGet();
    long deadline = currentNanoTime() + nanosToWait;
    boolean wasInterrupted = false;
    while (currentNanoTime() < deadline && !wasInterrupted) {
        long sleepBlockDuration = deadline - currentNanoTime();
        parkNanos(sleepBlockDuration);
        wasInterrupted = Thread.interrupted();
    }
    waitingThreads.decrementAndGet();
    if (wasInterrupted) {
        currentThread().interrupt();
    }
    return !wasInterrupted;
}
```

- `while` 루프로 감싼 이유는 `parkNanos`가 **spurious wakeup**을 허용하기 때문입니다. 데드라인까지 남은 시간을 매번 다시 계산해 다시 park합니다.
- `Thread.interrupted()`는 **플래그를 읽고 지웁니다.** 그래서 루프를 빠져나온 뒤 `currentThread().interrupt()`로 다시 세워 호출자에게 전달합니다. `InterruptedException`을 던지지 않고 인터럽트 상태만 보존하는, 라이브러리로서 올바른 처리입니다.
- 인터럽트되면 `false`를 반환하고, `acquirePermission()`은 그 값으로 `RateLimiterOnFailureEvent`를 발행합니다. 호출 쪽에서는 `RateLimiter.waitForPermission(rateLimiter, permits)` 정적 메서드가 인터럽트 상태를 먼저 확인해 `AcquirePermissionCancelledException`을, 그 외에는 `RequestNotPermitted`를 던집니다. **거절(RequestNotPermitted)과 취소(AcquirePermissionCancelledException)를 구분해야 합니다.** 전자는 재시도 대상이고 후자는 아닙니다.

`waitForPermissionIfNecessary()`의 마지막 분기도 눈여겨보세요.

```java
if (canAcquireImmediately) {
    return true;
}
if (canAcquireInTime) {
    return waitForPermission(nanosToWait);
}
waitForPermission(timeoutInNanos);
return false;
```

거절이 확정된 호출자도 `timeoutInNanos`만큼 **기다렸다가** `false`를 받습니다. 즉시 실패하지 않습니다. `timeoutDuration=5s`(기본값)로 두면 거절되는 요청도 5초를 붙잡고 있다가 실패합니다. 웹 요청 스레드에서 이 설정은 사고입니다.

### 3.6 세 가지 획득 방식

| 메서드 | 블로킹 | 반환 | 용도 |
|---|---|---|---|
| `acquirePermission(int permits)` | `timeoutDuration`까지 park | `boolean` | 동기 호출. 내부에서 대기까지 끝내줌 |
| `reservePermission(int permits)` | **하지 않음** | 즉시 가능 `0`, 가능하지만 대기 필요 `nanosToWait`, 불가 `-1` | 비동기/논블로킹. 대기는 호출자가 스케줄링 |
| `drainPermissions()` | 하지 않음 | `void` | 남은 허용량을 즉시 0으로 (서버가 429를 주면 남은 주기 포기) |

`reservePermission()`은 상태를 똑같이 갱신(=예약)하지만 park하지 않고 "몇 나노초 뒤에 오라"는 숫자만 돌려줍니다. 이벤트 루프나 Reactor 스케줄러에서 `Mono.delay(...)`로 바꿔 쓰기에 알맞습니다.

`drainPermissions()`는 `calculateNextState(prev.activePermissions, 0, prev)`로 남은 양만큼을 스스로 소비해 0으로 만듭니다. 발행하는 이벤트의 permit 수가 `Math.min(prev.activePermissions, 0)`이라 **항상 0 이하**인 반면, `SemaphoreBasedRateLimiter.drainPermissions()`는 `semaphore.drainPermits()`의 양수 결과를 그대로 넣습니다. 두 구현에서 `RateLimiterOnDrainedEvent.getNumberOfPermits()`의 의미가 다릅니다. 이 숫자로 알람을 만들지 마세요. 드레인이 일어났다는 사실만 쓰십시오.

### 3.7 메트릭은 상태를 바꾸지 않는다

```java
@Override
public int getAvailablePermissions() {
    State currentState = state.get();
    State estimatedState = calculateNextState(1, -1, currentState);
    return estimatedState.activePermissions;
}
```

`timeoutInNanos`에 `-1`을 넘기는 게 핵심입니다. `reservePermissions()`에서 `canAcquireInTime = (-1 >= nanosToWait)`는 `nanosToWait >= 0`인 한 항상 `false`이므로 **예약이 발생하지 않습니다.** 그리고 결과를 CAS하지 않으니 `state`도 그대로입니다. 순수 함수 하나를 계산용·조회용으로 같이 쓰는 깔끔한 재사용입니다.

`getDetailedMetrics()`로 받는 `AtomicRateLimiterMetrics`에는 `getNanosToWait()`, `getCycle()`이 추가로 있습니다. `RateLimiter.Metrics` 인터페이스에는 `getAvailablePermissions()`, `getNumberOfWaitingThreads()` 둘만 있으니, 상세 지표가 필요하면 `AtomicRateLimiter`로 캐스팅해야 합니다.

### 3.8 이벤트

`acquirePermission()` / `reservePermission()`은 결과에 따라 `RateLimiterOnSuccessEvent`(`Type.SUCCESSFUL_ACQUIRE`) 또는 `RateLimiterOnFailureEvent`(`Type.FAILED_ACQUIRE`)를, `drainPermissions()`는 `RateLimiterOnDrainedEvent`(`Type.DRAINED`)를 발행합니다. 셋 다 `eventProcessor.hasConsumers()` 가드 뒤에 있어서 **구독자가 없으면 객체를 아예 만들지 않습니다.**

주의할 점: `RateLimiter.EventPublisher`에는 `onSuccess(...)`, `onFailure(...)`만 있습니다. **`onDrained(...)`는 없습니다.** 드레인을 잡으려면 `onEvent(...)`로 전체를 받아 타입으로 걸러야 합니다.

```java
rateLimiter.getEventPublisher()
    .onEvent(event -> {
        if (event.getEventType() == RateLimiterEvent.Type.DRAINED) {
            log.warn("rate limiter {} drained", event.getRateLimiterName());
        }
    });
```

### 3.9 🖼 그림으로 보기 — `acquirePermission()` 한 번의 전체 경로

3.1~3.5에 흩어진 분기를 한 장으로 모으면 이렇습니다. `acquirePermission()` → `updateStateWithBackOff()` → `waitForPermissionIfNecessary()` 순서 그대로입니다.

```mermaid
flowchart TB
    S["acquirePermission(permits)"] --> U["updateStateWithBackOff()<br/>calculateNextState + CAS 루프<br/>(이 시점에 예약 여부까지 확정)"]
    U --> Q1{"nanosToWait ≤ 0 ?"}
    Q1 -->|yes| OK["즉시 true<br/>SUCCESSFUL_ACQUIRE"]
    Q1 -->|no| Q2{"timeoutInNanos ≥ nanosToWait ?"}
    Q2 -->|"yes · 이미 예약됨"| W1["waitForPermission(nanosToWait)<br/>parkNanos 루프"]
    W1 --> Q3{"대기 중 인터럽트?"}
    Q3 -->|no| OK2["true · 재확인 없이 진행"]
    Q3 -->|yes| CAN["false + 인터럽트 플래그 복원<br/>→ AcquirePermissionCancelledException"]
    Q2 -->|"no · 예약 안 됨"| W2["waitForPermission(timeoutInNanos)<br/>거절도 timeout 만큼 park"]
    W2 --> REJ["false → RequestNotPermitted<br/>FAILED_ACQUIRE"]
```

핵심은 **결정이 맨 위 CAS 한 번에서 끝난다**는 점입니다. 아래쪽 분기는 이미 정해진 결과에 맞춰 "얼마나 잘지"만 고릅니다. 세마포어처럼 자다가 깨서 다시 경쟁하는 단계가 없습니다.

### 3.10 🔢 숫자로 따라가기 — `limitForPeriod=2`, `limitRefreshPeriod=1s`

같은 요청 9건을 `timeoutDuration`만 바꿔서 흘려 봅니다. 리미터 생성 시각이 0초이고, 요청 처리 시간은 0으로 칩니다. "시작 값"은 사이클 반영(`min(남은 값 + 경과 사이클 × 2, 2)`)까지 끝낸 뒤의 `activePermissions`입니다.

**(A) `timeoutDuration=0` — 예약 없음**

| 요청 시각 | cycle | 시작 값 | `nanosToWait` | 결과 | 끝 값 |
|---|---|---|---|---|---|
| 0.00 | 0 | 2 | 0 | ✅ 통과 | 1 |
| 0.10 | 0 | 1 | 0 | ✅ 통과 | 0 |
| 0.20 | 0 | 0 | 0.80s | ❌ 즉시 거절 | 0 |
| 0.60 | 0 | 0 | 0.40s | ❌ 즉시 거절 | 0 |
| 0.70 | 0 | 0 | 0.30s | ❌ 즉시 거절 | 0 |
| 0.90 | 0 | 0 | 0.10s | ❌ 즉시 거절 | 0 |
| 1.05 | 1 | min(0+2, 2) = 2 | 0 | ✅ 통과 | 1 |
| 1.10 | 1 | 1 | 0 | ✅ 통과 | 0 |
| 1.60 | 1 | 0 | 0.40s | ❌ 즉시 거절 | 0 |

통과 4건, 거절 5건. 거절된 스레드가 잡혀 있던 시간은 **0초**입니다. 값이 한 번도 음수로 내려가지 않습니다.

**(B) `timeoutDuration=500ms` — 0.5초 안에 자리가 나면 예약**

| 요청 시각 | cycle | 시작 값 | `nanosToWait` | 계산 | 결과 | 끝 값 |
|---|---|---|---|---|---|---|
| 0.00 | 0 | 2 | 0 | | ✅ 즉시 실행 | 1 |
| 0.10 | 0 | 1 | 0 | | ✅ 즉시 실행 | 0 |
| 0.20 | 0 | 0 | 0.80s | 다음 경계까지 0.8s > 0.5s | ❌ 0.5s park 후 t=0.70에 거절 | 0 |
| 0.60 | 0 | 0 | 0.40s | 다음 경계 몫 2로 충분 | ⏳ 예약 → t=1.00 실행 | -1 |
| 0.70 | 0 | -1 | 0.30s | 다음 경계 몫 -1+2=1, 충분 | ⏳ 예약 → t=1.00 실행 | -2 |
| 0.90 | 0 | -2 | 1.10s | 다음 경계 몫 -2+2=0 → `divCeil`로 1사이클 더 | ❌ 0.5s park 후 t=1.40에 거절 | -2 |
| 1.05 | 1 | min(-2+2, 2) = 0 | 0.95s | 다음 경계까지 0.95s | ❌ t=1.55에 거절 | 0 |
| 1.10 | 1 | 0 | 0.90s | | ❌ t=1.60에 거절 | 0 |
| 1.60 | 1 | 0 | 0.40s | | ⏳ 예약 → t=2.00 실행 | -1 |

통과 5건, 거절 4건. 표에서 읽어야 할 것 네 가지입니다.

1. **실제 실행 시각은 사이클당 정확히 2건**입니다(0.00·0.10 / 1.00·1.00 / 2.00·…). 대기를 허용해도 레이트는 지켜집니다.
2. **먼저 온 0.20이 거절되고 나중에 온 0.60이 통과합니다.** 판단 기준이 "도착 순서"가 아니라 "기다려야 할 시간 ≤ `timeoutDuration`"이기 때문입니다. FIFO가 아닙니다.
3. **t=1.05·1.10은 새 사이클인데도 거절됩니다.** 0.60·0.70이 cycle 1의 몫을 미리 가져갔기 때문입니다.
4. **거절 4건이 각각 0.5초씩, 예약 3건이 0.3~0.4초씩 스레드를 잡았습니다.** 합하면 3.1 스레드·초입니다. (A)에서는 0이었습니다. `timeoutDuration`은 "처리량을 조금 더 얻는 대신 스레드를 잡아 두는 비용"입니다.

> 참고: 0.60처럼 `availablePermissions + permissionsPerCycle - permits`가 0 이상이면 `divCeil`의 첫 인자가 0 이하가 됩니다. Javadoc의 "x > 0" 전제와 달리 실제로는 이 경우도 들어오는데, `(x + y - 1) / y`가 0을 돌려주므로 "추가 사이클 0"으로 올바르게 계산됩니다(`AtomicRateLimiter.nanosToWaitForPermission()` / `divCeil()`).

---

## 4. 실전 예제

### 4-1. 외부 API 쿼터 "초당 10회"

```java
@Configuration
public class PaymentApiRateLimiterConfig {

    /** 결제 PG 쿼터: 10 req/s. 버스트도 10을 넘지 않는다. */
    @Bean
    RateLimiter paymentApiRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(10)
            .limitRefreshPeriod(Duration.ofSeconds(1))
            // 웹 요청 스레드에서 쓰므로 대기는 짧게. 기본 5초는 절대 쓰지 않는다.
            .timeoutDuration(Duration.ofMillis(200))
            .writableStackTraceEnabled(false) // 거절 로그 스팸 방지
            .build();
        return registry.rateLimiter("paymentApi", config);
    }
}
```

`limitRefreshPeriod`를 1초로 두면 초 경계에서 10개가 한 번에 열립니다. 상대 API가 더 촘촘한 평활화를 요구하면 `limitForPeriod(1)` + `limitRefreshPeriod(Duration.ofMillis(100))`로 쪼개세요. 같은 10 req/s지만 버스트가 1로 줄어듭니다. **같은 평균 레이트라도 버킷 크기는 전혀 다릅니다.**

### 4-2. `timeoutDuration=0` 즉시 거절 vs 대기 허용

```java
@Service
public class QuoteClient {

    private static final RateLimiterConfig FAIL_FAST = RateLimiterConfig.custom()
        .limitForPeriod(10)
        .limitRefreshPeriod(Duration.ofSeconds(1))
        .timeoutDuration(Duration.ZERO)          // 예약 없음 → 남은 게 없으면 즉시 거절
        .writableStackTraceEnabled(false)
        .build();

    private static final RateLimiterConfig WAIT_UP_TO_300MS = RateLimiterConfig.custom()
        .limitForPeriod(10)
        .limitRefreshPeriod(Duration.ofSeconds(1))
        .timeoutDuration(Duration.ofMillis(300)) // 300ms 안에 자리가 나면 예약 후 대기
        .writableStackTraceEnabled(false)
        .build();

    private final RateLimiter failFast = RateLimiter.of("quote-ff", FAIL_FAST);
    private final RateLimiter waiting = RateLimiter.of("quote-wait", WAIT_UP_TO_300MS);

    /** 사용자 대기 화면: 빨리 실패하고 폴백을 보여주는 쪽이 낫다. */
    public Quote fetchForUser(String symbol) {
        return failFast.executeSupplier(() -> http.getQuote(symbol));
    }

    /** 배치 적재: 조금 기다려도 되니 처리량을 챙긴다. */
    public Quote fetchForBatch(String symbol) {
        return waiting.executeSupplier(() -> http.getQuote(symbol));
    }
}
```

`timeoutDuration=0`이면 `reservePermissions()`의 `canAcquireInTime`이 `nanosToWait > 0`인 순간 `false`가 되어 예약이 생기지 않고, `waitForPermissionIfNecessary()`도 `waitForPermission(0)`을 지나 바로 `false`를 반환합니다. 즉 **`activePermissions`가 음수로 내려가지 않습니다.** 반대로 0보다 크게 두면 예약이 쌓이고, 쌓인 만큼 뒤에 온 호출자의 `nanosToWait`가 길어집니다.

리뷰 체크포인트: **동기 웹 요청 경로에서 `timeoutDuration`은 그 엔드포인트의 SLA보다 작아야 합니다.** 그렇지 않으면 rate limiter가 장애를 막는 대신 스레드를 붙잡아 장애를 만듭니다.

### 4-3. `reservePermission()`으로 논블로킹 대기

```java
@Component
public class ReactiveQuoteClient {

    private final RateLimiter rateLimiter;
    private final WebClient webClient;

    public Mono<Quote> fetch(String symbol) {
        return Mono.defer(() -> {
            long nanos = rateLimiter.reservePermission(1);
            if (nanos < 0) {
                return Mono.error(RequestNotPermitted.createRequestNotPermitted(rateLimiter));
            }
            Mono<Quote> call = webClient.get()
                .uri("/quotes/{s}", symbol)
                .retrieve()
                .bodyToMono(Quote.class);
            // 이미 자리는 예약됐다. 이벤트 루프를 막지 않고 시간만 흘려보낸다.
            return nanos == 0 ? call : Mono.delay(Duration.ofNanos(nanos)).then(call);
        });
    }
}
```

`reservePermission()`은 스레드를 park하지 않습니다. 이벤트 루프 스레드에서 `acquirePermission()`을 호출하면 루프 전체가 멈추니, Reactor/코루틴 경로에서는 반드시 이 형태를 쓰세요. 실제 Reactor 연산자(`RateLimiterOperator`)도 같은 방식입니다 — [advanced/08-reactor-operators](../advanced/08-reactor-operators.md).

### 4-4. 지표 감시

```java
@Component
@RequiredArgsConstructor
public class RateLimiterGauges {

    private final MeterRegistry meterRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;

    @PostConstruct
    void bind() {
        rateLimiterRegistry.getAllRateLimiters().forEach(rl -> {
            Gauge.builder("app.ratelimiter.available.permissions",
                    rl, r -> r.getMetrics().getAvailablePermissions())
                .tag("name", rl.getName())
                .register(meterRegistry);

            Gauge.builder("app.ratelimiter.waiting.threads",
                    rl, r -> r.getMetrics().getNumberOfWaitingThreads())
                .tag("name", rl.getName())
                .register(meterRegistry);

            if (rl instanceof AtomicRateLimiter atomic) {
                Gauge.builder("app.ratelimiter.nanos.to.wait",
                        atomic, a -> a.getDetailedMetrics().getNanosToWait())
                    .tag("name", rl.getName())
                    .register(meterRegistry);
            }
        });
    }
}
```

읽는 법:

- `availablePermissions`가 **음수로 머문다** → 예약이 쌓이고 있음. 유입이 한도를 넘었다는 신호
- `waitingThreads`가 0보다 크게 유지된다 → `timeoutDuration` 때문에 스레드가 붙잡혀 있음. 스레드풀 포화로 번질 수 있음
- `FAILED_ACQUIRE` 이벤트 비율이 상승 → 한도를 올릴지, 큐를 둘지, 상류를 줄일지 결정할 시점

`Micrometer` 바인딩을 직접 쓰면 `resilience4j.ratelimiter.available.permissions` 등 표준 이름으로 나옵니다 — [advanced/06-micrometer-metrics](../advanced/06-micrometer-metrics.md).

### 4-5. 서버가 429를 주면 남은 주기를 포기

```java
RateLimiterConfig config = RateLimiterConfig.custom()
    .limitForPeriod(10)
    .limitRefreshPeriod(Duration.ofSeconds(1))
    .timeoutDuration(Duration.ofMillis(100))
    .drainPermissionsOnResult(either -> either.isLeft()
        && either.getLeft() instanceof TooManyRequestsException)
    .build();
```

`drainPermissionsOnResult`는 `RateLimiter.drainIfNeeded(...)`에서 평가되고, `onError/onSuccess/onResult`를 통해 호출됩니다. 즉 **`decorate*`로 감싼 호출에서만 동작합니다.** `acquirePermission()`을 직접 부르고 비즈니스 로직을 따로 실행하면 결과를 알려주지 않으므로 드레인이 일어나지 않습니다. 내 추정보다 상대 서버의 판정이 정확하다는 걸 받아들이는 장치입니다.

### 4-6. 🔥 시나리오 — 기본 `timeoutDuration(5s)`가 웹 서버를 멈춘 날

**상황.** 포인트 지급 API가 외부 원장 서버를 호출합니다. 원장 서버 계약 쿼터에 맞춰 인스턴스당 `limitForPeriod(10)` + `limitRefreshPeriod(1s)`를 걸었고, `timeoutDuration`은 **적지 않았습니다**(= 기본 5초). Tomcat 최대 스레드는 200개입니다. 평소 유입은 인스턴스당 초당 5건이라 한 번도 거절이 없었습니다.

**시간 순서.**

| 시각 | 무슨 일 | 스레드 상황 |
|---|---|---|
| 10:00:00 | 출석 이벤트 오픈. 인스턴스당 유입이 초당 **60건**으로 뜀 | 매 사이클 10건만 즉시 통과 |
| 10:00:00~01 | 나머지 50건은 `nanosToWait`를 계산 → 5초 안에 자리가 나는 요청은 **예약**(음수 permission), 그 뒤 요청은 거절 확정 | 예약된 요청은 최대 5초 park, **거절 확정 요청도 5초 park**(3.5의 마지막 분기) |
| 10:00:01~05 | 예약이 앞으로 5사이클치(≈ 50건)까지 쌓여 `availablePermissions`가 -40~-50 근처에 머묾. 새로 오는 요청은 대부분 `nanosToWait > 5s` → 거절인데도 5초씩 대기 | 예약 대기 ≈ 50개 + 거절 대기 50건/s × 경과 초 |
| 10:00:03 무렵 | 50 + 50 × 3 = **200** → Tomcat 스레드 소진 | 원장과 상관없는 잔액 조회·헬스체크까지 응답 불가 |
| 10:00:05 이후 | 정상 상태에서 대기 스레드 수 ≈ 50(예약) + 250(거절 대기) = 300 > 200 | 리미터가 서비스를 지키는 게 아니라 **멈추게 함** |

**지표로 보면.** `getNumberOfWaitingThreads()`가 수백으로 치솟고, `availablePermissions`가 음수에 붙어 있습니다(4-4 읽는 법 그대로). 원장 서버 쪽 호출량은 정확히 초당 10건 — **쿼터는 완벽하게 지켜졌는데 우리 서버가 죽었습니다.**

**고친 설정.**

```java
RateLimiterConfig.custom()
    .limitForPeriod(10)
    .limitRefreshPeriod(Duration.ofSeconds(1))
    .timeoutDuration(Duration.ofMillis(100))   // 다음 경계가 0.1초 안일 때만 예약
    .writableStackTraceEnabled(false)
    .build();
```

- 거절 확정 요청이 잡히는 시간이 5초 → 0.1초. 거절 대기 스레드 ≈ 50건/s × 0.1s = **5개**.
- 예약은 "사이클이 끝나기 0.1초 전에 온 요청"만 생깁니다. 예약 대기 스레드도 한 자릿수~십여 개.
- 거절된 지급 요청은 `RequestNotPermitted`를 잡아 "지급 접수됨"으로 응답하고 큐에 적재해 비동기로 처리합니다. 쿼터를 늘릴 수 없다면 **줄을 세울 곳은 웹 스레드가 아니라 큐**입니다.

---

## 5. 정리

| 질문 | 답 |
|---|---|
| 토큰을 어디에 저장하나 | 저장하지 않는다. `currentNanos / cyclePeriodInNanos`로 사이클을 계산한다 |
| 상태는 무엇인가 | 불변 `State(config, activeCycle, activePermissions, nanosToWait)` 하나 |
| 동시성 처리 | `AtomicReference` CAS 루프 + 실패 시 `parkNanos(1)` back-off |
| 재계산이 안전한 이유 | `calculateNextState()`가 부수효과 없는 순수 함수 |
| 음수 `activePermissions` | 미래 허용량을 당겨쓴 **예약**. `reservePermissions()`에서 발생 |
| 예약이 생기는 조건 | `timeoutDuration >= nanosToWait` |
| 버스트 상한 | 항상 `limitForPeriod`. `min(..., permissionsPerCycle)`로 절단 |
| 거절도 기다리는가 | 그렇다. `waitForPermissionIfNecessary()`가 `timeoutInNanos`만큼 park 후 `false` |
| 인터럽트 처리 | `InterruptedException` 대신 플래그 재설정. 호출부에서 `AcquirePermissionCancelledException` |
| 기본 구현인가 | 그렇다. `RateLimiter.of*`, `InMemoryRateLimiterRegistry` 모두 `AtomicRateLimiter` |
| `EventPublisher`에 `onDrained`가 있나 | 없다. `onEvent()` + `Type.DRAINED`로 걸러야 한다 |
| 가장 흔한 설정 사고 | `limitForPeriod`만 바꾸고 기본 `limitRefreshPeriod(500ns)`를 남겨둠 / 웹 스레드에 기본 `timeoutDuration(5s)` |
| 한계 | 인스턴스 로컬. 서버 N대면 실제 한도 N배 |

---

## 6. ❓ 헷갈리는 포인트

- **Q. `timeoutDuration=0`이면 어떻게 되나요?** — 남은 허용량이 있으면 통과, 없으면 **즉시** 거절입니다. `reservePermissions()`의 `canAcquireInTime`(`0 >= nanosToWait`)이 `nanosToWait > 0`인 순간 `false`라 예약이 생기지 않고, `waitForPermission(0)`은 바로 빠져나옵니다. `activePermissions`가 음수로 내려가지 않습니다([3.10 (A)](#310--숫자로-따라가기--limitforperiod2-limitrefreshperiod1s)).
- **Q. "1초에 10개"면 100ms마다 1개씩 나가나요?** — 아닙니다. 사이클 경계에서 10개가 **한꺼번에** 열리고, 버스트 상한이 10입니다. 고르게 펴고 싶으면 `limitForPeriod(1)` + `limitRefreshPeriod(100ms)`처럼 주기를 쪼개야 합니다(4-1).
- **Q. 한참 안 쓰면 허용량이 쌓여서 나중에 몰아 쓸 수 있나요?** — 없습니다. `min(nextPermissions + accumulatedPermissions, permissionsPerCycle)`로 `limitForPeriod`에서 잘립니다.
- **Q. 사이클 경계는 매 정각 초인가요?** — 아닙니다. `currentNanoTime() = nanoTime() - nanoTimeStart`라 **리미터를 만든 시점 기준**입니다. 서버마다, 인스턴스마다 경계가 다릅니다.
- **Q. 거절이면 바로 예외가 나오나요?** — `timeoutDuration > 0`이면 아닙니다. 거절이 계산 시점에 확정돼도 `timeoutDuration`만큼 park한 뒤 `RequestNotPermitted`가 납니다. 반대로 `reservePermission()`은 park하지 않고 `-1`을 즉시 돌려줍니다.
- **Q. 예약한 자리를 취소할 수 있나요?** — `RateLimiter` 인터페이스에 예약을 되돌리는 메서드는 없습니다. `reservePermission()`이 양수를 돌려준 순간 자리는 이미 깎였으므로, 그 뒤에 호출을 포기하면 그 몫은 그냥 버려집니다.

---

## 7. 🧾 한 장 요약

- `AtomicRateLimiter`는 토큰을 채우지 않는다. **`(nanoTime - 생성시각) / limitRefreshPeriod` = 사이클 번호**를 계산하고, 사이클이 넘어갔으면 그때 한꺼번에 더한다(상한 `limitForPeriod`).
- 상태는 불변 `State` 하나, 갱신은 `AtomicReference` CAS 루프 + `parkNanos(1)` back-off. 허용 여부는 **CAS가 성공하는 그 한 번에 확정**된다.
- `timeoutDuration > 0`이면 미래 몫을 당겨쓰는 **예약**이 생기고 `activePermissions`가 음수가 된다. 예약된 호출자는 `nanosToWait`만큼 자고 재확인 없이 실행한다.
- 판단 기준은 도착 순서가 아니라 "기다릴 시간 ≤ `timeoutDuration`"이다. **FIFO가 아니다.**
- 거절이 확정된 호출자도 `timeoutDuration`만큼 스레드를 잡는다. **웹 스레드 경로에 기본값 5초를 두면 리미터가 서버를 멈춘다.**
- `timeoutDuration=0`은 예약 없음 · 즉시 거절. 논블로킹 경로는 `reservePermission()`(0 / 대기 나노초 / -1)으로 park 없이 쓴다.
- 기본 `limitRefreshPeriod`는 500ns다. `limitForPeriod`와 **항상 같이** 설정한다.
- 인스턴스 로컬이다. 서버 N대면 실제 한도는 N배.

---

## 관련 문서
- 선행: [Retry 내부 구현](./08-retry-internals.md)
- 후행: [RateLimiter ② Semaphore 방식과 둘의 선택 기준](./10-ratelimiter-semaphore.md)

---
*Resilience4j v2.4.0 기준 · 소스 커밋 7e3ab525*

*마지막 업데이트: 2026년 10월*
