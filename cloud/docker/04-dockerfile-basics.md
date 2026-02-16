# Dockerfile 기초

Dockerfile의 주요 명령어(instruction)와 빌드 컨텍스트, ENTRYPOINT vs CMD 차이를 정리한다.

## 목차

- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### Dockerfile이란?

Dockerfile은 Docker 이미지를 빌드하기 위한 **텍스트 기반 스크립트**이다. 각 줄의 명령어(instruction)가 순서대로 실행되며, 각 명령어는 이미지의 새 레이어를 생성한다.

### 주요 명령어

| 명령어 | 역할 | 예시 |
|--------|------|------|
| `FROM` | 베이스 이미지 지정 | `FROM openjdk:17-slim` |
| `RUN` | 빌드 시 명령어 실행 | `RUN apt-get update && apt-get install -y curl` |
| `COPY` | 호스트 파일을 이미지로 복사 | `COPY target/app.jar /app/` |
| `ADD` | COPY + URL 다운로드/압축 해제 | `ADD https://example.com/file.tar.gz /tmp/` |
| `CMD` | 컨테이너 시작 시 기본 명령어 | `CMD ["java", "-jar", "app.jar"]` |
| `ENTRYPOINT` | 컨테이너의 실행 파일 지정 | `ENTRYPOINT ["java", "-jar"]` |
| `ENV` | 환경 변수 설정 | `ENV SPRING_PROFILES_ACTIVE=prod` |
| `ARG` | 빌드 시 변수 (이미지에 안 남음) | `ARG JAR_FILE=app.jar` |
| `EXPOSE` | 문서화용 포트 선언 | `EXPOSE 8080` |
| `WORKDIR` | 작업 디렉토리 설정 | `WORKDIR /app` |
| `USER` | 실행 사용자 변경 | `USER 1001` |
| `VOLUME` | 마운트 포인트 선언 | `VOLUME /data` |
| `HEALTHCHECK` | 헬스체크 명령어 | `HEALTHCHECK CMD curl -f http://localhost:8080/health` |
| `LABEL` | 메타데이터 추가 | `LABEL maintainer="team@company.com"` |

---

## 2. 왜 알아야 하는가 (Why)

### 재현 가능한 빌드

```
수동 설치:                     Dockerfile:
─────────────                 ────────────
1. OS 설치                     모든 과정이 코드로 문서화
2. Java 설치 (어떤 버전?)        → 버전 관리 가능
3. 설정 파일 복사 (어디서?)       → 코드 리뷰 가능
4. 앱 배포 (어떤 방법?)          → CI/CD 자동화 가능
5. 실행 (어떤 옵션?)             → 누구나 동일하게 빌드
```

### CI/CD 파이프라인 기반

Dockerfile은 CI/CD에서 이미지를 빌드하는 핵심이다:

```
코드 커밋 → CI가 Dockerfile로 빌드 → Registry에 push → 배포
```

---

## 3. 내부 구현 분석 (How)

### 빌드 컨텍스트 (Build Context)

```bash
docker build -t my-app:1.0 .
                             │
                             └── 빌드 컨텍스트 = 현재 디렉토리
```

```
빌드 컨텍스트 전송 흐름:

┌──────────────┐     tar 아카이브      ┌──────────────┐
│ Docker CLI    │ ──────────────────▶ │ Docker Daemon │
│              │   (빌드 컨텍스트     │              │
│  현재 디렉토리 │    전체를 전송)      │  빌드 실행    │
└──────────────┘                    └──────────────┘
```

**주의**: 빌드 컨텍스트에 불필요한 파일이 많으면 전송 시간이 길어진다.

**.dockerignore로 불필요한 파일 제외:**

```
# .dockerignore
.git
.gitignore
node_modules
target
*.md
*.log
.env
.idea
.vscode
```

### ENTRYPOINT vs CMD

두 명령어의 차이를 이해하는 것이 중요하다:

