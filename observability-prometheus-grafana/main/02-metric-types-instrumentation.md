# Metric Types & Instrumentation

Prometheus가 지원하는 Counter, Gauge, Histogram, Summary 4가지 메트릭 타입의 내부 구조와 사용 시나리오, 그리고 Exposition Format과 OpenMetrics 표준을 소스코드 수준에서 분석한다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### 4가지 메트릭 타입

Prometheus는 4가지 핵심 메트릭 타입을 정의한다. 각 타입은 서로 다른 데이터 특성을 표현하며, 잘못된 타입 선택은 의미 없는 쿼리 결과로 이어진다.

```
┌───────────────────────────────────────────────────────────┐
│                  Prometheus Metric Types                    │
│                                                            │
│  ┌─────────┐  ┌─────────┐  ┌───────────┐  ┌─────────┐   │
│  │ Counter │  │  Gauge  │  │ Histogram │  │ Summary │   │
│  │         │  │         │  │           │  │         │   │
│  │ 단조 증가│  │ 임의 변동│  │ 분포 관찰  │  │ 분위수   │   │
│  │ only    │  │ up/down │  │ + 버킷    │  │ 사전계산  │   │
│  └─────────┘  └─────────┘  └───────────┘  └─────────┘   │
│       ↑            ↑             ↑              ↑         │
│   요청 수       온도/큐      응답 시간       P50/P99      │
│   에러 수      메모리        요청 크기       지연 시간      │
└───────────────────────────────────────────────────────────┘
```

#### Counter

- **정의**: 단조 증가(monotonically increasing)하는 누적값. 재시작 시에만 0으로 리셋.
- **사용 사례**: 요청 수, 에러 수, 처리된 바이트 수
- **쿼리 패턴**: `rate()` 또는 `increase()`로 변화율을 계산하여 사용

```
# 잘못된 사용: Counter 값 자체를 보는 것 (의미 없음)
http_requests_total = 1,000,000

# 올바른 사용: rate()로 초당 변화율 계산
rate(http_requests_total[5m]) = 150  # 초당 150 요청
```

#### Gauge

- **정의**: 임의로 올라가고 내려갈 수 있는 순간값.
- **사용 사례**: 현재 온도, 메모리 사용량, 큐 크기, 활성 goroutine 수
- **쿼리 패턴**: 현재값 그대로 사용, `min_over_time()`, `max_over_time()`

#### Histogram

- **정의**: 관측값을 사전 정의된 버킷에 분류하여 분포를 표현. 서버 측에서 분위수 계산 가능.
- **사용 사례**: HTTP 응답 시간, 요청 크기
- **노출 메트릭**: `_bucket{le="..."}` (누적 카운터), `_sum`, `_count`
- **쿼리 패턴**: `histogram_quantile(0.95, rate(...[5m]))`

#### Summary

- **정의**: 클라이언트 측에서 미리 계산된 분위수를 제공. 집계(aggregation) 불가.
- **사용 사례**: 정확한 분위수가 필요하지만 서비스 간 집계가 불필요한 경우
- **노출 메트릭**: `{quantile="0.5"}`, `{quantile="0.99"}`, `_sum`, `_count`
- **한계**: 여러 인스턴스의 Summary를 합산하여 평균 분위수를 구하는 것은 통계적으로 무의미

### Histogram vs Summary 비교

| 특성 | Histogram | Summary |
|------|-----------|---------|
| 분위수 계산 위치 | 서버 (PromQL) | 클라이언트 (SDK) |
| 집계 가능 여부 | 가능 (`sum(rate(...))`) | 불가능 |
| 버킷/분위수 설정 | 사전 정의 필요 | 사전 정의 필요 |
| 정확도 | 버킷 해상도에 의존 | 설정된 분위수에서 정확 |
| 비용 | 버킷 수 × 시계열 | 분위수 수 × 시계열 |
| 권장 사용 | 대부분의 경우 권장 | 특수한 경우만 |

---

## 2. 왜 알아야 하는가 (Why)

### 잘못된 메트릭 타입 선택의 결과

1. **Gauge로 요청 수를 세면**: 프로세스 재시작 시 정보 유실, `rate()` 계산 불가
2. **Counter로 큐 크기를 측정하면**: 감소를 표현 불가, 의미 없는 데이터
3. **Summary로 여러 서비스의 P99를 구하면**: 통계적으로 잘못된 결과 (분위수는 합산 불가)

### 카디널리티(Cardinality) 관리

메트릭 타입을 이해하면 시계열 폭발(cardinality explosion)을 방지할 수 있다:

