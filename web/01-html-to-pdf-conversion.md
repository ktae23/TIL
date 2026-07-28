# 브라우저에서 HTML을 PDF로 변환하기

## 개요

웹 페이지의 콘텐츠를 클라이언트 사이드에서 PDF로 변환하는 방법과, 실제 구현 시 마주치는 함정들을 정리한다.

## 주요 라이브러리

| 라이브러리 | 방식 | 특징 |
|-----------|------|------|
| **html2pdf.js** | html2canvas + jsPDF | 가장 대중적, 래스터 이미지 기반 |
| **html-to-image** | SVG foreignObject | html2canvas보다 CSS 지원 우수, 더 빠름 |
| **jsPDF** | 직접 PDF 구성 | 텍스트/도형 직접 그리기, HTML 렌더링 약함 |
| **pdfmake** | JSON → PDF | 구조화된 문서에 적합, CJK 폰트 임베딩 복잡 |
| **window.print()** | 브라우저 네이티브 | 벡터 PDF, 텍스트 선택 가능, 프린트 다이얼로그 필요 |

## html2pdf.js 아키텍처

```
html2pdf().from(element).save()

내부 흐름:
1. html2canvas가 element.ownerDocument를 숨겨진 iframe으로 클론
2. 클론된 문서에서 element의 bounding rect를 캔버스에 래스터화
3. 캔버스를 JPEG/PNG 이미지로 변환
4. jsPDF가 이미지를 A4 페이지에 배치하여 PDF 생성
```

**핵심**: html2canvas는 대상 요소만 캡처하는 것이 **아니라**, 전체 문서를 클론한 뒤 대상의 영역만 잘라낸다.

## 실전에서 마주치는 함정들

### 1. overflow 클리핑으로 인한 백지 PDF

**증상**: PDF 파일은 생성되지만 모든 페이지가 백지 (구조는 있으나 시각적 콘텐츠 없음)

**원인**: 복잡한 레이아웃의 조상 요소가 `overflow: hidden`이나 `height: calc(100vh - ...)`를 갖고 있으면, 클론된 문서에서 다른 viewport 크기로 계산되어 콘텐츠가 클리핑 영역 밖으로 밀려남

```
body
  .header (position: fixed)
  .container (height: calc(100vh - 60px))     ← 클론 시 다른 viewport
    .content-wrapper (overflow: hidden)        ← 클리핑 경계
      .content-area (overflow-y: auto)         ← 스크롤 컨테이너
        #content                               ← 캡처 대상
```

**해결**: `onclone` 콜백에서 클론된 문서의 제약 해제

```javascript
html2pdf().set({
    html2canvas: {
        onclone: function(clonedDoc) {
            // 조상 요소의 클리핑 해제
            clonedDoc.querySelectorAll('.container, .content-wrapper, .content-area')
                .forEach(function(el) {
                    el.style.setProperty('overflow', 'visible', 'important');
                    el.style.setProperty('height', 'auto', 'important');
                    el.style.setProperty('display', 'block', 'important');
                });
            // 불필요한 UI 요소 숨김
            clonedDoc.querySelectorAll('.header, .sidebar')
                .forEach(function(el) {
                    el.style.setProperty('display', 'none', 'important');
                });
        }
    }
}).from(target).save();
```

**`onclone`이 핵심인 이유**: 클론된 문서를 수정하므로 원본 페이지에 영향 없음. 클론 후 렌더링 전에 호출되어 타이밍이 완벽함.

### 2. 캔버스 최대 높이 제한

**증상**: 짧은 문서는 정상, 긴 문서(20페이지+)만 백지

**원인**: 브라우저별 캔버스 최대 크기 제한

| 브라우저 | 최대 캔버스 높이 |
|---------|----------------|
| Chrome | ~16,384px |
| Safari | ~16,384px |
| Firefox | ~32,767px |

scale: 2 적용 시 실효 제한은 절반. 33페이지 문서 × scale 2 = ~74,000px → 제한 초과

**해결**: 콘텐츠 높이에 따른 동적 scale 조정

```javascript
var contentHeight = target.scrollHeight;
var scale = 2;
if (contentHeight * scale > 14000) {
    scale = Math.max(1, Math.floor(14000 / contentHeight * 10) / 10);
}
```

### 3. file:// 프로토콜에서 JS 캐시 문제

**증상**: 코드를 수정해도 이전 동작이 반복됨

**원인**: `file://` 프로토콜에서 브라우저가 외부 JS/CSS 파일을 적극적으로 캐시. 쿼리 파라미터(`?v=timestamp`) 캐시 버스팅이 무시될 수 있음.

**해결**: JS/CSS를 HTML에 인라인으로 삽입

```python
# generate_viewer.py
with open("js/viewer.js", "r") as f:
    viewer_js = f.read()

html = f"""
<script>{viewer_js}</script>   <!-- 외부 참조 대신 인라인 -->
"""
```

### 4. CSS 변수와 다크모드

**증상**: 다크모드에서 PDF 배경이 어둡거나 텍스트가 안 보임

**원인**: html2canvas가 클론 시 CSS 변수(`var(--color)`)와 `data-theme` 속성도 복사

**해결**: `onclone`에서 라이트 테마 강제 + 원본 페이지 임시 전환

