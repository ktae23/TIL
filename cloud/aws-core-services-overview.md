# AWS 핵심 서비스 개요

백엔드 개발자가 알아야 할 AWS 핵심 서비스를 정리합니다.

## 목차

1. [컴퓨팅 서비스](#1-컴퓨팅-서비스)
2. [스토리지 서비스](#2-스토리지-서비스)
3. [데이터베이스 서비스](#3-데이터베이스-서비스)
4. [네트워킹 서비스](#4-네트워킹-서비스)
5. [메시징 서비스](#5-메시징-서비스)
6. [서비스 선택 가이드](#6-서비스-선택-가이드)

---

## 1. 컴퓨팅 서비스

### EC2 (Elastic Compute Cloud)

```
가상 서버 (인스턴스)

인스턴스 유형:
- t3: 범용 (버스트 가능)
- m5: 범용 (균형잡힌 성능)
- c5: 컴퓨팅 최적화
- r5: 메모리 최적화
- i3: 스토리지 최적화

구매 옵션:
- On-Demand: 시간당 과금, 유연함
- Reserved: 1-3년 약정, 최대 72% 할인
- Spot: 여유 용량 활용, 최대 90% 할인 (중단 가능)
```

### Lambda

```
서버리스 함수 실행

특징:
- 이벤트 기반 실행
- 자동 스케일링
- 실행 시간당 과금 (최대 15분)
- 메모리 128MB ~ 10GB

사용 사례:
- API 백엔드
- 이벤트 처리
- 스케줄 작업
- 데이터 변환
```

### ECS / EKS / Fargate

```
ECS (Elastic Container Service):
- AWS 자체 컨테이너 오케스트레이션
- EC2 또는 Fargate에서 실행

EKS (Elastic Kubernetes Service):
- 관리형 Kubernetes
- K8s 생태계 활용 가능

Fargate:
- 서버리스 컨테이너
- 인프라 관리 불필요
- vCPU/메모리 기반 과금
```

---

## 2. 스토리지 서비스

### S3 (Simple Storage Service)

```
객체 스토리지

스토리지 클래스:
- Standard: 자주 접근
- Standard-IA: 비자주 접근
- Glacier: 아카이브 (검색 지연)
- Glacier Deep Archive: 장기 보관

특징:
- 99.999999999% 내구성
- 버전 관리
- 수명 주기 정책
- 정적 웹 호스팅
```

### EBS (Elastic Block Store)

```
EC2용 블록 스토리지 (디스크)

볼륨 유형:
- gp3: 범용 SSD (권장)
- io2: 고성능 SSD (IOPS 보장)
- st1: 처리량 최적화 HDD
- sc1: 콜드 HDD

특징:
- 스냅샷 지원
- 암호화 지원
- 가용 영역 내 복제
```

### EFS (Elastic File System)

```
관리형 NFS

특징:
- 여러 EC2에서 공유 가능
- 자동 확장/축소
- 가용 영역 간 복제

사용 사례:
- 공유 파일 시스템
- 컨테이너 스토리지
- 빅데이터 분석
```

---

## 3. 데이터베이스 서비스

### RDS (Relational Database Service)

```
관리형 관계형 DB

지원 엔진:
- MySQL, PostgreSQL
- MariaDB
- Oracle, SQL Server
- Aurora (AWS 자체)

특징:
- 자동 백업
- Multi-AZ 고가용성
- Read Replica
- 자동 패치
```

### Aurora

```
AWS 클라우드 네이티브 RDB

특징:
- MySQL/PostgreSQL 호환
- RDS 대비 3-5배 성능
- 스토리지 자동 확장 (128TB)
- 6개 복제본 (3개 AZ)

Aurora Serverless:
- 자동 스케일링
- 초 단위 과금
- 간헐적 워크로드에 적합
```

### DynamoDB

```
관리형 NoSQL (키-값, 문서)

특징:
- 무제한 확장
- 밀리초 지연 시간
- 자동 스케일링
- 글로벌 테이블

과금:
- 온디맨드: 요청당
- 프로비저닝: RCU/WCU 설정
```

### ElastiCache

```
관리형 인메모리 캐시

엔진:
- Redis: 풍부한 자료구조, 복제, 영속성
- Memcached: 단순 캐시, 멀티스레드

사용 사례:
- 세션 저장
- 데이터베이스 캐시
- 실시간 리더보드
```

---

## 4. 네트워킹 서비스

### VPC (Virtual Private Cloud)

```
가상 네트워크

구성 요소:
- Subnet: 퍼블릭/프라이빗
- Route Table: 라우팅 규칙
- Internet Gateway: 인터넷 연결
- NAT Gateway: 프라이빗 서브넷 아웃바운드
- Security Group: 인스턴스 방화벽
- NACL: 서브넷 방화벽
```

### ELB (Elastic Load Balancing)

```
로드밸런서

유형:
- ALB (Application): L7, HTTP/HTTPS
- NLB (Network): L4, TCP/UDP, 고성능
- CLB (Classic): 레거시

ALB 특징:
- 경로 기반 라우팅
- 호스트 기반 라우팅
- 웹소켓 지원
```

### Route 53

```
관리형 DNS

기능:
- 도메인 등록
- DNS 라우팅
- 헬스 체크
- 트래픽 정책

라우팅 정책:
- Simple: 단일 레코드
- Weighted: 가중치 기반
- Latency: 지연 시간 기반
- Failover: 장애 조치
- Geolocation: 지리적 위치
```

### CloudFront

```
CDN (Content Delivery Network)

특징:
- 전 세계 엣지 로케이션
- HTTPS 지원
- 캐싱 정책
- Lambda@Edge (엣지 함수)

사용 사례:
- 정적 콘텐츠 배포
- API 가속
- 동적 콘텐츠 가속
```

---

## 5. 메시징 서비스

### SQS (Simple Queue Service)

```
관리형 메시지 큐

유형:
- Standard: 최소 1회 전달, 순서 보장 X
- FIFO: 정확히 1회 전달, 순서 보장

특징:
- 무제한 처리량
- 메시지 보존 (최대 14일)
- Dead Letter Queue
- Long Polling
```

### SNS (Simple Notification Service)

```
Pub/Sub 메시징

구독자:
- SQS, Lambda, HTTP/HTTPS
- Email, SMS
- 모바일 푸시

사용 사례:
- 팬아웃 패턴
- 알림 시스템
- 이벤트 브로드캐스트
```

### EventBridge

```
서버리스 이벤트 버스

특징:
- AWS 서비스 이벤트 자동 수집
- 규칙 기반 라우팅
- SaaS 연동
- 스키마 레지스트리

사용 사례:
- 이벤트 기반 아키텍처
- 서비스 통합
- 자동화 워크플로우
```

### Kinesis

```
실시간 스트리밍 데이터

서비스:
- Kinesis Data Streams: 데이터 스트림
- Kinesis Firehose: 데이터 전송
- Kinesis Analytics: 실시간 분석

사용 사례:
- 로그 수집
- 실시간 분석
- IoT 데이터 처리
```

---

## 6. 서비스 선택 가이드

### 컴퓨팅 선택

```
상시 운영 서버 → EC2
서버리스 함수 → Lambda
컨테이너 (관리 원함) → ECS + Fargate
컨테이너 (K8s 필요) → EKS
```

### 데이터베이스 선택

```
관계형 (범용) → RDS
관계형 (고성능) → Aurora
NoSQL (키-값) → DynamoDB
캐시 → ElastiCache (Redis)
```

### 스토리지 선택

```
객체/파일 저장 → S3
EC2 디스크 → EBS
공유 파일 시스템 → EFS
```

### 메시징 선택

```
비동기 작업 큐 → SQS
팬아웃/알림 → SNS
이벤트 라우팅 → EventBridge
실시간 스트리밍 → Kinesis
```

---

*마지막 업데이트: 2026년 01월*
