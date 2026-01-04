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

**소켓 구조체 (간략화):**
```c
struct socket {
    int fd;                    // 파일 디스크립터
    int type;                  // SOCK_STREAM (TCP), SOCK_DGRAM (UDP)
    int state;                 // 연결 상태
    struct sockaddr local;     // 로컬 주소:포트
    struct sockaddr remote;    // 원격 주소:포트
    struct tcp_info tcp_info;  // TCP 제어 정보
    // 송수신 버퍼
    // 타이머 정보
    // 기타 제어 정보
};
```

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

**socket() 시스템 콜:**
```c
#include <sys/socket.h>

int socket(int domain, int type, int protocol);

// domain: 주소 체계
//   - AF_INET: IPv4
//   - AF_INET6: IPv6
//   - AF_UNIX: 유닉스 도메인 소켓
//
// type: 소켓 타입
//   - SOCK_STREAM: TCP (연결 지향, 신뢰성)
//   - SOCK_DGRAM: UDP (비연결, 빠름)
//   - SOCK_RAW: 원시 소켓 (직접 패킷 제어)
//
// protocol: 프로토콜 (보통 0으로 자동 선택)
```

**Python 예시:**
```python
import socket

# TCP 소켓 생성
tcp_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# UDP 소켓 생성
udp_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

# 소켓 옵션 설정
tcp_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
tcp_sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)

print(f"TCP Socket FD: {tcp_sock.fileno()}")
```

**실무 사례 - 소켓 옵션:**

| 옵션 | 레벨 | 설명 | 사용 시기 |
|------|------|------|-----------|
| SO_REUSEADDR | SOL_SOCKET | TIME_WAIT 포트 재사용 | 서버 재시작 시 |
| SO_KEEPALIVE | SOL_SOCKET | 연결 유지 확인 | 장시간 유휴 연결 |
| TCP_NODELAY | IPPROTO_TCP | Nagle 알고리즘 비활성화 | 실시간 통신 |
| SO_RCVBUF | SOL_SOCKET | 수신 버퍼 크기 | 대용량 전송 |
| SO_SNDBUF | SOL_SOCKET | 송신 버퍼 크기 | 대용량 전송 |

```python
import socket

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# Keep-Alive 설정 (60초마다 확인)
sock.setsockopt(socket.SOL_SOCKET, socket.SO_KEEPALIVE, 1)
sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_KEEPIDLE, 60)
sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_KEEPINTVL, 10)
sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_KEEPCNT, 3)

# Nagle 알고리즘 비활성화 (실시간 애플리케이션)
sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)

# 버퍼 크기 설정
sock.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, 65536)
sock.setsockopt(socket.SOL_SOCKET, socket.SO_SNDBUF, 65536)
```

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

**실무 사례 - connect() 타임아웃:**

```python
import socket
import errno

def connect_with_timeout(host, port, timeout=5):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(timeout)

    try:
        sock.connect((host, port))
        print(f"Connected to {host}:{port}")
        return sock
    except socket.timeout:
        print(f"Connection timeout after {timeout}s")
        sock.close()
        return None
    except socket.error as e:
        if e.errno == errno.ECONNREFUSED:
            print(f"Connection refused by {host}:{port}")
        elif e.errno == errno.EHOSTUNREACH:
            print(f"Host {host} unreachable")
        else:
            print(f"Connection error: {e}")
        sock.close()
        return None

# 사용 예시
sock = connect_with_timeout("www.example.com", 80, timeout=3)
```

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

**send() / write() 시스템 콜:**

```c
#include <sys/socket.h>

ssize_t send(int sockfd, const void *buf, size_t len, int flags);

// flags:
//   - 0: 일반 전송
//   - MSG_DONTWAIT: 논블로킹
//   - MSG_NOSIGNAL: SIGPIPE 시그널 억제
//   - MSG_MORE: 더 많은 데이터 대기 (Nagle 알고리즘 힌트)
```

