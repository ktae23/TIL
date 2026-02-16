# Docker Compose 멀티 서비스 구성

depends_on/healthcheck를 활용한 서비스 의존성 관리, 프로필, 오버라이드 파일을 통한 환경별 구성 전략을 정리한다.

## 목차

- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### 멀티 서비스 환경의 과제

실제 개발 환경에서는 여러 서비스가 **특정 순서와 조건**으로 시작되어야 한다:

```
MySQL이 Ready 상태가 되어야 → Spring Boot 시작 가능
Kafka Broker가 시작되어야 → Consumer 서비스 시작 가능
Redis가 연결 가능해야    → 캐시 의존 서비스 시작 가능
```

### 주요 기능

| 기능 | 역할 |
|------|------|
| depends_on + healthcheck | 서비스 시작 순서와 준비 상태 확인 |
| profiles | 환경별로 선택적 서비스 활성화 |
| override files | 환경별 설정 오버라이드 |
| extends | 공통 설정 상속 |

---

## 2. 왜 알아야 하는가 (Why)

### 시작 순서 문제

```
문제 상황:

docker compose up -d
→ app, mysql, redis가 동시에 시작
→ app이 mysql보다 먼저 시작됨
→ DB 연결 실패로 app 크래시
→ restart policy로 재시작 반복
→ mysql 준비 후에야 app 정상 동작

"결국 되긴 하지만" 불필요한 에러 로그와 불안정한 시작
```

### 환경별 구성

```
개발 환경: app + mysql + redis + kafka + prometheus + grafana
테스트 환경: app + mysql + redis (최소 구성)
CI 환경: app + mysql + redis (테스트만)
```

profiles로 환경별 서비스를 선택적으로 활성화할 수 있다.

---

## 3. 내부 구현 분석 (How)

### depends_on + healthcheck

```yaml
services:
  mysql:
    image: mysql:8
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s      # 헬스체크 간격
      timeout: 5s         # 타임아웃
      retries: 5          # 실패 허용 횟수
      start_period: 30s   # 시작 유예 기간

  app:
    image: my-app
    depends_on:
      mysql:
        condition: service_healthy    # mysql이 healthy일 때 시작
      redis:
        condition: service_started    # redis 컨테이너 시작되면 바로
```

```
시작 흐름:

┌─────────────────────────────────────────────────┐
│ 1. mysql 컨테이너 시작                             │
│ 2. healthcheck 반복 실행 (10초 간격)               │
│    - mysqladmin ping → 실패 → 재시도              │
│    - mysqladmin ping → 성공 → status: healthy     │
│ 3. redis 컨테이너 시작                             │
│    - condition: service_started → 즉시 충족        │
│ 4. mysql=healthy, redis=started → app 시작        │
└─────────────────────────────────────────────────┘
```

**주요 healthcheck 패턴:**

```yaml
# MySQL
healthcheck:
  test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]

# PostgreSQL
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U postgres"]

# Redis
healthcheck:
  test: ["CMD", "redis-cli", "ping"]

# Kafka (KRaft)
healthcheck:
  test: ["CMD-SHELL", "kafka-broker-api-versions --bootstrap-server localhost:9092"]

# HTTP 엔드포인트
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/health"]

# 범용 TCP 체크
healthcheck:
  test: ["CMD-SHELL", "nc -z localhost 3306"]
```

### Profiles

프로필을 사용하여 선택적으로 서비스를 활성화한다:

```yaml
services:
  app:
    image: my-app
    # 프로필 없음 → 항상 시작

  mysql:
    image: mysql:8
    # 프로필 없음 → 항상 시작

  redis:
    image: redis:7
    # 프로필 없음 → 항상 시작

  kafka:
    image: confluentinc/cp-kafka:7.5
    profiles:
      - full        # "full" 프로필에서만 시작

  prometheus:
    image: prom/prometheus
    profiles:
      - monitoring  # "monitoring" 프로필에서만 시작

  grafana:
    image: grafana/grafana
    profiles:
      - monitoring  # "monitoring" 프로필에서만 시작
```

