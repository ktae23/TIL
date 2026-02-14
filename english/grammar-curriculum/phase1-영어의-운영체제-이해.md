# Phase 1: 영어의 운영체제 이해 (Week 1–2)

> 한국어와 영어는 완전히 다른 OS다. 코드를 짜기 전에 런타임을 이해하자.
> 이 Phase에서는 영어라는 언어가 **왜** 이런 구조를 갖게 되었는지,
> 그리고 그 구조가 한국어와 어떻게 다른지를 **개발자의 눈**으로 파악한다.

---

## 학습 목표

| 주차 | 핵심 질문 | 도달 목표 |
|------|-----------|-----------|
| Week 1 | 영어는 왜 어순이 곧 문법인가? | SVO 어순의 원리를 체득하고, 5형식을 "패턴"으로 이해한다 |
| Week 2 | 영어 단어의 "타입 시스템"은 무엇인가? | 품사를 자료형으로 인식하고, be동사/일반동사/시제의 원리를 안다 |

---

# Week 1: 왜 영어는 어순이 곧 문법인가

> "영어 문장을 읽는다는 것은, 배열을 인덱스 순서대로 순회하는 것과 같다."

---

## Lesson 1 — 영어는 "위치"로 역할이 정해지는 언어

### 1. WHY — 왜 영어는 위치가 중요한가

한국어를 먼저 생각해보자. 다음 세 문장은 어순이 다르지만 의미가 같다:

> - **내가** 너를 사랑한다.
> - 너를 **내가** 사랑한다.
> - 사랑한다, **내가** 너를.

한국어에서는 **조사**(은/는/이/가/을/를)가 각 단어의 역할을 표시해준다. "내**가**"라고 쓰면 어디에 놓든 주어다. "너**를**"이라고 쓰면 어디에 놓든 목적어다. 그래서 어순을 자유롭게 바꿀 수 있다.

영어는 다르다. 영어에는 조사가 없다. 그렇다면 어떻게 "누가 했는지"와 "누구에게 당했는지"를 구별할까?

**답: 위치(position)로 구별한다.**

```
I love you.    → I가 주어, you가 목적어
You love me.   → You가 주어, me가 목적어
```

`I`와 `You`는 둘 다 사람이다. 하지만 **동사(love) 앞에 오면 "하는 사람"**이 되고, **동사 뒤에 오면 "당하는 사람"**이 된다. 단어 자체가 아니라 **놓인 자리**가 의미를 결정한다.

이것이 영어 문법의 가장 근본적인 원리다:

> **영어에서 단어의 역할은 "위치"로 결정된다.**

#### 역사적 배경

사실 고대 영어(Old English, ~1100년 이전)에는 한국어처럼 격변화(case ending)가 있었다. 명사 끝에 붙는 어미로 주어/목적어를 구별했다. 그런데 노르만 정복(1066년) 이후 프랑스어와 섞이면서 이 격변화가 점점 사라졌다. 격변화가 사라지자, 영어는 "위치"에 의존해서 역할을 표시하는 언어로 진화한 것이다.

프로그래밍 언어로 비유하면, 옛 영어는 **named parameter**를 쓰던 언어였는데, 현대 영어는 **positional parameter**만 쓰는 언어로 바뀐 셈이다:

```python
# 옛 영어 (한국어) — named parameter, 순서 무관
love(subject="I", object="you")
love(object="you", subject="I")  # 같은 의미

# 현대 영어 — positional parameter, 순서가 곧 의미
love("I", "you")      # I가 주어
love("you", "I")      # You가 주어 → 의미 반전!
```

---

### 2. CORE — 핵심 개념을 최소 문장으로 체득

가장 짧은 문장으로 "위치 = 역할" 원리를 체감해보자.

| 문장 | 위치 [0] = 주어 | 위치 [1] = 동사 | 위치 [2] = 목적어 | 의미 |
|------|----------------|----------------|-------------------|------|
| `I hit you.` | I | hit | you | 내가 너를 때렸다 |
| `You hit me.` | You | hit | me | 네가 나를 때렸다 |
| `Dogs chase cats.` | Dogs | chase | cats | 개가 고양이를 쫓는다 |
| `Cats chase dogs.` | Cats | chase | dogs | 고양이가 개를 쫓는다 |

같은 단어, 같은 동사인데 **위치만 바뀌면 의미가 완전히 뒤집힌다**. 이것을 몸에 새기자.

> **규칙: 동사 앞 = 하는 놈(주어), 동사 뒤 = 당하는 놈(목적어)**

#### 연습: 다음 문장의 주어와 목적어를 즉시 파악하라

1. `The manager approved the request.`
2. `The request surprised the manager.`
3. `An error crashed the server.`
4. `The server logged an error.`

→ 동사 앞/뒤만 보면 된다. 한국어처럼 조사를 찾을 필요 없다.

---

### 3. EXPAND — 짧은 문장에서 확장

짧은 문장에 살을 붙여가며 확장해보자. 핵심은 **기본 골격(S-V-O)은 절대 안 변한다**는 것이다.

**Step 1: 최소 문장**
```
I fixed the bug.
```

**Step 2: 언제(시간) 추가**
```
I fixed the bug yesterday.
```

**Step 3: 어디서(장소) 추가**
```
I fixed the bug in the production server yesterday.
```

**Step 4: 어떻게(방법) 추가**
```
I quickly fixed the bug in the production server yesterday.
```

**Step 5: 왜(이유) 추가**
```
I quickly fixed the bug in the production server yesterday because the client complained.
```

아무리 길어져도 뼈대를 보라:

```
[I] [fixed] [the bug] in the production server yesterday because the client complained.
 S     V        O       ← 나머지는 전부 "수식어/부가정보"
```

> **영어 문장이 아무리 길어도, S-V-O 골격을 찾으면 핵심 의미를 즉시 파악할 수 있다.**

이것이 영어 독해의 핵심 기술이다. 긴 문장을 만나면 당황하지 말고, **주어-동사-목적어**만 먼저 찾아라.

---

### 4. CODE — 개발자 비유로 재이해

#### 비유 1: 한국어 = HashMap, 영어 = Array

```java
// 한국어: HashMap — 키(조사)로 역할을 찾는다
Map<String, String> korean = new HashMap<>();
korean.put("주어", "내가");     // "가"가 주어 표시
korean.put("목적어", "너를");   // "를"이 목적어 표시
korean.put("동사", "사랑한다");
// 순서를 바꿔도 키로 찾으니까 상관없다

// 영어: Array — 인덱스(위치)로 역할을 찾는다
String[] english = {"I", "love", "you"};
// english[0] = 주어 (항상!)
// english[1] = 동사 (항상!)
// english[2] = 목적어 (항상!)
// 순서를 바꾸면 의미가 달라진다!
```

#### 비유 2: SQL의 SELECT 문

영어 어순은 SQL과 매우 닮았다:

```sql
-- 영어식 사고: "누가 → 뭘 했다 → 어디서 → 언제"
SELECT result FROM table WHERE condition;

-- 한국어식 사고: "어디서 → 조건이 → 뭘 → 선택한다"
-- 만약 SQL이 한국어 어순이었다면:
FROM table WHERE condition result SELECT;
```

#### 비유 3: 함수 호출

```javascript
// 영어 문장 = 함수 호출
// "I sent him the report"

// 주어.동사(목적어1, 목적어2)
I.sent(him, theReport);

// 주어가 호출자, 동사가 메서드, 목적어가 인자
```

---

### 5. DOMAIN — 세무/기술 도메인 예문

실제 업무에서 쓸 법한 문장으로 "위치 = 역할" 원리를 확인하자.

#### 세무 도메인

| 문장 | S (주어) | V (동사) | O (목적어) |
|------|---------|---------|-----------|
| The taxpayer filed the return. | The taxpayer | filed | the return |
| The NTS reviewed the documents. | The NTS | reviewed | the documents |
| The accountant calculated the deduction. | The accountant | calculated | the deduction |
| A penalty exceeded the original tax. | A penalty | exceeded | the original tax |

> **NTS** = National Tax Service (국세청)

#### 기술 도메인

| 문장 | S (주어) | V (동사) | O (목적어) |
|------|---------|---------|-----------|
| The scheduler triggered the batch job. | The scheduler | triggered | the batch job |
| The API returned an error response. | The API | returned | an error response |
| The load balancer distributed the requests. | The load balancer | distributed | the requests |
| Our microservice processes tax calculations. | Our microservice | processes | tax calculations |

#### 복합 도메인 (세무 + 기술)

```
The tax engine validated the income data before calculating withholding amounts.
[S: The tax engine] [V: validated] [O: the income data] [부가: before calculating withholding amounts]
```

