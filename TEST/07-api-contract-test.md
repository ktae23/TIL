# API Contract Test

마이크로서비스 아키텍처에서 서비스 간 API 계약을 자동으로 검증하는 테스트 기법이다.

## 목차

- [Contract Test란?](#contract-test란)
- [왜 필요한가?](#왜-필요한가)
- [테스트 피라미드에서의 위치](#테스트-피라미드에서의-위치)
- [주요 도구 비교](#주요-도구-비교)
- [Pact 예제](#pact-예제)
- [Spring Cloud Contract 예제](#spring-cloud-contract-예제)
- [Consumer-Driven vs Provider-Driven](#consumer-driven-vs-provider-driven)
- [실전 팁](#실전-팁)

---

## Contract Test란?

**API 제공자(Provider)와 소비자(Consumer) 사이의 계약(Contract)을 자동화된 테스트로 검증**하는 방법이다.
E2E 테스트 없이도 서비스 간 통신이 깨지지 않았음을 보장할 수 있다.

```
[Consumer] ---HTTP 요청--→ [Provider]
     ↑                          ↑
     └── Contract(계약서) ───────┘
         "이 요청을 보내면, 이 응답이 온다"
```

## 왜 필요한가?

### 통합 테스트의 한계

```
❌ 전통적인 E2E 테스트
┌─────────┐    ┌─────────┐    ┌─────────┐
│ Service A│───→│ Service B│───→│ Service C│
└─────────┘    └─────────┘    └─────────┘
  ↑ 모든 서비스를 동시에 띄워야 함
  ↑ 느리고, 불안정하고, 실패 원인 파악 어려움
```

```
✅ Contract Test
┌─────────┐              ┌─────────┐
│ Service A│──Contract──→│ Service B│
└─────────┘   (독립)      └─────────┘
  ↑ 각 서비스를 독립적으로 테스트
  ↑ 빠르고, 안정적이고, 실패 원인 명확
```

### 주요 문제 시나리오

```java
// Provider가 응답 필드명을 변경한 경우
// Before: {"userName": "buzz"}
// After:  {"user_name": "buzz"}  ← Consumer 깨짐!

// Contract Test가 이를 사전에 감지한다
```

## 테스트 피라미드에서의 위치

```
        /  E2E  \          ← 느림, 비용 높음
       /----------\
      / Contract   \       ← ★ 여기! 서비스 간 계약 검증
     /--------------\
    / Integration    \     ← 컴포넌트 통합
   /------------------\
  /    Unit Tests      \   ← 빠름, 비용 낮음
 /______________________\
```

## 주요 도구 비교

| 특성 | **Pact** | **Spring Cloud Contract** |
|------|----------|--------------------------|
| 접근 방식 | Consumer-Driven | Provider-Driven (주로) |
| 언어 지원 | 다양 (Java, JS, Python, Go 등) | Java/Kotlin (Spring 생태계) |
| 계약 저장 | Pact Broker | Git 저장소 |
| 계약 형식 | JSON (Pact 파일) | Groovy DSL / YAML |
| 학습 곡선 | 중간 | 낮음 (Spring 사용자) |
| 비동기 지원 | Pact v4+ | 기본 지원 (Messaging) |

## Pact 예제

### Consumer 측 테스트 (주문 서비스 → 사용자 서비스)

```java
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "user-service", port = "8080")
class OrderServiceConsumerTest {

    // 1. 계약(Pact) 정의 - "이 요청을 보내면, 이 응답을 기대한다"
    @Pact(provider = "user-service", consumer = "order-service")
    V4Pact getUserPact(PactDslWithProvider builder) {
        return builder
            .given("사용자 ID 1이 존재함")
            .uponReceiving("사용자 정보 조회 요청")
                .path("/api/users/1")
                .method("GET")
            .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(newJsonBody(body -> {
                    body.integerType("id", 1);
                    body.stringType("name", "buzz");
                    body.stringType("email", "buzz@example.com");
                }).build())
            .toPact(V4Pact.class);
    }

    // 2. 계약 기반으로 Consumer 로직 테스트
    @Test
    @PactTestFor(pactMethod = "getUserPact")
    void shouldGetUserInfo(MockServer mockServer) {
        // Pact가 Mock 서버를 자동으로 띄워준다
        UserClient client = new UserClient(mockServer.getUrl());

        UserResponse user = client.getUser(1L);

        assertThat(user.getName()).isEqualTo("buzz");
        assertThat(user.getEmail()).isEqualTo("buzz@example.com");
    }
}
```

### Provider 측 검증 (사용자 서비스)

```java
@Provider("user-service")
@PactBroker(url = "http://pact-broker:9292")  // Pact Broker에서 계약 가져옴
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserServiceProviderTest {

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }

    // Provider State 설정 - Consumer가 정의한 "given" 조건 충족
    @State("사용자 ID 1이 존재함")
    void setupUser() {
        userRepository.save(new User(1L, "buzz", "buzz@example.com"));
    }
}
```

### 생성되는 Pact 파일 (JSON)

```json
{
  "consumer": { "name": "order-service" },
  "provider": { "name": "user-service" },
  "interactions": [
    {
      "description": "사용자 정보 조회 요청",
      "providerStates": [
        { "name": "사용자 ID 1이 존재함" }
      ],
      "request": {
        "method": "GET",
        "path": "/api/users/1"
      },
      "response": {
        "status": 200,
        "headers": { "Content-Type": "application/json" },
        "body": { "id": 1, "name": "buzz", "email": "buzz@example.com" }
      }
    }
  ]
}
```

## Spring Cloud Contract 예제

### 1. Contract DSL 작성 (Provider 측)

```groovy
// src/test/resources/contracts/user/getUser.groovy
Contract.make {
    description "사용자 정보 조회"

    request {
        method GET()
        url "/api/users/1"
        headers {
            contentType applicationJson()
        }
    }

    response {
        status OK()
        headers {
            contentType applicationJson()
        }
        body(
            id: 1,
            name: $(regex('[a-zA-Z]+')),
            email: $(regex('[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}'))
        )
    }
}
```

### 2. Provider 측 Base 테스트 클래스

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public abstract class BaseContractTest {

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private UserRepository userRepository;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.webAppContextSetup(context);

        // 테스트 데이터 설정
        when(userRepository.findById(1L))
            .thenReturn(Optional.of(new User(1L, "buzz", "buzz@example.com")));
    }
}
```

### 3. 빌드 시 자동 생성되는 테스트

```java
// build 시 자동 생성됨 (generated-test-sources)
public class ContractVerifierTest extends BaseContractTest {

    @Test
    public void validate_getUser() throws Exception {
        // Contract DSL 기반으로 자동 생성된 테스트
        given()
            .header("Content-Type", "application/json")
        .when()
            .get("/api/users/1")
        .then()
            .statusCode(200)
            .header("Content-Type", "application/json")
            .body("id", equalTo(1))
            .body("name", matchesPattern("[a-zA-Z]+"))
            .body("email", matchesPattern("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"));
    }
}
```

### 4. Consumer 측 Stub 활용

```java
@SpringBootTest
@AutoConfigureStubRunner(
    ids = "com.example:user-service:+:stubs:8080",
    stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
class OrderServiceContractTest {

    @Autowired
    private UserClient userClient;

    @Test
    void shouldGetUserFromStub() {
        // Provider의 Contract에서 자동 생성된 Stub(WireMock) 사용
        UserResponse user = userClient.getUser(1L);

        assertThat(user.getId()).isEqualTo(1);
        assertThat(user.getName()).isNotBlank();
        assertThat(user.getEmail()).contains("@");
    }
}
```

## Consumer-Driven vs Provider-Driven

```
Consumer-Driven (Pact):
  Consumer가 계약을 작성 → Provider가 충족하는지 검증
  "나는 이런 응답이 필요해" → "알겠어, 맞춰줄게"

Provider-Driven (Spring Cloud Contract):
  Provider가 계약을 작성 → Consumer에게 Stub 제공
  "내 API는 이렇게 동작해" → "알겠어, 그에 맞춰 호출할게"
```

```java
// Consumer-Driven: Consumer가 필요한 것만 명시
// → Provider 변경 시 Consumer 영향도를 정확히 파악 가능
@Pact(consumer = "order-service")
V4Pact pact(PactDslWithProvider builder) {
    return builder
        .uponReceiving("주문에 필요한 사용자 정보")
        .path("/api/users/1")
        .willRespondWith()
        .body(newJsonBody(b -> {
            b.stringType("name");   // 주문서에 name만 필요
            b.stringType("email");  // 알림용 email만 필요
            // address, phone 등은 신경 안 씀
        }).build())
        .toPact(V4Pact.class);
}
```

## 실전 팁

### CI/CD 파이프라인 통합

```yaml
# Provider 파이프라인
stages:
  - test:
      script:
        - ./gradlew test                    # 단위 테스트
        - ./gradlew contractTest            # Contract 검증
        - ./gradlew publishStubs            # Stub 발행
  - deploy:
      script:
        - ./gradlew pactVerify              # Pact Broker 검증
        - kubectl apply -f deployment.yaml  # 배포
```

### 자주 하는 실수

```java
// ❌ 응답 값을 하드코딩하지 말 것
body("name", equalTo("buzz"))  // 특정 값에 의존

// ✅ 타입과 패턴으로 검증할 것
body.stringType("name", "buzz")      // Pact: 타입 매칭
body(name: $(regex('[a-zA-Z]+')))     // SCC: 패턴 매칭
```

```java
// ❌ 모든 필드를 계약에 넣지 말 것
// Consumer가 실제로 사용하는 필드만 포함

// ✅ Consumer가 필요한 필드만 명시
body.stringType("name");   // 필요한 것만
body.stringType("email");  // 필요한 것만
// phone, address 등은 계약에서 제외
```

---

*마지막 업데이트: 2026년 02월*
