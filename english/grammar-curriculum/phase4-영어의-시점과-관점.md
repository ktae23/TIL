# Phase 4: 영어의 시점과 관점 (Week 9-10)

> 같은 사건도 어떤 관점에서 보느냐에 따라 표현이 달라진다.
> 프로그래밍에서 같은 데이터도 어떤 객체의 시점에서 접근하느냐에 따라 코드가 달라지듯,
> 영어도 **누구의 시점에서, 언제의 시점에서** 말하느냐에 따라 문법 구조가 바뀐다.

---

## 이 Phase에서 다루는 것

| Week | 주제 | 핵심 질문 |
|------|------|-----------|
| **Week 9** | 수동태 — 관점의 전환 | "누가 했느냐"보다 "무엇이 되었느냐"가 중요할 때 어떻게 말하는가? |
| **Week 10** | 완료 시제 — 시간의 연결 | 과거와 현재를 동시에 표현하려면 어떻게 하는가? |

### 선수 지식 확인

- Phase 1에서 배운 **SVO 어순** (주어-동사-목적어)
- Phase 2에서 배운 **시제의 기본 개념** (현재/과거/미래)
- Phase 3에서 배운 **조동사** (can, should, will, must)

이 세 가지가 합쳐져서 Phase 4의 수동태와 완료 시제가 만들어진다.

---

# Week 9: 수동태 — 관점의 전환

> **수동태는 "약한 표현"이 아니다. "관점의 선택"이다.**

---

## Lesson 24 — 수동태는 왜 존재하는가

### WHY — 수동태가 존재하는 진짜 이유

#### 한국어 화자가 수동태를 어려워하는 이유

한국어에서는 수동태가 영어만큼 자주 쓰이지 않는다. 한국어는 주어를 생략하는 것이 자연스럽기 때문이다.

```
한국어: "업데이트 배포했어." (누가? → 생략해도 자연스러움)
영어:   "Deployed the update." (X) → 비문. 주어가 반드시 필요.
영어:   "The update was deployed." (O) → 수동태로 주어 문제 해결.
```

영어는 **주어가 반드시 있어야 하는 언어**다. 그런데 행위자를 모르거나, 행위자가 중요하지 않을 때가 있다. 이때 **대상을 주어 자리에 올리는 것**이 수동태다.

#### 수동태를 쓰는 4가지 진짜 이유

| # | 이유 | 예문 | 설명 |
|---|------|------|------|
| 1 | **행위자를 모를 때** | `The server was hacked.` | 누가 해킹했는지 모름 |
| 2 | **행위자가 중요하지 않을 때** | `The bug was fixed.` | 누가 고쳤는지보다 고쳐진 사실이 중요 |
| 3 | **대상/결과를 강조할 때** | `The tax return was filed on time.` | 신고서가 제때 제출된 것이 핵심 |
| 4 | **객관적/공식적 톤을 낼 때** | `Errors should be logged.` | 기술 문서, 법률 문서의 기본 톤 |

> **핵심 원리**: 수동태는 "카메라 앵글"을 바꾸는 것이다.
> - 능동태 = 행위자에게 카메라를 맞춤
> - 수동태 = 대상/결과에 카메라를 맞춤

#### 프로그래밍에서의 관점 전환 비유

```java
// 능동태적 사고: 주체가 행동한다
teamService.deployUpdate(update);    // 팀 서비스가 업데이트를 배포한다

// 수동태적 사고: 대상이 행동을 받는다
update.wasDeployedBy(teamService);   // 업데이트가 (팀 서비스에 의해) 배포되었다
```

이것은 마치 **Observer Pattern**과 같다:
- 능동태 = `publisher.publish(event)` → 발행자 중심
- 수동태 = `event.wasPublishedAt(timestamp)` → 이벤트 중심

---

### CORE — 수동태의 기본 구조

#### 능동태 → 수동태 변환 공식

```
능동태: 주어(행위자) + 동사 + 목적어(대상)
수동태: 주어(대상) + be + 과거분사(p.p.) + [by 행위자]

The team    deployed    the update.
(행위자)     (동사)      (대상)
    ↓          ↓           ↓
The update  was deployed  (by the team).
(대상→주어)  (be+p.p.)    (행위자→생략 가능)
```

#### 가장 짧은 수동태 문장들

이 문장들을 소리 내어 읽으면서 구조를 체화하자:

```
It was done.          (그것은 완료되었다.)
It is fixed.          (그것은 수정되었다.)
It was sent.          (그것은 전송되었다.)
It is stored.         (그것은 저장되었다.)
It was tested.        (그것은 테스트되었다.)
It is deployed.       (그것은 배포되었다.)
It was approved.      (그것은 승인되었다.)
It is calculated.     (그것은 계산되었다.)
```

> **체화 포인트**: `be동사 + 과거분사(p.p.)` 조합이 눈에 자동으로 들어올 때까지 반복하라.

#### be동사의 시제에 따른 변화

| 시제 | be동사 | 수동태 예문 | 의미 |
|------|--------|------------|------|
| 현재 | `is/am/are` | `The report **is** generated.` | 보고서가 생성된다 |
| 과거 | `was/were` | `The report **was** generated.` | 보고서가 생성되었다 |
| 미래 | `will be` | `The report **will be** generated.` | 보고서가 생성될 것이다 |

---

### EXPAND — 짧은 문장에서 긴 문장으로

#### Step 1: 기본 수동태

```
The bug was fixed.
```

#### Step 2: by + 행위자 추가

```
The bug was fixed by the backend team.
```

#### Step 3: 시간 부사 추가

```
The bug was fixed by the backend team yesterday.
```

#### Step 4: 부사절 추가

```
The bug was fixed by the backend team yesterday after three hours of debugging.
```

#### Step 5: 복합 수동태 문장

```
The critical bug that had been reported by the QA team was finally fixed
by the backend team yesterday after three hours of intensive debugging,
and the hotfix was deployed to production within an hour.
```

#### 실전 확장 연습: 세무 도메인

```
Step 1: The return was filed.
        (신고서가 제출되었다.)

Step 2: The tax return was filed by the taxpayer.
        (세금 신고서가 납세자에 의해 제출되었다.)

Step 3: The tax return was filed by the taxpayer on March 15th.
        (세금 신고서가 3월 15일에 납세자에 의해 제출되었다.)

Step 4: The tax return was filed by the taxpayer on March 15th
        through our online platform.
        (세금 신고서가 3월 15일에 우리 온라인 플랫폼을 통해 납세자에 의해 제출되었다.)

Step 5: The comprehensive income tax return for the fiscal year 2025
        was filed by the individual taxpayer on March 15th
        through our automated online platform,
        and the confirmation receipt was sent to the registered email address
        within 30 seconds.
        (2025 회계연도 종합소득세 신고서가 3월 15일에 개인 납세자에 의해
        자동 온라인 플랫폼을 통해 제출되었으며, 확인 영수증이 등록된 이메일 주소로
        30초 이내에 발송되었다.)
```

---

### CODE — 개발자 비유로 재이해

#### 수동태 = Reactive Programming

능동태와 수동태의 차이는 **Imperative vs Reactive** 프로그래밍의 차이와 같다.

```java
// 능동태 = Imperative (명령형): "주체가 ~한다"
taxService.calculateTax(income);
validator.validateReturn(taxReturn);
scheduler.sendNotification(user);

// 수동태 = Reactive (반응형): "대상이 ~된다"
income.taxWasCalculated();
taxReturn.wasValidated();
notification.wasSentToUser();
```

#### 수동태 = Event Sourcing의 이벤트

Event Sourcing에서 이벤트는 항상 **과거분사 형태**로 기록된다:

```java
// Event Sourcing 이벤트 이름들 — 전부 수동태적 사고!
TaxReturnFiled       // 세금 신고서가 제출됨
DeductionApplied     // 공제가 적용됨
PaymentProcessed     // 결제가 처리됨
RefundIssued         // 환급이 발급됨
AccountVerified      // 계정이 인증됨
InvoiceGenerated     // 인보이스가 생성됨
```

> 이미 개발할 때 수동태적 사고를 하고 있었다!
> Event 이름 자체가 영어 수동태의 과거분사(p.p.)다.

#### 수동태 = DB 관점의 전환

```sql
-- 능동태적 쿼리: "누가 업데이트했나?"
SELECT updater_id FROM tax_returns WHERE id = 123;

-- 수동태적 쿼리: "이 레코드가 언제 업데이트되었나?"
SELECT updated_at FROM tax_returns WHERE id = 123;
```

