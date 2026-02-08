# Java 동시성 면접 질문

synchronized vs Lock, volatile, ThreadLocal 등 동시성 핵심 질문을 정리합니다.

## 목차

1. [synchronized vs Lock](#1-synchronized-vs-lock)
2. [volatile 키워드](#2-volatile-키워드)
3. [ThreadLocal](#3-threadlocal)
4. [Atomic 클래스](#4-atomic-클래스)
5. [Executor Framework](#5-executor-framework)
6. [동시성 문제 패턴](#6-동시성-문제-패턴)

---

## 1. synchronized vs Lock

### synchronized

```java
// 1. 메서드 레벨
public synchronized void increment() {
    count++;
}

// 2. 블록 레벨
public void increment() {
    synchronized (this) {
        count++;
    }
}

// 3. static 메서드 (클래스 레벨 락)
public static synchronized void staticMethod() {
    // Class 객체에 락
}

// 4. 특정 객체에 락
private final Object lock = new Object();
public void increment() {
    synchronized (lock) {
        count++;
    }
}
```

### Lock (ReentrantLock)

```java
private final Lock lock = new ReentrantLock();

public void increment() {
    lock.lock();
    try {
        count++;
    } finally {
        lock.unlock();  // 반드시 finally에서!
    }
}

// tryLock: 비블로킹 락 획득 시도
public boolean tryIncrement() {
    if (lock.tryLock()) {
        try {
            count++;
            return true;
        } finally {
            lock.unlock();
        }
    }
    return false;
}

// 타임아웃 설정
public boolean incrementWithTimeout() throws InterruptedException {
    if (lock.tryLock(1, TimeUnit.SECONDS)) {
        try {
            count++;
            return true;
        } finally {
            lock.unlock();
        }
    }
    return false;
}
```

### 비교

| 특성 | synchronized | Lock |
|------|--------------|------|
| 락 해제 | 자동 | 수동 (finally) |
| 인터럽트 | X | O (lockInterruptibly) |
| 타임아웃 | X | O (tryLock) |
| 공정성 | X | O (fair lock) |
| Condition | O (wait/notify) | O (여러 Condition) |
| 성능 | 비슷 (Java 6+) | 비슷 |

### ReadWriteLock

```java
private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
private final Lock readLock = rwLock.readLock();
private final Lock writeLock = rwLock.writeLock();

public String read() {
    readLock.lock();
    try {
        return data;
    } finally {
        readLock.unlock();
    }
}

public void write(String newData) {
    writeLock.lock();
    try {
        data = newData;
    } finally {
        writeLock.unlock();
    }
}

// 읽기는 동시에 가능
// 쓰기는 배타적
```

---

## 2. volatile 키워드

### 가시성 문제

```java
// 문제: 각 스레드가 캐시된 값 사용 가능
class Counter {
    private boolean running = true;  // 캐시될 수 있음

    public void stop() {
        running = false;  // 다른 스레드에서 안 보일 수 있음
    }

    public void run() {
        while (running) {  // 무한 루프 가능!
            // ...
        }
    }
}
```

### volatile 해결

```java
class Counter {
    private volatile boolean running = true;  // 항상 메인 메모리에서 읽음

    public void stop() {
        running = false;  // 즉시 다른 스레드에서 볼 수 있음
    }

    public void run() {
        while (running) {  // 정상 종료
            // ...
        }
    }
}
```

### volatile의 한계

```java
private volatile int count = 0;

// 원자성 보장 안 됨!
public void increment() {
    count++;  // read → increment → write (3단계)
    // 두 스레드가 동시에 읽으면 값 손실
}

// 해결: AtomicInteger 사용
private AtomicInteger count = new AtomicInteger(0);

public void increment() {
    count.incrementAndGet();  // 원자적
}
```

### volatile 사용 시점

```
적합:
- 단일 쓰기, 다중 읽기
- 상태 플래그 (boolean)
- 더블 체크 락킹 (DCL)

부적합:
- 복합 연산 (i++)
- check-then-act 패턴
```

---

## 3. ThreadLocal

### 기본 사용

```java
// 각 스레드마다 독립적인 값 저장
private static final ThreadLocal<User> userContext =
    ThreadLocal.withInitial(() -> null);

public void setUser(User user) {
    userContext.set(user);
}

public User getUser() {
    return userContext.get();
}

public void clear() {
    userContext.remove();  // 중요! 메모리 누수 방지
}
```

### 웹 애플리케이션 예시

```java
@Component
public class RequestContext {
    private static final ThreadLocal<RequestInfo> context =
        new ThreadLocal<>();

    public static void set(RequestInfo info) {
        context.set(info);
    }

    public static RequestInfo get() {
        return context.get();
    }

    public static void clear() {
        context.remove();
    }
}

// Filter에서 사용
@Override
public void doFilter(ServletRequest request, ...) {
    try {
        RequestContext.set(new RequestInfo(request));
        chain.doFilter(request, response);
    } finally {
        RequestContext.clear();  // 반드시 정리!
    }
}
```

### 메모리 누수 주의

```
스레드 풀 환경에서 주의!
- 스레드가 재사용됨
- ThreadLocal 값이 남아있을 수 있음
- 반드시 remove() 호출

┌─────────────────────────────────────────────────────────────┐
│  Thread Pool Thread                                         │
│  ├── ThreadLocalMap                                        │
│  │   └── Entry (WeakReference<ThreadLocal>, Value)         │
│  │                                                         │
│  ThreadLocal이 GC되어도 Value는 남을 수 있음 (메모리 누수)   │
│  → remove() 호출로 Entry 자체를 제거                        │
└─────────────────────────────────────────────────────────────┘
```

### InheritableThreadLocal

```java
// 자식 스레드에 값 전달
private static final InheritableThreadLocal<String> context =
    new InheritableThreadLocal<>();

context.set("parent-value");

new Thread(() -> {
    System.out.println(context.get());  // "parent-value"
}).start();

// 주의: 스레드 풀에서는 제대로 동작 안 함
// TransmittableThreadLocal (alibaba) 사용 권장
```

---

## 4. Atomic 클래스

### AtomicInteger

```java
AtomicInteger count = new AtomicInteger(0);

// 원자적 연산
count.incrementAndGet();  // ++count
count.getAndIncrement();  // count++
count.addAndGet(5);       // count += 5
count.compareAndSet(expected, newValue);  // CAS

// 활용
public int getNextId() {
    return count.incrementAndGet();
}
```

### CAS (Compare-And-Swap)

```java
// AtomicInteger 내부 구현
public final int incrementAndGet() {
    return U.getAndAddInt(this, VALUE, 1) + 1;
}

// CAS 동작
do {
    현재값 = 메모리에서 읽기;
    새값 = 현재값 + 1;
} while (!compareAndSwap(현재값, 새값));  // 실패하면 재시도

// Lock-free: 블로킹 없이 동시성 처리
// 경합이 적을 때 효율적
```

### AtomicReference

```java
AtomicReference<User> userRef = new AtomicReference<>();

// 객체 교체
userRef.set(new User("Kim"));
User oldUser = userRef.getAndSet(new User("Lee"));

// CAS
User expected = userRef.get();
User newUser = new User("Park");
boolean success = userRef.compareAndSet(expected, newUser);
```

### LongAdder (Java 8+)

```java
// 높은 경합 상황에서 AtomicLong보다 효율적
LongAdder counter = new LongAdder();

counter.increment();
counter.add(5);
long value = counter.sum();

// 내부적으로 여러 셀에 분산하여 경합 감소
// 최종 합계 시에만 모든 셀 합산
```

---

## 5. Executor Framework

### ExecutorService

```java
// 고정 크기 스레드 풀
ExecutorService executor = Executors.newFixedThreadPool(10);

// 캐시 스레드 풀 (필요시 생성)
ExecutorService executor = Executors.newCachedThreadPool();

// 단일 스레드
ExecutorService executor = Executors.newSingleThreadExecutor();

// 스케줄 실행
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
```

### ThreadPoolExecutor 직접 생성 (권장)

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5,                      // corePoolSize
    10,                     // maximumPoolSize
    60L, TimeUnit.SECONDS,  // keepAliveTime
    new LinkedBlockingQueue<>(100),  // workQueue
    new ThreadPoolExecutor.CallerRunsPolicy()  // 거부 정책
);

// 거부 정책
// AbortPolicy: RejectedExecutionException (기본)
// CallerRunsPolicy: 호출 스레드에서 실행
// DiscardPolicy: 조용히 버림
// DiscardOldestPolicy: 가장 오래된 작업 버리고 재시도
```

### Future와 Callable

```java
Future<String> future = executor.submit(() -> {
    Thread.sleep(1000);
    return "Result";
});

// 블로킹 대기
String result = future.get();

// 타임아웃
String result = future.get(5, TimeUnit.SECONDS);

// 취소
future.cancel(true);
```

### CompletableFuture

```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> fetchData())
    .thenApply(data -> process(data))
    .thenApply(result -> format(result))
    .exceptionally(ex -> "Error: " + ex.getMessage());

// 병렬 실행 후 결합
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> "A");
CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> "B");

CompletableFuture<String> combined = future1.thenCombine(future2,
    (a, b) -> a + b);  // "AB"

// 모두 완료 대기
CompletableFuture.allOf(future1, future2).join();
```

---

## 6. 동시성 문제 패턴

### Race Condition

```java
// 문제
private int count = 0;
public void increment() {
    count++;  // read-modify-write: 원자적이지 않음
}

// 해결
private AtomicInteger count = new AtomicInteger(0);
public void increment() {
    count.incrementAndGet();
}
```

### Double-Checked Locking

```java
// 싱글톤 패턴
public class Singleton {
    private static volatile Singleton instance;  // volatile 필수!

    public static Singleton getInstance() {
        if (instance == null) {  // 첫 번째 체크 (락 없이)
            synchronized (Singleton.class) {
                if (instance == null) {  // 두 번째 체크 (락 내에서)
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}

// 더 좋은 방법: Lazy Holder
public class Singleton {
    private Singleton() {}

    private static class Holder {
        private static final Singleton INSTANCE = new Singleton();
    }

    public static Singleton getInstance() {
        return Holder.INSTANCE;
    }
}
```

### Deadlock 방지

```java
// 문제: 락 순서 불일치
// Thread1: lockA → lockB
// Thread2: lockB → lockA

// 해결: 일관된 락 순서
public void transfer(Account from, Account to, int amount) {
    Account first = from.id < to.id ? from : to;
    Account second = from.id < to.id ? to : from;

    synchronized (first) {
        synchronized (second) {
            from.withdraw(amount);
            to.deposit(amount);
        }
    }
}
```

---

## 핵심 정리

| 도구 | 용도 |
|------|------|
| synchronized | 간단한 동기화 |
| Lock | 세밀한 제어 필요 시 |
| volatile | 가시성 보장 |
| ThreadLocal | 스레드별 데이터 |
| Atomic | Lock-free 카운터 |
| Executor | 스레드 풀 관리 |

| 주의사항 | 설명 |
|----------|------|
| ThreadLocal | remove() 필수 |
| volatile | 원자성 보장 안 됨 |
| 데드락 | 락 순서 통일 |
| 스레드 풀 | 적절한 크기 설정 |

---

*마지막 업데이트: 2025년 01월*
