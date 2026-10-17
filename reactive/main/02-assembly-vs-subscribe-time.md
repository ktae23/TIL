# 조립 시점 vs 구독 시점

"구독하지 않으면 아무 일도 일어나지 않는다(Nothing happens until you subscribe)"는 표어를 소스로 증명한다. 연산자를 이어 붙이는 **조립 시점**에는 객체 그래프만 만들어지고, `subscribe()`가 호출되는 **구독 시점**에 비로소 구독자 체인이 역방향으로 조립되며 데이터가 흐른다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

Reactor 코드의 수명은 뚜렷하게 두 단계로 갈린다.

| | 조립 시점 (assembly time) | 구독 시점 (subscribe time) |
|---|---|---|
| 트리거 | `.map()`, `.filter()`, `Flux.just()` … | `subscribe()`, `block()`, `blockFirst()` … |
| 하는 일 | `Publisher` 객체를 **생성해서 소스를 가리키게** 함 | `Subscriber` 객체를 **생성해서 다운스트림을 가리키게** 함 |
| 결과물 | `Publisher` 그래프 (업스트림 방향 링크) | `Subscriber` 체인 (다운스트림 방향 링크) |
| 횟수 | 체인을 정의할 때 **한 번** | 구독할 때마다 **매번** |
| 적용되는 훅 | `Hooks.onEachOperator` (`Flux.onAssembly()`) | `Hooks.onLastOperator` (`Operators.onLastAssembly()`) |
| 데이터 | 흐르지 않음 | 흐름 |

조립 시점에 만들어진 그래프는 **불변이고 재사용 가능하다**. 같은 `Flux`에 두 번 `subscribe()`하면 그래프는 공유되지만 구독자 체인은 두 벌 만들어진다.

```java
Flux<String> pipeline = Flux.range(1, 3)      // 조립: FluxRange 생성
                            .map(i -> i * 2)   // 조립: FluxMapFuseable 생성 (FluxRange 를 가리킴)
                            .filter(i -> i>2); // 조립: FluxFilterFuseable 생성 (FluxMapFuseable 를 가리킴)
// 여기까지 아무 데이터도 흐르지 않았다.

pipeline.subscribe(System.out::println);       // 구독: 여기서 비로소 흐른다
```

---

## 2. 왜 알아야 하는가 (Why)

### 2.1 조립 시점에 실행되는 코드는 "흐름 밖"이다

가장 흔한 버그가 여기서 나온다.

```java
// 잘못된 코드: userRepository.findById 가 조립 시점에 즉시 호출된다
Mono<User> bad = Mono.just(userId)
                     .flatMap(id -> Mono.just(userRepository.findById(id)));  // ← 블로킹 호출이 조립 중
```

`Mono.just(...)`의 인자는 `just`를 **호출하는 순간** 평가된다. 즉 `findById`는 `flatMap`의 람다가 실행될 때가 아니라, 람다 바깥 — 아니, 정확히는 람다 안이지만 그 람다가 `onNext`에 호출될 때 — 이라는 구분이 헷갈리기 쉽다. 더 명확한 사례는 이쪽이다.

```java
// 조립 시점에 즉시 실행됨
Mono<User> eager = Mono.just(userRepository.findById(id));  // findById 가 지금 실행된다

// 구독 시점으로 미뤄짐
Mono<User> lazy = Mono.fromCallable(() -> userRepository.findById(id));
```

`Mono.fromCallable`은 `Callable`을 품고 있다가 구독 시점에 호출한다 (`publisher/Mono.java:523`). 이 차이를 알려면 "어느 코드가 조립 시점에 돌고 어느 코드가 구독 시점에 도는가"를 구분할 수 있어야 한다.

### 2.2 디버깅 스택트레이스가 두 종류다

예외가 터졌을 때 스택이 `subscribe()` 호출 지점부터 시작하면 구독 시점 예외, 체인을 만든 코드 줄부터 시작하면 조립 시점 예외다. Reactor의 디버그 모드(`Hooks.onOperatorDebug()`)가 하는 일은 정확히 **조립 시점의 스택을 캡처해 두고 구독 시점 예외에 붙여주는 것**이다. `Flux.onAssembly()` 안에 이 분기가 있다 (`publisher/Flux.java:10960-10963`). 자세한 건 [테스트와 디버깅](12-testing-debugging.md) 참고.

### 2.3 깊은 체인에서 StackOverflow가 나지 않는 이유

