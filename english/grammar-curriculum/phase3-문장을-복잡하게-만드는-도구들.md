# Phase 3: 문장을 복잡하게 만드는 도구들 (Week 6-8)

> **단문을 복문으로 업그레이드하는 패턴들**
>
> Phase 1-2에서 우리는 영어 문장의 뼈대(주어+동사+목적어)와 시제를 배웠다.
> 이제 그 단순한 문장들을 **조합하고, 확장하고, 중첩**하는 도구들을 익힌다.
> 프로그래밍으로 치면, 단일 함수 호출에서 **미들웨어 체인, JOIN 쿼리, 조건 분기**로 넘어가는 단계다.

---

## Week 6: 조동사 — 동사에 모드를 추가하다

> 조동사(Modal Verbs)는 동사 앞에 붙어서 **말하는 사람의 태도**를 표현한다.
> 능력, 가능성, 의무, 허가, 추측 — 이 모든 것이 조동사 하나로 결정된다.

---

### Lesson 16 — 조동사 = 동사의 미들웨어

#### 1. WHY — 조동사는 왜 존재하는가?

영어에서 동사는 **"무엇을 하는가"**를 알려준다. 하지만 현실의 대화에서는 단순히 행동만 전달하는 게 아니다.

- "할 수 있다" (능력)
- "할 것이다" (의지)
- "해야 한다" (의무)
- "할지도 모른다" (가능성)

한국어에서는 이런 뉘앙스를 **어미 변화**로 처리한다: "한다 / 할 수 있다 / 해야 한다 / 할지도 모른다."
영어에서는 **동사 앞에 별도의 단어(조동사)**를 놓아서 이 의미를 추가한다.

```
동사만:       I deploy this.          (나는 이것을 배포한다)
조동사 추가:  I can deploy this.      (나는 이것을 배포할 수 있다)
              I must deploy this.     (나는 이것을 배포해야 한다)
              I might deploy this.    (나는 이것을 배포할지도 모른다)
```

**핵심 원리**: 동사는 **행동(action)**을 담당하고, 조동사는 그 행동에 대한 **화자의 판단(mode)**을 담당한다.

이것이 바로 "모달(modal)"이라는 이름의 유래다. **Mode(모드)**를 설정하는 동사라는 뜻이다.

#### 2. CORE — 핵심 개념을 가장 짧은 문장으로 체득

조동사의 기본 공식:

```
주어 + 조동사 + 동사원형
```

**절대 규칙**: 조동사 뒤에는 반드시 **동사원형**이 온다. 3인칭 단수 -s도 붙지 않는다.

```
(X) He cans fix it.
(X) She must fixes it.
(O) He can fix it.
(O) She must fix it.
```

각 조동사의 **핵심 한 줄 정의**:

| 조동사 | 핵심 의미 | 최소 문장 |
|--------|-----------|-----------|
| **can** | 능력/가능성 | I can code. |
| **could** | 약한 가능성/공손한 요청 | I could help. |
| **will** | 의지/미래 | I will fix it. |
| **would** | 가정/공손 | I would suggest this. |
| **should** | 권고/당위 | You should test it. |
| **must** | 강한 의무/확신 | We must ship today. |
| **may** | 허가/가능성 | You may proceed. |
| **might** | 약한 가능성 | It might fail. |

**부정형**: 조동사 + not

```
I cannot deploy this.    (= can't)
You should not skip tests.  (= shouldn't)
We must not ignore errors.  (= mustn't)
```

**의문형**: 조동사를 주어 앞으로

```
Can you fix this?
Should we refactor?
Will the test pass?
```

#### 3. EXPAND — 짧은 문장에서 복잡한 문장으로

**Step 1: 기본 — 조동사 + 동사원형**

```
I can deploy.
We should test.
They must fix.
```

**Step 2: 목적어 추가**

```
I can deploy the hotfix.
We should test the edge cases.
They must fix the memory leak.
```

**Step 3: 부사/전치사구 추가**

```
I can deploy the hotfix tonight.
We should test the edge cases before the release.
They must fix the memory leak in the payment module immediately.
```

**Step 4: 조동사 + be + -ing (진행 의미 추가)**

```
He could be working on the fix right now.
The server might be processing the request.
She should be reviewing the pull request by now.
```

**Step 5: 조동사 + have + p.p. (과거 사건에 대한 현재 판단)**

```
He could have fixed it yesterday.        (고칠 수 있었는데 안 했다)
She should have written tests first.     (먼저 테스트를 썼어야 했다)
The deploy must have failed last night.  (어젯밤 배포가 틀림없이 실패했다)
They might have already merged the PR.   (이미 PR을 머지했을 수도 있다)
```

이 구조가 **매우 중요하다**: `조동사 + have + p.p.`는 **과거 사실에 대한 현재 시점의 판단**을 표현한다.

**Step 6: 복합 문장 속 조동사**

```
If the API is stable, we can launch the feature next week.
Although the test passed, we should run it again with production data.
I think we must refactor this module before adding new endpoints.
```

#### 4. CODE — 개발자 비유로 재이해

**조동사 = HTTP 미들웨어 / 인터셉터**

Express.js에서 미들웨어가 요청(request)을 가로채서 **추가 처리**를 하듯이, 조동사는 동사(행동)를 가로채서 **추가 의미(모드)**를 부여한다.

```javascript
// 미들웨어 없이: 직접 실행
app.get('/deploy', deploy);  // "I deploy."

// 미들웨어 추가: 실행 전에 조건/모드 설정
app.get('/deploy', canMiddleware, deploy);     // "I can deploy."
app.get('/deploy', mustMiddleware, deploy);    // "I must deploy."
app.get('/deploy', shouldMiddleware, deploy);  // "I should deploy."
```

**각 조동사를 코드 개념으로 매핑**:

| 조동사 | 프로그래밍 비유 | 설명 |
|--------|----------------|------|
| can | `isCapable()` | 능력 체크 — 실행 가능 여부 |
| could | `isCapable() \|\| isPossible()` | 약한 가능성, 또는 과거의 능력 |
| will | `Future<T>` / `Promise` | 미래에 확실히 실행될 것 |
| would | `if (condition) { Future<T> }` | 조건부 미래 — 가정 상황 |
| should | `@Recommended` / `// TODO` | 권장 사항 — 하면 좋다 |
| must | `@Required` / `@NotNull` | 필수 — 안 하면 에러 |
| may | `@Nullable` / `Optional<T>` | 허가됨, 또는 있을 수도 없을 수도 |
| might | `Optional.empty()` 가능성 | may보다 약한 가능성 |

```java
// "You must validate the input."
// → @NotNull — 반드시 해야 한다. 안 하면 컴파일 에러(=문제 발생).

// "You should validate the input."
// → @Recommended — 안 해도 돌아가긴 하지만, 하는 게 좋다.

// "You can validate the input."
// → isCapable() — 능력/환경이 갖춰져 있다.

// "You might want to validate the input."
// → Optional — 할 수도 있고 안 할 수도 있다. 약한 제안.
```

**`조동사 + have + p.p.` = 트랜잭션 롤백 관점**

```java
// "We should have written tests first."
// → 이미 커밋된 트랜잭션에 대한 후회.
// → "롤백하고 싶지만 이미 실행됐다."
// → 과거에 대한 현재의 판단.

// "The deploy must have failed."
// → 로그를 안 봤지만, 현재 상태로 보아 틀림없이 실패했을 것.
// → 디버깅 추론: 결과(현재)에서 원인(과거)을 역추적.
```

#### 5. DOMAIN — 세무/기술 도메인 실제 예문

**세무 도메인**:

```
Taxpayers must file their returns by March 31.
(납세자는 3월 31일까지 신고해야 한다.)

You can claim a deduction for home office expenses.
(재택근무 비용에 대해 공제를 신청할 수 있습니다.)

This income might be subject to additional tax.
(이 소득은 추가 세금 대상일 수 있습니다.)

The tax authority may impose penalties for late filing.
(세무 당국은 늦은 신고에 대해 과태료를 부과할 수 있습니다.)

We should verify the withholding tax calculation before submission.
(제출 전에 원천징수세 계산을 확인해야 합니다.)

The client could have deducted those expenses last year.
(그 고객은 작년에 그 비용을 공제할 수 있었다. — 하지만 안 했다.)
```

**기술 도메인**:

```
The batch job should complete within 30 minutes.
(배치 작업은 30분 내에 완료되어야 합니다.)

We must ensure data consistency across all microservices.
(모든 마이크로서비스 간 데이터 일관성을 보장해야 합니다.)

The connection pool might be exhausted under heavy load.
(높은 부하 상태에서 커넥션 풀이 소진될 수 있습니다.)

You can use ShedLock to prevent duplicate batch execution.
(중복 배치 실행을 방지하기 위해 ShedLock을 사용할 수 있습니다.)

This endpoint will handle up to 10,000 requests per second.
(이 엔드포인트는 초당 10,000건의 요청을 처리할 것입니다.)

We should have implemented circuit breaker pattern earlier.
(서킷 브레이커 패턴을 더 일찍 구현했어야 했습니다.)
```

#### 6. PRACTICE — 직접 문장 만들기 연습

**A. 빈칸 채우기: 적절한 조동사를 고르시오**

```
1. You _______ validate user input before saving to the database.
   (데이터베이스에 저장하기 전에 사용자 입력을 반드시 검증해야 한다)
   → (a) can  (b) might  (c) must  (d) could

2. If we optimize the query, response time _______ drop below 100ms.
   (쿼리를 최적화하면 응답 시간이 100ms 이하로 떨어질 수 있을 것이다)
   → (a) must  (b) could  (c) can  (d) should

3. The server _______ be down. I'm getting 503 errors.
   (서버가 다운된 것 같다. 503 에러가 뜨고 있다)
   → (a) should  (b) might  (c) will  (d) can

4. We _______ have deployed to production without testing.
   (테스트 없이 프로덕션에 배포하지 말았어야 했다)
   → (a) should  (b) must  (c) shouldn't  (d) wouldn't
```

**B. 한국어 → 영어 작문**

```
1. 이 API는 초당 5,000건의 요청을 처리할 수 있습니다.
   → _______________________________________________

2. 배포 전에 코드 리뷰를 받아야 합니다. (권고)
   → _______________________________________________

3. 데이터베이스 마이그레이션이 실패했을 수도 있습니다.
   → _______________________________________________

4. 그 버그를 어제 고칠 수 있었는데 (안 고쳤다).
   → _______________________________________________
```

**C. 에러 메시지를 자연스러운 영어로 바꾸시오**

```
1. "Not possible to connect to database"
   → "We _______ connect to the database."

2. "It is necessary to restart the server"
   → "We _______ restart the server."

3. "It is recommended to use connection pooling"
   → "You _______ use connection pooling."
```

<details>
<summary>정답 확인</summary>

**A.** 1-(c) must, 2-(b) could, 3-(b) might, 4-(c) shouldn't

**B.**
1. This API can handle 5,000 requests per second.
2. You should get a code review before deployment.
3. The database migration might have failed.
4. We could have fixed that bug yesterday.

