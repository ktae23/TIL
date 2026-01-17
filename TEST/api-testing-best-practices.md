# API 테스트 Best Practices

MockMvc, REST Assured, Spring REST Docs를 활용한 API 테스트 방법을 정리합니다.

## 목차

1. [MockMvc 테스트](#1-mockmvc-테스트)
2. [REST Assured 테스트](#2-rest-assured-테스트)
3. [Spring REST Docs](#3-spring-rest-docs)
4. [테스트 패턴](#4-테스트-패턴)
5. [Best Practices](#5-best-practices)

---

## 1. MockMvc 테스트

### 기본 설정

```java
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;
}
```

### GET 요청 테스트

```java
@Test
void shouldGetUser() throws Exception {
    // given
    User user = new User(1L, "kim", "kim@example.com");
    given(userService.findById(1L)).willReturn(user);

    // when & then
    mockMvc.perform(get("/api/users/{id}", 1L)
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("kim"))
            .andExpect(jsonPath("$.email").value("kim@example.com"));
}
```

### POST 요청 테스트

```java
@Test
void shouldCreateUser() throws Exception {
    // given
    UserRequest request = new UserRequest("kim", "kim@example.com");
    User created = new User(1L, "kim", "kim@example.com");

    given(userService.create(any(UserRequest.class))).willReturn(created);

    // when & then
    mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/users/1"))
            .andExpect(jsonPath("$.id").value(1));
}
```

### 예외 처리 테스트

```java
@Test
void shouldReturn404WhenUserNotFound() throws Exception {
    // given
    given(userService.findById(999L))
            .willThrow(new UserNotFoundException(999L));

    // when & then
    mockMvc.perform(get("/api/users/{id}", 999L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("User not found: 999"));
}

@Test
void shouldReturn400WhenInvalidRequest() throws Exception {
    // given
    UserRequest invalidRequest = new UserRequest("", "invalid-email");

    // when & then
    mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errors[*].field").value(
                    containsInAnyOrder("name", "email")));
}
```

### 인증/인가 테스트

```java
@Test
@WithMockUser(roles = "ADMIN")
void adminCanDeleteUser() throws Exception {
    mockMvc.perform(delete("/api/users/{id}", 1L))
            .andExpect(status().isNoContent());
}

@Test
@WithMockUser(roles = "USER")
void userCannotDeleteOtherUser() throws Exception {
    mockMvc.perform(delete("/api/users/{id}", 1L))
            .andExpect(status().isForbidden());
}

@Test
void unauthenticatedCannotAccess() throws Exception {
    mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized());
}
```

---

## 2. REST Assured 테스트

### 의존성 추가

```groovy
testImplementation 'io.rest-assured:rest-assured:5.4.0'
testImplementation 'io.rest-assured:spring-mock-mvc:5.4.0'
```

### 기본 사용법

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class UserApiTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api";
    }

    @Test
    void shouldGetUser() {
        given()
            .pathParam("id", 1)
        .when()
            .get("/users/{id}")
        .then()
            .statusCode(200)
            .body("id", equalTo(1))
            .body("name", equalTo("kim"))
            .body("email", containsString("@"));
    }
}
```

### POST 요청

```java
@Test
void shouldCreateUser() {
    UserRequest request = new UserRequest("kim", "kim@example.com");

    given()
        .contentType(ContentType.JSON)
        .body(request)
    .when()
        .post("/users")
    .then()
        .statusCode(201)
        .header("Location", matchesPattern("/api/users/\\d+"))
        .body("id", notNullValue())
        .body("name", equalTo("kim"));
}
```

### 응답 추출 및 검증

```java
@Test
void shouldExtractAndVerifyResponse() {
    // 응답 추출
    UserResponse response =
        given()
            .pathParam("id", 1)
        .when()
            .get("/users/{id}")
        .then()
            .statusCode(200)
            .extract()
            .as(UserResponse.class);

    // 추가 검증
    assertThat(response.getName()).isEqualTo("kim");
    assertThat(response.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
}

@Test
void shouldExtractFromJsonPath() {
    String email =
        given()
            .pathParam("id", 1)
        .when()
            .get("/users/{id}")
        .then()
            .extract()
            .jsonPath()
            .getString("email");

    assertThat(email).contains("@");
}
```

### 리스트 응답 테스트

```java
@Test
void shouldGetUserList() {
    given()
        .queryParam("page", 0)
        .queryParam("size", 10)
    .when()
        .get("/users")
    .then()
        .statusCode(200)
        .body("content", hasSize(greaterThan(0)))
        .body("content[0].id", notNullValue())
        .body("totalElements", greaterThanOrEqualTo(1))
        .body("totalPages", greaterThanOrEqualTo(1));
}
```

---

## 3. Spring REST Docs

### 의존성 설정

```groovy
plugins {
    id 'org.asciidoctor.jvm.convert' version '3.3.2'
}

configurations {
    asciidoctorExt
}

dependencies {
    asciidoctorExt 'org.springframework.restdocs:spring-restdocs-asciidoctor'
    testImplementation 'org.springframework.restdocs:spring-restdocs-mockmvc'
}

ext {
    snippetsDir = file('build/generated-snippets')
}

test {
    outputs.dir snippetsDir
}

asciidoctor {
    inputs.dir snippetsDir
    configurations 'asciidoctorExt'
    dependsOn test
}
```

### 기본 설정

```java
@WebMvcTest(UserController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
class UserControllerDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;
}
```

### 문서화된 테스트

```java
@Test
void shouldDocumentGetUser() throws Exception {
    // given
    User user = new User(1L, "kim", "kim@example.com");
    given(userService.findById(1L)).willReturn(user);

    // when & then
    mockMvc.perform(get("/api/users/{id}", 1L)
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andDo(document("user-get",
                    pathParameters(
                            parameterWithName("id").description("사용자 ID")
                    ),
                    responseFields(
                            fieldWithPath("id").description("사용자 ID"),
                            fieldWithPath("name").description("사용자 이름"),
                            fieldWithPath("email").description("이메일 주소")
                    )
            ));
}
```

### POST 요청 문서화

```java
@Test
void shouldDocumentCreateUser() throws Exception {
    // given
    UserRequest request = new UserRequest("kim", "kim@example.com");
    User created = new User(1L, "kim", "kim@example.com");
    given(userService.create(any())).willReturn(created);

    // when & then
    mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andDo(document("user-create",
                    requestFields(
                            fieldWithPath("name").description("사용자 이름")
                                    .attributes(key("constraints").value("2-50자")),
                            fieldWithPath("email").description("이메일 주소")
                                    .attributes(key("constraints").value("유효한 이메일 형식"))
                    ),
                    responseFields(
                            fieldWithPath("id").description("생성된 사용자 ID"),
                            fieldWithPath("name").description("사용자 이름"),
                            fieldWithPath("email").description("이메일 주소")
                    ),
                    responseHeaders(
                            headerWithName("Location").description("생성된 리소스 URL")
                    )
            ));
}
```

### AsciiDoc 템플릿

```asciidoc
// src/docs/asciidoc/index.adoc
= User API 문서
:doctype: book
:toc: left
:toclevels: 2
:sectlinks:

== 개요
사용자 관리 API 문서입니다.

== 사용자 API

=== 사용자 조회
operation::user-get[snippets='http-request,path-parameters,http-response,response-fields']

=== 사용자 생성
operation::user-create[snippets='http-request,request-fields,http-response,response-fields']
```

---

## 4. 테스트 패턴

### Given-When-Then 패턴

```java
@Test
void shouldReturnUserWhenValidId() {
    // Given: 전제 조건 설정
    User expectedUser = new User(1L, "kim", "kim@example.com");
    given(userService.findById(1L)).willReturn(expectedUser);

    // When: 테스트 대상 실행
    ResultActions result = mockMvc.perform(
            get("/api/users/{id}", 1L));

    // Then: 결과 검증
    result.andExpect(status().isOk())
          .andExpect(jsonPath("$.name").value("kim"));
}
```

### 테스트 픽스처

```java
class UserApiTest {

    // 테스트 데이터 빌더
    private UserRequest validRequest() {
        return UserRequest.builder()
                .name("kim")
                .email("kim@example.com")
                .build();
    }

    private User savedUser(Long id) {
        return User.builder()
                .id(id)
                .name("kim")
                .email("kim@example.com")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldCreateUser() throws Exception {
        given(userService.create(any())).willReturn(savedUser(1L));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(validRequest())))
                .andExpect(status().isCreated());
    }
}
```

### 중복 제거: 커스텀 ResultMatcher

```java
public class UserResultMatchers {

    public static ResultMatcher isValidUser() {
        return result -> {
            MockMvcResultMatchers.jsonPath("$.id").exists().match(result);
            MockMvcResultMatchers.jsonPath("$.name").isNotEmpty().match(result);
            MockMvcResultMatchers.jsonPath("$.email").exists().match(result);
        };
    }

    public static ResultMatcher hasValidationError(String field) {
        return result -> {
            MockMvcResultMatchers.jsonPath("$.errors[*].field")
                    .value(hasItem(field))
                    .match(result);
        };
    }
}

// 사용
@Test
void shouldReturnValidUser() throws Exception {
    mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(isValidUser());
}
```

---

## 5. Best Practices

### 테스트 구조

```
테스트 클래스 구조:
┌─────────────────────────────────────────────────────────────┐
│  @WebMvcTest - 컨트롤러 단위 테스트                          │
│  - 빠른 실행                                                │
│  - Service Mock 사용                                        │
│                                                             │
│  @SpringBootTest - 통합 테스트                              │
│  - 전체 컨텍스트 로드                                        │
│  - 실제 DB (Testcontainers)                                 │
│  - E2E 시나리오                                             │
└─────────────────────────────────────────────────────────────┘
```

### 테스트 가독성

```java
// Bad: 무슨 테스트인지 알기 어려움
@Test
void test1() { ... }

// Good: 시나리오가 명확함
@Test
@DisplayName("존재하지 않는 사용자 조회 시 404 응답")
void shouldReturn404WhenUserNotFound() { ... }

// Good: 한글 메서드명
@Test
void 존재하지_않는_사용자_조회시_404_응답() { ... }
```

### 경계값 테스트

```java
@Nested
@DisplayName("사용자 생성 유효성 검증")
class CreateUserValidation {

    @Test
    @DisplayName("이름이 비어있으면 400 에러")
    void shouldRejectEmptyName() { ... }

    @Test
    @DisplayName("이름이 1자면 400 에러")
    void shouldRejectNameTooShort() { ... }

    @Test
    @DisplayName("이름이 2자면 성공")
    void shouldAcceptMinimumName() { ... }

    @Test
    @DisplayName("이름이 50자면 성공")
    void shouldAcceptMaximumName() { ... }

    @Test
    @DisplayName("이름이 51자면 400 에러")
    void shouldRejectNameTooLong() { ... }
}
```

### 응답 시간 검증

```java
@Test
void shouldRespondWithin500ms() throws Exception {
    long startTime = System.currentTimeMillis();

    mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk());

    long duration = System.currentTimeMillis() - startTime;
    assertThat(duration).isLessThan(500);
}
```

### 멱등성 테스트

```java
@Test
void putShouldBeIdempotent() throws Exception {
    UserRequest request = new UserRequest("kim", "kim@example.com");

    // 첫 번째 PUT
    mockMvc.perform(put("/api/users/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
            .andExpect(status().isOk());

    // 두 번째 PUT (동일 요청)
    mockMvc.perform(put("/api/users/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request)))
            .andExpect(status().isOk());

    // 결과가 동일해야 함
}
```

---

## MockMvc vs REST Assured 비교

| 특성 | MockMvc | REST Assured |
|------|---------|--------------|
| 서버 시작 | 불필요 | 필요 |
| 속도 | 빠름 | 상대적 느림 |
| 문법 | Spring 스타일 | BDD 스타일 |
| REST Docs 통합 | 기본 지원 | 추가 설정 필요 |
| 실제 HTTP | Mock | 실제 |
| 사용 시점 | 단위 테스트 | E2E 테스트 |

---

## 핵심 체크리스트

```
API 테스트 체크리스트:
□ 정상 케이스 (200, 201)
□ 클라이언트 에러 (400, 401, 403, 404)
□ 서버 에러 (500)
□ 유효성 검증 에러
□ 인증/인가
□ 페이지네이션
□ 정렬/필터링
□ 멱등성 (PUT, DELETE)
□ 에러 응답 형식
□ 응답 헤더 (Location, Content-Type)
```

---

*마지막 업데이트: 2025년 01월*
