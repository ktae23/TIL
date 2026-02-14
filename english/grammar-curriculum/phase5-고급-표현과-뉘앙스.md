# Phase 5: 고급 표현과 뉘앙스 (Week 11-12)

> **목표**: 가정법으로 현실/비현실을 구분하고, 비교 표현으로 대안을 평가하며, 구동사와 It/There 구문으로 영어다운 자연스러움을 획득한다.
>
> **한일 교수 핵심 원리**: "영어는 현실과의 거리를 동사 형태로 표현한다. 멀수록 과거형을 쓴다."

---

## Week 11: 가정법 — 현실과 비현실의 구분

---

### Lesson 28 — 가정법 과거: 현재 사실의 반대

---

#### 1. WHY — 왜 가정법이 존재하는가

한국어에서는 "내가 시니어 개발자**라면**" 하나로 끝난다. "-라면"만 붙이면 가정이 된다. 그런데 영어는 왜 이렇게 복잡한 체계를 만들었을까?

영어는 **현실과의 심리적 거리(psychological distance)** 를 동사 형태로 정밀하게 코딩한다. 이것이 한일 교수가 말하는 **"거리감의 원리"** 다.

| 현실과의 거리 | 동사 형태 | 예문 |
|---|---|---|
| 현실 (사실) | 현재형 | I **am** a junior developer. |
| 약간 먼 가정 (공손) | 과거형 | **Could** you review my code? |
| 현재 반대 가정 | 과거형 (가정법) | If I **were** a senior, I **would** refactor this. |
| 과거 반대 가정 | 과거완료형 (가정법) | If I **had known**, I **would have** fixed it. |

> **한일 교수**: "과거형이 항상 '과거 시간'을 뜻하는 것은 아니다. 영어에서 과거형은 '현실에서 한 발짝 물러남'을 의미하기도 한다. Could you~가 Can you~보다 공손한 이유가 바로 이것이다."

**왜 was가 아니라 were인가?**

가정법에서 `were`를 쓰는 것은 **"이건 현실이 아닙니다"** 라는 깃발(flag)이다. 문법적으로 `was`도 구어에서 점점 허용되지만, `were`가 전통적으로 사용되는 이유는 명확한 **비현실 마커(unreality marker)** 역할을 하기 때문이다.

```
현실: I was tired yesterday. (과거 사실)
비현실: If I were you, I would not deploy on Friday. (현재 반대 가정)
```

만약 둘 다 `was`를 쓰면 듣는 사람이 혼동할 수 있다. `were`는 "이건 사실이 아닌 상상의 세계입니다"라는 신호를 보내는 것이다.

---

#### 2. CORE — 최소 문장으로 체득

가정법 과거의 공식:

```
If + 주어 + 동사의 과거형 ~, 주어 + would/could/might + 동사원형 ~
```

**최소 문장 5개를 소리 내어 읽자:**

| # | 가정법 문장 | 현실 |
|---|---|---|
| 1 | If I **were** rich, I **would** quit. | 나는 부자가 아니다. |
| 2 | If I **knew** the answer, I **would** tell you. | 나는 답을 모른다. |
| 3 | If she **had** time, she **could** help. | 그녀는 시간이 없다. |
| 4 | If it **worked**, we **would** ship it. | 그것은 작동하지 않는다. |
| 5 | If I **were** you, I **would** write tests. | 나는 너가 아니다. |

> **체득 포인트**: 가정법 과거는 **"지금 현실이 아닌 것"** 을 말한다. 과거 시간 이야기가 아니다!

**주절에서 would / could / might 의 차이:**

| 조동사 | 뉘앙스 | 예문 |
|---|---|---|
| would | ~할 텐데 (확실한 결과) | If I had time, I **would** refactor this. |
| could | ~할 수 있을 텐데 (가능성) | If I had access, I **could** check the logs. |
| might | ~할지도 모를 텐데 (불확실) | If we changed the DB, it **might** break something. |

---

#### 3. EXPAND — 짧은 문장에서 복잡한 문장으로

**Step 1: 기본 가정**
```
If I were a senior developer, I would refactor this.
```

**Step 2: 목적/이유 추가**
```
If I were a senior developer, I would refactor this to improve maintainability.
```

**Step 3: 조건 구체화**
```
If I were a senior developer with full access to the repository,
I would refactor this legacy module to improve maintainability.
```

**Step 4: 결과 확장**
```
If I were a senior developer with full access to the repository,
I would refactor this legacy module to improve maintainability
and reduce the technical debt that has been accumulating for years.
```

**Step 5: 복합 가정 (두 가지 조건)**
```
If I were a senior developer and if the deadline weren't so tight,
I would refactor this legacy module to improve maintainability
and reduce the technical debt that has been accumulating for years.
```

---

**실전 확장 예문 — 업무 상황별:**

**코드 리뷰에서:**
```
기본: If I were you, I would add null checks.
확장: If I were you, I would add null checks to every public method
      in this service layer to prevent NullPointerExceptions
      from propagating to the controller.
```

**기술 미팅에서:**
```
기본: If we had more time, we could write tests.
확장: If we had more time before the release,
      we could write comprehensive integration tests
      that cover all the edge cases in the tax calculation module.
```

**장애 대응에서:**
```
기본: If the monitoring were better, we would catch this.
확장: If our monitoring system were more sophisticated,
      we would catch this kind of memory leak
      before it escalated into a full production outage.
```

**인사/커리어에서:**
```
기본: If I spoke English fluently, I could join global teams.
확장: If I spoke English fluently enough to lead technical discussions,
      I could join global teams and contribute to
      the company's international expansion strategy.
```

---

#### 4. CODE — 개발자 비유로 재이해

**가정법 = `if` 조건문에서 절대 `true`가 될 수 없는 조건**

```java
// 직설법 (Indicative) = 런타임에 실제로 평가되는 조건
if (user.getRole() == Role.ADMIN) {
    grantFullAccess(user);  // 실제로 실행될 수 있음
}

// 가정법 (Subjunctive) = 컴파일 타임에 이미 false인 조건
// 주석으로만 존재하는 "만약에" 시나리오
// if (currentDeveloper.getSeniority() == Level.SENIOR) {
//     refactorEntireCodebase();  // 현실에서는 실행 불가
// }
```

> **비유**: 가정법은 프로그래밍에서 **dead code** 또는 **unreachable branch** 와 같다. 실행되지 않을 것을 알면서도 "만약 실행된다면 이런 결과가 나올 것"이라고 추론하는 것이다.

**would = 보장된 반환값, could = Optional 반환값, might = nullable 반환값**

```java
// would: 확실한 결과
if (condition) return definiteResult;       // would

// could: 가능성이 있는 결과
if (condition) return Optional.of(result);  // could

// might: 불확실한 결과
if (condition) return maybeNull;            // might
```

**were vs was = strict mode vs loose mode**

```javascript
// "use strict"; ← were (엄격한 비현실 마커)
"use strict";
if (x === undefined) throw new Error();

// 일반 모드 ← was (구어에서 허용되지만 모호함)
if (x == undefined) console.log("maybe?");
```

`were`는 JavaScript의 `===` (strict equality)처럼 "이건 확실히 비현실입니다"라고 엄격하게 선언하는 것이고, `was`는 `==` (loose equality)처럼 구어에서 느슨하게 허용되지만 모호함을 남긴다.

---

#### 5. DOMAIN — 세무/기술 도메인 실전 예문

**세무 도메인:**

```
If this income were classified as business income instead of employment income,
the taxpayer would be subject to a significantly higher tax rate.
(이 소득이 근로소득이 아니라 사업소득으로 분류된다면, 납세자는 훨씬 높은 세율을 적용받을 것이다.)

If the taxpayer were eligible for the small business tax deduction,
the total tax liability would decrease by approximately 30%.
(납세자가 소기업 세액공제 대상이라면, 총 납세액이 약 30% 감소할 것이다.)

If we were to apply the amended tax law retroactively,
many previously filed returns would need to be corrected.
(개정된 세법을 소급 적용한다면, 이전에 제출된 많은 신고서가 수정되어야 할 것이다.)

If the tax filing deadline were extended by one month,
our batch processing system could handle the load more evenly.
(세금 신고 기한이 한 달 연장된다면, 배치 처리 시스템이 부하를 더 균등하게 처리할 수 있을 것이다.)
```

**기술 도메인:**

