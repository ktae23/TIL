# Spring Boot 3 자동 구성 추적 — 빈은 어디서 생기나

`application.yml` 에 적은 `resilience4j.circuitbreaker.instances.payment.failure-rate-threshold: 50` 한 줄이 어떤 클래스들을 거쳐 `CircuitBreakerRegistry` 빈이 되는지 끝까지 따라갑니다. 자동 구성을 "그냥 되는 것"에서 "근거를 대고 끌 수 있는 것"으로 바꾸는 게 목표입니다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

Resilience4j 의 Spring 지원은 **3개 모듈** 로 쪼개져 있습니다. 이걸 먼저 머리에 넣어야 나머지가 풀립니다.

| 모듈 | 하는 일 | Spring Boot 의존성 |
|---|---|---|
| `resilience4j-framework-common` | yml 로 바인딩될 POJO(`Common*ConfigurationProperties`)와 `*Config` 조립 로직 | **없음** (순수 Java) |
| `resilience4j-spring6` | `@Configuration` + `@Bean` + Aspect. 평범한 Spring 설정 | **없음** (Spring Framework 만) |
| `resilience4j-spring-boot3` | 위를 감싸 `@ConditionalOnMissingBean`/`@ConditionalOnClass` 를 붙이고 `.imports` 에 등록 | 있음 |

즉 **설정 조립 → 빈 정의 → 조건부 등록** 3층입니다. 뒤에 나오는 `*OnMissingBean` 2단 구조가 왜 존재하는지는 전부 이 분리에서 나옵니다.

---

## 2. 왜 알아야 하는가 (Why)

코드 리뷰에서 실제로 터지는 상황 셋입니다.

- **"왜 내 설정이 안 먹나요"** — 누군가 `@Bean CircuitBreakerRegistry` 를 직접 정의해 뒀고, 그 순간 라이브러리의 `@ConditionalOnMissingBean` 이 물러나면서 yml 의 `instances` 블록 전체가 **조용히 무시** 됩니다. 에러도 안 납니다.
- **"애너테이션을 붙였는데 아무 일도 안 일어나요"** — `AspectJOnClasspathCondition` 이 `ProceedingJoinPoint` 를 못 찾으면 모든 Aspect 빈이 등록되지 않습니다. 로그는 `debug` 한 줄뿐입니다.
- **"`Mono` 리턴 메서드에 서킷이 안 걸려요"** — `ReactorOnClasspathCondition` 은 Reactor **그리고** `resilience4j-reactor` 둘 다를 요구합니다. 후자가 없으면 `Mono` 가 블로킹 경로로 떨어져 **구독 전에 즉시 성공 처리** 됩니다. 메트릭만 보면 멀쩡해 보이는 가장 위험한 케이스입니다.

---

## 3. 내부 구현 분석 (How)

### 3.1 등록 지점: `.imports` 파일

실제 파일은 `resilience4j-spring-boot3/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 이며 **19줄** 입니다. 발췌:

```
io.github.resilience4j.springboot3.verifier.autoconfigure.SpringBoot3VerifierAutoConfiguration
io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration
io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerMetricsAutoConfiguration
io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakersHealthIndicatorAutoConfiguration
io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerStreamEventsAutoConfiguration
io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration
...  (retry/ratelimiter/bulkhead/timelimiter/micrometer/thread/scheduled 계열 13개 생략)
```

바로 눈에 들어와야 하는 디테일 둘.

**(1) `@AutoConfiguration` 을 쓰는 클래스는 3개뿐입니다** — `Resilience4jThreadAutoConfiguration`, `ThreadMetricsAutoConfiguration`, `SpringBoot3VerifierAutoConfiguration`. 나머지 16개는 평범한 `@Configuration` 입니다. 등록은 `.imports` 가 담당하므로 동작하지만, `@AutoConfiguration` 이 포함하는 `proxyBeanMethods = false` 를 못 받습니다. 순서 지정은 `@AutoConfigureAfter`/`@AutoConfigureBefore` 를 따로 붙여 해결합니다.

**(2) `NativeHintsConfiguration` 은 이 목록에 없습니다.** `spring-boot3` 에는 `aot.factories` 도 없습니다(리소스는 `.imports`, `spring.factories`, `additional-spring-configuration-metadata.json` 3개뿐). 즉 자동 적용되지 않습니다(3.6).

`spring.factories` 는 딱 한 줄, `org.springframework.boot.diagnostics.FailureAnalyzer=...SpringBootVerifierFailureAnalyzer` 등록만 남아 있습니다.

### 3.2 체인 1단: `CircuitBreakerAutoConfiguration`

```java
@Configuration
@ConditionalOnClass(CircuitBreaker.class)
@EnableConfigurationProperties(CircuitBreakerProperties.class)
@Import({CircuitBreakerConfigurationOnMissingBean.class, FallbackConfigurationOnMissingBean.class})
public class CircuitBreakerAutoConfiguration {

