# Reactor 테스트와 디버깅 — StepVerifier, 가상 시간, 그리고 쓸 수 있는 스택트레이스

리액티브 코드는 비동기라서 일반 assertion으로는 검증이 어렵고, 스택트레이스는 조립 지점을 잃어버려 쓸모가 없습니다. `reactor-test`의 `StepVerifier`/`VirtualTimeScheduler`/`TestPublisher`와 `reactor-tools`의 `ReactorDebugAgent`가 그 두 구멍을 메웁니다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### `reactor-test` 구성

```
reactor-test/src/main/java/reactor/test/
├── StepVerifier.java              — 선언형 검증 스크립트 (인터페이스)
├── StepVerifierOptions.java       — initialRequest, withInitialContext, virtualTimeSchedulerSupplier 등
├── DefaultStepVerifierBuilder.java— 실제 구현
├── publisher/
│   ├── TestPublisher.java         — 수동 제어 Publisher (+ Violation으로 규약 위반 시뮬레이션)
│   ├── DefaultTestPublisher.java  — hot 구현
│   ├── ColdTestPublisher.java     — cold 구현
│   └── PublisherProbe.java        — 구독/취소/요청 여부만 관찰
├── scheduler/
│   └── VirtualTimeScheduler.java  — 시간을 수동으로 전진
├── subscriber/
│   ├── TestSubscriber.java        — 저수준 Subscriber 검증
│   └── ConditionalTestSubscriber.java
└── util/
    ├── RaceTestUtils.java         — 경합 테스트
    ├── TestLogger.java / LoggerUtils.java
```

### `StepVerifier` 체인 타입

| 타입 | 위치 | 할 수 있는 것 |
|---|---|---|
| `FirstStep<T>` | 체인 시작 (`Step<T>` 상속) | `expectSubscription`, `expectFusion`, `expectNoFusionSupport`, `enableConditionalSupport` + `Step`의 전부 |
| `Step<T>` | 값 단계 (`LastStep` 상속) | `expectNext*`, `assertNext`, `thenRequest`, `thenAwait`, `expectNoEvent`, `recordWith`, `thenConsumeWhile`, `expectAccessibleContext` |
| `LastStep` | 종료 단계 | `expectComplete`, `expectError*`, `expectTimeout`, `thenCancel`, `verify*` 단축형 |
| `StepVerifier` | 완성된 스크립트 | `verify()`, `verify(Duration)`, `verifyThenAssertThat()` |
| `Assertions` | 사후 검증 | `hasDropped*`, `hasDiscarded*`, `hasOperatorErrors*`, `tookLessThan`, `tookMoreThan` |

### 디버깅 도구

| 도구 | 비용 | 범위 |
|---|---|---|
| `Flux#checkpoint(String)` | 거의 없음 (스택 수집 안 함) | 해당 지점만 |
| `Flux#checkpoint()` / `checkpoint(desc, true)` | 스택 수집 1회 (조립 시) | 해당 지점만 |
| `Hooks.onOperatorDebug()` | **모든 연산자 조립마다 스택 수집** | 전역 |
| `ReactorDebugAgent` (reactor-tools) | 바이트코드 계측, 런타임 비용 사실상 없음 | 전역 |
| `Scannable` | 조회 시점만 | 런타임 체인 관찰 |

---

## 2. 왜 알아야 하는가 (Why)

### `verify()`를 부르지 않으면 아무것도 검증되지 않는다

이게 가장 흔한 함정입니다.

```java
@Test
void 통과하는_것처럼_보이지만_아무것도_안_한다() {
    StepVerifier.create(Flux.just(1, 2, 3))
            .expectNext(99)          // 틀린 기대값
            .expectNext(98)
            .expectComplete();       // ← verify() 가 없다
    // 테스트가 그냥 통과한다
}
```

`StepVerifier`는 **스크립트를 조립하는 빌더**입니다. `expectNext(...)`는 기대값을 리스트에 추가할 뿐이고, 구독도 검증도 일어나지 않습니다. 클래스 Javadoc이 명시합니다 (`reactor-test/src/main/java/reactor/test/StepVerifier.java:44-46`).

> *"The verification must be triggered after the terminal expectations (completion, error, cancellation) have been declared, by calling one of the `verify()` methods."*

`expectComplete()`의 반환 타입이 `StepVerifier`(= 아직 실행 안 된 스크립트)라는 것이 타입 수준의 힌트입니다. 이 함정을 피하는 가장 쉬운 방법은 **`verify` 접두 단축형을 쓰는 것**입니다.

```java
.verifyComplete();                       // = expectComplete().verify()
.verifyError(MyException.class);         // = expectError(...).verify()
.verifyErrorMessage("boom");
.verifyErrorSatisfies(e -> { ... });
.verifyTimeout(Duration.ofSeconds(1));
```

`verifyComplete()`는 `Duration`을 반환하므로 "값을 반환하는 메서드"가 체인 끝에 오고, 호출을 빼먹기 어렵습니다. 그리고 기본 verify 타임아웃은 `Duration.ZERO`(= 무제한)입니다 (`StepVerifier.java:97`). CI에서 테스트가 영원히 멈추는 걸 막으려면 `verify(Duration)`을 쓰거나 `StepVerifier.setDefaultTimeout(...)`을 설정해야 합니다 (`StepVerifier.java:110`).

### 실제 시간을 기다리면 테스트가 느려지고 불안정해진다

```java
// 나쁜 테스트: 1시간을 실제로 기다린다
Flux.interval(Duration.ofHours(1)).take(3)
```

`VirtualTimeScheduler`는 `Schedulers`의 팩토리를 교체해 시간을 "변수"로 만듭니다. `thenAwait(Duration)`이 실제 sleep이 아니라 **스케줄러의 시계를 전진**시켜 예약된 작업을 즉시 실행합니다. 1시간 간격 테스트가 밀리초 단위로 끝납니다.