- Histogram의 버킷 10개 × 레이블 조합 100개 = **1,000개** 시계열
- 불필요한 레이블 하나 추가로 10x 카디널리티 증가 가능
- 프로덕션에서 카디널리티 폭발은 Prometheus OOM의 주요 원인

### Exposition Format 이해의 중요성

커스텀 Exporter를 작성하거나, 메트릭 디버깅 시 `/metrics` 엔드포인트의 텍스트 형식을 직접 읽어야 한다. 포맷을 모르면 파싱 오류의 원인을 파악하기 어렵다.

---

## 3. 내부 구현 분석 (How)

### Prometheus Exposition Format

Prometheus는 `/metrics` 엔드포인트에서 메트릭을 텍스트로 노출한다. 지원되는 형식은 3가지다:

```
┌──────────────────────────────────────────────────┐
│           Content-Type 기반 파서 선택              │
│                                                  │
│  text/plain                                      │
│  └──▶ PromParser (Prometheus text format)         │
│                                                  │
│  application/openmetrics-text                    │
│  └──▶ OpenMetricsParser                          │
│                                                  │
│  application/vnd.google.protobuf                 │
│  └──▶ ProtobufParser (proto3)                    │
└──────────────────────────────────────────────────┘
```

#### Prometheus Text Format 구문

```
# HELP <metric_name> <help_text>
# TYPE <metric_name> <type>
<metric_name>[{<label_name>=<label_value>,...}] <value> [<timestamp>]
```

#### 실제 노출 예시

```
# HELP http_requests_total The total number of HTTP requests.
# TYPE http_requests_total counter
http_requests_total{method="post",code="200"} 1027 1395066363000
http_requests_total{method="post",code="400"} 3 1395066363000

# HELP http_request_duration_seconds Request duration histogram.
# TYPE http_request_duration_seconds histogram
http_request_duration_seconds_bucket{le="0.005"} 24054
http_request_duration_seconds_bucket{le="0.01"} 33444
http_request_duration_seconds_bucket{le="0.025"} 100392
http_request_duration_seconds_bucket{le="0.05"} 129389
http_request_duration_seconds_bucket{le="0.1"} 133988
http_request_duration_seconds_bucket{le="+Inf"} 144320
http_request_duration_seconds_sum 53423.33
http_request_duration_seconds_count 144320

# HELP temperature_celsius Current temperature.
# TYPE temperature_celsius gauge
temperature_celsius 23.5

# HELP rpc_duration_seconds RPC duration summary.
# TYPE rpc_duration_seconds summary
rpc_duration_seconds{quantile="0.5"} 4773
rpc_duration_seconds{quantile="0.9"} 9001
rpc_duration_seconds{quantile="0.99"} 76656
rpc_duration_seconds_sum 1.7560473e+07
rpc_duration_seconds_count 2693
```

### 텍스트 파싱 구현 분석

Prometheus의 텍스트 파싱은 `model/textparse/` 패키지에서 구현된다.

#### Parser 인터페이스 (`model/textparse/interface.go`)

```go
// Parser parses samples from a byte slice of samples
// in different exposition formats.
type Parser interface {
    // Series returns the bytes of a series with a simple float64
    // as a value, the timestamp if set, and the value.
    Series() ([]byte, *int64, float64)

    // Histogram returns the bytes of a series with a sparse
    // histogram as a value.
    Histogram() ([]byte, *int64, *histogram.Histogram, *histogram.FloatHistogram)

    // Help returns the metric name and help text.
    Help() ([]byte, []byte)

    // Type returns the metric name and type.
    Type() ([]byte, model.MetricType)

    // Labels writes the labels of the current sample.
    Labels(l *labels.Labels)

    // Next advances the parser to the next sample.
    Next() (Entry, error)
}
```

#### Content-Type 기반 파서 팩토리 (`textparse.New`)

`model/textparse/interface.go:166`에서 Content-Type에 따라 적절한 파서를 반환한다:

```go
func New(b []byte, contentType string, st *labels.SymbolTable,
         opts ParserOptions) (Parser, error) {
    mediaType, err := extractMediaType(contentType, opts.FallbackContentType)

    switch mediaType {
    case "application/openmetrics-text":
        return NewOpenMetricsParser(b, st, ...), err
    case "application/vnd.google.protobuf":
        return NewProtobufParser(b, ...), err
    case "text/plain":
        return NewPromParser(b, st, ...), err
    }
}
```

#### PromParser의 lexer 구조 (`model/textparse/promparse.go`)

Prometheus text format 파서는 go-lex 기반 lexer를 사용한다:

