# Spring Batch 실무 베스트 프랙티스 - 정기 결제 & 정산 사례

실무에서 자주 사용되는 정기 결제와 정산 배치를 예시로 베스트 프랙티스를 정리한다.

## 목차

- [정기 결제 배치 설계](#정기-결제-배치-설계)
- [정산 배치 설계](#정산-배치-설계)
- [공통 베스트 프랙티스](#공통-베스트-프랙티스)
- [장애 대응 패턴](#장애-대응-패턴)
- [운영 체크리스트](#운영-체크리스트)

---

## 정기 결제 배치 설계

### 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────────────┐
│                    정기 결제 배치 Job                                │
│                                                                      │
│  ┌────────────┐   ┌────────────┐   ┌────────────┐   ┌────────────┐ │
│  │ 대상 조회   │──▶│ 결제 시도   │──▶│ 결과 처리   │──▶│ 알림 발송   │ │
│  │   Step     │   │   Step     │   │   Step     │   │   Step     │ │
│  └────────────┘   └────────────┘   └────────────┘   └────────────┘ │
│        │               │                │                │         │
│        ▼               ▼                ▼                ▼         │
│   subscription    payment_log      subscription      notification  │
│      table           table            table             queue      │
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

### Step 1: 결제 대상 조회

```java
@Configuration
@RequiredArgsConstructor
public class BillingTargetStepConfig {

    private final DataSource dataSource;

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

        Map<String, Order> sortKeys = new LinkedHashMap<>();
        sortKeys.put("id", Order.ASCENDING);

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
                .sortKeys(sortKeys)
                .pageSize(100)
                .rowMapper(new BeanPropertyRowMapper<>(Subscription.class))
                .build();
    }
}
```

### Step 2: 결제 처리 (핵심)

```java
@Configuration
@RequiredArgsConstructor
public class PaymentProcessStepConfig {

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
                .<Subscription, PaymentResult>chunk(10, transactionManager)  // 작은 chunk
                .reader(billingTargetReader(null))
                .processor(paymentProcessor())
                .writer(paymentResultWriter())
                .faultTolerant()
                .retry(PgConnectionException.class)      // PG 연결 오류는 재시도
                .retry(PgTimeoutException.class)
                .retryLimit(3)
                .backOffPolicy(exponentialBackOff())     // 지수 백오프
                .skip(PaymentDeclinedException.class)    // 카드 거절은 스킵
                .skip(InvalidPaymentMethodException.class)
                .skipLimit(Integer.MAX_VALUE)            // 스킵은 무제한 (로그로 추적)
                .listener(paymentSkipListener())
                .listener(paymentRetryListener())
                .build();
    }

    @Bean
    public BackOffPolicy exponentialBackOff() {
        ExponentialBackOffPolicy policy = new ExponentialBackOffPolicy();
        policy.setInitialInterval(1000);   // 1초
        policy.setMultiplier(2.0);         // 2배씩 증가
        policy.setMaxInterval(10000);      // 최대 10초
        return policy;
    }
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

    /**
     * Best Practice: 멱등성 보장
     * - 동일한 구독 + 결제일 조합으로 idempotencyKey 생성
     * - 이미 처리된 결제는 스킵
     * - PG사에도 idempotencyKey 전달
     */
    @Override
    public PaymentResult process(Subscription subscription) throws Exception {
        String idempotencyKey = generateIdempotencyKey(subscription);

        // 이미 처리된 결제인지 확인
        Optional<PaymentLog> existingLog = paymentLogRepository
                .findByIdempotencyKey(idempotencyKey);

        if (existingLog.isPresent()) {
            log.info("이미 처리된 결제 - subscriptionId: {}, key: {}",
                    subscription.getId(), idempotencyKey);
            return PaymentResult.alreadyProcessed(existingLog.get());
        }

        try {
            // PG 결제 요청 (타임아웃 설정 필수)
            PgResponse response = paymentGateway.charge(
                    PaymentRequest.builder()
                            .amount(subscription.getAmount())
                            .paymentMethodId(subscription.getPaymentMethodId())
                            .idempotencyKey(idempotencyKey)
                            .metadata(Map.of(
                                    "subscriptionId", subscription.getId(),
                                    "userId", subscription.getUserId()
                            ))
                            .build()
            );

            return PaymentResult.success(subscription, response.getTransactionId());

        } catch (PaymentDeclinedException e) {
            // 카드 거절: 재시도해도 의미 없음
            log.warn("결제 거절 - subscriptionId: {}, reason: {}",
                    subscription.getId(), e.getDeclineCode());
            return PaymentResult.failed(subscription, e.getDeclineCode());

        } catch (PgException e) {
            // PG 오류: 상위에서 재시도 처리
            log.error("PG 오류 - subscriptionId: {}", subscription.getId(), e);
            throw e;
        }
    }

    private String generateIdempotencyKey(Subscription subscription) {
        return String.format("billing_%d_%s",
                subscription.getId(),
                subscription.getNextBillingDate());
    }
}
```

### 결제 결과 Writer - 트랜잭션 분리

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentResultWriter implements ItemWriter<PaymentResult> {

    private final PaymentLogRepository paymentLogRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final EventPublisher eventPublisher;

    /**
     * Best Practice: 결과 처리 분리
     * - 결제 로그 저장 (항상)
     * - 구독 상태 업데이트 (성공/실패에 따라)
     * - 이벤트 발행 (후속 처리용)
     */
    @Override
    public void write(Chunk<? extends PaymentResult> results) throws Exception {
        List<PaymentLog> logs = new ArrayList<>();
        List<Subscription> subscriptionsToUpdate = new ArrayList<>();

        for (PaymentResult result : results) {
            if (result.isAlreadyProcessed()) {
                continue;  // 이미 처리된 건은 스킵
            }

            // 1. 결제 로그 생성
            PaymentLog log = PaymentLog.builder()
                    .subscriptionId(result.getSubscriptionId())
                    .userId(result.getUserId())
                    .amount(result.getAmount())
                    .transactionId(result.getTransactionId())
                    .status(result.isSuccess() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED)
                    .failureReason(result.getFailureReason())
                    .idempotencyKey(result.getIdempotencyKey())
                    .processedAt(LocalDateTime.now())
                    .build();
            logs.add(log);

            // 2. 구독 상태 업데이트
            Subscription subscription = result.getSubscription();
            if (result.isSuccess()) {
                subscription.paymentSucceeded();  // 다음 결제일 설정, 실패 횟수 초기화
            } else {
                subscription.paymentFailed();     // 실패 횟수 증가
            }
            subscriptionsToUpdate.add(subscription);
        }

        // 벌크 저장
        paymentLogRepository.saveAll(logs);
        subscriptionRepository.saveAll(subscriptionsToUpdate);

        // 3. 이벤트 발행 (비동기 후속 처리)
        for (PaymentResult result : results) {
            if (result.isSuccess()) {
                eventPublisher.publish(new PaymentSucceededEvent(result));
            } else {
                eventPublisher.publish(new PaymentFailedEvent(result));
            }
        }
    }
}
```

### Step 3: 실패 처리 (별도 Step)

```java
@Configuration
@RequiredArgsConstructor
public class FailureHandlingStepConfig {

    /**
     * Best Practice: 실패 케이스 별도 처리
     * - 1-2회 실패: 다음 날 재시도
     * - 3회 실패: 구독 일시정지 + 사용자 알림
     * - 결제 수단 만료: 업데이트 요청 알림
     */
    @Bean
    public Step failureHandlingStep(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager) {
        return new StepBuilder("failureHandlingStep", jobRepository)
                .<PaymentLog, FailureAction>chunk(100, transactionManager)
                .reader(failedPaymentReader(null))
                .processor(failureActionProcessor())
                .writer(failureActionWriter())
                .build();
    }

    @Bean
    @StepScope
    public JdbcPagingItemReader<PaymentLog> failedPaymentReader(
            @Value("#{jobParameters['billingDate']}") String billingDate) {

        return new JdbcPagingItemReaderBuilder<PaymentLog>()
                .name("failedPaymentReader")
                .dataSource(dataSource)
                .selectClause("SELECT pl.*, s.failed_attempts, s.user_id")
                .fromClause("""
                    FROM payment_logs pl
                    JOIN subscriptions s ON pl.subscription_id = s.id
                    """)
                .whereClause("""
                    WHERE DATE(pl.processed_at) = :billingDate
                      AND pl.status = 'FAILED'
                    """)
                .parameterValues(Map.of("billingDate", billingDate))
                .sortKeys(Map.of("pl.id", Order.ASCENDING))
                .pageSize(100)
                .rowMapper(new FailedPaymentRowMapper())
                .build();
    }

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
}
```

---

## 정산 배치 설계

### 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         정산 배치 Job                                    │
│                                                                          │
│  ┌──────────────┐                                                       │
│  │ 검증 Step    │  거래 내역 무결성 검증                                  │
│  └──────┬───────┘                                                       │
│         │                                                                │
│         ▼                                                                │
│  ┌──────────────┐                                                       │
│  │ 집계 Step    │  판매자별 거래 집계 (파티셔닝)                          │
│  │ (Partitioned)│                                                       │
│  └──────┬───────┘                                                       │
│         │                                                                │
│         ▼                                                                │
│  ┌──────────────┐                                                       │
│  │ 정산금 계산   │  수수료 차감, 세금 계산                                │
│  └──────┬───────┘                                                       │
│         │                                                                │
│         ▼                                                                │
│  ┌──────────────┐   ┌──────────────┐                                   │
│  │ 정산서 생성   │──▶│ 출금 요청    │  (별도 Job 또는 수동 승인 후)       │
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
    private LocalDate settlementDate;     // 정산 기준일
    private LocalDate periodStart;        // 정산 기간 시작
    private LocalDate periodEnd;          // 정산 기간 종료

    private BigDecimal totalSales;        // 총 매출
    private BigDecimal platformFee;       // 플랫폼 수수료
    private BigDecimal pgFee;             // PG 수수료
    private BigDecimal tax;               // 세금
    private BigDecimal netAmount;         // 정산 금액

    private int transactionCount;         // 거래 건수

    @Enumerated(EnumType.STRING)
    private SettlementStatus status;      // PENDING, CONFIRMED, PAID

    private String checksum;              // 무결성 검증용
}

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    private Long id;
    private Long orderId;
    private Long sellerId;
    private Long buyerId;
    private BigDecimal amount;
    private BigDecimal platformFee;
    private BigDecimal pgFee;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;     // COMPLETED, REFUNDED, CANCELLED

    private LocalDateTime completedAt;
    private boolean settled;              // 정산 완료 여부
    private Long settlementId;
}
```

### Step 1: 거래 내역 검증

```java
@Configuration
@RequiredArgsConstructor
public class ValidationStepConfig {

    /**
     * Best Practice: 정산 전 데이터 무결성 검증
     * - 거래 금액 합계 vs 결제 금액 합계 일치 확인
     * - 중복 거래 체크
     * - 누락 거래 체크
     */
    @Bean
    public Step validationStep(JobRepository jobRepository,
                               PlatformTransactionManager transactionManager) {
        return new StepBuilder("validationStep", jobRepository)
                .tasklet(validationTasklet(null, null), transactionManager)
                .listener(new StepExecutionListener() {
                    @Override
                    public ExitStatus afterStep(StepExecution stepExecution) {
                        // 검증 실패 시 Job 중단
                        Boolean isValid = (Boolean) stepExecution
                                .getExecutionContext().get("validationPassed");
                        if (!Boolean.TRUE.equals(isValid)) {
                            return new ExitStatus("VALIDATION_FAILED");
                        }
                        return ExitStatus.COMPLETED;
                    }
                })
                .build();
    }

    @Bean
    @StepScope
    public Tasklet validationTasklet(
            @Value("#{jobParameters['periodStart']}") String periodStart,
            @Value("#{jobParameters['periodEnd']}") String periodEnd) {

        return (contribution, chunkContext) -> {
            ExecutionContext context = chunkContext.getStepContext()
                    .getStepExecution().getExecutionContext();

            // 1. 거래 금액 합계 조회
            BigDecimal transactionSum = jdbcTemplate.queryForObject(
                    """
                    SELECT COALESCE(SUM(amount), 0)
                    FROM transactions
                    WHERE status = 'COMPLETED'
                      AND completed_at BETWEEN ? AND ?
                      AND settled = false
                    """,
                    BigDecimal.class, periodStart, periodEnd);

            // 2. 결제 금액 합계 조회
            BigDecimal paymentSum = jdbcTemplate.queryForObject(
                    """
                    SELECT COALESCE(SUM(amount), 0)
                    FROM payments
                    WHERE status = 'SUCCESS'
                      AND paid_at BETWEEN ? AND ?
                    """,
                    BigDecimal.class, periodStart, periodEnd);

            // 3. 차이 검증 (오차 허용: 0.01%)
            BigDecimal diff = transactionSum.subtract(paymentSum).abs();
            BigDecimal tolerance = transactionSum.multiply(new BigDecimal("0.0001"));

            boolean isValid = diff.compareTo(tolerance) <= 0;
            context.put("validationPassed", isValid);
            context.put("transactionSum", transactionSum);
            context.put("paymentSum", paymentSum);

            if (!isValid) {
                log.error("정산 검증 실패 - 거래합계: {}, 결제합계: {}, 차이: {}",
                        transactionSum, paymentSum, diff);
                // 알림 발송
                alertService.sendValidationFailure(transactionSum, paymentSum, diff);
            }

            return RepeatStatus.FINISHED;
        };
    }
}
```

### Step 2: 판매자별 집계 (파티셔닝)

```java
@Configuration
@RequiredArgsConstructor
public class AggregationStepConfig {

    /**
     * Best Practice: 대용량 집계는 파티셔닝으로
     * - 판매자 ID 기준 파티셔닝
     * - 각 파티션 독립적으로 처리
     * - 장애 시 해당 파티션만 재처리
     */
    @Bean
    public Step aggregationManagerStep(JobRepository jobRepository) {
        return new StepBuilder("aggregationManagerStep", jobRepository)
                .partitioner("aggregationWorkerStep", sellerPartitioner(null, null))
                .step(aggregationWorkerStep(null, null))
                .gridSize(10)  // 10개 파티션
                .taskExecutor(aggregationTaskExecutor())
                .build();
    }

    @Bean
    @StepScope
    public Partitioner sellerPartitioner(
            @Value("#{jobParameters['periodStart']}") String periodStart,
            @Value("#{jobParameters['periodEnd']}") String periodEnd) {

        return gridSize -> {
            // 정산 대상 판매자 목록 조회
            List<Long> sellerIds = jdbcTemplate.queryForList(
                    """
                    SELECT DISTINCT seller_id
                    FROM transactions
                    WHERE status = 'COMPLETED'
                      AND completed_at BETWEEN ? AND ?
                      AND settled = false
                    ORDER BY seller_id
                    """,
                    Long.class, periodStart, periodEnd);

            // 판매자를 gridSize 개의 파티션으로 분배
            Map<String, ExecutionContext> partitions = new HashMap<>();
            int partitionSize = (sellerIds.size() / gridSize) + 1;

            for (int i = 0; i < gridSize; i++) {
                int start = i * partitionSize;
                int end = Math.min(start + partitionSize, sellerIds.size());

                if (start >= sellerIds.size()) break;

                List<Long> partitionSellerIds = sellerIds.subList(start, end);

                ExecutionContext context = new ExecutionContext();
                context.put("sellerIds", partitionSellerIds);
                context.putString("partitionName", "partition" + i);

                partitions.put("partition" + i, context);
            }

            return partitions;
        };
    }

    @Bean
    public Step aggregationWorkerStep(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager) {
        return new StepBuilder("aggregationWorkerStep", jobRepository)
                .<Long, SellerAggregation>chunk(10, transactionManager)
                .reader(sellerIdReader(null))
                .processor(aggregationProcessor(null, null))
                .writer(aggregationWriter())
                .build();
    }

    @Bean
    @StepScope
    public ListItemReader<Long> sellerIdReader(
            @Value("#{stepExecutionContext['sellerIds']}") List<Long> sellerIds) {
        return new ListItemReader<>(sellerIds);
    }
}
```

### 집계 Processor

```java
@Component
@RequiredArgsConstructor
public class AggregationProcessor implements ItemProcessor<Long, SellerAggregation> {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Best Practice: 정확한 금액 계산
     * - BigDecimal 사용 (부동소수점 오차 방지)
     * - 반올림 정책 명시
     * - 수수료율 중앙 관리
     */
    @Override
    public SellerAggregation process(Long sellerId) throws Exception {
        // 판매자별 거래 집계
        Map<String, Object> result = jdbcTemplate.queryForMap(
                """
                SELECT
                    COUNT(*) as transaction_count,
                    COALESCE(SUM(amount), 0) as total_sales,
                    COALESCE(SUM(platform_fee), 0) as total_platform_fee,
                    COALESCE(SUM(pg_fee), 0) as total_pg_fee
                FROM transactions
                WHERE seller_id = ?
                  AND status = 'COMPLETED'
                  AND completed_at BETWEEN ? AND ?
                  AND settled = false
                """,
                sellerId, periodStart, periodEnd);

        BigDecimal totalSales = (BigDecimal) result.get("total_sales");
        BigDecimal platformFee = (BigDecimal) result.get("total_platform_fee");
        BigDecimal pgFee = (BigDecimal) result.get("total_pg_fee");
        int transactionCount = ((Number) result.get("transaction_count")).intValue();

        // 세금 계산 (원천징수 3.3%)
        BigDecimal taxRate = new BigDecimal("0.033");
        BigDecimal taxableAmount = totalSales.subtract(platformFee).subtract(pgFee);
        BigDecimal tax = taxableAmount.multiply(taxRate)
                .setScale(0, RoundingMode.DOWN);  // 원 단위 절사

        // 정산 금액
        BigDecimal netAmount = taxableAmount.subtract(tax);

        // 체크섬 생성 (무결성 검증용)
        String checksum = generateChecksum(sellerId, totalSales, netAmount);

        return SellerAggregation.builder()
                .sellerId(sellerId)
                .totalSales(totalSales)
                .platformFee(platformFee)
                .pgFee(pgFee)
                .tax(tax)
                .netAmount(netAmount)
                .transactionCount(transactionCount)
                .checksum(checksum)
                .build();
    }

    private String generateChecksum(Long sellerId, BigDecimal totalSales,
                                     BigDecimal netAmount) {
        String data = String.format("%d:%s:%s:%s",
                sellerId, totalSales, netAmount, periodStart);
        return DigestUtils.sha256Hex(data);
    }
}
```

### Step 3: 정산서 생성

```java
@Component
@RequiredArgsConstructor
public class SettlementWriter implements ItemWriter<SellerAggregation> {

    private final SettlementRepository settlementRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Best Practice: 정산 데이터 원자성 보장
     * - 정산서 생성 + 거래 내역 정산 완료 처리를 하나의 트랜잭션으로
     * - 정산 ID로 거래 내역 역추적 가능하도록
     */
    @Override
    @Transactional
    public void write(Chunk<? extends SellerAggregation> aggregations) throws Exception {
        for (SellerAggregation agg : aggregations) {
            // 1. 정산서 생성
            Settlement settlement = Settlement.builder()
                    .sellerId(agg.getSellerId())
                    .settlementDate(LocalDate.now())
                    .periodStart(periodStart)
                    .periodEnd(periodEnd)
                    .totalSales(agg.getTotalSales())
                    .platformFee(agg.getPlatformFee())
                    .pgFee(agg.getPgFee())
                    .tax(agg.getTax())
                    .netAmount(agg.getNetAmount())
                    .transactionCount(agg.getTransactionCount())
                    .status(SettlementStatus.PENDING)
                    .checksum(agg.getChecksum())
                    .build();

            settlement = settlementRepository.save(settlement);

            // 2. 해당 거래 내역에 정산 ID 기록 및 정산 완료 처리
            transactionRepository.markAsSettled(
                    agg.getSellerId(),
                    periodStart,
                    periodEnd,
                    settlement.getId()
            );

            log.info("정산서 생성 완료 - sellerId: {}, netAmount: {}, txCount: {}",
                    agg.getSellerId(), agg.getNetAmount(), agg.getTransactionCount());
        }
    }
}
```

---

## 공통 베스트 프랙티스

### 1. Job 파라미터 설계

```java
/**
 * Best Practice: 필수/선택 파라미터 명확히 구분
 * - 필수: 실행에 반드시 필요한 값
 * - 선택: 기본값이 있는 값
 * - 실행 시점 자동 생성: runId, timestamp 등
 */
@Bean
public Job billingJob(JobRepository jobRepository) {
    return new JobBuilder("billingJob", jobRepository)
            .validator(new CompositeJobParametersValidator(List.of(
                    // 필수 파라미터
                    new DefaultJobParametersValidator(
                            new String[]{"billingDate"},
                            new String[]{"dryRun", "maxRetryCount"}
                    ),
                    // 커스텀 검증
                    parameters -> {
                        String billingDate = parameters.getString("billingDate");
                        if (!isValidDate(billingDate)) {
                            throw new JobParametersInvalidException(
                                    "Invalid billingDate format: " + billingDate);
                        }
                    }
            )))
            .incrementer(new RunIdIncrementer())  // 동일 파라미터 재실행 허용
            .start(paymentStep())
            .build();
}

// 실행 예시
JobParameters params = new JobParametersBuilder()
        .addString("billingDate", "2024-01-15")
        .addString("dryRun", "false")           // 선택
        .addLong("runId", System.currentTimeMillis())  // 자동 생성
        .toJobParameters();
```

### 2. 배치 실행 모드 (Dry Run)

```java
/**
 * Best Practice: Dry Run 모드 지원
 * - 실제 DB/외부 시스템 변경 없이 로직 검증
 * - 예상 처리 건수, 금액 등 미리 확인
 */
@Component
@RequiredArgsConstructor
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

        // 실제 결제 로직
        return executePayment(subscription);
    }
}

@Component
public class DryRunWriter implements ItemWriter<PaymentResult> {

    @Value("#{jobParameters['dryRun'] ?: 'false'}")
    private boolean dryRun;

    @Override
    public void write(Chunk<? extends PaymentResult> items) {
        if (dryRun) {
            // 통계만 기록
            long successCount = items.getItems().stream()
                    .filter(PaymentResult::isSuccess).count();
            BigDecimal totalAmount = items.getItems().stream()
                    .map(PaymentResult::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.info("[DRY RUN] 처리 예정 - 건수: {}, 성공: {}, 금액: {}",
                    items.size(), successCount, totalAmount);
            return;
        }

        // 실제 저장 로직
        actualWriter.write(items);
    }
}
```

### 3. 배치 모니터링

```java
/**
 * Best Practice: 핵심 메트릭 수집
 * - 처리량 (items/sec)
 * - 성공/실패 건수
 * - 처리 시간
 * - 비즈니스 메트릭 (결제 금액 등)
 */
@Component
@RequiredArgsConstructor
public class BatchMetricsListener implements JobExecutionListener, StepExecutionListener {

    private final MeterRegistry meterRegistry;

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        String status = jobExecution.getStatus().toString();

        // Job 완료 카운터
        meterRegistry.counter("batch.job.completed",
                "job", jobName,
                "status", status
        ).increment();

        // Job 실행 시간
        long duration = Duration.between(
                jobExecution.getStartTime(),
                jobExecution.getEndTime()
        ).toMillis();

        meterRegistry.timer("batch.job.duration", "job", jobName)
                .record(duration, TimeUnit.MILLISECONDS);

        // 비즈니스 메트릭 (ExecutionContext에서 수집)
        ExecutionContext context = jobExecution.getExecutionContext();
        if (context.containsKey("totalPaymentAmount")) {
            meterRegistry.gauge("batch.billing.total_amount",
                    Tags.of("job", jobName),
                    context.getDouble("totalPaymentAmount"));
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String stepName = stepExecution.getStepName();

        // Step 처리 건수
        meterRegistry.gauge("batch.step.read_count",
                Tags.of("step", stepName),
                stepExecution.getReadCount());

        meterRegistry.gauge("batch.step.write_count",
                Tags.of("step", stepName),
                stepExecution.getWriteCount());

        meterRegistry.gauge("batch.step.skip_count",
                Tags.of("step", stepName),
                stepExecution.getSkipCount());

        // 처리량 계산
        long duration = Duration.between(
                stepExecution.getStartTime(),
                stepExecution.getEndTime()
        ).toSeconds();

        if (duration > 0) {
            double throughput = (double) stepExecution.getWriteCount() / duration;
            meterRegistry.gauge("batch.step.throughput",
                    Tags.of("step", stepName), throughput);
        }

        return stepExecution.getExitStatus();
    }
}
```

### 4. 알림 설정

```java
/**
 * Best Practice: 배치 상태별 알림
 * - 시작/종료: 정보성 알림
 * - 실패: 즉시 알림 (Slack/PagerDuty)
 * - 임계값 초과: 경고 알림
 */
@Component
@RequiredArgsConstructor
public class BatchAlertListener implements JobExecutionListener {

    private final SlackNotifier slackNotifier;
    private final PagerDutyClient pagerDuty;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        slackNotifier.send(SlackMessage.info(
                String.format("🚀 배치 시작: %s (params: %s)",
                        jobExecution.getJobInstance().getJobName(),
                        jobExecution.getJobParameters())
        ));
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        BatchStatus status = jobExecution.getStatus();

        if (status == BatchStatus.COMPLETED) {
            // 성공 시 요약 정보 발송
            String summary = buildSuccessSummary(jobExecution);
            slackNotifier.send(SlackMessage.success(summary));

            // 스킵 건수가 임계값 초과 시 경고
            long totalSkips = jobExecution.getStepExecutions().stream()
                    .mapToLong(StepExecution::getSkipCount)
                    .sum();

            if (totalSkips > 100) {
                slackNotifier.send(SlackMessage.warning(
                        String.format("⚠️ %s: 스킵 건수 %d건 (확인 필요)",
                                jobName, totalSkips)));
            }

        } else if (status == BatchStatus.FAILED) {
            // 실패 시 즉시 알림
            String errorMessage = jobExecution.getAllFailureExceptions().stream()
                    .map(Throwable::getMessage)
                    .collect(Collectors.joining(", "));

            slackNotifier.send(SlackMessage.error(
                    String.format("🚨 배치 실패: %s - %s", jobName, errorMessage)));

            // 중요 배치는 PagerDuty 호출
            if (isCriticalJob(jobName)) {
                pagerDuty.triggerIncident(
                        "Batch Job Failed: " + jobName,
                        errorMessage,
                        Severity.HIGH
                );
            }
        }
    }

    private boolean isCriticalJob(String jobName) {
        return Set.of("billingJob", "settlementJob", "payoutJob")
                .contains(jobName);
    }
}
```

---

## 장애 대응 패턴

### 1. 부분 재처리

```java
/**
 * Best Practice: 실패 건만 재처리
 * - 전체 재실행 대신 실패 건만 선별 처리
 * - 처리 시간 및 비용 절감
 */
@Bean
public Job retryFailedPaymentsJob(JobRepository jobRepository) {
    return new JobBuilder("retryFailedPaymentsJob", jobRepository)
            .start(retryStep())
            .build();
}

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
                if (result.isSuccess()) {
                    successfulPayments.add(result);
                }
            }

            // DB 저장 시도
            repository.saveAll(toEntities(results.getItems()));

        } catch (Exception e) {
            log.error("DB 저장 실패, 보상 트랜잭션 시작", e);

            // 성공한 결제 취소 (보상)
            for (PaymentResult payment : successfulPayments) {
                try {
                    paymentGateway.refund(RefundRequest.builder()
                            .transactionId(payment.getTransactionId())
                            .reason("SYSTEM_ERROR_COMPENSATION")
                            .idempotencyKey("refund_" + payment.getIdempotencyKey())
                            .build());

                    log.info("보상 환불 완료 - txId: {}", payment.getTransactionId());

                } catch (Exception refundError) {
                    // 환불도 실패하면 수동 처리 큐로
                    log.error("보상 환불 실패, 수동 처리 필요 - txId: {}",
                            payment.getTransactionId(), refundError);

                    manualQueue.enqueue(ManualTask.builder()
                            .type(TaskType.REFUND_REQUIRED)
                            .transactionId(payment.getTransactionId())
                            .amount(payment.getAmount())
                            .reason("Compensation refund failed")
                            .build());
                }
            }

            throw e;  // 원래 예외 다시 던짐
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
@Bean
@StepScope
public JdbcPagingItemReader<Subscription> orderedReader() {
    return new JdbcPagingItemReaderBuilder<Subscription>()
            .name("orderedReader")
            .dataSource(dataSource)
            .selectClause("SELECT * FROM subscriptions")
            .whereClause("WHERE status = 'ACTIVE'")
            .sortKeys(Map.of("id", Order.ASCENDING))  // 항상 ID 순서로
            .pageSize(100)
            .build();
}

// JPA 낙관적 락
@Entity
public class Settlement {
    @Version
    private Long version;  // 낙관적 락

    // ...
}

// 재시도 설정
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
            .backOffPolicy(new FixedBackOffPolicy())  // 1초 대기 후 재시도
            .build();
}
```

---

## 운영 체크리스트

### 배포 전 체크리스트

```markdown
## 배포 전 확인사항

### 코드
- [ ] Chunk Size 적절한가? (테스트 환경에서 검증)
- [ ] 멱등성 보장되는가? (동일 입력 → 동일 결과)
- [ ] Skip/Retry 정책 적절한가?
- [ ] 타임아웃 설정되어 있는가? (외부 연동)
- [ ] Dry Run 모드 동작하는가?

### 데이터
- [ ] 인덱스 최적화되어 있는가? (실행 계획 확인)
- [ ] 배치 테이블 파티셔닝 필요한가?
- [ ] 메타데이터 테이블 정리 정책 있는가?

### 모니터링
- [ ] 핵심 메트릭 수집되는가?
- [ ] 알림 설정되어 있는가?
- [ ] 로그 레벨 적절한가?

### 장애 대응
- [ ] 재실행 절차 문서화되어 있는가?
- [ ] 롤백 절차 준비되어 있는가?
- [ ] 담당자 연락처 최신화되어 있는가?
```

### 실행 전 체크리스트

```markdown
## 실행 전 확인사항

### 데이터 검증
- [ ] 처리 대상 건수 예상치와 일치하는가?
- [ ] 이전 실행 결과 정상 종료되었는가?
- [ ] 중복 실행 아닌가?

### 시스템 상태
- [ ] DB 커넥션 풀 여유 있는가?
- [ ] 디스크 용량 충분한가?
- [ ] 외부 시스템 정상인가? (PG, 알림 등)

### 실행 환경
- [ ] 파라미터 올바른가?
- [ ] 실행 시간대 적절한가? (트래픽 낮은 시간)
- [ ] 동시 실행 배치와 충돌 없는가?
```

### 실행 후 체크리스트

```markdown
## 실행 후 확인사항

### 결과 검증
- [ ] 처리 건수 예상치와 일치하는가?
- [ ] 실패/스킵 건수 허용 범위인가?
- [ ] 비즈니스 데이터 정합성 확인 (금액 합계 등)

### 후속 조치
- [ ] 실패 건 원인 분석 완료
- [ ] 재처리 필요 건 식별
- [ ] 이해관계자 결과 공유
```

---

## 관련 문서

- [Spring Batch 기초](./spring-batch-basics.md) - Job, Step, 실행 흐름
- [Spring Batch Chunk 처리](./spring-batch-chunk.md) - ItemReader, ItemProcessor, ItemWriter
- [Spring Batch ExecutionContext 가이드](./spring-batch-execution-context.md) - 상태 저장과 재시작
- [Spring Batch 고급 활용](./spring-batch-advanced.md) - 비동기, 파티셔닝, 원격 실행

*마지막 업데이트: 2024년 12월*
