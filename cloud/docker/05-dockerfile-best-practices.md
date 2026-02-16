# Dockerfile Best Practices

프로덕션 수준의 Dockerfile을 작성하기 위한 핵심 원칙과 최적화 기법을 정리한다. 레이어 최소화, 캐시 활용, 이미지 크기 최적화에 집중한다.

## 목차

- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### Dockerfile 최적화의 세 축

```
┌───────────────────────────────────────────────────┐
│              Dockerfile 최적화                      │
│                                                   │
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐    │
│  │ 빌드 속도 │  │ 이미지    │  │   보안        │    │
│  │          │  │ 크기     │  │              │    │
│  │ - 캐시    │  │ - 경량   │  │ - non-root   │    │
│  │   활용   │  │   베이스 │  │ - 최소 패키지  │    │
│  │ - 레이어  │  │ - 불필요 │  │ - 시크릿 관리  │    │
│  │   순서   │  │   파일   │  │              │    │
│  │          │  │   제거   │  │              │    │
│  └──────────┘  └──────────┘  └──────────────┘    │
└───────────────────────────────────────────────────┘
```

---

## 2. 왜 알아야 하는가 (Why)

### 실제 영향

```
최적화 전:                    최적화 후:
─────────                   ─────────
이미지 크기: 1.2GB           이미지 크기: 89MB
빌드 시간: 5분               빌드 시간: 30초 (캐시 히트)
보안 취약점: 347개           보안 취약점: 12개
배포 시간: 3분               배포 시간: 20초
```

- **CI/CD 비용**: 빌드 시간 = 비용. 매일 수십 번 빌드하면 큰 차이
- **배포 속도**: 이미지가 작을수록 pull 시간 단축, 롤백 속도 향상
- **보안**: 패키지가 적을수록 공격 표면(attack surface) 축소

---

## 3. 내부 구현 분석 (How)

### Practice 1: 적절한 베이스 이미지 선택

```
┌──────────────────────────────────────────────────┐
│ 이미지 태그별 크기 비교 (Java 17 기준)              │
│                                                  │
│  openjdk:17           ~470MB  ████████████████   │
│  openjdk:17-slim      ~220MB  ████████           │
│  eclipse-temurin:17   ~340MB  ████████████       │
│  eclipse-temurin:     ~190MB  ███████            │
│    17-jre-alpine                                 │
│  distroless/java17    ~190MB  ███████            │
│  (gcr.io)                                        │
└──────────────────────────────────────────────────┘
```

| 베이스 이미지 | 장점 | 단점 | 사용 시점 |
|--------------|------|------|----------|
| ubuntu/debian | 패키지 풍부 | 크기 큼 | 디버깅, 개발 |
| alpine | 매우 가벼움 (5MB) | musl libc (호환성 주의) | 프로덕션 |
| slim | 불필요한 패키지 제거 | alpine보다 큼 | 호환성 필요할 때 |
| distroless | 셸 없음, 최소 구성 | 디버깅 어려움 | 보안 최우선 |
| scratch | 빈 이미지 | 아무것도 없음 | Go 바이너리 등 |

### Practice 2: 레이어 캐시 최적화

**원칙: 변경 빈도가 낮은 것을 위에, 높은 것을 아래에 배치**

```dockerfile
# === 캐시 최적화 순서 ===

# 1. 거의 변하지 않음: 베이스 이미지
FROM eclipse-temurin:17-jre-alpine

# 2. 드물게 변함: 시스템 패키지
RUN apk add --no-cache curl

# 3. 가끔 변함: 의존성 파일
COPY build.gradle settings.gradle ./
COPY gradle/ gradle/
RUN ./gradlew dependencies --no-daemon

# 4. 자주 변함: 소스코드
COPY src/ src/
RUN ./gradlew build --no-daemon

# 5. 거의 변하지 않음: 실행 설정
EXPOSE 8080
CMD ["java", "-jar", "build/libs/app.jar"]
```

### Practice 3: RUN 명령어 최적화

```dockerfile
# 나쁜 예: 레이어 3개 생성, 중간 레이어에 캐시 잔류
RUN apt-get update
RUN apt-get install -y curl wget git
RUN apt-get clean

# 좋은 예: 레이어 1개, 캐시 정리 포함
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
      curl \
      wget \
      git && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*
```

**--no-install-recommends**: 권장 패키지를 설치하지 않아 크기 절약

### Practice 4: .dockerignore 활용

