# HikariCP 구현 원리와 Spring 통합

HikariCP는 "빠르고, 간단하고, 신뢰할 수 있는" JDBC 커넥션 풀 라이브러리다. Spring Boot 2.0부터 기본 커넥션 풀로 채택되었으며, 그 성능은 다른 풀 라이브러리 대비 압도적이다.

## 목차

1. [핵심 아키텍처](#핵심-아키텍처)
2. [ConcurrentBag - 고성능 커넥션 관리](#concurrentbag---고성능-커넥션-관리)
3. [커넥션 획득 플로우](#커넥션-획득-플로우)
4. [주요 설정 옵션](#주요-설정-옵션)
5. [Spring Boot 통합](#spring-boot-통합)
6. [모니터링과 JMX](#모니터링과-jmx)

---

## 핵심 아키텍처

HikariCP의 핵심 클래스 구조:

```
HikariDataSource (진입점)
    └── HikariPool (풀 관리)
            ├── ConcurrentBag<PoolEntry> (커넥션 저장소)
            ├── HouseKeeper (유지보수 스케줄러)
            └── PoolEntry (커넥션 래퍼)
                    └── ProxyConnection (프록시)
```

### HikariDataSource

`DataSource` 인터페이스 구현체로, 애플리케이션의 진입점이다.

```java
public class HikariDataSource extends HikariConfig implements DataSource, Closeable {
    private final HikariPool fastPathPool;  // 빠른 경로용 (생성자로 초기화 시)
    private volatile HikariPool pool;       // 지연 초기화용

    // 생성자로 HikariConfig를 전달하면 즉시 풀 시작 (권장)
    public HikariDataSource(HikariConfig configuration) {
        configuration.validate();
        configuration.copyStateTo(this);
        pool = fastPathPool = new HikariPool(this);  // 즉시 초기화
        this.seal();  // 설정 변경 불가
    }

    // 기본 생성자 사용 시 첫 getConnection()에서 풀 시작
    public Connection getConnection() throws SQLException {
        if (fastPathPool != null) {
            return fastPathPool.getConnection();  // 빠른 경로
        }

        // Double-checked locking으로 지연 초기화
        HikariPool result = pool;
        if (result == null) {
            synchronized (this) {
                result = pool;
                if (result == null) {
                    pool = result = new HikariPool(this);
                }
            }
        }
        return result.getConnection();
    }
}
```

**핵심 포인트**: `HikariConfig`를 생성자에 전달하면 `fastPathPool`이 설정되어 매 요청마다 volatile 읽기와 null 체크를 피할 수 있다.

---

## ConcurrentBag - 고성능 커넥션 관리

HikariCP 성능의 비밀은 `ConcurrentBag`에 있다. 기존의 `LinkedBlockingQueue`나 `LinkedTransferQueue` 대신 **ThreadLocal 기반의 lock-free 구조**를 사용한다.

### 상태 관리

```java
public interface IConcurrentBagEntry {
    int STATE_NOT_IN_USE = 0;   // 사용 가능
    int STATE_IN_USE = 1;       // 사용 중
    int STATE_REMOVED = -1;     // 제거됨
    int STATE_RESERVED = -2;    // 예약됨 (유지보수용)

    boolean compareAndSet(int expectState, int newState);
    void setState(int newState);
    int getState();
}
```

### borrow() - 커넥션 획득

```java
public T borrow(long timeout, final TimeUnit timeUnit) throws InterruptedException {
    // 1단계: ThreadLocal 리스트에서 먼저 찾기 (가장 빠름)
    final var list = threadLocalList.get();
    for (var i = list.size() - 1; i >= 0; i--) {
        final var entry = list.remove(i);
        final T bagEntry = (T) entry;
        if (bagEntry != null && bagEntry.compareAndSet(STATE_NOT_IN_USE, STATE_IN_USE)) {
            return bagEntry;  // CAS 성공 시 즉시 반환
        }
    }

    // 2단계: 공유 리스트 스캔
    final var waiting = waiters.incrementAndGet();
    try {
        for (T bagEntry : sharedList) {
            if (bagEntry.compareAndSet(STATE_NOT_IN_USE, STATE_IN_USE)) {
                if (waiting > 1) {
                    listener.addBagItem(waiting - 1);  // 다른 대기자를 위해 커넥션 추가 요청
                }
                return bagEntry;
            }
        }

        // 3단계: handoffQueue에서 대기 (SynchronousQueue)
        listener.addBagItem(waiting);
        timeout = timeUnit.toNanos(timeout);
        do {
            final var start = currentTime();
            final T bagEntry = handoffQueue.poll(timeout, NANOSECONDS);
            if (bagEntry == null || bagEntry.compareAndSet(STATE_NOT_IN_USE, STATE_IN_USE)) {
                return bagEntry;  // null이면 타임아웃
            }
            timeout -= elapsedNanos(start);
        } while (timeout > 10_000);

        return null;
    } finally {
        waiters.decrementAndGet();
    }
}
```

### requite() - 커넥션 반환

```java
public void requite(final T bagEntry) {
    bagEntry.setState(STATE_NOT_IN_USE);  // 상태만 변경 (컬렉션에서 제거 안 함!)

    // 대기 중인 스레드에게 직접 전달 시도
    for (int i = 1, waiting = waiters.get(); waiting > 0; i++, waiting = waiters.get()) {
        if (bagEntry.getState() != STATE_NOT_IN_USE || handoffQueue.offer(bagEntry)) {
            return;  // 누군가 이미 가져갔거나 전달 성공
        }
        // 스핀 대기 + 간헐적 park
        if ((i & 0xff) == 0xff) {
            parkNanos(MICROSECONDS.toNanos(10));
        } else {
            Thread.yield();
        }
    }

    // 대기자가 없으면 ThreadLocal에 캐싱 (최대 16개)
    if (threadLocalEntries.size() < 16) {
        threadLocalEntries.add(bagEntry);
    }
}
```

**핵심 인사이트**:
- 반환 시 컬렉션에서 제거하지 않고 상태만 변경
- ThreadLocal 캐싱으로 같은 스레드가 재사용 시 빠른 획득
- `SynchronousQueue`로 대기 스레드에 직접 핸드오프

---

## 커넥션 획득 플로우

```java
// HikariPool.getConnection()
public Connection getConnection(final long hardTimeout) throws SQLException {
    suspendResumeLock.acquire();  // 풀 일시정지 지원 시 락 획득
    final var startTime = currentTime();

    try {
        var timeout = hardTimeout;
        do {
            // ConcurrentBag에서 커넥션 획득
            var poolEntry = connectionBag.borrow(timeout, MILLISECONDS);
            if (poolEntry == null) {
                break;  // 타임아웃
            }

            final var now = currentTime();
            // 유효성 검증 (evicted 마킹됐거나 오래된 커넥션)
            if (poolEntry.isMarkedEvicted() ||
                (elapsedMillis(poolEntry.lastAccessed, now) > aliveBypassWindowMs
                    && isConnectionDead(poolEntry.connection))) {
                closeConnection(poolEntry, "evicted or dead");
                timeout = hardTimeout - elapsedMillis(startTime);
            } else {
                // 프록시 커넥션 생성하여 반환
                return poolEntry.createProxyConnection(leakTaskFactory.schedule(poolEntry));
            }
        } while (timeout > 0L);

        throw createTimeoutException(startTime);
    } finally {
        suspendResumeLock.release();
    }
}
```

### Alive Bypass Window

```java
private final long aliveBypassWindowMs = Long.getLong(
    "com.zaxxer.hikari.aliveBypassWindowMs",
    MILLISECONDS.toMillis(500)
);
```

최근 500ms 이내에 사용된 커넥션은 유효성 검사를 건너뛴다. 이미 정상 동작이 확인된 커넥션에 대한 불필요한 검증 오버헤드를 줄인다.

---

## 주요 설정 옵션

### 필수 설정

```yaml
spring:
  datasource:
    hikari:
      jdbc-url: jdbc:mysql://localhost:3306/mydb
      username: user
      password: password
      # 또는 driver-class-name + jdbc-url 조합
```

### 풀 크기 설정

```java
// HikariConfig 기본값
private static final int DEFAULT_POOL_SIZE = 10;

// 권장: CPU 코어 수 기반 공식
// connections = ((core_count * 2) + effective_spindle_count)
// SSD 사용 시 spindle = 1로 계산
```

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10      # 최대 커넥션 수 (기본값: 10)
      minimum-idle: 10           # 유휴 커넥션 수 (기본값: maximum-pool-size와 동일)
```

**권장**: `minimum-idle`을 `maximum-pool-size`와 동일하게 설정하여 고정 크기 풀로 운영

### 타임아웃 설정

```yaml
spring:
  datasource:
    hikari:
      connection-timeout: 30000   # 커넥션 획득 대기 시간 (기본값: 30초)
      validation-timeout: 5000    # 유효성 검사 타임아웃 (기본값: 5초)
      idle-timeout: 600000        # 유휴 커넥션 유지 시간 (기본값: 10분)
      max-lifetime: 1800000       # 커넥션 최대 수명 (기본값: 30분)
      keepalive-time: 120000      # keepalive 체크 주기 (기본값: 2분)
```

### 커넥션 수명 관리

```java
// 최대 수명에 분산 적용 (thundering herd 방지)
if (maxLifetime > 0) {
    // 최대 25% 분산 (variance factor = 4)
    final var variance = maxLifetime > 10_000L
        ? ThreadLocalRandom.current().nextLong(maxLifetime / 4)
        : 0L;
    final var lifetime = maxLifetime - variance;
    poolEntry.setFutureEol(
        houseKeepingExecutorService.schedule(
            new MaxLifetimeTask(poolEntry),
            lifetime,
            MILLISECONDS
        )
    );
}
```

### Leak Detection

```yaml
spring:
  datasource:
    hikari:
      leak-detection-threshold: 60000  # 60초 이상 반환 안 되면 경고
```

```java
// ProxyLeakTask 스케줄링
this.leakTaskFactory = new ProxyLeakTaskFactory(
    config.getLeakDetectionThreshold(),
    houseKeepingExecutorService
);

// 커넥션 반환 시 leak task 취소
return poolEntry.createProxyConnection(
    leakTaskFactory.schedule(poolEntry)  // 반환 시 cancel됨
);
```

---

## Spring Boot 통합

### 자동 구성

Spring Boot 2.0+에서는 `HikariCP`가 클래스패스에 있으면 자동으로 선택된다.

```java
// DataSourceConfiguration.Hikari (Spring Boot 내부)
@Configuration
@ConditionalOnClass(HikariDataSource.class)
@ConditionalOnMissingBean(DataSource.class)
@ConditionalOnProperty(name = "spring.datasource.type",
    havingValue = "com.zaxxer.hikari.HikariDataSource", matchIfMissing = true)
static class Hikari {
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    HikariDataSource dataSource(DataSourceProperties properties) {
        HikariDataSource dataSource = createDataSource(
            properties, HikariDataSource.class);
        if (StringUtils.hasText(properties.getName())) {
            dataSource.setPoolName(properties.getName());
        }
        return dataSource;
    }
}
```

### application.yml 전체 설정 예시

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=UTC
    username: myuser
    password: mypassword
    driver-class-name: com.mysql.cj.jdbc.Driver

    hikari:
      pool-name: MyHikariPool

      # 풀 크기
      maximum-pool-size: 20
      minimum-idle: 10

      # 타임아웃
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      validation-timeout: 5000
      keepalive-time: 120000

      # 커넥션 초기화
      connection-init-sql: SET NAMES utf8mb4
      connection-test-query: SELECT 1  # JDBC4 드라이버는 불필요

      # 동작 설정
      auto-commit: true
      read-only: false
      isolate-internal-queries: false

      # 모니터링
      register-mbeans: true
      leak-detection-threshold: 60000

      # MySQL 최적화 (data-source-properties)
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        useServerPrepStmts: true
```

### 프로그래밍 방식 설정

```java
@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariConfig hikariConfig() {
        return new HikariConfig();
    }

    @Bean
    public DataSource dataSource(HikariConfig hikariConfig) {
        return new HikariDataSource(hikariConfig);  // 권장: 생성자 주입
    }
}
```

### 다중 DataSource 설정

```java
@Configuration
public class MultiDataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.primary.hikari")
    public HikariDataSource primaryDataSource() {
        return new HikariDataSource();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.secondary.hikari")
    public HikariDataSource secondaryDataSource() {
        return new HikariDataSource();
    }
}
```

---

## 모니터링과 JMX

### JMX 활성화

```yaml
spring:
  datasource:
    hikari:
      register-mbeans: true
      pool-name: MyPool  # JMX에서 구분하기 위한 이름
```

### HikariPoolMXBean 인터페이스

```java
public interface HikariPoolMXBean {
    int getActiveConnections();     // 현재 사용 중인 커넥션 수
    int getIdleConnections();       // 유휴 커넥션 수
    int getTotalConnections();      // 전체 커넥션 수
    int getThreadsAwaitingConnection();  // 대기 중인 스레드 수

    void softEvictConnections();    // 모든 커넥션 소프트 제거
    void suspendPool();             // 풀 일시정지
    void resumePool();              // 풀 재개
}
```

### Micrometer 통합 (Spring Boot Actuator)

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics
  metrics:
    enable:
      hikaricp: true
```

```java
// 사용 가능한 메트릭
// hikaricp.connections - 총 커넥션 수
// hikaricp.connections.active - 활성 커넥션 수
// hikaricp.connections.idle - 유휴 커넥션 수
// hikaricp.connections.pending - 대기 중인 스레드 수
// hikaricp.connections.creation - 커넥션 생성 시간
// hikaricp.connections.acquire - 커넥션 획득 시간
// hikaricp.connections.usage - 커넥션 사용 시간
// hikaricp.connections.timeout - 타임아웃 횟수
```

### 런타임 풀 상태 조회

```java
@RestController
public class PoolStatusController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/pool/status")
    public Map<String, Object> getPoolStatus() {
        HikariDataSource hikariDs = (HikariDataSource) dataSource;
        HikariPoolMXBean poolMXBean = hikariDs.getHikariPoolMXBean();

        return Map.of(
            "activeConnections", poolMXBean.getActiveConnections(),
            "idleConnections", poolMXBean.getIdleConnections(),
            "totalConnections", poolMXBean.getTotalConnections(),
            "threadsAwaitingConnection", poolMXBean.getThreadsAwaitingConnection()
        );
    }
}
```

---

## HouseKeeper - 유지보수 작업

30초마다 실행되는 백그라운드 태스크:

```java
private final class HouseKeeper implements Runnable {
    @Override
    public void run() {
        // 1. MBean을 통해 변경된 설정값 갱신
        connectionTimeout = config.getConnectionTimeout();
        validationTimeout = config.getValidationTimeout();
        leakTaskFactory.updateLeakDetectionThreshold(config.getLeakDetectionThreshold());

        // 2. 시간 역행 감지 (NTP 동기화 등)
        if (plusMillis(now, 128) < plusMillis(previous, housekeepingPeriodMs)) {
            logger.warn("Retrograde clock change detected, soft-evicting connections");
            softEvictConnections();
            return;
        }

        // 3. 유휴 커넥션 정리 (idleTimeout 초과 시)
        if (idleTimeout > 0L && config.getMinimumIdle() < config.getMaximumPoolSize()) {
            for (PoolEntry entry : connectionBag.values(STATE_NOT_IN_USE)) {
                if (maxToRemove > 0 &&
                    elapsedMillis(entry.lastAccessed, now) > idleTimeout &&
                    connectionBag.reserve(entry)) {
                    closeConnection(entry, "(connection has passed idleTimeout)");
                    maxToRemove--;
                }
            }
        }

        // 4. 최소 커넥션 수 유지
        fillPool(true);
    }
}
```

---

## 성능 최적화 팁

### 1. 커넥션 풀 크기 공식

```
connections = ((core_count * 2) + effective_spindle_count)

예시 (4코어 CPU, SSD):
connections = (4 * 2) + 1 = 9
```

### 2. MySQL 드라이버 최적화

```yaml
spring:
  datasource:
    hikari:
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        useServerPrepStmts: true
        rewriteBatchedStatements: true
```

### 3. 빠른 시작을 위한 설정

```yaml
spring:
  datasource:
    hikari:
      initialization-fail-timeout: -1  # 시작 시 DB 연결 검증 스킵
```

### 4. 프로덕션 권장 설정

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 10            # 고정 크기 풀
      max-lifetime: 1800000       # 30분 (DB wait_timeout보다 작게)
      keepalive-time: 60000       # 1분 (방화벽 타임아웃 방지)
      leak-detection-threshold: 60000
      register-mbeans: true
```

---

## 참고 자료

- [HikariCP GitHub](https://github.com/brettwooldridge/HikariCP)
- [HikariCP Wiki - About Pool Sizing](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing)
- [MySQL Performance Tips](https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration)

*마지막 업데이트: 2025년 12월*
