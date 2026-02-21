# Hibernate ORM 내부 구현 심화 학습

Hibernate ORM의 내부 동작 원리를 소스 코드 레벨에서 분석한 학습 자료입니다.

## 학습 로드맵

### Main Track - 핵심 메커니즘

| # | 주제 | 파일 |
|---|------|------|
| 01 | SessionFactory 부트스트랩과 초기화 | [main/01-session-factory-bootstrap.md](main/01-session-factory-bootstrap.md) |
| 02 | Session 생명주기와 트랜잭션 관리 | [main/02-session-lifecycle.md](main/02-session-lifecycle.md) |
| 03 | PersistenceContext(1차 캐시) 내부 구조 | [main/03-persistence-context-internals.md](main/03-persistence-context-internals.md) |
| 04 | EntityEntry와 스냅샷 기반 Dirty Checking | [main/04-entity-entry-snapshot-dirty-checking.md](main/04-entity-entry-snapshot-dirty-checking.md) |
| 05 | ActionQueue와 Flush 실행 순서 | [main/05-action-queue-flush-ordering.md](main/05-action-queue-flush-ordering.md) |
| 06 | EntityInsertAction의 INSERT 처리 흐름 | [main/06-entity-insert-action-flow.md](main/06-entity-insert-action-flow.md) |
| 07 | CollectionUpdateAction과 FK UPDATE | [main/07-collection-action-fk-update.md](main/07-collection-action-fk-update.md) |
| 08 | OneToManyPersister: insertRows가 UPDATE를 실행하는 비밀 | [main/08-one-to-many-persister-update-secret.md](main/08-one-to-many-persister-update-secret.md) |
| 09 | Event/Listener 아키텍처 | [main/09-event-listener-architecture.md](main/09-event-listener-architecture.md) |
| 10 | Cascade 전파 메커니즘 | [main/10-cascade-propagation.md](main/10-cascade-propagation.md) |
| 11 | 엔티티 상태 전이 | [main/11-entity-state-transitions.md](main/11-entity-state-transitions.md) |
| 12 | ByteBuddy Proxy와 지연 로딩 | [main/12-proxy-lazy-loading.md](main/12-proxy-lazy-loading.md) |

### Advanced Track - 심화 분석

| # | 주제 | 파일 |
|---|------|------|
| 01 | HQL/JPQL 파싱과 SQM 트리 생성 | [advanced/01-hql-parsing-to-sqm.md](advanced/01-hql-parsing-to-sqm.md) |
| 02 | SQM 아키텍처 심화 | [advanced/02-sqm-architecture-deep-dive.md](advanced/02-sqm-architecture-deep-dive.md) |
| 03 | SQM에서 SQL AST로의 변환 과정 | [advanced/03-sqm-to-sql-ast-translation.md](advanced/03-sqm-to-sql-ast-translation.md) |
| 04 | SqlAstTranslator와 최종 SQL 생성 | [advanced/04-sql-ast-translator-sql-generation.md](advanced/04-sql-ast-translator-sql-generation.md) |
| 05 | EntityPersister 계층과 상속 전략 | [advanced/05-entity-persister-hierarchy.md](advanced/05-entity-persister-hierarchy.md) |
| 06 | MutationCoordinator 패턴 | [advanced/06-mutation-coordinator-pattern.md](advanced/06-mutation-coordinator-pattern.md) |
| 07 | OneToManyPersister vs BasicCollectionPersister | [advanced/07-collection-persister-comparison.md](advanced/07-collection-persister-comparison.md) |
| 08 | Flush 이벤트 파이프라인 전체 흐름 | [advanced/08-flush-event-pipeline.md](advanced/08-flush-event-pipeline.md) |
| 09 | Dirty Checking 심화 | [advanced/09-dirty-checking-deep-dive.md](advanced/09-dirty-checking-deep-dive.md) |
| 10 | Bytecode Enhancement 심화 | [advanced/10-bytecode-enhancement.md](advanced/10-bytecode-enhancement.md) |
| 11 | Batch Processing 내부 구현 | [advanced/11-batch-processing-internals.md](advanced/11-batch-processing-internals.md) |
| 12 | 2차 캐시 아키텍처 | [advanced/12-second-level-cache-architecture.md](advanced/12-second-level-cache-architecture.md) |

## 학습 순서 권장

1. **Main Track 01-04**: Session과 PersistenceContext의 기본 이해
2. **Main Track 05-08**: Flush와 Action 처리 메커니즘
3. **Main Track 09-12**: Event 시스템과 Proxy
4. **Advanced Track**: 쿼리 파이프라인과 고급 내부 구현

## 참고

- Hibernate ORM 6.x 기준
- 각 문서는 What/Why/How 구조로 작성됨
- 소스 코드 참조: `hibernate-core-6.5.3.Final-sources.jar`
