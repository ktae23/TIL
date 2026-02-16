# 멀티스테이지 빌드

멀티스테이지 빌드로 빌드 환경과 런타임 환경을 분리하여 경량 프로덕션 이미지를 만드는 방법을 정리한다.

## 목차

- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### 멀티스테이지 빌드란?

하나의 Dockerfile에서 **여러 개의 FROM 문**을 사용하여 빌드 단계(stage)를 분리하는 기법이다. 빌드에 필요한 도구(컴파일러, 빌드 도구 등)와 최종 실행에 필요한 파일을 분리한다.

```
┌──────────────────────────────────────────────────────┐
│                  Single-stage (기존)                    │
│                                                      │
│  FROM openjdk:17                                     │
│  ┌──────────────────────────────────┐                │
│  │  JDK + Maven + 소스코드 + 빌드 캐시  │                │
│  │  + 최종 JAR                        │  → 738MB      │
│  └──────────────────────────────────┘                │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│                  Multi-stage (개선)                     │
│                                                      │
│  Stage 1: builder (빌드)         Stage 2: runtime     │
│  FROM openjdk:17 AS builder     FROM openjdk:17-jre   │
│  ┌───────────────────┐         ┌──────────────┐      │
│  │ JDK + Maven       │  COPY   │  JRE          │      │
│  │ + 소스코드          │ ──────▶ │  + app.jar    │      │
│  │ + 빌드 캐시         │ (JAR만) │              │      │
│  │ + app.jar          │         └──────────────┘      │
│  └───────────────────┘            → 89MB              │
│     (최종 이미지에 포함 안됨)                              │
└──────────────────────────────────────────────────────┘
```

---

## 2. 왜 알아야 하는가 (Why)

### 이미지 크기 차이

| 언어 | Single-stage | Multi-stage | 감소율 |
|------|-------------|-------------|--------|
| Java (Spring Boot) | ~738MB | ~89MB | 88% |
| Go | ~810MB | ~12MB | 99% |
| Node.js | ~950MB | ~120MB | 87% |
| Rust | ~1.3GB | ~15MB | 99% |

### 보안 개선

빌드 스테이지에만 존재하는 것들이 최종 이미지에 포함되지 않는다:
- 컴파일러, 빌드 도구 (공격 도구로 악용 가능)
- 소스코드 (지적 재산 보호)
- 빌드 시 사용한 시크릿 (Private Registry 토큰 등)
- 테스트 코드와 개발 의존성

---

## 3. 내부 구현 분석 (How)

### 기본 구조

```dockerfile
# Stage 1: 빌드 스테이지 (이름: builder)
FROM openjdk:17 AS builder
WORKDIR /build
COPY . .
RUN ./mvnw package -DskipTests

# Stage 2: 런타임 스테이지 (최종 이미지)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/app.jar ./app.jar
CMD ["java", "-jar", "app.jar"]
```

**동작 방식:**
1. `builder` 스테이지에서 전체 빌드 수행
2. `COPY --from=builder`로 빌드 결과물(JAR)만 복사
3. 최종 이미지는 런타임 스테이지만 포함
4. builder 스테이지의 모든 중간 레이어는 버려짐

### COPY --from 옵션

```dockerfile
# 이전 스테이지에서 복사
COPY --from=builder /build/output /app/

# 특정 스테이지 번호로 복사 (0부터 시작)
COPY --from=0 /build/output /app/

# 외부 이미지에서 직접 복사
COPY --from=nginx:1.25 /etc/nginx/nginx.conf /app/nginx.conf
```

### 빌드 캐시와 멀티스테이지

```
첫 번째 빌드:
Stage 1 (builder): 소스코드 변경 → 전체 빌드 (~3분)
Stage 2 (runtime): JAR 복사 → 이미지 생성 (~2초)

두 번째 빌드 (소스코드 변경):
Stage 1 (builder): 의존성 캐시 사용, 컴파일만 재실행 (~30초)
Stage 2 (runtime): JAR 복사 → 이미지 생성 (~2초)
```

### 특정 스테이지만 빌드

```bash
# builder 스테이지까지만 빌드 (CI에서 테스트용)
docker build --target builder -t my-app:test .

# 최종 스테이지 빌드 (기본)
docker build -t my-app:latest .
```

---

## 4. 실전 예제

### Spring Boot 멀티스테이지 빌드 (Layered JAR)

