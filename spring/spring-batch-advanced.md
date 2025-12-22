# Spring Batch 고급 활용 - 비동기, 파티셔닝, 원격 실행

대용량 배치 처리의 성능을 극대화하는 고급 기법을 정리한다.

## 목차

- [멀티스레드 Step](#멀티스레드-step)
- [비동기 ItemProcessor/ItemWriter](#비동기-itemprocessoritemwriter)
- [파티셔닝](#파티셔닝)
- [병렬 Step 실행](#병렬-step-실행)
- [원격 청킹](#원격-청킹)
- [원격 파티셔닝](#원격-파티셔닝)
- [실무 적용 가이드](#실무-적용-가이드)

---

## 멀티스레드 Step

하나의 Step을 여러 스레드로 병렬 처리한다.

```
┌────────────────────────────────────────────────────────────┐
│                    Multi-threaded Step                      │
│                                                             │
│   ┌──────────────────────────────────────────────────┐     │
│   │                  TaskExecutor                      │     │
│   │   ┌─────────┐ ┌─────────┐ ┌─────────┐           │     │
│   │   │Thread 1 │ │Thread 2 │ │Thread 3 │ ...       │     │
│   │   │ R→P→W   │ │ R→P→W   │ │ R→P→W   │           │     │
│   │   └─────────┘ └─────────┘ └─────────┘           │     │
│   └──────────────────────────────────────────────────┘     │
│                           │                                 │
│                    Shared Reader                            │
│               (Thread-safe 필수!)                           │
└────────────────────────────────────────────────────────────┘
```

### 구현 방법

```java
@Bean
public Step multiThreadedStep(JobRepository jobRepository,
                               PlatformTransactionManager transactionManager) {
    return new StepBuilder("multiThreadedStep", jobRepository)
            .<Customer, CustomerDto>chunk(100, transactionManager)
            .reader(synchronizedReader())  // Thread-safe Reader
            .processor(processor())
            .writer(writer())
            .taskExecutor(taskExecutor())
            .throttleLimit(4)  // 동시 실행 스레드 수 제한
            .build();
}

@Bean
public TaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(8);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("batch-thread-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
}
```

### Thread-safe Reader

```java
// SynchronizedItemStreamReader 래핑
@Bean
public SynchronizedItemStreamReader<Customer> synchronizedReader() {
    JdbcPagingItemReader<Customer> reader = new JdbcPagingItemReaderBuilder<Customer>()
            .name("customerReader")
            .dataSource(dataSource)
            .selectClause("SELECT *")
            .fromClause("FROM customers")
            .sortKeys(Map.of("id", Order.ASCENDING))
            .pageSize(100)
            .rowMapper(new BeanPropertyRowMapper<>(Customer.class))
            .saveState(false)  // 멀티스레드에서는 상태 저장 비활성화
            .build();

    SynchronizedItemStreamReader<Customer> synchronizedReader =
            new SynchronizedItemStreamReader<>();
    synchronizedReader.setDelegate(reader);
    return synchronizedReader;
}
```

**주의사항:**
- Reader는 반드시 Thread-safe 해야 함
- `saveState(false)` 설정 필수 (재시작 기능 비활성화)
- 처리 순서가 보장되지 않음

---

## 비동기 ItemProcessor/ItemWriter

Processor와 Writer를 비동기로 실행한다.

```
┌────────────────────────────────────────────────────────────┐
│                  Async Processor/Writer                     │
│                                                             │
│   Reader    AsyncProcessor           AsyncWriter           │
│     │            │                        │                 │
│   item ──▶  Future<Output>  ──▶  Future unwrap ──▶ write   │
│     │       (별도 스레드)                  │                 │
│     │            │                        │                 │
│   chunk        async                   commit              │
│   경계         처리                      시점               │
└────────────────────────────────────────────────────────────┘
```

### AsyncItemProcessor

```java
@Bean
public AsyncItemProcessor<Customer, CustomerDto> asyncProcessor() {
    AsyncItemProcessor<Customer, CustomerDto> asyncProcessor =
            new AsyncItemProcessor<>();
    asyncProcessor.setDelegate(processor());
    asyncProcessor.setTaskExecutor(asyncTaskExecutor());
    return asyncProcessor;
}

@Bean
public ItemProcessor<Customer, CustomerDto> processor() {
    return customer -> {
        // 시간이 오래 걸리는 처리 (외부 API 호출 등)
        CustomerDetails details = externalApi.getDetails(customer.getId());
        return CustomerDto.from(customer, details);
    };
}
```

### AsyncItemWriter

```java
@Bean
public AsyncItemWriter<CustomerDto> asyncWriter() {
    AsyncItemWriter<CustomerDto> asyncWriter = new AsyncItemWriter<>();
    asyncWriter.setDelegate(writer());
    return asyncWriter;
}

@Bean
public Step asyncStep(JobRepository jobRepository,
                      PlatformTransactionManager transactionManager) {
    return new StepBuilder("asyncStep", jobRepository)
            .<Customer, Future<CustomerDto>>chunk(100, transactionManager)
            .reader(reader())
            .processor(asyncProcessor())
            .writer(asyncWriter())
            .build();
}
```

---

## 파티셔닝

데이터를 여러 파티션으로 나눠 병렬 처리한다.

```
┌────────────────────────────────────────────────────────────────┐
│                       Partitioned Step                          │
│                                                                 │
│                     ┌──────────────┐                           │
│                     │ Manager Step │                           │
│                     │ (Partitioner)│                           │
│                     └──────┬───────┘                           │
│            ┌───────────────┼───────────────┐                   │
│            ▼               ▼               ▼                   │
│   ┌─────────────┐ ┌─────────────┐ ┌─────────────┐             │
│   │  Worker 1   │ │  Worker 2   │ │  Worker 3   │             │
│   │ ID: 1-1000  │ │ ID:1001-2000│ │ ID:2001-3000│             │
│   │   R→P→W     │ │   R→P→W     │ │   R→P→W     │             │
│   └─────────────┘ └─────────────┘ └─────────────┘             │
└────────────────────────────────────────────────────────────────┘
```

### Partitioner 구현

```java
@Component
public class CustomerPartitioner implements Partitioner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        // 전체 ID 범위 조회
        Long minId = jdbcTemplate.queryForObject(
                "SELECT MIN(id) FROM customers", Long.class);
        Long maxId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM customers", Long.class);

        long targetSize = (maxId - minId) / gridSize + 1;

        Map<String, ExecutionContext> partitions = new HashMap<>();

        long start = minId;
        long end = start + targetSize - 1;

        for (int i = 0; i < gridSize; i++) {
            ExecutionContext context = new ExecutionContext();
            context.putLong("minId", start);
            context.putLong("maxId", Math.min(end, maxId));
            context.putString("partitionName", "partition" + i);

            partitions.put("partition" + i, context);

            start += targetSize;
            end += targetSize;
        }

        return partitions;
    }
}
```

### 파티션 Step 설정

```java
@Bean
public Step managerStep(JobRepository jobRepository,
                        CustomerPartitioner partitioner,
                        Step workerStep) {
    return new StepBuilder("managerStep", jobRepository)
            .partitioner("workerStep", partitioner)
            .step(workerStep)
            .gridSize(4)  // 파티션 수
            .taskExecutor(partitionTaskExecutor())
            .build();
}

@Bean
public Step workerStep(JobRepository jobRepository,
                       PlatformTransactionManager transactionManager) {
    return new StepBuilder("workerStep", jobRepository)
            .<Customer, CustomerDto>chunk(100, transactionManager)
            .reader(partitionedReader(null, null))
            .processor(processor())
            .writer(writer())
            .build();
}

@Bean
@StepScope
public JdbcPagingItemReader<Customer> partitionedReader(
        @Value("#{stepExecutionContext['minId']}") Long minId,
        @Value("#{stepExecutionContext['maxId']}") Long maxId) {

    Map<String, Order> sortKeys = new HashMap<>();
    sortKeys.put("id", Order.ASCENDING);

    return new JdbcPagingItemReaderBuilder<Customer>()
            .name("partitionedReader")
            .dataSource(dataSource)
            .selectClause("SELECT *")
            .fromClause("FROM customers")
            .whereClause("WHERE id >= :minId AND id <= :maxId")
            .parameterValues(Map.of("minId", minId, "maxId", maxId))
            .sortKeys(sortKeys)
            .pageSize(100)
            .rowMapper(new BeanPropertyRowMapper<>(Customer.class))
            .build();
}

@Bean
public TaskExecutor partitionTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(4);
    executor.setThreadNamePrefix("partition-");
    executor.initialize();
    return executor;
}
```

### ColumnRangePartitioner (범용)

```java
@Component
public class ColumnRangePartitioner implements Partitioner {

    private final JdbcTemplate jdbcTemplate;
    private String table;
    private String column;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> result = new HashMap<>();

        Long min = jdbcTemplate.queryForObject(
                "SELECT MIN(" + column + ") FROM " + table, Long.class);
        Long max = jdbcTemplate.queryForObject(
                "SELECT MAX(" + column + ") FROM " + table, Long.class);

        long targetSize = (max - min) / gridSize + 1;

        for (int i = 0; i < gridSize; i++) {
            ExecutionContext context = new ExecutionContext();

            long startValue = min + (targetSize * i);
            long endValue = Math.min(startValue + targetSize - 1, max);

            context.putLong("minValue", startValue);
            context.putLong("maxValue", endValue);

            result.put("partition" + i, context);
        }

        return result;
    }

    // setter methods...
}
```

---

## 병렬 Step 실행

독립적인 여러 Step을 동시에 실행한다.

```
┌────────────────────────────────────────────────────────────┐
│                    Parallel Flow                            │
│                                                             │
│                    ┌─────────┐                             │
│                    │  Step1  │                             │
│                    └────┬────┘                             │
│         ┌───────────────┴───────────────┐                  │
│         ▼                               ▼                  │
│   ┌───────────┐                   ┌───────────┐           │
│   │  Step2A   │      parallel     │  Step2B   │           │
│   └─────┬─────┘                   └─────┬─────┘           │
│         └───────────────┬───────────────┘                  │
│                         ▼                                  │
│                    ┌─────────┐                             │
│                    │  Step3  │                             │
│                    └─────────┘                             │
└────────────────────────────────────────────────────────────┘
```

### Split을 사용한 병렬 실행

```java
@Bean
public Job parallelJob(JobRepository jobRepository,
                       Step step1, Step step2A, Step step2B, Step step3) {

    Flow flow2A = new FlowBuilder<SimpleFlow>("flow2A")
            .start(step2A)
            .build();

    Flow flow2B = new FlowBuilder<SimpleFlow>("flow2B")
            .start(step2B)
            .build();

    Flow splitFlow = new FlowBuilder<SimpleFlow>("splitFlow")
            .split(new SimpleAsyncTaskExecutor())
            .add(flow2A, flow2B)
            .build();

    return new JobBuilder("parallelJob", jobRepository)
            .start(step1)
            .next(splitFlow)
            .next(step3)
            .end()
            .build();
}
```

### 여러 Flow 병렬 실행

```java
@Bean
public Job multiFlowParallelJob(JobRepository jobRepository) {
    Flow customerFlow = new FlowBuilder<SimpleFlow>("customerFlow")
            .start(customerStep1())
            .next(customerStep2())
            .build();

    Flow orderFlow = new FlowBuilder<SimpleFlow>("orderFlow")
            .start(orderStep1())
            .next(orderStep2())
            .build();

    Flow productFlow = new FlowBuilder<SimpleFlow>("productFlow")
            .start(productStep())
            .build();

    return new JobBuilder("multiFlowParallelJob", jobRepository)
            .start(initStep())
            .split(taskExecutor())
            .add(customerFlow, orderFlow, productFlow)
            .next(finalStep())
            .end()
            .build();
}
```

---

## 원격 청킹

Chunk 처리를 여러 워커 노드에 분산한다.

```
┌────────────────────────────────────────────────────────────────┐
│                     Remote Chunking                             │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    Manager Node                           │  │
│  │   ┌────────┐         ┌─────────────────────────────┐     │  │
│  │   │ Reader │────────▶│  ChunkMessageChannelItemWriter │    │  │
│  │   └────────┘         └──────────────┬──────────────┘     │  │
│  └──────────────────────────────────────┼────────────────────┘  │
│                                          │ Message Queue        │
│            ┌────────────────┬────────────┴────────────┐        │
│            ▼                ▼                         ▼        │
│  ┌──────────────┐  ┌──────────────┐         ┌──────────────┐  │
│  │  Worker 1    │  │  Worker 2    │   ...   │  Worker N    │  │
│  │ P → W        │  │ P → W        │         │ P → W        │  │
│  └──────────────┘  └──────────────┘         └──────────────┘  │
└────────────────────────────────────────────────────────────────┘
```

### Manager 설정

```java
@Configuration
public class ManagerConfig {

    @Bean
    public Step managerStep(JobRepository jobRepository,
                            PlatformTransactionManager transactionManager) {
        return new StepBuilder("managerStep", jobRepository)
                .<Customer, Customer>chunk(100, transactionManager)
                .reader(reader())
                .writer(chunkMessageChannelItemWriter())
                .build();
    }

    @Bean
    public ChunkMessageChannelItemWriter<Customer> chunkMessageChannelItemWriter() {
        ChunkMessageChannelItemWriter<Customer> writer =
                new ChunkMessageChannelItemWriter<>();
        writer.setMessagingOperations(messagingTemplate());
        writer.setReplyChannel(replies());
        return writer;
    }

    @Bean
    public DirectChannel requests() {
        return new DirectChannel();
    }

    @Bean
    public QueueChannel replies() {
        return new QueueChannel();
    }
}
```

### Worker 설정

```java
@Configuration
public class WorkerConfig {

    @Bean
    public IntegrationFlow workerFlow() {
        return IntegrationFlow
                .from(requests())
                .handle(chunkProcessorChunkHandler())
                .channel(replies())
                .get();
    }

    @Bean
    public ChunkProcessorChunkHandler<Customer> chunkProcessorChunkHandler() {
        SimpleChunkProcessor<Customer, CustomerDto> processor =
                new SimpleChunkProcessor<>(processor(), writer());
        ChunkProcessorChunkHandler<Customer> handler =
                new ChunkProcessorChunkHandler<>();
        handler.setChunkProcessor(processor);
        return handler;
    }
}
```

---

## 원격 파티셔닝

파티션을 여러 워커 노드에 분산한다.

```
┌────────────────────────────────────────────────────────────────┐
│                   Remote Partitioning                           │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    Manager Node                           │  │
│  │   ┌─────────────┐     ┌─────────────────────────┐        │  │
│  │   │ Partitioner │────▶│MessageChannelPartitionHandler│    │  │
│  │   └─────────────┘     └────────────┬────────────┘        │  │
│  └──────────────────────────────────────┼────────────────────┘  │
│                                          │                      │
│                                   Partition Messages            │
│            ┌────────────────┬────────────┴────────────┐        │
│            ▼                ▼                         ▼        │
│  ┌──────────────┐  ┌──────────────┐         ┌──────────────┐  │
│  │  Worker 1    │  │  Worker 2    │   ...   │  Worker N    │  │
│  │ Full Step    │  │ Full Step    │         │ Full Step    │  │
│  │ (R→P→W)      │  │ (R→P→W)      │         │ (R→P→W)      │  │
│  └──────────────┘  └──────────────┘         └──────────────┘  │
└────────────────────────────────────────────────────────────────┘
```

### Manager 설정

```java
@Configuration
public class ManagerConfig {

    @Bean
    public Step managerStep(JobRepository jobRepository) {
        return new StepBuilder("managerStep", jobRepository)
                .partitioner("workerStep", partitioner())
                .partitionHandler(partitionHandler())
                .build();
    }

    @Bean
    public MessageChannelPartitionHandler partitionHandler() {
        MessageChannelPartitionHandler handler =
                new MessageChannelPartitionHandler();
        handler.setStepName("workerStep");
        handler.setGridSize(4);
        handler.setMessagingOperations(messagingTemplate());
        handler.setReplyChannel(replies());
        return handler;
    }

    @Bean
    @ServiceActivator(inputChannel = "requests")
    public AggregatorFactoryBean aggregator() {
        AggregatorFactoryBean aggregator = new AggregatorFactoryBean();
        aggregator.setProcessorBean(partitionHandler());
        aggregator.setOutputChannel(replies());
        return aggregator;
    }
}
```

### Worker 설정

```java
@Configuration
public class WorkerConfig {

    @Bean
    @ServiceActivator(inputChannel = "requests", outputChannel = "replies")
    public StepExecutionRequestHandler stepExecutionRequestHandler() {
        StepExecutionRequestHandler handler = new StepExecutionRequestHandler();
        handler.setJobExplorer(jobExplorer);
        handler.setStepLocator(stepLocator());
        return handler;
    }

    @Bean
    public BeanFactoryStepLocator stepLocator() {
        return new BeanFactoryStepLocator();
    }

    @Bean
    public Step workerStep(JobRepository jobRepository,
                           PlatformTransactionManager transactionManager) {
        return new StepBuilder("workerStep", jobRepository)
                .<Customer, CustomerDto>chunk(100, transactionManager)
                .reader(partitionedReader(null, null))
                .processor(processor())
                .writer(writer())
                .build();
    }
}
```

---

## 실무 적용 가이드

### 처리 방식 선택 기준

| 방식 | 적합한 상황 | 장점 | 단점 |
|------|------------|------|------|
| **단일 스레드** | 소량 데이터, 순서 중요 | 단순, 디버깅 용이 | 느림 |
| **멀티스레드 Step** | 중간 규모, 순서 무관 | 구현 간단, 확장성 | Reader thread-safe 필요 |
| **비동기 Processor** | 외부 API 호출 많음 | I/O 대기 최소화 | 복잡도 증가 |
| **파티셔닝** | 대용량, 명확한 분할 기준 | 확장성 최고 | 파티셔닝 로직 필요 |
| **원격 청킹** | 처리가 무거운 경우 | 처리 분산 | 인프라 복잡 |
| **원격 파티셔닝** | 초대용량, 분산 환경 | 완전한 분산 | 가장 복잡 |

### 성능 튜닝 체크리스트

```java
@Bean
public Step optimizedStep(JobRepository jobRepository,
                          PlatformTransactionManager transactionManager) {
    return new StepBuilder("optimizedStep", jobRepository)
            .<Customer, CustomerDto>chunk(500, transactionManager)  // 1. Chunk 크기 최적화
            .reader(reader())
            .processor(processor())
            .writer(writer())
            .taskExecutor(taskExecutor())   // 2. 멀티스레드
            .throttleLimit(8)                // 3. 동시성 제한
            .faultTolerant()
            .skip(Exception.class)
            .skipLimit(100)                  // 4. 에러 허용
            .listener(performanceListener()) // 5. 모니터링
            .build();
}
```

### 모니터링 구현

```java
@Component
@Slf4j
public class BatchPerformanceListener implements StepExecutionListener, ChunkListener {

    private final MeterRegistry meterRegistry;
    private long stepStartTime;
    private AtomicLong totalProcessTime = new AtomicLong(0);

    @Override
    public void beforeStep(StepExecution stepExecution) {
        stepStartTime = System.currentTimeMillis();
        log.info("Step 시작: {}", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        long duration = System.currentTimeMillis() - stepStartTime;

        // 메트릭 기록
        meterRegistry.gauge("batch.step.duration",
                Tags.of("step", stepExecution.getStepName()), duration);
        meterRegistry.gauge("batch.step.read.count",
                Tags.of("step", stepExecution.getStepName()),
                stepExecution.getReadCount());
        meterRegistry.gauge("batch.step.write.count",
                Tags.of("step", stepExecution.getStepName()),
                stepExecution.getWriteCount());

        log.info("Step 완료: {} - 읽기: {}, 쓰기: {}, 스킵: {}, 소요시간: {}ms",
                stepExecution.getStepName(),
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getSkipCount(),
                duration);

        // 처리량 계산
        double throughput = stepExecution.getWriteCount() * 1000.0 / duration;
        log.info("처리량: {:.2f} items/sec", throughput);

        return stepExecution.getExitStatus();
    }

    @Override
    public void afterChunk(ChunkContext context) {
        StepExecution stepExecution = context.getStepContext().getStepExecution();
        log.debug("Chunk 완료 - 누적 쓰기: {}", stepExecution.getWriteCount());
    }
}
```

### 재시작 및 복구 전략

```java
@Bean
public Step restartableStep(JobRepository jobRepository,
                            PlatformTransactionManager transactionManager) {
    return new StepBuilder("restartableStep", jobRepository)
            .<Customer, CustomerDto>chunk(100, transactionManager)
            .reader(reader())
            .processor(processor())
            .writer(writer())
            .allowStartIfComplete(false)  // 완료된 Step 재실행 방지
            .startLimit(3)                 // 최대 시작 횟수
            .listener(new StepExecutionListener() {
                @Override
                public void beforeStep(StepExecution stepExecution) {
                    if (stepExecution.getExecutionContext().containsKey("lastProcessedId")) {
                        Long lastId = stepExecution.getExecutionContext()
                                .getLong("lastProcessedId");
                        log.info("이전 실행 지점부터 재시작: ID {}", lastId);
                    }
                }
            })
            .build();
}
```

---

## 관련 문서

- [Spring Batch 기초](./spring-batch-basics.md) - Job, Step, 실행 흐름
- [Spring Batch Chunk 처리](./spring-batch-chunk.md) - ItemReader, ItemProcessor, ItemWriter

*마지막 업데이트: 2024년 12월*
