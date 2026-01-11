# Backend 개발자 학습 로드맵

백엔드 개발자가 되기 위해 배워야 할 기술 스택과 학습 경로를 정리한 로드맵입니다.

## 목차

- [백엔드 개발이란?](#백엔드-개발이란)
- [백엔드 개발자의 주요 역할](#백엔드-개발자의-주요-역할)
- [학습 로드맵](#학습-로드맵)
  - [Phase 1: 기초 단계](#phase-1-기초-단계)
  - [Phase 2: 중급 단계](#phase-2-중급-단계)
  - [Phase 3: 고급 단계](#phase-3-고급-단계)
- [기술 스택 정리](#기술-스택-정리)
- [참고 자료](#참고-자료)

---

## 백엔드 개발이란?

**백엔드 개발(Backend Development)** 은 웹 개발의 서버 측면을 담당하는 분야로, 서버 로직, 데이터베이스, API 생성 및 관리에 집중합니다.

사용자가 직접 보는 프론트엔드와 달리, 백엔드는 **"보이지 않는 곳에서 동작하는 시스템"** 을 구축합니다.

---

## 백엔드 개발자의 주요 역할

| 역할 | 설명 |
|------|------|
| **서버 사이드 컴포넌트 개발** | 애플리케이션의 핵심 비즈니스 로직 구현 |
| **API 개발** | 클라이언트와 서버 간 데이터 통신을 위한 인터페이스 설계 |
| **데이터베이스 관리** | 데이터 저장, 조회, 수정, 삭제 작업 처리 |
| **트래픽 처리** | 대용량 트래픽을 효율적으로 처리하는 시스템 설계 |
| **외부 서비스 연동** | 결제 게이트웨이, 클라우드 솔루션 등 통합 |
| **성능 최적화** | 시스템 성능 및 확장성 향상 |
| **보안** | 데이터 처리 및 보안 강화 |

---

## 학습 로드맵

> 각 단계별 상세 문서를 참고하세요:
> - [Phase 1: 기초 단계](./phase1-fundamentals.md) - 언어, DB, 프레임워크
> - [Phase 2: 중급 단계](./phase2-intermediate.md) - API 설계, 인증, Git, 테스트
> - [Phase 3: 고급 단계](./phase3-advanced.md) - 아키텍처, 캐싱, DevOps

### Phase 1: 기초 단계

백엔드 개발의 첫 단계로, 프로그래밍 언어와 기본 개념을 익힙니다. [상세 보기 →](./phase1-fundamentals.md)

#### 1. 프로그래밍 언어 선택

하나의 언어를 깊이 있게 학습하는 것이 중요합니다.

| 언어 | 특징 | 추천 대상 |
|------|------|-----------|
| **Java** | 대기업, 금융권에서 많이 사용. 안정적이고 생태계가 풍부 | 취업 목적, 대규모 시스템 |
| **Python** | 배우기 쉽고, 다양한 분야에 활용 가능 | 입문자, 데이터/AI 관심자 |
| **JavaScript (Node.js)** | 프론트엔드와 백엔드 모두 가능 | 풀스택 지향 |
| **Go** | 간결하고 빠른 성능. 동시성 처리에 강점 | 고성능 시스템 |
| **C#** | .NET 생태계, 게임 서버 개발에 강점 | Microsoft 환경 |

#### 2. 패키지 매니저

선택한 언어의 패키지 매니저를 익힙니다.

- **Java**: Maven, Gradle
- **Python**: pip, Poetry
- **Node.js**: npm, yarn
- **Go**: Go Modules

#### 3. 관계형 데이터베이스 (RDBMS)

데이터베이스의 기본인 관계형 DB를 학습합니다.

```sql
-- 기본 CRUD 예시
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 조회
SELECT * FROM users WHERE id = 1;

-- 삽입
INSERT INTO users (name, email) VALUES ('홍길동', 'hong@example.com');

-- 수정
UPDATE users SET name = '김철수' WHERE id = 1;

-- 삭제
DELETE FROM users WHERE id = 1;
```

**추천 데이터베이스**: PostgreSQL (기능이 풍부하고 표준 SQL 준수)

#### 4. 웹 프레임워크

선택한 언어에 맞는 웹 프레임워크를 학습합니다.

| 언어 | 프레임워크 | 특징 |
|------|------------|------|
| Java | **Spring Boot** | 엔터프라이즈 표준, 풍부한 생태계 |
| Python | **Django**, FastAPI | Django는 풀스택, FastAPI는 고성능 API |
| Node.js | **Express.js**, NestJS | Express는 미니멀, NestJS는 구조화된 프레임워크 |
| Go | **Gin**, Echo | 경량화되고 빠른 성능 |

---

### Phase 2: 중급 단계

실무에서 필요한 핵심 기술들을 익힙니다. [상세 보기 →](./phase2-intermediate.md)

#### 1. RESTful API 설계

REST 원칙에 따른 API 설계 방법을 학습합니다.

```
# RESTful API 설계 예시

GET    /users          # 사용자 목록 조회
GET    /users/{id}     # 특정 사용자 조회
POST   /users          # 사용자 생성
PUT    /users/{id}     # 사용자 전체 수정
PATCH  /users/{id}     # 사용자 부분 수정
DELETE /users/{id}     # 사용자 삭제
```

**핵심 원칙**:
- URI는 명사를 사용 (동사 X)
- HTTP 메서드로 행위를 표현
- 적절한 상태 코드 반환 (200, 201, 400, 404, 500 등)

#### 2. 인증/인가 (Authentication/Authorization)

| 방식 | 설명 | 사용 사례 |
|------|------|-----------|
| **Session/Cookie** | 서버에서 세션 관리 | 전통적인 웹 애플리케이션 |
| **JWT** | 토큰 기반 인증, Stateless | REST API, 마이크로서비스 |
| **OAuth 2.0** | 소셜 로그인, 제3자 인증 | 구글/카카오 로그인 |

#### 3. 버전 관리 (Git)

```bash
# 기본 Git 워크플로우
git clone <repository>
git checkout -b feature/new-feature
git add .
git commit -m "feat: 새 기능 추가"
git push origin feature/new-feature
```

**필수 학습 내용**:
- Branch 전략 (Git Flow, GitHub Flow)
- Merge vs Rebase
- Conflict 해결

#### 4. 실전 프로젝트

학습한 내용을 종합하여 프로젝트를 진행합니다.

**추천 프로젝트**:
- 게시판 CRUD API
- 사용자 인증 시스템
- 간단한 쇼핑몰 백엔드

---

### Phase 3: 고급 단계

대규모 시스템 설계와 운영에 필요한 기술을 학습합니다. [상세 보기 →](./phase3-advanced.md)

#### 1. 아키텍처 패턴

| 패턴 | 설명 | 장단점 |
|------|------|--------|
| **모놀리식** | 단일 애플리케이션 | 단순하지만 확장성 제한 |
| **마이크로서비스** | 서비스별 분리 | 확장성 좋지만 복잡도 증가 |
| **서버리스** | FaaS 기반 | 관리 부담 감소, Cold Start 이슈 |
| **SOA** | 서비스 지향 아키텍처 | 엔터프라이즈 환경에 적합 |

#### 2. 메시지 브로커

서비스 간 비동기 통신을 위한 메시지 큐를 학습합니다.

- **RabbitMQ**: 전통적인 메시지 브로커
- **Apache Kafka**: 대용량 이벤트 스트리밍
- **Redis Pub/Sub**: 간단한 실시간 메시징

#### 3. 캐싱 전략

```
Client → CDN → API Gateway → Application Cache → Database
```

| 캐시 유형 | 도구 | 용도 |
|-----------|------|------|
| **애플리케이션 캐시** | Redis, Memcached | 세션, 자주 조회되는 데이터 |
| **HTTP 캐시** | Nginx, Varnish | 정적 리소스 |
| **CDN** | CloudFront, Cloudflare | 글로벌 콘텐츠 배포 |

#### 4. 데이터베이스 확장

- **수직 확장 (Scale Up)**: 서버 스펙 증가
- **수평 확장 (Scale Out)**: 서버 추가
- **Read Replica**: 읽기 전용 복제본
- **Sharding**: 데이터 분산 저장

#### 5. DevOps 기초

```yaml
# Docker 예시
FROM openjdk:17-slim
COPY target/app.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**필수 도구**:
- **Docker**: 컨테이너화
- **Kubernetes**: 컨테이너 오케스트레이션
- **CI/CD**: GitHub Actions, Jenkins, GitLab CI
- **모니터링**: Prometheus, Grafana, ELK Stack

---

## 기술 스택 정리

### 언어
Python, Java, Go, JavaScript (Node.js), Ruby, PHP, C#, Rust

### 데이터베이스
- **RDBMS**: PostgreSQL, MySQL
- **NoSQL**: MongoDB, Redis, Elasticsearch, DynamoDB

### 프레임워크
Django, Spring Boot, Express.js, Rails, Laravel, FastAPI, NestJS

### 인프라/도구
Docker, Kubernetes, CI/CD, 모니터링 솔루션

---

## 참고 자료

- [Backend Developer Roadmap](https://roadmap.sh/backend) - 공식 로드맵 사이트
- [Spring 공식 문서](https://spring.io/docs)
- [PostgreSQL 공식 문서](https://www.postgresql.org/docs/)

---

*마지막 업데이트: 2026년 01월*
