---
name: md-preview
description: 마크다운 파일을 HTML로 변환하여 브라우저에서 미리보기합니다.
allowed-tools: Bash, Read, Write, Glob, AskUserQuestion
---

# md-preview

마크다운 파일을 HTML로 변환하여 브라우저에서 미리보기를 엽니다.

## 실행 단계

/md-preview 실행 시 다음 단계를 순차적으로 진행합니다.

### Step 0. 작업 선택

**AskUserQuestion 도구**를 사용하여 수행할 작업을 선택받습니다:
- header: "작업"
- question: "수행할 작업을 선택해주세요."
- options:
  - "마크다운 미리보기": 마크다운 파일을 HTML로 변환하여 브라우저에서 열기
  - "임시 파일 정리 (tmp-clear)": /tmp 디렉터리의 모든 *-preview.html 파일 삭제

사용자가 "임시 파일 정리"를 선택하면:
1. `/tmp` 디렉터리에서 `*-preview.html` 패턴의 파일을 검색:
   ```bash
   ls /tmp/*-preview.html 2>/dev/null
   ```

2. 찾은 파일 목록을 사용자에게 보여주고 삭제 확인:
   ```
   다음 파일들을 삭제하시겠습니까?
   - /tmp/file1-preview.html
   - /tmp/file2-preview.html
   ```

3. 확인 후 파일 삭제:
   ```bash
   rm -f /tmp/*-preview.html
   ```

4. 완료 메시지 출력:
   ```
   ✓ {개수}개의 임시 HTML 파일을 삭제했습니다.
   ```

사용자가 "마크다운 미리보기"를 선택하면 Step 1로 진행합니다.

### Step 1. 디렉터리 선택

1. 현재 작업 디렉터리를 기준으로 마크다운 파일이 있는 하위 디렉터리를 검색합니다:
   ```bash
   find . -name "*.md" -type f | sed 's|/[^/]*$||' | sort -u | head -4
   ```

2. **AskUserQuestion 도구**를 사용하여 디렉터리 선택지를 제공합니다:
   - header: "디렉터리"
   - question: "마크다운 파일이 있는 디렉터리를 선택해주세요."
   - options: 검색된 디렉터리들 (최대 4개)
   - 현재 디렉터리(.)도 옵션에 포함
   - 사용자가 "Other"를 선택하면 디렉터리 경로를 직접 입력

### Step 2. 마크다운 파일 선택

1. 선택된 디렉터리에서 모든 .md 파일을 검색합니다 (하위 디렉터리 포함):
   ```bash
   find {선택된_디렉터리} -name "*.md" -type f | sort
   ```

2. **AskUserQuestion 도구**를 사용하여 파일 선택지를 제공합니다:
   - header: "파일"
   - question: "미리보기할 마크다운 파일을 선택해주세요."
   - options: 검색된 모든 .md 파일들 (최대 4개, 파일명만 표시)
   - 사용자가 "Other"를 선택하면 파일 경로를 직접 입력

### Step 3. HTML 변환 및 미리보기

1. 선택된 마크다운 파일을 읽습니다.

2. HTML 파일을 생성합니다:
   - 파일명: `/tmp/{원본파일명}-preview.html`
   - 스타일이 적용된 HTML 템플릿 사용
   - 마크다운 내용을 HTML로 변환하여 삽입

3. HTML 템플릿 구조:
   ```html
   <!DOCTYPE html>
   <html lang="ko">
   <head>
       <meta charset="UTF-8">
       <meta name="viewport" content="width=device-width, initial-scale=1.0">
       <title>{파일명}</title>
       <style>
           body {
               max-width: 800px;
               margin: 40px auto;
               padding: 20px;
               font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
               line-height: 1.6;
               color: #333;
           }
           h1, h2, h3, h4, h5, h6 {
               margin-top: 24px;
               margin-bottom: 16px;
               font-weight: 600;
               line-height: 1.25;
           }
           h1 { font-size: 2em; border-bottom: 1px solid #eaecef; padding-bottom: 0.3em; }
           h2 { font-size: 1.5em; border-bottom: 1px solid #eaecef; padding-bottom: 0.3em; }
           h3 { font-size: 1.25em; }
           code {
               background-color: #f6f8fa;
               padding: 2px 6px;
               border-radius: 3px;
               font-family: 'Courier New', monospace;
               font-size: 0.9em;
           }
           pre {
               background-color: #f6f8fa;
               padding: 16px;
               border-radius: 6px;
               overflow-x: auto;
           }
           pre code {
               background-color: transparent;
               padding: 0;
           }
           blockquote {
               border-left: 4px solid #dfe2e5;
               padding-left: 16px;
               color: #6a737d;
               margin: 0;
           }
           table {
               border-collapse: collapse;
               width: 100%;
               margin: 16px 0;
           }
           table th, table td {
               border: 1px solid #dfe2e5;
               padding: 8px 13px;
           }
           table th {
               background-color: #f6f8fa;
               font-weight: 600;
           }
           table tr:nth-child(2n) {
               background-color: #f6f8fa;
           }
           a {
               color: #0366d6;
               text-decoration: none;
           }
           a:hover {
               text-decoration: underline;
           }
           img {
               max-width: 100%;
               height: auto;
           }
           ul, ol {
               padding-left: 2em;
           }
           li {
               margin: 4px 0;
           }
       </style>
   </head>
   <body>
       <div id="content"></div>
       <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
       <script>
           const markdown = `{마크다운_내용_이스케이프}`;
           document.getElementById('content').innerHTML = marked.parse(markdown);
       </script>
   </body>
   </html>
   ```

