# Spring Security 면접 대비

## 목차
1. [Spring Security 개요](#spring-security-개요)
2. [SecurityFilterChain](#securityfilterchain)
3. [인증 (Authentication)](#인증-authentication)
4. [JWT 인증](#jwt-인증)
5. [OAuth2 / OpenID Connect](#oauth2--openid-connect)
6. [핵심 정리](#핵심-정리)

---

## Spring Security 개요

### 핵심 개념

```
┌──────────────────────────────────────────────────────────────────┐
│                   Spring Security 핵심 개념                       │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  인증 (Authentication)                                           │
│  └── "당신은 누구인가?" - 신원 확인                               │
│  └── 로그인 과정                                                  │
│                                                                   │
│  인가 (Authorization)                                            │
│  └── "무엇을 할 수 있는가?" - 권한 확인                           │
│  └── 접근 제어                                                    │
│                                                                   │
│  Principal: 현재 인증된 사용자                                    │
│  GrantedAuthority: 부여된 권한 (ROLE_ADMIN 등)                    │
│  SecurityContext: 인증 정보 저장소                                │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Spring Security 6.x 설정 (Spring Boot 3.x)

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // @PreAuthorize, @PostAuthorize 활성화
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())  // REST API는 보통 비활성화
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

## SecurityFilterChain

### 필터 체인 구조

```
HTTP 요청
    │
    ▼
┌────────────────────────────────────────────────────────────────┐
│                    Security Filter Chain                        │
├────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. SecurityContextPersistenceFilter                            │
│     └── SecurityContext 로드/저장                                │
│                                                                  │
│  2. LogoutFilter                                                │
│     └── 로그아웃 처리                                            │
│                                                                  │
│  3. UsernamePasswordAuthenticationFilter                        │
│     └── Form 로그인 처리                                         │
│                                                                  │
│  4. BasicAuthenticationFilter                                   │
│     └── HTTP Basic 인증                                          │
│                                                                  │
│  5. RequestCacheAwareFilter                                     │
│     └── 요청 캐시 처리                                           │
│                                                                  │
│  6. SecurityContextHolderAwareRequestFilter                     │
│     └── Servlet API 보안 메서드 지원                             │
│                                                                  │
│  7. AnonymousAuthenticationFilter                               │
│     └── 익명 사용자 처리                                         │
│                                                                  │
│  8. SessionManagementFilter                                     │
│     └── 세션 관리                                                │
│                                                                  │
│  9. ExceptionTranslationFilter                                  │
│     └── 보안 예외 처리                                           │
│                                                                  │
│  10. FilterSecurityInterceptor / AuthorizationFilter            │
│      └── 최종 인가 결정                                          │
│                                                                  │
└────────────────────────────────────────────────────────────────┘
    │
    ▼
DispatcherServlet → Controller
```

### 커스텀 필터 추가

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            String username = jwtTokenProvider.getUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

// 필터 등록
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        // ... 기타 설정
        .addFilterBefore(jwtAuthenticationFilter,
                         UsernamePasswordAuthenticationFilter.class)
        .build();
}
```

---

## 인증 (Authentication)

### 인증 아키텍처

```
┌──────────────────────────────────────────────────────────────────┐
│                    인증 처리 흐름                                 │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  1. 사용자 요청 (username/password)                              │
│          │                                                        │
│          ▼                                                        │
│  2. AuthenticationFilter                                         │
│     └── UsernamePasswordAuthenticationToken 생성                 │
│          │                                                        │
│          ▼                                                        │
│  3. AuthenticationManager                                        │
│     └── authenticate() 호출                                       │
│          │                                                        │
│          ▼                                                        │
│  4. AuthenticationProvider                                       │
│     └── 실제 인증 수행                                            │
│          │                                                        │
│          ▼                                                        │
│  5. UserDetailsService                                           │
│     └── loadUserByUsername() - DB에서 사용자 조회                 │
│          │                                                        │
│          ▼                                                        │
│  6. PasswordEncoder                                              │
│     └── 비밀번호 검증                                             │
│          │                                                        │
│          ▼                                                        │
│  7. 인증 성공 → SecurityContext에 저장                           │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### UserDetailsService 구현

```java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPassword())  // 암호화된 비밀번호
            .authorities(user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList()))
            .accountExpired(false)
            .accountLocked(user.isLocked())
            .credentialsExpired(false)
            .disabled(!user.isEnabled())
            .build();
    }
}

// 또는 UserDetails를 직접 구현
@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.user = user;
        this.authorities = user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
            .collect(Collectors.toList());
    }

    @Override
    public String getUsername() { return user.getEmail(); }

    @Override
    public String getPassword() { return user.getPassword(); }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return !user.isLocked(); }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return user.isEnabled(); }
}
```

### 메서드 레벨 보안

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    // 역할 기반 접근 제어
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // 표현식 기반 접근 제어
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    // 결과 필터링
    @PostAuthorize("returnObject.email == authentication.principal.username")
    @GetMapping("/me")
    public UserDto getCurrentUser() {
        return userService.getCurrentUser();
    }

    // 컬렉션 필터링
    @PostFilter("filterObject.owner == authentication.principal.username")
    @GetMapping("/documents")
    public List<Document> getDocuments() {
        return documentService.findAll();
    }
}
```

