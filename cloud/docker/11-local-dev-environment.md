# 로컬 개발환경 구성 실전

Kafka + Redis + MySQL + Prometheus를 Docker Compose로 통합 구성하고, Spring Boot 애플리케이션과 연동하는 실전 로컬 개발환경을 구축한다.

## 목차

- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### 로컬 개발환경이란?

프로덕션 인프라를 로컬 머신에서 Docker로 재현하여, 실제와 동일한 조건에서 개발/테스트할 수 있는 환경이다.

```
프로덕션 환경:                      로컬 개발환경:
──────────────                    ──────────────
AWS RDS (MySQL)                   MySQL 컨테이너
AWS ElastiCache (Redis)           Redis 컨테이너
AWS MSK (Kafka)                   Kafka 컨테이너
CloudWatch / Datadog              Prometheus + Grafana 컨테이너
Spring Boot (ECS/EKS)             IDE에서 직접 실행 또는 컨테이너
```

### 아키텍처 구성도

```
┌──────────────────────────────────────────────────────────────┐
│                    Local Development                          │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐    │
│  │                  Docker Compose                       │    │
│  │                                                      │    │
│  │  ┌────────┐  ┌────────┐  ┌──────────┐  ┌─────────┐ │    │
│  │  │ MySQL  │  │ Redis  │  │  Kafka   │  │Zookeeper│ │    │
│  │  │ :3306  │  │ :6379  │  │  :9092   │  │ :2181   │ │    │
│  │  └────────┘  └────────┘  └──────────┘  └─────────┘ │    │
│  │                                                      │    │
│  │  ┌───────────┐  ┌─────────┐  ┌───────────────────┐ │    │
│  │  │Prometheus │  │ Grafana │  │  kafka-ui          │ │    │
│  │  │  :9090    │  │ :3000   │  │  :8989             │ │    │
│  │  └───────────┘  └─────────┘  └───────────────────┘ │    │
│  └──────────────────────────────────────────────────────┘    │
│                          ▲                                    │
│                          │ 연결                               │
│                          │                                    │
│  ┌───────────────────────┴──────────────────────────────┐    │
│  │              Spring Boot (IDE 실행)                    │    │
│  │              localhost:8080                            │    │
│  │                                                      │    │
│  │  DB:    localhost:3306                                │    │
│  │  Redis: localhost:6379                                │    │
│  │  Kafka: localhost:9092                                │    │
│  └──────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────┘
```

---

## 2. 왜 알아야 하는가 (Why)

### 개발-운영 간극 최소화

| 문제 | Docker 로컬 환경으로 해결 |
|------|------------------------|
| "로컬에서는 H2, 운영은 MySQL" | 로컬도 MySQL 컨테이너 사용 |
| "Kafka 없이 개발, 운영에서 문제" | 로컬에 Kafka 컨테이너 구성 |
| "메트릭 확인은 배포 후에만" | Prometheus+Grafana로 로컬에서 확인 |
| "환경 설정이 팀원마다 다름" | compose.yml로 동일 환경 보장 |

### 비용 절감

```
개발자 10명이 각각 AWS 개발 환경 사용:
→ RDS + ElastiCache + MSK × 10 = 월 수천 달러

Docker 로컬 환경:
→ 각자 노트북에서 실행 = 추가 비용 $0
```

---

## 3. 내부 구현 분석 (How)

### MySQL 구성

```yaml
mysql:
  image: mysql:8
  container_name: local-mysql
  environment:
    MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root}
    MYSQL_DATABASE: myapp
    MYSQL_USER: app
    MYSQL_PASSWORD: ${MYSQL_PASSWORD:-app123}
    TZ: Asia/Seoul
  ports:
    - "3306:3306"
  volumes:
    - mysql-data:/var/lib/mysql
    - ./docker/mysql/init:/docker-entrypoint-initdb.d:ro  # 초기화 스크립트
    - ./docker/mysql/conf.d:/etc/mysql/conf.d:ro           # 커스텀 설정
  command: >
    --character-set-server=utf8mb4
    --collation-server=utf8mb4_unicode_ci
    --default-authentication-plugin=caching_sha2_password
    --max-connections=100
  healthcheck:
    test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD:-root}"]
    interval: 10s
    timeout: 5s
    retries: 5
    start_period: 30s
```

