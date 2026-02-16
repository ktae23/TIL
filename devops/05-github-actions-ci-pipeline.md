# GitHub Actions CI 파이프라인 구축 실습

GitHub Actions로 실제 프로젝트에 적용할 수 있는 CI 파이프라인을 구축한다. 테스트 자동화, 코드 품질 검사, Docker 이미지 빌드, 배포 워크플로우까지 프로덕션 수준의 파이프라인을 단계별로 완성한다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### 프로덕션 수준 CI 파이프라인의 구성 요소

```
┌─────────────────────────────────────────────────────────┐
│                   CI Pipeline                           │
│                                                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │   Lint   │  │   Test   │  │ Security │  ← 병렬     │
│  │  Check   │  │  Suite   │  │   Scan   │             │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘             │
│       └──────────────┼─────────────┘                    │
│                      ↓                                  │
│              ┌──────────────┐                           │
│              │    Build     │                           │
│              │Docker Image  │                           │
│              └──────┬───────┘                           │
│                     ↓                                   │
│              ┌──────────────┐                           │
│              │    Push      │                           │
│              │  Registry    │                           │
│              └──────┬───────┘                           │
│                     ↓                                   │
│              ┌──────────────┐                           │
│              │   Deploy     │                           │
│              │  Staging     │                           │
│              └──────────────┘                           │
└─────────────────────────────────────────────────────────┘
```

## 2. 왜 알아야 하는가 (Why)

단순히 `npm test`만 실행하는 파이프라인은 프로덕션 환경에서 부족하다. 실제 서비스 운영에서는:

- **코드 품질 게이트**: 코드 리뷰 전 자동 검사
- **보안 스캔**: 의존성 취약점, 시크릿 노출 탐지
- **다중 환경 테스트**: DB, 캐시 등 외부 서비스와의 통합 테스트
- **자동 배포**: 검증 완료 후 자동 스테이징/프로덕션 배포

이 모든 것이 자동화된 파이프라인으로 구현되어야 한다.

## 3. 내부 구현 분석 (How)

### Step 1: 코드 품질 검사 (Lint & Format)

```yaml
name: Code Quality

on:
  pull_request:
    branches: [main]

jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'

      - run: npm ci

      - name: ESLint
        run: npx eslint . --format=json --output-file=eslint-report.json
        continue-on-error: true

      - name: Prettier Check
        run: npx prettier --check "src/**/*.{ts,tsx}"

      - name: TypeScript Type Check
        run: npx tsc --noEmit

      - name: Upload ESLint Report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: eslint-report
          path: eslint-report.json
```

### Step 2: 테스트 자동화 (Unit + Integration)

```yaml
jobs:
  unit-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
      - run: npm ci
      - name: Run Unit Tests with Coverage
        run: npm test -- --coverage --coverageReporters=text --coverageReporters=lcov
      - name: Check Coverage Threshold
        run: |
          COVERAGE=$(npx coverage-summary | grep 'Lines' | awk '{print $2}')
          echo "Line coverage: $COVERAGE%"

      - name: Upload Coverage
        uses: actions/upload-artifact@v4
        with:
          name: coverage-report
          path: coverage/

  integration-test:
    runs-on: ubuntu-latest
    services:                        # Service Container 활용
      postgres:
        image: postgres:16
        env:
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
          POSTGRES_DB: testdb
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432

      redis:
        image: redis:7
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 6379:6379

    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
      - run: npm ci
      - name: Run Integration Tests
        env:
          DATABASE_URL: postgresql://test:test@localhost:5432/testdb
          REDIS_URL: redis://localhost:6379
        run: npm run test:integration
```

### Step 3: 보안 스캔

```yaml
jobs:
  security:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      # 의존성 취약점 스캔
      - name: Dependency Audit
        run: npm audit --audit-level=high

      # GitHub 기본 보안 스캔 (CodeQL)
      - name: Initialize CodeQL
        uses: github/codeql-action/init@v3
        with:
          languages: javascript-typescript

      - name: Perform CodeQL Analysis
        uses: github/codeql-action/analyze@v3

      # 시크릿 노출 탐지
      - name: Secret Scanning
        uses: trufflesecurity/trufflehog@main
        with:
          extra_args: --only-verified
```

### Step 4: Docker 이미지 빌드

