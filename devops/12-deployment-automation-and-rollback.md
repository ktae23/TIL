# 배포 자동화와 롤백 전략

배포 자동화는 코드 변경부터 프로덕션 배포까지의 전 과정을 자동화하는 것이다. 성공적인 배포만큼 중요한 것이 실패 시의 롤백이며, 신뢰할 수 있는 롤백 메커니즘이 배포 자신감의 핵심이다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### 배포 자동화의 범위

```
┌─────────────────────────────────────────────────────────────┐
│                    배포 자동화 파이프라인                      │
│                                                             │
│  코드 커밋                                                   │
│    ↓                                                        │
│  CI 파이프라인 (빌드, 테스트, 이미지 생성)                    │
│    ↓                                                        │
│  아티팩트 저장소 (Docker Registry, Nexus)                     │
│    ↓                                                        │
│  배포 트리거 (자동 또는 수동 승인)                             │
│    ↓                                                        │
│  Staging 배포 → 자동 테스트                                  │
│    ↓                                                        │
│  Production 배포 → 모니터링                                  │
│    ↓                                                        │
│  [성공] 배포 완료  /  [실패] 자동 롤백                        │
└─────────────────────────────────────────────────────────────┘
```

### 롤백의 유형

| 유형 | 설명 | 속도 |
|------|------|------|
| Application Rollback | 이전 버전의 애플리케이션으로 되돌림 | 빠름 |
| Database Rollback | DB 스키마/데이터를 이전 상태로 복원 | 느리고 위험 |
| Infrastructure Rollback | 인프라 설정을 이전 상태로 복원 | 중간 |
| Configuration Rollback | 설정값을 이전 상태로 복원 | 매우 빠름 |

## 2. 왜 알아야 하는가 (Why)

### 배포 자동화의 가치

수동 배포 vs 자동 배포:

| 항목 | 수동 배포 | 자동 배포 |
|------|----------|----------|
| 배포 시간 | 1~2시간 | 5~15분 |
| 에러 빈도 | 높음 (절차 누락) | 낮음 (항상 동일) |
| 배포 빈도 | 주 1회 이하 | 하루 수회~수십회 |
| 롤백 시간 | 30분~1시간 | 1~5분 |
| 감사 추적 | 불완전 | 완벽 (로그 자동 기록) |
| 야간/휴일 배포 | 담당자 필요 | 자동 가능 |

### 롤백이 중요한 이유

**"배포보다 롤백이 더 중요하다"**

- 모든 배포는 잠재적으로 실패할 수 있다
- 빠른 롤백 = 짧은 장애 시간 = 비즈니스 영향 최소화
- 롤백 자신감이 배포 빈도를 높인다

## 3. 내부 구현 분석 (How)

### 배포 자동화 설계 원칙

#### 1. Immutable Deployment (불변 배포)

```
빌드 시점에 모든 것이 결정됨:

[소스코드] + [의존성] + [설정 템플릿]
    ↓
[Docker Image v1.2.3]  ← 불변 아티팩트
    ↓
모든 환경에서 동일한 이미지 사용:
  dev:     image: my-app:1.2.3
  staging: image: my-app:1.2.3
  prod:    image: my-app:1.2.3
```

절대 하지 말아야 할 것:
- 프로덕션 서버에서 직접 코드 수정
- 환경별로 다른 빌드 프로세스
- 배포 시점에 의존성 다운로드

#### 2. GitOps — Git을 Single Source of Truth로

```
Application Repo          Config Repo (GitOps)
┌──────────┐              ┌──────────────┐
│ src/     │  CI →  push  │ k8s/         │
│ test/    │  image       │ ├── staging/ │
│ Dockerfile│  tag         │ │   └── deployment.yaml │
└──────────┘              │ └── prod/    │
                          │     └── deployment.yaml │
                          └──────┬───────┘
                                 │
                          ArgoCD/Flux 감시
                                 │
                          Kubernetes Cluster
```

