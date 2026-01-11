# Redis 동작 원리와 소스 분석

Redis의 내부 아키텍처와 동작 원리를 심층 분석합니다. 싱글 스레드 모델, 락 메커니즘, 요청 순차 처리 방식을 소스 코드 레벨에서 살펴봅니다.

## 목차

1. [Redis 싱글 스레드 아키텍처](#redis-싱글-스레드-아키텍처)
2. [이벤트 루프와 요청 처리](#이벤트-루프와-요청-처리)
3. [락 없는 동시성 처리](#락-없는-동시성-처리)
4. [요청 순차 처리 메커니즘](#요청-순차-처리-메커니즘)
5. [멀티스레딩 도입 (Redis 6.0+)](#멀티스레딩-도입-redis-60)

---

## Redis 싱글 스레드 아키텍처

### 왜 싱글 스레드인가?

Redis는 **메인 스레드 하나**로 모든 클라이언트 요청을 처리합니다. 이는 의도적인 설계 결정입니다.

```
┌─────────────────────────────────────────────┐
│              Redis Server                   │
│  ┌───────────────────────────────────────┐  │
│  │          Main Thread                  │  │
│  │  ┌─────────────────────────────────┐  │  │
│  │  │     Event Loop (ae.c)           │  │  │
│  │  │  - 파일 이벤트 처리              │  │  │
│  │  │  - 시간 이벤트 처리              │  │  │
│  │  │  - 클라이언트 요청/응답          │  │  │
│  │  └─────────────────────────────────┘  │  │
│  └───────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

**싱글 스레드의 장점:**

| 장점 | 설명 |
|------|------|
| **락 불필요** | 데이터 구조 접근 시 동기화 오버헤드 없음 |
| **컨텍스트 스위칭 없음** | CPU 캐시 효율 극대화 |
| **원자성 보장** | 모든 명령이 원자적으로 실행됨 |
| **단순한 코드** | 동시성 버그 없는 깔끔한 구현 |
| **예측 가능한 성능** | 일관된 지연 시간 |

### 소스 코드 분석: server.c

Redis 서버의 메인 함수는 다음과 같은 구조를 가집니다:

```c
// server.c - Redis 메인 진입점
int main(int argc, char **argv) {
    // 1. 서버 초기화
    initServerConfig();

    // 2. 이벤트 루프 생성
    server.el = aeCreateEventLoop(server.maxclients + CONFIG_FDSET_INCR);

    // 3. TCP 소켓 리스닝 시작
    listenToPort(server.port, &server.ipfd);

    // 4. 이벤트 핸들러 등록
    aeCreateFileEvent(server.el, fd, AE_READABLE, acceptTcpHandler, NULL);

    // 5. 메인 이벤트 루프 진입 (여기서 블로킹)
    aeMain(server.el);

    return 0;
}
```

---

## 이벤트 루프와 요청 처리

### ae.c - Redis 이벤트 라이브러리

Redis는 자체 이벤트 라이브러리 `ae`(A simple Event library)를 사용합니다. 이는 플랫폼별로 최적의 I/O 멀티플렉싱을 선택합니다:

```c
// ae.c - 플랫폼별 I/O 멀티플렉서 선택
#ifdef HAVE_EVPORT
#include "ae_evport.c"      // Solaris
#elif defined(HAVE_EPOLL)
#include "ae_epoll.c"       // Linux (가장 일반적)
#elif defined(HAVE_KQUEUE)
#include "ae_kqueue.c"      // BSD/macOS
#else
#include "ae_select.c"      // 폴백 (POSIX)
#endif
```

### 이벤트 루프 핵심 구조

```c
// ae.h - 이벤트 루프 구조체
typedef struct aeEventLoop {
    int maxfd;                      // 현재 등록된 최대 fd
    int setsize;                    // 추적 가능한 최대 fd 수
    long long timeEventNextId;      // 다음 시간 이벤트 ID
    aeFileEvent *events;            // 등록된 파일 이벤트 배열
    aeFiredEvent *fired;            // 발생한 이벤트 배열
    aeTimeEvent *timeEventHead;     // 시간 이벤트 연결 리스트
    int stop;                       // 루프 중지 플래그
    void *apidata;                  // 플랫폼별 데이터 (epoll fd 등)
} aeEventLoop;
```

### 메인 루프 동작

```c
// ae.c - 메인 이벤트 루프
void aeMain(aeEventLoop *eventLoop) {
    eventLoop->stop = 0;
    while (!eventLoop->stop) {
        // beforeSleep: 매 루프 전 실행 (응답 버퍼 플러시 등)
        if (eventLoop->beforesleep != NULL)
            eventLoop->beforesleep(eventLoop);

        // 이벤트 처리 (블로킹)
        aeProcessEvents(eventLoop, AE_ALL_EVENTS | AE_CALL_AFTER_SLEEP);
    }
}
```

### 이벤트 처리 흐름

```
클라이언트 요청 → epoll_wait() →
  ├─ 읽기 가능 이벤트 → readQueryFromClient()
  │   └─ 명령 파싱 → processCommand() → 명령 실행
  └─ 쓰기 가능 이벤트 → sendReplyToClient()
      └─ 응답 버퍼 전송
```

```c
// networking.c - 클라이언트 요청 읽기
void readQueryFromClient(connection *conn) {
    client *c = connGetPrivateData(conn);

    // 소켓에서 데이터 읽기
    nread = connRead(c->conn, c->querybuf + qblen, readlen);

    // 버퍼에 추가
    sdsIncrLen(c->querybuf, nread);

    // 명령 처리
    processInputBuffer(c);
}
```

---

## 락 없는 동시성 처리

### 왜 락이 필요 없는가?

Redis가 락을 사용하지 않는 이유는 **싱글 스레드**이기 때문입니다:

```
┌─────────────────────────────────────────────────────────┐
│  멀티스레드 시스템 (일반적인 데이터베이스)               │
│                                                         │
│  Thread 1 ──┐                                           │
│             ├──→ [Lock] → Data → [Unlock]              │
│  Thread 2 ──┘         ↑                                 │
│                       │                                 │
│              경쟁 상태, 데드락 가능                      │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  Redis 싱글 스레드                                       │
│                                                         │
│  Request 1 ───→ Data ───→ Response 1                    │
│  Request 2 ───→ Data ───→ Response 2  (순차 처리)       │
│  Request 3 ───→ Data ───→ Response 3                    │
│                                                         │
│              락 불필요, 항상 일관된 상태                  │
└─────────────────────────────────────────────────────────┘
```

### 원자적 명령 실행

모든 Redis 명령은 **원자적**으로 실행됩니다:

```c
// t_string.c - INCR 명령 구현
void incrCommand(client *c) {
    long long value, oldvalue;
    robj *o, *new;

    // 1. 키 조회 (인터럽트 없음)
    o = lookupKeyWrite(c->db, c->argv[1]);

    // 2. 값 증가 (다른 스레드 간섭 없음)
    oldvalue = value;
    value++;

    // 3. 새 값 저장 (원자적 완료)
    new = createStringObjectFromLongLong(value);
    dbOverwrite(c->db, c->argv[1], new);

    // 4. 응답
    addReplyLongLong(c, value);
}
```

**INCR이 원자적인 이유:**
1. 싱글 스레드이므로 실행 중간에 다른 명령이 끼어들 수 없음
2. 읽기-수정-쓰기 전체가 하나의 연속된 실행
3. 클라이언트 관점에서 완벽한 원자성 보장

---

## 요청 순차 처리 메커니즘

### 클라이언트 큐잉

각 클라이언트는 자신만의 입력 버퍼와 출력 버퍼를 가집니다:

```c
// server.h - 클라이언트 구조체 (간략화)
typedef struct client {
    int fd;                     // 소켓 파일 디스크립터
    sds querybuf;               // 입력 버퍼 (받은 명령)
    size_t qb_pos;              // 버퍼 읽기 위치

    list *reply;                // 출력 버퍼 (보낼 응답)

    int argc;                   // 현재 명령 인자 수
    robj **argv;                // 현재 명령 인자들

    struct redisCommand *cmd;   // 현재 실행할 명령
} client;
```

### 순차 처리 보장

```c
// networking.c - 명령 처리
void processInputBuffer(client *c) {
    while (sdslen(c->querybuf) > 0) {
        // 1. 명령 하나 파싱
        if (processMultibulkBuffer(c) != C_OK) break;

        // 2. 명령 실행 (완료될 때까지 블로킹)
        processCommand(c);

        // 3. 다음 명령으로 이동
    }
}
```

### 파이프라이닝과 순차 처리

클라이언트가 여러 명령을 한 번에 보내도(파이프라이닝) 순서가 보장됩니다:

```
클라이언트 전송: SET a 1 | SET b 2 | GET a | GET b

Redis 처리 순서:
  1. SET a 1  → OK
  2. SET b 2  → OK
  3. GET a    → "1"
  4. GET b    → "2"

응답 순서: OK | OK | "1" | "2" (전송 순서와 동일)
```

```c
// networking.c - 파이프라인 처리
void processInputBuffer(client *c) {
    while (c->qb_pos < sdslen(c->querybuf)) {
        // 버퍼에 완전한 명령이 있으면 처리
        if (processInlineBuffer(c) == C_OK ||
            processMultibulkBuffer(c) == C_OK) {

            // 순차적으로 명령 실행
            if (processCommandAndResetClient(c) == C_ERR) {
                return;
            }
        }
    }
}
```

---

## 멀티스레딩 도입 (Redis 6.0+)

### I/O 스레딩

Redis 6.0부터 **I/O 작업만** 멀티스레드로 처리할 수 있습니다:

```
┌─────────────────────────────────────────────────────────┐
│  Redis 6.0+ 멀티스레드 I/O                               │
│                                                         │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐                  │
│  │ I/O     │  │ I/O     │  │ I/O     │   I/O 스레드     │
│  │ Thread 1│  │ Thread 2│  │ Thread 3│   (읽기/쓰기)    │
│  └────┬────┘  └────┬────┘  └────┬────┘                  │
│       │           │            │                        │
│       └───────────┼────────────┘                        │
│                   ▼                                     │
│         ┌─────────────────┐                             │
│         │   Main Thread   │   메인 스레드               │
│         │  (명령 실행)     │   (실제 데이터 처리)        │
│         └─────────────────┘                             │
└─────────────────────────────────────────────────────────┘
```

**핵심: 명령 실행은 여전히 싱글 스레드**

```c
// networking.c - I/O 스레드 처리
void handleClientsWithPendingReadsUsingThreads(void) {
    // 1. 읽기 대기 중인 클라이언트를 I/O 스레드에 분배
    listIter li;
    listNode *ln;
    int item_id = 0;

    listRewind(server.clients_pending_read, &li);
    while ((ln = listNext(&li))) {
        client *c = listNodeValue(ln);
        int target_id = item_id % server.io_threads_num;
        listAddNodeTail(io_threads_list[target_id], c);
        item_id++;
    }

    // 2. I/O 스레드들이 읽기 완료할 때까지 대기
    while (getIOPendingCount() != 0) {
        // 바쁜 대기 (busy wait)
    }

    // 3. 메인 스레드에서 명령 실행 (순차적)
    while (listLength(server.clients_pending_read)) {
        client *c = listNodeValue(listFirst(server.clients_pending_read));
        processCommandAndResetClient(c);
    }
}
```

### I/O 스레딩 설정

```conf
# redis.conf
io-threads 4              # I/O 스레드 수 (CPU 코어에 맞게)
io-threads-do-reads yes   # 읽기도 멀티스레드로 처리
```

---

## 성능 최적화 포인트

### 병목 지점 이해

```
┌─────────────────────────────────────────────────────────┐
│  Redis 성능 병목                                         │
│                                                         │
│  1. 네트워크 I/O        ← I/O 스레딩으로 개선 가능       │
│  2. 시스템 콜 오버헤드   ← epoll로 최소화               │
│  3. 명령 실행 시간       ← O(1) 자료구조 사용           │
│  4. 메모리 할당          ← jemalloc 사용                │
└─────────────────────────────────────────────────────────┘
```

### 권장 사항

| 상황 | 권장 설정 |
|------|-----------|
| CPU 바운드 워크로드 | 싱글 스레드 유지 |
| 많은 클라이언트 연결 | I/O 스레딩 활성화 |
| 대용량 값 처리 | I/O 스레딩 + 적절한 스레드 수 |
| 낮은 지연 시간 필요 | 싱글 스레드 (컨텍스트 스위칭 없음) |

---

## 정리

| 개념 | 설명 |
|------|------|
| **싱글 스레드** | 메인 이벤트 루프가 모든 명령을 순차 처리 |
| **락 불필요** | 동시 접근이 없으므로 동기화 오버헤드 없음 |
| **원자성** | 각 명령은 중단 없이 완전히 실행됨 |
| **이벤트 기반** | epoll/kqueue로 효율적인 I/O 멀티플렉싱 |
| **I/O 스레딩** | 6.0+에서 네트워크 I/O만 병렬화 (명령 실행은 여전히 싱글) |

Redis의 단순한 싱글 스레드 모델은 복잡한 동시성 문제를 피하면서도 초당 수십만 개의 명령을 처리할 수 있는 뛰어난 성능을 제공합니다.

*마지막 업데이트: 2026년 01월*
