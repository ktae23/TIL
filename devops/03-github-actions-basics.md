# GitHub Actions 기초와 워크플로우 작성

GitHub Actions는 GitHub에 내장된 CI/CD 플랫폼으로, 리포지토리에서 발생하는 이벤트에 반응하여 자동화된 워크플로우를 실행한다. YAML 기반 설정으로 빌드, 테스트, 배포 파이프라인을 구성할 수 있다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### GitHub Actions의 핵심 구성 요소

```
Workflow (.yml 파일)
├── Event (트리거)
│   └── push, pull_request, schedule, workflow_dispatch ...
├── Job (작업 단위)
│   ├── runs-on: 실행 환경 (Runner)
│   ├── needs: 의존 관계
│   └── Step (실행 단계)
│       ├── uses: 재사용 가능한 Action
│       └── run: 셸 커맨드
└── 환경 변수, Secrets, Artifacts
```

**Workflow**: 자동화된 프로세스의 전체 정의. `.github/workflows/` 디렉토리에 YAML 파일로 저장한다.

**Event**: 워크플로우를 트리거하는 이벤트. push, pull_request, schedule 등이 있다.

**Job**: 워크플로우 내에서 같은 Runner에서 실행되는 Step들의 집합. 기본적으로 Job은 병렬 실행된다.

**Step**: Job 내에서 순차 실행되는 개별 작업. 셸 커맨드(`run`) 또는 Action(`uses`)을 실행한다.

**Action**: 재사용 가능한 작업 단위. GitHub Marketplace에서 공유되거나 직접 작성할 수 있다.

**Runner**: 워크플로우가 실행되는 서버. GitHub-hosted Runner 또는 Self-hosted Runner를 사용한다.

## 2. 왜 알아야 하는가 (Why)

### GitHub Actions의 장점

| 장점 | 설명 |
|------|------|
| GitHub 네이티브 통합 | PR, Issue, Release 등 GitHub 기능과 긴밀한 연동 |
| 무료 tier 제공 | Public 리포: 무제한, Private: 월 2,000분 (Free) |
| Marketplace 생태계 | 수만 개의 재사용 가능한 Action |
| 빠른 시작 | 별도 서버 설치 없이 바로 사용 가능 |
| Matrix Build | 다양한 OS/언어 버전 조합 테스트 |

### 언제 GitHub Actions를 선택하는가?

- GitHub을 코드 저장소로 사용하는 경우
- 별도 CI/CD 서버 관리를 원하지 않는 경우
- 오픈소스 프로젝트
- 소~중규모 팀의 일반적인 CI/CD 요구

## 3. 내부 구현 분석 (How)

### 워크플로우 파일 구조

```yaml
# .github/workflows/ci.yml

name: CI Pipeline              # 워크플로우 이름

on:                            # 트리거 이벤트 정의
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:                           # 워크플로우 수준 환경 변수
  NODE_VERSION: '20'

jobs:                          # Job 정의
  build:                       # Job ID
    name: Build & Test         # Job 표시 이름
    runs-on: ubuntu-latest     # Runner 지정

    steps:                     # Step 정의
      - name: Checkout
        uses: actions/checkout@v4   # Action 사용

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:                       # Action 입력 파라미터
          node-version: ${{ env.NODE_VERSION }}

      - name: Install Dependencies
        run: npm ci                 # 셸 커맨드 실행

      - name: Run Tests
        run: npm test
```

### 이벤트 (Triggers) 상세

#### Push/PR 이벤트

```yaml
on:
  push:
    branches:
      - main
      - 'release/**'        # 와일드카드 패턴
    paths:
      - 'src/**'             # 특정 경로 변경 시만 트리거
      - '!docs/**'           # docs 변경은 제외
    tags:
      - 'v*'                 # 태그 푸시

  pull_request:
    branches: [main]
    types: [opened, synchronize, reopened]  # PR 이벤트 타입
```

#### 스케줄 이벤트

```yaml
on:
  schedule:
    - cron: '0 9 * * 1-5'   # 평일 09:00 UTC
```

#### 수동 실행

```yaml
on:
  workflow_dispatch:
    inputs:
      environment:
        description: 'Deploy target environment'
        required: true
        type: choice
        options:
          - staging
          - production
      version:
        description: 'Version to deploy'
        required: true
        type: string
```

### Job 실행 모델

