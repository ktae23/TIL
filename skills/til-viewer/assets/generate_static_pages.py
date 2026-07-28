#!/usr/bin/env python3
"""NotebookLM 호환 정적 HTML 페이지 생성 스크립트

각 TIL 마크다운 문서를 개별 정적 HTML 페이지로 변환하여
JavaScript 없이도 콘텐츠를 읽을 수 있게 합니다.

생성 파일:
  - dist/pages/{category}/{filename}.html  (개별 문서)
  - dist/content-index.html                (전체 목록)
  - dist/sitemap.xml                       (검색엔진용)
  - dist/robots.txt                        (크롤러 설정)
  - dist/notebooklm-urls-by-category.txt   (카테고리별 URL 목록)
"""
import os
import sys
import xml.etree.ElementTree as ET
from datetime import datetime
from glob import glob

import markdown

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from til_ordering import order_key, numbered_title

BASE_URL = "https://ktae23.github.io/TIL"

INLINE_CSS = """
body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    max-width: 900px;
    margin: 0 auto;
    padding: 20px;
    line-height: 1.7;
    color: #24292e;
    background: #fff;
}
nav {
    font-size: 14px;
    color: #586069;
    margin-bottom: 24px;
    padding-bottom: 12px;
    border-bottom: 1px solid #e1e4e8;
}
nav a { color: #0366d6; text-decoration: none; }
nav a:hover { text-decoration: underline; }
article h1 { font-size: 2em; margin-bottom: 16px; padding-bottom: 8px; border-bottom: 1px solid #e1e4e8; }
article h2 { font-size: 1.5em; margin-top: 24px; margin-bottom: 16px; padding-bottom: 8px; border-bottom: 1px solid #e1e4e8; }
article h3 { font-size: 1.25em; margin-top: 20px; margin-bottom: 12px; }
article p { margin-bottom: 16px; }
article code { background: #f6f8fa; padding: 2px 6px; border-radius: 4px; font-size: 0.9em; }
article pre { background: #f6f8fa; padding: 16px; border-radius: 8px; overflow-x: auto; margin-bottom: 16px; }
article pre code { background: transparent; padding: 0; }
article table { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
article th, article td { border: 1px solid #e1e4e8; padding: 8px 12px; text-align: left; }
article th { background: #f6f8fa; font-weight: 600; }
article blockquote { border-left: 4px solid #0366d6; padding-left: 16px; color: #586069; margin: 0 0 16px 0; }
article ul, article ol { margin-bottom: 16px; padding-left: 24px; }
article li { margin-bottom: 8px; }
article img { max-width: 100%; height: auto; }
footer {
    margin-top: 48px;
    padding-top: 16px;
    border-top: 1px solid #e1e4e8;
    font-size: 13px;
    color: #586069;
}
footer a { color: #0366d6; text-decoration: none; }
footer a:hover { text-decoration: underline; }
""".strip()


def scan_files(til_path):
    """TIL 마크다운 파일을 스캔하여 카테고리별로 분류"""
    categories = {}

    # 학습 순서(하위폴더 순위 -> 파일명 번호 -> 이름)로 정렬
    md_files = sorted(
        glob(f"{til_path}/**/*.md", recursive=True),
        key=lambda p: order_key(os.path.relpath(p, til_path)),
    )
    # 카테고리별 통합 연번 카운터
    seq = {}

    for md_file in md_files:
        rel_path = os.path.relpath(md_file, til_path)
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

        # 카테고리 내 학습 순서 연번을 제목 앞에 부여
        seq[category] = seq.get(category, 0) + 1
        title = numbered_title(seq[category], title)

        if category not in categories:
            categories[category] = {"files": [], "subcategories": {}}

        # 정적 페이지용 경로 생성: pages/{category}/[{subcategory}/]{filename}.html
        if subcategory:
            page_path = f"{category}/{subcategory}/{filename.replace('.md', '.html')}"
        else:
            page_path = f"{category}/{filename.replace('.md', '.html')}"

        file_entry = {
            "filename": filename,
            "title": title,
            "rel_path": rel_path,
            "page_path": page_path,
            "content": content,
            "category": category,
            "subcategory": subcategory,
        }

        if subcategory:
            if subcategory not in categories[category]["subcategories"]:
                categories[category]["subcategories"][subcategory] = {"files": []}
            categories[category]["subcategories"][subcategory]["files"].append(file_entry)
        else:
            categories[category]["files"].append(file_entry)

    return categories


