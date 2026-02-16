# 컨테이너 생명주기

Docker 컨테이너의 상태 전이, 생명주기 관리 명령어, 리소스 제한(cgroups)을 정리한다.

## 목차

- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### 컨테이너 상태

컨테이너는 다음과 같은 상태를 가진다:

```
                    docker create
                         │
                         ▼
┌─────────┐     docker start      ┌──────────┐
│ Created  │ ──────────────────▶  │ Running   │
└─────────┘                      └────┬──────┘
                                      │
                           ┌──────────┼──────────┐
                           │          │          │
                    docker stop   docker pause  docker kill
                           │          │          │
                           ▼          ▼          │
                    ┌──────────┐ ┌─────────┐    │
                    │ Exited   │ │ Paused  │    │
                    └──────────┘ └─────────┘    │
                           │          │          │
                    docker start  docker unpause │
                           │          │          │
                           └──────────┼──────────┘
                                      │
                                      ▼
                               ┌──────────┐
                               │ Running   │
                               └──────────┘
```

| 상태 | 설명 |
|------|------|
| Created | 컨테이너가 생성되었지만 시작되지 않음 |
| Running | 컨테이너가 실행 중 |
| Paused | 컨테이너 프로세스가 일시 중지 (SIGSTOP) |
| Exited | 컨테이너가 종료됨 (exit code 보존) |
| Dead | 비정상 종료 상태 (삭제 필요) |
| Restarting | 재시작 중 |

---

## 2. 왜 알아야 하는가 (Why)

### 운영 안정성

- **Graceful Shutdown**: SIGTERM을 받았을 때 진행 중인 요청을 완료하고 종료해야 한다
- **리소스 누수 방지**: 종료된 컨테이너가 디스크 공간을 차지하지 않도록 관리
- **자동 복구**: restart policy를 통해 장애 시 자동 재시작 구성

### 디버깅 능력

- 컨테이너가 왜 종료되었는지 (exit code 분석)
- 실행 중인 컨테이너 내부 진입 (exec)
- 로그와 이벤트 분석

---

## 3. 내부 구현 분석 (How)

### docker run = create + start

`docker run`은 내부적으로 두 단계로 실행된다:

```
docker run -d --name my-app my-image:latest

내부 동작:
1. docker create my-image:latest  → 컨테이너 생성 (R/W 레이어 생성)
2. docker start <container-id>    → 컨테이너 시작 (프로세스 실행)
```

### 컨테이너 종료 시그널

```
docker stop (Graceful):
    ┌──────────────────────────────────┐
    │ 1. SIGTERM 전송                    │
    │ 2. Grace Period 대기 (기본 10초)    │
    │ 3. 응답 없으면 SIGKILL 전송         │
    └──────────────────────────────────┘

docker kill (Immediate):
    ┌──────────────────────────────────┐
    │ 1. SIGKILL 즉시 전송               │
    │ 2. 프로세스 강제 종료               │
    └──────────────────────────────────┘
```

**Grace Period 커스터마이징:**

```bash
# 30초 대기 후 SIGKILL
docker stop --time 30 my-app
```

### Exit Code 해석

| Exit Code | 의미 |
|-----------|------|
| 0 | 정상 종료 |
| 1 | 일반 에러 (애플리케이션 에러) |
| 126 | 명령어 실행 불가 (권한 문제) |
| 127 | 명령어를 찾을 수 없음 |
| 137 | SIGKILL (kill -9, OOM Killed) |
| 143 | SIGTERM (정상 종료 요청) |
| 255 | 종료 상태를 벗어난 값 |

```bash
# exit code 확인
docker inspect --format='{{.State.ExitCode}}' my-app
```

### Restart Policy

```bash
# 항상 재시작 (수동 stop 제외)
docker run -d --restart=always my-app

# 실패 시만 재시작 (최대 5회)
docker run -d --restart=on-failure:5 my-app

# Docker daemon 시작 시 재시작 (수동 stop 하지 않은 경우)
docker run -d --restart=unless-stopped my-app
```

| 정책 | 설명 |
|------|------|
| no | 재시작 안 함 (기본값) |
| always | 항상 재시작 (docker stop 후 daemon 재시작 시에도) |
| on-failure[:max] | exit code ≠ 0일 때만 재시작, 최대 횟수 지정 가능 |
| unless-stopped | docker stop으로 중지한 경우 제외하고 항상 재시작 |

### 리소스 제한 (cgroups)

```bash
docker run -d \
  --name resource-limited \
  --cpus="1.5"              # CPU 1.5코어 제한
  --cpu-shares=512           # 상대적 CPU 가중치 (기본 1024)
  --memory="512m"            # 메모리 512MB 제한
  --memory-swap="1g"         # 메모리+스왑 합계 1GB
  --memory-reservation="256m" # 소프트 제한 (보장 메모리)
  --pids-limit=100           # 프로세스 수 제한
  my-app
```

**OOM (Out of Memory) 동작:**

```
컨테이너 메모리 사용량이 --memory 한도 초과 시:
1. Linux OOM Killer가 컨테이너 메인 프로세스 SIGKILL
2. 컨테이너 exit code: 137
3. docker inspect → "OOMKilled": true
```

---

## 4. 실전 예제

### 기본 생명주기 관리

```bash
# 컨테이너 생성 (시작하지 않음)
docker create --name my-nginx -p 8080:80 nginx:1.25

# 컨테이너 시작
docker start my-nginx

# 실행 중인 컨테이너 목록
docker ps

# 모든 컨테이너 (종료된 것 포함)
docker ps -a

# 컨테이너 일시 중지 / 재개
docker pause my-nginx
docker unpause my-nginx

# Graceful 종료 (SIGTERM → 10초 → SIGKILL)
docker stop my-nginx

# 삭제
docker rm my-nginx

# 실행 중인 컨테이너 강제 삭제
docker rm -f my-nginx
```

### 컨테이너 내부 디버깅

```bash
# 실행 중인 컨테이너에 셸 접속
docker exec -it my-app /bin/bash

# 특정 명령어 실행
docker exec my-app cat /etc/hosts

# 환경 변수 확인
docker exec my-app env

# 프로세스 목록 확인
docker exec my-app ps aux
```

### 로그 관리

```bash
# 전체 로그
docker logs my-app

# 실시간 로그 (follow)
docker logs -f my-app

# 최근 100줄만
docker logs --tail 100 my-app

# 타임스탬프 포함
docker logs -t my-app

# 특정 시간 이후 로그
docker logs --since "2024-01-01T00:00:00" my-app
```

### 종료된 컨테이너 정리

```bash
# 종료된 컨테이너 모두 삭제
docker container prune

# 전체 정리 (컨테이너, 이미지, 네트워크, 볼륨)
docker system prune -a --volumes
```

---

## 5. 정리

| 개념 | 설명 |
|------|------|
| docker run | create + start를 한 번에 실행 |
| docker stop | SIGTERM → Grace Period → SIGKILL 순으로 종료 |
| docker kill | SIGKILL 즉시 전송 |
| Exit Code 137 | SIGKILL로 종료됨 (OOM Killed 가능성) |
| Restart Policy | 컨테이너 종료 시 자동 재시작 정책 |
| cgroups | CPU, 메모리 등 리소스 제한 커널 기능 |
| OOM Killer | 메모리 한도 초과 시 프로세스 강제 종료 |
| docker exec | 실행 중인 컨테이너에 명령어 실행 |

---

*참고: Docker Engine 24.x 기준*