4. 마크다운을 HTML로 변환:

   **클라이언트 사이드 변환 사용 (추천 - 의존성 없음)**

   marked.js CDN을 사용하여 브라우저에서 마크다운을 변환합니다.
   HTML 파일에 마크다운 내용을 포함시키고, JavaScript로 변환:

   ```html
   <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
   <script>
       const markdown = `{마크다운_내용_이스케이프}`;
       document.getElementById('content').innerHTML = marked.parse(markdown);
   </script>
   ```

   마크다운 내용을 JavaScript 문자열로 이스케이프할 때:
   - 백틱(`)을 `\``로 이스케이프
   - 백슬래시(\)를 `\\`로 이스케이프
   - ${를 `\${`로 이스케이프

5. 브라우저에서 HTML 파일 열기:
   ```bash
   open /tmp/{파일명}-preview.html
   ```

### Step 4. 완료 메시지

사용자에게 미리보기가 열렸음을 알립니다:
```
✓ {파일명}을 HTML로 변환했습니다.
✓ 브라우저에서 미리보기가 열렸습니다.
✓ 파일 위치: /tmp/{파일명}-preview.html
```

## 마크다운 변환 상세

### 이스케이프 처리

마크다운 내용을 JavaScript 문자열로 변환할 때 다음 문자들을 이스케이프해야 합니다:

```python
import json

# Read markdown file
with open(md_file, 'r', encoding='utf-8') as f:
    md_content = f.read()

# Escape for JavaScript template literal
# json.dumps로 이스케이프하면 안전하게 처리됨
escaped_content = json.dumps(md_content)[1:-1]  # 앞뒤 따옴표 제거
```

또는 간단하게:
```bash
# 마크다운 파일 읽기
MD_CONTENT=$(cat "$MD_FILE")

# Python으로 이스케이프 처리
ESCAPED=$(python3 -c "import json, sys; print(json.dumps(sys.stdin.read())[1:-1])" <<< "$MD_CONTENT")
```

## 사용 예시

### 마크다운 미리보기

```
/md-preview
→ Step 0: [작업 선택] 마크다운 미리보기 / 임시 파일 정리 → "마크다운 미리보기" 선택
→ Step 1: [디렉터리 선택] ./cache / ./python / ./java / ./auth → "./python" 선택
→ Step 2: [파일 선택] django-프레임워크-사용법.md / fastapi-프레임워크-사용법.md → "django-프레임워크-사용법.md" 선택
→ HTML 변환 중...
→ 브라우저 미리보기 열림
→ 완료!
```

### 임시 파일 정리

```
/md-preview
→ Step 0: [작업 선택] 마크다운 미리보기 / 임시 파일 정리 → "임시 파일 정리" 선택
→ 다음 3개의 파일을 찾았습니다:
  - /tmp/cache-strategies-preview.html
  - /tmp/rag-패턴-preview.html
  - /tmp/django-프레임워크-사용법-preview.html
→ [확인] 삭제하시겠습니까? → "예" 선택
→ ✓ 3개의 임시 HTML 파일을 삭제했습니다.
```

## 주의사항

- **의존성 없음**: Python 기본 라이브러리만 사용 (json 모듈)
- marked.js는 CDN에서 로드되므로 인터넷 연결 필요
- 임시 HTML 파일은 `/tmp` 디렉토리에 생성됩니다
- 매번 새로 실행 시 이전 HTML 파일을 덮어씁니다
- GitHub Flavored Markdown (GFM)을 지원합니다 (테이블, 체크박스 등)
- **임시 파일 정리**: `/tmp/*-preview.html` 패턴의 모든 파일을 삭제하므로 주의 필요
