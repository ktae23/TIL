# 클라우드 아키텍처 면접 핵심 질문 정리

5년차 백엔드 개발자 면접에서 자주 등장하는 클라우드 아키텍처 핵심 질문과 답변을 정리합니다.

## 목차

1. [고가용성 (High Availability)](#1-고가용성-high-availability)
2. [Auto Scaling](#2-auto-scaling)
3. [RTO와 RPO](#3-rto와-rpo)
4. [VPC와 네트워크](#4-vpc와-네트워크)
5. [AWS 핵심 서비스](#5-aws-핵심-서비스)
6. [클라우드 설계 원칙](#6-클라우드-설계-원칙)

---

## 1. 고가용성 (High Availability)

### Q: 고가용성 아키텍처를 어떻게 설계하나요?

**가용성 목표 (SLA)**
```
┌─────────────────────────────────────────────────────────────┐
│  가용성        │  연간 다운타임      │  월간 다운타임        │
├─────────────────────────────────────────────────────────────┤
│  99% (Two 9s)  │  3.65일            │  7.3시간              │
│  99.9%         │  8.76시간          │  43.8분               │
│  99.99%        │  52.6분            │  4.38분               │
│  99.999%       │  5.26분            │  26초                 │
└─────────────────────────────────────────────────────────────┘
```

**Multi-AZ 아키텍처**
```
┌─────────────────────────────────────────────────────────────┐
│                        Region (ap-northeast-2)              │
│  ┌─────────────────────┐    ┌─────────────────────┐        │
│  │   AZ-a              │    │   AZ-c              │        │
│  │  ┌─────────────┐    │    │  ┌─────────────┐    │        │
│  │  │ Web Server  │    │    │  │ Web Server  │    │        │
│  │  └─────────────┘    │    │  └─────────────┘    │        │
│  │  ┌─────────────┐    │    │  ┌─────────────┐    │        │
│  │  │ App Server  │    │    │  │ App Server  │    │        │
│  │  └─────────────┘    │    │  └─────────────┘    │        │
│  │  ┌─────────────┐    │    │  ┌─────────────┐    │        │
│  │  │ RDS Primary │←───┼────┼──│ RDS Standby │    │        │
│  │  └─────────────┘    │    │  └─────────────┘    │        │
│  └─────────────────────┘    └─────────────────────┘        │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Application Load Balancer               │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────┴─────────┐
                    │     Route 53      │
                    │   (Health Check)  │
                    └───────────────────┘
```

**주요 HA 패턴**

| 패턴 | 설명 | AWS 서비스 |
|------|------|-----------|
| Active-Active | 모든 인스턴스가 트래픽 처리 | ALB + ASG |
| Active-Passive | 장애 시 대기 인스턴스 활성화 | RDS Multi-AZ |
| Hot Standby | 대기 인스턴스도 실행 중 | Aurora Replica |
| Pilot Light | 최소 환경만 유지, 필요시 확장 | EC2 + AMI |

```yaml
# CloudFormation 예시: Multi-AZ RDS
Resources:
  MyDB:
    Type: AWS::RDS::DBInstance
    Properties:
      DBInstanceClass: db.r5.large
      Engine: mysql
      MultiAZ: true  # Multi-AZ 활성화
      StorageType: gp3
      AllocatedStorage: 100
```

---

## 2. Auto Scaling

### Q: Auto Scaling 전략과 설정 방법을 설명해주세요.

**Auto Scaling 구성 요소**
```
┌─────────────────────────────────────────────────────────────┐
│  Auto Scaling Group (ASG)                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Launch Template                                     │   │
│  │  - AMI ID                                            │   │
│  │  - Instance Type                                     │   │
│  │  - Security Group                                    │   │
│  │  - User Data (부팅 스크립트)                         │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  Scaling Policy                                             │
│  ├── Target Tracking (CPU 70% 유지)                        │
│  ├── Step Scaling (단계별 조정)                            │
│  └── Scheduled Scaling (시간대별)                          │
│                                                             │
│  Capacity Settings                                          │
│  ├── Min: 2                                                │
│  ├── Max: 10                                               │
│  └── Desired: 4                                            │
└─────────────────────────────────────────────────────────────┘
```

**Scaling 정책 종류**

| 정책 | 사용 시점 | 예시 |
|------|----------|------|
| Target Tracking | 지표를 특정 값으로 유지 | CPU 70% 유지 |
| Step Scaling | 지표 범위별 다른 조정 | 70%→+1, 90%→+3 |
| Simple Scaling | 단순 증감 | 조건 만족시 +1 |
| Scheduled | 예측 가능한 패턴 | 매일 9시 확장 |
| Predictive | ML 기반 예측 | 패턴 학습 후 선제 확장 |

```yaml
# Target Tracking Policy 예시
Resources:
  CPUPolicy:
    Type: AWS::AutoScaling::ScalingPolicy
    Properties:
      AutoScalingGroupName: !Ref MyASG
      PolicyType: TargetTrackingScaling
      TargetTrackingConfiguration:
        PredefinedMetricSpecification:
          PredefinedMetricType: ASGAverageCPUUtilization
        TargetValue: 70.0
        ScaleInCooldown: 300   # 축소 대기 시간
        ScaleOutCooldown: 60   # 확장 대기 시간
```

**Scaling 시 고려사항**
```
1. Warm-up 시간
   - 인스턴스 시작 후 트래픽 받을 준비 시간
   - Health Check 통과까지 대기

2. Cooldown 기간
   - 연속 Scaling 방지
   - Scale-out: 짧게 (빠른 대응)
   - Scale-in: 길게 (안정성)

3. Termination Policy
   - OldestInstance: 오래된 것부터
   - NewestInstance: 최신 것부터
   - OldestLaunchConfiguration: 구 설정부터
```

---

## 3. RTO와 RPO

### Q: RTO와 RPO를 설명하고 DR 전략을 어떻게 세우나요?

**정의**
```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  ←────────── RPO ──────────→ │ ←──────── RTO ─────────→    │
│                               │                             │
│  마지막 백업 ───────────── 장애발생 ───────────── 복구완료  │
│                               │                             │
│  RPO (Recovery Point Objective)                             │
│  = 허용 가능한 데이터 손실 기간                             │
│  = "얼마나 자주 백업해야 하나?"                             │
│                                                             │
│  RTO (Recovery Time Objective)                              │
│  = 허용 가능한 서비스 중단 시간                             │
│  = "얼마나 빨리 복구해야 하나?"                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**DR 전략별 비교**

| 전략 | RTO | RPO | 비용 | 설명 |
|------|-----|-----|------|------|
| Backup & Restore | 24시간+ | 24시간 | $ | S3 백업, 필요시 복원 |
| Pilot Light | 1-4시간 | 분 단위 | $$ | 핵심만 실행, 장애시 확장 |
| Warm Standby | 분 단위 | 분 단위 | $$$ | 축소된 환경 상시 운영 |
| Multi-Site Active | 초 단위 | 초 단위 | $$$$ | 동일 환경 복제 운영 |

**Pilot Light 예시**
```
┌─────────────────────────────────────────────────────────────┐
│  Primary Region (ap-northeast-2)                            │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                  │
│  │ Web (4)  │  │ App (4)  │  │ RDS      │───→ 복제        │
│  └──────────┘  └──────────┘  └──────────┘      │           │
└───────────────────────────────────────────────────────────┘
                                                   │
                                                   ↓
┌─────────────────────────────────────────────────────────────┐
│  DR Region (ap-southeast-1)                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                  │
│  │ Web (0)  │  │ App (0)  │  │ RDS      │  ← Read Replica  │
│  │ AMI 준비 │  │ AMI 준비 │  │ (Standby)│                  │
│  └──────────┘  └──────────┘  └──────────┘                  │
│                                                             │
│  장애 발생 시:                                              │
│  1. RDS 승격 (Primary로)                                   │
│  2. ASG Min/Desired 값 조정                                │
│  3. Route 53 페일오버                                      │
└─────────────────────────────────────────────────────────────┘
```

```bash
# Route 53 Health Check + Failover
# Primary 장애 시 자동으로 DR Region으로 전환
aws route53 create-health-check \
  --caller-reference $(date +%s) \
  --health-check-config '{
    "IPAddress": "primary.example.com",
    "Port": 443,
    "Type": "HTTPS",
    "RequestInterval": 30,
    "FailureThreshold": 3
  }'
```

---

## 4. VPC와 네트워크

### Q: VPC 피어링과 Transit Gateway의 차이는?

**VPC Peering**
```
┌─────────────────────────────────────────────────────────────┐
│  1:1 연결, 전이적 라우팅 불가                               │
│                                                             │
│   VPC A ────────── VPC B ────────── VPC C                  │
│                                                             │
│   A ↔ B 가능                                                │
│   B ↔ C 가능                                                │
│   A → B → C 불가능! (A↔C 별도 피어링 필요)                  │
│                                                             │
│  N개 VPC 전체 연결: N*(N-1)/2 개 피어링 필요                │
│  10개 VPC: 45개 피어링                                      │
└─────────────────────────────────────────────────────────────┘
```

**Transit Gateway**
```
┌─────────────────────────────────────────────────────────────┐
│  Hub-Spoke 모델, 중앙 집중 라우팅                           │
│                                                             │
│        VPC A ─────┐                                        │
│                   │                                        │
│        VPC B ─────┼───→ Transit Gateway ←─── On-Premise   │
│                   │           │                            │
│        VPC C ─────┘           │                            │
│                               ↓                            │
│                          라우팅 테이블                      │
│                                                             │
│  모든 VPC 간 통신 가능 (라우팅 설정으로 제어)               │
│  N개 VPC: N개 연결만 필요                                   │
└─────────────────────────────────────────────────────────────┘
```

| 특성 | VPC Peering | Transit Gateway |
|------|-------------|-----------------|
| 연결 방식 | 1:1 | Hub-Spoke |
| 전이적 라우팅 | X | O |
| 대역폭 | 제한 없음 | 50 Gbps |
| 비용 | 무료 (데이터 전송만) | 연결당 + 데이터 |
| 복잡도 | VPC 증가시 복잡 | 단순 |
| 사용 사례 | 소수 VPC | 대규모 네트워크 |

### Q: Security Group과 NACL의 차이는?

| 특성 | Security Group | NACL |
|------|----------------|------|
| 적용 범위 | 인스턴스 | 서브넷 |
| 상태 | Stateful | Stateless |
| 규칙 | 허용만 | 허용 + 거부 |
| 평가 순서 | 모든 규칙 평가 | 번호 순서대로 |
| 기본 | 모두 거부 | 모두 허용 |

```
Stateful (Security Group):
인바운드 허용 → 아웃바운드 응답 자동 허용

Stateless (NACL):
인바운드 허용 + 아웃바운드 응답도 별도 허용 필요
```

---

## 5. AWS 핵심 서비스

### Q: 서비스 선택 기준을 설명해주세요.

**컴퓨팅**

| 서비스 | 사용 시점 |
|--------|----------|
| EC2 | 완전한 제어 필요, 커스텀 환경 |
| ECS/EKS | 컨테이너 오케스트레이션 |
| Lambda | 이벤트 기반, 짧은 실행 시간 |
| Fargate | 서버리스 컨테이너 |

**데이터베이스**

| 서비스 | 사용 시점 |
|--------|----------|
| RDS | 관계형 DB, 관리형 필요 |
| Aurora | 고성능 RDS, MySQL/PostgreSQL 호환 |
| DynamoDB | NoSQL, 무제한 확장, 밀리초 지연 |
| ElastiCache | 인메모리 캐시 (Redis/Memcached) |
| DocumentDB | MongoDB 호환 |

**스토리지**

| 서비스 | 사용 시점 |
|--------|----------|
| S3 | 객체 스토리지, 정적 파일, 백업 |
| EBS | EC2 블록 스토리지 |
| EFS | 공유 파일 시스템 (NFS) |
| FSx | Windows 파일 서버, Lustre |

**메시징**

| 서비스 | 사용 시점 |
|--------|----------|
| SQS | 메시지 큐, 비동기 처리 |
| SNS | Pub/Sub, 알림 |
| EventBridge | 이벤트 버스, 서비스 통합 |
| Kinesis | 실시간 스트리밍 |

---

## 6. 클라우드 설계 원칙

### Q: Well-Architected Framework의 핵심 원칙은?

**6가지 핵심 Pillar**

```
┌─────────────────────────────────────────────────────────────┐
│  1. Operational Excellence (운영 우수성)                    │
│     - IaC (Infrastructure as Code)                          │
│     - 자동화된 배포                                         │
│     - 변경 관리                                             │
│     - 모니터링 및 로깅                                      │
├─────────────────────────────────────────────────────────────┤
│  2. Security (보안)                                         │
│     - 최소 권한 원칙                                        │
│     - 데이터 암호화 (전송 중 / 저장 시)                     │
│     - 추적성 (CloudTrail)                                   │
│     - 자동화된 보안 테스트                                  │
├─────────────────────────────────────────────────────────────┤
│  3. Reliability (신뢰성)                                    │
│     - 자동 복구                                             │
│     - 수평 확장                                             │
│     - 장애 격리                                             │
│     - 변경 관리                                             │
├─────────────────────────────────────────────────────────────┤
│  4. Performance Efficiency (성능 효율성)                    │
│     - 적절한 리소스 선택                                    │
│     - 글로벌 배포 (CDN, Edge)                               │
│     - 서버리스 활용                                         │
│     - 성능 모니터링                                         │
├─────────────────────────────────────────────────────────────┤
│  5. Cost Optimization (비용 최적화)                         │
│     - 적정 사이징                                           │
│     - Reserved / Spot 인스턴스                              │
│     - 사용하지 않는 리소스 제거                             │
│     - 비용 할당 태그                                        │
├─────────────────────────────────────────────────────────────┤
│  6. Sustainability (지속 가능성)                            │
│     - 효율적인 리소스 사용                                  │
│     - 탄소 발자국 최소화                                    │
│     - 리전 선택 고려                                        │
└─────────────────────────────────────────────────────────────┘
```

**설계 원칙**
```
1. Design for Failure
   - 모든 것은 실패할 수 있다고 가정
   - 단일 장애점(SPOF) 제거

2. Loosely Coupled
   - 서비스 간 느슨한 결합
   - SQS, SNS로 비동기 통신

3. Elasticity
   - 수요에 따라 자동 확장/축소
   - Auto Scaling 활용

4. Think Parallel
   - 수평 확장 가능한 설계
   - 작업 분산 처리

5. Cattle, Not Pets
   - 서버를 일회용으로 취급
   - 자동화된 프로비저닝
```

---

## 핵심 정리

| 주제 | 핵심 키워드 |
|------|-------------|
| 고가용성 | Multi-AZ, Active-Active, SLA 99.99% |
| Auto Scaling | Target Tracking, Cooldown, Warm-up |
| RTO/RPO | 복구시간/데이터손실, Pilot Light, Warm Standby |
| VPC | Peering(1:1), Transit Gateway(Hub-Spoke), SG vs NACL |
| 서비스 선택 | 용도별 적합 서비스, Managed vs Serverless |
| Well-Architected | 6 Pillars, Design for Failure |

---

*마지막 업데이트: 2025년 01월*