`updated_at`, `created_at`, `deleted_at` — 이 컬럼명들이 전부 **수동태(과거분사)**다.

| DB 컬럼명 | 영어 수동태 문장 | 한국어 |
|-----------|-----------------|--------|
| `created_at` | It was **created** at 10:00 AM. | 10시에 생성되었다 |
| `updated_at` | It was **updated** at 3:00 PM. | 3시에 수정되었다 |
| `deleted_at` | It was **deleted** at midnight. | 자정에 삭제되었다 |
| `filed_at` | It was **filed** at 11:59 PM. | 11시 59분에 신고되었다 |
| `approved_at` | It was **approved** at noon. | 정오에 승인되었다 |
| `rejected_at` | It was **rejected** at 2:00 PM. | 2시에 반려되었다 |

---

### DOMAIN — 세무/기술 도메인 실전 예문

#### 세무 도메인 수동태 필수 표현

세무 도메인에서는 수동태가 **표준 표현**이다. 공식 문서는 거의 항상 수동태로 쓴다.

```
1. The tax return was filed before the deadline.
   (세금 신고서가 기한 전에 제출되었다.)

2. The deduction was applied to the total income.
   (공제가 총소득에 적용되었다.)

3. The penalty was imposed for late filing.
   (과태료가 지연 신고에 대해 부과되었다.)

4. The refund will be issued within 30 days.
   (환급금이 30일 이내에 지급될 것이다.)

5. The income was reported correctly.
   (소득이 정확하게 신고되었다.)

6. The tax rate is determined by the income bracket.
   (세율은 소득 구간에 의해 결정된다.)

7. All receipts must be retained for five years.
   (모든 영수증은 5년간 보관되어야 한다.)

8. The tax base is calculated by subtracting deductions from gross income.
   (과세표준은 총소득에서 공제를 뺀 값으로 계산된다.)

9. Withholding tax is deducted at source by the employer.
   (원천징수세는 고용주에 의해 원천에서 공제된다.)

10. The amended return was submitted after the error was discovered.
    (수정 신고서가 오류 발견 후 제출되었다.)
```

#### 기술 도메인 수동태 필수 표현

API 문서, 기술 명세서, 에러 로그에서 수동태는 필수다.

```
1. The request was rejected due to invalid authentication.
   (요청이 잘못된 인증으로 인해 거부되었다.)

2. The data is encrypted before being stored in the database.
   (데이터는 데이터베이스에 저장되기 전에 암호화된다.)

3. The configuration file is loaded at startup.
   (설정 파일은 시작 시 로드된다.)

4. The response is cached for 60 seconds.
   (응답은 60초 동안 캐시된다.)

5. Deprecated endpoints will be removed in the next release.
   (사용 중단된 엔드포인트는 다음 릴리스에서 제거될 것이다.)

6. The batch job is scheduled to run at midnight.
   (배치 작업은 자정에 실행되도록 예약되어 있다.)

7. All API calls are logged for auditing purposes.
   (모든 API 호출은 감사 목적으로 로깅된다.)

8. The session token is refreshed every 15 minutes.
   (세션 토큰은 15분마다 갱신된다.)

9. Errors are handled by the global exception handler.
   (오류는 전역 예외 처리기에 의해 처리된다.)

10. The microservice was scaled up to handle the increased traffic.
    (마이크로서비스가 증가한 트래픽을 처리하기 위해 스케일업되었다.)
```

#### 세무 + 기술 융합 수동태 표현

삼쩜삼 같은 세무 플랫폼에서 실제로 쓸 문장들:

```
1. The user's income data is fetched from the NTS (National Tax Service) API.
   (사용자의 소득 데이터가 국세청 API에서 가져와진다.)

2. The tax calculation is performed by the tax engine service.
   (세금 계산이 세금 엔진 서비스에 의해 수행된다.)

3. The filing status is updated in real-time via WebSocket.
   (신고 상태가 WebSocket을 통해 실시간으로 업데이트된다.)

4. The refund amount was recalculated after the additional deduction was applied.
   (추가 공제 적용 후 환급액이 재계산되었다.)

5. User consent is required before personal data can be processed.
   (개인정보 처리 전에 사용자 동의가 필요하다.)
```

---

### PRACTICE — 연습 문제

#### 연습 1: 능동태 → 수동태 변환

다음 능동태 문장을 수동태로 바꾸시오.

```
1. The developer fixed the bug.
   → _______________________________________________

2. The system processes the payment.
   → _______________________________________________

3. The NTS reviews the tax return.
   → _______________________________________________

4. The team will deploy the update tonight.
   → _______________________________________________

5. The accountant calculated the deduction.
   → _______________________________________________
```

<details>
<summary>정답 확인</summary>

```
1. The bug was fixed (by the developer).
2. The payment is processed (by the system).
3. The tax return is reviewed (by the NTS).
4. The update will be deployed (by the team) tonight.
5. The deduction was calculated (by the accountant).
```

</details>

#### 연습 2: 빈칸 채우기

적절한 수동태 형태를 빈칸에 넣으시오.

```
1. The configuration file _______ (load) at application startup.
2. All tax returns _______ (file) before April 15th.
3. The error _______ (log) by the monitoring system yesterday.
4. The refund _______ (issue) within two weeks.
5. The API key _______ (revoke) after the security breach.
6. The batch process _______ (schedule) to run every night.
7. The data _______ (encrypt) before transmission.
8. A new tax regulation _______ (announce) by the government last week.
```

<details>
<summary>정답 확인</summary>

```
1. is loaded
2. must be filed / should be filed
3. was logged
4. will be issued
5. was revoked
6. is scheduled
7. is encrypted
8. was announced
```

</details>

#### 연습 3: 영작 (한국어 → 영어 수동태)

다음 한국어를 영어 수동태로 작성하시오.

```
1. 이 엔드포인트는 GET 메서드로 접근할 수 있다.
   → _______________________________________________

2. 세금 신고서가 어제 제출되었다.
   → _______________________________________________

3. 이 기능은 다음 스프린트에서 구현될 것이다.
   → _______________________________________________

4. 데이터베이스가 매일 자정에 백업된다.
   → _______________________________________________

5. 모든 거래 내역은 감사를 위해 기록된다.
   → _______________________________________________
```

<details>
<summary>정답 예시</summary>

```
1. This endpoint can be accessed via GET method.
2. The tax return was filed yesterday.
3. This feature will be implemented in the next sprint.
4. The database is backed up at midnight every day.
5. All transaction records are logged for auditing purposes.
```

</details>

---

## Lesson 25 — 수동태의 다양한 시제와 조동사 결합

### WHY — 왜 수동태에 시제와 조동사를 결합하는가

#### 수동태 + 시제 = 더 정밀한 시점 표현

Lesson 24에서 기본 수동태를 배웠다. 하지만 현실의 문장은 단순하지 않다.

```
"이 PR은 지금 리뷰 중이다."           → 현재진행 수동태
"이 버그는 이미 수정되었다."           → 현재완료 수동태
"이 기능은 테스트되어야 한다."         → 조동사 + 수동태
"이 API는 GET으로 접근할 수 있다."    → 조동사 + 수동태
```

영어는 **시제와 조동사를 수동태에 결합**함으로써, 하나의 문장 안에 "관점 + 시점 + 가능성/의무"를 모두 담을 수 있다.

> 이것은 마치 Java의 **제네릭 타입 조합**과 같다.
> `List<String>` 하나만으로도 "컬렉션 + 타입 안전성"을 동시에 표현하듯,
> `is being reviewed` 하나만으로 "수동 + 진행 + 현재"를 동시에 표현한다.

---

### CORE — 수동태 시제/조동사 결합 공식

#### 수동태 시제 전체 맵

| 시제 | 공식 | 예문 | 의미 |
|------|------|------|------|
| **단순 현재** | `is/are + p.p.` | `The data **is stored** in MySQL.` | 데이터가 MySQL에 저장된다 |
| **단순 과거** | `was/were + p.p.` | `The data **was stored** in MySQL.` | 데이터가 MySQL에 저장되었다 |
| **단순 미래** | `will be + p.p.` | `The data **will be stored** in MySQL.` | 데이터가 MySQL에 저장될 것이다 |
| **현재 진행** | `is/are being + p.p.` | `The data **is being migrated**.` | 데이터가 마이그레이션 중이다 |
| **과거 진행** | `was/were being + p.p.` | `The data **was being migrated**.` | 데이터가 마이그레이션 중이었다 |
| **현재 완료** | `has/have been + p.p.` | `The data **has been migrated**.` | 데이터가 마이그레이션 완료되었다 |
| **과거 완료** | `had been + p.p.` | `The data **had been migrated**.` | 데이터가 (그때 이전에) 마이그레이션 완료되어 있었다 |
| **미래 완료** | `will have been + p.p.` | `The data **will have been migrated** by Friday.` | 금요일까지 데이터 마이그레이션이 완료되어 있을 것이다 |

