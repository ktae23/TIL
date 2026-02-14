# Phase 2: 문장 확장의 원리 (Week 3-5)

> 최소 문장을 만들 수 있으면, 거기에 블록을 끼우듯 확장한다.
> Phase 1에서 영어의 OS를 이해했다. 이제 그 위에 모듈을 올려보자.

---

## 목차

- [Week 3: to부정사](#week-3-to부정사--왜-to부정사라고-부르는가)
  - [Lesson 7: "부정사(不定詞)"의 진짜 의미](#lesson-7--부정사不定詞의-진짜-의미)
  - [Lesson 8: to의 핵심 이미지 "→ 방향"](#lesson-8--to의-핵심-이미지--방향)
  - [Lesson 9: to부정사 vs 동명사](#lesson-9--to부정사-vs-동명사-어떤-동사-뒤에-뭘-쓰는가)
- [Week 4: 동명사와 분사](#week-4-동명사와-분사--동사의-멀티-인터페이스)
  - [Lesson 10: 동명사](#lesson-10--동명사-동사가-명사-역할을-한다)
  - [Lesson 11: 현재분사 vs 과거분사](#lesson-11--현재분사-vs-과거분사--ing와--ed의-차이)
  - [Lesson 12: 분사구문](#lesson-12--분사의-확장-분사구문)
- [Week 5: 전치사](#week-5-전치사--영어의-관계-정의자)
  - [Lesson 13: 전치사의 중요성](#lesson-13--전치사를-모르면-영어-못한다)
  - [Lesson 14: 핵심 전치사 이미지](#lesson-14--핵심-전치사-이미지-마스터)
  - [Lesson 15: 전치사구](#lesson-15--전치사--명사--부사형용사-덩어리)

---

## Week 3: to부정사 — 왜 "to부정사"라고 부르는가

---

### Lesson 7 — "부정사(不定詞)"의 진짜 의미

#### WHY: 왜 "부정사"라는 이름이 붙었나

대부분의 학생이 "to부정사"를 그냥 이름으로 외운다. 하지만 이 이름 자체에 핵심이 들어 있다.

**부정(不定)** = "정해지지 않은"이라는 뜻이다.

무엇이 정해지지 않았나? **품사가 정해지지 않았다.**

영어에서 동사는 원래 "동사" 역할만 한다. 그런데 `to`를 붙이면 동사가 갑자기 **명사, 형용사, 부사** 중 어떤 역할이든 할 수 있게 된다. 어떤 역할을 할지는 문장에서의 위치에 따라 결정된다.

```
동사 "eat"
├── To eat is important.          → 명사 역할 (주어)
├── I need something to eat.      → 형용사 역할 (something 수식)
└── I came here to eat.           → 부사 역할 (목적)
```

왜 이런 게 필요했을까? 영어는 **동사를 그대로 명사 자리에 넣을 수 없는 언어**이기 때문이다.

- (X) `Eat is important.` — 동사가 주어 자리에 올 수 없다
- (O) `To eat is important.` — to를 붙여 변환하면 가능
- (O) `Eating is important.` — -ing를 붙여도 가능 (동명사, Lesson 10)

#### CORE: 가장 짧은 문장으로 이해

| 역할 | 최소 문장 | 분석 |
|------|-----------|------|
| 명사 (주어) | `To code is fun.` | 코딩하는 것 = 재밌다 |
| 명사 (목적어) | `I want to code.` | 나는 코딩하기를 원한다 |
| 명사 (보어) | `My dream is to code.` | 내 꿈 = 코딩하는 것 |
| 형용사 | `I need time to code.` | 코딩할 시간 (time을 수식) |
| 부사 (목적) | `I came to code.` | 코딩하려고 왔다 |
| 부사 (원인) | `I'm happy to code.` | 코딩해서 기쁘다 |

핵심 공식: **`to + 동사원형` = 동사를 다른 품사로 변환하는 장치**

#### EXPAND: 문장 확장

**Step 1** — 최소 문장
```
I want to learn.
```

**Step 2** — 목적어 추가
```
I want to learn English.
```

**Step 3** — 부사(목적) 추가
```
I want to learn English to communicate with global teams.
```

**Step 4** — 형용사적 to부정사 추가
```
I want to learn English to communicate with global teams,
and I need a plan to follow.
```

**Step 5** — 도메인 확장
```
I want to learn English to communicate with global teams,
and I need a structured plan to follow during the onboarding period.
```

#### CODE: 개발자 비유

to부정사는 **어댑터 패턴(Adapter Pattern)**이다.

```java
// 동사 "code"는 원래 동사 타입
Verb code = new Verb("code");

// to를 붙이면 어댑터가 다른 타입으로 변환
Noun toCode = new ToInfinitiveAdapter(code).asNoun();
Adjective toCode = new ToInfinitiveAdapter(code).asAdjective();
Adverb toCode = new ToInfinitiveAdapter(code).asAdverb();
```

또 다른 비유: **타입 캐스팅(Type Casting)**

```java
// Java에서 타입을 바꾸듯이
Object obj = "hello";
String str = (String) obj;  // 캐스팅

// 영어에서 to가 타입 캐스팅 역할
// "learn"이라는 동사를 명사로 캐스팅
// (Noun) learn → to learn
```

함수 자체를 값으로 다루는 **일급 함수(First-class Function)** 개념과도 비슷하다:

```javascript
// JavaScript에서 함수를 변수에 담듯이
const myTask = () => learn();

// 영어에서 to부정사로 동사를 명사처럼 다룸
// My task is to learn.
```

#### DOMAIN: 세무/기술 도메인 예문

**세무 도메인:**

| 영어 | 한국어 | to부정사 역할 |
|------|--------|--------------|
| `To file a tax return is mandatory.` | 세금 신고를 하는 것은 의무이다 | 명사 (주어) |
| `The taxpayer needs to submit documents.` | 납세자는 서류를 제출해야 한다 | 명사 (목적어) |
| `The deadline to file is March 31.` | 신고할 마감일은 3월 31일이다 | 형용사 (deadline 수식) |
| `We use this module to calculate tax.` | 세금을 계산하기 위해 이 모듈을 사용한다 | 부사 (목적) |
| `I'm glad to announce the tax refund.` | 세금 환급을 알리게 되어 기쁘다 | 부사 (원인) |

**기술 도메인:**

| 영어 | 한국어 | to부정사 역할 |
|------|--------|--------------|
| `To deploy without testing is risky.` | 테스트 없이 배포하는 것은 위험하다 | 명사 (주어) |
| `I want to refactor this service.` | 이 서비스를 리팩토링하고 싶다 | 명사 (목적어) |
| `We need a tool to monitor latency.` | 지연시간을 모니터링할 도구가 필요하다 | 형용사 (tool 수식) |
| `I stayed late to fix the production bug.` | 프로덕션 버그를 고치려고 늦게까지 남았다 | 부사 (목적) |

#### PRACTICE: 연습 문제

**연습 1: to부정사의 역할 판별**

다음 문장에서 to부정사가 명사/형용사/부사 중 어떤 역할인지 판별하세요.

1. `To understand the codebase takes time.`
2. `She wants to join the AI team.`
3. `I need a framework to build microservices.`
4. `He went to the office to review the PR.`
5. `The best way to learn is to practice.`

<details>
<summary>정답 보기</summary>

1. 명사 (주어) — "코드베이스를 이해하는 것은"
2. 명사 (목적어) — "AI팀에 합류하기를"
3. 형용사 (framework 수식) — "마이크로서비스를 만들 프레임워크"
4. 부사 (목적) — "PR을 리뷰하러"
5. to learn = 형용사 (way 수식), to practice = 명사 (보어)

</details>

**연습 2: 문장 만들기**

다음 한국어를 to부정사를 사용한 영어로 바꾸세요.

1. 세금 신고를 하는 것은 모든 시민의 의무이다.
2. 나는 Spring Batch를 배우고 싶다.
3. 우리는 데이터를 암호화할 방법이 필요하다.
4. 그는 코드 리뷰를 하려고 일찍 출근했다.

<details>
<summary>예시 답안</summary>

1. `To file a tax return is every citizen's duty.`
2. `I want to learn Spring Batch.`
3. `We need a way to encrypt the data.`
4. `He came to work early to do a code review.`

</details>

---

### Lesson 8 — to의 핵심 이미지: "→ 방향"

#### WHY: to는 왜 이렇게 많은 곳에 쓰이는가

영어에서 `to`는 놀라울 정도로 자주 등장한다:

- 전치사 to: `I went to the office.`
- to부정사: `I want to code.`
- 간접목적어 표시: `I sent the report to him.`

이 세 가지가 같은 단어인 이유는, **to의 근본 이미지가 하나**이기 때문이다:

> **to = 화살표 (→)**
> 어떤 것에서 다른 것을 향해 가리키는 방향

이 하나의 이미지에서 모든 용법이 파생된다:

```
to의 핵심 이미지: →  (방향/지향점)
│
├── 물리적 방향: I went → the office      (장소로 향함)
├── 심리적 방향: I want → to learn        (배움을 향함)
├── 대상 방향:   I sent it → to him       (그에게 향함)
├── 목적 방향:   I came → to help         (돕기 위해 향함)
└── 시간 방향:   from 9 → to 6           (6시를 향해)
```

#### CORE: 가장 짧은 문장으로 이해

| to의 용법 | 최소 문장 | 화살표 이미지 |
|-----------|-----------|--------------|
| 방향 (전치사) | `Go to Seoul.` | 서울 → 쪽으로 가라 |
| 대상 (전치사) | `Talk to me.` | 나 → 에게 말해 |
| 목적 (부정사) | `I came to help.` | 돕는 것 → 을 향해 왔다 |
| 의지 (부정사) | `I want to learn.` | 배우는 것 → 을 원한다 |
| 범위 (전치사) | `9 to 6` | 9에서 6 → 까지 |

핵심: **모든 `to`는 "→ 무언가를 가리킨다"는 공통점**이 있다.

#### EXPAND: 문장 확장

**Step 1** — 가장 기본적인 방향
```
I went to the office.
```

**Step 2** — 목적의 to 추가
```
I went to the office to fix the server.
```

**Step 3** — 대상의 to 추가
```
I went to the office to fix the server to show the fix to my manager.
```

이렇게 to가 여러 번 나와도 각각의 역할이 다르다:
- `to the office` → 장소 방향 (전치사)
- `to fix` → 목적 (to부정사, 부사)
- `to show` → 목적 (to부정사, 부사)
- `to my manager` → 대상 방향 (전치사)

**실전 확장 예시:**

```
I sent an email.
→ I sent an email to the client.
→ I sent an email to the client to explain the tax deduction.
→ I sent an email to the client to explain the tax deduction
   that applies to their corporate income.
```

#### CODE: 개발자 비유

`to`는 프로그래밍에서 **포인터(Pointer) / 참조(Reference)**와 같다.

```java
// 포인터는 무언가를 "가리킨다"
int* ptr = &value;  // ptr → value

// to도 무언가를 "가리킨다"
// "I went to the office" → office를 가리킴
// "I want to learn"     → learn을 가리킴
```

또 다른 비유: **URL/엔드포인트**

```
// HTTP 요청도 "어디로(to)" 향하는지가 핵심
GET /api/v1/tax-returns        → 세금신고서를 향해
POST /api/v1/deductions        → 공제를 향해
PUT /api/v1/users/{id}/status  → 상태를 향해

// I went to the office to fix the server.
// = 나는 [office 엔드포인트]로 [fix 작업]을 수행하러 갔다
```

**의존성 주입(DI)** 비유도 가능하다:

```java
// to = 의존성의 방향을 나타냄
// "I need you to help me"
// = "나" → "너"에게 → "도움" 의존
@Autowired
private HelpService helpService;  // to help = helpService에 대한 참조
```

#### DOMAIN: 세무/기술 도메인 예문

**세무 도메인 — to의 다양한 용법이 한 문단에:**

> The taxpayer went **to** the tax office **to** submit the return **to** the officer.
> According **to** the regulation, it is important **to** file on time.
> Failure **to** comply may lead **to** penalties.

분석:
| to | 역할 | 의미 |
|----|------|------|
| to the tax office | 전치사 (방향) | 세무서로 |
| to submit | to부정사 (목적) | 제출하기 위해 |
| to the officer | 전치사 (대상) | 담당자에게 |
| According to | 전치사 (관련) | ~에 따르면 |
| to file | to부정사 (주어 — 가주어 it) | 신고하는 것 |
| to comply | to부정사 (형용사) | 준수할 |
| to penalties | 전치사 (방향) | 벌금으로 |

**기술 도메인:**

```
// API 문서에서 자주 보는 to 패턴
"Use this endpoint to retrieve tax data."         (목적)
"Refer to the documentation for details."         (대상)
"The request is forwarded to the backend server."  (방향)
"You need to authenticate to access this API."     (조건)
```

#### PRACTICE: 연습 문제

**연습 1: to의 용법 구분**

다음 문장에서 각 `to`가 전치사인지 to부정사인지 구분하세요.

1. `I went to the meeting to discuss the new API.`
2. `She talked to the manager to request a deadline extension.`
3. `We need to migrate the data to the new server.`
4. `According to the spec, we need to add validation to the input.`

<details>
<summary>정답 보기</summary>

1. `to the meeting` (전치사-방향), `to discuss` (to부정사-목적)
2. `to the manager` (전치사-대상), `to request` (to부정사-목적)
3. `to migrate` (to부정사-목적어), `to the new server` (전치사-방향)
4. `to the spec` (전치사-관련), `to add` (to부정사-목적어), `to the input` (전치사-대상)

</details>

**연습 2: 하나의 문장에 to를 3번 이상 사용하여 문장 만들기**

힌트: 방향 to + 목적 to + 대상 to를 조합하세요.

<details>
<summary>예시 답안</summary>

- `I went to the office to send the report to the client.`
- `She came to Korea to explain the new tax regulation to the team.`
- `We need to deploy the update to production to deliver the feature to users.`

</details>

---

### Lesson 9 — to부정사 vs 동명사: 어떤 동사 뒤에 뭘 쓰는가

#### WHY: 왜 어떤 동사는 to를, 어떤 동사는 -ing를 쓰는가

이것은 영어 학습자가 가장 혼란스러워하는 부분 중 하나다. 왜 `want to do`이고 `enjoy doing`인가?

**핵심 원리:**

> - **to부정사** = 미래 지향적 (아직 안 한 것, 앞으로 할 것을 가리킨다 →)
> - **동명사(-ing)** = 현재/과거 지향적 (이미 하고 있거나 경험한 것)

이것은 to의 화살표 이미지(→ 앞을 향함)에서 자연스럽게 나온다:

```
to = → (미래 방향)
  "I want to learn."     = 아직 안 배운 것을 → 향해 원한다
  "I plan to travel."    = 아직 안 간 여행을 → 향해 계획한다
  "I decided to quit."   = 앞으로 그만둘 것을 → 향해 결정했다

-ing = 지금/이미 (현재/과거 경험)
  "I enjoy coding."      = 지금 하고 있는 코딩을 즐긴다
  "I finished debugging." = 이미 하던 디버깅을 끝냈다
  "I recall meeting him." = 과거에 만났던 것을 기억한다
```

#### CORE: 핵심 규칙

**to부정사를 쓰는 동사 (미래/의지/계획):**

| 동사 | 뉘앙스 | 예문 |
|------|--------|------|
| want | 원하다 (아직 안 한 것) | `I want to deploy tonight.` |
| plan | 계획하다 (미래) | `I plan to refactor next sprint.` |
| decide | 결정하다 (앞으로) | `We decided to use PostgreSQL.` |
| hope | 희망하다 (미래) | `I hope to finish by Friday.` |
| expect | 기대하다 (미래) | `I expect to receive the data.` |
| promise | 약속하다 (미래) | `I promise to fix this bug.` |
| agree | 동의하다 (앞으로) | `They agreed to extend the deadline.` |
| refuse | 거부하다 (앞으로) | `He refused to merge the PR.` |
| need | 필요하다 (아직 안 한 것) | `We need to update the schema.` |
| learn | 배우다 (앞으로) | `I'm learning to use Kubernetes.` |

**동명사를 쓰는 동사 (현재 경험/과거 회상):**

| 동사 | 뉘앙스 | 예문 |
|------|--------|------|
| enjoy | 즐기다 (하고 있는 것) | `I enjoy debugging complex issues.` |
| finish | 끝내다 (하던 것) | `I finished writing the test.` |
| avoid | 피하다 (하고 있는 것) | `Avoid using global variables.` |
| mind | 꺼리다 (하는 것) | `Do you mind reviewing my code?` |
| suggest | 제안하다 (하는 것) | `I suggest using a queue.` |
| consider | 고려하다 (하는 것) | `Consider adding retry logic.` |
| practice | 연습하다 (하는 것) | `Practice writing clean code.` |
| give up | 포기하다 (하던 것) | `Don't give up debugging.` |
| keep | 계속하다 (하고 있는 것) | `Keep monitoring the logs.` |
| admit | 인정하다 (한 것) | `He admitted breaking the build.` |

#### EXPAND: 의미가 달라지는 경우

**가장 유명한 예: stop**

```
I stopped to smoke.   = 담배를 피우기 위해 멈췄다 (to = 목적)
I stopped smoking.    = 담배 피우는 것을 멈췄다 (ing = 대상)
```

**remember / forget:**

```
I remembered to lock the server room.   = (미래) 서버실 잠그는 걸 기억했다 → 잠갔다
I remembered locking the server room.   = (과거) 서버실 잠근 걸 기억한다 → 잠근 기억이 있다

I forgot to commit the code.            = (미래) 커밋하는 걸 잊었다 → 안 했다
I forgot committing the code.           = (과거) 커밋한 걸 잊었다 → 했는데 기억 못 함
```

**try:**

```
I tried to fix the bug.    = 버그를 고치려고 시도했다 (성공 여부 불확실)
I tried restarting the server. = 서버 재시작을 시도해봤다 (실험적으로 해본 것)
```

#### CODE: 개발자 비유

```java
// to부정사 = Future<T> (아직 실행 안 된 미래의 작업)
CompletableFuture<Result> task = CompletableFuture.supplyAsync(() -> deploy());
// "I want to deploy" = 아직 실행 안 된 배포를 원한다

// 동명사 = Runnable/이미 실행 중인 작업
Thread runningTask = new Thread(() -> monitor());
runningTask.start();
// "I enjoy monitoring" = 이미 실행 중인 모니터링을 즐긴다
```

또 다른 비유:

```
to부정사 = Promise (아직 resolve 안 된 미래 값)
  "I plan to deploy"  →  new Promise((resolve) => deploy())

동명사   = 이미 실행 중이거나 완료된 것
  "I enjoy deploying" →  setInterval(() => deploy(), ...) // 반복 실행 중
  "I finished testing" → Promise.resolve(testResult)      // 이미 완료
```

#### DOMAIN: 세무/기술 도메인 예문

**세무 도메인:**

| 문장 | to/ing | 이유 |
|------|--------|------|
| `The client wants to claim a deduction.` | to (미래) | 아직 안 한 공제 신청 |
| `We plan to submit the tax return next week.` | to (미래) | 다음 주에 할 예정 |
| `The auditor finished reviewing the documents.` | -ing (완료) | 이미 하던 검토를 끝냄 |
| `Avoid declaring income incorrectly.` | -ing (경고) | 잘못 신고하는 행위를 피해라 |
| `Consider using the simplified tax form.` | -ing (제안) | 간편 서식 사용을 고려해라 |
| `I suggest filing electronically.` | -ing (제안) | 전자 신고를 제안한다 |

**기술 도메인:**

| 문장 | to/ing | 이유 |
|------|--------|------|
| `We decided to migrate to AWS.` | to (결정) | 앞으로의 마이그레이션 |
| `I need to update the dependency.` | to (필요) | 아직 안 한 업데이트 |
| `Avoid using deprecated APIs.` | -ing (경고) | 사용하는 행위를 피해라 |
| `Keep monitoring the error rate.` | -ing (계속) | 계속 하고 있는 모니터링 |
| `I finished implementing the feature.` | -ing (완료) | 이미 하던 구현을 끝냄 |

#### PRACTICE: 연습 문제

**연습 1: to 또는 -ing 선택**

괄호 안에서 올바른 형태를 고르세요.

1. `I want (to deploy / deploying) the hotfix tonight.`
2. `She enjoys (to write / writing) unit tests.`
3. `We decided (to use / using) Kafka for messaging.`
4. `Avoid (to push / pushing) directly to main.`
5. `I finished (to review / reviewing) the PR.`
6. `They agreed (to extend / extending) the deadline.`
7. `Consider (to add / adding) input validation.`
8. `I hope (to complete / completing) the migration by Friday.`

<details>
<summary>정답 보기</summary>

1. to deploy (want = 미래 지향)
2. writing (enjoy = 현재 경험)
3. to use (decide = 미래 결정)
4. pushing (avoid = 현재 행위를 피함)
5. reviewing (finish = 하던 것을 끝냄)
6. to extend (agree = 미래 동의)
7. adding (consider = 현재 고려)
8. to complete (hope = 미래 희망)

</details>

**연습 2: stop/remember/forget 의미 구분**

다음 두 문장의 의미 차이를 한국어로 설명하세요.

1. a) `I stopped to check the logs.`
   b) `I stopped checking the logs.`

2. a) `I remembered to write the test.`
   b) `I remembered writing the test.`

3. a) `I forgot to commit.`
   b) `I forgot committing.`

<details>
<summary>정답 보기</summary>

1. a) 로그를 확인하기 위해 (하던 일을) 멈췄다 → 로그를 확인했다
   b) 로그를 확인하는 것을 그만뒀다 → 더 이상 확인 안 함

2. a) 테스트를 작성할 것을 기억했다 → 테스트를 작성했다
   b) 테스트를 작성한 것을 기억한다 → 과거에 작성한 기억이 있다

3. a) 커밋하는 것을 잊었다 → 커밋을 안 했다
   b) 커밋한 것을 잊었다 → 커밋을 했는데 기억을 못 한다

