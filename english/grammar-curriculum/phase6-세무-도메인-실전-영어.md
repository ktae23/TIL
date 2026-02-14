# Phase 6: 세무 도메인 실전 영어 (Week 13-14)

> **"문법은 OS, 도메인 영어는 Application이다."**
> Phase 1~5에서 설치한 문법 OS 위에, 이제 세무/기술 도메인이라는 실전 애플리케이션을 올린다.
> 이 Phase를 마치면 세무 영어 문서를 읽고, 기술 문서를 해석하고, 영어 이메일을 쓸 수 있다.

---

## 이 Phase의 학습 철학

```
Phase 1-5 = Grammar OS (시제, 조동사, 수동태, 관계사, 가정법...)
Phase 6   = Domain Application (세무 용어 + 기술 영어 + 비즈니스 커뮤니케이션)

// OS 없이 앱을 돌릴 수 없듯이, 문법 없이 도메인 영어는 없다.
// 하지만 OS만 있고 앱이 없으면 쓸모가 없다.
```

Phase 6는 지금까지 배운 모든 문법을 **실전 도메인에 적용**하는 단계다.
세무사 사무실에서, 코드 리뷰에서, 이메일에서 실제로 쓰이는 영어를 다룬다.

---

# Week 13: 세무/회계 핵심 영어 표현

---

## Lesson 33 — 세무 기본 용어 영어 마스터

### WHY — 왜 세무 용어를 영어로 알아야 하는가

세무 도메인에서 일하는 개발자가 영어 세무 용어를 알아야 하는 이유는 명확하다:

1. **국제 기준 이해**: IFRS(국제회계기준), OECD 세무 가이드라인 등 핵심 문서가 영어로 작성됨
2. **API/시스템 설계**: 세무 시스템의 DB 컬럼명, API 필드명은 영어 세무 용어 기반
3. **글로벌 커뮤니케이션**: 외국계 회계법인, 해외 파트너와의 협업
4. **기술 문서 독해**: tax engine, tax calculation library의 문서가 영어로 작성됨

```java
// 왜 세무 영어 용어를 알아야 하는가 — 코드에서 바로 만난다
public class TaxReturn {
    private BigDecimal taxableIncome;      // 과세 소득
    private BigDecimal withholdingTax;     // 원천징수세
    private BigDecimal taxDeduction;       // 세금 공제
    private BigDecimal taxCredit;          // 세액 공제
    private LocalDate filingDeadline;      // 신고 기한
}
```

용어를 모르면 코드를 읽을 수 없고, 코드를 읽을 수 없으면 도메인을 이해할 수 없다.

---

### CORE — 핵심 세무 용어 체계

세무 용어는 크게 **6개 카테고리**로 나눈다. 각 카테고리를 하나의 패키지로 생각하자.

```
com.tax.domain
├── income        // 소득 관련
├── corporate     // 법인세 관련
├── vat           // 부가가치세 관련
├── bookkeeping   // 기장/회계 관련
├── filing        // 신고/납부 관련
└── personnel     // 세무 인력/자격 관련
```

---

### EXPAND — 세무 용어 종합 사전 (50+ 용어)

#### Category 1: 소득세 관련 (Income Tax)

| # | 한국어 | English | 발음 팁 | 자주 쓰이는 표현 (Collocation) |
|---|--------|---------|---------|-------------------------------|
| 1 | 소득세 | Income tax | /ˈɪnkʌm tæks/ | pay income tax, file income tax |
| 2 | 과세 소득 | Taxable income | /ˈtæksəbl/ | calculate taxable income |
| 3 | 비과세 소득 | Non-taxable income / Tax-exempt income | /tæks ɪɡˈzempt/ | classified as non-taxable |
| 4 | 종합소득세 | Comprehensive income tax | /ˌkɑːmprɪˈhensɪv/ | comprehensive income tax return |
| 5 | 근로소득 | Employment income / Earned income | /ɜːrnd/ | report earned income |
| 6 | 사업소득 | Business income | | declare business income |
| 7 | 이자소득 | Interest income | /ˈɪntrəst/ | interest income from deposits |
| 8 | 배당소득 | Dividend income | /ˈdɪvɪdend/ | receive dividend income |
| 9 | 양도소득 | Capital gains | /ˈkæpɪtl ɡeɪnz/ | capital gains tax |
| 10 | 퇴직소득 | Retirement income / Severance pay | /rɪˈtaɪərmənt/ | retirement income tax |
| 11 | 기타소득 | Other income / Miscellaneous income | /ˌmɪsəˈleɪniəs/ | miscellaneous income category |
| 12 | 과세표준 | Tax base | /tæks beɪs/ | determine the tax base |
| 13 | 세율 | Tax rate | /tæks reɪt/ | progressive tax rate |
| 14 | 누진세율 | Progressive tax rate | /prəˈɡresɪv/ | apply the progressive tax rate |
| 15 | 원천징수 | Withholding (tax) | /wɪðˈhoʊldɪŋ/ | withholding tax on salary |
| 16 | 원천징수영수증 | Withholding tax receipt | | issue a withholding tax receipt |
| 17 | 연말정산 | Year-end tax settlement / Year-end adjustment | | perform year-end tax settlement |

#### Category 2: 공제 관련 (Deductions & Credits)

| # | 한국어 | English | 발음 팁 | 자주 쓰이는 표현 (Collocation) |
|---|--------|---------|---------|-------------------------------|
| 18 | 소득공제 | Income deduction | /dɪˈdʌkʃn/ | claim an income deduction |
| 19 | 세액공제 | Tax credit | /tæks ˈkredɪt/ | eligible for a tax credit |
| 20 | 기본공제 | Basic deduction / Personal exemption | /ɪɡˈzempʃn/ | basic deduction for dependents |
| 21 | 특별공제 | Special deduction | | apply special deduction |
| 22 | 의료비 공제 | Medical expense deduction | /ˈmedɪkl ɪkˈspens/ | medical expense deduction limit |
| 23 | 교육비 공제 | Education expense deduction | | education expense tax credit |
| 24 | 기부금 공제 | Charitable donation deduction | /ˈtʃærɪtəbl/ | charitable donation tax benefit |
| 25 | 감가상각 | Depreciation | /dɪˌpriːʃiˈeɪʃn/ | calculate depreciation expense |
| 26 | 손금 | Deductible expense | /dɪˈdʌktəbl/ | treated as a deductible expense |
| 27 | 익금 | Taxable revenue / Gross income inclusion | | included in gross income |

#### Category 3: 법인세 관련 (Corporate Tax)

| # | 한국어 | English | 발음 팁 | 자주 쓰이는 표현 (Collocation) |
|---|--------|---------|---------|-------------------------------|
| 28 | 법인세 | Corporate tax / Corporate income tax | /ˈkɔːrpərət/ | corporate tax liability |
| 29 | 법인 | Corporation / Legal entity | /ˌkɔːrpəˈreɪʃn/ | domestic corporation |
| 30 | 사업연도 | Fiscal year / Business year | /ˈfɪskl/ | end of the fiscal year |
| 31 | 이월결손금 | Carried-over loss / Net operating loss (NOL) carryforward | /ˈkæriˌoʊvər/ | NOL carryforward deduction |
| 32 | 이전가격 | Transfer pricing | /ˈtrænsfɜːr ˈpraɪsɪŋ/ | transfer pricing regulation |
| 33 | 연결납세 | Consolidated tax filing | /kənˈsɑːlɪdeɪtɪd/ | consolidated tax return |
| 34 | 법인세 신고 | Corporate tax return | | file a corporate tax return |

#### Category 4: 부가가치세 관련 (VAT)

| # | 한국어 | English | 발음 팁 | 자주 쓰이는 표현 (Collocation) |
|---|--------|---------|---------|-------------------------------|
| 35 | 부가가치세 | Value Added Tax (VAT) | /væt/ 또는 V-A-T | VAT registration |
| 36 | 매출세액 | Output VAT / Output tax | | charge output VAT |
| 37 | 매입세액 | Input VAT / Input tax | | claim input VAT credit |
| 38 | 세금계산서 | Tax invoice | /tæks ˈɪnvɔɪs/ | issue a tax invoice |
| 39 | 전자세금계산서 | Electronic tax invoice (e-Tax invoice) | | e-Tax invoice issuance |
| 40 | 영세율 | Zero-rate (VAT) | /ˈzɪroʊ reɪt/ | zero-rated supply |
| 41 | 면세 | Tax exemption / Tax-exempt | /ɪɡˈzempt/ | tax-exempt goods |
| 42 | 간이과세자 | Simplified taxpayer | /ˈsɪmplɪfaɪd/ | simplified taxation scheme |
| 43 | 일반과세자 | General taxpayer | | general taxpayer status |
| 44 | 부가세 신고 | VAT return / VAT filing | | submit a VAT return |

#### Category 5: 기장/회계 관련 (Bookkeeping & Accounting)

| # | 한국어 | English | 발음 팁 | 자주 쓰이는 표현 (Collocation) |
|---|--------|---------|---------|-------------------------------|
| 45 | 기장 | Bookkeeping | /ˈbʊkkiːpɪŋ/ | maintain bookkeeping records |
| 46 | 복식부기 | Double-entry bookkeeping | /ˈdʌbl ˈentri/ | double-entry bookkeeping system |
| 47 | 간편장부 | Simplified bookkeeping / Simple ledger | | use simplified bookkeeping |
| 48 | 장부 | Ledger / Books | /ˈledʒər/ | general ledger |
| 49 | 재무제표 | Financial statements | /faɪˈnænʃl ˈsteɪtmənts/ | prepare financial statements |
| 50 | 손익계산서 | Income statement / Profit and Loss (P&L) statement | | review the income statement |
| 51 | 대차대조표 | Balance sheet | /ˈbæləns ʃiːt/ | analyze the balance sheet |
| 52 | 현금흐름표 | Cash flow statement | /kæʃ floʊ/ | cash flow from operations |
| 53 | 매출 | Revenue / Sales | /ˈrevənjuː/ | total revenue for the period |
| 54 | 매출원가 | Cost of Goods Sold (COGS) | /kɒst əv ɡʊdz soʊld/ | calculate COGS |
| 55 | 영업이익 | Operating profit / Operating income | /ˈɑːpəreɪtɪŋ/ | report operating profit |
| 56 | 당기순이익 | Net income / Net profit | /net ˈɪnkʌm/ | net income after tax |

#### Category 6: 신고/납부/인력 관련 (Filing, Payment & Personnel)

