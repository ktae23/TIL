# JVM 메모리 구조와 가비지 컬렉션

JVM의 메모리 영역과 가비지 컬렉션 메커니즘에 대한 상세 분석입니다.

**기준 버전**: Java 8 → Java 11+ 변경사항 포함

## 목차

- [메모리 영역 개요](#메모리-영역-개요)
- [Metaspace](#metaspace)
- [Heap 메모리와 GC](#heap-메모리와-gc)
  - [JVM 힙 메모리 기본값 (Ergonomics)](#jvm-힙-메모리-기본값-ergonomics)
  - [힙 메모리 동적 확장 메커니즘](#힙-메모리-동적-확장-메커니즘)
  - [Java 8: Parallel GC](#java-8-기본-gc-parallel-gc)
  - [Java 9+: G1 GC](#java-9-기본-gc-g1-gc-garbage-first)
  - [Java 11: ZGC](#java-11-zgc-z-garbage-collector)
  - [Java 11: Epsilon GC](#java-11-epsilon-gc-no-op-garbage-collector)
  - [GC 알고리즘 통합 비교](#gc-알고리즘-통합-비교)
- [Runtime 클래스로 메모리 확인](#runtime-클래스로-메모리-확인)
- [실전 활용: Elastic Beanstalk 설정](#실전-활용-elastic-beanstalk-설정)
- [Stack](#stack)
- [핵심 요약](#핵심-요약)
- [참고 자료](#참고-자료)

---

## 메모리 영역 개요

JVM은 다음과 같은 메모리 영역으로 구분됩니다:

### 메모리 영역 비교표 (Java 8 기준)

| 영역 | 스레드 공유 | 생명주기 | 주요 저장 내용 | GC 대상 |
|------|------------|---------|--------------|---------|
| **Metaspace** | ✅ 공유 | JVM 시작~종료 | 클래스 메타데이터, static 변수, 상수 풀 | ✅ |
| **Heap** | ✅ 공유 | JVM 시작~종료 | 객체 인스턴스, 배열 | ✅ |
| **Stack** | ❌ 스레드별 | 스레드 시작~종료 | 지역 변수, 메서드 호출 정보 | ❌ |
| **PC Register** | ❌ 스레드별 | 스레드 시작~종료 | 현재 실행 중인 명령어 주소 | ❌ |
| **Native Method Stack** | ❌ 스레드별 | 스레드 시작~종료 | Native 메서드 호출 정보 | ❌ |

---

## Metaspace

### Java 8의 핵심 변경사항: PermGen → Metaspace

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

### Java 7 이전 (PermGen)

- Heap 내부에 위치
- 고정된 크기 (`-XX:MaxPermSize=128m`)
- 클래스 메타데이터, static 변수, 상수 풀 저장
- **문제점**: `OutOfMemoryError: PermGen space` 발생 빈번

### Java 8 이후 (Metaspace)

- Native Memory에 위치 (Heap 외부)
- 동적 확장 가능 (`-XX:MaxMetaspaceSize` 기본값: 무제한)
- OS 메모리 한도까지 자동 확장
- **장점**: PermGen 공간 부족 문제 해결, 메모리 효율성 향상

---

## Heap 메모리와 GC

### JVM 힙 메모리 기본값 (Ergonomics)

#### 별도 설정 없을 시 기본값

JVM은 물리 메모리를 기반으로 힙 크기를 자동 설정합니다 (Java 5 이후):

```
초기 힙 (-Xms) = 물리 메모리 / 64 (최소 32MB)
최대 힙 (-Xmx) = 물리 메모리 / 4
```

**예시: 16GB RAM 인스턴스**
- 초기 힙: 256MB (16GB ÷ 64)
- 최대 힙: 4GB (16GB ÷ 4)

#### Java 버전별 발전 과정

| 버전 | 연도 | 주요 변화 |
|------|------|-----------|
| Java 1.0 | 1996 | 동적 할당 메커니즘 존재 (수동 설정) |
| **Java 1.4** | **2002** | **Ergonomics 도입** - Server-Class Machine Detection |
| **Java 5** | **2004** | **1/64, 1/4 규칙 정립** |
| Java 6-7 | 2006-2011 | 최대 힙 1GB 제한 완화, G1GC 도입 |
| **Java 8** | **2014** | **현대적 기본값** - 제한 제거, PermGen → Metaspace |
| Java 10+ | 2018~ | 컨테이너(Docker/K8s) 환경 인식 개선 |

#### 컨테이너 환경에서의 차이

```bash
# Java 8 이하: 호스트 메모리 인식
컨테이너 제한 2GB, 호스트 16GB
→ JVM이 16GB 인식 → 최대 힙 4GB 할당 (❌ OOM 발생)

# Java 10 이상: 컨테이너 제한 인식
컨테이너 제한 2GB
→ JVM이 2GB 인식 → 최대 힙 512MB 할당 (✅ 올바름)
```

### 힙 메모리 동적 확장 메커니즘

#### 기본 개념

JVM은 **동적 할당**을 지원합니다 (Java 1.0부터):
- 초기 힙(Xms)으로 시작
- 필요 시 최대 힙(Xmx)까지 자동 확장
- 여유 시 힙 축소 가능

#### 확장 메커니즘 순서도

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

#### 확장/축소 트리거 조건

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

#### 실제 동작 예시

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

#### 힙 확장 시 GC 영향

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

#### 모니터링 패턴 해석

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
- ✅ 높은 처리량 (Throughput)
- ✅ 멀티 CPU 활용 효율적
- ❌ 긴 중지 시간 (특히 Old GC)
- ❌ 중지 시간 예측 어려움

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
│  O: Old               H: Humongous (대형 객체, ≥50% region)│
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
- ✅ 예측 가능한 중지 시간 (목표 시간 설정 가능)
- ✅ 큰 Heap에서 효율적 (6GB 이상 권장)
- ✅ 메모리 단편화 방지
- ❌ 작은 Heap에서는 Parallel GC보다 느릴 수 있음
- ❌ CPU 오버헤드 약간 증가 (Concurrent 작업)

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

## Java 11 최신 GC

Java 11은 LTS(Long-Term Support) 버전으로, GC 분야에서 혁신적인 발전이 있었습니다.

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
| ✅ GC 오버헤드 완전 제거 (0ms 중지 시간) | ❌ 장시간 실행 불가 (메모리 고갈) |
| ✅ 예측 가능한 성능 (GC로 인한 변동 없음) | ❌ 메모리 누수 위험 높음 |
| ✅ 벤치마크 테스트에 유용 | ❌ 프로덕션 환경에 부적합 |

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

**Java 8 Parallel GC vs Java 9 G1 GC vs Java 11 ZGC:**

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
| ✅ 일관된 초저지연 (10ms 미만) | ❌ 처리량 약간 낮음 (Load Barrier 오버헤드) |
| ✅ 대용량 Heap 효율적 처리 | ❌ 메모리 사용량 증가 (메타데이터) |
| ✅ 중지 시간이 Heap 크기에 비례하지 않음 | ❌ 작은 Heap에서는 G1보다 느릴 수 있음 |
| ✅ 실시간 애플리케이션에 적합 | ❌ Java 11 이상 필요 |

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

---

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

---

## Stack

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

---

## 참고 자료

- [Oracle Java 8 JVM Specification](https://docs.oracle.com/javase/specs/jvms/se8/html/)
- [Oracle Java SE Documentation - Ergonomics](https://docs.oracle.com/en/java/javase/21/gctuning/ergonomics.html)
- [From PermGen to Metaspace](https://www.baeldung.com/java-permgen-metaspace)
- [G1 GC Overview](https://www.oracle.com/technical-resources/articles/java/g1gc.html)
- [G1GC Tuning Guide](https://docs.oracle.com/en/java/javase/21/gctuning/garbage-first-g1-garbage-collector1.html)
- [ZGC Documentation](https://wiki.openjdk.org/display/zgc/Main)
- [AWS Elastic Beanstalk - Java SE Platform](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/java-se-platform.html)

---

*마지막 업데이트: 2025년 12월*