```
If our database were partitioned by region,
the query response time would improve dramatically.
(데이터베이스가 지역별로 파티셔닝되어 있다면, 쿼리 응답 시간이 크게 개선될 것이다.)

If we were using event-driven architecture,
we could decouple the tax calculation service from the filing service.
(이벤트 기반 아키텍처를 사용하고 있다면, 세금 계산 서비스를 신고 서비스와 분리할 수 있을 것이다.)

If the CI/CD pipeline were faster,
we might deploy hotfixes within minutes instead of hours.
(CI/CD 파이프라인이 더 빠르다면, 핫픽스를 몇 시간이 아니라 몇 분 안에 배포할 수 있을지도 모른다.)

If each microservice had its own database,
data consistency would be harder to maintain, but scalability would improve.
(각 마이크로서비스가 자체 데이터베이스를 가지고 있다면, 데이터 일관성 유지는 어렵겠지만 확장성은 좋아질 것이다.)
```

**세무 + 기술 융합 예문:**

```
If we were to migrate the tax calculation engine to a rule-based system,
we could update tax rules without redeploying the entire application.
(세금 계산 엔진을 규칙 기반 시스템으로 마이그레이션한다면,
전체 애플리케이션을 재배포하지 않고도 세금 규칙을 업데이트할 수 있을 것이다.)

If the National Tax Service API were more reliable,
we wouldn't need to implement so many retry mechanisms and fallback strategies.
(국세청 API가 더 안정적이라면,
이렇게 많은 재시도 메커니즘과 폴백 전략을 구현할 필요가 없을 것이다.)
```

---

#### 6. PRACTICE — 연습 문제

**A. 빈칸 채우기 — 현재 사실의 반대를 가정법으로 쓰기**

> 현실을 읽고, 가정법 문장을 완성하세요.

1. 현실: I don't have admin access.
   가정법: If I _______ admin access, I _______ check the production logs.

2. 현실: Our server is not fast enough.
   가정법: If our server _______ fast enough, the batch job _______ finish before midnight.

3. 현실: The API documentation is incomplete.
   가정법: If the API documentation _______ complete, we _______ integrate much faster.

4. 현실: I am not fluent in English.
   가정법: If I _______ fluent in English, I _______ participate more actively in global meetings.

5. 현실: We don't use Kubernetes.
   가정법: If we _______ Kubernetes, we _______ scale our services automatically.

**B. 작문 연습 — 다음 상황을 가정법 과거로 표현하기**

6. 당신은 주니어 개발자입니다. "내가 시니어라면 이 코드를 리팩토링할 텐데"를 영어로:
   → _____________________________________________

7. 현재 팀에 QA 엔지니어가 없습니다. "QA 엔지니어가 있다면 버그를 더 빨리 잡을 수 있을 텐데"를 영어로:
   → _____________________________________________

8. 세금 신고 시스템의 응답 시간이 느립니다. "응답 시간이 빠르다면 사용자 경험이 훨씬 좋을 텐데"를 영어로:
   → _____________________________________________

**C. 오류 교정 — 가정법 실수 찾기**

9. `If I was you, I will use a different framework.`
   → 수정: _____________________________________________

10. `If the server is faster, we would handle more requests.`
    → 수정: _____________________________________________

<details>
<summary>정답 보기</summary>

1. had / would (또는 could)
2. were / would
3. were / could (또는 would)
4. were / would (또는 could)
5. used / could (또는 would)
6. If I were a senior developer, I would refactor this code.
7. If we had a QA engineer, we could catch bugs faster.
8. If the response time were faster, the user experience would be much better.
9. If I **were** you, I **would** use a different framework.
10. If the server **were** faster, we would handle more requests.

</details>

---
---

### Lesson 29 — 가정법 과거완료: 과거 사실의 반대

---

#### 1. WHY — 왜 과거 사실의 반대를 말하는가

개발자라면 누구나 한 번쯤 이런 후회를 한다:

- "테스트를 작성했더라면 그 버그가 프로덕션에 안 갔을 텐데..."
- "코드 리뷰를 더 꼼꼼히 했더라면 그 장애를 막았을 텐데..."
- "마이그레이션 전에 백업을 했더라면..."

이것이 바로 **가정법 과거완료(Past Perfect Subjunctive)** 의 존재 이유다. **과거에 일어나지 않은 일** 을 상상하고, **그랬더라면 어떤 결과가 달랐을까** 를 표현한다.

> **한일 교수의 거리감 원리 복습**:
> - 가정법 과거: 현재 현실에서 **한 발짝** 물러남 → 동사 과거형
> - 가정법 과거완료: 과거 현실에서 **두 발짝** 물러남 → 동사 과거완료형
>
> 현실에서 멀수록 동사 형태가 더 "과거"로 밀려난다.

**가정법 과거 vs 가정법 과거완료 비교:**

| 구분 | 시점 | 공식 | 예문 |
|---|---|---|---|
| 가정법 과거 | 현재 반대 | If + 과거형, would + 동사원형 | If I **knew** Java, I **would** apply. |
| 가정법 과거완료 | 과거 반대 | If + had p.p., would have + p.p. | If I **had known** Java, I **would have applied**. |

---

#### 2. CORE — 최소 문장으로 체득

가정법 과거완료의 공식:

```
If + 주어 + had + 과거분사(p.p.) ~, 주어 + would/could/might + have + 과거분사(p.p.) ~
```

**최소 문장 5개:**

| # | 가정법 과거완료 | 과거 사실 |
|---|---|---|
| 1 | If I **had written** tests, the bug **wouldn't have reached** production. | 테스트를 안 썼고, 버그가 프로덕션에 갔다. |
| 2 | If we **had backed up** the data, we **could have restored** it. | 백업을 안 했고, 복구할 수 없었다. |
| 3 | If she **had reviewed** the PR, she **would have caught** the error. | PR 리뷰를 안 했고, 에러를 못 잡았다. |
| 4 | If they **had used** Git, they **wouldn't have lost** the code. | Git을 안 쓰고, 코드를 잃었다. |
| 5 | If I **had studied** harder, I **might have passed** the exam. | 공부를 덜 했고, 시험에 떨어졌다. |

> **체득 포인트**: "had + p.p."는 과거의 과거로 시간을 밀어넣어 "이건 과거에도 일어나지 않았던 일입니다"라는 신호를 보낸다.

---

#### 3. EXPAND — 짧은 문장에서 복잡한 문장으로

**Step 1: 기본 가정**
```
If I had written tests, the bug wouldn't have reached production.
```

**Step 2: 과거 시점 명시**
```
If I had written tests before the last release,
the bug wouldn't have reached production.
```

**Step 3: 결과의 영향 추가**
```
If I had written tests before the last release,
the bug wouldn't have reached production
and caused a three-hour outage.
```

**Step 4: 후회/감정 추가**
```
If I had written tests before the last release,
the bug wouldn't have reached production
and caused a three-hour outage
that affected thousands of users during tax filing season.
```

**Step 5: 교훈 도출**
```
If I had written tests before the last release,
the bug wouldn't have reached production
and caused a three-hour outage
that affected thousands of users during tax filing season.
That experience taught me to never skip unit tests again.
```

---

**I wish + 가정법 — 소망/후회 표현:**

`I wish`는 가정법의 또 다른 트리거다. 현재에 대한 소망이면 과거형, 과거에 대한 후회면 과거완료형을 쓴다.

| 시점 | 구조 | 예문 |
|---|---|---|
| 현재 소망 | I wish + 과거형 | I wish I **knew** how to use Kubernetes. |
| 과거 후회 | I wish + had p.p. | I wish I **had learned** Kubernetes earlier. |
| 미래 소망 | I wish + would | I wish the client **would** stop changing requirements. |

```
I wish I had chosen a better database schema from the beginning.
(처음부터 더 나은 DB 스키마를 선택했더라면 좋았을 텐데.)

I wish we had adopted microservices architecture earlier.
(우리가 마이크로서비스 아키텍처를 더 일찍 도입했더라면 좋았을 텐데.)

I wish I understood the tax code better.
(세법을 더 잘 이해하면 좋을 텐데.) — 현재 소망
```

**as if / as though + 가정법 — "마치 ~인 것처럼":**

```
He talks as if he were the CTO.
(그는 마치 CTO인 것처럼 말한다.) — 실제로는 CTO가 아님

The system behaves as if it had never been updated.
(시스템이 마치 업데이트된 적이 없는 것처럼 작동한다.)

She codes as though she had been programming for decades.
(그녀는 마치 수십 년간 프로그래밍한 것처럼 코딩한다.)
```

