# Testcontainers 활용 가이드

MySQL, Redis, Kafka 등 외부 의존성을 Docker 컨테이너로 테스트하는 방법을 정리합니다.

## 목차

1. [Testcontainers 개요](#1-testcontainers-개요)
2. [기본 설정](#2-기본-설정)
3. [데이터베이스 테스트](#3-데이터베이스-테스트)
4. [Redis 테스트](#4-redis-테스트)
5. [Kafka 테스트](#5-kafka-테스트)
6. [CI/CD 환경 설정](#6-cicd-환경-설정)

---

## 1. Testcontainers 개요

### 왜 Testcontainers인가?

```
기존 방식의 문제점:
┌─────────────────────────────────────────────────────────────┐
│  H2 In-Memory DB                                            │
│  - MySQL과 문법 차이 (JSON, 전문검색 등)                     │
│  - 실제 환경과 동작 차이                                     │
│                                                             │
│  로컬 Docker 직접 실행                                       │
│  - 수동 관리 필요                                            │
│  - 포트 충돌                                                 │
│  - 테스트 간 데이터 오염                                     │
│                                                             │
│  공유 테스트 DB                                              │
│  - 동시 실행 시 충돌                                         │
│  - 초기화 어려움                                             │
└─────────────────────────────────────────────────────────────┘

Testcontainers 해결:
┌─────────────────────────────────────────────────────────────┐
│  ✓ 실제 DB와 동일한 환경                                     │
│  ✓ 테스트마다 격리된 컨테이너                                │
│  ✓ 자동 시작/종료                                            │
│  ✓ CI/CD 환경 호환                                           │
└─────────────────────────────────────────────────────────────┘
```

### 동작 원리

```
테스트 시작
    ↓
Testcontainers가 Docker 컨테이너 시작
    ↓
랜덤 포트 할당 (충돌 방지)
    ↓
애플리케이션이 컨테이너에 연결
    ↓
테스트 실행
    ↓
테스트 종료 시 컨테이너 자동 삭제
```

---

## 2. 기본 설정

### 의존성 추가

```groovy
// build.gradle
dependencies {
    // Testcontainers BOM
    testImplementation platform('org.testcontainers:testcontainers-bom:1.19.3')

    // Core
    testImplementation 'org.testcontainers:testcontainers'
    testImplementation 'org.testcontainers:junit-jupiter'

    // 모듈별 (필요한 것만)
    testImplementation 'org.testcontainers:mysql'
    testImplementation 'org.testcontainers:postgresql'
    testImplementation 'org.testcontainers:kafka'
    testImplementation 'org.testcontainers:localstack'
}
```

### 기본 사용법

```java
@Testcontainers
class BasicContainerTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Test
    void testRedisConnection() {
        String host = redis.getHost();
        Integer port = redis.getMappedPort(6379);  // 실제 매핑된 포트

        // 연결 테스트
        Jedis jedis = new Jedis(host, port);
        jedis.set("key", "value");
        assertThat(jedis.get("key")).isEqualTo("value");
    }
}
```

### 컨테이너 라이프사이클

```java
// 1. 테스트 클래스당 하나 (공유)
@Container
static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

// 2. 테스트 메서드마다 새로 (격리)
@Container
MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

// 3. 수동 관리
@BeforeAll
static void setup() {
    mysql.start();
}

@AfterAll
static void teardown() {
    mysql.stop();
}
```

---

## 3. 데이터베이스 테스트

### MySQL 컨테이너

```java
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MySQLIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("schema.sql");  // 초기 스키마

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveUser() {
        User user = new User("kim", "kim@example.com");
        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
    }
}
```

### 재사용 가능한 컨테이너 (Singleton)

```java
// 테스트 간 컨테이너 재사용으로 속도 향상
public abstract class AbstractMySQLTest {

    static final MySQLContainer<?> MYSQL;

    static {
        MYSQL = new MySQLContainer<>("mysql:8.0")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);  // 재사용 활성화
        MYSQL.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
}

// 사용
class UserRepositoryTest extends AbstractMySQLTest {
    // 테스트 코드
}
```

### 재사용 설정 (~/.testcontainers.properties)

```properties
# 컨테이너 재사용 활성화
testcontainers.reuse.enable=true
```

### 트랜잭션 롤백

```java
@Testcontainers
@SpringBootTest
@Transactional  // 각 테스트 후 롤백
class TransactionalTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private UserService userService;

    @Test
    void test1() {
        userService.createUser("user1");
        // 테스트 종료 후 자동 롤백
    }

    @Test
    void test2() {
        // test1의 데이터가 없음 (롤백됨)
    }
}
```

---

## 4. Redis 테스트

### Redis 컨테이너

```java
@Testcontainers
@SpringBootTest
class RedisIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void shouldCacheValue() {
        redisTemplate.opsForValue().set("key", "value");

        String result = redisTemplate.opsForValue().get("key");
        assertThat(result).isEqualTo("value");
    }
}
```

### Redis Cluster 테스트

```java
@Testcontainers
class RedisClusterTest {

    // Redis Cluster는 복잡하므로 단일 노드로 테스트하거나
    // docker-compose 사용 권장

    @Container
    static DockerComposeContainer<?> compose =
            new DockerComposeContainer<>(new File("src/test/resources/redis-cluster.yml"))
                    .withExposedService("redis-node-1", 6379)
                    .withExposedService("redis-node-2", 6380)
                    .withExposedService("redis-node-3", 6381);
}
```

---

## 5. Kafka 테스트

### Kafka 컨테이너

```java
@Testcontainers
@SpringBootTest
class KafkaIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private KafkaConsumer consumer;  // 테스트용 Consumer

    @Test
    void shouldSendAndReceiveMessage() throws Exception {
        String topic = "test-topic";
        String message = "Hello Kafka";

        // 메시지 발행
        kafkaTemplate.send(topic, message).get();

        // Consumer에서 수신 확인
        await().atMost(10, TimeUnit.SECONDS)
               .untilAsserted(() ->
                   assertThat(consumer.getMessages()).contains(message)
               );
    }
}
```

### Embedded Kafka vs Testcontainers

```
Embedded Kafka (spring-kafka-test):
- 더 빠른 시작
- 메모리 내 실행
- 실제 Kafka와 미세한 차이

Testcontainers Kafka:
- 실제 Kafka와 동일
- 더 느린 시작 (컨테이너 부팅)
- 프로덕션 환경과 동일한 테스트
```

---

## 6. CI/CD 환경 설정

### GitHub Actions

```yaml
# .github/workflows/test.yml
name: Test

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      # Docker는 GitHub Actions에서 기본 제공
      - name: Run tests
        run: ./gradlew test
```

### Jenkins (Docker-in-Docker)

```groovy
// Jenkinsfile
pipeline {
    agent {
        docker {
            image 'gradle:8-jdk17'
            args '-v /var/run/docker.sock:/var/run/docker.sock'  // Docker 소켓 마운트
        }
    }

    stages {
        stage('Test') {
            steps {
                sh './gradlew test'
            }
        }
    }
}
```

### 공통 설정 최적화

```java
// src/test/java/support/TestcontainersConfig.java
@TestConfiguration
public class TestcontainersConfig {

    // 이미지 프리페칭 (CI에서 캐시)
    static {
        DockerImageName.parse("mysql:8.0").asCompatibleSubstituteFor("mysql");
        DockerImageName.parse("redis:7-alpine").asCompatibleSubstituteFor("redis");
    }
}
```

### 환경별 설정

```java
// 로컬에서는 재사용, CI에서는 새로 생성
public class ContainerFactory {

    public static MySQLContainer<?> createMySQLContainer() {
        MySQLContainer<?> container = new MySQLContainer<>("mysql:8.0");

        if (isLocalEnvironment()) {
            container.withReuse(true);
        }

        return container;
    }

    private static boolean isLocalEnvironment() {
        return System.getenv("CI") == null;
    }
}
```

---

## 테스트 성능 최적화

### 병렬 실행

```java
// junit-platform.properties
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=concurrent
junit.jupiter.execution.parallel.mode.classes.default=concurrent

// 컨테이너 공유 시 주의
// @Isolated 어노테이션으로 격리 필요한 테스트 분리
```

### 컨테이너 시작 최적화

```java
@Testcontainers
class OptimizedTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withTmpFs(Map.of("/var/lib/mysql", "rw"))  // tmpfs로 IO 향상
            .withCommand(
                "--character-set-server=utf8mb4",
                "--skip-log-bin",  // 바이너리 로그 비활성화
                "--innodb-flush-method=O_DIRECT_NO_FSYNC"  // fsync 최소화
            );
}
```

---

## 핵심 정리

| 상황 | 권장 방법 |
|------|----------|
| 단위 테스트 | Mock 사용 |
| 통합 테스트 | Testcontainers |
| 빠른 피드백 필요 | 컨테이너 재사용 |
| CI 환경 | 매번 새 컨테이너 |

| 서비스 | 컨테이너 |
|--------|----------|
| MySQL | MySQLContainer |
| PostgreSQL | PostgreSQLContainer |
| Redis | GenericContainer("redis") |
| Kafka | KafkaContainer |
| LocalStack (AWS) | LocalStackContainer |

```
장점:
✓ 실제 환경과 동일한 테스트
✓ 테스트 격리
✓ 설정 자동화
✓ CI/CD 호환

주의점:
- Docker 필수
- 첫 실행 시 이미지 다운로드
- 메모리 사용량 증가
```

---

*마지막 업데이트: 2026년 01월*
