# AEAD와 인증 암호화

암호화는 "읽을 수 없게" 만들 뿐 "바꿀 수 없게" 만들지는 않습니다. 기밀성과 무결성을 하나의 알고리즘으로 통합한 AEAD의 원리와, AES-GCM의 인증 태그·AAD를 실무에서 어떻게 활용하는지 정리합니다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### 1.1 기밀성과 무결성은 별개다

암호화를 배울 때 가장 흔한 오해가 "암호화했으니 안전하다"입니다. 실제로 암호화가 제공하는 것은 **기밀성(confidentiality)** 하나뿐입니다.

| 보안 속성 | 의미 | 암호화만으로 확보? |
|-----------|------|-------------------|
| 기밀성 (Confidentiality) | 제3자가 내용을 읽을 수 없다 | ✅ |
| 무결성 (Integrity) | 내용이 변조되지 않았다 | ❌ |
| 인증 (Authentication) | 정당한 발신자가 만든 것이다 | ❌ |

공격자가 암호문을 읽지 못해도, **암호문을 바꿔서 평문을 원하는 방향으로 조작하는 것**은 가능합니다. 이게 다음에 볼 비트 플리핑 공격입니다.

### 1.2 비트 플리핑 공격 (Bit-flipping)

CTR 모드는 `C = P XOR KS`(KS는 키스트림)로 동작합니다. XOR의 성질 때문에 다음이 성립합니다.

```
원래:   C[i] = P[i] XOR KS[i]
조작:   C'[i] = C[i] XOR Δ
복호화: P'[i] = C'[i] XOR KS[i]
             = (P[i] XOR KS[i] XOR Δ) XOR KS[i]
             = P[i] XOR Δ          ← 키를 몰라도 평문이 정확히 Δ만큼 바뀐다
```

구체적인 시나리오를 봅시다. 쿠키에 CTR로 암호화된 `role=user;id=1001` 이 들어 있고, 공격자는 평문 구조를 안다고 가정합니다(오픈소스 기반이면 흔한 일입니다).

```
평문 위치:  r  o  l  e  =  u  s  e  r  ;  i  d  =  1  0  0  1
인덱스:     0  1  2  3  4  5  6  7  8  9 ...

목표: 'user' → 'root' (같은 4바이트라 길이 유지)

Δ 계산:  'u' XOR 'r' = 0x75 XOR 0x72 = 0x07
         's' XOR 'o' = 0x73 XOR 0x6F = 0x1C
         'e' XOR 'o' = 0x65 XOR 0x6F = 0x0A
         'r' XOR 't' = 0x72 XOR 0x74 = 0x06

암호문 5~8번 바이트에 각각 XOR → 서버는 role=root 로 복호화한다
```

CBC 모드도 다릅니다만 마찬가지로 취약합니다. CBC는 `P[n] = D(C[n]) XOR C[n-1]` 이므로, **이전 블록 `C[n-1]`을 조작하면 `P[n]`을 정확히 원하는 값으로 바꿀 수 있습니다.** 대가로 `P[n-1]`이 쓰레기값이 되지만, JSON 파서가 관대하거나 앞 블록이 무시되는 필드라면 그대로 통과합니다.

**핵심: 암호화만 해서는 "변조 여부"를 알 수 없습니다.**

### 1.3 MAC과 HMAC

**MAC(Message Authentication Code)** 은 "비밀 키를 아는 사람만 만들 수 있는 체크섬"입니다.

```
송신: tag = MAC(K, message)  → message || tag 전송
수신: MAC(K, message) == tag ?  → 같으면 변조 없음
```

일반 해시(SHA-256)와의 차이는 **키의 존재**입니다. 단순 해시는 누구나 계산할 수 있으므로, 공격자가 메시지를 바꾸고 해시도 다시 계산하면 그만입니다. MAC은 키가 있어야 하므로 위조할 수 없습니다.

**HMAC**은 해시 함수를 MAC으로 안전하게 변환하는 표준 구조입니다.

```
HMAC(K, m) = H( (K ⊕ opad) || H( (K ⊕ ipad) || m ) )

ipad = 0x36 반복, opad = 0x5C 반복
```