**It's time + 가정법 — "~할 때가 되었다":**

```
It's time we upgraded our Java version.
(자바 버전을 업그레이드할 때가 되었다.)

It's time the company invested in better infrastructure.
(회사가 더 나은 인프라에 투자할 때가 되었다.)

It's (high/about) time we migrated to the cloud.
(클라우드로 마이그레이션할 때가 (한참) 되었다.)
```

> **주의**: `It's time` 뒤에 가정법 과거(과거형)를 쓴다. 현재 해야 할 일을 말하지만 아직 안 하고 있으므로 "현실과의 거리"를 표현하는 것이다.

---

#### 4. CODE — 개발자 비유로 재이해

**가정법 과거완료 = git에서 과거 커밋으로 돌아가서 다른 선택을 했다면?**

```bash
# 현실: 프로덕션에 버그가 나갔다
git log
# commit abc123 - "Deploy without tests"  ← 이 시점이 문제

# 가정법 과거완료: 만약 그때 다른 선택을 했다면...
# "If I had written tests at commit abc123,
#  the bug wouldn't have reached production."

# 이것은 마치 git rebase -i로 과거를 수정하는 상상과 같다
# 하지만 현실에서는 이미 push된 커밋을 바꿀 수 없다
# = 가정법 과거완료는 "이미 일어난 일을 되돌릴 수 없음"을 전제한다
```

**I wish = TODO 주석 (이미 놓친 기회)**

```java
// 가정법 과거완료 = 과거의 TODO를 지금 발견
// TODO: Should have added input validation here
// I wish I had added input validation before the release.

// 가정법 과거 = 현재의 불가능한 TODO
// TODO: Need admin access (don't have it)
// I wish I had admin access.
```

**as if = 인터페이스의 구현체가 다를 때**

```java
// "He talks as if he were the CTO."
// CTO 인터페이스를 구현(implement)하지 않았지만
// 마치 구현한 것처럼 행동하는 것

interface CTO {
    void makeStrategicDecisions();
}

class JuniorDeveloper {  // CTO를 implements 하지 않음
    void talk() {
        // as if he were CTO
        makeStrategicDecisions();  // 컴파일 에러! 권한 없음!
    }
}
```

**It's time = deprecated 경고**

```java
// It's time we upgraded our Java version.
// @Deprecated(since = "Java 8", forRemoval = true)
// "이 버전은 더 이상 지원되지 않습니다. 업그레이드할 때가 되었습니다."
```

---

#### 5. DOMAIN — 세무/기술 도메인 실전 예문

**세무 도메인 — 과거 사실의 반대:**

```
If the taxpayer had filed the return on time,
they wouldn't have incurred the late filing penalty.
(납세자가 신고를 제때 했더라면, 가산세가 부과되지 않았을 것이다.)

If we had applied the tax treaty provisions correctly,
the withholding tax would have been much lower.
(조세조약 조항을 정확히 적용했더라면, 원천징수세가 훨씬 낮았을 것이다.)

If the tax office had processed the refund earlier,
the taxpayer's cash flow wouldn't have been affected so severely.
(세무서가 환급을 더 일찍 처리했더라면,
납세자의 현금 흐름이 그렇게 심하게 영향받지 않았을 것이다.)

I wish the client had reported all their freelance income.
Now we need to file an amended return.
(고객이 모든 프리랜서 소득을 보고했더라면 좋았을 텐데.
이제 수정 신고를 해야 한다.)
```

**기술 도메인 — 장애 회고(Post-mortem)에서:**

```
If we had set up proper monitoring alerts,
we would have detected the memory leak within minutes.
(적절한 모니터링 알림을 설정했더라면,
메모리 누수를 몇 분 안에 감지했을 것이다.)

If the deployment pipeline had included a canary release step,
the faulty update wouldn't have affected all users simultaneously.
(배포 파이프라인에 카나리 릴리스 단계가 포함되어 있었더라면,
결함 있는 업데이트가 모든 사용자에게 동시에 영향을 미치지 않았을 것이다.)

If we had implemented circuit breakers in the payment service,
the cascade failure could have been prevented.
(결제 서비스에 서킷 브레이커를 구현했더라면,
연쇄 장애를 방지할 수 있었을 것이다.)

I wish we had invested in load testing before Black Friday.
The system went down during peak traffic.
(블랙 프라이데이 전에 부하 테스트에 투자했더라면 좋았을 텐데.
시스템이 최대 트래픽 시간에 다운되었다.)
```

**장애 회고 보고서 예문 (full paragraph):**

```
Looking back at the incident, if we had implemented proper input validation
on the tax calculation endpoint, the malformed request would not have caused
the service to crash. Furthermore, if our logging system had captured
the full request payload, we could have identified the root cause
within minutes instead of hours. We wish we had prioritized
these improvements during the last sprint.

(사건을 되돌아보면, 세금 계산 엔드포인트에 적절한 입력 유효성 검증을
구현했더라면, 잘못된 요청이 서비스 크래시를 일으키지 않았을 것이다.
또한 로깅 시스템이 전체 요청 페이로드를 캡처했더라면,
몇 시간이 아니라 몇 분 안에 근본 원인을 파악할 수 있었을 것이다.
지난 스프린트에서 이런 개선 사항을 우선시했더라면 좋았을 텐데.)
```

---

#### 6. PRACTICE — 연습 문제

**A. 빈칸 채우기 — 과거 사실의 반대를 가정법 과거완료로 쓰기**

> 과거 사실을 읽고, 가정법 과거완료 문장을 완성하세요.

1. 과거 사실: We didn't write integration tests. The bug went to production.
   가정법: If we _______ _______ integration tests, the bug _______ _______ _______ to production.

2. 과거 사실: The developer didn't back up the database. We lost critical data.
   가정법: If the developer _______ _______ _______ the database, we _______ _______ _______ critical data.

3. 과거 사실: I didn't learn Spring Boot. I couldn't get the job.
   가정법: If I _______ _______ Spring Boot, I _______ _______ _______ the job.

4. 과거 사실: The client didn't declare all income. They received a penalty.
   가정법: If the client _______ _______ all income, they _______ _______ _______ a penalty.

**B. I wish / as if / It's time 연습**

5. 후회: "Spring Batch를 더 일찍 배웠더라면 좋았을 텐데."
   → I wish I _____________________________________________

6. 현재 소망: "우리 팀에 DevOps 엔지니어가 있으면 좋을 텐데."
   → I wish we _____________________________________________

7. "그는 마치 모든 세법을 아는 것처럼 말한다." (실제로는 모르는데)
   → He talks as if he _____________________________________________

8. "클라우드로 마이그레이션할 때가 되었다."
   → It's time we _____________________________________________

**C. 장애 회고 작문 — 다음 상황을 가정법으로 서술하시오**

9. 상황: 금요일 저녁에 배포했다가 장애가 발생했다. 모니터링 알림이 없었고, 2시간 후에야 발견했다.
   → 가정법 과거완료 3문장으로 회고를 작성하세요.

<details>
<summary>정답 보기</summary>

1. had written / wouldn't have gone
2. had backed up / wouldn't have lost
3. had learned / could have gotten
4. had declared / wouldn't have received
5. had learned Spring Batch earlier.
6. had a DevOps engineer on our team.
7. knew all the tax laws. (현재 반대이므로 가정법 과거)
8. migrated to the cloud.
9. (예시 답안)
   If we had not deployed on Friday evening, the outage would not have occurred during the weekend.
   If we had set up monitoring alerts, we would have detected the issue immediately instead of two hours later.
   If we had followed the "no Friday deploy" rule, we could have avoided this entire incident.

</details>

---
---

### Lesson 30 — 비교 표현과 최상급

---

#### 1. WHY — 왜 비교 표현이 중요한가

개발자의 일상은 끊임없는 비교와 선택이다:

- "이 프레임워크가 저것보다 **더 빠르다**"
- "이것이 **가장 효율적인** 솔루션이다"
- "이 API는 저것**만큼 안정적**이다"

기술 의사결정, 성능 벤치마크, 코드 리뷰, 아키텍처 설계 — 모든 곳에서 비교 표현이 쓰인다. 비교 표현을 정확히 구사하면 기술 토론에서 자신의 주장을 논리적으로 펼칠 수 있다.

> **한일 교수**: "비교 표현은 두 가지를 나란히 놓고 차이를 정밀하게 기술하는 도구다. 영어는 형용사의 음절 수에 따라 비교 방법이 달라지는데, 이것은 발음의 효율성 때문이다."

