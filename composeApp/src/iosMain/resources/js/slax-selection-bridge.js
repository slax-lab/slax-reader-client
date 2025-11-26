var SlaxSelectionBridgeExports = (function (exports) {
    'use strict';

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
     * 获取Range的文本（包含换行）
     *
     * 从 slax-reader-web 直接迁移
     */
    function getRangeTextWithNewlines(range) {
        const selection = window.getSelection();
        if (!selection) {
            console.warn('无法获取 window.getSelection()');
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
     * 标记渲染器
     *
     * 负责在页面上绘制、更新和删除标记（划线和评论）
     *
     * 直接从 slax-reader-web/commons/selection 迁移核心逻辑
     */
    class MarkRenderer {
        constructor(container, currentUserId) {
            this.container = container;
            this.currentUserId = currentUserId;
        }
        /**
         * 根据 MarkPathItem 绘制标记
         */
        drawMark(id, paths, isStroke, hasComment, userId) {
            try {
                const isSelfStroke = userId !== undefined && userId === this.currentUserId;
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
                    debugger;
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
         *
         * 核心方法：从 slax-reader-web 直接迁移
         */
        transferNodeInfos(markItem) {
            const infos = [];
            if (markItem.type === 'text') {
                const baseElement = this.container.querySelector(markItem.path);
                if (!baseElement) {
                    return infos;
                }
                const nodes = this.getAllTextNodes(baseElement);
                const nodeLengths = nodes.map(node => (node.textContent || '').length);
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
         * 获取元素下的所有文本节点
         *
         * 从 slax-reader-web 直接迁移
         */
        getAllTextNodes(element) {
            const unsupportTags = ['UNSUPPORT-VIDEO', 'SCRIPT', 'STYLE', 'NOSCRIPT'];
            const textNodes = [];
            const traverse = (node) => {
                if (node.nodeType === Node.TEXT_NODE) {
                    textNodes.push(node);
                }
                else if (node.nodeType === Node.ELEMENT_NODE && unsupportTags.indexOf(node.tagName) === -1) {
                    node.childNodes.forEach(child => traverse(child));
                }
            };
            traverse(element);
            return textNodes;
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
            // 先清除所有高亮
            this.clearAllHighlights();
            const marks = Array.from(this.container.querySelectorAll(`slax-mark[data-uuid="${id}"]`));
            marks.forEach((mark) => mark.classList.add('highlighted'));
            // 滚动到第一个标记
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
     * 选择监听器
     *
     * 负责监听用户的文本选择操作
     */
    class SelectionMonitor {
        constructor(container) {
            this.isMonitoring = false;
            /**
             * 处理鼠标抬起事件
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
         */
        start(callback) {
            if (this.isMonitoring) {
                return;
            }
            this.onSelectionCallback = callback;
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
            this.container.removeEventListener('mouseup', this.handleMouseUp);
            this.container.removeEventListener('touchend', this.handleMouseUp);
            this.isMonitoring = false;
            this.onSelectionCallback = undefined;
        }
        /**
         * 解析选择的内容
         */
        parseSelection(range, event) {
            const selection = this.getSelectionInfo(range);
            const paths = this.convertSelectionToPaths(selection);
            const approx = this.getApproxInfo(range);
            const position = this.getPositionInfo(range, event);
            return {
                selection,
                paths,
                approx,
                position
            };
        }
        /**
         * 获取位置信息（用于显示菜单）
         * 参考 slax-reader-web 的实现
         */
        getPositionInfo(range, event) {
            // 获取 range 的边界矩形
            const rangeRect = range.getBoundingClientRect();
            // 获取容器的边界矩形
            const containerRect = this.container.getBoundingClientRect();
            // 获取鼠标/触摸位置
            let clientX;
            let clientY;
            if (event instanceof MouseEvent) {
                clientX = event.clientX;
                clientY = event.clientY;
            }
            else {
                // TouchEvent
                clientX = event.changedTouches[0].clientX;
                clientY = event.changedTouches[0].clientY;
            }
            // 计算相对于容器的位置
            const x = clientX - containerRect.left;
            const y = clientY - containerRect.top;
            // 返回完整的位置信息
            return {
                x,
                y,
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
                return range.compareBoundaryPoints(Range.START_TO_START, nodeRange) <= 0 && range.compareBoundaryPoints(Range.END_TO_END, nodeRange) >= 0;
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
                        selectedInfo.push({ type: 'image', src: element.src, element: element });
                    }
                    if (isNodePartiallyInRange(element)) {
                        for (const child of Array.from(element.childNodes))
                            processNode(child);
                    }
                }
            };
            processNode(range.commonAncestorContainer);
            return selectedInfo.length > 0 && !selectedInfo.every(item => item.type === 'text' && item.text.trim().length === 0) ? selectedInfo : [];
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
                    // 找到文本节点所在的父元素
                    let parent = item.node.parentElement;
                    while (parent && parent.tagName === 'SLAX-MARK') {
                        parent = parent.parentElement;
                    }
                    if (!parent)
                        continue;
                    const path = getElementPath(parent, this.container);
                    // 计算文本节点在父元素中的偏移 - 使用与 MarkRenderer 相同的逻辑
                    const allTextNodes = this.getAllTextNodes(parent);
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
                        // 合并连续的文本选择
                        currentEnd = end;
                    }
                    else {
                        // 保存上一个路径
                        if (currentPath !== null) {
                            paths.push({
                                type: 'text',
                                path: currentPath,
                                start: currentStart,
                                end: currentEnd
                            });
                        }
                        // 开始新的路径
                        currentPath = path;
                        currentStart = start;
                        currentEnd = end;
                    }
                }
                else if (item.type === 'image') {
                    // 保存之前的文本路径
                    if (currentPath !== null) {
                        paths.push({
                            type: 'text',
                            path: currentPath,
                            start: currentStart,
                            end: currentEnd
                        });
                        currentPath = null;
                    }
                    // 添加图片路径
                    const path = getElementPath(item.element, this.container);
                    paths.push({
                        type: 'image',
                        path
                    });
                }
            }
            // 保存最后一个文本路径
            if (currentPath !== null) {
                paths.push({
                    type: 'text',
                    path: currentPath,
                    start: currentStart,
                    end: currentEnd
                });
            }
            return paths;
        }
        /**
         * 获取近似匹配信息
         */
        getApproxInfo(range) {
            const exact = getRangeTextWithNewlines(range);
            // 获取前缀和后缀
            const prefixRange = document.createRange();
            prefixRange.setStart(this.container, 0);
            prefixRange.setEnd(range.startContainer, range.startOffset);
            const fullPrefix = getRangeTextWithNewlines(prefixRange);
            const prefix = fullPrefix.slice(-50); // 取最后50个字符
            const suffixRange = document.createRange();
            suffixRange.setStart(range.endContainer, range.endOffset);
            suffixRange.setEndAfter(this.container.lastChild);
            const fullSuffix = getRangeTextWithNewlines(suffixRange);
            const suffix = fullSuffix.slice(0, 50); // 取前50个字符
            return {
                exact,
                prefix,
                suffix,
                raw_text: exact
            };
        }
        /**
         * 获取元素下的所有文本节点
         *
         * ⚠️ 关键：必须与 MarkRenderer.getAllTextNodes 保持完全一致
         */
        getAllTextNodes(element) {
            const unsupportTags = ['UNSUPPORT-VIDEO', 'SCRIPT', 'STYLE', 'NOSCRIPT'];
            const textNodes = [];
            const traverse = (node) => {
                if (node.nodeType === Node.TEXT_NODE) {
                    textNodes.push(node);
                }
                else if (node.nodeType === Node.ELEMENT_NODE && unsupportTags.indexOf(node.tagName) === -1) {
                    node.childNodes.forEach(child => traverse(child));
                }
            };
            traverse(element);
            return textNodes;
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
     * 标记管理器
     *
     * 负责处理后端 MarkDetail 数据的预处理、分组和渲染
     */
    class MarkManager {
        constructor(container, currentUserId) {
            this.markItemInfos = [];
            this.container = container;
            this.currentUserId = currentUserId;
            this.renderer = new MarkRenderer(container, currentUserId);
        }
        /**
         * 绘制多个标记
         *
         * @param marks 后端返回的 MarkDetail 数据
         * @returns 键值对：uuid -> 该uuid对应的后端mark列表
         */
        drawMarks(marks) {
            // 1. 创建用户映射
            const userMap = this.createUserMap(marks.user_list);
            // 2. 构建评论映射
            const commentMap = this.buildCommentMap(marks.mark_list, userMap);
            // 3. 构建评论关系
            this.buildCommentRelationships(marks.mark_list, commentMap);
            // 4. 生成 MarkItemInfo 列表（按source分组）
            this.markItemInfos = this.generateMarkItemInfos(marks.mark_list, commentMap);
            // 5. 渲染所有标记
            for (const info of this.markItemInfos) {
                this.drawSingleMarkItem(info);
            }
            // 6. 构建返回结果：uuid -> BackendMarkInfo[]
            return this.buildDrawMarksResult(marks.mark_list);
        }
        /**
         * 根据 UUID 删除标记
         *
         * @param uuid 标记的UUID
         */
        removeMarkByUuid(uuid) {
            this.renderer.removeMark(uuid);
            // 从缓存中移除
            this.markItemInfos = this.markItemInfos.filter(info => info.id !== uuid);
        }
        /**
         * 清除所有标记
         */
        clearAllMarks() {
            this.renderer.clearAllMarks();
            this.markItemInfos = [];
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
            const BackendMarkType = {
                COMMENT: 2,
                REPLY: 3,
                ORIGIN_COMMENT: 5
            };
            for (const mark of markList) {
                if ([BackendMarkType.COMMENT, BackendMarkType.REPLY, BackendMarkType.ORIGIN_COMMENT].includes(mark.type)) {
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
            const BackendMarkType = {
                REPLY: 3
            };
            for (const mark of markList) {
                if (mark.type !== BackendMarkType.REPLY)
                    continue;
                if (!commentMap.has(mark.id) || !commentMap.has(mark.parent_id) || !commentMap.has(mark.root_id))
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
            const BackendMarkType = {
                LINE: 1,
                COMMENT: 2,
                REPLY: 3,
                ORIGIN_LINE: 4,
                ORIGIN_COMMENT: 5
            };
            for (const mark of markList) {
                const userId = mark.user_id;
                const source = mark.source;
                // 跳过 REPLY 类型和数字类型的 source
                if (typeof source === 'number' || mark.type === BackendMarkType.REPLY)
                    continue;
                // 跳过没有 approx_source 的原始标记
                if ([BackendMarkType.ORIGIN_LINE, BackendMarkType.ORIGIN_COMMENT].includes(mark.type) &&
                    (!mark.approx_source || Object.keys(mark.approx_source).length === 0)) {
                    continue;
                }
                const markSources = source;
                // 查找是否已有相同source的 MarkItemInfo
                let markInfoItem = infoItems.find(infoItem => this.checkMarkSourceIsSame(infoItem.source, markSources));
                if (!markInfoItem) {
                    // 生成 raw_text（如果有 approx_source）
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
                    // 创建新的 MarkItemInfo
                    markInfoItem = {
                        id: generateUUID(),
                        source: markSources,
                        comments: [],
                        stroke: [],
                        approx: mark.approx_source
                    };
                    infoItems.push(markInfoItem);
                }
                // 添加 stroke 或 comment
                if ([BackendMarkType.LINE, BackendMarkType.ORIGIN_LINE].includes(mark.type)) {
                    markInfoItem.stroke.push({ mark_id: mark.id, userId });
                }
                else if ([BackendMarkType.COMMENT, BackendMarkType.ORIGIN_COMMENT].includes(mark.type)) {
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
         * 检查两个 source 是否相同
         */
        checkMarkSourceIsSame(source1, source2) {
            return deepEqual(source1, source2);
        }
        /**
         * 从 approx 信息获取 Range（简化版，实际可能需要更复杂的匹配逻辑）
         */
        getRangeFromApprox(_approx) {
            // 这里只是占位实现，实际需要根据 approx.exact, prefix, suffix 来查找文本
            // 由于这个功能比较复杂，暂时返回 null
            return null;
        }
        /**
         * 渲染单个 MarkItemInfo
         */
        drawSingleMarkItem(info) {
            const hasStroke = info.stroke.length > 0;
            const hasComment = info.comments.length > 0;
            // 确定是否为当前用户的划线
            const userId = info.stroke.length > 0 ? info.stroke[0].userId : info.comments[0]?.userId;
            this.renderer.drawMark(info.id, info.source, hasStroke, hasComment, userId);
        }
        /**
         * 构建返回结果：uuid -> BackendMarkInfo[]
         */
        buildDrawMarksResult(markList) {
            const result = {};
            // 为每个 markItemInfo 构建对应的 BackendMarkInfo 列表
            for (const itemInfo of this.markItemInfos) {
                const relatedMarks = [];
                // 收集所有 stroke 对应的 mark
                for (const stroke of itemInfo.stroke) {
                    if (stroke.mark_id) {
                        const mark = markList.find(m => m.id === stroke.mark_id);
                        if (mark)
                            relatedMarks.push(mark);
                    }
                }
                // 收集所有 comment 对应的 mark（包括子评论）
                for (const comment of itemInfo.comments) {
                    const mark = markList.find(m => m.id === comment.markId);
                    if (mark)
                        relatedMarks.push(mark);
                    // 添加子评论
                    for (const child of comment.children) {
                        const childMark = markList.find(m => m.id === child.markId);
                        if (childMark)
                            relatedMarks.push(childMark);
                    }
                }
                result[itemInfo.id] = relatedMarks;
            }
            return result;
        }
    }

    /**
     * Bridge 通信层
     *
     * 负责与 Kotlin App 端进行通信
     * 支持通过 WebView 的 JavascriptInterface 进行双向通信
     */
    class AppBridge {
        constructor(debug = false) {
            this.debug = debug;
        }
        /**
         * 检查 Bridge 是否可用
         */
        isAvailable() {
            return typeof window.SlaxBridge !== 'undefined';
        }
        /**
         * 向 App 发送日志
         */
        log(message, level = 'info') {
            if (this.debug) {
                console.log(`[SlaxBridge] ${level}: ${message}`);
            }
            if (this.isAvailable()) {
                try {
                    window.SlaxBridge.log(level, message);
                }
                catch (e) {
                    console.error('Failed to send log to bridge:', e);
                }
            }
        }
        /**
         * 用户选择了文本
         * @param data 选择的数据
         * @param position 位置信息（用于显示菜单）
         */
        onTextSelected(data, position) {
            this.log('Text selected', 'debug');
            if (this.isAvailable()) {
                try {
                    window.SlaxBridge.onTextSelected(data, position);
                }
                catch (e) {
                    console.error('Failed to call onTextSelected:', e);
                    this.log(`onTextSelected error: ${e}`, 'error');
                }
            }
        }
        /**
         * 用户点击了已有的划线
         * @param markId 标记ID
         * @param data 标记数据
         * @param position 位置信息（用于显示菜单）
         */
        onMarkClicked(markId, data, position) {
            this.log(`Mark clicked: ${markId}`, 'debug');
            if (this.isAvailable()) {
                try {
                    window.SlaxBridge.onMarkClicked(markId, data, position);
                }
                catch (e) {
                    console.error('Failed to call onMarkClicked:', e);
                    this.log(`onMarkClicked error: ${e}`, 'error');
                }
            }
        }
        /**
         * 标记渲染完成
         * @param markId 标记ID
         * @param success 是否成功
         */
        onMarkRendered(markId, success) {
            this.log(`Mark rendered: ${markId}, success: ${success}`, 'debug');
            if (this.isAvailable()) {
                try {
                    window.SlaxBridge.onMarkRendered(markId, success);
                }
                catch (e) {
                    console.error('Failed to call onMarkRendered:', e);
                    this.log(`onMarkRendered error: ${e}`, 'error');
                }
            }
        }
        /**
         * 错误回调
         * @param error 错误信息
         */
        onError(error) {
            this.log(`Error: ${error}`, 'error');
            if (this.isAvailable()) {
                try {
                    window.SlaxBridge.onError(error);
                }
                catch (e) {
                    console.error('Failed to call onError:', e);
                }
            }
        }
    }

    /**
     * 后端标记类型
     */
    exports.BackendMarkType = void 0;
    (function (BackendMarkType) {
        /** 划线 */
        BackendMarkType[BackendMarkType["LINE"] = 1] = "LINE";
        /** 评论 */
        BackendMarkType[BackendMarkType["COMMENT"] = 2] = "COMMENT";
        /** 回复 */
        BackendMarkType[BackendMarkType["REPLY"] = 3] = "REPLY";
        /** 原始划线（兼容旧版本） */
        BackendMarkType[BackendMarkType["ORIGIN_LINE"] = 4] = "ORIGIN_LINE";
        /** 原始评论（兼容旧版本） */
        BackendMarkType[BackendMarkType["ORIGIN_COMMENT"] = 5] = "ORIGIN_COMMENT";
    })(exports.BackendMarkType || (exports.BackendMarkType = {}));

    /**
     * SlaxSelectionBridge 主类
     *
     * 提供给 Kotlin App 端调用的 JavaScript API
     */
    class SlaxSelectionBridge {
        /**
         * 构造函数
         * @param config 配置
         */
        constructor(config) {
            this.container = config.containerElement;
            this.currentUserId = config.currentUserId;
            this.renderer = new MarkRenderer(this.container, config.currentUserId);
            this.monitor = new SelectionMonitor(this.container);
            this.markManager = new MarkManager(this.container, config.currentUserId);
            this.bridge = new AppBridge(config.debug);
            this.bridge.log('SlaxSelectionBridge initialized', 'info');
            // 添加点击事件监听
            this.setupMarkClickListener();
        }
        /**
         * 开始监听文本选择
         */
        startMonitoring() {
            this.bridge.log('Start monitoring selection', 'debug');
            this.monitor.start((data) => {
                this.bridge.log(`Selection detected: ${data.selection.length} items`, 'debug');
                // 将选择数据序列化并发送给 App
                const jsonData = JSON.stringify({
                    paths: data.paths,
                    approx: data.approx,
                    selection: data.selection.map((item) => {
                        if (item.type === 'text') {
                            return {
                                type: 'text',
                                text: item.text,
                                startOffset: item.startOffset,
                                endOffset: item.endOffset
                            };
                        }
                        else {
                            return {
                                type: 'image',
                                src: item.src
                            };
                        }
                    })
                });
                // 位置信息单独序列化
                const positionData = JSON.stringify(data.position);
                this.bridge.onTextSelected(jsonData, positionData);
            });
        }
        /**
         * 停止监听文本选择
         */
        stopMonitoring() {
            this.bridge.log('Stop monitoring selection', 'debug');
            this.monitor.stop();
        }
        /**
         * 绘制标记
         * @param id 标记ID（如果为空则自动生成）
         * @param paths 标记路径
         * @param isStroke 是否为划线
         * @param hasComment 是否有评论
         * @param userId 用户ID（可选，用于判断是否为当前用户的划线）
         * @returns 标记ID
         */
        drawMark(id, paths, isStroke, hasComment, userId) {
            const markId = id || generateUUID();
            this.bridge.log(`Drawing mark: ${markId}`, 'debug');
            try {
                const success = this.renderer.drawMark(markId, paths, isStroke, hasComment, userId);
                this.bridge.onMarkRendered(markId, success);
                if (!success) {
                    this.bridge.log(`Failed to draw mark: ${markId}`, 'warn');
                }
                return markId;
            }
            catch (error) {
                this.bridge.onError(`Failed to draw mark: ${error}`);
                return markId;
            }
        }
        /**
         * 更新标记
         * @param id 标记ID
         * @param isStroke 是否为划线
         * @param hasComment 是否有评论
         * @param userId 用户ID（可选）
         */
        updateMark(id, isStroke, hasComment, userId) {
            this.bridge.log(`Updating mark: ${id}`, 'debug');
            try {
                this.renderer.updateMark(id, isStroke, hasComment, userId);
            }
            catch (error) {
                this.bridge.onError(`Failed to update mark: ${error}`);
            }
        }
        /**
         * 删除标记
         * @param id 标记ID
         */
        removeMark(id) {
            this.bridge.log(`Removing mark: ${id}`, 'debug');
            try {
                this.renderer.removeMark(id);
            }
            catch (error) {
                this.bridge.onError(`Failed to remove mark: ${error}`);
            }
        }
        /**
         * 高亮标记
         * @param id 标记ID
         */
        highlightMark(id) {
            this.bridge.log(`Highlighting mark: ${id}`, 'debug');
            try {
                this.renderer.highlightMark(id);
            }
            catch (error) {
                this.bridge.onError(`Failed to highlight mark: ${error}`);
            }
        }
        /**
         * 清除所有高亮
         */
        clearHighlights() {
            this.bridge.log('Clearing all highlights', 'debug');
            try {
                this.renderer.clearAllHighlights();
            }
            catch (error) {
                this.bridge.onError(`Failed to clear highlights: ${error}`);
            }
        }
        /**
         * 清除所有标记
         */
        clearAllMarks() {
            this.bridge.log('Clearing all marks', 'debug');
            try {
                this.renderer.clearAllMarks();
                this.markManager.clearAllMarks();
            }
            catch (error) {
                this.bridge.onError(`Failed to clear all marks: ${error}`);
            }
        }
        /**
         * 获取所有标记ID
         */
        getAllMarkIds() {
            try {
                return this.renderer.getAllMarkIds();
            }
            catch (error) {
                this.bridge.onError(`Failed to get all mark IDs: ${error}`);
                return [];
            }
        }
        /**
         * 绘制多个标记（从后端 MarkDetail 数据）
         *
         * @param markDetailJson MarkDetail 的 JSON 字符串
         * @returns DrawMarksResult 的 JSON 字符串：{ uuid: BackendMarkInfo[] }
         *
         * @example
         * ```javascript
         * const markDetail = {
         *   user_list: { "1": { user_id: 1, username: "Alice", avatar: "..." } },
         *   mark_list: [
         *     { id: 101, user_id: 1, type: 1, source: [...], ... },
         *     { id: 102, user_id: 1, type: 2, source: [...], ... }
         *   ]
         * }
         * const result = bridge.drawMarks(JSON.stringify(markDetail))
         * // result: { "uuid-123": [markInfo1, markInfo2], "uuid-456": [...] }
         * ```
         */
        drawMarks(markDetailJson) {
            this.bridge.log('Drawing marks from MarkDetail', 'debug');
            try {
                const markDetail = JSON.parse(markDetailJson);
                const result = this.markManager.drawMarks(markDetail);
                return JSON.stringify(result);
            }
            catch (error) {
                this.bridge.onError(`Failed to draw marks: ${error}`);
                return JSON.stringify({});
            }
        }
        /**
         * 根据 UUID 删除标记
         *
         * @param uuid 标记的本地 UUID（由 drawMarks 返回）
         *
         * @example
         * ```javascript
         * bridge.removeMarkByUuid("uuid-123")
         * ```
         */
        removeMarkByUuid(uuid) {
            this.bridge.log(`Removing mark by UUID: ${uuid}`, 'debug');
            try {
                this.markManager.removeMarkByUuid(uuid);
            }
            catch (error) {
                this.bridge.onError(`Failed to remove mark by UUID: ${error}`);
            }
        }
        /**
         * 清除当前选择
         */
        clearSelection() {
            this.bridge.log('Clearing selection', 'debug');
            this.monitor.clearSelection();
        }
        /**
         * 设置标记点击处理器
         */
        setMarkClickHandler(handler) {
            this.markClickHandler = handler;
        }
        /**
         * 设置当前用户ID
         */
        setCurrentUserId(userId) {
            this.currentUserId = userId;
            this.renderer = new MarkRenderer(this.container, userId);
            this.markManager = new MarkManager(this.container, userId);
        }
        /**
         * 设置标记点击监听
         */
        setupMarkClickListener() {
            this.container.addEventListener('click', (event) => {
                const target = event.target;
                // 检查是否点击了标记
                let markElement = target;
                while (markElement && markElement !== this.container) {
                    if (markElement.tagName === 'SLAX-MARK' && markElement.dataset.uuid) {
                        const markId = markElement.dataset.uuid;
                        this.bridge.log(`Mark clicked: ${markId}`, 'debug');
                        // 获取标记的完整数据
                        const markData = JSON.stringify({
                            id: markId,
                            text: markElement.textContent || '',
                            classList: Array.from(markElement.classList)
                        });
                        // 计算标记的位置信息
                        const position = this.getMarkPosition(markElement, event);
                        const positionData = JSON.stringify(position);
                        this.bridge.onMarkClicked(markId, markData, positionData);
                        // 调用自定义处理器
                        if (this.markClickHandler) {
                            this.markClickHandler(markId, markElement);
                        }
                        event.stopPropagation();
                        break;
                    }
                    markElement = markElement.parentElement;
                }
            });
        }
        /**
         * 获取标记的位置信息
         * 参考 slax-reader-web 的实现
         */
        getMarkPosition(markElement, event) {
            // 获取标记元素的边界矩形
            const markRect = markElement.getBoundingClientRect();
            // 获取容器的边界矩形
            const containerRect = this.container.getBoundingClientRect();
            // 获取点击位置
            const clientX = event.clientX;
            const clientY = event.clientY;
            // 计算相对于容器的位置
            const x = clientX - containerRect.left;
            const y = clientY - containerRect.top;
            // 返回完整的位置信息
            return {
                x,
                y,
                width: markRect.width,
                height: markRect.height,
                top: markRect.top - containerRect.top,
                left: markRect.left - containerRect.left,
                right: markRect.right - containerRect.left,
                bottom: markRect.bottom - containerRect.top
            };
        }
        /**
         * 销毁实例
         */
        destroy() {
            this.bridge.log('Destroying SlaxSelectionBridge', 'info');
            this.stopMonitoring();
            this.clearAllMarks();
        }
    }

    exports.SlaxSelectionBridge = SlaxSelectionBridge;

    return exports;

})({});
var SlaxSelectionBridge = SlaxSelectionBridgeExports.SlaxSelectionBridge;
//# sourceMappingURL=slax-selection-bridge.js.map
