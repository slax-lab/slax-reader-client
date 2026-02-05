package com.slax.reader.ui.inbox.compenents

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slax.reader.utils.i18n
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_outline_empty_inboxlist_icon

/**
 * 空数据引导组件
 */
@NonRestartableComposable
@Composable
fun EmptyView() {
    val tips = listOf(
        "empty_inbox_tip_1".i18n(),
        "empty_inbox_tip_2".i18n()
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .offset(y = (-65).dp)
                .padding(horizontal = 30.dp)
                .widthIn(max = 315.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_outline_empty_inboxlist_icon),
                contentDescription = "empty_inbox_icon_desc".i18n(),
                modifier = Modifier.size(24.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "empty_inbox_title".i18n(),
                style = TextStyle(
                    fontSize = 18.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF403735)
                )
            )

            Spacer(modifier = Modifier.height(34.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(Color(0x14333333))
            )

            Spacer(modifier = Modifier.height(34.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 25.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                tips.forEachIndexed { index, tip ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = "${index + 1}.",
                            style = TextStyle(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = Color(0xFF999999)
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = tip,
                            style = TextStyle(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = Color(0xFF999999)
                            )
                        )
                    }
                }
            }
        }
    }
}