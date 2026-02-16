# Docker 보안 기초

Docker 환경의 보안 위협과 대응 방안을 정리한다. non-root 실행, 읽기 전용 파일 시스템, 이미지 스캐닝, secrets 관리까지 다룬다.

## 목차

- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### Docker 보안의 범위

```
┌──────────────────────────────────────────────────────┐
│                Docker 보안 레이어                       │
│                                                      │
│  ┌──────────────┐  이미지 보안                        │
│  │ Image        │  - 베이스 이미지 취약점 스캐닝         │
│  │ Security     │  - 경량 이미지 사용 (attack surface↓) │
│  │              │  - 시크릿 미포함 확인                  │
│  └──────────────┘                                    │
│                                                      │
│  ┌──────────────┐  런타임 보안                        │
│  │ Runtime      │  - non-root 사용자 실행              │
│  │ Security     │  - 읽기 전용 파일 시스템               │
│  │              │  - 리소스 제한 (cgroups)              │
│  │              │  - Capability 제한                   │
│  └──────────────┘                                    │
│                                                      │
│  ┌──────────────┐  네트워크 보안                       │
│  │ Network      │  - 네트워크 격리                     │
│  │ Security     │  - 불필요한 포트 노출 제한             │
│  └──────────────┘                                    │
│                                                      │
│  ┌──────────────┐  시크릿 관리                        │
│  │ Secrets      │  - 환경 변수 vs secrets              │
│  │ Management   │  - BuildKit secrets                 │
│  └──────────────┘                                    │
└──────────────────────────────────────────────────────┘
```

---

## 2. 왜 알아야 하는가 (Why)

### 컨테이너 보안 위협

| 위협 | 설명 | 영향 |
|------|------|------|
| 컨테이너 탈출 | root 권한으로 호스트 접근 | 호스트 시스템 장악 |
| 이미지 취약점 | 알려진 CVE가 포함된 패키지 | 원격 코드 실행 |
| 시크릿 노출 | 이미지에 비밀번호/토큰 포함 | 인증 정보 탈취 |
| 권한 상승 | 불필요한 capability로 특권 획득 | 시스템 제어권 탈취 |

### 실제 사고 사례

```
2019: Alpine Docker 이미지에 빈 root 비밀번호 취약점 (CVE-2019-5021)
→ 모든 Alpine 기반 이미지에서 root 접근 가능

2020: Docker Hub에서 악성 이미지 20개 이상 발견
→ 암호화폐 채굴, 백도어 포함
→ 수백만 회 pull
```

---

## 3. 내부 구현 분석 (How)

### Practice 1: non-root 사용자 실행

```dockerfile
# === 나쁜 예: root로 실행 (기본값) ===
FROM openjdk:17-slim
COPY app.jar /app/
CMD ["java", "-jar", "/app/app.jar"]
# 컨테이너 내부에서 root 권한으로 실행됨

# === 좋은 예: 전용 사용자로 실행 ===
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -D -s /sbin/nologin appuser
WORKDIR /app
COPY --chown=appuser:appgroup app.jar ./
USER appuser
CMD ["java", "-jar", "app.jar"]
```

```bash
# 런타임에서 사용자 지정
docker run --user 1001:1001 my-app

# docker-compose에서
services:
  app:
    image: my-app
    user: "1001:1001"
```

### Practice 2: 읽기 전용 파일 시스템

```bash
# 컨테이너 파일 시스템을 읽기 전용으로 실행
docker run --read-only \
  --tmpfs /tmp \
  --tmpfs /var/run \
  my-app

# 쓰기가 필요한 경로만 tmpfs로 마운트
```

```yaml
# docker-compose에서
services:
  app:
    image: my-app
    read_only: true
    tmpfs:
      - /tmp
      - /var/run
    volumes:
      - app-logs:/app/logs  # 로그만 영속적으로 쓰기 허용
```

**효과**: 악성 코드가 파일 시스템에 쓸 수 없어 방어력 강화

### Practice 3: Linux Capability 제한

