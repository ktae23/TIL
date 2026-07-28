# Chapter 04. 액세스 회선을 통해 인터넷 내부로

## 개요

가정이나 회사의 네트워크에서 인터넷으로 연결되는 액세스 회선의 종류와 동작 원리를 다룹니다. ADSL, FTTH, 케이블 모뎀 등 다양한 액세스 기술과 ISP(인터넷 서비스 제공자)의 네트워크 구조, PPPoE 프로토콜의 동작을 실무 사례와 함께 살펴봅니다.

## 목차

1. [ADSL 기술을 이용한 액세스 회선](#1-adsl-기술을-이용한-액세스-회선)
2. [FTTH 기술을 이용한 액세스 회선](#2-ftth-기술을-이용한-액세스-회선)
3. [액세스 회선으로 이용하는 PPP와 터널링](#3-액세스-회선으로-이용하는-ppp와-터널링)
4. [프로바이더의 내부](#4-프로바이더의-내부)

---

## 1. ADSL 기술을 이용한 액세스 회선

### ADSL의 기본 개념

**ADSL (Asymmetric Digital Subscriber Line):**
- **비대칭 전송**: 다운로드 속도 > 업로드 속도
- **기존 전화선 사용**: 별도 배선 불필요
- **주파수 분할**: 음성과 데이터 동시 사용

**대칭 vs 비대칭:**

| 타입 | 다운로드 | 업로드 | 용도 |
|------|---------|--------|------|
| ADSL | 최대 24 Mbps | 최대 3.5 Mbps | 일반 가정 |
| SDSL | 동일 | 동일 | 기업 |
| VDSL | 최대 100 Mbps | 최대 40 Mbps | 근거리 고속 |

### ADSL 모뎀의 구조

**ADSL 연결 구성:**
```
[컴퓨터] ─ [라우터] ─ [ADSL 모뎀] ─ [스플리터] ─ 전화선
                                        │
                                   [전화기]
                                        │
                                        ↓
                                   전화국 (CO)
                                        │
                                   [DSLAM]
                                        │
                                   [BAS/BRAS]
                                        │
                                    ISP 네트워크
```

**스플리터 (Splitter):**
- **역할**: 음성 신호와 데이터 신호 분리
- **저역 통과 필터**: 음성 (0-4 kHz)
- **고역 통과 필터**: 데이터 (25-1,100 kHz)

### 주파수 분할

**ADSL 주파수 할당:**
```
0 kHz    4 kHz        25 kHz                138 kHz                1,100 kHz
|--------|------------|---------------------|------------------------|
  음성      보호대역      업로드 (Upstream)      다운로드 (Downstream)

음성 (POTS):    0 ~ 4 kHz
업로드:         25 ~ 138 kHz      (약 256개 서브채널)
다운로드:       138 ~ 1,100 kHz   (약 256개 서브채널)
```

**DMT (Discrete Multi-Tone) 변조:**
- 주파수 대역을 여러 서브채널로 분할
- 각 서브채널은 독립적으로 데이터 전송
- 노이즈가 많은 채널은 낮은 속도, 깨끗한 채널은 높은 속도

**실무 사례 - ADSL 속도 확인:**

```bash
# Linux - DSL 모뎀 통계 (모뎀이 Linux 기반인 경우)
$ cat /proc/net/atm/ADSL:0
AAL5 frames submitted:      123456
AAL5 frames received:       234567
Down stream rate:           8128 kbps
Up stream rate:             1024 kbps
SNR margin down:            12 dB
SNR margin up:              15 dB

# 모뎀 웹 인터페이스 접속
http://192.168.1.1 (일반적인 게이트웨이 주소)
```

### DSLAM (Digital Subscriber Line Access Multiplexer)

**DSLAM의 역할:**
1. 여러 ADSL 회선 집약
2. 아날로그-디지털 변환
3. ISP 백본 네트워크로 연결

**동작 과정:**
```
가정1 ADSL ─┐
가정2 ADSL ─┤
가정3 ADSL ─┼─ [DSLAM] ─ ATM/Ethernet ─ [BAS] ─ 인터넷
가정4 ADSL ─┤
가정N ADSL ─┘
```

### ADSL 속도 제한 요인

**거리와 속도 관계:**
```
거리          최대 속도
0 ~ 1.5 km    24 Mbps (ADSL2+)
1.5 ~ 3 km    12 Mbps
3 ~ 4.5 km    6 Mbps
4.5 ~ 5.5 km  3 Mbps
5.5 km 이상   연결 불가능
```

**실무 팁:**
- 전화국에서 멀수록 속도 감소
- SNR (Signal-to-Noise Ratio) 마진 확인
- 라인 품질 (Attenuation) 측정

**속도 테스트:**
```bash
# speedtest-cli 사용
$ pip install speedtest-cli
$ speedtest-cli

Retrieving speedtest.net configuration...
Testing from ISP (203.0.113.1)...
Selecting best server based on ping...
Download: 8.12 Mbit/s
Upload: 0.98 Mbit/s
```

**실무적 활용 사례:**

> ⚠️ **라인 탭핑 (Line Tapping)**: 전화선에 물리적으로 접근할 수 있는 공격자가 신호를 가로챌 수 있습니다. 현대에는 TLS/HTTPS가 기본이므로 콘텐츠 자체는 보호되지만, 메타데이터(접속 사이트)는 노출됩니다.

---

## 2. FTTH 기술을 이용한 액세스 회선

### FTTH의 개념

**FTTH (Fiber To The Home):**
- **광섬유**: 가입자 댁까지 직접 연결
- **고속 전송**: 1 Gbps ~ 10 Gbps
- **장거리**: 수십 km 전송 가능
- **저지연**: 빛의 속도로 전송

**ADSL vs FTTH 비교:**

| 특징 | ADSL | FTTH |
|------|------|------|
| 매체 | 전화선 (구리) | 광섬유 |
| 최대 속도 | 24 Mbps (다운) | 1~10 Gbps (대칭) |
| 거리 제한 | 5.5 km | 수십 km |
| 대칭성 | 비대칭 | 대칭 가능 |
| 간섭 | 있음 | 없음 |
| 가격 | 저렴 | 비쌈 (설치비) |

### FTTH 구조

**FTTx 분류:**
```
FTTH (Fiber To The Home):
  광섬유 → 가정까지

FTTB (Fiber To The Building):
  광섬유 → 건물까지 → UTP/동축

FTTC (Fiber To The Curb):
  광섬유 → 전봇대까지 → VDSL

FTTN (Fiber To The Node):
  광섬유 → 거점까지 → ADSL
```

### PON (Passive Optical Network)

**PON 구조:**
```
               [OLT]
                 |
              광섬유
                 |
            [스플리터]
           /    |    \
          /     |     \
    [ONT]    [ONT]    [ONT]
    (가정1)  (가정2)  (가정3)
```

**구성 요소:**
- **OLT (Optical Line Terminal)**: ISP 측 광 종단 장치
- **스플리터**: 광 신호 분배 (1:N, 일반적으로 1:32 또는 1:64)
- **ONT/ONU (Optical Network Terminal/Unit)**: 가입자 측 광 종단 장치

**PON 기술 종류:**

| 기술 | 다운로드 | 업로드 | 거리 | 분할비 |
|------|---------|--------|------|--------|
| EPON (Ethernet PON) | 1.25 Gbps | 1.25 Gbps | 20 km | 1:32 |
| GPON (Gigabit PON) | 2.488 Gbps | 1.244 Gbps | 20 km | 1:64 |
| 10G-EPON | 10 Gbps | 10 Gbps | 20 km | 1:32 |
| XG-PON | 10 Gbps | 2.5 Gbps | 20 km | 1:64 |
| NG-PON2 | 40 Gbps | 10 Gbps | 40 km | - |

### 광신호 전송 원리

**전반사 (Total Internal Reflection):**
```
     공기 (n=1.0)
  ----------------
    광섬유 코어 (n=1.5)
         /|
        / |  입사각 > 임계각
       /  |
      ↙   | 반사
     /    ↓
    /   클래딩 (n=1.45)
```

**광섬유 타입:**

**1. Single-Mode Fiber (SMF):**
- 코어 직경: 8-10 μm
- 파장: 1310 nm, 1550 nm
- 거리: 수십 km ~ 100 km
- 용도: 장거리, ISP 백본

**2. Multi-Mode Fiber (MMF):**
- 코어 직경: 50-62.5 μm
- 파장: 850 nm, 1300 nm
- 거리: 최대 2 km
- 용도: 건물 내, 데이터센터

**광 감쇠 (Attenuation):**
```
1310 nm: 0.35 dB/km
1550 nm: 0.2 dB/km (최소 감쇠)

예시: 20 km 전송 시
1550 nm: 20 × 0.2 = 4 dB 손실
```

**실무 사례 - 광섬유 테스트:**

```bash
# OTDR (Optical Time-Domain Reflectometer)로 측정
# - 광섬유 길이
# - 손실률
# - 단선 위치

# OLT에서 ONT 거리 및 상태 확인 (ISP 장비)
OLT# show gpon onu state
OnuIndex   Rx power     Distance
0/1/1      -20.5 dBm    2.5 km
0/1/2      -22.1 dBm    3.2 km
```

### ONT/ONU 장치

**ONT 기능:**
1. **광-전기 변환**: 광신호 ↔ 전기신호
2. **라우터 기능**: NAT, DHCP, 방화벽
3. **VoIP 게이트웨이**: 인터넷 전화
4. **IPTV**: 멀티캐스트 처리

**ONT 연결:**
```
[광섬유] → [ONT] → [라우터/공유기] → [PC/스마트폰]
                 → [전화기] (VoIP)
                 → [IPTV 셋톱박스]
```

**실무 사례 - ONT 설정:**

```bash
# ONT 관리 페이지 접속 (일반적)
http://192.168.1.1

# 광 수신 레벨 확인 (ONT CLI, 제조사별 상이)
ONT> show optical-module info
Temperature: 45°C
Tx Power: 2.5 dBm
Rx Power: -22.1 dBm  # -28 dBm 이하면 문제
Voltage: 3.3V

# 광 링크 상태
ONT> show gpon state
State: O5 (GPON_WORKING)
ONU-ID: 5
Distance: 2500 m
```

**AWS 서비스 활용:**

| FTTH 관련 개념 | AWS 서비스 | 설명 |
|---------------|-----------|------|
| 전용선 연결 | **Direct Connect** | AWS와 온프레미스 직접 연결 (1Gbps, 10Gbps, 100Gbps) |
| 광섬유 백본 | **AWS 글로벌 인프라** | 전 세계 리전 간 전용 광섬유 연결 |
| 저지연 연결 | **Direct Connect Gateway** | 여러 리전/VPC에 단일 연결 |
| 하이브리드 클라우드 | **Outposts** | 온프레미스에 AWS 인프라 확장 |

**실무적 활용 사례:**

> ⚠️ **광섬유 탭핑**: 광섬유도 물리적 접근 시 신호를 분기할 수 있습니다. PON 네트워크에서는 스플리터를 통해 다른 가입자의 데이터를 볼 수 있으므로, 암호화(ONU-OLT 간 AES)가 중요합니다.

---

## 3. 액세스 회선으로 이용하는 PPP와 터널링

### PPP (Point-to-Point Protocol)

**PPP의 역할:**
- **인증**: 사용자명/비밀번호 확인
- **IP 주소 할당**: ISP에서 동적 할당
- **압축**: 데이터 압축
- **암호화**: 선택적

**PPP 프로토콜 스택:**
```
+-------------------+
|   NCP (Network    |  IP, IPv6 주소 할당
|   Control         |
|   Protocol)       |
+-------------------+
|   LCP (Link       |  링크 설정, 인증
|   Control         |
|   Protocol)       |
+-------------------+
|   PPP Frame       |  프레임 구조
+-------------------+
|   Physical Layer  |  ADSL, 시리얼 등
+-------------------+
```

**PPP 연결 과정:**
```
1. LCP 협상:
   - 최대 프레임 크기 (MRU)
   - 인증 방식 (PAP, CHAP)
   - 압축 방식

2. 인증:
   - PAP (Password Authentication Protocol): 평문
   - CHAP (Challenge Handshake Authentication Protocol): 암호화

3. NCP 협상:
   - IP 주소 할당
   - DNS 서버 주소

4. 데이터 전송

5. 연결 종료
```

### PPPoE (PPP over Ethernet)

**PPPoE의 필요성:**
- ADSL/FTTH는 이더넷 기반
- PPP 인증 메커니즘 재사용
- 가입자별 세션 관리

**PPPoE 프레임 구조:**
```
+----------------+---------------+----------+-----+-----+
| Ethernet Header| PPPoE Header  | PPP      | Payload | FCS |
| (14 bytes)     | (6 bytes)     | (2 bytes)|         |     |
+----------------+---------------+----------+---------+-----+
```

**PPPoE 헤더:**
```
VER (4bit) | TYPE (4bit) | CODE (8bit) | SESSION_ID (16bit) | LENGTH (16bit)
```

**CODE 값:**
- `0x09`: PADI (PPPoE Active Discovery Initiation)
- `0x07`: PADO (PPPoE Active Discovery Offer)
- `0x19`: PADR (PPPoE Active Discovery Request)
- `0x65`: PADS (PPPoE Active Discovery Session-confirmation)
- `0x00`: Session Data
- `0xa7`: PADT (PPPoE Active Discovery Terminate)

**PPPoE 연결 과정:**

```
클라이언트                     BAS (Broadband Access Server)

1. PADI (브로드캐스트)
   "PPPoE 서버 찾습니다"
   --------------------------------→

2. PADO (유니캐스트)
   "저 여기 있어요"
   ←--------------------------------

3. PADR
   "연결 요청합니다"
   --------------------------------→

4. PADS
   "세션 ID: 1234"
   ←--------------------------------

5. LCP 협상
   ←----------------------------→

6. 인증 (PAP/CHAP)
   ←----------------------------→

7. IPCP 협상 (IP 주소 할당)
   IP: 203.0.113.50
   DNS: 8.8.8.8
   ←--------------------------------

8. 데이터 전송
   ←----------------------------→

9. PADT (종료)
   --------------------------------→
```

**실무 사례 - PPPoE 설정:**

**Linux (pppd):**
```bash
# PPPoE 패키지 설치
$ sudo apt install pppoeconf pppoe

# 자동 설정
$ sudo pppoeconf

# 수동 설정 - /etc/ppp/peers/dsl-provider
noipdefault
defaultroute
replacedefaultroute
hide-password
lcp-echo-interval 20
lcp-echo-failure 3
connect /bin/true
noauth
persist
mtu 1492
mru 1492
noaccomp
nic-eth0
user "your-username@isp.com"
plugin rp-pppoe.so

# 비밀번호 - /etc/ppp/chap-secrets
your-username@isp.com * your-password *

# 연결
$ sudo pon dsl-provider

# 상태 확인
$ ifconfig ppp0
ppp0: flags=4305<UP,POINTOPOINT,RUNNING,NOARP,MULTICAST>  mtu 1492
        inet 203.0.113.50  netmask 255.255.255.255  destination 10.64.64.64
        ppp  txqueuelen 3

$ ip route
default via 10.64.64.64 dev ppp0

# 연결 해제
$ sudo poff dsl-provider

# 로그 확인
$ tail -f /var/log/syslog | grep pppd
```

**실무적 활용 사례:**

> ⚠️ **PPPoE 세션 하이재킹**: 공격자가 같은 네트워크에서 PADO 패킷을 스푸핑하면 가짜 BAS로 연결을 유도할 수 있습니다. 이를 통해 인증 정보를 탈취하거나 트래픽을 가로챌 수 있습니다.

> ⚠️ **PAP 인증 취약점**: PAP는 비밀번호를 평문으로 전송합니다. 네트워크 스니핑으로 인증 정보가 노출될 수 있으므로, CHAP이나 EAP를 사용해야 합니다.

**OpenWrt/DD-WRT (라우터):**
```bash
# 웹 인터페이스:
Network → Interfaces → Add New Interface
Protocol: PPPoE
Username: your-username@isp.com
Password: your-password
```

**Cisco 라우터:**
```
# PPPoE 클라이언트 설정
Router(config)# interface dialer 0
Router(config-if)# ip address negotiated
Router(config-if)# encapsulation ppp
Router(config-if)# dialer pool 1
Router(config-if)# ppp authentication chap callin
Router(config-if)# ppp chap hostname your-username@isp.com
Router(config-if)# ppp chap password your-password

Router(config)# interface gigabitEthernet 0/0
Router(config-if)# no ip address
Router(config-if)# pppoe enable
Router(config-if)# pppoe-client dial-pool-number 1

# 상태 확인
Router# show pppoe session
Router# show interface dialer 0
```

### PPPoE MTU 문제

**MTU (Maximum Transmission Unit) 감소:**
```
일반 이더넷 MTU: 1500 bytes

PPPoE 오버헤드:
  - PPPoE 헤더: 6 bytes
  - PPP 헤더: 2 bytes
  - 총: 8 bytes

PPPoE MTU: 1500 - 8 = 1492 bytes
```

**MTU 불일치 문제:**
```
PC (MTU 1500) → 라우터 (MTU 1492) → 패킷 분할 → 성능 저하

증상:
- 일부 웹사이트 로딩 실패
- SSH 연결 후 멈춤
- 대용량 파일 전송 실패
```

**해결 방법:**

**1. MSS Clamping (TCP):**
```bash
# iptables MSS 조정
$ sudo iptables -t mangle -A POSTROUTING -p tcp --tcp-flags SYN,RST SYN -j TCPMSS --clamp-mss-to-pmtu

# 또는 고정 값
$ sudo iptables -t mangle -A POSTROUTING -p tcp --tcp-flags SYN,RST SYN -j TCPMSS --set-mss 1452
```

**2. Path MTU Discovery:**
```bash
# PMTUD 활성화 (Linux, 기본 활성화)
$ sysctl net.ipv4.ip_no_pmtu_disc
net.ipv4.ip_no_pmtu_disc = 0  # 0: 활성화

# ICMP Fragmentation Needed 허용 (방화벽)
$ sudo iptables -A INPUT -p icmp --icmp-type fragmentation-needed -j ACCEPT
```

**3. MTU 수동 설정:**
```bash
# 인터페이스 MTU 변경
$ sudo ip link set dev eth0 mtu 1492

# PPP 인터페이스 MTU
$ sudo ip link set dev ppp0 mtu 1492

# 영구 설정 (Ubuntu - /etc/network/interfaces)
auto eth0
iface eth0 inet dhcp
    mtu 1492
```

**MTU 테스트:**
```bash
# ping으로 MTU 확인 (-M do: Don't Fragment 플래그)
$ ping -M do -s 1464 www.example.com  # 1464 + 28(IP+ICMP) = 1492
$ ping -M do -s 1472 www.example.com  # 1472 + 28 = 1500 (실패 가능)

# tracepath로 Path MTU 확인
$ tracepath www.example.com
 1?: [LOCALHOST]                      pmtu 1500
 1:  gateway                          0.345ms
 2:  isp-router                       pmtu 1492
```

### L2TP (Layer 2 Tunneling Protocol)

**L2TP/IPsec VPN:**
- L2TP: 터널링
- IPsec: 암호화

**L2TP 연결 예시 (Linux):**
```bash
# xl2tpd 설치
$ sudo apt install xl2tpd

# 설정 - /etc/xl2tpd/xl2tpd.conf
[lac vpn-connection]
lns = vpn.example.com
ppp debug = yes
pppoptfile = /etc/ppp/options.l2tpd
length bit = yes

# PPP 옵션 - /etc/ppp/options.l2tpd
ipcp-accept-local
ipcp-accept-remote
refuse-eap
require-mschap-v2
noccp
noauth
idle 1800
mtu 1410
mru 1410
defaultroute
usepeerdns
connect-delay 5000
name your-username
password your-password

# 연결
$ sudo systemctl start xl2tpd
$ sudo sh -c 'echo "c vpn-connection" > /var/run/xl2tpd/l2tp-control'

# 상태 확인
$ ip addr show ppp0
```

**AWS 서비스 활용:**

| VPN 개념 | AWS 서비스 | 설명 |
|---------|-----------|------|
| Site-to-Site VPN | **AWS Site-to-Site VPN** | 온프레미스와 VPC 연결 (IPsec) |
| 원격 접속 VPN | **Client VPN** | 개별 사용자 VPN 접속 |
| 터널링 | **Transit Gateway** | 복잡한 네트워크 토폴로지 관리 |
| VPN + Direct Connect | **VPN over Direct Connect** | 전용선에 추가 암호화 |

**실무적 활용 사례:**

> ⚠️ **VPN 프로토콜 취약점**: L2TP/IPsec은 안전하지만, PPTP는 알려진 취약점이 있습니다. AWS Site-to-Site VPN은 IKEv1/IKEv2와 강력한 암호화를 지원합니다.

---

## 4. 프로바이더의 내부

### ISP (Internet Service Provider) 구조

**ISP 네트워크 계층:**
```
[가입자]
   ↓
[액세스 네트워크]
   - DSLAM, OLT
   - BAS/BRAS
   ↓
[집약 네트워크]
   - Metro Ethernet
   - MPLS 네트워크
   ↓
[코어 네트워크]
   - 고속 라우터
   - DWDM (광 전송)
   ↓
[피어링/트랜짓]
   - IXP (Internet Exchange Point)
   - 다른 ISP
   ↓
[인터넷]
```

### BAS/BRAS (Broadband Access Server)

**BAS 역할:**
1. **PPPoE 세션 종단**: 수천~수만 세션 관리
2. **인증**: RADIUS 서버와 연동
3. **IP 주소 할당**: DHCP 또는 IPCP
4. **트래픽 셰이핑**: QoS, 대역폭 제한
5. **과금**: 사용량 측정

**BAS 구조:**
```
       [RADIUS 서버]
       (인증/과금)
            ↑
            |
       [BAS/BRAS]
       /    |    \
      /     |     \
[DSLAM] [DSLAM] [OLT]
  |       |       |
[가입자] [가입자] [가입자]
```

**RADIUS (Remote Authentication Dial-In User Service) 인증:**
```
BAS → RADIUS: Access-Request (사용자명/비밀번호)
RADIUS → 데이터베이스: 사용자 확인
RADIUS → BAS: Access-Accept (IP, DNS, QoS 정책)
BAS → 가입자: IP 주소 할당
```

**실무 사례 - RADIUS 패킷:**
```
Access-Request:
  User-Name: "user@isp.com"
  User-Password: (암호화됨)
  NAS-IP-Address: 10.1.1.1
  NAS-Port: 1234

Access-Accept:
  Framed-IP-Address: 203.0.113.50
  Framed-IP-Netmask: 255.255.255.255
  Primary-DNS: 8.8.8.8
  Secondary-DNS: 8.8.4.4
  Session-Timeout: 86400
```

### IP 주소 할당

**동적 IP vs 고정 IP:**

| 타입 | 특징 | 용도 | 가격 |
|------|------|------|------|
| 동적 IP | 연결 시마다 변경 | 일반 가정 | 저렴 |
| 고정 IP | 항상 동일 | 서버 운영 | 비쌈 |

**IP 주소 풀 관리:**
```
ISP의 공인 IP 블록: 203.0.113.0/24 (256개)

할당 계획:
- 네트워크: 203.0.113.0
- 게이트웨이: 203.0.113.1
- BAS: 203.0.113.2
- 가입자 풀: 203.0.113.10 ~ 203.0.113.250 (241개)
- 예약: 203.0.113.251 ~ 203.0.113.254
- 브로드캐스트: 203.0.113.255
```

**DHCP vs IPCP:**
```
DHCP (Dynamic Host Configuration Protocol):
  - 이더넷 네트워크
  - 브로드캐스트 기반
  - 케이블 모뎀, FTTH (일부)

IPCP (IP Control Protocol):
  - PPP 네트워크
  - Point-to-Point
  - ADSL PPPoE, Dial-up
```

### NOC (Network Operations Center)

**NOC 역할:**
1. **네트워크 모니터링**: 24/7 감시
2. **장애 대응**: 신속한 복구
3. **트래픽 분석**: 병목 지점 파악
4. **보안**: DDoS 공격 대응

**모니터링 도구:**
```
- SNMP (Simple Network Management Protocol)
- Syslog
- NetFlow/sFlow
- Grafana + Prometheus
- Zabbix
- Nagios
```

**실무 사례 - SNMP 모니터링:**

```bash
# SNMP 쿼리
$ snmpwalk -v2c -c public router.isp.com ifDescr
IF-MIB::ifDescr.1 = STRING: GigabitEthernet0/0
IF-MIB::ifDescr.2 = STRING: GigabitEthernet0/1

# 인터페이스 트래픽
$ snmpget -v2c -c public router.isp.com ifInOctets.1
IF-MIB::ifInOctets.1 = Counter32: 1234567890

# SNMP Trap (알람)
$ snmptrap -v2c -c public noc.isp.com '' linkDown ifIndex i 1
```

### 트래픽 측정 및 과금

**NetFlow:**
```
라우터 → NetFlow Collector

Flow 레코드:
  Source IP: 203.0.113.50
  Dest IP: 8.8.8.8
  Source Port: 54321
  Dest Port: 53
  Protocol: UDP
  Bytes: 1024
  Packets: 10
  Start Time: 2026-01-04 10:00:00
  End Time: 2026-01-04 10:00:05
```

**Cisco NetFlow 설정:**
```
Router(config)# interface gigabitEthernet 0/0
Router(config-if)# ip flow ingress
Router(config-if)# ip flow egress

Router(config)# ip flow-export version 9
Router(config)# ip flow-export destination 10.1.1.100 2055

# 확인
Router# show ip flow export
Router# show ip cache flow
```

**nfdump (NetFlow 분석):**
```bash
# NetFlow 데이터 수집
$ nfcapd -w /var/cache/nfdump -p 2055

# 상위 트래픽 분석
$ nfdump -R /var/cache/nfdump -s srcip -n 10
Top 10 Source IP:
  203.0.113.50:  10.5 GB
  203.0.113.51:   8.2 GB
  ...

# 특정 IP 트래픽
$ nfdump -R /var/cache/nfdump 'src ip 203.0.113.50'
```

### 피어링과 트랜짓

**피어링 (Peering):**
- **무료 트래픽 교환**: 대등한 ISP 간
- **직접 연결**: Private Peering
- **공용 교환점**: IXP (Internet Exchange Point)

**트랜짓 (Transit):**
- **유료 연결**: 상위 ISP (Tier 1)에게 돈 지불
- **전체 인터넷 접근**: Full routing table

**ISP 계층:**
```
Tier 1 ISP (전 세계 백본):
  - AT&T, Verizon, NTT 등
  - 서로 피어링 (무료)
  - 전체 인터넷 라우팅 테이블 보유

Tier 2 ISP (지역 ISP):
  - Tier 1에게 트랜짓 구매
  - 같은 레벨과 피어링

Tier 3 ISP (로컬 ISP):
  - Tier 2에게 트랜짓 구매
  - 최종 사용자에게 서비스
```

**IXP (Internet Exchange Point) 예시:**
- **한국**: KINX, KORNET IX
- **미국**: Equinix, DE-CIX
- **일본**: JPIX, JPNAP

**BGP 피어링 설정 (Cisco):**
```
# eBGP 피어링
Router(config)# router bgp 65001
Router(config-router)# neighbor 203.0.113.1 remote-as 65002
Router(config-router)# neighbor 203.0.113.1 description Peer-ISP-XYZ

# 네트워크 광고
Router(config-router)# network 203.0.113.0 mask 255.255.255.0

# BGP 상태 확인
Router# show ip bgp summary
Neighbor        V    AS MsgRcvd MsgSent   TblVer  InQ OutQ Up/Down  State
203.0.113.1     4 65002   12345   12340        5    0    0 01:23:45 Established

Router# show ip bgp
Network          Next Hop            Metric LocPrf Weight Path
*> 203.0.113.0/24  0.0.0.0                  0         32768 i
*  8.8.8.0/24      203.0.113.1              0             0 65002 15169 i
```

**AWS 서비스 활용:**

| ISP 개념 | AWS 서비스 | 설명 |
|---------|-----------|------|
| 글로벌 백본 | **CloudFront** | 전 세계 엣지 로케이션 |
| 피어링 | **AWS Direct Connect 파트너** | 글로벌 파트너 네트워크 |
| IXP 연결 | **AWS 리전** | 주요 IXP 연결 보유 |
| Tier 1 ISP 역할 | **AWS 글로벌 네트워크** | 자체 해저 케이블 보유 |
| BGP 라우팅 | **AWS BYOIP** | 자체 IP 대역 AWS에서 사용 |

**실무적 활용 사례:**

> ⚠️ **ISP 레벨 감시**: ISP는 모든 트래픽을 볼 수 있습니다. DNS 쿼리, SNI(서버 이름), IP 주소로 방문 사이트를 파악할 수 있습니다. VPN이나 Tor로 우회할 수 있지만, 완전한 익명성은 어렵습니다.

> ⚠️ **RADIUS 서버 침해**: ISP의 RADIUS 서버가 해킹되면 모든 가입자의 인증 정보가 유출됩니다. ISP 선택 시 보안 인증(ISO 27001 등)을 확인하는 것이 좋습니다.

### DPI (Deep Packet Inspection)

**DPI 용도:**
1. **트래픽 분류**: QoS 적용
2. **차단**: 불법 콘텐츠, P2P
3. **과금**: 서비스별 요금제
4. **보안**: 악성코드 탐지

**DPI 동작:**
```
패킷 도착
  ↓
헤더 분석 (IP, 포트)
  ↓
페이로드 검사
  - HTTP Host 헤더
  - TLS SNI (Server Name Indication)
  - 애플리케이션 시그니처
  ↓
정책 적용
  - 허용/차단
  - QoS (대역폭 제한)
  - 로깅
```

**TLS SNI (암호화 트래픽 분류):**
```
Client Hello (평문):
  SNI: www.youtube.com

DPI → "YouTube 트래픽" 분류 → QoS 정책 적용
```

**우회 방법:**
- **Encrypted SNI (eSNI)**: SNI 암호화
- **DoH (DNS over HTTPS)**: DNS 암호화
- **VPN/Tor**: 트래픽 전체 암호화

**AWS 서비스 활용:**

| DPI 관련 | AWS 서비스 | 설명 |
|---------|-----------|------|
| 트래픽 분석 | **VPC Flow Logs** | VPC 트래픽 메타데이터 수집 |
| 패킷 검사 | **Network Firewall** | 관리형 IDS/IPS |
| 콘텐츠 필터링 | **AWS WAF** | 웹 트래픽 검사 및 필터링 |
| DNS 보안 | **Route 53 Resolver DNS Firewall** | DNS 쿼리 필터링 |

**실무적 활용 사례:**

> ⚠️ **DPI 프라이버시 문제**: ISP의 DPI는 사용자 행동 패턴을 분석할 수 있습니다. 어떤 서비스를 언제 사용하는지, 스트리밍 시청 습관 등을 파악할 수 있습니다. ECH (Encrypted Client Hello)와 DoH가 이를 완화합니다.

> ⚠️ **DPI 기반 차단 우회**: 일부 국가에서는 DPI로 VPN을 감지하고 차단합니다. 트래픽을 일반 HTTPS처럼 보이게 위장하는 기술(Obfsproxy, Shadowsocks)이 사용됩니다.

---

## 실무 팁

### 1. 액세스 회선 선택 가이드

| 용도 | 권장 기술 | 이유 |
|------|----------|------|
| 일반 가정 | FTTH (1Gbps) | 고속, 안정적 |
| 소규모 사무실 | FTTH/VDSL | 비용 대비 성능 |
| 서버 호스팅 | 전용선 | 고정 IP, 대칭 속도, SLA |
| 임시 사무실 | LTE/5G | 빠른 설치 |

### 2. 인터넷 속도 문제 해결

```bash
# 1. 속도 테스트
$ speedtest-cli

# 2. 라우터 재부팅
$ sudo reboot

# 3. 회선 품질 확인 (ADSL)
모뎀 관리 페이지 → 통계
- SNR Margin: 12 dB 이상
- Attenuation: 50 dB 이하

# 4. MTU 최적화
$ sudo ip link set dev eth0 mtu 1492

# 5. DNS 변경
# /etc/resolv.conf
nameserver 8.8.8.8
nameserver 1.1.1.1

# 6. ISP 고객센터 문의
```

### 3. 네트워크 보안

**홈 라우터 보안 체크리스트:**
```
1. 관리자 비밀번호 변경
2. 펌웨어 업데이트
3. WPA3 사용 (무선)
4. 원격 관리 비활성화
5. UPnP 비활성화 (필요 시만)
6. 방화벽 활성화
7. 게스트 네트워크 분리
```

**라우터 설정 예시:**
```bash
# SSH로 라우터 접속 (OpenWrt)
$ ssh root@192.168.1.1

# 비밀번호 변경
# passwd

# 펌웨어 업데이트
# opkg update
# opkg list-upgrades

# 방화벽 확인
# uci show firewall
```

### 4. PPPoE 문제 해결

**연결 실패 체크리스트:**
```
1. 케이블 연결 확인
2. 사용자명/비밀번호 재확인
3. MTU 설정 (1492)
4. ISP 장애 확인
5. 모뎀/ONT 재부팅
6. 로그 확인:
   $ tail -f /var/log/syslog | grep pppd
```

**일반적인 오류:**
```
LCP timeout: 네트워크 연결 문제
Authentication failed: 인증 정보 오류
No response to N echo-requests: 회선 불안정
```

---

## 네트워크 기술 비교표

### 액세스 기술 종류

| 기술 | 속도 (다운/업) | 매체 | 거리 제한 | 대칭성 | 비용 |
|------|--------------|------|-----------|--------|------|
| ADSL | 24M/3.5M | 전화선 | 5.5 km | 비대칭 | 저 |
| VDSL | 100M/40M | 전화선 | 1.5 km | 비대칭 | 중 |
| 케이블 모뎀 | 1G/35M | 동축 | 수 km | 비대칭 | 중 |
| FTTH (GPON) | 2.5G/1.2G | 광섬유 | 20 km | 비대칭 | 중 |
| FTTH (10G-EPON) | 10G/10G | 광섬유 | 20 km | 대칭 | 고 |
| 전용선 | 사용자 정의 | 광섬유 | 제한 없음 | 대칭 | 매우 고 |
| LTE | 150M/50M | 무선 | 수 km | 비대칭 | 중 |
| 5G | 1G+/100M+ | 무선 | 수백 m | 비대칭 | 중 |

---

## AWS 서비스 전체 요약

| 네트워크 개념 | AWS 서비스 | 설명 |
|-------------|-----------|------|
| 전용선 연결 | **Direct Connect** | 온프레미스-AWS 전용 광섬유 연결 |
| 전용선 게이트웨이 | **Direct Connect Gateway** | 여러 리전/VPC 연결 |
| Site-to-Site VPN | **AWS Site-to-Site VPN** | IPsec 기반 온프레미스 연결 |
| 원격 접속 VPN | **Client VPN** | 개별 사용자 VPN |
| 허브 라우팅 | **Transit Gateway** | 복잡한 네트워크 토폴로지 |
| 하이브리드 클라우드 | **Outposts** | 온프레미스에 AWS 확장 |
| 글로벌 네트워크 | **Global Accelerator** | AWS 백본 활용 가속 |
| CDN | **CloudFront** | 전 세계 엣지 로케이션 |
| 트래픽 분석 | **VPC Flow Logs** | 네트워크 트래픽 로깅 |
| IDS/IPS | **Network Firewall** | 관리형 패킷 검사 |
| DNS 보안 | **Route 53 Resolver DNS Firewall** | DNS 쿼리 필터링 |
| WAF | **AWS WAF** | 웹 애플리케이션 보호 |
| DDoS 보호 | **AWS Shield** | L3/L4/L7 DDoS 방어 |
| 자체 IP 사용 | **BYOIP (Bring Your Own IP)** | 온프레미스 IP 대역 AWS에서 사용 |
| 멀티 리전 연결 | **Cloud WAN** | 글로벌 네트워크 관리 |
| PoP 위치 | **Local Zones** | 사용자 근접 컴퓨팅 |

---

*마지막 업데이트: 2026년 1월*