```
Our API automatically generates tax reports for registered businesses every quarter.
[S: Our API] [V: generates] [O: tax reports] [부가: for registered businesses every quarter]
```

---

### 6. PRACTICE — 연습 문제

#### 연습 A: 주어-동사-목적어 찾기

다음 문장에서 S, V, O를 찾아 표시하시오.

1. `The system processed 10,000 tax returns overnight.`
2. `Our team deployed the new tax calculation module last Friday.`
3. `The client reported a discrepancy in the withholding tax amount.`
4. `Spring Batch executed the year-end settlement job successfully.`
5. `The tax authority imposed a late filing penalty on the corporation.`

#### 연습 B: 어순 배열

주어진 단어들을 올바른 영어 어순으로 배열하시오.

1. `[the return / submitted / the taxpayer / electronically]`
2. `[every midnight / runs / the cron job / automatically]`
3. `[the tax rate / for small businesses / reduced / the government]`
4. `[our system / in real-time / calculates / withholding tax]`

#### 연습 C: 위치 바꾸기 — 의미 변화 확인

아래 문장의 주어와 목적어를 바꿔 새 문장을 만들고, 의미가 어떻게 달라지는지 서술하시오.

1. `The auditor questioned the taxpayer.`
   → `The taxpayer questioned the auditor.` (의미: ____________)

2. `The API called the external service.`
   → `The external service called the API.` (의미: ____________)

3. `The penalty exceeded the profit.`
   → `The profit exceeded the penalty.` (의미: ____________)

#### 연습 D: 한국어 → 영어 전환

한국어를 영어 어순(S-V-O)으로 변환하시오. (핵심 골격만 먼저, 그 다음 부가정보)

1. "국세청이 세무조사를 실시했다."
2. "우리 팀이 어제 배치 작업을 수정했다."
3. "그 납세자는 매달 부가가치세를 신고한다."
4. "이 모듈은 원천징수세를 자동으로 계산한다."

---

## Lesson 2 — SVO: 영어의 기본 실행 순서

### 1. WHY — 왜 주어-동사-목적어 순서인가

모든 프로그래밍 언어에는 **실행 순서(execution order)**가 있다. JavaScript는 위에서 아래로, SQL은 FROM → WHERE → SELECT 순서로 내부 실행된다. 마찬가지로, 인간 언어에도 "사고의 실행 순서"가 있고, 그것이 어순으로 나타난다.

영어의 실행 순서는 이렇다:

```
1단계: 누가? (Subject)    → "The developer"
2단계: 뭘 했다? (Verb)    → "deployed"
3단계: 뭘? (Object)       → "the hotfix"
4단계: 부가정보            → "to the production server at 3 AM"
```

**가장 중요한 정보부터 먼저 말한다.** 이것이 영어의 근본 철학이다.

반면 한국어는:

```
1단계: 배경/맥락          → "새벽 3시에 프로덕션 서버에"
2단계: 뭘? (Object)       → "핫픽스를"
3단계: 누가? (Subject)    → "개발자가"
4단계: 뭘 했다? (Verb)    → "배포했다"
```

한국어는 배경을 먼저 깔고, 동사(결론)를 마지막에 말한다. **가장 중요한 정보(결론)가 문장 끝에 온다.**

이 차이가 한국인이 영어를 어려워하는 근본 원인이다. 단순히 "단어를 몰라서"가 아니라, **사고의 순서 자체가 반대**이기 때문이다.

#### 영어식 사고 = Early Return 패턴

개발자라면 이 패턴을 알 것이다:

```java
// Early Return 패턴 (영어식 사고)
public Result process(Request request) {
    if (request == null) return Result.error("null request");  // 결론 먼저
    if (!request.isValid()) return Result.error("invalid");     // 결론 먼저
    // ... 나머지 처리
    return Result.success(data);  // 결론 먼저
}

// 한국어식 사고 (중첩 조건문)
public Result process(Request request) {
    if (request != null) {          // 배경 깔기
        if (request.isValid()) {    // 배경 더 깔기
            // ... 처리 ...
            return Result.success(data);  // 결론이 가장 안쪽
        }
    }
    return Result.error("failed");
}
```

영어 화자는 **결론/핵심을 먼저 말하고**, 부가 설명을 뒤에 붙인다. 이것이 SVO 어순의 본질이다.

---

### 2. CORE — 핵심 개념을 최소 문장으로 체득

SVO의 세 요소를 가장 짧은 문장으로 확인하자.

| 패턴 | 문장 | 분석 |
|------|------|------|
| S + V | `I work.` | 나는 일한다 |
| S + V + O | `I write code.` | 나는 코드를 쓴다 |
| S + V + O + 부가 | `I write code daily.` | 나는 매일 코드를 쓴다 |

핵심 규칙:

> 1. **주어는 반드시 동사 앞에 온다.**
> 2. **목적어는 반드시 동사 뒤에 온다.**
> 3. **부가정보(시간, 장소, 방법, 이유)는 문장 끝에 온다.**

이 세 가지만 지키면 영어 문장의 80%는 만들 수 있다.

#### "핵심 먼저" 사고 훈련

한국어 문장을 볼 때, **결론부터 뽑아내는 연습**을 하자:

```
한국어: "어제 회사에서 팀장님이 새로운 프로젝트를 발표했다."
         ↓ 결론부터 뽑기
핵심:    팀장님이 + 발표했다 + 새로운 프로젝트를
         ↓ 영어 어순 (SVO)
영어:    The team lead announced a new project at the office yesterday.
         [S]              [V]        [O]              [부가]
```

---

### 3. EXPAND — 짧은 문장에서 확장

SVO 골격에 정보를 하나씩 추가하는 확장 연습이다.

#### 확장 패턴 1: 뒤로 늘리기 (가장 자연스러운 방식)

```
We deploy code.
We deploy code every Friday.
We deploy code every Friday after the code review.
We deploy code every Friday after the code review using Jenkins.
We deploy code every Friday after the code review using Jenkins to the production environment.
```

골격 `[We] [deploy] [code]`는 변하지 않고, 뒤에 정보가 계속 추가된다.

#### 확장 패턴 2: 주어를 구체화하기

```
The developer fixed the bug.
The senior backend developer fixed the bug.
The senior backend developer on our tax team fixed the bug.
The senior backend developer on our tax team who joined last month fixed the bug.
```

주어 `The developer`가 점점 구체적으로 변한다. 하지만 **동사 `fixed`는 항상 주어 바로 다음에 온다**는 원칙은 동일하다.

> 이것이 영어를 읽을 때 중요한 포인트다. 주어가 아무리 길어도, **동사를 찾으면 그 앞이 전부 주어**라는 뜻이다.

#### 확장 패턴 3: 목적어를 구체화하기

```
I reviewed the code.
I reviewed the code that handles tax calculations.
I reviewed the code that handles tax calculations for individual income tax returns.
I reviewed the code that handles tax calculations for individual income tax returns submitted through our API.
```

---

### 4. CODE — 개발자 비유로 재이해

#### 비유 1: SVO = `subject.verb(object)`

영어 문장은 객체지향 프로그래밍의 메서드 호출과 같다:

```java
// 영어 문장:    "The server processes the request."
// 코드로 변환:
server.process(request);

// 영어 문장:    "The scheduler sends the notification to the admin."
// 코드로 변환:
scheduler.send(notification, admin);

// 영어 문장:    "Our API validates the tax data before saving."
// 코드로 변환:
if (api.validate(taxData)) {
    api.save(taxData);
}
```

#### 비유 2: 부가정보 = 어노테이션/데코레이터

SVO 뒤에 붙는 부가정보는 코드의 어노테이션이나 데코레이터와 같다. 핵심 로직을 변경하지 않고 메타데이터를 추가하는 것이다:

```java
// "I deployed the service to production yesterday using Docker."

@When("yesterday")
@Where("production")
@Using("Docker")
I.deploy(service);

// 어노테이션을 다 빼도 핵심 로직은 동일:
I.deploy(service);
```

#### 비유 3: 한국어 → 영어 = 스택 → 큐

한국어는 **스택(Stack)**처럼 작동한다. 정보를 쌓아 올리다가 마지막(top)에 결론(동사)이 온다:

```
push("어제")
push("회사에서")
push("버그를")
pop() → "고쳤다"  // 결론이 마지막에
```

영어는 **큐(Queue)**처럼 작동한다. 핵심부터 먼저 나오고, 부가정보가 뒤따른다:

```
dequeue() → "I fixed"          // 결론이 먼저
dequeue() → "the bug"          // 대상
dequeue() → "at the office"    // 장소
dequeue() → "yesterday"        // 시간
```

