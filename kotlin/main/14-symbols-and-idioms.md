# 기호와 관용구 역인덱스 (Symbols & Idioms)

Kotlin 학습이 막히는 지점은 대개 "개념을 몰라서"가 아니라 **"코드를 읽다가 처음 보는 기호를 만나서"** 다. `::`, `?:`, `@field:`, `<*>`, `*args` 같은 기호는 검색조차 어렵다. 이 문서는 개념이 아니라 **기호에서 출발해 찾아보는 역방향 인덱스**다. 오픈소스나 AI가 생성한 코드를 읽다 막혔을 때 이 문서에서 기호를 찾으면 된다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### 1.1 람다·함수 관련 기호

| 기호 | 이름 | 의미 |
|------|------|------|
| `it` | 암묵적 파라미터 | 파라미터가 하나인 람다의 기본 이름 |
| `_` | 미사용 파라미터 | 쓰지 않는 파라미터 자리 표시 |
| `->` | 화살표 | 람다 파라미터 구분 / `when` 분기 / 함수 타입 |
| `::` | 참조 연산자 | 함수·프로퍼티·생성자·클래스 참조 |
| `{ }` 밖으로 뺀 람다 | trailing lambda | 마지막 파라미터가 함수면 괄호 밖으로 |

```kotlin
list.filter { it > 0 }                       // it: 단일 파라미터
map.forEach { (_, value) -> print(value) }   // _: key는 안 쓴다
val f: (Int) -> String = { it.toString() }   // ->: 함수 타입 + 람다

// :: 의 네 가지 용법
list.map(String::uppercase)        // 멤버 함수 참조
list.map(::parseUser)              // 최상위 함수 참조
list.map(::User)                   // 생성자 참조
val kClass = User::class           // 클래스 참조 (KClass)
val jClass = User::class.java      // Java Class — Spring/Jackson에 넘길 때 필요

// trailing lambda: 아래 둘은 완전히 같다
items.forEach({ println(it) })
items.forEach { println(it) }
repository.findAll(pageable) { it.isActive }   // 마지막만 밖으로
```

> `::class` 는 Kotlin의 `KClass`, `::class.java` 는 Java의 `Class`다. Spring의 `getBean()`, Jackson의 `readValue()` 등 Java API에는 `.java`를 붙여야 한다.

### 1.2 null 관련 기호

| 기호 | 이름 | 의미 |
|------|------|------|
| `?` | nullable 타입 | `String?` — null이 될 수 있는 타입 |
| `?.` | 안전 호출(safe call) | 수신 객체가 null이면 전체가 null |
| `?:` | 엘비스 연산자 | 왼쪽이 null이면 오른쪽 값 사용 |
| `!!` | not-null 단언 | null이면 NPE 발생 (최후의 수단) |
| `as?` | 안전 캐스트 | 캐스팅 실패 시 예외 대신 null |

```kotlin
val name: String? = user?.profile?.name          // 체인 중 하나라도 null이면 null
val display = name ?: "익명"                      // 엘비스
val length = name!!.length                        // null이면 NPE — 지양
val dto = payload as? UserDto ?: return           // 캐스팅 실패 시 이탈
```

**실무에서 가장 많이 쓰는 엘비스 관용구** — 오른쪽에 `return`/`throw`를 두는 조기 이탈:

```kotlin
fun activate(id: Long): Result {
    val user = repository.findByIdOrNull(id)
        ?: return Result.NotFound(id)             // 조기 반환

    val email = user.email
        ?: throw IllegalStateException("이메일 없음: $id")   // 조기 예외

    // 이 아래로는 user, email이 모두 non-null로 스마트 캐스트됨
    return Result.Success(user.activate(email))
}
```

`return`과 `throw`는 `Nothing` 타입을 반환하는데, `Nothing`은 모든 타입의 하위 타입이라 엘비스 오른쪽에 올 수 있다. 이 덕분에 위 코드가 성립한다.

### 1.3 `@` 의 다섯 가지 얼굴

같은 `@` 기호가 문맥에 따라 전혀 다른 의미를 갖는다. 가장 헷갈리는 부분이다.