---

## JWT 인증

### JWT 구조

```
┌──────────────────────────────────────────────────────────────────┐
│                        JWT 구조                                   │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Header.Payload.Signature                                        │
│                                                                   │
│  eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.                          │
│  eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwicm9sZXMiOlsiVVNFUiJdfQ.    │
│  SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c                     │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ Header (Base64)                                              │ │
│  │ {"alg": "HS256", "typ": "JWT"}                              │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ Payload (Base64)                                             │ │
│  │ {"sub": "user@example.com", "roles": ["USER"], "exp": ...}  │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ Signature                                                    │ │
│  │ HMACSHA256(base64(header) + "." + base64(payload), secret)  │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### JWT 토큰 프로바이더

```java
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-validity}")
    private long accessTokenValidity;  // 예: 30분

    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidity;  // 예: 7일

    private SecretKey key;

    @PostConstruct
    protected void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String username, List<String> roles) {
        return createToken(username, roles, accessTokenValidity);
    }

    public String createRefreshToken(String username) {
        return createToken(username, Collections.emptyList(), refreshTokenValidity);
    }

    private String createToken(String username, List<String> roles, long validity) {
        Claims claims = Jwts.claims().setSubject(username);
        claims.put("roles", roles);

        Date now = new Date();
        Date expiration = new Date(now.getTime() + validity);

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token");
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }

    public String getUsername(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        return (List<String>) Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody()
            .get("roles");
    }
}
```

### 인증 컨트롤러

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        // 인증
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(
            userDetails.getUsername(),
            userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList())
        );

        String refreshToken = jwtTokenProvider.createRefreshToken(userDetails.getUsername());

        // Refresh Token 저장 (Redis 또는 DB)
        refreshTokenService.save(userDetails.getUsername(), refreshToken);

        return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Refresh Token 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        String username = jwtTokenProvider.getUsername(refreshToken);

        // 저장된 Refresh Token과 비교
        if (!refreshTokenService.validate(username, refreshToken)) {
            throw new InvalidTokenException("Refresh token mismatch");
        }

        // 새 Access Token 발급
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        String newAccessToken = jwtTokenProvider.createAccessToken(
            username,
            userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList())
        );

        // Refresh Token Rotation (선택적)
        String newRefreshToken = jwtTokenProvider.createRefreshToken(username);
        refreshTokenService.save(username, newRefreshToken);

        return ResponseEntity.ok(new TokenResponse(newAccessToken, newRefreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails userDetails) {
        refreshTokenService.delete(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
```

---

## OAuth2 / OpenID Connect

### OAuth2 흐름

```
┌──────────────────────────────────────────────────────────────────┐
│                  OAuth2 Authorization Code Flow                   │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  User          Client App         Auth Server       Resource     │
│   │                │                   │             Server      │
│   │  1. 로그인 요청  │                   │                │        │
│   │───────────────►│                   │                │        │
│   │                │                   │                │        │
│   │  2. 리다이렉트   │                   │                │        │
│   │◄───────────────│                   │                │        │
│   │                │                   │                │        │
│   │  3. 로그인 페이지 ────────────────► │                │        │
│   │                │                   │                │        │
│   │  4. 로그인 + 동의 ────────────────► │                │        │
│   │                │                   │                │        │
│   │  5. Authorization Code             │                │        │
│   │◄──────────────────────────────────│                │        │
│   │                │                   │                │        │
│   │  6. Code 전달   │                   │                │        │
│   │───────────────►│                   │                │        │
│   │                │                   │                │        │
│   │                │  7. Code → Token  │                │        │
│   │                │──────────────────►│                │        │
│   │                │                   │                │        │
│   │                │  8. Access Token  │                │        │
│   │                │◄──────────────────│                │        │
│   │                │                   │                │        │
│   │                │  9. API 호출 ──────────────────────►│        │
│   │                │                   │                │        │
│   │                │  10. 리소스 ◄──────────────────────│        │
│   │                │                   │                │        │
│   │  11. 응답      │                   │                │        │
│   │◄───────────────│                   │                │        │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Spring Security OAuth2 Client 설정

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
            scope: openid, profile, email

          kakao:
            client-id: ${KAKAO_CLIENT_ID}
            client-secret: ${KAKAO_CLIENT_SECRET}
            client-authentication-method: client_secret_post
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: profile_nickname, profile_image, account_email

        provider:
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id
```