    @Configuration
    @ConditionalOnClass(Endpoint.class)
    static class CircuitBreakerEndpointAutoConfiguration {

        @Bean
        @ConditionalOnAvailableEndpoint
        public CircuitBreakerEndpoint circuitBreakerEndpoint(
            CircuitBreakerRegistry circuitBreakerRegistry) {
            return new CircuitBreakerEndpoint(circuitBreakerRegistry);
        }
        // circuitBreakerEventsEndpoint 생략
    }
}
```

- **2행** — `resilience4j-circuitbreaker` 가 없으면 이 클래스는 **파싱조차 되지 않고** 전부 사라집니다.
- **3행** — yml 바인딩 대상을 빈으로 올립니다. `CircuitBreakerProperties` 는 **본문이 빈 클래스** 로, `@ConfigurationProperties(prefix = "resilience4j.circuitbreaker")` 만 붙이고 `spring6` 의 `CircuitBreakerConfigurationProperties` 를 상속합니다.
- **4행** — 실질적인 빈 정의는 전부 위임합니다. 이 클래스가 직접 만드는 건 Actuator 엔드포인트 2개뿐입니다.
- **7~8행** 중첩 `static class` + `@ConditionalOnClass(Endpoint.class)` — Actuator 없는 앱에서 `NoClassDefFoundError` 를 피하는 표준 패턴. 바깥 클래스에 조건을 걸면 레지스트리까지 날아가므로 반드시 중첩 클래스로 격리합니다.

### 3.3 체인 2단: `*OnMissingBean` 2단 구조

```java
@Configuration
@Import({FallbackConfigurationOnMissingBean.class, SpelResolverConfigurationOnMissingBean.class})
public abstract class AbstractCircuitBreakerConfigurationOnMissingBean {

    protected final CircuitBreakerConfiguration circuitBreakerConfiguration;
    protected final CircuitBreakerConfigurationProperties circuitBreakerProperties;

