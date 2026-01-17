# AWS 네트워킹 면접 질문

VPC 피어링, Security Group vs NACL 등 AWS 네트워킹 핵심 질문을 정리합니다.

## 목차

1. [VPC 기초](#1-vpc-기초)
2. [VPC 피어링](#2-vpc-피어링)
3. [Security Group vs NACL](#3-security-group-vs-nacl)
4. [NAT Gateway vs NAT Instance](#4-nat-gateway-vs-nat-instance)
5. [VPC Endpoint](#5-vpc-endpoint)
6. [하이브리드 연결](#6-하이브리드-연결)

---

## 1. VPC 기초

### VPC 구성 요소

```
┌─────────────────────────────────────────────────────────────┐
│  VPC (10.0.0.0/16)                                          │
│                                                             │
│  ┌─────────────────────┐  ┌─────────────────────┐          │
│  │ Public Subnet       │  │ Private Subnet      │          │
│  │ 10.0.1.0/24 (AZ-a) │  │ 10.0.2.0/24 (AZ-a) │          │
│  │                     │  │                     │          │
│  │ ┌─────────────────┐ │  │ ┌─────────────────┐ │          │
│  │ │  Web Server     │ │  │ │  App Server     │ │          │
│  │ │  (Public IP)    │ │  │ │  (Private IP)   │ │          │
│  │ └─────────────────┘ │  │ └─────────────────┘ │          │
│  └──────────┬──────────┘  └──────────┬──────────┘          │
│             │                         │                     │
│  ┌──────────▼──────────┐  ┌──────────▼──────────┐          │
│  │ Route Table         │  │ Route Table         │          │
│  │ 0.0.0.0/0 → IGW    │  │ 0.0.0.0/0 → NAT GW │          │
│  └─────────────────────┘  └─────────────────────┘          │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                   Internet Gateway                   │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### CIDR 설계

```
/16: 65,536 IP (VPC 권장)
/24: 256 IP (서브넷 일반)
/28: 16 IP (최소 서브넷)

예시:
VPC: 10.0.0.0/16
├── Public Subnet AZ-a: 10.0.1.0/24
├── Public Subnet AZ-c: 10.0.2.0/24
├── Private Subnet AZ-a: 10.0.10.0/24
├── Private Subnet AZ-c: 10.0.20.0/24
└── DB Subnet: 10.0.100.0/24
```

---

## 2. VPC 피어링

### 개념

```
VPC 피어링: 두 VPC 간 프라이빗 연결

┌──────────────┐              ┌──────────────┐
│    VPC A     │──── Peer ────│    VPC B     │
│ 10.0.0.0/16  │   Connection │ 10.1.0.0/16  │
└──────────────┘              └──────────────┘

특징:
- 프라이빗 IP로 통신
- 다른 리전, 다른 계정 가능
- 대역폭 제한 없음
- 추가 하드웨어 불필요
```

### 제한 사항

```
1. 전이적 라우팅 불가
   A ↔ B ↔ C일 때
   A → B → C 불가능 (A-C 별도 피어링 필요)

2. CIDR 중복 불가
   VPC A: 10.0.0.0/16
   VPC B: 10.0.0.0/16 → 피어링 불가

3. 피어링당 하나의 연결만
   동일 VPC 쌍에 여러 피어링 불가
```

### 라우팅 설정

```
VPC A (10.0.0.0/16) ↔ VPC B (10.1.0.0/16)

VPC A 라우트 테이블:
| Destination  | Target      |
|-------------|-------------|
| 10.0.0.0/16 | local       |
| 10.1.0.0/16 | pcx-xxxxx   |  ← 피어링 연결

VPC B 라우트 테이블:
| Destination  | Target      |
|-------------|-------------|
| 10.1.0.0/16 | local       |
| 10.0.0.0/16 | pcx-xxxxx   |
```

### Transit Gateway vs VPC Peering

```
VPC Peering:
- 1:1 연결
- 전이적 라우팅 X
- 간단한 구성
- 비용: 무료 (데이터 전송만)

Transit Gateway:
- Hub-and-Spoke
- 전이적 라우팅 O
- 복잡한 네트워크에 적합
- 비용: 연결당 + 데이터
```

---

## 3. Security Group vs NACL

### 비교표

| 특성 | Security Group | NACL |
|------|----------------|------|
| 레벨 | 인스턴스 | 서브넷 |
| 상태 | Stateful | Stateless |
| 규칙 | 허용만 | 허용 + 거부 |
| 평가 | 모든 규칙 | 번호 순서 |
| 기본 | 모두 거부 | 모두 허용 |

### Stateful vs Stateless

```
Stateful (Security Group):
인바운드 허용 → 아웃바운드 응답 자동 허용

Request:  Client → EC2 (인바운드 허용됨)
Response: EC2 → Client (자동 허용, 규칙 불필요)

Stateless (NACL):
인바운드/아웃바운드 각각 규칙 필요

인바운드: 80 포트 허용
아웃바운드: Ephemeral 포트(1024-65535) 허용 필요!
```

### Security Group 예시

```
인바운드:
| Type  | Protocol | Port  | Source          |
|-------|----------|-------|-----------------|
| HTTP  | TCP      | 80    | 0.0.0.0/0       |
| HTTPS | TCP      | 443   | 0.0.0.0/0       |
| SSH   | TCP      | 22    | 10.0.0.0/8      |

아웃바운드:
| Type  | Protocol | Port  | Destination     |
|-------|----------|-------|-----------------|
| All   | All      | All   | 0.0.0.0/0       |

* Stateful이므로 응답 트래픽 자동 허용
```

### NACL 예시

```
인바운드:
| Rule# | Type  | Protocol | Port  | Source     | Action |
|-------|-------|----------|-------|------------|--------|
| 100   | HTTP  | TCP      | 80    | 0.0.0.0/0  | ALLOW  |
| 110   | HTTPS | TCP      | 443   | 0.0.0.0/0  | ALLOW  |
| 120   | SSH   | TCP      | 22    | 10.0.0.0/8 | ALLOW  |
| *     | All   | All      | All   | 0.0.0.0/0  | DENY   |

아웃바운드:
| Rule# | Type      | Protocol | Port       | Dest      | Action |
|-------|-----------|----------|------------|-----------|--------|
| 100   | HTTP      | TCP      | 80         | 0.0.0.0/0 | ALLOW  |
| 110   | HTTPS     | TCP      | 443        | 0.0.0.0/0 | ALLOW  |
| 120   | Ephemeral | TCP      | 1024-65535 | 0.0.0.0/0 | ALLOW  |
| *     | All       | All      | All        | 0.0.0.0/0 | DENY   |

* Stateless이므로 응답 포트(Ephemeral) 명시 필요
```

### 사용 가이드

```
Security Group:
- 주요 방화벽으로 사용
- 인스턴스 단위 제어
- 동적 참조 가능 (다른 SG ID)

NACL:
- 추가 방어층
- 서브넷 단위 차단
- 특정 IP 차단에 유용
```

---

## 4. NAT Gateway vs NAT Instance

### NAT Gateway

```
관리형 NAT 서비스

특징:
- 완전 관리형
- 고가용성 (AZ 내)
- 최대 45 Gbps
- 자동 확장

비용:
- 시간당 요금
- 데이터 처리 요금
```

### NAT Instance

```
EC2 기반 NAT

특징:
- 사용자 관리
- 스크립트로 HA 구성 필요
- 인스턴스 크기에 따른 대역폭
- Bastion Host 겸용 가능

비용:
- EC2 인스턴스 비용
- 저렴할 수 있음
```

### 비교

| 특성 | NAT Gateway | NAT Instance |
|------|-------------|--------------|
| 관리 | AWS | 사용자 |
| 가용성 | AZ 내 이중화 | 직접 구성 |
| 대역폭 | 45 Gbps | 인스턴스 의존 |
| 비용 | 높음 | 낮음 (소규모) |
| 보안그룹 | 연결 불가 | 연결 가능 |
| Bastion | 불가 | 가능 |

---

## 5. VPC Endpoint

### 유형

```
Gateway Endpoint:
- S3, DynamoDB 전용
- 무료
- 라우트 테이블에 추가

Interface Endpoint (PrivateLink):
- 대부분의 AWS 서비스
- ENI 생성
- 프라이빗 IP 부여
- 시간당 + 데이터 요금
```

### Gateway Endpoint 설정

```
VPC → S3 (퍼블릭 접근 없이)

라우트 테이블:
| Destination      | Target    |
|------------------|-----------|
| 10.0.0.0/16      | local     |
| pl-xxxxxxxx(S3)  | vpce-xxx  |  ← Gateway Endpoint

정책 예시:
{
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": "*",
            "Action": "s3:*",
            "Resource": "arn:aws:s3:::my-bucket/*"
        }
    ]
}
```

### Interface Endpoint 설정

```
VPC → SQS (프라이빗 연결)

엔드포인트 생성:
- 서비스: com.amazonaws.ap-northeast-2.sqs
- VPC: 선택
- 서브넷: 프라이빗 서브넷
- 보안 그룹: 443 인바운드 허용

DNS:
- vpce-xxx.sqs.ap-northeast-2.vpce.amazonaws.com
- 프라이빗 DNS 활성화 시 기존 엔드포인트 주소 사용 가능
```

---

## 6. 하이브리드 연결

### Site-to-Site VPN

```
온프레미스 ↔ AWS (인터넷 경유, 암호화)

구성 요소:
- Customer Gateway: 온프레미스 라우터
- Virtual Private Gateway: VPC 측 게이트웨이
- VPN Connection: IPsec 터널

특징:
- 빠른 구축 (시간 단위)
- 인터넷 경유
- 대역폭: 최대 1.25 Gbps
- 비용: 저렴
```

### Direct Connect

```
온프레미스 ↔ AWS (전용선)

구성:
- Direct Connect Location에서 연결
- 1 Gbps 또는 10 Gbps 포트

특징:
- 일관된 네트워크 성능
- 인터넷 미경유
- 구축 시간: 수 주
- 비용: 높음

HA 구성:
- 이중화 연결 권장
- VPN 백업 고려
```

### 비교

| 특성 | VPN | Direct Connect |
|------|-----|----------------|
| 구축 시간 | 분~시간 | 주~월 |
| 경로 | 인터넷 | 전용선 |
| 암호화 | O (IPsec) | 선택 |
| 대역폭 | ~1.25 Gbps | 1/10 Gbps |
| 지연 시간 | 변동적 | 일관적 |
| 비용 | 낮음 | 높음 |

---

## 핵심 정리

| 주제 | 핵심 포인트 |
|------|------------|
| VPC 피어링 | 전이적 라우팅 X, CIDR 중복 X |
| SG vs NACL | Stateful vs Stateless |
| NAT Gateway | 관리형, 고가용성, 비쌈 |
| VPC Endpoint | Gateway(S3/DDB), Interface(기타) |
| VPN vs DX | 빠른 구축 vs 일관된 성능 |

---

*마지막 업데이트: 2025년 01월*
