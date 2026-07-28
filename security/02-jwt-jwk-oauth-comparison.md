# JWT, JWK, OAuth 2.0 개념과 차이점 비교

인증(Authentication)과 인가(Authorization)를 구현할 때 자주 등장하는 JWT, JWK, OAuth 2.0의 개념과 차이점을 정리한다.

## 목차

- [개념 정리](#개념-정리)
  - [JWT (JSON Web Token)](#jwt-json-web-token)
  - [JWK (JSON Web Key)](#jwk-json-web-key)
  - [OAuth 2.0](#oauth-20)
- [핵심 차이점 비교](#핵심-차이점-비교)
- [사용 사례](#사용-사례)
- [보안 고려사항](#보안-고려사항)
- [함께 사용하는 방법](#함께-사용하는-방법)

---

## 개념 정리

### JWT (JSON Web Token)

**JWT**는 두 당사자 간에 정보를 안전하게 전송하기 위한 **토큰 형식(Format)**이다.

#### 구조

JWT는 점(`.`)으로 구분된 세 부분으로 구성된다:

```
xxxxx.yyyyy.zzzzz
Header.Payload.Signature
```

| 구성 요소 | 설명 | 예시 |
|----------|------|------|
| **Header** | 토큰 타입과 서명 알고리즘 | `{"alg": "RS256", "typ": "JWT"}` |
| **Payload** | 클레임(Claims) - 전달할 데이터 | `{"sub": "1234", "name": "John", "exp": 1234567890}` |
| **Signature** | 토큰 위변조 검증용 서명 | Header + Payload를 비밀키로 서명 |

#### 주요 클레임(Claims)

```json
{
  "iss": "https://auth.example.com",  // Issuer: 토큰 발급자
  "sub": "user123",                    // Subject: 토큰 대상자
  "aud": "https://api.example.com",   // Audience: 토큰 수신자
  "exp": 1735689600,                  // Expiration: 만료 시간
  "iat": 1735686000,                  // Issued At: 발급 시간
  "jti": "unique-token-id"            // JWT ID: 고유 식별자
}
```

---

### JWK (JSON Web Key)

**JWK**는 암호화 키를 JSON 형식으로 표현하는 **표준 방식**이다. JWT 서명 검증에 필요한 공개키를 배포할 때 주로 사용된다.

#### JWK 예시 (RSA 공개키)

```json
{
  "kty": "RSA",
  "kid": "my-key-id",
  "use": "sig",
  "alg": "RS256",
  "n": "0vx7agoebGcQSuuPiLJXZpt...",
  "e": "AQAB"
}
```

| 필드 | 설명 |
|------|------|
| `kty` | Key Type - 키 종류 (RSA, EC, oct 등) |
| `kid` | Key ID - 키 식별자 |
| `use` | 용도 (sig: 서명, enc: 암호화) |
| `alg` | 알고리즘 (RS256, ES256 등) |
| `n`, `e` | RSA 공개키 구성 요소 |

#### JWKS (JWK Set)

여러 JWK를 묶어서 제공하는 형식. 보통 `/.well-known/jwks.json` 엔드포인트로 제공된다.

```json
{
  "keys": [
    { "kty": "RSA", "kid": "key-1", ... },
    { "kty": "RSA", "kid": "key-2", ... }
  ]
}
```

---

### OAuth 2.0

**OAuth 2.0**은 리소스 접근 권한을 위임하기 위한 **인가 프레임워크(Authorization Framework)**이다.

#### 주요 역할

| 역할 | 설명 | 예시 |
|------|------|------|
| **Resource Owner** | 리소스 소유자 (사용자) | 일반 사용자 |
| **Client** | 리소스에 접근하려는 애플리케이션 | 웹/모바일 앱 |
| **Authorization Server** | 인증/토큰 발급 서버 | Google, Kakao 인증 서버 |
| **Resource Server** | 보호된 리소스를 가진 서버 | API 서버 |

#### 주요 Grant Types

| Grant Type | 사용 상황 | 보안 수준 |
|------------|----------|----------|
| **Authorization Code** | 서버 사이드 앱 (가장 안전) | 높음 |
| **Authorization Code + PKCE** | SPA, 모바일 앱 | 높음 |
| **Client Credentials** | 서버 간 통신 (사용자 없음) | 중간 |
| **Refresh Token** | 액세스 토큰 갱신 | - |
| ~~Implicit~~ | (더 이상 권장하지 않음) | 낮음 |
| ~~Password~~ | (더 이상 권장하지 않음) | 낮음 |

#### Authorization Code Flow

```
┌──────────┐                              ┌─────────────────────┐
│  User    │                              │ Authorization Server│
└────┬─────┘                              └──────────┬──────────┘
     │                                               │
     │  1. 로그인 요청                                │
     ├──────────────────────────────────────────────>│
     │                                               │
     │  2. 로그인 페이지 표시                         │
     │<──────────────────────────────────────────────┤
     │                                               │
     │  3. 자격 증명 제출                             │
     ├──────────────────────────────────────────────>│
     │                                               │
     │  4. Authorization Code 반환                   │
     │<──────────────────────────────────────────────┤
     │                                               │
┌────┴─────┐                                         │
│  Client  │  5. Code + Client Secret으로 토큰 요청   │
│  Server  ├────────────────────────────────────────>│
│          │                                         │
│          │  6. Access Token (+ Refresh Token) 반환 │
│          │<────────────────────────────────────────┤
└──────────┘
```

---

## 핵심 차이점 비교

| 구분 | JWT | JWK | OAuth 2.0 |
|------|-----|-----|-----------|
| **무엇인가?** | 토큰 형식 (Format) | 키 표현 방식 (Key Format) | 인가 프레임워크 (Framework) |
| **역할** | 정보를 안전하게 전달 | 암호화 키를 JSON으로 표현 | 권한 위임 절차 정의 |
| **레이어** | 데이터 포맷 | 키 관리 | 프로토콜/플로우 |
| **단독 사용** | 가능 | 불가 (JWT/JWE와 함께) | 가능 |
| **표준** | RFC 7519 | RFC 7517 | RFC 6749 |

### 관계도

```
┌─────────────────────────────────────────────────────────────┐
│                       OAuth 2.0                              │
│                    (인가 프레임워크)                           │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐   │
│   │              Access Token으로 JWT 사용               │   │
│   │                                                     │   │
│   │   ┌──────────────┐        ┌──────────────┐         │   │
│   │   │     JWT      │        │     JWK      │         │   │
│   │   │  (토큰 형식)  │◄───────│  (키 표현)    │         │   │
│   │   │              │  검증   │              │         │   │
│   │   └──────────────┘        └──────────────┘         │   │
│   └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## 사용 사례

### JWT 사용 사례

| 사례 | 설명 |
|------|------|
| **Stateless 인증** | 서버에 세션 저장 없이 토큰만으로 인증 |
| **마이크로서비스 간 통신** | 서비스 간 사용자 정보 전달 |
| **Single Sign-On (SSO)** | 여러 서비스에서 동일 토큰 사용 |
| **API 인증** | Bearer 토큰으로 API 접근 |

### JWK 사용 사례

| 사례 | 설명 |
|------|------|
| **공개키 배포** | JWKS 엔드포인트로 JWT 검증용 공개키 제공 |
| **키 로테이션** | kid로 여러 키 버전 관리 |
| **분산 시스템** | 여러 서비스가 동일 공개키로 JWT 검증 |

### OAuth 2.0 사용 사례

| 사례 | 설명 |
|------|------|
| **소셜 로그인** | Google, Kakao, Naver 로그인 |
| **써드파티 API 접근** | 사용자 대신 외부 서비스 접근 |
| **권한 범위 제어** | scope로 세밀한 권한 관리 |

---

## 보안 고려사항

### JWT 보안

| 주의점 | 권장사항 |
|--------|----------|
| **alg: none 공격** | 서버에서 허용 알고리즘 명시적 지정 |
| **토큰 탈취** | 짧은 만료 시간 설정 (15분~1시간) |
| **민감 정보 노출** | Payload에 민감 정보 저장 금지 (Base64 디코딩 가능) |
| **토큰 무효화** | Refresh Token + Token Blacklist 사용 |
| **알고리즘 선택** | RS256 또는 ES256 권장 (HS256보다 안전) |

### JWK 보안

| 주의점 | 권장사항 |
|--------|----------|
| **키 노출** | 공개키만 JWKS에 포함, 비밀키는 절대 노출 금지 |
| **키 로테이션** | 정기적인 키 교체, 이전 키는 일정 기간 유지 |
| **HTTPS 필수** | JWKS 엔드포인트는 반드시 HTTPS 사용 |

### OAuth 2.0 보안

| 주의점 | 권장사항 |
|--------|----------|
| **Authorization Code 탈취** | PKCE 사용 필수 (특히 public client) |
| **Redirect URI 조작** | 정확한 URI 화이트리스트 등록 |
| **CSRF 공격** | state 파라미터 사용 |
| **Token 저장** | Access Token: 메모리, Refresh Token: HttpOnly Cookie |

---

## 함께 사용하는 방법

실제 시스템에서는 세 가지를 조합하여 사용한다:

```
1. 사용자가 OAuth 2.0 Authorization Code Flow로 로그인
                    ↓
2. Authorization Server가 Access Token을 JWT 형식으로 발급
                    ↓
3. Resource Server가 JWKS 엔드포인트에서 JWK를 가져와 JWT 검증
                    ↓
4. 검증 성공 시 리소스 접근 허용
```

### 실제 구현 예시 (Spring Security)

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    // JWKS 엔드포인트에서 공개키를 가져와 JWT 검증
                    .jwkSetUri("https://auth.example.com/.well-known/jwks.json")
                )
            );
        return http.build();
    }
}
```

---

## 요약

| 개념 | 한 줄 정리 |
|------|-----------|
| **JWT** | JSON 기반의 자가 포함(self-contained) 토큰 형식 |
| **JWK** | 암호화 키를 JSON으로 표현하는 표준 |
| **OAuth 2.0** | 권한 위임을 위한 인가 프레임워크 |

세 개념은 서로 다른 레이어에서 동작하며, 현대 인증 시스템에서는 이들을 조합하여 사용한다:
- **OAuth 2.0**으로 인가 플로우를 정의하고
- **JWT**를 토큰 형식으로 사용하며
- **JWK**로 토큰 검증에 필요한 키를 배포한다

*마지막 업데이트: 2025년 12월*
