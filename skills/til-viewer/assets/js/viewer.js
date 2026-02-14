// ========================================
// TIL Viewer Application (Toss Design)
// ========================================
// Note: TIL_DATA is injected inline in the HTML file

// ========================================
// PDF PRINT CSS (재사용 가능한 인쇄 스타일)
// ========================================
function getPrintCSS() {
    return '@page { margin: 15mm; }' +
    '* { margin:0; padding:0; box-sizing:border-box; }' +
    'body { font-family:-apple-system,BlinkMacSystemFont,"Segoe UI","Noto Sans KR",Roboto,sans-serif; color:#212529; line-height:1.6; background:#fff; }' +
    '.content-inner { max-width:100%; }' +
    '.content-inner h1 { font-size:2rem; margin-bottom:8px; padding-bottom:16px; border-bottom:2px solid #3182f6; }' +
    '.content-inner h2 { font-size:1.5rem; margin:32px 0 16px; padding-bottom:8px; border-bottom:1px solid #dee2e6; }' +
    '.content-inner h3 { font-size:1.25rem; margin:24px 0 12px; color:#3182f6; }' +
    '.content-inner h4 { font-size:1.1rem; margin:20px 0 10px; }' +
    '.content-inner p { margin-bottom:16px; }' +
    '.content-inner ul,.content-inner ol { margin-bottom:16px; padding-left:24px; }' +
    '.content-inner li { margin-bottom:8px; }' +
    '.content-inner pre { background:#f4f4f5; border-radius:8px; padding:16px; overflow-x:auto; margin-bottom:16px; border:1px solid #dee2e6; line-height:1.45; page-break-inside:avoid; }' +
    '.content-inner code { font-family:"SF Mono",Monaco,Consolas,"Courier New",monospace; font-size:0.9em; }' +
    '.content-inner :not(pre)>code { background:#f4f4f5; padding:2px 6px; border-radius:4px; }' +
    '.content-inner pre code { background:transparent; padding:0; }' +
    '.content-inner blockquote { border-left:4px solid #3182f6; padding:12px 16px; margin:16px 0; color:#495057; background:#f8f9fa; border-radius:0 8px 8px 0; page-break-inside:avoid; }' +
    '.content-inner table { width:100%; border-collapse:collapse; margin-bottom:16px; page-break-inside:auto; }' +
    '.content-inner th,.content-inner td { border:1px solid #dee2e6; padding:10px 12px; text-align:left; }' +
    '.content-inner th { background:#f8f9fa; font-weight:600; }' +
    '.content-inner tr { page-break-inside:avoid; }' +
    '.content-inner hr { border:none; border-top:1px solid #dee2e6; margin:32px 0; }' +
    '.content-inner a { color:#3182f6; text-decoration:none; }' +
    '.content-inner strong { color:#3182f6; }' +
    '.content-inner img { max-width:100%; height:auto; page-break-inside:avoid; }' +
    'h1,h2,h3,h4 { page-break-after:avoid; }' +
    '.batch-doc + .batch-doc { page-break-before:always; }' +
    'pre code.hljs{display:block;overflow-x:auto;padding:1em}code.hljs{padding:3px 5px}' +
    '.hljs{color:#24292e;background:#fff}.hljs-doctag,.hljs-keyword,.hljs-meta .hljs-keyword,.hljs-template-tag,.hljs-template-variable,.hljs-type,.hljs-variable.language_{color:#d73a49}.hljs-title,.hljs-title.class_,.hljs-title.class_.inherited__,.hljs-title.function_{color:#6f42c1}.hljs-attr,.hljs-attribute,.hljs-literal,.hljs-meta,.hljs-number,.hljs-operator,.hljs-selector-attr,.hljs-selector-class,.hljs-selector-id,.hljs-variable{color:#005cc5}.hljs-meta .hljs-string,.hljs-regexp,.hljs-string{color:#032f62}.hljs-built_in,.hljs-symbol{color:#e36209}.hljs-code,.hljs-comment,.hljs-formula{color:#6a737d}.hljs-name,.hljs-quote,.hljs-selector-pseudo,.hljs-selector-tag{color:#22863a}.hljs-subst{color:#24292e}.hljs-section{color:#005cc5;font-weight:700}.hljs-bullet{color:#735c0f}.hljs-emphasis{color:#24292e;font-style:italic}.hljs-strong{color:#24292e;font-weight:700}.hljs-addition{color:#22863a;background-color:#f0fff4}.hljs-deletion{color:#b31d28;background-color:#ffeef0}';
}