### 리액티브 스택트레이스가 쓸모없는 이유

```
java.lang.ArithmeticException: / by zero
	at com.example.MyService.lambda$process$0(MyService.java:42)
	at reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onNext(FluxMapFuseable.java:113)
	at reactor.core.publisher.FluxFilterFuseable$FilterFuseableSubscriber.onNext(FluxFilterFuseable.java:118)
	at reactor.core.publisher.FluxRange$RangeSubscription.slowPath(FluxRange.java:156)
	at reactor.core.publisher.FluxRange$RangeSubscription.request(FluxRange.java:109)
	...
```

문제는 **"이 체인이 어디서 조립됐는지"가 없다**는 것입니다. 스택은 신호가 흘러온 경로(런타임 호출 스택)만 보여 주고, `.map(...)`을 어느 파일 몇 번째 줄에 썼는지는 사라져 있습니다. 조립과 실행이 분리된 구조의 필연적 결과입니다 (→ [02-assembly-vs-subscribe-time.md](02-assembly-vs-subscribe-time.md)).

더구나 같은 서비스에서 `map`을 10군데 쓰고 있다면 `FluxMapFuseable.java:113`은 아무 정보도 주지 않습니다. **어느 `map`인가**를 알아야 하고, 그게 traceback(assembly trace)이 제공하는 값입니다.

---

## 3. 내부 구현 분석 (How)

### 3.1 `StepVerifier`는 스크립트 빌더다

`StepVerifier.create(publisher)`는 `DefaultStepVerifierBuilder.newVerifier(options, () -> publisher)`를 호출합니다 (`StepVerifier.java:165-167`).

```java
static <T> FirstStep<T> create(Publisher<? extends T> publisher, StepVerifierOptions options) {
	return DefaultStepVerifierBuilder.newVerifier(options, () -> publisher);
}
```

`create(publisher)` → `create(publisher, Long.MAX_VALUE)` → `create(publisher, StepVerifierOptions.create().initialRequest(n))`로 환원됩니다 (`StepVerifier.java:134-151`). **기본 initial request가 `Long.MAX_VALUE`** 라는 것이 중요합니다 — 배압을 테스트하려면 명시적으로 줄여야 합니다.

`thenAwait`/`thenRequest` 같은 호출은 스크립트 리스트에 이벤트를 추가합니다 (`DefaultStepVerifierBuilder.java:707-710`).

```java
public DefaultStepVerifierBuilder<T> thenAwait(Duration timeshift) {
	// ...
	this.script.add(new WaitEvent<>(timeshift, "thenAwait"));
	return this;
}
```

`verify()`가 호출될 때 비로소 구독이 일어나고 스크립트가 순서대로 소비됩니다.

```
  StepVerifier.create(...)          → FirstStep (빌더)
       .expectNext("a")             → script.add(SignalEvent)
       .thenRequest(1)              → script.add(RequestEvent)
       .expectComplete()            → script.add(SignalEvent) → StepVerifier 반환
       .verify()                    → ★ 여기서 subscribe + 스크립트 실행 + AssertionError
```

### 3.2 `withVirtualTime` — 팩토리 교체의 타이밍

`withVirtualTime`은 `Supplier<Publisher>`를 받습니다. 왜 `Publisher`를 직접 받지 않는지가 핵심입니다.

```java
// StepVerifier.java:220-222
static <T> FirstStep<T> withVirtualTime(Supplier<? extends Publisher<? extends T>> scenarioSupplier,
		long n) {
	return withVirtualTime(scenarioSupplier, () -> VirtualTimeScheduler.getOrSet(true), n);
}
```

`VirtualTimeScheduler.getOrSet(true)`를 호출해 전역 `Schedulers` 팩토리를 교체한 **다음에** supplier를 실행해야 합니다. `Flux.interval(...)`은 조립 시점에 `Schedulers.parallel()`을 캡처하므로, 팩토리 교체 전에 조립했다면 실제 스케줄러를 들고 있게 되고 가상 시간이 안 먹습니다.

`DefaultStepVerifierBuilder`의 verify 경로가 그 순서를 지킵니다 (`DefaultStepVerifierBuilder.java:841-853`).

```java
final VirtualTimeScheduler vts;
// ... vtsLookup.get() 으로 생성
VirtualTimeScheduler.set(vts);      // ① 팩토리 교체
// ... scenarioSupplier.get() 으로 publisher 조립 후 구독 ②
VirtualTimeScheduler.reset();       // ③ 정리
```

`VirtualTimeScheduler` 팩토리 메서드의 차이:

| 메서드 | 전역 팩토리 교체 | `defer` |
|---|---|---|
| `create()` | 안 한다 | `false` (`VirtualTimeScheduler.java:57`) |
| `create(boolean defer)` | 안 한다 | 인자 (`:71`) |
| `getOrSet()` | **한다** | `false` (`:90`) |
| `getOrSet(boolean defer)` | **한다** | 인자 (`:110`) |
| `getOrSet(VirtualTimeScheduler)` | **한다** (이미 있으면 기존 것 반환) | — (`:130`) |
| `set(VirtualTimeScheduler)` | **한다** (무조건 교체) | — (`:149`) |

`defer=true`는 시간 관련 연산을 **지연 평가**합니다. `withVirtualTime`이 `getOrSet(true)`를 쓰는 이유는, 팩토리 교체 시점과 조립 시점 사이의 미세한 경합을 없애기 위해서입니다.

시간 전진 API는 셋입니다 (`VirtualTimeScheduler.java:274-293`).

```java
public void advanceTime()                      // advanceTimeBy(Duration.ZERO)
public void advanceTimeBy(Duration delayTime)
public void advanceTimeTo(Instant instant)
```

