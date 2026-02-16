# 버전 관리와 브랜칭 전략

브랜칭 전략은 CI/CD 파이프라인의 근간이 되는 요소로, 팀이 코드를 어떻게 분기하고 병합하는지 결정한다. 올바른 브랜칭 전략 선택은 배포 빈도, 코드 안정성, 팀 생산성에 직접적 영향을 미친다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### 브랜칭 전략이란?

브랜칭 전략은 소프트웨어 개발팀이 Git 브랜치를 생성, 관리, 병합하는 규칙과 관행의 집합이다. CI/CD 파이프라인은 브랜칭 전략에 따라 트리거 조건과 배포 대상이 달라진다.

### 주요 브랜칭 전략

1. **Git Flow** — Vincent Driessen이 제안한 체계적 브랜칭 모델
2. **GitHub Flow** — 단순화된 브랜칭 모델, PR 중심
3. **Trunk-Based Development** — 메인 브랜치 중심 개발
4. **GitLab Flow** — 환경별 브랜치를 활용한 배포 모델

## 2. 왜 알아야 하는가 (Why)

### 브랜칭 전략과 CI/CD의 관계

브랜칭 전략은 CI/CD의 복잡도와 배포 속도를 직접 결정한다:

| 브랜칭 전략 | 배포 빈도 | CI/CD 복잡도 | 적합한 팀 규모 |
|------------|----------|-------------|--------------|
| Git Flow | 낮음 (주/월 단위) | 높음 | 대규모 |
| GitHub Flow | 높음 (일 단위) | 낮음 | 소~중규모 |
| Trunk-Based | 매우 높음 (시간 단위) | 중간 | 모든 규모 |

- **긴 수명의 브랜치** → 머지 충돌 증가 → CI 신뢰도 하락
- **짧은 수명의 브랜치** → 빈번한 통합 → CI/CD 효율 극대화

## 3. 내부 구현 분석 (How)

### Git Flow

```
main (production)
│
├── hotfix/fix-login ──────────────────┐
│                                      ↓
├── release/1.2.0 ──────────── merge → main
│         ↑                            │
│         │                            ↓
develop ──┴── feature/user-auth ──→ develop
              feature/dashboard ──→ develop
              feature/api-v2 ────→ develop
```

**브랜치 구조:**
- `main`: 프로덕션에 배포된 코드
- `develop`: 다음 릴리스를 위한 개발 브랜치
- `feature/*`: 기능 개발 브랜치 (develop에서 분기)
- `release/*`: 릴리스 준비 브랜치 (develop에서 분기)
- `hotfix/*`: 긴급 수정 브랜치 (main에서 분기)

**CI/CD 파이프라인 매핑:**
```yaml
# Git Flow에서의 파이프라인 트리거
triggers:
  feature/*:
    - build → unit-test → lint
  develop:
    - build → test → deploy-to-dev
  release/*:
    - build → test → deploy-to-staging → acceptance-test
  main:
    - build → test → deploy-to-production
  hotfix/*:
    - build → test → deploy-to-staging → deploy-to-production
```

**장점:**
- 릴리스 버전 관리가 명확
- 프로덕션과 개발 코드의 분리가 철저
- 여러 버전을 동시에 유지보수 가능

**단점:**
- 브랜치 관리 오버헤드가 큼
- develop과 main 간 괴리 발생 가능
- CI/CD 파이프라인 구성이 복잡

### GitHub Flow

```
main ─────────────────────────────────────────→
       ↑           ↑            ↑
       │           │            │
       └─ feat-A ──┘  └─ feat-B ┘
          (PR #1)       (PR #2)
```

**규칙:**
1. `main`은 항상 배포 가능한 상태
2. 기능 개발은 `main`에서 브랜치 생성
3. 작업 완료 후 Pull Request 생성
4. 코드 리뷰 후 `main`에 병합
5. 병합 즉시 배포

**CI/CD 파이프라인 매핑:**
```yaml
# GitHub Flow에서의 파이프라인 트리거
on:
  pull_request:
    branches: [main]
    # → build, test, lint, preview deploy
  push:
    branches: [main]
    # → build, test, deploy to production
```

**장점:**
- 단순하고 이해하기 쉬움
- CI/CD 구성이 간단
- 빈번한 배포에 적합

