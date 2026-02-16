# Jenkins Scripted Pipeline과 Shared Library

Scripted Pipeline은 Groovy DSL을 직접 사용하여 파이프라인을 작성하는 방식으로, Declarative보다 높은 유연성을 제공한다. Shared Library는 여러 Jenkins 프로젝트에서 파이프라인 코드를 재사용할 수 있게 해주는 메커니즘이다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### Scripted Pipeline

Scripted Pipeline은 Jenkins Pipeline의 원래 형태로, Groovy 언어의 전체 기능을 활용하여 파이프라인을 작성한다. `node` 블록으로 시작하며 일반 Groovy 코드처럼 자유롭게 구성할 수 있다.

```groovy
// Scripted Pipeline 기본 형태
node {
    stage('Build') {
        sh 'mvn clean package'
    }
    stage('Test') {
        sh 'mvn test'
    }
}
```

### Shared Library

Shared Library는 Jenkins 파이프라인 코드를 별도 Git 리포지토리로 분리하여 여러 프로젝트에서 재사용하는 구조다.

```
shared-library-repo/
├── vars/                  # 글로벌 변수/함수 (Pipeline에서 직접 호출)
│   ├── buildApp.groovy
│   ├── deployApp.groovy
│   └── notifySlack.groovy
├── src/                   # Groovy 클래스 (라이브러리 코드)
│   └── com/
│       └── example/
│           └── Pipeline.groovy
└── resources/             # 리소스 파일 (config, template)
    └── deploy-template.yaml
```

## 2. 왜 알아야 하는가 (Why)

### Scripted Pipeline이 필요한 상황

| 상황 | 이유 |
|------|------|
| 복잡한 조건 로직 | if/else, switch, 루프 등 자유로운 제어 흐름 |
| 동적 스테이지 생성 | 런타임에 스테이지 수와 내용 결정 |
| 외부 API 호출 및 결과 처리 | HTTP 호출, JSON 파싱 등 |
| 에러 핸들링 | try-catch-finally로 세밀한 에러 처리 |

### Shared Library가 필요한 상황

- 조직 내 20+ 프로젝트가 유사한 빌드 패턴을 사용
- 배포 절차가 표준화되어야 하는 경우
- 보안/컴플라이언스 정책을 파이프라인에 강제해야 하는 경우
- Jenkins 관리자가 파이프라인 로직을 중앙에서 관리하려는 경우

## 3. 내부 구현 분석 (How)

### Scripted Pipeline 문법 상세

#### 기본 구조

```groovy
node('linux') {                    // Agent 레이블 지정
    def appVersion = ''

    stage('Checkout') {
        checkout scm
        appVersion = sh(
            script: 'cat VERSION',
            returnStdout: true
        ).trim()
        echo "Building version: ${appVersion}"
    }

    stage('Build') {
        sh "mvn clean package -Dversion=${appVersion}"
    }
}
```

#### 에러 처리 (try-catch-finally)

```groovy
node {
    def deploySuccess = false

    try {
        stage('Build') {
            sh 'mvn clean package'
        }

        stage('Deploy') {
            sh './deploy.sh production'
            deploySuccess = true
        }
    } catch (Exception e) {
        currentBuild.result = 'FAILURE'
        echo "Build failed: ${e.message}"

        // 배포 실패 시 롤백
        if (!deploySuccess) {
            stage('Rollback') {
                sh './rollback.sh production'
            }
        }
    } finally {
        // 항상 실행
        stage('Cleanup') {
            cleanWs()
        }

        // 알림
        if (currentBuild.result == 'FAILURE') {
            slackSend(color: 'danger', message: "Build failed: ${env.JOB_NAME}")
        } else {
            slackSend(color: 'good', message: "Build succeeded: ${env.JOB_NAME}")
        }
    }
}
```

#### 동적 스테이지 생성

```groovy
node {
    def services = ['api', 'web', 'worker', 'scheduler']

    stage('Checkout') {
        checkout scm
    }

    // 동적으로 스테이지 생성
    for (service in services) {
        stage("Build ${service}") {
            dir("services/${service}") {
                sh 'docker build -t ${service}:latest .'
            }
        }
    }

    // 병렬 동적 스테이지
    def parallelStages = [:]
    for (service in services) {
        def s = service  // 클로저 캡처를 위한 변수 복사
        parallelStages["Test ${s}"] = {
            node {
                stage("Test ${s}") {
                    sh "docker run --rm ${s}:latest npm test"
                }
            }
        }
    }
    parallel parallelStages
}
```

#### HTTP 호출과 JSON 처리

