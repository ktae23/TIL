# Jenkins Declarative Pipeline 기초

Jenkins는 가장 널리 사용되는 오픈소스 CI/CD 자동화 서버다. Declarative Pipeline은 Jenkins Pipeline의 현대적 작성 방식으로, 구조화된 문법을 통해 빌드/테스트/배포 파이프라인을 코드로 정의한다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### Jenkins 아키텍처

```
┌────────────────────────────────────────────────┐
│              Jenkins Controller                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐     │
│  │ Web UI   │  │ Job      │  │ Plugin   │     │
│  │          │  │ Scheduler│  │ Manager  │     │
│  └──────────┘  └──────────┘  └──────────┘     │
│                     │                           │
│            ┌────────┼────────┐                  │
│            ↓        ↓        ↓                  │
│      ┌─────────┐ ┌─────────┐ ┌─────────┐      │
│      │ Agent 1 │ │ Agent 2 │ │ Agent 3 │      │
│      │(Linux)  │ │(Windows)│ │(Docker) │      │
│      └─────────┘ └─────────┘ └─────────┘      │
└────────────────────────────────────────────────┘
```

- **Controller (Master)**: Jenkins 서버의 메인 프로세스. Job 스케줄링, 플러그인 관리, Web UI 제공
- **Agent (Slave/Node)**: 실제 빌드를 실행하는 워커. 다양한 OS/환경에서 실행 가능
- **Executor**: Agent 내에서 빌드를 실행하는 스레드. Agent당 여러 Executor 가능
- **Pipeline**: 빌드/테스트/배포 과정을 코드로 정의한 것. `Jenkinsfile`에 작성

### Declarative vs Scripted Pipeline

| 특성 | Declarative | Scripted |
|------|------------|---------|
| 문법 | 구조화된 블록 (`pipeline {}`) | Groovy DSL 자유형 |
| 학습 곡선 | 낮음 | 높음 |
| 유연성 | 제한적 (정해진 구조) | 매우 높음 |
| 에러 처리 | `post {}` 블록 | `try-catch-finally` |
| 권장 대상 | 대부분의 경우 | 복잡한 로직 필요 시 |
| 도입 시기 | Pipeline 2.x (2017~) | Pipeline 1.x (2016~) |

## 2. 왜 알아야 하는가 (Why)

### Jenkins가 여전히 중요한 이유

- **시장 점유율**: 기업 환경에서 가장 많이 사용되는 CI/CD 도구
- **Self-hosted**: 보안이 중요한 환경에서 온프레미스 운영 가능
- **플러그인 생태계**: 1,800+ 플러그인으로 거의 모든 도구와 연동
- **유연성**: 어떤 언어, 어떤 빌드 시스템이든 지원
- **레거시 시스템 연동**: 기존 인프라와의 통합이 용이

### GitHub Actions vs Jenkins

| 비교 항목 | GitHub Actions | Jenkins |
|----------|---------------|---------|
| 호스팅 | GitHub 관리형 | 자체 관리 |
| 설정 | 간단 (YAML) | 복잡 (Groovy/YAML) |
| 비용 | 무료 tier + 사용량 과금 | 서버 인프라 비용 |
| 확장성 | Marketplace Actions | Plugin 생태계 |
| 커스터마이징 | 제한적 | 거의 무제한 |
| 보안 | GitHub이 관리 | 자체 관리 필요 |

## 3. 내부 구현 분석 (How)

### Declarative Pipeline 기본 구조

```groovy
// Jenkinsfile
pipeline {
    agent any                          // 실행 환경

    options {
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    environment {                       // 환경 변수
        APP_NAME = 'my-app'
        REGISTRY = 'registry.example.com'
    }

    stages {                           // 파이프라인 스테이지
        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }

        stage('Deploy') {
            steps {
                sh './deploy.sh'
            }
        }
    }

    post {                             // 빌드 후 처리
        always {
            cleanWs()
        }
        success {
            echo 'Build succeeded!'
        }
        failure {
            echo 'Build failed!'
        }
    }
}
```

### Agent 지정

