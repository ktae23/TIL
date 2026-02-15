# Spring 트랜잭션 함정과 해결 방법

Spring @Transactional 사용 시 자주 발생하는 문제와 해결 방법을 정리합니다.

## 목차

1. [Self-Invocation 문제](#1-self-invocation-문제)
2. [Checked Exception 롤백](#2-checked-exception-롤백)
3. [전파 속성 오해](#3-전파-속성-오해)
4. [읽기 전용 트랜잭션](#4-읽기-전용-트랜잭션)
5. [프록시 관련 이슈](#5-프록시-관련-이슈)
6. [테스트에서의 트랜잭션](#6-테스트에서의-트랜잭션)

---

## 1. Self-Invocation 문제

### 문제 상황

```java
@Service
public class OrderService {

    public void createOrders(List<OrderRequest> requests) {
        for (OrderRequest request : requests) {
            createOrder(request);  // 내부 호출 → 트랜잭션 미적용!
        }
    }

    @Transactional
    public void createOrder(OrderRequest request) {
        // 이 메서드는 트랜잭션 없이 실행됨
        orderRepository.save(new Order(request));
    }
}
```

### 원인

```
Spring AOP 프록시 기반 동작:

클라이언트 → [Proxy] → [실제 객체]
                 ↓
            트랜잭션 시작
                 ↓
            실제 메서드 호출
                 ↓
            트랜잭션 커밋/롤백

내부 호출 시:
실제 객체 내부에서 this.method() 호출
→ Proxy를 거치지 않음
→ AOP 적용 안 됨
```

### 해결 방법 1: 클래스 분리 (권장)

```java
@Service
@RequiredArgsConstructor
public class OrderFacadeService {

    private final OrderService orderService;

    public void createOrders(List<OrderRequest> requests) {
        for (OrderRequest request : requests) {
            orderService.createOrder(request);  // 외부 호출 → 트랜잭션 적용
        }
    }
}

@Service
public class OrderService {

    @Transactional
    public void createOrder(OrderRequest request) {
        orderRepository.save(new Order(request));
    }
}
```

### 해결 방법 2: Self Injection

```java
@Service
public class OrderService {

    @Lazy
    @Autowired
    private OrderService self;  // 프록시 주입

    public void createOrders(List<OrderRequest> requests) {
        for (OrderRequest request : requests) {
            self.createOrder(request);  // 프록시를 통해 호출
        }
    }

    @Transactional
    public void createOrder(OrderRequest request) {
        orderRepository.save(new Order(request));
    }
}
```

### 해결 방법 3: ApplicationContext 사용

```java
@Service
public class OrderService implements ApplicationContextAware {

    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }

    public void createOrders(List<OrderRequest> requests) {
        OrderService proxy = context.getBean(OrderService.class);
        for (OrderRequest request : requests) {
            proxy.createOrder(request);
        }
    }

    @Transactional
    public void createOrder(OrderRequest request) {
        orderRepository.save(new Order(request));
    }
}
```

---

## 2. Checked Exception 롤백

### 문제 상황

```java
@Service
public class PaymentService {

    @Transactional
    public void processPayment(PaymentRequest request) throws PaymentException {
        orderRepository.save(order);

        try {
            paymentGateway.charge(request);
        } catch (PaymentGatewayException e) {
            throw new PaymentException("결제 실패", e);  // Checked Exception
        }
        // PaymentException 발생해도 롤백되지 않음!
    }
}
```

### 기본 롤백 규칙

```
RuntimeException, Error → 롤백
Checked Exception → 롤백 안 함 (커밋)

이유: Checked Exception은 복구 가능한 예외로 간주
```

### 해결 방법 1: rollbackFor 명시

```java
@Transactional(rollbackFor = Exception.class)  // 모든 예외에 롤백
public void processPayment(PaymentRequest request) throws PaymentException {
    // ...
}

// 특정 예외만
@Transactional(rollbackFor = {PaymentException.class, ValidationException.class})
public void processPayment(PaymentRequest request) throws PaymentException {
    // ...
}
```

### 해결 방법 2: RuntimeException으로 래핑

```java
@Transactional
public void processPayment(PaymentRequest request) {
    try {
        paymentGateway.charge(request);
    } catch (PaymentGatewayException e) {
        throw new PaymentFailedException("결제 실패", e);  // RuntimeException
    }
}

// 커스텀 RuntimeException
public class PaymentFailedException extends RuntimeException {
    public PaymentFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### 해결 방법 3: 글로벌 설정

```java
@Configuration
@EnableTransactionManagement
public class TransactionConfig {

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        JpaTransactionManager tm = new JpaTransactionManager(emf);
        tm.setRollbackOnCommitFailure(true);
        return tm;
    }
}
```

### 롤백 제외 설정

```java
// 특정 예외는 롤백하지 않음
@Transactional(
    rollbackFor = Exception.class,
    noRollbackFor = BusinessWarningException.class
)
public void process() {
    // BusinessWarningException 발생 시 롤백하지 않고 커밋
}
```

---

## 3. 전파 속성 오해

### REQUIRED (기본값)

```java
@Service
public class OrderService {

    @Transactional  // REQUIRED (기본값)
    public void createOrder() {
        orderRepository.save(order);
        stockService.decrease(productId, quantity);  // 같은 트랜잭션
    }
}

@Service
public class StockService {

    @Transactional  // REQUIRED
    public void decrease(Long productId, int quantity) {
        // OrderService와 같은 트랜잭션 사용
        // 여기서 예외 발생 시 OrderService도 롤백
    }
}
```

### REQUIRES_NEW 주의점

```java
@Service
public class OrderService {

    @Transactional
    public void createOrder() {
        orderRepository.save(order);

        try {
            logService.saveLog(order);  // REQUIRES_NEW
        } catch (Exception e) {
            // 로그 실패해도 주문은 성공시키려는 의도
        }
    }
}

@Service
public class LogService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog(Order order) {
        // 새 트랜잭션에서 실행
        // 실패해도 OrderService에 영향 없음
        logRepository.save(new OrderLog(order));
    }
}
```

```
주의: REQUIRES_NEW 호출 시
1. 기존 트랜잭션 일시 중단
2. 새 트랜잭션 시작
3. 새 트랜잭션 커밋/롤백
4. 기존 트랜잭션 재개

→ 같은 레코드 접근 시 데드락 가능!
```

### 데드락 발생 예시

```java
@Transactional
public void updateUser() {
    userRepository.findById(1L);  // User 1 락

    auditService.createAudit(1L);  // REQUIRES_NEW
    // → User 1에 다시 접근하면 데드락!
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void createAudit(Long userId) {
    User user = userRepository.findById(userId);  // 데드락!
    // ...
}
```

### NESTED vs REQUIRES_NEW

```java
// NESTED: 저장점(Savepoint) 사용
@Transactional(propagation = Propagation.NESTED)
public void nestedMethod() {
    // 부모 트랜잭션 내에서 저장점 생성
    // 실패 시 저장점으로 롤백 (부모는 계속 진행 가능)
    // 부모 롤백 시 함께 롤백
}

// REQUIRES_NEW: 완전히 독립된 트랜잭션
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void newMethod() {
    // 부모와 완전히 독립
    // 부모 롤백과 무관하게 커밋 가능
}
```

---

## 4. 읽기 전용 트랜잭션

### readOnly 효과

```java
@Transactional(readOnly = true)
public List<Order> getOrders() {
    return orderRepository.findAll();
}
```

```
readOnly = true 시:

1. Hibernate:
   - 더티 체킹 스킵 → 성능 향상
   - 플러시 모드를 NEVER로 설정

2. JPA:
   - 영속성 컨텍스트의 스냅샷 저장 안 함

3. JDBC:
   - 일부 DB에서 읽기 전용 힌트 전달

4. 읽기/쓰기 분리:
   - Slave(읽기 복제본)로 라우팅 가능
```

### 주의: readOnly에서 쓰기 시도

```java
@Transactional(readOnly = true)
public void updateOrder(Long id, String status) {
    Order order = orderRepository.findById(id).orElseThrow();
    order.setStatus(status);
    // 변경 감지 안 됨! 커밋해도 UPDATE 쿼리 실행 안 됨
}

// 명시적 save도 안 됨 (일부 JPA 구현체)
@Transactional(readOnly = true)
public void saveOrder(Order order) {
    orderRepository.save(order);  // 예외 또는 무시됨
}
```

### 서비스 레벨 readOnly 설정

```java
@Service
@Transactional(readOnly = true)  // 기본값: 읽기 전용
public class OrderQueryService {

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    public Order findById(Long id) {
        return orderRepository.findById(id).orElseThrow();
    }

    @Transactional  // 쓰기 필요한 메서드만 오버라이드
    public Order create(OrderRequest request) {
        return orderRepository.save(new Order(request));
    }
}
```

---

## 5. 프록시 관련 이슈

### private 메서드에 @Transactional

```java
@Service
public class OrderService {

    @Transactional
    private void saveOrder(Order order) {  // 동작 안 함!
        orderRepository.save(order);
    }
}
```

```
원인:
- Spring AOP는 프록시 기반
- private 메서드는 상속/오버라이드 불가
- 따라서 프록시가 가로챌 수 없음

해결:
- public 또는 protected로 변경
- 또는 AspectJ 위빙 사용 (복잡)
```

### final 클래스/메서드

```java
@Service
public final class OrderService {  // CGLIB 프록시 불가!

    @Transactional
    public final void save() {  // 오버라이드 불가!
        // ...
    }
}
```

```
해결:
- final 제거
- 또는 인터페이스 기반 프록시 사용
  spring.aop.proxy-target-class=false
```

### 인터페이스 프록시 vs CGLIB 프록시

```java
// 인터페이스가 있는 경우
public interface OrderService {
    void save();
}

@Service
public class OrderServiceImpl implements OrderService {
    @Override
    @Transactional
    public void save() { }
}

// JDK Dynamic Proxy 사용 가능
// 주입 시 인터페이스 타입 사용
@Autowired
private OrderService orderService;  // OK

@Autowired
private OrderServiceImpl orderService;  // 에러 가능!
```

```yaml
# CGLIB 강제 사용 (Spring Boot 기본값)
spring:
  aop:
    proxy-target-class: true
```

---

## 6. 테스트에서의 트랜잭션

### @Transactional 테스트의 롤백

```java
@SpringBootTest
@Transactional  // 테스트 후 자동 롤백
class OrderServiceTest {

    @Test
    void createOrder() {
        Order order = orderService.create(request);

        // 검증
        assertThat(order.getId()).isNotNull();

        // 테스트 종료 시 롤백 → DB에 데이터 안 남음
    }
}
```

### 롤백으로 인한 문제

```java
@Test
@Transactional
void testLazyLoading() {
    Order order = orderRepository.save(new Order());

    // 같은 트랜잭션 → 영속성 컨텍스트 공유
    // 실제 쿼리 없이 캐시에서 반환될 수 있음
    Order found = orderRepository.findById(order.getId()).orElseThrow();
}

// 해결: 영속성 컨텍스트 초기화
@Autowired
private EntityManager em;

@Test
@Transactional
void testLazyLoading() {
    Order order = orderRepository.save(new Order());

    em.flush();   // DB에 반영
    em.clear();   // 영속성 컨텍스트 초기화

    Order found = orderRepository.findById(order.getId()).orElseThrow();
    // 이제 실제 DB 쿼리 실행
}
```

### 비동기 호출 테스트

```java
@Service
public class OrderService {

    @Transactional
    public Order create(OrderRequest request) {
        Order order = orderRepository.save(new Order(request));
        eventPublisher.publishEvent(new OrderCreatedEvent(order));
        return order;
    }
}

@Test
@Transactional  // 문제 발생!
void testAsync() {
    orderService.create(request);

    // 비동기 이벤트 핸들러는 다른 스레드
    // → 테스트의 트랜잭션과 별개
    // → 아직 커밋 안 된 데이터 조회 시도 → 실패
}

// 해결: 트랜잭션 없이 테스트
@Test
// @Transactional 제거
void testAsync() {
    orderService.create(request);
    Thread.sleep(1000);  // 비동기 처리 대기
    // 또는 Awaitility 사용
}
```

### @Commit으로 롤백 방지

```java
@Test
@Transactional
@Commit  // 또는 @Rollback(false)
void shouldCommit() {
    // 테스트 데이터가 실제로 커밋됨
    // 주의: 다른 테스트에 영향 줄 수 있음
}
```

---

## 체크리스트

```
□ 내부 메서드 호출 시 프록시 거치는지 확인
□ Checked Exception에 rollbackFor 설정
□ REQUIRES_NEW 사용 시 데드락 가능성 검토
□ readOnly 트랜잭션에서 쓰기 시도하지 않는지
□ private/final 메서드에 @Transactional 사용하지 않는지
□ 테스트에서 영속성 컨텍스트 초기화 필요한지
□ 비동기 호출 시 트랜잭션 분리 고려
```

---

## 흔한 실수 요약

| 실수 | 증상 | 해결 |
|------|------|------|
| Self-Invocation | 트랜잭션 미적용 | 클래스 분리, Self Injection |
| Checked Exception | 롤백 안 됨 | rollbackFor 명시 |
| private 메서드 | 트랜잭션 미적용 | public으로 변경 |
| REQUIRES_NEW | 데드락 | 설계 재검토 |
| readOnly에서 쓰기 | 변경 미반영 | readOnly 제거 |
| 테스트 롤백 | 비동기 실패 | @Transactional 제거 |

---

*마지막 업데이트: 2026년 01월*
