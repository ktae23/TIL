# 매핑 설계와 최적화

Elasticsearch에서 매핑(Mapping)은 문서의 필드가 어떻게 저장되고 인덱싱되는지를 정의하는 스키마다. 올바른 매핑 설계는 검색 성능, 저장 효율, 집계 정확성에 직접적인 영향을 미친다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### Dynamic Mapping vs Explicit Mapping

Elasticsearch는 두 가지 매핑 방식을 제공한다.

| 구분 | Dynamic Mapping | Explicit Mapping |
|------|----------------|-----------------|
| 정의 | 문서 인덱싱 시 자동으로 필드 타입 추론 | 인덱스 생성 시 명시적으로 필드 타입 지정 |
| 장점 | 빠른 프로토타이핑, 설정 불필요 | 정확한 타입 제어, 최적화 가능 |
| 단점 | 타입 오추론, 불필요한 필드 생성 | 사전 스키마 설계 필요 |
| 적합 환경 | 개발/탐색 단계 | 프로덕션 환경 |

**Dynamic Mapping의 함정**: 숫자 문자열 `"12345"`가 `text` + `keyword`로 매핑되어 숫자 범위 쿼리가 불가능해지는 경우가 흔하다.

### 필드 타입 분류

```
Field Types
├── Core Types
│   ├── text          (전문 검색용, 분석기 적용)
│   ├── keyword       (정확한 값 매칭, 집계/정렬용)
│   ├── long/integer  (정수형)
│   ├── double/float  (부동소수점)
│   ├── boolean
│   └── date
├── Complex Types
│   ├── object        (JSON 객체, 평탄화 저장)
│   ├── nested        (독립 Lucene 문서로 저장)
│   └── flattened     (전체를 keyword로 저장)
├── Specialized Types
│   ├── geo_point / geo_shape
│   ├── ip
│   ├── completion    (자동완성용)
│   └── dense_vector  (벡터 검색용)
└── Meta Types
    ├── _source
    ├── _routing
    └── _meta
```

---

## 2. 왜 알아야 하는가 (Why)

### 매핑 실수의 실무 비용

1. **저장 공간 폭발**: Dynamic Mapping으로 모든 문자열이 `text` + `keyword` 이중 매핑되면, 저장 공간이 2-3배 증가한다.
2. **검색 성능 저하**: `text` 필드에 대해 집계를 수행하면 `fielddata`를 활성화해야 하고, 이는 힙 메모리를 대량 소비한다.
3. **Mapping Explosion**: 로그 데이터에서 동적 키가 무한히 생성되면 매핑 필드 수가 수천 개로 폭증하여 클러스터 불안정을 초래한다.
4. **되돌릴 수 없는 변경**: 기존 필드의 타입은 변경 불가. Reindex가 유일한 방법이며, 대규모 인덱스에서는 수 시간이 소요된다.

### 실제 장애 사례

```
시나리오: 주문 데이터에서 order_id를 Dynamic Mapping으로 인덱싱
→ "ORD-20240101-001"이 text+keyword로 매핑
→ 이후 숫자형 order_id (10001)가 유입
→ 타입 충돌로 해당 문서 인덱싱 실패
→ 데이터 유실 발생
```

---

## 3. 내부 구현 분석 (How)

### 3.1 text vs keyword 선택 전략

```mermaid
flowchart TD
    A[문자열 필드] --> B{전문 검색이 필요한가?}
    B -->|Yes| C{정렬/집계도 필요한가?}
    B -->|No| D{정확한 값 매칭만 필요한가?}
    C -->|Yes| E[text + keyword multi-field]
    C -->|No| F[text only]
    D -->|Yes| G[keyword only]
    D -->|No| H{구조화된 값인가?<br/>IP, 날짜, 숫자 등}
    H -->|Yes| I[해당 전용 타입 사용]
    H -->|No| G
```

**핵심 원칙**: 
- 사람이 읽는 자연어 텍스트 → `text`
- 시스템이 생성한 식별자/코드 → `keyword`
- 둘 다 필요하면 → multi-field

### 3.2 Nested vs Object 타입 비교

**Object 타입의 평탄화 문제**:

```json
// 원본 문서
{
  "order": {
    "items": [
      { "name": "notebook", "price": 15000 },
      { "name": "pen", "price": 3000 }
    ]
  }
}

// Elasticsearch 내부 저장 (Object 타입)
{
  "order.items.name": ["notebook", "pen"],
  "order.items.price": [15000, 3000]
}
```

