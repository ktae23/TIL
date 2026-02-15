# 스코프 함수 5총사

Kotlin의 스코프 함수(`let`, `run`, `with`, `apply`, `also`)는 객체의 컨텍스트 안에서 코드 블록을 실행하는 확장 함수다. 임시 스코프를 생성하여 코드를 더 간결하고 읽기 쉽게 만든다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### 5가지 스코프 함수 한눈에 보기

| 함수 | 객체 참조 | 반환값 | 확장 함수? |
|------|----------|--------|-----------|
| `let` | `it` | 람다 결과 | O |
| `run` | `this` | 람다 결과 | O |
| `with` | `this` | 람다 결과 | X (일반 함수) |
| `apply` | `this` | 컨텍스트 객체 | O |
| `also` | `it` | 컨텍스트 객체 | O |

두 가지 축으로 구분한다:

1. **객체 참조 방식**: `this`(수신 객체) vs `it`(람다 인자)
2. **반환값**: 람다 결과(마지막 표현식) vs 컨텍스트 객체(원래 객체)

### 각 함수의 시그니처

```kotlin
// let: 확장 함수, it으로 참조, 람다 결과 반환
public inline fun <T, R> T.let(block: (T) -> R): R

// run: 확장 함수, this로 참조, 람다 결과 반환
public inline fun <T, R> T.run(block: T.() -> R): R

// with: 일반 함수, this로 참조, 람다 결과 반환
public inline fun <T, R> with(receiver: T, block: T.() -> R): R

// apply: 확장 함수, this로 참조, 컨텍스트 객체 반환
public inline fun <T> T.apply(block: T.() -> Unit): T

// also: 확장 함수, it으로 참조, 컨텍스트 객체 반환
public inline fun <T> T.also(block: (T) -> Unit): T
```

### this vs it 차이

```kotlin
data class User(var name: String, var age: Int)

// this — 수신 객체로 접근 (프로퍼티를 바로 참조)
User("Kim", 25).apply {
    name = "Lee"    // this.name = "Lee" (this 생략 가능)
    age = 30        // this.age = 30
}

// it — 람다 인자로 접근 (명시적 이름 참조)
User("Kim", 25).let {
    it.name         // it으로 접근 필수
    it.age
}

// it은 이름을 변경할 수 있다
User("Kim", 25).let { user ->
    user.name       // 의미 있는 이름으로 가독성 향상
    user.age
}
```

---

## 2. 왜 알아야 하는가 (Why)

### null 안전 처리

```kotlin
// 기존 방식
val order = findOrder(id)
if (order != null) {
    println(order.total)
    sendNotification(order)
}

// let 활용
findOrder(id)?.let { order ->
    println(order.total)
    sendNotification(order)
}
```

### 객체 초기화 간결화

```kotlin
// 기존 방식
val config = ServerConfig()
config.host = "localhost"
config.port = 8080
config.ssl = true
config.timeout = Duration.ofSeconds(30)

// apply 활용
val config = ServerConfig().apply {
    host = "localhost"
    port = 8080
    ssl = true
    timeout = Duration.ofSeconds(30)
}
```

### 중간 변수 제거

```kotlin
// 기존 방식
val numbers = listOf(1, 2, 3, 4, 5)
val doubled = numbers.map { it * 2 }
val result = doubled.joinToString(", ")
println(result)

// run 활용
listOf(1, 2, 3, 4, 5).run {
    map { it * 2 }.joinToString(", ")
}.also { println(it) }
```

---

## 3. 내부 구현 분석 (How)

### 선택 기준 플로우차트

```mermaid
flowchart TD
    Start["스코프 함수 선택"] --> Q1{"반환값이\n필요한가?"}

    Q1 -->|"원래 객체 반환\n(체이닝)"| Q2{"객체를\n설정하는가?"}
    Q2 -->|"프로퍼티 설정\n(this)"| APPLY["apply"]
    Q2 -->|"부수효과 실행\n(로깅 등)"| ALSO["also"]

    Q1 -->|"변환 결과 반환\n(다른 타입)"| Q3{"객체 참조\n방식은?"}
    Q3 -->|"this\n(프로퍼티 직접 접근)"| Q4{"확장 함수로\n호출?"|}
    Q4 -->|Yes| RUN["run"]
    Q4 -->|No| WITH["with"]
    Q3 -->|"it\n(명시적 참조)"| LET["let"]

    style APPLY fill:#4CAF50,color:white
    style ALSO fill:#FF9800,color:white
    style RUN fill:#2196F3,color:white
    style WITH fill:#9C27B0,color:white
    style LET fill:#F44336,color:white
```

### 각 함수의 사용 시나리오

```
+----------+------------------+-------------------------------+
| 함수     | 핵심 시나리오     | 키워드                         |
+----------+------------------+-------------------------------+
| let      | null 체크 + 변환  | "null이 아니면 변환해라"        |
| run      | 객체로 계산       | "이 객체로 결과를 계산해라"      |
| with     | 객체의 함수 호출  | "이 객체에 대해 작업해라"        |
| apply    | 객체 초기화       | "이 객체를 설정하고 돌려줘"      |
| also     | 부수효과          | "이것도 해라 (로깅, 검증)"      |
+----------+------------------+-------------------------------+
```

### inline과 성능

모든 스코프 함수는 `inline`으로 선언되어 있다. 컴파일 시 람다가 호출 지점에 인라인되므로:

- 람다 객체 생성 없음
- 함수 호출 오버헤드 없음
- 일반 코드와 동일한 바이트코드 생성

