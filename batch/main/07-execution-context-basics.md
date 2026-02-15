# ExecutionContext 기초

Spring Batch의 상태 관리 메커니즘인 ExecutionContext의 구조, Job/Step 수준의 차이, 그리고 데이터 저장/조회 API를 다룬다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

ExecutionContext는 배치 실행 중 **상태를 저장하는 키-값 저장소**다. 메타데이터 테이블에 직렬화되어 저장되므로, Job이 실패 후 재시작할 때 이전 상태를 복구할 수 있다.

```
┌─────────────────────────────────────────────────────────────────┐
│                      ExecutionContext 흐름                       │
│                                                                  │
│   Step 실행 중                    DB 메타데이터 테이블            │
│   ┌──────────────┐               ┌──────────────────────┐       │
│   │ ExecutionContext │  ──────▶  │ BATCH_STEP_EXECUTION  │       │
│   │ {                │   직렬화   │ _CONTEXT              │       │
│   │   "lastId": 500, │           │ ───────────────────── │       │
│   │   "count": 1000  │           │ SHORT_CONTEXT (JSON)  │       │
│   │ }                │           └──────────────────────┘       │
│   └──────────────┘                                              │
│         │                                                        │
│         │ 재시작 시                                               │
│         ▼                                                        │
│   ┌──────────────┐                                              │
│   │ 이전 상태 복구  │  ◀──────  DB에서 읽어옴                      │
│   │ lastId = 500   │                                             │
│   └──────────────┘                                              │
└─────────────────────────────────────────────────────────────────┘
```

핵심 특징:
- **영속성**: 메타데이터 DB에 JSON으로 직렬화되어 저장
- **재시작 지원**: 실패 후 재시작 시 이전 상태를 자동 복구
- **두 가지 스코프**: Job 레벨과 Step 레벨 각각 독립적으로 존재

---

## 2. 왜 알아야 하는가 (Why)

- **재시작 안정성**: 배치가 100만 건 중 50만 건에서 실패했을 때, ExecutionContext에 저장된 상태 덕분에 50만 건째부터 다시 시작할 수 있다
- **Step 간 데이터 전달**: JobExecutionContext를 통해 앞선 Step의 결과를 뒤따르는 Step에서 사용할 수 있다
- **진행 상태 추적**: Chunk 처리마다 현재 진행률을 저장하여, 관리자가 배치 실행 상태를 모니터링할 수 있다
- **커스텀 Reader/Writer**: ItemStream 인터페이스를 구현할 때 ExecutionContext API를 정확히 알아야 한다

---

## 3. 내부 구현 분석 (How)

### 3.1 Job vs Step ExecutionContext

두 종류의 ExecutionContext가 있으며, **범위(scope)**가 다르다.

```
┌─────────────────────────────────────────────────────────────────┐
│                           Job                                    │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │              JobExecutionContext                         │   │
│   │              (Job 전체에서 공유)                          │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                  │
│   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐       │
│   │    Step 1     │   │    Step 2     │   │    Step 3     │       │
│   │ ┌──────────┐ │   │ ┌──────────┐ │   │ ┌──────────┐ │       │
│   │ │StepExec  │ │   │ │StepExec  │ │   │ │StepExec  │ │       │
│   │ │Context   │ │   │ │Context   │ │   │ │Context   │ │       │
│   │ └──────────┘ │   │ └──────────┘ │   │ └──────────┘ │       │
│   └──────────────┘   └──────────────┘   └──────────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

| 구분 | JobExecutionContext | StepExecutionContext |
|------|---------------------|---------------------|
| **범위** | Job 전체 | 해당 Step 내 |
| **공유** | 모든 Step에서 접근 가능 | 해당 Step에서만 접근 |
| **저장 테이블** | BATCH_JOB_EXECUTION_CONTEXT | BATCH_STEP_EXECUTION_CONTEXT |
| **용도** | Step 간 데이터 전달, 전역 상태 | Step 내 진행 상태 추적 |

### 3.2 접근 방법

Tasklet에서 두 종류의 ExecutionContext에 접근하는 방법이다.

```java
// Tasklet에서 접근
@Bean
public Tasklet myTasklet() {
    return (contribution, chunkContext) -> {
        // StepExecutionContext
        ExecutionContext stepContext = chunkContext.getStepContext()
                .getStepExecution()
                .getExecutionContext();

        // JobExecutionContext
        ExecutionContext jobContext = chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext();

        // 데이터 저장
        stepContext.putLong("lastProcessedId", 12345L);
        jobContext.putInt("totalProcessed", 1000);

        return RepeatStatus.FINISHED;
    };
}
```

### 3.3 데이터 저장과 조회

#### 지원하는 데이터 타입

ExecutionContext는 기본 타입에 대한 타입별 메서드를 제공한다.

```java
ExecutionContext context = stepExecution.getExecutionContext();

