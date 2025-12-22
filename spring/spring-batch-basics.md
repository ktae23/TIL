# Spring Batch 기초 - Job, Step, 실행 흐름

Spring Batch의 핵심 개념과 아키텍처를 정리한다.

## 목차

- [Spring Batch란?](#spring-batch란)
- [핵심 도메인 모델](#핵심-도메인-모델)
- [Job 구성하기](#job-구성하기)
- [Step 구성하기](#step-구성하기)
- [실행 흐름 제어](#실행-흐름-제어)
- [JobRepository와 메타데이터](#jobrepository와-메타데이터)

---

## Spring Batch란?

Spring Batch는 대용량 데이터 처리를 위한 경량 배치 프레임워크다. 로깅, 트랜잭션 관리, 재시작, 건너뛰기, 리소스 관리 등 배치 처리에 필수적인 기능을 제공한다.

**주요 사용 사례:**
- 대량 데이터 ETL (Extract-Transform-Load)
- 정산/결제 처리
- 대용량 파일 처리
- 주기적인 데이터 마이그레이션

---

## 핵심 도메인 모델

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

## Job 구성하기

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

### JobExecutionListener

```java
@Component
public class JobCompletionListener implements JobExecutionListener {

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
}

// Job에 리스너 등록
@Bean
public Job jobWithListener(JobRepository jobRepository, Step step1,
                           JobCompletionListener listener) {
    return new JobBuilder("jobWithListener", jobRepository)
            .listener(listener)
            .start(step1)
            .build();
}
```

---

## Step 구성하기

### Tasklet 기반 Step

단순한 단일 작업에 적합하다.

```java
@Bean
public Step cleanupStep(JobRepository jobRepository,
                        PlatformTransactionManager transactionManager) {
    return new StepBuilder("cleanupStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                // 임시 파일 삭제
                Files.deleteIfExists(Path.of("/tmp/batch-temp.dat"));
                log.info("임시 파일 정리 완료");
                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
}
```

### Chunk 기반 Step

대량 데이터 처리에 적합하다. (다음 문서에서 상세 설명)

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

        // 커스텀 종료 상태 반환 가능
        if (stepExecution.getSkipCount() > 100) {
            return new ExitStatus("COMPLETED WITH SKIPS");
        }
        return stepExecution.getExitStatus();
    }
}
```

---

## 실행 흐름 제어

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

### JobExecutionDecider 활용

```java
public class MyDecider implements JobExecutionDecider {

    @Override
    public FlowExecutionStatus decide(JobExecution jobExecution,
                                       StepExecution stepExecution) {
        String status = someBusinessLogic();
        return new FlowExecutionStatus(status);
    }
}

@Bean
public Job deciderJob(JobRepository jobRepository,
                      Step step1, Step evenStep, Step oddStep) {
    return new JobBuilder("deciderJob", jobRepository)
            .start(step1)
            .next(new MyDecider())
                .on("EVEN").to(evenStep)
                .on("ODD").to(oddStep)
            .end()
            .build();
}
```

---

## JobRepository와 메타데이터

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
        factory.setTablePrefix("BATCH_");  // 테이블 접두사
        factory.setIsolationLevelForCreate("ISOLATION_SERIALIZABLE");
        factory.afterPropertiesSet();
        return factory.getObject();
    }
}
```

### ExecutionContext 활용

```java
// Step에서 데이터 저장
@Bean
public Step saveContextStep(JobRepository jobRepository,
                            PlatformTransactionManager transactionManager) {
    return new StepBuilder("saveContextStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                ExecutionContext stepContext =
                        chunkContext.getStepContext().getStepExecution().getExecutionContext();
                stepContext.putInt("processedCount", 500);

                ExecutionContext jobContext =
                        chunkContext.getStepContext().getStepExecution()
                                .getJobExecution().getExecutionContext();
                jobContext.putString("status", "PROCESSING");

                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
}

// 다른 Step에서 데이터 읽기
@Bean
@StepScope
public Tasklet readContextTasklet(
        @Value("#{jobExecutionContext['status']}") String status) {
    return (contribution, chunkContext) -> {
        log.info("Job 상태: {}", status);
        return RepeatStatus.FINISHED;
    };
}
```

---

## Job 실행하기

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

## 다음 문서

- [Spring Batch Chunk 처리](./spring-batch-chunk.md) - ItemReader, ItemProcessor, ItemWriter 상세
- [Spring Batch 고급 활용](./spring-batch-advanced.md) - 비동기, 파티셔닝, 원격 실행

*마지막 업데이트: 2024년 12월*
