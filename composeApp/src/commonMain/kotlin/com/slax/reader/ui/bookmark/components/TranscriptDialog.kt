package com.slax.reader.ui.bookmark.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import com.slax.reader.ui.bookmark.YoutubeCue
import com.slax.reader.utils.i18n
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_outline_dialog_close

private fun formatCueTime(sec: Int): String {
    val s = (sec % 60).toString().padStart(2, '0')
    val totalM = sec / 60
    if (totalM < 60) return "$totalM:$s"
    val h = totalM / 60
    val m = (totalM % 60).toString().padStart(2, '0')
    return "$h:$m:$s"
}

@Composable
fun TranscriptDialog() {
    val viewModel = koinViewModel<BookmarkDetailViewModel>()
    val visible by viewModel.transcriptVisible.collectAsState()
    val cues by viewModel.youtubeCues.collectAsState()
    val currentTime by viewModel.youtubeCurrentTime.collectAsState()

    // 打开时请求当前播放时间，用于自动定位到当前行
    LaunchedEffect(visible) {
        if (visible) viewModel.requestYoutubeCurrentTime()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { viewModel.hideTranscript() }
        )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300))
        ) {
            TranscriptSheet(
                cues = cues,
                currentTime = currentTime,
                // 点击某句：跳转并收起字幕页
                onSeek = {
                    viewModel.requestSeekYoutube(it)
                    viewModel.hideTranscript()
                },
                onClose = { viewModel.hideTranscript() }
            )
        }
    }
}

/** 返回当前应高亮/定位的字幕行下标（最后一个起始秒 <= now 的行）；无则 -1 */
private fun activeIndexFor(cues: List<YoutubeCue>, now: Int): Int {
    if (now < 0) return -1
    var idx = -1
    for (i in cues.indices) {
        if (cues[i].t <= now) idx = i else break
    }
    return idx
}

@Composable
private fun TranscriptSheet(cues: List<YoutubeCue>, currentTime: Int, onSeek: (Int) -> Unit, onClose: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .statusBarsPadding()
            .padding(top = 36.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* 阻止点击穿透 */ }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            Text(
                text = "transcript_dialog_title".i18n(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp)
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (cues.isEmpty()) {
                    Text(
                        text = "transcript_dialog_empty".i18n(),
                        fontSize = 14.sp,
                        color = Color(0xFF999999),
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    val listState = rememberLazyListState()
                    val activeIndex = activeIndexFor(cues, currentTime)

                    // 拿到当前时间后，自动滚动并把当前行尽量居中
                    LaunchedEffect(activeIndex) {
                        if (activeIndex >= 0) {
                            listState.scrollToItem(activeIndex, scrollOffset = -200)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                    ) {
                        itemsIndexed(cues) { index, cue ->
                            CueRow(cue = cue, active = index == activeIndex, onClick = { onSeek(cue.t) })
                        }
                        item { Spacer(modifier = Modifier.height(40.dp)) }
                    }
                }
            }

            val closeInteraction = remember { MutableInteractionSource() }
            val isClosePressed by closeInteraction.collectIsPressedAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .alpha(if (isClosePressed) 0.5f else 1f)
                    .clickable(
                        interactionSource = closeInteraction,
                        indication = null,
                        onClick = onClose
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_outline_dialog_close),
                    contentDescription = "btn_close".i18n(),
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "btn_close".i18n(),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color(0xCC333333)
                )
            }
        }
    }
}

@Composable
private fun CueRow(cue: YoutubeCue, active: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) Color(0x148AD8A8) else Color.Transparent)
            .alpha(if (isPressed) 0.6f else 1f)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = formatCueTime(cue.t),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2F9E5E),
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0x298AD8A8))
                .border(1.dp, Color(0x808AD8A8), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
        Text(
            text = cue.text,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
            color = Color(0xFF333333),
            modifier = Modifier.weight(1f)
        )
    }
}