```groovy
node {
    stage('Check Dependencies') {
        def response = httpRequest(
            url: 'https://api.example.com/health',
            httpMode: 'GET',
            acceptType: 'APPLICATION_JSON'
        )

        def json = readJSON text: response.content
        if (json.status != 'healthy') {
            error "Dependency service is unhealthy: ${json.status}"
        }
    }

    stage('Deploy with API') {
        def payload = [
            version: env.BUILD_NUMBER,
            environment: 'staging',
            services: ['api', 'web']
        ]

        httpRequest(
            url: 'https://deploy-api.example.com/deploy',
            httpMode: 'POST',
            contentType: 'APPLICATION_JSON',
            requestBody: groovy.json.JsonOutput.toJson(payload),
            customHeaders: [[name: 'Authorization', value: "Bearer ${env.DEPLOY_TOKEN}"]]
        )
    }
}
```

### Shared Library 구현

#### vars/ 디렉토리 — 글로벌 함수

```groovy
// vars/buildJavaApp.groovy
def call(Map config = [:]) {
    def jdkVersion = config.jdkVersion ?: '21'
    def buildTool = config.buildTool ?: 'gradle'

    pipeline {
        agent any

        tools {
            jdk "jdk-${jdkVersion}"
        }

        stages {
            stage('Build') {
                steps {
                    script {
                        if (buildTool == 'gradle') {
                            sh './gradlew clean build -x test'
                        } else {
                            sh 'mvn clean package -DskipTests'
                        }
                    }
                }
            }

            stage('Test') {
                steps {
                    script {
                        if (buildTool == 'gradle') {
                            sh './gradlew test'
                        } else {
                            sh 'mvn test'
                        }
                    }
                }
            }
        }
    }
}
```

```groovy
// vars/notifySlack.groovy
def call(String status, String channel = '#ci-cd') {
    def color = status == 'SUCCESS' ? 'good' : 'danger'
    def emoji = status == 'SUCCESS' ? ':white_check_mark:' : ':x:'

    slackSend(
        channel: channel,
        color: color,
        message: "${emoji} *${env.JOB_NAME}* #${env.BUILD_NUMBER} - ${status}\n" +
                 "Branch: ${env.BRANCH_NAME}\n" +
                 "(<${env.BUILD_URL}|View Build>)"
    )
}
```

```groovy
// vars/dockerBuildPush.groovy
def call(Map config) {
    def registry = config.registry
    def imageName = config.imageName
    def tag = config.tag ?: env.BUILD_NUMBER

    docker.withRegistry("https://${registry}", config.credentialsId) {
        def image = docker.build("${imageName}:${tag}")
        image.push()
        image.push('latest')
        return "${registry}/${imageName}:${tag}"
    }
}
```

#### src/ 디렉토리 — Groovy 클래스

```groovy
// src/com/example/DeployConfig.groovy
package com.example

class DeployConfig implements Serializable {
    String environment
    String version
    String namespace
    int replicas
    Map<String, String> envVars = [:]

    static DeployConfig staging(String version) {
        return new DeployConfig(
            environment: 'staging',
            version: version,
            namespace: 'staging',
            replicas: 2,
            envVars: [LOG_LEVEL: 'debug']
        )
    }

    static DeployConfig production(String version) {
        return new DeployConfig(
            environment: 'production',
            version: version,
            namespace: 'production',
            replicas: 4,
            envVars: [LOG_LEVEL: 'info']
        )
    }
}
```

#### Shared Library 설정

Jenkins 관리 > System Configuration > Global Pipeline Libraries:

```
Name: my-shared-library
Default version: main
Retrieval method: Modern SCM (Git)
Project Repository: https://github.com/org/jenkins-shared-library.git
```

#### Shared Library 사용

```groovy
// Jenkinsfile - Shared Library 사용
@Library('my-shared-library') _

// 방법 1: vars/ 함수 직접 호출
buildJavaApp(jdkVersion: '21', buildTool: 'gradle')

// 방법 2: 여러 함수 조합
pipeline {
    agent any

    stages {
        stage('Build & Test') {
            steps {
                script {
                    buildJavaApp(jdkVersion: '21')
                }
            }
        }

        stage('Docker') {
            steps {
                script {
                    dockerBuildPush(
                        registry: 'ghcr.io',
                        imageName: 'my-org/my-app',
                        credentialsId: 'ghcr-token'
                    )
                }
            }
        }
    }

    post {
        success { notifySlack('SUCCESS') }
        failure { notifySlack('FAILURE') }
    }
}
```

```groovy
// 방법 3: src/ 클래스 사용
@Library('my-shared-library') _
import com.example.DeployConfig

node {
    def config = DeployConfig.staging(env.BUILD_NUMBER)

    stage('Deploy') {
        sh """
            kubectl set image deployment/app app=my-app:${config.version} \
                -n ${config.namespace}
            kubectl scale deployment/app --replicas=${config.replicas} \
                -n ${config.namespace}
        """
    }
}
```

