# CI/CD 개념과 파이프라인 설계 원칙

CI/CD는 소프트웨어 개발에서 코드 변경사항을 자동으로 빌드, 테스트, 배포하는 일련의 자동화 프로세스다. 현대 소프트웨어 개발의 핵심 관행으로, 빠르고 안정적인 소프트웨어 딜리버리를 가능하게 한다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### CI (Continuous Integration) — 지속적 통합

CI는 개발자들이 코드 변경사항을 공유 브랜치에 자주 병합(merge)하는 개발 관행이다. 각 병합마다 자동화된 빌드와 테스트가 실행되어 통합 오류를 빠르게 발견한다.

**CI의 핵심 원칙:**
- 모든 코드 변경은 즉시 메인 브랜치에 통합
- 자동화된 빌드와 테스트 실행
- 빌드 실패 시 즉시 수정 (broken build는 최우선 과제)
- 테스트 코드는 프로덕션 코드와 함께 작성

### CD (Continuous Delivery vs Continuous Deployment)

CD는 두 가지 의미를 가진다:

**Continuous Delivery (지속적 전달):**
- CI를 통과한 코드가 프로덕션 배포 가능한 상태로 유지
- 실제 배포는 수동 승인을 거쳐 실행
- "언제든 배포할 수 있는 상태"가 핵심

**Continuous Deployment (지속적 배포):**
- CI를 통과한 코드가 자동으로 프로덕션에 배포
- 수동 개입 없이 전 과정이 자동화
- 모든 테스트를 통과하면 즉시 릴리스

```
┌─────────────────────────────────────────────────────────────────┐
│                    CI/CD 스펙트럼                                │
│                                                                 │
│  Continuous Integration                                         │
│  ├── Code → Build → Unit Test → Integration Test               │
│  │                                                              │
│  Continuous Delivery                                            │
│  ├── ... → Staging Deploy → Acceptance Test → [Manual Approve]  │
│  │                                                              │
│  Continuous Deployment                                          │
│  └── ... → Staging Deploy → Acceptance Test → Auto Deploy       │
└─────────────────────────────────────────────────────────────────┘
```

### 파이프라인 (Pipeline)

파이프라인은 코드가 커밋부터 프로덕션까지 거치는 자동화된 단계의 연속이다. 각 단계(Stage)는 특정 작업을 수행하며, 이전 단계가 성공해야 다음 단계로 진행된다.

## 2. 왜 알아야 하는가 (Why)

### 수동 배포의 문제점

| 문제 | 설명 |
|------|------|
| Human Error | 수동 작업 중 실수 발생 가능 |
| 느린 피드백 | 통합 오류를 뒤늦게 발견 |
| 배포 공포 | 배포가 두려운 이벤트가 됨 |
| 일관성 부재 | 환경마다 다른 배포 절차 |
| 추적 불가 | 누가, 언제, 무엇을 배포했는지 불명확 |

### CI/CD의 비즈니스 가치

- **배포 빈도 증가**: 주 1회 → 하루 수십 회 배포 가능
- **변경 실패율 감소**: 자동화된 테스트로 결함 조기 발견
- **복구 시간 단축 (MTTR)**: 문제 발생 시 빠른 롤백
- **리드 타임 단축**: 코드 커밋부터 프로덕션 배포까지의 시간 최소화

이것은 DORA(DevOps Research and Assessment) 메트릭의 4가지 핵심 지표와 정확히 일치한다.

## 3. 내부 구현 분석 (How)

### 파이프라인 스테이지 설계

일반적인 CI/CD 파이프라인은 다음과 같은 스테이지로 구성된다:

```mermaid
graph LR
    A[Source] --> B[Build]
    B --> C[Unit Test]
    C --> D[Static Analysis]
    D --> E[Integration Test]
    E --> F[Package]
    F --> G[Deploy to Staging]
    G --> H[Acceptance Test]
    H --> I[Deploy to Production]
    I --> J[Post-Deploy Verify]
```

#### Stage 1: Source (소스)
- 코드 저장소에서 변경 감지
- 트리거: push, pull request, schedule, manual

#### Stage 2: Build (빌드)
- 소스 코드 컴파일 및 의존성 해결
- Docker 이미지 빌드
- Build artifact 생성

#### Stage 3: Test (테스트)
- Unit Test → Integration Test → E2E Test 순서로 실행
- **테스트 피라미드** 원칙 적용

```
        /\
       /  \      E2E Tests (소수, 느림)
      /    \
     /──────\
    / Integra \   Integration Tests (중간)
   /   tion    \
  /──────────────\
 /   Unit Tests   \  Unit Tests (다수, 빠름)
/──────────────────\
```

#### Stage 4: Static Analysis (정적 분석)
- 코드 품질 검사 (SonarQube, ESLint 등)
- 보안 취약점 스캔 (SAST)
- 의존성 취약점 검사 (Dependency Check)

#### Stage 5: Package (패키징)
- Docker 이미지 생성 및 레지스트리 푸시
- JAR/WAR 파일 생성
- Helm Chart 패키징