```dockerfile
# === Stage 1: 빌드 ===
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

# Gradle Wrapper 복사 및 의존성 다운로드 (캐시 활용)
COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
RUN ./gradlew dependencies --no-daemon --no-transfer-progress

# 소스코드 복사 및 빌드
COPY src/ src/
RUN ./gradlew bootJar --no-daemon --no-transfer-progress

# Spring Boot Layered JAR 추출
RUN java -Djarmode=layertools -jar build/libs/*.jar extract --destination extracted

# === Stage 2: 런타임 ===
FROM eclipse-temurin:17-jre-alpine

RUN adduser -u 1001 -D appuser
WORKDIR /app

# Layered JAR 순서대로 복사 (캐시 활용)
COPY --from=builder --chown=appuser /build/extracted/dependencies/ ./
COPY --from=builder --chown=appuser /build/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=appuser /build/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser /build/extracted/application/ ./

USER appuser
EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "org.springframework.boot.loader.launch.JarLauncher"]
```

**Spring Boot Layered JAR의 장점:**
- dependencies 레이어는 라이브러리 변경 없으면 캐시 활용
- application 레이어만 자주 변경되므로 빌드/배포 속도 향상

### Go 멀티스테이지 빌드 (scratch 사용)

```dockerfile
# === Stage 1: 빌드 ===
FROM golang:1.22-alpine AS builder
WORKDIR /build

# 의존성 먼저 (캐시 활용)
COPY go.mod go.sum ./
RUN go mod download

# 소스코드 복사 및 빌드
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-s -w" -o server ./cmd/server

# === Stage 2: 런타임 (scratch = 빈 이미지) ===
FROM scratch

# SSL 인증서 복사 (HTTPS 요청용)
COPY --from=builder /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/

# 바이너리 복사
COPY --from=builder /build/server /server

EXPOSE 8080
ENTRYPOINT ["/server"]
```

결과 이미지 크기: **약 10~15MB** (Go 바이너리만 포함)

### Node.js 멀티스테이지 빌드

```dockerfile
# === Stage 1: 빌드 ===
FROM node:20-alpine AS builder
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# === Stage 2: 런타임 ===
FROM node:20-alpine
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --only=production && npm cache clean --force
COPY --from=builder /build/dist ./dist

USER node
EXPOSE 3000
CMD ["node", "dist/server.js"]
```

### 3-스테이지 빌드 (테스트 포함)

```dockerfile
# === Stage 1: 의존성 설치 ===
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

# === Stage 2: 테스트 ===
FROM deps AS test
COPY . .
RUN npm run lint && npm run test

# === Stage 3: 빌드 ===
FROM deps AS builder
COPY . .
RUN npm run build

# === Stage 4: 런타임 ===
FROM node:20-alpine
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --only=production && npm cache clean --force
COPY --from=builder /app/dist ./dist
USER node
CMD ["node", "dist/server.js"]
```

```bash
# 테스트만 실행
docker build --target test .

# 전체 빌드 (테스트 통과 필수)
docker build -t my-app:latest .
```

### distroless 이미지 활용

```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build
COPY . .
RUN ./gradlew bootJar --no-daemon

# distroless: 셸 없음, 패키지 매니저 없음, 최소 런타임만 포함
FROM gcr.io/distroless/java17-debian12
WORKDIR /app
COPY --from=builder /build/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**distroless 특징:**
- 셸 없음 → `docker exec`로 접속 불가
- 패키지 매니저 없음 → 추가 패키지 설치 불가
- 공격 표면 최소화, CVE 수 대폭 감소

---

## 5. 정리

| 개념 | 설명 |
|------|------|
| 멀티스테이지 빌드 | 하나의 Dockerfile에서 여러 FROM으로 빌드/런타임 분리 |
| COPY --from | 이전 스테이지나 외부 이미지에서 파일 복사 |
| --target | 특정 스테이지까지만 빌드 (테스트, 디버깅용) |
| Layered JAR | Spring Boot의 레이어 분리 기능, 캐시 활용 극대화 |
| scratch | 빈 이미지, Go/Rust 정적 바이너리에 적합 |
| distroless | Google 제공 최소 런타임 이미지, 셸 없음 |
| 3-스테이지 | 의존성 → 테스트 → 빌드 → 런타임 분리 패턴 |

---

*참고: Docker Engine 24.x, BuildKit, Spring Boot 3.x Layered JAR 기준*
