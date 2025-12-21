# Spring Boot 자동 설정 (Auto Configuration)

Spring Boot의 자동 설정은 클래스패스에 있는 라이브러리를 기반으로 애플리케이션을 자동으로 구성하는 기능이다.

## 목차

- [Auto Configuration이란?](#auto-configuration이란)
- [동작 원리](#동작-원리)
- [@EnableAutoConfiguration](#enableautoconfiguration)
- [@Conditional 어노테이션](#conditional-어노테이션)
- [자동 설정 확인하기](#자동-설정-확인하기)
- [자동 설정 비활성화](#자동-설정-비활성화)
- [커스텀 Auto Configuration 만들기](#커스텀-auto-configuration-만들기)

---

## Auto Configuration이란?

Spring Boot는 클래스패스에 존재하는 jar 의존성을 분석하여 필요한 빈(Bean)을 자동으로 등록한다.

```java
@SpringBootApplication  // @EnableAutoConfiguration 포함
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

`@SpringBootApplication`은 다음 세 가지 어노테이션을 포함한다:

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootConfiguration
@EnableAutoConfiguration  // 자동 설정 활성화
@ComponentScan
public @interface SpringBootApplication {
    // ...
}
```

---

## 동작 원리

### 1. spring.factories 파일

자동 설정 클래스는 `META-INF/spring.factories` 파일에 등록된다.

```properties
# spring-boot-autoconfigure.jar의 spring.factories
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration,\
org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,\
...
```

### 2. Spring Boot 3.x에서의 변경

Spring Boot 3.x부터는 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 파일을 사용한다.

```text
org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
```

### 3. 로딩 순서

```
@SpringBootApplication
        ↓
@EnableAutoConfiguration
        ↓
AutoConfigurationImportSelector
        ↓
spring.factories / AutoConfiguration.imports 로드
        ↓
@Conditional 조건 평가
        ↓
조건 만족 시 빈 등록
```

---

## @EnableAutoConfiguration

자동 설정을 활성화하는 핵심 어노테이션이다.

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@AutoConfigurationPackage
@Import(AutoConfigurationImportSelector.class)
public @interface EnableAutoConfiguration {

    String ENABLED_OVERRIDE_PROPERTY = "spring.boot.enableautoconfiguration";

    // 제외할 자동 설정 클래스
    Class<?>[] exclude() default {};

    // 제외할 자동 설정 클래스 이름
    String[] excludeName() default {};
}
```

### AutoConfigurationImportSelector

실제 자동 설정 클래스를 로드하는 역할을 한다.

```java
public class AutoConfigurationImportSelector implements DeferredImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        if (!isEnabled(annotationMetadata)) {
            return NO_IMPORTS;
        }

        // 자동 설정 후보 로드
        AutoConfigurationEntry autoConfigurationEntry =
            getAutoConfigurationEntry(annotationMetadata);

        return StringUtils.toStringArray(autoConfigurationEntry.getConfigurations());
    }

    protected AutoConfigurationEntry getAutoConfigurationEntry(
            AnnotationMetadata annotationMetadata) {

        // 1. 모든 자동 설정 후보 로드
        List<String> configurations = getCandidateConfigurations(
            annotationMetadata, attributes);

        // 2. 중복 제거
        configurations = removeDuplicates(configurations);

        // 3. 제외 설정 적용
        Set<String> exclusions = getExclusions(annotationMetadata, attributes);
        configurations.removeAll(exclusions);

        // 4. 필터링 (@Conditional 조건 평가)
        configurations = getConfigurationClassFilter()
            .filter(configurations);

        return new AutoConfigurationEntry(configurations, exclusions);
    }
}
```

---

## @Conditional 어노테이션

조건부로 빈을 등록하는 핵심 메커니즘이다.

### 주요 @Conditional 어노테이션

| 어노테이션 | 설명 |
|-----------|------|
| `@ConditionalOnClass` | 특정 클래스가 클래스패스에 있을 때 |
| `@ConditionalOnMissingClass` | 특정 클래스가 클래스패스에 없을 때 |
| `@ConditionalOnBean` | 특정 빈이 존재할 때 |
| `@ConditionalOnMissingBean` | 특정 빈이 없을 때 |
| `@ConditionalOnProperty` | 특정 프로퍼티 값이 일치할 때 |
| `@ConditionalOnResource` | 특정 리소스가 존재할 때 |
| `@ConditionalOnWebApplication` | 웹 애플리케이션일 때 |
| `@ConditionalOnNotWebApplication` | 웹 애플리케이션이 아닐 때 |

### @ConditionalOnClass 예제

```java
@Configuration
@ConditionalOnClass(DataSource.class)  // DataSource 클래스가 있을 때만 활성화
public class DataSourceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean  // 사용자가 직접 등록한 빈이 없을 때만
    public DataSource dataSource(DataSourceProperties properties) {
        return DataSourceBuilder.create()
            .url(properties.getUrl())
            .username(properties.getUsername())
            .password(properties.getPassword())
            .build();
    }
}
```

### @ConditionalOnProperty 예제

```java
@Configuration
@ConditionalOnProperty(
    name = "myapp.feature.enabled",
    havingValue = "true",
    matchIfMissing = false  // 프로퍼티가 없으면 비활성화
)
public class MyFeatureAutoConfiguration {

    @Bean
    public MyFeatureService myFeatureService() {
        return new MyFeatureService();
    }
}
```

```yaml
# application.yml
myapp:
  feature:
    enabled: true
```

### @ConditionalOnBean 예제

```java
@Configuration
public class CacheAutoConfiguration {

    @Bean
    @ConditionalOnBean(CacheManager.class)  // CacheManager 빈이 있을 때
    public CacheService cacheService(CacheManager cacheManager) {
        return new CacheService(cacheManager);
    }

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)  // CacheManager 빈이 없을 때
    public CacheService noOpCacheService() {
        return new NoOpCacheService();
    }
}
```

### 커스텀 Condition 구현

```java
// 커스텀 조건 클래스
public class OnProductionCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context,
                          AnnotatedTypeMetadata metadata) {
        String env = context.getEnvironment()
            .getProperty("spring.profiles.active");
        return "prod".equals(env);
    }
}

// 사용
@Configuration
@Conditional(OnProductionCondition.class)
public class ProductionOnlyConfiguration {

    @Bean
    public ProductionService productionService() {
        return new ProductionService();
    }
}
```

---

## 자동 설정 확인하기

### 1. 디버그 모드 활성화

```yaml
# application.yml
debug: true
```

실행 시 아래와 같은 조건 평가 리포트가 출력된다:

```
============================
CONDITIONS EVALUATION REPORT
============================

Positive matches:
-----------------
   DataSourceAutoConfiguration matched:
      - @ConditionalOnClass found required classes 'javax.sql.DataSource'

Negative matches:
-----------------
   ActiveMQAutoConfiguration:
      Did not match:
         - @ConditionalOnClass did not find required class 'javax.jms.ConnectionFactory'
```

### 2. Actuator 엔드포인트

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: conditions
```

```bash
curl http://localhost:8080/actuator/conditions
```

### 3. 코드에서 확인

```java
@Component
public class AutoConfigurationReporter implements ApplicationRunner {

    @Autowired
    private ConditionEvaluationReport report;

    @Override
    public void run(ApplicationArguments args) {
        report.getConditionAndOutcomesBySource().forEach((source, outcomes) -> {
            System.out.println("Source: " + source);
            outcomes.forEach(outcome -> {
                System.out.println("  - " + outcome.getCondition().getClass().getSimpleName()
                    + ": " + outcome.isMatch());
            });
        });
    }
}
```

---

## 자동 설정 비활성화

### 1. exclude 속성 사용

```java
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    JpaRepositoriesAutoConfiguration.class
})
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

### 2. application.yml 사용

```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
```

### 3. 프로퍼티로 전체 비활성화

```yaml
spring:
  boot:
    enableautoconfiguration: false
```

---

## 커스텀 Auto Configuration 만들기

### 1. 자동 설정 클래스 작성

```java
@AutoConfiguration  // Spring Boot 3.x
@ConditionalOnClass(MyService.class)
@EnableConfigurationProperties(MyServiceProperties.class)
public class MyServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MyService myService(MyServiceProperties properties) {
        return new MyService(properties.getEndpoint(), properties.getTimeout());
    }
}
```

### 2. Properties 클래스

```java
@ConfigurationProperties(prefix = "myservice")
public class MyServiceProperties {