Docker 컨테이너는 기본적으로 일부 Linux capability를 가진다:

```bash
# 모든 capability 제거 후 필요한 것만 추가
docker run \
  --cap-drop=ALL \
  --cap-add=NET_BIND_SERVICE \
  my-app

# NET_BIND_SERVICE: 1024 이하 포트 바인딩 (nginx 등에 필요)
```

| Capability | 용도 | 보통 필요? |
|-----------|------|----------|
| NET_BIND_SERVICE | 1024 이하 포트 바인딩 | nginx, 웹서버만 |
| CHOWN | 파일 소유권 변경 | 거의 불필요 |
| DAC_OVERRIDE | 파일 권한 무시 | 불필요 |
| SYS_ADMIN | 시스템 관리 (마운트 등) | 위험, 거의 불필요 |
| NET_RAW | Raw 소켓 (ping 등) | 불필요 |

```yaml
# docker-compose에서
services:
  app:
    image: my-app
    cap_drop:
      - ALL
    cap_add:
      - NET_BIND_SERVICE
```

### Practice 4: 이미지 취약점 스캐닝

```bash
# Docker Scout (Docker Desktop 내장)
docker scout cves my-app:latest
docker scout quickview my-app:latest

# Trivy (오픈소스, 권장)
trivy image my-app:latest

# 출력 예시:
# my-app:latest (alpine 3.18.4)
# ============================
# Total: 3 (HIGH: 2, CRITICAL: 1)
#
# ┌───────────────┬────────────────┬──────────┬───────────────┐
# │   Library     │ Vulnerability  │ Severity │ Fixed Version │
# ├───────────────┼────────────────┼──────────┼───────────────┤
# │ libcrypto3    │ CVE-2024-XXXX  │ CRITICAL │ 3.1.4-r1      │
# │ libssl3       │ CVE-2024-YYYY  │ HIGH     │ 3.1.4-r1      │
# └───────────────┴────────────────┴──────────┴───────────────┘
```

```bash
# CI/CD에서 자동 스캐닝 (GitHub Actions 예시)
# .github/workflows/security.yml
# - name: Run Trivy
#   uses: aquasecurity/trivy-action@master
#   with:
#     image-ref: my-app:latest
#     severity: 'CRITICAL,HIGH'
#     exit-code: '1'       # 취약점 발견 시 빌드 실패
```

### Practice 5: Secrets 관리

**환경 변수의 문제:**

```bash
# 환경 변수는 여러 곳에서 노출될 수 있음
docker inspect my-app  # environment 섹션에 평문 노출
docker exec my-app env  # 환경 변수 전체 출력
/proc/1/environ         # 프로세스 환경 변수 파일
```

**Docker Secrets (Compose):**

```yaml
services:
  app:
    image: my-app
    secrets:
      - db_password
      - api_key
    environment:
      # 시크릿 파일 경로를 환경 변수로 전달
      DB_PASSWORD_FILE: /run/secrets/db_password

secrets:
  db_password:
    file: ./secrets/db_password.txt    # 로컬 파일에서 읽기
  api_key:
    environment: API_KEY               # 호스트 환경 변수에서 읽기
```

컨테이너 내부에서 `/run/secrets/db_password` 파일로 시크릿에 접근한다.

**BuildKit Secrets (빌드 시):**

```dockerfile
# syntax=docker/dockerfile:1
FROM node:20-alpine
WORKDIR /app
COPY package.json package-lock.json ./

# .npmrc를 시크릿으로 마운트 (레이어에 저장 안 됨)
RUN --mount=type=secret,id=npmrc,target=/root/.npmrc \
    npm ci

COPY . .
CMD ["node", "server.js"]
```

```bash
docker build --secret id=npmrc,src=.npmrc -t my-app .
```

### Practice 6: 네트워크 보안

```yaml
services:
  nginx:
    networks:
      - frontend    # 외부 접근 가능

  app:
    networks:
      - frontend    # nginx에서 접근
      - backend     # DB 접근

  mysql:
    networks:
      - backend     # app에서만 접근, 외부 접근 차단

networks:
  frontend:
  backend:
    internal: true  # 외부 접근 완전 차단
```

