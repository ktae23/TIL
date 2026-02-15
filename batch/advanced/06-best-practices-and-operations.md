# 공통 베스트 프랙티스와 운영

Spring Batch를 프로덕션에서 안정적으로 운영하기 위한 베스트 프랙티스(네이밍, 로깅, 모니터링)와 장애 대응 패턴(재시작, 수동 개입), 그리고 운영 체크리스트를 정리한다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### 베스트 프랙티스 영역

Spring Batch 운영에서 다루어야 할 핵심 영역은 크게 세 가지다:

```
┌────────────────────────────────────────────────────────────┐
│               Spring Batch 운영 3대 영역                     │
│                                                              │
│  1. 설계 시점 (Design-time)                                 │
│     └── Job 파라미터 설계, Dry Run 모드, 멱등성 보장        │
│                                                              │
│  2. 실행 시점 (Runtime)                                     │
│     └── 모니터링, 알림, 메트릭 수집                         │
│                                                              │
│  3. 장애 시점 (Failure-time)                                │
│     └── 부분 재처리, 보상 트랜잭션, 데드락 방지             │
└────────────────────────────────────────────────────────────┘
```

---

## 2. 왜 알아야 하는가 (Why)

배치 시스템은 "실행하면 끝"이 아니다. 프로덕션 환경에서는 다음과 같은 상황이 반드시 발생한다:

- 외부 시스템 장애로 인한 부분 실패
- 데이터 정합성 문제로 인한 Skip 급증
- 야간 배치 실패 후 아침에 발견하는 상황
- 동일 배치의 중복 실행 요청

이런 상황에 대비하지 않으면, 장애가 발생할 때마다 수동으로 데이터를 확인하고 수정해야 한다. 체계적인 운영 프랙티스는 장애 대응 시간을 줄이고 시스템 신뢰성을 높인다.

---

## 3. 내부 구현 분석 (How)

### 3.1 Job 파라미터 설계

Job 파라미터는 배치 실행의 입력값이자 멱등성의 기반이다. 필수/선택 파라미터를 명확히 구분하고, 유효성 검증을 반드시 수행한다.

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

핵심 포인트:
- `CompositeJobParametersValidator`로 여러 검증기를 조합
- 필수 파라미터(`billingDate`)와 선택 파라미터(`dryRun`, `maxRetryCount`)를 명시적으로 구분
- 커스텀 검증 로직으로 날짜 형식까지 검증

### 3.2 Dry Run 모드

프로덕션 환경에서 실제 실행 전에 시뮬레이션을 돌려볼 수 있어야 한다. `dryRun` 파라미터로 실행 모드를 제어한다.

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

### 3.3 배치 모니터링 (Micrometer 연동)

Spring Batch의 `JobExecutionListener`와 `StepExecutionListener`를 활용하여 Micrometer 메트릭을 수집한다.

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

수집되는 메트릭:

| 메트릭 | 타입 | 설명 |
|--------|------|------|
| `batch.job.completed` | Counter | Job 완료 횟수 (상태별) |
| `batch.job.duration` | Timer | Job 실행 시간 |
| `batch.step.read_count` | Gauge | Step별 읽기 건수 |
| `batch.step.write_count` | Gauge | Step별 쓰기 건수 |
| `batch.step.skip_count` | Gauge | Step별 Skip 건수 |

### 3.4 알림 설정 (Slack + PagerDuty)

배치 실패 시 자동 알림은 운영의 필수 요소다. 성공/경고/실패 수준별로 알림 채널을 분리한다.

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

알림 전략:

```
성공 (COMPLETED)     → Slack 성공 메시지
경고 (Skip > 100건)  → Slack 경고 메시지
실패 (FAILED)        → Slack 에러 메시지
크리티컬 실패         → Slack + PagerDuty 인시던트
```

---

## 4. 실전 예제

### 4.1 부분 재처리 (실패 건만 선별 처리)

전체 배치를 재실행하는 대신, 이전 실행에서 실패한 건만 선별하여 재처리한다.

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

### 4.2 보상 트랜잭션 (롤백 불가능한 외부 연동)

결제 성공 후 DB 저장이 실패하면 이미 처리된 결제를 취소해야 한다. 보상 실패 시에는 수동 처리 큐로 이관한다.

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

### 4.3 데드락 방지

낙관적 락(`@Version`)과 데드락/낙관적 락 예외에 대한 Retry를 조합한다.

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

### 4.4 운영 체크리스트

#### 배포 전 체크리스트

- [ ] Chunk Size 적절한가? (테스트 환경에서 검증)
- [ ] 멱등성 보장되는가? (동일 입력 -> 동일 결과)
- [ ] Skip/Retry 정책 적절한가?
- [ ] 타임아웃 설정되어 있는가? (외부 연동)
- [ ] Dry Run 모드 동작하는가?
- [ ] 인덱스 최적화되어 있는가? (실행 계획 확인)
- [ ] 메타데이터 테이블 정리 정책 있는가?
- [ ] 핵심 메트릭 수집되는가?
- [ ] 알림 설정되어 있는가?

#### 실행 전 체크리스트

- [ ] 처리 대상 건수 예상치와 일치하는가?
- [ ] 이전 실행 결과 정상 종료되었는가?
- [ ] 중복 실행 아닌가?
- [ ] DB 커넥션 풀 여유 있는가?
- [ ] 디스크 용량 충분한가?
- [ ] 외부 시스템 정상인가? (PG, 알림 등)
- [ ] 파라미터 올바른가?
- [ ] 실행 시간대 적절한가? (트래픽 낮은 시간)

#### 실행 후 체크리스트

- [ ] 처리 건수 예상치와 일치하는가?
- [ ] 실패/스킵 건수 허용 범위인가?
- [ ] 비즈니스 데이터 정합성 확인 (금액 합계 등)
- [ ] 실패 건 원인 분석 완료
- [ ] 재처리 필요 건 식별
- [ ] 이해관계자 결과 공유

---

## 5. 정리

| 영역 | 프랙티스 | 핵심 도구/패턴 |
|------|---------|---------------|
| **파라미터 설계** | 필수/선택 분리, 유효성 검증 | `CompositeJobParametersValidator` |
| **Dry Run** | 실제 실행 전 시뮬레이션 | Job Parameter 기반 분기 |
| **모니터링** | 메트릭 수집 (실행 시간, 건수) | Micrometer + `JobExecutionListener` |
| **알림** | 수준별 알림 채널 분리 | Slack (일반) + PagerDuty (크리티컬) |
| **부분 재처리** | 실패 건만 선별 재실행 | `originalJobExecutionId` 파라미터 |
| **보상 트랜잭션** | 외부 API 롤백 불가 시 보상 | `idempotencyKey` 기반 환불 |
| **데드락 방지** | 낙관적 락 + Retry | `@Version` + `retryLimit` |
| **운영 체크리스트** | 배포 전/실행 전/실행 후 3단계 | 팀 공유 문서화 |

배치 시스템의 안정성은 코드 품질만으로 결정되지 않는다. 설계 시점의 방어적 프로그래밍, 실행 시점의 모니터링, 장애 시점의 자동 대응이 삼위일체로 갖춰져야 프로덕션에서 신뢰할 수 있는 배치 시스템이 된다.

---
*참고: Spring Batch 5.x / Spring Boot 3.x 기준*