</details>

---

## Week 4: 동명사와 분사 — 동사의 멀티 인터페이스

---

### Lesson 10 — 동명사: 동사가 명사 역할을 한다

#### WHY: 동명사는 왜 필요한가

Phase 1에서 배웠듯이, 영어는 **위치가 역할을 결정**하는 언어다. 주어 자리에는 명사만 올 수 있고, 목적어 자리에도 명사만 올 수 있다.

그런데 "코딩하는 것은 재밌다"처럼 **동작 자체**를 주어로 만들고 싶을 때가 있다. 이때 동사에 `-ing`를 붙여서 명사처럼 만드는 것이 **동명사**다.

to부정사(Lesson 7)도 명사 역할을 할 수 있었다. 차이점은?

| 특징 | to부정사 | 동명사 |
|------|---------|--------|
| 시간 감각 | 미래지향 (아직 안 한 것) | 현재/일반적 (이미 하는 것) |
| 느낌 | 구체적, 일회적 | 일반적, 반복적, 경험적 |
| 예시 | `To code is to create.` (격언적) | `Coding is fun.` (일반적 사실) |

한일 교수의 핵심 포인트: **동명사는 동사의 성질을 유지하면서 명사 자리에 앉는다.** 즉, 목적어를 가질 수도 있고, 부사의 수식을 받을 수도 있다.