**C.** 1-cannot (can't), 2-must, 3-should

</details>

---

### Lesson 17 — 조동사의 과거형이 "과거"가 아닌 이유

#### 1. WHY — 영어에서 가장 혼란스러운 부분 중 하나

한국인 학습자가 가장 자주 하는 실수 중 하나:

> "could는 can의 과거형이니까, '~할 수 있었다'라는 뜻이겠지?"

**틀렸다.** 정확히 말하면, **절반만 맞다.**

`could`가 **실제 과거**를 의미하는 경우도 있지만, 대부분의 실무 영어에서 `could`는 **현재나 미래의 불확실한 가능성** 또는 **공손한 요청**을 뜻한다.

왜 이런 일이 벌어지는가? 영어에는 **"거리감(distance)"** 이라는 핵심 개념이 있기 때문이다.

```
시간적 거리 (temporal distance)  → 과거
심리적 거리 (psychological distance) → 불확실, 가정
사회적 거리 (social distance)    → 공손함, 격식
```

영어의 과거 시제 형태(-ed, could, would 등)는 이 **세 가지 거리**를 모두 표현할 수 있다. 시간적으로 먼 것(과거), 현실에서 먼 것(가정/불확실), 관계에서 먼 것(공손함) — 모두 같은 형태를 사용한다.

이것이 바로 **영어적 사고방식의 핵심** 중 하나다.

#### 2. CORE — 핵심 개념을 가장 짧은 문장으로 체득

**can vs could**:

```
I can fix it.    → 나 고칠 수 있어. (현재 능력, 확신)
I could fix it.  → 나 고칠 수도 있어. (가능성, 불확실) / 고쳐줄 수 있는데요. (공손)
```

**will vs would**:

```
I will do it.    → 나 할 거야. (확실한 의지/미래)
I would do it.   → 나라면 할 텐데. (가정) / 하겠습니다. (공손)
```

**핵심 정리표: 현재형 vs 과거형 조동사**

| 현재형 | 의미 | 과거형 | 의미 변화 |
|--------|------|--------|-----------|
| can | 할 수 있다 (확신) | could | 할 수도 있다 (약한 가능성) / 공손 |
| will | 할 것이다 (확신) | would | 할 텐데 (가정) / 공손 |
| shall | ~하겠다 | should | ~해야 한다 (권고) |
| may | 해도 된다 / 일 수 있다 | might | 일 수도 있다 (더 약한 가능성) |

**`shall → should` 변화가 특히 재미있다**: shall은 원래 "~하겠다"인데, 과거형 should는 "~해야 한다(권고)"로 의미가 완전히 달라졌다. 거리감이 생기면서 "직접적 의지" → "한 발 물러선 조언"이 된 것이다.

#### 3. EXPAND — 짧은 문장에서 복잡한 문장으로

**Pattern 1: could = 약한 가능성 (현재/미래)**

```
The test could fail.
(테스트가 실패할 수도 있다 — 확실하지 않지만 가능성 있음)

This approach could reduce memory usage by 40%.
(이 접근법은 메모리 사용량을 40% 줄일 수도 있다)

We could migrate to the new framework next quarter.
(다음 분기에 새 프레임워크로 마이그레이션할 수도 있다)
```

**Pattern 2: could = 공손한 요청**

```
Could you review my pull request?
(제 풀 리퀘스트 리뷰 해주실 수 있을까요?)

Could you explain the tax calculation logic?
(세금 계산 로직을 설명해 주실 수 있을까요?)
```

비교:
```
Can you fix this?    → (직접적) 이거 고칠 수 있어?
Could you fix this?  → (공손하게) 이거 고쳐주실 수 있을까요?
```

**Pattern 3: would = 가정 상황**

```
I would refactor this, but we don't have time.
(나라면 이걸 리팩토링하겠지만, 시간이 없다)

That would break backward compatibility.
(그러면 하위 호환성이 깨질 것이다 — 만약 그렇게 한다면)

If we had more engineers, we would deliver on time.
(엔지니어가 더 있었다면, 제때 납품할 텐데)
```

**Pattern 4: would = 공손한 제안/요청**

```
I would suggest using a message queue here.
(여기서 메시지 큐를 사용하는 것을 제안드립니다)

Would you like me to set up the CI pipeline?
(CI 파이프라인을 설정해 드릴까요?)

I would appreciate it if you could review this by Friday.
(금요일까지 리뷰해 주시면 감사하겠습니다)
```

**Pattern 5: 확실성의 스펙트럼**

영어에서 조동사는 **확실성의 단계**를 표현한다:

```
확실 ←――――――――――――――――――――――――→ 불확실

must be   will be   should be   can be   could be   may be   might be
틀림없다   ~일 것    ~일 것이다    ~일 수    ~일 수도    ~일 수    ~일지도
          (확신)    (예상)       있다      있다       있다      모르다
```

```
The server must be down.    (틀림없이 다운이다 — 99%)
The server will be down.    (다운될 거다 — 95%)
The server should be down.  (다운일 거다 — 80%)
The server could be down.   (다운일 수도 있다 — 50%)
The server may be down.     (다운일 수 있다 — 40%)
The server might be down.   (다운일지도 모른다 — 20%)
```

#### 4. CODE — 개발자 비유로 재이해

**거리감 = 추상화 레벨**

프로그래밍에서 추상화 레벨이 올라가면 **직접성**이 줄어들고 **유연성**이 늘어난다. 영어의 과거형 조동사도 같은 원리다.

```java
// can = 구체적, 직접적 (Low-level)
// "I can deploy this." → 직접 배포 능력이 있다.
Runtime.exec("deploy.sh");  // 직접 실행

// could = 추상적, 간접적 (High-level)
// "I could deploy this." → 배포할 수도 있다 (옵션 중 하나).
Optional<Deployment> deploy = Optional.of(new Deployment());
// 실행할 수도 있고 안 할 수도 있다.
```

**will vs would = 직접 호출 vs 콜백/조건부 실행**

```java
// will = 직접 호출 — "I will fix this."
bugFixer.fix(bug);  // 무조건 실행

// would = 조건부 실행 — "I would fix this (if I had time)."
if (hasTime) {
    bugFixer.fix(bug);  // 조건이 맞을 때만 실행
}
```

**확실성 스펙트럼 = 로그 레벨 매핑**

```java
// must be   → LOG.error()   — 확실한 문제
// will be   → LOG.warn()    — 높은 확률
// should be → LOG.info()    — 예상대로
// could be  → LOG.debug()   — 가능성 있음
// might be  → LOG.trace()   — 희박한 가능성

LOG.error("The server must be overloaded.");   // 확신
LOG.warn("The server will crash at this rate.");  // 높은 예측
LOG.info("The cache should be refreshed by now.");  // 합리적 예상
LOG.debug("The timeout could be causing issues.");  // 가능성
LOG.trace("Memory might be leaking somewhere.");    // 약한 추측
```

#### 5. DOMAIN — 세무/기술 도메인 실제 예문

**세무 도메인 — 공손한 고객 응대**:

```
직접적 (부자연스러움):
"Can you send your tax documents?"

공손한 (자연스러움):
"Could you send your tax documents by next Friday?"
(금요일까지 세무 서류를 보내주실 수 있을까요?)

더 공손한:
"Would you be able to send your tax documents at your earliest convenience?"
(편하실 때 세무 서류를 보내주실 수 있으시겠습니까?)
```

**세무 도메인 — 가능성 표현**:

```
The taxpayer could qualify for a small business deduction.
(그 납세자는 소기업 공제 자격이 될 수도 있습니다.)

This income would be taxable if the exemption doesn't apply.
(면제가 적용되지 않으면 이 소득은 과세 대상이 될 것입니다.)

The penalty might be waived if the taxpayer files an appeal.
(납세자가 이의를 제기하면 과태료가 면제될 수도 있습니다.)
```

**기술 도메인 — PR 리뷰 코멘트**:

```
"This could cause a race condition."
(이것은 경합 조건을 일으킬 수 있습니다 — 가능성 지적)

"We should add error handling here."
(여기에 에러 핸들링을 추가해야 합니다 — 권고)

"Would it make sense to extract this into a separate service?"
(이것을 별도의 서비스로 분리하는 것이 합리적일까요? — 공손한 제안)

"This would break if the input is null."
(입력이 null이면 이것은 깨질 것입니다 — 가정 상황)
```

#### 6. PRACTICE — 직접 문장 만들기 연습

**A. can vs could — 적절한 것을 고르시오**

```
1. _______ you help me debug this issue?
   (이 이슈 디버깅 좀 도와줄 수 있어? — 동료에게 편하게)
   → can / could

2. _______ you review this PR when you have a moment?
   (시간 되실 때 이 PR 리뷰 해주실 수 있을까요? — 시니어에게 공손하게)
   → can / could

3. This optimization _______ improve performance by 30%.
   (이 최적화가 성능을 30% 향상시킬 수도 있다 — 불확실)
   → can / could

4. Our system _______ handle 10,000 concurrent users.
   (우리 시스템은 1만 동시접속을 처리할 수 있다 — 확신)
   → can / could
```

**B. will vs would — 적절한 것을 고르시오**

```
1. If we add caching, the API _______ be much faster.
   → will / would

2. I _______ deploy the fix tonight.
   (오늘 밤에 수정 배포할 거야 — 확실한 의지)
   → will / would

3. I _______ recommend using PostgreSQL for this use case.
   (이 사용 사례에는 PostgreSQL을 추천드리겠습니다 — 공손)
   → will / would

4. _______ you like me to explain the architecture?
   (아키텍처를 설명해 드릴까요? — 공손한 제안)
   → Will / Would
```

**C. 확실성 단계별 작문: 같은 상황을 다른 조동사로 표현하시오**

```
상황: "서버 응답이 느리다. 원인을 추측하라."

확신 100%: The database _______ be overloaded.
확신 80%:  The database _______ be overloaded.
확신 50%:  The database _______ be overloaded.
확신 20%:  The database _______ be overloaded.
```

<details>
<summary>정답 확인</summary>

**A.** 1-can (편한 관계), 2-could (공손), 3-could (불확실), 4-can (확신)

**B.** 1-would (가정), 2-will (확실한 의지), 3-would (공손), 4-Would (공손한 제안)

**C.** 100%-must, 80%-should, 50%-could, 20%-might

</details>

---

## Week 7: 관계대명사 — 두 문장을 합치는 JOIN

> 관계대명사(Relative Pronouns)는 **두 개의 독립적인 문장을 하나로 결합**하는 도구다.
> 데이터베이스의 JOIN이 두 테이블을 연결하듯, 관계대명사는 두 문장을 하나의 명사 덩어리로 연결한다.

---

### Lesson 18 — 관계대명사가 존재하는 이유

#### 1. WHY — 왜 두 문장을 합쳐야 하는가?

다음 두 문장을 보자:

```
I hired a developer. The developer knows Spring Boot.
(나는 개발자를 고용했다. 그 개발자는 Spring Boot를 안다.)
```

이렇게 두 문장으로 쓰면 **유치하고 반복적**이다. "developer"가 두 번 나온다. 한국어에서도 마찬가지다:

```
나는 개발자를 고용했다. 그 개발자는 Spring Boot를 안다.
→ 나는 Spring Boot를 아는 개발자를 고용했다.
```

한국어는 **관형절(~는, ~은, ~하는)**을 명사 앞에 놓아서 결합한다.
영어는 **관계대명사(who, which, that)**를 사용해서 명사 뒤에 설명을 붙인다.

```
I hired a developer who knows Spring Boot.
```

**핵심 차이**: 한국어는 꾸미는 말이 명사 **앞**에, 영어는 명사 **뒤**에 온다.

```
한국어: [Spring Boot를 아는] + 개발자     ← 수식어가 앞
영어:   a developer + [who knows Spring Boot]  ← 수식어가 뒤
```

이 **어순의 차이**가 한국인 영어 학습자가 관계대명사를 어려워하는 가장 큰 이유다.

#### 2. CORE — 핵심 개념을 가장 짧은 문장으로 체득

**관계대명사의 종류와 역할:**

| 관계대명사 | 선행사 | 역할 | 예문 |
|-----------|--------|------|------|
| **who** | 사람 | 주어 | The dev **who** wrote this code... |
| **whom** | 사람 | 목적어 | The dev **whom** I hired... |
| **which** | 사물 | 주어/목적어 | The bug **which** crashed the server... |
| **that** | 사람/사물 | 주어/목적어 | The tool **that** we use... |
| **whose** | 사람/사물 | 소유 | The team **whose** code passed... |

**관계대명사 문장 만들기 공식:**

```
Step 1: 두 문장에서 공통 요소를 찾는다.
  → I met a developer. + The developer built the API.
  → 공통: "developer"

Step 2: 두 번째 문장의 공통 요소를 관계대명사로 바꾼다.
  → The developer → who

Step 3: 관계대명사를 선행사 바로 뒤에 놓는다.
  → I met a developer who built the API.
```

**최소 문장으로 연습:**

```
I have a colleague. + The colleague codes in Java.
→ I have a colleague who codes in Java.

I found a bug. + The bug crashes the app.
→ I found a bug that crashes the app.

I know a company. + The company's product went viral.
→ I know a company whose product went viral.
```

#### 3. EXPAND — 짧은 문장에서 복잡한 문장으로

**Step 1: 기본 — who/which/that**

```
The developer who fixed the bug is on our team.
The API which handles payments is down.
The tool that we use for deployment is Jenkins.
```

**Step 2: 목적격 관계대명사 (생략 가능)**

관계대명사가 **목적어** 역할을 할 때는 생략할 수 있다:

```
The developer (whom/that) I hired is talented.
                ↑ 생략 가능
→ The developer I hired is talented.

The framework (which/that) we chose is Spring Boot.
                  ↑ 생략 가능
→ The framework we chose is Spring Boot.
```

**주어 역할일 때는 생략 불가:**

```
The developer who fixed the bug... (O)
The developer fixed the bug...     (X) ← 주어가 둘이 되어 문장이 깨진다
```

**Step 3: whose — 소유 관계 표현**

```
I work with a developer. + The developer's code is clean.
→ I work with a developer whose code is clean.

We use a database. + The database's performance is excellent.
→ We use a database whose performance is excellent.
```

**Step 4: 전치사 + 관계대명사**

```
This is the project. + I worked on the project.
→ This is the project on which I worked.  (격식)
→ This is the project which I worked on.  (일반)
→ This is the project I worked on.        (구어, 관계대명사 생략)

She is the manager. + I report to the manager.
→ She is the manager to whom I report.    (격식)
→ She is the manager I report to.         (구어)
```

**Step 5: 긴 관계절 — 실무에서 흔한 형태**

```
The microservice that handles user authentication and communicates
with the OAuth provider has been experiencing intermittent failures
since last week's deployment.
(사용자 인증을 처리하고 OAuth 프로바이더와 통신하는 마이크로서비스가
지난주 배포 이후 간헐적 장애를 겪고 있다.)

The tax regulation that the government introduced in January 2025,
which affects all businesses with annual revenue over 500 million won,
requires additional documentation for overseas transactions.
(정부가 2025년 1월에 도입한 세금 규정은 — 연매출 5억 원 이상의 모든
사업체에 영향을 미치는 — 해외 거래에 대한 추가 문서를 요구한다.)
```

#### 4. CODE — 개발자 비유로 재이해

**관계대명사 = SQL JOIN**

두 테이블을 하나의 결과로 합치는 것과 정확히 같은 원리다.

```sql
-- 두 개의 별도 쿼리 (= 두 개의 문장)
SELECT * FROM developers WHERE id = 1;
SELECT * FROM projects WHERE developer_id = 1;

-- JOIN으로 합치기 (= 관계대명사로 합치기)
SELECT d.name, p.title
FROM developers d
JOIN projects p ON d.id = p.developer_id;
```

```
문장 1: I hired a developer.
문장 2: The developer knows Spring Boot.

→ JOIN 결과:
I hired a developer who knows Spring Boot.
```

| SQL | 관계대명사 | 설명 |
|-----|-----------|------|
| `ON d.id = p.developer_id` | `who` / `which` / `that` | 두 문장의 연결 고리 |
| `LEFT JOIN` | 계속적 용법 (,which) | 부가 정보, 없어도 메인 쿼리 동작 |
| `INNER JOIN` | 제한적 용법 | 필수 조건, 없으면 결과 달라짐 |

**관계대명사 종류 = JOIN 조건의 컬럼 타입**

```java
// who = PersonTable의 PK로 JOIN
// → 사람 엔티티 연결
Developer dev = developerRepo.findByIdWithProjects(id);

// which = ThingTable의 PK로 JOIN
// → 사물 엔티티 연결
Server server = serverRepo.findByIdWithIncidents(id);

// whose = FK(외래키)로 JOIN
// → 소유 관계 연결
// "a developer whose code is clean"
// = developers JOIN code ON developers.id = code.author_id

// that = 범용 JOIN — 사람이든 사물이든
// = polymorphic association
```

**목적격 관계대명사 생략 = DTO 프로젝션**

```java
// 전체 조회 (관계대명사 명시)
// "The framework which we chose is Spring Boot."
SELECT f.*, c.choice_date FROM frameworks f
JOIN choices c ON f.id = c.framework_id;

// 필요한 것만 (관계대명사 생략)
// "The framework we chose is Spring Boot."
SELECT f.name FROM frameworks f
JOIN choices c ON f.id = c.framework_id;
// → JOIN 조건을 생략하진 않지만, 불필요한 컬럼은 뺀다.
// 관계대명사 생략도 비슷 — 의미가 명확하면 생략해서 간결하게.
```

#### 5. DOMAIN — 세무/기술 도메인 실제 예문

**세무 도메인:**

```
The taxpayer who filed late will receive a penalty notice.
(늦게 신고한 납세자는 과태료 통지를 받을 것이다.)

Expenses that exceed 10 million won must be reported separately.
(1,000만 원을 초과하는 비용은 별도로 신고해야 한다.)

The deduction whose eligibility criteria changed in 2025 affects
small business owners.
(2025년에 자격 기준이 변경된 공제는 소상공인에게 영향을 미친다.)

The tax return that we submitted last week is under review.
(지난주에 제출한 세금 신고서가 검토 중이다.)

The accountant whom the client requested is currently unavailable.
(고객이 요청한 회계사는 현재 부재 중이다.)
```

**기술 도메인:**

```
The service that processes tax calculations runs on Kubernetes.
(세금 계산을 처리하는 서비스는 쿠버네티스에서 실행된다.)

The endpoint which returns user data requires authentication.
(사용자 데이터를 반환하는 엔드포인트는 인증이 필요하다.)

The engineer whose commit broke the pipeline is fixing it now.
(파이프라인을 깨뜨린 커밋을 한 엔지니어가 지금 고치고 있다.)

The library that we depend on for PDF generation has a known vulnerability.
(PDF 생성에 의존하는 라이브러리에 알려진 취약점이 있다.)

The SAGA pattern, which coordinates distributed transactions,
is essential for our microservice architecture.
(분산 트랜잭션을 조정하는 SAGA 패턴은 우리 마이크로서비스 아키텍처에 필수적이다.)
```

#### 6. PRACTICE — 직접 문장 만들기 연습

**A. 두 문장을 관계대명사로 합치시오**

```
1. I interviewed a candidate. + The candidate has 5 years of Java experience.
   → __________________________________________________

2. We use a framework. + The framework supports reactive programming.
   → __________________________________________________

3. I work with a team lead. + The team lead's experience spans 15 years.
   → __________________________________________________

4. She found the bug. + The bug caused data inconsistency.
   → __________________________________________________
```

**B. 관계대명사를 생략할 수 있으면 생략하고, 없으면 그대로 쓰시오**

```
1. The API that we built last month is now in production.
   → __________________________________________________

2. The developer who maintains this service is on vacation.
   → __________________________________________________

3. The tool that I mentioned in the meeting is Grafana.
   → __________________________________________________
```

**C. 한국어 → 영어 작문**

```
1. 세금 신고를 처리하는 배치 잡이 매일 밤 실행된다.
   → __________________________________________________

2. 내가 어제 리뷰한 코드에 보안 취약점이 있었다.
   → __________________________________________________

3. 성능이 뛰어난 데이터베이스를 선택해야 한다.
   → __________________________________________________
```

<details>
<summary>정답 확인</summary>

**A.**
1. I interviewed a candidate who has 5 years of Java experience.
2. We use a framework that supports reactive programming.
3. I work with a team lead whose experience spans 15 years.
4. She found the bug that caused data inconsistency.

**B.**
1. The API we built last month is now in production. (생략 가능 — 목적격)
2. The developer who maintains this service is on vacation. (생략 불가 — 주격)
3. The tool I mentioned in the meeting is Grafana. (생략 가능 — 목적격)

**C.**
1. The batch job that processes tax returns runs every night.
2. The code (that) I reviewed yesterday had a security vulnerability.
3. We should choose a database whose performance is excellent.

</details>

---

### Lesson 19 — 관계부사와 관계대명사의 차이

#### 1. WHY — 관계부사는 왜 따로 존재하는가?

관계대명사는 **명사**를 대체한다 (who = the developer, which = the bug).
하지만 때로는 **장소, 시간, 이유** 같은 부사적 정보를 연결해야 할 때가 있다.

```
I remember the day. + I started working on that day.
→ I remember the day when I started working.
```

여기서 "on that day"는 **부사구(시간 부사)**다. 명사가 아니라 **전치사 + 명사** 덩어리다. 이런 경우에 `when`, `where`, `why`라는 **관계부사**를 사용한다.

**핵심 공식:**

```
관계부사 = 전치사 + 관계대명사

where = in/at which   (장소)
when  = at/on/in which (시간)
why   = for which      (이유)
```

그래서 관계부사는 관계대명사의 **축약형/편의 기능**이라고 볼 수 있다.

#### 2. CORE — 핵심 개념을 가장 짧은 문장으로 체득

**where — 장소:**

```
This is the office. + I work in the office.
→ This is the office where I work.
= This is the office in which I work. (격식)
```

**when — 시간:**

```
I remember the day. + The server crashed on that day.
→ I remember the day when the server crashed.
= I remember the day on which the server crashed. (격식)
```

**why — 이유:**

```
Tell me the reason. + You chose this framework for that reason.
→ Tell me the reason why you chose this framework.
= Tell me the reason for which you chose this framework. (격식)
```

**how — 방법 (특이 케이스):**

```
Show me how you solved the problem.
```

주의: `how`는 선행사를 갖지 않는다. `the way how`는 틀린 표현이다.

```
(X) This is the way how I fixed it.
(O) This is the way I fixed it.
(O) This is how I fixed it.
```

`the way`와 `how`는 동시에 쓸 수 없다 — 둘 중 하나만 선택해야 한다.

#### 3. EXPAND — 짧은 문장에서 복잡한 문장으로

**Step 1: 기본 관계부사**

```
This is where we deploy.       (여기가 우리가 배포하는 곳이다)
That was when the error occurred. (그때 에러가 발생했다)
This is why we use caching.    (이것이 우리가 캐싱을 사용하는 이유다)
```

**Step 2: 선행사 + 관계부사**

```
The server room where our machines run is on the third floor.
The moment when the deployment completed was 3 AM.
The reason why we migrated to Kubernetes was scalability.
```

**Step 3: 관계부사 vs 관계대명사 — 구분법**

```
This is the office where I work.
     → "work"는 완전한 자동사. 목적어 없음. → where (부사)

This is the office which I designed.
     → "designed"는 타동사. "which"가 목적어 역할. → which (대명사)
```

**판별 기준: 관계절이 완전한 문장인가?**

```
관계부사 뒤: 완전한 문장 (주어+동사+목적어 다 있음)
  → The city where I live is Seoul.   ("I live"는 완전)

관계대명사 뒤: 불완전한 문장 (주어 또는 목적어가 빠짐)
  → The city which I visited is Seoul. ("I visited ___" — 목적어 빠짐)
```

**Step 4: 복합 문장에서 관계부사 활용**

```
We need to identify the exact point where the data gets corrupted,
which is likely in the transformation layer.
(데이터가 손상되는 정확한 지점을 찾아야 하는데,
그것은 변환 계층에 있을 가능성이 높다.)

The quarter when we migrated to the new tax system was Q3 2025,
and that was also when we hired three new backend developers.
(새 세금 시스템으로 마이그레이션한 분기는 2025년 3분기였고,
그때 백엔드 개발자 세 명을 채용하기도 했다.)
```

#### 4. CODE — 개발자 비유로 재이해

**관계부사 vs 관계대명사 = 변수 참조 vs 컨텍스트 참조**

```java
// 관계대명사 = 직접 변수를 참조
// "The bug which I fixed" → 직접 객체(bug)를 다룸
Bug bug = bugRepo.findById(id);  // 객체 직접 참조
bugService.fix(bug);

// 관계부사 = 컨텍스트(환경/조건)를 참조
// "The environment where the bug occurs" → 환경/맥락을 참조
Environment env = envRepo.findByCondition("production");
// 직접 객체가 아니라, 객체가 존재하는 '맥락'을 참조
```

**where/when/why = 메타데이터 쿼리**

```sql
-- 관계대명사: 엔티티 자체를 JOIN
-- "The developer who wrote the code"
SELECT d.* FROM developers d
JOIN code c ON d.id = c.author_id;

-- 관계부사 where: 위치 메타데이터로 조회
-- "The server where the error occurred"
SELECT s.* FROM servers s
JOIN errors e ON s.location = e.location;

-- 관계부사 when: 시간 메타데이터로 조회
-- "The day when the deploy failed"
SELECT * FROM deploys
WHERE deploy_date = '2025-03-15';

-- 관계부사 why: 원인 메타데이터로 조회
-- "The reason why we use Redis"
SELECT reason FROM architecture_decisions
WHERE technology = 'Redis';
```

**`the way` vs `how` = 인터페이스의 구현체**

```java
// 둘 다 같은 것을 가리키지만 동시에 선언할 수 없다.
// = 같은 빈(bean)에 대해 두 개의 별칭을 동시에 쓸 수 없는 것과 비슷

// (X) the way how → 중복 선언 에러
@Bean("theWay")
@Bean("how")  // 충돌!
public SolutionMethod solutionMethod() { ... }

// (O) 하나만 선택
// "This is the way I solved it."
// "This is how I solved it."
```

#### 5. DOMAIN — 세무/기술 도메인 실제 예문

**세무 도메인:**

```
The fiscal year when the new tax law takes effect is 2026.
(새 세법이 시행되는 회계연도는 2026년이다.)

This is the form where you report your overseas income.
(이것이 해외 소득을 신고하는 양식이다.)

The reason why the deduction was denied is insufficient documentation.
(공제가 거부된 이유는 불충분한 서류 때문이다.)

Can you explain how the withholding tax is calculated?
(원천징수세가 어떻게 계산되는지 설명해 주시겠습니까?)

The office where taxpayers submit their returns is on the second floor.
(납세자가 신고서를 제출하는 사무실은 2층에 있다.)
```

**기술 도메인:**

```
The cluster where we run our batch jobs has 16 nodes.
(배치 잡을 실행하는 클러스터에는 16개의 노드가 있다.)

The moment when the circuit breaker trips is critical for observability.
(서킷 브레이커가 작동하는 순간은 관측 가능성에 중요하다.)

This is why we implemented the Outbox pattern for event publishing.
(이것이 이벤트 발행을 위해 Outbox 패턴을 구현한 이유다.)

Let me show you how ShedLock prevents duplicate batch execution.
(ShedLock이 중복 배치 실행을 어떻게 방지하는지 보여드리겠습니다.)
```

#### 6. PRACTICE — 직접 문장 만들기 연습

**A. where, when, why, how 중 적절한 것을 고르시오**

```
1. This is the environment _______ we run our integration tests.
   → where / when / why / how

2. I don't know _______ the deployment failed.
   → where / when / why / how

3. The time _______ the batch job runs is midnight.
   → where / when / why / how

4. Show me _______ you implemented the caching layer.
   → where / when / why / how
```

**B. 관계부사 vs 관계대명사 — 적절한 것을 고르시오**

```
1. The server _______ hosts our API is in Seoul.
   → where / which

2. The server _______ the error occurred is in Seoul.
   → where / which

3. The day _______ we launched the product was exciting.
   → when / which

4. The day _______ I will never forget was the launch day.
   → when / which
```

**C. 한국어 → 영어 작문**

```
1. 이것이 우리가 마이크로서비스 아키텍처를 선택한 이유다.
   → __________________________________________________

2. 세금 신고서를 제출하는 웹사이트가 다운되었다.
   → __________________________________________________

3. 서버가 다운된 시간은 새벽 3시였다.
   → __________________________________________________
```

<details>
<summary>정답 확인</summary>

**A.** 1-where, 2-why, 3-when, 4-how

**B.** 1-which (주격: "hosts"의 주어), 2-where (부사: "the error occurred"는 완전한 문장), 3-when (시간 부사), 4-which (목적격: "I will never forget ___")

**C.**
1. This is why we chose microservice architecture.
2. The website where you submit tax returns is down.
3. The time when the server went down was 3 AM.

</details>

---

### Lesson 20 — 제한적 용법 vs 계속적 용법 (콤마의 유무)

#### 1. WHY — 콤마 하나가 의미를 바꾼다

영어에서 관계대명사 앞에 **콤마(,)가 있느냐 없느냐**에 따라 문장의 의미가 완전히 달라진다. 이것은 단순한 문법 규칙이 아니라, **정보의 중요도**를 결정하는 장치다.

```
(A) The developer who fixed the bug got promoted.
    (그 버그를 고친 개발자가 승진했다.)

(B) John, who fixed the bug, got promoted.
    (John이 — 그가 그 버그를 고쳤는데 — 승진했다.)
```

**(A)** 에서 "who fixed the bug"는 **필수 정보**다. 이 정보가 없으면 **어떤 개발자**인지 알 수 없다. 여러 개발자 중에서 "버그를 고친 그 개발자"를 **특정(제한)**하는 역할이다.

**(B)** 에서 "who fixed the bug"는 **부가 정보**다. "John"이라는 이름만으로 이미 누군지 알 수 있다. 관계절은 그냥 **추가 설명**을 덧붙이는 것이다.

| | 제한적 용법 | 계속적 용법 |
|---|---|---|
| 콤마 | 없음 | 있음 |
| 정보 성격 | 필수 (이것이 없으면 누구/무엇인지 모름) | 부가 (없어도 대상이 명확) |
| 삭제 가능? | 삭제하면 의미 변화 | 삭제해도 핵심 의미 유지 |
| that 사용 | 가능 | **불가능** (콤마 뒤에 that은 안 됨) |

#### 2. CORE — 핵심 개념을 가장 짧은 문장으로 체득

**제한적 용법 (Restrictive) — 콤마 없음:**

```
The employees who work remotely need VPN access.
(원격 근무하는 직원들은 VPN 접근이 필요하다.)
→ 모든 직원이 아니라, "원격 근무하는" 직원만 해당
→ "who work remotely"를 삭제하면: 모든 직원이 VPN이 필요하다는 의미로 변함
```

**계속적 용법 (Non-restrictive) — 콤마 있음:**

```
Our CTO, who has 20 years of experience, approved the migration plan.
(우리 CTO는 — 20년 경력의 — 마이그레이션 계획을 승인했다.)
→ CTO는 한 명. "who has 20 years of experience"는 부가 정보.
→ 삭제해도 문장 의미 동일: "Our CTO approved the migration plan."
```

**that vs which 규칙:**

```
제한적: that 또는 which 사용 가능 (미국식은 that 선호)
  → The API that/which handles payments...

계속적: which만 사용 가능 (that 불가)
  → Our main API, which handles payments, ...
  → Our main API, that handles payments, ... (X) ← 틀림!
```

#### 3. EXPAND — 짧은 문장에서 복잡한 문장으로

**콤마 유무에 따른 의미 차이 비교:**

```
(A) The servers that are in Seoul handle Korean users.
    (서울에 있는 서버들이 한국 사용자를 처리한다.)
    → 서울에 있는 서버만. 다른 곳 서버는 해당 안 됨.

(B) Our servers, which are in Seoul, handle Korean users.
    (우리 서버들은 — 서울에 있는 — 한국 사용자를 처리한다.)
    → 우리 서버 전부가 서울에 있고, 한국 사용자를 처리한다.
```

**계속적 용법의 확장 패턴:**

```
Pattern 1: 고유명사 + , who/which ...
  → Spring Boot, which is maintained by VMware, supports microservices.
  → Kim, who joined our team last month, already shipped two features.

Pattern 2: 유일한 대상 + , who/which ...
  → My laptop, which has 32GB RAM, can run the entire stack locally.
  → The CEO, who founded the company in 2015, announced the IPO.

Pattern 3: 절 전체를 받는 which
  → The deploy succeeded, which surprised everyone.
    (배포가 성공했는데, 그것이 모두를 놀라게 했다.)
  → 여기서 which는 "The deploy succeeded"라는 사실 전체를 가리킨다.

  → He skipped code review, which caused a production bug.
    (그는 코드 리뷰를 건너뛰었는데, 그것이 프로덕션 버그를 야기했다.)
```

**제한적 용법으로 정교하게 대상 특정하기:**

```
The tax returns that were filed after the deadline will incur penalties.
(마감일 이후에 제출된 세금 신고서는 과태료가 부과될 것이다.)
→ "마감일 이후에 제출된 것만" — 나머지는 해당 없음

The tests that failed in the last CI run need immediate attention.
(마지막 CI 실행에서 실패한 테스트들은 즉시 주의가 필요하다.)
→ "실패한 테스트만" — 통과한 것은 해당 없음

The endpoints that require authentication are listed in the security config.
(인증이 필요한 엔드포인트들이 보안 설정에 나열되어 있다.)
→ "인증이 필요한 엔드포인트만" — 공개 엔드포인트는 제외
```

#### 4. CODE — 개발자 비유로 재이해

**제한적 용법 = WHERE 절 (필터링)**

```sql
-- 제한적: "The servers that are in Seoul handle Korean users."
SELECT * FROM servers WHERE location = 'Seoul';
-- → WHERE 절은 필수. 없으면 다른 결과가 나온다.
```

**계속적 용법 = LEFT JOIN 또는 주석**

```sql
-- 계속적: "Our servers, which are in Seoul, handle Korean users."
SELECT s.*, 'Seoul' as location_info  -- 부가 정보
FROM our_servers s;
-- → 부가 정보 없어도 메인 쿼리 결과는 동일.
```

**코드 주석 비유:**

```java
// 제한적 용법 = 코드의 일부 (없으면 동작이 달라짐)
if (server.getLocation().equals("Seoul")) {  // 필수 조건
    server.handleKoreanUsers();
}

// 계속적 용법 = 주석 (없어도 코드 동작은 동일)
server.handleKoreanUsers(); // located in Seoul  ← 부가 설명
```

**that vs which 규칙 = strict mode**

```javascript
// 제한적 (that/which 모두 가능)
// = JavaScript의 == (느슨한 비교)
const filteredServers = servers.filter(s => s.location === "Seoul");

// 계속적 (which만 가능, that 불가)
// = TypeScript의 strict mode — 규칙이 더 엄격
// , which = 주석 또는 데코레이터 — 핵심 로직과 분리
@Description("Located in Seoul")  // 부가 정보, 로직에 영향 없음
class OurServer { ... }
```

**절 전체를 받는 which = 메서드 체이닝의 결과**

```java
// "The deploy succeeded, which surprised everyone."
deployService.deploy()
    .onSuccess(result -> {
        // "which" = 이전 결과(deploy 성공) 전체를 받아서 후속 처리
        notifyTeam("Surprised everyone: " + result);
    });
```

#### 5. DOMAIN — 세무/기술 도메인 실제 예문

**세무 도메인:**

```
제한적:
Taxpayers who earn more than 50 million won must file a comprehensive
income tax return.
(5,000만 원 이상을 버는 납세자는 종합소득세 신고를 해야 한다.)
→ "5,000만 원 이상 버는" 납세자만 해당

계속적:
The National Tax Service, which oversees all tax collection in Korea,
announced new guidelines for digital asset taxation.
(국세청은 — 한국의 모든 세금 징수를 관할하는 — 디지털 자산 과세에 대한
새로운 지침을 발표했다.)
→ 국세청은 하나. 관계절은 부가 설명.

절 전체를 받는 which:
The client missed the filing deadline, which resulted in a 20% penalty.
(고객이 신고 기한을 놓쳤고, 그 결과 20% 과태료가 부과되었다.)
```

**기술 도메인:**

```
제한적:
The microservices that handle financial data require encryption at rest.
(금융 데이터를 다루는 마이크로서비스는 정지 시 암호화가 필요하다.)
→ "금융 데이터를 다루는" 서비스만 해당

계속적:
Redis, which we use for session management, needs to be upgraded to v7.
(Redis는 — 우리가 세션 관리에 사용하는 — 버전 7로 업그레이드해야 한다.)
→ Redis는 특정 도구. 관계절은 부가 설명.

절 전체를 받는 which:
We enabled auto-scaling, which reduced our infrastructure costs by 30%.
(오토스케일링을 활성화했고, 그 결과 인프라 비용이 30% 감소했다.)
```

#### 6. PRACTICE — 직접 문장 만들기 연습

**A. 제한적 vs 계속적 — 콤마가 필요한 곳에 콤마를 추가하시오**

```
1. PostgreSQL which is an open-source database supports JSON columns.
   → __________________________________________________

2. The APIs that require API keys are listed in the documentation.
   → __________________________________________________

3. Our team lead who has a tax accounting background designed the
   calculation engine.
   → __________________________________________________

4. The batch jobs that run between midnight and 6 AM should not be
   interrupted.
   → __________________________________________________
```

**B. that vs which — 적절한 것을 고르시오**

```
1. The library _______ we use for JSON parsing is Jackson.
   → that / which (둘 다 가능)

2. Jackson, _______ is maintained by the open-source community,
   supports various data formats.
   → that / which

3. The bug _______ caused the outage has been fixed.
   → that / which (둘 다 가능)

4. We fixed the memory leak, _______ improved response time by 50%.
   → that / which
```

**C. 한국어 → 영어 작문 (제한적/계속적 구분)**

```
1. 인증이 필요한 엔드포인트는 /api/v2 경로 아래에 있다. (제한적)
   → __________________________________________________

2. 우리의 메인 데이터베이스인 PostgreSQL이 업그레이드가 필요하다. (계속적)
   → __________________________________________________

3. 그가 테스트를 건너뛰었고, 그 결과 프로덕션 버그가 발생했다. (절 전체 수식)
   → __________________________________________________
```

<details>
<summary>정답 확인</summary>

**A.**
1. PostgreSQL, which is an open-source database, supports JSON columns. (계속적 — PostgreSQL은 고유명사)
2. (그대로) The APIs that require API keys are listed in the documentation. (제한적 — API 키가 필요한 것만)
3. Our team lead, who has a tax accounting background, designed the calculation engine. (계속적 — 팀 리드는 한 명)
4. (그대로) The batch jobs that run between midnight and 6 AM should not be interrupted. (제한적 — 해당 시간대 배치만)

**B.** 1-that/which, 2-which (계속적은 which만), 3-that/which, 4-which (계속적은 which만)

**C.**
1. The endpoints that require authentication are under the /api/v2 path.
2. Our main database, PostgreSQL, which needs an upgrade, ...
   또는: Our main database, which is PostgreSQL, needs an upgrade.
3. He skipped testing, which caused a production bug.

</details>

---

## Week 8: 접속사와 복문 — 로직 흐름 제어

> 접속사(Conjunctions)는 문장과 문장을 연결하여 **논리적 흐름**을 만든다.
> 프로그래밍의 제어 흐름(if/else, &&, ||)과 정확히 대응되는 영어의 도구다.

---

### Lesson 21 — 등위접속사: AND, OR, BUT (논리 연산자)

#### 1. WHY — 문장을 연결하는 가장 기본적인 방법

지금까지 우리는 하나의 문장 안에서 문법을 다뤘다. 하지만 실제 의사소통에서는 **여러 생각을 연결**해야 한다.

```
I wrote the code.    (나는 코드를 작성했다)
It passed all tests. (모든 테스트를 통과했다)
```

이 두 문장의 **관계**가 무엇인가? 나열? 대조? 결과? 이 관계를 표현하는 도구가 **접속사(Conjunction)**다.

가장 기본적인 접속사는 **등위접속사(Coordinating Conjunctions)**로, 두 개의 **대등한** 요소를 연결한다. 영어에서는 **FANBOYS**라는 약자로 7개를 외운다:

```
F - For    (이유: ~이니까)
A - And    (나열: 그리고)
N - Nor    (부정 나열: ~도 아니고)
B - But    (대조: 하지만)
O - Or     (선택: 또는)
Y - Yet    (대조: 그럼에도)
S - So     (결과: 그래서)
```

이 중 실무에서 가장 많이 쓰는 것은 **and, or, but** 세 가지다.

#### 2. CORE — 핵심 개념을 가장 짧은 문장으로 체득

**and = 나열, 추가 (논리 AND: &&)**

```
I code and I test.
→ 두 행동을 모두 한다.
```

**or = 선택, 대안 (논리 OR: ||)**

```
Fix it or revert it.
→ 둘 중 하나를 해라.
```

**but = 대조, 예외 (예외 처리)**

```
I finished the feature, but it has bugs.
→ 기능은 완성했는데, 버그가 있다.
```

**등위접속사의 문법 규칙:**

1. 두 개의 **완전한 절(독립절)**을 연결할 때는 **콤마 + 접속사**를 쓴다:
   ```
   I wrote the code, and it passed all tests.
   ```

2. 단어나 구를 연결할 때는 콤마 없이:
   ```
   I like Java and Python.        (단어 나열)
   She works quickly and efficiently. (부사 나열)
   ```

3. 세 개 이상 나열할 때는 **Oxford comma** 사용:
   ```
   We use Java, Python, and Go.
   (마지막 항목 전에 콤마 + and)
   ```

#### 3. EXPAND — 짧은 문장에서 복잡한 문장으로

**and의 확장:**

```
Level 1: 단어 연결
  → Java and Python

Level 2: 구(phrase) 연결
  → writing code and reviewing PRs

Level 3: 절(clause) 연결
  → I wrote the code, and my colleague reviewed it.

Level 4: 연속 동작 (순서)
  → I cloned the repo, installed dependencies, and ran the tests.

Level 5: 복합 문장
  → The backend team refactored the authentication module and
    implemented rate limiting, and the frontend team redesigned
    the login page and added error handling.
```

**or의 확장:**

```
Level 1: 단순 선택
  → Use Redis or Memcached.

Level 2: 제안/대안 제시
  → We can optimize the query, or we can add a caching layer.

Level 3: 경고/위협 (명령문 + or)
  → Fix the memory leak now, or the server will crash.
  → Deploy the hotfix, or we'll lose customers.

Level 4: 불확실성 표현
  → The issue is in the database layer or the application layer.
  → It could take two days or two weeks, depending on the complexity.
```

**but의 확장:**

```
Level 1: 단순 대조
  → It works, but it's slow.

Level 2: 양보 + 반전
  → The code is clean, but the architecture needs improvement.

Level 3: 기대와 현실의 차이
  → We expected the migration to take one day, but it took an entire week.

Level 4: 부분 긍정/부정
  → The API handles reads well, but write performance is poor.
  → Not the codebase, but the deployment process needs fixing.
```

**yet의 활용 (but과 비슷하지만 더 강한 대조):**

```
The code passed all tests, yet it failed in production.
(모든 테스트를 통과했음에도 불구하고, 프로덕션에서 실패했다.)

She's a junior developer, yet her code quality is exceptional.
(주니어 개발자인데도, 코드 품질이 뛰어나다.)
```

**so의 활용 (결과):**

```
The database was full, so we had to archive old records.
(데이터베이스가 가득 찼고, 그래서 오래된 레코드를 아카이브해야 했다.)

The API response was too slow, so we added Redis caching.
(API 응답이 너무 느려서 Redis 캐싱을 추가했다.)
```

#### 4. CODE — 개발자 비유로 재이해

**등위접속사 = 논리 연산자 + 제어 흐름**

```java
// AND (&&) = "I wrote the code, and it passed all tests."
boolean success = wroteCode && passedTests;
// 두 조건이 모두 참이어야 전체가 참

// OR (||) = "Fix it now, or the server will crash."
if (!fixNow) {
    serverCrash();  // or 뒤의 결과
}
// 하나라도 참이면 문제 해결

// BUT = try-catch / 예외 처리
try {
    // "The feature works" (but 앞)
    feature.run();
} catch (PerformanceException e) {
    // "but it's slow" (but 뒤)
    LOG.warn("Feature works but is slow", e);
}
```

**FANBOYS = 제어 흐름 키워드 매핑**

| 접속사 | 프로그래밍 비유 | 예문 |
|--------|----------------|------|
| and | `&&` / 순차 실행 | I built and deployed the app. |
| or | `\|\|` / fallback | Cache it or query the DB. |
| but | `catch` / 예외 | It compiled, but crashed at runtime. |
| so | `then` / 결과 | OOM occurred, so I increased heap size. |
| yet | `catch (UnexpectedException)` | Tests passed, yet prod failed. |
| for | `// because` 주석 | We chose Kafka, for it handles high throughput. |
| nor | `!A && !B` | It neither compiles nor runs. |

**Oxford comma = 배열 리터럴**

```javascript
// Oxford comma 없이 → 모호함 발생 가능
const stack = ["React", "Node.js", "Redis and PostgreSQL"];
// Redis와 PostgreSQL이 하나의 항목인가, 둘인가?

// Oxford comma 있이 → 명확
const stack = ["React", "Node.js", "Redis", "PostgreSQL"];
// 또는: "React, Node.js, Redis, and PostgreSQL"
```

#### 5. DOMAIN — 세무/기술 도메인 실제 예문

**세무 도메인:**

```
and (나열):
The taxpayer must report all domestic and overseas income.
(납세자는 모든 국내 및 해외 소득을 신고해야 한다.)

Prepare the income statement, calculate the tax liability,
and submit the return before the deadline.
(손익계산서를 준비하고, 세금 부채를 계산하고, 마감일 전에 신고서를 제출하라.)

or (선택/경고):
File your return on time, or you will face penalties.
(제때 신고하시오, 그렇지 않으면 과태료에 직면할 것입니다.)

You can claim the standard deduction or itemize your deductions.
(표준 공제를 신청하거나 항목별 공제를 할 수 있습니다.)

but (대조):
The client earned significant income this year, but most of it
qualifies for tax exemptions.
(고객이 올해 상당한 소득을 올렸지만, 대부분 세금 면제 대상이다.)

so (결과):
The tax law changed in 2025, so we need to update our calculation engine.
(2025년에 세법이 바뀌었으므로 계산 엔진을 업데이트해야 한다.)
```

**기술 도메인:**

```
and:
We implemented SAGA pattern and Outbox pattern for distributed transactions.
(분산 트랜잭션을 위해 SAGA 패턴과 Outbox 패턴을 구현했다.)

or:
Use environment variables or a config server for sensitive settings.
(민감한 설정에는 환경 변수 또는 config 서버를 사용하라.)

but:
The batch job completed successfully, but the processing time exceeded
our SLA by 10 minutes.
(배치 잡은 성공적으로 완료되었지만, 처리 시간이 SLA를 10분 초과했다.)

so:
The connection pool was exhausted, so incoming requests started timing out.
(커넥션 풀이 소진되어 들어오는 요청이 타임아웃되기 시작했다.)

yet:
We followed all best practices, yet the system experienced downtime.
(모든 모범 사례를 따랐는데도 시스템 다운타임이 발생했다.)
```

#### 6. PRACTICE — 직접 문장 만들기 연습

**A. 적절한 등위접속사를 고르시오 (and, or, but, so, yet)**

```
1. I finished the refactoring, _______ the legacy tests are still failing.
   → ________

2. Add proper logging, _______ debugging will be a nightmare.
   → ________

3. We planned, implemented, tested, _______ deployed the new feature
   within one sprint.
   → ________

4. The server ran out of disk space, _______ we expanded the volume.
   → ________

5. The algorithm is correct, _______ it's too slow for production use.
   → ________
```

**B. 두 문장을 적절한 접속사로 연결하시오**

```
1. The test passed locally. + It failed in CI.
   → __________________________________________________

2. Upgrade the JDK to 21. + You won't be able to use virtual threads.
   → __________________________________________________

3. We optimized the database queries. + We added a caching layer.
   → __________________________________________________
```

**C. 한국어 → 영어 작문**

```
1. 세금 신고서를 수정하거나 추가 서류를 제출하세요.
   → __________________________________________________

2. API가 안정적이고, 성능도 좋고, 문서화도 잘 되어 있다.
   → __________________________________________________

3. 모든 테스트가 통과했으므로, 프로덕션에 배포할 수 있다.
   → __________________________________________________
```

<details>
<summary>정답 확인</summary>

**A.** 1-but, 2-or, 3-and, 4-so, 5-yet (또는 but)

**B.**
1. The test passed locally, but it failed in CI.
2. Upgrade the JDK to 21, or you won't be able to use virtual threads.
3. We optimized the database queries and added a caching layer.

**C.**
1. Amend your tax return, or submit additional documents.
2. The API is stable, performant, and well-documented.
3. All tests passed, so we can deploy to production.

</details>

---

### Lesson 22 — 종속접속사: if, when, because, although (조건문/분기)

#### 1. WHY — 등위접속사만으로는 부족하다

등위접속사는 두 절을 **대등하게** 연결한다. 하지만 실제 논리에서는 **종속 관계**가 필요하다:

```
대등: 코드를 쓴다 + 테스트를 쓴다
     → I write code, and I write tests. (둘 다 동등한 행동)

종속: 만약 테스트가 통과하면 → 배포한다
     → If the test passes, I deploy. (조건 → 결과)
```

**종속접속사(Subordinating Conjunctions)**는 하나의 절을 다른 절에 **의존하게** 만든다. 종속절(부사절)은 혼자서는 완전한 문장이 되지 못한다:

```
"If the test passes" ← 이것만으로는 문장이 안 됨. 뭘 어쩌라는 거?
"If the test passes, I deploy." ← 주절과 합쳐져야 완성.
```

이것이 프로그래밍의 **조건문, 이벤트 리스너, 에러 핸들링**과 정확히 대응된다.

#### 2. CORE — 핵심 개념을 가장 짧은 문장으로 체득

**주요 종속접속사와 의미:**

| 접속사 | 의미 | 프로그래밍 비유 |
|--------|------|----------------|
| **if** | ~하면 | `if (condition)` |
| **unless** | ~하지 않으면 | `if (!condition)` |
| **when** | ~할 때 | `@EventListener` |
| **while** | ~하는 동안 | `while (condition)` |
| **before** | ~하기 전에 | `@Before` / 전처리 |
| **after** | ~한 후에 | `@After` / 후처리 |
| **because** | ~이기 때문에 | `// reason:` 주석 |
| **since** | ~이후로 / ~이기 때문에 | `since timestamp` / 이유 |
| **although** | ~에도 불구하고 | `try-catch` |
| **even though** | ~에도 불구하고 (강조) | `catch (Exception e)` |
| **so that** | ~하기 위해 | `// purpose:` 주석 |
| **as long as** | ~하는 한 | `while (invariant)` |

**어순 규칙:**

종속절은 **앞에도, 뒤에도** 올 수 있다:

```
종속절 앞: If the test passes, I deploy.      (콤마 필요)
종속절 뒤: I deploy if the test passes.       (콤마 불필요)
```

종속절이 앞에 오면 **콤마**로 구분한다. 뒤에 오면 콤마 없이 쓴다.

#### 3. EXPAND — 짧은 문장에서 복잡한 문장으로

**if — 조건:**

```
Level 1: If it breaks, fix it.
Level 2: If the API returns a 500 error, retry the request.
Level 3: If the database migration fails, roll back to the previous
         version and notify the on-call engineer.
Level 4: If the response time exceeds 200ms and the error rate is
         above 1%, the circuit breaker should trip automatically.
```

**unless — 부정 조건 (if not):**

```
Unless you test it, don't deploy it.
(테스트하지 않으면 배포하지 마라.)

The batch job runs every night unless the system is in maintenance mode.
(시스템이 유지보수 모드가 아닌 한 배치 잡은 매일 밤 실행된다.)

Don't merge the PR unless all reviewers have approved.
(모든 리뷰어가 승인하지 않으면 PR을 머지하지 마라.)
```

**when — 시점/이벤트:**

```
When the user logs in, generate a new session token.
(사용자가 로그인하면 새 세션 토큰을 생성하라.)

When the server starts, it loads all configurations from the config server.
(서버가 시작되면 config 서버에서 모든 설정을 로드한다.)

Notify the team when the deployment is complete.
(배포가 완료되면 팀에 알려라.)
```

**because — 이유:**

```
We chose Kotlin because it offers null safety.
(null 안전성을 제공하기 때문에 Kotlin을 선택했다.)

Because the API was unstable, we implemented a fallback mechanism.
(API가 불안정했기 때문에 폴백 메커니즘을 구현했다.)

The build failed because a dependency was missing from the POM file.
(POM 파일에서 의존성이 누락되어 빌드가 실패했다.)
```

**although / even though — 양보 (예상 반전):**

```
Although the test coverage is 95%, we still found a critical bug.
(테스트 커버리지가 95%인데도 치명적인 버그를 발견했다.)

Even though we doubled the server capacity, the response time didn't improve.
(서버 용량을 두 배로 늘렸는데도 응답 시간이 개선되지 않았다.)

The system remained stable although the traffic spiked to 5x the normal level.
(트래픽이 평상시의 5배로 급증했지만 시스템은 안정적이었다.)
```

**before / after — 순서:**

```
Before you deploy, make sure all tests pass.
(배포 전에 모든 테스트가 통과하는지 확인하라.)

After the migration completes, verify the data integrity.
(마이그레이션이 완료된 후 데이터 무결성을 확인하라.)

Run the linter before committing your code.
(코드를 커밋하기 전에 린터를 실행하라.)
```

**while — 동시성:**

```
While the batch job is running, don't restart the database.
(배치 잡이 실행 중인 동안 데이터베이스를 재시작하지 마라.)

While the frontend team works on the UI, the backend team will
build the API endpoints.
(프론트엔드 팀이 UI 작업을 하는 동안, 백엔드 팀이 API 엔드포인트를 만들 것이다.)
```

**so that — 목적:**

```
We added indexing so that queries would run faster.
(쿼리가 더 빠르게 실행되도록 인덱싱을 추가했다.)

Log every transaction so that we can audit them later.
(나중에 감사할 수 있도록 모든 트랜잭션을 로깅하라.)
```

#### 4. CODE — 개발자 비유로 재이해

**종속접속사 = 프로그래밍 제어 흐름 1:1 매핑**

```java
// if = if문
// "If the request fails, retry after 5 seconds."
if (request.fails()) {
    retry(Duration.ofSeconds(5));
}

// unless = if (!condition)
// "Unless you test it, don't deploy it."
if (!tested) {
    throw new DeploymentBlockedException("Test first!");
}

// when = @EventListener
// "When the user logs in, generate a session token."
@EventListener(UserLoginEvent.class)
public void onLogin(UserLoginEvent event) {
    sessionService.generateToken(event.getUser());
}

// while = while loop / concurrent execution
// "While the batch runs, don't restart the DB."
while (batchJob.isRunning()) {
    dbRestartService.block();
}

// because = 주석 또는 로깅
// "We chose Redis because it's fast."
// → Redis를 선택한 이유를 설명하는 것 = 코드 주석/ADR
/**
 * Architecture Decision Record
 * Decision: Use Redis for caching
 * Reason (because): It provides sub-millisecond latency
 */

// although = try-catch (예상과 다른 결과)
// "Although the test passed, it failed in production."
try {
    test.run();  // 테스트 통과 (although 앞)
    production.run();  // 프로덕션 실패 (although 뒤)
} catch (ProductionException e) {
    // 테스트는 통과했는데 프로덕션에서 실패하는 상황
    LOG.error("Test passed but production failed", e);
}

// before / after = @Before / @After (JUnit lifecycle)
// "Before you deploy, run all tests."
@BeforeEach
void setUp() {
    testRunner.runAll();  // before deploy
}

@AfterEach
void tearDown() {
    dataIntegrityChecker.verify();  // after migration
}

// so that = 의도/목적을 명시하는 주석
// "We added caching so that the API responds faster."
// → 목적 지향 프로그래밍
cache.put(key, value);  // so that subsequent reads are faster
```

**종속절의 어순 = 코드의 가독성 선택**

```java
// 종속절이 앞에 오는 패턴 (조건 먼저)
// "If the test passes, deploy."
if (test.passes()) {
    deploy();
}

// 종속절이 뒤에 오는 패턴 (행동 먼저)
// "Deploy if the test passes."
deploy().onlyIf(test::passes);

// 둘 다 같은 의미. 어떤 정보를 먼저 강조하느냐의 차이.
// 영어도 같다: 조건을 먼저 말할지, 행동을 먼저 말할지.
```

#### 5. DOMAIN — 세무/기술 도메인 실제 예문

**세무 도메인:**

```
if:
If the taxpayer's annual income exceeds 80 million won,
the highest tax bracket applies.
(납세자의 연간 소득이 8,000만 원을 초과하면 최고 세율이 적용된다.)

unless:
The penalty cannot be waived unless the taxpayer provides valid
documentation for the delay.
(납세자가 지연에 대한 유효한 서류를 제출하지 않으면 과태료를 면제받을 수 없다.)

because:
The deduction was rejected because the supporting documents
were incomplete.
(증빙 서류가 불완전했기 때문에 공제가 거부되었다.)

although:
Although the client filed on time, the return was flagged for
additional review due to unusually high deductions.
(고객이 제때 신고했음에도, 비정상적으로 높은 공제액으로 인해
추가 검토 대상으로 지정되었다.)

before:
Before submitting the tax return, verify all income sources
and applicable deductions.
(세금 신고서를 제출하기 전에, 모든 소득원과 적용 가능한 공제를 확인하라.)

so that:
We automated the withholding tax calculation so that accountants
can focus on complex cases.
(회계사가 복잡한 사례에 집중할 수 있도록 원천징수세 계산을 자동화했다.)
```

**기술 도메인:**

```
if:
If the circuit breaker is open, return a cached response instead.
(서킷 브레이커가 열려 있으면 캐시된 응답을 대신 반환하라.)

when:
When the Spring context loads, ShedLock acquires a distributed lock
to prevent duplicate batch execution.
(Spring 컨텍스트가 로드되면, ShedLock이 분산 잠금을 획득하여
중복 배치 실행을 방지한다.)

because:
We adopted the Outbox pattern because it guarantees at-least-once
delivery of domain events.
(도메인 이벤트의 최소 1회 전달을 보장하기 때문에 Outbox 패턴을 채택했다.)

although:
Although Kubernetes provides auto-scaling, we still need to set
proper resource limits.
(쿠버네티스가 오토스케일링을 제공하지만, 적절한 리소스 제한을 설정해야 한다.)

while:
While the primary database handles writes, the read replica
serves all read queries.
(주 데이터베이스가 쓰기를 처리하는 동안, 읽기 복제본이 모든 조회 쿼리를 처리한다.)
```

#### 6. PRACTICE — 직접 문장 만들기 연습

**A. 적절한 종속접속사를 고르시오**

```
1. _______ the API response is successful, parse the JSON body.
   → if / although / because / while

2. _______ we use connection pooling, creating new connections is expensive.
   → unless / because / although / when

3. _______ the migration script is running, no other writes should occur.
   → if / while / because / unless

4. The system handles the load well _______ we only have two servers.
   → because / while / although / unless

5. _______ you configure the firewall properly, the API will be exposed.
   → if / unless / although / when
```

**B. 두 문장을 종속접속사로 연결하시오 (적절한 접속사 선택)**

```
1. The build failed. + A dependency was missing.
   → __________________________________________________

2. Always back up the database. + You run a migration script.
   → __________________________________________________

3. The team worked overtime for two weeks. + They couldn't meet the deadline.
   → __________________________________________________

4. We implemented rate limiting. + Malicious users can't overwhelm the server.
   → __________________________________________________
```

**C. 한국어 → 영어 작문**

```
1. API 키가 만료되면 자동으로 갱신됩니다.
   → __________________________________________________

2. 테스트 커버리지가 80% 미만이면 PR을 머지할 수 없습니다.
   → __________________________________________________

3. 데이터 무결성이 중요하기 때문에 트랜잭션을 사용합니다.
   → __________________________________________________

4. 서버 용량을 늘렸는데도 불구하고 응답 시간이 개선되지 않았다.
   → __________________________________________________
```

<details>
<summary>정답 확인</summary>

**A.** 1-if, 2-because, 3-while, 4-although, 5-unless

**B.**
1. The build failed because a dependency was missing.
2. Always back up the database before you run a migration script.
3. Although the team worked overtime for two weeks, they couldn't meet the deadline.
4. We implemented rate limiting so that malicious users can't overwhelm the server.

**C.**
1. When the API key expires, it is automatically renewed.
2. Unless the test coverage is above 80%, you cannot merge the PR.
   (또는: You cannot merge the PR if the test coverage is below 80%.)
3. We use transactions because data integrity is important.
4. Although we increased the server capacity, the response time didn't improve.

</details>

---

### Lesson 23 — that절: 문장 안에 문장 넣기 (중첩 구조)

#### 1. WHY — 문장을 명사처럼 쓰는 기술

지금까지 배운 접속사들은 두 절을 **나란히 또는 종속적으로** 연결했다. 하지만 `that절`은 전혀 다른 일을 한다: **문장 자체를 명사로 바꿔서** 다른 문장 안에 집어넣는다.

```
사실: The API needs refactoring.
     (API가 리팩토링이 필요하다.)

내 생각: I think [사실].
        (나는 [사실]이라고 생각한다.)

합체: I think that the API needs refactoring.
     (나는 API가 리팩토링이 필요하다고 생각한다.)
```

`that`은 여기서 **"~라는 것"**이라는 의미로, 뒤따르는 문장 전체를 하나의 **명사 덩어리**로 만든다.

왜 이것이 필요한가? 우리가 전달하는 정보는 크게 두 가지다:

1. **사실/정보 자체**: "The server is down."
2. **그 사실에 대한 태도/판단**: "I believe / I know / I think / I doubt..."

That절은 이 두 가지를 **하나의 문장에 담는** 도구다.

#### 2. CORE — 핵심 개념을 가장 짧은 문장으로 체득

**that절의 기본 구조:**

```
주어 + 동사 + that + [완전한 문장]
```

**that절을 취하는 대표 동사들:**

| 동사 | 의미 | 예문 |
|------|------|------|
| think | 생각하다 | I think that it works. |
| believe | 믿다 | I believe that the fix is correct. |
| know | 알다 | I know that the deadline is Friday. |
| say | 말하다 | He said that the deploy succeeded. |
| hope | 바라다 | I hope that the tests pass. |
| expect | 기대하다 | We expect that traffic will increase. |
| assume | 가정하다 | I assume that the data is valid. |
| doubt | 의심하다 | I doubt that this will scale. |
| notice | 알아차리다 | I noticed that the log file was empty. |
| suggest | 제안하다 | I suggest that we add more logging. |
| confirm | 확인하다 | He confirmed that the issue was resolved. |

**that 생략 규칙:**

구어체/비격식에서는 `that`을 **생략**할 수 있다:

```
I think (that) the API needs refactoring.
I know (that) the deadline is Friday.
He said (that) the deploy succeeded.
```

하지만 **생략하면 안 되는 경우**:

1. that절이 **주어**일 때:
   ```
   That he passed the exam surprised everyone. (O)
   He passed the exam surprised everyone.      (X) ← 문장 구조 깨짐
   ```

2. **두 개의 that절**이 나열될 때:
   ```
   He said that the bug was fixed and that the tests all passed. (O)
   He said the bug was fixed and the tests all passed.           (?) ← 모호함
   ```

#### 3. EXPAND — 짧은 문장에서 복잡한 문장으로

**Step 1: 기본 — 생각/의견 표현**

```
I think that we need more tests.
I believe that this approach is better.
I know that the database is the bottleneck.
```

**Step 2: 보고/전달 — 간접화법**

```
The PM said that the deadline was moved to next Friday.
The client reported that the system was running slowly.
The team lead announced that we would migrate to Kubernetes.
```

**Step 3: 감정/반응**

```
I'm glad that the deployment was successful.
I'm surprised that the test coverage is so low.
I'm concerned that the memory usage keeps increasing.
```

**Step 4: It + be + 형용사 + that절 (가주어 구문)**

```
It is important that all tests pass before deployment.
(배포 전에 모든 테스트가 통과하는 것이 중요하다.)

It is clear that the architecture needs restructuring.
(아키텍처가 재구성이 필요한 것은 분명하다.)

It is unlikely that the bug will affect production.
(그 버그가 프로덕션에 영향을 미칠 가능성은 낮다.)

It is essential that the database be backed up daily.
(데이터베이스가 매일 백업되는 것이 필수적이다.)
```

주의: `It is essential/important/necessary that...` 구문에서 that절의 동사는 **원형(subjunctive)**을 쓴다:
```
It is essential that the data be encrypted. (O) ← 원형
It is essential that the data is encrypted. (?) ← 구어체에서는 가능
```

**Step 5: that절이 주어인 경우**

```
That the server crashed at 3 AM is suspicious.
(서버가 새벽 3시에 다운된 것은 수상하다.)
→ 이런 형태는 무거우므로, 보통 가주어 It으로 바꾼다:
→ It is suspicious that the server crashed at 3 AM.

That he finished the migration in one day impressed the entire team.
→ It impressed the entire team that he finished the migration in one day.
```

**Step 6: 다중 that절 — 복잡한 보고**

```
The postmortem revealed that the root cause was a memory leak,
that the monitoring system failed to detect it, and that the
on-call engineer was unreachable at the time of the incident.
(사후 분석 결과, 근본 원인은 메모리 누수였고, 모니터링 시스템이
이를 감지하지 못했으며, 사고 당시 온콜 엔지니어에게 연락이
닿지 않았다는 것이 밝혀졌다.)
```

**Step 7: that절 + 조동사/시제 변환**

```
I knew that the API would fail under heavy load.
(나는 API가 높은 부하에서 실패할 것을 알고 있었다.)

She hoped that the fix would resolve all the edge cases.
(그녀는 수정이 모든 엣지 케이스를 해결하기를 바랐다.)

We didn't realize that the bug had been in production for two weeks.
(우리는 그 버그가 2주간 프로덕션에 있었다는 것을 깨닫지 못했다.)
```

#### 4. CODE — 개발자 비유로 재이해

**that절 = 함수 안에 함수 호출 (Nested Call / Callback)**

```java
// 단순 문장 = 단순 함수 호출
deploy();  // "I deploy."

// that절 = 중첩 함수 호출
think(deploy());  // "I think that I should deploy."

// 더 풀어쓰면:
String fact = "the API needs refactoring";  // that절의 내용
String opinion = think(fact);  // 그 사실에 대한 생각
// → "I think that the API needs refactoring."
```

**that절 = 문장의 래핑(wrapping)**

```java
// 원본 데이터 (= 원본 문장)
String rawFact = "The server is down.";

// 래핑 (= that절로 감싸기)
Opinion wrapped = Opinion.of("I think", rawFact);
// → "I think that the server is down."

Report report = Report.of("He said", rawFact);
// → "He said that the server is down."

Surprise surprise = Surprise.of("I'm surprised", rawFact);
// → "I'm surprised that the server is down."

// 같은 사실(rawFact)을 다른 컨텍스트(think/said/surprised)로 감쌀 수 있다.
```

**that절 = DTO / Value Object로 문장을 객체화**

```java
// that절 = 문장을 "명사 덩어리"로 만든다 = 문장을 객체로 만든다

// 원래 문장: "The API needs refactoring."
// → 그냥 실행(statement)이면 사실의 선언.

// that절로 감싸면: 이 문장이 "객체"가 되어 다른 곳에 전달 가능
class ThatClause {
    String content;  // "the API needs refactoring"
}

// 주어 위치에 넣기
// "That the API needs refactoring is obvious."
functionCall(new ThatClause("the API needs refactoring"));

// 목적어 위치에 넣기
// "I think that the API needs refactoring."
iThink(new ThatClause("the API needs refactoring"));

// 보어 위치에 넣기
// "The problem is that the API needs refactoring."
Problem problem = new Problem(new ThatClause("the API needs refactoring"));
```

**It + be + adj + that = 팩토리 메서드 패턴**

```java
// "It is important that all tests pass."
// → "It"은 가주어(placeholder) = 팩토리의 정적 메서드

// 직접 생성 (that절이 주어 — 무겁고 읽기 어려움)
new Importance("that all tests pass").evaluate();

// 팩토리 사용 (It 구문 — 가볍고 읽기 쉬움)
Importance.of("that all tests pass");  // It is important that...

// 프로그래밍에서도 직접 new보다 팩토리 메서드가 더 읽기 좋듯이,
// 영어에서도 that절 주어보다 It 가주어가 더 자연스럽다.
```

**that 생략 = 타입 추론 (Type Inference)**

```java
// that 명시 = 타입 명시
// "I think that the API needs refactoring."
List<String> list = new ArrayList<String>();

// that 생략 = 타입 추론 (diamond operator)
// "I think the API needs refactoring."
List<String> list = new ArrayList<>();

// 문맥에서 충분히 추론 가능하면 생략 가능.
// 하지만 모호한 상황에서는 명시하는 것이 안전.
```

#### 5. DOMAIN — 세무/기술 도메인 실제 예문

**세무 도메인:**

```
The tax authority confirmed that the refund would be processed
within 30 business days.
(세무 당국이 환급이 30 영업일 이내에 처리될 것이라고 확인했다.)

I believe that this expense qualifies as a business deduction.
(이 비용이 사업 공제 대상이 된다고 생각합니다.)

It is important that all income sources be reported accurately.
(모든 소득원이 정확하게 신고되는 것이 중요하다.)

The accountant noticed that the withholding tax had been
calculated incorrectly.
(회계사가 원천징수세가 잘못 계산되었다는 것을 알아차렸다.)

We assumed that the client had already filed last year's return,
but it turned out that they hadn't.
(우리는 고객이 이미 작년 신고를 했을 것으로 가정했지만,
하지 않았던 것으로 밝혀졌다.)

The regulation states that all businesses with annual revenue
exceeding 500 million won must submit quarterly VAT returns.
(규정에 따르면 연매출 5억 원 이상의 모든 사업체는 분기별
부가가치세 신고서를 제출해야 한다.)
```

**기술 도메인:**

```
I think that we should migrate to a message queue architecture.
(메시지 큐 아키텍처로 마이그레이션해야 한다고 생각합니다.)

The monitoring dashboard shows that CPU usage has been above 90%
for the past hour.
(모니터링 대시보드에 의하면 CPU 사용량이 지난 1시간 동안
90% 이상이었다.)

It is essential that the Outbox table be polled at regular intervals.
(Outbox 테이블이 정기적으로 폴링되는 것이 필수적이다.)

We didn't realize that the SAGA compensating transaction had failed
silently.
(우리는 SAGA 보상 트랜잭션이 조용히 실패했다는 것을 깨닫지 못했다.)

The post-incident review revealed that the root cause was
a misconfigured ShedLock retention period.
(사고 후 검토 결과, 근본 원인은 잘못 설정된 ShedLock 보존 기간이었다.)

He suggested that we implement the circuit breaker pattern
to handle downstream service failures gracefully.
(그는 다운스트림 서비스 장애를 우아하게 처리하기 위해
서킷 브레이커 패턴을 구현할 것을 제안했다.)
```

#### 6. PRACTICE — 직접 문장 만들기 연습

**A. 두 문장을 that절로 합치시오**

```
1. I think + The database is the bottleneck.
   → __________________________________________________

2. The PM announced + We would release the feature next sprint.
   → __________________________________________________

3. It is clear + The current architecture cannot handle the load.
   → __________________________________________________

4. She noticed + The log file had been growing rapidly.
   → __________________________________________________
```

**B. that 생략이 가능한 문장에는 O, 불가능하면 X를 표시하시오**

```
1. I believe that the fix is correct.              → O / X
2. That the server crashed is concerning.           → O / X
3. He said that he would review the PR.            → O / X
4. It is important that all tests pass.             → O / X
5. He confirmed that the bug was fixed and
   that the patch was deployed.                     → O / X
```

**C. 한국어 → 영어 작문 (that절 사용)**

```
1. 나는 이 API가 리팩토링이 필요하다고 생각한다.
   → __________________________________________________

2. 그 엔지니어는 버그가 수정되었다고 보고했다.
   → __________________________________________________

3. 모든 마이크로서비스가 독립적으로 배포 가능한 것이 중요하다.
   → __________________________________________________

4. 우리는 데이터베이스 연결이 끊어진 것을 깨닫지 못했다.
   → __________________________________________________

5. 감사 결과, 3건의 세금 신고서에 오류가 있었다는 것이 밝혀졌다.
   → __________________________________________________
```

**D. 실무 이메일 작성: 다음 상황을 that절을 사용하여 영어로 쓰시오**

```
상황: 당신은 팀 리드에게 이메일을 쓰고 있다.
     배포 후 성능 저하가 발견되었다.
     원인은 N+1 쿼리 문제라고 판단한다.
     롤백을 제안하고 싶다.

Hi [Team Lead],

I noticed that __________________________________________

I believe that __________________________________________

I suggest that __________________________________________

Best regards,
[Your name]
```

<details>
<summary>정답 확인</summary>

**A.**
1. I think that the database is the bottleneck.
2. The PM announced that we would release the feature next sprint.
3. It is clear that the current architecture cannot handle the load.
4. She noticed that the log file had been growing rapidly.

**B.** 1-O (목적어 that절), 2-X (주어 that절), 3-O (목적어 that절), 4-X (가주어 구문), 5-X (나열된 that절은 명시하는 것이 좋음)

**C.**
1. I think that this API needs refactoring.
2. The engineer reported that the bug had been fixed.
3. It is important that all microservices be independently deployable.
4. We didn't realize that the database connection had been lost.
5. The audit revealed that three tax returns contained errors.

**D.** 예시 답안:
```
I noticed that the API response time has increased significantly
since yesterday's deployment.

I believe that the root cause is an N+1 query issue in the tax
calculation module.

I suggest that we roll back to the previous version while we
investigate and fix the underlying problem.
```

</details>

---

## Phase 3 총정리

### 배운 도구들과 프로그래밍 매핑

| 문법 도구 | 기능 | 프로그래밍 비유 |
|-----------|------|----------------|
| 조동사 (can, must, should...) | 동사에 모드 추가 | 미들웨어 / 인터셉터 |
| 조동사 과거형 (could, would) | 거리감 표현 | 추상화 레벨 |
| 관계대명사 (who, which, that) | 두 문장 합치기 | SQL JOIN |
| 관계부사 (where, when, why) | 맥락 정보 연결 | 메타데이터 쿼리 |
| 제한적/계속적 용법 | 필수 정보 vs 부가 정보 | WHERE절 vs 주석 |
| 등위접속사 (and, or, but) | 대등한 절 연결 | &&, \|\|, catch |
| 종속접속사 (if, when, because) | 종속 관계 표현 | if문, 이벤트 리스너 |
| that절 | 문장을 명사화 | 중첩 함수 호출 / DTO |

### Phase 3 자가 진단 체크리스트

```
[ ] 조동사로 능력/의무/가능성을 구분해서 표현할 수 있다.
[ ] could/would가 "과거"뿐 아니라 "공손/불확실"을 뜻한다는 것을 안다.
[ ] 관계대명사로 두 문장을 하나로 합칠 수 있다.
[ ] 관계대명사와 관계부사의 차이를 설명할 수 있다.
[ ] 콤마 유무(제한적/계속적)에 따른 의미 차이를 안다.
[ ] 등위접속사로 대등한 절을 연결할 수 있다.
[ ] 종속접속사로 조건/이유/양보를 표현할 수 있다.
[ ] that절로 문장을 명사화하여 다른 문장에 넣을 수 있다.
```

### 다음 단계: Phase 4

Phase 3에서 문장을 **복잡하게 만드는** 도구를 배웠다면, Phase 4에서는 이 복잡한 문장을 **더 자연스럽고 효율적으로 표현**하는 방법을 배운다: **준동사(to부정사, 동명사, 분사)**와 **비교 구문**.