def render_html(title, content_html, category, subcategory, page_path):
    """개별 문서용 자기완결적 HTML 생성"""
    description = ""
    # content_html에서 첫 텍스트 150자 추출
    import re
    text_only = re.sub(r"<[^>]+>", "", content_html)
    text_only = re.sub(r"\s+", " ", text_only).strip()
    description = text_only[:150]

    breadcrumb_parts = [f'<a href="{BASE_URL}/">Home</a>']
    breadcrumb_parts.append(f'<a href="{BASE_URL}/content-index.html">{category}</a>')
    if subcategory:
        breadcrumb_parts.append(subcategory)
    breadcrumb_parts.append(title)
    breadcrumb = " &gt; ".join(breadcrumb_parts)

    canonical_url = f"{BASE_URL}/pages/{page_path}"

    return f"""<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>{title} - ktae23 TIL</title>
  <meta name="description" content="{description}">
  <meta property="og:title" content="{title} - ktae23 TIL">
  <meta property="og:description" content="{description}">
  <meta property="og:url" content="{canonical_url}">
  <link rel="canonical" href="{canonical_url}">
  <style>{INLINE_CSS}</style>
</head>
<body>
  <nav>{breadcrumb}</nav>
  <article>
{content_html}
  </article>
  <footer>
    <a href="{BASE_URL}/">TIL Viewer</a> |
    <a href="{BASE_URL}/content-index.html">All Documents</a>
  </footer>
</body>
</html>"""


def generate_content_index(categories, output_dir):
    """전체 문서 카테고리별 목록 페이지 생성"""
    sections = []
    for cat_name in sorted(categories.keys()):
        cat = categories[cat_name]
        items = []

        for f in sorted(cat["files"], key=lambda x: x["title"]):
            url = f"{BASE_URL}/pages/{f['page_path']}"
            items.append(f'    <li><a href="{url}">{f["title"]}</a></li>')

        for sub_name in sorted(cat["subcategories"].keys()):
            sub = cat["subcategories"][sub_name]
            items.append(f'    <li><strong>{sub_name}</strong><ul>')
            for f in sorted(sub["files"], key=lambda x: x["title"]):
                url = f"{BASE_URL}/pages/{f['page_path']}"
                items.append(f'      <li><a href="{url}">{f["title"]}</a></li>')
            items.append("    </ul></li>")

        total = len(cat["files"]) + sum(
            len(s["files"]) for s in cat["subcategories"].values()
        )
        sections.append(
            f'  <h2>{cat_name} ({total} documents)</h2>\n  <ul>\n'
            + "\n".join(items)
            + "\n  </ul>"
        )

    body = "\n".join(sections)
    total_docs = sum(
        len(c["files"]) + sum(len(s["files"]) for s in c["subcategories"].values())
        for c in categories.values()
    )

    html = f"""<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>TIL Document Index - ktae23</title>
  <meta name="description" content="ktae23 TIL 전체 문서 목록 ({total_docs}개 문서, {len(categories)}개 카테고리)">
  <link rel="canonical" href="{BASE_URL}/content-index.html">
  <style>{INLINE_CSS}</style>
</head>
<body>
  <h1>ktae23 TIL - Document Index</h1>
  <p>Total: {total_docs} documents in {len(categories)} categories</p>
  <p><a href="{BASE_URL}/">TIL Viewer (Interactive)</a></p>
{body}
  <footer>
    <a href="{BASE_URL}/">TIL Viewer</a>
  </footer>
</body>
</html>"""

    with open(os.path.join(output_dir, "content-index.html"), "w", encoding="utf-8") as f:
        f.write(html)


def generate_sitemap(all_pages, output_dir):
    """sitemap.xml 생성"""
    urlset = ET.Element("urlset")
    urlset.set("xmlns", "http://www.sitemaps.org/schemas/sitemap/0.9")

    # content-index 추가
    url_el = ET.SubElement(urlset, "url")
    ET.SubElement(url_el, "loc").text = f"{BASE_URL}/content-index.html"
    ET.SubElement(url_el, "changefreq").text = "weekly"
    ET.SubElement(url_el, "priority").text = "1.0"

    for page_path in sorted(all_pages):
        url_el = ET.SubElement(urlset, "url")
        ET.SubElement(url_el, "loc").text = f"{BASE_URL}/pages/{page_path}"
        ET.SubElement(url_el, "changefreq").text = "monthly"
        ET.SubElement(url_el, "priority").text = "0.6"

    tree = ET.ElementTree(urlset)
    ET.indent(tree, space="  ")
    tree.write(
        os.path.join(output_dir, "sitemap.xml"),
        encoding="unicode",
        xml_declaration=True,
    )