`StepVerifier.thenAwait(Duration)`이 내부적으로 `advanceTimeBy`를 호출합니다. 테스트 종료 시 `VirtualTimeScheduler.reset()`으로 전역 상태를 되돌리는 것을 잊으면 **다른 테스트가 가상 시간에 걸려 멈춥니다.** `withVirtualTime`을 쓰면 이 정리를 자동으로 해 주고, `create()`로 직접 만들어 `set()`한 경우에는 `@AfterEach`에서 직접 `reset()`해야 합니다.

### 3.3 `TestPublisher`와 `Violation`

`TestPublisher<T>`는 `Publisher<T>`이면서 `PublisherProbe<T>`입니다 (`publisher/TestPublisher.java:48`). 즉 값을 밀어넣는 동시에 구독·취소·요청을 검증할 수 있습니다.

| 팩토리 | 성질 |
|---|---|
| `create()` | hot. 구독 전 값은 사라짐. RS 규약 준수 (`:56`) |
| `createCold()` | cold. 구독 전 값을 버퍼링해 늦은 구독자에게 재생 (`:85`) |
| `createColdNonBuffering()` | cold, 버퍼링 없음 (`:102`) |
| `createNoncompliant(Violation first, Violation... rest)` | **규약 위반을 허용** (`:69`) |
| `createColdNonCompliant(boolean errorOnOverflow, Violation...)` | cold + 위반 (`:119`) |

`Violation` 네 가지 (`publisher/TestPublisher.java:275-297`):

| 값 | 허용하는 위반 |
|---|---|
| `REQUEST_OVERFLOW` | request보다 많이 `next()` 호출 (`IllegalStateException` 없이) |
| `ALLOW_NULL` | `next(null)` (`NullPointerException` 없이) |
| `CLEANUP_ON_TERMINATE` | `complete()`/`error()`/`emit()`을 여러 번 |
| `DEFER_CANCELLATION` | 취소를 무시하고 계속 신호 방출 |

이것이 `TestPublisher`의 진짜 가치입니다. **내가 만든 연산자가 못된 업스트림을 만났을 때 어떻게 되는지**를 테스트할 수 있습니다. 실제 라이브러리나 서드파티 `Publisher`가 규약을 지키지 않는 경우가 있고, 그때 내 코드가 터지는지 버티는지는 테스트로만 알 수 있습니다.

검증 메서드도 풍부합니다 — `assertMinRequested(long)`, `assertMaxRequested(long)`, `assertSubscribers(int)`, `assertNoSubscribers()`, `assertCancelled(int)`, `assertRequestOverflow()` (`publisher/TestPublisher.java:140-212`).

편의 메서드 `emit(T...)`은 `next` 반복 + `complete`입니다 (`publisher/TestPublisher.java:264-270`).

### 3.4 `PublisherProbe` — 분기를 검증한다

`PublisherProbe<T>`는 "이 Publisher가 구독됐는가"만 묻는 도구입니다 (`publisher/PublisherProbe.java:48`).

```java
default void assertWasSubscribed()    / assertWasNotSubscribed()
default void assertWasCancelled()     / assertWasNotCancelled()
default void assertWasRequested()     / assertWasNotRequested()
Mono<T> mono()  /  Flux<T> flux()
static <T> PublisherProbe<T> of(Publisher<? extends T> source)   // :159
static <T> PublisherProbe<T> empty()                             // :171
```

용도가 명확합니다 — `switchIfEmpty`, `onErrorResume`, `Mono.firstWithValue` 같은 **분기 연산자가 어느 쪽을 탔는지** 검증합니다. 반환값으로는 알 수 없는 경우가 있습니다 (둘 다 같은 값을 내놓는 경우, 둘 다 empty인 경우).

`mono()`와 `flux()`는 **같은 probe를 공유**합니다 (`publisher/PublisherProbe.java:105-111` 주석). 둘 중 어느 쪽을 구독해도 같은 카운터가 올라갑니다.

### 3.5 traceback — `checkpoint`와 `Hooks.onOperatorDebug()`

`Flux.checkpoint(description, forceStackTrace)`의 전부 (`reactor-core/src/main/java/reactor/core/publisher/Flux.java:3688-3698`):

```java
public final Flux<T> checkpoint(@Nullable String description, boolean forceStackTrace) {
	final AssemblySnapshot stacktrace;
	if (!forceStackTrace) {
		stacktrace = new CheckpointLightSnapshot(description);
	}
	else {
		stacktrace = new CheckpointHeavySnapshot(description, Traces.callSiteSupplierFactory.get());
	}
	return new FluxOnAssembly<>(this, stacktrace);
}
```

세 오버로드의 차이가 여기서 갈립니다 (`Flux.java:3628`, `3653`, `3688`).

| 호출 | forceStackTrace | 스냅샷 | 비용 |
|---|---|---|---|
| `checkpoint()` | `true` | `CheckpointHeavySnapshot` | 스택 수집 |
| `checkpoint(String)` | `false` | `CheckpointLightSnapshot` | **설명 문자열만** |
| `checkpoint(String, true)` | `true` | `CheckpointHeavySnapshot` | 스택 수집 |

즉 **`checkpoint("설명")`은 비용이 사실상 없습니다.** 운영 코드에 그대로 둘 수 있습니다. 설명만 잘 적으면 어느 체인인지 식별할 수 있으니, 실무에서는 이 변종을 씁니다.

`Hooks.onOperatorDebug()`는 전역 플래그 하나입니다 (`Hooks.java:336-339`).

```java
public static void onOperatorDebug() {
	log.debug("Enabling stacktrace debugging via onOperatorDebug");
	GLOBAL_TRACE = true;
}
```

그리고 `Flux.onAssembly()`가 모든 연산자 조립 시 이 플래그를 봅니다 (`Flux.java:10955-10965`).

```java
protected static <T> Flux<T> onAssembly(Flux<T> source) {
	Function<Publisher, Publisher> hook = Hooks.onEachOperatorHook;
	if(hook != null) {
		source = (Flux<T>) hook.apply(source);
	}
	if (Hooks.GLOBAL_TRACE) {
		AssemblySnapshot stacktrace = new AssemblySnapshot(null, Traces.callSiteSupplierFactory.get());
		source = (Flux<T>) Hooks.addAssemblyInfo(source, stacktrace);
	}
	return source;
}
```

