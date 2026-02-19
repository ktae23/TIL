# Prometheus Service Discovery 내부 구현

Prometheus의 Service Discovery(SD) 시스템은 Discoverer 인터페이스를 중심으로, discovery.Manager가 다양한 SD 프로바이더로부터 타겟 그룹 업데이트를 수신하여 scrape.Manager로 전달하는 파이프라인을 구현한다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

Prometheus는 스크래핑할 타겟을 자동으로 발견하는 Service Discovery 시스템을 내장하고 있다. 정적 설정(`static_configs`) 대신 Kubernetes, Consul, DNS, EC2, File 등 다양한 소스에서 동적으로 타겟 목록을 갱신한다.

### 핵심 인터페이스

```go
// discovery/discovery.go
type Discoverer interface {
    // Run은 타겟 그룹 업데이트를 채널을 통해 전달한다.
    // context가 취소되면 반환해야 한다.
    Run(ctx context.Context, up chan<- []*targetgroup.Group)
}

// discovery/discovery.go
type Config interface {
    Name() string                                    // SD 메커니즘 이름
    NewDiscoverer(DiscovererOptions) (Discoverer, error)  // Discoverer 생성
    NewDiscovererMetrics(...) DiscovererMetrics       // 메트릭 생성
}
```

### targetgroup.Group 구조

```go
// discovery/targetgroup/targetgroup.go
type Group struct {
    Targets []model.LabelSet  // 개별 타겟의 레이블 셋
    Labels  model.LabelSet    // 그룹 공통 레이블
    Source  string             // 고유 식별자
}
```

---

## 2. 왜 알아야 하는가 (Why)

1. **동적 인프라 대응**: 쿠버네티스 환경에서 Pod가 생성/삭제될 때 자동으로 스크래핑 타겟이 갱신되는 원리를 이해해야 한다.
2. **커스텀 SD 개발**: 내장 SD로 지원되지 않는 인프라에 대해 커스텀 Discoverer를 구현할 수 있다.
3. **타겟 누락 디버깅**: 타겟이 발견되지 않거나 지연되는 문제를 진단하려면 SD 파이프라인의 각 단계를 이해해야 한다.
4. **relabel 최적화**: SD가 전달하는 메타 레이블(`__meta_*`)을 활용한 relabel 설정을 최적화할 수 있다.
5. **SD 메트릭 해석**: `prometheus_sd_discovered_targets`, `prometheus_sd_received_updates_total` 등의 메트릭을 정확히 해석할 수 있다.

---

## 3. 내부 구현 분석 (How)

### 3.1 전체 아키텍처

```mermaid
graph TB
    subgraph "Service Discovery Providers"
        K8S[Kubernetes SD]
        CONSUL[Consul SD]
        FILE[File SD]
        DNS[DNS SD]
        STATIC[Static Config]
    end

    subgraph "discovery.Manager"
        direction TB
        P1[Provider 1] -->|updates chan| UPD[updater goroutine]
        P2[Provider 2] -->|updates chan| UPD
        P3[Provider 3] -->|updates chan| UPD
        UPD -->|triggerSend| SEND[sender goroutine]
        SEND -->|syncCh| OUT["map[string][]*targetgroup.Group"]
    end

    K8S --> P1
    CONSUL --> P2
    FILE --> P3

    OUT -->|SyncCh()| SM[scrape.Manager]
    SM --> T1[Target 1]
    SM --> T2[Target 2]
    SM --> TN[Target N]
```

### 3.2 discovery.Manager 구조체

```go
// discovery/manager.go
type Manager struct {
    logger   *slog.Logger
    name     string
    ctx      context.Context

    // 타겟 상태 관리
    targets    map[poolKey]map[string]*targetgroup.Group
    targetsMtx sync.Mutex

    // SD 프로바이더 목록
    providers []*Provider

    // 업데이트 전달 채널
    syncCh      chan map[string][]*targetgroup.Group
    triggerSend chan struct{}

    // 업데이트 쓰로틀링 간격 (기본 5초)
    updatert time.Duration

    metrics   *Metrics
    sdMetrics map[string]DiscovererMetrics
}
```

### 3.3 Provider 구조체

```go
// discovery/manager.go
type Provider struct {
    name   string              // 프로바이더 고유 이름
    d      Discoverer          // SD 구현체
    config any                 // 원본 설정
    cancel context.CancelFunc  // 프로바이더 중지용
    done   func()              // 정리 완료 콜백

    mu      sync.RWMutex
    subs    map[string]struct{}    // 구독 중인 scrape job 이름들
    newSubs map[string]struct{}    // 설정 리로드 시 새 구독 목록
}
```

### 3.4 Manager 실행 흐름

#### Manager.Run()