**GitOps 배포 흐름:**
1. 개발자가 Application Repo에 코드 푸시
2. CI 파이프라인이 Docker 이미지 빌드/푸시
3. Config Repo의 이미지 태그를 업데이트 (PR 생성)
4. PR 승인 → Config Repo에 머지
5. ArgoCD가 Config Repo 변경 감지
6. Kubernetes 클러스터에 자동 동기화

**GitOps의 롤백:**
```bash
# Config Repo의 이전 커밋으로 revert = 즉시 롤백
git revert HEAD
git push origin main
# → ArgoCD가 이전 상태로 자동 동기화
```

#### 3. 배포 게이트 (Deployment Gates)

```
Build → [Gate 1: Test] → [Gate 2: Security] → [Gate 3: Approval] → Deploy
         ↓ Fail              ↓ Fail              ↓ Reject
       Abort                Abort               Abort

각 Gate를 통과해야만 다음 단계로 진행
```

### 롤백 전략 상세

#### 자동 롤백 트리거

```yaml
# 자동 롤백 조건 정의
rollback_triggers:
  # 1. 배포 직후 Health Check 실패
  health_check:
    endpoint: /health
    timeout: 60s
    threshold: 3           # 3회 연속 실패 시 롤백

  # 2. 에러율 급증
  error_rate:
    metric: http_5xx_rate
    threshold: 5%          # 5xx 비율 5% 초과 시
    window: 5m             # 5분 관찰 창
    comparison: relative   # 이전 버전 대비

  # 3. 응답 시간 급증
  latency:
    metric: http_response_time_p99
    threshold: 2x          # P99가 2배 이상 증가 시
    window: 5m

  # 4. 비즈니스 메트릭 이상
  business:
    metric: order_success_rate
    threshold: -10%        # 주문 성공률 10% 이상 하락 시
```

#### Database Rollback 전략

```
방법 1: Forward-only Migration (권장)
  ─ 롤백 시에도 새로운 마이그레이션을 적용
  ─ 데이터 손실 위험 최소화

  v1.0 → v1.1 (마이그레이션: 칼럼 추가)
  v1.1 → v1.0 (새 마이그레이션: 칼럼을 nullable로 변경)

방법 2: Backward Compatible Migration
  ─ 마이그레이션이 이전/현재 버전 모두와 호환
  ─ 앱 롤백 시 DB 변경 불필요

  Step 1: 새 칼럼 추가 (기존 앱은 무시)
  Step 2: 새 앱 배포 (새 칼럼 사용)
  Step 3: (나중에) 구 칼럼 제거

방법 3: Snapshot Restore (최후의 수단)
  ─ DB 스냅샷에서 복원
  ─ 스냅샷 이후 데이터 손실 발생
  ─ 치명적 데이터 손상 시에만 사용
```

### 배포 알림과 감사

```
배포 이벤트 로그:
┌─────────────────────────────────────────────────────────────┐
│ 2024-01-15 14:30:00 | DEPLOY_START | v2.1.0 | prod | user1 │
│ 2024-01-15 14:32:00 | HEALTH_OK   | v2.1.0 | prod | auto  │
│ 2024-01-15 14:35:00 | DEPLOY_DONE | v2.1.0 | prod | auto  │
│ 2024-01-15 15:10:00 | ALERT       | error_rate > 5%       │
│ 2024-01-15 15:11:00 | ROLLBACK_START | v2.0.0 | prod      │
│ 2024-01-15 15:13:00 | ROLLBACK_DONE  | v2.0.0 | prod      │
└─────────────────────────────────────────────────────────────┘
```

## 4. 실전 예제

### 예제 1: GitHub Actions 배포 자동화 + 롤백