**Python 예시:**
```python
import socket

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.connect(('www.example.com', 80))

# HTTP 요청 메시지
request = b"GET / HTTP/1.1\r\n"
request += b"Host: www.example.com\r\n"
request += b"Connection: close\r\n\r\n"

# 전체 데이터 전송 (부분 전송 시 재시도)
sock.sendall(request)

# 또는 send() 사용 (전송된 바이트 수 반환)
total_sent = 0
while total_sent < len(request):
    sent = sock.send(request[total_sent:])
    if sent == 0:
        raise RuntimeError("Socket connection broken")
    total_sent += sent
```

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

**Python으로 MSS 확인:**
```python
import socket

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.connect(('www.example.com', 80))

# TCP_MAXSEG 옵션으로 MSS 확인
mss = sock.getsockopt(socket.IPPROTO_TCP, socket.TCP_MAXSEG)
print(f"MSS: {mss} bytes")

sock.close()
```

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

```python
# 실무 사례 - 재전송 확인 (Linux)
$ ss -ti
tcp   ESTAB   0        0        192.168.1.100:53214   93.184.216.34:80
         cubic wscale:7,7 rto:204 rtt:3.5/1.2 ato:40 mss:1460
         #       ^^^^^^ RTO (밀리초)
         #              ^^^ RTT (왕복 시간)
```

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

**실무 사례 - 윈도우 크기 확인:**
```python
import socket
import struct

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.connect(('www.example.com', 80))

# SO_RCVBUF: 수신 버퍼 크기
rcvbuf = sock.getsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF)
print(f"Receive Buffer: {rcvbuf} bytes")

# SO_SNDBUF: 송신 버퍼 크기
sndbuf = sock.getsockopt(socket.SOL_SOCKET, socket.SO_SNDBUF)
print(f"Send Buffer: {sndbuf} bytes")

sock.close()
```

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

**실무 사례:**
```python
import socket

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# 실시간 통신 (게임, VoIP): Nagle OFF
sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)

# 대용량 파일 전송: Nagle ON (기본값)
# 별도 설정 없음

sock.connect(('www.example.com', 80))
```

### HTTP 응답 메시지를 수신한다

**recv() 시스템 콜:**

```python
import socket

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.connect(('www.example.com', 80))

# 요청 전송
request = b"GET / HTTP/1.1\r\nHost: www.example.com\r\n\r\n"
sock.sendall(request)

# 응답 수신
response = b""
while True:
    chunk = sock.recv(4096)  # 최대 4096바이트씩 수신
    if not chunk:
        break  # 연결 종료
    response += chunk

print(response.decode('utf-8', errors='ignore'))
sock.close()
```

**논블로킹 I/O:**

```python
import socket
import select

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.setblocking(False)  # 논블로킹 모드

try:
    sock.connect(('www.example.com', 80))
except BlockingIOError:
    pass  # 논블로킹 모드에서는 정상

# select()로 읽기 가능 대기
readable, writable, exceptional = select.select([sock], [sock], [sock], 5.0)

if writable:
    sock.send(b"GET / HTTP/1.1\r\nHost: www.example.com\r\n\r\n")

if readable:
    data = sock.recv(4096)
    print(data)

sock.close()
```

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

```python
import socket

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.connect(('www.example.com', 80))

# 방법 1: shutdown() - 단방향 종료
sock.shutdown(socket.SHUT_WR)   # 송신만 종료, 수신은 계속 가능
# 또는
sock.shutdown(socket.SHUT_RD)   # 수신만 종료
# 또는
sock.shutdown(socket.SHUT_RDWR) # 송수신 모두 종료

# 방법 2: close() - 즉시 종료
sock.close()
```

**실무 사례 - 우아한 종료 (Graceful Shutdown):**

```python
import socket

def graceful_shutdown(sock):
    """우아한 소켓 종료"""
    # 1. 송신 종료 (FIN 전송)
    sock.shutdown(socket.SHUT_WR)

    # 2. 남은 데이터 수신
    try:
        while True:
            data = sock.recv(4096)
            if not data:
                break
            # 데이터 처리...
    except Exception as e:
        print(f"Error during shutdown: {e}")

    # 3. 소켓 닫기
    sock.close()

# 사용 예시
sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.connect(('www.example.com', 80))
# ... 데이터 송수신 ...
graceful_shutdown(sock)
```

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

