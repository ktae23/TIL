# Gradle 기초와 Maven 비교

Gradle의 핵심 개념과 Maven과의 차이점을 정리합니다.

## 목차

- [Gradle이란?](#gradle이란)
- [Gradle vs Maven 비교](#gradle-vs-maven-비교)
- [build.gradle 기본 구조](#buildgradle-기본-구조)
- [의존성 관리](#의존성-관리)
- [Task와 Plugin](#task와-plugin)
- [자주 사용하는 명령어](#자주-사용하는-명령어)

---

## Gradle이란?

**Gradle**은 Groovy/Kotlin DSL 기반의 빌드 자동화 도구입니다.

```
소스코드 → 컴파일 → 테스트 → 패키징 → 배포
         ↑ Gradle이 자동화하는 영역
```

### 특징

- **유연성**: Groovy/Kotlin 스크립트로 커스텀 로직 작성 가능
- **성능**: 증분 빌드, 빌드 캐시, 병렬 실행
- **확장성**: 플러그인 시스템으로 기능 확장

---

## Gradle vs Maven 비교

### 핵심 차이점

| 구분 | Gradle | Maven |
|------|--------|-------|
| **설정 파일** | build.gradle (Groovy/Kotlin) | pom.xml (XML) |
| **설정 방식** | 스크립트 (프로그래밍) | 선언적 (XML) |
| **빌드 속도** | 빠름 (증분 빌드, 캐시) | 상대적으로 느림 |
| **커스터마이징** | 쉬움 (코드 작성) | 어려움 (플러그인 의존) |
| **학습 곡선** | 높음 | 낮음 |
| **멀티 모듈** | 유연함 | 표준화됨 |

### 설정 파일 비교

```xml
<!-- Maven: pom.xml -->
<project>
    <groupId>com.example</groupId>
    <artifactId>my-app</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <version>3.2.0</version>
        </dependency>
    </dependencies>
</project>
```

```groovy
// Gradle: build.gradle
plugins {
    id 'java'
}

group = 'com.example'
version = '1.0.0'

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web:3.2.0'
}
```

### 의존성 스코프 비교

| Maven | Gradle | 설명 |
|-------|--------|------|
| `compile` (deprecated) | `implementation` | 컴파일 + 런타임, 전이 의존성 노출 안함 |
| `compile` | `api` | 컴파일 + 런타임, 전이 의존성 노출 |
| `provided` | `compileOnly` | 컴파일에만 사용 |
| `runtime` | `runtimeOnly` | 런타임에만 사용 |
| `test` | `testImplementation` | 테스트 컴파일 + 런타임 |
| - | `testRuntimeOnly` | 테스트 런타임에만 사용 |
| - | `annotationProcessor` | 어노테이션 프로세서 |

### 빌드 명령어 비교

| 작업 | Maven | Gradle |
|------|-------|--------|
| 클린 | `mvn clean` | `./gradlew clean` |
| 컴파일 | `mvn compile` | `./gradlew compileJava` |
| 테스트 | `mvn test` | `./gradlew test` |
| 패키징 | `mvn package` | `./gradlew build` |
| 설치 | `mvn install` | `./gradlew publishToMavenLocal` |
| 의존성 트리 | `mvn dependency:tree` | `./gradlew dependencies` |

### 빌드 성능 비교

| 시나리오 | Maven | Gradle |
|----------|-------|--------|
| 클린 빌드 | 기준 | 2-3배 빠름 |
| 증분 빌드 | 지원 안함 | 변경된 부분만 빌드 |
| 빌드 캐시 | 지원 안함 | 로컬/원격 캐시 지원 |
| 병렬 빌드 | `-T` 옵션 필요 | 기본 지원 (`--parallel`) |
| 데몬 프로세스 | 없음 | JVM 재사용으로 빠른 시작 |

---

## build.gradle 기본 구조

### Groovy DSL

```groovy
// 플러그인
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

// 프로젝트 정보
group = 'com.example'
version = '1.0.0'

// Java 버전
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// 저장소
repositories {
    mavenCentral()
    maven { url 'https://repo.spring.io/milestone' }
}

// 의존성
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

// 테스트 설정
tasks.named('test') {
    useJUnitPlatform()
}
```

### Kotlin DSL (build.gradle.kts)

```kotlin
plugins {
    java
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.example"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

---

## 의존성 관리

### 의존성 선언

```groovy
dependencies {
    // 기본 형식
    implementation 'group:artifact:version'

    // BOM (Bill of Materials) 사용
    implementation platform('org.springframework.boot:spring-boot-dependencies:3.2.0')
    implementation 'org.springframework.boot:spring-boot-starter-web'  // 버전 생략

    // 특정 모듈 제외
    implementation('org.springframework.boot:spring-boot-starter-web') {
        exclude group: 'org.springframework.boot', module: 'spring-boot-starter-tomcat'
    }

    // 로컬 JAR
    implementation files('libs/my-lib.jar')
    implementation fileTree(dir: 'libs', include: ['*.jar'])
}
```

### 의존성 버전 관리

```groovy
// gradle.properties
springBootVersion=3.2.0
lombokVersion=1.18.30

// build.gradle
dependencies {
    implementation "org.springframework.boot:spring-boot-starter-web:${springBootVersion}"
    compileOnly "org.projectlombok:lombok:${lombokVersion}"
}
```

```groovy
// 또는 ext 블록 사용
ext {
    versions = [
        springBoot: '3.2.0',
        lombok: '1.18.30'
    ]
}

dependencies {
    implementation "org.springframework.boot:spring-boot-starter-web:${versions.springBoot}"
}
```

### Version Catalog (권장, Gradle 7.0+)

```toml
# gradle/libs.versions.toml
[versions]
spring-boot = "3.2.0"
lombok = "1.18.30"

[libraries]
spring-boot-starter-web = { module = "org.springframework.boot:spring-boot-starter-web", version.ref = "spring-boot" }
lombok = { module = "org.projectlombok:lombok", version.ref = "lombok" }

[plugins]
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
```

```groovy
// build.gradle
plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation libs.spring.boot.starter.web
    compileOnly libs.lombok
}
```

---

## Task와 Plugin

### 커스텀 Task

```groovy
// 간단한 Task
tasks.register('hello') {
    doLast {
        println 'Hello, Gradle!'
    }
}

// 의존성 있는 Task
tasks.register('goodbye') {
    dependsOn 'hello'
    doLast {
        println 'Goodbye!'
    }
}

// 타입 지정 Task
tasks.register('copyDocs', Copy) {
    from 'src/docs'
    into 'build/docs'
}
```

### 주요 플러그인

```groovy
plugins {
    id 'java'                    // Java 컴파일, JAR 생성
    id 'java-library'            // Java 라이브러리 (api 의존성)
    id 'application'             // 실행 가능한 애플리케이션
    id 'war'                     // WAR 패키징
    id 'jacoco'                  // 코드 커버리지

    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
}
```

---

## 자주 사용하는 명령어

```bash
# 빌드
./gradlew build              # 전체 빌드
./gradlew build -x test      # 테스트 제외 빌드
./gradlew clean build        # 클린 빌드

# 실행
./gradlew bootRun            # Spring Boot 실행
./gradlew run                # application 플러그인

# 테스트
./gradlew test               # 테스트 실행
./gradlew test --tests "*.UserServiceTest"  # 특정 테스트만

# 의존성
./gradlew dependencies       # 의존성 트리
./gradlew dependencyInsight --dependency lombok  # 특정 의존성 분석

# 정보
./gradlew tasks              # 사용 가능한 Task 목록
./gradlew projects           # 프로젝트 목록 (멀티 모듈)
./gradlew properties         # 프로젝트 속성

# 캐시
./gradlew build --build-cache       # 빌드 캐시 사용
./gradlew clean --no-build-cache    # 캐시 없이 클린

# 디버그
./gradlew build --info       # 상세 로그
./gradlew build --debug      # 디버그 로그
./gradlew build --scan       # 빌드 스캔 (웹 리포트)
```

---

## 선택 가이드

| 상황 | 추천 |
|------|------|
| 신규 프로젝트 | **Gradle** - 빠른 빌드, 유연성 |
| 레거시/안정성 우선 | **Maven** - 표준화, 풍부한 레퍼런스 |
| 복잡한 빌드 로직 필요 | **Gradle** - 스크립트 작성 가능 |
| 팀 경험이 Maven 위주 | **Maven** - 학습 비용 고려 |
| Android 프로젝트 | **Gradle** - 공식 빌드 도구 |
| 멀티 모듈 대규모 | **Gradle** - 성능, 유연성 |

---

*마지막 업데이트: 2026년 01월*
