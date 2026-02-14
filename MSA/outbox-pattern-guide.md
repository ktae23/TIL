# Outbox 패턴 가이드

마이크로서비스 환경에서 Dual Write 문제를 해결하는 Transactional Outbox 패턴의 개념, 구현 방법, CDC 연동, Saga 조합, 그리고 세무 도메인 적용 사례를 다룹니다.

## 목차
1. [Dual Write 문제](#dual-write-문제)
2. [Outbox 패턴 해결](#outbox-패턴-해결)
3. [Polling Publisher 구현](#polling-publisher-구현)
4. [CDC (Change Data Capture)](#cdc-change-data-capture)
5. [Saga + Outbox 조합](#saga--outbox-조합)
6. [세무 도메인 적용](#세무-도메인-적용)
7. [운영 고려사항](#운영-고려사항)
8. [핵심 정리](#핵심-정리)
9. [면접 대비 핵심 질문](#면접-대비-핵심-질문)

---

## Dual Write 문제

### Dual Write란 무엇인가

Dual Write는 하나의 비즈니스 작업에서 두 개 이상의 독립적인 시스템(DB와 메시지 브로커)에 동시에 쓰기를 수행하는 것입니다. 두 쓰기 작업은 원자적으로(atomically) 수행할 수 없으므로 데이터 불일치가 발생할 수 있습니다.

```
┌──────────────────────────────────────────────────────────────────┐
│                     Dual Write 문제 상황                          │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  기장 서비스에서 거래내역 등록 시:                                  │
│                                                                   │
│  @Transactional                                                   │
│  public void registerTransaction(TransactionRequest req) {        │
│      // 1. DB에 거래내역 저장                                     │
│      transactionRepository.save(transaction);  ← DB 쓰기          │
│                                                                   │
│      // 2. 세금계산 서비스에 이벤트 발행                            │
│      kafkaTemplate.send("tax-events", event);  ← Kafka 쓰기      │
│  }                                                                │
│                                                                   │
│  ❌ 시나리오 1: DB 성공 → Kafka 실패                               │
│  ┌──────┐        ┌──────┐        ┌──────┐                        │
│  │ App  │──✓──►│  DB  │  ✓    │Kafka │  ✗                      │
│  └──────┘        └──────┘        └──────┘                        │
│  → DB에는 저장됨, 세금계산 이벤트 유실                              │
│  → 세금 미계산, 데이터 불일치                                      │
│                                                                   │
│  ❌ 시나리오 2: Kafka 성공 → DB 롤백                               │
│  ┌──────┐        ┌──────┐        ┌──────┐                        │
│  │ App  │──✗──►│  DB  │  ✗    │Kafka │  ✓                      │
│  └──────┘        └──────┘        └──────┘                        │
│  → DB 롤백됨, 세금계산 이벤트는 발행됨                              │
│  → 존재하지 않는 거래내역에 대해 세금 계산 시도                     │
│                                                                   │
│  ❌ 시나리오 3: App 크래시                                         │
│  ┌──────┐        ┌──────┐        ┌──────┐                        │
│  │ App☠│──?──►│  DB  │  ?    │Kafka │  ?                      │
│  └──────┘        └──────┘        └──────┘                        │
│  → DB 커밋 후 Kafka 발행 전 크래시                                 │
│  → 이벤트 유실                                                    │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 왜 2PC로 해결할 수 없는가

```
┌──────────────────────────────────────────────────────────────────┐
│            2PC (Two-Phase Commit) 의 한계                         │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  이론적 해결:                                                      │
│  DB와 Kafka를 하나의 분산 트랜잭션으로 묶기                       │
│                                                                   │
│  실제 한계:                                                       │
│  1. Kafka는 XA 트랜잭션 미지원                                    │
│  2. 2PC는 코디네이터 단일 장애점 (SPOF)                           │
│  3. 모든 참여자가 락을 유지 → 성능 저하                            │
│  4. 네트워크 파티션에 취약                                        │
│  5. MSA 환경에서 서비스 간 2PC는 anti-pattern                     │
│                                                                   │
│  결론: Outbox 패턴으로 "최종 일관성" 접근                         │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

---

## Outbox 패턴 해결

### Transactional Outbox 패턴 개념

Outbox 패턴의 핵심 아이디어는 **메시지 발행을 DB 트랜잭션 안으로 가져오는 것**입니다. 메시지를 직접 Kafka로 보내는 대신, 동일 DB의 outbox 테이블에 저장합니다. 그 후 별도 프로세스가 outbox 테이블을 읽어 메시지 브로커로 발행합니다.

```
┌──────────────────────────────────────────────────────────────────┐
│                Transactional Outbox 패턴 아키텍처                  │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌─────────────────────────────────────────────────┐             │
│  │            하나의 DB 트랜잭션                     │             │
│  │                                                  │             │
│  │  1. 비즈니스 데이터 저장                          │             │
│  │     INSERT INTO transactions (...)               │             │
│  │                                                  │             │
│  │  2. Outbox 이벤트 저장                           │             │
│  │     INSERT INTO outbox_events (...)              │             │
│  │                                                  │             │
│  │  → COMMIT (원자적 저장 보장)                      │             │
│  └───────────────────────┬─────────────────────────┘             │
│                          │                                        │
│                          ▼                                        │
│  ┌───────────────────────────────────────────────┐               │
│  │           Outbox Publisher (별도 프로세스)       │               │
│  │                                                │               │
│  │   방법 1: Polling Publisher (@Scheduled)        │               │
│  │   - 주기적으로 outbox 테이블 조회               │               │
│  │   - 미발행 이벤트를 Kafka로 발행               │               │
│  │   - 발행 완료 마킹                              │               │
│  │                                                │               │
│  │   방법 2: CDC (Change Data Capture)            │               │
│  │   - DB 변경 로그를 실시간 감지                  │               │
│  │   - Debezium + Kafka Connect                   │               │
│  │   - outbox 테이블 INSERT 감지 → Kafka 발행     │               │
│  └───────────────────────┬───────────────────────┘               │
│                          │                                        │
│                          ▼                                        │
│                    ┌───────────┐                                  │
│                    │   Kafka   │                                  │
│                    └─────┬─────┘                                  │
│                          │                                        │
│                          ▼                                        │
│                  ┌──────────────┐                                 │
│                  │ Consumer     │                                 │
│                  │ (세금계산 등) │                                 │
│                  └──────────────┘                                 │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Outbox 테이블 설계

```sql
-- Outbox 이벤트 테이블
CREATE TABLE outbox_events (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_type  VARCHAR(100)  NOT NULL,     -- 도메인 집합체 타입 (e.g., 'Transaction', 'TaxReturn')
    aggregate_id    VARCHAR(100)  NOT NULL,     -- 집합체 ID (e.g., 거래내역 ID)
    event_type      VARCHAR(100)  NOT NULL,     -- 이벤트 타입 (e.g., 'TransactionCreated')
    payload         JSON          NOT NULL,     -- 이벤트 데이터 (JSON)
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',  -- PENDING, PUBLISHED, FAILED
    created_at      TIMESTAMP(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    published_at    TIMESTAMP(3)  NULL,
    retry_count     INT           NOT NULL DEFAULT 0,
    last_error      TEXT          NULL,

    -- 인덱스
    INDEX idx_outbox_status_created (status, created_at),
    INDEX idx_outbox_aggregate (aggregate_type, aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 동작 흐름 상세

```
┌──────────────────────────────────────────────────────────────────┐
│                    Outbox 패턴 동작 흐름                           │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Phase 1: 비즈니스 로직 + Outbox 저장 (동일 트랜잭션)             │
│  ─────────────────────────────────────────────────────            │
│                                                                   │
│  [Request] ──► [Service] ──┬──► [Business Table] ──► COMMIT     │
│                            │                                      │
│                            └──► [Outbox Table]   ──► COMMIT     │
│                                                                   │
│  → DB 트랜잭션 하나로 원자적 저장 보장                             │
│  → 비즈니스 데이터와 이벤트가 항상 함께 저장되거나 함께 롤백       │
│                                                                   │
│  Phase 2: 이벤트 발행 (비동기)                                    │
│  ─────────────────────────────────────────────────────            │
│                                                                   │
│  [Outbox Table] ──► [Publisher] ──► [Kafka] ──► [Consumer]       │
│       │                                             │             │
│       └── status: PENDING → PUBLISHED               │             │
│                                                     │             │
│  → 발행 실패 시 재시도 (retry_count 증가)            │             │
│  → Consumer는 멱등성 보장 필수                       │             │
│                                                                   │
│  Phase 3: 클린업 (주기적)                                         │
│  ─────────────────────────────────────────────────────            │
│                                                                   │
│  [Outbox Table] ──► 오래된 PUBLISHED 이벤트 삭제                  │
│                 ──► FAILED 이벤트 알림 후 처리                    │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

---

## Polling Publisher 구현

### Outbox Entity 및 Repository

```java
@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String aggregateType;

    @Column(nullable = false, length = 100)
    private String aggregateId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, columnDefinition = "JSON")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private int retryCount = 0;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    public static OutboxEvent create(String aggregateType,
                                      String aggregateId,
                                      String eventType,
                                      Object payload) {
        OutboxEvent event = new OutboxEvent();
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.eventType = eventType;
        event.payload = JsonUtil.toJson(payload);
        event.createdAt = LocalDateTime.now();
        return event;
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void markFailed(String error) {
        this.retryCount++;
        this.lastError = error;
        if (this.retryCount >= 5) {
            this.status = OutboxStatus.FAILED;
        }
    }
}

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
```

```java
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("""
        SELECT e FROM OutboxEvent e
        WHERE e.status = 'PENDING'
        ORDER BY e.createdAt ASC
        LIMIT :batchSize
        """)
    List<OutboxEvent> findPendingEvents(@Param("batchSize") int batchSize);

    @Modifying
    @Query("""
        DELETE FROM OutboxEvent e
        WHERE e.status = 'PUBLISHED'
        AND e.publishedAt < :cutoff
        """)
    int deletePublishedBefore(@Param("cutoff") LocalDateTime cutoff);

    List<OutboxEvent> findByStatusAndRetryCountLessThan(
        OutboxStatus status, int maxRetries);
}
```

### Polling Publisher 서비스

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPollingPublisher {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final int BATCH_SIZE = 100;

    /**
     * Outbox 폴링 - 미발행 이벤트를 Kafka로 발행
     * - 5초마다 실행
     * - ShedLock으로 다중 인스턴스 중복 방지
     */
    @Scheduled(fixedDelay = 5000)
    @SchedulerLock(
        name = "outboxPolling",
        lockAtMostFor = "PT30S",
        lockAtLeastFor = "PT4S"
    )
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository
            .findPendingEvents(BATCH_SIZE);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Outbox 이벤트 발행 시작: {}건", pendingEvents.size());

        int successCount = 0;
        for (OutboxEvent event : pendingEvents) {
            try {
                publishEvent(event);
                event.markPublished();
                successCount++;
            } catch (Exception e) {
                log.error("Outbox 이벤트 발행 실패: id={}, type={}",
                    event.getId(), event.getEventType(), e);
                event.markFailed(e.getMessage());
            }
        }

        outboxRepository.saveAll(pendingEvents);
        log.info("Outbox 이벤트 발행 완료: 성공={}/{}",
            successCount, pendingEvents.size());
    }

    private void publishEvent(OutboxEvent event) {
        String topic = resolveTopicName(event.getAggregateType());

        ProducerRecord<String, String> record = new ProducerRecord<>(
            topic,
            event.getAggregateId(),  // key (파티셔닝)
            event.getPayload()       // value
        );

        // 헤더에 메타데이터 추가
        record.headers()
            .add("eventType", event.getEventType().getBytes())
            .add("eventId", String.valueOf(event.getId()).getBytes())
            .add("aggregateType", event.getAggregateType().getBytes());

        // 동기 발행 (발행 확인)
        kafkaTemplate.send(record).get(5, TimeUnit.SECONDS);
    }

    private String resolveTopicName(String aggregateType) {
        return switch (aggregateType) {
            case "Transaction" -> "transaction-events";
            case "TaxReturn" -> "tax-return-events";
            case "Bookkeeping" -> "bookkeeping-events";
            default -> "domain-events";
        };
    }
}
```

### 비즈니스 서비스에서 Outbox 사용

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final OutboxEventRepository outboxRepository;

    /**
     * 거래내역 등록
     * - 거래내역 저장과 이벤트 발행이 하나의 트랜잭션
     * - Dual Write 문제 해결
     */
    @Transactional
    public Transaction registerTransaction(TransactionRequest request) {
        // 1. 비즈니스 데이터 저장
        Transaction transaction = Transaction.create(
            request.getBusinessNumber(),
            request.getAmount(),
            request.getType(),
            request.getDescription()
        );
        transactionRepository.save(transaction);

        // 2. Outbox 이벤트 저장 (동일 트랜잭션)
        TransactionCreatedEvent event = new TransactionCreatedEvent(
            transaction.getId(),
            transaction.getBusinessNumber(),
            transaction.getAmount(),
            transaction.getType(),
            transaction.getTransactionDate()
        );

        OutboxEvent outboxEvent = OutboxEvent.create(
            "Transaction",
            String.valueOf(transaction.getId()),
            "TransactionCreated",
            event
        );
        outboxRepository.save(outboxEvent);

        log.info("거래내역 등록 완료: id={}, outboxId={}",
            transaction.getId(), outboxEvent.getId());

        return transaction;
    }
}
```

### Spring ApplicationEvent를 활용한 Outbox 추상화

비즈니스 코드에서 Outbox 직접 참조를 제거하여 관심사를 분리할 수 있습니다.

```java
// 도메인 이벤트 인터페이스
public interface DomainEvent {
    String getAggregateType();
    String getAggregateId();
    String getEventType();
}

// 도메인 이벤트 리스너 → Outbox 저장
@Component
@RequiredArgsConstructor
public class OutboxDomainEventListener {

    private final OutboxEventRepository outboxRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleDomainEvent(DomainEvent event) {
        OutboxEvent outboxEvent = OutboxEvent.create(
            event.getAggregateType(),
            event.getAggregateId(),
            event.getEventType(),
            event
        );
        outboxRepository.save(outboxEvent);
    }
}

// 비즈니스 서비스 (Outbox 직접 참조 없음)
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Transaction registerTransaction(TransactionRequest request) {
        Transaction transaction = Transaction.create(request);
        transactionRepository.save(transaction);

        // Spring Event 발행 → OutboxDomainEventListener가 처리
        eventPublisher.publishEvent(
            new TransactionCreatedEvent(transaction));

        return transaction;
    }
}
```

---

## CDC (Change Data Capture)

### CDC 개념과 Outbox 연동

CDC는 데이터베이스의 변경 사항(INSERT, UPDATE, DELETE)을 실시간으로 감지하여 다른 시스템으로 전달하는 기술입니다. Outbox 패턴과 결합하면 Polling 방식의 지연 문제를 해결할 수 있습니다.

```
┌──────────────────────────────────────────────────────────────────┐
│                    CDC + Outbox 아키텍처                           │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌────────────┐                                                  │
│  │ Application │                                                 │
│  │             │──── INSERT into outbox_events                   │
│  └──────┬─────┘                                                  │
│         │                                                         │
│         ▼                                                         │
│  ┌──────────────┐      binlog/WAL      ┌────────────────┐       │
│  │   MySQL DB    │ ─────────────────► │   Debezium      │       │
│  │              │   (변경 로그 감지)    │   Connector     │       │
│  │ outbox_events│                      └───────┬────────┘       │
│  └──────────────┘                              │                 │
│                                                │                 │
│                                    ┌───────────▼──────────┐     │
│                                    │   Kafka Connect       │     │
│                                    │   (SMT: Outbox Event  │     │
│                                    │    Router)             │     │
│                                    └───────────┬──────────┘     │
│                                                │                 │
│                                                ▼                 │
│                                    ┌────────────────────┐       │
│                                    │   Kafka Topic       │       │
│                                    │   (도메인별 토픽)    │       │
│                                    └────────────────────┘       │
│                                                                   │
│  Polling 대비 장점:                                               │
│  - 실시간 이벤트 감지 (밀리초 단위)                               │
│  - DB 폴링 부하 없음                                             │
│  - 이벤트 순서 보장 (binlog 순서)                                 │
│  - 삭제된 이벤트도 감지 가능                                      │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Debezium 소개 및 설정

Debezium은 CDC를 위한 오픈소스 분산 플랫폼으로, Kafka Connect 커넥터로 동작합니다.

```json
{
  "name": "outbox-connector",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "tasks.max": "1",

    "database.hostname": "mysql-primary",
    "database.port": "3306",
    "database.user": "debezium",
    "database.password": "${DEBEZIUM_DB_PASSWORD}",
    "database.server.id": "184054",

    "database.include.list": "bookkeeping_service",
    "table.include.list": "bookkeeping_service.outbox_events",

    "topic.prefix": "cdc",
    "schema.history.internal.kafka.bootstrap.servers": "kafka:9092",
    "schema.history.internal.kafka.topic": "schema-changes.bookkeeping",

    "transforms": "outbox",
    "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
    "transforms.outbox.table.field.event.id": "id",
    "transforms.outbox.table.field.event.key": "aggregate_id",
    "transforms.outbox.table.field.event.type": "event_type",
    "transforms.outbox.table.field.event.payload": "payload",
    "transforms.outbox.route.by.field": "aggregate_type",
    "transforms.outbox.route.topic.replacement": "${routedByValue}-events",

    "tombstones.on.delete": "false"
  }
}
```

### Debezium Outbox Event Router

Debezium의 Outbox Event Router SMT(Single Message Transform)는 outbox 테이블의 레코드를 도메인 이벤트로 변환합니다.

```
┌──────────────────────────────────────────────────────────────────┐
│              Debezium Outbox Event Router 동작                    │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  outbox_events 테이블 레코드:                                     │
│  ┌─────────────────────────────────────────────────────────┐     │
│  │ id: 1                                                    │     │
│  │ aggregate_type: "Transaction"                           │     │
│  │ aggregate_id: "txn-12345"                               │     │
│  │ event_type: "TransactionCreated"                        │     │
│  │ payload: {"id": "txn-12345", "amount": 100000, ...}     │     │
│  └─────────────────────────────────────────────────────────┘     │
│                          │                                        │
│                  Event Router SMT                                 │
│                          │                                        │
│                          ▼                                        │
│  Kafka 메시지:                                                    │
│  ┌─────────────────────────────────────────────────────────┐     │
│  │ Topic: transaction-events                                │     │
│  │ Key: "txn-12345"                                        │     │
│  │ Value: {"id": "txn-12345", "amount": 100000, ...}       │     │
│  │ Headers:                                                 │     │
│  │   eventType: "TransactionCreated"                       │     │
│  │   eventId: "1"                                          │     │
│  └─────────────────────────────────────────────────────────┘     │
│                                                                   │
│  → aggregate_type으로 토픽 라우팅                                  │
│  → aggregate_id를 메시지 키로 사용 (파티셔닝)                     │
│  → payload를 메시지 본문으로 전달                                  │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Polling vs CDC 비교

| 비교 항목 | Polling Publisher | CDC (Debezium) |
|-----------|------------------|----------------|
| **지연 시간** | 폴링 주기에 의존 (초~분) | 실시간 (밀리초) |
| **DB 부하** | 주기적 SELECT 쿼리 | 없음 (binlog 기반) |
| **인프라 복잡도** | 낮음 (추가 인프라 불필요) | 높음 (Kafka Connect + Debezium) |
| **순서 보장** | 어려움 (동시 폴링 시) | 보장 (binlog 순서) |
| **운영 난이도** | 낮음 | 높음 (Debezium 모니터링 필요) |
| **장애 복구** | 단순 (재시작 시 재폴링) | 복잡 (offset 관리) |
| **확장성** | 제한적 (DB 폴링 병목) | 높음 (Kafka Connect 확장) |
| **적합 케이스** | 소규모, 낮은 지연 허용 | 대규모, 실시간 이벤트 필수 |

```
선택 가이드:
─────────────────────────────────────────
Polling Publisher가 적합한 경우:
  - 초기 MVP, 작은 팀
  - 이벤트 볼륨이 낮음 (분당 수십 건)
  - 수초의 지연이 허용됨
  - 인프라를 단순하게 유지하고 싶을 때

CDC (Debezium)가 적합한 경우:
  - 대규모 트래픽 (분당 수천 건 이상)
  - 실시간 이벤트 처리 필수
  - 이미 Kafka Connect 인프라 보유
  - 이벤트 순서 보장이 중요
```

---

## Saga + Outbox 조합

### 왜 Saga와 Outbox를 함께 사용하는가

Saga 패턴에서 각 서비스 간 메시지 전달은 Dual Write 문제를 동일하게 갖습니다. Outbox 패턴을 결합하면 Saga의 각 단계에서 안정적인 메시지 전달이 보장됩니다.

```
┌──────────────────────────────────────────────────────────────────┐
│              Saga without Outbox (문제 발생 가능)                  │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Orchestrator:                                                    │
│  1. Saga 상태 UPDATE → DB COMMIT ✓                               │
│  2. 다음 서비스에 Command 발행 → Kafka SEND ✗ (실패!)            │
│  → Saga가 멈춤 (stuck state)                                      │
│                                                                   │
│  각 서비스:                                                       │
│  1. 비즈니스 로직 처리 → DB COMMIT ✓                              │
│  2. 결과 이벤트 발행 → Kafka SEND ✗ (실패!)                      │
│  → Orchestrator에 응답 전달 불가                                   │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│              Saga + Outbox (안정적 메시지 전달)                    │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Orchestrator:                                                    │
│  1. Saga 상태 UPDATE + Outbox INSERT → 단일 DB COMMIT ✓          │
│  2. Outbox Publisher가 Command를 Kafka로 발행 (재시도 가능)       │
│  → Saga 상태와 Command 발행이 항상 일관적                         │
│                                                                   │
│  각 서비스:                                                       │
│  1. 비즈니스 로직 + Outbox INSERT → 단일 DB COMMIT ✓             │
│  2. Outbox Publisher가 이벤트를 Kafka로 발행 (재시도 가능)        │
│  → 비즈니스 처리와 응답 이벤트가 항상 일관적                      │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Orchestration Saga + Outbox 아키텍처

```
┌──────────────────────────────────────────────────────────────────┐
│           Orchestration Saga + Outbox 전체 흐름                   │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                  기장 서비스 (Orchestrator)                │   │
│  │                                                           │   │
│  │  ┌─── DB Transaction ───────────────────────────┐        │   │
│  │  │ 1. saga_state UPDATE (STARTED → TAX_PENDING) │        │   │
│  │  │ 2. outbox INSERT (CalculateTaxCommand)       │        │   │
│  │  │ → COMMIT                                      │        │   │
│  │  └───────────────────────────────────────────────┘        │   │
│  │              │                                             │   │
│  │     Outbox Publisher                                      │   │
│  │              │                                             │   │
│  └──────────────┼────────────────────────────────────────────┘   │
│                 │                                                 │
│                 ▼                                                 │
│          ┌───────────┐                                           │
│          │   Kafka   │                                           │
│          └─────┬─────┘                                           │
│                │                                                  │
│                ▼                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    세금계산 서비스                          │   │
│  │                                                           │   │
│  │  ┌─── DB Transaction ───────────────────────────┐        │   │
│  │  │ 1. 세금 계산 처리                             │        │   │
│  │  │ 2. outbox INSERT (TaxCalculatedEvent)        │        │   │
│  │  │ → COMMIT                                      │        │   │
│  │  └───────────────────────────────────────────────┘        │   │
│  │              │                                             │   │
│  │     Outbox Publisher                                      │   │
│  │              │                                             │   │
│  └──────────────┼────────────────────────────────────────────┘   │
│                 │                                                 │
│                 ▼                                                 │
│          ┌───────────┐                                           │
│          │   Kafka   │ ──► 기장 서비스 (Orchestrator)            │
│          └───────────┘     → 다음 Saga 단계 진행                 │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Saga + Outbox 코드 예제

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class BookkeepingSagaOrchestrator {

    private final SagaStateRepository sagaRepository;
    private final OutboxEventRepository outboxRepository;

    /**
     * 기장 등록 Saga 시작
     * - Saga 상태와 Command를 하나의 트랜잭션으로 저장
     */
    @Transactional
    public String startBookkeepingSaga(BookkeepingRequest request) {
        String sagaId = UUID.randomUUID().toString();

        // 1. Saga 상태 저장
        SagaState saga = SagaState.create(
            sagaId,
            "BookkeepingSaga",
            SagaStatus.STARTED,
            JsonUtil.toJson(request)
        );
        sagaRepository.save(saga);

        // 2. 세금계산 Command를 Outbox에 저장 (동일 트랜잭션)
        CalculateTaxCommand command = new CalculateTaxCommand(
            sagaId,
            request.getBusinessNumber(),
            request.getTransactions()
        );

        OutboxEvent outbox = OutboxEvent.create(
            "BookkeepingSaga",
            sagaId,
            "CalculateTaxCommand",
            command
        );
        outboxRepository.save(outbox);

        log.info("기장 Saga 시작: sagaId={}", sagaId);
        return sagaId;
    }

    /**
     * 세금계산 완료 이벤트 처리
     * - 다음 단계(신고서 생성)로 진행
     */
    @Transactional
    public void handleTaxCalculated(TaxCalculatedEvent event) {
        SagaState saga = sagaRepository.findById(event.getSagaId())
            .orElseThrow();

        // 1. Saga 상태 업데이트
        saga.updateStatus(SagaStatus.TAX_CALCULATED);
        sagaRepository.save(saga);

        // 2. 다음 Command를 Outbox에 저장
        GenerateTaxReturnCommand command = new GenerateTaxReturnCommand(
            saga.getSagaId(),
            event.getTaxCalculationId()
        );

        OutboxEvent outbox = OutboxEvent.create(
            "BookkeepingSaga",
            saga.getSagaId(),
            "GenerateTaxReturnCommand",
            command
        );
        outboxRepository.save(outbox);
    }

    /**
     * 실패 시 보상 트랜잭션 (Outbox 경유)
     */
    @Transactional
    public void handleTaxCalculationFailed(TaxCalculationFailedEvent event) {
        SagaState saga = sagaRepository.findById(event.getSagaId())
            .orElseThrow();

        // 1. Saga 상태를 COMPENSATING으로 변경
        saga.updateStatus(SagaStatus.COMPENSATING);
        saga.setFailureReason(event.getReason());
        sagaRepository.save(saga);

        // 2. 보상 Command를 Outbox에 저장
        CancelBookkeepingCommand command = new CancelBookkeepingCommand(
            saga.getSagaId(),
            saga.getAggregateId()
        );

        OutboxEvent outbox = OutboxEvent.create(
            "BookkeepingSaga",
            saga.getSagaId(),
            "CancelBookkeepingCommand",
            command
        );
        outboxRepository.save(outbox);
    }
}
```

---

## 세무 도메인 적용

### 기장 등록 → 세금계산 이벤트 발행

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class BookkeepingService {

    private final BookkeepingRepository bookkeepingRepository;
    private final OutboxEventRepository outboxRepository;

    @Transactional
    public Bookkeeping registerBookkeeping(BookkeepingRequest request) {
        // 1. 기장 데이터 저장
        Bookkeeping bookkeeping = Bookkeeping.create(
            request.getBusinessNumber(),
            request.getTaxType(),
            request.getPeriod(),
            request.getTransactions()
        );
        bookkeepingRepository.save(bookkeeping);

        // 2. 세금계산 이벤트를 Outbox에 저장
        BookkeepingRegisteredEvent event = new BookkeepingRegisteredEvent(
            bookkeeping.getId(),
            bookkeeping.getBusinessNumber(),
            bookkeeping.getTaxType(),
            bookkeeping.getPeriod(),
            bookkeeping.getTotalAmount()
        );

        outboxRepository.save(OutboxEvent.create(
            "Bookkeeping",
            String.valueOf(bookkeeping.getId()),
            "BookkeepingRegistered",
            event
        ));

        return bookkeeping;
    }
}
```

### 신고서 제출 → 환급 처리 이벤트

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TaxReturnService {

    private final TaxReturnRepository taxReturnRepository;
    private final OutboxEventRepository outboxRepository;

    @Transactional
    public TaxReturn submitTaxReturn(TaxReturnSubmitRequest request) {
        TaxReturn taxReturn = taxReturnRepository.findById(request.getId())
            .orElseThrow();

        // 1. 신고서 상태 변경
        taxReturn.submit();
        taxReturnRepository.save(taxReturn);

        // 2. 환급 처리 이벤트 발행 (환급 대상인 경우)
        if (taxReturn.getRefundAmount() > 0) {
            TaxReturnSubmittedEvent event = new TaxReturnSubmittedEvent(
                taxReturn.getId(),
                taxReturn.getBusinessNumber(),
                taxReturn.getTaxType(),
                taxReturn.getRefundAmount(),
                taxReturn.getSubmittedAt()
            );

            outboxRepository.save(OutboxEvent.create(
                "TaxReturn",
                String.valueOf(taxReturn.getId()),
                "TaxReturnSubmitted",
                event
            ));
        }

        // 3. 세무사 알림 이벤트
        outboxRepository.save(OutboxEvent.create(
            "TaxReturn",
            String.valueOf(taxReturn.getId()),
            "TaxReturnNotification",
            new TaxReturnNotificationEvent(
                taxReturn.getId(),
                taxReturn.getAssignedAccountantId(),
                "신고서 제출 완료"
            )
        ));

        return taxReturn;
    }
}
```

### 거래내역 변경 → 장부 재계산 이벤트

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionModificationService {

    private final TransactionRepository transactionRepository;
    private final OutboxEventRepository outboxRepository;

    @Transactional
    public Transaction modifyTransaction(Long transactionId,
                                          TransactionModifyRequest request) {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow();

        BigDecimal previousAmount = transaction.getAmount();

        // 1. 거래내역 수정
        transaction.modify(
            request.getAmount(),
            request.getType(),
            request.getDescription()
        );
        transactionRepository.save(transaction);

        // 2. 장부 재계산 이벤트
        TransactionModifiedEvent event = new TransactionModifiedEvent(
            transaction.getId(),
            transaction.getBusinessNumber(),
            previousAmount,
            transaction.getAmount(),
            transaction.getBookkeepingPeriod()
        );

        outboxRepository.save(OutboxEvent.create(
            "Transaction",
            String.valueOf(transaction.getId()),
            "TransactionModified",
            event
        ));

        // 3. 영향받는 세금 신고서 재계산 이벤트
        if (!previousAmount.equals(transaction.getAmount())) {
            outboxRepository.save(OutboxEvent.create(
                "Transaction",
                String.valueOf(transaction.getId()),
                "TaxRecalculationRequired",
                new TaxRecalculationEvent(
                    transaction.getBusinessNumber(),
                    transaction.getBookkeepingPeriod(),
                    "거래내역 금액 변경"
                )
            ));
        }

        return transaction;
    }
}
```

### 이벤트 스키마 설계

```java
// 이벤트 스키마 공통 필드
public abstract class BaseDomainEvent implements DomainEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime occurredAt;
    private int version;

    protected BaseDomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
        this.version = 1;
    }
}

// 기장 등록 이벤트
@Getter
public class BookkeepingRegisteredEvent extends BaseDomainEvent {
    private Long bookkeepingId;
    private String businessNumber;
    private TaxType taxType;         // VAT, INCOME_TAX, CORPORATE_TAX
    private String period;           // "2025-Q1", "2025-01"
    private BigDecimal totalAmount;

    @Override
    public String getAggregateType() { return "Bookkeeping"; }

    @Override
    public String getAggregateId() {
        return String.valueOf(bookkeepingId);
    }

    @Override
    public String getEventType() { return "BookkeepingRegistered"; }
}

// 신고서 제출 이벤트
@Getter
public class TaxReturnSubmittedEvent extends BaseDomainEvent {
    private Long taxReturnId;
    private String businessNumber;
    private TaxType taxType;
    private BigDecimal refundAmount;
    private LocalDateTime submittedAt;

    @Override
    public String getAggregateType() { return "TaxReturn"; }

    @Override
    public String getAggregateId() {
        return String.valueOf(taxReturnId);
    }

    @Override
    public String getEventType() { return "TaxReturnSubmitted"; }
}

// 거래내역 변경 이벤트
@Getter
public class TransactionModifiedEvent extends BaseDomainEvent {
    private Long transactionId;
    private String businessNumber;
    private BigDecimal previousAmount;
    private BigDecimal newAmount;
    private String bookkeepingPeriod;

    @Override
    public String getAggregateType() { return "Transaction"; }

    @Override
    public String getAggregateId() {
        return String.valueOf(transactionId);
    }

    @Override
    public String getEventType() { return "TransactionModified"; }
}
```

```
세무 도메인 이벤트 흐름:
──────────────────────────────────────────────────────────────

거래내역 등록  ──►  BookkeepingRegistered
                      │
                      ├──► 세금계산 서비스: 세금 계산
                      └──► 장부 서비스: 장부 갱신

신고서 제출    ──►  TaxReturnSubmitted
                      │
                      ├──► 환급 서비스: 환급 신청
                      ├──► 알림 서비스: 세무사 알림
                      └──► 이력 서비스: 신고 이력 기록

거래내역 수정  ──►  TransactionModified
                      │
                      ├──► 장부 서비스: 장부 재계산
                      └──► 세금계산 서비스: 세금 재계산
```

---

## 운영 고려사항

### Outbox 테이블 관리 (Retention, Cleanup)

Outbox 테이블은 이벤트가 계속 쌓이므로 주기적인 정리가 필수입니다.

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxCleanupService {

    private final OutboxEventRepository outboxRepository;

    /**
     * 발행 완료된 이벤트 삭제
     * - 매일 새벽 4시 실행
     * - 7일 이상 지난 PUBLISHED 이벤트 삭제
     */
    @Scheduled(cron = "0 0 4 * * *")
    @SchedulerLock(
        name = "outboxCleanup",
        lockAtMostFor = "PT30M",
        lockAtLeastFor = "PT5M"
    )
    @Transactional
    public void cleanupPublishedEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        int deleted = outboxRepository.deletePublishedBefore(cutoff);
        log.info("Outbox 클린업 완료: {}건 삭제 (기준: {})", deleted, cutoff);
    }

    /**
     * FAILED 이벤트 리포트
     * - 매 시간 실행
     * - 실패한 이벤트 목록을 슬랙으로 알림
     */
    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(
        name = "outboxFailedReport",
        lockAtMostFor = "PT5M",
        lockAtLeastFor = "PT1M"
    )
    public void reportFailedEvents() {
        List<OutboxEvent> failedEvents = outboxRepository
            .findByStatusAndRetryCountLessThan(
                OutboxStatus.FAILED, Integer.MAX_VALUE);

        if (!failedEvents.isEmpty()) {
            log.warn("실패한 Outbox 이벤트 {}건 발견", failedEvents.size());
            alertService.sendSlackAlert(
                formatFailedEventsReport(failedEvents));
        }
    }
}
```

### 멱등성 보장 (Idempotent Consumer)

Outbox 패턴에서는 이벤트가 최소 한 번(at-least-once) 전달되므로, 중복 메시지를 처리할 수 있어야 합니다.

```java
@Entity
@Table(name = "processed_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEvent {

    @Id
    private String eventId;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    public ProcessedEvent(String eventId) {
        this.eventId = eventId;
        this.processedAt = LocalDateTime.now();
    }
}

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotentConsumer {

    private final ProcessedEventRepository processedEventRepository;

    /**
     * 멱등성 보장 메시지 처리
     * - eventId로 중복 여부 확인
     * - 동일 트랜잭션으로 처리 완료 기록
     */
    @Transactional
    public <T> void processIdempotently(String eventId,
                                         Supplier<T> handler) {
        // 이미 처리된 이벤트인지 확인
        if (processedEventRepository.existsById(eventId)) {
            log.info("이벤트 중복 수신 무시: eventId={}", eventId);
            return;
        }

        // 비즈니스 로직 실행
        handler.get();

        // 처리 완료 기록 (동일 트랜잭션)
        processedEventRepository.save(new ProcessedEvent(eventId));
    }
}

// Consumer에서 사용
@Service
@RequiredArgsConstructor
public class TaxCalculationConsumer {

    private final IdempotentConsumer idempotentConsumer;
    private final TaxCalculationService taxService;

    @KafkaListener(topics = "bookkeeping-events",
                   groupId = "tax-calculation")
    public void handleBookkeepingEvent(
            @Payload String payload,
            @Header("eventId") String eventId,
            @Header("eventType") String eventType) {

        if ("BookkeepingRegistered".equals(eventType)) {
            idempotentConsumer.processIdempotently(eventId, () -> {
                BookkeepingRegisteredEvent event = JsonUtil
                    .fromJson(payload, BookkeepingRegisteredEvent.class);
                taxService.calculateTax(event);
                return null;
            });
        }
    }
}
```

### 순서 보장

```
┌──────────────────────────────────────────────────────────────────┐
│                      이벤트 순서 보장 전략                         │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  1. Kafka 파티셔닝으로 순서 보장                                   │
│  ────────────────────────────────────────────                     │
│  - aggregate_id를 Kafka 메시지 키로 사용                          │
│  - 동일 aggregate의 이벤트는 동일 파티션으로 전달                  │
│  - 파티션 내에서는 순서 보장됨                                     │
│                                                                   │
│  Partition 0: [Txn-1 Created] → [Txn-1 Modified] → [Txn-1 Deleted]
│  Partition 1: [Txn-2 Created] → [Txn-2 Modified]                 │
│  Partition 2: [Txn-3 Created]                                     │
│                                                                   │
│  2. Outbox 테이블에서 순서 보장                                    │
│  ────────────────────────────────────────────                     │
│  - Auto Increment ID로 삽입 순서 보장                             │
│  - Polling 시 ORDER BY id ASC로 조회                              │
│  - 동일 aggregate 이벤트를 순차 발행                               │
│                                                                   │
│  3. 주의사항                                                       │
│  ────────────────────────────────────────────                     │
│  - Consumer 병렬 처리 시 순서 깨질 수 있음                        │
│  - concurrency = 1 또는 파티션 단위 처리                          │
│  - Consumer 재시작 시 offset 관리 주의                            │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

```java
// Kafka Consumer 순서 보장 설정
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
            orderedKafkaListenerContainerFactory(
                ConsumerFactory<String, String> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // 파티션 당 1개 스레드로 순서 보장
        factory.setConcurrency(1);

        // 수동 ACK으로 정확한 오프셋 관리
        factory.getContainerProperties()
            .setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }
}
```

### 모니터링

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxMonitoring {

    private final OutboxEventRepository outboxRepository;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedRate = 30000)  // 30초마다
    public void collectMetrics() {
        // 상태별 이벤트 수
        long pendingCount = outboxRepository.countByStatus(OutboxStatus.PENDING);
        long failedCount = outboxRepository.countByStatus(OutboxStatus.FAILED);

        meterRegistry.gauge("outbox.events.pending", pendingCount);
        meterRegistry.gauge("outbox.events.failed", failedCount);

        // PENDING이 급격히 증가하면 Publisher 장애 의심
        if (pendingCount > 1000) {
            log.warn("Outbox PENDING 이벤트 {}건 - Publisher 상태 확인 필요",
                pendingCount);
        }

        // FAILED 이벤트 존재 시 알림
        if (failedCount > 0) {
            log.warn("Outbox FAILED 이벤트 {}건 - 수동 조치 필요",
                failedCount);
        }
    }
}
```

```
Outbox 모니터링 대시보드 (Grafana) 권장 패널:
──────────────────────────────────────────────────
1. PENDING 이벤트 수 (시계열) - 급증 시 알림
2. 발행 성공/실패 비율 (파이 차트)
3. 이벤트 발행 지연 시간 (히스토그램)
4. FAILED 이벤트 목록 (테이블)
5. Outbox 테이블 행 수 (클린업 효과 확인)
6. 이벤트 타입별 처리량 (시계열)
```

---

## 핵심 정리

### Outbox 패턴 도입 판단 기준

| 상황 | 권장 솔루션 |
|------|-----------|
| 단일 서비스, DB만 사용 | Outbox 불필요 (로컬 트랜잭션) |
| 서비스 간 이벤트 전달 필요 | **Outbox 패턴** |
| 이벤트 유실 허용 가능 | 직접 Kafka 발행 (Fire-and-Forget) |
| 실시간 이벤트 필수 | Outbox + CDC (Debezium) |
| Saga 패턴 사용 | **Outbox + Saga 조합** |

### 구현 체크리스트

```
Outbox 패턴 도입 체크리스트:
─────────────────────────────────────────
□ outbox_events 테이블 생성 (DDL)
□ OutboxEvent 엔티티 및 Repository 구현
□ 비즈니스 서비스에서 Outbox 저장 통합
□ Publisher 선택 (Polling vs CDC)
□ Polling: @Scheduled + ShedLock 설정
□ CDC: Debezium + Kafka Connect 설정
□ Consumer 멱등성 보장 (ProcessedEvent)
□ 순서 보장 전략 (Kafka 파티셔닝)
□ Outbox 클린업 스케줄 설정
□ 모니터링 구성 (메트릭 + 알림)
□ FAILED 이벤트 수동 처리 절차 문서화
□ 장애 시 복구 절차 문서화
```

---

## 면접 대비 핵심 질문

1. **Q: Dual Write 문제란 무엇이고 왜 위험한가요?**
   - A: Dual Write는 하나의 비즈니스 작업에서 DB와 메시지 브로커 등 두 개 이상의 시스템에 동시에 쓰기를 수행하는 것입니다. 두 쓰기 작업은 원자적으로 수행할 수 없어서, DB 저장 후 Kafka 발행 실패, 또는 Kafka 발행 후 DB 롤백 등의 시나리오에서 데이터 불일치가 발생합니다. 세무 도메인에서는 거래내역은 저장되었는데 세금계산 이벤트가 유실되면 세금이 계산되지 않는 심각한 문제가 발생할 수 있습니다.

2. **Q: Outbox 패턴의 핵심 아이디어는 무엇인가요?**
   - A: 메시지 발행을 DB 트랜잭션 안으로 가져오는 것입니다. Kafka에 직접 발행하는 대신, 동일 DB의 outbox 테이블에 이벤트를 저장합니다. 비즈니스 데이터와 이벤트가 하나의 DB 트랜잭션으로 커밋되므로 원자성이 보장됩니다. 이후 별도 프로세스(Polling Publisher 또는 CDC)가 outbox 테이블에서 이벤트를 읽어 메시지 브로커로 발행합니다. "at-least-once" 전달 보장이며, Consumer에서 멱등성을 보장해야 합니다.

3. **Q: Polling Publisher와 CDC 방식의 차이점은?**
   - A: Polling Publisher는 @Scheduled로 주기적으로 outbox 테이블을 조회하여 미발행 이벤트를 발행하는 방식입니다. 구현이 단순하고 추가 인프라가 불필요하지만, 폴링 주기만큼 지연이 발생하고 DB에 부하를 줍니다. CDC(Debezium)는 DB의 binlog/WAL을 실시간 감지하여 이벤트를 발행합니다. 밀리초 단위 지연, DB 부하 없음, 이벤트 순서 보장 등의 장점이 있으나 Kafka Connect + Debezium이라는 추가 인프라 운영이 필요합니다.

4. **Q: Saga 패턴과 Outbox 패턴을 함께 사용하는 이유는?**
   - A: Saga 패턴에서 Orchestrator가 다음 서비스에 Command를 보내거나, 각 서비스가 결과 이벤트를 Orchestrator에 보낼 때 Dual Write 문제가 동일하게 발생합니다. Outbox 패턴을 결합하면 Saga 상태 변경과 Command/Event 발행이 하나의 DB 트랜잭션으로 처리되어 Saga의 각 단계에서 안정적인 메시지 전달이 보장됩니다. Saga가 stuck 상태에 빠지는 것을 방지합니다.

5. **Q: Outbox 테이블 관리는 어떻게 하나요?**
   - A: Outbox 테이블은 이벤트가 계속 쌓이므로 주기적 정리가 필수입니다. PUBLISHED 상태의 이벤트는 보존 기간(예: 7일) 후 삭제합니다. FAILED 이벤트는 알림을 통해 운영팀이 수동으로 처리합니다. 클린업 배치는 ShedLock으로 중복 실행을 방지하고, 대량 삭제 시에는 batch 단위로 나누어 처리하여 DB 락 점유를 최소화합니다.

6. **Q: Consumer에서 멱등성을 어떻게 보장하나요?**
   - A: Outbox 패턴은 at-least-once 전달을 보장하므로 중복 메시지가 전달될 수 있습니다. Consumer 측에서는 processed_events 테이블에 eventId를 기록하고, 메시지 처리 전에 이미 처리된 이벤트인지 확인합니다. 이 확인과 비즈니스 로직 처리를 하나의 DB 트랜잭션으로 묶어 원자성을 보장합니다. 대안으로 비즈니스 로직 자체를 멱등하게 설계할 수도 있습니다(예: UPSERT 사용, 상태 기반 검증).

7. **Q: Outbox 패턴에서 이벤트 순서를 어떻게 보장하나요?**
   - A: Outbox 테이블의 Auto Increment ID로 삽입 순서를 보장하고, aggregate_id를 Kafka 메시지 키로 사용하여 동일 aggregate의 이벤트가 같은 파티션으로 전달되게 합니다. Kafka 파티션 내에서는 순서가 보장됩니다. Consumer 측에서는 concurrency를 1로 설정하거나 파티션 단위로 순차 처리해야 합니다. CDC 방식은 binlog 순서를 따르므로 더 강력한 순서 보장이 가능합니다.

8. **Q: 세무 도메인에서 Outbox 패턴이 특히 중요한 이유는?**
   - A: 세무 도메인은 데이터 정합성이 매우 중요합니다. 거래내역이 등록되었는데 세금계산 이벤트가 유실되면 세금이 잘못 계산되고, 신고서 제출 이벤트가 유실되면 환급 처리가 누락됩니다. 이는 고객의 세금 신고 오류로 이어져 가산세 등 금전적 피해를 야기합니다. Outbox 패턴으로 이벤트 유실을 방지하고, 멱등성 보장으로 중복 처리도 방지하여 세무 데이터의 정확성을 담보합니다.

---

*마지막 업데이트: 2026년 02월*
