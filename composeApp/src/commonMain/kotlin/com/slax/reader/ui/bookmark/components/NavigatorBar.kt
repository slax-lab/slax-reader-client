package com.slax.reader.ui.bookmark.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import com.slax.reader.ui.bookmark.LocalToolbarVisible
import com.slax.reader.utils.i18n
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_sm_back

@Composable
fun NavigatorBar() {
    println("[watch][UI] recomposition NavigatorBar")
    val viewModel = koinViewModel<BookmarkDetailViewModel>()
    val visible by LocalToolbarVisible.current

    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else (-100).dp,
        animationSpec = tween(durationMillis = 300)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(44.dp)
            .graphicsLayer {
                translationY = offsetY.toPx()
            }
            .background(Color.Transparent)
            .padding(horizontal = 20.dp)
    ) {
        // 左侧返回按钮
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(24.dp)
                .alpha(if (isPressed) 0.5f else 1f)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    viewModel.requestNavigateBack()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_sm_back),
                contentDescription = "btn_back".i18n(),
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )
        }

        // 中间标题
//        if (false) {
//            Text(
//                text = "",
//                fontSize = 18.sp,
//                fontWeight = FontWeight.Medium,
//                color = Color(0xFF0F1419),
//                textAlign = TextAlign.Center,
//                modifier = Modifier.align(Alignment.Center)
//            )
//        }

//        // 右侧操作按钮
//        Row(
//            modifier = Modifier.align(Alignment.CenterEnd),
//            horizontalArrangement = Arrangement.spacedBy(8.dp),
//            verticalAlignment = Alignment.CenterVertically,
//            content = {  }
//        )
    }
}