```kotlin
// (1) 어노테이션
@Service
class UserService

// (2) 라벨 정의 — 반복문/람다 앞
outer@ for (i in 1..10) { }
list.forEach item@{ }

// (3) 라벨 참조 — return/break/continue 뒤
list.forEach { return@forEach }
for (i in 1..10) { break@outer }

// (4) this 한정 — 어느 계층의 수신 객체인가
this@UserService.log("...")

// (5) 어노테이션 사용 지점 타겟 — 콜론과 함께
data class SignUpRequest(
    @field:NotBlank val email: String,
    @get:JsonProperty("user_name") val name: String,
)
```

(5)번이 Spring 백엔드에서 실전 함정이다. Kotlin의 생성자 프로퍼티는 **필드, getter, 생성자 파라미터**를 동시에 만들어내는데, 어노테이션을 그냥 붙이면 기본 타겟(보통 생성자 파라미터)에만 적용된다.

```kotlin
// ❌ 검증이 동작하지 않을 수 있다
data class SignUpRequest(@NotBlank val email: String)

// ✅ 필드에 명시적으로 적용
data class SignUpRequest(@field:NotBlank val email: String)
```

사용 지점 타겟 종류: `@field:`, `@get:`, `@set:`, `@param:`, `@property:`, `@setparam:`, `@delegate:`, `@receiver:`

### 1.4 타입·제네릭 기호

| 기호 | 의미 |
|------|------|
| `is` / `!is` | 타입 검사 (검사 후 스마트 캐스트) |
| `as` / `as?` | 타입 캐스팅 / 안전 캐스팅 |
| `out T` | 공변(covariant) — 생산자(Producer) |
| `in T` | 반공변(contravariant) — 소비자(Consumer) |
| `<*>` | 스타 프로젝션 — 타입 인자를 모를 때 |
| `*args` | 스프레드 연산자 — 배열을 vararg로 펼침 |
| `reified T` | 런타임에 타입 정보 보존 (inline 함수에서만) |

```kotlin
if (payload is UserDto) {
    println(payload.email)          // 스마트 캐스트 — 별도 캐스팅 불필요
}

fun printAll(vararg items: String) { }
val arr = arrayOf("a", "b")
printAll(*arr)                       // 스프레드 — 배열을 개별 인자로 펼침

fun describe(list: List<*>) = "크기 ${list.size}"   // 스타 프로젝션 — 원소 타입은 모름

inline fun <reified T> parse(json: String): T = mapper.readValue(json, T::class.java)
val user = parse<User>(body)         // reified 덕분에 T::class.java 사용 가능
```

`*` 는 곱셈, 스프레드, 스타 프로젝션 세 가지로 쓰인다. **타입 위치에 있으면 스타 프로젝션, 인자 앞에 있으면 스프레드**로 읽으면 된다.

### 1.5 범위·컬렉션 기호

```kotlin
for (i in 1..10) { }            // 1 이상 10 이하 (닫힌 범위)
for (i in 1..<10) { }           // 1 이상 10 미만 (Kotlin 1.9+, until과 동일)
for (i in 1 until 10) { }       // 1 이상 10 미만
for (i in 10 downTo 1) { }      // 역순
for (i in 1..10 step 2) { }     // 2씩 증가

if (code in 200..299) { }       // 범위 포함 검사
if (name !in blockList) { }     // 미포함 검사
if (key in map) { }             // Map의 키 존재 검사 (containsKey)

val pair = "key" to "value"     // to — Pair 생성 (infix 함수)
val map = mapOf("a" to 1, "b" to 2)

for ((key, value) in map) { }   // 구조 분해 — Map 순회
val (id, name) = user           // 구조 분해 — data class의 component1(), component2()
```

`in`은 문맥에 따라 세 가지다: **for문 순회**, **포함 검사(`contains`)**, **반공변 선언**. 헷갈리면 `for` 뒤인지, `if` 안인지, 타입 파라미터 자리인지를 보면 된다.

### 1.6 문자열 기호

```kotlin
val msg = "안녕, $name"                    // 단순 변수
val info = "총 ${items.size}건"            // 표현식은 중괄호 필수
val price = "가격: ${'$'}$amount"          // 달러 기호 자체를 출력

val sql = """
    SELECT *
      FROM users
     WHERE status = 'ACTIVE'
""".trimIndent()                           // raw string — 이스케이프 불필요

val json = """{"name": "홍길동"}"""         // 따옴표 이스케이프 없이 사용
```

