# Logstash 큐 시스템

Logstash의 내부 큐 시스템은 파이프라인의 input과 filter/output 사이에서 이벤트를 버퍼링하며, Memory Queue와 Persistent Queue 두 가지 모드를 통해 성능과 내구성 사이의 트레이드오프를 제공한다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### Memory Queue vs Persistent Queue

Logstash는 두 가지 큐 모드를 제공한다.

| 구분 | Memory Queue | Persistent Queue (PQ) |
|------|-------------|----------------------|
| 저장 위치 | JVM 힙 메모리 | 디스크 (memory-mapped file) |
| 내구성 | 프로세스 종료 시 유실 | 프로세스 재시작 후 복구 |
| 성능 | 높음 (메모리 직접 접근) | 약간 낮음 (디스크 I/O) |
| 설정 | `queue.type: memory` (기본값) | `queue.type: persisted` |
| 용량 제한 | `pipeline.batch.size` 기반 | `queue.max_bytes` 설정 |

### AckedQueue 핵심 구조

Persistent Queue의 핵심은 `Queue` 클래스로, Head Page와 Tail Page 개념을 사용한다.

- **Head Page**: 현재 쓰기가 진행되는 활성 페이지
- **Tail Pages**: 쓰기가 완료되어 읽기 전용이 된 페이지들
- **Unread Tail Pages**: 아직 읽히지 않은 tail 페이지 목록
- **Sequence Number**: 각 이벤트에 순차적으로 부여되는 고유 번호
- **Acknowledgement (Ack)**: 이벤트가 성공적으로 처리되었음을 표시

### Dead Letter Queue (DLQ)

처리 실패한 이벤트를 별도의 큐에 저장하여 데이터 유실을 방지하는 메커니즘이다.

- 세그먼트 파일 기반의 순차 저장
- Age/Size 기반 보관 정책
- `DROP_NEWER` / `DROP_OLDER` 스토리지 정책

## 2. 왜 알아야 하는가 (Why)

### 데이터 유실 방지

프로덕션 환경에서 Logstash 프로세스가 비정상 종료되면 Memory Queue에 있던 모든 미처리 이벤트가 유실된다. Persistent Queue를 사용하면 디스크에 기록된 이벤트는 프로세스 재시작 후 복구할 수 있다.

### 백프레셔(Backpressure) 관리

Elasticsearch가 일시적으로 느려지거나 다운되었을 때, 큐가 버퍼 역할을 하여 upstream 시스템(Beats, 애플리케이션)에 대한 백프레셔를 완화한다. `queue.max_bytes` 설정으로 디스크 사용량을 제어하면서도 충분한 버퍼를 확보할 수 있다.

### 장애 분석을 위한 DLQ

output 플러그인에서 매핑 오류 등으로 이벤트 처리가 실패할 때, 해당 이벤트를 DLQ에 보관하여 나중에 원인 분석 및 재처리가 가능하다.

## 3. 내부 구현 분석 (How)

### Persistent Queue 아키텍처

```
                    Logstash Pipeline
 ┌──────────────────────────────────────────────────┐
 │                                                  │
 │  Input ──▶ [Queue] ──▶ Filter ──▶ Output         │
 │             │                        │            │
 │             ▼                        ▼            │
 │     ┌──────────────┐         ┌──────────────┐    │
 │     │ Persistent Q │         │ Dead Letter Q │    │
 │     │              │         │               │    │
 │     │ ┌──────────┐ │         │  1.log.tmp    │    │
 │     │ │ Tail Page│ │         │  2.log        │    │
 │     │ │ (sealed) │ │         │  3.log        │    │
 │     │ ├──────────┤ │         └───────────────┘    │
 │     │ │ Tail Page│ │                              │
 │     │ │ (sealed) │ │                              │
 │     │ ├──────────┤ │                              │
 │     │ │ Head Page│ │                              │
 │     │ │ (active) │ │                              │
 │     │ └──────────┘ │                              │
 │     │  Checkpoint   │                              │
 │     │  Files (.cp)  │                              │
 │     └──────────────┘                              │
 └──────────────────────────────────────────────────┘
```

### Queue 클래스 - 쓰기 흐름

`Queue.write()` 메서드 (`Queue.java:422`)에서 이벤트 쓰기가 이루어진다.