**초기화 스크립트 (docker-entrypoint-initdb.d):**

```sql
-- docker/mysql/init/01-schema.sql
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- docker/mysql/init/02-seed.sql
INSERT INTO users (email, name) VALUES
('admin@example.com', 'Admin'),
('test@example.com', 'Test User');
```

### Redis 구성

```yaml
redis:
  image: redis:7-alpine
  container_name: local-redis
  ports:
    - "6379:6379"
  volumes:
    - redis-data:/data
  command: >
    redis-server
    --appendonly yes
    --maxmemory 256mb
    --maxmemory-policy allkeys-lru
    --requirepass ${REDIS_PASSWORD:-redis123}
  healthcheck:
    test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD:-redis123}", "ping"]
    interval: 10s
    timeout: 3s
    retries: 3
```

### Kafka 구성 (KRaft 모드 - Zookeeper 없이)

```yaml
kafka:
  image: confluentinc/cp-kafka:7.5.0
  container_name: local-kafka
  ports:
    - "9092:9092"      # 호스트에서 접속용
    - "29092:29092"    # 컨테이너 간 통신용
  environment:
    KAFKA_NODE_ID: 1
    KAFKA_PROCESS_ROLES: broker,controller
    KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:29093
    KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:29092,CONTROLLER://0.0.0.0:29093,EXTERNAL://0.0.0.0:9092
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,EXTERNAL://localhost:9092
    KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT,EXTERNAL:PLAINTEXT
    KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
    KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
    KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
    KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    CLUSTER_ID: "MkU3OEVBNTcwNTJENDM2Qk"
  volumes:
    - kafka-data:/var/lib/kafka/data
  healthcheck:
    test: ["CMD-SHELL", "kafka-broker-api-versions --bootstrap-server localhost:29092 || exit 1"]
    interval: 15s
    timeout: 10s
    retries: 5
    start_period: 30s
```

**Kafka UI (토픽/메시지 관리):**

```yaml
kafka-ui:
  image: provectuslabs/kafka-ui:latest
  container_name: local-kafka-ui
  ports:
    - "8989:8080"
  environment:
    KAFKA_CLUSTERS_0_NAME: local
    KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092
  depends_on:
    kafka:
      condition: service_healthy
  profiles:
    - tools
```

### Prometheus + Grafana 구성

```yaml
prometheus:
  image: prom/prometheus:v2.48.0
  container_name: local-prometheus
  ports:
    - "9090:9090"
  volumes:
    - ./docker/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
    - prometheus-data:/prometheus
  command:
    - '--config.file=/etc/prometheus/prometheus.yml'
    - '--storage.tsdb.retention.time=7d'
  profiles:
    - monitoring

grafana:
  image: grafana/grafana:10.2.0
  container_name: local-grafana
  ports:
    - "3000:3000"
  environment:
    GF_SECURITY_ADMIN_USER: admin
    GF_SECURITY_ADMIN_PASSWORD: admin
    GF_AUTH_ANONYMOUS_ENABLED: "true"
  volumes:
    - grafana-data:/var/lib/grafana
    - ./docker/grafana/provisioning:/etc/grafana/provisioning:ro
  depends_on:
    - prometheus
  profiles:
    - monitoring
```

**Prometheus 설정 파일:**

```yaml
# docker/prometheus/prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'spring-boot'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8080']
    # host.docker.internal: Docker 컨테이너에서 호스트 머신 접근
```

---

## 4. 실전 예제

### 전체 compose.yml