    private String endpoint = "http://localhost:8080";
    private int timeout = 5000;

    // getters, setters
}
```

### 3. 등록 파일 작성

**Spring Boot 3.x:**

`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```text
com.example.autoconfigure.MyServiceAutoConfiguration
```

**Spring Boot 2.x:**

`src/main/resources/META-INF/spring.factories`:

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.example.autoconfigure.MyServiceAutoConfiguration
```

### 4. 자동 설정 순서 지정

```java
@AutoConfiguration(
    after = DataSourceAutoConfiguration.class,
    before = JpaRepositoriesAutoConfiguration.class
)
@ConditionalOnClass(MyService.class)
public class MyServiceAutoConfiguration {
    // ...
}
```

### 5. 사용 예시

```yaml
# application.yml
myservice:
  endpoint: https://api.example.com
  timeout: 10000
```

```java
@RestController
@RequiredArgsConstructor
public class MyController {

    private final MyService myService;  // 자동 주입

    @GetMapping("/data")
    public String getData() {
        return myService.fetchData();
    }
}
```

---

## 정리

| 개념 | 설명 |
|------|------|
| `@EnableAutoConfiguration` | 자동 설정 활성화 |
| `@Conditional*` | 조건부 빈 등록 |
| `spring.factories` | 자동 설정 클래스 등록 (2.x) |
| `AutoConfiguration.imports` | 자동 설정 클래스 등록 (3.x) |
| `@ConditionalOnMissingBean` | 사용자 정의 빈 우선 |

**자동 설정의 핵심 원칙:**
1. 클래스패스 기반 조건부 설정
2. 사용자 정의 빈이 항상 우선
3. 프로퍼티로 세부 동작 제어

---

*마지막 업데이트: 2025년 12월*
