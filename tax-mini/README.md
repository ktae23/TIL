# tax-mini 학습 로드맵

> tax-mini 프로젝트에 필요한 TIL 문서를 학습 순서대로 정리한 카테고리입니다.
> 모든 파일은 원본 카테고리의 심볼릭 링크이며, 번호 순서가 권장 학습 순서입니다.

## 프로젝트 개요

**tax-mini**는 세무기장 서비스를 MSA로 구현하는 학습용 프로젝트입니다.

- **bookkeeping-service**: 장부 기장 CRUD + 이벤트 발행
- **tax-calc-service**: 세금 계산 + Kafka 이벤트 소비
- **batch-service**: Spring Batch + ShedLock 기반 정산/리포트
- **인프라**: Kubernetes, Kafka, Redis, MySQL

---

## 학습 순서

### Phase 1: 도메인 이해 (Week 1)

| # | 문서 | 설명 |
|---|------|------|
| 01 | [세무기장 도메인 기초 가이드](01-tax-accounting-basics.md) | 복식부기, 계정과목, 세무신고 등 도메인 핵심 개념 |

### Phase 2: 빌드 도구 (Week 1)

| # | 문서 | 설명 |
|---|------|------|
| 02 | [Gradle 기초와 Maven 비교](02-gradle-basics.md) | Gradle 문법, 의존성 관리, Maven과의 차이 |
| 03 | [Gradle 멀티 모듈 프로젝트](03-gradle-multi-module.md) | 멀티 모듈 구성, common 모듈 분리 전략 |

### Phase 3: 데이터 레이어 (Week 1~2)

| # | 문서 | 설명 |
|---|------|------|
| 04 | [JPA 기초 개념](04-jpa-basics.md) | 영속성 컨텍스트, 엔티티 매핑, 연관관계 |
| 05 | [QueryDSL과 JPA 어노테이션](05-jpa-querydsl-annotations.md) | QueryDSL 설정, 동적 쿼리, 주요 어노테이션 |
| 06 | [Spring 트랜잭션 기초](06-spring-transaction-basics.md) | @Transactional, 전파 옵션, 격리 수준 |
| 07 | [MySQL Lock 완전 정리](07-mysql-lock-deep-dive.md) | 낙관적/비관적 락, 데드락 분석 및 해결 |

### Phase 4: 캐시 (Week 2)

| # | 문서 | 설명 |
|---|------|------|
| 08 | [Redis 기초](08-redis-basics.md) | 자료구조, 명령어, Spring Data Redis 연동 |
| 09 | [Redis 분산 락 구현 가이드](09-redis-distributed-lock.md) | Redisson, 분산 환경 동시성 제어 |
| 10 | [캐시 전략과 패턴](10-cache-strategies-and-patterns.md) | Cache-Aside, Write-Through, TTL 전략 |

### Phase 5: MSA & 이벤트 (Week 2)

| # | 문서 | 설명 |
|---|------|------|
| 11 | [이벤트 기반 아키텍처](11-event-driven-architecture.md) | EDA 개념, 이벤트 소싱, CQRS |
| 12 | [Kafka 완벽 가이드](12-kafka-complete-guide.md) | Producer/Consumer, 파티션, 멱등성, Spring Kafka |
| 13 | [Outbox 패턴 가이드](13-outbox-pattern-guide.md) | 트랜잭션-메시징 일관성 보장, Polling Publisher |
| 14 | [Saga 패턴 심층 분석](14-saga-pattern-deep-dive.md) | Choreography vs Orchestration, 보상 트랜잭션 |

### Phase 6: 배치 & 스케줄링 (Week 3)

| # | 문서 | 설명 |
|---|------|------|
| 15 | [Spring Batch 완벽 가이드](15-spring-batch-complete-guide.md) | Job/Step, Chunk 처리, Partitioning |
| 16 | [Quartz Scheduler](16-quartz.md) | Quartz 구조, 클러스터링, Spring 연동 |
| 17 | [ShedLock 분산 스케줄링 가이드](17-shedlock-distributed-scheduling.md) | Jenkins 없는 배치 스케줄링, DB 기반 락 |

### Phase 7: 테스트 (Week 3)

| # | 문서 | 설명 |
|---|------|------|
| 18 | [JUnit 5 기초](18-junit5-basics.md) | JUnit 5 구조, Assertions, Mockito 연동 |
| 19 | [Testcontainers 활용 가이드](19-testcontainers-guide.md) | Docker 기반 통합 테스트, MySQL/Kafka/Redis |

### Phase 8: 보안 (Week 3)

| # | 문서 | 설명 |
|---|------|------|
| 20 | [JWT, JWK, OAuth 2.0 비교](20-jwt-jwk-oauth-comparison.md) | 인증/인가 개념, 토큰 기반 인증 구현 |

### Phase 9: 인프라 & 배포 (Week 4)

| # | 문서 | 설명 |
|---|------|------|
| 21 | [Kubernetes 완벽 가이드](21-kubernetes-complete-guide.md) | Pod, Service, Deployment, Helm, HPA |

### Phase 10: 모니터링 (Week 4)

| # | 문서 | 설명 |
|---|------|------|
| 22 | [Prometheus + Grafana 모니터링](22-spring-boot-prometheus-grafana-monitoring.md) | Micrometer, 메트릭 수집, 대시보드 구성 |

---

## 참고

- 이 디렉토리의 모든 `.md` 파일은 원본 카테고리로의 **심볼릭 링크**입니다
- 원본 수정 시 이곳에도 자동 반영됩니다
- 연관 프로젝트: [`tax-mini`](https://github.com/buzz/tax-mini)
