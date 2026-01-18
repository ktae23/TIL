# Kotlin 기초 문법

Kotlin의 핵심 문법을 Java와 비교하며 정리합니다. 변수/함수/클래스 기초부터 Null Safety, 확장 함수 등 코틀린스러운 문법까지 다룹니다.

## 목차

- [변수 선언](#변수-선언)
- [함수 정의](#함수-정의)
- [클래스와 생성자](#클래스와-생성자)
- [Null Safety](#null-safety)
- [확장 함수](#확장-함수)
- [스코프 함수](#스코프-함수)
- [Data Class](#data-class)
- [기타 코틀린스러운 문법](#기타-코틀린스러운-문법)

---

## 변수 선언

### val vs var

```kotlin
val immutable = "변경 불가"  // Java의 final
var mutable = "변경 가능"

mutable = "새 값"  // OK
// immutable = "에러"  // 컴파일 에러
```

### 타입 추론

```kotlin
val name = "Kotlin"        // String으로 추론
val count = 42             // Int로 추론
val price = 19.99          // Double로 추론

// 명시적 타입 지정
val explicit: String = "명시적"
```

### Java vs Kotlin 비교

```java
// Java
final String name = "Java";
int count = 42;
```

```kotlin
// Kotlin
val name = "Kotlin"
var count = 42
```

---

## 함수 정의

### 기본 함수

```kotlin
fun greet(name: String): String {
    return "Hello, $name!"
}

// 단일 표현식 함수 (= 사용)
fun greet(name: String): String = "Hello, $name!"

// 반환 타입 추론
fun greet(name: String) = "Hello, $name!"
```

### 기본값과 명명된 인자

```kotlin
fun createUser(
    name: String,
    age: Int = 0,           // 기본값
    email: String = ""
): User {
    return User(name, age, email)
}

// 호출
createUser("Kim")
createUser("Kim", 25)
createUser("Kim", email = "kim@test.com")  // 명명된 인자
createUser(name = "Kim", age = 25, email = "kim@test.com")
```

### Unit 타입 (void 대체)

```kotlin
fun printMessage(msg: String): Unit {
    println(msg)
}

// Unit은 생략 가능
fun printMessage(msg: String) {
    println(msg)
}
```

---

## 클래스와 생성자

### 기본 클래스

```kotlin
class Person(val name: String, var age: Int)

// 사용
val person = Person("Kim", 25)  // new 키워드 없음
println(person.name)  // getter 자동 생성
person.age = 26       // setter (var만)
```

### 주 생성자와 init 블록

```kotlin
class Person(val name: String, var age: Int) {

    init {
        require(age >= 0) { "나이는 0 이상이어야 합니다" }
        println("Person 생성: $name")
    }
}
```

### 부 생성자

```kotlin
class Person(val name: String, var age: Int) {

    var email: String = ""

    constructor(name: String, age: Int, email: String) : this(name, age) {
        this.email = email
    }
}
```

### Java vs Kotlin 클래스 비교

```java
// Java - 많은 보일러플레이트
public class Person {
    private final String name;
    private int age;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() { return name; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
}
```

```kotlin
// Kotlin - 한 줄로 동일한 기능
class Person(val name: String, var age: Int)
```

---

## Null Safety

Kotlin의 가장 강력한 특징 중 하나입니다. 컴파일 타임에 NPE를 방지합니다.

### Nullable 타입

```kotlin
var nonNull: String = "hello"
// nonNull = null  // 컴파일 에러!

var nullable: String? = "hello"  // ?로 nullable 선언
nullable = null  // OK
```

### Safe Call (?.)

```kotlin
val name: String? = null

// Safe call - null이면 null 반환
val length: Int? = name?.length

// 체이닝
val city: String? = user?.address?.city
```

### Elvis 연산자 (?:)

```kotlin
val name: String? = null

// null이면 기본값 사용
val displayName = name ?: "Unknown"

// 함수 조기 반환에 활용
fun process(name: String?) {
    val validName = name ?: return
    println(validName)
}
```

### Not-null 단언 (!!)

```kotlin
val name: String? = "Kotlin"

// null이 아님을 단언 (NPE 가능성 있음)
val length = name!!.length

// 주의: 확실한 경우에만 사용
```

### let과 함께 사용

```kotlin
val name: String? = "Kotlin"

// null이 아닐 때만 블록 실행
name?.let {
    println("Name is $it")
    println("Length is ${it.length}")
}
```

### 스마트 캐스트

```kotlin
fun printLength(str: String?) {
    if (str != null) {
        // 이 블록 안에서는 자동으로 String으로 캐스트
        println(str.length)
    }
}

// when과 함께
fun describe(obj: Any): String = when (obj) {
    is Int -> "정수: ${obj + 1}"      // 자동 캐스트
    is String -> "문자열 길이: ${obj.length}"
    else -> "알 수 없음"
}
```

---

## 확장 함수

기존 클래스를 수정하지 않고 새로운 함수를 추가합니다.

### 기본 확장 함수

```kotlin
// String에 확장 함수 추가
fun String.addExclamation(): String {
    return "$this!"
}

// 사용
val greeting = "Hello".addExclamation()  // "Hello!"
```

### 실용적인 예제들

```kotlin
// 리스트 확장
fun <T> List<T>.secondOrNull(): T? = if (size >= 2) this[1] else null

val list = listOf(1, 2, 3)
println(list.secondOrNull())  // 2

// Int 확장
fun Int.isEven(): Boolean = this % 2 == 0

println(4.isEven())  // true

// 날짜 포맷팅
fun LocalDate.toKoreanFormat(): String {
    return "${year}년 ${monthValue}월 ${dayOfMonth}일"
}
```

### 확장 프로퍼티

```kotlin
val String.lastChar: Char
    get() = this[length - 1]

println("Kotlin".lastChar)  // n
```

### Nullable 타입 확장

```kotlin
fun String?.orEmpty(): String = this ?: ""

val name: String? = null
println(name.orEmpty())  // ""
```

---

## 스코프 함수

객체 컨텍스트 내에서 코드 블록을 실행하는 함수들입니다.

### let

```kotlin
// null 체크와 함께 사용
val name: String? = "Kotlin"
name?.let {
    println("Name: $it")
}

// 변환에 사용
val numbers = listOf(1, 2, 3)
val result = numbers.first().let { it * it }  // 1
```

### apply

```kotlin
// 객체 초기화에 유용 (this 사용, 객체 자신 반환)
val person = Person().apply {
    name = "Kim"
    age = 25
    email = "kim@test.com"
}
```

### also

```kotlin
// 부수 효과 (it 사용, 객체 자신 반환)
val numbers = mutableListOf(1, 2, 3)
    .also { println("초기 리스트: $it") }
    .also { it.add(4) }
    .also { println("추가 후: $it") }
```

### run

```kotlin
// 객체 초기화 + 결과 계산 (this 사용, 람다 결과 반환)
val greeting = Person("Kim", 25).run {
    "Hello, I'm $name and $age years old"
}
```

### with

```kotlin
// 이미 생성된 객체에 여러 작업 (비확장 함수)
val person = Person("Kim", 25)
val info = with(person) {
    println(name)
    println(age)
    "$name ($age)"  // 반환값
}
```

### 스코프 함수 선택 가이드

| 함수 | 객체 참조 | 반환값 | 주 용도 |
|------|----------|--------|---------|
| let | it | 람다 결과 | null 체크, 변환 |
| apply | this | 객체 자신 | 객체 초기화 |
| also | it | 객체 자신 | 부수 효과, 로깅 |
| run | this | 람다 결과 | 초기화 + 계산 |
| with | this | 람다 결과 | 그룹화된 작업 |

---

## Data Class

```kotlin
data class User(
    val id: Long,
    val name: String,
    val email: String
)

// 자동 생성되는 것들
val user1 = User(1, "Kim", "kim@test.com")
val user2 = User(1, "Kim", "kim@test.com")

println(user1)                    // toString()
println(user1 == user2)           // equals() - true
println(user1.hashCode())         // hashCode()
val user3 = user1.copy(name = "Lee")  // copy()

// 구조 분해
val (id, name, email) = user1
```

### Java의 Record와 비교

```java
// Java 16+ Record
public record User(Long id, String name, String email) {}
```

```kotlin
// Kotlin Data Class - copy() 등 더 많은 기능
data class User(val id: Long, val name: String, val email: String)
```

---

## 기타 코틀린스러운 문법

### 문자열 템플릿

```kotlin
val name = "Kotlin"
val age = 10

println("$name is $age years old")
println("${name.uppercase()} has ${name.length} characters")
```

### when 표현식

```kotlin
// Java switch의 강화판
val result = when (x) {
    1 -> "one"
    2, 3 -> "two or three"
    in 4..10 -> "between 4 and 10"
    is String -> "it's a string"
    else -> "unknown"
}

// 조건 분기로도 사용
val grade = when {
    score >= 90 -> "A"
    score >= 80 -> "B"
    score >= 70 -> "C"
    else -> "F"
}
```

### 범위와 순회

```kotlin
// 범위
val range = 1..5      // 1, 2, 3, 4, 5
val until = 1 until 5  // 1, 2, 3, 4

// 역순
for (i in 5 downTo 1) { }

// step
for (i in 1..10 step 2) { }  // 1, 3, 5, 7, 9

// in 연산자
if (x in 1..10) { }
```

### 싱글톤 (object)

```kotlin
// 싱글톤 선언
object DatabaseConfig {
    val url = "jdbc:mysql://localhost:3306/db"
    fun connect() { }
}

// 사용
DatabaseConfig.connect()
```

### Companion Object

```kotlin
class User private constructor(val name: String) {

    companion object {
        fun create(name: String): User {
            return User(name)
        }
    }
}

// Java의 static 메서드처럼 사용
val user = User.create("Kim")
```

### 지연 초기화

```kotlin
// lazy - val에 사용, 첫 접근 시 초기화
val heavyObject: HeavyObject by lazy {
    println("초기화 중...")
    HeavyObject()
}

// lateinit - var에 사용, 나중에 초기화
lateinit var service: MyService

fun setup() {
    service = MyService()
}
```

### 연산자 오버로딩

```kotlin
data class Point(val x: Int, val y: Int) {
    operator fun plus(other: Point) = Point(x + other.x, y + other.y)
    operator fun times(scale: Int) = Point(x * scale, y * scale)
}

val p1 = Point(1, 2)
val p2 = Point(3, 4)
println(p1 + p2)   // Point(4, 6)
println(p1 * 2)    // Point(2, 4)
```

### infix 함수

```kotlin
infix fun Int.times(str: String) = str.repeat(this)

// 사용
val result = 3 times "Hi "  // "Hi Hi Hi "
```

---

## 참고 자료

- [Kotlin 공식 문서](https://kotlinlang.org/docs/home.html)
- [Kotlin Koans](https://play.kotlinlang.org/koans/overview)
- [From Java to Kotlin](https://github.com/MindorksOpenSource/from-java-to-kotlin)

*마지막 업데이트: 2025년 01월*
