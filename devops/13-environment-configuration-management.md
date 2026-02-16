# 환경별 설정 관리 (dev/staging/prod)

환경별 설정 관리는 동일한 애플리케이션 코드를 개발, 스테이징, 프로덕션 등 서로 다른 환경에서 안전하고 일관되게 실행하기 위한 전략이다. 설정의 외부화, Secret 관리, 환경 분리 원칙은 안정적인 CI/CD 파이프라인의 기반이 된다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### 환경 분리의 계층

```
┌──────────────────────────────────────────────────┐
│                   환경 구성                        │
│                                                  │
│  Local Dev  → Development  → Staging  → Production │
│  (개발자 PC)   (개발 서버)    (검증 서버)  (운영 서버) │
│                                                  │
│  설정이 다른 항목들:                                │
│  - DB 연결 정보                                    │
│  - API 엔드포인트                                  │
│  - 로그 레벨                                       │
│  - 기능 플래그                                     │
│  - 자원 할당 (CPU, Memory)                         │
│  - 인증/보안 설정                                  │
│  - 외부 서비스 연동 정보                            │
└──────────────────────────────────────────────────┘
```

### 설정의 종류

| 종류 | 예시 | 민감도 |
|------|------|--------|
| Application Config | 로그 레벨, 캐시 TTL, 타임아웃 | 낮음 |
| Infrastructure Config | DB 호스트, 포트, 커넥션 풀 크기 | 중간 |
| Secrets | DB 비밀번호, API 키, 인증서 | 높음 |
| Feature Flags | 기능 활성화/비활성화 | 낮음 |
| Environment Variables | 환경 구분자, 리전 정보 | 낮음 |

### Twelve-Factor App의 설정 원칙

> "설정을 코드에서 엄격하게 분리한다. 설정은 배포(deploy)마다 달라질 수 있지만, 코드는 그렇지 않다."
> — The Twelve-Factor App, Factor III: Config

```
코드 (Git에 저장, 모든 환경에서 동일)
  ↕ 분리
설정 (환경 변수/외부 시스템, 환경마다 다름)
```

## 2. 왜 알아야 하는가 (Why)

### 잘못된 설정 관리의 위험

| 위험 | 사례 |
|------|------|
| Secret 유출 | API 키가 Git에 커밋되어 공개됨 |
| 환경 오작동 | 프로덕션 서버가 개발 DB에 연결 |
| 설정 불일치 | 스테이징에서 테스트된 설정과 프로덕션 설정이 다름 |
| 롤백 실패 | 이전 버전과 현재 설정이 호환되지 않음 |

### 올바른 설정 관리의 가치

- 환경 간 **일관성** 보장
- 민감 정보의 **안전한** 관리
- 설정 변경의 **추적**과 **감사**
- 빠르고 안전한 **롤백**

## 3. 내부 구현 분석 (How)

### 설정 외부화 패턴

#### 1. 환경 변수 (Environment Variables)

가장 기본적이고 보편적인 방법:

```bash
# 환경 변수 설정
export DATABASE_URL=postgresql://user:pass@db.prod.internal:5432/myapp
export REDIS_URL=redis://cache.prod.internal:6379
export LOG_LEVEL=info
export APP_ENV=production
```

```java
// Spring Boot application.yml — 환경 변수 참조
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

logging:
  level:
    root: ${LOG_LEVEL:info}    # 기본값 지정

app:
  feature:
    new-ui: ${FEATURE_NEW_UI:false}
```

#### 2. 환경별 설정 파일

```
src/main/resources/
├── application.yml              # 공통 설정
├── application-dev.yml          # 개발 환경
├── application-staging.yml      # 스테이징 환경
└── application-prod.yml         # 프로덕션 환경
```

```yaml
# application.yml (공통)
spring:
  application:
    name: my-app
server:
  port: 8080

---
# application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/myapp_dev
  jpa:
    show-sql: true
logging:
  level:
    root: DEBUG

---
# application-staging.yml
spring:
  datasource:
    url: jdbc:postgresql://db-staging.internal:5432/myapp
  jpa:
    show-sql: false
logging:
  level:
    root: INFO

---
# application-prod.yml
spring:
  datasource:
    url: jdbc:postgresql://db-prod.internal:5432/myapp
  jpa:
    show-sql: false
logging:
  level:
    root: WARN
```

#### 3. Kubernetes ConfigMap / Secret

