package com.slax.reader.ui.bookmark.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slax.reader.ui.bookmark.OverviewViewBounds
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_xs_blue_down_arrow

@Composable
fun OverviewView(
    modifier: Modifier = Modifier,
    onExpand: () -> Unit = {},
    onBoundsChanged: (OverviewViewBounds) -> Unit = {}
) {
    val density = LocalDensity.current

    Surface(
        modifier = modifier
            .then(Modifier.fillMaxWidth())
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInRoot()
                with(density) {
                    onBoundsChanged(
                        OverviewViewBounds(
                            x = position.x,
                            y = position.y,
                            width = coordinates.size.width.toFloat(),
                            height = coordinates.size.height.toFloat()
                        )
                    )
                }
            },
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF5F5F3)
    ) {
        Column() {
            Text("全文概要", modifier = Modifier.padding(12.dp), style = TextStyle())
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .height(1.dp)
                    .background(Color(0x14333333))
            )

            Surface(
                onClick = onExpand,
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "展开全部",
                            style = TextStyle(fontSize = 12.sp, lineHeight = 16.5.sp, color = Color(0xFF5490C2))
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
    }
}
