package com.slax.reader.ui.bookmark.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import com.slax.reader.ui.bookmark.states.BookmarkOverlay
import com.slax.reader.ui.bookmark.states.OverviewViewBounds
import com.slax.reader.utils.i18n
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_xs_blue_down_arrow

@Composable
fun OverviewView(
    modifier: Modifier = Modifier,
) {
    println("[watch][UI] recomposition OverviewView")
    val viewModel = koinViewModel<BookmarkDetailViewModel>()

    LaunchedEffect(Unit) {
        viewModel.loadOverview()
    }

    val overviewState by viewModel.overviewDelegate.overviewState.collectAsState()
    val content = overviewState.overview

    if (content.isEmpty()) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInRoot()
                viewModel.overviewDelegate.updateBounds(
                    OverviewViewBounds(
                        x = position.x,
                        y = position.y,
                        width = coordinates.size.width.toFloat(),
                        height = coordinates.size.height.toFloat()
                    )
                )
            },
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF5F5F3)
    ) {
        Column {
            OverviewContent(content = content)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .height(0.5.dp)
                    .background(Color(0x14333333))
            )

            ExpandButton(onClick = { viewModel.overlayDelegate.showOverlay(BookmarkOverlay.Overview) })
        }
    }
}

@Composable
private fun OverviewContent(content: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        val annotatedText = remember(content) {
            buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color(0xFF999999))) {
                    append("overview_prefix".i18n())
                }
                append(content)
            }
        }

        Text(
            text = annotatedText,
            modifier = Modifier.fillMaxWidth(),
            style = TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color(0xFF333333)
            ),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ExpandButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth().height(45.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "overview_expand_all".i18n(),
                    style = TextStyle(
                        fontSize = 12.sp,
                        lineHeight = 16.5.sp,
                        color = Color(0xFF5490C2)
                    )
                )
                Icon(
                    painter = painterResource(Res.drawable.ic_xs_blue_down_arrow),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(8.dp)
                )
            }
        }
    }
}
