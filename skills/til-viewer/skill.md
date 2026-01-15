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

## 경로 설정 지침

**이 지침은 매 실행마다 동일하게 적용됩니다:**

1. **SKILL_DIR 탐지**: til-viewer 스킬 디렉토리를 동적으로 찾습니다
2. **TIL_PATH 탐지**: til 스킬의 `저장소 경로:` 라인에서 읽어옵니다
3. 하드코딩된 경로를 사용하지 않습니다

## 실행 단계

### Step 1. 경로 확인

Bash로 SKILL_DIR을 동적으로 확인합니다:

```bash
SKILL_DIR=$(dirname "$(find ~/.claude/skills -name 'skill.md' -path '*/til-viewer/*' 2>/dev/null | head -1)")
echo "SKILL_DIR: $SKILL_DIR"
```

til skill에서 TIL 저장소 경로를 읽습니다:
- Read 도구로 `{SKILL_DIR}/../til/skill.md` 파일을 읽습니다
- "저장소 경로:" 라인에서 경로를 추출합니다
- 값이 `$TIL_ROOT`이면 현재 작업 디렉토리(pwd)를 사용합니다

### Step 2. Python 스크립트로 뷰어 생성

Bash로 두 개의 Python 스크립트를 실행합니다:

```bash
# TIL Viewer 생성
python3 "{SKILL_DIR}/assets/generate_viewer.py" "{TIL_PATH}" "{SKILL_DIR}/assets"

# Algorithm Practice 뷰어 생성
python3 "{SKILL_DIR}/assets/generate_algorithm_practice.py" "{TIL_PATH}" "{SKILL_DIR}/assets"
```

**경로 치환 예시:**
- SKILL_DIR: `/Users/username/.claude/skills/til-viewer`
- TIL_PATH: `/Users/username/til`

```bash
python3 "/Users/username/.claude/skills/til-viewer/assets/generate_viewer.py" "/Users/username/til" "/Users/username/.claude/skills/til-viewer/assets"
python3 "/Users/username/.claude/skills/til-viewer/assets/generate_algorithm_practice.py" "/Users/username/til" "/Users/username/.claude/skills/til-viewer/assets"
```

### Step 3. 브라우저 열기

```bash
open "{SKILL_DIR}/assets/til-viewer.html"
```

### Step 4. 완료 메시지

사용자에게 알립니다:

```
TIL Viewer를 생성했습니다.

통계:
  - 카테고리: {N}개
  - 파일: {N}개
  - 총 라인: {N}줄
  - Algorithm Practice: {N}개 토픽

사용 팁:
  - Ctrl+K: 검색
  - Ctrl+D: 다크/라이트 테마 전환
  - 사이드바 상단 "Algorithm Practice" 버튼으로 알고리즘 실습 페이지 이동
```

## 주의사항

- **Read 도구 사용 금지**: 마크다운 파일을 Read 도구로 개별 읽으면 컨텍스트 폭발
- Python 스크립트 한 번 실행으로 모든 처리 완료
- 파일 크기: HTML 파일이 2-3MB가 될 수 있음
- **경로는 항상 동적으로 탐지**: 하드코딩된 절대 경로 사용 금지

## 생성 파일

- `til-viewer.html`: TIL 메인 뷰어
- `algorithm-practice.html`: 알고리즘 실습 뷰어 (algorithm/practice 폴더 기반)