**단점:**
- 릴리스 버전 관리가 어려움
- 환경별 배포(staging/prod) 분리가 불명확
- 대규모 팀에서는 main에 부하 집중

### Trunk-Based Development

```
main (trunk) ─────────────────────────────────→
     ↑    ↑    ↑    ↑    ↑    ↑    ↑    ↑
     │    │    │    │    │    │    │    │
     └─s──┘ └─s──┘ └─s──┘ └─s──┘ └─s──┘
       (short-lived branches, < 1-2 days)
```

**규칙:**
1. 모든 개발자가 `main`(trunk)에 직접 커밋하거나 매우 짧은 수명의 브랜치 사용
2. 브랜치 수명은 최대 1~2일
3. Feature Flag로 미완성 기능을 숨김
4. 릴리스 브랜치는 필요 시에만 생성 (release/1.x)

**CI/CD 파이프라인 매핑:**
```yaml
# Trunk-Based에서의 파이프라인 트리거
on:
  push:
    branches: [main]
    # → build, test, deploy (모든 커밋마다)
```

**장점:**
- 통합 충돌 최소화
- CI/CD가 가장 단순
- 배포 빈도 극대화
- Google, Facebook 등 대규모 기업에서 사용

**단점:**
- Feature Flag 인프라 필요
- 높은 테스트 자동화 수준 요구
- 팀의 코드 리뷰 문화가 성숙해야 함

### GitLab Flow

```
main ──────────────────────────────────→
  │
  ├── pre-production ─────────────────→
  │         │
  └── production ────────────────────→

  feature branches → main → pre-production → production
```

**특징:**
- 환경별 브랜치(environment branches) 사용
- upstream first: 항상 main에 먼저 병합 후 하위 환경으로 전파
- 릴리스 브랜치 대신 환경 브랜치로 배포 관리

## 4. 실전 예제

### 예제 1: GitHub Flow + CI/CD 워크플로우

```yaml
# .github/workflows/ci-cd.yml
name: CI/CD Pipeline

on:
  pull_request:
    branches: [main]
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run Tests
        run: |
          npm install
          npm test

  deploy:
    needs: test
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to Production
        run: echo "Deploying..."
```

### 예제 2: Trunk-Based + Feature Flag

```java
// Feature Flag를 사용한 미완성 기능 숨김
public class PaymentService {

    private final FeatureFlagService featureFlags;

    public PaymentResult processPayment(Order order) {
        if (featureFlags.isEnabled("new-payment-gateway")) {
            // 새로운 결제 게이트웨이 (개발 중)
            return newPaymentGateway.process(order);
        }
        // 기존 결제 게이트웨이
        return legacyPaymentGateway.process(order);
    }
}
```

### 예제 3: 브랜칭 전략 선택 의사결정 트리

```
팀 규모가 10명 이상인가?
├── Yes → 릴리스 주기가 고정되어 있는가?
│         ├── Yes → Git Flow
│         └── No  → Trunk-Based Development
└── No  → 배포 빈도가 높은가? (일 1회 이상)
          ├── Yes → Trunk-Based Development
          └── No  → GitHub Flow
```

## 5. 정리

| 전략 | 브랜치 복잡도 | 배포 빈도 | CI/CD 복잡도 | 핵심 키워드 |
|------|-------------|----------|-------------|------------|
| Git Flow | 높음 | 낮음 | 높음 | 릴리스 관리, 버전 |
| GitHub Flow | 낮음 | 중~높음 | 낮음 | PR, 단순함 |
| Trunk-Based | 매우 낮음 | 매우 높음 | 낮음 | Feature Flag, 빈번한 통합 |
| GitLab Flow | 중간 | 중간 | 중간 | 환경 브랜치 |

### 핵심 원칙
1. **브랜치 수명은 짧게** — 오래된 브랜치는 통합 비용을 증가시킨다
2. **CI/CD와 일관성 유지** — 브랜칭 전략과 파이프라인 트리거가 일치해야 한다
3. **팀 규모와 문화에 맞게** — 가장 좋은 전략은 팀이 실천할 수 있는 전략이다
4. **점진적 전환** — Git Flow에서 Trunk-Based로의 전환은 단계적으로

---
*참고: Vincent Driessen - A Successful Git Branching Model, Trunk Based Development - trunkbaseddevelopment.com*
