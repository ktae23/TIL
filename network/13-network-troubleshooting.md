# 네트워크 트러블슈팅

## 목차
1. [트러블슈팅 기본 접근법](#트러블슈팅-기본-접근법)
2. [tcpdump 활용](#tcpdump-활용)
3. [netstat/ss 명령어](#netstatss-명령어)
4. [패킷 손실 추적](#패킷-손실-추적)
5. [실전 트러블슈팅 시나리오](#실전-트러블슈팅-시나리오)
6. [핵심 정리](#핵심-정리)

---

## 트러블슈팅 기본 접근법

### OSI 레이어별 진단 순서

```
┌──────────────────────────────────────────────────────────────────┐
│                    Bottom-Up 접근법                               │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Layer 1 (Physical)                                              │
│  └── 케이블 연결, 네트워크 인터페이스 상태                         │
│      $ ip link show                                               │
│                                                                   │
│  Layer 2 (Data Link)                                             │
│  └── MAC 주소, ARP 테이블                                         │
│      $ arp -a                                                     │
│                                                                   │
│  Layer 3 (Network)                                               │
│  └── IP 설정, 라우팅, ICMP                                        │
│      $ ip addr show / ping / traceroute                          │
│                                                                   │
│  Layer 4 (Transport)                                             │
│  └── TCP/UDP 포트, 연결 상태                                      │
│      $ netstat / ss / tcpdump                                     │
│                                                                   │
│  Layer 7 (Application)                                           │
│  └── HTTP 응답, 애플리케이션 로그                                  │
│      $ curl -v / 애플리케이션 로그                                 │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 기본 진단 명령어

```bash
# 1. 네트워크 인터페이스 상태 확인
ip link show
# eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> ← UP 상태 확인

# 2. IP 주소 확인
ip addr show
# inet 10.0.1.100/24 scope global eth0

# 3. 라우팅 테이블 확인
ip route show
# default via 10.0.1.1 dev eth0

# 4. DNS 설정 확인
cat /etc/resolv.conf
# nameserver 8.8.8.8

# 5. 연결 테스트
ping -c 4 8.8.8.8           # ICMP 연결
telnet example.com 80        # TCP 포트 연결
nc -zv example.com 443       # 포트 스캔
curl -I https://example.com  # HTTP 응답 헤더
```

---

## tcpdump 활용

### 기본 사용법

```bash
# 기본 캡처 (모든 인터페이스)
sudo tcpdump -i any

# 특정 인터페이스
sudo tcpdump -i eth0

# 특정 호스트
sudo tcpdump host 10.0.1.100

# 특정 포트
sudo tcpdump port 8080

# 특정 포트 범위
sudo tcpdump portrange 8000-9000

# TCP만 캡처
sudo tcpdump tcp

# HTTP 트래픽 (포트 80)
sudo tcpdump -i eth0 'tcp port 80'
```

### 고급 필터링

```bash
# 출발지 또는 목적지 IP
sudo tcpdump src 10.0.1.100
sudo tcpdump dst 10.0.1.200

# 조합 필터
sudo tcpdump 'src 10.0.1.100 and dst port 443'
sudo tcpdump 'host 10.0.1.100 and (port 80 or port 443)'

# SYN 패킷만 캡처 (연결 시작)
sudo tcpdump 'tcp[tcpflags] & (tcp-syn) != 0'

# SYN-ACK 패킷 (연결 응답)
sudo tcpdump 'tcp[tcpflags] & (tcp-syn|tcp-ack) == (tcp-syn|tcp-ack)'

# RST 패킷 (연결 거부/리셋)
sudo tcpdump 'tcp[tcpflags] & (tcp-rst) != 0'

# HTTP GET 요청만
sudo tcpdump -s 0 -A 'tcp dst port 80 and (tcp[((tcp[12:1] & 0xf0) >> 2):4] = 0x47455420)'
```

### 출력 옵션

```bash
# 상세 출력
sudo tcpdump -v      # verbose
sudo tcpdump -vv     # more verbose
sudo tcpdump -vvv    # even more verbose

# 패킷 내용 출력
sudo tcpdump -X      # 헥사 + ASCII
sudo tcpdump -A      # ASCII만

# 타임스탬프 형식
sudo tcpdump -tttt   # 사람이 읽기 쉬운 형식

# 파일로 저장 (나중에 Wireshark로 분석)
sudo tcpdump -w capture.pcap

# 저장된 파일 읽기
sudo tcpdump -r capture.pcap

# 패킷 수 제한
sudo tcpdump -c 100  # 100개만 캡처
```

### 실전 예제

```bash
# 1. 3-way handshake 확인
sudo tcpdump -i eth0 'tcp[tcpflags] & (tcp-syn|tcp-ack) != 0' -nn

# 결과 예시:
# 10:00:01 IP 10.0.1.100.54321 > 10.0.1.200.80: Flags [S], seq 1234
# 10:00:01 IP 10.0.1.200.80 > 10.0.1.100.54321: Flags [S.], seq 5678, ack 1235
# 10:00:01 IP 10.0.1.100.54321 > 10.0.1.200.80: Flags [.], ack 5679

# 2. 느린 응답 추적 (타임스탬프 포함)
sudo tcpdump -i eth0 -tttt 'host api.example.com and port 443'

# 3. HTTP 요청/응답 확인
sudo tcpdump -i eth0 -A -s 0 'tcp port 80 and (((ip[2:2] - ((ip[0]&0xf)<<2)) - ((tcp[12]&0xf0)>>2)) != 0)'

# 4. 특정 시간 동안만 캡처
timeout 60 sudo tcpdump -i eth0 -w traffic.pcap
```

---

## netstat/ss 명령어

### netstat (전통적 도구)

```bash
# 모든 연결 상태 확인
netstat -an

# TCP 연결만
netstat -ant

# UDP 연결만
netstat -anu

# 리스닝 포트
netstat -tlnp

# 프로세스 정보 포함
sudo netstat -tulnp

# 연결 상태별 카운트
netstat -ant | awk '{print $6}' | sort | uniq -c | sort -rn

# 결과 예시:
#   150 ESTABLISHED
#    50 TIME_WAIT
#    20 CLOSE_WAIT
#    10 LISTEN
```

### ss (Socket Statistics - 권장)

```bash
# 모든 TCP 소켓
ss -ta

# 리스닝 소켓
ss -tln

# 연결된 소켓
ss -t state established

# 특정 포트 필터링
ss -t '( dport = :443 or sport = :443 )'

# 상세 정보 (타이머, 메모리 등)
ss -ti

# 결과 예시:
# ESTAB  0  0  10.0.1.100:54321  10.0.1.200:443
#    cubic wscale:7,7 rto:204 rtt:1.5/0.75 mss:1448 cwnd:10

# 연결 상태별 요약
ss -s
# Total: 500
# TCP:   300 (estab 150, closed 50, orphaned 0, timewait 50)
```

### 연결 상태 해석

```
┌──────────────────────────────────────────────────────────────────┐
│                    TCP 연결 상태                                  │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  LISTEN       서버가 연결 대기 중                                 │
│  SYN_SENT     클라이언트가 SYN 전송, 응답 대기                    │
│  SYN_RECEIVED 서버가 SYN 수신, SYN-ACK 전송                       │
│  ESTABLISHED  연결 수립 완료, 데이터 전송 가능                    │
│                                                                   │
│  FIN_WAIT_1   능동 종료 시작, FIN 전송                            │
│  FIN_WAIT_2   FIN에 대한 ACK 수신                                 │
│  CLOSE_WAIT   수동 종료, FIN 수신 (⚠️ 많으면 문제)                │
│  LAST_ACK     FIN 전송, 마지막 ACK 대기                           │
│  TIME_WAIT    연결 종료 대기 (2MSL)                               │
│  CLOSED       연결 종료                                           │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 문제 상태 진단

```bash
# 1. CLOSE_WAIT 상태가 많을 때 (애플리케이션에서 close() 미호출)
ss -t state close-wait
# 원인: 애플리케이션이 연결을 제대로 닫지 않음
# 해결: 코드에서 connection.close() 확인

# 2. TIME_WAIT 상태가 많을 때
ss -t state time-wait | wc -l
# 원인: 많은 단시간 연결
# 해결: 커넥션 풀 사용, tcp_tw_reuse 설정

# 3. 특정 포트 연결 수 확인
ss -t state established '( dport = :3306 )' | wc -l
```

---

## 패킷 손실 추적

### ping을 이용한 기본 테스트

```bash
# 기본 ping (패킷 손실률 확인)
ping -c 100 target.example.com

# 결과 분석:
# 100 packets transmitted, 98 received, 2% packet loss
# rtt min/avg/max/mdev = 1.234/2.345/5.678/0.987 ms

# 패킷 크기 지정 (MTU 문제 확인)
ping -c 10 -s 1472 target.example.com  # 1472 + 28(헤더) = 1500 MTU

# Don't Fragment 플래그와 함께
ping -c 10 -M do -s 1472 target.example.com
# "Message too long" → MTU 문제 있음
```

### traceroute/mtr을 이용한 경로 분석

```bash
# traceroute - 경로 추적
traceroute example.com

# 결과 예시:
#  1  10.0.1.1 (10.0.1.1)  1.234 ms  1.456 ms  1.678 ms
#  2  192.168.1.1 (192.168.1.1)  5.123 ms  5.456 ms  5.789 ms
#  3  * * *  ← 응답 없음 (방화벽 또는 패킷 손실)
#  4  72.14.215.85  20.123 ms  20.456 ms  20.789 ms

# TCP 포트로 traceroute (ICMP 차단 시)
traceroute -T -p 443 example.com

# mtr - 실시간 네트워크 진단 (traceroute + ping)
mtr example.com

# mtr 결과 예시:
#                          Loss%   Snt   Last   Avg  Best  Wrst
# 1. router.local          0.0%    10    1.2   1.3   1.1   1.5
# 2. isp-gateway           0.0%    10    5.4   5.5   5.2   6.0
# 3. backbone-router       2.0%    10   15.3  16.2  15.1  18.5  ← 손실 발생 지점
# 4. target.example.com    2.0%    10   20.1  21.3  19.8  25.0

# mtr 보고서 생성
mtr -r -c 100 example.com > mtr_report.txt
```

### 패킷 손실 원인 분석

```
┌──────────────────────────────────────────────────────────────────┐
│                    패킷 손실 원인과 해결                          │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  1. 네트워크 혼잡                                                │
│     - 증상: 특정 시간대에 손실 증가                               │
│     - 해결: QoS 설정, 대역폭 확장                                 │
│                                                                   │
│  2. 하드웨어 장애                                                │
│     - 증상: 특정 홉에서 지속적 손실                               │
│     - 해결: 장비 교체, 경로 변경                                  │
│                                                                   │
│  3. MTU 불일치                                                   │
│     - 증상: 큰 패킷에서만 손실                                    │
│     - 해결: MTU 조정, Path MTU Discovery                         │
│                                                                   │
│  4. 방화벽/보안 장비                                             │
│     - 증상: 특정 포트/프로토콜에서 손실                           │
│     - 해결: 방화벽 규칙 확인                                      │
│                                                                   │
│  5. 버퍼 오버플로우                                              │
│     - 증상: 고부하 시 손실 증가                                   │
│     - 해결: 버퍼 크기 조정, 처리량 개선                           │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 서버 네트워크 통계

```bash
# 인터페이스 에러 통계
ip -s link show eth0
# TX errors, RX errors, dropped 확인

# 네트워크 프로토콜 통계
netstat -s | grep -A5 "Tcp:"
# retransmitted, failed connection attempts 확인

# TCP 재전송 모니터링
watch -n 1 'netstat -s | grep retransmit'

# 소켓 버퍼 오버플로우
cat /proc/net/sockstat
# sockets: used 1024
# TCP: inuse 500 orphan 10 tw 50 alloc 600 mem 100
```

---

## 실전 트러블슈팅 시나리오

### 시나리오 1: API 타임아웃

```bash
# 1. 연결 가능 여부 확인
nc -zv api.example.com 443
# Connection to api.example.com 443 port [tcp/https] succeeded!

# 2. DNS 해석 시간 확인
time nslookup api.example.com
# real 0m0.050s ← 50ms (정상)

# 3. TCP 연결 시간 측정
curl -w "@curl-format.txt" -o /dev/null -s https://api.example.com/health

# curl-format.txt 내용:
#     time_namelookup:  %{time_namelookup}s\n
#        time_connect:  %{time_connect}s\n
#     time_appconnect:  %{time_appconnect}s\n
#    time_pretransfer:  %{time_pretransfer}s\n
#       time_redirect:  %{time_redirect}s\n
#  time_starttransfer:  %{time_starttransfer}s\n
#                     ----------\n
#          time_total:  %{time_total}s\n

# 4. 패킷 캡처로 지연 구간 확인
sudo tcpdump -i eth0 -tttt 'host api.example.com and port 443' -c 50
```

### 시나리오 2: 연결 거부 (Connection Refused)

```bash
# 1. 서비스 리스닝 확인
ss -tlnp | grep 8080
# 비어있음 → 서비스가 실행되지 않음

# 2. 프로세스 확인
ps aux | grep java
systemctl status my-app

# 3. 방화벽 확인 (AWS Security Group, iptables)
sudo iptables -L -n | grep 8080

# 4. 포트 바인딩 확인
sudo lsof -i :8080
```

### 시나리오 3: 간헐적 네트워크 문제

```bash
# 1. 지속적인 연결 모니터링
while true; do
    date
    curl -s -o /dev/null -w "%{http_code} %{time_total}s\n" \
        https://api.example.com/health
    sleep 5
done | tee connection_log.txt

# 2. mtr로 실시간 경로 품질 모니터링
mtr --report-cycles 1000 api.example.com

# 3. 시스템 리소스 연관 확인
sar -n DEV 1 60  # 네트워크 인터페이스 통계
sar -n TCP 1 60  # TCP 연결 통계
```

### 시나리오 4: CLOSE_WAIT 누적

```bash
# 1. CLOSE_WAIT 상태 확인
ss -t state close-wait

# 2. 관련 프로세스 확인
ss -tp state close-wait
# CLOSE-WAIT  0  0  10.0.1.100:54321  10.0.1.200:3306  users:(("java",pid=12345,fd=50))

# 3. 프로세스의 파일 디스크립터 확인
ls -la /proc/12345/fd | wc -l
# 열린 파일/소켓 수

# 4. 해결: 애플리케이션에서 커넥션 풀 설정 확인
# - maxIdle, maxActive, maxWait
# - 커넥션 반환 (close) 코드 확인
```

---

## 핵심 정리

### 트러블슈팅 도구 요약

| 도구 | 용도 | 주요 옵션 |
|------|------|----------|
| tcpdump | 패킷 캡처 및 분석 | -i, -w, -r, -A |
| ss/netstat | 소켓 상태 확인 | -t, -l, -n, -p |
| ping | 연결 및 지연 시간 | -c, -s, -M |
| traceroute/mtr | 경로 분석 | -T, -p, -r |
| curl | HTTP 레벨 테스트 | -w, -v, -o |
| nc (netcat) | 포트 연결 테스트 | -z, -v |

### 상황별 진단 순서

| 증상 | 1단계 | 2단계 | 3단계 |
|------|-------|-------|-------|
| 연결 불가 | ping, nc | traceroute | tcpdump |
| 타임아웃 | curl -w | tcpdump | ss 상태 |
| 간헐적 오류 | mtr | tcpdump -w | 로그 분석 |
| 느린 응답 | curl 시간 | tcpdump | 애플리케이션 로그 |

### 실무 기반 핵심 질문

1. **Q: CLOSE_WAIT 상태가 많을 때 원인과 해결 방법은?**
   - A: 애플리케이션에서 소켓을 close()하지 않음. 코드에서 try-with-resources 사용, 커넥션 풀 반환 로직 확인

2. **Q: TIME_WAIT 상태가 많을 때 문제점은?**
   - A: 포트 고갈 가능. 해결: tcp_tw_reuse 활성화, 커넥션 풀 사용으로 단시간 연결 줄이기

3. **Q: 패킷 손실 원인을 어떻게 찾나요?**
   - A: mtr로 경로별 손실률 확인, tcpdump로 재전송 패킷 분석, 서버 netstat -s로 통계 확인

4. **Q: tcpdump와 Wireshark의 차이점은?**
   - A: tcpdump는 CLI 도구로 서버에서 직접 캡처, Wireshark는 GUI로 상세 분석. 서버에서 tcpdump -w로 캡처 후 Wireshark로 분석하는 것이 일반적

---

*마지막 업데이트: 2026년 01월*