| # | 한국어 | English | 발음 팁 | 자주 쓰이는 표현 (Collocation) |
|---|--------|---------|---------|-------------------------------|
| 57 | 세금 신고 | Tax filing / Tax return | /ˈfaɪlɪŋ/ | complete the tax filing |
| 58 | 신고 기한 | Filing deadline / Due date | /ˈdedlaɪn/ | meet the filing deadline |
| 59 | 납부 | Tax payment | /ˈpeɪmənt/ | make a tax payment |
| 60 | 가산세 | Penalty tax / Surcharge | /ˈpenəlti/ | penalty for late filing |
| 61 | 환급 | Tax refund | /rɪˈfʌnd/ | receive a tax refund |
| 62 | 수정신고 | Amended return / Amended tax filing | /əˈmendɪd/ | file an amended return |
| 63 | 경정청구 | Claim for correction / Request for rectification | /ˌrektɪfɪˈkeɪʃn/ | submit a claim for correction |
| 64 | 세무조사 | Tax audit | /tæks ˈɔːdɪt/ | undergo a tax audit |
| 65 | 세무사 | Tax accountant / Tax agent | | licensed tax accountant |
| 66 | 공인회계사 | Certified Public Accountant (CPA) | /ˈsɜːrtɪfaɪd/ | hire a CPA |
| 67 | 국세청 | National Tax Service (NTS) | | NTS regulations |
| 68 | 세무서 | Tax office / District tax office | | local tax office |
| 69 | 홈택스 | Hometax (Korean e-Tax portal) | | file through Hometax |
| 70 | 전자신고 | E-filing / Electronic filing | | e-filing system |

---

### CODE — 개발자 비유로 재이해

세무 용어 체계를 Java 패키지 구조로 매핑하면 이렇다:

```java
package com.tax.domain;

/**
 * 세무 용어를 코드 관점에서 보면:
 *
 * Tax Return     = HTTP Response (세금 계산 결과를 "반환"한다)
 * Tax Filing     = API Request  (세금 데이터를 "제출"한다)
 * Tax Base       = Input Parameter (계산의 "입력값")
 * Tax Rate       = Business Rule   (적용할 "규칙")
 * Tax Credit     = Discount Coupon (최종 금액에서 "차감")
 * Tax Deduction  = Filter          (과세 대상에서 "걸러냄")
 */

// Deduction vs Credit — 개발자라면 이 차이를 코드로 이해하자
public class TaxCalculator {

    /**
     * Tax Deduction (소득공제): 과세표준을 줄인다 → WHERE 절의 필터
     * "세금을 계산하기 전에" 소득에서 빼는 것
     */
    public BigDecimal applyDeduction(BigDecimal grossIncome, BigDecimal deduction) {
        BigDecimal taxBase = grossIncome.subtract(deduction);  // 과세표준 축소
        return taxBase.multiply(taxRate);                       // 그 다음 세율 적용
    }

    /**
     * Tax Credit (세액공제): 산출세액을 줄인다 → 최종 결과에서 할인
     * "세금을 계산한 후에" 세액에서 빼는 것
     */
    public BigDecimal applyCredit(BigDecimal calculatedTax, BigDecimal credit) {
        return calculatedTax.subtract(credit);  // 최종 세액에서 직접 차감
    }
}
```

> **핵심 차이**:
> - `Deduction` = 입력값(소득)을 줄임 → SQL의 `WHERE income > deduction`
> - `Credit` = 출력값(세액)을 줄임 → `return result - credit`

```java
// DB 컬럼명으로 만나는 세무 용어
@Entity
@Table(name = "tax_returns")
public class TaxReturn {

    @Column(name = "taxpayer_id")
    private String taxpayerId;           // 납세자 ID

    @Column(name = "taxable_income")
    private BigDecimal taxableIncome;    // 과세 소득

    @Column(name = "gross_income")
    private BigDecimal grossIncome;      // 총 소득

    @Column(name = "total_deductions")
    private BigDecimal totalDeductions;  // 총 공제액

    @Column(name = "tax_base")
    private BigDecimal taxBase;          // 과세표준

    @Column(name = "calculated_tax")
    private BigDecimal calculatedTax;    // 산출세액

    @Column(name = "tax_credits")
    private BigDecimal taxCredits;       // 세액공제

    @Column(name = "final_tax")
    private BigDecimal finalTax;         // 최종 납부세액

    @Column(name = "withholding_tax")
    private BigDecimal withholdingTax;   // 원천징수세액

    @Column(name = "refund_amount")
    private BigDecimal refundAmount;     // 환급액

    @Enumerated(EnumType.STRING)
    @Column(name = "filing_status")
    private FilingStatus filingStatus;   // 신고 상태

    @Column(name = "fiscal_year")
    private Integer fiscalYear;          // 사업연도

    @Column(name = "filing_deadline")
    private LocalDate filingDeadline;    // 신고 기한
}

public enum FilingStatus {
    DRAFT,      // 작성 중
    SUBMITTED,  // 제출 완료
    ACCEPTED,   // 접수 완료
    AMENDED,    // 수정 신고
    AUDITED     // 세무조사 대상
}
```

---

### DOMAIN — 세무 도메인에서의 실제 사용

#### 실무에서 만나는 세무 영어 상황들

**상황 1: API 스펙 문서에서**
```
POST /api/v1/tax-returns
Request Body:
{
  "taxpayerId": "string",
  "fiscalYear": 2025,
  "taxableIncome": 50000000,
  "deductions": [...],
  "credits": [...]
}

Response:
{
  "calculatedTax": 7200000,
  "withholdingTax": 6000000,
  "refundAmount": 1200000,
  "filingStatus": "SUBMITTED"
}
```

**상황 2: 외국계 회계법인과의 이메일에서**
> "Please submit the **tax return** for the 2025 **fiscal year** by March 31.
> The **taxable income** should reflect all **deductions** and **credits** applied.
> We also need the **withholding tax receipts** for all employees."

**상황 3: JIRA 티켓에서**
> **Title**: Implement VAT calculation engine for e-Tax invoice
> **Description**: We need to calculate **output VAT** and **input VAT** based on
> the **tax invoices** uploaded by the user. The system should handle both
> **general taxpayers** and **simplified taxpayers**.

---

### PRACTICE — 연습 문제

#### 연습 1: 빈칸 채우기

다음 문장의 빈칸에 알맞은 세무 영어 용어를 넣으시오.

1. The company must file its ____________ (법인세 신고서) by March 31.
2. ____________ (원천징수세) is deducted from the employee's salary each month.
3. The ____________ (과세표준) is calculated after all deductions are applied.
4. A ____________ (세금계산서) must be issued for every B2B transaction.
5. If you miss the ____________ (신고 기한), a ____________ (가산세) will be imposed.
6. The taxpayer received a ____________ (환급) of 1.2 million won.
7. The ____________ (감가상각) of the equipment is spread over 5 years.
8. Our ____________ (세무사) recommended filing an ____________ (수정신고).

<details>
<summary>정답 보기</summary>

1. corporate tax return
2. Withholding tax
3. tax base
4. tax invoice
5. filing deadline, penalty tax (surcharge)
6. tax refund
7. depreciation
8. tax accountant, amended return
</details>

#### 연습 2: 한국어 → 영어 번역

다음 세무 문장을 영어로 번역하시오.

1. "부가가치세 신고는 분기별로 해야 합니다."
2. "이 거래는 영세율이 적용됩니다."
3. "간이과세자는 연 매출 8천만원 미만인 사업자입니다."
4. "연말정산에서 의료비 공제를 받을 수 있습니다."
5. "국세청이 세무조사를 실시할 예정입니다."

<details>
<summary>정답 예시</summary>

1. "VAT returns must be filed quarterly."
2. "The zero-rate (VAT) applies to this transaction." / "This transaction is zero-rated."
3. "A simplified taxpayer is a business owner whose annual revenue is less than 80 million won."
4. "You can claim a medical expense deduction in the year-end tax settlement."
5. "The National Tax Service (NTS) is planning to conduct a tax audit."
</details>

#### 연습 3: DB 컬럼명 영작

다음 한국어 필드명을 영어 DB 컬럼명(snake_case)으로 변환하시오.

| 한국어 | 영어 컬럼명 |
|--------|------------|
| 납세자 번호 | ? |
| 과세 소득 | ? |
| 세액 공제 합계 | ? |
| 신고 상태 | ? |
| 매출세액 | ? |
| 기장 유형 | ? |
| 환급 금액 | ? |
| 가산세 금액 | ? |

<details>
<summary>정답 보기</summary>

| 한국어 | 영어 컬럼명 |
|--------|------------|
| 납세자 번호 | `taxpayer_id` |
| 과세 소득 | `taxable_income` |
| 세액 공제 합계 | `total_tax_credits` |
| 신고 상태 | `filing_status` |
| 매출세액 | `output_vat` |
| 기장 유형 | `bookkeeping_type` |
| 환급 금액 | `refund_amount` |
| 가산세 금액 | `penalty_amount` |
</details>

---
---

## Lesson 34 — 세무 도메인 영어 문장 패턴

### WHY — 왜 문장 패턴을 익혀야 하는가

단어를 아는 것과 문장을 만드는 것은 전혀 다른 능력이다.

```
단어 = 변수 선언
문장 패턴 = 함수 정의

// 변수만 있으면 프로그램이 안 돈다.
// 함수가 있어야 변수가 의미를 가진다.
```

세무 문서에는 **반복되는 문장 패턴**이 있다. 이 패턴을 익히면:
- 세무 규정을 빠르게 읽을 수 있다
- 세무 관련 이메일/보고서를 쓸 수 있다
- Phase 1~5에서 배운 문법이 실전에서 어떻게 쓰이는지 체감한다

---

### CORE — 세무 문장의 5대 패턴

세무 영어 문장은 대부분 다음 5가지 패턴에 속한다:

| 패턴 | 문법 근거 | 세무 맥락 | 예문 |
|------|-----------|-----------|------|
| **의무/규정** | 조동사 must/shall | ~해야 한다 | The return **must be filed** by March 31. |
| **조건/적용** | if/when 조건절 | ~하면 ~이 적용된다 | **If** income exceeds the threshold, a higher rate **applies**. |
| **수동 처리** | be + p.p. | ~이 처리/계산된다 | The tax **is calculated** based on the tax base. |
| **완료/결과** | 현재완료 | ~이 완료되었다 | The deduction **has been applied** to the return. |
| **가정/예외** | 가정법/unless | ~이 아니면 ~이다 | **Unless** the taxpayer files on time, penalties **will be** imposed. |

> 이 5가지 패턴은 Phase 2(시제), Phase 3(조동사), Phase 4(수동태)에서 배운 문법의 실전 적용이다.

