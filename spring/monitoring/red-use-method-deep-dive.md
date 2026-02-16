# RED Method와 USE Method 심화

서비스와 리소스를 체계적으로 모니터링하기 위한 두 가지 핵심 방법론인 RED Method와 USE Method의 심화 적용을 정리한다. 기존 문서에서 소개한 개념을 넘어, 실전 구현과 대시보드 설계, 장애 분석 워크플로우까지 다룬다.

## 목차

1. [두 방법론의 관계와 적용 범위](#1-두-방법론의-관계와-적용-범위)
2. [RED Method 실전 구현](#2-red-method-실전-구현)
3. [USE Method 실전 구현](#3-use-method-실전-구현)
4. [Four Golden Signals와의 관계](#4-four-golden-signals와의-관계)
5. [장애 분석 워크플로우](#5-장애-분석-워크플로우)
6. [Spring Boot 통합 대시보드](#6-spring-boot-통합-대시보드)
7. [SLI/SLO/SLA 연계](#7-slislosla-연계)

---

## 1. 두 방법론의 관계와 적용 범위

### 1.1 방법론 비교

```
                    ┌─────────────────────────────────┐
                    │        모니터링 대상              │
                    ├────────────────┬────────────────┤
                    │    서비스       │    리소스       │
                    │  (워크로드)     │  (인프라)       │
                    ├────────────────┼────────────────┤
  RED Method   ──>  │  Rate          │                │
  (Tom Wilkie)      │  Errors        │                │
                    │  Duration      │                │
                    ├────────────────┼────────────────┤
  USE Method   ──>  │                │  Utilization   │
  (Brendan Gregg)   │                │  Saturation    │
                    │                │  Errors        │
                    └────────────────┴────────────────┘

RED = "내 서비스가 잘 작동하고 있는가?" (사용자 관점)
USE = "내 인프라가 건강한가?" (시스템 관점)
```

### 1.2 언제 어떤 방법론을 사용하는가

| 상황 | 방법론 | 이유 |
|-----|--------|------|
| API 서비스 모니터링 | RED | 사용자 경험 직접 반영 |
| 데이터베이스 모니터링 | USE + RED | 리소스(USE) + 쿼리 성능(RED) |
| 메시지 큐 모니터링 | USE | 큐 깊이 = Saturation |
| 캐시 모니터링 | RED + USE | Hit Rate(RED) + Memory(USE) |
| 배치 작업 모니터링 | RED | 처리량, 실패율, 소요 시간 |
| 서버/VM 모니터링 | USE | CPU, 메모리, 디스크, 네트워크 |

---

## 2. RED Method 실전 구현

### 2.1 Rate (요청률)

```java
// Spring Boot에서 Rate 메트릭은 Micrometer가 자동 수집
// http_server_requests_seconds_count 로 자동 노출

// 커스텀 비즈니스 Rate
@Component
@RequiredArgsConstructor
public class BusinessRateMetrics {

    private final MeterRegistry registry;

    // 주문 처리율
    public void recordOrderProcessed(String type) {
        registry.counter("business.orders.processed",
            "type", type).increment();
    }

    // 메시지 처리율
    public void recordMessageProcessed(String topic) {
        registry.counter("business.messages.processed",
            "topic", topic).increment();
    }
}
```

```promql
# Rate 쿼리 패턴

# 전체 RPS (초당 요청 수)
sum(rate(http_server_requests_seconds_count{application="$app"}[5m]))

# 엔드포인트별 RPS
sum by (uri) (rate(http_server_requests_seconds_count{application="$app"}[5m]))

# 트래픽 변화 감지: 전시간 대비
sum(rate(http_server_requests_seconds_count{application="$app"}[5m]))
/ sum(rate(http_server_requests_seconds_count{application="$app"}[5m] offset 1h))
- 1

# 비즈니스 Rate: 분당 주문 수
rate(business_orders_processed_total{application="$app"}[5m]) * 60
```

### 2.2 Errors (에러율)

```java
// 에러 분류를 세분화하여 수집
@Component
@RequiredArgsConstructor
public class ErrorMetrics {

    private final MeterRegistry registry;

    public void recordError(String errorType, String service) {
        registry.counter("app.errors",
            "type", errorType,      // TIMEOUT, VALIDATION, INTERNAL, EXTERNAL
            "service", service      // 에러 발생 서비스
        ).increment();
    }
}
```

```promql
# Error 쿼리 패턴

# 전체 에러율 (%)
sum(rate(http_server_requests_seconds_count{application="$app",status=~"5.."}[5m]))
/ sum(rate(http_server_requests_seconds_count{application="$app"}[5m])) * 100

# 클라이언트 에러 포함 에러율
sum(rate(http_server_requests_seconds_count{application="$app",status=~"[45].."}[5m]))
/ sum(rate(http_server_requests_seconds_count{application="$app"}[5m])) * 100

# 에러 유형별 분포
sum by (status) (
  rate(http_server_requests_seconds_count{application="$app",status=~"[45].."}[5m])
)

# 가용성 (1 - 에러율)
1 - (
  sum(rate(http_server_requests_seconds_count{application="$app",status=~"5.."}[5m]))
  / sum(rate(http_server_requests_seconds_count{application="$app"}[5m]))
)
```

### 2.3 Duration (응답 시간)

```promql
# Duration 쿼리 패턴

# 평균 응답 시간 (참고용, 알림에 부적합)
rate(http_server_requests_seconds_sum{application="$app"}[5m])
/ rate(http_server_requests_seconds_count{application="$app"}[5m])

# P50 (중앙값)
histogram_quantile(0.50,
  sum by (le) (rate(http_server_requests_seconds_bucket{application="$app"}[5m]))
)

# P95 (대부분의 사용자 경험)
histogram_quantile(0.95,
  sum by (le) (rate(http_server_requests_seconds_bucket{application="$app"}[5m]))
)

# P99 (최악의 사용자 경험)
histogram_quantile(0.99,
  sum by (le) (rate(http_server_requests_seconds_bucket{application="$app"}[5m]))
)

# 엔드포인트별 P95 (느린 엔드포인트 찾기)
histogram_quantile(0.95,
  sum by (uri, le) (rate(http_server_requests_seconds_bucket{application="$app"}[5m]))
)
```

**평균(Mean)을 피해야 하는 이유**:

```
실제 응답 시간 분포:
  요청 95건: 50ms
  요청  4건: 200ms
  요청  1건: 5000ms

  평균: 99ms  <- 정상처럼 보임
  P95:  200ms
  P99: 5000ms <- 실제 1%의 사용자는 5초 대기

=> 알림은 반드시 P95/P99 기반으로 설정
```

---

## 3. USE Method 실전 구현

### 3.1 리소스별 USE 매핑

| 리소스 | Utilization | Saturation | Errors |
|--------|------------|------------|--------|
| **CPU** | `process_cpu_usage` | Load Average, Runqueue | - |
| **Memory** | `jvm_memory_used_bytes / max` | GC Frequency, OOM 횟수 | OOM Kill |
| **Thread Pool** | `active / max` | Queue Size, Rejected | Rejected 횟수 |
| **Connection Pool** | `active / max` | Pending Requests | Timeout 횟수 |
| **Disk** | `disk_usage / total` | I/O Wait | I/O Errors |
| **Network** | Bandwidth Usage | TCP Retransmits | Packet Errors |

### 3.2 JVM 리소스 USE 메트릭

```promql
# CPU Utilization
process_cpu_usage{application="$app"}

# Memory Utilization (힙)
jvm_memory_used_bytes{application="$app",area="heap"}
/ jvm_memory_max_bytes{application="$app",area="heap"}

# Memory Saturation (GC 빈도 - 포화 징후)
rate(jvm_gc_pause_seconds_count{application="$app"}[5m])

# Memory Saturation (GC 시간 비율)
rate(jvm_gc_pause_seconds_sum{application="$app"}[1m])

# Thread Pool Utilization (Tomcat)
tomcat_threads_busy_threads{application="$app"}
/ tomcat_threads_config_max_threads{application="$app"}

# Thread Pool Saturation (큐 대기)
tomcat_threads_busy_threads{application="$app"}
== tomcat_threads_config_max_threads{application="$app"}
```

### 3.3 HikariCP Connection Pool USE

```promql
# Utilization: 활성 커넥션 비율
hikaricp_connections_active{application="$app"}
/ hikaricp_connections_max{application="$app"}

# Saturation: 대기 중인 커넥션 요청
hikaricp_connections_pending{application="$app"}

# Errors: 커넥션 타임아웃
rate(hikaricp_connections_timeout_total{application="$app"}[5m])

# 유휴 커넥션 수
hikaricp_connections_idle{application="$app"}

# 커넥션 생성 시간
hikaricp_connections_creation_seconds_sum{application="$app"}
/ hikaricp_connections_creation_seconds_count{application="$app"}
```

```java
// Connection Pool 상세 모니터링 설정
@Configuration
public class HikariMetricsConfig {

    @Bean
    public HikariDataSource dataSource(MeterRegistry registry) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://localhost:3306/mydb");
        ds.setMaximumPoolSize(20);
        ds.setMinimumIdle(5);
        ds.setConnectionTimeout(30000);  // 30초

        // 메트릭 레지스트리 설정
        ds.setMetricRegistry(registry);

        return ds;
    }
}
```

### 3.4 Kafka Consumer USE

```promql
# Utilization: 처리 속도 / 유입 속도
rate(kafka_consumer_records_consumed_total{application="$app"}[5m])

# Saturation: Consumer Lag (밀린 메시지 수)
kafka_consumer_records_lag_max{application="$app"}

# Errors: 처리 실패율
rate(kafka_consumer_fetch_manager_records_error_total{application="$app"}[5m])
```

---

## 4. Four Golden Signals와의 관계

Google SRE Book의 Four Golden Signals는 RED + USE의 핵심을 결합한 것이다.

### 4.1 매핑

```
Four Golden Signals        RED Method       USE Method
─────────────────────     ──────────────    ──────────────
Latency (지연)       <-->  Duration          -
Traffic (트래픽)     <-->  Rate              -
Errors (에러)        <-->  Errors            Errors
Saturation (포화)    <-->  -                 Saturation + Utilization
```

### 4.2 Spring Boot 서비스에 적용

```promql
# 1. Latency: P99 응답 시간
histogram_quantile(0.99,
  sum by (le) (rate(http_server_requests_seconds_bucket{application="$app"}[5m]))
)

# 2. Traffic: 초당 요청 수
sum(rate(http_server_requests_seconds_count{application="$app"}[5m]))

# 3. Errors: 에러율
sum(rate(http_server_requests_seconds_count{application="$app",status=~"5.."}[5m]))
/ sum(rate(http_server_requests_seconds_count{application="$app"}[5m]))

# 4. Saturation: 리소스 포화도 (가장 빈 리소스 기준)
max(
  hikaricp_connections_active{application="$app"}
    / hikaricp_connections_max{application="$app"},
  tomcat_threads_busy_threads{application="$app"}
    / tomcat_threads_config_max_threads{application="$app"},
  jvm_memory_used_bytes{application="$app",area="heap"}
    / jvm_memory_max_bytes{application="$app",area="heap"}
)
```

---

## 5. 장애 분석 워크플로우

### 5.1 RED -> USE 분석 흐름

```
Step 1: RED 확인 (증상 파악)
├── Error Rate 증가? ──> Step 2로
├── Latency 증가?    ──> Step 2로
└── Rate 급변?       ──> 외부 요인 확인 (DDoS, 이벤트 등)

Step 2: USE 확인 (원인 파악)
├── CPU Utilization 높음?   ──> 프로파일링 (CPU flame graph)
├── Memory Utilization 높음? ──> 힙 덤프 분석, GC 튜닝
├── Thread Pool Saturation? ──> 스레드 덤프, 블로킹 I/O 확인
├── Connection Pool Saturation? ──> DB 쿼리 분석, 커넥션 풀 확대
└── 리소스 정상?              ──> Step 3으로

Step 3: 의존성 확인
├── 하위 서비스 RED 확인
├── DB 쿼리 성능 확인
├── 외부 API 응답 시간 확인
└── 분산 추적으로 병목 Span 확인
```

### 5.2 실전 시나리오: P95 응답 시간 증가

```
1. [RED] P95 응답 시간: 500ms -> 2초 (Alert 발생)
2. [RED] 에러율: 0.5% -> 3% (함께 증가)
3. [RED] RPS: 변화 없음 (트래픽은 정상)

4. [USE] CPU: 40% (정상)
5. [USE] 메모리: 75% (정상)
6. [USE] HikariCP Active: 18/20 (포화 근접!) <-- 원인 후보
7. [USE] HikariCP Pending: 15 (대기 중!) <-- 확인

8. [분석] 느린 DB 쿼리가 커넥션을 장시간 점유
9. [Trace] 특정 엔드포인트의 DB Span이 1.5초
10. [Log] "Slow query: SELECT * FROM orders WHERE ..."

11. [대응] 인덱스 추가 / 쿼리 최적화 / 커넥션 풀 확대
```

### 5.3 대시보드에서 분석 흐름 시각화

```
┌─────────────────────────────────────────────────────────┐
│ Dashboard: Incident Investigation                        │
│                                                          │
│ 1. 무엇이 문제인가? (RED)                                 │
│ ┌──────────┐ ┌──────────┐ ┌──────────┐                  │
│ │ RPS      │ │ Error %  │ │ P95 Lat  │ <- 이 중 이상 확인│
│ └──────────┘ └──────────┘ └──────────┘                  │
│                                                          │
│ 2. 어디가 포화되었는가? (USE)                              │
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐    │
│ │ CPU      │ │ Memory   │ │ Threads  │ │ Conn Pool│    │
│ │ Usage    │ │ Usage    │ │ Active   │ │ Active   │    │
│ └──────────┘ └──────────┘ └──────────┘ └──────────┘    │
│                                                          │
│ 3. 어떤 요청이 문제인가? (상세)                            │
│ ┌─────────────────────────────────────────┐              │
│ │ Endpoint별 P95 응답 시간 (Top 10)        │              │
│ │ 최근 에러 로그                           │              │
│ └─────────────────────────────────────────┘              │
└─────────────────────────────────────────────────────────┘
```

---

## 6. Spring Boot 통합 대시보드

### 6.1 Grafana JSON 패널 구성 예시

```json
{
  "title": "Spring Boot RED/USE Overview",
  "rows": [
    {
      "title": "RED Metrics - Service Health",
      "panels": [
        { "title": "Request Rate (RPS)", "type": "stat" },
        { "title": "Error Rate (%)", "type": "stat" },
        { "title": "P95 Latency (ms)", "type": "stat" },
        { "title": "Request Rate Trend", "type": "timeseries" },
        { "title": "Error Rate Trend", "type": "timeseries" },
        { "title": "Latency Distribution", "type": "heatmap" }
      ]
    },
    {
      "title": "USE Metrics - Resource Health",
      "panels": [
        { "title": "CPU Usage", "type": "gauge" },
        { "title": "Heap Memory", "type": "gauge" },
        { "title": "Thread Pool", "type": "gauge" },
        { "title": "Connection Pool", "type": "gauge" },
        { "title": "GC Pause Time", "type": "timeseries" },
        { "title": "Connection Pool Detail", "type": "timeseries" }
      ]
    }
  ]
}
```

### 6.2 Alert Rule 설계 (RED + USE 기반)

```yaml
groups:
  # RED 기반 알림 (증상)
  - name: red-alerts
    rules:
      - alert: HighErrorRate
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
          / sum(rate(http_server_requests_seconds_count[5m])) > 0.01
        for: 5m
        labels:
          severity: critical
          method: RED

      - alert: HighLatency
        expr: |
          histogram_quantile(0.95,
            sum by (le) (rate(http_server_requests_seconds_bucket[5m]))
          ) > 1
        for: 10m
        labels:
          severity: warning
          method: RED

  # USE 기반 알림 (원인)
  - name: use-alerts
    rules:
      - alert: ConnectionPoolSaturation
        expr: |
          hikaricp_connections_pending > 0
        for: 5m
        labels:
          severity: warning
          method: USE
        annotations:
          summary: "DB 커넥션 풀 포화"
          action: "느린 쿼리 확인, 커넥션 풀 크기 조정 검토"

      - alert: ThreadPoolSaturation
        expr: |
          tomcat_threads_busy_threads
          / tomcat_threads_config_max_threads > 0.9
        for: 5m
        labels:
          severity: warning
          method: USE

      - alert: HighHeapUsage
        expr: |
          jvm_memory_used_bytes{area="heap"}
          / jvm_memory_max_bytes{area="heap"} > 0.85
        for: 15m
        labels:
          severity: warning
          method: USE
```

---

## 7. SLI/SLO/SLA 연계

### 7.1 용어 정의

| 용어 | 정의 | 예시 |
|-----|------|------|
| **SLI** (Service Level Indicator) | 서비스 품질 측정 지표 | P95 응답 시간, 에러율 |
| **SLO** (Service Level Objective) | SLI의 목표 수치 | P95 < 500ms, 에러율 < 0.1% |
| **SLA** (Service Level Agreement) | 고객과의 계약 | 99.9% 가용성, 위반 시 크레딧 |
| **Error Budget** | SLO 위반 허용 범위 | 30일 중 43.2분 다운타임 허용 |

### 7.2 RED 기반 SLI/SLO

```promql
# SLI 1: 가용성 (에러율 기반)
# SLO: 99.9% (30일 기준 에러 버짓 = 0.1%)
1 - (
  sum(rate(http_server_requests_seconds_count{status=~"5.."}[30d]))
  / sum(rate(http_server_requests_seconds_count[30d]))
)

# SLI 2: 응답 시간 (지연 기반)
# SLO: 요청의 99%가 500ms 이내 응답
sum(rate(http_server_requests_seconds_bucket{le="0.5"}[30d]))
/ sum(rate(http_server_requests_seconds_count[30d]))

# Error Budget 남은 비율
(
  0.001 - (
    sum(increase(http_server_requests_seconds_count{status=~"5.."}[30d]))
    / sum(increase(http_server_requests_seconds_count[30d]))
  )
) / 0.001 * 100
```

### 7.3 Error Budget 기반 의사결정

```
Error Budget > 50%:  기능 개발 우선
Error Budget 20~50%: 기능 + 안정성 균형
Error Budget < 20%:  안정성 작업 우선 (tech debt, 최적화)
Error Budget = 0%:   기능 개발 동결, 안정성 집중
```

---

## 요약

| 방법론 | 적용 대상 | 핵심 지표 | 목적 |
|--------|---------|----------|------|
| **RED** | 서비스 (워크로드) | Rate, Errors, Duration | 사용자 경험 파악 |
| **USE** | 리소스 (인프라) | Utilization, Saturation, Errors | 병목/포화 감지 |
| **Golden Signals** | 종합 | Latency, Traffic, Errors, Saturation | SRE 표준 모니터링 |

**장애 분석 순서**: RED(증상) -> USE(원인) -> Traces(위치) -> Logs(상세)

*마지막 업데이트: 2026년 02월*
