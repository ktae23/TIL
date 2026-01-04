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

**소켓 수신 대기 (listen):**

```python
import socket

# 소켓 생성
server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# 주소 재사용 (TIME_WAIT 대응)
server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

# 바인딩
server_socket.bind(('0.0.0.0', 80))

# 리스닝 (백로그 큐: 128)
server_socket.listen(128)
print("Server listening on port 80...")

while True:
    # 연결 수락
    client_socket, client_address = server_socket.accept()
    print(f"Connection from {client_address}")

    # 요청 수신
    request = client_socket.recv(4096)
    print(f"Request:\n{request.decode()}")

    # 응답 전송
    response = b"HTTP/1.1 200 OK\r\nContent-Length: 13\r\n\r\nHello, World!"
    client_socket.sendall(response)

    # 연결 종료
    client_socket.close()
```

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

```python
def parse_http_request(request_bytes):
    # 헤더와 본문 분리
    header, _, body = request_bytes.partition(b'\r\n\r\n')
    header_lines = header.split(b'\r\n')

    # 요청 라인 파싱
    request_line = header_lines[0].decode('utf-8')
    method, path, version = request_line.split(' ')

    # 헤더 파싱
    headers = {}
    for line in header_lines[1:]:
        if b': ' in line:
            key, value = line.decode('utf-8').split(': ', 1)
            headers[key.lower()] = value

    return {
        'method': method,
        'path': path,
        'version': version,
        'headers': headers,
        'body': body
    }

# 사용 예시
request = b"""GET /index.html HTTP/1.1\r
Host: www.example.com\r
User-Agent: Mozilla/5.0\r
\r
"""

parsed = parse_http_request(request)
print(parsed)
# {
#   'method': 'GET',
#   'path': '/index.html',
#   'version': 'HTTP/1.1',
#   'headers': {'host': 'www.example.com', 'user-agent': 'Mozilla/5.0'},
#   'body': b''
# }
```

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

**동적 라우팅 (Flask 예시):**

```python
from flask import Flask, request, jsonify

app = Flask(__name__)

# 정적 경로
@app.route('/')
def index():
    return 'Hello, World!'

# 동적 경로 (파라미터)
@app.route('/users/<int:user_id>')
def get_user(user_id):
    # 데이터베이스에서 사용자 조회
    user = db.query(f'SELECT * FROM users WHERE id={user_id}')
    return jsonify(user)

# POST 요청 처리
@app.route('/users', methods=['POST'])
def create_user():
    data = request.get_json()
    # 데이터베이스에 저장
    user_id = db.insert('users', data)
    return jsonify({'id': user_id}), 201

# 쿼리 파라미터
@app.route('/search')
def search():
    query = request.args.get('q')
    page = request.args.get('page', 1, type=int)
    results = db.search(query, page)
    return jsonify(results)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
```

**Express.js (Node.js) 라우팅:**

```javascript
const express = require('express');
const app = express();

// JSON 파싱 미들웨어
app.use(express.json());

// 로깅 미들웨어
app.use((req, res, next) => {
    console.log(`${req.method} ${req.url}`);
    next();
});

// 라우트
app.get('/', (req, res) => {
    res.send('Hello, World!');
});

app.get('/api/users/:id', async (req, res) => {
    const userId = req.params.id;
    const user = await db.query('SELECT * FROM users WHERE id = ?', [userId]);
    res.json(user);
});

app.post('/api/users', async (req, res) => {
    const { name, email } = req.body;
    const result = await db.insert('INSERT INTO users (name, email) VALUES (?, ?)', [name, email]);
    res.status(201).json({ id: result.insertId });
});

// 404 핸들러
app.use((req, res) => {
    res.status(404).json({ error: 'Not Found' });
});

// 에러 핸들러
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({ error: 'Internal Server Error' });
});

app.listen(3000, () => {
    console.log('Server listening on port 3000');
});
```

### 요청 검증 및 보안

**입력 검증:**

