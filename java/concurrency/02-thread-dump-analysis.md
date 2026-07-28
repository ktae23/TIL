# Java 스레드 덤프 분석

스레드 상태 해석과 데드락 감지 방법을 정리합니다.

## 목차

1. [스레드 덤프 생성](#1-스레드-덤프-생성)
2. [스레드 상태 이해](#2-스레드-상태-이해)
3. [덤프 분석 방법](#3-덤프-분석-방법)
4. [데드락 감지](#4-데드락-감지)
5. [일반적인 문제 패턴](#5-일반적인-문제-패턴)
6. [분석 도구](#6-분석-도구)

---

## 1. 스레드 덤프 생성

### 덤프 생성 방법

```bash
# 1. jstack 사용
jstack <PID> > thread_dump.txt

# 강제 덤프 (응답 없는 JVM)
jstack -F <PID> > thread_dump.txt

# 2. jcmd 사용 (권장)
jcmd <PID> Thread.print > thread_dump.txt

# 3. kill 시그널 (Linux)
kill -3 <PID>
# 표준 출력으로 덤프됨

# 4. JVisualVM / JConsole (GUI)
# Threads 탭에서 Thread Dump 버튼
```

### PID 확인

```bash
# jps
jps -v | grep MyApp

# ps
ps aux | grep java

# jcmd
jcmd -l
```

### 여러 번 덤프

```bash
# 3초 간격으로 3번 덤프 (추세 분석용)
for i in 1 2 3; do
    jstack <PID> > thread_dump_$i.txt
    sleep 3
done
```

---

## 2. 스레드 상태 이해

### 스레드 상태

```
┌─────────────────────────────────────────────────────────────┐
│  NEW         → 생성됨, 아직 start() 호출 안 됨             │
│      ↓ start()                                              │
│  RUNNABLE    → 실행 중 또는 실행 대기                       │
│      ↓ ↑                                                    │
│  BLOCKED     → 모니터 락 대기 (synchronized)                │
│      ↓ ↑                                                    │
│  WAITING     → 무기한 대기 (wait, join, park)               │
│      ↓ ↑                                                    │
│  TIMED_WAITING → 시간 제한 대기 (sleep, wait(timeout))      │
│      ↓                                                      │
│  TERMINATED  → 종료됨                                       │
└─────────────────────────────────────────────────────────────┘
```

### 상태별 설명

```
RUNNABLE:
  - CPU에서 실행 중이거나 실행 대기 중
  - I/O 대기도 RUNNABLE로 표시됨

BLOCKED:
  - synchronized 블록 진입 대기
  - 다른 스레드가 모니터 락 보유 중

WAITING:
  - Object.wait() (notify 대기)
  - Thread.join() (다른 스레드 종료 대기)
  - LockSupport.park()

TIMED_WAITING:
  - Thread.sleep(ms)
  - Object.wait(ms)
  - Thread.join(ms)
  - LockSupport.parkNanos/parkUntil
```

---

## 3. 덤프 분석 방법

### 덤프 형식

```
"http-nio-8080-exec-1" #20 daemon prio=5 os_prio=0 tid=0x00007f... nid=0x5a0f waiting on condition [0x00007f...]
   java.lang.Thread.State: TIMED_WAITING (sleeping)
        at java.lang.Thread.sleep(Native Method)
        at com.example.MyService.process(MyService.java:42)
        at com.example.MyController.handle(MyController.java:25)
        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        ...

   Locked ownable synchronizers:
        - None
```

### 주요 정보

```
"http-nio-8080-exec-1"  → 스레드 이름
#20                     → 스레드 번호
daemon                  → 데몬 스레드 여부
prio=5                  → Java 우선순위
tid=0x00007f...         → Java 스레드 ID
nid=0x5a0f              → Native 스레드 ID (OS)

Thread.State: TIMED_WAITING (sleeping)  → 현재 상태

스택 트레이스:
- 맨 위가 현재 실행 중인 메서드
- 아래로 갈수록 호출 스택 상위
```

### 분석 단계

```
1. 스레드 개수 확인
   - 전체 스레드 수
   - 상태별 분포

2. 문제 스레드 식별
   - BLOCKED 상태 확인
   - WAITING 상태가 과다한지

3. 스택 트레이스 분석
   - 어느 코드에서 멈춰있는지
   - 어떤 락을 기다리는지

4. 패턴 확인
   - 여러 스레드가 같은 지점에서 대기?
   - 데드락 존재?
```

---

## 4. 데드락 감지

### 데드락 덤프 예시

```
Found one Java-level deadlock:
=============================
"Thread-1":
  waiting to lock monitor 0x00007f... (object 0x00000000..., a java.lang.Object),
  which is held by "Thread-2"
"Thread-2":
  waiting to lock monitor 0x00007f... (object 0x00000000..., a java.lang.Object),
  which is held by "Thread-1"

Java stack information for the threads listed above:
===================================================
"Thread-1":
        at com.example.DeadlockExample.method1(DeadlockExample.java:20)
        - waiting to lock <0x00000000...> (a java.lang.Object)
        - locked <0x00000000...> (a java.lang.Object)
        at com.example.DeadlockExample.run(DeadlockExample.java:10)

"Thread-2":
        at com.example.DeadlockExample.method2(DeadlockExample.java:30)
        - waiting to lock <0x00000000...> (a java.lang.Object)
        - locked <0x00000000...> (a java.lang.Object)
        at com.example.DeadlockExample.run(DeadlockExample.java:15)
```

### 데드락 원인 코드

```java
public class DeadlockExample {
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();

    public void method1() {
        synchronized (lock1) {
            // ... 작업 ...
            synchronized (lock2) {  // lock2 대기
                // ...
            }
        }
    }

    public void method2() {
        synchronized (lock2) {
            // ... 작업 ...
            synchronized (lock1) {  // lock1 대기 → 데드락!
                // ...
            }
        }
    }
}
```

### 해결 방법

```java
// 1. 락 순서 통일
public void method1() {
    synchronized (lock1) {
        synchronized (lock2) {
            // ...
        }
    }
}

public void method2() {
    synchronized (lock1) {  // lock1 먼저!
        synchronized (lock2) {
            // ...
        }
    }
}

// 2. tryLock 사용
public void methodWithTryLock() {
    while (true) {
        if (lock1.tryLock()) {
            try {
                if (lock2.tryLock()) {
                    try {
                        // 작업 수행
                        return;
                    } finally {
                        lock2.unlock();
                    }
                }
            } finally {
                lock1.unlock();
            }
        }
        Thread.sleep(100);  // 재시도 전 대기
    }
}
```

---

## 5. 일반적인 문제 패턴

### 스레드 풀 고갈

```
증상: 많은 스레드가 WAITING 또는 TIMED_WAITING
원인: 작업이 완료되지 않고 스레드 점유

"http-nio-8080-exec-1" WAITING
  at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await
  at java.util.concurrent.LinkedBlockingQueue.take
  at java.util.concurrent.ThreadPoolExecutor.getTask

해결:
- 스레드 풀 크기 증가
- 작업 타임아웃 설정
- 블로킹 작업 최적화
```

### 락 경합

```
증상: 많은 스레드가 BLOCKED 상태
원인: 하나의 락에 여러 스레드가 대기

"Thread-1" BLOCKED
  at com.example.Service.process(Service.java:20)
  - waiting to lock <0x00000000...>

"Thread-2" BLOCKED
  at com.example.Service.process(Service.java:20)
  - waiting to lock <0x00000000...>

"Thread-3" RUNNABLE
  at com.example.Service.process(Service.java:25)
  - locked <0x00000000...>  ← 이 스레드가 락 보유

해결:
- 락 범위 축소
- 읽기/쓰기 락 분리
- 락 프리 자료구조 사용
```

### CPU 100% 문제

```
증상: 특정 스레드가 계속 RUNNABLE
원인: 무한 루프 또는 과도한 연산

"Worker-1" RUNNABLE
  at java.util.HashMap.hash(HashMap.java:338)
  at java.util.HashMap.get(HashMap.java:556)
  at com.example.Cache.lookup(Cache.java:45)
  ...

해결:
- 반복 덤프로 같은 위치 확인
- 해당 코드 검토
- 알고리즘 최적화
```

---

## 6. 분석 도구

### 온라인 도구

```
- FastThread (https://fastthread.io)
  - 웹 기반 분석
  - 시각화 제공

- Spotify Thread Analyzer
  - 오픈소스
  - 상태별 분류
```

### 명령줄 분석

```bash
# 상태별 스레드 수 카운트
grep "java.lang.Thread.State:" thread_dump.txt | sort | uniq -c

# BLOCKED 스레드 찾기
grep -A 30 "BLOCKED" thread_dump.txt

# 특정 패키지 스레드 찾기
grep -B 2 -A 20 "com.example" thread_dump.txt
```

### 스크립트 예시

```bash
#!/bin/bash
# 스레드 상태 요약

echo "=== Thread State Summary ==="
grep "java.lang.Thread.State:" $1 | \
    sed 's/.*State: //' | \
    sort | uniq -c | sort -rn

echo ""
echo "=== Deadlock Check ==="
if grep -q "Found.*deadlock" $1; then
    echo "⚠️  DEADLOCK DETECTED!"
    grep -A 50 "Found.*deadlock" $1
else
    echo "✅ No deadlock found"
fi

echo ""
echo "=== BLOCKED Threads ==="
grep "BLOCKED" $1 | wc -l
```

---

## 분석 체크리스트

```
□ 전체 스레드 수 확인
□ 상태별 분포 확인
□ 데드락 여부 확인 (자동 감지 메시지)
□ BLOCKED 스레드 분석
  - 어떤 락을 기다리는지
  - 누가 락을 보유하는지
□ WAITING 스레드 분석
  - 정상적인 대기인지
  - 너무 많은 스레드가 대기 중인지
□ CPU 문제 시 RUNNABLE 스레드 확인
  - 같은 위치에서 반복 실행되는지
□ 반복 덤프로 추세 확인
```

---

*마지막 업데이트: 2026년 01월*