// ========================================
// PDF PRINT TEMPLATE (buildPrintHTML은 getPrintCSS 사용)
// ========================================
function buildPrintHTML(title) {
    return '<!DOCTYPE html><html lang="ko"><head><meta charset="UTF-8">' +
    '<title>' + title.replace(/</g, '&lt;') + '</title><style>' +
    getPrintCSS() +
    '</style></head><body><div class="content-inner" id="pdf-content"></div></body></html>';
}

// ========================================
// APPLICATION STATE
// ========================================
const state = {
    currentFile: null,
    currentTheme: localStorage.getItem('til-theme') || 'light',
    searchIndex: null,
    collapsedCategories: new Set(),
    currentFilter: 'all',
    fileOrder: [],  // flat list of all file paths for arrow nav
    selectMode: false,
    selectedFiles: new Set()
};

// ========================================
// INITIALIZATION
// ========================================
function init() {
    applyTheme(state.currentTheme);

    // Build flat file order for arrow navigation
    buildFileOrder();

    // 기본적으로 모든 카테고리를 닫힌 상태로 설정
    Object.keys(TIL_DATA.categories).forEach(category => {
        state.collapsedCategories.add(category);
    });

    initFilter();
    buildFileList();
    initSearch();
    initKeyboardShortcuts();
    initRouter();
    initMobileMenu();
    initProgressBar();

    // Display stats
    updateStats();

    // 우선순위: URL hash > localStorage
    const hashFile = getFileFromHash();
    const lastFile = localStorage.getItem('til-last-file');

    if (hashFile && findFileByPath(hashFile)) {
        loadFile(hashFile, { updateUrl: false });
    } else if (lastFile && findFileByPath(lastFile)) {
        loadFile(lastFile);
    }
}

// ========================================
// STATS
// ========================================
function updateStats() {
    const statsEl = document.getElementById('header-stats');
    if (statsEl && TIL_DATA.metadata) {
        const { totalFiles, totalCategories } = TIL_DATA.metadata;
        statsEl.textContent = `${totalCategories} categories · ${totalFiles} files`;
    }
}

// ========================================
// FILE ORDER (for arrow navigation)
// ========================================
function buildFileOrder() {
    state.fileOrder = [];
    const categories = Object.keys(TIL_DATA.categories).sort();
    categories.forEach(category => {
        TIL_DATA.categories[category].files.forEach(file => {
            state.fileOrder.push(file.path);
        });
    });
}

// ========================================
// DATE FILTER
// ========================================
function initFilter() {
    const filterButtons = document.querySelectorAll('.filter-btn');
    filterButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            filterButtons.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            state.currentFilter = btn.dataset.filter;
            buildFileList();
        });
    });
}

function isWithinDays(dateStr, days) {
    if (days === 'all') return true;
    const fileDate = new Date(dateStr);
    const cutoff = new Date();
    cutoff.setDate(cutoff.getDate() - parseInt(days));
    return fileDate >= cutoff;
}

function getFilteredData() {
    if (state.currentFilter === 'all') {
        return TIL_DATA.categories;
    }

    const filtered = {};
    for (const category in TIL_DATA.categories) {
        const files = TIL_DATA.categories[category].files.filter(
            file => isWithinDays(file.createdAt, state.currentFilter)
        );
        if (files.length > 0) {
            filtered[category] = { files };
        }
    }
    return filtered;
}

