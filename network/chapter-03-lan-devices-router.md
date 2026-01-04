# Chapter 03. 케이블의 앞은 LAN 기기였다

## 개요

패킷이 케이블을 통과한 후 만나는 LAN 기기들(허브, 스위치, 라우터)의 동작 원리를 살펴봅니다. 각 장비가 어떻게 패킷을 처리하고 전달하는지, 그리고 라우터가 어떻게 인터넷으로 패킷을 라우팅하는지 실무 사례와 함께 다룹니다.

## 목차

1. [케이블과 리피터, 허브 속을 신호가 흐른다](#1-케이블과-리피터-허브-속을-신호가-흐른다)
2. [스위칭 허브는 주소 테이블로 중계한다](#2-스위칭-허브는-주소-테이블로-중계한다)
3. [라우터의 패킷 중계 동작](#3-라우터의-패킷-중계-동작)
4. [라우터의 부가 기능](#4-라우터의-부가-기능)

---

## 1. 케이블과 리피터, 허브 속을 신호가 흐른다

### LAN의 기본 구조

**LAN (Local Area Network) 토폴로지:**

```
버스 토폴로지 (구형):
[PC] --- [PC] --- [PC] --- [PC]
         (동축 케이블)

스타 토폴로지 (현대):
         [스위치/허브]
            /  |  \  \
           /   |   \  \
        [PC] [PC] [PC] [서버]
```

**현대 LAN 구성:**
```
인터넷
  |
[라우터]
  |
[코어 스위치]
  |
  +--- [L2 스위치] --- [PC 그룹 1]
  |
  +--- [L2 스위치] --- [PC 그룹 2]
  |
  +--- [L2 스위치] --- [서버]
```

### 허브의 동작 원리

**허브 (Hub):**
- **레이어**: 물리 계층 (Layer 1)
- **동작**: 수신한 신호를 모든 포트로 복사하여 전송 (브로드캐스트)
- **지능**: 없음 (신호 재생 및 증폭만)
- **현황**: 거의 사용되지 않음

**허브의 신호 전달:**
```
포트 1에서 신호 수신
    ↓
신호 재생 및 증폭
    ↓
포트 2, 3, 4, 5, ...로 모두 전송
```

**문제점:**
1. **충돌 도메인**: 모든 포트가 하나의 충돌 도메인
2. **대역폭 공유**: 모든 장비가 대역폭 공유
3. **보안 취약**: 모든 장비가 모든 트래픽 수신 가능

**실무 사례 - 충돌 감지:**
```bash
# 네트워크 인터페이스 통계 확인
$ ifconfig eth0
RX errors: 0  dropped: 0  overruns: 0  frame: 0
TX errors: 0  dropped: 0  overruns: 0  carrier: 0
  collisions: 15  # 충돌 횟수
```

### 리피터의 역할

**리피터 (Repeater):**
- 신호 감쇠 방지
- 신호 재생 및 증폭
- 전송 거리 연장

**케이블 최대 거리:**
| 케이블 타입 | 최대 거리 | 용도 |
|------------|----------|------|
| 10Base-T (Cat3 UTP) | 100m | 10 Mbps |
| 100Base-TX (Cat5 UTP) | 100m | 100 Mbps |
| 1000Base-T (Cat5e/6 UTP) | 100m | 1 Gbps |
| 10GBase-T (Cat6a/7 UTP) | 100m | 10 Gbps |
| 광섬유 (Single-mode) | 10km+ | 장거리 |

### 이더넷 케이블의 종류

**UTP (Unshielded Twisted Pair) 케이블:**

```
Cat5e:
  - 최대 속도: 1 Gbps
  - 대역폭: 100 MHz
  - 용도: 일반 네트워크

Cat6:
  - 최대 속도: 10 Gbps (55m까지)
  - 대역폭: 250 MHz
  - 용도: 고속 네트워크

Cat6a:
  - 최대 속도: 10 Gbps (100m)
  - 대역폭: 500 MHz
  - 용도: 데이터센터

Cat7:
  - 최대 속도: 10+ Gbps
  - 대역폭: 600 MHz
  - 차폐: 개별 차폐
```

**케이블 배선 (T568A vs T568B):**
```
T568A:
1. White/Green
2. Green
3. White/Orange
4. Blue
5. White/Blue
6. Orange
7. White/Brown
8. Brown

T568B:
1. White/Orange
2. Orange
3. White/Green
4. Blue
5. White/Blue
6. Green
7. White/Brown
8. Brown
```

**실무 사례 - 케이블 테스트:**
```bash
# ethtool: 케이블 상태 확인
$ sudo ethtool eth0
Speed: 1000Mb/s
Duplex: Full
Port: Twisted Pair
Link detected: yes

# 케이블 길이 측정 (일부 NIC 지원)
$ sudo ethtool -t eth0
```

### 전이중 모드와 반이중 모드

**반이중 (Half-Duplex):**
- 송신과 수신을 동시에 할 수 없음
- 충돌 가능 (CSMA/CD 필요)
- 허브 사용 시

**전이중 (Full-Duplex):**
- 송신과 수신을 동시에 수행
- 충돌 없음
- 스위치 사용 시

**CSMA/CD (Carrier Sense Multiple Access with Collision Detection):**
```
1. 전송 전 회선 확인 (Carrier Sense)
2. 전송 시작
3. 충돌 감지 시:
   a. Jam 신호 전송
   b. 랜덤 시간 대기 (Backoff)
   c. 재전송
```

**실무 사례 - Duplex 확인 및 설정:**
```bash
# 현재 설정 확인
$ ethtool eth0 | grep -i duplex
Duplex: Full

# Auto-negotiation 확인
$ ethtool eth0 | grep -i auto
Auto-negotiation: on

# 수동 설정 (비권장, Auto-negotiation 권장)
$ sudo ethtool -s eth0 speed 1000 duplex full autoneg off

# Auto-negotiation 재활성화
$ sudo ethtool -s eth0 autoneg on
```

---

## 2. 스위칭 허브는 주소 테이블로 중계한다

### 스위치의 기본 동작

**스위치 (Switching Hub, L2 Switch):**
- **레이어**: 데이터 링크 계층 (Layer 2)
- **기능**: MAC 주소 기반 프레임 전달
- **지능**: MAC 주소 학습 및 필터링
- **장점**: 충돌 도메인 분리, 대역폭 독립

**스위치 vs 허브:**

| 특징 | 허브 | 스위치 |
|------|------|--------|
| 계층 | Layer 1 | Layer 2 |
| 전달 방식 | 브로드캐스트 | 유니캐스트 (목적지만) |
| MAC 학습 | 불가 | 가능 |
| 충돌 도메인 | 1개 (모든 포트) | 포트당 1개 |
| 대역폭 | 공유 | 독립 |
| 보안 | 낮음 | 높음 |

### MAC 주소 테이블 (CAM Table)

**MAC 주소 학습 과정:**

```
초기 상태:
MAC Table: (비어있음)

1. PC-A (MAC: AA:AA:AA:AA:AA:AA) → 포트 1에서 프레임 도착
   MAC Table:
   AA:AA:AA:AA:AA:AA → Port 1

2. PC-B (MAC: BB:BB:BB:BB:BB:BB) → 포트 2에서 프레임 도착
   MAC Table:
   AA:AA:AA:AA:AA:AA → Port 1
   BB:BB:BB:BB:BB:BB → Port 2

3. 프레임 전달:
   - 목적지 MAC이 테이블에 있음 → 해당 포트로만 전송
   - 목적지 MAC이 테이블에 없음 → 모든 포트로 플러딩
   - 브로드캐스트 (FF:FF:FF:FF:FF:FF) → 모든 포트
```

**실무 도구 - MAC 주소 테이블 확인:**

**Cisco 스위치:**
```
# MAC 주소 테이블 확인
Switch# show mac address-table
          Mac Address Table
-------------------------------------------
Vlan    Mac Address       Type        Ports
----    -----------       --------    -----
   1    00aa.00bb.00cc    DYNAMIC     Gi0/1
   1    00dd.00ee.00ff    DYNAMIC     Gi0/2
   1    0011.2233.4455    DYNAMIC     Gi0/3

# 특정 포트의 MAC 주소
Switch# show mac address-table interface GigabitEthernet0/1

# 특정 VLAN의 MAC 주소
Switch# show mac address-table vlan 10

# MAC 주소 테이블 초기화
Switch# clear mac address-table dynamic
```

**Linux 브리지:**
```bash
# MAC 주소 테이블 확인
$ brctl showmacs br0
port no mac addr                is local?       ageing timer
  1     00:1a:2b:3c:4d:5e       no                 5.23
  2     a0:b1:c2:d3:e4:f5       no                10.45

# bridge 명령 (현대적)
$ bridge fdb show br br0
00:1a:2b:3c:4d:5e dev eth0 master br0
a0:b1:c2:d3:e4:f5 dev eth1 master br0
```

### 프레임 전달 방식

**1. Store-and-Forward:**
```
프레임 전체 수신 → 오류 검사 (FCS) → 전달
- 장점: 오류 프레임 차단, 신뢰성 높음
- 단점: 지연 시간 증가
- 사용: 대부분의 현대 스위치
```

**2. Cut-Through:**
```
목적지 MAC 주소 읽기 (처음 14바이트) → 즉시 전달
- 장점: 지연 시간 최소화
- 단점: 오류 프레임도 전달
- 사용: 초저지연 환경
```

**3. Fragment-Free:**
```
처음 64바이트 수신 → 전달
- 장점: 충돌 프레임 차단, 지연 시간 적당
- 단점: 일부 오류 프레임 통과 가능
```

### VLAN (Virtual LAN)

**VLAN의 필요성:**
1. **브로드캐스트 도메인 분리**: 불필요한 브로드캐스트 차단
2. **보안**: 네트워크 격리
3. **관리 편의성**: 논리적 그룹화
4. **성능 향상**: 트래픽 분리

**VLAN 구성:**
```
         [스위치]
            |
  +------+------+------+
  |      |      |      |
VLAN 10 VLAN 20 VLAN 30
(영업)  (개발)  (서버)
```

**IEEE 802.1Q 태그:**
```
일반 이더넷 프레임:
[Dest MAC][Src MAC][EtherType][Data][FCS]

802.1Q 태깅 프레임:
[Dest MAC][Src MAC][802.1Q Tag][EtherType][Data][FCS]
                    └─ VLAN ID (12bit, 1-4094)
```

**실무 사례 - VLAN 설정 (Cisco):**

```
# VLAN 생성
Switch(config)# vlan 10
Switch(config-vlan)# name Engineering
Switch(config-vlan)# exit

Switch(config)# vlan 20
Switch(config-vlan)# name Sales
Switch(config-vlan)# exit

# 포트를 VLAN에 할당
Switch(config)# interface gigabitEthernet 0/1
Switch(config-if)# switchport mode access
Switch(config-if)# switchport access vlan 10

# 트렁크 포트 설정 (여러 VLAN 통과)
Switch(config)# interface gigabitEthernet 0/24
Switch(config-if)# switchport mode trunk
Switch(config-if)# switchport trunk allowed vlan 10,20,30

# VLAN 확인
Switch# show vlan brief
VLAN Name                             Status    Ports
---- -------------------------------- --------- -------------------------------
1    default                          active    Gi0/5, Gi0/6, Gi0/7
10   Engineering                      active    Gi0/1, Gi0/2
20   Sales                            active    Gi0/3, Gi0/4
```

**Linux VLAN 설정:**
```bash
# VLAN 인터페이스 생성
$ sudo ip link add link eth0 name eth0.10 type vlan id 10
$ sudo ip addr add 192.168.10.1/24 dev eth0.10
$ sudo ip link set dev eth0.10 up

# VLAN 확인
$ ip -d link show eth0.10
5: eth0.10@eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500
    link/ether 00:1a:2b:3c:4d:5e brd ff:ff:ff:ff:ff:ff
    vlan protocol 802.1Q id 10 <REORDER_HDR>

# VLAN 삭제
$ sudo ip link delete eth0.10
```

### 스패닝 트리 프로토콜 (STP)

**루프 문제:**
```
[스위치 A] ←→ [스위치 B]
     ↓              ↓
     └─ [스위치 C] ─┘

브로드캐스트 프레임 → 무한 순환 → 네트워크 마비
```

**STP (Spanning Tree Protocol) 동작:**
```
1. 루트 브리지 선출 (가장 낮은 Bridge ID)
2. 각 스위치에서 루트 브리지로 가는 최적 경로 선택
3. 루프 방지를 위해 일부 포트 차단
4. 토폴로지 변경 시 재계산
```

**포트 상태:**
| 상태 | 설명 | 소요 시간 |
|------|------|----------|
| Blocking | 차단 상태, 데이터 전달 안 함 | - |
| Listening | BPDU 수신/전송만 | 15초 |
| Learning | MAC 학습 시작 | 15초 |
| Forwarding | 정상 전달 | - |
| Disabled | 관리자가 비활성화 | - |

**실무 사례 - STP 확인 (Cisco):**
```
# STP 상태 확인
Switch# show spanning-tree
VLAN0001
  Spanning tree enabled protocol ieee
  Root ID    Priority    32769
             Address     00aa.00bb.00cc
             This bridge is the root

  Bridge ID  Priority    32769
             Address     00aa.00bb.00cc

# 포트별 STP 상태
Switch# show spanning-tree interface gigabitEthernet 0/1
Vlan                Role Sts Cost      Prio.Nbr Type
------------------- ---- --- --------- -------- --------------------------------
VLAN0001            Desg FWD 4         128.1    P2p

# RSTP (Rapid STP) 활성화
Switch(config)# spanning-tree mode rapid-pvst
```

### 포트 미러링 (Port Mirroring)

**용도:**
- 네트워크 모니터링
- 트래픽 분석
- IDS/IPS 연동

**설정 예시 (Cisco SPAN):**
```
# 포트 1의 트래픽을 포트 24로 미러링
Switch(config)# monitor session 1 source interface gigabitEthernet 0/1
Switch(config)# monitor session 1 destination interface gigabitEthernet 0/24

# VLAN 전체 미러링
Switch(config)# monitor session 2 source vlan 10
Switch(config)# monitor session 2 destination interface gigabitEthernet 0/24

# 확인
Switch# show monitor session 1
```

---

## 3. 라우터의 패킷 중계 동작

### 라우터의 기본 개념

**라우터 (Router):**
- **레이어**: 네트워크 계층 (Layer 3)
- **기능**: IP 주소 기반 패킷 라우팅
- **역할**: 서로 다른 네트워크 간 연결

**라우터 vs 스위치:**

| 특징 | L2 스위치 | 라우터 (L3) |
|------|----------|------------|
| 계층 | Layer 2 | Layer 3 |
| 주소 | MAC 주소 | IP 주소 |
| 전달 기준 | MAC Table | Routing Table |
| 브로드캐스트 | 전달 | 차단 |
| 네트워크 | 동일 네트워크 | 다른 네트워크 연결 |

### 라우팅 테이블

**라우팅 테이블 구조:**
```
Destination     Gateway         Genmask         Flags  Iface
0.0.0.0         192.168.1.1     0.0.0.0         UG     eth0
192.168.1.0     0.0.0.0         255.255.255.0   U      eth0
10.0.0.0        192.168.1.254   255.0.0.0       UG     eth0
```

**필드 설명:**
- **Destination**: 목적지 네트워크
- **Gateway**: 다음 홉 라우터 (0.0.0.0은 직접 연결)
- **Genmask**: 서브넷 마스크
- **Flags**:
  - U: Up (활성)
  - G: Gateway (게이트웨이 사용)
  - H: Host (호스트 라우트)

**실무 도구 - 라우팅 테이블 확인:**

```bash
# Linux
$ ip route show
default via 192.168.1.1 dev eth0 proto dhcp metric 100
192.168.1.0/24 dev eth0 proto kernel scope link src 192.168.1.100
10.0.0.0/8 via 192.168.1.254 dev eth0

# 또는
$ route -n
$ netstat -rn

# macOS
$ netstat -rn
$ route -n get default

# Windows
> route print
> netstat -rn
```

### 라우팅 프로세스

**패킷 라우팅 단계:**

```
1. 패킷 수신
   ↓
2. 목적지 IP 주소 추출
   ↓
3. 라우팅 테이블 검색 (최장 일치 원칙)
   ↓
4. 다음 홉 결정
   ↓
5. TTL 감소 (0이면 폐기)
   ↓
6. 새로운 이더넷 프레임 생성
   ↓
7. ARP로 다음 홉 MAC 주소 조회
   ↓
8. 패킷 전달
```

**최장 일치 원칙 (Longest Prefix Match):**

```
목적지 IP: 192.168.10.100

라우팅 테이블:
1. 0.0.0.0/0           (default)
2. 192.168.0.0/16      (매치)
3. 192.168.10.0/24     (매치, 더 구체적)
4. 192.168.10.100/32   (매치, 가장 구체적) ← 선택

→ 가장 긴 프리픽스 매치 선택
```

**실무 사례 - 정적 라우트 추가:**

```bash
# Linux - 특정 네트워크로 가는 경로 추가
$ sudo ip route add 10.0.0.0/8 via 192.168.1.254

# 호스트 라우트
$ sudo ip route add 8.8.8.8 via 192.168.1.1

# 기본 게이트웨이 변경
$ sudo ip route del default
$ sudo ip route add default via 192.168.1.1

# 영구 설정 (Ubuntu/Debian - /etc/network/interfaces)
auto eth0
iface eth0 inet static
    address 192.168.1.100
    netmask 255.255.255.0
    gateway 192.168.1.1
    up ip route add 10.0.0.0/8 via 192.168.1.254

# 영구 설정 (RHEL/CentOS - /etc/sysconfig/network-scripts/route-eth0)
10.0.0.0/8 via 192.168.1.254
```

**Cisco 라우터:**
```
# 정적 라우트 추가
Router(config)# ip route 10.0.0.0 255.0.0.0 192.168.1.254

# 기본 경로
Router(config)# ip route 0.0.0.0 0.0.0.0 192.168.1.1

# 라우팅 테이블 확인
Router# show ip route
Codes: C - connected, S - static, R - RIP, O - OSPF

Gateway of last resort is 192.168.1.1 to network 0.0.0.0

S*   0.0.0.0/0 [1/0] via 192.168.1.1
C    192.168.1.0/24 is directly connected, GigabitEthernet0/0
S    10.0.0.0/8 [1/0] via 192.168.1.254
```

### 동적 라우팅 프로토콜

**라우팅 프로토콜 분류:**

**1. IGP (Interior Gateway Protocol) - 내부 라우팅:**
- **RIP (Routing Information Protocol)**
  - Distance Vector
  - 메트릭: Hop Count (최대 15)
  - 간단하지만 느림

- **OSPF (Open Shortest Path First)**
  - Link State
  - 메트릭: Cost (대역폭 기반)
  - 빠른 수렴, 확장성 우수

- **EIGRP (Enhanced Interior Gateway Routing Protocol)**
  - Hybrid (Cisco 독점)
  - 메트릭: 대역폭, 지연, 부하 등
  - 빠른 수렴

**2. EGP (Exterior Gateway Protocol) - 외부 라우팅:**
- **BGP (Border Gateway Protocol)**
  - Path Vector
  - 인터넷 백본 라우팅
  - AS (Autonomous System) 간 라우팅

**라우팅 프로토콜 비교:**

| 프로토콜 | 타입 | 메트릭 | 수렴 속도 | 확장성 | 사용처 |
|---------|------|--------|----------|--------|--------|
| RIP | Distance Vector | Hop Count | 느림 | 낮음 | 소규모 네트워크 |
| OSPF | Link State | Cost | 빠름 | 높음 | 대규모 기업 |
| EIGRP | Hybrid | Composite | 매우 빠름 | 높음 | Cisco 환경 |
| BGP | Path Vector | AS Path | 느림 | 매우 높음 | ISP, 인터넷 백본 |

**실무 사례 - OSPF 설정 (Cisco):**

```
# OSPF 활성화
Router(config)# router ospf 1
Router(config-router)# network 192.168.1.0 0.0.0.255 area 0
Router(config-router)# network 10.0.0.0 0.255.255.255 area 1

# 라우터 ID 설정
Router(config-router)# router-id 1.1.1.1

# OSPF 확인
Router# show ip ospf neighbor
Neighbor ID     Pri   State           Dead Time   Address         Interface
2.2.2.2           1   FULL/DR         00:00:35    192.168.1.2     Gi0/0

Router# show ip ospf database
Router# show ip route ospf
```

**Linux - FRRouting (Quagga 후속):**
```bash
# FRRouting 설치
$ sudo apt install frr

# OSPF 설정 (/etc/frr/frr.conf)
router ospf
 network 192.168.1.0/24 area 0
 network 10.0.0.0/8 area 1

# OSPF 상태 확인
$ sudo vtysh -c "show ip ospf neighbor"
$ sudo vtysh -c "show ip route ospf"
```

### 서브넷과 서브넷 마스크

**서브넷팅의 목적:**
1. IP 주소 효율적 사용
2. 브로드캐스트 도메인 축소
3. 보안 강화
4. 네트워크 관리 편의성

**CIDR (Classless Inter-Domain Routing) 표기법:**
```
192.168.1.0/24
           └─ 프리픽스 길이 (24비트가 네트워크 부분)

서브넷 마스크: 255.255.255.0
네트워크 주소: 192.168.1.0
브로드캐스트: 192.168.1.255
사용 가능 호스트: 192.168.1.1 ~ 192.168.1.254 (254개)
```

**서브넷팅 예시:**

```
원본 네트워크: 192.168.1.0/24 (256 주소)

4개 서브넷으로 분할 (/26):
1. 192.168.1.0/26    (192.168.1.0   ~ 192.168.1.63)   64개
2. 192.168.1.64/26   (192.168.1.64  ~ 192.168.1.127)  64개
3. 192.168.1.128/26  (192.168.1.128 ~ 192.168.1.191)  64개
4. 192.168.1.192/26  (192.168.1.192 ~ 192.168.1.255)  64개
```

**VLSM (Variable Length Subnet Mask) 예시:**
```
192.168.1.0/24 네트워크를 가변 크기로 분할:

서버 서브넷 (60 호스트 필요):
  192.168.1.0/26   (62 호스트)

사무실 A (30 호스트 필요):
  192.168.1.64/27  (30 호스트)

사무실 B (30 호스트 필요):
  192.168.1.96/27  (30 호스트)

포인트-투-포인트 링크 (2 호스트):
  192.168.1.128/30 (2 호스트)
  192.168.1.132/30 (2 호스트)
```

**실무 도구 - 서브넷 계산:**

```bash
# ipcalc
$ ipcalc 192.168.1.0/24
Address:   192.168.1.0          11000000.10101000.00000001. 00000000
Netmask:   255.255.255.0 = 24   11111111.11111111.11111111. 00000000
Wildcard:  0.0.0.255            00000000.00000000.00000000. 11111111
=>
Network:   192.168.1.0/24       11000000.10101000.00000001. 00000000
HostMin:   192.168.1.1          11000000.10101000.00000001. 00000001
HostMax:   192.168.1.254        11000000.10101000.00000001. 11111110
Broadcast: 192.168.1.255        11000000.10101000.00000001. 11111111
Hosts/Net: 254

# Python 계산
$ python3 << 'EOF'
import ipaddress

network = ipaddress.ip_network('192.168.1.0/24')
print(f"Network: {network}")
print(f"Netmask: {network.netmask}")
print(f"Broadcast: {network.broadcast_address}")
print(f"Num hosts: {network.num_addresses - 2}")
print(f"First host: {network.network_address + 1}")
print(f"Last host: {network.broadcast_address - 1}")
EOF
```

---

## 4. 라우터의 부가 기능

### NAT (Network Address Translation)

**NAT의 필요성:**
1. **IPv4 주소 부족 문제 해결**
2. **사설 IP 사용 가능**
3. **보안 (내부 네트워크 숨김)**

**사설 IP 대역 (RFC 1918):**
```
10.0.0.0/8        (10.0.0.0 ~ 10.255.255.255)      16,777,216개
172.16.0.0/12     (172.16.0.0 ~ 172.31.255.255)    1,048,576개
192.168.0.0/16    (192.168.0.0 ~ 192.168.255.255)  65,536개
```

**NAT 유형:**

**1. Static NAT (1:1 매핑):**
```
내부 IP: 192.168.1.10 ←→ 공인 IP: 203.0.113.10
내부 IP: 192.168.1.20 ←→ 공인 IP: 203.0.113.20
```

**2. Dynamic NAT (풀 사용):**
```
내부 IP: 192.168.1.x → 공인 IP 풀 (203.0.113.10 ~ 203.0.113.20)
```

**3. PAT (Port Address Translation) / NAT Overload (다:1):**
```
192.168.1.10:54321 → 203.0.113.1:10001
192.168.1.20:54322 → 203.0.113.1:10002
192.168.1.30:54323 → 203.0.113.1:10003
         └─ 내부 IP:포트 → 공인 IP:포트 변환
```

**NAT 동작 과정 (PAT):**
```
내부 → 외부:
1. 내부 호스트 (192.168.1.10:54321) → 외부 서버 (8.8.8.8:53) 요청
2. 라우터가 변환:
   Source: 192.168.1.10:54321 → 203.0.113.1:10001
3. NAT 테이블 기록:
   192.168.1.10:54321 ↔ 203.0.113.1:10001 → 8.8.8.8:53
4. 외부 전송: Source 203.0.113.1:10001 → Dest 8.8.8.8:53

외부 → 내부:
1. 응답 도착: Source 8.8.8.8:53 → Dest 203.0.113.1:10001
2. NAT 테이블 조회
3. 역변환: Dest 203.0.113.1:10001 → 192.168.1.10:54321
4. 내부 전달
```

**실무 사례 - NAT 설정:**

**Linux (iptables):**
```bash
# SNAT (Source NAT) - 내부 → 외부
$ sudo iptables -t nat -A POSTROUTING -s 192.168.1.0/24 -o eth0 -j SNAT --to-source 203.0.113.1

# MASQUERADE (동적 공인 IP)
$ sudo iptables -t nat -A POSTROUTING -s 192.168.1.0/24 -o eth0 -j MASQUERADE

# DNAT (Destination NAT) - 포트 포워딩
$ sudo iptables -t nat -A PREROUTING -p tcp --dport 80 -j DNAT --to-destination 192.168.1.10:80

# IP 포워딩 활성화
$ sudo sysctl -w net.ipv4.ip_forward=1
$ echo "net.ipv4.ip_forward=1" | sudo tee -a /etc/sysctl.conf

# NAT 테이블 확인
$ sudo iptables -t nat -L -n -v
```

**Cisco 라우터:**
```
# NAT 내부/외부 인터페이스 지정
Router(config)# interface gigabitEthernet 0/0
Router(config-if)# ip nat inside

Router(config)# interface gigabitEthernet 0/1
Router(config-if)# ip nat outside

# PAT (NAT Overload) 설정
Router(config)# access-list 1 permit 192.168.1.0 0.0.0.255
Router(config)# ip nat inside source list 1 interface gigabitEthernet 0/1 overload

# 정적 NAT
Router(config)# ip nat inside source static 192.168.1.10 203.0.113.10

# 포트 포워딩
Router(config)# ip nat inside source static tcp 192.168.1.10 80 203.0.113.1 80

# NAT 확인
Router# show ip nat translations
Router# show ip nat statistics
```

### 포트 포워딩

**용도:**
- 외부에서 내부 서버 접근 허용
- 웹 서버, 게임 서버, SSH 등

**설정 예시:**
```bash
# Linux iptables
# 외부 80 포트 → 내부 192.168.1.10:8080
$ sudo iptables -t nat -A PREROUTING -p tcp --dport 80 -j DNAT --to-destination 192.168.1.10:8080
$ sudo iptables -A FORWARD -p tcp -d 192.168.1.10 --dport 8080 -j ACCEPT

# 여러 포트 포워딩
$ sudo iptables -t nat -A PREROUTING -p tcp --dport 22 -j DNAT --to-destination 192.168.1.10:22
$ sudo iptables -t nat -A PREROUTING -p tcp --dport 443 -j DNAT --to-destination 192.168.1.20:443
```

**NAT 문제점:**
1. **P2P 통신 어려움** → STUN/TURN 서버 필요
2. **프로토콜 제한** (FTP, SIP 등) → ALG (Application Level Gateway) 필요
3. **End-to-End 원칙 위배**

### 패킷 필터링 (Firewall)

**방화벽 유형:**
1. **Stateless Firewall**: 각 패킷 독립적 검사
2. **Stateful Firewall**: 연결 상태 추적

**실무 사례 - iptables 방화벽:**

```bash
# 기본 정책: 모든 입력 차단
$ sudo iptables -P INPUT DROP
$ sudo iptables -P FORWARD DROP
$ sudo iptables -P OUTPUT ACCEPT

# Loopback 허용
$ sudo iptables -A INPUT -i lo -j ACCEPT

# 기존 연결 허용 (Stateful)
$ sudo iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT

# SSH 허용 (특정 IP만)
$ sudo iptables -A INPUT -p tcp --dport 22 -s 203.0.113.0/24 -j ACCEPT

# HTTP/HTTPS 허용
$ sudo iptables -A INPUT -p tcp --dport 80 -j ACCEPT
$ sudo iptables -A INPUT -p tcp --dport 443 -j ACCEPT

# ICMP (ping) 허용
$ sudo iptables -A INPUT -p icmp --icmp-type echo-request -j ACCEPT

# Rate Limiting (DDoS 방어)
$ sudo iptables -A INPUT -p tcp --dport 80 -m limit --limit 25/minute --limit-burst 100 -j ACCEPT

# 특정 IP 차단
$ sudo iptables -A INPUT -s 198.51.100.10 -j DROP

# 로깅
$ sudo iptables -A INPUT -j LOG --log-prefix "IPTables-Dropped: "

# 규칙 저장
$ sudo iptables-save | sudo tee /etc/iptables/rules.v4
```

**nftables (현대적 방화벽):**
```bash
# nftables 설정
$ sudo nft add table inet filter
$ sudo nft add chain inet filter input { type filter hook input priority 0 \; policy drop \; }
$ sudo nft add rule inet filter input ct state established,related accept
$ sudo nft add rule inet filter input tcp dport 22 accept
$ sudo nft add rule inet filter input tcp dport 80 accept
$ sudo nft add rule inet filter input tcp dport 443 accept

# 규칙 확인
$ sudo nft list ruleset
```

### QoS (Quality of Service)

**QoS의 필요성:**
- 중요한 트래픽 우선 처리
- 대역폭 보장
- 지연 시간 최소화

**QoS 메커니즘:**
1. **분류 (Classification)**: 트래픽 식별
2. **마킹 (Marking)**: DSCP, CoS 값 설정
3. **큐잉 (Queuing)**: 우선순위 큐
4. **셰이핑 (Shaping)**: 대역폭 제한
5. **폴리싱 (Policing)**: 초과 트래픽 차단

**DSCP 값:**
```
EF (Expedited Forwarding): 46    - VoIP
AF41 (Assured Forwarding): 34    - 비디오
AF31: 26                          - 스트리밍
AF21: 18                          - 이메일
BE (Best Effort): 0               - 일반 트래픽
```

**실무 사례 - Linux tc (Traffic Control):**
```bash
# HTB (Hierarchical Token Bucket) QoS 설정
$ sudo tc qdisc add dev eth0 root handle 1: htb default 30

# 전체 대역폭 100Mbps
$ sudo tc class add dev eth0 parent 1: classid 1:1 htb rate 100mbit

# VoIP (최우선, 20Mbps 보장)
$ sudo tc class add dev eth0 parent 1:1 classid 1:10 htb rate 20mbit ceil 30mbit prio 1

# 비디오 (30Mbps 보장)
$ sudo tc class add dev eth0 parent 1:1 classid 1:20 htb rate 30mbit ceil 50mbit prio 2

# 일반 트래픽 (나머지)
$ sudo tc class add dev eth0 parent 1:1 classid 1:30 htb rate 50mbit ceil 100mbit prio 3

# 필터 설정 (포트 기반)
$ sudo tc filter add dev eth0 protocol ip parent 1:0 prio 1 u32 match ip dport 5060 0xffff flowid 1:10  # SIP
$ sudo tc filter add dev eth0 protocol ip parent 1:0 prio 2 u32 match ip dport 1935 0xffff flowid 1:20  # RTMP

# 확인
$ sudo tc -s qdisc show dev eth0
$ sudo tc -s class show dev eth0
```

---

## 실무 팁

### 1. 네트워크 토폴로지 설계

**베스트 프랙티스:**
```
인터넷
  |
[방화벽]
  |
[코어 라우터]
  |
  +--- [DMZ 스위치] --- [웹 서버]
  |                  +-- [메일 서버]
  |
  +--- [내부 스위치] --- [VLAN 10: 영업]
                     +-- [VLAN 20: 개발]
                     +-- [VLAN 30: 관리]
```

### 2. 네트워크 모니터링

```bash
# 인터페이스 통계
$ ifstat -t -i eth0
$ iftop -i eth0

# 대역폭 모니터링
$ vnstat -i eth0
$ bmon -p eth0

# 실시간 트래픽
$ nload eth0
```

### 3. 네트워크 문제 해결

```bash
# 연결성 확인
$ ping -c 5 8.8.8.8

# 경로 추적
$ traceroute www.example.com
$ mtr www.example.com

# DNS 확인
$ dig www.example.com
$ nslookup www.example.com

# 포트 확인
$ telnet www.example.com 80
$ nc -zv www.example.com 80

# 패킷 캡처
$ sudo tcpdump -i eth0 -n host 192.168.1.10
```

### 4. 보안 강화

```bash
# MAC 주소 필터링 (스위치)
# Cisco:
Switch(config-if)# switchport port-security
Switch(config-if)# switchport port-security maximum 2
Switch(config-if)# switchport port-security violation restrict
Switch(config-if)# switchport port-security mac-address sticky

# DHCP Snooping
Switch(config)# ip dhcp snooping
Switch(config)# ip dhcp snooping vlan 10

# Dynamic ARP Inspection
Switch(config)# ip arp inspection vlan 10
```

---

*마지막 업데이트: 2026년 1월*
