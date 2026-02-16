# Docker 핵심 개념

Docker의 기본 아키텍처와 컨테이너 기술의 핵심 개념을 정리한다. 컨테이너와 VM의 차이, Docker 구성 요소, OCI 표준까지 이해한다.

## 목차

- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### Docker란?

Docker는 애플리케이션을 **컨테이너**라는 격리된 환경에서 빌드, 배포, 실행하기 위한 플랫폼이다. 2013년 Solomon Hykes가 dotCloud(현 Docker Inc.)에서 오픈소스로 공개했다.

### 컨테이너 vs VM

```
┌─────────────────────────────────────────────────────────┐
│              컨테이너 (Container)          가상머신 (VM)  │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌──────┐ ┌──────┐ ┌──────┐    ┌──────┐ ┌──────┐      │
│  │App A │ │App B │ │App C │    │App A │ │App B │      │
│  ├──────┤ ├──────┤ ├──────┤    ├──────┤ ├──────┤      │
│  │Bins/ │ │Bins/ │ │Bins/ │    │Bins/ │ │Bins/ │      │
│  │Libs  │ │Libs  │ │Libs  │    │Libs  │ │Libs  │      │
│  └──┬───┘ └──┬───┘ └──┬───┘    ├──────┤ ├──────┤      │
│     │        │        │        │Guest │ │Guest │      │
│     └────────┼────────┘        │  OS  │ │  OS  │      │
│              │                 └──┬───┘ └──┬───┘      │
│     ┌────────┴────────┐          │        │           │
│     │  Container       │     ┌───┴────────┴───┐       │
│     │  Runtime(Docker) │     │   Hypervisor    │       │
│     ├─────────────────┤     ├────────────────┤       │
│     │   Host OS        │     │    Host OS      │       │
│     ├─────────────────┤     ├────────────────┤       │
│     │   Hardware       │     │    Hardware     │       │
│     └─────────────────┘     └────────────────┘       │
└─────────────────────────────────────────────────────────┘
```

| 비교 항목 | 컨테이너 | VM |
|-----------|----------|-----|
| 격리 수준 | 프로세스 수준 (namespace/cgroup) | 하드웨어 수준 (Hypervisor) |
| 시작 시간 | 수 밀리초 ~ 수 초 | 수 십 초 ~ 수 분 |
| 이미지 크기 | 수 MB ~ 수백 MB | 수 GB |
| 성능 오버헤드 | 거의 없음 (네이티브에 가까움) | Hypervisor 오버헤드 존재 |
| OS 공유 | Host OS 커널 공유 | 각 VM마다 독립 OS |
| 밀도 | 하나의 호스트에 수백 개 가능 | 하나의 호스트에 수십 개 |

### 핵심 용어

- **Image**: 컨테이너를 생성하기 위한 읽기 전용 템플릿. 레이어 구조로 구성
- **Container**: 이미지의 실행 인스턴스. 격리된 프로세스
- **Registry**: 이미지를 저장/배포하는 저장소 (Docker Hub, ECR, GCR 등)
- **Dockerfile**: 이미지를 빌드하기 위한 명령어 스크립트
- **Docker Compose**: 멀티 컨테이너 애플리케이션 정의/실행 도구

---

## 2. 왜 알아야 하는가 (Why)

### 개발 환경의 문제

```
개발자 A의 로컬: Java 17, MySQL 8.0, Redis 7.0
개발자 B의 로컬: Java 11, MySQL 5.7, Redis 6.2
CI 서버:        Java 17, MySQL 8.0, Redis 6.2
운영 서버:       Java 17, MySQL 8.0, Redis 7.0
```

**"내 컴퓨터에서는 되는데..."** 문제의 근본 원인은 환경 차이다.

### Docker가 해결하는 것

1. **환경 일관성**: 개발/테스트/운영 환경을 동일하게 유지
2. **빠른 온보딩**: `docker-compose up` 한 줄로 전체 개발 환경 구성
3. **격리된 실행**: 서비스 간 의존성 충돌 방지
4. **재현 가능한 빌드**: Dockerfile로 빌드 과정을 코드화
5. **마이크로서비스 배포**: 각 서비스를 독립적으로 빌드/배포

---

## 3. 내부 구현 분석 (How)

### Docker 아키텍처