---

### EXPAND — 30+ 실전 문장 패턴

#### Pattern 1: 의무/규정 (Must/Shall — 조동사 Phase 3 적용)

세무 법규, 규정, 안내문에서 가장 많이 등장하는 패턴이다.

| # | 영어 문장 | 한국어 의미 | 사용되는 문법 |
|---|-----------|-------------|---------------|
| 1 | The tax return **must be filed** by the deadline. | 세금 신고서는 기한까지 제출되어야 한다. | 조동사 + 수동태 |
| 2 | All taxpayers **shall report** their income annually. | 모든 납세자는 매년 소득을 신고해야 한다. | shall (법률 의무) |
| 3 | Corporations **are required to** file their tax returns within 3 months after the fiscal year ends. | 법인은 사업연도 종료 후 3개월 이내에 신고해야 한다. | be required to |
| 4 | The employer **must withhold** income tax from the employee's salary. | 고용주는 직원 급여에서 소득세를 원천징수해야 한다. | must + 동사원형 |
| 5 | A tax invoice **must be issued** within the prescribed period. | 세금계산서는 정해진 기간 내에 발행되어야 한다. | must + be p.p. |
| 6 | The taxpayer **is obligated to** keep records for at least 5 years. | 납세자는 최소 5년간 기록을 보관할 의무가 있다. | be obligated to |
| 7 | Estimated tax **should be paid** quarterly. | 예정 세액은 분기별로 납부되어야 한다. | should (권고) |

**주의: must vs shall vs should**
```
must   = 강한 의무 (일반 규정)     → "You MUST file by March 31."
shall  = 법적 의무 (법률 조문)     → "The taxpayer SHALL report..."
should = 권고/권장 (가이드라인)    → "You SHOULD keep receipts."

// 프로그래밍 비유:
// must   = throw new IllegalStateException() — 안 하면 에러
// shall  = @NotNull annotation — 계약 위반
// should = @Deprecated — 하는 게 좋지만 안 해도 당장 에러는 아님
```

#### Pattern 2: 조건/적용 (If/When — 조건절 Phase 5 적용)

세무 규정은 본질적으로 조건문이다: "~하면 ~이다."

| # | 영어 문장 | 한국어 의미 | 사용되는 문법 |
|---|-----------|-------------|---------------|
| 8 | **If** the taxable income **exceeds** 50 million won, a 24% tax rate **applies**. | 과세 소득이 5천만원을 초과하면 24% 세율이 적용된다. | if + 현재시제 |
| 9 | **When** VAT is charged, the seller **must issue** a tax invoice. | 부가세가 부과될 때 판매자는 세금계산서를 발행해야 한다. | when + must |
| 10 | **If** the return **is not filed** on time, a penalty **will be imposed**. | 기한 내에 신고하지 않으면 가산세가 부과된다. | if + 수동 + will |
| 11 | **Where** the taxpayer has multiple sources of income, they **must file** a comprehensive return. | 납세자에게 다수의 소득원이 있는 경우, 종합 신고를 해야 한다. | where (= if/when, 법률체) |
| 12 | The zero-rate **applies** only **if** the goods **are exported**. | 영세율은 물품이 수출되는 경우에만 적용된다. | if + 수동 |
| 13 | **In the event that** the taxpayer fails to comply, enforcement action **may be taken**. | 납세자가 이행하지 않는 경우, 강제 조치가 취해질 수 있다. | in the event that (격식) |
| 14 | **Unless** otherwise specified, the standard rate **shall apply**. | 달리 명시하지 않는 한, 표준 세율이 적용된다. | unless (= if not) |
| 15 | **Provided that** all documents are submitted, the refund **will be processed** within 30 days. | 모든 서류가 제출되면, 환급은 30일 이내에 처리된다. | provided that (조건) |

```java
// 세무 규정 = if-else 체인
public BigDecimal calculateIncomeTax(BigDecimal taxableIncome) {
    // "If the taxable income exceeds 50M, a 24% rate applies."
    if (taxableIncome.compareTo(new BigDecimal("50000000")) > 0) {
        return taxableIncome.multiply(new BigDecimal("0.24"));
    }
    // "If the taxable income exceeds 12M, a 15% rate applies."
    else if (taxableIncome.compareTo(new BigDecimal("12000000")) > 0) {
        return taxableIncome.multiply(new BigDecimal("0.15"));
    }
    // "Otherwise, a 6% rate applies."
    else {
        return taxableIncome.multiply(new BigDecimal("0.06"));
    }
}
```

#### Pattern 3: 수동 처리 (Passive Voice — Phase 4 적용)

세무 문서는 **행위자보다 행위 자체**에 초점을 맞추기 때문에 수동태가 매우 빈번하다.

| # | 영어 문장 | 한국어 의미 | 능동태 비교 |
|---|-----------|-------------|-------------|
| 16 | The tax **is calculated** based on the tax base. | 세금은 과세표준을 기반으로 계산된다. | (We) calculate the tax... |
| 17 | Withholding tax **is deducted** from the employee's monthly salary. | 원천징수세는 직원의 월급에서 공제된다. | (The employer) deducts... |
| 18 | The penalty **is imposed** for late filing. | 가산세는 지연 신고에 대해 부과된다. | (The NTS) imposes... |
| 19 | The tax invoice **was issued** on January 15. | 세금계산서는 1월 15일에 발행되었다. | (The seller) issued... |
| 20 | Input VAT **can be claimed** against output VAT. | 매입세액은 매출세액에서 공제받을 수 있다. | (The taxpayer) can claim... |
| 21 | The depreciation **is spread** over the useful life of the asset. | 감가상각은 자산의 내용연수에 걸쳐 배분된다. | (The accountant) spreads... |
| 22 | The amended return **was submitted** on March 28. | 수정신고서는 3월 28일에 제출되었다. | (The taxpayer) submitted... |

> **왜 세무 영어에서 수동태가 많은가?**
> 1. 규정은 "누가" 하는지보다 "무엇이" 되는지가 중요하다
> 2. 법률 문서는 객관적/비인칭적 표현을 선호한다
> 3. 시스템 관점에서도 "세금이 계산된다"가 자연스럽다

#### Pattern 4: 완료/결과 (Present Perfect — Phase 2 적용)

신고 완료, 처리 결과를 나타낼 때 현재완료가 쓰인다.

| # | 영어 문장 | 한국어 의미 | 뉘앙스 |
|---|-----------|-------------|--------|
| 23 | The return **has been filed** successfully. | 신고서가 성공적으로 제출되었다. | 완료 + 현재 결과 |
| 24 | All deductions **have been applied** to the taxable income. | 모든 공제가 과세 소득에 적용되었다. | 완료 상태 강조 |
| 25 | The NTS **has completed** the tax audit. | 국세청이 세무조사를 완료했다. | 결과 존재 |
| 26 | We **have not yet received** the withholding tax receipt. | 원천징수영수증을 아직 받지 못했다. | 미완료 상태 |
| 27 | The refund **has been processed** and will be deposited within 5 business days. | 환급이 처리되었으며 5영업일 이내에 입금된다. | 완료 → 미래 |

```java
// 현재완료 = 상태 조회
// "The return has been filed" → filingStatus == SUBMITTED
if (taxReturn.getFilingStatus() == FilingStatus.SUBMITTED) {
    log.info("The return has been filed successfully.");
}

// 과거시제 = 이벤트 기록
// "The return was filed on March 15" → 특정 시점의 행위
log.info("The return was filed on {}", taxReturn.getFiledDate());
```

#### Pattern 5: 가정/예외 (Subjunctive/Unless — Phase 5 적용)

예외 사항, 가정 상황, 벌칙 조건 등을 나타낸다.

| # | 영어 문장 | 한국어 의미 | 사용되는 문법 |
|---|-----------|-------------|---------------|
| 28 | **Unless** the taxpayer has a reasonable excuse, the penalty **will apply**. | 납세자에게 합리적 사유가 없는 한, 가산세가 적용된다. | unless + will |
| 29 | **Had** the taxpayer filed on time, the penalty **would not have been** imposed. | 납세자가 제때 신고했더라면, 가산세가 부과되지 않았을 것이다. | 가정법 과거완료 도치 |
| 30 | **Should** the taxpayer wish to appeal, they **may file** a claim within 90 days. | 납세자가 이의를 제기하고자 하는 경우, 90일 이내에 청구할 수 있다. | should 도치 (격식) |
| 31 | **If** the income **were** to be reclassified, the tax liability **would change** significantly. | 만약 소득이 재분류된다면, 세금 부담이 크게 달라질 것이다. | 가정법 과거 (were to) |
| 32 | The exemption applies, **provided that** the conditions are met. | 조건이 충족되는 한, 면세가 적용된다. | provided that |
| 33 | **Notwithstanding** the above provision, the Commissioner **may grant** an extension. | 위 규정에도 불구하고, 국세청장은 연장을 허가할 수 있다. | notwithstanding (법률체) |

---

### CODE — 문법 패턴의 프로그래밍 매핑

```java
/**
 * 세무 문장 패턴 = 프로그래밍 패턴
 *
 * Pattern 1 (의무) = Validation Rules
 *   "must be filed"     → @NotNull, @Valid
 *
 * Pattern 2 (조건) = if-else / switch-case
 *   "if income exceeds" → if (income > threshold)
 *
 * Pattern 3 (수동) = Service Layer Processing
 *   "tax is calculated" → taxService.calculate(input)
 *
 * Pattern 4 (완료) = Status Check
 *   "has been filed"    → status == COMPLETED
 *
 * Pattern 5 (가정) = Exception Handling
 *   "unless ... penalty" → try-catch, fallback logic
 */

// 세무 규정을 코드로 표현하면:
public class TaxFilingService {

    // Pattern 1: "The return must be filed by the deadline."
    @Scheduled(cron = "0 0 0 31 3 *")  // March 31
    public void enforceFilingDeadline() {
        List<TaxReturn> unfiled = repository.findUnfiledReturns();
        unfiled.forEach(this::imposePenalty);
    }

    // Pattern 2: "If income exceeds the threshold, a higher rate applies."
    public TaxRate determineRate(BigDecimal taxableIncome) {
        return taxRateTable.stream()
            .filter(bracket -> taxableIncome.compareTo(bracket.getThreshold()) > 0)
            .findFirst()
            .orElse(TaxRate.DEFAULT);
    }

    // Pattern 3: "The tax is calculated based on the tax base."
    public BigDecimal calculateTax(TaxReturn taxReturn) {
        BigDecimal taxBase = taxReturn.getTaxableIncome()
            .subtract(taxReturn.getTotalDeductions());
        return taxBase.multiply(determineRate(taxBase).getValue());
    }

    // Pattern 4: "The return has been filed successfully."
    public boolean hasBeenFiled(String taxpayerId, int fiscalYear) {
        return repository.findByTaxpayerIdAndFiscalYear(taxpayerId, fiscalYear)
            .map(r -> r.getStatus() == FilingStatus.SUBMITTED)
            .orElse(false);
    }

    // Pattern 5: "Unless the taxpayer has a reasonable excuse, the penalty will apply."
    public void imposePenalty(TaxReturn taxReturn) {
        if (!hasReasonableExcuse(taxReturn.getTaxpayerId())) {
            penaltyService.impose(taxReturn);
        }
    }
}
```

