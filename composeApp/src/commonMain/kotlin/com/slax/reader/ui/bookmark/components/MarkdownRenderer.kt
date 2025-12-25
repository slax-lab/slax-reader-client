package com.slax.reader.ui.bookmark.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.text.TextLinkStyles
import com.slax.reader.ui.bookmark.BookmarkDetailViewModel
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.rememberMarkdownState


@Composable
fun MarkdownRenderer(
    detailViewModel: BookmarkDetailViewModel,
    onLinkClick: (String) -> Unit = {}
) {
    val outlineState by detailViewModel.outlineState.collectAsState()

    val markdownState = rememberMarkdownState(
        content = outlineState.outline,
        retainState = true,
        immediate = false
    )

    val customUriHandler = remember {
        object : UriHandler {
            override fun openUri(uri: String) {
                onLinkClick(uri)
            }
        }
    }

    CompositionLocalProvider(LocalUriHandler provides customUriHandler) {
        Markdown(
            markdownState = markdownState,
            modifier = Modifier.fillMaxWidth().preferredFrameRate(FrameRateCategory.High),
            colors = markdownColor(
                text = Color(0xFF333333),
                codeBackground = Color(0xFFF5F5F5),
                dividerColor = Color(0xFFE0E0E0)
            ),
            typography = markdownTypography(
                h1 = TextStyle(
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F1419)
                ),
                h2 = TextStyle(
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F1419)
                ),
                h3 = TextStyle(
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F1419)
                ),
                h4 = TextStyle(
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F1419)
                ),
                h5 = TextStyle(
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F1419)
                ),
                h6 = TextStyle(
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F1419)
                ),
                paragraph = TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF333333)
                ),
                text = TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF333333)
                ),
                ordered = TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF333333)
                ),
                bullet = TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF333333)
                ),
                list = TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF333333)
                ),
                quote = TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF666666),
                    fontWeight = FontWeight.Normal
                ),
                code = TextStyle(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFF333333)
                ),
                inlineCode = TextStyle(
                    fontSize = 14.sp,
                    color = Color(0xFFE01E5A),
                    background = Color(0xFFF5F5F5)
                ),
                textLink = TextLinkStyles(
                    style = SpanStyle(
                        color = Color(0xFF4d4d4d),
                        background = Color(0x1F16B998),
                        fontSize = 12.sp
                    ),
                    hoveredStyle = SpanStyle(
                        color = Color(0xFF333333),
                        background = Color(0x3016B998)
                    ),
                    pressedStyle = SpanStyle(
                        color = Color(0xFF333333),
                        background = Color(0x4016B998)
                    )
                )
            )
        )
    }
}
