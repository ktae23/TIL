# Spring Context Event (ApplicationContext 라이프사이클 이벤트)

## 목차

- [개요](#개요)
- [Spring Context 라이프사이클 이벤트 종류](#spring-context-라이프사이클-이벤트-종류)
- [이벤트 발생 순서](#이벤트-발생-순서)
- [각 이벤트 상세](#각-이벤트-상세)
  - [ContextRefreshedEvent](#1-contextrefreshedevent)
  - [ContextStartedEvent](#2-contextstartedevent)
  - [ContextStoppedEvent](#3-contextstoppedevent)
  - [ContextClosedEvent](#4-contextclosedevent)
- [이벤트 리스닝 방법](#이벤트-리스닝-방법)
- [실무 활용 사례](#실무-활용-사례)
- [주의사항](#주의사항)

---

## 개요

Spring의 `ApplicationContext`는 라이프사이클의 특정 시점에 **내장 이벤트(Built-in Event)**를 발행한다. 이 이벤트들을 활용하면 애플리케이션 시작/종료 시점에 필요한 초기화/정리 작업을 선언적으로 처리할 수 있다.

모든 컨텍스트 이벤트는 `ApplicationContextEvent`를 상속하며, 이는 다시 `ApplicationEvent`를 상속한다.

```
ApplicationEvent
  └── ApplicationContextEvent  ← getApplicationContext() 제공
        ├── ContextRefreshedEvent
        ├── ContextStartedEvent
        ├── ContextStoppedEvent
        └── ContextClosedEvent
```

## Spring Context 라이프사이클 이벤트 종류

| 이벤트 | 발생 시점 | 대표 용도 |
|--------|----------|----------|
| `ContextRefreshedEvent` | Context 초기화 또는 refresh 완료 시 | 캐시 워밍업, 데이터 검증 |
| `ContextStartedEvent` | `ConfigurableApplicationContext.start()` 호출 시 | Lifecycle 빈 시작 알림 |
| `ContextStoppedEvent` | `ConfigurableApplicationContext.stop()` 호출 시 | Lifecycle 빈 중지 알림 |
| `ContextClosedEvent` | `ConfigurableApplicationContext.close()` 호출 시 | 리소스 정리, 커넥션 해제 |

> **Spring Boot 추가 이벤트**: `ApplicationStartingEvent` → `ApplicationEnvironmentPreparedEvent` → `ApplicationContextInitializedEvent` → `ApplicationPreparedEvent` → `ApplicationStartedEvent` → `ApplicationReadyEvent` 등이 별도로 존재한다.

## 이벤트 발생 순서

```
[애플리케이션 시작]
    │
    ▼
ContextRefreshedEvent        ← 모든 빈 초기화 완료, 가장 자주 사용
    │
    ▼
ContextStartedEvent          ← start() 명시 호출 시에만 발생
    │
    ▼
  (애플리케이션 실행 중...)
    │
    ▼
ContextStoppedEvent          ← stop() 명시 호출 시에만 발생
    │
    ▼
ContextClosedEvent           ← close() 또는 JVM 종료 시 (Shutdown Hook)
```

**핵심 포인트**: `ContextRefreshedEvent`와 `ContextClosedEvent`는 자동으로 발생하지만, `ContextStartedEvent`와 `ContextStoppedEvent`는 **명시적으로 `start()`/`stop()`을 호출해야만** 발생한다.

## 각 이벤트 상세

### 1. ContextRefreshedEvent

**발생 시점**: `ApplicationContext`가 초기화되거나 refresh될 때. 모든 빈이 로드되고, post-processor가 실행되고, 모든 싱글톤 빈이 인스턴스화된 후 발생한다.

```java
@Component
public class CacheWarmer implements ApplicationListener<ContextRefreshedEvent> {

    private final ProductRepository productRepository;
    private final CacheManager cacheManager;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 애플리케이션 시작 시 인기 상품 캐시 워밍업
        List<Product> topProducts = productRepository.findTop100ByOrderBySalesDesc();
        Cache cache = cacheManager.getCache("products");
        topProducts.forEach(p -> cache.put(p.getId(), p));

        log.info("캐시 워밍업 완료: {}건", topProducts.size());
    }
}
```

**특징**:
- `ConfigurableApplicationContext.refresh()` 호출마다 발생 (여러 번 발생 가능)
- 모든 빈이 준비된 상태이므로 DI된 빈을 안전하게 사용 가능
- Spring Boot에서는 보통 1번만 발생

### 2. ContextStartedEvent

**발생 시점**: `ConfigurableApplicationContext.start()`를 명시적으로 호출할 때.

```java
@Component
public class SchedulerStarter implements ApplicationListener<ContextStartedEvent> {

    private final TaskScheduler scheduler;

    @Override
    public void onApplicationEvent(ContextStartedEvent event) {
        // Lifecycle 빈들이 start 신호를 받은 후 스케줄러 시작
        log.info("컨텍스트 시작 → 배치 스케줄러 활성화");
    }
}
```

**특징**:
- `Lifecycle` 인터페이스를 구현한 빈들에게 `start()` 신호를 보낸 후 발생
- 일반적인 Spring Boot 애플리케이션에서는 **자동으로 발생하지 않음**
- `stop()` 후 재시작 시나리오에서 활용

### 3. ContextStoppedEvent

**발생 시점**: `ConfigurableApplicationContext.stop()`을 명시적으로 호출할 때.

```java
@Component
public class GracefulShutdownListener implements ApplicationListener<ContextStoppedEvent> {

    private final ExecutorService executorService;

    @Override
    public void onApplicationEvent(ContextStoppedEvent event) {
        // 진행 중인 작업 완료 대기
        executorService.shutdown();
        try {
            executorService.awaitTermination(30, TimeUnit.SECONDS);
            log.info("모든 작업 정상 완료");
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            log.warn("강제 종료됨");
        }
    }
}
```

**특징**:
- `Lifecycle` 빈들에게 `stop()` 신호를 보낸 후 발생
- `stop()` 후 `start()`로 재시작 가능 (컨텍스트가 파괴되지 않음)
- `close()`와 다르게 **빈이 소멸되지 않음**

### 4. ContextClosedEvent

**발생 시점**: `ConfigurableApplicationContext.close()` 호출 시 또는 JVM Shutdown Hook에 의해 발생.

```java
@Component
public class ResourceCleanup implements ApplicationListener<ContextClosedEvent> {

    private final DataSource dataSource;
    private final RedisConnectionFactory redisFactory;

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.info("애플리케이션 종료 → 리소스 정리 시작");

        // 외부 연결 정리
        if (dataSource instanceof HikariDataSource hikari) {
            hikari.close();
        }

        // 임시 파일 정리
        FileSystemUtils.deleteRecursively(Path.of("/tmp/app-cache"));

        log.info("리소스 정리 완료");
    }
}
```

**특징**:
- **가장 실무에서 많이 사용되는 종료 이벤트**
- 이 이벤트 이후 모든 싱글톤 빈이 소멸됨 (`@PreDestroy` 호출)
- Spring Boot의 `registerShutdownHook()`이 기본 활성화되어 있어 `kill -15` (SIGTERM)에도 정상 발생
- `kill -9` (SIGKILL)에는 발생하지 않음

## 이벤트 리스닝 방법

### 방법 1: `@EventListener` 어노테이션 (권장)

```java
@Component
public class ContextEventHandler {

    @EventListener
    public void onRefreshed(ContextRefreshedEvent event) {
        log.info("Context refreshed: {}", event.getApplicationContext().getId());
    }

    @EventListener
    public void onClosed(ContextClosedEvent event) {
        log.info("Context closed: {}", event.getApplicationContext().getId());
    }
}
```

### 방법 2: `ApplicationListener<T>` 인터페이스

```java
@Component
public class ShutdownListener implements ApplicationListener<ContextClosedEvent> {

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.info("종료 처리");
    }
}
```

### 방법 3: `@EventListener` + 조건/순서 지정

```java
@Component
public class OrderedEventHandler {

    @EventListener
    @Order(1)  // 낮을수록 먼저 실행
    public void firstHandler(ContextClosedEvent event) {
        log.info("1순위: 신규 요청 차단");
    }

    @EventListener
    @Order(2)
    public void secondHandler(ContextClosedEvent event) {
        log.info("2순위: 진행 중 작업 완료 대기");
    }

    @EventListener(condition = "#event.applicationContext.parent == null")
    public void rootContextOnly(ContextRefreshedEvent event) {
        // Root Context에서만 실행 (자식 Context 무시)
        log.info("Root Context 초기화 완료");
    }
}
```

## 실무 활용 사례

### 1. Graceful Shutdown (ContextClosedEvent)

```java
@Component
@RequiredArgsConstructor
public class GracefulShutdownHandler {

    private final RequestCounter requestCounter;

    @EventListener
    public void handleShutdown(ContextClosedEvent event) {
        // 1. 헬스체크 실패 처리 (로드밸런서에서 제외)
        HealthIndicator.markUnhealthy();

        // 2. 진행 중인 요청 완료 대기 (최대 30초)
        long deadline = System.currentTimeMillis() + 30_000;
        while (requestCounter.getActiveCount() > 0
               && System.currentTimeMillis() < deadline) {
            Thread.sleep(500);
        }

        log.info("Graceful shutdown 완료. 남은 요청: {}", requestCounter.getActiveCount());
    }
}
```

### 2. 초기 데이터 로딩 (ContextRefreshedEvent)

```java
@Component
@RequiredArgsConstructor
public class CodeDataLoader {

    private final CommonCodeRepository codeRepository;
    private final Map<String, String> codeCache = new ConcurrentHashMap<>();

    @EventListener
    public void loadCodes(ContextRefreshedEvent event) {
        List<CommonCode> codes = codeRepository.findAllActive();
        codes.forEach(c -> codeCache.put(c.getCode(), c.getValue()));
        log.info("공통코드 로딩 완료: {}건", codes.size());
    }

    public String getCodeValue(String code) {
        return codeCache.get(code);
    }
}
```

### 3. 외부 서비스 연결 상태 확인 (ContextRefreshedEvent)

```java
@Component
public class ExternalServiceHealthCheck {

    @EventListener
    public void verifyConnections(ContextRefreshedEvent event) {
        // 애플리케이션 시작 시 외부 서비스 연결 검증
        checkDatabaseConnection();
        checkRedisConnection();
        checkKafkaBrokerConnection();

        log.info("모든 외부 서비스 연결 정상");
    }
}
```

## 주의사항

| 주의 항목 | 설명 |
|----------|------|
| **ContextRefreshedEvent 중복 발생** | 계층형 Context(Root + Servlet)에서는 2번 발생할 수 있음. `event.getApplicationContext().getParent() == null` 조건으로 Root만 필터링 |
| **ContextClosedEvent에서 빈 접근** | 이벤트 핸들러 실행 시점에는 빈이 아직 살아 있으나, `@PreDestroy` 순서와 경합할 수 있으므로 주의 |
| **비동기 이벤트 리스너 주의** | `@Async @EventListener`로 ContextClosedEvent를 처리하면 스레드풀이 이미 종료되어 실행되지 않을 수 있음 |
| **start()/stop()은 잘 안 쓰임** | 대부분의 Spring Boot 앱에서는 `ContextStartedEvent`, `ContextStoppedEvent`를 직접 사용할 일이 거의 없음 |
| **SIGKILL 대응 불가** | `kill -9`로 종료하면 Shutdown Hook이 실행되지 않아 `ContextClosedEvent`도 발생하지 않음 |

---

*마지막 업데이트: 2025년 02월*
