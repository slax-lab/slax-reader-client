package com.slax.reader.ui.bookmark.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TagItem(
    tag: String,
    onClick: () -> Unit,
    showDeleteButton: Boolean = false,
    onDelete: (() -> Unit)? = null,
    isLargeStyle: Boolean = false  // 是否使用大尺寸样式
) {
    // println("[watch][UI] recomposition TagItem")

    // 根据样式选择不同的尺寸参数
    val height = if (isLargeStyle) 30.dp else 21.dp
    val fontSize = if (isLargeStyle) 15.sp else 12.sp
    val lineHeight = if (isLargeStyle) 21.sp else 15.sp
    val horizontalPadding = if (isLargeStyle) 6.dp else 4.dp
    val endPadding = if (isLargeStyle && showDeleteButton) 4.dp else if (showDeleteButton) 2.dp else horizontalPadding

    Box(
        modifier = Modifier
            .height(height) // 动态高度
            .clip(RoundedCornerShape(3.dp))
            .border(
                width = 1.dp,
                color = Color(0xFFE4D6BA),
                shape = RoundedCornerShape(3.dp)
            )
            .then(
                if (showDeleteButton) {
                    // 有删除按钮时不需要整体可点击
                    Modifier.padding(start = horizontalPadding, end = endPadding)
                } else {
                    // 没有删除按钮时整体可点击
                    Modifier
                        .clickable(
                            onClick = onClick,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                        .padding(horizontal = horizontalPadding)
                }
            ),
        contentAlignment = if (showDeleteButton) Alignment.CenterStart else Alignment.Center
    ) {
        if (showDeleteButton && onDelete != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = tag,
                    style = TextStyle(
                        color = Color(0xFFA28D64),
                        fontSize = fontSize,
                        lineHeight = lineHeight
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(12.dp)
                        .background(Color(0x140F1419))
                )

                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .clickable(
                            onClick = onDelete,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "×",
                        style = TextStyle(
                            color = Color(0xFFA28D64),
                            fontSize = 14.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            }
        } else {
            // 普通标签布局
            Text(
                text = tag,
                style = TextStyle(
                    color = Color(0xFFA28D64),
                    fontSize = fontSize,
                    lineHeight = lineHeight
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
