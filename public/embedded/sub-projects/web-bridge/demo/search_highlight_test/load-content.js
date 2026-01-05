// Available datasets
const datasets = ['canon', 'notebookllm'];

let currentDataset = 'canon';
let buttons = [];

// Handle URL parameters
const urlParams = new URLSearchParams(window.location.search);
const indexParam = urlParams.get('index');
if (indexParam !== null) {
    const index = parseInt(indexParam, 10);
    if (!isNaN(index) && index >= 0 && index < datasets.length) {
        currentDataset = datasets[index];
    }
}

// Initialize
// Populate select options
const selectEl = document.getElementById('dataset-select');
if (selectEl) {
    selectEl.innerHTML = '';
    datasets.forEach(ds => {
        const option = document.createElement('option');
        option.value = ds;
        option.textContent = ds.charAt(0).toUpperCase() + ds.slice(1); // Capitalize
        selectEl.appendChild(option);
    });
    selectEl.value = currentDataset;
}

loadDataset(currentDataset);

function changeDataset(datasetName) {
    if (datasets.includes(datasetName)) {
        currentDataset = datasetName;
        loadDataset(datasetName);
    }
}

function loadDataset(datasetName) {
    // Clear existing content and buttons
    document.getElementById('content-area').innerHTML = '<div style="text-align: center; padding: 50px; color: #666;">Loading content...</div>';
    const container = document.getElementById('buttons-container');
    container.innerHTML = '';
    buttons = [];

    const basePath = `search_highlight_test/test_data/${datasetName}`;
    const htmlPath = `${basePath}/data.html`;
    const mdPath = `${basePath}/data.md`;

    // Load HTML content
    fetch(htmlPath)
        .then(response => {
            if (!response.ok) throw new Error(`Failed to load HTML: ${response.statusText}`);
            return response.text();
        })
        .then(html => {
            const parser = new DOMParser();
            const doc = parser.parseFromString(html, 'text/html');
            const content = doc.querySelector('.page') || doc.body;
            
            const scripts = content.querySelectorAll('script');
            scripts.forEach(s => s.remove());

            document.getElementById('content-area').innerHTML = content.innerHTML;
            logToConsole(`Content loaded: ${htmlPath}`, 'success');
        })
        .catch(err => {
            console.error(err);
            document.getElementById('content-area').innerHTML = '<p style="color:red">Error loading content: ' + err.message + '</p>';
            logToConsole('Error loading content', 'error');
        });

    // Load MD content and extract anchors
    fetch(mdPath)
        .then(response => {
            if (!response.ok) throw new Error(`Failed to load MD: ${response.statusText}`);
            return response.text();
        })
        .then(md => {
            const anchors = extractAnchors(md);
            anchors.forEach(item => {
                const btn = document.createElement('button');
                btn.className = 'test-btn';
                btn.textContent = `[${item.id}]`;
                btn.title = item.text;
                btn.onclick = () => runTest(item.text);
                btn.dataset.text = item.text;
                container.appendChild(btn);
                buttons.push(btn);
            });
            logToConsole(`Anchors loaded: ${anchors.length}`, 'success');
        })
        .catch(err => {
            console.error(err);
            logToConsole('Error loading anchors: ' + err.message, 'error');
        });
}

function extractAnchors(md) {
    const regex = /\[(\d+)\]\(#([^)]+)\)/g;
    const anchors = [];
    let match;
    while ((match = regex.exec(md)) !== null) {
        anchors.push({
            id: match[1],
            text: match[2]
        });
    }
    return anchors;
}

function clearHighlights() {
    // Assuming the bridge has a clear method or we reload?
    // The bridge API might not have a clear method exposed directly on the bridge instance if it's not defined.
    // But usually search clears previous highlights or we can implement a clear.
    // Let's check if the bridge has a clear method.
    if (window.SlaxWebViewBridge && window.SlaxWebViewBridge.clearHighlight) {
        window.SlaxWebViewBridge.clearHighlight();
        logToConsole('Highlights cleared', 'info');
    } else {
        // Fallback: reload page or remove classes
        document.querySelectorAll('.slax-highlight, .slax-current-highlight').forEach(el => {
            const parent = el.parentNode;
            parent.replaceChild(document.createTextNode(el.textContent), el);
            parent.normalize(); // Merge text nodes
        });
        logToConsole('Highlights cleared (DOM manipulation)', 'info');
    }
}

function checkAnchors() {
    logToConsole('Checking anchors availability...', 'info');
    if (!window.SlaxWebViewBridge) {
        logToConsole('SlaxWebViewBridge not initialized', 'error');
        return;
    }

    let foundCount = 0;
    let missingCount = 0;

    buttons.forEach(btn => {
        const text = btn.dataset.text;
        try {
            const result = window.SlaxWebViewBridge.findMatchingElement(text);
            // Check if result is valid (not null/undefined and if array, length > 0)
            const isValid = result && (Array.isArray(result) ? result.length > 0 : true);
            
            if (isValid) {
                btn.classList.remove('disabled');
                btn.disabled = false;
                foundCount++;
            } else {
                btn.classList.add('disabled');
                btn.disabled = true;
                missingCount++;
            }
        } catch (e) {
            console.error('Error checking anchor:', text, e);
            btn.classList.add('disabled');
            btn.disabled = true;
            missingCount++;
        }
    });

    logToConsole(`Check complete. Found: ${foundCount}, Missing: ${missingCount}`, 'success');
}

function runTest(text) {
    logToConsole(`Searching for: "${text}"`, 'info');
    
    if (!window.SlaxWebViewBridge) {
        logToConsole('SlaxWebViewBridge not initialized', 'error');
        return;
    }

    try {
        // The search method might return a promise or result directly
        // Based on typical bridge implementations, it might be async or sync.
        // Let's assume it returns the result or a promise.
        const result = window.SlaxWebViewBridge.findMatchingElement(text);
        
        if (result instanceof Promise) {
            result.then(handleResult).catch(e => logToConsole('Search error: ' + e, 'error'));
        } else {
            handleResult(result);
        }
    } catch (e) {
        logToConsole('Error running search: ' + e.message, 'error');
        console.error(e);
    }
}

function handleResult(result) {
    console.log('Search result:', result);
    if (result && (Array.isArray(result) ? result.length > 0 : result)) {
        logToConsole(`Found matches. Highlighting and Scrolling...`, 'success');
        window.SlaxWebViewBridge.highlightElement(result);
        // Scroll to the element
        if (window.SlaxWebViewBridge.scrollToElement) {
            // result might be { element, range } or just element
            const element = result.element || result;
            if (element instanceof HTMLElement) {
                window.SlaxWebViewBridge.scrollToElement(element);
                logToConsole('Scrolled to element', 'info');
            }
            // if element is node
            else if (element instanceof Node) {
                const el = element.nodeType === Node.ELEMENT_NODE ? element : (element.parentElement);
                if (el) {
                    window.SlaxWebViewBridge.scrollToElement(el);
                    logToConsole('Scrolled to element (from node)', 'info');
                } else {
                    logToConsole('Could not determine element to scroll to from node', 'warn');
                }
            }
        } else {
            logToConsole('scrollToElement not available on bridge', 'warn');
        }
    } else {
        logToConsole('No results found', 'error');
    }
}