---

### 5. DOMAIN — 세무/기술 도메인 예문

#### 세무 도메인 — SVO 구조 분석

```
The National Tax Service audited the company's financial records.
[S: The NTS]  [V: audited]  [O: the company's financial records]
```

```
The taxpayer claimed an excessive deduction on the income tax return.
[S: The taxpayer]  [V: claimed]  [O: an excessive deduction]  [부가: on the income tax return]
```

```
The revised tax law reduced the corporate tax rate from 25% to 22%.
[S: The revised tax law]  [V: reduced]  [O: the corporate tax rate]  [부가: from 25% to 22%]
```

#### 기술 도메인 — SVO 구조 분석

```
The message queue buffered 50,000 events during the peak hour.
[S: The message queue]  [V: buffered]  [O: 50,000 events]  [부가: during the peak hour]
```

```
The circuit breaker prevented cascading failures across microservices.
[S: The circuit breaker]  [V: prevented]  [O: cascading failures]  [부가: across microservices]
```

#### 세무 기술 도메인 — 실무 문장

```
Our tax calculation engine processes withholding tax for 500,000 employees every month.
[S: Our tax calculation engine]  [V: processes]  [O: withholding tax]
[부가: for 500,000 employees every month]
```

```
The batch job reconciles the tax payment records with the NTS database nightly.
[S: The batch job]  [V: reconciles]  [O: the tax payment records]
[부가: with the NTS database nightly]
```

---

### 6. PRACTICE — 연습 문제

#### 연습 A: SVO 골격 추출

다음 긴 문장에서 S-V-O 골격만 추출하시오.

1. `Our newly developed microservice efficiently handles complex tax calculations for individual taxpayers across all regions.`
2. `The experienced tax accountant carefully reviewed the quarterly VAT returns submitted by the client last week.`
3. `The automated testing pipeline detected three critical bugs in the tax rate calculation module before deployment.`

#### 연습 B: 확장 연습

주어진 최소 문장을 5단계로 확장하시오 (한 단계마다 정보 하나씩 추가).

1. 시작: `The system calculates tax.`
2. 시작: `We deployed the service.`
3. 시작: `The API returns data.`

#### 연습 C: 한국어 → 영어 (SVO 전환)

다음 한국어를 "핵심 먼저" 원칙으로 영어 SVO 문장으로 전환하시오.

1. "지난 분기에 우리 팀이 세금 계산 엔진을 전면 리팩토링했다."
2. "국세청이 작년에 대기업 300곳에 대해 세무조사를 실시했다."
3. "매일 자정에 배치 작업이 원천징수 데이터를 자동으로 처리한다."
4. "이 서비스는 사업자 등록 정보를 실시간으로 검증한다."

#### 연습 D: 영어식 사고 전환

다음 상황을 "결론 먼저" 영어식으로 한 문장으로 말하시오.

1. 상황: 어제, 프로덕션 서버에서, 메모리 누수 때문에, 서비스가, 다운되었다.
2. 상황: 이번 주, 새로운 세율 적용 로직을, 우리 팀이, Kafka를 사용해서, 구현했다.
3. 상황: 분기마다, 종합소득세 신고 데이터를, 우리 시스템이, NTS API를 통해, 전송한다.

---

## Lesson 3 — 영어 문장의 5가지 설계 패턴 (5형식)

### 1. WHY — 왜 하필 5개인가

영어 문장이 수십억 개 존재하지만, 그 구조를 분석하면 **딱 5가지 패턴**으로 수렴한다. 이것은 프로그래밍에서 **디자인 패턴**과 같다. GoF 디자인 패턴이 23개로 대부분의 설계 문제를 커버하듯, 영어의 5형식이 모든 문장 구조를 커버한다.

왜 5개일까? 그것은 **동사의 성질**에 따라 결정된다.

동사(함수)가 어떤 종류의 "인자(argument)"를 필요로 하느냐에 따라 문장 구조가 달라진다:

| 동사 유형 | 필요한 인자 | 형식 |
|-----------|------------|------|
| 자동사 (인자 불필요) | 없음 | 1형식 |
| 연결동사 (상태 서술) | 보어 1개 | 2형식 |
| 타동사 (대상 필요) | 목적어 1개 | 3형식 |
| 수여동사 (주고받기) | 목적어 2개 | 4형식 |
| 불완전타동사 (대상 + 상태) | 목적어 + 보어 | 5형식 |

이것은 함수의 **시그니처(signature)**와 정확히 같은 개념이다:

```typescript
// 1형식: 인자 없음
function run(): void

// 2형식: 주어의 상태를 설명하는 보어 1개
function is(complement: State): void

// 3형식: 목적어 1개
function fix(target: Bug): void

// 4형식: 목적어 2개 (간접 + 직접)
function send(recipient: Person, item: Report): void

// 5형식: 목적어 + 그 목적어의 상태
function make(target: Thing, state: State): void
```

---

### 2. CORE — 핵심 개념을 최소 문장으로 체득

각 형식의 가장 짧은 문장을 외우자. 이것이 **템플릿**이 된다.

#### 1형식: S + V (주어 + 동사)

> "주어가 뭔가를 한다" — 동사만으로 의미 완결

```
The server runs.         서버가 작동한다.
Time flies.              시간이 흐른다.
The meeting ended.       회의가 끝났다.
Errors occur.            에러가 발생한다.
```

특징: 동사 뒤에 아무것도 안 와도 문장이 성립한다. 동사 자체로 완결된다.

#### 2형식: S + V + C (주어 + 동사 + 보어)

> "주어 = 보어" — 주어의 정체/상태를 설명

```
The bug is critical.         그 버그는 심각하다.     (bug = critical)
I am a developer.            나는 개발자다.          (I = developer)
The code looks clean.        코드가 깔끔해 보인다.   (code ≈ clean)
The response seems slow.     응답이 느린 것 같다.    (response ≈ slow)
```

핵심: **S = C** 관계가 성립한다. `The bug = critical`, `I = developer`.

> 2형식의 동사를 **연결동사(linking verb)**라고 부른다. 대표적으로:
> `be`, `become`, `seem`, `appear`, `look`, `feel`, `smell`, `taste`, `sound`, `remain`, `stay`, `get`, `turn`, `grow`

이 동사들은 "행동"이 아니라 "상태/정체"를 연결하는 역할을 한다.

#### 3형식: S + V + O (주어 + 동사 + 목적어)

> "주어가 목적어에 작용한다" — 가장 흔한 패턴

```
I fixed the bug.             내가 버그를 고쳤다.
She wrote the test.          그녀가 테스트를 작성했다.
The system processes data.   시스템이 데이터를 처리한다.
We need more time.           우리는 더 많은 시간이 필요하다.
```

#### 4형식: S + V + IO + DO (주어 + 동사 + 간접목적어 + 직접목적어)

> "주어가 누구에게 무엇을 준다" — 수여/전달 패턴

```
I sent him the report.       나는 그에게 보고서를 보냈다.
She gave me the access.      그녀가 나에게 접근 권한을 줬다.
The API returns us the data. API가 우리에게 데이터를 돌려준다.
He told me the truth.        그가 나에게 진실을 말했다.
```

핵심: 간접목적어(IO) = 받는 사람, 직접목적어(DO) = 주는 것.

> 4형식 대표 동사: `give`, `send`, `tell`, `show`, `teach`, `offer`, `bring`, `lend`, `return`, `grant`

#### 5형식: S + V + O + OC (주어 + 동사 + 목적어 + 목적격보어)

> "주어가 목적어를 어떤 상태로 만든다" — 변화/인식 패턴

```
The test made me happy.       테스트가 나를 행복하게 만들었다.   (me = happy)
I found the code messy.       나는 코드가 지저분하다고 느꼈다.   (code = messy)
We elected him the leader.    우리가 그를 리더로 선출했다.       (him = leader)
They called the project done. 그들이 프로젝트를 완료라고 불렀다. (project = done)
```

핵심: **O = OC** 관계가 성립한다. `me = happy`, `code = messy`.

> 5형식 대표 동사: `make`, `find`, `keep`, `leave`, `call`, `name`, `elect`, `consider`, `think`, `believe`

---

### 3. EXPAND — 짧은 문장에서 확장

각 형식의 문장을 실제 업무 수준으로 확장해보자.

#### 1형식 확장

```
The server runs.
The server runs smoothly.
The server runs smoothly on AWS.
The server runs smoothly on AWS during peak hours.
The production server runs smoothly on AWS during peak hours without any performance degradation.
```

#### 2형식 확장

```
The issue is critical.
The performance issue is critical.
The performance issue in our tax module is critical.
The performance issue in our tax module is critical enough to block the release.
The performance issue in our tax calculation module is critical enough to block the upcoming quarterly release.
```

