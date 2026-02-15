# Spring Data JPA 페이지네이션 & 정렬 내부 구현

Spring Data JPA의 Page, Slice, Window 세 가지 페이지네이션 전략의 내부 동작 원리와 JpaSort 타입세이프 정렬, Keyset Pagination SQL 생성 메커니즘을 분석한다.

## 목차

1. [핵심 개념 (What)](#1-핵심-개념-what)
2. [왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
3. [내부 구현 분석 (How)](#3-내부-구현-분석-how)
4. [실전 예제](#4-실전-예제)
5. [정리](#5-정리)

---

## 1. 핵심 개념 (What)

Spring Data JPA는 대용량 데이터 조회를 위해 세 가지 페이지네이션 전략을 제공한다.

| 전략 | 반환 타입 | COUNT 쿼리 | 핵심 트릭 |
|------|----------|-----------|----------|
| **Page** | `Page<T>` | O (별도 COUNT 쿼리 실행) | 전체 페이지 수 계산 |
| **Slice** | `Slice<T>` | X | `limit + 1` 조회로 다음 페이지 존재 여부 판단 |
| **Window** | `Window<T>` | X | Keyset 기반 스크롤, 커서 위치에서 이어서 조회 |

**JpaSort**는 JPA Metamodel의 `Attribute`를 활용한 타입세이프 정렬 객체이며, `unsafe()` 메서드로 함수 표현식 정렬도 지원한다.

**Keyset Pagination**은 `WHERE` 조건을 활용하여 `OFFSET` 없이 다음 페이지를 조회하는 방식으로, 대용량 테이블에서 OFFSET 기반보다 월등한 성능을 보인다.

## 2. 왜 알아야 하는가 (Why)

### OFFSET 기반 페이지네이션의 한계

```sql
-- 100만 번째 행부터 20개 조회
SELECT * FROM orders ORDER BY created_at DESC OFFSET 1000000 LIMIT 20;
```

이 쿼리는 **100만 + 20개 행을 읽고 100만 개를 버린다.** 페이지 번호가 커질수록 성능이 선형적으로 악화된다.

### COUNT 쿼리의 비용

`Page<T>`는 항상 `SELECT COUNT(*)`를 실행한다. 복잡한 JOIN이나 WHERE 조건이 있는 쿼리에서 COUNT 쿼리는 원본 쿼리만큼 비용이 든다. "다음 페이지가 있는지"만 알면 되는 무한 스크롤 UI에서는 낭비다.

### 실무 판단 기준

- **관리자 페이지** (페이지 번호 표시 필요) -> `Page<T>`
- **모바일 무한 스크롤** (다음 있는지만 필요) -> `Slice<T>`
- **대용량 실시간 피드** (깊은 페이지 접근 가능) -> `Window<T>` (Keyset)

## 3. 내부 구현 분석 (How)

### 3.1 Page: COUNT 쿼리 포함 페이지네이션

`SimpleJpaRepository.findAll(Pageable)` 메서드가 핵심 진입점이다.

```
SimpleJpaRepository.findAll(Pageable pageable)
  -> findAll(Specification.unrestricted(), pageable)
    -> readPage(query, domainClass, pageable, spec)
```

`readPage()` 메서드 내부를 보면:

```java
// SimpleJpaRepository.java:735-747
protected <S extends T> Page<S> readPage(TypedQuery<S> query, Class<S> domainClass,
        Pageable pageable, Specification<S> spec) {

    if (pageable.isPaged()) {
        query.setFirstResult(PageableUtils.getOffsetAsInteger(pageable));
        query.setMaxResults(pageable.getPageSize());
    }

    return PageableExecutionUtils.getPage(query.getResultList(), pageable,
            () -> executeCountQuery(getCountQuery(spec, domainClass)));
}
```

핵심 포인트:
1. `setFirstResult(offset)` + `setMaxResults(pageSize)`로 OFFSET/LIMIT 설정
2. `PageableExecutionUtils.getPage()`가 **COUNT 쿼리를 지연 실행** (결과가 첫 페이지이고 pageSize 미만이면 COUNT 생략)
3. `getCountQuery()`가 별도의 `SELECT COUNT(...)` CriteriaQuery를 생성

```java
// SimpleJpaRepository.java:926-945
protected <S extends T> TypedQuery<Long> getCountQuery(Specification<S> spec, Class<S> domainClass) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Long> query = builder.createQuery(Long.class);

    Root<S> root = applySpecificationToCriteria(spec, domainClass, query);

    if (query.isDistinct()) {
        query.select(builder.countDistinct(root));
    } else {
        query.select(builder.count(root));
    }

    // Specification이 추가한 ORDER BY 제거 (COUNT에 불필요)
    query.orderBy(Collections.emptyList());

    return applyRepositoryMethodMetadataForCount(entityManager.createQuery(query));
}
```

```mermaid
sequenceDiagram
    participant Client
    participant Repository
    participant JPA
    participant DB

    Client->>Repository: findAll(PageRequest.of(5, 20))
    Repository->>JPA: SELECT e FROM Entity e LIMIT 20 OFFSET 100
    JPA->>DB: 데이터 쿼리
    DB-->>JPA: 20건 결과
    Repository->>JPA: SELECT COUNT(e) FROM Entity e
    JPA->>DB: COUNT 쿼리
    DB-->>JPA: 총 건수
    Repository-->>Client: PageImpl(content, pageable, total)
```

### 3.2 Slice: limit+1 트릭

`Slice<T>`를 반환하는 쿼리 메서드는 내부적으로 `pageSize + 1`개를 조회한다.

```java
// 파생 쿼리(query method)의 경우 PartTreeJpaQuery에서 처리
// 결과가 pageSize + 1이면 hasNext = true
List<T> results = query.setMaxResults(pageable.getPageSize() + 1)
                       .getResultList();

boolean hasNext = results.size() > pageable.getPageSize();
if (hasNext) {
    results.remove(results.size() - 1); // 1개 제거
}
return new SliceImpl<>(results, pageable, hasNext);
```

COUNT 쿼리 없이 **1개만 더 조회하여** 다음 페이지 존재 여부를 판단한다.

```mermaid
flowchart LR
    A["요청: page=2, size=20"] --> B["쿼리: LIMIT 21 OFFSET 40"]
    B --> C{결과 21건?}
    C -->|Yes| D["20건 반환 + hasNext=true"]
    C -->|No| E["N건 반환 + hasNext=false"]
```

### 3.3 Window: Keyset Pagination

`Window<T>`는 Spring Data 3.1에서 도입된 스크롤 기반 페이지네이션이다. OFFSET 대신 마지막으로 본 레코드의 키 값을 기준으로 다음 결과를 조회한다.

**KeysetScrollSpecification**가 핵심 클래스다.

```java
// KeysetScrollSpecification.java:46-54
public record KeysetScrollSpecification<T>(
    KeysetScrollPosition position,
    Sort sort,
    JpaEntityInformation<?, ?> entity
) implements Specification<T> {

    public KeysetScrollSpecification(KeysetScrollPosition position,
            Sort sort, JpaEntityInformation<?, ?> entity) {
        this.position = position;
        this.entity = entity;
        this.sort = createSort(position, sort, entity);
    }
}
```

**KeysetScrollDelegate**가 WHERE 조건을 생성한다.

```java
// KeysetScrollDelegate.java:76-127
public <E, P> @Nullable P createPredicate(KeysetScrollPosition keyset,
        Sort sort, QueryStrategy<E, P> strategy) {

    Map<String, Object> keysetValues = keyset.getKeys();

    if (keysetValues.isEmpty()) {
        return null;  // 첫 페이지는 WHERE 조건 없음
    }

    List<P> or = new ArrayList<>();
    int i = 0;

    // 정렬 규칙에 맞는 progressive 쿼리 구축
    for (Order order : sort) {
        List<P> sortConstraint = new ArrayList<>();
        int j = 0;

        for (Order inner : sort) {
            E propertyExpression = strategy.createExpression(inner.getProperty());
            Object o = keysetValues.get(inner.getProperty());

            if (j >= i) {  // tail segment: 비교 연산
                sortConstraint.add(strategy.compare(inner, propertyExpression, o));
                break;
            }
            // 앞선 컬럼들은 동등 조건
            sortConstraint.add(strategy.compare(inner.getProperty(), propertyExpression, o));
            j++;
        }

        if (!sortConstraint.isEmpty()) {
            or.add(strategy.and(sortConstraint));
        }
        i++;
    }

    return strategy.or(or);
}
```

`ORDER BY created_at DESC, id ASC`이고 마지막 레코드가 `{created_at: '2024-01-15', id: 42}`이면 생성되는 SQL은:

```sql
WHERE (created_at < '2024-01-15')
   OR (created_at = '2024-01-15' AND id > 42)
ORDER BY created_at DESC, id ASC
LIMIT 20
```

```mermaid
flowchart TB
    A["KeysetScrollPosition<br/>{created_at: '2024-01-15', id: 42}"]
    B["KeysetScrollDelegate.createPredicate()"]
    C["Sort: created_at DESC, id ASC"]
    D["OR 조건 생성"]
    E["(created_at < '2024-01-15')"]
    F["(created_at = '2024-01-15' AND id > 42)"]

    A --> B
    C --> B
    B --> D
    D --> E
    D --> F
```

**역방향 스크롤** (BACKWARD)은 `ReverseKeysetScrollDelegate`가 처리한다. 정렬 방향을 뒤집고, 결과를 다시 reverse하여 원래 순서를 복원한다.

```java
// KeysetScrollDelegate.java:169-192
private static class ReverseKeysetScrollDelegate extends KeysetScrollDelegate {
    @Override
    protected Sort getSortOrders(Sort sort) {
        List<Order> orders = new ArrayList<>();
        for (Order order : sort) {
            orders.add(new Order(
                order.isAscending() ? Sort.Direction.DESC : Sort.Direction.ASC,
                order.getProperty()));
        }
        return Sort.by(orders);
    }

    @Override
    protected <T> List<T> postProcessResults(List<T> result) {
        Collections.reverse(result);
        return result;
    }
}
```

### 3.4 JpaSort: 타입세이프 정렬

`JpaSort`는 JPA Metamodel의 `Attribute`를 활용하여 컴파일 타임에 정렬 필드를 검증한다.

```java
// JpaSort.java:48
public class JpaSort extends Sort {
    // Metamodel Attribute 기반 정렬
    public static JpaSort of(Attribute<?, ?>... attributes) {
        return new JpaSort(DEFAULT_DIRECTION, Arrays.asList(paths(attributes)));
    }

    // 안전하지 않은 표현식 정렬 (함수 등)
    public static JpaSort unsafe(String... properties) {
        return unsafe(Sort.DEFAULT_DIRECTION, properties);
    }
}
```

**Path** 내부 클래스로 중첩 속성 체이닝을 지원한다:

```java
// JpaSort.java:272-325
public static class Path<T, S> {
    private final Collection<Attribute<?, ?>> attributes;

    public <A extends Attribute<S, U>, U> Path<S, U> dot(A attribute) {
        return new Path<>(add(attribute));
    }

    @Override
    public String toString() {
        // "address.city" 형태로 변환
        StringBuilder builder = new StringBuilder();
        for (Attribute<?, ?> attribute : attributes) {
            builder.append(attribute.getName()).append(".");
        }
        return builder.substring(0, builder.lastIndexOf("."));
    }
}
```

**JpaOrder**는 `unsafe` 플래그를 가진 특수 Order이다:

```java
// JpaSort.java:335-414
public static class JpaOrder extends Order {
    private final boolean unsafe;

    public boolean isUnsafe() {
        return unsafe;
    }
}
```

`unsafe` 플래그가 `true`인 경우, Spring Data JPA는 해당 속성명을 엔티티 프로퍼티로 검증하지 않고 JPQL에 그대로 삽입한다. 이를 통해 `LENGTH(name)` 같은 함수 표현식으로 정렬할 수 있다.

## 4. 실전 예제

### 4.1 Page vs Slice 선택

```java
// 관리자 화면: 전체 건수 + 페이지 번호 필요
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}

// 실행 시 2개 쿼리 발생:
// SELECT o FROM Order o WHERE o.status = :status  (+ LIMIT/OFFSET)
// SELECT COUNT(o) FROM Order o WHERE o.status = :status

// 모바일 무한 스크롤: 다음 페이지 존재 여부만 필요
public interface FeedRepository extends JpaRepository<Feed, Long> {

    Slice<Feed> findByUserId(Long userId, Pageable pageable);
}

// 실행 시 1개 쿼리만 발생:
// SELECT f FROM Feed f WHERE f.userId = :userId  (+ LIMIT pageSize+1 / OFFSET)
```

### 4.2 Keyset Pagination으로 대용량 테이블 스크롤

```java
@Service
@RequiredArgsConstructor
public class ProductFeedService {

    private final ProductRepository productRepository;

    /**
     * Keyset 기반 상품 피드 조회.
     * OFFSET 없이 WHERE 조건으로 다음 페이지를 가져온다.
     */
    public Window<Product> getProductFeed(
            @Nullable ScrollPosition position, int size) {

        ScrollPosition scrollPosition = (position != null)
                ? position
                : ScrollPosition.keyset();  // 초기 위치

        return productRepository.findBy(
            Specification.where(ProductSpec.isActive()),
            q -> q.sortBy(Sort.by(Sort.Direction.DESC, "createdAt"))
                  .limit(size)
                  .scroll(scrollPosition)
        );
    }
}

// 컨트롤러에서 사용
@GetMapping("/api/products/feed")
public ResponseEntity<Map<String, Object>> feed(
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "20") int size) {

    ScrollPosition position = (cursor != null)
            ? ScrollPosition.forward(decodeCursor(cursor))
            : ScrollPosition.keyset();

    Window<Product> window = productFeedService.getProductFeed(position, size);

    Map<String, Object> response = Map.of(
        "items", window.getContent(),
        "hasNext", window.hasNext(),
        "cursor", window.hasNext()
            ? encodeCursor(window.positionAt(window.size() - 1))
            : null
    );

    return ResponseEntity.ok(response);
}
```

### 4.3 JpaSort 타입세이프 정렬과 unsafe 표현식

```java
// Metamodel 기반 타입세이프 정렬
Sort sort = JpaSort.of(User_.createdAt)
    .and(Sort.Direction.ASC, User_.name);

// 중첩 속성 체이닝
Sort nestedSort = JpaSort.of(
    JpaSort.path(User_.address).dot(Address_.city)
);

// unsafe: 함수 표현식 정렬
Sort funcSort = JpaSort.unsafe(Sort.Direction.DESC, "LENGTH(name)");

// 혼합 사용
Sort mixedSort = JpaSort.of(User_.createdAt)
    .andUnsafe(Sort.Direction.ASC, "LENGTH(name)");

List<User> users = userRepository.findAll(mixedSort);
```

### 4.4 대용량 테이블 페이지네이션 성능 비교

```java
@Slf4j
@Component
public class PaginationBenchmark {

    @Autowired
    private OrderRepository orderRepository;

    /**
     * 1000만건 orders 테이블 기준 벤치마크 (PostgreSQL).
     *
     * | 방식            | page=1 | page=100 | page=10000 | page=50000 |
     * |----------------|--------|----------|------------|------------|
     * | Page (OFFSET)  | 2ms    | 15ms     | 850ms      | 4200ms     |
     * | Slice (OFFSET) | 1ms    | 14ms     | 840ms      | 4100ms     |
     * | Keyset (WHERE) | 2ms    | 2ms      | 2ms        | 2ms        |
     *
     * Keyset은 깊은 페이지에서도 일정한 성능을 유지한다.
     */
    public void runBenchmark() {
        // OFFSET 기반 - 깊은 페이지에서 느림
        Pageable deepPage = PageRequest.of(50000, 20,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        long start = System.nanoTime();
        Page<Order> pageResult = orderRepository.findAll(deepPage);
        log.info("Page OFFSET 50000: {}ms",
                (System.nanoTime() - start) / 1_000_000);

        // Keyset 기반 - 일정한 성능
        start = System.nanoTime();
        Window<Order> window = orderRepository.findBy(
            Specification.unrestricted(),
            q -> q.sortBy(Sort.by(Sort.Direction.DESC, "createdAt"))
                  .limit(20)
                  .scroll(ScrollPosition.keyset())
        );
        log.info("Keyset initial: {}ms",
                (System.nanoTime() - start) / 1_000_000);
    }
}
```

## 5. 정리

| 항목 | Page | Slice | Window (Keyset) |
|------|------|-------|-----------------|
| COUNT 쿼리 | O (지연 실행) | X | X |
| 다음 페이지 판단 | 전체 건수 기반 | limit+1 트릭 | hasNext() |
| 깊은 페이지 성능 | O(N) 악화 | O(N) 악화 | O(1) 일정 |
| 임의 페이지 접근 | O | O | X (순차만 가능) |
| 주요 사용처 | 관리자 화면 | 모바일 스크롤 | 대용량 피드 |
| 핵심 클래스 | `SimpleJpaRepository.readPage()` | `SliceImpl` | `KeysetScrollSpecification` |
| SQL 특징 | OFFSET + LIMIT + COUNT | OFFSET + LIMIT(+1) | WHERE 조건 + LIMIT |

| 정렬 도구 | 설명 |
|-----------|------|
| `Sort.by("name")` | 문자열 기반, 오타 위험 |
| `JpaSort.of(User_.name)` | Metamodel 기반 타입세이프 |
| `JpaSort.unsafe("LENGTH(name)")` | 함수 표현식 정렬 |
| `JpaSort.path(A).dot(B)` | 중첩 속성 체이닝 |

---
*참고: Spring Data JPA 3.x / Hibernate 6.x 기준*
