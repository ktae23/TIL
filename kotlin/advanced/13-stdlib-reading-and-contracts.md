# 표준 라이브러리 읽기와 Contract

Kotlin 문법을 "대략 안다"에서 "깊이 이해한다"로 넘어가는 가장 빠른 길은 **표준 라이브러리(stdlib) 소스를 직접 읽는 것**이다. `let`, `require`, `takeIf`, `use` 같은 함수는 대부분 5~10줄이고, 그 안에 `inline`, `contract`, `Nothing` 같은 언어 핵심 기능이 압축되어 있다. 이 문서는 stdlib를 읽는 방법과, 읽을 때 반드시 만나게 되는 `contract`를 다룬다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### 1.1 stdlib를 읽는 방법

IntelliJ에서 함수 위에 커서를 두고 `Cmd+B`(macOS) / `Ctrl+B`(Windows)를 누르면 선언부로 이동한다. Kotlin stdlib는 소스가 함께 배포되므로 실제 구현을 그대로 볼 수 있다.

| 단축키 | 동작 |
|--------|------|
| `Cmd+B` / `Ctrl+B` | 선언부로 이동 (Go to Declaration) |
| `Cmd+Shift+A` → "Show Kotlin Bytecode" | 바이트코드 뷰어 → `Decompile` 버튼으로 Java 코드 확인 |
| `Cmd+Option+B` | 구현체로 이동 (Go to Implementation) |
| `Option+Space` | 팝업으로 미리보기 |

읽을 때는 이 순서로 본다.

1. **시그니처** — `inline`인가? 확장 함수인가? 제네릭은?
2. **contract 블록** — 컴파일러에게 무엇을 약속하는가?
3. **본문** — 대개 1~3줄
4. **디컴파일** — 실제로 뭐가 되는가?

### 1.2 스코프 함수의 실제 구현

```kotlin
// kotlin/util/Standard.kt
public inline fun <T, R> T.let(block: (T) -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return block(this)
}

public inline fun <T> T.also(block: (T) -> Unit): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    block(this)
    return this
}

public inline fun <T> T.apply(block: T.() -> Unit): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    block()
    return this
}

public inline fun <T, R> T.run(block: T.() -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return block()
}

public inline fun <T, R> with(receiver: T, block: T.() -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return receiver.block()
}
```

5개 함수의 차이가 소스에서 한눈에 드러난다. 외울 필요가 없다.

| 함수 | 람다 파라미터 | 반환값 | 확장 함수? |
|------|--------------|--------|-----------|
| `let` | `(T) -> R` → `it` | 람다 결과 `R` | O |
| `run` | `T.() -> R` → `this` | 람다 결과 `R` | O |
| `also` | `(T) -> Unit` → `it` | 수신 객체 `T` | O |
| `apply` | `T.() -> Unit` → `this` | 수신 객체 `T` | O |
| `with` | `T.() -> R` → `this` | 람다 결과 `R` | X (일반 함수) |

**규칙 두 개면 끝난다.**
- 람다 타입이 `T.()`면 `this`, `(T)`면 `it`
- 반환 타입이 `R`이면 람다 결과, `T`면 수신 객체 자신

### 1.3 `contract` — 컴파일러와의 계약

`contract`는 함수가 컴파일러에게 "내 동작을 이렇게 보장한다"고 알려주는 장치다. 컴파일러는 이 정보를 스마트 캐스트와 초기화 분석에 사용한다.

```kotlin
// kotlin/text/Strings.kt
@OptIn(ExperimentalContracts::class)
public inline fun CharSequence?.isNullOrBlank(): Boolean {
    contract {
        returns(false) implies (this@isNullOrBlank != null)
    }
    return this == null || this.isBlank()
}
```

"이 함수가 `false`를 반환했다면, 수신 객체는 null이 아니다"를 컴파일러에게 알려준다. 덕분에 다음이 가능해진다.

```kotlin
fun process(input: String?) {
    if (input.isNullOrBlank()) return
    println(input.length)     // ✅ 스마트 캐스트 — input이 String으로 좁혀짐
}
```

`contract`가 없다면 컴파일러는 `isNullOrBlank()`가 뭘 하는지 알 수 없으므로 스마트 캐스트가 불가능하다.

