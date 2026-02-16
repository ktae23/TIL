# Canary Deployment

Canary Deployment는 새 버전을 소수의 사용자 트래픽에 먼저 노출시키고, 주요 메트릭을 모니터링하면서 점진적으로 트래픽 비율을 확대하는 배포 전략이다. 실제 프로덕션 환경에서 리스크를 최소화하면서 새 버전을 검증할 수 있다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### Canary Deployment란?

"Canary"라는 이름은 탄광에서 유독 가스를 감지하기 위해 카나리아 새를 먼저 보낸 관행에서 유래했다. 마찬가지로, 소수의 트래픽을 새 버전에 보내어 문제가 있는지 먼저 확인한다.

```
┌──────────────────────────────────────────────────────┐
│                   Canary 배포 흐름                     │
│                                                      │
│  Step 1: Deploy canary (1-5% traffic)                │
│  ┌────────────────────────────┐  ┌───┐              │
│  │     v1 (95-99%)            │  │v2 │ ← Canary     │
│  └────────────────────────────┘  └───┘              │
│                                                      │
│  Step 2: Monitor metrics (error rate, latency, CPU)  │
│  ┌─ Metrics OK? ────┐                               │
│  │  Yes → Step 3     │                               │
│  │  No  → Rollback   │                               │
│  └───────────────────┘                               │
│                                                      │
│  Step 3: Gradually increase traffic                  │
│  5% → 10% → 25% → 50% → 75% → 100%                │
│                                                      │
│  Step 4: Complete rollout                            │
│  ┌────────────────────────────────────┐              │
│  │           v2 (100%)                 │              │
│  └────────────────────────────────────┘              │
└──────────────────────────────────────────────────────┘
```

### 핵심 구성 요소

1. **Traffic Splitting**: 트래픽을 비율로 분배하는 메커니즘
2. **Health Metrics**: 새 버전의 상태를 판단하는 지표
3. **Automated Analysis**: 메트릭 기반 자동 판단 (promote/rollback)
4. **Progressive Delivery**: 점진적 트래픽 확대 스케줄

## 2. 왜 알아야 하는가 (Why)

### Canary vs 다른 전략

| 비교 항목 | Blue-Green | Canary |
|----------|-----------|--------|
| 트래픽 전환 | 0% → 100% (한 번에) | 0% → 5% → 25% → ... → 100% |
| 영향 범위 | 모든 사용자에게 즉시 | 소수 사용자부터 점진적 |
| 프로덕션 검증 | 전환 전 별도 환경 테스트 | 실제 트래픽으로 검증 |
| 롤백 비용 | 즉시 (스위치) | 빠름 (트래픽 비율 0%로) |
| 인프라 비용 | 2x | 1.x (소규모 추가) |

### Canary가 적합한 상황

- 변경 범위가 크고 영향 예측이 어려운 릴리스
- 성능에 민감한 서비스 (응답 시간이 매출에 직결)
- 사용자 경험의 회귀를 사전에 감지하고 싶은 경우
- 글로벌 서비스에서 지역별 점진적 롤아웃이 필요한 경우

## 3. 내부 구현 분석 (How)

### 트래픽 분배 메커니즘

#### 1. Load Balancer 가중치

```
Load Balancer (Weighted Routing)
├── 95% → v1 instances (10개)
└── 5%  → v2 instances (1개)
```

#### 2. Service Mesh (Istio)

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: my-app
spec:
  hosts:
    - my-app
  http:
    - route:
        - destination:
            host: my-app
            subset: stable    # v1
          weight: 95
        - destination:
            host: my-app
            subset: canary    # v2
          weight: 5
---
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: my-app
spec:
  host: my-app
  subsets:
    - name: stable
      labels:
        version: v1
    - name: canary
      labels:
        version: v2