```java
// Queue.java:422-507 (핵심 흐름 요약)
public long write(Queueable element) throws IOException {
    // 1. 직렬화 및 압축
    byte[] serializedBytes = element.serialize();
    byte[] data = compressionCodec.encode(serializedBytes);

    lock.lock();
    try {
        // 2. Head Page 용량 확인 → 부족하면 새 Head Page 생성
        if (!this.headPage.hasSpace(data.length)) {
            int newHeadPageNum = this.headPage.pageNum + 1;
            if (this.headPage.isFullyAcked()) {
                this.headPage.purge();  // 완전 ack된 페이지는 삭제
            } else {
                behead();  // tail로 전환
            }
            newCheckpointedHeadpage(newHeadPageNum);
        }

        // 3. 시퀀스 번호 할당 후 쓰기
        long seqNum = this.seqNum += 1;
        this.headPage.write(data, seqNum, this.checkpointMaxWrites);
        this.unreadCount++;

        // 4. 큐 full 상태면 notFull 조건 대기
        while (isFull() && !isClosed()) {
            notFull.await();
        }
        return seqNum;
    } finally {
        lock.unlock();
    }
}
```

### MmapPageIOV2 - Memory-Mapped I/O

각 페이지 파일은 `MmapPageIOV2` (`MmapPageIOV2.java:40`)를 통해 memory-mapped file로 관리된다.

```
 Page File Layout (MmapPageIOV2)
 ┌─────────────────────────────────────────────────┐
 │ Version (1 byte)                                │
 ├─────────────────────────────────────────────────┤
 │ Element 1:                                      │
 │   SeqNum (8 bytes) │ Length (4 bytes) │          │
 │   Data (variable)  │ CRC32 (4 bytes)            │
 ├─────────────────────────────────────────────────┤
 │ Element 2:                                      │
 │   SeqNum │ Length │ Data │ CRC32                 │
 ├─────────────────────────────────────────────────┤
 │ ...                                             │
 │                     ▲ head (write position)      │
 │                                                 │
 │ (unused capacity)                               │
 └─────────────────────────────────────────────────┘
```

각 엘리먼트는 `SeqNum(8B) + Length(4B) + Data(가변) + CRC32(4B)` 구조로 저장된다. CRC32 체크섬으로 데이터 무결성을 검증하며, `recover()` 메서드가 비정상 종료 후 유효한 엘리먼트만 복구한다.

```java
// MmapPageIOV2.java:188-198 - 페이지 생성
public void create() throws IOException {
    try (RandomAccessFile raf = new RandomAccessFile(this.file, "rw")) {
        this.buffer = raf.getChannel().map(
            FileChannel.MapMode.READ_WRITE, 0, this.capacity);
    }
    buffer.position(0);
    buffer.put(VERSION_TWO);
    buffer.force();  // fsync로 디스크에 강제 기록
}
```

### Checkpoint 메커니즘

체크포인트는 큐의 상태를 디스크에 기록하여 복구 시점을 제공한다.

- **Head Checkpoint**: head 페이지의 현재 상태 (minSeqNum, elementCount, firstUnackedSeqNum)
- **Tail Checkpoint**: 각 tail 페이지별 상태
- `checkpointMaxWrites`: N번 쓰기마다 체크포인트 수행
- `checkpointMaxAcks`: N번 ack마다 체크포인트 수행

큐 복구 시 (`Queue.openPages()`, `Queue.java:212`) 체크포인트 파일을 읽어 tail 페이지를 재구성하고, head 페이지는 `pageIO.recover()`로 유효한 데이터를 복원한다.

### Dead Letter Queue 구현

`DeadLetterQueueWriter` (`DeadLetterQueueWriter.java:109`)는 세그먼트 파일 기반으로 실패한 이벤트를 저장한다.

```
 DLQ Segment Management
 ┌──────────────────────────────────────────┐
 │ DLQ Writer                              │
 │                                         │
 │  writeEntry() ──▶ innerWriteEntry()     │
 │     │                │                  │
 │     │    ┌───────────▼──────────┐       │
 │     │    │ Age Retention Policy │       │
 │     │    │ (retentionTime)      │       │
 │     │    └───────────┬──────────┘       │
 │     │    ┌───────────▼──────────┐       │
 │     │    │ Storage Policy       │       │
 │     │    │ DROP_NEWER/OLDER     │       │
 │     │    └───────────┬──────────┘       │
 │     │                ▼                  │
 │     │    Segment: N.log.tmp (writing)   │
 │     │    → sealed → N.log              │
 │     │                                   │
 │  Flusher Thread (1s interval)           │
 │    - stale segment sealing              │
 │    - age policy enforcement             │
 │                                         │
 │  FS Watcher Thread (3s interval)        │
 │    - consumed segment cleanup notify    │
 └──────────────────────────────────────────┘
```