`"""` 안에서는 `\n`, `\"` 같은 이스케이프가 동작하지 않는다. 그대로 출력된다. `trimIndent()`는 공통 들여쓰기를 제거해준다.

### 1.7 선언 키워드

| 키워드 | 의미 | 예시 |
|--------|------|------|
| `by` | 위임(delegation) | `val x by lazy { }`, `class A : B by impl` |
| `infix` | 중위 표기 허용 | `1 to 2`, `a shl 2` |
| `operator` | 연산자 오버로딩 | `operator fun plus(o: Money)` → `a + b` |
| `typealias` | 타입 별칭 | `typealias UserMap = Map<Long, User>` |
| `fun interface` | SAM 인터페이스 | 람다로 구현 가능 |
| `const val` | 컴파일 타임 상수 | 어노테이션 인자로 사용 가능 |
| `lateinit var` | 지연 초기화 | non-null인데 나중에 주입 |
| `@JvmInline value class` | 값 클래스 | 런타임 래핑 비용 없는 타입 안전성 |
| `internal` | 모듈 내부 공개 | 같은 컴파일 모듈에서만 접근 |
| `sealed` | 봉인 클래스/인터페이스 | 하위 타입을 컴파일 타임에 고정 |

### 1.8 기타 자주 보는 기호

```kotlin
// 백틱 — 예약어를 식별자로 쓰거나, 테스트 함수명에 공백 사용
val `object` = javaObject
@Test fun `잔액이 부족하면 예외가 발생한다`() { }

// 숫자 가독성 구분자
val maxAmount = 1_000_000_000L

// 참조 동등성 (Java의 ==)  vs  구조적 동등성 (Java의 equals)
a == b       // equals() 호출 — Kotlin의 == 는 equals다
a === b      // 참조 비교 — Java의 == 에 해당
a !== b

// trailing comma — 마지막 인자 뒤 쉼표 허용 (git diff가 깔끔해짐)
data class User(
    val id: Long,
    val name: String,
)

// 커스텀 getter와 backing field
class Account(initial: BigDecimal) {
    var balance: BigDecimal = initial
        get() = field.setScale(2, RoundingMode.HALF_UP)   // field = 실제 저장 공간
        private set                                        // setter만 private

    val isEmpty: Boolean                                   // backing field 없음
        get() = balance == BigDecimal.ZERO                 // 매번 계산
}
```

`field`는 **커스텀 접근자 안에서만 쓸 수 있는 특별한 식별자**로, 프로퍼티의 실제 저장 공간(backing field)을 가리킨다. `get() = balance`라고 쓰면 getter가 자기 자신을 호출해 스택 오버플로가 나므로, 반드시 `field`를 써야 한다.

---

## 2. 왜 알아야 하는가 (Why)

### AI 시대에 더 중요해진 능력

AI가 코드를 생성해주는 환경에서 개발자의 핵심 역량은 **작성(write)에서 판별(read & judge)로 이동**했다. 생성된 코드를 읽고 "이건 프로덕션에서 터진다"를 즉시 알아채야 하는데, 기호 하나에서 막히면 판별 자체가 불가능하다.

```kotlin
// AI가 생성한 코드 — 이 안에 문제가 있다. 보이는가?
data class OrderRequest(
    @NotBlank
    val orderNo: String,
    @Min(1)
    val quantity: Int,
)
```

`@field:`가 없어서 Bean Validation이 동작하지 않을 수 있다. 기호를 알아야 잡아낼 수 있는 결함이다.

### 검색이 어려운 문법이라는 문제

`?:`나 `::`는 구글 검색이 사실상 불가능하다. 개념 이름(엘비스 연산자, 함수 참조)을 알아야 검색할 수 있는데, 이름을 모르니 검색을 못 하는 악순환이 생긴다. 이 문서는 그 진입점 역할을 한다.

### 면접 관점

면접에서 코드를 보여주고 "이 코드 설명해보세요"라고 하는 경우가 늘고 있다. 기호를 정확한 이름으로 부르는 것만으로 인상이 달라진다.

- ❌ "여기 물음표 콜론으로 기본값 주고..."
- ✅ "엘비스 연산자로 null일 때 조기 반환합니다. `return`이 `Nothing` 타입이라 가능한 패턴이죠."

---

## 3. 내부 구현 분석 (How)