```python
from flask import Flask, request, jsonify
from marshmallow import Schema, fields, ValidationError

app = Flask(__name__)

# 스키마 정의
class UserSchema(Schema):
    name = fields.Str(required=True, validate=lambda x: len(x) > 0)
    email = fields.Email(required=True)
    age = fields.Int(required=True, validate=lambda x: 0 < x < 150)

user_schema = UserSchema()

@app.route('/api/users', methods=['POST'])
def create_user():
    try:
        # 검증
        data = user_schema.load(request.get_json())

        # DB 저장
        user_id = db.insert('users', data)

        return jsonify({'id': user_id}), 201

    except ValidationError as err:
        return jsonify({'errors': err.messages}), 400
```

**SQL 인젝션 방지:**

```python
# 나쁜 예 (취약)
user_id = request.args.get('id')
query = f"SELECT * FROM users WHERE id = {user_id}"
# 공격: ?id=1 OR 1=1

# 좋은 예 (안전)
user_id = request.args.get('id')
query = "SELECT * FROM users WHERE id = ?"
cursor.execute(query, (user_id,))
```

**XSS 방지:**

```python
from flask import escape

@app.route('/search')
def search():
    query = request.args.get('q', '')
    # HTML 이스케이프
    safe_query = escape(query)
    return f'<h1>Search results for: {safe_query}</h1>'
```

**CSRF 방지:**

```python
from flask_wtf.csrf import CSRFProtect

app = Flask(__name__)
app.config['SECRET_KEY'] = 'your-secret-key'
csrf = CSRFProtect(app)

@app.route('/api/users', methods=['POST'])
@csrf.exempt  # API는 토큰 기반 인증 사용
def create_user():
    # ...
    pass
```

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

**CGI 스크립트 예시 (Python):**

```python
#!/usr/bin/env python3
import os
import cgi

# HTTP 헤더 출력 (필수)
print("Content-Type: text/html\n")

# HTML 출력
print("<html>")
print("<head><title>CGI Test</title></head>")
print("<body>")
print("<h1>CGI Environment Variables</h1>")
print(f"<p>REQUEST_METHOD: {os.environ.get('REQUEST_METHOD')}</p>")
print(f"<p>QUERY_STRING: {os.environ.get('QUERY_STRING')}</p>")
print(f"<p>REMOTE_ADDR: {os.environ.get('REMOTE_ADDR')}</p>")

# POST 데이터 처리
if os.environ.get('REQUEST_METHOD') == 'POST':
    form = cgi.FieldStorage()
    name = form.getvalue('name', 'Unknown')
    print(f"<p>Hello, {name}!</p>")

print("</body>")
print("</html>")
```

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

**WSGI 애플리케이션 (Python):**

```python
# app.py
def application(environ, start_response):
    """WSGI 애플리케이션"""
    status = '200 OK'
    headers = [('Content-Type', 'text/html; charset=utf-8')]
    start_response(status, headers)

    # 요청 정보
    method = environ['REQUEST_METHOD']
    path = environ['PATH_INFO']
    query = environ.get('QUERY_STRING', '')

    response = f"""
    <html>
    <body>
        <h1>WSGI Application</h1>
        <p>Method: {method}</p>
        <p>Path: {path}</p>
        <p>Query: {query}</p>
    </body>
    </html>
    """

    return [response.encode('utf-8')]
```

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

**Node.js (비동기 I/O):**

```javascript
const http = require('http');
const fs = require('fs').promises;

const server = http.createServer(async (req, res) => {
    if (req.url === '/') {
        try {
            // 비동기 파일 읽기
            const data = await fs.readFile('index.html', 'utf8');
            res.writeHead(200, { 'Content-Type': 'text/html' });
            res.end(data);
        } catch (err) {
            res.writeHead(500);
            res.end('Internal Server Error');
        }
    } else if (req.url === '/api/data') {
        // 비동기 데이터베이스 조회
        const data = await db.query('SELECT * FROM users');
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(data));
    }
});

server.listen(3000, () => {
    console.log('Server listening on port 3000');
});
```

**Python asyncio (비동기):**

```python
import asyncio
from aiohttp import web

async def handle(request):
    name = request.match_info.get('name', 'World')

    # 비동기 I/O 작업
    await asyncio.sleep(0.1)  # 시뮬레이션

    return web.Response(text=f'Hello, {name}!')

async def handle_api(request):
    # 비동기 데이터베이스 조회
    data = await db.fetch_all('SELECT * FROM users')
    return web.json_response(data)

app = web.Application()
app.add_routes([
    web.get('/', handle),
    web.get('/api/users', handle_api),
])

if __name__ == '__main__':
    web.run_app(app, host='0.0.0.0', port=8080)
```

