---
name: til-viewer
description: TIL 저장소 전체를 인터랙티브 웹 뷰어로 열어 탐색, 검색, 미리보기합니다.
allowed-tools: Bash, Read, Write, Glob
---

# til-viewer

TIL 저장소의 모든 마크다운 파일을 하나의 인터랙티브 웹 페이지로 변환하여 브라우저에서 탐색할 수 있습니다.

## 경로 설정

- assets 경로: /Users/buzz/.claude/skills/til-viewer/assets
- til skill 경로: /Users/buzz/.claude/skills/til/skill.md

## 실행 단계

### Step 1. TIL 디렉토리 확인

1. til skill에서 TIL 저장소 경로 읽기:
   - Read 도구로 `/Users/buzz/.claude/skills/til/skill.md` 파일을 읽습니다.
   - "저장소 경로:" 라인에서 경로를 추출합니다.
   - 예: `- 저장소 경로: /Users/buzz/til` → TIL 경로: `/Users/buzz/til`

2. 경로 파싱:
   ```
   skill.md 내용에서 "저장소 경로:" 패턴을 찾아 경로 추출
   ```

3. TIL 저장소인지 검증 (카테고리 디렉토리 확인):
   ```bash
   ls -d {til_path}/*/ 2>/dev/null | head -5
   ```

4. 경로를 찾을 수 없는 경우:
   - 에러 메시지 출력: "til skill에서 저장소 경로를 찾을 수 없습니다."
   - 현재 작업 디렉토리를 폴백으로 사용

### Step 2. 마크다운 파일 스캔

1. Glob 도구를 사용하여 모든 .md 파일 검색:
   - pattern: `**/*.md`
   - path: TIL 저장소 경로

2. skills 디렉토리 제외:
   - `skills/` 아래의 .md 파일은 스킬 정의 파일이므로 데이터에서 제외

3. 파일 경로에서 카테고리 추출:
   - 예: `spring/spring-batch-basics.md` → 카테고리: `spring`

### Step 3. 각 파일의 메타데이터 추출

각 마크다운 파일에 대해 Read 도구로 읽고:

1. **제목 추출**:
   - 첫 번째 `# ` 라인을 찾아서 제목으로 사용
   - 없으면 파일명 (확장자 제거)을 제목으로 사용

2. **메타데이터 수집**:
   - `filename`: 파일명 (예: `spring-batch-basics.md`)
   - `title`: 추출한 제목
   - `path`: 상대 경로 (예: `spring/spring-batch-basics.md`)
   - `content`: 전체 마크다운 내용
   - `size`: 파일 크기 (바이트)
   - `lines`: 줄 수

3. **카테고리별 그룹화**:
   ```python
   categories = {}
   # 예시:
   # categories["spring"] = {
   #   "files": [
   #     {
   #       "filename": "spring-batch-basics.md",
   #       "title": "Spring Batch 기초 - Job, Step, 실행 흐름",
   #       "path": "spring/spring-batch-basics.md",
   #       "content": "# Spring Batch...",
   #       "size": 12345,
   #       "lines": 234
   #     }
   #   ]
   # }
   ```

### Step 4. JSON 데이터 생성

Python을 사용하여 TIL 데이터를 JSON으로 직렬화:

```python
import json
from datetime import datetime

til_data = {
    "categories": categories,  # Step 3에서 생성한 데이터
    "metadata": {
        "totalFiles": total_files_count,
        "totalCategories": len(categories),
        "generatedAt": datetime.now().isoformat()
    }
}

# JSON 문자열 생성 (한글 유지)
json_str = json.dumps(til_data, ensure_ascii=False, indent=2)
```

### Step 5. HTML 파일 생성

Write 도구를 사용하여 `/tmp/til-viewer.html` 파일 생성:

**assets 경로 설정**:
```python
assets_path = "/Users/buzz/.claude/skills/til-viewer/assets"
```

**HTML 템플릿 구조**:

```html
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TIL Viewer</title>

    <!-- Local CSS -->
    <link rel="stylesheet" href="{ASSETS_PATH}/css/viewer.css">

    <!-- highlight.js themes (local) -->
    <link rel="stylesheet" href="{ASSETS_PATH}/lib/highlight/github.min.css" id="hljs-light">
    <link rel="stylesheet" href="{ASSETS_PATH}/lib/highlight/github-dark.min.css" id="hljs-dark" disabled>
</head>
<body>
    <!-- Header -->
    <div class="header">
        <h1>TIL Viewer</h1>
        <div class="search-container">
            <input type="text" id="search-input" placeholder="Search files and content... (Ctrl+K)" autocomplete="off" />
        </div>
        <button class="theme-toggle" id="theme-toggle">Dark</button>
    </div>

    <!-- Main Container -->
    <div class="main-container">
        <!-- Sidebar -->
        <div class="sidebar" id="sidebar">
            <div id="file-list"></div>
        </div>

        <!-- Content Area -->
        <div class="content-area" id="content-area">
            <div class="content-inner" id="content">
                <div class="loading">Select a file from the sidebar to view</div>
            </div>
        </div>

        <!-- TOC Panel -->
        <div class="toc-panel" id="toc-panel">
            <h4>목차</h4>
            <div id="toc-list"></div>
        </div>
    </div>

    <!-- Local Libraries -->
    <script src="{ASSETS_PATH}/lib/marked.min.js"></script>
    <script src="{ASSETS_PATH}/lib/fuse.min.js"></script>
    <script src="{ASSETS_PATH}/lib/highlight/highlight.min.js"></script>

    <!-- Embedded TIL Data (동적 주입) -->
    <script>
        const TIL_DATA = {TIL_JSON_DATA};
    </script>

    <!-- Local Viewer Logic -->
    <script src="{ASSETS_PATH}/js/viewer.js"></script>
</body>
</html>
```

