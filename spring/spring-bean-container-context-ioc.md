# Spring Bean Container, Context, IoC 핵심 개념

Spring Framework의 핵심인 IoC(Inversion of Control) 컨테이너와 Bean 관리 메커니즘을 이해한다.

## 목차

- [IoC (Inversion of Control)란?](#ioc-inversion-of-control란)
- [DI (Dependency Injection)](#di-dependency-injection)
- [Spring Container](#spring-container)
- [BeanFactory vs ApplicationContext](#beanfactory-vs-applicationcontext)
- [Bean 등록 방법](#bean-등록-방법)
- [Bean Scope](#bean-scope)
- [Bean 생명주기](#bean-생명주기)
- [의존성 주입 방식](#의존성-주입-방식)

---

## IoC (Inversion of Control)란?

IoC는 **제어의 역전**으로, 객체의 생성과 의존성 관리를 개발자가 아닌 프레임워크가 담당하는 설계 원칙이다.

### 기존 방식 (직접 제어)

```java
public class OrderService {
    // 개발자가 직접 객체를 생성하고 관리
    private OrderRepository orderRepository = new OrderRepository();
    private PaymentService paymentService = new PaymentService();

    public void createOrder(Order order) {
        orderRepository.save(order);
        paymentService.process(order);
    }
}
```

**문제점:**
- 강한 결합 (Tight Coupling)
- 테스트 어려움
- 유연성 부족

### IoC 적용 후

```java
@Service
public class OrderService {
    // Spring Container가 객체를 생성하고 주입
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;

    @Autowired
    public OrderService(OrderRepository orderRepository,
                       PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
    }

    public void createOrder(Order order) {
        orderRepository.save(order);
        paymentService.process(order);
    }
}
```

**장점:**
- 느슨한 결합 (Loose Coupling)
- 테스트 용이 (Mock 주입 가능)
- 유연한 구성 변경

---

## DI (Dependency Injection)

DI는 IoC를 구현하는 방법 중 하나로, **의존성을 외부에서 주입**받는 패턴이다.

### DI의 핵심 원리

```java
// 인터페이스 정의
public interface MessageService {
    void sendMessage(String message);
}

// 구현체 1
@Component("emailService")
public class EmailService implements MessageService {
    @Override
    public void sendMessage(String message) {
        System.out.println("Email: " + message);
    }
}

// 구현체 2
@Component("smsService")
public class SmsService implements MessageService {
    @Override
    public void sendMessage(String message) {
        System.out.println("SMS: " + message);
    }
}

// 사용하는 클래스
@Service
public class NotificationService {
    private final MessageService messageService;

    // 인터페이스 타입으로 주입 - 구현체 교체 용이
    @Autowired
    public NotificationService(@Qualifier("emailService") MessageService messageService) {
        this.messageService = messageService;
    }

    public void notify(String message) {
        messageService.sendMessage(message);
    }
}
```

---

## Spring Container

Spring Container는 Bean의 생성, 관리, 소멸을 담당하는 핵심 컴포넌트다.

### Container의 역할

```
┌─────────────────────────────────────────────────────┐
│                  Spring Container                    │
│  ┌─────────────────────────────────────────────┐    │
│  │              Bean Factory                    │    │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐     │    │
│  │  │ Bean A  │  │ Bean B  │  │ Bean C  │     │    │
│  │  └────┬────┘  └────┬────┘  └────┬────┘     │    │
│  │       │            │            │           │    │
│  │       └────────────┼────────────┘           │    │
│  │                    ▼                        │    │
│  │            의존성 주입 (DI)                  │    │
│  └─────────────────────────────────────────────┘    │
│                                                      │
│  ┌─────────────────────────────────────────────┐    │
│  │  Configuration Metadata                      │    │
│  │  (XML, Annotation, Java Config)              │    │
│  └─────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────┘
```

### Container 생성 코드

```java
// 1. Java Config 기반
@Configuration
public class AppConfig {

    @Bean
    public OrderRepository orderRepository() {
        return new JpaOrderRepository();
    }

    @Bean
    public OrderService orderService() {
        return new OrderService(orderRepository());
    }
}

// Container 생성
ApplicationContext context =
    new AnnotationConfigApplicationContext(AppConfig.class);

// Bean 조회
OrderService orderService = context.getBean(OrderService.class);
```

```java
// 2. Component Scan 기반
@Configuration
@ComponentScan(basePackages = "com.example")
public class AppConfig {
}

// 자동 스캔되는 컴포넌트
@Repository
public class JpaOrderRepository implements OrderRepository {
    // ...
}

@Service
public class OrderService {
    private final OrderRepository orderRepository;

    @Autowired
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
}
```

---

## BeanFactory vs ApplicationContext

### BeanFactory

가장 기본적인 컨테이너 인터페이스로, Bean의 생성과 DI만 담당한다.

```java
public interface BeanFactory {
    Object getBean(String name) throws BeansException;
    <T> T getBean(String name, Class<T> requiredType) throws BeansException;
    <T> T getBean(Class<T> requiredType) throws BeansException;
    boolean containsBean(String name);
    boolean isSingleton(String name) throws NoSuchBeanDefinitionException;
    boolean isPrototype(String name) throws NoSuchBeanDefinitionException;
    // ...
}
```

**특징:**
- Lazy Loading (Bean 요청 시 생성)
- 메모리 절약
- 최소 기능만 제공

### ApplicationContext

BeanFactory를 확장한 인터페이스로, 엔터프라이즈 기능을 제공한다.

```java
public interface ApplicationContext extends
        EnvironmentCapable,          // 환경 변수 처리
        ListableBeanFactory,         // Bean 목록 조회
        HierarchicalBeanFactory,     // 부모 컨텍스트 지원
        MessageSource,               // 국제화 (i18n)
        ApplicationEventPublisher,   // 이벤트 발행
        ResourcePatternResolver {    // 리소스 로딩
    // ...
}
```

### 주요 구현체

```java
// 1. AnnotationConfigApplicationContext - Java Config 기반
ApplicationContext context =
    new AnnotationConfigApplicationContext(AppConfig.class);

// 2. ClassPathXmlApplicationContext - XML 기반
ApplicationContext context =
    new ClassPathXmlApplicationContext("applicationContext.xml");

// 3. GenericWebApplicationContext - 웹 애플리케이션용
// (Spring Boot에서 자동 생성)
```

### 비교표

| 기능 | BeanFactory | ApplicationContext |
|------|-------------|-------------------|
| Bean 생성/관리 | O | O |
| 의존성 주입 | O | O |
| Bean 후처리기 | O | O |
| 국제화 (MessageSource) | X | O |
| 이벤트 발행 | X | O |
| AOP 통합 | X | O |
| 환경 추상화 | X | O |
| Bean 로딩 시점 | Lazy | Eager (기본) |

---

## Bean 등록 방법

### 1. @Component 계열 어노테이션

```java
@Component  // 일반 컴포넌트
public class MyComponent { }

@Service    // 서비스 레이어
public class MyService { }

@Repository // 데이터 접근 레이어
public class MyRepository { }

@Controller // 웹 컨트롤러
public class MyController { }

@RestController // REST API 컨트롤러
public class MyRestController { }
```

### 2. @Bean 어노테이션

```java
@Configuration
public class AppConfig {

    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:mysql://localhost:3306/mydb");
        dataSource.setUsername("root");
        dataSource.setPassword("password");
        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // 조건부 Bean 등록
    @Bean
    @ConditionalOnProperty(name = "cache.enabled", havingValue = "true")
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }
}
```

### 3. @Import 사용

```java
@Configuration
@Import({DatabaseConfig.class, SecurityConfig.class})
public class AppConfig {
}
```

### 4. FactoryBean 사용

```java
public class MyServiceFactoryBean implements FactoryBean<MyService> {

    @Override
    public MyService getObject() throws Exception {
        MyService service = new MyService();
        // 복잡한 초기화 로직
        service.init();
        return service;
    }

    @Override
    public Class<?> getObjectType() {
        return MyService.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
```

---

## Bean Scope

Bean의 생존 범위를 정의한다.

### Singleton (기본값)

```java
@Component
@Scope("singleton")  // 생략 가능
public class SingletonBean {
    // Container당 하나의 인스턴스
}
```

```java
// 테스트
@Test
void singletonTest() {
    ApplicationContext ctx = new AnnotationConfigApplicationContext(AppConfig.class);

    SingletonBean bean1 = ctx.getBean(SingletonBean.class);
    SingletonBean bean2 = ctx.getBean(SingletonBean.class);

    assertThat(bean1).isSameAs(bean2);  // 동일 인스턴스
}
```

### Prototype

```java
@Component
@Scope("prototype")
public class PrototypeBean {
    // 요청마다 새 인스턴스 생성
}
```

```java
// 테스트
@Test
void prototypeTest() {
    ApplicationContext ctx = new AnnotationConfigApplicationContext(AppConfig.class);

    PrototypeBean bean1 = ctx.getBean(PrototypeBean.class);
    PrototypeBean bean2 = ctx.getBean(PrototypeBean.class);

    assertThat(bean1).isNotSameAs(bean2);  // 다른 인스턴스
}
```

### Web Scopes

```java
@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestScopedBean {
    // HTTP 요청마다 새 인스턴스
}

@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class SessionScopedBean {
    // HTTP 세션마다 새 인스턴스
}
```

### Singleton 내부에서 Prototype 사용 시 주의

```java
@Component
public class SingletonBean {

    @Autowired
    private PrototypeBean prototypeBean;  // 항상 같은 인스턴스!

    // 해결책 1: ObjectProvider 사용
    @Autowired
    private ObjectProvider<PrototypeBean> prototypeBeanProvider;

    public void doSomething() {
        PrototypeBean bean = prototypeBeanProvider.getObject();  // 새 인스턴스
    }

    // 해결책 2: @Lookup 사용
    @Lookup
    public PrototypeBean getPrototypeBean() {
        return null;  // Spring이 오버라이드
    }
}
```

---

## Bean 생명주기

```
Container 시작
      │
      ▼
┌─────────────┐
│  Bean 인스턴스화  │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   의존성 주입    │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  초기화 콜백    │  ← @PostConstruct, InitializingBean, init-method
└──────┬──────┘
       │
       ▼
┌─────────────┐
│    Bean 사용    │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   소멸 콜백     │  ← @PreDestroy, DisposableBean, destroy-method
└──────┬──────┘
       │
       ▼
Container 종료
```

### 생명주기 콜백 구현

```java
@Component
public class MyBean implements InitializingBean, DisposableBean {

    private Connection connection;

    // 1. @PostConstruct (권장)
    @PostConstruct
    public void init() {
        System.out.println("1. @PostConstruct");
        this.connection = createConnection();
    }

    // 2. InitializingBean 인터페이스
    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("2. afterPropertiesSet");
    }

    // 3. DisposableBean 인터페이스
    @Override
    public void destroy() throws Exception {
        System.out.println("4. destroy");
    }

    // 4. @PreDestroy (권장)
    @PreDestroy
    public void cleanup() {
        System.out.println("3. @PreDestroy");
        closeConnection(connection);
    }
}
```

### @Bean 에서 지정

```java
@Configuration
public class AppConfig {

    @Bean(initMethod = "init", destroyMethod = "cleanup")
    public MyBean myBean() {
        return new MyBean();
    }
}
```

### BeanPostProcessor

모든 Bean의 초기화 전후에 커스텀 로직을 실행할 수 있다.

```java
@Component
public class CustomBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        System.out.println("Before Init: " + beanName);
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        System.out.println("After Init: " + beanName);

        // 프록시 생성 등 가능
        if (bean instanceof MyService) {
            return createProxy(bean);
        }
        return bean;
    }
}
```

---

## 의존성 주입 방식

### 1. 생성자 주입 (권장)

```java
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;

    // 생성자가 하나면 @Autowired 생략 가능
    public OrderService(OrderRepository orderRepository,
                       PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
    }
}
```

**장점:**
- 불변성 보장 (final 사용 가능)
- 필수 의존성 명확
- 테스트 용이
- 순환 참조 컴파일 시점에 발견

### 2. Setter 주입

```java
@Service
public class OrderService {

    private OrderRepository orderRepository;

    @Autowired
    public void setOrderRepository(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
}
```

**사용 케이스:** 선택적 의존성

### 3. 필드 주입 (비권장)

```java
@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;  // 테스트 어려움
}
```

### Lombok 활용

```java
@Service
@RequiredArgsConstructor  // final 필드에 대한 생성자 자동 생성
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;

    // 생성자 자동 생성됨
}
```

---

## 정리

| 개념 | 설명 |
|------|------|
| IoC | 객체 생성/관리 권한을 Container에 위임 |
| DI | 의존성을 외부에서 주입받는 패턴 |
| BeanFactory | 기본적인 Bean 관리 컨테이너 |
| ApplicationContext | BeanFactory + 엔터프라이즈 기능 |
| @Component | 자동 스캔 대상 Bean 등록 |
| @Bean | 수동 Bean 등록 (Java Config) |
| Singleton | 기본 Scope, 컨테이너당 하나 |
| Prototype | 요청마다 새 인스턴스 |

**핵심 원칙:**
1. 생성자 주입을 기본으로 사용
2. 인터페이스에 의존하여 유연성 확보
3. Bean Scope를 명확히 이해하고 적절히 사용

---

*마지막 업데이트: 2025년 12월*