```go
// discovery/manager.go
func (m *Manager) Run() error {
    go m.sender()     // 백그라운드 sender 고루틴 시작
    <-m.ctx.Done()    // context 취소까지 대기
    m.cancelDiscoverers()
    return m.ctx.Err()
}
```

#### ApplyConfig() - 설정 적용

```go
// discovery/manager.go
func (m *Manager) ApplyConfig(cfg map[string]Configs) error {
    // 1. 새 프로바이더 등록
    for name, scfg := range cfg {
        m.registerProviders(scfg, name)
    }

    // 2. 기존 프로바이더 처리
    for _, prov := range m.providers {
        if len(prov.newSubs) == 0 && prov.cancel != nil {
            // 구독자가 없어진 프로바이더 취소
            prov.cancel()
        } else if !prov.IsStarted() {
            // 새 프로바이더 시작
            m.startProvider(m.ctx, prov)
        }
    }

    // 3. 즉시 업데이트 트리거 (downstream이 최신 상태를 받도록)
    select {
    case m.triggerSend <- struct{}{}:
    default:
    }
}
```

#### startProvider() - 프로바이더 시작

```go
// discovery/manager.go
func (m *Manager) startProvider(ctx context.Context, p *Provider) {
    ctx, cancel := context.WithCancel(ctx)
    updates := make(chan []*targetgroup.Group)

    p.cancel = cancel

    go p.d.Run(ctx, updates)       // SD 구현체 실행
    go m.updater(ctx, p, updates)  // 업데이트 수신 고루틴
}
```

### 3.5 updater - 프로바이더 업데이트 처리

```go
// discovery/manager.go
func (m *Manager) updater(ctx context.Context, p *Provider, updates chan []*targetgroup.Group) {
    defer m.cleaner(p)  // 종료 시 타겟 정리

    for {
        select {
        case <-ctx.Done():
            return
        case tgs, ok := <-updates:
            m.metrics.ReceivedUpdates.Inc()

            // 모든 구독자에게 타겟 그룹 업데이트 전파
            for s := range p.subs {
                m.updateGroup(poolKey{setName: s, provider: p.name}, tgs)
            }

            // sender에게 전송 신호
            select {
            case m.triggerSend <- struct{}{}:
            default:
            }
        }
    }
}
```

### 3.6 sender - 쓰로틀링과 전송

```go
// discovery/manager.go
func (m *Manager) sender() {
    ticker := time.NewTicker(m.updatert)  // 기본 5초

    for {
        select {
        case <-m.ctx.Done():
            return
        case <-ticker.C:
            // 쓰로틀: 최소 updatert 간격으로 전송
            select {
            case <-m.triggerSend:
                select {
                case m.syncCh <- m.allGroups():
                    // scrape.Manager로 전달 성공
                }
            default:
            }
        }
    }
}
```

핵심: `updatert`(기본 5초) 쓰로틀링으로 잦은 SD 업데이트가 과도한 타겟 재로드를 유발하지 않도록 방지한다.

### 3.7 데이터 흐름 요약

```
Provider.Run(ctx, updates)
    |
    v
[updates chan] -- targetgroup.Group 전송
    |
    v
Manager.updater()
    |
    +-> updateGroup(poolKey, tgs)  -- targets map 업데이트
    +-> triggerSend <- struct{}{}  -- sender에게 신호
    |
    v
Manager.sender() -- ticker(5s) 쓰로틀링
    |
    v
syncCh <- allGroups()
    |
    v
scrape.Manager -- 타겟 목록 수신 및 scrape loop 관리
```

### 3.8 주요 SD 구현 비교

| SD 타입 | 감지 메커니즘 | 갱신 방식 | 메타 레이블 예시 |
|---------|-------------|----------|----------------|
| **Kubernetes** | Watch API (Informer) | 실시간 이벤트 | `__meta_kubernetes_pod_name` |
| **Consul** | Long Polling / Watch | 변경 시 즉시 | `__meta_consul_service` |
| **File** | fsnotify + 주기적 폴링 | 파일 변경 시 | `__meta_filepath` |
| **DNS** | 주기적 DNS 조회 | 폴링 (refresh_interval) | `__meta_dns_name` |
| **EC2** | AWS API 호출 | 폴링 (refresh_interval) | `__meta_ec2_instance_id` |

### 3.9 Kubernetes SD 상세

Kubernetes SD는 가장 복잡한 SD 구현체 중 하나로, 여러 Kubernetes 리소스를 감시한다.

```
Kubernetes SD가 감시하는 리소스:

role: node       -> Node 객체
role: pod        -> Pod 객체 (가장 많이 사용)
role: service    -> Service 객체
role: endpoints  -> Endpoints 객체
role: endpointslice -> EndpointSlice 객체
role: ingress    -> Ingress 객체
```

