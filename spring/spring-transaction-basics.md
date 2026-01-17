# Spring 트랜잭션 기초

## 목차
1. [트랜잭션 개념](#트랜잭션-개념)
2. [@Transactional 어노테이션](#transactional-어노테이션)
3. [전파 속성 (Propagation)](#전파-속성-propagation)
4. [격리 수준 (Isolation Level)](#격리-수준-isolation-level)
5. [롤백 규칙](#롤백-규칙)
6. [트랜잭션 주의사항](#트랜잭션-주의사항)
7. [핵심 정리](#핵심-정리)

---

## 트랜잭션 개념

### ACID 속성

```
┌──────────────────────────────────────────────────────────────────┐
│                      ACID 속성                                    │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  A - Atomicity (원자성)                                          │
│      └── 트랜잭션은 전체 성공 또는 전체 실패                      │
│      └── 부분 실행 없음                                          │
│                                                                   │
│  C - Consistency (일관성)                                        │
│      └── 트랜잭션 전후로 데이터 무결성 유지                       │
│      └── 제약조건, 비즈니스 규칙 준수                             │
│                                                                   │
│  I - Isolation (격리성)                                          │
│      └── 동시 트랜잭션이 서로 영향 미치지 않음                    │
│      └── 격리 수준으로 조절                                      │
│                                                                   │
│  D - Durability (영속성)                                         │
│      └── 커밋된 트랜잭션은 영구 보존                              │
│      └── 시스템 장애에도 유지                                    │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Spring 트랜잭션 추상화

```java
// Spring의 트랜잭션 추상화 계층
public interface PlatformTransactionManager {
    TransactionStatus getTransaction(TransactionDefinition definition);
    void commit(TransactionStatus status);
    void rollback(TransactionStatus status);
}

// 구현체
// - DataSourceTransactionManager: JDBC
// - JpaTransactionManager: JPA
// - HibernateTransactionManager: Hibernate
// - JtaTransactionManager: JTA (분산 트랜잭션)
```

---

## @Transactional 어노테이션

### 기본 사용법

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;

    @Transactional  // 메서드 레벨
    public Order createOrder(OrderRequest request) {
        // 1. 재고 차감
        inventoryService.decreaseStock(request.getItems());

        // 2. 주문 생성
        Order order = Order.create(request);
        orderRepository.save(order);

        // 3. 결제 처리
        paymentService.processPayment(order);

        // 모든 작업 성공 → 커밋
        // 예외 발생 → 롤백
        return order;
    }
}

// 클래스 레벨 적용
@Service
@Transactional  // 모든 public 메서드에 적용
public class UserService {

    @Transactional(readOnly = true)  // 읽기 전용 오버라이드
    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow();
    }

    public User createUser(UserRequest request) {
        // 클래스 레벨 @Transactional 적용
        return userRepository.save(User.create(request));
    }
}
```

### 주요 속성

```java
@Transactional(
    propagation = Propagation.REQUIRED,     // 전파 속성
    isolation = Isolation.READ_COMMITTED,   // 격리 수준
    timeout = 30,                           // 타임아웃 (초)
    readOnly = false,                       // 읽기 전용 여부
    rollbackFor = Exception.class,          // 롤백할 예외
    noRollbackFor = BusinessException.class // 롤백하지 않을 예외
)
public void transactionalMethod() {
    // ...
}
```

### readOnly 속성

```java
@Service
public class UserService {

    // readOnly = true 장점
    // 1. JPA: 더티 체킹 스킵 → 성능 향상
    // 2. JDBC: 드라이버 힌트로 최적화 가능
    // 3. 복제 DB 사용 시 읽기 전용 슬레이브로 라우팅
    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public UserDetailDto getUserDetail(Long id) {
        User user = userRepository.findById(id).orElseThrow();
        // 엔티티 수정해도 DB 반영 안 됨 (더티 체킹 안 함)
        user.setName("변경");  // ⚠️ DB에 반영되지 않음
        return UserDetailDto.from(user);
    }
}

// DataSource 라우팅 예시
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource routingDataSource() {
        ReplicationRoutingDataSource routingDataSource = new ReplicationRoutingDataSource();
        routingDataSource.setTargetDataSources(Map.of(
            "master", masterDataSource(),
            "slave", slaveDataSource()
        ));
        return routingDataSource;
    }
}

public class ReplicationRoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly() ?
            "slave" : "master";
    }
}
```

---

## 전파 속성 (Propagation)

### 전파 속성 종류

```
┌──────────────────────────────────────────────────────────────────┐
│                      전파 속성 (Propagation)                      │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  REQUIRED (기본값)                                               │
│  └── 기존 트랜잭션 있음 → 참여                                   │
│  └── 기존 트랜잭션 없음 → 새로 생성                              │
│                                                                   │
│  REQUIRES_NEW                                                    │
│  └── 항상 새 트랜잭션 생성                                       │
│  └── 기존 트랜잭션 있으면 일시 중단                              │
│                                                                   │
│  NESTED                                                          │
│  └── 기존 트랜잭션 있음 → 중첩 트랜잭션 (Savepoint)              │
│  └── 기존 트랜잭션 없음 → REQUIRED처럼 동작                      │
│                                                                   │
│  SUPPORTS                                                        │
│  └── 기존 트랜잭션 있음 → 참여                                   │
│  └── 기존 트랜잭션 없음 → 트랜잭션 없이 실행                     │
│                                                                   │
│  NOT_SUPPORTED                                                   │
│  └── 트랜잭션 없이 실행                                          │
│  └── 기존 트랜잭션 있으면 일시 중단                              │
│                                                                   │
│  MANDATORY                                                       │
│  └── 기존 트랜잭션 필수 (없으면 예외)                            │
│                                                                   │
│  NEVER                                                           │
│  └── 트랜잭션 없이 실행 (있으면 예외)                            │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### REQUIRED vs REQUIRES_NEW

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final AuditService auditService;

    @Transactional  // REQUIRED (기본)
    public Order createOrder(OrderRequest request) {
        Order order = orderRepository.save(Order.create(request));

        // REQUIRED: 같은 트랜잭션 참여
        // 주문 실패 시 감사 로그도 같이 롤백됨
        auditService.logOrderCreation(order);

        return order;
    }
}

@Service
public class AuditService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logOrderCreation(Order order) {
        // 별도 트랜잭션으로 실행
        // 메인 트랜잭션 실패해도 감사 로그는 유지됨
        auditRepository.save(new AuditLog("ORDER_CREATED", order.getId()));
    }
}
```

```
REQUIRED 동작:
┌─────────────────────────────────────────────┐
│            외부 트랜잭션                     │
│  ┌─────────────────────────────────────┐   │
│  │ createOrder()                        │   │
│  │         ↓                            │   │
│  │ logOrderCreation() ← 같은 트랜잭션   │   │
│  └─────────────────────────────────────┘   │
│  → 하나라도 실패하면 전체 롤백              │
└─────────────────────────────────────────────┘

REQUIRES_NEW 동작:
┌─────────────────────────────────────────────┐
│            외부 트랜잭션                     │
│  ┌──────────────────┐                       │
│  │ createOrder()    │ ← 일시 중단           │
│  └────────┬─────────┘                       │
└───────────┼─────────────────────────────────┘
            ↓
    ┌───────────────────────┐
    │   새 트랜잭션          │
    │ logOrderCreation()   │ ← 독립 트랜잭션
    └───────────────────────┘
            ↓
┌─────────────────────────────────────────────┐
│  외부 트랜잭션 재개                          │
│  → 각각 독립적으로 커밋/롤백                 │
└─────────────────────────────────────────────┘
```

### NESTED

```java
@Service
public class BulkOrderService {

    @Transactional
    public void processOrders(List<OrderRequest> requests) {
        for (OrderRequest request : requests) {
            try {
                processOrder(request);  // NESTED 트랜잭션
            } catch (Exception e) {
                // 개별 주문 실패해도 계속 진행
                log.error("주문 처리 실패: {}", request.getId());
            }
        }
        // 전체 커밋
    }

    @Transactional(propagation = Propagation.NESTED)
    public void processOrder(OrderRequest request) {
        // Savepoint 생성
        // 실패 시 이 Savepoint까지만 롤백
        orderRepository.save(Order.create(request));
    }
}

// 주의: NESTED는 JDBC Savepoint 필요
// JPA 단독으로는 지원 안 될 수 있음
```

---

## 격리 수준 (Isolation Level)

### 격리 수준과 문제 현상

```
┌──────────────────────────────────────────────────────────────────┐
│                  격리 수준과 동시성 문제                          │
├────────────────┬───────────┬────────────┬───────────────────────┤
│ 격리 수준       │ Dirty Read│Non-Repeat  │ Phantom Read         │
├────────────────┼───────────┼────────────┼───────────────────────┤
│ READ_UNCOMMITTED│    O      │     O      │      O               │
│ READ_COMMITTED │    X      │     O      │      O               │
│ REPEATABLE_READ│    X      │     X      │      O               │
│ SERIALIZABLE   │    X      │     X      │      X               │
└────────────────┴───────────┴────────────┴───────────────────────┘
```

### 문제 현상 설명

```
Dirty Read (더티 리드):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Tx1: UPDATE users SET balance = 1000 WHERE id = 1
                                          ↓
Tx2:              SELECT balance FROM users WHERE id = 1 → 1000 읽음
                                          ↓
Tx1: ROLLBACK                      ← 커밋 안 됨!
                                          ↓
Tx2:              1000을 사용 ← 잘못된 데이터!

Non-Repeatable Read (반복 불가능 읽기):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Tx1: SELECT balance FROM users WHERE id = 1 → 500
                                          ↓
Tx2:              UPDATE users SET balance = 1000 WHERE id = 1
                  COMMIT
                                          ↓
Tx1: SELECT balance FROM users WHERE id = 1 → 1000 (다른 값!)

Phantom Read (팬텀 리드):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Tx1: SELECT COUNT(*) FROM orders WHERE status = 'PENDING' → 5
                                          ↓
Tx2:              INSERT INTO orders (status) VALUES ('PENDING')
                  COMMIT
                                          ↓
Tx1: SELECT COUNT(*) FROM orders WHERE status = 'PENDING' → 6 (유령 행!)
```

### 격리 수준 설정

```java
// 트랜잭션별 설정
@Transactional(isolation = Isolation.READ_COMMITTED)
public void updateBalance(Long userId, BigDecimal amount) {
    // ...
}

// 재고 업데이트 - 높은 격리 수준 필요
@Transactional(isolation = Isolation.SERIALIZABLE)
public void decreaseStock(Long productId, int quantity) {
    Product product = productRepository.findByIdForUpdate(productId);
    product.decreaseStock(quantity);
}

// MySQL InnoDB 기본: REPEATABLE_READ
// PostgreSQL 기본: READ_COMMITTED
// Oracle 기본: READ_COMMITTED
```

### 낙관적 락 vs 비관적 락

```java
// 낙관적 락 (Optimistic Lock) - @Version 사용
@Entity
public class Product {
    @Id
    private Long id;

    @Version  // 버전 컬럼
    private Long version;

    private int stock;
}

// 동시 수정 시 OptimisticLockException 발생
// → 재시도 로직 필요

// 비관적 락 (Pessimistic Lock)
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)  // SELECT ... FOR UPDATE
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_READ)   // SELECT ... FOR SHARE
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForShare(@Param("id") Long id);
}
```

---

## 롤백 규칙

### 기본 롤백 규칙

```java
// Spring 기본 동작:
// - RuntimeException (Unchecked) → 롤백
// - Exception (Checked) → 커밋

@Transactional
public void defaultBehavior() {
    // RuntimeException 발생 → 롤백
    throw new RuntimeException("롤백됨");
}

@Transactional
public void checkedExceptionBehavior() throws IOException {
    // Checked Exception 발생 → 커밋됨!
    throw new IOException("커밋됨");  // ⚠️ 롤백 안 됨
}
```

### 롤백 규칙 커스터마이징

```java
// 모든 Exception에서 롤백
@Transactional(rollbackFor = Exception.class)
public void rollbackForAllExceptions() throws Exception {
    throw new IOException("이제 롤백됨");
}

// 특정 예외는 롤백하지 않음
@Transactional(noRollbackFor = BusinessException.class)
public void noRollbackForBusiness() {
    throw new BusinessException("롤백 안 됨");
}

// 여러 예외 지정
@Transactional(
    rollbackFor = {IOException.class, SQLException.class},
    noRollbackFor = {IgnorableException.class}
)
public void complexRollbackRules() {
    // ...
}
```

### 예외 처리와 트랜잭션

```java
@Service
public class OrderService {

    @Transactional
    public Order createOrder(OrderRequest request) {
        try {
            Order order = orderRepository.save(Order.create(request));
            paymentService.processPayment(order);
            return order;
        } catch (PaymentException e) {
            // ⚠️ 예외를 잡아서 처리하면 롤백 안 됨!
            log.error("결제 실패", e);
            return null;  // 트랜잭션은 커밋됨
        }
    }

    // 올바른 처리
    @Transactional
    public Order createOrderCorrect(OrderRequest request) {
        try {
            Order order = orderRepository.save(Order.create(request));
            paymentService.processPayment(order);
            return order;
        } catch (PaymentException e) {
            log.error("결제 실패", e);
            throw e;  // 다시 던져서 롤백 유도
            // 또는 TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
    }
}
```

---

## 트랜잭션 주의사항

### 1. 자기 호출 문제

```java
@Service
public class UserService {

    @Transactional
    public void createUsers(List<UserRequest> requests) {
        for (UserRequest request : requests) {
            createUser(request);  // ⚠️ 프록시 우회, @Transactional 무시
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createUser(UserRequest request) {
        // REQUIRES_NEW가 적용되지 않음!
        userRepository.save(User.create(request));
    }
}

// 해결책 1: 클래스 분리
@Service
public class UserService {
    private final UserCreator userCreator;

    @Transactional
    public void createUsers(List<UserRequest> requests) {
        for (UserRequest request : requests) {
            userCreator.createUser(request);  // 다른 빈 호출 → 프록시 통과
        }
    }
}

@Service
public class UserCreator {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createUser(UserRequest request) {
        userRepository.save(User.create(request));
    }
}

// 해결책 2: self-injection (권장하지 않음)
@Service
public class UserService {
    @Autowired
    private UserService self;  // 프록시 주입

    @Transactional
    public void createUsers(List<UserRequest> requests) {
        for (UserRequest request : requests) {
            self.createUser(request);  // 프록시 통과
        }
    }
}
```

### 2. public 메서드만 적용

```java
@Service
public class OrderService {

    @Transactional
    public void publicMethod() {
        // ✅ 트랜잭션 적용
    }

    @Transactional
    protected void protectedMethod() {
        // ⚠️ 트랜잭션 미적용 (CGLIB)
    }

    @Transactional
    private void privateMethod() {
        // ⚠️ 트랜잭션 미적용
    }
}
```

### 3. 긴 트랜잭션 문제

```java
// ❌ 긴 트랜잭션 - 피해야 함
@Transactional
public void processLargeData(List<Data> dataList) {
    for (Data data : dataList) {
        processData(data);      // DB 작업
        callExternalApi(data);  // ⚠️ 외부 API 호출은 트랜잭션 밖에서!
        sendEmail(data);        // ⚠️ 메일 발송도 트랜잭션 밖에서!
    }
}

// ✅ 개선된 버전
public void processLargeData(List<Data> dataList) {
    for (Data data : dataList) {
        processDataTransactional(data);  // DB 작업만 트랜잭션
        callExternalApi(data);           // 트랜잭션 밖
        sendEmail(data);                 // 트랜잭션 밖
    }
}

@Transactional
public void processDataTransactional(Data data) {
    // DB 작업만
}
```

### 4. @Transactional + @Async

```java
@Service
public class AsyncService {

    // ⚠️ @Async 메서드에서 @Transactional은 별도 스레드에서 새 트랜잭션 시작
    @Async
    @Transactional
    public CompletableFuture<Result> processAsync(Request request) {
        // 새 스레드, 새 트랜잭션
        // 호출자의 트랜잭션과 무관
        return CompletableFuture.completedFuture(result);
    }
}
```

---

## 핵심 정리

### 전파 속성 선택 가이드

| 상황 | 권장 전파 속성 |
|------|---------------|
| 일반적인 비즈니스 로직 | REQUIRED (기본) |
| 독립적인 로깅/감사 | REQUIRES_NEW |
| 읽기 전용 조회 | SUPPORTS |
| 배치에서 개별 항목 처리 | NESTED 또는 REQUIRES_NEW |

### 트랜잭션 체크리스트

```
□ public 메서드에 @Transactional 적용했는가?
□ 자기 호출이 아닌가? (같은 클래스 내부 호출)
□ 외부 API 호출이 트랜잭션 안에 있지 않은가?
□ 트랜잭션 범위가 너무 넓지 않은가?
□ 읽기 전용은 readOnly = true 설정했는가?
□ 적절한 격리 수준을 선택했는가?
□ 예외 처리 후 롤백이 필요하면 예외를 다시 던지는가?
```

### 면접 대비 핵심 질문

1. **Q: @Transactional의 동작 원리는?**
   - A: Spring AOP 기반 프록시로 동작. 메서드 호출 시 프록시가 트랜잭션을 시작하고, 정상 종료 시 커밋, RuntimeException 시 롤백

2. **Q: REQUIRED와 REQUIRES_NEW의 차이점은?**
   - A: REQUIRED는 기존 트랜잭션에 참여(없으면 생성), REQUIRES_NEW는 항상 새 트랜잭션 생성(기존 것 일시 중단). 독립적인 커밋/롤백 필요 시 REQUIRES_NEW

3. **Q: 같은 클래스 내 @Transactional 메서드 호출이 안 되는 이유는?**
   - A: 프록시 기반 AOP는 외부 호출만 인터셉트. this 호출은 실제 객체를 직접 호출해서 프록시 우회. 클래스 분리로 해결

4. **Q: Checked Exception에서 롤백되지 않는 이유와 해결책은?**
   - A: Spring 기본 정책은 RuntimeException만 롤백. `rollbackFor = Exception.class`로 모든 예외에서 롤백 설정 가능

---

*마지막 업데이트: 2025년 01월*