```
Debugging complex systems requires patience.
         ↑ 목적어(complex systems)를 가짐 = 동사의 성질
↑ 주어 자리에 위치 = 명사의 역할
```

#### CORE: 가장 짧은 문장으로 이해

| 역할 | 최소 문장 | 분석 |
|------|-----------|------|
| 주어 | `Coding is fun.` | 코딩하는 것은 재밌다 |
| 목적어 | `I enjoy coding.` | 나는 코딩을 즐긴다 |
| 보어 | `My hobby is coding.` | 내 취미는 코딩이다 |
| 전치사의 목적어 | `I'm good at coding.` | 나는 코딩을 잘한다 |

**중요: 전치사 뒤에는 반드시 동명사!** (to부정사 X)

- (O) `I'm interested in learning Spring.`
- (X) `I'm interested in to learn Spring.`
- (O) `Thank you for helping.`
- (X) `Thank you for to help.`

왜? 전치사 뒤는 명사 자리 → 동명사(명사 역할)만 가능

#### EXPAND: 문장 확장

**Step 1** — 최소 문장
```
Debugging is important.
```

**Step 2** — 동명사에 목적어 추가 (동사 성질)
```
Debugging production issues is important.
```

**Step 3** — 동명사에 부사 추가
```
Debugging production issues quickly is important.
```