```yaml
# ConfigMap — 비민감 설정
apiVersion: v1
kind: ConfigMap
metadata:
  name: my-app-config
  namespace: production
data:
  APP_ENV: "production"
  LOG_LEVEL: "info"
  CACHE_TTL: "300"
  MAX_CONNECTIONS: "100"
---
# Secret — 민감 설정 (base64 인코딩)
apiVersion: v1
kind: Secret
metadata:
  name: my-app-secrets
  namespace: production
type: Opaque
data:
  DB_PASSWORD: cHJvZHVjdGlvbl9wYXNz        # base64
  API_KEY: c2VjcmV0X2FwaV9rZXk=
---
# Deployment에서 사용
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
spec:
  template:
    spec:
      containers:
        - name: my-app
          image: my-app:1.0.0
          envFrom:
            - configMapRef:
                name: my-app-config       # ConfigMap 전체 로드
            - secretRef:
                name: my-app-secrets      # Secret 전체 로드
          env:
            - name: DATABASE_URL
              valueFrom:
                secretKeyRef:
                  name: my-app-secrets
                  key: DB_PASSWORD
```

### Secret 관리 도구

#### HashiCorp Vault

```
┌──────────────────────────────────────────┐
│               Vault Server               │
│                                          │
│  Secret Engines:                         │
│  ├── kv/   (Key-Value)                  │
│  ├── aws/  (동적 AWS 자격증명)           │
│  ├── pki/  (인증서 발급)                 │
│  └── db/   (동적 DB 자격증명)            │
│                                          │
│  Auth Methods:                           │
│  ├── kubernetes (ServiceAccount)         │
│  ├── github                              │
│  └── approle                             │
│                                          │
│  Features:                               │
│  ├── Secret 자동 만료 & 갱신             │
│  ├── 감사 로그                           │
│  └── 동적 Secret 생성                    │
└──────────────────────────────────────────┘
```

```bash
# Vault에 Secret 저장
vault kv put secret/my-app/production \
    db_password="prod_secret_pass" \
    api_key="sk_live_xxx"

# Secret 읽기
vault kv get secret/my-app/production
```

#### AWS Secrets Manager / Parameter Store

```bash
# AWS Secrets Manager에 Secret 저장
aws secretsmanager create-secret \
    --name "my-app/production/db" \
    --secret-string '{"username":"admin","password":"prod_pass"}'

# Parameter Store에 설정 저장 (계층 구조)
aws ssm put-parameter \
    --name "/my-app/production/db-url" \
    --value "postgresql://db-prod:5432/myapp" \
    --type "SecureString"
```

#### Sealed Secrets (Kubernetes)

```bash
# 암호화된 Secret을 Git에 안전하게 저장
kubeseal --format yaml \
    --controller-namespace kube-system \
    < my-secret.yaml > sealed-secret.yaml

# sealed-secret.yaml은 Git에 커밋 가능
# 클러스터의 Sealed Secrets Controller만 복호화 가능
```

### GitOps 환경 관리

```
k8s-config/
├── base/                        # 공통 설정
│   ├── deployment.yaml
│   ├── service.yaml
│   └── kustomization.yaml
├── overlays/
│   ├── dev/                     # 개발 환경 오버레이
│   │   ├── kustomization.yaml
│   │   ├── configmap.yaml
│   │   └── resource-patch.yaml
│   ├── staging/                 # 스테이징 환경 오버레이
│   │   ├── kustomization.yaml
│   │   ├── configmap.yaml
│   │   └── resource-patch.yaml
│   └── production/              # 프로덕션 환경 오버레이
│       ├── kustomization.yaml
│       ├── configmap.yaml
│       ├── resource-patch.yaml
│       └── hpa.yaml
```

```yaml
# base/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - deployment.yaml
  - service.yaml

# overlays/production/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - ../../base
  - hpa.yaml
patchesStrategicMerge:
  - resource-patch.yaml
configMapGenerator:
  - name: my-app-config
    literals:
      - APP_ENV=production
      - LOG_LEVEL=warn

# overlays/production/resource-patch.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
spec:
  replicas: 4                   # 프로덕션은 4개 Pod
  template:
    spec:
      containers:
        - name: my-app
          resources:
            requests:
              cpu: 1000m         # 프로덕션은 더 많은 리소스
              memory: 1Gi
            limits:
              cpu: 2000m
              memory: 2Gi
```

## 4. 실전 예제

### 예제 1: GitHub Actions에서 환경별 배포