```go
type promlexer struct {
    b     []byte  // 입력 바이트 슬라이스
    i     int     // 현재 위치
    start int     // 현재 토큰 시작 위치
    err   error
    state int     // lexer 상태
}
```

토큰 타입은 다음과 같다:

| 토큰 | 설명 |
|------|------|
| `tHelp` | `# HELP` 라인 |
| `tType` | `# TYPE` 라인 |
| `tMName` | 메트릭 이름 |
| `tLName` | 레이블 이름 |
| `tLValue` | 레이블 값 |
| `tValue` | 메트릭 값 |
| `tTimestamp` | 타임스탬프 |
| `tBraceOpen/Close` | `{` `}` |

### OpenMetrics 표준

OpenMetrics는 Prometheus Exposition Format의 표준화 버전이다 (IETF에 제출, Content-Type: `application/openmetrics-text`).

#### Prometheus Format과의 주요 차이점

| 항목 | Prometheus Format | OpenMetrics |
|------|------------------|-------------|
| EOF 마커 | 불필요 | `# EOF` 필수 |
| Counter 접미사 | `_total` 관례 | `_total` 필수 |
| Created Timestamp | 미지원 | `_created` 메트릭 지원 |
| Info/StateSet 타입 | 미지원 | 지원 |
| Exemplar | 미지원 | 지원 (`# {trace_id="..."}`) |
| Unit | 미지원 | `# UNIT` 지원 |

#### OpenMetrics 예시

```
# TYPE http_requests_total counter
# HELP http_requests_total Total HTTP requests.
# UNIT http_requests_total requests
http_requests_total{method="post"} 1027 # {trace_id="abc123"} 1.0
# TYPE temperature_celsius gauge
# HELP temperature_celsius Current temperature.
temperature_celsius 23.5
# EOF
```

### Native Histogram (Prometheus v3+)

Prometheus v3부터 Native Histogram이 도입되었다. 기존 classic histogram의 사전 정의 버킷 대신, 지수적(exponential) 스키마로 동적 버킷 경계를 사용한다.

```
Classic Histogram:        Native Histogram:
  le="0.005" → 24054       Schema: 3 (exponential)
  le="0.01"  → 33444       ZeroThreshold: 1e-128
  le="0.025" → 100392      ZeroCount: 42
  le="0.05"  → 129389      PositiveBuckets: [...]
  le="0.1"   → 133988      NegativeBuckets: [...]
  le="+Inf"  → 144320
  _sum       → 53423.33    Sum: 53423.33
  _count     → 144320      Count: 144320
```

---

## 4. 실전 예제

### 예제 1: Go 클라이언트에서 4가지 메트릭 타입 사용

```go
package main

import (
    "math/rand"
    "net/http"
    "time"

    "github.com/prometheus/client_golang/prometheus"
    "github.com/prometheus/client_golang/prometheus/promauto"
    "github.com/prometheus/client_golang/prometheus/promhttp"
)

var (
    // Counter: HTTP 요청 총 수
    httpRequestsTotal = promauto.NewCounterVec(
        prometheus.CounterOpts{
            Name: "http_requests_total",
            Help: "Total number of HTTP requests.",
        },
        []string{"method", "status"},
    )

    // Gauge: 현재 활성 연결 수
    activeConnections = promauto.NewGauge(
        prometheus.GaugeOpts{
            Name: "active_connections",
            Help: "Number of currently active connections.",
        },
    )

    // Histogram: HTTP 응답 시간 분포
    httpDuration = promauto.NewHistogramVec(
        prometheus.HistogramOpts{
            Name:    "http_request_duration_seconds",
            Help:    "HTTP request duration in seconds.",
            Buckets: prometheus.DefBuckets, // {.005, .01, .025, .05, .1, .25, .5, 1, 2.5, 5, 10}
        },
        []string{"handler"},
    )

    // Summary: RPC 응답 시간 분위수
    rpcDuration = promauto.NewSummary(
        prometheus.SummaryOpts{
            Name:       "rpc_duration_seconds",
            Help:       "RPC latency distributions.",
            Objectives: map[float64]float64{0.5: 0.05, 0.9: 0.01, 0.99: 0.001},
        },
    )
)

func handleRequest(w http.ResponseWriter, r *http.Request) {
    // Gauge: 연결 시작 시 증가, 종료 시 감소
    activeConnections.Inc()
    defer activeConnections.Dec()

    start := time.Now()

    // 비즈니스 로직 시뮬레이션
    time.Sleep(time.Duration(rand.Intn(500)) * time.Millisecond)

    duration := time.Since(start).Seconds()

    // Counter: 요청 수 증가
    httpRequestsTotal.WithLabelValues(r.Method, "200").Inc()

    // Histogram: 응답 시간 관측
    httpDuration.WithLabelValues("/api").Observe(duration)

    // Summary: RPC 지연 시간 관측
    rpcDuration.Observe(duration)

    w.WriteHeader(http.StatusOK)
}

func main() {
    http.HandleFunc("/api", handleRequest)
    http.Handle("/metrics", promhttp.Handler())
    http.ListenAndServe(":8080", nil)
}
```

