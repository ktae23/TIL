# Grafana 대시보드 실전 구축 가이드

프로덕션 환경에서 효과적인 Grafana 대시보드를 설계하고 구축하는 실전 가이드를 정리한다. 단순 패널 나열이 아닌, 장애 대응과 의사결정에 즉시 활용할 수 있는 대시보드 설계 철학을 다룬다.

## 목차

1. [대시보드 설계 원칙](#1-대시보드-설계-원칙)
2. [계층별 대시보드 구조](#2-계층별-대시보드-구조)
3. [Spring Boot 서비스 대시보드](#3-spring-boot-서비스-대시보드)
4. [PromQL 실전 패널 쿼리](#4-promql-실전-패널-쿼리)
5. [Variable과 Template 활용](#5-variable과-template-활용)
6. [대시보드 as Code](#6-대시보드-as-code)
7. [시각화 유형 선택 가이드](#7-시각화-유형-선택-가이드)

---

## 1. 대시보드 설계 원칙

### 1.1 USE/RED 기반 레이아웃

대시보드는 상단에서 하단으로 "전체 상태 -> 상세 분석" 순서로 구성한다.

```
┌─────────────────────────────────────────────────────────┐
│ Row 1: Overview (한눈에 전체 상태 파악)                    │
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐    │
│ │ RPS      │ │ Error %  │ │ P95 Lat  │ │ Uptime   │    │
│ │ (Stat)   │ │ (Stat)   │ │ (Stat)   │ │ (Stat)   │    │
│ └──────────┘ └──────────┘ └──────────┘ └──────────┘    │
├─────────────────────────────────────────────────────────┤
│ Row 2: RED Metrics (서비스 관점)                          │
│ ┌─────────────────────┐ ┌─────────────────────┐        │
│ │ Request Rate        │ │ Error Rate           │        │
│ │ (Time Series)       │ │ (Time Series)        │        │
│ └─────────────────────┘ └─────────────────────┘        │
│ ┌─────────────────────┐ ┌─────────────────────┐        │
│ │ Latency p50/p95/p99 │ │ Latency Heatmap     │        │
│ │ (Time Series)       │ │ (Heatmap)           │        │
│ └─────────────────────┘ └─────────────────────┘        │
├─────────────────────────────────────────────────────────┤
│ Row 3: USE Metrics (리소스 관점)                          │
│ ┌─────────────────────┐ ┌─────────────────────┐        │
│ │ CPU / Memory        │ │ JVM Heap / GC        │        │
│ └─────────────────────┘ └─────────────────────┘        │
│ ┌─────────────────────┐ ┌─────────────────────┐        │
│ │ Thread Pool         │ │ Connection Pool      │        │
│ └─────────────────────┘ └─────────────────────┘        │
├─────────────────────────────────────────────────────────┤
│ Row 4: Details (상세 분석)                                │
│ ┌─────────────────────┐ ┌─────────────────────┐        │
│ │ Top Endpoints       │ │ Error Breakdown      │        │
│ │ (Table)             │ │ (Pie Chart)          │        │
│ └─────────────────────┘ └─────────────────────┘        │
└─────────────────────────────────────────────────────────┘
```

### 1.2 대시보드 설계 5원칙

| 원칙 | 설명 |
|-----|------|
| **5초 규칙** | 대시보드를 열고 5초 안에 서비스 상태(정상/이상)를 판단할 수 있어야 한다 |
| **Top-Down** | 전체 요약 -> 카테고리별 추세 -> 상세 분석 순서로 배치 |
| **Actionable** | 각 패널은 "이 값이 이상하면 무엇을 해야 하는가"에 대한 답이 있어야 한다 |
| **Context** | 현재 값만이 아닌, 임계값/SLO/이전 기간과의 비교가 포함되어야 한다 |
| **Owner** | 각 대시보드는 명확한 소유 팀이 있어야 하며, 정기적으로 리뷰해야 한다 |

---

## 2. 계층별 대시보드 구조

### 2.1 3-Tier 대시보드 전략

```
Level 1: Executive Dashboard (경영진/전사)
├── 서비스 전체 가용성 (SLA %)
├── 주요 비즈니스 KPI
└── 장애 현황 요약

Level 2: Service Dashboard (팀/서비스별)
├── RED Metrics (Rate, Errors, Duration)
├── USE Metrics (Utilization, Saturation, Errors)
├── 주요 의존성 상태
└── 비즈니스 메트릭

Level 3: Debug Dashboard (장애 분석)
├── 인스턴스별 상세 메트릭
├── JVM 내부 메트릭 (GC, Thread, Memory)
├── 데이터베이스 커넥션 풀 상세
└── 외부 API 호출 상세
```

### 2.2 폴더 구조

```
Grafana Folders:
├── Platform/
│   ├── Infrastructure Overview
│   ├── Kubernetes Cluster
│   └── Database Overview
├── Services/
│   ├── Order Service
│   ├── Payment Service
│   └── User Service
├── Business/
│   ├── Revenue Dashboard
│   └── User Engagement
└── On-Call/
    ├── Service Health (L1 요약)
    └── Incident Response (L3 디버그)
```

---

## 3. Spring Boot 서비스 대시보드

### 3.1 Overview Row: Stat 패널

```promql
# 현재 RPS (초당 요청 수)
sum(rate(http_server_requests_seconds_count{application="$app"}[5m]))

# 에러율 (%) - 빨간색 임계값 설정
sum(rate(http_server_requests_seconds_count{application="$app",status=~"5.."}[5m]))
/ sum(rate(http_server_requests_seconds_count{application="$app"}[5m])) * 100

# P95 응답 시간
histogram_quantile(0.95,
  sum by (le) (rate(http_server_requests_seconds_bucket{application="$app"}[5m]))
)

# 업타임 (인스턴스 수 / 전체 타겟 수)
count(up{job="$app"} == 1)
```

**Stat 패널 설정 팁**:
- Thresholds: 녹색(정상) -> 황색(주의) -> 적색(위험)
- 에러율: 0-1%(녹), 1-5%(황), 5%+(적)
- P95 지연: 0-200ms(녹), 200-500ms(황), 500ms+(적)

### 3.2 RED Metrics Row

```promql
# Request Rate by endpoint (Time Series)
sum by (uri) (rate(http_server_requests_seconds_count{application="$app"}[5m]))

# Error Rate by status code (Time Series)
sum by (status) (
  rate(http_server_requests_seconds_count{application="$app",status=~"[45].."}[5m])
)

# Latency percentiles (Time Series - 하나의 패널에 p50/p95/p99)
histogram_quantile(0.50,
  sum by (le) (rate(http_server_requests_seconds_bucket{application="$app"}[5m]))
)
# 별도 쿼리로 p95, p99 추가
```

### 3.3 JVM 상세 패널

```promql
# 힙 메모리 사용량 (영역별)
jvm_memory_used_bytes{application="$app",area="heap"}

# 힙 메모리 최대값 (기준선)
jvm_memory_max_bytes{application="$app",area="heap"}

# GC 일시정지 시간 (Young + Old)
rate(jvm_gc_pause_seconds_sum{application="$app"}[1m])

# GC 횟수
rate(jvm_gc_pause_seconds_count{application="$app"}[1m])

# 활성 스레드 수
jvm_threads_live_threads{application="$app"}

# 스레드 상태별 분포
jvm_threads_states_threads{application="$app"}

# HikariCP 커넥션 풀
hikaricp_connections_active{application="$app"}
hikaricp_connections_idle{application="$app"}
hikaricp_connections_max{application="$app"}
hikaricp_connections_pending{application="$app"}
```

---

## 4. PromQL 실전 패널 쿼리

### 4.1 Apdex 스코어 패널

Apdex(Application Performance Index)는 사용자 만족도를 0~1 사이 값으로 표현한다.

```promql
# Apdex Score (T=0.5초 기준)
# Satisfied: <= T, Tolerating: <= 4T, Frustrated: > 4T
(
  sum(rate(http_server_requests_seconds_bucket{le="0.5",application="$app"}[5m]))
  +
  sum(rate(http_server_requests_seconds_bucket{le="2.0",application="$app"}[5m]))
  -
  sum(rate(http_server_requests_seconds_bucket{le="0.5",application="$app"}[5m]))
) / 2
/ sum(rate(http_server_requests_seconds_count{application="$app"}[5m]))
```

간소화하면:

```promql
(
  sum(rate(http_server_requests_seconds_bucket{le="0.5",application="$app"}[5m]))
  + sum(rate(http_server_requests_seconds_bucket{le="2.0",application="$app"}[5m]))
) / 2
/ sum(rate(http_server_requests_seconds_count{application="$app"}[5m]))
```

### 4.2 SLO 번다운 차트

```promql
# 30일 SLO 에러 버짓 남은 비율 (SLO 99.9% 기준)
1 - (
  (
    sum(increase(http_server_requests_seconds_count{application="$app",status=~"5.."}[30d]))
    / sum(increase(http_server_requests_seconds_count{application="$app"}[30d]))
  ) / (1 - 0.999)
)
```

### 4.3 트래픽 이상 감지

```promql
# Z-score 기반 이상 감지: 평균에서 2 표준편차 이상 벗어난 경우
(
  sum(rate(http_server_requests_seconds_count{application="$app"}[5m]))
  - avg_over_time(sum(rate(http_server_requests_seconds_count{application="$app"}[5m]))[1h:5m])
)
/ stddev_over_time(sum(rate(http_server_requests_seconds_count{application="$app"}[5m]))[1h:5m])
```

---

## 5. Variable과 Template 활용

### 5.1 Variable 정의

```
# Application 선택
Name: app
Type: Query
Query: label_values(jvm_info, application)
Multi-value: true
Include All option: true

# Instance 선택
Name: instance
Type: Query
Query: label_values(jvm_info{application="$app"}, instance)
Multi-value: true

# Interval (자동 조절)
Name: interval
Type: Interval
Values: 1m, 5m, 15m, 1h
Auto: true
```

### 5.2 Ad Hoc Filters

Grafana의 Ad Hoc Filters를 활용하면 모든 패널에 동적 필터를 적용할 수 있다.

```
# Variable 설정
Name: Filters
Type: Ad hoc filters
Data source: Prometheus

# 사용자가 대시보드에서 동적으로 필터 추가 가능
# 예: method = GET, uri = /api/orders
```

### 5.3 Annotation으로 이벤트 표시

```json
// Grafana Annotation API로 배포 이벤트 기록
// CI/CD 파이프라인에서 호출
{
  "dashboardUID": "spring-boot-service",
  "time": 1700000000000,
  "tags": ["deploy", "v2.1.0"],
  "text": "Deployed version 2.1.0 to production"
}
```

```bash
# 배포 시 Grafana Annotation 생성 (CI/CD 파이프라인)
curl -X POST http://grafana:3000/api/annotations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $GRAFANA_API_KEY" \
  -d '{
    "tags": ["deploy"],
    "text": "Deploy '"$VERSION"' to '"$ENV"'"
  }'
```

---

## 6. 대시보드 as Code

### 6.1 Grafana Provisioning

```yaml
# grafana/provisioning/dashboards/dashboard.yml
apiVersion: 1

providers:
  - name: 'default'
    orgId: 1
    folder: 'Services'
    type: file
    disableDeletion: false
    editable: true
    updateIntervalSeconds: 10
    options:
      path: /var/lib/grafana/dashboards
      foldersFromFilesStructure: true
```

### 6.2 Grafonnet (Jsonnet 기반)

```jsonnet
// dashboard.jsonnet
local grafana = import 'grafonnet/grafana.libsonnet';
local dashboard = grafana.dashboard;
local row = grafana.row;
local prometheus = grafana.prometheus;
local graphPanel = grafana.graphPanel;
local statPanel = grafana.statPanel;

local appVariable = grafana.template.custom(
  'app',
  'order-service,payment-service,user-service',
  'order-service',
);

dashboard.new(
  'Spring Boot Service',
  tags=['spring-boot', 'auto-generated'],
  refresh='30s',
  time_from='now-1h',
)
.addTemplate(appVariable)
.addRow(
  row.new(title='Overview')
  .addPanel(
    statPanel.new(
      title='Request Rate',
      datasource='Prometheus',
    ).addTarget(
      prometheus.target(
        'sum(rate(http_server_requests_seconds_count{application="$app"}[5m]))',
        legendFormat='RPS',
      )
    ),
    gridPos={ x: 0, y: 0, w: 6, h: 4 },
  )
)
```

### 6.3 Terraform Provider

```hcl
# Grafana 대시보드를 Terraform으로 관리
resource "grafana_dashboard" "spring_boot_service" {
  config_json = file("dashboards/spring-boot-service.json")
  folder      = grafana_folder.services.id
}

resource "grafana_folder" "services" {
  title = "Services"
}

resource "grafana_data_source" "prometheus" {
  type = "prometheus"
  name = "Prometheus"
  url  = "http://prometheus:9090"
}
```

---

## 7. 시각화 유형 선택 가이드

### 7.1 데이터 특성별 패널 선택

| 데이터 특성 | 추천 시각화 | 예시 |
|-----------|-----------|------|
| **현재 값 하나** | Stat / Gauge | 현재 RPS, 에러율, 업타임 |
| **시간에 따른 변화** | Time Series | 트래픽 추세, 메모리 사용량 |
| **분포** | Heatmap | 응답 시간 분포 |
| **비율/구성** | Pie Chart | 에러 유형 분포, 트래픽 비율 |
| **순위/비교** | Bar Chart / Table | 느린 엔드포인트 Top N |
| **상태** | Status Map / State Timeline | 서비스 가용성 이력 |
| **로그와 연계** | Logs Panel | Loki 연동 로그 |

### 7.2 색상 활용 규칙

```
녹색 계열: 정상 상태, SLO 달성
황색 계열: 경고, 임계값 근접
적색 계열: 위험, SLO 위반, 장애
청색 계열: 정보성 데이터, 트래픽
보라색 계열: 외부 요인, 배포 이벤트
```

---

## 요약

| 항목 | 핵심 포인트 |
|-----|------------|
| 설계 원칙 | 5초 규칙, Top-Down, Actionable |
| 구조 | L1(Executive) -> L2(Service) -> L3(Debug) |
| 필수 패널 | RPS, Error Rate, P95 Latency, JVM, Connection Pool |
| Variable | Application, Instance, Interval 필수 |
| 관리 | Dashboard as Code (Provisioning, Grafonnet, Terraform) |
| 색상 | 녹(정상) / 황(주의) / 적(위험) 일관성 유지 |

*마지막 업데이트: 2026년 02월*