#### 3형식 확장

```
I reviewed the code.
I reviewed the tax calculation code.
I reviewed the tax calculation code with the team.
I reviewed the tax calculation code with the team during the morning standup.
I thoroughly reviewed the tax calculation code with the backend team during this morning's standup meeting.
```

#### 4형식 확장

```
I sent him the report.
I sent the team lead the test report.
I sent the team lead the quarterly test report.
I sent the team lead the quarterly test report via Slack.
I sent the team lead the comprehensive quarterly test report via Slack right after the deployment.
```

#### 5형식 확장

```
We found the code messy.
We found the legacy code messy.
We found the legacy tax code extremely messy.
We found the legacy tax calculation code extremely messy and hard to maintain.
We found the legacy tax calculation code, written three years ago, extremely messy and practically impossible to maintain.
```

---

### 4. CODE — 개발자 비유로 재이해

#### 5형식 = 함수 시그니처 5가지 패턴

```typescript
// ============================================
// 1형식: S + V → 인자 없는 함수
// ============================================
class Server {
    run(): void {
        // "The server runs."
        // 아무 인자도 필요 없다
    }
}

// ============================================
// 2형식: S + V + C → 상태를 반환/설정하는 함수
// ============================================
class Bug {
    is(state: string): boolean {
        // "The bug is critical."
        // this(S) === state(C) 관계
        return this.status === state;
    }
}

// ============================================
// 3형식: S + V + O → 인자 1개 함수
// ============================================
class Developer {
    fix(bug: Bug): void {
        // "I fixed the bug."
        // 하나의 대상에 작용
    }
}

// ============================================
// 4형식: S + V + IO + DO → 인자 2개 함수 (수신자 + 내용)
// ============================================
class Developer {
    send(recipient: Person, report: Report): void {
        // "I sent him the report."
        // 누구에게(IO) 무엇을(DO)
    }
}

// ============================================
// 5형식: S + V + O + OC → 대상의 상태를 변경하는 함수
// ============================================
class Test {
    make(target: Person, state: Emotion): void {
        // "The test made me happy."
        // target의 상태를 state로 변경
        target.setEmotion(state);
    }
}
```

#### 형식 판별 = 타입 체크

코드에서 함수를 호출할 때 인자 타입과 개수를 확인하듯, 영어 문장을 읽을 때 형식을 판별하면 구조가 즉시 보인다:

```typescript
// 형식 판별 알고리즘
function detectPattern(sentence: Sentence): number {
    const verb = sentence.getVerb();

    if (verb.isIntransitive()) return 1;           // 목적어 없음
    if (verb.isLinkingVerb()) return 2;             // S = C
    if (verb.hasDoubleObject()) return 4;            // IO + DO
    if (verb.hasObjectComplement()) return 5;        // O = OC
    return 3;                                         // 기본: S + V + O
}
```

---

### 5. DOMAIN — 세무/기술 도메인 예문

#### 1형식 (S + V)

```
Tax regulations change frequently.
[세금 규정은 자주 바뀐다.]

The filing deadline passed.
[신고 기한이 지났다.]

The deployment succeeded.
[배포가 성공했다.]
```

#### 2형식 (S + V + C)

```
The new tax rate is 22%.
[새 세율은 22%다.]

The API response looks incorrect.
[API 응답이 잘못된 것 같다.]

The taxpayer remains eligible for the deduction.
[납세자는 여전히 공제 대상이다.]
```

#### 3형식 (S + V + O)

```
The auditor examined the financial statements.
[감사인이 재무제표를 검토했다.]

Our system handles over 1 million tax returns annually.
[우리 시스템은 연간 100만 건 이상의 세금 신고를 처리한다.]

The developer refactored the entire tax calculation module.
[개발자가 세금 계산 모듈 전체를 리팩토링했다.]
```

#### 4형식 (S + V + IO + DO)

```
The NTS sent the taxpayer a penalty notice.
[국세청이 납세자에게 과태료 통지서를 보냈다.]

The system grants the accountant access to the tax records.
[시스템이 회계사에게 세무 기록 접근 권한을 부여한다.]

I showed the client the tax calculation result.
[나는 고객에게 세금 계산 결과를 보여주었다.]
```

#### 5형식 (S + V + O + OC)

```
The audit found the company non-compliant.
[감사가 그 회사를 비준수 상태로 판정했다.]

The new regulation made the filing process more complex.
[새 규정이 신고 절차를 더 복잡하게 만들었다.]

We consider the tax engine reliable.
[우리는 세금 엔진이 신뢰할 만하다고 판단한다.]
```

---

### 6. PRACTICE — 연습 문제

#### 연습 A: 형식 판별

다음 문장의 형식(1~5)을 판별하시오.

1. `The cron job runs every midnight.`
2. `The tax rate became lower.`
3. `The government reduced the corporate tax rate.`
4. `The manager gave the team the deadline.`
5. `The new policy made remote work possible.`
6. `Spring Batch processes tax data efficiently.`
7. `The error message looks confusing.`
8. `She taught the junior developer clean code practices.`
9. `The connection timed out.`
10. `We consider the module production-ready.`

#### 연습 B: 형식별 문장 만들기

다음 주어와 동사로 5형식 문장을 각각 하나씩 만드시오.

| 주어 | 동사 후보 |
|------|----------|
| The API | `responds`, `is`, `returns`, `sends`, `makes` |

- 1형식 (S + V): The API _______________
- 2형식 (S + V + C): The API _______________
- 3형식 (S + V + O): The API _______________
- 4형식 (S + V + IO + DO): The API _______________
- 5형식 (S + V + O + OC): The API _______________

#### 연습 C: 형식 변환

같은 의미를 다른 형식으로 표현해보시오.

1. 4형식 → 3형식: `I sent him the report.` → `I sent the report ___ him.`
2. 4형식 → 3형식: `She gave me the access.` → `She gave the access ___ me.`
3. 어떤 전치사가 들어갔는가? 이 변환의 규칙은 무엇인가?

#### 연습 D: 도메인 문장 작성

다음 상황을 지정된 형식으로 영어 문장을 만드시오.

1. **(1형식)** 서버가 충돌했다.
2. **(2형식)** 이 API는 RESTful하다.
3. **(3형식)** 우리 팀이 세금 계산 로직을 최적화했다.
4. **(4형식)** 시스템이 관리자에게 알림을 보냈다.
5. **(5형식)** 새 아키텍처가 배포 과정을 더 빠르게 만들었다.

---

# Week 2: 품사 — 코드의 자료형

> "단어의 품사를 모르고 문장을 만드는 것은, 변수의 타입을 모르고 코드를 짜는 것과 같다."

---

## Lesson 4 — 왜 품사를 알아야 하는가

### 1. WHY — 품사 = 자료형(Type System)

프로그래밍에서 **자료형(Type)**이 왜 중요한가?

```typescript
// 타입이 있는 언어 (TypeScript)
const name: string = "Alice";
const age: number = 30;
const isActive: boolean = true;

name + age;        // 컴파일러가 경고: string + number?
isActive.length;   // 에러: boolean에 length 없음
```

타입이 있으면 **어떤 연산이 가능하고 어떤 위치에 올 수 있는지**가 명확해진다. 타입이 없으면 런타임에서 예측 불가능한 에러가 터진다.

영어의 **품사(Part of Speech)**가 바로 이 "타입"이다.

```
명사(Noun)     = 변수/객체     → 주어, 목적어, 보어 자리에 올 수 있음
동사(Verb)     = 함수/메서드   → 서술어 자리에만 올 수 있음
형용사(Adj)    = 속성/필드     → 명사를 수식하거나 보어 자리에 올 수 있음
부사(Adv)      = 옵션/설정값   → 동사, 형용사, 다른 부사를 수식
전치사(Prep)   = 관계 연산자   → 명사와 다른 요소의 관계를 표시
접속사(Conj)   = 논리 연산자   → 단어/구/절을 연결 (AND, OR, BUT)
관사(Article)  = 참조 타입     → a/an (새 인스턴스), the (기존 참조)
대명사(Pron)   = 포인터/레퍼런스 → 이미 선언된 명사를 가리킴
```

#### 품사를 왜 "알아야" 하는가

한국어 화자는 품사를 의식하지 않아도 한국어를 잘 한다. 그런데 왜 영어에서는 품사가 중요할까?

이유는 간단하다: **영어는 위치(Position)가 역할을 결정하는 언어**이고, **각 위치에 올 수 있는 품사가 정해져 있기 때문**이다.

