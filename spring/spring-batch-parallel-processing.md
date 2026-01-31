# Spring Batch 비동기 및 대용량 처리 완벽 가이드

Spring Batch에서 제공하는 모든 병렬/비동기 처리 기법을 총정리한다. Flow, Split, Partition, Remote Chunking, Remote Partitioning 등 대용량 데이터 처리를 위한 모든 기능을 다룬다.

## 목차

- [개요](#개요)
- [Flow를 이용한 조건부 흐름 제어](#flow를-이용한-조건부-흐름-제어)
- [Step 순서 제어와 동기화](#step-순서-제어와-동기화)
- [Split을 이용한 병렬 Flow 실행](#split을-이용한-병렬-flow-실행)
- [Partitioning을 이용한 대용량 처리](#partitioning을-이용한-대용량-처리)
- [Remote Partitioning (원격 파티셔닝)](#remote-partitioning-원격-파티셔닝)
- [Remote Chunking (원격 청킹)](#remote-chunking-원격-청킹)
- [AsyncItemProcessor/Writer](#asyncitemprocessorwriter)
- [Multi-threaded Step](#multi-threaded-step)
- [Repeat, Retry, Skip 정책](#repeat-retry-skip-정책)
- [Job/Step 리스너](#jobstep-리스너)
- [ExecutionContext와 데이터 공유](#executioncontext와-데이터-공유)
- [JobLauncher와 비동기 실행](#joblauncher와-비동기-실행)
- [비교 정리](#비교-정리)
- [주의사항](#주의사항)

## 개요

Spring Batch는 대용량 데이터 처리를 위해 다양한 병렬/비동기 처리 방식을 제공한다:

| 방식 | 특징 | 확장성 | 사용 시점 |
|------|------|--------|----------|
| **Flow + Split** | 여러 Step을 병렬 실행 | 단일 JVM | 독립적인 Step들을 동시에 실행할 때 |
| **Partitioning** | 하나의 Step을 여러 파티션으로 분할 | 단일 JVM | 대용량 데이터를 분할 처리할 때 |
| **Remote Partitioning** | 파티션을 원격 노드에서 실행 | 다중 JVM | 분산 환경에서 처리량 확장 |
| **Remote Chunking** | Chunk 처리를 원격으로 위임 | 다중 JVM | Processor가 병목일 때 |
| **AsyncItemProcessor** | ItemProcessor를 비동기 실행 | 단일 JVM | 처리 로직이 I/O 바운드일 때 |
| **Multi-threaded Step** | Chunk를 멀티스레드로 처리 | 단일 JVM | 단순하게 처리 속도를 높일 때 |

## Flow를 이용한 조건부 흐름 제어

Flow는 여러 Step을 논리적으로 그룹화하고, 조건에 따라 분기 처리할 수 있게 해준다.

### 기본 Flow 정의

```java
@Configuration
@RequiredArgsConstructor
public class FlowJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job flowJob() {
        return new JobBuilder("flowJob", jobRepository)
                .start(flow1())
                .next(step3())
                .end()
                .build();
    }

    @Bean
    public Flow flow1() {
        return new FlowBuilder<SimpleFlow>("flow1")
                .start(step1())
                .next(step2())
                .build();
    }

    @Bean
    public Step step1() {
        return new StepBuilder("step1", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("Step 1 실행");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step step2() {
        return new StepBuilder("step2", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("Step 2 실행");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step step3() {
        return new StepBuilder("step3", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("Step 3 실행");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
```

### 조건부 Flow (on/to 사용)

```java
@Bean
public Job conditionalFlowJob() {
    return new JobBuilder("conditionalFlowJob", jobRepository)
            .start(step1())
                .on("COMPLETED").to(step2())  // step1 성공 시 step2로
                .from(step1())
                .on("FAILED").to(failStep())  // step1 실패 시 failStep으로
                .from(step1())
                .on("*").to(step3())          // 그 외의 경우 step3으로
            .end()
            .build();
}

@Bean
public Step step1() {
    return new StepBuilder("step1", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                // ExitStatus에 따라 분기
                boolean success = doSomething();
                if (!success) {
                    contribution.setExitStatus(ExitStatus.FAILED);
                }
                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
}
```

### 커스텀 ExitStatus를 이용한 분기

```java
@Bean
public Job customExitStatusJob() {
    return new JobBuilder("customExitStatusJob", jobRepository)
            .start(deciderStep())
                .on("ODD").to(oddStep())
                .from(deciderStep())
                .on("EVEN").to(evenStep())
            .end()
            .build();
}

@Bean
public Step deciderStep() {
    return new StepBuilder("deciderStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                int number = (int) (Math.random() * 100);
                System.out.println("생성된 숫자: " + number);

                if (number % 2 == 0) {
                    contribution.setExitStatus(new ExitStatus("EVEN"));
                } else {
                    contribution.setExitStatus(new ExitStatus("ODD"));
                }
                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
}
```

### JobExecutionDecider 사용

```java
@Bean
public Job deciderJob() {
    return new JobBuilder("deciderJob", jobRepository)
            .start(startStep())
            .next(decider())
                .on("WEEKDAY").to(weekdayStep())
                .from(decider())
                .on("WEEKEND").to(weekendStep())
            .end()
            .build();
}

@Bean
public JobExecutionDecider decider() {
    return (jobExecution, stepExecution) -> {
        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();

        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return new FlowExecutionStatus("WEEKEND");
        }
        return new FlowExecutionStatus("WEEKDAY");
    };
}
```

## Step 순서 제어와 동기화

### 앞선 Step 완료 후 실행 보장

```java
@Bean
public Job sequentialJob() {
    return new JobBuilder("sequentialJob", jobRepository)
            .start(step1())
            .next(step2())  // step1 완료 후 실행
            .next(step3())  // step2 완료 후 실행
            .build();
}
```

### 비동기 Step 이후 동기화 (Split 후 Join)

```java
@Bean
public Job splitThenSyncJob() {
    return new JobBuilder("splitThenSyncJob", jobRepository)
            .start(splitFlow())            // 병렬 실행
            .next(afterSplitStep())        // 모든 병렬 Flow 완료 후 실행 (자동 동기화)
            .build();
}

@Bean
public Flow splitFlow() {
    return new FlowBuilder<SimpleFlow>("splitFlow")
            .split(taskExecutor())
            .add(flow1(), flow2(), flow3())
            .build();
}
```

### StepExecutionListener를 통한 완료 확인

```java
@Bean
public Step stepWithListener() {
    return new StepBuilder("stepWithListener", jobRepository)
            .tasklet(myTasklet(), transactionManager)
            .listener(new StepExecutionListener() {
                @Override
                public void beforeStep(StepExecution stepExecution) {
                    // Step 시작 전
                    System.out.println("Step 시작: " + stepExecution.getStepName());
                }

                @Override
                public ExitStatus afterStep(StepExecution stepExecution) {
                    // Step 완료 후 - 다음 Step 실행 전 보장
                    System.out.println("Step 완료: " + stepExecution.getExitStatus());

                    // 결과에 따라 ExitStatus 변경 가능
                    if (stepExecution.getReadCount() == 0) {
                        return new ExitStatus("NO_DATA");
                    }
                    return stepExecution.getExitStatus();
                }
            })
            .build();
}
```

### JobExecutionListener를 통한 전체 흐름 제어

```java
@Bean
public Job jobWithListener() {
    return new JobBuilder("jobWithListener", jobRepository)
            .start(step1())
            .next(step2())
            .listener(new JobExecutionListener() {
                @Override
                public void beforeJob(JobExecution jobExecution) {
                    // Job 시작 전 초기화 작업
                    System.out.println("Job 시작: " + jobExecution.getJobInstance().getJobName());
                }

                @Override
                public void afterJob(JobExecution jobExecution) {
                    // 모든 Step 완료 후 실행
                    if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                        System.out.println("Job 성공적으로 완료");
                    } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
                        System.out.println("Job 실패: " + jobExecution.getAllFailureExceptions());
                    }
                }
            })
            .build();
}
```

## Split을 이용한 병렬 Flow 실행

Split은 여러 Flow를 **동시에 병렬로 실행**할 수 있게 해준다.

### 기본 Split 구성

```java
@Configuration
@RequiredArgsConstructor
public class SplitJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job splitJob() {
        return new JobBuilder("splitJob", jobRepository)
                .start(splitFlow())
                .next(finalStep())  // 모든 병렬 Flow 완료 후 실행
                .build();
    }

    @Bean
    public Flow splitFlow() {
        return new FlowBuilder<SimpleFlow>("splitFlow")
                .split(taskExecutor())  // 병렬 실행을 위한 TaskExecutor
                .add(flow1(), flow2(), flow3())  // 병렬로 실행할 Flow들
                .build();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(6);
        executor.setThreadNamePrefix("split-thread-");
        executor.initialize();
        return executor;
    }

    @Bean
    public Flow flow1() {
        return new FlowBuilder<SimpleFlow>("flow1")
                .start(step1a())
                .next(step1b())
                .build();
    }

    @Bean
    public Flow flow2() {
        return new FlowBuilder<SimpleFlow>("flow2")
                .start(step2a())
                .next(step2b())
                .build();
    }

    @Bean
    public Flow flow3() {
        return new FlowBuilder<SimpleFlow>("flow3")
                .start(step3a())
                .build();
    }

    @Bean
    public Step step1a() {
        return new StepBuilder("step1a", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("[" + Thread.currentThread().getName() + "] Step 1a 실행");
                    Thread.sleep(2000);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    // ... 나머지 Step들도 동일하게 정의
}
```

### 실행 결과 예시

```
[split-thread-1] Step 1a 실행
[split-thread-2] Step 2a 실행
[split-thread-3] Step 3a 실행
[split-thread-1] Step 1b 실행  // flow1의 다음 step
[split-thread-2] Step 2b 실행  // flow2의 다음 step
[main] Final Step 실행         // 모든 flow 완료 후
```

## Partitioning을 이용한 대용량 처리

Partitioning은 **하나의 Step을 여러 파티션으로 분할**하여 병렬 처리한다. Master-Slave 구조로 동작한다.

### Partitioning 구조

```
         ┌──────────────┐
         │  Master Step │
         └──────┬───────┘
                │ Partitioner가 데이터 분할
        ┌───────┼───────┬───────┐
        ▼       ▼       ▼       ▼
   ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
   │Slave 0 │ │Slave 1 │ │Slave 2 │ │Slave 3 │
   └────────┘ └────────┘ └────────┘ └────────┘
```

### Partitioner 구현

```java
public class ColumnRangePartitioner implements Partitioner {

    private final JdbcTemplate jdbcTemplate;
    private final String table;
    private final String column;

    public ColumnRangePartitioner(JdbcTemplate jdbcTemplate, String table, String column) {
        this.jdbcTemplate = jdbcTemplate;
        this.table = table;
        this.column = column;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        // 최소/최대 ID 조회
        Long min = jdbcTemplate.queryForObject(
                "SELECT MIN(" + column + ") FROM " + table, Long.class);
        Long max = jdbcTemplate.queryForObject(
                "SELECT MAX(" + column + ") FROM " + table, Long.class);

        if (min == null || max == null) {
            return Collections.emptyMap();
        }

        long targetSize = (max - min) / gridSize + 1;

        Map<String, ExecutionContext> result = new HashMap<>();
        long start = min;
        long end = start + targetSize - 1;

        for (int i = 0; i < gridSize; i++) {
            ExecutionContext context = new ExecutionContext();
            context.putLong("minId", start);
            context.putLong("maxId", Math.min(end, max));

            result.put("partition" + i, context);

            start = end + 1;
            end = start + targetSize - 1;
        }

        return result;
    }
}
```

### Partition Job 설정

```java
@Configuration
@RequiredArgsConstructor
public class PartitionJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

    @Bean
    public Job partitionJob() {
        return new JobBuilder("partitionJob", jobRepository)
                .start(masterStep())
                .build();
    }

    @Bean
    public Step masterStep() {
        return new StepBuilder("masterStep", jobRepository)
                .partitioner("slaveStep", partitioner())
                .step(slaveStep())
                .gridSize(4)  // 파티션 개수
                .taskExecutor(partitionTaskExecutor())
                .build();
    }

    @Bean
    public Partitioner partitioner() {
        return new ColumnRangePartitioner(
                new JdbcTemplate(dataSource),
                "users",
                "id"
        );
    }

    @Bean
    public Step slaveStep() {
        return new StepBuilder("slaveStep", jobRepository)
                .<User, User>chunk(100, transactionManager)
                .reader(partitionReader(null, null))  // @StepScope로 주입
                .processor(userProcessor())
                .writer(userWriter())
                .build();
    }

    @Bean
    @StepScope
    public JdbcPagingItemReader<User> partitionReader(
            @Value("#{stepExecutionContext['minId']}") Long minId,
            @Value("#{stepExecutionContext['maxId']}") Long maxId) {

        Map<String, Object> params = new HashMap<>();
        params.put("minId", minId);
        params.put("maxId", maxId);

        return new JdbcPagingItemReaderBuilder<User>()
                .name("partitionReader")
                .dataSource(dataSource)
                .selectClause("SELECT id, name, email")
                .fromClause("FROM users")
                .whereClause("WHERE id >= :minId AND id <= :maxId")
                .parameterValues(params)
                .sortKeys(Map.of("id", Order.ASCENDING))
                .rowMapper(new BeanPropertyRowMapper<>(User.class))
                .pageSize(100)
                .build();
    }

    @Bean
    public TaskExecutor partitionTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setThreadNamePrefix("partition-");
        executor.initialize();
        return executor;
    }
}
```

### 다양한 Partitioner 예제

#### 날짜 기반 Partitioner

```java
public class DateRangePartitioner implements Partitioner {

    private final LocalDate startDate;
    private final LocalDate endDate;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> result = new HashMap<>();

        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        long daysPerPartition = totalDays / gridSize;

        LocalDate currentStart = startDate;

        for (int i = 0; i < gridSize; i++) {
            ExecutionContext context = new ExecutionContext();
            LocalDate currentEnd = (i == gridSize - 1)
                    ? endDate
                    : currentStart.plusDays(daysPerPartition - 1);

            context.put("startDate", currentStart.toString());
            context.put("endDate", currentEnd.toString());

            result.put("partition" + i, context);
            currentStart = currentEnd.plusDays(1);
        }

        return result;
    }
}
```

#### 파일 기반 Partitioner

```java
public class MultiFilePartitioner implements Partitioner {

    private final Resource[] resources;

    public MultiFilePartitioner(Resource[] resources) {
        this.resources = resources;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> result = new HashMap<>();

        for (int i = 0; i < resources.length; i++) {
            ExecutionContext context = new ExecutionContext();
            context.putString("fileName", resources[i].getFilename());
            context.put("resource", resources[i]);
            result.put("partition" + i, context);
        }

        return result;
    }
}
```

## Remote Partitioning (원격 파티셔닝)

Remote Partitioning은 파티션된 Step을 **원격 JVM(Worker)에서 실행**한다. Master는 파티션을 분배하고 결과만 수집한다.

### 아키텍처

```
┌─────────────────┐
│   Master JVM    │
│  ┌───────────┐  │
│  │ Partitioner│  │
│  └─────┬─────┘  │
│        │ 메시지 큐로 파티션 전송
└────────┼────────┘
         │
    ┌────┴────┐
    ▼         ▼
┌────────┐ ┌────────┐
│Worker 1│ │Worker 2│
│ Step   │ │ Step   │
│ 실행   │ │ 실행   │
└────────┘ └────────┘
```

### Master 설정 (Spring Integration + RabbitMQ)

```java
@Configuration
@EnableBatchIntegration
public class RemotePartitionMasterConfig {

    @Autowired
    private RemotePartitioningMasterStepBuilderFactory masterStepBuilderFactory;

    @Bean
    public Job remotePartitionJob(JobRepository jobRepository) {
        return new JobBuilder("remotePartitionJob", jobRepository)
                .start(masterStep())
                .build();
    }

    @Bean
    public Step masterStep() {
        return masterStepBuilderFactory.get("masterStep")
                .partitioner("workerStep", partitioner())
                .gridSize(4)
                .outputChannel(outboundRequests())   // Worker로 요청 전송
                .inputChannel(inboundReplies())      // Worker로부터 결과 수신
                .build();
    }

    @Bean
    public Partitioner partitioner() {
        return new ColumnRangePartitioner(jdbcTemplate, "orders", "id");
    }

    // RabbitMQ 채널 설정
    @Bean
    public DirectChannel outboundRequests() {
        return new DirectChannel();
    }

    @Bean
    public QueueChannel inboundReplies() {
        return new QueueChannel();
    }

    @Bean
    public IntegrationFlow outboundFlow(AmqpTemplate amqpTemplate) {
        return IntegrationFlow.from(outboundRequests())
                .handle(Amqp.outboundAdapter(amqpTemplate)
                        .routingKey("partition.requests"))
                .get();
    }

    @Bean
    public IntegrationFlow inboundFlow(ConnectionFactory connectionFactory) {
        return IntegrationFlow.from(Amqp.inboundAdapter(connectionFactory, "partition.replies"))
                .channel(inboundReplies())
                .get();
    }
}
```

### Worker 설정

```java
@Configuration
@EnableBatchIntegration
public class RemotePartitionWorkerConfig {

    @Autowired
    private RemotePartitioningWorkerStepBuilderFactory workerStepBuilderFactory;

    @Bean
    public IntegrationFlow inboundFlow(ConnectionFactory connectionFactory) {
        return IntegrationFlow.from(Amqp.inboundAdapter(connectionFactory, "partition.requests"))
                .channel(inboundRequests())
                .get();
    }

    @Bean
    public DirectChannel inboundRequests() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow outboundFlow(AmqpTemplate amqpTemplate) {
        return IntegrationFlow.from(outboundReplies())
                .handle(Amqp.outboundAdapter(amqpTemplate)
                        .routingKey("partition.replies"))
                .get();
    }

    @Bean
    public DirectChannel outboundReplies() {
        return new DirectChannel();
    }

    @Bean
    public Step workerStep(JobRepository jobRepository,
                           PlatformTransactionManager transactionManager) {
        return workerStepBuilderFactory.get("workerStep")
                .inputChannel(inboundRequests())
                .outputChannel(outboundReplies())
                .stepLocator(stepLocator())
                .build();
    }

    @Bean
    public StepLocator stepLocator() {
        return new BeanFactoryStepLocator();  // Step Bean을 찾아서 실행
    }

    // 실제 실행할 Step 정의
    @Bean
    public Step actualWorkerStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager) {
        return new StepBuilder("actualWorkerStep", jobRepository)
                .<Order, Order>chunk(100, transactionManager)
                .reader(partitionReader(null, null))
                .processor(orderProcessor())
                .writer(orderWriter())
                .build();
    }

    @Bean
    @StepScope
    public JdbcPagingItemReader<Order> partitionReader(
            @Value("#{stepExecutionContext['minId']}") Long minId,
            @Value("#{stepExecutionContext['maxId']}") Long maxId) {
        // 파티션 범위에 해당하는 데이터만 읽기
        // ... 구현
    }
}
```

## Remote Chunking (원격 청킹)

Remote Chunking은 **Processor와 Writer를 원격으로 위임**한다. Reader는 Master에서 실행하고, 처리할 데이터를 Worker로 전송한다.

### 아키텍처

```
┌─────────────────────┐
│     Master JVM      │
│  ┌────────────────┐ │
│  │ ItemReader     │ │
│  │ (데이터 읽기)  │ │
│  └───────┬────────┘ │
│          │ Chunk 전송│
└──────────┼──────────┘
           │
    ┌──────┴──────┐
    ▼             ▼
┌──────────┐ ┌──────────┐
│ Worker 1 │ │ Worker 2 │
│Processor │ │Processor │
│+ Writer  │ │+ Writer  │
└──────────┘ └──────────┘
```

### Master 설정

```java
@Configuration
@EnableBatchIntegration
public class RemoteChunkingMasterConfig {

    @Autowired
    private RemoteChunkingMasterStepBuilderFactory masterStepBuilderFactory;

    @Bean
    public Job remoteChunkingJob(JobRepository jobRepository) {
        return new JobBuilder("remoteChunkingJob", jobRepository)
                .start(masterStep())
                .build();
    }

    @Bean
    public Step masterStep() {
        return masterStepBuilderFactory.get("masterStep")
                .<Order, Order>chunk(100)
                .reader(orderReader())           // Master에서 읽기만 수행
                .outputChannel(outboundRequests()) // Chunk를 Worker로 전송
                .inputChannel(inboundReplies())    // 처리 결과 수신
                .build();
    }

    @Bean
    public JdbcCursorItemReader<Order> orderReader() {
        return new JdbcCursorItemReaderBuilder<Order>()
                .name("orderReader")
                .dataSource(dataSource)
                .sql("SELECT * FROM orders WHERE status = 'PENDING'")
                .rowMapper(new BeanPropertyRowMapper<>(Order.class))
                .build();
    }

    // 메시지 채널 설정 (RabbitMQ/Kafka)
    @Bean
    public DirectChannel outboundRequests() {
        return new DirectChannel();
    }

    @Bean
    public QueueChannel inboundReplies() {
        return new QueueChannel();
    }
}
```

### Worker 설정

```java
@Configuration
@EnableBatchIntegration
public class RemoteChunkingWorkerConfig {

    @Autowired
    private RemoteChunkingWorkerBuilder<Order, Order> workerBuilder;

    @Bean
    public IntegrationFlow workerFlow() {
        return workerBuilder
                .inputChannel(inboundRequests())
                .outputChannel(outboundReplies())
                .itemProcessor(orderProcessor())
                .itemWriter(orderWriter())
                .build();
    }

    @Bean
    public ItemProcessor<Order, Order> orderProcessor() {
        return order -> {
            // 무거운 처리 로직 (외부 API 호출 등)
            order.setStatus("PROCESSED");
            order.setProcessedAt(LocalDateTime.now());
            return order;
        };
    }

    @Bean
    public ItemWriter<Order> orderWriter() {
        return new JdbcBatchItemWriterBuilder<Order>()
                .dataSource(dataSource)
                .sql("UPDATE orders SET status = :status, processed_at = :processedAt WHERE id = :id")
                .beanMapped()
                .build();
    }
}
```

### Remote Partitioning vs Remote Chunking 비교

| 구분 | Remote Partitioning | Remote Chunking |
|------|---------------------|-----------------|
| **데이터 위치** | Worker 로컬 데이터 | Master에서 읽어서 전송 |
| **네트워크 부하** | 낮음 (메타데이터만 전송) | 높음 (실제 데이터 전송) |
| **Worker 역할** | Read + Process + Write | Process + Write |
| **적합한 경우** | Worker가 데이터에 가까울 때 | Processor가 병목일 때 |
| **구현 복잡도** | 낮음 | 높음 |

## AsyncItemProcessor/Writer

ItemProcessor의 처리를 비동기로 실행하여 I/O 바운드 작업의 성능을 향상시킨다.

### AsyncItemProcessor 설정

```java
@Configuration
@RequiredArgsConstructor
public class AsyncJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job asyncJob() {
        return new JobBuilder("asyncJob", jobRepository)
                .start(asyncStep())
                .build();
    }

    @Bean
    public Step asyncStep() {
        return new StepBuilder("asyncStep", jobRepository)
                .<User, Future<User>>chunk(100, transactionManager)
                .reader(userReader())
                .processor(asyncProcessor())
                .writer(asyncWriter())
                .build();
    }

    @Bean
    public AsyncItemProcessor<User, User> asyncProcessor() {
        AsyncItemProcessor<User, User> processor = new AsyncItemProcessor<>();
        processor.setDelegate(userProcessor());  // 실제 처리 로직
        processor.setTaskExecutor(asyncTaskExecutor());
        return processor;
    }

    @Bean
    public ItemProcessor<User, User> userProcessor() {
        return user -> {
            // 외부 API 호출 등 I/O 바운드 작업
            Thread.sleep(100);  // 외부 호출 시뮬레이션
            user.setProcessedAt(LocalDateTime.now());
            return user;
        };
    }

    @Bean
    public AsyncItemWriter<User> asyncWriter() {
        AsyncItemWriter<User> writer = new AsyncItemWriter<>();
        writer.setDelegate(userWriter());  // 실제 저장 로직
        return writer;
    }

    @Bean
    public ItemWriter<User> userWriter() {
        return users -> {
            for (User user : users) {
                System.out.println("저장: " + user.getName());
            }
        };
    }

    @Bean
    public TaskExecutor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
```

### 처리 흐름

```
Reader → AsyncProcessor(비동기 실행) → Future<결과> → AsyncWriter(Future에서 결과 추출) → Writer
```

## Multi-threaded Step

Chunk 처리를 여러 스레드가 병렬로 실행한다.

### Multi-threaded Step 설정

```java
@Bean
public Step multiThreadedStep() {
    return new StepBuilder("multiThreadedStep", jobRepository)
            .<User, User>chunk(100, transactionManager)
            .reader(synchronizedReader())  // Thread-safe reader 필요!
            .processor(userProcessor())
            .writer(userWriter())
            .taskExecutor(multiThreadTaskExecutor())
            .build();
}

@Bean
@StepScope
public SynchronizedItemStreamReader<User> synchronizedReader() {
    JdbcCursorItemReader<User> reader = new JdbcCursorItemReaderBuilder<User>()
            .name("cursorReader")
            .dataSource(dataSource)
            .sql("SELECT id, name, email FROM users")
            .rowMapper(new BeanPropertyRowMapper<>(User.class))
            .build();

    // Thread-safe wrapper
    SynchronizedItemStreamReader<User> synchronizedReader = new SynchronizedItemStreamReader<>();
    synchronizedReader.setDelegate(reader);
    return synchronizedReader;
}

@Bean
public TaskExecutor multiThreadTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(8);
    executor.setThreadNamePrefix("multi-thread-");
    executor.initialize();
    return executor;
}
```

### Thread-safe Reader 주의사항

```java
// JdbcPagingItemReader는 기본적으로 thread-safe
@Bean
@StepScope
public JdbcPagingItemReader<User> pagingReader() {
    return new JdbcPagingItemReaderBuilder<User>()
            .name("pagingReader")
            .dataSource(dataSource)
            .selectClause("SELECT id, name, email")
            .fromClause("FROM users")
            .sortKeys(Map.of("id", Order.ASCENDING))
            .rowMapper(new BeanPropertyRowMapper<>(User.class))
            .pageSize(100)
            .saveState(false)  // Multi-thread 환경에서는 상태 저장 비활성화
            .build();
}
```

## Repeat, Retry, Skip 정책

### Retry (재시도) 설정

```java
@Bean
public Step retryStep() {
    return new StepBuilder("retryStep", jobRepository)
            .<User, User>chunk(100, transactionManager)
            .reader(userReader())
            .processor(userProcessor())
            .writer(userWriter())
            .faultTolerant()
            .retryLimit(3)  // 최대 3번 재시도
            .retry(TransientDataAccessException.class)  // 재시도할 예외
            .retry(DeadlockLoserDataAccessException.class)
            .noRetry(ValidationException.class)  // 재시도하지 않을 예외
            .build();
}
```

### Skip (건너뛰기) 설정

```java
@Bean
public Step skipStep() {
    return new StepBuilder("skipStep", jobRepository)
            .<User, User>chunk(100, transactionManager)
            .reader(userReader())
            .processor(userProcessor())
            .writer(userWriter())
            .faultTolerant()
            .skipLimit(10)  // 최대 10개까지 건너뛰기 허용
            .skip(FlatFileParseException.class)
            .skip(ValidationException.class)
            .noSkip(FileNotFoundException.class)  // 건너뛰지 않을 예외
            .build();
}
```

### RetryTemplate을 이용한 세밀한 제어

```java
@Bean
public Step advancedRetryStep() {
    return new StepBuilder("advancedRetryStep", jobRepository)
            .<User, User>chunk(100, transactionManager)
            .reader(userReader())
            .processor(userProcessor())
            .writer(userWriter())
            .faultTolerant()
            .retryPolicy(retryPolicy())
            .backOffPolicy(backOffPolicy())
            .build();
}

@Bean
public RetryPolicy retryPolicy() {
    Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
    retryableExceptions.put(TransientDataAccessException.class, true);
    retryableExceptions.put(DeadlockLoserDataAccessException.class, true);

    SimpleRetryPolicy policy = new SimpleRetryPolicy(3, retryableExceptions, true);
    return policy;
}

@Bean
public BackOffPolicy backOffPolicy() {
    ExponentialBackOffPolicy policy = new ExponentialBackOffPolicy();
    policy.setInitialInterval(1000);  // 1초
    policy.setMultiplier(2.0);        // 2배씩 증가
    policy.setMaxInterval(10000);     // 최대 10초
    return policy;
}
```

### SkipListener를 통한 건너뛴 항목 처리

```java
@Bean
public Step skipWithListenerStep() {
    return new StepBuilder("skipWithListenerStep", jobRepository)
            .<User, User>chunk(100, transactionManager)
            .reader(userReader())
            .processor(userProcessor())
            .writer(userWriter())
            .faultTolerant()
            .skipLimit(100)
            .skip(Exception.class)
            .listener(skipListener())
            .build();
}

@Bean
public SkipListener<User, User> skipListener() {
    return new SkipListener<>() {
        @Override
        public void onSkipInRead(Throwable t) {
            log.warn("읽기 중 스킵 발생: {}", t.getMessage());
        }

        @Override
        public void onSkipInProcess(User item, Throwable t) {
            log.warn("처리 중 스킵 발생 - Item: {}, Error: {}", item.getId(), t.getMessage());
            // 실패 항목을 별도 테이블에 저장
            errorRepository.save(new ErrorRecord(item.getId(), t.getMessage()));
        }

        @Override
        public void onSkipInWrite(User item, Throwable t) {
            log.warn("쓰기 중 스킵 발생 - Item: {}, Error: {}", item.getId(), t.getMessage());
        }
    };
}
```

## Job/Step 리스너

### ChunkListener

```java
@Bean
public Step stepWithChunkListener() {
    return new StepBuilder("stepWithChunkListener", jobRepository)
            .<User, User>chunk(100, transactionManager)
            .reader(userReader())
            .writer(userWriter())
            .listener(new ChunkListener() {
                @Override
                public void beforeChunk(ChunkContext context) {
                    log.info("Chunk 시작 - Count: {}", context.getStepContext().getStepExecution().getCommitCount());
                }

                @Override
                public void afterChunk(ChunkContext context) {
                    log.info("Chunk 완료 - Read: {}, Write: {}",
                            context.getStepContext().getStepExecution().getReadCount(),
                            context.getStepContext().getStepExecution().getWriteCount());
                }

                @Override
                public void afterChunkError(ChunkContext context) {
                    log.error("Chunk 에러 발생");
                }
            })
            .build();
}
```

### ItemReadListener / ItemProcessListener / ItemWriteListener

```java
@Component
public class CustomItemListener implements ItemReadListener<User>,
                                            ItemProcessListener<User, User>,
                                            ItemWriteListener<User> {

    @Override
    public void beforeRead() {
        // 읽기 전
    }

    @Override
    public void afterRead(User item) {
        log.debug("읽음: {}", item.getId());
    }

    @Override
    public void onReadError(Exception ex) {
        log.error("읽기 에러: {}", ex.getMessage());
    }

    @Override
    public void beforeProcess(User item) {
        // 처리 전
    }

    @Override
    public void afterProcess(User input, User output) {
        if (output == null) {
            log.info("필터링됨: {}", input.getId());
        }
    }

    @Override
    public void onProcessError(User item, Exception e) {
        log.error("처리 에러 - ID: {}, Error: {}", item.getId(), e.getMessage());
    }

    @Override
    public void beforeWrite(Chunk<? extends User> items) {
        log.info("쓰기 시작: {} 건", items.size());
    }

    @Override
    public void afterWrite(Chunk<? extends User> items) {
        log.info("쓰기 완료: {} 건", items.size());
    }

    @Override
    public void onWriteError(Exception exception, Chunk<? extends User> items) {
        log.error("쓰기 에러: {} 건 실패", items.size());
    }
}
```

## ExecutionContext와 데이터 공유

### Step 간 데이터 전달

```java
@Bean
public Step step1() {
    return new StepBuilder("step1", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                // Step 간 공유할 데이터를 JobExecutionContext에 저장
                ExecutionContext jobContext = chunkContext.getStepContext()
                        .getStepExecution()
                        .getJobExecution()
                        .getExecutionContext();

                jobContext.put("processedCount", 100);
                jobContext.put("startTime", LocalDateTime.now().toString());

                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
}

@Bean
public Step step2() {
    return new StepBuilder("step2", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                // 이전 Step에서 저장한 데이터 읽기
                ExecutionContext jobContext = chunkContext.getStepContext()
                        .getStepExecution()
                        .getJobExecution()
                        .getExecutionContext();

                int processedCount = jobContext.getInt("processedCount");
                String startTime = jobContext.getString("startTime");

                log.info("이전 Step 처리 건수: {}, 시작 시간: {}", processedCount, startTime);

                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
}
```

### ExecutionContextPromotionListener

```java
@Bean
public Step promotionStep() {
    return new StepBuilder("promotionStep", jobRepository)
            .<User, User>chunk(100, transactionManager)
            .reader(userReader())
            .writer(userWriter())
            .listener(promotionListener())
            .build();
}

@Bean
public ExecutionContextPromotionListener promotionListener() {
    ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
    // StepExecutionContext의 특정 키를 JobExecutionContext로 승격
    listener.setKeys(new String[]{"totalCount", "errorCount"});
    return listener;
}
```

## JobLauncher와 비동기 실행

### 비동기 JobLauncher 설정

```java
@Configuration
public class AsyncJobLauncherConfig {

    @Bean
    public JobLauncher asyncJobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());  // 비동기 실행
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }
}
```

### REST API를 통한 Job 실행

```java
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchController {

    private final JobLauncher asyncJobLauncher;
    private final Job myJob;

    @PostMapping("/run")
    public ResponseEntity<String> runJob(@RequestParam Map<String, String> params) {
        try {
            JobParametersBuilder builder = new JobParametersBuilder();
            builder.addLong("timestamp", System.currentTimeMillis());  // 유니크 파라미터
            params.forEach(builder::addString);

            // 비동기 실행 - 즉시 반환
            JobExecution execution = asyncJobLauncher.run(myJob, builder.toJobParameters());

            return ResponseEntity.ok("Job started with ID: " + execution.getId());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to start job: " + e.getMessage());
        }
    }

    @GetMapping("/status/{executionId}")
    public ResponseEntity<JobExecutionStatus> getStatus(@PathVariable Long executionId) {
        // JobExplorer를 통해 상태 조회
        JobExecution execution = jobExplorer.getJobExecution(executionId);
        return ResponseEntity.ok(new JobExecutionStatus(
                execution.getStatus(),
                execution.getExitStatus(),
                execution.getStartTime(),
                execution.getEndTime()
        ));
    }
}
```

### 스케줄링을 통한 Job 실행

```java
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job dailyJob;
    private final Job hourlyJob;

    @Scheduled(cron = "0 0 2 * * *")  // 매일 새벽 2시
    public void runDailyJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLocalDate("date", LocalDate.now())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(dailyJob, params);
        } catch (Exception e) {
            log.error("Daily job failed", e);
        }
    }

    @Scheduled(fixedRate = 3600000)  // 1시간마다
    public void runHourlyJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(hourlyJob, params);
        } catch (Exception e) {
            log.error("Hourly job failed", e);
        }
    }
}
```

## 비교 정리

| 구분 | Split | Partition | Remote Partition | Remote Chunking | Async | Multi-thread |
|------|-------|-----------|------------------|-----------------|-------|--------------|
| **병렬화 대상** | Flow | 데이터 | 데이터 | Chunk | Processor | Chunk |
| **실행 환경** | 단일 JVM | 단일 JVM | 다중 JVM | 다중 JVM | 단일 JVM | 단일 JVM |
| **데이터 전송** | X | X | 메타데이터 | 실제 데이터 | X | X |
| **확장성** | 제한적 | 제한적 | 높음 | 높음 | 제한적 | 제한적 |
| **복잡도** | 낮음 | 중간 | 높음 | 높음 | 낮음 | 낮음 |
| **재시작** | 용이 | 용이 | 용이 | 어려움 | 용이 | 어려움 |

## 주의사항

### 1. Thread-safe 보장

```java
// 잘못된 예: 공유 상태 사용
public class UnsafeProcessor implements ItemProcessor<User, User> {
    private int count = 0;  // 공유 상태!

    @Override
    public User process(User user) {
        count++;  // 동시성 문제 발생
        return user;
    }
}

// 올바른 예: ExecutionContext 또는 AtomicInteger 사용
public class SafeProcessor implements ItemProcessor<User, User> {
    private final AtomicInteger count = new AtomicInteger(0);

    @Override
    public User process(User user) {
        count.incrementAndGet();
        return user;
    }
}
```

### 2. DB Connection Pool 설정

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # 파티션 수 + 여유분
      minimum-idle: 10
```

### 3. 트랜잭션 고려

```java
// 파티션별로 독립적인 트랜잭션
// 하나의 파티션 실패가 다른 파티션에 영향 없음
@Bean
public Step slaveStep() {
    return new StepBuilder("slaveStep", jobRepository)
            .<User, User>chunk(100, transactionManager)
            .reader(reader())
            .writer(writer())
            .faultTolerant()
            .skipLimit(10)
            .skip(Exception.class)
            .build();
}
```

### 4. gridSize 결정 기준

```java
// CPU 바운드: CPU 코어 수
int gridSize = Runtime.getRuntime().availableProcessors();

// I/O 바운드: 코어 수 * 2 이상
int gridSize = Runtime.getRuntime().availableProcessors() * 2;

// 데이터 특성에 따라: 전체 데이터 / 파티션당 처리량
long totalCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
int gridSize = (int) Math.ceil((double) totalCount / 100000);  // 파티션당 10만건
```

### 5. 원격 처리 시 메시지 직렬화

```java
// 메시지 큐로 전송되는 객체는 Serializable 구현 필요
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String status;
    // ...
}
```

### 6. 재시작과 멱등성

```java
// 멱등한 Writer 구현 (UPSERT)
@Bean
public ItemWriter<User> idempotentWriter() {
    return new JdbcBatchItemWriterBuilder<User>()
            .dataSource(dataSource)
            .sql("""
                INSERT INTO users (id, name, email, updated_at)
                VALUES (:id, :name, :email, :updatedAt)
                ON DUPLICATE KEY UPDATE
                    name = VALUES(name),
                    email = VALUES(email),
                    updated_at = VALUES(updated_at)
                """)
            .beanMapped()
            .build();
}
```

*마지막 업데이트: 2026년 01월*