**Step 4** — 문장 뒤에 부사구 추가
```
Debugging production issues quickly is important for maintaining service reliability.
```

**Step 5** — 전치사 + 동명사 추가
```
Debugging production issues quickly is important for maintaining service reliability
without affecting user experience.
```

#### CODE: 개발자 비유

동명사는 **인터페이스 구현(implements)**이다.

```java
// 동사 "debug"는 원래 Verb 타입
// -ing를 붙이면 Noun 인터페이스를 구현한 것

// 원래 동사: void debug(Issue issue) { ... }
// 동명사: "debugging" = 이 함수 자체를 객체로 만든 것

// Java의 함수형 인터페이스와 비슷
Runnable debugging = () -> debug(issue);

// 이제 이 "debugging"을 변수처럼 어디든 넣을 수 있다
// 주어: Debugging is fun.        → Runnable task = debugging; // 변수에 할당
// 목적어: I enjoy debugging.     → execute(debugging);        // 파라미터로 전달
// 전치사 목적어: good at debugging → skillLevel.get(debugging); // 키로 사용
```

**Lambda vs Method Reference** 비유:

```java
// 동명사 = Method Reference
list.forEach(System.out::println);  // "printing" 자체를 전달
// "I enjoy printing." — 함수 자체를 값으로 전달

// to부정사 = Lambda (아직 실행 안 된 것)
CompletableFuture.supplyAsync(() -> fetchData());
// "I want to fetch data." — 미래에 실행할 것을 전달
```

#### DOMAIN: 세무/기술 도메인 예문

**세무 도메인:**

| 문장 | 동명사 역할 |
|------|------------|
| `Filing tax returns is mandatory for all businesses.` | 주어 |
| `The system supports calculating withholding tax automatically.` | 목적어 |
| `Her specialty is analyzing corporate tax structures.` | 보어 |
| `Thank you for submitting the documents on time.` | 전치사 목적어 |
| `Before filing, verify all the income sources.` | 전치사 목적어 |
| `After reviewing the deduction, the agent approved the claim.` | 전치사 목적어 |

**기술 도메인:**

| 문장 | 동명사 역할 |
|------|------------|
| `Refactoring legacy code improves maintainability.` | 주어 |
| `I recommend using connection pooling.` | 목적어 |
| `The bottleneck is processing large CSV files.` | 보어 |
| `We're responsible for maintaining the API gateway.` | 전치사 목적어 |
| `Instead of rewriting, consider extending the existing module.` | 전치사 목적어 |

#### PRACTICE: 연습 문제

**연습 1: 동명사를 사용하여 문장 완성**

1. _______ (write) clean code is a developer's responsibility.
2. She is good at _______ (solve) complex problems.
3. _______ (test) regularly prevents production bugs.
4. Thank you for _______ (review) my pull request.
5. Instead of _______ (delete) the data, archive it.

<details>
<summary>정답 보기</summary>

1. Writing clean code is a developer's responsibility.
2. She is good at solving complex problems.
3. Testing regularly prevents production bugs.
4. Thank you for reviewing my pull request.
5. Instead of deleting the data, archive it.

</details>

**연습 2: 세무 도메인 문장 만들기**

다음 동사를 동명사로 바꿔 세무 관련 문장을 만드세요: file, calculate, deduct, audit, report

<details>
<summary>예시 답안</summary>

- `Filing a tax return electronically saves time.`
- `Calculating the exact tax amount requires accurate data.`
- `Deducting business expenses can significantly reduce taxable income.`
- `Auditing financial records is part of the compliance process.`
- `Reporting income accurately is the taxpayer's legal obligation.`

</details>

---

### Lesson 11 — 현재분사 vs 과거분사: -ing와 -ed의 차이

#### WHY: 같은 동사에서 나온 두 형태가 왜 다른 의미를 갖는가

영어에서 동사 하나로 두 종류의 형용사를 만들 수 있다:

- **현재분사 (-ing)**: exciting, boring, confusing, surprising
- **과거분사 (-ed/-en)**: excited, bored, confused, surprised

왜 이런 구분이 있을까?

> **-ing = 능동/원인** → 그 감정을 "일으키는" 쪽
> **-ed = 수동/결과** → 그 감정을 "느끼는/당하는" 쪽

```
The bug is confusing.    → 버그가 혼란을 일으킨다 (버그 = 원인)
The developer is confused. → 개발자가 혼란을 느낀다 (개발자 = 결과/피해자)
```

이것은 **시점(관점)의 문제**다:
- -ing: 주어가 **그 작용을 하는** 관점 (능동)
- -ed: 주어가 **그 작용을 받는** 관점 (수동)

#### CORE: 핵심 대비표

| 동사 | -ing (원인/능동) | -ed (결과/수동) |
|------|-----------------|----------------|
| excite | exciting (흥미진진한) | excited (흥분한) |
| bore | boring (지루하게 하는) | bored (지루한) |
| confuse | confusing (혼란스럽게 하는) | confused (혼란스러운) |
| surprise | surprising (놀라운) | surprised (놀란) |
| tire | tiring (피곤하게 하는) | tired (피곤한) |
| interest | interesting (흥미로운) | interested (관심 있는) |
| satisfy | satisfying (만족스러운) | satisfied (만족한) |
| disappoint | disappointing (실망스러운) | disappointed (실망한) |
| frustrate | frustrating (좌절시키는) | frustrated (좌절한) |
| overwhelm | overwhelming (압도적인) | overwhelmed (압도당한) |

**기억법:**
- **-ing** → **사물/상황**에 주로 사용 (감정을 일으키는 원인)
- **-ed** → **사람**에 주로 사용 (감정을 느끼는 주체)

```
The meeting was boring.     (회의가 지루했다 → 회의 = 원인)
I was bored in the meeting. (내가 지루했다 → 나 = 결과)
```

#### EXPAND: 문장 확장

**현재분사의 확장 — 명사를 수식하는 형용사로:**