### 1.4 contract의 두 가지 형태

```kotlin
contract {
    // (1) returns(...) implies (조건)
    //     "이 값을 반환했다면, 이 조건이 참이다"
    returns(true) implies (value is String)
    returns() implies (value != null)          // 정상 반환(예외 없이)했다면
    returnsNotNull() implies (value != null)   // null이 아닌 값을 반환했다면

    // (2) callsInPlace(람다, 호출 횟수)
    //     "이 람다는 이 함수를 벗어나지 않고, 정확히 N번 호출된다"
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
}
```

`InvocationKind` 종류:

| 값 | 의미 | 효과 |
|----|------|------|
| `EXACTLY_ONCE` | 정확히 1회 | 람다 안에서 `val` 초기화 가능 |
| `AT_LEAST_ONCE` | 1회 이상 | 초기화 보장은 되지만 재할당 불가 |
| `AT_MOST_ONCE` | 0 또는 1회 | 초기화 보장 안 됨 |
| `UNKNOWN` | 알 수 없음 | 기본값 |

`EXACTLY_ONCE`의 효과는 이렇게 나타난다.

```kotlin
val config: Config           // val인데 선언 시 초기화하지 않음
someResource.let {
    config = parseConfig(it)  // ✅ contract 덕분에 "정확히 1번 할당"이 보장되어 컴파일 통과
}
println(config)               // ✅ 초기화 완료로 인식
```

### 1.5 `require` / `check` / `error` — 방어 코드 3형제

```kotlin
// kotlin/Preconditions.kt
public inline fun require(value: Boolean, lazyMessage: () -> Any): Unit {
    contract { returns() implies value }
    if (!value) {
        val message = lazyMessage()
        throw IllegalArgumentException(message.toString())
    }
}

public inline fun check(value: Boolean, lazyMessage: () -> Any): Unit {
    contract { returns() implies value }
    if (!value) {
        val message = lazyMessage()
        throw IllegalStateException(message.toString())
    }
}

public inline fun error(message: Any): Nothing = throw IllegalStateException(message.toString())
```

| 함수 | 던지는 예외 | 용도 |
|------|-----------|------|
| `require(조건)` | `IllegalArgumentException` | **인자** 검증 (호출자 잘못) |
| `requireNotNull(값)` | `IllegalArgumentException` | 인자 null 검증 + 스마트 캐스트 |
| `check(조건)` | `IllegalStateException` | **상태** 검증 (객체 상태 잘못) |
| `checkNotNull(값)` | `IllegalStateException` | 상태 null 검증 + 스마트 캐스트 |
| `error(메시지)` | `IllegalStateException` | 도달하면 안 되는 분기 |

`lazyMessage`가 람다인 이유가 중요하다. **조건이 참일 때는 메시지 문자열을 아예 만들지 않는다.**

```kotlin
require(amount > BigDecimal.ZERO) { "금액이 0 이하입니다: $amount, 계좌: ${account.no}" }
//                                  ↑ 조건이 참이면 이 문자열 연결은 실행되지 않음
```

정상 경로에서 문자열 생성 비용이 0이다. `inline`이라 람다 객체도 생성되지 않는다. 로깅 API의 `log.debug { }` 와 같은 원리다.

### 1.6 `takeIf` / `takeUnless`

```kotlin
public inline fun <T> T.takeIf(predicate: (T) -> Boolean): T? {
    contract { callsInPlace(predicate, InvocationKind.EXACTLY_ONCE) }
    return if (predicate(this)) this else null
}

public inline fun <T> T.takeUnless(predicate: (T) -> Boolean): T? {
    contract { callsInPlace(predicate, InvocationKind.EXACTLY_ONCE) }
    return if (!predicate(this)) this else null
}
```

"조건을 만족하면 자기 자신, 아니면 null"을 반환한다. **`if`를 표현식 체인으로 바꿔주는 도구**다.

```kotlin
// Before
val validEmail = if (email.contains("@")) email else null

// After
val validEmail = email.takeIf { it.contains("@") }

// 엘비스와 결합하면 강력해진다
val port = System.getenv("PORT")
    ?.takeIf { it.isNotBlank() }
    ?.toIntOrNull()
    ?.takeIf { it in 1..65535 }
    ?: DEFAULT_PORT
```