**비용이 여기서 명확해집니다.** `onAssembly`는 거의 모든 연산자 호출에서 실행되므로, `GLOBAL_TRACE`가 켜지면 `.map().filter().flatMap()` 체인 하나가 조립될 때마다 스택을 세 번 수집하고 래퍼 연산자를 세 개 추가합니다. 조립이 요청마다 일어나는 서버 코드(핸들러 안에서 체인을 만드는 일반적 패턴)에서는 **요청마다** 그 비용이 붙습니다. Javadoc도 "운영에서 켜지 말라"는 취지로 적혀 있습니다 (`Hooks.java:327-335`).

### 3.6 `ReactorDebugAgent` — 비용 없이 콜사이트를 복원한다

`reactor-tools`는 다른 접근을 씁니다. 스택을 **런타임에 수집**하는 대신, **클래스 로딩 시 바이트코드에 콜사이트 정보를 심습니다.**

```java
// reactor-tools/src/main/java/reactor/tools/agent/ReactorDebugAgent.java:48-59
public static synchronized void init() {
	if (System.getProperty(INSTALLED_PROPERTY) != null) {
		return;
	}
	if (instrumentation != null) {
		return;
	}
	instrumentation = ByteBuddyAgent.install();
	instrument(instrumentation);
}
```

`instrument()`가 등록하는 `ClassFileTransformer`는 이렇게 필터링합니다 (`ReactorDebugAgent.java:61-119`).

```java
if (loader == null) return null;                      // 부트스트랩 클래스 제외
if (className == null
		|| className.startsWith("java/")
		|| className.startsWith("jdk/")
		|| className.startsWith("sun/")
		|| className.startsWith("com/sun/")
		|| className.startsWith("reactor/core/")) {    // ← Reactor 자신도 제외
	return null;
}
// 사용자 클래스만 ReactorDebugClassVisitor로 재작성
```

`reactor/core/`를 제외하는 게 핵심입니다. **사용자 코드에서 연산자를 호출하는 지점**만 계측하므로 "어느 파일 몇 번째 줄의 `map`인가"를 알 수 있습니다.

계측 방식이라 **런타임에 스택을 수집하지 않습니다.** 조립 시점의 비용은 상수에 가깝고, 그래서 운영에도 쓸 수 있는 유일한 전역 traceback입니다.

```
Hooks.onOperatorDebug()        ReactorDebugAgent
──────────────────────         ─────────────────
조립마다 Thread 스택 수집   vs  클래스 로딩 시 1회 바이트코드 재작성
+ 래퍼 연산자 추가              + 상수 비용
→ 개발/디버깅 전용              → 운영 가능
```

두 가지 설치 방법이 있습니다.

| 방법 | 코드 | 비고 |
|---|---|---|
| JVM 에이전트 | `-javaagent:reactor-tools.jar` | 모든 클래스 계측 |
| 런타임 self-attach | `ReactorDebugAgent.init()` | main 최상단에서 호출 |
| + 이미 로드된 클래스 | `ReactorDebugAgent.processExistingClasses()` | `init()` 직후 |

`processExistingClasses()`는 `init()` 이전에 이미 로드된 클래스를 다시 변환합니다. `instrumentation`이 `null`이면 `IllegalStateException("Must be initialized first!")`를 던지고 (`ReactorDebugAgent.java:128-130`), `-javaagent`로 실행 중이면 no-op입니다 (`:123-126`).

### 3.7 `Scannable` — 런타임 체인 내부를 들여다본다

`Scannable`은 연산자 내부 상태를 타입 안전한 키로 조회하는 인터페이스입니다.

```java
// reactor-core/src/main/java/reactor/core/Scannable.java:396-404
static Scannable from(@Nullable Object o) {
	if (o == null) return Attr.NULL_SCAN;
	if (o instanceof Scannable) return ((Scannable) o);
	return Attr.UNAVAILABLE_SCAN;
}
```

`Scannable.Attr` 주요 키 (`Scannable.java:87-229`):

| 키 | 타입 | 의미 |
|---|---|---|
| `PARENT` | `Scannable` | 직접 업스트림 (`:172`) |
| `ACTUAL` | `Scannable` | 직접 다운스트림 (`:87`) |
| `ACTUAL_METADATA` | `Boolean` | 이 단계를 체인 집계에 포함할지 — `steps()`가 사용 (`:98`) |
| `PREFETCH` | `Integer` | 처리 속도(prefetch) (`:193`) |
| `CAPACITY` | `Integer` | `PREFETCH`가 없을 때의 용량 (`:119`) |
| `BUFFERED` | `Integer` | 현재 버퍼에 쌓인 개수 (`:110`) |
| `LARGE_BUFFERED` | `Long` | `BUFFERED`의 long 버전 (`:155`) |
| `REQUESTED_FROM_DOWNSTREAM` | `Long` | 다운스트림의 미충족 demand (`:200`) |
| `CANCELLED` | `Boolean` | 다운스트림이 취소했는가 (`:127`) |
| `TERMINATED` | `Boolean` | 업스트림이 종료했는가 (`:208`) |
| `ERROR` | `Throwable` | 보관 중인 에러 (`:142`) |
| `DELAY_ERROR` | `Boolean` | 에러 지연 모드 (`:136`) |
| `NAME` | `String` | `Flux#name(String)`으로 붙인 이름 (`:160`) |
| `TAGS` | `Stream<Tuple2<String,String>>` | `Flux#tag(k,v)` (`:214`) |
| `RUN_ON` | `Scannable` | 실행 중인 `Worker`/`Scheduler` (`:182`) |
| `RUN_STYLE` | `RunStyle` | `SYNC`/`ASYNC`/`UNKNOWN` (`:222`) |
| `LIFTER` | `String` | lift 훅 이름 (`:229`) |

