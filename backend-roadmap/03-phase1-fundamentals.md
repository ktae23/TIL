# Phase 1: 기초 단계 - Backend 개발 입문

백엔드 개발의 첫 단계로, 프로그래밍 언어의 기본기를 다지고 데이터베이스와 웹 프레임워크를 익힙니다.

## 목차

- [1. 프로그래밍 언어 선택](#1-프로그래밍-언어-선택)
- [2. 패키지 매니저](#2-패키지-매니저)
- [3. 관계형 데이터베이스](#3-관계형-데이터베이스)
- [4. 웹 프레임워크](#4-웹-프레임워크)
- [5. 기초 단계 체크리스트](#5-기초-단계-체크리스트)

---

## 1. 프로그래밍 언어 선택

### 왜 하나의 언어에 집중해야 하는가?

여러 언어를 얕게 아는 것보다 **하나의 언어를 깊이 있게 마스터**하는 것이 중요합니다. 언어의 철학, 생태계, 베스트 프랙티스를 이해하면 다른 언어로 전환할 때도 빠르게 적응할 수 있습니다.

### 언어별 상세 비교

#### Java

```java
// Java의 특징을 보여주는 간단한 예시
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    }
}
```

| 항목 | 내용 |
|------|------|
| **강점** | 정적 타입, 강력한 생태계, 대규모 시스템에 적합 |
| **약점** | 보일러플레이트 코드가 많음, 러닝커브 |
| **주요 사용처** | 대기업, 금융권, 안드로이드 앱 |
| **프레임워크** | Spring Boot, Spring MVC |
| **빌드 도구** | Maven, Gradle |
| **취업 시장** | 국내 취업에 가장 유리 |

#### Python

```python
# Python의 간결함을 보여주는 예시
from fastapi import FastAPI, HTTPException

app = FastAPI()

@app.get("/users/{user_id}")
async def get_user(user_id: int):
    user = await user_repository.find_by_id(user_id)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    return user
```

| 항목 | 내용 |
|------|------|
| **강점** | 배우기 쉬움, 간결한 문법, 다양한 활용 분야 |
| **약점** | 상대적으로 느린 속도, 동적 타입의 단점 |
| **주요 사용처** | 스타트업, 데이터 분석, AI/ML, 자동화 |
| **프레임워크** | Django, FastAPI, Flask |
| **패키지 매니저** | pip, Poetry, pipenv |
| **취업 시장** | AI/데이터 분야에서 강세 |

#### JavaScript (Node.js)

```javascript
// Node.js + Express 예시
const express = require('express');
const app = express();

app.get('/users/:id', async (req, res) => {
    try {
        const user = await UserService.findById(req.params.id);
        res.json(user);
    } catch (error) {
        res.status(404).json({ error: 'User not found' });
    }
});
```

| 항목 | 내용 |
|------|------|
| **강점** | 프론트엔드와 언어 통일, 비동기 처리에 강함 |
| **약점** | 콜백 지옥(개선됨), 타입 안정성 부족(TypeScript로 해결) |
| **주요 사용처** | 풀스택, 실시간 애플리케이션, API 서버 |
| **프레임워크** | Express.js, NestJS, Fastify |
| **패키지 매니저** | npm, yarn, pnpm |
| **취업 시장** | 스타트업, 풀스택 포지션 |

#### Go

```go
// Go의 간결하고 명시적인 에러 처리
func (s *UserService) FindById(id int64) (*User, error) {
    user, err := s.repository.FindById(id)
    if err != nil {
        return nil, fmt.Errorf("user not found: %w", err)
    }
    return user, nil
}
```

| 항목 | 내용 |
|------|------|
| **강점** | 빠른 컴파일, 뛰어난 동시성, 단순한 문법 |
| **약점** | 제네릭 지원 부족(개선 중), 생태계가 상대적으로 작음 |
| **주요 사용처** | 클라우드 인프라, 마이크로서비스, CLI 도구 |
| **프레임워크** | Gin, Echo, Fiber |
| **패키지 매니저** | Go Modules |
| **취업 시장** | 클라우드/인프라 분야에서 수요 증가 |

### 언어 선택 가이드

```
취업 목적 (국내 대기업/금융) → Java
├── Spring 생태계가 압도적
└── 채용 공고 가장 많음

빠른 프로토타이핑/스타트업 → Python or Node.js
├── 개발 속도 빠름
└── 유연한 환경

고성능/클라우드 네이티브 → Go
├── Docker, Kubernetes가 Go로 작성됨
└── 마이크로서비스에 적합

풀스택 개발 → JavaScript/TypeScript
├── 프론트엔드와 언어 통일
└── 하나의 언어로 전체 스택 커버
```

---

## 2. 패키지 매니저

패키지 매니저는 외부 라이브러리를 관리하고 프로젝트 의존성을 처리하는 도구입니다.

### Java: Maven vs Gradle

#### Maven

```xml
<!-- pom.xml -->
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>my-app</artifactId>
    <version>1.0.0</version>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <version>3.2.0</version>
        </dependency>
    </dependencies>
</project>
```

#### Gradle

```groovy
// build.gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

| 비교 항목 | Maven | Gradle |
|----------|-------|--------|
| 설정 파일 | XML (pom.xml) | Groovy/Kotlin DSL |
| 빌드 속도 | 상대적으로 느림 | 빠름 (증분 빌드) |
| 유연성 | 제한적 | 높음 |
| 러닝커브 | 낮음 | 중간 |
| 추천 | 레거시 프로젝트 | 신규 프로젝트 |

### Python: pip vs Poetry

#### pip + requirements.txt

```bash
# 패키지 설치
pip install fastapi uvicorn

# 의존성 저장
pip freeze > requirements.txt

# 의존성 설치
pip install -r requirements.txt
```

#### Poetry (권장)

```bash
# 프로젝트 초기화
poetry init

# 패키지 추가
poetry add fastapi uvicorn

# 개발 의존성 추가
poetry add --dev pytest

# 의존성 설치
poetry install
```

```toml
# pyproject.toml
[tool.poetry.dependencies]
python = "^3.11"
fastapi = "^0.104.0"
uvicorn = "^0.24.0"

[tool.poetry.dev-dependencies]
pytest = "^7.4.0"
```

### Node.js: npm vs yarn vs pnpm

```bash
# npm
npm init -y
npm install express
npm install --save-dev jest

# yarn
yarn init -y
yarn add express
yarn add --dev jest

# pnpm (디스크 효율적)
pnpm init
pnpm add express
pnpm add -D jest
```

```json
// package.json
{
  "name": "my-app",
  "version": "1.0.0",
  "dependencies": {
    "express": "^4.18.2"
  },
  "devDependencies": {
    "jest": "^29.7.0"
  },
  "scripts": {
    "start": "node index.js",
    "test": "jest"
  }
}
```

---

## 3. 관계형 데이터베이스

### 왜 관계형 DB부터 배워야 하는가?

1. **데이터 무결성**: ACID 트랜잭션으로 데이터 일관성 보장
2. **표준화된 언어**: SQL은 어디서나 통용
3. **실무 기반**: 대부분의 서비스가 RDBMS 사용
4. **NoSQL 이해의 기반**: RDB를 알아야 NoSQL의 장단점을 이해

### PostgreSQL vs MySQL

| 비교 항목 | PostgreSQL | MySQL |
|----------|------------|-------|
| 표준 SQL 준수 | 높음 | 중간 |
| 고급 기능 | JSON, 배열, 전문 검색 | 기본적인 기능 |
| 성능 | 복잡한 쿼리에 강함 | 단순 읽기에 강함 |
| 확장성 | 뛰어남 | 보통 |
| 추천 | 신규 프로젝트 | 레거시/단순 프로젝트 |

### SQL 기초 마스터하기

#### 테이블 생성 (DDL)

```sql
-- 사용자 테이블
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 게시글 테이블 (외래 키 관계)
CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(200) NOT NULL,
    content TEXT,
    view_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX idx_posts_user_id ON posts(user_id);
CREATE INDEX idx_posts_created_at ON posts(created_at DESC);
```

#### CRUD 작업 (DML)

```sql
-- CREATE: 데이터 삽입
INSERT INTO users (email, password_hash, name)
VALUES ('user@example.com', 'hashed_password', '홍길동');

-- READ: 데이터 조회
-- 단일 조회
SELECT * FROM users WHERE id = 1;

-- 조건부 조회
SELECT id, email, name
FROM users
WHERE status = 'ACTIVE'
ORDER BY created_at DESC
LIMIT 10;

-- JOIN 조회
SELECT
    p.id,
    p.title,
    u.name AS author_name,
    p.created_at
FROM posts p
INNER JOIN users u ON p.user_id = u.id
WHERE p.created_at > NOW() - INTERVAL '7 days'
ORDER BY p.created_at DESC;

-- UPDATE: 데이터 수정
UPDATE users
SET name = '김철수', updated_at = NOW()
WHERE id = 1;

-- DELETE: 데이터 삭제
DELETE FROM posts WHERE id = 1;

-- Soft Delete 패턴 (권장)
UPDATE posts
SET status = 'DELETED', deleted_at = NOW()
WHERE id = 1;
```

#### 집계와 그룹화

```sql
-- 사용자별 게시글 수
SELECT
    u.name,
    COUNT(p.id) AS post_count
FROM users u
LEFT JOIN posts p ON u.id = p.user_id
GROUP BY u.id, u.name
HAVING COUNT(p.id) > 5
ORDER BY post_count DESC;

-- 일별 게시글 통계
SELECT
    DATE(created_at) AS date,
    COUNT(*) AS daily_posts,
    SUM(view_count) AS total_views
FROM posts
WHERE created_at > NOW() - INTERVAL '30 days'
GROUP BY DATE(created_at)
ORDER BY date DESC;
```

#### 트랜잭션

```sql
-- 트랜잭션 예시: 포인트 이체
BEGIN;

-- 보내는 사람 잔액 차감
UPDATE accounts
SET balance = balance - 1000
WHERE user_id = 1 AND balance >= 1000;

-- 받는 사람 잔액 증가
UPDATE accounts
SET balance = balance + 1000
WHERE user_id = 2;

-- 이체 기록 저장
INSERT INTO transfers (from_user, to_user, amount)
VALUES (1, 2, 1000);

COMMIT;
-- 에러 발생 시: ROLLBACK;
```

---

## 4. 웹 프레임워크

### 프레임워크의 역할

웹 프레임워크는 HTTP 요청/응답 처리, 라우팅, 미들웨어 등을 추상화하여 비즈니스 로직에 집중할 수 있게 해줍니다.

### Spring Boot (Java)

```java
// 컨트롤러
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        User user = userService.create(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(UserResponse.from(user));
    }
}

// 서비스
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User create(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        return userRepository.save(request.toEntity());
    }
}
```

### FastAPI (Python)

```python
from fastapi import FastAPI, HTTPException, Depends
from pydantic import BaseModel, EmailStr
from sqlalchemy.orm import Session

app = FastAPI()

class UserCreate(BaseModel):
    email: EmailStr
    name: str
    password: str

class UserResponse(BaseModel):
    id: int
    email: str
    name: str

    class Config:
        from_attributes = True

@app.get("/api/users/{user_id}", response_model=UserResponse)
async def get_user(user_id: int, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    return user

@app.post("/api/users", response_model=UserResponse, status_code=201)
async def create_user(user: UserCreate, db: Session = Depends(get_db)):
    db_user = User(**user.dict())
    db.add(db_user)
    db.commit()
    db.refresh(db_user)
    return db_user
```

### Express.js (Node.js)

```javascript
const express = require('express');
const { body, validationResult } = require('express-validator');

const app = express();
app.use(express.json());

// 미들웨어
const asyncHandler = (fn) => (req, res, next) => {
    Promise.resolve(fn(req, res, next)).catch(next);
};

// 라우트
app.get('/api/users/:id', asyncHandler(async (req, res) => {
    const user = await UserService.findById(req.params.id);
    if (!user) {
        return res.status(404).json({ error: 'User not found' });
    }
    res.json(user);
}));

app.post('/api/users',
    body('email').isEmail(),
    body('name').notEmpty(),
    asyncHandler(async (req, res) => {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({ errors: errors.array() });
        }

        const user = await UserService.create(req.body);
        res.status(201).json(user);
    })
);

// 에러 핸들러
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({ error: 'Internal server error' });
});
```

---

## 5. 기초 단계 체크리스트

### 언어 기초
- [ ] 변수, 자료형, 연산자
- [ ] 조건문, 반복문
- [ ] 함수/메서드 정의
- [ ] 클래스와 객체지향 기본
- [ ] 예외 처리
- [ ] 컬렉션/자료구조 활용

### 패키지 매니저
- [ ] 패키지 설치/제거
- [ ] 의존성 파일 관리
- [ ] 버전 관리 이해
- [ ] 스크립트 실행

### 데이터베이스
- [ ] 테이블 설계 (정규화 기초)
- [ ] CRUD 쿼리 작성
- [ ] JOIN 이해 및 활용
- [ ] 인덱스 기본 개념
- [ ] 트랜잭션 이해

### 웹 프레임워크
- [ ] 프로젝트 구조 이해
- [ ] 라우팅 설정
- [ ] 요청/응답 처리
- [ ] JSON 다루기
- [ ] 에러 핸들링

### 추천 프로젝트

**간단한 TODO API 만들기**
- 할 일 CRUD API 구현
- 데이터베이스 연동
- 입력값 검증
- 에러 처리

---

*마지막 업데이트: 2026년 01월*