---

## 4. 데이터베이스 조회 및 처리

### 데이터베이스 연결

**Connection Pool (연결 풀):**

```python
import pymysql
from dbutils.pooled_db import PooledDB

# 연결 풀 생성
pool = PooledDB(
    creator=pymysql,
    maxconnections=10,  # 최대 연결 수
    mincached=2,  # 최소 유지 연결
    maxcached=5,  # 최대 유지 연결
    blocking=True,  # 연결 대기
    host='localhost',
    user='user',
    password='password',
    database='mydb',
    charset='utf8mb4'
)

def get_user(user_id):
    # 풀에서 연결 가져오기
    conn = pool.connection()
    try:
        with conn.cursor() as cursor:
            sql = "SELECT * FROM users WHERE id = %s"
            cursor.execute(sql, (user_id,))
            result = cursor.fetchone()
            return result
    finally:
        # 연결 반환 (닫지 않음)
        conn.close()
```

**ORM (Object-Relational Mapping) - SQLAlchemy:**

```python
from sqlalchemy import create_engine, Column, Integer, String
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker

# 데이터베이스 연결
engine = create_engine('mysql+pymysql://user:password@localhost/mydb',
                       pool_size=10, max_overflow=20)
Base = declarative_base()

# 모델 정의
class User(Base):
    __tablename__ = 'users'

    id = Column(Integer, primary_key=True)
    name = Column(String(100))
    email = Column(String(100), unique=True)
    age = Column(Integer)

# 세션 팩토리
Session = sessionmaker(bind=engine)

# 조회
def get_user(user_id):
    session = Session()
    try:
        user = session.query(User).filter(User.id == user_id).first()
        return user
    finally:
        session.close()

# 생성
def create_user(name, email, age):
    session = Session()
    try:
        user = User(name=name, email=email, age=age)
        session.add(user)
        session.commit()
        return user.id
    except Exception as e:
        session.rollback()
        raise
    finally:
        session.close()
```

### 쿼리 최적화

**N+1 문제:**

```python
# 나쁜 예 (N+1)
users = session.query(User).all()  # 1 쿼리
for user in users:
    posts = user.posts  # N 쿼리 (사용자마다)
    print(f"{user.name}: {len(posts)} posts")

# 좋은 예 (Eager Loading)
from sqlalchemy.orm import joinedload

users = session.query(User).options(joinedload(User.posts)).all()  # 1 쿼리 (JOIN)
for user in users:
    posts = user.posts  # 추가 쿼리 없음
    print(f"{user.name}: {len(posts)} posts")
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

**캐싱 (Redis):**

```python
import redis
import json

r = redis.Redis(host='localhost', port=6379, decode_responses=True)

def get_user_cached(user_id):
    # 캐시 확인
    cache_key = f'user:{user_id}'
    cached = r.get(cache_key)

    if cached:
        return json.loads(cached)

    # DB 조회
    user = db.query(f'SELECT * FROM users WHERE id = {user_id}')

    # 캐시 저장 (1시간 TTL)
    r.setex(cache_key, 3600, json.dumps(user))

    return user

def invalidate_user_cache(user_id):
    """사용자 업데이트 시 캐시 무효화"""
    r.delete(f'user:{user_id}')
```

### 트랜잭션 처리

```python
from sqlalchemy.orm import Session

def transfer_money(from_user_id, to_user_id, amount):
    session = Session()
    try:
        # 트랜잭션 시작
        from_user = session.query(User).filter(User.id == from_user_id).with_for_update().first()
        to_user = session.query(User).filter(User.id == to_user_id).with_for_update().first()

        if from_user.balance < amount:
            raise ValueError("Insufficient balance")

        # 잔액 업데이트
        from_user.balance -= amount
        to_user.balance += amount

        # 커밋
        session.commit()

        return True

    except Exception as e:
        # 롤백
        session.rollback()
        print(f"Transaction failed: {e}")
        return False

    finally:
        session.close()
```

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

**응답 생성 (Flask):**

```python
from flask import Flask, jsonify, request

