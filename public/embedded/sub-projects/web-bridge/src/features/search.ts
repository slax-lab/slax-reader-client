import search from 'approx-string-match';
import { toRange as toRangePos } from 'dom-anchor-text-position';


function normalizeWithRanges(raw: string) {
  const ranges: { start: number; end: number }[] = [];
  const text = raw.replace(/(\s+)|([^\s])/g, (match, space, _char, offset) => {
    ranges.push({ start: offset, end: offset + match.length });
    return space ? ' ' : match;
  });

  return { text, ranges };
}

export function findBestMatch(text: string, dom?: Element, fuzzy: boolean = true): { element: HTMLElement, match: { start: number, end: number, errors: number } } | null {
    const normalizedText = text.trim().replace(/\s+/g, ' ');
    // 长度允许一定的错误
    const maxErrors = fuzzy ? Math.max(3, Math.floor(normalizedText.length / 3)) : 0;
    
    const result: { candidate: { element: HTMLElement, errors: number, length: number,  match: { start: number, end: number, errors: number } } | null } = { candidate: null };

    function traverse(node: Node) {
        if (node.nodeType === Node.ELEMENT_NODE) {
            const element = node as HTMLElement;
            
            const isJSDOM = typeof navigator !== 'undefined' && navigator.userAgent.includes('jsdom');
            if (!isJSDOM && element.offsetHeight === 0 && element.offsetWidth === 0) {
                return;
            }

            const content = element.textContent;
            const { text, ranges } = normalizeWithRanges(content || '');

            if (content && content.length >= normalizedText.length - maxErrors) {
                const matches = search(text, normalizedText, maxErrors);
                if (matches.length > 0) {
                    matches.sort((a, b) => a.errors - b.errors);
                    const bestMatchInElement = matches[0];
                    
                    if (!result.candidate || 
                        bestMatchInElement.errors < result.candidate.errors || 
                        (bestMatchInElement.errors === result.candidate.errors && content.length <= result.candidate.length)) {
                        
                        const rawStart = ranges[bestMatchInElement.start].start;
                        const rawEnd = ranges[bestMatchInElement.end - 1].end;

                        result.candidate = {
                            element,
                            errors: bestMatchInElement.errors,
                            length: content.length,
                            match: {
                                start: rawStart,
                                end: rawEnd,
                                errors: bestMatchInElement.errors
                            }
                        };
                    }
                } else if (fuzzy) {
                    const regText = normalizedText.replace(/[.*+?^${}()|[\]\\]/g, '\\$&').replace(/\s+/g, "\\s+");
                    const regex = new RegExp(regText, 'i');
                    const exactMatch = content.match(regex);
                    if (exactMatch && exactMatch.index !== undefined) {
                        result.candidate = {
                            element,
                            errors: 0,
                            length: content.length,
                            match: {
                                start: exactMatch.index,
                                end: exactMatch.index + exactMatch[0].length,
                                errors: 0
                            }
                        };
                    }
                }
            }
 
            Array.from(element.children).forEach(child => traverse(child));
        }
    }

    traverse(dom || document.body);
    
    if (result.candidate) {
        console.log(`[WebView Bridge] Fuzzy match found: ${result.candidate.element.tagName} (Lengths: ${normalizedText.length}, Errors: ${result.candidate.errors}, Error Rate: ${(result.candidate.errors / normalizedText.length * 100).toFixed(2)}%， Available Text Length: ${normalizedText.length - result.candidate.errors}）`);
        return { element: result.candidate.element, match: result.candidate.match };
    }
    
    return null;
}

/**
 * 查找单个匹配元素
 */
export function findMatchingElement(anchorText: string): { element: HTMLElement, range: Range | null } | null {
    const normalizedAnchor = anchorText.trim().replace(/\s+/g, ' ');

    try {
        const fuzzyResult = findBestMatch(normalizedAnchor);
        if (fuzzyResult) {
            const { element, match } = fuzzyResult;
            const range = toRangePos(element, { start: match.start, end: match.end });

            if (range) {
                console.log(`[WebView Bridge] Found approximate text match in: ${element.tagName}`);
                let container = range.commonAncestorContainer;
                if (container.nodeType === Node.TEXT_NODE && container.parentNode) {
                    container = container.parentNode;
                }
                return { element: container as HTMLElement || element, range };
            }
        }
    } catch (error) {
        console.warn('[WebView Bridge] Error searching for approximate text:', error);
    }

    console.warn(`[WebView Bridge] No matching element found: ${anchorText}`);
    return null;
}