```
Step 1: The news is exciting.                    (보어)
Step 2: the exciting news                        (명사 앞 수식)
Step 3: the exciting news about the new project  (전치사구 추가)
Step 4: The exciting news about the new project spread quickly.  (완전한 문장)
```

**과거분사의 확장:**

```
Step 1: The developer is frustrated.                    (보어)
Step 2: the frustrated developer                        (명사 앞 수식)
Step 3: the frustrated developer on the team             (전치사구 추가)
Step 4: The frustrated developer on the team finally found the root cause.
```

**분사가 구(phrase)를 이끌 때 — 뒤에서 수식:**

```
The developer debugging the server looked tired.
            ↑ 현재분사구 (뒤에서 developer 수식)
= "서버를 디버깅하고 있는 개발자"

The code written by the intern needs review.
         ↑ 과거분사구 (뒤에서 code 수식)
= "인턴이 작성한 코드"
```

#### CODE: 개발자 비유

분사는 **Task/Thread의 상태**와 같다:

```java
// -ing = ACTIVE / RUNNING 상태 (능동적으로 작동 중)
Thread runningThread;     // "the running process" = 실행 중인 프로세스
Task processingTask;      // "the processing task" = 처리 중인 작업

// -ed = COMPLETED / DONE 상태 (작업이 완료된/처리된)
Task completedTask;       // "the completed task" = 완료된 작업
Data processedData;       // "the processed data" = 처리된 데이터
```

**Enum 상태머신으로 비유:**

```java
enum TaskStatus {
    PENDING,      // 대기 중
    PROCESSING,   // -ing: 처리하고 있는 (능동/진행)
    PROCESSED,    // -ed: 처리된 (수동/완료)
    FAILED        // 실패한
}

// "the processing request"  → status = PROCESSING  (진행 중)
// "the processed request"   → status = PROCESSED   (완료)
// "the failing test"        → status = FAILING      (실패하고 있는)
// "the failed deployment"   → status = FAILED       (실패한)
```

#### DOMAIN: 세무/기술 도메인 예문

**세무 도메인:**

| -ing (능동/원인) | -ed (수동/결과) |
|-----------------|----------------|
| `the taxing process` (과세 과정) | `the taxed income` (과세된 소득) |
| `the remaining balance` (남아있는 잔액) | `the submitted return` (제출된 신고서) |
| `the withholding amount` (원천징수하는 금액) | `the withheld tax` (원천징수된 세금) |
| `an increasing rate` (증가하는 세율) | `the updated regulation` (업데이트된 규정) |
| `the calculating module` (계산하는 모듈) | `the calculated deduction` (계산된 공제) |

**기술 도메인:**

| -ing (능동/진행) | -ed (수동/완료) |
|-----------------|----------------|
| `a running container` (실행 중인 컨테이너) | `a stopped container` (정지된 컨테이너) |
| `the loading screen` (로딩 중인 화면) | `the loaded data` (로드된 데이터) |
| `a blocking call` (블로킹하는 호출) | `a blocked thread` (블록된 스레드) |
| `the failing test` (실패하는 테스트) | `the failed deployment` (실패한 배포) |
| `an existing record` (존재하는 레코드) | `a deleted record` (삭제된 레코드) |

#### PRACTICE: 연습 문제

**연습 1: -ing 또는 -ed 선택**

1. The code review was very _______ (tire).
2. I'm _______ (confuse) by the error message.
3. This is an _______ (excite) new feature.
4. The team was _______ (disappoint) with the test results.
5. The _______ (remain) tasks need to be completed by Friday.
6. All _______ (process) transactions are stored in the archive.

<details>
<summary>정답 보기</summary>

1. tiring (코드 리뷰 = 피곤하게 하는 원인)
2. confused (나 = 혼란을 느끼는 주체)
3. exciting (기능 = 흥미를 일으키는 원인)
4. disappointed (팀 = 실망을 느끼는 주체)
5. remaining (작업 = 남아있는 상태, 능동)
6. processed (거래 = 처리된 상태, 수동)

</details>

**연습 2: 분사를 사용한 문장 만들기**

다음 동사의 -ing와 -ed 형태를 각각 사용하여 문장을 만드세요.

1. overwhelm → _______ / _______
2. update → _______ / _______

<details>
<summary>예시 답안</summary>

1. `The overwhelming amount of tax data slowed the system.` / `The overwhelmed developer asked for help.`
2. `The updating process takes about 30 minutes.` / `The updated module passed all tests.`

</details>

---

### Lesson 12 — 분사의 확장: 분사구문

#### WHY: 분사구문은 왜 존재하는가

영어는 **간결함을 추구하는 언어**다. 같은 내용을 더 짧게 표현할 수 있다면 그렇게 한다.

**분사구문 = 접속사 + 주어를 생략한 압축 표현**

```
원래: While I was reviewing the code, I found a bug.
축약: Reviewing the code, I found a bug.

생략된 것:
- 접속사 "While" 생략
- 주어 "I" 생략 (주절 주어와 동일)
- was 생략 (be동사 불필요)
```

왜 이런 압축이 가능한가?
1. 주어가 같으면 반복할 필요 없다 (DRY 원칙!)
2. 접속사의 의미는 문맥에서 유추 가능
3. 결과적으로 더 세련되고 간결한 문장이 된다

#### CORE: 핵심 변환 패턴

| 원래 문장 (접속사 + 절) | 분사구문 |
|------------------------|---------|
| **While** I was debugging, I found the issue. | **Debugging**, I found the issue. |
| **Because** she knew Java, she joined the team. | **Knowing** Java, she joined the team. |
| **After** he finished the task, he went home. | **Having finished** the task, he went home. |
| **When** it is used correctly, the tool saves time. | **Used** correctly, the tool saves time. |

**변환 규칙:**
1. 접속사 생략 (While, Because, When, After 등)
2. 주어 생략 (주절 주어와 동일할 때만!)
3. 동사를 분사로 변환:
   - 능동 → `-ing` (현재분사)
   - 수동 → `-ed/-en` (과거분사)
   - 완료 → `Having + p.p.`

#### EXPAND: 문장 확장

**능동 분사구문 (-ing):**

```
Step 1: Working late, I fixed the bug.
Step 2: Working late at the office, I fixed the critical production bug.
Step 3: Working late at the office on Friday night, I fixed the critical production bug
         that had been affecting the tax calculation module.
```

**수동 분사구문 (-ed):**

```
Step 1: Written in Java, the module runs on JVM.
Step 2: Written in Java 17, the module runs efficiently on modern JVMs.
Step 3: Written in Java 17 with Spring Boot, the tax calculation module
         runs efficiently on modern JVMs and supports cloud deployment.
```

**완료 분사구문 (Having + p.p.):**

```
Step 1: Having deployed the update, I went home.
Step 2: Having deployed the update to production, I went home relieved.
Step 3: Having successfully deployed the critical security update to production,
         I finally went home at midnight, relieved that no further issues arose.
```

#### CODE: 개발자 비유

분사구문은 **코드 리팩토링**이다. 중복을 제거하고 간결하게 만든다.

```java
// Before refactoring (접속사 + 완전한 절)
if (user != null && user.isActive()) {
    if (user.getRole().equals("ADMIN")) {
        grantAccess(user);
    }
}

// After refactoring (분사구문처럼 간결하게)
Optional.ofNullable(user)
    .filter(User::isActive)
    .filter(u -> u.getRole().equals("ADMIN"))
    .ifPresent(this::grantAccess);
```

또 다른 비유: **메서드 체이닝**

```java
// 분사구문 = 메서드 체이닝 (주어를 반복하지 않고 연속 실행)
result = data.stream()
    .filtering(...)     // "Filtering the data,"
    .mapping(...)       // "mapping each record,"
    .collecting(...);   // "we collected the results."

// = "Filtering the data and mapping each record, we collected the results."
```