핵심 정책:
- **Age Retention**: `retentionTime` 설정 기반, 만료된 세그먼트 자동 삭제
- **Storage Policy**: `maxQueueSize` 초과 시 `DROP_NEWER`(새 이벤트 버림) 또는 `DROP_OLDER`(오래된 세그먼트 삭제) 적용
- **Segment Sealing**: `.log.tmp` → `.log` 파일로 atomic move하여 세그먼트 확정

```java
// DeadLetterQueueWriter.java:449-469 - 이벤트 쓰기 핵심 로직
private void innerWriteEntry(DLQEntry entry) throws IOException {
    if (alreadyProcessed(event)) { return; }  // 중복 방지
    byte[] record = entry.serialize();
    int eventPayloadSize = RECORD_HEADER_SIZE + record.length;

    executeAgeRetentionPolicy();       // 만료 세그먼트 정리
    boolean skipWrite = executeStoragePolicy(eventPayloadSize);  // 용량 정책
    if (skipWrite) { return; }

    if (exceedSegmentSize(eventPayloadSize)) {
        finalizeSegment(FinalizeWhen.ALWAYS, SealReason.SEGMENT_FULL);
    }
    long writtenBytes = currentWriter.writeEvent(record);
    currentQueueSize.getAndAdd(writtenBytes);
}
```

## 4. 실전 예제

### Persistent Queue 설정

```yaml
# logstash.yml - Persistent Queue 활성화
queue.type: persisted
queue.max_bytes: 4gb
queue.page_capacity: 64mb
queue.max_events: 0           # 0 = 무제한 (max_bytes로 제어)
queue.checkpoint.acks: 1024   # 1024 ack마다 체크포인트
queue.checkpoint.writes: 1024 # 1024 write마다 체크포인트
queue.drain: false            # shutdown 시 큐 비우기 여부
path.queue: /var/lib/logstash/queue
```

### Dead Letter Queue 설정 및 재처리 파이프라인

```yaml
# logstash.yml - DLQ 활성화
dead_letter_queue.enable: true
dead_letter_queue.max_bytes: 1024mb
dead_letter_queue.storage_policy: drop_newer
dead_letter_queue.retain.age: 7d
dead_letter_queue.flush_interval: 5000ms
path.dead_letter_queue: /var/lib/logstash/dead_letter_queue
```

```ruby
# dlq-reprocess.conf - DLQ 재처리 파이프라인
input {
  dead_letter_queue {
    path => "/var/lib/logstash/dead_letter_queue"
    pipeline_id => "main"
    commit_offsets => true
  }
}

filter {
  # DLQ 메타데이터에서 실패 원인 확인
  mutate {
    add_field => {
      "dlq_reason" => "%{[@metadata][dead_letter_queue][reason]}"
      "dlq_plugin" => "%{[@metadata][dead_letter_queue][plugin_id]}"
    }
  }

  # 매핑 오류 수정 (예: 타입 변환)
  if [dlq_reason] =~ /mapper_parsing_exception/ {
    mutate {
      convert => { "response_time" => "float" }
    }
  }
}

output {
  elasticsearch {
    hosts => ["http://es-node:9200"]
    index => "recovered-%{+YYYY.MM.dd}"
  }
}
```

## 5. 정리

| 항목 | 설명 |
|------|------|
| Memory Queue | JVM 힙 기반, 최고 성능, 프로세스 종료 시 데이터 유실 |
| Persistent Queue | MmapPageIOV2 기반 디스크 저장, 프로세스 복구 가능 |
| Page 구조 | Head Page(쓰기 활성) + Tail Pages(읽기 전용), 각 엘리먼트에 SeqNum/CRC32 포함 |
| Checkpoint | head/tail 체크포인트 파일로 큐 상태 기록, 복구 시점 보장 |
| Dead Letter Queue | 처리 실패 이벤트를 세그먼트 파일에 보관, Age/Size 보관 정책 적용 |
| 스레드 안전성 | ReentrantLock + Condition(notFull/notEmpty)으로 생산자-소비자 패턴 구현 |
| 핵심 소스 | `Queue.java`, `MmapPageIOV2.java`, `DeadLetterQueueWriter.java` |

---
*마지막 업데이트: 2026년 03월*