// ========================================
// FILE MANAGEMENT
// ========================================
function buildFileList() {
    const container = document.getElementById('file-list');
    container.innerHTML = '';

    const filteredCategories = getFilteredData();
    const categories = Object.keys(filteredCategories).sort();

    // 필터 적용 시 정보 표시
    if (state.currentFilter !== 'all') {
        const totalFiles = Object.values(filteredCategories)
            .reduce((sum, cat) => sum + cat.files.length, 0);
        const filterInfo = document.createElement('div');
        filterInfo.className = 'filter-info';
        filterInfo.textContent = `최근 ${state.currentFilter}일: ${totalFiles}개 문서`;
        container.appendChild(filterInfo);
    }

    categories.forEach(category => {
        const categoryData = filteredCategories[category];
        const isCollapsed = state.collapsedCategories.has(category);

        const categoryDiv = document.createElement('div');
        categoryDiv.className = 'category';

        const titleDiv = document.createElement('div');
        titleDiv.className = 'category-title';
        titleDiv.innerHTML = `
            <span class="category-arrow ${isCollapsed ? '' : 'expanded'}">&#9654;</span>
            <span>${category}</span>
            <span class="category-count">(${categoryData.files.length})</span>
        `;
        titleDiv.onclick = (e) => {
            if (e.target.closest('.category-select-all')) return;
            toggleCategory(category);
        };

        if (state.selectMode) {
            const allSelected = categoryData.files.every(f => state.selectedFiles.has(f.path));
            const selectAllBtn = document.createElement('span');
            selectAllBtn.className = 'category-select-all';
            selectAllBtn.textContent = allSelected ? '☑' : '☐';
            selectAllBtn.title = '전체 선택/해제';
            selectAllBtn.onclick = (e) => {
                e.stopPropagation();
                const paths = categoryData.files.map(f => f.path);
                if (allSelected) {
                    paths.forEach(p => state.selectedFiles.delete(p));
                } else {
                    paths.forEach(p => state.selectedFiles.add(p));
                }
                buildFileList();
                updateSelectBar();
            };
            titleDiv.appendChild(selectAllBtn);
        }

        categoryDiv.appendChild(titleDiv);

        const fileList = document.createElement('div');
        fileList.className = `file-list ${isCollapsed ? 'collapsed' : ''}`;

        categoryData.files.forEach(file => {
            const fileItem = document.createElement('div');
            fileItem.className = 'file-item';
            if (state.selectMode) {
                const cb = document.createElement('input');
                cb.type = 'checkbox';
                cb.className = 'file-select-cb';
                cb.checked = state.selectedFiles.has(file.path);
                cb.onclick = (e) => {
                    e.stopPropagation();
                    if (cb.checked) {
                        state.selectedFiles.add(file.path);
                    } else {
                        state.selectedFiles.delete(file.path);
                    }
                    updateSelectBar();
                };
                fileItem.appendChild(cb);
                const span = document.createElement('span');
                span.textContent = file.title;
                fileItem.appendChild(span);
            } else {
                fileItem.textContent = file.title;
            }
            fileItem.title = file.createdAt ? `${file.title} (${file.createdAt})` : file.title;
            fileItem.onclick = () => {
                if (state.selectMode) {
                    const cb = fileItem.querySelector('.file-select-cb');
                    if (cb) { cb.checked = !cb.checked; cb.onclick({ stopPropagation() {} }); }
                    return;
                }
                loadFile(file.path);
            };

            if (state.currentFile === file.path) {
                fileItem.classList.add('active');
            }

            fileList.appendChild(fileItem);
        });

        categoryDiv.appendChild(fileList);
        container.appendChild(categoryDiv);
    });
}

function toggleCategory(category) {
    if (state.collapsedCategories.has(category)) {
        state.collapsedCategories.delete(category);
    } else {
        state.collapsedCategories.add(category);
    }
    buildFileList();
}

function findFileByPath(path) {
    for (const category in TIL_DATA.categories) {
        const file = TIL_DATA.categories[category].files.find(f => f.path === path);
        if (file) return file;
    }
    return null;
}

function loadFile(filePath, options = {}) {
    const file = findFileByPath(filePath);
    if (!file) return;

    state.currentFile = filePath;
    localStorage.setItem('til-last-file', filePath);

    // URL hash 업데이트
    if (options.updateUrl !== false) {
        updateHash(filePath);
    }

    // 해당 카테고리 자동 펼치기
    const category = filePath.split('/')[0];
    if (state.collapsedCategories.has(category)) {
        state.collapsedCategories.delete(category);
    }

    // Render markdown
    const contentDiv = document.getElementById('content');
    contentDiv.innerHTML = marked.parse(file.content);

    // Highlight code blocks
    contentDiv.querySelectorAll('pre code').forEach(block => {
        hljs.highlightElement(block);
    });

    // Generate TOC
    generateTOC();

    // Update active state in sidebar
    buildFileList();

    // Scroll to top
    document.getElementById('content-area').scrollTop = 0;

    // Init checkboxes
    initCheckboxes();

    // Show PDF download button
    const pdfBtn = document.getElementById('pdf-download-btn');
    if (pdfBtn) pdfBtn.style.display = '';

    // Close sidebar on mobile
    if (window.innerWidth <= 768) {
        closeMobileSidebar();
    }
}

