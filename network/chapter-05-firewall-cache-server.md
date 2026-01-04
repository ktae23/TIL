# Chapter 05. 방화벽, 캐시 서버를 통과한다

## 개요

인터넷 상의 패킷이 최종 목적지인 웹 서버에 도달하기 전 거치는 중요한 중간 시스템들을 다룹니다. 방화벽의 패킷 필터링과 보안 메커니즘, 로드 밸런서의 부하 분산 전략, 캐시 서버와 CDN을 통한 성능 최적화, 프록시 서버의 동작 원리를 실무 사례와 함께 살펴봅니다.

## 목차

1. [방화벽의 패킷 필터링](#1-방화벽의-패킷-필터링)
2. [로드 밸런서를 통한 부하 분산](#2-로드-밸런서를-통한-부하-분산)
3. [캐시 서버로 웹 페이지를 캐시한다](#3-캐시-서버로-웹-페이지를-캐시한다)
4. [CDN과 콘텐츠 전송 최적화](#4-cdn과-콘텐츠-전송-최적화)
5. [프록시 서버의 동작](#5-프록시-서버의-동작)

---

## 1. 방화벽의 패킷 필터링

### 방화벽의 기본 개념

**방화벽 (Firewall):**
- **목적**: 허용된 트래픽만 통과, 악의적 트래픽 차단
- **위치**: 네트워크 경계 (인터넷-내부망 사이)
- **동작**: 패킷 헤더 및 내용 검사

**방화벽 배치 위치:**
```
인터넷
  |
[외부 방화벽] ← 1차 방어선
  |
[DMZ] ← 공개 서버 (웹, 메일)
  |
[내부 방화벽] ← 2차 방어선
  |
[내부 네트워크] ← 중요 자산
```

**방화벽 유형:**

| 유형 | 계층 | 검사 범위 | 성능 | 보안 수준 |
|------|------|----------|------|----------|
| 패킷 필터링 | Layer 3-4 | IP, 포트 | 매우 빠름 | 낮음 |
| Stateful 방화벽 | Layer 3-4 | 연결 상태 추적 | 빠름 | 중간 |
| 애플리케이션 방화벽 | Layer 7 | 애플리케이션 프로토콜 | 느림 | 높음 |
| 차세대 방화벽 (NGFW) | Layer 2-7 | DPI, IPS, 악성코드 검사 | 중간 | 매우 높음 |

**AWS 서비스 활용:**

| 방화벽 유형 | AWS 서비스 | 설명 |
|------------|-----------|------|
| 패킷 필터링 | **Security Group** | Stateful, 인스턴스 레벨 |
| Stateless 방화벽 | **NACL** | 서브넷 레벨 |
| 웹 방화벽 (WAF) | **AWS WAF** | OWASP Top 10 방어 |
| 네트워크 방화벽 | **Network Firewall** | VPC 레벨 IDS/IPS |
| DDoS 방어 | **AWS Shield** | L3/L4/L7 보호 |

### 패킷 필터링 방화벽

**동작 원리:**
```
패킷 도착
  ↓
5-tuple 추출:
  - 출발지 IP
  - 목적지 IP
  - 출발지 포트
  - 목적지 포트
  - 프로토콜 (TCP/UDP/ICMP)
  ↓
규칙 테이블 매칭 (위에서 아래로)
  ↓
일치하는 규칙 발견
  ↓
액션 수행 (ACCEPT/DROP/REJECT)
```

**실무 사례 - iptables 방화벽 규칙:**

```bash
#!/bin/bash
# 기본 정책: 모든 입력 차단
iptables -P INPUT DROP
iptables -P FORWARD DROP
iptables -P OUTPUT ACCEPT

# Loopback 허용
iptables -A INPUT -i lo -j ACCEPT

# 기존 연결 허용 (Stateful)
iptables -A INPUT -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT

# SSH (포트 22) - 특정 IP에서만 허용
iptables -A INPUT -p tcp --dport 22 -s 203.0.113.0/24 -m conntrack --ctstate NEW -j ACCEPT

# HTTP/HTTPS 허용
iptables -A INPUT -p tcp --dport 80 -m conntrack --ctstate NEW -j ACCEPT
iptables -A INPUT -p tcp --dport 443 -m conntrack --ctstate NEW -j ACCEPT

# DNS 허용 (UDP/TCP)
iptables -A INPUT -p udp --dport 53 -j ACCEPT
iptables -A INPUT -p tcp --dport 53 -j ACCEPT

# ICMP (ping) 제한적 허용
iptables -A INPUT -p icmp --icmp-type echo-request -m limit --limit 1/s -j ACCEPT

# 로깅 (차단 전)
iptables -A INPUT -m limit --limit 5/min -j LOG --log-prefix "IPTables-Dropped: " --log-level 4

# 나머지 모두 차단 (명시적)
iptables -A INPUT -j DROP

# 규칙 저장
iptables-save > /etc/iptables/rules.v4
```

**방화벽 규칙 확인:**
```bash
# 규칙 목록
$ sudo iptables -L -n -v --line-numbers
Chain INPUT (policy DROP 0 packets, 0 bytes)
num   pkts bytes target     prot opt in     out     source               destination
1      100  8000 ACCEPT     all  --  lo     *       0.0.0.0/0            0.0.0.0/0
2     5000  500K ACCEPT     all  --  *      *       0.0.0.0/0            0.0.0.0/0            ctstate RELATED,ESTABLISHED
3       50  3000 ACCEPT     tcp  --  *      *       203.0.113.0/24       0.0.0.0/0            tcp dpt:22 ctstate NEW

# 특정 체인만 보기
$ sudo iptables -L INPUT -n -v

# NAT 테이블
$ sudo iptables -t nat -L -n -v

# 규칙 삭제 (번호로)
$ sudo iptables -D INPUT 3
```

### Stateful 방화벽 (연결 상태 추적)

**연결 상태 (Connection States):**

| 상태 | 의미 | 예시 |
|------|------|------|
| NEW | 새로운 연결 | SYN 패킷 |
| ESTABLISHED | 양방향 통신 설정됨 | 데이터 전송 중 |
| RELATED | 기존 연결과 관련 | FTP 데이터 채널 |
| INVALID | 알 수 없는 연결 | 잘못된 패킷 |

**연결 추적 (Connection Tracking) 테이블:**
```bash
# conntrack 테이블 확인
$ sudo conntrack -L
tcp      6 431999 ESTABLISHED src=192.168.1.100 dst=93.184.216.34 sport=54321 dport=80
         src=93.184.216.34 dst=203.0.113.1 sport=80 dport=10001 [ASSURED] mark=0 use=1

# 통계
$ sudo conntrack -S
cpu=0   found=0 invalid=12 ignore=0 insert=1000 insert_failed=0 drop=0 early_drop=0

# 연결 수 제한 설정
$ sudo sysctl -w net.netfilter.nf_conntrack_max=65536
```

**실무 사례 - Stateful 규칙:**
```bash
# 외부로 나가는 연결 허용, 응답은 자동 허용
iptables -A OUTPUT -m conntrack --ctstate NEW,ESTABLISHED -j ACCEPT
iptables -A INPUT -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT

# 내부에서 시작된 연결만 허용 (외부에서 시작 차단)
iptables -A FORWARD -i eth0 -o eth1 -m conntrack --ctstate NEW,ESTABLISHED,RELATED -j ACCEPT
iptables -A FORWARD -i eth1 -o eth0 -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT

# INVALID 패킷 차단
iptables -A INPUT -m conntrack --ctstate INVALID -j DROP
```

### 애플리케이션 방화벽 (Layer 7)

**WAF (Web Application Firewall):**
- HTTP/HTTPS 트래픽 심층 검사
- SQL 인젝션, XSS 공격 차단
- OWASP Top 10 보호

**ModSecurity (Apache/Nginx WAF) 예시:**

```bash
# ModSecurity 설치 (Nginx)
$ sudo apt install libnginx-mod-security

# 설정 - /etc/nginx/modsec/modsecurity.conf
SecRuleEngine On
SecRequestBodyAccess On
SecResponseBodyAccess Off

# SQL 인젝션 차단
SecRule ARGS "@detectSQLi" \
    "id:1,phase:2,block,msg:'SQL Injection Attack'"

# XSS 차단
SecRule ARGS "@detectXSS" \
    "id:2,phase:2,block,msg:'XSS Attack'"

# 파일 업로드 크기 제한
SecRequestBodyLimit 10485760  # 10MB

# Core Rule Set (CRS) 적용
Include /etc/nginx/modsec/crs/crs-setup.conf
Include /etc/nginx/modsec/crs/rules/*.conf
```

**Nginx 설정:**
```nginx
http {
    modsecurity on;
    modsecurity_rules_file /etc/nginx/modsec/modsecurity.conf;

    server {
        listen 80;
        server_name www.example.com;

        location / {
            modsecurity_rules '
                SecRule ARGS "@streq malicious" "id:10,deny,status:403"
            ';
            proxy_pass http://backend;
        }
    }
}
```

### DDoS 공격 방어

**DDoS 공격 유형:**

1. **Volumetric Attacks**: 대역폭 고갈
   - UDP Flood
   - ICMP Flood
   - DNS Amplification

2. **Protocol Attacks**: 서버 리소스 고갈
   - SYN Flood
   - ACK Flood
   - Ping of Death

3. **Application Layer Attacks**: 애플리케이션 취약점
   - HTTP Flood
   - Slowloris
   - DNS Query Flood

**SYN Flood 방어:**

```bash
# SYN Cookies 활성화
$ sudo sysctl -w net.ipv4.tcp_syncookies=1

# SYN+ACK 재시도 횟수 감소
$ sudo sysctl -w net.ipv4.tcp_synack_retries=2

# SYN 백로그 큐 증가
$ sudo sysctl -w net.ipv4.tcp_max_syn_backlog=4096

# /etc/sysctl.conf에 영구 설정
net.ipv4.tcp_syncookies=1
net.ipv4.tcp_synack_retries=2
net.ipv4.tcp_max_syn_backlog=4096
net.ipv4.tcp_fin_timeout=30
```

**iptables Rate Limiting:**
```bash
# HTTP 연결 제한 (IP당 분당 20개)
iptables -A INPUT -p tcp --dport 80 -m connlimit --connlimit-above 20 -j REJECT

# SYN 패킷 제한 (IP당 초당 3개)
iptables -A INPUT -p tcp --syn -m limit --limit 3/s --limit-burst 10 -j ACCEPT
iptables -A INPUT -p tcp --syn -j DROP

# 특정 IP 차단
iptables -A INPUT -s 198.51.100.10 -j DROP

# 지역 차단 (ipset 사용)
ipset create blacklist hash:net
ipset add blacklist 198.51.100.0/24
iptables -A INPUT -m set --match-set blacklist src -j DROP
```

**fail2ban (자동 차단):**
```bash
# fail2ban 설치
$ sudo apt install fail2ban

# 설정 - /etc/fail2ban/jail.local
[DEFAULT]
bantime = 3600
findtime = 600
maxretry = 5

[sshd]
enabled = true
port = ssh
logpath = /var/log/auth.log

[nginx-http-auth]
enabled = true
port = http,https
logpath = /var/log/nginx/error.log

[nginx-limit-req]
enabled = true
filter = nginx-limit-req
logpath = /var/log/nginx/error.log
maxretry = 10

# 상태 확인
$ sudo fail2ban-client status
$ sudo fail2ban-client status sshd

# 수동 차단 해제
$ sudo fail2ban-client set sshd unbanip 203.0.113.50
```

### 차세대 방화벽 (NGFW)

**NGFW 기능:**
1. **패킷 필터링**: 기본 방화벽 기능
2. **IPS (Intrusion Prevention System)**: 침입 방지
3. **DPI (Deep Packet Inspection)**: 애플리케이션 인식
4. **SSL/TLS 복호화**: 암호화 트래픽 검사
5. **안티바이러스/안티맬웨어**: 악성코드 차단
6. **애플리케이션 제어**: 특정 앱 차단 (P2P, 게임 등)
7. **사용자 인증**: Active Directory 연동

**pfSense (오픈소스 방화벽) 설정 예시:**

```bash
# pfSense CLI 명령
# 규칙 확인
$ pfctl -sr

# NAT 확인
$ pfctl -sn

# 상태 테이블 확인
$ pfctl -ss

# 트래픽 통계
$ pfctl -si

# Snort (IPS) 로그 확인
$ tail -f /var/log/snort/alert
```

**Suricata (IDS/IPS) 규칙:**
```bash
# /etc/suricata/rules/local.rules
# SQL 인젝션 탐지
alert http any any -> any any (msg:"SQL Injection Attempt"; flow:to_server; \
    content:"UNION"; nocase; content:"SELECT"; nocase; sid:1000001;)

# SSH 브루트포스 탐지
alert tcp any any -> any 22 (msg:"SSH Brute Force Attempt"; \
    flow:to_server; flags:S; threshold:type both, track by_src, count 5, seconds 60; sid:1000002;)

# 포트 스캔 탐지
alert tcp any any -> any any (msg:"Port Scan Detected"; \
    flags:S; threshold:type threshold, track by_src, count 100, seconds 10; sid:1000003;)

# Suricata 시작
$ sudo suricata -c /etc/suricata/suricata.yaml -i eth0

# 이벤트 로그 확인
$ tail -f /var/log/suricata/fast.log
```

**실무적 활용 사례:**

> ⚠️ **방화벽 우회 공격**: 공격자가 허용된 포트(80, 443)를 통해 악성 트래픽을 전송합니다. 패킷 필터링만으로는 부족하며, Layer 7 검사(WAF)가 필수입니다.

> ⚠️ **방화벽 규칙 오류**: 잘못된 규칙 순서나 과도하게 넓은 허용 규칙이 보안 취약점이 됩니다. 최소 권한 원칙(Principle of Least Privilege)을 적용하세요.

---

## 2. 로드 밸런서를 통한 부하 분산

### 로드 밸런서의 필요성

**로드 밸런싱 목적:**
1. **가용성 향상**: 서버 장애 시 자동 전환
2. **성능 향상**: 트래픽 분산으로 응답 시간 단축
3. **확장성**: 서버 추가로 용량 증설
4. **유지보수**: 무중단 배포

**로드 밸런서 배치:**
```
[클라이언트]
     ↓
[로드 밸런서] ← 단일 진입점
   /   |   \
  /    |    \
[Web1][Web2][Web3] ← 백엔드 서버
```

### 로드 밸런싱 알고리즘

**1. Round Robin (라운드 로빈):**
```
요청1 → 서버1
요청2 → 서버2
요청3 → 서버3
요청4 → 서버1
요청5 → 서버2
...
```
- 장점: 간단, 공평한 분산
- 단점: 서버 성능 차이 무시

**2. Weighted Round Robin (가중 라운드 로빈):**
```
서버1 (가중치 3): 요청1, 요청2, 요청3
서버2 (가중치 2): 요청4, 요청5
서버3 (가중치 1): 요청6
서버1 (가중치 3): 요청7, 요청8, 요청9
```
- 장점: 서버 성능에 따라 조정 가능

**3. Least Connections (최소 연결):**
```
서버1: 10개 연결
서버2: 5개 연결 ← 선택
서버3: 8개 연결
```
- 장점: 부하 균등 분산
- 단점: 연결 추적 오버헤드

**4. IP Hash (IP 해시):**
```
hash(클라이언트 IP) % 서버 수
203.0.113.50 → hash → 서버2
```
- 장점: 세션 유지 (같은 클라이언트 → 같은 서버)
- 단점: 불균등 분산 가능

**5. Least Response Time (최소 응답 시간):**
- 응답 시간이 가장 짧은 서버 선택
- 장점: 성능 최적화
- 단점: 복잡한 모니터링 필요

### 헬스 체크 (Health Check)

**헬스 체크 방법:**

**1. TCP 연결 확인:**
```bash
# 포트 80 열림 확인
$ nc -zv 192.168.1.10 80
Connection to 192.168.1.10 80 port [tcp/http] succeeded!
```

**2. HTTP 상태 코드 확인:**
```bash
# HTTP GET 요청
$ curl -I http://192.168.1.10/health
HTTP/1.1 200 OK
```

**3. HTTP 응답 내용 확인:**
```bash
# 특정 문자열 포함 확인
$ curl http://192.168.1.10/health | grep "OK"
{"status":"OK"}
```

**헬스 체크 설정 예시:**
```
간격: 5초
타임아웃: 3초
재시도: 3회
연속 성공: 2회 → 정상
연속 실패: 3회 → 비정상 (서버 제거)
```

### HAProxy (고성능 로드 밸런서)

**HAProxy 설치 및 설정:**

```bash
# 설치
$ sudo apt install haproxy

# 설정 - /etc/haproxy/haproxy.cfg
global
    log /dev/log local0
    maxconn 4096
    user haproxy
    group haproxy
    daemon

defaults
    log global
    mode http
    option httplog
    option dontlognull
    timeout connect 5000ms
    timeout client 50000ms
    timeout server 50000ms

# 통계 페이지
listen stats
    bind *:8080
    stats enable
    stats uri /stats
    stats refresh 30s
    stats auth admin:password

# 프론트엔드 (클라이언트 접속점)
frontend http_front
    bind *:80
    bind *:443 ssl crt /etc/ssl/certs/example.com.pem

    # ACL (Access Control List)
    acl is_api path_beg /api
    acl is_static path_beg /static

    # 백엔드 선택
    use_backend api_servers if is_api
    use_backend static_servers if is_static
    default_backend web_servers

# 백엔드 (서버 풀)
backend web_servers
    balance roundrobin
    option httpchk GET /health

    server web1 192.168.1.10:80 check inter 5s fall 3 rise 2
    server web2 192.168.1.11:80 check inter 5s fall 3 rise 2
    server web3 192.168.1.12:80 check inter 5s fall 3 rise 2

backend api_servers
    balance leastconn
    option httpchk GET /api/health

    server api1 192.168.1.20:8080 check weight 2
    server api2 192.168.1.21:8080 check weight 2
    server api3 192.168.1.22:8080 check weight 1

backend static_servers
    balance source  # IP Hash
    server static1 192.168.1.30:80 check
    server static2 192.168.1.31:80 check

# HAProxy 재시작
$ sudo systemctl restart haproxy

# 상태 확인
$ sudo systemctl status haproxy

# 설정 검증
$ haproxy -c -f /etc/haproxy/haproxy.cfg
```

**통계 페이지 접속:**
```
http://your-lb-ip:8080/stats
```

### Nginx 로드 밸런서

```nginx
http {
    # 업스트림 서버 정의
    upstream backend {
        # 로드 밸런싱 알고리즘
        # round-robin (기본값)
        # least_conn;
        # ip_hash;
        # hash $request_uri consistent;

        server 192.168.1.10:80 weight=3 max_fails=3 fail_timeout=30s;
        server 192.168.1.11:80 weight=2;
        server 192.168.1.12:80 weight=1 backup;  # 백업 서버
    }

    server {
        listen 80;
        server_name www.example.com;

        location / {
            proxy_pass http://backend;

            # 프록시 헤더
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;

            # 타임아웃
            proxy_connect_timeout 5s;
            proxy_send_timeout 10s;
            proxy_read_timeout 10s;

            # 버퍼링
            proxy_buffering on;
            proxy_buffer_size 4k;
            proxy_buffers 8 4k;
        }

        # 헬스 체크 엔드포인트
        location /health {
            access_log off;
            return 200 "OK\n";
            add_header Content-Type text/plain;
        }
    }
}
```

### Layer 4 vs Layer 7 로드 밸런싱

**Layer 4 (전송 계층):**
```
TCP/UDP 패킷 헤더만 검사:
  - 출발지/목적지 IP
  - 출발지/목적지 포트

장점:
  - 매우 빠름
  - 낮은 지연
단점:
  - 세션 유지 제한적
  - URL 기반 라우팅 불가
```

**Layer 7 (애플리케이션 계층):**
```
HTTP 헤더 및 내용 검사:
  - Host 헤더
  - URL 경로
  - Cookie
  - HTTP 메서드

장점:
  - 유연한 라우팅
  - 세션 유지 (쿠키)
  - SSL Offloading
단점:
  - 느림
  - 높은 CPU 사용
```

**실무 사례:**
```
Layer 4: 고성능 필요 시 (게임 서버, DNS, VoIP)
Layer 7: 웹 애플리케이션 (HTTP/HTTPS)
```

**AWS 서비스 활용:**

| 로드 밸런서 유형 | AWS 서비스 | 설명 |
|----------------|-----------|------|
| Layer 7 (HTTP/HTTPS) | **ALB (Application Load Balancer)** | 경로/헤더 기반 라우팅 |
| Layer 4 (TCP/UDP) | **NLB (Network Load Balancer)** | 초저지연, 고성능 |
| Classic | **CLB (Classic Load Balancer)** | 레거시, 권장 안 함 |
| 글로벌 가속 | **Global Accelerator** | 최적 엣지로 라우팅 |
| 멀티 리전 | **Route 53 헬스체크** | DNS 기반 장애 조치 |

**실무적 활용 사례:**

> ⚠️ **HTTP Host Header 공격**: 공격자가 Host 헤더를 조작하여 다른 백엔드로 라우팅되게 할 수 있습니다. 허용된 호스트만 처리하도록 설정하세요.

> ⚠️ **세션 하이재킹**: IP Hash 없이 라운드 로빈만 사용하면 세션 쿠키를 탈취한 공격자가 다른 사용자 세션에 접근할 수 있습니다.

### SSL/TLS Offloading (SSL Termination)

**SSL Offloading 이점:**
1. 백엔드 서버 부하 감소
2. 중앙화된 인증서 관리
3. 암호화 트래픽 검사 가능

**HAProxy SSL Offloading:**
```
frontend https_front
    bind *:443 ssl crt /etc/ssl/certs/example.com.pem

    # HTTP Strict Transport Security
    http-response set-header Strict-Transport-Security "max-age=31536000; includeSubDomains"

    # 백엔드로는 평문 HTTP
    default_backend web_servers
```

**Nginx SSL Offloading:**
```nginx
server {
    listen 443 ssl http2;
    server_name www.example.com;

    ssl_certificate /etc/ssl/certs/example.com.crt;
    ssl_certificate_key /etc/ssl/private/example.com.key;

    # SSL 설정
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    location / {
        proxy_pass http://backend;  # HTTP (평문)
    }
}
```

---

## 3. 캐시 서버로 웹 페이지를 캐시한다

### 캐시의 기본 개념

**캐시 (Cache):**
- **목적**: 자주 요청되는 콘텐츠를 임시 저장하여 응답 속도 향상
- **효과**:
  - 서버 부하 감소
  - 네트워크 대역폭 절약
  - 사용자 응답 시간 단축

**캐시 계층:**
```
[브라우저 캐시] ← 1차
     ↓
[프록시 캐시] ← 2차
     ↓
[CDN 캐시] ← 3차
     ↓
[역방향 프록시 캐시] ← 4차
     ↓
[원본 서버]
```

### HTTP 캐시 제어 헤더

**Cache-Control 디렉티브:**

```http
# 클라이언트 요청
GET /image.jpg HTTP/1.1
Host: www.example.com
Cache-Control: max-age=3600

# 서버 응답
HTTP/1.1 200 OK
Cache-Control: public, max-age=86400
ETag: "abc123"
Last-Modified: Sat, 04 Jan 2026 10:00:00 GMT
Content-Type: image/jpeg

<image data>
```

**Cache-Control 값:**

| 디렉티브 | 의미 | 사용처 |
|---------|------|--------|
| public | 모든 캐시 가능 | 정적 리소스 |
| private | 브라우저만 캐시 | 개인 정보 |
| no-cache | 재검증 필요 | 동적 콘텐츠 |
| no-store | 캐시 금지 | 민감 정보 |
| max-age=N | N초 동안 신선 | 모든 콘텐츠 |
| s-maxage=N | 공유 캐시 max-age | 프록시 |
| must-revalidate | 만료 후 재검증 필수 | 중요 콘텐츠 |

**조건부 요청 (Conditional Request):**

```http
# 1차 요청
GET /data.json HTTP/1.1
Host: api.example.com

HTTP/1.1 200 OK
Cache-Control: max-age=60
ETag: "v1.0"
Last-Modified: Sat, 04 Jan 2026 10:00:00 GMT
{"data":"value"}

# 2차 요청 (60초 후)
GET /data.json HTTP/1.1
Host: api.example.com
If-None-Match: "v1.0"
If-Modified-Since: Sat, 04 Jan 2026 10:00:00 GMT

# 변경 없음
HTTP/1.1 304 Not Modified
ETag: "v1.0"
(본문 없음 - 대역폭 절약)

# 또는 변경됨
HTTP/1.1 200 OK
ETag: "v2.0"
Last-Modified: Sat, 04 Jan 2026 11:00:00 GMT
{"data":"new_value"}
```

### Varnish (고성능 HTTP 캐시)

**Varnish 설치 및 설정:**

```bash
# 설치
$ sudo apt install varnish

# 설정 - /etc/varnish/default.vcl
vcl 4.1;

# 백엔드 정의
backend default {
    .host = "192.168.1.10";
    .port = "80";
    .connect_timeout = 600s;
    .first_byte_timeout = 600s;
    .between_bytes_timeout = 600s;
}

# 수신 처리
sub vcl_recv {
    # POST 요청은 캐시 안 함
    if (req.method == "POST") {
        return (pass);
    }

    # 쿠키 제거 (정적 리소스)
    if (req.url ~ "\.(jpg|jpeg|png|gif|css|js|ico)$") {
        unset req.http.Cookie;
    }

    # 관리자 페이지는 캐시 안 함
    if (req.url ~ "^/admin") {
        return (pass);
    }

    return (hash);
}

# 백엔드 응답 처리
sub vcl_backend_response {
    # TTL 설정
    if (bereq.url ~ "\.(jpg|jpeg|png|gif)$") {
        set beresp.ttl = 7d;  # 이미지: 7일
    } elsif (bereq.url ~ "\.(css|js)$") {
        set beresp.ttl = 1d;  # CSS/JS: 1일
    } else {
        set beresp.ttl = 1h;  # 기타: 1시간
    }

    # 캐시 가능 여부 확인
    if (beresp.status == 404) {
        set beresp.ttl = 5m;  # 404: 5분
    }

    return (deliver);
}

# 클라이언트 전달 시
sub vcl_deliver {
    # 캐시 히트 여부 표시
    if (obj.hits > 0) {
        set resp.http.X-Cache = "HIT";
        set resp.http.X-Cache-Hits = obj.hits;
    } else {
        set resp.http.X-Cache = "MISS";
    }

    return (deliver);
}

# Varnish 재시작
$ sudo systemctl restart varnish

# 통계 확인
$ varnishstat

# 로그 확인
$ varnishlog
$ varnishncsa  # Apache 스타일 로그
```

**Varnish 성능 모니터링:**
```bash
# 실시간 통계
$ varnishstat -1
cache_hit         1234567  .  Cache hits
cache_miss          12345  .  Cache misses
client_req        1246912  .  Client requests

# 히트율 계산
Hit Rate = cache_hit / (cache_hit + cache_miss) × 100%
         = 1234567 / (1234567 + 12345) × 100%
         = 99.0%

# 캐시 삭제 (purge)
$ varnishadm "ban req.url ~ /path/to/page"
```

### Nginx 캐시

```nginx
http {
    # 캐시 경로 설정
    proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=my_cache:10m max_size=1g inactive=60m use_temp_path=off;

    server {
        listen 80;
        server_name www.example.com;

        location / {
            proxy_pass http://backend;

            # 캐시 활성화
            proxy_cache my_cache;

            # 캐시 키
            proxy_cache_key "$scheme$request_method$host$request_uri";

            # 캐시 유효 시간
            proxy_cache_valid 200 302 10m;
            proxy_cache_valid 404 1m;

            # 캐시 우회 조건
            proxy_cache_bypass $cookie_nocache $arg_nocache;

            # 캐시 상태 헤더 추가
            add_header X-Cache-Status $upstream_cache_status;

            # 캐시 잠금 (동일 요청 중복 방지)
            proxy_cache_lock on;
            proxy_cache_lock_timeout 5s;
        }

        # 정적 파일 캐시
        location ~* \.(jpg|jpeg|png|gif|ico|css|js)$ {
            expires 30d;
            add_header Cache-Control "public, immutable";
        }

        # 캐시 삭제 엔드포인트
        location ~ /purge(/.*) {
            allow 127.0.0.1;
            deny all;
            proxy_cache_purge my_cache "$scheme$request_method$host$1";
        }
    }
}
```

**캐시 삭제:**
```bash
# 특정 URL 캐시 삭제
$ curl -X PURGE http://127.0.0.1/purge/path/to/page

# 전체 캐시 삭제
$ sudo rm -rf /var/cache/nginx/*
```

### 캐시 전략

**1. Cache-Aside (Lazy Loading):**
```
1. 캐시 확인 → 있으면 반환
2. 캐시 없으면 → DB 조회
3. 조회 결과를 캐시에 저장
4. 결과 반환
```
- 장점: 필요한 데이터만 캐싱, 캐시 장애 시에도 동작
- 단점: 첫 요청은 느림 (cache miss)

**2. Write-Through:**
```
1. DB 업데이트
2. 캐시 업데이트 (동기)
```
- 장점: 캐시와 DB 항상 일치
- 단점: 쓰기 지연 발생

**3. Write-Behind (Write-Back):**
```
1. 캐시에만 저장 (빠름)
2. 비동기로 DB 업데이트
```
- 장점: 쓰기 성능 우수
- 단점: 캐시 장애 시 데이터 손실 위험

**AWS 서비스 활용:**

| 캐시 전략 | AWS 서비스 | 설명 |
|----------|-----------|------|
| In-Memory 캐시 | **ElastiCache (Redis/Memcached)** | 고성능 분산 캐시 |
| 세션 스토어 | **ElastiCache** | 세션 데이터 저장 |
| DB 캐싱 | **DAX (DynamoDB Accelerator)** | DynamoDB 전용 캐시 |
| CDN 캐시 | **CloudFront** | 정적/동적 콘텐츠 캐시 |

**실무적 활용 사례:**

> ⚠️ **캐시 침투 공격 (Cache Penetration)**: 존재하지 않는 키를 대량으로 요청하면 모든 요청이 DB로 전달되어 부하가 급증합니다. Bloom Filter나 빈 결과 캐싱으로 방어합니다.

> ⚠️ **캐시 스탬피드 (Cache Stampede)**: 인기 있는 캐시가 만료되는 순간 대량의 요청이 동시에 DB로 몰립니다. 캐시 락이나 점진적 갱신으로 방어합니다.

---

## 4. CDN과 콘텐츠 전송 최적화

### CDN (Content Delivery Network) 개념

**CDN의 필요성:**
1. **지연 시간 감소**: 사용자와 가까운 서버에서 콘텐츠 제공
2. **서버 부하 분산**: 원본 서버 부하 감소
3. **대역폭 절약**: ISP 트래픽 비용 절감
4. **가용성 향상**: 장애 대응, DDoS 방어

**CDN 구조:**
```
          [원본 서버]
               |
    +-----------+-----------+
    |           |           |
[엣지 서버]  [엣지 서버]  [엣지 서버]
  서울        도쿄        LA
    |           |           |
[한국 사용자] [일본 사용자] [미국 사용자]
```

**주요 CDN 제공업체:**
- Cloudflare
- Akamai
- Amazon CloudFront
- Fastly
- Google Cloud CDN
- Azure CDN

### CDN 동작 원리

**콘텐츠 요청 흐름:**

```
1. 사용자 요청:
   https://cdn.example.com/image.jpg

2. DNS 조회:
   cdn.example.com → 가장 가까운 엣지 서버 IP

3. 엣지 서버 확인:
   캐시 있음 → 즉시 응답 (캐시 히트)
   캐시 없음 → 원본 서버 요청 (캐시 미스)

4. 원본 서버 요청 (캐시 미스 시):
   엣지 → 원본 서버 → 콘텐츠 가져오기
   엣지에 캐시 저장

5. 사용자에게 응답:
   엣지 서버 → 사용자
```

**GeoDNS (지리적 DNS):**
```bash
# 사용자 위치에 따라 다른 IP 반환
$ dig cdn.example.com
# 한국에서 조회:
cdn.example.com. 300 IN A 203.0.113.10  # 서울 엣지

$ dig cdn.example.com
# 미국에서 조회:
cdn.example.com. 300 IN A 198.51.100.20  # LA 엣지
```

### Cloudflare 설정 예시

**1. DNS 설정:**
```
도메인: example.com

DNS 레코드:
  www.example.com  CNAME  example.com  (Proxied ☁️)
  example.com      A      203.0.113.1  (Proxied ☁️)
  api.example.com  A      203.0.113.2  (DNS only)
```

**2. 캐시 규칙 (Edge Computing):**

엣지 컴퓨팅을 사용하면 사용자에게 가장 가까운 위치에서 코드를 실행할 수 있습니다.

| 엣지 컴퓨팅 | AWS 서비스 | 설명 |
|------------|-----------|------|
| 엣지 함수 | **Lambda@Edge** | CloudFront 엣지에서 실행 |
| 경량 함수 | **CloudFront Functions** | 간단한 변환/리다이렉트 |
| 엣지 캐시 | **CloudFront** | 전 세계 엣지 로케이션 |
| 원본 보호 | **Origin Shield** | 원본 서버 부하 감소 |

**3. 페이지 규칙:**
```
URL: example.com/static/*
설정:
  - 캐시 레벨: 모두 캐시
  - 엣지 캐시 TTL: 7일
  - 브라우저 캐시 TTL: 1일

URL: example.com/api/*
설정:
  - 캐시 레벨: 캐시 안 함
  - SSL: 전체 (엄격)
```

### 원본 서버 보호 (Origin Shield)

**Origin Shield:**
- CDN 엣지와 원본 서버 사이에 추가 캐시 계층
- 원본 서버 부하 최소화

```
[사용자]
   ↓
[엣지 서버 1] ⟍
[엣지 서버 2] ⟶ [Origin Shield] → [원본 서버]
[엣지 서버 3] ⟋
```

**CloudFront Origin Shield 설정:**
```json
{
  "Origins": [{
    "Id": "my-origin",
    "DomainName": "origin.example.com",
    "OriginShield": {
      "Enabled": true,
      "OriginShieldRegion": "ap-northeast-2"
    }
  }]
}
```

### 이미지 최적화

**자동 이미지 최적화 (Cloudflare):**
```html
<!-- 원본 이미지 -->
<img src="https://example.com/image.jpg" alt="photo">

<!-- Cloudflare가 자동으로: -->
- WebP 변환 (지원 브라우저)
- 크기 조정
- 메타데이터 제거
- 압축
```

**URL 파라미터로 이미지 조정:**
```
https://cdn.example.com/image.jpg?width=300&quality=80&format=webp
```

**imgproxy (자체 호스팅 이미지 프록시):**
```bash
# imgproxy 실행
$ docker run -p 8080:8080 -e IMGPROXY_KEY=secret darthsim/imgproxy

# 이미지 요청
# /<signature>/<resize>/<gravity>/<url>
https://imgproxy.example.com/insecure/resize:fill:300:200/plain/https://example.com/image.jpg
```

### CDN 성능 모니터링

**Cloudflare Analytics:**
```
지표:
  - 요청 수
  - 대역폭 절약 (캐시 히트율)
  - 응답 시간
  - 오류율 (4xx, 5xx)
  - 위협 차단 (방화벽)
```

**AWS 서비스 활용:**

| CDN 기능 | AWS 서비스 | 설명 |
|---------|-----------|------|
| CDN | **CloudFront** | 전 세계 400+ 엣지 로케이션 |
| 원본 보호 | **Origin Shield** | 원본 요청 최소화 |
| 실시간 로그 | **CloudFront 실시간 로그** | Kinesis Data Streams 연동 |
| 엣지 컴퓨팅 | **Lambda@Edge / CloudFront Functions** | 엣지에서 코드 실행 |
| 보안 | **CloudFront + WAF + Shield** | 통합 보안 |

**실무적 활용 사례:**

> ⚠️ **캐시 포이즈닝 (Cache Poisoning)**: 공격자가 악성 콘텐츠를 CDN에 캐시시켜 다른 사용자에게 전달합니다. 캐시 키에 Host 헤더를 포함하고, 입력값을 검증해야 합니다.

**실시간 로그 분석:**
```bash
# Cloudflare Logpush (S3, BigQuery 등으로 전송)
{
  "ClientIP": "203.0.113.50",
  "ClientRequestHost": "www.example.com",
  "ClientRequestURI": "/image.jpg",
  "EdgeResponseStatus": 200,
  "EdgeStartTimestamp": 1704355200,
  "CacheResponseStatus": "hit",
  "EdgeServerIP": "104.16.132.229"
}
```

---

## 5. 프록시 서버의 동작

### 프록시 서버 유형

**Forward Proxy (정방향 프록시):**
```
[클라이언트] → [프록시] → [인터넷] → [서버]
               ↑
          클라이언트가 설정
```
- 용도:
  - 익명성 보장
  - 콘텐츠 필터링
  - 캐싱
  - 우회 접속

**Reverse Proxy (역방향 프록시):**
```
[클라이언트] → [인터넷] → [프록시] → [백엔드 서버]
                          ↑
                    서버 측에서 설정
```
- 용도:
  - 로드 밸런싱
  - SSL/TLS 종료
  - 캐싱
  - 보안 (백엔드 숨김)

### Squid (Forward Proxy)

```bash
# Squid 설치
$ sudo apt install squid

# 설정 - /etc/squid/squid.conf
# ACL 정의
acl localnet src 192.168.1.0/24
acl Safe_ports port 80 443
acl CONNECT method CONNECT

# 접근 규칙
http_access deny !Safe_ports
http_access deny CONNECT !Safe_ports
http_access allow localnet
http_access deny all

# 프록시 포트
http_port 3128

# 캐시 설정
cache_dir ufs /var/spool/squid 10000 16 256
maximum_object_size 50 MB
cache_mem 256 MB

# 로그
access_log /var/log/squid/access.log squid

# Squid 재시작
$ sudo systemctl restart squid

# 클라이언트 설정 (프록시 사용)
$ export http_proxy=http://proxy.example.com:3128
$ export https_proxy=http://proxy.example.com:3128

# 또는
$ curl -x http://proxy.example.com:3128 http://www.example.com
```

**Squid 투명 프록시 (Transparent Proxy):**
```bash
# iptables 리다이렉션
$ sudo iptables -t nat -A PREROUTING -i eth1 -p tcp --dport 80 -j REDIRECT --to-port 3128

# Squid 설정
http_port 3128 intercept
```

### SOCKS 프록시

**SOCKS5 프록시 (Dante):**
```bash
# Dante 설치
$ sudo apt install dante-server

# 설정 - /etc/danted.conf
logoutput: /var/log/danted.log

internal: 0.0.0.0 port = 1080
external: eth0

socksmethod: username
user.privileged: root
user.unprivileged: nobody

client pass {
    from: 192.168.1.0/24 to: 0.0.0.0/0
}

socks pass {
    from: 192.168.1.0/24 to: 0.0.0.0/0
    protocol: tcp udp
}

# 재시작
$ sudo systemctl restart danted

# 클라이언트 사용
$ curl --socks5 proxy.example.com:1080 http://www.example.com
```

### HTTP 프록시 헤더

**프록시 관련 HTTP 헤더:**

```http
# 클라이언트 → 프록시
GET http://www.example.com/path HTTP/1.1
Host: www.example.com
Proxy-Authorization: Basic dXNlcjpwYXNz
Via: 1.1 proxy1.example.com

# 프록시 → 서버
GET /path HTTP/1.1
Host: www.example.com
X-Forwarded-For: 203.0.113.50  # 클라이언트 IP
X-Forwarded-Proto: https
X-Forwarded-Host: www.example.com
X-Real-IP: 203.0.113.50
Via: 1.1 proxy1.example.com, 1.1 proxy2.example.com
```

**Nginx 프록시 헤더 설정:**
```nginx
location / {
    proxy_pass http://backend;

    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Host $host;
    proxy_set_header X-Forwarded-Port $server_port;
}
```

### 프록시 체인

**다단계 프록시:**
```
[클라이언트] → [프록시1] → [프록시2] → [프록시3] → [서버]

Via 헤더:
1.1 proxy1.example.com, 1.1 proxy2.example.com, 1.1 proxy3.example.com
```

**프록시 체인 설정 (ProxyChains):**
```bash
# ProxyChains 설치
$ sudo apt install proxychains4

# 설정 - /etc/proxychains4.conf
strict_chain
proxy_dns

[ProxyList]
socks5  127.0.0.1 9050  # Tor
http    proxy.example.com 3128
socks5  proxy2.example.com 1080

# 사용
$ proxychains4 curl http://www.example.com
$ proxychains4 ssh user@server.com
```

**실무적 활용 사례:**

> ⚠️ **오픈 프록시 악용**: 설정이 잘못된 프록시 서버가 익명 공격에 악용됩니다. 반드시 인증을 설정하고 접근을 제한하세요.

> ⚠️ **SSRF (Server-Side Request Forgery)**: 프록시를 통해 내부 네트워크의 서비스에 접근할 수 있습니다. 요청 URL을 검증하고 내부 IP 대역 접근을 차단하세요.

---

## 실무 팁

### 1. 캐시 전략 선택 가이드

| 콘텐츠 타입 | 캐시 전략 | TTL | 이유 |
|------------|----------|-----|------|
| HTML 페이지 | no-cache, must-revalidate | - | 자주 변경 |
| CSS/JS | public, max-age | 1일~1주 | 버전 관리 가능 |
| 이미지 | public, immutable | 1년 | 거의 변경 없음 |
| API 응답 | private, max-age | 5분~1시간 | 개인화 데이터 |
| 동영상 | public, max-age | 1주~1달 | 크기 크고 정적 |

### 2. 보안 체크리스트

```bash
# 1. 방화벽 규칙 최소화 원칙
- 기본 정책: 모두 차단 (deny all)
- 필요한 것만 명시적 허용

# 2. SSL/TLS 강화
- TLS 1.2 이상만 허용
- 약한 암호화 알고리즘 비활성화

# 3. DDoS 방어
- Rate limiting
- SYN cookies
- fail2ban

# 4. 로그 모니터링
- 실시간 로그 분석
- 이상 징후 탐지
- 정기적 리뷰
```

### 3. 성능 최적화

```bash
# Nginx 튜닝
worker_processes auto;
worker_connections 10000;
keepalive_timeout 65;
gzip on;
gzip_types text/plain text/css application/json application/javascript;

# 커널 튜닝
sysctl -w net.core.somaxconn=65535
sysctl -w net.ipv4.tcp_max_syn_backlog=8192
sysctl -w net.ipv4.tcp_fin_timeout=30
```

### 4. 모니터링 도구

```bash
# 실시간 트래픽 모니터링
$ iftop -i eth0
$ nethogs

# 로그 분석
$ tail -f /var/log/nginx/access.log | grep -E "HTTP/[0-9.]+ [45]"

# 성능 측정
$ ab -n 10000 -c 100 http://www.example.com/
$ wrk -t12 -c400 -d30s http://www.example.com/
```

---

## AWS 서비스 전체 요약

| 네트워크 개념 | AWS 서비스 | 설명 |
|-------------|-----------|------|
| 패킷 필터링 방화벽 | **Security Group** | 인스턴스 레벨 Stateful 방화벽 |
| 서브넷 방화벽 | **NACL** | 서브넷 레벨 Stateless 방화벽 |
| 웹 방화벽 (WAF) | **AWS WAF** | SQL Injection, XSS 방어 |
| IDS/IPS | **Network Firewall** | VPC 레벨 침입 탐지/방지 |
| DDoS 방어 | **AWS Shield** | L3/L4/L7 DDoS 보호 |
| L7 로드 밸런서 | **ALB (Application Load Balancer)** | HTTP/HTTPS 라우팅 |
| L4 로드 밸런서 | **NLB (Network Load Balancer)** | TCP/UDP 고성능 로드밸런싱 |
| 글로벌 로드 밸런싱 | **Global Accelerator** | AWS 백본 활용 가속 |
| DNS 기반 라우팅 | **Route 53** | 지리적/가중치 기반 라우팅 |
| CDN | **CloudFront** | 전 세계 400+ 엣지 로케이션 |
| 원본 보호 | **Origin Shield** | CDN-원본 간 캐시 계층 |
| 엣지 컴퓨팅 | **Lambda@Edge** | CloudFront 엣지에서 코드 실행 |
| 경량 엣지 함수 | **CloudFront Functions** | 간단한 변환/리다이렉트 |
| In-Memory 캐시 | **ElastiCache (Redis)** | 고성능 분산 캐시 |
| DynamoDB 캐시 | **DAX** | DynamoDB 전용 가속기 |
| SSL/TLS 인증서 | **ACM (Certificate Manager)** | 무료 SSL 인증서 관리 |
| 프라이빗 연결 | **PrivateLink** | VPC 간 프라이빗 연결 |
| API 게이트웨이 | **API Gateway** | API 관리, 인증, 캐싱 |
| 콘텐츠 가속 | **S3 Transfer Acceleration** | 글로벌 업로드 가속 |

---

*마지막 업데이트: 2026년 1월*