---

### DOMAIN — 세무 문서/이메일 실전 읽기

#### 실전 1: 세무 규정 읽기 (영문 국세기본법 스타일)

```
Article 47 (Penalty Tax for Failure to File Return)

(1) Where a taxpayer fails to file a tax return by the filing deadline
    prescribed under the relevant tax law, the Commissioner of the
    National Tax Service shall impose a penalty tax equivalent to 20%
    of the tax payable.

(2) Notwithstanding paragraph (1), if the taxpayer has filed the return
    within one month after the deadline, the penalty shall be reduced
    to 10% of the tax payable.

(3) The penalty under paragraph (1) shall not apply where the taxpayer
    proves that there was a reasonable cause for the failure to file.
```

> 분석:
> - **shall impose** → 법적 의무 (Pattern 1)
> - **Where a taxpayer fails** → 조건 (Pattern 2, where = if)
> - **shall be reduced** → 수동 + 의무 (Pattern 1 + 3)
> - **shall not apply where** → 예외 (Pattern 5)

#### 실전 2: 세무 관련 이메일

```
Subject: Year-End Tax Settlement — Documents Required

Dear Mr. Kim,

I am writing to inform you that the year-end tax settlement for 2025
is now in progress. Please submit the following documents by February 15:

1. Medical expense receipts for 2025
2. Education expense receipts (if applicable)
3. Charitable donation receipts
4. Housing loan interest certificates

If you have already submitted these documents through Hometax, no further
action is required.

Should you have any questions, please do not hesitate to contact our office.

Best regards,
Park, Tax Accountant
```

> 분석:
> - **I am writing to inform you** → 비즈니스 이메일 정형 표현
> - **Please submit ... by February 15** → 의무/요청 (Pattern 1)
> - **If you have already submitted** → 조건 + 완료 (Pattern 2 + 4)
> - **Should you have any questions** → 격식 조건 도치 (Pattern 5)

---

### PRACTICE — 연습 문제

#### 연습 1: 패턴 식별

다음 문장이 어떤 패턴(1~5)에 해당하는지 표시하시오.

| 문장 | 패턴 |
|------|------|
| The VAT return has been submitted. | ? |
| All businesses must register for VAT. | ? |
| If the taxpayer disagrees, they may file an appeal. | ? |
| The penalty is calculated at 20% of the unpaid tax. | ? |
| Unless the documents are provided, the deduction will be denied. | ? |
| Had the company kept proper records, the audit would have been smoother. | ? |
| The refund was processed on April 5. | ? |

<details>
<summary>정답 보기</summary>

| 문장 | 패턴 |
|------|------|
| The VAT return has been submitted. | **4** (완료/결과) |
| All businesses must register for VAT. | **1** (의무/규정) |
| If the taxpayer disagrees, they may file an appeal. | **2** (조건/적용) |
| The penalty is calculated at 20% of the unpaid tax. | **3** (수동 처리) |
| Unless the documents are provided, the deduction will be denied. | **5** (가정/예외) |
| Had the company kept proper records, the audit would have been smoother. | **5** (가정/예외 — 가정법 과거완료) |
| The refund was processed on April 5. | **3** (수동 처리 — 과거 수동) |
</details>

#### 연습 2: 영작 — 세무 문장 만들기

다음 한국어를 위 5가지 패턴을 활용하여 영어로 번역하시오.

1. "전자세금계산서는 거래일로부터 10일 이내에 발행되어야 한다." (Pattern 1)
2. "매출이 8천만원 미만이면 간이과세자로 분류된다." (Pattern 2)
3. "원천징수세는 매월 10일까지 납부된다." (Pattern 3)
4. "수정신고서가 성공적으로 제출되었습니다." (Pattern 4)
5. "납세자가 기한 내에 신고했더라면, 가산세가 부과되지 않았을 것이다." (Pattern 5)

<details>
<summary>정답 예시</summary>

1. "The electronic tax invoice must be issued within 10 days from the transaction date."
2. "If annual revenue is less than 80 million won, the business is classified as a simplified taxpayer."
3. "Withholding tax is paid by the 10th of each month."
4. "The amended return has been submitted successfully."
5. "Had the taxpayer filed within the deadline, the penalty would not have been imposed."
   또는: "If the taxpayer had filed within the deadline, the penalty would not have been imposed."
</details>

#### 연습 3: 세무 규정 영작

다음 한국어 규정을 영어 법률체로 작성하시오.

> "사업자가 부가가치세 확정신고를 기한까지 하지 않은 경우, 무신고 가산세로서 납부할 세액의 20%에 해당하는 금액이 부과된다. 다만, 기한 후 1개월 이내에 신고한 경우에는 10%로 감경된다."

<details>
<summary>정답 예시</summary>

"Where a business operator fails to file a final VAT return by the prescribed deadline, a penalty tax equivalent to 20% of the tax payable shall be imposed for failure to file. Provided, however, that where the return is filed within one month after the deadline, the penalty shall be reduced to 10%."
</details>

---
---

# Week 14: 개발자를 위한 기술 영어

---

## Lesson 35 — API 문서/기술 문서 읽기 패턴

### WHY — 왜 기술 영어 패턴을 익혀야 하는가

개발자가 하루에 읽는 영어의 대부분은 **기술 문서**다:

- API 문서 (Javadoc, Swagger/OpenAPI)
- README 파일
- PR(Pull Request) 리뷰 코멘트
- 커밋 메시지
- Stack Overflow 답변
- Spring/Java 공식 문서
- 에러 메시지

이 모든 텍스트에는 **반복되는 패턴**이 있다. 패턴을 알면 속독이 된다.

```
// 기술 영어 = 제한된 패턴의 반복
// 일반 영어: 무한한 표현 가능성
// 기술 영어: 50개 정도의 패턴이 90%를 커버

// 마치 디자인 패턴처럼:
// Singleton, Factory, Observer... 이 패턴들만 알면 대부분의 코드를 이해할 수 있듯이
// "Returns...", "Throws...", "Defaults to..." 이 패턴만 알면 대부분의 문서를 읽을 수 있다.
```

---

### CORE — 기술 문서의 5대 영역

| 영역 | 주요 패턴 | 빈도 |
|------|-----------|------|
| API Documentation | Returns, Throws, Params | 매우 높음 |
| README / Guide | Getting Started, Usage, Configuration | 높음 |
| PR Review | LGTM, Could you, Nit | 매우 높음 |
| Commit Message | Add, Fix, Refactor, Update | 매우 높음 |
| Error Message | Failed to, Unable to, Cannot | 높음 |

---

### EXPAND — 영역별 실전 패턴

#### Area 1: API Documentation (Javadoc / Swagger)

**메서드 설명 패턴:**

```java
/**
 * 패턴 1: "Returns ..."  — 메서드가 무엇을 반환하는지
 * Returns the calculated tax amount for the given income.
 *
 * 패턴 2: "Throws ... when/if ..." — 예외 발생 조건
 * @throws IllegalArgumentException if the income is negative
 * @throws TaxCalculationException when the tax rate is not found
 *
 * 패턴 3: "@param ... the ..." — 매개변수 설명
 * @param taxableIncome the taxable income after deductions
 * @param fiscalYear the fiscal year for the calculation
 *
 * 패턴 4: "If not specified, defaults to ..." — 기본값 설명
 * @param taxRate the tax rate to apply. If not specified, defaults to
 *                the standard rate for the fiscal year.
 *
 * 패턴 5: "@return the ..." — 반환값 설명
 * @return the calculated tax amount, never null
 *
 * 패턴 6: "@since ..." — 버전 정보
 * @since 2.0
 *
 * 패턴 7: "@deprecated Use X instead" — 폐기 안내
 * @deprecated Use {@link #calculateTaxV2} instead. This method will be
 *             removed in version 4.0.
 *
 * 패턴 8: "Note that ..." — 주의사항
 * Note that this method is not thread-safe. Use {@link #calculateTaxAsync}
 * for concurrent access.
 *
 * 패턴 9: "This method is equivalent to ..." — 동치 설명
 * This method is equivalent to calling {@code calculate(income, DEFAULT_RATE)}.
 */
public BigDecimal calculateTax(BigDecimal taxableIncome, int fiscalYear) {
    // ...
}
```

**Spring Framework 문서에서 자주 보이는 패턴:**

```java
/**
 * 패턴 A: "Indicates that ..." — 어노테이션 설명
 * Indicates that the annotated class is a service component.
 *
 * 패턴 B: "Marks a method as ..." — 마킹 설명
 * Marks a method as a scheduled task that runs at a fixed rate.
 *
 * 패턴 C: "Specifies the ..." — 설정 설명
 * Specifies the name of the bean to be injected.
 *
 * 패턴 D: "Used to ..." — 용도 설명
 * Used to configure the transaction isolation level.
 *
 * 패턴 E: "Enables ..." — 기능 활성화
 * Enables Spring's asynchronous method execution capability.
 */

// Spring Boot application.properties 문서 패턴:
// "The maximum number of ..."
// "Whether to enable ..."
// "The timeout in milliseconds for ..."
// "Comma-separated list of ..."
```

**Swagger/OpenAPI 문서 패턴:**

```yaml
# 패턴: "Creates a new ..." / "Retrieves the ..." / "Updates the ..."
paths:
  /api/v1/tax-returns:
    post:
      summary: Creates a new tax return          # 동사원형으로 시작
      description: |
        Creates a new tax return for the specified taxpayer and fiscal year.
        The return is created in DRAFT status.

        **Note:** The taxpayer must be registered before filing.

        Returns 201 if the tax return is created successfully.
        Returns 400 if the request body is invalid.
        Returns 409 if a return already exists for the given fiscal year.
      parameters:
        - name: taxpayerId
          description: The unique identifier of the taxpayer  # "The ..."
          required: true    # required / optional
      responses:
        '201':
          description: Tax return created successfully        # 과거분사
        '400':
          description: Invalid request body                   # 형용사 + 명사
        '404':
          description: Taxpayer not found                     # 과거분사
```