    public AbstractCircuitBreakerConfigurationOnMissingBean(
        CircuitBreakerConfigurationProperties circuitBreakerProperties) {
        this.circuitBreakerProperties = circuitBreakerProperties;
        this.circuitBreakerConfiguration = new CircuitBreakerConfiguration(
            circuitBreakerProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerRegistry circuitBreakerRegistry(
        EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry,
        RegistryEventConsumer<CircuitBreaker> circuitBreakerRegistryEventConsumer,
        @Qualifier("compositeCircuitBreakerCustomizer")
        CompositeCustomizer<CircuitBreakerConfigCustomizer> compositeCircuitBreakerCustomizer) {
        return circuitBreakerConfiguration
            .circuitBreakerRegistry(eventConsumerRegistry, circuitBreakerRegistryEventConsumer,
                compositeCircuitBreakerCustomizer);
    }
```

- 11행 `new CircuitBreakerConfiguration(...)` — `spring6` 의 설정 클래스를 **빈으로 올리지 않고 직접 `new`** 합니다. 빈으로 올리면 그 안의 조건 없는 `@Bean` 메서드가 전부 활성화되어 `@ConditionalOnMissingBean` 을 우회하기 때문입니다. "로직은 재사용하되 빈 정의는 가져오지 않는다"를 손으로 구현한 것입니다.
- 16행 `@ConditionalOnMissingBean` 은 **spring-boot3 쪽에만** 있습니다. `spring6` 의 같은 메서드에는 조건이 없습니다. 이유는 단순합니다 — `@ConditionalOnMissingBean` 은 `spring-boot-autoconfigure` 의 애너테이션이고 `spring6` 은 Boot 에 의존하지 않기 때문입니다. **Boot 를 안 쓰는 순수 Spring 앱도 `@Import(CircuitBreakerConfiguration.class)` 로 쓸 수 있게** 하려는 설계입니다.

구체 클래스 `CircuitBreakerConfigurationOnMissingBean` 이 추가로 하는 일은 하나뿐입니다.

```java
@Bean
@ConditionalOnMissingBean(value = CircuitBreakerEvent.class,
                          parameterizedContainer = EventConsumerRegistry.class)
public EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry() {
    return circuitBreakerConfiguration.eventConsumerRegistry();
}
```

`parameterizedContainer` 는 "제네릭 타입 인자까지 보고 판정하라"는 지시입니다. `EventConsumerRegistry<RetryEvent>` 가 이미 있어도 `<CircuitBreakerEvent>` 는 별개로 만들어야 하므로 필요합니다.

예외가 하나 있습니다. `circuitBreakerRegistryEventConsumer()` 에는 `@ConditionalOnMissingBean` 이 **없고 `@Primary`** 가 붙습니다. 사용자가 `RegistryEventConsumer<CircuitBreaker>` 빈을 추가해도 라이브러리가 물러나지 않고 `Optional<List<...>>` 로 수집해 `CompositeRegistryEventConsumer` 로 합칩니다. **"교체"가 아니라 "합성"** 이 의도된 확장점입니다.

### 3.4 체인 3단: `spring6` 의 조립 로직

```java
@Bean
public CircuitBreakerRegistry circuitBreakerRegistry( /* 파라미터 생략 */ ) {
    CircuitBreakerRegistry circuitBreakerRegistry = createCircuitBreakerRegistry(
        circuitBreakerProperties, circuitBreakerRegistryEventConsumer,
        compositeCircuitBreakerCustomizer);
    registerEventConsumer(circuitBreakerRegistry, eventConsumerRegistry);
    initCircuitBreakerRegistry(circuitBreakerRegistry, compositeCircuitBreakerCustomizer);
    return circuitBreakerRegistry;
}
```

1. `createCircuitBreakerRegistry()` — `configs:` 맵을 돌며 named config 를 만들고 `CircuitBreakerRegistry.of(configs, consumer, tags)` 로 레지스트리를 생성. 이 시점에는 인스턴스가 없습니다.
2. `registerEventConsumer()` — `onEntryAdded`/`onEntryReplaced`/`onEntryRemoved` 훅을 걸어 **런타임에 생기는 인스턴스까지** 이벤트 버퍼가 자동으로 붙게 합니다. 버퍼는 `getEventConsumerBufferSize()` 가 없으면 **100** 개.
3. `initCircuitBreakerRegistry()` — `instances:` 를 돌며 인스턴스를 기동 시점에 미리(eager) 생성. 뒷부분이 자주 간과됩니다.

```java
circuitBreakerProperties.getInstances().forEach((name, properties) ->
    circuitBreakerRegistry.circuitBreaker(name,
        circuitBreakerProperties.createCircuitBreakerConfig(name, properties,
            compositeCircuitBreakerCustomizer)));

compositeCircuitBreakerCustomizer.instanceNames()
    .stream()
    .filter(name -> circuitBreakerRegistry.getConfiguration(name).isEmpty())
    .forEach(name -> circuitBreakerRegistry.circuitBreaker(name,
        circuitBreakerProperties.createCircuitBreakerConfig(name, null,
            compositeCircuitBreakerCustomizer)));
```

1~4행은 yml 의 `instances.<name>` → 인스턴스 생성. 6~11행은 **yml 에 전혀 없는데 `Customizer` 빈만 있는 이름도 인스턴스로 만들어 줍니다** — `CircuitBreakerConfigCustomizer.of("payment", ...)` 빈만 등록해도 `payment` 서킷이 생깁니다.

### 3.5 `CompositeCustomizer` 가 끼어드는 지점

`framework-common` 의 `CompositeCustomizer` 는 이름 → 커스터마이저 맵입니다. 생성자에서 **같은 이름이 둘이면 `IllegalStateException`** 을 던져 기동을 실패시킵니다(`"It is not possible to define more than one customizer per instance name "`). 모듈별로 설정을 쪼갠 큰 프로젝트에서 밟는 지뢰입니다.

적용 지점은 `CommonCircuitBreakerConfigurationProperties.buildConfig()` 의 **맨 마지막** 입니다.

```java
compositeCircuitBreakerCustomizer.getCustomizer(instanceName).ifPresent(
    circuitBreakerConfigCustomizer -> circuitBreakerConfigCustomizer.customize(builder));
return builder.build();
```

`builder.build()` 직전이므로 **커스터마이저는 yml 로 들어온 모든 값을 덮어쓸 최종 발언권** 을 갖습니다. 병합 규칙 전체는 [설정 계층과 Customizer](./04-config-hierarchy.md).

### 3.6 조건 클래스 · Verifier · NativeHints

| 조건 클래스 | 검사 대상 | 꺼지면 사라지는 것 |
|---|---|---|
| `AspectJOnClasspathCondition` | `org.aspectj.lang.ProceedingJoinPoint` | 모든 `*Aspect`, `FallbackDecorators`, `FallbackExecutor` |
| `ReactorOnClasspathCondition` | `reactor.core.publisher.Flux` **AND** `io.github.resilience4j.reactor.AbstractSubscriber` | `ReactorCircuitBreakerAspectExt`, `ReactorFallbackDecorator` |
| `RxJava2OnClasspathCondition` | `io.reactivex.Flowable` **AND** `io.github.resilience4j.AbstractSubscriber` | `RxJava2*AspectExt`, `RxJava2FallbackDecorator` |
| `RxJava3OnClasspathCondition` | `io.reactivex.rxjava3.core.Flowable` **AND** `io.github.resilience4j.rxjava3.AbstractSubscriber` | `RxJava3*AspectExt`, `RxJava3FallbackDecorator` |

구현은 전부 `AspectUtil.checkClassIfFound()` 의 `ClassLoader.loadClass()` 한 줄이고, 실패 시 **`logger.debug`** 만 남깁니다. 기본 로그 레벨에서는 흔적이 없습니다. 조건이 둘씩 묶인 이유는 리액티브 타입만 있고 Resilience4j 연동 모듈이 없으면 오퍼레이터를 못 만들기 때문입니다.

`SpringBoot3Verifier` 는 일부러 기동을 깨뜨립니다.

```java
public void verifyCompatibility() {
    var springBootMajorVersion = Integer.parseInt(SpringBootVersion.getVersion().split("\\.")[0]);
    if (springBootMajorVersion == 4) {
        throw new IncompatibleSpringBootVersionException(
            "Module 'io.github.resilience4j:resilience4j-spring-boot3' is only compatible with Spring Boot 3.x",
            "Update your project to use 'io.github.resilience4j:resilience4j-spring-boot4'");
    } else if (springBootMajorVersion != 3) {
        throw new IncompatibleSpringBootVersionException(
            "Module 'io.github.resilience4j:resilience4j-spring-boot3' is only compatible with Spring Boot 3.x",
            "Update your project to use compatible spring boot module");
    }
}
```

`SpringBoot3VerifierAutoConfiguration` 은 `@AutoConfiguration(before = {BulkheadAutoConfiguration, CircuitBreakerAutoConfiguration, RateLimiterAutoConfiguration, RetryAutoConfiguration, TimeLimiterAutoConfiguration})` 로 **가장 먼저** 돌고 `@Bean` 메서드 안에서 즉시 검증합니다. `SpringBootVerifierFailureAnalyzer` 가 `FailureAnalysis(message, action, cause)` 로 변환하므로, 콘솔에는 스택트레이스 대신 `APPLICATION FAILED TO START` 와 함께 위 메시지가 `Description`, 해결책이 `Action` 으로 찍힙니다. 런타임에 이상하게 동작시키는 대신 **기동 시점에 해결책까지 알려 주고 죽는** 쪽을 택한 설계입니다.

`NativeHintsConfiguration` 은 `RuntimeHintsRegistrar` 로서 Aspect 5개 + `FallbackExecutor` + `FallbackMethod` + `AnnotationExtractor` + `MethodBasedEvaluationContext` 에 `INVOKE_DECLARED_METHODS` 힌트를 등록합니다. 대상이 이것들인 이유는 **폴백 탐색과 SpEL 평가가 전부 리플렉션** 이기 때문입니다. 단 3.1 에서 본 대로 `spring-boot3` 에서는 자동 등록되지 않으니 **직접 `@Import(NativeHintsConfiguration.class)`** 해야 합니다(`spring-boot4` 는 `aot.factories` 보유).

### 3.7 전체 흐름도

```mermaid
flowchart TD
    A["application.yml<br/>resilience4j.circuitbreaker.instances.payment.*"] --> B["CircuitBreakerProperties<br/>@ConfigurationProperties"]
    B --> C["CircuitBreakerConfigurationProperties (spring6)<br/>+ aspect order"]
    C --> D["CommonCircuitBreakerConfigurationProperties<br/>(framework-common) configs/instances 맵"]

    E["CircuitBreakerAutoConfiguration<br/>@ConditionalOnClass(CircuitBreaker)"] -->|@Import| F["CircuitBreakerConfigurationOnMissingBean"]
    F -->|extends| G["AbstractCircuitBreakerConfigurationOnMissingBean<br/>@ConditionalOnMissingBean"]
    G -->|"new (빈 아님)"| H["CircuitBreakerConfiguration (spring6)"]

    I["List&lt;CircuitBreakerConfigCustomizer&gt;"] --> J["CompositeCustomizer<br/>이름 → 커스터마이저"]

    D --> K["createCircuitBreakerConfig(name, props, customizer)"]
    J --> K
    H --> K
    K --> L["buildConfig()<br/>yml 값 적용 → customize(builder) → build()"]
    L --> M["CircuitBreakerConfig"]
    M --> N["CircuitBreakerRegistry.of(configs, consumer, tags)"]
    N --> O["registerEventConsumer(): onEntryAdded 훅"]
    O --> P["initCircuitBreakerRegistry()<br/>instances + customizer 이름 eager 생성"]
    P --> Q["CircuitBreaker 'payment'"]

    R["AspectJOnClasspathCondition"] -.->|통과| S["CircuitBreakerAspect"]
    N --> S
```

### 3.8 5개 컴포넌트 자동 구성 표

| 컴포넌트 | AutoConfiguration | `@ConditionalOnClass` | Properties prefix | 위임(`spring6`) |
|---|---|---|---|---|
| CircuitBreaker | `CircuitBreakerAutoConfiguration` | `CircuitBreaker` | `resilience4j.circuitbreaker` | `CircuitBreakerConfiguration` |
| Retry | `RetryAutoConfiguration` | `Retry` | `resilience4j.retry` | `RetryConfiguration` |
| RateLimiter | `RateLimiterAutoConfiguration` | `RateLimiter` | `resilience4j.ratelimiter` | `RateLimiterConfiguration` |
| Bulkhead | `BulkheadAutoConfiguration` | `Bulkhead` | `resilience4j.bulkhead` + `resilience4j.thread-pool-bulkhead` | `BulkheadConfiguration` |
| TimeLimiter | `TimeLimiterAutoConfiguration` | `TimeLimiter` | `resilience4j.timelimiter` | `TimeLimiterConfiguration` |
| (부록) Timer | `TimerAutoConfiguration` | `Timer` | `resilience4j.micrometer.timer` | `TimerConfiguration` |

5개 모두 `@Import({<X>ConfigurationOnMissingBean.class, FallbackConfigurationOnMissingBean.class})` 형태입니다. `BulkheadAutoConfiguration` 만 Properties 2개를 `@EnableConfigurationProperties` 에 넣습니다(세마포어/스레드풀이 별도 prefix).

메트릭/헬스는 별도 클래스로 분리돼 조건이 다릅니다. 메트릭 계열은 `@ConditionalOnClass(MeterRegistry, …)` + `@ConditionalOnProperty("resilience4j.<x>.metrics.enabled", matchIfMissing = true)` + `@AutoConfigureAfter(MetricsAutoConfiguration)`, 헬스 계열은 `@AutoConfigureAfter(<X>AutoConfiguration)` + `@AutoConfigureBefore(HealthContributorAutoConfiguration)` 입니다. 따라서 메트릭 끄기는 `resilience4j.circuitbreaker.metrics.enabled: false` 한 줄로 됩니다([Actuator 와 헬스](./05-actuator-and-health.md), [Micrometer 메트릭](./06-micrometer-metrics.md)).

---

## 4. 실전 예제

### 4.1 자동 구성을 끄고 수동 구성하기

`spring6` 의 `CircuitBreakerConfiguration` 을 직접 `@Import` 하면 Boot 의 back-off 계층을 건너뛰고 동일한 빈들(`CircuitBreakerRegistry`, `CircuitBreakerAspect`, `EventConsumerRegistry`)을 얻습니다.

```java
package com.example.resilience;

import io.github.resilience4j.spring6.circuitbreaker.configure.CircuitBreakerConfiguration;
import io.github.resilience4j.spring6.circuitbreaker.configure.CircuitBreakerConfigurationProperties;
// Bean, Configuration, Import, java.time.Duration import 생략

@Configuration(proxyBeanMethods = false)
@Import(CircuitBreakerConfiguration.class)
public class ManualCircuitBreakerConfig {

    /** CircuitBreakerConfiguration 의 생성자 파라미터. yml 바인딩 대신 코드로 채운다. */
    @Bean
    public CircuitBreakerConfigurationProperties circuitBreakerConfigurationProperties() {
        var properties = new CircuitBreakerConfigurationProperties();
        properties.setCircuitBreakerAspectOrder(1000);   // 트랜잭션보다 확실히 바깥

        var payment = new CircuitBreakerConfigurationProperties.InstanceProperties();
        payment.setSlidingWindowSize(50);
        payment.setFailureRateThreshold(50f);            // 세터가 1 초과 100 이하를 검증한다
        payment.setWaitDurationInOpenState(Duration.ofSeconds(30));
        properties.getInstances().put("payment", payment);
        return properties;
    }
}
```

```yaml
spring:
  autoconfigure:
    exclude:
      - io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration
```

`InstanceProperties` 세터가 범위를 직접 검증하므로 코드로 채우면 오히려 더 빨리 터져서 안전합니다.

### 4.2 `CircuitBreakerRegistry` 빈을 직접 정의해 교체하기

동작은 하지만 **부작용이 큽니다.** 그 부작용을 정확히 아는 게 이 예제의 목적입니다.

```java
@Configuration(proxyBeanMethods = false)
public class ReplacedRegistryConfig {

    /**
     * 이 빈이 존재하면 AbstractCircuitBreakerConfigurationOnMissingBean
     * .circuitBreakerRegistry() 의 @ConditionalOnMissingBean 이 물러난다.
     * → yml 의 configs/instances 가 전부 무시된다. 경고 로그도 없다.
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(
        EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry,
        RegistryEventConsumer<CircuitBreaker> registryEventConsumer) {

        CircuitBreakerConfig payment = CircuitBreakerConfig.custom()
            .slidingWindowSize(50).minimumNumberOfCalls(20).failureRateThreshold(50f)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .ignoreException(t -> t instanceof IllegalArgumentException)  // yml 로는 불가
            .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(
            Map.of("default", CircuitBreakerConfig.ofDefaults(), "payment", payment),
            registryEventConsumer,
            Map.of("service", "order-api"));

        // 라이브러리가 해 주던 일을 직접 복원: 이벤트 버퍼 연결
        registry.getEventPublisher()
            .onEntryAdded(e -> e.getAddedEntry().getEventPublisher()
                .onEvent(eventConsumerRegistry.createEventConsumer(
                    e.getAddedEntry().getName(), 100)));

        registry.circuitBreaker("payment", payment);   // eager 생성
        return registry;
    }
}
```

주석의 "복원"이 핵심입니다. `registerEventConsumer()`/`initCircuitBreakerRegistry()` 가 하던 일이 사라지므로 이 코드가 없으면 **`/actuator/circuitbreakerevents` 가 빈 응답을 돌려줍니다.** 레지스트리 교체는 보통 과한 선택이고, 대부분은 `CircuitBreakerConfigCustomizer` 빈 하나로 끝납니다([설정 계층 문서](./04-config-hierarchy.md) 4.2).

### 4.3 `--debug` 로 `ConditionEvaluationReport` 읽기

```bash
java -jar app.jar --debug      # 또는 ./gradlew bootRun --args='--debug'
```

```
AUTO-CONFIGURATION REPORT
Positive matches:
   AbstractCircuitBreakerConfigurationOnMissingBean#circuitBreakerRegistry matched:
      - @ConditionalOnMissingBean (types: ...CircuitBreakerRegistry;
        SearchStrategy: all) did not find any beans (OnBeanCondition)
Negative matches:
   AbstractCircuitBreakerConfigurationOnMissingBean#reactorCircuitBreakerAspect:
      Did not match:
         - @Conditional(ReactorOnClasspathCondition) did not match
```

읽는 법:

- `#circuitBreakerRegistry ... did not find any beans` → 라이브러리가 레지스트리를 만들었다는 뜻. 반대로 `Negative matches` 에 `found beans of type ...` 으로 나오면 **누군가 빈을 선점했다**는 증거입니다(4.2 적용 시 바로 확인됨).
- `#reactorCircuitBreakerAspect` 가 `Negative` 면 `Mono`/`Flux` 가 리액티브 경로로 안 갑니다. `resilience4j-reactor` 의존성을 확인하세요.

운영에서는 Actuator 로 같은 정보를 꺼냅니다.

```bash
curl -s localhost:8080/actuator/conditions | jq '.contexts.application.negativeMatches
  | with_entries(select(.key | test("Resilience|CircuitBreaker|Retry")))'

# 등록된 Aspect 빈 — 비어 있으면 AspectJ 가 없다
curl -s localhost:8080/actuator/beans | jq -r '.contexts.application.beans
  | keys[] | select(test("Aspect|Registry|Customizer"))'
```

조건 클래스가 `debug` 로만 로그를 남기므로 `logging.level.io.github.resilience4j.spring6.utils: DEBUG` 를 켜면 `"Reactor related Aspect extensions are not activated because Resilience4j Reactor module is not on the classpath."` 가 그대로 보입니다.

---

## 5. 정리

| 질문 | 답 |
|---|---|
| 자동 구성 등록 위치 | `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (19개). `spring.factories` 는 `FailureAnalyzer` 1줄만 |
| 전부 `@AutoConfiguration` 인가 | 아니다. 3개만. 나머지 16개는 `@Configuration` → `proxyBeanMethods = false` 를 못 받음 |
| yml 값 경로 | `<X>Properties`(prefix 선언만) → `spring6 <X>ConfigurationProperties`(aspect order 추가) → `framework-common Common<X>ConfigurationProperties`(조립 로직) |
| `*OnMissingBean` 2단 이유 | `spring6` 은 Boot 에 의존하지 않아야 하므로 `@ConditionalOnMissingBean` 을 못 붙인다. Boot 전용 back-off 를 `spring-boot3` 가 래핑 |
| 내 `CircuitBreakerRegistry` 빈을 정의하면 | 라이브러리가 물러나고 **yml configs/instances 전부 무시**. 이벤트 버퍼 연결도 사라짐 |
| `RegistryEventConsumer` 빈 추가 시 | 교체가 아니라 **합성**(`@Primary` + `Optional<List<...>>` → `CompositeRegistryEventConsumer`) |
| Aspect 가 안 걸리는 1순위 | AspectJ 부재. `AspectJOnClasspathCondition` 이 `debug` 로그만 남기고 전부 끔 |
| `Mono` 가 리액티브로 안 돌 때 | `ReactorOnClasspathCondition` 은 Reactor **그리고** `resilience4j-reactor` 둘 다 요구 |
| Customizer | `build()` **직전** 적용 → yml 보다 우선. yml 에 없고 Customizer 만 있는 이름도 인스턴스가 **생성된다**. 같은 이름 2개면 `IllegalStateException` |
| Boot 4 + `spring-boot3` | 기동 실패 + "Update your project to use resilience4j-spring-boot4" 액션 메시지 |
| GraalVM native image | `NativeHintsConfiguration` 은 `spring-boot3` 에서 자동 등록되지 않음 → 직접 `@Import` |
| 진단 | `--debug` 의 `AUTO-CONFIGURATION REPORT`, 또는 `/actuator/conditions` + `/actuator/beans` |

---

## 관련 문서
- 선행: [아키텍처 개요](../main/01-architecture-overview.md)
- 선행: [Registry 와 Config](../main/02-registry-and-config.md)
- 후행: [애스펙트 동작과 프록시의 한계](./02-aop-aspects.md)
- 후행: [설정 계층과 Customizer](./04-config-hierarchy.md)

---
*Resilience4j v2.4.0 기준 · 소스 커밋 7e3ab525*
