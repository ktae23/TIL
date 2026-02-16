# GitHub Actions 고급 기능

GitHub Actions의 Matrix Build, Reusable Workflows, Composite Actions, Cache/Artifacts 등 고급 기능을 활용하면 복잡한 CI/CD 파이프라인을 효율적이고 DRY하게 구성할 수 있다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### Matrix Build

여러 OS, 언어 버전, 설정 조합으로 동일한 Job을 병렬 실행하는 기능이다. 하나의 Job 정의로 N개의 실행 인스턴스를 자동 생성한다.

### Reusable Workflows

다른 워크플로우에서 호출할 수 있는 재사용 가능한 워크플로우이다. `workflow_call` 이벤트로 트리거된다.

### Composite Actions

여러 Step을 하나의 Action으로 묶어 재사용하는 방식이다. `action.yml`에 정의하며 조직 내 공통 작업을 표준화할 수 있다.

### Cache와 Artifacts

- **Cache**: 의존성 등 빌드 입력물을 캐싱하여 실행 시간 단축
- **Artifacts**: 빌드 결과물을 저장하고 Job 간 또는 후속 작업에서 다운로드

## 2. 왜 알아야 하는가 (Why)

| 문제 | 해결 기능 |
|------|----------|
| 여러 환경에서 테스트해야 함 | Matrix Build |
| 워크플로우 코드 중복 | Reusable Workflows |
| 공통 Step 반복 작성 | Composite Actions |
| 매번 의존성 재설치 → 느린 빌드 | Cache |
| Job 간 빌드 결과 공유 | Artifacts |

## 3. 내부 구현 분석 (How)

### Matrix Build 상세

```yaml
jobs:
  test:
    runs-on: ${{ matrix.os }}
    strategy:
      matrix:
        os: [ubuntu-latest, macos-latest, windows-latest]
        node-version: [18, 20, 22]
        # 총 3 x 3 = 9개 조합이 병렬 실행됨
      fail-fast: false  # 하나 실패해도 나머지 계속 실행

    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: ${{ matrix.node-version }}
      - run: npm ci
      - run: npm test
```

#### Matrix Include/Exclude

```yaml
strategy:
  matrix:
    os: [ubuntu-latest, windows-latest]
    node-version: [18, 20]
    include:
      # 특정 조합에 추가 변수 설정
      - os: ubuntu-latest
        node-version: 20
        coverage: true
    exclude:
      # 특정 조합 제외
      - os: windows-latest
        node-version: 18
```

#### 동적 Matrix

```yaml
jobs:
  generate-matrix:
    runs-on: ubuntu-latest
    outputs:
      matrix: ${{ steps.set-matrix.outputs.matrix }}
    steps:
      - id: set-matrix
        run: |
          echo 'matrix={"service":["api","web","worker"]}' >> "$GITHUB_OUTPUT"

  build:
    needs: generate-matrix
    runs-on: ubuntu-latest
    strategy:
      matrix: ${{ fromJson(needs.generate-matrix.outputs.matrix) }}
    steps:
      - run: echo "Building ${{ matrix.service }}"
```

### Reusable Workflows

#### 재사용 워크플로우 정의 (호출받는 쪽)

```yaml
# .github/workflows/reusable-deploy.yml
name: Reusable Deploy

on:
  workflow_call:          # 다른 워크플로우에서 호출 가능
    inputs:
      environment:
        required: true
        type: string
      version:
        required: true
        type: string
    secrets:
      deploy-key:
        required: true
    outputs:
      deploy-url:
        description: "배포된 URL"
        value: ${{ jobs.deploy.outputs.url }}

jobs:
  deploy:
    runs-on: ubuntu-latest
    outputs:
      url: ${{ steps.deploy.outputs.url }}
    environment: ${{ inputs.environment }}
    steps:
      - uses: actions/checkout@v4
      - name: Deploy
        id: deploy
        env:
          DEPLOY_KEY: ${{ secrets.deploy-key }}
        run: |
          echo "Deploying ${{ inputs.version }} to ${{ inputs.environment }}"
          echo "url=https://${{ inputs.environment }}.example.com" >> "$GITHUB_OUTPUT"
```

#### 호출하는 쪽

```yaml
# .github/workflows/main.yml
name: Main Pipeline

on:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm test

  deploy-staging:
    needs: test
    uses: ./.github/workflows/reusable-deploy.yml  # 재사용 워크플로우 호출
    with:
      environment: staging
      version: ${{ github.sha }}
    secrets:
      deploy-key: ${{ secrets.STAGING_DEPLOY_KEY }}

  deploy-production:
    needs: deploy-staging
    uses: ./.github/workflows/reusable-deploy.yml
    with:
      environment: production
      version: ${{ github.sha }}
    secrets:
      deploy-key: ${{ secrets.PROD_DEPLOY_KEY }}
```

### Composite Actions