// ========================================
// ARROW NAVIGATION
// ========================================
function navigateFile(direction) {
    if (!state.currentFile) return;
    const currentIndex = state.fileOrder.indexOf(state.currentFile);
    if (currentIndex === -1) return;

    const newIndex = currentIndex + direction;
    if (newIndex >= 0 && newIndex < state.fileOrder.length) {
        loadFile(state.fileOrder[newIndex]);
    }
}

// ========================================
// SEARCH FUNCTIONALITY
// ========================================
function initSearch() {
    const searchItems = [];

    for (const category in TIL_DATA.categories) {
        TIL_DATA.categories[category].files.forEach(file => {
            searchItems.push({
                path: file.path,
                title: file.title,
                filename: file.filename,
                category: category,
                content: file.content
            });
        });
    }

    state.searchIndex = new Fuse(searchItems, {
        keys: [
            { name: 'title', weight: 2.0 },
            { name: 'filename', weight: 1.5 },
            { name: 'category', weight: 1.0 },
            { name: 'content', weight: 0.5 }
        ],
        threshold: 0.4,
        includeMatches: true,
        includeScore: true,
        minMatchCharLength: 2
    });

    const searchInput = document.getElementById('search-input');
    let searchTimeout;

    searchInput.addEventListener('input', (e) => {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => {
            performSearch(e.target.value);
        }, 300);
    });

    searchInput.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            searchInput.value = '';
            performSearch('');
            searchInput.blur();
        }
    });
}

function performSearch(query) {
    if (!query.trim()) {
        buildFileList();
        return;
    }

    const results = state.searchIndex.search(query);

    const container = document.getElementById('file-list');
    container.innerHTML = '';

    if (results.length === 0) {
        container.innerHTML = '<div class="loading">검색 결과가 없습니다</div>';
        return;
    }

    const resultHeader = document.createElement('div');
    resultHeader.className = 'search-results-header';
    resultHeader.textContent = `${results.length}개 결과`;
    container.appendChild(resultHeader);

    results.slice(0, 50).forEach(result => {
        const item = result.item;
        const resultDiv = document.createElement('div');
        resultDiv.className = 'search-result-item';

        const titleDiv = document.createElement('div');
        titleDiv.className = 'search-result-title';
        titleDiv.textContent = item.title;

        const metaDiv = document.createElement('div');
        metaDiv.className = 'search-result-meta';
        metaDiv.textContent = `${item.category} / ${item.filename}`;

        resultDiv.appendChild(titleDiv);
        resultDiv.appendChild(metaDiv);

        resultDiv.onclick = () => {
            loadFile(item.path);
            document.getElementById('search-input').value = '';
            buildFileList();
        };

        container.appendChild(resultDiv);
    });
}

// ========================================
// TABLE OF CONTENTS
// ========================================
function generateTOC() {
    const contentDiv = document.getElementById('content');
    const tocDiv = document.getElementById('toc-list');

    const headings = contentDiv.querySelectorAll('h2, h3, h4');

    if (headings.length === 0) {
        tocDiv.innerHTML = '<div style="color: var(--text-secondary); font-size: 0.8rem;">목차 없음</div>';
        return;
    }

    tocDiv.innerHTML = '';

    headings.forEach((heading, index) => {
        if (!heading.id) {
            heading.id = `heading-${index}`;
        }

        const tocItem = document.createElement('a');
        tocItem.className = `toc-item toc-${heading.tagName.toLowerCase()}`;
        tocItem.textContent = heading.textContent;
        tocItem.title = heading.textContent;

        tocItem.onclick = () => {
            heading.scrollIntoView({ behavior: 'smooth', block: 'start' });
        };

        tocDiv.appendChild(tocItem);
    });
}

// ========================================
// THEME MANAGEMENT
// ========================================
function applyTheme(theme) {
    state.currentTheme = theme;
    localStorage.setItem('til-theme', theme);

    document.documentElement.setAttribute('data-theme', theme);

    const button = document.getElementById('theme-toggle');
    if (button) {
        button.textContent = theme === 'dark' ? 'Light' : 'Dark';
    }

    const hljsLight = document.getElementById('hljs-light');
    const hljsDark = document.getElementById('hljs-dark');
    if (hljsLight) hljsLight.disabled = (theme === 'dark');
    if (hljsDark) hljsDark.disabled = (theme === 'light');
}

