# DNS 질의 과정과 Route53

## 목차
1. [DNS 개요](#dns-개요)
2. [DNS 질의 과정](#dns-질의-과정)
3. [DNS 레코드 타입](#dns-레코드-타입)
4. [TTL과 캐싱](#ttl과-캐싱)
5. [AWS Route53](#aws-route53)
6. [핵심 정리](#핵심-정리)

---

## DNS 개요

DNS(Domain Name System)는 도메인 이름을 IP 주소로 변환하는 분산 데이터베이스 시스템입니다.

### DNS 계층 구조

```
                    ┌─────────────┐
                    │  Root DNS   │  전 세계 13개 클러스터
                    │     (.)     │  a.root-servers.net ~ m.root-servers.net
                    └──────┬──────┘
                           │
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
    ┌──────────┐    ┌──────────┐    ┌──────────┐
    │   .com   │    │   .org   │    │   .kr    │  TLD DNS
    └────┬─────┘    └────┬─────┘    └────┬─────┘
         │               │               │
    ┌────▼─────┐    ┌────▼─────┐    ┌────▼─────┐
    │ example  │    │ wikipedia│    │  naver   │  Authoritative DNS
    │   .com   │    │   .org   │    │   .kr    │
    └──────────┘    └──────────┘    └──────────┘
```

---

## DNS 질의 과정

### 전체 질의 흐름

```
사용자                Local DNS              Root DNS        TLD DNS        Auth DNS
  │                  (Resolver)                │               │               │
  │  www.example.com     │                     │               │               │
  │ ─────────────────►   │                     │               │               │
  │                      │  .com 담당 TLD?     │               │               │
  │                      │ ────────────────►   │               │               │
  │                      │  ◄──── a.gtld.net ──│               │               │
  │                      │                     │               │               │
  │                      │  example.com NS?    │               │               │
  │                      │ ────────────────────────────────►   │               │
  │                      │  ◄──── ns1.example.com ─────────────│               │
  │                      │                     │               │               │
  │                      │  www.example.com A? │               │               │
  │                      │ ────────────────────────────────────────────────►   │
  │                      │  ◄──── 93.184.216.34 ───────────────────────────────│
  │  ◄─ 93.184.216.34 ───│                     │               │               │
  │                      │                     │               │               │
```

### 재귀 질의 vs 반복 질의

```java
// 개념적 의사 코드로 이해하기
public class DNSResolver {

    // 재귀 질의 (Recursive Query): 클라이언트 → Local DNS
    // Local DNS가 최종 결과를 책임지고 반환
    public String resolveRecursive(String domain) {
        // 캐시 확인
        String cached = cache.get(domain);
        if (cached != null) {
            return cached;
        }

        // 반복 질의로 최종 IP 획득
        return resolveIterative(domain);
    }

    // 반복 질의 (Iterative Query): Local DNS → 각 DNS 서버
    // 각 서버가 다음 서버 주소만 알려줌
    public String resolveIterative(String domain) {
        // 1. Root DNS에 질의 → TLD 서버 주소 획득
        String tldServer = queryRootDNS(domain);

        // 2. TLD DNS에 질의 → Authoritative 서버 주소 획득
        String authServer = queryTLD(tldServer, domain);

        // 3. Authoritative DNS에 질의 → 최종 IP 획득
        String ip = queryAuthoritative(authServer, domain);

        return ip;
    }
}
```

### 실제 DNS 질의 확인

```bash
# dig 명령어로 DNS 질의 과정 추적
dig +trace www.example.com

# 결과 예시
# .                     518400  IN  NS  a.root-servers.net.
# com.                  172800  IN  NS  a.gtld-servers.net.
# example.com.          172800  IN  NS  ns1.example.com.
# www.example.com.      86400   IN  A   93.184.216.34

# 특정 DNS 서버에 직접 질의
dig @8.8.8.8 www.example.com

# 상세 정보 확인
dig www.example.com +noall +answer +stats
```

---

## DNS 레코드 타입

### 주요 레코드

| 레코드 | 용도 | 예시 |
|--------|------|------|
| A | IPv4 주소 매핑 | example.com → 93.184.216.34 |
| AAAA | IPv6 주소 매핑 | example.com → 2606:2800:220:1:248:1893:25c8:1946 |
| CNAME | 도메인 별칭 | www.example.com → example.com |
| MX | 메일 서버 | example.com → mail.example.com (priority: 10) |
| TXT | 텍스트 정보 | SPF, DKIM 인증 정보 |
| NS | 네임서버 지정 | example.com → ns1.example.com |
| SOA | 권한 시작 | 도메인의 기본 정보 |

### CNAME vs A 레코드

```
# A 레코드: 직접 IP 매핑
api.example.com.    A       10.0.0.1
api.example.com.    A       10.0.0.2    # 다중 IP 가능

# CNAME: 다른 도메인으로 별칭
www.example.com.    CNAME   example.com.   # Zone Apex에는 사용 불가
cdn.example.com.    CNAME   d123.cloudfront.net.

# 주의: CNAME과 다른 레코드 혼용 불가
# 잘못된 예시:
# www.example.com.   CNAME   example.com.
# www.example.com.   MX      mail.example.com.   # 에러!
```

---

## TTL과 캐싱

### TTL (Time To Live)

DNS 레코드가 캐시에 유지되는 시간(초)입니다.

```
example.com.    300     IN  A   93.184.216.34
                 ▲
                 └── TTL: 300초 (5분) 동안 캐시

# TTL 설정 전략
낮은 TTL (60~300초):
  - 장점: 빠른 변경 반영, 장애 시 빠른 전환
  - 단점: DNS 서버 부하 증가
  - 사용: 마이그레이션, 장애 대비

높은 TTL (3600초 이상):
  - 장점: DNS 부하 감소, 빠른 응답
  - 단점: 변경 반영 지연
  - 사용: 안정적인 서비스
```

### 캐시 계층

```
┌─────────────────────────────────────────────────────────────┐
│                     브라우저 캐시                            │
│                   (chrome://net-internals/#dns)             │
└─────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       OS 캐시                                │
│        Linux: systemd-resolved, nscd                        │
│        macOS: dscacheutil                                   │
│        Windows: ipconfig /displaydns                        │
└─────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   Local DNS Resolver                        │
│              (ISP DNS, 8.8.8.8, 1.1.1.1)                    │
└─────────────────────────────────────────────────────────────┘
```

### 캐시 관련 명령어

```bash
# macOS: DNS 캐시 초기화
sudo dscacheutil -flushcache
sudo killall -HUP mDNSResponder

# Linux: systemd-resolved 캐시 확인
resolvectl statistics

# Windows: DNS 캐시 확인 및 초기화
ipconfig /displaydns
ipconfig /flushdns

# Java에서 DNS 캐시 설정
# JVM 옵션: -Dsun.net.inetaddr.ttl=60
```

---

## AWS Route53

### Route53 특징

- 100% SLA 가용성
- 글로벌 Anycast 네트워크
- 다양한 라우팅 정책 지원
- 헬스 체크 통합

### 라우팅 정책

```
┌─────────────────────────────────────────────────────────────────┐
│                        Route53 라우팅 정책                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. Simple (단순)                                                │
│     └── 단일 리소스로 라우팅                                      │
│                                                                  │
│  2. Weighted (가중치)                                            │
│     ├── 서버 A: 70%                                              │
│     └── 서버 B: 30%                                              │
│                                                                  │
│  3. Latency (지연 시간)                                          │
│     └── 가장 낮은 지연 시간의 리전으로 라우팅                      │
│                                                                  │
│  4. Geolocation (지리적 위치)                                    │
│     ├── 한국 → 서울 리전                                         │
│     └── 미국 → 버지니아 리전                                     │
│                                                                  │
│  5. Failover (장애 조치)                                         │
│     ├── Primary: 메인 서버                                       │
│     └── Secondary: 헬스 체크 실패 시 전환                        │
│                                                                  │
│  6. Multivalue Answer                                            │
│     └── 여러 IP 반환 + 헬스 체크                                  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Route53 설정 예시 (Terraform)

```hcl
# Hosted Zone 생성
resource "aws_route53_zone" "main" {
  name = "example.com"
}

# A 레코드 - ALB 연결
resource "aws_route53_record" "api" {
  zone_id = aws_route53_zone.main.zone_id
  name    = "api.example.com"
  type    = "A"

  alias {
    name                   = aws_lb.main.dns_name
    zone_id                = aws_lb.main.zone_id
    evaluate_target_health = true
  }
}

# 가중치 기반 라우팅
resource "aws_route53_record" "weighted_primary" {
  zone_id        = aws_route53_zone.main.zone_id
  name           = "app.example.com"
  type           = "A"
  ttl            = 300
  set_identifier = "primary"

  weighted_routing_policy {
    weight = 70
  }

  records = ["10.0.1.100"]
}

resource "aws_route53_record" "weighted_secondary" {
  zone_id        = aws_route53_zone.main.zone_id
  name           = "app.example.com"
  type           = "A"
  ttl            = 300
  set_identifier = "secondary"

  weighted_routing_policy {
    weight = 30
  }

  records = ["10.0.2.100"]
}

# Failover 라우팅 + 헬스 체크
resource "aws_route53_health_check" "primary" {
  fqdn              = "primary.example.com"
  port              = 443
  type              = "HTTPS"
  resource_path     = "/health"
  failure_threshold = 3
  request_interval  = 30
}

resource "aws_route53_record" "failover_primary" {
  zone_id        = aws_route53_zone.main.zone_id
  name           = "service.example.com"
  type           = "A"
  ttl            = 60
  set_identifier = "primary"

  failover_routing_policy {
    type = "PRIMARY"
  }

  health_check_id = aws_route53_health_check.primary.id
  records         = ["10.0.1.100"]
}

resource "aws_route53_record" "failover_secondary" {
  zone_id        = aws_route53_zone.main.zone_id
  name           = "service.example.com"
  type           = "A"
  ttl            = 60
  set_identifier = "secondary"

  failover_routing_policy {
    type = "SECONDARY"
  }

  records = ["10.0.2.100"]
}
```

### Alias 레코드 vs CNAME

| 구분 | Alias | CNAME |
|------|-------|-------|
| Zone Apex 사용 | 가능 (example.com) | 불가능 |
| 비용 | AWS 리소스 무료 | 쿼리당 비용 |
| 대상 | AWS 리소스만 | 모든 도메인 |
| TTL | 대상 리소스 TTL 상속 | 직접 설정 |

---

## 핵심 정리

### DNS 질의 과정 요약

| 단계 | 서버 | 응답 |
|------|------|------|
| 1 | Root DNS | TLD 서버 주소 (.com NS) |
| 2 | TLD DNS | Authoritative 서버 주소 |
| 3 | Authoritative DNS | 최종 IP 주소 |

### Route53 라우팅 정책 선택 가이드

| 요구사항 | 권장 정책 |
|----------|----------|
| 단순 IP 매핑 | Simple |
| 카나리 배포, A/B 테스트 | Weighted |
| 글로벌 서비스 지연 시간 최적화 | Latency |
| 국가별 콘텐츠 제공 | Geolocation |
| DR 구성 | Failover |
| 고가용성 + 헬스 체크 | Multivalue Answer |

### 면접 대비 핵심 질문

1. **Q: DNS 질의 과정을 설명해주세요**
   - A: 브라우저 캐시 → OS 캐시 → Local DNS → (Root → TLD → Authoritative) 순서로 질의하며, Local DNS가 반복 질의 수행

2. **Q: CNAME을 Zone Apex에 사용할 수 없는 이유는?**
   - A: RFC 규정상 CNAME은 해당 이름에 다른 레코드와 공존 불가. Zone Apex에는 SOA, NS 레코드가 필수이므로 CNAME 사용 불가. Route53 Alias로 해결 가능

3. **Q: TTL을 낮게 설정하면 어떤 문제가 있나요?**
   - A: DNS 서버 부하 증가, 네트워크 트래픽 증가, 약간의 지연 시간 증가. 그러나 빠른 장애 대응 가능

4. **Q: DNS Failover 구성 시 주의점은?**
   - A: 헬스 체크 간격과 TTL을 고려한 전환 시간 계산, 헬스 체크 엔드포인트의 안정성 확보

---

*마지막 업데이트: 2026년 01월*
