# Spring AOP 기초

## 목차
1. [AOP 개념](#aop-개념)
2. [AOP 핵심 용어](#aop-핵심-용어)
3. [JDK Dynamic Proxy vs CGLIB](#jdk-dynamic-proxy-vs-cglib)
4. [Spring AOP 구현](#spring-aop-구현)
5. [실전 활용 패턴](#실전-활용-패턴)
6. [핵심 정리](#핵심-정리)

---

## AOP 개념

AOP(Aspect-Oriented Programming)는 횡단 관심사(Cross-Cutting Concerns)를 모듈화하는 프로그래밍 패러다임입니다.

### 횡단 관심사 문제

```
┌──────────────────────────────────────────────────────────────────┐
│                    횡단 관심사 (Without AOP)                      │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  OrderService           PaymentService        UserService        │
│  ┌─────────────┐       ┌─────────────┐       ┌─────────────┐    │
│  │ 로깅        │       │ 로깅        │       │ 로깅        │    │
│  │ 보안 체크    │       │ 보안 체크    │       │ 보안 체크    │    │
│  │ 트랜잭션     │       │ 트랜잭션     │       │ 트랜잭션     │    │
│  │ ─────────── │       │ ─────────── │       │ ─────────── │    │
│  │ 핵심 로직   │       │ 핵심 로직   │       │ 핵심 로직   │    │
│  │ ─────────── │       │ ─────────── │       │ ─────────── │    │
│  │ 로깅        │       │ 로깅        │       │ 로깅        │    │
│  └─────────────┘       └─────────────┘       └─────────────┘    │
│                                                                   │
│  → 코드 중복! 유지보수 어려움!                                    │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### AOP로 해결

```
┌──────────────────────────────────────────────────────────────────┐
│                      AOP 적용 후                                  │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│                        Aspect (횡단 관심사)                       │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  LoggingAspect │ SecurityAspect │ TransactionAspect       │  │
│  └────────────────────────────────────────────────────────────┘  │
│                              │                                    │
│                              ▼ 적용 (Weaving)                     │
│                                                                   │
│  OrderService           PaymentService        UserService        │
│  ┌─────────────┐       ┌─────────────┐       ┌─────────────┐    │
│  │ 핵심 로직   │       │ 핵심 로직   │       │ 핵심 로직   │    │
│  │  (주문 처리) │       │ (결제 처리) │       │ (사용자 관리)│    │
│  └─────────────┘       └─────────────┘       └─────────────┘    │
│                                                                   │
│  → 핵심 로직에만 집중! 횡단 관심사는 Aspect로 분리!              │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

---

## AOP 핵심 용어

### 주요 개념

| 용어 | 설명 | 예시 |
|------|------|------|
| **Aspect** | 횡단 관심사를 모듈화한 것 | 로깅, 트랜잭션, 보안 |
| **Join Point** | Aspect가 적용될 수 있는 지점 | 메서드 실행, 필드 접근 |
| **Pointcut** | Join Point를 선택하는 표현식 | `execution(* com.example.service.*.*(..))` |
| **Advice** | Join Point에서 실행할 동작 | @Before, @After, @Around |
| **Target** | Aspect가 적용되는 대상 객체 | OrderService 빈 |
| **Weaving** | Aspect를 Target에 적용하는 과정 | 컴파일/로드/런타임 위빙 |

### Advice 유형

```java
@Aspect
@Component
public class LoggingAspect {

    // 메서드 실행 전
    @Before("execution(* com.example.service.*.*(..))")
    public void beforeMethod(JoinPoint joinPoint) {
        log.info("Before: {}", joinPoint.getSignature().getName());
    }

    // 메서드 정상 종료 후 (예외 발생 시 실행 안 됨)
    @AfterReturning(
        pointcut = "execution(* com.example.service.*.*(..))",
        returning = "result"
    )
    public void afterReturning(JoinPoint joinPoint, Object result) {
        log.info("After Returning: {} = {}", joinPoint.getSignature().getName(), result);
    }

    // 메서드에서 예외 발생 시
    @AfterThrowing(
        pointcut = "execution(* com.example.service.*.*(..))",
        throwing = "exception"
    )
    public void afterThrowing(JoinPoint joinPoint, Exception exception) {
        log.error("Exception in {}: {}", joinPoint.getSignature().getName(), exception.getMessage());
    }

    // 메서드 종료 후 (항상 실행, finally와 유사)
    @After("execution(* com.example.service.*.*(..))")
    public void afterMethod(JoinPoint joinPoint) {
        log.info("After: {}", joinPoint.getSignature().getName());
    }

    // 메서드 실행 전후 제어 (가장 강력)
    @Around("execution(* com.example.service.*.*(..))")
    public Object aroundMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();  // 실제 메서드 실행
            return result;
        } finally {
            long end = System.currentTimeMillis();
            log.info("{} executed in {}ms", joinPoint.getSignature().getName(), end - start);
        }
    }
}
```

### Advice 실행 순서

```
@Around (시작)
    │
    ▼
@Before
    │
    ▼
Target Method 실행
    │
    ├── 정상 종료 ──► @AfterReturning
    │                      │
    └── 예외 발생 ──► @AfterThrowing
                           │
                           ▼
                        @After
                           │
                           ▼
                    @Around (종료)
```

---

## JDK Dynamic Proxy vs CGLIB

### 프록시 생성 방식 비교

```
┌──────────────────────────────────────────────────────────────────┐
│               JDK Dynamic Proxy vs CGLIB                          │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  JDK Dynamic Proxy                    CGLIB                       │
│  ┌────────────────────┐              ┌────────────────────┐      │
│  │    <<interface>>   │              │   Target Class     │      │
│  │     UserService    │              │   UserService      │      │
│  └─────────┬──────────┘              └─────────┬──────────┘      │
│            │ implements                         │ extends         │
│            ▼                                    ▼                 │
│  ┌────────────────────┐              ┌────────────────────┐      │
│  │   Proxy (동적 생성)  │              │ Proxy$$EnhancerBy  │      │
│  │   $Proxy0          │              │ SpringCGLIB$$0     │      │
│  └────────────────────┘              └────────────────────┘      │
│                                                                   │
│  조건: 인터페이스 필수                 조건: final 클래스 불가     │
│  방식: 인터페이스 구현                 방식: 상속                  │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### JDK Dynamic Proxy 동작 원리

```java
// 인터페이스 기반
public interface UserService {
    User findById(Long id);
}

@Service
public class UserServiceImpl implements UserService {
    @Override
    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow();
    }
}

// JDK Proxy 생성 (Spring 내부 동작)
public class JdkDynamicProxyExample {

    public static void main(String[] args) {
        UserService target = new UserServiceImpl();

        UserService proxy = (UserService) Proxy.newProxyInstance(
            target.getClass().getClassLoader(),
            new Class[]{UserService.class},
            (proxyObj, method, params) -> {
                // Before advice
                System.out.println("Before: " + method.getName());

                // 실제 메서드 호출
                Object result = method.invoke(target, params);

                // After advice
                System.out.println("After: " + method.getName());

                return result;
            }
        );

        proxy.findById(1L);  // 프록시를 통해 호출
    }
}
```

### CGLIB 동작 원리

```java
// 클래스 기반 (인터페이스 없음)
@Service
public class OrderService {
    public Order createOrder(OrderRequest request) {
        // 주문 생성 로직
        return new Order();
    }
}

// CGLIB Proxy 생성 (Spring 내부 동작)
public class CglibProxyExample {

    public static void main(String[] args) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(OrderService.class);
        enhancer.setCallback((MethodInterceptor) (obj, method, params, proxy) -> {
            // Before advice
            System.out.println("Before: " + method.getName());

            // 실제 메서드 호출 (상위 클래스)
            Object result = proxy.invokeSuper(obj, params);

            // After advice
            System.out.println("After: " + method.getName());

            return result;
        });

        OrderService proxy = (OrderService) enhancer.create();
        proxy.createOrder(new OrderRequest());
    }
}
```

### Spring Boot의 프록시 설정

```yaml
# application.yml
spring:
  aop:
    proxy-target-class: true  # CGLIB 강제 사용 (기본값: true in Spring Boot 2.x+)
    # false: 인터페이스 있으면 JDK Proxy, 없으면 CGLIB
```

```java
// 프로그래밍 방식 확인
@Service
public class ProxyChecker {

    @Autowired
    private UserService userService;

    public void checkProxy() {
        System.out.println(userService.getClass().getName());
        // JDK: com.sun.proxy.$Proxy123
        // CGLIB: com.example.UserService$$EnhancerBySpringCGLIB$$abc123
    }
}
```

### 비교 정리

| 특성 | JDK Dynamic Proxy | CGLIB |
|------|-------------------|-------|
| 조건 | 인터페이스 필수 | 인터페이스 불필요 |
| 방식 | 인터페이스 구현 | 상속 |
| 성능 | 약간 느림 | 더 빠름 |
| final 클래스 | 프록시 가능 | 불가능 |
| final 메서드 | 프록시 가능 | 불가능 (바이패스) |
| Spring Boot 기본 | 2.0 이전 기본 | 2.0 이후 기본 |

---

## Spring AOP 구현

### Pointcut 표현식

```java
@Aspect
@Component
public class PointcutExamples {

    // execution: 메서드 실행 매칭
    // 형식: execution(접근제어자? 반환타입 패키지.클래스.메서드(파라미터) 예외?)
    @Pointcut("execution(public * com.example.service.*.*(..))")
    public void allServiceMethods() {}

    @Pointcut("execution(* com.example..*Service.*(..))")  // 하위 패키지 포함
    public void allServiceClassMethods() {}

    @Pointcut("execution(* com.example.service.*.find*(..))")  // find로 시작하는 메서드
    public void findMethods() {}

    // within: 특정 타입 내의 모든 메서드
    @Pointcut("within(com.example.service.*)")  // service 패키지의 모든 클래스
    public void withinService() {}

    @Pointcut("within(com.example..*)")  // 하위 패키지 포함
    public void withinExample() {}

    // @annotation: 특정 어노테이션이 붙은 메서드
    @Pointcut("@annotation(com.example.annotation.Loggable)")
    public void loggableMethods() {}

    // @within: 특정 어노테이션이 붙은 클래스의 모든 메서드
    @Pointcut("@within(org.springframework.stereotype.Service)")
    public void serviceAnnotatedClasses() {}

    // bean: 특정 빈 이름 매칭
    @Pointcut("bean(orderService)")
    public void orderServiceBean() {}

    @Pointcut("bean(*Service)")  // *Service로 끝나는 빈
    public void allServiceBeans() {}

    // 조합
    @Pointcut("execution(* com.example.service.*.*(..)) && @annotation(Transactional)")
    public void transactionalServiceMethods() {}
}
```

### 커스텀 어노테이션 + AOP

```java
// 커스텀 어노테이션 정의
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExecutionTime {
    String value() default "";
}

// Aspect 구현
@Aspect
@Component
@Slf4j
public class ExecutionTimeAspect {

    @Around("@annotation(executionTime)")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint,
                                       ExecutionTime executionTime) throws Throwable {
        long start = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String description = executionTime.value().isEmpty() ?
            methodName : executionTime.value();

        try {
            return joinPoint.proceed();
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("[{}] 실행 시간: {}ms", description, duration);

            // 메트릭 수집 (Micrometer)
            meterRegistry.timer("method.execution.time",
                "method", methodName,
                "class", joinPoint.getTarget().getClass().getSimpleName())
                .record(duration, TimeUnit.MILLISECONDS);
        }
    }
}

// 사용
@Service
public class OrderService {

    @ExecutionTime("주문 생성")
    public Order createOrder(OrderRequest request) {
        // 주문 생성 로직
        return order;
    }
}
```

---

## 실전 활용 패턴

### 1. 로깅 Aspect

```java
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("execution(* com.example..controller.*.*(..))")
    public Object logControllerMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("▶ {}.{}() 호출 - 파라미터: {}", className, methodName, Arrays.toString(args));

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            log.info("◀ {}.{}() 완료 - 결과: {}, 소요시간: {}ms",
                     className, methodName, result, System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.error("✗ {}.{}() 예외 발생: {}", className, methodName, e.getMessage());
            throw e;
        }
    }
}
```

### 2. 권한 검증 Aspect

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    String[] value();
}

@Aspect
@Component
public class AuthorizationAspect {

    @Before("@annotation(requireRole)")
    public void checkRole(JoinPoint joinPoint, RequireRole requireRole) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }

        Set<String> userRoles = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());

        boolean hasRole = Arrays.stream(requireRole.value())
            .anyMatch(userRoles::contains);

        if (!hasRole) {
            throw new AccessDeniedException("권한이 없습니다. 필요 권한: "
                + Arrays.toString(requireRole.value()));
        }
    }
}

// 사용
@RestController
public class AdminController {

    @RequireRole({"ROLE_ADMIN", "ROLE_SUPER_ADMIN"})
    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.delete(id);
    }
}
```

### 3. 재시도 Aspect

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Retryable {
    int maxAttempts() default 3;
    long delay() default 1000;
    Class<? extends Exception>[] value() default {Exception.class};
}

@Aspect
@Component
@Slf4j
public class RetryAspect {

    @Around("@annotation(retryable)")
    public Object retry(ProceedingJoinPoint joinPoint, Retryable retryable) throws Throwable {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < retryable.maxAttempts()) {
            try {
                attempts++;
                return joinPoint.proceed();
            } catch (Exception e) {
                lastException = e;

                boolean shouldRetry = Arrays.stream(retryable.value())
                    .anyMatch(exClass -> exClass.isInstance(e));

                if (!shouldRetry || attempts >= retryable.maxAttempts()) {
                    throw e;
                }

                log.warn("시도 {}/{} 실패: {}. {}ms 후 재시도",
                         attempts, retryable.maxAttempts(), e.getMessage(), retryable.delay());

                Thread.sleep(retryable.delay());
            }
        }

        throw lastException;
    }
}