#### 조동사 + 수동태 결합

| 조동사 | 공식 | 예문 | 의미 |
|--------|------|------|------|
| **can** | `can be + p.p.` | `This endpoint **can be accessed** via GET.` | ~될 수 있다 (가능) |
| **could** | `could be + p.p.` | `The error **could be caused** by a null pointer.` | ~될 수도 있다 (가능성) |
| **should** | `should be + p.p.` | `Input **should be validated** before processing.` | ~되어야 한다 (권장) |
| **must** | `must be + p.p.` | `The password **must be encrypted**.` | ~되어야만 한다 (필수) |
| **may** | `may be + p.p.` | `The feature **may be deprecated** soon.` | ~될 수도 있다 (약한 가능성) |
| **will** | `will be + p.p.` | `The fix **will be deployed** tonight.` | ~될 것이다 (미래) |
| **might** | `might be + p.p.` | `Some data **might be lost** during migration.` | ~될지도 모른다 (불확실) |
| **has to** | `has to be + p.p.` | `The report **has to be submitted** by Friday.` | ~되어야 한다 (의무) |

---

### EXPAND — 실전 문장 확장

#### 진행 수동태 (`is/are being + p.p.`)

"지금 ~되고 있는 중이다"를 표현한다.

```
기본:     The code is being reviewed.
          (코드가 리뷰되고 있다.)

확장 1:   The code is being reviewed by the senior developer.
          (코드가 시니어 개발자에 의해 리뷰되고 있다.)

확장 2:   The code is being reviewed by the senior developer
          as part of the sprint review process.
          (코드가 스프린트 리뷰 프로세스의 일환으로 시니어 개발자에 의해 리뷰되고 있다.)

확장 3:   The pull request containing the tax calculation logic
          is currently being reviewed by the senior developer
          as part of the sprint review process,
          and it is expected to be approved by end of day.
          (세금 계산 로직이 포함된 풀 리퀘스트가 현재 스프린트 리뷰 프로세스의 일환으로
          시니어 개발자에 의해 리뷰되고 있으며, 오늘 중으로 승인될 것으로 예상된다.)
```

#### 완료 수동태 (`has/have been + p.p.`)

"이미 ~되었다 (그리고 그 결과가 현재까지 유효하다)"를 표현한다.

```
기본:     The bug has been fixed.
          (버그가 수정되었다. → 지금 수정된 상태다.)

확장 1:   The bug has been fixed and deployed to staging.
          (버그가 수정되어 스테이징에 배포되었다.)

확장 2:   The critical bug that was causing incorrect tax calculations
          has been fixed and deployed to the staging environment
          for QA verification.
          (잘못된 세금 계산을 유발하던 치명적 버그가 수정되어
          QA 검증을 위해 스테이징 환경에 배포되었다.)
```

> **`was fixed` vs `has been fixed` 차이**:
> - `The bug was fixed yesterday.` → 어제 고쳤다는 "과거 사실"만 진술
> - `The bug has been fixed.` → 고쳐졌고 "지금도 고쳐진 상태"임을 강조

#### 조동사 + 수동태 실전 확장

```
기본:     Input should be validated.
          (입력값은 검증되어야 한다.)

확장 1:   User input should be validated before being stored.
          (사용자 입력값은 저장되기 전에 검증되어야 한다.)

확장 2:   All user input should be validated on the server side
          before being stored in the database,
          even if client-side validation has already been performed.
          (모든 사용자 입력값은, 클라이언트 측 검증이 이미 수행되었더라도,
          데이터베이스에 저장되기 전에 서버 측에서 검증되어야 한다.)
```

---

### CODE — 개발자 비유로 재이해

#### 수동태 시제 = Git 상태 비유

| 수동태 시제 | Git 비유 | 예문 |
|------------|----------|------|
| `is reviewed` (단순 현재) | `status: OPEN` (리뷰 대상) | The PR is reviewed every week. |
| `is being reviewed` (진행) | `status: IN_REVIEW` (리뷰 중) | The PR is being reviewed right now. |
| `has been reviewed` (완료) | `status: APPROVED` (리뷰 완료) | The PR has been reviewed and approved. |
| `was reviewed` (과거) | `git log` (과거 기록) | The PR was reviewed last Monday. |
| `will be reviewed` (미래) | `scheduled` (예정) | The PR will be reviewed tomorrow. |

#### 조동사 + 수동태 = 코드의 접근 제어자(Access Modifier)

```java
// can be + p.p. = public (누구나 접근 가능)
// "This method can be accessed by any class."
public void calculate() { }

// should be + p.p. = protected (권장되는 접근 방식)
// "This method should be accessed through the parent class."
protected void validate() { }

// must be + p.p. = private + @Required (반드시 필요)
// "This field must be initialized before use."
@Required
private String taxId;

// might be + p.p. = @Nullable (있을 수도 없을 수도)
// "This value might be set to null."
@Nullable
private String middleName;
```

#### API 문서에서의 수동태 패턴

REST API 문서는 수동태의 보물창고다.

```markdown
## GET /api/v1/tax-returns/{id}

**Description**: A tax return **can be retrieved** by its ID.

**Authentication**: A valid API key **must be provided** in the header.

**Response**: The tax return object **is returned** in JSON format.

**Errors**:
- 401: The request **was not authenticated**.
- 403: Access to this resource **is forbidden**.
- 404: The requested resource **was not found**.
- 429: Too many requests. The client **is being rate-limited**.
- 500: An internal server error **has occurred**.

**Deprecation Notice**: This endpoint **will be deprecated** in v3
and **should be replaced** with the new `/api/v2/returns/{id}` endpoint.
```

#### Spring Framework 로그/에러 메시지의 수동태

```
// 실제로 Spring에서 보는 로그 메시지들
"Bean 'taxService' was created."
"Transaction is being rolled back."
"Connection pool has been initialized."
"Request was rejected by the security filter."
"The application context is being refreshed."
"Cache 'taxRateCache' has been evicted."
"The scheduled task will be executed at midnight."
"DataSource must be configured before use."
```

---

### DOMAIN — 세무/기술 도메인 고급 예문

#### 세무 법률/규정 문서 스타일

세무 규정 문서는 수동태가 기본이다. 이 패턴을 익혀두면 영문 세무 자료를 쉽게 읽을 수 있다.

```
1. Income tax shall be levied on the total income of an individual.
   (소득세는 개인의 총소득에 부과된다.)

2. The tax base is determined by subtracting allowable deductions
   from the gross income.
   (과세표준은 총소득에서 허용된 공제를 차감하여 결정된다.)

3. Withholding tax must be remitted to the NTS by the 10th
   of the following month.
   (원천징수세는 다음 달 10일까지 국세청에 납부되어야 한다.)

4. A penalty of 20% may be imposed if the return is filed
   more than one month after the deadline.
   (신고서가 기한으로부터 1개월 이상 지연 제출될 경우
   20%의 가산세가 부과될 수 있다.)

5. Amended returns can be filed within five years
   from the original filing date.
   (수정 신고서는 최초 신고일로부터 5년 이내에 제출될 수 있다.)
```

#### 기술 명세서/변경 로그 스타일

```
## v2.5.0 Release Notes

### Changes
- The tax calculation engine has been optimized for performance.
- Input validation is now being enforced on all API endpoints.
- The deprecated `/v1/filing` endpoint has been removed.

### Bug Fixes
- The rounding error in tax amount calculation has been fixed.
- An issue where duplicate notifications were being sent has been resolved.

### Known Issues
- Large batch filings may be delayed during peak hours.
- This issue will be addressed in the next release.

### Security
- All passwords are now hashed using bcrypt.
- API keys must be rotated every 90 days.
- Sensitive data is encrypted at rest and in transit.
```

---

### PRACTICE — 연습 문제

#### 연습 1: 올바른 수동태 시제 선택

괄호 안에서 올바른 형태를 고르시오.

```
1. The migration (is performing / is being performed / has performing) right now.
2. The feature (has been deployed / has deployed / was being deployed) to production.
   You can use it now.
3. The tax return (was filed / is being filed / has been filing) last March.
4. The API endpoint (will be deprecate / will be deprecated / will deprecated) next year.
5. The data (must be encrypt / must be encrypted / must encrypted) before storage.
6. By the time we noticed, the server (has been hacked / had been hacked / was hacking).
```