이중 해시 구조인 이유는 **길이 확장 공격(length extension attack)** 방어입니다. `H(K || m)` 같은 순진한 구성은 Merkle–Damgård 계열 해시(MD5, SHA-1, SHA-256)에서 공격자가 `m`에 데이터를 이어붙이고 유효한 태그를 만들어낼 수 있습니다. Flickr API가 2009년 이 방식으로 뚫린 적이 있습니다.

### 1.4 조합 순서 — Encrypt-then-MAC만 안전하다

암호화와 MAC을 결합하는 세 가지 방식입니다.

```
① Encrypt-and-MAC   :  C = E(K1, P),  T = MAC(K2, P)   →  C || T
   문제: MAC은 결정적이므로 같은 평문이면 같은 태그.
         평문이 같은지 다른지가 새어나간다(기밀성 손상).
         전송: SSH

② MAC-then-Encrypt  :  T = MAC(K2, P),  C = E(K1, P || T)
   문제: 태그를 보려면 먼저 복호화해야 함 → 패딩 검증이 MAC보다 먼저 일어남
         → 패딩 오라클 공격 표면 노출. Lucky Thirteen이 이 지점을 뚫었다.
         전송: TLS 1.0~1.2의 CBC 조합

③ Encrypt-then-MAC  :  C = E(K1, P),  T = MAC(K2, IV || C)  →  C || T   ✅
   장점: 태그 검증 실패 시 복호화 자체를 하지 않음 → 오라클 원천 차단
         IND-CCA2 안전성이 증명된 유일한 조합
         전송: IPsec, TLS 1.3(AEAD로 통합)
```

Bellare–Namprempre(2000)의 논문이 ①②③의 안전성을 형식적으로 분석했고, 결론은 **Encrypt-then-MAC만이 일반적으로 안전하다**는 것이었습니다.

### 1.5 AEAD

**AEAD(Authenticated Encryption with Associated Data)** 는 위 조합을 개발자가 직접 하지 않도록 **알고리즘 자체에 통합**한 것입니다.

```
AEAD 암호화:  (ciphertext, tag) = Encrypt(key, nonce, plaintext, aad)
AEAD 복호화:  plaintext = Decrypt(key, nonce, ciphertext, tag, aad)
              └─ 태그 검증 실패 시 평문을 절대 반환하지 않고 예외 발생
```

제공하는 것:
- **기밀성** — plaintext는 암호화됨
- **무결성** — ciphertext 변조 시 태그 검증 실패
- **AAD 무결성** — 암호화하지 않지만 변조는 감지되는 부가 데이터

대표 알고리즘: **AES-GCM**, **ChaCha20-Poly1305**, AES-CCM, AES-SIV, XChaCha20-Poly1305.

---

## 2. 왜 알아야 하는가 (Why)

### 2.1 직접 조립하면 반드시 틀린다

Encrypt-then-MAC을 손으로 구현할 때 흔히 나오는 실수들입니다.

| 실수 | 결과 |
|------|------|
| 암호화 키와 MAC 키를 같은 값으로 사용 | 알고리즘 간 상호작용으로 취약점 발생 가능 |
| MAC 범위에 IV를 포함하지 않음 | IV 변조로 첫 블록 평문 조작 가능 |
| `Arrays.equals()`로 태그 비교 | 타이밍 공격으로 태그 위조 가능 |
| MAC 검증 전에 복호화 수행 | 패딩 오라클 부활 |
| 길이 필드를 MAC에 미포함 | 길이 혼동(length confusion) 공격 |

이 중 하나만 틀려도 전체가 무너집니다. AEAD는 이 모든 것을 알고리즘 스펙 안에서 처리합니다. **"직접 만들지 말고 검증된 것을 쓴다"** 는 암호학의 제1원칙이 여기에 그대로 적용됩니다.

### 2.2 실제 사고 사례

**Zoom E2E 암호화 논란 (2020)**
Zoom은 "종단간 암호화"를 표방했지만 실제로는 AES-128 **ECB** 모드를 사용했습니다. ECB는 기밀성조차 제대로 제공하지 못하고(같은 평문 블록 → 같은 암호문 블록), 당연히 무결성도 없었습니다. 이후 AES-256-GCM으로 전환했습니다.