```
# .dockerignore

# 버전 관리
.git
.gitignore

# IDE
.idea
.vscode
*.iml

# 빌드 산출물 (이미지 내에서 빌드할 경우)
target/
build/
node_modules/
dist/

# 문서, 테스트
*.md
docs/
tests/
__tests__/

# 환경 설정 (보안)
.env
.env.*
*.pem
*.key

# OS 파일
.DS_Store
Thumbs.db

# Docker
Dockerfile*
docker-compose*
```

### Practice 5: non-root 사용자

```dockerfile
# Alpine 기반
RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -D -s /sbin/nologin appuser

# Debian/Ubuntu 기반
RUN groupadd -g 1001 appgroup && \
    useradd -u 1001 -g appgroup -r -s /usr/sbin/nologin appuser

# 파일 소유권 설정
COPY --chown=appuser:appgroup target/app.jar /app/

USER appuser
```

### Practice 6: 빌드 시크릿 보호

```dockerfile
# 나쁜 예: 시크릿이 레이어에 영구 저장
COPY .npmrc /app/.npmrc
RUN npm install
RUN rm /app/.npmrc          # 삭제해도 이전 레이어에 남아있음!

# 좋은 예: BuildKit secret mount
# syntax=docker/dockerfile:1
RUN --mount=type=secret,id=npmrc,target=/app/.npmrc \
    npm install
# secret은 레이어에 저장되지 않음
```

```bash
# 빌드 시 시크릿 전달
DOCKER_BUILDKIT=1 docker build \
  --secret id=npmrc,src=.npmrc \
  -t my-app .
```

### Practice 7: COPY vs ADD

```dockerfile
# COPY: 로컬 파일 복사 (권장)
COPY config/application.yml /app/config/

# ADD: 자동 압축 해제가 필요할 때만 사용
ADD archive.tar.gz /app/

# ADD는 URL 다운로드도 가능하지만 권장하지 않음
# 대신 RUN curl/wget 사용 (캐시 제어 가능)
RUN curl -fsSL https://example.com/file -o /app/file
```

---

## 4. 실전 예제

### 최적화 전/후 비교 (Spring Boot)

```dockerfile
# === 최적화 전: 738MB ===
FROM openjdk:17
COPY . /app
WORKDIR /app
RUN ./mvnw package -DskipTests
CMD ["java", "-jar", "target/app.jar"]
```

```dockerfile
# === 최적화 후: 89MB ===
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:resolve --no-transfer-progress
COPY src src
RUN ./mvnw package -DskipTests --no-transfer-progress && \
    java -Djarmode=layertools -jar target/app.jar extract

FROM eclipse-temurin:17-jre-alpine
RUN adduser -u 1001 -D appuser
WORKDIR /app
COPY --from=builder --chown=appuser /app/dependencies/ ./
COPY --from=builder --chown=appuser /app/spring-boot-loader/ ./
COPY --from=builder --chown=appuser /app/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser /app/application/ ./
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

### hadolint를 활용한 Dockerfile 린팅

```bash
# hadolint 설치 (macOS)
brew install hadolint

# Dockerfile 검사
hadolint Dockerfile

# 출력 예시:
# Dockerfile:3 DL3008 Pin versions in apt get install
# Dockerfile:7 DL3025 Use arguments JSON notation for CMD and ENTRYPOINT
# Dockerfile:5 SC2046 Quote this to prevent word splitting
```

### dive로 이미지 레이어 분석

```bash
# dive 설치 (macOS)
brew install dive

# 이미지 레이어별 크기 분석
dive my-app:latest

# 각 레이어에서 추가/수정/삭제된 파일 확인 가능
# 불필요한 파일이 포함된 레이어 식별
```

---

## 5. 정리

| Practice | 핵심 |
|----------|------|
| 경량 베이스 이미지 | alpine, slim, distroless 사용 |
| 레이어 캐시 | 변경 빈도 낮은 것을 위에 배치 |
| RUN 합치기 | && 로 연결, 한 레이어에서 설치+정리 |
| .dockerignore | 빌드 컨텍스트에서 불필요한 파일 제외 |
| non-root | USER 명령어로 비특권 사용자 실행 |
| 시크릿 보호 | BuildKit --mount=type=secret 사용 |
| COPY 우선 | ADD는 압축 해제 필요할 때만 사용 |
| Exec form | CMD/ENTRYPOINT는 JSON 배열 형식 |
| hadolint | Dockerfile 정적 분석 도구 |
| dive | 이미지 레이어 시각적 분석 도구 |

---

*참고: Docker Engine 24.x, BuildKit, hadolint v2.x 기준*