### 1.7 `use` — try-with-resources

```kotlin
public inline fun <T : Closeable?, R> T.use(block: (T) -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    var exception: Throwable? = null
    try {
        return block(this)
    } catch (e: Throwable) {
        exception = e
        throw e
    } finally {
        this.closeFinally(exception)
    }
}
```

Java의 try-with-resources에 대응한다. Kotlin에는 try-with-resources 문법이 없고 이 함수를 쓴다.

```kotlin
connection.use { conn ->
    conn.prepareStatement(sql).use { stmt ->
        stmt.executeQuery().use { rs ->
            generateSequence { if (rs.next()) rs.toEntry() else null }.toList()
        }
    }
}
```

`closeFinally`는 **suppressed exception 처리**까지 해준다. 본문에서 예외가 났는데 `close()`에서도 예외가 나면, `close()`의 예외를 원래 예외에 `addSuppressed()`로 붙인다. 직접 `try-finally`를 쓰면 이 처리가 누락되어 원인 예외가 사라지는 사고가 난다.

### 1.8 `Nothing` — 값이 없는 타입

```kotlin
public class Nothing private constructor()
```

인스턴스를 만들 수 없는 클래스다. **모든 타입의 하위 타입**이라는 특성 때문에 여러 곳에서 쓰인다.

```kotlin
// 1) throw는 Nothing을 반환한다 → 어떤 타입 자리에도 올 수 있다
val user = findUser(id) ?: throw NotFoundException()   // user는 User 타입

// 2) return도 Nothing
val user = findUser(id) ?: return null

// 3) TODO()도 Nothing — 미구현 표시
fun calculate(): BigDecimal = TODO("정산 로직 미구현")

// 4) 컴파일러가 도달 불가 코드를 인식
fun fail(msg: String): Nothing = throw IllegalStateException(msg)

fun process(input: String?) {
    val value = input ?: fail("입력 없음")
    println(value.length)     // ✅ value는 String — fail()이 Nothing이라 여기 도달 = null 아님
}
```

`Nothing?`은 `null`만 담을 수 있는 타입이다. `listOf()`의 타입이 `List<Nothing>`인 이유도 여기에 있다.

### 1.9 `tailrec` — 꼬리 재귀 최적화

```kotlin
tailrec fun findRoot(node: Node): Node =
    if (node.parent == null) node else findRoot(node.parent)
```

컴파일러가 재귀를 **while 루프로 변환**해서 스택 오버플로를 방지한다. 조건은 "재귀 호출이 함수의 마지막 연산일 것"이다.

```kotlin
// ❌ tailrec 적용 불가 — 재귀 후에 곱셈이 남아 있음
tailrec fun factorial(n: Int): Int = if (n <= 1) 1 else n * factorial(n - 1)
//                                                       ↑ 재귀가 마지막이 아님 → 경고 발생

// ✅ 누적 파라미터로 변경하면 적용 가능
tailrec fun factorial(n: Int, acc: Int = 1): Int =
    if (n <= 1) acc else factorial(n - 1, acc * n)
```

조건을 만족하지 않으면 컴파일러가 **경고**를 낸다(에러가 아님). 경고를 놓치면 최적화 없이 그냥 재귀로 동작하므로, 깊은 재귀에서 `StackOverflowError`가 난다.

### 1.10 자주 쓰이지만 안 알려진 stdlib 함수

```kotlin
// repeat — 인덱스를 받는 반복
repeat(3) { i -> log.warn("재시도 ${i + 1}회") }

// generateSequence — 무한/조건부 시퀀스
generateSequence(1) { it * 2 }.take(10).toList()        // 1,2,4,8,...
generateSequence { readLine() }.forEach { process(it) } // null이 나올 때까지

// buildList / buildString / buildMap — 가변 빌더를 불변으로 마무리
val result = buildList {
    add("header")
    if (includeDetail) addAll(details)
    add("footer")
}   // 반환 타입은 불변 List

// associateBy / groupBy / partition
val byId = users.associateBy { it.id }                  // Map<Long, User>
val byRole = users.groupBy { it.role }                  // Map<Role, List<User>>
val (active, inactive) = users.partition { it.isActive } // Pair<List, List>

// chunked / windowed — 배치 처리에 유용
items.chunked(1000).forEach { batch -> repository.saveAll(batch) }
prices.windowed(3) { it.average() }                     // 이동 평균

// firstNotNullOfOrNull — map + first 조합을 한 번의 순회로
val found = resolvers.firstNotNullOfOrNull { it.resolve(key) }

// runningFold / scan — 누적 중간값
amounts.runningFold(BigDecimal.ZERO) { acc, v -> acc + v }   // 잔액 추이
```

