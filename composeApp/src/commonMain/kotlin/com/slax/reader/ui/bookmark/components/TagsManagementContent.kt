package com.slax.reader.ui.bookmark.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slax.reader.data.database.model.UserTag

@Composable
fun TagsManagementContent(
    selectedTags: List<UserTag>,
    unselectedTags: List<UserTag>,
    onTagAdd: (UserTag) -> Unit,
    onTagRemove: (UserTag) -> Unit,
    onCreateNewTag: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    println("[watch][UI] recomposition TagsManagementContent")

    Column(
        modifier = Modifier.padding(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = "取消",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onDismiss()
                    },
                style = TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF333333)
                )
            )

            Text(
                text = "标签",
                modifier = Modifier.align(Alignment.Center),
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp,
                    color = Color(0xFF0F1419)
                )
            )

            Text(
                text = "确定",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onConfirm()
                    },
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp,
                    color = Color(0xFF16b998)
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(0.5.dp)
                .background(Color(0x14333333))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            if (selectedTags.isNotEmpty()) {
                Text(
                    text = "已添加",
                    modifier = Modifier.padding(bottom = 12.dp),
                    style = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFF999999)
                    )
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 30.dp)
                ) {
                    selectedTags.forEach { tag ->
                        key(tag.id) {
                            TagItem(
                                tag = tag.tag_name,
                                onClick = {},
                                showDeleteButton = true,
                                onDelete = { onTagRemove(tag) },
                                isLargeStyle = true
                            )
                        }
                    }
                }
            }

            Text(
                text = "可添加",
                modifier = Modifier.padding(bottom = 12.dp),
                style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF999999)
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .border(
                        width = 0.5.dp,
                        color = Color(0x140F1419),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .background(
                        color = Color(0xF2F5F5F3),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onCreateNewTag() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "创建新标签",
                    style = TextStyle(
                        fontSize = 15.sp,
                        color = Color(0x99A28D64),
                        lineHeight = 21.sp
                    )
                )
            }

            if (unselectedTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    unselectedTags.forEach { tag ->
                        key(tag.id) {
                            TagItem(
                                tag = tag.tag_name,
                                onClick = { onTagAdd(tag) },
                                isLargeStyle = true
                            )
                        }
                    }
                }
            }
        }
    }
}