```
[위치 0: 주어]     → 명사/대명사만 올 수 있음
[위치 1: 동사]     → 동사만 올 수 있음
[위치 2: 목적어]   → 명사/대명사만 올 수 있음
[위치 3: 보어]     → 명사 또는 형용사가 올 수 있음
```

이것은 프로그래밍의 **타입 제약(Type Constraint)**과 같다:

```typescript
interface Sentence {
    subject: Noun | Pronoun;           // 명사 또는 대명사만
    verb: Verb;                         // 동사만
    object?: Noun | Pronoun;           // 명사 또는 대명사만
    complement?: Noun | Adjective;     // 명사 또는 형용사
}
```

품사를 모르면, 어떤 단어를 어디에 놓아야 하는지 알 수 없다. 그래서 문장이 안 만들어진다.

---

### 2. CORE — 핵심 개념을 최소 문장으로 체득

각 품사가 문장 안에서 어떤 역할을 하는지, 최소 문장으로 확인하자.

```
The   developer  quickly  fixed  the  critical  bug   in   production.
관사   명사       부사      동사   관사  형용사    명사  전치사  명사
(ref)  (주어)    (옵션)   (함수)  (ref) (속성)   (객체) (관계)  (객체)
```

이 문장의 "타입 체크"를 해보면:

| 위치 | 필요한 타입 | 실제 단어 | 타입 일치? |
|------|-----------|----------|-----------|
| 주어 | Noun | developer | OK |
| 동사 | Verb | fixed | OK |
| 목적어 | Noun | bug | OK |
| 동사 수식 | Adverb | quickly | OK |
| 명사 수식 | Adjective | critical | OK |

모든 타입이 일치한다. "타입 에러" 없이 컴파일(문법적으로 올바른 문장)된다.

만약 타입을 잘못 넣으면?

```
❌ The developer quick fixed the critical bug.
    → "quick"는 형용사(Adj)인데 동사 수식 위치에 넣었다.
    → 타입 에러! 부사(Adv) "quickly"를 써야 한다.

❌ The develop quickly fixed the critical bug.
    → "develop"는 동사(Verb)인데 주어 위치에 넣었다.
    → 타입 에러! 명사(Noun) "developer"를 써야 한다.
```

---

### 3. EXPAND — 짧은 문장에서 확장

같은 뿌리(root)에서 파생되는 다양한 품사를 활용해 문장을 확장해보자.

#### "develop" 파생 품사 활용

| 품사 | 단어 | 역할 |
|------|------|------|
| 동사 | develop | 행위 (개발하다) |
| 명사 | developer | 행위자 (개발자) |
| 명사 | development | 행위/결과 (개발) |
| 형용사 | developmental | 속성 (~의 개발적인) |

```
We develop software.                     (동사로 사용)
The developer writes clean code.         (명사-행위자로 사용)
The development took three months.       (명사-행위로 사용)
The developmental cost was high.         (형용사로 사용)
```

#### "tax" 파생 품사 활용

| 품사 | 단어 | 역할 |
|------|------|------|
| 명사 | tax | 대상 (세금) |
| 동사 | tax | 행위 (과세하다) |
| 형용사 | taxable | 속성 (과세 대상인) |
| 명사 | taxation | 행위/제도 (과세) |
| 명사 | taxpayer | 행위자 (납세자) |

```
The tax is 10%.                           (명사)
The government taxes the income.          (동사)
This income is taxable.                   (형용사)
The taxation policy changed.              (명사)
The taxpayer filed the return.            (명사)
```

> **핵심 인사이트**: 같은 뿌리(root)의 단어도 품사에 따라 문장 내 위치가 달라진다. 품사(타입)를 알면 자유자재로 문장을 만들 수 있다.

#### 품사를 바꿔 같은 의미를 다르게 표현하기

```
동사 중심:  We successfully deployed the service.
명사 중심:  The deployment of the service was successful.
형용사 중심: The service deployment was successful.

동사 중심:  The NTS audited the company.
명사 중심:  The NTS conducted an audit of the company.
형용사 중심: The NTS audit was thorough.
```

영어에서 품사 전환(word form change)을 자유롭게 구사하면 표현력이 급상승한다.

---

### 4. CODE — 개발자 비유로 재이해

#### 비유 1: 품사 = TypeScript의 Type System

```typescript
// 품사 = 자료형 정의
type Noun = string;           // 명사: 사물/사람/개념의 이름
type Verb = Function;         // 동사: 실행할 수 있는 행위
type Adjective = Property;    // 형용사: 명사의 속성
type Adverb = Config;         // 부사: 행위의 설정값

// 문장 = 타입이 정해진 구조체
interface Sentence {
    article?: "a" | "an" | "the";
    subject: Noun;
    adverb?: Adverb;
    verb: Verb;
    adjective?: Adjective;
    object?: Noun;
}

// 유효한 문장 (타입 체크 통과)
const sentence: Sentence = {
    article: "the",
    subject: "developer",      // Noun ✓
    adverb: "quickly",         // Adverb ✓
    verb: fix,                 // Verb ✓
    adjective: "critical",     // Adjective ✓
    object: "bug"              // Noun ✓
};
```

#### 비유 2: 전치사 = 관계 연산자

전치사는 **두 요소 사이의 관계**를 정의한다. 이것은 데이터베이스의 관계(Relation)나 코드의 연산자와 같다:

```java
// 전치사 = 관계 정의
in    → CONTAINED_IN     // "the bug in the code" = code.contains(bug)
on    → LOCATED_ON       // "the app on the server" = server.hosts(app)
for   → PURPOSE_OF       // "a tool for testing" = tool.purpose = testing
with  → ASSOCIATED_WITH  // "a report with charts" = report.includes(charts)
from  → ORIGIN_OF        // "data from the API" = data.source = API
to    → DESTINATION_OF   // "a request to the server" = request.target = server
by    → AGENT_OF         // "fixed by the developer" = fix.agent = developer
at    → POINT_AT         // "deployed at midnight" = deploy.time = midnight
```

#### 비유 3: 접속사 = 논리 연산자

```java
// 접속사 = 논리 연산자
and   → &&   // "fast and reliable" = fast && reliable
or    → ||   // "Java or Kotlin" = java || kotlin
but   → &&!  // "fast but expensive" = fast && !cheap
if    → if() // "I will deploy if the tests pass" = if(tests.pass()) deploy()
```

---

### 5. DOMAIN — 세무/기술 도메인 예문

각 품사별로 세무/기술 도메인의 핵심 단어를 정리하고 예문을 만들어보자.

#### 핵심 명사 (Nouns)

| 세무 도메인 | 기술 도메인 |
|------------|------------|
| tax (세금) | server (서버) |
| deduction (공제) | deployment (배포) |
| return (신고서) | endpoint (엔드포인트) |
| withholding (원천징수) | microservice (마이크로서비스) |
| penalty (가산세) | batch job (배치 작업) |
| taxpayer (납세자) | database (데이터베이스) |
| audit (세무조사) | repository (리포지토리) |
| filing (신고) | transaction (트랜잭션) |

#### 핵심 동사 (Verbs)

| 세무 도메인 | 기술 도메인 |
|------------|------------|
| file (신고하다) | deploy (배포하다) |
| deduct (공제하다) | refactor (리팩토링하다) |
| withhold (원천징수하다) | implement (구현하다) |
| audit (감사하다) | optimize (최적화하다) |
| calculate (계산하다) | debug (디버그하다) |
| impose (부과하다) | migrate (마이그레이션하다) |
| comply (준수하다) | configure (설정하다) |
| exempt (면제하다) | validate (검증하다) |

#### 핵심 형용사 (Adjectives)

| 세무 도메인 | 기술 도메인 |
|------------|------------|
| taxable (과세 대상인) | scalable (확장 가능한) |
| deductible (공제 가능한) | reliable (신뢰할 수 있는) |
| exempt (면세인) | deprecated (사용 중단된) |
| overdue (연체된) | asynchronous (비동기적인) |
| compliant (준수하는) | idempotent (멱등적인) |

#### 도메인 예문

```
The taxable income exceeded the threshold.
(형용사 taxable이 명사 income을 수식)

The system reliably processes withholding tax calculations.
(부사 reliably가 동사 processes를 수식)

The overdue penalty for late filing accumulated significantly.
(형용사 overdue가 명사 penalty를 수식, 전치사 for가 관계 표시)

Our scalable microservice handles concurrent tax return submissions efficiently.
(형용사 scalable이 명사 microservice를 수식, 부사 efficiently가 동사 handles를 수식)
```

---

### 6. PRACTICE — 연습 문제

#### 연습 A: 품사 판별

다음 문장에서 밑줄 친 단어의 품사를 판별하시오.