### 예제 2: 커스텀 Collector로 동적 메트릭 생성

```go
package main

import (
    "github.com/prometheus/client_golang/prometheus"
)

// QueueCollector는 메시지 큐의 실시간 상태를 수집한다.
type QueueCollector struct {
    queueSize  *prometheus.Desc
    oldestMsg  *prometheus.Desc
    processedTotal *prometheus.Desc
}

func NewQueueCollector() *QueueCollector {
    return &QueueCollector{
        queueSize: prometheus.NewDesc(
            "message_queue_size",
            "Current number of messages in the queue.",
            []string{"queue_name"}, nil,
        ),
        oldestMsg: prometheus.NewDesc(
            "message_queue_oldest_msg_age_seconds",
            "Age of the oldest message in seconds.",
            []string{"queue_name"}, nil,
        ),
        processedTotal: prometheus.NewDesc(
            "message_queue_processed_total",
            "Total number of processed messages.",
            []string{"queue_name"}, nil,
        ),
    }
}

func (c *QueueCollector) Describe(ch chan<- *prometheus.Desc) {
    ch <- c.queueSize
    ch <- c.oldestMsg
    ch <- c.processedTotal
}

func (c *QueueCollector) Collect(ch chan<- prometheus.Metric) {
    // 실제로는 큐 시스템에서 조회
    queues := getQueueStats()
    for _, q := range queues {
        ch <- prometheus.MustNewConstMetric(
            c.queueSize, prometheus.GaugeValue,
            float64(q.Size), q.Name,
        )
        ch <- prometheus.MustNewConstMetric(
            c.oldestMsg, prometheus.GaugeValue,
            q.OldestAge.Seconds(), q.Name,
        )
        ch <- prometheus.MustNewConstMetric(
            c.processedTotal, prometheus.CounterValue,
            float64(q.ProcessedTotal), q.Name,
        )
    }
}
```

### 예제 3: PromQL로 각 메트릭 타입 활용

```promql
# Counter: 초당 요청률 (5분 범위)
rate(http_requests_total{job="myapp"}[5m])

# Counter: 5xx 에러 비율
sum(rate(http_requests_total{status=~"5.."}[5m]))
  /
sum(rate(http_requests_total[5m]))

# Gauge: 메모리 사용량의 5분간 최대값
max_over_time(process_resident_memory_bytes{job="myapp"}[5m])

# Histogram: P95 응답 시간
histogram_quantile(0.95,
  sum(rate(http_request_duration_seconds_bucket{job="myapp"}[5m])) by (le)
)

# Histogram: Apdex Score 계산 (Target: 300ms, Tolerating: 1.2s)
(
  sum(rate(http_request_duration_seconds_bucket{le="0.3"}[5m]))
  +
  sum(rate(http_request_duration_seconds_bucket{le="1.2"}[5m]))
)
/ 2
/ sum(rate(http_request_duration_seconds_count[5m]))
```

---

## 5. 정리

| 메트릭 타입 | 동작 | 대표 사용 사례 | 핵심 PromQL |
|-----------|------|-------------|------------|
| **Counter** | 단조 증가 | 요청 수, 에러 수 | `rate()`, `increase()` |
| **Gauge** | 자유 변동 | 온도, 메모리, 큐 크기 | 현재값, `min/max_over_time()` |
| **Histogram** | 버킷 분류 | 응답 시간, 요청 크기 | `histogram_quantile()` |
| **Summary** | 클라이언트 분위수 | 정확한 P50/P99 | 직접 읽기 (집계 불가) |

| 형식 | Content-Type | 특징 |
|------|-------------|------|
| **Prometheus Text** | `text/plain` | 가장 널리 사용, 간단한 구문 |
| **OpenMetrics** | `application/openmetrics-text` | IETF 표준화, Exemplar/Unit 지원 |
| **Protobuf** | `application/vnd.google.protobuf` | Native Histogram 지원, 효율적 |

---
*참고: Prometheus v3.2.x, client_golang v1.21.x, OpenMetrics 1.0 기준*