<details>
<summary>정답 확인</summary>

```
1. is being performed (지금 수행되고 있는 중 → 현재 진행 수동태)
2. has been deployed (배포 완료되어 지금 사용 가능 → 현재 완료 수동태)
3. was filed (지난 3월에 제출됨 → 과거 수동태)
4. will be deprecated (내년에 사용 중단될 것 → 미래 수동태)
5. must be encrypted (암호화되어야 함 → 조동사 + 수동태)
6. had been hacked (알아차리기 전에 이미 해킹됨 → 과거 완료 수동태)
```

</details>

#### 연습 2: 기술 문서 작성 연습

다음 상황을 영어 수동태 문장으로 작성하시오.

```
1. [Jira 코멘트] 이 버그가 현재 조사 중입니다.
   → _______________________________________________

2. [PR 설명] 입력 검증 로직이 리팩토링되었습니다.
   → _______________________________________________

3. [릴리스 노트] 이 기능은 v3.0에서 제거될 예정입니다.
   → _______________________________________________

4. [API 문서] 이 엔드포인트는 인증된 사용자만 접근할 수 있습니다.
   → _______________________________________________

5. [에러 로그] 요청이 유효하지 않은 토큰으로 인해 거부되었습니다.
   → _______________________________________________

6. [설계 문서] 모든 이벤트는 Kafka에 발행되어야 합니다.
   → _______________________________________________
```

<details>
<summary>정답 예시</summary>

```
1. This bug is currently being investigated.
2. The input validation logic has been refactored.
3. This feature will be removed in v3.0.
4. This endpoint can only be accessed by authenticated users.
5. The request was rejected due to an invalid token.
6. All events must be published to Kafka.
```

</details>

#### 연습 3: 능동태 vs 수동태 선택 판단

다음 상황에서 능동태와 수동태 중 더 적절한 것을 고르고 이유를 설명하시오.

```
상황 1: 슬랙에서 팀원에게 보고
  (a) I fixed the login bug. ← 능동태
  (b) The login bug was fixed. ← 수동태
  → 정답: (a) — 내가 고쳤다는 것을 팀에 알리는 상황이므로 능동태가 자연스럽다.

상황 2: JIRA 티켓 상태 업데이트
  (a) John reviewed the code. ← 능동태
  (b) The code has been reviewed. ← 수동태
  → 정답: _____ — 이유: _____________________________

상황 3: API 문서 작성
  (a) The server validates the request. ← 능동태
  (b) The request is validated by the server. ← 수동태
  → 정답: _____ — 이유: _____________________________

상황 4: 장애 보고서 (Post-mortem)
  (a) Someone deleted the production database. ← 능동태
  (b) The production database was accidentally deleted. ← 수동태
  → 정답: _____ — 이유: _____________________________
```

<details>
<summary>정답 확인</summary>

```
상황 2: (b) — JIRA에서는 "누가"보다 "코드 리뷰가 완료된 상태"가 중요.
              현재완료 수동태로 "지금 리뷰 완료 상태"임을 강조.

상황 3: (b) — API 문서에서는 요청(request)이 주인공.
              사용자 관점에서 "요청이 어떻게 처리되는지"가 핵심.
              (단, Swagger 문서에서는 (a)도 쓰임. 맥락에 따라 다름.)

상황 4: (b) — 장애 보고서에서 특정 개인을 지목하는 것은 부적절.
              수동태로 "사건"에 초점을 맞추고 비난을 피함.
              "accidentally"를 추가하여 실수임을 명시.
```

</details>

---
---

# Week 10: 완료 시제 — 현재와 과거의 연결

> **한국어에는 없는 개념: "과거이면서 동시에 현재"**

---

## Lesson 26 — 현재완료: "과거~현재 연결 상태"

### WHY — 현재완료가 존재하는 진짜 이유

#### 한국어 화자에게 현재완료가 어려운 근본적 이유

한국어에는 **현재완료에 정확히 대응하는 시제가 없다.**

```
영어: I have finished the task.
한국어: 나는 그 작업을 끝냈다.  ← 과거형으로 번역됨!

영어: I finished the task.
한국어: 나는 그 작업을 끝냈다.  ← 같은 한국어!
```

한국어로는 둘 다 "끝냈다"로 번역되기 때문에, 한국어 화자는 두 문장의 **차이를 느끼기 어렵다.**

> **핵심 차이**: 현재완료는 **"현재와의 연결"**이 있고, 단순 과거는 **"과거에서 끊긴 사실"**이다.

#### 시각적으로 이해하기

```
단순 과거 (Simple Past):
과거 ──────X──────────────────── 현재
           ↑
     "그때 끝남" (과거의 사건으로 끊김)

현재완료 (Present Perfect):
과거 ──────X════════════════════ 현재
           ↑                      ↑
     "그때 시작/발생"        "지금도 관련있음"
```

#### 왜 이런 시제가 필요한가?

영어는 **시간과 사건의 관계**를 매우 정밀하게 표현하려는 언어다.

```
과거 사실만 전달:
"I read that book." (그 책 읽었어.) → 언제? 과거 어느 시점에.

과거 경험이 현재까지 유효:
"I have read that book." (그 책 읽었어.) → 그래서 지금 내용을 알고 있어.
```

| 상황 | 단순 과거 | 현재완료 |
|------|-----------|----------|
| 버그 수정 보고 | `I fixed the bug yesterday.` (어제 고쳤다는 사실) | `I have fixed the bug.` (고쳤고, 지금 동작한다) |
| 배포 상태 | `We deployed v2.0 last Friday.` (금요일에 배포한 사실) | `We have deployed v2.0.` (배포했고, 지금 운영 중) |
| 신고 상태 | `He filed the return in March.` (3월에 제출한 사실) | `He has filed the return.` (제출 완료, 처리 대기 중) |

---

### CORE — 현재완료의 기본 구조와 4가지 용법

#### 기본 공식

```
have/has + 과거분사(p.p.)

I/You/We/They + have + p.p.
He/She/It      + has  + p.p.
```

#### 최소 문장으로 체득하기

```
I have finished.      (끝냈다 → 지금 끝난 상태)
She has left.         (떠났다 → 지금 여기 없다)
They have arrived.    (도착했다 → 지금 여기 있다)
It has stopped.       (멈췄다 → 지금 멈춰 있다)
We have decided.      (결정했다 → 지금 결정된 상태)
He has approved.      (승인했다 → 지금 승인된 상태)
```

> **체화 포인트**: 현재완료를 볼 때마다 "→ 그래서 지금은?"을 자동으로 떠올려라.

#### 현재완료의 4가지 용법 — 모두 "현재와의 연결"

| 용법 | 의미 | 예문 | "지금"과의 연결 |
|------|------|------|----------------|
| **완료** | 방금/최근 완료됨 | `I have just deployed the fix.` | 지금 막 배포된 상태 |
| **경험** | ~한 적 있다 | `I have used Spring Boot before.` | 지금 그 경험이 있는 상태 |
| **계속** | 과거~현재 계속 | `I have worked here for 3 years.` | 지금도 여기서 일하고 있다 |
| **결과** | 과거 행위의 현재 결과 | `The server has crashed.` | 지금 서버가 다운된 상태 |

#### 용법별 상세 예문

**1. 완료 (Completion) — "방금 끝났다"**

```
I have just finished the code review.
(방금 코드 리뷰를 끝냈다. → 지금 리뷰 완료 상태)

The deployment has completed successfully.
(배포가 성공적으로 완료되었다. → 지금 배포된 상태)

She has already submitted the tax return.
(그녀는 이미 세금 신고서를 제출했다. → 지금 제출 완료 상태)
```

> **자주 쓰이는 부사**: `just` (방금), `already` (이미), `yet` (아직, 부정/의문문)

**2. 경험 (Experience) — "~한 적이 있다"**

```
I have implemented a tax calculation engine before.
(전에 세금 계산 엔진을 구현한 적이 있다. → 지금 그 경험이 있다)

Have you ever used Kafka in production?
(프로덕션에서 Kafka를 써본 적 있나요? → 현재 경험 유무 확인)

She has never missed a filing deadline.
(그녀는 신고 기한을 놓친 적이 없다. → 지금까지 무결한 기록)
```

> **자주 쓰이는 부사**: `ever` (경험 질문), `never` (경험 없음), `before` (이전에)

**3. 계속 (Continuation) — "지금까지 계속"**

