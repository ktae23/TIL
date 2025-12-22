# Spring Batch Chunk 처리 - ItemReader, ItemProcessor, ItemWriter

대용량 데이터를 효율적으로 처리하는 Chunk 기반 처리 방식을 정리한다.

## 목차

- [Chunk 처리 개념](#chunk-처리-개념)
- [ItemReader](#itemreader)
- [ItemProcessor](#itemprocessor)
- [ItemWriter](#itemwriter)
- [오류 처리와 Skip/Retry](#오류-처리와-skipretry)
- [트랜잭션 관리](#트랜잭션-관리)

---

## Chunk 처리 개념

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

### 기본 구조

```java
@Bean
public Step chunkStep(JobRepository jobRepository,
                      PlatformTransactionManager transactionManager) {
    return new StepBuilder("chunkStep", jobRepository)
            .<String, String>chunk(100, transactionManager)  // 100개 단위로 커밋
            .reader(reader())
            .processor(processor())
            .writer(writer())
            .build();
}
```

**Chunk Size 선택 기준:**

| Chunk Size | 적합한 상황 |
|------------|------------|
| 10~50 | 개별 아이템 처리 시간이 긴 경우 |
| 100~500 | 일반적인 데이터 처리 |
| 1000+ | 단순 데이터 이관, I/O 최적화 필요 시 |

---

## ItemReader

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

// QueryDSL 사용 시
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

## ItemProcessor

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

@Bean
public Validator customerValidator() {
    return new Validator() {
        @Override
        public boolean supports(Class<?> clazz) {
            return Customer.class.isAssignableFrom(clazz);
        }

        @Override
        public void validate(Object target, Errors errors) {
            Customer customer = (Customer) target;
            if (StringUtils.isBlank(customer.getEmail())) {
                errors.rejectValue("email", "email.required");
            }
        }
    };
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

@Bean
public ItemProcessor<Customer, Customer> enrichmentProcessor() {
    return customer -> {
        // 외부 API 호출하여 추가 정보 조회
        CustomerDetails details = externalApi.getDetails(customer.getId());
        customer.setDetails(details);
        return customer;
    };
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

## ItemWriter

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

// 포맷 지정
@Bean
public FlatFileItemWriter<CustomerDto> formattedWriter() {
    return new FlatFileItemWriterBuilder<CustomerDto>()
            .name("formattedWriter")
            .resource(new FileSystemResource("output/customers.txt"))
            .formatted()
            .format("%-10d %-30s %-50s")
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

// ItemPreparedStatementSetter 사용
@Bean
public JdbcBatchItemWriter<CustomerDto> jdbcWriterWithSetter(DataSource dataSource) {
    return new JdbcBatchItemWriterBuilder<CustomerDto>()
            .dataSource(dataSource)
            .sql("INSERT INTO customers_backup (id, name, email) VALUES (?, ?, ?)")
            .itemPreparedStatementSetter((item, ps) -> {
                ps.setLong(1, item.getId());
                ps.setString(2, item.getFullName());
                ps.setString(3, item.getEmail());
            })
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

        // 배치로 API 호출
        apiClient.bulkCreate(customers);

        log.info("{}건 API 전송 완료", customers.size());
    }
}
```

---

## 오류 처리와 Skip/Retry

### Skip 설정

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
            .skipLimit(100)  // 최대 100건까지 스킵
            .skipPolicy(new AlwaysSkipItemSkipPolicy())  // 커스텀 정책
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

### Retry 설정

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

### Skip + Retry 조합

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
            .noSkip(FileNotFoundException.class)  // 이 예외는 스킵하지 않음
            .noRetry(ValidationException.class)   // 이 예외는 재시도하지 않음
            .build();
}
```

---

## 트랜잭션 관리

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

Reader를 트랜잭션 밖에서 실행한다.

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

## 성능 최적화 팁

### Chunk Size 튜닝

```java
// 처리량 측정 리스너
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

### Writer 배치 크기

```java
@Bean
public JpaItemWriter<CustomerEntity> optimizedJpaWriter(EntityManagerFactory emf) {
    JpaItemWriter<CustomerEntity> writer = new JpaItemWriter<>();
    writer.setEntityManagerFactory(emf);
    return writer;
}

// application.yml
// spring.jpa.properties.hibernate.jdbc.batch_size=50
```

---

## 다음 문서

- [Spring Batch 기초](./spring-batch-basics.md) - Job, Step, 실행 흐름
- [Spring Batch ExecutionContext 가이드](./spring-batch-execution-context.md) - 상태 저장과 재시작
- [Spring Batch 고급 활용](./spring-batch-advanced.md) - 비동기, 파티셔닝, 원격 실행
- [Spring Batch 실무 베스트 프랙티스](./spring-batch-best-practices.md) - 정기 결제, 정산 사례

*마지막 업데이트: 2024년 12월*