`internal: true`로 설정된 네트워크는 외부(호스트, 인터넷)에서 접근할 수 없다.

### Practice 7: --no-new-privileges

```bash
# 프로세스가 실행 중에 새 권한을 얻는 것을 방지
docker run --security-opt=no-new-privileges my-app
```

```yaml
# docker-compose에서
services:
  app:
    security_opt:
      - no-new-privileges:true
```

setuid/setgid 바이너리를 통한 권한 상승을 방지한다.

---

## 4. 실전 예제

### 보안 강화된 프로덕션 Dockerfile

```dockerfile
# syntax=docker/dockerfile:1
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build
COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
RUN ./gradlew dependencies --no-daemon
COPY src/ src/
RUN ./gradlew bootJar --no-daemon

# distroless: 셸 없음, 패키지 매니저 없음
FROM gcr.io/distroless/java17-debian12

# distroless는 nonroot 사용자 내장 (UID 65532)
USER nonroot:nonroot

WORKDIR /app
COPY --from=builder --chown=nonroot:nonroot /build/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
```

### 보안 강화된 docker-compose.yml

```yaml
services:
  app:
    image: my-app:latest
    read_only: true
    tmpfs:
      - /tmp:size=100m
    user: "1001:1001"
    cap_drop:
      - ALL
    security_opt:
      - no-new-privileges:true
    deploy:
      resources:
        limits:
          cpus: "1.0"
          memory: 512M
    secrets:
      - db_password
    environment:
      DB_PASSWORD_FILE: /run/secrets/db_password
    networks:
      - backend

  mysql:
    image: mysql:8
    read_only: true
    tmpfs:
      - /tmp
      - /var/run/mysqld
    cap_drop:
      - ALL
    cap_add:
      - DAC_OVERRIDE   # MySQL이 데이터 파일 접근에 필요
      - CHOWN
      - SETGID
      - SETUID
    security_opt:
      - no-new-privileges:true
    volumes:
      - mysql-data:/var/lib/mysql
    networks:
      - backend

networks:
  backend:
    internal: true  # 외부 접근 차단

secrets:
  db_password:
    file: ./secrets/db_password.txt

volumes:
  mysql-data:
```

### Docker Bench for Security

Docker 환경 전체의 보안 상태를 점검하는 도구:

```bash
# Docker Bench Security 실행
docker run --rm \
  --net host \
  --pid host \
  --userns host \
  --cap-add audit_control \
  -v /etc:/etc:ro \
  -v /var/lib:/var/lib:ro \
  -v /var/run/docker.sock:/var/run/docker.sock:ro \
  docker/docker-bench-security

# CIS Docker Benchmark 기준으로 점검
# [PASS] / [WARN] / [INFO] 결과 출력
```

### .gitignore에 시크릿 제외

```
# .gitignore
secrets/
.env
.env.*
*.pem
*.key
*.p12
*.jks
```

---

## 5. 정리

| Practice | 핵심 |
|----------|------|
| non-root 실행 | USER 명령어로 비특권 사용자 지정 |
| 읽기 전용 FS | --read-only + tmpfs로 쓰기 최소화 |
| Capability 제한 | --cap-drop=ALL 후 필요한 것만 추가 |
| 이미지 스캐닝 | Trivy/Scout로 CVE 탐지, CI/CD 통합 |
| Secrets 관리 | Docker Secrets 또는 BuildKit secrets 사용 |
| 네트워크 격리 | internal 네트워크로 외부 접근 차단 |
| no-new-privileges | 런타임 권한 상승 방지 |
| distroless | 셸 없는 최소 이미지, 공격 표면 최소화 |
| Docker Bench | CIS Benchmark 기반 보안 자동 점검 |

---

*참고: Docker Engine 24.x, Trivy, Docker Bench for Security, CIS Docker Benchmark v1.6 기준*