---

## 2. 왜 알아야 하는가 (Why)

### 문법책이 채워주지 못하는 영역

문법책은 "이렇게 쓴다"를 알려주지만 "왜 이렇게 만들었는가"는 알려주지 않는다. stdlib 소스에는 설계 의도가 그대로 드러나 있다.

- `let`에 `inline`이 왜 붙었나 → 람다 객체 생성 비용 제거 + 비지역 반환 허용
- `require`의 메시지가 왜 람다인가 → 정상 경로에서 문자열 생성 비용 0
- `isNullOrBlank()` 뒤에 스마트 캐스트가 왜 되나 → `contract`

이 세 질문에 답할 수 있으면 "문법을 안다"에서 "언어를 이해한다"로 넘어간 것이다.

### 직접 만드는 유틸 함수의 품질이 달라진다

stdlib 패턴을 알면 팀 유틸 함수의 수준이 올라간다.

```kotlin
// ❌ 흔한 유틸 — 스마트 캐스트가 안 되고, 문자열이 항상 생성됨
fun validate(value: String?, message: String) {
    if (value == null) throw IllegalArgumentException(message)
}

fun handle(input: String?) {
    validate(input, "필수값 누락: ${input?.length}")
    println(input.length)     // ❌ 컴파일 에러 — 여전히 String?
}

// ✅ stdlib 패턴 적용
@OptIn(ExperimentalContracts::class)
inline fun validateNotNull(value: String?, lazyMessage: () -> String) {
    contract {
        returns() implies (value != null)
        callsInPlace(lazyMessage, InvocationKind.AT_MOST_ONCE)
    }
    if (value == null) throw IllegalArgumentException(lazyMessage())
}

fun handle(input: String?) {
    validateNotNull(input) { "필수값 누락" }
    println(input.length)     // ✅ 스마트 캐스트 동작
}
```

### 면접 관점

"스코프 함수 5개 차이를 설명해보세요"는 흔한 질문이다. 외운 표를 읊는 것과 시그니처로 설명하는 것은 인상이 다르다.

- ❌ "let은 it을 쓰고 결과를 반환하고, apply는 this를 쓰고 자기 자신을 반환하고..."
- ✅ "람다 타입이 `T.()`인지 `(T)`인지가 `this`/`it`을 가르고, 반환 타입이 `R`인지 `T`인지가 결과/자기자신을 가릅니다. 5개 함수는 이 2×2 조합에 `with`만 확장 함수가 아닌 형태입니다."

---

## 3. 내부 구현 분석 (How)

### contract가 컴파일러에 미치는 영향

`contract`는 런타임에 아무 일도 하지 않는다. 순수하게 **컴파일 타임 정보**다.

```kotlin
public inline fun require(value: Boolean, lazyMessage: () -> Any): Unit {
    contract { returns() implies value }
    if (!value) throw IllegalArgumentException(lazyMessage().toString())
}
```

```java
// 디컴파일 결과 — contract 블록은 흔적도 없다
public static final void require(boolean value, Function0 lazyMessage) {
    if (!value) {
        throw new IllegalArgumentException(lazyMessage().invoke().toString());
    }
}
```

컴파일러는 `contract` 정보를 **메타데이터**(`@kotlin.Metadata` 어노테이션)에 담아 저장하고, 호출부를 분석할 때 참조한다. 그래서 Java에서 호출하면 contract 효과가 전혀 없다.

### `contract` 사용 시 제약

직접 `contract`를 쓰려면 조건이 있다.

1. 함수 본문의 **첫 문장**이어야 한다
2. **최상위 함수**여야 한다 (멤버 함수/확장 프로퍼티 불가 — 실험적 제약)
3. `@OptIn(ExperimentalContracts::class)` 필요
4. contract 안에서는 **파라미터 참조만** 가능하다 (지역 변수 불가)

