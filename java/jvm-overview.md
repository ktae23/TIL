# JVM 완전 가이드 (Java 8 → Java 17+)

Java Virtual Machine의 메모리 구조, 실행 흐름, 클래스 로딩, 컴파일러, 가비지 컬렉션에 대한 종합 가이드입니다.

**기준 버전**: Java 8 (Java 9, 11, 17 변경사항 포함)

---

## 📚 문서 구성

이 가이드는 3개의 상세 문서로 구성되어 있습니다:

### 1. [JVM 메모리 구조와 가비지 컬렉션](jvm-memory-and-gc.md)

JVM의 메모리 영역과 GC 메커니즘을 다룹니다.

**주요 내용:**
- **메모리 영역**: Metaspace, Heap, Stack, PC Register
- **Java 8 Parallel GC**: 고정된 Young/Old 구조, 높은 처리량
- **Java 9+ G1 GC**: Region 기반, 예측 가능한 중지 시간
- **Java 11 ZGC**: 초저지연 (10ms 이하), 대용량 Heap (TB급)
- **Java 11 Epsilon GC**: No-Op GC, 성능 테스트용

**핵심 비교:**

| GC | Java 버전 | 중지 시간 | 적합한 환경 |
|---|---|---|---|
| Parallel | Java 8 기본 | 수백ms~수초 | 배치, 높은 처리량 |
| G1 | Java 9+ 기본 | 수십~수백ms | 일반 서버, 대용량 Heap |
| ZGC | Java 11+ | **10ms 미만** | 실시간 서비스, 초대용량 |

---

### 2. [JVM 실행 흐름과 컴파일러](jvm-execution-and-compilation.md)

바이트코드 실행 흐름과 다양한 컴파일러를 다룹니다.

**주요 내용:**
- **전체 실행 과정**: 소스 코드 → 바이트코드 → 실행 → GC
- **Stack Frame**: 메서드 호출과 지역 변수 관리
- **Java 8 인터프리터 + JIT**: Hot Spot 감지, C1/C2 컴파일러
- **Java 9+ AOT**: 사전 컴파일, 빠른 시작 (Java 17에서 제거)
- **Java 17+ GraalVM Native Image**: JVM 불필요, 밀리초 시작

**핵심 비교:**

| 방식 | 시작 시간 | Peak 성능 | 메모리 | 적합한 환경 |
|---|---|---|---|---|
| JIT (Java 8) | 느림 (워밍업) | 매우 높음 | 높음 | 장시간 실행 서버 |
| AOT (Java 9-16) | 30-70% 단축 | JIT보다 5-10% 낮음 | 중간 | 짧은 실행 |
| GraalVM Native Image | **90% 이상 단축** | JIT보다 10-30% 낮음 | **1/10** | 서버리스, 마이크로서비스 |

---

### 3. [JVM 클래스 로더와 모듈 시스템](jvm-classloader-and-modules.md)

클래스 로딩 메커니즘과 Java 9 모듈 시스템을 다룹니다.

**주요 내용:**
- **Java 8 클래스 로더**: Bootstrap / Extension / Application
- **Java 9+ 모듈 시스템 (JPMS)**: rt.jar 제거, Platform ClassLoader
- **클래스 로딩 3단계**: Loading → Linking → Initialization
- **어노테이션 프로세서**: 컴파일 타임 코드 생성 (Lombok, QueryDSL)

**핵심 변경:**

| 항목 | Java 8 | Java 9+ |
|---|---|---|
| 핵심 라이브러리 | rt.jar (60MB+) | 모듈로 분리 (java.base 등) |
| Extension CL | `ExtClassLoader` | `PlatformClassLoader` |
| 캡슐화 | 약함 (internal API 접근 가능) | 강함 (모듈 경계 강제) |
| 클래스 경로 | classpath만 | classpath + module-path |

---

## 🔑 Java 버전별 핵심 변경사항

### Java 8 (2014)
- **Metaspace 도입**: PermGen 제거, Native Memory 사용
- **Parallel GC 기본**: 고정된 Young/Old 구조
- **Lambda와 Stream API**: 함수형 프로그래밍

### Java 9 (2017)
- **모듈 시스템 (JPMS)**: rt.jar 제거, 강력한 캡슐화
- **G1 GC 기본**: Region 기반, 예측 가능한 중지 시간
- **AOT 컴파일러**: 사전 컴파일로 빠른 시작

### Java 11 (2018, LTS)
- **ZGC**: 초저지연 (10ms 미만), TB급 Heap 지원
- **Epsilon GC**: No-Op GC, 성능 테스트용
- **Flight Recorder 오픈소스화**: 무료 프로파일링 도구

### Java 17 (2021, LTS)
- **AOT/Graal JIT 제거**: GraalVM Native Image로 대체
- **Sealed Classes**: 상속 제한
- **Pattern Matching for switch**: 패턴 매칭

---

## 📖 추천 학습 순서

1. **초급**: [메모리 구조와 GC](jvm-memory-and-gc.md) → Java 8 Parallel GC 부분만
2. **중급**: [실행 흐름](jvm-execution-and-compilation.md) → JIT 컴파일러까지
3. **고급**: [클래스 로더](jvm-classloader-and-modules.md) → 모듈 시스템
4. **심화**: 각 문서의 Java 9+, Java 11 변경사항

---

## 🔗 참고 자료

- [Oracle Java 8 JVM Specification](https://docs.oracle.com/javase/specs/jvms/se8/html/)
- [G1 GC Overview](https://www.oracle.com/technical-resources/articles/java/g1gc.html)
- [ZGC Documentation](https://wiki.openjdk.org/display/zgc/Main)
- [GraalVM Native Image](https://www.graalvm.org/latest/reference-manual/native-image/)
- [Java Platform Module System](https://openjdk.org/projects/jigsaw/)

---

*마지막 업데이트: 2025년 12월*
