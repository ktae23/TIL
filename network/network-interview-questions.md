# 네트워크 면접 핵심 질문 정리

5년차 백엔드 개발자 면접에서 자주 등장하는 네트워크 핵심 질문과 답변을 정리합니다.

## 목차

1. [OSI 7계층과 TCP/IP](#1-osi-7계층과-tcpip)
2. [HTTP 버전 비교](#2-http-버전-비교)
3. [TCP 3-way/4-way Handshake](#3-tcp-3-way4-way-handshake)
4. [TIME_WAIT 상태](#4-time_wait-상태)
5. [HTTPS와 TLS](#5-https와-tls)
6. [로드밸런서와 L4/L7](#6-로드밸런서와-l4l7)

---

## 1. OSI 7계층과 TCP/IP

### Q: OSI 7계층을 설명하고 각 계층의 역할은?

```
┌─────────────────────────────────────────────────────────────┐
│  OSI 7계층           │  TCP/IP 4계층    │  프로토콜/장비    │
├─────────────────────────────────────────────────────────────┤
│  7. Application      │                  │  HTTP, FTP, DNS   │
│  6. Presentation     │  Application     │  SSL/TLS, JPEG    │
│  5. Session          │                  │  NetBIOS, RPC     │
├─────────────────────────────────────────────────────────────┤
│  4. Transport        │  Transport       │  TCP, UDP         │
├─────────────────────────────────────────────────────────────┤
│  3. Network          │  Internet        │  IP, ICMP, Router │
├─────────────────────────────────────────────────────────────┤
│  2. Data Link        │  Network Access  │  Ethernet, Switch │
│  1. Physical         │                  │  케이블, 허브     │
└─────────────────────────────────────────────────────────────┘
```

**각 계층 역할**

| 계층 | 핵심 역할 | 데이터 단위 |
|------|----------|------------|
| Application | 사용자 인터페이스, 프로토콜 | 메시지 |
| Presentation | 암호화, 압축, 인코딩 | 메시지 |
| Session | 세션 관리, 동기화 | 메시지 |
| Transport | 종단간 통신, 흐름 제어 | 세그먼트/데이터그램 |
| Network | 라우팅, 논리 주소(IP) | 패킷 |
| Data Link | MAC 주소, 에러 검출 | 프레임 |
| Physical | 비트 전송, 물리적 연결 | 비트 |

**패킷 캡슐화 과정**
```
[Application]  HTTP 요청 생성
      ↓ + HTTP 헤더
[Transport]    TCP 세그먼트
      ↓ + TCP 헤더 (포트, 시퀀스)
[Network]      IP 패킷
      ↓ + IP 헤더 (출발지/목적지 IP)
[Data Link]    이더넷 프레임
      ↓ + MAC 헤더 + 트레일러
[Physical]     전기 신호로 변환 → 전송
```

---

## 2. HTTP 버전 비교

### Q: HTTP/1.1, HTTP/2, HTTP/3의 차이점은?

**HTTP/1.1**
```
특징:
- Connection: Keep-Alive (연결 재사용)
- 파이프라이닝 (요청 연속 전송, 응답은 순서대로)
- Head-of-Line Blocking 문제

┌────────────────────────────────────────┐
│  요청1 → 요청2 → 요청3                 │
│  ↓                                     │
│  응답1 (오래 걸림) → 응답2 → 응답3      │  ← 응답1 대기
└────────────────────────────────────────┘
```

**HTTP/2**
```
특징:
- 바이너리 프레이밍 (텍스트 → 바이너리)
- 멀티플렉싱 (하나의 연결에서 다중 스트림)
- 헤더 압축 (HPACK)
- 서버 푸시
- 스트림 우선순위

┌────────────────────────────────────────┐
│  단일 TCP 연결                         │
│  ├── Stream 1: 요청/응답               │
│  ├── Stream 2: 요청/응답  ← 병렬 처리  │
│  └── Stream 3: 요청/응답               │
└────────────────────────────────────────┘

문제: TCP 레벨 HOL Blocking
패킷 손실 시 모든 스트림 대기
```

**HTTP/3**
```
특징:
- QUIC 프로토콜 (UDP 기반)
- 독립적 스트림 (패킷 손실이 다른 스트림에 영향 없음)
- 0-RTT 연결 설정
- 연결 마이그레이션 (IP 변경에도 연결 유지)

┌────────────────────────────────────────┐
│  QUIC 연결 (UDP)                       │
│  ├── Stream 1: 독립적 전송             │
│  ├── Stream 2: 패킷 손실 → 이것만 대기 │
│  └── Stream 3: 영향 없이 진행          │
└────────────────────────────────────────┘
```

**버전별 비교**

| 특성 | HTTP/1.1 | HTTP/2 | HTTP/3 |
|------|----------|--------|--------|
| 전송 계층 | TCP | TCP | QUIC (UDP) |
| 멀티플렉싱 | X | O | O |
| 헤더 압축 | X | HPACK | QPACK |
| HOL Blocking | O (HTTP) | △ (TCP) | X |
| 연결 설정 | 1-2 RTT | 1-2 RTT | 0-1 RTT |
| 서버 푸시 | X | O | O |

---

## 3. TCP 3-way/4-way Handshake

### Q: TCP 연결 수립과 종료 과정을 설명해주세요.

**3-way Handshake (연결 수립)**
```
Client                              Server
   │                                   │
   │────── SYN (seq=x) ───────────────→│  1. 연결 요청
   │                                   │     SYN_SENT
   │                                   │
   │←───── SYN+ACK (seq=y, ack=x+1) ───│  2. 요청 수락
   │       SYN_RCVD                    │
   │                                   │
   │────── ACK (ack=y+1) ─────────────→│  3. 연결 확립
   │       ESTABLISHED                 │     ESTABLISHED
   │                                   │

왜 3-way?
- 양방향 통신 가능 확인
- 초기 시퀀스 번호 교환
- 2-way면 서버가 클라이언트 수신 가능 여부 모름
```

**4-way Handshake (연결 종료)**
```
Client                              Server
   │                                   │
   │────── FIN ───────────────────────→│  1. 종료 요청
   │       FIN_WAIT_1                  │     CLOSE_WAIT
   │                                   │
   │←───── ACK ────────────────────────│  2. 수신 확인
   │       FIN_WAIT_2                  │     (데이터 전송 가능)
   │                                   │
   │←───── FIN ────────────────────────│  3. 서버도 종료
   │       TIME_WAIT                   │     LAST_ACK
   │                                   │
   │────── ACK ───────────────────────→│  4. 최종 확인
   │       (2MSL 대기)                 │     CLOSED
   │       CLOSED                      │

왜 4-way?
- 양방향 독립적 종료 (Half-Close)
- 한쪽이 종료해도 다른 쪽은 데이터 전송 가능
```

---

## 4. TIME_WAIT 상태

### Q: TIME_WAIT 상태가 무엇이고 왜 필요한가요?

**TIME_WAIT 목적**
```
1. 지연된 패킷 처리
┌────────────────────────────────────────────────────────────┐
│  연결 A (port 5000) 종료                                   │
│  ↓                                                         │
│  바로 연결 B (같은 port 5000) 생성                         │
│  ↓                                                         │
│  연결 A의 지연된 패킷 도착 → 연결 B가 잘못 수신!           │
│                                                            │
│  해결: TIME_WAIT 동안 같은 포트 재사용 금지                │
└────────────────────────────────────────────────────────────┘

2. 마지막 ACK 유실 대비
┌────────────────────────────────────────────────────────────┐
│  Client → Server: ACK (마지막)                             │
│  ↓ 유실                                                    │
│  Server: ACK 안 옴 → FIN 재전송                            │
│  Client: TIME_WAIT 상태 → 재전송된 FIN에 ACK 응답 가능     │
└────────────────────────────────────────────────────────────┘
```

**TIME_WAIT 문제와 해결**
```bash
# 문제: 많은 연결 생성/종료 시 포트 고갈
netstat -an | grep TIME_WAIT | wc -l
# 수만 개의 TIME_WAIT 소켓

# 해결 1: SO_REUSEADDR 옵션
# TIME_WAIT 상태 소켓의 포트 재사용 허용

# 해결 2: tcp_tw_reuse (Linux)
sysctl -w net.ipv4.tcp_tw_reuse=1
# 새 연결 시 TIME_WAIT 소켓 재사용

# 해결 3: Connection Pool
# 연결을 재사용하여 TIME_WAIT 발생 자체를 줄임

# 해결 4: 서버가 먼저 연결 종료하지 않게 설계
# TIME_WAIT는 먼저 FIN을 보낸 쪽에서 발생
```

**TIME_WAIT 지속 시간**
```
2 * MSL (Maximum Segment Lifetime)
- Linux: 기본 60초 (MSL = 30초)
- 왕복 시간의 최대치를 고려

# 확인
cat /proc/sys/net/ipv4/tcp_fin_timeout
```

---

## 5. HTTPS와 TLS

### Q: HTTPS 연결 과정(TLS Handshake)을 설명해주세요.

**TLS 1.2 Handshake**
```
Client                                     Server
   │                                          │
   │─── ClientHello ─────────────────────────→│
   │    (지원 암호화 스위트, 랜덤값)           │
   │                                          │
   │←── ServerHello ──────────────────────────│
   │    (선택된 암호화 스위트, 랜덤값)         │
   │←── Certificate (서버 인증서) ────────────│
   │←── ServerKeyExchange ────────────────────│
   │←── ServerHelloDone ──────────────────────│
   │                                          │
   │─── ClientKeyExchange ───────────────────→│
   │    (Pre-Master Secret, 서버 공개키로 암호화)
   │─── ChangeCipherSpec ────────────────────→│
   │─── Finished ────────────────────────────→│
   │                                          │
   │←── ChangeCipherSpec ─────────────────────│
   │←── Finished ─────────────────────────────│
   │                                          │
   │═══ 암호화된 통신 시작 (대칭키 사용) ═════│

RTT: 2회
```

**TLS 1.3 Handshake (개선)**
```
Client                                     Server
   │                                          │
   │─── ClientHello + KeyShare ──────────────→│
   │    (지원 암호화 + DH 공개키 미리 전송)    │
   │                                          │
   │←── ServerHello + KeyShare ───────────────│
   │←── EncryptedExtensions ──────────────────│
   │←── Certificate ──────────────────────────│
   │←── CertificateVerify ────────────────────│
   │←── Finished ─────────────────────────────│
   │                                          │
   │─── Finished ────────────────────────────→│
   │                                          │
   │═══ 암호화된 통신 시작 ═══════════════════│

RTT: 1회 (0-RTT도 지원)
```

**인증서 검증 과정**
```
1. 서버가 인증서 전송
   ├── 서버 공개키
   ├── 도메인 정보
   └── CA 서명

2. 클라이언트 검증
   ├── CA 인증서로 서명 검증
   ├── 인증서 체인 검증 (Root CA까지)
   ├── 도메인 일치 확인
   └── 유효기간 확인

3. 키 교환
   ├── ECDHE (Elliptic Curve Diffie-Hellman)
   └── 세션 키 생성 (대칭키)

4. 대칭키로 데이터 암호화
   └── AES-GCM 등 사용
```

---

## 6. 로드밸런서와 L4/L7

### Q: L4 로드밸런서와 L7 로드밸런서의 차이는?

**L4 로드밸런서 (Transport Layer)**
```
┌─────────────────────────────────────────────────────────────┐
│  L4 Load Balancer                                           │
│                                                             │
│  판단 기준:                                                 │
│  - IP 주소                                                  │
│  - 포트 번호                                                │
│  - TCP/UDP 정보                                             │
│                                                             │
│  패킷 내용(HTTP 헤더, URL 등)은 확인하지 않음               │
└─────────────────────────────────────────────────────────────┘

동작 방식:
┌──────────┐       ┌──────────┐       ┌──────────┐
│  Client  │──────→│  L4 LB   │──────→│ Server 1 │
│          │       │          │──────→│ Server 2 │
│          │       │(IP/Port) │──────→│ Server 3 │
└──────────┘       └──────────┘       └──────────┘

특징:
- 빠름 (패킷 내용 해석 불필요)
- 단순한 분산 (Round Robin, Least Connection)
- TCP 연결 유지 가능 (DSR)
- 예: AWS NLB, HAProxy (TCP 모드)
```

**L7 로드밸런서 (Application Layer)**
```
┌─────────────────────────────────────────────────────────────┐
│  L7 Load Balancer                                           │
│                                                             │
│  판단 기준:                                                 │
│  - URL 경로 (/api/*, /images/*)                            │
│  - HTTP 헤더 (Host, Cookie, User-Agent)                    │
│  - HTTP 메서드 (GET, POST)                                 │
│  - 요청 본문                                                │
│                                                             │
│  패킷 내용을 해석하여 라우팅                                │
└─────────────────────────────────────────────────────────────┘

동작 방식:
┌──────────┐       ┌──────────┐       ┌──────────┐
│  Client  │──────→│  L7 LB   │──────→│  API     │ /api/*
│  /api/v1 │       │          │──────→│  Static  │ /static/*
│          │       │(HTTP 해석)│──────→│  Legacy  │ /old/*
└──────────┘       └──────────┘       └──────────┘

특징:
- L4보다 느림 (HTTP 파싱 필요)
- 세밀한 라우팅 가능
- SSL Termination 가능
- 캐싱, 압축, 인증 추가 가능
- 예: AWS ALB, Nginx, HAProxy (HTTP 모드)
```

**비교**

| 특성 | L4 | L7 |
|------|----|----|
| 계층 | Transport | Application |
| 속도 | 빠름 | 상대적 느림 |
| 라우팅 | IP/Port | URL/Header/Cookie |
| SSL | Pass-through | Termination 가능 |
| 세션 유지 | IP 해시 | Cookie 기반 |
| 사용 사례 | TCP 기반 서비스 | HTTP 웹 서비스 |

**부하 분산 알고리즘**
```
1. Round Robin
   요청을 순서대로 분배

2. Least Connection
   현재 연결 수가 가장 적은 서버로

3. IP Hash
   클라이언트 IP 기반 (세션 유지)

4. Weighted
   서버 성능에 따라 가중치 부여

5. Least Response Time
   응답 시간이 가장 빠른 서버로
```

---

## 핵심 정리

| 주제 | 핵심 키워드 |
|------|-------------|
| OSI 7계층 | 캡슐화, PDU, TCP/IP 4계층 매핑 |
| HTTP 버전 | 멀티플렉싱, HOL Blocking, QUIC/UDP |
| TCP Handshake | 3-way(SYN/ACK), 4-way(FIN/ACK), 시퀀스 번호 |
| TIME_WAIT | 2MSL, 포트 고갈, SO_REUSEADDR |
| TLS | 인증서 검증, 키 교환, 1.3은 1-RTT |
| 로드밸런서 | L4(IP/Port), L7(HTTP), 알고리즘 |

---

*마지막 업데이트: 2025년 01월*