## 4. 실전 예제

### 예제 1: Declarative + Scripted 혼용

```groovy
pipeline {
    agent any

    stages {
        stage('Dynamic Parallel Build') {
            steps {
                script {
                    // Declarative 안에서 Scripted 사용 (script {} 블록)
                    def changedServices = sh(
                        script: "git diff --name-only HEAD~1 | grep 'services/' | cut -d/ -f2 | sort -u",
                        returnStdout: true
                    ).trim().split('\n')

                    def parallelBuilds = [:]
                    changedServices.each { service ->
                        parallelBuilds["Build ${service}"] = {
                            sh "cd services/${service} && docker build -t ${service}:latest ."
                        }
                    }

                    if (parallelBuilds.size() > 0) {
                        parallel parallelBuilds
                    } else {
                        echo 'No services changed'
                    }
                }
            }
        }
    }
}
```

### 예제 2: Shared Library 기반 표준 파이프라인

```groovy
// vars/standardPipeline.groovy — 조직 표준 파이프라인
def call(Map config) {
    pipeline {
        agent any

        options {
            timeout(time: config.timeout ?: 30, unit: 'MINUTES')
            buildDiscarder(logRotator(numToKeepStr: '20'))
        }

        stages {
            stage('Build') {
                steps {
                    script {
                        switch (config.buildTool) {
                            case 'gradle':
                                sh './gradlew clean build -x test'
                                break
                            case 'maven':
                                sh 'mvn clean package -DskipTests'
                                break
                            case 'npm':
                                sh 'npm ci && npm run build'
                                break
                        }
                    }
                }
            }

            stage('Test') {
                steps {
                    script {
                        switch (config.buildTool) {
                            case 'gradle':
                                sh './gradlew test'
                                break
                            case 'maven':
                                sh 'mvn test'
                                break
                            case 'npm':
                                sh 'npm test'
                                break
                        }
                    }
                }
            }

            stage('Docker Build & Push') {
                when { branch 'main' }
                steps {
                    script {
                        dockerBuildPush(
                            registry: config.registry ?: 'ghcr.io',
                            imageName: config.imageName,
                            credentialsId: config.registryCredentials
                        )
                    }
                }
            }

            stage('Deploy Staging') {
                when { branch 'main' }
                steps {
                    script {
                        deployToK8s(
                            namespace: 'staging',
                            deployment: config.deploymentName,
                            image: "${config.registry}/${config.imageName}:${env.BUILD_NUMBER}"
                        )
                    }
                }
            }
        }

        post {
            success { notifySlack('SUCCESS', config.slackChannel ?: '#ci-cd') }
            failure { notifySlack('FAILURE', config.slackChannel ?: '#ci-cd') }
        }
    }
}
```

사용:
```groovy
// 각 프로젝트의 Jenkinsfile — 단 3줄
@Library('my-shared-library') _

standardPipeline(
    buildTool: 'gradle',
    imageName: 'my-org/user-service',
    deploymentName: 'user-service',
    registry: 'ghcr.io',
    registryCredentials: 'ghcr-token',
    slackChannel: '#backend'
)
```

## 5. 정리

| 개념 | 설명 | 사용 시점 |
|------|------|----------|
| Scripted Pipeline | Groovy 기반 자유형 파이프라인 | 복잡한 로직, 동적 스테이지 |
| Declarative Pipeline | 구조화된 블록 기반 파이프라인 | 일반적인 CI/CD (권장) |
| script {} 블록 | Declarative 내에서 Scripted 사용 | Declarative에서 유연성 필요 시 |
| Shared Library vars/ | 글로벌 함수 정의 | 파이프라인에서 직접 호출 |
| Shared Library src/ | Groovy 클래스 정의 | 복잡한 비즈니스 로직 캡슐화 |
| @Library | Shared Library 로드 | Jenkinsfile 최상단에 선언 |

### Shared Library 베스트 프랙티스

1. **버전 태그 사용** — `@Library('my-lib@v2.0')` 으로 특정 버전 고정
2. **유닛 테스트 작성** — Jenkins Pipeline Unit 프레임워크 활용
3. **문서화** — 각 vars/ 함수에 사용법과 파라미터 설명
4. **점진적 마이그레이션** — 한 번에 모든 프로젝트를 전환하지 않기
5. **최소 권한 원칙** — Sandbox 모드에서 필요한 메서드만 화이트리스트

---
*참고: Jenkins Documentation - Pipeline Syntax, Extending with Shared Libraries*