```
I have worked at this company for three years.
(이 회사에서 3년째 일하고 있다. → 지금도 일하는 중)

The system has been running without issues since January.
(시스템이 1월부터 문제없이 돌아가고 있다. → 지금도 돌아가는 중)

We have used this tax engine since 2023.
(2023년부터 이 세금 엔진을 사용해 왔다. → 지금도 사용 중)
```

> **자주 쓰이는 전치사**: `for` (기간), `since` (시작 시점)
>
> ```
> for three years (3년 동안) — 기간의 길이
> since 2023 (2023년부터) — 시작 시점
> ```

**4. 결과 (Result) — "그래서 지금 이런 상태"**

```
The server has crashed. (We can't access it now.)
(서버가 크래시됐다. → 지금 접속 불가)

I have lost my SSH key. (I can't log in now.)
(SSH 키를 잃어버렸다. → 지금 로그인 불가)

The client has changed the requirements. (We need to redesign.)
(클라이언트가 요구사항을 바꿨다. → 지금 재설계 필요)
```

---

### EXPAND — 과거 시제와의 정밀 비교

#### 핵심 비교: 같은 상황, 다른 시제

**상황: 버그를 수정했다**

```
(A) I fixed the bug.
    → 과거의 사실. 언제 고쳤는지가 중요할 수 있음.
    → "When?" 질문이 가능: "When did you fix it?" "Yesterday."

(B) I have fixed the bug.
    → 현재 결과에 초점. 고친 결과 지금 작동한다.
    → "When?" 질문이 부자연스러움. 대신 "Great, let's deploy it."
```

**상황: 세금 신고서를 제출했다**

```
(A) The taxpayer filed the return on March 15th.
    → 3월 15일이라는 구체적 시점의 과거 사실.
    → 뉴스 보도, 기록 조회에 적합.

(B) The taxpayer has filed the return.
    → 제출이 완료된 현재 상태.
    → 상태 확인, 다음 단계 논의에 적합.
```

#### 현재완료 vs 과거 — 판단 기준표

| 기준 | 단순 과거 사용 | 현재완료 사용 |
|------|---------------|--------------|
| 구체적 시점(when) 언급 | `I deployed it **yesterday**.` | X (불가) |
| 현재 상태/결과 강조 | X (부적절) | `I **have** deployed it.` (지금 배포된 상태) |
| `ago`, `last ~`, `in 2024` | `I started **two years ago**.` | X (불가) |
| `just`, `already`, `yet` | X (부적절) | `I have **just** finished.` |
| `ever`, `never` | X (부적절) | `Have you **ever** used Redis?` |
| `for`, `since` (계속) | X (끝난 것으로 해석됨) | `I have worked here **for** 3 years.` |

> **절대 규칙**: 구체적인 과거 시점(`yesterday`, `last week`, `in 2024`, `3 days ago`)과
> 현재완료(`have/has + p.p.`)는 **함께 쓸 수 없다.**
>
> ```
> X  I have fixed the bug yesterday.     (틀림!)
> O  I fixed the bug yesterday.           (맞음)
> O  I have fixed the bug.                (맞음)
> O  I have already fixed the bug.        (맞음)
> ```

#### 대화 속에서의 전환 패턴

실제 대화에서는 현재완료로 시작한 뒤 과거로 넘어가는 것이 자연스럽다.

```
A: "Have you finished the tax module?" (현재완료 — 완료 여부 확인)
B: "Yes, I have."                      (현재완료 — 완료 상태 확인)
A: "When did you finish it?"           (과거 — 구체적 시점 질문)
B: "I finished it last night."         (과거 — 구체적 시점 답변)
```

```
A: "Has the deployment been completed?"   (현재완료 수동태 — 상태 확인)
B: "Yes, it has."
A: "Who deployed it?"                     (과거 — 구체적 행위자 질문)
B: "John deployed it at 3 PM."           (과거 — 구체적 시점+행위자)
```

---

### CODE — 개발자 비유로 재이해

#### 현재완료 = Git에서 HEAD에 반영된 커밋

```
단순 과거 = git log에 기록된 커밋 (과거의 사실)
현재완료 = HEAD가 가리키는 커밋 (현재 상태에 반영됨)
```

```bash
# 단순 과거: "그때 커밋했다"는 사실
git log --oneline
# a1b2c3d Fix tax calculation bug    ← "I fixed the bug." (과거 사실)

# 현재완료: "그 커밋이 지금 HEAD에 반영되어 있다"
git log -1 HEAD
# a1b2c3d Fix tax calculation bug    ← "I have fixed the bug." (현재 상태)
```

| 현재완료 용법 | Git 비유 |
|--------------|----------|
| 완료 | `git commit` 직후 — "방금 커밋했고, staging area는 비워진 상태" |
| 경험 | `git log --all --author=me` — "이 저장소에서 작업한 경험이 있다" |
| 계속 | `git log --since="2023-01-01"` — "2023년부터 계속 커밋해 왔다" |
| 결과 | `git status`가 보여주는 현재 상태 — "변경 결과가 반영된 상태" |

#### 현재완료 = 캐시의 상태

```java
// 단순 과거: 캐시에 저장하는 행위 자체 (과거 시점)
cache.put("taxRate", 0.35);  // "I stored the value."

// 현재완료: 캐시에 값이 들어있는 현재 상태
cache.get("taxRate");  // returns 0.35
// "I have stored the value." → 지금 캐시에 값이 있는 상태
```

```java
// 현재완료 = 상태를 가진 객체
class Task {
    private boolean completed;

    // 단순 과거: "complete()를 호출했다"는 행위의 기록
    void complete() {
        this.completed = true;
        this.completedAt = LocalDateTime.now();
    }

    // 현재완료: "complete()가 호출된 결과 지금 완료 상태"
    boolean hasBeenCompleted() {
        return this.completed;  // 현재 상태를 반환
    }
}

task.complete();                    // "I completed the task." (과거 행위)
task.hasBeenCompleted();            // true → "I have completed the task." (현재 상태)
```

#### 4가지 용법의 코드 비유

```java
// 1. 완료 (Completion) — 캐시 무효화 직후
cache.invalidate("oldRate");
// "The cache has been invalidated." → 지금 무효화된 상태

// 2. 경험 (Experience) — 기능 플래그 확인
featureFlags.hasEverBeenEnabled("betaTaxEngine");
// "This feature has been enabled before." → 활성화된 적이 있다

// 3. 계속 (Continuation) — 업타임 확인
Duration uptime = server.getUptime();  // 3650 hours
// "The server has been running for 3650 hours." → 지금도 돌아가는 중

// 4. 결과 (Result) — 현재 상태 반환
if (connection.isClosed()) {
    // "The connection has been closed." → 지금 닫혀 있다
    throw new ConnectionClosedException();
}
```

---

### DOMAIN — 세무/기술 도메인 실전 예문

#### 세무 도메인 현재완료

```
1. The taxpayer has filed all required returns for the current year.
   (납세자가 올해 필요한 모든 신고서를 제출했다. → 제출 완료 상태)

2. We have received the amended return from the client.
   (고객으로부터 수정 신고서를 받았다. → 지금 수정 신고서가 있다)

3. The NTS has updated the income tax rates for 2026.
   (국세청이 2026년 소득세율을 업데이트했다. → 새 세율이 적용된 상태)

4. Have you ever been audited by the tax authority?
   (세무 당국에 의해 감사를 받은 적이 있나요?)

5. The deduction limit has not been changed since 2020.
   (공제 한도가 2020년 이후로 변경되지 않았다. → 지금도 같은 한도)

6. She has claimed the education expense deduction for three consecutive years.
   (그녀는 3년 연속으로 교육비 공제를 신청해 왔다. → 올해도 유효)

7. The penalty has already been waived by the NTS.
   (가산세가 이미 국세청에 의해 면제되었다. → 면제된 상태)

8. I have just finished calculating the estimated tax for Q1.
   (방금 1분기 예상 세금 계산을 끝냈다. → 결과가 나온 상태)
```

#### 기술 도메인 현재완료

