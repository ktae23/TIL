# Feature Flag와 점진적 배포

Feature Flag(기능 플래그)는 코드 배포와 기능 릴리스를 분리하는 기법이다. 배포 없이 런타임에 기능을 활성화/비활성화할 수 있으며, 점진적 배포(Progressive Delivery)와 결합하면 특정 사용자 그룹에게만 새 기능을 점진적으로 노출시키는 것이 가능하다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### Feature Flag란?

Feature Flag는 코드 내에 삽입된 조건문으로, 외부 설정에 따라 특정 기능의 실행 경로를 결정한다.

```
기존 배포 방식:
  배포 = 릴리스 (동시에 일어남)
  코드 변경 → 빌드 → 테스트 → 배포 → 사용자에게 노출

Feature Flag 방식:
  배포 ≠ 릴리스 (분리됨)
  코드 변경 → 빌드 → 테스트 → 배포 (Flag OFF)
                                       ↓
                              원하는 시점에 Flag ON → 사용자에게 노출
```

### Feature Flag의 유형

```
┌──────────────────────────────────────────────────────────────┐
│                    Feature Flag 유형                          │
│                                                              │
│  1. Release Flag (릴리스 플래그)                              │
│     - 미완성 기능을 숨기고 Trunk-Based Development 지원       │
│     - 수명: 짧음 (기능 완성 후 제거)                          │
│                                                              │
│  2. Experiment Flag (실험 플래그)                             │
│     - A/B 테스트, 사용자 행동 분석                            │
│     - 수명: 중간 (실험 완료 후 제거)                          │
│                                                              │
│  3. Ops Flag (운영 플래그)                                   │
│     - 시스템 동작 제어 (Circuit Breaker, 부하 제어)           │
│     - 수명: 길거나 영구적                                    │
│                                                              │
│  4. Permission Flag (권한 플래그)                             │
│     - 유료 기능, 베타 테스터 등 사용자별 기능 제어            │
│     - 수명: 영구적                                           │
└──────────────────────────────────────────────────────────────┘
```

### 점진적 배포 (Progressive Delivery)

점진적 배포는 Feature Flag와 배포 전략을 결합하여 새 기능을 단계적으로 릴리스하는 방법론이다.

```
Progressive Delivery = Feature Flags + Canary + Observability

Phase 1: Internal (직원만)     → 1% 사용자
Phase 2: Beta Users            → 5% 사용자
Phase 3: Early Adopters        → 20% 사용자
Phase 4: General Availability  → 100% 사용자
```

## 2. 왜 알아야 하는가 (Why)

### Feature Flag의 비즈니스 가치

| 가치 | 설명 |
|------|------|
| 배포 리스크 감소 | 문제 시 Flag OFF로 즉시 비활성화 (롤백 불필요) |
| Trunk-Based Development | 미완성 기능을 숨기고 main에 바로 커밋 |
| A/B 테스트 | 데이터 기반 의사결정으로 비즈니스 최적화 |
| 점진적 릴리스 | 소수 사용자에게 먼저 노출하여 검증 |
| 다크 런칭 | 사용자 모르게 새 기능을 배포하고 내부 테스트 |
| Kill Switch | 장애 시 특정 기능을 즉시 비활성화 |

### Feature Flag 없이 겪는 문제

```
문제 1: Long-lived Feature Branch
  develop ─────────────────────────────→
           ↑ feature/big-feature (3달)  ↑
           └─────── 충돌 지옥 ──────────┘

문제 2: Big Bang Release
  3달간 개발 → 한 번에 배포 → 버그 다발 → 전체 롤백

Feature Flag 해결:
  main ─── commit ── commit ── commit ──→
           (flag off) (flag off) (flag on → 1% → 10% → 100%)
```

## 3. 내부 구현 분석 (How)

### 기본 구현 패턴

#### 1. 단순 Boolean Flag

```java
public class FeatureFlagService {

    private final Map<String, Boolean> flags;

    public boolean isEnabled(String flagName) {
        return flags.getOrDefault(flagName, false);
    }
}

// 사용
if (featureFlags.isEnabled("new-checkout-flow")) {
    return newCheckoutService.process(cart);
} else {
    return legacyCheckoutService.process(cart);
}
```

#### 2. 사용자 기반 타겟팅

```java
public class FeatureFlagService {

    public boolean isEnabled(String flagName, User user) {
        FlagConfig config = getFlagConfig(flagName);

        // 1. Kill switch 확인
        if (!config.isGloballyEnabled()) return false;

        // 2. 특정 사용자 화이트리스트
        if (config.getWhitelistedUsers().contains(user.getId())) return true;

        // 3. 사용자 그룹 확인
        if (config.getTargetGroups().contains(user.getGroup())) return true;

        // 4. 비율 기반 롤아웃
        if (config.getRolloutPercentage() > 0) {
            int hash = Math.abs(user.getId().hashCode() % 100);
            return hash < config.getRolloutPercentage();
        }

        return false;
    }
}
```

