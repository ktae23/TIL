# 이미지와 레이어 아키텍처

Docker 이미지의 레이어 구조와 Union Filesystem의 동작 원리를 이해한다. 레이어 캐싱을 활용한 빌드 최적화까지 다룬다.

## 목차

- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### Docker 이미지란?

Docker 이미지는 컨테이너를 실행하기 위한 **읽기 전용 파일 시스템 스냅샷**이다. 여러 개의 **레이어(Layer)**가 순서대로 쌓여 하나의 파일 시스템을 형성한다.

### 레이어 구조

```
┌─────────────────────────────────────────┐
│         Container Layer (R/W)            │  ← 컨테이너 실행 시 추가
├─────────────────────────────────────────┤
│  Layer 5: CMD ["java", "-jar", ...]      │  ← Dockerfile 명령어마다
├─────────────────────────────────────────┤
│  Layer 4: COPY app.jar /app/             │     레이어 1개 생성
├─────────────────────────────────────────┤
│  Layer 3: RUN apt-get install -y curl    │
├─────────────────────────────────────────┤
│  Layer 2: RUN apt-get update             │
├─────────────────────────────────────────┤
│  Layer 1: FROM ubuntu:22.04              │  ← Base Image
└─────────────────────────────────────────┘
```

**핵심 규칙:**
- 각 레이어는 **이전 레이어 위의 변경 사항(diff)**만 저장
- 이미지 레이어는 **읽기 전용(Read-Only)**
- 컨테이너 실행 시 **읽기/쓰기(R/W) 레이어**가 최상위에 추가
- 레이어는 **SHA256 해시**로 식별, 동일 레이어는 재사용

---

## 2. 왜 알아야 하는가 (Why)

### 빌드 속도 최적화

레이어 구조를 모르면 매번 전체 이미지를 처음부터 빌드하게 된다.

```dockerfile
# 나쁜 예: 소스코드 변경 시 의존성도 매번 다시 설치
COPY . /app
RUN npm install

# 좋은 예: package.json 변경 없으면 npm install 캐시 활용
COPY package.json package-lock.json /app/
RUN npm install
COPY . /app
```

### 이미지 크기 최적화

불필요한 레이어나 파일이 포함되면 이미지 크기가 비대해진다.

```
잘못된 이미지: 1.2GB (빌드 도구, 캐시, 임시 파일 포함)
최적화된 이미지: 150MB (런타임만 포함)
```

### 저장소 비용 절감

레이어가 공유되면 Registry 저장 비용과 네트워크 전송량이 줄어든다.

---

## 3. 내부 구현 분석 (How)

### Union Filesystem (OverlayFS)

Docker는 기본적으로 **OverlayFS**를 사용한다. 여러 디렉토리를 하나의 파일 시스템처럼 합쳐서 보여주는 기술이다.

```
┌──────────────────────────────────────────────┐
│              Merged (통합 뷰)                  │
│     /app/server.jar  /etc/nginx.conf          │
│     /var/log/app.log                          │
├──────────────────────────────────────────────┤
│   Upper Layer (R/W)  │   Lower Layers (R/O)  │
│                      │                        │
│  /var/log/app.log    │  /app/server.jar       │
│  (새로 생성된 파일)    │  /etc/nginx.conf       │
│                      │  (이미지 레이어)         │
└──────────────────────┴────────────────────────┘
```

**Copy-on-Write (CoW) 전략:**

1. **읽기**: Lower Layer에서 파일을 그대로 읽음
2. **수정**: Lower Layer의 파일을 Upper Layer로 복사한 뒤 수정
3. **삭제**: Upper Layer에 **whiteout 파일**을 생성하여 가림
4. **생성**: Upper Layer에 직접 새 파일 생성

### 레이어 캐싱 메커니즘

```
Dockerfile 빌드 시 캐시 판단 흐름:

┌──────────────────────────────┐
│ 명령어(instruction) 읽기      │
└──────────┬───────────────────┘
           ▼
┌──────────────────────────────┐
│ 이전 빌드에 동일 명령어의      │
│ 캐시 레이어가 있는가?          │
└──────────┬───────────────────┘
       Yes │        │ No
           ▼        ▼
    ┌──────────┐  ┌───────────────┐
    │캐시 사용  │  │ 새 레이어 생성  │
    │(CACHED)  │  │ (이후 모든     │
    └──────────┘  │ 레이어 캐시    │
                  │ 무효화)        │
                  └───────────────┘
```

