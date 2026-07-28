# Flask, FastAPI, Django 프레임워크 비교

Python 웹 프레임워크의 3대장인 Flask, FastAPI, Django를 비교 분석한 문서입니다. 각 프레임워크의 특징, 장단점, 사용 사례를 표 형식으로 정리했습니다.

## 목차

1. [개요](#개요)
2. [기본 특성 비교](#기본-특성-비교)
3. [Hello World 예제](#hello-world-예제)
4. [상세 비교](#상세-비교)
5. [성능 비교](#성능-비교)
6. [사용 사례 및 권장사항](#사용-사례-및-권장사항)

---

## 개요

### Flask
- **타입**: 마이크로 프레임워크 (Micro Framework)
- **출시**: 2010년
- **철학**: 심플함과 확장성
- **특징**: 최소한의 기능만 제공, 필요한 기능을 자유롭게 추가

### FastAPI
- **타입**: 현대적 웹 프레임워크
- **출시**: 2018년
- **철학**: 빠른 성능과 개발자 경험
- **특징**: 비동기 지원, 자동 문서화, 타입 힌트 기반

### Django
- **타입**: 풀스택 프레임워크 (Full-Stack Framework)
- **출시**: 2005년
- **철학**: "Batteries Included" (모든 기능 포함)
- **특징**: ORM, Admin 패널, 인증 등 기본 제공

---

## 기본 특성 비교

| 특성 | Flask | FastAPI | Django |
|------|-------|---------|--------|
| **타입** | 마이크로 프레임워크 | API 프레임워크 | 풀스택 프레임워크 |
| **학습 곡선** | 낮음 | 중간 | 높음 |
| **성능** | 중간 | 매우 높음 | 중간 |
| **비동기 지원** | 제한적 (Flask 2.0+) | 네이티브 지원 | 제한적 (Django 3.1+) |
| **자동 문서화** | 없음 (확장 필요) | 자동 (Swagger/ReDoc) | 없음 |
| **타입 검증** | 없음 | Pydantic 기반 | 없음 (ORM 레벨) |
| **ORM** | 없음 (SQLAlchemy 사용) | 없음 (선택적) | Django ORM 내장 |
| **Admin 패널** | 없음 | 없음 | 기본 제공 |
| **템플릿 엔진** | Jinja2 | Jinja2 (선택적) | Django Templates |
| **폼 처리** | WTForms (확장) | Pydantic | Django Forms 내장 |
| **인증** | 확장 필요 | 직접 구현/확장 | 기본 제공 |
| **주요 용도** | 소규모 앱, API | REST API, 마이크로서비스 | 풀스택 웹 앱 |

---

## Hello World 예제

### Flask

```python
from flask import Flask

app = Flask(__name__)

@app.route('/')
def hello():
    return {'message': 'Hello, World!'}

if __name__ == '__main__':
    app.run(debug=True)
```

### FastAPI

```python
from fastapi import FastAPI

app = FastAPI()

@app.get('/')
async def hello():
    return {'message': 'Hello, World!'}

# 실행: uvicorn main:app --reload
```

### Django

```python
# settings.py와 urls.py 설정 필요

# views.py
from django.http import JsonResponse

def hello(request):
    return JsonResponse({'message': 'Hello, World!'})

# urls.py
from django.urls import path
from . import views

urlpatterns = [
    path('', views.hello),
]
```

---

## 상세 비교

### 아키텍처 및 설계 철학

| 측면 | Flask | FastAPI | Django |
|------|-------|---------|--------|
| **설계 철학** | 최소주의 | 성능 + 개발자 경험 | 완결성 |
| **확장성** | 매우 높음 | 높음 | 중간 |
| **유연성** | 매우 높음 | 높음 | 중간 |
| **규약** | 적음 | 중간 | 많음 (강한 규약) |
| **프로젝트 구조** | 자유로움 | 자유로움 | 정해진 구조 |

### 개발자 경험

| 측면 | Flask | FastAPI | Django |
|------|-------|---------|--------|
| **초기 설정** | 매우 간단 | 간단 | 복잡 |
| **보일러플레이트** | 적음 | 적음 | 많음 |
| **IDE 지원** | 좋음 | 매우 좋음 (타입 힌트) | 좋음 |
| **디버깅** | 쉬움 | 쉬움 | 중간 |
| **테스팅** | pytest 권장 | pytest 권장 | 내장 테스트 프레임워크 |
| **문서 품질** | 좋음 | 매우 좋음 | 매우 좋음 |

### 생태계 및 커뮤니티

| 측면 | Flask | FastAPI | Django |
|------|-------|---------|--------|
| **커뮤니티 크기** | 대형 | 급성장 중 | 매우 대형 |
| **확장 패키지** | 매우 많음 | 증가 중 | 매우 많음 |
| **기업 도입** | 높음 | 증가 중 | 매우 높음 |
| **활발한 개발** | 보통 | 매우 활발 | 활발 |
| **Stack Overflow** | 많은 답변 | 증가 중 | 가장 많은 답변 |

### 데이터베이스 지원

| 측면 | Flask | FastAPI | Django |
|------|-------|---------|--------|
| **기본 ORM** | 없음 | 없음 | Django ORM |
| **SQLAlchemy** | 권장 | 선택적 | 가능 |
| **마이그레이션** | Flask-Migrate | Alembic | 내장 (makemigrations) |
| **비동기 DB** | 제한적 | SQLAlchemy 2.0 지원 | 제한적 |
| **NoSQL 지원** | 확장으로 가능 | 직접 구현 | 확장으로 가능 |

---

## 성능 비교

### 요청 처리 성능 (대략적인 비교)

| 프레임워크 | 초당 요청 수 (RPS) | 상대 성능 |
|-----------|-------------------|----------|
| **FastAPI** | ~25,000 | 가장 빠름 ⚡️ |
| **Flask** | ~3,000 | 중간 |
| **Django** | ~2,500 | 중간 |

**주의**: 실제 성능은 애플리케이션 구조, 데이터베이스, 배포 환경 등에 따라 크게 달라질 수 있습니다.

### 성능 특성

| 측면 | Flask | FastAPI | Django |
|------|-------|---------|--------|
| **동기/비동기** | 주로 동기 | 비동기 네이티브 | 주로 동기 |
| **WebSocket** | 확장 필요 | 네이티브 지원 | 확장 필요 |
| **백그라운드 작업** | Celery | BackgroundTasks | Celery |
| **스트리밍** | 가능 | 최적화됨 | 가능 |
| **메모리 사용량** | 낮음 | 낮음 | 중간-높음 |

---

## 사용 사례 및 권장사항

### Flask를 선택해야 할 때

**장점**
- 빠른 프로토타이핑
- 유연한 구조가 필요할 때
- 기존 Python 라이브러리와 쉽게 통합
- 학습이 쉽고 진입 장벽이 낮음

**단점**
- 모든 것을 직접 구현해야 함
- 대규모 프로젝트에서 구조화 어려움
- 표준화된 방법이 없어 팀마다 다른 구조

**추천 사용 사례**
- 소규모 API 서버
- 마이크로서비스
- 간단한 웹 애플리케이션
- 프로토타입 개발

```python
# Flask 프로젝트 예시
from flask import Flask, jsonify
from flask_sqlalchemy import SQLAlchemy

app = Flask(__name__)
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///app.db'
db = SQLAlchemy(app)

@app.route('/api/users')
def get_users():
    users = User.query.all()
    return jsonify([user.to_dict() for user in users])
```

---

### FastAPI를 선택해야 할 때

**장점**
- 매우 빠른 성능 (Starlette 기반)
- 자동 API 문서화 (Swagger UI, ReDoc)
- 타입 힌트를 통한 자동 검증
- 비동기 프로그래밍 네이티브 지원
- 현대적인 개발자 경험

**단점**
- 비교적 새로운 프레임워크 (성숙도)
- 커뮤니티가 작음 (증가 중)
- 비동기 개념 이해 필요

**추천 사용 사례**
- REST API 개발
- 마이크로서비스 아키텍처
- 고성능이 필요한 서비스
- 실시간 데이터 처리
- ML 모델 서빙

```python
# FastAPI 프로젝트 예시
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List

app = FastAPI()

class User(BaseModel):
    id: int
    name: str
    email: str

@app.get('/api/users', response_model=List[User])
async def get_users():
    # 자동으로 Swagger 문서 생성
    # Pydantic으로 자동 검증
    return await fetch_users_from_db()
```

---

### Django를 선택해야 할 때

**장점**
- 모든 기능이 포함됨 (Batteries Included)
- 강력한 ORM과 마이그레이션
- 관리자 페이지 자동 생성
- 검증된 보안 기능
- 대규모 커뮤니티와 풍부한 패키지

**단점**
- 무겁고 복잡한 구조
- 유연성이 낮음
- API만 필요한 경우 오버헤드
- 학습 곡선이 가파름

**추천 사용 사례**
- 대규모 웹 애플리케이션
- CMS, 전자상거래 사이트
- 관리자 페이지가 필요한 서비스
- 데이터 중심 애플리케이션
- 빠른 MVP 개발 (이미 Django 경험이 있다면)

```python
# Django 프로젝트 예시
# models.py
from django.db import models

class User(models.Model):
    name = models.CharField(max_length=100)
    email = models.EmailField(unique=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-created_at']

# admin.py에 등록하면 자동으로 관리 페이지 생성
from django.contrib import admin
admin.site.register(User)
```

---

## 프레임워크 선택 가이드

### 결정 플로우차트

```
프로젝트 시작
    |
    ├─ API만 필요한가?
    |   └─ Yes
    |       ├─ 고성능/비동기 필요?
    |       |   └─ Yes → FastAPI ⚡️
    |       |   └─ No → Flask
    |       └─ 자동 문서화 중요?
    |           └─ Yes → FastAPI
    |           └─ No → Flask
    |
    └─ 풀스택 웹앱?
        ├─ 관리자 페이지 필요?
        |   └─ Yes → Django 🎯
        |
        ├─ 대규모 프로젝트?
        |   └─ Yes → Django
        |
        └─ 빠른 프로토타입?
            └─ Flask
```

### 규모별 추천

| 프로젝트 규모 | 추천 프레임워크 | 이유 |
|-------------|--------------|------|
| **소규모 API** | FastAPI, Flask | 간결함, 빠른 개발 |
| **중규모 API** | FastAPI | 성능, 문서화, 타입 안정성 |
| **대규모 API** | FastAPI, Django REST Framework | 확장성, 생태계 |
| **소규모 웹앱** | Flask | 유연성, 학습 용이 |
| **중대규모 웹앱** | Django | 내장 기능, 관리자 페이지 |
| **마이크로서비스** | FastAPI, Flask | 가벼움, 독립성 |
| **데이터 중심 앱** | Django | 강력한 ORM |

---

## 실무 팁

### 혼용 전략

실제 프로젝트에서는 여러 프레임워크를 함께 사용하기도 합니다:

**예시 1: Django + FastAPI**
- Django: 메인 웹 애플리케이션, 관리자 페이지
- FastAPI: 고성능 API 엔드포인트, 실시간 기능

**예시 2: Flask + FastAPI**
- Flask: 레거시 시스템, 웹 페이지
- FastAPI: 새로운 API 서비스

### 마이그레이션 고려사항

| From → To | 난이도 | 고려사항 |
|----------|--------|---------|
| Flask → FastAPI | 쉬움 | 비슷한 구조, 비동기 개념 학습 필요 |
| Flask → Django | 중간 | 구조 재설계 필요, ORM 전환 |
| Django → FastAPI | 어려움 | ORM/Admin 대체 필요, 아키텍처 변경 |
| FastAPI → Flask | 쉬움 | 비동기 포기, 기능 추가 개발 |

---

## 참고 자료

- [Flask 공식 문서](https://flask.palletsprojects.com/)
- [FastAPI 공식 문서](https://fastapi.tiangolo.com/)
- [Django 공식 문서](https://docs.djangoproject.com/)
- [Web Framework Benchmarks](https://www.techempower.com/benchmarks/)
- [Python Web Framework Comparison (2024)](https://github.com/topics/python-web-framework)

*마지막 업데이트: 2025년 12월*