**핵심 API 문서 동사 목록:**

| 동사 | 의미 | 예시 |
|------|------|------|
| Returns | ~을 반환한다 | Returns the calculated amount. |
| Throws | ~을 던진다 (예외) | Throws an exception if null. |
| Creates | ~을 생성한다 | Creates a new tax return. |
| Retrieves | ~을 조회한다 | Retrieves the filing status. |
| Updates | ~을 수정한다 | Updates the existing record. |
| Deletes | ~을 삭제한다 | Deletes the specified entry. |
| Validates | ~을 검증한다 | Validates the input parameters. |
| Computes | ~을 계산한다 | Computes the tax liability. |
| Initializes | ~을 초기화한다 | Initializes the tax engine. |
| Converts | ~을 변환한다 | Converts the amount to KRW. |

---

#### Area 2: README / Documentation

**README 구조와 패턴:**

```markdown
# Tax Calculation Engine                    ← 프로젝트명

> A high-performance tax calculation engine  ← 한 줄 설명 ("A ..." 패턴)
> for Korean tax regulations.

## Overview                                 ← 개요 (3-5 문장)

This library provides a comprehensive tax calculation engine
that supports income tax, corporate tax, and VAT calculations
based on the latest Korean tax regulations.

## Features                                 ← 기능 목록 (동사원형 시작)

- **Calculate** income tax using progressive rates
- **Support** both general and simplified taxpayers
- **Generate** tax invoices in e-Tax format
- **Handle** year-end tax settlements automatically
- **Integrate** with NTS Hometax via API

## Getting Started                          ← 시작 가이드

### Prerequisites                           ← 사전 조건 ("You need ..." 패턴)

- Java 17 or higher
- Spring Boot 3.x
- MySQL 8.0 or PostgreSQL 14+

### Installation                            ← 설치 방법 ("Add ... to ..." 패턴)

Add the following dependency to your `build.gradle`:

### Usage                                   ← 사용법 ("To ... , use ..." 패턴)

To calculate income tax, use the `TaxCalculator` class:

### Configuration                           ← 설정 ("Set ... to ..." 패턴)

Set the following properties in `application.yml`:

## API Reference                            ← API 참조

See the [API Documentation](./docs/api.md) for detailed information.

## Contributing                             ← 기여 방법 ("Please read ..." 패턴)

Please read [CONTRIBUTING.md](./CONTRIBUTING.md) before submitting a PR.

## License                                  ← 라이센스

This project is licensed under the MIT License.
```

**자주 쓰이는 README 표현:**

| 패턴 | 의미 | 예시 |
|------|------|------|
| To [verb], [instruction] | ~하려면, ~하세요 | To install, run `npm install`. |
| Make sure (that) ... | ~을 확인하세요 | Make sure you have Java 17 installed. |
| You can also ... | ~도 가능합니다 | You can also use Docker. |
| For more information, see ... | 더 자세한 내용은 ~을 참고 | For more information, see the docs. |
| Note: ... | 참고: | Note: This is still in beta. |
| By default, ... | 기본적으로 | By default, the port is 8080. |
| This is useful when ... | ~할 때 유용합니다 | This is useful when testing locally. |
| Alternatively, ... | 또는 | Alternatively, you can use Gradle. |

---

#### Area 3: PR Review Comments

**PR 리뷰에서 가장 자주 쓰이는 표현 30선:**

**승인/칭찬:**

| 표현 | 의미 | 사용 상황 |
|------|------|-----------|
| LGTM (Looks Good To Me) | 좋아 보입니다 | 최종 승인 |
| LGTM! Nice work. | 잘했어요! | 승인 + 칭찬 |
| Ship it! | 배포해도 됩니다 | 강한 승인 |
| Awesome, this is clean! | 깔끔하네요! | 코드 품질 칭찬 |
| Thanks for the thorough tests. | 테스트 꼼꼼하게 감사합니다. | 테스트 칭찬 |

**질문/제안:**

| 표현 | 의미 | 사용 상황 |
|------|------|-----------|
| Could you explain why ...? | ~한 이유를 설명해주시겠어요? | 코드 의도 질문 |
| What do you think about ...? | ~에 대해 어떻게 생각하세요? | 대안 제안 |
| Have you considered ...? | ~을 고려해보셨나요? | 개선 제안 |
| I wonder if ... would be better. | ~이 더 나을지 궁금합니다. | 부드러운 제안 |
| Would it make sense to ...? | ~하는 게 맞을까요? | 설계 토론 |
| Not sure if this is intentional, but ... | 의도인지 모르겠지만 ... | 잠재적 버그 지적 |

**수정 요청:**

| 표현 | 의미 | 사용 상황 |
|------|------|-----------|
| Nit: ... | 사소한 점: | 작은 스타일 이슈 |
| Minor: ... | 경미한 건: | 크지 않은 이슈 |
| Can we rename this to ...? | ~로 이름을 변경할 수 있을까요? | 네이밍 제안 |
| I think we should ... | ~해야 할 것 같습니다 | 변경 요청 |
| This might cause issues when ... | ~할 때 문제가 될 수 있습니다 | 잠재적 문제 |
| Could you add a comment explaining ...? | ~을 설명하는 주석을 추가해줄 수 있나요? | 가독성 개선 |
| Let's extract this into a separate method. | 이걸 별도 메서드로 추출합시다. | 리팩토링 제안 |
| This looks like it could be simplified. | 이건 더 간단해질 수 있을 것 같습니다. | 복잡도 개선 |
| We should handle the edge case where ... | ~인 엣지 케이스를 처리해야 합니다. | 견고성 개선 |

**실전 PR 리뷰 대화 예시:**

```markdown
## PR #142: Add VAT calculation for simplified taxpayers

### Reviewer A:
> LGTM overall! A few comments:
>
> 1. `VatCalculator.java` L45: Nit: Could you rename `calc` to
>    `calculateVat` for consistency?
>
> 2. `VatCalculator.java` L72: Have you considered using
>    `BigDecimal.ZERO` instead of `new BigDecimal("0")`?
>
> 3. `VatCalculatorTest.java`: Nice test coverage! But I think
>    we should also add a test for the edge case where the
>    revenue is exactly 80 million won.

### Author (you):
> Thanks for the review!
>
> 1. Good point, renamed.
> 2. You're right, fixed.
> 3. Great catch! Added the edge case test. I also added a test
>    for null input.
>
> PTAL (Please Take Another Look).

### Reviewer A:
> LGTM! Ship it. 🚀
```

---

#### Area 4: Commit Messages

**커밋 메시지 Conventional Commits 패턴:**

```
<type>(<scope>): <description>

[optional body]
[optional footer]
```

**Type 종류와 예시:**

| Type | 의미 | 커밋 메시지 예시 |
|------|------|-----------------|
| feat | 새 기능 추가 | `feat(tax): add VAT calculation for simplified taxpayers` |
| fix | 버그 수정 | `fix(tax): correct rounding error in income tax calculation` |
| refactor | 리팩토링 | `refactor(tax): extract tax rate lookup into separate service` |
| docs | 문서 수정 | `docs(api): update tax return endpoint documentation` |
| test | 테스트 추가/수정 | `test(tax): add edge case tests for zero-rate VAT` |
| chore | 빌드/설정 변경 | `chore(deps): upgrade Spring Boot to 3.2.1` |
| perf | 성능 개선 | `perf(tax): optimize batch tax calculation query` |
| style | 코드 스타일 | `style: fix indentation in TaxReturn entity` |
| ci | CI/CD 변경 | `ci: add tax calculation integration test to pipeline` |

**좋은 커밋 메시지 패턴:**

```
# 동사원형으로 시작 (명령형)
feat: add withholding tax calculation         ← add (추가)
fix: resolve null pointer in tax filing       ← resolve (해결)
refactor: simplify tax rate determination     ← simplify (간소화)
perf: improve tax return query performance    ← improve (개선)
docs: clarify VAT registration requirements   ← clarify (명확화)

# Body: "왜" 이 변경을 했는지
feat(tax): add penalty calculation for late filing

The penalty is calculated as 20% of the unpaid tax amount.
If the return is filed within one month after the deadline,
the penalty is reduced to 10%.

Closes #234

# 나쁜 커밋 메시지:
❌ "Fixed stuff"
❌ "WIP"
❌ "Updated code"
❌ "Changes"
❌ "Modified TaxCalculator.java"   ← 파일명을 나열하지 말 것
```

---

#### Area 5: Error Messages & Logs

**에러 메시지 패턴:**

| 패턴 | 의미 | 예시 |
|------|------|------|
| Failed to [verb] | ~하는 데 실패했습니다 | Failed to calculate tax. |
| Unable to [verb] | ~할 수 없습니다 | Unable to connect to NTS API. |
| Cannot [verb] | ~할 수 없습니다 | Cannot file return: missing taxpayer ID. |
| [noun] not found | ~을 찾을 수 없습니다 | Tax rate not found for the given bracket. |
| Invalid [noun] | 유효하지 않은 ~ | Invalid fiscal year: must be between 2000 and 2025. |
| [noun] already exists | ~이 이미 존재합니다 | Tax return already exists for fiscal year 2025. |
| [noun] is required | ~은 필수입니다 | Taxpayer ID is required. |
| Unexpected [noun] | 예상치 못한 ~ | Unexpected error during tax calculation. |
| [noun] mismatch | ~이 일치하지 않습니다 | Amount mismatch between invoice and payment. |
| Access denied | 접근이 거부되었습니다 | Access denied: insufficient permissions. |
| Timed out [verb]ing | ~하는 동안 시간 초과 | Timed out waiting for NTS response. |
| [noun] has expired | ~이 만료되었습니다 | Session has expired. Please log in again. |

**로그 메시지 패턴:**

```java
// INFO: 성공적 처리 과정
log.info("Tax return filed successfully for taxpayer {}", taxpayerId);
log.info("Processing {} tax returns for fiscal year {}", count, year);
log.info("VAT calculation completed in {}ms", elapsed);

// WARN: 주의 필요 상황
log.warn("Filing deadline approaching for taxpayer {}", taxpayerId);
log.warn("Tax rate configuration not found, falling back to default");
log.warn("Deprecated API version used by client {}", clientId);

// ERROR: 오류 발생
log.error("Failed to calculate tax for taxpayer {}: {}", taxpayerId, e.getMessage());
log.error("Unable to connect to NTS API after {} retries", maxRetries);
log.error("Invalid tax return data: {}", validationErrors);
```