function toggleTheme() {
    const newTheme = state.currentTheme === 'light' ? 'dark' : 'light';
    applyTheme(newTheme);
}

// ========================================
// CHECKBOX PERSISTENCE
// ========================================
function initCheckboxes() {
    const checkboxes = document.querySelectorAll('input[type="checkbox"]');
    const savedState = JSON.parse(localStorage.getItem('til-checkboxState') || '{}');

    checkboxes.forEach((cb, index) => {
        const key = state.currentFile + '-' + index;
        if (savedState[key]) {
            cb.checked = true;
        }

        cb.addEventListener('change', function() {
            const s = JSON.parse(localStorage.getItem('til-checkboxState') || '{}');
            s[state.currentFile + '-' + index] = this.checked;
            localStorage.setItem('til-checkboxState', JSON.stringify(s));
        });
    });
}

// ========================================
// PROGRESS BAR
// ========================================
function initProgressBar() {
    const contentArea = document.getElementById('content-area');
    if (!contentArea) return;

    contentArea.addEventListener('scroll', function() {
        const scrollTop = this.scrollTop;
        const scrollHeight = this.scrollHeight - this.clientHeight;
        const progress = scrollHeight > 0 ? (scrollTop / scrollHeight) * 100 : 0;
        const fill = document.getElementById('progress-fill');
        if (fill) fill.style.width = progress + '%';
    });
}

// ========================================
// KEYBOARD SHORTCUTS
// ========================================
function initKeyboardShortcuts() {
    document.addEventListener('keydown', (e) => {
        // Close modal on Escape
        if (e.key === 'Escape') {
            const modal = document.getElementById('shortcuts-modal');
            if (modal) modal.classList.remove('show');
        }

        // Don't trigger shortcuts when typing in input
        if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') {
            // Ctrl+K / Cmd+K: Focus search (even in input)
            if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
                e.preventDefault();
                document.getElementById('search-input').focus();
            }
            return;
        }

        // Ctrl+K / Cmd+K: Focus search
        if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
            e.preventDefault();
            document.getElementById('search-input').focus();
            return;
        }

        switch(e.key) {
            case 'ArrowLeft':
                navigateFile(-1);
                break;
            case 'ArrowRight':
                navigateFile(1);
                break;
            case 't':
            case 'T':
                toggleTheme();
                break;
            case 'Home':
                scrollToTop();
                break;
            case 'p':
            case 'P':
                downloadPDF();
                break;
            case 's':
            case 'S':
                toggleSelectMode();
                break;
            case '?':
                showShortcuts();
                break;
        }
    });
}

// ========================================
// PDF DOWNLOAD (window.print() 벡터 방식)
// ========================================
function downloadPDF() {
    var file = findFileByPath(state.currentFile);
    if (!file) return;

    var btn = document.getElementById('pdf-download-btn');
    btn.textContent = '⏳';
    btn.disabled = true;

    generatePDF(file).then(function() {
        btn.textContent = '📥';
        btn.disabled = false;
    });
}

function generatePDF(file) {
    // html2pdf.js로 단일 파일 PDF 생성 — Promise 반환
    var container = document.createElement('div');
    container.className = 'content-inner';
    container.innerHTML = '<div class="batch-doc">' + marked.parse(file.content) + '</div>';
    container.querySelectorAll('pre code').forEach(function(block) {
        hljs.highlightElement(block);
    });
    convertDiagramsToImages(container);

    var wrapper = document.createElement('div');
    wrapper.id = 'pdf-render-wrapper';
    var style = document.createElement('style');
    style.textContent = getPrintCSS() +
        '#pdf-render-wrapper{position:fixed;left:0;top:0;width:100%;height:100%;overflow:auto;background:#fff;z-index:99999;padding:15mm;}';
    wrapper.appendChild(style);
    wrapper.appendChild(container);
    document.body.appendChild(wrapper);

    // 브라우저 렌더링 완료 대기 후 PDF 캡처
    return new Promise(function(resolve) {
        setTimeout(resolve, 500);
    }).then(function() {
        return html2pdf().set({
            margin: 10,
            filename: file.title + '.pdf',
            image: { type: 'jpeg', quality: 0.90 },
            html2canvas: { scale: 1, useCORS: true, logging: false, windowWidth: 800 },
            jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' },
            pagebreak: { mode: ['css', 'legacy'] }
        }).from(container).save();
    }).then(function() {
        wrapper.remove();
    }).catch(function(err) {
        console.error('PDF generation failed:', err);
        wrapper.remove();
    });
}