```kotlin
@OptIn(ExperimentalContracts::class)
fun Any?.isValidUser(): Boolean {
    contract { returns(true) implies (this@isValidUser is User) }
    return this is User && this.isActive
}

fun handle(obj: Any?) {
    if (obj.isValidUser()) {
        println(obj.email)      // ✅ User로 스마트 캐스트
    }
}
```

### `use`의 suppressed exception 처리

```kotlin
// 내부 구현 (JVM)
private fun Closeable?.closeFinally(cause: Throwable?) = when {
    this == null -> {}
    cause == null -> close()
    else -> try {
        close()
    } catch (closeException: Throwable) {
        cause.addSuppressed(closeException)    // 원인 예외를 보존
    }
}
```

직접 `try-finally`로 짠 코드와의 차이를 보자.

```kotlin
// ❌ 원인 예외가 사라진다
val stream = openStream()
try {
    process(stream)              // 여기서 BusinessException 발생
} finally {
    stream.close()               // 여기서도 IOException 발생 → BusinessException이 삼켜짐
}

// ✅ use — IOException이 suppressed로 붙고 BusinessException이 전파됨
openStream().use { process(it) }
```

장애 분석 시 "진짜 원인이 로그에 안 남는" 문제가 이 차이에서 나온다.

### `tailrec`의 바이트코드

```kotlin
tailrec fun sum(list: List<Int>, index: Int = 0, acc: Int = 0): Int =
    if (index == list.size) acc else sum(list, index + 1, acc + list[index])
```

```java
// 디컴파일 — 재귀가 사라지고 while 루프가 됨
public static int sum(List list, int index, int acc) {
    while (true) {
        if (index == list.size()) {
            return acc;
        }
        int newIndex = index + 1;
        int newAcc = acc + (Integer) list.get(index);
        index = newIndex;      // 파라미터를 재할당하고 루프 반복
        acc = newAcc;
    }
}
```

스택 프레임이 쌓이지 않으므로 리스트가 100만 개여도 안전하다.

### `inline` + `contract` + `Nothing`의 조합

`fail()` 패턴이 동작하는 원리를 정리하면 다음과 같다.

```mermaid
flowchart TD
    A["val v = input ?: fail(\"...\")"] --> B{컴파일러 분석}
    B --> C["fail의 반환 타입이 Nothing"]
    C --> D["Nothing은 모든 타입의 하위 타입<br/>→ 엘비스 오른쪽에 올 수 있음"]
    C --> E["Nothing = 정상 반환이 불가능<br/>→ 이 분기는 도달 후 종료"]
    E --> F["따라서 다음 줄에 도달했다면<br/>input은 null이 아니다"]
    F --> G["스마트 캐스트 적용"]
```

---

## 4. 실전 예제

### 예제 1: 정산 서비스의 방어 코드 정리

```kotlin
// Before — 검증과 로직이 뒤섞이고, 스마트 캐스트가 안 됨
@Service
class SettlementService(private val repository: SettlementRepository) {

    fun settle(request: SettlementRequest): Settlement {
        if (request.merchantId == null) {
            throw IllegalArgumentException("가맹점 ID 누락: " + request.toString())
        }
        if (request.amount == null || request.amount <= BigDecimal.ZERO) {
            throw IllegalArgumentException("금액 오류: " + request.amount)
        }
        val merchant = repository.findMerchant(request.merchantId!!)   // !! 필요
        if (merchant == null) {
            throw IllegalStateException("가맹점 없음")
        }
        if (merchant!!.status != Status.ACTIVE) {
            throw IllegalStateException("비활성 가맹점")
        }
        return doSettle(merchant!!, request.amount!!)
    }
}
```

```kotlin
// After — stdlib 활용
@Service
class SettlementService(private val repository: SettlementRepository) {

    fun settle(request: SettlementRequest): Settlement {
        val merchantId = requireNotNull(request.merchantId) {
            "가맹점 ID 누락: $request"                  // 정상 경로에선 문자열 생성 안 됨
        }
        val amount = requireNotNull(request.amount) { "금액 누락" }
        require(amount > BigDecimal.ZERO) { "금액은 0보다 커야 합니다: $amount" }

        val merchant = repository.findMerchant(merchantId)
            ?: error("가맹점을 찾을 수 없습니다: $merchantId")

        check(merchant.status == Status.ACTIVE) {
            "비활성 가맹점입니다: $merchantId, status=${merchant.status}"
        }

        return doSettle(merchant, amount)              // !! 없이 모두 non-null
    }
}
```