---

### CODE — 기술 문서 읽기의 프로그래밍 비유

```java
/**
 * 기술 문서 읽기 = Pattern Matching
 *
 * 기술 문서에서 반복되는 패턴을 코드의 정규표현식처럼 매칭한다.
 *
 * Pattern: "Returns the [noun]"
 *   → 이 메서드는 [명사]를 반환한다
 *   → 핵심 정보: 반환 타입과 의미
 *
 * Pattern: "Throws [Exception] if/when [condition]"
 *   → [조건]일 때 [예외]를 던진다
 *   → 핵심 정보: 예외 상황 파악
 *
 * Pattern: "If not specified, defaults to [value]"
 *   → 지정하지 않으면 기본값은 [값]이다
 *   → 핵심 정보: 선택적 파라미터의 기본 동작
 */

// 기술 문서 읽기 전략 = 스캐너
public class TechDocScanner {

    // Step 1: 목적 파악 → "What does this do?"
    // 첫 문장만 읽으면 된다: "Returns the ...", "Creates a ...", "Validates the ..."

    // Step 2: 입력 파악 → "What does it need?"
    // @param 섹션, Parameters 테이블을 본다

    // Step 3: 출력 파악 → "What does it give back?"
    // @return, Response 섹션을 본다

    // Step 4: 예외 파악 → "What can go wrong?"
    // @throws, Error codes, 4xx/5xx 응답을 본다

    // Step 5: 주의사항 파악 → "Anything I should know?"
    // Note:, Warning:, Important:, @deprecated 를 본다
}
```

---

### DOMAIN — Spring/Java 기술 문서 실전 예시

#### Spring Boot 공식 문서 읽기

```
## Spring Boot Auto-configuration

Spring Boot auto-configuration attempts to automatically configure
your Spring application based on the jar dependencies that you have added.

For example, if HSQLDB is on your classpath, and you have not manually
configured any database connection beans, then Spring Boot auto-configures
an in-memory database.
```

> 패턴 분석:
> - "attempts to automatically configure" → ~을 자동으로 구성하려고 한다
> - "based on" → ~을 기반으로
> - "if ... is on your classpath" → 조건
> - "and you have not ... configured" → 현재완료 부정 (아직 ~하지 않은 상태)
> - "then ... auto-configures" → 결과

#### Stack Overflow 답변 패턴

```markdown
## Question: How to calculate progressive tax rate in Java?

### Accepted Answer (Score: 127):

You can use a `TreeMap` to define the tax brackets:

    TreeMap<BigDecimal, BigDecimal> brackets = new TreeMap<>();
    brackets.put(new BigDecimal("12000000"), new BigDecimal("0.06"));
    brackets.put(new BigDecimal("50000000"), new BigDecimal("0.15"));

The key idea is to use `floorEntry()` to find the applicable bracket.

**Note:** Make sure to use `BigDecimal` instead of `double` for
monetary calculations to avoid floating-point precision issues.

**Edit:** As @user123 pointed out, you also need to handle the
cumulative calculation for progressive rates.
```

> Stack Overflow 특유의 패턴:
> - "You can use ..." → 해결책 제시
> - "The key idea is to ..." → 핵심 아이디어
> - "Note: Make sure to ..." → 주의사항
> - "Edit: As @user pointed out, ..." → 수정/보완

---

### PRACTICE — 연습 문제

#### 연습 1: API 문서 읽기

다음 Javadoc을 읽고 질문에 답하시오.

```java
/**
 * Calculates the income tax for the specified taxpayer.
 *
 * <p>The calculation is based on the progressive tax rates
 * defined in the Korean Income Tax Act. If the taxpayer has
 * filed an amended return, the most recent filing is used.
 *
 * @param taxpayerId the unique identifier of the taxpayer
 * @param fiscalYear the fiscal year for the calculation (must be >= 2000)
 * @return the calculated tax amount, or zero if no taxable income exists
 * @throws TaxpayerNotFoundException if no taxpayer is found with the given ID
 * @throws InvalidFiscalYearException if the fiscal year is before 2000
 * @since 3.1
 * @deprecated Use {@link #calculateIncomeTaxV2} instead. This method
 *             does not support the updated 2024 tax brackets.
 */
public BigDecimal calculateIncomeTax(String taxpayerId, int fiscalYear);
```

질문:
1. 이 메서드는 무엇을 반환하는가?
2. fiscalYear에 1999를 넣으면 어떻게 되는가?
3. 과세 소득이 없으면 어떤 값이 반환되는가?
4. 이 메서드를 사용해도 되는가? 왜?
5. 수정신고가 있는 경우 어떤 데이터를 사용하는가?

<details>
<summary>정답 보기</summary>

1. 계산된 세액(BigDecimal)을 반환한다. ("the calculated tax amount")
2. `InvalidFiscalYearException`이 발생한다. ("if the fiscal year is before 2000")
3. 0(zero)이 반환된다. ("or zero if no taxable income exists")
4. 사용하지 않는 것이 좋다. `@deprecated` 표시가 있으며, `calculateIncomeTaxV2`를 대신 사용해야 한다. 이유: 2024년 개정 세율 구간을 지원하지 않기 때문.
5. 가장 최근 신고 데이터를 사용한다. ("the most recent filing is used")
</details>

#### 연습 2: PR 리뷰 영작

다음 상황에서 PR 리뷰 코멘트를 영어로 작성하시오.

1. **상황**: 동료가 변수명을 `t`로 지었다. `taxAmount`가 더 나을 것 같다.
2. **상황**: 테스트 코드가 없다. 단위 테스트를 추가해달라고 요청하고 싶다.
3. **상황**: 전체적으로 코드가 좋지만, null 체크가 빠진 곳이 하나 있다.
4. **상황**: 코드 리뷰를 마치고 승인하고 싶다.
5. **상황**: BigDecimal 대신 double을 사용한 부분이 있어서 금액 계산에 문제가 될 수 있다.

<details>
<summary>정답 예시</summary>

1. "Nit: Could you rename `t` to `taxAmount` for readability? It's not immediately clear what `t` represents."

2. "Could you add unit tests for this? I think we should at least cover the happy path and the edge case where the income is zero."

3. "LGTM overall! One minor thing: I think we need a null check for `taxpayerId` on line 42. If it's null, this will throw a NullPointerException."

4. "LGTM! Clean code, good test coverage. Ship it!"

5. "I noticed you're using `double` for the tax amount calculation on line 78. This might cause precision issues with monetary values. Could you use `BigDecimal` instead? See: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/math/BigDecimal.html"
</details>

#### 연습 3: 커밋 메시지 영작

다음 변경 사항에 대한 커밋 메시지를 Conventional Commits 형식으로 작성하시오.

1. 부가세 계산 기능을 새로 추가함
2. 원천징수세 계산에서 소수점 반올림 버그를 수정함
3. 세금 계산 로직을 별도 서비스 클래스로 분리함
4. API 문서에 세금 신고 엔드포인트 설명을 추가함
5. 세무조사 대상자 조회 쿼리의 성능을 개선함

<details>
<summary>정답 예시</summary>

1. `feat(vat): add VAT calculation for general and simplified taxpayers`
2. `fix(withholding): correct rounding error in withholding tax calculation`
3. `refactor(tax): extract tax calculation logic into TaxCalculationService`
4. `docs(api): add tax filing endpoint documentation`
5. `perf(audit): optimize tax audit target query using indexed search`
</details>

---
---

## Lesson 36 — 비즈니스 커뮤니케이션

### WHY — 왜 비즈니스 영어 커뮤니케이션을 익혀야 하는가

개발자도 코드만 쓰지 않는다:

1. **이메일**: 외국인 동료/클라이언트와의 업무 이메일
2. **미팅**: 영어로 진행되는 스탠드업, 회의, 발표
3. **비동기 소통**: Slack, Teams에서의 빠른 의사소통
4. **문서 작성**: 기술 제안서, 디자인 문서, 위키

```
// 비즈니스 영어 = API 인터페이스
// 코드가 아무리 좋아도 API가 나쁘면 사용자가 쓰지 않는다.
// 기술력이 아무리 좋아도 소통이 안 되면 팀에서 빛나지 못한다.

public interface BusinessCommunication {
    Email writeEmail(Context context);           // 이메일 작성
    String speakInMeeting(Agenda agenda);         // 미팅 발언
    SlackMessage sendSlackMessage(Channel ch);    // 슬랙 메시지
}
```

---

### CORE — 비즈니스 영어의 3원칙

| 원칙 | 설명 | 개발자 비유 |
|------|------|-------------|
| **Clear** (명확하게) | 한 문장 = 한 가지 뜻 | Single Responsibility Principle |
| **Concise** (간결하게) | 불필요한 단어 제거 | Clean Code |
| **Courteous** (공손하게) | 상대를 존중하는 톤 | Good API Design (friendly error messages) |

```
// Bad API: "Error occurred. Check logs."
// Good API: "The tax calculation failed because the fiscal year (2025)
//            is not yet available. Please use 2024 or contact support."

// Bad Email: "Send me the document."
// Good Email: "Could you send me the tax return document when you get a chance?"
```

---

### EXPAND — 영역별 실전 표현

#### Area 1: 이메일 (Email)

##### 이메일의 기본 구조

```
Subject: [간결하고 구체적인 제목]

Dear [이름/직함],                     ← 인사 (Greeting)

[첫 문장: 목적 밝히기]               ← 오프닝 (Opening)

[본문: 상세 내용]                    ← 바디 (Body)

[마무리: 요청/다음 단계]             ← 클로징 (Closing)

Best regards,                        ← 맺음 (Sign-off)
[이름]
```

##### 오프닝 패턴 (Opening Lines)

| 패턴 | 사용 상황 | 격식도 |
|------|-----------|--------|
| I'm writing to inform you that ... | 정보 전달 | 격식 |
| I'm reaching out regarding ... | 주제 도입 | 준격식 |
| I wanted to follow up on ... | 후속 확인 | 준격식 |
| Thank you for your email regarding ... | 답장 | 격식 |
| As discussed in our meeting, ... | 미팅 후속 | 준격식 |
| I hope this email finds you well. | 안부 인사 | 격식 |
| Just a quick note to ... | 간단한 연락 | 비격식 |
| Per our conversation, ... | 이전 대화 참고 | 준격식 |

##### 요청 패턴 (Request Lines)