Object 타입은 내부 배열의 필드 간 연관 관계가 사라진다. `name=pen AND price=15000`으로 검색하면 매칭되어 버리는 오류가 발생한다.

**Nested 타입의 내부 구조**:

```mermaid
graph TB
    subgraph "Lucene 세그먼트"
        A["Parent Doc (order)"]
        B["Nested Doc 0<br/>name=notebook, price=15000"]
        C["Nested Doc 1<br/>name=pen, price=3000"]
    end
    A --> B
    A --> C
    
    style B fill:#e1f5fe
    style C fill:#e1f5fe
```

Nested 문서는 별도의 Lucene 문서로 저장되므로 필드 간 연관 관계가 유지된다. 하지만 다음 비용이 발생한다:

| 항목 | Object | Nested |
|------|--------|--------|
| Lucene 문서 수 | 1 | 1 + N (배열 요소 수) |
| 쿼리 복잡도 | 일반 쿼리 | nested 쿼리 필수 |
| 인덱싱 속도 | 빠름 | 느림 (별도 문서 생성) |
| 업데이트 비용 | 낮음 | 높음 (전체 재인덱싱) |

**선택 기준**:
- 배열 내 객체 간 필드 연관 쿼리가 필요 → `nested`
- 단순 필터링/집계만 필요 → `object`
- 배열 크기가 수백 개 이상 → `flattened` 또는 별도 인덱스 고려

### 3.3 Multi-field 매핑 패턴

하나의 소스 필드를 여러 방식으로 인덱싱하는 패턴이다.

```json
{
  "mappings": {
    "properties": {
      "product_name": {
        "type": "text",
        "analyzer": "standard",
        "fields": {
          "keyword": {
            "type": "keyword",
            "ignore_above": 256
          },
          "autocomplete": {
            "type": "text",
            "analyzer": "edge_ngram_analyzer"
          },
          "korean": {
            "type": "text",
            "analyzer": "nori_analyzer"
          }
        }
      }
    }
  }
}
```

**활용 패턴**:
- `product_name` → 일반 전문 검색
- `product_name.keyword` → 정렬, 집계, 정확한 매칭
- `product_name.autocomplete` → 자동완성
- `product_name.korean` → 한국어 형태소 분석 검색

### 3.4 Dynamic Template을 활용한 매핑 제어

Dynamic Mapping을 완전히 끄지 않으면서도 제어하는 방법이다.

```json
{
  "mappings": {
    "dynamic": "strict",
    "dynamic_templates": [
      {
        "strings_as_keywords": {
          "match_mapping_type": "string",
          "match": "*_id",
          "mapping": {
            "type": "keyword"
          }
        }
      },
      {
        "strings_as_text": {
          "match_mapping_type": "string",
          "match": "*_desc",
          "mapping": {
            "type": "text",
            "analyzer": "standard"
          }
        }
      },
      {
        "longs_as_integers": {
          "match_mapping_type": "long",
          "mapping": {
            "type": "integer"
          }
        }
      }
    ],
    "properties": {
      // 명시적 매핑은 여기에 정의
    }
  }
}
```

---

## 4. 실전 예제

### 예제 1: E-commerce 상품 인덱스 매핑 설계

```json
PUT /products
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1,
    "analysis": {
      "analyzer": {
        "korean_analyzer": {
          "type": "custom",
          "tokenizer": "nori_tokenizer",
          "filter": ["lowercase", "nori_part_of_speech"]
        },
        "edge_ngram_analyzer": {
          "type": "custom",
          "tokenizer": "edge_ngram_tokenizer",
          "filter": ["lowercase"]
        }
      },
      "tokenizer": {
        "edge_ngram_tokenizer": {
          "type": "edge_ngram",
          "min_gram": 2,
          "max_gram": 20,
          "token_chars": ["letter", "digit"]
        }
      }
    }
  },
  "mappings": {
    "dynamic": "strict",
    "properties": {
      "product_id": { "type": "keyword" },
      "name": {
        "type": "text",
        "analyzer": "korean_analyzer",
        "fields": {
          "keyword": { "type": "keyword", "ignore_above": 256 },
          "autocomplete": { "type": "text", "analyzer": "edge_ngram_analyzer" }
        }
      },
      "description": {
        "type": "text",
        "analyzer": "korean_analyzer"
      },
      "category": {
        "type": "keyword"
      },
      "price": { "type": "integer" },
      "discount_rate": { "type": "scaled_float", "scaling_factor": 100 },
      "brand": {
        "type": "text",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "tags": { "type": "keyword" },
      "attributes": {
        "type": "nested",
        "properties": {
          "key": { "type": "keyword" },
          "value": { "type": "keyword" }
        }
      },
      "reviews": {
        "type": "nested",
        "properties": {
          "user_id": { "type": "keyword" },
          "rating": { "type": "byte" },
          "comment": { "type": "text", "analyzer": "korean_analyzer" },
          "created_at": { "type": "date" }
        }
      },
      "location": { "type": "geo_point" },
      "created_at": { "type": "date" },
      "updated_at": { "type": "date" },
      "is_active": { "type": "boolean" }
    }
  }
}
```

