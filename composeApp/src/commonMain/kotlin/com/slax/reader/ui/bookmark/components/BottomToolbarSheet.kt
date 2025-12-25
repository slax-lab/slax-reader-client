package com.slax.reader.ui.bookmark.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.slax.reader.const.component.rememberDismissableVisibility
import com.slax.reader.data.database.model.UserBookmark
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import com.slax.reader.utils.i18n
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import slax_reader_client.composeapp.generated.resources.*

data class ToolbarIcon(
    val id: String,
    val label: String,
    val iconRes: DrawableResource? = null,
    val proFeature: Boolean = false
)

/**
 * 底部弹出工具栏
 * 带显示/隐藏动画，点击外部区域隐藏
 */
@Composable
fun BottomToolbarSheet(
    detail: UserBookmark,
    detailView: BookmarkDetailViewModel,
    onDismissRequest: () -> Unit,
    onSubscriptionRequired: (() -> Unit)? = null,
    onIconClick: (pageId: String, iconIndex: Int) -> Unit
) {
    println("[watch][UI] recomposition BottomToolbarSheet")

    val toolbarPages = remember(detail.isStarred, detail.archiveStatus) {
        listOf(
            listOf(
//                ToolbarIcon("chat", "Chat", Res.drawable.ic_bottom_panel_chatbot),
                ToolbarIcon("summary", "detail_toolbar_summary".i18n(), Res.drawable.ic_bottom_panel_summary, proFeature = true),
                ToolbarIcon(
                    "star",
                    "detail_toolbar_star".i18n(),
                    if (detail.isStarred == 1) Res.drawable.ic_bottom_panel_starred else Res.drawable.ic_bottom_panel_star
                ),
                ToolbarIcon(
                    "archive",
                    "detail_toolbar_archive".i18n(),
                    if (detail.archiveStatus == 1) Res.drawable.ic_bottom_panel_archieved else Res.drawable.ic_bottom_panel_archieve
                ),
//                ToolbarIcon("underline", "划线", Res.drawable.ic_bottom_panel_underline),
//                ToolbarIcon("comment", "评论", Res.drawable.ic_bottom_panel_comment),
                ToolbarIcon("edit_title", "detail_toolbar_edit_title".i18n(), Res.drawable.ic_bottom_panel_edittitle),
//                ToolbarIcon("share", "分享", Res.drawable.ic_bottom_panel_share)
            )
//            listOf(
////                ToolbarIcon("feedback", "反馈", Res.drawable.ic_bottom_panel_feedback),
////                ToolbarIcon("delete", "删除", Res.drawable.ic_bottom_panel_delete),
//            )
        )
    }

    val (visible, dismiss) = rememberDismissableVisibility(
        scope = detailView.viewModelScope,
        animationDuration = 300L,
        onDismissRequest = onDismissRequest
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.0f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    dismiss()
                }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            )
        ) {
            Surface(
                color = Color(0xFFF5F5F3),
                border = BorderStroke(0.5.dp, Color(0x140F1419)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* 阻止点击事件穿透 */ },
            ) {
                PagerToolbar(
                    pages = toolbarPages,
                    onIconClick = { pageId, iconIndex ->
                        detailView.viewModelScope.launch {
                            if (pageId == "star") {
                                detailView.toggleStar(detail.isStarred != 1)
                            } else if (pageId == "archive") {
                                detailView.toggleArchive(detail.archiveStatus != 1)
                            }

                            if (pageId == "summary") {
                                val isSubscribed = detailView.checkUserIsSubscribed()
                                if (!isSubscribed) {
                                    dismiss()
                                    if (onSubscriptionRequired != null) {
                                        onSubscriptionRequired()
                                    }
                                    return@launch
                                }
                            }

                            onIconClick(pageId, iconIndex)
                            dismiss()
                        }
                    },
                    modifier = Modifier.padding(top = 30.dp)
                )
            }
        }
    }
}