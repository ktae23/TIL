# Alerting 전략과 Alert Fatigue 방지

효과적인 알림(Alerting) 시스템을 구축하고 Alert Fatigue를 방지하는 전략을 정리한다. 알림은 "사람의 개입이 필요한 상황"에만 발생해야 하며, 그렇지 않은 알림은 노이즈다.

## 목차

1. [Alerting 철학](#1-alerting-철학)
2. [Alert Fatigue란](#2-alert-fatigue란)
3. [효과적인 Alert Rule 설계](#3-효과적인-alert-rule-설계)
4. [Alertmanager 고급 설정](#4-alertmanager-고급-설정)
5. [On-Call 운영 전략](#5-on-call-운영-전략)
6. [Alert Fatigue 방지 체크리스트](#6-alert-fatigue-방지-체크리스트)
7. [Grafana Alerting 활용](#7-grafana-alerting-활용)

---

## 1. Alerting 철학

### 1.1 Google SRE의 알림 원칙

> "Every time the pager goes off, I should be able to react with a sense of urgency.
> I can only react with a sense of urgency a few times a day before I become fatigued."
> -- Google SRE Book

**좋은 알림의 조건**:
1. **긴급성(Urgent)**: 즉시 대응이 필요한 상황인가?
2. **실행 가능성(Actionable)**: 알림을 받고 무엇을 해야 하는지 명확한가?
3. **실제적(Real)**: 오탐(false positive)이 아닌 실제 문제인가?
4. **새로운 정보(Novel)**: 이미 알고 있는 상황의 중복 알림이 아닌가?

### 1.2 알림 vs 기록 vs 무시

```
┌──────────────────────────────────────────────────────┐
│                    이벤트 발생                         │
└──────────┬───────────────────────────────────────────┘
           │
     ┌─────▼─────┐
     │ 즉시 대응  │──Yes──> Page (PagerDuty/전화)
     │ 필요?     │         Severity: Critical
     └─────┬─────┘
           │ No
     ┌─────▼─────┐
     │ 업무시간   │──Yes──> Ticket (Jira/Slack)
     │ 내 조치?  │         Severity: Warning
     └─────┬─────┘
           │ No
     ┌─────▼─────┐
     │ 추세 분석  │──Yes──> Dashboard / Recording Rule
     │ 필요?     │         Severity: Info
     └─────┬─────┘
           │ No
           ▼
         무시 (메트릭만 수집)
```

### 1.3 Severity 정의

| Severity | 의미 | 대응 시간 | 알림 채널 | 예시 |
|---------|------|----------|----------|------|
| **Critical** | 서비스 장애, 데이터 손실 위험 | 5분 이내 | PagerDuty, 전화 | 전체 서비스 다운, 에러율 >10% |
| **Warning** | 성능 저하, 곧 장애 가능 | 업무시간 내 | Slack 채널 | 메모리 85%, P95 >2초 |
| **Info** | 참고 사항 | 다음 스프린트 | 대시보드 기록 | 디스크 60%, 트래픽 증가 추세 |

---

## 2. Alert Fatigue란

### 2.1 정의와 증상

**Alert Fatigue**: 너무 많은 알림이 발생하여 운영자가 알림에 둔감해지는 현상

**증상**:
- 알림 채널을 음소거(mute)하거나 무시
- 모든 알림을 확인 없이 승인(acknowledge)
- Critical 알림에도 느린 대응
- "늑대가 나타났다(Cry Wolf)" 효과로 실제 장애 대응 지연

### 2.2 원인

| 원인 | 설명 |
|-----|------|
| **낮은 임계값** | 정상 변동 범위를 넘는 경우에도 알림 발생 |
| **짧은 for 기간** | 일시적 스파이크에도 알림 발생 |
| **중복 알림** | 같은 문제에 여러 알림이 동시 발생 |
| **미해결 알림** | 해결되지 않은 알림이 반복 발생 |
| **가치 없는 알림** | 대응할 필요 없는 정보성 알림 |
| **자동 복구 가능** | 사람 개입 없이 복구되는 상황에 알림 |

### 2.3 측정 지표

```promql
# 팀의 Alert Fatigue 수준 측정

# 1. 일별 알림 수 (10건/일 초과 시 위험)
count_over_time(ALERTS{alertstate="firing"}[24h])

# 2. 오탐 비율 (30% 초과 시 위험)
# (수동 추적 필요: 알림 발생 후 실제 대응이 필요했던 비율)

# 3. 평균 응답 시간 (점점 길어지면 fatigue 징후)

# 4. 알림 해소 시간 (5분 이내 자동 해소 비율이 높으면 임계값 재조정 필요)
```

---

## 3. 효과적인 Alert Rule 설계

### 3.1 증상 기반 알림 (Symptom-Based)

```yaml
# 좋은 예: 증상 기반 (사용자 경험에 영향)
groups:
  - name: symptom-based-alerts
    rules:
      # 사용자가 에러를 경험하고 있다
      - alert: HighErrorRate
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
          / sum(rate(http_server_requests_seconds_count[5m])) > 0.01
        for: 5m
        labels:
          severity: critical

      # 사용자가 느린 응답을 경험하고 있다
      - alert: HighLatency
        expr: |
          histogram_quantile(0.95,
            sum by (le) (rate(http_server_requests_seconds_bucket[5m]))
          ) > 1
        for: 10m
        labels:
          severity: warning
```

```yaml
# 나쁜 예: 원인 기반 (사용자 영향과 무관할 수 있음)
      # CPU가 높다고 해서 반드시 문제는 아님
      - alert: HighCpuUsage
        expr: process_cpu_usage > 0.8
        for: 1m  # 너무 짧은 for
        labels:
          severity: critical  # 과도한 severity

      # GC가 발생하는 것은 정상적인 동작
      - alert: GCOccurred
        expr: increase(jvm_gc_pause_seconds_count[1m]) > 0
        labels:
          severity: warning
```

### 3.2 멀티 윈도우 멀티 번인 (Multi-Window Multi-Burn)

Google SRE 방식의 SLO 기반 알림. Error Budget 소진 속도를 기준으로 알림한다.

```yaml
# SLO: 99.9% 가용성 (30일 기준 에러 버짓 = 0.1%)
groups:
  - name: slo-alerts
    rules:
      # 빠른 번인 (2% 소진/시간): 1분 윈도우로 빠르게 감지
      - alert: ErrorBudgetFastBurn
        expr: |
          (
            sum(rate(http_server_requests_seconds_count{status=~"5.."}[1m]))
            / sum(rate(http_server_requests_seconds_count[1m]))
          ) > 14.4 * 0.001
          and
          (
            sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
            / sum(rate(http_server_requests_seconds_count[5m]))
          ) > 14.4 * 0.001
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Error budget 빠른 소진 감지"
          description: "현재 에러율로 1시간 내 에러 버짓의 2%가 소진됩니다"
          runbook: "https://wiki.internal/runbooks/error-budget-fast-burn"

      # 느린 번인 (5% 소진/6시간): 6시간 윈도우로 서서히 소진 감지
      - alert: ErrorBudgetSlowBurn
        expr: |
          (
            sum(rate(http_server_requests_seconds_count{status=~"5.."}[30m]))
            / sum(rate(http_server_requests_seconds_count[30m]))
          ) > 1.0 * 0.001
          and
          (
            sum(rate(http_server_requests_seconds_count{status=~"5.."}[6h]))
            / sum(rate(http_server_requests_seconds_count[6h]))
          ) > 1.0 * 0.001
        for: 15m
        labels:
          severity: warning
        annotations:
          summary: "Error budget 느린 소진 감지"
          description: "현재 에러율로 6시간 내 에러 버짓의 5%가 소진됩니다"
```

### 3.3 for 기간 가이드라인

| 상황 | 권장 for 기간 | 이유 |
|-----|-------------|------|
| 서비스 완전 다운 | 1~2분 | 즉시 인지 필요 |
| 높은 에러율 (>5%) | 5분 | 일시적 스파이크 필터링 |
| P95 지연 증가 | 10분 | 배포/스케일링 중 일시적 증가 가능 |
| 메모리/디스크 높음 | 15~30분 | GC/정리 작업으로 자연 해소 가능 |
| 느린 에러 버짓 소진 | 15~30분 | 추세 확인에 충분한 시간 필요 |

---

## 4. Alertmanager 고급 설정

### 4.1 라우팅 트리와 그룹핑

```yaml
# alertmanager.yml
global:
  resolve_timeout: 5m
  slack_api_url: 'https://hooks.slack.com/services/xxx'

# 억제 규칙: Critical이 발생하면 같은 서비스의 Warning 억제
inhibit_rules:
  - source_matchers:
      - severity = "critical"
    target_matchers:
      - severity = "warning"
    equal: ['alertname', 'service']  # 같은 알림명 + 서비스에서만

route:
  receiver: 'default-slack'
  group_by: ['alertname', 'service', 'severity']
  group_wait: 30s       # 같은 그룹의 알림을 30초 동안 모아서 발송
  group_interval: 5m    # 같은 그룹에 새 알림 추가 시 5분 간격
  repeat_interval: 4h   # 미해결 알림 반복 주기

  routes:
    # Critical: PagerDuty + Slack
    - matchers:
        - severity = "critical"
      receiver: 'pagerduty-critical'
      group_wait: 10s
      repeat_interval: 1h
      continue: true  # 다음 라우트도 평가

    - matchers:
        - severity = "critical"
      receiver: 'slack-critical'

    # Warning: Slack만
    - matchers:
        - severity = "warning"
      receiver: 'slack-warning'
      repeat_interval: 12h  # 덜 자주 반복

    # 업무 시간 외: 축소 운영
    - matchers:
        - severity = "warning"
      active_time_intervals:
        - 'business-hours'
      receiver: 'slack-warning'

    # 특정 팀 라우팅
    - matchers:
        - team = "payment"
      receiver: 'payment-team-slack'

receivers:
  - name: 'default-slack'
    slack_configs:
      - channel: '#alerts-general'
        send_resolved: true

  - name: 'slack-critical'
    slack_configs:
      - channel: '#alerts-critical'
        title: '{{ .GroupLabels.alertname }} - CRITICAL'
        text: >-
          {{ range .Alerts }}
          *Alert:* {{ .Annotations.summary }}
          *Description:* {{ .Annotations.description }}
          *Runbook:* {{ .Annotations.runbook }}
          *Since:* {{ .StartsAt | since }}
          {{ end }}
        send_resolved: true

  - name: 'slack-warning'
    slack_configs:
      - channel: '#alerts-warning'
        send_resolved: true

  - name: 'pagerduty-critical'
    pagerduty_configs:
      - routing_key: '<integration-key>'
        severity: critical

  - name: 'payment-team-slack'
    slack_configs:
      - channel: '#payment-alerts'

time_intervals:
  - name: 'business-hours'
    time_intervals:
      - weekdays: ['monday:friday']
        times:
          - start_time: '09:00'
            end_time: '18:00'
        location: 'Asia/Seoul'
```

### 4.2 Silence(일시 음소거) 활용

```bash
# 계획된 유지보수 시 특정 알림 음소거
# amtool로 CLI에서 silence 생성
amtool silence add \
  --alertmanager.url=http://alertmanager:9093 \
  --author="ops-team" \
  --comment="Planned maintenance window" \
  --duration=2h \
  alertname="HighLatency" \
  service="order-service"
```

### 4.3 알림 메시지에 포함할 정보

```yaml
annotations:
  summary: "{{ $labels.service }}: 에러율 {{ $value | humanizePercentage }}"
  description: |
    서비스 {{ $labels.service }}의 에러율이 임계값을 초과했습니다.
    현재 에러율: {{ $value | humanizePercentage }}
    영향 인스턴스: {{ $labels.instance }}
  runbook: "https://wiki.internal/runbooks/high-error-rate"
  dashboard: "https://grafana.internal/d/spring-boot/{{ $labels.service }}"
  grafana_panel: "https://grafana.internal/d/spring-boot?viewPanel=error-rate&var-app={{ $labels.service }}"
```

**좋은 알림 메시지의 구성 요소**:
1. **무엇이** 문제인지 (summary)
2. **얼마나** 심각한지 (현재 값과 임계값)
3. **어디서** 발생했는지 (서비스, 인스턴스)
4. **어떻게** 대응해야 하는지 (runbook 링크)
5. **상세 분석** 바로가기 (dashboard 링크)

---

## 5. On-Call 운영 전략

### 5.1 Runbook 표준 템플릿

```markdown
# Runbook: HighErrorRate

## 개요
- 알림 조건: 5xx 에러율이 1%를 5분간 초과
- 영향: 사용자 요청 실패
- Severity: Critical

## 진단 순서
1. Grafana 대시보드 확인: [링크]
2. 에러 로그 확인: `kubectl logs -l app=order-service --tail=100`
3. 최근 배포 확인: `kubectl rollout history deployment/order-service`
4. 의존 서비스 상태 확인: [상태 페이지 링크]

## 대응 절차
### 최근 배포가 원인인 경우
```
kubectl rollout undo deployment/order-service
```

### 의존 서비스 장애인 경우
1. Circuit Breaker 상태 확인
2. Fallback 동작 확인
3. 의존 서비스 팀에 알림

### 트래픽 급증인 경우
1. HPA 상태 확인
2. 수동 스케일 아웃: `kubectl scale --replicas=5 deployment/order-service`

## 에스컬레이션
- 15분 내 미해결: 시니어 엔지니어 호출
- 30분 내 미해결: 엔지니어링 매니저 알림
```

### 5.2 Post-Incident 알림 개선

장애 후 반드시 알림 시스템을 리뷰한다:

```
1. 이 알림이 장애를 제때 감지했는가?
   -> 아니면: 새 알림 추가 또는 임계값 조정

2. 오탐이 있었는가?
   -> 있으면: for 기간 증가, 임계값 조정, 조건 정교화

3. 너무 많은 중복 알림이 발생했는가?
   -> 있으면: group_by 조정, inhibit_rules 추가

4. Runbook이 유용했는가?
   -> 아니면: 실제 대응 과정을 반영하여 업데이트

5. 자동화할 수 있는 대응이 있었는가?
   -> 있으면: 자동 복구 로직 구현 (auto-remediation)
```

---

## 6. Alert Fatigue 방지 체크리스트

### 6.1 알림 생성 시 검증

```
[ ] 이 알림은 즉시 사람의 대응이 필요한 상황인가?
[ ] 대응 절차(Runbook)가 문서화되어 있는가?
[ ] for 기간이 일시적 변동을 필터링하기에 충분한가?
[ ] 임계값이 정상 변동 범위보다 충분히 높은가?
[ ] 유사한 알림이 이미 존재하지 않는가?
[ ] severity가 적절한가? (Critical은 정말 critical한가?)
[ ] 자동 복구가 가능한 상황은 아닌가?
```

### 6.2 정기 리뷰 (월 1회)

```
[ ] 지난 30일간 발생한 알림 총 수 확인 (목표: 일 평균 <5건)
[ ] 오탐(false positive) 비율 확인 (목표: <10%)
[ ] 반복 발생하는 알림 식별 및 근본 원인 해결
[ ] 한 번도 발생하지 않은 알림 리뷰 (임계값 너무 높은가?)
[ ] 5분 이내 자동 해소된 알림 리뷰 (for 기간 조정 필요?)
[ ] 새로 추가된 알림의 효과성 평가
[ ] Silence 목록 정리 (만료되지 않은 오래된 silence)
```

### 6.3 팀 건강 지표

| 지표 | 건강 | 주의 | 위험 |
|-----|------|------|------|
| 일 평균 알림 수 | < 2건 | 2~5건 | > 5건 |
| 오탐 비율 | < 5% | 5~20% | > 20% |
| 평균 대응 시간 | < 5분 | 5~15분 | > 15분 |
| 야간 호출 빈도 | < 1회/주 | 1~3회/주 | > 3회/주 |
| Runbook 존재율 | 100% | 80~99% | < 80% |

---

## 7. Grafana Alerting 활용

### 7.1 Prometheus Alertmanager vs Grafana Alerting

| 항목 | Prometheus Alertmanager | Grafana Alerting |
|-----|----------------------|-----------------|
| **PromQL 지원** | 네이티브 | 지원 |
| **다중 데이터소스** | Prometheus만 | 모든 데이터소스 |
| **시각적 설정** | YAML 수동 | UI 기반 |
| **알림 히스토리** | 별도 구현 필요 | 내장 |
| **권장 사용** | 대규모/복잡한 환경 | 중소규모/빠른 설정 |

### 7.2 Grafana Alerting 설정 예시

Grafana 11+에서는 통합 Alerting을 사용한다:

1. **Alert Rule 생성**: 대시보드 패널 -> Alert 탭 -> Create Alert Rule
2. **Contact Point 설정**: Slack, PagerDuty, Email 등
3. **Notification Policy**: 라우팅, 그룹핑, 음소거 설정
4. **Silence**: 유지보수 윈도우 설정

---

## 요약

| 원칙 | 핵심 포인트 |
|-----|------------|
| 증상 기반 알림 | 원인이 아닌 사용자 경험 영향을 기준으로 알림 |
| SLO 기반 | Error Budget 소진 속도로 알림 우선순위 결정 |
| for 기간 | 일시적 변동을 필터링하기에 충분히 길게 |
| 중복 방지 | group_by, inhibit_rules로 알림 폭풍 방지 |
| 실행 가능성 | 모든 알림에 Runbook 필수 |
| 정기 리뷰 | 월 1회 알림 효과성 점검 |

*마지막 업데이트: 2026년 02월*