```kotlin
// Kotlin 소스
val result = "hello".let { it.uppercase() }

// 컴파일 후 (인라인 적용)
String $this = "hello";
String result = $this.toUpperCase();  // 람다/함수 호출 없이 직접 실행
```

---

## 4. 실전 예제

### 예제 1: let — null 체크와 변환

```kotlin
// 패턴 1: nullable 값 처리
fun getDisplayName(user: User?): String =
    user?.let { "${it.firstName} ${it.lastName}" } ?: "Anonymous"

// 패턴 2: 스코프 제한 (임시 변수 범위 축소)
val result = inputString.let { input ->
    val trimmed = input.trim()
    val normalized = trimmed.lowercase()
    normalized.replace(Regex("\\s+"), "-")
}

// 패턴 3: 타입 변환 체이닝
val userId: Long? = request.getParameter("userId")
    ?.let { it.toLongOrNull() }
    ?.let { userRepository.findById(it) }
    ?.let { it.id }
```

### 예제 2: apply — 객체 설정과 빌더 대체

```kotlin
// 패턴 1: 객체 초기화
fun createHttpClient(): OkHttpClient =
    OkHttpClient.Builder().apply {
        connectTimeout(30, TimeUnit.SECONDS)
        readTimeout(30, TimeUnit.SECONDS)
        addInterceptor(LoggingInterceptor())
        addInterceptor(AuthInterceptor(tokenProvider))
    }.build()

// 패턴 2: 테스트 데이터 생성
fun createTestUser(): User =
    User().apply {
        name = "테스트 사용자"
        email = "test@example.com"
        role = Role.ADMIN
        createdAt = LocalDateTime.of(2026, 1, 1, 0, 0)
    }
```

### 예제 3: also — 부수효과와 디버깅

```kotlin
// 패턴 1: 로깅
fun findUser(id: Long): User? =
    userRepository.findById(id)
        .also { logger.debug("findUser($id) 결과: $it") }

// 패턴 2: 검증
fun createOrder(request: OrderRequest): Order =
    request
        .also { require(it.items.isNotEmpty()) { "주문 항목이 비어있습니다" } }
        .also { require(it.totalAmount > BigDecimal.ZERO) { "금액은 0보다 커야 합니다" } }
        .let { orderService.create(it) }

// 패턴 3: 체이닝 중간에 삽입
listOf(3, 1, 4, 1, 5)
    .sorted()
    .also { println("정렬 후: $it") }         // [1, 1, 3, 4, 5]
    .filter { it > 2 }
    .also { println("필터 후: $it") }         // [3, 4, 5]
    .map { it * 10 }                          // [30, 40, 50]
```

### 예제 4: run — 객체 컨텍스트에서 계산

```kotlin
// 패턴 1: 객체의 여러 프로퍼티로 결과 계산
val summary = transaction.run {
    """
    거래 요약
    --------
    거래 ID: $id
    금액: ${amount.formatKRW()}
    계정: $accountType
    일시: ${timestamp.toKoreanFormat()}
    """.trimIndent()
}

// 패턴 2: 비-확장 run (코드 블록 실행)
val hexColor = run {
    val r = (Math.random() * 256).toInt()
    val g = (Math.random() * 256).toInt()
    val b = (Math.random() * 256).toInt()
    "#%02x%02x%02x".format(r, g, b)
}
```

### 예제 5: with — 객체에 대한 그룹 연산

```kotlin
// 패턴: 하나의 객체에 여러 작업 수행
fun renderUser(user: User): String = with(user) {
    StringBuilder().apply {
        appendLine("이름: $name")
        appendLine("이메일: $email")
        appendLine("가입일: ${createdAt.toKoreanFormat()}")
        if (isAdmin) appendLine("[관리자]")
    }.toString()
}
```

### 예제 6: 스코프 함수 체이닝

```kotlin
// 실전 서비스 코드 흐름
fun processTransaction(request: TransactionRequest): ApiResponse<TransactionDto> =
    request
        .also { logger.info("거래 처리 시작: ${it.description}") }
        .let { it.toEntity() }
        .apply { validate() }
        .let { transactionRepository.save(it) }
        .also { publishEvent(BookkeepingEvent.created(it.id, it.amount, it.accountType)) }
        .let { it.toDto() }
        .let { ApiResponse.ok(it) }
```

---

## 5. 정리

| 함수 | 객체 참조 | 반환값 | 대표 용도 |
|------|----------|--------|----------|
| **let** | `it` | 람다 결과 | null 체크, 변환, 스코프 제한 |
| **run** | `this` | 람다 결과 | 객체 프로퍼티로 결과 계산 |
| **with** | `this` | 람다 결과 | 비-확장, 하나의 객체에 그룹 연산 |
| **apply** | `this` | 객체 자체 | 객체 초기화, 설정 |
| **also** | `it` | 객체 자체 | 부수효과 (로깅, 검증) |

**선택 기준 요약:**

```
원래 객체가 필요하면 → apply (설정) / also (부수효과)
변환 결과가 필요하면 → let (it) / run (this) / with (비확장)
null 체크가 목적이면 → ?.let { }
```

> 스코프 함수는 남용하면 오히려 코드 가독성을 해친다. 중첩 깊이 2단계 이상은 피하고, 각 함수의 의미적 역할에 맞게 사용하는 것이 핵심이다. "어떤 함수를 쓸까"보다 "여기서 스코프 함수가 정말 필요한가"를 먼저 판단하자.

---
*참고: Kotlin 2.0 기준*
