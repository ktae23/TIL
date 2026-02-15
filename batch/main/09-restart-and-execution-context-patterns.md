# 재시작과 ExecutionContext 실무 패턴

Spring Batch의 재시작(Restart) 메커니즘과 ExecutionContext를 활용한 실무 패턴을 다룬다. 장애 복구, 상태 추적, Step 간 데이터 전달 등 프로덕션 환경에서 반드시 알아야 할 핵심 기법을 정리한다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### 재시작(Restart)이란?

Spring Batch는 실패한 Job을 **마지막으로 성공한 지점부터** 다시 실행할 수 있다. 이 메커니즘의 핵심은 `ExecutionContext`에 저장된 상태 정보다.

- **StepExecutionContext**: Step 단위의 상태 저장 (예: `lastProcessedId`)
- **JobExecutionContext**: Job 전체에서 공유하는 데이터 (예: Step 간 전달 데이터)

### lastProcessedId 패턴

Chunk 기반 처리에서 "어디까지 처리했는가"를 추적하는 가장 기본적인 패턴이다. Reader가 `ItemStream` 인터페이스를 구현하면 `open()` / `update()` / `close()` 생명주기를 통해 자동으로 상태를 관리할 수 있다.

---

## 2. 왜 알아야 하는가 (Why)

### 프로덕션에서의 재시작은 필수

- **수백만 건 배치가 90% 지점에서 실패**하면 처음부터 다시 실행할 수 없다
- 재시작 메커니즘이 없으면 **수동으로 실패 지점을 찾아 데이터를 보정**해야 한다
- ExecutionContext를 올바르게 활용하면 **자동 장애 복구**가 가능하다

### 실무에서 흔한 문제

1. `saveState(false)` 설정을 모르고 재시작이 안 되는 상황
2. ExecutionContext에 대용량 데이터를 넣어 메타데이터 테이블이 폭발하는 상황
3. Key 이름 충돌로 다른 컴포넌트의 상태를 덮어쓰는 상황
4. `null` 값 처리 미흡으로 재시작 시 NPE 발생

---

## 3. 내부 구현 분석 (How)

### 3.1 재시작 시나리오

#### 시나리오 1: 정상 재시작

```
[1차 실행]
Chunk 1: ID 1-100 처리 -> ExecutionContext: {lastId: 100}
Chunk 2: ID 101-200 처리 -> ExecutionContext: {lastId: 200}
Chunk 3: ID 201-300 처리 중 실패
         -> ExecutionContext: {lastId: 200} (Chunk 3은 롤백)

[2차 실행 - 재시작]
open() 호출 -> ExecutionContext에서 lastId: 200 복구
Chunk 3: ID 201-300 처리 (다시 시작) -> 성공
Chunk 4: ID 301-400 처리 -> 성공
...완료
```

Chunk 3이 실패하면 해당 트랜잭션이 롤백되므로 ExecutionContext에는 Chunk 2까지의 상태(`lastId: 200`)만 남는다. 재시작 시 `open()` 메서드에서 이 값을 복구하여 Chunk 3부터 다시 처리한다.

#### 시나리오 2: saveState(false) 사용 시

```java
@Bean
@StepScope
public JdbcPagingItemReader<Customer> nonRestartableReader() {
    return new JdbcPagingItemReaderBuilder<Customer>()
            .name("nonRestartableReader")
            .dataSource(dataSource)
            .saveState(false)  // ExecutionContext에 상태 저장 안 함
            .build();
}
```

**saveState(false) 사용 시점:**
- 멀티스레드 Step (상태 저장 불가)
- 멱등성이 보장되어 중복 처리해도 무방할 때
- 재시작 기능이 필요 없을 때

#### 시나리오 3: Step 간 데이터 전달

```java
@Configuration
public class MultiStepJobConfig {

    // Step 1: JobExecutionContext에 저장
    @Bean
    public Step aggregationStep(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager) {
        return new StepBuilder("aggregationStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    ExecutionContext jobContext = chunkContext.getStepContext()
                            .getStepExecution()
                            .getJobExecution()
                            .getExecutionContext();

                    jobContext.putInt("totalCount", calculateTotalCount());
                    jobContext.put("totalAmount", calculateTotalAmount());

                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    // Step 2: JobExecutionContext에서 읽기
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
}
```

#### ExecutionContextPromotionListener

StepExecutionContext의 특정 키를 JobExecutionContext로 승격시킨다.