**Steam 티켓 위조 (2016 이전)**
Valve의 인증 티켓 처리에서 CBC 암호문의 무결성 검증이 미흡해, 비트 플리핑으로 티켓 내용을 조작할 수 있었던 사례가 보고되었습니다.

**Nonce 재사용에 의한 GCM 붕괴**
AEAD를 쓴다고 안심할 수는 없습니다. GCM은 **같은 키로 같은 nonce를 재사용하면 인증 키(H) 자체가 복원**되어 공격자가 임의의 메시지에 유효한 태그를 붙일 수 있게 됩니다. 2016년 다수의 TLS 서버가 nonce를 랜덤이 아닌 예측 가능한 값으로 생성하다 취약점이 발견되었습니다. 자세한 내용은 [IV와 Nonce](./04-iv-and-nonce.md)를 참고하세요.

### 2.3 실무 판단 기준

새로 코드를 짠다면 선택지는 단순합니다.

```
개인정보 필드 암호화, API 페이로드, 토큰, 세션 데이터
  → AES-256-GCM (하드웨어 가속 있는 서버 환경)
  → ChaCha20-Poly1305 (모바일/IoT, AES-NI 없는 환경)

CBC + HMAC 은 레거시 호환이 필요할 때만
ECB, CBC 단독 은 어떤 경우에도 금지
```

면접에서 "AES-CBC와 AES-GCM의 차이"를 물으면 "GCM은 인증 태그가 있다" 수준을 넘어 **"CBC는 무결성이 없어 비트 플리핑에 취약하고, GCM은 Encrypt-then-MAC이 알고리즘에 통합되어 있으며 병렬 처리도 가능하다"** 까지 말할 수 있어야 합니다.

---

## 3. 내부 구현 분석 (How)

### 3.1 AES-GCM의 구조

GCM = **CTR 모드(기밀성) + GHASH(무결성)** 입니다.

```mermaid
flowchart TB
    subgraph CTR["기밀성: CTR 모드"]
        N[Nonce 96bit + Counter 32bit] --> E1[AES-K]
        E1 --> X1((XOR))
        P1[평문 블록 1] --> X1
        X1 --> C1[암호문 블록 1]
    end

    subgraph GHASH["무결성: GHASH (GF 2^128 곱셈)"]
        AAD[AAD 블록] --> G1((⊗H))
        C1 --> G2((⊗H))
        G1 --> G2
        LEN[len AAD || len C] --> G3((⊗H))
        G2 --> G3
        G3 --> T0[GHASH 결과]
    end

    T0 --> XT((XOR))
    EJ0[AES-K counter=0] --> XT
    XT --> TAG[인증 태그 128bit]
```

동작 순서를 정리하면 이렇습니다.

```
1. H = AES_K(0^128)                 ← GHASH용 해시 서브키
2. J0 = nonce || 0x00000001         ← 96비트 nonce일 때의 초기 카운터
3. 암호화: C = P XOR AES_K(J0+1), AES_K(J0+2), ...   (CTR)
4. GHASH: AAD와 C를 GF(2^128)에서 H와 곱셈 누적
5. Tag = GHASH_result XOR AES_K(J0)
```

주목할 점:
- **CTR 기반이므로 패딩이 없다** → 패딩 오라클 불가능
- **암호문에 대해 MAC을 계산** → 구조적으로 Encrypt-then-MAC
- **GF(2^128) 곱셈은 병렬화 가능** → CBC보다 훨씬 빠름 (AES-NI + PCLMULQDQ 명령어 활용 시)

### 3.2 인증 태그 (Authentication Tag)

태그는 기본 128비트(16바이트)입니다. GCM 스펙상 96/104/112/120/128비트를 선택할 수 있지만, **실무에서는 128비트 외의 선택지를 고려하지 마세요.**

| 태그 길이 | 위조 성공 확률(1회 시도) | 권장 여부 |
|-----------|------------------------|----------|
| 128비트 | 2⁻¹²⁸ | ✅ 기본값 |
| 112비트 | 2⁻¹¹² | 제약 환경만 |
| 96비트 | 2⁻⁹⁶ | 비권장 |
| 64비트 이하 | 2⁻⁶⁴ | ❌ NIST SP 800-38D가 명시적으로 제한 |

