# MySQL 트러블슈팅 가이드

실무에서 자주 발생하는 MySQL 문제와 해결 방법을 정리합니다.

## 목차

1. [슬로우 쿼리 분석](#1-슬로우-쿼리-분석)
2. [데드락 분석 및 해결](#2-데드락-분석-및-해결)
3. [Connection Pool 고갈](#3-connection-pool-고갈)
4. [인덱스 문제 진단](#4-인덱스-문제-진단)
5. [Replication Lag 대응](#5-replication-lag-대응)
6. [운영 모니터링](#6-운영-모니터링)

---

## 1. 슬로우 쿼리 분석

### 슬로우 쿼리 로그 설정

```sql
-- 현재 설정 확인
SHOW VARIABLES LIKE 'slow_query%';
SHOW VARIABLES LIKE 'long_query_time';

-- 슬로우 쿼리 로그 활성화
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;  -- 1초 이상
SET GLOBAL slow_query_log_file = '/var/log/mysql/slow.log';

-- 인덱스 미사용 쿼리도 기록
SET GLOBAL log_queries_not_using_indexes = 'ON';
```

### 슬로우 쿼리 분석 도구

```bash
# mysqldumpslow 사용
mysqldumpslow -s t -t 10 /var/log/mysql/slow.log
# -s t: 총 실행시간 기준 정렬
# -t 10: 상위 10개

# pt-query-digest (Percona Toolkit)
pt-query-digest /var/log/mysql/slow.log > report.txt
```

### EXPLAIN 분석

```sql
EXPLAIN SELECT * FROM orders o
JOIN order_items oi ON o.id = oi.order_id
WHERE o.user_id = 100 AND o.status = 'COMPLETED';

-- EXPLAIN 결과 해석
┌────┬────────────┬───────┬──────┬─────────┬────────┬─────────┐
│ id │ select_type│ table │ type │ key     │ rows   │ Extra   │
├────┼────────────┼───────┼──────┼─────────┼────────┼─────────┤
│ 1  │ SIMPLE     │ o     │ ref  │ idx_user│ 50     │ Using   │
│    │            │       │      │         │        │ where   │
│ 1  │ SIMPLE     │ oi    │ ref  │ idx_ord │ 3      │         │
└────┴────────────┴───────┴──────┴─────────┴────────┴─────────┘
```

**type 컬럼 성능 순서 (좋음 → 나쁨)**
```
system > const > eq_ref > ref > range > index > ALL

- const: PK/Unique 조회 (1건)
- eq_ref: JOIN에서 PK/Unique 사용
- ref: 인덱스 동등 조건
- range: 인덱스 범위 스캔
- index: 인덱스 풀 스캔
- ALL: 테이블 풀 스캔 (개선 필요!)
```

**Extra 컬럼 주의 사항**
```
- Using filesort: 정렬에 인덱스 미사용 (개선 고려)
- Using temporary: 임시 테이블 사용 (메모리 주의)
- Using where: WHERE 조건 필터링
- Using index: 커버링 인덱스 (좋음!)
```

### 쿼리 최적화 예시

```sql
-- 문제: 풀 테이블 스캔
SELECT * FROM orders WHERE DATE(created_at) = '2024-01-15';
-- DATE() 함수로 인덱스 사용 불가

-- 해결: 범위 조건으로 변경
SELECT * FROM orders
WHERE created_at >= '2024-01-15 00:00:00'
  AND created_at < '2024-01-16 00:00:00';

-- 문제: 불필요한 정렬
SELECT * FROM products ORDER BY price DESC LIMIT 10;
-- price에 인덱스 없으면 filesort

-- 해결: 인덱스 추가
CREATE INDEX idx_price ON products(price DESC);
```

---

## 2. 데드락 분석 및 해결

### 데드락 정보 확인

```sql
-- 최근 데드락 정보
SHOW ENGINE INNODB STATUS\G

-- 데드락 로그 자동 기록
SET GLOBAL innodb_print_all_deadlocks = ON;
```

### 데드락 로그 분석

```
------------------------
LATEST DETECTED DEADLOCK
------------------------
*** (1) TRANSACTION:
TRANSACTION 12345, ACTIVE 0 sec starting index read
mysql tables in use 1, locked 1
LOCK WAIT 3 lock struct(s), heap size 1136, 2 row lock(s)
MySQL thread id 100, OS thread handle 12345
*** (1) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 100 page no 10 n bits 80 index PRIMARY
  of table `shop`.`orders` trx id 12345 lock_mode X waiting

*** (2) TRANSACTION:
TRANSACTION 12346, ACTIVE 0 sec starting index read
*** (2) HOLDS THE LOCK(S):
RECORD LOCKS space id 100 page no 10 n bits 80 index PRIMARY
  of table `shop`.`orders` trx id 12346 lock_mode X
*** (2) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 101 page no 5 n bits 72 index PRIMARY
  of table `shop`.`products` trx id 12346 lock_mode X waiting

*** WE ROLL BACK TRANSACTION (1)
```

### 데드락 해결 전략

```java
// 1. 일관된 락 순서
// Bad: 트랜잭션마다 다른 순서
// TX1: orders → products
// TX2: products → orders

// Good: 항상 같은 순서 (작은 ID부터)
public void transfer(Long fromId, Long toId) {
    Long first = Math.min(fromId, toId);
    Long second = Math.max(fromId, toId);

    Account acc1 = accountRepository.findByIdForUpdate(first);
    Account acc2 = accountRepository.findByIdForUpdate(second);
    // ...
}

// 2. 짧은 트랜잭션
@Transactional
public void processOrder(OrderRequest request) {
    // 긴 외부 API 호출은 트랜잭션 밖에서!
    PaymentResult result = paymentService.process(request);  // 이건 밖으로

    // DB 작업만 트랜잭션 안에서
    orderRepository.save(order);
}

// 3. 인덱스 활용 (락 범위 최소화)
// Bad: 인덱스 없으면 테이블 전체에 락
UPDATE orders SET status = 'SHIPPED' WHERE user_id = 100;

// Good: user_id 인덱스 추가
CREATE INDEX idx_user ON orders(user_id);

// 4. 재시도 로직
@Retryable(
    value = DeadlockLoserDataAccessException.class,
    maxAttempts = 3,
    backoff = @Backoff(delay = 100, multiplier = 2)
)
public void updateWithRetry() {
    // 데드락 발생 시 재시도
}
```

---

## 3. Connection Pool 고갈

### 증상 및 진단

```sql
-- 현재 연결 상태
SHOW STATUS LIKE 'Threads_connected';
SHOW STATUS LIKE 'Max_used_connections';
SHOW PROCESSLIST;

-- 연결 대기 상태 확인
SHOW STATUS LIKE 'Threads_running';
```

```java
// HikariCP 메트릭 모니터링
// application.yml
spring:
  datasource:
    hikari:
      pool-name: HikariPool
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000  # 30초
      leak-detection-threshold: 60000  # 60초 (leak 감지)
```

### Connection Leak 감지

```java
// 로그에서 Connection Leak 확인
// "Connection leak detection triggered"

// 문제 코드: 커넥션 미반환
public void badMethod() {
    Connection conn = dataSource.getConnection();
    // return 하지 않음 -> 커넥션 누수!
}

// 해결: try-with-resources 사용
public void goodMethod() {
    try (Connection conn = dataSource.getConnection()) {
        // 사용 후 자동 반환
    }
}

// JPA 사용 시 주의: @Transactional 없이 lazy loading
@Entity
public class Order {
    @OneToMany(fetch = FetchType.LAZY)
    private List<OrderItem> items;
}

// 문제: 트랜잭션 밖에서 lazy loading 시도
public void problem() {
    Order order = orderRepository.findById(1L);
    // 여기서 새 커넥션 획득 시도
    order.getItems().size();  // LazyInitializationException 또는 커넥션 점유
}
```

### Connection Pool 튜닝

```yaml
# HikariCP 권장 설정
spring:
  datasource:
    hikari:
      # 기본 설정
      maximum-pool-size: 20  # CPU 코어 * 2 + effective_spindle_count
      minimum-idle: 10
      idle-timeout: 600000   # 10분
      max-lifetime: 1800000  # 30분
      connection-timeout: 30000  # 30초

      # 검증 쿼리
      connection-test-query: SELECT 1

      # 누수 감지
      leak-detection-threshold: 60000
```

```
Pool Size 공식 (PostgreSQL 기준, MySQL도 유사):
connections = ((core_count * 2) + effective_spindle_count)

예시: 4코어 서버, SSD
connections = (4 * 2) + 1 = 9~10

주의: 너무 많은 커넥션은 오히려 성능 저하
- Context Switching 비용
- 메모리 사용량 증가
```

---

## 4. 인덱스 문제 진단

### 인덱스 사용 여부 확인

```sql
-- 테이블 인덱스 정보
SHOW INDEX FROM orders;

-- 인덱스 사용 통계
SELECT * FROM sys.schema_index_statistics
WHERE table_name = 'orders';

-- 미사용 인덱스 찾기
SELECT * FROM sys.schema_unused_indexes;
```

### 인덱스가 사용되지 않는 경우

```sql
-- 1. 함수/연산 사용
-- Bad
SELECT * FROM users WHERE YEAR(created_at) = 2024;
-- Good
SELECT * FROM users
WHERE created_at >= '2024-01-01' AND created_at < '2025-01-01';

-- 2. 암묵적 타입 변환
-- user_id가 VARCHAR인 경우
-- Bad
SELECT * FROM users WHERE user_id = 12345;  -- 숫자로 비교
-- Good
SELECT * FROM users WHERE user_id = '12345';

-- 3. LIKE '%패턴'
-- Bad (인덱스 사용 불가)
SELECT * FROM products WHERE name LIKE '%phone%';
-- 대안: Full-text Search 또는 Elasticsearch

-- 4. OR 조건
-- Bad
SELECT * FROM orders WHERE status = 'PENDING' OR user_id = 100;
-- Good (UNION으로 분리)
SELECT * FROM orders WHERE status = 'PENDING'
UNION
SELECT * FROM orders WHERE user_id = 100;

-- 5. NOT 조건
-- Bad (인덱스 활용 어려움)
SELECT * FROM orders WHERE status != 'COMPLETED';
-- Good (IN 사용)
SELECT * FROM orders WHERE status IN ('PENDING', 'PROCESSING');
```

### 복합 인덱스 설계

```sql
-- 쿼리 패턴 분석 후 인덱스 설계
-- 쿼리: WHERE status = ? AND user_id = ? ORDER BY created_at DESC

-- 방법 1: 등호 조건 + 정렬
CREATE INDEX idx_status_user_created
ON orders(status, user_id, created_at DESC);

-- 카디널리티 확인
SELECT
    COUNT(DISTINCT status) as status_card,
    COUNT(DISTINCT user_id) as user_card
FROM orders;
-- user_id가 더 높으면 순서 변경 고려
```

---

## 5. Replication Lag 대응

### Lag 모니터링

```sql
-- Slave에서 실행
SHOW SLAVE STATUS\G

-- 주요 지표
-- Seconds_Behind_Master: 지연 시간 (초)
-- Relay_Log_Space: 릴레이 로그 크기
-- Slave_IO_Running: I/O 스레드 상태
-- Slave_SQL_Running: SQL 스레드 상태
```

### 읽기 일관성 보장

```java
// 1. 쓰기 후 읽기는 Master에서
@Service
public class OrderService {

    @Transactional
    public Order createOrder(OrderRequest request) {
        Order order = orderRepository.save(new Order(request));
        // 방금 생성한 주문은 Master에서 읽기
        return orderRepository.findById(order.getId()).orElseThrow();
    }
}

// 2. 중요한 조회는 Master로 라우팅
@Transactional(readOnly = false)  // readOnly=false면 Master 사용
public Order getOrderForUpdate(Long orderId) {
    return orderRepository.findById(orderId).orElseThrow();
}

// 3. 지연 허용 가능한 조회만 Slave
@Transactional(readOnly = true)  // Slave로 라우팅
public List<Order> getOrderHistory(Long userId) {
    return orderRepository.findByUserId(userId);
}
```

### Spring Data JPA 읽기/쓰기 분리

```java
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource routingDataSource(
            @Qualifier("masterDataSource") DataSource master,
            @Qualifier("slaveDataSource") DataSource slave) {

        ReplicationRoutingDataSource routingDataSource =
            new ReplicationRoutingDataSource();

        Map<Object, Object> dataSources = new HashMap<>();
        dataSources.put("master", master);
        dataSources.put("slave", slave);

        routingDataSource.setTargetDataSources(dataSources);
        routingDataSource.setDefaultTargetDataSource(master);

        return routingDataSource;
    }
}

public class ReplicationRoutingDataSource
        extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return TransactionSynchronizationManager
            .isCurrentTransactionReadOnly() ? "slave" : "master";
    }
}
```

---

## 6. 운영 모니터링

### 핵심 메트릭

```sql
-- QPS (Queries Per Second)
SHOW GLOBAL STATUS LIKE 'Queries';
-- 1초 후 다시 조회하여 차이 계산

-- Buffer Pool 히트율 (99% 이상 권장)
SHOW GLOBAL STATUS LIKE 'Innodb_buffer_pool_read%';
-- 히트율 = 1 - (Innodb_buffer_pool_reads / Innodb_buffer_pool_read_requests)

-- 테이블 락 경합
SHOW GLOBAL STATUS LIKE 'Table_locks%';
-- Table_locks_waited가 높으면 문제

-- 슬로우 쿼리 수
SHOW GLOBAL STATUS LIKE 'Slow_queries';
```

### Prometheus + Grafana 모니터링

```yaml
# mysql_exporter 설정
# docker-compose.yml
services:
  mysql-exporter:
    image: prom/mysqld-exporter
    environment:
      DATA_SOURCE_NAME: "exporter:password@(mysql:3306)/"
    ports:
      - "9104:9104"
```

```sql
-- Exporter용 사용자 생성
CREATE USER 'exporter'@'%' IDENTIFIED BY 'password';
GRANT PROCESS, REPLICATION CLIENT ON *.* TO 'exporter'@'%';
GRANT SELECT ON performance_schema.* TO 'exporter'@'%';
```

### 알림 설정 예시

```yaml
# Prometheus alerting rules
groups:
- name: mysql
  rules:
  - alert: MySQLDown
    expr: mysql_up == 0
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "MySQL instance down"

  - alert: MySQLSlowQueries
    expr: rate(mysql_global_status_slow_queries[5m]) > 0.1
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "High slow query rate"

  - alert: MySQLConnectionsHigh
    expr: mysql_global_status_threads_connected / mysql_global_variables_max_connections > 0.8
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "MySQL connections > 80%"
```

---

## 체크리스트

### 슬로우 쿼리 발생 시

```
□ slow query log 확인
□ EXPLAIN 분석
□ 인덱스 확인 및 추가
□ 쿼리 리팩토링
□ 페이지네이션/캐싱 검토
```

### 데드락 발생 시

```
□ SHOW ENGINE INNODB STATUS 확인
□ 트랜잭션 순서 분석
□ 락 범위 확인 (인덱스 유무)
□ 트랜잭션 크기 축소
□ 재시도 로직 추가
```

### Connection 부족 시

```
□ 현재 연결 수 확인
□ Pool 설정 검토
□ Connection Leak 확인
□ 슬로우 쿼리로 인한 점유 확인
□ 트랜잭션 범위 확인
```

---

*마지막 업데이트: 2025년 01월*