```yaml
name: Deploy with Rollback

on:
  push:
    branches: [main]

env:
  IMAGE: ghcr.io/${{ github.repository }}

jobs:
  build:
    runs-on: ubuntu-latest
    outputs:
      image-tag: ${{ steps.tag.outputs.tag }}
    steps:
      - uses: actions/checkout@v4

      - name: Generate tag
        id: tag
        run: echo "tag=${{ github.sha }}" >> "$GITHUB_OUTPUT"

      - name: Build and Push
        uses: docker/build-push-action@v6
        with:
          push: true
          tags: ${{ env.IMAGE }}:${{ steps.tag.outputs.tag }}

  deploy:
    needs: build
    runs-on: ubuntu-latest
    environment: production
    steps:
      - uses: actions/checkout@v4

      - name: Record current version (for rollback)
        id: current
        run: |
          CURRENT_IMAGE=$(kubectl get deployment my-app -n prod \
            -o jsonpath='{.spec.template.spec.containers[0].image}')
          echo "image=$CURRENT_IMAGE" >> "$GITHUB_OUTPUT"
          echo "Current version: $CURRENT_IMAGE"

      - name: Deploy new version
        run: |
          kubectl set image deployment/my-app \
            my-app=${{ env.IMAGE }}:${{ needs.build.outputs.image-tag }} \
            -n prod
          kubectl rollout status deployment/my-app -n prod --timeout=300s

      - name: Post-deploy verification
        id: verify
        continue-on-error: true
        run: |
          echo "Running post-deploy checks..."
          sleep 30

          # Health check
          HEALTH=$(curl -s -o /dev/null -w "%{http_code}" https://api.example.com/health)
          if [ "$HEALTH" != "200" ]; then
            echo "Health check failed: $HEALTH"
            exit 1
          fi

          # Error rate check (Prometheus query)
          ERROR_RATE=$(curl -s "http://prometheus:9090/api/v1/query" \
            --data-urlencode 'query=sum(rate(http_requests_total{status=~"5.."}[5m]))/sum(rate(http_requests_total[5m]))' \
            | jq -r '.data.result[0].value[1] // 0')

          if (( $(echo "$ERROR_RATE > 0.05" | bc -l) )); then
            echo "Error rate too high: $ERROR_RATE"
            exit 1
          fi

          echo "All checks passed!"

      - name: Rollback on failure
        if: steps.verify.outcome == 'failure'
        run: |
          echo "Rolling back to ${{ steps.current.outputs.image }}"
          kubectl set image deployment/my-app \
            my-app=${{ steps.current.outputs.image }} -n prod
          kubectl rollout status deployment/my-app -n prod --timeout=300s

          # 슬랙 알림
          curl -X POST "${{ secrets.SLACK_WEBHOOK }}" \
            -H 'Content-Type: application/json' \
            -d "{\"text\": \"ROLLBACK: Deployment ${{ github.sha }} failed. Rolled back to previous version.\"}"

          exit 1  # 워크플로우 실패로 표시
```

### 예제 2: ArgoCD GitOps 배포

```yaml
# ArgoCD Application 정의
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: my-app
  namespace: argocd
spec:
  project: default

  source:
    repoURL: https://github.com/my-org/k8s-config.git
    targetRevision: main
    path: apps/my-app/production

  destination:
    server: https://kubernetes.default.svc
    namespace: production

  syncPolicy:
    automated:
      prune: true            # Git에서 삭제된 리소스 제거
      selfHeal: true         # 수동 변경 시 자동 복원
    syncOptions:
      - CreateNamespace=true

    retry:
      limit: 3               # 실패 시 3회 재시도
      backoff:
        duration: 5s
        factor: 2
        maxDuration: 1m
```

```bash
# ArgoCD 롤백
# 방법 1: Git revert (권장 — GitOps 원칙 유지)
cd k8s-config
git revert HEAD
git push origin main
# → ArgoCD가 자동으로 이전 상태로 동기화

# 방법 2: ArgoCD CLI로 직접 롤백
argocd app history my-app
# ID  DATE                 REVISION
# 1   2024-01-15 14:00:00  abc1234
# 2   2024-01-15 15:00:00  def5678

argocd app rollback my-app 1
```