짧은 태그는 저장 공간을 몇 바이트 아끼는 대신 위조 공격 성공률을 기하급수적으로 높입니다. NIST SP 800-38D는 64/32비트 태그 사용에 별도의 사용량 제한 조건을 걸고 있습니다.

Java에서는 `GCMParameterSpec(128, iv)`의 첫 인자가 **비트 단위** 태그 길이입니다. `GCMParameterSpec(16, iv)`라고 쓰면 16바이트가 아니라 16**비트** 태그가 되어 보안이 완전히 무너집니다. 실무에서 실제로 나오는 실수입니다.

### 3.3 AAD (Associated Data)

AAD는 **암호화하지는 않지만 무결성은 보장하고 싶은 데이터**입니다.

```
Encrypt(key, nonce, plaintext, aad)
                    └암호화됨┘  └평문 그대로, 태그에만 반영┘
```

왜 필요할까요? 대표적인 시나리오는 **암호문 교체 공격(ciphertext substitution)** 방어입니다.

```
DB에 이렇게 저장되어 있다고 하자.

  users 테이블
  ┌────┬───────────┬──────────────────────────┐
  │ id │ tenant_id │ encrypted_ssn            │
  ├────┼───────────┼──────────────────────────┤
  │ 1  │ 100       │ [IV][CT][TAG]  ← A사 직원 │
  │ 2  │ 200       │ [IV][CT][TAG]  ← B사 직원 │
  └────┴───────────┴──────────────────────────┘

공격: DB 쓰기 권한을 얻은 내부자가 id=1의 encrypted_ssn을
      id=2의 값으로 통째로 복사한다.

  → 암호문 자체는 정상이므로 GCM 태그 검증도 통과한다!
  → A사 조회 화면에 B사 직원의 주민번호가 나온다.
```

AAD로 컨텍스트를 묶으면 이 공격이 막힙니다.

```
암호화 시:  aad = "users:1:ssn:tenant=100"
복호화 시:  aad = "users:2:ssn:tenant=200"  ← 다름 → 태그 검증 실패 → 예외
```

AAD에 넣기 좋은 것들:
- 테넌트 ID / 조직 ID (멀티테넌시 격리)
- 레코드 PK, 컬럼명 (행·열 간 교체 방지)
- 키 버전, 스키마 버전 (키 로테이션 추적)
- 목적 태그 (`purpose=ssn` vs `purpose=phone`)

AAD에 넣으면 **안 되는** 것: 자주 바뀌는 값(수정 시각 등). AAD가 바뀌면 기존 암호문을 복호화할 수 없게 되므로, **불변 식별자만** 사용해야 합니다.

### 3.4 ChaCha20-Poly1305

AES-GCM의 대안입니다.

| 항목 | AES-256-GCM | ChaCha20-Poly1305 |
|------|-------------|-------------------|
| 구조 | 블록 암호 + GHASH | 스트림 암호(ARX) + Poly1305 |
| 키 길이 | 128/192/256비트 | 256비트 고정 |
| Nonce | 96비트 권장 | 96비트 (XChaCha20은 192비트) |
| 태그 | 128비트 | 128비트 |
| HW 가속 있을 때 | 매우 빠름 (AES-NI) | 비슷하거나 약간 느림 |
| HW 가속 없을 때 | 느림 | **3배 이상 빠름** |
| 타이밍 안전성 | 소프트웨어 구현 시 캐시 타이밍 위험 | ARX 연산만 사용 → 상수 시간 자연 확보 |
| Nonce 재사용 | 치명적 (인증 키 노출) | 치명적이지만 GCM보다는 덜 파괴적 |
| Java 지원 | 기본 (`AES/GCM/NoPadding`) | JDK 11+ (`ChaCha20-Poly1305`) |

**선택 기준:** x86 서버(AES-NI 있음) → AES-GCM. 모바일 앱, IoT, ARM 저사양 → ChaCha20-Poly1305. Google이 모바일 Chrome에서 ChaCha20-Poly1305를 우선하는 이유가 정확히 이것입니다.

