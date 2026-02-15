# 로드 밸런서 심층 분석

## 목차
1. [로드 밸런서 개요](#로드-밸런서-개요)
2. [L4 vs L7 로드 밸런서](#l4-vs-l7-로드-밸런서)
3. [부하 분산 알고리즘](#부하-분산-알고리즘)
4. [세션 유지 (Sticky Session)](#세션-유지-sticky-session)
5. [헬스 체크](#헬스-체크)
6. [AWS ELB 종류](#aws-elb-종류)
7. [핵심 정리](#핵심-정리)

---

## 로드 밸런서 개요

로드 밸런서는 들어오는 트래픽을 여러 서버에 분산하여 가용성과 확장성을 제공합니다.

### 로드 밸런서의 역할

```
┌─────────────────────────────────────────────────────────────────┐
│                       로드 밸런서 기능                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. 부하 분산 (Load Distribution)                               │
│     └── 트래픽을 여러 서버에 균등하게 분배                       │
│                                                                  │
│  2. 고가용성 (High Availability)                                │
│     └── 서버 장애 시 자동으로 트래픽 우회                        │
│                                                                  │
│  3. 확장성 (Scalability)                                        │
│     └── 서버 추가/제거 시 자동 반영                              │
│                                                                  │
│  4. SSL 종료 (SSL Termination)                                  │
│     └── HTTPS 암호화/복호화 처리                                 │
│                                                                  │
│  5. 헬스 체크 (Health Check)                                    │
│     └── 서버 상태 모니터링 및 자동 제외                          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## L4 vs L7 로드 밸런서

### OSI 계층별 동작

```
OSI Layer          L4 LB              L7 LB
─────────────────────────────────────────────────
Layer 7 (App)         │                  ●  ← HTTP/HTTPS 내용 분석
Layer 6 (Present)     │                  │
Layer 5 (Session)     │                  │
Layer 4 (Transport)   ●  ← TCP/UDP 분석  │
Layer 3 (Network)     │                  │
Layer 2 (Data Link)   │                  │
Layer 1 (Physical)    │                  │
```

### L4 로드 밸런서 (전송 계층)

```
클라이언트 요청 (IP: 1.2.3.4, Port: 443)
                    │
                    ▼
┌─────────────────────────────────────────┐
│            L4 Load Balancer             │
│                                          │
│  분석 정보:                              │
│  - Source IP: 1.2.3.4                   │
│  - Destination IP: 10.0.0.1             │
│  - Source Port: 54321                   │
│  - Destination Port: 443                │
│  - Protocol: TCP                        │
│                                          │
│  → 패킷 레벨 라우팅 (내용 미확인)         │
└─────────────────────────────────────────┘
                    │
        ┌───────────┴───────────┐
        ▼                       ▼
   Server A                Server B
   (10.0.1.1)              (10.0.1.2)
```

### L7 로드 밸런서 (응용 계층)

```
HTTP 요청:
GET /api/users HTTP/1.1
Host: api.example.com
Cookie: session=abc123
X-User-Type: premium
                    │
                    ▼
┌─────────────────────────────────────────┐
│            L7 Load Balancer             │
│                                          │
│  분석 정보:                              │
│  - HTTP Method: GET                     │
│  - URL Path: /api/users                 │
│  - Host Header: api.example.com         │
│  - Cookies: session=abc123              │
│  - Headers: X-User-Type=premium         │
│                                          │
│  → 콘텐츠 기반 라우팅 가능                │
└─────────────────────────────────────────┘
                    │
    ┌───────────────┼───────────────┐
    ▼               ▼               ▼
 /api/*          /static/*       /admin/*
 API Server      CDN/Static      Admin Server
```

### L4 vs L7 비교

| 특성 | L4 (NLB) | L7 (ALB) |
|------|----------|----------|
| 처리 속도 | 빠름 (패킷 레벨) | 상대적으로 느림 |
| 분석 정보 | IP, Port, Protocol | HTTP 헤더, URL, 쿠키 등 |
| 라우팅 기준 | IP/Port 기반 | 콘텐츠 기반 |
| SSL 종료 | 가능 (TLS) | 가능 (HTTPS) |
| WebSocket | 지원 | 지원 |
| 지연 시간 | 매우 낮음 | 낮음 |
| 비용 | 상대적으로 저렴 | 상대적으로 비쌈 |
| 사용 사례 | TCP/UDP 서비스, 게임, IoT | HTTP API, 웹 서비스 |

---

## 부하 분산 알고리즘

### 1. Round Robin (라운드 로빈)

```
요청 순서: 1 → 2 → 3 → 4 → 5 → 6

Server A: ●───●───●
Server B:   ●───●───●
Server C:     ●───●───●

요청 1 → A
요청 2 → B
요청 3 → C
요청 4 → A  (반복)
요청 5 → B
요청 6 → C
```

**장점**: 단순하고 공평한 분배
**단점**: 서버 성능 차이 미반영

### 2. Weighted Round Robin (가중치 라운드 로빈)

```java
// Nginx 설정 예시
upstream backend {
    server 10.0.1.1 weight=5;  // 50%
    server 10.0.1.2 weight=3;  // 30%
    server 10.0.1.3 weight=2;  // 20%
}

// 동작 예시 (10개 요청)
// Server A: 5개, Server B: 3개, Server C: 2개
```

### 3. Least Connections (최소 연결)

```
현재 연결 수:
Server A: 10 connections
Server B: 5 connections   ← 선택됨
Server C: 8 connections

새 요청 → Server B로 라우팅 (가장 적은 연결)
```

```java
// Spring Cloud LoadBalancer에서 커스텀 구현
@Bean
public ReactorLoadBalancer<ServiceInstance> leastConnectionsLoadBalancer(
        Environment environment,
        LoadBalancerClientFactory clientFactory) {
    String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
    return new LeastConnectionsLoadBalancer(
        clientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class),
        name
    );
}
```

### 4. IP Hash (IP 해시)

```
Client IP: 192.168.1.100
Hash(192.168.1.100) % 3 = 1 → Server B

# 같은 IP는 항상 같은 서버로 라우팅
# 세션 유지에 유용
```

```nginx
# Nginx 설정
upstream backend {
    ip_hash;
    server 10.0.1.1;
    server 10.0.1.2;
    server 10.0.1.3;
}
```

### 5. Least Response Time (최소 응답 시간)

```
서버별 평균 응답 시간:
Server A: 50ms
Server B: 30ms   ← 선택됨
Server C: 45ms

새 요청 → Server B로 라우팅 (가장 빠른 응답)
```

### 알고리즘 선택 가이드

| 알고리즘 | 사용 사례 |
|----------|----------|
| Round Robin | 동일 스펙 서버, 무상태 서비스 |
| Weighted RR | 서버 성능 차이가 있는 경우 |
| Least Connections | 요청 처리 시간이 불균일한 경우 |
| IP Hash | 세션 유지가 필요한 경우 |
| Least Response Time | 응답 시간 최적화가 중요한 경우 |

---

## 세션 유지 (Sticky Session)

### 세션 유지가 필요한 경우

```
❌ 문제 상황 (세션 유지 없음):

요청 1: 로그인 → Server A (세션 저장)
요청 2: 대시보드 → Server B (세션 없음!) → 재로그인 필요

✅ 해결 (세션 유지):

요청 1: 로그인 → Server A (세션 저장)
요청 2: 대시보드 → Server A (동일 서버) → 정상 동작
```

### 세션 유지 방식

#### 1. 쿠키 기반 (ALB)

```
# AWS ALB에서 생성하는 쿠키
Set-Cookie: AWSALB=abc123; Expires=...; Path=/

# 애플리케이션 쿠키 사용
Set-Cookie: JSESSIONID=xyz789; Path=/
```

#### 2. Source IP 기반 (NLB)

```
# 클라이언트 IP를 해싱하여 동일 서버 선택
# NAT 뒤의 클라이언트들은 같은 서버로 몰릴 수 있음
```

### 세션 유지의 대안: 분산 세션

```java
// Redis를 사용한 분산 세션 - 권장 방식
@Configuration
@EnableRedisHttpSession
public class SessionConfig {

    @Bean
    public LettuceConnectionFactory connectionFactory() {
        return new LettuceConnectionFactory(
            new RedisStandaloneConfiguration("redis.example.com", 6379)
        );
    }
}

// application.yml
spring:
  session:
    store-type: redis
    redis:
      namespace: spring:session
    timeout: 30m
```

```
분산 세션 아키텍처:

Client → LB → Server A ─┐
              Server B ─┼──→ Redis (세션 저장소)
              Server C ─┘

어떤 서버로 요청이 가도 동일한 세션 접근 가능
```

---

## 헬스 체크

### 헬스 체크 유형

```
┌─────────────────────────────────────────────────────────────────┐
│                        헬스 체크 유형                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. TCP 헬스 체크                                               │
│     - 포트 연결 가능 여부만 확인                                 │
│     - 빠르지만 애플리케이션 상태 미확인                          │
│                                                                  │
│  2. HTTP 헬스 체크                                              │
│     - 특정 경로로 요청, 상태 코드 확인                           │
│     - 애플리케이션 레벨 상태 확인 가능                           │
│                                                                  │
│  3. HTTPS 헬스 체크                                             │
│     - SSL 인증서 유효성까지 확인                                 │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Spring Boot Actuator 헬스 체크

```java
// build.gradle
implementation 'org.springframework.boot:spring-boot-starter-actuator'

// application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always
      group:
        readiness:
          include: db,redis,kafka
        liveness:
          include: ping

// 커스텀 헬스 인디케이터
@Component
public class ExternalServiceHealthIndicator implements HealthIndicator {

    private final ExternalServiceClient client;

    @Override
    public Health health() {
        try {
            boolean isHealthy = client.ping();
            if (isHealthy) {
                return Health.up()
                    .withDetail("service", "external-api")
                    .withDetail("status", "reachable")
                    .build();
            }
            return Health.down()
                .withDetail("service", "external-api")
                .withDetail("error", "ping failed")
                .build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
```

### ALB 타겟 그룹 헬스 체크 설정

```hcl
# Terraform
resource "aws_lb_target_group" "api" {
  name     = "api-target-group"
  port     = 8080
  protocol = "HTTP"
  vpc_id   = aws_vpc.main.id

  health_check {
    enabled             = true
    healthy_threshold   = 2      # 정상 판정 연속 성공 횟수
    unhealthy_threshold = 3      # 비정상 판정 연속 실패 횟수
    timeout             = 5      # 응답 대기 시간 (초)
    interval            = 30     # 체크 간격 (초)
    path                = "/actuator/health"
    port                = "traffic-port"
    protocol            = "HTTP"
    matcher             = "200"  # 성공 상태 코드
  }

  deregistration_delay = 30  # 드레이닝 대기 시간
}
```

---

## AWS ELB 종류

### ELB 비교

| 특성 | ALB | NLB | CLB (Legacy) |
|------|-----|-----|--------------|
| 레이어 | L7 | L4 | L4/L7 |
| 프로토콜 | HTTP, HTTPS, gRPC | TCP, UDP, TLS | TCP, SSL, HTTP, HTTPS |
| 라우팅 | 경로, 호스트, 헤더 기반 | IP, Port 기반 | 기본 라우팅 |
| WebSocket | 지원 | 지원 | 미지원 |
| 고정 IP | 미지원 | 지원 | 미지원 |
| 지연 시간 | ms 단위 | μs 단위 | ms 단위 |
| 비용 | LCU 기반 | NLCU 기반 | 시간 + 데이터 |

### ALB 경로 기반 라우팅

```hcl
# /api/* → API 서버
resource "aws_lb_listener_rule" "api" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 100

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.api.arn
  }

  condition {
    path_pattern {
      values = ["/api/*"]
    }
  }
}

# /static/* → Static 서버
resource "aws_lb_listener_rule" "static" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 200

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.static.arn
  }

  condition {
    path_pattern {
      values = ["/static/*"]
    }
  }
}

# 호스트 헤더 기반 라우팅
resource "aws_lb_listener_rule" "admin" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 50

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.admin.arn
  }

  condition {
    host_header {
      values = ["admin.example.com"]
    }
  }
}
```

---

## 핵심 정리

### L4 vs L7 선택 기준

| 요구사항 | L4 (NLB) | L7 (ALB) |
|----------|----------|----------|
| 초저지연 필요 | O | X |
| 고정 IP 필요 | O | X |
| HTTP 라우팅 필요 | X | O |
| gRPC 지원 | X | O |
| TCP/UDP 서비스 | O | X |
| SSL 오프로딩 | O | O |

### 알고리즘 선택 체크리스트

```
□ 서버 스펙이 동일한가?
  → Yes: Round Robin
  → No: Weighted Round Robin

□ 요청 처리 시간이 일정한가?
  → Yes: Round Robin
  → No: Least Connections

□ 세션 유지가 필요한가?
  → Yes: IP Hash 또는 쿠키 기반 세션
  → No: 성능 기반 알고리즘

□ 응답 시간이 중요한가?
  → Yes: Least Response Time
  → No: 다른 알고리즘 고려
```

### 실무 기반 핵심 질문

1. **Q: L4와 L7 로드 밸런서의 차이점은?**
   - A: L4는 TCP/UDP 레벨에서 IP, Port 기반 라우팅. L7은 HTTP 레벨에서 URL, 헤더, 쿠키 기반 라우팅. L4가 더 빠르고, L7이 더 유연함

2. **Q: Sticky Session의 문제점과 대안은?**
   - A: 특정 서버에 부하 집중, 서버 장애 시 세션 유실. 대안으로 Redis 같은 분산 세션 저장소 사용

3. **Q: 헬스 체크 설정 시 고려사항은?**
   - A: 간격(너무 짧으면 부하), 임계값(너무 낮으면 오탐), 타임아웃(적절한 응답 대기), 엔드포인트(의존성 포함 여부)

4. **Q: ALB vs NLB 선택 기준은?**
   - A: HTTP 기반 라우팅이 필요하면 ALB, 초저지연이나 고정 IP가 필요하면 NLB. 일반적인 웹 서비스는 ALB 권장

---

*마지막 업데이트: 2026년 01월*