// ========================================
// DIAGRAM TO PNG CONVERSION
// ========================================
var DIAGRAM_CHARS = /[─│┌┐└┘├┤┬┴┼┏┓┗┛┣┫┳┻╋═║╔╗╚╝╠╣╦╩╬▸▶◀◁△▽→←↑↓↔⇒⇐⇑⇓]/;
var ASCII_DIAGRAM = /[\+\-\|]{4,}/;

function isDiagramBlock(text) {
    return DIAGRAM_CHARS.test(text) || (ASCII_DIAGRAM.test(text) && /\+.*\-.*\+/.test(text));
}

function preToImageDataURL(preEl) {
    var text = preEl.textContent;
    var lines = text.split('\n');
    // 끝 빈줄 제거
    while (lines.length && !lines[lines.length - 1].trim()) lines.pop();
    if (lines.length === 0) return null;

    var canvas = document.createElement('canvas');
    var ctx = canvas.getContext('2d');
    var fontSize = 13;
    var lineHeight = Math.ceil(fontSize * 1.55);
    var font = fontSize + 'px Consolas, Monaco, "Courier New", monospace';
    var padding = 16;
    var dpr = 2;

    // 최대 너비 측정
    ctx.font = font;
    var maxWidth = 0;
    lines.forEach(function(line) {
        var w = ctx.measureText(line).width;
        if (w > maxWidth) maxWidth = w;
    });

    var totalW = maxWidth + padding * 2;
    var totalH = lines.length * lineHeight + padding * 2;
    canvas.width = totalW * dpr;
    canvas.height = totalH * dpr;
    ctx.scale(dpr, dpr);

    // 배경
    ctx.fillStyle = '#f4f4f5';
    ctx.beginPath();
    if (ctx.roundRect) {
        ctx.roundRect(0, 0, totalW, totalH, 8);
    } else {
        ctx.rect(0, 0, totalW, totalH);
    }
    ctx.fill();

    // 테두리
    ctx.strokeStyle = '#dee2e6';
    ctx.lineWidth = 1;
    ctx.beginPath();
    if (ctx.roundRect) {
        ctx.roundRect(0.5, 0.5, totalW - 1, totalH - 1, 8);
    } else {
        ctx.rect(0.5, 0.5, totalW - 1, totalH - 1);
    }
    ctx.stroke();

    // 텍스트 렌더링
    ctx.font = font;
    ctx.fillStyle = '#24292e';
    ctx.textBaseline = 'top';
    lines.forEach(function(line, i) {
        ctx.fillText(line, padding, padding + i * lineHeight);
    });

    return canvas.toDataURL('image/png');
}

function convertDiagramsToImages(container) {
    var preBlocks = container.querySelectorAll('pre');
    preBlocks.forEach(function(pre) {
        if (!isDiagramBlock(pre.textContent)) return;

        var dataUrl = preToImageDataURL(pre);
        if (!dataUrl) return;

        var img = pre.ownerDocument.createElement('img');
        img.src = dataUrl;
        img.style.cssText = 'max-width:100%; height:auto; page-break-inside:avoid; display:block; margin-bottom:16px;';
        pre.parentNode.replaceChild(img, pre);
    });
}

// ========================================
// SELECT MODE & BATCH PDF
// ========================================
function toggleSelectMode() {
    state.selectMode = !state.selectMode;
    if (!state.selectMode) {
        state.selectedFiles.clear();
        removeSelectBar();
    } else {
        showSelectBar();
        // 모바일에서 선택 모드 진입 시 사이드바 열기
        if (window.innerWidth <= 768) {
            openMobileSidebar();
        }
    }
    buildFileList();

    var btn = document.getElementById('select-mode-btn');
    if (btn) btn.textContent = state.selectMode ? '✓' : '☰';
}

function showSelectBar() {
    if (document.getElementById('select-bar')) return;
    var bar = document.createElement('div');
    bar.id = 'select-bar';
    bar.innerHTML = '<span id="select-count">0개 선택</span>' +
        '<button onclick="toggleSelectMode()">취소</button>' +
        '<button id="select-download-btn" onclick="downloadSelectedPDF()" disabled>선택 PDF 다운로드</button>';
    document.body.appendChild(bar);
    var qa = document.getElementById('quick-actions');
    if (qa) qa.classList.add('has-select-bar');
}