// 사용
@Service
public class ExternalApiService {

    @Retryable(maxAttempts = 3, delay = 2000, value = {IOException.class, TimeoutException.class})
    public ApiResponse callExternalApi(String endpoint) {
        return restTemplate.getForObject(endpoint, ApiResponse.class);
    }
}
```

---

## 핵심 정리

### AOP 프록시 선택 기준

| 상황 | 권장 프록시 |
|------|------------|
| 인터페이스가 있는 서비스 | JDK Dynamic Proxy 또는 CGLIB |
| 인터페이스가 없는 클래스 | CGLIB |
| final 클래스/메서드 | JDK Dynamic Proxy (인터페이스 필요) |
| 성능 중요 | CGLIB |
| Spring Boot 2.x+ | CGLIB (기본값) |

### AOP 주의사항

```java
// 1. 자기 호출 (Self-invocation) 문제
@Service
public class OrderService {

    @Transactional
    public void createOrder() {
        // ...
        this.updateInventory();  // ⚠️ 프록시가 아닌 실제 객체 호출 → AOP 미적용!
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateInventory() {
        // 이 트랜잭션 설정이 적용되지 않음!
    }
}

// 해결책 1: 클래스 분리
// 해결책 2: AopContext.currentProxy() 사용
@Service
public class OrderService {

