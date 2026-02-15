#!/usr/bin/env python3
"""GitHub Pages용 TIL Viewer HTML 생성 스크립트

기존 generate_viewer.py와 달리:
- 상대 경로로 assets 참조 (GitHub Pages 호환)
- 출력 디렉토리 지정 가능
- assets 폴더를 출력 디렉토리로 복사
"""
import json
import os
import shutil
import sys
from datetime import datetime
from glob import glob


def main():
    if len(sys.argv) < 3:
        print("Usage: python3 generate_viewer_pages.py <TIL_PATH> <OUTPUT_DIR>")
        print("Example: python3 generate_viewer_pages.py /path/to/til ./dist")
        sys.exit(1)

    TIL_PATH = sys.argv[1]
    OUTPUT_DIR = sys.argv[2]
    SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

    # 출력 디렉토리 생성
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    # 1. 파일 스캔 및 메타데이터 추출
    categories = {}
    total_lines = 0

    for md_file in sorted(glob(f"{TIL_PATH}/**/*.md", recursive=True)):
        rel_path = os.path.relpath(md_file, TIL_PATH)
        if rel_path.startswith("skills/"):
            continue
        parts = rel_path.split("/")
        if len(parts) < 2:
            continue
        category = parts[0]
        filename = parts[-1]
        subcategory = parts[1] if len(parts) >= 3 else None

        with open(md_file, "r", encoding="utf-8") as f:
            content = f.read()

        title = filename.replace(".md", "")
        for line in content.split("\n"):
            if line.startswith("# "):
                title = line[2:].strip()
                break

        lines = content.count("\n") + 1
        total_lines += lines

        if category not in categories:
            categories[category] = {"files": [], "subcategories": {}}

        file_entry = {
            "filename": filename,
            "title": title,
            "path": rel_path,
            "content": content,
            "size": len(content.encode("utf-8")),
            "lines": lines
        }

        if subcategory:
            if subcategory not in categories[category]["subcategories"]:
                categories[category]["subcategories"][subcategory] = {"files": []}
            categories[category]["subcategories"][subcategory]["files"].append(file_entry)
        else:
            categories[category]["files"].append(file_entry)

    # 2. JSON 데이터 생성
    til_data = {
        "categories": categories,
        "metadata": {
            "totalFiles": sum(
                len(cat["files"]) + sum(len(sub["files"]) for sub in cat.get("subcategories", {}).values())
                for cat in categories.values()
            ),
            "totalCategories": len(categories),
            "totalLines": total_lines,
            "generatedAt": datetime.now().isoformat()
        }
    }
    json_str = json.dumps(til_data, ensure_ascii=False)
    # HTML script 태그 내에서 안전하게 사용하기 위해 이스케이프
    json_str = json_str.replace("</", "<\\/").replace("<!--", "<\\!--")

    # 캐시 버스팅용 타임스탬프
    cache_bust = int(datetime.now().timestamp())

    # 3. HTML 생성 (상대 경로 사용)
    html_content = f"""<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TIL Viewer</title>
    <link rel="stylesheet" href="assets/css/viewer.css?v={cache_bust}">
    <link rel="stylesheet" href="assets/lib/highlight/github.min.css" id="hljs-light">
    <link rel="stylesheet" href="assets/lib/highlight/github-dark.min.css" id="hljs-dark" disabled>
</head>
<body>
    <div class="sidebar-overlay" id="sidebar-overlay"></div>
    <div class="header">
        <button class="menu-button" id="menu-button">☰</button>
        <h1>TIL Viewer</h1>
        <div class="search-container">
            <input type="text" id="search-input" placeholder="Search files and content... (Ctrl+K)" autocomplete="off" />
        </div>
        <button class="theme-toggle" id="theme-toggle">Dark</button>
    </div>
    <div class="main-container">
        <div class="sidebar" id="sidebar">
            <div class="special-links">
                <a href="algorithm-practice.html" class="special-link">📚 Algorithm Practice</a>
            </div>
            <div id="file-list"></div>
        </div>
        <div class="content-area" id="content-area">
            <div class="content-inner" id="content">
                <div class="loading">Select a file from the sidebar to view</div>
            </div>
        </div>
        <div class="toc-panel" id="toc-panel">
            <h4>목차</h4>
            <div id="toc-list"></div>
        </div>
    </div>
    <div class="quick-actions" id="quick-actions">
        <button class="quick-btn" id="pdf-download-btn" onclick="downloadPDF()" title="PDF 다운로드 (P)" style="display:none">📥</button>
        <button class="quick-btn" onclick="showShortcuts()" title="단축키 (?)">?</button>
        <button class="quick-btn" onclick="scrollToTop()" title="맨 위로">&#8593;</button>
    </div>

    <div class="shortcuts-modal" id="shortcuts-modal">
        <h3>키보드 단축키</h3>
        <div class="shortcut-item"><span>이전 문서</span><span class="shortcut-key">&#8592;</span></div>
        <div class="shortcut-item"><span>다음 문서</span><span class="shortcut-key">&#8594;</span></div>
        <div class="shortcut-item"><span>검색</span><span class="shortcut-key">Ctrl+K</span></div>
        <div class="shortcut-item"><span>테마 전환</span><span class="shortcut-key">T</span></div>
        <div class="shortcut-item"><span>PDF 다운로드</span><span class="shortcut-key">P</span></div>
        <div class="shortcut-item"><span>맨 위로</span><span class="shortcut-key">Home</span></div>
        <div class="shortcut-item"><span>닫기</span><span class="shortcut-key">Esc</span></div>
    </div>

    <script src="assets/lib/marked.min.js"></script>
    <script src="assets/lib/fuse.min.js"></script>
    <script src="assets/lib/highlight/highlight.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/html2pdf.js/0.10.2/html2pdf.bundle.min.js"></script>
    <script>const TIL_DATA = {json_str};</script>
    <script src="assets/js/viewer.js?v={cache_bust}"></script>
</body>
</html>"""

    # 4. index.html 저장 (기존 TIL Viewer)
    output_html = os.path.join(OUTPUT_DIR, "index.html")
    with open(output_html, "w", encoding="utf-8") as f:
        f.write(html_content)

    # 5. assets 폴더 복사
    src_assets = SCRIPT_DIR
    dst_assets = os.path.join(OUTPUT_DIR, "assets")

    # 기존 assets 삭제 후 복사
    if os.path.exists(dst_assets):
        shutil.rmtree(dst_assets)

    # css, js, lib 폴더만 복사 (Python 스크립트 제외)
    os.makedirs(dst_assets, exist_ok=True)
    for folder in ["css", "js", "lib"]:
        src_folder = os.path.join(src_assets, folder)
        dst_folder = os.path.join(dst_assets, folder)
        if os.path.exists(src_folder):
            shutil.copytree(src_folder, dst_folder)

    total_files = til_data["metadata"]["totalFiles"]
    print(f"GitHub Pages 빌드 완료!")
    print(f"  카테고리: {len(categories)}개")
    print(f"  파일: {total_files}개")
    print(f"  총 라인: {total_lines}줄")
    print(f"  출력 위치: {OUTPUT_DIR}/")
    print(f"    - index.html (README)")
    print(f"    - viewer.html (TIL Viewer)")
    print(f"    - assets/")


if __name__ == "__main__":
    main()