1. The **efficient** system processed the tax **return** **quickly**.
2. We need to **validate** the **taxable** amount before **submission**.
3. The **newly** deployed service **handles** requests **reliably**.
4. An **experienced** auditor **conducted** the **annual** review.
5. The **database** migration **failed** **unexpectedly** during the **nightly** batch.

#### 연습 B: 품사 변환

주어진 단어의 다른 품사 형태를 쓰고, 각각을 사용한 문장을 만드시오.

| 기본 형태 | 동사 | 명사 | 형용사 | 부사 |
|-----------|------|------|--------|------|
| calculate | ? | ? | ? | - |
| comply | ? | ? | ? | - |
| automate | ? | ? | ? | ? |

#### 연습 C: 타입 에러 찾기

다음 문장에서 "타입 에러"(품사 오용)를 찾아 수정하시오.

1. `The system process the data efficient.`
2. `We need a reliably solution for this problem.`
3. `The develop team complete the tax module successful.`
4. `The annually audit reveal several compliance issue.`

#### 연습 D: 문장 조립

다음 단어들의 품사를 파악하고 올바른 문장을 조립하시오.

1. `[calculated / the / tax engine / automatically / deductions / the]`
2. `[new / compliant / is / the / fully / system / with / regulations / tax]`
3. `[reliably / our / processes / service / returns / tax / millions of / annually]`

---

## Lesson 5 — be동사: 영어의 "할당 연산자"

### 1. WHY — be동사는 왜 존재하는가

프로그래밍에서 가장 기본적인 연산은 무엇인가? **할당(assignment)**이다.

```javascript
let status = "active";      // 상태를 설정한다
let role = "developer";     // 역할을 부여한다
let count = 0;              // 값을 초기화한다
```

영어에서 be동사가 하는 일이 바로 이것이다:

```
I am a developer.         → I = developer      (역할 할당)
She is tired.             → she.status = tired  (상태 설정)
They are Korean.          → they.nationality = Korean (속성 설정)
The server is down.       → server.status = down (상태 확인)
```

be동사는 **"이다/~하다"가 아니다**. be동사의 본질은 **주어와 보어를 등호(=)로 연결하는 것**이다.

그렇다면 왜 be동사의 형태가 이렇게 제각각일까?

| 주어 | 현재형 | 과거형 |
|------|--------|--------|
| I | am | was |
| You | are | were |
| He/She/It | is | was |
| We/They | are | were |

이것은 역사적 이유다. 고대 영어에서 서로 다른 세 개의 동사 뿌리가 합쳐져서 하나의 be동사가 되었다:

1. **be-** 계열 (be, been, being) — 게르만어 *bēonan*
2. **am/is/are** 계열 — 인도유럽어 *es-/s-*
3. **was/were** 계열 — 게르만어 *wesan*

세 가지 뿌리가 하나로 합쳐지다 보니, 형태가 불규칙해진 것이다. 외울 수밖에 없다. 하지만 핵심 기능은 동일하다: **주어 = 보어를 연결하는 것.**

---

### 2. CORE — 핵심 개념을 최소 문장으로 체득

#### be동사의 3가지 핵심 기능

**기능 1: 정체 선언 (A = B)**

```
I am a developer.          나는 개발자다.
She is the team lead.      그녀는 팀장이다.
This is a tax return.      이것은 세금 신고서다.
```

프로그래밍으로: `const role: string = "developer";`

**기능 2: 상태 서술 (A.status = B)**

```
The server is down.        서버가 다운이다.
The bug is critical.       버그가 심각하다.
I am tired.                나는 피곤하다.
```

프로그래밍으로: `server.status = Status.DOWN;`

**기능 3: 위치/존재 (A.location = B)**

```
The file is on the server.     파일이 서버에 있다.
We are in the meeting room.    우리는 회의실에 있다.
The error is in line 42.       에러가 42번째 줄에 있다.
```

프로그래밍으로: `file.location = server;`

#### be동사 활용표

| | 현재 | 과거 | 미래 |
|---|---|---|---|
| I | am | was | will be |
| You | are | were | will be |
| He/She/It | is | was | will be |
| We/They | are | were | will be |

> 현재형만 3개(am/is/are)로 나뉘고, 과거형은 2개(was/were), 미래형은 1개(will be)로 수렴한다.

---

### 3. EXPAND — 짧은 문장에서 확장

#### 정체 선언 확장

```
I am a developer.
I am a backend developer.
I am a backend developer at a tax-tech company.
I am a backend developer at a tax-tech company in Seoul.
I am a senior backend developer at a leading tax-tech company in Seoul, Korea.
```

#### 상태 서술 확장

```
The API is slow.
The API is extremely slow.
The API is extremely slow today.
The API is extremely slow today due to heavy traffic.
The API is extremely slow today due to heavy traffic from the year-end tax filing rush.
```

#### 부정문과 의문문

be동사의 부정문과 의문문은 매우 간단하다. 조동사(do/does)가 필요 없다.

```
긍정:   The server is running.
부정:   The server is not running.       (is 뒤에 not 추가)
축약:   The server isn't running.
의문:   Is the server running?           (is를 앞으로 이동)
```

이것은 다른 동사보다 훨씬 단순하다:

```
일반동사 부정:  The server does not run.    (do/does 필요)
be동사 부정:    The server is not running.  (not만 추가)
```

> be동사는 자체적으로 "조동사 역할"을 겸하기 때문에, do/does의 도움 없이 혼자서 부정문과 의문문을 만들 수 있다.

---

### 4. CODE — 개발자 비유로 재이해

#### 비유 1: be동사 = 할당 연산자 (=)

```javascript
// be동사 = 할당 연산자
// "I am a developer" → 할당
const I = {
    role: "developer"          // am = 할당
};

// "The server is down" → 상태 설정
server.status = "down";        // is = 할당

// "The tests are passing" → 상태 확인
tests.status = "passing";      // are = 할당

// "The bug was critical" → 과거 상태
bug.previousStatus = "critical";  // was = 과거 할당
```

#### 비유 2: be동사 vs 일반동사 = 선언 vs 실행

```javascript
// be동사: 상태를 "선언"한다 (=)
const bug = { severity: "critical" };     // "The bug is critical"
const server = { status: "running" };     // "The server is running"

// 일반동사: 행위를 "실행"한다 (function call)
developer.fix(bug);                        // "The developer fixed the bug"
server.process(request);                   // "The server processes requests"
```

> **be동사** = 상태 서술 (declarative)
> **일반동사** = 행위 서술 (imperative)

#### 비유 3: be동사 = equals() 메서드

```java
// "The bug is critical" = 상태 비교/할당
bug.equals("critical");     // true
bug.setState("critical");

// "I am a developer" = 타입 체크
this instanceof Developer;  // true

// "They are in the office" = 위치 확인
this.location.equals("office");  // true
```

---

### 5. DOMAIN — 세무/기술 도메인 예문

#### 세무 도메인 be동사 문장

```
정체 선언:
The NTS is the national tax authority of South Korea.
(국세청은 대한민국의 국세 행정 기관이다.)

VAT is a consumption tax imposed on goods and services.
(부가가치세는 재화와 용역에 부과되는 소비세다.)

상태 서술:
The tax return is overdue.
(세금 신고가 연체되었다.)

The deduction amount is incorrect.
(공제 금액이 잘못되었다.)

The company is compliant with all tax regulations.
(그 회사는 모든 세금 규정을 준수하고 있다.)

위치/존재:
The error is in the withholding tax calculation.
(에러가 원천징수세 계산에 있다.)
```

#### 기술 도메인 be동사 문장

```
정체 선언:
Kafka is a distributed event streaming platform.
(Kafka는 분산 이벤트 스트리밍 플랫폼이다.)

Spring Batch is a framework for batch processing.
(Spring Batch는 배치 처리를 위한 프레임워크다.)

상태 서술:
The API endpoint is deprecated.
(그 API 엔드포인트는 사용 중단되었다.)

The database connection pool is exhausted.
(데이터베이스 커넥션 풀이 고갈되었다.)

The new feature is production-ready.
(새 기능은 프로덕션 준비가 되었다.)

위치/존재:
The configuration is in the application.yml file.
(설정은 application.yml 파일에 있다.)
```

---

### 6. PRACTICE — 연습 문제

#### 연습 A: be동사 선택

빈칸에 알맞은 be동사(am/is/are/was/were/will be)를 넣으시오.

1. I ___ a backend developer specializing in tax systems.
2. The servers ___ down for maintenance last night.
3. The new tax rate ___ effective from next January.
4. The test cases ___ all passing now.
5. The audit report ___ submitted to the NTS yesterday.
6. These microservices ___ deployed on Kubernetes.
7. The database migration ___ completed by Friday.
8. I ___ responsible for the tax calculation module.

