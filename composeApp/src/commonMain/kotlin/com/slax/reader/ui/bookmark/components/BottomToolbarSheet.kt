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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.slax.reader.const.component.rememberDismissableVisibility
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import com.slax.reader.ui.bookmark.states.BookmarkOverlay
import com.slax.reader.utils.i18n
import org.jetbrains.compose.resources.DrawableResource
import org.koin.compose.viewmodel.koinViewModel
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
fun BottomToolbarSheet() {
    println("[watch][UI] recomposition BottomToolbarSheet")
    val viewModel = koinViewModel<BookmarkDetailViewModel>()
    val detailState by viewModel.bookmarkDelegate.bookmarkDetailState.collectAsState()

    val toolbarPages = remember(detailState.isStarred, detailState.isArchived) {
        listOf(
            listOf(
                ToolbarIcon("summary", "detail_toolbar_summary".i18n(), Res.drawable.ic_bottom_panel_summary, proFeature = true),
                ToolbarIcon(
                    "star",
                    "detail_toolbar_star".i18n(),
                    if (detailState.isStarred) Res.drawable.ic_bottom_panel_starred else Res.drawable.ic_bottom_panel_star
                ),
                ToolbarIcon(
                    "archive",
                    "detail_toolbar_archive".i18n(),
                    if (detailState.isArchived) Res.drawable.ic_bottom_panel_archieved else Res.drawable.ic_bottom_panel_archieve
                ),
//                ToolbarIcon("underline", "划线", Res.drawable.ic_bottom_panel_underline),
//                ToolbarIcon("comment", "评论", Res.drawable.ic_bottom_panel_comment),
                ToolbarIcon("edit_title", "detail_toolbar_edit_title".i18n(), Res.drawable.ic_bottom_panel_edittitle),
                ToolbarIcon("feedback", "detail_toolbar_feedback".i18n(), Res.drawable.ic_bottom_panel_feedback),
//                ToolbarIcon("share", "分享", Res.drawable.ic_bottom_panel_share)
            )
//            listOf(
////                ToolbarIcon("delete", "删除", Res.drawable.ic_bottom_panel_delete),
//            )
        )
    }

    val (visible, dismiss) = rememberDismissableVisibility(
        scope = viewModel.viewModelScope,
        animationDuration = 300L,
        onDismissRequest = { viewModel.overlayDelegate.dismissOverlay(BookmarkOverlay.Toolbar) }
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
                        viewModel.onToolbarIconClick(pageId)
                        dismiss()
                    },
                    modifier = Modifier.padding(top = 30.dp, bottom = 50.dp)
                )
            }
        }
    }
}