```

#### 3. Nginx Weighted Upstream

```nginx
upstream backend {
    server v1.internal:8080 weight=95;
    server v2.internal:8080 weight=5;
}
```

### 모니터링 메트릭

Canary 배포의 성공은 올바른 메트릭 선정에 달려 있다.

#### 핵심 메트릭 (Golden Signals)

```
┌──────────────────────────────────────────────────┐
│              Canary Health Metrics                │
│                                                  │
│  1. Error Rate (에러율)                           │
│     - HTTP 5xx 비율                               │
│     - Application 에러 로그 비율                   │
│     - 기준: v1 대비 ±0.1% 이내                    │
│                                                  │
│  2. Latency (응답 시간)                           │
│     - P50, P95, P99 응답 시간                     │
│     - 기준: v1 대비 10% 이내 증가                  │
│                                                  │
│  3. Throughput (처리량)                           │
│     - 초당 요청 수 (RPS)                          │
│     - 기준: v1과 유사한 수준                       │
│                                                  │
│  4. Saturation (자원 사용률)                      │
│     - CPU, Memory 사용량                          │
│     - 기준: 임계값(80%) 미만                       │
│                                                  │
│  5. Business Metrics (비즈니스 지표)              │
│     - 전환율, 장바구니 이탈률 등                    │
│     - 기준: v1 대비 통계적으로 유의한 차이 없음      │
└──────────────────────────────────────────────────┘
```

### 자동 Canary 분석 (Automated Canary Analysis)

```
┌───────────────┐     ┌──────────────┐     ┌──────────────┐
│  Prometheus   │────→│   Canary     │────→│   Decision   │
│  (Metrics)    │     │  Analyzer    │     │              │
│               │     │              │     │  - Promote   │
│  v1 metrics   │     │  비교 분석    │     │  - Rollback  │
│  v2 metrics   │     │  통계 검정    │     │  - Continue  │
└───────────────┘     └──────────────┘     └──────────────┘
```

**분석 알고리즘:**
1. v1과 v2의 동일 시간대 메트릭을 수집
2. 각 메트릭을 통계적으로 비교 (Mann-Whitney U test 등)
3. 모든 메트릭이 기준을 통과하면 다음 단계로 진행
4. 하나라도 실패하면 롤백 실행

### Progressive Delivery 스케줄

```yaml
# Canary 진행 스케줄 예시
canary_steps:
  - weight: 5      # 5% 트래픽
    pause: 5m       # 5분 관찰
    analysis: true   # 자동 분석

  - weight: 10     # 10% 트래픽
    pause: 10m
    analysis: true

  - weight: 25     # 25% 트래픽
    pause: 15m
    analysis: true

  - weight: 50     # 50% 트래픽
    pause: 15m
    analysis: true

  - weight: 75     # 75% 트래픽
    pause: 10m
    analysis: true

  - weight: 100    # 100% 트래픽 → 완료
```

## 4. 실전 예제

### 예제 1: Argo Rollouts를 활용한 Canary

```yaml
# canary-rollout.yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: my-app
spec:
  replicas: 10
  selector:
    matchLabels:
      app: my-app
  template:
    metadata:
      labels:
        app: my-app
    spec:
      containers:
        - name: my-app
          image: my-app:1.1.0
          ports:
            - containerPort: 8080
  strategy:
    canary:
      # Canary 스텝 정의
      steps:
        - setWeight: 5
        - pause: { duration: 5m }
        - analysis:
            templates:
              - templateName: canary-analysis
        - setWeight: 20
        - pause: { duration: 10m }
        - analysis:
            templates:
              - templateName: canary-analysis
        - setWeight: 50
        - pause: { duration: 15m }
        - analysis:
            templates:
              - templateName: canary-analysis
        - setWeight: 80
        - pause: { duration: 10m }

      # 트래픽 관리 (Istio 연동)
      trafficRouting:
        istio:
          virtualService:
            name: my-app-vsvc
            routes:
              - primary

      # 안티 어피니티 — Canary Pod 분산
      antiAffinity:
        preferredDuringSchedulingIgnoredDuringExecution:
          weight: 100
---
# 분석 템플릿
apiVersion: argoproj.io/v1alpha1
kind: AnalysisTemplate
metadata:
  name: canary-analysis
spec:
  metrics:
    - name: error-rate
      interval: 1m
      count: 5
      successCondition: result[0] < 0.01  # 에러율 1% 미만
      failureLimit: 2
      provider:
        prometheus:
          address: http://prometheus:9090
          query: |
            sum(rate(http_requests_total{status=~"5..",app="my-app",version="canary"}[5m]))
            /
            sum(rate(http_requests_total{app="my-app",version="canary"}[5m]))

    - name: latency-p99
      interval: 1m
      count: 5
      successCondition: result[0] < 500  # P99 500ms 미만
      failureLimit: 2
      provider:
        prometheus:
          address: http://prometheus:9090
          query: |
            histogram_quantile(0.99,
              sum(rate(http_request_duration_seconds_bucket{app="my-app",version="canary"}[5m]))
              by (le)
            ) * 1000