#### 3. 비율 기반 점진적 롤아웃

```java
// 일관된 사용자 경험 보장 (같은 사용자는 항상 같은 결과)
public boolean isInRollout(String flagName, String userId, int percentage) {
    // userId + flagName의 해시로 0~99 사이 값 생성
    String key = flagName + ":" + userId;
    int bucket = Math.abs(key.hashCode() % 100);
    return bucket < percentage;
}

// 5% 롤아웃: bucket 0~4인 사용자만 활성화
// 10% 롤아웃: bucket 0~9인 사용자만 활성화 (5%에 포함된 사용자는 계속 포함)
// 100% 롤아웃: 모든 사용자 활성화
```

### Feature Flag 관리 도구

#### 주요 도구 비교

| 도구 | 유형 | 특징 |
|------|------|------|
| LaunchDarkly | SaaS | 가장 풍부한 기능, 실시간 변경, 비용 높음 |
| Unleash | OSS/SaaS | 셀프 호스팅 가능, 무료 tier |
| Flagsmith | OSS/SaaS | Feature Flag + Remote Config |
| Split | SaaS | A/B 테스트 특화 |
| ConfigCat | SaaS | 가벼운 구현, 저렴한 가격 |

#### Unleash 연동 예시

```java
// Unleash Java SDK
import io.getunleash.Unleash;
import io.getunleash.DefaultUnleash;
import io.getunleash.util.UnleashConfig;

@Configuration
public class FeatureFlagConfig {

    @Bean
    public Unleash unleash() {
        UnleashConfig config = UnleashConfig.builder()
            .appName("my-app")
            .instanceId("my-app-instance-1")
            .unleashAPI("http://unleash.internal:4242/api")
            .apiKey("default:development.xxx")
            .build();

        return new DefaultUnleash(config);
    }
}

@Service
public class PaymentService {

    private final Unleash unleash;

    public PaymentResult process(Order order, User user) {
        UnleashContext context = UnleashContext.builder()
            .userId(user.getId())
            .addProperty("plan", user.getPlan())
            .build();

        if (unleash.isEnabled("new-payment-gateway", context)) {
            return stripeGateway.process(order);
        }
        return legacyGateway.process(order);
    }
}
```

### 점진적 배포 워크플로우

```
┌─────────────────────────────────────────────────────────┐
│              Progressive Delivery Workflow                │
│                                                         │
│  Day 1: Deploy with Flag OFF                            │
│    ├── 코드 배포 (모든 서버)                              │
│    └── 새 기능은 비활성화 상태                            │
│                                                         │
│  Day 2: Internal Testing (Flag ON for employees)         │
│    ├── 직원 이메일 도메인으로 타겟팅                      │
│    └── 내부 QA 및 버그 수정                              │
│                                                         │
│  Day 3: Beta Users (1% rollout)                          │
│    ├── 베타 프로그램 가입 사용자에게 노출                  │
│    └── 에러율, 사용 패턴 모니터링                        │
│                                                         │
│  Day 5: Gradual Rollout (10% → 25% → 50%)               │
│    ├── 메트릭 확인 후 단계적 확대                         │
│    └── 이상 감지 시 비율 축소 또는 0%로 설정              │
│                                                         │
│  Day 7: General Availability (100%)                      │
│    ├── 모든 사용자에게 활성화                             │
│    └── 안정화 확인 후 Flag 코드 제거                     │
│                                                         │
│  Day 14: Cleanup                                        │
│    └── Feature Flag 코드와 이전 코드 경로 제거           │
└─────────────────────────────────────────────────────────┘
```

### A/B 테스트 연계

```
Feature Flag + A/B Testing:

                    ┌─ Variant A (기존 체크아웃) → 전환율 측정
사용자 ─→ Flag ─→ ─┤
                    └─ Variant B (새 체크아웃)   → 전환율 측정

통계적으로 유의한 차이가 나면:
  - B가 우수 → B를 기본으로 설정
  - A가 우수 → A 유지, B 코드 제거
```

```java
// A/B 테스트 구현
public String getCheckoutVariant(User user) {
    UnleashContext context = UnleashContext.builder()
        .userId(user.getId())
        .build();

    Variant variant = unleash.getVariant("checkout-experiment", context);

    // variant.getName() = "control" 또는 "new-checkout"
    // variant.getPayload() = 추가 설정값
    return variant.getName();
}
```

## 4. 실전 예제

### 예제 1: Spring Boot Feature Flag 구현