// 기본 타입
context.putString("status", "PROCESSING");
context.putLong("lastId", 12345L);
context.putInt("count", 100);
context.putDouble("rate", 0.95);

// 조회
String status = context.getString("status");
Long lastId = context.getLong("lastId");
int count = context.getInt("count");

// 기본값 지정
Long id = context.getLong("lastId", 0L);  // 없으면 0L 반환

// 존재 여부 확인
if (context.containsKey("lastId")) {
    // ...
}
```

#### 복합 객체 저장

`Serializable`을 구현한 객체라면 `put()` 메서드로 저장할 수 있다. JSON으로 직렬화되어 DB에 저장된다.

```java
// 직렬화 가능한 객체 저장
context.put("checkpoint", new Checkpoint(lastId, lastDate));
Checkpoint checkpoint = (Checkpoint) context.get("checkpoint");

// 주의: 객체는 Serializable 구현 필수
@Data
public class Checkpoint implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long lastId;
    private LocalDate lastDate;
}
```

#### List/Map 저장

컬렉션 타입도 저장 가능하다. 단, 직렬화 가능한 구현체를 사용해야 한다.

```java
// List 저장
List<Long> processedIds = Arrays.asList(1L, 2L, 3L);
context.put("processedIds", new ArrayList<>(processedIds));

// Map 저장
Map<String, Integer> stats = new HashMap<>();
stats.put("success", 100);
stats.put("failed", 5);
context.put("stats", stats);
```

---

## 4. 실전 예제

### Step 간 데이터 전달 패턴

Step 1에서 집계한 결과를 Step 2에서 사용하는 전형적인 패턴이다.

```java
// Step 1: 집계 결과를 JobExecutionContext에 저장
@Bean
public Tasklet aggregationTasklet() {
    return (contribution, chunkContext) -> {
        ExecutionContext jobContext = chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext();

        int totalCount = calculateTotalCount();
        jobContext.putInt("totalCount", totalCount);
        jobContext.putString("aggregationDate", LocalDate.now().toString());

        return RepeatStatus.FINISHED;
    };
}

// Step 2: JobExecutionContext에서 값 조회
@Bean
public Tasklet reportTasklet() {
    return (contribution, chunkContext) -> {
        ExecutionContext jobContext = chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext();

        int totalCount = jobContext.getInt("totalCount");
        String date = jobContext.getString("aggregationDate");

        generateReport(totalCount, date);
        return RepeatStatus.FINISHED;
    };
}
```

### 재시작을 위한 Checkpoint 저장

```java
@Bean
public Tasklet checkpointTasklet() {
    return (contribution, chunkContext) -> {
        ExecutionContext stepContext = chunkContext.getStepContext()
                .getStepExecution()
                .getExecutionContext();

        // 재시작 시 이전 체크포인트 복구
        long startId = stepContext.getLong("lastProcessedId", 0L);

        List<Customer> customers = repository.findByIdGreaterThan(startId);
        for (Customer c : customers) {
            process(c);
            // 주기적으로 체크포인트 갱신
            stepContext.putLong("lastProcessedId", c.getId());
        }

        return RepeatStatus.FINISHED;
    };
}
```

---

## 5. 정리

| 항목 | 설명 |
|------|------|
| **ExecutionContext** | 배치 실행 상태를 저장하는 키-값 저장소 |
| **영속성** | 메타데이터 DB에 JSON으로 직렬화하여 저장 |
| **JobExecutionContext** | Job 전체에서 공유, Step 간 데이터 전달에 사용 |
| **StepExecutionContext** | 해당 Step 내에서만 유효, 진행 상태 추적에 사용 |
| **기본 타입 API** | `putString()`, `putLong()`, `putInt()`, `putDouble()` |
| **복합 객체** | `Serializable` 구현 필수, `put()/get()`으로 저장/조회 |
| **기본값 조회** | `getLong("key", defaultValue)`로 null 안전 처리 |
| **저장 테이블** | `BATCH_JOB_EXECUTION_CONTEXT` / `BATCH_STEP_EXECUTION_CONTEXT` |

---
*참고: Spring Batch 5.x / Spring Boot 3.x 기준*
