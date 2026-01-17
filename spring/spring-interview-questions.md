# Spring 면접 핵심 질문 정리

5년차 백엔드 개발자 면접에서 자주 등장하는 Spring 핵심 질문과 답변을 정리합니다.

## 목차

1. [IoC와 DI](#1-ioc와-di)
2. [Bean 스코프와 생명주기](#2-bean-스코프와-생명주기)
3. [순환 참조 (Circular Dependency)](#3-순환-참조-circular-dependency)
4. [AOP 동작 원리](#4-aop-동작-원리)
5. [Spring MVC 처리 흐름](#5-spring-mvc-처리-흐름)
6. [@Transactional 동작 원리](#6-transactional-동작-원리)

---

## 1. IoC와 DI

### Q: IoC(Inversion of Control)와 DI(Dependency Injection)의 차이점은?

**IoC (제어의 역전)**
- 프레임워크가 프로그램의 흐름을 제어
- 개발자가 아닌 프레임워크가 객체 생성, 생명주기 관리

```java
// 전통적인 방식: 개발자가 직접 객체 생성
public class OrderService {
    private OrderRepository repository = new OrderRepository();  // 직접 생성
}

// IoC: 프레임워크가 객체 관리
@Service
public class OrderService {
    private final OrderRepository repository;  // 프레임워크가 주입

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}
```

**DI (의존성 주입)**
- IoC를 구현하는 하나의 패턴
- 객체의 의존성을 외부에서 주입

**DI 방식 비교**

```java
// 1. 생성자 주입 (권장)
@Service
public class OrderService {
    private final OrderRepository repository;

    @Autowired  // 생성자 1개일 경우 생략 가능
    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}

// 2. Setter 주입
@Service
public class OrderService {
    private OrderRepository repository;

    @Autowired
    public void setRepository(OrderRepository repository) {
        this.repository = repository;
    }
}

// 3. 필드 주입 (비권장)
@Service
public class OrderService {
    @Autowired
    private OrderRepository repository;
}
```

**생성자 주입이 권장되는 이유**
| 특성 | 생성자 주입 | 필드 주입 |
|------|------------|----------|
| 불변성 | final 사용 가능 | 불가 |
| 테스트 용이성 | Mock 주입 쉬움 | 리플렉션 필요 |
| 순환 참조 감지 | 컴파일/시작 시 감지 | 런타임 시 감지 |
| 필수 의존성 명시 | 명확함 | 불명확 |

---

## 2. Bean 스코프와 생명주기

### Q: Spring Bean의 스코프 종류를 설명해주세요.

| 스코프 | 설명 | 사용 시점 |
|--------|------|----------|
| singleton | 컨테이너당 1개 (기본값) | 상태 없는 서비스 |
| prototype | 요청마다 새 인스턴스 | 상태 있는 빈 |
| request | HTTP 요청당 1개 | 요청 정보 보관 |
| session | HTTP 세션당 1개 | 사용자 세션 정보 |
| application | ServletContext당 1개 | 앱 전역 설정 |
| websocket | WebSocket당 1개 | WebSocket 연결 정보 |

```java
@Component
@Scope("prototype")
public class PrototypeBean {
    // 매번 새 인스턴스 생성
}

// Singleton에서 Prototype 주입 시 주의
@Service
public class SingletonService {
    @Autowired
    private PrototypeBean prototypeBean;  // 항상 같은 인스턴스!

    // 해결책 1: ObjectProvider
    @Autowired
    private ObjectProvider<PrototypeBean> prototypeBeanProvider;

    public void usePrototype() {
        PrototypeBean bean = prototypeBeanProvider.getObject();  // 매번 새 인스턴스
    }

    // 해결책 2: @Lookup
    @Lookup
    public PrototypeBean getPrototypeBean() {
        return null;  // Spring이 구현 오버라이드
    }
}
```

### Q: Bean의 생명주기를 설명해주세요.

```
스프링 컨테이너 생성
        ↓
    Bean 인스턴스화
        ↓
    의존성 주입 (DI)
        ↓
    초기화 콜백
    ├── @PostConstruct
    ├── InitializingBean.afterPropertiesSet()
    └── @Bean(initMethod = "init")
        ↓
    사용 가능 상태
        ↓
    소멸 전 콜백
    ├── @PreDestroy
    ├── DisposableBean.destroy()
    └── @Bean(destroyMethod = "cleanup")
        ↓
   스프링 컨테이너 종료
```

```java
@Component
public class LifeCycleBean {

    @PostConstruct
    public void init() {
        // 초기화 로직 (DB 연결 풀, 캐시 워밍업 등)
        System.out.println("Bean 초기화 완료");
    }

    @PreDestroy
    public void destroy() {
        // 정리 로직 (리소스 반환, 연결 종료 등)
        System.out.println("Bean 소멸 전");
    }
}
```

---

## 3. 순환 참조 (Circular Dependency)

### Q: 순환 참조가 무엇이고 어떻게 해결하나요?

**순환 참조란?**
```java
@Service
public class ServiceA {
    @Autowired
    private ServiceB serviceB;  // A → B
}

@Service
public class ServiceB {
    @Autowired
    private ServiceA serviceA;  // B → A (순환!)
}
```

**Spring Boot 2.6+ 기본 동작**
```
┌─────────────────────────────────────────────────────────┐
│ The dependencies of some of the beans in the application│
│ context form a cycle:                                   │
│                                                         │
│ ┌──────┐      ┌──────┐                                  │
│ │  A   │ ───→ │  B   │                                  │
│ └──────┘      └──────┘                                  │
│     ↑            │                                      │
│     └────────────┘                                      │
└─────────────────────────────────────────────────────────┘
```

**해결 방법**

```java
// 1. 설계 변경 (권장) - 공통 로직 분리
@Service
public class ServiceA {
    @Autowired
    private CommonService commonService;
}

@Service
public class ServiceB {
    @Autowired
    private CommonService commonService;
}

// 2. @Lazy 사용 (지연 초기화)
@Service
public class ServiceA {
    @Autowired
    @Lazy
    private ServiceB serviceB;  // 실제 사용 시점에 초기화
}

// 3. Setter 주입 사용 (비권장)
@Service
public class ServiceA {
    private ServiceB serviceB;

    @Autowired
    public void setServiceB(ServiceB serviceB) {
        this.serviceB = serviceB;
    }
}

// 4. ApplicationContext에서 직접 조회 (비권장)
@Service
public class ServiceA {
    @Autowired
    private ApplicationContext context;

    public void useServiceB() {
        ServiceB serviceB = context.getBean(ServiceB.class);
    }
}
```

**생성자 주입에서 순환 참조가 빨리 발견되는 이유**
```java
// 생성자 주입: 객체 생성 시점에 의존성 필요
new ServiceA(new ServiceB(new ServiceA(...)))  // 무한 루프 → 즉시 에러

// 필드/Setter 주입: 객체 생성 후 주입
// 1. new ServiceA() → 성공
// 2. new ServiceB() → 성공
// 3. serviceA.serviceB = serviceB → 성공
// 4. serviceB.serviceA = serviceA → 성공 (이미 생성된 객체 참조)
```

---

## 4. AOP 동작 원리

### Q: Spring AOP의 동작 방식을 설명해주세요.

**프록시 기반 AOP**
```
클라이언트 요청
      ↓
┌─────────────────────────────┐
│        Proxy 객체           │
│   ┌─────────────────────┐   │
│   │   Before Advice     │   │
│   ├─────────────────────┤   │
│   │    실제 메서드 호출   │ ──→ Target 객체
│   ├─────────────────────┤   │
│   │   After Advice      │   │
│   └─────────────────────┘   │
└─────────────────────────────┘
```

**JDK Dynamic Proxy vs CGLIB**

| 구분 | JDK Dynamic Proxy | CGLIB |
|------|-------------------|-------|
| 조건 | 인터페이스 필요 | 클래스 직접 상속 |
| 방식 | InvocationHandler | MethodInterceptor |
| 성능 | 상대적 느림 | 상대적 빠름 |
| Spring 기본 | X (과거) | O (Spring Boot) |

```java
// 인터페이스 있는 경우
public interface OrderService {
    void createOrder();
}

@Service
public class OrderServiceImpl implements OrderService {
    @Override
    public void createOrder() { }
}
// → JDK Proxy 또는 CGLIB (설정에 따라)

// 인터페이스 없는 경우
@Service
public class OrderService {
    public void createOrder() { }
}
// → CGLIB만 가능
```

**AOP 적용 예시**
```java
@Aspect
@Component
public class LoggingAspect {

    // Pointcut 정의
    @Pointcut("execution(* com.example.service.*.*(..))")
    public void serviceLayer() {}

    // Before Advice
    @Before("serviceLayer()")
    public void logBefore(JoinPoint joinPoint) {
        log.info("Method called: {}", joinPoint.getSignature().getName());
    }

    // Around Advice
    @Around("serviceLayer()")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();

        Object result = pjp.proceed();  // 실제 메서드 실행

        long duration = System.currentTimeMillis() - start;
        log.info("Execution time: {}ms", duration);

        return result;
    }

    // AfterReturning Advice
    @AfterReturning(pointcut = "serviceLayer()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        log.info("Method returned: {}", result);
    }

    // AfterThrowing Advice
    @AfterThrowing(pointcut = "serviceLayer()", throwing = "ex")
    public void logAfterThrowing(JoinPoint joinPoint, Exception ex) {
        log.error("Method threw exception: {}", ex.getMessage());
    }
}
```

---

## 5. Spring MVC 처리 흐름

### Q: Spring MVC 요청 처리 과정을 설명해주세요.

```
HTTP Request
     ↓
┌────────────────────────────────────────────────────────────────┐
│                    DispatcherServlet                           │
│    ┌──────────────────────────────────────────────────────┐    │
│    │  1. HandlerMapping으로 핸들러 조회                    │    │
│    │     - RequestMappingHandlerMapping                   │    │
│    │     - @Controller + @RequestMapping 매핑 정보 확인    │    │
│    └──────────────────────────────────────────────────────┘    │
│                           ↓                                    │
│    ┌──────────────────────────────────────────────────────┐    │
│    │  2. HandlerAdapter 조회                              │    │
│    │     - RequestMappingHandlerAdapter                   │    │
│    └──────────────────────────────────────────────────────┘    │
│                           ↓                                    │
│    ┌──────────────────────────────────────────────────────┐    │
│    │  3. Handler(Controller) 실행                         │    │
│    │     - ArgumentResolver로 파라미터 바인딩              │    │
│    │     - @RequestBody → HttpMessageConverter            │    │
│    └──────────────────────────────────────────────────────┘    │
│                           ↓                                    │
│    ┌──────────────────────────────────────────────────────┐    │
│    │  4. ReturnValueHandler로 응답 처리                   │    │
│    │     - @ResponseBody → HttpMessageConverter           │    │
│    │     - ViewResolver (View 반환 시)                    │    │
│    └──────────────────────────────────────────────────────┘    │
└────────────────────────────────────────────────────────────────┘
     ↓
HTTP Response
```

**주요 컴포넌트**
```java
// 1. Controller
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        // 비즈니스 로직
        return ResponseEntity.ok(user);
    }
}

// 2. ArgumentResolver 커스터마이징
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUser.class);
    }

    @Override
    public Object resolveArgument(...) {
        // 세션에서 사용자 정보 추출
        return sessionUser;
    }
}

// 3. Interceptor
@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, ...) {
        // 인증 체크
        return true;  // 진행, false면 중단
    }
}
```

---

## 6. @Transactional 동작 원리

### Q: @Transactional은 어떻게 동작하나요?

**프록시 기반 동작**
```
┌──────────────────────────────────────────────────────────┐
│                    Proxy 객체                            │
│  ┌────────────────────────────────────────────────────┐  │
│  │  1. TransactionInterceptor 호출                    │  │
│  │  2. PlatformTransactionManager.getTransaction()   │  │
│  │  3. Connection 획득, autoCommit = false           │  │
│  └────────────────────────────────────────────────────┘  │
│                          ↓                               │
│  ┌────────────────────────────────────────────────────┐  │
│  │  4. 실제 비즈니스 로직 실행                         │  │
│  └────────────────────────────────────────────────────┘  │
│                          ↓                               │
│  ┌────────────────────────────────────────────────────┐  │
│  │  5. 성공 시 commit() / 예외 시 rollback()          │  │
│  └────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

**전파 속성 (Propagation)**

| 속성 | 동작 |
|------|------|
| REQUIRED | 기존 트랜잭션 사용, 없으면 새로 생성 (기본값) |
| REQUIRES_NEW | 항상 새 트랜잭션 생성 (기존 일시 중단) |
| NESTED | 중첩 트랜잭션 (Savepoint 사용) |
| SUPPORTS | 기존 트랜잭션 사용, 없으면 없이 실행 |
| NOT_SUPPORTED | 트랜잭션 없이 실행 (기존 일시 중단) |
| MANDATORY | 기존 트랜잭션 필수, 없으면 예외 |
| NEVER | 트랜잭션 없이 실행, 있으면 예외 |

```java
@Service
public class OrderService {

    @Transactional
    public void createOrder() {
        // 주문 생성
        logService.saveLog();  // REQUIRES_NEW로 별도 커밋
    }
}

@Service
public class LogService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog() {
        // 새 트랜잭션에서 로그 저장
        // OrderService가 롤백되어도 로그는 유지
    }
}
```

**롤백 규칙**
```java
// 기본: RuntimeException, Error만 롤백
@Transactional
public void defaultRollback() {
    throw new RuntimeException();  // 롤백 O
    throw new IOException();       // 롤백 X (Checked Exception)
}

// Checked Exception도 롤백
@Transactional(rollbackFor = Exception.class)
public void rollbackForAll() {
    throw new IOException();       // 롤백 O
}

// 특정 예외 롤백 제외
@Transactional(noRollbackFor = BusinessException.class)
public void noRollbackForBusiness() {
    throw new BusinessException();  // 롤백 X
}
```

---

## 핵심 정리

| 주제 | 핵심 키워드 |
|------|-------------|
| IoC/DI | 제어의 역전, 의존성 주입, 생성자 주입 권장 |
| Bean | 싱글톤 기본, 생명주기 콜백, @PostConstruct |
| 순환 참조 | 설계 변경 권장, @Lazy, 생성자 주입으로 조기 발견 |
| AOP | 프록시 기반, CGLIB, @Aspect, Pointcut |
| MVC | DispatcherServlet, HandlerMapping, ArgumentResolver |
| Transaction | 프록시, 전파 속성, Checked Exception 롤백 주의 |

---

*마지막 업데이트: 2025년 01월*