이게 이 문서의 핵심이다. `.map()`을 10,000번 이어 붙인 체인을 구독하면, 순진하게 구현하면 `subscribe`가 10,000단 재귀로 내려간다. Reactor는 `OptimizableOperator`로 이 재귀를 **while 루프로 평탄화**한다. 이걸 알면 "왜 `subscribeOrReturn`이라는 어색한 이름의 메서드가 있는가"가 설명된다.

---

## 3. 내부 구현 분석 (How)

### 3.1 조립 시점 — `onAssembly`가 만드는 것

```java
public final <V> Flux<V> map(Function<? super T, ? extends V> mapper) {
	if (this instanceof Fuseable) {
		return onAssembly(new FluxMapFuseable<>(this, mapper));
	}
	return onAssembly(new FluxMap<>(this, mapper));
}
```
`publisher/Flux.java:6629-6634`

여기서 벌어지는 일 전부:
1. 소스(`this`)가 `Fuseable`인지 **조립 시점에** 검사한다.
2. 그 결과에 따라 `FluxMapFuseable` 또는 `FluxMap` 인스턴스를 만든다.
3. `onAssembly()`로 훅을 적용한다.

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
`publisher/Flux.java:10954-10965`

`Hooks.onEachOperatorHook`은 **연산자 하나하나마다** 적용된다(`onEach`). 그리고 `GLOBAL_TRACE`가 켜져 있으면 지금 이 순간의 콜사이트를 `AssemblySnapshot`으로 캡처해 둔다. 조립 시점이 어디였는지 기억하는 유일한 수단이다.

새 객체가 만들어지면서 소스는 `final` 필드로 저장된다.

```java
public abstract class FluxOperator<I, O> extends Flux<O> implements Scannable {
	protected final Flux<? extends I> source;

	protected FluxOperator(Flux<? extends I> source) {
		this.source = Objects.requireNonNull(source);
	}
```
`publisher/FluxOperator.java:32-43`

링크는 **다운스트림 → 업스트림** 한 방향뿐이다. `FluxRange`는 자기를 감싼 `FluxMapFuseable`을 모른다. 그래서 같은 소스를 여러 체인이 공유할 수 있다.

```
조립 시점에 만들어지는 Publisher 그래프 (↑ = source 필드)

   [FluxFilterFuseable]      ← pipeline 변수가 가리키는 객체
          ↑ source
   [FluxMapFuseable]
          ↑ source
   [FluxRange]               ← SourceProducer, source 필드 없음
```

### 3.2 구독 시점 — `Flux.subscribe(Subscriber)` 전문

사용자 API는 결국 한 곳으로 모인다.

```java
public final Disposable subscribe(Consumer<? super T> consumer) {
	Objects.requireNonNull(consumer, "consumer");
	return subscribe(consumer, null, null);
}
```
`publisher/Flux.java:8721-8724`

```java
public final Disposable subscribe(
		@Nullable Consumer<? super T> consumer,
		@Nullable Consumer<? super Throwable> errorConsumer,
		@Nullable Runnable completeConsumer,
		@Nullable Context initialContext) {
	return subscribeWith(new LambdaSubscriber<>(consumer, errorConsumer,
			completeConsumer,
			null,
			initialContext));
}
```
`publisher/Flux.java:8851-8860`

```java
public final <E extends Subscriber<? super T>> E subscribeWith(E subscriber) {
	subscribe(subscriber);
	return subscriber;
}
```
`publisher/Flux.java:9011-9014`

람다 오버로드들은 모두 `LambdaSubscriber`를 만들어 `subscribe(Subscriber)`에 넘긴다. `LambdaSubscriber`는 `InnerConsumer<T>`(= `CoreSubscriber` + `Scannable`)를 구현한다 (`publisher/LambdaSubscriber.java:35-36`). 즉 이미 `CoreSubscriber`라서 `StrictSubscriber` 래핑이 안 붙는다.

그리고 모든 길이 모이는 지점:

