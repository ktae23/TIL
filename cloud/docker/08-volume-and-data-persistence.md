# 볼륨과 데이터 영속성

Docker에서 데이터를 영속적으로 관리하는 방법을 정리한다. Named Volume, Bind Mount, tmpfs의 차이와 실전 백업/복원 전략까지 다룬다.

## 목차

- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### 컨테이너의 데이터 문제

컨테이너는 본질적으로 **임시적(ephemeral)**이다. 컨테이너가 삭제되면 R/W 레이어에 기록된 모든 데이터도 함께 삭제된다.

```
컨테이너 생명주기와 데이터:

docker run mysql → 데이터 저장 → docker rm mysql → 데이터 소실!
```

### 데이터 영속성 방법 3가지

```
┌─────────────────────────────────────────────────────────┐
│                     Host Machine                         │
│                                                         │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │ Named Volume │  │ Bind Mount   │  │   tmpfs       │   │
│  │             │  │              │  │              │   │
│  │ Docker이     │  │ 호스트의       │  │ 호스트 메모리  │   │
│  │ 관리하는     │  │ 특정 디렉토리  │  │ 에만 저장      │   │
│  │ 저장 영역    │  │ 마운트         │  │ (디스크 없음)  │   │
│  └──────┬──────┘  └──────┬───────┘  └──────┬───────┘   │
│         │               │                │             │
│         └───────────────┼────────────────┘             │
│                         │                               │
│                ┌────────▼────────┐                      │
│                │   Container     │                      │
│                │  /var/lib/mysql │                      │
│                └─────────────────┘                      │
└─────────────────────────────────────────────────────────┘
```

| 유형 | 호스트 경로 | Docker 관리 | 영속성 | 사용 시점 |
|------|------------|-------------|--------|----------|
| Named Volume | `/var/lib/docker/volumes/{name}/_data` | O | O | DB, 앱 데이터 |
| Bind Mount | 사용자 지정 경로 | X | O | 소스코드 마운트, 설정 파일 |
| tmpfs | 메모리 | X | X | 임시 데이터, 시크릿 |

---

## 2. 왜 알아야 하는가 (Why)

### 데이터베이스 영속성

```
시나리오: MySQL 컨테이너 업그레이드

볼륨 없이:
mysql:8.0 → docker rm → 데이터 소실 → mysql:8.1 → 빈 DB

볼륨 사용:
mysql:8.0 (데이터: mysql-data 볼륨)
→ docker rm
→ mysql:8.1 (동일 볼륨 마운트)
→ 데이터 유지!
```

### 개발 환경 효율

Bind Mount로 호스트의 소스코드를 컨테이너에 실시간 반영:

```bash
docker run -v $(pwd)/src:/app/src my-app
# 호스트에서 코드 수정 → 컨테이너에 즉시 반영 (핫 리로드)
```

---

## 3. 내부 구현 분석 (How)

### Named Volume

```bash
# 볼륨 생성
docker volume create mysql-data

# 볼륨으로 컨테이너 실행
docker run -d \
  --name mysql \
  -v mysql-data:/var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=secret \
  mysql:8

# 볼륨 정보 확인
docker volume inspect mysql-data
```

```json
{
  "CreatedAt": "2024-01-01T00:00:00Z",
  "Driver": "local",
  "Labels": {},
  "Mountpoint": "/var/lib/docker/volumes/mysql-data/_data",
  "Name": "mysql-data",
  "Options": {},
  "Scope": "local"
}
```

**Named Volume 특징:**
- Docker가 생성/관리하는 저장 영역
- 컨테이너 삭제 시에도 볼륨은 유지
- 볼륨 드라이버로 NFS, AWS EBS 등 원격 스토리지 지원
- `docker volume rm`으로 명시적 삭제 필요

### Bind Mount

```bash
# 호스트 디렉토리를 컨테이너에 마운트
docker run -d \
  --name dev-app \
  -v /Users/dev/project/src:/app/src \
  -v /Users/dev/project/config:/app/config:ro \
  my-app

# :ro = read-only (컨테이너에서 수정 불가)
```

**Bind Mount 특징:**
- 호스트의 절대 경로를 지정
- 호스트 파일 시스템에 직접 접근
- 파일 변경이 양방향으로 즉시 반영
- Docker가 관리하지 않음 (docker volume ls에 안 나옴)

### tmpfs Mount

```bash
# 메모리에만 데이터 저장 (디스크에 쓰지 않음)
docker run -d \
  --name secure-app \
  --tmpfs /run/secrets:rw,noexec,nosuid,size=64m \
  my-app
```

