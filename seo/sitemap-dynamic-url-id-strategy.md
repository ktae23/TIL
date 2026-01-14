# SiteMap 생성 시 동적 URL ID 조회 전략

동적으로 생성되는 페이지(예: `/building/{id}`, `/neighborhood/{id}`)의 URL을 SiteMap에 포함시킬 때, ID 목록을 어디서 조회하는 것이 효율적인지 비교 분석한다.

## 배경

- 지역별 건물/동네 상세 페이지가 수만~수십만 개 존재
- 빌드 시점에 SiteMap을 생성해야 함
- 검색 엔진 크롤링을 위해 모든 유효한 URL을 포함해야 함

## 조회 전략 비교

| 전략 | 설명 | 장점 | 단점 |
|------|------|------|------|
| **1. 빌드 시 백엔드 API 호출** | 빌드 타임에 백엔드 API를 호출하여 ID 목록 조회 | 구현이 단순함, 기존 API 재사용 | 빌드 시간 증가, API 서버 부하, 타임아웃 위험 |
| **2. API Gateway + Lambda** | API Gateway + Lambda로 ID 목록 전용 엔드포인트 구성 | 백엔드 서버 부하 분리, 독립적 스케일링 | 추가 인프라 비용, Cold start 지연 |
| **3. 사전 생성 후 S3 주입** | 배치로 SiteMap 생성 → S3 저장 → 빌드 시 주입 | 빌드 시간 최소화, 안정적 | 실시간성 떨어짐, 추가 배치 작업 필요 |
| **4. ISR/On-demand 생성** | 요청 시점에 동적으로 SiteMap 생성 (Next.js ISR 등) | 항상 최신 데이터, 빌드 부담 없음 | 캐시 전략 필요, 초기 응답 지연 가능 |
| **5. Redis/ElastiCache 캐싱** | ID 목록을 Redis에 캐싱, 빌드 시 캐시 조회 | 빠른 응답, DB 부하 감소 | 캐시 동기화 로직 필요, 추가 인프라 |
| **6. CDN Edge 동적 생성** | Cloudflare Workers, Vercel Edge에서 생성 | 글로벌 저지연, 서버리스 | Edge 런타임 제약, 복잡한 로직 어려움 |

## 일반적인 방법

**빌드 시 API 호출 (전략 1)**이 가장 일반적으로 사용된다.

대부분의 서비스가 소~중규모(ID 수천 개 이하)이고, 이 정도 규모에서는 빌드 시 API를 호출해도 시간/부하 문제가 크지 않기 때문이다. 또한 별도 인프라 구축 없이 기존 백엔드 API를 그대로 재사용할 수 있어 구현이 가장 단순하다.

```javascript
// next-sitemap.config.js 또는 빌드 스크립트
async function fetchBuildingIds() {
  const response = await fetch('https://api.example.com/buildings/ids');
  return response.json();
}
```

**왜 일반적인가?**
- 대부분의 서비스가 소~중규모이므로 복잡한 아키텍처가 불필요
- Next.js, Gatsby 등 SSG 프레임워크에서 기본 패턴으로 지원
- 추가 인프라(Lambda, S3 등) 없이 즉시 구현 가능
- 기존 백엔드 API 엔드포인트 재사용

**한계점:** ID가 수만 개 이상이거나, 빌드 빈도가 높아 API 서버 부하가 문제되면 다른 전략 검토 필요

## 안정적인 방법 (전략 3)

**사전 생성 후 S3 주입** 방식은 빌드 시간을 데이터 양과 무관하게 일정하게 유지할 수 있어, 빌드 안정성이 중요한 경우에 적합하다.

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

**적합한 경우:** 수만 개 이상의 동적 URL을 가진 대형 서비스, 빌드 시간을 예측 가능하게 유지해야 하는 경우

## 효율적인 방법 추천

### 상황별 추천

| 상황 | 판단 기준 | 추천 전략 |
|------|-----------|-----------|
| 문제 없음 | 빌드 시간 수 초, API 응답 빠름 | 빌드 시 백엔드 API 호출 |
| 백엔드 부하 우려 | 빌드 빈도 높거나 API 서버 리소스 공유 | API Gateway + Lambda |
| 빌드 시간 길어짐 | API 응답 수 초 이상, 빌드 지연 체감 | Redis 캐싱 또는 S3 사전 생성 |
| 타임아웃 발생 | CI/CD 타임아웃, API 504 에러 | **S3 사전 생성 (필수)** |
| 실시간 반영 필요 | 콘텐츠 추가/삭제가 빈번, 즉시 크롤링 필요 | API Gateway + Lambda 또는 ISR |
| 글로벌 서비스 | 다국가 크롤링 대응 필요 | CDN Edge + S3 조합 |

> **참고:** Google sitemap 권장 사항은 파일당 최대 50,000개 URL이다. 이를 초과하면 sitemap index로 분할 필요.

### lastmod 값 설정 기준

| 전략 | lastmod 처리 | 장점 | 주의점 |
|------|-------------|------|--------|
| API Gateway + Lambda | 빌드 시점에 DB의 `updated_at` 조회 가능 | 실제 수정일 반영, 정확한 크롤링 유도 | 매 빌드마다 DB 조회 필요 |
| S3 사전 생성 | 배치 시점에 DB `updated_at` 포함하여 저장 | 빌드 시 추가 조회 불필요 | 배치 주기만큼 지연 발생 |
| 고정값 사용 | 모든 URL에 동일한 날짜 설정 | 구현 단순 | 크롤러가 불필요하게 재방문할 수 있음 |