```java
@Bean
public ExecutionContextPromotionListener promotionListener() {
    ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
    listener.setKeys(new String[]{"totalCount", "errorCount"});
    return listener;
}
```

### 3.2 lastProcessedId 심화

#### 어떤 값을 기준으로 선택하나?

| 기준 | 적합한 상황 | 예시 |
|------|------------|------|
| **PK (Auto Increment)** | 단일 테이블, 순차 처리 | `id` |
| **생성일시** | 시간 기반 처리 | `created_at` |
| **복합 키** | 여러 조건 조합 | `date + sequence` |
| **오프셋** | 페이징, API 호출 | `offset`, `page` |

**선택 기준 핵심 3가지:**
- **유일성**: 중복 없이 식별 가능
- **순서 보장**: 정렬했을 때 일관된 순서
- **불변성**: 처리 중 값이 변하지 않음

#### 저장되는 시점

```
┌─────────── 트랜잭션 시작 ───────────┐
│                                      │
│  [Read 100건]                        │
│       ↓                              │
│  [Process 100건]                     │
│       ↓                              │
│  [Write 100건]                       │  <- 비즈니스 데이터 저장
│       ↓                              │
│  [ItemStream.update() 호출]          │  <- ExecutionContext 업데이트
│       ↓                              │
└─────────── 트랜잭션 커밋 ───────────┘  <- 메타데이터 테이블 저장
```

**핵심 포인트:**
- `update()` 호출은 DB 저장이 아님
- **트랜잭션 커밋 시점**에 `BATCH_STEP_EXECUTION_CONTEXT` 테이블에 저장
- 비즈니스 데이터와 메타데이터가 **같은 트랜잭션**으로 묶임

#### 구현 방법 A: ItemStream 직접 구현 (권장)

```java
@Component
public class OrderReader implements ItemStreamReader<Order> {

    private static final String LAST_ID_KEY = "lastProcessedId";
    private Long lastProcessedId = 0L;
    private Iterator<Order> iterator;

    @Override
    public void open(ExecutionContext context) {
        if (context.containsKey(LAST_ID_KEY)) {
            lastProcessedId = context.getLong(LAST_ID_KEY);
        }
        iterator = orderRepository.findByIdGreaterThan(lastProcessedId).iterator();
    }

    @Override
    public Order read() {
        if (iterator.hasNext()) {
            Order order = iterator.next();
            lastProcessedId = order.getId();
            return order;
        }
        return null;
    }

    @Override
    public void update(ExecutionContext context) {
        context.putLong(LAST_ID_KEY, lastProcessedId);
    }

    @Override
    public void close() {}
}
```

#### 구현 방법 B: ChunkListener 사용

```java
@Bean
public Step orderStep(JobRepository jobRepository,
                      PlatformTransactionManager transactionManager) {
    return new StepBuilder("orderStep", jobRepository)
            .<Order, Order>chunk(100, transactionManager)
            .reader(reader(null))
            .writer(writer())
            .listener(new ChunkListener() {
                @Override
                public void afterChunk(ChunkContext context) {
                    StepExecution stepExecution = context.getStepContext()
                            .getStepExecution();
                    Long lastId = (Long) context.getAttribute("lastWrittenId");
                    if (lastId != null) {
                        stepExecution.getExecutionContext()
                                .putLong("lastProcessedId", lastId);
                    }
                }
            })
            .build();
}
```

#### 구현 방법 C: 내장 Reader 자동 저장

Spring Batch의 내장 Reader들은 자동으로 상태를 저장한다:

```java
@Bean
public JdbcPagingItemReader<Order> reader() {
    return new JdbcPagingItemReaderBuilder<Order>()
            .name("orderReader")  // name 필수! (저장 키로 사용)
            .dataSource(dataSource)
            .selectClause("SELECT *")
            .fromClause("FROM orders")
            .sortKeys(Map.of("id", Order.ASCENDING))
            .pageSize(100)
            // saveState(true)가 기본값 -> 자동으로 현재 페이지 저장
            .build();
}
```

---

## 4. 실전 예제

### 패턴 1: 진행률 추적

배치 작업의 실시간 진행 상황을 ExecutionContext에 기록하여, 재시작 시에도 정확한 진행률을 표시한다.