**플레이스홀더 교체**:
- `{ASSETS_PATH}` → `/Users/buzz/.claude/skills/til-viewer/assets`
- `{TIL_JSON_DATA}` → Step 4에서 생성한 JSON

### Step 6. 브라우저 열기

```bash
open /tmp/til-viewer.html
```

### Step 7. 완료 메시지

사용자에게 다음과 같이 알립니다:

```
TIL Viewer를 생성했습니다.
브라우저에서 뷰어가 열렸습니다.

통계:
  - 카테고리: {totalCategories}개
  - 파일: {totalFiles}개
  - 총 라인: {totalLines}줄

파일 위치: /tmp/til-viewer.html

사용 팁:
  - Ctrl+K: 검색
  - Ctrl+D: 다크/라이트 테마 전환
  - 사이드바에서 카테고리 클릭: 펼치기/접기
```

## 구현 가이드

### Python 코드 예시

Claude는 다음과 같은 방식으로 데이터를 처리합니다:

```python
import json
import os
from pathlib import Path
from datetime import datetime

# Step 2-3: 파일 스캔 및 메타데이터 추출
categories = {}
total_lines = 0

for md_file in sorted(glob_results):
    # skills 디렉토리 제외
    if 'skills/' in md_file:
        continue

    # 카테고리 추출
    parts = md_file.split('/')
    if len(parts) < 2:
        continue

    category = parts[0]
    filename = parts[-1]

    # 파일 읽기
    with open(md_file, 'r', encoding='utf-8') as f:
        content = f.read()

    # 제목 추출
    title = filename.replace('.md', '')
    for line in content.split('\n'):
        if line.startswith('# '):
            title = line[2:].strip()
            break

    # 메타데이터
    lines = content.count('\n') + 1
    total_lines += lines

    if category not in categories:
        categories[category] = {"files": []}

    categories[category]["files"].append({
        "filename": filename,
        "title": title,
        "path": md_file,
        "content": content,
        "size": len(content.encode('utf-8')),
        "lines": lines
    })

# Step 4: JSON 생성
til_data = {
    "categories": categories,
    "metadata": {
        "totalFiles": sum(len(cat["files"]) for cat in categories.values()),
        "totalCategories": len(categories),
        "totalLines": total_lines,
        "generatedAt": datetime.now().isoformat()
    }
}

json_str = json.dumps(til_data, ensure_ascii=False, indent=2)

# Step 5: HTML 생성
assets_path = "/Users/buzz/.claude/skills/til-viewer/assets"
html_template = """..."""  # 위의 HTML 템플릿
html_content = html_template.replace('{ASSETS_PATH}', assets_path)
html_content = html_content.replace('{TIL_JSON_DATA}', json_str)

with open('/tmp/til-viewer.html', 'w', encoding='utf-8') as f:
    f.write(html_content)
```

## 주의사항

- **파일 크기**: 모든 마크다운을 임베딩하므로 HTML 파일이 2-3MB가 됩니다
- **브라우저 호환성**: 최신 브라우저 (Chrome, Firefox, Safari, Edge) 필요
- **한글 지원**: UTF-8 인코딩과 `ensure_ascii=False` 사용
- **임시 파일**: `/tmp/til-viewer.html`은 시스템 재부팅 시 삭제될 수 있음
- **로컬 파일**: 라이브러리와 뷰어 코드는 assets 폴더에 저장되어 오프라인에서도 동작

## 라이브러리 버전

assets 폴더에 저장된 라이브러리:
- marked 11.1.1
- fuse.js 7.0.0
- highlight.js 11.9.0

## 사용 예시

```
/til-viewer
→ til skill에서 TIL 경로 읽기...
→ TIL 경로: /Users/buzz/til
→ 36개의 마크다운 파일 스캔 중...
→ 메타데이터 추출 중...
→ JSON 데이터 생성...
→ HTML 뷰어 생성 중...
→ 브라우저에서 열기...
완료!
```