#### DOMAIN: 세무/기술 도메인 예문

**세무 도메인:**

| 분사구문 | 원래 문장 |
|---------|----------|
| `Reviewing the financial statements, the auditor found discrepancies.` | While the auditor was reviewing... |
| `Filed electronically, the tax return is processed faster.` | When/If the tax return is filed electronically... |
| `Having submitted all documents, the taxpayer waited for the refund.` | After the taxpayer had submitted... |
| `Based on the submitted income data, the tax is calculated automatically.` | Because it is based on... |
| `Considering the new tax regulation, we need to update the system.` | Because we are considering... |

**기술 도메인:**

| 분사구문 | 원래 문장 |
|---------|----------|
| `Running on Kubernetes, the service scales automatically.` | Because it runs on Kubernetes... |
| `Written in Kotlin, the code is concise and null-safe.` | Because it was written in Kotlin... |
| `Having completed all unit tests, we moved to integration testing.` | After we had completed... |
| `Using Spring Batch, we process millions of tax records nightly.` | Because we use Spring Batch... |
| `Configured with ShedLock, the batch job runs only once across instances.` | When it is configured with ShedLock... |

#### PRACTICE: 연습 문제

**연습 1: 분사구문으로 변환**

다음 문장을 분사구문으로 바꾸세요.

1. `Because I didn't know the API, I read the documentation first.`
2. `After she had reviewed the code, she approved the PR.`
3. `When it is deployed to production, the service handles 10K requests per second.`
4. `While he was debugging the tax module, he discovered a calculation error.`

<details>
<summary>정답 보기</summary>

1. `Not knowing the API, I read the documentation first.`
2. `Having reviewed the code, she approved the PR.`
3. `Deployed to production, the service handles 10K requests per second.`
4. `Debugging the tax module, he discovered a calculation error.`

</details>

**연습 2: 분사구문을 원래 문장으로 복원**

1. `Working remotely, the team maintained high productivity.`
2. `Having migrated to the cloud, we reduced infrastructure costs by 40%.`
3. `Optimized for performance, the query returns results in under 100ms.`

<details>
<summary>정답 보기</summary>

1. `While the team was working remotely, they maintained high productivity.`
2. `After we had migrated to the cloud, we reduced infrastructure costs by 40%.`
3. `Because the query was optimized for performance, it returns results in under 100ms.`

</details>

---

## Week 5: 전치사 — 영어의 관계 정의자

---

### Lesson 13 — 전치사를 모르면 영어 못한다

#### WHY: 전치사가 왜 그렇게 중요한가

한일 교수가 반복해서 강조하는 것: **"전치사를 모르면 영어를 절대 못한다."**

왜? 영어에서 단어와 단어의 **관계**를 표현하는 거의 유일한 수단이 전치사이기 때문이다.

한국어에는 조사(~에, ~에서, ~으로, ~까지)가 있다. 영어에는 전치사(in, on, at, to, for, with, by...)가 그 역할을 한다.

```
한국어: 서울에서 → "에서" = 조사
영어:   in Seoul  → "in" = 전치사

한국어: 3시에 → "에" = 조사
영어:   at 3pm → "at" = 전치사
```

전치사가 없으면 영어 문장을 만들 수 없다:

- `I work __ Seoul.` → in? at? 전치사 없으면 의미 불완전
- `The meeting is __ Monday.` → on? 전치사 없으면 문장 불성립
- `I sent it __ you.` → to? for? 전치사에 따라 의미가 달라짐

#### CORE: in / on / at — 공간의 크기 원리

가장 많이 쓰이는 3개의 전치사 in, on, at의 핵심은 **공간의 크기/차원**이다:

```
in  = 3차원 공간 (안에 둘러싸인 느낌)     📦
on  = 2차원 표면 (위에 접촉한 느낌)       📋
at  = 0차원 점  (특정 지점)              📍
```

**공간에 적용:**

| 전치사 | 공간 크기 | 예시 |
|--------|----------|------|
| in | 큰 공간 (나라, 도시, 방) | `in Korea`, `in Seoul`, `in the office` |
| on | 표면 (길, 층) | `on the street`, `on the 3rd floor` |
| at | 지점 (특정 위치) | `at the bus stop`, `at the entrance` |

**시간에도 같은 원리:**

| 전치사 | 시간 크기 | 예시 |
|--------|----------|------|
| in | 긴 기간 (연, 월, 계절) | `in 2026`, `in March`, `in summer` |
| on | 특정 날 (요일, 날짜) | `on Monday`, `on March 15` |
| at | 시점 (시각) | `at 3pm`, `at noon`, `at midnight` |

**기억법: 크기가 클수록 in, 작아질수록 at으로 간다**

```
in 2026 → in March → on March 15 → on Monday → at 3pm → at the moment
(년)      (월)       (날짜)         (요일)      (시각)    (순간)
[  큰 기간  ─────────────────────────────── 작은 시점  ]
   in                    on                    at
```

#### EXPAND: 문장 확장

**Step 1** — 최소 문장
```
I work at the office.
```

**Step 2** — 시간 전치사 추가
```
I work at the office on weekdays.
```

**Step 3** — 더 구체적인 시간
```
I work at the office on weekdays in the morning.
```

**Step 4** — 장소 구체화
```
I work at the office in Seoul on weekdays in the morning.
```

**Step 5** — 도메인 확장
```
I work at the tax technology office in Seoul on weekdays,
usually arriving at 9am in the morning.
```

#### CODE: 개발자 비유

전치사는 **관계 연산자(Relational Operator)**다.

```java
// 프로그래밍에서 관계를 표현하듯
user.location = "Seoul";           // in Seoul
user.platform = "Linux";           // on Linux
user.position = new Point(37, 127); // at a specific point

// SQL에서도 전치사적 사고
SELECT * FROM employees
WHERE department IN ('engineering', 'design')  -- in = 범위 안에
  AND hired_on = '2024-01-15'                  -- on = 특정 날
  AND starts_at = '09:00';                     -- at = 특정 시점
```

**파일 시스템 비유:**

```
in = 디렉토리 안에 (폴더)
    in /Users/buzz/til/english/

on = 특정 레이어/표면에
    on the filesystem, on the network layer

at = 정확한 위치 (경로)
    at /Users/buzz/til/english/grammar-curriculum/phase2.md
```

#### DOMAIN: 세무/기술 도메인 예문

**세무 도메인:**

| 전치사 | 예문 | 의미 |
|--------|------|------|
| in | `In Korea, the tax year starts in January.` | 한국에서 / 1월에 |
| on | `The tax return must be filed on March 31.` | 3월 31일에 |
| at | `At the current tax rate, the liability is...` | 현재 세율에서 |
| in | `The deduction is listed in the financial statement.` | 재무제표 안에 |
| on | `Click on the 'Submit' button on the tax portal.` | 세무 포털 위의 |

**기술 도메인:**

| 전치사 | 예문 | 의미 |
|--------|------|------|
| in | `The bug exists in the payment module.` | 결제 모듈 안에 |
| on | `The app runs on AWS.` | AWS 위에 (플랫폼) |
| at | `The error occurs at line 42.` | 42번째 줄에서 |
| in | `I wrote it in Java.` | Java로 (언어 = 큰 환경) |
| on | `It depends on the Spring version.` | Spring 버전에 달려 있다 |

#### PRACTICE: 연습 문제

**연습 1: in, on, at 선택**

1. The meeting is ___ 2pm ___ Friday.
2. I live ___ Seoul, ___ the 5th floor.
3. The deadline is ___ March ___ 2026.
4. The bug is ___ line 127 ___ the UserService class.
5. We deploy ___ Mondays ___ midnight.

<details>
<summary>정답 보기</summary>

