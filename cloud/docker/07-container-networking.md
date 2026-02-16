# 컨테이너 네트워킹

Docker 네트워크의 종류(bridge, host, overlay), DNS 서비스 디스커버리, 포트 매핑, 네트워크 격리를 정리한다.

## 목차

- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### Docker 네트워크 드라이버

| 드라이버 | 설명 | 사용 시점 |
|----------|------|----------|
| bridge | 동일 호스트 내 컨테이너 간 통신 (기본값) | 로컬 개발, 단일 호스트 |
| host | 호스트 네트워크 직접 사용 | 성능 최우선, 포트 매핑 불필요 |
| overlay | 여러 Docker 호스트 간 네트워크 | Swarm, 멀티 호스트 클러스터 |
| none | 네트워크 비활성화 | 완전 격리 필요 시 |
| macvlan | 컨테이너에 물리 MAC 주소 할당 | 레거시 시스템 연동 |

---

## 2. 왜 알아야 하는가 (Why)

### 마이크로서비스 통신

```
docker-compose 환경에서:

┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Spring Boot │────▶│    Redis     │     │   MySQL      │
│  :8080       │     │  :6379       │     │  :3306       │
│              │────▶│              │     │              │
│              │────────────────────────▶│              │
└─────────────┘     └─────────────┘     └─────────────┘

서비스 간 통신은 "컨테이너 이름"으로 가능: redis:6379, mysql:3306
```

### 네트워크 격리

프론트엔드와 백엔드를 서로 다른 네트워크에 배치하여 보안 강화:

```
frontend-net:  nginx ←→ react-app
                         ↕
backend-net:   react-app ←→ spring-boot ←→ mysql
```

nginx가 mysql에 직접 접근할 수 없다.

---

## 3. 내부 구현 분석 (How)

### Bridge 네트워크 (기본)

```
┌─────────────────────────────────────────────────────┐
│                    Host Machine                      │
│                                                     │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐      │
│  │Container │    │Container │    │Container │      │
│  │  A       │    │  B       │    │  C       │      │
│  │172.17.0.2│    │172.17.0.3│    │172.17.0.4│      │
│  └────┬─────┘    └────┬─────┘    └────┬─────┘      │
│       │               │               │             │
│  ─────┴───────────────┴───────────────┴─────        │
│           docker0 bridge (172.17.0.1)               │
│                       │                              │
│               ┌───────┴───────┐                      │
│               │   iptables    │                      │
│               │   NAT/FORWARD │                      │
│               └───────┬───────┘                      │
│                       │                              │
│  ─────────────────────┴─────────────────────        │
│                   eth0 (호스트 NIC)                   │
└─────────────────────────────────────────────────────┘
```

**기본 bridge (docker0):**
- Docker 설치 시 자동 생성
- 컨테이너 간 IP 통신 가능
- DNS 서비스 디스커버리 **없음** (IP만 사용)

**사용자 정의 bridge:**
- `docker network create`로 생성
- DNS 서비스 디스커버리 **지원** (컨테이너 이름으로 통신)
- 네트워크 격리 가능

### DNS 서비스 디스커버리

```
사용자 정의 bridge 네트워크 내부:

Container A → "redis" 로 DNS 질의
     │
     ▼
Docker 내장 DNS 서버 (127.0.0.11)
     │
     ▼
"redis" → 172.18.0.3 (Container B의 IP)
     │
     ▼
Container A → 172.18.0.3:6379 연결
```

**중요**: 기본 bridge(docker0)에서는 DNS가 작동하지 않는다. 반드시 사용자 정의 네트워크를 사용해야 한다.

### 포트 매핑 (-p)

```bash
docker run -p 8080:80 nginx
#           │    │
#           │    └── 컨테이너 포트
#           └─────── 호스트 포트
```

```
외부 요청:  localhost:8080
               │
               ▼
iptables DNAT: 8080 → 172.17.0.2:80
               │
               ▼
컨테이너 nginx: 80번 포트에서 수신
```

**포트 매핑 옵션:**