### OAuth2 사용자 서비스

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
            .getProviderDetails()
            .getUserInfoEndpoint()
            .getUserNameAttributeName();

        OAuthAttributes attributes = OAuthAttributes.of(
            registrationId, userNameAttributeName, oauth2User.getAttributes());

        User user = saveOrUpdate(attributes, registrationId);

        return new CustomOAuth2User(
            Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
            oauth2User.getAttributes(),
            userNameAttributeName,
            user
        );
    }

    private User saveOrUpdate(OAuthAttributes attributes, String provider) {
        User user = userRepository.findByEmailAndProvider(attributes.getEmail(), provider)
            .map(entity -> entity.update(attributes.getName(), attributes.getPicture()))
            .orElse(attributes.toEntity(provider));

        return userRepository.save(user);
    }
}

// OAuthAttributes - 플랫폼별 응답 파싱
@Getter
public class OAuthAttributes {
    private String name;
    private String email;
    private String picture;
    private Map<String, Object> attributes;

    public static OAuthAttributes of(String registrationId, String userNameAttributeName,
                                     Map<String, Object> attributes) {
        if ("kakao".equals(registrationId)) {
            return ofKakao(userNameAttributeName, attributes);
        } else if ("naver".equals(registrationId)) {
            return ofNaver(userNameAttributeName, attributes);
        }
        return ofGoogle(userNameAttributeName, attributes);
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName,
                                            Map<String, Object> attributes) {
        return OAuthAttributes.builder()
            .name((String) attributes.get("name"))
            .email((String) attributes.get("email"))
            .picture((String) attributes.get("picture"))
            .attributes(attributes)
            .build();
    }

    @SuppressWarnings("unchecked")
    private static OAuthAttributes ofKakao(String userNameAttributeName,
                                           Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        return OAuthAttributes.builder()
            .name((String) profile.get("nickname"))
            .email((String) kakaoAccount.get("email"))
            .picture((String) profile.get("profile_image_url"))
            .attributes(attributes)
            .build();
    }
}
```

### OAuth2 + JWT 통합

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/oauth2/**", "/login/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo ->
                    userInfo.userService(customOAuth2UserService))
                .successHandler(oAuth2SuccessHandler)
            )
            .addFilterBefore(jwtAuthenticationFilter,
                             UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}

// OAuth2 성공 핸들러 - JWT 발급
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        String accessToken = jwtTokenProvider.createAccessToken(
            oAuth2User.getEmail(),
            List.of("ROLE_USER")
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(oAuth2User.getEmail());

        refreshTokenService.save(oAuth2User.getEmail(), refreshToken);

        // 프론트엔드로 토큰 전달 (쿼리 파라미터 또는 쿠키)
        String redirectUrl = String.format(
            "http://localhost:3000/oauth/callback?accessToken=%s&refreshToken=%s",
            accessToken, refreshToken);

        response.sendRedirect(redirectUrl);
    }
}
```

---

## 핵심 정리

### 인증 방식 비교

| 방식 | 장점 | 단점 | 사용 사례 |
|------|------|------|----------|
| Session | 서버 제어 용이 | 확장성 제한, 상태 관리 | 전통적 웹앱 |
| JWT | Stateless, 확장 용이 | 토큰 무효화 어려움 | REST API, MSA |
| OAuth2 | 표준화, 소셜 로그인 | 복잡한 흐름 | 서드파티 연동 |

### Security 설정 체크리스트

```
□ CSRF: REST API는 비활성화, 폼 기반은 활성화
□ CORS: 필요한 Origin만 허용
□ Session: Stateless API는 STATELESS 정책
□ 비밀번호: BCryptPasswordEncoder 사용
□ JWT: 짧은 만료 + Refresh Token 사용
□ 민감 정보: @Value + 환경 변수로 관리
□ HTTPS: 프로덕션 필수
```

### 면접 대비 핵심 질문

1. **Q: Spring Security 필터 체인의 동작 방식은?**
   - A: 서블릿 필터 기반. 요청이 들어오면 SecurityFilterChain의 필터들이 순서대로 실행. 인증 → 세션 → 예외처리 → 인가 순. 커스텀 필터는 addFilterBefore/After로 특정 위치에 삽입

2. **Q: JWT의 장단점과 보안 고려사항은?**
   - A: 장점은 Stateless, 확장성. 단점은 토큰 무효화 어려움, 토큰 탈취 위험. 보안: 짧은 만료 시간, Refresh Token Rotation, HTTPS 필수, HttpOnly 쿠키 저장

3. **Q: OAuth2 Authorization Code Flow를 설명해주세요**
   - A: 사용자가 인증 서버에서 로그인 → Authorization Code 발급 → 클라이언트가 Code로 Token 교환 → Token으로 리소스 접근. Code는 일회용이고 Back-channel에서 Token 교환하므로 안전

4. **Q: @PreAuthorize vs @Secured의 차이점은?**
   - A: @Secured는 단순 역할만 확인 가능. @PreAuthorize는 SpEL로 복잡한 표현식 가능 (파라미터 접근, 메서드 호출 등). @PreAuthorize가 더 유연

---

*마지막 업데이트: 2026년 01월*