탐색 메서드:

```java
default <T> T scan(Attr<T> key)                     // :562
default <T> T scanOrDefault(Attr<T> key, T def)     // :581
default Stream<? extends Scannable> parents()       // :531  업스트림 재귀
default Stream<? extends Scannable> actuals()       // :413  다운스트림 재귀
default Stream<? extends Scannable> inners()        // :422  내부 publisher (flatMap 등)
default Stream<String> steps()                      // :497  체인 전체를 문자열로
default String name()                               // :442  NAME 또는 부모의 NAME 또는 stepName()
default String stepName()                           // :459  클래스명에서 연산자명 추출
```

`steps()`는 `parents()`를 역순으로 + 자신 + `actuals()`를 이어 붙여 **체인 전체**를 보여 줍니다 (`Scannable.java:497-504`). 커스텀 연산자를 만들거나 "왜 fusion이 안 되는지" 조사할 때 가장 빠른 확인 수단입니다.

---

## 4. 실전 예제

### 4.1 `StepVerifier` 기본 — 반드시 `verify`로 끝낸다

```java
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

class StepVerifierBasicsTest {

    @Test
    void 값과_완료() {
        StepVerifier.create(Flux.just("a", "b", "c"))
                .expectNext("a")
                .expectNext("b", "c")          // 가변 인자로 여러 개
                .verifyComplete();
    }

    @Test
    void 개수만_검증() {
        StepVerifier.create(Flux.range(1, 100))
                .expectNextCount(100)
                .verifyComplete();
    }

    @Test
    void 조건으로_검증() {
        StepVerifier.create(Flux.just(2, 4, 6))
                .expectNextMatches(i -> i % 2 == 0)
                .thenConsumeWhile(i -> i % 2 == 0)   // 조건이 유지되는 동안 소비
                .verifyComplete();
    }

    @Test
    void assertNext_로_AssertJ_쓰기() {
        record Order(String id, int amount) {}

        StepVerifier.create(Mono.just(new Order("o-1", 5000)))
                .assertNext(o -> {
                    org.assertj.core.api.Assertions.assertThat(o.id()).isEqualTo("o-1");
                    org.assertj.core.api.Assertions.assertThat(o.amount()).isPositive();
                })
                .verifyComplete();
    }

    @Test
    void 에러_검증_세_가지_층위() {
        Flux<Integer> failing = Flux.concat(Flux.just(1), Flux.error(new IllegalStateException("boom")));

        // ① 타입만
        StepVerifier.create(failing).expectNext(1)
                .verifyError(IllegalStateException.class);

        // ② 메시지까지
        StepVerifier.create(failing).expectNext(1)
                .verifyErrorMessage("boom");

        // ③ 임의 검증
        StepVerifier.create(failing).expectNext(1)
                .verifyErrorSatisfies(e -> {
                    assert e instanceof IllegalStateException;
                    assert e.getMessage().contains("boo");
                });
    }

    @Test
    void 타임아웃을_반드시_걸자() {
        // 기본 verify 타임아웃은 Duration.ZERO = 무제한
        StepVerifier.create(Flux.just(1, 2))
                .expectNext(1, 2)
                .expectComplete()
                .verify(Duration.ofSeconds(5));     // CI에서 멈추지 않게
    }

    @Test
    void 무한_스트림은_thenCancel_로_끝낸다() {
        StepVerifier.create(Flux.interval(Duration.ofMillis(10)))
                .expectNext(0L, 1L, 2L)
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }
}
```

### 4.2 배압 검증 — `initialRequest`와 `thenRequest`

```java
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.test.StepVerifierOptions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

class BackpressureTest {

    @Test
    void initialRequest_0_에서_시작해_수동으로_당긴다() {
        StepVerifier.create(Flux.just(1, 2, 3, 4), 0)   // initial request = 0
                .expectSubscription()
                .expectNoEvent(Duration.ofMillis(50))   // 아직 아무 신호도 없어야 한다
                .thenRequest(2)
                .expectNext(1, 2)
                .expectNoEvent(Duration.ofMillis(50))
                .thenRequest(2)
                .expectNext(3, 4)
                .verifyComplete();
    }

    @Test
    void 소스가_요청량을_지키는지_확인한다() {
        List<Long> requests = new ArrayList<>();

        Flux<Integer> observed = Flux.range(1, 10)
                .doOnRequest(requests::add);

        StepVerifier.create(observed, 0)
                .thenRequest(3)
                .expectNext(1, 2, 3)
                .thenRequest(7)
                .expectNextCount(7)
                .verifyComplete();

        assert requests.equals(List.of(3L, 7L));
    }

    @Test
    void options_로_초기_Context까지_넣는다() {
        StepVerifierOptions options = StepVerifierOptions.create()
                .initialRequest(1)
                .withInitialContext(reactor.util.context.Context.of("tenant", "acme"))
                .scenarioName("tenant-aware-read");

        StepVerifier.create(
                        reactor.core.publisher.Mono.deferContextual(
                                ctx -> reactor.core.publisher.Mono.just(ctx.<String>get("tenant"))),
                        options)
                .expectNext("acme")
                .verifyComplete();
    }
}
```

`expectNoEvent(Duration)`가 배압 테스트의 핵심 도구입니다 — "이 시간 동안 아무 신호도 오지 않아야 한다"를 검증합니다. `expectSubscription()`을 먼저 호출하지 않으면 `onSubscribe` 자체가 "이벤트"로 잡혀 실패하니 주의하세요.

### 4.3 가상 시간 — 1시간 테스트를 밀리초로

