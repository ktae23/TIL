# Java 동시성 도구 완전 정리 - Thread Pool, ExecutorService, ForkJoinPool

Java의 동시성(Concurrency) 프로그래밍에서 스레드를 직접 관리하는 것은 복잡하고 위험하다. Java는 이를 위해 `ExecutorService`, `ForkJoinPool` 등 다양한 추상화 도구를 제공한다. 이 문서는 각 도구의 **동작 원리, 내부 구조, 장단점, 실무 선택 기준**을 실제 코드 예시와 함께 정리한다.

---

## 목차

1. [직접 구현한 Thread Pool](#1-직접-구현한-thread-pool-simplethreadpool)
2. [ExecutorService 종류별 비교](#2-executorservice-종류별-비교)
3. [Callable과 Future](#3-callable과-future)
4. [ForkJoinPool](#4-forkjoinpool)
5. [원자적 연산 (Atomic Operation)](#5-원자적-연산-atomic-operation)
6. [실무 연결 — CompletableFuture, Spring @Async, Virtual Thread](#6-실무-연결--completablefuture-spring-async-virtual-thread)
7. [실무 선택 가이드](#7-실무-선택-가이드)

---

## 1. 직접 구현한 Thread Pool (SimpleThreadPool)

스레드 풀의 핵심 원리를 이해하기 위해 직접 구현해 볼 수 있다.

### 동작 원리

```
[submit(task)] → [LinkedBlockingDeque] → [Worker-0] → task.run()
                                        → [Worker-1] → task.run()
                                        → [Worker-2] → task.run()
                                        → [Worker-3] → task.run()
```

- **작업 큐**: `LinkedBlockingDeque`에 작업을 넣고, Worker 스레드들이 꺼내서 실행
- **블로킹 대기**: `queue.take()`는 큐가 비어있으면 블로킹되어 대기 → busy waiting 방지
- **배압(Backpressure)**: `queue.put()`은 큐가 가득 차면 블로킹 → 생산자 속도 조절

### 코드 예시

```java
public class SimpleThreadPool implements AutoCloseable {

    private final LinkedBlockingDeque<Runnable> queue;
    private final ThreadGroup threadGroup;
    private volatile boolean running = true;

    public SimpleThreadPool(int poolSize, int queueSize) {
        Worker[] threads = new Worker[poolSize];
        this.queue = new LinkedBlockingDeque<>(queueSize);
        this.threadGroup = new ThreadGroup("SimpleThreadPool");

        for (int i = 0; i < poolSize; i++) {
            threads[i] = new Worker(threadGroup, "Worker-" + i);
            threads[i].start();
        }
    }

    public void submit(Runnable task) {
        try {
            queue.put(task);  // 큐가 가득 차면 블로킹
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() {
        running = false;
        threadGroup.interrupt();  // 모든 Worker 스레드에 인터럽트 전달
    }

    @Override
    public void close() {
        // 큐의 남은 작업이 모두 처리될 때까지 대기
        while (!queue.isEmpty()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        shutdown();
    }

    class Worker extends Thread {
        public Worker(ThreadGroup threadGroup, String name) {
            super(threadGroup, name);
        }

        @Override
        public void run() {
            while (running) {
                try {
                    Runnable task = queue.take();  // 블로킹 대기
                    task.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
```

### 핵심 포인트

| 요소 | 설명 |
|------|------|
| `volatile boolean running` | 모든 스레드에서 가시성(visibility) 보장 |
| `queue.take()` | 큐가 비면 블로킹, 작업이 들어오면 깨어남 |
| `queue.put()` | 큐가 가득 차면 블로킹 (배압 제공) |
| `AutoCloseable` | try-with-resources로 안전한 종료 보장 |
| `threadGroup.interrupt()` | 한 번에 모든 Worker 스레드 인터럽트 |

### 실무에서는 직접 구현하지 않는 이유

- **예외 처리**: 작업 중 예외 발생 시 Worker 스레드가 죽을 수 있음
- **스레드 재생성**: 죽은 스레드를 자동으로 재생성하는 로직 필요
- **graceful shutdown**: `awaitTermination`, `shutdownNow` 등 세밀한 종료 전략 부재
- **모니터링**: 활성 스레드 수, 큐 크기, 완료 작업 수 등의 통계 미제공
- **거부 정책**: 큐가 가득 찼을 때의 다양한 전략 (AbortPolicy, CallerRunsPolicy 등) 미지원

> **결론**: 학습 목적으로 이해한 뒤, 실무에서는 반드시 `ExecutorService`를 사용한다.

---

## 2. ExecutorService 종류별 비교

Java의 `Executors` 팩토리 클래스는 용도별로 최적화된 스레드 풀을 제공한다. 모든 구현체는 내부적으로 `ThreadPoolExecutor` 또는 `ForkJoinPool`을 사용한다.

### 2-1. newFixedThreadPool

**고정 크기 스레드 풀** — CPU 바운드 작업에 적합

```java
// 내부 구현
public static ExecutorService newFixedThreadPool(int nThreads) {
    return new ThreadPoolExecutor(nThreads, nThreads,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());
}
```

```java
// 사용 예시
final int cpuCores = Runtime.getRuntime().availableProcessors();
try (ExecutorService pool = Executors.newFixedThreadPool(cpuCores)) {
    for (int i = 0; i < 20; i++) {
        pool.execute(() -> System.out.println(
                Thread.currentThread().getName() + " is executing a task"
        ));
    }
}
```

| 항목 | 설명 |
|------|------|
| **corePoolSize** | nThreads (고정) |
| **maximumPoolSize** | nThreads (고정, 확장 없음) |
| **keepAliveTime** | 0 (유휴 스레드 회수 안 함) |
| **작업 큐** | `LinkedBlockingQueue` (무한 큐) |
| **장점** | 스레드 수 고정으로 리소스 사용량 예측 가능 |
| **단점** | 큐가 무한하므로 작업이 쌓이면 **OOM 위험** |
| **적합한 상황** | CPU 바운드 작업, `availableProcessors()` 기반 설정 |

### 2-2. newCachedThreadPool

**탄력적 스레드 풀** — I/O 바운드 작업에 적합

```java
// 내부 구현
public static ExecutorService newCachedThreadPool() {
    return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>());
}
```

```java
// 사용 예시
try (ExecutorService pool = Executors.newCachedThreadPool()) {
    for (int i = 0; i < 20; i++) {
        pool.execute(() -> System.out.println(
                Thread.currentThread().getName() + " is executing a task"
        ));
    }
}
```

| 항목 | 설명 |
|------|------|
| **corePoolSize** | 0 (유휴 시 모든 스레드 회수 가능) |
| **maximumPoolSize** | `Integer.MAX_VALUE` (사실상 무제한) |
| **keepAliveTime** | 60초 (유휴 스레드 자동 회수) |
| **작업 큐** | `SynchronousQueue` (저장 공간 없음, 즉시 핸드오프) |
| **장점** | 유휴 스레드 자동 회수, 필요 시 즉시 생성 |
| **단점** | 요청 폭증 시 **스레드 폭증** → 시스템 불안정 |
| **적합한 상황** | 짧은 비동기 I/O 작업, 요청량 예측 가능 시 |

> **주의**: `SynchronousQueue`는 저장 공간이 없다. 작업 제출 시 대기 중인 스레드가 없으면 **새 스레드를 즉시 생성**한다.

### 2-3. newSingleThreadExecutor

**단일 스레드 풀** — 순서 보장이 필요한 작업에 적합

```java
// 내부 구현
public static ExecutorService newSingleThreadExecutor() {
    return new AutoShutdownDelegatedExecutorService(
            new ThreadPoolExecutor(1, 1,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>()));
}
```

```java
// 사용 예시
try (ExecutorService pool = Executors.newSingleThreadExecutor()) {
    for (int i = 0; i < 20; i++) {
        pool.execute(() -> System.out.println(
                Thread.currentThread().getName() + " is executing a task"
        ));
    }
}
```

| 항목 | 설명 |
|------|------|
| **corePoolSize** | 1 |
| **maximumPoolSize** | 1 |
| **작업 큐** | `LinkedBlockingQueue` (무한 큐) |
| **장점** | 작업 **순서 보장** (FIFO), 스레드 안전 |
| **단점** | 병렬 처리 불가, 단일 스레드 병목 |
| **적합한 상황** | 로그 기록, 이벤트 순차 처리, DB 순차 쓰기 |

### 2-4. newScheduledThreadPool

**스케줄링 스레드 풀** — 주기적/지연 실행 작업에 적합

```java
// 내부 구현
public ScheduledThreadPoolExecutor(int corePoolSize) {
    super(corePoolSize, Integer.MAX_VALUE,
            DEFAULT_KEEPALIVE_MILLIS, MILLISECONDS,
            new DelayedWorkQueue());
}
```

```java
// 사용 예시
try (ScheduledExecutorService pool = Executors.newScheduledThreadPool(2)) {
    pool.scheduleAtFixedRate(
            () -> System.out.println(Thread.currentThread().getName() + " running"),
            0,       // 초기 지연
            5,       // 반복 간격
            TimeUnit.SECONDS
    );
}
```

| 항목 | 설명 |
|------|------|
| **작업 큐** | `DelayedWorkQueue` (우선순위 힙 기반) |
| **장점** | 주기적 실행, 지연 실행, cron 대체 |
| **단점** | Timer 대비 복잡, 예외 시 후속 실행 중단 |

#### scheduleAtFixedRate vs scheduleWithFixedDelay

```
scheduleAtFixedRate(task, 0, 5, SECONDS)
|--task--|     |--task--|     |--task--|
0        3    5        8    10       13
→ 시작 시점 기준으로 5초 간격 (작업이 3초 걸려도 5초마다 시작)

scheduleWithFixedDelay(task, 0, 5, SECONDS)
|--task--|          |--task--|          |--task--|
0        3         8        11        16       19
→ 종료 시점 기준으로 5초 후 다음 시작 (작업이 3초 걸리면 8초에 다음 시작)
```

### ExecutorService 전체 비교 표

| 구분 | core | max | keepAlive | 큐 | 적합한 상황 |
|------|------|-----|-----------|------|-------------|
| **FixedThreadPool** | N | N | 0 | `LinkedBlockingQueue` | CPU 바운드 |
| **CachedThreadPool** | 0 | MAX | 60s | `SynchronousQueue` | I/O 바운드 |
| **SingleThreadExecutor** | 1 | 1 | 0 | `LinkedBlockingQueue` | 순서 보장 |
| **ScheduledThreadPool** | N | MAX | 10ms | `DelayedWorkQueue` | 주기적 실행 |
| **WorkStealingPool** | - | - | - | ForkJoinPool 기반 | 병렬 분할 처리 |

---

## 3. Callable과 Future

### Runnable vs Callable

| 구분 | Runnable | Callable\<V\> |
|------|----------|--------------|
| 반환값 | `void` | `V` (제네릭) |
| 예외 | `RuntimeException`만 가능 | `Exception` 선언 가능 |
| 제출 방법 | `execute()` 또는 `submit()` | `submit()`만 가능 |
| 결과 받기 | 불가 | `Future<V>` 통해 받기 |

### Future.get()의 블로킹 특성

```java
List<Future<Long>> futures = new ArrayList<>();
List<Integer> fibonacciIndices = List.of(10, 20, 30, 40, 50);

try (ExecutorService pool = Executors.newCachedThreadPool()) {
    for (int index : fibonacciIndices) {
        futures.add(pool.submit(() -> fibonacci(index)));
    }
    for (Future<Long> future : futures) {
        System.out.println("Fibonacci number: " + future.get());  // 블로킹!
    }
}
```

- `future.get()`은 결과가 준비될 때까지 **호출 스레드를 블로킹**한다
- 여러 Future를 순차적으로 get()하면 앞의 작업이 끝날 때까지 뒤의 결과도 못 받음
- 타임아웃 설정 가능: `future.get(5, TimeUnit.SECONDS)`

### ConcurrentHashMap을 활용한 메모이제이션

```java
static final Map<Integer, Long> cache = new ConcurrentHashMap<>(Map.of(0, 0L, 1, 1L));

private static Long fibonacci(int n) {
    if (cache.containsKey(n)) {
        return cache.get(n);
    } else {
        long result = fibonacci(n - 1) + fibonacci(n - 2);
        cache.put(n, result);
        return result;
    }
}
```

- `ConcurrentHashMap`은 thread-safe하여 여러 스레드에서 동시에 캐시 읽기/쓰기 가능
- 다만 `containsKey` → `get` 사이에 race condition 가능 → `computeIfAbsent` 사용이 더 안전

---

## 4. ForkJoinPool

### 4-1. 동작 원리 — Work-Stealing 알고리즘

일반적인 스레드 풀은 **하나의 공유 큐**를 사용하지만, ForkJoinPool은 **스레드마다 개별 Deque(덱)**를 가진다.

```
Traditional ThreadPool:
[공유 큐] ←── Worker-0, Worker-1, Worker-2 (경합 발생)

ForkJoinPool (Work-Stealing):
Worker-0: [task-A, task-B, task-C]  ← 자신의 덱에서 LIFO로 꺼냄
Worker-1: [task-D]
Worker-2: []  → Worker-0의 덱에서 task-A를 FIFO로 훔침(steal)
```

- **자기 큐**: LIFO(Last-In-First-Out)로 최신 작업부터 처리 → 캐시 지역성 향상
- **훔치기**: 다른 스레드의 큐에서 FIFO(First-In-First-Out)로 오래된 작업부터 훔침
- **장점**: 유휴 스레드가 없어지고, 공유 큐 경합이 최소화됨

### 4-2. RecursiveTask vs RecursiveAction

| 구분 | RecursiveTask\<V\> | RecursiveAction |
|------|-------------------|-----------------|
| 반환값 | `V` (결과 반환) | `void` (결과 없음) |
| 핵심 메서드 | `compute()` → return V | `compute()` → void |
| 사용 사례 | 합계, 피보나치, 정렬 | 배열 변환, 로그 처리 |

### 4-3. Traditional ThreadPool vs ForkJoinPool — 피보나치 비교

#### Traditional ThreadPool의 문제점

```java
private static long getFibonacci(int i, ExecutorService pool) {
    if (cache.containsKey(i)) return cache.get(i);

    Future<Long> future1 = pool.submit(() -> getFibonacci(i - 1, pool));
    Future<Long> future2 = pool.submit(() -> getFibonacci(i - 2, pool));
    try {
        long result = future1.get() + future2.get();  // 블로킹!
        cache.put(i, result);
        return result;
    } catch (InterruptedException | ExecutionException e) {
        throw new RuntimeException(e);
    }
}
```

**문제**: 부모 작업이 `future.get()`으로 블로킹되면서 스레드를 점유 → 자식 작업이 스레드를 받지 못하면 **데드락(Deadlock)** 발생 가능

```
Thread-1: getFibonacci(20) → submit(fib(19)) + submit(fib(18)) → get() 대기 (블로킹)
Thread-2: getFibonacci(19) → submit(fib(18)) + submit(fib(17)) → get() 대기 (블로킹)
...
Thread-100: 모든 스레드가 get() 대기 중 → 새 작업 실행 불가 → 데드락!
```

#### ForkJoinPool의 해결 방식

```java
static class FibonacciTask extends RecursiveTask<Long> {
    private final int n;

    public FibonacciTask(int n) { this.n = n; }

    @Override
    protected Long compute() {
        if (cache.containsKey(n)) return cache.get(n);

        FibonacciTask f1 = new FibonacciTask(n - 1);
        f1.fork();          // 다른 스레드에 비동기 제출
        FibonacciTask f2 = new FibonacciTask(n - 2);
        long result = f2.compute()  // 현재 스레드에서 직접 실행
                    + f1.join();    // f1 결과 대기 (work-stealing으로 다른 작업 처리)
        cache.put(n, result);
        return result;
    }
}
```

**핵심 패턴: `fork()` → `compute()` → `join()`**

| 메서드 | 동작 |
|--------|------|
| `f1.fork()` | f1을 현재 스레드의 **덱에 넣어** 다른 스레드가 훔칠 수 있게 함 |
| `f2.compute()` | f2는 **현재 스레드에서 직접 실행** (스레드 전환 없음) |
| `f1.join()` | f1의 결과를 기다림. 아직 시작 안 됐으면 **현재 스레드가 직접 실행** |

> **왜 ForkJoinPool은 데드락이 안 나는가?**
> `join()` 대기 중 스레드가 놀지 않고, 다른 작업을 **훔쳐서(steal)** 실행한다. 즉, 블로킹 상태에서도 유휴 자원이 발생하지 않는다.

### 4-4. newWorkStealingPool과 비동기 모드

```java
// 내부 구현
public static ExecutorService newWorkStealingPool() {
    return new ForkJoinPool(
            Runtime.getRuntime().availableProcessors(),
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            null,
            true  // asyncMode = true (FIFO)
    );
}
```

```java
// 사용 예시 — 이벤트 처리
try (ExecutorService pool = Executors.newWorkStealingPool(4)) {
    for (int i = 0; i < 10; i++) {
        pool.submit(new EventTask("Event " + i));
    }
}

record EventTask(String eventName) implements Runnable {
    @Override
    public void run() {
        System.out.println("Processing " + eventName + " in " + Thread.currentThread().getName());
        try { Thread.sleep(1000); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Completed " + eventName);
    }
}
```

#### asyncMode의 의미

| 모드 | 자기 큐 처리 순서 | 적합한 상황 |
|------|-------------------|-------------|
| `asyncMode = false` (기본) | **LIFO** | 분할 정복 (fork/join) |
| `asyncMode = true` | **FIFO** | 이벤트 처리, 독립 작업 |

- LIFO: 최근 fork된 작업 먼저 → 부모-자식 관계의 분할 정복에 유리 (캐시 지역성)
- FIFO: 먼저 제출된 작업 먼저 → 독립적인 작업의 공정한 처리에 유리

> **가상 스레드(Virtual Thread)**의 내부 스케줄러도 `asyncMode=true`인 `ForkJoinPool`을 사용한다.

---

## 5. 원자적 연산 (Atomic Operation)

### VarHandle + CAS 기반 Lock-free 카운터

```java
public class AtomicCounter {
    private volatile int counter = 0;

    private static final VarHandle COUNTER_HANDLE;

    static {
        try {
            COUNTER_HANDLE = MethodHandles.lookup().findVarHandle(
                    AtomicCounter.class, "counter", int.class
            );
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public void increment() {
        int current;
        int next;
        do {
            current = counter;
            next = current + 1;
        } while (!COUNTER_HANDLE.compareAndSet(this, current, next));
    }

    public int get() {
        return counter;
    }
}
```

### CAS(Compare-And-Set) 동작 원리

```
Thread-A:                         Thread-B:
1. current = 0                    1. current = 0
2. next = 1                      2. next = 1
3. CAS(0 → 1) ✅ 성공            3. CAS(0 → 1) ❌ 실패 (이미 1)
                                  4. current = 1 (재시도)
                                  5. next = 2
                                  6. CAS(1 → 2) ✅ 성공
```

- **Lock-free**: `synchronized`나 `ReentrantLock` 없이 스레드 안전 보장
- **낙관적 동시성**: "충돌은 드물 것"이라 가정하고, 충돌 시 재시도
- **volatile**: `counter` 필드의 변경이 모든 스레드에 즉시 가시(visible)

### VarHandle vs AtomicInteger

| 구분 | AtomicInteger | VarHandle |
|------|---------------|-----------|
| 도입 시기 | Java 5 | Java 9 |
| 사용 편의성 | ✅ 간단 (`incrementAndGet()`) | ❌ 복잡 (리플렉션 기반 설정) |
| 유연성 | 일반 필드에 적용 불가 | **어떤 필드든 CAS 적용 가능** |
| 성능 | 내부적으로 VarHandle 사용 | 직접 사용으로 간접 호출 제거 |
| 적합한 상황 | 단독 카운터 | 기존 클래스 필드에 원자적 연산 추가 |

> 실무에서는 대부분 `AtomicInteger`, `AtomicLong`, `AtomicReference`로 충분하다. `VarHandle`은 라이브러리/프레임워크 수준에서 성능 최적화가 필요할 때 사용한다.

---

## 6. 실무 연결 — CompletableFuture, Spring @Async, Virtual Thread

### 6-1. CompletableFuture와 ForkJoinPool.commonPool()

`CompletableFuture`의 비동기 메서드(`supplyAsync`, `runAsync`)는 **Executor를 지정하지 않으면** 내부적으로 `ForkJoinPool.commonPool()`을 사용한다.

```java
// ❌ commonPool 사용 — JVM 전체에서 공유, 다른 작업에 영향
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> callExternalApi());

// ✅ 전용 Executor 지정 — 격리된 스레드 풀
ExecutorService ioPool = Executors.newFixedThreadPool(20);
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> callExternalApi(), ioPool);
```

**주의**: `commonPool`은 JVM 전체에서 공유되므로, 하나의 무거운 작업이 다른 모든 `CompletableFuture`와 `parallelStream()`에 영향을 줄 수 있다 (thread starvation). 프로덕션에서는 **반드시 전용 Executor를 지정**한다.

```java
// 실무 패턴: 체이닝을 통한 비동기 파이프라인
CompletableFuture.supplyAsync(() -> fetchUserFromDB(userId), dbPool)
        .thenApplyAsync(user -> enrichWithProfile(user), ioPool)
        .thenAcceptAsync(user -> sendNotification(user), ioPool)
        .exceptionally(ex -> { log.error("파이프라인 실패", ex); return null; });
```

### 6-2. Spring @Async와 ThreadPoolTaskExecutor

Spring의 `@Async`는 내부적으로 `TaskExecutor` 빈을 찾아 사용한다.

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("ioExecutor")
    public ThreadPoolTaskExecutor ioExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("io-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean("cpuExecutor")
    public ThreadPoolTaskExecutor cpuExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors());
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("cpu-");
        executor.initialize();
        return executor;
    }
}

@Service
public class OrderService {
    @Async("ioExecutor")     // I/O 바운드 작업은 ioExecutor로
    public CompletableFuture<Void> sendOrderEmail(Order order) { ... }

    @Async("cpuExecutor")    // CPU 바운드 작업은 cpuExecutor로
    public CompletableFuture<Report> generateReport(List<Order> orders) { ... }
}
```

**핵심**: `@Async` 기본값인 `SimpleAsyncTaskExecutor`는 **매번 새 스레드를 생성**하므로 프로덕션에서는 반드시 `ThreadPoolTaskExecutor`를 등록한다.

### 6-3. Java 21 Virtual Thread와 기존 스레드 풀

Virtual Thread는 **기존 스레드 풀을 대체하는 것이 아니라**, I/O 바운드 작업에서 스레드 풀 자체를 불필요하게 만드는 접근이다.

```java
// Virtual Thread: 태스크마다 새로 생성 (풀링하지 않음)
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 100_000; i++) {
        executor.submit(() -> {
            // I/O 바운드 작업 — 10만 개도 문제 없음
            String result = httpClient.send(request, bodyHandler).body();
            processResult(result);
        });
    }
}
```

| 구분 | Platform Thread | Virtual Thread |
|------|----------------|----------------|
| 비용 | ~1MB 스택 메모리 | ~수 KB |
| 스케줄링 | OS 커널 | JVM (ForkJoinPool, asyncMode=true) |
| 풀링 | 필수 (비용이 비쌈) | 불필요 (가볍고 저렴) |
| 적합한 작업 | CPU 바운드 | I/O 바운드 |
| 최대 동시 수 | 수백~수천 | 수십만~수백만 |

**마이그레이션 주의사항**:
- `synchronized` 블록은 carrier thread를 **고정(pinning)**하므로 `ReentrantLock`으로 교체 권장
- `ThreadLocal` 캐싱은 Virtual Thread에서 비효율적 (매번 새 스레드이므로 캐시 무효화)
- CPU 바운드 작업에는 여전히 `FixedThreadPool`이나 `ForkJoinPool`이 적합

### 6-4. ThreadPoolExecutor의 거부 정책 (RejectedExecutionHandler)

`maximumPoolSize`에 도달하고 작업 큐도 가득 찼을 때 새 작업을 어떻게 처리할지 결정한다.

```
submit(task) → corePool에 빈 스레드? → [Yes] → 즉시 실행
                     ↓ [No]
              workQueue에 공간? → [Yes] → 큐에 추가
                     ↓ [No]
              maxPoolSize 미만? → [Yes] → 새 스레드 생성
                     ↓ [No]
              RejectedExecutionHandler 실행!
```

| 정책 | 동작 | 실무 사용 |
|------|------|-----------|
| **AbortPolicy** (기본값) | `RejectedExecutionException` 발생 | 작업 손실 불허 시 |
| **CallerRunsPolicy** | 호출자 스레드가 직접 실행 | **가장 실무적** — 자연스러운 backpressure |
| **DiscardPolicy** | 조용히 버림 | 로그, 메트릭 등 손실 허용 가능 시 |
| **DiscardOldestPolicy** | 큐의 가장 오래된 작업을 버림 | 최신 데이터가 중요한 경우 |

> **CallerRunsPolicy가 실무에서 선호되는 이유**: 호출자 스레드가 직접 작업을 처리하므로, 그 동안 새 작업 제출이 자연스럽게 느려진다. 별도의 rate limiter 없이도 **backpressure**가 자동으로 동작한다.

---

## 7. 실무 선택 가이드

### 상황별 추천 도구

| 상황 | 추천 도구 | 이유 |
|------|-----------|------|
| CPU 바운드 병렬 처리 | `FixedThreadPool` / `ForkJoinPool` | 코어 수에 맞는 고정 스레드로 컨텍스트 스위칭 최소화 |
| I/O 바운드 다수 요청 | `CachedThreadPool` / Virtual Thread | I/O 대기 중 다른 요청 처리, 유휴 스레드 자동 회수 |
| 주기적 스케줄링 | `ScheduledThreadPool` | cron 대체, 지연/반복 실행 내장 |
| 순서 보장 | `SingleThreadExecutor` | 단일 스레드로 FIFO 순서 보장 |
| 분할 정복 알고리즘 | `ForkJoinPool` + `RecursiveTask` | work-stealing으로 효율적 재귀 분할 |
| 독립 작업 병렬 실행 | `newWorkStealingPool` | asyncMode로 공정한 작업 분배 |
| Lock-free 카운터 | `AtomicInteger` / `VarHandle` CAS | 락 없이 원자적 업데이트 |

### 스레드 수 설정 공식

```
CPU 바운드:  스레드 수 = Runtime.getRuntime().availableProcessors()
I/O 바운드:  스레드 수 = CPU 코어 수 × (1 + I/O 대기시간 / CPU 처리시간)
```

### 위험 요소 체크리스트

- ⚠️ `LinkedBlockingQueue` (무한 큐) → 작업 폭주 시 OOM
- ⚠️ `CachedThreadPool` → 요청 폭증 시 스레드 폭증
- ⚠️ `Future.get()` 블로킹 → 재귀 작업에서 데드락
- ⚠️ `ExecutorService` 미종료 → JVM 종료 안 됨 (try-with-resources 사용)
- ⚠️ `CompletableFuture` Executor 미지정 → commonPool 공유로 thread starvation
- ⚠️ Virtual Thread에서 `synchronized` → carrier thread pinning
- ⚠️ `@Async` 기본값 → `SimpleAsyncTaskExecutor`가 매번 새 스레드 생성

### 전체 도구 관계도

```
┌─────────────────────────────────────────────────────────┐
│                    java.util.concurrent                  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │              ThreadPoolExecutor                    │   │
│  │  ┌────────────┐  ┌──────────────┐  ┌──────────┐  │   │
│  │  │FixedThread │  │CachedThread  │  │Single    │  │   │
│  │  │Pool        │  │Pool          │  │ThreadExec│  │   │
│  │  └────────────┘  └──────────────┘  └──────────┘  │   │
│  └──────────────────────────────────────────────────┘   │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │         ScheduledThreadPoolExecutor               │   │
│  │              (extends ThreadPoolExecutor)          │   │
│  └──────────────────────────────────────────────────┘   │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │               ForkJoinPool                        │   │
│  │  ┌────────────┐  ┌──────────────────────────┐    │   │
│  │  │WorkStealing│  │Virtual Thread Scheduler   │    │   │
│  │  │Pool        │  │(asyncMode=true)           │    │   │
│  │  └────────────┘  └──────────────────────────┘    │   │
│  └──────────────────────────────────────────────────┘   │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │  CompletableFuture → 기본: ForkJoinPool.common() │   │
│  │  Spring @Async    → 기본: SimpleAsyncTaskExecutor│   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

---

## 참고 자료

- [Java Concurrency in Practice](https://jcip.net/) — Brian Goetz
- [JDK 21 Documentation - ForkJoinPool](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ForkJoinPool.html)
- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [JEP 193: Variable Handles](https://openjdk.org/jeps/193)
- [Baeldung - Guide to the Fork/Join Framework](https://www.baeldung.com/java-fork-join)
- [Baeldung - CompletableFuture ThreadPool](https://www.baeldung.com/java-completablefuture-threadpool)

*마지막 업데이트: 2026년 03월*
