# Chunk 트랜잭션과 성능 최적화

Spring Batch Chunk 기반 Step의 트랜잭션 경계, 롤백 제어, 그리고 Chunk Size와 Fetch Size를 활용한 성능 최적화 전략을 다룬다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### Chunk 단위 트랜잭션

Spring Batch의 Chunk 기반 Step은 **각 Chunk를 하나의 트랜잭션**으로 처리한다. `chunk(100, transactionManager)`으로 설정하면, 100건을 읽어서 처리한 후 Writer에서 한 번에 쓰고 커밋한다.

```
┌────────────────────── 트랜잭션 1 ──────────────────────┐
│  Read(1) → Read(2) → ... → Read(100)                   │
│  Process(1) → Process(2) → ... → Process(100)          │
│  Write([1..100])                                        │
│  COMMIT                                                 │
└─────────────────────────────────────────────────────────┘
┌────────────────────── 트랜잭션 2 ──────────────────────┐
│  Read(101) → Read(102) → ... → Read(200)               │
│  Process(101) → ... → Process(200)                      │
│  Write([101..200])                                      │
│  COMMIT                                                 │
└─────────────────────────────────────────────────────────┘
```

### commit-interval (Chunk Size)

`chunk(size)`의 `size`가 곧 commit-interval이다. 이 값은 다음을 결정한다:

- **한 트랜잭션에서 처리할 아이템 수**
- **Writer에 한 번에 전달되는 아이템 수**
- **커밋 주기** -- 실패 시 최대 이 수만큼 재처리된다

### Fetch Size vs Chunk Size

| 설정 | 역할 | 적용 위치 |
|------|------|----------|
| **Chunk Size** | 한 트랜잭션에서 처리할 아이템 수 | Step 레벨 |
| **Fetch Size** | DB에서 한 번에 가져올 행 수 (네트워크 왕복 최소화) | Reader 레벨 |
| **Page Size** | 페이징 쿼리에서 한 페이지의 크기 | PagingReader 레벨 |

---

## 2. 왜 알아야 하는가 (Why)

- **데이터 정합성**: 트랜잭션 경계를 이해하지 못하면 실패 시 어디까지 롤백되는지 예측할 수 없다
- **성능 병목 해소**: Chunk Size와 Fetch Size의 부적절한 설정은 심각한 성능 저하를 야기한다
- **재시작 안정성**: commit-interval이 곧 재시작 시 재처리 범위를 결정하므로, 멱등성 설계와 직결된다
- **메모리 관리**: Chunk Size가 너무 크면 OOM, 너무 작으면 과도한 트랜잭션 오버헤드가 발생한다

---

## 3. 내부 구현 분석 (How)

### 3.1 Chunk 단위 트랜잭션

기본적으로 각 Chunk는 하나의 트랜잭션으로 처리된다. `transactionAttribute`를 통해 트랜잭션 전파 수준을 설정할 수 있다.

```java
@Bean
public Step transactionalStep(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager) {
    return new StepBuilder("transactionalStep", jobRepository)
            .<Customer, CustomerDto>chunk(100, transactionManager)
            .reader(reader())
            .processor(processor())
            .writer(writer())
            .transactionAttribute(new DefaultTransactionAttribute(
                    TransactionDefinition.PROPAGATION_REQUIRED))
            .build();
}
```

### 3.2 Reader 트랜잭션 분리

기본적으로 Reader도 Chunk 트랜잭션 안에서 실행된다. 하지만 JMS 큐처럼 **트랜잭션 내에서 읽으면 안 되는** 경우, `readerIsTransactionalQueue()`로 Reader를 트랜잭션 밖으로 분리할 수 있다.

```java
@Bean
public Step readerOutsideTxStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager) {
    return new StepBuilder("readerOutsideTxStep", jobRepository)
            .<Customer, CustomerDto>chunk(100, transactionManager)
            .reader(reader())
            .processor(processor())
            .writer(writer())
            .readerIsTransactionalQueue()  // Reader를 트랜잭션 밖에서 실행
            .build();
}
```

```
┌─── 트랜잭션 외부 ───┐   ┌─── 트랜잭션 내부 ──────────────┐
│  Read(1..100)        │ → │  Process(1..100)                │
│  (JMS ACK 별도 관리)  │   │  Write([1..100])                │
└──────────────────────┘   │  COMMIT                         │
                           └──────────────────────────────────┘
```

### 3.3 롤백 제어

`faultTolerant()` 모드에서 특정 예외에 대해 롤백을 하지 않도록 설정할 수 있다. 데이터 유효성 문제처럼 재시도해도 같은 결과가 나오는 경우에 유용하다.

