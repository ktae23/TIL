# JPA 기초 개념

Java Persistence API(JPA)의 핵심 개념과 기본 사용법을 정리합니다.

## 목차

- [JPA란?](#jpa란)
- [영속성 컨텍스트](#영속성-컨텍스트)
- [엔티티 생명주기](#엔티티-생명주기)
- [엔티티 매핑](#엔티티-매핑)
- [연관관계 매핑](#연관관계-매핑)
- [Spring Data JPA Repository](#spring-data-jpa-repository)

---

## JPA란?

**JPA(Java Persistence API)**는 자바 ORM 기술의 표준 명세입니다. 구현체로는 Hibernate, EclipseLink 등이 있으며, Spring Data JPA는 JPA를 더 쉽게 사용할 수 있도록 추상화한 모듈입니다.

```
Application → Spring Data JPA → JPA (Hibernate) → JDBC → Database
```

### 의존성 설정

```gradle
// build.gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'com.mysql:mysql-connector-j'
}
```

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: validate  # none, validate, update, create, create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        default_batch_fetch_size: 100
```

---

## 영속성 컨텍스트

**영속성 컨텍스트(Persistence Context)**는 엔티티를 영구 저장하는 환경으로, EntityManager를 통해 접근합니다.

### 영속성 컨텍스트의 이점

```java
@Service
@Transactional
public class MemberService {

    @PersistenceContext
    private EntityManager em;

    public void example() {
        Member member = new Member("Kim");

        // 1. 1차 캐시
        em.persist(member);  // 영속화
        Member found = em.find(Member.class, member.getId());  // DB 조회 없이 1차 캐시에서 반환

        // 2. 동일성 보장
        Member found2 = em.find(Member.class, member.getId());
        System.out.println(found == found2);  // true (같은 인스턴스)

        // 3. 쓰기 지연 (트랜잭션 커밋 시점에 INSERT)
        em.persist(new Member("Lee"));
        em.persist(new Member("Park"));
        // 커밋 시점에 INSERT 쿼리 2개 한번에 실행

        // 4. 변경 감지 (Dirty Checking)
        Member m = em.find(Member.class, 1L);
        m.setName("Changed");  // UPDATE 쿼리 자동 실행 (em.update() 같은 메서드 없음)

        // 5. 지연 로딩
        // 연관 엔티티를 실제 사용할 때까지 조회 지연
    }
}
```

### 플러시(Flush)

영속성 컨텍스트의 변경 내용을 DB에 반영합니다.

```java
@Transactional
public void flushExample() {
    Member member = new Member("Kim");
    em.persist(member);

    // 플러시 발생 시점
    // 1. em.flush() 직접 호출
    em.flush();

    // 2. 트랜잭션 커밋 시
    // @Transactional 메서드 종료 시 자동 호출

    // 3. JPQL 쿼리 실행 시
    List<Member> members = em.createQuery("SELECT m FROM Member m", Member.class)
        .getResultList();  // 플러시 자동 호출 후 쿼리 실행
}
```

---

## 엔티티 생명주기

```
비영속(new) → persist() → 영속(managed)
                              ↓ detach(), clear(), close()
                          준영속(detached)
                              ↓ merge()
                          영속(managed)
                              ↓ remove()
                          삭제(removed)
```

```java
public void lifecycleExample() {
    // 비영속 상태
    Member member = new Member("Kim");

    // 영속 상태
    em.persist(member);

    // 준영속 상태
    em.detach(member);  // 특정 엔티티만 분리
    em.clear();         // 영속성 컨텍스트 초기화
    em.close();         // 영속성 컨텍스트 종료

    // 다시 영속 상태로
    Member merged = em.merge(member);  // 새로운 영속 엔티티 반환

    // 삭제
    em.remove(member);
}
```

---

## 엔티티 매핑

### 기본 엔티티

```java
@Entity
@Table(name = "members")  // 테이블명 지정
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(unique = true)
    private String email;

    @Enumerated(EnumType.STRING)  // ORDINAL 사용 금지
    private MemberStatus status;

    @Lob
    private String description;

    @Transient  // DB 컬럼과 매핑하지 않음
    private String tempData;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

### 기본키 생성 전략

```java
// 1. IDENTITY - MySQL AUTO_INCREMENT
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

// 2. SEQUENCE - Oracle, PostgreSQL
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_seq")
@SequenceGenerator(name = "member_seq", sequenceName = "MEMBER_SEQ", allocationSize = 50)
private Long id;

// 3. TABLE - 키 생성 전용 테이블 사용
@Id
@GeneratedValue(strategy = GenerationType.TABLE, generator = "member_table")
@TableGenerator(name = "member_table", table = "ID_GENERATOR",
                pkColumnValue = "MEMBER_ID", allocationSize = 50)
private Long id;

// 4. AUTO - DB 방언에 따라 자동 선택
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
private Long id;
```

### 복합키 매핑

```java
// 방법 1: @IdClass
@Entity
@IdClass(MemberProductId.class)
public class MemberProduct {
    @Id
    private Long memberId;

    @Id
    private Long productId;
}

public class MemberProductId implements Serializable {
    private Long memberId;
    private Long productId;
    // equals, hashCode 필수
}

// 방법 2: @EmbeddedId (권장)
@Entity
public class MemberProduct {
    @EmbeddedId
    private MemberProductId id;
}

@Embeddable
public class MemberProductId implements Serializable {
    private Long memberId;
    private Long productId;
    // equals, hashCode 필수
}
```

---

## 연관관계 매핑

### 다대일 (N:1) - 가장 많이 사용

```java
@Entity
public class Order {
    @Id @GeneratedValue
    private Long id;

    // 다대일 단방향
    @ManyToOne(fetch = FetchType.LAZY)  // LAZY 필수!
    @JoinColumn(name = "member_id")
    private Member member;
}

@Entity
public class Member {
    @Id @GeneratedValue
    private Long id;

    // 다대일 양방향 (연관관계 주인은 Order.member)
    @OneToMany(mappedBy = "member")  // 읽기 전용
    private List<Order> orders = new ArrayList<>();
}
```

### 일대다 (1:N) - 권장하지 않음

```java
@Entity
public class Team {
    @Id @GeneratedValue
    private Long id;

    // 일대다 단방향 (외래키가 다른 테이블에 있음)
    @OneToMany
    @JoinColumn(name = "team_id")  // Member 테이블의 team_id
    private List<Member> members = new ArrayList<>();
    // INSERT Member 후 UPDATE Member로 team_id 설정 → 비효율
}
```

### 일대일 (1:1)

```java
@Entity
public class Member {
    @Id @GeneratedValue
    private Long id;

    // 일대일 주 테이블에 외래키
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locker_id")
    private Locker locker;
}

@Entity
public class Locker {
    @Id @GeneratedValue
    private Long id;

    @OneToOne(mappedBy = "locker")
    private Member member;
}
```

### 다대다 (N:M) - 실무에서 사용 금지

```java
// 다대다 매핑 (비권장)
@Entity
public class Member {
    @ManyToMany
    @JoinTable(name = "member_product")
    private List<Product> products = new ArrayList<>();
}

// 중간 테이블을 엔티티로 승격 (권장)
@Entity
public class MemberProduct {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    private int quantity;
    private LocalDateTime orderedAt;
}
```

### 연관관계 편의 메서드

```java
@Entity
public class Order {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    // 연관관계 편의 메서드
    public void setMember(Member member) {
        this.member = member;
        member.getOrders().add(this);
    }

    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }
}
```

---

## Spring Data JPA Repository

### 기본 Repository

```java
public interface MemberRepository extends JpaRepository<Member, Long> {
    // 기본 제공 메서드
    // save(), findById(), findAll(), delete(), count(), existsById() 등
}
```

### 쿼리 메서드

```java
public interface MemberRepository extends JpaRepository<Member, Long> {

    // 이름으로 조회
    List<Member> findByName(String name);

    // 이름 포함
    List<Member> findByNameContaining(String name);

    // 여러 조건
    List<Member> findByNameAndStatus(String name, MemberStatus status);

    // 정렬
    List<Member> findByStatusOrderByCreatedAtDesc(MemberStatus status);

    // 페이징
    Page<Member> findByStatus(MemberStatus status, Pageable pageable);

    // 상위 N개
    List<Member> findTop10ByOrderByCreatedAtDesc();

    // 존재 여부
    boolean existsByEmail(String email);

    // 개수
    long countByStatus(MemberStatus status);

    // 삭제
    void deleteByStatus(MemberStatus status);
}
```

### 쿼리 메서드 키워드

| 키워드 | 예시 | JPQL |
|--------|------|------|
| And | findByNameAndAge | where name = ? and age = ? |
| Or | findByNameOrAge | where name = ? or age = ? |
| Is, Equals | findByName, findByNameIs | where name = ? |
| Between | findByAgeBetween | where age between ? and ? |
| LessThan | findByAgeLessThan | where age < ? |
| GreaterThanEqual | findByAgeGreaterThanEqual | where age >= ? |
| Like | findByNameLike | where name like ? |
| Containing | findByNameContaining | where name like %?% |
| StartingWith | findByNameStartingWith | where name like ?% |
| In | findByAgeIn(Collection ages) | where age in (?) |
| True/False | findByActiveTrue | where active = true |
| OrderBy | findByOrderByAgeDesc | order by age desc |
| Not | findByNameNot | where name <> ? |
| IsNull | findByNameIsNull | where name is null |

### @Query 사용

```java
public interface MemberRepository extends JpaRepository<Member, Long> {

    // JPQL
    @Query("SELECT m FROM Member m WHERE m.email = :email")
    Optional<Member> findByEmailCustom(@Param("email") String email);

    // Native Query
    @Query(value = "SELECT * FROM members WHERE email = :email", nativeQuery = true)
    Optional<Member> findByEmailNative(@Param("email") String email);

    // DTO 프로젝션
    @Query("SELECT new com.example.dto.MemberDto(m.id, m.name, m.email) FROM Member m")
    List<MemberDto> findAllMemberDto();

    // 벌크 연산
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Member m SET m.status = :status WHERE m.lastLoginAt < :date")
    int bulkUpdateStatus(@Param("status") MemberStatus status, @Param("date") LocalDateTime date);
}
```

### 페이징과 정렬

```java
@Service
public class MemberService {

    public Page<Member> getMembers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return memberRepository.findAll(pageable);
    }

    public Slice<Member> getMembersAsSlice(int page, int size) {
        // Slice: 다음 페이지 존재 여부만 확인 (count 쿼리 없음)
        Pageable pageable = PageRequest.of(page, size);
        return memberRepository.findByStatus(MemberStatus.ACTIVE, pageable);
    }
}
```

### 명세(Specification) 활용

```java
public interface MemberRepository extends JpaRepository<Member, Long>,
                                          JpaSpecificationExecutor<Member> {
}

// Specification 정의
public class MemberSpecs {
    public static Specification<Member> hasName(String name) {
        return (root, query, cb) -> cb.equal(root.get("name"), name);
    }

    public static Specification<Member> hasStatus(MemberStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}

// 사용
List<Member> members = memberRepository.findAll(
    MemberSpecs.hasName("Kim").and(MemberSpecs.hasStatus(MemberStatus.ACTIVE))
);
```

---

*마지막 업데이트: 2026년 01월*
