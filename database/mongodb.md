# MongoDB 종합 정리

MongoDB는 JSON과 유사한 BSON(Binary JSON) 문서를 저장하는 **NoSQL Document Database**다. 스키마가 유연하고, 수평 확장(Sharding)에 강하며, 복잡한 쿼리와 Aggregation을 지원한다.

---

## 목차

- [1. 핵심 개념](#1-핵심-개념)
- [2. CRUD 연산](#2-crud-연산)
- [3. 쿼리 연산자](#3-쿼리-연산자)
- [4. Aggregation Framework](#4-aggregation-framework)
- [5. 인덱스와 성능 최적화](#5-인덱스와-성능-최적화)
- [6. 스키마 설계 패턴](#6-스키마-설계-패턴)
- [7. Spring Data MongoDB](#7-spring-data-mongodb)
- [8. 트랜잭션](#8-트랜잭션)
- [9. RDBMS vs MongoDB 비교](#9-rdbms-vs-mongodb-비교)
- [10. 운영 팁](#10-운영-팁)

---

## 1. 핵심 개념

### 용어 매핑

| RDBMS | MongoDB |
|-------|---------|
| Database | Database |
| Table | Collection |
| Row | Document |
| Column | Field |
| JOIN | Embedding / `$lookup` |
| Primary Key | `_id` (자동 생성) |

### BSON Document 구조

```json
{
  "_id": ObjectId("507f1f77bcf86cd799439011"),
  "name": "홍길동",
  "age": 30,
  "address": {
    "city": "서울",
    "zipcode": "06000"
  },
  "hobbies": ["축구", "독서"],
  "createdAt": ISODate("2026-03-14T09:00:00Z")
}
```

- **`_id`**: 모든 Document에 자동 부여되는 고유 키 (12바이트 ObjectId)
- **Embedded Document**: `address`처럼 문서 안에 문서를 중첩
- **Array**: `hobbies`처럼 배열 타입 지원

### mongosh 접속

```bash
# 로컬 접속
mongosh

# URI로 접속
mongosh "mongodb://localhost:27017/mydb"

# Atlas 접속
mongosh "mongodb+srv://user:pass@cluster.mongodb.net/mydb"
```

---

## 2. CRUD 연산

### Create (삽입)

```javascript
// 단일 문서 삽입
db.users.insertOne({
  name: "김철수",
  email: "chulsoo@example.com",
  age: 25,
  role: "user"
});

// 다수 문서 삽입
db.users.insertMany([
  { name: "이영희", email: "young@example.com", age: 28, role: "admin" },
  { name: "박민수", email: "minsu@example.com", age: 32, role: "user" },
  { name: "최지은", email: "jieun@example.com", age: 22, role: "user" }
]);
```

### Read (조회)

```javascript
// 전체 조회
db.users.find();

// 조건 조회
db.users.find({ role: "admin" });

// 단일 문서 조회
db.users.findOne({ email: "chulsoo@example.com" });

// 프로젝션 (필요한 필드만 선택)
db.users.find(
  { role: "user" },
  { name: 1, email: 1, _id: 0 }
);

// 정렬 + 제한
db.users.find()
  .sort({ age: -1 })   // 나이 내림차순
  .limit(5)             // 5건만
  .skip(10);            // 10건 건너뛰기 (페이징)
```

### Update (수정)

```javascript
// 단일 문서 수정
db.users.updateOne(
  { email: "chulsoo@example.com" },
  { $set: { age: 26, updatedAt: new Date() } }
);

// 다수 문서 수정
db.users.updateMany(
  { role: "user" },
  { $set: { isActive: true } }
);

// 필드 증가
db.users.updateOne(
  { name: "김철수" },
  { $inc: { loginCount: 1 } }
);

// 배열에 요소 추가
db.users.updateOne(
  { name: "김철수" },
  { $push: { hobbies: "게임" } }
);

// 배열에서 요소 제거
db.users.updateOne(
  { name: "김철수" },
  { $pull: { hobbies: "게임" } }
);

// upsert: 없으면 삽입, 있으면 수정
db.users.updateOne(
  { email: "new@example.com" },
  { $set: { name: "신규유저", role: "user" } },
  { upsert: true }
);
```

### Delete (삭제)

```javascript
// 단일 삭제
db.users.deleteOne({ email: "chulsoo@example.com" });

// 다수 삭제
db.users.deleteMany({ isActive: false });

// 전체 삭제 (주의!)
db.users.deleteMany({});
```

---

## 3. 쿼리 연산자

### 비교 연산자

```javascript
// $eq, $ne, $gt, $gte, $lt, $lte
db.users.find({ age: { $gte: 25, $lte: 35 } });

// $in, $nin
db.users.find({ role: { $in: ["admin", "moderator"] } });
```

### 논리 연산자

```javascript
// $and (암시적)
db.users.find({ age: { $gte: 25 }, role: "user" });

// $or
db.users.find({
  $or: [
    { role: "admin" },
    { age: { $gte: 30 } }
  ]
});

// $not
db.users.find({ age: { $not: { $lt: 20 } } });
```

### 요소/배열 연산자

```javascript
// 필드 존재 여부
db.users.find({ phone: { $exists: true } });

// 타입 체크
db.users.find({ age: { $type: "number" } });

// 배열 크기
db.users.find({ hobbies: { $size: 3 } });

// 배열 요소 조건 (모든 조건을 만족하는 단일 요소)
db.orders.find({
  items: {
    $elemMatch: { product: "노트북", quantity: { $gte: 2 } }
  }
});
```

### 정규식

```javascript
// 이름이 '김'으로 시작하는 사용자
db.users.find({ name: { $regex: /^김/, $options: "i" } });
```

---

## 4. Aggregation Framework

Aggregation Pipeline은 **데이터를 단계별로 변환/집계**하는 강력한 프레임워크다. SQL의 GROUP BY, JOIN, 서브쿼리를 대체한다.

### 기본 구조

```javascript
db.collection.aggregate([
  { $stage1: { ... } },
  { $stage2: { ... } },
  // ...
]);
```

### 주요 스테이지 예제

```javascript
// 주문 데이터 기반 분석
db.orders.aggregate([
  // 1. 필터링 (WHERE)
  { $match: { status: "completed", orderDate: { $gte: ISODate("2026-01-01") } } },

  // 2. 필드 추가/변환
  { $addFields: {
    totalAmount: { $multiply: ["$price", "$quantity"] },
    month: { $month: "$orderDate" }
  }},

  // 3. 그룹핑 (GROUP BY)
  { $group: {
    _id: { userId: "$userId", month: "$month" },
    totalSpent: { $sum: "$totalAmount" },
    orderCount: { $sum: 1 },
    avgOrderAmount: { $avg: "$totalAmount" },
    maxOrder: { $max: "$totalAmount" }
  }},

  // 4. 정렬
  { $sort: { totalSpent: -1 } },

  // 5. 상위 10명
  { $limit: 10 },

  // 6. 출력 필드 정리
  { $project: {
    _id: 0,
    userId: "$_id.userId",
    month: "$_id.month",
    totalSpent: 1,
    orderCount: 1,
    avgOrderAmount: { $round: ["$avgOrderAmount", 0] }
  }}
]);
```

### $lookup (JOIN)

```javascript
// users와 orders 조인
db.users.aggregate([
  {
    $lookup: {
      from: "orders",           // 조인할 컬렉션
      localField: "_id",        // users의 필드
      foreignField: "userId",   // orders의 필드
      as: "userOrders"          // 결과 배열 필드명
    }
  },
  {
    $addFields: {
      orderCount: { $size: "$userOrders" }
    }
  },
  {
    $match: { orderCount: { $gte: 1 } }
  }
]);
```

### $unwind (배열 펼치기)

```javascript
// 태그별 게시글 수 집계
db.posts.aggregate([
  { $unwind: "$tags" },
  { $group: {
    _id: "$tags",
    count: { $sum: 1 }
  }},
  { $sort: { count: -1 } }
]);
```

### $bucket (범위 그룹핑)

```javascript
// 나이대별 사용자 분포
db.users.aggregate([
  {
    $bucket: {
      groupBy: "$age",
      boundaries: [0, 20, 30, 40, 50, 100],
      default: "기타",
      output: {
        count: { $sum: 1 },
        names: { $push: "$name" }
      }
    }
  }
]);
```

---

## 5. 인덱스와 성능 최적화

### 인덱스 생성

```javascript
// 단일 필드 인덱스
db.users.createIndex({ email: 1 });          // 오름차순
db.users.createIndex({ createdAt: -1 });     // 내림차순

// 복합 인덱스 (ESR Rule: Equality → Sort → Range)
db.orders.createIndex({ status: 1, orderDate: -1, totalAmount: 1 });

// 유니크 인덱스
db.users.createIndex({ email: 1 }, { unique: true });

// TTL 인덱스 (자동 삭제 — 세션, 로그 등)
db.sessions.createIndex({ expireAt: 1 }, { expireAfterSeconds: 0 });

// 텍스트 인덱스 (전문 검색)
db.articles.createIndex({ title: "text", content: "text" });

// Partial 인덱스 (조건부)
db.orders.createIndex(
  { userId: 1 },
  { partialFilterExpression: { status: "active" } }
);
```

### explain()으로 쿼리 분석

```javascript
db.users.find({ email: "test@example.com" }).explain("executionStats");
```

**핵심 지표**:

| 지표 | 좋은 값 | 나쁜 값 |
|------|---------|---------|
| `stage` | `IXSCAN` | `COLLSCAN` (풀스캔) |
| `nReturned` vs `totalDocsExamined` | 비슷함 | 차이가 큼 |
| `executionTimeMillis` | 낮을수록 좋음 | - |

### 인덱스 관리

```javascript
// 인덱스 목록 조회
db.users.getIndexes();

// 인덱스 삭제
db.users.dropIndex("email_1");

// 인덱스 사용 통계
db.users.aggregate([{ $indexStats: {} }]);
```

### 인덱스 설계 원칙 (ESR Rule)

**복합 인덱스 필드 순서**: Equality → Sort → Range

```javascript
// 쿼리: status가 "active"이고, createdAt 내림차순, age가 25~35
// 좋은 인덱스:
db.users.createIndex({ status: 1, createdAt: -1, age: 1 });
//                      E(quality)  S(ort)         R(ange)
```

---

## 6. 스키마 설계 패턴

### Embedding vs Referencing

```javascript
// ✅ Embedding: 1:1 또는 1:Few 관계, 함께 조회하는 데이터
{
  _id: ObjectId("..."),
  name: "상품A",
  price: 29900,
  reviews: [                          // 리뷰를 문서 안에 내장
    { user: "김철수", rating: 5, comment: "좋아요" },
    { user: "이영희", rating: 4, comment: "괜찮아요" }
  ]
}

// ✅ Referencing: 1:Many 또는 Many:Many 관계, 독립 조회
// users 컬렉션
{ _id: ObjectId("u1"), name: "김철수" }

// orders 컬렉션
{ _id: ObjectId("o1"), userId: ObjectId("u1"), total: 50000 }
```

### 판단 기준

| 기준 | Embedding | Referencing |
|------|-----------|-------------|
| 관계 | 1:1, 1:Few | 1:Many, Many:Many |
| 데이터 크기 | 작음 (16MB 제한) | 제한 없음 |
| 읽기 패턴 | 항상 함께 조회 | 독립적으로 조회 |
| 업데이트 | 드물게 변경 | 자주 변경 |
| 일관성 | 단일 Document 원자성 | 참조 무결성 직접 관리 |

### Bucket Pattern (시계열 데이터)

```javascript
// 센서 데이터를 1시간 단위로 버킷에 모음
{
  sensorId: "sensor-001",
  startDate: ISODate("2026-03-14T10:00:00Z"),
  endDate: ISODate("2026-03-14T10:59:59Z"),
  count: 120,
  measurements: [
    { ts: ISODate("2026-03-14T10:00:30Z"), temp: 22.5, humidity: 45 },
    { ts: ISODate("2026-03-14T10:01:00Z"), temp: 22.6, humidity: 44 },
    // ... 더 많은 측정값
  ]
}
```

---

## 7. Spring Data MongoDB

### 의존성 추가

```groovy
// build.gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'
}
```

### 설정

```yaml
# application.yml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/mydb
      # 또는 개별 설정
      # host: localhost
      # port: 27017
      # database: mydb
      # username: admin
      # password: secret
```

### Entity 정의

```java
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String name;

    private int age;

    @Field("role")
    private UserRole role;

    private Address address;             // Embedded Document

    private List<String> hobbies;        // Array

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // 생성자, getter/setter 생략
}

// Embedded Document
public class Address {
    private String city;
    private String zipcode;
}

public enum UserRole {
    USER, ADMIN, MODERATOR
}
```

### Repository

```java
public interface UserRepository extends MongoRepository<User, String> {

    // 메서드 이름 기반 쿼리
    List<User> findByRole(UserRole role);

    List<User> findByAgeBetween(int min, int max);

    Optional<User> findByEmail(String email);

    List<User> findByNameContainingIgnoreCase(String keyword);

    // 정렬 + 페이징
    Page<User> findByRole(UserRole role, Pageable pageable);

    // @Query 어노테이션
    @Query("{ 'age': { $gte: ?0 }, 'role': ?1 }")
    List<User> findActiveAdults(int minAge, UserRole role);

    // 프로젝션
    @Query(value = "{ 'role': ?0 }", fields = "{ 'name': 1, 'email': 1 }")
    List<User> findNameAndEmailByRole(UserRole role);

    // 존재 여부
    boolean existsByEmail(String email);

    // 삭제
    long deleteByRole(UserRole role);
}
```

### MongoTemplate (복잡한 쿼리)

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final MongoTemplate mongoTemplate;

    // 동적 쿼리
    public List<User> search(String name, Integer minAge, UserRole role) {
        Query query = new Query();

        if (name != null) {
            query.addCriteria(Criteria.where("name").regex(name, "i"));
        }
        if (minAge != null) {
            query.addCriteria(Criteria.where("age").gte(minAge));
        }
        if (role != null) {
            query.addCriteria(Criteria.where("role").is(role));
        }

        query.with(Sort.by(Sort.Direction.DESC, "createdAt"));
        query.limit(20);

        return mongoTemplate.find(query, User.class);
    }

    // Update
    public long deactivateOldUsers(int days) {
        Query query = Query.query(
            Criteria.where("lastLoginAt")
                .lt(LocalDateTime.now().minusDays(days))
        );
        Update update = new Update()
            .set("isActive", false)
            .set("updatedAt", LocalDateTime.now());

        UpdateResult result = mongoTemplate.updateMulti(query, update, User.class);
        return result.getModifiedCount();
    }

    // Aggregation
    public List<Document> getUserStatsByRole() {
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.group("role")
                .count().as("count")
                .avg("age").as("avgAge")
                .max("createdAt").as("latestJoin"),
            Aggregation.sort(Sort.Direction.DESC, "count"),
            Aggregation.project()
                .and("_id").as("role")
                .andInclude("count", "avgAge", "latestJoin")
                .andExclude("_id")
        );

        return mongoTemplate
            .aggregate(aggregation, "users", Document.class)
            .getMappedResults();
    }

    // Upsert
    public void upsertUser(String email, String name) {
        Query query = Query.query(Criteria.where("email").is(email));
        Update update = new Update()
            .set("name", name)
            .set("updatedAt", LocalDateTime.now())
            .setOnInsert("createdAt", LocalDateTime.now())
            .setOnInsert("role", UserRole.USER);

        mongoTemplate.upsert(query, update, User.class);
    }
}
```

### Auditing 설정

```java
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
```

```java
@Document(collection = "users")
public class User {
    // ...

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String modifiedBy;
}
```

---

## 8. 트랜잭션

MongoDB 4.0부터 **Multi-Document 트랜잭션**을 지원한다. (Replica Set 필수)

### mongosh

```javascript
const session = db.getMongo().startSession();
session.startTransaction();

try {
  const users = session.getDatabase("mydb").users;
  const accounts = session.getDatabase("mydb").accounts;

  users.updateOne({ _id: userId }, { $set: { status: "premium" } });
  accounts.updateOne({ userId: userId }, { $inc: { balance: -10000 } });

  session.commitTransaction();
} catch (e) {
  session.abortTransaction();
  throw e;
} finally {
  session.endSession();
}
```

### Spring Data MongoDB

```java
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final MongoTemplate mongoTemplate;
    private final MongoTransactionManager transactionManager;

    @Transactional
    public void processPayment(String userId, int amount) {
        // 잔액 차감
        Query accountQuery = Query.query(Criteria.where("userId").is(userId));
        Update debit = new Update().inc("balance", -amount);
        mongoTemplate.updateFirst(accountQuery, debit, Account.class);

        // 결제 내역 생성
        Payment payment = new Payment(userId, amount, LocalDateTime.now());
        mongoTemplate.insert(payment);
    }
}

// TransactionManager 설정
@Configuration
public class MongoTransactionConfig {

    @Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
}
```

---

## 9. RDBMS vs MongoDB 비교

| 항목 | RDBMS (MySQL 등) | MongoDB |
|------|------------------|---------|
| **데이터 모델** | 정규화된 테이블 | 유연한 Document |
| **스키마** | 고정 스키마 (DDL) | Schema-less / 유연 |
| **확장** | 수직 확장 (Scale-up) | 수평 확장 (Sharding) |
| **JOIN** | 네이티브 JOIN | `$lookup` 또는 Embedding |
| **트랜잭션** | 강력한 ACID | Multi-Doc 트랜잭션 (4.0+) |
| **적합한 사용처** | 정형 데이터, 복잡한 관계 | 비정형, 빠른 변경, 대용량 |
| **쿼리 언어** | SQL | MQL (MongoDB Query Language) |
| **인덱스** | B-Tree, Hash 등 | B-Tree, Text, Geospatial 등 |

### MongoDB가 적합한 경우

- 스키마가 자주 변경되는 **애자일 개발**
- **대용량 로그/이벤트** 데이터 저장
- **실시간 분석** (Aggregation Pipeline)
- **Content Management** (CMS, 블로그)
- **IoT 센서 데이터** (Time Series Collection)
- **사용자 프로필** (다양한 속성)

### RDBMS가 적합한 경우

- 복잡한 **JOIN이 빈번**한 비즈니스 로직
- **강력한 트랜잭션** 보장이 필요한 금융 시스템
- **데이터 무결성**이 최우선인 시스템

---

## 10. 운영 팁

### Replica Set 기본 구성

```javascript
// 3노드 Replica Set (Primary 1 + Secondary 2)
rs.initiate({
  _id: "myReplicaSet",
  members: [
    { _id: 0, host: "mongo1:27017" },
    { _id: 1, host: "mongo2:27017" },
    { _id: 2, host: "mongo3:27017" }
  ]
});
```

### Docker Compose로 로컬 환경 구성

```yaml
# docker-compose.yml
version: "3.8"
services:
  mongodb:
    image: mongo:7
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: password
    volumes:
      - mongo-data:/data/db

  mongo-express:
    image: mongo-express
    ports:
      - "8081:8081"
    environment:
      ME_CONFIG_MONGODB_ADMINUSERNAME: admin
      ME_CONFIG_MONGODB_ADMINPASSWORD: password
      ME_CONFIG_MONGODB_URL: mongodb://admin:password@mongodb:27017/

volumes:
  mongo-data:
```

### 유용한 모니터링 명령어

```javascript
// 서버 상태
db.serverStatus();

// 현재 실행 중인 쿼리
db.currentOp({ "active": true, "secs_running": { $gte: 5 } });

// 느린 쿼리 킬
db.killOp(opId);

// 컬렉션 통계
db.users.stats();

// 데이터베이스 크기
db.stats();
```

### 백업/복원

```bash
# 백업
mongodump --uri="mongodb://localhost:27017/mydb" --out=/backup/$(date +%Y%m%d)

# 복원
mongorestore --uri="mongodb://localhost:27017/mydb" /backup/20260314/mydb
```

---

*마지막 업데이트: 2026년 03월*