XChaCha20-Poly1305는 nonce가 192비트라 **랜덤 생성해도 충돌 걱정이 없다**는 큰 장점이 있습니다. 96비트 nonce는 랜덤 생성 시 생일 역설로 약 2³² 메시지 근처에서 충돌 위험이 생기지만, 192비트는 사실상 무한합니다. Java 표준에는 없고 Tink/libsodium 계열 라이브러리에서 제공합니다.

---

## 4. 실전 예제

### 4.1 AAD를 활용한 필드 암호화

```kotlin
@Component
class FieldEncryptor(
    private val keyProvider: KeyProvider   // 키 버전별 관리
) {
    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val NONCE_SIZE = 12    // 96비트 — GCM 권장값
        private const val TAG_BITS = 128     // 비트 단위임에 주의
    }

    /**
     * @param context 암호문을 특정 레코드/컬럼에 묶는 불변 식별자
     */
    fun encrypt(plain: String, context: FieldContext): String {
        val keyVersion = keyProvider.currentVersion()
        val key = keyProvider.get(keyVersion)

        val nonce = ByteArray(NONCE_SIZE).also { SecureRandom().nextBytes(it) }

        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
            updateAAD(context.toAad(keyVersion))   // ← doFinal 이전에 호출해야 한다
        }
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))

        // v1:base64(nonce):base64(ct||tag)
        return "v$keyVersion:${b64(nonce)}:${b64(ct)}"
    }

    fun decrypt(stored: String, context: FieldContext): String {
        val (versionPart, noncePart, ctPart) = stored.split(":")
        val keyVersion = versionPart.removePrefix("v").toInt()
        val key = keyProvider.get(keyVersion)

        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, unb64(noncePart)))
            updateAAD(context.toAad(keyVersion))
        }

        return try {
            String(cipher.doFinal(unb64(ctPart)), Charsets.UTF_8)
        } catch (e: AEADBadTagException) {
            // 이 예외는 "복호화 실패"가 아니라 "변조 감지"다
            throw TamperDetectedException("field integrity violation: $context", e)
        }
    }
}

data class FieldContext(
    val tenantId: Long,
    val table: String,
    val column: String,
    val recordId: Long
) {
    fun toAad(keyVersion: Int): ByteArray =
        "v$keyVersion|t$tenantId|$table.$column|#$recordId".toByteArray(Charsets.UTF_8)
}
```

**주의사항 3가지**

1. `updateAAD()`는 반드시 `init()` 이후, `update()/doFinal()` **이전**에 호출해야 합니다. 순서가 어긋나면 `IllegalStateException`이 발생합니다.
2. `Cipher` 인스턴스는 **스레드 안전하지 않습니다.** 필드로 두고 공유하면 동시성 버그가 발생합니다. 매번 `getInstance()`로 새로 만들거나 `ThreadLocal`을 쓰세요.
3. 같은 키로 **같은 nonce를 두 번 쓰면 GCM은 완전히 무너집니다.** `SecureRandom` 96비트도 대량 트래픽에서는 카운터 기반이 더 안전합니다.

### 4.2 안티패턴 모음

```java
// ❌ 안티패턴 1: 태그 길이를 바이트로 착각
new GCMParameterSpec(16, iv);   // 16비트 태그! 위조 성공률 1/65536
new GCMParameterSpec(128, iv);  // ✅ 올바름

// ❌ 안티패턴 2: nonce 고정
private static final byte[] IV = new byte[12];  // 전부 0 — 재사용 = 파멸
private static final byte[] IV = "1234567890ab".getBytes();  // 같은 문제

// ❌ 안티패턴 3: Cipher 인스턴스 공유
@Component
public class BadEncryptor {
    private final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");  // 위험
}

// ❌ 안티패턴 4: 태그 검증 실패를 삼키기
try {
    return decrypt(data);
} catch (AEADBadTagException e) {
    return null;   // 변조 사실이 조용히 묻힌다
}

// ❌ 안티패턴 5: 무결성 없는 모드로 돌아가기
Cipher.getInstance("AES/CTR/NoPadding");  // 비트 플리핑에 무방비

// ❌ 안티패턴 6: 스트리밍 복호화에서 부분 평문 사용
CipherInputStream cis = new CipherInputStream(in, gcmCipher);
process(cis.read(...));  // 태그 검증 전의 평문을 이미 사용해버림
```

