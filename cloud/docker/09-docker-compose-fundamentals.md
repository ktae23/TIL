# Docker Compose 기초

Docker Compose의 핵심 구조(services, networks, volumes), 주요 명령어, 환경 변수 관리를 정리한다.

## 목차

- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### Docker Compose란?

Docker Compose는 **멀티 컨테이너 애플리케이션**을 YAML 파일로 정의하고, 한 번의 명령으로 실행/종료하는 도구이다.

```
개별 docker run 명령어:                Docker Compose:
──────────────────────                ──────────────
docker run -d mysql ...               docker compose up -d
docker run -d redis ...               (compose.yml에 모두 정의)
docker run -d kafka ...
docker run -d my-app ...
docker network create ...             docker compose down
docker volume create ...              (한 번에 정리)
```

### Docker Compose V2

```bash
# V1 (레거시, 별도 바이너리)
docker-compose up

# V2 (현재 표준, Docker CLI 플러그인)
docker compose up
```

Docker Compose V2는 Docker CLI에 통합되었다. `docker-compose` 대신 `docker compose`를 사용한다.

---

## 2. 왜 알아야 하는가 (Why)

### 개발 환경 표준화

```
신규 팀원 온보딩:

이전: "MySQL 설치하고, Redis 설치하고, Kafka 설치하고..."
     → 환경 설정에 2-3일 소요, 버전 불일치 문제

이후: git clone → docker compose up -d
     → 5분 내에 전체 개발 환경 준비
```

### 인프라를 코드로 관리 (IaC)

- compose.yml을 Git에 커밋 → 환경 변경 이력 추적
- 코드 리뷰를 통한 인프라 변경 검증
- 누구나 동일한 환경 재현 가능

---

## 3. 내부 구현 분석 (How)

### compose.yml 핵심 구조

```yaml
# compose.yml (docker-compose.yml도 가능)

# 서비스 정의 (필수)
services:
  app:
    image: my-app:latest
    # 또는 빌드
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=local
    depends_on:
      - mysql
      - redis
    volumes:
      - ./src:/app/src
    networks:
      - backend

  mysql:
    image: mysql:8
    environment:
      MYSQL_ROOT_PASSWORD: secret
      MYSQL_DATABASE: myapp
    volumes:
      - mysql-data:/var/lib/mysql
    networks:
      - backend

  redis:
    image: redis:7-alpine
    volumes:
      - redis-data:/data
    networks:
      - backend

# 네트워크 정의 (선택)
networks:
  backend:
    driver: bridge

# 볼륨 정의 (선택)
volumes:
  mysql-data:
  redis-data:
```

### 서비스 설정 옵션

```yaml
services:
  app:
    # === 이미지 소스 ===
    image: my-app:1.0          # 이미지 사용
    build:                     # 또는 빌드
      context: ./app
      dockerfile: Dockerfile
      args:
        - JAR_FILE=app.jar

    # === 네트워크 ===
    ports:
      - "8080:8080"            # 호스트:컨테이너
      - "5005:5005"            # 디버그 포트
    expose:
      - "8080"                 # 컨테이너 간에만 노출
    networks:
      - backend

    # === 환경 변수 ===
    environment:               # 인라인 정의
      SPRING_PROFILES_ACTIVE: local
      DB_HOST: mysql
    env_file:                  # 파일에서 로드
      - .env.local

    # === 볼륨 ===
    volumes:
      - mysql-data:/var/lib/mysql    # Named Volume
      - ./config:/app/config:ro      # Bind Mount (읽기 전용)

    # === 실행 설정 ===
    command: ["java", "-jar", "app.jar"]   # CMD 오버라이드
    entrypoint: ["/entrypoint.sh"]         # ENTRYPOINT 오버라이드
    working_dir: /app
    user: "1001:1001"

    # === 리소스 제한 ===
    deploy:
      resources:
        limits:
          cpus: "1.0"
          memory: 512M
        reservations:
          cpus: "0.5"
          memory: 256M

    # === 재시작 정책 ===
    restart: unless-stopped

    # === 로깅 ===
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"
```

### 주요 명령어

```bash
# === 기본 명령어 ===

# 서비스 시작 (백그라운드)
docker compose up -d

# 서비스 시작 (이미지 강제 재빌드)
docker compose up -d --build

# 서비스 중지 및 리소스 정리
docker compose down

# 서비스 중지 + 볼륨도 삭제
docker compose down -v

# === 상태 확인 ===

# 실행 중인 서비스 목록
docker compose ps

# 서비스 로그
docker compose logs -f app

# 특정 서비스 로그 (최근 100줄)
docker compose logs --tail 100 mysql

# === 개별 서비스 관리 ===

# 특정 서비스만 재시작
docker compose restart app

# 특정 서비스만 중지/시작
docker compose stop mysql
docker compose start mysql

# 서비스 내부 접속
docker compose exec app /bin/bash

# 서비스 스케일링
docker compose up -d --scale worker=3
```

### 환경 변수 관리

```
환경 변수 우선순위 (높은 순):

1. docker compose run -e VAR=value
2. 셸 환경 변수 (export VAR=value)
3. .env 파일
4. compose.yml의 environment
5. Dockerfile의 ENV
```

**.env 파일:**

```bash
# .env (compose.yml과 같은 디렉토리)
MYSQL_ROOT_PASSWORD=secret
MYSQL_DATABASE=myapp
SPRING_PROFILES_ACTIVE=local
APP_VERSION=1.0.0
```

```yaml
# compose.yml에서 변수 참조
services:
  mysql:
    image: mysql:8
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}

  app:
    image: my-app:${APP_VERSION:-latest}  # 기본값: latest
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE}
```

**환경별 .env 파일 분리:**

```bash
# 개발 환경
docker compose --env-file .env.dev up -d

# 스테이징 환경
docker compose --env-file .env.staging up -d
```

---

## 4. 실전 예제

### Spring Boot + MySQL + Redis 기본 구성

```yaml
# compose.yml
services:
  app:
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/myapp
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: 6379
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started
    restart: unless-stopped

  mysql:
    image: mysql:8
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: myapp
    volumes:
      - mysql-data:/var/lib/mysql
      - ./db/init.sql:/docker-entrypoint-initdb.d/init.sql:ro
    ports:
      - "3306:3306"
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    volumes:
      - redis-data:/data
    ports:
      - "6379:6379"
    restart: unless-stopped

volumes:
  mysql-data:
  redis-data:
```

### compose.yml 유효성 검사

```bash
# 설정 파일 문법 검사
docker compose config

# 서비스 목록만 출력
docker compose config --services

# 볼륨 목록만 출력
docker compose config --volumes
```

---

## 5. 정리

| 개념 | 설명 |
|------|------|
| compose.yml | 멀티 컨테이너 앱을 YAML로 정의하는 파일 |
| services | 각 컨테이너(서비스) 정의 |
| networks | 서비스 간 네트워크 정의 |
| volumes | 데이터 영속성을 위한 볼륨 정의 |
| docker compose up -d | 모든 서비스를 백그라운드로 시작 |
| docker compose down | 모든 서비스 중지 및 리소스 정리 |
| .env | compose.yml에서 사용할 환경 변수 파일 |
| depends_on | 서비스 시작 순서 지정 |
| healthcheck | 서비스 헬스 체크 설정 |

---

*참고: Docker Compose V2, compose.yml specification 3.x 기준*
