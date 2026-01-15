#!/usr/bin/env python3
"""TIL Viewer HTML 생성 스크립트"""
import json
import os
import sys
from datetime import datetime
from glob import glob

def main():
    if len(sys.argv) < 3:
        print("Usage: python3 generate_viewer.py <TIL_PATH> <ASSETS_PATH>")
        sys.exit(1)

    TIL_PATH = sys.argv[1]
    ASSETS_PATH = sys.argv[2]

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
            categories[category] = {"files": []}

        categories[category]["files"].append({
            "filename": filename,
            "title": title,
            "path": rel_path,
            "content": content,
            "size": len(content.encode("utf-8")),
            "lines": lines
        })

    # 2. JSON 데이터 생성
    til_data = {
        "categories": categories,
        "metadata": {
            "totalFiles": sum(len(cat["files"]) for cat in categories.values()),
            "totalCategories": len(categories),
            "totalLines": total_lines,
            "generatedAt": datetime.now().isoformat()
        }
    }
    json_str = json.dumps(til_data, ensure_ascii=False)
    # </script> 태그가 JSON 내에 있으면 스크립트가 깨지므로 이스케이프
    json_str = json_str.replace("</script>", "<\\/script>")

    # 3. HTML 생성
    # 상대 경로 사용 (HTML이 assets 폴더에 생성됨)
    html_content = f"""<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TIL Viewer</title>
    <link rel="stylesheet" href="css/viewer.css">
    <link rel="stylesheet" href="lib/highlight/github.min.css" id="hljs-light">
    <link rel="stylesheet" href="lib/highlight/github-dark.min.css" id="hljs-dark" disabled>
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
    <script src="lib/marked.min.js"></script>
    <script src="lib/fuse.min.js"></script>
    <script src="lib/highlight/highlight.min.js"></script>
    <script>const TIL_DATA = {json_str};</script>
    <script src="js/viewer.js"></script>
</body>
</html>"""

    # HTML을 assets 폴더에 생성 (상대 경로가 동작하도록)
    output_path = os.path.join(ASSETS_PATH, "til-viewer.html")
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(html_content)

    total_files = til_data["metadata"]["totalFiles"]
    print(f"카테고리: {len(categories)}개")
    print(f"파일: {total_files}개")
    print(f"총 라인: {total_lines}줄")
    print(f"파일 위치: {output_path}")

if __name__ == "__main__":
    main()