```yaml
# .github/actions/setup-and-build/action.yml
name: 'Setup and Build'
description: 'Node.js 설정, 의존성 설치, 빌드 실행'

inputs:
  node-version:
    description: 'Node.js version'
    required: false
    default: '20'

outputs:
  build-path:
    description: 'Build output path'
    value: ${{ steps.build.outputs.path }}

runs:
  using: 'composite'          # Composite Action
  steps:
    - name: Setup Node.js
      uses: actions/setup-node@v4
      with:
        node-version: ${{ inputs.node-version }}
        cache: 'npm'

    - name: Install Dependencies
      shell: bash                # composite에서는 shell 명시 필수
      run: npm ci

    - name: Build
      id: build
      shell: bash
      run: |
        npm run build
        echo "path=./dist" >> "$GITHUB_OUTPUT"
```

사용:
```yaml
steps:
  - uses: actions/checkout@v4
  - uses: ./.github/actions/setup-and-build   # 로컬 Composite Action
    with:
      node-version: '20'
```

### Cache

```yaml
steps:
  - uses: actions/checkout@v4

  # 방법 1: setup-node의 내장 캐시
  - uses: actions/setup-node@v4
    with:
      node-version: '20'
      cache: 'npm'            # 자동으로 node_modules 캐시

  # 방법 2: 수동 캐시 설정
  - name: Cache node_modules
    uses: actions/cache@v4
    with:
      path: node_modules
      key: ${{ runner.os }}-node-${{ hashFiles('**/package-lock.json') }}
      restore-keys: |
        ${{ runner.os }}-node-

  # 방법 3: Gradle 캐시
  - name: Cache Gradle
    uses: actions/cache@v4
    with:
      path: |
        ~/.gradle/caches
        ~/.gradle/wrapper
      key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
```

### Artifacts

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm run build

      # Artifact 업로드
      - uses: actions/upload-artifact@v4
        with:
          name: build-output
          path: dist/
          retention-days: 5       # 보관 기간

  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      # Artifact 다운로드
      - uses: actions/download-artifact@v4
        with:
          name: build-output
          path: dist/

      - run: echo "Deploying from dist/"
```

## 4. 실전 예제

### 예제 1: 모노레포 변경 감지 + Matrix

```yaml
name: Monorepo CI

on:
  push:
    branches: [main]
  pull_request:

jobs:
  detect-changes:
    runs-on: ubuntu-latest
    outputs:
      services: ${{ steps.filter.outputs.changes }}
    steps:
      - uses: actions/checkout@v4
      - uses: dorny/paths-filter@v3
        id: filter
        with:
          filters: |
            api:
              - 'services/api/**'
            web:
              - 'services/web/**'
            shared:
              - 'packages/shared/**'

  build:
    needs: detect-changes
    if: needs.detect-changes.outputs.services != '[]'
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service: ${{ fromJson(needs.detect-changes.outputs.services) }}
    steps:
      - uses: actions/checkout@v4
      - run: echo "Building ${{ matrix.service }}"
```

### 예제 2: 완성된 CI/CD 파이프라인 (Reusable 활용)

```yaml
name: Full CI/CD

on:
  push:
    branches: [main]

jobs:
  ci:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: ./.github/actions/setup-and-build
      - run: npm test
      - uses: actions/upload-artifact@v4
        with:
          name: app-build
          path: dist/

  deploy-staging:
    needs: ci
    uses: ./.github/workflows/reusable-deploy.yml
    with:
      environment: staging
      version: ${{ github.sha }}
    secrets:
      deploy-key: ${{ secrets.STAGING_KEY }}

  e2e-test:
    needs: deploy-staging
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm run test:e2e -- --base-url=${{ needs.deploy-staging.outputs.deploy-url }}

  deploy-production:
    needs: e2e-test
    uses: ./.github/workflows/reusable-deploy.yml
    with:
      environment: production
      version: ${{ github.sha }}
    secrets:
      deploy-key: ${{ secrets.PROD_KEY }}
```

## 5. 정리

| 기능 | 용도 | 핵심 키워드 |
|------|------|------------|
| Matrix Build | 다중 환경 병렬 테스트 | `strategy.matrix`, `include/exclude` |
| Reusable Workflows | 워크플로우 간 재사용 | `workflow_call`, `uses: ./.github/workflows/` |
| Composite Actions | Step 묶음 재사용 | `action.yml`, `using: composite` |
| Cache | 의존성 캐싱으로 속도 향상 | `actions/cache@v4`, `hashFiles()` |
| Artifacts | Job 간 빌드 결과 공유 | `upload-artifact`, `download-artifact` |
| Dynamic Matrix | 런타임에 Matrix 생성 | `fromJson()`, `outputs` |

### 재사용 전략 선택 가이드

```
같은 리포 내 여러 워크플로우에서 사용?
├── Step 수준 재사용 → Composite Action
└── Job/Workflow 수준 재사용 → Reusable Workflow

여러 리포에서 사용?
├── 조직 내 Action → .github 리포에 Composite Action
└── 공개 배포 → Marketplace에 Action 게시
```

---
*참고: GitHub Actions Documentation - Reusing Workflows, Creating Composite Actions*