```
1. We have migrated the database to PostgreSQL.
   (데이터베이스를 PostgreSQL로 마이그레이션했다. → 지금 PostgreSQL 사용 중)

2. The team has adopted a microservices architecture since last year.
   (팀이 작년부터 마이크로서비스 아키텍처를 채택해 왔다. → 지금도 사용 중)

3. I have never seen this error before.
   (이 에러를 전에 본 적이 없다.)

4. The CI/CD pipeline has failed three times today.
   (CI/CD 파이프라인이 오늘 3번 실패했다. → 오늘이 아직 안 끝남)

5. Have you pushed the latest changes to the remote repository?
   (최신 변경사항을 원격 저장소에 푸시했나요? → 현재 상태 확인)

6. The memory leak has been identified and patched.
   (메모리 누수가 확인되어 패치되었다. → 지금 패치된 상태)

7. We have been using Spring Batch for our tax calculation jobs for two years.
   (세금 계산 작업에 Spring Batch를 2년째 사용해 왔다. → 지금도 사용 중)

8. The API response time has improved significantly after the optimization.
   (최적화 후 API 응답 시간이 크게 개선되었다. → 지금 개선된 상태)
```

#### 슬랙/JIRA에서 자주 쓰는 현재완료 표현

```
Daily Standup:
"I have completed the tax engine refactoring."
"I have started working on the batch processing module."
"I haven't finished the unit tests yet."

JIRA Comments:
"The code review has been completed."
"All test cases have passed."
"The feature has been merged into the develop branch."

Slack Messages:
"Hey, I've deployed the hotfix to staging."
"Has anyone reviewed my PR yet?"
"The client has just approved the design."
```

---

### PRACTICE — 연습 문제

#### 연습 1: 과거 vs 현재완료 선택

올바른 시제를 고르고 이유를 설명하시오.

```
1. I (fixed / have fixed) the bug. You can test it now.
2. I (fixed / have fixed) the bug yesterday at 3 PM.
3. She (worked / has worked) at Samsung for 5 years. (아직 재직 중)
4. She (worked / has worked) at Samsung for 5 years. (이미 퇴사)
5. The server (crashed / has crashed). We need to restart it.
6. The server (crashed / has crashed) at 2 AM last night.
7. (Did you ever use / Have you ever used) Kubernetes?
8. We (launched / have launched) the service in 2023.
```

<details>
<summary>정답 확인</summary>

```
1. have fixed — "You can test it now"에서 현재 상태(지금 테스트 가능)와 연결.
2. fixed — "yesterday at 3 PM"이라는 구체적 과거 시점이 있으므로 단순 과거.
3. has worked — 아직 재직 중이므로 과거~현재 연결(계속 용법).
4. worked — 이미 퇴사했으므로 과거 사실(끊긴 과거).
5. has crashed — 서버가 크래시된 결과 "지금" 재시작이 필요(결과 용법).
6. crashed — "at 2 AM last night"이라는 구체적 과거 시점.
7. Have you ever used — 경험을 묻는 질문은 현재완료.
8. launched — "in 2023"이라는 구체적 과거 시점.
```

</details>

#### 연습 2: 빈칸 채우기

`have/has + 적절한 과거분사`로 빈칸을 채우시오.

```
1. The team _______ (deploy) the new version to production.
2. I _______ (never / see) this kind of tax regulation before.
3. The database migration _______ (complete) successfully.
4. _______ you _______ (push) the code to the remote repository yet?
5. She _______ (work) on the tax engine since January.
6. We _______ (already / resolve) all critical bugs.
7. The client _______ (not / approve) the final design yet.
8. How many tax returns _______ you _______ (process) this month?
```

<details>
<summary>정답 확인</summary>

```
1. has deployed
2. have never seen
3. has completed (또는 has been completed — 수동태)
4. Have you pushed
5. has worked
6. have already resolved
7. has not approved (hasn't approved)
8. have you processed
```

</details>

#### 연습 3: 영작 (한국어 → 현재완료 영어)

다음 한국어 문장을 현재완료를 사용하여 영작하시오.

```
1. 방금 코드 리뷰를 끝냈습니다.
   → _______________________________________________

2. 이 API를 사용해 본 적이 있나요?
   → _______________________________________________

3. 우리는 2022년부터 이 세금 엔진을 사용해 왔습니다.
   → _______________________________________________

4. 서버가 다운되었습니다. (지금 서비스 불가)
   → _______________________________________________

5. 그는 아직 세금 신고서를 제출하지 않았습니다.
   → _______________________________________________

6. 팀이 마이크로서비스 아키텍처로 전환 완료했습니다.
   → _______________________________________________
```

<details>
<summary>정답 예시</summary>

```
1. I have just finished the code review.
2. Have you ever used this API?
3. We have used this tax engine since 2022.
4. The server has gone down. / The server has crashed.
5. He has not filed the tax return yet. / He hasn't filed the tax return yet.
6. The team has completed the transition to a microservices architecture.
```

</details>

---
---

## Lesson 27 — 과거완료와 미래완료

### WHY — 왜 "완료의 완료"가 필요한가

#### 시간의 기준점을 이동시키다

현재완료가 "과거 → 현재"의 연결이었다면,
과거완료는 "더 먼 과거 → 과거"의 연결이고,
미래완료는 "현재/과거 → 미래"의 연결이다.

```
현재완료:  과거 ────────═══════ 현재
           (과거의 일이 현재에 연결)

과거완료:  더 먼 과거 ══════ 과거 ──── 현재
           (더 먼 과거의 일이 과거 시점에 연결)

미래완료:  현재 ═══════════════ 미래
           (지금부터의 일이 미래 시점까지 연결)
```

#### 왜 이것이 필요한가?

**과거완료가 필요한 상황:**

두 개의 과거 사건이 있고, 순서를 명확히 해야 할 때.

```
상황: "내가 도착했을 때, 회의는 이미 끝나 있었다."

(a) When I arrived, the meeting ended.
    → 문법적으로는 맞지만, 두 사건이 거의 동시에 일어난 것처럼 읽힐 수 있다.

(b) When I arrived, the meeting had ended.
    → 내가 도착하기 "전에" 회의가 끝났음이 명확하다.
```

**미래완료가 필요한 상황:**

미래의 특정 시점까지 완료를 보장해야 할 때.

```
상황: "금요일까지 이 작업을 끝내놓겠습니다."

(a) I will finish by Friday.
    → 금요일에 끝낸다. (금요일 당일 마무리할 수도)

(b) I will have finished by Friday.
    → 금요일이 되면 이미 끝나 있을 것이다. (더 확실한 완료 보장)
```

---

### CORE — 과거완료와 미래완료의 기본 구조

#### 과거완료 (Past Perfect)

```
had + 과거분사(p.p.)

"과거의 특정 시점 이전에 이미 완료된 상태"
```

최소 문장으로 체득:

```
It had already started.       (그것은 이미 시작되어 있었다.)
She had left.                 (그녀는 떠나 있었다.)
They had finished.            (그들은 끝내놓은 상태였다.)
The bug had existed for weeks. (그 버그는 몇 주째 존재해 있었다.)
I had never seen it before.   (나는 전에 그것을 본 적이 없었다.)
```

#### 미래완료 (Future Perfect)

```
will have + 과거분사(p.p.)

"미래의 특정 시점까지 완료되어 있을 상태"
```

최소 문장으로 체득:

```
I will have finished.         (끝내놓은 상태일 것이다.)
It will have been resolved.   (해결된 상태일 것이다.)
We will have deployed.        (배포해놓은 상태일 것이다.)
She will have filed it.       (제출해놓은 상태일 것이다.)
They will have completed it.  (완료해놓은 상태일 것이다.)
```

#### 시각적 비교

```
과거완료:
  ══X════ Y ──────── 현재
  (X가 먼저)  (Y 시점)
  "had done"  "과거의 기준점"

  "By the time Y happened, X had already been done."

현재완료:
  ════════ X ═══════ 현재
                     "현재의 기준점"
  "have done"

미래완료:
  현재 ═══════════ Z
                   (Z 시점)
                   "미래의 기준점"
  "will have done"

  "By the time Z happens, we will have done it."
```

---

### EXPAND — 문장 확장과 실전 패턴

#### 과거완료: 패턴별 확장

**패턴 1: By the time + 과거, 주어 + had + p.p.**

```
기본:     By the time I arrived, the meeting had ended.
          (내가 도착했을 때, 회의는 이미 끝나 있었다.)

확장 1:   By the time I arrived at the office,
          the deployment had already been completed by the DevOps team.
          (내가 사무실에 도착했을 때, 배포는 DevOps 팀에 의해 이미 완료되어 있었다.)

확장 2:   By the time the tax filing deadline arrived on March 31st,
          over 90% of our users had already submitted their returns
          through our automated filing system.
          (3월 31일 세금 신고 기한이 도착했을 때,
          우리 사용자의 90% 이상이 자동 신고 시스템을 통해
          이미 신고서를 제출해 놓은 상태였다.)
```

**패턴 2: Before/After + 과거, 주어 + had + p.p.**

