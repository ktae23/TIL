# TCP/IP 기초와 핸드셰이크

## 목차
1. [TCP/IP 프로토콜 스택](#tcpip-프로토콜-스택)
2. [3-Way Handshake](#3-way-handshake)
3. [4-Way Handshake](#4-way-handshake)
4. [흐름 제어 (Flow Control)](#흐름-제어-flow-control)
5. [혼잡 제어 (Congestion Control)](#혼잡-제어-congestion-control)
6. [핵심 정리](#핵심-정리)

---

## TCP/IP 프로토콜 스택

```
┌─────────────────────────────┐
│     Application Layer       │  HTTP, FTP, SMTP, DNS
├─────────────────────────────┤
│     Transport Layer         │  TCP, UDP
├─────────────────────────────┤
│     Internet Layer          │  IP, ICMP, ARP
├─────────────────────────────┤
│     Network Access Layer    │  Ethernet, Wi-Fi
└─────────────────────────────┘
```

### TCP vs UDP 비교

| 특성 | TCP | UDP |
|------|-----|-----|
| 연결 방식 | 연결 지향 (Connection-oriented) | 비연결 (Connectionless) |
| 신뢰성 | 보장 (순서, 재전송) | 보장 안 함 |
| 속도 | 상대적으로 느림 | 빠름 |
| 사용 사례 | HTTP, FTP, 이메일 | 스트리밍, DNS, 게임 |

---

## 3-Way Handshake

TCP 연결 수립 과정으로, 클라이언트와 서버 간 신뢰성 있는 연결을 설정합니다.

```
Client                                Server
   │                                     │
   │  ──────── SYN (seq=x) ─────────►   │  LISTEN
   │                                     │
   │  ◄─── SYN-ACK (seq=y, ack=x+1) ──  │  SYN_RECEIVED
   │                                     │
   │  ──────── ACK (ack=y+1) ─────────► │  ESTABLISHED
   │                                     │
   │          ESTABLISHED               │
```

### 각 단계 설명

1. **SYN (Synchronize)**: 클라이언트가 서버에 연결 요청, 초기 시퀀스 번호(ISN) 전송
2. **SYN-ACK**: 서버가 요청을 수락하고 자신의 ISN과 클라이언트 ISN에 대한 ACK 전송
3. **ACK (Acknowledge)**: 클라이언트가 서버의 ISN에 대한 ACK 전송, 연결 완료

### Java Socket 예제

```java
// Server
try (ServerSocket serverSocket = new ServerSocket(8080)) {
    System.out.println("Server listening on port 8080");

    // accept()에서 3-way handshake 완료된 연결 수락
    Socket clientSocket = serverSocket.accept();
    System.out.println("Client connected: " + clientSocket.getInetAddress());

    // 데이터 송수신
    BufferedReader in = new BufferedReader(
        new InputStreamReader(clientSocket.getInputStream()));
    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

    String message = in.readLine();
    out.println("Echo: " + message);
}

// Client
try (Socket socket = new Socket("localhost", 8080)) {
    // 생성자에서 3-way handshake 수행
    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
    BufferedReader in = new BufferedReader(
        new InputStreamReader(socket.getInputStream()));

    out.println("Hello Server");
    System.out.println("Server response: " + in.readLine());
}
```

---

## 4-Way Handshake

TCP 연결 종료 과정으로, 양방향으로 데이터 전송이 완료되었음을 확인합니다.

```
Client                                Server
   │                                     │
   │  ──────── FIN (seq=x) ─────────►   │
   │                                     │  CLOSE_WAIT
   │  ◄─────── ACK (ack=x+1) ─────────  │
   │                                     │
   │          FIN_WAIT_2                │  (데이터 전송 완료 대기)
   │                                     │
   │  ◄─────── FIN (seq=y) ────────────  │
   │                                     │
   │  ──────── ACK (ack=y+1) ─────────► │  CLOSED
   │                                     │
   │          TIME_WAIT (2MSL)          │
   │          CLOSED                    │
```

### TIME_WAIT 상태의 중요성

```java
// TIME_WAIT 문제 해결 - SO_REUSEADDR 설정
ServerSocket serverSocket = new ServerSocket();
serverSocket.setReuseAddress(true);  // TIME_WAIT 상태의 포트 재사용 허용
serverSocket.bind(new InetSocketAddress(8080));
```

**TIME_WAIT가 필요한 이유:**
- 지연된 패킷 처리: 네트워크에 남아있는 중복 패킷 처리
- 신뢰성 있는 연결 종료: 마지막 ACK 손실 시 재전송 가능

---

## 흐름 제어 (Flow Control)

수신자의 버퍼 오버플로우를 방지하기 위해 송신 속도를 조절합니다.

### 슬라이딩 윈도우 (Sliding Window)

```
송신자 버퍼
┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐
│ 1 │ 2 │ 3 │ 4 │ 5 │ 6 │ 7 │ 8 │ 9 │10 │
└───┴───┴───┴───┴───┴───┴───┴───┴───┴───┘
  ▲       ▲               ▲
  │       │               │
  │       └── 송신 윈도우 ─┘
  │
  └── ACK 받은 마지막 바이트

수신자가 Window Size=4 광고:
- 송신자는 최대 4개 세그먼트까지 ACK 없이 전송 가능
- ACK 받으면 윈도우가 슬라이드
```

### 윈도우 크기 조절 예시

```
시간 t1: 수신자 Window Size = 4096 bytes
         → 송신자: 4KB 전송 가능

시간 t2: 수신자 버퍼 처리 느림, Window Size = 1024 bytes
         → 송신자: 전송 속도 감소

시간 t3: 수신자 버퍼 여유, Window Size = 8192 bytes
         → 송신자: 전송 속도 증가
```

---

## 혼잡 제어 (Congestion Control)

네트워크 혼잡을 감지하고 전송 속도를 조절하여 네트워크 붕괴를 방지합니다.

### 주요 알고리즘

#### 1. Slow Start (느린 시작)

```
cwnd (Congestion Window)
  │
  │                    ┌── ssthresh에 도달
  │                    ▼
 16├─────────────────●───────────
  │                ╱│
  8├──────────────●  │ ← Congestion Avoidance (선형 증가)
  │              ╱   │
  4├────────────●    │
  │            ╱     │
  2├──────────●      │
  │          ╱       │
  1├────────●        │
  │                  │
  └──────────────────┴───────────► RTT
     1   2   3   4   5   6
```

#### 2. Congestion Avoidance (혼잡 회피)

ssthresh 도달 후 선형적으로 증가 (매 RTT마다 cwnd += 1)

#### 3. Fast Retransmit & Fast Recovery

```
송신자: 1, 2, 3, 4, 5 전송
         ▼
패킷 2 손실
         ▼
수신자: ACK 1, ACK 1, ACK 1 (중복 ACK 3개)
         ▼
송신자: 타임아웃 대기 없이 즉시 패킷 2 재전송
         ▼
Fast Recovery: cwnd = ssthresh + 3, 선형 증가로 복구
```

### 현대적 혼잡 제어 알고리즘

```bash
# Linux에서 현재 혼잡 제어 알고리즘 확인
sysctl net.ipv4.tcp_congestion_control

# 사용 가능한 알고리즘 목록
sysctl net.ipv4.tcp_available_congestion_control

# BBR 알고리즘 적용 (Google 개발, 처리량 최적화)
sysctl -w net.ipv4.tcp_congestion_control=bbr
```

| 알고리즘 | 특징 | 사용 환경 |
|----------|------|----------|
| Reno | 전통적, 패킷 손실 기반 | 일반적 환경 |
| CUBIC | Linux 기본, 고대역폭 최적화 | 대부분의 Linux 서버 |
| BBR | 대역폭 + RTT 기반, 높은 처리량 | 클라우드, CDN |

---

## 핵심 정리

### 3-Way vs 4-Way Handshake

| 구분 | 3-Way Handshake | 4-Way Handshake |
|------|-----------------|-----------------|
| 목적 | 연결 수립 | 연결 종료 |
| 패킷 수 | 3개 | 4개 |
| 시작자 | 클라이언트 | 양쪽 모두 가능 |
| 핵심 플래그 | SYN, ACK | FIN, ACK |
| 상태 | ESTABLISHED로 전환 | TIME_WAIT 후 CLOSED |

### 흐름 제어 vs 혼잡 제어

| 구분 | 흐름 제어 | 혼잡 제어 |
|------|----------|----------|
| 대상 | 수신자 버퍼 | 네트워크 |
| 목적 | 버퍼 오버플로우 방지 | 네트워크 붕괴 방지 |
| 윈도우 | Receive Window (rwnd) | Congestion Window (cwnd) |
| 결정자 | 수신자 | 송신자 |
| 실제 윈도우 | min(rwnd, cwnd) | - |

### 실무 기반 핵심 질문

1. **Q: 왜 연결 종료에는 4-way가 필요한가?**
   - A: 양방향 스트림이므로 각 방향별로 FIN/ACK가 필요. 한쪽이 종료해도 다른 쪽은 데이터 전송 가능 (Half-Close)

2. **Q: SYN Flooding 공격이란?**
   - A: 대량의 SYN 패킷을 보내고 ACK를 보내지 않아 서버의 SYN_RECEIVED 상태를 고갈시키는 공격
   - 대응: SYN Cookie, 방화벽 설정

3. **Q: Nagle 알고리즘이란?**
   - A: 작은 패킷을 모아서 전송하여 네트워크 효율성 향상. 실시간 서비스에서는 비활성화 권장

```java
// Nagle 알고리즘 비활성화
socket.setTcpNoDelay(true);
```

---

*마지막 업데이트: 2026년 01월*