```
┌─────────────────────────────────────────────────────┐
│                    Docker Client                     │
│                (docker CLI / API)                    │
│                                                     │
│  docker build    docker pull    docker run           │
│      │               │              │               │
└──────┼───────────────┼──────────────┼───────────────┘
       │               │              │
       ▼               ▼              ▼
┌─────────────────────────────────────────────────────┐
│                  Docker Daemon (dockerd)              │
│                                                     │
│  ┌───────────┐  ┌───────────┐  ┌──────────────┐    │
│  │  Images    │  │ Containers│  │  Networks     │    │
│  │  Manager   │  │  Manager  │  │  Manager      │    │
│  └─────┬─────┘  └─────┬─────┘  └──────────────┘    │
│        │              │                              │
│        ▼              ▼                              │
│  ┌─────────────────────────────┐                    │
│  │       containerd             │                    │
│  │  (container runtime)         │                    │
│  └──────────┬──────────────────┘                    │
│             │                                        │
│             ▼                                        │
│  ┌─────────────────────────────┐                    │
│  │         runc                 │                    │
│  │  (OCI runtime)               │                    │
│  └──────────────────────────────┘                    │
└─────────────────────────────────────────────────────┘
```

**구성 요소:**

| 구성 요소 | 역할 |
|-----------|------|
| Docker CLI | 사용자 명령어 인터페이스. REST API로 daemon과 통신 |
| Docker Daemon (dockerd) | 이미지, 컨테이너, 네트워크, 볼륨 관리 |
| containerd | 컨테이너 생명주기 관리 (CNCF 프로젝트) |
| runc | OCI 표준 기반 실제 컨테이너 실행 |

### Linux 커널 기술 기반

Docker 컨테이너는 두 가지 Linux 커널 기능에 의존한다:

**1. Namespace (격리)**

| Namespace | 격리 대상 |
|-----------|----------|
| PID | 프로세스 ID 공간 |
| NET | 네트워크 인터페이스, 라우팅 테이블 |
| MNT | 파일시스템 마운트 포인트 |
| UTS | 호스트명, 도메인명 |
| IPC | System V IPC, POSIX 메시지 큐 |
| USER | UID/GID 매핑 |
| CGROUP | cgroup 루트 디렉토리 |

**2. Control Groups (cgroups) — 리소스 제한**

```bash
# 컨테이너에 CPU 1코어, 메모리 512MB 제한
docker run --cpus="1.0" --memory="512m" nginx
```

cgroups가 제한하는 리소스:
- CPU 사용량
- 메모리 사용량
- 디스크 I/O
- 네트워크 대역폭

### OCI (Open Container Initiative)

Docker가 사실상 표준이 되면서 2015년 Linux Foundation 산하에 OCI가 설립되었다.

- **Runtime Specification**: 컨테이너 실행 방법 표준 (runc가 참조 구현)
- **Image Specification**: 이미지 형식 표준
- **Distribution Specification**: 이미지 배포 방법 표준

이 덕분에 Docker 외에도 Podman, containerd, CRI-O 등 다양한 컨테이너 런타임이 호환된다.

---

## 4. 실전 예제

### Docker 설치 확인

```bash
# Docker 버전 확인
docker version

# Docker 시스템 정보
docker info

# Hello World 실행
docker run hello-world
```

### 기본 명령어 흐름

```bash
# 1. 이미지 다운로드
docker pull nginx:1.25

# 2. 컨테이너 실행
docker run -d --name my-nginx -p 8080:80 nginx:1.25

# 3. 실행 중인 컨테이너 확인
docker ps

# 4. 컨테이너 로그 확인
docker logs my-nginx

# 5. 컨테이너 내부 접속
docker exec -it my-nginx /bin/bash

# 6. 컨테이너 중지 및 삭제
docker stop my-nginx
docker rm my-nginx

# 7. 이미지 삭제
docker rmi nginx:1.25
```

### 리소스 제한 실행 예제

```bash
# CPU 0.5코어, 메모리 256MB 제한으로 실행
docker run -d \
  --name limited-app \
  --cpus="0.5" \
  --memory="256m" \
  --memory-swap="512m" \
  nginx:1.25

# 리소스 사용량 실시간 모니터링
docker stats limited-app
```

---

## 5. 정리

| 개념 | 설명 |
|------|------|
| Docker | 컨테이너 기반 애플리케이션 빌드/배포/실행 플랫폼 |
| Container | Host OS 커널을 공유하는 격리된 프로세스 |
| Image | 컨테이너 생성을 위한 읽기 전용 레이어 템플릿 |
| Namespace | 프로세스 격리를 위한 Linux 커널 기능 |
| cgroups | 리소스 제한을 위한 Linux 커널 기능 |
| OCI | 컨테이너 런타임/이미지/배포 표준 |
| containerd | CNCF 컨테이너 런타임 (Docker 내부에서 사용) |
| runc | OCI 표준 참조 구현 런타임 |

---

*참고: Docker Engine 24.x, containerd 1.7.x 기준*