**비교급/최상급 형태 규칙:**

| 음절 수 | 비교급 | 최상급 | 예시 |
|---|---|---|---|
| 1음절 | -er | -est | fast → faster → fastest |
| 1음절 (e로 끝남) | -r | -st | large → larger → largest |
| 1음절 (단모음+단자음) | 자음 겹침 + -er | 자음 겹침 + -est | big → bigger → biggest |
| 2음절 (-y로 끝남) | y → -ier | y → -iest | easy → easier → easiest |
| 2음절 이상 | more + 원급 | most + 원급 | efficient → more efficient → most efficient |
| 불규칙 | 변형 | 변형 | good → better → best |

**자주 쓰이는 불규칙 비교급/최상급:**

| 원급 | 비교급 | 최상급 |
|---|---|---|
| good / well | better | best |
| bad / badly | worse | worst |
| many / much | more | most |
| little | less | least |
| far | farther / further | farthest / furthest |

---

#### 2. CORE — 최소 문장으로 체득

**패턴 1: 비교급 — A is ~er / more ~ than B**

```
This framework is faster than that one.
This solution is more efficient than the previous one.
```

**패턴 2: 최상급 — A is the ~est / the most ~**

```
This is the fastest algorithm.
This is the most efficient solution.
```

**패턴 3: 동등 비교 — A is as ~ as B**

```
This API is as reliable as the old one.
Java is as popular as Python in enterprise development.
```

**패턴 4: 열등 비교 — A is not as ~ as B / A is less ~ than B**

```
This library is not as fast as the native implementation.
This approach is less scalable than microservices.
```

**최소 문장 5개:**

| # | 비교 유형 | 문장 |
|---|---|---|
| 1 | 비교급 | PostgreSQL is **more reliable** than MySQL **for** complex queries. |
| 2 | 최상급 | Redis is **the fastest** in-memory data store. |
| 3 | 동등 | This API is **as stable as** the previous version. |
| 4 | 열등 | The old system is **not as scalable as** the new one. |
| 5 | 불규칙 | The refactored code performs **better than** the original. |

---

#### 3. EXPAND — 짧은 문장에서 복잡한 문장으로

**비교급 확장:**

```
Step 1: Spring Boot is faster than the old framework.
Step 2: Spring Boot is significantly faster than the old framework for REST API development.
Step 3: Spring Boot is significantly faster than the old framework for REST API development,
        especially when it comes to auto-configuration and dependency management.
Step 4: In our benchmarks, Spring Boot proved to be significantly faster than the old framework
        for REST API development, especially when it comes to auto-configuration
        and dependency management, reducing initial setup time by approximately 60%.
```

**최상급 확장:**

```
Step 1: This is the most efficient solution.
Step 2: This is the most efficient solution we have tested so far.
Step 3: This is the most efficient solution we have tested so far
        in terms of both memory usage and response time.
Step 4: Among all the approaches we evaluated during the architecture review,
        this is the most efficient solution we have tested so far
        in terms of both memory usage and response time,
        making it the ideal choice for our high-traffic tax filing service.
```

**비교 표현의 다양한 강조/완화 표현:**

| 강도 | 비교급 수식어 | 예문 |
|---|---|---|
| 매우 강함 | far / much / significantly | This is **far more efficient** than the old approach. |
| 강함 | considerably / substantially | The new API is **considerably faster**. |
| 약간 | slightly / a little / a bit | The latency is **slightly higher** than expected. |
| 같음 | exactly / just | This solution is **just as good as** the other one. |
| 점점 | even / still | The second iteration was **even faster**. |

**the + 비교급, the + 비교급 패턴 (비례 관계):**

```
The more data we process, the longer the batch job takes.
(데이터를 많이 처리할수록, 배치 작업이 더 오래 걸린다.)

The simpler the code, the easier it is to maintain.
(코드가 단순할수록, 유지보수가 쉽다.)

The more tests we write, the fewer bugs we encounter in production.
(테스트를 많이 작성할수록, 프로덕션에서 버그를 적게 만난다.)

The earlier we catch the bug, the cheaper it is to fix.
(버그를 빨리 잡을수록, 수정 비용이 적게 든다.)
```

**비교급을 활용한 고급 표현:**

```
no + 비교급 + than:
This solution is no better than the previous one.
(이 솔루션은 이전 것보다 나을 게 없다. = 똑같이 나쁘다.)

비교급 + and + 비교급 (점점 ~해지다):
The codebase is getting bigger and bigger.
(코드베이스가 점점 커지고 있다.)

The system is becoming more and more complex.
(시스템이 점점 더 복잡해지고 있다.)
```

---

#### 4. CODE — 개발자 비유로 재이해

**비교급 = Comparable 인터페이스의 compareTo()**

```java
// 비교급 = compareTo()로 두 객체를 비교
public class Framework implements Comparable<Framework> {
    private int speed;

    @Override
    public int compareTo(Framework other) {
        return this.speed - other.speed;
        // 양수: this is faster than other (비교급)
        // 0: this is as fast as other (동등 비교)
        // 음수: this is slower than other (열등 비교)
    }
}

// "Spring Boot is faster than Django"
springBoot.compareTo(django) > 0  // true → 비교급
```

**최상급 = Collections.max() / Collections.min()**

```java
// 최상급 = 컬렉션에서 최대/최소 찾기
List<Framework> frameworks = Arrays.asList(spring, django, express, rails);

Framework fastest = Collections.max(frameworks);
// "Spring Boot is the fastest framework."

Framework slowest = Collections.min(frameworks);
// "Rails is the slowest framework in our benchmark."
```

**as ~ as = equals()**

```java
// 동등 비교 = equals()
// "This API is as reliable as the old one."
newApi.getReliability().equals(oldApi.getReliability())  // true
```

**the 비교급, the 비교급 = 정비례 함수**

```java
// "The more data we process, the longer it takes."
// y = f(x) where y increases as x increases
long processingTime = dataSize * timePerRecord;  // 정비례
```

---

#### 5. DOMAIN — 세무/기술 도메인 실전 예문

**성능 벤치마크 보고:**

```
PostgreSQL is approximately 3 times faster than MySQL
for complex JOIN queries involving more than five tables.
(PostgreSQL은 5개 이상의 테이블을 포함하는 복잡한 JOIN 쿼리에서
MySQL보다 약 3배 빠르다.)

The new tax calculation engine is 40% more efficient
than the legacy system in terms of memory consumption.
(새 세금 계산 엔진은 메모리 소비 면에서 레거시 시스템보다 40% 더 효율적이다.)

Among all the caching strategies we tested,
Redis with LRU eviction policy showed the best performance,
handling up to 100,000 requests per second.
(우리가 테스트한 모든 캐싱 전략 중에서,
LRU 제거 정책을 적용한 Redis가 최고 성능을 보여
초당 최대 10만 건의 요청을 처리했다.)
```

**기술 의사결정 문서(ADR)에서:**

```
Option A is simpler than Option B, but Option B is more scalable.
(옵션 A가 옵션 B보다 단순하지만, 옵션 B가 더 확장성이 있다.)

The event-driven approach is significantly more complex to implement
than the synchronous approach, but it provides far better decoupling
between services.
(이벤트 기반 접근 방식은 동기 방식보다 구현이 상당히 복잡하지만,
서비스 간 분리가 훨씬 우수하다.)

Kubernetes is the most widely adopted container orchestration platform,
but it has the steepest learning curve among the alternatives.
(Kubernetes는 가장 널리 채택된 컨테이너 오케스트레이션 플랫폼이지만,
대안들 중 학습 곡선이 가장 가파르다.)
```

**세무 도메인:**

```
The corporate tax rate is lower than the individual income tax rate
for high-income brackets.
(법인세율은 고소득 구간의 개인 소득세율보다 낮다.)

The more deductions a taxpayer claims,
the lower their effective tax rate becomes.
(납세자가 공제를 많이 신청할수록, 실효 세율이 낮아진다.)

Among all the tax filing methods,
electronic filing is the fastest and most accurate.
(모든 세금 신고 방법 중에서, 전자 신고가 가장 빠르고 정확하다.)

The penalty for late filing is not as severe as
the penalty for tax evasion.
(지연 신고 가산세는 탈세에 대한 벌금만큼 심하지 않다.)
```

**세무 + 기술 융합:**