#### 연습 B: be동사 vs 일반동사 구별

다음 상황을 be동사 문장과 일반동사 문장으로 각각 작성하시오.

1. 서버 상태가 안정적이다
   - be동사: _______________
   - 일반동사: _______________

2. 이 API가 빠르다
   - be동사: _______________
   - 일반동사: _______________

3. 배포가 성공적이었다
   - be동사: _______________
   - 일반동사: _______________

#### 연습 C: be동사 부정문/의문문 전환

다음 긍정문을 부정문과 의문문으로 바꾸시오.

1. `The tax calculation is accurate.`
   - 부정문: _______________
   - 의문문: _______________

2. `The developers were ready for the deployment.`
   - 부정문: _______________
   - 의문문: _______________

3. `This income is taxable.`
   - 부정문: _______________
   - 의문문: _______________

#### 연습 D: 도메인 문장 작성

be동사를 사용하여 다음을 영어로 작성하시오.

1. "ShedLock은 분산 스케줄러 잠금 라이브러리다." (정체)
2. "현재 시스템 응답 시간이 느리다." (상태)
3. "에러 로그가 /var/log/app.log 파일에 있다." (위치)
4. "이 소득은 비과세다." (상태)
5. "우리 팀은 세무 기장 서비스를 담당하고 있다." (역할)

---

## Lesson 6 — 일반동사와 시제: 함수의 실행 시점

### 1. WHY — 왜 영어는 동사 형태를 바꿔서 시간을 표현하는가

한국어에서 시간은 이렇게 표현된다:

```
나는 코드를 작성한다.     (현재)
나는 코드를 작성했다.     (과거)
나는 코드를 작성할 것이다. (미래)
```

한국어는 동사 어간 "작성하-"는 변하지 않고, **어미**(-ㄴ다/-했다/-할 것이다)가 바뀐다.

영어도 비슷하지만, **동사 자체의 형태**가 변한다:

```
I write code.             (현재: write)
I wrote code.             (과거: wrote ← 형태 변화!)
I will write code.        (미래: will + write)
```

그런데 영어의 시제는 단순히 "시간"만 표현하지 않는다. **화자의 관점과 태도**까지 담고 있다.

이것이 한국인이 영어 시제를 어려워하는 이유다. 한국어에서 시제는 "언제"를 말하지만, 영어에서 시제는 "언제 + 어떤 관점으로"를 말한다.

하지만 Phase 1에서는 기본 3시제(현재/과거/미래)만 확실히 잡겠다. 완료형, 진행형 등은 이후 Phase에서 다룬다.

---

### 2. CORE — 핵심 개념을 최소 문장으로 체득

#### 현재 시제: 반복/습관/불변의 사실

현재 시제는 "지금 이 순간"이 아니다. **"항상 그렇다"**라는 뜻이다.

```
The cron job runs every night.           크론 작업은 매일 밤 실행된다. (반복)
I write code in Java.                     나는 자바로 코드를 쓴다. (습관)
Water boils at 100°C.                     물은 100도에서 끓는다. (사실)
The API returns JSON by default.          API는 기본적으로 JSON을 반환한다. (사실)
```

> **개발자 비유**: 현재 시제 = `@Scheduled` 어노테이션. 반복 실행되는 것, 또는 시스템의 기본 설정(default behavior).

**주의**: 3인칭 단수 현재형에는 `-s`를 붙인다!

```
I run the test.        (1인칭: run)
You run the test.      (2인칭: run)
He runs the test.      (3인칭 단수: runs ← -s 추가!)
The server runs well.  (3인칭 단수: runs)
They run the tests.    (3인칭 복수: run)
```

이 `-s`는 왜 있을까? 고대 영어의 인칭 변화(conjugation)가 대부분 사라졌지만, 3인칭 단수 `-s`만 살아남은 것이다. 영어에서 유일하게 남은 동사 인칭 변화의 흔적이다.

#### 과거 시제: 완료된 이벤트

과거 시제는 "이미 끝난 일"을 말한다. **"그때 그랬고, 지금은 아닐 수도 있다"**라는 뉘앙스가 있다.

```
The deployment failed yesterday.          어제 배포가 실패했다.
I fixed the bug this morning.             오늘 아침에 버그를 고쳤다.
The client reported an issue last week.   지난 주에 클라이언트가 이슈를 보고했다.
We migrated the database last month.      지난 달에 데이터베이스를 마이그레이션했다.
```

> **개발자 비유**: 과거 시제 = 이미 실행이 끝난 함수. `completedFuture`, 이미 resolve된 Promise.

**과거형 만드는 법**:

| 유형 | 규칙 | 예시 |
|------|------|------|
| 규칙 동사 | -ed 추가 | deploy → deployed, fix → fixed, process → processed |
| 불규칙 동사 | 형태 변화 | write → wrote, run → ran, send → sent, break → broke |

불규칙 동사는 외울 수밖에 없다. 하지만 자주 쓰는 것은 한정되어 있다:

| 현재 | 과거 | 과거분사 | 의미 |
|------|------|---------|------|
| write | wrote | written | 쓰다 |
| run | ran | run | 실행하다 |
| send | sent | sent | 보내다 |
| break | broke | broken | 깨뜨리다 |
| find | found | found | 찾다 |
| make | made | made | 만들다 |
| take | took | taken | 가져가다 |
| give | gave | given | 주다 |
| know | knew | known | 알다 |
| get | got | gotten/got | 얻다 |
| go | went | gone | 가다 |
| see | saw | seen | 보다 |
| come | came | come | 오다 |
| think | thought | thought | 생각하다 |
| build | built | built | 만들다 |
| set | set | set | 설정하다 |

#### 미래 시제: 예정/의지/예측

미래 시제는 **"아직 안 일어난 일"**을 말한다. `will` 또는 `be going to`를 동사 앞에 붙인다.

```
I will refactor this module next sprint.
(다음 스프린트에 이 모듈을 리팩토링할 것이다.)

We will deploy the new version tomorrow.
(내일 새 버전을 배포할 것이다.)

The new tax law will take effect in January.
(새 세법이 1월에 시행될 것이다.)

The system is going to process the year-end data tonight.
(시스템이 오늘 밤에 연말 데이터를 처리할 예정이다.)
```

> **will vs be going to**:
> - `will`: 그 순간의 의지/결심, 또는 일반적 예측 → "할게", "~일 것이다"
> - `be going to`: 이미 계획된 것, 또는 근거 있는 예측 → "~할 예정이다"

```
// will: 순간 결심
"The server is down!" → "I will check the logs right now."

// be going to: 계획된 것
"We are going to migrate to Kubernetes next quarter."  (이미 계획)
```

---

### 3. EXPAND — 짧은 문장에서 확장

#### 시제별 확장 연습

**현재 시제 확장**:
```
The system processes data.
The system processes tax data.
The system processes tax data for individual returns.
The system processes tax data for individual returns every quarter.
The system automatically processes tax data for individual income tax returns every quarter.
```

**과거 시제 확장**:
```
We deployed the service.
We deployed the tax service.
We deployed the tax service to production.
We deployed the tax service to production last Friday.
We successfully deployed the updated tax calculation service to the production environment last Friday afternoon.
```

**미래 시제 확장**:
```
I will fix the bug.
I will fix the calculation bug.
I will fix the calculation bug tomorrow.
I will fix the calculation bug in the withholding module tomorrow.
I will fix the critical calculation bug in the withholding tax module by tomorrow morning.
```

#### 하나의 이야기를 3시제로 전환

```
현재 (습관/반복):
Our team deploys code every Friday.

과거 (지난 이벤트):
Our team deployed a critical hotfix last night.

미래 (계획/예정):
Our team will deploy the new tax engine next Monday.
```

---

### 4. CODE — 개발자 비유로 재이해

#### 비유 1: 시제 = 함수의 실행 상태

```java
// 현재 시제 = 스케줄링된 작업 (반복 실행)
@Scheduled(cron = "0 0 * * *")
public void processData() {
    // "The system processes data every day."
    // 항상 반복. "현재" = "항상 그렇다"
}

// 과거 시제 = 이미 완료된 작업 (CompletedFuture)
CompletableFuture<Result> result = CompletableFuture.completedFuture(data);
// "The system processed the data."
// 이미 끝남. result는 이미 존재.

// 미래 시제 = 아직 실행 안 된 작업 (Pending Future)
CompletableFuture<Result> future = scheduler.schedule(task, delay);
// "The system will process the data."
// 아직 안 끝남. future는 pending 상태.
```

#### 비유 2: 시제 = Git 히스토리