```java
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

class VirtualTimeTest {

    @Test
    void interval_을_즉시_검증한다() {
        StepVerifier.withVirtualTime(() -> Flux.interval(Duration.ofHours(1)).take(3))
                .expectSubscription()
                .expectNoEvent(Duration.ofMinutes(59))
                .thenAwait(Duration.ofMinutes(1))
                .expectNext(0L)
                .thenAwait(Duration.ofHours(1))
                .expectNext(1L)
                .thenAwait(Duration.ofHours(1))
                .expectNext(2L)
                .verifyComplete();
        // 실제 소요 시간: 밀리초 단위
    }

    @Test
    void delayElements_검증() {
        StepVerifier.withVirtualTime(() ->
                        Flux.just("a", "b", "c").delayElements(Duration.ofSeconds(10)))
                .expectSubscription()
                .thenAwait(Duration.ofSeconds(10)).expectNext("a")
                .thenAwait(Duration.ofSeconds(10)).expectNext("b")
                .thenAwait(Duration.ofSeconds(10)).expectNext("c")
                .verifyComplete();
    }

    @Test
    void 백오프_재시도의_지연을_검증한다() {
        AtomicInteger attempts = new AtomicInteger();

        // supplier 안에서 조립해야 가상 시간이 적용된다
        StepVerifier.withVirtualTime(() ->
                        Mono.fromCallable(() -> {
                                    attempts.incrementAndGet();
                                    throw new IllegalStateException("always fails");
                                })
                                .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(30))))
                .expectSubscription()
                .expectNoEvent(Duration.ofSeconds(29))      // 첫 재시도 전
                .thenAwait(Duration.ofSeconds(1))
                .expectNoEvent(Duration.ofSeconds(29))      // 두 번째 재시도 전
                .thenAwait(Duration.ofSeconds(1))
                .expectErrorSatisfies(e -> {
                    assert reactor.core.Exceptions.isRetryExhausted(e);
                })
                .verify(Duration.ofSeconds(5));             // 실제 벽시계 기준

        assert attempts.get() == 3;   // 최초 1회 + 재시도 2회
    }

    @Test
    void timeout_검증() {
        StepVerifier.withVirtualTime(() ->
                        Mono.never().timeout(Duration.ofSeconds(5)))
                .expectSubscription()
                .thenAwait(Duration.ofSeconds(5))
                .verifyError(java.util.concurrent.TimeoutException.class);
    }
}
```

**`withVirtualTime`의 람다 안에서 체인을 조립해야 합니다.** 밖에서 만든 `Flux`를 넘기면 이미 실제 `Schedulers.parallel()`을 캡처한 상태라 `thenAwait`가 아무 효과도 없고, 테스트가 영원히 대기합니다. 이것이 `withVirtualTime`이 `Publisher`가 아니라 `Supplier<Publisher>`를 받는 이유입니다 (3.2절).

`verify(Duration)`의 인자는 **실제 벽시계 시간**입니다 — 가상 시간과 무관하게 "이 테스트가 5초 넘게 걸리면 실패"라는 안전망입니다.

### 4.4 `TestPublisher` — 규약 위반을 시뮬레이션

```java
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

class TestPublisherTest {

    @Test
    void 신호_타이밍을_직접_제어한다() {
        TestPublisher<String> publisher = TestPublisher.create();

        StepVerifier.create(publisher.flux().map(String::toUpperCase))
                .then(() -> publisher.next("a"))
                .expectNext("A")
                .then(() -> publisher.next("b", "c"))
                .expectNext("B", "C")
                .then(publisher::complete)
                .verifyComplete();
    }

    @Test
    void 요청량을_검증한다() {
        TestPublisher<Integer> publisher = TestPublisher.create();

        StepVerifier.create(publisher.flux().take(2), 0)
                .thenRequest(2)
                .then(() -> {
                    publisher.assertMinRequested(2);
                    publisher.next(1, 2);
                })
                .expectNext(1, 2)
                .verifyComplete();

        publisher.assertCancelled();   // take(2)가 상류를 취소했다
    }

    @Test
    void REQUEST_OVERFLOW_위반으로_내_연산자를_공격한다() {
        TestPublisher<Integer> rude = TestPublisher.createNoncompliant(
                TestPublisher.Violation.REQUEST_OVERFLOW);

        // request(1)만 했는데 3개를 밀어넣는다 — 내 체인이 어떻게 반응하는가
        StepVerifier.create(rude.flux().onBackpressureBuffer(), 1)
                .then(() -> rude.next(1, 2, 3))
                .expectNext(1)
                .thenRequest(2)
                .expectNext(2, 3)
                .then(rude::complete)
                .verifyComplete();

        rude.assertRequestOverflow();   // 실제로 오버플로가 일어났음을 확인
    }

    @Test
    void ALLOW_NULL_위반으로_null_안전성을_검증한다() {
        TestPublisher<String> nullable = TestPublisher.createNoncompliant(
                TestPublisher.Violation.ALLOW_NULL);

        StepVerifier.create(nullable.flux().filter(java.util.Objects::nonNull))
                .then(() -> nullable.next("ok"))
                .expectNext("ok")
                .then(() -> nullable.next((String) null))
                // filter가 null을 어떻게 다루는지가 드러난다
                .then(nullable::complete)
                .verifyComplete();
    }

    @Test
    void cold_는_늦은_구독자에게_재생한다() {
        TestPublisher<Integer> cold = TestPublisher.createCold();
        cold.next(1, 2, 3);                      // 구독자 0명

        StepVerifier.create(cold.flux())
                .expectNext(1, 2, 3)             // 재생된다
                .then(cold::complete)
                .verifyComplete();
    }

    @Test
    void hot_은_구독_전_값을_잃는다() {
        TestPublisher<Integer> hot = TestPublisher.create();
        hot.next(1, 2, 3);                       // 사라진다

        StepVerifier.create(hot.flux())
                .then(() -> hot.next(4))
                .expectNext(4)
                .then(hot::complete)
                .verifyComplete();
    }
}
```

마지막 두 테스트의 대비가 `create()` vs `createCold()`의 실질적 차이입니다. 내 연산자가 hot 소스를 전제하는지 cold를 전제하는지를 테스트로 고정해 두면, 나중에 소스를 바꿀 때 회귀를 잡을 수 있습니다.