각 role은 Kubernetes Informer(Watch)를 사용하여 실시간으로 변경을 감지한다. 이벤트가 발생하면 `targetgroup.Group`으로 변환하여 updates 채널에 전송한다.

### 3.10 poolKey와 타겟 관리

```go
// discovery/manager.go
type poolKey struct {
    setName  string  // scrape job 이름
    provider string  // 프로바이더 이름
}

// targets 맵 구조:
// map[poolKey]map[string]*targetgroup.Group
//     |              |
//     |              +-- source(Group.Source) -> Group
//     +-- (job이름, provider이름) 복합키
```

하나의 scrape job이 여러 SD 프로바이더를 사용할 수 있고, 각 프로바이더의 결과는 poolKey로 구분되어 독립적으로 관리된다.

---

## 4. 실전 예제

### 예제 1: Kubernetes Pod SD 설정

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'kubernetes-pods'
    kubernetes_sd_configs:
      - role: pod
        namespaces:
          names:
            - default
            - production

    relabel_configs:
      # annotation prometheus.io/scrape=true 인 Pod만 스크래핑
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true

      # 커스텀 metrics path 지원
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)

      # 커스텀 port 지원
      - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
        action: replace
        regex: ([^:]+)(?::\d+)?;(\d+)
        replacement: $1:$2
        target_label: __address__

      # Pod 이름을 instance 레이블로
      - source_labels: [__meta_kubernetes_pod_name]
        action: replace
        target_label: kubernetes_pod_name
```

### 예제 2: File SD (동적 타겟 관리)

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'file-sd'
    file_sd_configs:
      - files:
          - '/etc/prometheus/targets/*.json'
        refresh_interval: 30s
```

```json
// /etc/prometheus/targets/web-servers.json
[
  {
    "targets": ["web1:9100", "web2:9100", "web3:9100"],
    "labels": {
      "env": "production",
      "role": "web",
      "datacenter": "us-east-1"
    }
  },
  {
    "targets": ["api1:9100", "api2:9100"],
    "labels": {
      "env": "production",
      "role": "api",
      "datacenter": "us-east-1"
    }
  }
]
```

### 예제 3: SD 상태 모니터링

```promql
# 발견된 타겟 수 (job별)
prometheus_sd_discovered_targets

# SD 업데이트 수신 횟수
rate(prometheus_sd_received_updates_total[5m])

# SD 업데이트 전송 횟수 (scrape.Manager로)
rate(prometheus_sd_updates_total[5m])

# SD 설정 실패 수
prometheus_sd_failed_configs

# 특정 job의 타겟 수 확인
count(up{job="kubernetes-pods"})

# scrape 실패한 타겟 확인
up{job="kubernetes-pods"} == 0
```

---

## 5. 정리

| 구성 요소 | 역할 | 소스 파일 |
|----------|------|----------|
| **Discoverer** | SD 프로바이더 인터페이스 | `discovery/discovery.go` |
| **Config** | SD 설정 및 Discoverer 팩토리 | `discovery/discovery.go` |
| **Provider** | Discoverer 인스턴스 래퍼 | `discovery/manager.go` |
| **Manager** | 모든 Provider 관리, 업데이트 병합 | `discovery/manager.go` |
| **targetgroup.Group** | 타겟 목록과 공통 레이블 | `discovery/targetgroup/` |
| **syncCh** | Manager -> scrape.Manager 전달 채널 | `discovery/manager.go` |

### SD 파이프라인 타이밍

| 단계 | 지연 | 설명 |
|------|------|------|
| SD Provider -> Manager | 실시간 ~ 수초 | Watch 기반은 즉시, 폴링 기반은 refresh_interval |
| Manager 내부 쓰로틀링 | 최대 5초 | `updatert` 기본값 |
| Manager -> scrape.Manager | 즉시 | syncCh 채널 전송 |
| scrape.Manager 타겟 반영 | 즉시 | 수신 즉시 타겟 목록 갱신 |
| **총 지연** | **~5초 ~ refresh_interval** | SD 타입에 따라 다름 |

### poolKey 매핑 구조

```
scrape_config "job-a":
  kubernetes_sd  -> poolKey{"job-a", "kubernetes/0"}
  consul_sd      -> poolKey{"job-a", "consul/1"}

scrape_config "job-b":
  file_sd        -> poolKey{"job-b", "file/2"}

=> 각 poolKey별로 독립적인 targetgroup.Group 맵 유지
=> allGroups() 호출 시 모든 poolKey의 결과를 job 이름 기준으로 병합
```

---
*참고: Prometheus v3.x, discovery 패키지 기준*
