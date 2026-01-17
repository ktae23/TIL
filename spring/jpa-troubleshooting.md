# JPA 실무 예제 및 트러블슈팅

JPA 실무에서 자주 마주치는 문제와 해결 방법을 정리합니다.

## 목차

- [N+1 문제](#n1-문제)
- [LazyInitializationException](#lazyinitializationexception)
- [영속성 컨텍스트 관련](#영속성-컨텍스트-관련)
- [벌크 연산 주의사항](#벌크-연산-주의사항)
- [성능 최적화](#성능-최적화)
- [실무 패턴](#실무-패턴)

---

## N+1 문제

### 문제 상황

```java
@Entity
public class Team {
    @Id @GeneratedValue
    private Long id;

    @OneToMany(mappedBy = "team")
    private List<Member> members = new ArrayList<>();
}

// N+1 발생!
List<Team> teams = teamRepository.findAll();  // 1번 쿼리
for (Team team : teams) {
    System.out.println(team.getMembers().size());  // 팀 개수(N)만큼 쿼리
}
// 결과: 1 + N 개의 쿼리 실행
```

### 해결방법 1: Fetch Join

```java
public interface TeamRepository extends JpaRepository<Team, Long> {

    @Query("SELECT t FROM Team t JOIN FETCH t.members")
    List<Team> findAllWithMembers();
}

// 또는 QueryDSL
public List<Team> findAllWithMembers() {
    return queryFactory
        .selectFrom(team)
        .join(team.members, member).fetchJoin()
        .distinct()  // 중복 제거
        .fetch();
}
```

### 해결방법 2: @EntityGraph

```java
public interface TeamRepository extends JpaRepository<Team, Long> {

    @EntityGraph(attributePaths = {"members"})
    @Query("SELECT t FROM Team t")
    List<Team> findAllWithMembers();

    // 또는 메서드명 쿼리에도 사용 가능
    @EntityGraph(attributePaths = {"members"})
    List<Team> findAll();
}
```

### 해결방법 3: Batch Size (글로벌 설정)

```yaml
# application.yml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 100
```

```java
// 또는 엔티티에 직접 설정
@Entity
public class Team {
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "team")
    private List<Member> members = new ArrayList<>();
}
```

### 컬렉션 Fetch Join 주의사항

```java
// 컬렉션 페치 조인은 페이징 불가!
@Query("SELECT t FROM Team t JOIN FETCH t.members")
Page<Team> findAllWithMembers(Pageable pageable);  // 경고 발생!
// HHH000104: firstResult/maxResults specified with collection fetch; applying in memory!

// 해결: 컬렉션은 Batch Size로 해결
@Query("SELECT t FROM Team t")
Page<Team> findAllTeams(Pageable pageable);
// + batch_fetch_size 설정으로 N+1 방지
```

---

## LazyInitializationException

### 문제 상황

```java
@Service
public class MemberService {

    public MemberDto getMember(Long id) {
        Member member = memberRepository.findById(id).orElseThrow();
        // 트랜잭션 종료 후 team 접근 시 예외!
        return new MemberDto(member.getName(), member.getTeam().getName());
    }
}
// org.hibernate.LazyInitializationException: could not initialize proxy
```

### 해결방법 1: Fetch Join 사용

```java
public interface MemberRepository extends JpaRepository<Member, Long> {

    @Query("SELECT m FROM Member m JOIN FETCH m.team WHERE m.id = :id")
    Optional<Member> findByIdWithTeam(@Param("id") Long id);
}

@Service
public class MemberService {
    public MemberDto getMember(Long id) {
        Member member = memberRepository.findByIdWithTeam(id).orElseThrow();
        return new MemberDto(member.getName(), member.getTeam().getName());
    }
}
```

### 해결방법 2: @Transactional 범위 조정

```java
@Service
@Transactional(readOnly = true)
public class MemberService {

    public MemberDto getMember(Long id) {
        Member member = memberRepository.findById(id).orElseThrow();
        // 트랜잭션 내에서 접근
        String teamName = member.getTeam().getName();
        return new MemberDto(member.getName(), teamName);
    }
}
```

### 해결방법 3: DTO 직접 조회

```java
public interface MemberRepository extends JpaRepository<Member, Long> {

    @Query("SELECT new com.example.dto.MemberDto(m.name, t.name) " +
           "FROM Member m JOIN m.team t WHERE m.id = :id")
    Optional<MemberDto> findMemberDtoById(@Param("id") Long id);
}
```

### OSIV (Open Session In View) 설정

```yaml
# application.yml
spring:
  jpa:
    open-in-view: false  # 기본값 true → 실무에서는 false 권장
```

```
OSIV ON:  요청 시작 ~ 응답 끝까지 영속성 컨텍스트 유지 (DB 커넥션 오래 점유)
OSIV OFF: 트랜잭션 범위 내에서만 영속성 컨텍스트 유지 (권장)
```

---

## 영속성 컨텍스트 관련

### 변경 감지가 안 될 때

```java
// 문제: 준영속 상태 엔티티
@Transactional
public void updateMember(MemberUpdateDto dto) {
    // 새로운 객체이므로 영속성 컨텍스트가 관리하지 않음
    Member member = new Member();
    member.setId(dto.getId());
    member.setName(dto.getName());
    // 변경 감지 안됨!
}

// 해결: 영속 상태로 조회 후 변경
@Transactional
public void updateMember(MemberUpdateDto dto) {
    Member member = memberRepository.findById(dto.getId()).orElseThrow();
    member.changeName(dto.getName());  // 변경 감지 동작
}
```

### merge() vs 변경 감지

```java
// merge: 모든 필드 업데이트 (null도 업데이트!)
@Transactional
public void updateWithMerge(Member detachedMember) {
    Member merged = em.merge(detachedMember);  // 비권장
}

// 변경 감지: 변경된 필드만 업데이트 (권장)
@Transactional
public void updateWithDirtyChecking(Long id, String name) {
    Member member = memberRepository.findById(id).orElseThrow();
    member.changeName(name);
}
```

### 영속성 컨텍스트 초기화

```java
@Transactional
public void bulkOperation() {
    // 벌크 연산은 영속성 컨텍스트를 무시하고 DB 직접 수정
    queryFactory
        .update(member)
        .set(member.age, member.age.add(1))
        .execute();

    // 영속성 컨텍스트 초기화 필수!
    em.flush();
    em.clear();

    // 이후 조회는 DB에서 새로 가져옴
    Member member = memberRepository.findById(1L).orElseThrow();
}
```

---

## 벌크 연산 주의사항

### @Modifying 사용

```java
public interface MemberRepository extends JpaRepository<Member, Long> {

    // clearAutomatically: 벌크 연산 후 영속성 컨텍스트 자동 초기화
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Member m SET m.age = m.age + 1 WHERE m.age < :age")
    int bulkAgePlus(@Param("age") int age);

    // flushAutomatically: 벌크 연산 전 영속성 컨텍스트 플러시
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Member m WHERE m.status = :status")
    int deleteByStatus(@Param("status") MemberStatus status);
}
```

### 벌크 연산 순서

```java
@Transactional
public void process() {
    // 1. 먼저 조회 (영속성 컨텍스트에 캐시됨)
    Member member = memberRepository.findById(1L).orElseThrow();
    System.out.println(member.getAge());  // 20

    // 2. 벌크 연산 (DB만 수정)
    memberRepository.bulkAgePlus(30);

    // 3. 영속성 컨텍스트 초기화 없이 조회하면 잘못된 값!
    Member cached = memberRepository.findById(1L).orElseThrow();
    System.out.println(cached.getAge());  // 여전히 20 (1차 캐시)

    // @Modifying(clearAutomatically = true) 사용 시
    // 자동으로 clear 되어 DB에서 다시 조회
}
```

---

## 성능 최적화

### 읽기 전용 쿼리 최적화

```java
@Service
public class MemberQueryService {

    // 읽기 전용 트랜잭션: 스냅샷 저장 안함, 플러시 안함
    @Transactional(readOnly = true)
    public List<MemberDto> getMembers() {
        return memberRepository.findAllMemberDto();
    }
}

// 하이버네이트 힌트 사용
public interface MemberRepository extends JpaRepository<Member, Long> {

    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    @Query("SELECT m FROM Member m")
    List<Member> findAllReadOnly();
}
```

### 페이징 성능 최적화

```java
// 커버링 인덱스 활용
public Page<MemberDto> searchWithCoveringIndex(Pageable pageable) {
    // 1. ID만 조회 (커버링 인덱스)
    List<Long> ids = queryFactory
        .select(member.id)
        .from(member)
        .where(member.status.eq(MemberStatus.ACTIVE))
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .orderBy(member.id.desc())
        .fetch();

    if (ids.isEmpty()) {
        return Page.empty();
    }

    // 2. ID로 실제 데이터 조회
    List<Member> members = queryFactory
        .selectFrom(member)
        .where(member.id.in(ids))
        .orderBy(member.id.desc())
        .fetch();

    // 3. DTO 변환
    List<MemberDto> content = members.stream()
        .map(MemberDto::from)
        .collect(Collectors.toList());

    // 4. 카운트 쿼리
    Long total = queryFactory
        .select(member.count())
        .from(member)
        .where(member.status.eq(MemberStatus.ACTIVE))
        .fetchOne();

    return new PageImpl<>(content, pageable, total);
}
```

### No Offset 페이징 (무한 스크롤)

```java
public List<MemberDto> searchNoOffset(Long lastId, int size) {
    return queryFactory
        .select(new QMemberDto(member.id, member.name, member.age))
        .from(member)
        .where(
            member.status.eq(MemberStatus.ACTIVE),
            ltMemberId(lastId)  // id < lastId 조건
        )
        .orderBy(member.id.desc())
        .limit(size)
        .fetch();
}

private BooleanExpression ltMemberId(Long lastId) {
    return lastId != null ? member.id.lt(lastId) : null;
}
```

### 통계 쿼리 분리

```java
// 통계용 테이블 별도 관리
@Entity
@Table(name = "member_statistics")
public class MemberStatistics {
    @Id
    private Long id;

    private Long totalCount;
    private Long activeCount;
    private LocalDateTime calculatedAt;
}

// 배치로 주기적 갱신
@Scheduled(cron = "0 0 * * * *")  // 매시간
@Transactional
public void updateStatistics() {
    Long total = memberRepository.count();
    Long active = memberRepository.countByStatus(MemberStatus.ACTIVE);

    MemberStatistics stats = statisticsRepository.findById(1L)
        .orElse(new MemberStatistics(1L));
    stats.update(total, active);
}
```

---

## 실무 패턴

### Repository 계층 분리

```java
// 기본 CRUD - Spring Data JPA
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    boolean existsByEmail(String email);
}

// 복잡한 조회 - QueryDSL
@Repository
@RequiredArgsConstructor
public class MemberQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<MemberDto> search(MemberSearchCondition condition, Pageable pageable) {
        // 복잡한 동적 쿼리
    }

    public List<MemberStatDto> getStatistics(LocalDate from, LocalDate to) {
        // 통계 쿼리
    }
}

// Service에서 주입
@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final MemberQueryRepository memberQueryRepository;
}
```

### 엔티티 생성 패턴

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    // 정적 팩토리 메서드
    public static Order createOrder(Member member, List<OrderItem> orderItems) {
        Order order = new Order();
        order.setMember(member);
        orderItems.forEach(order::addOrderItem);
        order.orderDate = LocalDateTime.now();
        order.status = OrderStatus.ORDER;
        return order;
    }

    // 연관관계 편의 메서드
    private void setMember(Member member) {
        this.member = member;
        member.getOrders().add(this);
    }

    private void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    // 비즈니스 로직
    public void cancel() {
        if (status == OrderStatus.DELIVERY) {
            throw new IllegalStateException("배송중인 상품은 취소가 불가능합니다.");
        }
        this.status = OrderStatus.CANCEL;
        orderItems.forEach(OrderItem::cancel);
    }
}
```

### 상속보다 조합

```java
// 상속 대신 Embedded 사용
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Money {
    private BigDecimal amount;
    private Currency currency;

    public Money(BigDecimal amount, Currency currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public Money add(Money money) {
        validateCurrency(money);
        return new Money(this.amount.add(money.amount), this.currency);
    }

    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }
}

@Entity
public class Product {
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "price")),
        @AttributeOverride(name = "currency", column = @Column(name = "price_currency"))
    })
    private Money price;
}
```

### Soft Delete 패턴

```java
@Entity
@Where(clause = "deleted = false")  // 기본 조회 시 삭제된 데이터 제외
@SQLDelete(sql = "UPDATE members SET deleted = true WHERE id = ?")
public class Member {

    @Id @GeneratedValue
    private Long id;

    private boolean deleted = false;

    private LocalDateTime deletedAt;
}

// 삭제된 데이터도 조회 필요 시
public interface MemberRepository extends JpaRepository<Member, Long> {

    @Query("SELECT m FROM Member m WHERE m.id = :id")
    @Where(clause = "")  // 조건 무시
    Optional<Member> findByIdIncludeDeleted(@Param("id") Long id);
}
```

---

*마지막 업데이트: 2026년 01월*