| 격식도 | 표현 | 예시 |
|--------|------|------|
| 매우 격식 | I would be grateful if you could ... | I would be grateful if you could send the tax documents. |
| 격식 | Could you please ... ? | Could you please review the attached report? |
| 준격식 | Would you mind ...ing? | Would you mind updating the filing status? |
| 준격식 | Please find attached ... | Please find attached the Q4 VAT report. |
| 비격식 | Can you ...? | Can you check the calculation? |
| 비격식 | Let me know if ... | Let me know if you need anything else. |

##### 마무리 패턴 (Closing Lines)

| 패턴 | 사용 상황 |
|------|-----------|
| Please let me know if you have any questions. | 일반적 마무리 |
| I look forward to hearing from you. | 답장 기대 |
| Thank you for your time and consideration. | 감사 표현 |
| Please do not hesitate to contact me if you need further information. | 격식 마무리 |
| I appreciate your prompt attention to this matter. | 긴급 사항 |
| Looking forward to your feedback. | 피드백 요청 |

##### 맺음말 (Sign-offs) — 격식도 순서

| 격식도 | Sign-off |
|--------|----------|
| 매우 격식 | Yours sincerely, / Respectfully, |
| 격식 | Best regards, / Kind regards, |
| 준격식 | Best, / Thanks, / Regards, |
| 비격식 | Cheers, / Thanks! |

---

##### 실전 이메일 템플릿

**템플릿 1: 세무 서류 요청 (Formal)**

```
Subject: Request for Tax Documents — FY2025

Dear Mr. Park,

I hope this email finds you well.

I am writing to request the following tax documents for fiscal year 2025:

1. Year-end tax settlement report
2. Withholding tax receipts for all employees
3. Corporate tax return (draft)
4. VAT filing records for Q1-Q4

Could you please provide these documents by February 28? We need
them to finalize the tax review before the filing deadline.

If any of the documents are not yet available, please let me know
the expected timeline.

Thank you for your time and assistance.

Best regards,
Kim Minjun
AI Service Cell — Tax Bookkeeping Team
```

**템플릿 2: 기술 이슈 보고 (Semi-formal)**

```
Subject: Bug Report — Incorrect VAT Calculation for Simplified Taxpayers

Hi Team,

I wanted to flag an issue I found during testing.

**Issue:** The VAT calculation returns incorrect results for simplified
taxpayers when the quarterly revenue is exactly 20 million won.

**Expected behavior:** The system should apply the simplified rate (1.5%).
**Actual behavior:** The system applies the general rate (10%).

**Steps to reproduce:**
1. Create a taxpayer with status "SIMPLIFIED"
2. Enter quarterly revenue of 20,000,000 won
3. Run VAT calculation

I've created a JIRA ticket (TAX-456) with more details. Could you take
a look when you get a chance?

Thanks,
Minjun
```

**템플릿 3: 외부 파트너 협업 요청 (Formal)**

```
Subject: Partnership Inquiry — Tax Calculation API Integration

Dear Integration Team,

I'm reaching out from the AI Service Cell at [Company], where we develop
tax bookkeeping automation services for Korean taxpayers.

We are currently exploring the possibility of integrating your tax
calculation API into our platform. Specifically, we are interested in:

- Income tax calculation endpoints
- VAT processing capabilities
- Bulk filing API for corporate clients

Could you provide the following information?
1. API documentation and sandbox access
2. Pricing details for enterprise usage
3. SLA and uptime guarantees

We would also appreciate the opportunity to schedule a brief call to
discuss the technical requirements in more detail.

Thank you for your consideration. I look forward to hearing from you.

Best regards,
Kim Minjun
Backend Developer
AI Service Cell — Tax Bookkeeping Team
[Company]
```

---

#### Area 2: 미팅 영어 (Meeting English)

##### 스탠드업 미팅 패턴

```
Three-part structure:
1. Yesterday / What I did:
   - "Yesterday, I worked on the VAT calculation module."
   - "I completed the PR for the withholding tax feature."
   - "I finished implementing the tax invoice API."

2. Today / What I'll do:
   - "Today, I'm going to work on the unit tests."
   - "I plan to start on the year-end settlement feature."
   - "I'll be focusing on the tax return filing endpoint."

3. Blockers / Issues:
   - "I'm blocked by the NTS API being down."
   - "I need the tax rate table from the business team."
   - "No blockers for today."
```

##### 미팅 핵심 표현 30선

**의견 제시 (Giving Opinions):**

| 표현 | 격식도 | 예시 |
|------|--------|------|
| I think we should ... | 보통 | I think we should prioritize the VAT feature. |
| I'd like to suggest ... | 격식 | I'd like to suggest a different approach. |
| In my opinion, ... | 보통 | In my opinion, we need more test coverage. |
| From my perspective, ... | 격식 | From my perspective, the deadline is tight. |
| I believe ... | 보통 | I believe this will improve performance. |

**동의/반대 (Agreeing/Disagreeing):**

| 표현 | 의미 | 격식도 |
|------|------|--------|
| I agree with that. | 동의합니다. | 보통 |
| That makes sense. | 일리가 있네요. | 보통 |
| I see your point, but ... | 말씀은 이해하지만... | 부드러운 반대 |
| I'm not sure I agree. | 동의하기 어렵습니다. | 부드러운 반대 |
| I see it differently. | 저는 다르게 봅니다. | 부드러운 반대 |
| With all due respect, I think ... | 존중하지만, 제 생각은... | 격식 반대 |

**확인/정리 (Clarifying/Summarizing):**

| 표현 | 사용 상황 |
|------|-----------|
| Just to clarify, ... | 명확하게 확인할 때 |
| If I understand correctly, ... | 이해 확인할 때 |
| To summarize, ... | 내용 정리할 때 |
| So what you're saying is ... | 상대 의견 확인할 때 |
| Can we align on ...? | 합의점 도출할 때 |
| Let me make sure I understand. | 이해 확인 전 |
| Let's circle back to ... | 이전 주제로 돌아갈 때 |

**행동 요청 (Action Items):**

| 표현 | 사용 상황 |
|------|-----------|
| Can you take the lead on this? | 담당 요청 |
| I'll take care of that. | 자신이 담당 |
| Let's follow up on this offline. | 별도 논의 제안 |
| I'll share the document after the meeting. | 후속 행동 약속 |
| Can we set a deadline for this? | 기한 설정 |
| Let's table this for now. | 나중에 논의하자 |

##### 실전 미팅 시나리오

```
[Sprint Planning Meeting]

Tech Lead: "Alright, let's discuss the priorities for this sprint.
           We have the VAT calculation feature and the tax invoice
           integration. What do you think we should tackle first?"

You:       "I'd like to suggest starting with the VAT calculation.
           The tax invoice integration depends on the calculation
           being done, so it makes sense to do VAT first."

PM:        "That makes sense. How long do you think the VAT feature
           will take?"

You:       "If I understand the requirements correctly, the core
           calculation should take about 3 days. But we also need
           to handle the edge case for simplified taxpayers, which
           might add another day."

Tech Lead: "Can you break that down into subtasks?"

You:       "Sure. I'll create the subtasks after this meeting. Just
           to clarify — are we supporting both general and simplified
           taxpayers in this sprint?"

PM:        "Yes, both. Let's also add unit tests."

You:       "Got it. I'll take care of that. To summarize: I'll work
           on VAT calculation for both taxpayer types, including tests,
           estimated at 4 days total."

Tech Lead: "LGTM. Let's move on."
```

---

#### Area 3: Slack/Teams 비동기 소통 (Async Communication)

##### Slack 메시지의 원칙

```
// Slack = 비동기 API 호출
// 1. 요청을 보내고 (메시지 전송)
// 2. 응답을 기다린다 (상대방이 읽고 답장)
//
// 따라서:
// - 한 번에 필요한 정보를 모두 포함해야 한다 (round-trip 최소화)
// - 상대방이 읽었을 때 바로 행동할 수 있어야 한다
// - 컨텍스트를 충분히 제공해야 한다
```

##### Slack 필수 표현

**질문하기:**

| 패턴 | 예시 | 톤 |
|------|------|-----|
| Quick question: ... | Quick question: Is the VAT rate 10% for all general taxpayers? | 캐주얼 |
| Hey, do you know ...? | Hey, do you know where the tax rate config is? | 캐주얼 |
| Does anyone know ...? | Does anyone know how to handle zero-rated VAT? | 채널 질문 |
| I have a question about ... | I have a question about the withholding tax flow. | 보통 |

**상태 업데이트:**

| 패턴 | 예시 |
|------|------|
| Heads up: ... | Heads up: The NTS API is down. No filings until it's back. |
| FYI: ... | FYI: I pushed the VAT calculation fix to staging. |
| Update: ... | Update: The tax invoice feature is code-complete. Starting tests now. |
| Just merged ... | Just merged the withholding tax PR. Please pull latest. |

**도움 요청:**

| 패턴 | 예시 |
|------|------|
| Could use some help with ... | Could use some help with the tax rate lookup logic. |
| Anyone available to ...? | Anyone available to review my PR? |
| I'm stuck on ... | I'm stuck on the penalty calculation. Any ideas? |
| Can someone point me to ...? | Can someone point me to the tax filing docs? |

**응답하기:**

| 패턴 | 예시 |
|------|------|
| Sure, I can help with that. | (도움 수락) |
| Let me look into it. | (확인해보겠다) |
| I'll get back to you on that. | (나중에 답변하겠다) |
| Not sure, but you might want to check ... | (방향 제시) |
| +1 / Agreed | (동의) |

##### 실전 Slack 대화 예시

**예시 1: 기술 질문 (채널)**

```
#backend-dev

You: Hey team, quick question 🧵

     I'm implementing the VAT calculation for simplified taxpayers.
     The rate should be 1.5% of the revenue, right? Or is it
     1.5% of the supply value (excluding VAT)?

     Context: I'm looking at the `VatCalculator` class and the
     current logic seems to use the supply value, but the PM's
     spec says "revenue."

     cc @park-tax-lead

Park: Good catch. It should be the supply value.
      The PM's spec uses "revenue" loosely — I'll update the doc.
      Go with supply value for now.

You: Got it, thanks! I'll proceed with supply value and add a
     comment in the code for clarity.
```

**예시 2: PR 리뷰 요청 (DM)**

```
You → Park:

Hey Park, could you review my PR when you get a chance?

PR: #142 — Add VAT calculation for simplified taxpayers
Link: github.com/company/tax-engine/pull/142

It's about 200 lines, mostly the calculation logic + tests.
No rush — end of day is fine.

Park: Sure, I'll take a look after lunch. 👍

--- (after review) ---

Park: Left a couple of comments. Mostly minor stuff.
      One thing — I think we should add an edge case test
      for when revenue is exactly at the threshold.

You: Thanks for the review! I'll address the comments and
     add that test. Will push the updates in about an hour.
```