**tmpfs 특징:**
- 메모리에만 저장, 컨테이너 종료 시 삭제
- 디스크에 쓰지 않으므로 민감 데이터(시크릿, 토큰)에 적합
- Linux에서만 사용 가능

### --mount vs -v 구문

```bash
# -v (짧은 형식)
docker run -v mysql-data:/var/lib/mysql mysql:8

# --mount (명시적 형식, 권장)
docker run --mount type=volume,source=mysql-data,target=/var/lib/mysql mysql:8

# --mount bind mount
docker run --mount type=bind,source=/host/path,target=/container/path,readonly my-app

# --mount tmpfs
docker run --mount type=tmpfs,target=/tmp,tmpfs-size=100m my-app
```

**-v와 --mount의 차이:**
- `-v`: 볼륨이 없으면 자동 생성
- `--mount`: 볼륨이 없으면 에러 발생 (더 안전)

---

## 4. 실전 예제

### 데이터베이스 볼륨 관리

```bash
# MySQL 데이터 볼륨
docker volume create mysql-data

docker run -d \
  --name mysql \
  --mount type=volume,source=mysql-data,target=/var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=secret \
  -e MYSQL_DATABASE=myapp \
  mysql:8

# MySQL 업그레이드 (데이터 유지)
docker stop mysql && docker rm mysql
docker run -d \
  --name mysql \
  --mount type=volume,source=mysql-data,target=/var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=secret \
  mysql:8.1
```

### 볼륨 백업/복원

```bash
# 백업: 볼륨 데이터를 tar 파일로 추출
docker run --rm \
  -v mysql-data:/data:ro \
  -v $(pwd)/backup:/backup \
  alpine \
  tar czf /backup/mysql-data-backup.tar.gz -C /data .

# 복원: tar 파일에서 볼륨으로 복원
docker volume create mysql-data-restored

docker run --rm \
  -v mysql-data-restored:/data \
  -v $(pwd)/backup:/backup:ro \
  alpine \
  tar xzf /backup/mysql-data-backup.tar.gz -C /data
```

### 개발 환경 Bind Mount

```bash
# Spring Boot 개발 환경 (소스코드 핫 리로드)
docker run -d \
  --name dev-app \
  -v $(pwd)/src:/app/src \
  -v $(pwd)/build.gradle:/app/build.gradle:ro \
  -p 8080:8080 \
  my-dev-image

# Node.js 개발 환경
docker run -d \
  --name dev-frontend \
  -v $(pwd)/src:/app/src \
  -v /app/node_modules \
  -p 3000:3000 \
  my-node-dev
```

**주의**: `-v /app/node_modules`는 **anonymous volume**으로, 컨테이너의 node_modules가 호스트의 빈 디렉토리로 덮어씌워지는 것을 방지한다.

### docker-compose에서의 볼륨

```yaml
services:
  mysql:
    image: mysql:8
    volumes:
      - mysql-data:/var/lib/mysql          # Named Volume
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql:ro  # Bind Mount
    environment:
      MYSQL_ROOT_PASSWORD: secret

  redis:
    image: redis:7
    volumes:
      - redis-data:/data
    command: redis-server --appendonly yes  # AOF 영속성 활성화

  app:
    image: my-app
    volumes:
      - ./src:/app/src                     # 개발용 Bind Mount
      - app-logs:/app/logs                 # 로그 볼륨

volumes:
  mysql-data:                              # Docker가 관리
    driver: local
  redis-data:
    driver: local
  app-logs:
    driver: local
```

### 볼륨 정리

```bash
# 모든 볼륨 목록
docker volume ls

# 사용하지 않는 볼륨 (어떤 컨테이너에도 연결되지 않은) 삭제
docker volume prune

# 특정 볼륨 삭제
docker volume rm mysql-data

# 전체 정리 (볼륨 포함)
docker system prune --volumes
```

---

## 5. 정리

| 개념 | 설명 |
|------|------|
| Named Volume | Docker 관리 저장 영역, DB 데이터에 적합 |
| Bind Mount | 호스트 경로 직접 마운트, 개발 환경에 적합 |
| tmpfs | 메모리 저장, 민감 데이터에 적합 |
| --mount | 명시적 마운트 구문 (볼륨 미존재 시 에러, 안전) |
| -v | 짧은 마운트 구문 (볼륨 미존재 시 자동 생성) |
| :ro | 읽기 전용 마운트 |
| Anonymous Volume | 이름 없는 볼륨, 컨테이너 내부 경로 보호용 |
| docker volume prune | 미사용 볼륨 일괄 삭제 |

---

*참고: Docker Engine 24.x, local volume driver 기준*
