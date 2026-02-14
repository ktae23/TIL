# Spring Boot + Prometheus + Grafana 모니터링 구성

Spring Boot 애플리케이션의 메트릭을 Prometheus로 수집하고 Grafana로 시각화하는 모니터링 시스템 구성 방법을 정리한다.

## 목차

1. [모니터링 아키텍처 개요](#1-모니터링-아키텍처-개요)
2. [Spring Boot 설정](#2-spring-boot-설정)
3. [Prometheus 설정](#3-prometheus-설정)
4. [Grafana 대시보드 구성](#4-grafana-대시보드-구성)
5. [주요 모니터링 지표](#5-주요-모니터링-지표)
6. [실무 적용 가이드](#6-실무-적용-가이드)
7. [알림(Alerting) 설정](#7-알림alerting-설정)

---

## 1. 모니터링 아키텍처 개요

### 전체 흐름

```
┌─────────────────┐     Pull      ┌─────────────┐    Query    ┌─────────────┐
│  Spring Boot    │◄─────────────│  Prometheus │◄───────────│   Grafana   │
│  (Micrometer)   │   /actuator  │  (TSDB)     │             │  (시각화)    │
│                 │   /prometheus│             │             │             │
└─────────────────┘              └─────────────┘             └─────────────┘
        │                              │                           │
        ▼                              ▼                           ▼
   메트릭 생성                    메트릭 저장/집계              대시보드/알림
```

### 각 컴포넌트의 역할

| 컴포넌트 | 역할 | 특징 |
|---------|------|------|
| **Micrometer** | 메트릭 수집 Facade | 벤더 중립적, 다양한 모니터링 시스템 지원 |
| **Spring Boot Actuator** | 메트릭 엔드포인트 제공 | `/actuator/prometheus` 엔드포인트 노출 |
| **Prometheus** | 시계열 데이터베이스 | Pull 방식 수집, PromQL 쿼리 언어 |
| **Grafana** | 시각화 도구 | 대시보드, 알림, 다양한 데이터소스 지원 |

---

## 2. Spring Boot 설정

### 2.1 의존성 추가

```gradle
// build.gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-registry-prometheus'
}
```

```xml
<!-- pom.xml -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
</dependencies>
```

### 2.2 application.yml 설정

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus, metrics
      base-path: /actuator
  endpoint:
    health:
      show-details: always
    prometheus:
      enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
      sla:
        http.server.requests: 100ms, 500ms, 1s, 5s
```

### 2.3 커스텀 메트릭 등록

```java
@Component
public class CustomMetricsConfig {

    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeUsers = new AtomicInteger(0);

    public CustomMetricsConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Gauge: 현재 활성 사용자 수
        Gauge.builder("app.users.active", activeUsers, AtomicInteger::get)
            .description("현재 활성 사용자 수")
            .tag("type", "online")
            .register(meterRegistry);
    }

    // Counter: 주문 처리 횟수
    public void incrementOrderCount(String status) {
        Counter.builder("app.orders.total")
            .description("총 주문 처리 횟수")
            .tag("status", status)
            .register(meterRegistry)
            .increment();
    }

    // Timer: API 응답 시간 측정
    public void recordApiLatency(String endpoint, long durationMs) {
        Timer.builder("app.api.latency")
            .description("API 응답 시간")
            .tag("endpoint", endpoint)
            .register(meterRegistry)
            .record(Duration.ofMillis(durationMs));
    }

    // Distribution Summary: 요청 크기 분포
    public void recordRequestSize(long bytes) {
        DistributionSummary.builder("app.request.size")
            .description("요청 크기 분포")
            .baseUnit("bytes")
            .register(meterRegistry)
            .record(bytes);
    }

    public void setActiveUsers(int count) {
        activeUsers.set(count);
    }
}
```

### 2.4 @Timed 어노테이션 활용

```java
@Configuration
public class TimedConfig {

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}

@Service
public class OrderService {

    @Timed(value = "order.process.time",
           description = "주문 처리 시간",
           percentiles = {0.5, 0.95, 0.99})
    public Order processOrder(OrderRequest request) {
        // 주문 처리 로직
    }
}
```

---

## 3. Prometheus 설정

### 3.1 prometheus.yml 설정

```yaml
global:
  scrape_interval: 15s      # 메트릭 수집 주기
  evaluation_interval: 15s  # 규칙 평가 주기

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

rule_files:
  - "alert_rules.yml"

scrape_configs:
  # Prometheus 자체 메트릭
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  # Spring Boot 애플리케이션
  - job_name: 'spring-boot-app'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    static_configs:
      - targets: ['app-server:8080']
        labels:
          environment: 'production'
          team: 'backend'

    # 동적 타겟 (서비스 디스커버리)
    # kubernetes_sd_configs:
    #   - role: pod

  # 여러 인스턴스 모니터링
  - job_name: 'spring-boot-cluster'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
        - 'app-server-1:8080'
        - 'app-server-2:8080'
        - 'app-server-3:8080'
```

### 3.2 Docker Compose 구성

```yaml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - ./alert_rules.yml:/etc/prometheus/alert_rules.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--storage.tsdb.retention.time=15d'
      - '--web.enable-lifecycle'
    networks:
      - monitoring

  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_USERS_ALLOW_SIGN_UP=false
    volumes:
      - grafana_data:/var/lib/grafana
      - ./grafana/provisioning:/etc/grafana/provisioning
    depends_on:
      - prometheus
    networks:
      - monitoring

  alertmanager:
    image: prom/alertmanager:latest
    container_name: alertmanager
    ports:
      - "9093:9093"
    volumes:
      - ./alertmanager.yml:/etc/alertmanager/alertmanager.yml
    networks:
      - monitoring

volumes:
  prometheus_data:
  grafana_data:

networks:
  monitoring:
    driver: bridge
```

---

## 4. Grafana 대시보드 구성

### 4.1 데이터소스 설정

Grafana에서 Prometheus 데이터소스 추가:
- URL: `http://prometheus:9090`
- Access: Server (default)
- Scrape interval: 15s

### 4.2 주요 PromQL 쿼리

```promql
# 1. HTTP 요청률 (RPS)
rate(http_server_requests_seconds_count{application="my-app"}[5m])

# 2. HTTP 요청 평균 응답 시간
rate(http_server_requests_seconds_sum{application="my-app"}[5m])
/ rate(http_server_requests_seconds_count{application="my-app"}[5m])

# 3. HTTP 요청 95 퍼센타일 응답 시간
histogram_quantile(0.95,
  rate(http_server_requests_seconds_bucket{application="my-app"}[5m])
)

# 4. 에러율 (5xx 응답 비율)
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
/ sum(rate(http_server_requests_seconds_count[5m])) * 100

# 5. JVM 힙 메모리 사용률
jvm_memory_used_bytes{area="heap"}
/ jvm_memory_max_bytes{area="heap"} * 100

# 6. GC 일시 정지 시간
increase(jvm_gc_pause_seconds_sum[1m])

# 7. 활성 스레드 수
jvm_threads_live_threads

# 8. 데이터베이스 커넥션 풀 사용률
hikaricp_connections_active
/ hikaricp_connections_max * 100

# 9. 커스텀 메트릭 - 분당 주문 수
rate(app_orders_total[1m]) * 60
```

### 4.3 추천 대시보드 템플릿

Grafana Dashboard Import에서 사용할 수 있는 추천 대시보드:

| Dashboard ID | 이름 | 설명 |
|-------------|------|------|
| **4701** | JVM (Micrometer) | JVM 메트릭 전체 |
| **6756** | Spring Boot Statistics | Spring Boot 통계 |
| **12900** | Spring Boot 2.1 Statistics | 상세 Spring Boot 메트릭 |
| **14430** | Spring Boot APM | APM 스타일 대시보드 |

---

## 5. 주요 모니터링 지표

### 5.1 RED 메서드 (Rate, Errors, Duration)

서비스 모니터링의 핵심 지표:

| 지표 | 설명 | PromQL |
|-----|------|--------|
| **Rate** | 초당 요청 수 | `rate(http_server_requests_seconds_count[5m])` |
| **Errors** | 에러 비율 | `sum(rate(...{status=~"5.."}[5m])) / sum(rate(...[5m]))` |
| **Duration** | 응답 시간 | `histogram_quantile(0.95, rate(..._bucket[5m]))` |

### 5.2 USE 메서드 (Utilization, Saturation, Errors)

리소스 모니터링의 핵심 지표:

| 지표 | 설명 | 대상 |
|-----|------|------|
| **Utilization** | 리소스 사용률 | CPU, Memory, Disk, Connection Pool |
| **Saturation** | 리소스 포화도 | Queue Length, Thread Pool Waiting |
| **Errors** | 에러 발생 수 | Connection Timeout, OOM |

### 5.3 4대 황금 신호 (Four Golden Signals)

| 신호 | 메트릭 | 임계값 예시 |
|-----|--------|------------|
| **Latency** | p95 응답 시간 | > 500ms |
| **Traffic** | 초당 요청 수 | 급격한 변화 |
| **Errors** | 에러율 | > 1% |
| **Saturation** | 리소스 사용률 | > 80% |

### 5.4 JVM 핵심 메트릭

```yaml
# 반드시 모니터링해야 할 JVM 메트릭
Memory:
  - jvm_memory_used_bytes{area="heap"}     # 힙 메모리 사용량
  - jvm_memory_max_bytes{area="heap"}      # 힙 메모리 최대값
  - jvm_buffer_memory_used_bytes           # 다이렉트 버퍼

GC:
  - jvm_gc_pause_seconds_count             # GC 횟수
  - jvm_gc_pause_seconds_sum               # GC 총 시간
  - jvm_gc_memory_promoted_bytes_total     # Old Gen 승격량

Threads:
  - jvm_threads_live_threads               # 활성 스레드 수
  - jvm_threads_peak_threads               # 피크 스레드 수
  - jvm_threads_states_threads             # 스레드 상태별 수

Classes:
  - jvm_classes_loaded_classes             # 로드된 클래스 수
```

---

## 6. 실무 적용 가이드

### 6.1 보안 설정

```yaml
# application.yml - 프로덕션 환경
management:
  endpoints:
    web:
      exposure:
        include: health, prometheus
  endpoint:
    health:
      show-details: when_authorized  # 인증된 사용자만
  server:
    port: 8081  # 별도 포트로 분리

# Spring Security 설정
@Configuration
public class ActuatorSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .requestMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeRequests()
                .requestMatchers(EndpointRequest.to("health")).permitAll()
                .requestMatchers(EndpointRequest.to("prometheus"))
                    .hasRole("MONITORING")
            .and()
            .httpBasic();
    }
}
```

### 6.2 레이블(Tag) 전략

```java
@Configuration
public class MetricsConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> commonTags() {
        return registry -> registry.config()
            .commonTags(
                "application", "order-service",
                "environment", System.getenv("ENVIRONMENT"),
                "region", System.getenv("AWS_REGION"),
                "version", BuildProperties.getVersion()
            );
    }
}
```

**레이블 설계 원칙:**
- **고정적인 값**: application, environment, version
- **동적이지만 카디널리티가 낮은 값**: status, method, endpoint
- **피해야 할 값**: userId, requestId 등 고유 식별자 (카디널리티 폭발)

### 6.3 성능 최적화

```yaml
management:
  metrics:
    distribution:
      # 히스토그램 버킷 최적화
      minimum-expected-value:
        http.server.requests: 10ms
      maximum-expected-value:
        http.server.requests: 10s

    # 불필요한 메트릭 비활성화
    enable:
      jvm.gc.memory.allocated: false
      jvm.gc.memory.promoted: false
      process.files: false
```

### 6.4 Kubernetes 환경 설정

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    metadata:
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      containers:
        - name: app
          ports:
            - containerPort: 8080
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
```

---

## 7. 알림(Alerting) 설정

### 7.1 Prometheus Alert Rules

```yaml
# alert_rules.yml
groups:
  - name: spring-boot-alerts
    rules:
      # 높은 에러율
      - alert: HighErrorRate
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
          / sum(rate(http_server_requests_seconds_count[5m])) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "높은 에러율 감지"
          description: "5xx 에러율이 5%를 초과했습니다 (현재: {{ $value | printf \"%.2f\" }}%)"

      # 느린 응답 시간
      - alert: HighLatency
        expr: |
          histogram_quantile(0.95,
            rate(http_server_requests_seconds_bucket[5m])
          ) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "응답 시간 지연"
          description: "P95 응답 시간이 1초를 초과했습니다"

      # JVM 힙 메모리 부족
      - alert: HighHeapUsage
        expr: |
          jvm_memory_used_bytes{area="heap"}
          / jvm_memory_max_bytes{area="heap"} > 0.85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "힙 메모리 사용률 높음"
          description: "힙 메모리 사용률이 85%를 초과했습니다"

      # 애플리케이션 다운
      - alert: ApplicationDown
        expr: up{job="spring-boot-app"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "애플리케이션 다운"
          description: "{{ $labels.instance }}가 응답하지 않습니다"
```

### 7.2 Alertmanager 설정

```yaml
# alertmanager.yml
global:
  resolve_timeout: 5m

route:
  group_by: ['alertname', 'severity']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h
  receiver: 'slack-notifications'
  routes:
    - match:
        severity: critical
      receiver: 'pagerduty-critical'
    - match:
        severity: warning
      receiver: 'slack-notifications'

receivers:
  - name: 'slack-notifications'
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/xxx/yyy/zzz'
        channel: '#alerts'
        title: '{{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'

  - name: 'pagerduty-critical'
    pagerduty_configs:
      - service_key: 'your-pagerduty-key'
```

---

## 요약

| 단계 | 작업 | 핵심 포인트 |
|-----|------|------------|
| 1 | Spring Boot 설정 | Actuator + Micrometer 의존성, 엔드포인트 노출 |
| 2 | 커스텀 메트릭 | Counter, Gauge, Timer, Summary 활용 |
| 3 | Prometheus 설정 | scrape_configs, 수집 주기 설정 |
| 4 | Grafana 대시보드 | PromQL 쿼리, 템플릿 활용 |
| 5 | 알림 설정 | Alert Rules, Alertmanager 연동 |
| 6 | 운영 | 보안, 레이블 전략, 성능 최적화 |

*마지막 업데이트: 2026년 01월*
