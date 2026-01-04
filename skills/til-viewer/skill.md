---
name: til-viewer
description: TIL 저장소 전체를 인터랙티브 웹 뷰어로 열어 탐색, 검색, 미리보기합니다.
allowed-tools: Bash, Read
---

# til-viewer

TIL 저장소의 모든 마크다운 파일을 하나의 인터랙티브 웹 페이지로 변환하여 브라우저에서 탐색할 수 있습니다.

## 중요: 컨텍스트 절약

**절대로 Read 도구로 마크다운 파일들을 개별 읽지 마세요!**
파일이 많으면 컨텍스트가 폭발합니다. Python 스크립트 한 번 실행으로 모든 처리를 완료합니다.

## 실행 단계

### Step 1. 경로 확인

Bash로 SKILL_DIR을 확인합니다:

```bash
SKILL_DIR=$(dirname "$(find ~/.claude/skills -name 'skill.md' -path '*/til-viewer/*' 2>/dev/null)")
echo "SKILL_DIR: $SKILL_DIR"
```

til skill에서 TIL 저장소 경로를 읽습니다:
- Read 도구로 `{SKILL_DIR}/../til/skill.md` 파일을 읽습니다
- "저장소 경로:" 라인에서 경로를 추출합니다 (예: `/Users/buzz/til`)

### Step 2. Python 스크립트로 뷰어 생성

Bash로 Python 스크립트를 실행합니다 (TIL_PATH와 ASSETS_PATH를 적절히 치환):

```bash
python3 "{SKILL_DIR}/assets/generate_viewer.py" "{TIL_PATH}" "{SKILL_DIR}/assets"
```

예시:
```bash
python3 "/Users/buzz/.claude/skills/til-viewer/assets/generate_viewer.py" "/Users/buzz/til" "/Users/buzz/.claude/skills/til-viewer/assets"
```

### Step 3. 브라우저 열기

```bash
open /tmp/til-viewer.html
```

### Step 4. 완료 메시지

사용자에게 알립니다:

```
TIL Viewer를 생성했습니다.

통계:
  - 카테고리: {N}개
  - 파일: {N}개
  - 총 라인: {N}줄

파일 위치: /tmp/til-viewer.html

사용 팁:
  - Ctrl+K: 검색
  - Ctrl+D: 다크/라이트 테마 전환
```

## 주의사항

- **Read 도구 사용 금지**: 마크다운 파일을 Read 도구로 개별 읽으면 컨텍스트 폭발
- Python 스크립트 한 번 실행으로 모든 처리 완료
- 파일 크기: HTML 파일이 2-3MB가 될 수 있음
- 임시 파일: `/tmp/til-viewer.html`은 시스템 재부팅 시 삭제될 수 있음
