# 백엔드 개발자를 위한 보안 기초

인증, 인가, 암호화 등 백엔드 개발자가 반드시 알아야 할 보안 개념과 실무 예제를 정리합니다.

## 목차

- [인증 vs 인가](#인증-vs-인가)
- [인증 방식](#인증-방식)
- [JWT 인증](#jwt-인증)
- [OAuth 2.0](#oauth-20)
- [암호화 기초](#암호화-기초)
- [비밀번호 저장](#비밀번호-저장)
- [HTTPS와 TLS](#https와-tls)
- [보안 헤더](#보안-헤더)
- [주요 취약점과 대응](#주요-취약점과-대응)

---

## 인증 vs 인가

| 구분 | 인증 (Authentication) | 인가 (Authorization) |
|------|----------------------|---------------------|
| 질문 | "너 누구야?" | "너 이거 해도 돼?" |
| 확인 대상 | 신원 (Identity) | 권한 (Permission) |
| 시점 | 먼저 수행 | 인증 후 수행 |
| 실패 응답 | 401 Unauthorized | 403 Forbidden |
| 예시 | 로그인 | 관리자 페이지 접근 |

```java
@RestController
@RequestMapping("/api")
public class UserController {

    // 인증 실패 → 401
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(user);
    }

    // 인가 실패 → 403
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminPage() {
        return ResponseEntity.ok("Admin Page");
    }
}
```

---

## 인증 방식

### 1. 세션 기반 인증

```
클라이언트                    서버
    |--- 로그인 요청 -------->|
    |<-- Set-Cookie: SID --- |  (세션 저장소에 저장)
    |                        |
    |--- Cookie: SID ------->|  (세션 조회)
    |<-- 응답 --------------|
```

```java
@RestController
public class SessionAuthController {

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpSession session) {

        User user = userService.authenticate(request);
        session.setAttribute("USER", user);
        session.setMaxInactiveInterval(30 * 60); // 30분

        return ResponseEntity.ok("Login Success");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("Logout Success");
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        User user = (User) session.getAttribute("USER");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(user);
    }
}
```

**분산 환경 세션 공유 (Redis)**

```java
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
public class SessionConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory("localhost", 6379);
    }
}
```

### 2. 토큰 기반 인증 (Stateless)

```
클라이언트                         서버
    |--- 로그인 요청 ------------->|
    |<-- Access Token + Refresh --|  (서버에 저장 안함)
    |                             |
    |--- Authorization: Bearer ---|  (토큰 검증)
    |<-- 응답 -------------------|
```

| 구분 | 세션 기반 | 토큰 기반 |
|------|----------|----------|
| 상태 | Stateful | Stateless |
| 저장 위치 | 서버 (메모리/DB) | 클라이언트 |
| 확장성 | 세션 클러스터링 필요 | 우수 |
| 보안 | 세션 탈취 위험 | 토큰 탈취 위험 |

---

## JWT 인증

### JWT 구조

```
Header.Payload.Signature
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIn0.xxxxx

Header:  {"alg": "HS256", "typ": "JWT"}
Payload: {"sub": "user", "exp": 1234567890, "roles": ["USER"]}
Signature: HMACSHA256(base64(header) + "." + base64(payload), secret)
```

### JWT 생성/검증

```java
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiry}")
    private long accessTokenExpiry;  // 30분

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpiry; // 7일

    private Key key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // Access Token 생성
    public String createAccessToken(String userId, List<String> roles) {
        Claims claims = Jwts.claims().setSubject(userId);
        claims.put("roles", roles);
        claims.put("type", "access");

        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiry);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // Refresh Token 생성
    public String createRefreshToken(String userId) {
        Claims claims = Jwts.claims().setSubject(userId);
        claims.put("type", "refresh");

        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpiry);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 토큰 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException("토큰이 만료되었습니다");
        } catch (JwtException e) {
            throw new InvalidTokenException("유효하지 않은 토큰입니다");
        }
    }

    // 토큰에서 사용자 정보 추출
    public String getUserId(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return (List<String>) claims.get("roles");
    }
}
```

### JWT 필터

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            String userId = jwtTokenProvider.getUserId(token);
            List<String> roles = jwtTokenProvider.getRoles(token);

            List<GrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
```

### Refresh Token 전략

```java
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @PostMapping("/auth/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        User user = userService.authenticate(request);

        String accessToken = tokenProvider.createAccessToken(user.getId(), user.getRoles());
        String refreshToken = tokenProvider.createRefreshToken(user.getId());

        // Refresh Token은 DB/Redis에 저장
        refreshTokenRepository.save(new RefreshToken(user.getId(), refreshToken));

        return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        // 1. Refresh Token 유효성 검증
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("유효하지 않은 Refresh Token");
        }

        // 2. DB에 저장된 토큰과 비교
        String userId = tokenProvider.getUserId(refreshToken);
        RefreshToken saved = refreshTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new InvalidTokenException("Refresh Token이 존재하지 않습니다"));

        if (!saved.getToken().equals(refreshToken)) {
            // 토큰 재사용 감지 → 모든 토큰 무효화
            refreshTokenRepository.deleteByUserId(userId);
            throw new TokenReusedException("Refresh Token 재사용이 감지되었습니다");
        }

        // 3. 새 토큰 발급 (Refresh Token Rotation)
        User user = userService.findById(userId);
        String newAccessToken = tokenProvider.createAccessToken(user.getId(), user.getRoles());
        String newRefreshToken = tokenProvider.createRefreshToken(user.getId());

        refreshTokenRepository.save(new RefreshToken(userId, newRefreshToken));

        return ResponseEntity.ok(new TokenResponse(newAccessToken, newRefreshToken));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal String userId) {
        refreshTokenRepository.deleteByUserId(userId);
        return ResponseEntity.ok().build();
    }
}
```

---

## OAuth 2.0

### Grant Types

| Grant Type | 용도 | 특징 |
|------------|------|------|
| Authorization Code | 웹 앱 | 가장 안전, 서버에서 토큰 교환 |
| PKCE | SPA, 모바일 | Auth Code + Code Verifier |
| Client Credentials | 서버 간 통신 | 사용자 없이 앱 자체 인증 |
| ~~Implicit~~ | ~~SPA~~ | 보안 취약, 사용 금지 |
| ~~Password~~ | ~~1st party~~ | 보안 취약, 사용 금지 |

### Authorization Code Flow

```
1. 사용자 → 클라이언트: 로그인 버튼 클릭
2. 클라이언트 → 인증 서버: /authorize?response_type=code&client_id=...
3. 인증 서버 → 사용자: 로그인 페이지
4. 사용자 → 인증 서버: 로그인 정보 입력
5. 인증 서버 → 클라이언트: redirect_uri?code=AUTH_CODE
6. 클라이언트(서버) → 인증 서버: POST /token (code + client_secret)
7. 인증 서버 → 클라이언트(서버): Access Token + Refresh Token
```

### Spring Security OAuth2 설정

```yaml
# application.yml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: email, profile
          kakao:
            client-id: ${KAKAO_CLIENT_ID}
            client-secret: ${KAKAO_CLIENT_SECRET}
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: profile_nickname, account_email
        provider:
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id
```

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2UserService oAuth2UserService;
    private final OAuth2SuccessHandler successHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oAuth2UserService))
                .successHandler(successHandler)
            )
            .build();
    }
}
```

```java
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttribute = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // OAuth2 제공자별 사용자 정보 추출
        OAuthAttributes attributes = OAuthAttributes.of(
                registrationId, userNameAttribute, oAuth2User.getAttributes());

        // 사용자 저장 또는 업데이트
        User user = saveOrUpdate(attributes);

        return new CustomOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole())),
                attributes.getAttributes(),
                attributes.getNameAttributeKey(),
                user);
    }

    private User saveOrUpdate(OAuthAttributes attributes) {
        User user = userRepository.findByEmail(attributes.getEmail())
                .map(entity -> entity.update(attributes.getName(), attributes.getPicture()))
                .orElse(attributes.toEntity());

        return userRepository.save(user);
    }
}
```

---

## 암호화 기초

### 대칭키 vs 비대칭키

| 구분 | 대칭키 (AES) | 비대칭키 (RSA) |
|------|-------------|---------------|
| 키 | 암/복호화 동일 키 | 공개키/개인키 쌍 |
| 속도 | 빠름 | 느림 |
| 용도 | 데이터 암호화 | 키 교환, 서명 |
| 키 배포 | 안전한 채널 필요 | 공개키는 공개 가능 |

### AES 암호화 (대칭키)

```java
@Component
public class AesEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    @Value("${encryption.aes.key}")
    private String secretKey;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    // 암호화
    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // IV + 암호문
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new EncryptionException("암호화 실패", e);
        }
    }

    // 복호화
    public String decrypt(String cipherText) {
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];

            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new EncryptionException("복호화 실패", e);
        }
    }
}
```

### RSA 암호화 (비대칭키)

```java
@Component
public class RsaEncryptor {

    private PublicKey publicKey;
    private PrivateKey privateKey;

    @PostConstruct
    public void init() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        this.publicKey = keyPair.getPublic();
        this.privateKey = keyPair.getPrivate();
    }

    // 공개키로 암호화
    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new EncryptionException("RSA 암호화 실패", e);
        }
    }

    // 개인키로 복호화
    public String decrypt(String cipherText) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new EncryptionException("RSA 복호화 실패", e);
        }
    }
}
```

### JPA AttributeConverter로 자동 암호화

```java
@Converter
@RequiredArgsConstructor
public class EncryptConverter implements AttributeConverter<String, String> {

    private final AesEncryptor aesEncryptor;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        return aesEncryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return aesEncryptor.decrypt(dbData);
    }
}

@Entity
public class User {

    @Id @GeneratedValue
    private Long id;

    private String name;

    @Convert(converter = EncryptConverter.class)
    private String phoneNumber;  // 자동 암호화/복호화

    @Convert(converter = EncryptConverter.class)
    private String ssn;  // 주민등록번호
}
```

---

## 비밀번호 저장

### 해시 vs 암호화

| 구분 | 해시 (Hash) | 암호화 (Encryption) |
|------|------------|-------------------|
| 방향 | 단방향 | 양방향 |
| 복원 | 불가능 | 가능 |
| 용도 | 비밀번호 저장 | 데이터 보호 |
| 알고리즘 | BCrypt, Argon2 | AES, RSA |

### BCrypt (권장)

```java
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 기본 strength: 10 (2^10 = 1024 라운드)
        return new BCryptPasswordEncoder();
    }
}

@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public void register(RegisterRequest request) {
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)  // $2a$10$xxxxx...
                .build();

        userRepository.save(user);
    }

    public void login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("사용자를 찾을 수 없습니다"));

        // 비밀번호 검증 (해시 비교)
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException("비밀번호가 일치하지 않습니다");
        }
    }
}
```

### Argon2 (더 강력)

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new Argon2PasswordEncoder(
        16,     // saltLength
        32,     // hashLength
        1,      // parallelism
        65536,  // memory (64MB)
        3       // iterations
    );
}
```

### 비밀번호 정책

```java
@Component
public class PasswordValidator {

    // 최소 8자, 대문자, 소문자, 숫자, 특수문자 포함
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    public void validate(String password) {
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new InvalidPasswordException(
                "비밀번호는 8자 이상, 대소문자, 숫자, 특수문자를 포함해야 합니다"
            );
        }
    }
}
```

---

## HTTPS와 TLS

### TLS 핸드셰이크

```
클라이언트                           서버
    |--- Client Hello ------------->|  (지원 암호화 방식)
    |<-- Server Hello --------------|  (선택된 암호화 방식)
    |<-- 서버 인증서 ---------------|  (공개키 포함)
    |--- 키 교환 ------------------>|  (대칭키 생성)
    |<-- 암호화 통신 시작 ----------|
```

### Spring Boot HTTPS 설정

```yaml
# application.yml
server:
  port: 443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_PASSWORD}
    key-store-type: PKCS12
    key-alias: tomcat

# HTTP → HTTPS 리다이렉트
  http:
    port: 80
```

```java
@Configuration
public class HttpsConfig {

    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                SecurityConstraint securityConstraint = new SecurityConstraint();
                securityConstraint.setUserConstraint("CONFIDENTIAL");
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                securityConstraint.addCollection(collection);
                context.addConstraint(securityConstraint);
            }
        };
        tomcat.addAdditionalTomcatConnectors(httpConnector());
        return tomcat;
    }

    private Connector httpConnector() {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(80);
        connector.setSecure(false);
        connector.setRedirectPort(443);
        return connector;
    }
}
```

---

## 보안 헤더

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .headers(headers -> headers
                // XSS 방지
                .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                // 클릭재킹 방지
                .frameOptions(frame -> frame.deny())
                // MIME 타입 스니핑 방지
                .contentTypeOptions(Customizer.withDefaults())
                // HTTPS 강제
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
                // CSP
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self'"))
            )
            .build();
    }
}
```

