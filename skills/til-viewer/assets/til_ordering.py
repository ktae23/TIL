#!/usr/bin/env python3
"""TIL 문서 학습 순서 정렬 공통 모듈

파일 경로 알파벳순으로 정렬하면 `advanced/`가 `main/`보다 앞에 와서
심화 문서가 기초 문서보다 먼저 노출된다. 이 모듈은 학습 순서를 기준으로
정렬 키를 만들고, 카테고리별 통합 연번을 부여한다.

정렬 우선순위:
  1) 카테고리 이름 (최상위 폴더)
  2) 하위 폴더 순위 — 루트(0) < main(1) < advanced(2) < 기타(4, 이름순)
  3) 파일명 앞 숫자 — `01-`, `02-` 를 정수로 파싱 (번호 없으면 맨 뒤)
  4) 파일명 알파벳순
"""
import re

# 하위 폴더 학습 순서. 값이 작을수록 앞에 온다.
SUBCATEGORY_RANK = {
    # 기초 트랙
    "main": 1,
    "basic": 1,
    "basics": 1,
    "core": 1,
    "fundamentals": 1,
    # 심화 트랙
    "advanced": 2,
    # 실습 트랙
    "practice": 3,
}

# 하위 폴더가 없는 루트 파일의 순위
ROOT_RANK = 0

# 위 표에 없는 하위 폴더의 순위 (같은 순위 안에서는 폴더 이름순)
UNKNOWN_RANK = 4

# 파일명에 번호가 없을 때 부여할 정렬용 번호 (맨 뒤로 보냄)
NO_NUMBER = 10 ** 6

_NUM_PREFIX = re.compile(r"^(\d+)")


def subcategory_of(rel_path):
    """카테고리 아래의 하위 폴더 경로를 반환한다. 없으면 빈 문자열."""
    parts = rel_path.split("/")
    return "/".join(parts[1:-1]) if len(parts) > 2 else ""


def order_key(rel_path):
    """학습 순서 정렬용 키를 만든다.

    rel_path 는 TIL 루트 기준 상대 경로 (예: "kotlin/main/01-overview.md").
    같은 카테고리끼리 묶이도록 카테고리 이름을 키 맨 앞에 둔다.
    """
    parts = rel_path.split("/")
    category = parts[0]
    filename = parts[-1]
    sub = subcategory_of(rel_path)

    if not sub:
        rank = (ROOT_RANK, "")
    elif sub in SUBCATEGORY_RANK:
        rank = (SUBCATEGORY_RANK[sub], "")
    else:
        rank = (UNKNOWN_RANK, sub.lower())

    match = _NUM_PREFIX.match(filename)
    number = int(match.group(1)) if match else NO_NUMBER

    return (category, rank, number, filename.lower())


def sort_paths(rel_paths):
    """상대 경로 목록을 학습 순서로 정렬해 반환한다."""
    return sorted(rel_paths, key=order_key)


def numbered_title(index, title):
    """사이드바 표시용으로 제목 앞에 순번을 붙인다."""
    return f"{index}. {title}"
