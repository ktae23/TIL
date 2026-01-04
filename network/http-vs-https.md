# HTTP vs HTTPS 완벽 가이드

웹 통신의 근간이 되는 HTTP와 HTTPS의 차이점, 동작 원리, 보안 메커니즘을 상세히 정리합니다.

---

## 목차

1. [개요](#1-개요)
2. [기본 개념](#2-기본-개념)
3. [상세 비교표](#3-상세-비교표)
4. [HTTP 동작 원리](#4-http-동작-원리)
5. [HTTPS 동작 원리](#5-https-동작-원리-tls-handshake)
6. [TLS 버전별 특징](#6-tls-버전별-특징)
7. [암호화 기술 상세](#7-암호화-기술-상세)
8. [인증서 체계](#8-인증서-체계-pki)
9. [보안 위협 비교](#9-보안-위협-비교)
10. [실무 적용 가이드](#10-실무-적용-가이드)

---

## 1. 개요

### HTTP (HyperText Transfer Protocol)

- **1991년** Tim Berners-Lee가 월드 와이드 웹과 함께 개발
- 웹 브라우저와 서버 간 **하이퍼텍스트 문서를 전송**하기 위한 애플리케이션 계층 프로토콜
- **무상태(Stateless)** 프로토콜: 각 요청이 독립적
- **요청-응답(Request-Response)** 모델 기반

### HTTPS (HTTP Secure)

- **1994년** Netscape가 SSL(Secure Sockets Layer)과 함께 도입
- HTTP + **SSL/TLS 암호화** = HTTPS
- 현재는 SSL이 아닌 **TLS(Transport Layer Security)** 사용
  - SSL 3.0 → TLS 1.0 → TLS 1.1 → TLS 1.2 → **TLS 1.3** (현재 권장)

---

## 2. 기본 개념

### OSI 7계층에서의 위치

```
┌─────────────────────────────────────────┐
│ 7. Application Layer  │ HTTP, HTTPS    │
├─────────────────────────────────────────┤
│ 6. Presentation Layer │ SSL/TLS        │  ← HTTPS는 여기서 암호화
├─────────────────────────────────────────┤
│ 5. Session Layer      │                │
├─────────────────────────────────────────┤
│ 4. Transport Layer    │ TCP            │
├─────────────────────────────────────────┤
│ 3. Network Layer      │ IP             │
├─────────────────────────────────────────┤
│ 2. Data Link Layer    │ Ethernet       │
├─────────────────────────────────────────┤
│ 1. Physical Layer     │ Cable, Wi-Fi   │
└─────────────────────────────────────────┘
```

### 프로토콜 스택 비교

```
HTTP:                      HTTPS:
┌──────────────┐           ┌──────────────┐
│     HTTP     │           │     HTTP     │
├──────────────┤           ├──────────────┤
│     TCP      │           │   SSL/TLS    │  ← 암호화 계층 추가
├──────────────┤           ├──────────────┤
│     IP       │           │     TCP      │
└──────────────┘           ├──────────────┤
                           │     IP       │
                           └──────────────┘
```

---

## 3. 상세 비교표

### 기본 특성 비교

| 구분 | HTTP | HTTPS |
|------|------|-------|
| **전체 이름** | HyperText Transfer Protocol | HyperText Transfer Protocol Secure |
| **기본 포트** | 80 | 443 |
| **URL 스킴** | `http://` | `https://` |
| **표준 문서** | RFC 2616 (HTTP/1.1), RFC 7540 (HTTP/2) | RFC 2818 (HTTPS), RFC 8446 (TLS 1.3) |
| **개발 연도** | 1991년 | 1994년 |

### 보안 특성 비교

| 구분 | HTTP | HTTPS |
|------|------|-------|
| **암호화** | 없음 (평문 전송) | SSL/TLS 암호화 |
| **데이터 무결성** | 보장 안 됨 | MAC(Message Authentication Code)으로 보장 |
| **서버 인증** | 없음 | 인증서로 서버 신원 확인 |
| **클라이언트 인증** | 없음 | 상호 TLS(mTLS)로 가능 |
| **인증서 필요** | 불필요 | SSL/TLS 인증서 필수 |

### 성능 및 운영 비교

| 구분 | HTTP | HTTPS |
|------|------|-------|
| **초기 연결 속도** | 빠름 (TCP 3-way만) | 약간 느림 (TLS Handshake 추가) |
| **데이터 전송 속도** | 동일 | 동일 (암호화 오버헤드 무시 가능) |
| **CPU 사용량** | 낮음 | 약간 높음 (암/복호화) |
| **캐싱** | 프록시 캐싱 자유로움 | 제한적 (중간자 복호화 불가) |
| **인증서 비용** | 없음 | 무료 (Let's Encrypt) ~ 유료 |

### 기타 비교

| 구분 | HTTP | HTTPS |
|------|------|-------|
| **SEO 점수** | 불리 | Google 등 검색엔진 우대 |
| **브라우저 표시** | "안전하지 않음" 경고 | 자물쇠 아이콘 |
| **HTTP/2 지원** | 브라우저에서 미지원 | 필수 (브라우저 정책) |
| **HTTP/3 지원** | 미지원 | 지원 |
| **PWA 지원** | 불가 | 필수 요건 |
| **Geolocation API** | 차단됨 | 허용 |

---

## 4. HTTP 동작 원리

### 기본 통신 흐름

```
┌──────────────┐                              ┌──────────────┐
│   Client     │                              │    Server    │
│  (Browser)   │                              │  (Web App)   │
└──────┬───────┘                              └──────┬───────┘
       │                                             │
       │──────── 1. TCP SYN ────────────────────────>│
       │<─────── 2. TCP SYN-ACK ─────────────────────│
       │──────── 3. TCP ACK ────────────────────────>│
       │                                             │
       │         === TCP 연결 완료 (3-way) ===       │
       │                                             │
       │──────── 4. HTTP Request (평문) ────────────>│
       │         GET /index.html HTTP/1.1            │
       │         Host: example.com                   │
       │         User-Agent: Chrome/120              │
       │                                             │
       │<─────── 5. HTTP Response (평문) ────────────│
       │         HTTP/1.1 200 OK                     │
       │         Content-Type: text/html             │
       │         <html>...</html>                    │
       │                                             │
```

### HTTP 요청 구조

```http
GET /api/users HTTP/1.1
Host: example.com
User-Agent: Mozilla/5.0
Accept: application/json
Cookie: session_id=abc123
Authorization: Bearer eyJhbGciOiJIUzI1...

(요청 본문 - POST/PUT인 경우)
```

### HTTP 응답 구조

```http
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 256
Set-Cookie: session_id=xyz789
Cache-Control: max-age=3600

{"id": 1, "name": "John", "email": "john@example.com"}
```

### HTTP의 보안 취약점

```
┌──────────┐                    ┌──────────┐                    ┌──────────┐
│  Client  │═══════════════════>│ Attacker │═══════════════════>│  Server  │
└──────────┘     평문 데이터     └──────────┘     평문 데이터     └──────────┘
                    │
                    ▼
            ┌──────────────┐
            │ 데이터 탈취   │
            │ 데이터 변조   │
            │ 세션 하이재킹  │
            └──────────────┘
```

---

## 5. HTTPS 동작 원리 (TLS Handshake)

### TLS 1.2 Handshake (전통적 방식)

```
┌──────────────┐                              ┌──────────────┐
│   Client     │                              │    Server    │
└──────┬───────┘                              └──────┬───────┘
       │                                             │
       │══════ TCP 3-way Handshake (동일) ══════════│
       │                                             │
       │────────── TLS Handshake 시작 ──────────────│
       │                                             │
       │─── 1. ClientHello ─────────────────────────>│
       │    • 지원 TLS 버전: 1.2, 1.3               │
       │    • 지원 Cipher Suites 목록               │
       │    • 클라이언트 랜덤값 (32 bytes)          │
       │    • Session ID (재연결 시)                │
       │                                             │
       │<── 2. ServerHello ──────────────────────────│
       │    • 선택된 TLS 버전: 1.2                  │
       │    • 선택된 Cipher Suite                   │
       │    • 서버 랜덤값 (32 bytes)                │
       │                                             │
       │<── 3. Certificate ──────────────────────────│
       │    • 서버 인증서 체인                      │
       │    • 서버 공개키 포함                      │
       │                                             │
       │<── 4. ServerKeyExchange (선택적) ───────────│
       │    • DHE/ECDHE 키 교환 파라미터            │
       │                                             │
       │<── 5. ServerHelloDone ──────────────────────│
       │                                             │
       │    [클라이언트: 인증서 검증]                │
       │    • CA 서명 확인                          │
       │    • 유효기간 확인                         │
       │    • 도메인 일치 확인                      │
       │    • 인증서 폐기 확인 (CRL/OCSP)           │
       │                                             │
       │─── 6. ClientKeyExchange ───────────────────>│
       │    • Pre-Master Secret                     │
       │    • (서버 공개키로 암호화)                │
       │                                             │
       │    [양측: Master Secret 생성]               │
       │    Master Secret = PRF(                     │
       │      Pre-Master Secret,                     │
       │      "master secret",                       │
       │      ClientRandom + ServerRandom            │
       │    )                                        │
       │                                             │
       │─── 7. ChangeCipherSpec ────────────────────>│
       │    (이후 암호화 통신 전환 알림)            │
       │                                             │
       │─── 8. Finished (암호화) ───────────────────>│
       │    • 핸드셰이크 검증 해시                  │
       │                                             │
       │<── 9. ChangeCipherSpec ─────────────────────│
       │<── 10. Finished (암호화) ───────────────────│
       │                                             │
       │══════════ TLS Handshake 완료 ══════════════│
       │                                             │
       │<═══════ 암호화된 HTTP 통신 ════════════════>│
       │         (Application Data)                  │
       │                                             │
```

### TLS 1.3 Handshake (개선된 방식)

TLS 1.3은 **1-RTT (Round Trip Time)** 으로 핸드셰이크를 단축했습니다.

```
┌──────────────┐                              ┌──────────────┐
│   Client     │                              │    Server    │
└──────┬───────┘                              └──────┬───────┘
       │                                             │
       │─── 1. ClientHello ─────────────────────────>│
       │    • 지원 TLS 버전                         │
       │    • 지원 Cipher Suites                    │
       │    • Key Share (ECDHE 공개키)              │ ← 키 교환 미리 시작
       │    • Signature Algorithms                  │
       │                                             │
       │<── 2. ServerHello ──────────────────────────│
       │    • 선택된 Cipher Suite                   │
       │    • Key Share (ECDHE 공개키)              │
       │                                             │
       │<── 3. EncryptedExtensions ──────────────────│
       │<── 4. Certificate ──────────────────────────│
       │<── 5. CertificateVerify ────────────────────│
       │<── 6. Finished ─────────────────────────────│
       │                                             │
       │    [모든 이후 통신 암호화됨]                │
       │                                             │
       │─── 7. Finished ────────────────────────────>│
       │                                             │
       │<═══════ 암호화된 HTTP 통신 ════════════════>│
       │                                             │
```

### TLS 1.3 0-RTT (Zero Round Trip Time)

이전에 연결했던 서버에 재연결 시 첫 패킷부터 암호화된 데이터 전송 가능:

```
┌──────────────┐                              ┌──────────────┐
│   Client     │                              │    Server    │
└──────┬───────┘                              └──────┬───────┘
       │                                             │
       │─── ClientHello + Early Data ───────────────>│
       │    • PSK (Pre-Shared Key)                  │
       │    • 암호화된 HTTP 요청 (0-RTT)            │ ← 첫 패킷부터 데이터 전송!
       │                                             │
       │<── ServerHello + Finished ──────────────────│
       │<── 암호화된 HTTP 응답 ──────────────────────│
       │                                             │
```

> **주의**: 0-RTT는 Replay Attack에 취약할 수 있어 멱등성(Idempotent) 요청에만 권장

---

## 6. TLS 버전별 특징

| 버전 | 출시 | 상태 | 주요 특징 |
|------|------|------|----------|
| **SSL 2.0** | 1995 | 폐기 | 심각한 보안 취약점 |
| **SSL 3.0** | 1996 | 폐기 | POODLE 취약점 |
| **TLS 1.0** | 1999 | 폐기 | BEAST 취약점 |
| **TLS 1.1** | 2006 | 폐기 | CBC 취약점 일부 해결 |
| **TLS 1.2** | 2008 | 사용 | AEAD 암호화 지원, SHA-256 |
| **TLS 1.3** | 2018 | 권장 | 1-RTT, 0-RTT, 취약 알고리즘 제거 |

### TLS 1.3에서 제거된 것들

- RSA 키 교환 (Forward Secrecy 미지원)
- CBC 모드 암호화
- RC4, DES, 3DES
- MD5, SHA-1
- 압축 (CRIME 공격 방지)
- 재협상 (Renegotiation)

---

## 7. 암호화 기술 상세

### 암호화 종류와 용도

| 구분 | 알고리즘 예시 | 용도 | 특징 |
|------|--------------|------|------|
| **비대칭 암호화** | RSA, ECDSA, Ed25519 | 키 교환, 서명 | 느리지만 안전한 키 교환 |
| **대칭 암호화** | AES-128-GCM, ChaCha20-Poly1305 | 데이터 암호화 | 빠른 대량 데이터 처리 |
| **해시 함수** | SHA-256, SHA-384 | 무결성 검증 | 단방향, 고정 길이 출력 |
| **MAC** | HMAC-SHA256, Poly1305 | 메시지 인증 | 무결성 + 인증 |
| **키 교환** | ECDHE, X25519 | 세션 키 생성 | Forward Secrecy 제공 |

### Cipher Suite 구조

Cipher Suite는 사용할 암호화 알고리즘 조합을 정의합니다:

```
TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
│   │     │        │   │   │   │
│   │     │        │   │   │   └── PRF 해시: SHA-384
│   │     │        │   │   └────── 인증 태그 방식: GCM
│   │     │        │   └────────── 키 길이: 256비트
│   │     │        └────────────── 대칭 암호화: AES
│   │     └─────────────────────── 인증서 타입: RSA
│   └───────────────────────────── 키 교환: ECDHE
└───────────────────────────────── 프로토콜: TLS
```

### TLS 1.3 Cipher Suites (간소화됨)

```
TLS_AES_256_GCM_SHA384
TLS_AES_128_GCM_SHA256
TLS_CHACHA20_POLY1305_SHA256
```

> TLS 1.3에서는 키 교환은 항상 ECDHE, 인증서 서명은 별도 협상

### Forward Secrecy (전방향 비밀성)

```
일반 RSA 키 교환 (Forward Secrecy 없음):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
• 서버의 개인키가 유출되면 과거 모든 통신 복호화 가능
• 공격자가 과거 트래픽을 저장해두었다가 나중에 복호화


ECDHE 키 교환 (Forward Secrecy 있음):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
• 매 세션마다 새로운 임시 키 쌍 생성
• 서버 개인키가 유출되어도 과거 세션 키 복구 불가
• 각 세션이 독립적으로 보호됨
```

---

## 8. 인증서 체계 (PKI)

### 인증서 체인 구조

```
┌─────────────────────────────────────────────────────────┐
│                    Root CA 인증서                       │
│  (DigiCert, Let's Encrypt, GlobalSign 등)               │
│  • 브라우저/OS에 미리 내장 (Trust Store)                │
│  • 자체 서명 (Self-signed)                             │
│  • 유효기간: 20-30년                                   │
└─────────────────────────┬───────────────────────────────┘
                          │ 서명
                          ▼
┌─────────────────────────────────────────────────────────┐
│               Intermediate CA 인증서                    │
│  (중간 인증 기관)                                       │
│  • Root CA가 서명                                       │
│  • 실제 서버 인증서 발급 담당                           │
│  • Root CA 보호 (오프라인 보관)                         │
└─────────────────────────┬───────────────────────────────┘
                          │ 서명
                          ▼
┌─────────────────────────────────────────────────────────┐
│                End-Entity 인증서                        │
│  (서버 인증서, 예: example.com)                         │
│  • Intermediate CA가 서명                               │
│  • 서버에 설치                                          │
│  • 유효기간: 90일 ~ 1년                                 │
└─────────────────────────────────────────────────────────┘
```

### 인증서 내용 (X.509 v3)

```
Certificate:
    Data:
        Version: 3 (0x2)
        Serial Number: 04:00:00:00:00:01:2f:4e:e6:52:a3
        Signature Algorithm: sha256WithRSAEncryption
        Issuer: CN=DigiCert SHA2 Extended Validation Server CA
        Validity:
            Not Before: Jan  1 00:00:00 2024 GMT
            Not After : Dec 31 23:59:59 2024 GMT
        Subject: CN=www.example.com, O=Example Inc, L=Seoul, C=KR
        Subject Public Key Info:
            Public Key Algorithm: rsaEncryption
            RSA Public-Key: (2048 bit)
        X509v3 extensions:
            X509v3 Subject Alternative Name:
                DNS:example.com, DNS:www.example.com, DNS:api.example.com
            X509v3 Key Usage:
                Digital Signature, Key Encipherment
            X509v3 Extended Key Usage:
                TLS Web Server Authentication
```

### 인증서 유형 비교

| 유형 | 검증 내용 | 발급 시간 | 비용 | 표시 |
|------|----------|----------|------|------|
| **DV (Domain Validation)** | 도메인 소유권만 | 수 분 | 무료 ~ 저렴 | 자물쇠 |
| **OV (Organization Validation)** | 도메인 + 조직 | 수 일 | 중간 | 자물쇠 + 조직명 |
| **EV (Extended Validation)** | 도메인 + 조직 + 법적 실체 | 수 주 | 고가 | 녹색 주소창 (구형 브라우저) |

### 인증서 폐기 확인

```
┌──────────────────────────────────────────────────────────┐
│ CRL (Certificate Revocation List)                        │
│━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━│
│ • CA가 주기적으로 폐기 목록 발행                         │
│ • 클라이언트가 다운로드하여 확인                         │
│ • 단점: 목록이 커지면 느림                               │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│ OCSP (Online Certificate Status Protocol)                │
│━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━│
│ • 실시간으로 CA에 인증서 상태 질의                       │
│ • 개별 인증서 확인 가능                                  │
│ • 단점: 프라이버시 우려 (CA가 방문 사이트 알 수 있음)    │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│ OCSP Stapling                                            │
│━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━│
│ • 서버가 미리 OCSP 응답을 받아 클라이언트에 제공         │
│ • 프라이버시 보호 + 성능 향상                            │
│ • 현재 권장 방식                                         │
└──────────────────────────────────────────────────────────┘
```

---

## 9. 보안 위협 비교

### 공격 유형별 방어 여부

| 공격 유형 | 설명 | HTTP | HTTPS |
|-----------|------|------|-------|
| **도청 (Eavesdropping)** | 네트워크 트래픽 감청 | 취약 | 방어됨 |
| **중간자 공격 (MITM)** | 통신 중간에서 가로채기/변조 | 취약 | 방어됨 |
| **세션 하이재킹** | 세션 쿠키 탈취 | 취약 | 방어됨 |
| **데이터 변조** | 전송 중 내용 수정 | 취약 | 방어됨 |
| **Replay Attack** | 캡처한 요청 재전송 | 취약 | 부분 방어 |
| **SSL Stripping** | HTTPS→HTTP 다운그레이드 | N/A | HSTS로 방어 |
| **피싱** | 가짜 사이트로 유도 | 취약 | EV 인증서로 일부 방어 |

### 중간자 공격 (MITM) 시나리오

```
HTTP (취약):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

[사용자] ──평문──> [공격자] ──평문──> [서버]
   │                  │
   │                  ▼
   │           ┌─────────────┐
   │           │ 비밀번호    │
   │           │ 카드번호    │  ← 탈취!
   │           │ 개인정보    │
   │           └─────────────┘


HTTPS (방어됨):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

[사용자] ══암호화══> [공격자] ══암호화══> [서버]
   │                    │
   │                    ▼
   │              ┌──────────┐
   │              │ ???????? │  ← 복호화 불가!
   │              │ ???????? │
   │              └──────────┘
```

### SSL Stripping 공격과 방어

```
공격 시나리오:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1. 사용자가 http://bank.com 입력
2. 공격자가 중간에서 요청 가로챔
3. 공격자는 서버와 HTTPS로 통신
4. 사용자에게는 HTTP로 응답
5. 사용자는 평문으로 로그인 정보 전송 → 탈취!


HSTS (HTTP Strict Transport Security)로 방어:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

HTTP Response Header:
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload

• 브라우저가 해당 도메인은 항상 HTTPS로만 접속
• max-age 동안 HTTP 접속 시도 자체를 차단
• preload: 브라우저에 미리 등록 (hstspreload.org)
```

---

## 10. 실무 적용 가이드

### HTTPS 적용 체크리스트

```
□ 1. 인증서 발급
   ├─ Let's Encrypt (무료, 90일 자동 갱신)
   ├─ 상용 CA (DigiCert, Sectigo 등)
   └─ 와일드카드 인증서 (*.example.com)

□ 2. 서버 설정
   ├─ TLS 1.2+ 만 허용
   ├─ 강력한 Cipher Suite 설정
   ├─ OCSP Stapling 활성화
   └─ HTTP → HTTPS 리다이렉트

□ 3. 보안 헤더 설정
   ├─ Strict-Transport-Security (HSTS)
   ├─ Content-Security-Policy
   └─ X-Content-Type-Options

□ 4. 인증서 관리
   ├─ 자동 갱신 설정 (certbot)
   ├─ 만료 알림 모니터링
   └─ 인증서 체인 완전성 확인
```

### Nginx HTTPS 설정 예시

```nginx
server {
    listen 443 ssl http2;
    server_name example.com;

    # 인증서 설정
    ssl_certificate /etc/letsencrypt/live/example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/example.com/privkey.pem;

    # TLS 설정
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;
    ssl_prefer_server_ciphers off;

    # OCSP Stapling
    ssl_stapling on;
    ssl_stapling_verify on;
    resolver 8.8.8.8 8.8.4.4 valid=300s;

    # 보안 헤더
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
}

# HTTP → HTTPS 리다이렉트
server {
    listen 80;
    server_name example.com;
    return 301 https://$server_name$request_uri;
}
```

### SSL Labs 등급 기준

| 등급 | 점수 | 의미 |
|------|------|------|
| **A+** | 80+ & HSTS | 최상의 보안 설정 |
| **A** | 80-100 | 우수한 보안 |
| **B** | 65-79 | 양호하나 개선 필요 |
| **C** | 50-64 | 취약점 존재 |
| **F** | 0-49 | 심각한 취약점 |

> 테스트: https://www.ssllabs.com/ssltest/

### 현대 웹 환경에서의 HTTPS

| 기술/정책 | HTTPS 요구 사항 |
|-----------|----------------|
| **HTTP/2** | 브라우저에서 HTTPS 필수 |
| **HTTP/3 (QUIC)** | TLS 1.3 내장 (필수) |
| **PWA (Progressive Web App)** | Service Worker 사용 시 필수 |
| **Geolocation API** | Secure Context 필수 |
| **Web Bluetooth/USB** | Secure Context 필수 |
| **Payment Request API** | Secure Context 필수 |
| **Clipboard API** | Secure Context 필수 |
| **Google SEO** | HTTPS 사이트 순위 우대 |
| **Chrome 브라우저** | HTTP 사이트 "안전하지 않음" 표시 |

---

## 참고 자료

- [RFC 2818 - HTTP Over TLS](https://tools.ietf.org/html/rfc2818)
- [RFC 8446 - TLS 1.3](https://tools.ietf.org/html/rfc8446)
- [Mozilla SSL Configuration Generator](https://ssl-config.mozilla.org/)
- [SSL Labs Server Test](https://www.ssllabs.com/ssltest/)
- [HSTS Preload List](https://hstspreload.org/)
- [Let's Encrypt Documentation](https://letsencrypt.org/docs/)

---

*마지막 업데이트: 2026년 1월*