개선점:
- `requireNotNull`의 contract 덕분에 이후 코드에서 `!!` 불필요
- 메시지가 람다라 정상 경로에서 문자열 생성 비용 0
- `require`(인자 오류, 400) / `check`(상태 오류, 409) 구분으로 예외 → HTTP 상태 매핑이 명확해짐

### 예제 2: 설정 파싱 파이프라인

```kotlin
@Component
class KafkaProperties(private val env: Environment) {

    val bootstrapServers: String = requireEnv("KAFKA_BOOTSTRAP_SERVERS")

    val batchSize: Int = env.getProperty("kafka.batch-size")
        ?.takeIf { it.isNotBlank() }
        ?.toIntOrNull()
        ?.takeIf { it in 1..100_000 }
        ?: DEFAULT_BATCH_SIZE

    val topics: List<String> = buildList {
        add(env.getRequiredProperty("kafka.topic.main"))
        env.getProperty("kafka.topic.dlq")?.let(::add)
        env.getProperty("kafka.topic.retry")?.let(::add)
    }

    private fun requireEnv(key: String): String =
        env.getProperty(key)?.takeIf { it.isNotBlank() }
            ?: error("필수 환경변수 누락: $key")

    companion object {
        private const val DEFAULT_BATCH_SIZE = 500
    }
}
```

`takeIf` 체인으로 "값이 있고, 파싱되고, 범위 안일 때만 사용"을 선언적으로 표현했다. `if` 중첩이 사라진다.

### 예제 3: 배치 처리에 stdlib 활용

```kotlin
@Component
class LedgerBatchProcessor(
    private val repository: LedgerRepository,
    private val publisher: EventPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun processMonthly(yearMonth: YearMonth): BatchResult {
        val entries = repository.findByYearMonth(yearMonth)

        // partition — 한 번 순회로 두 그룹 분리
        val (valid, invalid) = entries.partition { it.isBalanced() }

        invalid.takeIf { it.isNotEmpty() }
            ?.also { log.warn("불균형 항목 {}건 제외", it.size) }
            ?.forEach { publisher.publish(UnbalancedDetected(it.id)) }

        // chunked — DB 배치 사이즈에 맞춰 분할 저장
        var saved = 0
        valid.chunked(BATCH_SIZE).forEach { batch ->
            repository.saveAll(batch)
            saved += batch.size
            log.debug("진행률: {}/{}", saved, valid.size)   // 람다라 debug 꺼져 있으면 비용 0
        }

        // groupBy + mapValues — 계정별 합계
        val byAccount = valid
            .groupBy { it.accountCode }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        return BatchResult(
            processed = saved,
            skipped = invalid.size,
            accountTotals = byAccount,
        )
    }

    companion object {
        private const val BATCH_SIZE = 1_000
    }
}
```

### 예제 4: 커스텀 contract 유틸

팀 공통 검증 함수에 contract를 적용하면 호출부가 깔끔해진다.

```kotlin
@OptIn(ExperimentalContracts::class)
inline fun <T : Any> T?.orThrow(lazyException: () -> Exception): T {
    contract {
        returns() implies (this@orThrow != null)
        callsInPlace(lazyException, InvocationKind.AT_MOST_ONCE)
    }
    return this ?: throw lazyException()
}

@OptIn(ExperimentalContracts::class)
inline fun <T> measureAndLog(label: String, block: () -> T): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val start = System.nanoTime()
    try {
        return block()
    } finally {
        val elapsed = (System.nanoTime() - start) / 1_000_000
        LoggerFactory.getLogger("perf").info("{} 소요: {}ms", label, elapsed)
    }
}

// 사용
fun handle(id: Long): Response {
    val result: Settlement                              // val인데 초기화 없이 선언
    measureAndLog("정산조회") {
        result = repository.findById(id)
            .orThrow { NotFoundException("정산 없음: $id") }
    }                                                   // ✅ EXACTLY_ONCE라 val 할당 허용
    return Response.of(result)                          // ✅ 초기화 완료로 인식
}
```