```javascript
// 원본 페이지 임시 전환 (computedStyle이 라이트 기준으로 계산되도록)
const wasDark = document.documentElement.getAttribute('data-theme') === 'dark';
if (wasDark) document.documentElement.setAttribute('data-theme', 'light');

// onclone에서도 클론 문서에 라이트 강제
onclone: function(clonedDoc) {
    clonedDoc.documentElement.setAttribute('data-theme', 'light');
}

// PDF 생성 완료 후 복원
.then(function() {
    if (wasDark) document.documentElement.setAttribute('data-theme', 'dark');
});
```

### 5. 프린트 다이얼로그 파일명

**증상**: `iframe.contentWindow.print()` 호출 시 저장 파일명이 부모 페이지 제목("TIL Viewer")으로 설정됨

**원인**: Chrome은 iframe 프린트 시 iframe의 `document.title`이 아닌 **부모 페이지의 `document.title`**을 사용

**해결**: 프린트 직전에 부모 타이틀을 임시 변경, 프린트 후 복원

```javascript
var originalTitle = document.title;
document.title = file.title;       // 파일명으로 사용될 제목
iframe.contentWindow.print();
document.title = originalTitle;     // 원래 제목 복원
```

## 최종 결론: window.print()가 정답

html2pdf.js의 html2canvas 기반 래스터 방식은 실전에서 여러 한계에 부딪힌다:

| 문제 | html2pdf.js | window.print() |
|------|-------------|----------------|
| overflow 클리핑 백지 | `onclone`으로 우회 가능 | 해당 없음 |
| 캔버스 높이 제한 (긴 문서 백지) | **근본적 한계** | 제한 없음 |
| 표 페이지 나눔 잘림 | 부분적 해결 | CSS `page-break` 완벽 지원 |
| 해상도 | scale 의존 (래스터) | **벡터 PDF** (무한 확대) |
| 텍스트 선택 | 불가 (이미지) | **가능** |

특히 태블릿에서 S펜으로 필기하며 학습하는 용도라면 window.print()의 벡터 PDF가 압도적으로 유리하다.

### window.print() 구현 패턴

```javascript
function downloadPDF() {
    // 1. 격리된 iframe 생성 (인라인 CSS + 페이지 나눔 규칙 포함)
    var iframe = document.createElement('iframe');
    iframe.style.cssText = 'position:fixed;left:-10000px;';
    document.body.appendChild(iframe);

    // 2. 깨끗한 문서에 콘텐츠 렌더링
    var doc = iframe.contentDocument;
    doc.open();
    doc.write(PRINT_TEMPLATE);  // @page, page-break CSS 포함
    doc.close();

    iframe.onload = function() {
        doc.getElementById('content').innerHTML = renderedMarkdown;

        // 3. 부모 타이틀 임시 변경 → 파일명 설정
        var originalTitle = document.title;
        document.title = '원하는 파일명';
        iframe.contentWindow.print();
        document.title = originalTitle;

        // 4. 정리
        document.body.removeChild(iframe);
    };
}
```

### 페이지 나눔 CSS (print용)

```css
@page { margin: 15mm; }
tr { page-break-inside: avoid; }         /* 표 행 내부 잘림 방지 */
pre, blockquote { page-break-inside: avoid; }  /* 코드블록/인용문 보호 */
h1, h2, h3, h4 { page-break-after: avoid; }   /* 제목 고아 방지 */
table { page-break-inside: auto; }       /* 표 자체는 나눔 허용 */
```

## html2canvas 대안적 접근법 (참고)

### iframe 격리 방식 (시도 후 실패)

`target.ownerDocument`를 깨끗한 iframe 문서로 분리하는 아이디어:

```javascript
const iframe = document.createElement('iframe');
iframe.srcdoc = cleanHTML;
// html2pdf().from(iframeElement) → iframe 문서만 클론
```

**실패 원인**: `file://` 프로토콜과 `srcdoc`(`about:srcdoc` origin) 사이의 cross-origin 제약으로 html2canvas가 문서 접근 실패. HTTP 환경에서는 작동할 수 있으나 범용성 부족.

### SVG foreignObject 방식 (html-to-image)

```
DOM → SVG foreignObject → Canvas → Image
```

- 브라우저 네이티브 렌더링 엔진 활용
- flex, shadow, 웹폰트 지원이 html2canvas보다 우수
- CORS 제약에 취약, 외부 리소스 로딩 보장 없음

## 실전 체크리스트

- [ ] **래스터 vs 벡터**: 텍스트 선택/확대가 필요하면 window.print(), 원클릭이 필요하면 html2pdf
- [ ] 긴 문서(20p+)는 html2pdf 캔버스 제한으로 사실상 window.print() 필수
- [ ] `overflow: hidden` 조상이 있으면 html2canvas `onclone`으로 해제
- [ ] 프린트 파일명은 `document.title`을 임시 변경하여 제어
- [ ] 다크모드 지원 시 인라인 CSS에 라이트 테마 하드코딩
- [ ] `file://` 배포 시 JS/CSS 인라인 삽입 (캐시 방지)
- [ ] 표/코드블록/제목에 `page-break-inside: avoid` CSS 적용
