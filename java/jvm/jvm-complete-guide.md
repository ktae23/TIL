# JVM 완전 가이드 (Java 8 → Java 17+)

Java Virtual Machine의 메모리 구조, 실행 흐름, 클래스 로딩, 컴파일러, 가비지 컬렉션에 대한 종합 가이드입니다.

**기준 버전**: Java 8 (Java 9, 11, 17 변경사항 포함)

---

## 목차

- [1. JVM 개요 및 학습 가이드](#1-jvm-개요-및-학습-가이드)
  - [Java 버전별 핵심 변경사항](#java-버전별-핵심-변경사항)
  - [추천 학습 순서](#추천-학습-순서)
- [2. 메모리 구조](#2-메모리-구조)
  - [메모리 영역 개요](#메모리-영역-개요)
  - [Metaspace](#metaspace)
  - [Heap 메모리와 GC](#heap-메모리와-gc)
  - [Runtime 클래스로 메모리 확인](#runtime-클래스로-메모리-확인)
  - [실전 활용: Elastic Beanstalk 설정](#실전-활용-elastic-beanstalk-설정)
  - [Stack](#stack)
  - [메모리 핵심 요약](#메모리-핵심-요약)
- [3. 가비지 컬렉션](#3-가비지-컬렉션)
  - [Java 8 기본 GC: Parallel GC](#java-8-기본-gc-parallel-gc)
  - [Java 9+ 기본 GC: G1 GC](#java-9-기본-gc-g1-gc-garbage-first)
  - [Java 11: ZGC](#java-11-zgc-z-garbage-collector)
  - [Java 11: Epsilon GC](#java-11-epsilon-gc-no-op-garbage-collector)
  - [GC 알고리즘 통합 비교](#gc-알고리즘-통합-비교)
- [4. 실행 흐름과 컴파일러](#4-실행-흐름과-컴파일러)
  - [전체 실행 과정](#전체-실행-과정-상세)
  - [메서드 호출과 Stack Frame](#메서드-호출-예시-stack-frame-생성-과정)
  - [Java 8: 인터프리터 + JIT](#java-8-인터프리터--jit-컴파일러)
  - [Java 9+: AOT 컴파일러](#java-9-aot-ahead-of-time-컴파일러-추가)
  - [Java 17+: GraalVM Native Image](#java-17-aotgraal-jit-제거-및-graalvm-native-image로의-전환)
- [5. 클래스 로더와 모듈 시스템](#5-클래스-로더와-모듈-시스템)
  - [클래스 로더 계층 구조](#클래스-로더-계층-구조)
  - [클래스 로딩 과정 (3단계)](#클래스-로딩-과정-3단계)
- [6. 어노테이션 프로세서](#6-어노테이션-프로세서)
  - [처리 시점 비교](#처리-시점-비교)
  - [주요 사용 사례](#주요-사용-사례)
  - [동작 원리](#동작-원리-java-8-annotation-processing-api)
  - [Annotation Processor vs Reflection](#annotation-processor-vs-reflection)
- [참고 자료](#참고-자료)

---

## 1. JVM 개요 및 학습 가이드

### Java 버전별 핵심 변경사항

#### Java 8 (2014)
- **Metaspace 도입**: PermGen 제거, Native Memory 사용
- **Parallel GC 기본**: 고정된 Young/Old 구조
- **Lambda와 Stream API**: 함수형 프로그래밍

#### Java 9 (2017)
- **모듈 시스템 (JPMS)**: rt.jar 제거, 강력한 캡슐화
- **G1 GC 기본**: Region 기반, 예측 가능한 중지 시간
- **AOT 컴파일러**: 사전 컴파일로 빠른 시작

#### Java 11 (2018, LTS)
- **ZGC**: 초저지연 (10ms 미만), TB급 Heap 지원
- **Epsilon GC**: No-Op GC, 성능 테스트용
- **Flight Recorder 오픈소스화**: 무료 프로파일링 도구

#### Java 17 (2021, LTS)
- **AOT/Graal JIT 제거**: GraalVM Native Image로 대체
- **Sealed Classes**: 상속 제한
- **Pattern Matching for switch**: 패턴 매칭

### 추천 학습 순서

1. **초급**: 메모리 구조와 GC → Java 8 Parallel GC 부분만
2. **중급**: 실행 흐름 → JIT 컴파일러까지
3. **고급**: 클래스 로더 → 모듈 시스템
4. **심화**: 각 섹션의 Java 9+, Java 11 변경사항

---

## 2. 메모리 구조

### 메모리 영역 개요

JVM은 다음과 같은 메모리 영역으로 구분됩니다:

#### 메모리 영역 비교표 (Java 8 기준)

| 영역 | 스레드 공유 | 생명주기 | 주요 저장 내용 | GC 대상 |
|------|------------|---------|--------------|---------|
| **Metaspace** | 공유 | JVM 시작~종료 | 클래스 메타데이터, static 변수, 상수 풀 | O |
| **Heap** | 공유 | JVM 시작~종료 | 객체 인스턴스, 배열 | O |
| **Stack** | 스레드별 | 스레드 시작~종료 | 지역 변수, 메서드 호출 정보 | X |
| **PC Register** | 스레드별 | 스레드 시작~종료 | 현재 실행 중인 명령어 주소 | X |
| **Native Method Stack** | 스레드별 | 스레드 시작~종료 | Native 메서드 호출 정보 | X |

---

### Metaspace

#### Java 8의 핵심 변경사항: PermGen → Metaspace

```
Java 7 이전              Java 8 이후
┌─────────────────┐      ┌─────────────────┐
│   Heap Memory   │      │   Heap Memory   │
│  ┌───────────┐  │      │  ┌───────────┐  │
│  │  PermGen  │  │  →   │  │ (PermGen  │  │
│  │ (고정크기) │  │      │  │   제거)   │  │
│  └───────────┘  │      │  └───────────┘  │
└─────────────────┘      └─────────────────┘
                         ┌─────────────────┐
                         │ Native Memory   │
                         │  ┌───────────┐  │
                         │  │Metaspace  │  │
                         │  │(동적확장)  │  │
                         │  └───────────┘  │
                         └─────────────────┘
```

#### Java 7 이전 (PermGen)

- Heap 내부에 위치
- 고정된 크기 (`-XX:MaxPermSize=128m`)
- 클래스 메타데이터, static 변수, 상수 풀 저장
- **문제점**: `OutOfMemoryError: PermGen space` 발생 빈번

#### Java 8 이후 (Metaspace)

- Native Memory에 위치 (Heap 외부)
- 동적 확장 가능 (`-XX:MaxMetaspaceSize` 기본값: 무제한)
- OS 메모리 한도까지 자동 확장
- **장점**: PermGen 공간 부족 문제 해결, 메모리 효율성 향상

---

### Heap 메모리와 GC

#### JVM 힙 메모리 기본값 (Ergonomics)

##### 별도 설정 없을 시 기본값

JVM은 물리 메모리를 기반으로 힙 크기를 자동 설정합니다 (Java 5 이후):

```
초기 힙 (-Xms) = 물리 메모리 / 64 (최소 32MB)
최대 힙 (-Xmx) = 물리 메모리 / 4
```

**예시: 16GB RAM 인스턴스**
- 초기 힙: 256MB (16GB / 64)
- 최대 힙: 4GB (16GB / 4)

##### Java 버전별 발전 과정

| 버전 | 연도 | 주요 변화 |
|------|------|-----------|
| Java 1.0 | 1996 | 동적 할당 메커니즘 존재 (수동 설정) |
| **Java 1.4** | **2002** | **Ergonomics 도입** - Server-Class Machine Detection |
| **Java 5** | **2004** | **1/64, 1/4 규칙 정립** |
| Java 6-7 | 2006-2011 | 최대 힙 1GB 제한 완화, G1GC 도입 |
| **Java 8** | **2014** | **현대적 기본값** - 제한 제거, PermGen → Metaspace |
| Java 10+ | 2018~ | 컨테이너(Docker/K8s) 환경 인식 개선 |

##### 컨테이너 환경에서의 차이

```bash
# Java 8 이하: 호스트 메모리 인식
컨테이너 제한 2GB, 호스트 16GB
→ JVM이 16GB 인식 → 최대 힙 4GB 할당 (OOM 발생)

# Java 10 이상: 컨테이너 제한 인식
컨테이너 제한 2GB
→ JVM이 2GB 인식 → 최대 힙 512MB 할당 (올바름)
```

#### 힙 메모리 동적 확장 메커니즘

##### 기본 개념

JVM은 **동적 할당**을 지원합니다 (Java 1.0부터):
- 초기 힙(Xms)으로 시작
- 필요 시 최대 힙(Xmx)까지 자동 확장
- 여유 시 힙 축소 가능

##### 확장 메커니즘 순서도

```
1. 초기 힙 할당 (예: 256MB)
   ↓
2. 객체 생성 → 메모리 사용량 증가
   ↓
3. 힙이 거의 가득 찬 상태 (임계값 도달)
   ↓
4. GC 실행 (메모리 회수 시도)
   ↓
5. GC 후에도 여전히 부족?
   ├─ YES → 힙 확장 (예: 1GB → 1.5GB)
   └─ NO  → 계속 실행
   ↓
6. 반복 (최대 Xmx까지)
   ↓
7. Xmx 도달 후에도 부족?
   └─ OutOfMemoryError
```

##### 확장/축소 트리거 조건

**확장 조건:**
```java
if (GC 후 여유 공간 < 40%) {
    힙 확장 (현재 크기의 약 20~50% 증가)
}
```

**축소 조건:**
```java
if (GC 후 여유 공간 > 70%) {
    힙 축소 고려 (확장보다 덜 적극적)
}
```

##### 실제 동작 예시

**시나리오: 사진 업로드 처리**

```
시작: 256MB 할당
├─ 평소 사용: 500MB까지 증가
├─ GC 실행 → 300MB로 회수
└─ 다시 사용: 800MB까지 증가

사진 업로드 폭주:
├─ 메모리 사용: 950MB (힙 거의 가득)
├─ GC 실행 → 700MB로 회수
├─ 여전히 부족 → 힙 확장: 1GB → 1.5GB
├─ 계속 사용: 1.3GB
├─ GC 실행 → 1GB로 회수
├─ 여전히 부족 → 힙 확장: 1.5GB → 2GB
└─ 반복... 최대 4GB까지
```

##### 힙 확장 시 GC 영향

**문제점:**

```
힙 부족 → GC → 확장 → 또 부족 → GC → 확장...
→ GC가 너무 자주 발생
→ CPU 사용량 증가
→ 애플리케이션 일시 정지 (STW - Stop The World)
```

**Full GC 발생:**
- 힙 확장 시 Full GC 트리거
- 전체 힙 스캔
- 수백ms ~ 수초 정지
- 사용자 요청 지연

##### 모니터링 패턴 해석

**톱니 패턴 (Sawtooth Pattern):**

```
메모리 사용량
  ^
1G|     /\      /\      /\
  |    /  \    /  \    /  \
  |   /    \  /    \  /    \
500|  /      \/      \/      \
  |_________________________> 시간
```

- **증가**: 객체 생성
- **급격한 하락**: GC 실행
- **정상적인 패턴**: GC가 잘 작동 중
- **주의**: 너무 빈번하면 성능 저하

---

### Runtime 클래스로 메모리 확인

#### 주요 메서드

| 메서드 | 설명 | 반환값 |
|--------|------|--------|
| `maxMemory()` | 최대 힙 크기 (-Xmx) | bytes |
| `totalMemory()` | 현재 할당된 힙 크기 | bytes |
| `freeMemory()` | 할당된 힙 중 사용 가능한 메모리 | bytes |

**실제 사용 중인 메모리 계산:**
```
사용 중 메모리 = totalMemory() - freeMemory()
```

#### 코드 예제

```java
public class MemoryMonitor {
    public static void printMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();

        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        System.out.println("=== JVM Memory Info ===");
        System.out.println("Max Memory:   " + formatBytes(maxMemory) +
                          " (-Xmx)");
        System.out.println("Total Memory: " + formatBytes(totalMemory) +
                          " (현재 할당)");
        System.out.println("Used Memory:  " + formatBytes(usedMemory) +
                          " (실제 사용)");
        System.out.println("Free Memory:  " + formatBytes(freeMemory) +
                          " (할당 중 여유)");
        System.out.println("Usage: " +
                          (usedMemory * 100 / totalMemory) + "%");
    }

    private static String formatBytes(long bytes) {
        long mb = bytes / (1024 * 1024);
        return mb + "MB (" + bytes + " bytes)";
    }

    public static void main(String[] args) {
        // 초기 상태
        printMemoryInfo();

        // 메모리 사용 시뮬레이션
        System.out.println("\n큰 배열 생성...");
        int[] largeArray = new int[10_000_000]; // 약 40MB

        printMemoryInfo();

        // GC 강제 실행 (권장하지 않음, 테스트용)
        System.out.println("\nGC 실행...");
        System.gc();

        printMemoryInfo();
    }
}
```

**실행 결과 예시:**

```
=== JVM Memory Info ===
Max Memory:   4096MB (-Xmx)
Total Memory: 256MB (현재 할당)
Used Memory:  50MB (실제 사용)
Free Memory:  206MB (할당 중 여유)
Usage: 19%

큰 배열 생성...
=== JVM Memory Info ===
Max Memory:   4096MB (-Xmx)
Total Memory: 512MB (현재 할당)  ← 힙 확장됨
Used Memory:  90MB (실제 사용)
Free Memory:  422MB (할당 중 여유)
Usage: 17%

GC 실행...
=== JVM Memory Info ===
Max Memory:   4096MB (-Xmx)
Total Memory: 512MB (현재 할당)
Used Memory:  45MB (실제 사용)  ← GC로 회수
Free Memory:  467MB (할당 중 여유)
Usage: 8%
```

#### 실전 활용: 메모리 부족 감지

```java
public class MemoryWatcher {
    private static final double WARNING_THRESHOLD = 0.8; // 80%
    private static final double CRITICAL_THRESHOLD = 0.9; // 90%

    public static void checkMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();

        double usage = (double) usedMemory / maxMemory;

        if (usage > CRITICAL_THRESHOLD) {
            System.err.println("CRITICAL: Memory usage " +
                              (int)(usage * 100) + "%");
            // 로깅, 알림 발송 등
        } else if (usage > WARNING_THRESHOLD) {
            System.out.println("WARNING: Memory usage " +
                              (int)(usage * 100) + "%");
        }
    }
}
```

---

### 실전 활용: Elastic Beanstalk 설정

#### 인스턴스 타입별 권장 힙 크기

| 인스턴스 | RAM | 권장 힙 (-Xmx) | 초기 힙 (-Xms) |
|----------|-----|----------------|----------------|
| t3.small | 2GB | 1~1.5GB | 512MB~1GB |
| t3.medium | 4GB | 2~3GB | 1~2GB |
| m7g.large | 8GB | 4~6GB | 2~4GB |
| **m7g.xlarge** | **16GB** | **6~10GB** | **3~6GB** |

**메모리 할당 가이드:**
- 인스턴스 RAM의 50~75%를 힙에 할당
- 나머지는 OS, Metaspace, Direct Memory 등을 위해 예약

#### 설정 방법

##### 방법 1: 환경 속성 (Console)

1. Elastic Beanstalk Console → 환경 선택
2. **구성(Configuration)** → **소프트웨어(Software)**
3. **환경 속성(Environment properties)** 추가:
   ```
   이름: JAVA_TOOL_OPTIONS
   값: -Xmx6g -Xms3g -XX:+UseG1GC
   ```

##### 방법 2: `.ebextensions` 설정

```yaml
# .ebextensions/jvm-options.config
option_settings:
  aws:elasticbeanstalk:application:environment:
    JAVA_TOOL_OPTIONS: "-Xmx6g -Xms3g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

##### 방법 3: Java 플랫폼 전용 설정

```yaml
# .ebextensions/java-settings.config
option_settings:
  aws:elasticbeanstalk:container:java:
    Xmx: 6g
    Xms: 3g
```

#### 자동 설정 vs 명시적 설정 비교

| 구분 | 자동 설정 | 명시적 설정 |
|------|-----------|-------------|
| **설정** | 없음 (기본값) | `-Xmx6g -Xms3g` |
| **초기 힙** | 256MB | 3GB |
| **최대 힙** | 4GB | 6GB |
| **확장** | 필요 시 자동 | 최소화 |
| **GC 빈도** | 높음 | 낮음 |
| **성능** | 예측 불가 | 안정적 |
| **메모리 활용** | 보수적 | 적극적 |
| **적합한 경우** | 테스트, 소규모 | **프로덕션, 대용량** |

#### 사진 처리 워크로드 사례

**문제 상황:**
```
현재: 힙 1GB, 사용률 75~90%
추가: 사진 업로드/처리 기능
이미지: 10MB 사진 → 메모리 50~100MB 차지
```

**자동 확장 시 문제점:**
```
사용자 동시 업로드
→ 메모리 급증 (1GB → 2GB 필요)
→ GC 실행 → 힙 확장 → Full GC
→ 수백ms ~ 수초 정지
→ 사용자 요청 지연
```

**권장 설정 (m7g.xlarge, 16GB):**
```bash
JAVA_TOOL_OPTIONS: "-Xmx8g -Xms4g -XX:+UseG1GC"
```

**효과:**
- 시작부터 4GB 확보
- 사진 처리 시 여유 있음
- 힙 확장 최소화
- 예측 가능한 성능

#### 배포 시 주의사항

**롤링 배포 정책:**

| 정책 | 다운타임 | 용량 감소 | 환경 속성 변경 시 |
|------|----------|-----------|-------------------|
| 전체 한번에 | 있음 | 100% | 전체 재시작 |
| 롤링 | 없음 | 일시적 | 순차 재시작 |
| **추가 배치 롤링** | **없음** | **없음** | **무중단 업데이트** |
| 변경 불가능 | 없음 | 없음 | 새 인스턴스 세트 |

**추가 배치 롤링 (권장):**
1. 새 인스턴스 추가 생성 (새 JVM 설정)
2. 헬스체크 통과
3. 기존 인스턴스 순차 교체
4. 완료 후 추가 인스턴스 제거

**확인 방법:**
```bash
# 로그에서 JVM 옵션 확인
eb logs | grep "JAVA_TOOL_OPTIONS"

# 출력 예시:
# Picked up JAVA_TOOL_OPTIONS: -Xmx8g -Xms4g -XX:+UseG1GC
```

---

### Stack

```
Thread-1 Stack       Thread-2 Stack
┌──────────────┐     ┌──────────────┐
│ Stack Frame  │     │ Stack Frame  │
│ ┌──────────┐ │     │ ┌──────────┐ │
│ │지역 변수  │ │     │ │지역 변수  │ │
│ │operand   │ │     │ │operand   │ │
│ │return    │ │     │ │return    │ │
│ │  addr    │ │     │ │  addr    │ │
│ └──────────┘ │     │ └──────────┘ │
└──────────────┘     └──────────────┘
```

- 메서드 호출마다 Stack Frame 생성
- 메서드 종료 시 자동 제거 (GC 불필요)
- `-Xss` 옵션으로 크기 조정 (기본: 1MB)

---

### 메모리 핵심 요약

#### 기억할 것

1. **기본값은 자동 설정됨** (Java 5+)
   - 초기: RAM / 64
   - 최대: RAM / 4

2. **동적 확장은 Java 1.0부터 지원**
   - 메모리 부족 → GC → 확장 → 반복

3. **자동 확장의 비용**
   - GC 빈발 → 성능 저하
   - Full GC → 일시 정지

4. **프로덕션에서는 명시적 설정 권장**
   - 예측 가능한 성능
   - 메모리 집약적 워크로드 대비

5. **메모리 모니터링 필수**
   - Runtime 클래스 활용
   - CloudWatch, APM 도구 사용

#### 빠른 의사결정 가이드

```
메모리 사용률 < 50% → 현재 설정 유지
메모리 사용률 50~70% → 모니터링 강화
메모리 사용률 > 70% → 힙 크기 증설 고려
GC 빈도 증가 → 힙 크기 증설
Full GC 빈발 → GC 알고리즘 변경 또는 힙 증설
```

---

## 3. 가비지 컬렉션

### Java 8 기본 GC: Parallel GC

```
┌──────────────────────────────────────────┐
│              Heap Memory                 │
├──────────────────────┬───────────────────┤
│  Young Generation    │  Old Generation   │
│ ┌─────┬──────────┐   │                   │
│ │Eden │Survivor 0│   │   (장기 객체)      │
│ │     │Survivor 1│   │                   │
│ └─────┴──────────┘   │                   │
└──────────────────────┴───────────────────┘
```

**특징:**
- **Young Generation**: 새로 생성된 객체 저장 (Minor GC 대상)
  - Eden: 객체 최초 생성 위치
  - Survivor 0/1: Minor GC에서 살아남은 객체
- **Old Generation**: 오래 살아남은 객체 저장 (Major GC 대상)
- **고정된 세대 구조**: Young과 Old 영역이 물리적으로 분리
- **처리량 중심**: 멀티 스레드로 병렬 GC 수행
- **Stop-The-World**: GC 중 애플리케이션 일시 중지

**장단점:**
- 높은 처리량 (Throughput)
- 멀티 CPU 활용 효율적
- 긴 중지 시간 (특히 Old GC)
- 중지 시간 예측 어려움

---

### Java 9+ 기본 GC: G1 GC (Garbage First)

```
┌─────────────────────────────────────────────────────────┐
│                    Heap Memory (G1)                     │
│  ┌────┬────┬────┬────┬────┬────┬────┬────┬────┬────┐   │
│  │ E  │ E  │ S  │ O  │ O  │ H  │ E  │ S  │ O  │ H  │   │
│  ├────┼────┼────┼────┼────┼────┼────┼────┼────┼────┤   │
│  │ O  │ E  │ E  │ H  │ S  │ O  │ O  │ E  │ S  │ E  │   │
│  └────┴────┴────┴────┴────┴────┴────┴────┴────┴────┘   │
│                                                          │
│  E: Eden (Young)      S: Survivor (Young)               │
│  O: Old               H: Humongous (대형 객체, >=50% region)│
└─────────────────────────────────────────────────────────┘
```

**Region 기반 구조:**
- Heap을 동일한 크기의 **Region**으로 분할 (기본 1MB~32MB, 자동 계산)
- 각 Region은 Eden, Survivor, Old, Humongous 역할을 **동적으로 변경**
- Young/Old 구분은 논리적으로만 존재

**동작 방식:**
1. **Young GC (Evacuation Pause)**
   - Eden과 Survivor Region의 살아있는 객체를 다른 Region으로 복사
   - 빈 Region은 재사용 가능
   - 병렬 처리로 빠른 수행

2. **Mixed GC**
   - Young + Old Region 일부를 동시에 GC
   - **가비지 비율이 높은 Region 우선 수집** (Garbage First)
   - 목표 중지 시간 내에서 최대한 많은 가비지 수집

3. **Full GC (최후 수단)**
   - 모든 Region을 단일 스레드로 GC (매우 느림)
   - Concurrent Mark를 통해 최대한 회피

**특징:**
- **예측 가능한 중지 시간**: `-XX:MaxGCPauseMillis=200` (기본 200ms)
- **Concurrent Marking**: 애플리케이션과 동시에 마킹 작업
- **점진적 수집**: 전체 Heap을 한 번에 수집하지 않음
- **압축 (Compaction)**: GC 중 메모리 단편화 자동 해결

**장단점:**
- 예측 가능한 중지 시간 (목표 시간 설정 가능)
- 큰 Heap에서 효율적 (6GB 이상 권장)
- 메모리 단편화 방지
- 작은 Heap에서는 Parallel GC보다 느릴 수 있음
- CPU 오버헤드 약간 증가 (Concurrent 작업)

**Java 8 vs Java 9+ GC 비교:**

| 항목 | Java 8 (Parallel GC) | Java 9+ (G1 GC) |
|------|---------------------|-----------------|
| **기본 GC** | Parallel GC | G1 GC |
| **메모리 구조** | 고정된 Young/Old 영역 | 동적 Region 기반 |
| **목표** | 높은 처리량 (Throughput) | 예측 가능한 중지 시간 |
| **중지 시간** | 길고 예측 어려움 (수백ms~수초) | 짧고 예측 가능 (목표 시간 설정) |
| **적합한 환경** | 배치 처리, 백그라운드 작업 | 실시간 서비스, 대용량 Heap |
| **Full GC 빈도** | 높음 | 낮음 (Concurrent Mark) |
| **튜닝 복잡도** | 중간 | 낮음 (자동 튜닝) |

---

### Java 11: ZGC (Z Garbage Collector)

**개념:**
- **초저지연 GC**: 중지 시간 10ms 이하 목표
- **대용량 Heap 지원**: 수백 GB ~ 수 TB까지
- **동시성 GC**: 대부분의 작업을 애플리케이션과 동시 수행

**동작 방식:**

```
┌─────────────────────────────────────────────┐
│ Heap Memory (ZGC Region 기반)              │
│  ┌──┬──┬──┬──┬──┬──┬──┬──┬──┬──┬──┬──┐   │
│  │S │S │M │M │M │L │L │L │L │M │S │M │   │
│  └──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┘   │
│                                             │
│  S: Small (2MB)   M: Medium (32MB)         │
│  L: Large (N * 2MB, 동적 크기)             │
└─────────────────────────────────────────────┘

동작 단계:
1. Concurrent Mark (동시 마킹)
   - 애플리케이션 실행 중 마킹
   - STW 없음

2. Concurrent Prepare (준비)
   - 재배치 대상 선정
   - STW 없음

3. Concurrent Relocate (재배치)
   - 살아있는 객체 이동
   - 메모리 단편화 해결
   - STW: 1ms 미만

4. Concurrent Remap (재매핑)
   - 객체 참조 업데이트
   - STW 없음
```

**핵심 기술:**

1. **Colored Pointers (착색 포인터)**
   - 64비트 포인터의 일부 비트를 메타데이터로 사용
   - 객체 상태를 포인터에 인코딩 (Marked, Remapped 등)

2. **Load Barriers**
   - 객체 접근 시 포인터 상태 확인
   - 필요시 재매핑 (런타임 오버헤드 < 4%)

**Parallel GC vs G1 GC vs ZGC 비교:**

| 항목 | Parallel GC (Java 8) | G1 GC (Java 9+) | ZGC (Java 11+) |
|------|---------------------|-----------------|----------------|
| **중지 시간** | 수백ms ~ 수초 | 수십ms ~ 수백ms (목표 시간 설정 가능) | **10ms 미만 (일관됨)** |
| **대용량 Heap** | 비효율적 (8GB 이상) | 효율적 (수십 GB) | **매우 효율적 (수 TB)** |
| **메모리 구조** | 고정된 Young/Old | 동적 Region | 동적 Region (3 사이즈) |
| **동시성** | STW 중심 | 일부 Concurrent | **대부분 Concurrent** |
| **처리량** | 매우 높음 (100%) | 높음 (90-95%) | 중간 (85-90%, Load Barrier 오버헤드) |
| **적합한 환경** | 배치, 높은 처리량 | 일반 서버 | **실시간 서비스, 대용량 메모리** |
| **최소 Heap** | 제한 없음 | 제한 없음 | 8GB 이상 권장 |

**ZGC 사용 방법:**

```bash
# Java 11-14 (실험적)
java -XX:+UnlockExperimentalVMOptions \
     -XX:+UseZGC \
     -Xmx16g \
     MyApp

# Java 15+ (프로덕션 준비)
java -XX:+UseZGC \
     -Xmx16g \
     MyApp

# ZGC 로그 확인
java -XX:+UseZGC \
     -Xlog:gc*:gc.log \
     MyApp

# 출력 예시:
# [2.123s][info][gc] GC(10) Pause Mark Start 0.012ms
# [2.456s][info][gc] GC(10) Concurrent Mark 333.245ms
# [2.789s][info][gc] GC(10) Pause Mark End 0.008ms
# [3.012s][info][gc] GC(10) Concurrent Relocate 223.567ms
```

**ZGC 장단점:**

| 장점 | 단점 |
|------|------|
| 일관된 초저지연 (10ms 미만) | 처리량 약간 낮음 (Load Barrier 오버헤드) |
| 대용량 Heap 효율적 처리 | 메모리 사용량 증가 (메타데이터) |
| 중지 시간이 Heap 크기에 비례하지 않음 | 작은 Heap에서는 G1보다 느릴 수 있음 |
| 실시간 애플리케이션에 적합 | Java 11 이상 필요 |

---

### Java 11: Epsilon GC (No-Op Garbage Collector)

**개념:**
- **아무것도 하지 않는 GC**: 메모리 할당만 수행, 회수는 하지 않음
- 메모리가 다 차면 JVM 종료

**동작 방식:**

```
애플리케이션 실행
     ↓
Heap 메모리 할당
     ↓
객체 생성 계속...
     ↓
Heap 메모리 고갈
     ↓
OutOfMemoryError 발생
     ↓
JVM 종료
```

**사용 사례:**
- **성능 테스트**: GC 오버헤드 측정
- **초단기 실행 작업**: 배치 작업, 함수형 프로그래밍 (메모리 부족 전에 종료)
- **GC 성능 분석**: GC가 없을 때 vs 있을 때 비교

**사용 방법:**

```bash
java -XX:+UnlockExperimentalVMOptions \
     -XX:+UseEpsilonGC \
     -Xmx1g \
     MyApp

# 출력 예시:
# [0.001s][info][gc] Using Epsilon
# ...
# [10.5s][info][gc] OutOfMemoryError: Java heap space
```

**장단점:**

| 장점 | 단점 |
|------|------|
| GC 오버헤드 완전 제거 (0ms 중지 시간) | 장시간 실행 불가 (메모리 고갈) |
| 예측 가능한 성능 (GC로 인한 변동 없음) | 메모리 누수 위험 높음 |
| 벤치마크 테스트에 유용 | 프로덕션 환경에 부적합 |

---

### GC 알고리즘 통합 비교

| GC 종류 | Java 버전 | 중지 시간 | 처리량 | 대용량 Heap | 적합한 상황 |
|---------|-----------|----------|--------|-------------|-------------|
| **Serial GC** | Java 1.0+ | 긴 시간 | 낮음 | 비효율적 | 작은 애플리케이션, 단일 코어 |
| **Parallel GC** | Java 5+ (Java 8 기본) | 수백ms~수초 | **매우 높음** | 비효율적 (8GB 이상) | 배치 작업, 처리량 중시 |
| **G1GC** | Java 9+ (기본) | 수십~수백ms (예측 가능) | 높음 (90-95%) | 효율적 (수십 GB) | 범용, 대부분의 애플리케이션 |
| **ZGC** | Java 11+ | **10ms 미만** | 중간 (85-90%) | **매우 효율적 (TB급)** | 대용량 힙, 낮은 지연 요구 |
| **Shenandoah** | Java 12+ | 낮은 일시정지 | 중간 | 효율적 | 실시간성 요구 애플리케이션 |
| **Epsilon GC** | Java 11+ | **0ms** (GC 없음) | **100%** | N/A | 성능 테스트, 초단기 실행 |

---

## 4. 실행 흐름과 컴파일러

### 전체 실행 과정 (상세)

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. 개발 단계: 소스 코드 작성                                      │
│    Example.java                                                  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. 컴파일: javac 컴파일러                                         │
│    - 소스 코드(.java) → 바이트코드(.class) 변환                   │
│    - 어노테이션 프로세서 실행 (컴파일 타임 코드 생성)              │
│    Example.class (바이트코드)                                     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. JVM 시작: java Example                                        │
│    - JVM 프로세스 생성                                            │
│    - 메모리 영역 초기화 (Heap, Metaspace, Stack 등)               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. 클래스 로딩: Class Loader Subsystem                            │
│    ┌──────────────────────────────────────────────────────┐     │
│    │ 4-1. Loading (로딩)                                   │     │
│    │      - .class 파일을 메모리로 로드                     │     │
│    │      - 위임 모델: Bootstrap → Extension → Application │     │
│    └──────────────────────────────────────────────────────┘     │
│                           ↓                                      │
│    ┌──────────────────────────────────────────────────────┐     │
│    │ 4-2. Linking (링킹)                                   │     │
│    │      - Verification: 바이트코드 검증                   │     │
│    │      - Preparation: static 변수 기본값 초기화          │     │
│    │      - Resolution: 심볼릭 참조 → 실제 주소 변환        │     │
│    └──────────────────────────────────────────────────────┘     │
│                           ↓                                      │
│    ┌──────────────────────────────────────────────────────┐     │
│    │ 4-3. Initialization (초기화)                          │     │
│    │      - static 변수 실제 값 할당                        │     │
│    │      - static 블록 실행                                │     │
│    └──────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5. 실행 엔진: Execution Engine                                    │
│    ┌──────────────────────────────────────────────────────┐     │
│    │ 바이트코드 → 기계어 변환                              │     │
│    │                                                       │     │
│    │  ┌─────────────┐          ┌──────────────┐          │     │
│    │  │ 인터프리터   │          │ JIT 컴파일러  │          │     │
│    │  │             │          │              │          │     │
│    │  │ - 한줄씩 해석│ ──────→  │ - Hot Spot   │          │     │
│    │  │ - 초기 실행  │  빈번한   │   감지       │          │     │
│    │  │             │  코드     │ - 네이티브   │          │     │
│    │  │             │  발견시   │   코드 생성  │          │     │
│    │  └─────────────┘          └──────────────┘          │     │
│    └──────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 6. 런타임 실행                                                    │
│    ┌──────────────────────────────────────────────────────┐     │
│    │ 메서드 호출                                           │     │
│    │  ↓                                                    │     │
│    │ Stack에 Frame 생성                                    │     │
│    │  - 지역 변수 할당                                      │     │
│    │  - Operand Stack (연산 스택)                          │     │
│    │  - Return Address                                     │     │
│    └──────────────────────────────────────────────────────┘     │
│                           ↓                                      │
│    ┌──────────────────────────────────────────────────────┐     │
│    │ 객체 생성 (new 키워드)                                │     │
│    │  ↓                                                    │     │
│    │ Heap의 Eden 영역에 메모리 할당                         │     │
│    │  - 생성자 실행                                         │     │
│    │  - 참조값을 Stack의 지역 변수에 저장                   │     │
│    └──────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 7. 가비지 컬렉션 (GC)                                             │
│    ┌──────────────────────────────────────────────────────┐     │
│    │ Minor GC (Young Generation)                           │     │
│    │  - Eden 영역 가득 차면 실행                            │     │
│    │  - 살아있는 객체 → Survivor로 이동                     │     │
│    │  - 일정 횟수 생존 → Old Generation으로 이동 (Promotion)│     │
│    └──────────────────────────────────────────────────────┘     │
│                           ↓                                      │
│    ┌──────────────────────────────────────────────────────┐     │
│    │ Major GC (Old Generation)                             │     │
│    │  - Old 영역 가득 차면 실행                             │     │
│    │  - STW (Stop-The-World) 발생                          │     │
│    │  - 애플리케이션 일시 중지                              │     │
│    └──────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────┘
```

### 메서드 호출 예시 (Stack Frame 생성 과정)

```java
public class Example {
    public static void main(String[] args) {  // Frame 1
        int result = calculate(5);             // Frame 2 생성 예정
        System.out.println(result);
    }

    public static int calculate(int num) {     // Frame 2
        int temp = num * 2;                    // Frame 2의 지역 변수
        return temp;
    }
}
```

**Stack 상태 변화:**

```
1. main() 호출 시
┌──────────────┐
│  Frame 1     │
│  main()      │
│ ─────────    │
│ args: [...]  │
└──────────────┘

2. calculate(5) 호출 시
┌──────────────┐
│  Frame 2     │ ← 현재 실행
│ calculate()  │
│ ─────────    │
│ num: 5       │
│ temp: 10     │
├──────────────┤
│  Frame 1     │
│  main()      │
│ ─────────    │
│ args: [...]  │
│ result: ?    │
└──────────────┘

3. calculate() 종료 후
┌──────────────┐
│  Frame 1     │
│  main()      │
│ ─────────    │
│ args: [...]  │
│ result: 10   │ ← 반환값 저장
└──────────────┘
```

### 실행 엔진 비교

#### Java 8: 인터프리터 + JIT 컴파일러

**동작 흐름:**

```
바이트코드 실행
     ↓
┌─────────────────┐
│  인터프리터      │ ← 초기 실행 (즉시 시작)
└─────────────────┘
     ↓
  Hot Spot 감지
  (메서드 호출 횟수 카운팅)
     ↓
┌─────────────────┐
│ JIT C1 컴파일러  │ ← Client 컴파일러 (빠른 컴파일)
│ (Client)        │   - 기본 최적화
└─────────────────┘   - 적은 컴파일 시간
     ↓
  더 많은 호출 감지
     ↓
┌─────────────────┐
│ JIT C2 컴파일러  │ ← Server 컴파일러 (고도 최적화)
│ (Server)        │   - 인라이닝, 루프 최적화
└─────────────────┘   - 긴 컴파일 시간
     ↓
  네이티브 코드 캐싱
  (Code Cache에 저장)
```

**비교표 (Java 8):**

| 방식 | 동작 원리 | 장점 | 단점 | 사용 시점 | Hot Spot 임계값 |
|------|----------|------|------|---------|----------------|
| **인터프리터** | 바이트코드를 한 줄씩 읽어 즉시 실행 | 즉시 실행 가능, 메모리 효율적 | 느린 실행 속도 (10-100배 느림) | 프로그램 초기 실행, 한 번만 실행되는 코드 | - |
| **JIT C1** | Hot Spot 코드를 네이티브 코드로 컴파일 | 빠른 실행 속도, 짧은 컴파일 시간 | C2보다 최적화 부족 | 반복 실행되는 코드 | 1,500회 호출 |
| **JIT C2** | 고도로 최적화된 네이티브 코드 생성 | 매우 빠른 실행 속도 | 긴 컴파일 시간 (워밍업) | 매우 자주 실행되는 코드 | 10,000회 호출 |

**JIT 컴파일 최적화 기법:**
- **인라이닝 (Inlining)**: 메서드 호출을 메서드 본문으로 치환
- **루프 최적화**: 루프 펼치기, 루프 병합
- **Escape Analysis**: 객체가 메서드 외부로 탈출하지 않으면 Stack 할당
- **Dead Code Elimination**: 실행되지 않는 코드 제거

---

#### Java 9+: AOT (Ahead-of-Time) 컴파일러 추가

**AOT 컴파일러 도입 배경:**
- **문제**: JIT는 런타임에 컴파일하므로 워밍업 시간 필요
- **필요성**: 클라우드 네이티브 환경에서 빠른 시작 시간 요구
- **해결**: 실행 전에 미리 네이티브 코드로 컴파일

**AOT 컴파일 흐름:**

```
┌─────────────────────────────────────┐
│ 1. 컴파일 타임 (개발 단계)           │
│    jaotc 도구 사용                   │
└─────────────────────────────────────┘
         ↓
    .class 파일
         ↓
┌─────────────────────────────────────┐
│ $ jaotc --output mylib.so \         │
│         --module java.base          │
└─────────────────────────────────────┘
         ↓
    .so 파일 (네이티브 코드)
         ↓
┌─────────────────────────────────────┐
│ 2. 런타임 (실행 시)                  │
│    $ java -XX:AOTLibrary=./mylib.so │
│           MyApp                     │
└─────────────────────────────────────┘
         ↓
    즉시 네이티브 코드 실행
    (JIT 워밍업 불필요)
```

**Java 8 vs Java 9+ 실행 엔진 비교:**

| 항목 | Java 8 | Java 9+ | 차이점 |
|------|--------|---------|--------|
| **컴파일 방식** | JIT (런타임) | JIT + AOT (선택 가능) | AOT는 사전 컴파일 |
| **시작 시간** | 느림 (워밍업 필요) | 빠름 (AOT 사용 시) | 워밍업 시간 제거 |
| **Peak 성능** | 매우 높음 (C2 최적화) | JIT와 동일 | AOT는 런타임 정보 없어 최적화 제한 |
| **메모리 사용** | Code Cache 필요 | .so 파일 로드 | AOT는 메모리 절약 |
| **적합한 환경** | 장시간 실행 서버 | 짧은 실행 (Lambda, CLI) | 사용 패턴에 따라 선택 |
| **프로파일링** | 런타임 프로파일링 가능 | 사전 컴파일로 제한적 | JIT가 더 정확한 최적화 |

**AOT 사용 예시:**

```bash
# 1. Java 모듈을 AOT 컴파일
jaotc --output java.base.so --module java.base

# 2. 애플리케이션 실행 (AOT 라이브러리 사용)
java -XX:AOTLibrary=./java.base.so MyApp

# 3. 성능 측정
# - 시작 시간: 30-70% 단축
# - Peak 성능: JIT보다 5-10% 낮음 (런타임 최적화 부족)
```

**AOT의 장단점:**

| 장점 | 단점 |
|------|------|
| 빠른 시작 시간 (워밍업 불필요) | Peak 성능이 JIT보다 낮음 (런타임 정보 부족) |
| 예측 가능한 성능 (사전 컴파일) | 플랫폼 종속적 (.so 파일은 OS별로 다름) |
| 메모리 효율적 (Code Cache 불필요) | 컴파일 파일 크기 증가 |
| 보안 향상 (디컴파일 어려움) | 동적 클래스 로딩 지원 제한 |

**적합한 사용 사례:**
- **AOT 추천**: 서버리스 (AWS Lambda), CLI 도구, 마이크로서비스 (빠른 스케일링)
- **JIT 추천**: 장시간 실행 서버, 높은 처리량 필요 애플리케이션

---

#### Java 17+: AOT/Graal JIT 제거 및 GraalVM Native Image로의 전환

**변경 이유:**
- Java 9-16의 `jaotc` 기반 AOT는 제한적인 성능 향상
- 플랫폼 종속적이고 유지보수 비용 증가
- **GraalVM Native Image**가 더 우수한 대안으로 부상

**Java AOT vs GraalVM Native Image 비교:**

| 항목 | Java AOT (Java 9-16) | GraalVM Native Image (Java 17+) |
|------|---------------------|----------------------------------|
| **컴파일 범위** | 일부 모듈만 AOT 컴파일 | 전체 애플리케이션을 네이티브 바이너리로 컴파일 |
| **런타임** | 여전히 JVM 필요 | **JVM 불필요 (독립 실행 파일)** |
| **시작 시간** | 30-70% 단축 | **90% 이상 단축 (밀리초 단위)** |
| **메모리 사용** | JVM 오버헤드 존재 | **매우 낮음 (1/10 수준)** |
| **Peak 성능** | JIT보다 5-10% 낮음 | JIT보다 10-30% 낮음 (트레이드오프) |
| **배포 크기** | JRE 포함 필요 (수십 MB) | **단일 바이너리 (수 MB)** |
| **플랫폼 지원** | JVM 지원 플랫폼 | OS별 네이티브 바이너리 빌드 |
| **리플렉션 지원** | 완전 지원 | 제한적 (사전 등록 필요) |

**GraalVM Native Image 동작 방식:**

```
┌─────────────────────────────────────────┐
│ 1. Java 애플리케이션 코드               │
│    MyApp.java + 의존성 라이브러리       │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│ 2. AOT 컴파일 (native-image 도구)       │
│    - 정적 분석 (Closed World Assumption)│
│    - 사용되는 모든 클래스/메서드 탐색    │
│    - 리플렉션/동적 프록시 사전 등록      │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│ 3. 네이티브 코드 생성                    │
│    - Substrate VM (경량 VM) 포함        │
│    - GC 포함 (Serial GC 또는 G1 GC)     │
│    - 모든 클래스를 네이티브 코드로 변환  │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│ 4. 네이티브 바이너리 생성                │
│    myapp (실행 파일)                    │
│    - JVM 불필요                         │
│    - OS 직접 실행                       │
└─────────────────────────────────────────┘
```

**GraalVM Native Image 사용 예시:**

```bash
# 1. GraalVM 설치
sdk install java 21-graal

# 2. Native Image 빌드
native-image -jar myapp.jar myapp

# 빌드 옵션:
# --no-fallback : 런타임 컴파일 완전 비활성화
# -H:+ReportExceptionStackTraces : 빌드 오류 상세 출력
# --initialize-at-build-time=<class> : 빌드 타임에 초기화

# 3. 실행 (JVM 불필요!)
./myapp

# 성능 측정:
# 시작 시간: 0.005s (기존 JVM: 0.5s, 100배 빠름)
# 메모리 사용: 10MB (기존 JVM: 100MB, 1/10 수준)
```

**Spring Boot + GraalVM Native Image:**

```java
// Spring Boot 3.0+ 기본 지원
// pom.xml
<plugin>
    <groupId>org.graalvm.buildtools</groupId>
    <artifactId>native-maven-plugin</artifactId>
</plugin>

// 빌드
mvn -Pnative native:compile

// 결과:
// - 시작 시간: 0.05초 (기존 2-3초 → 50배 빠름)
// - 메모리: 20MB (기존 200MB → 1/10)
// - 컨테이너 이미지: 50MB (기존 200MB → 1/4)
```

**GraalVM Native Image 제약사항 및 해결책:**

| 제약사항 | 이유 | 해결책 |
|---------|------|--------|
| **리플렉션 사전 등록** | 정적 분석으로 리플렉션 대상 파악 불가 | `reflect-config.json` 작성 또는 자동 생성 |
| **동적 프록시 제한** | 런타임 클래스 생성 불가 | `proxy-config.json` 작성 |
| **JNI 호출** | 네이티브 코드 호출 사전 등록 필요 | `jni-config.json` 작성 |
| **클래스 패스 스캐닝** | 런타임 클래스 로딩 불가 | 빌드 타임에 모든 클래스 포함 |

**리플렉션 설정 예시:**

```json
// reflect-config.json
[
  {
    "name": "com.example.MyClass",
    "allDeclaredMethods": true,
    "allDeclaredFields": true,
    "allDeclaredConstructors": true
  }
]
```

**자동 설정 생성 (Agent 사용):**

```bash
# 1. Agent로 애플리케이션 실행 (메타데이터 수집)
java -agentlib:native-image-agent=config-output-dir=META-INF/native-image \
     -jar myapp.jar

# 2. 수집된 설정 파일로 Native Image 빌드
native-image -jar myapp.jar
```

**언제 GraalVM Native Image를 사용할까?**

추천하는 경우:
- 서버리스 환경 (AWS Lambda, Google Cloud Functions)
- 컨테이너 기반 마이크로서비스 (빠른 스케일링)
- CLI 도구 (즉각적인 실행)
- IoT 디바이스 (제한된 리소스)
- 클라우드 비용 절감 (낮은 메모리 사용)

권장하지 않는 경우:
- 리플렉션/동적 프록시를 많이 사용하는 레거시 애플리케이션
- 최고 성능(Peak Performance)이 중요한 장시간 실행 서버
- 빌드 시간이 매우 중요한 경우 (Native Image 빌드는 느림, 수 분 소요)

**컴파일러 방식별 최종 비교:**

| 환경 | 권장 방식 |
|------|---------|
| 전통적인 서버 애플리케이션 | **JIT (JVM)** |
| 서버리스, 마이크로서비스 | **GraalVM Native Image** |
| 개발/테스트 환경 | **JIT (빠른 빌드)** |
| 프로덕션 (클라우드) | **GraalVM Native Image (비용 절감)** |

---

## 5. 클래스 로더와 모듈 시스템

### 클래스 로더 계층 구조

클래스 로더는 `.class` 파일을 JVM 메모리에 로딩하는 역할을 수행합니다.

#### Java 8: JAR 기반 클래스 로딩

```
┌─────────────────────────────────────┐
│  Bootstrap ClassLoader              │
│  (Native 코드, null로 표현)         │
│  - rt.jar (java.lang.*, ...)        │
│  - 핵심 JDK 클래스 로딩              │
└─────────────────────────────────────┘
             ↓ 부모
┌─────────────────────────────────────┐
│  Extension ClassLoader              │
│  (sun.misc.Launcher$ExtClassLoader) │
│  - jre/lib/ext/*.jar                │
│  - 확장 라이브러리 로딩              │
└─────────────────────────────────────┘
             ↓ 부모
┌─────────────────────────────────────┐
│  Application ClassLoader            │
│  (sun.misc.Launcher$AppClassLoader) │
│  - classpath의 애플리케이션 클래스   │
│  - 사용자 작성 클래스 로딩           │
└─────────────────────────────────────┘
             ↓ 부모 (optional)
┌─────────────────────────────────────┐
│  Custom ClassLoader (사용자 정의)   │
│  - 특수 로딩 로직 (암호화, 네트워크) │
└─────────────────────────────────────┘
```

**Java 8 클래스 로더 특징:**
- **단일 거대 JAR**: rt.jar에 모든 핵심 클래스 포함 (60MB+)
- **전역 classpath**: 모든 클래스가 동일한 classpath 공유
- **캡슐화 부족**: internal API 접근 가능 (`sun.*`, `com.sun.*`)
- **의존성 충돌**: JAR Hell 문제 (같은 클래스 다른 버전)

#### 클래스 로더 종류 (Java 8)

| 클래스 로더 | 로딩 대상 | 구현 언어 | 확인 방법 |
|-----------|---------|---------|----------|
| **Bootstrap** | JDK 핵심 라이브러리 `rt.jar` (java.lang.*, java.util.*) | Native 코드 (C/C++) | `String.class.getClassLoader()` → null |
| **Extension** | 확장 라이브러리 `jre/lib/ext/*.jar` | Java | `DNSNameService.class.getClassLoader()` |
| **Application** | 애플리케이션 클래스 `classpath`의 모든 클래스 | Java | `MyClass.class.getClassLoader()` |
| **Custom** | 사용자 정의 로딩 로직 | Java | 직접 구현 (`extends ClassLoader`) |

**클래스 로더 확인 코드 (Java 8):**

```java
public class ClassLoaderTest {
    public static void main(String[] args) {
        // Bootstrap ClassLoader (null 반환)
        System.out.println("String: " + String.class.getClassLoader());
        // 출력: String: null

        // Extension ClassLoader (Java 8)
        System.out.println("DNSNameService: " +
            com.sun.jndi.dns.DnsClient.class.getClassLoader());
        // 출력: sun.misc.Launcher$ExtClassLoader@...

        // Application ClassLoader
        System.out.println("MyClass: " + ClassLoaderTest.class.getClassLoader());
        // 출력: sun.misc.Launcher$AppClassLoader@...

        // 부모 확인
        ClassLoader appCL = ClassLoaderTest.class.getClassLoader();
        System.out.println("Parent: " + appCL.getParent());
        // 출력: sun.misc.Launcher$ExtClassLoader@...
        System.out.println("Parent's Parent: " + appCL.getParent().getParent());
        // 출력: null (Bootstrap)
    }
}
```

---

#### Java 9+: 모듈 시스템 (JPMS) 기반 클래스 로딩

```
┌─────────────────────────────────────────────────────────┐
│  Bootstrap ClassLoader (BootLoader)                     │
│  (Native 코드, null로 표현)                             │
│  ┌─────────────────────────────────────────────────┐   │
│  │ 핵심 모듈 (rt.jar 제거, 모듈로 분리)             │   │
│  │  - java.base (필수, 자동 로드)                   │   │
│  │  - java.logging, java.xml, java.prefs           │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
             ↓ 부모
┌─────────────────────────────────────────────────────────┐
│  Platform ClassLoader (PlatformClassLoader)             │
│  (jdk.internal.loader.ClassLoaders$PlatformClassLoader) │
│  ┌─────────────────────────────────────────────────┐   │
│  │ Java SE 플랫폼 모듈 (Extension 대체)             │   │
│  │  - java.se (Java SE API)                        │   │
│  │  - java.sql, java.naming, java.management       │   │
│  │  - java.desktop, java.compiler                  │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
             ↓ 부모
┌─────────────────────────────────────────────────────────┐
│  Application ClassLoader (AppClassLoader)               │
│  (jdk.internal.loader.ClassLoaders$AppClassLoader)      │
│  ┌─────────────────────────────────────────────────┐   │
│  │ 애플리케이션 모듈 + classpath                    │   │
│  │  - 사용자 정의 모듈 (module-path)                │   │
│  │  - classpath의 클래스 (하위 호환)                │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
             ↓ 부모 (optional)
┌─────────────────────────────────────────────────────────┐
│  Custom ClassLoader (사용자 정의)                       │
│  - 동일 (모듈 시스템과 호환)                             │
└─────────────────────────────────────────────────────────┘
```

**Java 9+ 클래스 로더 변경사항:**

1. **Extension ClassLoader → Platform ClassLoader**
   - 이름 변경: `ExtClassLoader` → `PlatformClassLoader`
   - 역할 확대: Java SE 플랫폼 모듈 로딩
   - `jre/lib/ext` 디렉토리 제거

2. **rt.jar 제거, 모듈로 분리**
   - 단일 거대 JAR 제거
   - 90개 이상의 모듈로 분리 (`jmods/` 디렉토리)
   - 필요한 모듈만 선택적 로드 (경량화)

3. **모듈 경로 (module-path) 도입**
   - classpath와 별도로 module-path 존재
   - `--module-path` 또는 `-p` 옵션으로 지정

4. **강력한 캡슐화**
   - internal API 접근 차단 (`sun.*`, `jdk.internal.*`)
   - `--add-exports`, `--add-opens`로만 접근 가능

**Java 8 vs Java 9+ 클래스 로더 비교:**

| 항목 | Java 8 | Java 9+ | 변경 이유 |
|------|--------|---------|----------|
| **핵심 클래스 로더** | Bootstrap / Extension / Application | Bootstrap / Platform / Application | 모듈 시스템 지원 |
| **Extension CL 이름** | `sun.misc.Launcher$ExtClassLoader` | `jdk.internal.loader.ClassLoaders$PlatformClassLoader` | 역할 변경 (확장→플랫폼) |
| **Application CL 이름** | `sun.misc.Launcher$AppClassLoader` | `jdk.internal.loader.ClassLoaders$AppClassLoader` | 모듈 경로 지원 |
| **핵심 라이브러리** | rt.jar (단일 거대 JAR) | 모듈로 분리 (java.base 등) | 경량화, 선택적 로드 |
| **확장 라이브러리** | jre/lib/ext/*.jar | Platform 모듈 (java.se 등) | 모듈 시스템 통합 |
| **클래스 경로** | classpath만 존재 | classpath + module-path | 모듈과 JAR 공존 |
| **캡슐화** | 약함 (internal API 접근 가능) | 강함 (모듈 경계 강제) | 보안, API 안정성 |
| **로딩 방식** | JAR 전체 스캔 | 모듈 디스크립터 기반 | 성능 향상 |

**모듈 시스템 예시:**

```java
// module-info.java (모듈 디스크립터)
module com.example.myapp {
    requires java.base;      // 자동 포함 (명시 불필요)
    requires java.sql;       // Platform CL이 로드
    requires java.logging;   // Platform CL이 로드

    exports com.example.api; // 외부에 공개
    // com.example.internal은 캡슐화 (외부 접근 불가)
}
```

**클래스 로더 확인 코드 (Java 9+):**

```java
public class ModuleClassLoaderTest {
    public static void main(String[] args) {
        // Bootstrap ClassLoader (여전히 null)
        System.out.println("String: " + String.class.getClassLoader());
        // 출력: null

        // Platform ClassLoader (java.sql 모듈)
        System.out.println("java.sql.Connection: " +
            java.sql.Connection.class.getClassLoader());
        // 출력: jdk.internal.loader.ClassLoaders$PlatformClassLoader@...

        // Application ClassLoader
        System.out.println("MyClass: " + ModuleClassLoaderTest.class.getClassLoader());
        // 출력: jdk.internal.loader.ClassLoaders$AppClassLoader@...

        // 모듈 정보 확인
        Module stringModule = String.class.getModule();
        System.out.println("String 모듈: " + stringModule.getName());
        // 출력: java.base

        System.out.println("java.base는 모든 모듈에 자동 포함: " +
            stringModule.getDescriptor().requires());
    }
}
```

**주요 모듈 목록:**

```bash
# Java 9+ 모듈 확인
java --list-modules

# 핵심 모듈 (Bootstrap CL)
java.base                  # 필수 모듈 (자동 로드)
java.logging
java.xml
java.prefs

# 플랫폼 모듈 (Platform CL)
java.se                    # Java SE 전체 API
java.sql
java.naming
java.management
java.desktop
java.compiler

# JDK 전용 모듈
jdk.compiler               # javac 컴파일러
jdk.jshell                 # JShell REPL
jdk.httpserver             # 간단한 HTTP 서버
```

---

### 클래스 로딩 과정 (3단계)

```
┌──────────────────────────────────────────────────────┐
│ 1. Loading (로딩)                                     │
│    - 클래스 로더가 .class 파일을 찾아 바이트로 읽음    │
│    - Method Area(Metaspace)에 클래스 정보 저장        │
│    - Heap에 Class 객체 생성 (java.lang.Class)         │
│                                                       │
│    위임 모델 (Delegation Model):                      │
│    ┌───────────────────────────────────────┐         │
│    │ 1. Application CL이 요청 받음         │         │
│    │ 2. Extension CL에 위임                │         │
│    │ 3. Bootstrap CL에 위임                │         │
│    │ 4. Bootstrap이 로드 실패 → Ext 시도   │         │
│    │ 5. Extension도 실패 → App 시도        │         │
│    │ 6. 모두 실패 → ClassNotFoundException │         │
│    └───────────────────────────────────────┘         │
└──────────────────────────────────────────────────────┘
                       ↓
┌──────────────────────────────────────────────────────┐
│ 2. Linking (링킹)                                     │
│    ┌──────────────────────────────────────┐          │
│    │ 2-1. Verification (검증)             │          │
│    │      - 바이트코드 형식 검사           │          │
│    │      - 타입 안전성 검사               │          │
│    │      - final 클래스 상속 여부 확인    │          │
│    └──────────────────────────────────────┘          │
│                    ↓                                  │
│    ┌──────────────────────────────────────┐          │
│    │ 2-2. Preparation (준비)              │          │
│    │      - static 변수 메모리 할당        │          │
│    │      - 기본값으로 초기화              │          │
│    │        int → 0, boolean → false, ... │          │
│    └──────────────────────────────────────┘          │
│                    ↓                                  │
│    ┌──────────────────────────────────────┐          │
│    │ 2-3. Resolution (해석) - Optional    │          │
│    │      - 심볼릭 참조→실제 메모리 주소   │          │
│    │      - 클래스, 필드, 메서드 참조 확정 │          │
│    │      - Lazy Resolution (필요시 수행)  │          │
│    └──────────────────────────────────────┘          │
└──────────────────────────────────────────────────────┘
                       ↓
┌──────────────────────────────────────────────────────┐
│ 3. Initialization (초기화)                            │
│    - static 변수에 실제 값 할당                        │
│    - static 블록 실행 (위에서 아래로 순차 실행)        │
│    - <clinit> 메서드 호출                             │
│    - Thread-safe 보장 (동시 초기화 방지)              │
└──────────────────────────────────────────────────────┘
```

#### 클래스 로딩 예시 코드

```java
public class Example {
    static int count = 10;       // Preparation: 0 → Initialization: 10
    static String name;          // Preparation: null (그대로 유지)

    static {
        System.out.println("Static block 1");  // Initialization 시 실행
        count = 20;
    }

    static {
        System.out.println("Static block 2");  // 순차 실행
        name = "Example";
    }

    public static void main(String[] args) {
        System.out.println(count);  // 출력: 20
        System.out.println(name);   // 출력: Example
    }
}

// 실행 결과:
// Static block 1
// Static block 2
// 20
// Example
```

---

## 6. 어노테이션 프로세서

어노테이션 프로세서는 **컴파일 타임**에 어노테이션을 분석하고 코드를 생성하거나 검증하는 도구입니다.

### 처리 시점 비교

| 시점 | 도구 | 예시 | 특징 | 성능 영향 |
|------|------|------|------|---------|
| **컴파일 타임** | Annotation Processor | Lombok, QueryDSL, AutoValue | 소스 코드 생성, 컴파일 오류 검출 | 런타임 오버헤드 없음 |
| **런타임** | Reflection API | Spring AOP, JPA, Jackson | 동적 프록시, 메타데이터 읽기 | 리플렉션 오버헤드 있음 |

### 주요 사용 사례

**1. 코드 생성 (Boilerplate 제거)**

```java
// Lombok 사용 전 (Java 8 기준)
public class User {
    private Long id;
    private String name;
    private String email;

    public User() {}

    public User(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) &&
               Objects.equals(name, user.name) &&
               Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, email);
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', email='" + email + "'}";
    }
}
```

```java
// Lombok 사용 후 (컴파일 타임에 위 코드가 자동 생성됨)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class User {
    private Long id;
    private String name;
    private String email;
}
```

**2. QueryDSL - 타입 안전 쿼리 생성**

```java
// JPA 엔티티
@Entity
public class Product {
    @Id private Long id;
    private String name;
    private int price;
}

// 컴파일 타임에 QProduct.java 자동 생성
// 생성된 코드 (일부):
public class QProduct extends EntityPathBase<Product> {
    public final NumberPath<Long> id = createNumber("id", Long.class);
    public final StringPath name = createString("name");
    public final NumberPath<Integer> price = createNumber("price", Integer.class);
}

// 타입 안전 쿼리 사용
QProduct product = QProduct.product;
List<Product> result = queryFactory
    .selectFrom(product)
    .where(product.price.gt(10000))
    .fetch();
```

**3. 컴파일 타임 검증**

```java
// AutoValue - 불변 객체 생성 및 검증
@AutoValue
public abstract class Money {
    public abstract String currency();
    public abstract long amount();

    public static Money create(String currency, long amount) {
        if (amount < 0) {
            // 컴파일 타임에 오류 검출
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        return new AutoValue_Money(currency, amount);
    }
}
```

### 동작 원리 (Java 8 Annotation Processing API)

```
┌───────────────────────────────────────────────────┐
│ 1. 소스 코드 작성                                  │
│    User.java (@Getter @Setter 어노테이션 포함)    │
└───────────────────────────────────────────────────┘
                     ↓
┌───────────────────────────────────────────────────┐
│ 2. javac 컴파일러 실행                             │
│    javac -processor LombokProcessor User.java    │
└───────────────────────────────────────────────────┘
                     ↓
┌───────────────────────────────────────────────────┐
│ 3. Annotation Processor 초기화                    │
│    - javax.annotation.processing.Processor 로드   │
│    - META-INF/services에 등록된 프로세서 탐색      │
└───────────────────────────────────────────────────┘
                     ↓
┌───────────────────────────────────────────────────┐
│ 4. AST (Abstract Syntax Tree) 생성                │
│    - 소스 코드를 트리 구조로 파싱                  │
│    - 컴파일러 내부 표현으로 변환                   │
└───────────────────────────────────────────────────┘
                     ↓
┌───────────────────────────────────────────────────┐
│ 5. Annotation Processing Rounds                   │
│    ┌───────────────────────────────────┐          │
│    │ Round 1:                          │          │
│    │  - @Getter, @Setter 어노테이션 발견│          │
│    │  - getter/setter 메서드 코드 생성  │          │
│    │  - 새로운 소스 파일/바이트코드 생성│          │
│    └───────────────────────────────────┘          │
│                   ↓                                │
│    ┌───────────────────────────────────┐          │
│    │ Round 2:                          │          │
│    │  - 새로 생성된 파일 검사           │          │
│    │  - 추가 어노테이션 없으면 종료     │          │
│    └───────────────────────────────────┘          │
└───────────────────────────────────────────────────┘
                     ↓
┌───────────────────────────────────────────────────┐
│ 6. 최종 컴파일                                     │
│    - 생성된 코드 포함하여 .class 파일 생성         │
│    User.class (getter/setter 메서드 포함)         │
└───────────────────────────────────────────────────┘
```

### Annotation Processor vs Reflection

```java
// Annotation Processor (컴파일 타임)
@Retention(RetentionPolicy.SOURCE)  // .class 파일에 포함되지 않음
public @interface Getter {}

// 컴파일 후:
// - Getter 어노테이션은 제거됨
// - 생성된 getter 메서드만 .class에 존재
// - 런타임 오버헤드 없음
```

```java
// Reflection (런타임)
@Retention(RetentionPolicy.RUNTIME)  // .class 파일에 포함
public @interface Entity {}

// 런타임에:
if (User.class.isAnnotationPresent(Entity.class)) {
    // 리플렉션으로 메타데이터 읽기
    Entity entity = User.class.getAnnotation(Entity.class);
    // 동적 처리 (프록시 생성, 테이블 매핑 등)
}
// - 런타임 오버헤드 발생
// - 동적 처리 가능
```

**비교 표:**

| 특성 | Annotation Processor | Reflection |
|------|---------------------|------------|
| **실행 시점** | 컴파일 타임 | 런타임 |
| **Retention** | SOURCE, CLASS | RUNTIME |
| **성능** | 런타임 오버헤드 없음 | 리플렉션 비용 발생 |
| **타입 안전성** | 컴파일 타임 검증 | 런타임 오류 가능 |
| **코드 생성** | 가능 | 불가능 (프록시만 가능) |
| **사용 예** | Lombok, QueryDSL | Spring, JPA, Jackson |

---

## 참고 자료

- [Oracle Java 8 JVM Specification](https://docs.oracle.com/javase/specs/jvms/se8/html/)
- [Oracle Java SE Documentation - Ergonomics](https://docs.oracle.com/en/java/javase/21/gctuning/ergonomics.html)
- [From PermGen to Metaspace](https://www.baeldung.com/java-permgen-metaspace)
- [G1 GC Overview](https://www.oracle.com/technical-resources/articles/java/g1gc.html)
- [G1GC Tuning Guide](https://docs.oracle.com/en/java/javase/21/gctuning/garbage-first-g1-garbage-collector1.html)
- [ZGC Documentation](https://wiki.openjdk.org/display/zgc/Main)
- [JIT Compiler Overview](https://docs.oracle.com/en/java/javase/11/vm/java-virtual-machine-technology-overview.html)
- [GraalVM Native Image](https://www.graalvm.org/latest/reference-manual/native-image/)
- [Java Platform Module System](https://openjdk.org/projects/jigsaw/)
- [Java Class Loading Mechanism](https://docs.oracle.com/javase/8/docs/technotes/tools/findingclasses.html)
- [Annotation Processing in Java](https://docs.oracle.com/javase/8/docs/api/javax/annotation/processing/package-summary.html)
- [AWS Elastic Beanstalk - Java SE Platform](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/java-se-platform.html)

---

*마지막 업데이트: 2026년 02월*
