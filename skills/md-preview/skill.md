---
name: md-preview
description: 마크다운 파일을 HTML로 변환하여 브라우저에서 미리보기합니다.
allowed-tools: Bash, Read, Write, Glob, AskUserQuestion
---

# md-preview

마크다운 파일을 HTML로 변환하여 브라우저에서 미리보기를 엽니다.

## 실행 단계

/md-preview 실행 시 다음 단계를 순차적으로 진행합니다.

### Step 1. 마크다운 파일 선택

1. 현재 디렉토리에서 .md 파일을 검색합니다:
   ```bash
   ls -t *.md 2>/dev/null | head -4
   ```

2. **AskUserQuestion 도구**를 사용하여 파일 선택지를 제공합니다:
   - header: "파일"
   - question: "미리보기할 마크다운 파일을 선택해주세요."
   - options: 검색된 .md 파일들 (최대 4개, 최근 수정순)
   - 사용자가 "Other"를 선택하면 파일 경로를 직접 입력

### Step 2. HTML 변환 및 미리보기

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

### Step 3. 완료 메시지

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

```
/md-preview
→ Step 1: [파일 선택] README.md / example.md / notes.md → "README.md" 선택
→ HTML 변환 중...
→ 브라우저 미리보기 열림
→ 완료!
```

## 주의사항

- **의존성 없음**: Python 기본 라이브러리만 사용 (json 모듈)
- marked.js는 CDN에서 로드되므로 인터넷 연결 필요
- 임시 HTML 파일은 `/tmp` 디렉토리에 생성됩니다
- 매번 새로 실행 시 이전 HTML 파일을 덮어씁니다
- GitHub Flavored Markdown (GFM)을 지원합니다 (테이블, 체크박스 등)