```
Before I joined the team, they had already built the tax engine.
(내가 팀에 합류하기 전에, 그들은 이미 세금 엔진을 구축해 놓았었다.)

After the bug had been reported, the team immediately started investigating.
(버그가 보고된 후, 팀은 즉시 조사를 시작했다.)

The developer realized that he had forgotten to handle the edge case.
(개발자는 엣지 케이스를 처리하는 것을 잊었었다는 것을 깨달았다.)
```

**패턴 3: 과거완료 + because/so/when**

```
The batch job failed because the database connection had timed out.
(배치 작업이 실패했다, 왜냐하면 데이터베이스 연결이 타임아웃되어 있었기 때문이다.)

The test passed because the developer had fixed the calculation logic.
(테스트가 통과했다, 왜냐하면 개발자가 계산 로직을 수정해 놓았기 때문이다.)

When I checked the logs, I found that the error had occurred at 3 AM.
(로그를 확인했을 때, 에러가 새벽 3시에 발생했었다는 것을 발견했다.)
```

#### 미래완료: 패턴별 확장

**패턴 1: By + 미래 시점, 주어 + will have + p.p.**

```
기본:     I will have finished by Friday.
          (금요일까지 끝내놓겠다.)

확장 1:   By the end of this sprint,
          I will have completed the tax calculation module.
          (이번 스프린트 끝까지 세금 계산 모듈을 완성해 놓겠다.)

확장 2:   By the time the tax filing season begins in March,
          we will have fully tested and deployed
          the new automated filing system to production.
          (3월에 세금 신고 시즌이 시작될 때까지,
          우리는 새로운 자동 신고 시스템을 완전히 테스트하고
          프로덕션에 배포해 놓을 것이다.)
```

**패턴 2: Before + 미래 시점, 주어 + will have + p.p.**

```
Before the next release, we will have resolved all critical bugs.
(다음 릴리스 전에, 모든 치명적 버그를 해결해 놓을 것이다.)

Before the deadline, the team will have reviewed all pull requests.
(기한 전에, 팀이 모든 풀 리퀘스트를 리뷰해 놓을 것이다.)
```

**패턴 3: 미래완료의 누적/경험 표현**

```
By December, I will have worked here for two years.
(12월이면 여기서 일한 지 2년이 된다.)

By next month, the system will have processed
over one million tax returns.
(다음 달이면 시스템이 100만 건 이상의 세금 신고서를 처리한 것이 된다.)
```

---

### CODE — 개발자 비유로 재이해

#### 과거완료 = 이전 커밋 시점의 상태 (git diff)

```bash
# 과거완료: "어떤 과거 시점에서 봤을 때, 그 이전에 이미 완료된 것"

# 현재 커밋: abc123 (현재 = "과거의 기준점")
# 이전 커밋: def456 (과거완료 = "그 이전에 이미 완료된 것")

git diff def456..abc123
# "abc123 시점에서 봤을 때, def456에서 이미 변경되어 있던 것들"
# → "By the time we reached abc123, the changes had already been made in def456."
```

```java
// 과거완료 = 트랜잭션 이전의 상태 (Rollback 시점)
@Transactional
void processReturn(TaxReturn taxReturn) {
    // 트랜잭션 시작 시점의 데이터 상태 = "과거완료"
    // "Before the transaction started, the data had been in state X."

    taxReturn.setStatus(PROCESSING);
    // 트랜잭션 실행 중 = "과거"

    // 롤백 시: "had been" 상태로 되돌아감
}
```

#### 미래완료 = 마일스톤/스프린트 완료 예정

```java
// 미래완료 = Sprint Goal에 대한 약속

// Sprint Planning에서:
// "By the end of Sprint 12, we will have completed the following:"
class Sprint12Goal {
    // will have been completed by sprint end
    Task taxEngineRefactoring;     // 세금 엔진 리팩토링 완료 예정
    Task batchJobOptimization;     // 배치 작업 최적화 완료 예정
    Task unitTestCoverage80;       // 단위 테스트 커버리지 80% 달성 예정
}
```

```
// CI/CD 파이프라인에서의 미래완료
Pipeline stages:
1. Build        → "By the time testing starts, the build will have completed."
2. Test         → "By the time deployment starts, all tests will have passed."
3. Deploy       → "By the time monitoring begins, the deployment will have finished."
4. Monitor      → 최종 상태 확인
```

#### 과거완료 = Event Sourcing에서 이전 이벤트

```java
// Event Sourcing에서 과거완료:
// "이 이벤트가 발생했을 때, 이전 이벤트들이 이미 적용되어 있었다"

List<DomainEvent> events = List.of(
    new TaxReturnCreated(),       // 1. 신고서 생성됨
    new DeductionApplied(),       // 2. 공제 적용됨
    new TaxAmountCalculated(),    // 3. 세금 계산됨
    new ReturnSubmitted()         // 4. 신고서 제출됨
);

// 이벤트 4(ReturnSubmitted) 시점에서 보면:
// "By the time the return was submitted(과거),
//  the tax amount had already been calculated(과거완료),
//  and the deduction had been applied(과거완료)."
```

#### 시제 비교 총정리 — 코드 비유

| 시제 | 코드 비유 | 예문 |
|------|-----------|------|
| 단순 과거 | `git log` — 과거 기록 | `I deployed it last night.` |
| 현재완료 | `git status` — 현재 반영 상태 | `I have deployed it.` |
| 과거완료 | `git diff HEAD~2..HEAD~1` — 과거 시점 이전의 상태 | `By the time I checked, it had been deployed.` |
| 미래완료 | Sprint Goal — 미래 완료 예정 | `By Friday, I will have deployed it.` |

---

### DOMAIN — 세무/기술 도메인 실전 예문

#### 과거완료 — 세무 도메인

```
1. By the time the audit began, the company had already prepared all documents.
   (감사가 시작되었을 때, 회사는 이미 모든 서류를 준비해 놓은 상태였다.)

2. The taxpayer discovered that he had overpaid taxes for the past three years.
   (납세자는 지난 3년간 세금을 과다 납부했었다는 것을 발견했다.)

3. The amended return was filed because the accountant had made
   an error in the original calculation.
   (수정 신고서가 제출된 것은 회계사가 원래 계산에서
   오류를 범했었기 때문이다.)

4. Before the new tax law took effect, many businesses had already
   adjusted their accounting systems.
   (새 세법이 시행되기 전에, 많은 기업들이 이미
   회계 시스템을 조정해 놓은 상태였다.)

5. The NTS imposed a penalty because the return had not been filed on time.
   (국세청이 과태료를 부과한 것은 신고서가 제때 제출되지 않았었기 때문이다.)
```

#### 과거완료 — 기술 도메인

```
1. By the time the on-call engineer responded, the auto-scaling had already
   resolved the traffic spike.
   (온콜 엔지니어가 대응했을 때, 오토스케일링이 이미
   트래픽 급증을 해결해 놓은 상태였다.)

2. The deployment failed because the migration script had not been executed.
   (배포가 실패한 것은 마이그레이션 스크립트가 실행되지 않았었기 때문이다.)

3. We realized that the data had been corrupted during the migration.
   (데이터가 마이그레이션 중에 손상되었었다는 것을 우리가 깨달았다.)

4. Before we implemented the new caching strategy,
   the API had been experiencing frequent timeouts.
   (새 캐싱 전략을 구현하기 전에,
   API가 빈번한 타임아웃을 겪고 있었었다.)

5. The team had spent two weeks debugging before they found the root cause.
   (팀은 근본 원인을 찾기 전에 2주 동안 디버깅을 했었다.)
```

#### 미래완료 — 세무 도메인

```
1. By the end of March, all individual taxpayers will have filed
   their comprehensive income tax returns.
   (3월 말까지 모든 개인 납세자가 종합소득세 신고서를 제출해 놓을 것이다.)

2. By the time the tax filing season ends,
   our system will have processed over 500,000 returns.
   (세금 신고 시즌이 끝날 때까지,
   우리 시스템은 50만 건 이상의 신고서를 처리해 놓을 것이다.)

3. The refund will have been issued by the end of next month.
   (환급금은 다음 달 말까지 지급되어 있을 것이다.)

4. Before the audit starts in June,
   we will have organized all financial records.
   (6월 감사가 시작되기 전에,
   우리는 모든 재무 기록을 정리해 놓을 것이다.)
```

#### 미래완료 — 기술 도메인