| 헤더 | 값 | 설명 |
|------|---|------|
| X-Content-Type-Options | nosniff | MIME 스니핑 방지 |
| X-Frame-Options | DENY | 클릭재킹 방지 |
| X-XSS-Protection | 1; mode=block | XSS 필터 |
| Strict-Transport-Security | max-age=31536000 | HTTPS 강제 |
| Content-Security-Policy | default-src 'self' | 리소스 로드 제한 |

---

## 주요 취약점과 대응

### SQL Injection

```java
// 취약한 코드 ❌
String query = "SELECT * FROM users WHERE id = '" + userId + "'";
// 입력: ' OR '1'='1
// 결과: SELECT * FROM users WHERE id = '' OR '1'='1'

// 안전한 코드 ✅
@Query("SELECT u FROM User u WHERE u.id = :id")
Optional<User> findById(@Param("id") Long id);

// 또는 JdbcTemplate
jdbcTemplate.queryForObject(
    "SELECT * FROM users WHERE id = ?",
    new Object[]{userId},
    userRowMapper
);
```

### XSS (Cross-Site Scripting)

```java
// 취약한 코드 ❌
@GetMapping("/user")
public String user(@RequestParam String name, Model model) {
    model.addAttribute("name", name);  // <script>alert('xss')</script>
    return "user";  // HTML에서 그대로 출력
}

// 안전한 코드 ✅
@GetMapping("/user")
public String user(@RequestParam String name, Model model) {
    String sanitized = HtmlUtils.htmlEscape(name);
    model.addAttribute("name", sanitized);
    return "user";
}

// Thymeleaf 자동 이스케이프 (기본 활성화)
// <p th:text="${name}"></p>  ← 자동 이스케이프
// <p th:utext="${name}"></p> ← 이스케이프 안함 (주의!)
```

### CSRF (Cross-Site Request Forgery)

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            // REST API는 CSRF 비활성화 (토큰 인증 사용 시)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")
                // 또는 완전 비활성화
                // .disable()
            )
            // 웹 폼은 CSRF 토큰 사용
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            )
            .build();
    }
}
```

```html
<!-- Thymeleaf CSRF 토큰 자동 포함 -->
<form th:action="@{/transfer}" method="post">
    <!-- 자동 추가: <input type="hidden" name="_csrf" value="..."/> -->
    <button type="submit">전송</button>
</form>
```

### 민감 정보 노출 방지

```java
// 로그에 민감 정보 제외
@Slf4j
public class UserService {

    public void login(LoginRequest request) {
        // ❌ 비밀번호 로깅
        log.info("Login attempt: {}", request);

        // ✅ 민감 정보 마스킹
        log.info("Login attempt: email={}", request.getEmail());
    }
}

// 응답에서 민감 필드 제외
@JsonIgnoreProperties({"password", "ssn"})
public class UserResponse {
    private Long id;
    private String name;
    private String password;  // JSON 변환 시 제외
}

// 또는 별도 DTO 사용
public record UserResponse(Long id, String name, String email) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
```

---

*마지막 업데이트: 2026년 01월*