```bash
# 호스트 8080 → 컨테이너 80
-p 8080:80

# 호스트 임의 포트 → 컨테이너 80
-p 80

# 특정 IP에만 바인딩
-p 127.0.0.1:8080:80

# UDP 포트
-p 8080:80/udp

# 포트 범위
-p 8080-8090:80-90
```

### Host 네트워크

```bash
docker run --network host nginx
# 컨테이너가 호스트의 네트워크 스택을 직접 사용
# 포트 매핑 불필요, nginx는 호스트의 80번 포트에서 직접 수신
# 네트워크 성능 최대 (NAT 오버헤드 없음)
# macOS/Windows에서는 VM 기반이라 제한적
```

---

## 4. 실전 예제

### 사용자 정의 네트워크 생성 및 사용

```bash
# 네트워크 생성
docker network create --driver bridge my-network

# 특정 서브넷으로 생성
docker network create \
  --driver bridge \
  --subnet 172.20.0.0/16 \
  --gateway 172.20.0.1 \
  my-network

# 네트워크에 컨테이너 연결
docker run -d --name redis --network my-network redis:7
docker run -d --name app --network my-network my-app

# app 컨테이너에서 redis에 DNS로 접근 가능
docker exec app ping redis
# PING redis (172.20.0.2): 56 data bytes
```

### 네트워크 격리 (멀티 네트워크)

```bash
# 프론트엔드 네트워크
docker network create frontend-net

# 백엔드 네트워크
docker network create backend-net

# nginx: 프론트엔드만
docker run -d --name nginx --network frontend-net -p 80:80 nginx

# app: 프론트엔드 + 백엔드 (양쪽 연결)
docker run -d --name app --network frontend-net my-app
docker network connect backend-net app

# db: 백엔드만
docker run -d --name db --network backend-net mysql:8

# nginx → app: 통신 가능 (frontend-net)
# app → db:    통신 가능 (backend-net)
# nginx → db:  통신 불가 (네트워크 격리!)
```

### 네트워크 진단 명령어

```bash
# 네트워크 목록
docker network ls

# 네트워크 상세 정보 (연결된 컨테이너, 서브넷 등)
docker network inspect my-network

# 컨테이너의 네트워크 정보 확인
docker inspect --format='{{json .NetworkSettings.Networks}}' my-app

# 컨테이너 간 연결 테스트
docker exec app ping redis
docker exec app nslookup redis
docker exec app curl http://api:8080/health

# 사용하지 않는 네트워크 정리
docker network prune
```

### docker-compose에서의 네트워크

```yaml
# docker-compose.yml
services:
  app:
    image: my-app
    networks:
      - frontend
      - backend

  nginx:
    image: nginx
    ports:
      - "80:80"
    networks:
      - frontend

  mysql:
    image: mysql:8
    networks:
      - backend

  redis:
    image: redis:7
    networks:
      - backend

networks:
  frontend:
    driver: bridge
  backend:
    driver: bridge
    # 커스텀 서브넷 지정
    ipam:
      config:
        - subnet: 172.28.0.0/16
```

docker-compose는 기본적으로 `{프로젝트명}_default` 네트워크를 자동 생성하며, 모든 서비스가 해당 네트워크에 연결된다. 명시적으로 networks를 정의하면 격리가 가능하다.

---

## 5. 정리

| 개념 | 설명 |
|------|------|
| bridge | 동일 호스트 내 컨테이너 통신, 가장 일반적 |
| host | 호스트 네트워크 직접 사용, NAT 없음, 최고 성능 |
| overlay | 멀티 호스트 간 컨테이너 통신 (Swarm) |
| 사용자 정의 bridge | DNS 서비스 디스커버리 지원 (컨테이너 이름 통신) |
| 기본 bridge (docker0) | DNS 미지원, IP로만 통신 |
| 포트 매핑 (-p) | 호스트 포트 → 컨테이너 포트 iptables NAT |
| DNS (127.0.0.11) | Docker 내장 DNS 서버, 사용자 정의 네트워크에서 작동 |
| 네트워크 격리 | 서로 다른 네트워크의 컨테이너는 통신 불가 |
| docker network connect | 실행 중인 컨테이너를 네트워크에 추가 연결 |

---

*참고: Docker Engine 24.x, bridge/host 네트워크 드라이버 기준*
