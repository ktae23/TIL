# Java Stream API

Stream API의 중간/최종 연산과 병렬 스트림 주의점을 정리합니다.

## 목차

1. [Stream 기본](#1-stream-기본)
2. [중간 연산](#2-중간-연산)
3. [최종 연산](#3-최종-연산)
4. [Collectors](#4-collectors)
5. [병렬 스트림](#5-병렬-스트림)
6. [주의사항 및 Best Practices](#6-주의사항-및-best-practices)

---

## 1. Stream 기본

### Stream이란

```java
// 선언형 데이터 처리
// 내부 반복 (External Iteration → Internal Iteration)

// 전통적 방식
List<String> names = new ArrayList<>();
for (User user : users) {
    if (user.getAge() > 20) {
        names.add(user.getName());
    }
}

// Stream 방식
List<String> names = users.stream()
    .filter(user -> user.getAge() > 20)
    .map(User::getName)
    .collect(Collectors.toList());
```

### Stream 생성

```java
// 컬렉션에서
List<String> list = Arrays.asList("a", "b", "c");
Stream<String> stream = list.stream();

// 배열에서
String[] arr = {"a", "b", "c"};
Stream<String> stream = Arrays.stream(arr);

// 직접 생성
Stream<String> stream = Stream.of("a", "b", "c");

// 무한 스트림
Stream<Integer> infinite = Stream.iterate(0, n -> n + 2);
Stream<Double> randoms = Stream.generate(Math::random);

// 기본형 스트림
IntStream intStream = IntStream.range(1, 100);  // 1~99
LongStream longStream = LongStream.rangeClosed(1, 100);  // 1~100
```

### 스트림 특성

```
1. 한 번만 사용 가능
   stream.forEach(...);
   stream.forEach(...);  // IllegalStateException!

2. 지연 평가 (Lazy Evaluation)
   중간 연산은 최종 연산 호출 전까지 실행되지 않음

3. 원본 데이터 불변
   스트림 연산은 원본 컬렉션을 수정하지 않음
```

---

## 2. 중간 연산

### filter

```java
// 조건에 맞는 요소만 통과
List<User> adults = users.stream()
    .filter(user -> user.getAge() >= 18)
    .collect(Collectors.toList());
```

### map

```java
// 요소 변환
List<String> names = users.stream()
    .map(User::getName)
    .collect(Collectors.toList());

// flatMap: 중첩 구조 평탄화
List<List<Integer>> nested = Arrays.asList(
    Arrays.asList(1, 2),
    Arrays.asList(3, 4)
);
List<Integer> flat = nested.stream()
    .flatMap(Collection::stream)
    .collect(Collectors.toList());  // [1, 2, 3, 4]
```

### distinct / sorted

```java
// 중복 제거
List<Integer> unique = numbers.stream()
    .distinct()
    .collect(Collectors.toList());

// 정렬
List<User> sorted = users.stream()
    .sorted(Comparator.comparing(User::getName))
    .collect(Collectors.toList());

// 역순
List<User> reversed = users.stream()
    .sorted(Comparator.comparing(User::getAge).reversed())
    .collect(Collectors.toList());
```

### limit / skip

```java
// 처음 N개
List<User> top5 = users.stream()
    .limit(5)
    .collect(Collectors.toList());

// N개 건너뛰기
List<User> afterFirst5 = users.stream()
    .skip(5)
    .collect(Collectors.toList());

// 페이지네이션
int page = 2;
int size = 10;
List<User> pageData = users.stream()
    .skip((page - 1) * size)
    .limit(size)
    .collect(Collectors.toList());
```

### peek

```java
// 중간에 동작 수행 (디버깅용)
List<User> result = users.stream()
    .filter(u -> u.getAge() > 20)
    .peek(u -> System.out.println("Filtered: " + u.getName()))
    .map(User::getName)
    .peek(n -> System.out.println("Mapped: " + n))
    .collect(Collectors.toList());
```

---

## 3. 최종 연산

### forEach

```java
// 각 요소에 대해 동작 수행
users.stream()
    .filter(u -> u.getAge() > 20)
    .forEach(u -> System.out.println(u.getName()));

// 병렬 스트림에서는 순서 보장 안 됨
// forEachOrdered 사용
users.parallelStream()
    .forEachOrdered(System.out::println);
```

### collect

```java
// 결과를 컬렉션으로 수집
List<String> list = stream.collect(Collectors.toList());
Set<String> set = stream.collect(Collectors.toSet());
```

### reduce

```java
// 요소들을 하나로 결합
int sum = numbers.stream()
    .reduce(0, (a, b) -> a + b);

// Optional 반환 (초기값 없이)
Optional<Integer> max = numbers.stream()
    .reduce(Integer::max);

// 복잡한 reduce
String concatenated = strings.stream()
    .reduce("", (a, b) -> a + ", " + b);
```

### count / min / max

```java
long count = users.stream()
    .filter(u -> u.getAge() > 20)
    .count();

Optional<User> youngest = users.stream()
    .min(Comparator.comparing(User::getAge));

Optional<User> oldest = users.stream()
    .max(Comparator.comparing(User::getAge));
```

### anyMatch / allMatch / noneMatch

```java
// 하나라도 만족?
boolean hasAdult = users.stream()
    .anyMatch(u -> u.getAge() >= 18);

// 모두 만족?
boolean allAdults = users.stream()
    .allMatch(u -> u.getAge() >= 18);

// 모두 불만족?
boolean noMinors = users.stream()
    .noneMatch(u -> u.getAge() < 18);
```

### findFirst / findAny

```java
// 첫 번째 요소
Optional<User> first = users.stream()
    .filter(u -> u.getAge() > 20)
    .findFirst();

// 아무 요소 (병렬에서 효율적)
Optional<User> any = users.parallelStream()
    .filter(u -> u.getAge() > 20)
    .findAny();
```

---

## 4. Collectors

### 기본 수집

```java
// List, Set
List<String> list = stream.collect(Collectors.toList());
Set<String> set = stream.collect(Collectors.toSet());

// 특정 컬렉션
TreeSet<String> treeSet = stream.collect(
    Collectors.toCollection(TreeSet::new));

// 배열
String[] array = stream.toArray(String[]::new);
```

### 그룹핑

```java
// 나이대별 그룹
Map<Integer, List<User>> byAge = users.stream()
    .collect(Collectors.groupingBy(User::getAge));

// 그룹별 개수
Map<String, Long> countByCity = users.stream()
    .collect(Collectors.groupingBy(
        User::getCity,
        Collectors.counting()
    ));

// 그룹별 합계
Map<String, Integer> sumByCity = users.stream()
    .collect(Collectors.groupingBy(
        User::getCity,
        Collectors.summingInt(User::getScore)
    ));

// 다단계 그룹핑
Map<String, Map<Integer, List<User>>> byCityAndAge = users.stream()
    .collect(Collectors.groupingBy(
        User::getCity,
        Collectors.groupingBy(User::getAge)
    ));
```

### 파티셔닝

```java
// true/false 두 그룹으로 분류
Map<Boolean, List<User>> partitioned = users.stream()
    .collect(Collectors.partitioningBy(
        u -> u.getAge() >= 18
    ));

List<User> adults = partitioned.get(true);
List<User> minors = partitioned.get(false);
```

### joining

```java
// 문자열 결합
String names = users.stream()
    .map(User::getName)
    .collect(Collectors.joining());  // "KimLeePark"

String csv = users.stream()
    .map(User::getName)
    .collect(Collectors.joining(", "));  // "Kim, Lee, Park"

String formatted = users.stream()
    .map(User::getName)
    .collect(Collectors.joining(", ", "[", "]"));  // "[Kim, Lee, Park]"
```

### toMap

```java
// Map으로 변환
Map<Long, User> byId = users.stream()
    .collect(Collectors.toMap(
        User::getId,
        Function.identity()
    ));

// 충돌 처리
Map<String, User> byName = users.stream()
    .collect(Collectors.toMap(
        User::getName,
        Function.identity(),
        (existing, replacement) -> existing  // 충돌 시 기존 값 유지
    ));
```

---

## 5. 병렬 스트림

### 병렬 스트림 생성

```java
// 병렬 스트림 생성
Stream<User> parallelStream = users.parallelStream();

// 기존 스트림을 병렬로
Stream<User> parallel = users.stream().parallel();

// 순차로 전환
Stream<User> sequential = parallelStream.sequential();
```

### 언제 사용해야 하나?

```
적합한 경우:
- 대용량 데이터 (수만 건 이상)
- CPU 집약적 연산
- 독립적인 작업 (상태 공유 없음)
- 분할 용이한 자료구조 (ArrayList, 배열)

부적합한 경우:
- 소량 데이터
- I/O 바운드 작업
- 순서가 중요한 경우
- LinkedList (분할 비효율)
- 공유 상태 존재
```

### 주의사항

```java
// 1. 순서 보장 안 됨
numbers.parallelStream()
    .forEach(System.out::println);  // 순서 랜덤

// 순서 필요 시
numbers.parallelStream()
    .forEachOrdered(System.out::println);

// 2. 공유 상태 금지!
List<Integer> results = new ArrayList<>();  // Thread-unsafe!
numbers.parallelStream()
    .map(n -> n * 2)
    .forEach(results::add);  // 동시성 문제!

// 올바른 방법
List<Integer> results = numbers.parallelStream()
    .map(n -> n * 2)
    .collect(Collectors.toList());

// 3. ForkJoinPool 공유
// 기본적으로 공통 ForkJoinPool 사용
// 커스텀 풀 사용하려면:
ForkJoinPool customPool = new ForkJoinPool(4);
List<String> result = customPool.submit(() ->
    users.parallelStream()
        .map(User::getName)
        .collect(Collectors.toList())
).get();
```

---

## 6. 주의사항 및 Best Practices

### 스트림 재사용 불가

```java
Stream<User> stream = users.stream();
stream.count();
stream.collect(Collectors.toList());  // IllegalStateException!

// Supplier 사용
Supplier<Stream<User>> streamSupplier = () -> users.stream();
streamSupplier.get().count();
streamSupplier.get().collect(Collectors.toList());
```

### 무한 스트림 주의

```java
// limit 없이 사용 시 무한 루프
Stream.iterate(0, n -> n + 1)
    .forEach(System.out::println);  // 무한!

// limit 필수
Stream.iterate(0, n -> n + 1)
    .limit(100)
    .forEach(System.out::println);
```

### 박싱/언박싱 비용

```java
// 비효율: Integer 박싱/언박싱
int sum = numbers.stream()
    .mapToInt(Integer::intValue)
    .sum();

// 효율: 기본형 스트림
int sum = IntStream.range(1, 100).sum();
```

### Optional 활용

```java
// findFirst, reduce 등의 결과 처리
users.stream()
    .filter(u -> u.getAge() > 20)
    .findFirst()
    .ifPresentOrElse(
        user -> System.out.println(user.getName()),
        () -> System.out.println("Not found")
    );
```

---

## 핵심 정리

| 연산 유형 | 예시 | 특징 |
|----------|------|------|
| 중간 연산 | filter, map, flatMap, sorted | 지연 평가, 파이프라인 |
| 최종 연산 | collect, reduce, forEach, count | 스트림 소비, 결과 생성 |

| 주의사항 | 설명 |
|----------|------|
| 일회성 | 스트림은 한 번만 사용 |
| 지연 평가 | 최종 연산까지 실행 안 됨 |
| 병렬 주의 | 공유 상태, 순서, 자료구조 고려 |
| 박싱 비용 | 기본형 스트림 활용 |

---

*마지막 업데이트: 2025년 01월*
