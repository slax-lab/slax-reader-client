var SlaxReaderWebBridgeExports = (function (exports) {
    'use strict';

    function detectPlatform() {
        // Android 平台：检查 NativeBridge.postMessage 是否存在
        if (window.NativeBridge?.postMessage) {
            return 'android';
        }
        // iOS 平台：检查 webkit.messageHandlers.NativeBridge 是否存在
        if (window.webkit?.messageHandlers?.NativeBridge) {
            return 'ios';
        }
        return 'unknown';
    }

    /**
     * 发送消息给 Native
     */
    function postToNativeBridge(payload) {
        const message = JSON.stringify(payload);
        const platform = detectPlatform();
        // Android 平台
        if (platform === 'android') {
            window.NativeBridge.postMessage(message);
            return true;
        }
        // iOS 平台
        if (platform === 'ios') {
            window.webkit.messageHandlers.NativeBridge.postMessage(message);
            return true;
        }
        console.warn('Native bridge not available');
        return false;
    }

    /**
     * 获取页面内容高度
     */
    function getContentHeight() {
        return Math.max(document.body.scrollHeight, document.body.offsetHeight, document.documentElement.clientHeight, document.documentElement.scrollHeight, document.documentElement.offsetHeight);
    }

    /**
     * 获取图片元素的 URL
     */
    function getImageUrl(element) {
        const tagName = element.tagName.toLowerCase();
        if (tagName === 'img') {
            const img = element;
            return img.currentSrc || img.src || '';
        }
        if (tagName === 'image') {
            const svgImage = element;
            return svgImage.href?.baseVal ||
                element.getAttribute('href') ||
                element.getAttribute('xlink:href') ||
                '';
        }
        return '';
    }
    /**
     * 处理图片加载和样式
     */
    function handleImageLoading(imgs) {
        const loadingKey = 'slax-image-loading';
        imgs.forEach(img => {
            img.srcset = '';
            img.onload = () => {
                img.classList.remove(loadingKey);
                if (img.naturalWidth < 5 || img.naturalHeight < 5) {
                    img.setAttribute('style', 'display: none;');
                    return;
                }
                else if (img.naturalWidth < 200) {
                    img.setAttribute('style', `width: ${img.naturalWidth}px !important;`);
                    return;
                }
                ['padding: 0 !important', 'height: auto !important;'].forEach(style => {
                    img.setAttribute('style', style);
                });
            };
            img.referrerPolicy = '';
            img.onerror = () => {
                img.classList.remove(loadingKey);
                img.style.display = 'none';
            };
            img.classList.add(loadingKey);
            const parentElement = img.parentElement;
            const parentChilds = parentElement ? Array.from(parentElement.childNodes) : [];
            const isOnlyImages = parentChilds.every(child => {
                if (child.nodeType === Node.ELEMENT_NODE) {
                    const element = child;
                    return element.tagName.toLowerCase() === 'img';
                }
                return true;
            });
            if (isOnlyImages) {
                img.style.cssFloat = 'none';
            }
        });
    }
    /**
     * 如果是 tweet 内容，移除包裹 img 的 a 标签（保留子内容）
     */
    function unwrapImgAnchorsInTweet() {
        const firstDiv = document.body?.querySelector(':scope > div');
        if (!firstDiv?.classList.contains('tweet'))
            return;
        document.querySelectorAll('a img').forEach(img => {
            const anchor = img.closest('a');
            if (!anchor)
                return;
            const parent = anchor.parentNode;
            if (!parent)
                return;
            while (anchor.firstChild) {
                parent.insertBefore(anchor.firstChild, anchor);
            }
            parent.removeChild(anchor);
        });
    }
    /**
     * 初始化图片点击处理程序
     */
    function initImageClickHandlers() {
        unwrapImgAnchorsInTweet();
        const images = document.querySelectorAll('img, image');
        const htmlImages = Array.from(images).filter(img => img.tagName.toLowerCase() === 'img');
        handleImageLoading(htmlImages);
        images.forEach(img => {
            img.addEventListener('click', (event) => {
                const validSchemes = ['https://', 'http://', 'slaxstatics://', 'slaxstatic://'];
                const allImageUrls = Array.from(images)
                    .map(getImageUrl)
                    .filter(url => url && validSchemes.some(scheme => url.startsWith(scheme)));
                const currentTarget = event.currentTarget;
                const clickedImageUrl = getImageUrl(currentTarget);
                postToNativeBridge({
                    type: 'imageClick',
                    src: clickedImageUrl,
                    allImages: allImageUrls,
                    index: allImageUrls.indexOf(clickedImageUrl)
                });
            });
        });
        console.log(`[WebView Bridge] Initialized ${images.length} image click handlers`);
    }

    /**
     * 高亮目标元素
     */
    function highlightElement(target) {
        let element = null;
        let range = null;
        if (target instanceof HTMLElement || target === null) {
            element = target;
        }
        else {
            element = target.element || null;
            range = target.range || null;
        }
        if (!element && !range) {
            console.warn('[WebView Bridge] Target element/range does not exist, cannot highlight');
            return;
        }
        try {
            const selection = window.getSelection();
            if (!range && element) {
                range = document.createRange();
                range.selectNodeContents(element);
            }
            if (selection && range) {
                selection.removeAllRanges();
                selection.addRange(range);
            }
        }
        catch (error) {
            console.warn('[WebView Bridge] Failed to select element:', error);
        }
    }

    /**
     * Implementation of Myers' online approximate string matching algorithm [1],
     * with additional optimizations suggested by [2].
     *
     * This has O((k/w) * n) expected-time where `n` is the length of the
     * text, `k` is the maximum number of errors allowed (always <= the pattern
     * length) and `w` is the word size. Because JS only supports bitwise operations
     * on 32 bit integers, `w` is 32.
     *
     * As far as I am aware, there aren't any online algorithms which are
     * significantly better for a wide range of input parameters. The problem can be
     * solved faster using "filter then verify" approaches which first filter out
     * regions of the text that cannot match using a "cheap" check and then verify
     * the remaining potential matches. The verify step requires an algorithm such
     * as this one however.
     *
     * The algorithm's approach is essentially to optimize the classic dynamic
     * programming solution to the problem by computing columns of the matrix in
     * word-sized chunks (ie. dealing with 32 chars of the pattern at a time) and
     * avoiding calculating regions of the matrix where the minimum error count is
     * guaranteed to exceed the input threshold.
     *
     * The paper consists of two parts, the first describes the core algorithm for
     * matching patterns <= the size of a word (implemented by `advanceBlock` here).
     * The second uses the core algorithm as part of a larger block-based algorithm
     * to handle longer patterns.
     *
     * [1] G. Myers, “A Fast Bit-Vector Algorithm for Approximate String Matching
     * Based on Dynamic Programming,” vol. 46, no. 3, pp. 395–415, 1999.
     *
     * [2] Šošić, M. (2014). An simd dynamic programming c/c++ library (Doctoral
     * dissertation, Fakultet Elektrotehnike i računarstva, Sveučilište u Zagrebu).
     */
    function reverse(s) {
        return s.split("").reverse().join("");
    }
    /**
     * Given the ends of approximate matches for `pattern` in `text`, find
     * the start of the matches.
     *
     * @param findEndFn - Function for finding the end of matches in
     * text.
     * @return Matches with the `start` property set.
     */
    function findMatchStarts(text, pattern, matches) {
        const patRev = reverse(pattern);
        return matches.map((m) => {
            // Find start of each match by reversing the pattern and matching segment
            // of text and searching for an approx match with the same number of
            // errors.
            const minStart = Math.max(0, m.end - pattern.length - m.errors);
            const textRev = reverse(text.slice(minStart, m.end));
            // If there are multiple possible start points, choose the one that
            // maximizes the length of the match.
            const start = findMatchEnds(textRev, patRev, m.errors).reduce((min, rm) => {
                if (m.end - rm.end < min) {
                    return m.end - rm.end;
                }
                return min;
            }, m.end);
            return {
                start,
                end: m.end,
                errors: m.errors,
            };
        });
    }
    /**
     * Return 1 if a number is non-zero or zero otherwise, without using
     * conditional operators.
     *
     * This should get inlined into `advanceBlock` below by the JIT.
     *
     * Adapted from https://stackoverflow.com/a/3912218/434243
     */
    function oneIfNotZero(n) {
        return ((n | -n) >> 31) & 1;
    }
    /**
     * Block calculation step of the algorithm.
     *
     * From Fig 8. on p. 408 of [1], additionally optimized to replace conditional
     * checks with bitwise operations as per Section 4.2.3 of [2].
     *
     * @param ctx - The pattern context object
     * @param peq - The `peq` array for the current character (`ctx.peq.get(ch)`)
     * @param b - The block level
     * @param hIn - Horizontal input delta ∈ {1,0,-1}
     * @return Horizontal output delta ∈ {1,0,-1}
     */
    function advanceBlock(ctx, peq, b, hIn) {
        let pV = ctx.P[b];
        let mV = ctx.M[b];
        const hInIsNegative = hIn >>> 31; // 1 if hIn < 0 or 0 otherwise.
        const eq = peq[b] | hInIsNegative;
        // Step 1: Compute horizontal deltas.
        const xV = eq | mV;
        const xH = (((eq & pV) + pV) ^ pV) | eq;
        let pH = mV | ~(xH | pV);
        let mH = pV & xH;
        // Step 2: Update score (value of last row of this block).
        const hOut = oneIfNotZero(pH & ctx.lastRowMask[b]) -
            oneIfNotZero(mH & ctx.lastRowMask[b]);
        // Step 3: Update vertical deltas for use when processing next char.
        pH <<= 1;
        mH <<= 1;
        mH |= hInIsNegative;
        pH |= oneIfNotZero(hIn) - hInIsNegative; // set pH[0] if hIn > 0
        pV = mH | ~(xV | pH);
        mV = pH & xV;
        ctx.P[b] = pV;
        ctx.M[b] = mV;
        return hOut;
    }
    /**
     * Find the ends and error counts for matches of `pattern` in `text`.
     *
     * Only the matches with the lowest error count are reported. Other matches
     * with error counts <= maxErrors are discarded.
     *
     * This is the block-based search algorithm from Fig. 9 on p.410 of [1].
     */
    function findMatchEnds(text, pattern, maxErrors) {
        if (pattern.length === 0) {
            return [];
        }
        // Clamp error count so we can rely on the `maxErrors` and `pattern.length`
        // rows being in the same block below.
        maxErrors = Math.min(maxErrors, pattern.length);
        const matches = [];
        // Word size.
        const w = 32;
        // Index of maximum block level.
        const bMax = Math.ceil(pattern.length / w) - 1;
        // Context used across block calculations.
        const ctx = {
            P: new Uint32Array(bMax + 1),
            M: new Uint32Array(bMax + 1),
            lastRowMask: new Uint32Array(bMax + 1),
        };
        ctx.lastRowMask.fill(1 << 31);
        ctx.lastRowMask[bMax] = 1 << (pattern.length - 1) % w;
        // Dummy "peq" array for chars in the text which do not occur in the pattern.
        const emptyPeq = new Uint32Array(bMax + 1);
        // Map of UTF-16 character code to bit vector indicating positions in the
        // pattern that equal that character.
        const peq = new Map();
        // Version of `peq` that only stores mappings for small characters. This
        // allows faster lookups when iterating through the text because a simple
        // array lookup can be done instead of a hash table lookup.
        const asciiPeq = [];
        for (let i = 0; i < 256; i++) {
            asciiPeq.push(emptyPeq);
        }
        // Calculate `ctx.peq` - a map of character values to bitmasks indicating
        // positions of that character within the pattern, where each bit represents
        // a position in the pattern.
        for (let c = 0; c < pattern.length; c += 1) {
            const val = pattern.charCodeAt(c);
            if (peq.has(val)) {
                // Duplicate char in pattern.
                continue;
            }
            const charPeq = new Uint32Array(bMax + 1);
            peq.set(val, charPeq);
            if (val < asciiPeq.length) {
                asciiPeq[val] = charPeq;
            }
            for (let b = 0; b <= bMax; b += 1) {
                charPeq[b] = 0;
                // Set all the bits where the pattern matches the current char (ch).
                // For indexes beyond the end of the pattern, always set the bit as if the
                // pattern contained a wildcard char in that position.
                for (let r = 0; r < w; r += 1) {
                    const idx = b * w + r;
                    if (idx >= pattern.length) {
                        continue;
                    }
                    const match = pattern.charCodeAt(idx) === val;
                    if (match) {
                        charPeq[b] |= 1 << r;
                    }
                }
            }
        }
        // Index of last-active block level in the column.
        let y = Math.max(0, Math.ceil(maxErrors / w) - 1);
        // Initialize maximum error count at bottom of each block.
        const score = new Uint32Array(bMax + 1);
        for (let b = 0; b <= y; b += 1) {
            score[b] = (b + 1) * w;
        }
        score[bMax] = pattern.length;
        // Initialize vertical deltas for each block.
        for (let b = 0; b <= y; b += 1) {
            ctx.P[b] = -1;
            ctx.M[b] = 0;
        }
        // Process each char of the text, computing the error count for `w` chars of
        // the pattern at a time.
        for (let j = 0; j < text.length; j += 1) {
            // Lookup the bitmask representing the positions of the current char from
            // the text within the pattern.
            const charCode = text.charCodeAt(j);
            let charPeq;
            if (charCode < asciiPeq.length) {
                // Fast array lookup.
                charPeq = asciiPeq[charCode];
            }
            else {
                // Slower hash table lookup.
                charPeq = peq.get(charCode);
                if (typeof charPeq === "undefined") {
                    charPeq = emptyPeq;
                }
            }
            // Calculate error count for blocks that we definitely have to process for
            // this column.
            let carry = 0;
            for (let b = 0; b <= y; b += 1) {
                carry = advanceBlock(ctx, charPeq, b, carry);
                score[b] += carry;
            }
            // Check if we also need to compute an additional block, or if we can reduce
            // the number of blocks processed for the next column.
            if (score[y] - carry <= maxErrors &&
                y < bMax &&
                (charPeq[y + 1] & 1 || carry < 0)) {
                // Error count for bottom block is under threshold, increase the number of
                // blocks processed for this column & next by 1.
                y += 1;
                ctx.P[y] = -1;
                ctx.M[y] = 0;
                let maxBlockScore;
                if (y === bMax) {
                    const remainder = pattern.length % w;
                    maxBlockScore = remainder === 0 ? w : remainder;
                }
                else {
                    maxBlockScore = w;
                }
                score[y] =
                    score[y - 1] +
                        maxBlockScore -
                        carry +
                        advanceBlock(ctx, charPeq, y, carry);
            }
            else {
                // Error count for bottom block exceeds threshold, reduce the number of
                // blocks processed for the next column.
                while (y > 0 && score[y] >= maxErrors + w) {
                    y -= 1;
                }
            }
            // If error count is under threshold, report a match.
            if (y === bMax && score[y] <= maxErrors) {
                if (score[y] < maxErrors) {
                    // Discard any earlier, worse matches.
                    matches.splice(0, matches.length);
                }
                matches.push({
                    start: -1,
                    end: j + 1,
                    errors: score[y],
                });
                // Because `search` only reports the matches with the lowest error count,
                // we can "ratchet down" the max error threshold whenever a match is
                // encountered and thereby save a small amount of work for the remainder
                // of the text.
                maxErrors = score[y];
            }
        }
        return matches;
    }
    /**
     * Search for matches for `pattern` in `text` allowing up to `maxErrors` errors.
     *
     * Returns the start, and end positions and error counts for each lowest-cost
     * match. Only the "best" matches are returned.
     */
    function search(text, pattern, maxErrors) {
        const matches = findMatchEnds(text, pattern, maxErrors);
        return findMatchStarts(text, pattern, matches);
    }

    var lib$1 = {};

    var lib = {};

    var hasRequiredLib$1;

    function requireLib$1 () {
    	if (hasRequiredLib$1) return lib;
    	hasRequiredLib$1 = 1;
    	(function (exports$1) {

    		Object.defineProperty(exports$1, "__esModule", {
    		  value: true
    		});
    		exports$1["default"] = seek;
    		var E_END = 'Iterator exhausted before seek ended.';
    		var E_SHOW = 'Argument 1 of seek must use filter NodeFilter.SHOW_TEXT.';
    		var E_WHERE = 'Argument 2 of seek must be an integer or a Text Node.';
    		var DOCUMENT_POSITION_PRECEDING = 2;
    		var SHOW_TEXT = 4;
    		var TEXT_NODE = 3;

    		function seek(iter, where) {
    		  if (iter.whatToShow !== SHOW_TEXT) {
    		    var error; // istanbul ignore next

    		    try {
    		      error = new DOMException(E_SHOW, 'InvalidStateError');
    		    } catch (_unused) {
    		      error = new Error(E_SHOW);
    		      error.code = 11;
    		      error.name = 'InvalidStateError';

    		      error.toString = function () {
    		        return "InvalidStateError: ".concat(E_SHOW);
    		      };
    		    }

    		    throw error;
    		  }

    		  var count = 0;
    		  var node = iter.referenceNode;
    		  var predicates = null;

    		  if (isInteger(where)) {
    		    predicates = {
    		      forward: function forward() {
    		        return count < where;
    		      },
    		      backward: function backward() {
    		        return count > where || !iter.pointerBeforeReferenceNode;
    		      }
    		    };
    		  } else if (isText(where)) {
    		    var forward = before(node, where) ? function () {
    		      return false;
    		    } : function () {
    		      return node !== where;
    		    };

    		    var backward = function backward() {
    		      return node !== where || !iter.pointerBeforeReferenceNode;
    		    };

    		    predicates = {
    		      forward: forward,
    		      backward: backward
    		    };
    		  } else {
    		    throw new TypeError(E_WHERE);
    		  }

    		  while (predicates.forward()) {
    		    node = iter.nextNode();

    		    if (node === null) {
    		      throw new RangeError(E_END);
    		    }

    		    count += node.nodeValue.length;
    		  }

    		  if (iter.nextNode()) {
    		    node = iter.previousNode();
    		  }

    		  while (predicates.backward()) {
    		    node = iter.previousNode();

    		    if (node === null) {
    		      throw new RangeError(E_END);
    		    }

    		    count -= node.nodeValue.length;
    		  }

    		  if (!isText(iter.referenceNode)) {
    		    throw new RangeError(E_END);
    		  }

    		  return count;
    		}

    		function isInteger(n) {
    		  if (typeof n !== 'number') return false;
    		  return isFinite(n) && Math.floor(n) === n;
    		}

    		function isText(node) {
    		  return node.nodeType === TEXT_NODE;
    		}

    		function before(ref, node) {
    		  return ref.compareDocumentPosition(node) & DOCUMENT_POSITION_PRECEDING;
    		}
    		
    	} (lib));
    	return lib;
    }

    var domSeek;
    var hasRequiredDomSeek;

    function requireDomSeek () {
    	if (hasRequiredDomSeek) return domSeek;
    	hasRequiredDomSeek = 1;
    	domSeek = requireLib$1()['default'];
    	return domSeek;
    }

    var rangeToString = {};

    var hasRequiredRangeToString;

    function requireRangeToString () {
    	if (hasRequiredRangeToString) return rangeToString;
    	hasRequiredRangeToString = 1;
    	(function (exports$1) {

    		Object.defineProperty(exports$1, "__esModule", {
    		  value: true
    		});
    		exports$1["default"] = rangeToString;

    		/**
    		 * Return the next node after `node` in a tree order traversal of the document.
    		 */
    		function nextNode(node, skipChildren) {
    		  if (!skipChildren && node.firstChild) {
    		    return node.firstChild;
    		  }

    		  do {
    		    if (node.nextSibling) {
    		      return node.nextSibling;
    		    }

    		    node = node.parentNode;
    		  } while (node);
    		  /* istanbul ignore next */


    		  return node;
    		}

    		function firstNode(range) {
    		  if (range.startContainer.nodeType === Node.ELEMENT_NODE) {
    		    var node = range.startContainer.childNodes[range.startOffset];
    		    return node || nextNode(range.startContainer, true
    		    /* skip children */
    		    );
    		  }

    		  return range.startContainer;
    		}

    		function firstNodeAfter(range) {
    		  if (range.endContainer.nodeType === Node.ELEMENT_NODE) {
    		    var node = range.endContainer.childNodes[range.endOffset];
    		    return node || nextNode(range.endContainer, true
    		    /* skip children */
    		    );
    		  }

    		  return nextNode(range.endContainer);
    		}

    		function forEachNodeInRange(range, cb) {
    		  var node = firstNode(range);
    		  var pastEnd = firstNodeAfter(range);

    		  while (node !== pastEnd) {
    		    cb(node);
    		    node = nextNode(node);
    		  }
    		}
    		/**
    		 * A ponyfill for Range.toString().
    		 * Spec: https://dom.spec.whatwg.org/#dom-range-stringifier
    		 *
    		 * Works around the buggy Range.toString() implementation in IE and Edge.
    		 * See https://github.com/tilgovi/dom-anchor-text-position/issues/4
    		 */


    		function rangeToString(range) {
    		  // This is a fairly direct translation of the Range.toString() implementation
    		  // in Blink.
    		  var text = '';
    		  forEachNodeInRange(range, function (node) {
    		    if (node.nodeType !== Node.TEXT_NODE) {
    		      return;
    		    }

    		    var start = node === range.startContainer ? range.startOffset : 0;
    		    var end = node === range.endContainer ? range.endOffset : node.textContent.length;
    		    text += node.textContent.slice(start, end);
    		  });
    		  return text;
    		}
    		
    	} (rangeToString));
    	return rangeToString;
    }

    var hasRequiredLib;

    function requireLib () {
    	if (hasRequiredLib) return lib$1;
    	hasRequiredLib = 1;

    	Object.defineProperty(lib$1, "__esModule", {
    	  value: true
    	});
    	lib$1.fromRange = fromRange;
    	lib$1.toRange = toRange;

    	var _domSeek = _interopRequireDefault(requireDomSeek());

    	var _rangeToString = _interopRequireDefault(requireRangeToString());

    	function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

    	var SHOW_TEXT = 4;

    	function fromRange(root, range) {
    	  if (root === undefined) {
    	    throw new Error('missing required parameter "root"');
    	  }

    	  if (range === undefined) {
    	    throw new Error('missing required parameter "range"');
    	  }

    	  var document = root.ownerDocument;
    	  var prefix = document.createRange();
    	  var startNode = range.startContainer;
    	  var startOffset = range.startOffset;
    	  prefix.setStart(root, 0);
    	  prefix.setEnd(startNode, startOffset);
    	  var start = (0, _rangeToString["default"])(prefix).length;
    	  var end = start + (0, _rangeToString["default"])(range).length;
    	  return {
    	    start: start,
    	    end: end
    	  };
    	}

    	function toRange(root) {
    	  var selector = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {};

    	  if (root === undefined) {
    	    throw new Error('missing required parameter "root"');
    	  }

    	  var document = root.ownerDocument;
    	  var range = document.createRange();
    	  var iter = document.createNodeIterator(root, SHOW_TEXT);
    	  var start = selector.start || 0;
    	  var end = selector.end || start;
    	  var startOffset = start - (0, _domSeek["default"])(iter, start);
    	  var startNode = iter.referenceNode;
    	  var remainder = end - start + startOffset;
    	  var endOffset = remainder - (0, _domSeek["default"])(iter, remainder);
    	  var endNode = iter.referenceNode;
    	  range.setStart(startNode, startOffset);
    	  range.setEnd(endNode, endOffset);
    	  return range;
    	}
    	
    	return lib$1;
    }

    var domAnchorTextPosition;
    var hasRequiredDomAnchorTextPosition;

    function requireDomAnchorTextPosition () {
    	if (hasRequiredDomAnchorTextPosition) return domAnchorTextPosition;
    	hasRequiredDomAnchorTextPosition = 1;
    	domAnchorTextPosition = requireLib();
    	return domAnchorTextPosition;
    }

    var domAnchorTextPositionExports = requireDomAnchorTextPosition();

    function normalizeWithRanges(raw) {
        const ranges = [];
        const text = raw.replace(/(\s+)|([^\s])/g, (match, space, _char, offset) => {
            ranges.push({ start: offset, end: offset + match.length });
            return space ? ' ' : match;
        });
        return { text, ranges };
    }
    function findBestMatch(text, dom, fuzzy = true) {
        const normalizedText = text.trim().replace(/\s+/g, ' ');
        // 长度允许一定的错误
        const maxErrors = fuzzy ? Math.max(3, Math.floor(normalizedText.length / 3)) : 0;
        const result = { candidate: null };
        function traverse(node) {
            if (node.nodeType === Node.ELEMENT_NODE) {
                const element = node;
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
                    }
                    else if (fuzzy) {
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
        traverse(document.body);
        if (result.candidate) {
            console.log(`[WebView Bridge] Fuzzy match found: ${result.candidate.element.tagName} (Lengths: ${normalizedText.length}, Errors: ${result.candidate.errors}, Error Rate: ${(result.candidate.errors / normalizedText.length * 100).toFixed(2)}%， Available Text Length: ${normalizedText.length - result.candidate.errors}）`);
            return { element: result.candidate.element, match: result.candidate.match };
        }
        return null;
    }
    /**
     * 查找单个匹配元素
     */
    function findMatchingElement(anchorText) {
        const normalizedAnchor = anchorText.trim().replace(/\s+/g, ' ');
        try {
            const fuzzyResult = findBestMatch(normalizedAnchor);
            if (fuzzyResult) {
                const { element, match } = fuzzyResult;
                const range = domAnchorTextPositionExports.toRange(element, { start: match.start, end: match.end });
                if (range) {
                    console.log(`[WebView Bridge] Found approximate text match in: ${element.tagName}`);
                    let container = range.commonAncestorContainer;
                    if (container.nodeType === Node.TEXT_NODE && container.parentNode) {
                        container = container.parentNode;
                    }
                    return { element: container || element, range };
                }
            }
        }
        catch (error) {
            console.warn('[WebView Bridge] Error searching for approximate text:', error);
        }
        console.warn(`[WebView Bridge] No matching element found: ${anchorText}`);
        return null;
    }

    /**
     * 滚动到指定元素
     */
    function scrollToElement(target) {
        const element = target && 'element' in target ? target.element : target;
        const range = target && 'range' in target ? target.range : null;
        if (!element) {
            console.warn('[WebView Bridge] Target element does not exist, cannot scroll');
            return;
        }
        const platform = detectPlatform();
        if (platform === 'android') {
            const rect = range ? range.getBoundingClientRect() : element.getBoundingClientRect();
            const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
            const elementTop = rect.top + scrollTop;
            const documentHeight = Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);
            postToNativeBridge({
                type: 'scrollToPosition',
                percentage: elementTop / documentHeight
            });
        }
        else if (platform === 'ios') {
            if (range) {
                const rect = range.getBoundingClientRect();
                const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
                const elementTop = rect.top + scrollTop;
                const documentHeight = Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);
                postToNativeBridge({
                    type: 'scrollToPosition',
                    percentage: elementTop / documentHeight
                });
            }
            else {
                element.scrollIntoView({
                    behavior: 'smooth',
                    block: 'center',
                    inline: 'nearest'
                });
            }
        }
    }
    /**
     * 滚动到锚点文本对应的内容
     */
    function scrollToAnchor(anchorText) {
        console.log(`[WebView Bridge] Start finding anchor: ${anchorText}`);
        const decodedAnchor = decodeURIComponent(anchorText);
        const match = findMatchingElement(decodedAnchor);
        if (match) {
            highlightElement(match);
            scrollToElement(match);
            return true;
        }
        else {
            console.warn(`[WebView Bridge] No matching element found: ${anchorText}`);
            return false;
        }
    }

    /**
     * 初始化书签未找到页面的按钮点击处理程序
     */
    function initBookmarkNotFoundHandlers() {
        const container = document.querySelector('body > .slax-reader-notfound-container > .slax-reader-notfound-btn-container');
        if (!container) {
            console.log('[WebView Bridge] Bookmark not found container not present');
            return;
        }
        // 获取重试按钮和反馈按钮
        const retryBtn = container.querySelector('.retry-btn');
        const feedbackBtn = container.querySelector('.feedback-btn');
        // 为重试按钮添加点击事件
        if (retryBtn) {
            retryBtn.addEventListener('click', () => {
                postToNativeBridge({
                    type: 'refreshContent'
                });
                console.log('[WebView Bridge] Bookmark retry button clicked');
            });
        }
        // 为反馈按钮添加点击事件
        if (feedbackBtn) {
            feedbackBtn.addEventListener('click', () => {
                postToNativeBridge({
                    type: 'feedback'
                });
                console.log('[WebView Bridge] Bookmark feedback button clicked');
            });
        }
        console.log('[WebView Bridge] Initialized bookmark not found handlers');
    }

    /**
     * 应用必要的 Polyfills 确保兼容性
     */
    function applyPolyfills() {
        if (!window.CSS || !window.CSS.escape) {
            window.CSS = window.CSS || {};
            window.CSS.escape = function (value) {
                if (arguments.length === 0) {
                    throw new TypeError('`CSS.escape` requires an argument.');
                }
                var string = String(value);
                var length = string.length;
                var index = -1;
                var codeUnit;
                var result = '';
                while (++index < length) {
                    codeUnit = string.charCodeAt(index);
                    // 注意：处理代理对等
                    if (codeUnit === 0x0000) {
                        result += '\uFFFD';
                        continue;
                    }
                    if (
                    // 如果字符是 [0-9A-Za-z_-]
                    (codeUnit >= 0x0030 && codeUnit <= 0x0039) ||
                        (codeUnit >= 0x0041 && codeUnit <= 0x005A) ||
                        (codeUnit >= 0x0061 && codeUnit <= 0x007A) ||
                        codeUnit === 0x005F ||
                        codeUnit === 0x002D) {
                        result += string.charAt(index);
                        continue;
                    }
                    // 转义其他字符
                    result += '\\' + string.charAt(index);
                }
                return result;
            };
        }
    }

    /**
     * 生成UUID
     */
    function generateUUID() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
            const r = (Math.random() * 16) | 0;
            const v = c === 'x' ? r : (r & 0x3) | 0x8;
            return v.toString(16);
        });
    }
    /**
     * 移除外层标签，保留内容
     */
    function removeOuterTag(element) {
        const parent = element.parentNode;
        if (!parent)
            return;
        while (element.firstChild) {
            parent.insertBefore(element.firstChild, element);
        }
        parent.removeChild(element);
    }
    /**
     * 获取元素的CSS选择器路径
     */
    function getElementPath(element, container) {
        const path = [];
        let current = element;
        while (current && current !== container) {
            let selector = current.tagName.toLowerCase();
            // 添加ID
            if (current.id) {
                selector += `#${current.id}`;
            }
            // 添加类名
            if (current.className && typeof current.className === 'string') {
                const classes = current.className.trim().split(/\s+/).filter(c => c);
                if (classes.length > 0) {
                    selector += '.' + classes.join('.');
                }
            }
            // 添加nth-child
            if (current.parentElement) {
                const siblings = Array.from(current.parentElement.children);
                const index = siblings.indexOf(current);
                if (siblings.length > 1) {
                    selector += `:nth-child(${index + 1})`;
                }
            }
            path.unshift(selector);
            current = current.parentElement;
        }
        return path.join(' > ');
    }
    /**
     * 获取元素下的所有文本节点
     *
     * ⚠️ 关键：必须与 mark-renderer 中的 getAllTextNodes 保持完全一致
     */
    function getAllTextNodes(element) {
        const unsupportTags = ['UNSUPPORT-VIDEO', 'SCRIPT', 'STYLE', 'NOSCRIPT'];
        const textNodes = [];
        const traverse = (node) => {
            if (node.nodeType === Node.TEXT_NODE) {
                textNodes.push(node);
            }
            else if (node.nodeType === Node.ELEMENT_NODE &&
                unsupportTags.indexOf(node.tagName) === -1) {
                node.childNodes.forEach((child) => traverse(child));
            }
        };
        traverse(element);
        return textNodes;
    }
    /**
     * 获取Range的文本（包含换行）
     */
    function getRangeTextWithNewlines(range) {
        const selection = window.getSelection();
        if (!selection) {
            const temp = document.createElement('div');
            temp.appendChild(range.cloneContents());
            return temp.innerText;
        }
        const originalRanges = [];
        for (let i = 0; i < selection.rangeCount; i++) {
            originalRanges.push(selection.getRangeAt(i));
        }
        try {
            selection.removeAllRanges();
            selection.addRange(range);
            const text = selection.toString();
            return text;
        }
        finally {
            selection.removeAllRanges();
            for (const originalRange of originalRanges) {
                selection.addRange(originalRange);
            }
        }
    }
    /**
     * 深度比较两个对象是否相等
     */
    function deepEqual(obj1, obj2) {
        if (obj1 === obj2)
            return true;
        if (typeof obj1 !== 'object' || typeof obj2 !== 'object' || obj1 === null || obj2 === null) {
            return false;
        }
        const keys1 = Object.keys(obj1);
        const keys2 = Object.keys(obj2);
        if (keys1.length !== keys2.length)
            return false;
        for (const key of keys1) {
            if (!keys2.includes(key) || !deepEqual(obj1[key], obj2[key])) {
                return false;
            }
        }
        return true;
    }

    /**
     * 选择监听器
     *
     * 负责监听用户的文本选择操作
     */
    class SelectionMonitor {
        constructor(container) {
            this.isMonitoring = false;
            /**
             * 处理选择变化事件（带防抖）
             */
            this.handleSelectionChange = () => {
                if (this.selectionChangeTimeout) {
                    clearTimeout(this.selectionChangeTimeout);
                }
                this.selectionChangeTimeout = setTimeout(() => {
                    const selection = window.getSelection();
                    if (!selection || selection.rangeCount === 0) {
                        this.onSelectionClearedCallback?.();
                        return;
                    }
                    const range = selection.getRangeAt(0);
                    if (range.collapsed) {
                        this.onSelectionClearedCallback?.();
                        return;
                    }
                    if (!this.container.contains(range.commonAncestorContainer)) {
                        return;
                    }
                    const selectionInfo = this.parseSelectionFromRange(range);
                    if (selectionInfo.selection.length === 0) {
                        return;
                    }
                    if (this.onSelectionCallback) {
                        this.onSelectionCallback(selectionInfo);
                    }
                    this.selectionChangeTimeout = undefined;
                }, 300);
            };
            /**
             * 处理鼠标抬起事件（备用方案）
             */
            this.handleMouseUp = (event) => {
                setTimeout(() => {
                    const selection = window.getSelection();
                    if (!selection || selection.rangeCount === 0) {
                        return;
                    }
                    const range = selection.getRangeAt(0);
                    if (range.collapsed) {
                        return;
                    }
                    const selectionInfo = this.parseSelection(range, event);
                    if (selectionInfo.selection.length === 0) {
                        return;
                    }
                    if (this.onSelectionCallback) {
                        this.onSelectionCallback(selectionInfo);
                    }
                }, 10);
            };
            this.container = container;
        }
        /**
         * 开始监听选择
         * @param callback 选区变化时的回调
         * @param onSelectionCleared 选区取消（collapsed 或清空）时的回调
         */
        start(callback, onSelectionCleared) {
            if (this.isMonitoring) {
                return;
            }
            this.onSelectionCallback = callback;
            this.onSelectionClearedCallback = onSelectionCleared;
            // 使用 selectionchange 事件（更适合 Android WebView）
            document.addEventListener('selectionchange', this.handleSelectionChange);
            // 保留 mouseup 和 touchend 作为备用（兼容性）
            this.container.addEventListener('mouseup', this.handleMouseUp);
            this.container.addEventListener('touchend', this.handleMouseUp);
            this.isMonitoring = true;
        }
        /**
         * 停止监听选择
         */
        stop() {
            if (!this.isMonitoring) {
                return;
            }
            if (this.selectionChangeTimeout) {
                clearTimeout(this.selectionChangeTimeout);
                this.selectionChangeTimeout = undefined;
            }
            document.removeEventListener('selectionchange', this.handleSelectionChange);
            this.container.removeEventListener('mouseup', this.handleMouseUp);
            this.container.removeEventListener('touchend', this.handleMouseUp);
            this.isMonitoring = false;
            this.onSelectionCallback = undefined;
            this.onSelectionClearedCallback = undefined;
        }
        /**
         * 从 range 解析选择（不需要事件对象）
         */
        parseSelectionFromRange(range) {
            const selection = this.getSelectionInfo(range);
            const paths = this.convertSelectionToPaths(selection);
            const approx = this.getApproxInfo(range);
            const position = this.getPositionInfoFromRange(range);
            return { selection, paths, approx, position };
        }
        /**
         * 解析选择的内容（带事件对象）
         */
        parseSelection(range, event) {
            const selection = this.getSelectionInfo(range);
            const paths = this.convertSelectionToPaths(selection);
            const approx = this.getApproxInfo(range);
            const position = this.getPositionInfo(range, event);
            return { selection, paths, approx, position };
        }
        /**
         * 从 range 获取位置信息（不需要事件对象）
         */
        getPositionInfoFromRange(range) {
            const rangeRect = range.getBoundingClientRect();
            const containerRect = this.container.getBoundingClientRect();
            const clientX = rangeRect.left + rangeRect.width / 2;
            const clientY = rangeRect.bottom;
            return {
                x: clientX - containerRect.left,
                y: clientY - containerRect.top,
                width: rangeRect.width,
                height: rangeRect.height,
                top: rangeRect.top - containerRect.top,
                left: rangeRect.left - containerRect.left,
                right: rangeRect.right - containerRect.left,
                bottom: rangeRect.bottom - containerRect.top
            };
        }
        /**
         * 获取位置信息（用于显示菜单）
         */
        getPositionInfo(range, event) {
            const rangeRect = range.getBoundingClientRect();
            const containerRect = this.container.getBoundingClientRect();
            let clientX;
            let clientY;
            if (event instanceof MouseEvent) {
                clientX = event.clientX;
                clientY = event.clientY;
            }
            else {
                clientX = event.changedTouches[0].clientX;
                clientY = event.changedTouches[0].clientY;
            }
            return {
                x: clientX - containerRect.left,
                y: clientY - containerRect.top,
                width: rangeRect.width,
                height: rangeRect.height,
                top: rangeRect.top - containerRect.top,
                left: rangeRect.left - containerRect.left,
                right: rangeRect.right - containerRect.left,
                bottom: rangeRect.bottom - containerRect.top
            };
        }
        /**
         * 获取选择信息
         */
        getSelectionInfo(range) {
            if (!range) {
                return [];
            }
            const selectedInfo = [];
            const isNodeFullyInRange = (node) => {
                const nodeRange = document.createRange();
                nodeRange.selectNodeContents(node);
                return (range.compareBoundaryPoints(Range.START_TO_START, nodeRange) <= 0 &&
                    range.compareBoundaryPoints(Range.END_TO_END, nodeRange) >= 0);
            };
            const isNodePartiallyInRange = (node) => range.intersectsNode(node);
            const processTextNode = (textNode) => {
                if (!isNodePartiallyInRange(textNode))
                    return;
                let startOffset = textNode === range.startContainer ? range.startOffset : 0;
                let endOffset = textNode === range.endContainer ? range.endOffset : textNode.length;
                startOffset = Math.max(0, Math.min(startOffset, textNode.length));
                endOffset = Math.max(startOffset, Math.min(endOffset, textNode.length));
                if (endOffset > startOffset) {
                    selectedInfo.push({
                        type: 'text',
                        node: textNode,
                        startOffset,
                        endOffset,
                        text: textNode.textContent.slice(startOffset, endOffset)
                    });
                }
            };
            const processNode = (node) => {
                if (node.nodeType === Node.TEXT_NODE && (node.textContent?.trim() || '').length > 0) {
                    processTextNode(node);
                }
                else if (node.nodeType === Node.ELEMENT_NODE) {
                    const element = node;
                    if (element.tagName === 'IMG' && isNodeFullyInRange(element)) {
                        selectedInfo.push({
                            type: 'image',
                            src: element.src,
                            element: element
                        });
                    }
                    if (isNodePartiallyInRange(element)) {
                        for (const child of Array.from(element.childNodes))
                            processNode(child);
                    }
                }
            };
            processNode(range.commonAncestorContainer);
            return selectedInfo.length > 0 &&
                !selectedInfo.every((item) => item.type === 'text' && item.text.trim().length === 0)
                ? selectedInfo
                : [];
        }
        /**
         * 将选择信息转换为路径
         */
        convertSelectionToPaths(selection) {
            const paths = [];
            let currentPath = null;
            let currentStart = 0;
            let currentEnd = 0;
            for (const item of selection) {
                if (item.type === 'text') {
                    let parent = item.node.parentElement;
                    while (parent && parent.tagName === 'SLAX-MARK') {
                        parent = parent.parentElement;
                    }
                    if (!parent)
                        continue;
                    const path = getElementPath(parent, this.container);
                    // 使用与 mark-renderer 相同的逻辑计算文本节点偏移
                    const allTextNodes = getAllTextNodes(parent);
                    let offset = 0;
                    for (const textNode of allTextNodes) {
                        if (textNode === item.node) {
                            break;
                        }
                        offset += (textNode.textContent || '').length;
                    }
                    const start = offset + item.startOffset;
                    const end = offset + item.endOffset;
                    if (path === currentPath) {
                        currentEnd = end;
                    }
                    else {
                        if (currentPath !== null) {
                            paths.push({ type: 'text', path: currentPath, start: currentStart, end: currentEnd });
                        }
                        currentPath = path;
                        currentStart = start;
                        currentEnd = end;
                    }
                }
                else if (item.type === 'image') {
                    if (currentPath !== null) {
                        paths.push({ type: 'text', path: currentPath, start: currentStart, end: currentEnd });
                        currentPath = null;
                    }
                    const path = getElementPath(item.element, this.container);
                    paths.push({ type: 'image', path });
                }
            }
            if (currentPath !== null) {
                paths.push({ type: 'text', path: currentPath, start: currentStart, end: currentEnd });
            }
            return paths;
        }
        /**
         * 获取近似匹配信息
         */
        getApproxInfo(range) {
            const exact = getRangeTextWithNewlines(range);
            const prefixRange = document.createRange();
            prefixRange.setStart(this.container, 0);
            prefixRange.setEnd(range.startContainer, range.startOffset);
            const fullPrefix = getRangeTextWithNewlines(prefixRange);
            const prefix = fullPrefix.slice(-50);
            const suffixRange = document.createRange();
            suffixRange.setStart(range.endContainer, range.endOffset);
            suffixRange.setEndAfter(this.container.lastChild);
            const fullSuffix = getRangeTextWithNewlines(suffixRange);
            const suffix = fullSuffix.slice(0, 50);
            return { exact, prefix, suffix, raw_text: exact };
        }
        /**
         * 清除选择
         */
        clearSelection() {
            const selection = window.getSelection();
            if (selection) {
                selection.removeAllRanges();
            }
        }
    }

    /**
     * 标记渲染器
     *
     * 负责在页面上绘制、更新和删除标记（划线和评论）
     */
    class MarkRenderer {
        constructor(container, currentUserId, onMarkTap) {
            this.container = container;
            this.currentUserId = currentUserId;
            this.onMarkTap = onMarkTap;
        }
        /**
         * 根据 MarkPathItem 绘制标记
         */
        drawMark(id, paths, isStroke, hasComment, userId) {
            try {
                const isSelfStroke = userId !== undefined && userId === this.currentUserId && isStroke;
                const baseInfo = {
                    id,
                    isStroke,
                    hasComment,
                    isSelfStroke,
                    isHighlighted: false
                };
                let drawMarkSuccess = false;
                for (const markItem of paths) {
                    if (!isStroke && !hasComment)
                        continue;
                    const infos = this.transferNodeInfos(markItem);
                    for (const infoItem of infos) {
                        if (infoItem.type === 'image') {
                            this.addImageMark({ ...baseInfo, ele: infoItem.ele });
                            continue;
                        }
                        this.addMark({ ...baseInfo, node: infoItem.node, start: infoItem.start, end: infoItem.end });
                    }
                    drawMarkSuccess = infos.length > 0;
                }
                return drawMarkSuccess;
            }
            catch (error) {
                console.error('Failed to draw mark:', error);
                return false;
            }
        }
        /**
         * 将标记路径项转换为节点信息
         */
        transferNodeInfos(markItem) {
            const infos = [];
            if (markItem.type === 'text') {
                const baseElement = this.container.querySelector(markItem.path);
                if (!baseElement) {
                    return infos;
                }
                const nodes = getAllTextNodes(baseElement);
                const nodeLengths = nodes.map((node) => (node.textContent || '').length);
                let startOffset = markItem.start || 0;
                const endOffset = markItem.end || 0;
                let base = 0;
                for (let i = 0; i < nodeLengths.length; i++) {
                    if (base + nodeLengths[i] <= startOffset) {
                        base += nodeLengths[i];
                        continue;
                    }
                    if (endOffset - base <= nodeLengths[i]) {
                        infos.push({ type: 'text', start: startOffset - base, end: endOffset - base, node: nodes[i] });
                        break;
                    }
                    else {
                        infos.push({ type: 'text', start: startOffset - base, end: nodeLengths[i], node: nodes[i] });
                        startOffset += nodeLengths[i] - (startOffset - base);
                        base += nodeLengths[i];
                    }
                }
            }
            else if (markItem.type === 'image') {
                let element = this.container.querySelector(markItem.path);
                if (!element || !element.src) {
                    // 尝试在slax-mark标签内查找
                    const paths = markItem.path.split('>');
                    const tailIdx = paths.length - 1;
                    const newPath = [...paths.slice(0, tailIdx), ' slax-mark ', paths[tailIdx]];
                    element = this.container.querySelector(newPath.join('>'));
                }
                if (element) {
                    infos.push({ type: 'image', ele: element });
                }
            }
            return infos;
        }
        /**
         * 在文本节点上添加标记
         */
        addMark(info) {
            const { id, node, start, end, isStroke, hasComment, isSelfStroke, isHighlighted } = info;
            const range = document.createRange();
            range.setStart(node, start);
            range.setEnd(node, end);
            const mark = document.createElement('slax-mark');
            mark.dataset.uuid = id;
            if (isStroke)
                mark.classList.add('stroke');
            if (hasComment)
                mark.classList.add('comment');
            if (isSelfStroke)
                mark.classList.add('self-stroke');
            if (isHighlighted)
                mark.classList.add('highlighted');
            // 直接在元素上绑定 touchend，避免容器级别委托时 event.target 不可靠的问题
            if (this.onMarkTap) {
                const tapCallback = this.onMarkTap;
                mark.addEventListener('touchend', (e) => tapCallback(id, e));
            }
            try {
                range.surroundContents(mark);
            }
            catch (error) {
                console.error('Failed to surround contents:', error);
            }
        }
        /**
         * 添加图片标记
         */
        addImageMark(info) {
            const { id, ele, isStroke, hasComment, isSelfStroke, isHighlighted } = info;
            const mark = document.createElement('slax-mark');
            mark.dataset.uuid = id;
            if (isStroke)
                mark.classList.add('stroke');
            if (hasComment)
                mark.classList.add('comment');
            if (isSelfStroke)
                mark.classList.add('self-stroke');
            if (isHighlighted)
                mark.classList.add('highlighted');
            // 直接在元素上绑定 touchend
            if (this.onMarkTap) {
                const tapCallback = this.onMarkTap;
                mark.addEventListener('touchend', (e) => tapCallback(id, e));
            }
            ele.parentElement?.insertBefore(mark, ele);
            ele.remove();
            mark.appendChild(ele);
        }
        /**
         * 更新标记
         */
        updateMark(id, isStroke, hasComment, userId) {
            const marks = Array.from(this.container.querySelectorAll(`slax-mark[data-uuid="${id}"]`));
            const isSelfStroke = userId !== undefined && userId === this.currentUserId;
            marks.forEach((mark) => {
                if (isStroke) {
                    mark.classList.add('stroke');
                }
                else {
                    mark.classList.remove('stroke');
                }
                if (hasComment) {
                    mark.classList.add('comment');
                }
                else {
                    mark.classList.remove('comment');
                }
                if (isSelfStroke) {
                    mark.classList.add('self-stroke');
                }
                else {
                    mark.classList.remove('self-stroke');
                }
                // 如果既没有划线也没有评论，删除标记
                if (!isStroke && !hasComment) {
                    removeOuterTag(mark);
                }
            });
        }
        /**
         * 删除标记
         */
        removeMark(id) {
            const marks = Array.from(this.container.querySelectorAll(`slax-mark[data-uuid="${id}"]`));
            marks.forEach((mark) => removeOuterTag(mark));
        }
        /**
         * 高亮标记
         */
        highlightMark(id) {
            this.clearAllHighlights();
            const marks = Array.from(this.container.querySelectorAll(`slax-mark[data-uuid="${id}"]`));
            marks.forEach((mark) => mark.classList.add('highlighted'));
            if (marks.length > 0) {
                marks[0].scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
        }
        /**
         * 清除所有高亮
         */
        clearAllHighlights() {
            const marks = Array.from(this.container.querySelectorAll('slax-mark.highlighted'));
            marks.forEach((mark) => mark.classList.remove('highlighted'));
        }
        /**
         * 清除所有标记
         */
        clearAllMarks() {
            const marks = Array.from(this.container.querySelectorAll('slax-mark'));
            marks.forEach((mark) => removeOuterTag(mark));
        }
        /**
         * 获取所有标记ID
         */
        getAllMarkIds() {
            const marks = Array.from(this.container.querySelectorAll('slax-mark[data-uuid]'));
            const ids = new Set();
            marks.forEach((mark) => {
                const id = mark.dataset.uuid;
                if (id)
                    ids.add(id);
            });
            return Array.from(ids);
        }
    }

    /**
     * 标记管理器
     *
     * 负责处理后端 MarkDetail 数据的预处理、分组和渲染
     */
    class MarkManager {
        /** 获取当前选中区域对应的 MarkItemInfo */
        get currentMarkItemInfo() {
            return this._currentMarkItemInfo;
        }
        constructor(container, currentUserId, onMarkTap, onSelectionMarkInfoChange) {
            this.markItemInfos = [];
            /** 当前选中区域对应的 MarkItemInfo（选区存在时有值，取消选区后为 null） */
            this._currentMarkItemInfo = null;
            this.container = container;
            this.renderer = new MarkRenderer(container, currentUserId, onMarkTap);
            this.onSelectionMarkInfoChange = onSelectionMarkInfoChange;
        }
        /**
         * 根据当前选区检测对应的 MarkItemInfo
         *
         * 解析选区的 paths，如果与已有的 MarkItemInfo 的 source 完全匹配，
         * 则返回该 MarkItemInfo；否则创建一个临时包装的 MarkItemInfo。
         * 同时更新 currentMarkItemInfo 并触发回调。
         *
         * @param paths 当前选区解析出的 MarkPathItem 数组
         * @param approx 当前选区的近似匹配信息
         */
        detectSelectionMarkItemInfo(paths, approx) {
            if (paths.length === 0)
                return;
            // 选区未变化时跳过，避免 selectionchange 事件重复触发导致回调被反复调用
            if (this._currentMarkItemInfo && this.checkMarkSourceIsSame(this._currentMarkItemInfo.source, paths)) {
                return;
            }
            const existing = this.markItemInfos.find((info) => this.checkMarkSourceIsSame(info.source, paths));
            const markItemInfo = existing ?? {
                id: '',
                source: paths,
                comments: [],
                stroke: [],
                approx
            };
            this._currentMarkItemInfo = markItemInfo;
            this.onSelectionMarkInfoChange?.(markItemInfo);
        }
        /**
         * 清除当前选区对应的 MarkItemInfo（取消选区时调用，不触发回调）
         */
        clearCurrentMarkItemInfo() {
            this._currentMarkItemInfo = null;
        }
        /**
         * 绘制多个标记
         *
         * @param marks 后端返回的 MarkDetail 数据
         * @returns 键值对：uuid -> 该uuid对应的后端mark列表
         */
        drawMarks(marks) {
            const userMap = this.createUserMap(marks.user_list);
            const commentMap = this.buildCommentMap(marks.mark_list, userMap);
            this.buildCommentRelationships(marks.mark_list, commentMap);
            this.markItemInfos = this.generateMarkItemInfos(marks.mark_list, commentMap);
            for (const info of this.markItemInfos) {
                this.drawSingleMarkItem(info);
            }
            return this.buildDrawMarksResult(marks.mark_list);
        }
        /**
         * 根据 UUID 删除标记
         */
        removeMarkByUuid(uuid) {
            this.renderer.removeMark(uuid);
            this.markItemInfos = this.markItemInfos.filter((info) => info.id !== uuid);
        }
        /**
         * 根据本地 UUID 获取 MarkItemInfo
         */
        getMarkItemInfoByUuid(uuid) {
            return this.markItemInfos.find((info) => info.id === uuid) ?? null;
        }
        /**
         * 清除所有标记
         */
        clearAllMarks() {
            this.renderer.clearAllMarks();
            this.markItemInfos = [];
        }
        /**
         * 根据 UUID 为指定用户添加划线
         *
         * 在对应 MarkItemInfo 的 stroke 数组中追加一条记录，并重新渲染该标记的 DOM 样式。
         * 如果该用户已有划线，则跳过（幂等）。
         *
         * @param uuid MarkItemInfo 的本地 UUID
         * @param userId 执行划线的用户ID
         * @returns 是否成功添加（false 表示 uuid 不存在或用户已有划线）
         */
        addStrokeByUuid(uuid, userId) {
            const infoItem = this.markItemInfos.find((info) => info.id === uuid);
            if (!infoItem) {
                console.warn('[MarkManager] addStrokeByUuid 未找到对应的 MarkItemInfo，uuid:', uuid);
                return false;
            }
            const alreadyStroked = infoItem.stroke.some((s) => s.userId === userId);
            if (alreadyStroked) {
                console.log('[MarkManager] addStrokeByUuid 用户已有划线，跳过，uuid:', uuid, 'userId:', userId);
                return false;
            }
            infoItem.stroke.push({ mark_id: undefined, userId });
            this.updateMarkItemUI(infoItem);
            console.log('[MarkManager] addStrokeByUuid 成功，uuid:', uuid, 'userId:', userId);
            return true;
        }
        /**
         * 通过 source 添加划线（用于临时选区场景）
         *
         * 当 markItemInfo 是临时的（id 为空、未被 markItemInfos 持有）时使用此方法。
         * 流程为：
         * 1. 根据 source 检查 markItemInfos 中是否已有匹配项
         * 2. 若没有则创建新的 MarkItemInfo 并 push 到 markItemInfos
         * 3. 在对应 MarkItemInfo 中插入划线记录
         * 4. 渲染 DOM 样式
         * 5. 返回 uuid 和接口所需的 source/select_content/approx_source 数据
         *
         * @param source MarkPathItem 数组（从临时 markItemInfo 中获取）
         * @param userId 执行划线的用户ID
         * @param approx 近似位置信息（可选，为空时自动从 source 生成）
         * @returns 包含 uuid 及接口入参的数据，失败返回 null
         */
        addStrokeBySource(source, userId, approx) {
            if (!source || source.length === 0) {
                console.warn('[MarkManager] addStrokeBySource 终止：source 为空');
                return null;
            }
            // 若 approx 为空，根据 source 定位 DOM 元素重新生成
            let resolvedApprox = approx;
            if (!resolvedApprox) {
                resolvedApprox = this.buildApproxFromSource(source);
                if (resolvedApprox) {
                    console.log('[MarkManager] addStrokeBySource 根据 source 生成了 approx:', resolvedApprox);
                }
            }
            // 1. 检查是否已有匹配的 MarkItemInfo
            let infoItem = this.markItemInfos.find((info) => this.checkMarkSourceIsSame(info.source, source));
            // 2. 没有则创建新的
            if (!infoItem) {
                const uuid = generateUUID();
                infoItem = {
                    id: uuid,
                    source,
                    stroke: [],
                    comments: [],
                    approx: resolvedApprox
                };
                this.markItemInfos.push(infoItem);
                console.log('[MarkManager] addStrokeBySource 创建新 MarkItemInfo，uuid:', uuid);
            }
            else {
                console.log('[MarkManager] addStrokeBySource 命中已有 MarkItemInfo，uuid:', infoItem.id);
            }
            // 3. 幂等检查后插入划线记录
            const alreadyStroked = infoItem.stroke.some((s) => s.userId === userId);
            if (!alreadyStroked) {
                infoItem.stroke.push({ mark_id: undefined, userId });
            }
            // 4. 渲染 DOM
            this.drawSingleMarkItem(infoItem);
            // 5. 构造返回数据
            const apiSource = this.convertToApiSource(source);
            const selectContent = resolvedApprox?.raw_text
                ? [{ type: 'text', text: resolvedApprox.raw_text, src: '' }]
                : [{ type: 'text', text: resolvedApprox?.exact ?? '', src: '' }];
            const result = {
                uuid: infoItem.id,
                source: apiSource,
                select_content: selectContent,
                approx_source: resolvedApprox ? {
                    exact: resolvedApprox.exact,
                    prefix: resolvedApprox.prefix,
                    suffix: resolvedApprox.suffix,
                    position_start: 0,
                    position_end: resolvedApprox.exact.length
                } : undefined
            };
            console.log('[MarkManager] addStrokeBySource 完成，返回:', result);
            return result;
        }
        /**
         * 根据 UUID 删除指定用户的划线
         *
         * 从对应 MarkItemInfo 的 stroke 数组中移除该用户的记录，并重新渲染 DOM 样式。
         * 如果移除后 stroke 和 comments 均为空，则整体删除该标记。
         *
         * @param uuid MarkItemInfo 的本地 UUID
         * @param userId 要删除划线的用户ID
         * @returns 是否成功删除（false 表示 uuid 不存在或该用户无划线）
         */
        removeStrokeByUuid(uuid, userId) {
            const infoItem = this.markItemInfos.find((info) => info.id === uuid);
            if (!infoItem) {
                console.warn('[MarkManager] removeStrokeByUuid 未找到对应的 MarkItemInfo，uuid:', uuid);
                return false;
            }
            const strokeIndex = infoItem.stroke.findIndex((s) => s.userId === userId);
            if (strokeIndex === -1) {
                console.log('[MarkManager] removeStrokeByUuid 该用户无划线，跳过，uuid:', uuid, 'userId:', userId);
                return false;
            }
            infoItem.stroke.splice(strokeIndex, 1);
            // 划线和评论都为空时，整体删除该标记
            if (infoItem.stroke.length === 0 && infoItem.comments.length === 0) {
                this.removeMarkByUuid(uuid);
                console.log('[MarkManager] removeStrokeByUuid 标记已无划线和评论，整体删除，uuid:', uuid);
            }
            else {
                this.updateMarkItemUI(infoItem);
                console.log('[MarkManager] removeStrokeByUuid 成功，uuid:', uuid, 'userId:', userId);
            }
            return true;
        }
        /**
         * 根据 UUID 添加评论
         *
         * 在对应 MarkItemInfo 的 comments 数组中追加一条评论记录，并重新渲染 DOM 样式。
         *
         * @param uuid MarkItemInfo 的本地 UUID
         * @param params 评论参数对象
         * @param params.userId 发表评论的用户ID
         * @param params.comment 评论内容
         * @param params.username 用户名（用于即时展示）
         * @param params.avatar 用户头像URL（用于即时展示）
         * @returns 是否成功添加（false 表示 uuid 不存在）
         */
        addCommentByUuid(uuid, params) {
            const infoItem = this.markItemInfos.find((info) => info.id === uuid);
            if (!infoItem) {
                console.warn('[MarkManager] addCommentByUuid 未找到对应的 MarkItemInfo，uuid:', uuid);
                return false;
            }
            const commentInfo = {
                markId: 0,
                comment: params.comment,
                userId: params.userId,
                username: params.username ?? '',
                avatar: params.avatar ?? '',
                isDeleted: false,
                children: [],
                createdAt: new Date(),
                showInput: false,
                loading: false,
                operateLoading: false
            };
            infoItem.comments.push(commentInfo);
            this.updateMarkItemUI(infoItem);
            console.log('[MarkManager] addCommentByUuid 成功，uuid:', uuid, 'userId:', params.userId);
            return true;
        }
        /**
         * 通过 source 添加评论（用于临时选区场景）
         *
         * 当 markItemInfo 是临时的（id 为空、未被 markItemInfos 持有）时使用此方法。
         * 流程为：
         * 1. 根据 source 检查 markItemInfos 中是否已有匹配项
         * 2. 若没有则创建新的 MarkItemInfo 并 push 到 markItemInfos
         * 3. 在对应 MarkItemInfo 中插入评论记录
         * 4. 渲染 DOM 样式
         * 5. 返回 uuid 和接口所需的 source/select_content/approx_source 数据
         *
         * @param source MarkPathItem 数组（从临时 markItemInfo 中获取）
         * @param commentParams 评论参数
         * @param approx 近似位置信息（可选，从临时 markItemInfo 中获取）
         * @returns 包含 uuid 及接口入参的数据，失败返回 null
         */
        addCommentBySource(source, commentParams, approx) {
            if (!source || source.length === 0) {
                console.warn('[MarkManager] addCommentBySource 终止：source 为空');
                return null;
            }
            // 若 approx 为空，根据 source 定位 DOM 元素重新生成
            let resolvedApprox = approx;
            if (!resolvedApprox) {
                resolvedApprox = this.buildApproxFromSource(source);
                if (resolvedApprox) {
                    console.log('[MarkManager] addCommentBySource 根据 source 生成了 approx:', resolvedApprox);
                }
            }
            // 1. 检查是否已有匹配的 MarkItemInfo
            let infoItem = this.markItemInfos.find((info) => this.checkMarkSourceIsSame(info.source, source));
            // 2. 没有则创建新的
            if (!infoItem) {
                const uuid = generateUUID();
                infoItem = {
                    id: uuid,
                    source,
                    stroke: [],
                    comments: [],
                    approx: resolvedApprox
                };
                this.markItemInfos.push(infoItem);
                console.log('[MarkManager] addCommentBySource 创建新 MarkItemInfo，uuid:', uuid);
            }
            else {
                console.log('[MarkManager] addCommentBySource 命中已有 MarkItemInfo，uuid:', infoItem.id);
            }
            // 3. 插入评论记录
            const commentInfo = {
                markId: 0,
                comment: commentParams.comment,
                userId: commentParams.userId,
                username: commentParams.username ?? '',
                avatar: commentParams.avatar ?? '',
                isDeleted: false,
                children: [],
                createdAt: new Date(),
                showInput: false,
                loading: false,
                operateLoading: false
            };
            infoItem.comments.push(commentInfo);
            // 4. 渲染 DOM
            this.drawSingleMarkItem(infoItem);
            // 5. 构造返回数据
            const apiSource = this.convertToApiSource(source);
            const selectContent = resolvedApprox?.raw_text
                ? [{ type: 'text', text: resolvedApprox.raw_text, src: '' }]
                : [{ type: 'text', text: resolvedApprox?.exact ?? '', src: '' }];
            const result = {
                uuid: infoItem.id,
                source: apiSource,
                select_content: selectContent,
                approx_source: resolvedApprox ? {
                    exact: resolvedApprox.exact,
                    prefix: resolvedApprox.prefix,
                    suffix: resolvedApprox.suffix,
                    position_start: 0,
                    position_end: resolvedApprox.exact.length
                } : undefined
            };
            console.log('[MarkManager] addCommentBySource 完成，返回:', result);
            return result;
        }
        /**
         * 更新单个 MarkItemInfo 对应的 DOM 样式
         *
         * 根据当前 stroke 和 comments 的状态，通过 renderer.updateMark 刷新 slax-mark 的 CSS class。
         */
        updateMarkItemUI(info) {
            const hasStroke = info.stroke.length > 0;
            const hasComment = info.comments.length > 0;
            const userId = info.stroke.length > 0 ? info.stroke[0].userId : info.comments[0]?.userId;
            this.renderer.updateMark(info.id, hasStroke, hasComment, userId);
        }
        /**
         * 高亮指定UUID的标记
         */
        highlightMark(uuid) {
            this.renderer.highlightMark(uuid);
        }
        /**
         * 清除所有高亮
         */
        clearAllHighlights() {
            this.renderer.clearAllHighlights();
        }
        /**
         * 获取所有标记ID
         */
        getAllMarkIds() {
            return this.renderer.getAllMarkIds();
        }
        /**
         * 步骤1：创建用户映射
         */
        createUserMap(userList) {
            return new Map(Object.entries(userList).map(([key, value]) => [Number(key), value]));
        }
        /**
         * 步骤2：构建评论映射
         */
        buildCommentMap(markList, userMap) {
            const commentMap = new Map();
            const COMMENT_TYPES = [2, 3, 5]; // COMMENT, REPLY, ORIGIN_COMMENT
            for (const mark of markList) {
                if (COMMENT_TYPES.includes(mark.type)) {
                    const user = userMap.get(mark.user_id);
                    const comment = {
                        markId: mark.id,
                        comment: mark.comment,
                        userId: mark.user_id,
                        username: user?.username || '',
                        avatar: user?.avatar || '',
                        isDeleted: mark.is_deleted,
                        children: [],
                        createdAt: typeof mark.created_at === 'string' ? new Date(mark.created_at) : mark.created_at,
                        rootId: mark.root_id,
                        showInput: false,
                        loading: false,
                        operateLoading: false
                    };
                    commentMap.set(mark.id, comment);
                }
            }
            return commentMap;
        }
        /**
         * 步骤3：构建评论关系（回复的父子关系）
         */
        buildCommentRelationships(markList, commentMap) {
            const REPLY_TYPE = 3;
            for (const mark of markList) {
                if (mark.type !== REPLY_TYPE)
                    continue;
                if (!commentMap.has(mark.id) ||
                    !commentMap.has(mark.parent_id) ||
                    !commentMap.has(mark.root_id))
                    continue;
                const comment = commentMap.get(mark.id);
                const parentComment = commentMap.get(mark.parent_id);
                comment.reply = {
                    id: parentComment.markId,
                    username: parentComment.username,
                    userId: parentComment.userId,
                    avatar: parentComment.avatar
                };
                const rootComment = commentMap.get(mark.root_id);
                if (rootComment) {
                    rootComment.children.push(comment);
                }
            }
        }
        /**
         * 步骤4：生成 MarkItemInfo 列表（按source分组）
         */
        generateMarkItemInfos(markList, commentMap) {
            const infoItems = [];
            const LINE_TYPES = [1, 4]; // LINE, ORIGIN_LINE
            const COMMENT_TYPES = [2, 5]; // COMMENT, ORIGIN_COMMENT
            const ORIGIN_TYPES = [4, 5]; // ORIGIN_LINE, ORIGIN_COMMENT
            const REPLY_TYPE = 3;
            for (const mark of markList) {
                const source = mark.source;
                // 跳过 REPLY 类型和数字类型的 source
                if (typeof source === 'number' || mark.type === REPLY_TYPE)
                    continue;
                // 跳过没有 approx_source 的原始标记
                if (ORIGIN_TYPES.includes(mark.type) &&
                    (!mark.approx_source || Object.keys(mark.approx_source).length === 0)) {
                    continue;
                }
                const markSources = source;
                let markInfoItem = infoItems.find((infoItem) => this.checkMarkSourceIsSame(infoItem.source, markSources));
                if (!markInfoItem) {
                    if (mark.approx_source) {
                        try {
                            const newRange = this.getRangeFromApprox(mark.approx_source);
                            const rawText = newRange ? getRangeTextWithNewlines(newRange) : undefined;
                            mark.approx_source.raw_text = rawText;
                        }
                        catch (error) {
                            console.error('create raw text failed', error, mark.approx_source?.exact);
                        }
                    }
                    markInfoItem = {
                        id: generateUUID(),
                        source: markSources,
                        comments: [],
                        stroke: [],
                        approx: mark.approx_source
                    };
                    infoItems.push(markInfoItem);
                }
                if (LINE_TYPES.includes(mark.type)) {
                    markInfoItem.stroke.push({ mark_id: mark.id, userId: mark.user_id });
                }
                else if (COMMENT_TYPES.includes(mark.type)) {
                    const comment = commentMap.get(mark.id);
                    if (!comment || (comment.isDeleted && comment.children.length === 0)) {
                        continue;
                    }
                    markInfoItem.comments.push(comment);
                }
            }
            return infoItems;
        }
        /**
         * 对当前选中区域执行划线处理
         *
         * 读取 window.getSelection() → 解析路径和 approx → 构建 MarkItemInfo → 渲染划线
         *
         * 注意：此方法不调用后端 API，只在本地渲染。
         * 返回值包含调用 /v1/mark/create 接口所需的全部字段，以及用于后续关联的 uuid。
         * 拿到后端 mark_id 后，调用 updateMarkIdByUuid 完成关联。
         *
         * @param userId 当前用户ID（可选）
         * @returns StrokeCreateData（含 uuid 及接口入参），若选区无效则返回 null
         */
        strokeCurrentSelection(userId) {
            console.log('[MarkManager] strokeCurrentSelection 开始，userId:', userId);
            const selection = window.getSelection();
            if (!selection || selection.rangeCount === 0) {
                console.log('[MarkManager] strokeCurrentSelection 终止：无选区');
                return null;
            }
            const range = selection.getRangeAt(0);
            if (range.collapsed) {
                console.log('[MarkManager] strokeCurrentSelection 终止：选区已折叠');
                return null;
            }
            if (!this.container.contains(range.commonAncestorContainer)) {
                console.log('[MarkManager] strokeCurrentSelection 终止：选区不在容器内');
                return null;
            }
            // 一次性解析选区内的节点信息，后续各步骤共用
            const selectionInfo = this.getSelectionInfoFromRange(range);
            console.log('[MarkManager] strokeCurrentSelection selectionInfo 解析结果，数量:', selectionInfo.length, selectionInfo);
            if (selectionInfo.length === 0) {
                console.log('[MarkManager] strokeCurrentSelection 终止：selectionInfo 为空');
                return null;
            }
            // 解析渲染所需的路径（MarkPathItem[]）
            const paths = this.buildPathsFromSelectionInfo(selectionInfo);
            console.log('[MarkManager] strokeCurrentSelection paths 解析结果，数量:', paths.length, paths);
            if (paths.length === 0) {
                console.log('[MarkManager] strokeCurrentSelection 终止：paths 为空');
                return null;
            }
            // 解析 approx（同时得到接口格式的 approx_source，含 position_start/position_end）
            const { approx, approxCreate } = this.parseApproxFromRange(range);
            console.log('[MarkManager] strokeCurrentSelection approx:', approx, 'approxCreate:', approxCreate);
            // 构建接口所需的 select_content
            const selectContent = this.buildSelectContent(selectionInfo);
            console.log('[MarkManager] strokeCurrentSelection selectContent:', selectContent);
            // 接口所需的 source（xpath 格式）
            const apiSource = this.convertToApiSource(paths);
            console.log('[MarkManager] strokeCurrentSelection apiSource:', apiSource);
            // 检查是否已存在相同 source 的 MarkItemInfo（幂等处理）
            const existing = this.markItemInfos.find((info) => this.checkMarkSourceIsSame(info.source, paths));
            if (existing) {
                console.log('[MarkManager] strokeCurrentSelection 命中已有 MarkItemInfo，uuid:', existing.id);
                const alreadyStroked = existing.stroke.some((s) => s.userId === (userId ?? 0));
                if (!alreadyStroked) {
                    console.log('[MarkManager] strokeCurrentSelection 当前用户尚未划线，追加 stroke');
                    existing.stroke.push({ mark_id: undefined, userId: userId ?? 0 });
                    this.drawSingleMarkItem(existing);
                }
                else {
                    console.log('[MarkManager] strokeCurrentSelection 当前用户已有划线，跳过渲染');
                }
                return {
                    uuid: existing.id,
                    source: apiSource,
                    select_content: selectContent,
                    approx_source: approxCreate
                };
            }
            // 构建新的 MarkItemInfo 并渲染
            const uuid = generateUUID();
            console.log('[MarkManager] strokeCurrentSelection 创建新 MarkItemInfo，uuid:', uuid);
            const infoItem = {
                id: uuid,
                source: paths,
                stroke: [{ mark_id: undefined, userId: userId ?? 0 }],
                comments: [],
                approx
            };
            this.markItemInfos.push(infoItem);
            this.drawSingleMarkItem(infoItem);
            const result = {
                uuid,
                source: apiSource,
                select_content: selectContent,
                approx_source: approxCreate
            };
            console.log('[MarkManager] strokeCurrentSelection 完成，返回:', result);
            return result;
        }
        /**
         * 通过 uuid 找到对应的 MarkItemInfo，并将其 stroke 中 mark_id 为空的项更新为指定 mark_id
         *
         * 用于在后端 API 返回 mark_id 后，将本地临时记录与后端数据关联
         *
         * @param uuid MarkItemInfo 的 uuid（由 strokeCurrentSelection 返回）
         * @param markId 后端返回的 mark_id
         * @param userId 用户ID（用于精确匹配对应 stroke 条目，可选）
         */
        updateMarkIdByUuid(uuid, markId, userId) {
            console.log('[MarkManager] updateMarkIdByUuid 开始，uuid:', uuid, 'markId:', markId, 'userId:', userId);
            console.log('[MarkManager] updateMarkIdByUuid 当前 markItemInfos（共 %d 条）:', this.markItemInfos.length, JSON.parse(JSON.stringify(this.markItemInfos)));
            const infoItem = this.markItemInfos.find((info) => info.id === uuid);
            if (!infoItem) {
                console.warn('[MarkManager] updateMarkIdByUuid 未找到对应的 MarkItemInfo，uuid:', uuid);
                return;
            }
            console.log('[MarkManager] updateMarkIdByUuid 找到 MarkItemInfo:', JSON.parse(JSON.stringify(infoItem)));
            console.log('[MarkManager] updateMarkIdByUuid 当前 stroke 列表:', JSON.parse(JSON.stringify(infoItem.stroke)));
            const stroke = infoItem.stroke.find((s) => !s.mark_id && (userId === undefined || s.userId === userId));
            if (stroke) {
                console.log('[MarkManager] updateMarkIdByUuid 找到匹配 stroke，更新前:', JSON.parse(JSON.stringify(stroke)));
                stroke.mark_id = markId;
                console.log('[MarkManager] updateMarkIdByUuid 更新后 stroke:', JSON.parse(JSON.stringify(stroke)));
            }
            else {
                console.warn('[MarkManager] updateMarkIdByUuid 未找到可更新的 stroke（mark_id 为空且 userId 匹配）', 'userId 过滤条件:', userId);
            }
            console.log('[MarkManager] updateMarkIdByUuid 完成，最新 markItemInfos:', JSON.parse(JSON.stringify(this.markItemInfos)));
        }
        /**
         * 通过 uuid 将后端返回的 mark_id 回补到评论记录
         *
         * 找到指定 uuid 的 MarkItemInfo，将 comments 中最后一条 markId === 0 的临时评论
         * 更新为后端返回的真实 markId，确保后续删除/更新操作能正确关联后端数据。
         *
         * @param uuid MarkItemInfo 的本地 UUID
         * @param markId 后端返回的 mark_id
         * @returns 是否成功更新
         */
        updateCommentMarkIdByUuid(uuid, markId) {
            const infoItem = this.markItemInfos.find((info) => info.id === uuid);
            if (!infoItem) {
                console.warn('[MarkManager] updateCommentMarkIdByUuid 未找到对应的 MarkItemInfo，uuid:', uuid);
                return false;
            }
            // 从后往前找第一条 markId 为 0 的临时评论（即最近一次 addCommentByUuid 添加的）
            for (let i = infoItem.comments.length - 1; i >= 0; i--) {
                if (infoItem.comments[i].markId === 0) {
                    infoItem.comments[i].markId = markId;
                    console.log('[MarkManager] updateCommentMarkIdByUuid 成功，uuid:', uuid, 'markId:', markId);
                    return true;
                }
            }
            console.warn('[MarkManager] updateCommentMarkIdByUuid 未找到 markId 为 0 的临时评论，uuid:', uuid);
            return false;
        }
        /**
         * 将 Range 转换为 MarkPathItem 数组
         *
         * @deprecated 内部请改用 buildPathsFromSelectionInfo，避免重复解析 Range
         */
        parseRangeToPaths(range) {
            return this.buildPathsFromSelectionInfo(this.getSelectionInfoFromRange(range));
        }
        /**
         * 从已解析的选区节点信息构建 MarkPathItem 数组（供渲染使用）
         *
         * 相邻同 path 的文本项合并为一个条目；SLAX-MARK 标签会被穿透，
         * 使用其真实父元素的路径，与 SelectionMonitor.convertSelectionToPaths 逻辑一致
         */
        buildPathsFromSelectionInfo(selectionInfo) {
            const paths = [];
            let currentPath = null;
            let currentStart = 0;
            let currentEnd = 0;
            for (const item of selectionInfo) {
                if (item.type === 'text') {
                    let parent = item.node.parentElement;
                    while (parent && parent.tagName === 'SLAX-MARK') {
                        parent = parent.parentElement;
                    }
                    if (!parent)
                        continue;
                    const path = getElementPath(parent, this.container);
                    const allTextNodes = getAllTextNodes(parent);
                    let offset = 0;
                    for (const textNode of allTextNodes) {
                        if (textNode === item.node)
                            break;
                        offset += (textNode.textContent || '').length;
                    }
                    const start = offset + item.startOffset;
                    const end = offset + item.endOffset;
                    if (path === currentPath) {
                        currentEnd = end;
                    }
                    else {
                        if (currentPath !== null) {
                            paths.push({ type: 'text', path: currentPath, start: currentStart, end: currentEnd });
                        }
                        currentPath = path;
                        currentStart = start;
                        currentEnd = end;
                    }
                }
                else if (item.type === 'image') {
                    if (currentPath !== null) {
                        paths.push({ type: 'text', path: currentPath, start: currentStart, end: currentEnd });
                        currentPath = null;
                    }
                    const path = getElementPath(item.element, this.container);
                    paths.push({ type: 'image', path });
                }
            }
            if (currentPath !== null) {
                paths.push({ type: 'text', path: currentPath, start: currentStart, end: currentEnd });
            }
            return paths;
        }
        /**
         * 将渲染用的 MarkPathItem[] 转换为接口入参格式 StrokeCreateSource[]
         *
         * 字段映射：path → xpath，start → start_offset，end → end_offset
         * 图片类型的偏移量固定为 0
         */
        convertToApiSource(paths) {
            return paths.map((p) => ({
                type: p.type,
                xpath: p.path,
                start_offset: p.start ?? 0,
                end_offset: p.end ?? 0
            }));
        }
        /**
         * 从已解析的选区节点信息构建 select_content
         *
         * 构建逻辑参考 DwebArticleSelection.handleMouseUp 对 list 的遍历：
         * - 相邻文本项合并（去除换行）
         * - 图片独立一项
         */
        buildSelectContent(selectionInfo) {
            const result = [];
            for (const item of selectionInfo) {
                if (item.type === 'text') {
                    const rawText = (item.node.textContent || '').slice(item.startOffset, item.endOffset);
                    const text = rawText.replace(/\n/g, '');
                    const last = result[result.length - 1];
                    if (last?.type === 'text') {
                        // 与 DwebArticleSelection 一致：相邻文本合并
                        last.text += text;
                    }
                    else {
                        result.push({ type: 'text', text, src: '' });
                    }
                }
                else if (item.type === 'image') {
                    result.push({ type: 'image', text: '', src: item.element.src });
                }
            }
            return result;
        }
        /**
         * 从 Range 获取选区信息（文本节点 + 图片列表）
         */
        getSelectionInfoFromRange(range) {
            const result = [];
            const isFullyInRange = (node) => {
                const nr = document.createRange();
                nr.selectNodeContents(node);
                return (range.compareBoundaryPoints(Range.START_TO_START, nr) <= 0 &&
                    range.compareBoundaryPoints(Range.END_TO_END, nr) >= 0);
            };
            const partiallyInRange = (node) => range.intersectsNode(node);
            const processNode = (node) => {
                if (node.nodeType === Node.TEXT_NODE && (node.textContent?.trim() || '').length > 0) {
                    if (!partiallyInRange(node))
                        return;
                    let startOffset = node === range.startContainer ? range.startOffset : 0;
                    let endOffset = node === range.endContainer ? range.endOffset : node.length;
                    startOffset = Math.max(0, Math.min(startOffset, node.length));
                    endOffset = Math.max(startOffset, Math.min(endOffset, node.length));
                    if (endOffset > startOffset) {
                        result.push({ type: 'text', node, startOffset, endOffset });
                    }
                }
                else if (node.nodeType === Node.ELEMENT_NODE) {
                    const el = node;
                    if (el.tagName === 'IMG' && isFullyInRange(el)) {
                        result.push({ type: 'image', element: el });
                    }
                    if (partiallyInRange(el)) {
                        for (const child of Array.from(el.childNodes))
                            processNode(child);
                    }
                }
            };
            processNode(range.commonAncestorContainer);
            return result;
        }
        /**
         * 从 Range 中提取 approx 信息，同时返回渲染格式和接口格式
         *
         * - approx：供 MarkItemInfo 内部使用（含 raw_text）
         * - approxCreate：供 /v1/mark/create 接口使用（含 position_start / position_end）
         *
         * position_start = 容器起点到选区起点的完整文本长度
         * position_end   = position_start + exact.length
         */
        parseApproxFromRange(range) {
            const exact = getRangeTextWithNewlines(range);
            const prefixRange = document.createRange();
            prefixRange.setStart(this.container, 0);
            prefixRange.setEnd(range.startContainer, range.startOffset);
            const fullPrefix = getRangeTextWithNewlines(prefixRange);
            const prefix = fullPrefix.slice(-50);
            const suffixRange = document.createRange();
            suffixRange.setStart(range.endContainer, range.endOffset);
            if (this.container.lastChild) {
                suffixRange.setEndAfter(this.container.lastChild);
            }
            const fullSuffix = getRangeTextWithNewlines(suffixRange);
            const suffix = fullSuffix.slice(0, 50);
            const position_start = fullPrefix.length;
            const position_end = position_start + exact.length;
            return {
                approx: { exact, prefix, suffix, raw_text: exact },
                approxCreate: { exact, prefix, suffix, position_start, position_end }
            };
        }
        /**
         * 根据 source（MarkPathItem[]）构建 Range 并复用 parseApproxFromRange 生成 MarkPathApprox
         *
         * @param source MarkPathItem 数组
         * @returns 生成的 MarkPathApprox，若 DOM 元素不存在则返回 undefined
         */
        buildApproxFromSource(source) {
            const range = this.buildRangeFromSource(source);
            if (!range)
                return undefined;
            try {
                const { approx } = this.parseApproxFromRange(range);
                return approx;
            }
            catch (error) {
                console.warn('[MarkManager] buildApproxFromSource parseApproxFromRange 失败:', error);
                return undefined;
            }
        }
        /**
         * 根据 source（MarkPathItem[]）定位 DOM 元素，构建一个覆盖整个选区的 Range
         *
         * @param source MarkPathItem 数组
         * @returns 构建的 Range，若 DOM 元素不存在则返回 null
         */
        buildRangeFromSource(source) {
            // 只处理文本类型的 source
            const textSources = source.filter((s) => s.type === 'text');
            if (textSources.length === 0)
                return null;
            const first = textSources[0];
            const last = textSources[textSources.length - 1];
            const firstElement = this.container.querySelector(first.path);
            const lastElement = this.container.querySelector(last.path);
            if (!firstElement || !lastElement)
                return null;
            // 在第一个元素中定位起始文本节点和偏移
            const startResult = this.findTextNodeAtOffset(firstElement, first.start ?? 0);
            // 在最后一个元素中定位结束文本节点和偏移
            const endResult = this.findTextNodeAtOffset(lastElement, last.end ?? 0);
            if (!startResult || !endResult)
                return null;
            try {
                const range = document.createRange();
                range.setStart(startResult.node, startResult.offset);
                range.setEnd(endResult.node, endResult.offset);
                return range;
            }
            catch (error) {
                console.warn('[MarkManager] buildRangeFromSource Range 构建失败:', error);
                return null;
            }
        }
        /**
         * 在元素的文本节点中定位指定字符偏移所对应的 { node, offset }
         */
        findTextNodeAtOffset(element, targetOffset) {
            const textNodes = getAllTextNodes(element);
            let accumulated = 0;
            for (const node of textNodes) {
                const nodeLen = (node.textContent || '').length;
                if (accumulated + nodeLen >= targetOffset) {
                    return { node, offset: targetOffset - accumulated };
                }
                accumulated += nodeLen;
            }
            // 偏移超出范围，定位到最后一个文本节点末尾
            if (textNodes.length > 0) {
                const lastNode = textNodes[textNodes.length - 1];
                return { node: lastNode, offset: (lastNode.textContent || '').length };
            }
            return null;
        }
        /**
         * 检查两个 source 是否相同
         */
        checkMarkSourceIsSame(source1, source2) {
            return deepEqual(source1, source2);
        }
        /**
         * 从 approx 信息获取 Range（占位实现）
         */
        getRangeFromApprox(_approx) {
            return null;
        }
        /**
         * 渲染单个 MarkItemInfo
         */
        drawSingleMarkItem(info) {
            const hasStroke = info.stroke.length > 0;
            const hasComment = info.comments.length > 0;
            const userId = info.stroke.length > 0 ? info.stroke[0].userId : info.comments[0]?.userId;
            this.renderer.drawMark(info.id, info.source, hasStroke, hasComment, userId);
        }
        /**
         * 构建返回结果：uuid -> BackendMarkInfo[]
         */
        buildDrawMarksResult(markList) {
            const result = {};
            for (const itemInfo of this.markItemInfos) {
                const relatedMarks = [];
                for (const stroke of itemInfo.stroke) {
                    if (stroke.mark_id) {
                        const mark = markList.find((m) => m.id === stroke.mark_id);
                        if (mark)
                            relatedMarks.push(mark);
                    }
                }
                for (const comment of itemInfo.comments) {
                    const mark = markList.find((m) => m.id === comment.markId);
                    if (mark)
                        relatedMarks.push(mark);
                    for (const child of comment.children) {
                        const childMark = markList.find((m) => m.id === child.markId);
                        if (childMark)
                            relatedMarks.push(childMark);
                    }
                }
                result[itemInfo.id] = relatedMarks;
            }
            return result;
        }
    }

    class SlaxWebViewBridge {
        constructor() {
            // selection 相关状态
            this.selectionMonitor = null;
            this.markRenderer = null;
            this.markManager = null;
            this.selectionContainer = null;
            this.markClickCleanup = null;
            this.onMarkTap = null;
            this.onSelectionMarkInfoChange = null;
            this.postMessage = postToNativeBridge;
            this.getContentHeight = getContentHeight;
            this.scrollToAnchor = scrollToAnchor;
            this.highlightElement = highlightElement;
            this.findMatchingElement = findMatchingElement;
            this.scrollToElement = scrollToElement;
            this.init();
        }
        init() {
            applyPolyfills();
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', () => {
                    this.onDOMReady();
                });
            }
            else {
                this.onDOMReady();
            }
            console.log('[WebView Bridge] Bridge initialized successfully');
        }
        onDOMReady() {
            initImageClickHandlers();
            initBookmarkNotFoundHandlers();
            // 通知 native bridge DOM 已加载完成
            postToNativeBridge({
                type: 'domReady'
            });
            console.log('[WebView Bridge] DOM ready event sent to native bridge');
        }
        // ==================== 划线选择功能 ====================
        /**
         * 开始监听文本选择
         * @param containerSelector 监听容器的 CSS 选择器
         * @param currentUserId 当前用户ID（可选，用于判断是否为自己的划线）
         */
        startSelectionMonitoring(containerSelector, currentUserId) {
            const container = document.querySelector(containerSelector);
            if (!container) {
                console.error(`[WebView Bridge] Container not found: ${containerSelector}`);
                return;
            }
            // 如果已有监听器，先停止
            this.stopSelectionMonitoring();
            this.selectionContainer = container;
            // 追踪 touchstart 位置，供每个 slax-mark 的 touchend 回调做滚动判断
            let touchStartX = 0;
            let touchStartY = 0;
            const trackTouchStart = (e) => {
                if (e.touches.length === 1) {
                    touchStartX = e.touches[0].clientX;
                    touchStartY = e.touches[0].clientY;
                }
            };
            document.addEventListener('touchstart', trackTouchStart, { passive: true });
            this.markClickCleanup = () => document.removeEventListener('touchstart', trackTouchStart);
            /**
             * 每个 slax-mark 元素绑定此回调（在 MarkRenderer 内直接 addEventListener）。
             * 在元素自身的 touchend 中判断：选区是否为空、手指是否移动过大，
             * 再聚合同 UUID 所有 mark 的文本发送给 native。
             */
            const onMarkTap = (markId, event) => {
                if (event.changedTouches.length === 0)
                    return;
                // 有文本选中说明用户在选词，不触发划线点击
                const selection = window.getSelection();
                if (selection && !selection.isCollapsed)
                    return;
                const touch = event.changedTouches[0];
                const dx = Math.abs(touch.clientX - touchStartX);
                const dy = Math.abs(touch.clientY - touchStartY);
                // 移动超过 10px 视为滚动
                if (dx > 10 || dy > 10)
                    return;
                const allMarks = Array.from(container.querySelectorAll(`slax-mark[data-uuid="${markId}"]`));
                const fullText = allMarks.map((el) => el.textContent || '').join('');
                const markItemInfo = this.markManager?.getMarkItemInfoByUuid(markId) ?? null;
                postToNativeBridge({
                    type: 'markClicked',
                    markId,
                    text: fullText,
                    markItemInfo: markItemInfo ? JSON.stringify(markItemInfo) : null
                });
            };
            this.onMarkTap = onMarkTap;
            /**
             * 选区对应的 MarkItemInfo 变化时，通过 native bridge 通知原生端
             */
            const onSelectionMarkInfoChange = (markItemInfo) => {
                console.log('[WebView Bridge] Selection MarkItemInfo changed:', markItemInfo);
                postToNativeBridge({
                    type: 'selectionMarkItemInfo',
                    markItemInfo: JSON.stringify(markItemInfo)
                });
            };
            this.onSelectionMarkInfoChange = onSelectionMarkInfoChange;
            this.markRenderer = new MarkRenderer(container, currentUserId, onMarkTap);
            this.markManager = new MarkManager(container, currentUserId, onMarkTap, onSelectionMarkInfoChange);
            this.selectionMonitor = new SelectionMonitor(container);
            this.selectionMonitor.start((data) => {
                // 选区变化时，检测当前选区是否匹配已有的 MarkItemInfo
                this.markManager?.detectSelectionMarkItemInfo(data.paths, data.approx);
                const jsonData = JSON.stringify({
                    paths: data.paths,
                    approx: data.approx,
                    selection: data.selection.map((item) => {
                        if (item.type === 'text') {
                            return {
                                type: 'text',
                                text: item.text,
                                start_offset: item.startOffset,
                                end_offset: item.endOffset
                            };
                        }
                        else {
                            return { type: 'image', src: item.src };
                        }
                    })
                });
                postToNativeBridge({
                    type: 'textSelected',
                    data: jsonData,
                    position: JSON.stringify(data.position)
                });
            }, () => {
                // 选区取消时，清除当前选区对应的 MarkItemInfo（不触发回调）
                this.markManager?.clearCurrentMarkItemInfo();
            });
            console.log(`[WebView Bridge] Selection monitoring started on: ${containerSelector}`);
        }
        /**
         * 停止监听文本选择
         */
        stopSelectionMonitoring() {
            if (this.selectionMonitor) {
                this.selectionMonitor.stop();
                this.selectionMonitor = null;
            }
            if (this.markClickCleanup) {
                this.markClickCleanup();
                this.markClickCleanup = null;
            }
            this.selectionContainer = null;
            this.markRenderer = null;
            this.markManager = null;
            this.onSelectionMarkInfoChange = null;
        }
        /**
         * 清除当前文本选择
         */
        clearSelection() {
            this.selectionMonitor?.clearSelection();
        }
        /**
         * 更新标记
         */
        updateMark(id, isStroke, hasComment, userId) {
            if (!this.markRenderer)
                return;
            try {
                this.markRenderer.updateMark(id, isStroke, hasComment, userId);
            }
            catch (error) {
                postToNativeBridge({ type: 'selectionError', error: `Failed to update mark: ${error}` });
            }
        }
        /**
         * 删除标记
         */
        removeMark(id) {
            if (!this.markRenderer)
                return;
            try {
                this.markRenderer.removeMark(id);
            }
            catch (error) {
                postToNativeBridge({ type: 'selectionError', error: `Failed to remove mark: ${error}` });
            }
        }
        /**
         * 高亮标记
         */
        highlightMark(id) {
            if (!this.markRenderer)
                return;
            try {
                this.markRenderer.highlightMark(id);
            }
            catch (error) {
                postToNativeBridge({ type: 'selectionError', error: `Failed to highlight mark: ${error}` });
            }
        }
        /**
         * 获取所有标记ID
         */
        getAllMarkIds() {
            return this.markRenderer?.getAllMarkIds() ?? [];
        }
        /**
         * 批量绘制标记（从后端 MarkDetail 数据）
         * @param markDetailJson MarkDetail 的 JSON 字符串
         * @returns DrawMarksResult 的 JSON 字符串：{ uuid: BackendMarkInfo[] }
         */
        drawMarks(markDetailJson) {
            if (!this.markManager) {
                console.warn('[WebView Bridge] drawMarks: selection monitoring not started');
                return JSON.stringify({});
            }
            try {
                const markDetail = JSON.parse(markDetailJson);
                const result = this.markManager.drawMarks(markDetail);
                return JSON.stringify(result);
            }
            catch (error) {
                postToNativeBridge({ type: 'selectionError', error: `Failed to draw marks: ${error}` });
                return JSON.stringify({});
            }
        }
        /**
         * 根据 UUID 删除标记
         */
        removeMarkByUuid(uuid) {
            if (!this.markManager)
                return;
            try {
                this.markManager.removeMarkByUuid(uuid);
            }
            catch (error) {
                postToNativeBridge({ type: 'selectionError', error: `Failed to remove mark by UUID: ${error}` });
            }
        }
        /**
         * 对当前选中区域执行划线处理（不调用后端 API，仅本地渲染）
         *
         * 读取 window.getSelection() → 解析路径和 approx → 构建 MarkItemInfo → 渲染划线标记
         *
         * 返回 JSON 字符串，结构如下（StrokeCreateData）：
         * ```
         * {
         *   uuid: string              // 本地 UUID，用于 updateMarkIdByUuid 关联后端 mark_id
         *   source: StrokeCreateSource[]        // /v1/mark/create 接口的 source 字段
         *   select_content: StrokeCreateSelectContent[] // 接口的 select_content 字段
         *   approx_source?: StrokeCreateApproxSource    // 接口的 approx_source 字段（含 position_start/position_end）
         * }
         * ```
         *
         * 选区无效时返回 null。
         *
         * @param userId 当前用户ID（可选，用于判断是否为自己的划线样式）
         * @returns StrokeCreateData 的 JSON 字符串，或 null
         */
        strokeCurrentSelection(userId) {
            if (!this.markManager) {
                console.warn('[WebView Bridge] strokeCurrentSelection: selection monitoring not started');
                return null;
            }
            try {
                const result = this.markManager.strokeCurrentSelection(userId);
                return result ? JSON.stringify(result) : null;
            }
            catch (error) {
                postToNativeBridge({ type: 'selectionError', error: `Failed to stroke selection: ${error}` });
                return null;
            }
        }
        /**
         * 通过 uuid 将后端返回的 mark_id 关联到本地 MarkItemInfo 的 stroke 记录
         *
         * 在调用 strokeCurrentSelection 拿到 uuid 后，等后端 API 返回 mark_id，
         * 再调用此方法完成关联，以便后续删除/更新操作能找到正确的后端 ID。
         *
         * @param uuid strokeCurrentSelection 返回的 uuid
         * @param markId 后端返回的 mark_id
         * @param userId 用户ID（可选，用于精确匹配对应 stroke 条目）
         */
        updateMarkIdByUuid(uuid, markId, userId) {
            if (!this.markManager)
                return;
            try {
                this.markManager.updateMarkIdByUuid(uuid, markId, userId);
            }
            catch (error) {
                postToNativeBridge({ type: 'selectionError', error: `Failed to update mark id by UUID: ${error}` });
            }
        }
        /**
         * 根据 UUID 为指定用户添加划线
         *
         * 更新 MarkItemInfo 的 stroke 数组并刷新页面中对应 slax-mark 的样式。
         * 已有该用户划线时幂等跳过。
         *
         * @param uuid MarkItemInfo 的本地 UUID
         * @param userId 执行划线的用户ID
         * @returns 是否成功添加
         */
        addStrokeByUuid(uuid, userId) {
            if (!this.markManager) {
                console.warn('[WebView Bridge] addStrokeByUuid: selection monitoring not started');
                return false;
            }
            try {
                return this.markManager.addStrokeByUuid(uuid, userId);
            }
            catch (error) {
                postToNativeBridge({ type: 'selectionError', error: `Failed to add stroke by UUID: ${error}` });
                return false;
            }
        }
        /**
         * 通过 source 添加划线（用于临时选区场景）
         *
         * 当 markItemInfo 是临时的（id 为空、未被 markItemInfos 持有）时使用此方法。
         * 会自动创建或复用已有的 MarkItemInfo，插入划线并渲染 DOM。
         *
         * @param sourceJson MarkPathItem[] 的 JSON 字符串
         * @param userId 执行划线的用户ID
         * @param approxJson MarkPathApprox 的 JSON 字符串（可选）
         * @returns StrokeCreateData 的 JSON 字符串（含 uuid 及接口入参），失败返回 null
         */
        addStrokeBySource(sourceJson, userId, approxJson) {
            if (!this.markManager) {
                console.warn('[WebView Bridge] addStrokeBySource: selection monitoring not started');
                return null;
            }
            try {
                const source = JSON.parse(sourceJson);
                const approx = approxJson ? JSON.parse(approxJson) : undefined;
                const result = this.markManager.addStrokeBySource(source, userId, approx);
                return result ? JSON.stringify(result) : null;
            }
            catch (error) {
                postToNativeBridge({ type: 'selectionError', error: `Failed to add stroke by source: ${error}` });
                return null;
            }
        }
        /**
         * 根据 UUID 删除指定用户的划线
         *
         * 从 MarkItemInfo 的 stroke 数组中移除该用户的记录并刷新 slax-mark 样式。
         * 若 stroke 和 comments 均为空，则整体删除该标记。
         *
         * @param uuid MarkItemInfo 的本地 UUID
         * @param userId 要删除划线的用户ID
         * @returns 是否成功删除
         */
        removeStrokeByUuid(uuid, userId) {
            if (!this.markManager) {
                console.warn('[WebView Bridge] removeStrokeByUuid: selection monitoring not started');
                return false;
            }
            try {
                return this.markManager.removeStrokeByUuid(uuid, userId);
            }
            catch (error) {
                postToNativeBridge({ type: 'selectionError', error: `Failed to remove stroke by UUID: ${error}` });
                return false;
            }
        }
        /**
         * 根据 UUID 添加评论
         *
         * 在 MarkItemInfo 的 comments 数组中追加一条评论并刷新 slax-mark 样式（添加 .comment class）。
         *
         * @param uuid MarkItemInfo 的本地 UUID
         * @param params 评论参数对象，包含 userId、comment、username、avatar
         * @returns 是否成功添加
         */
        addCommentByUuid(uuid, params) {
            if (!this.markManager) {
                console.warn('[WebView Bridge] addCommentByUuid: selection monitoring not started');
                return false;
            }
            try {
                return this.markManager.addCommentByUuid(uuid, params);
            }
            catch (error) {
                postToNativeBridge({ type: 'selectionError', error: `Failed to add comment by UUID: ${error}` });
                return false;
            }
        }
        /**
         * 通过 UUID 将后端返回的 mark_id 回补到评论记录
         *
         * 在调用 addCommentByUuid 添加本地临时评论后，等后端 API 返回 mark_id，
         * 再调用此方法将临时评论（markId=0）的 markId 更新为真实值。
         *
         * @param uuid MarkItemInfo 的本地 UUID
         * @param markId 后端返回的 mark_id
         * @returns 是否成功更新
         */
        updateCommentMarkIdByUuid(uuid, markId) {
            if (!this.markManager) {
                console.warn('[WebView Bridge] updateCommentMarkIdByUuid: selection monitoring not started');
                return false;
            }
            try {
                return this.markManager.updateCommentMarkIdByUuid(uuid, markId);
            }
            catch (error) {
                postToNativeBridge({ type: 'selectionError', error: `Failed to update comment mark id by UUID: ${error}` });
                return false;
            }
        }
        /**
         * 通过 source 添加评论（用于临时选区场景）
         *
         * 当 markItemInfo 是临时的（id 为空、未被 markItemInfos 持有）时使用此方法。
         * 会自动创建或复用已有的 MarkItemInfo，插入评论并渲染 DOM。
         *
         * @param sourceJson MarkPathItem[] 的 JSON 字符串
         * @param commentParams 评论参数对象
         * @param approxJson MarkPathApprox 的 JSON 字符串（可选）
         * @returns StrokeCreateData 的 JSON 字符串（含 uuid 及接口入参），失败返回 null
         */
        addCommentBySource(sourceJson, commentParams, approxJson) {
            if (!this.markManager) {
                console.warn('[WebView Bridge] addCommentBySource: selection monitoring not started');
                return null;
            }
            try {
                const source = JSON.parse(sourceJson);
                const approx = approxJson ? JSON.parse(approxJson) : undefined;
                const result = this.markManager.addCommentBySource(source, commentParams, approx);
                return result ? JSON.stringify(result) : null;
            }
            catch (error) {
                postToNativeBridge({ type: 'selectionError', error: `Failed to add comment by source: ${error}` });
                return null;
            }
        }
        /**
         * 设置当前用户ID（会重建内部 renderer/manager）
         */
        setCurrentUserId(userId) {
            if (this.selectionContainer) {
                this.markRenderer = new MarkRenderer(this.selectionContainer, userId, this.onMarkTap ?? undefined);
                this.markManager = new MarkManager(this.selectionContainer, userId, this.onMarkTap ?? undefined, this.onSelectionMarkInfoChange ?? undefined);
            }
        }
    }

    exports.SlaxWebViewBridge = SlaxWebViewBridge;

    return exports;

})({});
window.SlaxWebViewBridge = new SlaxReaderWebBridgeExports.SlaxWebViewBridge();