```java
@Override
@SuppressWarnings("unchecked")
public final void subscribe(Subscriber<? super T> actual) {
	CorePublisher publisher = Operators.onLastAssembly(this);
	CoreSubscriber subscriber = Operators.toCoreSubscriber(actual);

	if (subscriber instanceof Fuseable.QueueSubscription && this != publisher && this instanceof Fuseable && !(publisher instanceof Fuseable)) {
		subscriber = new FluxHide.SuppressFuseableSubscriber<>(subscriber);
	}

	try {
		if (publisher instanceof OptimizableOperator) {
			OptimizableOperator operator = (OptimizableOperator) publisher;
			while (true) {
				subscriber = operator.subscribeOrReturn(subscriber);
				if (subscriber == null) {
					// null means "I will subscribe myself", returning...
					return;
				}
				OptimizableOperator newSource = operator.nextOptimizableSource();
				if (newSource == null) {
					publisher = operator.source();
					break;
				}
				operator = newSource;
			}
		}

		subscriber = Operators.restoreContextOnSubscriberIfPublisherNonInternal(publisher, subscriber);
		publisher.subscribe(subscriber);
	}
	catch (Throwable e) {
		Operators.reportThrowInSubscribe(subscriber, e);
		return;
	}
}
```
`Flux.subscribe()` `publisher/Flux.java:8862-8897`

`Mono.subscribe()`는 `catch` 블록의 `return` 문 유무만 다르고 **구조가 완전히 동일하다** (`publisher/Mono.java:4546-4580`).

한 줄씩 보자.

**(1) `Operators.onLastAssembly(this)` — 전역 훅 적용 (`publisher/Flux.java:8865`)**

```java
public static <T> CorePublisher<T> onLastAssembly(CorePublisher<T> source) {
	Function<Publisher, Publisher> hook = Hooks.onLastOperatorHook;
	if (hook == null) {
		return source;
	}
	Publisher<T> publisher = Objects.requireNonNull(hook.apply(source),"LastOperator hook returned null");
	if (publisher instanceof CorePublisher) {
		return (CorePublisher<T>) publisher;
	}
	else {
		return new CorePublisherAdapter<>(publisher);
	}
}
```
`publisher/Operators.java:977-991`

`onLastOperator` 훅은 체인의 **마지막 연산자(= 사용자가 구독한 그 객체)에 한 번만** 적용된다. 훅이 `CorePublisher`가 아닌 걸 반환하면 `CorePublisherAdapter`로 감싸서 내부 계약을 복원한다. 1번 문서에서 본 `CorePublisher.subscribe(CoreSubscriber)`가 이 훅을 우회하도록 설계된 이유가 여기서 보인다 — 내부 연산자들이 서로를 구독할 때 이 훅이 매번 다시 걸리면 안 되니까.

**(2) `Operators.toCoreSubscriber(actual)` — 규약 경계 (`publisher/Flux.java:8866`)**