**예시 3: 장애 상황 (채널)**

```
#incidents

You: 🚨 Alert: Tax filing API returning 500 errors

     **Impact:** Users cannot submit tax returns
     **Since:** ~10 minutes ago (2:30 PM KST)
     **Affected:** POST /api/v1/tax-returns endpoint

     I'm investigating now. Looks like a database connection
     issue. Will update in 15 minutes.

--- (15 minutes later) ---

You: Update: Found the root cause. The connection pool was
     exhausted due to a long-running query in the tax audit
     batch job. Killed the query and restarted the pool.

     Service is recovering now. Will monitor for the next
     30 minutes.

--- (30 minutes later) ---

You: ✅ Resolved. Tax filing API is back to normal.

     Root cause: Batch job for tax audit queries was holding
     too many connections. Filed TAX-789 to add connection
     timeout to the batch job config.

     @on-call — no further action needed.
```

---

### CODE — 비즈니스 커뮤니케이션의 프로그래밍 비유

```java
/**
 * 비즈니스 커뮤니케이션 = API Design
 *
 * 좋은 API와 좋은 커뮤니케이션의 공통점:
 *
 * 1. Clear Interface (명확한 인터페이스)
 *    API: 명확한 엔드포인트, 파라미터, 응답 형식
 *    이메일: 명확한 제목, 본문, 요청사항
 *
 * 2. Error Handling (에러 처리)
 *    API: 의미 있는 에러 메시지, 적절한 상태 코드
 *    이메일: 문제 상황을 명확히 설명, 해결책 제시
 *
 * 3. Versioning (버전 관리)
 *    API: v1, v2로 하위 호환성 유지
 *    이메일: 상대의 격식 수준에 맞춰 톤 조절
 *
 * 4. Documentation (문서화)
 *    API: Swagger, Javadoc
 *    이메일: 후속 메일에서 이전 내용 요약
 */

// 격식 수준 = API 버전
public enum Formality {
    VERY_FORMAL,   // "I would be most grateful if..."    → API v1 (legacy, verbose)
    FORMAL,        // "Could you please..."               → API v2 (standard)
    SEMI_FORMAL,   // "Would you mind..."                 → API v3 (modern)
    INFORMAL       // "Can you...?"                       → Internal API (compact)
}

// 이메일 = HTTP Request
public class EmailRequest {
    private String subject;       // = HTTP Method + URL (목적을 한눈에)
    private String greeting;      // = Header (메타데이터)
    private String opening;       // = Request Path (왜 보내는지)
    private String body;          // = Request Body (상세 내용)
    private String closing;       // = Expected Response (다음 단계)
    private String signOff;       // = Content-Type (형식)
}
```

---

### DOMAIN — 세무 개발자의 실전 비즈니스 영어 시나리오

#### 시나리오 1: 세무 시스템 장애 보고 이메일

```
Subject: [URGENT] Tax Filing System Outage — Action Required

Dear Operations Team,

I am writing to report a critical issue with the tax filing system.

**Issue Summary:**
Since 2:30 PM KST today (February 14), the tax return filing API
(POST /api/v1/tax-returns) has been returning HTTP 500 errors.
Users are unable to submit tax returns.

**Root Cause:**
The database connection pool was exhausted due to an unoptimized
query in the tax audit batch job (TAX-789).

**Impact:**
- Approximately 500 filing attempts have failed in the last hour
- The filing deadline for amended returns is February 28
- If not resolved promptly, this could delay year-end settlements

**Actions Taken:**
1. Terminated the problematic batch query
2. Restarted the database connection pool
3. Service has been restored as of 3:15 PM KST

**Recommended Follow-up:**
- Add connection timeout to batch job configuration
- Implement circuit breaker for the filing endpoint
- Schedule a post-mortem for next Monday

Please let me know if you need any additional information.

Best regards,
Kim Minjun
Backend Developer — Tax Bookkeeping Team
```

#### 시나리오 2: 세무 기능 제안 이메일

```
Subject: Proposal — Automated Penalty Calculation Feature

Hi Team,

I'd like to propose adding an automated penalty calculation feature
to our tax filing system.

**Background:**
Currently, penalty calculations for late filing are done manually
by the tax accountants. This is time-consuming and error-prone.

**Proposed Solution:**
Implement an automated penalty calculation engine that:
- Calculates penalties based on the number of days past the deadline
- Applies the reduced rate for filings within one month
- Handles special cases (reasonable excuse, force majeure)

**Estimated Effort:**
- Backend: 5 days (calculation engine + API)
- Frontend: 3 days (penalty display + notification)
- Testing: 2 days (including edge cases)

**Benefits:**
- Reduces manual work for tax accountants
- Eliminates calculation errors
- Provides instant feedback to users

What do you think? I can prepare a more detailed design document
if the team is interested.

Looking forward to your feedback.

Thanks,
Minjun
```

---

### PRACTICE — 연습 문제

#### 연습 1: 이메일 영작

다음 상황에 맞는 비즈니스 이메일을 영어로 작성하시오.

**상황**: 세무사(Park)에게 2025년 법인세 신고에 필요한 재무제표를 요청하는 이메일. 기한은 3월 15일까지이며, 전자 형식(PDF)으로 보내달라고 요청.

<details>
<summary>정답 예시</summary>

```
Subject: Request for Financial Statements — Corporate Tax Filing FY2025

Dear Mr. Park,

I hope this email finds you well.

I am writing to request the financial statements required for the
2025 corporate tax filing. Specifically, we need the following documents:

1. Balance sheet (as of December 31, 2025)
2. Income statement (January - December 2025)
3. Cash flow statement
4. Statement of changes in equity

Could you please provide these documents in PDF format by March 15?
This will give us sufficient time to prepare the corporate tax return
before the filing deadline.

If any of the documents require additional time, please let me know
so we can adjust our schedule accordingly.

Thank you for your assistance.

Best regards,
Kim Minjun
Backend Developer — Tax Bookkeeping Team
```
</details>

#### 연습 2: Slack 메시지 영작

다음 상황에 맞는 Slack 메시지를 영어로 작성하시오.

1. **상황**: 팀 채널에서 방금 배포한 세금 계산 기능에 대해 알리기
2. **상황**: 동료에게 DM으로 PR 리뷰 요청하기
3. **상황**: 채널에서 "과세표준 계산 로직에서 공제 순서가 어떻게 되는지" 질문하기

<details>
<summary>정답 예시</summary>

1.
```
#backend-dev

FYI: Just deployed the tax calculation feature to staging.

Changes:
- Progressive income tax calculation
- VAT for both general and simplified taxpayers
- Withholding tax deduction

Please test and let me know if you find any issues.
Prod deployment is planned for Thursday.
```

2.
```
You → Lee:

Hey Lee, could you review my PR when you have a moment?

PR #156: Implement year-end tax settlement calculation
Link: github.com/company/tax-engine/pull/156

~300 lines, mostly the settlement logic + tests.
Would appreciate your input on the deduction ordering logic.
No rush — tomorrow is fine.
```

3.
```
#tax-domain

Quick question about the tax base calculation:

When calculating the tax base (과세표준), what's the correct
order for applying deductions?

My understanding:
1. Gross income
2. Subtract income deductions (소득공제)
3. = Tax base (과세표준)
4. Apply tax rate
5. = Calculated tax (산출세액)
6. Subtract tax credits (세액공제)
7. = Final tax (결정세액)

Is step 2 correct, or should some deductions be applied after
the tax rate calculation?

cc @park-tax-lead
```
</details>

#### 연습 3: 미팅 영어 롤플레이

다음 미팅 상황에서 영어로 응답하시오.

**상황**: Sprint Review에서 Tech Lead가 묻는다:
> "We're behind schedule on the tax filing feature. What do you suggest?"

<details>
<summary>정답 예시</summary>

"I think we should break the feature into two phases. For this sprint, let's focus on the core filing flow — the basic tax return submission and validation. We can push the advanced features like amended returns and penalty calculation to the next sprint. This way, we can deliver a working MVP by the deadline. What do you think?"

**대안 1 (더 간결하게):**
"I'd suggest reducing the scope for this sprint. We can ship the basic filing first and add the edge cases in the next sprint."

**대안 2 (도움 요청):**
"I think we need additional support. If someone from the team could help with the test cases, I can focus on the core logic, and we might still make the deadline."
</details>

---
---

## Phase 6 총정리: 실전 영어 체크리스트

### 이 Phase에서 획득한 능력

```
[x] 세무 용어 70개를 영어로 읽고/쓸 수 있다 (Lesson 33)
[x] 세무 문서의 5대 문장 패턴을 인식하고 작성할 수 있다 (Lesson 34)
[x] API 문서, PR 리뷰, 커밋 메시지를 읽고 쓸 수 있다 (Lesson 35)
[x] 비즈니스 이메일, 미팅 발언, Slack 메시지를 작성할 수 있다 (Lesson 36)
```

### 문법 Phase와의 연결 맵

| Phase | 문법 | Phase 6에서의 적용 |
|-------|------|-------------------|
| Phase 1 | 문장 구조/5형식 | 기본 세무 문장 읽기 |
| Phase 2 | 시제 | 완료/결과 표현 (has been filed) |
| Phase 3 | 조동사 | 의무/규정 표현 (must, shall) |
| Phase 4 | 수동태 | 세무 문서의 수동 처리 표현 |
| Phase 5 | 가정법/조건 | 세무 규정의 조건/예외 표현 |
| **Phase 6** | **도메인 적용** | **세무 + 기술 + 비즈니스 실전** |

### 매일 실천할 수 있는 영어 습관

```
Morning (15분):
  - 영어 기술 블로그 1개 읽기 (Spring Blog, Baeldung, etc.)
  - 모르는 패턴 1개 노트하기

During Work (틈틈이):
  - 커밋 메시지 영어로 작성하기
  - PR 리뷰 코멘트 영어로 달아보기
  - 에러 메시지 한 번 더 읽고 패턴 인식하기

Evening (15분):
  - 오늘 쓴 코드의 Javadoc을 영어로 작성해보기
  - 세무 용어 5개 복습하기
```

---

> **Phase 6 완료!**
> 문법 OS 위에 도메인 애플리케이션이 설치되었다.
> 이제 세무 영어 문서를 읽고, 기술 문서를 해석하고, 영어로 소통할 수 있다.
>
> 다음은 지속적인 실전 적용이다. 매일 조금씩, 꾸준히.
> `while (career.isActive()) { english.practice(); }`