```
The new automated tax calculation feature processes returns
three times faster than the manual review process,
while maintaining the same level of accuracy.
(새로운 자동 세금 계산 기능은 수동 검토 프로세스보다
3배 빠르게 신고서를 처리하면서도 동일한 수준의 정확도를 유지한다.)

The more complex the tax scenario becomes,
the more important it is to have comprehensive test coverage.
(세무 시나리오가 복잡해질수록,
포괄적인 테스트 커버리지를 갖추는 것이 더 중요해진다.)
```

---

#### 6. PRACTICE — 연습 문제

**A. 빈칸 채우기**

1. PostgreSQL is _______ (reliable) _______ MySQL for transactional workloads.
2. This is the _______ (efficient) algorithm we have tested.
3. The new system is 5 times _______ (fast) _______ the old one.
4. The _______ (early) we detect bugs, the _______ (cheap) they are to fix.
5. This API is _______ _______ _______ (not / stable / as) the previous version.

**B. 기술 비교 작문 — 다음을 영어로 표현하세요**

6. "Spring Boot는 Django보다 엔터프라이즈 환경에서 더 널리 사용된다."
   → _____________________________________________

7. "Redis는 우리가 테스트한 캐시 솔루션 중 가장 빠르다."
   → _____________________________________________

8. "새 API의 응답 시간은 이전 버전만큼 빠르다."
   → _____________________________________________

9. "코드가 복잡해질수록 유지보수가 어려워진다."
   → _____________________________________________

**C. 비교 표현을 사용한 기술 의사결정 문서 작성**

10. 당신은 세금 계산 서비스의 DB를 MySQL에서 PostgreSQL로 마이그레이션하자고 제안합니다. 비교급, 최상급, 동등 비교를 각각 1개 이상 사용하여 3-5문장의 제안서를 영어로 작성하세요.

<details>
<summary>정답 보기</summary>

1. more reliable than
2. most efficient
3. faster than
4. earlier / cheaper
5. not as stable as
6. Spring Boot is more widely used than Django in enterprise environments.
7. Redis is the fastest cache solution we have tested.
8. The new API's response time is as fast as the previous version.
9. The more complex the code becomes, the harder it is to maintain.
10. (예시 답안)
    I propose migrating our tax calculation database from MySQL to PostgreSQL.
    PostgreSQL is significantly more reliable than MySQL for complex transactional queries.
    It handles concurrent connections better than MySQL, which is crucial during tax filing season.
    In our benchmarks, PostgreSQL performed as well as MySQL for simple queries
    but was far superior for the complex JOIN operations our tax calculations require.
    Among all the relational databases we evaluated,
    PostgreSQL offers the best balance of performance, reliability, and cost.

</details>

---
---

## Week 12: 영어다운 표현 — 자연스러움의 비밀

---

### Lesson 31 — 구동사(Phrasal Verbs): 쉬운 동사 + 전치사 = 고급 표현

---

#### 1. WHY — 왜 원어민은 어려운 단어 대신 구동사를 쓰는가

한국인 영어 학습자들은 종종 이런 의문을 품는다:

- "investigate"라는 단어가 있는데 왜 "look into"를 쓰는가?
- "discover"가 있는데 왜 "find out"을 쓰는가?
- "establish"가 있는데 왜 "set up"을 쓰는가?

> **한일 교수**: "원어민은 라틴어 계통의 어려운 단어보다 게르만어 계통의 쉬운 동사 + 전치사 조합을 일상에서 압도적으로 많이 쓴다. 이것이 구동사(phrasal verb)다. 구동사를 모르면 원어민의 말을 50%밖에 이해할 수 없다."

**왜 구동사가 더 자연스러운가?**

영어에는 두 가지 어휘 계층이 있다:

| 계층 | 기원 | 특징 | 예 |
|---|---|---|---|
| 일상어 (구동사) | 게르만어 (Anglo-Saxon) | 짧고 직관적, 대화체 | figure out, set up, come up with |
| 격식어 (라틴어 계열) | 라틴어/프랑스어 | 길고 학술적, 문서체 | comprehend, establish, devise |

원어민은 이메일, 슬랙, 미팅에서 구동사를 사용하고, 공식 문서나 학술 논문에서 라틴어 계열 단어를 사용한다. 개발자 세계에서도 마찬가지다:

```
격식체 (문서): We need to investigate the root cause.
자연스러운 대화: We need to look into the root cause.

격식체 (문서): I encountered an unexpected error.
자연스러운 대화: I ran into a weird error.

격식체 (문서): Please establish the development environment.
자연스러운 대화: Please set up the dev environment.
```

---

#### 2. CORE — 개발자 필수 구동사 30선

**구동사의 구조:**

```
동사 + 전치사/부사 = 새로운 의미
(기본 동사)  (방향/상태)  (확장된 의미)

look + up = 찾아보다 (investigate)
set + up = 설정하다 (configure/establish)
figure + out = 알아내다 (understand/solve)
```

**개발자가 반드시 알아야 할 구동사 30개:**

| # | 구동사 | 의미 | 격식 동의어 | 예문 |
|---|---|---|---|---|
| 1 | **set up** | 설정하다 | configure, establish | Let me **set up** the dev environment. |
| 2 | **figure out** | 알아내다, 해결하다 | determine, solve | We need to **figure out** why it crashed. |
| 3 | **look into** | 조사하다 | investigate | I'll **look into** the performance issue. |
| 4 | **come up with** | 생각해내다 | devise, conceive | She **came up with** a brilliant solution. |
| 5 | **run into** | 우연히 만나다/부딪히다 | encounter | I **ran into** a weird error. |
| 6 | **turn out** | ~로 판명되다 | prove to be | It **turned out** to be a configuration issue. |
| 7 | **break down** | 고장나다/분해하다 | malfunction, decompose | The server **broke down** at midnight. |
| 8 | **roll out** | 출시/배포하다 | deploy, release | We'll **roll out** the update next week. |
| 9 | **roll back** | 되돌리다 | revert | We had to **roll back** the deployment. |
| 10 | **spin up** | (서버/인스턴스를) 시작하다 | launch, initialize | **Spin up** a new EC2 instance. |
| 11 | **shut down** | 종료하다 | terminate | **Shut down** the staging server. |
| 12 | **log in / log out** | 로그인/로그아웃하다 | authenticate / deauthenticate | Users can't **log in** to the portal. |
| 13 | **back up** | 백업하다 | create a backup | Always **back up** the database before migration. |
| 14 | **clean up** | 정리하다 | refactor, organize | We need to **clean up** this legacy code. |
| 15 | **speed up** | 빠르게 하다 | accelerate, optimize | How can we **speed up** the query? |
| 16 | **slow down** | 느리게 하다 | decelerate | The memory leak is **slowing down** the server. |
| 17 | **point out** | 지적하다 | indicate, highlight | She **pointed out** a critical bug in the review. |
| 18 | **carry out** | 수행하다 | execute, perform | We need to **carry out** the migration carefully. |
| 19 | **opt in / opt out** | 참여/탈퇴하다 | subscribe / unsubscribe | Users can **opt out** of email notifications. |
| 20 | **plug in** | 연결하다/플러그인 추가 | integrate, connect | **Plug in** the monitoring tool. |
| 21 | **check out** | 확인하다/체크아웃 | examine, review | **Check out** this new library. |
| 22 | **come across** | 우연히 발견하다 | discover | I **came across** a useful Stack Overflow answer. |
| 23 | **end up** | 결국 ~하게 되다 | eventually result in | We **ended up** rewriting the entire module. |
| 24 | **keep up with** | ~를 따라가다 | maintain pace with | It's hard to **keep up with** all the new frameworks. |
| 25 | **cut down on** | 줄이다 | reduce | We need to **cut down on** unnecessary API calls. |
| 26 | **go through** | 검토하다/겪다 | review, experience | Let me **go through** the error logs. |
| 27 | **put off** | 미루다 | postpone, defer | Don't **put off** writing documentation. |
| 28 | **take on** | 맡다/떠맡다 | assume, undertake | She **took on** the most challenging ticket. |
| 29 | **bring up** | 꺼내다/언급하다 | mention, raise | He **brought up** an important concern. |
| 30 | **wrap up** | 마무리하다 | conclude, finalize | Let's **wrap up** the sprint review. |

---

#### 3. EXPAND — 구동사를 문장 안에서 확장하기

**set up:**

```
기본: Set up the environment.
확장: Could you set up the local development environment
      with Docker and the latest database schema?
고급: Before we can start testing the new tax calculation module,
      we need to set up a staging environment
      that mirrors the production configuration as closely as possible.
```

