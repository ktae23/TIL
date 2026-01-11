# Phase 2: 중급 단계 - 실무 핵심 기술

실무에서 반드시 필요한 API 설계, 인증/인가, 버전 관리를 마스터하고 실전 프로젝트를 통해 역량을 쌓습니다.

## 목차

- [1. RESTful API 설계](#1-restful-api-설계)
- [2. 인증과 인가](#2-인증과-인가)
- [3. 버전 관리 (Git)](#3-버전-관리-git)
- [4. 테스트 작성](#4-테스트-작성)
- [5. 실전 프로젝트](#5-실전-프로젝트)
- [6. 중급 단계 체크리스트](#6-중급-단계-체크리스트)

---

## 1. RESTful API 설계

### REST란?

**REST(Representational State Transfer)** 는 웹 서비스 설계를 위한 아키텍처 스타일입니다. HTTP 프로토콜을 활용하여 자원(Resource)을 URI로 표현하고, HTTP 메서드로 행위를 정의합니다.

### REST 6가지 제약 조건

| 제약 조건 | 설명 |
|----------|------|
| **Client-Server** | 클라이언트와 서버의 역할 분리 |
| **Stateless** | 각 요청은 독립적, 서버에 상태 저장 안 함 |
| **Cacheable** | 응답은 캐싱 가능해야 함 |
| **Uniform Interface** | 일관된 인터페이스 유지 |
| **Layered System** | 계층화된 시스템 아키텍처 |
| **Code on Demand** | (선택) 클라이언트에서 코드 실행 가능 |

### URI 설계 원칙

```
✅ 좋은 예시
GET    /users                    # 사용자 목록
GET    /users/123                # 특정 사용자
GET    /users/123/posts          # 특정 사용자의 게시글
POST   /users                    # 사용자 생성
PUT    /users/123                # 사용자 전체 수정
PATCH  /users/123                # 사용자 부분 수정
DELETE /users/123                # 사용자 삭제

❌ 나쁜 예시
GET    /getUsers                 # 동사 사용
GET    /user/123                 # 단수형 사용
POST   /users/create             # 동사 중복
GET    /users/123/delete         # GET으로 삭제
```

### URI 설계 규칙

```
1. 명사를 사용 (동사 X)
   ✅ /orders
   ❌ /createOrder

2. 복수형 사용
   ✅ /users/123
   ❌ /user/123

3. 소문자 사용
   ✅ /user-profiles
   ❌ /userProfiles

4. 언더스코어(_) 대신 하이픈(-) 사용
   ✅ /user-profiles
   ❌ /user_profiles

5. 파일 확장자 포함 안 함
   ✅ /users/123
   ❌ /users/123.json

6. 계층 관계 표현
   ✅ /users/123/orders/456
   ❌ /user-order?userId=123&orderId=456
```

### HTTP 메서드 활용

| 메서드 | 용도 | 멱등성 | 안전성 | 예시 |
|--------|------|--------|--------|------|
| **GET** | 조회 | O | O | 리소스 조회 |
| **POST** | 생성 | X | X | 새 리소스 생성 |
| **PUT** | 전체 수정 | O | X | 리소스 전체 교체 |
| **PATCH** | 부분 수정 | X | X | 리소스 일부 수정 |
| **DELETE** | 삭제 | O | X | 리소스 삭제 |

### HTTP 상태 코드

```
2xx: 성공
├── 200 OK              # 일반 성공
├── 201 Created         # 생성 성공
├── 204 No Content      # 성공, 응답 본문 없음

3xx: 리다이렉션
├── 301 Moved Permanently   # 영구 이동
├── 302 Found               # 임시 이동
└── 304 Not Modified        # 캐시 사용

4xx: 클라이언트 오류
├── 400 Bad Request         # 잘못된 요청
├── 401 Unauthorized        # 인증 필요
├── 403 Forbidden           # 권한 없음
├── 404 Not Found           # 리소스 없음
├── 409 Conflict            # 충돌 (중복 등)
└── 422 Unprocessable Entity # 유효성 검증 실패

5xx: 서버 오류
├── 500 Internal Server Error   # 서버 에러
├── 502 Bad Gateway             # 게이트웨이 에러
└── 503 Service Unavailable     # 서비스 이용 불가
```

### 요청/응답 설계

#### 요청 (Request)

```json
// POST /api/users
{
    "email": "user@example.com",
    "name": "홍길동",
    "password": "securePassword123!"
}

// PATCH /api/users/123
{
    "name": "김철수"
}
```

#### 응답 (Response)

```json
// 단일 리소스 응답
{
    "id": 123,
    "email": "user@example.com",
    "name": "홍길동",
    "createdAt": "2024-01-15T09:30:00Z"
}

// 목록 응답 (페이지네이션)
{
    "data": [
        { "id": 1, "name": "홍길동" },
        { "id": 2, "name": "김철수" }
    ],
    "pagination": {
        "page": 1,
        "size": 20,
        "totalElements": 100,
        "totalPages": 5
    }
}

// 에러 응답
{
    "error": {
        "code": "VALIDATION_ERROR",
        "message": "입력값이 올바르지 않습니다",
        "details": [
            {
                "field": "email",
                "message": "유효한 이메일 형식이 아닙니다"
            }
        ]
    }
}
```

### 쿼리 파라미터 활용

```
# 페이지네이션
GET /api/users?page=1&size=20

# 정렬
GET /api/users?sort=createdAt,desc

# 필터링
GET /api/users?status=ACTIVE&role=ADMIN

# 검색
GET /api/users?q=홍길동

# 필드 선택 (Sparse Fieldsets)
GET /api/users?fields=id,name,email

# 복합 예시
GET /api/users?status=ACTIVE&sort=createdAt,desc&page=1&size=20
```

### API 버저닝

```
# URL Path 방식 (가장 일반적)
GET /api/v1/users
GET /api/v2/users

# Header 방식
GET /api/users
Accept: application/vnd.company.v1+json

# Query Parameter 방식
GET /api/users?version=1
```

---

## 2. 인증과 인가

### 인증 vs 인가

| 구분 | 인증 (Authentication) | 인가 (Authorization) |
|------|----------------------|---------------------|
| 질문 | "당신은 누구인가?" | "당신은 무엇을 할 수 있는가?" |
| 목적 | 신원 확인 | 권한 확인 |
| 시점 | 먼저 수행 | 인증 후 수행 |
| 예시 | 로그인 | 관리자 페이지 접근 |

### Session 기반 인증

```
┌──────────┐                    ┌──────────┐
│  Client  │                    │  Server  │
└────┬─────┘                    └────┬─────┘
     │                               │
     │  1. POST /login               │
     │  {email, password}            │
     │──────────────────────────────>│
     │                               │
     │  2. 세션 생성 & 저장           │
     │     Session ID 발급            │
     │                               │
     │  3. Set-Cookie: SESSIONID=xxx │
     │<──────────────────────────────│
     │                               │
     │  4. GET /api/profile          │
     │  Cookie: SESSIONID=xxx        │
     │──────────────────────────────>│
     │                               │
     │  5. 세션 조회 & 검증           │
     │                               │
     │  6. 사용자 정보 응답           │
     │<──────────────────────────────│
```

```java
// Spring Security Session 설정
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(true)
            )
            .formLogin(form -> form
                .loginProcessingUrl("/login")
                .successHandler(successHandler())
            );
        return http.build();
    }
}
```

### JWT (JSON Web Token) 인증

#### JWT 구조

```
Header.Payload.Signature

eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.    # Header (Base64)
eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4iLCJpYXQiOjE1MTYyMzkwMjJ9.    # Payload (Base64)
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c    # Signature
```

#### JWT 인증 흐름

```
┌──────────┐                    ┌──────────┐
│  Client  │                    │  Server  │
└────┬─────┘                    └────┬─────┘
     │                               │
     │  1. POST /login               │
     │  {email, password}            │
     │──────────────────────────────>│
     │                               │
     │  2. 사용자 검증                │
     │     JWT 생성                   │
     │                               │
     │  3. { accessToken, refreshToken } │
     │<──────────────────────────────│
     │                               │
     │  4. GET /api/profile          │
     │  Authorization: Bearer {token}│
     │──────────────────────────────>│
     │                               │
     │  5. JWT 검증 (서명 확인)       │
     │                               │
     │  6. 사용자 정보 응답           │
     │<──────────────────────────────│
```

#### JWT 구현 예시 (Java)

```java
@Service
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-validity}")
    private long accessTokenValidity; // 30분

    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidity; // 7일

    public String createAccessToken(Long userId, String role) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenValidity);

        return Jwts.builder()
            .setSubject(String.valueOf(userId))
            .claim("role", role)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(SignatureAlgorithm.HS256, secretKey)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(secretKey)
            .parseClaimsJws(token)
            .getBody();
        return Long.parseLong(claims.getSubject());
    }
}
```

### Access Token + Refresh Token 전략

```
┌─────────────────────────────────────────────────────────┐
│                    Token 전략                           │
├─────────────────┬───────────────────────────────────────┤
│ Access Token    │ - 짧은 유효기간 (15분 ~ 1시간)         │
│                 │ - 매 요청마다 사용                     │
│                 │ - 탈취 시 피해 최소화                  │
├─────────────────┼───────────────────────────────────────┤
│ Refresh Token   │ - 긴 유효기간 (7일 ~ 30일)            │
│                 │ - Access Token 갱신용                  │
│                 │ - 안전한 저장소에 보관                 │
│                 │ - DB에 저장하여 무효화 가능            │
└─────────────────┴───────────────────────────────────────┘
```

```java
// 토큰 갱신 API
@PostMapping("/auth/refresh")
public TokenResponse refresh(@RequestBody RefreshRequest request) {
    // 1. Refresh Token 유효성 검증
    if (!jwtTokenProvider.validateToken(request.refreshToken())) {
        throw new InvalidTokenException();
    }

    // 2. DB에서 Refresh Token 확인 (화이트리스트 방식)
    RefreshToken stored = refreshTokenRepository
        .findByToken(request.refreshToken())
        .orElseThrow(InvalidTokenException::new);

    // 3. 새로운 Access Token 발급
    String newAccessToken = jwtTokenProvider.createAccessToken(
        stored.getUserId(),
        stored.getRole()
    );

    return new TokenResponse(newAccessToken);
}
```

### OAuth 2.0

소셜 로그인(Google, Kakao 등)에서 사용하는 인증 프레임워크입니다.

```
┌────────┐     ┌────────────┐     ┌──────────────┐     ┌────────────┐
│  User  │     │   Client   │     │ Auth Server  │     │  Resource  │
│        │     │ (우리 서버) │     │ (카카오 등)   │     │   Server   │
└───┬────┘     └─────┬──────┘     └──────┬───────┘     └─────┬──────┘
    │                │                    │                   │
    │ 1. 로그인 버튼  │                    │                   │
    │───────────────>│                    │                   │
    │                │                    │                   │
    │ 2. 인증 페이지로 리다이렉트          │                   │
    │<───────────────│                    │                   │
    │                │                    │                   │
    │ 3. 로그인 & 권한 동의               │                   │
    │────────────────────────────────────>│                   │
    │                │                    │                   │
    │ 4. Authorization Code               │                   │
    │<────────────────────────────────────│                   │
    │                │                    │                   │
    │ 5. Code 전달   │                    │                   │
    │───────────────>│                    │                   │
    │                │                    │                   │
    │                │ 6. Code로 Token 요청│                   │
    │                │───────────────────>│                   │
    │                │                    │                   │
    │                │ 7. Access Token    │                   │
    │                │<───────────────────│                   │
    │                │                    │                   │
    │                │ 8. 사용자 정보 요청  │                   │
    │                │────────────────────────────────────────>│
    │                │                    │                   │
    │                │ 9. 사용자 정보      │                   │
    │                │<────────────────────────────────────────│
    │                │                    │                   │
    │ 10. 로그인 완료 (자체 JWT 발급)      │                   │
    │<───────────────│                    │                   │
```

### 권한 관리 (RBAC)

**RBAC (Role-Based Access Control)**: 역할 기반 접근 제어

```java
// 역할 정의
public enum Role {
    USER,       // 일반 사용자
    SELLER,     // 판매자
    ADMIN       // 관리자
}

// Spring Security 권한 설정
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            // 공개 API
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()

            // 인증 필요
            .requestMatchers("/api/users/me").authenticated()

            // 특정 역할 필요
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/seller/**").hasAnyRole("SELLER", "ADMIN")

            // 그 외 모든 요청
            .anyRequest().authenticated()
        );
        return http.build();
    }
}

// 메서드 레벨 권한
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/users/{id}")
public void deleteUser(@PathVariable Long id) {
    userService.delete(id);
}

@PreAuthorize("#userId == authentication.principal.id or hasRole('ADMIN')")
@GetMapping("/users/{userId}/orders")
public List<Order> getUserOrders(@PathVariable Long userId) {
    return orderService.findByUserId(userId);
}
```

---

## 3. 버전 관리 (Git)

### Git 기본 명령어

```bash
# 저장소 초기화/복제
git init
git clone <url>

# 상태 확인
git status
git log --oneline --graph

# 변경사항 스테이징
git add <file>
git add .

# 커밋
git commit -m "메시지"
git commit --amend  # 마지막 커밋 수정

# 브랜치
git branch                    # 목록
git branch <name>             # 생성
git checkout <branch>         # 전환
git checkout -b <branch>      # 생성 + 전환
git branch -d <branch>        # 삭제

# 원격 저장소
git remote add origin <url>
git push origin <branch>
git pull origin <branch>
git fetch origin

# 병합
git merge <branch>
git rebase <branch>

# 되돌리기
git reset --soft HEAD~1       # 커밋만 취소
git reset --mixed HEAD~1      # 커밋 + 스테이징 취소
git reset --hard HEAD~1       # 모든 변경 취소 (주의!)
git revert <commit>           # 커밋 되돌리기 (새 커밋 생성)
```

### Git Flow vs GitHub Flow

#### Git Flow

```
main ────●────────────●────────────●──────
          \          /            /
hotfix     \────●───/            /
            \                   /
release      \────●────●───────/
              \               /
develop ───●───●───●───●───●───●───●──────
            \     /       \     /
feature      ●───●         ●───●
```

| 브랜치 | 용도 |
|--------|------|
| main | 프로덕션 코드 |
| develop | 개발 통합 브랜치 |
| feature/* | 기능 개발 |
| release/* | 릴리즈 준비 |
| hotfix/* | 긴급 버그 수정 |

#### GitHub Flow (권장 - 단순함)

```
main ────●────●────●────●────●────●──────
          \  /      \  /      \  /
feature    ●         ●         ●
           │         │         │
           PR        PR        PR
```

```bash
# GitHub Flow 워크플로우
# 1. main에서 브랜치 생성
git checkout main
git pull origin main
git checkout -b feature/user-auth

# 2. 작업 및 커밋
git add .
git commit -m "feat: add JWT authentication"

# 3. 원격에 푸시
git push origin feature/user-auth

# 4. Pull Request 생성 (GitHub에서)

# 5. 코드 리뷰 후 머지

# 6. 로컬 정리
git checkout main
git pull origin main
git branch -d feature/user-auth
```

### 커밋 메시지 컨벤션

```
<type>(<scope>): <subject>

<body>

<footer>
```

#### Type 종류

| Type | 설명 |
|------|------|
| feat | 새로운 기능 |
| fix | 버그 수정 |
| docs | 문서 수정 |
| style | 코드 포맷팅 (동작 변경 없음) |
| refactor | 리팩토링 |
| test | 테스트 추가/수정 |
| chore | 빌드, 패키지 등 기타 변경 |

#### 예시

```bash
# 좋은 커밋 메시지
feat(auth): add JWT token refresh mechanism

- Add refresh token rotation
- Store refresh tokens in Redis
- Add token blacklist for logout

Closes #123

# 나쁜 커밋 메시지
fix bug
update code
작업중
```

### Conflict 해결

```bash
# 1. 충돌 발생
git merge feature/other-branch
# CONFLICT (content): Merge conflict in src/User.java

# 2. 충돌 파일 확인
git status

# 3. 파일 열어서 충돌 해결
<<<<<<< HEAD
현재 브랜치 내용
=======
병합하려는 브랜치 내용
>>>>>>> feature/other-branch

# 4. 해결 후 커밋
git add src/User.java
git commit -m "resolve: merge conflict in User.java"
```

---

## 4. 테스트 작성

### 테스트 피라미드

```
        ╱╲
       ╱  ╲         E2E Tests (적음)
      ╱────╲        - 전체 시스템 테스트
     ╱      ╲       - 느리고 비용 높음
    ╱────────╲
   ╱          ╲     Integration Tests (중간)
  ╱────────────╲    - 컴포넌트 간 연동
 ╱              ╲   - DB, 외부 API 포함
╱────────────────╲
       ▲            Unit Tests (많음)
       │            - 개별 함수/클래스
       │            - 빠르고 독립적
```

### 단위 테스트 (Unit Test)

```java
// JUnit 5 + Mockito
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("사용자 조회 - 존재하는 ID로 조회 시 사용자 반환")
    void findById_ExistingId_ReturnsUser() {
        // Given
        Long userId = 1L;
        User expected = new User(userId, "test@example.com", "홍길동");
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(expected));

        // When
        User result = userService.findById(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getName()).isEqualTo("홍길동");
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("사용자 조회 - 존재하지 않는 ID로 조회 시 예외 발생")
    void findById_NonExistingId_ThrowsException() {
        // Given
        Long userId = 999L;
        when(userRepository.findById(userId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.findById(userId))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessageContaining("사용자를 찾을 수 없습니다");
    }
}
```

### 통합 테스트 (Integration Test)

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("POST /api/users - 사용자 생성 성공")
    void createUser_ValidInput_Returns201() throws Exception {
        // Given
        CreateUserRequest request = new CreateUserRequest(
            "test@example.com",
            "password123",
            "홍길동"
        );

        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.name").value("홍길동"))
            .andExpect(jsonPath("$.id").exists());

        // DB 확인
        assertThat(userRepository.findByEmail("test@example.com"))
            .isPresent();
    }

    @Test
    @DisplayName("POST /api/users - 중복 이메일 시 409 반환")
    void createUser_DuplicateEmail_Returns409() throws Exception {
        // Given
        userRepository.save(new User("test@example.com", "홍길동"));

        CreateUserRequest request = new CreateUserRequest(
            "test@example.com",
            "password123",
            "김철수"
        );

        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("DUPLICATE_EMAIL"));
    }
}
```

### 테스트 작성 원칙

```
F.I.R.S.T 원칙

F - Fast (빠르게)
    테스트는 빠르게 실행되어야 함

I - Independent (독립적)
    테스트 간 의존성 없어야 함

R - Repeatable (반복 가능)
    어떤 환경에서도 동일한 결과

S - Self-Validating (자가 검증)
    성공/실패를 명확히 판단

T - Timely (적시에)
    프로덕션 코드 작성 전/후 즉시
```

---

## 5. 실전 프로젝트

### 프로젝트 1: 게시판 API

**기능 요구사항**:
- 사용자 CRUD
- 게시글 CRUD
- 댓글 CRUD
- 페이지네이션
- 검색 기능

**기술 스택**:
- 선택한 언어/프레임워크
- PostgreSQL
- JWT 인증

**API 설계 예시**:

```
# 인증
POST   /api/auth/signup          # 회원가입
POST   /api/auth/login           # 로그인
POST   /api/auth/refresh         # 토큰 갱신

# 사용자
GET    /api/users/me             # 내 정보 조회
PATCH  /api/users/me             # 내 정보 수정

# 게시글
GET    /api/posts                # 목록 (페이지네이션)
GET    /api/posts/{id}           # 상세
POST   /api/posts                # 작성
PUT    /api/posts/{id}           # 수정 (작성자만)
DELETE /api/posts/{id}           # 삭제 (작성자만)

# 댓글
GET    /api/posts/{postId}/comments
POST   /api/posts/{postId}/comments
DELETE /api/comments/{id}
```

### 프로젝트 2: 쇼핑몰 백엔드

**기능 요구사항**:
- 상품 관리
- 장바구니
- 주문/결제
- 주문 상태 관리

**추가 학습 포인트**:
- 트랜잭션 관리
- 동시성 처리 (재고 관리)
- 상태 머신 (주문 상태)

---

## 6. 중급 단계 체크리스트

### RESTful API
- [ ] REST 제약 조건 이해
- [ ] 적절한 HTTP 메서드 사용
- [ ] 상태 코드 올바르게 반환
- [ ] 일관된 응답 형식 설계
- [ ] 페이지네이션 구현
- [ ] 에러 핸들링

### 인증/인가
- [ ] Session vs JWT 차이 이해
- [ ] JWT 토큰 발급/검증 구현
- [ ] Refresh Token 전략
- [ ] 권한 기반 접근 제어
- [ ] OAuth 2.0 흐름 이해

### Git
- [ ] 브랜치 전략 이해 및 적용
- [ ] 커밋 메시지 컨벤션 준수
- [ ] Pull Request 프로세스
- [ ] Conflict 해결

### 테스트
- [ ] 단위 테스트 작성
- [ ] Mock 객체 활용
- [ ] 통합 테스트 작성
- [ ] 테스트 커버리지 이해

### 실전 프로젝트
- [ ] 게시판 API 완성
- [ ] JWT 인증 적용
- [ ] API 문서화 (Swagger/OpenAPI)
- [ ] GitHub에 코드 공개

---

*마지막 업데이트: 2026년 01월*
