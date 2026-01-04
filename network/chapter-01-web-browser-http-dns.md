# Chapter 01. 웹 브라우저가 메시지를 만든다

## 개요

웹 브라우저가 URL을 입력받아 웹 서버와 통신하기까지의 전체 과정을 다룹니다. HTTP 리퀘스트 메시지 생성부터 DNS를 통한 IP 주소 조회, 그리고 프로토콜 스택을 통한 메시지 송신까지의 원리를 실무 사례와 함께 살펴봅니다.

## 목차

1. [HTTP 리퀘스트 메시지 작성](#1-http-리퀘스트-메시지-작성)
2. [DNS 서버를 통한 IP 주소 조회](#2-dns-서버를-통한-ip-주소-조회)
3. [전 세계 DNS 서버의 연대](#3-전-세계-dns-서버의-연대)
4. [프로토콜 스택에 메시지 송신 의뢰](#4-프로토콜-스택에-메시지-송신-의뢰)

---

## 1. HTTP 리퀘스트 메시지 작성

### 탐험 여행은 URL 입력부터 시작한다

웹 브라우저의 동작은 사용자가 URL을 입력하는 순간부터 시작됩니다. URL(Uniform Resource Locator)은 인터넷 상의 리소스 위치를 나타내는 표준 형식입니다.

### 브라우저는 먼저 URL을 해독한다

**URL의 기본 구조:**
```
http://user:password@www.example.com:80/path/to/resource?key1=value1&key2=value2#section
```

각 부분의 의미:
- **프로토콜(Scheme)**: `http://` 또는 `https://` - 통신 방식 지정
- **사용자 정보**: `user:password@` - 기본 인증 정보 (현재는 거의 사용되지 않음)
- **호스트명**: `www.example.com` - 웹 서버의 도메인 이름
- **포트 번호**: `:80` - 생략 시 HTTP는 80, HTTPS는 443 사용
- **경로**: `/path/to/resource` - 서버 내 리소스 위치
- **쿼리 문자열**: `?key1=value1&key2=value2` - 서버에 전달할 파라미터
- **프래그먼트**: `#section` - 페이지 내 특정 위치 (서버로 전송되지 않음)

**실무 사례:**
```
https://api.example.com/v1/users?page=2&limit=20
```
- 프로토콜: `https` (보안 연결)
- 호스트: `api.example.com`
- 경로: `/v1/users` (RESTful API 엔드포인트)
- 쿼리: `page=2&limit=20` (페이지네이션 파라미터)

### 파일명을 생략한 경우

경로의 마지막이 `/`로 끝나는 경우, 웹 서버는 **디렉터리 인덱스** 파일을 반환합니다.

```
http://www.example.com/products/
↓
http://www.example.com/products/index.html (또는 index.php, default.html 등)
```

**서버 설정 예시 (Apache):**
```apache
DirectoryIndex index.html index.php index.htm
```

**실무 사례:**
- `https://example.com/` → 일반적으로 `index.html` 반환
- Next.js, React 등 SPA: `/` → `index.html` → JavaScript가 라우팅 처리

### HTTP의 기본 개념

HTTP(HyperText Transfer Protocol)는 **클라이언트-서버 모델**을 따르는 **비연결성(Connectionless)**, **무상태(Stateless)** 프로토콜입니다.

**주요 특징:**
1. **요청-응답 구조**: 클라이언트가 요청하면 서버가 응답
2. **비연결성**: 요청/응답 후 연결 종료 (HTTP/1.0), Keep-Alive로 개선 (HTTP/1.1+)
3. **무상태성**: 각 요청은 독립적, 이전 요청 정보 유지 안 함

### HTTP 리퀘스트 메시지를 만든다

**HTTP 리퀘스트 메시지 구조:**
```http
GET /api/users/123 HTTP/1.1
Host: api.example.com
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)
Accept: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Connection: keep-alive

```

**구성 요소:**
1. **요청 라인(Request Line)**:
   - 메서드: `GET`, `POST`, `PUT`, `DELETE` 등
   - 요청 URI: `/api/users/123`
   - HTTP 버전: `HTTP/1.1`

2. **헤더(Headers)**:
   - `Host`: 대상 서버 (HTTP/1.1에서 필수)
   - `User-Agent`: 클라이언트 정보
   - `Accept`: 클라이언트가 처리 가능한 콘텐츠 타입
   - `Authorization`: 인증 정보
   - `Content-Type`: 요청 본문의 타입 (POST, PUT 등)

3. **빈 줄**: 헤더와 본문 구분

4. **본문(Body)**: POST, PUT 등에서 전송할 데이터

**실무 사례 - RESTful API POST 요청:**
```http
POST /api/users HTTP/1.1
Host: api.example.com
Content-Type: application/json
Content-Length: 58
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

{"name":"John Doe","email":"john@example.com","age":30}
```

**주요 HTTP 메서드:**
| 메서드 | 용도 | 멱등성 | 안전성 | 실무 예시 |
|--------|------|--------|--------|-----------|
| GET | 리소스 조회 | O | O | 사용자 목록 조회 |
| POST | 리소스 생성 | X | X | 새 게시글 작성 |
| PUT | 리소스 전체 수정 | O | X | 사용자 정보 전체 업데이트 |
| PATCH | 리소스 부분 수정 | △ | X | 사용자 이메일만 수정 |
| DELETE | 리소스 삭제 | O | X | 게시글 삭제 |
| HEAD | 헤더 정보만 조회 | O | O | 파일 존재 여부 확인 |
| OPTIONS | 지원 메서드 조회 | O | O | CORS preflight |

### 리퀘스트 메시지를 보내면 응답이 되돌아온다

**HTTP 응답 메시지 구조:**
```http
HTTP/1.1 200 OK
Date: Sat, 04 Jan 2026 10:00:00 GMT
Server: nginx/1.18.0
Content-Type: application/json; charset=utf-8
Content-Length: 89
Cache-Control: no-cache
Connection: keep-alive

{"id":123,"name":"John Doe","email":"john@example.com","created_at":"2026-01-01"}
```

**상태 라인 구성:**
- HTTP 버전: `HTTP/1.1`
- 상태 코드: `200`
- 상태 메시지: `OK`

**주요 HTTP 상태 코드:**

| 코드 | 분류 | 의미 | 실무 예시 |
|------|------|------|-----------|
| 200 | 성공 | OK | API 요청 성공 |
| 201 | 성공 | Created | 리소스 생성 완료 |
| 204 | 성공 | No Content | 삭제 성공 (본문 없음) |
| 301 | 리다이렉션 | Moved Permanently | URL 영구 이동 |
| 302 | 리다이렉션 | Found | 임시 리다이렉트 |
| 304 | 리다이렉션 | Not Modified | 캐시된 리소스 사용 |
| 400 | 클라이언트 오류 | Bad Request | 잘못된 요청 형식 |
| 401 | 클라이언트 오류 | Unauthorized | 인증 필요 |
| 403 | 클라이언트 오류 | Forbidden | 권한 없음 |
| 404 | 클라이언트 오류 | Not Found | 리소스 없음 |
| 429 | 클라이언트 오류 | Too Many Requests | Rate Limit 초과 |
| 500 | 서버 오류 | Internal Server Error | 서버 내부 오류 |
| 502 | 서버 오류 | Bad Gateway | 게이트웨이 오류 |
| 503 | 서버 오류 | Service Unavailable | 서비스 일시 중단 |

---

## 2. DNS 서버를 통한 IP 주소 조회

### IP 주소의 기본

인터넷에서 통신하려면 **IP 주소**가 필요합니다. IP 주소는 네트워크 상에서 컴퓨터를 식별하는 고유한 주소입니다.

**IPv4 주소 구조:**
```
192.168.1.100
```
- 32비트 (4바이트)
- 0~255 범위의 숫자 4개를 점(.)으로 구분
- 약 43억 개의 주소 표현 가능

**IPv6 주소 구조:**
```
2001:0db8:85a3:0000:0000:8a2e:0370:7334
```
- 128비트 (16바이트)
- 16진수 8개 그룹을 콜론(:)으로 구분
- 약 340간(10^38개) 개의 주소 표현 가능

**IP 주소 클래스 (IPv4):**

| 클래스 | 범위 | 기본 서브넷 마스크 | 용도 |
|--------|------|-------------------|------|
| A | 1.0.0.0 ~ 126.255.255.255 | 255.0.0.0 | 대규모 네트워크 |
| B | 128.0.0.0 ~ 191.255.255.255 | 255.255.0.0 | 중규모 네트워크 |
| C | 192.0.0.0 ~ 223.255.255.255 | 255.255.255.0 | 소규모 네트워크 |

**특수 IP 주소:**
- `127.0.0.1`: 로컬호스트 (localhost)
- `0.0.0.0`: 모든 인터페이스
- `255.255.255.255`: 브로드캐스트
- 사설 IP 대역:
  - `10.0.0.0 ~ 10.255.255.255`
  - `172.16.0.0 ~ 172.31.255.255`
  - `192.168.0.0 ~ 192.168.255.255`

### 도메인명과 IP 주소를 구분하여 사용하는 이유

**도메인명의 장점:**
1. **가독성**: `www.google.com` vs `142.250.189.206`
2. **유연성**: IP 주소 변경 시 도메인명은 그대로 유지
3. **부하 분산**: 하나의 도메인을 여러 IP로 매핑 가능
4. **서비스 분리**: 서브도메인으로 서비스 구분 (api.example.com, cdn.example.com)

**실무 사례:**
```bash
# Google의 경우 여러 IP 주소로 분산
$ nslookup www.google.com
Address: 142.250.189.206
Address: 142.250.189.196
Address: 142.250.189.228
```

### Socket 라이브러리가 IP 주소를 찾는 기능을 제공한다

브라우저는 **Socket 라이브러리**의 **리졸버(Resolver)** 기능을 사용하여 DNS 조회를 수행합니다.

**프로그램 예시 (C):**
```c
#include <netdb.h>
#include <stdio.h>

int main() {
    struct hostent *host;

    // 도메인명으로 IP 주소 조회
    host = gethostbyname("www.example.com");

    if (host != NULL) {
        printf("IP Address: %s\n",
               inet_ntoa(*((struct in_addr *)host->h_addr)));
    }

    return 0;
}
```

**Python 예시:**
```python
import socket

# 도메인명으로 IP 주소 조회
ip_address = socket.gethostbyname("www.example.com")
print(f"IP Address: {ip_address}")

# 더 상세한 정보
addr_info = socket.getaddrinfo("www.example.com", 80)
for info in addr_info:
    print(f"Family: {info[0]}, Address: {info[4][0]}")
```

### 리졸버를 이용하여 DNS 서버를 조회한다

**DNS 조회 과정:**

1. **애플리케이션**: 도메인명 입력
2. **리졸버**: 로컬 DNS 캐시 확인
3. **로컬 DNS 서버**: ISP 또는 기업 DNS 서버에 질의
4. **재귀적 질의**: 루트 DNS → TLD DNS → 권한 DNS
5. **응답**: IP 주소 반환

**실무 도구 - DNS 조회 명령어:**

```bash
# nslookup - 기본 DNS 조회
$ nslookup www.example.com
Server:  192.168.1.1
Address: 192.168.1.1#53

Non-authoritative answer:
Name:    www.example.com
Address: 93.184.216.34

# dig - 상세한 DNS 정보
$ dig www.example.com

; <<>> DiG 9.10.6 <<>> www.example.com
;; QUESTION SECTION:
;www.example.com.        IN      A

;; ANSWER SECTION:
www.example.com. 3600   IN      A       93.184.216.34

;; Query time: 23 msec
;; SERVER: 192.168.1.1#53(192.168.1.1)

# host - 간단한 조회
$ host www.example.com
www.example.com has address 93.184.216.34
```

### 리졸버 내부의 작동

**DNS 쿼리 메시지 구조:**

```
+------------------+
|      Header      |  12 bytes
+------------------+
|     Question     |  가변 길이
+------------------+
|      Answer      |  가변 길이
+------------------+
|    Authority     |  가변 길이
+------------------+
|    Additional    |  가변 길이
+------------------+
```

**Header 구조:**
- Transaction ID: 쿼리와 응답 매칭
- Flags: QR(쿼리/응답), Opcode, AA(권한 있는 응답), TC(절단), RD(재귀 요청)
- Question Count, Answer Count, Authority Count, Additional Count

**실무 사례 - DNS 캐시 확인:**

```bash
# macOS/Linux - DNS 캐시 확인 (systemd-resolved 사용 시)
$ systemd-resolve --statistics

# Windows - DNS 캐시 확인
> ipconfig /displaydns

# DNS 캐시 삭제
# macOS
$ sudo dscacheutil -flushcache; sudo killall -HUP mDNSResponder

# Windows
> ipconfig /flushdns

# Linux (systemd-resolved)
$ sudo systemd-resolve --flush-caches
```

---

## 3. 전 세계 DNS 서버의 연대

### DNS 서버의 기본 동작

DNS는 **분산 계층 구조**로 전 세계에 분산되어 있으며, 각 서버는 특정 도메인 영역에 대한 정보를 관리합니다.

**DNS 서버 유형:**

1. **리졸빙 네임서버 (Recursive Resolver)**
   - 클라이언트 대신 DNS 질의 수행
   - ISP나 기업에서 운영
   - 예: Google DNS (8.8.8.8), Cloudflare DNS (1.1.1.1)

2. **권한 있는 네임서버 (Authoritative Name Server)**
   - 특정 도메인에 대한 최종 정보 보유
   - 도메인 소유자가 관리

3. **루트 네임서버**
   - DNS 계층 구조의 최상위
   - 전 세계 13개 (논리적, 실제로는 수백 개 미러 서버)

4. **TLD 네임서버**
   - Top-Level Domain (.com, .org, .kr 등) 관리

### 도메인의 계층

**도메인 계층 구조:**
```
                        .
                       (루트)
                        |
        +-------+-------+-------+-------+
        |       |       |       |       |
       com     org     net     kr      ...
        |               |       |
    +---+---+       +---+---+   |
    |       |       |       |   |
  google amazon    |       |  co.kr
    |               |       |
   www            www     www

www.google.com
    |     |    |
    3     2    1  (읽는 순서: 오른쪽에서 왼쪽)
```

**FQDN (Fully Qualified Domain Name):**
```
www.example.com.
               ^ (루트를 나타내는 점, 보통 생략)
```

**실무 예시 - 서브도메인 구조:**
```
example.com              (메인 도메인)
├── www.example.com      (웹 서버)
├── api.example.com      (API 서버)
├── cdn.example.com      (CDN)
├── mail.example.com     (메일 서버)
└── dev.example.com      (개발 환경)
    └── api.dev.example.com
```

### 담당 DNS 서버를 찾아 IP 주소를 가져온다

**재귀적 DNS 조회 과정:**

```
1. 클라이언트 → 로컬 DNS (예: 8.8.8.8)
   "www.example.com의 IP 주소는?"

2. 로컬 DNS → 루트 네임서버
   "www.example.com의 IP 주소는?"
   루트 NS: ".com을 담당하는 TLD 서버에 물어보세요"

3. 로컬 DNS → .com TLD 네임서버
   "www.example.com의 IP 주소는?"
   TLD NS: "example.com을 담당하는 권한 서버에 물어보세요"

4. 로컬 DNS → example.com 권한 네임서버
   "www.example.com의 IP 주소는?"
   권한 NS: "93.184.216.34입니다"

5. 로컬 DNS → 클라이언트
   "93.184.216.34입니다"
```

**DNS 레코드 타입:**

| 타입 | 용도 | 예시 |
|------|------|------|
| A | IPv4 주소 매핑 | `example.com. IN A 93.184.216.34` |
| AAAA | IPv6 주소 매핑 | `example.com. IN AAAA 2606:2800:220:1:248:1893:25c8:1946` |
| CNAME | 별칭 (다른 도메인명으로 매핑) | `www.example.com. IN CNAME example.com.` |
| MX | 메일 서버 | `example.com. IN MX 10 mail.example.com.` |
| NS | 네임서버 | `example.com. IN NS ns1.example.com.` |
| TXT | 텍스트 정보 (SPF, DKIM 등) | `example.com. IN TXT "v=spf1 ..."` |
| SOA | 도메인 권한 정보 | 시작 권한, 관리자 이메일 등 |
| PTR | 역방향 조회 (IP → 도메인) | `34.216.184.93.in-addr.arpa. IN PTR example.com.` |

**실무 사례 - DNS 레코드 조회:**

```bash
# A 레코드 조회
$ dig example.com A

# MX 레코드 조회 (메일 서버)
$ dig example.com MX

# NS 레코드 조회 (네임서버)
$ dig example.com NS

# 모든 레코드 조회
$ dig example.com ANY

# CNAME 추적
$ dig www.github.com
# www.github.com → github.com (CNAME)
```

### DNS 서버는 캐시 기능으로 빠르게 회답할 수 있다

**DNS 캐싱 계층:**

1. **브라우저 캐시**: 브라우저 자체 DNS 캐시
2. **OS 캐시**: 운영체제 수준 DNS 캐시
3. **로컬 DNS 서버 캐시**: 리졸버의 캐시
4. **ISP DNS 캐시**: ISP가 운영하는 DNS 서버 캐시

**TTL (Time To Live):**

DNS 레코드에는 캐시 유효 시간이 설정되어 있습니다.

```bash
$ dig example.com

;; ANSWER SECTION:
example.com.    3600    IN    A    93.184.216.34
                ^^^^
               TTL (초)
```

**실무 사례 - TTL 설정 전략:**

| 상황 | 권장 TTL | 이유 |
|------|----------|------|
| 일반적인 경우 | 3600~86400 (1~24시간) | 안정성과 성능 균형 |
| 서버 이전 예정 | 300~600 (5~10분) | 빠른 전파 필요 |
| CDN 사용 | 300~1800 (5~30분) | 유연한 트래픽 분산 |
| 거의 변경 없음 | 86400+ (24시간 이상) | 캐시 효율 극대화 |

**브라우저 DNS 캐시 확인 (Chrome):**
```
chrome://net-internals/#dns
```

---

## 4. 프로토콜 스택에 메시지 송신 의뢰

### 데이터 송·수신 동작의 개요

브라우저가 HTTP 메시지를 만들고 IP 주소를 얻으면, **Socket 라이브러리**를 통해 **프로토콜 스택**(운영체제의 네트워크 제어 소프트웨어)에 데이터 송신을 의뢰합니다.

**프로토콜 스택 구조:**
```
+---------------------------+
|     애플리케이션          |  (브라우저, 메일 클라이언트 등)
+---------------------------+
|    Socket 라이브러리      |  (시스템 콜 인터페이스)
+---------------------------+
|    TCP/UDP (전송 계층)    |  (포트 번호, 연결 관리)
+---------------------------+
|      IP (네트워크 계층)   |  (IP 주소, 라우팅)
+---------------------------+
|  드라이버/LAN 어댑터      |  (MAC 주소, 물리적 전송)
+---------------------------+
```

### 소켓의 작성 단계

**소켓(Socket)**: 네트워크 통신의 종착점(Endpoint)으로, 애플리케이션과 프로토콜 스택을 연결하는 인터페이스입니다.

**소켓 생성 (C 언어):**
```c
#include <sys/socket.h>

// 소켓 생성
int sockfd = socket(AF_INET, SOCK_STREAM, 0);
// AF_INET: IPv4
// SOCK_STREAM: TCP (SOCK_DGRAM은 UDP)
// 0: 프로토콜 자동 선택

if (sockfd < 0) {
    perror("socket creation failed");
    return -1;
}
```

**Python 예시:**
```python
import socket

# TCP 소켓 생성
sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# UDP 소켓 생성
udp_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
```

**소켓 디스크립터:**
- 운영체제가 각 소켓에 할당하는 고유 번호
- 파일 디스크립터와 유사한 개념
- 이후 모든 소켓 작업에서 이 번호로 소켓 식별

### 파이프를 연결하는 접속 단계

**TCP 3-Way Handshake (연결 수립):**

```
클라이언트                    서버
    |                          |
    | -------- SYN --------->  | (1) 연결 요청
    |                          |
    | <----- SYN+ACK --------  | (2) 연결 수락 + 확인
    |                          |
    | -------- ACK --------->  | (3) 확인
    |                          |
   [연결 수립 완료]
```

**connect() 시스템 콜 (C):**
```c
#include <arpa/inet.h>

struct sockaddr_in server_addr;

// 서버 주소 설정
server_addr.sin_family = AF_INET;
server_addr.sin_port = htons(80);  // 포트 80 (HTTP)
inet_pton(AF_INET, "93.184.216.34", &server_addr.sin_addr);

// 서버에 연결
if (connect(sockfd, (struct sockaddr *)&server_addr,
            sizeof(server_addr)) < 0) {
    perror("connection failed");
    return -1;
}
```

**Python 예시:**
```python
import socket

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# 서버에 연결 (www.example.com:80)
server_address = ('www.example.com', 80)
sock.connect(server_address)
print(f"Connected to {server_address}")
```

**실무 사례 - 연결 타임아웃 설정:**
```python
import socket

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.settimeout(5.0)  # 5초 타임아웃

try:
    sock.connect(('www.example.com', 80))
except socket.timeout:
    print("Connection timeout")
except socket.error as e:
    print(f"Connection error: {e}")
```

### 메시지를 주고받는 송·수신 단계

**send() / recv() 시스템 콜 (C):**
```c
// HTTP GET 요청 메시지
char request[] = "GET / HTTP/1.1\r\n"
                 "Host: www.example.com\r\n"
                 "Connection: close\r\n\r\n";

// 데이터 송신
ssize_t sent = send(sockfd, request, strlen(request), 0);

// 응답 수신
char buffer[4096];
ssize_t received = recv(sockfd, buffer, sizeof(buffer) - 1, 0);
buffer[received] = '\0';
printf("Received:\n%s\n", buffer);
```

**Python 예시 - 완전한 HTTP 클라이언트:**
```python
import socket

def http_get(host, path="/"):
    # 소켓 생성 및 연결
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.connect((host, 80))

    # HTTP 요청 메시지 작성
    request = f"GET {path} HTTP/1.1\r\n"
    request += f"Host: {host}\r\n"
    request += "Connection: close\r\n\r\n"

    # 요청 전송
    sock.sendall(request.encode('utf-8'))

    # 응답 수신
    response = b""
    while True:
        chunk = sock.recv(4096)
        if not chunk:
            break
        response += chunk

    sock.close()
    return response.decode('utf-8', errors='ignore')

# 사용 예시
response = http_get("www.example.com")
print(response)
```

**실무 사례 - 논블로킹 I/O:**
```python
import socket
import select

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.setblocking(False)  # 논블로킹 모드

try:
    sock.connect(('www.example.com', 80))
except BlockingIOError:
    pass  # 논블로킹 모드에서는 정상

# select()로 연결 완료 대기
_, writable, _ = select.select([], [sock], [], 5.0)

if writable:
    print("Connection established")
else:
    print("Connection timeout")
```

### 연결 끊기 단계에서 송·수신이 종료된다

**TCP 4-Way Handshake (연결 종료):**

```
클라이언트                    서버
    |                          |
    | -------- FIN --------->  | (1) 연결 종료 요청
    |                          |
    | <------- ACK ----------  | (2) 확인
    |                          |
    | <------- FIN ----------  | (3) 서버도 종료 요청
    |                          |
    | -------- ACK --------->  | (4) 확인
    |                          |
   [연결 종료 완료]
```

**close() 시스템 콜 (C):**
```c
// 소켓 닫기
close(sockfd);
```

**Python 예시:**
```python
# 우아한 종료 (송신만 종료)
sock.shutdown(socket.SHUT_WR)

# 완전 종료
sock.close()
```

**TIME_WAIT 상태:**
- 연결 종료 후 일정 시간(보통 2MSL, 약 1~4분) 대기
- 지연된 패킷 처리를 위함
- 같은 포트 재사용 시 주의 필요

**실무 사례 - SO_REUSEADDR 옵션:**
```python
import socket

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# TIME_WAIT 상태의 포트 재사용 허용
sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

sock.bind(('0.0.0.0', 8080))
sock.listen(5)
```

**컨텍스트 매니저 사용 (권장):**
```python
import socket

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
    sock.connect(('www.example.com', 80))
    sock.sendall(b"GET / HTTP/1.1\r\nHost: www.example.com\r\n\r\n")
    response = sock.recv(4096)
    # 블록 종료 시 자동으로 close() 호출
```

---

## 전체 흐름 요약

```
1. 사용자 URL 입력
   ↓
2. 브라우저가 URL 해독
   ↓
3. DNS 조회 (도메인 → IP 주소)
   ├─ 브라우저 캐시
   ├─ OS 캐시
   ├─ 로컬 DNS 서버
   └─ 재귀적 조회 (루트 → TLD → 권한 DNS)
   ↓
4. Socket 라이브러리 호출
   ↓
5. 프로토콜 스택 동작
   ├─ socket() - 소켓 생성
   ├─ connect() - TCP 3-Way Handshake
   ├─ send() - HTTP 요청 전송
   ├─ recv() - HTTP 응답 수신
   └─ close() - TCP 4-Way Handshake
   ↓
6. 브라우저가 응답 처리 및 렌더링
```

---

## 실무 팁

### 1. DNS 문제 해결

```bash
# DNS 서버 변경 테스트
$ dig @8.8.8.8 example.com       # Google DNS
$ dig @1.1.1.1 example.com       # Cloudflare DNS

# DNS 전파 확인
$ dig +trace example.com         # 전체 조회 경로 추적

# 역방향 조회 (IP → 도메인)
$ dig -x 93.184.216.34
```

### 2. HTTP 디버깅 도구

- **curl**: 명령줄 HTTP 클라이언트
  ```bash
  curl -v https://www.example.com
  ```

- **HTTPie**: 사용자 친화적 HTTP 클라이언트
  ```bash
  http GET https://api.example.com/users
  ```

- **Wireshark**: 패킷 캡처 및 분석

- **브라우저 개발자 도구**: Network 탭

### 3. 성능 최적화

- **DNS Prefetching**: 미리 DNS 조회
  ```html
  <link rel="dns-prefetch" href="//cdn.example.com">
  ```

- **HTTP/2 사용**: 멀티플렉싱, 헤더 압축

- **Keep-Alive**: 연결 재사용
  ```http
  Connection: keep-alive
  Keep-Alive: timeout=5, max=100
  ```

### 4. 보안 고려사항

- **HTTPS 사용 필수**: 암호화된 통신
- **DNSSEC**: DNS 응답 위변조 방지
- **HSTS**: HTTP → HTTPS 강제 리다이렉트
- **CSP**: Content Security Policy 설정

---

*마지막 업데이트: 2026년 1월*