```java
@Component
@Slf4j
public class ProgressTrackingReader implements ItemStreamReader<Customer> {

    private int processedCount = 0;
    private int totalCount = 0;

    @Override
    public void open(ExecutionContext context) {
        totalCount = (int) repository.countByStatus(Status.PENDING);
        context.putInt("totalCount", totalCount);

        if (context.containsKey("processedCount")) {
            processedCount = context.getInt("processedCount");
            log.info("재시작 - 진행률: {}/{} ({}%)",
                    processedCount, totalCount,
                    (processedCount * 100) / totalCount);
        }
    }

    @Override
    public void update(ExecutionContext context) {
        context.putInt("processedCount", processedCount);
        int progress = (processedCount * 100) / totalCount;
        if (progress % 10 == 0) {
            log.info("진행률: {}% ({}/{})", progress, processedCount, totalCount);
        }
    }
}
```

### 패턴 2: 오류 컨텍스트 저장

처리 중 발생한 검증 오류를 ExecutionContext에 수집하여, 배치 완료 후 일괄 리포팅한다.

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
            List<String> errors = getOrCreateErrorList();
            errors.add(String.format("Order %d: %s", order.getId(), e.getMessage()));
            stepContext.put("validationErrors", errors);
            return null;  // 필터링
        }
    }
}
```

### 패턴 3: 외부 리소스 체크포인트

S3 파일 등 외부 리소스를 읽을 때, 현재 파일과 라인 위치를 ExecutionContext에 저장하여 재시작 시 정확한 위치에서 이어서 처리한다.

```java
@Component
@Slf4j
public class S3FileReader implements ItemStreamReader<String> {

    private static final String CURRENT_FILE_KEY = "currentFileKey";
    private static final String CURRENT_LINE = "currentLine";

    @Override
    public void open(ExecutionContext context) {
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
    public void update(ExecutionContext context) {
        context.putString(CURRENT_FILE_KEY, fileKeys.get(currentFileIndex));
        context.putLong(CURRENT_LINE, currentLine);
    }
}
```

### ExecutionContext 주의사항

#### 1. 직렬화 가능한 데이터만 저장

```java
// 잘못된 예: 직렬화 불가능한 객체
context.put("connection", dataSource.getConnection());  // 안 됨!

// 올바른 예
context.putString("connectionInfo", "jdbc:mysql://...");
context.putLong("lastId", 12345L);
```

#### 2. 큰 데이터 저장 금지

```java
// 잘못된 예: 대용량 데이터 저장
context.put("customers", repository.findAll());  // 메타데이터 테이블 폭발!

// 올바른 예: ID만 저장
context.putLong("lastCustomerId", 999999L);
```

#### 3. 멀티스레드 환경에서는 saveState(false) 필수

```java
@Bean
public JdbcPagingItemReader<Customer> reader() {
    return new JdbcPagingItemReaderBuilder<Customer>()
            .name("reader")
            .saveState(false)  // 멀티스레드에서는 필수!
            .build();
}
```

#### 4. Key 이름 충돌 방지

```java
// 잘못된 예: 일반적인 이름
context.put("count", 100);  // 다른 컴포넌트와 충돌 가능

// 올바른 예: 네임스페이스 사용
context.put("payment.processor.count", 100);
```

#### 5. null 값 처리

```java
// 잘못된 예: NPE 위험
Long lastId = context.getLong("lastId");  // 없으면 예외!

// 올바른 예: 기본값 지정
Long lastId = context.getLong("lastId", 0L);

// 또는 존재 여부 확인
if (context.containsKey("lastId")) {
    Long lastId = context.getLong("lastId");
}
```

---

## 5. 정리

| 항목 | 핵심 내용 |
|------|-----------|
| **재시작 원리** | ExecutionContext에 저장된 상태를 `open()`에서 복구하여 실패 지점부터 재개 |
| **상태 저장 시점** | Chunk 트랜잭션 커밋 시 `BATCH_STEP_EXECUTION_CONTEXT` 테이블에 저장 |
| **saveState(false)** | 멀티스레드, 멱등성 보장, 재시작 불필요 시 사용 |
| **Step 간 전달** | JobExecutionContext 또는 ExecutionContextPromotionListener 사용 |
| **lastProcessedId 기준** | 유일성 + 순서 보장 + 불변성을 만족하는 값 선택 |
| **구현 방법** | ItemStream 직접 구현(권장), ChunkListener 사용, 내장 Reader 자동 저장 |
| **주의사항** | 직렬화 가능 데이터만, 소량 데이터만, 네임스페이스 Key, null 기본값 |

---
*참고: Spring Batch 5.x / Spring Boot 3.x 기준*