```yaml
jobs:
  build-image:
    needs: [lint, unit-test, integration-test, security]
    runs-on: ubuntu-latest
    outputs:
      image-tag: ${{ steps.meta.outputs.tags }}
      image-digest: ${{ steps.build.outputs.digest }}

    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Docker Meta
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ghcr.io/${{ github.repository }}
          tags: |
            type=sha,prefix=
            type=ref,event=branch
            type=semver,pattern={{version}}

      - name: Login to GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and Push
        id: build
        uses: docker/build-push-action@v6
        with:
          context: .
          push: ${{ github.event_name != 'pull_request' }}
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

### Step 5: 스테이징 배포

```yaml
jobs:
  deploy-staging:
    needs: build-image
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    environment:
      name: staging
      url: https://staging.example.com

    steps:
      - uses: actions/checkout@v4

      - name: Configure kubectl
        uses: azure/setup-kubectl@v4

      - name: Set Kubeconfig
        run: |
          mkdir -p ~/.kube
          echo "${{ secrets.KUBE_CONFIG_STAGING }}" | base64 -d > ~/.kube/config

      - name: Deploy to Staging
        run: |
          kubectl set image deployment/app \
            app=ghcr.io/${{ github.repository }}:${{ github.sha }} \
            -n staging

      - name: Wait for Rollout
        run: |
          kubectl rollout status deployment/app -n staging --timeout=300s

      - name: Smoke Test
        run: |
          for i in $(seq 1 10); do
            STATUS=$(curl -s -o /dev/null -w "%{http_code}" https://staging.example.com/health)
            if [ "$STATUS" = "200" ]; then
              echo "Health check passed"
              exit 0
            fi
            sleep 5
          done
          echo "Health check failed"
          exit 1
```

## 4. 실전 예제

### 예제 1: Spring Boot 프로젝트 완성 파이프라인

```yaml
name: Spring Boot CI/CD

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

env:
  JAVA_VERSION: '21'
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      mysql:
        image: mysql:8
        env:
          MYSQL_ROOT_PASSWORD: test
          MYSQL_DATABASE: testdb
        ports:
          - 3306:3306
        options: >-
          --health-cmd "mysqladmin ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v4

      - name: Setup JDK
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: temurin

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Run Tests
        env:
          SPRING_DATASOURCE_URL: jdbc:mysql://localhost:3306/testdb
          SPRING_DATASOURCE_USERNAME: root
          SPRING_DATASOURCE_PASSWORD: test
        run: ./gradlew test

      - name: Upload Test Results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: build/reports/tests/

  build-and-push:
    needs: test
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - uses: actions/checkout@v4

      - name: Setup JDK
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: temurin

      - name: Build JAR
        run: ./gradlew bootJar

      - name: Login to GHCR
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and Push Docker Image
        uses: docker/build-push-action@v6
        with:
          context: .
          push: true
          tags: |
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:latest
```

### 예제 2: PR 체크 워크플로우 (Status Checks)

```yaml
name: PR Checks

on:
  pull_request:
    branches: [main]

concurrency:
  group: ${{ github.workflow }}-${{ github.head_ref }}
  cancel-in-progress: true          # 같은 PR의 이전 실행 취소

jobs:
  changes:
    runs-on: ubuntu-latest
    outputs:
      src: ${{ steps.filter.outputs.src }}
      docs: ${{ steps.filter.outputs.docs }}
    steps:
      - uses: actions/checkout@v4
      - uses: dorny/paths-filter@v3
        id: filter
        with:
          filters: |
            src:
              - 'src/**'
              - 'package.json'
            docs:
              - 'docs/**'

  test:
    needs: changes
    if: needs.changes.outputs.src == 'true'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
      - run: npm ci
      - run: npm test

  lint:
    needs: changes
    if: needs.changes.outputs.src == 'true'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
      - run: npm ci
      - run: npx eslint .

  # PR에 필수 Status Check로 설정할 Job
  status-check:
    if: always()
    needs: [test, lint]
    runs-on: ubuntu-latest
    steps:
      - name: Check Results
        run: |
          if [[ "${{ needs.test.result }}" == "failure" ]] || \
             [[ "${{ needs.lint.result }}" == "failure" ]]; then
            echo "Required checks failed"
            exit 1
          fi
          echo "All checks passed"
```

## 5. 정리

| 파이프라인 단계 | 목적 | 주요 도구/Action |
|----------------|------|-----------------|
| Lint & Format | 코드 스타일 일관성 | ESLint, Prettier, tsc |
| Unit Test | 기능 정확성 검증 | Jest, JUnit, pytest |
| Integration Test | 외부 시스템 연동 테스트 | Service Containers |
| Security Scan | 취약점 탐지 | CodeQL, npm audit, TruffleHog |
| Docker Build | 컨테이너 이미지 생성 | docker/build-push-action |
| Deploy | 환경별 배포 | kubectl, Helm, ArgoCD |

### 파이프라인 최적화 체크리스트

- [ ] `concurrency`로 중복 실행 방지
- [ ] `paths-filter`로 변경된 코드만 검사
- [ ] Docker Layer Cache (`cache-from: type=gha`) 활성화
- [ ] 의존성 캐시 (`actions/cache` 또는 `setup-*`의 cache 옵션)
- [ ] 병렬 실행 가능한 Job은 `needs` 없이 독립 실행
- [ ] `fail-fast: false`로 한 Job 실패 시에도 다른 결과 확인

---
*참고: GitHub Actions Best Practices - docs.github.com/en/actions/using-workflows*
