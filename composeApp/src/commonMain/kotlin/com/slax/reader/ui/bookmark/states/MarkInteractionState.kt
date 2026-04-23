package com.slax.reader.ui.bookmark.states

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slax.reader.utils.BridgeMarkItemInfo

@Stable
class MarkInteractionState {
    var selectedText by mutableStateOf("")
        private set

    var selectionY by mutableFloatStateOf(0f)
        private set

    var menuVisible by mutableStateOf(false)
        private set

    var panelVisible by mutableStateOf(false)
        private set

    /** 面板打开时是否自动聚焦输入框（选区→评论 时 true，点击已有 mark 时 false） */
    var shouldAutoFocus by mutableStateOf(false)
        private set

    var selectedMark by mutableStateOf<BridgeMarkItemInfo?>(null)

    /** 文本选中时预捕获的选区数据（source/approx），供"评论"按钮使用 */
    var capturedSelectionMark by mutableStateOf<BridgeMarkItemInfo?>(null)

    fun onTextSelected(text: String, y: Float, markInfo: BridgeMarkItemInfo? = null) {
        selectedText = text
        selectionY = y
        if (!panelVisible) {
            menuVisible = true
        }
        capturedSelectionMark = markInfo
    }

    fun onTextDeselected() {
        menuVisible = false
        if (!panelVisible) {
            selectedText = ""
        }
        selectionY = 0f
        capturedSelectionMark = null
    }

    fun onMarkItemInfosChanged(markItemInfos: List<BridgeMarkItemInfo>) {
        val current = selectedMark ?: return

        val matched = if (current.id.isBlank()) {
            markItemInfos.find { it.source == current.source || it.approx == current.approx }
        } else {
            markItemInfos.find { it.id == current.id }
        } ?: return

        if (matched.stroke != current.stroke || matched.comments != current.comments) {
            selectedMark = matched
        }
    }

    fun onMarkClicked(text: String, mark: BridgeMarkItemInfo) {
        selectedText = text
        selectedMark = mark
        panelVisible = true
        shouldAutoFocus = false
    }

    /** 选区→评论：打开面板并自动聚焦输入框 */
    fun openPanelForNewComment(text: String, mark: BridgeMarkItemInfo) {
        selectedText = text
        selectedMark = mark
        panelVisible = true
        shouldAutoFocus = true
    }

    fun dismissPanel() {
        panelVisible = false
        selectedMark = null
        shouldAutoFocus = false
    }

    fun dismissMenu() {
        menuVisible = false
    }

    fun showPanel() {
        panelVisible = true
    }
}

val LocalMarkInteraction = compositionLocalOf<MarkInteractionState> {
    error("LocalMarkInteraction not provided")
}
