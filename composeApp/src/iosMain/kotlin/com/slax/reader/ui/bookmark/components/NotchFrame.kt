package com.slax.reader.ui.bookmark.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.slax.reader.utils.getNotchInfo
import com.slax.reader.utils.isDynamicIsland
import com.slax.reader.utils.isNotchOrDynamicIsland
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.global_default_avatar

@OptIn(ExperimentalForeignApi::class)
@Composable
fun NotchBorderOverlay() {
    val isDynamicIslandDevice = remember { isDynamicIsland() }
    val isNotchDevice = remember { isNotchOrDynamicIsland() && !isDynamicIslandDevice }

    if (!isDynamicIslandDevice && !isNotchDevice) {
        return
    }

    val notchInfo = remember { getNotchInfo() }
    val logoPainter = painterResource(Res.drawable.global_default_avatar)
    val density = LocalDensity.current

    notchInfo?.let { info ->
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width

            info.frame.useContents {
                val x = origin.x.toFloat()
                val y = origin.y.toFloat()
                val width = size.width.toFloat()
                val height = size.height.toFloat()

                with(density) {
                    val topLeftPx = Offset(x.dp.toPx(), y.dp.toPx())
                    val sizePx = Size(width.dp.toPx(), height.dp.toPx())

                    if (isDynamicIslandDevice) {
                        // ===== 灵动岛胶囊形状 =====
                        val cornerRadiusPx = sizePx.height / 2f
                        val strokeWidth = 3.dp.toPx()

                        drawRoundRect(
                            color = Color.Magenta,
                            topLeft = topLeftPx,
                            size = sizePx,
                            cornerRadius = CornerRadius(cornerRadiusPx),
                            style = Stroke(width = strokeWidth)
                        )
                    } else if (isNotchDevice) {
                        // ===== 刘海轮廓 =====
                        val strokeWidth = 2.dp.toPx()

                        // 刘海的实际形状参数
                        val notchLeftPx = topLeftPx.x
                        val notchRightPx = topLeftPx.x + sizePx.width
                        val notchTopPx = topLeftPx.y
                        val notchBottomPx = topLeftPx.y + sizePx.height
                        val notchCenterX = (notchLeftPx + notchRightPx) / 2f

                        // 刘海的圆角半径
                        val topCornerRadius = 15.dp.toPx()
                        val bottomCornerRadius = 25.dp.toPx()

                        val notchPath = Path().apply {
                            // 从屏幕左边开始
                            moveTo(0f, notchTopPx)

                            // 到达刘海左侧
                            lineTo(notchLeftPx - topCornerRadius, notchTopPx)

                            // 左上圆角
                            quadraticTo(
                                notchLeftPx, notchTopPx,
                                notchLeftPx, notchTopPx + topCornerRadius
                            )

                            // 刘海左边
                            lineTo(notchLeftPx, notchBottomPx - bottomCornerRadius)

                            // 左下圆角
                            cubicTo(
                                x1 = notchLeftPx,
                                y1 = notchBottomPx - bottomCornerRadius * 0.6f,
                                x2 = notchLeftPx + bottomCornerRadius * 0.4f,
                                y2 = notchBottomPx,
                                x3 = notchLeftPx + bottomCornerRadius,
                                y3 = notchBottomPx
                            )

                            // 刘海底部
                            lineTo(notchRightPx - bottomCornerRadius, notchBottomPx)

                            // 右下圆角
                            cubicTo(
                                x1 = notchRightPx - bottomCornerRadius * 0.6f,
                                y1 = notchBottomPx,
                                x2 = notchRightPx,
                                y2 = notchBottomPx - bottomCornerRadius * 0.4f,
                                x3 = notchRightPx,
                                y3 = notchBottomPx - bottomCornerRadius
                            )

                            // 刘海右边
                            lineTo(notchRightPx, notchTopPx + topCornerRadius)

                            // 右上圆角
                            quadraticTo(
                                notchRightPx, notchTopPx,
                                notchRightPx + topCornerRadius, notchTopPx
                            )

                            // 到屏幕右边
                            lineTo(canvasWidth, notchTopPx)
                        }

                        drawPath(
                            path = notchPath,
                            color = Color.Magenta,
                            style = Stroke(width = strokeWidth)
                        )
                    }

                    translate(left = topLeftPx.x, top = topLeftPx.y) {
                        with(logoPainter) {
                            draw(size = sizePx)
                        }
                    }
                }
            }
        }
    }
}



