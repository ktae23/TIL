# Kafka 완벽 가이드

Apache Kafka의 아키텍처부터 Spring Boot 통합, 메시지 보장 수준, SQS 비교, 세무 도메인 적용까지 실무 중심으로 정리한 가이드입니다.

## 목차
1. [Kafka 아키텍처](#kafka-아키텍처)
2. [Spring Kafka 통합](#spring-kafka-통합)
3. [메시지 보장 수준](#메시지-보장-수준)
4. [SQS vs Kafka 비교](#sqs-vs-kafka-비교)
5. [세무 도메인 적용](#세무-도메인-적용)
6. [운영 및 모니터링](#운영-및-모니터링)
7. [핵심 정리](#핵심-정리)
8. [면접 대비 핵심 질문](#면접-대비-핵심-질문)

---

## Kafka 아키텍처

### Kafka란?

Apache Kafka는 LinkedIn에서 개발하고 Apache Software Foundation에서 관리하는 **분산 이벤트 스트리밍 플랫폼**입니다. 높은 처리량, 내결함성, 확장성을 갖춘 메시지 시스템으로 실시간 데이터 파이프라인과 이벤트 기반 아키텍처의 핵심 인프라입니다.

```
┌──────────────────────────────────────────────────────────────────┐
│                    Kafka 핵심 컨셉 개요                            │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Producer ──► Broker Cluster ──► Consumer                        │
│                                                                   │
│  ┌─────────┐    ┌──────────────────────┐    ┌─────────┐         │
│  │Producer1│───►│  Kafka Cluster       │───►│Consumer1│         │
│  │Producer2│───►│  ┌──────┐ ┌──────┐  │───►│Consumer2│         │
│  │Producer3│───►│  │Broker│ │Broker│  │───►│Consumer3│         │
│  └─────────┘    │  │  1   │ │  2   │  │    └─────────┘         │
│                  │  └──────┘ └──────┘  │                         │
│                  │  ┌──────┐           │                         │
│                  │  │Broker│           │                         │
│                  │  │  3   │           │                         │
│                  │  └──────┘           │                         │
│                  └──────────────────────┘                         │
│                                                                   │
│  핵심 특징:                                                      │
│  - 분산 로그 기반 메시징                                         │
│  - Pull 방식 (Consumer가 데이터를 가져감)                        │
│  - 디스크 기반 영속성 (리텐션 기간 동안 보관)                    │
│  - 수평 확장 가능 (파티션 추가)                                  │
│  - 메시지 순서 보장 (파티션 내)                                  │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Broker, Topic, Partition

```
┌──────────────────────────────────────────────────────────────────┐
│                    Topic과 Partition 구조                         │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Topic: "tax-transactions"                                       │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                                                              │  │
│  │  Partition 0: [msg0][msg3][msg6][msg9 ][msg12] ──► offset  │  │
│  │  Partition 1: [msg1][msg4][msg7][msg10][msg13] ──► offset  │  │
│  │  Partition 2: [msg2][msg5][msg8][msg11][msg14] ──► offset  │  │
│  │                                                              │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                   │
│  Broker 1           Broker 2           Broker 3                  │
│  ┌──────────┐      ┌──────────┐      ┌──────────┐              │
│  │ P0(리더) │      │ P1(리더) │      │ P2(리더) │              │
│  │ P1(팔로워)│      │ P2(팔로워)│      │ P0(팔로워)│              │
│  └──────────┘      └──────────┘      └──────────┘              │
│                                                                   │
│  - Topic: 논리적 메시지 카테고리                                 │
│  - Partition: Topic의 물리적 분할 단위                           │
│  - Broker: Kafka 서버 인스턴스 (파티션을 호스팅)                 │
│  - Offset: 파티션 내 메시지의 순차적 번호                        │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Partition과 Consumer Group의 관계

```
┌──────────────────────────────────────────────────────────────────┐
│              Consumer Group과 Partition 매핑                      │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Topic: "bookkeeping-events" (6 Partitions)                      │
│                                                                   │
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐             │
│  │ P0  │ │ P1  │ │ P2  │ │ P3  │ │ P4  │ │ P5  │             │
│  └──┬──┘ └──┬──┘ └──┬──┘ └──┬──┘ └──┬──┘ └──┬──┘             │
│     │       │       │       │       │       │                    │
│  ───┼───────┼───────┼───────┼───────┼───────┼──── Group A       │
│     ▼       ▼       ▼       ▼       ▼       ▼                    │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐                   │
│  │ Consumer 1 │ │ Consumer 2 │ │ Consumer 3 │                   │
│  │ (P0, P1)   │ │ (P2, P3)   │ │ (P4, P5)   │                   │
│  └────────────┘ └────────────┘ └────────────┘                   │
│                                                                   │
│  ───┼───────┼───────┼───────┼───────┼───────┼──── Group B       │
│     ▼       ▼       ▼       ▼       ▼       ▼                    │
│  ┌─────────────────────────────────────────────┐                │
│  │ Consumer 1 (P0, P1, P2, P3, P4, P5)        │                │
│  └─────────────────────────────────────────────┘                │
│                                                                   │
│  규칙:                                                           │
│  - 하나의 파티션은 그룹 내 하나의 Consumer만 소비 가능           │
│  - Consumer 수 > Partition 수 → 유휴 Consumer 발생              │
│  - Consumer 수 < Partition 수 → 하나의 Consumer가 여러 파티션    │
│  - 서로 다른 Consumer Group은 독립적으로 소비                    │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Replication Factor와 ISR (In-Sync Replicas)

```
┌──────────────────────────────────────────────────────────────────┐
│                    Replication 구조                                │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Topic: "tax-transactions" (Replication Factor = 3)              │
│                                                                   │
│  Broker 1            Broker 2            Broker 3                │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐        │
│  │ P0 [Leader]  │   │ P0 [Follower]│   │ P0 [Follower]│        │
│  │ offset: 0-99 │   │ offset: 0-99 │   │ offset: 0-97 │        │
│  │ ISR ✓        │   │ ISR ✓        │   │ ISR ✗ (Lag)  │        │
│  └──────────────┘   └──────────────┘   └──────────────┘        │
│                                                                   │
│  ISR = {Broker1(Leader), Broker2}                                │
│                                                                   │
│  Write 흐름:                                                     │
│  Producer ──► Leader(Broker1) ──► Follower(Broker2) 동기 복제    │
│                                ──► Follower(Broker3) 비동기 복제  │
│                                                                   │
│  Leader 장애 시:                                                  │
│  - ISR 중 하나가 새 Leader로 선출 (Broker2)                      │
│  - Broker3은 ISR이 아니므로 Leader 후보에서 제외                 │
│  - unclean.leader.election.enable=false 권장                     │
│                                                                   │
│  acks 설정:                                                      │
│  - acks=0    : 응답 안 기다림 (최고 성능, 데이터 유실 가능)      │
│  - acks=1    : Leader만 확인 (기본값, Leader 장애 시 유실 가능)  │
│  - acks=all  : 모든 ISR 확인 (안전, 지연 증가)                   │
│                                                                   │
│  min.insync.replicas=2 + acks=all 조합이 운영 환경 권장          │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### KRaft 모드 (Zookeeper 없는 Kafka)

Kafka 3.3부터 프로덕션 준비된 **KRaft (Kafka Raft)** 모드가 도입되었습니다. Kafka 4.0에서 Zookeeper는 완전히 제거될 예정입니다.

```
┌──────────────────────────────────────────────────────────────────┐
│              Zookeeper 모드 vs KRaft 모드                         │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  [기존: Zookeeper 모드]                                          │
│  ┌──────────────────┐     ┌──────────────────┐                  │
│  │ Zookeeper Cluster│◄───►│  Kafka Cluster   │                  │
│  │ ┌────┐ ┌────┐   │     │ ┌──────┐┌──────┐│                  │
│  │ │ ZK │ │ ZK │   │     │ │Broker││Broker││                  │
│  │ └────┘ └────┘   │     │ └──────┘└──────┘│                  │
│  │ ┌────┐          │     │ ┌──────┐        │                  │
│  │ │ ZK │          │     │ │Broker│        │                  │
│  │ └────┘          │     │ └──────┘        │                  │
│  └──────────────────┘     └──────────────────┘                  │
│  → 별도 Zookeeper 클러스터 운영 필요                             │
│  → 메타데이터 관리를 ZK에 위임                                   │
│                                                                   │
│  [신규: KRaft 모드]                                              │
│  ┌──────────────────────────────────────────┐                   │
│  │          Kafka Cluster (KRaft)           │                   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐│                   │
│  │  │Controller│ │Controller│ │Controller││                   │
│  │  │+ Broker  │ │+ Broker  │ │+ Broker  ││                   │
│  │  │(Active)  │ │(Standby) │ │(Standby) ││                   │
│  │  └──────────┘ └──────────┘ └──────────┘│                   │
│  └──────────────────────────────────────────┘                   │
│  → Zookeeper 불필요                                              │
│  → Raft 합의 알고리즘으로 메타데이터 관리                        │
│  → 단일 시스템으로 운영 단순화                                   │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

| 구분 | Zookeeper 모드 | KRaft 모드 |
|------|---------------|------------|
| 의존성 | Zookeeper 클러스터 별도 운영 | 자체 내장 |
| 메타데이터 관리 | Zookeeper에 저장 | Kafka 내부 토픽 (__cluster_metadata) |
| 파티션 수 제한 | ~200K (ZK 병목) | 수백만 파티션 가능 |
| 장애 복구 | Controller 재선출 느림 | Raft 기반 빠른 리더 선출 |
| 운영 복잡도 | 높음 (2개 시스템) | 낮음 (1개 시스템) |
| 시작 시간 | 느림 (ZK 동기화) | 빠름 |

```properties
# KRaft 모드 설정 (server.properties)
process.roles=broker,controller
node.id=1
controller.quorum.voters=1@broker1:9093,2@broker2:9093,3@broker3:9093
controller.listener.names=CONTROLLER
listeners=PLAINTEXT://:9092,CONTROLLER://:9093
log.dirs=/var/kafka/data
```

---

## Spring Kafka 통합

### Producer 설정 및 구현

#### 의존성

```xml
<!-- build.gradle.kts 또는 pom.xml -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

#### application.yml 설정

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092,localhost:9093,localhost:9094
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all                        # 모든 ISR 확인
      retries: 3                       # 재시도 횟수
      properties:
        enable.idempotence: true       # 멱등성 활성화
        max.in.flight.requests.per.connection: 5
        delivery.timeout.ms: 120000
        linger.ms: 10                  # 배치 전송 대기 (ms)
        batch.size: 16384              # 배치 크기 (bytes)
        buffer.memory: 33554432        # 버퍼 메모리 (32MB)
```

#### KafkaTemplate 기반 Producer

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TaxTransactionProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // 동기 전송
    public void sendSync(String topic, String key, TaxTransactionEvent event) {
        try {
            SendResult<String, Object> result = kafkaTemplate.send(topic, key, event)
                .get(10, TimeUnit.SECONDS);

            RecordMetadata metadata = result.getRecordMetadata();
            log.info("메시지 전송 완료 - topic: {}, partition: {}, offset: {}",
                metadata.topic(), metadata.partition(), metadata.offset());

        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            log.error("메시지 전송 실패 - topic: {}, key: {}", topic, key, e);
            throw new KafkaProduceException("메시지 전송 실패", e);
        }
    }

    // 비동기 전송 (Callback)
    public void sendAsync(String topic, String key, TaxTransactionEvent event) {
        kafkaTemplate.send(topic, key, event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("메시지 전송 실패 - key: {}", key, ex);
                    handleFailure(topic, key, event, ex);
                } else {
                    RecordMetadata metadata = result.getRecordMetadata();
                    log.info("메시지 전송 완료 - partition: {}, offset: {}",
                        metadata.partition(), metadata.offset());
                }
            });
    }

    // ProducerRecord 직접 구성 (헤더 추가)
    public void sendWithHeaders(TaxTransactionEvent event) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(
            "tax-transactions",
            null,                           // partition (null = 키 기반 자동 배정)
            event.getBusinessId(),          // key
            event,                          // value
            List.of(
                new RecordHeader("event-type", event.getType().getBytes()),
                new RecordHeader("trace-id", MDC.get("traceId").getBytes()),
                new RecordHeader("timestamp", String.valueOf(System.currentTimeMillis()).getBytes())
            )
        );
        kafkaTemplate.send(record);
    }

    private void handleFailure(String topic, String key, Object event, Throwable ex) {
        // DLQ 전송 또는 DB 저장 등 실패 처리
        log.error("DLQ로 전송 또는 재시도 로직 실행", ex);
    }
}
```

#### ProducerConfig 커스텀 설정

```java
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // 안정성 설정
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // 성능 설정
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768);  // 32KB
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 67108864);  // 64MB

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

### Consumer 설정 및 구현

#### application.yml Consumer 설정

```yaml
spring:
  kafka:
    consumer:
      group-id: tax-service-group
      auto-offset-reset: earliest          # 처음부터 읽기 (latest: 최신부터)
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      enable-auto-commit: false            # 수동 커밋
      max-poll-records: 500               # poll당 최대 레코드 수
      properties:
        spring.json.trusted.packages: "com.taxservice.event.*"
        session.timeout.ms: 30000
        heartbeat.interval.ms: 10000
        max.poll.interval.ms: 300000       # 5분 내 처리 완료 필요
```

#### @KafkaListener 기반 Consumer

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class TaxTransactionConsumer {

    private final TaxTransactionService transactionService;
    private final ProcessedMessageRepository processedMessageRepository;

    // 기본 Listener
    @KafkaListener(
        topics = "tax-transactions",
        groupId = "tax-service-group",
        concurrency = "3"    // 3개의 Consumer Thread
    )
    public void consume(
            @Payload TaxTransactionEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        log.info("메시지 수신 - partition: {}, offset: {}, event: {}",
            partition, offset, event.getEventId());

        try {
            // 멱등성 체크
            if (processedMessageRepository.existsById(event.getEventId())) {
                log.info("이미 처리된 메시지: {}", event.getEventId());
                ack.acknowledge();
                return;
            }

            // 비즈니스 로직 처리
            transactionService.processTransaction(event);

            // 처리 완료 기록
            processedMessageRepository.save(
                new ProcessedMessage(event.getEventId(), LocalDateTime.now())
            );

            // 수동 커밋
            ack.acknowledge();

        } catch (Exception e) {
            log.error("메시지 처리 실패 - eventId: {}", event.getEventId(), e);
            // ack하지 않으면 재처리됨 (또는 ErrorHandler로 위임)
            throw e;
        }
    }

    // 배치 Listener
    @KafkaListener(
        topics = "bookkeeping-events",
        groupId = "bookkeeping-batch-group",
        containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void consumeBatch(
            List<ConsumerRecord<String, BookkeepingEvent>> records,
            Acknowledgment ack) {

        log.info("배치 수신 - {} 건", records.size());

        try {
            List<BookkeepingEvent> events = records.stream()
                .map(ConsumerRecord::value)
                .toList();

            transactionService.processBatch(events);
            ack.acknowledge();

        } catch (Exception e) {
            log.error("배치 처리 실패 - {} 건", records.size(), e);
            throw e;
        }
    }

    // 특정 파티션 할당
    @KafkaListener(
        topicPartitions = @TopicPartition(
            topic = "tax-calculation-results",
            partitions = {"0", "1"}
        ),
        groupId = "tax-calc-group"
    )
    public void consumeFromPartitions(
            @Payload TaxCalculationResult result,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {

        log.info("세금 계산 결과 수신 - partition: {}", partition);
        transactionService.saveCalculationResult(result);
    }
}
```

#### ConsumerConfig 커스텀 설정

```java
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "tax-service-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // 오프셋 관리
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // 성능 설정
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);         // 1KB
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);

        // 세션 관리
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);

        // JSON 역직렬화 설정
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.taxservice.event.*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    // 단건 처리 Container Factory
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
            kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties()
            .setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(errorHandler());

        return factory;
    }

    // 배치 처리 Container Factory
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
            batchKafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.setBatchListener(true);
        factory.getContainerProperties()
            .setAckMode(ContainerProperties.AckMode.MANUAL);

        return factory;
    }

    @Bean
    public DefaultErrorHandler errorHandler() {
        // 최대 3회 재시도, 1초 간격 (지수 백오프)
        BackOff backOff = new ExponentialBackOff(1000L, 2.0);
        ((ExponentialBackOff) backOff).setMaxElapsedTime(10000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(
            new DeadLetterPublishingRecoverer(kafkaTemplate()),
            backOff
        );

        // 재시도하지 않을 예외 등록
        handler.addNotRetryableExceptions(
            DeserializationException.class,
            IllegalArgumentException.class
        );

        return handler;
    }
}
```

### 직렬화/역직렬화

#### JSON 직렬화

```java
// 이벤트 클래스
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxTransactionEvent {
    private String eventId;
    private String businessId;          // 사업자번호
    private String transactionType;     // INCOME, EXPENSE, TAX_PAYMENT
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private Map<String, String> metadata;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }
}

// JsonSerializer 커스텀 설정
@Bean
public ProducerFactory<String, Object> producerFactory() {
    JsonSerializer<Object> serializer = new JsonSerializer<>();
    serializer.setAddTypeInfo(true);  // __TypeId__ 헤더에 타입 정보 추가

    return new DefaultKafkaProducerFactory<>(
        producerProps(),
        new StringSerializer(),
        serializer
    );
}
```

#### Avro 직렬화 (Schema Registry 사용)

```xml
<!-- Avro 의존성 -->
<dependency>
    <groupId>io.confluent</groupId>
    <artifactId>kafka-avro-serializer</artifactId>
    <version>7.5.0</version>
</dependency>
```

```avro
// tax_transaction.avsc
{
  "type": "record",
  "name": "TaxTransaction",
  "namespace": "com.taxservice.avro",
  "fields": [
    {"name": "eventId", "type": "string"},
    {"name": "businessId", "type": "string"},
    {"name": "transactionType", "type": {"type": "enum", "name": "TransactionType",
      "symbols": ["INCOME", "EXPENSE", "TAX_PAYMENT"]}},
    {"name": "amount", "type": {"type": "bytes", "logicalType": "decimal",
      "precision": 18, "scale": 2}},
    {"name": "transactionDate", "type": {"type": "long", "logicalType": "timestamp-millis"}}
  ]
}
```

```yaml
# Avro + Schema Registry 설정
spring:
  kafka:
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: io.confluent.kafka.serializers.KafkaAvroSerializer
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: io.confluent.kafka.serializers.KafkaAvroDeserializer
    properties:
      schema.registry.url: http://localhost:8081
      specific.avro.reader: true
```

### Error Handling

```
┌──────────────────────────────────────────────────────────────────┐
│                    Kafka Error Handling 전략                      │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  메시지 수신                                                     │
│     │                                                             │
│     ▼                                                             │
│  처리 시도 ──── 성공 ──► offset commit ──► 다음 메시지            │
│     │                                                             │
│     │ 실패                                                       │
│     ▼                                                             │
│  재시도 (RetryBackOff)                                           │
│     │                                                             │
│     │ 재시도 소진                                                 │
│     ▼                                                             │
│  Dead Letter Topic (DLT) 전송                                    │
│  topic: "tax-transactions.DLT"                                   │
│     │                                                             │
│     ▼                                                             │
│  알림 발송 + 수동 처리 대기                                      │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

```java
// Dead Letter Topic 설정
@Configuration
public class KafkaErrorHandlingConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> template) {
        // DLT로 전송하는 Recoverer
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            template,
            (record, ex) -> {
                // 커스텀 DLT 토픽명 결정
                return new TopicPartition(
                    record.topic() + ".DLT",
                    record.partition()
                );
            }
        );

        // 지수 백오프: 1초 시작, 최대 10초, 3회 재시도
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxElapsedTime(10000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

        // 재시도 불필요한 예외 (즉시 DLT로 전송)
        handler.addNotRetryableExceptions(
            DeserializationException.class,
            ClassCastException.class,
            NullPointerException.class
        );

        // 재시도 리스너 (모니터링용)
        handler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.warn("재시도 {}/{} - topic: {}, offset: {}",
                deliveryAttempt, 3, record.topic(), record.offset());
        });

        return handler;
    }

    // DLT 메시지 처리 (수동 재처리용)
    @KafkaListener(
        topics = "tax-transactions.DLT",
        groupId = "dlt-processor"
    )
    public void processDlt(
            ConsumerRecord<String, Object> record,
            @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) String exMessage,
            @Header(KafkaHeaders.DLT_ORIGINAL_TOPIC) String originalTopic) {

        log.error("DLT 메시지 - 원본 토픽: {}, 에러: {}, 값: {}",
            originalTopic, exMessage, record.value());

        // 알림 발송
        alertService.sendDltAlert(originalTopic, exMessage, record);
    }
}
```

### Spring Boot Auto-Configuration 활용

Spring Boot는 `spring.kafka.*` 프로퍼티를 기반으로 자동으로 다음을 구성합니다:

```
┌──────────────────────────────────────────────────────────────────┐
│              Spring Boot Kafka Auto-Configuration                │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  spring.kafka.* 프로퍼티                                         │
│       │                                                           │
│       ▼                                                           │
│  KafkaAutoConfiguration                                          │
│       │                                                           │
│       ├──► KafkaTemplate (자동 생성)                              │
│       ├──► ProducerFactory (자동 생성)                            │
│       ├──► ConsumerFactory (자동 생성)                            │
│       ├──► ConcurrentKafkaListenerContainerFactory (자동 생성)    │
│       └──► KafkaAdmin (토픽 자동 생성)                            │
│                                                                   │
│  @EnableKafka는 Spring Boot에서 자동 적용 (명시 불필요)          │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

```java
// 토픽 자동 생성 (KafkaAdmin 활용)
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic taxTransactionsTopic() {
        return TopicBuilder.name("tax-transactions")
            .partitions(6)
            .replicas(3)
            .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(7 * 24 * 60 * 60 * 1000))
            .config(TopicConfig.CLEANUP_POLICY_CONFIG, "delete")
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
            .build();
    }

    @Bean
    public NewTopic bookkeepingEventsTopic() {
        return TopicBuilder.name("bookkeeping-events")
            .partitions(12)
            .replicas(3)
            .compact()    // Log Compaction
            .build();
    }

    @Bean
    public NewTopic taxCalculationResultsTopic() {
        return TopicBuilder.name("tax-calculation-results")
            .partitions(6)
            .replicas(3)
            .build();
    }
}
```

---

## 메시지 보장 수준

### At-most-once, At-least-once, Exactly-once

```
┌──────────────────────────────────────────────────────────────────┐
│              메시지 전달 보장 수준 비교                            │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  At-most-once (최대 한 번)                                       │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ Producer ──► Broker ──► Consumer                           │  │
│  │                          commit offset 먼저                │  │
│  │                          처리 중 실패 → 메시지 유실        │  │
│  │                                                             │  │
│  │ 설정: enable.auto.commit=true, acks=0                      │  │
│  │ 적합: 로그, 메트릭 (유실 허용)                             │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                   │
│  At-least-once (최소 한 번) ← 가장 일반적                       │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ Producer ──► Broker ──► Consumer                           │  │
│  │                          처리 완료 후 commit offset         │  │
│  │                          commit 전 장애 → 재처리 (중복)    │  │
│  │                                                             │  │
│  │ 설정: enable.auto.commit=false, acks=all, 수동 커밋        │  │
│  │ 적합: 대부분의 비즈니스 로직 (멱등성 처리 필요)            │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                   │
│  Exactly-once (정확히 한 번)                                     │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ Producer ──► Broker ──► Consumer                           │  │
│  │ Transactional Producer + Transactional Consumer            │  │
│  │ 또는 Idempotent Producer + Consumer 멱등성 처리            │  │
│  │                                                             │  │
│  │ 설정: Kafka Transactions API 사용                          │  │
│  │ 적합: 금융, 결제 (중복/유실 불허)                          │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

| 보장 수준 | 중복 가능 | 유실 가능 | 성능 | 사용 사례 |
|-----------|----------|----------|------|-----------|
| At-most-once | X | O | 최고 | 로그, 메트릭 |
| At-least-once | O | X | 높음 | 일반 비즈니스 (멱등성 필요) |
| Exactly-once | X | X | 보통 | 금융, 결제, 세금 계산 |

### Idempotent Producer

```
┌──────────────────────────────────────────────────────────────────┐
│                    Idempotent Producer 동작                       │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  enable.idempotence=true                                         │
│                                                                   │
│  Producer                          Broker                        │
│     │                                 │                           │
│     │ msg(PID=1, Seq=0) ────────────► │ 저장 ✓                   │
│     │                                 │                           │
│     │ msg(PID=1, Seq=1) ────────────► │ 저장 ✓                   │
│     │                                 │                           │
│     │ msg(PID=1, Seq=1) ────────────► │ 중복 감지! 무시          │
│     │  (네트워크 재시도)               │                           │
│     │                                 │                           │
│     │ msg(PID=1, Seq=2) ────────────► │ 저장 ✓                   │
│     │                                 │                           │
│                                                                   │
│  PID (Producer ID) + Sequence Number로 중복 감지                 │
│  - Broker가 (PID, Partition, Seq) 조합을 추적                    │
│  - 동일 Seq 재수신 시 자동 무시 (DeDup)                          │
│  - 단일 파티션 내에서만 보장                                     │
│                                                                   │
│  자동 설정되는 값:                                                │
│  - acks=all (자동 강제)                                           │
│  - retries=Integer.MAX_VALUE                                     │
│  - max.in.flight.requests.per.connection ≤ 5                     │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

```java
// Idempotent Producer 설정
@Bean
public ProducerFactory<String, Object> idempotentProducerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

    // 멱등성 활성화 (이것만으로 Producer 측 중복 방지)
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

    // 아래 값은 자동 설정되지만 명시 가능
    // props.put(ProducerConfig.ACKS_CONFIG, "all");
    // props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
    // props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

    return new DefaultKafkaProducerFactory<>(props);
}
```

### Transactional Messaging

```java
// Transactional Producer 설정
@Configuration
public class KafkaTransactionConfig {

    @Bean
    public ProducerFactory<String, Object> transactionalProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // 트랜잭션 ID 접두사 (인스턴스별 고유해야 함)
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "tax-service-tx-");

        DefaultKafkaProducerFactory<String, Object> factory =
            new DefaultKafkaProducerFactory<>(props);
        factory.setTransactionIdPrefix("tax-service-tx-");

        return factory;
    }

    @Bean
    public KafkaTransactionManager<String, Object> kafkaTransactionManager() {
        return new KafkaTransactionManager<>(transactionalProducerFactory());
    }
}

// Transactional Producer 사용
@Service
@RequiredArgsConstructor
public class TransactionalTaxProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // 방법 1: executeInTransaction 사용
    public void sendTransactional(TaxTransactionEvent event) {
        kafkaTemplate.executeInTransaction(operations -> {
            operations.send("tax-transactions", event.getBusinessId(), event);
            operations.send("audit-log", event.getBusinessId(),
                new AuditLogEvent(event));
            return true;
            // 두 메시지가 모두 성공하거나 모두 실패
        });
    }

    // 방법 2: @Transactional 어노테이션 사용
    @Transactional("kafkaTransactionManager")
    public void sendWithAnnotation(TaxTransactionEvent event) {
        kafkaTemplate.send("tax-transactions", event.getBusinessId(), event);
        kafkaTemplate.send("audit-log", event.getBusinessId(),
            new AuditLogEvent(event));
        // 메서드 종료 시 트랜잭션 커밋
    }

    // 방법 3: DB 트랜잭션 + Kafka 트랜잭션 연동
    @Transactional  // JPA + Kafka 트랜잭션 동기화 (ChainedTransactionManager)
    public void sendWithDbTransaction(TaxTransactionEvent event) {
        // DB 저장
        transactionRepository.save(toEntity(event));

        // Kafka 전송 (같은 트랜잭션)
        kafkaTemplate.send("tax-transactions", event.getBusinessId(), event);
    }
}
```

### Consumer Offset 관리

```
┌──────────────────────────────────────────────────────────────────┐
│              Consumer Offset 관리 전략                            │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  __consumer_offsets 토픽에 offset 저장                            │
│                                                                   │
│  자동 커밋 (Auto Commit)                                         │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ enable.auto.commit=true                                    │  │
│  │ auto.commit.interval.ms=5000 (기본 5초)                    │  │
│  │                                                             │  │
│  │ 장점: 단순함                                               │  │
│  │ 단점: 처리 완료 전 커밋 → 메시지 유실 가능                 │  │
│  │       처리 완료 후 커밋 전 장애 → 중복 처리                │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                   │
│  수동 커밋 (Manual Commit) ← 운영 환경 권장                     │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ enable.auto.commit=false                                   │  │
│  │                                                             │  │
│  │ AckMode 옵션:                                              │  │
│  │ - MANUAL:           acknowledge() 호출 시 배치 커밋        │  │
│  │ - MANUAL_IMMEDIATE: acknowledge() 호출 시 즉시 커밋        │  │
│  │ - RECORD:           레코드마다 자동 커밋                   │  │
│  │ - BATCH:            poll() 반환 레코드 모두 처리 후 커밋   │  │
│  │ - TIME:             일정 시간 간격으로 커밋                │  │
│  │ - COUNT:            일정 개수마다 커밋                     │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

```java
// AckMode별 구현 예시
@Configuration
public class AckModeExamples {

    // MANUAL_IMMEDIATE: 건별 즉시 커밋 (가장 안전, 성능 낮음)
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
            manualImmediateFactory(ConsumerFactory<String, Object> cf) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(cf);
        factory.getContainerProperties()
            .setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    // BATCH: poll() 단위로 커밋 (성능과 안전성 균형)
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
            batchAckFactory(ConsumerFactory<String, Object> cf) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(cf);
        factory.getContainerProperties()
            .setAckMode(ContainerProperties.AckMode.BATCH);
        return factory;
    }
}
```

### Consumer Rebalancing 전략

```
┌──────────────────────────────────────────────────────────────────┐
│              Consumer Rebalancing 과정                            │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Rebalancing 트리거 조건:                                        │
│  - Consumer 추가/제거 (스케일 아웃/인)                           │
│  - Consumer 장애 (heartbeat 타임아웃)                            │
│  - 토픽 파티션 수 변경                                           │
│  - Consumer가 max.poll.interval.ms 초과                          │
│                                                                   │
│  Rebalancing 중 발생하는 일:                                     │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ 1. 모든 Consumer가 파티션 할당 해제 (Stop-the-World)       │  │
│  │ 2. Group Coordinator가 새 할당 계산                        │  │
│  │ 3. 모든 Consumer에 새 파티션 할당                          │  │
│  │ → 이 동안 메시지 소비 중단!                                │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                   │
│  파티션 할당 전략:                                                │
│  ┌──────────────┬──────────────────────────────────────────┐    │
│  │ RangeAssignor │ 토픽별로 파티션을 연속 범위로 할당       │    │
│  │ (기본값)      │ C1:[P0,P1] C2:[P2,P3] C3:[P4,P5]       │    │
│  ├──────────────┼──────────────────────────────────────────┤    │
│  │ RoundRobin   │ 모든 파티션을 순환 배분                   │    │
│  │ Assignor     │ C1:[P0,P3] C2:[P1,P4] C3:[P2,P5]        │    │
│  ├──────────────┼──────────────────────────────────────────┤    │
│  │ Sticky       │ 기존 할당 최대한 유지 (최소 이동)         │    │
│  │ Assignor     │ 재할당 시 변경 최소화                     │    │
│  ├──────────────┼──────────────────────────────────────────┤    │
│  │ Cooperative  │ 점진적 재할당 (Stop-the-World 없음)       │    │
│  │ Sticky       │ Kafka 2.4+ 권장, 가용성 향상              │    │
│  └──────────────┴──────────────────────────────────────────┘    │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

```java
// CooperativeSticky 전략 설정 (Kafka 2.4+)
@Bean
public ConsumerFactory<String, Object> consumerFactory() {
    Map<String, Object> props = new HashMap<>();
    // ... 기본 설정 생략 ...

    // Cooperative Sticky 할당 전략 (재밸런싱 중 메시지 소비 중단 최소화)
    props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
        CooperativeStickyAssignor.class.getName());

    return new DefaultKafkaConsumerFactory<>(props);
}

// Rebalancing 이벤트 감지
@Component
@Slf4j
public class RebalanceListener implements ConsumerAwareRebalanceListener {

    @Override
    public void onPartitionsRevoked(Consumer<?, ?> consumer,
                                     Collection<TopicPartition> partitions) {
        log.info("파티션 해제됨: {}", partitions);
        // 처리 중인 메시지 커밋
        consumer.commitSync();
    }

    @Override
    public void onPartitionsAssigned(Consumer<?, ?> consumer,
                                      Collection<TopicPartition> partitions) {
        log.info("파티션 할당됨: {}", partitions);
    }

    @Override
    public void onPartitionsLost(Consumer<?, ?> consumer,
                                  Collection<TopicPartition> partitions) {
        log.warn("파티션 유실됨 (비정상 해제): {}", partitions);
    }
}
```

---

## SQS vs Kafka 비교

> zaritalk에서 SQS를 사용한 경험을 바탕으로, Kafka와의 차이를 정리합니다.

### 아키텍처 근본 차이

```
┌──────────────────────────────────────────────────────────────────┐
│              SQS vs Kafka 아키텍처 비교                           │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  [SQS: Push 기반 메시지 큐]                                      │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                                                             │  │
│  │  Producer ──► SQS Queue ──► Consumer                       │  │
│  │                   │                                         │  │
│  │                   │  메시지 처리 후 삭제                    │  │
│  │                   │  (한 번 소비하면 사라짐)                │  │
│  │                   │                                         │  │
│  │  특징:                                                     │  │
│  │  - 메시지는 소비 후 삭제됨                                 │  │
│  │  - Visibility Timeout으로 중복 방지                        │  │
│  │  - FIFO Queue로 순서 보장 가능 (초당 300 TPS 제한)         │  │
│  │  - 서버리스 (관리 불필요)                                  │  │
│  │  - 메시지 최대 14일 보관                                   │  │
│  │                                                             │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                   │
│  [Kafka: Pull 기반 이벤트 스트리밍]                              │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                                                             │  │
│  │  Producer ──► Kafka Topic ──► Consumer Group A             │  │
│  │                    │                                        │  │
│  │                    │──────► Consumer Group B                │  │
│  │                    │                                        │  │
│  │                    │  메시지 유지 (리텐션 기간)             │  │
│  │                    │  (여러 Consumer가 독립적으로 소비)     │  │
│  │                    │                                        │  │
│  │  특징:                                                     │  │
│  │  - 메시지는 리텐션 기간 동안 보관 (삭제 안 됨)            │  │
│  │  - Consumer가 offset으로 위치 관리                         │  │
│  │  - 파티션 내 순서 보장                                     │  │
│  │  - 클러스터 직접 운영 필요 (또는 Managed Service)          │  │
│  │  - 무제한 보관 가능 (디스크 한도)                          │  │
│  │                                                             │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 상세 비교표

| 항목 | SQS | Kafka |
|------|-----|-------|
| **모델** | 메시지 큐 (Point-to-Point) | 이벤트 스트리밍 (Pub/Sub) |
| **메시지 보관** | 소비 후 삭제 | 리텐션 기간 동안 보관 |
| **소비 방식** | Poll (Long Polling) | Pull (Consumer가 offset 관리) |
| **순서 보장** | FIFO Queue만 (300 TPS) | 파티션 내 보장 (무제한 TPS) |
| **처리량** | Standard: 무제한, FIFO: 300 TPS | 초당 수백만 메시지 가능 |
| **지연시간** | ~ms (Standard) | ~ms |
| **다중 Consumer** | 불가 (한 번 소비) | 가능 (Consumer Group 독립) |
| **메시지 리플레이** | 불가 | 가능 (offset 이동) |
| **운영 부담** | 없음 (AWS 관리형) | 높음 (직접 운영) 또는 MSK |
| **비용 모델** | 요청당 과금 | 인스턴스 + 스토리지 과금 |
| **메시지 크기** | 최대 256KB | 기본 1MB (설정 가능) |
| **DLQ** | 내장 지원 | 직접 구현 |

### 비용 모델 비교

```
┌──────────────────────────────────────────────────────────────────┐
│              비용 모델 비교 (월간 추정)                            │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  시나리오: 하루 1,000만 건 메시지, 월 3억 건                     │
│                                                                   │
│  SQS Standard:                                                   │
│  - 첫 100만 건 무료                                              │
│  - 이후 100만 건당 $0.40                                         │
│  - 월 비용: ~$120 (메시지만, 데이터 전송 별도)                   │
│  - 트래픽 적을 때 매우 저렴                                      │
│                                                                   │
│  Kafka (Self-managed, 3 Broker):                                 │
│  - EC2: m5.xlarge × 3 = ~$450/월                                │
│  - EBS: 500GB × 3 = ~$150/월                                    │
│  - 월 비용: ~$600 (고정 비용)                                    │
│  - 트래픽 많을수록 비용 효율적                                   │
│                                                                   │
│  손익분기점:                                                      │
│  SQS ────────────────┐                                           │
│                       ├── 하루 약 5,000만 건 이상이면 Kafka 유리  │
│  Kafka ───────────────┘                                           │
│                                                                   │
│  결론:                                                            │
│  - 소규모/변동 트래픽 → SQS                                     │
│  - 대규모/안정 트래픽 → Kafka                                    │
│  - 이벤트 리플레이/멀티 컨슈머 필요 → Kafka                     │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 사용 사례별 선택 기준

```
✅ SQS를 선택해야 하는 경우:
   - 단순한 작업 큐 (비동기 작업 처리)
   - 서버리스 아키텍처 (Lambda + SQS)
   - AWS 생태계 내에서 간단한 메시징
   - 운영 부담을 최소화하고 싶을 때
   - 트래픽이 불규칙하거나 소규모일 때
   - zaritalk 사례: 푸시 알림 발송, 비동기 이메일 처리

❌ SQS가 부적합한 경우:
   - 여러 서비스가 같은 이벤트를 소비해야 할 때
   - 이벤트 히스토리 리플레이가 필요할 때
   - 높은 처리량 + 순서 보장이 동시에 필요할 때

✅ Kafka를 선택해야 하는 경우:
   - 이벤트 소싱 / CQRS 패턴
   - 실시간 데이터 파이프라인 (거래내역 스트리밍)
   - 여러 Consumer Group이 독립적으로 소비
   - 이벤트 리플레이가 필요한 경우
   - 높은 처리량 (초당 수십만 건 이상)
   - 세무 플랫폼 사례: 거래내역 스트리밍, 기장 자동화 파이프라인

❌ Kafka가 부적합한 경우:
   - 단순한 작업 큐 (오버엔지니어링)
   - 서버리스 환경 (관리형 제외)
   - 소규모 트래픽 (비용 비효율)
```

### 코드 비교 예시

```java
// ============= SQS (zaritalk 스타일) =============

// SQS Producer
@Service
@RequiredArgsConstructor
public class SqsNotificationProducer {

    private final AmazonSQS amazonSQS;

    @Value("${aws.sqs.notification-queue-url}")
    private String queueUrl;

    public void sendNotification(NotificationEvent event) {
        SendMessageRequest request = new SendMessageRequest()
            .withQueueUrl(queueUrl)
            .withMessageBody(objectMapper.writeValueAsString(event))
            .withMessageGroupId(event.getUserId())     // FIFO 전용
            .withMessageDeduplicationId(event.getEventId());  // 중복 방지

        amazonSQS.sendMessage(request);
    }
}

// SQS Consumer
@Component
@RequiredArgsConstructor
public class SqsNotificationConsumer {

    @SqsListener(value = "${aws.sqs.notification-queue-url}",
                 deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void consume(@Payload NotificationEvent event) {
        // 처리 성공 시 자동 삭제
        notificationService.send(event);
    }
}

// ============= Kafka (세무 플랫폼 스타일) =============

// Kafka Producer
@Service
@RequiredArgsConstructor
public class KafkaTransactionProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendTransaction(TaxTransactionEvent event) {
        kafkaTemplate.send(
            "tax-transactions",          // topic
            event.getBusinessId(),       // key (같은 사업자는 같은 파티션)
            event                        // value
        ).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("전송 실패", ex);
            }
        });
    }
}

// Kafka Consumer
@Component
@RequiredArgsConstructor
public class KafkaTransactionConsumer {

    @KafkaListener(topics = "tax-transactions",
                   groupId = "bookkeeping-service")
    public void consume(@Payload TaxTransactionEvent event,
                        Acknowledgment ack) {
        try {
            bookkeepingService.process(event);
            ack.acknowledge();  // 수동 커밋
        } catch (Exception e) {
            // 재시도 또는 DLT로 이동
            throw e;
        }
    }
}
```

---

## 세무 도메인 적용

### 거래내역 이벤트 스트리밍 시나리오

```
┌──────────────────────────────────────────────────────────────────┐
│              세무 서비스 이벤트 스트리밍 아키텍처                  │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  은행/카드 연동          Kafka Cluster           소비 서비스      │
│  ┌──────────┐      ┌──────────────────┐     ┌──────────────┐    │
│  │ 은행 API │──┐   │                  │  ┌─►│ 기장 서비스  │    │
│  └──────────┘  │   │  tax-            │  │  │ (자동 분개)  │    │
│  ┌──────────┐  ├──►│  transactions    │──┤  └──────────────┘    │
│  │ 카드사   │──┤   │  (12 partitions) │  │  ┌──────────────┐    │
│  │ API      │  │   │                  │  ├─►│ 세금 계산    │    │
│  └──────────┘  │   └──────────────────┘  │  │ 서비스       │    │
│  ┌──────────┐  │   ┌──────────────────┐  │  └──────────────┘    │
│  │ 홈택스   │──┘   │  bookkeeping-    │  │  ┌──────────────┐    │
│  │ 스크래핑 │      │  events          │──┼─►│ 대시보드     │    │
│  └──────────┘      │  (6 partitions)  │  │  │ (실시간)     │    │
│                     └──────────────────┘  │  └──────────────┘    │
│                     ┌──────────────────┐  │  ┌──────────────┐    │
│                     │  tax-calculation-│  └─►│ 알림 서비스  │    │
│                     │  results         │     │ (고객 알림)  │    │
│                     │  (6 partitions)  │     └──────────────┘    │
│                     └──────────────────┘                         │
│                                                                   │
│  핵심 설계:                                                      │
│  - 사업자번호(businessId)를 Partition Key로 사용                 │
│  - 같은 사업자의 거래내역은 같은 파티션 → 순서 보장              │
│  - 여러 Consumer Group이 독립적으로 소비                         │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 기장 자동화 파이프라인

```
┌──────────────────────────────────────────────────────────────────┐
│              기장 자동화 파이프라인 (Event-Driven)                │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Step 1: 거래내역 수집                                           │
│  ┌────────────┐    ┌─────────────────────────────────────────┐  │
│  │ 은행/카드  │───►│ topic: raw-transactions                 │  │
│  │ 데이터수집 │    │ key: businessId                          │  │
│  └────────────┘    │ value: {txId, amount, date, memo, ...}  │  │
│                     └──────────────────┬──────────────────────┘  │
│                                         │                        │
│  Step 2: AI 분류                        ▼                        │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ AI 분류 서비스 (Consumer Group: ai-classifier)              ││
│  │ - 거래 내용(memo) 기반 계정과목 자동 추론                   ││
│  │ - 매입/매출 구분                                            ││
│  │ - 부가세 과세/면세 판단                                     ││
│  └──────────────────┬──────────────────────────────────────────┘│
│                      │                                           │
│                      ▼                                           │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ topic: classified-transactions                              ││
│  │ key: businessId                                             ││
│  │ value: {txId, accountCode, taxType, amount, ...}            ││
│  └──────────────────┬──────────────────────────────────────────┘│
│                      │                                           │
│  Step 3: 분개 생성   ▼                                           │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ 기장 서비스 (Consumer Group: bookkeeping-service)           ││
│  │ - 복식부기 분개 자동 생성                                   ││
│  │ - 차변/대변 엔트리 생성                                     ││
│  └──────────────────┬──────────────────────────────────────────┘│
│                      │                                           │
│                      ▼                                           │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ topic: bookkeeping-events                                   ││
│  │ key: businessId                                             ││
│  │ value: {journalId, debitAccount, creditAccount, amount, ...}││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 토픽 설계 예시

```java
@Configuration
public class TaxKafkaTopicConfig {

    // 원시 거래내역 (높은 처리량)
    @Bean
    public NewTopic rawTransactionsTopic() {
        return TopicBuilder.name("raw-transactions")
            .partitions(12)
            .replicas(3)
            .config(TopicConfig.RETENTION_MS_CONFIG,
                String.valueOf(Duration.ofDays(30).toMillis()))
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
            .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "snappy")
            .build();
    }

    // AI 분류 완료 거래내역
    @Bean
    public NewTopic classifiedTransactionsTopic() {
        return TopicBuilder.name("classified-transactions")
            .partitions(6)
            .replicas(3)
            .config(TopicConfig.RETENTION_MS_CONFIG,
                String.valueOf(Duration.ofDays(90).toMillis()))
            .build();
    }

    // 기장(분개) 이벤트 - Log Compaction
    @Bean
    public NewTopic bookkeepingEventsTopic() {
        return TopicBuilder.name("bookkeeping-events")
            .partitions(6)
            .replicas(3)
            .config(TopicConfig.CLEANUP_POLICY_CONFIG, "compact")
            .config(TopicConfig.MIN_COMPACTION_LAG_MS_CONFIG,
                String.valueOf(Duration.ofHours(1).toMillis()))
            .build();
    }

    // 세금 계산 결과
    @Bean
    public NewTopic taxCalculationResultsTopic() {
        return TopicBuilder.name("tax-calculation-results")
            .partitions(6)
            .replicas(3)
            .config(TopicConfig.RETENTION_MS_CONFIG,
                String.valueOf(Duration.ofDays(365).toMillis()))  // 세금 기록 장기 보관
            .build();
    }

    // 부가세 신고 이벤트
    @Bean
    public NewTopic vatReportEventsTopic() {
        return TopicBuilder.name("vat-report-events")
            .partitions(3)
            .replicas(3)
            .config(TopicConfig.RETENTION_MS_CONFIG,
                String.valueOf(Duration.ofDays(365 * 5).toMillis()))  // 5년 보관 (국세기본법)
            .build();
    }

    // Dead Letter Topics
    @Bean
    public NewTopic rawTransactionsDlt() {
        return TopicBuilder.name("raw-transactions.DLT")
            .partitions(3)
            .replicas(3)
            .build();
    }
}
```

### 이벤트 모델 설계

```java
// 공통 이벤트 베이스
@Getter
@SuperBuilder
@NoArgsConstructor
public abstract class TaxDomainEvent {
    private String eventId;
    private String businessId;      // 사업자등록번호
    private String eventType;
    private LocalDateTime occurredAt;
    private Map<String, String> metadata;
}

// 거래내역 이벤트
@Getter
@SuperBuilder
@NoArgsConstructor
public class TaxTransactionEvent extends TaxDomainEvent {
    private String transactionId;
    private TransactionType type;       // INCOME, EXPENSE
    private BigDecimal amount;
    private BigDecimal vatAmount;       // 부가세
    private BigDecimal supplyAmount;    // 공급가액
    private String counterpartyName;    // 거래처명
    private String counterpartyBizNo;  // 거래처 사업자번호
    private String description;
    private LocalDate transactionDate;
    private TaxClassification taxClass; // TAXABLE, TAX_FREE, ZERO_RATED
}

public enum TransactionType {
    INCOME,          // 매출
    EXPENSE,         // 매입
    TAX_PAYMENT,     // 세금 납부
    SALARY,          // 급여
    DEPRECIATION     // 감가상각
}

public enum TaxClassification {
    TAXABLE,         // 과세
    TAX_FREE,        // 면세
    ZERO_RATED       // 영세율
}

// 기장(분개) 이벤트
@Getter
@SuperBuilder
@NoArgsConstructor
public class BookkeepingEvent extends TaxDomainEvent {
    private String journalEntryId;
    private String debitAccountCode;    // 차변 계정코드
    private String debitAccountName;    // 차변 계정명
    private String creditAccountCode;   // 대변 계정코드
    private String creditAccountName;   // 대변 계정명
    private BigDecimal amount;
    private String description;
    private LocalDate bookkeepingDate;
    private JournalStatus status;       // DRAFT, CONFIRMED, CANCELLED
}

// 세금 계산 결과 이벤트
@Getter
@SuperBuilder
@NoArgsConstructor
public class TaxCalculationResultEvent extends TaxDomainEvent {
    private String calculationId;
    private TaxType taxType;            // VAT, INCOME_TAX, CORPORATE_TAX
    private String taxPeriod;           // "2025-01" (부가세 1기)
    private BigDecimal totalSales;      // 총 매출
    private BigDecimal totalPurchases;  // 총 매입
    private BigDecimal taxableAmount;   // 과세표준
    private BigDecimal taxAmount;       // 세액
    private BigDecimal deductibleVat;   // 매입세액 공제
    private BigDecimal payableAmount;   // 납부세액
}
```

---

## 운영 및 모니터링

### Kafka Lag 모니터링

```
┌──────────────────────────────────────────────────────────────────┐
│                    Consumer Lag 개념                              │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Partition 0:                                                    │
│  ┌──┬──┬──┬──┬──┬──┬──┬──┬──┬──┬──┬──┬──┬──┬──┐               │
│  │ 0│ 1│ 2│ 3│ 4│ 5│ 6│ 7│ 8│ 9│10│11│12│13│14│               │
│  └──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┘               │
│                          ▲                    ▲                   │
│                     Consumer              Latest                 │
│                     Offset=6             Offset=14                │
│                          │                    │                   │
│                          ├────── Lag = 8 ─────┤                  │
│                                                                   │
│  Lag = Latest Offset - Consumer Offset                           │
│  → Lag이 계속 증가하면 Consumer가 처리 속도를 따라가지 못하는 것 │
│                                                                   │
│  알림 기준 (예시):                                                │
│  - Lag > 1,000  → WARNING                                        │
│  - Lag > 10,000 → CRITICAL                                       │
│  - Lag 증가 추세 5분 이상 지속 → ALERT                           │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

```java
// Kafka Lag 모니터링 (Micrometer 연동)
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaLagMonitor {

    private final KafkaAdmin kafkaAdmin;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedRate = 30000)  // 30초마다
    public void monitorLag() {
        try (AdminClient adminClient = AdminClient.create(
                kafkaAdmin.getConfigurationProperties())) {

            // Consumer Group 목록 조회
            List<String> groupIds = adminClient.listConsumerGroups().all().get()
                .stream()
                .map(ConsumerGroupListing::groupId)
                .toList();

            for (String groupId : groupIds) {
                Map<TopicPartition, OffsetAndMetadata> offsets =
                    adminClient.listConsumerGroupOffsets(groupId)
                        .partitionsToOffsetAndMetadata().get();

                // 각 파티션의 최신 offset 조회
                Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets =
                    adminClient.listOffsets(
                        offsets.keySet().stream()
                            .collect(Collectors.toMap(
                                tp -> tp,
                                tp -> OffsetSpec.latest()
                            ))
                    ).all().get();

                // Lag 계산
                long totalLag = 0;
                for (var entry : offsets.entrySet()) {
                    TopicPartition tp = entry.getKey();
                    long consumerOffset = entry.getValue().offset();
                    long latestOffset = endOffsets.get(tp).offset();
                    long lag = latestOffset - consumerOffset;
                    totalLag += lag;

                    // 파티션별 Lag 메트릭
                    meterRegistry.gauge("kafka.consumer.lag",
                        Tags.of(
                            "group", groupId,
                            "topic", tp.topic(),
                            "partition", String.valueOf(tp.partition())
                        ),
                        lag
                    );
                }

                // 그룹 전체 Lag 메트릭
                meterRegistry.gauge("kafka.consumer.lag.total",
                    Tags.of("group", groupId),
                    totalLag
                );

                if (totalLag > 10000) {
                    log.warn("Consumer Lag 위험 - group: {}, totalLag: {}",
                        groupId, totalLag);
                }
            }
        } catch (Exception e) {
            log.error("Kafka Lag 모니터링 실패", e);
        }
    }
}
```

### Consumer Group 관리

```bash
# Consumer Group 목록 조회
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list

# Consumer Group 상세 정보 (Lag 확인)
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --group tax-service-group --describe

# 출력 예시:
# GROUP             TOPIC             PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
# tax-service-group tax-transactions  0          1000            1050            50
# tax-service-group tax-transactions  1          2000            2000            0
# tax-service-group tax-transactions  2          1500            1600            100

# Consumer Group offset 리셋 (장애 복구 시)
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --group tax-service-group --topic tax-transactions \
  --reset-offsets --to-earliest --execute

# 특정 시간으로 offset 리셋
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --group tax-service-group --topic tax-transactions \
  --reset-offsets --to-datetime 2025-02-01T00:00:00.000 --execute

# Consumer Group 삭제 (모든 Consumer 종료 후)
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --group old-service-group --delete
```

### 토픽 관리

#### 파티션 수 결정 가이드

```
┌──────────────────────────────────────────────────────────────────┐
│              파티션 수 결정 가이드                                 │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  공식: Partitions = max(Tp, Tc)                                  │
│  - Tp = 목표 처리량 / 파티션당 Producer 처리량                   │
│  - Tc = 목표 처리량 / 파티션당 Consumer 처리량                   │
│                                                                   │
│  예시: 세무 거래내역 처리                                        │
│  - 목표: 초당 10,000건                                           │
│  - Producer: 파티션당 5,000건/초 가능                            │
│  - Consumer: 파티션당 2,000건/초 가능 (DB 쓰기 포함)             │
│  - Tp = 10,000 / 5,000 = 2                                      │
│  - Tc = 10,000 / 2,000 = 5                                      │
│  - → 최소 5개 파티션 필요 (여유분 포함 6~12 권장)                │
│                                                                   │
│  주의사항:                                                       │
│  - 파티션은 늘릴 수 있지만 줄일 수 없음                          │
│  - 파티션 수 변경 시 키 기반 라우팅 깨짐                         │
│  - 너무 많은 파티션: 리더 선출 지연, 메모리 증가                 │
│  - 일반 권장: 브로커당 4,000개 이하                              │
│                                                                   │
│  도메인별 권장 파티션 수:                                        │
│  ┌────────────────────────────┬────────────┐                    │
│  │ 토픽                       │ 파티션 수  │                    │
│  ├────────────────────────────┼────────────┤                    │
│  │ raw-transactions          │ 12         │                    │
│  │ classified-transactions   │ 6          │                    │
│  │ bookkeeping-events        │ 6          │                    │
│  │ tax-calculation-results   │ 6          │                    │
│  │ vat-report-events         │ 3          │                    │
│  │ notification-events       │ 3          │                    │
│  └────────────────────────────┴────────────┘                    │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

#### 리텐션 정책

```java
// 리텐션 정책 설정
@Configuration
public class KafkaRetentionConfig {

    // 시간 기반 리텐션
    @Bean
    public NewTopic timeBasedRetention() {
        return TopicBuilder.name("tax-transactions")
            .config(TopicConfig.RETENTION_MS_CONFIG,
                String.valueOf(Duration.ofDays(30).toMillis()))    // 30일 보관
            .config(TopicConfig.RETENTION_BYTES_CONFIG,
                String.valueOf(10L * 1024 * 1024 * 1024))         // 10GB 제한
            .config(TopicConfig.CLEANUP_POLICY_CONFIG, "delete")  // 만료 시 삭제
            .build();
    }

    // Log Compaction (최신 값만 유지)
    @Bean
    public NewTopic compactedTopic() {
        return TopicBuilder.name("business-profiles")
            .config(TopicConfig.CLEANUP_POLICY_CONFIG, "compact")
            .config(TopicConfig.MIN_COMPACTION_LAG_MS_CONFIG,
                String.valueOf(Duration.ofHours(1).toMillis()))
            .config(TopicConfig.DELETE_RETENTION_MS_CONFIG,
                String.valueOf(Duration.ofDays(1).toMillis()))
            .build();
        // key별 최신 레코드만 보관
        // 사업자 프로필 같은 상태 데이터에 적합
    }

    // 복합 정책 (compact + delete)
    @Bean
    public NewTopic compactDeleteTopic() {
        return TopicBuilder.name("bookkeeping-snapshots")
            .config(TopicConfig.CLEANUP_POLICY_CONFIG, "compact,delete")
            .config(TopicConfig.RETENTION_MS_CONFIG,
                String.valueOf(Duration.ofDays(365).toMillis()))
            .build();
        // 1년 이내: compact (최신 값만 유지)
        // 1년 이후: delete (완전 삭제)
    }
}
```

### Kafka 클러스터 모니터링 대시보드 지표

```
┌──────────────────────────────────────────────────────────────────┐
│              핵심 모니터링 지표                                    │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  [Broker 지표]                                                   │
│  - UnderReplicatedPartitions    : ISR에서 빠진 파티션 수         │
│  - ActiveControllerCount        : 1이어야 정상                   │
│  - OfflinePartitionsCount       : 0이어야 정상                   │
│  - RequestHandlerAvgIdlePercent : 50% 이상 유지 권장             │
│  - NetworkProcessorAvgIdlePercent: 50% 이상 유지 권장            │
│                                                                   │
│  [Producer 지표]                                                 │
│  - record-send-rate             : 초당 전송 레코드 수            │
│  - record-error-rate            : 초당 에러 수 (0에 가까워야)    │
│  - request-latency-avg          : 평균 요청 지연시간             │
│  - batch-size-avg               : 평균 배치 크기                 │
│                                                                   │
│  [Consumer 지표]                                                 │
│  - records-consumed-rate        : 초당 소비 레코드 수            │
│  - records-lag-max              : 최대 Lag                       │
│  - commit-rate                  : 초당 커밋 횟수                 │
│  - rebalance-rate-and-time      : 리밸런싱 빈도 및 시간          │
│                                                                   │
│  [알림 설정 예시]                                                │
│  - Consumer Lag > 10,000       → CRITICAL                       │
│  - UnderReplicatedPartitions > 0 → WARNING                      │
│  - OfflinePartitionsCount > 0   → CRITICAL                       │
│  - RequestHandler Idle < 30%    → WARNING                        │
│  - Disk 사용량 > 80%            → WARNING                        │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

```java
// Spring Boot Actuator + Micrometer로 Kafka 메트릭 노출
@Configuration
public class KafkaMetricsConfig {

    // Kafka 기본 메트릭은 Spring Boot가 자동 수집
    // application.yml에서 설정
}
```

```yaml
# Kafka 메트릭 노출 설정
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    tags:
      application: tax-service
    export:
      prometheus:
        enabled: true

# Prometheus에서 수집할 주요 메트릭:
# kafka_consumer_records_lag_max
# kafka_consumer_records_consumed_total
# kafka_producer_record_send_total
# kafka_producer_record_error_total
```

---

## 핵심 정리

### 핵심 개념 요약

| 개념 | 설명 | 핵심 포인트 |
|------|------|------------|
| Broker | Kafka 서버 인스턴스 | 3대 이상 클러스터 구성 권장 |
| Topic | 논리적 메시지 분류 단위 | 도메인 이벤트별 토픽 분리 |
| Partition | Topic의 물리적 분할 | 파티션 내 순서 보장, 병렬 처리 단위 |
| Consumer Group | 논리적 Consumer 묶음 | 그룹 내 파티션 분배, 그룹 간 독립 소비 |
| Offset | 파티션 내 메시지 위치 | 자동/수동 커밋, 리플레이 가능 |
| ISR | 동기화된 Replica 집합 | min.insync.replicas + acks=all |
| KRaft | Zookeeper 대체 모드 | Kafka 3.3+ 프로덕션 지원 |

### 설정 체크리스트

```
Producer 설정:
□ acks=all (데이터 안전성)
□ enable.idempotence=true (중복 방지)
□ retries=3 이상
□ linger.ms 조정 (배치 효율)
□ compression.type=snappy 또는 lz4
□ max.in.flight.requests.per.connection ≤ 5

Consumer 설정:
□ enable.auto.commit=false (수동 커밋)
□ auto.offset.reset=earliest (신규 그룹)
□ max.poll.records 적절한 값 설정
□ max.poll.interval.ms (처리 시간 고려)
□ session.timeout.ms / heartbeat.interval.ms
□ partition.assignment.strategy=CooperativeStickyAssignor

토픽 설정:
□ replication.factor=3 (운영 환경)
□ min.insync.replicas=2
□ 파티션 수 결정 (처리량 기반)
□ retention.ms 설정 (도메인 요구사항)
□ cleanup.policy (delete 또는 compact)

운영:
□ Consumer Lag 모니터링
□ DLT(Dead Letter Topic) 설정
□ 알림 설정 (Lag, ISR 이탈 등)
□ offset 리셋 절차 문서화
□ 파티션 키 설계 (순서 보장 대상)
```

### SQS vs Kafka 선택 결정 트리

```
메시지 시스템 선택이 필요한가?
│
├─ 단순 작업 큐인가? ──── Yes ──► SQS
│
├─ 여러 서비스가 같은 이벤트를 소비? ──── Yes ──► Kafka
│
├─ 이벤트 리플레이 필요? ──── Yes ──► Kafka
│
├─ 처리량 > 10만 건/초? ──── Yes ──► Kafka
│
├─ 서버리스/운영 최소화? ──── Yes ──► SQS
│
├─ AWS 외 환경? ──── Yes ──► Kafka
│
└─ 이벤트 소싱/CQRS? ──── Yes ──► Kafka
```

---

## 면접 대비 핵심 질문

### Q1: Kafka의 핵심 아키텍처를 설명해주세요.

**A:** Kafka는 분산 이벤트 스트리밍 플랫폼으로, Broker 클러스터가 Topic을 호스팅합니다. Topic은 여러 Partition으로 분할되어 병렬 처리가 가능하며, 각 파티션 내에서 메시지 순서가 보장됩니다. Producer가 메시지를 발행하면 Partition Key에 따라 특정 파티션에 기록되고, Consumer Group의 각 Consumer가 할당된 파티션에서 메시지를 Pull 방식으로 가져갑니다. Replication Factor를 통해 데이터 복제가 이루어지며, ISR(In-Sync Replicas)에 속한 Replica만이 Leader 장애 시 새 Leader로 선출됩니다.

### Q2: Kafka에서 메시지 순서 보장을 어떻게 하나요?

**A:** Kafka는 파티션 내에서만 순서를 보장합니다. 따라서 순서가 중요한 메시지는 같은 Partition Key를 사용해야 합니다. 예를 들어, 세무 서비스에서 같은 사업자의 거래내역 순서를 보장하려면 사업자번호(businessId)를 Partition Key로 사용합니다. 추가로 Idempotent Producer(enable.idempotence=true)를 활성화하면 max.in.flight.requests.per.connection이 5 이하일 때 네트워크 재시도로 인한 순서 역전도 방지됩니다.

### Q3: At-least-once와 Exactly-once의 차이, 그리고 실무에서 어떻게 선택하나요?

**A:** At-least-once는 메시지가 최소 한 번 전달되지만 중복 가능성이 있고, Exactly-once는 정확히 한 번만 전달됩니다. 실무에서는 대부분 At-least-once + Consumer 측 멱등성 처리가 가장 현실적입니다. 처리된 메시지 ID를 DB에 기록하고 중복 체크하는 방식입니다. Exactly-once는 Kafka Transactions API를 사용하지만 성능 오버헤드가 있어, 금융이나 세금 계산처럼 정확성이 절대적으로 중요한 경우에만 사용합니다.

### Q4: Consumer Lag이 계속 증가하면 어떻게 대응하나요?

**A:** 단계별 대응 전략이 있습니다. 먼저 Consumer의 처리 로직에 병목이 있는지 확인합니다 (DB 쿼리, 외부 API 호출 등). 다음으로 Consumer 인스턴스를 파티션 수까지 스케일 아웃합니다. 그래도 부족하면 배치 처리(BatchListener)로 전환하거나, max.poll.records를 조정합니다. 근본적으로는 파티션 수를 늘려 병렬도를 높이되, 이때 기존 키 라우팅이 깨질 수 있으므로 신중해야 합니다. 일시적 급증이라면 Consumer의 max.poll.interval.ms를 늘려 타임아웃을 방지합니다.

### Q5: SQS 대신 Kafka를 선택해야 하는 경우는?

**A:** zaritalk에서는 SQS로 푸시 알림과 비동기 작업을 처리했는데, 단순 작업 큐에는 적합했습니다. 하지만 세무 서비스처럼 여러 서비스가 같은 거래내역 이벤트를 독립적으로 소비해야 하거나(기장 서비스, 세금 계산 서비스, 대시보드 등), 이벤트 히스토리 리플레이가 필요하거나, 높은 처리량과 순서 보장이 동시에 필요한 경우에는 Kafka가 적합합니다. SQS는 메시지 소비 후 삭제되므로 멀티 컨슈머 패턴이 불가능하고, FIFO 큐는 300 TPS 제한이 있습니다.

### Q6: Kafka의 Replication과 ISR에 대해 설명해주세요.

**A:** Kafka는 각 파티션을 설정된 Replication Factor만큼 복제합니다. 하나의 Replica가 Leader로 선출되어 읽기/쓰기를 처리하고, 나머지 Follower가 Leader의 데이터를 복제합니다. ISR(In-Sync Replicas)은 Leader와 동기화 상태를 유지하는 Replica 집합입니다. acks=all 설정 시 ISR의 모든 Replica가 기록을 확인해야 Producer에 응답하므로 데이터 손실을 방지합니다. min.insync.replicas=2로 설정하면 ISR이 2개 미만일 때 쓰기를 거부하여 데이터 안전성을 보장합니다. 운영 환경에서는 replication.factor=3, min.insync.replicas=2, acks=all 조합을 권장합니다.

### Q7: Consumer Rebalancing이란 무엇이고 어떻게 최적화하나요?

**A:** Consumer Rebalancing은 Consumer Group 내 파티션 할당을 재조정하는 과정입니다. Consumer 추가/제거, 장애, 토픽 파티션 변경 시 트리거됩니다. 기본 전략(Eager Rebalancing)은 모든 파티션 할당을 해제하고 재배분하므로 전체 소비가 중단(Stop-the-World)됩니다. CooperativeStickyAssignor를 사용하면 점진적 재할당(Incremental Rebalancing)이 가능하여 변경이 필요한 파티션만 이동시킵니다. 추가로 session.timeout.ms와 heartbeat.interval.ms를 적절히 설정하고, max.poll.interval.ms를 처리 시간에 맞게 설정하여 불필요한 리밸런싱을 방지해야 합니다.

### Q8: Kafka를 세무 도메인에 적용할 때 토픽 설계 전략은?

**A:** 세무 도메인에서는 이벤트의 성격에 따라 토픽을 분리합니다. raw-transactions(원시 거래내역, 12 파티션), classified-transactions(AI 분류 완료, 6 파티션), bookkeeping-events(기장 이벤트, 6 파티션), tax-calculation-results(세금 계산 결과, 6 파티션) 등으로 나눕니다. 사업자번호를 Partition Key로 사용하여 같은 사업자의 이벤트 순서를 보장합니다. 리텐션은 국세기본법의 5년 보관 의무를 고려하여 세금 관련 토픽은 장기 보관하고, 중간 처리 토픽은 30~90일로 설정합니다. 또한 각 토픽에 DLT를 설정하여 처리 실패 메시지를 별도 관리합니다.

### Q9: Kafka에서 Exactly-once 처리를 구현하려면?

**A:** Kafka의 Exactly-once는 두 가지 레벨에서 구현됩니다. Producer 레벨에서는 Idempotent Producer(enable.idempotence=true)로 네트워크 재시도 중복을 방지합니다. 전체 파이프라인에서는 Transactional Producer + read_committed isolation level Consumer를 사용합니다. Spring Kafka에서는 KafkaTransactionManager를 설정하고 @Transactional로 감싸면 됩니다. 다만 Kafka-to-Kafka 파이프라인에서만 완전한 Exactly-once가 가능하고, 외부 시스템(DB 등)이 관여하면 Consumer 측에서 별도로 멱등성을 보장해야 합니다. 예를 들어 처리된 이벤트 ID를 DB에 저장하고 비즈니스 로직과 같은 트랜잭션으로 커밋합니다.

### Q10: KRaft 모드가 Zookeeper를 대체하는 이유는?

**A:** Zookeeper 모드에서는 메타데이터(토픽, 파티션, 브로커 정보 등)를 별도의 Zookeeper 클러스터에 저장했기 때문에 운영 복잡도가 높았습니다. 또한 Zookeeper의 성능 한계로 파티션 수가 약 20만 개를 넘기기 어려웠습니다. KRaft 모드는 Raft 합의 알고리즘을 사용하여 Kafka 내부에서 메타데이터를 관리하므로, 단일 시스템으로 운영이 단순해지고 파티션 수 제한이 크게 완화됩니다. Controller 장애 시 리더 선출 속도도 빨라지며, Kafka 시작 시간도 단축됩니다. Kafka 4.0에서 Zookeeper가 완전 제거될 예정이므로, 신규 프로젝트는 KRaft를 사용하는 것이 바람직합니다.

---

*마지막 업데이트: 2026년 02월*
