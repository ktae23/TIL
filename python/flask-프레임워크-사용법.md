# Flask 프레임워크 사용법

Flask를 사용한 웹 애플리케이션 개발의 기초부터 실무 활용까지 다루는 실전 가이드입니다. 많은 코드 예제와 함께 Flask의 핵심 기능을 학습합니다.

## 목차

1. [설치 및 기본 설정](#설치-및-기본-설정)
2. [라우팅과 뷰 함수](#라우팅과-뷰-함수)
3. [템플릿 엔진 (Jinja2)](#템플릿-엔진-jinja2)
4. [폼 처리](#폼-처리)
5. [데이터베이스 연동 (SQLAlchemy)](#데이터베이스-연동-sqlalchemy)
6. [블루프린트를 통한 모듈화](#블루프린트를-통한-모듈화)
7. [에러 핸들링](#에러-핸들링)
8. [RESTful API 만들기](#restful-api-만들기)
9. [인증과 세션 관리](#인증과-세션-관리)
10. [배포 준비](#배포-준비)

---

## 설치 및 기본 설정

### Flask 설치

```bash
# 가상환경 생성 (권장)
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# Flask 설치
pip install Flask

# 추가 패키지 (실무에서 자주 사용)
pip install Flask-SQLAlchemy Flask-Migrate Flask-WTF python-dotenv
```

### 기본 프로젝트 구조

```
my_flask_app/
├── app.py              # 메인 애플리케이션
├── config.py           # 설정 파일
├── requirements.txt    # 의존성 목록
├── .env                # 환경 변수
├── static/             # 정적 파일 (CSS, JS, 이미지)
│   ├── css/
│   ├── js/
│   └── images/
├── templates/          # HTML 템플릿
│   ├── base.html
│   └── index.html
└── models/             # 데이터베이스 모델
    └── user.py
```

### 최소 Flask 앱

```python
# app.py
from flask import Flask

app = Flask(__name__)

@app.route('/')
def index():
    return 'Hello, Flask!'

if __name__ == '__main__':
    app.run(debug=True)
```

```bash
# 실행
python app.py
# 또는
flask run
```

### 환경 변수 설정

```python
# .env
FLASK_APP=app.py
FLASK_ENV=development
SECRET_KEY=your-secret-key-here
DATABASE_URL=sqlite:///app.db
```

```python
# config.py
import os
from dotenv import load_dotenv

load_dotenv()

class Config:
    SECRET_KEY = os.getenv('SECRET_KEY', 'dev-secret-key')
    SQLALCHEMY_DATABASE_URI = os.getenv('DATABASE_URL', 'sqlite:///app.db')
    SQLALCHEMY_TRACK_MODIFICATIONS = False

# app.py에서 사용
from config import Config
app.config.from_object(Config)
```

---

## 라우팅과 뷰 함수

### 기본 라우팅

```python
from flask import Flask, request, jsonify

app = Flask(__name__)

# GET 요청
@app.route('/')
def home():
    return 'Home Page'

# 동적 URL
@app.route('/user/<username>')
def show_user(username):
    return f'User: {username}'

# 타입 지정
@app.route('/post/<int:post_id>')
def show_post(post_id):
    return f'Post ID: {post_id}'

# 여러 HTTP 메서드
@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        username = request.form.get('username')
        password = request.form.get('password')
        return f'Logging in {username}'
    return 'Login Form'
```

### URL 변수 타입

```python
# string (기본값)
@app.route('/user/<username>')

# int
@app.route('/post/<int:post_id>')

# float
@app.route('/price/<float:amount>')

# path (슬래시 포함)
@app.route('/files/<path:filepath>')

# uuid
@app.route('/object/<uuid:obj_id>')
```

### 쿼리 파라미터 처리

```python
from flask import request

@app.route('/search')
def search():
    # /search?q=flask&page=1
    query = request.args.get('q', '')
    page = request.args.get('page', 1, type=int)

    return {
        'query': query,
        'page': page,
        'results': perform_search(query, page)
    }
```

### URL 리다이렉트와 에러

```python
from flask import redirect, url_for, abort

@app.route('/')
def index():
    return redirect(url_for('login'))

@app.route('/admin')
def admin():
    if not is_admin():
        abort(403)  # Forbidden
    return 'Admin Panel'

@app.route('/user/<int:user_id>')
def get_user(user_id):
    user = User.query.get(user_id)
    if user is None:
        abort(404)
    return render_template('user.html', user=user)
```

---

## 템플릿 엔진 (Jinja2)

### 기본 템플릿

```python
# app.py
from flask import render_template

@app.route('/hello/<name>')
def hello(name):
    return render_template('hello.html', name=name)
```

```html
<!-- templates/hello.html -->
<!DOCTYPE html>
<html>
<head>
    <title>Hello</title>
</head>
<body>
    <h1>Hello, {{ name }}!</h1>
</body>
</html>
```

### 템플릿 상속

```html
<!-- templates/base.html -->
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>{% block title %}My App{% endblock %}</title>
    <link rel="stylesheet" href="{{ url_for('static', filename='css/style.css') }}">
</head>
<body>
    <nav>
        <a href="{{ url_for('index') }}">Home</a>
        <a href="{{ url_for('about') }}">About</a>
    </nav>

    <main>
        {% block content %}{% endblock %}
    </main>

    <footer>
        <p>&copy; 2025 My App</p>
    </footer>
</body>
</html>
```

```html
<!-- templates/index.html -->
{% extends "base.html" %}

{% block title %}Home - My App{% endblock %}

{% block content %}
    <h1>Welcome to My App</h1>
    <p>This is the home page.</p>
{% endblock %}
```

### 조건문과 반복문

```html
<!-- templates/users.html -->
{% extends "base.html" %}

{% block content %}
    <h1>Users</h1>

    {% if users %}
        <ul>
        {% for user in users %}
            <li>
                {{ user.name }} ({{ user.email }})
                {% if user.is_admin %}
                    <span class="badge">Admin</span>
                {% endif %}
            </li>
        {% endfor %}
        </ul>
    {% else %}
        <p>No users found.</p>
    {% endif %}
{% endblock %}
```

### 필터 사용

```html
<!-- Jinja2 필터 예제 -->
<h1>{{ title|upper }}</h1>
<p>{{ description|truncate(100) }}</p>
<p>Posted: {{ date|datetimeformat }}</p>

<!-- 커스텀 필터 -->
<p>{{ price|currency }}</p>
```

```python
# 커스텀 필터 정의
@app.template_filter('currency')
def currency_filter(value):
    return f'₩{value:,.0f}'

@app.template_filter('datetimeformat')
def datetimeformat(value, format='%Y-%m-%d %H:%M'):
    return value.strftime(format)
```

---

## 폼 처리

### Flask-WTF를 사용한 폼

```python
# forms.py
from flask_wtf import FlaskForm
from wtforms import StringField, PasswordField, SubmitField, TextAreaField
from wtforms.validators import DataRequired, Email, Length, EqualTo

class LoginForm(FlaskForm):
    email = StringField('이메일',
                       validators=[DataRequired(), Email()])
    password = PasswordField('비밀번호',
                            validators=[DataRequired()])
    submit = SubmitField('로그인')

class RegisterForm(FlaskForm):
    username = StringField('사용자명',
                          validators=[DataRequired(), Length(min=3, max=20)])
    email = StringField('이메일',
                       validators=[DataRequired(), Email()])
    password = PasswordField('비밀번호',
                            validators=[DataRequired(), Length(min=6)])
    confirm_password = PasswordField('비밀번호 확인',
                                    validators=[DataRequired(),
                                               EqualTo('password')])
    submit = SubmitField('가입하기')
```

```python
# app.py
from flask import render_template, redirect, url_for, flash
from forms import LoginForm, RegisterForm

@app.route('/login', methods=['GET', 'POST'])
def login():
    form = LoginForm()

    if form.validate_on_submit():
        email = form.email.data
        password = form.password.data

        # 인증 로직
        user = User.query.filter_by(email=email).first()
        if user and user.check_password(password):
            flash('로그인 성공!', 'success')
            return redirect(url_for('dashboard'))
        else:
            flash('이메일 또는 비밀번호가 잘못되었습니다.', 'danger')

    return render_template('login.html', form=form)
```

```html
<!-- templates/login.html -->
{% extends "base.html" %}

{% block content %}
    <h2>로그인</h2>

    {% with messages = get_flashed_messages(with_categories=true) %}
        {% if messages %}
            {% for category, message in messages %}
                <div class="alert alert-{{ category }}">{{ message }}</div>
            {% endfor %}
        {% endif %}
    {% endwith %}

    <form method="POST">
        {{ form.hidden_tag() }}

        <div class="form-group">
            {{ form.email.label }}
            {{ form.email(class="form-control") }}
            {% if form.email.errors %}
                <div class="error">{{ form.email.errors[0] }}</div>
            {% endif %}
        </div>

        <div class="form-group">
            {{ form.password.label }}
            {{ form.password(class="form-control") }}
            {% if form.password.errors %}
                <div class="error">{{ form.password.errors[0] }}</div>
            {% endif %}
        </div>

        {{ form.submit(class="btn btn-primary") }}
    </form>
{% endblock %}
```

---

## 데이터베이스 연동 (SQLAlchemy)

### 설정

```python
# app.py
from flask import Flask
from flask_sqlalchemy import SQLAlchemy
from flask_migrate import Migrate

app = Flask(__name__)
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///app.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

db = SQLAlchemy(app)
migrate = Migrate(app, db)
```

### 모델 정의

```python
# models.py
from datetime import datetime
from app import db

class User(db.Model):
    __tablename__ = 'users'

    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(80), unique=True, nullable=False)
    email = db.Column(db.String(120), unique=True, nullable=False)
    password_hash = db.Column(db.String(200))
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    # 관계 설정
    posts = db.relationship('Post', backref='author', lazy=True)

    def __repr__(self):
        return f'<User {self.username}>'

class Post(db.Model):
    __tablename__ = 'posts'

    id = db.Column(db.Integer, primary_key=True)
    title = db.Column(db.String(200), nullable=False)
    content = db.Column(db.Text, nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow,
                          onupdate=datetime.utcnow)

    # 외래 키
    user_id = db.Column(db.Integer, db.ForeignKey('users.id'),
                       nullable=False)

    def __repr__(self):
        return f'<Post {self.title}>'
```

### 데이터베이스 초기화

```bash
# 마이그레이션 초기화
flask db init

# 마이그레이션 파일 생성
flask db migrate -m "Initial migration"

# 마이그레이션 적용
flask db upgrade
```

### CRUD 작업

```python
# Create
@app.route('/user/create', methods=['POST'])
def create_user():
    username = request.form.get('username')
    email = request.form.get('email')

    user = User(username=username, email=email)
    db.session.add(user)
    db.session.commit()

    return jsonify({'id': user.id}), 201

# Read
@app.route('/users')
def get_users():
    users = User.query.all()
    return render_template('users.html', users=users)

@app.route('/user/<int:user_id>')
def get_user(user_id):
    user = User.query.get_or_404(user_id)
    return render_template('user.html', user=user)

# 복잡한 쿼리
@app.route('/search/users')
def search_users():
    query = request.args.get('q', '')
    users = User.query.filter(
        User.username.like(f'%{query}%')
    ).order_by(User.created_at.desc()).all()

    return render_template('search_results.html', users=users)

# Update
@app.route('/user/<int:user_id>/edit', methods=['POST'])
def update_user(user_id):
    user = User.query.get_or_404(user_id)
    user.username = request.form.get('username')
    user.email = request.form.get('email')

    db.session.commit()
    flash('User updated successfully', 'success')
    return redirect(url_for('get_user', user_id=user.id))

# Delete
@app.route('/user/<int:user_id>/delete', methods=['POST'])
def delete_user(user_id):
    user = User.query.get_or_404(user_id)
    db.session.delete(user)
    db.session.commit()

    flash('User deleted', 'info')
    return redirect(url_for('get_users'))
```

---

## 블루프린트를 통한 모듈화

### 블루프린트 생성

```python
# blueprints/auth.py
from flask import Blueprint, render_template, redirect, url_for
from forms import LoginForm, RegisterForm

auth_bp = Blueprint('auth', __name__, url_prefix='/auth')

@auth_bp.route('/login', methods=['GET', 'POST'])
def login():
    form = LoginForm()
    if form.validate_on_submit():
        # 로그인 로직
        return redirect(url_for('main.index'))
    return render_template('auth/login.html', form=form)

@auth_bp.route('/register', methods=['GET', 'POST'])
def register():
    form = RegisterForm()
    if form.validate_on_submit():
        # 회원가입 로직
        return redirect(url_for('auth.login'))
    return render_template('auth/register.html', form=form)

@auth_bp.route('/logout')
def logout():
    # 로그아웃 로직
    return redirect(url_for('main.index'))
```

```python
# blueprints/blog.py
from flask import Blueprint, render_template, request
from models import Post

blog_bp = Blueprint('blog', __name__, url_prefix='/blog')

@blog_bp.route('/')
def index():
    posts = Post.query.order_by(Post.created_at.desc()).all()
    return render_template('blog/index.html', posts=posts)

@blog_bp.route('/post/<int:post_id>')
def show_post(post_id):
    post = Post.query.get_or_404(post_id)
    return render_template('blog/post.html', post=post)
```

### 블루프린트 등록

```python
# app.py
from flask import Flask
from blueprints.auth import auth_bp
from blueprints.blog import blog_bp

app = Flask(__name__)

# 블루프린트 등록
app.register_blueprint(auth_bp)
app.register_blueprint(blog_bp)

@app.route('/')
def index():
    return render_template('index.html')
```

---

## 에러 핸들링

### 커스텀 에러 페이지

```python
# app.py
@app.errorhandler(404)
def not_found_error(error):
    return render_template('errors/404.html'), 404

@app.errorhandler(500)
def internal_error(error):
    db.session.rollback()  # DB 롤백
    return render_template('errors/500.html'), 500

@app.errorhandler(403)
def forbidden_error(error):
    return render_template('errors/403.html'), 403
```

```html
<!-- templates/errors/404.html -->
{% extends "base.html" %}

{% block content %}
    <div class="error-page">
        <h1>404</h1>
        <h2>Page Not Found</h2>
        <p>요청하신 페이지를 찾을 수 없습니다.</p>
        <a href="{{ url_for('index') }}">홈으로 돌아가기</a>
    </div>
{% endblock %}
```

### 예외 처리

```python
from werkzeug.exceptions import BadRequest

@app.route('/api/data', methods=['POST'])
def process_data():
    try:
        data = request.get_json()
        if not data:
            raise BadRequest('No data provided')

        # 데이터 처리
        result = process(data)
        return jsonify(result), 200

    except ValueError as e:
        return jsonify({'error': str(e)}), 400
    except Exception as e:
        app.logger.error(f'Error processing data: {e}')
        return jsonify({'error': 'Internal server error'}), 500
```

---

## RESTful API 만들기

### API 엔드포인트

```python
# api/resources.py
from flask import Blueprint, request, jsonify
from models import User, Post
from app import db

api_bp = Blueprint('api', __name__, url_prefix='/api')

# Users API
@api_bp.route('/users', methods=['GET'])
def get_users():
    users = User.query.all()
    return jsonify([{
        'id': user.id,
        'username': user.username,
        'email': user.email
    } for user in users])

@api_bp.route('/users/<int:user_id>', methods=['GET'])
def get_user(user_id):
    user = User.query.get_or_404(user_id)
    return jsonify({
        'id': user.id,
        'username': user.username,
        'email': user.email,
        'created_at': user.created_at.isoformat()
    })

@api_bp.route('/users', methods=['POST'])
def create_user():
    data = request.get_json()

    # 검증
    if not data or not data.get('username') or not data.get('email'):
        return jsonify({'error': 'Missing required fields'}), 400

    user = User(
        username=data['username'],
        email=data['email']
    )
    db.session.add(user)
    db.session.commit()

    return jsonify({'id': user.id}), 201

@api_bp.route('/users/<int:user_id>', methods=['PUT'])
def update_user(user_id):
    user = User.query.get_or_404(user_id)
    data = request.get_json()

    user.username = data.get('username', user.username)
    user.email = data.get('email', user.email)

    db.session.commit()

    return jsonify({'message': 'User updated'})

@api_bp.route('/users/<int:user_id>', methods=['DELETE'])
def delete_user(user_id):
    user = User.query.get_or_404(user_id)
    db.session.delete(user)
    db.session.commit()

    return jsonify({'message': 'User deleted'}), 204
```

---

## 인증과 세션 관리

### Flask-Login 사용

```python
# app.py
from flask_login import LoginManager, UserMixin, login_user, logout_user, login_required, current_user

login_manager = LoginManager()
login_manager.init_app(app)
login_manager.login_view = 'auth.login'

@login_manager.user_loader
def load_user(user_id):
    return User.query.get(int(user_id))
```

```python
# models.py
from flask_login import UserMixin
from werkzeug.security import generate_password_hash, check_password_hash

class User(UserMixin, db.Model):
    # ... 기존 필드들 ...

    def set_password(self, password):
        self.password_hash = generate_password_hash(password)

    def check_password(self, password):
        return check_password_hash(self.password_hash, password)
```

```python
# views.py
from flask_login import login_user, logout_user, login_required, current_user

@app.route('/login', methods=['POST'])
def login():
    email = request.form.get('email')
    password = request.form.get('password')

    user = User.query.filter_by(email=email).first()

    if user and user.check_password(password):
        login_user(user, remember=True)
        return redirect(url_for('dashboard'))

    flash('Invalid email or password', 'danger')
    return redirect(url_for('login'))

@app.route('/logout')
@login_required
def logout():
    logout_user()
    return redirect(url_for('index'))

@app.route('/dashboard')
@login_required
def dashboard():
    return render_template('dashboard.html', user=current_user)
```

---

## 배포 준비

### 프로덕션 설정

```python
# config.py
import os

class Config:
    SECRET_KEY = os.getenv('SECRET_KEY')
    SQLALCHEMY_DATABASE_URI = os.getenv('DATABASE_URL')
    SQLALCHEMY_TRACK_MODIFICATIONS = False

class DevelopmentConfig(Config):
    DEBUG = True
    SQLALCHEMY_ECHO = True

class ProductionConfig(Config):
    DEBUG = False

config = {
    'development': DevelopmentConfig,
    'production': ProductionConfig,
    'default': DevelopmentConfig
}
```

### WSGI 서버 (Gunicorn)

```bash
# Gunicorn 설치
pip install gunicorn

# 실행
gunicorn -w 4 -b 0.0.0.0:8000 app:app
```

### requirements.txt

```
Flask==3.0.0
Flask-SQLAlchemy==3.1.1
Flask-Migrate==4.0.5
Flask-WTF==1.2.1
Flask-Login==0.6.3
python-dotenv==1.0.0
gunicorn==21.2.0
```

### Docker 배포

```dockerfile
# Dockerfile
FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8000

CMD ["gunicorn", "-w", "4", "-b", "0.0.0.0:8000", "app:app"]
```

```yaml
# docker-compose.yml
version: '3.8'

services:
  web:
    build: .
    ports:
      - "8000:8000"
    environment:
      - FLASK_ENV=production
      - DATABASE_URL=postgresql://user:pass@db:5432/myapp
    depends_on:
      - db

  db:
    image: postgres:15
    environment:
      - POSTGRES_USER=user
      - POSTGRES_PASSWORD=pass
      - POSTGRES_DB=myapp
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

---

## 참고 자료

- [Flask 공식 문서](https://flask.palletsprojects.com/)
- [Flask-SQLAlchemy 문서](https://flask-sqlalchemy.palletsprojects.com/)
- [Flask-WTF 문서](https://flask-wtf.readthedocs.io/)
- [Flask-Login 문서](https://flask-login.readthedocs.io/)
- [Flask Mega-Tutorial](https://blog.miguelgrinberg.com/post/the-flask-mega-tutorial-part-i-hello-world)

*마지막 업데이트: 2025년 12월*