### 4.5 `PublisherProbe` — 분기를 검증

```java
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.PublisherProbe;

class PublisherProbeTest {

    // 테스트 대상: 캐시 미스 시 DB로 떨어진다
    static Mono<String> loadUser(Mono<String> cache, Mono<String> db) {
        return cache.switchIfEmpty(db);
    }

    @Test
    void 캐시_히트면_DB를_건드리지_않는다() {
        PublisherProbe<String> dbProbe = PublisherProbe.of(Mono.just("from-db"));

        StepVerifier.create(loadUser(Mono.just("from-cache"), dbProbe.mono()))
                .expectNext("from-cache")
                .verifyComplete();

        dbProbe.assertWasNotSubscribed();    // ← 반환값만으로는 알 수 없는 것
    }

    @Test
    void 캐시_미스면_DB로_떨어진다() {
        PublisherProbe<String> dbProbe = PublisherProbe.of(Mono.just("from-db"));

        StepVerifier.create(loadUser(Mono.empty(), dbProbe.mono()))
                .expectNext("from-db")
                .verifyComplete();

        dbProbe.assertWasSubscribed();
        dbProbe.assertWasRequested();
    }

    @Test
    void 취소가_전파되는지_확인한다() {
        PublisherProbe<String> slowProbe = PublisherProbe.of(Mono.never());

        StepVerifier.create(slowProbe.mono())
                .expectSubscription()
                .thenCancel()
                .verify();

        slowProbe.assertWasCancelled();
    }

    @Test
    void empty_probe_는_신호_없이_관찰만() {
        PublisherProbe<Object> probe = PublisherProbe.empty();

        StepVerifier.create(Mono.just("x").then(probe.mono()))
                .verifyComplete();

        probe.assertWasSubscribed();
    }
}
```

첫 테스트가 `PublisherProbe`의 존재 이유입니다. `expectNext("from-cache")`만으로는 "DB를 안 건드렸다"를 증명할 수 없습니다. 캐시 레이어 테스트에서 **불필요한 DB 호출이 없음**을 보장해야 할 때 이게 유일한 수단입니다.

### 4.6 `Context` 검증

```java
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

class ContextAssertionTest {

    @Test
    void 체인이_보는_Context를_검증한다() {
        StepVerifier.create(
                        Mono.just("v")
                                .contextWrite(Context.of("traceId", "t-1", "tenant", "acme")))
                .expectAccessibleContext()
                .hasKey("traceId")
                .contains("tenant", "acme")
                .hasSize(2)
                .then()                      // Step으로 복귀
                .expectNext("v")
                .verifyComplete();
    }

    @Test
    void Context가_없음을_검증한다() {
        StepVerifier.create(Mono.just("v"))
                .expectNoAccessibleContext()
                .expectNext("v")
                .verifyComplete();
    }
}
```

`expectAccessibleContext()`는 `ContextExpectations<T>`를 반환하고 (`StepVerifier.java:843`), `hasKey`/`hasSize`/`contains`/`containsAllOf`/`containsOnly`/`assertThat`을 체인한 뒤 `then()`으로 `Step`으로 돌아옵니다 (`StepVerifier.java:1278-1353`).

### 4.7 디버깅 — 세 가지 수단 비교

```java
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.tools.agent.ReactorDebugAgent;

public class DebuggingDemo {

    // ① checkpoint(String) — 비용 거의 없음. 운영에도 둘 수 있다
    static Flux<Integer> withCheckpoint() {
        return Flux.just(1, 2, 0, 4)
                .map(i -> 10 / i)
                .checkpoint("order-amount-division")   // forceStackTrace = false
                .filter(i -> i > 1);
    }

    // ② checkpoint() — 스택 수집. 개발 중 임시로
    static Flux<Integer> withHeavyCheckpoint() {
        return Flux.just(1, 2, 0, 4)
                .map(i -> 10 / i)
                .checkpoint();                         // forceStackTrace = true
    }

    // ③ 전역 traceback
    public static void main(String[] args) {
        // 개발 전용 — 조립마다 스택을 수집한다
        if (Boolean.getBoolean("app.debug.reactor")) {
            Hooks.onOperatorDebug();
        }

        // 운영 가능 — 바이트코드 계측. main 최상단에서
        ReactorDebugAgent.init();
        ReactorDebugAgent.processExistingClasses();   // init 이전 로드분까지

        withCheckpoint().subscribe(
                System.out::println,
                e -> e.printStackTrace());
    }
}
```

선택 기준:

| 상황 | 선택 |
|---|---|
| 특정 체인을 식별하고 싶다 (운영 포함) | `checkpoint("설명")` — 비용 없음 |
| 특정 체인의 조립 위치까지 알고 싶다 (개발) | `checkpoint()` |
| 어디서 터지는지 전혀 모른다 (개발/테스트) | `Hooks.onOperatorDebug()` |
| 운영에서 전역 traceback이 필요하다 | `ReactorDebugAgent` + `-javaagent` 또는 `init()` |

`Hooks.onOperatorDebug()`를 운영에 켜는 선택은 피하세요 — `onAssembly`가 호출되는 모든 지점에서 스택을 수집하고 래퍼 연산자를 추가합니다 (3.5절).

### 4.8 `Scannable`로 체인 내부 관찰

