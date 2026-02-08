# Spring Batch 완벽 가이드

Spring Batch의 기초부터 대용량 처리, 실무 베스트 프랙티스까지 총정리한다.

## 목차

- [1. Spring Batch 기초](#1-spring-batch-기초)
  - [1.1 Spring Batch란?](#11-spring-batch란)
  - [1.2 핵심 도메인 모델](#12-핵심-도메인-모델)
  - [1.3 Job 구성하기](#13-job-구성하기)
  - [1.4 Step 구성하기](#14-step-구성하기)
  - [1.5 실행 흐름 제어](#15-실행-흐름-제어)
  - [1.6 JobRepository와 메타데이터](#16-jobrepository와-메타데이터)
  - [1.7 Job 실행하기](#17-job-실행하기)
- [2. Chunk 처리](#2-chunk-처리)
  - [2.1 Chunk 처리 개념](#21-chunk-처리-개념)
  - [2.2 ItemReader](#22-itemreader)
  - [2.3 ItemProcessor](#23-itemprocessor)
  - [2.4 ItemWriter](#24-itemwriter)
  - [2.5 트랜잭션 관리](#25-트랜잭션-관리)
  - [2.6 Chunk 성능 최적화](#26-chunk-성능-최적화)
- [3. ExecutionContext](#3-executioncontext)
  - [3.1 ExecutionContext란?](#31-executioncontext란)
  - [3.2 Job vs Step ExecutionContext](#32-job-vs-step-executioncontext)
  - [3.3 데이터 저장과 조회](#33-데이터-저장과-조회)
  - [3.4 SpEL을 통한 Late Binding](#34-spel을-통한-late-binding)
  - [3.5 ItemStream 인터페이스](#35-itemstream-인터페이스)
  - [3.6 재시작 시나리오](#36-재시작-시나리오)
  - [3.7 lastProcessedId 심화](#37-lastprocessedid-심화)
  - [3.8 ExecutionContext 실무 활용 패턴](#38-executioncontext-실무-활용-패턴)
  - [3.9 ExecutionContext 주의사항](#39-executioncontext-주의사항)
- [4. 고급 활용](#4-고급-활용)
  - [4.1 Flow를 이용한 조건부 흐름 제어](#41-flow를-이용한-조건부-흐름-제어)
  - [4.2 JobExecutionDecider](#42-jobexecutiondecider)
  - [4.3 Skip 설정](#43-skip-설정)
  - [4.4 Retry 설정](#44-retry-설정)
  - [4.5 Skip + Retry 조합](#45-skip--retry-조합)
  - [4.6 Listener 총정리](#46-listener-총정리)
- [5. 비동기 및 대용량 처리](#5-비동기-및-대용량-처리)
  - [5.1 처리 방식 비교](#51-처리-방식-비교)
  - [5.2 Multi-threaded Step](#52-multi-threaded-step)
  - [5.3 AsyncItemProcessor/Writer](#53-asyncitemprocessorwriter)
  - [5.4 병렬 Step 실행 (Split)](#54-병렬-step-실행-split)
  - [5.5 Partitioning](#55-partitioning)
  - [5.6 원격 청킹 (Remote Chunking)](#56-원격-청킹-remote-chunking)
  - [5.7 원격 파티셔닝 (Remote Partitioning)](#57-원격-파티셔닝-remote-partitioning)
  - [5.8 JobLauncher와 비동기 실행](#58-joblauncher와-비동기-실행)
  - [5.9 비동기 처리 주의사항](#59-비동기-처리-주의사항)
- [6. 실무 베스트 프랙티스](#6-실무-베스트-프랙티스)
  - [6.1 정기 결제 배치 설계](#61-정기-결제-배치-설계)
  - [6.2 정산 배치 설계](#62-정산-배치-설계)
  - [6.3 공통 베스트 프랙티스](#63-공통-베스트-프랙티스)
  - [6.4 장애 대응 패턴](#64-장애-대응-패턴)
  - [6.5 운영 체크리스트](#65-운영-체크리스트)

---

# 1. Spring Batch 기초

## 1.1 Spring Batch란?

Spring Batch는 대용량 데이터 처리를 위한 경량 배치 프레임워크다. 로깅, 트랜잭션 관리, 재시작, 건너뛰기, 리소스 관리 등 배치 처리에 필수적인 기능을 제공한다.

**주요 사용 사례:**
- 대량 데이터 ETL (Extract-Transform-Load)
- 정산/결제 처리
- 대용량 파일 처리
- 주기적인 데이터 마이그레이션

---

## 1.2 핵심 도메인 모델

```
┌─────────────────────────────────────────────────────────┐
│                         Job                              │
│  ┌─────────┐   ┌─────────┐   ┌─────────┐               │
│  │  Step1  │──▶│  Step2  │──▶│  Step3  │               │
│  └─────────┘   └─────────┘   └─────────┘               │
└─────────────────────────────────────────────────────────┘
         │                           │
         ▼                           ▼
┌─────────────────┐         ┌─────────────────┐
│  JobExecution   │         │  StepExecution  │
└─────────────────┘         └─────────────────┘
         │                           │
         └───────────┬───────────────┘
                     ▼
            ┌─────────────────┐
            │  JobRepository  │
            └─────────────────┘
```

| 개념 | 설명 |
|------|------|
| **Job** | 배치 처리의 최상위 단위. 여러 Step으로 구성 |
| **Step** | Job 내의 독립적인 처리 단계 |
| **JobInstance** | Job의 논리적 실행 단위 (Job + JobParameters) |
| **JobExecution** | JobInstance의 실제 실행 시도 |
| **StepExecution** | Step의 실제 실행 시도 |
| **ExecutionContext** | 실행 중 상태를 저장하는 키-값 저장소 |

---

## 1.3 Job 구성하기

### 기본 Job 설정

```java
@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Bean
    public Job sampleJob(JobRepository jobRepository, Step step1, Step step2) {
        return new JobBuilder("sampleJob", jobRepository)
                .start(step1)
                .next(step2)
                .build();
    }
}
```

### JobParameters 활용

```java
@Bean
public Job parameterizedJob(JobRepository jobRepository, Step step1) {
    return new JobBuilder("parameterizedJob", jobRepository)
            .start(step1)
            .incrementer(new RunIdIncrementer())  // 매번 새 인스턴스 생성
            .validator(new DefaultJobParametersValidator(
                    new String[]{"inputFile"},     // 필수 파라미터
                    new String[]{"outputFile"}     // 선택 파라미터
            ))
            .build();
}

// 파라미터 사용
@Bean
@StepScope
public FlatFileItemReader<String> reader(
        @Value("#{jobParameters['inputFile']}") String inputFile) {
    return new FlatFileItemReaderBuilder<String>()
            .name("fileReader")
            .resource(new FileSystemResource(inputFile))
            .lineMapper(new PassThroughLineMapper())
            .build();
}
```

---

## 1.4 Step 구성하기

### Tasklet 기반 Step

단순한 단일 작업에 적합하다.

```java
@Bean
public Step cleanupStep(JobRepository jobRepository,
                        PlatformTransactionManager transactionManager) {
    return new StepBuilder("cleanupStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                Files.deleteIfExists(Path.of("/tmp/batch-temp.dat"));
                log.info("임시 파일 정리 완료");
                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
}
```

### Chunk 기반 Step

대량 데이터 처리에 적합하다. (상세 내용은 [2. Chunk 처리](#2-chunk-처리) 참고)

```java
@Bean
public Step chunkStep(JobRepository jobRepository,
                      PlatformTransactionManager transactionManager,
                      ItemReader<Input> reader,
                      ItemProcessor<Input, Output> processor,
                      ItemWriter<Output> writer) {
    return new StepBuilder("chunkStep", jobRepository)
            .<Input, Output>chunk(100, transactionManager)  // 100개씩 처리
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
}
```

---

## 1.5 실행 흐름 제어

### 순차 실행

```java
@Bean
public Job sequentialJob(JobRepository jobRepository,
                         Step step1, Step step2, Step step3) {
    return new JobBuilder("sequentialJob", jobRepository)
            .start(step1)
            .next(step2)
            .next(step3)
            .build();
}
```

### 조건부 실행

```java
@Bean
public Job conditionalJob(JobRepository jobRepository,
                          Step step1, Step successStep, Step failStep) {
    return new JobBuilder("conditionalJob", jobRepository)
            .start(step1)
                .on("COMPLETED").to(successStep)  // 성공 시
                .from(step1)
                .on("FAILED").to(failStep)        // 실패 시
                .from(step1)
                .on("*").stop()                   // 그 외 중단
            .end()
            .build();
}
```

### 커스텀 ExitStatus로 분기

```java
@Bean
public Step decisionStep(JobRepository jobRepository,
                         PlatformTransactionManager transactionManager) {
    return new StepBuilder("decisionStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                int count = getRecordCount();
                if (count > 1000) {
                    contribution.setExitStatus(new ExitStatus("LARGE_DATASET"));
                } else {
                    contribution.setExitStatus(new ExitStatus("SMALL_DATASET"));
                }
                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
}

@Bean
public Job branchingJob(JobRepository jobRepository,
                        Step decisionStep, Step largeStep, Step smallStep) {
    return new JobBuilder("branchingJob", jobRepository)
            .start(decisionStep)
                .on("LARGE_DATASET").to(largeStep)
                .from(decisionStep)
                .on("SMALL_DATASET").to(smallStep)
            .end()
            .build();
}
```

---

## 1.6 JobRepository와 메타데이터

### 메타데이터 테이블 구조

Spring Batch는 실행 상태를 다음 테이블에 저장한다:

| 테이블명 | 설명 |
|---------|------|
| BATCH_JOB_INSTANCE | Job 인스턴스 정보 |
| BATCH_JOB_EXECUTION | Job 실행 이력 |
| BATCH_JOB_EXECUTION_PARAMS | Job 파라미터 |
| BATCH_JOB_EXECUTION_CONTEXT | Job ExecutionContext |
| BATCH_STEP_EXECUTION | Step 실행 이력 |
| BATCH_STEP_EXECUTION_CONTEXT | Step ExecutionContext |

### JobRepository 설정

```java
@Configuration
public class BatchInfraConfig {

    @Bean
    public JobRepository jobRepository(DataSource dataSource,
                                        PlatformTransactionManager transactionManager)
            throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTransactionManager(transactionManager);
        factory.setDatabaseType("MYSQL");
        factory.setTablePrefix("BATCH_");
        factory.setIsolationLevelForCreate("ISOLATION_SERIALIZABLE");
        factory.afterPropertiesSet();
        return factory.getObject();
    }
}
```

---

## 1.7 Job 실행하기

### CommandLineJobRunner

```bash
java -jar my-batch.jar \
  --spring.batch.job.name=sampleJob \
  inputFile=/data/input.csv \
  date=2024-01-15
```

### JobLauncher를 통한 프로그래밍 방식

```java
@Service
@RequiredArgsConstructor
public class BatchJobService {

    private final JobLauncher jobLauncher;
    private final Job sampleJob;

    public void runJob(String inputFile) throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addString("inputFile", inputFile)
                .addLocalDateTime("runTime", LocalDateTime.now())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(sampleJob, params);
        log.info("Job 실행 결과: {}", execution.getStatus());
    }
}
```

### 스케줄러 연동

```java
@Component
@RequiredArgsConstructor
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job dailyJob;

    @Scheduled(cron = "0 0 2 * * *")  // 매일 새벽 2시
    public void runDailyBatch() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLocalDateTime("scheduledTime", LocalDateTime.now())
                    .toJobParameters();
            jobLauncher.run(dailyJob, params);
        } catch (Exception e) {
            log.error("스케줄된 배치 실패", e);
        }
    }
}
```

---

# 2. Chunk 처리

## 2.1 Chunk 처리 개념

Chunk 처리는 데이터를 일정 단위(chunk)로 나누어 처리하는 방식이다.

```
┌──────────────────────────────────────────────────────────────┐
│                      Chunk Processing                         │
│                                                               │
│   ┌─────────┐      ┌───────────┐      ┌─────────┐           │
│   │ Reader  │─────▶│ Processor │─────▶│ Writer  │           │
│   └─────────┘      └───────────┘      └─────────┘           │
│       │                 │                  │                 │
│       │    chunk-size   │    chunk-size    │                 │
│       │◀───────────────▶│◀────────────────▶│                 │
│       │     (1개씩)      │     (1개씩)       │   (chunk 단위)  │
│                                                               │
│   [트랜잭션 시작] ──────────────────────── [커밋] ────────────│
└──────────────────────────────────────────────────────────────┘
```

**Chunk Size 선택 기준:**

| Chunk Size | 적합한 상황 |
|------------|------------|
| 10~50 | 개별 아이템 처리 시간이 긴 경우 |
| 100~500 | 일반적인 데이터 처리 |
| 1000+ | 단순 데이터 이관, I/O 최적화 필요 시 |

---

## 2.2 ItemReader

데이터 소스에서 아이템을 하나씩 읽어오는 역할을 한다.

### FlatFileItemReader (파일)

```java
@Bean
public FlatFileItemReader<Customer> fileReader() {
    return new FlatFileItemReaderBuilder<Customer>()
            .name("customerReader")
            .resource(new ClassPathResource("customers.csv"))
            .linesToSkip(1)  // 헤더 스킵
            .delimited()
            .delimiter(",")
            .names("id", "name", "email", "age")
            .targetType(Customer.class)
            .build();
}

// 고정 길이 파일
@Bean
public FlatFileItemReader<Customer> fixedLengthReader() {
    return new FlatFileItemReaderBuilder<Customer>()
            .name("fixedLengthReader")
            .resource(new ClassPathResource("customers.dat"))
            .fixedLength()
            .columns(new Range(1, 10), new Range(11, 30), new Range(31, 50))
            .names("id", "name", "email")
            .targetType(Customer.class)
            .build();
}
```

### JdbcCursorItemReader (DB - Cursor)

```java
@Bean
public JdbcCursorItemReader<Customer> cursorReader(DataSource dataSource) {
    return new JdbcCursorItemReaderBuilder<Customer>()
            .name("customerCursorReader")
            .dataSource(dataSource)
            .sql("SELECT id, name, email, age FROM customers WHERE status = ?")
            .preparedStatementSetter(ps -> ps.setString(1, "ACTIVE"))
            .rowMapper((rs, rowNum) -> Customer.builder()
                    .id(rs.getLong("id"))
                    .name(rs.getString("name"))
                    .email(rs.getString("email"))
                    .age(rs.getInt("age"))
                    .build())
            .build();
}
```

### JdbcPagingItemReader (DB - Paging)

대용량 데이터 처리에 적합하다. 메모리 효율적.

```java
@Bean
public JdbcPagingItemReader<Customer> pagingReader(DataSource dataSource) {
    Map<String, Order> sortKeys = new HashMap<>();
    sortKeys.put("id", Order.ASCENDING);

    return new JdbcPagingItemReaderBuilder<Customer>()
            .name("customerPagingReader")
            .dataSource(dataSource)
            .selectClause("SELECT id, name, email, age")
            .fromClause("FROM customers")
            .whereClause("WHERE status = :status")
            .parameterValues(Map.of("status", "ACTIVE"))
            .sortKeys(sortKeys)
            .pageSize(100)
            .rowMapper(new BeanPropertyRowMapper<>(Customer.class))
            .build();
}
```

### JpaPagingItemReader (JPA)

```java
@Bean
public JpaPagingItemReader<Customer> jpaReader(EntityManagerFactory emf) {
    return new JpaPagingItemReaderBuilder<Customer>()
            .name("customerJpaReader")
            .entityManagerFactory(emf)
            .queryString("SELECT c FROM Customer c WHERE c.status = :status")
            .parameterValues(Map.of("status", Status.ACTIVE))
            .pageSize(100)
            .build();
}

// Spring Data Repository 사용 시
@Bean
public RepositoryItemReader<Customer> repositoryReader(CustomerRepository repository) {
    return new RepositoryItemReaderBuilder<Customer>()
            .name("repositoryReader")
            .repository(repository)
            .methodName("findByStatus")
            .arguments(Status.ACTIVE)
            .pageSize(100)
            .sorts(Map.of("id", Sort.Direction.ASC))
            .build();
}
```

### 커스텀 ItemReader

```java
@Component
public class ApiItemReader implements ItemReader<ApiData> {

    private final ApiClient apiClient;
    private Iterator<ApiData> dataIterator;
    private boolean initialized = false;

    @Override
    public ApiData read() {
        if (!initialized) {
            List<ApiData> data = apiClient.fetchAll();
            dataIterator = data.iterator();
            initialized = true;
        }

        if (dataIterator.hasNext()) {
            return dataIterator.next();
        }
        return null;  // null 반환 시 읽기 종료
    }
}
```

### @StepScope로 Late Binding

```java
@Bean
@StepScope
public FlatFileItemReader<Customer> scopedReader(
        @Value("#{jobParameters['inputFile']}") String inputFile,
        @Value("#{stepExecutionContext['minId']}") Long minId) {

    return new FlatFileItemReaderBuilder<Customer>()
            .name("scopedReader")
            .resource(new FileSystemResource(inputFile))
            .delimited()
            .names("id", "name", "email")
            .targetType(Customer.class)
            .build();
}
```

---

## 2.3 ItemProcessor

읽은 데이터를 변환하거나 필터링하는 역할을 한다.

### 기본 Processor

```java
@Bean
public ItemProcessor<Customer, CustomerDto> processor() {
    return customer -> {
        // null 반환 시 해당 아이템 필터링 (Writer로 전달 안 됨)
        if (!customer.isActive()) {
            return null;
        }

        return CustomerDto.builder()
                .id(customer.getId())
                .fullName(customer.getFirstName() + " " + customer.getLastName())
                .email(customer.getEmail().toLowerCase())
                .build();
    };
}
```

### ValidatingItemProcessor

```java
@Bean
public ValidatingItemProcessor<Customer> validatingProcessor() {
    ValidatingItemProcessor<Customer> processor = new ValidatingItemProcessor<>();
    processor.setValidator(new SpringValidator<>(customerValidator()));
    processor.setFilter(true);  // 유효성 검사 실패 시 필터링 (예외 대신)
    return processor;
}
```

### CompositeItemProcessor (체이닝)

```java
@Bean
public CompositeItemProcessor<Customer, CustomerDto> compositeProcessor() {
    return new CompositeItemProcessorBuilder<Customer, CustomerDto>()
            .delegates(
                    validationProcessor(),   // 1. 유효성 검사
                    enrichmentProcessor(),   // 2. 데이터 보강
                    transformProcessor()     // 3. DTO 변환
            )
            .build();
}
```

### ClassifierCompositeItemProcessor (조건부 처리)

```java
@Bean
public ClassifierCompositeItemProcessor<Customer, CustomerDto> classifierProcessor() {
    ClassifierCompositeItemProcessor<Customer, CustomerDto> processor =
            new ClassifierCompositeItemProcessor<>();

    processor.setClassifier(customer -> {
        if (customer.getType() == CustomerType.PREMIUM) {
            return premiumProcessor();
        } else {
            return standardProcessor();
        }
    });

    return processor;
}
```

---

## 2.4 ItemWriter

처리된 데이터를 출력하는 역할을 한다. Chunk 단위로 호출된다.

### FlatFileItemWriter (파일)

```java
@Bean
public FlatFileItemWriter<CustomerDto> fileWriter() {
    return new FlatFileItemWriterBuilder<CustomerDto>()
            .name("customerWriter")
            .resource(new FileSystemResource("output/customers.csv"))
            .headerCallback(writer -> writer.write("ID,NAME,EMAIL"))
            .footerCallback(writer -> writer.write("--- END OF FILE ---"))
            .delimited()
            .delimiter(",")
            .names("id", "fullName", "email")
            .build();
}
```

### JdbcBatchItemWriter (DB)

```java
@Bean
public JdbcBatchItemWriter<CustomerDto> jdbcWriter(DataSource dataSource) {
    return new JdbcBatchItemWriterBuilder<CustomerDto>()
            .dataSource(dataSource)
            .sql("INSERT INTO customers_backup (id, name, email, created_at) " +
                 "VALUES (:id, :fullName, :email, :createdAt)")
            .beanMapped()
            .build();
}
```

### JpaItemWriter (JPA)

```java
@Bean
public JpaItemWriter<CustomerEntity> jpaWriter(EntityManagerFactory emf) {
    JpaItemWriter<CustomerEntity> writer = new JpaItemWriter<>();
    writer.setEntityManagerFactory(emf);
    writer.setUsePersist(true);  // persist() 사용 (기본: merge())
    return writer;
}

// Spring Data JPA Repository 사용
@Bean
public RepositoryItemWriter<CustomerEntity> repositoryWriter(
        CustomerRepository repository) {
    return new RepositoryItemWriterBuilder<CustomerEntity>()
            .repository(repository)
            .methodName("save")
            .build();
}
```

### CompositeItemWriter (다중 출력)

```java
@Bean
public CompositeItemWriter<CustomerDto> compositeWriter() {
    return new CompositeItemWriterBuilder<CustomerDto>()
            .delegates(
                    jdbcWriter(),    // DB 저장
                    fileWriter(),    // 파일 출력
                    kafkaWriter()    // Kafka 전송
            )
            .build();
}
```

### ClassifierCompositeItemWriter (조건부 출력)

```java
@Bean
public ClassifierCompositeItemWriter<CustomerDto> classifierWriter() {
    ClassifierCompositeItemWriter<CustomerDto> writer =
            new ClassifierCompositeItemWriter<>();

    writer.setClassifier(customer -> {
        if (customer.getCountry().equals("KR")) {
            return koreanDbWriter();
        } else {
            return globalDbWriter();
        }
    });

    return writer;
}
```

### 커스텀 ItemWriter

```java
@Component
public class ApiItemWriter implements ItemWriter<CustomerDto> {

    private final ApiClient apiClient;

    @Override
    public void write(Chunk<? extends CustomerDto> items) {
        List<CustomerDto> customers = new ArrayList<>(items.getItems());
        apiClient.bulkCreate(customers);
        log.info("{}건 API 전송 완료", customers.size());
    }
}
```

---

## 2.5 트랜잭션 관리

### Chunk 단위 트랜잭션

기본적으로 각 Chunk는 하나의 트랜잭션으로 처리된다.

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

### Reader 트랜잭션 분리

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

### 롤백 제어

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

---

## 2.6 Chunk 성능 최적화

### Chunk Size 튜닝

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

### Reader Fetch Size

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

# 3. ExecutionContext

## 3.1 ExecutionContext란?

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

## 3.2 Job vs Step ExecutionContext

두 종류의 ExecutionContext가 있으며, **범위(scope)**가 다르다.

```
┌─────────────────────────────────────────────────────────────────┐
│                           Job                                    │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │              JobExecutionContext                         │   │
│   │              (Job 전체에서 공유)                          │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                  │
│   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐       │
│   │    Step 1     │   │    Step 2     │   │    Step 3     │       │
│   │ ┌──────────┐ │   │ ┌──────────┐ │   │ ┌──────────┐ │       │
│   │ │StepExec  │ │   │ │StepExec  │ │   │ │StepExec  │ │       │
│   │ │Context   │ │   │ │Context   │ │   │ │Context   │ │       │
│   │ └──────────┘ │   │ └──────────┘ │   │ └──────────┘ │       │
│   └──────────────┘   └──────────────┘   └──────────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

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

## 3.3 데이터 저장과 조회

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
Checkpoint checkpoint = (Checkpoint) context.get("checkpoint");

// 주의: 객체는 Serializable 구현 필수
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
```

---

## 3.4 SpEL을 통한 Late Binding

**@StepScope**와 **SpEL(Spring Expression Language)**을 조합하면, Step 실행 시점에 ExecutionContext 값을 주입받을 수 있다.

```java
@Bean
@StepScope  // 필수! Step 실행 시점에 빈 생성
public ItemReader<Customer> reader(
        @Value("#{stepExecutionContext['lastProcessedId']}") Long lastId,
        @Value("#{jobExecutionContext['globalSetting']}") String setting,
        @Value("#{jobParameters['inputFile']}") String inputFile) {
    // lastId, setting, inputFile 사용
}
```

### null 안전 처리 (Elvis 연산자)

```java
// 방법 1: Elvis 연산자 (?:)
@Value("#{stepExecutionContext['lastId'] ?: 0}")
Long lastId;  // null이면 0

// 방법 2: 코드에서 처리
@Bean
@StepScope
public ItemReader<Customer> reader(
        @Value("#{stepExecutionContext['lastId']}") Long lastId) {
    long startId = Optional.ofNullable(lastId).orElse(0L);
    // ...
}
```

---

## 3.5 ItemStream 인터페이스

**ItemStream**은 Reader/Writer가 ExecutionContext와 상호작용하는 표준 인터페이스다.

```java
public interface ItemStream {
    void open(ExecutionContext executionContext);    // Step 시작 시 - 상태 복구
    void update(ExecutionContext executionContext);  // Chunk 완료 시 - 상태 저장
    void close();                                    // Step 종료 시 - 리소스 정리
}
```

### 동작 흐름

```
Step 시작
    │
    ▼
  open(executionContext) - DB에서 이전 상태 로드
    │
    ▼
  Chunk 1: Read -> Process -> Write
    └─▶ update(executionContext) - 진행 상태 저장
  Chunk 2: Read -> Process -> Write
    └─▶ update(executionContext) - 진행 상태 저장
  ...반복...
    │
    ▼
  close() - 리소스 정리
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
        if (executionContext.containsKey(LAST_OFFSET_KEY)) {
            this.currentOffset = executionContext.getInt(LAST_OFFSET_KEY);
            this.totalRead = executionContext.getInt(TOTAL_READ_KEY);
            log.info("이전 상태 복구 - offset: {}, totalRead: {}",
                    currentOffset, totalRead);
        }
    }

    @Override
    public ApiData read() {
        if (bufferIndex >= buffer.size()) {
            buffer = apiClient.fetchData(currentOffset, 100);
            bufferIndex = 0;
            currentOffset += 100;
            if (buffer.isEmpty()) {
                return null;
            }
        }
        totalRead++;
        return buffer.get(bufferIndex++);
    }

    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.putInt(LAST_OFFSET_KEY, currentOffset);
        executionContext.putInt(TOTAL_READ_KEY, totalRead);
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

## 3.6 재시작 시나리오

### 시나리오 1: 정상 재시작

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

### 시나리오 2: saveState(false) 사용 시

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

### 시나리오 3: Step 간 데이터 전달

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

### ExecutionContextPromotionListener

StepExecutionContext의 특정 키를 JobExecutionContext로 승격시킨다.

```java
@Bean
public ExecutionContextPromotionListener promotionListener() {
    ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
    listener.setKeys(new String[]{"totalCount", "errorCount"});
    return listener;
}
```

---

## 3.7 lastProcessedId 심화

### 어떤 값을 기준으로 선택하나?

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

### 저장되는 시점

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

### 구현 방법 A: ItemStream 직접 구현 (권장)

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

### 구현 방법 B: ChunkListener 사용

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

### 구현 방법 C: 내장 Reader 자동 저장

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

## 3.8 ExecutionContext 실무 활용 패턴

### 패턴 1: 진행률 추적

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

---

## 3.9 ExecutionContext 주의사항

### 1. 직렬화 가능한 데이터만 저장

```java
// 잘못된 예: 직렬화 불가능한 객체
context.put("connection", dataSource.getConnection());  // 안 됨!

// 올바른 예
context.putString("connectionInfo", "jdbc:mysql://...");
context.putLong("lastId", 12345L);
```

### 2. 큰 데이터 저장 금지

```java
// 잘못된 예: 대용량 데이터 저장
context.put("customers", repository.findAll());  // 메타데이터 테이블 폭발!

// 올바른 예: ID만 저장
context.putLong("lastCustomerId", 999999L);
```

### 3. 멀티스레드 환경에서는 saveState(false) 필수

```java
@Bean
public JdbcPagingItemReader<Customer> reader() {
    return new JdbcPagingItemReaderBuilder<Customer>()
            .name("reader")
            .saveState(false)  // 멀티스레드에서는 필수!
            .build();
}
```

### 4. Key 이름 충돌 방지

```java
// 잘못된 예: 일반적인 이름
context.put("count", 100);  // 다른 컴포넌트와 충돌 가능

// 올바른 예: 네임스페이스 사용
context.put("payment.processor.count", 100);
```

### 5. null 값 처리

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

# 4. 고급 활용

## 4.1 Flow를 이용한 조건부 흐름 제어

Flow는 여러 Step을 논리적으로 그룹화하고, 조건에 따라 분기 처리할 수 있게 해준다.

### 기본 Flow 정의

```java
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
```

### 조건부 Flow

```java
@Bean
public Job conditionalFlowJob() {
    return new JobBuilder("conditionalFlowJob", jobRepository)
            .start(step1())
                .on("COMPLETED").to(step2())
                .from(step1())
                .on("FAILED").to(failStep())
                .from(step1())
                .on("*").to(step3())
            .end()
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

---

## 4.2 JobExecutionDecider

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

---

## 4.3 Skip 설정

특정 예외 발생 시 해당 아이템을 건너뛰고 계속 진행한다.

```java
@Bean
public Step skipStep(JobRepository jobRepository,
                     PlatformTransactionManager transactionManager) {
    return new StepBuilder("skipStep", jobRepository)
            .<Customer, CustomerDto>chunk(100, transactionManager)
            .reader(reader())
            .processor(processor())
            .writer(writer())
            .faultTolerant()
            .skip(ValidationException.class)
            .skip(DuplicateKeyException.class)
            .skipLimit(100)
            .skipPolicy(new AlwaysSkipItemSkipPolicy())  // 커스텀 정책
            .noSkip(FileNotFoundException.class)  // 이 예외는 스킵하지 않음
            .listener(new SkipListener<Customer, CustomerDto>() {
                @Override
                public void onSkipInRead(Throwable t) {
                    log.warn("읽기 중 스킵: {}", t.getMessage());
                }
                @Override
                public void onSkipInProcess(Customer item, Throwable t) {
                    log.warn("처리 중 스킵 - ID: {}, 오류: {}",
                            item.getId(), t.getMessage());
                }
                @Override
                public void onSkipInWrite(CustomerDto item, Throwable t) {
                    log.warn("쓰기 중 스킵 - ID: {}", item.getId());
                }
            })
            .build();
}
```

---

## 4.4 Retry 설정

일시적 오류에 대해 재시도한다.

```java
@Bean
public Step retryStep(JobRepository jobRepository,
                      PlatformTransactionManager transactionManager) {
    return new StepBuilder("retryStep", jobRepository)
            .<Customer, CustomerDto>chunk(100, transactionManager)
            .reader(reader())
            .processor(processor())
            .writer(writer())
            .faultTolerant()
            .retry(DeadlockLoserDataAccessException.class)
            .retry(OptimisticLockingFailureException.class)
            .retryLimit(3)
            .retryPolicy(new SimpleRetryPolicy(3,
                    Map.of(TransientDataAccessException.class, true)))
            .backOffPolicy(new ExponentialBackOffPolicy())  // 지수 백오프
            .noRetry(ValidationException.class)  // 이 예외는 재시도하지 않음
            .listener(new RetryListener() {
                @Override
                public <T, E extends Throwable> void onError(
                        RetryContext context, RetryCallback<T, E> callback, Throwable t) {
                    log.warn("재시도 #{}: {}", context.getRetryCount(), t.getMessage());
                }
            })
            .build();
}
```

### RetryTemplate을 이용한 세밀한 제어

```java
@Bean
public RetryPolicy retryPolicy() {
    Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
    retryableExceptions.put(TransientDataAccessException.class, true);
    retryableExceptions.put(DeadlockLoserDataAccessException.class, true);
    return new SimpleRetryPolicy(3, retryableExceptions, true);
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

---

## 4.5 Skip + Retry 조합

```java
@Bean
public Step robustStep(JobRepository jobRepository,
                       PlatformTransactionManager transactionManager) {
    return new StepBuilder("robustStep", jobRepository)
            .<Customer, CustomerDto>chunk(100, transactionManager)
            .reader(reader())
            .processor(processor())
            .writer(writer())
            .faultTolerant()
            // Retry 설정
            .retry(TransientDataAccessException.class)
            .retryLimit(3)
            // Skip 설정 (재시도 후에도 실패하면 스킵)
            .skip(Exception.class)
            .skipLimit(10)
            .noSkip(FileNotFoundException.class)
            .noRetry(ValidationException.class)
            .build();
}
```

---

## 4.6 Listener 총정리

### JobExecutionListener

```java
@Bean
public Job jobWithListener() {
    return new JobBuilder("jobWithListener", jobRepository)
            .start(step1())
            .listener(new JobExecutionListener() {
                @Override
                public void beforeJob(JobExecution jobExecution) {
                    log.info("Job 시작: {}", jobExecution.getJobInstance().getJobName());
                }
                @Override
                public void afterJob(JobExecution jobExecution) {
                    if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                        log.info("Job 완료! 처리 시간: {}ms",
                                jobExecution.getEndTime().toEpochMilli()
                                - jobExecution.getStartTime().toEpochMilli());
                    } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
                        log.error("Job 실패: {}",
                                jobExecution.getAllFailureExceptions());
                    }
                }
            })
            .build();
}
```

### StepExecutionListener

```java
@Component
public class StepLoggingListener implements StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("Step 시작: {}", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("Step 완료 - 읽기: {}, 쓰기: {}, 건너뛰기: {}",
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getSkipCount());

        if (stepExecution.getSkipCount() > 100) {
            return new ExitStatus("COMPLETED WITH SKIPS");
        }
        return stepExecution.getExitStatus();
    }
}
```

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
                    log.info("Chunk 시작");
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
    public void afterRead(User item) {
        log.debug("읽음: {}", item.getId());
    }

    @Override
    public void onReadError(Exception ex) {
        log.error("읽기 에러: {}", ex.getMessage());
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

---

# 5. 비동기 및 대용량 처리

## 5.1 처리 방식 비교

| 방식 | 적합한 상황 | 장점 | 단점 |
|------|------------|------|------|
| **단일 스레드** | 소량 데이터, 순서 중요 | 단순, 디버깅 용이 | 느림 |
| **멀티스레드 Step** | 중간 규모, 순서 무관 | 구현 간단 | Reader thread-safe 필요 |
| **비동기 Processor** | 외부 API 호출 많음 | I/O 대기 최소화 | 복잡도 증가 |
| **파티셔닝** | 대용량, 명확한 분할 기준 | 확장성 최고 | 파티셔닝 로직 필요 |
| **Split (병렬 Flow)** | 독립적인 Step 동시 실행 | 구현 간단 | 제한적 확장성 |
| **원격 청킹** | 처리가 무거운 경우 | 처리 분산 | 인프라 복잡 |
| **원격 파티셔닝** | 초대용량, 분산 환경 | 완전한 분산 | 가장 복잡 |

| 구분 | Split | Partition | Remote Partition | Remote Chunking | Async | Multi-thread |
|------|-------|-----------|------------------|-----------------|-------|--------------|
| **병렬화 대상** | Flow | 데이터 | 데이터 | Chunk | Processor | Chunk |
| **실행 환경** | 단일 JVM | 단일 JVM | 다중 JVM | 다중 JVM | 단일 JVM | 단일 JVM |
| **데이터 전송** | X | X | 메타데이터 | 실제 데이터 | X | X |
| **재시작** | 용이 | 용이 | 용이 | 어려움 | 용이 | 어려움 |

---

## 5.2 Multi-threaded Step

하나의 Step을 여러 스레드로 병렬 처리한다.

```
┌────────────────────────────────────────────────────────────┐
│                    Multi-threaded Step                      │
│   ┌──────────────────────────────────────────────────┐     │
│   │                  TaskExecutor                      │     │
│   │   ┌─────────┐ ┌─────────┐ ┌─────────┐           │     │
│   │   │Thread 1 │ │Thread 2 │ │Thread 3 │ ...       │     │
│   │   │ R->P->W │ │ R->P->W │ │ R->P->W │           │     │
│   │   └─────────┘ └─────────┘ └─────────┘           │     │
│   └──────────────────────────────────────────────────┘     │
│                    Shared Reader (Thread-safe 필수!)        │
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

## 5.3 AsyncItemProcessor/Writer

Processor와 Writer를 비동기로 실행한다.

```
Reader -> AsyncProcessor(비동기 실행) -> Future<결과> -> AsyncWriter(Future에서 결과 추출) -> Writer
```

```java
@Bean
public Step asyncStep() {
    return new StepBuilder("asyncStep", jobRepository)
            .<Customer, Future<CustomerDto>>chunk(100, transactionManager)
            .reader(reader())
            .processor(asyncProcessor())
            .writer(asyncWriter())
            .build();
}

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

@Bean
public AsyncItemWriter<CustomerDto> asyncWriter() {
    AsyncItemWriter<CustomerDto> asyncWriter = new AsyncItemWriter<>();
    asyncWriter.setDelegate(writer());
    return asyncWriter;
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
```

---

## 5.4 병렬 Step 실행 (Split)

독립적인 여러 Step을 동시에 실행한다.

```
         ┌─────────┐
         │  Step1  │
         └────┬────┘
  ┌───────────┴───────────────┐
  ▼                           ▼
┌───────────┐           ┌───────────┐
│  Step2A   │  parallel │  Step2B   │
└─────┬─────┘           └─────┬─────┘
      └───────────┬───────────┘
                  ▼
             ┌─────────┐
             │  Step3  │
             └─────────┘
```

### Split을 사용한 병렬 실행

```java
@Bean
public Job parallelJob(JobRepository jobRepository,
                       Step step1, Step step2A, Step step2B, Step step3) {

    Flow flow2A = new FlowBuilder<SimpleFlow>("flow2A")
            .start(step2A).build();

    Flow flow2B = new FlowBuilder<SimpleFlow>("flow2B")
            .start(step2B).build();

    Flow splitFlow = new FlowBuilder<SimpleFlow>("splitFlow")
            .split(new SimpleAsyncTaskExecutor())
            .add(flow2A, flow2B)
            .build();

    return new JobBuilder("parallelJob", jobRepository)
            .start(step1)
            .next(splitFlow)
            .next(step3)  // 모든 병렬 Flow 완료 후 실행 (자동 동기화)
            .end()
            .build();
}
```

### 여러 Flow 병렬 실행

```java
@Bean
public Job multiFlowParallelJob(JobRepository jobRepository) {
    Flow customerFlow = new FlowBuilder<SimpleFlow>("customerFlow")
            .start(customerStep1()).next(customerStep2()).build();

    Flow orderFlow = new FlowBuilder<SimpleFlow>("orderFlow")
            .start(orderStep1()).next(orderStep2()).build();

    Flow productFlow = new FlowBuilder<SimpleFlow>("productFlow")
            .start(productStep()).build();

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

## 5.5 Partitioning

데이터를 여러 파티션으로 나눠 병렬 처리한다.

```
                  ┌──────────────┐
                  │ Manager Step │
                  │ (Partitioner)│
                  └──────┬───────┘
       ┌─────────────────┼───────────────┐
       ▼                 ▼               ▼
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│  Worker 1   │  │  Worker 2   │  │  Worker 3   │
│ ID: 1-1000  │  │ ID:1001-2000│  │ ID:2001-3000│
│   R->P->W   │  │   R->P->W   │  │   R->P->W   │
└─────────────┘  └─────────────┘  └─────────────┘
```

### Partitioner 구현

```java
public class ColumnRangePartitioner implements Partitioner {

    private final JdbcTemplate jdbcTemplate;
    private final String table;
    private final String column;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
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

### 파티션 Step 설정

```java
@Bean
public Step managerStep(JobRepository jobRepository,
                        Partitioner partitioner, Step workerStep) {
    return new StepBuilder("managerStep", jobRepository)
            .partitioner("workerStep", partitioner)
            .step(workerStep)
            .gridSize(4)
            .taskExecutor(partitionTaskExecutor())
            .build();
}

@Bean
@StepScope
public JdbcPagingItemReader<Customer> partitionedReader(
        @Value("#{stepExecutionContext['minId']}") Long minId,
        @Value("#{stepExecutionContext['maxId']}") Long maxId) {
    return new JdbcPagingItemReaderBuilder<Customer>()
            .name("partitionedReader")
            .dataSource(dataSource)
            .selectClause("SELECT *")
            .fromClause("FROM customers")
            .whereClause("WHERE id >= :minId AND id <= :maxId")
            .parameterValues(Map.of("minId", minId, "maxId", maxId))
            .sortKeys(Map.of("id", Order.ASCENDING))
            .pageSize(100)
            .rowMapper(new BeanPropertyRowMapper<>(Customer.class))
            .build();
}
```

### 날짜 기반 Partitioner

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

### 파일 기반 Partitioner

```java
public class MultiFilePartitioner implements Partitioner {

    private final Resource[] resources;

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

---

## 5.6 원격 청킹 (Remote Chunking)

Chunk 처리를 여러 워커 노드에 분산한다. Reader는 Master에서 실행하고, 처리할 데이터를 Worker로 전송한다.

```
┌─────────────────────┐
│     Master JVM      │
│  ┌────────────────┐ │
│  │ ItemReader     │ │
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
    public Step masterStep() {
        return masterStepBuilderFactory.get("masterStep")
                .<Order, Order>chunk(100)
                .reader(orderReader())
                .outputChannel(outboundRequests())
                .inputChannel(inboundReplies())
                .build();
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
}
```

---

## 5.7 원격 파티셔닝 (Remote Partitioning)

파티션을 여러 워커 노드에 분산한다. Worker가 데이터에 가까울 때 적합하다.

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
│ Full   │ │ Full   │
│ Step   │ │ Step   │
│(R->P->W)│ │(R->P->W)│
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
    public Step masterStep() {
        return masterStepBuilderFactory.get("masterStep")
                .partitioner("workerStep", partitioner())
                .gridSize(4)
                .outputChannel(outboundRequests())
                .inputChannel(inboundReplies())
                .build();
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
    public Step workerStep() {
        return workerStepBuilderFactory.get("workerStep")
                .inputChannel(inboundRequests())
                .outputChannel(outboundReplies())
                .stepLocator(new BeanFactoryStepLocator())
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

---

## 5.8 JobLauncher와 비동기 실행

### 비동기 JobLauncher 설정

```java
@Bean
public JobLauncher asyncJobLauncher(JobRepository jobRepository) throws Exception {
    TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
    jobLauncher.setJobRepository(jobRepository);
    jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
    jobLauncher.afterPropertiesSet();
    return jobLauncher;
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
            builder.addLong("timestamp", System.currentTimeMillis());
            params.forEach(builder::addString);

            JobExecution execution = asyncJobLauncher.run(myJob, builder.toJobParameters());
            return ResponseEntity.ok("Job started with ID: " + execution.getId());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to start job: " + e.getMessage());
        }
    }

    @GetMapping("/status/{executionId}")
    public ResponseEntity<JobExecutionStatus> getStatus(@PathVariable Long executionId) {
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

---

## 5.9 비동기 처리 주의사항

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

// 올바른 예: AtomicInteger 사용
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

### 3. gridSize 결정 기준

```java
// CPU 바운드: CPU 코어 수
int gridSize = Runtime.getRuntime().availableProcessors();

// I/O 바운드: 코어 수 * 2 이상
int gridSize = Runtime.getRuntime().availableProcessors() * 2;

// 데이터 특성에 따라: 전체 데이터 / 파티션당 처리량
long totalCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
int gridSize = (int) Math.ceil((double) totalCount / 100000);
```

### 4. 원격 처리 시 메시지 직렬화

```java
// 메시지 큐로 전송되는 객체는 Serializable 구현 필요
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String status;
}
```

### 5. 재시작과 멱등성

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

---

# 6. 실무 베스트 프랙티스

## 6.1 정기 결제 배치 설계

### 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────────────┐
│                    정기 결제 배치 Job                                │
│                                                                      │
│  ┌────────────┐   ┌────────────┐   ┌────────────┐   ┌────────────┐ │
│  │ 대상 조회   │──▶│ 결제 시도   │──▶│ 결과 처리   │──▶│ 알림 발송   │ │
│  │   Step     │   │   Step     │   │   Step     │   │   Step     │ │
│  └────────────┘   └────────────┘   └────────────┘   └────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

### 도메인 모델

```java
@Entity
@Table(name = "subscriptions")
public class Subscription {
    @Id
    private Long id;
    private Long userId;
    private Long planId;
    private BigDecimal amount;
    private LocalDate nextBillingDate;
    private int failedAttempts;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;  // ACTIVE, SUSPENDED, CANCELLED

    private LocalDateTime lastPaymentAt;
    private String paymentMethodId;
}

@Entity
@Table(name = "payment_logs")
public class PaymentLog {
    @Id
    private Long id;
    private Long subscriptionId;
    private Long userId;
    private BigDecimal amount;
    private String transactionId;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;  // SUCCESS, FAILED, PENDING

    private String failureReason;
    private LocalDateTime processedAt;
    private String idempotencyKey;  // 멱등성 보장용
}
```

### 결제 대상 조회

```java
/**
 * Best Practice: 결제 대상 조회 시 조건을 명확히
 * - 오늘 결제 예정인 구독
 * - ACTIVE 상태
 * - 실패 횟수 3회 미만 (3회 이상은 별도 처리)
 */
@Bean
@StepScope
public JdbcPagingItemReader<Subscription> billingTargetReader(
        @Value("#{jobParameters['billingDate']}") String billingDate) {

    return new JdbcPagingItemReaderBuilder<Subscription>()
            .name("billingTargetReader")
            .dataSource(dataSource)
            .selectClause("""
                SELECT id, user_id, plan_id, amount, next_billing_date,
                       failed_attempts, status, payment_method_id
                """)
            .fromClause("FROM subscriptions")
            .whereClause("""
                WHERE next_billing_date = :billingDate
                  AND status = 'ACTIVE'
                  AND failed_attempts < 3
                """)
            .parameterValues(Map.of("billingDate", billingDate))
            .sortKeys(Map.of("id", Order.ASCENDING))
            .pageSize(100)
            .rowMapper(new BeanPropertyRowMapper<>(Subscription.class))
            .build();
}
```

### 결제 처리 Step

```java
/**
 * Best Practice: 결제 처리 Chunk 설계
 * - Chunk Size는 작게 (10~50): 결제 실패 시 롤백 범위 최소화
 * - 멱등성 보장: idempotencyKey 사용
 * - 타임아웃 설정: 외부 PG 연동 시 필수
 */
@Bean
public Step paymentProcessStep(JobRepository jobRepository,
                               PlatformTransactionManager transactionManager) {
    return new StepBuilder("paymentProcessStep", jobRepository)
            .<Subscription, PaymentResult>chunk(10, transactionManager)
            .reader(billingTargetReader(null))
            .processor(paymentProcessor())
            .writer(paymentResultWriter())
            .faultTolerant()
            .retry(PgConnectionException.class)
            .retry(PgTimeoutException.class)
            .retryLimit(3)
            .backOffPolicy(exponentialBackOff())
            .skip(PaymentDeclinedException.class)
            .skip(InvalidPaymentMethodException.class)
            .skipLimit(Integer.MAX_VALUE)
            .listener(paymentSkipListener())
            .build();
}
```

### 결제 Processor - 멱등성 보장

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentProcessor implements ItemProcessor<Subscription, PaymentResult> {

    private final PaymentGateway paymentGateway;
    private final PaymentLogRepository paymentLogRepository;

    @Override
    public PaymentResult process(Subscription subscription) throws Exception {
        String idempotencyKey = generateIdempotencyKey(subscription);

        // 이미 처리된 결제인지 확인
        Optional<PaymentLog> existingLog = paymentLogRepository
                .findByIdempotencyKey(idempotencyKey);

        if (existingLog.isPresent()) {
            log.info("이미 처리된 결제 - subscriptionId: {}", subscription.getId());
            return PaymentResult.alreadyProcessed(existingLog.get());
        }

        try {
            PgResponse response = paymentGateway.charge(
                    PaymentRequest.builder()
                            .amount(subscription.getAmount())
                            .paymentMethodId(subscription.getPaymentMethodId())
                            .idempotencyKey(idempotencyKey)
                            .build()
            );
            return PaymentResult.success(subscription, response.getTransactionId());
        } catch (PaymentDeclinedException e) {
            return PaymentResult.failed(subscription, e.getDeclineCode());
        } catch (PgException e) {
            throw e;  // 상위에서 재시도 처리
        }
    }

    private String generateIdempotencyKey(Subscription subscription) {
        return String.format("billing_%d_%s",
                subscription.getId(), subscription.getNextBillingDate());
    }
}
```

### 실패 처리 (별도 Step)

```java
/**
 * Best Practice: 실패 케이스 별도 처리
 * - 1-2회 실패: 다음 날 재시도
 * - 3회 실패: 구독 일시정지 + 사용자 알림
 * - 결제 수단 만료: 업데이트 요청 알림
 */
@Bean
public ItemProcessor<PaymentLog, FailureAction> failureActionProcessor() {
    return log -> {
        int failedAttempts = log.getFailedAttempts();
        if (failedAttempts >= 3) {
            return FailureAction.suspendSubscription(log);
        } else if ("card_expired".equals(log.getFailureReason())) {
            return FailureAction.requestCardUpdate(log);
        } else {
            return FailureAction.scheduleRetry(log, LocalDate.now().plusDays(1));
        }
    };
}
```

---

## 6.2 정산 배치 설계

### 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         정산 배치 Job                                    │
│                                                                          │
│  ┌──────────────┐                                                       │
│  │ 검증 Step    │  거래 내역 무결성 검증                                  │
│  └──────┬───────┘                                                       │
│         ▼                                                                │
│  ┌──────────────┐                                                       │
│  │ 집계 Step    │  판매자별 거래 집계 (파티셔닝)                          │
│  │ (Partitioned)│                                                       │
│  └──────┬───────┘                                                       │
│         ▼                                                                │
│  ┌──────────────┐                                                       │
│  │ 정산금 계산   │  수수료 차감, 세금 계산                                │
│  └──────┬───────┘                                                       │
│         ▼                                                                │
│  ┌──────────────┐   ┌──────────────┐                                   │
│  │ 정산서 생성   │──▶│ 출금 요청    │                                    │
│  └──────────────┘   └──────────────┘                                   │
└─────────────────────────────────────────────────────────────────────────┘
```

### 도메인 모델

```java
@Entity
@Table(name = "settlements")
public class Settlement {
    @Id
    private Long id;
    private Long sellerId;
    private LocalDate settlementDate;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    private BigDecimal totalSales;
    private BigDecimal platformFee;
    private BigDecimal pgFee;
    private BigDecimal tax;
    private BigDecimal netAmount;
    private int transactionCount;

    @Enumerated(EnumType.STRING)
    private SettlementStatus status;  // PENDING, CONFIRMED, PAID

    private String checksum;  // 무결성 검증용
}
```

### 거래 내역 검증

```java
/**
 * Best Practice: 정산 전 데이터 무결성 검증
 * - 거래 금액 합계 vs 결제 금액 합계 일치 확인
 * - 중복 거래 체크 / 누락 거래 체크
 */
@Bean
@StepScope
public Tasklet validationTasklet(
        @Value("#{jobParameters['periodStart']}") String periodStart,
        @Value("#{jobParameters['periodEnd']}") String periodEnd) {

    return (contribution, chunkContext) -> {
        BigDecimal transactionSum = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
                "WHERE status = 'COMPLETED' AND completed_at BETWEEN ? AND ? AND settled = false",
                BigDecimal.class, periodStart, periodEnd);

        BigDecimal paymentSum = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM payments " +
                "WHERE status = 'SUCCESS' AND paid_at BETWEEN ? AND ?",
                BigDecimal.class, periodStart, periodEnd);

        BigDecimal diff = transactionSum.subtract(paymentSum).abs();
        BigDecimal tolerance = transactionSum.multiply(new BigDecimal("0.0001"));
        boolean isValid = diff.compareTo(tolerance) <= 0;

        chunkContext.getStepContext().getStepExecution()
                .getExecutionContext().put("validationPassed", isValid);

        if (!isValid) {
            log.error("정산 검증 실패 - 거래합계: {}, 결제합계: {}", transactionSum, paymentSum);
        }
        return RepeatStatus.FINISHED;
    };
}
```

### 집계 Processor

```java
/**
 * Best Practice: 정확한 금액 계산
 * - BigDecimal 사용 (부동소수점 오차 방지)
 * - 반올림 정책 명시
 * - 체크섬으로 무결성 검증
 */
@Override
public SellerAggregation process(Long sellerId) throws Exception {
    Map<String, Object> result = jdbcTemplate.queryForMap(
            "SELECT COUNT(*) as transaction_count, " +
            "COALESCE(SUM(amount), 0) as total_sales, " +
            "COALESCE(SUM(platform_fee), 0) as total_platform_fee, " +
            "COALESCE(SUM(pg_fee), 0) as total_pg_fee " +
            "FROM transactions WHERE seller_id = ? AND status = 'COMPLETED' " +
            "AND completed_at BETWEEN ? AND ? AND settled = false",
            sellerId, periodStart, periodEnd);

    BigDecimal totalSales = (BigDecimal) result.get("total_sales");
    BigDecimal platformFee = (BigDecimal) result.get("total_platform_fee");
    BigDecimal pgFee = (BigDecimal) result.get("total_pg_fee");

    // 세금 계산 (원천징수 3.3%)
    BigDecimal taxableAmount = totalSales.subtract(platformFee).subtract(pgFee);
    BigDecimal tax = taxableAmount.multiply(new BigDecimal("0.033"))
            .setScale(0, RoundingMode.DOWN);
    BigDecimal netAmount = taxableAmount.subtract(tax);

    String checksum = DigestUtils.sha256Hex(
            String.format("%d:%s:%s:%s", sellerId, totalSales, netAmount, periodStart));

    return SellerAggregation.builder()
            .sellerId(sellerId).totalSales(totalSales)
            .platformFee(platformFee).pgFee(pgFee)
            .tax(tax).netAmount(netAmount)
            .checksum(checksum).build();
}
```

---

## 6.3 공통 베스트 프랙티스

### 1. Job 파라미터 설계

```java
@Bean
public Job billingJob(JobRepository jobRepository) {
    return new JobBuilder("billingJob", jobRepository)
            .validator(new CompositeJobParametersValidator(List.of(
                    new DefaultJobParametersValidator(
                            new String[]{"billingDate"},        // 필수
                            new String[]{"dryRun", "maxRetryCount"}  // 선택
                    ),
                    parameters -> {
                        String billingDate = parameters.getString("billingDate");
                        if (!isValidDate(billingDate)) {
                            throw new JobParametersInvalidException(
                                    "Invalid billingDate format: " + billingDate);
                        }
                    }
            )))
            .incrementer(new RunIdIncrementer())
            .start(paymentStep())
            .build();
}
```

### 2. 배치 실행 모드 (Dry Run)

```java
@Component
public class PaymentProcessor implements ItemProcessor<Subscription, PaymentResult> {

    @Value("#{jobParameters['dryRun'] ?: 'false'}")
    private boolean dryRun;

    @Override
    public PaymentResult process(Subscription subscription) {
        if (dryRun) {
            log.info("[DRY RUN] 결제 시뮬레이션 - subscriptionId: {}, amount: {}",
                    subscription.getId(), subscription.getAmount());
            return PaymentResult.dryRun(subscription);
        }
        return executePayment(subscription);
    }
}
```

### 3. 배치 모니터링

```java
@Component
@RequiredArgsConstructor
public class BatchMetricsListener implements JobExecutionListener, StepExecutionListener {

    private final MeterRegistry meterRegistry;

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        String status = jobExecution.getStatus().toString();

        meterRegistry.counter("batch.job.completed",
                "job", jobName, "status", status).increment();

        long duration = Duration.between(
                jobExecution.getStartTime(), jobExecution.getEndTime()).toMillis();
        meterRegistry.timer("batch.job.duration", "job", jobName)
                .record(duration, TimeUnit.MILLISECONDS);
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String stepName = stepExecution.getStepName();
        meterRegistry.gauge("batch.step.read_count",
                Tags.of("step", stepName), stepExecution.getReadCount());
        meterRegistry.gauge("batch.step.write_count",
                Tags.of("step", stepName), stepExecution.getWriteCount());
        meterRegistry.gauge("batch.step.skip_count",
                Tags.of("step", stepName), stepExecution.getSkipCount());
        return stepExecution.getExitStatus();
    }
}
```

### 4. 알림 설정

```java
@Component
@RequiredArgsConstructor
public class BatchAlertListener implements JobExecutionListener {

    private final SlackNotifier slackNotifier;
    private final PagerDutyClient pagerDuty;

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        BatchStatus status = jobExecution.getStatus();

        if (status == BatchStatus.COMPLETED) {
            slackNotifier.send(SlackMessage.success(buildSuccessSummary(jobExecution)));

            long totalSkips = jobExecution.getStepExecutions().stream()
                    .mapToLong(StepExecution::getSkipCount).sum();
            if (totalSkips > 100) {
                slackNotifier.send(SlackMessage.warning(
                        String.format("%s: 스킵 건수 %d건 (확인 필요)", jobName, totalSkips)));
            }
        } else if (status == BatchStatus.FAILED) {
            String errorMessage = jobExecution.getAllFailureExceptions().stream()
                    .map(Throwable::getMessage).collect(Collectors.joining(", "));
            slackNotifier.send(SlackMessage.error(
                    String.format("배치 실패: %s - %s", jobName, errorMessage)));

            if (isCriticalJob(jobName)) {
                pagerDuty.triggerIncident("Batch Job Failed: " + jobName,
                        errorMessage, Severity.HIGH);
            }
        }
    }
}
```

---

## 6.4 장애 대응 패턴

### 1. 부분 재처리

```java
/**
 * Best Practice: 실패 건만 재처리
 * - 전체 재실행 대신 실패 건만 선별 처리
 */
@Bean
@StepScope
public JdbcPagingItemReader<PaymentLog> failedPaymentReader(
        @Value("#{jobParameters['originalJobExecutionId']}") Long executionId) {

    return new JdbcPagingItemReaderBuilder<PaymentLog>()
            .name("failedPaymentReader")
            .dataSource(dataSource)
            .selectClause("SELECT * FROM payment_logs")
            .whereClause("""
                WHERE job_execution_id = :executionId
                  AND status = 'FAILED'
                  AND retry_count < 3
                """)
            .parameterValues(Map.of("executionId", executionId))
            .sortKeys(Map.of("id", Order.ASCENDING))
            .pageSize(100)
            .build();
}
```

### 2. 보상 트랜잭션

```java
/**
 * Best Practice: 롤백 불가능한 외부 연동 보상 처리
 * - 결제 성공 후 DB 저장 실패 시 결제 취소
 * - 보상 실패 시 수동 처리 큐로 이관
 */
@Component
@RequiredArgsConstructor
public class CompensatingPaymentWriter implements ItemWriter<PaymentResult> {

    private final PaymentGateway paymentGateway;
    private final PaymentLogRepository repository;
    private final ManualProcessingQueue manualQueue;

    @Override
    public void write(Chunk<? extends PaymentResult> results) {
        List<PaymentResult> successfulPayments = new ArrayList<>();
        try {
            for (PaymentResult result : results) {
                if (result.isSuccess()) successfulPayments.add(result);
            }
            repository.saveAll(toEntities(results.getItems()));
        } catch (Exception e) {
            log.error("DB 저장 실패, 보상 트랜잭션 시작", e);

            for (PaymentResult payment : successfulPayments) {
                try {
                    paymentGateway.refund(RefundRequest.builder()
                            .transactionId(payment.getTransactionId())
                            .reason("SYSTEM_ERROR_COMPENSATION")
                            .idempotencyKey("refund_" + payment.getIdempotencyKey())
                            .build());
                } catch (Exception refundError) {
                    manualQueue.enqueue(ManualTask.builder()
                            .type(TaskType.REFUND_REQUIRED)
                            .transactionId(payment.getTransactionId())
                            .amount(payment.getAmount())
                            .reason("Compensation refund failed")
                            .build());
                }
            }
            throw e;
        }
    }
}
```

### 3. 데드락 방지

```java
/**
 * Best Practice: 데드락 방지 전략
 * - 일관된 락 순서 (ID 오름차순)
 * - 락 타임아웃 설정
 * - 낙관적 락 사용 고려
 */
@Entity
public class Settlement {
    @Version
    private Long version;  // 낙관적 락
}

@Bean
public Step settlementStep(JobRepository jobRepository,
                           PlatformTransactionManager transactionManager) {
    return new StepBuilder("settlementStep", jobRepository)
            .<SellerAggregation, Settlement>chunk(10, transactionManager)
            .reader(aggregationReader())
            .writer(settlementWriter())
            .faultTolerant()
            .retry(OptimisticLockingFailureException.class)
            .retry(DeadlockLoserDataAccessException.class)
            .retryLimit(3)
            .backOffPolicy(new FixedBackOffPolicy())
            .build();
}
```

---

## 6.5 운영 체크리스트

### 배포 전 체크리스트

- [ ] Chunk Size 적절한가? (테스트 환경에서 검증)
- [ ] 멱등성 보장되는가? (동일 입력 -> 동일 결과)
- [ ] Skip/Retry 정책 적절한가?
- [ ] 타임아웃 설정되어 있는가? (외부 연동)
- [ ] Dry Run 모드 동작하는가?
- [ ] 인덱스 최적화되어 있는가? (실행 계획 확인)
- [ ] 메타데이터 테이블 정리 정책 있는가?
- [ ] 핵심 메트릭 수집되는가?
- [ ] 알림 설정되어 있는가?

### 실행 전 체크리스트

- [ ] 처리 대상 건수 예상치와 일치하는가?
- [ ] 이전 실행 결과 정상 종료되었는가?
- [ ] 중복 실행 아닌가?
- [ ] DB 커넥션 풀 여유 있는가?
- [ ] 디스크 용량 충분한가?
- [ ] 외부 시스템 정상인가? (PG, 알림 등)
- [ ] 파라미터 올바른가?
- [ ] 실행 시간대 적절한가? (트래픽 낮은 시간)

### 실행 후 체크리스트

- [ ] 처리 건수 예상치와 일치하는가?
- [ ] 실패/스킵 건수 허용 범위인가?
- [ ] 비즈니스 데이터 정합성 확인 (금액 합계 등)
- [ ] 실패 건 원인 분석 완료
- [ ] 재처리 필요 건 식별
- [ ] 이해관계자 결과 공유

---

*마지막 업데이트: 2026년 02월*
