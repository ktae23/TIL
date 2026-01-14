# SiteMap 생성 시 동적 URL ID 조회 전략

동적으로 생성되는 페이지(예: `/building/{id}`, `/neighborhood/{id}`)의 URL을 SiteMap에 포함시킬 때, ID 목록을 어디서 조회하는 것이 효율적인지 비교 분석한다.

## 배경

- 지역별 건물/동네 상세 페이지가 수만~수십만 개 존재
- 빌드 시점에 SiteMap을 생성해야 함
- 검색 엔진 크롤링을 위해 모든 유효한 URL을 포함해야 함

## 조회 전략 비교

| 전략 | 설명 | 장점 | 단점 |
|------|------|------|------|
| **1. 빌드 시 API 호출** | 빌드 타임에 백엔드 API를 호출하여 ID 목록 조회 | 구현이 단순함, 기존 API 재사용 | 빌드 시간 증가, API 서버 부하, 타임아웃 위험 |
| **2. AWS Lambda + DB 직접 조회** | Lambda 함수로 DB에서 직접 ID 목록 조회 | API 서버 부하 없음, 필요한 데이터만 조회 | Lambda 추가 관리, DB 연결 설정 필요, Cold start |
| **3. 사전 생성 후 S3 주입** | 백엔드에서 주기적으로 SiteMap 생성 → S3 저장 → 빌드 시 주입 | 빌드 시간 최소화, 안정적 | 실시간성 떨어짐, 추가 배치 작업 필요 |
| **4. ISR/On-demand 생성** | 요청 시점에 동적으로 SiteMap 생성 (Next.js ISR 등) | 항상 최신 데이터, 빌드 부담 없음 | 캐시 전략 필요, 초기 응답 지연 가능 |

## 일반적인 방법

**빌드 시 API 호출 (전략 1)**이 가장 일반적이다.

```javascript
// next-sitemap.config.js 또는 빌드 스크립트
async function fetchBuildingIds() {
  const response = await fetch('https://api.example.com/buildings/ids');
  return response.json();
}
```

- 대부분의 정적 사이트 생성기(Next.js, Gatsby 등)에서 기본적으로 지원
- 추가 인프라 없이 구현 가능
- 소규모~중규모 사이트에 적합

## 잘 알려진 방법 (Best Practice)

**사전 생성 후 S3 주입 (전략 3)**이 대규모 서비스에서 널리 사용된다.

```
[백엔드 배치 작업]
    ↓ (새벽 스케줄링)
[SiteMap XML 생성]
    ↓
[S3 업로드]
    ↓ (빌드 시점)
[프론트엔드 빌드에서 S3 파일 다운로드]
    ↓
[정적 파일로 배포]
```

**장점:**
- 빌드 시간이 데이터 양에 영향받지 않음
- 백엔드 API 서버에 부하 없음
- 실패해도 이전 버전 SiteMap 유지 가능

**사용 사례:** 네이버, 카카오, 쿠팡 등 대형 서비스

## 효율적인 방법 추천

### 데이터 규모별 추천

| 규모 | ID 개수 | 추천 전략 |
|------|---------|-----------|
| 소규모 | ~1,000개 | 빌드 시 API 호출 |
| 중규모 | 1,000~10,000개 | Lambda + DB 또는 API 호출 (페이지네이션) |
| 대규모 | 10,000개 이상 | **S3 사전 생성 (강력 추천)** |

### 대규모 서비스 권장 아키텍처

```
┌─────────────────────────────────────────────────────┐
│                    추천 아키텍처                      │
├─────────────────────────────────────────────────────┤
│                                                     │
│  [EventBridge]  ──(매일 새벽 3시)──▶  [Lambda]      │
│                                           │         │
│                                           ▼         │
│                                      [RDS/DB]       │
│                                           │         │
│                                           ▼         │
│                                    [SiteMap 생성]   │
│                                           │         │
│                                           ▼         │
│                                      [S3 버킷]      │
│                                           │         │
│  [CI/CD 빌드] ◀────(다운로드)─────────────┘         │
│       │                                             │
│       ▼                                             │
│  [CloudFront/CDN 배포]                              │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### 구현 예시 (S3 사전 생성 방식)

**Lambda 함수 (Python)**
```python
import boto3
import pymysql
from datetime import datetime

def lambda_handler(event, context):
    # DB에서 ID 목록 조회
    connection = pymysql.connect(host='...', user='...', password='...', db='...')
    with connection.cursor() as cursor:
        cursor.execute("SELECT id FROM buildings WHERE is_active = 1")
        building_ids = [row[0] for row in cursor.fetchall()]

    # SiteMap XML 생성
    sitemap_xml = generate_sitemap(building_ids)

    # S3 업로드
    s3 = boto3.client('s3')
    s3.put_object(
        Bucket='my-sitemap-bucket',
        Key='sitemaps/buildings.xml',
        Body=sitemap_xml,
        ContentType='application/xml'
    )

    return {'statusCode': 200, 'body': f'Generated sitemap with {len(building_ids)} URLs'}
```

**빌드 스크립트**
```bash
# 빌드 전 S3에서 SiteMap 다운로드
aws s3 cp s3://my-sitemap-bucket/sitemaps/ ./public/sitemaps/ --recursive

# 빌드 실행
npm run build
```

## 결론

| 상황 | 추천 |
|------|------|
| 빠르게 구현하고 싶다 | 빌드 시 API 호출 |
| 대규모 + 안정성 중요 | **S3 사전 생성** ⭐ |
| 실시간성이 중요하다 | ISR/On-demand |
| 인프라 최소화하고 싶다 | API 호출 + 캐싱 |

**최종 추천:** 수만 개 이상의 동적 URL이 있다면 **S3 사전 생성 방식**이 가장 효율적이고 안정적이다. 빌드 시간을 예측 가능하게 유지하면서 백엔드 부하도 분산시킬 수 있다.

*마지막 업데이트: 2025년 01월*