#### Stage 6: Deploy (배포)
- Staging 환경 배포 → 검증 → Production 배포
- 배포 전략 적용 (Blue-Green, Canary, Rolling)

#### Stage 7: Verify (검증)
- Smoke Test 실행
- 헬스 체크 확인
- 모니터링 메트릭 검증

### 파이프라인 설계 원칙

#### 1. Fail Fast (빠른 실패)
가장 빠르고 자주 실패하는 단계를 앞에 배치한다.

```
좋은 순서:  Lint → Unit Test → Build → Integration Test → Deploy
나쁜 순서:  Build → Deploy → Integration Test → Unit Test → Lint
```

#### 2. Pipeline as Code
파이프라인 정의를 코드로 관리하여 버전 관리, 코드 리뷰, 재사용이 가능하게 한다.

#### 3. Immutable Artifacts
한 번 빌드된 아티팩트는 변경하지 않고 모든 환경에서 동일하게 사용한다.

```
Build Stage에서 생성된 artifact (v1.2.3)
  → Staging에서 테스트: 동일한 v1.2.3
  → Production에 배포: 동일한 v1.2.3
```

#### 4. Environment Parity (환경 동등성)
개발/스테이징/프로덕션 환경을 최대한 동일하게 유지한다.

#### 5. Secret 관리
민감한 정보(API 키, 비밀번호)는 파이프라인 코드에 포함하지 않고 별도 Secret Manager를 사용한다.

## 4. 실전 예제

### 예제 1: 전형적인 웹 애플리케이션 파이프라인

```yaml
# 개념적인 파이프라인 정의 (CI/CD 도구 공통)
pipeline:
  stages:
    - name: build
      steps:
        - checkout code
        - install dependencies
        - compile source
        - build docker image

    - name: test
      parallel:
        - unit-test:
            command: npm test
            coverage: 80%
        - lint:
            command: npm run lint
        - security-scan:
            command: npm audit

    - name: deploy-staging
      steps:
        - push docker image
        - deploy to staging cluster
        - run smoke tests
      trigger: auto  # 자동 실행

    - name: deploy-production
      steps:
        - deploy with canary strategy
        - monitor metrics for 10m
        - promote or rollback
      trigger: manual  # 수동 승인 필요
```

### 예제 2: 마이크로서비스 파이프라인 설계

```
┌─────────────────────────────────────────────────────┐
│              Monorepo 파이프라인                      │
│                                                     │
│  Change Detection                                   │
│  ├── service-a/ 변경됨? → Service A Pipeline 실행    │
│  ├── service-b/ 변경됨? → Service B Pipeline 실행    │
│  ├── shared-lib/ 변경됨? → 전체 Pipeline 실행        │
│  └── infra/ 변경됨? → Infrastructure Pipeline 실행   │
│                                                     │
│  각 서비스 파이프라인은 독립적으로 병렬 실행            │
└─────────────────────────────────────────────────────┘
```

### 예제 3: 파이프라인 실행 시간 최적화

```yaml
# 병렬 실행으로 파이프라인 시간 단축
stages:
  - name: parallel-checks
    parallel:
      - job: unit-test        # 2분
      - job: lint              # 30초
      - job: security-scan     # 1분
      - job: build-image       # 3분
    # 전체 소요: 3분 (직렬 시 6분 30초)

  - name: integration-test     # 5분
    needs: [parallel-checks]

  - name: deploy               # 2분
    needs: [integration-test]

# 총 소요: 10분 (직렬 시 15분 30초)
```

## 5. 정리

| 개념 | 설명 | 핵심 포인트 |
|------|------|------------|
| CI | 지속적 통합 | 코드 변경을 자주 병합하고 자동 테스트 |
| Continuous Delivery | 지속적 전달 | 항상 배포 가능한 상태 유지, 수동 승인 |
| Continuous Deployment | 지속적 배포 | 테스트 통과 시 자동 프로덕션 배포 |
| Pipeline | 파이프라인 | 코드→빌드→테스트→배포의 자동화된 흐름 |
| Fail Fast | 빠른 실패 | 빠른 검증 단계를 먼저 실행 |
| Immutable Artifact | 불변 아티팩트 | 동일한 빌드 결과물을 모든 환경에 사용 |
| Pipeline as Code | 코드형 파이프라인 | 파이프라인 정의를 코드로 관리 |

### CI/CD 도구 비교

| 도구 | 유형 | 특징 |
|------|------|------|
| GitHub Actions | SaaS | GitHub 통합, YAML 기반, 무료 tier |
| Jenkins | Self-hosted | 높은 커스터마이징, 플러그인 생태계 |
| GitLab CI | SaaS/Self-hosted | GitLab 통합, Auto DevOps |
| CircleCI | SaaS | Docker 네이티브, 빠른 실행 |
| ArgoCD | Self-hosted | GitOps 기반 CD, Kubernetes 특화 |

---
*참고: DORA Metrics 2023, Martin Fowler - Continuous Integration*