```groovy
pipeline {
    // 방법 1: 아무 Agent에서 실행
    agent any

    // 방법 2: 특정 레이블의 Agent
    agent { label 'linux && docker' }

    // 방법 3: Docker 컨테이너에서 실행
    agent {
        docker {
            image 'maven:3.9-eclipse-temurin-21'
            args '-v /root/.m2:/root/.m2'  // Maven 캐시 마운트
        }
    }

    // 방법 4: Kubernetes Pod에서 실행
    agent {
        kubernetes {
            yaml '''
                apiVersion: v1
                kind: Pod
                spec:
                  containers:
                  - name: maven
                    image: maven:3.9-eclipse-temurin-21
                    command:
                    - sleep
                    args:
                    - infinity
            '''
        }
    }

    // 방법 5: 스테이지별 다른 Agent
    agent none
    stages {
        stage('Build') {
            agent { docker { image 'maven:3.9' } }
            steps { sh 'mvn package' }
        }
        stage('Test') {
            agent { docker { image 'node:20' } }
            steps { sh 'npm test' }
        }
    }
}
```

### Environment와 Credentials

```groovy
pipeline {
    agent any

    environment {
        // 일반 환경 변수
        APP_VERSION = '1.0.0'

        // Jenkins Credentials 참조
        AWS_CREDS = credentials('aws-credentials')
        // → AWS_CREDS_USR (username), AWS_CREDS_PSW (password)

        // Secret text
        API_KEY = credentials('api-key-secret')
        // → API_KEY에 직접 값 할당

        // 파이프라인 내 동적 값
        GIT_COMMIT_SHORT = sh(
            script: 'git rev-parse --short HEAD',
            returnStdout: true
        ).trim()
    }

    stages {
        stage('Deploy') {
            environment {
                // 스테이지 수준 환경 변수 (이 스테이지에서만 유효)
                DEPLOY_ENV = 'production'
            }
            steps {
                sh '''
                    echo "Deploying ${APP_VERSION} (${GIT_COMMIT_SHORT})"
                    echo "Environment: ${DEPLOY_ENV}"
                '''
            }
        }
    }
}
```

### 조건부 실행 (when)

```groovy
pipeline {
    agent any

    stages {
        stage('Deploy to Staging') {
            when {
                branch 'develop'
            }
            steps {
                sh './deploy.sh staging'
            }
        }

        stage('Deploy to Production') {
            when {
                allOf {
                    branch 'main'
                    not { changeRequest() }  // PR이 아닌 경우
                }
            }
            steps {
                sh './deploy.sh production'
            }
        }

        stage('Run E2E') {
            when {
                expression {
                    return env.RUN_E2E == 'true'
                }
            }
            steps {
                sh 'npm run test:e2e'
            }
        }

        stage('Build Docs') {
            when {
                changeset 'docs/**'  // docs 디렉토리 변경 시
            }
            steps {
                sh 'mkdocs build'
            }
        }
    }
}
```

### 병렬 실행 (parallel)

```groovy
pipeline {
    agent any

    stages {
        stage('Parallel Tests') {
            parallel {
                stage('Unit Tests') {
                    agent { docker { image 'maven:3.9' } }
                    steps {
                        sh 'mvn test -pl unit-tests'
                    }
                }
                stage('Integration Tests') {
                    agent { docker { image 'maven:3.9' } }
                    steps {
                        sh 'mvn test -pl integration-tests'
                    }
                }
                stage('Performance Tests') {
                    agent { label 'performance' }
                    steps {
                        sh 'k6 run load-test.js'
                    }
                }
            }
        }
    }
}
```

### Post 블록

```groovy
pipeline {
    agent any

    stages {
        stage('Build') {
            steps { sh 'mvn package' }
        }
    }

    post {
        always {
            // 항상 실행 (성공/실패 무관)
            junit '**/target/surefire-reports/*.xml'    // 테스트 리포트
            archiveArtifacts artifacts: 'target/*.jar'  // 아티팩트 보관
            cleanWs()                                   // 워크스페이스 정리
        }
        success {
            slackSend(
                color: 'good',
                message: "Build #${env.BUILD_NUMBER} succeeded"
            )
        }
        failure {
            slackSend(
                color: 'danger',
                message: "Build #${env.BUILD_NUMBER} failed"
            )
        }
        unstable {
            // 테스트 실패가 있지만 빌드는 성공
            echo 'Some tests failed'
        }
    }
}
```

## 4. 실전 예제