```java
@Bean
public Step rollbackControlStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager) {
    return new StepBuilder("rollbackControlStep", jobRepository)
            .<Customer, CustomerDto>chunk(100, transactionManager)
            .reader(reader())
            .processor(processor())
            .writer(writer())
            .faultTolerant()
            .noRollback(ValidationException.class)  // 이 예외는 롤백하지 않음
            .build();
}
```

### 3.4 Chunk Size 튜닝

최적의 Chunk Size는 환경마다 다르다. ChunkListener를 활용하여 Chunk별 처리 시간을 측정하고 최적값을 찾을 수 있다.

```java
@Component
public class ChunkPerformanceListener implements ChunkListener {

    private long chunkStartTime;

    @Override
    public void beforeChunk(ChunkContext context) {
        chunkStartTime = System.currentTimeMillis();
    }

    @Override
    public void afterChunk(ChunkContext context) {
        long duration = System.currentTimeMillis() - chunkStartTime;
        StepExecution stepExecution = context.getStepContext().getStepExecution();
        log.info("Chunk 처리 완료 - 읽기: {}, 쓰기: {}, 소요시간: {}ms",
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                duration);
    }
}
```

Chunk Size에 따른 트레이드오프:

```
작은 Chunk Size (10~50)          큰 Chunk Size (500~5000)
├── 잦은 커밋 → 안전             ├── 적은 커밋 → 고성능
├── 실패 시 재처리 범위 작음       ├── 실패 시 재처리 범위 큼
├── 트랜잭션 오버헤드 높음         ├── 트랜잭션 오버헤드 낮음
└── 메모리 사용 적음              └── 메모리 사용 많음
```

### 3.5 Reader Fetch Size 최적화

JdbcCursorItemReader의 `fetchSize`는 DB에서 한 번의 네트워크 왕복으로 가져올 행 수를 결정한다. 기본값이 작으면 네트워크 왕복이 빈번해져 성능이 저하된다.

```java
@Bean
public JdbcCursorItemReader<Customer> optimizedReader(DataSource dataSource) {
    return new JdbcCursorItemReaderBuilder<Customer>()
            .name("optimizedReader")
            .dataSource(dataSource)
            .sql("SELECT * FROM customers WHERE status = 'ACTIVE'")
            .fetchSize(1000)  // DB에서 한 번에 가져올 행 수
            .rowMapper(new BeanPropertyRowMapper<>(Customer.class))
            .build();
}
```

---

## 4. 실전 예제

### Chunk Size / Fetch Size 최적 조합

일반적으로 Fetch Size >= Chunk Size로 설정하는 것이 좋다. Chunk Size보다 작으면 한 Chunk를 처리하는 동안 여러 번 DB를 왕복하게 된다.

```java
@Bean
public Step optimizedStep(JobRepository jobRepository,
                          PlatformTransactionManager transactionManager,
                          DataSource dataSource) {
    return new StepBuilder("optimizedStep", jobRepository)
            .<Customer, CustomerDto>chunk(500, transactionManager)  // Chunk Size = 500
            .reader(
                new JdbcCursorItemReaderBuilder<Customer>()
                    .name("reader")
                    .dataSource(dataSource)
                    .sql("SELECT * FROM customers WHERE status = 'ACTIVE'")
                    .fetchSize(1000)  // Fetch Size >= Chunk Size
                    .rowMapper(new BeanPropertyRowMapper<>(Customer.class))
                    .build()
            )
            .processor(processor())
            .writer(jdbcWriter())
            .listener(new ChunkPerformanceListener())  // 성능 모니터링
            .build();
}
```

### Chunk Size 권장값 가이드

| 시나리오 | 권장 Chunk Size | 이유 |
|---------|----------------|------|
| 단순 DB to DB 복사 | 500~1000 | I/O 비용이 적고 벌크 INSERT가 효율적 |
| 외부 API 호출 포함 | 10~50 | API 호출 실패 시 재처리 범위 최소화 |
| 파일 읽기/쓰기 | 100~500 | 파일 I/O 버퍼 최적화 |
| 복잡한 변환 로직 | 50~200 | 메모리 사용량과 처리 시간 균형 |

---

## 5. 정리

| 항목 | 설명 |
|------|------|
| **트랜잭션 단위** | 각 Chunk가 하나의 트랜잭션 |
| **commit-interval** | `chunk(size)`의 size가 곧 커밋 주기 |
| **Reader 트랜잭션 분리** | `readerIsTransactionalQueue()`로 Reader를 TX 외부에서 실행 |
| **롤백 제어** | `faultTolerant().noRollback(Exception.class)` |
| **Fetch Size** | DB 네트워크 왕복 최소화, Chunk Size 이상으로 설정 권장 |
| **성능 모니터링** | ChunkListener로 Chunk별 소요시간 측정 |
| **Chunk Size 트레이드오프** | 작으면 안전하지만 느림, 크면 빠르지만 실패 범위 증가 |

---
*참고: Spring Batch 5.x / Spring Boot 3.x 기준*
