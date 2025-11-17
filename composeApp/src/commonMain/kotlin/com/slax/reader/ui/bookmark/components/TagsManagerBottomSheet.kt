package com.slax.reader.ui.bookmark.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.slax.reader.data.database.model.UserTag
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun TagsManageBottomSheet(
    detailViewModel: BookmarkDetailViewModel,
    visible: Boolean,
    onDismissRequest: () -> Unit,
    enableDrag: Boolean = false,
    onConfirm: (List<UserTag>) -> Unit = {}
) {
    println("[watch][UI] recomposition TagsManageBottomSheet")

    val availableTags by detailViewModel.userTagList.collectAsState(emptyList())
    val remoteSelectedTags by detailViewModel.selectedTagList.collectAsState()

    val addedTags = remember { mutableStateListOf<UserTag>() }
    val removedTags = remember { mutableStateListOf<UserTag>() }
    var showCreatingScreen by remember { mutableStateOf(false) }

    var internalVisible by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        internalVisible = visible
    }

    LaunchedEffect(internalVisible) {
        if (!internalVisible) {
            delay(300)
            onDismissRequest()
        }
    }

    LaunchedEffect(remoteSelectedTags) {
        removedTags.removeAll { it !in remoteSelectedTags }
        addedTags.removeAll { it in remoteSelectedTags }
    }

    val selectedTags by remember {
        derivedStateOf {
            (remoteSelectedTags + addedTags).filterNot { it in removedTags }.distinctBy { it.id }
        }
    }

    val unselectedTags by remember {
        derivedStateOf {
            availableTags.filterNot { tag -> selectedTags.any { it.id == tag.id } }
        }
    }

    val density = LocalDensity.current
    var offsetY by remember { mutableFloatStateOf(0f) }


    AnimatedVisibility(
        visible = internalVisible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (showCreatingScreen) {
                        showCreatingScreen = false
                    } else {
                        internalVisible = false
                    }
                }
        )
    }


    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {

        AnimatedVisibility(
            visible = internalVisible,
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
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    // 最多占据65%的屏幕
                    .fillMaxHeight(0.65f)
                    .padding(0.dp)
                    .offset { IntOffset(0, offsetY.roundToInt()) }
                    .then(
                        if (enableDrag) {
                            Modifier
                                .draggable(
                                    orientation = Orientation.Vertical,
                                    state = rememberDraggableState { delta ->
                                        offsetY = (offsetY + delta).coerceAtLeast(0f)
                                    },
                                    onDragStopped = {
                                        if (offsetY > with(density) { 100.dp.toPx() }) {
                                            internalVisible = false
                                        } else {
                                            offsetY = 0f
                                        }
                                    }
                                )
                        } else {
                            Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {}
                        }
                    ),
                shadowElevation = 8.dp
            ) {
                if (showCreatingScreen) {
                    TagCreatingScreen(
                        detailViewModel = detailViewModel,
                        onTagCreated = { newTag ->
                            removedTags.remove(newTag)
                            if (!addedTags.any { it.id == newTag.id }) {
                                addedTags.add(newTag)
                            }
                        },
                        onBack = {
                            showCreatingScreen = false
                        }
                    )
                } else {
                    TagsManagementContent(
                        selectedTags = selectedTags,
                        unselectedTags = unselectedTags,
                        onTagAdd = { tag ->
                            removedTags.remove(tag)
                            if (!addedTags.any { it.id == tag.id } && tag !in remoteSelectedTags) {
                                addedTags.add(tag)
                            }
                        },
                        onTagRemove = { tag ->
                            if (addedTags.removeAll { it.id == tag.id }) {
                            } else if (tag in remoteSelectedTags && tag !in removedTags) {
                                removedTags.add(tag)
                            }
                        },
                        onCreateNewTag = {
                            showCreatingScreen = true
                        },
                        onConfirm = {
                            onConfirm(selectedTags)
                            onDismissRequest()
                        },
                        onDismiss = {
                            internalVisible = false
                        }
                    )
                }
            }
        }
    }
}