### `?.` 와 `?:` 의 바이트코드

```kotlin
val length = user?.name?.length ?: 0
```

```java
// 디컴파일 결과 (개념적 표현)
User user = ...;
Integer tmp;
if (user == null) {
    tmp = null;
} else {
    String name = user.getName();
    tmp = (name == null) ? null : name.length();
}
int length = (tmp == null) ? 0 : tmp;
```

안전 호출은 **단순한 null 분기**로 컴파일된다. 마법이 아니라 컴파일러가 `if (x == null)`을 대신 써주는 것이다. 다만 체인이 길어지면 박싱(`Integer`)이 발생할 수 있다는 점은 알아둘 만하다.

### `::` 함수 참조의 실체

```kotlin
list.map(String::uppercase)
```

함수 참조는 `Function1` 구현 객체로 컴파일된다. 람다 `{ it.uppercase() }`와 바이트코드상 거의 동일하다. 다만 **`inline` 함수에 넘길 때는 인라인되지 않는 경우가 있어**, 성능이 민감한 루프에서는 람다가 유리할 수 있다.

### `value class`의 언박싱

```kotlin
@JvmInline
value class AccountNo(val value: String)

fun transfer(from: AccountNo, to: AccountNo) { }
```

```java
// 디컴파일 — AccountNo 객체가 아예 생성되지 않는다
public static void transfer(String from, String to) { }
```

`value class`는 컴파일 타임에만 존재하고 런타임에는 내부 값으로 대체된다. 타입 안전성은 얻으면서 래핑 비용은 0이다. 단, nullable로 쓰거나(`AccountNo?`) 제네릭 인자로 쓰면 박싱이 발생한다.

### `const val` vs `val`

```kotlin
object Config {
    const val MAX_RETRY = 3      // 컴파일 타임 상수
    val timeout = 30             // 런타임 초기화 + getter
}
```

```java
// const val — 사용처에 값이 그대로 박힌다 (인라인)
int retry = 3;

// val — getter 호출
int t = Config.INSTANCE.getTimeout();
```

`const val`은 어노테이션 인자(`@Retryable(maxAttempts = MAX_RETRY)`)로 쓸 수 있지만 `val`은 못 쓴다. 이 차이가 여기서 나온다.

### 구조 분해의 정체

```kotlin
val (id, name) = user
```

```java
// 디컴파일
long id = user.component1();
String name = user.component2();
```

구조 분해는 **`componentN()` 함수 호출**로 변환된다. `data class`가 이 함수들을 자동 생성해주기 때문에 동작하는 것이다. 일반 클래스도 `operator fun component1()`을 직접 정의하면 구조 분해가 가능해진다.

**주의**: 구조 분해는 **순서 기반**이다. `data class`의 프로퍼티 순서를 바꾸면 구조 분해하는 모든 코드가 조용히 잘못된 값을 받는다. 컴파일 에러도 안 난다.

```kotlin
data class Point(val x: Int, val y: Int)
val (x, y) = point

// 나중에 누군가 순서를 바꾸면...
data class Point(val y: Int, val x: Int)   // ❌ 위 코드는 여전히 컴파일되지만 값이 뒤바뀜
```

---

## 4. 실전 예제

### 예제 1: 실전 코드 해독 훈련

아래는 실무에서 볼 법한 코드다. 기호 하나하나를 짚어보자.

```kotlin
@Service
class SettlementService(
    private val repository: SettlementRepository,
    private val client: PaymentClient,
) {
    fun settle(merchantId: Long, date: LocalDate): SettlementResult =
        repository.findByMerchantAndDate(merchantId, date)
            ?.takeIf { it.status == Status.PENDING }
            ?.let { settlement ->
                val amounts = settlement.items
                    .filterNot { it.isRefunded }
                    .map(SettlementItem::amount)
                    .takeIf { it.isNotEmpty() }
                    ?: return SettlementResult.Empty

                SettlementResult.Success(
                    total = amounts.reduce(BigDecimal::add),
                    count = amounts.size,
                )
            }
            ?: SettlementResult.NotFound(merchantId, date)
}
```

해독:

| 위치 | 기호 | 읽는 법 |
|------|------|---------|
| `): SettlementResult =` | `=` | 단일 표현식 함수 — 본문이 표현식 하나 |
| `?.takeIf { }` | `?.` | 앞이 null이면 건너뜀 |
| `takeIf { }` | — | 조건을 만족하면 자기 자신, 아니면 null |
| `?.let { settlement -> }` | `->` | `it` 대신 이름을 붙임 (가독성) |
| `.map(SettlementItem::amount)` | `::` | 프로퍼티 참조 — `{ it.amount }`와 동일 |
| `?: return ...` | `?:` | null이면 **함수 전체**를 조기 반환 |
| `BigDecimal::add` | `::` | 멤버 함수 참조 — `{ a, b -> a.add(b) }` |
| 마지막 `?: SettlementResult.NotFound` | `?:` | 체인 전체가 null이면 기본값 |

여기서 **중요한 포인트**: 중간의 `?: return SettlementResult.Empty`는 `let` 람다가 아니라 `settle` 함수 전체를 종료시킨다. `let`이 `inline` 함수이기 때문에 가능한 비지역 반환이다. (참고: [13-labels-and-returns.md](./13-labels-and-returns.md))

### 예제 2: Spring DTO에서 어노테이션 타겟 실수

```kotlin
// ❌ Before — 검증이 동작하지 않음
data class TransferRequest(
    @NotBlank
    val fromAccount: String,
    @Positive
    val amount: BigDecimal,
    @JsonProperty("memo_text")
    val memo: String?,
)

// ✅ After — 명시적 타겟 지정
data class TransferRequest(
    @field:NotBlank(message = "출금 계좌는 필수입니다")
    val fromAccount: String,

    @field:Positive(message = "금액은 0보다 커야 합니다")
    val amount: BigDecimal,

    @param:JsonProperty("memo_text")
    @get:JsonProperty("memo_text")
    val memo: String?,
)
```

- Bean Validation은 **필드**를 읽으므로 `@field:`
- Jackson 역직렬화는 **생성자 파라미터**를 보므로 `@param:`, 직렬화는 **getter**를 보므로 `@get:`

이 실수는 컴파일도 통과하고 테스트에서도 놓치기 쉬워서, 프로덕션에서 "검증이 왜 안 되지?"로 나타난다.

### 예제 3: 커스텀 getter와 `field`

```kotlin
@Entity
class Ledger(
    @Column(name = "total_amount")
    var totalAmount: BigDecimal = BigDecimal.ZERO,
) {
    // backing field 있음 — field로 실제 저장 공간 접근
    var status: LedgerStatus = LedgerStatus.OPEN
        set(value) {
            require(field != LedgerStatus.CLOSED) { "마감된 장부는 상태 변경 불가" }
            field = value
        }

    // backing field 없음 — 매번 계산되는 파생 프로퍼티
    val isClosed: Boolean
        get() = status == LedgerStatus.CLOSED

    // ⚠️ JPA 엔티티에서는 파생 프로퍼티에 @Transient가 필요할 수 있다
    @get:Transient
    val displayAmount: String
        get() = totalAmount.setScale(0, RoundingMode.DOWN).toPlainString()
}
```

`get() = status == ...` 처럼 backing field를 참조하지 않으면 필드가 생성되지 않는다. JPA는 getter를 보고 매핑을 시도하므로, 계산 프로퍼티에는 `@get:Transient`를 붙여야 할 수 있다. 여기서도 사용 지점 타겟이 등장한다.

### 예제 4: 스프레드 연산자와 vararg

```kotlin
fun buildQuery(base: String, vararg conditions: String): String =
    if (conditions.isEmpty()) base
    else "$base WHERE ${conditions.joinToString(" AND ")}"

// 개별 인자로 호출
buildQuery("SELECT * FROM users", "status = 'ACTIVE'", "age > 20")

// 배열/리스트를 펼쳐서 전달
val filters = listOf("status = 'ACTIVE'", "age > 20")
buildQuery("SELECT * FROM users", *filters.toTypedArray())   // * 스프레드 필수
```

`vararg` 자리에 컬렉션을 그냥 넘길 수 없다. 배열로 바꾼 뒤 `*`로 펼쳐야 한다. Java의 `varargs`에 배열을 그대로 넘길 수 있던 것과 다르다.

---

## 5. 정리

### 기호 빠른 조회표

