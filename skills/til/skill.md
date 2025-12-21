---
name: til
description: TIL(Today I Learned) 학습 기록을 관리합니다. 새 학습 내용 작성, 카테고리 관리, push까지 한 번에 처리합니다.
allowed-tools: Bash, Read, Write, Glob, Grep, AskUserQuestion
---

# til

TIL(Today I Learned) 저장소에 학습 내용을 기록하고 관리합니다.

## TIL 저장소 정보

- 저장소 경로: $TIL_ROOT
- 카테고리별 폴더 구조: java/, domain/, spring/, docker/, database/ 등

## 실행 단계

/til 실행 시 다음 단계를 순차적으로 진행합니다. **반드시 AskUserQuestion 도구를 사용하여 선택지 기반으로 입력받습니다.**

### Step 0. TIL 경로 설정 (최초 1회)

스킬 최초 실행 시 `$TIL_ROOT` 변수가 설정되어 있지 않으면:

1. 현재 작업 디렉토리(pwd)를 확인합니다
2. **AskUserQuestion 도구**를 사용하여 TIL 저장소 경로를 확인합니다:
   - header: "경로"
   - question: "TIL 저장소 경로를 확인해주세요."
   - options:
     - 현재 디렉토리 경로 (Recommended)
   - 사용자가 "Other"를 선택하면 직접 경로 입력

3. 확인된 경로를 `$TIL_ROOT`에 설정하고, 이 skill.md 파일의 `$TIL_ROOT` 값을 실제 경로로 업데이트합니다:
   - 위의 "저장소 경로" 라인을 실제 경로로 변경
   - 예: `- 저장소 경로: /Users/username/til`

**이후 실행에서는 이 단계를 건너뜁니다.**

### Step 1. 카테고리 선택

1. 먼저 기존 카테고리 목록을 조회합니다:
   ```bash
   ls -d $TIL_ROOT/*/
   ```

2. **AskUserQuestion 도구**를 사용하여 카테고리 선택지를 제공합니다:
   - 조회된 기존 카테고리들을 옵션으로 제공 (최대 4개, 가장 최근 수정된 순)
   - 사용자가 "Other"를 선택하면 새 카테고리명을 입력받아 폴더 생성

### Step 2. 주제 입력

**AskUserQuestion 도구**를 사용하여 주제 입력을 요청합니다:
- header: "주제"
- question: "TIL에 기록할 주제/제목을 입력해주세요."
- options에 예시 주제 2-3개 제공 (해당 카테고리에 맞는 예시)
- 사용자가 "Other"를 선택하면 직접 입력

### Step 3. 학습 내용 입력

**AskUserQuestion 도구**를 사용하여 학습 내용을 요청합니다:
- header: "내용"
- question: "정리할 학습 내용을 입력해주세요."
- options: 예시로 간단한 템플릿 1-2개 제공
- 사용자가 "Other"를 선택하면 직접 입력

### Step 4. 추가 지시사항 (Optional)

**AskUserQuestion 도구**를 사용하여 추가 지시사항을 선택받습니다:
- header: "스타일"
- question: "문서 작성 스타일을 선택해주세요."
- options:
  - "코드 예제 많이": 실용적인 코드 예제를 많이 포함
  - "간결하게": 핵심만 간단히 정리
  - "상세하게": 배경 설명과 함께 자세히
  - "비교 표 포함": 표 형식으로 비교 정리
- 사용자가 "Other"를 선택하면 직접 지시사항 입력

### Step 5. 최종 확인 및 시작

**AskUserQuestion 도구**를 사용하여 최종 확인을 받습니다:
- header: "확인"
- question: "다음 내용으로 TIL을 작성하시겠습니까?\n\n- 카테고리: {선택한 카테고리}\n- 주제: {입력한 주제}\n- 스타일: {선택한 스타일}"
- options:
  - "시작": 작성 시작
  - "취소": 작업 취소

사용자가 "시작"을 선택하면 다음을 수행합니다:

1. 마크다운 파일 작성:
   - 파일명: `{주제-kebab-case}.md`
   - 경로: `$TIL_ROOT/{category}/{filename}.md`

2. 파일 내용:
   - 제목 (# 헤더)
   - 설명 및 개요
   - 목차 (내용이 긴 경우)
   - 본문 (코드 예시, 표, 설명 포함)
   - 참고 자료

3. 마크다운 미리보기:

   마크다운 파일 작성 완료 후 브라우저에서 미리보기를 엽니다:
   ```bash
   open "{생성된_파일_경로}"
   ```

4. 미리보기 확인 및 Git 진행:

   **AskUserQuestion 도구**를 사용하여 미리보기 확인을 받습니다:
   - header: "확인"
   - question: "마크다운 미리보기를 확인하셨나요?"
   - options:
     - "Push 진행": 확인 완료, git commit & push 진행
     - "수정 필요": 파일을 수정하고 다시 확인
     - "취소": 작업 취소 (파일은 유지)

   사용자가 "수정 필요"를 선택하면:
   - 수정할 내용을 입력받아 파일 수정
   - 다시 미리보기 열기
   - 이 단계 반복

   사용자가 "Push 진행"을 선택하면:
   ```bash
   git add .
   git commit -m "docs: add TIL - {제목}"
   git push
   ```

## 마크다운 작성 가이드

- 명확한 제목과 계층적 구조
- 코드 블록에 언어 지정 (```java, ```python 등)
- 핵심 개념은 **굵은 글씨**로 강조
- 비교/정리는 표 형식 활용
- 실용적인 예제 코드 포함
- 마지막에 작성일 표시: `*마지막 업데이트: YYYY년 MM월*`

## 사용 예시

```
/til
→ Step 1: [카테고리 선택] java / spring / docker / database  → "java" 선택
→ Step 2: [주제 입력] Stream API / Optional 활용 / 람다식  → "Other" 선택 후 "Stream API 병렬 처리" 입력
→ Step 3: [내용 입력] → "Other" 선택 후 학습 내용 입력
→ Step 4: [스타일 선택] 코드 예제 많이 / 간결하게 / 상세하게 / 비교 표 포함  → "코드 예제 많이" 선택
→ Step 5: [최종 확인] 시작 / 취소  → "시작" 선택
→ 마크다운 파일 생성 → 미리보기 자동 열림
→ [미리보기 확인] Push 진행 / 수정 필요 / 취소  → "Push 진행" 선택
→ git commit → git push
```

## 주의사항

- 기존 파일 덮어쓰기 전 사용자 확인
- push 실패 시 원인 안내
- 커밋 메시지는 변경 내용을 명확히 설명