    @Transactional
    public void createOrder() {
        ((OrderService) AopContext.currentProxy()).updateInventory();
    }
}
// 설정: @EnableAspectJAutoProxy(exposeProxy = true)

// 2. private 메서드에 AOP 미적용
// CGLIB도 상속 기반이므로 private 메서드 프록시 불가

// 3. Aspect 순서 제어
@Aspect
@Order(1)  // 낮은 숫자가 먼저 실행
@Component
public class FirstAspect {}

@Aspect
@Order(2)
@Component
public class SecondAspect {}
```

### 면접 대비 핵심 질문

1. **Q: AOP란 무엇이고 왜 사용하나요?**
   - A: 횡단 관심사를 모듈화하는 프로그래밍 패러다임. 로깅, 트랜잭션, 보안 같은 공통 기능을 핵심 비즈니스 로직과 분리하여 코드 중복 제거, 유지보수성 향상

2. **Q: JDK Dynamic Proxy와 CGLIB의 차이점은?**
   - A: JDK Proxy는 인터페이스 기반(구현), CGLIB은 클래스 기반(상속). CGLIB이 더 빠르고 인터페이스 없이 사용 가능하지만 final 클래스/메서드는 프록시 불가

3. **Q: @Transactional이 동일 클래스 내부 호출에서 동작하지 않는 이유는?**
   - A: AOP는 프록시 기반으로 동작하는데, 내부 호출은 `this`를 통해 실제 객체를 직접 호출하므로 프록시를 거치지 않음. 클래스 분리 또는 AopContext.currentProxy() 사용으로 해결

4. **Q: @Around와 @Before/@After의 차이점은?**
   - A: @Around는 메서드 실행 전후 모두 제어하고 proceed() 호출 여부 결정 가능. @Before/@After는 단방향. 반환값 변경, 예외 처리가 필요하면 @Around 사용

---

*마지막 업데이트: 2025년 01월*