**figure out:**

```
기본: I'll figure it out.
확장: I'm trying to figure out why the batch job is failing
      every night at 3 AM.
고급: After spending three hours analyzing the logs,
      we finally figured out that the root cause was
      a race condition in the concurrent tax calculation threads
      that only manifested under heavy load.
```

**run into:**

```
기본: I ran into a bug.
확장: I ran into a weird null pointer exception
      in the payment processing module.
고급: While migrating the tax filing service to the new API version,
      we ran into an unexpected compatibility issue
      that required us to rewrite the entire request validation layer.
```

**come up with:**

```
기본: We came up with a solution.
확장: The team came up with an elegant solution
      to the database scaling problem.
고급: During the architecture review meeting,
      our lead developer came up with an innovative caching strategy
      that reduced the average response time for tax calculations
      from 2 seconds to under 200 milliseconds.
```

**roll out / roll back:**

```
기본: We rolled out the update. / We rolled it back.
확장: We rolled out the new tax calculation engine last Tuesday,
      but had to roll it back within two hours
      due to a critical bug in the income classification logic.
고급: After carefully rolling out the v2.0 update to 5% of users
      through a canary deployment strategy,
      we observed a 15% increase in error rates
      and made the decision to roll back to v1.9
      until the team could identify and resolve the underlying issues.
```

---

**구동사의 분리 가능/불가능 규칙:**

구동사에는 **분리형(separable)** 과 **비분리형(inseparable)** 이 있다.

| 유형 | 규칙 | 예 |
|---|---|---|
| 분리형 | 목적어가 명사면 사이에 넣을 수도, 뒤에 놓을 수도 있음 | **Set** the environment **up** / **Set up** the environment |
| 분리형 + 대명사 | 목적어가 대명사면 반드시 사이에 넣음 | **Set it up** (O) / ~~Set up it~~ (X) |
| 비분리형 | 항상 붙어 있어야 함 | **Look into** the issue (O) / ~~Look the issue into~~ (X) |

```
분리형:
✅ Turn the server off.  / Turn off the server.
✅ Turn it off.          / ❌ Turn off it.

✅ Set the environment up. / Set up the environment.
✅ Set it up.              / ❌ Set up it.

비분리형:
✅ Look into the issue.  / ❌ Look the issue into.
✅ Run into a problem.   / ❌ Run a problem into.
✅ Come up with a plan.  / ❌ Come a plan up with.
```

---

#### 4. CODE — 개발자 비유로 재이해

**구동사 = 메서드 체이닝 / 조합 패턴**

```java
// 구동사는 기본 동사(클래스)에 전치사(메서드)를 체이닝하여
// 새로운 의미(기능)를 만들어내는 패턴이다.

class Look {
    void up()   { /* 찾아보다 (look up) */ }
    void into() { /* 조사하다 (look into) */ }
    void out()  { /* 조심하다 (look out) */ }
    void over() { /* 검토하다 (look over) */ }
}

class Set {
    void up()   { /* 설정하다 (set up) */ }
    void off()  { /* 출발시키다 (set off) */ }
    void back() { /* 지연시키다 (set back) */ }
}

class Come {
    void up()       { /* 다가오다 (come up) */ }
    void upWith()   { /* 생각해내다 (come up with) */ }
    void across()   { /* 우연히 발견하다 (come across) */ }
}

// 같은 기본 동사(클래스)라도 전치사(메서드)에 따라 완전히 다른 동작을 한다
// 이것은 마치 같은 클래스의 오버로딩된 메서드들과 같다
```

**분리형 구동사 = 인자 위치가 유연한 함수**

```java
// 분리형: 인자 위치 유연 (명사일 때)
server.turnOff();        // Turn off the server
server.turn(Off);        // Turn the server off

// 분리형: 대명사는 반드시 사이에 (필수 인라인 파라미터)
it.turn(Off);            // Turn it off ✅
// turnOff(it);          // Turn off it ❌ 컴파일 에러!

// 비분리형: 인자 위치 고정 (메서드 뒤에만)
issue.lookInto();        // Look into the issue ✅
// issue.look(Into);     // Look the issue into ❌ 컴파일 에러!
```

---

#### 5. DOMAIN — 세무/기술 도메인 실전 예문

**슬랙/이메일에서 흔히 쓰는 구동사:**

```
"Can you look into why the tax calculation is returning wrong numbers?"
(세금 계산이 왜 잘못된 숫자를 반환하는지 조사해줄 수 있어?)

"I ran into a weird issue while testing the VAT module."
(부가세 모듈 테스트 중에 이상한 문제를 만났어.)

"Let me figure out what's causing the timeout."
(타임아웃의 원인이 뭔지 알아볼게.)

"We need to set up a new staging environment for the tax API."
(세금 API를 위한 새 스테이징 환경을 설정해야 해.)

"I came across a potential security vulnerability in the authentication flow."
(인증 플로우에서 잠재적 보안 취약점을 발견했어.)

"The deployment went wrong, so we had to roll it back."
(배포가 잘못돼서 롤백해야 했어.)

"Let's wrap up the sprint review and go through the action items."
(스프린트 리뷰를 마무리하고 액션 아이템을 검토하자.)
```

**회의에서 쓰는 구동사:**

```
"I'd like to bring up a concern about the deadline."
(마감일에 대한 우려 사항을 말씀드리고 싶습니다.)

"We ended up rewriting the tax calculation logic from scratch."
(결국 세금 계산 로직을 처음부터 다시 작성하게 되었습니다.)

"It turned out that the bug was caused by a timezone conversion error."
(그 버그는 시간대 변환 오류에 의해 발생한 것으로 판명되었습니다.)

"She came up with an innovative approach to handle the peak load during filing season."
(그녀가 신고 시즌 최대 부하를 처리하기 위한 혁신적인 접근법을 생각해냈습니다.)

"Don't put off writing the API documentation. It'll be harder to do later."
(API 문서 작성을 미루지 마세요. 나중에 하면 더 어렵습니다.)
```

**세무 도메인 특화:**

```
"The tax authority will look into the discrepancy in the filed returns."
(세무 당국이 제출된 신고서의 불일치를 조사할 것입니다.)

"We need to figure out whether this income falls under
the business category or the employment category."
(이 소득이 사업소득에 해당하는지 근로소득에 해당하는지 파악해야 합니다.)

"The client opted out of the automatic filing service."
(고객이 자동 신고 서비스에서 탈퇴했습니다.)

"We should speed up the refund processing to improve customer satisfaction."
(고객 만족도를 높이기 위해 환급 처리 속도를 높여야 합니다.)

"The amended tax law cut down on the number of eligible deductions."
(개정된 세법이 적격 공제 항목 수를 줄였습니다.)
```

---

#### 6. PRACTICE — 연습 문제

**A. 구동사로 바꾸기 — 격식 동사를 구동사로 전환**

| # | 격식 문장 | 구동사 전환 |
|---|---|---|
| 1 | We need to investigate the issue. | We need to _______ _______ the issue. |
| 2 | Please configure the server. | Please _______ _______ the server. |
| 3 | I encountered an error. | I _______ _______ an error. |
| 4 | She devised a solution. | She _______ _______ _______ a solution. |
| 5 | We must revert the deployment. | We must _______ _______ the deployment. |
| 6 | We need to reduce API calls. | We need to _______ _______ _______ API calls. |
| 7 | Let's conclude the meeting. | Let's _______ _______ the meeting. |
| 8 | Don't postpone writing tests. | Don't _______ _______ writing tests. |

**B. 대명사 위치 연습 — 올바른 문장 고르기**

9. a) Set up it. b) Set it up.
10. a) Figure out it. b) Figure it out.
11. a) Look into it. b) Look it into.
12. a) Turn off it. b) Turn it off.
13. a) Come up with it. b) Come it up with.

**C. 구동사를 사용한 슬랙 메시지 작성**

14. 상황: 배포 후 버그를 발견했고, 원인을 조사 중이며, 필요하면 롤백하겠다는 메시지를 슬랙에 쓰세요. 최소 3개의 구동사를 사용하세요.

15. 상황: 세금 계산 모듈의 성능 문제를 발견했고, 원인을 파악했으며, 해결책을 생각해냈다는 내용의 업데이트 메시지를 쓰세요. 최소 3개의 구동사를 사용하세요.

<details>
<summary>정답 보기</summary>