```yaml
name: Deploy

on:
  push:
    branches: [main, develop]

jobs:
  deploy:
    runs-on: ubuntu-latest
    # 환경별 다른 secrets와 설정 사용
    environment: ${{ github.ref == 'refs/heads/main' && 'production' || 'staging' }}

    steps:
      - uses: actions/checkout@v4

      - name: Set Environment Variables
        run: |
          if [ "${{ github.ref }}" = "refs/heads/main" ]; then
            echo "DEPLOY_ENV=production" >> "$GITHUB_ENV"
            echo "NAMESPACE=prod" >> "$GITHUB_ENV"
            echo "REPLICAS=4" >> "$GITHUB_ENV"
          else
            echo "DEPLOY_ENV=staging" >> "$GITHUB_ENV"
            echo "NAMESPACE=staging" >> "$GITHUB_ENV"
            echo "REPLICAS=2" >> "$GITHUB_ENV"
          fi

      - name: Deploy with Kustomize
        run: |
          kubectl apply -k overlays/${{ env.DEPLOY_ENV }}/
```

### 예제 2: 환경 변수 검증 스크립트

```bash
#!/bin/bash
# validate-env.sh — 배포 전 필수 환경 변수 검증

REQUIRED_VARS=(
    "DATABASE_URL"
    "REDIS_URL"
    "API_KEY"
    "APP_ENV"
)

MISSING=()
for var in "${REQUIRED_VARS[@]}"; do
    if [ -z "${!var}" ]; then
        MISSING+=("$var")
    fi
done

if [ ${#MISSING[@]} -gt 0 ]; then
    echo "ERROR: Missing required environment variables:"
    for var in "${MISSING[@]}"; do
        echo "  - $var"
    done
    exit 1
fi

# 환경별 검증
case "$APP_ENV" in
    production)
        if [[ "$DATABASE_URL" == *"localhost"* ]]; then
            echo "ERROR: Production cannot use localhost database!"
            exit 1
        fi
        ;;
    staging)
        if [[ "$DATABASE_URL" == *"prod"* ]]; then
            echo "WARNING: Staging is pointing to production database!"
            exit 1
        fi
        ;;
esac

echo "All environment variables validated successfully."
```

### 예제 3: Helm Values를 활용한 환경 관리

```yaml
# values-common.yaml (공통)
replicaCount: 1
image:
  repository: my-app
  pullPolicy: IfNotPresent
service:
  type: ClusterIP
  port: 8080
resources:
  requests:
    cpu: 250m
    memory: 256Mi

# values-staging.yaml
replicaCount: 2
image:
  tag: "staging-latest"
env:
  APP_ENV: staging
  LOG_LEVEL: debug
resources:
  requests:
    cpu: 500m
    memory: 512Mi

# values-production.yaml
replicaCount: 4
image:
  tag: "1.2.3"
env:
  APP_ENV: production
  LOG_LEVEL: warn
resources:
  requests:
    cpu: 1000m
    memory: 1Gi
  limits:
    cpu: 2000m
    memory: 2Gi
autoscaling:
  enabled: true
  minReplicas: 4
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
```

```bash
# Helm 배포
helm upgrade --install my-app ./charts/my-app \
    -f values-common.yaml \
    -f values-production.yaml \
    -n production
```

## 5. 정리

| 항목 | 설명 |
|------|------|
| 설정 외부화 | 코드와 설정의 엄격한 분리 (12-Factor) |
| 환경 변수 | 가장 기본적인 설정 주입 방법 |
| ConfigMap/Secret | Kubernetes 네이티브 설정 관리 |
| Vault | 중앙화된 Secret 관리 (동적 생성, 감사) |
| Kustomize | 환경별 오버레이로 설정 분리 |
| Helm Values | 환경별 values 파일로 설정 관리 |
| Sealed Secrets | 암호화된 Secret을 Git에 안전하게 저장 |

### 환경별 설정 관리 원칙

1. **코드와 설정을 분리** — 환경이 달라도 코드는 동일
2. **Secret은 코드에 절대 포함하지 않음** — .gitignore, git-secrets 활용
3. **환경 동등성(Parity) 유지** — 스테이징과 프로덕션 설정 차이 최소화
4. **설정 변경도 코드 리뷰** — GitOps로 설정 변경을 PR로 관리
5. **Secret 접근 권한 최소화** — RBAC으로 환경별 Secret 접근 제어

---
*참고: The Twelve-Factor App (12factor.net), HashiCorp Vault Documentation, Kubernetes ConfigMap and Secrets*