```bash
# 현재 시제 = 현재 동작 중인 것 (현재 브랜치의 코드)
git status    # "The application runs on port 8080."

# 과거 시제 = 이미 커밋된 것 (Git 히스토리)
git log       # "We deployed version 2.3.1 last week."

# 미래 시제 = 아직 머지 안 된 PR (계획된 변경)
gh pr list    # "We will release the new feature next sprint."
```

#### 비유 3: 3인칭 단수 -s = null check

3인칭 단수 현재형에 `-s`를 붙이는 것은, 자바에서 null check를 하는 것과 비슷하다: 안 해도 될 것 같지만, 안 하면 에러(문법 오류)가 난다.

```java
// 3인칭 단수 -s를 빼먹으면
"The server run well."     // ❌ 문법 에러!
"The server runs well."    // ✓ 올바름

// null check를 빼먹으면
server.getStatus();                      // ❌ NullPointerException 가능!
if (server != null) server.getStatus();  // ✓ 안전
```

귀찮지만 반드시 해야 하는 것. 습관으로 만들어야 한다.

---

### 5. DOMAIN — 세무/기술 도메인 예문

#### 현재 시제 — 시스템 동작/규정 설명

```
The tax engine calculates withholding tax based on the latest tax brackets.
(세금 엔진은 최신 세율 구간에 따라 원천징수세를 계산한다.)

The NTS requires all businesses to file VAT returns quarterly.
(국세청은 모든 사업자에게 분기마다 부가가치세 신고를 요구한다.)

Our API validates the taxpayer identification number before processing.
(우리 API는 처리 전에 납세자 식별번호를 검증한다.)

ShedLock prevents duplicate execution of scheduled tasks in a distributed environment.
(ShedLock은 분산 환경에서 스케줄 작업의 중복 실행을 방지한다.)
```

#### 과거 시제 — 이벤트/이슈 보고

```
The batch job failed at 2:00 AM due to a database connection timeout.
(배치 작업이 새벽 2시에 데이터베이스 연결 타임아웃으로 실패했다.)

We discovered a critical bug in the year-end tax settlement calculation last week.
(지난 주에 연말정산 계산에서 치명적인 버그를 발견했다.)

The team migrated the entire tax data from Oracle to PostgreSQL last quarter.
(팀이 지난 분기에 전체 세무 데이터를 Oracle에서 PostgreSQL로 마이그레이션했다.)

The client submitted an amended tax return through our system yesterday.
(고객이 어제 우리 시스템을 통해 수정 신고서를 제출했다.)
```

#### 미래 시제 — 계획/예정

```
We will implement the SAGA pattern for the tax payment workflow next sprint.
(다음 스프린트에 세금 납부 워크플로우에 SAGA 패턴을 구현할 것이다.)

The new pension reform rules will affect our calculation logic starting from April.
(새 연금 개혁 규칙이 4월부터 우리 계산 로직에 영향을 미칠 것이다.)

Our team is going to refactor the tax filing module to support the Outbox pattern.
(우리 팀은 Outbox 패턴을 지원하도록 세금 신고 모듈을 리팩토링할 예정이다.)

The government will raise the corporate tax threshold in the next fiscal year.
(정부는 다음 회계연도에 법인세 기준금액을 인상할 것이다.)
```

---

### 6. PRACTICE — 연습 문제

#### 연습 A: 올바른 시제 선택

빈칸에 알맞은 시제의 동사를 넣으시오.

1. The cron job ___ (run) every midnight. [반복]
2. We ___ (deploy) the hotfix last night. [과거 이벤트]
3. The new tax law ___ (take) effect next January. [미래 계획]
4. Our API ___ (return) JSON responses by default. [시스템 사실]
5. The database migration ___ (fail) three times yesterday. [과거 이벤트]
6. I ___ (refactor) this module next sprint. [미래 의지]
7. The NTS ___ (require) quarterly VAT filings. [규정/사실]
8. The team ___ (complete) the migration last month. [과거 이벤트]

#### 연습 B: 3인칭 단수 -s 연습

다음 문장에서 동사를 올바른 현재형으로 고치시오.

1. `The system (process) tax returns every day.`
2. `Our API (validate) the input data before saving.`
3. `The batch job (run) at midnight.`
4. `Each microservice (handle) its own database.`
5. `The tax engine (calculate) withholding amounts automatically.`

#### 연습 C: 시제 전환

다음 문장을 지시된 시제로 바꾸시오.

1. `We deploy the service.` → 과거: ___ / 미래: ___
2. `The NTS audited the company.` → 현재: ___ / 미래: ___
3. `I will fix the bug.` → 현재: ___ / 과거: ___
4. `The system processes the data.` → 과거: ___ / 미래: ___

#### 연습 D: 상황별 시제 선택 + 문장 작성

다음 상황을 적절한 시제로 영어 문장을 작성하시오.

1. **시스템 명세서에 쓸 문장**: "이 서비스는 원천징수세를 자동으로 계산한다."
   → 시제: ___ / 문장: ___

2. **장애 보고서에 쓸 문장**: "어제 배치 작업이 메모리 부족으로 실패했다."
   → 시제: ___ / 문장: ___

3. **스프린트 계획에 쓸 문장**: "다음 주에 세율 적용 로직을 리팩토링할 것이다."
   → 시제: ___ / 문장: ___

4. **API 문서에 쓸 문장**: "이 엔드포인트는 납세자 정보를 반환한다."
   → 시제: ___ / 문장: ___

5. **회고록에 쓸 문장**: "우리 팀이 지난 분기에 MSA 전환을 완료했다."
   → 시제: ___ / 문장: ___

#### 연습 E: 부정문과 의문문 만들기 (일반동사)

일반동사의 부정문과 의문문에는 `do/does/did`가 필요하다. 다음 문장을 부정문과 의문문으로 바꾸시오.

```
현재 긍정:  The system processes the data.
현재 부정:  The system does not process the data.
현재 의문:  Does the system process the data?
```

1. `The API validates the tax ID.`
   - 부정문: _______________
   - 의문문: _______________

2. `We deployed the update yesterday.`
   - 부정문: _______________
   - 의문문: _______________

3. `The batch job runs at midnight.`
   - 부정문: _______________
   - 의문문: _______________

4. `The developer fixed the calculation error.`
   - 부정문: _______________
   - 의문문: _______________

> **주의**: 부정문/의문문에서 `does/did`를 쓰면, **본동사는 원형**으로 돌아간다!
> - `The API validates...` → `Does the API validate...?` (validates → validate)
> - `We deployed...` → `Did we deploy...?` (deployed → deploy)

---

# Phase 1 마무리

## 핵심 정리 표

| Lesson | 핵심 원리 | 개발자 비유 | 한 줄 요약 |
|--------|----------|------------|-----------|
| 1 | 위치 = 역할 | Array(인덱스 접근) | 동사 앞 = 주어, 동사 뒤 = 목적어 |
| 2 | SVO 어순 | Early Return 패턴 | 결론(누가+뭘 했다)을 먼저, 부가정보는 뒤에 |
| 3 | 5형식 | 함수 시그니처 패턴 | 동사(함수)가 요구하는 인자 개수/종류로 형식 결정 |
| 4 | 품사 = 타입 | Type System | 각 위치에 올 수 있는 품사(타입)가 정해져 있다 |
| 5 | be동사 = 할당 | 할당 연산자 (=) | 주어의 정체/상태/위치를 선언한다 |
| 6 | 시제 = 실행 시점 | 함수의 실행 상태 | 현재(반복) / 과거(완료) / 미래(예정) |

## Phase 1 자가 진단 체크리스트

- [ ] 영어 문장을 보면 S-V-O 골격을 3초 안에 찾을 수 있다
- [ ] 5형식의 차이를 함수 시그니처로 설명할 수 있다
- [ ] 8가지 품사를 개발 개념으로 매핑할 수 있다
- [ ] be동사와 일반동사의 차이를 설명할 수 있다
- [ ] 현재/과거/미래 시제를 상황에 맞게 선택할 수 있다
- [ ] 일반동사의 부정문/의문문에 do/does/did가 필요하다는 것을 안다
- [ ] 3인칭 단수 현재형 -s를 빼먹지 않는다
- [ ] 세무/기술 도메인의 기본 영어 어휘 30개 이상을 안다

## 다음 Phase 예고

> **Phase 2: 문장의 확장 도구** (Week 3-4)
> - 조동사: 코드의 접근 제어자 (can/must/should/may)
> - 준동사: 함수를 값으로 전달하기 (to부정사, 동명사, 분사)
> - 접속사: 논리 연산자와 제어 흐름 (and, but, or, if, when, because)