**A.**
1. look into
2. set up
3. ran into
4. came up with
5. roll back
6. cut down on
7. wrap up
8. put off

**B.**
9. b) Set it up.
10. b) Figure it out.
11. a) Look into it. (비분리형이므로 붙어 있어야 함)
12. b) Turn it off.
13. a) Come up with it. (비분리형이므로 붙어 있어야 함)

**C.**
14. (예시)
"Hey team, we ran into a critical bug after rolling out the latest update.
I'm looking into the root cause right now.
If I can't figure it out within the next hour,
we'll roll it back to the previous version. Will keep you posted."

15. (예시)
"Quick update on the tax calculation performance issue:
I went through the profiler results and figured out that
the bottleneck was in the deduction validation step.
I came up with a caching strategy that should speed up
the calculation by at least 50%. Setting up a POC now."

</details>

---
---

### Lesson 32 — It/There 구문: 영어의 자리 예약 패턴

---

#### 1. WHY — 영어는 왜 가주어/가목적어를 쓰는가

한국어는 주어를 생략해도 자연스럽다:

```
한국어: "깨끗한 코드를 쓰는 것이 중요하다."  (주어 생략 가능)
한국어: "중요하다, 깨끗한 코드를 쓰는 것."   (도치도 자연스러움)
```

하지만 영어는 **주어 자리가 반드시 채워져야 하는 언어** 다. 영어 문장의 기본 구조는 `S + V + O`이며, 주어 자리가 비어 있으면 문법적으로 불완전하다.

> **한일 교수**: "영어는 '자리'가 곧 '의미'인 언어다. 주어 자리에 뭔가가 있어야 '아, 이제 동사가 나오겠구나'라고 듣는 사람이 예측할 수 있다. 그래서 진짜 주어가 길거나 복잡하면, 일단 it이나 there로 자리를 예약(placeholder)해 놓고 진짜 주어는 뒤로 보낸다."

**왜 placeholder가 필요한가?**

```
❌ 어색: To write clean code is important.
   → 주어(To write clean code)가 너무 길어서 동사(is)까지 오래 걸림
   → 듣는 사람이 "도대체 주어가 언제 끝나지?" 하고 혼란

✅ 자연: It is important to write clean code.
   → it이 주어 자리를 예약해 놓고, 듣는 사람은 바로 "is important"를 들음
   → 뒤에 to write clean code가 와서 무엇이 중요한지 보충
```

이것은 마치 **레스토랑 예약** 과 같다. 먼저 자리를 잡아놓고(it), 나중에 진짜 손님(to write clean code)이 도착하는 것이다.

---

#### 2. CORE — 가주어 it과 There is/are의 차이

**패턴 1: 가주어 It — "~하는 것은 ...하다"**

```
It is + 형용사 + to 부정사
It is important to write clean code.
It is difficult to maintain legacy systems.
It is necessary to validate user input.
```

```
It is + 형용사 + that절
It is clear that the system needs an upgrade.
It is obvious that we should add more tests.
It is unlikely that the bug will fix itself.
```

**패턴 2: There is/are — "~이 있다/존재한다"**

```
There is + 단수 명사
There is a bug in the payment module.
There is a meeting at 3 PM.

There are + 복수 명사
There are several issues in the pull request.
There are many ways to solve this problem.
```

**두 패턴의 차이:**

| 구문 | 용도 | 초점 |
|---|---|---|
| **It is ~** | 판단/평가 (중요하다, 어렵다, 필요하다) | 사물의 **성질/특성** |
| **There is/are ~** | 존재/발생 (있다, 존재한다) | 사물의 **존재 여부** |

```
It is important to fix this bug.      (이 버그를 고치는 것은 중요하다 → 평가)
There is a critical bug in the code.  (코드에 치명적인 버그가 있다 → 존재)
```

---

#### 3. EXPAND — 짧은 문장에서 복잡한 문장으로

**가주어 It 확장:**

```
Step 1: It is important to test.
Step 2: It is important to test your code before deploying.
Step 3: It is critically important to test your code thoroughly
        before deploying to production.
Step 4: It is critically important to test your code thoroughly
        before deploying to production,
        especially when the changes affect the tax calculation logic.
Step 5: It is critically important to test your code thoroughly
        before deploying to production,
        especially when the changes affect the tax calculation logic
        that processes millions of transactions during filing season.
```

**가주어 It의 다양한 패턴:**

```
It is + 형용사 + to V:
It is easy to write code. It is hard to write good code.
It is impossible to predict every edge case.
It is crucial to handle exceptions properly.

It is + 형용사 + that S + V:
It is clear that we need more engineers.
It is surprising that no one noticed the bug.
It is well-known that Java is widely used in enterprise.

It is + 형용사 + for + 사람 + to V:
It is difficult for junior developers to understand legacy code.
It is important for the team to follow coding standards.
It is essential for every developer to write tests.

It + takes + 시간 + to V:
It takes about two hours to run the full test suite.
It took us three days to identify the root cause.
It will take approximately one week to complete the migration.

It + seems/appears + that S + V:
It seems that the server is overloaded.
It appears that the issue was caused by a memory leak.
It turned out that the configuration was incorrect.

It + is said/believed/reported + that S + V:
It is said that microservices solve all problems. (They don't.)
It is believed that the new tax law will reduce filing complexity.
It is reported that 70% of enterprises use Java.
```

**There is/are 확장:**

```
Step 1: There is a bug.
Step 2: There is a bug in the payment module.
Step 3: There is a critical bug in the payment module
        that causes incorrect tax calculations.
Step 4: There is a critical bug in the payment module
        that causes incorrect tax calculations
        for users who have multiple income sources.
Step 5: There is a critical bug in the payment module
        that causes incorrect tax calculations
        for users who have multiple income sources
        and claim deductions for home office expenses.
```

**There is/are의 다양한 시제:**

```
현재: There is a problem with the API.
과거: There was an outage last night.
미래: There will be a maintenance window this weekend.
현재완료: There have been several complaints about performance.
진행형: There are currently three engineers working on the fix.

부정: There is no way to recover the lost data.
      There aren't any available slots for the meeting.

의문: Is there a backup of the database?
      Are there any known issues with this version?
```

---

**가목적어 It — "~하는 것을 ...하게 만들다/여기다"**

가주어뿐만 아니라 **가목적어** 로도 it이 쓰인다:

```
구조: S + V + it + 형용사/명사 + to V / that절

The new API makes it easy to integrate third-party services.
(새 API는 서드파티 서비스를 통합하는 것을 쉽게 만든다.)

I find it difficult to work with undocumented code.
(문서화되지 않은 코드로 작업하는 것이 어렵다고 생각한다.)

The team considers it essential to conduct code reviews.
(팀은 코드 리뷰를 수행하는 것이 필수적이라고 여긴다.)

TypeScript makes it possible to catch type errors at compile time.
(TypeScript는 컴파일 시점에 타입 에러를 잡는 것을 가능하게 한다.)
```

> **가목적어가 필요한 이유**: "The new API makes to integrate third-party services easy."는 목적어(to integrate...)와 보어(easy)의 관계가 불분명하다. it을 넣으면 구조가 명확해진다.

---

#### 4. CODE — 개발자 비유로 재이해

**가주어 It = 변수 선언 후 나중에 값 할당 (Lazy Initialization)**

```java
// 가주어 it = 먼저 변수를 선언(placeholder)하고 나중에 진짜 값을 할당
// It is important to write clean code.

Object it;  // placeholder 선언 (= 가주어 it)
it = "to write clean code";  // 나중에 진짜 값(주어) 할당

// 결과: it(= to write clean code) is important
```

```java
// 프로그래밍에서의 Lazy Initialization:
private Connection connection;  // placeholder

public Connection getConnection() {
    if (connection == null) {
        connection = createNewConnection();  // 나중에 실제 객체 생성
    }
    return connection;
}

// 영어의 가주어 it도 같은 패턴:
// 먼저 it(placeholder)을 주어 자리에 놓고
// 진짜 주어(to부정사/that절)는 뒤에서 구체화
```

**There is/are = Collection.isEmpty() 확인**

```java
// There is/are = 컬렉션에 요소가 존재하는지 확인

// "There is a bug in the payment module."
List<Bug> bugs = paymentModule.getBugs();
if (!bugs.isEmpty()) {  // There is a bug!
    handleBug(bugs.get(0));
}

// "There are several issues in the PR."
List<Issue> issues = pullRequest.getIssues();
if (issues.size() > 1) {  // There are several issues!
    reviewAll(issues);
}

// "There is no way to recover the data."
if (recoveryOptions.isEmpty()) {  // There is no way!
    throw new UnrecoverableException("Data is lost");
}
```

