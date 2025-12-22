# Spring Batch ExecutionContext 완벽 가이드

배치 실행 상태를 저장하고 재시작 시 복구하는 ExecutionContext의 활용법을 정리한다.

## 목차

- [ExecutionContext란?](#executioncontext란)
- [Job vs Step ExecutionContext](#job-vs-step-executioncontext)
- [데이터 저장과 조회](#데이터-저장과-조회)
- [SpEL을 통한 Late Binding](#spel을-통한-late-binding)
- [ItemStream 인터페이스](#itemstream-인터페이스)
- [재시작 시나리오](#재시작-시나리오)
- [실무 활용 패턴](#실무-활용-패턴)
- [주의사항](#주의사항)

---

## ExecutionContext란?

ExecutionContext는 배치 실행 중 **상태를 저장하는 키-값 저장소**다. 메타데이터 테이블에 직렬화되어 저장되므로, Job이 실패 후 재시작할 때 이전 상태를 복구할 수 있다.

```
┌─────────────────────────────────────────────────────────────────┐
│                      ExecutionContext 흐름                       │
│                                                                  │
│   Step 실행 중                    DB 메타데이터 테이블            │
│   ┌──────────────┐               ┌──────────────────────┐       │
│   │ ExecutionContext │  ──────▶  │ BATCH_STEP_EXECUTION  │       │
│   │ {                │   직렬화   │ _CONTEXT              │       │
│   │   "lastId": 500, │           │ ───────────────────── │       │
│   │   "count": 1000  │           │ SHORT_CONTEXT (JSON)  │       │
│   │ }                │           └──────────────────────┘       │
│   └──────────────┘                                              │
│         │                                                        │
│         │ 재시작 시                                               │
│         ▼                                                        │
│   ┌──────────────┐                                              │
│   │ 이전 상태 복구  │  ◀──────  DB에서 읽어옴                      │
│   │ lastId = 500   │                                             │
│   └──────────────┘                                              │
└─────────────────────────────────────────────────────────────────┘
```

---

## Job vs Step ExecutionContext

두 종류의 ExecutionContext가 있으며, **범위(scope)**가 다르다.

```
┌─────────────────────────────────────────────────────────────────┐
│                           Job                                    │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │              JobExecutionContext                         │   │
│   │              (Job 전체에서 공유)                          │   │
│   │              예: 총 처리 건수, 전역 설정                   │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                  │
│   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐       │
│   │    Step 1     │   │    Step 2     │   │    Step 3     │       │
│   │ ┌──────────┐ │   │ ┌──────────┐ │   │ ┌──────────┐ │       │
│   │ │StepExec  │ │   │ │StepExec  │ │   │ │StepExec  │ │       │
│   │ │Context   │ │   │ │Context   │ │   │ │Context   │ │       │
│   │ │(Step 내) │ │   │ │(Step 내) │ │   │ │(Step 내) │ │       │
│   │ └──────────┘ │   │ └──────────┘ │   │ └──────────┘ │       │
│   └──────────────┘   └──────────────┘   └──────────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

### 비교

| 구분 | JobExecutionContext | StepExecutionContext |
|------|---------------------|---------------------|
| **범위** | Job 전체 | 해당 Step 내 |
| **공유** | 모든 Step에서 접근 가능 | 해당 Step에서만 접근 |
| **저장 테이블** | BATCH_JOB_EXECUTION_CONTEXT | BATCH_STEP_EXECUTION_CONTEXT |
| **용도** | Step 간 데이터 전달, 전역 상태 | Step 내 진행 상태 추적 |

### 접근 방법

```java
// Tasklet에서 접근
@Bean
public Tasklet myTasklet() {
    return (contribution, chunkContext) -> {
        // StepExecutionContext
        ExecutionContext stepContext = chunkContext.getStepContext()
                .getStepExecution()
                .getExecutionContext();

        // JobExecutionContext
        ExecutionContext jobContext = chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext();

        // 데이터 저장
        stepContext.putLong("lastProcessedId", 12345L);
        jobContext.putInt("totalProcessed", 1000);

        return RepeatStatus.FINISHED;
    };
}
```

---

## 데이터 저장과 조회

### 지원하는 데이터 타입

```java
ExecutionContext context = stepExecution.getExecutionContext();

// 기본 타입
context.putString("status", "PROCESSING");
context.putLong("lastId", 12345L);
context.putInt("count", 100);
context.putDouble("rate", 0.95);

// 조회
String status = context.getString("status");
Long lastId = context.getLong("lastId");
int count = context.getInt("count");
double rate = context.getDouble("rate");

// 기본값 지정
Long id = context.getLong("lastId", 0L);  // 없으면 0L 반환

// 존재 여부 확인
if (context.containsKey("lastId")) {
    // ...
}
```

### 복합 객체 저장

```java
// 직렬화 가능한 객체 저장
context.put("checkpoint", new Checkpoint(lastId, lastDate));

// 조회
Checkpoint checkpoint = (Checkpoint) context.get("checkpoint");

// ⚠️ 주의: 객체는 Serializable 구현 필수
@Data
public class Checkpoint implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long lastId;
    private LocalDate lastDate;
}
```

### List/Map 저장

```java
// List 저장
List<Long> processedIds = Arrays.asList(1L, 2L, 3L);
context.put("processedIds", new ArrayList<>(processedIds));

// Map 저장
Map<String, Integer> stats = new HashMap<>();
stats.put("success", 100);
stats.put("failed", 5);
context.put("stats", stats);

// 조회
List<Long> ids = (List<Long>) context.get("processedIds");
Map<String, Integer> savedStats = (Map<String, Integer>) context.get("stats");
```

---

## SpEL을 통한 Late Binding

**@StepScope**와 **SpEL(Spring Expression Language)**을 조합하면, Step 실행 시점에 ExecutionContext 값을 주입받을 수 있다.

### 기본 문법

```java
@Bean
@StepScope  // 필수! Step 실행 시점에 빈 생성
public ItemReader<Customer> reader(
        // StepExecutionContext에서 값 주입
        @Value("#{stepExecutionContext['lastProcessedId']}") Long lastId,

        // JobExecutionContext에서 값 주입
        @Value("#{jobExecutionContext['globalSetting']}") String setting,

        // JobParameters에서 값 주입
        @Value("#{jobParameters['inputFile']}") String inputFile) {

    // lastId, setting, inputFile 사용
}
```

### 상세 예시: 마지막 처리 ID 기반 재시작

```java
@Configuration
@RequiredArgsConstructor
public class RestartableStepConfig {

    private final DataSource dataSource;

    /**
     * 마지막으로 처리한 ID 이후부터 읽기 시작
     * - 최초 실행: lastProcessedId = null → 0부터 시작
     * - 재시작: lastProcessedId = 이전 마지막 ID → 그 이후부터
     */
    @Bean
    @StepScope
    public JdbcPagingItemReader<Customer> reader(
            @Value("#{stepExecutionContext['lastProcessedId']}") Long lastProcessedId) {

        // null 처리 (최초 실행 시)
        long startId = (lastProcessedId != null) ? lastProcessedId : 0L;

        log.info("Reader 시작 ID: {}", startId);

        return new JdbcPagingItemReaderBuilder<Customer>()
                .name("customerReader")
                .dataSource(dataSource)
                .selectClause("SELECT id, name, email, status")
                .fromClause("FROM customers")
                .whereClause("WHERE id > :startId AND status = 'PENDING'")
                .parameterValues(Map.of("startId", startId))
                .sortKeys(Map.of("id", Order.ASCENDING))
                .pageSize(100)
                .rowMapper(new BeanPropertyRowMapper<>(Customer.class))
                .build();
    }

    /**
     * 처리한 마지막 ID를 ExecutionContext에 저장
     */
    @Bean
    public ItemWriter<Customer> writer() {
        return new ItemWriter<>() {
            @Override
            public void write(Chunk<? extends Customer> customers) throws Exception {
                // 실제 저장 로직
                customerRepository.saveAll(customers.getItems());
            }
        };
    }

    /**
     * Chunk 처리 후 마지막 ID 업데이트
     */
    @Bean
    public StepExecutionListener lastIdTracker() {
        return new StepExecutionListener() {
            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                // 마지막 처리 ID 로깅
                Long lastId = stepExecution.getExecutionContext()
                        .getLong("lastProcessedId", 0L);
                log.info("Step 완료 - 마지막 처리 ID: {}", lastId);
                return stepExecution.getExitStatus();
            }
        };
    }

    @Bean
    public Step restartableStep(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager) {
        return new StepBuilder("restartableStep", jobRepository)
                .<Customer, Customer>chunk(100, transactionManager)
                .reader(reader(null))  // @StepScope로 실제 값은 런타임에 주입
                .processor(processor())
                .writer(writer())
                .listener(lastIdTracker())
                .listener(new ChunkListener() {
                    @Override
                    public void afterChunk(ChunkContext context) {
                        // Chunk 완료 후 마지막 ID 저장
                        StepExecution stepExecution = context.getStepContext()
                                .getStepExecution();
                        // ItemStream에서 저장됨 (아래 참고)
                    }
                })
                .build();
    }
}
```

### null 안전 처리 (Elvis 연산자)

```java
// 방법 1: Elvis 연산자 (?:)
@Value("#{stepExecutionContext['lastId'] ?: 0}")
Long lastId;  // null이면 0

// 방법 2: 삼항 연산자
@Value("#{stepExecutionContext['lastId'] != null ? stepExecutionContext['lastId'] : 0}")
Long lastId;

// 방법 3: 코드에서 처리
@Bean
@StepScope
public ItemReader<Customer> reader(
        @Value("#{stepExecutionContext['lastId']}") Long lastId) {
    long startId = Optional.ofNullable(lastId).orElse(0L);
    // ...
}
```

---

## ItemStream 인터페이스

**ItemStream**은 Reader/Writer가 ExecutionContext와 상호작용하는 표준 인터페이스다.

```java
public interface ItemStream {
    // Step 시작 시 호출 - ExecutionContext에서 상태 복구
    void open(ExecutionContext executionContext);

    // Chunk 완료 시 호출 - ExecutionContext에 상태 저장
    void update(ExecutionContext executionContext);

    // Step 종료 시 호출 - 리소스 정리
    void close();
}
```

### 동작 흐름

```
┌─────────────────────────────────────────────────────────────────┐
│                    ItemStream 라이프사이클                        │
│                                                                  │
│  Step 시작                                                       │
│      │                                                           │
│      ▼                                                           │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ open(executionContext)                                   │    │
│  │   - DB에서 이전 ExecutionContext 로드                     │    │
│  │   - 재시작이면 lastProcessedId 등 복구                    │    │
│  └─────────────────────────────────────────────────────────┘    │
│      │                                                           │
│      ▼                                                           │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Chunk 1: Read → Process → Write                          │    │
│  │   └─▶ update(executionContext) - 진행 상태 저장           │    │
│  │                                                           │    │
│  │ Chunk 2: Read → Process → Write                          │    │
│  │   └─▶ update(executionContext) - 진행 상태 저장           │    │
│  │                                                           │    │
│  │ ...반복...                                                │    │
│  └─────────────────────────────────────────────────────────┘    │
│      │                                                           │
│      ▼                                                           │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ close()                                                   │    │
│  │   - 리소스 정리 (파일 핸들, DB 커서 등)                    │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### 커스텀 ItemStreamReader 구현

```java
@Component
@Slf4j
public class RestartableApiReader implements ItemStreamReader<ApiData> {

    private static final String LAST_OFFSET_KEY = "lastOffset";
    private static final String TOTAL_READ_KEY = "totalRead";

    private final ApiClient apiClient;

    private int currentOffset = 0;
    private int totalRead = 0;
    private List<ApiData> buffer = new ArrayList<>();
    private int bufferIndex = 0;

    @Override
    public void open(ExecutionContext executionContext) {
        // 재시작 시 이전 상태 복구
        if (executionContext.containsKey(LAST_OFFSET_KEY)) {
            this.currentOffset = executionContext.getInt(LAST_OFFSET_KEY);
            this.totalRead = executionContext.getInt(TOTAL_READ_KEY);
            log.info("이전 상태 복구 - offset: {}, totalRead: {}",
                    currentOffset, totalRead);
        } else {
            log.info("신규 실행 시작");
        }
    }

    @Override
    public ApiData read() {
        // 버퍼가 비었으면 API 호출
        if (bufferIndex >= buffer.size()) {
            buffer = apiClient.fetchData(currentOffset, 100);
            bufferIndex = 0;
            currentOffset += 100;

            if (buffer.isEmpty()) {
                return null;  // 더 이상 데이터 없음
            }
        }

        totalRead++;
        return buffer.get(bufferIndex++);
    }

    @Override
    public void update(ExecutionContext executionContext) {
        // 현재 진행 상태 저장 (Chunk 완료마다 호출)
        executionContext.putInt(LAST_OFFSET_KEY, currentOffset);
        executionContext.putInt(TOTAL_READ_KEY, totalRead);
        log.debug("상태 저장 - offset: {}, totalRead: {}", currentOffset, totalRead);
    }

    @Override
    public void close() {
        buffer.clear();
        log.info("Reader 종료 - 총 읽기: {}", totalRead);
    }
}
```

### 커스텀 ItemStreamWriter 구현

```java
@Component
@Slf4j
public class CheckpointWriter implements ItemStreamWriter<ProcessedData> {

    private static final String LAST_WRITTEN_ID_KEY = "lastWrittenId";
    private static final String WRITE_COUNT_KEY = "writeCount";

    private final DataRepository repository;

    private Long lastWrittenId = 0L;
    private int writeCount = 0;

    @Override
    public void open(ExecutionContext executionContext) {
        if (executionContext.containsKey(LAST_WRITTEN_ID_KEY)) {
            this.lastWrittenId = executionContext.getLong(LAST_WRITTEN_ID_KEY);
            this.writeCount = executionContext.getInt(WRITE_COUNT_KEY);
            log.info("Writer 상태 복구 - lastWrittenId: {}", lastWrittenId);
        }
    }

    @Override
    public void write(Chunk<? extends ProcessedData> items) throws Exception {
        for (ProcessedData item : items) {
            repository.save(item);
            lastWrittenId = item.getId();
            writeCount++;
        }
    }

    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.putLong(LAST_WRITTEN_ID_KEY, lastWrittenId);
        executionContext.putInt(WRITE_COUNT_KEY, writeCount);
    }

    @Override
    public void close() {
        log.info("Writer 종료 - 총 쓰기: {}", writeCount);
    }
}
```

---

## 재시작 시나리오

### 시나리오 1: 정상 재시작

```
[1차 실행]
Chunk 1: ID 1-100 처리 → ExecutionContext: {lastId: 100}
Chunk 2: ID 101-200 처리 → ExecutionContext: {lastId: 200}
Chunk 3: ID 201-300 처리 중 실패 ❌
         → ExecutionContext: {lastId: 200} (Chunk 3은 롤백)

[2차 실행 - 재시작]
open() 호출 → ExecutionContext에서 lastId: 200 복구
Chunk 3: ID 201-300 처리 (다시 시작) → 성공 ✅
Chunk 4: ID 301-400 처리 → 성공 ✅
...완료
```

```java
@Bean
public Step restartableStep(JobRepository jobRepository,
                            PlatformTransactionManager transactionManager) {
    return new StepBuilder("restartableStep", jobRepository)
            .<Customer, Customer>chunk(100, transactionManager)
            .reader(reader(null))
            .writer(writer())
            .allowStartIfComplete(false)  // 완료된 Step은 재실행 안 함
            .build();
}
```

### 시나리오 2: saveState(false) 사용 시

```java
@Bean
@StepScope
public JdbcPagingItemReader<Customer> nonRestartableReader() {
    return new JdbcPagingItemReaderBuilder<Customer>()
            .name("nonRestartableReader")
            .dataSource(dataSource)
            .saveState(false)  // ExecutionContext에 상태 저장 안 함
            // ... 기타 설정
            .build();
}
```

```
[1차 실행]
Chunk 1: ID 1-100 처리
Chunk 2: ID 101-200 처리
Chunk 3: 실패 ❌

[2차 실행]
saveState(false)이므로 처음부터 다시 시작!
Chunk 1: ID 1-100 (중복 처리 ⚠️)
...
```

**saveState(false) 사용 시점:**
- 멀티스레드 Step (상태 저장 불가)
- 멱등성이 보장되어 중복 처리해도 무방할 때
- 재시작 기능이 필요 없을 때

### 시나리오 3: Step 간 데이터 전달

```java
@Configuration
public class MultiStepJobConfig {

    // Step 1: 데이터 집계 후 결과를 JobExecutionContext에 저장
    @Bean
    public Step aggregationStep(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager) {
        return new StepBuilder("aggregationStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    int totalCount = calculateTotalCount();
                    BigDecimal totalAmount = calculateTotalAmount();

                    // JobExecutionContext에 저장 (다른 Step에서 사용)
                    ExecutionContext jobContext = chunkContext.getStepContext()
                            .getStepExecution()
                            .getJobExecution()
                            .getExecutionContext();

                    jobContext.putInt("totalCount", totalCount);
                    jobContext.put("totalAmount", totalAmount);

                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    // Step 2: JobExecutionContext에서 데이터 읽기
    @Bean
    @StepScope
    public Tasklet reportTasklet(
            @Value("#{jobExecutionContext['totalCount']}") Integer totalCount,
            @Value("#{jobExecutionContext['totalAmount']}") BigDecimal totalAmount) {

        return (contribution, chunkContext) -> {
            log.info("리포트 생성 - 건수: {}, 금액: {}", totalCount, totalAmount);
            generateReport(totalCount, totalAmount);
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step reportStep(JobRepository jobRepository,
                           PlatformTransactionManager transactionManager) {
        return new StepBuilder("reportStep", jobRepository)
                .tasklet(reportTasklet(null, null), transactionManager)
                .build();
    }

    @Bean
    public Job multiStepJob(JobRepository jobRepository) {
        return new JobBuilder("multiStepJob", jobRepository)
                .start(aggregationStep(null, null))
                .next(reportStep(null, null))
                .build();
    }
}
```

---

## 실무 활용 패턴

### 패턴 1: 진행률 추적

```java
@Component
@Slf4j
public class ProgressTrackingReader implements ItemStreamReader<Customer> {

    private static final String PROCESSED_COUNT = "processedCount";
    private static final String TOTAL_COUNT = "totalCount";

    private final CustomerRepository repository;
    private Iterator<Customer> iterator;
    private int processedCount = 0;
    private int totalCount = 0;

    @Override
    public void open(ExecutionContext context) {
        // 전체 건수 조회
        totalCount = (int) repository.countByStatus(Status.PENDING);
        context.putInt(TOTAL_COUNT, totalCount);

        // 재시작 시 이전 진행 상태 복구
        if (context.containsKey(PROCESSED_COUNT)) {
            processedCount = context.getInt(PROCESSED_COUNT);
            log.info("재시작 - 진행률: {}/{} ({}%)",
                    processedCount, totalCount,
                    (processedCount * 100) / totalCount);
        }

        // Iterator 초기화 (offset 적용)
        List<Customer> customers = repository.findByStatusWithOffset(
                Status.PENDING, processedCount);
        iterator = customers.iterator();
    }

    @Override
    public Customer read() {
        if (iterator.hasNext()) {
            processedCount++;
            return iterator.next();
        }
        return null;
    }

    @Override
    public void update(ExecutionContext context) {
        context.putInt(PROCESSED_COUNT, processedCount);

        // 진행률 로깅 (10% 단위)
        int progress = (processedCount * 100) / totalCount;
        if (progress % 10 == 0) {
            log.info("진행률: {}% ({}/{})", progress, processedCount, totalCount);
        }
    }

    @Override
    public void close() {
        log.info("처리 완료: {}/{}", processedCount, totalCount);
    }
}
```

### 패턴 2: 파티션 정보 전달

```java
@Configuration
public class PartitionedStepConfig {

    @Bean
    public Partitioner rangePartitioner() {
        return gridSize -> {
            Map<String, ExecutionContext> partitions = new HashMap<>();

            long min = 1;
            long max = 1000000;
            long range = (max - min) / gridSize + 1;

            for (int i = 0; i < gridSize; i++) {
                ExecutionContext context = new ExecutionContext();

                long start = min + (range * i);
                long end = Math.min(start + range - 1, max);

                context.putLong("minId", start);
                context.putLong("maxId", end);
                context.putString("partitionName", "partition" + i);

                partitions.put("partition" + i, context);
            }

            return partitions;
        };
    }

    @Bean
    @StepScope
    public JdbcPagingItemReader<Customer> partitionedReader(
            @Value("#{stepExecutionContext['minId']}") Long minId,
            @Value("#{stepExecutionContext['maxId']}") Long maxId,
            @Value("#{stepExecutionContext['partitionName']}") String partitionName) {

        log.info("{} 시작 - ID 범위: {} ~ {}", partitionName, minId, maxId);

        return new JdbcPagingItemReaderBuilder<Customer>()
                .name("partitionedReader_" + partitionName)
                .dataSource(dataSource)
                .selectClause("SELECT *")
                .fromClause("FROM customers")
                .whereClause("WHERE id BETWEEN :minId AND :maxId")
                .parameterValues(Map.of("minId", minId, "maxId", maxId))
                .sortKeys(Map.of("id", Order.ASCENDING))
                .pageSize(1000)
                .build();
    }
}
```

### 패턴 3: 오류 컨텍스트 저장

```java
@Component
public class ErrorTrackingProcessor implements ItemProcessor<Order, Order> {

    @Value("#{stepExecutionContext}")
    private ExecutionContext stepContext;

    @Override
    public Order process(Order order) {
        try {
            return processOrder(order);
        } catch (ValidationException e) {
            // 오류 정보 저장
            List<String> errors = getOrCreateErrorList();
            errors.add(String.format("Order %d: %s", order.getId(), e.getMessage()));
            stepContext.put("validationErrors", errors);

            return null;  // 필터링
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getOrCreateErrorList() {
        if (!stepContext.containsKey("validationErrors")) {
            stepContext.put("validationErrors", new ArrayList<String>());
        }
        return (List<String>) stepContext.get("validationErrors");
    }
}

// Step 완료 후 오류 리포트
@Bean
public StepExecutionListener errorReportListener() {
    return new StepExecutionListener() {
        @Override
        public ExitStatus afterStep(StepExecution stepExecution) {
            ExecutionContext context = stepExecution.getExecutionContext();

            if (context.containsKey("validationErrors")) {
                List<String> errors = (List<String>) context.get("validationErrors");
                log.warn("검증 오류 {} 건 발생:", errors.size());
                errors.forEach(e -> log.warn("  - {}", e));

                // 오류 리포트 발송
                sendErrorReport(errors);
            }

            return stepExecution.getExitStatus();
        }
    };
}
```

### 패턴 4: 외부 리소스 체크포인트

```java
@Component
@Slf4j
public class S3FileReader implements ItemStreamReader<String> {

    private static final String CURRENT_FILE_KEY = "currentFileKey";
    private static final String CURRENT_LINE = "currentLine";

    private final S3Client s3Client;
    private final List<String> fileKeys;

    private int currentFileIndex = 0;
    private long currentLine = 0;
    private BufferedReader currentReader;

    @Override
    public void open(ExecutionContext context) {
        // 재시작 시 이전 파일/라인 위치 복구
        if (context.containsKey(CURRENT_FILE_KEY)) {
            String lastFileKey = context.getString(CURRENT_FILE_KEY);
            currentFileIndex = fileKeys.indexOf(lastFileKey);
            currentLine = context.getLong(CURRENT_LINE);

            log.info("재시작 - 파일: {}, 라인: {}", lastFileKey, currentLine);
        }

        openFile(currentFileIndex);
        skipLines(currentLine);
    }

    @Override
    public String read() throws Exception {
        String line = currentReader.readLine();

        if (line == null) {
            // 현재 파일 끝 → 다음 파일로
            currentFileIndex++;
            currentLine = 0;

            if (currentFileIndex >= fileKeys.size()) {
                return null;  // 모든 파일 처리 완료
            }

            openFile(currentFileIndex);
            return read();  // 재귀 호출
        }

        currentLine++;
        return line;
    }

    @Override
    public void update(ExecutionContext context) {
        context.putString(CURRENT_FILE_KEY, fileKeys.get(currentFileIndex));
        context.putLong(CURRENT_LINE, currentLine);
    }

    @Override
    public void close() {
        if (currentReader != null) {
            try {
                currentReader.close();
            } catch (IOException e) {
                log.warn("Reader 종료 실패", e);
            }
        }
    }

    private void openFile(int index) {
        String key = fileKeys.get(index);
        InputStream stream = s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());
        currentReader = new BufferedReader(new InputStreamReader(stream));
    }

    private void skipLines(long lines) {
        for (long i = 0; i < lines; i++) {
            try {
                currentReader.readLine();
            } catch (IOException e) {
                throw new RuntimeException("라인 스킵 실패", e);
            }
        }
    }
}
```

---

## 주의사항

### 1. 직렬화 가능한 데이터만 저장

```java
// ❌ 잘못된 예: 직렬화 불가능한 객체
context.put("connection", dataSource.getConnection());  // 안 됨!
context.put("lambda", (Runnable) () -> {});  // 안 됨!

// ✅ 올바른 예
context.putString("connectionInfo", "jdbc:mysql://...");
context.putLong("lastId", 12345L);
```

### 2. 큰 데이터 저장 금지

```java
// ❌ 잘못된 예: 대용량 데이터 저장
List<Customer> allCustomers = repository.findAll();  // 100만 건
context.put("customers", allCustomers);  // 메타데이터 테이블 폭발!

// ✅ 올바른 예: ID만 저장
context.putLong("lastCustomerId", 999999L);
```

### 3. 멀티스레드 환경 주의

```java
// 멀티스레드 Step에서는 ExecutionContext 공유 불가
@Bean
public Step multiThreadStep(JobRepository jobRepository,
                            PlatformTransactionManager transactionManager) {
    return new StepBuilder("multiThreadStep", jobRepository)
            .<Customer, Customer>chunk(100, transactionManager)
            .reader(reader())
            .writer(writer())
            .taskExecutor(taskExecutor())
            .build();
}

@Bean
public JdbcPagingItemReader<Customer> reader() {
    return new JdbcPagingItemReaderBuilder<Customer>()
            .name("reader")
            .saveState(false)  // 멀티스레드에서는 필수!
            // ...
            .build();
}
```

### 4. Key 이름 충돌 방지

```java
// ❌ 잘못된 예: 일반적인 이름
context.put("count", 100);  // 다른 컴포넌트와 충돌 가능

// ✅ 올바른 예: 네임스페이스 사용
context.put("payment.processor.count", 100);
context.put("customer.reader.lastId", 12345L);
```

### 5. null 값 처리

```java
// ❌ 잘못된 예: NPE 위험
Long lastId = context.getLong("lastId");  // 없으면 예외!

// ✅ 올바른 예: 기본값 지정
Long lastId = context.getLong("lastId", 0L);

// 또는 존재 여부 확인
if (context.containsKey("lastId")) {
    Long lastId = context.getLong("lastId");
}
```

---

## 관련 문서

- [Spring Batch 기초](./spring-batch-basics.md) - Job, Step, 실행 흐름
- [Spring Batch Chunk 처리](./spring-batch-chunk.md) - ItemReader, ItemProcessor, ItemWriter
- [Spring Batch 고급 활용](./spring-batch-advanced.md) - 비동기, 파티셔닝, 원격 실행
- [Spring Batch 실무 베스트 프랙티스](./spring-batch-best-practices.md) - 정기 결제, 정산 사례

*마지막 업데이트: 2024년 12월*