function removeSelectBar() {
    var bar = document.getElementById('select-bar');
    if (bar) bar.remove();
    var qa = document.getElementById('quick-actions');
    if (qa) qa.classList.remove('has-select-bar');
}

function updateSelectBar() {
    var count = state.selectedFiles.size;
    var countEl = document.getElementById('select-count');
    if (countEl) countEl.textContent = count + '개 선택';
    var dlBtn = document.getElementById('select-download-btn');
    if (dlBtn) dlBtn.disabled = count === 0;
}

function downloadSelectedPDF() {
    if (state.selectedFiles.size === 0) return;

    var files = [];
    state.fileOrder.forEach(function(path) {
        if (state.selectedFiles.has(path)) {
            var f = findFileByPath(path);
            if (f) files.push(f);
        }
    });
    if (files.length === 0) return;

    var dlBtn = document.getElementById('select-download-btn');
    if (dlBtn) { dlBtn.textContent = '⏳ 0/' + files.length; dlBtn.disabled = true; }

    // 순차 다운로드: 각 파일을 개별 PDF로 저장 (파일 간 1초 간격)
    var chain = Promise.resolve();
    files.forEach(function(file, i) {
        chain = chain.then(function() {
            if (dlBtn) dlBtn.textContent = '⏳ ' + (i + 1) + '/' + files.length;
            return generatePDF(file).then(function() {
                return new Promise(function(resolve) { setTimeout(resolve, 1000); });
            });
        });
    });

    chain.then(function() {
        toggleSelectMode();
        if (state.currentFile) {
            loadFile(state.currentFile);
        }
        if (window.innerWidth <= 768) {
            closeMobileSidebar();
        }
    });
}

// ========================================
// QUICK ACTIONS
// ========================================
function scrollToTop() {
    const contentArea = document.getElementById('content-area');
    if (contentArea) contentArea.scrollTo({ top: 0, behavior: 'smooth' });
}

function showShortcuts() {
    const modal = document.getElementById('shortcuts-modal');
    if (modal) modal.classList.toggle('show');
}

// ========================================
// URL ROUTING
// ========================================
function getFileFromHash() {
    const hash = window.location.hash.slice(1);
    if (!hash) return null;
    return decodeURIComponent(hash);
}

function updateHash(filePath) {
    if (filePath) {
        const newHash = '#' + encodeURIComponent(filePath);
        if (window.location.hash !== newHash) {
            history.pushState(null, '', newHash);
        }
    }
}

function initRouter() {
    window.addEventListener('hashchange', () => {
        const filePath = getFileFromHash();
        if (filePath && filePath !== state.currentFile) {
            loadFile(filePath, { updateUrl: false });
        }
    });

    window.addEventListener('popstate', () => {
        const filePath = getFileFromHash();
        if (filePath) {
            loadFile(filePath, { updateUrl: false });
        }
    });
}

// ========================================
// MOBILE MENU
// ========================================
let mobileSidebarOpen = false;

function openMobileSidebar() {
    const sidebar = document.getElementById('sidebar');
    const overlay = document.getElementById('overlay') || document.getElementById('sidebar-overlay');
    if (sidebar) sidebar.classList.add('open');
    if (overlay) overlay.classList.add('show');
    mobileSidebarOpen = true;
}

function closeMobileSidebar() {
    const sidebar = document.getElementById('sidebar');
    const overlay = document.getElementById('overlay') || document.getElementById('sidebar-overlay');
    if (sidebar) sidebar.classList.remove('open');
    if (overlay) overlay.classList.remove('show');
    mobileSidebarOpen = false;
}

function toggleSidebar() {
    if (mobileSidebarOpen) {
        closeMobileSidebar();
    } else {
        openMobileSidebar();
    }
}

function initMobileMenu() {
    const menuButton = document.getElementById('menu-toggle') || document.getElementById('menu-button');
    if (menuButton) {
        menuButton.addEventListener('click', toggleSidebar);
    }

    const overlay = document.getElementById('overlay') || document.getElementById('sidebar-overlay');
    if (overlay) {
        overlay.addEventListener('click', closeMobileSidebar);
    }
}

// ========================================
// START APPLICATION
// ========================================
document.addEventListener('DOMContentLoaded', init);
