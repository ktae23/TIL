# Virtual Thread 내부 구현 원리: Continuation, Scheduler, epoll

---

## 목차

1. [Continuation과 ContinuationScope](#1-continuation과-continuationscope)
   - [1.1 Continuation이란?](#11-continuation이란)
   - [1.2 ContinuationScope](#12-continuationscope)
   - [1.3 기본 예제로 이해하기](#13-기본-예제로-이해하기)
   - [1.4 실행 흐름 상세 분석](#14-실행-흐름-상세-분석)
2. [Virtual Thread의 내부 스케줄링 메커니즘](#2-virtual-thread의-내부-스케줄링-메커니즘)
   - [2.1 구조 개요](#21-구조-개요)
   - [2.2 커스텀 Virtual Thread로 이해하기](#22-커스텀-virtual-thread로-이해하기)
   - [2.3 JDK 내부의 실제 스케줄링](#23-jdk-내부의-실제-스케줄링)
3. [epoll과 Poller: 비동기 I/O의 핵심](#3-epoll과-poller-비동기-io의-핵심)
   - [3.1 전통적인 Blocking I/O의 문제](#31-전통적인-blocking-io의-문제)
   - [3.2 epoll 기본 개념](#32-epoll-기본-개념)
   - [3.3 JDK Poller 구현](#33-jdk-poller-구현)
   - [3.4 Virtual Thread에서 I/O 발생 시 전체 흐름](#34-virtual-thread에서-io-발생-시-전체-흐름)
4. [실무 예제: 커스텀 Virtual Thread 구현](#4-실무-예제-커스텀-virtual-thread-구현)
   - [4.1 MyVirtualThread — Continuation 기반 경량 스레드](#41-myvirtualthread--continuation-기반-경량-스레드)
   - [4.2 MyVirtualThreadScheduler — ForkJoinPool 기반 스케줄러](#42-myvirtualthreadscheduler--forkjoinpool-기반-스케줄러)
   - [4.3 FileOperation — I/O 시뮬레이션과 yield](#43-fileoperation--io-시뮬레이션과-yield)
   - [4.4 실행 결과 분석](#44-실행-결과-분석)
5. [JDK 소스로 보는 핵심 구현](#5-jdk-소스로-보는-핵심-구현)
6. [정리: Platform Thread vs Virtual Thread 내부 비교](#6-정리-platform-thread-vs-virtual-thread-내부-비교)
7. [참고 자료](#7-참고-자료)

---

## 1. Continuation과 ContinuationScope

### 1.1 Continuation이란?

**Continuation**은 "실행을 일시 중단하고, 나중에 중단된 지점부터 다시 재개할 수 있는 실행 단위"이다. 코루틴(Coroutine)과 유사한 개념으로, Virtual Thread의 핵심 기반 기술이다.

```
일반 스레드:    [시작] ──────────────────────────> [종료]
                       (중단 불가, OS가 선점)

Continuation:  [시작] ───> [yield] ───> [재개] ───> [yield] ───> [재개] ───> [종료]
                             ↑              ↑              ↑
                        자발적 중단      자발적 중단      자발적 중단
```

핵심 특징:
- **자발적 중단(cooperative)**: OS의 선점(preemptive)이 아닌, 코드가 스스로 `yield()`를 호출하여 중단
- **스택 프레임 보존**: yield 시 현재 콜 스택 전체가 힙(heap)에 저장되고, 재개 시 복원
- **JDK 내부 API**: `jdk.internal.vm` 패키지에 위치하며, 직접 사용하려면 `--add-exports` 필요

### 1.2 ContinuationScope

**ContinuationScope**는 Continuation의 "경계"를 정의한다. yield가 호출되면, 해당 scope까지의 스택 프레임만 중단/저장된다.

```java
ContinuationScope scope = new ContinuationScope("my-scope");
```

하나의 scope에 여러 Continuation을 연결할 수 있으며, 중첩 scope도 가능하다. Virtual Thread에서는 `VThreadContinuation`이라는 전용 scope를 사용한다.

### 1.3 기본 예제로 이해하기

```java
import jdk.internal.vm.Continuation;
import jdk.internal.vm.ContinuationScope;

public class ContinuationExample {

    public static void main(String[] args) {
        ContinuationScope scope = new ContinuationScope("main");
        Continuation continuation = new Continuation(
                scope, () -> {
            System.out.println("Hello from continuation");       // (A)
            Continuation.yield(scope);                           // 첫 번째 중단
            System.out.println("Hello again from continuation"); // (B)
            Continuation.yield(scope);                           // 두 번째 중단
            System.out.println("Done from continuation");        // (C)
        });

        System.out.println("Before starting continuation");     // (1)
        continuation.run();   // (A) 실행 → 첫 번째 yield에서 멈춤
        System.out.println("After starting continuation");      // (2)
        continuation.run();   // (B) 실행 → 두 번째 yield에서 멈춤
        System.out.println("After starting continuation again");// (3)
        continuation.run();   // (C) 실행 → 완료
    }
}
```

> 컴파일/실행 시 `--add-exports java.base/jdk.internal.vm=ALL-UNNAMED` 옵션이 필요하다.

### 1.4 실행 흐름 상세 분석

```
main 스레드        │  Continuation
───────────────────┼──────────────────────────
(1) Before...      │
continuation.run() │→ (A) Hello from continuation
                   │→ yield(scope) ─── 스택을 힙에 저장
(2) After...       │
continuation.run() │→ 힙에서 스택 복원
                   │→ (B) Hello again from continuation
                   │→ yield(scope) ─── 스택을 힙에 저장
(3) After...again  │
continuation.run() │→ 힙에서 스택 복원
                   │→ (C) Done from continuation
                   │→ Continuation 종료
```

**핵심 포인트**: `continuation.run()`은 항상 **같은 carrier thread**(여기서는 main 스레드)에서 실행되지만, Continuation 내부의 실행 지점은 yield 사이를 왔다 갔다 한다. 이것이 Virtual Thread가 하나의 OS 스레드 위에서 여러 작업을 번갈아 실행할 수 있는 원리이다.

---

## 2. Virtual Thread의 내부 스케줄링 메커니즘

### 2.1 구조 개요

Virtual Thread는 내부적으로 세 가지 핵심 컴포넌트로 구성된다:

```
┌──────────────────────────────────────────────────────┐
│                    Virtual Thread                     │
│                                                      │
│  ┌──────────────┐  ┌─────────────┐  ┌─────────────┐ │
│  │ Continuation  │  │  Scheduler  │  │   State     │ │
│  │ (실행 단위)    │  │ (ForkJoin   │  │ (RUNNABLE,  │ │
│  │              │  │   Pool)     │  │  PARKING..) │ │
│  └──────────────┘  └─────────────┘  └─────────────┘ │
└──────────────────────────────────────────────────────┘
         │                    │
         ▼                    ▼
   [yield/resume]     [Carrier Thread에 마운트]
         │                    │
         ▼                    ▼
  ┌────────────────────────────────────┐
  │     ForkJoinPool (Carrier Threads) │
  │  ┌────────┐ ┌────────┐ ┌────────┐ │
  │  │Worker-1│ │Worker-2│ │Worker-3│ │
  │  └────────┘ └────────┘ └────────┘ │
  └────────────────────────────────────┘
         │
         ▼
    OS Platform Threads
```

| 컴포넌트 | 역할 |
|---------|------|
| **Continuation** | 실행의 중단/재개를 담당. yield 시 스택을 힙에 저장 |
| **Scheduler (ForkJoinPool)** | Continuation을 어떤 carrier thread에서 실행할지 결정 |
| **Carrier Thread** | 실제 OS 스레드. Virtual Thread가 마운트되어 실행되는 물리적 스레드 |

### 2.2 커스텀 Virtual Thread로 이해하기

JDK 내부를 단순화한 커스텀 구현으로 원리를 이해해보자.

**MyVirtualThread** — Continuation을 감싸는 경량 스레드:

```java
public class MyVirtualThread {
    public static final MyVirtualThreadScheduler VIRTUAL_THREAD_SCHEDULER
            = new MyVirtualThreadScheduler();
    public static final ContinuationScope SCOPE
            = new ContinuationScope("virtual-thread-scope");

    private final Continuation continuation;
    private final int vtid;

    private MyVirtualThread(Runnable runnable) {
        this.vtid = COUNTER.getAndIncrement();
        this.continuation = new Continuation(SCOPE, runnable);
    }

    public static void start(Runnable runnable) {
        var virtualThread = new MyVirtualThread(runnable);
        VIRTUAL_THREAD_SCHEDULER.schedule(virtualThread);  // 스케줄러에 등록
    }

    public void run() {
        continuation.run();  // carrier thread에서 Continuation 실행
    }
}
```

**MyVirtualThreadScheduler** — Work-Stealing Pool 기반 스케줄러:

```java
public class MyVirtualThreadScheduler {
    public static final ThreadLocal<MyVirtualThread> CURRENT_VIRTUAL_THREAD
            = new ThreadLocal<>();
    public static final ScheduledExecutorService IO_EVENT_VIRTUAL_THREAD_SCHEDULER
            = Executors.newSingleThreadScheduledExecutor();

    private final ExecutorService workStealingPool = Executors.newWorkStealingPool(2);

    public void schedule(MyVirtualThread virtualThread) {
        workStealingPool.submit(() -> {
            CURRENT_VIRTUAL_THREAD.set(virtualThread);
            virtualThread.run();         // Continuation.run() 호출
            CURRENT_VIRTUAL_THREAD.remove();
        });
    }
}
```

이 구조에서 `workStealingPool`이 JDK의 **ForkJoinPool**(carrier thread pool)에 해당한다.

### 2.3 JDK 내부의 실제 스케줄링

JDK의 `java.lang.VirtualThread` 클래스 내부에서는 다음과 같은 흐름으로 스케줄링된다:

```
1. Thread.start() 호출
       ↓
2. VirtualThread.start() → scheduler.execute(runContinuation)
       ↓
3. ForkJoinPool의 worker thread가 태스크를 가져감
       ↓
4. runContinuation() → continuation.run()
       ↓
5. 사용자 코드 실행
       ↓
6-a. I/O 블로킹 발생 → Continuation.yield() → carrier thread 반환
6-b. 작업 완료 → Continuation 종료
       ↓
7. (I/O 완료 후) scheduler.execute(runContinuation) → 다시 3번으로
```

JDK의 기본 스케줄러는 `ForkJoinPool`이며, carrier thread 수는 기본적으로 **CPU 코어 수**와 동일하다:

```java
// JDK 내부 (VirtualThread.java)
private static final ForkJoinPool DEFAULT_SCHEDULER = createDefaultScheduler();

// 기본 parallelism = Runtime.getRuntime().availableProcessors()
// JVM 옵션으로 조정 가능: -Djdk.virtualThreadScheduler.parallelism=N
```

---

## 3. epoll과 Poller: 비동기 I/O의 핵심

### 3.1 전통적인 Blocking I/O의 문제

Platform Thread에서 `socket.read()`를 호출하면:

```
Platform Thread-1:  [read() 호출] ──── OS 커널 대기 ────── [데이터 수신] → 계속 실행
                                       ↑
                              OS 스레드가 통째로 블로킹
                              (다른 작업 불가)
```

스레드 1개가 I/O 대기로 완전히 묶이므로, 동시 연결 1만 개 = 스레드 1만 개가 필요하다.

### 3.2 epoll 기본 개념

**epoll**은 Linux 커널이 제공하는 I/O 이벤트 통지 메커니즘이다 (macOS에서는 **kqueue**).

```
┌──────────────────────────────────────────────────┐
│                  epoll 인스턴스                    │
│                                                  │
│  관심 fd 목록:                                    │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐            │
│  │fd=10 │ │fd=11 │ │fd=12 │ │fd=13 │ ...        │
│  │소켓A  │ │소켓B  │ │소켓C  │ │소켓D  │            │
│  └──────┘ └──────┘ └──────┘ └──────┘            │
│                                                  │
│  epoll_wait() → "fd=11, fd=13에 데이터 도착!"     │
└──────────────────────────────────────────────────┘
```

핵심 시스템 콜:

| 시스템 콜 | 역할 |
|----------|------|
| `epoll_create` | epoll 인스턴스 생성 |
| `epoll_ctl(ADD)` | 감시할 fd(파일 디스크립터) 등록 |
| `epoll_wait` | 이벤트가 발생한 fd 목록을 반환 (블로킹 또는 타임아웃) |

**스레드 1개로 수만 개의 소켓을 동시에 감시**할 수 있다는 것이 핵심이다.

### 3.3 JDK Poller 구현

JDK 21+에서는 `sun.nio.ch.Poller` 클래스가 epoll/kqueue를 래핑하여 Virtual Thread의 I/O를 관리한다.

```
┌─────────────────────────────────────────────────────────┐
│                     JDK Poller                           │
│                                                         │
│  ┌───────────────┐     ┌──────────────────────────────┐ │
│  │ Poller Thread  │────→│ epoll_wait() / kqueue()     │ │
│  │ (데몬 스레드)   │     │ 이벤트 발생한 fd 감지         │ │
│  └───────────────┘     └──────────────────────────────┘ │
│          │                                               │
│          ▼                                               │
│  이벤트 발생 시:                                          │
│  fd → VirtualThread 매핑 테이블에서 해당 VT를 찾아         │
│  scheduler.execute(runContinuation) 호출                  │
│  → Virtual Thread가 carrier thread에 다시 마운트           │
└─────────────────────────────────────────────────────────┘
```

JDK 내부 Poller의 핵심 코드 흐름 (단순화):

```java
// sun.nio.ch.Poller (단순화된 개념 코드)
class Poller {
    // fd → 대기 중인 Virtual Thread 매핑
    private final Map<Integer, Continuation> waitingThreads = new ConcurrentHashMap<>();

    // I/O 대기 등록 (Virtual Thread가 read/write 시 호출)
    void register(int fd, Continuation cont) {
        waitingThreads.put(fd, cont);
        epollCtl(epfd, EPOLL_CTL_ADD, fd, events);  // epoll에 fd 등록
    }

    // Poller 데몬 스레드가 실행하는 루프
    void pollLoop() {
        while (true) {
            int[] readyFds = epollWait(epfd, timeout);  // 이벤트 대기
            for (int fd : readyFds) {
                Continuation cont = waitingThreads.remove(fd);
                scheduler.execute(() -> cont.run());     // VT 재개!
            }
        }
    }
}
```

### 3.4 Virtual Thread에서 I/O 발생 시 전체 흐름

`Thread.ofVirtual().start(() -> socket.read(buf))` 호출 시 내부에서 벌어지는 일:

```
시간 →

[Carrier Thread-1]
  │
  ├── VirtualThread-1 마운트
  │     │
  │     ├── socket.read(buf) 호출
  │     │     │
  │     │     ├── NIO 내부: fd에 데이터 없음 (EAGAIN)
  │     │     │
  │     │     ├── Poller.register(fd, this)  ← epoll에 fd 등록
  │     │     │
  │     │     └── Continuation.yield()       ← VT 중단, 스택을 힙에 저장
  │     │
  │     └── VirtualThread-1 언마운트 (carrier thread 반환!)
  │
  ├── VirtualThread-2 마운트            ← 즉시 다른 VT 실행 가능!
  │     └── ... 다른 작업 수행 ...
  │

[Poller Thread] (백그라운드 데몬)
  │
  ├── epoll_wait() → fd에 데이터 도착 감지!
  │
  └── scheduler.execute(VirtualThread-1.runContinuation)
              │
              ▼
[Carrier Thread-2]  (아무 carrier thread)
  │
  ├── VirtualThread-1 마운트 (다른 carrier thread에서 재개될 수 있음!)
  │     │
  │     └── socket.read(buf) 완료 → 데이터 반환
  │
  └── 계속 실행...
```

**핵심 포인트**:
1. **Carrier thread가 블로킹되지 않는다** — yield로 반환하여 즉시 다른 VT를 실행
2. **epoll이 I/O 완료를 감지** — OS 수준에서 효율적으로 수만 개 fd를 감시
3. **재개 시 다른 carrier thread에서 실행 가능** — work-stealing 덕분

---

## 4. 실무 예제: 커스텀 Virtual Thread 구현

### 4.1 MyVirtualThread — Continuation 기반 경량 스레드

```java
package org.example.virtual;

import jdk.internal.vm.Continuation;
import jdk.internal.vm.ContinuationScope;
import java.util.concurrent.atomic.AtomicInteger;

public class MyVirtualThread {
    public static final MyVirtualThreadScheduler VIRTUAL_THREAD_SCHEDULER
            = new MyVirtualThreadScheduler();
    public static final AtomicInteger COUNTER = new AtomicInteger(1);
    public static final ContinuationScope SCOPE
            = new ContinuationScope("virtual-thread-scope");

    private final Continuation continuation;
    private final int vtid;

    private MyVirtualThread(Runnable runnable) {
        this.vtid = COUNTER.getAndIncrement();
        this.continuation = new Continuation(SCOPE, runnable);
    }

    // 팩토리 메서드: VT 생성 → 스케줄러에 등록
    public static void start(Runnable runnable) {
        var virtualThread = new MyVirtualThread(runnable);
        VIRTUAL_THREAD_SCHEDULER.schedule(virtualThread);
    }

    public void run() {
        continuation.run();
    }

    public static MyVirtualThread currentVirtualThread() {
        return MyVirtualThreadScheduler.CURRENT_VIRTUAL_THREAD.get();
    }

    @Override
    public String toString() {
        return "VirtualThread - " + vtid + "-" + Thread.currentThread().getName();
    }
}
```

이 클래스는 JDK의 `java.lang.VirtualThread`를 단순화한 것이다:

| 커스텀 구현 | JDK 내부 | 역할 |
|------------|----------|------|
| `MyVirtualThread` | `VirtualThread` | Continuation + 스케줄러를 감싸는 경량 스레드 |
| `SCOPE` | `VThreadContinuation.VTHREAD_SCOPE` | Continuation의 yield 경계 |
| `VIRTUAL_THREAD_SCHEDULER` | `DEFAULT_SCHEDULER` (ForkJoinPool) | 스케줄러 |

### 4.2 MyVirtualThreadScheduler — ForkJoinPool 기반 스케줄러

```java
package org.example.virtual;

import java.util.concurrent.*;

public class MyVirtualThreadScheduler {
    // 현재 carrier thread에서 실행 중인 VT를 추적
    public static final ThreadLocal<MyVirtualThread> CURRENT_VIRTUAL_THREAD
            = new ThreadLocal<>();

    // I/O 이벤트 시뮬레이션용 (JDK에서는 Poller가 이 역할)
    public static final ScheduledExecutorService IO_EVENT_VIRTUAL_THREAD_SCHEDULER
            = Executors.newSingleThreadScheduledExecutor();

    // carrier thread pool (JDK에서는 ForkJoinPool)
    private final ExecutorService workStealingPool = Executors.newWorkStealingPool(2);

    public void schedule(MyVirtualThread virtualThread) {
        workStealingPool.submit(() -> {
            CURRENT_VIRTUAL_THREAD.set(virtualThread);  // VT를 carrier thread에 마운트
            virtualThread.run();                         // Continuation.run()
            CURRENT_VIRTUAL_THREAD.remove();             // VT 언마운트
        });
    }
}
```

`IO_EVENT_VIRTUAL_THREAD_SCHEDULER`가 JDK의 **Poller** 역할을 시뮬레이션한다:
- JDK Poller: epoll_wait()로 I/O 완료 감지 → VT 재스케줄링
- 이 구현: ScheduledExecutorService로 지연 후 → VT 재스케줄링

### 4.3 FileOperation — I/O 시뮬레이션과 yield

```java
package org.example.virtual;

import jdk.internal.vm.Continuation;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.example.virtual.MyVirtualThread.SCOPE;
import static org.example.virtual.MyVirtualThread.currentVirtualThread;

public class FileOperation {
    private final Random random = new Random();

    public void transfer(String filePath) {
        System.out.println("Start transferring file: " + filePath);

        MyVirtualThread myVirtualThread = currentVirtualThread();

        // ① I/O 완료 이벤트를 시뮬레이션 (JDK에서는 Poller가 담당)
        MyVirtualThreadScheduler.IO_EVENT_VIRTUAL_THREAD_SCHEDULER.schedule(
                () -> MyVirtualThread.VIRTUAL_THREAD_SCHEDULER.schedule(myVirtualThread),
                random.nextInt(1000),    // 랜덤 지연 = I/O 대기 시간
                TimeUnit.MILLISECONDS
        );

        // ② carrier thread에서 VT 정보 제거
        MyVirtualThreadScheduler.CURRENT_VIRTUAL_THREAD.remove();

        // ③ Continuation yield — carrier thread를 반환!
        Continuation.yield(SCOPE);

        // ④ I/O 완료 후 여기서 재개
        System.out.println("Transfer completed for file: " + filePath);
    }
}
```

이 흐름을 JDK 내부와 대비하면:

```
커스텀 구현                              JDK 내부
─────────────────────────────────────────────────────
IO_EVENT_SCHEDULER.schedule(...)    →   Poller.register(fd, vt)
  random delay 후 재스케줄링              epoll이 fd 이벤트 감지 후 재스케줄링
Continuation.yield(SCOPE)           →   Continuation.yield(VTHREAD_SCOPE)
  carrier thread 반환                    carrier thread 반환
scheduler.schedule(vt)              →   scheduler.execute(runContinuation)
  다른 carrier thread에서 재개            다른 carrier thread에서 재개
```

### 4.4 실행 결과 분석

```java
public class MyVirtualThreadDemo {
    public static void main(String[] args) throws InterruptedException {
        FileOperation fileOperation = new FileOperation();
        for (int i = 0; i < 4; i++) {
            int finalI = i;
            MyVirtualThread.start(() -> {
                System.out.println("Transfer: File_" + finalI
                    + " Running in VirtualThread: " + MyVirtualThread.currentVirtualThread());
                fileOperation.transfer("File_" + finalI);
                System.out.println("Transfer: File_" + finalI
                    + " Completed in VirtualThread: " + MyVirtualThread.currentVirtualThread());
            });
        }
        Thread.sleep(Duration.ofSeconds(5));
    }
}
```

실행 결과 (비동기 실행, 순서 불확정):

```
Transfer: File_0 Running in VirtualThread: VirtualThread - 1-ForkJoinPool-1-worker-1
Transfer: File_1 Running in VirtualThread: VirtualThread - 2-ForkJoinPool-1-worker-2
Start transferring file: File_0
Start transferring file: File_1
Transfer: File_2 Running in VirtualThread: VirtualThread - 3-ForkJoinPool-1-worker-1
Transfer: File_3 Running in VirtualThread: VirtualThread - 4-ForkJoinPool-1-worker-2
Start transferring file: File_2
Start transferring file: File_3
Transfer completed for file: File_1           ← File_1이 먼저 완료!
Transfer: File_1 Completed in VirtualThread: VirtualThread - 2-ForkJoinPool-1-worker-1
Transfer completed for file: File_3
Transfer: File_3 Completed in VirtualThread: VirtualThread - 4-ForkJoinPool-1-worker-2
Transfer completed for file: File_0
Transfer: File_0 Completed in VirtualThread: VirtualThread - 1-ForkJoinPool-1-worker-1
Transfer completed for file: File_2
Transfer: File_2 Completed in VirtualThread: VirtualThread - 3-ForkJoinPool-1-worker-2
```

**주목할 점**:
- **carrier thread는 2개뿐** (`worker-1`, `worker-2`)이지만 **4개의 VT가 동시에 실행**된다
- I/O 대기(yield) 중에 carrier thread가 다른 VT를 실행한다
- 완료 순서가 시작 순서와 다르다 — 비동기 처리의 증거

---

## 5. JDK 소스로 보는 핵심 구현

### VirtualThread.java의 park/unpark

Virtual Thread에서 I/O 블로킹이 발생하면 내부적으로 `park()`가 호출된다:

```java
// java.lang.VirtualThread (JDK 21 소스 단순화)
void park() {
    setState(PARKING);

    // Continuation yield — carrier thread 반환
    boolean yielded = yieldContinuation();

    if (!yielded) {
        // yield 실패 시 (pinned 상태) → carrier thread를 직접 블로킹
        parkOnCarrierThread();
    }
}

void unpark() {
    // I/O 완료 → Poller가 호출
    setState(RUNNABLE);
    scheduler.execute(runContinuation);  // 다시 스케줄링
}

private boolean yieldContinuation() {
    // 스택 프레임을 힙에 복사하고 carrier thread를 반환
    return Continuation.yield(VTHREAD_SCOPE);
}
```

### NIO SocketChannel의 Virtual Thread 대응

```java
// sun.nio.ch.SocketChannelImpl (단순화)
int read(ByteBuffer buf) throws IOException {
    int n = IOUtil.read(fd, buf);          // non-blocking read 시도

    if (n == IOStatus.UNAVAILABLE) {       // 데이터 없음 (EAGAIN)
        // Virtual Thread라면:
        Poller.register(fd, READ);          // epoll에 fd 등록
        LockSupport.park();                 // → VirtualThread.park() → yield

        // unpark 후 여기서 재개
        n = IOUtil.read(fd, buf);           // 이번에는 데이터가 있음
    }
    return n;
}
```

### Poller의 이벤트 루프

```java
// sun.nio.ch.Poller (단순화)
class Poller {
    void poll() {
        while (!shutdown) {
            // OS별 구현: Linux=epoll_wait, macOS=kevent
            int numEvents = pollWait(pollfd, events, timeout);

            for (int i = 0; i < numEvents; i++) {
                int fd = events[i].fd;
                Thread vt = fdToThread.remove(fd);
                LockSupport.unpark(vt);     // → VirtualThread.unpark() → 재스케줄링
            }
        }
    }
}
```

---

## 6. 정리: Platform Thread vs Virtual Thread 내부 비교

| 항목 | Platform Thread | Virtual Thread |
|------|----------------|----------------|
| **스케줄링 주체** | OS 커널 | JVM (ForkJoinPool) |
| **스택 저장 위치** | OS가 할당한 고정 메모리 (기본 1MB) | 힙 메모리 (동적, 수 KB~) |
| **I/O 블로킹 시** | OS 스레드 자체가 블로킹 | Continuation.yield()로 carrier thread 반환 |
| **I/O 완료 감지** | OS가 스레드를 깨움 | Poller(epoll/kqueue)가 감지 → unpark |
| **컨텍스트 스위칭** | 커널 모드 전환 (수 µs) | 유저 모드에서 스택 swap (수십 ns) |
| **생성 비용** | 높음 (커널 자원 할당) | 낮음 (일반 Java 객체) |
| **동시 실행 수** | 수천 개가 한계 | 수백만 개 가능 |

### 전체 아키텍처 요약도

```
┌─────────────────────── JVM ───────────────────────────┐
│                                                       │
│  VirtualThread-1  VirtualThread-2  ...  VirtualThread-N│
│       │                │                     │        │
│       └── Continuation ┘─── Continuation ────┘        │
│                    │                                  │
│              ┌─────┴──────┐                           │
│              │  Scheduler  │ (ForkJoinPool)            │
│              └─────┬──────┘                           │
│        ┌───────────┼───────────┐                      │
│   Carrier-1   Carrier-2   Carrier-3                   │
│                                                       │
│   ┌──────────────────────────┐                        │
│   │       Poller Thread       │                        │
│   │  epoll_wait / kqueue      │                        │
│   │  fd 이벤트 → VT unpark     │                        │
│   └──────────────────────────┘                        │
└───────────────────────────────────────────────────────┘
                      │
              ┌───────┴───────┐
              │   OS Kernel    │
              │  epoll/kqueue  │
              │  실제 I/O 처리  │
              └───────────────┘
```

---

## 7. 참고 자료

- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [JEP 425: Virtual Threads (Preview)](https://openjdk.org/jeps/425)
- [Ron Pressler — Project Loom: Fibers and Continuations (JVM Language Summit)](https://cr.openjdk.org/~rpressler/loom/Loom-Proposal.html)
- [OpenJDK VirtualThread.java 소스](https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/lang/VirtualThread.java)
- [OpenJDK Poller.java 소스](https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/sun/nio/ch/Poller.java)
- [Linux epoll man page](https://man7.org/linux/man-pages/man7/epoll.7.html)

*마지막 업데이트: 2026년 03월*
