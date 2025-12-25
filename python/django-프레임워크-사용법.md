# Django 프레임워크 사용법

Django를 사용한 풀스택 웹 애플리케이션 개발의 핵심 개념과 실전 예제를 정리한 문서입니다.

## 목차

1. [설치 및 프로젝트 생성](#설치-및-프로젝트-생성)
2. [모델과 ORM](#모델과-orm)
3. [뷰 (Views)](#뷰-views)
4. [템플릿 (Templates)](#템플릿-templates)
5. [폼 (Forms)](#폼-forms)
6. [URL 라우팅](#url-라우팅)
7. [Admin 관리자 페이지](#admin-관리자-페이지)
8. [인증 시스템](#인증-시스템)
9. [Django REST Framework](#django-rest-framework)
10. [배포](#배포)

---

## 설치 및 프로젝트 생성

### Django 설치

```bash
# 가상환경 생성
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# Django 설치
pip install django

# 버전 확인
python -m django --version
```

### 프로젝트 및 앱 생성

```bash
# 프로젝트 생성
django-admin startproject myproject
cd myproject

# 앱 생성
python manage.py startapp blog
python manage.py startapp accounts

# 개발 서버 실행
python manage.py runserver
```

### 프로젝트 구조

```
myproject/
├── manage.py
├── myproject/
│   ├── __init__.py
│   ├── settings.py
│   ├── urls.py
│   ├── asgi.py
│   └── wsgi.py
├── blog/
│   ├── migrations/
│   ├── __init__.py
│   ├── admin.py
│   ├── apps.py
│   ├── models.py
│   ├── tests.py
│   └── views.py
└── accounts/
    └── ...
```

### 앱 등록

```python
# myproject/settings.py
INSTALLED_APPS = [
    'django.contrib.admin',
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.messages',
    'django.contrib.staticfiles',
    'blog',  # 추가
    'accounts',  # 추가
]
```

---

## 모델과 ORM

### 모델 정의

```python
# blog/models.py
from django.db import models
from django.contrib.auth.models import User
from django.utils import timezone

class Category(models.Model):
    name = models.CharField(max_length=100, unique=True)
    slug = models.SlugField(max_length=100, unique=True)
    description = models.TextField(blank=True)

    class Meta:
        verbose_name_plural = "Categories"
        ordering = ['name']

    def __str__(self):
        return self.name

class Post(models.Model):
    STATUS_CHOICES = [
        ('draft', 'Draft'),
        ('published', 'Published'),
    ]

    title = models.CharField(max_length=200)
    slug = models.SlugField(max_length=200, unique=True)
    author = models.ForeignKey(User, on_delete=models.CASCADE, related_name='blog_posts')
    category = models.ForeignKey(Category, on_delete=models.SET_NULL, null=True, related_name='posts')
    content = models.TextField()
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    published_at = models.DateTimeField(default=timezone.now)
    status = models.CharField(max_length=10, choices=STATUS_CHOICES, default='draft')

    class Meta:
        ordering = ['-published_at']
        indexes = [
            models.Index(fields=['-published_at']),
        ]

    def __str__(self):
        return self.title

class Comment(models.Model):
    post = models.ForeignKey(Post, on_delete=models.CASCADE, related_name='comments')
    author = models.ForeignKey(User, on_delete=models.CASCADE)
    content = models.TextField()
    created_at = models.DateTimeField(auto_now_add=True)
    active = models.BooleanField(default=True)

    class Meta:
        ordering = ['created_at']

    def __str__(self):
        return f'Comment by {self.author} on {self.post}'
```

### 마이그레이션

```bash
# 마이그레이션 파일 생성
python manage.py makemigrations

# 마이그레이션 적용
python manage.py migrate

# 마이그레이션 확인
python manage.py showmigrations
```

### ORM 쿼리

```python
# Shell에서 테스트
python manage.py shell

# Create
from blog.models import Post, Category
from django.contrib.auth.models import User

user = User.objects.first()
category = Category.objects.create(name="Technology", slug="tech")

post = Post.objects.create(
    title="First Post",
    slug="first-post",
    author=user,
    category=category,
    content="This is my first post",
    status="published"
)

# Read
all_posts = Post.objects.all()
published_posts = Post.objects.filter(status='published')
post = Post.objects.get(id=1)
post = Post.objects.get(slug='first-post')

# 복잡한 쿼리
from django.db.models import Q, Count

# OR 조건
posts = Post.objects.filter(
    Q(title__icontains='django') | Q(content__icontains='django')
)

# 관계 조회 (select_related, prefetch_related)
posts = Post.objects.select_related('author', 'category').all()
posts = Post.objects.prefetch_related('comments').all()

# 집계
category_post_count = Category.objects.annotate(
    post_count=Count('posts')
).filter(post_count__gt=0)

# Update
post.title = "Updated Title"
post.save()

# 또는
Post.objects.filter(id=1).update(title="Updated Title")

# Delete
post.delete()
Post.objects.filter(status='draft').delete()
```

---

## 뷰 (Views)

### Function-Based Views (FBV)

```python
# blog/views.py
from django.shortcuts import render, get_object_or_404, redirect
from django.http import HttpResponse, Http404
from django.contrib.auth.decorators import login_required
from .models import Post, Category

def post_list(request):
    posts = Post.objects.filter(status='published').select_related('author', 'category')
    return render(request, 'blog/post_list.html', {'posts': posts})

def post_detail(request, slug):
    post = get_object_or_404(Post, slug=slug, status='published')
    comments = post.comments.filter(active=True)

    return render(request, 'blog/post_detail.html', {
        'post': post,
        'comments': comments
    })

@login_required
def post_create(request):
    if request.method == 'POST':
        # 폼 처리
        title = request.POST.get('title')
        content = request.POST.get('content')

        post = Post.objects.create(
            title=title,
            content=content,
            author=request.user
        )
        return redirect('post_detail', slug=post.slug)

    return render(request, 'blog/post_form.html')
```

### Class-Based Views (CBV)

```python
# blog/views.py
from django.views.generic import ListView, DetailView, CreateView, UpdateView, DeleteView
from django.contrib.auth.mixins import LoginRequiredMixin
from django.urls import reverse_lazy
from .models import Post

class PostListView(ListView):
    model = Post
    template_name = 'blog/post_list.html'
    context_object_name = 'posts'
    paginate_by = 10

    def get_queryset(self):
        return Post.objects.filter(status='published').select_related('author', 'category')

class PostDetailView(DetailView):
    model = Post
    template_name = 'blog/post_detail.html'
    context_object_name = 'post'

    def get_queryset(self):
        return Post.objects.filter(status='published')

class PostCreateView(LoginRequiredMixin, CreateView):
    model = Post
    template_name = 'blog/post_form.html'
    fields = ['title', 'slug', 'category', 'content', 'status']
    success_url = reverse_lazy('post_list')

    def form_valid(self, form):
        form.instance.author = self.request.user
        return super().form_valid(form)

class PostUpdateView(LoginRequiredMixin, UpdateView):
    model = Post
    template_name = 'blog/post_form.html'
    fields = ['title', 'category', 'content', 'status']

    def get_queryset(self):
        # 본인의 글만 수정 가능
        return Post.objects.filter(author=self.request.user)

class PostDeleteView(LoginRequiredMixin, DeleteView):
    model = Post
    success_url = reverse_lazy('post_list')

    def get_queryset(self):
        return Post.objects.filter(author=self.request.user)
```

---

## 템플릿 (Templates)

### 템플릿 설정

```python
# settings.py
TEMPLATES = [
    {
        'BACKEND': 'django.template.backends.django.DjangoTemplates',
        'DIRS': [BASE_DIR / 'templates'],
        'APP_DIRS': True,
        ...
    },
]
```

### 베이스 템플릿

```html
<!-- templates/base.html -->
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{% block title %}My Blog{% endblock %}</title>
    {% load static %}
    <link rel="stylesheet" href="{% static 'css/style.css' %}">
</head>
<body>
    <nav>
        <a href="{% url 'post_list' %}">Home</a>
        {% if user.is_authenticated %}
            <a href="{% url 'post_create' %}">New Post</a>
            <a href="{% url 'logout' %}">Logout ({{ user.username }})</a>
        {% else %}
            <a href="{% url 'login' %}">Login</a>
        {% endif %}
    </nav>

    <main>
        {% if messages %}
            {% for message in messages %}
                <div class="alert alert-{{ message.tags }}">
                    {{ message }}
                </div>
            {% endfor %}
        {% endif %}

        {% block content %}{% endblock %}
    </main>

    <footer>
        <p>&copy; 2025 My Blog</p>
    </footer>
</body>
</html>
```

### 포스트 목록 템플릿

```html
<!-- templates/blog/post_list.html -->
{% extends 'base.html' %}

{% block title %}Blog Posts{% endblock %}

{% block content %}
    <h1>Blog Posts</h1>

    {% for post in posts %}
        <article>
            <h2><a href="{% url 'post_detail' post.slug %}">{{ post.title }}</a></h2>
            <p class="meta">
                By {{ post.author.username }} in {{ post.category.name }}
                | {{ post.published_at|date:"Y-m-d" }}
            </p>
            <p>{{ post.content|truncatewords:30 }}</p>
        </article>
    {% empty %}
        <p>No posts yet.</p>
    {% endfor %}

    <!-- 페이지네이션 -->
    {% if is_paginated %}
        <div class="pagination">
            {% if page_obj.has_previous %}
                <a href="?page=1">First</a>
                <a href="?page={{ page_obj.previous_page_number }}">Previous</a>
            {% endif %}

            <span>Page {{ page_obj.number }} of {{ page_obj.paginator.num_pages }}</span>

            {% if page_obj.has_next %}
                <a href="?page={{ page_obj.next_page_number }}">Next</a>
                <a href="?page={{ page_obj.paginator.num_pages }}">Last</a>
            {% endif %}
        </div>
    {% endif %}
{% endblock %}
```

### 템플릿 필터와 태그

```html
<!-- 내장 필터 -->
{{ post.title|upper }}
{{ post.content|truncatewords:50 }}
{{ post.published_at|date:"Y년 m월 d일" }}
{{ post.content|linebreaks }}

<!-- 커스텀 필터 -->
{% load blog_extras %}
{{ post.content|markdown }}
```

```python
# blog/templatetags/blog_extras.py
from django import template
import markdown as md

register = template.Library()

@register.filter(name='markdown')
def markdown_format(text):
    return md.markdown(text, extensions=['extra', 'codehilite'])
```

---

## 폼 (Forms)

### Django Forms

```python
# blog/forms.py
from django import forms
from .models import Post, Comment

class PostForm(forms.ModelForm):
    class Meta:
        model = Post
        fields = ['title', 'slug', 'category', 'content', 'status']
        widgets = {
            'title': forms.TextInput(attrs={'class': 'form-control'}),
            'content': forms.Textarea(attrs={'class': 'form-control', 'rows': 10}),
        }

    def clean_slug(self):
        slug = self.cleaned_data.get('slug')
        if Post.objects.filter(slug=slug).exists():
            raise forms.ValidationError('This slug already exists.')
        return slug

class CommentForm(forms.ModelForm):
    class Meta:
        model = Comment
        fields = ['content']
        widgets = {
            'content': forms.Textarea(attrs={'rows': 3, 'placeholder': 'Write your comment...'})
        }
```

### 폼 사용

```python
# views.py
from .forms import PostForm, CommentForm

def post_create(request):
    if request.method == 'POST':
        form = PostForm(request.POST)
        if form.is_valid():
            post = form.save(commit=False)
            post.author = request.user
            post.save()
            messages.success(request, 'Post created successfully!')
            return redirect('post_detail', slug=post.slug)
    else:
        form = PostForm()

    return render(request, 'blog/post_form.html', {'form': form})
```

```html
<!-- templates/blog/post_form.html -->
{% extends 'base.html' %}

{% block content %}
    <h1>Create Post</h1>

    <form method="post">
        {% csrf_token %}
        {{ form.as_p }}
        <button type="submit">Submit</button>
    </form>
{% endblock %}
```

---

## URL 라우팅

### 프로젝트 URL 설정

```python
# myproject/urls.py
from django.contrib import admin
from django.urls import path, include

urlpatterns = [
    path('admin/', admin.site.urls),
    path('blog/', include('blog.urls')),
    path('accounts/', include('accounts.urls')),
]
```

### 앱 URL 설정

```python
# blog/urls.py
from django.urls import path
from . import views

urlpatterns = [
    # FBV
    path('', views.post_list, name='post_list'),
    path('post/<slug:slug>/', views.post_detail, name='post_detail'),

    # CBV
    path('posts/', views.PostListView.as_view(), name='post_list_cbv'),
    path('post/<slug:slug>/', views.PostDetailView.as_view(), name='post_detail_cbv'),
    path('post/create/', views.PostCreateView.as_view(), name='post_create'),
    path('post/<slug:slug>/edit/', views.PostUpdateView.as_view(), name='post_update'),
    path('post/<slug:slug>/delete/', views.PostDeleteView.as_view(), name='post_delete'),
]
```

---

## Admin 관리자 페이지

### Admin 등록

```python
# blog/admin.py
from django.contrib import admin
from .models import Category, Post, Comment

@admin.register(Category)
class CategoryAdmin(admin.ModelAdmin):
    list_display = ['name', 'slug']
    prepopulated_fields = {'slug': ('name',)}

@admin.register(Post)
class PostAdmin(admin.ModelAdmin):
    list_display = ['title', 'author', 'category', 'status', 'published_at']
    list_filter = ['status', 'category', 'created_at', 'published_at']
    search_fields = ['title', 'content']
    prepopulated_fields = {'slug': ('title',)}
    date_hierarchy = 'published_at'
    ordering = ['-published_at']

    # 인라인 편집
    class CommentInline(admin.TabularInline):
        model = Comment
        extra = 0

    inlines = [CommentInline]

@admin.register(Comment)
class CommentAdmin(admin.ModelAdmin):
    list_display = ['author', 'post', 'created_at', 'active']
    list_filter = ['active', 'created_at']
    search_fields = ['author__username', 'content']
    actions = ['approve_comments']

    def approve_comments(self, request, queryset):
        queryset.update(active=True)
    approve_comments.short_description = "Approve selected comments"
```

### 슈퍼유저 생성

```bash
python manage.py createsuperuser
# Username: admin
# Email: admin@example.com
# Password: ****
```

---

## 인증 시스템

### 내장 인증 뷰 사용

```python
# myproject/urls.py
from django.contrib.auth import views as auth_views

urlpatterns = [
    path('login/', auth_views.LoginView.as_view(template_name='accounts/login.html'), name='login'),
    path('logout/', auth_views.LogoutView.as_view(), name='logout'),
    path('password_change/', auth_views.PasswordChangeView.as_view(), name='password_change'),
]
```

### 커스텀 회원가입

```python
# accounts/forms.py
from django.contrib.auth.forms import UserCreationForm
from django.contrib.auth.models import User

class SignUpForm(UserCreationForm):
    email = forms.EmailField(required=True)

    class Meta:
        model = User
        fields = ['username', 'email', 'password1', 'password2']

# accounts/views.py
def signup(request):
    if request.method == 'POST':
        form = SignUpForm(request.POST)
        if form.is_valid():
            user = form.save()
            login(request, user)
            return redirect('post_list')
    else:
        form = SignUpForm()

    return render(request, 'accounts/signup.html', {'form': form})
```

### 로그인 필수 데코레이터

```python
from django.contrib.auth.decorators import login_required

@login_required
def protected_view(request):
    return render(request, 'protected.html')

# CBV
from django.contrib.auth.mixins import LoginRequiredMixin

class ProtectedView(LoginRequiredMixin, View):
    login_url = '/login/'
```

---

## Django REST Framework

### 설치 및 설정

```bash
pip install djangorestframework
```

```python
# settings.py
INSTALLED_APPS = [
    ...
    'rest_framework',
]

REST_FRAMEWORK = {
    'DEFAULT_PAGINATION_CLASS': 'rest_framework.pagination.PageNumberPagination',
    'PAGE_SIZE': 10
}
```

### Serializer

```python
# blog/serializers.py
from rest_framework import serializers
from .models import Post, Category

class CategorySerializer(serializers.ModelSerializer):
    class Meta:
        model = Category
        fields = ['id', 'name', 'slug']

class PostSerializer(serializers.ModelSerializer):
    author = serializers.ReadOnlyField(source='author.username')
    category = CategorySerializer(read_only=True)

    class Meta:
        model = Post
        fields = ['id', 'title', 'slug', 'author', 'category', 'content', 'published_at', 'status']
        read_only_fields = ['author']
```

### API Views

```python
# blog/api_views.py
from rest_framework import viewsets
from rest_framework.permissions import IsAuthenticatedOrReadOnly
from .models import Post
from .serializers import PostSerializer

class PostViewSet(viewsets.ModelViewSet):
    queryset = Post.objects.filter(status='published')
    serializer_class = PostSerializer
    permission_classes = [IsAuthenticatedOrReadOnly]

    def perform_create(self, serializer):
        serializer.save(author=self.request.user)
```

### API URL

```python
# blog/urls.py
from rest_framework.routers import DefaultRouter
from .api_views import PostViewSet

router = DefaultRouter()
router.register(r'posts', PostViewSet)

urlpatterns = [
    path('api/', include(router.urls)),
]
```

---

## 배포

### 프로덕션 설정

```python
# settings.py
DEBUG = False
ALLOWED_HOSTS = ['yourdomain.com', 'www.yourdomain.com']

# 정적 파일
STATIC_URL = '/static/'
STATIC_ROOT = BASE_DIR / 'staticfiles'

# 미디어 파일
MEDIA_URL = '/media/'
MEDIA_ROOT = BASE_DIR / 'media'
```

### 정적 파일 수집

```bash
python manage.py collectstatic
```

### Gunicorn으로 실행

```bash
pip install gunicorn

gunicorn myproject.wsgi:application --bind 0.0.0.0:8000
```

### requirements.txt

```
Django==5.0.1
djangorestframework==3.14.0
gunicorn==21.2.0
psycopg2-binary==2.9.9
python-decouple==3.8
Pillow==10.2.0
```

### Dockerfile

```dockerfile
FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

RUN python manage.py collectstatic --noinput

EXPOSE 8000

CMD ["gunicorn", "myproject.wsgi:application", "--bind", "0.0.0.0:8000"]
```

---

## 참고 자료

- [Django 공식 문서](https://docs.djangoproject.com/)
- [Django REST Framework 문서](https://www.django-rest-framework.org/)
- [Django Girls Tutorial](https://tutorial.djangogirls.org/)
- [Two Scoops of Django](https://www.feldroy.com/books/two-scoops-of-django-3-x)

*마지막 업데이트: 2025년 12월*