`CoreSubscriber`가 아니면 `StrictSubscriber`로 감싼다 (`publisher/Operators.java:1378-1392`). [1번 문서 §3.3](01-reactive-streams-flux-mono.md#33-경계에서의-엄격-모드--strictsubscriber) 참고.

**(3) 퓨전 억제 분기 (`publisher/Flux.java:8868-8870`)**

`onLastOperator` 훅이 `Fuseable`한 소스를 non-`Fuseable`로 바꿔치기했는데 구독자는 여전히 `QueueSubscription`을 기대하는 상황을 `FluxHide.SuppressFuseableSubscriber`로 막는다. 조건 네 개를 모두 보면 의도가 읽힌다 — `this != publisher`(훅이 개입했고) && `this instanceof Fuseable`(원래는 퓨전 가능) && `!(publisher instanceof Fuseable)`(바뀐 건 불가능).

**(4) `OptimizableOperator` while 루프 (`publisher/Flux.java:8873-8888`)** — 다음 절.

**(5) `publisher.subscribe(subscriber)` (`publisher/Flux.java:8891`)**

드디어 `CorePublisher.subscribe(CoreSubscriber)`가 호출된다. 루프를 다 통과한 뒤의 `publisher`는 **더 이상 `OptimizableOperator`가 아닌 소스**이고, `subscriber`는 체인 전체의 구독자가 겹겹이 쌓인 결과물이다.

**(6) `catch` → `Operators.reportThrowInSubscribe` (`publisher/Flux.java:8893-8896`)**

구독 도중 예외가 터지면 그냥 던지지 않는다.

```java
public static void reportThrowInSubscribe(CoreSubscriber<?> subscriber, Throwable e) {
	try {
		subscriber.onSubscribe(EmptySubscription.FROM_SUBSCRIBE_INSTANCE);
	}
	catch (Throwable onSubscribeError) {
		Exceptions.throwIfFatal(onSubscribeError);
		e.addSuppressed(onSubscribeError);
	}
	subscriber.onError(onOperatorError(e, subscriber.currentContext()));
}
```
`publisher/Operators.java:224-233`

전용 `Subscription` 싱글턴(`FROM_SUBSCRIBE_INSTANCE`)으로 `onSubscribe`를 한 번 쳐준 다음 `onError`를 보낸다. 구독자가 이미 `Subscription`을 받았을 수도 있으니 `§1.9`에 따라 두 번째 `onSubscribe`는 무시될 걸 전제하고, 무시하지 않는 구독자를 위해 `Operators.canAppearAfterOnSubscribe()`로 식별할 수 있게 해 둔다 (`publisher/Operators.java:185-187`).

### 3.3 재귀 대신 루프 — `OptimizableOperator`

```java
interface OptimizableOperator<IN, OUT> extends CorePublisher<IN> {

	@Nullable
	CoreSubscriber<? super OUT> subscribeOrReturn(CoreSubscriber<? super IN> actual) throws Throwable;

	CorePublisher<? extends OUT> source();

	@Nullable
	OptimizableOperator<?, ? extends OUT> nextOptimizableSource();
}
```
`publisher/OptimizableOperator.java:34-61`

인터페이스 javadoc이 목적을 직접 말한다 — "looping instead of recursive subscribes" (`publisher/OptimizableOperator.java:27`).

계약 세 가지:
- `subscribeOrReturn(actual)` → 다음(업스트림)에 넘길 구독자를 **반환**한다. 또는 "내가 직접 구독할게"라는 뜻으로 `null`을 반환한다.
- `nextOptimizableSource()` → 체인을 한 칸 위로 올라간다. `subscribeOrReturn`이 non-null을 반환했다면 이 메서드도 **반드시** non-null이어야 한다 (javadoc `publisher/OptimizableOperator.java:54-55`).
- `source()` → `nextOptimizableSource()`가 `null`일 때 실제로 `subscribe`를 걸 `CorePublisher`.

`InternalFluxOperator`가 이 셋을 구현한다.

```java
protected InternalFluxOperator(Flux<? extends I> source) {
	super(source);
	if (source instanceof OptimizableOperator) {
		OptimizableOperator<?, I> optimSource = (OptimizableOperator<?, I>) source;
		this.optimizableOperator = optimSource;
	}
	else {
		this.optimizableOperator = null;
	}
}
```
`publisher/InternalFluxOperator.java:37-47`

**조립 시점에** 소스가 `OptimizableOperator`인지 판정해서 `optimizableOperator` 필드에 캐시한다. 구독 시점에는 `instanceof` 검사조차 안 한다.

```java
@Override
public final OptimizableOperator<?, ? extends I> nextOptimizableSource() {
	return optimizableOperator;
}
```
`publisher/InternalFluxOperator.java:88-91`

그리고 `InternalFluxOperator` 자신이 `CorePublisher.subscribe(CoreSubscriber)`를 구현할 때도 **같은 while 루프를 쓴다**.

```java
@Override
@SuppressWarnings("unchecked")
public final void subscribe(CoreSubscriber<? super O> subscriber) {
	OptimizableOperator operator = this;
	try {
		while (true) {
			subscriber = operator.subscribeOrReturn(subscriber);
			if (subscriber == null) {
				// null means "I will subscribe myself", returning...
				return;
			}
			OptimizableOperator newSource = operator.nextOptimizableSource();
			if (newSource == null) {
				CorePublisher operatorSource = operator.source();
				subscriber = Operators.restoreContextOnSubscriberIfPublisherNonInternal(operatorSource, subscriber);
				operatorSource.subscribe(subscriber);
				return;
			}
			operator = newSource;
		}
	}
	catch (Throwable e) {
		Operators.reportThrowInSubscribe(subscriber, e);
		return;
	}
}
```
`InternalFluxOperator.subscribe()` `publisher/InternalFluxOperator.java:49-78`

`InternalMonoOperator.subscribe()`도 동일하다 (`publisher/InternalMonoOperator.java:57-86`).

### 3.4 두 스택을 나눠 그린 그림

`Flux.range(1,3).map(i -> i*2).filter(i -> i>2).subscribe(printer)` 를 두 단계로 분리하면 이렇다.

```
┌──────────────────── 조립 시점 (assembly time) ─────────────────────┐
│                                                                   │
│  Flux.range(1,3)                                                  │
│    └─ new FluxRange(1,3)                  Fuseable, SourceProducer│
│                                                                   │
│  .map(i -> i*2)                                                   │
│    └─ this instanceof Fuseable == true                            │
│       └─ new FluxMapFuseable(source=FluxRange, mapper)            │
│          └─ optimizableOperator = null  (FluxRange 는 Optimizable 아님)
│                                                                   │
│  .filter(i -> i>2)                                                │
│    └─ this instanceof Fuseable == true                            │
│       └─ new FluxFilterFuseable(source=FluxMapFuseable, pred)     │
│          └─ optimizableOperator = FluxMapFuseable                 │
│                                                                   │
│  결과: Publisher 3개. 데이터 0건. 스레드 작업 0건.                 │
└───────────────────────────────────────────────────────────────────┘

┌──────────────────── 구독 시점 (subscribe time) ────────────────────┐
│                                                                   │
│  Flux.subscribe(printer)            publisher/Flux.java:8864      │
│   1) publisher  = Operators.onLastAssembly(FluxFilterFuseable)    │
│   2) subscriber = Operators.toCoreSubscriber(printer)             │
│                                                                   │
│   3) while 루프 (재귀 아님!)                                       │
│      ┌─────────────────────────────────────────────────────────┐  │
│      │ 회차 1: operator = FluxFilterFuseable                    │  │
│      │   subscribeOrReturn(printer)                             │  │
│      │     → new FilterFuseableSubscriber(printer, pred)        │  │
│      │   nextOptimizableSource() → FluxMapFuseable (non-null)   │  │
│      │   operator = FluxMapFuseable                             │  │
│      ├─────────────────────────────────────────────────────────┤  │
│      │ 회차 2: operator = FluxMapFuseable                       │  │
│      │   subscribeOrReturn(FilterFuseableSubscriber)            │  │
│      │     → new MapFuseableSubscriber(filterSub, mapper)       │  │
│      │   nextOptimizableSource() → null                         │  │
│      │   publisher = operator.source() = FluxRange              │  │
│      │   break                                                  │  │
│      └─────────────────────────────────────────────────────────┘  │
│                                                                   │
│   4) FluxRange.subscribe(MapFuseableSubscriber)                   │
│        → onSubscribe(RangeSubscription) 이 다운스트림으로 전파     │
│        → printer.onSubscribe → request(Long.MAX_VALUE)            │
│        → 업스트림으로 request 전달 → onNext 흐름 시작             │
│                                                                   │
│  결과: Subscriber 체인 3개. 스택 깊이는 체인 길이와 무관하게 일정. │
└───────────────────────────────────────────────────────────────────┘
```

핵심은 **방향이 뒤집힌다**는 점이다.

```
Publisher 그래프 (조립):   Filter ──source──▶ Map ──source──▶ Range
Subscriber 체인 (구독):    Range ──onNext──▶ MapSub ──onNext──▶ FilterSub ──onNext──▶ printer
                           Range ◀──request── MapSub ◀──request── FilterSub ◀──request── printer
```

### 3.5 `subscribeOrReturn`이 `null`을 반환하는 경우

"내가 직접 구독할게"의 실제 사례가 `FluxSubscribeOn`이다.

```java
@Override
public CoreSubscriber<? super T> subscribeOrReturn(CoreSubscriber<? super T> actual) {
	Worker worker = Objects.requireNonNull(scheduler.createWorker(),
			"The scheduler returned a null Function");

	SubscribeOnSubscriber<T> parent = new SubscribeOnSubscriber<>(source,
			actual, worker, requestOnSeparateThread);
	actual.onSubscribe(parent);

	try {
		worker.schedule(parent);
	}
	catch (RejectedExecutionException ree) {
		if (parent.s != Operators.cancelledSubscription()) {
			actual.onError(Operators.onRejectedExecution(ree, parent, null, null,
					actual.currentContext()));
		}
	}
	return null;
}
```
`FluxSubscribeOn.subscribeOrReturn()` `publisher/FluxSubscribeOn.java:52-71`

업스트림 구독을 **다른 스레드에서** 일으켜야 하므로 지금 이 자리에서 체인을 계속 이어갈 수가 없다. 그래서 `onSubscribe`를 직접 쳐주고, `worker.schedule(parent)`로 구독 작업을 넘기고, `null`을 반환해 while 루프를 끊는다. `subscribeOn`이 체인 어디에 있든 **구독 전체**의 스레드를 바꾸는 이유가 정확히 이 구조다. 자세한 건 [스케줄러와 스레딩](08-schedulers-and-threading.md) 참고.

---

## 4. 실전 예제

### 예제 1: 조립 시점에는 아무 일도 없다는 것을 증명

```java
import reactor.core.publisher.Flux;

public class NothingHappensDemo {

    static int sideEffectCount = 0;

    public static void main(String[] args) {
        System.out.println("=== 조립 시작 ===");
        Flux<Integer> pipeline = Flux.range(1, 3)
                .doOnSubscribe(s -> System.out.println("  [구독 시점] FluxRange 구독됨"))
                .map(i -> {
                    sideEffectCount++;
                    System.out.println("  [구독 시점] map 실행: " + i);
                    return i * 2;
                });
        System.out.println("=== 조립 끝. sideEffectCount = " + sideEffectCount + " ===");
        System.out.println("체인 객체: " + pipeline.getClass().getSimpleName());

        System.out.println("=== 1차 구독 ===");
        pipeline.subscribe(v -> System.out.println("  받음: " + v));

        System.out.println("=== 2차 구독 (같은 Publisher 그래프, 새 Subscriber 체인) ===");
        pipeline.subscribe(v -> System.out.println("  받음: " + v));

        System.out.println("=== 총 sideEffectCount = " + sideEffectCount + " ===");
    }
}
```

예상 출력:

```
=== 조립 시작 ===
=== 조립 끝. sideEffectCount = 0 ===
체인 객체: FluxMapFuseable
=== 1차 구독 ===
  [구독 시점] FluxRange 구독됨
  [구독 시점] map 실행: 1
  받음: 2
  ...
=== 총 sideEffectCount = 6 ===
```

조립 후 `sideEffectCount`가 0이라는 것, 그리고 두 번 구독하니 6(= 3 × 2)이 된다는 것 둘 다 확인된다. 체인 객체 이름이 `FluxMapFuseable`인 것도 중요하다 — `FluxRange`가 `Fuseable`(`publisher/FluxRange.java:31-32`)이라서 `map`의 조립 시점 분기가 퓨전 버전을 골랐다.

### 예제 2: while 루프 평탄화를 깊은 체인으로 확인

```java
import reactor.core.publisher.Flux;

public class DeepChainDemo {

    public static void main(String[] args) {
        int depth = 20_000;

        Flux<Integer> flux = Flux.just(1);
        for (int i = 0; i < depth; i++) {
            flux = flux.map(x -> x + 1);
        }

        // 조립은 끝났다. 이제 구독해도 StackOverflowError 가 나지 않는다.
        // InternalFluxOperator.subscribe() 의 while 루프가 재귀를 대신하기 때문.
        //   publisher/InternalFluxOperator.java:49-78
        Integer result = flux.blockLast();
        System.out.println("depth=" + depth + " 결과 = " + result);  // 20001
    }
}
```

`subscribe` 경로는 루프라서 스택이 늘지 않는다. 다만 **신호(onNext) 전파는 여전히 구독자 체인을 타고 가는 중첩 호출**이다. 즉 이 최적화는 "구독 과정"의 스택만 평탄화한다. `onNext` 쪽 스택 깊이는 퓨전이 줄여준다 — [연산자 퓨전](06-operator-fusion.md) 참고.

### 예제 3: `onLastOperator`와 `onEachOperator`가 적용되는 횟수 비교

```java
import java.util.concurrent.atomic.AtomicInteger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;

public class HookTimingDemo {

    public static void main(String[] args) {
        AtomicInteger eachCount = new AtomicInteger();
        AtomicInteger lastCount = new AtomicInteger();

        Hooks.onEachOperator("demo-each", p -> { eachCount.incrementAndGet(); return p; });
        Hooks.onLastOperator("demo-last", p -> { lastCount.incrementAndGet(); return p; });

        try {
            // 조립: Flux.onAssembly() 가 연산자마다 onEachOperatorHook 적용
            //   publisher/Flux.java:10956-10959
            Flux<Integer> f = Flux.range(1, 3).map(i -> i * 2).filter(i -> i > 2);
            System.out.println("조립 후 — each=" + eachCount.get() + ", last=" + lastCount.get());

            // 구독: Operators.onLastAssembly() 가 마지막 연산자에 한 번만 적용
            //   publisher/Operators.java:977-991
            f.subscribe();
            f.subscribe();
            System.out.println("두 번 구독 후 — each=" + eachCount.get() + ", last=" + lastCount.get());
        }
        finally {
            Hooks.resetOnEachOperator("demo-each");
            Hooks.resetOnLastOperator("demo-last");
        }
    }
}
```

`each`는 조립 시점에 연산자 개수만큼 올라가고 구독해도 더 안 올라간다. `last`는 조립 중에는 0이고 **구독할 때마다 1씩** 올라간다. 두 훅의 이름이 왜 `onEach`/`onLast`인지가 이 숫자로 설명된다.

---

## 5. 정리

- 조립 시점에는 `Publisher` 객체가 만들어지고 `source` 필드로 업스트림을 가리킨다. 데이터는 전혀 흐르지 않는다. `FluxOperator`의 `protected final Flux<? extends I> source`가 그 링크다 (`publisher/FluxOperator.java:34`).
- 조립 시점에 끝나는 일 두 가지: `Hooks.onEachOperator` 적용과 `AssemblySnapshot` 캡처(`Flux.onAssembly()` `publisher/Flux.java:10954-10965`), 그리고 **소스가 `Fuseable`/`OptimizableOperator`인지의 판정**. 구독 시점에 다시 검사하지 않도록 필드에 캐시한다.
- 모든 사용자 `subscribe` 오버로드는 `LambdaSubscriber`를 만들어 `subscribeWith` → `subscribe(Subscriber)` 하나로 수렴한다 (`publisher/Flux.java:8851-8860`, `9011-9014`).
- `Flux.subscribe(Subscriber)`의 순서는 ① `Operators.onLastAssembly()` (전역 `onLastOperator` 훅, 체인당 한 번) ② `Operators.toCoreSubscriber()` (규약 엄격 경계) ③ 퓨전 억제 분기 ④ `OptimizableOperator` while 루프 ⑤ `CorePublisher.subscribe(CoreSubscriber)` ⑥ 예외는 `Operators.reportThrowInSubscribe()`로 신호화. `Mono.subscribe(Subscriber)`도 구조가 같다 (`publisher/Mono.java:4546-4580`).
- while 루프의 본체는 `subscribeOrReturn(subscriber)` → `nextOptimizableSource()` 두 호출이다. 전자가 구독자를 하나 더 쌓고, 후자가 체인을 한 칸 올라간다. `nextOptimizableSource()`가 `null`이면 `source()`를 꺼내 루프를 빠져나온다.
- 이 루프가 **깊은 체인의 StackOverflow를 막는다**. `InternalFluxOperator.subscribe()`(`publisher/InternalFluxOperator.java:49-78`)와 `InternalMonoOperator.subscribe()`(`publisher/InternalMonoOperator.java:57-86`)가 같은 루프를 쓴다. 단, 평탄화되는 건 구독 과정뿐이고 `onNext` 전파 스택은 별개다.
- `subscribeOrReturn`이 `null`을 반환하면 "내가 직접 구독한다"는 뜻이고 루프가 즉시 끝난다. `FluxSubscribeOn`이 대표 사례로, 업스트림 구독을 `Worker`에 넘기기 위해 이 경로를 쓴다 (`publisher/FluxSubscribeOn.java:52-71`).
- 방향이 뒤집힌다는 걸 기억하면 된다. `Publisher` 링크는 다운스트림 → 업스트림, `Subscriber` 체인은 업스트림 → 다운스트림. `request`는 다시 거꾸로 올라간다.

---

## 관련 문서
- 선행: [리액티브 스트림 규약과 Flux/Mono](01-reactive-streams-flux-mono.md)
- 후행: [연산자 체인의 실제 구조](03-operator-chain-internals.md)
- 참고: [연산자 퓨전](06-operator-fusion.md) — 조립 시점 `Fuseable` 분기가 실제로 하는 일
- 참고: [스케줄러와 스레딩](08-schedulers-and-threading.md) — `subscribeOrReturn`이 `null`을 반환하는 `subscribeOn`의 동작
- 참고: [테스트와 디버깅](12-testing-debugging.md) — `AssemblySnapshot`과 조립 시점 스택 복원

---
*참고: Reactor Core 3.7.19 기준(태그 `v3.7.19`). 본문의 소스 경로는 레포 루트의 `reactor-core/src/main/java/reactor/` 기준 상대경로다 — 예: `publisher/Operators.java:70` = `reactor-core/src/main/java/reactor/core/publisher/Operators.java:70`.*
