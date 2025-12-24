# JVM 메모리 구조와 가비지 컬렉션

JVM의 메모리 영역과 가비지 컬렉션 메커니즘에 대한 상세 분석입니다.

**기준 버전**: Java 8 → Java 11+ 변경사항 포함

## 목차

- [메모리 영역 개요](#메모리-영역-개요)
- [Metaspace](#metaspace)
- [Heap 메모리와 GC](#heap-메모리와-gc)
  - [Java 8: Parallel GC](#java-8-기본-gc-parallel-gc)
  - [Java 9+: G1 GC](#java-9-기본-gc-g1-gc-garbage-first)
  - [Java 11: ZGC](#java-11-zgc-z-garbage-collector)
  - [Java 11: Epsilon GC](#java-11-epsilon-gc-no-op-garbage-collector)
- [Stack](#stack)
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

## 참고 자료

- [Oracle Java 8 JVM Specification](https://docs.oracle.com/javase/specs/jvms/se8/html/)
- [From PermGen to Metaspace](https://www.baeldung.com/java-permgen-metaspace)
- [G1 GC Overview](https://www.oracle.com/technical-resources/articles/java/g1gc.html)
- [ZGC Documentation](https://wiki.openjdk.org/display/zgc/Main)

---

*마지막 업데이트: 2025년 12월*
