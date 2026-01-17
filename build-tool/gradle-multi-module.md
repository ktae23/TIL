# Gradle 멀티 모듈 프로젝트

Gradle 멀티 모듈 프로젝트 구성 방법과 실무 패턴을 상세히 정리합니다.

## 목차

- [멀티 모듈이란?](#멀티-모듈이란)
- [프로젝트 구조](#프로젝트-구조)
- [기본 설정](#기본-설정)
- [공통 설정 관리](#공통-설정-관리)
- [모듈 간 의존성](#모듈-간-의존성)
- [실무 프로젝트 예제](#실무-프로젝트-예제)
- [빌드 최적화](#빌드-최적화)

---

## 멀티 모듈이란?

하나의 프로젝트를 여러 모듈로 분리하여 관리하는 방식입니다.

### 장점

- **관심사 분리**: 도메인, API, 인프라 등 역할별 분리
- **재사용성**: 공통 모듈을 여러 모듈에서 사용
- **빌드 최적화**: 변경된 모듈만 빌드
- **팀 협업**: 모듈별 독립적 개발 가능
- **배포 유연성**: 모듈별 개별 배포 가능

---

## 프로젝트 구조

### 기본 구조

```
my-project/
├── settings.gradle              # 프로젝트 설정, 모듈 정의
├── build.gradle                 # 루트 빌드 스크립트
├── gradle.properties            # 공통 프로퍼티
├── gradle/
│   └── libs.versions.toml       # 버전 카탈로그
│
├── module-core/                 # 핵심 도메인 모듈
│   ├── build.gradle
│   └── src/
│       └── main/java/
│
├── module-api/                  # API 모듈
│   ├── build.gradle
│   └── src/
│       └── main/java/
│
├── module-batch/                # 배치 모듈
│   ├── build.gradle
│   └── src/
│       └── main/java/
│
└── module-common/               # 공통 유틸 모듈
    ├── build.gradle
    └── src/
        └── main/java/
```

### 실무 추천 구조 (Layered)

```
my-project/
├── settings.gradle
├── build.gradle
│
├── core/
│   ├── core-domain/             # 순수 도메인 (JPA 엔티티, VO)
│   ├── core-usecase/            # 유스케이스 인터페이스
│   └── core-port/               # 포트 인터페이스 (in/out)
│
├── infrastructure/
│   ├── infra-persistence/       # JPA Repository 구현
│   ├── infra-redis/             # Redis 어댑터
│   └── infra-external-api/      # 외부 API 클라이언트
│
├── application/
│   ├── app-api/                 # REST API 애플리케이션
│   └── app-batch/               # 배치 애플리케이션
│
└── support/
    ├── support-logging/         # 로깅 유틸
    └── support-test/            # 테스트 유틸
```

---

## 기본 설정

### settings.gradle

```groovy
// settings.gradle
rootProject.name = 'my-project'

// 단일 레벨 모듈
include 'module-core'
include 'module-api'
include 'module-batch'
include 'module-common'

// 중첩 모듈 (디렉토리 구조 반영)
include ':core:core-domain'
include ':core:core-usecase'
include ':infrastructure:infra-persistence'
include ':application:app-api'
include ':application:app-batch'

// 모듈 경로 커스터마이징
project(':app-api').projectDir = file('application/app-api')
```

### 루트 build.gradle

```groovy
// build.gradle (루트)
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0' apply false
    id 'io.spring.dependency-management' version '1.1.4' apply false
}

// 모든 프로젝트에 적용
allprojects {
    group = 'com.example'
    version = '1.0.0'

    repositories {
        mavenCentral()
    }
}

// 서브 프로젝트에만 적용
subprojects {
    apply plugin: 'java'
    apply plugin: 'io.spring.dependency-management'

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    configurations {
        compileOnly {
            extendsFrom annotationProcessor
        }
    }

    dependencies {
        compileOnly 'org.projectlombok:lombok'
        annotationProcessor 'org.projectlombok:lombok'
        testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
    }

    tasks.named('test') {
        useJUnitPlatform()
    }
}
```

### 서브 모듈 build.gradle

```groovy
// module-core/build.gradle
dependencies {
    // 다른 모듈 의존
    implementation project(':module-common')

    // 외부 의존성
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
}
```

```groovy
// module-api/build.gradle
plugins {
    id 'org.springframework.boot'
}

dependencies {
    implementation project(':module-core')
    implementation project(':module-common')

    implementation 'org.springframework.boot:spring-boot-starter-web'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

---

## 공통 설정 관리

### 방법 1: buildSrc (권장)

```
my-project/
├── buildSrc/
│   ├── build.gradle
│   └── src/main/groovy/
│       └── com.example.java-conventions.gradle
```

```groovy
// buildSrc/build.gradle
plugins {
    id 'groovy-gradle-plugin'
}

repositories {
    gradlePluginPortal()
}
```

```groovy
// buildSrc/src/main/groovy/com.example.java-conventions.gradle
plugins {
    id 'java'
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly 'org.projectlombok:lombok:1.18.30'
    annotationProcessor 'org.projectlombok:lombok:1.18.30'
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

```groovy
// module-core/build.gradle (사용)
plugins {
    id 'com.example.java-conventions'
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
}
```

### 방법 2: Convention Plugin 분리

```groovy
// buildSrc/src/main/groovy/com.example.spring-boot-conventions.gradle
plugins {
    id 'com.example.java-conventions'
    id 'org.springframework.boot'
    id 'io.spring.dependency-management'
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

bootJar {
    enabled = false
}

jar {
    enabled = true
}
```

```groovy
// buildSrc/src/main/groovy/com.example.spring-boot-application.gradle
plugins {
    id 'com.example.spring-boot-conventions'
}

bootJar {
    enabled = true
}

jar {
    enabled = false
}
```

```groovy
// module-api/build.gradle (실행 가능한 모듈)
plugins {
    id 'com.example.spring-boot-application'
}

dependencies {
    implementation project(':module-core')
}
```

---

## 모듈 간 의존성

### implementation vs api

```groovy
// module-core/build.gradle
plugins {
    id 'java-library'  // api 사용하려면 필요
}

dependencies {
    // implementation: 전이 의존성 노출 안함
    implementation 'com.google.guava:guava:32.1.3-jre'

    // api: 전이 의존성 노출 (의존하는 모듈에서도 사용 가능)
    api 'org.springframework.boot:spring-boot-starter-data-jpa'
}
```

```
module-api → module-core → guava (implementation)
                         → spring-data-jpa (api)

module-api에서:
- guava 사용 불가 ❌ (implementation)
- spring-data-jpa 사용 가능 ✅ (api)
```

### 의존성 방향

```groovy
// 올바른 의존성 방향 (위에서 아래로)
// app-api → core-usecase → core-domain

// app-api/build.gradle
dependencies {
    implementation project(':core:core-usecase')
    implementation project(':infrastructure:infra-persistence')
}

// core-usecase/build.gradle
dependencies {
    implementation project(':core:core-domain')
    implementation project(':core:core-port')
}

// infra-persistence/build.gradle
dependencies {
    implementation project(':core:core-domain')
    implementation project(':core:core-port')
}
```

### 순환 의존성 방지

```groovy
// 잘못된 예: 순환 의존성
// module-a → module-b → module-a ❌

// 해결: 공통 모듈 분리
// module-a → module-common ← module-b ✅
```

---

## 실무 프로젝트 예제

### 전체 구조

```
ecommerce/
├── settings.gradle
├── build.gradle
├── gradle/
│   └── libs.versions.toml
│
├── buildSrc/
│   ├── build.gradle
│   └── src/main/groovy/
│       ├── ecommerce.java-conventions.gradle
│       ├── ecommerce.spring-conventions.gradle
│       └── ecommerce.spring-app.gradle
│
├── core/
│   ├── core-domain/
│   │   ├── build.gradle
│   │   └── src/main/java/com/example/domain/
│   │       ├── member/
│   │       │   ├── Member.java
│   │       │   └── MemberRepository.java
│   │       └── order/
│   │           ├── Order.java
│   │           └── OrderRepository.java
│   │
│   └── core-service/
│       ├── build.gradle
│       └── src/main/java/com/example/service/
│           ├── MemberService.java
│           └── OrderService.java
│
├── infrastructure/
│   ├── infra-jpa/
│   │   ├── build.gradle
│   │   └── src/main/java/com/example/infra/jpa/
│   │       ├── MemberJpaRepository.java
│   │       └── OrderJpaRepository.java
│   │
│   └── infra-redis/
│       ├── build.gradle
│       └── src/main/java/com/example/infra/redis/
│           └── RedisCacheRepository.java
│
└── application/
    ├── app-api/
    │   ├── build.gradle
    │   └── src/main/java/com/example/api/
    │       ├── ApiApplication.java
    │       └── controller/
    │
    └── app-admin/
        ├── build.gradle
        └── src/main/java/com/example/admin/
            └── AdminApplication.java
```

### settings.gradle

```groovy
// settings.gradle
rootProject.name = 'ecommerce'

// Core 모듈
include ':core:core-domain'
include ':core:core-service'

// Infrastructure 모듈
include ':infrastructure:infra-jpa'
include ':infrastructure:infra-redis'

// Application 모듈
include ':application:app-api'
include ':application:app-admin'
```

### gradle/libs.versions.toml

```toml
[versions]
spring-boot = "3.2.0"
spring-dependency-management = "1.1.4"
lombok = "1.18.30"
mapstruct = "1.5.5.Final"
querydsl = "5.0.0"

[libraries]
# Spring
spring-boot-starter = { module = "org.springframework.boot:spring-boot-starter" }
spring-boot-starter-web = { module = "org.springframework.boot:spring-boot-starter-web" }
spring-boot-starter-data-jpa = { module = "org.springframework.boot:spring-boot-starter-data-jpa" }
spring-boot-starter-data-redis = { module = "org.springframework.boot:spring-boot-starter-data-redis" }
spring-boot-starter-test = { module = "org.springframework.boot:spring-boot-starter-test" }

# Lombok
lombok = { module = "org.projectlombok:lombok", version.ref = "lombok" }

# MapStruct
mapstruct = { module = "org.mapstruct:mapstruct", version.ref = "mapstruct" }
mapstruct-processor = { module = "org.mapstruct:mapstruct-processor", version.ref = "mapstruct" }

# QueryDSL
querydsl-jpa = { module = "com.querydsl:querydsl-jpa", version.ref = "querydsl" }
querydsl-apt = { module = "com.querydsl:querydsl-apt", version.ref = "querydsl" }

[plugins]
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
spring-dependency-management = { id = "io.spring.dependency-management", version.ref = "spring-dependency-management" }

[bundles]
mapstruct = ["mapstruct", "mapstruct-processor"]
```

### buildSrc 설정

```groovy
// buildSrc/build.gradle
plugins {
    id 'groovy-gradle-plugin'
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-gradle-plugin:3.2.0'
    implementation 'io.spring.gradle:dependency-management-plugin:1.1.4'
}
```

```groovy
// buildSrc/src/main/groovy/ecommerce.java-conventions.gradle
plugins {
    id 'java-library'
}

group = 'com.example'
version = '1.0.0'

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

dependencies {
    compileOnly libs.lombok
    annotationProcessor libs.lombok

    testCompileOnly libs.lombok
    testAnnotationProcessor libs.lombok
}

tasks.withType(JavaCompile) {
    options.encoding = 'UTF-8'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

```groovy
// buildSrc/src/main/groovy/ecommerce.spring-conventions.gradle
plugins {
    id 'ecommerce.java-conventions'
    id 'org.springframework.boot'
    id 'io.spring.dependency-management'
}

dependencies {
    implementation libs.spring.boot.starter
    testImplementation libs.spring.boot.starter.test
}

// 라이브러리 모듈은 bootJar 비활성화
bootJar {
    enabled = false
}

jar {
    enabled = true
}
```

```groovy
// buildSrc/src/main/groovy/ecommerce.spring-app.gradle
plugins {
    id 'ecommerce.spring-conventions'
}

// 실행 가능한 애플리케이션은 bootJar 활성화
bootJar {
    enabled = true
}

jar {
    enabled = false
}
```

### 각 모듈 build.gradle

```groovy
// core/core-domain/build.gradle
plugins {
    id 'ecommerce.java-conventions'
}

dependencies {
    // 순수 도메인 - 최소한의 의존성
    implementation 'jakarta.persistence:jakarta.persistence-api:3.1.0'
}
```

```groovy
// core/core-service/build.gradle
plugins {
    id 'ecommerce.spring-conventions'
}

dependencies {
    api project(':core:core-domain')

    implementation libs.spring.boot.starter
}
```

```groovy
// infrastructure/infra-jpa/build.gradle
plugins {
    id 'ecommerce.spring-conventions'
}

dependencies {
    implementation project(':core:core-domain')

    implementation libs.spring.boot.starter.data.jpa

    // QueryDSL
    implementation(libs.querydsl.jpa) {
        artifact {
            classifier = 'jakarta'
        }
    }
    annotationProcessor(libs.querydsl.apt) {
        artifact {
            classifier = 'jakarta'
        }
    }
    annotationProcessor 'jakarta.annotation:jakarta.annotation-api'
    annotationProcessor 'jakarta.persistence:jakarta.persistence-api'

    runtimeOnly 'com.mysql:mysql-connector-j'
    testRuntimeOnly 'com.h2database:h2'
}

// QueryDSL Q클래스 생성 위치
def querydslDir = "$buildDir/generated/querydsl"

sourceSets {
    main.java.srcDirs += [ querydslDir ]
}

tasks.withType(JavaCompile) {
    options.annotationProcessorGeneratedSourcesDirectory = file(querydslDir)
}

clean.doLast {
    file(querydslDir).deleteDir()
}
```

```groovy
// infrastructure/infra-redis/build.gradle
plugins {
    id 'ecommerce.spring-conventions'
}

dependencies {
    implementation project(':core:core-domain')

    implementation libs.spring.boot.starter.data.redis
}
```

```groovy
// application/app-api/build.gradle
plugins {
    id 'ecommerce.spring-app'
}

dependencies {
    implementation project(':core:core-service')
    implementation project(':infrastructure:infra-jpa')
    implementation project(':infrastructure:infra-redis')

    implementation libs.spring.boot.starter.web

    // MapStruct
    implementation libs.mapstruct
    annotationProcessor libs.mapstruct.processor
}
```

---

## 빌드 최적화

### 병렬 빌드

```properties
# gradle.properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
org.gradle.jvmargs=-Xmx2g -XX:+HeapDumpOnOutOfMemoryError
```

### 특정 모듈만 빌드

```bash
# 특정 모듈만 빌드
./gradlew :app-api:build

# 의존하는 모듈 포함 빌드
./gradlew :app-api:build --include-build

# 특정 모듈만 테스트
./gradlew :core:core-domain:test

# 변경된 모듈만 테스트
./gradlew test --continuous
```

### 빌드 캐시 설정

```groovy
// settings.gradle
buildCache {
    local {
        enabled = true
        directory = new File(rootDir, '.gradle/build-cache')
    }
    // 팀 공유 원격 캐시
    remote(HttpBuildCache) {
        url = 'https://cache.example.com/'
        push = System.getenv('CI') != null
        credentials {
            username = System.getenv('CACHE_USER')
            password = System.getenv('CACHE_PASSWORD')
        }
    }
}
```

### 의존성 잠금

```bash
# 의존성 버전 잠금
./gradlew dependencies --write-locks
```

```groovy
// build.gradle
dependencyLocking {
    lockAllConfigurations()
}
```

---

## 트러블슈팅

### 순환 의존성 감지

```bash
./gradlew dependencies --configuration compileClasspath
```

### 중복 클래스 문제

```groovy
// 특정 의존성 제외
configurations.all {
    exclude group: 'org.slf4j', module: 'slf4j-log4j12'
}
```

### 버전 충돌 해결

```groovy
configurations.all {
    resolutionStrategy {
        // 특정 버전 강제
        force 'com.google.guava:guava:32.1.3-jre'

        // 버전 충돌 시 실패
        failOnVersionConflict()
    }
}
```

---

*마지막 업데이트: 2026년 01월*