```

### 예제 2: GitHub Actions + Kubernetes Canary

```yaml
name: Canary Deployment

on:
  workflow_dispatch:
    inputs:
      version:
        description: 'Image tag to deploy'
        required: true

jobs:
  canary-deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Configure kubectl
        uses: azure/setup-kubectl@v4

      - name: Deploy Canary (5%)
        run: |
          # Canary Deployment 생성
          kubectl set image deployment/my-app-canary \
            my-app=my-app:${{ inputs.version }} -n production
          kubectl scale deployment/my-app-canary --replicas=1 -n production
          kubectl rollout status deployment/my-app-canary -n production

      - name: Wait and Monitor (5min)
        run: |
          echo "Monitoring canary for 5 minutes..."
          sleep 300

          # 에러율 체크
          ERROR_RATE=$(curl -s "http://prometheus:9090/api/v1/query" \
            --data-urlencode 'query=sum(rate(http_requests_total{status=~"5..",version="canary"}[5m]))/sum(rate(http_requests_total{version="canary"}[5m]))' \
            | jq -r '.data.result[0].value[1]')

          echo "Canary error rate: $ERROR_RATE"
          if (( $(echo "$ERROR_RATE > 0.01" | bc -l) )); then
            echo "Error rate too high! Rolling back..."
            kubectl scale deployment/my-app-canary --replicas=0 -n production
            exit 1
          fi

      - name: Increase to 25%
        run: |
          kubectl scale deployment/my-app-canary --replicas=3 -n production
          echo "Canary at 25%. Monitoring for 10 minutes..."
          sleep 600

      - name: Full Rollout
        run: |
          # Stable deployment 업데이트
          kubectl set image deployment/my-app-stable \
            my-app=my-app:${{ inputs.version }} -n production
          kubectl rollout status deployment/my-app-stable -n production

          # Canary deployment 스케일 다운
          kubectl scale deployment/my-app-canary --replicas=0 -n production
          echo "Canary promotion complete!"
```

### 예제 3: 헤더 기반 Canary (내부 테스트용)

```yaml
# Istio VirtualService — 특정 헤더가 있으면 Canary로 라우팅
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: my-app
spec:
  hosts:
    - my-app
  http:
    # 내부 테스터: 헤더로 Canary 강제 라우팅
    - match:
        - headers:
            x-canary:
              exact: "true"
      route:
        - destination:
            host: my-app
            subset: canary

    # 일반 사용자: 가중치 기반
    - route:
        - destination:
            host: my-app
            subset: stable
          weight: 95
        - destination:
            host: my-app
            subset: canary
          weight: 5
```

```bash
# 내부 테스터가 Canary 버전 직접 테스트
curl -H "x-canary: true" https://app.example.com/api/v1/products
```

## 5. 정리

| 항목 | 내용 |
|------|------|
| 핵심 원리 | 소수 트래픽으로 새 버전 검증 후 점진적 확대 |
| 트래픽 분배 | Load Balancer 가중치, Service Mesh, Ingress |
| 핵심 메트릭 | Error Rate, Latency, Throughput, Saturation |
| 자동 분석 | Prometheus + Argo Rollouts AnalysisTemplate |
| 롤백 | 트래픽 비율을 0%로 설정 → 즉시 롤백 |
| 적합한 경우 | 리스크가 큰 변경, 성능 민감 서비스, 대규모 사용자 |

### Canary 배포 베스트 프랙티스

1. **작은 비율부터 시작** — 첫 단계는 1~5%로 시작
2. **충분한 관찰 시간** — 각 단계에서 최소 5~15분 관찰
3. **자동 롤백 설정** — 임계값 초과 시 자동으로 롤백
4. **비즈니스 메트릭 포함** — 기술 메트릭만으로는 부족
5. **피크 시간 배포 회피** — 트래픽이 적을 때 Canary 시작
6. **관찰 가능성(Observability) 필수** — 로그/메트릭/트레이스 분리 태깅

---
*참고: Argo Rollouts Documentation, Flagger Progressive Delivery*
