# Geohash 가이드 - 한국 지역 기준

## 목차
1. [Geohash 개요](#geohash-개요)
2. [정밀도별 셀 크기](#정밀도별-셀-크기)
3. [한국 Geohash 전체 목록](#한국-geohash-전체-목록)
4. [주요 도시별 Geohash](#주요-도시별-geohash)
5. [Java Geohash 라이브러리](#java-geohash-라이브러리)
6. [활용 예제](#활용-예제)

---

## Geohash 개요

Geohash는 위도/경도 좌표를 짧은 문자열로 인코딩하는 지리적 좌표 시스템입니다.

### 핵심 특징

| 특징 | 설명 |
|------|------|
| **계층적 구조** | 문자열이 길수록 더 정밀한 위치 표현 |
| **근접성** | 비슷한 접두사 = 지리적으로 가까운 위치 |
| **인코딩** | Base32 (0-9, b-z, a/i/l/o 제외) |
| **격자 분할** | 각 문자 추가 시 32개 하위 셀로 분할 |

### 작동 원리

1. 전 세계를 32개 영역으로 분할
2. 각 영역을 다시 32개로 분할 (반복)
3. 위도/경도를 이진수로 변환 후 교차 배치
4. 5비트씩 묶어 Base32 문자로 변환

---

## 정밀도별 셀 크기

| 길이 | 셀 너비 | 셀 높이 | 용도 예시 |
|------|---------|---------|-----------|
| 1 | 5,000km | 5,000km | 대륙 수준 |
| 2 | 1,250km | 625km | 국가 수준 |
| 3 | 156km | 156km | 대규모 지역 |
| 4 | 39km | 19.5km | 도시 수준 |
| 5 | 4.9km | 4.9km | 구/군 수준 |
| 6 | 1.2km | 610m | 동네 수준 |
| 7 | 153m | 153m | 블록 수준 |
| 8 | 38m | 19m | 건물 수준 |
| 9 | 4.8m | 4.8m | 정밀 위치 |

---

## 한국 Geohash 전체 목록

### 한국 좌표 범위

```
위도: 33.0° ~ 38.6° N (제주도 ~ 휴전선)
경도: 124.5° ~ 132.0° E (서해 ~ 독도)
```

### 2자리 Geohash (한국 전체 커버)

| Geohash | 대략적 범위 |
|---------|-------------|
| **wy** | 한반도 대부분 (서울, 경기, 강원, 충청, 경상, 전라 북부) |
| **wz** | 동해안 일부, 울릉도, 독도 |
| **wv** | 제주도, 전라남도 남부 해안 |
| **wx** | 전라남도 서부 해안, 서해 일부 |

### 3자리 Geohash (지역 수준)

#### 수도권 및 강원

| Geohash | 지역 |
|---------|------|
| **wyd** | 서울 중심, 경기 중부 |
| **wyc** | 서울 서부, 인천, 경기 서부 |
| **wyf** | 경기 동부, 강원 서부 |
| **wy9** | 경기 북부, 연천, 철원 |
| **wyg** | 강원 영서 (춘천, 원주) |
| **wyu** | 강원 영동 (강릉, 속초) |
| **wye** | 경기 남동부, 이천, 여주 |

#### 충청권

| Geohash | 지역 |
|---------|------|
| **wy6** | 충남 북부 (천안, 아산) |
| **wy3** | 충남 서부 (서산, 태안) |
| **wy7** | 충북 (청주, 충주) |
| **wys** | 충북 동부, 단양 |

#### 전라권

| Geohash | 지역 |
|---------|------|
| **wy1** | 전북 (전주, 익산, 군산) |
| **wy4** | 전북 동부, 남원 |
| **wxz** | 전남 서부 (목포, 무안) |
| **wxr** | 전남 서남부 해안 |
| **wvp** | 전남 남부 해안 (여수, 완도) |
| **wvn** | 전남 남해안, 해남 |

#### 경상권

| Geohash | 지역 |
|---------|------|
| **wy5** | 경북 서부 (구미, 김천) |
| **wyk** | 경북 중부 (대구, 경산) |
| **wym** | 경북 동부 (포항, 영덕) |
| **wyt** | 경북 북부 (안동, 영주) |
| **wyq** | 경북 동북부 (울진) |
| **wy7** | 경남 북부, 충북 남부 |
| **wyh** | 경남 중부 (창원, 김해) |
| **wy7** | 경남 서부 (진주, 사천) |
| **wz0** | 울릉도 |
| **wz2** | 독도 |

#### 제주권

| Geohash | 지역 |
|---------|------|
| **wvb** | 제주시 |
| **wv8** | 제주 동부 (성산, 표선) |
| **wv2** | 서귀포시 |
| **wv0** | 제주 서부 (한림, 애월) |

### 4자리 Geohash (주요 도시 상세)

#### 서울

| Geohash | 지역 |
|---------|------|
| **wydm** | 강남, 서초, 송파 |
| **wydk** | 용산, 성동, 광진 |
| **wydh** | 강동, 하남 |
| **wyd7** | 마포, 서대문, 은평 |
| **wyd5** | 종로, 중구, 동대문 |
| **wyde** | 성북, 강북, 도봉, 노원 |
| **wyds** | 관악, 동작, 영등포 |
| **wyc** | 강서, 양천, 구로, 금천 |

#### 부산/울산

| Geohash | 지역 |
|---------|------|
| **wy78** | 부산 중구, 동구, 부산진구 |
| **wy7b** | 부산 해운대, 기장 |
| **wy7c** | 울산 중구, 남구 |
| **wy7f** | 울산 울주군 |

#### 대구

| Geohash | 지역 |
|---------|------|
| **wykq** | 대구 중구, 남구, 수성구 |
| **wykn** | 대구 북구, 동구 |
| **wykj** | 대구 달서구, 서구 |

#### 인천

| Geohash | 지역 |
|---------|------|
| **wyc9** | 인천 중구, 동구, 미추홀구 |
| **wycc** | 인천 연수구, 남동구 |
| **wycb** | 인천 부평구, 계양구 |
| **wyc3** | 인천 서구, 강화도 |

---

## 주요 도시별 Geohash

### 주요 랜드마크 Geohash (6자리)

| 장소 | 좌표 (위도, 경도) | Geohash |
|------|-------------------|---------|
| 서울시청 | 37.5666, 126.9784 | wydm6v |
| 강남역 | 37.4980, 127.0276 | wydmb0 |
| 잠실역 | 37.5132, 127.1001 | wydmfz |
| 명동역 | 37.5609, 126.9860 | wydm6x |
| 홍대입구역 | 37.5571, 126.9236 | wydm3q |
| 인천공항 | 37.4602, 126.4407 | wyc2wu |
| 부산역 | 35.1150, 129.0416 | wy78kh |
| 해운대해수욕장 | 35.1587, 129.1604 | wy7b5c |
| 대구역 | 35.8792, 128.6283 | wykqhg |
| 광주역 | 35.1595, 126.9024 | wy4pw2 |
| 대전역 | 36.3323, 127.4346 | wy6qfq |
| 제주시청 | 33.4996, 126.5312 | wvb8tz |
| 성산일출봉 | 33.4583, 126.9426 | wv8wm4 |
| 울릉도 도동항 | 37.4846, 130.9062 | wz0dxm |
| 독도 | 37.2426, 131.8647 | wz2mgh |

---

## Java Geohash 라이브러리

### 1. ch.hsr:geohash (권장)

가장 널리 사용되는 Java Geohash 라이브러리입니다.

#### Maven 의존성

```xml
<dependency>
    <groupId>ch.hsr</groupId>
    <artifactId>geohash</artifactId>
    <version>1.4.0</version>
</dependency>
```

#### Gradle 의존성

```groovy
implementation 'ch.hsr:geohash:1.4.0'
```

#### 주요 클래스

| 클래스 | 설명 |
|--------|------|
| `GeoHash` | 핵심 Geohash 인코딩/디코딩 클래스 |
| `WGS84Point` | 위도/경도 좌표 표현 |
| `BoundingBox` | Geohash 셀의 경계 박스 |
| `GeoHashCircleQuery` | 원형 범위 검색 |

### GeoHashCircleQuery 상세

`GeoHashCircleQuery`는 **특정 중심점에서 원형 반경 내의 모든 GeoHash를 검색**할 때 사용합니다.

#### 동작 원리

```
        ┌─────────────────────┐
        │    ┌───┬───┬───┐    │
        │    │   │   │   │    │
        │    ├───┼───┼───┤    │
        │    │   │ ● │   │ ←── 반경 내 포함되는 모든 GeoHash 셀 반환
        │    ├───┼───┼───┤    │
        │    │   │   │   │    │
        │    └───┴───┴───┘    │
        └─────────────────────┘
              원형 반경 쿼리
```

#### 사용법

```java
import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;
import ch.hsr.geohash.queries.GeoHashCircleQuery;

// 강남역 좌표에서 반경 1km 내 모든 GeoHash 검색
WGS84Point center = new WGS84Point(37.4980, 127.0276);
double radiusMeters = 1000.0;  // 1km

GeoHashCircleQuery query = new GeoHashCircleQuery(center, radiusMeters);

// 검색된 모든 GeoHash 순회
for (GeoHash hash : query.getSearchHashes()) {
    System.out.println(hash.toBase32());
}
```

#### 주요 메서드

| 메서드 | 설명 |
|--------|------|
| `getSearchHashes()` | 반경 내 포함/겹치는 모든 GeoHash 반환 |
| `contains(GeoHash)` | 특정 GeoHash가 원 안에 포함되는지 확인 |

#### 실전 활용 예제

```java
@Service
public class NearbySearchService {

    public List<Store> findStoresWithinRadius(double lat, double lon, double radiusKm) {
        WGS84Point center = new WGS84Point(lat, lon);
        GeoHashCircleQuery query = new GeoHashCircleQuery(center, radiusKm * 1000);

        // 원형 범위 내 모든 geohash prefix 수집
        Set<String> prefixes = new HashSet<>();
        for (GeoHash hash : query.getSearchHashes()) {
            prefixes.add(hash.toBase32());
        }

        // DB에서 해당 prefix로 검색
        List<Store> candidates = storeRepository.findByGeohashIn(prefixes);

        // 실제 거리 계산으로 정확한 필터링
        return candidates.stream()
            .filter(s -> calculateDistance(lat, lon, s.getLat(), s.getLon()) <= radiusKm)
            .toList();
    }
}
```

#### getAdjacent()와의 비교

| 방식 | 용도 | 반환 |
|------|------|------|
| `getAdjacent()` | 인접 8개 셀 조회 | 항상 8개 |
| `GeoHashCircleQuery` | 반경 기반 검색 | 반경에 따라 유동적 |

`GeoHashCircleQuery`는 반경이 클수록 더 많은 셀을, 작을수록 적은 셀을 반환하여 **동적 범위 검색**에 적합합니다.

#### 기본 사용법

```java
import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;

public class GeoHashExample {

    // 좌표 → Geohash 변환
    public String encode(double latitude, double longitude, int precision) {
        GeoHash geoHash = GeoHash.withCharacterPrecision(latitude, longitude, precision);
        return geoHash.toBase32();
    }

    // Geohash → 좌표 변환
    public WGS84Point decode(String geohashString) {
        GeoHash geoHash = GeoHash.fromGeohashString(geohashString);
        return geoHash.getOriginatingPoint();
    }

    // 인접 셀 조회
    public GeoHash[] getNeighbors(String geohashString) {
        GeoHash geoHash = GeoHash.fromGeohashString(geohashString);
        return geoHash.getAdjacent();
    }
}
```

### 2. Spatial4j

Apache 라이센스의 공간 데이터 라이브러리입니다.

#### Maven 의존성

```xml
<dependency>
    <groupId>org.locationtech.spatial4j</groupId>
    <artifactId>spatial4j</artifactId>
    <version>0.8</version>
</dependency>
```

#### 사용 예제

```java
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.io.GeohashUtils;

public class Spatial4jExample {

    public String encode(double lat, double lon, int precision) {
        return GeohashUtils.encodeLatLon(lat, lon, precision);
    }

    public double[] decode(String geohash) {
        return GeohashUtils.decode(geohash, SpatialContext.GEO);
    }
}
```

### 3. Elasticsearch Java Client (고급)

Elasticsearch를 사용하는 경우 내장 geo 기능을 활용할 수 있습니다.

```java
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.geometry.utils.Geohash;

public class ElasticsearchGeoExample {

    public String encode(double lat, double lon, int precision) {
        return Geohash.stringEncode(lon, lat, precision);
    }
}
```

---

## 활용 예제

### 1. 근처 매물 검색 서비스

```java
import ch.hsr.geohash.GeoHash;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertySearchService {

    private final PropertyRepository propertyRepository;

    /**
     * 특정 위치 근처의 매물 검색
     * @param lat 위도
     * @param lon 경도
     * @param precision geohash 정밀도 (5: 약 5km, 6: 약 1km)
     */
    public List<Property> findNearbyProperties(double lat, double lon, int precision) {
        GeoHash centerHash = GeoHash.withCharacterPrecision(lat, lon, precision);

        // 중심 셀 + 인접 8개 셀의 geohash 수집
        List<String> searchHashes = new ArrayList<>();
        searchHashes.add(centerHash.toBase32());

        for (GeoHash adjacent : centerHash.getAdjacent()) {
            searchHashes.add(adjacent.toBase32());
        }

        // geohash prefix로 검색 (인덱스 활용)
        return propertyRepository.findByGeohashStartingWithIn(searchHashes);
    }
}
```

### 2. Repository 쿼리

```java
@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {

    // 단일 geohash prefix 검색
    List<Property> findByGeohashStartingWith(String geohashPrefix);

    // 여러 geohash prefix 검색 (인접 셀 포함)
    @Query("SELECT p FROM Property p WHERE " +
           "SUBSTRING(p.geohash, 1, :length) IN :prefixes")
    List<Property> findByGeohashPrefixes(
        @Param("prefixes") List<String> prefixes,
        @Param("length") int length
    );
}
```

### 3. Entity에 Geohash 저장

```java
@Entity
@Table(name = "property", indexes = {
    @Index(name = "idx_property_geohash", columnList = "geohash")
})
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double latitude;
    private Double longitude;

    @Column(length = 12)
    private String geohash;

    @PrePersist
    @PreUpdate
    public void updateGeohash() {
        if (latitude != null && longitude != null) {
            this.geohash = GeoHash
                .withCharacterPrecision(latitude, longitude, 9)
                .toBase32();
        }
    }
}
```

### 4. Redis 캐싱 활용

```java
@Service
@RequiredArgsConstructor
public class LocationCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final int CACHE_PRECISION = 6; // 약 1km 단위
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    public void cacheLocationData(double lat, double lon, Object data) {
        String geohash = GeoHash
            .withCharacterPrecision(lat, lon, CACHE_PRECISION)
            .toBase32();

        String cacheKey = "location:" + geohash;
        redisTemplate.opsForValue().set(cacheKey, data, CACHE_TTL);
    }

    public Object getLocationData(double lat, double lon) {
        String geohash = GeoHash
            .withCharacterPrecision(lat, lon, CACHE_PRECISION)
            .toBase32();

        String cacheKey = "location:" + geohash;
        return redisTemplate.opsForValue().get(cacheKey);
    }
}
```

### 5. 거리 계산 유틸리티

```java
public class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Haversine 공식을 사용한 두 지점 간 거리 계산
     */
    public static double calculateDistance(
            double lat1, double lon1,
            double lat2, double lon2) {

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) *
                   Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * 정밀도에 따른 적절한 geohash 길이 반환
     */
    public static int getPrecisionForRadius(double radiusKm) {
        if (radiusKm > 1000) return 2;
        if (radiusKm > 150) return 3;
        if (radiusKm > 30) return 4;
        if (radiusKm > 5) return 5;
        if (radiusKm > 1) return 6;
        if (radiusKm > 0.15) return 7;
        if (radiusKm > 0.04) return 8;
        return 9;
    }
}
```

---

## 주의사항

### 1. 경계선 문제 (Edge Case)

Geohash의 가장 큰 단점은 **물리적으로 가까운 두 지점이 완전히 다른 geohash를 가질 수 있다**는 점입니다.

```
예시: 서울-인천 경계
- 서울 강서구: wyc...
- 인천 계양구: wyc... (같을 수도 있지만)
- 경계 지점에서는 완전히 다른 prefix를 가질 수 있음
```

**해결책**: 항상 인접한 8개 셀도 함께 검색

```java
// 잘못된 방법 (경계 누락 가능)
List<Property> results = repo.findByGeohashStartingWith("wydm");

// 올바른 방법 (인접 셀 포함)
GeoHash center = GeoHash.fromGeohashString("wydm");
List<String> searchPrefixes = new ArrayList<>();
searchPrefixes.add("wydm");
for (GeoHash adj : center.getAdjacent()) {
    searchPrefixes.add(adj.toBase32());
}
List<Property> results = repo.findByGeohashPrefixes(searchPrefixes, 4);
```

### 2. 정밀도 선택 가이드

| 사용 사례 | 권장 정밀도 | 이유 |
|-----------|-------------|------|
| 전국 단위 서비스 | 4 | 광역시/도 수준 그룹화 |
| 도시 내 검색 | 5-6 | 동네 수준 검색 |
| 상세 위치 매칭 | 7-8 | 건물/블록 수준 |
| 정밀 위치 저장 | 9 | 최대 정밀도 |

### 3. 인덱싱 전략

```sql
-- MySQL에서 geohash 컬럼 인덱싱
CREATE INDEX idx_geohash ON property (geohash(6));

-- prefix 검색이 인덱스를 활용하도록
SELECT * FROM property WHERE geohash LIKE 'wydm%';
```

---

## 참고 자료

- [Geohash Wikipedia](https://en.wikipedia.org/wiki/Geohash)
- [ch.hsr.geohash GitHub](https://github.com/kungfoo/geohash-java)
- [Geohash Explorer (시각화 도구)](http://geohash.gofreerange.com/)