```python
import socket
import struct

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# SO_LINGER 설정: (on/off, timeout_seconds)
# on=1, timeout=0: 즉시 RST 전송 (비정상 종료)
# on=1, timeout>0: timeout 초 동안 남은 데이터 전송 시도
sock.setsockopt(socket.SOL_SOCKET, socket.SO_LINGER, struct.pack('ii', 1, 0))

sock.connect(('www.example.com', 80))
# ...
sock.close()  # RST 전송
```

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

**DNS 조회 (UDP 사용):**

```python
import socket

# UDP 소켓 생성
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

# DNS 서버에 쿼리 전송 (Google DNS: 8.8.8.8)
dns_query = b'\x00\x00\x01\x00\x00\x01\x00\x00\x00\x00\x00\x00' + \
            b'\x03www\x06google\x03com\x00\x00\x01\x00\x01'

sock.sendto(dns_query, ('8.8.8.8', 53))

# 응답 수신
data, addr = sock.recvfrom(512)
print(f"Received {len(data)} bytes from {addr}")

sock.close()
```

**실무 사례 - UDP 에코 서버/클라이언트:**

```python
import socket

# 서버
def udp_server(host='0.0.0.0', port=9999):
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind((host, port))
    print(f"UDP server listening on {host}:{port}")

    while True:
        data, addr = sock.recvfrom(1024)
        print(f"Received from {addr}: {data.decode()}")
        sock.sendto(data, addr)  # 에코

# 클라이언트
def udp_client(host='127.0.0.1', port=9999):
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

    message = b"Hello, UDP!"
    sock.sendto(message, (host, port))

    data, addr = sock.recvfrom(1024)
    print(f"Received from {addr}: {data.decode()}")

    sock.close()
```

### 음성 및 동영상 데이터

**실시간 스트리밍 (RTP over UDP):**

```python
import socket
import time

def stream_video(host, port):
    """간단한 비디오 스트리밍 (UDP)"""
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

    frame_number = 0
    while True:
        # 프레임 데이터 (실제로는 비디오 인코더에서 가져옴)
        frame_data = f"Frame {frame_number}".encode()

        # 전송 (재전송 없음, 손실 허용)
        sock.sendto(frame_data, (host, port))

        frame_number += 1
        time.sleep(1/30)  # 30 FPS

# 사용 예시
# stream_video('224.0.0.1', 5000)  # 멀티캐스트
```

**WebRTC - UDP 기반 실시간 통신:**

```python
# aiortc 라이브러리 사용 예시
from aiortc import RTCPeerConnection, RTCSessionDescription
import asyncio

async def run_webrtc():
    pc = RTCPeerConnection()

    @pc.on("track")
    async def on_track(track):
        if track.kind == "video":
            # 비디오 프레임 수신 (UDP)
            while True:
                frame = await track.recv()
                # 프레임 처리...

    # STUN/TURN 서버를 통한 NAT 통과
    # UDP 홀 펀칭...
```

**QUIC (HTTP/3) - UDP 기반 신뢰성 있는 전송:**

QUIC는 UDP 위에 TCP와 유사한 신뢰성 메커니즘을 구현했습니다.

```
TCP/TLS (HTTP/2):
  3-Way Handshake + TLS Handshake = 2 RTT

QUIC (HTTP/3):
  단일 Handshake = 0~1 RTT (0-RTT 재연결)
```

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

### 4. 프로그래밍 모범 사례

**연결 풀 사용:**
```python
from urllib3 import PoolManager

# HTTP 연결 풀
http = PoolManager(maxsize=10, block=True)

# 연결 재사용
response = http.request('GET', 'http://www.example.com')
```

**타임아웃 설정:**
```python
import socket

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.settimeout(5.0)  # 5초 타임아웃

try:
    sock.connect(('www.example.com', 80))
except socket.timeout:
    print("Connection timeout")
```

---

*마지막 업데이트: 2026년 1월*