```
┌───────────────────────────────────────────────────────┐
│                   ENTRYPOINT + CMD                     │
├───────────────────────────────────────────────────────┤
│                                                       │
│  ENTRYPOINT: 컨테이너의 "실행 파일" (고정)               │
│  CMD:        실행 파일의 "기본 인자" (오버라이드 가능)      │
│                                                       │
│  최종 실행: ENTRYPOINT + CMD                            │
│                                                       │
├───────────────────────────────────────────────────────┤
│                                                       │
│  Dockerfile:                                          │
│    ENTRYPOINT ["java", "-jar"]                        │
│    CMD ["app.jar"]                                    │
│                                                       │
│  docker run my-app                                    │
│  → java -jar app.jar                                  │
│                                                       │
│  docker run my-app other.jar                          │
│  → java -jar other.jar  (CMD만 오버라이드)              │
│                                                       │
│  docker run --entrypoint /bin/bash my-app             │
│  → /bin/bash  (ENTRYPOINT 오버라이드)                   │
│                                                       │
└───────────────────────────────────────────────────────┘
```

**Shell form vs Exec form:**

```dockerfile
# Shell form — /bin/sh -c 로 감싸서 실행
CMD java -jar app.jar
# 실제 실행: /bin/sh -c "java -jar app.jar"
# PID 1: /bin/sh (SIGTERM이 java에 전달되지 않음!)

# Exec form — 직접 실행 (권장)
CMD ["java", "-jar", "app.jar"]
# 실제 실행: java -jar app.jar
# PID 1: java (SIGTERM 정상 수신)
```

**Exec form을 사용해야 하는 이유:**
- Graceful shutdown을 위해 SIGTERM이 애플리케이션에 직접 전달되어야 함
- Shell form은 `/bin/sh`가 PID 1이 되어 시그널이 자식 프로세스에 전달되지 않음

### ARG vs ENV

```dockerfile
# ARG: 빌드 시에만 사용, 이미지에 저장되지 않음
ARG JAVA_VERSION=17
FROM openjdk:${JAVA_VERSION}-slim

# ENV: 빌드 + 런타임 모두 사용, 이미지에 저장됨
ENV APP_HOME=/app
WORKDIR ${APP_HOME}

# ARG → ENV 변환 패턴
ARG APP_VERSION=1.0.0
ENV APP_VERSION=${APP_VERSION}
```

```bash
# 빌드 시 ARG 오버라이드
docker build --build-arg JAVA_VERSION=21 -t my-app .

# 런타임 시 ENV 오버라이드
docker run -e SPRING_PROFILES_ACTIVE=dev my-app
```

---

## 4. 실전 예제

### Spring Boot Dockerfile

```dockerfile
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="backend-team@company.com"

# 보안: non-root 사용자
RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -D appuser

WORKDIR /app

# JAR 파일 복사
COPY target/*.jar app.jar

# 포트 선언 (문서화)
EXPOSE 8080

# 소유권 변경
RUN chown -R appuser:appgroup /app

USER appuser

# 헬스체크
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# 실행
ENTRYPOINT ["java"]
CMD ["-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

### Node.js Dockerfile

```dockerfile
FROM node:20-alpine

WORKDIR /app

# 의존성 파일만 먼저 복사 (캐시 활용)
COPY package.json package-lock.json ./
RUN npm ci --only=production

# 소스코드 복사
COPY . .

EXPOSE 3000

# non-root 사용자 (node 이미지에 기본 포함)
USER node

CMD ["node", "server.js"]
```

### HEALTHCHECK 활용

```dockerfile
# HTTP 헬스체크
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1
```

```bash
# 헬스체크 상태 확인
docker ps
# STATUS 컬럼에 (healthy), (unhealthy), (starting) 표시

docker inspect --format='{{json .State.Health}}' my-app
```

---

## 5. 정리

| 개념 | 설명 |
|------|------|
| Dockerfile | 이미지 빌드 명령어 스크립트 |
| Build Context | docker build 시 Daemon에 전송되는 파일 묶음 |
| .dockerignore | 빌드 컨텍스트에서 제외할 파일 패턴 |
| ENTRYPOINT | 컨테이너 실행 파일 (고정, 오버라이드 어려움) |
| CMD | 기본 인자 (docker run 시 오버라이드 가능) |
| Exec form | `["cmd", "arg"]` 형식, PID 1로 직접 실행 (권장) |
| Shell form | `cmd arg` 형식, /bin/sh -c로 감싸서 실행 |
| ARG | 빌드 시에만 사용, 이미지에 미저장 |
| ENV | 빌드 + 런타임 사용, 이미지에 저장 |
| HEALTHCHECK | 컨테이너 헬스 상태 자동 체크 |

---

*참고: Docker Engine 24.x, Dockerfile 1.4+ syntax 기준*
