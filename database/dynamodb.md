# DynamoDB 종합 정리

AWS의 완전 관리형 NoSQL 데이터베이스 서비스인 DynamoDB의 핵심 개념, 데이터 모델링, 싱글 테이블 설계, 그리고 RDB와의 비교를 코드 예제 중심으로 정리한다.

## 목차

- [1. 핵심 개념](#1-핵심-개념)
- [2. 데이터 모델링](#2-데이터-모델링)
- [3. CRUD 기본 조작](#3-crud-기본-조작)
- [4. 쿼리와 스캔](#4-쿼리와-스캔)
- [5. 보조 인덱스 (GSI / LSI)](#5-보조-인덱스-gsi--lsi)
- [6. 싱글 테이블 설계](#6-싱글-테이블-설계)
- [7. DynamoDB vs RDB 비교](#7-dynamodb-vs-rdb-비교)
- [8. 용량 모드와 과금](#8-용량-모드와-과금)
- [9. DynamoDB Streams & TTL](#9-dynamodb-streams--ttl)
- [10. 실전 팁](#10-실전-팁)

---

## 1. 핵심 개념

### 테이블 구조

DynamoDB는 **테이블(Table) → 아이템(Item) → 속성(Attribute)** 구조로 구성된다.

| 개념 | RDB 대응 | 설명 |
|------|----------|------|
| Table | Table | 데이터의 최상위 컨테이너 |
| Item | Row | 하나의 데이터 레코드 (최대 400KB) |
| Attribute | Column | 키-값 쌍. **스키마리스** — 아이템마다 다를 수 있음 |
| Partition Key (PK) | Primary Key | 해시 함수를 통해 파티션을 결정하는 필수 키 |
| Sort Key (SK) | Composite Key 일부 | 같은 PK 내에서 정렬 기준이 되는 선택적 키 |

### 키 설계의 핵심

```
# 단순 기본 키 (Partition Key만)
PK: userId = "user-001"

# 복합 기본 키 (Partition Key + Sort Key)
PK: userId = "user-001"
SK: orderDate = "2026-03-14T10:30:00Z"
```

- **Partition Key(PK)**: 데이터 분산의 핵심. 카디널리티가 높을수록 좋다
- **Sort Key(SK)**: 같은 PK 내에서 범위 쿼리(`BETWEEN`, `begins_with`)를 가능하게 한다

### 데이터 타입

```python
# DynamoDB가 지원하는 데이터 타입
item = {
    "userId":    {"S": "user-001"},         # String
    "age":       {"N": "28"},               # Number (문자열로 전송)
    "isActive":  {"BOOL": True},            # Boolean
    "tags":      {"SS": ["python", "aws"]}, # String Set
    "scores":    {"NS": ["95", "87"]},      # Number Set
    "metadata":  {"M": {                    # Map (중첩 객체)
        "level": {"S": "premium"}
    }},
    "history":   {"L": [                    # List
        {"S": "login"}, {"S": "purchase"}
    ]},
    "avatar":    {"B": b"binary-data"},     # Binary
    "deletedAt": {"NULL": True}             # Null
}
```

---

## 2. 데이터 모델링

### 액세스 패턴 우선 설계

RDB와 가장 큰 차이점: **쿼리 패턴을 먼저 정의하고, 테이블을 설계한다.**

```
# Step 1: 액세스 패턴 나열
1. 사용자 ID로 프로필 조회
2. 사용자의 주문 목록 조회 (최신순)
3. 특정 날짜 범위의 주문 조회
4. 주문 ID로 단건 조회
5. 사용자의 최근 주문 N건 조회

# Step 2: 키 설계
PK = USER#<userId>
SK = ORDER#<orderDate>#<orderId>   ← 날짜 역순 정렬 가능
```

### 복합 Sort Key 패턴

```python
# 계층적 데이터를 Sort Key로 표현
items = [
    # 사용자 프로필
    {"PK": "USER#001", "SK": "PROFILE",           "name": "김개발", "email": "dev@ex.com"},
    # 사용자 주문
    {"PK": "USER#001", "SK": "ORDER#2026-03-14#A", "amount": 50000, "status": "completed"},
    {"PK": "USER#001", "SK": "ORDER#2026-03-13#B", "amount": 30000, "status": "pending"},
    # 사용자 주소
    {"PK": "USER#001", "SK": "ADDRESS#HOME",       "city": "서울", "zip": "06234"},
    {"PK": "USER#001", "SK": "ADDRESS#WORK",       "city": "판교", "zip": "13487"},
]
```

---

## 3. CRUD 기본 조작

### boto3 클라이언트 설정

```python
import boto3
from boto3.dynamodb.conditions import Key, Attr

# Resource 방식 (고수준 API — 타입 변환 자동)
dynamodb = boto3.resource("dynamodb", region_name="ap-northeast-2")
table = dynamodb.Table("Users")

# Client 방식 (저수준 API — 타입 명시 필요)
client = boto3.client("dynamodb", region_name="ap-northeast-2")
```

### PutItem (생성/덮어쓰기)

```python
# 아이템 생성
table.put_item(
    Item={
        "PK": "USER#001",
        "SK": "PROFILE",
        "name": "김개발",
        "email": "dev@example.com",
        "age": 28,
        "createdAt": "2026-03-14T10:00:00Z"
    }
)

# 조건부 생성 (이미 존재하면 실패)
table.put_item(
    Item={
        "PK": "USER#001",
        "SK": "PROFILE",
        "name": "김개발",
        "email": "dev@example.com"
    },
    ConditionExpression="attribute_not_exists(PK)"  # PK가 없을 때만 생성
)
```

### GetItem (단건 조회)

```python
response = table.get_item(
    Key={
        "PK": "USER#001",
        "SK": "PROFILE"
    },
    ProjectionExpression="#n, email, age",  # 특정 속성만 조회
    ExpressionAttributeNames={"#n": "name"}  # name은 예약어
)
item = response.get("Item")

# Consistent Read (강한 일관성 읽기)
response = table.get_item(
    Key={"PK": "USER#001", "SK": "PROFILE"},
    ConsistentRead=True  # 최신 데이터 보장 (비용 2배)
)
```

### UpdateItem (부분 수정)

```python
# 특정 속성만 업데이트
table.update_item(
    Key={"PK": "USER#001", "SK": "PROFILE"},
    UpdateExpression="SET #n = :name, age = :age, updatedAt = :now",
    ExpressionAttributeNames={"#n": "name"},
    ExpressionAttributeValues={
        ":name": "김시니어",
        ":age": 29,
        ":now": "2026-03-14T12:00:00Z"
    },
    ReturnValues="UPDATED_NEW"  # 업데이트된 속성 반환
)

# 원자적 카운터 (Atomic Counter)
table.update_item(
    Key={"PK": "PRODUCT#100", "SK": "METADATA"},
    UpdateExpression="SET viewCount = viewCount + :inc",
    ExpressionAttributeValues={":inc": 1}
)

# 리스트에 요소 추가
table.update_item(
    Key={"PK": "USER#001", "SK": "PROFILE"},
    UpdateExpression="SET tags = list_append(if_not_exists(tags, :empty), :newTags)",
    ExpressionAttributeValues={
        ":empty": [],
        ":newTags": ["dynamodb"]
    }
)
```

### DeleteItem (삭제)

```python
# 단건 삭제
table.delete_item(
    Key={"PK": "USER#001", "SK": "PROFILE"}
)

# 조건부 삭제
table.delete_item(
    Key={"PK": "USER#001", "SK": "PROFILE"},
    ConditionExpression="age < :threshold",
    ExpressionAttributeValues={":threshold": 18}
)
```

### BatchWriteItem (배치 쓰기)

```python
with table.batch_writer() as batch:
    for i in range(100):
        batch.put_item(Item={
            "PK": f"USER#{i:03d}",
            "SK": "PROFILE",
            "name": f"user-{i}",
            "email": f"user{i}@example.com"
        })
# batch_writer()가 자동으로 25개씩 나눠서 전송
# 미처리 항목(UnprocessedItems)도 자동 재시도
```

### TransactWriteItems (트랜잭션)

```python
client.transact_write_items(
    TransactItems=[
        {
            "Update": {
                "TableName": "Users",
                "Key": {"PK": {"S": "USER#001"}, "SK": {"S": "PROFILE"}},
                "UpdateExpression": "SET balance = balance - :amount",
                "ConditionExpression": "balance >= :amount",
                "ExpressionAttributeValues": {":amount": {"N": "10000"}}
            }
        },
        {
            "Update": {
                "TableName": "Users",
                "Key": {"PK": {"S": "USER#002"}, "SK": {"S": "PROFILE"}},
                "UpdateExpression": "SET balance = balance + :amount",
                "ExpressionAttributeValues": {":amount": {"N": "10000"}}
            }
        }
    ]
)
# 두 업데이트가 원자적으로 실행 — 하나라도 실패하면 전체 롤백
```

---

## 4. 쿼리와 스캔

### Query (효율적 조회)

```python
# PK 기반 조회 — 해당 파티션만 읽음
response = table.query(
    KeyConditionExpression=Key("PK").eq("USER#001")
)

# PK + SK 범위 조회
response = table.query(
    KeyConditionExpression=(
        Key("PK").eq("USER#001") &
        Key("SK").begins_with("ORDER#2026-03")  # 2026년 3월 주문만
    )
)

# 역순 정렬 + 최근 5건
response = table.query(
    KeyConditionExpression=Key("PK").eq("USER#001") & Key("SK").begins_with("ORDER#"),
    ScanIndexForward=False,  # SK 역순 (최신순)
    Limit=5
)

# 필터 표현식 (SK 조건 후 추가 필터링)
response = table.query(
    KeyConditionExpression=Key("PK").eq("USER#001") & Key("SK").begins_with("ORDER#"),
    FilterExpression=Attr("status").eq("completed") & Attr("amount").gte(10000),
    ProjectionExpression="SK, amount, #s",
    ExpressionAttributeNames={"#s": "status"}
)
# 주의: FilterExpression은 읽은 후 필터링 → RCU 절약 안 됨
```

### 페이지네이션

```python
def query_all_pages(table, pk, sk_prefix):
    """모든 페이지를 순회하며 결과 수집"""
    items = []
    kwargs = {
        "KeyConditionExpression": Key("PK").eq(pk) & Key("SK").begins_with(sk_prefix)
    }

    while True:
        response = table.query(**kwargs)
        items.extend(response["Items"])

        # LastEvaluatedKey가 없으면 마지막 페이지
        if "LastEvaluatedKey" not in response:
            break
        kwargs["ExclusiveStartKey"] = response["LastEvaluatedKey"]

    return items
```

### Scan (전체 테이블 읽기)

```python
# 전체 스캔 — 비용이 크므로 운영 환경에서 지양
response = table.scan(
    FilterExpression=Attr("age").gt(30)
)

# 병렬 스캔 (대량 데이터 마이그레이션 등)
import concurrent.futures

def parallel_scan(table, total_segments=4):
    items = []

    def scan_segment(segment):
        response = table.scan(
            TotalSegments=total_segments,
            Segment=segment
        )
        return response["Items"]

    with concurrent.futures.ThreadPoolExecutor() as executor:
        futures = [executor.submit(scan_segment, i) for i in range(total_segments)]
        for future in concurrent.futures.as_completed(futures):
            items.extend(future.result())

    return items
```

---

## 5. 보조 인덱스 (GSI / LSI)

### GSI (Global Secondary Index)

기본 테이블과 **다른 Partition Key**로 쿼리할 수 있는 인덱스.

```python
# 테이블 생성 시 GSI 정의
client.create_table(
    TableName="Users",
    KeySchema=[
        {"AttributeName": "PK", "KeyType": "HASH"},
        {"AttributeName": "SK", "KeyType": "RANGE"}
    ],
    AttributeDefinitions=[
        {"AttributeName": "PK", "AttributeType": "S"},
        {"AttributeName": "SK", "AttributeType": "S"},
        {"AttributeName": "GSI1PK", "AttributeType": "S"},
        {"AttributeName": "GSI1SK", "AttributeType": "S"}
    ],
    GlobalSecondaryIndexes=[
        {
            "IndexName": "GSI1",
            "KeySchema": [
                {"AttributeName": "GSI1PK", "KeyType": "HASH"},
                {"AttributeName": "GSI1SK", "KeyType": "RANGE"}
            ],
            "Projection": {"ProjectionType": "ALL"},  # 모든 속성 복제
            # KEYS_ONLY: 키만 | INCLUDE: 지정 속성만 | ALL: 전체
        }
    ],
    BillingMode="PAY_PER_REQUEST"
)
```

```python
# GSI로 쿼리 — 이메일로 사용자 조회
response = table.query(
    IndexName="GSI1",
    KeyConditionExpression=Key("GSI1PK").eq("EMAIL#dev@example.com")
)
```

### LSI (Local Secondary Index)

같은 Partition Key 내에서 **다른 Sort Key**로 쿼리. 테이블 생성 시에만 정의 가능.

```python
# LSI: 같은 PK, 다른 SK로 정렬
LocalSecondaryIndexes=[
    {
        "IndexName": "LSI1-byAmount",
        "KeySchema": [
            {"AttributeName": "PK", "KeyType": "HASH"},     # 기본 PK와 동일
            {"AttributeName": "amount", "KeyType": "RANGE"}  # 다른 SK
        ],
        "Projection": {"ProjectionType": "ALL"}
    }
]

# LSI 쿼리 — 주문을 금액순으로 조회
response = table.query(
    IndexName="LSI1-byAmount",
    KeyConditionExpression=Key("PK").eq("USER#001") & Key("amount").between(10000, 50000)
)
```

### GSI vs LSI 비교

| 특성 | GSI | LSI |
|------|-----|-----|
| Partition Key | 테이블과 **다름** | 테이블과 **같음** |
| Sort Key | 선택 | 필수 |
| 생성 시점 | 언제든 추가/삭제 가능 | **테이블 생성 시에만** |
| 일관성 읽기 | Eventually Consistent만 | Strong Consistent 가능 |
| 용량 제한 | 없음 | PK당 10GB |
| 별도 처리량 | 자체 RCU/WCU 필요 | 테이블 RCU/WCU 공유 |
| 최대 개수 | 20개 | 5개 |

---

## 6. 싱글 테이블 설계

### 왜 싱글 테이블인가?

DynamoDB는 **테이블당 JOIN이 없고**, 요청당 비용이 발생하므로, 관련 데이터를 하나의 테이블에 모아 **한 번의 Query로 가져오는 것**이 효율적이다.

```
# 멀티 테이블 (비효율) — 3번의 API 호출
GET /users/001        → Users 테이블 쿼리
GET /users/001/orders → Orders 테이블 쿼리
GET /users/001/addr   → Addresses 테이블 쿼리

# 싱글 테이블 (효율) — 1번의 API 호출
Query PK="USER#001"   → 프로필 + 주문 + 주소 전부 반환
```

### 실전 설계: 전자상거래

```
액세스 패턴:
1. 사용자 프로필 조회
2. 사용자의 주문 목록 (최신순)
3. 주문 상세 조회
4. 주문의 상품 목록
5. 특정 상태의 주문 조회 (GSI 필요)
6. 이메일로 사용자 검색 (GSI 필요)
```

```python
# 싱글 테이블 데이터 예시
items = [
    # === 사용자 ===
    {
        "PK": "USER#001",
        "SK": "PROFILE",
        "GSI1PK": "EMAIL#kim@example.com",
        "GSI1SK": "USER#001",
        "name": "김개발",
        "email": "kim@example.com",
        "type": "User"
    },

    # === 주문 ===
    {
        "PK": "USER#001",
        "SK": "ORDER#2026-03-14#ORD-A01",
        "GSI1PK": "ORDER#ORD-A01",
        "GSI1SK": "ORDER#2026-03-14",
        "GSI2PK": "STATUS#completed",
        "GSI2SK": "2026-03-14",
        "totalAmount": 85000,
        "status": "completed",
        "type": "Order"
    },
    {
        "PK": "USER#001",
        "SK": "ORDER#2026-03-13#ORD-B02",
        "GSI1PK": "ORDER#ORD-B02",
        "GSI1SK": "ORDER#2026-03-13",
        "GSI2PK": "STATUS#pending",
        "GSI2SK": "2026-03-13",
        "totalAmount": 32000,
        "status": "pending",
        "type": "Order"
    },

    # === 주문 상품 ===
    {
        "PK": "ORDER#ORD-A01",
        "SK": "ITEM#001",
        "productName": "키보드",
        "price": 55000,
        "quantity": 1,
        "type": "OrderItem"
    },
    {
        "PK": "ORDER#ORD-A01",
        "SK": "ITEM#002",
        "productName": "마우스",
        "price": 30000,
        "quantity": 1,
        "type": "OrderItem"
    },
]
```

### 액세스 패턴별 쿼리 구현

```python
# 패턴 1: 사용자 프로필 조회
def get_user_profile(user_id):
    return table.get_item(
        Key={"PK": f"USER#{user_id}", "SK": "PROFILE"}
    )["Item"]

# 패턴 2: 사용자의 주문 목록 (최신순, 최대 20건)
def get_user_orders(user_id, limit=20):
    return table.query(
        KeyConditionExpression=(
            Key("PK").eq(f"USER#{user_id}") &
            Key("SK").begins_with("ORDER#")
        ),
        ScanIndexForward=False,
        Limit=limit
    )["Items"]

# 패턴 3: 주문 상세 + 상품 목록 (1회 쿼리)
def get_order_detail(order_id):
    return table.query(
        KeyConditionExpression=Key("PK").eq(f"ORDER#{order_id}")
    )["Items"]
    # 결과: [주문 메타데이터, 상품1, 상품2, ...]

# 패턴 4: 특정 상태의 주문 조회 (GSI2 사용)
def get_orders_by_status(status, start_date, end_date):
    return table.query(
        IndexName="GSI2",
        KeyConditionExpression=(
            Key("GSI2PK").eq(f"STATUS#{status}") &
            Key("GSI2SK").between(start_date, end_date)
        )
    )["Items"]

# 패턴 5: 이메일로 사용자 검색 (GSI1 사용)
def get_user_by_email(email):
    result = table.query(
        IndexName="GSI1",
        KeyConditionExpression=Key("GSI1PK").eq(f"EMAIL#{email}")
    )["Items"]
    return result[0] if result else None
```

### 오버로딩 패턴 정리

```
┌──────────────────────────────────────────────────────────────┐
│  PK              │  SK                    │  GSI1PK          │
├──────────────────┼────────────────────────┼──────────────────┤
│  USER#001        │  PROFILE               │  EMAIL#kim@..    │
│  USER#001        │  ORDER#2026-03-14#A01  │  ORDER#ORD-A01   │
│  USER#001        │  ORDER#2026-03-13#B02  │  ORDER#ORD-B02   │
│  USER#001        │  ADDRESS#HOME          │  —               │
│  ORDER#ORD-A01   │  ITEM#001              │  —               │
│  ORDER#ORD-A01   │  ITEM#002              │  —               │
└──────────────────┴────────────────────────┴──────────────────┘
```

---

## 7. DynamoDB vs RDB 비교

### 핵심 차이점

| 항목 | RDB (MySQL/PostgreSQL) | DynamoDB |
|------|----------------------|----------|
| **스키마** | 고정 스키마 (DDL 필요) | 스키마리스 (아이템마다 다른 속성 가능) |
| **확장** | 수직 확장 (Scale Up) | 수평 확장 (자동 파티셔닝) |
| **JOIN** | 다중 테이블 JOIN 지원 | JOIN 없음 — 비정규화 또는 앱 레벨 조인 |
| **트랜잭션** | 복잡한 트랜잭션 지원 | 25개 아이템 이내 트랜잭션 |
| **쿼리 유연성** | 자유로운 ad-hoc 쿼리 | 사전 정의된 액세스 패턴에 최적화 |
| **일관성** | Strong Consistency 기본 | Eventually Consistent 기본 (Strong 선택 가능) |
| **과금** | 인스턴스 기반 (시간당) | 요청 기반 또는 프로비저닝 |
| **성능** | 데이터 증가 시 성능 저하 가능 | 규모와 무관하게 **일정한 지연 시간** (ms 단위) |
| **관리** | 패치, 백업, 복제 직접 관리 | 완전 관리형 (서버리스) |
| **인덱스** | 자유롭게 생성 | GSI 20개, LSI 5개 제한 |

### 언제 DynamoDB를 선택하는가?

```
DynamoDB가 적합한 경우:
✅ 밀리초 단위 응답 시간이 필요한 경우
✅ 트래픽이 예측 불가능하거나 급격히 변동하는 경우
✅ 액세스 패턴이 명확하고 제한적인 경우
✅ 수평 확장이 필요한 대규모 서비스
✅ 서버리스 아키텍처 (Lambda + API Gateway + DynamoDB)
✅ 세션 스토어, 장바구니, 게임 상태 등 키-값 중심 데이터

RDB가 적합한 경우:
✅ 복잡한 JOIN과 집계 쿼리가 빈번한 경우
✅ 쿼리 패턴이 자주 바뀌는 경우 (ad-hoc 분석)
✅ 강한 관계 무결성이 필요한 경우 (FK 제약 등)
✅ 복잡한 트랜잭션이 필요한 금융 시스템
✅ 데이터 규모가 크지 않고 단일 서버로 충분한 경우
```

### 같은 요구사항, 다른 구현

```sql
-- RDB: 특정 사용자의 최근 주문 5건 + 상품 상세
SELECT o.order_id, o.order_date, o.total_amount,
       oi.product_name, oi.price, oi.quantity
FROM orders o
JOIN order_items oi ON o.order_id = oi.order_id
WHERE o.user_id = 'user-001'
ORDER BY o.order_date DESC
LIMIT 5;
```

```python
# DynamoDB: 2번의 쿼리로 동일한 결과
# 1) 사용자의 최근 주문 5건
orders = table.query(
    KeyConditionExpression=(
        Key("PK").eq("USER#001") & Key("SK").begins_with("ORDER#")
    ),
    ScanIndexForward=False,
    Limit=5
)["Items"]

# 2) 각 주문의 상품 목록 (BatchGetItem으로 최적화 가능)
for order in orders:
    order_id = order["SK"].split("#")[2]
    items = table.query(
        KeyConditionExpression=Key("PK").eq(f"ORDER#{order_id}")
    )["Items"]
    order["items"] = items
```

---

## 8. 용량 모드와 과금

### On-Demand vs Provisioned

```python
# On-Demand 모드 — 트래픽 예측 불가 시
client.create_table(
    TableName="Events",
    BillingMode="PAY_PER_REQUEST",  # 사용한 만큼만 과금
    KeySchema=[
        {"AttributeName": "PK", "KeyType": "HASH"},
        {"AttributeName": "SK", "KeyType": "RANGE"}
    ],
    AttributeDefinitions=[
        {"AttributeName": "PK", "AttributeType": "S"},
        {"AttributeName": "SK", "AttributeType": "S"}
    ]
)

# Provisioned 모드 — 트래픽 예측 가능 시 (비용 절감)
client.create_table(
    TableName="Events",
    BillingMode="PROVISIONED",
    ProvisionedThroughput={
        "ReadCapacityUnits": 100,   # 초당 최대 100 읽기
        "WriteCapacityUnits": 50    # 초당 최대 50 쓰기
    },
    # ... KeySchema, AttributeDefinitions
)
```

### RCU / WCU 계산

```
RCU (Read Capacity Unit):
- Strong Consistent:    1 RCU = 4KB 아이템 1건/초
- Eventually Consistent: 1 RCU = 4KB 아이템 2건/초
- Transactional:        2 RCU = 4KB 아이템 1건/초

WCU (Write Capacity Unit):
- 표준 쓰기:   1 WCU = 1KB 아이템 1건/초
- Transactional: 2 WCU = 1KB 아이템 1건/초

예) 평균 2KB 아이템, 초당 500건 읽기 (Eventually Consistent)
→ 필요 RCU = ceil(2/4) * 500 / 2 = 250 RCU
```

---

## 9. DynamoDB Streams & TTL

### DynamoDB Streams

테이블 변경 사항을 실시간으로 캡처하는 변경 데이터 캡처(CDC) 기능.

```python
# Streams 활성화
client.update_table(
    TableName="Users",
    StreamSpecification={
        "StreamEnabled": True,
        "StreamViewType": "NEW_AND_OLD_IMAGES"
        # NEW_IMAGE | OLD_IMAGE | NEW_AND_OLD_IMAGES | KEYS_ONLY
    }
)

# Lambda 트리거로 변경 사항 처리
def lambda_handler(event, context):
    for record in event["Records"]:
        event_name = record["eventName"]  # INSERT | MODIFY | REMOVE
        new_image = record["dynamodb"].get("NewImage", {})
        old_image = record["dynamodb"].get("OldImage", {})

        if event_name == "INSERT":
            print(f"새 아이템: {new_image}")
        elif event_name == "MODIFY":
            print(f"변경: {old_image} → {new_image}")
        elif event_name == "REMOVE":
            print(f"삭제됨: {old_image}")
```

### TTL (Time To Live)

지정된 시간이 지나면 아이템을 자동 삭제.

```python
# TTL 활성화
client.update_time_to_live(
    TableName="Sessions",
    TimeToLiveSpecification={
        "Enabled": True,
        "AttributeName": "expiresAt"  # epoch 초 단위 타임스탬프
    }
)

# TTL이 있는 아이템 생성
import time

table.put_item(Item={
    "PK": "SESSION#abc123",
    "SK": "DATA",
    "userId": "USER#001",
    "expiresAt": int(time.time()) + 3600  # 1시간 후 자동 삭제
})
```

---

## 10. 실전 팁

### Hot Partition 방지

```python
# 나쁜 예: 모든 쓰기가 하나의 파티션에 집중
{"PK": "GLOBAL_COUNTER", "SK": "count", "value": 999999}

# 좋은 예: Write Sharding으로 분산
import random

shard = random.randint(0, 9)
table.update_item(
    Key={"PK": f"COUNTER#shard-{shard}", "SK": "count"},
    UpdateExpression="ADD #v :inc",
    ExpressionAttributeNames={"#v": "value"},
    ExpressionAttributeValues={":inc": 1}
)

# 전체 카운트 조회 시 모든 샤드 합산
def get_total_count():
    total = 0
    for shard in range(10):
        response = table.get_item(
            Key={"PK": f"COUNTER#shard-{shard}", "SK": "count"}
        )
        total += response.get("Item", {}).get("value", 0)
    return total
```

### 에러 처리와 재시도

```python
from botocore.exceptions import ClientError

def safe_put_item(table, item, max_retries=3):
    for attempt in range(max_retries):
        try:
            table.put_item(
                Item=item,
                ConditionExpression="attribute_not_exists(PK)"
            )
            return True
        except ClientError as e:
            code = e.response["Error"]["Code"]
            if code == "ConditionalCheckFailedException":
                print("아이템이 이미 존재합니다")
                return False
            elif code == "ProvisionedThroughputExceededException":
                wait = 2 ** attempt  # 지수 백오프
                print(f"처리량 초과. {wait}초 후 재시도...")
                time.sleep(wait)
            else:
                raise
    return False
```

### DynamoDB Local (로컬 개발)

```bash
# Docker로 DynamoDB Local 실행
docker run -d -p 8000:8000 amazon/dynamodb-local

# 로컬 DynamoDB에 연결
```

```python
dynamodb = boto3.resource(
    "dynamodb",
    endpoint_url="http://localhost:8000",
    region_name="ap-northeast-2",
    aws_access_key_id="dummy",
    aws_secret_access_key="dummy"
)

# 테이블 생성 및 테스트
table = dynamodb.create_table(
    TableName="TestTable",
    KeySchema=[
        {"AttributeName": "PK", "KeyType": "HASH"},
        {"AttributeName": "SK", "KeyType": "RANGE"}
    ],
    AttributeDefinitions=[
        {"AttributeName": "PK", "AttributeType": "S"},
        {"AttributeName": "SK", "AttributeType": "S"}
    ],
    BillingMode="PAY_PER_REQUEST"
)
table.wait_until_exists()
```

---

## 정리

| 개념 | 핵심 포인트 |
|------|------------|
| **키 설계** | PK로 분산, SK로 정렬 + 범위 쿼리. 카디널리티가 높은 PK 선택 |
| **액세스 패턴** | 쿼리 패턴을 먼저 정의하고 테이블을 설계한다 |
| **GSI/LSI** | 다른 키 조합으로 쿼리가 필요하면 GSI, 같은 PK에서 다른 정렬이면 LSI |
| **싱글 테이블** | 관련 엔티티를 한 테이블에 모아 1회 쿼리로 조회. PK/SK 오버로딩 |
| **vs RDB** | 액세스 패턴이 명확하면 DynamoDB, ad-hoc 쿼리가 필요하면 RDB |
| **용량 모드** | 트래픽 예측 가능 → Provisioned, 불확실 → On-Demand |
| **Streams** | 변경 이벤트 캡처 → Lambda 트리거로 실시간 처리 |
| **TTL** | epoch 초 기반 자동 삭제. 세션, 캐시 등에 활용 |

---

*마지막 업데이트: 2026년 03월*
