# ShedLock 분산 스케줄링 가이드

다중 인스턴스 환경에서 `@Scheduled` 작업의 중복 실행을 방지하는 ShedLock의 동작 원리, 설정 방법, Spring Batch 연동, 그리고 세무 도메인 적용 사례를 다룹니다.

## 목차
1. [ShedLock 개요](#shedlock-개요)
2. [ShedLock 설정 및 구현](#shedlock-설정-및-구현)
3. [Spring Batch without Jenkins](#spring-batch-without-jenkins)
4. [Quartz vs ShedLock 비교](#quartz-vs-shedlock-비교)
5. [Lock Provider 종류](#lock-provider-종류)
6. [세무 도메인 적용](#세무-도메인-적용)
7. [운영 고려사항](#운영-고려사항)
8. [핵심 정리](#핵심-정리)
9. [면접 대비 핵심 질문](#면접-대비-핵심-질문)

---

## ShedLock 개요

### ShedLock이 해결하는 문제

Spring의 `@Scheduled`는 각 인스턴스에서 독립적으로 실행됩니다. 다중 인스턴스 환경에서는 동일한 스케줄 작업이 모든 인스턴스에서 동시에 실행되어 데이터 중복 처리, 외부 API 중복 호출 등의 문제가 발생합니다.

```
┌──────────────────────────────────────────────────────────────────┐
│              다중 인스턴스에서 @Scheduled 문제                      │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ❌ ShedLock 미사용 시:                                           │
│                                                                   │
│  Instance A ──── @Scheduled("0 0 * * *") ──── 세금계산 실행 ✓     │
│  Instance B ──── @Scheduled("0 0 * * *") ──── 세금계산 실행 ✓     │
│  Instance C ──── @Scheduled("0 0 * * *") ──── 세금계산 실행 ✓     │
│                                                                   │
│  → 3개 인스턴스가 동시에 동일 작업 실행                            │
│  → 세금 3번 계산, 중복 알림 발송, 데이터 정합성 붕괴               │
│                                                                   │
│  ✅ ShedLock 사용 시:                                              │
│                                                                   │
│  Instance A ──── Lock 획득 ──── 세금계산 실행 ✓                   │
│  Instance B ──── Lock 획득 실패 ──── SKIP                         │
│  Instance C ──── Lock 획득 실패 ──── SKIP                         │
│                                                                   │
│  → 오직 1개 인스턴스만 작업 실행                                   │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 동작 원리

ShedLock은 공유 저장소(DB, Redis 등)를 이용한 분산 락 메커니즘입니다. 스케줄러가 아닌 락(Lock)이라는 점이 핵심입니다. ShedLock은 작업 스케줄링을 대체하지 않고, 이미 존재하는 스케줄러 위에 락 레이어만 추가합니다.

```
┌──────────────────────────────────────────────────────────────────┐
│                  ShedLock 동작 흐름                                │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  1. @Scheduled 트리거 시점 도달                                    │
│     │                                                             │
│  2. ShedLock Proxy가 가로챔 (AOP)                                 │
│     │                                                             │
│  3. Lock Provider에 락 획득 시도                                   │
│     │                                                             │
│     ├── 성공 → 4a. 실제 메서드 실행                                │
│     │           │                                                 │
│     │           5a. 실행 완료 후 lock_until 갱신                   │
│     │               (lockAtLeastFor까지 유지)                      │
│     │                                                             │
│     └── 실패 → 4b. 메서드 실행 SKIP                               │
│                    (다른 인스턴스가 이미 실행 중)                    │
│                                                                   │
│  ┌─────────────────────────────────────────────┐                  │
│  │         공유 저장소 (DB / Redis)              │                  │
│  │                                              │                  │
│  │  name       | lock_until          | locked_by│                  │
│  │  ─────────────────────────────────────────── │                  │
│  │  taxCalc    | 2025-02-01 01:00:00 | inst-A   │                  │
│  │  refundSync | 2025-02-01 00:35:00 | inst-B   │                  │
│  └─────────────────────────────────────────────┘                  │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 핵심 개념: lockAtMostFor와 lockAtLeastFor

```
┌──────────────────────────────────────────────────────────────────┐
│                lockAtMostFor vs lockAtLeastFor                     │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  lockAtMostFor (최대 잠금 시간):                                   │
│  ─────────────────────────────────────────────────────            │
│  - 작업이 비정상 종료되었을 때 락이 영원히 유지되는 것 방지         │
│  - 안전장치 역할                                                  │
│  - 예: "PT15M" → 최대 15분간 락 유지                               │
│  - 작업이 5분 만에 끝나도 15분까지 락 유지 가능 (lockAtLeastFor    │
│    미설정 시 즉시 해제)                                            │
│                                                                   │
│  lockAtLeastFor (최소 잠금 시간):                                   │
│  ─────────────────────────────────────────────────────            │
│  - 작업이 너무 빨리 끝나서 다른 인스턴스에서 재실행되는 것 방지    │
│  - 클럭 차이(Clock Drift) 보호                                    │
│  - 예: "PT5M" → 최소 5분간 락 유지                                 │
│                                                                   │
│  시간 흐름 예시:                                                    │
│                                                                   │
│  0분        2분(작업완료)    5분(최소잠금)     15분(최대잠금)       │
│  |────작업실행────|──────락유지──────|                              │
│                                     ↑                              │
│                              여기서 락 해제                        │
│                       (lockAtLeastFor = 5분)                       │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

---

## ShedLock 설정 및 구현

### 의존성 추가 (Gradle Kotlin DSL)

```kotlin
// build.gradle.kts
dependencies {
    // ShedLock Core
    implementation("net.javacrumbs.shedlock:shedlock-spring:5.12.0")

    // JDBC Lock Provider (MySQL/PostgreSQL)
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.12.0")

    // 또는 Redis Lock Provider
    // implementation("net.javacrumbs.shedlock:shedlock-provider-redis-spring:5.12.0")

    // 또는 MongoDB Lock Provider
    // implementation("net.javacrumbs.shedlock:shedlock-provider-mongo:5.12.0")
}
```

### DB 테이블 생성

```sql
-- MySQL
CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at  TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

-- PostgreSQL
CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
```

### Spring Boot 설정

```java
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class SchedulerConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime()  // DB 시간 사용 (클럭 차이 방지)
                .build()
        );
    }

    // TaskScheduler 커스터마이징 (스레드 풀 설정)
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("scheduled-task-");
        scheduler.setErrorHandler(t ->
            log.error("Scheduled task error", t));
        return scheduler;
    }
}
```

### @SchedulerLock 어노테이션 사용법

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TaxCalculationScheduler {

    private final TaxCalculationService taxCalculationService;

    @Scheduled(cron = "0 0 2 * * *")  // 매일 새벽 2시
    @SchedulerLock(
        name = "dailyTaxCalculation",
        lockAtMostFor = "PT30M",   // 최대 30분 락 유지
        lockAtLeastFor = "PT5M"    // 최소 5분 락 유지
    )
    public void calculateDailyTax() {
        log.info("일일 세금 계산 배치 시작");
        taxCalculationService.calculateAll();
        log.info("일일 세금 계산 배치 완료");
    }

    @Scheduled(cron = "0 0 3 L * *")  // 매월 말일 새벽 3시
    @SchedulerLock(
        name = "monthlyBookkeepingClose",
        lockAtMostFor = "PT1H",
        lockAtLeastFor = "PT10M"
    )
    public void closeMonthlyBookkeeping() {
        log.info("월별 기장 마감 배치 시작");
        taxCalculationService.closeMonthlyBooks();
        log.info("월별 기장 마감 배치 완료");
    }
}
```

### 프로그래밍 방식 락 사용

`@SchedulerLock`은 `@Scheduled`와 함께 사용하지만, 프로그래밍 방식으로도 락을 사용할 수 있습니다.

```java
@Service
@RequiredArgsConstructor
public class ManualLockService {

    private final LockProvider lockProvider;

    public void executeWithLock() {
        LockConfiguration lockConfig = new LockConfiguration(
            Instant.now(),
            "manualTask",
            Duration.ofMinutes(30),  // lockAtMostFor
            Duration.ofMinutes(5)    // lockAtLeastFor
        );

        Optional<SimpleLock> lock = lockProvider.lock(lockConfig);

        if (lock.isPresent()) {
            try {
                // 락 획득 성공 - 작업 실행
                doWork();
            } finally {
                lock.get().unlock();
            }
        } else {
            log.info("락 획득 실패 - 다른 인스턴스에서 실행 중");
        }
    }
}
```

---

## Spring Batch without Jenkins

### 왜 Jenkins 없이 Spring Batch를 운영하는가?

```
┌──────────────────────────────────────────────────────────────────┐
│            Jenkins vs ShedLock + @Scheduled 비교                  │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Jenkins 기반 배치:                                               │
│  ┌─────────┐     HTTP/SSH     ┌─────────────────────────┐       │
│  │ Jenkins  │────────────────►│ Spring Batch App         │       │
│  │ Server   │                 │ (REST API로 Job 트리거)  │       │
│  └─────────┘                  └─────────────────────────┘       │
│  - Jenkins 서버 별도 운영 필요                                    │
│  - 네트워크 의존성                                                │
│  - Jenkins 장애 시 배치 미실행                                    │
│  - 모니터링 포인트 분산                                           │
│                                                                   │
│  ShedLock + @Scheduled 기반 배치:                                 │
│  ┌─────────────────────────────────────────┐                     │
│  │ Spring Boot App (Batch + Scheduler)     │                     │
│  │   @Scheduled → JobLauncher → Job 실행   │                     │
│  │   ShedLock → 중복 실행 방지             │                     │
│  └─────────────────────────────────────────┘                     │
│  - 단일 애플리케이션으로 통합                                     │
│  - 외부 의존성 없음                                               │
│  - 배포와 스케줄 관리 일원화                                      │
│  - 코드 기반 스케줄 관리 (Git으로 추적)                           │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Spring Batch Job을 @Scheduled에서 트리거

```java
@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Bean
    public Job taxCalculationJob(JobRepository jobRepository,
                                  Step taxCalculationStep) {
        return new JobBuilder("taxCalculationJob", jobRepository)
            .start(taxCalculationStep)
            .build();
    }

    @Bean
    public Step taxCalculationStep(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager,
                                    ItemReader<TaxTarget> reader,
                                    ItemProcessor<TaxTarget, TaxResult> processor,
                                    ItemWriter<TaxResult> writer) {
        return new StepBuilder("taxCalculationStep", jobRepository)
            .<TaxTarget, TaxResult>chunk(100, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .faultTolerant()
            .retryLimit(3)
            .retry(TransientDataAccessException.class)
            .build();
    }
}
```

### ShedLock + JobLauncher 통합

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job taxCalculationJob;
    private final Job monthlyClosingJob;
    private final Job refundStatusCheckJob;

    /**
     * 일일 세금 계산 배치
     * - 매일 새벽 2시 실행
     * - 전일 등록된 거래내역의 세금 계산
     */
    @Scheduled(cron = "0 0 2 * * *")
    @SchedulerLock(
        name = "taxCalculationBatch",
        lockAtMostFor = "PT1H",
        lockAtLeastFor = "PT10M"
    )
    public void runTaxCalculationJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                .addString("runId", UUID.randomUUID().toString())
                .addLocalDate("targetDate", LocalDate.now().minusDays(1))
                .toJobParameters();

            JobExecution execution = jobLauncher.run(taxCalculationJob, params);

            log.info("세금 계산 배치 완료: status={}, duration={}ms",
                execution.getStatus(),
                Duration.between(execution.getStartTime(),
                    execution.getEndTime()).toMillis());

        } catch (Exception e) {
            log.error("세금 계산 배치 실패", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 월말 기장 마감 배치
     * - 매월 마지막 영업일 새벽 3시
     * - lockAtMostFor을 2시간으로 설정 (대량 데이터 처리)
     */
    @Scheduled(cron = "0 0 3 L * *")
    @SchedulerLock(
        name = "monthlyClosingBatch",
        lockAtMostFor = "PT2H",
        lockAtLeastFor = "PT30M"
    )
    public void runMonthlyClosingJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                .addString("runId", UUID.randomUUID().toString())
                .addLocalDate("closingMonth",
                    LocalDate.now().withDayOfMonth(1))
                .toJobParameters();

            jobLauncher.run(monthlyClosingJob, params);

        } catch (Exception e) {
            log.error("월말 기장 마감 배치 실패", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 환급 상태 조회 배치
     * - 매 30분마다 실행
     * - 홈택스 API로 환급 처리 상태 동기화
     */
    @Scheduled(fixedRate = 1800000)  // 30분
    @SchedulerLock(
        name = "refundStatusCheckBatch",
        lockAtMostFor = "PT25M",
        lockAtLeastFor = "PT5M"
    )
    public void runRefundStatusCheckJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                .addString("runId", UUID.randomUUID().toString())
                .toJobParameters();

            jobLauncher.run(refundStatusCheckJob, params);

        } catch (Exception e) {
            log.error("환급 상태 조회 배치 실패", e);
            throw new RuntimeException(e);
        }
    }
}
```

### 배치 실행 결과 모니터링

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class BatchJobListener implements JobExecutionListener {

    private final MeterRegistry meterRegistry;
    private final AlertService alertService;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("배치 시작: job={}, params={}",
            jobExecution.getJobInstance().getJobName(),
            jobExecution.getJobParameters());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        BatchStatus status = jobExecution.getStatus();
        long duration = Duration.between(
            jobExecution.getStartTime(),
            jobExecution.getEndTime()
        ).toMillis();

        // 메트릭 기록
        meterRegistry.timer("batch.job.duration",
            Tags.of("job", jobName, "status", status.name()))
            .record(duration, TimeUnit.MILLISECONDS);

        meterRegistry.counter("batch.job.execution",
            Tags.of("job", jobName, "status", status.name()))
            .increment();

        // 실패 시 알림
        if (status == BatchStatus.FAILED) {
            String errorMessage = jobExecution.getAllFailureExceptions()
                .stream()
                .map(Throwable::getMessage)
                .collect(Collectors.joining(", "));

            alertService.sendSlackAlert(
                String.format("[BATCH FAILED] %s: %s", jobName, errorMessage)
            );
        }

        log.info("배치 완료: job={}, status={}, duration={}ms",
            jobName, status, duration);
    }
}
```

---

## Quartz vs ShedLock 비교

### 아키텍처 차이

```
┌──────────────────────────────────────────────────────────────────┐
│                    Quartz 아키텍처                                 │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────────────────────────────────┐                │
│  │              Quartz Scheduler                 │                │
│  │  ┌──────────┐  ┌──────────┐  ┌────────────┐ │                │
│  │  │ Trigger   │  │ JobStore  │  │ ThreadPool │ │                │
│  │  │ (Cron/   │  │ (RAM/    │  │ (Worker    │ │                │
│  │  │  Simple) │  │  JDBC)   │  │  Threads)  │ │                │
│  │  └──────────┘  └──────────┘  └────────────┘ │                │
│  └──────────────────────────────────────────────┘                │
│                         │                                         │
│            ┌────────────┼────────────┐                            │
│            ▼            ▼            ▼                            │
│    QRTZ_TRIGGERS  QRTZ_JOB_DETAILS  QRTZ_LOCKS                  │
│    QRTZ_CRON...   QRTZ_CALENDARS    QRTZ_FIRED...               │
│    (11개 이상의 테이블)                                            │
│                                                                   │
│  → 완전한 Job 스케줄링 프레임워크                                  │
│  → 자체 스레드풀, 트리거 관리, 클러스터링                          │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                    ShedLock 아키텍처                               │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────────────────────────────────┐                │
│  │          Spring @Scheduled (기존)             │                │
│  │                    │                          │                │
│  │          ┌─────────▼──────────┐               │                │
│  │          │  ShedLock Proxy    │               │                │
│  │          │  (AOP Interceptor) │               │                │
│  │          └─────────┬──────────┘               │                │
│  │                    │                          │                │
│  │          ┌─────────▼──────────┐               │                │
│  │          │  Lock Provider     │               │                │
│  │          │  (JDBC/Redis/Mongo)│               │                │
│  │          └────────────────────┘               │                │
│  └──────────────────────────────────────────────┘                │
│                         │                                         │
│                         ▼                                         │
│                  shedlock (1개 테이블)                             │
│                                                                   │
│  → 락 전용 라이브러리 (스케줄러 아님)                              │
│  → Spring의 @Scheduled에 락만 추가                                │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 상세 비교 테이블

| 비교 항목 | Quartz | ShedLock |
|-----------|--------|----------|
| **역할** | 완전한 Job 스케줄링 프레임워크 | 분산 락 라이브러리 |
| **DB 테이블** | 11개 이상 (QRTZ_*) | 1개 (shedlock) |
| **설정 복잡도** | 높음 (quartz.properties, JobFactory 등) | 낮음 (어노테이션 기반) |
| **클러스터링** | 자체 클러스터링 (DB 기반) | 분산 락으로 중복 방지 |
| **Job 관리** | 동적 Job 추가/삭제/일시정지 가능 | 코드 기반 (정적) |
| **Misfire 처리** | 정교한 정책 (FIRE_ONCE, DO_NOTHING 등) | 별도 정책 없음 |
| **트리거 유형** | Cron, Simple, Calendar, Daily 등 | Spring @Scheduled 의존 |
| **의존성 크기** | 크고 무거움 | 가볍고 최소한 |
| **학습 곡선** | 높음 | 낮음 |
| **적합 케이스** | 동적 Job 관리 필요, 복잡한 스케줄링 | 단순한 중복 실행 방지 |

### 마이그레이션 시나리오: Quartz → ShedLock

기존 Quartz 기반 스케줄러를 ShedLock으로 마이그레이션하는 단계별 접근법입니다.

```java
// AS-IS: Quartz 기반 Job
@Component
public class TaxCalcQuartzJob implements Job {

    @Override
    public void execute(JobExecutionContext context) {
        // 세금 계산 로직
        taxCalculationService.calculateAll();
    }
}

// Quartz 설정
@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail taxCalcJobDetail() {
        return JobBuilder.newJob(TaxCalcQuartzJob.class)
            .withIdentity("taxCalcJob")
            .storeDurably()
            .build();
    }

    @Bean
    public Trigger taxCalcTrigger(JobDetail taxCalcJobDetail) {
        return TriggerBuilder.newTrigger()
            .forJob(taxCalcJobDetail)
            .withSchedule(CronScheduleBuilder.cronSchedule("0 0 2 * * ?"))
            .build();
    }
}
```

```java
// TO-BE: ShedLock 기반 스케줄링
@Service
@RequiredArgsConstructor
@Slf4j
public class TaxCalcScheduler {

    private final TaxCalculationService taxCalculationService;

    @Scheduled(cron = "0 0 2 * * *")
    @SchedulerLock(
        name = "taxCalcJob",
        lockAtMostFor = "PT30M",
        lockAtLeastFor = "PT5M"
    )
    public void calculateTax() {
        taxCalculationService.calculateAll();
    }
}
```

```
마이그레이션 체크리스트:
─────────────────────────────────────────────────
□ Quartz Job 목록 정리 및 실행 주기 파악
□ shedlock 테이블 생성
□ ShedLock 의존성 추가 및 LockProvider 설정
□ Quartz Job → @Scheduled + @SchedulerLock 변환
□ lockAtMostFor 값 설정 (Job 최대 실행 시간 기준)
□ lockAtLeastFor 값 설정 (스케줄 주기 고려)
□ 테스트 환경에서 중복 실행 방지 검증
□ QRTZ_* 테이블 정리 (마이그레이션 완료 후)
□ Quartz 의존성 제거
```

---

## Lock Provider 종류

### JDBC Lock Provider (MySQL)

가장 일반적으로 사용되는 Lock Provider입니다. 기존 RDBMS를 활용하므로 추가 인프라가 필요 없습니다.

```java
@Configuration
public class JdbcLockProviderConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime()  // 중요: DB 시간 사용
                .build()
        );
    }
}
```

**동작 원리 (SQL 레벨)**

```sql
-- 락 획득 시도 (INSERT or UPDATE)
INSERT INTO shedlock (name, lock_until, locked_at, locked_by)
VALUES ('taskName', NOW() + INTERVAL 30 MINUTE, NOW(), 'instance-1')
ON DUPLICATE KEY UPDATE
    lock_until = IF(lock_until <= NOW(),
        NOW() + INTERVAL 30 MINUTE, lock_until),
    locked_at = IF(lock_until <= NOW(), NOW(), locked_at),
    locked_by = IF(lock_until <= NOW(), 'instance-1', locked_by);

-- 영향 받은 행이 1이면 락 획득 성공
-- lock_until이 아직 미래 시간이면 UPDATE 조건 불충족 → 획득 실패
```

### Redis Lock Provider

Redis를 사용하면 DB 부하를 줄이고 더 빠른 락 획득이 가능합니다.

```java
@Configuration
public class RedisLockProviderConfig {

    @Bean
    public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory, "prod-env");
    }
}
```

```yaml
# application.yml
spring:
  data:
    redis:
      host: redis-cluster.internal
      port: 6379
      password: ${REDIS_PASSWORD}
      timeout: 5000ms
      lettuce:
        pool:
          max-active: 10
          max-idle: 5
```

### MongoDB Lock Provider

```java
@Configuration
public class MongoLockProviderConfig {

    @Bean
    public LockProvider lockProvider(MongoClient mongoClient) {
        return new MongoLockProvider(
            mongoClient.getDatabase("scheduler")
        );
    }
}
```

### Lock Provider 비교

| 비교 항목 | JDBC (MySQL) | Redis | MongoDB |
|-----------|-------------|-------|---------|
| **추가 인프라** | 불필요 (기존 DB 활용) | Redis 서버 필요 | MongoDB 필요 |
| **락 속도** | 보통 (DB I/O) | 빠름 (인메모리) | 보통 |
| **영속성** | 강함 (트랜잭션) | 약함 (메모리 기반) | 보통 |
| **가용성** | DB 가용성에 의존 | Redis Sentinel/Cluster | Replica Set |
| **DB 부하** | 있음 (추가 쿼리) | 없음 | 별도 DB |
| **적합 케이스** | 이미 RDBMS 사용 | 이미 Redis 사용, 고빈도 스케줄 | MongoDB 프로젝트 |
| **권장 환경** | 대부분의 프로젝트 | 높은 동시성 | MongoDB 기반 서비스 |

---

## 세무 도메인 적용

### 월별 기장 마감 배치 (매월 말일)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class BookkeepingScheduler {

    private final BookkeepingService bookkeepingService;
    private final NotificationService notificationService;

    /**
     * 월별 기장 마감 배치
     * - 매월 마지막 날 23:00에 실행
     * - 당월 거래내역 마감 처리
     * - 세무사에게 마감 완료 알림
     */
    @Scheduled(cron = "0 0 23 L * *")
    @SchedulerLock(
        name = "monthlyBookkeepingClose",
        lockAtMostFor = "PT2H",
        lockAtLeastFor = "PT30M"
    )
    public void closeMonthlyBookkeeping() {
        YearMonth targetMonth = YearMonth.now();
        log.info("월별 기장 마감 시작: {}", targetMonth);

        try {
            // 1. 미처리 거래내역 확인
            long pendingCount = bookkeepingService
                .countPendingTransactions(targetMonth);

            if (pendingCount > 0) {
                log.warn("미처리 거래내역 {}건 존재 - 자동 분류 시도",
                    pendingCount);
                bookkeepingService.autoClassify(targetMonth);
            }

            // 2. 기장 마감 처리
            BookkeepingCloseResult result = bookkeepingService
                .closeMonth(targetMonth);

            // 3. 마감 결과 알림
            notificationService.notifyBookkeepingClosed(
                targetMonth, result);

            log.info("월별 기장 마감 완료: month={}, processed={}건",
                targetMonth, result.getProcessedCount());

        } catch (Exception e) {
            log.error("월별 기장 마감 실패: {}", targetMonth, e);
            notificationService.notifyBookkeepingCloseFailed(
                targetMonth, e.getMessage());
        }
    }
}
```

### 세금 계산 배치 (분기별)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TaxScheduler {

    private final TaxCalculationService taxService;
    private final TaxCalendarService taxCalendarService;

    /**
     * 분기별 부가가치세 예정/확정 신고 계산
     * - 1월: 2기 확정 (전년 7~12월)
     * - 4월: 1기 예정 (1~3월)
     * - 7월: 1기 확정 (1~6월)
     * - 10월: 2기 예정 (7~9월)
     */
    @Scheduled(cron = "0 0 1 10 1,4,7,10 *")
    @SchedulerLock(
        name = "quarterlyVatCalculation",
        lockAtMostFor = "PT3H",
        lockAtLeastFor = "PT30M"
    )
    public void calculateQuarterlyVat() {
        Quarter quarter = taxCalendarService.getCurrentTaxQuarter();
        log.info("분기별 부가세 계산 시작: {}", quarter);

        List<TaxPayer> taxPayers = taxService.getActiveTaxPayers();

        for (TaxPayer payer : taxPayers) {
            try {
                VatCalculation result = taxService
                    .calculateVat(payer, quarter);

                log.debug("부가세 계산 완료: taxpayer={}, amount={}",
                    payer.getBusinessNumber(), result.getTotalAmount());

            } catch (Exception e) {
                log.error("부가세 계산 실패: taxpayer={}",
                    payer.getBusinessNumber(), e);
                // 개별 실패는 건너뛰고 계속 처리
            }
        }
    }

    /**
     * 종합소득세 계산 (매년 5월)
     */
    @Scheduled(cron = "0 0 0 1 5 *")
    @SchedulerLock(
        name = "annualIncomeTaxCalculation",
        lockAtMostFor = "PT4H",
        lockAtLeastFor = "PT1H"
    )
    public void calculateAnnualIncomeTax() {
        int targetYear = Year.now().getValue() - 1;
        log.info("종합소득세 계산 시작: {}년 귀속", targetYear);
        taxService.calculateIncomeTax(targetYear);
    }
}
```

### 환급 상태 조회 배치 (매일)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class RefundScheduler {

    private final RefundService refundService;
    private final HometaxApiClient hometaxApiClient;

    /**
     * 환급 상태 동기화
     * - 매일 09:00, 14:00, 18:00 실행
     * - 홈택스 API로 환급 처리 상태 조회
     */
    @Scheduled(cron = "0 0 9,14,18 * * MON-FRI")
    @SchedulerLock(
        name = "refundStatusSync",
        lockAtMostFor = "PT30M",
        lockAtLeastFor = "PT10M"
    )
    public void syncRefundStatus() {
        List<RefundRequest> pendingRefunds = refundService
            .getPendingRefunds();

        log.info("환급 상태 조회 시작: {}건", pendingRefunds.size());

        int updatedCount = 0;
        for (RefundRequest refund : pendingRefunds) {
            try {
                RefundStatus status = hometaxApiClient
                    .getRefundStatus(refund.getRefundId());

                if (status != refund.getStatus()) {
                    refundService.updateStatus(refund.getId(), status);
                    updatedCount++;

                    if (status == RefundStatus.COMPLETED) {
                        notificationService.notifyRefundCompleted(refund);
                    }
                }
            } catch (Exception e) {
                log.error("환급 상태 조회 실패: refundId={}",
                    refund.getRefundId(), e);
            }
        }

        log.info("환급 상태 조회 완료: 갱신 {}건 / 전체 {}건",
            updatedCount, pendingRefunds.size());
    }
}
```

### 세무 달력 기반 알림 배치

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TaxCalendarAlertScheduler {

    private final TaxCalendarService calendarService;
    private final NotificationService notificationService;

    /**
     * 세무 일정 사전 알림
     * - 매일 오전 9시 실행
     * - D-7, D-3, D-1 알림 발송
     */
    @Scheduled(cron = "0 0 9 * * MON-FRI")
    @SchedulerLock(
        name = "taxCalendarAlert",
        lockAtMostFor = "PT15M",
        lockAtLeastFor = "PT3M"
    )
    public void sendTaxCalendarAlerts() {
        LocalDate today = LocalDate.now();

        // D-7 알림
        List<TaxDeadline> upcoming7 = calendarService
            .getDeadlines(today.plusDays(7));
        sendAlerts(upcoming7, 7);

        // D-3 알림
        List<TaxDeadline> upcoming3 = calendarService
            .getDeadlines(today.plusDays(3));
        sendAlerts(upcoming3, 3);

        // D-1 알림 (긴급)
        List<TaxDeadline> upcoming1 = calendarService
            .getDeadlines(today.plusDays(1));
        sendAlerts(upcoming1, 1);
    }

    private void sendAlerts(List<TaxDeadline> deadlines, int daysLeft) {
        for (TaxDeadline deadline : deadlines) {
            notificationService.sendTaxDeadlineAlert(
                deadline, daysLeft);
        }
    }
}
```

---

## 운영 고려사항

### DB 커넥션 풀 영향

ShedLock은 매 스케줄 실행 시 DB 쿼리를 수행합니다. 스케줄 작업이 많으면 커넥션 풀에 부하가 발생할 수 있습니다.

```yaml
# application.yml - HikariCP 설정
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 5000
      # ShedLock 전용 DataSource 분리 권장 (작업이 많은 경우)
```

```java
// ShedLock 전용 DataSource 분리
@Configuration
public class ShedLockDataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.shedlock")
    public DataSource shedlockDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public LockProvider lockProvider(
            @Qualifier("shedlockDataSource") DataSource dataSource) {
        return new JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime()
                .build()
        );
    }
}
```

### 모니터링 (락 획득 실패 로깅)

```java
@Aspect
@Component
@Slf4j
public class ShedLockMonitoringAspect {

    private final MeterRegistry meterRegistry;

    public ShedLockMonitoringAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Around("@annotation(schedulerLock)")
    public Object monitorLock(ProceedingJoinPoint joinPoint,
                               SchedulerLock schedulerLock) throws Throwable {
        String lockName = schedulerLock.name();
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            Object result = joinPoint.proceed();

            // 메서드가 실행되었다면 락 획득 성공
            meterRegistry.counter("shedlock.acquired",
                Tags.of("name", lockName)).increment();

            sample.stop(meterRegistry.timer("shedlock.duration",
                Tags.of("name", lockName, "status", "success")));

            return result;
        } catch (Exception e) {
            meterRegistry.counter("shedlock.failed",
                Tags.of("name", lockName, "error", e.getClass().getSimpleName()))
                .increment();

            sample.stop(meterRegistry.timer("shedlock.duration",
                Tags.of("name", lockName, "status", "failed")));

            throw e;
        }
    }
}
```

### 장애 시 복구

```
┌──────────────────────────────────────────────────────────────────┐
│                    장애 시나리오 및 복구                            │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  1. 인스턴스 비정상 종료:                                          │
│     - lockAtMostFor 시간이 지나면 자동으로 락 해제                 │
│     - 다음 스케줄 시점에 다른 인스턴스가 실행                      │
│                                                                   │
│  2. DB 장애:                                                      │
│     - 락 획득 시도 시 예외 발생                                    │
│     - @Scheduled는 계속 트리거되므로 DB 복구 시 자동 재개          │
│     - fallback 전략 고려 (Redis 이중화 등)                        │
│                                                                   │
│  3. 락이 풀리지 않는 경우 (수동 복구):                             │
│     UPDATE shedlock                                               │
│     SET lock_until = NOW()                                        │
│     WHERE name = 'stuck_job_name';                                │
│                                                                   │
│  4. 시간 동기화 문제:                                              │
│     - usingDbTime() 설정으로 DB 시간 기준 통일                    │
│     - NTP 동기화 권장                                              │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 성능 최적화

```java
@Configuration
public class SchedulerOptimizationConfig {

    /**
     * TaskScheduler 스레드 풀 최적화
     * - 동시 실행 스케줄 수에 맞게 풀 크기 조정
     * - 너무 크면 불필요한 리소스 낭비
     * - 너무 작으면 스케줄 지연 발생
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(
            Runtime.getRuntime().availableProcessors());
        scheduler.setThreadNamePrefix("sched-");
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        return scheduler;
    }
}
```

```
성능 최적화 체크리스트:
──────────────────────────────────────────
□ usingDbTime() 설정 (클럭 차이 방지)
□ lockAtMostFor를 작업 최대 실행 시간의 2배로 설정
□ lockAtLeastFor를 스케줄 주기보다 짧게 설정
□ 고빈도 스케줄은 Redis Lock Provider 검토
□ 스케줄 작업이 10개 이상이면 전용 DataSource 분리
□ 불필요한 스케줄 작업 정리 (미사용 락 삭제)
□ ThreadPool 크기를 동시 실행 스케줄 수에 맞게 조정
```

---

## 핵심 정리

### ShedLock 도입 판단 기준

| 상황 | 권장 솔루션 |
|------|-----------|
| 단일 인스턴스, 단순 스케줄 | @Scheduled만 사용 |
| 다중 인스턴스, 단순 중복 방지 | **ShedLock** |
| 동적 Job 관리, 복잡한 트리거 | Quartz |
| 대규모 배치 + 스케줄링 | ShedLock + Spring Batch |
| 이벤트 기반 트리거 | Spring Cloud Stream / Kafka |

### 설정 체크리스트

```
ShedLock 도입 체크리스트:
─────────────────────────────────────────
□ Lock Provider 선택 (JDBC / Redis / MongoDB)
□ shedlock 테이블(또는 컬렉션) 생성
□ @EnableSchedulerLock 설정
□ defaultLockAtMostFor 설정
□ 각 작업별 lockAtMostFor / lockAtLeastFor 설정
□ usingDbTime() 활성화 (JDBC)
□ 모니터링 구성 (Micrometer + Grafana)
□ 장애 복구 절차 문서화
□ 테스트 환경 검증 (다중 인스턴스 시뮬레이션)
```

---

## 면접 대비 핵심 질문

1. **Q: ShedLock과 분산 락의 차이점은 무엇인가요?**
   - A: ShedLock은 분산 락의 일종이지만, 스케줄링에 특화된 라이브러리입니다. 범용 분산 락(Redisson, ZooKeeper)과 달리 `@Scheduled` 어노테이션과 통합되어 선언적으로 사용할 수 있고, lockAtMostFor/lockAtLeastFor 같은 스케줄링 특화 설정을 제공합니다. ShedLock은 "at most once" 실행을 보장하되, 정확히 한 번 실행을 보장하지는 않습니다(인스턴스가 전부 다운되면 실행되지 않음).

2. **Q: lockAtMostFor와 lockAtLeastFor의 설정 기준은?**
   - A: lockAtMostFor는 작업의 예상 최대 실행 시간의 2배 정도로 설정합니다. 이보다 짧으면 작업 실행 중에 락이 풀려 다른 인스턴스에서 중복 실행될 수 있습니다. lockAtLeastFor는 클럭 드리프트와 네트워크 지연을 고려하여 스케줄 간격보다 짧게 설정합니다. 예를 들어 매 5분 실행 작업이라면 lockAtLeastFor는 "PT4M" 이하로 설정합니다.

3. **Q: Jenkins 대신 ShedLock으로 배치를 관리하는 이유는?**
   - A: Jenkins를 사용하면 별도 서버 운영, 네트워크 의존성, Jenkins 장애 시 배치 미실행 등의 문제가 있습니다. ShedLock + @Scheduled를 사용하면 배치 실행 로직이 애플리케이션에 포함되어 배포와 스케줄 관리가 일원화됩니다. Git으로 스케줄 변경 이력을 추적할 수 있고, 외부 시스템 의존성이 제거됩니다. 다만 동적 Job 관리나 복잡한 트리거가 필요하면 Quartz나 Jenkins가 더 적합할 수 있습니다.

4. **Q: Quartz와 ShedLock 중 어떤 것을 선택해야 하나요?**
   - A: ShedLock은 "이미 존재하는 @Scheduled에 중복 실행 방지만 추가"하는 경량 솔루션입니다. Quartz는 동적 Job 추가/삭제, 복잡한 트리거(Calendar, Misfire 정책), Job 일시정지/재개 등 완전한 스케줄링 프레임워크가 필요할 때 선택합니다. 대부분의 경우 ShedLock으로 충분하며, 설정이 단순하고 테이블도 1개만 필요합니다. zaritalk에서 Quartz를 사용한 경험 기반으로 말하면, Quartz의 11개 이상 테이블 관리와 복잡한 설정은 단순 중복 방지 목적에는 과도합니다.

5. **Q: DB 장애 시 ShedLock은 어떻게 동작하나요?**
   - A: DB 장애 시 Lock Provider가 락 획득에 실패하여 예외가 발생합니다. 이 경우 스케줄 작업은 실행되지 않습니다. @Scheduled는 계속 트리거를 시도하므로 DB가 복구되면 자동으로 락 획득이 가능해져 작업이 재개됩니다. 높은 가용성이 필요하면 Redis Lock Provider를 fallback으로 구성하거나, DB 이중화(Primary-Replica)를 적용합니다. lockAtMostFor가 적절히 설정되어 있다면 비정상 종료된 인스턴스의 락도 자동 해제됩니다.

6. **Q: ShedLock에서 작업 실행 여부를 모니터링하려면?**
   - A: ShedLock 자체는 모니터링 기능을 제공하지 않으므로 별도 구현이 필요합니다. AOP Aspect를 활용하여 락 획득 성공/실패를 Micrometer 메트릭으로 수집하고, Grafana 대시보드로 시각화합니다. shedlock 테이블의 locked_at, lock_until 값을 주기적으로 조회하여 작업 실행 이력을 확인할 수도 있습니다. 장시간 락이 유지되는 경우(stuck job)를 감지하여 알림을 보내는 것도 중요합니다.

7. **Q: 세무 도메인에서 ShedLock을 활용하는 주요 배치 작업은?**
   - A: 월별 기장 마감 배치(매월 말일), 분기별 부가세 계산(1/4/7/10월), 종합소득세 계산(매년 5월), 환급 상태 동기화(매일), 세무 달력 알림(D-7/3/1) 등이 있습니다. 각 배치는 세무 달력에 맞춘 cron 표현식을 사용하고, lockAtMostFor은 작업 규모에 따라 15분~4시간으로 차등 설정합니다. 특히 신고 기한 직전에는 대량 데이터가 처리되므로 lockAtMostFor를 넉넉하게 설정해야 합니다.

8. **Q: ShedLock 테스트는 어떻게 하나요?**
   - A: 단위 테스트에서는 ShedLock을 비활성화하고 비즈니스 로직만 테스트합니다. 통합 테스트에서는 Testcontainers로 실제 DB를 사용하거나, H2 인메모리 DB로 shedlock 테이블을 생성하여 테스트합니다. 중복 실행 방지를 검증하려면 두 개의 스레드에서 동시에 실행하여 하나만 성공하는지 확인합니다.

---

*마지막 업데이트: 2026년 02월*
