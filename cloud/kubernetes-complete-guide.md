# Kubernetes 완벽 가이드

Kubernetes의 핵심 개념부터 Spring Boot 배포, Helm, 스케일링, AWS EKS, 세무 도메인 적용까지 총정리한다.

## 목차

- [1. Kubernetes 핵심 개념](#1-kubernetes-핵심-개념)
  - [1.1 Kubernetes란?](#11-kubernetes란)
  - [1.2 클러스터 아키텍처](#12-클러스터-아키텍처)
  - [1.3 Pod](#13-pod)
  - [1.4 ReplicaSet](#14-replicaset)
  - [1.5 Deployment](#15-deployment)
  - [1.6 StatefulSet](#16-statefulset)
  - [1.7 Service](#17-service)
  - [1.8 Ingress](#18-ingress)
  - [1.9 ConfigMap과 Secret](#19-configmap과-secret)
  - [1.10 Namespace와 RBAC](#110-namespace와-rbac)
  - [1.11 Volume, PersistentVolume, PVC](#111-volume-persistentvolume-pvc)
- [2. Spring Boot + Kubernetes 배포](#2-spring-boot--kubernetes-배포)
  - [2.1 Dockerfile 멀티스테이지 빌드](#21-dockerfile-멀티스테이지-빌드)
  - [2.2 Deployment manifest 작성](#22-deployment-manifest-작성)
  - [2.3 Service와 Ingress 설정](#23-service와-ingress-설정)
  - [2.4 ConfigMap으로 application.yml 외부화](#24-configmap으로-applicationyml-외부화)
  - [2.5 Secret으로 DB 비밀번호 관리](#25-secret으로-db-비밀번호-관리)
  - [2.6 Health Probe 설정](#26-health-probe-설정)
  - [2.7 Graceful Shutdown](#27-graceful-shutdown)
- [3. Helm Chart](#3-helm-chart)
  - [3.1 Helm 기본 개념](#31-helm-기본-개념)
  - [3.2 Spring Boot용 Helm Chart 구조](#32-spring-boot용-helm-chart-구조)
  - [3.3 values.yaml 활용](#33-valuesyaml-활용)
  - [3.4 멀티 환경 배포](#34-멀티-환경-배포)
- [4. HPA와 스케일링](#4-hpa와-스케일링)
  - [4.1 HPA 설정](#41-hpa-설정)
  - [4.2 CPU/메모리 기반 오토스케일링](#42-cpu메모리-기반-오토스케일링)
  - [4.3 Custom Metrics 기반 스케일링](#43-custom-metrics-기반-스케일링)
  - [4.4 VPA](#44-vpa)
- [5. 배포 전략](#5-배포-전략)
  - [5.1 롤링 업데이트](#51-롤링-업데이트)
  - [5.2 Blue/Green 배포](#52-bluegreen-배포)
  - [5.3 카나리 배포](#53-카나리-배포)
- [6. AWS EKS](#6-aws-eks)
  - [6.1 EKS 클러스터 구성](#61-eks-클러스터-구성)
  - [6.2 IRSA](#62-irsa)
  - [6.3 ALB Ingress Controller](#63-alb-ingress-controller)
  - [6.4 EKS + RDS, ElastiCache 연동](#64-eks--rds-elasticache-연동)
- [7. 세무 도메인 적용](#7-세무-도메인-적용)
  - [7.1 종소세 시즌 트래픽 스케일링](#71-종소세-시즌-트래픽-스케일링)
  - [7.2 배치 Job의 CronJob 전환](#72-배치-job의-cronjob-전환)
  - [7.3 마이크로서비스별 배포 전략](#73-마이크로서비스별-배포-전략)
- [8. 운영 및 트러블슈팅](#8-운영-및-트러블슈팅)
  - [8.1 kubectl 필수 명령어](#81-kubectl-필수-명령어)
  - [8.2 로그 수집](#82-로그-수집)
  - [8.3 Pod 디버깅](#83-pod-디버깅)
- [핵심 정리](#핵심-정리)
- [면접 대비 핵심 질문](#면접-대비-핵심-질문)

---

# 1. Kubernetes 핵심 개념

## 1.1 Kubernetes란?

Kubernetes(K8s)는 컨테이너화된 애플리케이션의 배포, 스케일링, 운영을 자동화하는 오픈소스 컨테이너 오케스트레이션 플랫폼이다. Google이 내부에서 사용하던 Borg 시스템을 기반으로 2014년 오픈소스로 공개했다.

**핵심 특징:**
- **자동 스케일링**: 트래픽에 따라 Pod 수를 자동 조절
- **자가 치유(Self-healing)**: 장애 발생 시 자동으로 컨테이너 재시작/교체
- **서비스 디스커버리**: DNS 기반으로 서비스 간 통신 자동화
- **롤링 업데이트**: 무중단 배포 기본 지원
- **선언적 구성(Declarative Configuration)**: YAML로 원하는 상태를 선언하면 K8s가 맞춰줌

---

## 1.2 클러스터 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                      Kubernetes Cluster                         │
│                                                                 │
│  ┌───────────────────────── Control Plane ───────────────────┐  │
│  │                                                           │  │
│  │  ┌──────────────┐  ┌───────────────┐  ┌──────────────┐   │  │
│  │  │  API Server  │  │   Scheduler   │  │  Controller  │   │  │
│  │  │  (kube-api)  │  │               │  │   Manager    │   │  │
│  │  └──────┬───────┘  └───────────────┘  └──────────────┘   │  │
│  │         │                                                 │  │
│  │  ┌──────▼───────┐  ┌───────────────┐                     │  │
│  │  │     etcd     │  │  Cloud Ctrl   │                     │  │
│  │  │  (Key-Value) │  │   Manager     │                     │  │
│  │  └──────────────┘  └───────────────┘                     │  │
│  └───────────────────────────────────────────────────────────┘  │
│                              │                                   │
│                              ▼                                   │
│  ┌──────────────── Worker Node 1 ────────────────────────────┐  │
│  │  ┌─────────┐  ┌─────────────┐  ┌──────────────────────┐  │  │
│  │  │ kubelet │  │ kube-proxy  │  │  Container Runtime   │  │  │
│  │  └─────────┘  └─────────────┘  │  (containerd/CRI-O)  │  │  │
│  │                                 └──────────────────────┘  │  │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐                   │  │
│  │  │  Pod A  │  │  Pod B  │  │  Pod C  │                   │  │
│  │  └─────────┘  └─────────┘  └─────────┘                   │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────── Worker Node 2 ────────────────────────────┐  │
│  │  ┌─────────┐  ┌─────────────┐  ┌──────────────────────┐  │  │
│  │  │ kubelet │  │ kube-proxy  │  │  Container Runtime   │  │  │
│  │  └─────────┘  └─────────────┘  └──────────────────────┘  │  │
│  │  ┌─────────┐  ┌─────────┐                                │  │
│  │  │  Pod D  │  │  Pod E  │                                │  │
│  │  └─────────┘  └─────────┘                                │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

| 컴포넌트 | 역할 |
|---------|------|
| **API Server** | 모든 요청의 진입점. REST API로 클러스터 상태를 관리 |
| **etcd** | 클러스터의 모든 상태 데이터를 저장하는 분산 Key-Value 스토어 |
| **Scheduler** | 새로 생성된 Pod를 적절한 Node에 배치 |
| **Controller Manager** | ReplicaSet, Deployment 등 각종 컨트롤러 실행 |
| **kubelet** | 각 Node에서 Pod의 생명주기를 관리하는 에이전트 |
| **kube-proxy** | 네트워크 규칙 관리, Service의 로드밸런싱 구현 |

---

## 1.3 Pod

Pod는 Kubernetes에서 배포할 수 있는 가장 작은 단위다. 하나 이상의 컨테이너를 포함하며, 같은 Pod 내의 컨테이너는 네트워크와 스토리지를 공유한다.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: tax-api-pod
  labels:
    app: tax-api
    version: v1
spec:
  containers:
    - name: tax-api
      image: tax-service/tax-api:1.0.0
      ports:
        - containerPort: 8080
      resources:
        requests:
          cpu: "250m"
          memory: "512Mi"
        limits:
          cpu: "500m"
          memory: "1Gi"
      env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
    - name: log-agent        # 사이드카 컨테이너
      image: fluent/fluent-bit:latest
      volumeMounts:
        - name: log-volume
          mountPath: /var/log/app
  volumes:
    - name: log-volume
      emptyDir: {}
```

**Pod 핵심 특성:**
- Pod 내 컨테이너는 `localhost`로 서로 통신 가능
- Pod는 일시적(ephemeral)이다 - 언제든 삭제/재생성될 수 있음
- 직접 Pod를 생성하지 않고, Deployment를 통해 관리하는 것이 권장됨
- 각 Pod는 고유한 IP 주소를 부여받음

---

## 1.4 ReplicaSet

ReplicaSet은 지정된 수의 Pod 복제본이 항상 실행되도록 보장한다. Pod가 죽으면 자동으로 새 Pod를 생성한다.

```yaml
apiVersion: apps/v1
kind: ReplicaSet
metadata:
  name: tax-api-rs
spec:
  replicas: 3
  selector:
    matchLabels:
      app: tax-api
  template:
    metadata:
      labels:
        app: tax-api
    spec:
      containers:
        - name: tax-api
          image: tax-service/tax-api:1.0.0
          ports:
            - containerPort: 8080
```

> **실무 참고**: ReplicaSet을 직접 만들지 않고 Deployment를 통해 간접적으로 관리한다. Deployment가 ReplicaSet을 자동 생성/관리한다.

---

## 1.5 Deployment

Deployment는 Pod와 ReplicaSet의 선언적 업데이트를 제공하는 가장 많이 사용되는 워크로드 리소스다.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: tax-api
  labels:
    app: tax-api
spec:
  replicas: 3
  selector:
    matchLabels:
      app: tax-api
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1          # 최대 1개 추가 Pod 허용
      maxUnavailable: 0     # 서비스 중단 없이 배포
  template:
    metadata:
      labels:
        app: tax-api
    spec:
      containers:
        - name: tax-api
          image: tax-service/tax-api:2.0.0
          ports:
            - containerPort: 8080
          resources:
            requests:
              cpu: "500m"
              memory: "1Gi"
            limits:
              cpu: "1000m"
              memory: "2Gi"
```

**Deployment 동작 흐름:**

```
Deployment (tax-api)
    │
    ├── ReplicaSet (tax-api-6d4f5b7c8)  ← 이전 버전 (replicas: 0)
    │       ├── Pod (tax-api-6d4f5b7c8-abc12) [Terminated]
    │       └── Pod (tax-api-6d4f5b7c8-def34) [Terminated]
    │
    └── ReplicaSet (tax-api-9a8b7c6d5)  ← 현재 버전 (replicas: 3)
            ├── Pod (tax-api-9a8b7c6d5-ghi56) [Running]
            ├── Pod (tax-api-9a8b7c6d5-jkl78) [Running]
            └── Pod (tax-api-9a8b7c6d5-mno90) [Running]
```

**유용한 Deployment 명령어:**

```bash
# 배포 상태 확인
kubectl rollout status deployment/tax-api

# 배포 이력 확인
kubectl rollout history deployment/tax-api

# 이전 버전으로 롤백
kubectl rollout undo deployment/tax-api

# 특정 리비전으로 롤백
kubectl rollout undo deployment/tax-api --to-revision=2

# 스케일링
kubectl scale deployment/tax-api --replicas=5
```

---

## 1.6 StatefulSet

StatefulSet은 상태를 가진 애플리케이션(DB, 메시지 큐 등)을 위한 워크로드다. 각 Pod에 고정된 네트워크 ID와 영구 스토리지를 제공한다.

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
spec:
  serviceName: "postgres"
  replicas: 3
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
        - name: postgres
          image: postgres:15
          ports:
            - containerPort: 5432
          volumeMounts:
            - name: postgres-data
              mountPath: /var/lib/postgresql/data
          env:
            - name: POSTGRES_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: postgres-secret
                  key: password
  volumeClaimTemplates:
    - metadata:
        name: postgres-data
      spec:
        accessModes: ["ReadWriteOnce"]
        storageClassName: gp3
        resources:
          requests:
            storage: 50Gi
```

| 특성 | Deployment | StatefulSet |
|------|-----------|-------------|
| **Pod 이름** | 랜덤 해시 (tax-api-abc12) | 순차적 인덱스 (postgres-0, postgres-1) |
| **생성 순서** | 병렬 생성 | 순차적 생성 (0 → 1 → 2) |
| **삭제 순서** | 병렬 삭제 | 역순 삭제 (2 → 1 → 0) |
| **스토리지** | Pod 삭제 시 함께 삭제 | Pod 삭제해도 PVC 유지 |
| **네트워크** | 임의 IP | 고정 DNS (postgres-0.postgres.ns.svc.cluster.local) |
| **사용 사례** | Stateless 앱 (API 서버) | Stateful 앱 (DB, Kafka, Redis) |

---

## 1.7 Service

Service는 Pod 집합에 대한 안정적인 네트워크 엔드포인트를 제공한다. Pod는 언제든 죽고 다시 생성될 수 있지만, Service는 고정된 IP와 DNS를 제공한다.

```
┌─────────────────────────────────────────────────────────┐
│                    Service Types                         │
│                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌────────────────┐  │
│  │  ClusterIP  │  │  NodePort   │  │ LoadBalancer   │  │
│  │ (기본, 내부)│  │ (노드 포트) │  │  (외부 LB)     │  │
│  │             │  │             │  │                │  │
│  │ 클러스터    │  │ ClusterIP + │  │ NodePort +     │  │
│  │ 내부에서만  │  │ 각 노드의   │  │ 클라우드       │  │
│  │ 접근 가능   │  │ 특정 포트   │  │ 로드밸런서     │  │
│  └─────────────┘  └─────────────┘  └────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### ClusterIP (기본)

클러스터 내부에서만 접근 가능한 가상 IP를 할당한다.

```yaml
apiVersion: v1
kind: Service
metadata:
  name: tax-api-svc
spec:
  type: ClusterIP        # 기본값
  selector:
    app: tax-api
  ports:
    - port: 80            # Service 포트
      targetPort: 8080    # Pod 포트
      protocol: TCP
```

### NodePort

각 Node의 특정 포트를 통해 외부에서 접근할 수 있다.

```yaml
apiVersion: v1
kind: Service
metadata:
  name: tax-api-nodeport
spec:
  type: NodePort
  selector:
    app: tax-api
  ports:
    - port: 80
      targetPort: 8080
      nodePort: 30080     # 30000-32767 범위
```

### LoadBalancer

클라우드 프로바이더의 로드밸런서를 자동으로 프로비저닝한다.

```yaml
apiVersion: v1
kind: Service
metadata:
  name: tax-api-lb
  annotations:
    service.beta.kubernetes.io/aws-load-balancer-type: "nlb"
    service.beta.kubernetes.io/aws-load-balancer-scheme: "internet-facing"
spec:
  type: LoadBalancer
  selector:
    app: tax-api
  ports:
    - port: 443
      targetPort: 8080
      protocol: TCP
```

**Service DNS 규칙:**
- `{service-name}.{namespace}.svc.cluster.local`
- 예: `tax-api-svc.default.svc.cluster.local`
- 같은 Namespace 내에서는 `tax-api-svc`만으로 접근 가능

---

## 1.8 Ingress

Ingress는 HTTP/HTTPS 라우팅 규칙을 정의하여 외부 트래픽을 클러스터 내부 Service로 전달한다. Ingress Controller(Nginx, ALB 등)가 실제 동작을 수행한다.

```
┌──────────────────────────────────────────────────┐
│                    Internet                       │
└──────────────────────┬───────────────────────────┘
                       │
                       ▼
              ┌────────────────┐
              │    Ingress     │
              │   Controller   │
              │  (Nginx/ALB)   │
              └───────┬────────┘
                      │
          ┌───────────┼───────────┐
          │           │           │
          ▼           ▼           ▼
   /api/tax/*   /api/user/*   /api/filing/*
          │           │           │
          ▼           ▼           ▼
   ┌──────────┐ ┌──────────┐ ┌──────────┐
   │ tax-svc  │ │ user-svc │ │filing-svc│
   └──────────┘ └──────────┘ └──────────┘
```

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: tax-platform-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/rate-limit: "100"
spec:
  ingressClassName: nginx
  tls:
    - hosts:
        - api.tax-api.example.com
      secretName: tls-secret
  rules:
    - host: api.tax-api.example.com
      http:
        paths:
          - path: /api/tax
            pathType: Prefix
            backend:
              service:
                name: tax-api-svc
                port:
                  number: 80
          - path: /api/user
            pathType: Prefix
            backend:
              service:
                name: user-api-svc
                port:
                  number: 80
          - path: /api/filing
            pathType: Prefix
            backend:
              service:
                name: filing-api-svc
                port:
                  number: 80
```

---

## 1.9 ConfigMap과 Secret

### ConfigMap

환경별 설정을 Pod와 분리하여 관리한다.

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: tax-api-config
data:
  # 단순 키-값
  SPRING_PROFILES_ACTIVE: "production"
  SERVER_PORT: "8080"
  LOG_LEVEL: "INFO"

  # 파일 형태
  application.yml: |
    spring:
      datasource:
        url: jdbc:postgresql://postgres-svc:5432/taxdb
        hikari:
          maximum-pool-size: 20
          minimum-idle: 5
      redis:
        host: redis-svc
        port: 6379
    management:
      endpoints:
        web:
          exposure:
            include: health,info,prometheus
```

**ConfigMap 사용 방법:**

```yaml
# 환경변수로 주입
env:
  - name: SPRING_PROFILES_ACTIVE
    valueFrom:
      configMapKeyRef:
        name: tax-api-config
        key: SPRING_PROFILES_ACTIVE

# 볼륨으로 마운트
volumes:
  - name: config-volume
    configMap:
      name: tax-api-config
      items:
        - key: application.yml
          path: application.yml
```

### Secret

민감한 데이터(비밀번호, 토큰 등)를 Base64 인코딩하여 관리한다.

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: tax-db-secret
type: Opaque
data:
  username: dGF4X3VzZXI=          # echo -n "tax_user" | base64
  password: c3VwZXJfc2VjcmV0     # echo -n "super_secret" | base64

---
# stringData를 사용하면 Base64 인코딩 없이 작성 가능
apiVersion: v1
kind: Secret
metadata:
  name: tax-db-secret
type: Opaque
stringData:
  username: tax_user
  password: super_secret
```

> **주의**: Secret은 Base64 인코딩일 뿐 암호화가 아니다. 실 운영에서는 AWS Secrets Manager나 Vault와 연동하는 것을 권장한다.

---

## 1.10 Namespace와 RBAC

### Namespace

클러스터 내 리소스를 논리적으로 격리하는 가상 클러스터 단위다.

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: tax-production
  labels:
    env: production
    team: tax-service
```

```bash
# 네임스페이스별 리소스 조회
kubectl get pods -n tax-production
kubectl get all -n tax-staging

# 기본 네임스페이스 변경
kubectl config set-context --current --namespace=tax-production
```

**권장 네임스페이스 구조:**

```
├── tax-production      # 세무 서비스 프로덕션
├── tax-staging         # 세무 서비스 스테이징
├── tax-dev             # 세무 서비스 개발
├── monitoring          # Prometheus, Grafana
├── ingress-system      # Ingress Controller
└── kube-system         # K8s 시스템 컴포넌트
```

### RBAC (Role-Based Access Control)

```yaml
# Role: 네임스페이스 내 권한 정의
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  namespace: tax-production
  name: tax-developer
rules:
  - apiGroups: [""]
    resources: ["pods", "pods/log", "services", "configmaps"]
    verbs: ["get", "list", "watch"]
  - apiGroups: ["apps"]
    resources: ["deployments"]
    verbs: ["get", "list", "watch"]

---
# RoleBinding: 사용자에게 Role 부여
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: tax-developer-binding
  namespace: tax-production
subjects:
  - kind: User
    name: developer@example.com
    apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: Role
  name: tax-developer
  apiGroup: rbac.authorization.k8s.io
```

| 리소스 | 범위 | 설명 |
|--------|------|------|
| **Role** | Namespace | 특정 네임스페이스 내 권한 |
| **ClusterRole** | Cluster 전체 | 클러스터 전체 범위의 권한 |
| **RoleBinding** | Namespace | Role을 사용자에게 바인딩 |
| **ClusterRoleBinding** | Cluster 전체 | ClusterRole을 사용자에게 바인딩 |

---

## 1.11 Volume, PersistentVolume, PVC

```
┌──────────────────────────────────────────────────────┐
│                    Volume 계층 구조                    │
│                                                      │
│   Pod                  PVC                PV         │
│  ┌──────┐         ┌─────────┐        ┌─────────┐   │
│  │ App  │──mount──│  PVC    │──bind──│   PV    │   │
│  │      │         │ 10Gi   │        │  50Gi   │   │
│  └──────┘         │ RWO    │        │  gp3    │   │
│                    └─────────┘        └────┬────┘   │
│                                            │        │
│                                     ┌──────▼──────┐ │
│                                     │  EBS Volume │ │
│                                     │  (AWS)      │ │
│                                     └─────────────┘ │
└──────────────────────────────────────────────────────┘
```

### PersistentVolume (PV)

클러스터 관리자가 프로비저닝하는 스토리지 리소스다.

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: tax-data-pv
spec:
  capacity:
    storage: 50Gi
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  storageClassName: gp3
  csi:
    driver: ebs.csi.aws.com
    volumeHandle: vol-0abc123def456
```

### PersistentVolumeClaim (PVC)

사용자(개발자)가 스토리지를 요청하는 리소스다.

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: tax-data-pvc
spec:
  accessModes:
    - ReadWriteOnce
  storageClassName: gp3
  resources:
    requests:
      storage: 10Gi
```

| Access Mode | 약자 | 설명 |
|-------------|------|------|
| **ReadWriteOnce** | RWO | 단일 노드에서 읽기/쓰기 |
| **ReadOnlyMany** | ROX | 다수 노드에서 읽기 전용 |
| **ReadWriteMany** | RWX | 다수 노드에서 읽기/쓰기 (EFS 등) |

---

# 2. Spring Boot + Kubernetes 배포

## 2.1 Dockerfile 멀티스테이지 빌드

Java 21 + Spring Boot 3.2 기반의 최적화된 Dockerfile이다.

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Gradle 캐싱 최적화
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# 소스 복사 및 빌드
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# JAR 레이어 분리 (캐싱 최적화)
RUN java -Djarmode=layertools -jar build/libs/*.jar extract --destination /extracted

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime

# 보안: non-root 사용자
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

# 레이어별 복사 (변경 빈도 낮은 순서)
COPY --from=builder /extracted/dependencies/ ./
COPY --from=builder /extracted/spring-boot-loader/ ./
COPY --from=builder /extracted/snapshot-dependencies/ ./
COPY --from=builder /extracted/application/ ./

# JVM 옵션
ENV JAVA_OPTS="-XX:+UseG1GC \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseContainerSupport \
  -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
```

**핵심 포인트:**
- **멀티스테이지 빌드**: 빌드 도구를 최종 이미지에서 제거하여 이미지 크기 절감 (1GB+ → 200MB)
- **레이어 분리**: Spring Boot의 layertools로 의존성과 애플리케이션 코드를 분리하여 빌드 캐싱 효율 극대화
- **non-root 실행**: 컨테이너 보안 모범 사례
- **UseContainerSupport**: JVM이 컨테이너의 CPU/메모리 제한을 인식

---

## 2.2 Deployment manifest 작성

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: tax-api
  namespace: tax-production
  labels:
    app: tax-api
    version: v2.1.0
spec:
  replicas: 3
  revisionHistoryLimit: 5
  selector:
    matchLabels:
      app: tax-api
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app: tax-api
        version: v2.1.0
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      serviceAccountName: tax-api-sa
      terminationGracePeriodSeconds: 60

      # Pod 간 분산 배치
      topologySpreadConstraints:
        - maxSkew: 1
          topologyKey: topology.kubernetes.io/zone
          whenUnsatisfiable: DoNotSchedule
          labelSelector:
            matchLabels:
              app: tax-api

      containers:
        - name: tax-api
          image: 123456789.dkr.ecr.ap-northeast-2.amazonaws.com/tax-api:v2.1.0
          ports:
            - containerPort: 8080
              name: http

          resources:
            requests:
              cpu: "500m"
              memory: "1Gi"
            limits:
              cpu: "1000m"
              memory: "2Gi"

          envFrom:
            - configMapRef:
                name: tax-api-config

          env:
            - name: DB_USERNAME
              valueFrom:
                secretKeyRef:
                  name: tax-db-secret
                  key: username
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: tax-db-secret
                  key: password

          volumeMounts:
            - name: app-config
              mountPath: /app/config
              readOnly: true

          # Health Probes
          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
            failureThreshold: 30

          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            periodSeconds: 10
            failureThreshold: 3

          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            periodSeconds: 5
            failureThreshold: 3

      volumes:
        - name: app-config
          configMap:
            name: tax-api-config
            items:
              - key: application.yml
                path: application.yml
```

---

## 2.3 Service와 Ingress 설정

```yaml
apiVersion: v1
kind: Service
metadata:
  name: tax-api-svc
  namespace: tax-production
spec:
  type: ClusterIP
  selector:
    app: tax-api
  ports:
    - name: http
      port: 80
      targetPort: 8080
      protocol: TCP

---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: tax-api-ingress
  namespace: tax-production
  annotations:
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:ap-northeast-2:123456789:certificate/xxx
    alb.ingress.kubernetes.io/healthcheck-path: /actuator/health
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTPS":443}]'
    alb.ingress.kubernetes.io/ssl-redirect: "443"
spec:
  ingressClassName: alb
  rules:
    - host: api.tax-api.example.com
      http:
        paths:
          - path: /api/tax
            pathType: Prefix
            backend:
              service:
                name: tax-api-svc
                port:
                  number: 80
```

---

## 2.4 ConfigMap으로 application.yml 외부화

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: tax-api-config
  namespace: tax-production
data:
  SPRING_PROFILES_ACTIVE: "production"
  JAVA_OPTS: >-
    -XX:+UseG1GC
    -XX:MaxRAMPercentage=75.0
    -XX:+UseContainerSupport
    -Xlog:gc*:file=/tmp/gc.log:time,uptime,level,tags:filecount=5,filesize=10m

  application.yml: |
    spring:
      datasource:
        url: jdbc:postgresql://tax-db.cluster-xxx.ap-northeast-2.rds.amazonaws.com:5432/taxdb
        hikari:
          maximum-pool-size: 20
          minimum-idle: 5
          connection-timeout: 3000
          validation-timeout: 1000

      jpa:
        open-in-view: false
        properties:
          hibernate:
            default_batch_fetch_size: 100

      redis:
        host: tax-redis.xxx.ng.0001.apn2.cache.amazonaws.com
        port: 6379
        timeout: 1000ms

      kafka:
        bootstrap-servers: b-1.tax-kafka.xxx.kafka.ap-northeast-2.amazonaws.com:9092
        consumer:
          group-id: tax-api-group
          auto-offset-reset: earliest

    server:
      shutdown: graceful
      tomcat:
        accept-count: 100
        max-connections: 8192
        threads:
          max: 200
          min-spare: 20

    management:
      endpoints:
        web:
          exposure:
            include: health,info,prometheus,metrics
      endpoint:
        health:
          probes:
            enabled: true
          show-details: always
          group:
            readiness:
              include: db,redis
            liveness:
              include: ping
```

---

## 2.5 Secret으로 DB 비밀번호 관리

```bash
# 방법 1: kubectl로 Secret 생성 (권장 - YAML에 평문을 남기지 않음)
kubectl create secret generic tax-db-secret \
  --from-literal=username=tax_user \
  --from-literal=password='S3cur3P@ssw0rd!' \
  -n tax-production
```

```yaml
# 방법 2: YAML 정의 (CI/CD에서 동적 생성 시)
apiVersion: v1
kind: Secret
metadata:
  name: tax-db-secret
  namespace: tax-production
type: Opaque
stringData:
  username: tax_user
  password: "S3cur3P@ssw0rd!"
```

**AWS Secrets Manager 연동 (External Secrets Operator):**

```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: tax-db-external-secret
  namespace: tax-production
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secrets-manager
    kind: ClusterSecretStore
  target:
    name: tax-db-secret
    creationPolicy: Owner
  data:
    - secretKey: username
      remoteRef:
        key: tax-service/production/db
        property: username
    - secretKey: password
      remoteRef:
        key: tax-service/production/db
        property: password
```

---

## 2.6 Health Probe 설정

Spring Boot Actuator와 연동한 3단계 헬스 체크를 구성한다.

```
┌─────────────────────────────────────────────────────────┐
│                  Pod Lifecycle Probes                     │
│                                                         │
│  Pod 시작                                                │
│     │                                                   │
│     ▼                                                   │
│  ┌──────────────┐    실패 시: 계속 재시도               │
│  │ Startup      │    (failureThreshold 초과 시 재시작)  │
│  │ Probe        │    성공 시: ▼                          │
│  └──────┬───────┘                                       │
│         │ 성공                                           │
│         ▼                                               │
│  ┌──────────────┐    ┌──────────────┐                   │
│  │ Liveness     │    │ Readiness    │ ← 동시에 시작     │
│  │ Probe        │    │ Probe        │                   │
│  │              │    │              │                   │
│  │ 실패 시:     │    │ 실패 시:     │                   │
│  │ Pod 재시작   │    │ 트래픽 제외  │                   │
│  └──────────────┘    └──────────────┘                   │
└─────────────────────────────────────────────────────────┘
```

**Spring Boot Actuator 설정:**

```java
// build.gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    runtimeOnly 'io.micrometer:micrometer-registry-prometheus'
}
```

```yaml
# application.yml
management:
  endpoint:
    health:
      probes:
        enabled: true       # liveness, readiness 엔드포인트 활성화
      show-details: always
      group:
        liveness:
          include: ping     # /actuator/health/liveness
        readiness:
          include: db, redis # /actuator/health/readiness
  endpoints:
    web:
      exposure:
        include: health, info, prometheus
```

**K8s Probe YAML:**

```yaml
# Startup Probe: 앱이 완전히 뜰 때까지 기다림
startupProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 10     # 첫 체크까지 대기
  periodSeconds: 5            # 5초마다 체크
  failureThreshold: 30        # 최대 150초(5*30) 대기
  # Spring Boot는 초기화 시간이 길 수 있으므로 넉넉히 설정

# Liveness Probe: 앱이 살아있는지 확인
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  periodSeconds: 10
  failureThreshold: 3         # 3회 연속 실패 시 재시작
  timeoutSeconds: 3

# Readiness Probe: 트래픽을 받을 수 있는지 확인
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  periodSeconds: 5
  failureThreshold: 3         # 3회 연속 실패 시 트래픽 제외
  timeoutSeconds: 3
```

---

## 2.7 Graceful Shutdown

배포 시 진행 중인 요청을 안전하게 처리하고 종료하는 설정이다.

```yaml
# application.yml
server:
  shutdown: graceful          # Graceful Shutdown 활성화

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s   # 최대 30초 대기
```

```yaml
# Deployment manifest
spec:
  template:
    spec:
      terminationGracePeriodSeconds: 60   # K8s가 Pod에게 주는 종료 시간
      containers:
        - name: tax-api
          lifecycle:
            preStop:
              exec:
                command: ["sh", "-c", "sleep 5"]  # Service에서 제외될 시간 확보
```

**Graceful Shutdown 흐름:**

```
1. kubectl delete pod / 롤링 업데이트 시작
   │
2. Pod가 Terminating 상태로 전환
   │
3. preStop hook 실행 (sleep 5)
   │    → 이 사이에 Service endpoints에서 Pod IP 제거됨
   │    → 새 트래픽이 이 Pod로 오지 않음
   │
4. SIGTERM 시그널 전송
   │    → Spring Boot Graceful Shutdown 시작
   │    → 새 요청 거부, 기존 요청 처리 완료 대기
   │
5. 처리 완료 또는 timeout-per-shutdown-phase 경과
   │
6. terminationGracePeriodSeconds 내 종료되지 않으면 SIGKILL
```

---

# 3. Helm Chart

## 3.1 Helm 기본 개념

Helm은 Kubernetes의 패키지 매니저로, 복잡한 K8s 리소스를 하나의 패키지(Chart)로 관리한다.

```
┌─────────────────────────────────────────────┐
│              Helm 핵심 개념                   │
│                                             │
│  Chart                Release               │
│  ┌──────────┐        ┌──────────────┐       │
│  │ K8s 리소스│  ───▶  │ Chart의 실제  │       │
│  │ 템플릿    │ install│ 배포 인스턴스 │       │
│  │ 패키지    │        │              │       │
│  └──────────┘        └──────────────┘       │
│       │                                     │
│  Repository                                  │
│  ┌──────────┐                               │
│  │ Chart    │                               │
│  │ 저장소   │                               │
│  └──────────┘                               │
└─────────────────────────────────────────────┘
```

**Helm 주요 명령어:**

```bash
# Chart 설치
helm install tax-api ./tax-api-chart -n tax-production

# values 파일 지정하여 설치
helm install tax-api ./tax-api-chart -f values-prod.yaml -n tax-production

# 업그레이드
helm upgrade tax-api ./tax-api-chart -f values-prod.yaml -n tax-production

# 설치 또는 업그레이드 (idempotent)
helm upgrade --install tax-api ./tax-api-chart -f values-prod.yaml -n tax-production

# 릴리스 목록
helm list -n tax-production

# 롤백
helm rollback tax-api 1 -n tax-production

# 삭제
helm uninstall tax-api -n tax-production
```

---

## 3.2 Spring Boot용 Helm Chart 구조

```
tax-api-chart/
├── Chart.yaml              # Chart 메타데이터
├── values.yaml             # 기본값 정의
├── values-dev.yaml         # 개발 환경 오버라이드
├── values-staging.yaml     # 스테이징 환경 오버라이드
├── values-prod.yaml        # 프로덕션 환경 오버라이드
└── templates/
    ├── _helpers.tpl         # 공통 템플릿 헬퍼
    ├── deployment.yaml
    ├── service.yaml
    ├── ingress.yaml
    ├── configmap.yaml
    ├── secret.yaml
    ├── hpa.yaml
    └── serviceaccount.yaml
```

**Chart.yaml:**

```yaml
apiVersion: v2
name: tax-api
description: 세무 API 서비스
type: application
version: 1.2.0          # Chart 버전
appVersion: "2.1.0"     # 애플리케이션 버전
```

**templates/deployment.yaml:**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "tax-api.fullname" . }}
  labels:
    {{- include "tax-api.labels" . | nindent 4 }}
spec:
  replicas: {{ .Values.replicaCount }}
  selector:
    matchLabels:
      {{- include "tax-api.selectorLabels" . | nindent 6 }}
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: {{ .Values.strategy.maxSurge }}
      maxUnavailable: {{ .Values.strategy.maxUnavailable }}
  template:
    metadata:
      labels:
        {{- include "tax-api.selectorLabels" . | nindent 8 }}
    spec:
      serviceAccountName: {{ include "tax-api.serviceAccountName" . }}
      terminationGracePeriodSeconds: {{ .Values.terminationGracePeriodSeconds }}
      containers:
        - name: {{ .Chart.Name }}
          image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
          imagePullPolicy: {{ .Values.image.pullPolicy }}
          ports:
            - containerPort: {{ .Values.service.targetPort }}
          resources:
            {{- toYaml .Values.resources | nindent 12 }}
          envFrom:
            - configMapRef:
                name: {{ include "tax-api.fullname" . }}-config
          startupProbe:
            {{- toYaml .Values.startupProbe | nindent 12 }}
          livenessProbe:
            {{- toYaml .Values.livenessProbe | nindent 12 }}
          readinessProbe:
            {{- toYaml .Values.readinessProbe | nindent 12 }}
```

---

## 3.3 values.yaml 활용

```yaml
# values.yaml (기본값)
replicaCount: 2

image:
  repository: 123456789.dkr.ecr.ap-northeast-2.amazonaws.com/tax-api
  tag: "latest"
  pullPolicy: IfNotPresent

service:
  type: ClusterIP
  port: 80
  targetPort: 8080

ingress:
  enabled: true
  className: alb
  host: api.tax-api.example.com

resources:
  requests:
    cpu: "500m"
    memory: "1Gi"
  limits:
    cpu: "1000m"
    memory: "2Gi"

strategy:
  maxSurge: 1
  maxUnavailable: 0

terminationGracePeriodSeconds: 60

autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilization: 70

startupProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
  failureThreshold: 30

livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  periodSeconds: 10
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  periodSeconds: 5
  failureThreshold: 3
```

---

## 3.4 멀티 환경 배포

```yaml
# values-dev.yaml
replicaCount: 1

image:
  tag: "dev-latest"

resources:
  requests:
    cpu: "250m"
    memory: "512Mi"
  limits:
    cpu: "500m"
    memory: "1Gi"

ingress:
  host: dev-api.tax-api.example.com

autoscaling:
  enabled: false
```

```yaml
# values-staging.yaml
replicaCount: 2

image:
  tag: "v2.1.0-rc1"

resources:
  requests:
    cpu: "500m"
    memory: "1Gi"
  limits:
    cpu: "1000m"
    memory: "2Gi"

ingress:
  host: staging-api.tax-api.example.com

autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 5
```

```yaml
# values-prod.yaml
replicaCount: 3

image:
  tag: "v2.1.0"

resources:
  requests:
    cpu: "1000m"
    memory: "2Gi"
  limits:
    cpu: "2000m"
    memory: "4Gi"

ingress:
  host: api.tax-api.example.com

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 20
  targetCPUUtilization: 60
```

**배포 명령어:**

```bash
# 개발 환경
helm upgrade --install tax-api ./tax-api-chart \
  -f values-dev.yaml -n tax-dev

# 스테이징 환경
helm upgrade --install tax-api ./tax-api-chart \
  -f values-staging.yaml -n tax-staging

# 프로덕션 환경
helm upgrade --install tax-api ./tax-api-chart \
  -f values-prod.yaml -n tax-production
```

---

# 4. HPA와 스케일링

## 4.1 HPA 설정

HPA(Horizontal Pod Autoscaler)는 메트릭 기반으로 Pod 수를 자동 조절한다.

```
┌──────────────────────────────────────────────────────┐
│                  HPA 동작 흐름                        │
│                                                      │
│  Metrics Server  ──▶  HPA Controller  ──▶  Deployment│
│  (메트릭 수집)        (판단/결정)          (스케일링) │
│                                                      │
│                                                      │
│  CPU 70% 목표 / 현재 90%                             │
│                                                      │
│  현재 Pod: 3개                                       │
│  필요 Pod: ceil(3 * 90/70) = ceil(3.86) = 4개        │
│  → 1개 추가 생성                                     │
└──────────────────────────────────────────────────────┘
```

---

## 4.2 CPU/메모리 기반 오토스케일링

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: tax-api-hpa
  namespace: tax-production
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: tax-api
  minReplicas: 3
  maxReplicas: 20
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 30   # 스케일 업 안정화 기간
      policies:
        - type: Pods
          value: 4                      # 한번에 최대 4개 추가
          periodSeconds: 60
        - type: Percent
          value: 100                    # 또는 현재의 100% 추가
          periodSeconds: 60
      selectPolicy: Max
    scaleDown:
      stabilizationWindowSeconds: 300  # 스케일 다운은 5분 안정화
      policies:
        - type: Pods
          value: 1                      # 한번에 1개씩만 감소
          periodSeconds: 60
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
```

**HPA 모니터링 명령어:**

```bash
# HPA 상태 확인
kubectl get hpa -n tax-production

# 상세 확인
kubectl describe hpa tax-api-hpa -n tax-production

# 실시간 모니터링
kubectl get hpa -n tax-production -w
```

---

## 4.3 Custom Metrics 기반 스케일링

Prometheus Adapter를 통해 Kafka Consumer Lag 등 커스텀 메트릭 기반 스케일링이 가능하다.

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: tax-consumer-hpa
  namespace: tax-production
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: tax-event-consumer
  minReplicas: 2
  maxReplicas: 15
  metrics:
    # CPU 기반
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    # Kafka Consumer Lag 기반 (Custom Metric)
    - type: Pods
      pods:
        metric:
          name: kafka_consumer_lag
        target:
          type: AverageValue
          averageValue: "1000"    # Pod당 평균 lag 1000 이하 유지
    # 요청 수 기반 (External Metric)
    - type: External
      external:
        metric:
          name: http_requests_per_second
          selector:
            matchLabels:
              service: tax-api
        target:
          type: AverageValue
          averageValue: "500"     # Pod당 500 RPS
```

**Prometheus Adapter 설정 예시:**

```yaml
# prometheus-adapter-config.yaml
rules:
  - seriesQuery: 'kafka_consumergroup_lag{namespace!="",pod!=""}'
    resources:
      overrides:
        namespace: {resource: "namespace"}
        pod: {resource: "pod"}
    name:
      matches: "^(.*)$"
      as: "kafka_consumer_lag"
    metricsQuery: 'sum(kafka_consumergroup_lag{<<.LabelMatchers>>}) by (<<.GroupBy>>)'
```

---

## 4.4 VPA

VPA(Vertical Pod Autoscaler)는 Pod의 CPU/메모리 requests와 limits를 자동으로 조정한다.

```yaml
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: tax-api-vpa
  namespace: tax-production
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: tax-api
  updatePolicy:
    updateMode: "Off"       # Off: 추천만, Auto: 자동 적용
  resourcePolicy:
    containerPolicies:
      - containerName: tax-api
        minAllowed:
          cpu: "250m"
          memory: "512Mi"
        maxAllowed:
          cpu: "4000m"
          memory: "8Gi"
        controlledResources: ["cpu", "memory"]
```

| 모드 | 설명 | 사용 사례 |
|------|------|----------|
| **Off** | 추천값만 제공 (적용 안 함) | 초기 리소스 산정 시 |
| **Initial** | Pod 생성 시에만 적용 | HPA와 병행 사용 시 |
| **Auto** | 기존 Pod도 재시작하여 적용 | VPA 단독 사용 시 |

> **주의**: HPA와 VPA를 동일 메트릭(CPU)에 대해 동시에 사용하면 충돌이 발생한다. HPA는 CPU 기반, VPA는 메모리 기반으로 분리하거나, VPA는 Off 모드로 추천값만 참고하는 것을 권장한다.

---

# 5. 배포 전략

## 5.1 롤링 업데이트

Kubernetes의 기본 배포 전략이다. 새 버전의 Pod를 하나씩 생성하면서 구 버전을 하나씩 제거한다.

```
Rolling Update 과정 (maxSurge=1, maxUnavailable=0)

단계 1: [v1] [v1] [v1]          ← 초기 상태 (3개)
단계 2: [v1] [v1] [v1] [v2]     ← v2 1개 추가 (maxSurge=1)
단계 3: [v1] [v1] [v2] [v2]     ← v1 1개 제거, v2 1개 추가
단계 4: [v1] [v2] [v2] [v2]     ← v1 1개 제거, v2 1개 추가
단계 5: [v2] [v2] [v2]          ← 완료
```

```yaml
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1           # 최대 추가 Pod 수 (절대값 또는 %)
      maxUnavailable: 0     # 최대 비가용 Pod 수 (0 = 무중단)
```

---

## 5.2 Blue/Green 배포

기존 버전(Blue)과 새 버전(Green)을 동시에 운영하다가 트래픽을 한번에 전환하는 방식이다.

```
┌─────────────────────────────────────────────────────┐
│              Blue/Green 배포                          │
│                                                     │
│  단계 1: Blue 운영 중                                │
│                                                     │
│  Service ──▶ [Blue v1] [Blue v1] [Blue v1]          │
│              (selector: version=blue)                │
│                                                     │
│  단계 2: Green 배포 완료 (트래픽 없음)               │
│                                                     │
│  Service ──▶ [Blue v1] [Blue v1] [Blue v1]          │
│              [Green v2] [Green v2] [Green v2]        │
│              (아직 트래픽은 Blue로)                   │
│                                                     │
│  단계 3: 트래픽 전환                                 │
│                                                     │
│  Service ──▶ [Green v2] [Green v2] [Green v2]        │
│              (selector: version=green)               │
│              [Blue v1] 대기 (롤백 대비)              │
│                                                     │
│  단계 4: Blue 제거                                   │
│                                                     │
│  Service ──▶ [Green v2] [Green v2] [Green v2]        │
└─────────────────────────────────────────────────────┘
```

```yaml
# Blue Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: tax-api-blue
spec:
  replicas: 3
  selector:
    matchLabels:
      app: tax-api
      version: blue
  template:
    metadata:
      labels:
        app: tax-api
        version: blue
    spec:
      containers:
        - name: tax-api
          image: tax-service/tax-api:1.0.0

---
# Green Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: tax-api-green
spec:
  replicas: 3
  selector:
    matchLabels:
      app: tax-api
      version: green
  template:
    metadata:
      labels:
        app: tax-api
        version: green
    spec:
      containers:
        - name: tax-api
          image: tax-service/tax-api:2.0.0

---
# Service (트래픽 전환 시 selector 변경)
apiVersion: v1
kind: Service
metadata:
  name: tax-api-svc
spec:
  selector:
    app: tax-api
    version: blue      # green으로 변경하면 트래픽 전환
  ports:
    - port: 80
      targetPort: 8080
```

**트래픽 전환:**

```bash
# Blue → Green 전환
kubectl patch service tax-api-svc -n tax-production \
  -p '{"spec":{"selector":{"version":"green"}}}'

# 문제 발생 시 즉시 롤백
kubectl patch service tax-api-svc -n tax-production \
  -p '{"spec":{"selector":{"version":"blue"}}}'
```

---

## 5.3 카나리 배포

새 버전을 소수의 Pod에만 배포하여 트래픽 일부를 새 버전으로 보내 검증한 후 점진적으로 확대하는 전략이다.

```
┌─────────────────────────────────────────────────────┐
│              Canary 배포                              │
│                                                     │
│  단계 1: Canary 10%                                  │
│  Service ──▶ [v1] [v1] [v1] [v1] [v1]              │
│              [v1] [v1] [v1] [v1] [v2]  ← Canary    │
│              (90% v1 / 10% v2)                      │
│                                                     │
│  단계 2: Canary 30%                                  │
│  Service ──▶ [v1] [v1] [v1] [v1] [v1]              │
│              [v1] [v1] [v2] [v2] [v2]              │
│              (70% v1 / 30% v2)                      │
│                                                     │
│  단계 3: 검증 완료, 전체 전환                        │
│  Service ──▶ [v2] [v2] [v2] [v2] [v2]              │
│              [v2] [v2] [v2] [v2] [v2]              │
│              (100% v2)                              │
└─────────────────────────────────────────────────────┘
```

**Kubernetes 네이티브 카나리 배포:**

```yaml
# Stable Deployment (v1)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: tax-api-stable
spec:
  replicas: 9                    # 전체의 90%
  selector:
    matchLabels:
      app: tax-api
      track: stable
  template:
    metadata:
      labels:
        app: tax-api
        track: stable
    spec:
      containers:
        - name: tax-api
          image: tax-service/tax-api:1.0.0

---
# Canary Deployment (v2)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: tax-api-canary
spec:
  replicas: 1                    # 전체의 10%
  selector:
    matchLabels:
      app: tax-api
      track: canary
  template:
    metadata:
      labels:
        app: tax-api
        track: canary
    spec:
      containers:
        - name: tax-api
          image: tax-service/tax-api:2.0.0

---
# Service (app: tax-api 라벨만 매칭하여 양쪽 모두에 트래픽 전달)
apiVersion: v1
kind: Service
metadata:
  name: tax-api-svc
spec:
  selector:
    app: tax-api             # track 라벨 없이 app만 매칭
  ports:
    - port: 80
      targetPort: 8080
```

| 배포 전략 | 장점 | 단점 | 적합한 상황 |
|----------|------|------|------------|
| **롤링 업데이트** | 간단, K8s 기본 지원 | v1/v2 공존 기간 존재 | 일반적인 배포 |
| **Blue/Green** | 즉각 전환/롤백, 버전 혼재 없음 | 2배 리소스 필요 | 중요 서비스, 빠른 롤백 필수 |
| **카나리** | 위험 최소화, 점진적 검증 | 복잡한 설정, 모니터링 필수 | 대규모 서비스, 신규 기능 |

---

# 6. AWS EKS

## 6.1 EKS 클러스터 구성

```
┌──────────────────────────────────────────────────────────┐
│                      AWS EKS Architecture                 │
│                                                          │
│  ┌─────────── VPC (10.0.0.0/16) ──────────────────────┐ │
│  │                                                     │ │
│  │  ┌── AZ-a ────────┐    ┌── AZ-b ────────┐         │ │
│  │  │                 │    │                 │         │ │
│  │  │ Public Subnet   │    │ Public Subnet   │         │ │
│  │  │ ┌─────────────┐│    │ ┌─────────────┐│         │ │
│  │  │ │  NAT GW     ││    │ │  NAT GW     ││         │ │
│  │  │ │  ALB        ││    │ │  ALB        ││         │ │
│  │  │ └─────────────┘│    │ └─────────────┘│         │ │
│  │  │                 │    │                 │         │ │
│  │  │ Private Subnet  │    │ Private Subnet  │         │ │
│  │  │ ┌─────────────┐│    │ ┌─────────────┐│         │ │
│  │  │ │ Worker Nodes││    │ │ Worker Nodes││         │ │
│  │  │ │ (Pod 실행)  ││    │ │ (Pod 실행)  ││         │ │
│  │  │ └─────────────┘│    │ └─────────────┘│         │ │
│  │  └─────────────────┘    └─────────────────┘         │ │
│  │                                                     │ │
│  │          ┌───────────────────────┐                  │ │
│  │          │   EKS Control Plane   │ ← AWS 관리      │ │
│  │          │   (API Server, etcd)  │                  │ │
│  │          └───────────────────────┘                  │ │
│  └─────────────────────────────────────────────────────┘ │
│                                                          │
│  ┌─── 외부 서비스 ───────────────────────────────────┐   │
│  │  RDS (PostgreSQL)  │  ElastiCache (Redis)         │   │
│  │  MSK (Kafka)       │  S3 (파일 저장)              │   │
│  └───────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────┘
```

**eksctl로 클러스터 생성:**

```yaml
# cluster-config.yaml
apiVersion: eksctl.io/v1alpha5
kind: ClusterConfig

metadata:
  name: tax-service-cluster
  region: ap-northeast-2
  version: "1.29"

vpc:
  cidr: 10.0.0.0/16
  nat:
    gateway: HighlyAvailable

managedNodeGroups:
  - name: tax-api-nodes
    instanceType: m6i.xlarge       # 4 vCPU, 16GB
    desiredCapacity: 3
    minSize: 2
    maxSize: 10
    privateNetworking: true
    labels:
      role: api
    tags:
      team: tax-service
    iam:
      withAddonPolicies:
        albIngress: true
        cloudWatch: true
        ebs: true

  - name: tax-batch-nodes
    instanceType: c6i.2xlarge      # 8 vCPU, 16GB (CPU 최적화)
    desiredCapacity: 2
    minSize: 0
    maxSize: 5
    privateNetworking: true
    labels:
      role: batch
    taints:
      - key: dedicated
        value: batch
        effect: NoSchedule

addons:
  - name: vpc-cni
    version: latest
  - name: coredns
    version: latest
  - name: kube-proxy
    version: latest
  - name: aws-ebs-csi-driver
    version: latest
```

```bash
# 클러스터 생성
eksctl create cluster -f cluster-config.yaml

# kubeconfig 업데이트
aws eks update-kubeconfig --name tax-service-cluster --region ap-northeast-2
```

---

## 6.2 IRSA

IRSA(IAM Roles for Service Accounts)는 K8s ServiceAccount에 AWS IAM Role을 바인딩하여 Pod 단위로 AWS 권한을 제어한다.

```
┌─────────────────────────────────────────────────┐
│                IRSA 동작 원리                     │
│                                                 │
│  Pod                                            │
│  ┌──────────┐    ServiceAccount    IAM Role     │
│  │ tax-api  │──▶ tax-api-sa  ──▶  TaxApiRole   │
│  └──────────┘                                   │
│       │                              │          │
│       │    AWS STS                   │          │
│       └────(AssumeRole)──────────────┘          │
│                    │                            │
│                    ▼                            │
│           S3, SQS, Secrets Manager 접근         │
└─────────────────────────────────────────────────┘
```

```bash
# IRSA 설정
eksctl create iamserviceaccount \
  --name tax-api-sa \
  --namespace tax-production \
  --cluster tax-service-cluster \
  --attach-policy-arn arn:aws:iam::123456789:policy/TaxApiPolicy \
  --approve
```

```json
// IAM Policy (TaxApiPolicy)
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Resource": "arn:aws:s3:::tax-documents-bucket/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": "arn:aws:secretsmanager:ap-northeast-2:123456789:secret:tax-service/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "sqs:SendMessage",
        "sqs:ReceiveMessage",
        "sqs:DeleteMessage"
      ],
      "Resource": "arn:aws:sqs:ap-northeast-2:123456789:tax-filing-queue"
    }
  ]
}
```

```yaml
# Deployment에서 ServiceAccount 지정
spec:
  template:
    spec:
      serviceAccountName: tax-api-sa    # IRSA가 연결된 SA
      containers:
        - name: tax-api
          image: tax-service/tax-api:2.1.0
          # 별도의 AWS credential 설정 불필요
          # SDK가 자동으로 IRSA를 통해 인증
```

---

## 6.3 ALB Ingress Controller

AWS ALB(Application Load Balancer)를 K8s Ingress로 관리하는 컨트롤러다.

```bash
# AWS Load Balancer Controller 설치 (Helm)
helm repo add eks https://aws.github.io/eks-charts
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=tax-service-cluster \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller
```

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: tax-api-ingress
  namespace: tax-production
  annotations:
    # ALB 설정
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/subnets: subnet-aaa,subnet-bbb

    # SSL/TLS
    alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:ap-northeast-2:123456789:certificate/xxx
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTPS":443}]'
    alb.ingress.kubernetes.io/ssl-redirect: "443"

    # 헬스 체크
    alb.ingress.kubernetes.io/healthcheck-path: /actuator/health
    alb.ingress.kubernetes.io/healthcheck-interval-seconds: "15"
    alb.ingress.kubernetes.io/healthy-threshold-count: "2"
    alb.ingress.kubernetes.io/unhealthy-threshold-count: "3"

    # WAF 연동
    alb.ingress.kubernetes.io/wafv2-acl-arn: arn:aws:wafv2:ap-northeast-2:123456789:regional/webacl/tax-api-waf/xxx

    # 접근 로깅
    alb.ingress.kubernetes.io/load-balancer-attributes: >-
      access_logs.s3.enabled=true,
      access_logs.s3.bucket=tax-alb-logs,
      idle_timeout.timeout_seconds=60
spec:
  ingressClassName: alb
  rules:
    - host: api.tax-api.example.com
      http:
        paths:
          - path: /api/tax
            pathType: Prefix
            backend:
              service:
                name: tax-api-svc
                port:
                  number: 80
          - path: /api/filing
            pathType: Prefix
            backend:
              service:
                name: filing-api-svc
                port:
                  number: 80
```

---

## 6.4 EKS + RDS, ElastiCache 연동

```yaml
# ConfigMap으로 연결 정보 관리
apiVersion: v1
kind: ConfigMap
metadata:
  name: tax-api-aws-config
  namespace: tax-production
data:
  application-aws.yml: |
    spring:
      datasource:
        url: jdbc:postgresql://tax-db.cluster-xxx.ap-northeast-2.rds.amazonaws.com:5432/taxdb
        username: ${DB_USERNAME}
        password: ${DB_PASSWORD}
        hikari:
          maximum-pool-size: 20
          minimum-idle: 5

      data:
        redis:
          host: tax-redis.xxx.ng.0001.apn2.cache.amazonaws.com
          port: 6379
          ssl:
            enabled: true
          timeout: 1000ms
          lettuce:
            pool:
              max-active: 16
              max-idle: 8
              min-idle: 4

      kafka:
        bootstrap-servers: >-
          b-1.tax-msk.xxx.kafka.ap-northeast-2.amazonaws.com:9096,
          b-2.tax-msk.xxx.kafka.ap-northeast-2.amazonaws.com:9096
        properties:
          security.protocol: SASL_SSL
          sasl.mechanism: AWS_MSK_IAM
          sasl.jaas.config: >-
            software.amazon.msk.auth.iam.IAMLoginModule required;
          sasl.client.callback.handler.class: >-
            software.amazon.msk.auth.iam.IAMClientCallbackHandler
```

**네트워크 연결 구성:**

```
┌─── VPC (10.0.0.0/16) ──────────────────────────────────┐
│                                                         │
│  ┌── Private Subnet ──────┐  ┌── Private Subnet ─────┐ │
│  │  EKS Worker Nodes      │  │  RDS (Multi-AZ)       │ │
│  │  ┌─────┐ ┌─────┐      │  │  ┌────────────────┐   │ │
│  │  │Pod A│ │Pod B│──────────▶│  PostgreSQL     │   │ │
│  │  └─────┘ └─────┘      │  │  │  Primary       │   │ │
│  │                        │  │  └────────────────┘   │ │
│  │  Security Group:       │  │  Security Group:      │ │
│  │  eks-node-sg           │  │  rds-sg               │ │
│  └────────────────────────┘  │  (inbound: 5432       │ │
│                              │   from eks-node-sg)   │ │
│  ┌── Private Subnet ─────┐  └───────────────────────┘ │
│  │  ElastiCache           │                            │
│  │  ┌────────────────┐   │                            │
│  │  │  Redis Cluster │   │                            │
│  │  └────────────────┘   │                            │
│  │  Security Group:      │                            │
│  │  redis-sg             │                            │
│  │  (inbound: 6379       │                            │
│  │   from eks-node-sg)   │                            │
│  └───────────────────────┘                            │
└─────────────────────────────────────────────────────────┘
```

---

# 7. 세무 도메인 적용

## 7.1 종소세 시즌 트래픽 스케일링

5월 종합소득세 신고 시즌(5/1~5/31)에는 평소 대비 10~20배 트래픽이 발생한다. 이에 대한 K8s 기반 스케일링 전략이다.

```
┌──────────────────────────────────────────────────────┐
│           5월 종소세 시즌 트래픽 패턴                  │
│                                                      │
│  트래픽                                              │
│    ▲                                    ┌──┐         │
│    │                               ┌────┤  │         │
│    │                          ┌────┤    │  │         │
│    │                     ┌────┤    │    │  │         │
│    │                ┌────┤    │    │    │  │         │
│    │           ┌────┤    │    │    │    │  │         │
│  ──┼───────────┤    │    │    │    │    │  ├──       │
│    │    평소   │4/25│5/1 │5/10│5/20│5/25│31│         │
│    └───────────┴────┴────┴────┴────┴────┴──┴──▶ 시간 │
│                                                      │
│   Phase 1     Phase 2        Phase 3      Phase 4    │
│   사전 준비   본격 시작      피크         안정화      │
└──────────────────────────────────────────────────────┘
```

**Phase별 스케일링 전략:**

```yaml
# Phase 1: 사전 준비 (4월 25일~)
# Node Group 미리 확장
apiVersion: apps/v1
kind: Deployment
metadata:
  name: tax-filing-api
  annotations:
    phase: "pre-season"
spec:
  replicas: 10      # 평소 3개 → 10개로 사전 확장

---
# Phase 2~3: 피크 시즌 HPA
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: tax-filing-hpa-peak
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: tax-filing-api
  minReplicas: 10                # 최소 10개 유지
  maxReplicas: 50                # 최대 50개까지 확장
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 0    # 즉시 스케일 업
      policies:
        - type: Pods
          value: 10                     # 한번에 10개 추가 가능
          periodSeconds: 60
    scaleDown:
      stabilizationWindowSeconds: 600  # 10분간 안정 후 스케일 다운
      policies:
        - type: Pods
          value: 2
          periodSeconds: 120
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 50       # 피크 시즌엔 여유 있게 50%
```

**Cluster Autoscaler / Karpenter 설정:**

```yaml
# Karpenter NodePool (EKS 권장)
apiVersion: karpenter.sh/v1beta1
kind: NodePool
metadata:
  name: tax-peak-season
spec:
  template:
    spec:
      requirements:
        - key: "karpenter.sh/capacity-type"
          operator: In
          values: ["on-demand"]        # 피크 시즌은 On-Demand
        - key: "node.kubernetes.io/instance-type"
          operator: In
          values: ["m6i.xlarge", "m6i.2xlarge", "m5.xlarge", "m5.2xlarge"]
        - key: "topology.kubernetes.io/zone"
          operator: In
          values: ["ap-northeast-2a", "ap-northeast-2c"]
      nodeClassRef:
        name: default
  limits:
    cpu: "200"                          # 총 200 vCPU까지
    memory: "800Gi"
  disruption:
    consolidationPolicy: WhenEmpty
    expireAfter: 720h                   # 30일 후 만료
```

---

## 7.2 배치 Job의 CronJob 전환

Spring Batch Job을 K8s CronJob으로 전환하여 스케줄러 의존성을 제거한다.

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: daily-tax-calculation
  namespace: tax-production
spec:
  schedule: "0 2 * * *"            # 매일 새벽 2시
  concurrencyPolicy: Forbid         # 이전 작업이 실행 중이면 건너뜀
  successfulJobsHistoryLimit: 3
  failedJobsHistoryLimit: 3
  startingDeadlineSeconds: 600      # 스케줄 시간 후 10분 내 시작
  jobTemplate:
    spec:
      backoffLimit: 2               # 최대 2번 재시도
      activeDeadlineSeconds: 7200   # 최대 2시간 실행
      template:
        spec:
          serviceAccountName: tax-batch-sa
          nodeSelector:
            role: batch              # 배치 전용 노드에서 실행
          tolerations:
            - key: dedicated
              value: batch
              effect: NoSchedule
          restartPolicy: OnFailure
          containers:
            - name: tax-batch
              image: 123456789.dkr.ecr.ap-northeast-2.amazonaws.com/tax-batch:v1.5.0
              args: ["--spring.batch.job.name=dailyTaxCalculationJob"]
              resources:
                requests:
                  cpu: "2000m"
                  memory: "4Gi"
                limits:
                  cpu: "4000m"
                  memory: "8Gi"
              env:
                - name: SPRING_PROFILES_ACTIVE
                  value: "production,batch"
                - name: JAVA_OPTS
                  value: >-
                    -XX:+UseG1GC
                    -XX:MaxRAMPercentage=75.0
                    -XX:+UseContainerSupport
              envFrom:
                - configMapRef:
                    name: tax-batch-config
                - secretRef:
                    name: tax-db-secret
```

**주요 CronJob 스케줄:**

| CronJob | 스케줄 | 설명 |
|---------|--------|------|
| `daily-tax-calculation` | `0 2 * * *` | 일일 세금 계산 집계 |
| `monthly-filing-report` | `0 3 1 * *` | 월별 신고 현황 리포트 |
| `quarterly-vat-prep` | `0 4 1 1,4,7,10 *` | 분기별 부가세 사전 계산 |
| `yearly-income-tax-prep` | `0 0 1 4 *` | 연간 종소세 사전 준비 |
| `data-cleanup` | `0 5 * * 0` | 주간 임시 데이터 정리 |

---

## 7.3 마이크로서비스별 배포 전략

```
┌─────────────────────────────────────────────────────┐
│         세무 플랫폼 마이크로서비스 배포 전략                │
│                                                     │
│  서비스              배포 전략       근거            │
│  ─────────────────   ──────────     ──────────      │
│  tax-filing-api      카나리         핵심 신고 로직, │
│  (세무 신고)                        오류 시 큰 영향 │
│                                                     │
│  tax-calculation     Blue/Green     정확성 최우선,  │
│  (세액 계산)                        즉각 롤백 필요  │
│                                                     │
│  user-api            롤링 업데이트  비교적 안정,    │
│  (사용자 관리)                      표준 배포 충분  │
│                                                     │
│  notification-svc    롤링 업데이트  비핵심 서비스   │
│  (알림)                                             │
│                                                     │
│  ai-review-api       카나리         AI 모델 변경 시 │
│  (AI 검토)                          점진적 검증     │
│                                                     │
│  batch-service       CronJob        스케줄 기반     │
│  (배치 처리)                        독립 실행       │
└─────────────────────────────────────────────────────┘
```

---

# 8. 운영 및 트러블슈팅

## 8.1 kubectl 필수 명령어

### 조회

```bash
# Pod 조회
kubectl get pods -n tax-production
kubectl get pods -n tax-production -o wide          # IP, Node 정보 포함
kubectl get pods -n tax-production -l app=tax-api   # 라벨 필터링
kubectl get pods --all-namespaces                   # 전체 네임스페이스

# 리소스 상세 조회
kubectl describe pod tax-api-xxx -n tax-production
kubectl describe deployment tax-api -n tax-production
kubectl describe node ip-10-0-1-123.ap-northeast-2.compute.internal

# 리소스 사용량
kubectl top pods -n tax-production
kubectl top nodes
kubectl top pods -n tax-production --sort-by=cpu
```

### 로그

```bash
# Pod 로그
kubectl logs tax-api-xxx -n tax-production
kubectl logs tax-api-xxx -n tax-production --tail=100
kubectl logs tax-api-xxx -n tax-production -f          # 실시간 팔로우
kubectl logs tax-api-xxx -n tax-production --previous   # 이전 컨테이너 로그
kubectl logs tax-api-xxx -n tax-production -c log-agent # 특정 컨테이너

# 라벨로 여러 Pod 로그
kubectl logs -l app=tax-api -n tax-production --tail=50
```

### 디버깅

```bash
# Pod 내부 접속
kubectl exec -it tax-api-xxx -n tax-production -- /bin/sh

# 임시 디버그 컨테이너 (distroless 이미지용)
kubectl debug -it tax-api-xxx -n tax-production --image=busybox --target=tax-api

# DNS 확인
kubectl run dns-test --image=busybox:1.36 --rm -it --restart=Never -- \
  nslookup tax-api-svc.tax-production.svc.cluster.local

# 네트워크 연결 확인
kubectl run net-test --image=busybox:1.36 --rm -it --restart=Never -- \
  wget -qO- http://tax-api-svc.tax-production:80/actuator/health

# Port forwarding
kubectl port-forward svc/tax-api-svc 8080:80 -n tax-production
```

### 관리

```bash
# 롤링 재시작 (설정 변경 반영)
kubectl rollout restart deployment/tax-api -n tax-production

# 강제 Pod 삭제
kubectl delete pod tax-api-xxx -n tax-production --grace-period=0 --force

# 리소스 스케일링
kubectl scale deployment tax-api --replicas=5 -n tax-production

# 노드 유지보수 (drain)
kubectl drain node-xxx --ignore-daemonsets --delete-emptydir-data
kubectl uncordon node-xxx
```

---

## 8.2 로그 수집

Fluent Bit을 DaemonSet으로 배포하여 모든 노드의 로그를 수집한다.

```yaml
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: fluent-bit
  namespace: monitoring
spec:
  selector:
    matchLabels:
      app: fluent-bit
  template:
    metadata:
      labels:
        app: fluent-bit
    spec:
      serviceAccountName: fluent-bit-sa
      tolerations:
        - operator: Exists    # 모든 노드에 배포
      containers:
        - name: fluent-bit
          image: fluent/fluent-bit:2.2
          volumeMounts:
            - name: varlog
              mountPath: /var/log
            - name: varlibdockercontainers
              mountPath: /var/lib/docker/containers
              readOnly: true
            - name: config
              mountPath: /fluent-bit/etc/
          resources:
            requests:
              cpu: "100m"
              memory: "128Mi"
            limits:
              cpu: "200m"
              memory: "256Mi"
      volumes:
        - name: varlog
          hostPath:
            path: /var/log
        - name: varlibdockercontainers
          hostPath:
            path: /var/lib/docker/containers
        - name: config
          configMap:
            name: fluent-bit-config
```

**Fluent Bit 설정 (CloudWatch로 전송):**

```ini
# fluent-bit.conf
[SERVICE]
    Flush         5
    Log_Level     info
    Parsers_File  parsers.conf

[INPUT]
    Name              tail
    Tag               kube.*
    Path              /var/log/containers/*.log
    Parser            docker
    Mem_Buf_Limit     50MB
    Skip_Long_Lines   On

[FILTER]
    Name                kubernetes
    Match               kube.*
    Kube_URL            https://kubernetes.default.svc:443
    Kube_Tag_Prefix     kube.var.log.containers.
    Merge_Log           On
    K8S-Logging.Parser  On

[OUTPUT]
    Name                cloudwatch_logs
    Match               kube.*
    region              ap-northeast-2
    log_group_name      /eks/tax-service-cluster
    log_stream_prefix   fluentbit-
    auto_create_group   true
```

---

## 8.3 Pod 디버깅

### Pod가 시작되지 않을 때

```bash
# 1. Pod 상태 확인
kubectl get pod tax-api-xxx -n tax-production

# 2. 이벤트 확인
kubectl describe pod tax-api-xxx -n tax-production
# Events 섹션에서 원인 파악

# 3. 이전 컨테이너 로그 확인
kubectl logs tax-api-xxx -n tax-production --previous
```

**자주 발생하는 에러와 해결:**

| 상태 | 원인 | 해결 방법 |
|------|------|----------|
| **ImagePullBackOff** | 이미지 pull 실패 | 이미지 이름/태그 확인, ECR 인증 확인 |
| **CrashLoopBackOff** | 컨테이너 반복 비정상 종료 | `kubectl logs --previous`로 에러 확인 |
| **Pending** | 스케줄링 불가 | 리소스 부족, nodeSelector/taint 확인 |
| **OOMKilled** | 메모리 초과 | memory limit 증가, 메모리 누수 확인 |
| **CreateContainerConfigError** | ConfigMap/Secret 없음 | 참조하는 CM/Secret 존재 여부 확인 |
| **Evicted** | 노드 리소스 부족 | 노드 리소스 확인, Pod 리소스 조정 |

### OOMKilled 디버깅

```bash
# 1. OOMKilled 확인
kubectl describe pod tax-api-xxx -n tax-production | grep -A 5 "Last State"
# Last State: Terminated
#   Reason: OOMKilled
#   Exit Code: 137

# 2. 메모리 사용량 모니터링
kubectl top pod tax-api-xxx -n tax-production

# 3. JVM 힙 덤프 (Java 앱)
kubectl exec tax-api-xxx -n tax-production -- \
  jcmd 1 GC.heap_dump /tmp/heapdump.hprof

# 4. 파일 복사
kubectl cp tax-production/tax-api-xxx:/tmp/heapdump.hprof ./heapdump.hprof
```

### 네트워크 문제 디버깅

```bash
# 1. Service endpoints 확인 (Pod가 Service에 등록되었는지)
kubectl get endpoints tax-api-svc -n tax-production

# 2. DNS 해석 확인
kubectl run dns-debug --image=busybox:1.36 --rm -it --restart=Never -- \
  nslookup tax-api-svc.tax-production.svc.cluster.local

# 3. Pod 간 통신 확인
kubectl exec tax-api-xxx -n tax-production -- \
  wget -qO- --timeout=5 http://user-api-svc.tax-production:80/actuator/health

# 4. kube-proxy 상태 확인
kubectl logs -n kube-system -l k8s-app=kube-proxy --tail=50
```

---

## 핵심 정리

| 영역 | 핵심 포인트 |
|------|------------|
| **Pod** | K8s 최소 배포 단위. 직접 생성하지 말고 Deployment를 사용 |
| **Deployment** | Stateless 앱의 표준 배포 단위. 롤링 업데이트, 롤백 기본 지원 |
| **StatefulSet** | DB, Kafka 등 상태 보존이 필요한 앱용. 순차 배포, 고정 DNS |
| **Service** | Pod 앞단의 안정적 네트워크 엔드포인트. ClusterIP/NodePort/LoadBalancer |
| **Ingress** | L7 라우팅. 호스트/경로 기반 트래픽 분배. ALB Controller로 AWS 연동 |
| **ConfigMap/Secret** | 설정과 민감 정보 분리. 실 운영은 External Secrets Operator 권장 |
| **HPA** | CPU/메모리/커스텀 메트릭 기반 자동 스케일링 |
| **Helm** | K8s 패키지 매니저. values.yaml로 환경별 배포 관리 |
| **EKS** | AWS 관리형 K8s. IRSA로 Pod 단위 IAM 권한, ALB Controller 연동 |
| **Graceful Shutdown** | preStop hook + Spring Boot graceful shutdown으로 무중단 배포 보장 |
| **Health Probe** | Startup/Liveness/Readiness 3단계. Actuator 연동 필수 |
| **세무 도메인** | 종소세 시즌 사전 스케일링, Karpenter로 노드 자동 확장 |

---

## 면접 대비 핵심 질문

### Q1. Pod, Container, Node의 관계를 설명해주세요.

**A.** Node는 K8s 클러스터의 물리적/가상 서버로 워커 머신이다. Pod는 K8s에서 배포할 수 있는 최소 단위로, 하나 이상의 컨테이너를 포함한다. Container는 애플리케이션이 실행되는 격리된 환경이다. Node 안에 여러 Pod가 있고, Pod 안에 여러 Container가 있을 수 있다. 같은 Pod 내 컨테이너는 네트워크(localhost)와 볼륨을 공유하며, 주로 사이드카 패턴으로 활용한다.

### Q2. Deployment와 StatefulSet의 차이점은?

**A.** Deployment는 Stateless 앱용으로 Pod가 동일하고 교체 가능하다. Pod 이름은 랜덤 해시이며, 병렬로 생성/삭제되고, Pod가 삭제되면 스토리지도 함께 삭제된다. StatefulSet은 Stateful 앱(DB, Kafka)용으로 각 Pod에 고유한 순차적 이름(xxx-0, xxx-1)과 고정 DNS를 부여한다. 순차적으로 생성(0→1→2)되고 역순으로 삭제(2→1→0)되며, Pod가 삭제되어도 PVC는 유지된다.

### Q3. Service의 ClusterIP, NodePort, LoadBalancer 차이는?

**A.** ClusterIP는 기본 타입으로 클러스터 내부에서만 접근 가능한 가상 IP를 제공한다. 서비스 간 내부 통신에 사용한다. NodePort는 ClusterIP에 더해 모든 Node의 특정 포트(30000-32767)로 외부 접근을 허용한다. LoadBalancer는 NodePort에 더해 클라우드 로드밸런서를 자동 프로비저닝하여 외부 트래픽을 분산한다. 실무에서는 보통 Ingress Controller + ClusterIP 조합을 사용한다.

### Q4. Liveness Probe와 Readiness Probe의 차이와 설정 전략은?

**A.** Liveness Probe는 "앱이 살아있는가?"를 판단하며, 실패하면 Pod를 재시작한다. DB 연결 등 외부 의존성을 포함하면 연쇄 재시작이 발생할 수 있으므로 `/actuator/health/liveness`에 ping만 포함하는 것이 좋다. Readiness Probe는 "트래픽을 받을 준비가 되었는가?"를 판단하며, 실패하면 Service endpoints에서 제외하여 트래픽이 오지 않게 한다. DB, Redis 등 외부 의존성을 포함하여 실제 요청 처리 가능 여부를 판단한다. 추가로 Startup Probe를 설정하여 Spring Boot처럼 초기화 시간이 긴 앱의 시작 완료를 기다린다.

### Q5. 무중단 배포를 위한 K8s 설정을 설명해주세요.

**A.** 4가지를 조합한다. (1) Deployment의 `maxUnavailable: 0`으로 항상 최소 Pod 수를 유지한다. (2) Readiness Probe로 새 Pod가 완전히 준비된 후에만 트래픽을 받게 한다. (3) `preStop` hook에 `sleep 5`를 넣어 Service endpoints에서 Pod이 제거될 시간을 확보한다. (4) Spring Boot의 `server.shutdown: graceful`로 진행 중인 요청을 처리 완료한 후 종료한다. `terminationGracePeriodSeconds`는 preStop + graceful shutdown 시간보다 넉넉하게 설정한다.

### Q6. HPA의 동작 원리와 주의할 점은?

**A.** HPA는 Metrics Server에서 수집한 메트릭(CPU, 메모리, 커스텀)을 기반으로 Deployment의 replicas를 자동 조정한다. 계산 공식은 `ceil(현재 Pod수 * 현재 메트릭값/목표 메트릭값)`이다. 주의할 점은: (1) Pod에 반드시 resource requests를 설정해야 CPU 사용률 계산이 가능하다. (2) scaleDown에 stabilizationWindow를 설정하여 급격한 축소(flapping)를 방지한다. (3) VPA와 동일 메트릭에 대해 동시 사용하면 충돌이 발생하므로, HPA는 CPU 기반, VPA는 메모리 기반(또는 Off 모드)으로 분리한다.

### Q7. Helm을 사용하는 이유와 환경별 배포 관리 방법은?

**A.** Helm은 K8s의 패키지 매니저로, 복잡한 여러 K8s manifest를 하나의 Chart로 관리한다. 장점은: (1) 템플릿 기반으로 환경별 차이를 values.yaml 파일로 관리하여 중복 제거 (2) `helm rollback`으로 이전 릴리스로 원클릭 롤백 (3) Chart 버전 관리와 의존성 관리. 환경별 배포는 `values-dev.yaml`, `values-staging.yaml`, `values-prod.yaml`을 각각 만들어 `helm upgrade --install -f values-prod.yaml`처럼 환경별 values 파일을 지정하여 배포한다.

### Q8. 5월 종소세 시즌에 대비한 K8s 스케일링 전략은?

**A.** 4단계로 접근한다. (1) **사전 준비(4월 말)**: minReplicas를 평소의 3배로 상향하고, Karpenter NodePool의 limits를 확장한다. 부하 테스트로 병목을 사전 확인한다. (2) **시즌 시작(5/1~)**: HPA의 targetCPU를 50%로 낮춰 여유있게 스케일 업 하고, scaleUp.stabilizationWindow를 0으로 설정하여 즉시 반응하게 한다. (3) **피크(5/25~31)**: maxReplicas를 최대로 설정하고, DB 커넥션 풀, Redis 커넥션도 동시에 확장한다. (4) **안정화(6월)**: 설정을 평소로 원복한다. 핵심은 Node 레벨(Karpenter/CA)과 Pod 레벨(HPA) 스케일링을 모두 설정하고, DB 등 외부 의존성도 함께 확장하는 것이다.

### Q9. IRSA(IAM Roles for Service Accounts)의 동작 원리와 장점은?

**A.** IRSA는 K8s ServiceAccount에 AWS IAM Role을 연결하는 메커니즘이다. Pod가 생성되면 K8s가 ServiceAccount에 연결된 IAM Role의 임시 자격 증명을 OIDC를 통해 주입한다. 장점은: (1) **최소 권한 원칙**: Pod 단위로 필요한 AWS 권한만 부여 (vs Node 단위 IAM Role) (2) **자격 증명 관리 불필요**: Access Key를 코드나 환경변수에 넣을 필요 없음 (3) **자동 로테이션**: STS 임시 자격 증명이 자동 갱신됨. 예를 들어 세금 계산 서비스에는 S3 읽기만, 신고 서비스에는 S3 쓰기+SQS 접근을 각각 다른 Role로 부여한다.

### Q10. Pod가 CrashLoopBackOff 상태일 때 디버깅 절차는?

**A.** 단계적으로 접근한다. (1) `kubectl describe pod`로 Events 섹션을 확인하여 OOMKilled, 설정 오류 등 원인 파악 (2) `kubectl logs --previous`로 이전 컨테이너 로그를 확인하여 애플리케이션 에러 확인 (3) OOMKilled이면 memory limits 증가 또는 메모리 누수 점검. Java의 경우 `-XX:MaxRAMPercentage=75.0`으로 JVM 힙을 컨테이너 메모리의 75% 이내로 제한 (4) ConfigMap/Secret 참조 오류면 해당 리소스 존재 여부 확인 (5) 앱 자체 문제면 `kubectl debug`로 임시 디버그 컨테이너를 붙여 분석한다.

---

*마지막 업데이트: 2026년 02월*
