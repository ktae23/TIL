# JVM Memory와 GC

JVM의 힙 메모리 동적 할당 메커니즘과 가비지 컬렉션(GC)의 동작 원리를 정리합니다.

## 목차

- [JVM 힙 메모리 기본값 (Ergonomics)](#jvm-힙-메모리-기본값-ergonomics)
- [힙 메모리 동적 확장 메커니즘](#힙-메모리-동적-확장-메커니즘)
- [GC와 메모리 관리](#gc와-메모리-관리)
- [Runtime 클래스로 메모리 확인](#runtime-클래스로-메모리-확인)
- [실전 활용: Elastic Beanstalk 설정](#실전-활용-elastic-beanstalk-설정)

## JVM 힙 메모리 기본값 (Ergonomics)

### 별도 설정 없을 시 기본값

JVM은 물리 메모리를 기반으로 힙 크기를 자동 설정합니다 (Java 5 이후):

```
초기 힙 (-Xms) = 물리 메모리 / 64 (최소 32MB)
최대 힙 (-Xmx) = 물리 메모리 / 4
```

**예시: 16GB RAM 인스턴스**
- 초기 힙: 256MB (16GB ÷ 64)
- 최대 힙: 4GB (16GB ÷ 4)

### Java 버전별 발전 과정

| 버전 | 연도 | 주요 변화 |
|------|------|-----------|
| Java 1.0 | 1996 | 동적 할당 메커니즘 존재 (수동 설정) |
| **Java 1.4** | **2002** | **Ergonomics 도입** - Server-Class Machine Detection |
| **Java 5** | **2004** | **1/64, 1/4 규칙 정립** |
| Java 6-7 | 2006-2011 | 최대 힙 1GB 제한 완화, G1GC 도입 |
| **Java 8** | **2014** | **현대적 기본값** - 제한 제거, PermGen → Metaspace |
| Java 10+ | 2018~ | 컨테이너(Docker/K8s) 환경 인식 개선 |

### 컨테이너 환경에서의 차이

```bash
# Java 8 이하: 호스트 메모리 인식
컨테이너 제한 2GB, 호스트 16GB
→ JVM이 16GB 인식 → 최대 힙 4GB 할당 (❌ OOM 발생)

# Java 10 이상: 컨테이너 제한 인식
컨테이너 제한 2GB
→ JVM이 2GB 인식 → 최대 힙 512MB 할당 (✅ 올바름)
```

## 힙 메모리 동적 확장 메커니즘

### 기본 개념

JVM은 **동적 할당**을 지원합니다 (Java 1.0부터):
- 초기 힙(Xms)으로 시작
- 필요 시 최대 힙(Xmx)까지 자동 확장
- 여유 시 힙 축소 가능

### 확장 메커니즘 순서도

```
1. 초기 힙 할당 (예: 256MB)
   ↓
2. 객체 생성 → 메모리 사용량 증가
   ↓
3. 힙이 거의 가득 찬 상태 (임계값 도달)
   ↓
4. 🔄 GC 실행 (메모리 회수 시도)
   ↓
5. GC 후에도 여전히 부족?
   ├─ YES → 📈 힙 확장 (예: 1GB → 1.5GB)
   └─ NO  → ✅ 계속 실행
   ↓
6. 반복 (최대 Xmx까지)
   ↓
7. Xmx 도달 후에도 부족?
   └─ 💥 OutOfMemoryError
```

### 확장/축소 트리거 조건

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

### 실제 동작 예시

**시나리오: 사진 업로드 처리**

```
시작: 256MB 할당
├─ 평소 사용: 500MB까지 증가
├─ GC 실행 → 300MB로 회수
└─ 다시 사용: 800MB까지 증가

사진 업로드 폭주:
├─ 메모리 사용: 950MB (힙 거의 가득)
├─ 🔄 GC 실행 → 700MB로 회수
├─ 여전히 부족 → 📈 힙 확장: 1GB → 1.5GB
├─ 계속 사용: 1.3GB
├─ 🔄 GC 실행 → 1GB로 회수
├─ 여전히 부족 → 📈 힙 확장: 1.5GB → 2GB
└─ 반복... 최대 4GB까지
```

## GC와 메모리 관리

### GC 알고리즘 비교

| GC 종류 | Java 버전 | 특징 | 적합한 상황 |
|---------|-----------|------|-------------|
| **G1GC** | Java 9+ (기본) | 균형잡힌 성능, 예측 가능한 일시정지 시간 | 범용, 대부분의 애플리케이션 |
| **ZGC** | Java 11+ | 초저지연 (<10ms), 대용량 힙(TB급) 지원 | 대용량 힙, 낮은 지연 요구 |
| **Shenandoah** | Java 12+ | 낮은 일시정지, 동시 압축 | 실시간성 요구 애플리케이션 |
| Serial GC | Java 1.0+ | 단일 스레드, 간단 | 작은 애플리케이션, 단일 코어 |
| Parallel GC | Java 5+ | 높은 처리량 | 배치 작업, 처리량 중시 |

### 힙 확장 시 GC 영향

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

### 모니터링 패턴 해석

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

## Runtime 클래스로 메모리 확인

### 주요 메서드

| 메서드 | 설명 | 반환값 |
|--------|------|--------|
| `maxMemory()` | 최대 힙 크기 (-Xmx) | bytes |
| `totalMemory()` | 현재 할당된 힙 크기 | bytes |
| `freeMemory()` | 할당된 힙 중 사용 가능한 메모리 | bytes |

**실제 사용 중인 메모리 계산:**
```
사용 중 메모리 = totalMemory() - freeMemory()
```

### 코드 예제

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

### 실전 활용: 메모리 부족 감지

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

## 실전 활용: Elastic Beanstalk 설정

### 인스턴스 타입별 권장 힙 크기

| 인스턴스 | RAM | 권장 힙 (-Xmx) | 초기 힙 (-Xms) |
|----------|-----|----------------|----------------|
| t3.small | 2GB | 1~1.5GB | 512MB~1GB |
| t3.medium | 4GB | 2~3GB | 1~2GB |
| m7g.large | 8GB | 4~6GB | 2~4GB |
| **m7g.xlarge** | **16GB** | **6~10GB** | **3~6GB** |

**메모리 할당 가이드:**
- 인스턴스 RAM의 50~75%를 힙에 할당
- 나머지는 OS, Metaspace, Direct Memory 등을 위해 예약

### 설정 방법

#### 방법 1: 환경 속성 (Console)

1. Elastic Beanstalk Console → 환경 선택
2. **구성(Configuration)** → **소프트웨어(Software)**
3. **환경 속성(Environment properties)** 추가:
   ```
   이름: JAVA_TOOL_OPTIONS
   값: -Xmx6g -Xms3g -XX:+UseG1GC
   ```

#### 방법 2: `.ebextensions` 설정

```yaml
# .ebextensions/jvm-options.config
option_settings:
  aws:elasticbeanstalk:application:environment:
    JAVA_TOOL_OPTIONS: "-Xmx6g -Xms3g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

#### 방법 3: Java 플랫폼 전용 설정

```yaml
# .ebextensions/java-settings.config
option_settings:
  aws:elasticbeanstalk:container:java:
    Xmx: 6g
    Xms: 3g
```

### 자동 설정 vs 명시적 설정 비교

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

### 사진 처리 워크로드 사례

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
- ✅ 시작부터 4GB 확보
- ✅ 사진 처리 시 여유 있음
- ✅ 힙 확장 최소화
- ✅ 예측 가능한 성능

### 배포 시 주의사항

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

## 핵심 요약

### 기억할 것

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

### 빠른 의사결정 가이드

```
메모리 사용률 < 50% → 현재 설정 유지
메모리 사용률 50~70% → 모니터링 강화
메모리 사용률 > 70% → 힙 크기 증설 고려
GC 빈도 증가 → 힙 크기 증설
Full GC 빈발 → GC 알고리즘 변경 또는 힙 증설
```

## 참고 자료

- [Oracle Java SE Documentation - Ergonomics](https://docs.oracle.com/en/java/javase/21/gctuning/ergonomics.html)
- [AWS Elastic Beanstalk - Java SE Platform](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/java-se-platform.html)
- [G1GC Tuning Guide](https://docs.oracle.com/en/java/javase/21/gctuning/garbage-first-g1-garbage-collector1.html)

*마지막 업데이트: 2024년 12월*