| 기호 | 이름 | 한 줄 요약 |
|------|------|-----------|
| `it` | 암묵적 파라미터 | 단일 파라미터 람다의 기본 이름 |
| `_` | 언더스코어 | 미사용 파라미터 / 숫자 자릿수 구분 |
| `->` | 화살표 | 람다 파라미터 / when 분기 / 함수 타입 |
| `::` | 참조 연산자 | 함수·프로퍼티·생성자·클래스 참조 |
| `?` | nullable 표시 | null이 될 수 있는 타입 |
| `?.` | 안전 호출 | null이면 전체가 null |
| `?:` | 엘비스 연산자 | null이면 오른쪽 사용 (`?: return` 관용구) |
| `!!` | not-null 단언 | null이면 NPE — 최후의 수단 |
| `as?` | 안전 캐스트 | 실패 시 null |
| `@어노테이션` | 어노테이션 | 메타데이터 |
| `라벨@` | 라벨 정의 | 반복문/람다 이름 붙이기 |
| `return@라벨` | 라벨 참조 | 지역 반환 |
| `this@이름` | this 한정 | 어느 계층의 수신 객체인지 지정 |
| `@field:` `@get:` `@param:` | 사용 지점 타겟 | 어노테이션을 어디에 붙일지 지정 |
| `*배열` | 스프레드 | 배열을 vararg로 펼침 |
| `<*>` | 스타 프로젝션 | 타입 인자를 모를 때 |
| `out` / `in` | 변성 | 공변(생산자) / 반공변(소비자) |
| `is` / `!is` | 타입 검사 | 검사 후 스마트 캐스트 |
| `===` / `!==` | 참조 동등성 | Java의 `==` |
| `==` | 구조적 동등성 | Java의 `equals()` |
| `..` / `..<` / `until` | 범위 | 닫힌 범위 / 반열린 범위 |
| `downTo` / `step` | 범위 제어 | 역순 / 증분 |
| `in` | 포함 검사 | `contains()` 호출 |
| `to` | Pair 생성 | infix 함수 |
| `$` / `${}` | 문자열 템플릿 | 변수 / 표현식 삽입 |
| `"""` | raw string | 이스케이프 없는 여러 줄 문자열 |
| `by` | 위임 | 프로퍼티 위임 / 클래스 위임 |
| `field` | backing field | 커스텀 접근자 안에서만 유효 |
| `` `name` `` | 백틱 | 예약어 이스케이프 / 테스트 함수명 |
| `1_000` | 숫자 구분자 | 가독성용, 값에는 영향 없음 |

### 헷갈리기 쉬운 다의어 기호

| 기호 | 문맥 1 | 문맥 2 | 문맥 3 |
|------|--------|--------|--------|
| `@` | 어노테이션 | 라벨 | 사용 지점 타겟 (`@field:`) |
| `*` | 곱셈 | 스프레드 (`*args`) | 스타 프로젝션 (`List<*>`) |
| `in` | for문 순회 | 포함 검사 | 반공변 선언 |
| `out` | — | 공변 선언 | (다른 용도 없음) |
| `it` | 람다 파라미터 | (중첩 시 바깥 `it` 가려짐) | — |
| `->` | 람다 | when 분기 | 함수 타입 선언 |

> **핵심 포인트**: Kotlin 학습의 실질적 병목은 개념이 아니라 **검색할 이름을 모르는 기호**다. 기호를 만날 때마다 정확한 이름을 확인하고 "이게 컴파일되면 뭐가 되는가"를 한 번 확인하면, 같은 기호에서 두 번 막히지 않는다. 특히 `@`의 다섯 가지 용법과 `@field:`류 사용 지점 타겟은 Spring 백엔드에서 실제 버그로 이어지므로 반드시 구분할 것.

**관련 문서**
- [13-labels-and-returns.md](./13-labels-and-returns.md) — `@` 라벨 상세
- [02-type-system-null-safety.md](./02-type-system-null-safety.md) — null 관련 연산자 심화
- [11-generics-variance.md](./11-generics-variance.md) — `out`/`in`/`<*>` 변성 상세
- [09-lambdas-higher-order.md](./09-lambdas-higher-order.md) — `::` 함수 참조 상세
- [../advanced/08-java-interop.md](../advanced/08-java-interop.md) — `@Jvm*` 어노테이션

---
*참고: Kotlin 2.0 기준*
