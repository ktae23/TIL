# 통합 테스트 전략

## 목차
1. [통합 테스트 개요](#통합-테스트-개요)
2. [@SpringBootTest](#springboottest)
3. [슬라이스 테스트](#슬라이스-테스트)
4. [Testcontainers](#testcontainers)
5. [테스트 설정 최적화](#테스트-설정-최적화)
6. [핵심 정리](#핵심-정리)

---

## 통합 테스트 개요

### 통합 테스트 범위

```
┌──────────────────────────────────────────────────────────────────┐
│                    통합 테스트 범위                               │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  @SpringBootTest (전체 컨텍스트)                                 │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  Controller → Service → Repository → Database              │ │
│  │       ↓          ↓           ↓            ↓                 │ │
│  │  @WebMvcTest  @Mock      @DataJpaTest  Testcontainers      │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  슬라이스 테스트 (특정 레이어만)                                  │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐                │
│  │ @WebMvcTest │ │ @DataJpaTest│ │ @WebFlux    │                │
│  │ (Web 레이어) │ │ (JPA 레이어)│ │ Test        │                │
│  └─────────────┘ └─────────────┘ └─────────────┘                │
│                                                                   │
│  선택 기준:                                                      │
│  - 전체 흐름 검증 → @SpringBootTest                              │
│  - 특정 레이어만 → 슬라이스 테스트                               │
│  - 실제 DB 필요 → Testcontainers                                 │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

---

## @SpringBootTest

### 기본 사용법

```java
@SpringBootTest
class ApplicationIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void contextLoads() {
        // 애플리케이션 컨텍스트 로드 확인
        assertThat(userService).isNotNull();
    }

    @Test
    void createAndFindUser() {
        // Given
        UserRequest request = new UserRequest("test@example.com", "password");

        // When
        User created = userService.createUser(request);
        User found = userService.findById(created.getId());

        // Then
        assertThat(found.getEmail()).isEqualTo("test@example.com");
    }
}
```

### WebEnvironment 옵션

```java
// MOCK (기본값): MockMvc로 테스트, 서버 시작 안 함
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class MockEnvTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void test() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk());
    }
}

// RANDOM_PORT: 랜덤 포트로 실제 서버 시작
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RandomPortTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void test() {
        ResponseEntity<User> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/users/1",
            User.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}

// DEFINED_PORT: application.yml에 정의된 포트 사용
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class DefinedPortTest {
    // server.port 사용
}

// NONE: 웹 환경 없이 테스트
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class NoWebEnvTest {
    // 서비스 레이어만 테스트
}
```

### 설정 커스터마이징

```java
// 특정 설정 클래스만 로드
@SpringBootTest(classes = {UserService.class, UserRepository.class})
class SpecificConfigTest {
    // ...
}

// 프로퍼티 오버라이드
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PropertyOverrideTest {
    // ...
}

// 프로파일 설정
@SpringBootTest
@ActiveProfiles("test")
class ProfileTest {
    // application-test.yml 사용
}
```

---

## 슬라이스 테스트

### @WebMvcTest

```java
// Controller 레이어만 테스트
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean  // Spring Context의 Bean을 Mock으로 대체
    private UserService userService;

    @Test
    @DisplayName("사용자 조회 API 테스트")
    void getUser() throws Exception {
        // Given
        User user = new User(1L, "test@example.com", "홍길동");
        when(userService.findById(1L)).thenReturn(user);

        // When & Then
        mockMvc.perform(get("/api/users/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.name").value("홍길동"))
            .andDo(print());
    }

    @Test
    @DisplayName("사용자 생성 API 테스트")
    void createUser() throws Exception {
        // Given
        UserRequest request = new UserRequest("test@example.com", "password");
        User createdUser = new User(1L, "test@example.com", "신규사용자");

        when(userService.createUser(any(UserRequest.class))).thenReturn(createdUser);

        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(header().exists("Location"));
    }

    @Test
    @DisplayName("잘못된 요청 시 400 응답")
    void createUser_InvalidRequest() throws Exception {
        // Given
        UserRequest request = new UserRequest("", "");  // 유효성 검증 실패

        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").isArray());
    }
}
```

### @DataJpaTest

```java
// JPA Repository 레이어만 테스트
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("이메일로 사용자 조회")
    void findByEmail() {
        // Given
        User user = new User("test@example.com", "홍길동");
        entityManager.persist(user);
        entityManager.flush();

        // When
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("커스텀 쿼리 테스트")
    void findActiveUsersByRole() {
        // Given
        User admin = new User("admin@example.com", "관리자", Role.ADMIN, true);
        User user = new User("user@example.com", "사용자", Role.USER, true);
        User inactive = new User("inactive@example.com", "비활성", Role.ADMIN, false);

        entityManager.persist(admin);
        entityManager.persist(user);
        entityManager.persist(inactive);
        entityManager.flush();

        // When
        List<User> activeAdmins = userRepository.findByRoleAndActiveTrue(Role.ADMIN);

        // Then
        assertThat(activeAdmins).hasSize(1);
        assertThat(activeAdmins.get(0).getEmail()).isEqualTo("admin@example.com");
    }
}

// Testcontainers와 함께 사용
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryWithContainerTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    // 테스트 코드...
}
```

### 기타 슬라이스 테스트

```java
// @WebFluxTest: WebFlux Controller
@WebFluxTest(UserController.class)
class UserControllerWebFluxTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private UserService userService;

    @Test
    void getUser() {
        when(userService.findById(1L)).thenReturn(Mono.just(user));

        webTestClient.get()
            .uri("/api/users/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody(User.class)
            .isEqualTo(user);
    }
}