### 예제 1: Spring Boot 완성 파이프라인

```groovy
pipeline {
    agent any

    tools {
        jdk 'jdk-21'
        gradle 'gradle-8'
    }

    environment {
        REGISTRY = 'registry.example.com'
        IMAGE_NAME = 'my-spring-app'
        VERSION = "${env.BUILD_NUMBER}-${env.GIT_COMMIT[0..6]}"
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'gradle clean build -x test'
            }
        }

        stage('Test') {
            parallel {
                stage('Unit Test') {
                    steps {
                        sh 'gradle test'
                    }
                    post {
                        always {
                            junit 'build/test-results/test/*.xml'
                        }
                    }
                }
                stage('Integration Test') {
                    steps {
                        sh 'gradle integrationTest'
                    }
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonar-server') {
                    sh 'gradle sonarqube'
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Docker Build & Push') {
            when { branch 'main' }
            steps {
                script {
                    docker.withRegistry("https://${REGISTRY}", 'registry-credentials') {
                        def app = docker.build("${IMAGE_NAME}:${VERSION}")
                        app.push()
                        app.push('latest')
                    }
                }
            }
        }

        stage('Deploy to Staging') {
            when { branch 'main' }
            steps {
                sh """
                    kubectl set image deployment/my-app \
                        my-app=${REGISTRY}/${IMAGE_NAME}:${VERSION} \
                        -n staging
                """
            }
        }

        stage('Approval') {
            when { branch 'main' }
            steps {
                input message: 'Deploy to production?',
                      ok: 'Deploy',
                      submitter: 'admin,lead-dev'
            }
        }

        stage('Deploy to Production') {
            when { branch 'main' }
            steps {
                sh """
                    kubectl set image deployment/my-app \
                        my-app=${REGISTRY}/${IMAGE_NAME}:${VERSION} \
                        -n production
                """
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            slackSend(
                color: 'good',
                message: ":white_check_mark: *${env.JOB_NAME}* #${env.BUILD_NUMBER} succeeded"
            )
        }
        failure {
            slackSend(
                color: 'danger',
                message: ":x: *${env.JOB_NAME}* #${env.BUILD_NUMBER} failed"
            )
        }
    }
}
```

### 예제 2: 파라미터를 활용한 배포 파이프라인

```groovy
pipeline {
    agent any

    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['staging', 'production'],
            description: '배포 대상 환경'
        )
        string(
            name: 'VERSION',
            defaultValue: 'latest',
            description: '배포할 이미지 태그'
        )
        booleanParam(
            name: 'RUN_SMOKE_TEST',
            defaultValue: true,
            description: 'Smoke Test 실행 여부'
        )
    }

    stages {
        stage('Confirm') {
            when {
                expression { params.ENVIRONMENT == 'production' }
            }
            steps {
                input message: "Production 배포를 진행하시겠습니까?\nVersion: ${params.VERSION}"
            }
        }

        stage('Deploy') {
            steps {
                echo "Deploying ${params.VERSION} to ${params.ENVIRONMENT}"
                sh "./deploy.sh ${params.ENVIRONMENT} ${params.VERSION}"
            }
        }

        stage('Smoke Test') {
            when {
                expression { params.RUN_SMOKE_TEST }
            }
            steps {
                sh "./smoke-test.sh ${params.ENVIRONMENT}"
            }
        }
    }
}
```

## 5. 정리

| 구성 요소 | 역할 | 핵심 키워드 |
|-----------|------|------------|
| pipeline {} | 최상위 블록 | 필수, 전체 파이프라인 정의 |
| agent | 실행 환경 지정 | any, docker, kubernetes, label |
| stages/stage | 파이프라인 단계 | 순차 실행, 시각적 표현 |
| steps | 실행 명령 | sh, bat, script |
| environment | 환경 변수 | credentials(), 스테이지별 스코프 |
| when | 조건부 실행 | branch, changeset, expression |
| parallel | 병렬 실행 | 스테이지 내 병렬 분기 |
| post | 빌드 후 처리 | always, success, failure |
| options | 파이프라인 옵션 | timeout, retry, buildDiscarder |
| parameters | 빌드 파라미터 | choice, string, booleanParam |

---
*참고: Jenkins Documentation - jenkins.io/doc/book/pipeline/syntax*
