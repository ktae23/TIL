# 기술 면접 대비 학습 로드맵

5년차 백엔드 개발자 기술 면접 준비를 위한 최적 학습 순서

---

## 학습 전략 개요

```
면접 출제 빈도 + 개념 의존성을 고려한 순서

Phase 1: 핵심 기초 (거의 100% 출제)
  → Java, Spring, DB 기본기

Phase 2: 심화 + 실무 (80% 출제)
  → 동시성, 트랜잭션, 캐시, 네트워크

Phase 3: 아키텍처 (60% 출제)
  → MSA, 시스템 설계, 클라우드

Phase 4: 차별화 (선택적 심화)
  → 트러블슈팅 경험, 테스트 전략
```

---

## Phase 1: 핵심 기초 (1주차)

**목표**: 거의 모든 면접에서 물어보는 기본기 완성

### Day 1-2: Java 기본
| 순서 | 파일 | 핵심 질문 |
|------|------|----------|
| 1 | [java-interview-questions.md](til/java/java-interview-questions.md) | String 불변성, equals/hashCode, 제네릭 |
| 2 | [java-collections-deep-dive.md](til/java/java-collections-deep-dive.md) | HashMap 동작 원리, ConcurrentHashMap |

### Day 3-4: Spring 기본
| 순서 | 파일 | 핵심 질문 |
|------|------|----------|
| 3 | [spring-interview-questions.md](til/spring/spring-interview-questions.md) | IoC/DI, Bean 생명주기, 순환 참조 |
| 4 | [spring-aop-basics.md](til/spring/spring-aop-basics.md) | AOP 동작, JDK Proxy vs CGLIB |
| 5 | [spring-transaction-basics.md](til/spring/spring-transaction-basics.md) | @Transactional 전파 속성 |

### Day 5-7: DB 기본
| 순서 | 파일 | 핵심 질문 |
|------|------|----------|
| 6 | [mysql-interview-questions.md](til/DB/mysql-interview-questions.md) | MVCC, Redo/Undo 로그, Replication |
| 7 | [mysql-index-basics.md](til/DB/mysql-index-basics.md) | B-Tree, 클러스터드 인덱스, 커버링 인덱스 |
| 8 | [mysql-transaction-isolation.md](til/DB/mysql-transaction-isolation.md) | 4가지 격리 수준, Phantom Read |

---

## Phase 2: 심화 + 실무 (2주차)

**목표**: 중급 이상 질문 대비, 실무 경험 어필 포인트

### Day 8-9: 동시성
| 순서 | 파일 | 핵심 질문 |
|------|------|----------|
| 9 | [java-concurrency-interview.md](til/java/java-concurrency-interview.md) | synchronized vs Lock, volatile, ThreadLocal |
| 10 | [mysql-lock-deep-dive.md](til/DB/mysql-lock-deep-dive.md) | Gap Lock, Next-Key Lock, 데드락 |

### Day 10-11: 캐시
| 순서 | 파일 | 핵심 질문 |
|------|------|----------|
| 11 | [redis-interview-questions.md](til/cache/redis-interview-questions.md) | 싱글스레드 성능, RDB/AOF, Cluster |
| 12 | [cache-patterns.md](til/cache/cache-patterns.md) | Cache-Aside, Write-Behind 패턴 |
| 13 | [cache-invalidation-strategies.md](til/cache/cache-invalidation-strategies.md) | TTL, 이벤트 기반 무효화 |

### Day 12-14: 네트워크
| 순서 | 파일 | 핵심 질문 |
|------|------|----------|
| 14 | [network-interview-questions.md](til/network/network-interview-questions.md) | OSI 7계층, HTTP 버전 비교, TIME_WAIT |
| 15 | [tcp-ip-fundamentals.md](til/network/tcp-ip-fundamentals.md) | 3-way/4-way 핸드셰이크 |
| 16 | [load-balancer-deep-dive.md](til/network/load-balancer-deep-dive.md) | L4 vs L7, 부하 분산 알고리즘 |

---

## Phase 3: 아키텍처 (3주차)

**목표**: 시스템 설계 면접 대비, 아키텍처 관점 답변

### Day 15-17: MSA
| 순서 | 파일 | 핵심 질문 |
|------|------|----------|
| 17 | [msa-interview-questions.md](til/MSA/msa-interview-questions.md) | 분산 트랜잭션, CAP, Eventual Consistency |
| 18 | [msa-fundamentals.md](til/MSA/msa-fundamentals.md) | MSA vs 모놀리식, 서비스 분리 기준 |
| 19 | [saga-pattern-deep-dive.md](til/MSA/saga-pattern-deep-dive.md) | Choreography vs Orchestration |
| 20 | [circuit-breaker-implementation.md](til/MSA/circuit-breaker-implementation.md) | Resilience4j, Fallback 전략 |

