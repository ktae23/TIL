// ========================================
// TIL Viewer Application (Toss Design)
// ========================================
// Note: TIL_DATA is injected inline in the HTML file

// ========================================
// APPLICATION STATE
// ========================================
const state = {
    currentFile: null,
    currentTheme: localStorage.getItem('til-theme') || 'light',
    searchIndex: null,
    collapsedCategories: new Set(),
    currentFilter: 'all',
    fileOrder: []  // flat list of all file paths for arrow nav
};

function sortSubcategories(keys) {
    return keys.sort((a, b) => {
        if (a === 'main') return -1;
        if (b === 'main') return 1;
        return a.localeCompare(b);
    });
}

// ========================================
// INITIALIZATION
// ========================================
function init() {
    applyTheme(state.currentTheme);
    initMermaid();

    // Build flat file order for arrow navigation
    buildFileOrder();

    // 기본적으로 모든 카테고리와 서브카테고리를 닫힌 상태로 설정
    Object.keys(TIL_DATA.categories).forEach(category => {
        state.collapsedCategories.add(category);
        const subs = TIL_DATA.categories[category].subcategories;
        if (subs) {
            Object.keys(subs).forEach(sub => {
                state.collapsedCategories.add(category + '/' + sub);
            });
        }
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
        const catData = TIL_DATA.categories[category];
        catData.files.forEach(file => {
            state.fileOrder.push(file.path);
        });
        if (catData.subcategories) {
            sortSubcategories(Object.keys(catData.subcategories)).forEach(sub => {
                catData.subcategories[sub].files.forEach(file => {
                    state.fileOrder.push(file.path);
                });
            });
        }
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
        const catData = TIL_DATA.categories[category];
        const files = catData.files.filter(
            file => isWithinDays(file.createdAt, state.currentFilter)
        );
        const subcategories = {};
        if (catData.subcategories) {
            for (const sub in catData.subcategories) {
                const subFiles = catData.subcategories[sub].files.filter(
                    file => isWithinDays(file.createdAt, state.currentFilter)
                );
                if (subFiles.length > 0) {
                    subcategories[sub] = { files: subFiles };
                }
            }
        }
        if (files.length > 0 || Object.keys(subcategories).length > 0) {
            filtered[category] = { files, subcategories };
        }
    }
    return filtered;
}

// ========================================
// FILE MANAGEMENT
// ========================================
function countCategoryFiles(categoryData) {
    let count = categoryData.files.length;
    if (categoryData.subcategories) {
        for (const sub in categoryData.subcategories) {
            count += categoryData.subcategories[sub].files.length;
        }
    }
    return count;
}

function buildFileList() {
    const container = document.getElementById('file-list');
    container.innerHTML = '';

    const filteredCategories = getFilteredData();
    const categories = Object.keys(filteredCategories).sort();

    // 필터 적용 시 정보 표시
    if (state.currentFilter !== 'all') {
        const totalFiles = Object.values(filteredCategories)
            .reduce((sum, cat) => sum + countCategoryFiles(cat), 0);
        const filterInfo = document.createElement('div');
        filterInfo.className = 'filter-info';
        filterInfo.textContent = `최근 ${state.currentFilter}일: ${totalFiles}개 문서`;
        container.appendChild(filterInfo);
    }

    categories.forEach(category => {
        const categoryData = filteredCategories[category];
        const isCollapsed = state.collapsedCategories.has(category);
        const totalCount = countCategoryFiles(categoryData);

        const categoryDiv = document.createElement('div');
        categoryDiv.className = 'category';

        const titleDiv = document.createElement('div');
        titleDiv.className = 'category-title';
        titleDiv.innerHTML = `
            <span class="category-arrow ${isCollapsed ? '' : 'expanded'}">&#9654;</span>
            <span>${category}</span>
            <span class="category-count">(${totalCount})</span>
            <span class="category-print-btn" title="${category} 전체 인쇄">&#128424;</span>
        `;
        titleDiv.onclick = (e) => {
            if (e.target.classList.contains('category-print-btn')) {
                e.stopPropagation();
                printCategory(category);
                return;
            }
            toggleCategory(category);
        };

        categoryDiv.appendChild(titleDiv);

        const fileList = document.createElement('div');
        fileList.className = `file-list ${isCollapsed ? 'collapsed' : ''}`;

        // 직속 파일 렌더링
        categoryData.files.forEach(file => {
            const fileItem = document.createElement('div');
            fileItem.className = 'file-item';
            fileItem.textContent = file.title;
            fileItem.title = file.createdAt ? `${file.title} (${file.createdAt})` : file.title;
            fileItem.onclick = () => loadFile(file.path);

            if (state.currentFile === file.path) {
                fileItem.classList.add('active');
            }

            fileList.appendChild(fileItem);
        });

        // 서브카테고리 렌더링
        if (categoryData.subcategories) {
            sortSubcategories(Object.keys(categoryData.subcategories)).forEach(sub => {
                const subData = categoryData.subcategories[sub];
                const subKey = category + '/' + sub;
                const isSubCollapsed = state.collapsedCategories.has(subKey);

                const subDiv = document.createElement('div');
                subDiv.className = 'subcategory';

                const subTitle = document.createElement('div');
                subTitle.className = 'subcategory-title';
                subTitle.innerHTML = `
                    <span class="category-arrow ${isSubCollapsed ? '' : 'expanded'}">&#9654;</span>
                    <span>${sub}</span>
                    <span class="subcategory-count">(${subData.files.length})</span>
                `;
                subTitle.onclick = () => toggleCategory(subKey);
                subDiv.appendChild(subTitle);

                const subFiles = document.createElement('div');
                subFiles.className = `subcategory-files ${isSubCollapsed ? 'collapsed' : ''}`;

                subData.files.forEach(file => {
                    const fileItem = document.createElement('div');
                    fileItem.className = 'file-item';
                    fileItem.textContent = file.title;
                    fileItem.title = file.createdAt ? `${file.title} (${file.createdAt})` : file.title;
                    fileItem.onclick = () => loadFile(file.path);

                    if (state.currentFile === file.path) {
                        fileItem.classList.add('active');
                    }

                    subFiles.appendChild(fileItem);
                });

                subDiv.appendChild(subFiles);
                fileList.appendChild(subDiv);
            });
        }

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
        const catData = TIL_DATA.categories[category];
        const file = catData.files.find(f => f.path === path);
        if (file) return file;
        if (catData.subcategories) {
            for (const sub in catData.subcategories) {
                const subFile = catData.subcategories[sub].files.find(f => f.path === path);
                if (subFile) return subFile;
            }
        }
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

    // 해당 카테고리 + 서브카테고리 자동 펼치기
    const pathParts = filePath.split('/');
    const category = pathParts[0];
    if (state.collapsedCategories.has(category)) {
        state.collapsedCategories.delete(category);
    }
    if (pathParts.length >= 3) {
        const subKey = pathParts[0] + '/' + pathParts[1];
        if (state.collapsedCategories.has(subKey)) {
            state.collapsedCategories.delete(subKey);
        }
    }

    // Render markdown
    const contentDiv = document.getElementById('content');
    contentDiv.innerHTML = marked.parse(file.content);

    // Highlight code blocks (skip mermaid)
    contentDiv.querySelectorAll('pre code').forEach(block => {
        if (!block.classList.contains('language-mermaid')) {
            hljs.highlightElement(block);
        }
    });

    // Render Mermaid diagrams
    renderMermaid(contentDiv);

    // Generate TOC
    generateTOC();

    // Update active state in sidebar
    buildFileList();

    // Scroll to top
    document.getElementById('content-area').scrollTop = 0;

    // Init checkboxes
    initCheckboxes();
    updateDocNav();

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

function updateDocNav() {
    const existing = document.getElementById('doc-nav');
    if (existing) existing.remove();
    if (!state.currentFile) return;

    const idx = state.fileOrder.indexOf(state.currentFile);
    if (idx === -1) return;

    const prevPath = idx > 0 ? state.fileOrder[idx - 1] : null;
    const nextPath = idx < state.fileOrder.length - 1 ? state.fileOrder[idx + 1] : null;
    if (!prevPath && !nextPath) return;

    const nav = document.createElement('nav');
    nav.className = 'doc-nav';
    nav.id = 'doc-nav';

    if (prevPath) {
        const prev = findFileByPath(prevPath);
        const btn = document.createElement('a');
        btn.className = 'doc-nav-btn doc-nav-prev';
        btn.onclick = () => navigateFile(-1);
        btn.innerHTML = '<span class="doc-nav-arrow">&#8592;</span>'
            + '<span class="doc-nav-label">이전</span>'
            + '<span class="doc-nav-title">' + escapeHtml(prev.title) + '</span>';
        nav.appendChild(btn);
    }

    if (nextPath) {
        const next = findFileByPath(nextPath);
        const btn = document.createElement('a');
        btn.className = 'doc-nav-btn doc-nav-next';
        btn.onclick = () => navigateFile(1);
        btn.innerHTML = '<span class="doc-nav-label">다음</span>'
            + '<span class="doc-nav-arrow">&#8594;</span>'
            + '<span class="doc-nav-title">' + escapeHtml(next.title) + '</span>';
        nav.appendChild(btn);
    }

    document.getElementById('content').appendChild(nav);
}

function escapeHtml(text) {
    const d = document.createElement('div');
    d.textContent = text;
    return d.innerHTML;
}

// ========================================
// SEARCH FUNCTIONALITY
// ========================================
function initSearch() {
    const searchItems = [];

    for (const category in TIL_DATA.categories) {
        const catData = TIL_DATA.categories[category];
        catData.files.forEach(file => {
            searchItems.push({
                path: file.path,
                title: file.title,
                filename: file.filename,
                category: category,
                subcategory: null,
                content: file.content
            });
        });
        if (catData.subcategories) {
            for (const sub in catData.subcategories) {
                catData.subcategories[sub].files.forEach(file => {
                    searchItems.push({
                        path: file.path,
                        title: file.title,
                        filename: file.filename,
                        category: category,
                        subcategory: sub,
                        content: file.content
                    });
                });
            }
        }
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
        metaDiv.textContent = item.subcategory
            ? `${item.category} / ${item.subcategory} / ${item.filename}`
            : `${item.category} / ${item.filename}`;

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

    // Re-render Mermaid with new theme
    if (typeof mermaid !== 'undefined') {
        mermaid.initialize({ startOnLoad: false, theme: theme === 'dark' ? 'dark' : 'default' });
        if (state.currentFile) loadFile(state.currentFile);
    }
}

function toggleTheme() {
    const newTheme = state.currentTheme === 'light' ? 'dark' : 'light';
    applyTheme(newTheme);
}

// ========================================
// MERMAID DIAGRAM RENDERING
// ========================================
function initMermaid() {
    if (typeof mermaid === 'undefined') return;
    mermaid.initialize({
        startOnLoad: false,
        theme: state.currentTheme === 'dark' ? 'dark' : 'default'
    });
}

function renderMermaid(container) {
    if (typeof mermaid === 'undefined') return;

    const mermaidBlocks = container.querySelectorAll('pre code.language-mermaid');
    if (mermaidBlocks.length === 0) return;

    mermaidBlocks.forEach(block => {
        const pre = block.parentElement;
        const div = document.createElement('div');
        div.className = 'mermaid';
        div.textContent = block.textContent;
        pre.replaceWith(div);
    });

    mermaid.run();
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
                printDocument();
                break;
            case '?':
                showShortcuts();
                break;
        }
    });
}

// ========================================
// PRINT (window.print 벡터 PDF)
// ========================================
var _originalTitle = null;

function printDocument() {
    if (!state.currentFile) return;
    var file = findFileByPath(state.currentFile);
    if (!file) return;
    _originalTitle = document.title;
    document.title = file.title;
    window.print();
}

var _printQueue = null;

function printCategory(category) {
    var catData = TIL_DATA.categories[category];
    if (!catData) return;

    // 카테고리 내 모든 파일 수집 (직속 + 서브카테고리)
    var files = [];
    catData.files.forEach(function(f) { files.push(f); });
    if (catData.subcategories) {
        sortSubcategories(Object.keys(catData.subcategories)).forEach(function(sub) {
            catData.subcategories[sub].files.forEach(function(f) { files.push(f); });
        });
    }
    if (files.length === 0) return;

    _printQueue = { files: files, idx: 0 };
    _printNext();
}

function _printNext() {
    if (!_printQueue || _printQueue.idx >= _printQueue.files.length) {
        _printQueue = null;
        return;
    }
    var file = _printQueue.files[_printQueue.idx];
    loadFile(file.path);
    setTimeout(function() {
        document.title = file.title;
        window.print();
    }, 300);
}

window.addEventListener('afterprint', function() {
    if (_originalTitle !== null) {
        document.title = _originalTitle;
        _originalTitle = null;
    }
    if (!_printQueue) return;
    _printQueue.idx++;
    if (_printQueue.idx < _printQueue.files.length) {
        setTimeout(_printNext, 500);
    } else {
        _printQueue = null;
    }
});


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

function toggleTOC() {
    const toc = document.getElementById('toc-panel');
    if (toc) toc.classList.toggle('open');
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