마지막 항목은 특히 놓치기 쉽습니다. GCM에서 태그 검증은 **모든 데이터를 처리한 마지막에** 일어납니다. 스트리밍으로 읽으면서 중간 결과를 바로 쓰면, 검증 실패가 나기 전에 이미 변조된 평문으로 작업한 셈이 됩니다. 대용량 파일이라면 **청크 단위로 각각 독립 AEAD를 적용**(청크 인덱스를 AAD에 포함)하는 프레이밍 설계가 필요합니다.

### 4.3 태그 검증 실패의 운영 처리

```java
@Service
public class SecureDataService {

    private static final Logger log = LoggerFactory.getLogger(SecureDataService.class);

    private final MeterRegistry meterRegistry;
    private final SecurityEventPublisher eventPublisher;

    public String read(Long recordId, Long tenantId) {
        String stored = repository.findEncrypted(recordId);
        FieldContext ctx = new FieldContext(tenantId, "users", "ssn", recordId);

        try {
            return encryptor.decrypt(stored, ctx);

        } catch (TamperDetectedException e) {
            // 1. 메트릭 — 급증 시 알림
            meterRegistry.counter("crypto.tamper.detected",
                    "table", "users", "tenant", String.valueOf(tenantId)).increment();

            // 2. 보안 이벤트 발행 — SIEM 연동
            eventPublisher.publish(SecurityEvent.tamperDetected(recordId, tenantId));

            // 3. 상세 로그는 내부에만
            log.error("AEAD tag verification failed. record={} tenant={}", recordId, tenantId, e);

            // 4. 외부 응답은 일반화된 메시지로
            throw new DataAccessException("데이터를 조회할 수 없습니다");
        }
    }
}
```

태그 검증 실패는 **거의 항상 비정상 상황**입니다. 정상 동작 중에는 발생할 수 없기 때문에, 발생하면 셋 중 하나입니다. (1) 실제 변조 시도, (2) 키 로테이션 버그, (3) AAD 컨텍스트 불일치 버그. 어느 쪽이든 조용히 넘어가면 안 됩니다.

### 4.4 ChaCha20-Poly1305 (JDK 11+)

```java
public class ChaChaEncryptor {

    private static final String TRANSFORMATION = "ChaCha20-Poly1305";
    private static final int NONCE_SIZE = 12;

    public byte[] encrypt(SecretKey key, byte[] plain, byte[] aad)
            throws GeneralSecurityException {
        byte[] nonce = new byte[NONCE_SIZE];
        SecureRandom.getInstanceStrong().nextBytes(nonce);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        // GCMParameterSpec 이 아니라 IvParameterSpec 을 쓴다
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(nonce));
        if (aad != null) {
            cipher.updateAAD(aad);
        }
        byte[] ct = cipher.doFinal(plain);   // 태그가 뒤에 붙어서 반환됨

        return ByteBuffer.allocate(nonce.length + ct.length)
                .put(nonce).put(ct).array();
    }
}
```

API 형태는 GCM과 거의 같습니다. 차이는 `GCMParameterSpec` 대신 `IvParameterSpec`을 쓴다는 것(태그 길이가 128비트 고정이라 지정할 필요가 없음)과, 키가 256비트 고정이라는 점입니다.

### 4.5 Spring Boot에서 AttributeConverter로 적용

```java
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    // JPA Converter는 Spring 빈 주입이 까다로워 정적 홀더 패턴을 자주 쓴다
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        return CryptoHolder.encryptor().encryptGlobal(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return CryptoHolder.encryptor().decryptGlobal(dbData);
    }
}
```

다만 `AttributeConverter`는 **엔티티의 다른 필드(테넌트 ID, PK)에 접근할 수 없어 AAD를 제대로 구성하기 어렵습니다.** PK는 INSERT 시점에 아직 채번되지 않았을 수도 있습니다. 레코드 단위 AAD가 필요하다면 `@PrePersist`/`@PostLoad` 콜백이나 서비스 레이어에서 명시적으로 처리하는 편이 낫습니다. 자세한 패턴은 [데이터베이스 필드 암호화](../advanced/02-database-field-encryption.md)를 참고하세요.