// @JsonTest: JSON 직렬화/역직렬화
@JsonTest
class UserJsonTest {

    @Autowired
    private JacksonTester<User> json;

    @Test
    void serialize() throws Exception {
        User user = new User(1L, "test@example.com");

        assertThat(json.write(user))
            .hasJsonPathNumberValue("$.id")
            .extractingJsonPathNumberValue("$.id").isEqualTo(1);
    }

    @Test
    void deserialize() throws Exception {
        String content = "{\"id\":1,\"email\":\"test@example.com\"}";

        assertThat(json.parse(content))
            .extracting(User::getEmail)
            .isEqualTo("test@example.com");
    }
}

// @RestClientTest: RestTemplate/WebClient
@RestClientTest(ExternalApiClient.class)
class ExternalApiClientTest {

    @Autowired
    private ExternalApiClient client;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void getUser() {
        server.expect(requestTo("/api/external/users/1"))
            .andRespond(withSuccess("{\"name\":\"John\"}", MediaType.APPLICATION_JSON));

        ExternalUser user = client.getUser(1L);

        assertThat(user.getName()).isEqualTo("John");
    }
}
```

---

## Testcontainers

### 기본 설정

```java
// 의존성: testImplementation 'org.testcontainers:junit-jupiter:1.19.3'
//         testImplementation 'org.testcontainers:mysql:1.19.3'

@SpringBootTest
@Testcontainers
class ContainerIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    void testWithRealDatabase() {
        User user = userRepository.save(new User("test@example.com"));
        assertThat(user.getId()).isNotNull();
    }
}
```

### 여러 컨테이너 조합

```java
@SpringBootTest
@Testcontainers
class MultiContainerTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7")
        .withExposedPorts(6379);

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // MySQL
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);

        // Redis
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
}
```

### 컨테이너 재사용

```java
// 테스트 간 컨테이너 공유 (빠른 실행)
// ~/.testcontainers.properties에 testcontainers.reuse.enable=true 추가

@Container
static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
    .withReuse(true);  // 재사용 활성화

// 또는 추상 클래스로 공유
abstract class AbstractIntegrationTest {

    @Container
    protected static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
}

class UserServiceIntegrationTest extends AbstractIntegrationTest {
    // 컨테이너 공유
}

class OrderServiceIntegrationTest extends AbstractIntegrationTest {
    // 컨테이너 공유
}
```

---

## 테스트 설정 최적화

### 테스트 설정 분리

```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

  cache:
    type: none  # 캐시 비활성화