```java
// 간단한 자체 구현 Feature Flag
@Component
public class FeatureFlags {

    @Value("${features.new-search:false}")
    private boolean newSearch;

    @Value("${features.new-search.rollout-percentage:0}")
    private int newSearchRollout;

    public boolean isNewSearchEnabled(String userId) {
        if (!newSearch) return false;
        if (newSearchRollout >= 100) return true;
        if (newSearchRollout <= 0) return false;

        int bucket = Math.abs(userId.hashCode() % 100);
        return bucket < newSearchRollout;
    }
}

@RestController
public class SearchController {

    private final FeatureFlags featureFlags;
    private final SearchService legacySearch;
    private final SearchService newSearch;

    @GetMapping("/search")
    public SearchResult search(
            @RequestParam String query,
            @AuthenticationPrincipal UserDetails user) {

        if (featureFlags.isNewSearchEnabled(user.getUsername())) {
            return newSearch.search(query);
        }
        return legacySearch.search(query);
    }
}
```

```yaml
# application-prod.yml
features:
  new-search: true
  new-search:
    rollout-percentage: 10    # 10% 사용자에게 활성화
```

### 예제 2: React Feature Flag

```typescript
// Feature Flag Context
interface FeatureFlags {
  newDashboard: boolean;
  darkMode: boolean;
  betaFeatures: boolean;
}

const FeatureFlagContext = createContext<FeatureFlags>({
  newDashboard: false,
  darkMode: false,
  betaFeatures: false,
});

function FeatureFlagProvider({ children }: { children: ReactNode }) {
  const [flags, setFlags] = useState<FeatureFlags>(defaultFlags);

  useEffect(() => {
    // 서버에서 Feature Flag 가져오기
    fetch('/api/feature-flags')
      .then(res => res.json())
      .then(setFlags);
  }, []);

  return (
    <FeatureFlagContext.Provider value={flags}>
      {children}
    </FeatureFlagContext.Provider>
  );
}

// Feature Flag Hook
function useFeatureFlag(flagName: keyof FeatureFlags): boolean {
  const flags = useContext(FeatureFlagContext);
  return flags[flagName] ?? false;
}

// 사용
function Dashboard() {
  const isNewDashboard = useFeatureFlag('newDashboard');

  if (isNewDashboard) {
    return <NewDashboard />;
  }
  return <LegacyDashboard />;
}
```

### 예제 3: Ops Flag — Circuit Breaker 패턴

```java
// 운영 중 외부 서비스 장애 대응용 Flag
@Service
public class RecommendationService {

    private final FeatureFlags featureFlags;

    public List<Product> getRecommendations(User user) {
        // ML 추천 서비스가 장애 시 Flag OFF로 즉시 대체
        if (featureFlags.isEnabled("ml-recommendation-service")) {
            try {
                return mlRecommendationClient.getRecommendations(user);
            } catch (Exception e) {
                log.warn("ML recommendation failed, falling back", e);
            }
        }

        // Fallback: 인기 상품 목록
        return productRepository.findTopByOrderCount(10);
    }
}

// 장애 발생 시:
// 1. ML 서비스 장애 감지
// 2. Ops 담당자가 "ml-recommendation-service" Flag OFF
// 3. 모든 서버가 즉시 Fallback으로 전환
// 4. ML 서비스 복구 후 Flag ON
// → 배포 없이 실시간으로 동작 변경
```

## 5. 정리

| 항목 | 설명 |
|------|------|
| Feature Flag | 코드 배포와 기능 릴리스를 분리하는 조건문 |
| Release Flag | 미완성 기능 숨김, 수명 짧음 |
| Experiment Flag | A/B 테스트, 사용자 행동 분석 |
| Ops Flag | 시스템 동작 제어, Kill Switch |
| Progressive Delivery | Flag + Canary + Observability 결합 |
| A/B Testing | 데이터 기반 기능 비교 실험 |

### Feature Flag 베스트 프랙티스

1. **Flag의 수명을 관리** — Release Flag는 기능 완성 후 반드시 제거
2. **Flag 명명 규칙 통일** — `feature.{domain}.{name}` 형식 등
3. **기본값은 OFF** — 새 Flag의 기본 상태는 비활성화
4. **Flag 목록을 정기 정리** — "Flag Debt"이 쌓이지 않도록
5. **테스트에서 양쪽 경로 검증** — Flag ON/OFF 모두 테스트
6. **모니터링 연동** — Flag 변경 이벤트를 메트릭/로그에 기록
7. **접근 권한 관리** — 프로덕션 Flag 변경 권한을 제한

### Feature Flag 기술 부채 관리

```
Flag 생애주기:
  Created → Active → Stale → Removed

정리 기준:
  - Release Flag: 기능 GA 후 2주 이내 제거
  - Experiment Flag: 실험 종료 후 1주 이내 제거
  - Ops Flag: 정기 리뷰 (분기 1회)
  - Permission Flag: 영구 유지 가능

자동화:
  - Flag 생성 시 만료일 설정 필수
  - CI에서 만료된 Flag 경고
  - 대시보드에 활성 Flag 수 모니터링
```

---
*참고: Martin Fowler - Feature Toggles, Pete Hodgson - Feature Toggles (Types), LaunchDarkly Blog*