1. at 2pm / on Friday
2. in Seoul / on the 5th floor
3. in March / in 2026
4. at line 127 / in the UserService class
5. on Mondays / at midnight

</details>

---

### Lesson 14 — 핵심 전치사 이미지 마스터

#### WHY: 전치사는 왜 하나의 이미지로 이해해야 하는가

전치사를 뜻으로 외우면 끝이 없다. `for`의 뜻을 사전에서 찾으면 20개가 넘게 나온다. 하지만 **하나의 핵심 이미지**를 잡으면 모든 용법이 연결된다.

한일 교수의 접근법: **전치사 = 이미지. 뜻이 아니라 그림으로 기억하라.**

#### CORE: 10개 핵심 전치사의 이미지

| 전치사 | 핵심 이미지 | 시각화 |
|--------|-----------|--------|
| **in** | 안에 둘러싸임 | 📦 상자 안 |
| **on** | 표면에 접촉 | 📋 판 위에 |
| **at** | 한 점 | 📍 정확한 위치 |
| **to** | → 방향/도달 | ➡️ 화살표 |
| **for** | ⟶ 향해/위해 | 🎯 목표를 향해 |
| **with** | 함께/도구 | 🤝 나란히 |
| **by** | 바로 옆에 | 📐 근접/경유 |
| **from** | ← 출발점 | ⬅️ 시작점 |
| **of** | ~의/소속 | 🔗 연결/분리 |
| **about** | 주변을 맴돔 | 🔄 관련/대략 |

#### EXPAND: 각 전치사의 확장 용법

**for: "~를 향해/위해" 🎯**

핵심 이미지: 어떤 대상을 **향해** 나아감

```
물리적 방향:   I left for Seoul.              (서울을 향해 떠났다)
목적/이유:     This is for the client.         (클라이언트를 위한)
기간:          I waited for 3 hours.           (3시간 동안 → 3시간을 향해 기다림)
대상:          Is this for me?                 (이거 나를 위한 거야?)
교환:          I paid $100 for the license.    (라이선스를 위해 $100 지불)
```

**with: "함께/도구" 🤝**

핵심 이미지: 무언가와 **나란히** 있음

```
동반:    I work with the design team.       (디자인팀과 함께)
도구:    I built it with Spring Boot.       (Spring Boot를 가지고)
특성:    a server with 16GB RAM             (16GB RAM을 가진)
감정:    I'm happy with the result.         (결과에 만족 → 결과와 함께 행복)
```

**by: "옆에/~에 의해/~까지" 📐**

핵심 이미지: 바로 **옆, 근접**

```
위치:    the desk by the window             (창문 옆 책상)
수단:    sent by email                      (이메일에 의해 → 이메일로)
행위자:  built by the team                  (팀에 의해 만들어진)
기한:    by Friday                          (금요일까지 → 금요일 옆까지)
정도:    increased by 10%                   (10% 만큼)
```

**about: "주변을 맴돔" 🔄**

핵심 이미지: 핵심 주변을 **맴도는** 느낌

```
주제:    a book about tax law               (세법에 관한)
대략:    about 100 records                  (약 100개 → 100 주변)
관심:    I'm worried about the deadline.    (마감에 대해 걱정)
```

**of: "~의/소속/분리" 🔗**

핵심 이미지: A **of** B = B에서 **분리/소속**된 A

```
소속:    the head of engineering            (엔지니어링의 수장)
부분:    part of the system                 (시스템의 일부)
내용:    a list of items                    (항목들의 목록)
재료:    made of steel                      (강철로 된)
원인:    died of hunger                     (굶주림으로)
```

#### CODE: 개발자 비유

```java
// 전치사 = 프로그래밍의 관계 표현
class TaxReport {
    // of → 소속/포함
    List<Item> listOfDeductions;          // "list of deductions"

    // for → 목적/대상
    void calculateForClient(Client c);    // "calculate for the client"

    // with → 도구/동반
    Report buildWithTemplate(Template t); // "build with a template"

    // by → 수단/행위자
    Report generatedBySystem();           // "generated by the system"

    // about → 주제
    String descriptionAboutChanges;       // "description about changes"
}
```

**SQL 비유:**

```sql
-- for = WHERE (특정 대상을 위해)
SELECT * FROM deductions FOR UPDATE;

-- with = JOIN (함께)
SELECT * FROM users u WITH (NOLOCK);

-- by = GROUP BY / ORDER BY (기준)
SELECT * FROM transactions ORDER BY date;

-- of = 소속 관계
-- "part of the system" → table.column (테이블의 컬럼)
```

#### DOMAIN: 세무/기술 도메인 예문

**세무 도메인:**

| 전치사 | 예문 |
|--------|------|
| for | `The deduction is for business expenses.` |
| with | `File the return with supporting documents.` |
| by | `The tax must be paid by March 31.` |
| about | `The regulation about corporate income tax was revised.` |
| of | `The calculation of taxable income follows specific rules.` |
| from | `Income from overseas sources is also taxable.` |

**기술 도메인:**

| 전치사 | 예문 |
|--------|------|
| for | `This API is designed for mobile clients.` |
| with | `We integrated the service with Kafka.` |
| by | `The request was handled by the gateway.` |
| about | `Read the documentation about rate limiting.` |
| of | `The architecture of the microservice is event-driven.` |
| from | `The data is fetched from the external API.` |

#### PRACTICE: 연습 문제

**연습 1: 적절한 전치사 선택**

1. The report was generated ___ the system ___ the client.
2. I'm concerned ___ the performance ___ the API.
3. The module was built ___ React ___ the frontend team.
4. We need to submit the documents ___ the deadline.
5. This is a list ___ all taxable income sources ___ 2025.

<details>
<summary>정답 보기</summary>

1. by the system / for the client (시스템에 의해 / 고객을 위해)
2. about the performance / of the API (성능에 대해 / API의)
3. with React / by the frontend team (React로 / 프론트엔드 팀에 의해)
4. by the deadline (마감까지)
5. of all taxable income sources / from 2025 (과세 소득원의 / 2025년부터의)

</details>

---

### Lesson 15 — 전치사 + 명사 = 부사/형용사 덩어리

#### WHY: 전치사구는 왜 중요한가

영어 문장을 확장하는 가장 쉬운 방법이 **전치사구(prepositional phrase)**를 추가하는 것이다.

전치사구 = 전치사 + 명사. 이 덩어리가 문장에서 **부사** 또는 **형용사** 역할을 한다.

```
전치사구의 역할:
├── 부사 역할: 문장 전체나 동사를 수식 (언제, 어디서, 어떻게)
│   └── "I work in the morning."  (언제? → 아침에)
│   └── "She coded at the cafe."  (어디서? → 카페에서)
│
└── 형용사 역할: 명사를 수식 (어떤?)
    └── "the book on the desk"    (어떤 책? → 책상 위의)
    └── "the team in Seoul"       (어떤 팀? → 서울의)
```

이것이 왜 중요한가? **전치사구는 문장 어디에든 끼워넣을 수 있는 "플러그인 모듈"**이기 때문이다. 기본 문장에 전치사구만 추가하면 정보를 무한히 확장할 수 있다.

#### CORE: 기본 패턴

**부사 역할 (동사/문장 수식):**

| 질문 | 전치사구 | 예문 |
|------|---------|------|
| 어디서? (장소) | in/at/on + 장소 | `I debug in the office.` |
| 언제? (시간) | in/at/on + 시간 | `We deploy at midnight.` |
| 어떻게? (방법) | with/by + 수단 | `We monitor with Grafana.` |
| 왜? (이유) | for/because of + 이유 | `I stayed for the release.` |

**형용사 역할 (명사 수식):**