**권장:** `lastmod`는 실제 콘텐츠 수정일을 반영해야 SEO에 유리하다. 매번 현재 시간(`new Date()`)으로 설정하면 크롤러가 변경되지 않은 페이지도 계속 재방문하게 되어 크롤링 예산이 낭비된다.

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

### 구현 예시 (API Gateway + Lambda 방식)

```
┌─────────────────────────────────────────────────────┐
│            API Gateway + Lambda 아키텍처             │
├─────────────────────────────────────────────────────┤
│                                                     │
│  [CI/CD 빌드]                                       │
│       │                                             │
│       ▼                                             │
│  [API Gateway] ──▶ [Lambda] ──▶ [RDS/DB]           │
│       │               │                             │
│       │               ▼                             │
│       │         [ID 목록 응답]                       │
│       │               │                             │
│       ◀───────────────┘                             │
│       │                                             │
│       ▼                                             │
│  [SiteMap 생성 & 배포]                              │
│                                                     │
└─────────────────────────────────────────────────────┘
```

**Lambda 함수 (ID 목록 조회용)**
```typescript
// lambda/get-building-ids.ts
import { APIGatewayProxyHandler } from 'aws-lambda';
import mysql from 'mysql2/promise';

export const handler: APIGatewayProxyHandler = async (event) => {
  const connection = await mysql.createConnection({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
  });

  // 페이지네이션 지원
  const page = parseInt(event.queryStringParameters?.page || '1');
  const limit = parseInt(event.queryStringParameters?.limit || '10000');
  const offset = (page - 1) * limit;

  const [rows] = await connection.execute(
    'SELECT id FROM buildings WHERE is_active = 1 LIMIT ? OFFSET ?',
    [limit, offset]
  );

  const [countResult] = await connection.execute(
    'SELECT COUNT(*) as total FROM buildings WHERE is_active = 1'
  );

  await connection.end();

  return {
    statusCode: 200,
    headers: {
      'Content-Type': 'application/json',
      'Cache-Control': 'public, max-age=3600', // 1시간 캐시
    },
    body: JSON.stringify({
      ids: rows.map((row: any) => row.id),
      pagination: {
        page,
        limit,
        total: countResult[0].total,
        hasNext: offset + limit < countResult[0].total,
      },
    }),
  };
};
```

**빌드 시 호출**
```typescript
// scripts/fetch-ids-for-sitemap.ts
async function fetchAllBuildingIds(): Promise<number[]> {
  const API_URL = 'https://api-gateway-id.execute-api.ap-northeast-2.amazonaws.com/prod/building-ids';
  const allIds: number[] = [];
  let page = 1;
  let hasNext = true;

  while (hasNext) {
    const response = await fetch(`${API_URL}?page=${page}&limit=10000`);
    const data = await response.json();
    allIds.push(...data.ids);
    hasNext = data.pagination.hasNext;
    page++;
  }

  return allIds;
}
```

---

### 구현 예시 (S3 사전 생성 방식)

**Lambda 함수 (Node.js/TypeScript)**
```typescript
// lambda/generate-sitemap.ts
import { S3Client, PutObjectCommand } from '@aws-sdk/client-s3';
import mysql from 'mysql2/promise';

const s3Client = new S3Client({ region: 'ap-northeast-2' });

interface BuildingRow {
  id: number;
}

function generateSitemap(ids: number[], baseUrl: string): string {
  const urls = ids.map(id => `
    <url>
      <loc>${baseUrl}/building/${id}</loc>
      <lastmod>${new Date().toISOString().split('T')[0]}</lastmod>
      <changefreq>weekly</changefreq>
      <priority>0.8</priority>
    </url>`).join('');

  return `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
${urls}
</urlset>`;
}

export const handler = async () => {
  // DB에서 ID 목록 조회
  const connection = await mysql.createConnection({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
  });

  const [rows] = await connection.execute<BuildingRow[]>(
    'SELECT id FROM buildings WHERE is_active = 1'
  );
  const buildingIds = rows.map(row => row.id);

  await connection.end();

  // SiteMap XML 생성
  const sitemapXml = generateSitemap(buildingIds, 'https://example.com');

  // S3 업로드
  await s3Client.send(new PutObjectCommand({
    Bucket: 'my-sitemap-bucket',
    Key: 'sitemaps/buildings.xml',
    Body: sitemapXml,
    ContentType: 'application/xml',
  }));

  return {
    statusCode: 200,
    body: `Generated sitemap with ${buildingIds.length} URLs`
  };
};
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
| 빠르게 구현하고 싶다 | 빌드 시 백엔드 API 호출 |
| 백엔드 부하 분리가 필요하다 | API Gateway + Lambda |
| 대규모 + 안정성 중요 | **S3 사전 생성** ⭐ |
| 실시간성이 중요하다 | ISR/On-demand |
| DB 부하를 줄이고 싶다 | Redis/ElastiCache 캐싱 |
| 글로벌 서비스 | CDN Edge 동적 생성 |

**최종 추천:** 수만 개 이상의 동적 URL이 있다면 **S3 사전 생성 방식**이 가장 효율적이고 안정적이다. 중규모 서비스에서 백엔드 서버 부하가 걱정된다면 **API Gateway + Lambda**로 ID 조회를 분리하는 것도 좋은 선택이다.

*마지막 업데이트: 2025년 01월*
