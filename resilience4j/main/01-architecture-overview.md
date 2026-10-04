# Resilience4j 전체 구조와 모듈 지도

Resilience4j 는 "상속해서 커맨드를 만드는" 라이브러리가 아니라 "함수를 감싸는" 라이브러리다. 이 문서는 `settings.gradle` 과 각 모듈 `build.gradle` 을 실제로 읽어서 31개 모듈이 어떤 계층을 이루는지, 그리고 왜 `Supplier` 를 감싸는 설계를 택했는지를 정리한다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
  - [🖼 그림으로 보기 — 4층 구조로 줄인 모듈 지도](#-그림으로-보기--4층-구조로-줄인-모듈-지도)
  - [🖼 그림으로 보기 — 데코레이터 하나의 실행 흐름](#-그림으로-보기--데코레이터-하나의-실행-흐름)
  - [🖼 그림으로 보기 — 양파 껍질과 호출 한 번의 왕복](#-그림으로-보기--양파-껍질과-호출-한-번의-왕복)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)
- [6. 🔢 숫자로 따라가기](#6--숫자로-따라가기)
- [7. 🔥 시나리오](#7--시나리오)
- [8. ❓ 헷갈리는 포인트](#8--헷갈리는-포인트)
- [9. 🧾 한 장 요약](#9--한-장-요약)

---

## 1. 핵심 개념 (What)

### 함수형 데코레이터(functional decorator)

Resilience4j 의 모든 컴포넌트는 "실행 가능한 것을 받아서, 같은 타입의 실행 가능한 것을 돌려준다". 예를 들어 `CircuitBreaker.decorateSupplier()` 의 시그니처는 이렇다.

```java
static <T> Supplier<T> decorateSupplier(CircuitBreaker circuitBreaker, Supplier<T> supplier)
```

`Supplier<T>` 가 들어가고 `Supplier<T>` 가 나온다. 반환 타입이 입력 타입과 같으므로 **합성(composition)이 공짜로 가능**하다. `Retry.decorateSupplier(retry, CircuitBreaker.decorateSupplier(cb, supplier))` 처럼 몇 겹이든 쌓을 수 있고, 타입 시스템이 조립 가능 여부를 보장한다.

감싸는 대상은 Java 표준 함수형 인터페이스(`Supplier`, `Function`, `Runnable`, `Callable`, `Consumer`, `CompletionStage`)와, 체크 예외를 던질 수 있도록 Resilience4j 가 직접 정의한 `core/functions/` 의 인터페이스들이다.

| 인터페이스 | 추상 메서드 | 용도 |
|---|---|---|
| `CheckedSupplier<T>` | `T get() throws Throwable` | 값을 반환하며 체크 예외를 던지는 호출 |
| `CheckedFunction<T, R>` | `R apply(T t) throws Throwable` | 입력을 받아 변환하는 호출 |
| `CheckedRunnable` | `void run() throws Throwable` | 반환값 없는 호출 |
| `CheckedConsumer<T>` | `void accept(T t) throws Throwable` | 입력만 받는 호출 |
| `Either<L, R>` | — | 성공/실패를 한 값으로 표현 (`Left`=예외, `Right`=결과) |

### 5개 핵심 컴포넌트가 각각 막는 것

| 컴포넌트 | 막는 것 | 판단 근거 | 거부 시 예외 |
|---|---|---|---|
| **CircuitBreaker** | 이미 죽은 상대를 계속 때리는 것 | 슬라이딩 윈도 안의 실패율·느린호출률 | `CallNotPermittedException` |
| **RateLimiter** | 상대가 허용한 속도를 넘는 것 | 갱신 주기당 허용 permit 수 | `RequestNotPermitted` |
| **Retry** | 일시적(transient) 실패로 요청을 버리는 것 | 예외/결과 predicate + 최대 시도 횟수 | 마지막 예외 또는 `MaxRetriesExceededException` |
| **Bulkhead** | 느린 의존성 하나가 전체 스레드를 먹는 것 | 동시 실행 수(세마포어) 또는 별도 스레드풀 | `BulkheadFullException` |
| **TimeLimiter** | 영원히 안 끝나는 호출 | `timeoutDuration` | `TimeoutException` |

기본값도 함께 외워두면 코드 리뷰에서 바로 쓸 수 있다.

- **CircuitBreaker** (`CircuitBreakerConfig` 상수)
  - `DEFAULT_FAILURE_RATE_THRESHOLD = 50` (%) — 윈도 안 호출 중 실패 비율이 이 값 이상이면 OPEN 으로 넘어간다.
  - `DEFAULT_SLOW_CALL_RATE_THRESHOLD = 100` (%) — "느린 호출" 비율 기준. 기본 100% 라 **전부 느려야** 열린다.
  - `DEFAULT_SLOW_CALL_DURATION_THRESHOLD = 60` (초) — 이보다 오래 걸린 호출을 "느린 호출"로 센다. 기본 60초라 사실상 느린 호출 판정이 꺼져 있는 셈이다.
  - `DEFAULT_SLIDING_WINDOW_TYPE = COUNT_BASED` — 최근 N**건**으로 판단한다(시간 기반은 `TIME_BASED`).
  - `DEFAULT_SLIDING_WINDOW_SIZE = 100` — 최근 100건을 기억하는 윈도 크기.
  - `DEFAULT_MINIMUM_NUMBER_OF_CALLS = 100` — 이만큼 호출이 쌓이기 전에는 실패율을 계산하지 않는다(= 서킷이 안 열린다).
  - `DEFAULT_WAIT_DURATION_IN_OPEN_STATE = 60` (초) — OPEN 으로 막아 두는 시간. 지나면 HALF_OPEN 으로 시험한다.
  - `DEFAULT_PERMITTED_CALLS_IN_HALF_OPEN_STATE = 10` — HALF_OPEN 에서 시험 삼아 통과시키는 호출 수. 이 결과로 CLOSED/OPEN 을 정한다.
  - 기록 대상 예외 기본값 = **모든 예외를 실패로** 센다(`recordException` 기본 `throwable -> true`, ignore 기본 없음). 비즈니스 예외를 거르지 않으면 서킷이 오염된다.
- **RateLimiter** (`RateLimiterConfig.Builder` 기본값 — 상수가 아니라 필드 초기값)
  - `limitForPeriod = 50` — 한 주기에 허용하는 호출 수.
  - `limitRefreshPeriod = 500ns` — 허용 수가 다시 채워지는 주기. 기본값은 사실상 "제한 없음"에 가까워서 그대로 쓰면 안 된다.
  - `timeoutDuration = 5s` — 허용을 못 받았을 때 기다리는 최대 시간. 지나면 `RequestNotPermitted`.
- **Retry** (`RetryConfig` 상수)
  - `DEFAULT_MAX_ATTEMPTS = 3` — **첫 호출 포함** 총 시도 횟수(재시도는 2번).
  - `DEFAULT_WAIT_DURATION = 500` (ms) — 시도 사이 대기. 기본은 고정 간격이라 백오프·지터가 없다.
  - 재시도 대상 예외 기본값 = **모든 예외**를 재시도한다. 비멱등 호출에 그대로 붙이면 이중 처리가 난다.
- **Bulkhead** (`BulkheadConfig` 상수, 세마포어 방식)
  - `DEFAULT_MAX_CONCURRENT_CALLS = 25` — 동시에 들어갈 수 있는 호출 수(세마포어 permit 수).
  - `DEFAULT_MAX_WAIT_DURATION = 0s` — 자리가 없을 때 기다리지 않고 바로 `BulkheadFullException`.
  - `DEFAULT_FAIR_CALL_HANDLING_STRATEGY_ENABLED = true` — 공정(FIFO) 세마포어. 순서는 지키지만 처리량은 조금 손해다.
- **TimeLimiter** (`TimeLimiterConfig` 필드 초기값)
  - `timeoutDuration = 1s` — 이 시간 안에 안 끝나면 `TimeoutException`.
  - `cancelRunningFuture = true` — 시간 초과 시 실행 중인 `Future` 를 취소한다(비동기 호출 대상, 동기 메서드에는 못 붙는다).

> `DEFAULT_MINIMUM_NUMBER_OF_CALLS = 100` 은 리뷰에서 가장 자주 지적할 지점이다. 기본 설정으로는 **100번 호출이 모일 때까지 서킷이 절대 열리지 않는다.** 하루 수십 건 호출되는 배치성 API 에 기본값을 붙여놓고 "왜 안 열려요?" 하는 상황이 여기서 나온다.

## 2. 왜 알아야 하는가 (Why)

### Hystrix 와 무엇이 다른가

| 축 | Hystrix | Resilience4j |
|---|---|---|
| 적용 방식 | `HystrixCommand` 를 **상속**해서 `run()` 구현 | `Supplier`/`Function` 을 **감싸는** static 팩토리 |
| 스레드 격리 | 기본이 스레드풀 격리(강제) | 선택 — `SemaphoreBulkhead`(같은 스레드) 또는 `ThreadPoolBulkhead` |
| 조합 | 커맨드 안에 모든 정책이 섞여 있음 | 5개 컴포넌트를 독립적으로 켜고 끔 |
| 설정 소스 | Archaius | `Registry` + `Config` (불변 설정 객체 + 이름 기반 조회) |
| 외부 의존성 | Archaius, RxJava 등 | **코어는 의존성 0** |

마지막 줄이 핵심이다. `resilience4j-core/build.gradle` 의 `dependencies` 블록에는 **런타임 의존성이 한 줄도 없다**. 테스트 픽스처와 테스트 라이브러리뿐이다. v1 에서 코어가 들고 있던 vavr 는 v2 에서 떼어내 `resilience4j-vavr` 로 분리됐다. 그 흔적은 소스에 그대로 남아 있다 — `core/functions/CheckedSupplier.java` 의 라이선스 헤더는 아직 `Copyright 2014-2019 Vavr, http://vavr.io` 이고, `Either` 의 javadoc 에는 `This interface is similar to {@link io.vavr.control.Either}.` 라고 적혀 있다. vavr 타입(`Try`, `io.vavr.CheckedFunction0`)으로 데코레이트하고 싶으면 `resilience4j-vavr` 의 `VavrCircuitBreaker`, `VavrRetry`, `VavrDecorators` 를 쓴다.

### 실무에서 이게 왜 중요한가

1. **라이브러리 코드에 Resilience4j 를 넣어도 안전하다.** 코어가 의존성 0 이라 버전 충돌이 날 게 없다.
2. **AOP 없이도 쓸 수 있다.** Spring 밖(배치 스크립트, Kafka 컨슈머의 내부 루프, 테스트 코드)에서도 `Decorators` 한 줄로 끝난다. Spring 애너테이션만 알고 있으면 "AOP 가 안 걸리는 self-invocation 상황"에서 손이 묶인다.
3. **데코레이션 순서를 내가 정한다.** 순서에 따라 의미가 완전히 달라진다 (3절).

## 3. 내부 구현 분석 (How)

### 모듈 의존 그래프

`settings.gradle` 에 31개 모듈이 등록되어 있다. 각 모듈 `build.gradle` 의 `api`/`implementation`/`compileOnly` 선언을 따라가면 다음 계층이 나온다.

```mermaid
graph TD
    core["resilience4j-core<br/>(런타임 의존성 0)"]

    cb[resilience4j-circuitbreaker]
    rl[resilience4j-ratelimiter]
    rt[resilience4j-retry]
    bh[resilience4j-bulkhead]
    tl[resilience4j-timelimiter]

    core --> cb
    core --> rl
    core --> rt
    core --> bh
    core --> tl
    core --> cache[resilience4j-cache]
    core --> hedge[resilience4j-hedge]

    mm["resilience4j-micrometer<br/>(Timer + Tagged*Metrics)"]
    cb --> mm
    rl --> mm
    rt --> mm
    bh --> mm
    tl --> mm

    fc[resilience4j-framework-common]
    cb --> fc
    rl --> fc
    rt --> fc
    bh --> fc
    tl --> fc
    mm --> fc

    ann[resilience4j-annotations]
    cons["resilience4j-consumer<br/>CircularEventConsumer"]
    cbuf[resilience4j-circularbuffer]
    cbuf --> cons
    core --> cons

    sp6[resilience4j-spring6]
    fc --> sp6
    ann --> sp6
    cons --> sp6

    sp6 --> sb3[resilience4j-spring-boot3]
    sp6 --> sb4[resilience4j-spring-boot4]
    fc --> mn[resilience4j-micronaut]
    ann --> mn
    fc --> cc[resilience4j-commons-configuration]

    all["resilience4j-all<br/>Decorators"]
    cb --> all
    rl --> all
    rt --> all
    bh --> all
    tl --> all
    cache --> all
    mm --> all

    reactor[resilience4j-reactor]
    rx["resilience4j-rxjava2 / rxjava3"]
    kt[resilience4j-kotlin]
    vavr[resilience4j-vavr]
    cb --> reactor
    cb --> rx
    cb --> kt
    cb --> vavr

    feign[resilience4j-feign]
    dw["resilience4j-metrics<br/>(Dropwizard)"]
    cb --> feign
    cb --> dw
```

읽을 때 주의할 점 두 가지.

- **`resilience4j-micrometer` 는 단순 부가 모듈이 아니다.** `resilience4j-framework-common/build.gradle` 이 `api(project(":resilience4j-micrometer"))` 를 선언한다. 지표 바인딩(`TaggedCircuitBreakerMetrics`)만 주는 게 아니라 `Timer` 라는 **6번째 데코레이터**를 제공하기 때문이다. `Decorators` 의 모든 빌더에 `withTimer(Timer timer)` 가 있다.
- **어댑터 모듈들은 `compileOnly` 를 적극적으로 쓴다.** `resilience4j-kotlin/build.gradle` 은 5개 컴포넌트를 전부 `compileOnly` 로 잡는다. Kotlin 확장함수만 쓰는 사람에게 안 쓰는 컴포넌트가 끌려오지 않게 하려는 의도다. `resilience4j-spring6` 도 Spring·AspectJ·Reactor·RxJava 를 전부 `compileOnly` 로 둔다.

### 🖼 그림으로 보기 — 4층 구조로 줄인 모듈 지도

위 그래프는 화살표가 많아 처음 보면 길을 잃는다. 같은 내용을 "층"으로만 줄이면 이렇다. 아래층은 위층을 모른다 — core 는 CircuitBreaker 를 모르고, CircuitBreaker 는 Spring 을 모른다.

```mermaid
flowchart TD
    subgraph L3["4층 · 프레임워크 통합"]
        direction LR
        s6["spring6"] --- sb["spring-boot3 / 4"] --- mn["micronaut"]
    end
    subgraph L2["3층 · 공통 설정·지표"]
        direction LR
        fc["framework-common<br/>(yml 프로퍼티 → Config)"] --- mm["micrometer<br/>(지표 + Timer)"]
    end
    subgraph L1["2층 · 5개 핵심 컴포넌트"]
        direction LR
        cb["circuitbreaker"] --- rt["retry"] --- rl["ratelimiter"] --- bh["bulkhead"] --- tl["timelimiter"]
    end
    subgraph L0["1층 · 공통 뼈대"]
        core["core<br/>Registry · EventProcessor · Metrics · functions"]
    end
    side["옆 갈래 · 어댑터<br/>reactor / rxjava / kotlin / vavr / feign"]

    L0 --> L1
    L1 --> L2
    L2 --> L3
    L1 -.-> side
```

읽는 법: **Spring 없이 쓰고 싶으면 2층까지만** 가져오면 된다(예제 2). Spring Boot 의 `application.yml` 이 Config 객체로 바뀌는 곳은 3층(`framework-common`)이고, 애너테이션(`@CircuitBreaker`)이 동작하는 곳은 4층이다.

### `decorateSupplier()` 안쪽: 데코레이터 하나의 전체 모습

`CircuitBreaker.decorateSupplier()` 가 하는 일은 20줄이 안 된다. 이 20줄이 Resilience4j 전체의 설계 패턴이다.

```java
static <T> Supplier<T> decorateSupplier(CircuitBreaker circuitBreaker, Supplier<T> supplier) {
    return () -> {
        circuitBreaker.acquirePermission();
        final long start = circuitBreaker.getCurrentTimestamp();
        try {
            T result = supplier.get();
            long duration = circuitBreaker.getCurrentTimestamp() - start;
            circuitBreaker.onResult(duration, circuitBreaker.getTimestampUnit(), result);
            return result;
        } catch (Exception exception) {
            // Do not handle java.lang.Error
            long duration = circuitBreaker.getCurrentTimestamp() - start;
            circuitBreaker.onError(duration, circuitBreaker.getTimestampUnit(), exception);
            throw exception;
        }
    };
}
```

줄 단위로 보면:

- **`return () -> {...}`** — 즉시 실행이 아니다. 새 `Supplier` 를 만들어 돌려준다. 그래서 데코레이션 비용은 람다 객체 하나뿐이고, 애플리케이션 시작 시점에 미리 조립해 둘 수 있다.
- **`acquirePermission()`** — 통과 허가. OPEN 이면 여기서 `CallNotPermittedException` 이 던져지고 원본 `supplier.get()` 은 **호출조차 되지 않는다**. 서킷의 본질은 "호출을 안 하는 것"이다.
- **`getCurrentTimestamp()`** — `System.currentTimeMillis()` 가 아니라 설정 가능한 함수다. `CircuitBreakerConfig` 의 `DEFAULT_TIMESTAMP_FUNCTION = clock -> System.nanoTime()`, `DEFAULT_TIMESTAMP_UNIT = NANOSECONDS`. 테스트에서 시간을 가짜로 주입할 수 있는 이유.
- **`onResult(duration, unit, result)`** — 성공 경로에서도 `onSuccess` 가 아니라 `onResult` 를 부른다. `recordResultPredicate` 로 "200 OK 지만 body 가 에러인 응답"을 실패로 셀 수 있게 하려는 설계다.
- **`catch (Exception exception)`** — `Throwable` 이 아니라 `Exception`. 주석 그대로 `java.lang.Error`(OOM, StackOverflow)는 집계하지 않고 그냥 통과시킨다. OOM 때문에 서킷이 열려 서비스 전체가 멈추는 걸 막는다.
- **`throw exception`** — 삼키지 않는다. 데코레이터는 관찰하고 기록할 뿐, 예외 변환은 `withFallback` 의 몫이다.

5개 컴포넌트의 `decorateXxx()` 는 전부 이 구조의 변형이다. 그래서 하나를 읽으면 나머지가 다 읽힌다.

### 🖼 그림으로 보기 — 데코레이터 하나의 실행 흐름

위 20줄을 갈림길 기준으로 그리면 네 갈래다. 핵심은 **왼쪽 끝 갈래에서 원본 호출이 아예 일어나지 않는다**는 것과, **오른쪽 끝 갈래(`Error`)는 아무것도 기록하지 않는다**는 것이다.

```mermaid
flowchart TD
    start["decorated.get() 호출"] --> perm{"acquirePermission()<br/>통과?"}
    perm -->|"거부 (OPEN)"| rej["CallNotPermittedException<br/>원본 supplier 는 호출되지 않음"]
    perm -->|"통과"| ts["start = getCurrentTimestamp()"]
    ts --> run["supplier.get() — 진짜 원격 호출"]
    run -->|"값 반환"| ok["onResult(duration, unit, result)<br/>recordResultPredicate 로 성공/실패 판정"]
    ok --> ret["result 반환"]
    run -->|"Exception"| err["onError(duration, unit, e)<br/>윈도에 실패(또는 ignore) 기록"]
    err --> rethrow["throw e — 그대로 다시 던짐"]
    run -->|"java.lang.Error"| pass["catch 대상 아님<br/>집계 없이 그대로 전파"]
```

### 데코레이터 체인과 순서

`Decorators` 의 javadoc 이 순서 규칙을 명시한다.

> Decorators are applied in the order of the builder chain. ... This results in the following composition when executing the supplier: `Fallback(Retry(CircuitBreaker(Supplier)))` ... Each Decorator makes its own determination whether an exception represents a failure.

즉 **빌더에서 나중에 호출한 것이 바깥**이다. 체인을 그려보면:

```mermaid
graph LR
    A["Retry<br/>(가장 바깥)"] -->|"호출 위임"| B[CircuitBreaker]
    B -->|"acquirePermission"| C[RateLimiter]
    C -->|"permit 획득"| D[TimeLimiter]
    D -->|"타임아웃 감시"| E[Bulkhead]
    E -->|"세마포어 획득"| F["실제 호출"]
    F -.->|"결과 / 예외가<br/>역순으로 전파"| A
```

이 순서는 Spring Boot 의 AOP 기본값과 일치한다. `resilience4j-spring6` 의 `*ConfigurationProperties` 에 박혀 있는 값이다.

| 애스펙트 | 기본 order | 위치 |
|---|---|---|
| `RetryAspect` | `LOWEST_PRECEDENCE - 5` | 가장 바깥 |
| `CircuitBreakerAspect` | `LOWEST_PRECEDENCE - 4` | |
| `RateLimiterAspect` | `LOWEST_PRECEDENCE - 3` | |
| `TimeLimiterAspect` | `LOWEST_PRECEDENCE - 2` | |
| `BulkheadAspect` | `LOWEST_PRECEDENCE - 1` | |
| `TimerAspect` | `LOWEST_PRECEDENCE` | 가장 안쪽 |

`Ordered` 값이 작을수록 우선순위가 높고 바깥에 온다. **Retry 가 CircuitBreaker 보다 바깥**이라는 점이 중요하다. 재시도가 서킷 바깥이면 "서킷이 OPEN 인데도 재시도가 계속 돌아 `CallNotPermittedException` 을 3번 받는" 동작이 된다. 반대로 Retry 를 안쪽에 두면 재시도 전체가 한 번의 서킷 호출로 집계되어, 실패율이 과소 집계된다. 둘 다 정답이 있는 게 아니라 **의도적으로 선택해야 하는 지점**이고, 리뷰에서 "왜 이 순서냐"를 물을 수 있어야 한다.

### 🖼 그림으로 보기 — 양파 껍질과 호출 한 번의 왕복

위의 일렬 화살표 그림을 "감싸는 관계"로 다시 그리면 양파가 된다. 바깥 껍질일수록 **먼저 들어가고 나중에 나온다.** 요청은 바깥에서 안으로, 결과·예외는 안에서 바깥으로 지나간다.

```mermaid
flowchart LR
    client(["호출자"]) --> R
    subgraph R["Retry — 가장 바깥, 실패하면 통째로 다시"]
        subgraph C["CircuitBreaker — 열려 있으면 여기서 거부"]
            subgraph RL["RateLimiter — 속도 초과면 거부"]
                subgraph T["TimeLimiter — 제한 시간 감시"]
                    subgraph B["Bulkhead — 동시 실행 수 제한"]
                        call["실제 원격 호출"]
                    end
                end
            end
        end
    end
```

호출 한 번이 실제로 어떤 순서로 지나가는지 시간 순서로 펴면 아래와 같다. 첫 시도는 원격 서버가 `IOException` 을 던지고, Retry 가 대기 후 두 번째 시도에서 성공하는 경우다.

```mermaid
sequenceDiagram
    participant U as 호출자
    participant R as Retry
    participant C as CircuitBreaker
    participant L as RateLimiter
    participant T as TimeLimiter
    participant B as Bulkhead
    participant S as 원격 서버

    U->>R: get()
    R->>C: 시도 1
    C->>C: acquirePermission() — CLOSED 라 통과
    C->>L: 위임
    L->>L: permit 1개 소비
    L->>T: 위임
    T->>B: 위임 (타이머 시작)
    B->>B: 세마포어 획득
    B->>S: 호출
    S-->>B: IOException
    B-->>T: 세마포어 반납 후 예외 전파
    T-->>L: 예외 전파
    L-->>C: 예외 전파
    C->>C: onError — 윈도에 실패 1건 기록
    C-->>R: IOException
    R->>R: 대기 (기본 500ms)
    R->>C: 시도 2 — 같은 경로를 처음부터 다시
    C->>S: (L → T → B 를 거쳐) 호출
    S-->>C: 성공 응답
    C->>C: onResult — 윈도에 성공 1건 기록
    C-->>R: 결과
    R-->>U: 결과
```

이 그림에서 읽어야 할 사실 두 개. **(1)** CircuitBreaker 입장에서는 "실패 1 + 성공 1" 로 **2건**이 기록된다 — Retry 가 바깥에 있으면 시도 하나하나가 CircuitBreaker 의 호출이다. **(2)** RateLimiter 도 permit 을 **2개** 쓴다. 재시도가 많아질수록 허용 속도를 빨리 소진한다.

## 4. 실전 예제

### 예제 1 — 5개 컴포넌트를 모두 엮기 (프레임워크 없음)

TimeLimiter 는 비동기 호출에만 의미가 있으므로 `Decorators.ofCompletionStage()` 로 시작한다. `DecorateSupplier` 에는 `withTimeLimiter` 가 없다 — `withTimeLimiter(TimeLimiter, ScheduledExecutorService)` 는 `DecorateCompletionStage` 에만 선언되어 있다. 리뷰에서 자주 보는 실수다.

```java
package com.example.resilience;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

public final class PaymentGateway {

    private final Supplier<CompletionStage<String>> decorated;

    public PaymentGateway(Supplier<CompletionStage<String>> remoteCall,
                          ScheduledExecutorService scheduler) {

        CircuitBreaker circuitBreaker = CircuitBreaker.of("payment", CircuitBreakerConfig.custom()
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(200)
            .minimumNumberOfCalls(20)          // 기본값 100 은 저트래픽에서 영원히 안 열린다
            .failureRateThreshold(50f)
            .slowCallDurationThreshold(Duration.ofSeconds(2))
            .slowCallRateThreshold(60f)
            .waitDurationInOpenState(Duration.ofSeconds(10))
            .permittedNumberOfCallsInHalfOpenState(5)
            .ignoreExceptions(IllegalArgumentException.class)   // 400 류는 서버 장애가 아니다
            .build());

        RateLimiter rateLimiter = RateLimiter.of("payment", RateLimiterConfig.custom()
            .limitForPeriod(100)
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .timeoutDuration(Duration.ZERO)    // 대기 없이 즉시 거부
            .build());

        Bulkhead bulkhead = Bulkhead.of("payment", BulkheadConfig.custom()
            .maxConcurrentCalls(30).maxWaitDuration(Duration.ZERO).build());

        TimeLimiter timeLimiter = TimeLimiter.of("payment", TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(3)).cancelRunningFuture(true).build());

        Retry retry = Retry.of("payment", RetryConfig.custom()
            .maxAttempts(3)
            .intervalFunction(io.github.resilience4j.core.IntervalFunction
                .ofExponentialRandomBackoff(Duration.ofMillis(200), 2.0))
            .retryExceptions(IOException.class, TimeoutException.class)
            .ignoreExceptions(CallNotPermittedException.class)  // OPEN 이면 재시도 무의미
            .build());

        // 빌더 순서 = 안쪽부터. 결과 체인:
        // Fallback( Retry( CircuitBreaker( RateLimiter( TimeLimiter( Bulkhead(call) ) ) ) ) )
        this.decorated = Decorators.ofCompletionStage(remoteCall)
            .withBulkhead(bulkhead)
            .withTimeLimiter(timeLimiter, scheduler)
            .withRateLimiter(rateLimiter)
            .withCircuitBreaker(circuitBreaker)
            .withRetry(retry, scheduler)
            .withFallback(List.of(CallNotPermittedException.class, TimeoutException.class),
                throwable -> "DEGRADED")
            .decorate();
    }

    public CompletionStage<String> charge() {
        return decorated.get();
    }
}
```

주목할 점:

- `withRetry(retry, scheduler)` — `DecorateCompletionStage` 의 Retry 는 **스케줄러를 요구한다**. 비동기 경로에서 `Thread.sleep()` 백오프는 이벤트 루프 스레드를 잡아먹으므로, 내부적으로 `Retry.decorateCompletionStage(retry, scheduler, stageSupplier)` 가 스케줄러에 재시도를 예약한다.
- `ignoreExceptions(CallNotPermittedException.class)` — OPEN 이라 거부된 건을 재시도하면 OPEN 기간 내내 똑같이 거부된다. 무의미한 지연만 늘어난다.
- `withFallback(List.of(...), ...)` — 전체를 삼키면(`withFallback(e -> ...)`) 실제 버그가 조용히 묻힌다.

### 예제 2 — 순수 Java, 동기 호출 한 줄 조립

Spring 밖(배치, CLI, 테스트)에서 쓰는 최소 형태.

```java
package com.example.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;

import java.util.function.Supplier;

public final class LegacySoapClientRunner {

    // Registry 는 애플리케이션당 하나. 인스턴스를 매번 new 하면 윈도가 초기화된다.
    private static final CircuitBreakerRegistry CB_REGISTRY = CircuitBreakerRegistry.ofDefaults();
    private static final RetryRegistry RETRY_REGISTRY = RetryRegistry.ofDefaults();

    public static void main(String[] args) {
        CircuitBreaker cb = CB_REGISTRY.circuitBreaker("legacy-soap");
        Retry retry = RETRY_REGISTRY.retry("legacy-soap");

        // 이벤트 구독은 동기 호출이다 — 여기서 블로킹하면 호출 경로가 막힌다 (03 문서 참조)
        cb.getEventPublisher().onStateTransition(e ->
            System.out.printf("[CB] %s -> %s%n", e.getStateTransition().getFromState(),
                                                 e.getStateTransition().getToState()));

        // decorate() 는 조립된 Supplier 를 반환. 시작 시 한 번 만들어 필드에 보관하는 게 정석.
        Supplier<String> guarded = Decorators.ofSupplier(LegacySoapClientRunner::callLegacy)
            .withCircuitBreaker(cb)
            .withRetry(retry)
            .withFallback(throwable -> "cached-value")
            .decorate();

        for (int i = 0; i < 500; i++) {
            System.out.println(guarded.get());
        }
    }

    private static String callLegacy() {
        if (Math.random() < 0.3) {
            throw new IllegalStateException("SOAP fault");
        }
        return "ok";
    }
}
```

## 5. 정리

| 항목 | 내용 |
|---|---|
| 설계 철학 | 상속·프록시가 아니라 `Supplier`/`Function` 을 감싸는 함수형 데코레이터 |
| 핵심 시그니처 | `static <T> Supplier<T> decorateSupplier(CircuitBreaker, Supplier<T>)` — 입력 타입 = 반환 타입이라 무한 합성 가능 |
| 체크 예외 대응 | `core/functions/` 의 `CheckedSupplier`, `CheckedFunction`, `CheckedRunnable`, `CheckedConsumer`, `Either` |
| 코어 의존성 | 0개. v1 의 vavr 의존은 `resilience4j-vavr` 로 분리 |
| 모듈 계층 | core → 5개 컴포넌트 → micrometer → framework-common → spring6/spring-boot3·4/micronaut, 별도로 어댑터(reactor/rxjava/kotlin/vavr)와 부가(metrics/cache/feign/hedge/circularbuffer/consumer) |
| 데코레이션 순서 | 빌더에서 **나중에 호출한 것이 바깥**. Spring AOP 기본은 Retry(-5) → CircuitBreaker(-4) → RateLimiter(-3) → TimeLimiter(-2) → Bulkhead(-1) → Timer |
| `Error` 처리 | `decorateSupplier` 는 `catch (Exception)` — `java.lang.Error` 는 집계하지 않고 통과 |
| 가장 흔한 함정 | `minimumNumberOfCalls` 기본값 100. 저트래픽 API 에서 서킷이 영원히 안 열림 |
| 두 번째 함정 | `withTimeLimiter` 는 `DecorateCompletionStage` 에만 있음. 동기 `Supplier` 에는 타임아웃을 걸 수 없다 |

## 6. 🔢 숫자로 따라가기

"Retry 가 CircuitBreaker 바깥"이라는 말이 숫자로 어떤 차이를 만드는지 따라간다. 원격 서버는 완전히 죽어 있어 모든 호출이 `IOException` 을 던진다고 가정한다.

- Retry: 기본값 그대로 — `maxAttempts=3`(첫 호출 포함), 대기 500ms 고정, **모든 예외 재시도**
- CircuitBreaker: `COUNT_BASED`, `slidingWindowSize=10`, `minimumNumberOfCalls=5`, `failureRateThreshold=50`
- 조립: `Decorators.ofSupplier(call).withCircuitBreaker(cb).withRetry(retry)` → `Retry(CircuitBreaker(call))`

| 시각 | 사용자 요청 | Retry 시도 | CircuitBreaker 판정 | 윈도 (기록/실패) | 실패율 | 상태 | 원격 호출 |
|---|---|---|---|---|---|---|---|
| 0.0s | A | 1 | 통과 → 실패 기록 | 1 / 1 | 판단 보류(1 < 5) | CLOSED | 1회째 |
| 0.5s | A | 2 | 통과 → 실패 기록 | 2 / 2 | 판단 보류 | CLOSED | 2회째 |
| 1.0s | A | 3 | 통과 → 실패 기록 | 3 / 3 | 판단 보류 | CLOSED | 3회째 → A 는 `IOException` 으로 끝남 |
| 1.2s | B | 1 | 통과 → 실패 기록 | 4 / 4 | 판단 보류 | CLOSED | 4회째 |
| 1.7s | B | 2 | 통과 → 실패 기록 | 5 / 5 | **100% ≥ 50%** | **OPEN** | 5회째 |
| 2.2s | B | 3 | `acquirePermission()` 거부 | 5 / 5 (거부는 기록 안 됨) | — | OPEN | 없음 → B 는 `CallNotPermittedException` 으로 끝남 |
| 3.0s | C | 1 | 거부 | 5 / 5 | — | OPEN | 없음 |
| 3.5s | C | 2 | 거부 | 5 / 5 | — | OPEN | 없음 |
| 4.0s | C | 3 | 거부 | 5 / 5 | — | OPEN | 없음 → C 는 1초를 기다린 뒤 `CallNotPermittedException` |

여기서 나오는 결론 세 가지.

1. **사용자 요청 2건이 CircuitBreaker 에는 실패 5건으로 보인다.** `minimumNumberOfCalls=5` 를 "사용자 요청 5건"으로 생각하면 틀린다. Retry 가 바깥이면 기준은 "시도 수"다.
2. **요청 C 는 원격 서버를 한 번도 안 불렀는데 1초를 기다렸다.** Retry 기본값이 `CallNotPermittedException` 까지 재시도하기 때문이다. 예제 1처럼 `ignoreExceptions(CallNotPermittedException.class)` 를 주면 C 는 0초 만에 실패(또는 fallback)한다.
3. **거부된 시도는 윈도에 들어가지 않는다.** OPEN 상태의 거부는 `circuitBreakerMetrics.onCallNotPermitted()` 로 별도 카운터만 올린다. 윈도는 5/5 그대로다.

## 7. 🔥 시나리오

**상황: "결제 승인 API" 가 외부 승인 서버를 비동기로 호출한다.** 설정은 위 예제 1(`PaymentGateway`)과 같다 — TimeLimiter 3초, Bulkhead 30, RateLimiter 초당 100, CircuitBreaker(윈도 200 / 최소 20 / 실패율 50% / 느린 호출 2초·60% / OPEN 10초 / HALF_OPEN 5건), Retry 3회(`IOException`·`TimeoutException` 만, `CallNotPermittedException` 무시), fallback `"DEGRADED"`.

| 순서 | 외부 상황 | 어느 층이 움직이나 | 사용자가 보는 것 | 운영자가 보는 것 |
|---|---|---|---|---|
| ① | 평상시. 승인 서버 응답 200ms | 모든 층이 통과만 시킨다 | 정상 승인 | 지표 평온 |
| ② | 승인 서버가 4초씩 걸리기 시작 | **TimeLimiter** 가 3초에서 `TimeoutException`. CircuitBreaker 는 이를 실패이자 느린 호출(`SLOW_ERROR`, 3초 > 2초)로 기록. **Retry** 가 `TimeoutException` 이라 재시도 | 응답이 길게는 "3초 × 3회 + 백오프" 만큼 늦다가 `"DEGRADED"` | 타임아웃·재시도 이벤트 증가 |
| ③ | 늦은 요청이 쌓여 동시 실행이 30개 도달 | **Bulkhead** 가 31번째를 `BulkheadFullException` 으로 즉시 거부. Bulkhead 가 CircuitBreaker 안쪽이라 이 거부도 **CircuitBreaker 실패로 기록**된다(기본값은 모든 예외를 실패로 셈) | fallback 대상이 아니라 그대로 오류 | Bulkhead 거부 지표 상승 |
| ④ | 윈도의 실패율이 50% 이상 (기록 20건 이상) | **CircuitBreaker** 가 OPEN. 이후 호출은 `acquirePermission()` 에서 거부되어 승인 서버로 나가지 않는다. Retry 는 `CallNotPermittedException` 을 무시하므로 재시도하지 않는다 | 즉시 `"DEGRADED"` (기다림 없음) | 상태 전이 이벤트 CLOSED → OPEN |
| ⑤ | 10초 뒤 | CircuitBreaker 가 HALF_OPEN 으로 넘어가 5건만 시험 통과 | 일부는 정상 처리, 나머지는 `"DEGRADED"` | OPEN → HALF_OPEN |
| ⑥ | 승인 서버 복구 | 시험 5건 결과가 임계값 아래면 CLOSED | 정상 승인 | HALF_OPEN → CLOSED |

③ 이 이 시나리오의 함정이다. Bulkhead 가 "우리 쪽 자원 보호"로 거부한 것까지 "상대 서버 장애"로 세면 서킷이 의도보다 빨리 열린다. 그게 싫으면 CircuitBreaker 에 `ignoreExceptions(BulkheadFullException.class)` 를 주거나 순서를 바꾼다. 순서를 정하는 일이 곧 "무엇을 상대 장애로 볼 것인가"를 정하는 일이다.

## 8. ❓ 헷갈리는 포인트

- **Q. `decorateSupplier()` 를 부르는 순간 원격 호출이 나가나?**
  A. 아니다. 새 `Supplier` 람다를 돌려줄 뿐이다. 호출은 돌려받은 객체의 `get()` 을 부를 때 일어난다. 그래서 시작 시 한 번 조립해 필드에 두는 게 정석이다.
- **Q. `Decorators` 빌더에서 먼저 쓴 것이 바깥인가?**
  A. 반대다. **나중에 쓴 것이 바깥**이다. `.withCircuitBreaker(cb).withRetry(retry)` 는 `Retry(CircuitBreaker(call))` 이다.
- **Q. Retry 가 3번 시도하면 CircuitBreaker 에는 1건으로 기록되나?**
  A. Retry 가 바깥이면 **3건**이다(6절 표). Retry 가 안쪽이면 재시도 묶음 전체가 1건이다.
- **Q. `OutOfMemoryError` 도 실패율에 잡히나?**
  A. 안 잡힌다. `decorateSupplier` 는 `catch (Exception)` 만 하므로 `java.lang.Error` 는 기록 없이 통과한다.
- **Q. 동기 메서드에도 TimeLimiter 로 타임아웃을 걸 수 있나?**
  A. `Decorators.ofSupplier()` 체인에는 `withTimeLimiter` 가 없다. `CompletionStage` 로 바꾸거나, 클라이언트 라이브러리 자체의 타임아웃(커넥션·읽기 타임아웃)을 써야 한다.

## 9. 🧾 한 장 요약

- Resilience4j 는 상속이 아니라 **함수를 감싼다** — `Supplier` 를 넣으면 `Supplier` 가 나오므로 몇 겹이든 쌓인다.
- 모듈은 **core → 5개 컴포넌트 → framework-common·micrometer → spring/micronaut** 4층. core 는 런타임 의존성이 0이다.
- 데코레이터 하나는 `acquirePermission → 실행 → onResult/onError → 그대로 반환/재던짐` 20줄짜리 구조다.
- OPEN 이면 **원본 호출 자체가 일어나지 않는다** — 그게 서킷의 본질이다.
- 빌더에서 **나중에 쓴 것이 바깥**. Spring 기본 순서는 Retry → CircuitBreaker → RateLimiter → TimeLimiter → Bulkhead.
- Retry 가 바깥이면 **시도마다** CircuitBreaker 에 기록되고, OPEN 거부(`CallNotPermittedException`)까지 재시도하지 않도록 막아야 한다.
- 안쪽 컴포넌트의 거부 예외(예: `BulkheadFullException`)도 바깥 CircuitBreaker 의 실패로 세어진다. 순서 = 무엇을 장애로 볼지의 결정이다.
- `java.lang.Error` 는 집계하지 않고, 동기 `Supplier` 에는 TimeLimiter 를 걸 수 없다.

---

## 관련 문서
- 선행: 없음 (이 문서가 시작점)
- 후행: [Registry와 설정 해석 — 인스턴스는 어디서 오는가](./02-registry-and-config.md)
- 참고: [Spring Boot 자동 설정](../advanced/01-spring-boot-autoconfiguration.md), [AOP 애스펙트](../advanced/02-aop-aspects.md), [TimeLimiter와 조합](./12-timelimiter-and-composition.md)
---
*Resilience4j v2.4.0 기준 · 소스 커밋 7e3ab525*

*마지막 업데이트: 2026년 10월*