```
┌─────────────────────────────────────────────┐
│              Workflow Run                    │
│                                             │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐    │
│  │  lint    │  │  test   │  │  build  │    │
│  │ (Job 1) │  │ (Job 2) │  │ (Job 3) │    │
│  └────┬────┘  └────┬────┘  └────┬────┘    │
│       │             │            │          │
│       └──────┬──────┘            │          │
│              │ needs: [lint,test]│          │
│              ↓                   │          │
│        ┌──────────┐             │          │
│        │  deploy  │←────────────┘          │
│        │ (Job 4)  │ needs: [build]         │
│        └──────────┘                        │
└─────────────────────────────────────────────┘
```

```yaml
jobs:
  lint:
    runs-on: ubuntu-latest
    steps: [...]

  test:
    runs-on: ubuntu-latest
    steps: [...]

  build:
    runs-on: ubuntu-latest
    steps: [...]

  deploy:
    needs: [lint, test, build]    # 세 Job 완료 후 실행
    runs-on: ubuntu-latest
    steps: [...]
```

### Secrets 관리

```yaml
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Deploy
        env:
          AWS_ACCESS_KEY_ID: ${{ secrets.AWS_ACCESS_KEY_ID }}
          AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        run: aws s3 sync ./build s3://my-bucket

      - name: Notify Slack
        env:
          SLACK_WEBHOOK: ${{ secrets.SLACK_WEBHOOK_URL }}
        run: |
          curl -X POST "$SLACK_WEBHOOK" \
            -H 'Content-Type: application/json' \
            -d '{"text": "Deployment completed"}'
```

**Secrets 설정 위치:**
- Repository Settings → Secrets and variables → Actions
- Organization-level Secrets (여러 리포 공유)
- Environment-level Secrets (환경별 분리)

### Context와 Expression

```yaml
steps:
  - name: Conditional Step
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    run: echo "This runs only on push to main"

  - name: Use Context
    run: |
      echo "Repository: ${{ github.repository }}"
      echo "Actor: ${{ github.actor }}"
      echo "SHA: ${{ github.sha }}"
      echo "Ref: ${{ github.ref }}"
      echo "Run ID: ${{ github.run_id }}"
```

주요 Context:

| Context | 설명 | 예시 |
|---------|------|------|
| `github` | 이벤트/리포 정보 | `github.ref`, `github.sha` |
| `env` | 환경 변수 | `env.NODE_VERSION` |
| `secrets` | Secret 값 | `secrets.API_KEY` |
| `job` | 현재 Job 정보 | `job.status` |
| `steps` | 이전 Step 출력 | `steps.build.outputs.version` |
| `runner` | Runner 정보 | `runner.os`, `runner.temp` |
| `matrix` | Matrix 변수 | `matrix.node-version` |

## 4. 실전 예제

### 예제 1: Node.js 프로젝트 기본 CI

```yaml
name: Node.js CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest

    strategy:
      matrix:
        node-version: [18, 20, 22]

    steps:
      - uses: actions/checkout@v4

      - name: Use Node.js ${{ matrix.node-version }}
        uses: actions/setup-node@v4
        with:
          node-version: ${{ matrix.node-version }}
          cache: 'npm'

      - run: npm ci
      - run: npm run build --if-present
      - run: npm test
```

### 예제 2: Java/Gradle 프로젝트 CI

```yaml
name: Java CI with Gradle

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Build with Gradle
        run: ./gradlew build

      - name: Run Tests
        run: ./gradlew test

      - name: Upload Test Report
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: test-report
          path: build/reports/tests/
```

### 예제 3: Docker 이미지 빌드 및 푸시

```yaml
name: Docker Build & Push

on:
  push:
    tags: ['v*']

jobs:
  docker:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Docker meta
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ghcr.io/${{ github.repository }}
          tags: |
            type=semver,pattern={{version}}
            type=semver,pattern={{major}}.{{minor}}

      - name: Login to GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push
        uses: docker/build-push-action@v6
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
```

## 5. 정리

| 구성 요소 | 역할 | 핵심 포인트 |
|-----------|------|------------|
| Workflow | 자동화 프로세스 전체 정의 | `.github/workflows/*.yml` |
| Event | 워크플로우 트리거 | push, PR, schedule, manual |
| Job | 실행 단위 (기본 병렬) | `needs`로 의존관계 설정 |
| Step | Job 내 순차 실행 단위 | `uses`(Action) 또는 `run`(Shell) |
| Action | 재사용 가능한 작업 | Marketplace에서 검색 |
| Runner | 실행 환경 | GitHub-hosted 또는 Self-hosted |
| Secrets | 민감 정보 관리 | Repository/Org/Environment 수준 |

---
*참고: GitHub Actions Documentation - docs.github.com/en/actions*