```bash
# 기본 서비스만 (app + mysql + redis)
docker compose up -d

# full 프로필 포함 (+ kafka)
docker compose --profile full up -d

# monitoring 프로필 포함 (+ prometheus + grafana)
docker compose --profile monitoring up -d

# 여러 프로필 동시 활성화
docker compose --profile full --profile monitoring up -d

# 환경 변수로 프로필 설정
COMPOSE_PROFILES=full,monitoring docker compose up -d
```

### Override Files

```
파일 병합 순서:
compose.yml (기본) + compose.override.yml (자동 병합)
```

```yaml
# compose.yml (기본 설정)
services:
  app:
    image: my-app:latest
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: local

  mysql:
    image: mysql:8
    volumes:
      - mysql-data:/var/lib/mysql
```

```yaml
# compose.override.yml (개발용 오버라이드, 자동 병합)
services:
  app:
    build:
      context: .                    # 이미지 대신 빌드
    volumes:
      - ./src:/app/src              # 소스코드 핫 리로드
    ports:
      - "5005:5005"                 # 디버그 포트 추가
    environment:
      JAVA_TOOL_OPTIONS: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

```bash
# compose.yml + compose.override.yml 자동 병합
docker compose up -d

# 특정 파일 지정 (override 무시)
docker compose -f compose.yml up -d

# 프로덕션 설정 사용
docker compose -f compose.yml -f compose.prod.yml up -d
```

### extends (공통 설정 상속)

```yaml
# common.yml
services:
  base-service:
    restart: unless-stopped
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"
    deploy:
      resources:
        limits:
          memory: 512M
```

```yaml
# compose.yml
services:
  app:
    extends:
      file: common.yml
      service: base-service
    image: my-app:latest
    ports:
      - "8080:8080"

  worker:
    extends:
      file: common.yml
      service: base-service
    image: my-worker:latest
```

---

## 4. 실전 예제

### 서비스 간 통신 패턴

```yaml
services:
  # API Gateway
  nginx:
    image: nginx:1.25-alpine
    ports:
      - "80:80"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      app:
        condition: service_healthy

  # Application
  app:
    image: my-app:latest
    environment:
      DB_HOST: mysql
      DB_PORT: 3306
      REDIS_HOST: redis
      REDIS_PORT: 6379
      KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 15s
      timeout: 5s
      retries: 3
      start_period: 60s
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy

  # Database
  mysql:
    image: mysql:8
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: myapp
    volumes:
      - mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  # Cache
  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes --maxmemory 256mb --maxmemory-policy allkeys-lru
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 3

volumes:
  mysql-data:
```

### 스케일링

```bash
# worker 서비스를 3개로 스케일링
docker compose up -d --scale worker=3

# 실행 상태 확인
docker compose ps
# NAME          SERVICE   STATUS    PORTS
# my-app-1      app       running   0.0.0.0:8080->8080/tcp
# my-worker-1   worker    running
# my-worker-2   worker    running
# my-worker-3   worker    running
```

**스케일링 시 주의**: 포트 매핑이 있으면 포트 충돌 발생. 스케일링할 서비스는 `expose`만 사용하고, 앞에 로드밸런서(nginx)를 둔다.

### 빌드 + 실행 통합

```bash
# 코드 변경 후 재빌드 + 재시작
docker compose up -d --build app

# 모든 서비스 이미지 빌드 (실행하지 않음)
docker compose build

# 특정 서비스만 빌드
docker compose build app worker

# 캐시 없이 빌드
docker compose build --no-cache app
```

---

## 5. 정리

| 개념 | 설명 |
|------|------|
| depends_on + condition | 서비스 시작 순서 및 준비 상태 조건 |
| healthcheck | 서비스 상태 주기적 확인, depends_on condition과 연동 |
| service_healthy | healthcheck 통과 시 의존 서비스 시작 |
| profiles | 환경별 선택적 서비스 활성화 |
| compose.override.yml | 기본 설정 위에 자동 병합되는 오버라이드 파일 |
| -f 옵션 | 여러 compose 파일을 명시적으로 병합 |
| extends | 공통 서비스 설정 상속 |
| --scale | 서비스 인스턴스 수 동적 조정 |

---

*참고: Docker Compose V2, Compose Specification 기준*