`EXACTLY_ONCE` contract가 없으면 `result`를 `val`로 선언할 수 없고 `var` + nullable로 만들어야 한다. contract 하나가 호출부 코드 품질을 바꾼다.

---

## 5. 정리

### stdlib 읽기 체크리스트

함수 하나를 읽을 때 확인할 것:

- [ ] `inline`인가? → 람다 비용 없음, 비지역 반환 가능
- [ ] `contract`가 있는가? → 스마트 캐스트/초기화 분석 효과
- [ ] 확장 함수인가 일반 함수인가? → 호출 형태가 달라짐
- [ ] 반환 타입이 `T`인가 `R`인가 `Nothing`인가?
- [ ] 람다 타입이 `T.()`인가 `(T)`인가? → `this` vs `it`
- [ ] 디컴파일하면 무엇이 되는가?

### 핵심 함수 요약

| 함수 | 시그니처 핵심 | 용도 |
|------|--------------|------|
| `let` | `(T) -> R`, 반환 `R` | null 체크 + 변환 |
| `also` | `(T) -> Unit`, 반환 `T` | 부수 효과 (로깅) |
| `apply` | `T.() -> Unit`, 반환 `T` | 객체 초기화 |
| `run` | `T.() -> R`, 반환 `R` | 수신 객체 컨텍스트 계산 |
| `takeIf` | 조건 만족 시 자기 자신 | `if`를 체인으로 |
| `takeUnless` | 조건 불만족 시 자기 자신 | 부정 조건 |
| `require` | `IllegalArgumentException` | 인자 검증 (호출자 잘못) |
| `check` | `IllegalStateException` | 상태 검증 (객체 상태 잘못) |
| `error` | 반환 `Nothing` | 도달 불가 분기 |
| `use` | `Closeable` 자동 해제 | try-with-resources |
| `TODO()` | 반환 `Nothing` | 미구현 표시 |
| `repeat` | 인덱스 반복 | 재시도 루프 |
| `buildList` | 가변 → 불변 | 조건부 리스트 구성 |
| `chunked` | 배치 분할 | 대량 저장 |
| `partition` | 두 그룹 분리 | 유효/무효 분리 |

### contract 요약

| 문법 | 의미 |
|------|------|
| `returns(true) implies (조건)` | true 반환 시 조건 참 |
| `returns() implies (조건)` | 예외 없이 반환 시 조건 참 |
| `returnsNotNull() implies (조건)` | null 아닌 값 반환 시 조건 참 |
| `callsInPlace(람다, EXACTLY_ONCE)` | 람다가 정확히 1회 호출 → `val` 초기화 가능 |

제약: 함수 본문 첫 문장 / 최상위 함수 / `@OptIn(ExperimentalContracts::class)` 필요 / 파라미터만 참조 가능

> **핵심 포인트**: stdlib는 최고의 Kotlin 교과서다. 함수 대부분이 5~10줄이고, 그 안에 `inline`, `contract`, `Nothing`, 확장 함수, 수신 객체 있는 람다가 전부 들어 있다. 새 함수를 쓸 때마다 `Cmd+B`로 구현을 확인하는 습관 하나가, 문법서 한 권보다 깊은 이해를 만든다. 특히 `contract`는 "왜 여기선 스마트 캐스트가 되고 저기선 안 되는가"라는 오래된 의문을 한 번에 해소해준다.

**관련 문서**
- [11-inline-performance.md](./11-inline-performance.md) — `inline`/`crossinline`/`reified` 상세
- [09-error-handling-patterns.md](./09-error-handling-patterns.md) — `Result`/`runCatching`/sealed 에러 모델
- [../main/07-scope-functions.md](../main/07-scope-functions.md) — 스코프 함수 선택 가이드
- [../main/13-labels-and-returns.md](../main/13-labels-and-returns.md) — 비지역 반환과 `inline`의 관계
- [../main/14-symbols-and-idioms.md](../main/14-symbols-and-idioms.md) — 기호 역인덱스

---
*참고: Kotlin 2.0 기준*