```
1. By the end of Q2, we will have migrated all services to Kubernetes.
   (2분기 말까지, 모든 서비스를 Kubernetes로 마이그레이션 완료해 놓을 것이다.)

2. By next sprint, the team will have implemented
   the new authentication module.
   (다음 스프린트까지, 팀이 새 인증 모듈을 구현해 놓을 것이다.)

3. The load test will have been completed
   before we proceed to the production deployment.
   (프로덕션 배포를 진행하기 전에, 부하 테스트가 완료되어 있을 것이다.)

4. By the time the new developer joins,
   we will have finished the documentation.
   (새 개발자가 합류할 때까지, 문서 작성을 완료해 놓을 것이다.)

5. By December, this service will have been running
   in production for a full year without downtime.
   (12월이면, 이 서비스가 다운타임 없이 프로덕션에서
   꼬박 1년 동안 운영된 것이 된다.)
```

---

### PRACTICE — 연습 문제

#### 연습 1: 과거완료 vs 단순 과거 선택

올바른 시제를 고르시오.

```
1. By the time I (arrived / had arrived), the meeting (already ended / had already ended).
2. The deployment (failed / had failed) because someone (deleted / had deleted)
   the config file.
3. When I (checked / had checked) the dashboard, the error
   (already / had already) been resolved.
4. After the team (finished / had finished) the sprint review,
   they (went / had gone) out for dinner.
5. I (realized / had realized) that I (forgot / had forgotten) to add the unit tests.
```

<details>
<summary>정답 확인</summary>

```
1. arrived (기준 시점) / had already ended (그 이전에 완료)
   → By the time I arrived, the meeting had already ended.

2. failed (결과 — 과거) / had deleted (원인 — 그 이전에 발생)
   → The deployment failed because someone had deleted the config file.

3. checked (기준 시점) / had already (그 이전에 완료)
   → When I checked the dashboard, the error had already been resolved.

4. had finished (먼저 완료) / went (그 다음 행동)
   → After the team had finished the sprint review, they went out for dinner.

5. realized (깨닫는 시점) / had forgotten (그 이전에 잊음)
   → I realized that I had forgotten to add the unit tests.
```

</details>

#### 연습 2: 미래완료 문장 완성

`will have + p.p.` 형태로 빈칸을 채우시오.

```
1. By the end of this week, I _______ (complete) the tax engine module.
2. By the time you read this, the deployment _______ (finish).
3. Before the next release, all bugs _______ (fix).
4. By March, the system _______ (process) over a million returns.
5. By the time the new regulation takes effect,
   we _______ (update) our calculation logic.
6. By next year, I _______ (work) here for three years.
```

<details>
<summary>정답 확인</summary>

```
1. will have completed
2. will have finished
3. will have been fixed (수동태)
4. will have processed
5. will have updated
6. will have worked
```

</details>

#### 연습 3: 종합 영작 — 과거완료 / 미래완료

다음 상황을 영어로 작성하시오.

```
1. [과거완료] 배포가 실패한 이유는 테스트가 실행되지 않았었기 때문이다.
   → _______________________________________________

2. [과거완료] 내가 확인했을 때, 다른 팀원이 이미 그 이슈를 해결해 놓았었다.
   → _______________________________________________

3. [미래완료] 다음 스프린트가 끝날 때까지 모든 API 엔드포인트를 구현해 놓겠다.
   → _______________________________________________

4. [미래완료] 세금 신고 시즌이 시작되기 전에, 우리는 시스템 부하 테스트를 완료해 놓을 것이다.
   → _______________________________________________

5. [과거완료] 새 세법이 발표되기 전에, 우리 팀은 이미 변경 가능성을 예측하고 있었다.
   → _______________________________________________

6. [미래완료] 12월이면 이 프로젝트를 시작한 지 6개월이 된다.
   → _______________________________________________
```

<details>
<summary>정답 예시</summary>

```
1. The deployment failed because the tests had not been run.

2. When I checked, another team member had already resolved the issue.

3. By the end of the next sprint, I will have implemented all API endpoints.

4. Before the tax filing season begins, we will have completed
   the system load testing.

5. Before the new tax law was announced, our team had already been
   anticipating potential changes.

6. By December, it will have been six months since we started this project.
```

</details>

#### 연습 4: 시제 총정리 — 하나의 시나리오로 4가지 시제 사용

다음 시나리오를 4가지 시제로 각각 표현해 보시오.

**시나리오: 세금 계산 엔진 리팩토링**

```
단순 과거:
"We refactored the tax calculation engine last month."
(우리는 지난달에 세금 계산 엔진을 리팩토링했다.)
→ 과거의 사실

현재완료:
"We have refactored the tax calculation engine."
(우리는 세금 계산 엔진을 리팩토링했다.)
→ 리팩토링이 완료되어 지금 새 엔진이 동작 중

과거완료:
"By the time the filing season started,
we had already refactored the tax calculation engine."
(신고 시즌이 시작되었을 때,
우리는 이미 세금 계산 엔진을 리팩토링해 놓은 상태였다.)
→ 신고 시즌이라는 과거 시점 이전에 완료

미래완료:
"By next quarter, we will have refactored the tax calculation engine."
(다음 분기까지, 세금 계산 엔진 리팩토링을 완료해 놓을 것이다.)
→ 미래 시점까지의 완료 예정
```

**연습: 다음 시나리오를 4가지 시제로 작성하시오.**

시나리오: 데이터베이스 마이그레이션

```
단순 과거:    _______________________________________________
현재완료:     _______________________________________________
과거완료:     _______________________________________________
미래완료:     _______________________________________________
```

<details>
<summary>정답 예시</summary>

```
단순 과거:    We migrated the database to PostgreSQL last Friday.
현재완료:     We have migrated the database to PostgreSQL. (It's running on PostgreSQL now.)
과거완료:     By the time the new developer joined, we had already migrated the database.
미래완료:     By the end of this sprint, we will have migrated the database to PostgreSQL.
```

</details>

---

## Phase 4 핵심 요약

### 수동태 (Week 9)

```
공식: be + 과거분사(p.p.)

핵심: 행위자가 아닌 대상/결과에 초점을 맞추는 관점의 전환

필수 패턴:
- 단순 수동:    is/was + p.p.         "The bug was fixed."
- 진행 수동:    is being + p.p.       "The code is being reviewed."
- 완료 수동:    has been + p.p.       "The task has been completed."
- 조동사 수동:  can/should be + p.p.  "Input should be validated."
```

### 완료 시제 (Week 10)

```
핵심: "시간의 연결" — 두 시점을 잇는 다리

현재완료: have/has + p.p.
→ "과거의 일이 현재와 연결"
→ 4가지 용법: 완료/경험/계속/결과

과거완료: had + p.p.
→ "과거의 과거" — 두 과거 사건의 순서 표현

미래완료: will have + p.p.
→ "미래 시점까지의 완료" — 마감/마일스톤 표현
```

### 개발자를 위한 비유 총정리

| 문법 개념 | 프로그래밍 비유 |
|-----------|----------------|
| 수동태 | Event Sourcing의 이벤트 이름 (`TaxReturnFiled`) |
| 수동태 시제 | Git 상태 (`OPEN` → `IN_REVIEW` → `APPROVED`) |
| 조동사 + 수동태 | Access Modifier (`public`/`protected`/`private`) |
| 현재완료 | `git status` — HEAD에 반영된 현재 상태 |
| 과거완료 | `git diff HEAD~2..HEAD~1` — 과거 시점 이전의 상태 |
| 미래완료 | Sprint Goal — 미래 완료 예정 |

### Phase 4 체크리스트

- [ ] 수동태의 존재 이유(관점 전환)를 설명할 수 있다
- [ ] 수동태의 다양한 시제(진행/완료/조동사)를 정확히 사용할 수 있다
- [ ] API 문서/기술 명세서를 수동태로 작성할 수 있다
- [ ] 현재완료와 단순 과거의 차이를 명확히 구분할 수 있다
- [ ] 현재완료의 4가지 용법을 상황에 맞게 사용할 수 있다
- [ ] 과거완료로 두 과거 사건의 순서를 표현할 수 있다
- [ ] 미래완료로 마감/마일스톤 완료를 표현할 수 있다
- [ ] 세무 도메인에서 수동태와 완료 시제를 자연스럽게 사용할 수 있다

---

> **다음 Phase 예고**: Phase 5에서는 **조건과 가정 (가정법)**을 다룬다.
> "만약 ~라면"이라는 가정의 세계를 영어로 표현하는 법을 배운다.
> if 조건문이 프로그래밍의 `if-else`와 어떻게 다른지, 왜 영어에서는
> 가정법에 과거 시제를 쓰는지를 탐구한다.