logging:
  level:
    org.springframework.test: DEBUG
    org.testcontainers: INFO
```

### 테스트 속도 최적화

```java
// 1. 컨텍스트 캐싱 활용 (같은 설정은 재사용)
@SpringBootTest
@ActiveProfiles("test")  // 동일 프로파일 = 컨텍스트 재사용
class Test1 {}

@SpringBootTest
@ActiveProfiles("test")  // 컨텍스트 재사용
class Test2 {}

// 2. 필요한 빈만 로드
@SpringBootTest(classes = {
    UserService.class,
    UserRepository.class,
    TestConfig.class
})
class LimitedContextTest {}

// 3. @DirtiesContext 최소화 (컨텍스트 오염 시에만)
@SpringBootTest
class Test {

    @Test
    @DirtiesContext  // 이 테스트 후 컨텍스트 재생성
    void dirtyTest() {
        // 컨텍스트를 변경하는 테스트
    }
}

// 4. 병렬 실행
// junit-platform.properties
junit.jupiter.execution.parallel.enabled = true
junit.jupiter.execution.parallel.mode.default = concurrent
junit.jupiter.execution.parallel.mode.classes.default = concurrent
```

### 테스트 데이터 관리

```java
// SQL 스크립트로 데이터 초기화
@Sql(scripts = "/data/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/data/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@Test
void testWithSqlData() {}

// 또는 @BeforeEach에서 직접 관리
@BeforeEach
void setUp() {
    userRepository.deleteAll();
    userRepository.save(new User("test@example.com"));
}

// Transactional 롤백
@SpringBootTest
@Transactional  // 각 테스트 후 롤백
class TransactionalTest {

    @Test
    void test1() {
        userRepository.save(new User("test1@example.com"));
        // 테스트 종료 시 롤백
    }

    @Test
    void test2() {
        // test1의 데이터 없음 (롤백됨)
    }
}
```

---

## 핵심 정리

### 테스트 어노테이션 선택

| 어노테이션 | 범위 | 속도 | 용도 |
|-----------|------|------|------|
| @SpringBootTest | 전체 | 느림 | 통합 테스트 |
| @WebMvcTest | Web | 빠름 | Controller |
| @DataJpaTest | JPA | 빠름 | Repository |
| @JsonTest | JSON | 빠름 | 직렬화 |
| @RestClientTest | Client | 빠름 | 외부 API |

### 테스트 전략 체크리스트

```
□ 슬라이스 테스트로 충분한가? (빠른 피드백)
□ Testcontainers로 실제 DB 테스트가 필요한가?
□ @MockBean vs @Mock 선택 (Spring Context 필요 여부)
□ 테스트 데이터 초기화 전략 결정
□ 컨텍스트 캐싱 최대 활용
□ 병렬 실행 가능 여부 확인
```

### 면접 대비 핵심 질문

1. **Q: @SpringBootTest와 @WebMvcTest의 차이점은?**
   - A: @SpringBootTest는 전체 컨텍스트 로드, @WebMvcTest는 Web 레이어만. 속도는 @WebMvcTest가 빠르고, 전체 흐름 테스트는 @SpringBootTest

2. **Q: @MockBean과 @Mock의 차이점은?**
   - A: @MockBean은 Spring Context의 Bean을 Mock으로 대체, @Mock은 Mockito 단독 Mock 생성. 슬라이스 테스트에서는 @MockBean 사용

3. **Q: Testcontainers를 사용하는 이유는?**
   - A: 실제 DB(MySQL, PostgreSQL 등)로 테스트하여 H2와의 차이 문제 방지. Docker로 격리된 환경 제공, CI/CD에서도 동일하게 실행

4. **Q: 테스트 컨텍스트 캐싱이란?**
   - A: 동일한 설정(@ActiveProfiles, properties 등)의 테스트는 같은 ApplicationContext 재사용. @DirtiesContext 사용 시 재생성

---

*마지막 업데이트: 2026년 01월*