| 전치사구 | 수식하는 명사 | 의미 |
|---------|-------------|------|
| `the server in production` | server | 프로덕션에 있는 서버 |
| `the meeting on Friday` | meeting | 금요일 회의 |
| `the tool for monitoring` | tool | 모니터링을 위한 도구 |
| `the bug in the module` | bug | 모듈 안의 버그 |
| `the developer with 10 years of experience` | developer | 10년 경력의 개발자 |

#### EXPAND: 전치사구로 문장 확장하기

전치사구는 레고 블록처럼 쌓을 수 있다:

```
Step 1: I fixed a bug.
Step 2: I fixed a bug in the module.                    (+장소)
Step 3: I fixed a bug in the module on Monday.          (+시간)
Step 4: I fixed a bug in the module on Monday with help from the senior dev. (+방법)
Step 5: I fixed a bug in the tax calculation module on Monday morning
        with help from the senior developer on our team
        for the quarterly release.                       (+목적)
```

**5개의 전치사구가 하나의 문장에!** 이것이 영어 문장이 길어지는 원리다.

핵심: 영어 문장이 아무리 길어도 **S + V + O + 전치사구 + 전치사구 + 전치사구...** 구조다. 기본 골격(SVO)을 찾고, 나머지는 전치사구로 추가된 정보임을 인식하면 된다.

#### CODE: 개발자 비유

전치사구는 **메서드 체이닝의 설정 옵션** 또는 **빌더 패턴**이다.

```java
// 빌더 패턴처럼 전치사구를 하나씩 추가
String sentence = SentenceBuilder
    .subject("I")
    .verb("fixed")
    .object("a bug")
    .where("in the module")           // 전치사구 1: 장소
    .when("on Monday")                // 전치사구 2: 시간
    .how("with the debugger")         // 전치사구 3: 방법
    .why("for the release")           // 전치사구 4: 목적
    .build();
// → "I fixed a bug in the module on Monday with the debugger for the release."
```

**HTTP 요청 비유:**

```
// HTTP 요청도 기본 구조 + 추가 정보(헤더/파라미터)
GET /api/bugs                          → "I found bugs"
    ?module=payment                    → "in the payment module"
    &date=2026-02-14                   → "on February 14"
    &assignee=buzz                     → "by Buzz"
    &priority=critical                 → "with critical priority"
```

#### DOMAIN: 세무/기술 도메인 예문

**세무 도메인 — 전치사구가 풍부한 실전 문장:**

```
The taxpayer in Seoul filed a tax return
for the fiscal year 2025
on March 15
with electronic signature
through the NTS portal
without any errors.
```

분석:
| 전치사구 | 역할 | 의미 |
|---------|------|------|
| in Seoul | 형용사 (taxpayer 수식) | 서울에 있는 |
| for the fiscal year 2025 | 부사 (목적) | 2025 회계연도를 위해 |
| on March 15 | 부사 (시간) | 3월 15일에 |
| with electronic signature | 부사 (수단) | 전자서명으로 |
| through the NTS portal | 부사 (경로) | 국세청 포털을 통해 |
| without any errors | 부사 (상태) | 오류 없이 |

**기술 도메인:**

```
The API endpoint in the payment service
on the production server
for processing tax calculations
with rate limiting
by the engineering team in Seoul
handles over 10,000 requests per minute
without downtime.
```

#### PRACTICE: 연습 문제

**연습 1: 전치사구의 역할 판별**

다음 문장에서 밑줄 친 전치사구가 부사인지 형용사인지 판별하세요.

1. The developer `on our team` fixed the bug.
2. I deploy the code `at midnight`.
3. The document `in the shared folder` needs review.
4. We communicate `with Slack`.
5. The meeting `about the new feature` is `on Friday`.

<details>
<summary>정답 보기</summary>

1. 형용사 (developer를 수식 — "우리 팀의 개발자")
2. 부사 (deploy를 수식 — "자정에")
3. 형용사 (document를 수식 — "공유 폴더에 있는 문서")
4. 부사 (communicate를 수식 — "Slack으로")
5. about the new feature = 형용사 (meeting 수식), on Friday = 부사 (시간)

</details>

**연습 2: 전치사구를 추가하여 문장 확장**

기본 문장에 3개 이상의 전치사구를 추가하세요.

기본 문장: `The team deployed the update.`

<details>
<summary>예시 답안</summary>

`The team in Seoul deployed the update to the production server on Friday night with zero downtime for the tax filing season.`

전치사구:
- in Seoul (어떤 팀?)
- to the production server (어디로?)
- on Friday night (언제?)
- with zero downtime (어떻게?)
- for the tax filing season (왜?)

</details>

**연습 3: 긴 문장에서 SVO 골격 찾기**

다음 문장에서 기본 SVO를 찾고, 나머지 전치사구를 분리하세요.

`The senior developer on the platform team at our company in Seoul fixed a critical bug in the authentication module on Monday morning with a one-line patch for the quarterly security update.`

<details>
<summary>정답 보기</summary>

**SVO**: `The senior developer fixed a critical bug.`

전치사구:
- on the platform team (형용사 — developer 수식)
- at our company (형용사 — team 수식)
- in Seoul (형용사 — company 수식)
- in the authentication module (형용사 — bug 수식)
- on Monday morning (부사 — 시간)
- with a one-line patch (부사 — 수단)
- for the quarterly security update (부사 — 목적)

</details>

---

## Phase 2 마무리

### 핵심 요약표

| 문법 도구 | 핵심 비유 | 하는 일 |
|-----------|----------|---------|
| to부정사 | Adapter Pattern / Type Casting | 동사를 명사/형용사/부사로 변환 |
| to의 이미지 | Pointer / 화살표 (→) | 방향, 목적, 대상을 가리킴 |
| to vs -ing | Future vs Present/Past | 미래지향 vs 경험/현재 |
| 동명사 | Interface 구현 / Lambda | 동사에 명사 인터페이스 추가 |
| 현재분사 (-ing) | ACTIVE / RUNNING 상태 | 능동/진행 중인 상태 표현 |
| 과거분사 (-ed) | COMPLETED / DONE 상태 | 수동/완료된 상태 표현 |
| 분사구문 | 코드 리팩토링 / DRY 원칙 | 접속사+주어 생략하여 간결화 |
| 전치사 | 관계 연산자 | 단어 간의 관계 정의 |
| in/on/at | 3D/2D/0D (공간 크기) | 공간과 시간의 크기 구분 |
| 전치사구 | 빌더 패턴 / 플러그인 모듈 | 문장에 정보를 끼워넣어 확장 |

### 자기 점검 리스트

- [ ] to부정사가 명사/형용사/부사 중 어떤 역할인지 판별할 수 있다
- [ ] to부정사와 동명사의 시간 감각 차이를 설명할 수 있다
- [ ] stop/remember/forget + to와 + -ing의 의미 차이를 구분한다
- [ ] 현재분사(-ing)와 과거분사(-ed)가 각각 능동/수동인 이유를 안다
- [ ] 복잡한 문장을 분사구문으로 압축할 수 있다
- [ ] in/on/at을 공간 크기 원리로 구분할 수 있다
- [ ] for/with/by/about/of의 핵심 이미지를 그릴 수 있다
- [ ] 전치사구를 추가하여 기본 문장을 확장할 수 있다
- [ ] 긴 영어 문장에서 SVO 골격과 전치사구를 분리할 수 있다

### 다음 Phase 미리보기

**Phase 3: 문장을 복잡하게 만드는 도구들 (Week 6-8)**

Phase 2에서 단일 문장을 확장하는 법을 배웠다면, Phase 3에서는 **여러 문장을 결합**하는 방법을 배운다:
- 조동사: 동사에 "할 수 있다/해야 한다/할 것이다" 모드 추가
- 관계대명사: 두 문장을 하나로 합치는 SQL JOIN
- 접속사: if, when, because로 로직 흐름 제어