### 예제 3: 배포 자동화 스크립트 (범용)

```bash
#!/bin/bash
# deploy.sh — 범용 배포 자동화 스크립트

set -euo pipefail

# 설정
APP_NAME="my-app"
NAMESPACE="production"
DEPLOYMENT_TIMEOUT="300s"
VERIFY_WAIT="60"

# 인자 파싱
VERSION="${1:?Usage: deploy.sh <version>}"
HEALTH_URL="${2:-https://api.example.com/health}"

echo "=== Deploying ${APP_NAME} version ${VERSION} ==="

# 1. 현재 버전 기록 (롤백용)
CURRENT_IMAGE=$(kubectl get deployment "${APP_NAME}" -n "${NAMESPACE}" \
    -o jsonpath='{.spec.template.spec.containers[0].image}')
echo "Current version: ${CURRENT_IMAGE}"

# 2. 배포 실행
echo "Deploying new version..."
kubectl set image "deployment/${APP_NAME}" \
    "${APP_NAME}=my-registry/${APP_NAME}:${VERSION}" \
    -n "${NAMESPACE}"

# 3. 롤아웃 대기
echo "Waiting for rollout to complete..."
if ! kubectl rollout status "deployment/${APP_NAME}" \
    -n "${NAMESPACE}" --timeout="${DEPLOYMENT_TIMEOUT}"; then
    echo "ERROR: Rollout timed out. Initiating rollback..."
    kubectl rollout undo "deployment/${APP_NAME}" -n "${NAMESPACE}"
    exit 1
fi

# 4. 안정화 대기
echo "Waiting ${VERIFY_WAIT}s for stabilization..."
sleep "${VERIFY_WAIT}"

# 5. 검증
echo "Running post-deploy verification..."
RETRY=5
for i in $(seq 1 $RETRY); do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${HEALTH_URL}")
    if [ "$HTTP_CODE" = "200" ]; then
        echo "Health check passed (attempt ${i}/${RETRY})"
        echo ""
        echo "=== Deployment successful ==="
        echo "App: ${APP_NAME}"
        echo "Version: ${VERSION}"
        echo "Namespace: ${NAMESPACE}"
        exit 0
    fi
    echo "Health check failed with ${HTTP_CODE} (attempt ${i}/${RETRY})"
    sleep 10
done

# 6. 검증 실패 — 롤백
echo "ERROR: Post-deploy verification failed. Rolling back..."
kubectl set image "deployment/${APP_NAME}" \
    "${APP_NAME}=${CURRENT_IMAGE}" -n "${NAMESPACE}"
kubectl rollout status "deployment/${APP_NAME}" \
    -n "${NAMESPACE}" --timeout="${DEPLOYMENT_TIMEOUT}"

echo "Rolled back to: ${CURRENT_IMAGE}"
exit 1
```

## 5. 정리

| 항목 | 설명 |
|------|------|
| Immutable Deployment | 동일한 아티팩트를 모든 환경에서 사용 |
| GitOps | Git을 배포 상태의 Single Source of Truth로 |
| Deployment Gates | 각 단계를 통과해야 다음 단계 진행 |
| 자동 롤백 | Health Check, 에러율 기반 자동 판단 |
| DB 롤백 | Forward-only 또는 Backward Compatible Migration |
| 감사 추적 | 모든 배포 이벤트를 로그로 기록 |

### 배포 자동화 성숙도 모델

```
Level 0: 수동 배포 (SSH + 수동 명령)
Level 1: 스크립트 배포 (배포 스크립트 실행)
Level 2: CI/CD 파이프라인 (자동 빌드 + 수동 배포 승인)
Level 3: 자동 배포 (CI 통과 시 자동 배포)
Level 4: Progressive Delivery (Canary + 자동 분석)
Level 5: GitOps (선언적 배포 + 자동 동기화 + 자동 롤백)
```

---
*참고: Google SRE Book - Release Engineering, Argo CD Documentation, Flux CD Documentation*