```yaml
# compose.yml
services:
  # ===== Core Services (항상 시작) =====
  mysql:
    image: mysql:8
    container_name: local-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root}
      MYSQL_DATABASE: myapp
      MYSQL_USER: app
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-app123}
      TZ: Asia/Seoul
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./docker/mysql/init:/docker-entrypoint-initdb.d:ro
    command: >
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
      --max-connections=100
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    container_name: local-redis
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    command: redis-server --appendonly yes --maxmemory 256mb --maxmemory-policy allkeys-lru
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 3
    restart: unless-stopped

  # ===== Kafka (full 프로필) =====
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: local-kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:29093
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:29092,CONTROLLER://0.0.0.0:29093,EXTERNAL://0.0.0.0:9092
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,EXTERNAL://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT,EXTERNAL:PLAINTEXT
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
      CLUSTER_ID: "MkU3OEVBNTcwNTJENDM2Qk"
    volumes:
      - kafka-data:/var/lib/kafka/data
    healthcheck:
      test: ["CMD-SHELL", "kafka-broker-api-versions --bootstrap-server localhost:29092 || exit 1"]
      interval: 15s
      timeout: 10s
      retries: 5
      start_period: 30s
    profiles:
      - full
    restart: unless-stopped

  # ===== Monitoring (monitoring 프로필) =====
  prometheus:
    image: prom/prometheus:v2.48.0
    container_name: local-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./docker/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.retention.time=7d'
    profiles:
      - monitoring
    restart: unless-stopped

  grafana:
    image: grafana/grafana:10.2.0
    container_name: local-grafana
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_USER: admin
      GF_SECURITY_ADMIN_PASSWORD: admin
    volumes:
      - grafana-data:/var/lib/grafana
    depends_on:
      - prometheus
    profiles:
      - monitoring
    restart: unless-stopped

  # ===== Tools (tools 프로필) =====
  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: local-kafka-ui
    ports:
      - "8989:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092
    depends_on:
      kafka:
        condition: service_healthy
    profiles:
      - tools

volumes:
  mysql-data:
  redis-data:
  kafka-data:
  prometheus-data:
  grafana-data:
```

### Spring Boot application-local.yml

```yaml
# src/main/resources/application-local.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/myapp
    username: app
    password: app123
    driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      host: localhost
      port: 6379

  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: my-app-local
      auto-offset-reset: earliest

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### 사용법

```bash
# 1. 기본 실행 (MySQL + Redis)
docker compose up -d

# 2. Kafka 포함 실행
docker compose --profile full up -d

# 3. 모니터링 포함 실행
docker compose --profile monitoring up -d

# 4. 전체 실행 (모든 프로필)
docker compose --profile full --profile monitoring --profile tools up -d

# 5. Spring Boot 실행 (IDE 또는 CLI)
./gradlew bootRun --args='--spring.profiles.active=local'

# 6. 상태 확인
docker compose ps

# 7. 로그 확인
docker compose logs -f mysql kafka

# 8. 전체 중지 + 정리
docker compose --profile full --profile monitoring --profile tools down

# 9. 데이터 포함 완전 초기화
docker compose down -v
```

### Makefile로 편의성 향상

```makefile
# Makefile
.PHONY: up down full monitor reset logs

up:
	docker compose up -d

down:
	docker compose down

full:
	docker compose --profile full up -d

monitor:
	docker compose --profile full --profile monitoring up -d

all:
	docker compose --profile full --profile monitoring --profile tools up -d

reset:
	docker compose down -v
	docker compose up -d

logs:
	docker compose logs -f $(service)
```

```bash
make up           # 기본 시작
make full         # Kafka 포함
make monitor      # 모니터링 포함
make all          # 전체 시작
make reset        # 데이터 초기화 후 재시작
make logs service=kafka   # 특정 서비스 로그
```

---

## 5. 정리

| 개념 | 설명 |
|------|------|
| 로컬 개발환경 | 프로덕션 인프라를 Docker로 로컬 재현 |
| KRaft 모드 | Zookeeper 없는 Kafka (Kafka 3.3+) |
| host.docker.internal | 컨테이너에서 호스트 머신 접근 주소 |
| profiles | 환경별 선택적 서비스 구성 (full, monitoring, tools) |
| docker-entrypoint-initdb.d | MySQL/PostgreSQL 초기화 스크립트 디렉토리 |
| Prometheus + Grafana | 메트릭 수집 + 대시보드 시각화 |
| Kafka UI | 웹 기반 Kafka 토픽/메시지 관리 도구 |
| Makefile | docker compose 명령어 간소화 |

---

*참고: Docker Compose V2, Confluent Kafka 7.5, Prometheus 2.48, Grafana 10.2 기준*