### Day 18-19: 클라우드
| 순서 | 파일 | 핵심 질문 |
|------|------|----------|
| 21 | [cloud-architecture-interview.md](til/cloud/cloud-architecture-interview.md) | 고가용성, Auto Scaling, RTO/RPO |
| 22 | [aws-core-services-overview.md](til/cloud/aws-core-services-overview.md) | EC2, RDS, S3, Lambda, ELB |
| 23 | [aws-networking-interview.md](til/cloud/aws-networking-interview.md) | VPC, Security Group vs NACL |

### Day 20-21: 보안 + API
| 순서 | 파일 | 핵심 질문 |
|------|------|----------|
| 24 | [spring-security-interview.md](til/spring/spring-security-interview.md) | SecurityFilterChain, JWT, OAuth2 |
| 25 | [api-gateway-patterns.md](til/MSA/api-gateway-patterns.md) | 라우팅, Rate Limiting |

---

## Phase 4: 차별화 (4주차)

**목표**: 실무 경험 기반 답변으로 차별화

### Day 22-24: 트러블슈팅
| 순서 | 파일 | 어필 포인트 |
|------|------|------------|
| 26 | [mysql-troubleshooting.md](til/DB/mysql-troubleshooting.md) | 슬로우쿼리 해결 경험 |
| 27 | [java-memory-leak-detection.md](til/java/java-memory-leak-detection.md) | OOM 분석 경험 |
| 28 | [java-thread-dump-analysis.md](til/java/java-thread-dump-analysis.md) | 데드락 해결 경험 |
| 29 | [spring-transaction-pitfalls.md](til/spring/spring-transaction-pitfalls.md) | Self-Invocation 이슈 경험 |

### Day 25-26: 고급 캐시/MSA
| 순서 | 파일 | 어필 포인트 |
|------|------|------------|
| 30 | [redis-distributed-lock.md](til/cache/redis-distributed-lock.md) | 분산 락 구현 경험 |
| 31 | [cache-stampede-prevention.md](til/cache/cache-stampede-prevention.md) | 대규모 트래픽 대응 |
| 32 | [event-driven-architecture.md](til/MSA/event-driven-architecture.md) | 이벤트 소싱, CQRS |
| 33 | [distributed-tracing-basics.md](til/MSA/distributed-tracing-basics.md) | 분산 추적 구축 경험 |

### Day 27-28: 테스트
| 순서 | 파일 | 어필 포인트 |
|------|------|------------|
| 34 | [testing-interview-questions.md](til/TEST/testing-interview-questions.md) | TDD/BDD, 테스트 전략 |
| 35 | [integration-testing-strategies.md](til/TEST/integration-testing-strategies.md) | 통합 테스트 설계 |
| 36 | [testcontainers-guide.md](til/TEST/testcontainers-guide.md) | 테스트 환경 구축 경험 |

---

## 시간 없을 때: 핵심 8개만

면접까지 시간이 없다면 이것만 집중:

| 우선순위 | 파일 | 출제 빈도 |
|---------|------|----------|
| ⭐⭐⭐ | [java-interview-questions.md](til/java/java-interview-questions.md) | 100% |
| ⭐⭐⭐ | [spring-interview-questions.md](til/spring/spring-interview-questions.md) | 100% |
| ⭐⭐⭐ | [mysql-interview-questions.md](til/DB/mysql-interview-questions.md) | 95% |
| ⭐⭐⭐ | [java-concurrency-interview.md](til/java/java-concurrency-interview.md) | 90% |
| ⭐⭐ | [redis-interview-questions.md](til/cache/redis-interview-questions.md) | 85% |
| ⭐⭐ | [network-interview-questions.md](til/network/network-interview-questions.md) | 80% |
| ⭐⭐ | [msa-interview-questions.md](til/MSA/msa-interview-questions.md) | 75% |
| ⭐ | [cloud-architecture-interview.md](til/cloud/cloud-architecture-interview.md) | 60% |

---

## 학습 팁

### 1. 면접 답변 구조화
```
STAR 기법 적용:
- Situation: 어떤 상황이었는지
- Task: 무엇을 해결해야 했는지
- Action: 어떻게 해결했는지 (기술적 내용)
- Result: 결과와 배운 점
```

### 2. 꼬리 질문 대비
```
"HashMap 동작 원리" 질문 시:
→ 해시 충돌 처리 (Chaining → Treeify)
→ 로드 팩터와 리사이징
→ ConcurrentHashMap과 차이
→ 실무에서 주의할 점
```

### 3. 실무 연결
```
모든 개념을 실무 경험과 연결:
- "프로젝트에서 이 기술을 사용해서..."
- "이 문제를 겪었을 때..."
- "이 방식을 선택한 이유는..."
```

---

## til-viewer로 학습

```bash
# 브라우저에서 전체 TIL 열람
/til-viewer
```

- `Ctrl+K`: 검색
- `Ctrl+D`: 다크/라이트 모드

---

*최종 업데이트: 2025년 01월*
