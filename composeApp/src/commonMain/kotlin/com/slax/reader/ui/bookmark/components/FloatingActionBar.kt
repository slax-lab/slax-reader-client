package com.slax.reader.ui.bookmark.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_floating_panel_archieve
import slax_reader_client.composeapp.generated.resources.ic_floating_panel_more
import slax_reader_client.composeapp.generated.resources.ic_floating_panel_star

@Composable
fun FloatingActionBar(
    modifier: Modifier = Modifier,
    onMoreClick: () -> Unit = {}
) {
    // println("[watch][UI] recomposition FloatingActionBar")

    Box(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 10.dp)
                .shadow(
                    elevation = 40.dp,
                    shape = RoundedCornerShape(25.dp),
                    ambientColor = Color.Black.copy(alpha = 1.0f),
                    spotColor = Color.Black.copy(alpha = 1.0f)
                )
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(25.dp))
                    .border(
                        width = 1.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(25.dp)
                    )
                    .background(Color(0xFFF5F5F5))

            ) {
                Surface(
                    onClick = { /* 点击事件 */ },
                    modifier = Modifier
                        .size(50.dp),
                    color = Color.Transparent,
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_floating_panel_star),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Surface(
                    onClick = { /* 点击事件 */ },
                    modifier = Modifier
                        .size(50.dp),
                    color = Color.Transparent,
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_floating_panel_archieve),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Box(modifier = Modifier.width(12.dp))

            Surface(
                onClick = onMoreClick,
                modifier = Modifier
                    .size(50.dp),
                color = Color(0xFFF5F5F5),
                shape = RoundedCornerShape(25.dp),
                border = BorderStroke(1.dp, Color.White)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_floating_panel_more),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