**캐시 무효화 규칙:**
- `COPY`/`ADD`: 파일 내용의 체크섬(checksum)이 변경되면 무효화
- `RUN`: 명령어 문자열이 변경되면 무효화
- 한 레이어의 캐시가 무효화되면 **그 이후 모든 레이어**의 캐시도 무효화

### 이미지 Manifest

이미지는 **manifest** 파일로 레이어 정보를 관리한다:

```json
{
  "schemaVersion": 2,
  "mediaType": "application/vnd.oci.image.manifest.v1+json",
  "config": {
    "mediaType": "application/vnd.oci.image.config.v1+json",
    "digest": "sha256:abc123...",
    "size": 7023
  },
  "layers": [
    {
      "mediaType": "application/vnd.oci.image.layer.v1.tar+gzip",
      "digest": "sha256:layer1...",
      "size": 32654
    },
    {
      "mediaType": "application/vnd.oci.image.layer.v1.tar+gzip",
      "digest": "sha256:layer2...",
      "size": 16724
    }
  ]
}
```

---

## 4. 실전 예제

### 이미지 레이어 분석

```bash
# 이미지 히스토리 확인 (각 레이어별 명령어와 크기)
docker history nginx:1.25

# 출력 예시:
# IMAGE          CREATED       CREATED BY                                      SIZE
# 7d3c40f240e1   2 weeks ago   CMD ["nginx" "-g" "daemon off;"]                0B
# <missing>      2 weeks ago   STOPSIGNAL SIGQUIT                              0B
# <missing>      2 weeks ago   EXPOSE map[80/tcp:{}]                           0B
# <missing>      2 weeks ago   ENTRYPOINT ["/docker-entrypoint.sh"]            0B
# <missing>      2 weeks ago   COPY 30-tune-worker-processes.sh ... 4.62kB    4.62kB
# <missing>      2 weeks ago   RUN /bin/sh -c set -x ...                      61.1MB
# <missing>      2 weeks ago   /bin/sh -c #(nop) ADD file:... in /            77.8MB
```

```bash
# 이미지 상세 정보 (JSON 형식)
docker inspect nginx:1.25

# 이미지 크기 확인
docker images nginx:1.25 --format "{{.Repository}}:{{.Tag}} - {{.Size}}"
```

### 레이어 캐싱 효과 비교

```dockerfile
# === 캐시 비효율적 Dockerfile ===
FROM node:20-alpine
WORKDIR /app
COPY . .                    # 소스코드 변경 → 전체 캐시 무효화
RUN npm ci                  # 매번 다시 실행
CMD ["node", "server.js"]
```

```dockerfile
# === 캐시 최적화 Dockerfile ===
FROM node:20-alpine
WORKDIR /app
COPY package.json package-lock.json ./    # 의존성 파일만 먼저
RUN npm ci                                # 의존성 변경 없으면 캐시 사용
COPY . .                                  # 소스코드는 마지막에
CMD ["node", "server.js"]
```

```bash
# 빌드 시간 비교 (두 번째 빌드, 소스코드만 변경 시)
# 캐시 비효율: ~45초 (npm ci 매번 실행)
# 캐시 최적화: ~3초 (npm ci 캐시 사용)
```

### dangling 이미지 정리

```bash
# 태그 없는(dangling) 이미지 확인
docker images -f "dangling=true"

# dangling 이미지 삭제
docker image prune

# 사용하지 않는 모든 이미지 삭제
docker image prune -a

# 디스크 사용량 전체 확인
docker system df
```

---

## 5. 정리

| 개념 | 설명 |
|------|------|
| Layer | Dockerfile 명령어마다 생성되는 읽기 전용 파일 시스템 diff |
| Union FS (OverlayFS) | 여러 레이어를 하나의 파일 시스템으로 합쳐 보여주는 기술 |
| Copy-on-Write | 수정 시에만 Upper Layer로 복사하는 전략, 저장 공간 절약 |
| 레이어 캐싱 | 동일 명령어+동일 입력이면 기존 레이어 재사용, 빌드 속도 향상 |
| 캐시 무효화 | 한 레이어 변경 시 이후 모든 레이어 캐시 무효화 |
| whiteout 파일 | OverlayFS에서 Lower Layer 파일 삭제를 표현하는 특수 파일 |
| Manifest | 이미지의 레이어 목록과 설정을 담은 메타데이터 파일 |

---

*참고: Docker Engine 24.x, OverlayFS (overlay2 storage driver) 기준*