def generate_robots_txt(output_dir):
    """robots.txt 생성"""
    content = f"""User-agent: *
Allow: /

Sitemap: {BASE_URL}/sitemap.xml
"""
    with open(os.path.join(output_dir, "robots.txt"), "w", encoding="utf-8") as f:
        f.write(content)


def generate_url_list(categories, output_dir):
    """카테고리별 URL 목록 생성 (NotebookLM import용)"""
    lines = []

    for cat_name in sorted(categories.keys()):
        cat = categories[cat_name]

        # 카테고리 직속 파일
        if cat["files"]:
            count = len(cat["files"])
            lines.append(f"## {cat_name} ({count} documents)")
            for f in sorted(cat["files"], key=lambda x: x["title"]):
                lines.append(f"{BASE_URL}/pages/{f['page_path']}")
            lines.append("")

        # 서브카테고리
        for sub_name in sorted(cat["subcategories"].keys()):
            sub = cat["subcategories"][sub_name]
            count = len(sub["files"])
            lines.append(f"## {cat_name} > {sub_name} ({count} documents)")
            for f in sorted(sub["files"], key=lambda x: x["title"]):
                lines.append(f"{BASE_URL}/pages/{f['page_path']}")
            lines.append("")

    with open(
        os.path.join(output_dir, "notebooklm-urls-by-category.txt"),
        "w",
        encoding="utf-8",
    ) as f:
        f.write("\n".join(lines))


def main():
    if len(sys.argv) < 3:
        print("Usage: python3 generate_static_pages.py <TIL_PATH> <OUTPUT_DIR>")
        print("Example: python3 generate_static_pages.py /path/to/til ./dist")
        sys.exit(1)

    til_path = sys.argv[1]
    output_dir = sys.argv[2]

    os.makedirs(output_dir, exist_ok=True)

    md_renderer = markdown.Markdown(extensions=["tables", "fenced_code", "toc"])

    # 1. 파일 스캔
    categories = scan_files(til_path)

    # 2. 개별 정적 HTML 페이지 생성
    all_pages = []
    page_count = 0

    for cat in categories.values():
        for file_entry in cat["files"]:
            md_renderer.reset()
            content_html = md_renderer.convert(file_entry["content"])
            html = render_html(
                file_entry["title"],
                content_html,
                file_entry["category"],
                file_entry["subcategory"],
                file_entry["page_path"],
            )
            out_path = os.path.join(output_dir, "pages", file_entry["page_path"])
            os.makedirs(os.path.dirname(out_path), exist_ok=True)
            with open(out_path, "w", encoding="utf-8") as f:
                f.write(html)
            all_pages.append(file_entry["page_path"])
            page_count += 1

        for sub in cat["subcategories"].values():
            for file_entry in sub["files"]:
                md_renderer.reset()
                content_html = md_renderer.convert(file_entry["content"])
                html = render_html(
                    file_entry["title"],
                    content_html,
                    file_entry["category"],
                    file_entry["subcategory"],
                    file_entry["page_path"],
                )
                out_path = os.path.join(output_dir, "pages", file_entry["page_path"])
                os.makedirs(os.path.dirname(out_path), exist_ok=True)
                with open(out_path, "w", encoding="utf-8") as f:
                    f.write(html)
                all_pages.append(file_entry["page_path"])
                page_count += 1

    # 3. content-index.html 생성
    generate_content_index(categories, output_dir)

    # 4. sitemap.xml 생성
    generate_sitemap(all_pages, output_dir)

    # 5. robots.txt 생성
    generate_robots_txt(output_dir)

    # 6. NotebookLM URL 목록 생성
    generate_url_list(categories, output_dir)

    print(f"정적 페이지 빌드 완료!")
    print(f"  카테고리: {len(categories)}개")
    print(f"  정적 페이지: {page_count}개")
    print(f"  출력 위치: {output_dir}/")
    print(f"    - pages/          (개별 HTML)")
    print(f"    - content-index.html")
    print(f"    - sitemap.xml")
    print(f"    - robots.txt")
    print(f"    - notebooklm-urls-by-category.txt")


if __name__ == "__main__":
    main()