app = Flask(__name__)

@app.route('/api/users/<int:user_id>')
def get_user(user_id):
    user = db.get_user(user_id)

    if not user:
        return jsonify({'error': 'User not found'}), 404

    return jsonify(user), 200

@app.route('/api/users', methods=['POST'])
def create_user():
    data = request.get_json()

    # 검증
    if not data.get('email'):
        return jsonify({'error': 'Email is required'}), 400

    # 중복 확인
    if db.user_exists(data['email']):
        return jsonify({'error': 'Email already exists'}), 409

    # 생성
    user_id = db.create_user(data)

    return jsonify({'id': user_id}), 201

@app.route('/api/users/<int:user_id>', methods=['DELETE'])
def delete_user(user_id):
    if not db.user_exists(user_id):
        return jsonify({'error': 'User not found'}), 404

    db.delete_user(user_id)

    # 본문 없음
    return '', 204
```

### 응답 헤더 설정

**보안 헤더:**

```python
from flask import Flask, make_response

app = Flask(__name__)

@app.after_request
def set_security_headers(response):
    # XSS 방어
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['X-Frame-Options'] = 'SAMEORIGIN'
    response.headers['X-XSS-Protection'] = '1; mode=block'

    # HSTS (HTTPS만)
    if request.is_secure:
        response.headers['Strict-Transport-Security'] = 'max-age=31536000; includeSubDomains'

    # CSP (Content Security Policy)
    response.headers['Content-Security-Policy'] = "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'"

    return response

@app.route('/')
def index():
    resp = make_response('Hello, World!')

    # 캐시 제어
    resp.headers['Cache-Control'] = 'public, max-age=3600'
    resp.headers['ETag'] = '"abc123"'

    return resp
```

**CORS (Cross-Origin Resource Sharing):**

```python
from flask import Flask
from flask_cors import CORS

app = Flask(__name__)

# 모든 도메인 허용 (개발 환경)
CORS(app)

# 특정 도메인만 허용 (프로덕션)
CORS(app, origins=['https://example.com'])

# 수동 설정
@app.after_request
def add_cors_headers(response):
    response.headers['Access-Control-Allow-Origin'] = 'https://example.com'
    response.headers['Access-Control-Allow-Methods'] = 'GET, POST, PUT, DELETE, OPTIONS'
    response.headers['Access-Control-Allow-Headers'] = 'Content-Type, Authorization'
    response.headers['Access-Control-Max-Age'] = '3600'
    return response
```

### 콘텐츠 압축

**Gzip 압축:**

```python
from flask import Flask
from flask_compress import Compress

app = Flask(__name__)
Compress(app)  # 자동 Gzip 압축

@app.route('/data')
def get_data():
    # 큰 JSON 응답
    data = {'items': [{'id': i, 'name': f'Item {i}'} for i in range(10000)]}
    return jsonify(data)  # 자동으로 Gzip 압축됨
```

**응답 크기 비교:**
```
원본: 500 KB
Gzip: 50 KB (90% 감소)
```

### 응답 스트리밍

**대용량 파일 스트리밍:**

```python
from flask import Flask, Response, stream_with_context

app = Flask(__name__)

@app.route('/download/<filename>')
def download_file(filename):
    def generate():
        with open(f'/files/{filename}', 'rb') as f:
            while True:
                chunk = f.read(4096)
                if not chunk:
                    break
                yield chunk

    return Response(
        stream_with_context(generate()),
        mimetype='application/octet-stream',
        headers={
            'Content-Disposition': f'attachment; filename={filename}',
            'X-Content-Type-Options': 'nosniff'
        }
    )

@app.route('/stream')
def stream():
    def generate():
        for i in range(100):
            yield f'data: {i}\n\n'
            time.sleep(0.1)

    return Response(generate(), mimetype='text/event-stream')
```

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

**sendfile() 시스템 콜:**

```c
// 일반 방식 (비효율적)
read(file_fd, buffer, size);     // 커널 → 사용자 공간
write(socket_fd, buffer, size);  // 사용자 공간 → 커널

// sendfile (효율적, zero-copy)
sendfile(socket_fd, file_fd, offset, size);  // 커널 내부에서 직접 전송
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

*마지막 업데이트: 2026년 1월*
