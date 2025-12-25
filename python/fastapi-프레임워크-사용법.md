# FastAPI 프레임워크 사용법

FastAPI를 사용한 고성능 API 개발의 핵심 개념과 실용적인 코드 예제를 정리한 문서입니다.

## 목차

1. [설치 및 기본 설정](#설치-및-기본-설정)
2. [라우팅과 경로 매개변수](#라우팅과-경로-매개변수)
3. [Pydantic을 활용한 요청/응답 검증](#pydantic을-활용한-요청응답-검증)
4. [의존성 주입 (Dependency Injection)](#의존성-주입-dependency-injection)
5. [비동기 프로그래밍](#비동기-프로그래밍)
6. [데이터베이스 연동](#데이터베이스-연동)
7. [인증과 보안](#인증과-보안)
8. [자동 문서화](#자동-문서화)
9. [배포](#배포)

---

## 설치 및 기본 설정

### 설치

```bash
# 가상환경 생성
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# FastAPI 및 Uvicorn 설치
pip install fastapi uvicorn[standard]

# 추가 패키지
pip install pydantic-settings sqlalchemy databases[sqlite]
```

### 기본 앱 구조

```python
# main.py
from fastapi import FastAPI

app = FastAPI(
    title="My API",
    description="API 설명",
    version="1.0.0"
)

@app.get("/")
async def root():
    return {"message": "Hello, FastAPI!"}

@app.get("/health")
async def health_check():
    return {"status": "healthy"}
```

```bash
# 실행
uvicorn main:app --reload

# 자동 문서 확인
# http://localhost:8000/docs (Swagger UI)
# http://localhost:8000/redoc (ReDoc)
```

---

## 라우팅과 경로 매개변수

### 기본 라우팅

```python
from fastapi import FastAPI, Path, Query
from typing import Optional

app = FastAPI()

# 경로 매개변수
@app.get("/items/{item_id}")
async def read_item(item_id: int):
    return {"item_id": item_id}

# 경로 매개변수 검증
@app.get("/users/{user_id}")
async def read_user(
    user_id: int = Path(..., title="사용자 ID", ge=1, le=1000)
):
    return {"user_id": user_id}

# 쿼리 매개변수
@app.get("/search")
async def search(
    q: str = Query(..., min_length=1, max_length=50),
    page: int = Query(1, ge=1),
    size: int = Query(10, ge=1, le=100)
):
    return {
        "query": q,
        "page": page,
        "size": size,
        "results": []
    }

# Optional 쿼리 매개변수
@app.get("/filter")
async def filter_items(
    category: Optional[str] = None,
    min_price: Optional[float] = None,
    max_price: Optional[float] = None
):
    filters = {}
    if category:
        filters["category"] = category
    if min_price:
        filters["min_price"] = min_price
    if max_price:
        filters["max_price"] = max_price

    return {"filters": filters}
```

---

## Pydantic을 활용한 요청/응답 검증

### Request Body 모델

```python
from pydantic import BaseModel, Field, EmailStr, validator
from typing import Optional, List
from datetime import datetime

class UserCreate(BaseModel):
    username: str = Field(..., min_length=3, max_length=50)
    email: EmailStr
    password: str = Field(..., min_length=8)
    age: int = Field(..., ge=0, le=150)

    @validator('username')
    def username_alphanumeric(cls, v):
        assert v.isalnum(), '사용자명은 영숫자만 가능합니다'
        return v

class UserResponse(BaseModel):
    id: int
    username: str
    email: str
    created_at: datetime

    class Config:
        orm_mode = True  # ORM 객체를 Pydantic 모델로 변환

# 사용 예제
@app.post("/users", response_model=UserResponse, status_code=201)
async def create_user(user: UserCreate):
    # 실제로는 DB에 저장
    new_user = {
        "id": 1,
        "username": user.username,
        "email": user.email,
        "created_at": datetime.now()
    }
    return new_user
```

### 중첩 모델

```python
class Address(BaseModel):
    street: str
    city: str
    country: str
    postal_code: str

class UserProfile(BaseModel):
    username: str
    email: EmailStr
    address: Address
    tags: List[str] = []

@app.post("/profiles")
async def create_profile(profile: UserProfile):
    return profile

# 요청 예시:
# {
#   "username": "john",
#   "email": "john@example.com",
#   "address": {
#     "street": "123 Main St",
#     "city": "Seoul",
#     "country": "Korea",
#     "postal_code": "12345"
#   },
#   "tags": ["developer", "python"]
# }
```

### Response Model 활용

```python
from typing import List

class Item(BaseModel):
    id: int
    name: str
    description: Optional[str] = None
    price: float
    tax: Optional[float] = None

@app.get("/items/{item_id}", response_model=Item)
async def get_item(item_id: int):
    # DB에서 조회
    return {
        "id": item_id,
        "name": "Sample Item",
        "price": 29.99,
        "tax": 2.99
    }

@app.get("/items", response_model=List[Item])
async def list_items(skip: int = 0, limit: int = 10):
    # DB에서 목록 조회
    items = [
        {"id": i, "name": f"Item {i}", "price": i * 10.0}
        for i in range(skip, skip + limit)
    ]
    return items
```

---

## 의존성 주입 (Dependency Injection)

### 기본 의존성

```python
from fastapi import Depends, HTTPException, Header

# 공통 쿼리 매개변수
async def common_parameters(
    skip: int = 0,
    limit: int = 10,
    q: Optional[str] = None
):
    return {"skip": skip, "limit": limit, "q": q}

@app.get("/items")
async def read_items(commons: dict = Depends(common_parameters)):
    return commons

# 헤더 검증
async def verify_token(x_token: str = Header(...)):
    if x_token != "secret-token":
        raise HTTPException(status_code=401, detail="Invalid token")
    return x_token

@app.get("/protected")
async def protected_route(token: str = Depends(verify_token)):
    return {"message": "You have access", "token": token}
```

### 클래스 기반 의존성

```python
from typing import Optional

class Pagination:
    def __init__(
        self,
        page: int = Query(1, ge=1),
        size: int = Query(10, ge=1, le=100)
    ):
        self.page = page
        self.size = size
        self.skip = (page - 1) * size

@app.get("/users")
async def get_users(pagination: Pagination = Depends()):
    return {
        "page": pagination.page,
        "size": pagination.size,
        "skip": pagination.skip,
        "users": []
    }
```

### 데이터베이스 세션 의존성

```python
from sqlalchemy.orm import Session

# 데이터베이스 세션 생성
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

@app.get("/users/{user_id}")
async def get_user(
    user_id: int,
    db: Session = Depends(get_db)
):
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    return user
```

---

## 비동기 프로그래밍

### async/await 사용

```python
import asyncio
import httpx

@app.get("/sync")
def sync_endpoint():
    # 동기 함수
    return {"type": "sync"}

@app.get("/async")
async def async_endpoint():
    # 비동기 함수
    return {"type": "async"}

# 외부 API 호출
@app.get("/external-data")
async def fetch_external_data():
    async with httpx.AsyncClient() as client:
        response = await client.get("https://api.example.com/data")
        return response.json()

# 여러 비동기 작업 병렬 실행
@app.get("/parallel")
async def parallel_requests():
    async with httpx.AsyncClient() as client:
        tasks = [
            client.get("https://api.example.com/users"),
            client.get("https://api.example.com/posts"),
            client.get("https://api.example.com/comments")
        ]
        results = await asyncio.gather(*tasks)
        return {
            "users": results[0].json(),
            "posts": results[1].json(),
            "comments": results[2].json()
        }
```

### 백그라운드 작업

```python
from fastapi import BackgroundTasks

def send_email(email: str, message: str):
    # 이메일 발송 로직 (시간이 걸리는 작업)
    print(f"Sending email to {email}: {message}")

@app.post("/send-notification")
async def send_notification(
    email: str,
    background_tasks: BackgroundTasks
):
    background_tasks.add_task(send_email, email, "Welcome!")
    return {"message": "Notification will be sent in background"}
```

---

## 데이터베이스 연동

### SQLAlchemy 설정

```python
# database.py
from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker

SQLALCHEMY_DATABASE_URL = "sqlite:///./app.db"

engine = create_engine(
    SQLALCHEMY_DATABASE_URL,
    connect_args={"check_same_thread": False}
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()
```

### 모델 정의

```python
# models.py
from sqlalchemy import Column, Integer, String, DateTime, ForeignKey
from sqlalchemy.orm import relationship
from datetime import datetime
from database import Base

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String, unique=True, index=True)
    email = Column(String, unique=True, index=True)
    hashed_password = Column(String)
    created_at = Column(DateTime, default=datetime.utcnow)

    posts = relationship("Post", back_populates="author")

class Post(Base):
    __tablename__ = "posts"

    id = Column(Integer, primary_key=True, index=True)
    title = Column(String, index=True)
    content = Column(String)
    author_id = Column(Integer, ForeignKey("users.id"))
    created_at = Column(DateTime, default=datetime.utcnow)

    author = relationship("User", back_populates="posts")
```

### CRUD 작업

```python
# crud.py
from sqlalchemy.orm import Session
from models import User, Post
from schemas import UserCreate, PostCreate

def get_user(db: Session, user_id: int):
    return db.query(User).filter(User.id == user_id).first()

def get_users(db: Session, skip: int = 0, limit: int = 10):
    return db.query(User).offset(skip).limit(limit).all()

def create_user(db: Session, user: UserCreate):
    db_user = User(
        username=user.username,
        email=user.email,
        hashed_password=hash_password(user.password)
    )
    db.add(db_user)
    db.commit()
    db.refresh(db_user)
    return db_user

def create_post(db: Session, post: PostCreate, user_id: int):
    db_post = Post(**post.dict(), author_id=user_id)
    db.add(db_post)
    db.commit()
    db.refresh(db_post)
    return db_post
```

### API 엔드포인트

```python
# main.py
from fastapi import FastAPI, Depends, HTTPException
from sqlalchemy.orm import Session
import crud, models, schemas
from database import engine, SessionLocal

models.Base.metadata.create_all(bind=engine)

app = FastAPI()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

@app.post("/users", response_model=schemas.UserResponse)
def create_user(user: schemas.UserCreate, db: Session = Depends(get_db)):
    db_user = crud.get_user_by_email(db, email=user.email)
    if db_user:
        raise HTTPException(status_code=400, detail="Email already registered")
    return crud.create_user(db=db, user=user)

@app.get("/users/{user_id}", response_model=schemas.UserResponse)
def read_user(user_id: int, db: Session = Depends(get_db)):
    db_user = crud.get_user(db, user_id=user_id)
    if db_user is None:
        raise HTTPException(status_code=404, detail="User not found")
    return db_user

@app.get("/users", response_model=List[schemas.UserResponse])
def read_users(skip: int = 0, limit: int = 10, db: Session = Depends(get_db)):
    users = crud.get_users(db, skip=skip, limit=limit)
    return users
```

---

## 인증과 보안

### JWT 인증

```python
from datetime import datetime, timedelta
from jose import JWTError, jwt
from passlib.context import CryptContext
from fastapi.security import OAuth2PasswordBearer, OAuth2PasswordRequestForm

# 설정
SECRET_KEY = "your-secret-key"
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 30

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="token")

# 비밀번호 해싱
def verify_password(plain_password, hashed_password):
    return pwd_context.verify(plain_password, hashed_password)

def get_password_hash(password):
    return pwd_context.hash(password)

# JWT 토큰 생성
def create_access_token(data: dict):
    to_encode = data.copy()
    expire = datetime.utcnow() + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt

# 현재 사용자 가져오기
async def get_current_user(
    token: str = Depends(oauth2_scheme),
    db: Session = Depends(get_db)
):
    credentials_exception = HTTPException(
        status_code=401,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        username: str = payload.get("sub")
        if username is None:
            raise credentials_exception
    except JWTError:
        raise credentials_exception

    user = crud.get_user_by_username(db, username=username)
    if user is None:
        raise credentials_exception
    return user

# 로그인 엔드포인트
@app.post("/token")
async def login(
    form_data: OAuth2PasswordRequestForm = Depends(),
    db: Session = Depends(get_db)
):
    user = authenticate_user(db, form_data.username, form_data.password)
    if not user:
        raise HTTPException(status_code=401, detail="Incorrect credentials")

    access_token = create_access_token(data={"sub": user.username})
    return {"access_token": access_token, "token_type": "bearer"}

# 보호된 라우트
@app.get("/me")
async def read_users_me(current_user: User = Depends(get_current_user)):
    return current_user
```

---

## 자동 문서화

### Swagger UI 커스터마이징

```python
app = FastAPI(
    title="My API",
    description="API 상세 설명",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json"
)

@app.get(
    "/items/{item_id}",
    summary="아이템 조회",
    description="ID로 아이템을 조회합니다.",
    response_description="조회된 아이템",
    tags=["items"]
)
async def get_item(item_id: int):
    """
    아이템을 조회하는 엔드포인트입니다.

    - **item_id**: 조회할 아이템의 ID
    """
    return {"item_id": item_id}
```

### 태그로 그룹화

```python
tags_metadata = [
    {
        "name": "users",
        "description": "사용자 관련 작업",
    },
    {
        "name": "items",
        "description": "아이템 관리",
    },
]

app = FastAPI(openapi_tags=tags_metadata)

@app.get("/users", tags=["users"])
async def get_users():
    return []

@app.get("/items", tags=["items"])
async def get_items():
    return []
```

---

## 배포

### 프로덕션 설정

```python
# config.py
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    database_url: str
    secret_key: str
    algorithm: str = "HS256"
    access_token_expire_minutes: int = 30

    class Config:
        env_file = ".env"

settings = Settings()
```

### Uvicorn 실행

```bash
# 개발
uvicorn main:app --reload --host 0.0.0.0 --port 8000

# 프로덕션
uvicorn main:app --host 0.0.0.0 --port 8000 --workers 4
```

### Docker

```dockerfile
# Dockerfile
FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
```

```yaml
# docker-compose.yml
version: '3.8'

services:
  api:
    build: .
    ports:
      - "8000:8000"
    environment:
      - DATABASE_URL=postgresql://user:pass@db/mydb
      - SECRET_KEY=your-secret-key
    depends_on:
      - db

  db:
    image: postgres:15
    environment:
      - POSTGRES_USER=user
      - POSTGRES_PASSWORD=pass
      - POSTGRES_DB=mydb
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

### requirements.txt

```
fastapi==0.109.0
uvicorn[standard]==0.27.0
pydantic==2.5.3
pydantic-settings==2.1.0
sqlalchemy==2.0.25
databases[sqlite]==0.8.0
python-jose[cryptography]==3.3.0
passlib[bcrypt]==1.7.4
python-multipart==0.0.6
```

---

## 참고 자료

- [FastAPI 공식 문서](https://fastapi.tiangolo.com/)
- [Pydantic 문서](https://docs.pydantic.dev/)
- [SQLAlchemy 문서](https://docs.sqlalchemy.org/)
- [Uvicorn 문서](https://www.uvicorn.org/)

*마지막 업데이트: 2025년 12월*