```java
import org.junit.jupiter.api.Test;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;

import java.util.stream.Collectors;

class ScannableTest {

    @Test
    void 체인_단계를_문자열로_본다() {
        Flux<Integer> chain = Flux.range(1, 10)
                .name("order-pipeline")
                .tag("domain", "order")
                .filter(i -> i % 2 == 0)
                .map(i -> i * 10)
                .publishOn(reactor.core.scheduler.Schedulers.parallel());

        Scannable scannable = Scannable.from(chain);

        System.out.println("steps  = " + scannable.steps().collect(Collectors.toList()));
        System.out.println("name   = " + scannable.name());
        System.out.println("prefetch = " + scannable.scan(Scannable.Attr.PREFETCH));
        System.out.println("runStyle = " + scannable.scan(Scannable.Attr.RUN_STYLE));

        // 업스트림을 거슬러 올라간다
        scannable.parents().forEach(p ->
                System.out.println("  parent: " + p.stepName()
                        + " prefetch=" + p.scanOrDefault(Scannable.Attr.PREFETCH, -1)));

        // name()은 NAME 속성을 찾거나, 부모에서 찾거나, stepName()으로 떨어진다
        assert scannable.isScanAvailable();
    }

    @Test
    void 태그를_확인한다() {
        Flux<Integer> tagged = Flux.range(1, 3)
                .tag("team", "payments")
                .tag("env", "test");

        // tags()는 Stream<Tuple2<String, String>>를 반환한다 (Scannable.java:605)
        String tags = Scannable.from(tagged)
                .tags()
                .map(t -> t.getT1() + "=" + t.getT2())
                .collect(Collectors.joining(", "));
        System.out.println("tags = " + tags);
    }

    @Test
    void Scannable이_아니면_스캔할_수_없다() {
        // Attr.NULL_SCAN / Attr.UNAVAILABLE_SCAN 상수 자체는 패키지 전용이므로
        // 사용자 코드에서는 isScanAvailable()로 판별한다
        assert !Scannable.from("just a string").isScanAvailable();
        assert !Scannable.from(null).isScanAvailable();
    }
}
```

`Scannable`은 **커스텀 연산자를 만들 때** 가장 유용합니다. `scanUnsafe(Attr)`를 제대로 구현하지 않으면 `steps()`에서 체인이 끊겨 보이고, `Hooks.onOperatorDebug()`의 traceback도 부정확해집니다. 또 "fusion이 왜 안 되는지"를 `RUN_STYLE`과 `parents()`로 추적할 수 있습니다 (→ [06-operator-fusion.md](06-operator-fusion.md)).

실행 가능한 코드로 값을 확인하도록 `System.out.println`으로 써 뒀습니다 — `steps()`의 정확한 출력 문자열은 연산자 조합과 fusion 적용 여부에 따라 달라지므로 `assertEquals`로 고정하지 않는 편이 안전합니다.

---

## 5. 정리

| 항목 | 내용 |
|---|---|
| 최대 함정 | **`verify()`를 부르지 않으면 구독도 검증도 일어나지 않는다.** `verifyComplete()`/`verifyError*()` 단축형을 쓰자 |
| 기본 타임아웃 | `Duration.ZERO` = 무제한 (`StepVerifier.java:97`). CI에서는 `verify(Duration)` 또는 `setDefaultTimeout` |
| 기본 request | `Long.MAX_VALUE`. 배압 테스트는 `create(pub, 0)` + `thenRequest` |
| 체인 타입 | `FirstStep`(fusion/subscription) → `Step`(값) → `LastStep`(종료) → `StepVerifier`(실행) → `Assertions`(사후) |
| `expectNoEvent` | "이 시간 동안 아무 신호 없음". `expectSubscription()`을 먼저 호출해야 함 |
| 가상 시간 | `withVirtualTime(Supplier<Publisher>)`. **람다 안에서 조립**해야 함 — 팩토리 교체 후 조립 순서가 필수 |
| `VirtualTimeScheduler` | `create*`는 전역 교체 없음, `getOrSet*`/`set`은 전역 교체. 직접 `set`했다면 `reset()` 필수 |
| `TestPublisher` | `create()`(hot) / `createCold()` / `createNoncompliant(Violation...)`. Violation: `REQUEST_OVERFLOW`, `ALLOW_NULL`, `CLEANUP_ON_TERMINATE`, `DEFER_CANCELLATION` |
| `PublisherProbe` | 구독/취소/요청 여부만 검증. 분기 연산자 테스트의 유일한 수단. `mono()`/`flux()`는 같은 probe 공유 |
| 스택트레이스 문제 | 런타임 호출 경로만 남고 **조립 위치가 사라진다** (조립/실행 분리의 결과) |
| `checkpoint(String)` | `CheckpointLightSnapshot` — 스택 수집 없음. 운영 가능 |
| `checkpoint()` | `CheckpointHeavySnapshot` — 스택 수집. 개발용 |
| `Hooks.onOperatorDebug()` | `GLOBAL_TRACE=true`. `Flux.onAssembly()`가 **모든 연산자 조립마다** 스택 수집 + 래퍼 추가. 개발 전용 |
| `ReactorDebugAgent` | ByteBuddy로 **클래스 로딩 시 1회 계측**. `reactor/core/` 제외해 사용자 콜사이트만 복원. `init()` + `processExistingClasses()` |
| `Scannable` | `Attr` 키로 런타임 내부 관찰 (`PARENT`/`ACTUAL`/`PREFETCH`/`BUFFERED`/`REQUESTED_FROM_DOWNSTREAM`/`RUN_STYLE`/`NAME`/`TAGS`). `steps()`로 체인 전체 |

---

## 관련 문서
- 선행: [조립 시점과 구독 시점](02-assembly-vs-subscribe-time.md) — 스택트레이스가 왜 조립 위치를 잃는지
- 선행: [연산자 fusion](06-operator-fusion.md) — `expectFusion`/`RUN_STYLE`로 검증하기
- 선행: [스케줄러와 스레딩](08-schedulers-and-threading.md) — `VirtualTimeScheduler`가 교체하는 대상
- 선행: [Sinks: 명령형 코드와의 경계](11-sinks.md)
- 후행: [WebClient와 커넥터](../advanced/01-webclient-builder-connectors.md)

---
*참고: Reactor Core 3.7.19 / Reactor Test 3.7.19 / Reactor Tools 3.7.19 기준*
