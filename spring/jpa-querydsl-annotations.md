# QueryDSL과 JPA 어노테이션

QueryDSL 설정 및 활용법과 자주 사용하는 JPA 어노테이션을 정리합니다.

## 목차

- [QueryDSL 설정](#querydsl-설정)
- [QueryDSL 기본 문법](#querydsl-기본-문법)
- [QueryDSL 고급 기능](#querydsl-고급-기능)
- [JPA 핵심 어노테이션](#jpa-핵심-어노테이션)
- [Auditing 어노테이션](#auditing-어노테이션)
- [검증 어노테이션](#검증-어노테이션)

---

## QueryDSL 설정

### Gradle 설정 (Spring Boot 3.x)

```gradle
// build.gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'

    // QueryDSL
    implementation 'com.querydsl:querydsl-jpa:5.0.0:jakarta'
    annotationProcessor 'com.querydsl:querydsl-apt:5.0.0:jakarta'
    annotationProcessor 'jakarta.annotation:jakarta.annotation-api'
    annotationProcessor 'jakarta.persistence:jakarta.persistence-api'
}

// Q클래스 생성 위치
def querydslDir = "$buildDir/generated/querydsl"

sourceSets {
    main.java.srcDirs += [ querydslDir ]
}

tasks.withType(JavaCompile) {
    options.annotationProcessorGeneratedSourcesDirectory = file(querydslDir)
}

clean.doLast {
    file(querydslDir).deleteDir()
}
```

### JPAQueryFactory 설정

```java
@Configuration
public class QueryDslConfig {

    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
```

---

## QueryDSL 기본 문법

### 기본 조회

```java
@Repository
@RequiredArgsConstructor
public class MemberQueryRepository {

    private final JPAQueryFactory queryFactory;

    // 단건 조회
    public Member findByUsername(String username) {
        return queryFactory
            .selectFrom(member)
            .where(member.username.eq(username))
            .fetchOne();
    }

    // 리스트 조회
    public List<Member> findByAge(int age) {
        return queryFactory
            .selectFrom(member)
            .where(member.age.eq(age))
            .fetch();
    }

    // 첫 번째 결과
    public Member findFirst() {
        return queryFactory
            .selectFrom(member)
            .fetchFirst();  // limit(1).fetchOne()
    }
}
```

### 검색 조건

```java
public List<Member> search(String name, Integer ageGoe, Integer ageLoe) {
    return queryFactory
        .selectFrom(member)
        .where(
            member.name.eq(name),                    // =
            member.name.ne(name),                    // !=
            member.name.like("Kim%"),                // like
            member.name.contains("im"),              // like %im%
            member.name.startsWith("K"),             // like K%
            member.age.goe(ageGoe),                  // >=
            member.age.gt(ageGoe),                   // >
            member.age.loe(ageLoe),                  // <=
            member.age.lt(ageLoe),                   // <
            member.age.between(10, 30),              // between
            member.age.in(10, 20, 30),               // in
            member.name.isNotNull()                  // is not null
        )
        .fetch();
}

// 동적 쿼리 - BooleanBuilder
public List<Member> searchDynamic(MemberSearchCondition condition) {
    BooleanBuilder builder = new BooleanBuilder();

    if (hasText(condition.getUsername())) {
        builder.and(member.username.contains(condition.getUsername()));
    }
    if (condition.getAgeGoe() != null) {
        builder.and(member.age.goe(condition.getAgeGoe()));
    }
    if (condition.getAgeLoe() != null) {
        builder.and(member.age.loe(condition.getAgeLoe()));
    }

    return queryFactory
        .selectFrom(member)
        .where(builder)
        .fetch();
}

// 동적 쿼리 - Where 다중 파라미터 (권장)
public List<Member> searchWhere(MemberSearchCondition condition) {
    return queryFactory
        .selectFrom(member)
        .where(
            usernameContains(condition.getUsername()),
            ageGoe(condition.getAgeGoe()),
            ageLoe(condition.getAgeLoe())
        )
        .fetch();
}

private BooleanExpression usernameContains(String username) {
    return hasText(username) ? member.username.contains(username) : null;
}

private BooleanExpression ageGoe(Integer age) {
    return age != null ? member.age.goe(age) : null;
}

private BooleanExpression ageLoe(Integer age) {
    return age != null ? member.age.loe(age) : null;
}

// 조건 조합
private BooleanExpression ageBetween(Integer ageGoe, Integer ageLoe) {
    return ageGoe(ageGoe).and(ageLoe(ageLoe));
}
```

### 정렬과 페이징

```java
public List<Member> findWithPaging(int offset, int limit) {
    return queryFactory
        .selectFrom(member)
        .orderBy(member.age.desc(), member.username.asc().nullsLast())
        .offset(offset)
        .limit(limit)
        .fetch();
}

// Spring Data Page 반환
public Page<Member> searchPage(MemberSearchCondition condition, Pageable pageable) {
    List<Member> content = queryFactory
        .selectFrom(member)
        .where(
            usernameContains(condition.getUsername()),
            ageGoe(condition.getAgeGoe())
        )
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .orderBy(member.id.desc())
        .fetch();

    JPAQuery<Long> countQuery = queryFactory
        .select(member.count())
        .from(member)
        .where(
            usernameContains(condition.getUsername()),
            ageGoe(condition.getAgeGoe())
        );

    // count 쿼리 최적화 - 필요할 때만 실행
    return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
}
```

### 조인

```java
// 기본 조인
public List<Member> findMembersWithTeam() {
    return queryFactory
        .selectFrom(member)
        .join(member.team, team)
        .where(team.name.eq("TeamA"))
        .fetch();
}

// 페치 조인
public List<Member> findMembersWithTeamFetch() {
    return queryFactory
        .selectFrom(member)
        .join(member.team, team).fetchJoin()
        .fetch();
}

// Left 조인
public List<Member> findMembersLeftJoin() {
    return queryFactory
        .selectFrom(member)
        .leftJoin(member.team, team)
        .fetch();
}

// 세타 조인 (연관관계 없는 조인)
public List<Member> findMembersByTeamName(String teamName) {
    return queryFactory
        .select(member)
        .from(member, team)
        .where(member.username.eq(team.name))
        .fetch();
}

// On 절
public List<Tuple> findMembersOnTeam() {
    return queryFactory
        .select(member, team)
        .from(member)
        .leftJoin(team).on(member.username.eq(team.name))
        .fetch();
}
```

---

## QueryDSL 고급 기능

### 프로젝션 (DTO 반환)

```java
// 1. 프로퍼티 접근 (Setter)
public List<MemberDto> findMemberDtoBySetter() {
    return queryFactory
        .select(Projections.bean(MemberDto.class,
            member.username,
            member.age))
        .from(member)
        .fetch();
}

// 2. 필드 접근
public List<MemberDto> findMemberDtoByField() {
    return queryFactory
        .select(Projections.fields(MemberDto.class,
            member.username,
            member.age))
        .from(member)
        .fetch();
}

// 3. 생성자 접근
public List<MemberDto> findMemberDtoByConstructor() {
    return queryFactory
        .select(Projections.constructor(MemberDto.class,
            member.username,
            member.age))
        .from(member)
        .fetch();
}

// 4. @QueryProjection (컴파일 타임 오류 체크)
@Getter
public class MemberDto {
    private String username;
    private int age;

    @QueryProjection
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}

public List<MemberDto> findMemberDtoByQueryProjection() {
    return queryFactory
        .select(new QMemberDto(member.username, member.age))
        .from(member)
        .fetch();
}

// 필드명이 다를 때
public List<UserDto> findUserDto() {
    return queryFactory
        .select(Projections.fields(UserDto.class,
            member.username.as("name"),  // 필드명 매핑
            ExpressionUtils.as(
                JPAExpressions.select(memberSub.age.max())
                    .from(memberSub), "age")
        ))
        .from(member)
        .fetch();
}
```

### 서브쿼리

```java
public List<Member> findMembersWithMaxAge() {
    QMember memberSub = new QMember("memberSub");

    return queryFactory
        .selectFrom(member)
        .where(member.age.eq(
            JPAExpressions
                .select(memberSub.age.max())
                .from(memberSub)
        ))
        .fetch();
}

// 서브쿼리 IN
public List<Member> findMembersInSubquery() {
    QMember memberSub = new QMember("memberSub");

    return queryFactory
        .selectFrom(member)
        .where(member.age.in(
            JPAExpressions
                .select(memberSub.age)
                .from(memberSub)
                .where(memberSub.age.gt(10))
        ))
        .fetch();
}

// Select 절 서브쿼리
public List<Tuple> findMembersWithAvgAge() {
    QMember memberSub = new QMember("memberSub");

    return queryFactory
        .select(member.username,
            JPAExpressions
                .select(memberSub.age.avg())
                .from(memberSub))
        .from(member)
        .fetch();
}
```

### Case 문

```java
public List<String> findAgeGroup() {
    return queryFactory
        .select(new CaseBuilder()
            .when(member.age.between(0, 20)).then("0~20살")
            .when(member.age.between(21, 30)).then("21~30살")
            .otherwise("기타"))
        .from(member)
        .fetch();
}

// 복잡한 조건
public List<Tuple> findWithComplexCase() {
    NumberExpression<Integer> rankPath = new CaseBuilder()
        .when(member.age.between(0, 20)).then(2)
        .when(member.age.between(21, 30)).then(1)
        .otherwise(3);

    return queryFactory
        .select(member.username, member.age, rankPath)
        .from(member)
        .orderBy(rankPath.desc())
        .fetch();
}
```

### 벌크 연산

```java
// 벌크 업데이트
@Transactional
public long bulkAgePlus(int age) {
    long count = queryFactory
        .update(member)
        .set(member.age, member.age.add(1))
        .where(member.age.lt(age))
        .execute();

    // 영속성 컨텍스트 초기화 필수!
    em.flush();
    em.clear();

    return count;
}

// 벌크 삭제
@Transactional
public long bulkDelete(int age) {
    return queryFactory
        .delete(member)
        .where(member.age.gt(age))
        .execute();
}

// 문자열 치환
@Transactional
public long bulkUpdateName() {
    return queryFactory
        .update(member)
        .set(member.username, member.username.concat("_OLD"))
        .where(member.age.gt(50))
        .execute();
}
```

---

## JPA 핵심 어노테이션

### 엔티티 관련

```java
@Entity                     // JPA 엔티티 선언
@Table(name = "members",    // 테이블 매핑
    indexes = @Index(name = "idx_email", columnList = "email"),
    uniqueConstraints = @UniqueConstraint(columnNames = {"username", "email"}))
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
        name = "member_name",       // 컬럼명
        nullable = false,           // NOT NULL
        length = 100,               // VARCHAR 길이
        unique = true,              // 유니크 제약
        columnDefinition = "TEXT"   // DDL 직접 지정
    )
    private String username;

    @Enumerated(EnumType.STRING)    // ORDINAL 절대 사용 금지
    private MemberStatus status;

    @Lob                            // CLOB, BLOB 매핑
    private String description;

    @Transient                      // 매핑 제외
    private String tempField;

    @Embedded                       // 값 타입 포함
    private Address address;

    @ElementCollection              // 값 타입 컬렉션
    @CollectionTable(name = "member_phones",
        joinColumns = @JoinColumn(name = "member_id"))
    private List<String> phones = new ArrayList<>();
}
```

### 값 타입

```java
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Address {

    private String city;
    private String street;
    private String zipcode;

    // 값 타입은 불변으로!
    // Setter 없음
}

@Entity
public class Member {

    @Embedded
    private Address homeAddress;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "city", column = @Column(name = "work_city")),
        @AttributeOverride(name = "street", column = @Column(name = "work_street")),
        @AttributeOverride(name = "zipcode", column = @Column(name = "work_zipcode"))
    })
    private Address workAddress;
}
```

### 연관관계 어노테이션

```java
@Entity
public class Order {

    @ManyToOne(fetch = FetchType.LAZY)  // N:1
    @JoinColumn(name = "member_id",
        foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))  // FK 제약 없음
    private Member member;

    @OneToMany(mappedBy = "order",      // 1:N (읽기 전용)
        cascade = CascadeType.ALL,       // 영속성 전이
        orphanRemoval = true)            // 고아 객체 제거
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)   // 1:1
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;
}
```

### 상속 관계 매핑

```java
// 1. 조인 전략 (정규화, 권장)
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "dtype")
public abstract class Item {
    @Id @GeneratedValue
    private Long id;
    private String name;
}

@Entity
@DiscriminatorValue("BOOK")
public class Book extends Item {
    private String author;
    private String isbn;
}

// 2. 단일 테이블 전략 (성능 우선)
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype")
public abstract class Item { ... }

// 3. 구현 클래스마다 테이블 (비권장)
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Item { ... }
```

---

## Auditing 어노테이션

### 설정

```java
@Configuration
@EnableJpaAuditing
public class JpaConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext())
            .map(SecurityContext::getAuthentication)
            .filter(Authentication::isAuthenticated)
            .map(Authentication::getName);
    }
}
```

### BaseEntity

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity extends BaseTimeEntity {

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;
}

@Entity
public class Member extends BaseEntity {
    // createdAt, updatedAt, createdBy, updatedBy 자동 관리
}
```

---

## 검증 어노테이션

```java
@Entity
public class Member {

    @NotNull
    @Size(min = 2, max = 50)
    private String username;

    @Email
    private String email;

    @Min(0) @Max(150)
    private Integer age;

    @Pattern(regexp = "^\\d{3}-\\d{4}-\\d{4}$")
    private String phone;

    @NotBlank
    private String nickname;

    @Positive
    private Integer point;

    @PastOrPresent
    private LocalDate birthDate;

    @Future
    private LocalDateTime reservationTime;
}
```

### Custom Validator

```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneValidator.class)
public @interface Phone {
    String message() default "올바른 전화번호 형식이 아닙니다";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class PhoneValidator implements ConstraintValidator<Phone, String> {
    private static final Pattern PHONE_PATTERN =
        Pattern.compile("^01[016789]-\\d{3,4}-\\d{4}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        return PHONE_PATTERN.matcher(value).matches();
    }
}
```

---

*마지막 업데이트: 2026년 01월*