**가목적어 It = Builder 패턴의 중간 객체**

```java
// "The new API makes it easy to integrate third-party services."
// make(it).easy().to(integrate(thirdPartyServices))

// Builder 패턴:
ApiResult result = newApi.make()    // make
    .target(it)                     // it (placeholder)
    .quality(Easy.class)            // easy
    .action(() -> integrate(thirdPartyServices));  // to integrate
```

---

#### 5. DOMAIN — 세무/기술 도메인 실전 예문

**기술 문서에서 가주어 It:**

```
It is essential to validate all user inputs
before processing tax returns.
(세금 신고서를 처리하기 전에 모든 사용자 입력을 검증하는 것이 필수적이다.)

It is recommended to use parameterized queries
to prevent SQL injection attacks.
(SQL 인젝션 공격을 방지하기 위해 매개변수화된 쿼리를 사용하는 것이 권장된다.)

It takes approximately 30 seconds to process a single tax return
through our calculation engine.
(우리 계산 엔진을 통해 세금 신고서 하나를 처리하는 데 약 30초가 걸린다.)

It is well-documented that the legacy tax calculation system
has significant performance limitations during peak filing periods.
(레거시 세금 계산 시스템이 최대 신고 기간에 심각한 성능 제한이 있다는 것은
잘 문서화되어 있다.)

It appears that the discrepancy in the tax calculation
is caused by a rounding error in the decimal handling logic.
(세금 계산의 불일치는 소수점 처리 로직의 반올림 오류에 의해 발생한 것으로 보인다.)
```

**기술 문서에서 There is/are:**

```
There is a known issue with the date formatting
in the tax filing report generator.
(세금 신고 보고서 생성기의 날짜 포맷에 알려진 이슈가 있습니다.)

There are currently 15 open tickets
related to the tax calculation module.
(세금 계산 모듈과 관련하여 현재 15개의 열린 티켓이 있습니다.)

There will be a scheduled maintenance window
this Saturday from 2 AM to 6 AM KST.
(이번 토요일 KST 오전 2시부터 6시까지 예정된 점검 시간이 있습니다.)

There have been multiple reports of timeout errors
during the peak tax filing period.
(세금 신고 최대 기간 동안 타임아웃 오류에 대한 여러 보고가 있었습니다.)

Is there a rollback plan in case the migration fails?
(마이그레이션이 실패할 경우 롤백 계획이 있습니까?)
```

**세무 도메인에서:**

```
It is the taxpayer's responsibility to report all sources of income.
(모든 소득 원천을 보고하는 것은 납세자의 책임이다.)

It is important for businesses to keep accurate financial records
for at least five years.
(기업이 최소 5년간 정확한 재무 기록을 보관하는 것이 중요하다.)

There is a significant difference between tax avoidance and tax evasion.
(절세와 탈세 사이에는 중요한 차이가 있다.)

There are various deductions available
for taxpayers who work from home.
(재택근무를 하는 납세자에게 이용 가능한 다양한 공제가 있다.)

It is estimated that approximately 25% of taxpayers
file their returns in the last week before the deadline.
(약 25%의 납세자가 마감일 직전 마지막 주에 신고서를 제출하는 것으로 추정된다.)
```

**이메일/보고서에서 자주 쓰이는 패턴:**

```
It has come to my attention that several API endpoints
are not handling errors gracefully.
(여러 API 엔드포인트가 에러를 적절히 처리하지 못하고 있다는 것이 제 주의를 끌었습니다.)

It would be appreciated if you could review the PR by end of day.
(오늘 안으로 PR을 리뷰해주시면 감사하겠습니다.)

There seems to be a misunderstanding about the requirements.
(요구사항에 대한 오해가 있는 것 같습니다.)

It is worth noting that the performance improved by 40%
after the database index optimization.
(데이터베이스 인덱스 최적화 후 성능이 40% 향상되었다는 점은 주목할 만합니다.)

It goes without saying that security should be our top priority.
(보안이 최우선 사항이어야 한다는 것은 말할 필요도 없습니다.)
```

---

#### 6. PRACTICE — 연습 문제

**A. 가주어 It으로 문장 바꾸기**

원래 문장을 가주어 It 구문으로 바꾸세요.

1. To deploy without testing is dangerous.
   → It is _____________________________________________

2. That the server crashed during peak hours is unfortunate.
   → It is _____________________________________________

3. To learn a new programming language takes time and practice.
   → It _____________________________________________

4. For a junior developer to understand microservices architecture is challenging.
   → It is _____________________________________________

**B. There is/are로 문장 만들기**

다음 상황을 There is/are 구문으로 표현하세요.

5. 코드에 심각한 보안 취약점이 존재한다.
   → _____________________________________________

6. 현재 이 이슈를 해결하기 위해 작업 중인 엔지니어가 3명 있다.
   → _____________________________________________

7. 마이그레이션이 실패할 경우를 대비한 롤백 계획이 있나요?
   → _____________________________________________

8. 지난 달에 이 API에 대한 다수의 타임아웃 보고가 있었다.
   → _____________________________________________

**C. 가목적어 It 연습**

9. TypeScript는 컴파일 시점에 타입 에러를 잡는 것을 가능하게 한다.
   → TypeScript makes _____________________________________________

10. 나는 문서화 없이 레거시 코드를 유지보수하는 것이 어렵다고 생각한다.
    → I find _____________________________________________

**D. 종합 작문**

11. 다음 상황을 영어로 작성하세요. 가주어 It과 There is/are를 각각 2회 이상 사용하세요.

    상황: 당신은 팀 리드입니다. 다가오는 세금 신고 시즌을 앞두고 시스템 준비 상태에 대한 이메일을 작성합니다. 현재 알려진 이슈 3개가 있고, 성능 테스트가 중요하며, 마감일까지 2주 남았습니다.

<details>
<summary>정답 보기</summary>

**A.**
1. It is dangerous to deploy without testing.
2. It is unfortunate that the server crashed during peak hours.
3. It takes time and practice to learn a new programming language.
4. It is challenging for a junior developer to understand microservices architecture.

**B.**
5. There is a critical security vulnerability in the code.
6. There are currently three engineers working on resolving this issue.
7. Is there a rollback plan in case the migration fails?
8. There were multiple timeout reports for this API last month. (또는: There have been multiple timeout reports...)

**C.**
9. TypeScript makes it possible to catch type errors at compile time.
10. I find it difficult to maintain legacy code without documentation.

**D.**
11. (예시 답안)

Hi team,

As we approach the upcoming tax filing season, I'd like to share a quick update on our system readiness.

There are currently three known issues that we need to address before the filing period begins. First, there is a performance bottleneck in the tax calculation endpoint that affects response times under heavy load. Second, there is a date formatting bug in the report generator. Third, there are intermittent timeout errors in the API gateway.

It is critically important that we complete performance testing by the end of next week. It takes approximately two hours to run the full load test suite, so please plan accordingly. It is also essential for every team member to review the incident response playbook before the season starts.

There are only two weeks remaining until the deadline. It would be appreciated if each team lead could provide a status update by Friday.

Best regards

</details>

---
---

## Phase 5 요약: 핵심 원리 복습

| 문법 도구 | 핵심 원리 | 개발자 비유 |
|---|---|---|
| **가정법 과거** | 현실에서 한 발짝 물러남 → 과거형 | unreachable code branch |
| **가정법 과거완료** | 과거에서 두 발짝 물러남 → 과거완료형 | git 과거 커밋 수정 상상 |
| **I wish / as if / It's time** | 가정법의 확장 트리거 | TODO 주석 / deprecated 경고 |
| **비교급/최상급** | 두 사물의 차이를 정밀하게 기술 | Comparable.compareTo() / Collections.max() |
| **구동사** | 쉬운 동사 + 전치사 = 자연스러운 표현 | 메서드 체이닝 패턴 |
| **가주어 It** | 주어 자리를 먼저 예약, 진짜 주어는 뒤로 | Lazy Initialization |
| **There is/are** | 존재/발생을 알림 | Collection.isEmpty() 확인 |

> **다음 Phase 예고**: Phase 6에서는 **고급 문장 구조** 를 다룹니다. 분사구문, 도치, 강조 구문 등 영어 글쓰기의 세련됨을 결정하는 문법 도구를 배우게 됩니다.
