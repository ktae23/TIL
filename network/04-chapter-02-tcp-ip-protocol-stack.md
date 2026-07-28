# Chapter 02. TCP/IP의 데이터를 전기신호로 만들어 보낸다

## 개요

프로토콜 스택과 LAN 어댑터의 내부 동작을 살펴봅니다. 소켓 생성부터 TCP 연결 수립, 데이터 송수신, 연결 종료까지의 전 과정과 IP/이더넷 계층의 패킷 처리, 그리고 UDP 프로토콜의 동작 원리를 실무 사례와 함께 다룹니다.

## 목차

1. [소켓을 작성한다](#1-소켓을-작성한다)
2. [서버에 접속한다](#2-서버에-접속한다)
3. [데이터를 송·수신한다](#3-데이터를-송수신한다)
4. [서버에서 연결을 끊어 소켓을 말소한다](#4-서버에서-연결을-끊어-소켓을-말소한다)
5. [IP와 이더넷의 패킷 송·수신 동작](#5-ip와-이더넷의-패킷-송수신-동작)
6. [UDP 프로토콜을 이용한 송·수신 동작](#6-udp-프로토콜을-이용한-송수신-동작)

---

## 1. 소켓을 작성한다

### 프로토콜 스택의 내부 구성

프로토콜 스택은 운영체제의 네트워크 제어 소프트웨어로, 계층적 구조를 가집니다.

**계층 구조:**
```
+--------------------------------+
|      애플리케이션 계층         |  HTTP, FTP, SMTP, DNS
+--------------------------------+
|   전송 계층 (Transport)        |
|   - TCP: 신뢰성 있는 전송      |
|   - UDP: 빠른 전송             |
+--------------------------------+
|   네트워크 계층 (Network)      |
|   - IP: 라우팅, 주소 지정      |
|   - ICMP: 오류 보고            |
+--------------------------------+
|   데이터 링크 계층             |
|   - Ethernet, Wi-Fi            |
+--------------------------------+
|   물리 계층                     |
|   - 전기/광 신호               |
+--------------------------------+
```

**실무 도구 - 프로토콜 스택 상태 확인:**

```bash
# Linux - TCP 연결 상태 확인
$ ss -tan
State    Recv-Q Send-Q Local Address:Port  Peer Address:Port
ESTAB    0      0      192.168.1.100:443   93.184.216.34:80
LISTEN   0      128    0.0.0.0:22          0.0.0.0:*

# macOS/BSD
$ netstat -an -p tcp

# Windows
> netstat -an -p tcp

# 상세 통계
$ ss -s
Total: 200
TCP:   50 (estab 10, closed 30, orphaned 0, timewait 20)
```

### 소켓의 실체는 통신 제어용 제어 정보

**소켓이 관리하는 정보:**

| 정보 | 설명 |
|------|------|
| 파일 디스크립터 | 소켓을 식별하는 정수 |
| 소켓 타입 | TCP (SOCK_STREAM) 또는 UDP (SOCK_DGRAM) |
| 연결 상태 | LISTEN, ESTABLISHED, TIME_WAIT 등 |
| 로컬 주소:포트 | 자신의 IP 주소와 포트 번호 |
| 원격 주소:포트 | 상대방의 IP 주소와 포트 번호 |
| TCP 제어 정보 | 시퀀스 번호, 윈도우 크기, 타이머 등 |
| 송수신 버퍼 | 데이터를 임시 저장하는 메모리 |

**TCP 제어 블록 (TCB: Transmission Control Block):**
- 연결 상태 (LISTEN, SYN_SENT, ESTABLISHED 등)
- 시퀀스 번호, ACK 번호
- 윈도우 크기
- 재전송 타이머
- 혼잡 제어 정보

**실무 사례 - /proc을 통한 소켓 정보 확인 (Linux):**
```bash
# 프로세스의 열린 소켓 확인
$ lsof -p <PID> -a -i
COMMAND   PID USER   FD   TYPE DEVICE SIZE/OFF NODE NAME
chrome   1234 buzz   50u  IPv4 123456      0t0  TCP 192.168.1.100:53214->142.250.189.206:443 (ESTABLISHED)

# 소켓 상세 정보
$ ss -tep
```

### Socket을 호출했을 때의 동작

**socket() 시스템 콜 파라미터:**

| 파라미터 | 옵션 | 설명 |
|---------|------|------|
| domain (주소 체계) | AF_INET | IPv4 |
| | AF_INET6 | IPv6 |
| | AF_UNIX | 유닉스 도메인 소켓 (프로세스 간 통신) |
| type (소켓 타입) | SOCK_STREAM | TCP (연결 지향, 신뢰성 보장) |
| | SOCK_DGRAM | UDP (비연결, 빠름) |
| | SOCK_RAW | 원시 소켓 (직접 패킷 제어) |
| protocol | 0 | 자동 선택 (일반적으로 사용) |

**소켓 옵션:**

| 옵션 | 레벨 | 설명 | 사용 시기 |
|------|------|------|-----------|
| SO_REUSEADDR | SOL_SOCKET | TIME_WAIT 포트 재사용 | 서버 재시작 시 |
| SO_KEEPALIVE | SOL_SOCKET | 연결 유지 확인 | 장시간 유휴 연결 |
| TCP_NODELAY | IPPROTO_TCP | Nagle 알고리즘 비활성화 | 실시간 통신 |
| SO_RCVBUF | SOL_SOCKET | 수신 버퍼 크기 | 대용량 전송 |
| SO_SNDBUF | SOL_SOCKET | 송신 버퍼 크기 | 대용량 전송 |

**AWS 서비스 활용:**

| 소켓 옵션 개념 | AWS 서비스 | 설명 |
|--------------|-----------|------|
| Keep-Alive | **ALB Idle Timeout** | 기본 60초, 최대 4000초 설정 가능 |
| 버퍼 크기 | **NLB** | 대용량 TCP 트래픽 처리 최적화 |
| 연결 재사용 | **ALB Connection Reuse** | 백엔드 연결 풀링 |

**실무적 활용 사례:**

> ⚠️ **소켓 고갈 공격**: 공격자가 많은 연결을 열고 닫지 않으면 서버의 파일 디스크립터가 고갈되어 새 연결을 수락할 수 없습니다.

> ⚠️ **Slowloris 공격**: 공격자가 HTTP 요청을 매우 느리게 보내 연결을 장시간 유지하여 서버 리소스를 고갈시킵니다.

---

## 2. 서버에 접속한다

### 접속의 의미

TCP 연결은 **논리적인 통신 경로**를 설정하는 것입니다. 물리적 회선을 독점하는 것이 아니라, 양쪽 소켓에 제어 정보를 기록하여 통신 상태를 관리합니다.

### 맨 앞부분에 제어 정보를 기록한 헤더를 배치한다

**TCP 헤더 구조 (20바이트):**

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|          Source Port          |       Destination Port        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                        Sequence Number                        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                    Acknowledgment Number                      |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|  Data |       |C|E|U|A|P|R|S|F|                               |
| Offset| Rsrvd |W|C|R|C|S|S|Y|I|            Window             |
|       |       |R|E|G|K|H|T|N|N|                               |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|           Checksum            |         Urgent Pointer        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

**주요 필드:**

| 필드 | 크기 | 설명 |
|------|------|------|
| Source Port | 16bit | 송신 포트 번호 |
| Destination Port | 16bit | 수신 포트 번호 |
| Sequence Number | 32bit | 데이터의 순서 번호 |
| Acknowledgment Number | 32bit | 수신 확인 번호 |
| Data Offset | 4bit | TCP 헤더 길이 |
| Control Flags | 8bit | SYN, ACK, FIN, RST, PSH, URG |
| Window Size | 16bit | 수신 윈도우 크기 |
| Checksum | 16bit | 오류 검사 |

**Control Flags (제어 비트):**

| 비트 | 이름 | 의미 |
|------|------|------|
| SYN | Synchronize | 연결 수립 요청 |
| ACK | Acknowledgment | 확인 응답 |
| FIN | Finish | 연결 종료 요청 |
| RST | Reset | 연결 강제 종료 |
| PSH | Push | 즉시 전달 |
| URG | Urgent | 긴급 데이터 |

### 접속 동작의 실제

**TCP 3-Way Handshake (상세):**

```
클라이언트 (192.168.1.100:53214)        서버 (93.184.216.34:80)
    | [CLOSED]                          | [LISTEN]
    |                                   |
    | (1) SYN                           |
    |     Seq=1000, ACK=0               |
    |     SYN=1, ACK=0                  |
    | --------------------------------> |
    | [SYN_SENT]                        | [SYN_RECEIVED]
    |                                   |
    | (2) SYN+ACK                       |
    |     Seq=2000, ACK=1001            |
    |     SYN=1, ACK=1                  |
    | <-------------------------------- |
    |                                   |
    | (3) ACK                           |
    |     Seq=1001, ACK=2001            |
    |     SYN=0, ACK=1                  |
    | --------------------------------> |
    | [ESTABLISHED]                     | [ESTABLISHED]
```

**패킷 분석 예시 (Wireshark):**
```
# 1. SYN 패킷
192.168.1.100:53214 → 93.184.216.34:80
Flags: [SYN]
Seq=1000
Win=65535
Options: MSS=1460, SACK_PERM, Timestamps, NOP, WScale=7

# 2. SYN+ACK 패킷
93.184.216.34:80 → 192.168.1.100:53214
Flags: [SYN, ACK]
Seq=2000
Ack=1001
Win=29200

# 3. ACK 패킷
192.168.1.100:53214 → 93.184.216.34:80
Flags: [ACK]
Seq=1001
Ack=2001
```

**연결 오류 유형:**

| 오류 | 원인 | 네트워크 상황 |
|------|------|-------------|
| ECONNREFUSED | 서버 포트가 닫혀 있음 | RST 패킷 수신 |
| ETIMEDOUT | 응답 없음 | SYN 패킷에 응답 없음 |
| EHOSTUNREACH | 호스트 도달 불가 | 라우팅 실패 |
| ENETUNREACH | 네트워크 도달 불가 | 네트워크 경로 없음 |

**AWS 서비스 활용:**

| 연결 문제 | AWS 서비스 | 해결 방법 |
|----------|-----------|----------|
| 타임아웃 | **Security Group** | 인바운드 규칙에 포트 허용 |
| 연결 거부 | **NACL** | 서브넷 레벨 트래픽 허용 |
| 호스트 도달 불가 | **Route Table** | 올바른 라우팅 설정 확인 |

**실무적 활용 사례:**

> ⚠️ **TCP 시퀀스 번호 예측 공격**: 이 단계에서 공격자가 초기 시퀀스 번호(ISN)를 예측할 수 있으면, 연결을 가로채거나 위조된 패킷을 주입할 수 있습니다. 현대 OS는 랜덤 ISN을 사용하여 방어합니다.

**TCP 연결 상태 머신:**

```
CLOSED → LISTEN (서버)
CLOSED → SYN_SENT → ESTABLISHED (클라이언트)
LISTEN → SYN_RECEIVED → ESTABLISHED (서버)

ESTABLISHED → FIN_WAIT_1 → FIN_WAIT_2 → TIME_WAIT → CLOSED
ESTABLISHED → CLOSE_WAIT → LAST_ACK → CLOSED
```

---

## 3. 데이터를 송·수신한다

### 프로토콜 스택에 HTTP 리퀘스트 메시지를 넘긴다

**send() 시스템 콜 동작:**

```
send(sockfd, 데이터, 길이, 플래그) 호출:

1. 데이터를 커널 송신 버퍼에 복사
2. TCP가 세그먼트 크기(MSS)로 분할
3. 각 세그먼트에 TCP 헤더 추가
4. IP 계층으로 전달

플래그 옵션:
  - 0: 일반 전송
  - MSG_DONTWAIT: 논블로킹
  - MSG_NOSIGNAL: SIGPIPE 억제
  - MSG_MORE: Nagle 힌트
```

**실무적 활용 사례:**

> ⚠️ **패킷 주입 공격**: 이 단계에서 공격자가 동일 네트워크에 있다면, 정상 패킷 사이에 악성 패킷을 주입할 수 있습니다. 올바른 시퀀스 번호를 추측해야 하므로 어렵지만, 성공 시 세션을 탈취할 수 있습니다.

### 데이터가 클 때는 분할하여 보낸다

**MSS (Maximum Segment Size):**

- TCP에서 한 번에 전송할 수 있는 최대 데이터 크기
- 일반적으로 **1460바이트** (Ethernet MTU 1500 - IP 헤더 20 - TCP 헤더 20)
- SYN 패킷에서 MSS 값 협상

**MTU (Maximum Transmission Unit):**
- 한 번에 전송 가능한 최대 패킷 크기
- Ethernet: 1500바이트
- 점보 프레임: 9000바이트

**패킷 분할 예시:**
```
애플리케이션 데이터: 5000바이트

MSS = 1460바이트인 경우:
  패킷 1: 1460바이트 (Seq=1001)
  패킷 2: 1460바이트 (Seq=2461)
  패킷 3: 1460바이트 (Seq=3921)
  패킷 4: 620바이트  (Seq=5381)
```

**실무 사례 - MTU 확인 및 설정:**

```bash
# Linux - MTU 확인
$ ip link show eth0
2: eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 ...

# MTU 변경
$ sudo ip link set dev eth0 mtu 9000

# macOS
$ ifconfig en0 | grep mtu
  mtu 1500

# Path MTU Discovery 테스트
$ ping -M do -s 1472 www.example.com  # 1472 + 28(IP+ICMP) = 1500
```

**AWS 서비스 활용:**

| MTU 관련 | AWS 서비스 | 설명 |
|---------|-----------|------|
| 점보 프레임 | **VPC (같은 리전)** | EC2 인스턴스 간 9001 바이트 MTU 지원 |
| Path MTU | **VPC Peering** | 리전 간 1500 바이트 MTU 제한 |
| MTU 오버헤드 | **VPN** | IPsec 오버헤드로 유효 MTU 감소 |

**실무적 활용 사례:**

> ⚠️ **MTU 블랙홀**: ICMP "Fragmentation Needed" 패킷이 차단되면, 큰 패킷이 도달하지 못하고 연결이 중단됩니다. 일부 웹사이트만 접속되지 않는 현상이 발생합니다.

### ACK 번호를 사용하여 패킷이 도착했는지 확인한다

**ACK (Acknowledgment) 메커니즘:**

```
클라이언트                                서버
    | Seq=1001, Len=1460                  |
    | ----------------------------------→ |
    |                                     |
    | ←---------------------------------- |
    | ACK=2461 (다음 받을 Seq)            |
    |                                     |
    | Seq=2461, Len=1460                  |
    | ----------------------------------→ |
    |                                     |
    | ←---------------------------------- |
    | ACK=3921                            |
```

**누적 ACK (Cumulative ACK):**
- ACK=2461은 "2461 이전의 모든 데이터를 받았음"을 의미
- 중간 패킷 손실 시 재전송 필요

**선택적 ACK (SACK: Selective Acknowledgment):**
```
# TCP 옵션으로 SACK 활성화
Options: SACK_PERM

# 패킷 1, 3은 도착, 패킷 2 손실
ACK=2461, SACK=[3921:5381]
  → "2461까지 받았고, 3921~5381도 받았음"
  → 패킷 2만 재전송하면 됨
```

**재전송 타이머 (RTO: Retransmission Timeout):**

```bash
# 실무 도구 - 재전송 상태 확인 (Linux)
$ ss -ti
tcp   ESTAB   0        0        192.168.1.100:53214   93.184.216.34:80
         cubic wscale:7,7 rto:204 rtt:3.5/1.2 ato:40 mss:1460
         #       ^^^^^^ RTO (밀리초)
         #              ^^^ RTT (왕복 시간)
```

**실무적 활용 사례:**

> ⚠️ **ACK 스푸핑**: 공격자가 위조된 ACK를 보내면 송신자는 데이터가 전달되었다고 잘못 인식합니다. 이로 인해 데이터 손실이 발생할 수 있습니다.

### 패킷 평균 왕복 시간으로 ACK 번호의 대기 시간을 조정한다

**RTT (Round Trip Time) 측정:**

```
RTT = 패킷 송신 시간 → 패킷 수신 시간 + ACK 송신 시간 → ACK 수신 시간
```

**RTO 계산 (Karn's Algorithm):**
```
SRTT (Smoothed RTT) = (1 - α) × SRTT + α × RTT_sample
RTTVAR = (1 - β) × RTTVAR + β × |SRTT - RTT_sample|
RTO = SRTT + 4 × RTTVAR

일반적으로 α=1/8, β=1/4
```

**실무 사례 - ping으로 RTT 측정:**
```bash
$ ping -c 10 www.example.com
PING www.example.com (93.184.216.34): 56 data bytes
64 bytes from 93.184.216.34: icmp_seq=0 ttl=56 time=15.2 ms
64 bytes from 93.184.216.34: icmp_seq=1 ttl=56 time=14.8 ms
...
--- www.example.com ping statistics ---
10 packets transmitted, 10 packets received, 0.0% packet loss
round-trip min/avg/max/stddev = 14.5/15.1/16.2/0.5 ms
```

### 윈도우 제어 방식으로 ACK 번호를 관리한다

**슬라이딩 윈도우 (Sliding Window):**

수신자가 한 번에 받을 수 있는 데이터 양을 제어합니다.

```
윈도우 크기 = 65535바이트 (2^16 - 1, Window Scaling으로 확장 가능)

송신 버퍼:
[전송됨+ACK받음] [전송됨+ACK대기] [전송가능] [전송불가]
                  ←------ 윈도우 ------→

예시:
Seq: 1000   2000   3000   4000   5000   6000   7000
     [ACK]  [전송완료] [전송완료] [전송가능] [대기]
            ←-------- Window=4000 --------→
```

**윈도우 확장 (Window Scaling):**

```
# TCP 옵션
Options: WScale=7

실제 윈도우 크기 = 헤더의 Window × 2^7
                = 65535 × 128
                = 8,388,480바이트 (약 8MB)
```

**AWS 서비스 활용:**

| TCP 윈도우 | AWS 서비스 | 설명 |
|-----------|-----------|------|
| 윈도우 스케일링 | **NLB** | 대용량 TCP 연결 최적화 |
| 버퍼 튜닝 | **EC2 Enhanced Networking** | 고성능 네트워크 스택 |
| 혼잡 제어 | **Global Accelerator** | AWS 백본 네트워크로 혼잡 회피 |

**실무적 활용 사례:**

> ⚠️ **윈도우 크기 0 공격**: 공격자가 윈도우 크기를 0으로 설정하면 송신자는 데이터 전송을 중단하고 대기합니다. 이를 반복하면 연결을 장시간 유지하여 리소스를 고갈시킵니다.

### ACK 번호와 윈도우를 합승한다

**지연 ACK (Delayed ACK):**

매 패킷마다 ACK를 보내지 않고, 여러 패킷을 받은 후 하나의 ACK로 응답하여 네트워크 효율을 높입니다.

```
# 일반 ACK
패킷 1 도착 → ACK 1 전송
패킷 2 도착 → ACK 2 전송
패킷 3 도착 → ACK 3 전송

# 지연 ACK
패킷 1 도착 → 대기 (40~500ms)
패킷 2 도착 → ACK 2 전송 (패킷 1, 2 모두 확인)
```

**Nagle 알고리즘:**

작은 패킷 여러 개를 하나로 모아서 전송하여 네트워크 효율을 높입니다.

```
# Nagle 알고리즘 OFF (TCP_NODELAY)
send(1바이트) → 즉시 전송
send(1바이트) → 즉시 전송
send(1바이트) → 즉시 전송

# Nagle 알고리즘 ON
send(1바이트) → 버퍼에 저장
send(1바이트) → 버퍼에 저장
send(1바이트) → 버퍼에 저장
ACK 도착 또는 MSS 도달 → 한 번에 전송
```

**Nagle 알고리즘 적용 시나리오:**

| 애플리케이션 | Nagle | 이유 |
|------------|-------|------|
| 게임, VoIP | OFF (TCP_NODELAY) | 실시간 응답 필요 |
| 파일 전송 | ON (기본값) | 효율적인 대역폭 사용 |
| SSH | OFF | 키 입력 즉시 전송 |
| HTTP/2 | ON | 멀티플렉싱으로 효율화 |

### HTTP 응답 메시지를 수신한다

**recv() 동작 과정:**

```
1. 커널 수신 버퍼에 데이터 도착 확인
2. 데이터가 없으면:
   - 블로킹 모드: 데이터 도착까지 대기
   - 논블로킹 모드: 즉시 반환 (EAGAIN)
3. 데이터가 있으면:
   - 요청한 크기만큼 사용자 버퍼로 복사
   - 실제 복사된 바이트 수 반환
4. 연결 종료 시: 0 반환 (EOF)
```

**AWS 서비스 활용:**

| I/O 모델 | AWS 서비스 | 설명 |
|---------|-----------|------|
| 비동기 처리 | **Lambda** | 이벤트 기반 비동기 실행 |
| 연결 풀링 | **RDS Proxy** | 데이터베이스 연결 효율화 |
| 웹소켓 | **API Gateway WebSocket** | 양방향 실시간 통신 |

---

## 4. 서버에서 연결을 끊어 소켓을 말소한다

### 데이터 보내기를 완료했을 때 연결을 끊는다

**TCP 4-Way Handshake:**

```
클라이언트                                서버
    | [ESTABLISHED]                      | [ESTABLISHED]
    |                                     |
    | (1) FIN                             |
    |     Seq=5000, ACK=3000              |
    | ----------------------------------→ |
    | [FIN_WAIT_1]                        | [CLOSE_WAIT]
    |                                     |
    | (2) ACK                             |
    |     Seq=3000, ACK=5001              |
    | ←---------------------------------- |
    | [FIN_WAIT_2]                        |
    |                                     |
    | (3) FIN                             |
    |     Seq=3000, ACK=5001              |
    | ←---------------------------------- |
    | [TIME_WAIT]                         | [LAST_ACK]
    |                                     |
    | (4) ACK                             |
    |     Seq=5001, ACK=3001              |
    | ----------------------------------→ |
    | [TIME_WAIT → CLOSED]                | [CLOSED]
      (2MSL 대기)
```

**close() vs shutdown():**

| 함수 | 동작 | 사용 시점 |
|------|------|----------|
| shutdown(SHUT_WR) | 송신만 종료, FIN 전송 | 송신 완료 후 수신 대기 |
| shutdown(SHUT_RD) | 수신만 종료 | 더 이상 수신 안 함 |
| shutdown(SHUT_RDWR) | 양방향 종료 | 즉시 종료 |
| close() | 소켓 리소스 해제 | 완전 종료 |

**우아한 종료 (Graceful Shutdown) 과정:**

```
1. shutdown(SHUT_WR) 호출 → FIN 전송
2. 상대방의 남은 데이터 수신 대기
3. 상대방 FIN 수신
4. close() 호출 → 리소스 해제
```

**AWS 서비스 활용:**

| 연결 종료 | AWS 서비스 | 설명 |
|----------|-----------|------|
| 드레이닝 | **Target Group Deregistration Delay** | 기존 연결 완료까지 대기 |
| 타임아웃 | **ALB Idle Timeout** | 유휴 연결 자동 종료 |
| 헬스체크 | **Target Group Health Check** | 비정상 인스턴스 연결 차단 |

### 소켓을 말소한다

**TIME_WAIT 상태:**

연결 종료 후 **2MSL (Maximum Segment Lifetime)** 동안 대기합니다.

- MSL: 패킷이 네트워크에서 살아있을 수 있는 최대 시간 (보통 30초~2분)
- 2MSL: 약 1~4분
- 이유:
  1. 지연된 패킷이 새 연결에 영향을 주지 않도록
  2. 마지막 ACK가 손실되었을 때 재전송 가능

**실무 도구 - TIME_WAIT 상태 확인:**

```bash
# TIME_WAIT 소켓 개수 확인
$ ss -tan | grep TIME_WAIT | wc -l
342

# 포트별 TIME_WAIT 확인
$ ss -tan state time-wait '( sport = :80 )'

# TIME_WAIT 상태의 소켓 재사용 허용 (Linux)
$ sudo sysctl -w net.ipv4.tcp_tw_reuse=1
```

**SO_LINGER 옵션:**

| 설정 | 동작 | 사용 시점 |
|------|------|----------|
| LINGER OFF (기본) | close() 즉시 반환, 백그라운드 종료 | 일반적인 사용 |
| LINGER ON, timeout=0 | 즉시 RST 전송, 강제 종료 | 비정상 연결 정리 |
| LINGER ON, timeout>0 | timeout 동안 데이터 전송 시도 | 중요 데이터 보장 |

**실무적 활용 사례:**

> ⚠️ **RST 주입 공격**: 공격자가 위조된 RST 패킷을 보내면 정상적인 TCP 연결이 강제로 끊어집니다. 이를 통해 서비스 거부 공격이 가능합니다.

### 데이터 송·수신 동작을 정리한다

**전체 흐름:**

```
1. socket()      - 소켓 생성
2. bind()        - 주소 할당 (서버만)
3. listen()      - 대기 상태 (서버만)
4. accept()      - 연결 수락 (서버만)
5. connect()     - 연결 요청 (클라이언트)
   → 3-Way Handshake
6. send()/recv() - 데이터 송수신
7. shutdown()    - 단방향 종료 (선택)
8. close()       - 소켓 닫기
   → 4-Way Handshake
```

---

## 5. IP와 이더넷의 패킷 송·수신 동작

### 패킷의 기본

**캡슐화 (Encapsulation):**

```
+---------------------------+
|    애플리케이션 데이터    |  HTTP, FTP 등
+---------------------------+
       ↓ (TCP/UDP 추가)
+----------+----------------+
| TCP 헤더 |      데이터    |
+----------+----------------+
       ↓ (IP 추가)
+----------+----------+-----+
| IP 헤더  | TCP 헤더 | 데이터 |
+----------+----------+-----+
       ↓ (이더넷 추가)
+------------+----------+----------+-----+------------+
| 이더넷 헤더 | IP 헤더  | TCP 헤더 | 데이터 | 이더넷 트레일러 |
+------------+----------+----------+-----+------------+
     (14B)      (20B)      (20B)           (4B CRC)
```

### 패킷 송·수신 동작의 개요

**계층별 처리:**

1. **전송 계층 (TCP)**: 세그먼트 생성, 포트 번호 추가
2. **네트워크 계층 (IP)**: IP 헤더 추가, 라우팅
3. **데이터 링크 계층 (Ethernet)**: 이더넷 헤더/트레일러 추가
4. **물리 계층**: 전기/광 신호로 변환

### 수신처 IP 주소를 기록한 IP 헤더를 만든다

**IPv4 헤더 (20바이트):**

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|Version|  IHL  |Type of Service|          Total Length         |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|         Identification        |Flags|      Fragment Offset    |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|  Time to Live |    Protocol   |         Header Checksum       |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                       Source Address                          |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                    Destination Address                        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

**주요 필드:**

| 필드 | 크기 | 설명 |
|------|------|------|
| Version | 4bit | IP 버전 (4 또는 6) |
| IHL | 4bit | 헤더 길이 (보통 5 = 20바이트) |
| TOS/DSCP | 8bit | 서비스 품질 |
| Total Length | 16bit | 전체 패킷 길이 (헤더 + 데이터) |
| TTL | 8bit | Time To Live (홉 수) |
| Protocol | 8bit | 상위 프로토콜 (6=TCP, 17=UDP, 1=ICMP) |
| Source Address | 32bit | 송신지 IP 주소 |
| Destination Address | 32bit | 수신지 IP 주소 |

**실무 사례 - TTL 추적:**

```bash
# traceroute: 경로상의 라우터 확인
$ traceroute www.google.com
 1  192.168.1.1 (192.168.1.1)  1.234 ms  0.987 ms  0.876 ms
 2  10.0.0.1 (10.0.0.1)  5.432 ms  5.321 ms  5.234 ms
 3  203.0.113.1 (203.0.113.1)  10.234 ms  10.123 ms  10.098 ms
...

# Windows
> tracert www.google.com

# mtr (My TraceRoute): 실시간 모니터링
$ mtr www.google.com
```

### 이더넷용 MAC 헤더를 만든다

**이더넷 프레임 구조:**

```
+-------------+-------------+------+---------+-----+
| Destination | Source      | Type | Payload | FCS |
| MAC (6B)    | MAC (6B)    | (2B) | (46-    | (4B)|
|             |             |      | 1500B)  |     |
+-------------+-------------+------+---------+-----+
```

**MAC 주소 (48비트):**
```
00:1A:2B:3C:4D:5E
│  │  │  │  │  │
OUI (24비트) + 고유번호 (24비트)
제조사 식별    장치 고유번호
```

**EtherType 값:**
- `0x0800`: IPv4
- `0x0806`: ARP
- `0x86DD`: IPv6

**실무 도구 - MAC 주소 확인:**

```bash
# Linux
$ ip link show
2: eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc fq_codel state UP mode DEFAULT
    link/ether 00:1a:2b:3c:4d:5e brd ff:ff:ff:ff:ff:ff

# macOS
$ ifconfig en0 | grep ether
  ether 00:1a:2b:3c:4d:5e

# Windows
> ipconfig /all
  물리적 주소 . . . . . . . . : 00-1A-2B-3C-4D-5E
```

### ARP로 수신처 라우터의 MAC 주소를 조사한다

**ARP (Address Resolution Protocol):**

IP 주소로부터 MAC 주소를 찾는 프로토콜입니다.

**ARP 동작:**
```
1. ARP Request (브로드캐스트)
   "192.168.1.1의 MAC 주소는 누구인가요?"
   Destination MAC: FF:FF:FF:FF:FF:FF (브로드캐스트)

2. ARP Reply (유니캐스트)
   "192.168.1.1의 MAC은 00:1A:2B:3C:4D:5E입니다"
   Source MAC: 00:1A:2B:3C:4D:5E
```

**ARP 캐시 테이블:**

```bash
# ARP 캐시 확인
$ arp -a
? (192.168.1.1) at 00:1a:2b:3c:4d:5e on en0 ifscope [ethernet]
? (192.168.1.100) at a0:b1:c2:d3:e4:f5 on en0 ifscope [ethernet]

# Windows
> arp -a

# ARP 캐시 삭제
$ sudo arp -d 192.168.1.1

# ARP 요청 전송 (arping)
$ sudo arping -I eth0 192.168.1.1
```

**실무 사례 - Gratuitous ARP:**

```bash
# 자신의 IP에 대한 ARP 요청 전송 (IP 충돌 감지, ARP 캐시 갱신)
$ sudo arping -U -I eth0 192.168.1.100
```

### 이더넷의 기본

**CSMA/CD (Carrier Sense Multiple Access with Collision Detection):**

- **Carrier Sense**: 전송 전 회선 사용 여부 확인
- **Multiple Access**: 여러 장치가 공유
- **Collision Detection**: 충돌 감지 시 재전송

현대 이더넷(스위치 사용)에서는 전이중 모드로 충돌이 발생하지 않습니다.

### IP 패킷을 전기나 빛의 신호로 변환하여 송신한다

**신호 변환:**

1. **전기 신호**: UTP 케이블 (10/100/1000Base-T)
2. **광 신호**: 광섬유 (1000Base-LX, 10GBase-SR)

**Manchester Encoding, 4B/5B 등의 인코딩 방식 사용**

### 패킷에 3개의 제어용 데이터를 추가한다

**이더넷 프레임 (완전한 형태):**

```
+----------+-------------+-------------+------+---------+-----+
| Preamble | Destination | Source      | Type | Payload | FCS |
| (7B) +   | MAC (6B)    | MAC (6B)    | (2B) | (46-    | (4B)|
| SFD (1B) |             |             |      | 1500B)  |     |
+----------+-------------+-------------+------+---------+-----+
```

- **Preamble**: 동기화 신호 (10101010 × 7)
- **SFD (Start Frame Delimiter)**: 프레임 시작 (10101011)
- **FCS (Frame Check Sequence)**: CRC-32 오류 검사

### 허브를 향해 패킷을 송신한다

**허브 vs 스위치:**

| 특징 | 허브 (Hub) | 스위치 (Switch) |
|------|-----------|----------------|
| 동작 | 모든 포트로 전송 (브로드캐스트) | MAC 주소 기반 목적지만 전송 |
| 충돌 | CSMA/CD 필요 | 전이중 모드, 충돌 없음 |
| 성능 | 낮음 | 높음 |
| 현황 | 거의 사용 안 함 | 주류 |

---

## 6. UDP 프로토콜을 이용한 송·수신 동작

### 수정 송신이 필요없는 데이터의 송신은 UDP가 효율적이다

**TCP vs UDP:**

| 특징 | TCP | UDP |
|------|-----|-----|
| 연결 | 연결 지향 (3-Way Handshake) | 비연결 |
| 신뢰성 | 보장 (재전송, 순서 보장) | 보장 안 함 |
| 속도 | 느림 | 빠름 |
| 헤더 크기 | 20바이트 (최소) | 8바이트 |
| 용도 | 웹, 이메일, 파일 전송 | 스트리밍, DNS, VoIP |

**UDP 헤더 (8바이트):**

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|          Source Port          |       Destination Port        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|            Length             |           Checksum            |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

### 제어용 짧은 데이터

**UDP 사용 사례:**

| 프로토콜 | 포트 | 용도 | 특징 |
|---------|------|------|------|
| DNS | 53 | 도메인 조회 | 빠른 응답 필요 |
| DHCP | 67/68 | IP 할당 | 브로드캐스트 사용 |
| NTP | 123 | 시간 동기화 | 짧은 패킷 |
| SNMP | 161/162 | 네트워크 관리 | 간단한 쿼리/응답 |

**AWS 서비스 활용:**

| UDP 용도 | AWS 서비스 | 설명 |
|---------|-----------|------|
| DNS | **Route 53 Resolver** | VPC 내 DNS 해석 |
| NTP | **Amazon Time Sync** | EC2 인스턴스 시간 동기화 |
| 게임/스트리밍 | **GameLift** | 실시간 게임 서버 |

**실무적 활용 사례:**

> ⚠️ **UDP Flood 공격**: 대량의 UDP 패킷을 전송하여 서버의 대역폭과 리소스를 고갈시킵니다. UDP는 핸드셰이크가 없어 출발지 IP 위조가 쉽습니다.

> ⚠️ **DNS Amplification 공격**: 작은 DNS 쿼리가 큰 응답을 생성하는 점을 악용합니다. 공격자는 피해자 IP를 출발지로 위조하여 DNS 서버에 쿼리를 보내고, 증폭된 응답이 피해자에게 전달됩니다.

### 음성 및 동영상 데이터

**실시간 스트리밍 프로토콜:**

| 프로토콜 | 전송 계층 | 용도 | 특징 |
|---------|----------|------|------|
| RTP | UDP | 실시간 오디오/비디오 | 타임스탬프, 시퀀스 번호 |
| RTCP | UDP | RTP 제어 | 품질 피드백 |
| WebRTC | UDP (ICE) | P2P 통신 | NAT 통과, DTLS 암호화 |
| HLS/DASH | TCP | 적응형 스트리밍 | HTTP 기반 |

**AWS 서비스 활용:**

| 스트리밍 용도 | AWS 서비스 | 설명 |
|-------------|-----------|------|
| 라이브 스트리밍 | **MediaLive** | 실시간 비디오 인코딩 |
| VOD | **MediaConvert** | 비디오 트랜스코딩 |
| WebRTC | **Kinesis Video Streams** | 양방향 비디오 스트리밍 |
| CDN 배포 | **CloudFront** | 전 세계 콘텐츠 전송 |

**QUIC (HTTP/3) - UDP 기반 신뢰성 있는 전송:**

| 특성 | TCP/TLS (HTTP/2) | QUIC (HTTP/3) |
|------|-----------------|---------------|
| 핸드셰이크 | 3-Way + TLS = 2 RTT | 0~1 RTT |
| 헤드오브라인 블로킹 | 있음 | 없음 (스트림 독립) |
| 연결 마이그레이션 | 불가 | 가능 (Connection ID) |

**실무적 활용 사례:**

> ⚠️ **WebRTC SRTP 키 탈취**: DTLS 협상 과정에서 키 교환이 노출되면 미디어 스트림이 복호화될 수 있습니다. SRTP 암호화가 중요합니다.

---

## 실무 팁

### 1. 네트워크 디버깅 도구

```bash
# tcpdump: 패킷 캡처
$ sudo tcpdump -i eth0 -n port 80
$ sudo tcpdump -i eth0 -w capture.pcap

# Wireshark: GUI 패킷 분석
$ wireshark

# ngrep: 네트워크 grep
$ sudo ngrep -q -W byline port 80

# ss: 소켓 통계
$ ss -s     # 요약
$ ss -tan   # TCP 연결
$ ss -uan   # UDP 연결
```

### 2. 성능 튜닝

**Linux sysctl 설정:**
```bash
# TCP 버퍼 크기 증가
net.core.rmem_max = 16777216
net.core.wmem_max = 16777216
net.ipv4.tcp_rmem = 4096 87380 16777216
net.ipv4.tcp_wmem = 4096 65536 16777216

# TIME_WAIT 재사용
net.ipv4.tcp_tw_reuse = 1

# SYN 백로그 큐 증가
net.ipv4.tcp_max_syn_backlog = 4096
net.core.somaxconn = 4096
```

### 3. 보안 고려사항

**SYN Flood 공격 방어:**
```bash
# SYN Cookies 활성화
$ sudo sysctl -w net.ipv4.tcp_syncookies=1

# SYN+ACK 재시도 횟수 감소
$ sudo sysctl -w net.ipv4.tcp_synack_retries=2
```

**방화벽 (iptables):**
```bash
# 특정 포트만 허용
$ sudo iptables -A INPUT -p tcp --dport 80 -j ACCEPT
$ sudo iptables -A INPUT -p tcp --dport 443 -j ACCEPT

# Rate Limiting
$ sudo iptables -A INPUT -p tcp --dport 22 -m limit --limit 3/min -j ACCEPT
```

### 4. AWS 서비스 활용

**연결 풀과 타임아웃 관리:**

| 요구사항 | AWS 서비스 | 설정 |
|---------|-----------|------|
| HTTP 연결 풀 | **ALB** | Connection reuse 자동 제공 |
| 데이터베이스 연결 풀 | **RDS Proxy** | 연결 풀링, 자동 장애 조치 |
| 연결 타임아웃 | **ALB Idle Timeout** | 1~4000초 설정 가능 |
| 요청 타임아웃 | **API Gateway** | 최대 29초 통합 타임아웃 |

**실무적 활용 사례:**

> ⚠️ **연결 풀 고갈**: 연결을 제대로 반환하지 않으면 풀이 고갈되어 새 요청을 처리할 수 없습니다. RDS Proxy는 유휴 연결을 자동으로 정리하여 이 문제를 완화합니다.

> ⚠️ **타임아웃 불일치**: 클라이언트와 서버의 타임아웃 설정이 다르면 예기치 않은 연결 종료가 발생합니다. ALB Idle Timeout보다 백엔드 Keep-Alive를 길게 설정해야 합니다.

---

## AWS 서비스 전체 요약

| 네트워크 개념 | AWS 서비스 | 설명 |
|-------------|-----------|------|
| TCP 연결 관리 | **ALB (Application Load Balancer)** | 연결 재사용, Idle Timeout 설정 |
| 대용량 TCP 트래픽 | **NLB (Network Load Balancer)** | 초당 수백만 연결 처리 |
| 연결 풀링 | **RDS Proxy** | 데이터베이스 연결 효율화 |
| 보안 그룹 | **Security Group** | 인스턴스 레벨 방화벽 |
| 네트워크 ACL | **NACL** | 서브넷 레벨 트래픽 제어 |
| 라우팅 | **Route Table** | VPC 내 트래픽 라우팅 |
| MTU 최적화 | **VPC** | 점보 프레임 (9001 바이트) 지원 |
| 연결 드레이닝 | **Target Group** | Deregistration Delay 설정 |
| 실시간 통신 | **API Gateway WebSocket** | 양방향 WebSocket 지원 |
| 비동기 처리 | **Lambda** | 이벤트 기반 실행 |
| DNS 해석 | **Route 53 Resolver** | VPC 내 DNS |
| 시간 동기화 | **Amazon Time Sync** | NTP 서비스 |
| 게임 서버 | **GameLift** | UDP 기반 게임 호스팅 |
| 라이브 스트리밍 | **MediaLive** | 실시간 비디오 인코딩 |
| CDN | **CloudFront** | 전 세계 콘텐츠 전송 |
| DDoS 방어 | **AWS Shield** | SYN Flood, UDP Flood 방어 |
| WAF | **AWS WAF** | 애플리케이션 계층 공격 방어 |
| 네트워크 가속 | **Global Accelerator** | AWS 백본 네트워크 활용 |

---

*마지막 업데이트: 2026년 1월*
