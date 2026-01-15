#!/usr/bin/env python3
"""알고리즘 실습 뷰어 생성 스크립트 (GitHub Pages용)"""
import json
import os
import re
import sys


def extract_md_content(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        return f.read()


def extract_java_code(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        return f.read()


def generate_algorithm_practice(til_path, output_dir):
    """알고리즘 실습 HTML 생성"""
    practice_dir = os.path.join(til_path, 'algorithm', 'practice')
    java_dir = os.path.join(practice_dir, 'java')

    if not os.path.exists(practice_dir):
        print("algorithm/practice 폴더가 없습니다.")
        return False

    # MD 파일과 Java 파일 매핑
    mapping = {
        '00_complexity': 'TimeSpaceComplexity.java',
        '03_recursion': 'RecursionBasic.java',
        '04_combination': 'CombinationBasic.java',
        '05_permutation': 'PermutationBasic.java',
        '06_sorting': 'SortingBasic.java',
        '07_bruteforce': 'BruteForceBasic.java',
        '08_greedy': 'GreedyBasic.java',
        '09_dp': 'DPBasic.java',
        '10_binary_search': 'BinarySearchBasic.java',
        '11_parametric_search': 'ParametricSearchBasic.java',
        '12_two_pointer': 'TwoPointerBasic.java',
        '13_graph': 'GraphRepresentation.java',
        '14_dfs': 'DFSBasic.java',
        '15_bfs': 'BFSBasic.java',
        '16_dijkstra': 'DijkstraBasic.java',
        '17_tree': 'TreeTraversal.java',
    }

    topics = []
    md_files = sorted([f for f in os.listdir(practice_dir) if f.endswith('.md')])

    for md_file in md_files:
        md_path = os.path.join(practice_dir, md_file)
        md_content = extract_md_content(md_path)

        title_match = re.search(r'^#\s+(.+)$', md_content, re.MULTILINE)
        title = title_match.group(1) if title_match else md_file.replace('.md', '')

        base_name = md_file.replace('.md', '')
        java_file = mapping.get(base_name, '')
        java_code = ''
        if java_file:
            java_path = os.path.join(java_dir, java_file)
            if os.path.exists(java_path):
                java_code = extract_java_code(java_path)

        topics.append({
            'id': base_name,
            'title': title,
            'md_content': md_content,
            'java_code': java_code,
            'java_file': java_file
        })

    if not topics:
        print("토픽을 찾을 수 없습니다.")
        return False

    data_json = json.dumps(topics, ensure_ascii=False)
    data_json = data_json.replace("</", "<\\/").replace("<!--", "<\\!--")

    html_content = f'''<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Algorithm Practice - TIL</title>
    <link rel="stylesheet" href="lib/codemirror/codemirror.min.css">
    <link rel="stylesheet" href="lib/codemirror/dracula.min.css">
    <link rel="stylesheet" href="lib/highlight/github.min.css" id="hljs-light">
    <link rel="stylesheet" href="lib/highlight/github-dark.min.css" id="hljs-dark" disabled>
    <style>
        :root {{ --bg-primary: #ffffff; --bg-secondary: #f6f8fa; --bg-tertiary: #e1e4e8; --text-primary: #24292e; --text-secondary: #586069; --border-color: #e1e4e8; --accent-color: #0366d6; --sidebar-width: 260px; }}
        [data-theme="dark"] {{ --bg-primary: #0d1117; --bg-secondary: #161b22; --bg-tertiary: #21262d; --text-primary: #c9d1d9; --text-secondary: #8b949e; --border-color: #30363d; --accent-color: #58a6ff; }}
        * {{ margin: 0; padding: 0; box-sizing: border-box; }}
        body {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: var(--bg-primary); color: var(--text-primary); height: 100vh; overflow: hidden; }}
        .app-container {{ display: flex; height: 100vh; }}
        .sidebar {{ width: var(--sidebar-width); background: var(--bg-secondary); border-right: 1px solid var(--border-color); display: flex; flex-direction: column; flex-shrink: 0; }}
        .sidebar-header {{ padding: 16px; border-bottom: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center; gap: 8px; }}
        .sidebar-header h1 {{ font-size: 16px; font-weight: 600; margin: 0; }}
        .back-link {{ color: var(--accent-color); text-decoration: none; font-size: 13px; padding: 4px 8px; border-radius: 4px; background: var(--bg-tertiary); }}
        .back-link:hover {{ opacity: 0.8; }}
        .theme-toggle {{ padding: 4px 8px; font-size: 12px; background: var(--bg-tertiary); border: none; border-radius: 4px; color: var(--text-primary); cursor: pointer; }}
        .topic-list {{ flex: 1; overflow-y: auto; padding: 8px; }}
        .topic-item {{ padding: 10px 12px; border-radius: 6px; cursor: pointer; font-size: 14px; margin-bottom: 4px; transition: background 0.2s; }}
        .topic-item:hover {{ background: var(--bg-tertiary); }}
        .topic-item.active {{ background: var(--accent-color); color: white; }}
        .main-content {{ flex: 1; display: flex; flex-direction: column; overflow: hidden; }}
        .tabs {{ display: flex; background: var(--bg-secondary); border-bottom: 1px solid var(--border-color); padding: 0 16px; }}
        .tab {{ padding: 12px 24px; cursor: pointer; font-size: 14px; font-weight: 500; border-bottom: 2px solid transparent; transition: all 0.2s; color: var(--text-secondary); }}
        .tab:hover {{ color: var(--text-primary); }}
        .tab.active {{ color: var(--accent-color); border-bottom-color: var(--accent-color); }}
        .panel-container {{ flex: 1; overflow: hidden; }}
        .panel {{ height: 100%; display: none; overflow: hidden; }}
        .panel.active {{ display: flex; }}
        #description-panel {{ flex-direction: column; }}
        .md-content {{ flex: 1; overflow-y: auto; padding: 32px; max-width: 900px; }}
        .md-content h1 {{ font-size: 2em; margin-bottom: 16px; padding-bottom: 8px; border-bottom: 1px solid var(--border-color); }}
        .md-content h2 {{ font-size: 1.5em; margin-top: 24px; margin-bottom: 16px; padding-bottom: 8px; border-bottom: 1px solid var(--border-color); }}
        .md-content h3 {{ font-size: 1.25em; margin-top: 20px; margin-bottom: 12px; }}
        .md-content p {{ margin-bottom: 16px; line-height: 1.7; }}
        .md-content code {{ background: var(--bg-tertiary); padding: 2px 6px; border-radius: 4px; font-size: 0.9em; }}
        .md-content pre {{ background: var(--bg-secondary); padding: 16px; border-radius: 8px; overflow-x: auto; margin-bottom: 16px; }}
        .md-content pre code {{ background: transparent; padding: 0; }}
        .md-content table {{ width: 100%; border-collapse: collapse; margin-bottom: 16px; }}
        .md-content th, .md-content td {{ border: 1px solid var(--border-color); padding: 8px 12px; text-align: left; }}
        .md-content th {{ background: var(--bg-secondary); font-weight: 600; }}
        .md-content blockquote {{ border-left: 4px solid var(--accent-color); padding-left: 16px; color: var(--text-secondary); margin-bottom: 16px; }}
        .md-content ul, .md-content ol {{ margin-bottom: 16px; padding-left: 24px; }}
        .md-content li {{ margin-bottom: 8px; line-height: 1.6; }}
        .md-content hr {{ border: none; border-top: 1px solid var(--border-color); margin: 24px 0; }}
        #practice-panel {{ flex-direction: column; }}
        .editor-container {{ flex: 1; display: flex; flex-direction: column; overflow: hidden; }}
        .editor-header {{ display: flex; justify-content: space-between; align-items: center; padding: 8px 16px; background: var(--bg-secondary); border-bottom: 1px solid var(--border-color); }}
        .editor-title {{ font-size: 14px; font-weight: 500; }}
        .editor-actions {{ display: flex; gap: 8px; }}
        .btn {{ padding: 6px 12px; font-size: 13px; border: none; border-radius: 4px; cursor: pointer; font-weight: 500; transition: all 0.2s; }}
        .btn-primary {{ background: var(--accent-color); color: white; }}
        .btn-primary:hover {{ opacity: 0.9; }}
        .btn-secondary {{ background: var(--bg-tertiary); color: var(--text-primary); }}
        .btn-secondary:hover {{ background: var(--border-color); }}
        .editor-wrapper {{ flex: 1; overflow: hidden; }}
        .CodeMirror {{ height: 100% !important; font-size: 14px; font-family: 'Fira Code', 'Monaco', monospace; }}
        .loading {{ display: flex; align-items: center; justify-content: center; height: 100%; color: var(--text-secondary); }}
        @media (max-width: 768px) {{ .sidebar {{ position: fixed; left: -260px; top: 0; height: 100%; z-index: 100; transition: left 0.3s; }} .sidebar.open {{ left: 0; }} .mobile-header {{ display: flex !important; }} }}
        .mobile-header {{ display: none; padding: 12px 16px; background: var(--bg-secondary); border-bottom: 1px solid var(--border-color); align-items: center; gap: 12px; }}
        .menu-btn {{ background: none; border: none; font-size: 20px; cursor: pointer; color: var(--text-primary); }}
    </style>
</head>
<body>
    <div class="app-container">
        <aside class="sidebar" id="sidebar">
            <div class="sidebar-header">
                <h1>Algorithm</h1>
                <a href="til-viewer.html" class="back-link">← TIL</a>
                <button class="theme-toggle" id="theme-toggle">Dark</button>
            </div>
            <div class="topic-list" id="topic-list"></div>
        </aside>
        <main class="main-content">
            <div class="mobile-header">
                <button class="menu-btn" id="menu-btn">☰</button>
                <span id="current-topic-title">Algorithm Practice</span>
            </div>
            <div class="tabs">
                <div class="tab active" data-tab="description">설명</div>
                <div class="tab" data-tab="practice">예제 코드</div>
            </div>
            <div class="panel-container">
                <div class="panel active" id="description-panel">
                    <div class="md-content" id="md-content">
                        <div class="loading">토픽을 선택하세요</div>
                    </div>
                </div>
                <div class="panel" id="practice-panel">
                    <div class="editor-container">
                        <div class="editor-header">
                            <span class="editor-title" id="editor-title">Example.java</span>
                            <div class="editor-actions">
                                <button class="btn btn-secondary" id="reset-btn">초기화</button>
                                <button class="btn btn-primary" id="copy-btn">복사</button>
                            </div>
                        </div>
                        <div class="editor-wrapper">
                            <textarea id="code-editor"></textarea>
                        </div>
                    </div>
                </div>
            </div>
        </main>
    </div>
    <script src="lib/marked.min.js"></script>
    <script src="lib/highlight/highlight.min.js"></script>
    <script src="lib/codemirror/codemirror.min.js"></script>
    <script src="lib/codemirror/clike.min.js"></script>
    <script>
        const TOPICS = {data_json};
        const state = {{ currentTopic: null, currentTab: 'description', theme: localStorage.getItem('algo-theme') || 'light', editor: null, originalCode: '' }};

        function init() {{
            applyTheme(state.theme);
            buildTopicList();
            initTabs();
            initEditor();
            initEventHandlers();
            if (TOPICS.length > 0) loadTopic(TOPICS[0].id);
        }}

        function buildTopicList() {{
            const container = document.getElementById('topic-list');
            container.innerHTML = '';
            TOPICS.forEach(topic => {{
                const item = document.createElement('div');
                item.className = 'topic-item' + (state.currentTopic === topic.id ? ' active' : '');
                item.textContent = topic.title;
                item.onclick = () => loadTopic(topic.id);
                container.appendChild(item);
            }});
        }}

        function loadTopic(topicId) {{
            const topic = TOPICS.find(t => t.id === topicId);
            if (!topic) return;
            state.currentTopic = topicId;
            state.originalCode = topic.java_code;
            buildTopicList();
            document.getElementById('current-topic-title').textContent = topic.title;
            const mdContent = document.getElementById('md-content');
            mdContent.innerHTML = marked.parse(topic.md_content);
            mdContent.querySelectorAll('pre code').forEach(block => hljs.highlightElement(block));
            buildTOC();
            document.getElementById('editor-title').textContent = topic.java_file || 'Example.java';
            if (state.editor) state.editor.setValue(topic.java_code || '// Java 코드');
            document.getElementById('sidebar').classList.remove('open');
        }}

        function buildTOC() {{
            const mdContent = document.getElementById('md-content');
            const headings = mdContent.querySelectorAll('h2');
            if (headings.length === 0) return;

            // 기존 TOC 제거
            const oldToc = mdContent.querySelector('.inline-toc');
            if (oldToc) oldToc.remove();

            // 인라인 TOC 생성
            const toc = document.createElement('div');
            toc.className = 'inline-toc';
            toc.style.cssText = 'background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 8px; padding: 16px; margin-bottom: 24px;';

            const tocTitle = document.createElement('div');
            tocTitle.style.cssText = 'font-weight: 600; margin-bottom: 12px; font-size: 14px;';
            tocTitle.textContent = '📑 목차';
            toc.appendChild(tocTitle);

            const tocList = document.createElement('div');
            tocList.style.cssText = 'display: flex; flex-wrap: wrap; gap: 8px 16px;';

            headings.forEach((heading, idx) => {{
                const id = 'section-' + idx;
                heading.id = id;
                const a = document.createElement('a');
                a.href = '#' + id;
                a.textContent = heading.textContent;
                a.style.cssText = 'color: var(--accent-color); text-decoration: none; font-size: 13px;';
                a.onclick = (e) => {{
                    e.preventDefault();
                    heading.scrollIntoView({{ behavior: 'smooth' }});
                }};
                tocList.appendChild(a);
            }});

            toc.appendChild(tocList);

            // h1 다음에 삽입
            const h1 = mdContent.querySelector('h1');
            if (h1 && h1.nextSibling) {{
                h1.parentNode.insertBefore(toc, h1.nextSibling);
            }} else {{
                mdContent.insertBefore(toc, mdContent.firstChild);
            }}
        }}

        function initTabs() {{
            document.querySelectorAll('.tab').forEach(tab => {{
                tab.onclick = () => {{
                    state.currentTab = tab.dataset.tab;
                    document.querySelectorAll('.tab').forEach(t => t.classList.toggle('active', t.dataset.tab === tab.dataset.tab));
                    document.querySelectorAll('.panel').forEach(p => p.classList.toggle('active', p.id === tab.dataset.tab + '-panel'));
                    if (tab.dataset.tab === 'practice' && state.editor) state.editor.refresh();
                }};
            }});
        }}

        function initEditor() {{
            state.editor = CodeMirror.fromTextArea(document.getElementById('code-editor'), {{
                mode: 'text/x-java', theme: state.theme === 'dark' ? 'dracula' : 'default',
                lineNumbers: true, indentUnit: 4, tabSize: 4, indentWithTabs: false, lineWrapping: true, matchBrackets: true, readOnly: false
            }});
        }}

        function initEventHandlers() {{
            document.getElementById('theme-toggle').onclick = () => applyTheme(state.theme === 'light' ? 'dark' : 'light');
            document.getElementById('copy-btn').onclick = () => {{
                navigator.clipboard.writeText(state.editor.getValue()).then(() => {{
                    const btn = document.getElementById('copy-btn');
                    btn.textContent = '복사됨!';
                    setTimeout(() => btn.textContent = '복사', 1500);
                }});
            }};
            document.getElementById('reset-btn').onclick = () => state.editor.setValue(state.originalCode);
            document.getElementById('menu-btn').onclick = () => document.getElementById('sidebar').classList.toggle('open');
        }}

        function applyTheme(theme) {{
            state.theme = theme;
            localStorage.setItem('algo-theme', theme);
            document.documentElement.setAttribute('data-theme', theme);
            document.getElementById('theme-toggle').textContent = theme === 'dark' ? 'Light' : 'Dark';
            document.getElementById('hljs-light').disabled = (theme === 'dark');
            document.getElementById('hljs-dark').disabled = (theme === 'light');
            if (state.editor) state.editor.setOption('theme', theme === 'dark' ? 'dracula' : 'default');
        }}

        document.addEventListener('DOMContentLoaded', init);
    </script>
</body>
</html>'''

    output_html = os.path.join(output_dir, 'algorithm-practice.html')
    with open(output_html, 'w', encoding='utf-8') as f:
        f.write(html_content)

    print(f"✓ 알고리즘 실습 뷰어 생성: {output_html}")
    print(f"  토픽 수: {len(topics)}개")
    return True


if __name__ == '__main__':
    if len(sys.argv) < 3:
        print("Usage: python3 generate_algorithm_practice.py <TIL_PATH> <OUTPUT_DIR>")
        sys.exit(1)
    generate_algorithm_practice(sys.argv[1], sys.argv[2])