---

## 5. 정리

| 항목 | 내용 |
|------|------|
| 암호화가 주는 것 | 기밀성뿐. 무결성·인증은 별도 |
| 비트 플리핑 | CTR/CBC에서 암호문 XOR 조작으로 평문을 원하는 값으로 변경 |
| MAC vs 해시 | MAC은 키가 필요 → 위조 불가. 해시는 누구나 재계산 가능 |
| HMAC 이중 구조 이유 | 길이 확장 공격 방어 |
| 안전한 조합 | **Encrypt-then-MAC** 만. MAC-then-Encrypt는 패딩 오라클 노출 |
| AEAD | 기밀성 + 무결성 + AAD 무결성을 알고리즘에 통합 |
| GCM 구성 | CTR(기밀성) + GHASH(무결성), 패딩 없음, 병렬 처리 가능 |
| 태그 길이 | 128비트 권장. `GCMParameterSpec(128, iv)` — **비트 단위** |
| AAD 용도 | 테넌트/레코드/컬럼/키버전을 묶어 암호문 교체 공격 차단 |
| AAD 금지 항목 | 가변 값(수정시각 등) — 복호화 불가 상태를 만든다 |
| GCM 최대 금기 | **nonce 재사용** → 인증 키 노출로 전체 붕괴 |
| ChaCha20-Poly1305 | AES-NI 없는 환경에서 3배 이상 빠르고 타이밍 안전 |
| Java 주의 | `Cipher`는 스레드 비안전, `updateAAD`는 `doFinal` 이전, `AEADBadTagException`은 변조 신호 |

> **핵심 포인트**: "암호화했으니 안전하다"는 명제는 절반만 참입니다. CTR이나 CBC로 암호화한 데이터는 읽을 수는 없어도 **원하는 방향으로 조작할 수는 있습니다** — 비트 플리핑 한 번이면 `role=user`가 `role=root`가 됩니다. 이 구멍을 막으려면 MAC이 필요하고, MAC을 붙이는 순서는 **Encrypt-then-MAC** 하나뿐이며, 이걸 직접 조립하면 키 분리·IV 포함·상수 시간 비교 중 어딘가에서 반드시 실수가 납니다. 그래서 정답은 처음부터 **AEAD(AES-256-GCM 또는 ChaCha20-Poly1305)** 를 쓰는 것입니다. 실무에서 한 걸음 더 나아가려면 **AAD를 적극 활용**하세요. 테넌트 ID와 레코드 PK를 AAD로 묶어두면 DB 접근 권한을 가진 내부자가 암호문을 행 간에 복사해도 태그 검증에서 걸립니다. 마지막으로, GCM의 유일한 치명적 금기는 **nonce 재사용**입니다. 태그 길이는 항상 128비트, nonce는 절대 재사용 금지 — 이 두 가지만 지키면 나머지는 알고리즘이 알아서 해줍니다.

---

## 관련 문서

- [백엔드 개발자를 위한 보안 기초](../01-backend-security-fundamentals.md)
- [JWT/JWK/OAuth 비교](../02-jwt-jwk-oauth-comparison.md)
- [암호화 기초](./01-encryption-fundamentals.md)
- [AES 알고리즘 구조](./02-aes-algorithm-structure.md)
- [블록 암호 운용 모드](./03-block-cipher-modes.md)
- [IV와 Nonce](./04-iv-and-nonce.md)
- [패딩과 패딩 오라클 공격](./05-padding-and-oracle-attack.md)
- [해시 함수와 비밀번호 저장](./07-hashing-and-password-storage.md)
- [비대칭키 암호와 전자서명](./08-asymmetric-crypto-and-signature.md)
- [키 관리와 봉투 암호화](../advanced/01-key-management-envelope-encryption.md)
- [데이터베이스 필드 암호화](../advanced/02-database-field-encryption.md)
- [Spring Boot 암호화 실무](../advanced/03-spring-boot-encryption-practice.md)
- [TLS와 전송 구간 암호화](../advanced/04-tls-and-transport-security.md)

---
*참고: Java 17 / Spring Boot 3.x 기준*