**설계 근거**:
- `product_id`: 정확한 값 조회 전용 → `keyword`
- `name`: 검색 + 정렬 + 자동완성 → multi-field
- `price`: 정수로 충분 → `integer` (float 사용 시 범위 쿼리 느림)
- `discount_rate`: 소수점 필요하지만 정밀도 고정 → `scaled_float`
- `attributes`: 키-값 쌍의 연관 관계 유지 필요 → `nested`

### 예제 2: 매핑 변경 전략 — Reindex with Alias

기존 인덱스의 매핑을 변경해야 할 때 무중단으로 처리하는 패턴이다.

```mermaid
sequenceDiagram
    participant App as Application
    participant A as Alias: products
    participant V1 as products_v1
    participant V2 as products_v2

    App->>A: 검색/인덱싱
    A->>V1: (현재 연결)
    
    Note over V2: 1. 새 매핑으로 인덱스 생성
    Note over V1,V2: 2. Reindex API로 데이터 복사
    V1-->>V2: _reindex
    
    Note over A: 3. Alias 전환 (atomic)
    A->>V2: (새로 연결)
    
    Note over V1: 4. 구 인덱스 삭제
```

```json
// Step 1: 새 매핑으로 인덱스 생성
PUT /products_v2
{
  "mappings": {
    "properties": {
      "price": { "type": "long" }
    }
  }
}

// Step 2: Reindex
POST /_reindex
{
  "source": { "index": "products_v1" },
  "dest": { "index": "products_v2" }
}

// Step 3: Alias 원자적 전환
POST /_aliases
{
  "actions": [
    { "remove": { "index": "products_v1", "alias": "products" } },
    { "add": { "index": "products_v2", "alias": "products" } }
  ]
}

// Step 4: 확인 후 구 인덱스 삭제
DELETE /products_v1
```

### 예제 3: Mapping Explosion 방지

```json
PUT /logs
{
  "settings": {
    "index.mapping.total_fields.limit": 500,
    "index.mapping.depth.limit": 5,
    "index.mapping.nested_fields.limit": 25,
    "index.mapping.nested_objects.limit": 10000
  },
  "mappings": {
    "dynamic": "strict",
    "properties": {
      "timestamp": { "type": "date" },
      "level": { "type": "keyword" },
      "message": { "type": "text" },
      "metadata": {
        "type": "flattened"
      }
    }
  }
}
```

`metadata` 필드에 `flattened` 타입을 사용하면, 내부의 모든 키-값 쌍이 단일 필드로 취급되어 매핑 필드 수가 폭증하지 않는다. 단, 전문 검색과 범위 쿼리는 불가하고 정확한 값 매칭만 가능하다.

---

## 5. 정리

| 항목 | 핵심 내용 |
|------|----------|
| **Dynamic vs Explicit** | 프로덕션은 반드시 `"dynamic": "strict"` 사용 |
| **text vs keyword** | 전문 검색 → text, 집계/정렬/정확 매칭 → keyword |
| **Nested vs Object** | 배열 내 필드 연관 쿼리 필요 → nested, 아니면 object |
| **Multi-field** | 하나의 필드를 검색/정렬/자동완성 등 다목적으로 활용 |
| **매핑 변경** | Alias + Reindex로 무중단 전환 |
| **Mapping Explosion 방지** | `total_fields.limit` 설정 + `flattened` 타입 활용 |
| **숫자형 필드** | 범위 쿼리 빈번 → numeric, 정확 매칭만 → keyword |
| **날짜 필드** | 반드시 `date` 타입으로 명시 (문자열 추론 방지) |

---
*참고: Elasticsearch 8.x 기준*
