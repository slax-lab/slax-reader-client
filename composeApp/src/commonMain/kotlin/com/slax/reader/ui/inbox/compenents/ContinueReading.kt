package com.slax.reader.ui.inbox.compenents

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slax.reader.data.preferences.AppPreferences
import com.slax.reader.data.preferences.ContinueReadingBookmark
import com.slax.reader.utils.i18n
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_continue_reading_close
import slax_reader_client.composeapp.generated.resources.ic_continue_reading_icon

@Composable
fun ContinueReading(
    onClick: ((bookmarkId: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val appPreferences: AppPreferences = koinInject()
    val coroutineScope = rememberCoroutineScope()
    var showContinueData by remember { mutableStateOf<ContinueReadingBookmark?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val bookmark = appPreferences.getContinueReadingBookmark() ?: return@launch

            showContinueData = bookmark
            appPreferences.clearContinueReadingBookmark()

            delay(300)
            visible = true
        }
    }

    if (!visible) return

    LaunchedEffect(visible) {
        if (!visible) {
            delay(300)
            showContinueData = null
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)) +
                slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(300)
                ),
        exit = fadeOut(animationSpec = tween(300)) +
                slideOutVertically(
                    targetOffsetY = { it / 2 },
                    animationSpec = tween(300)
                ),
        modifier = modifier
            .fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xE6FCFCFC),
                                Color(0xFFFCFCFC)
                            )
                        )
                    )
            ) {
                Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                    Spacer(
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x00FCFCFC),
                                        Color(0xFFFCFCFC)
                                    )
                                )
                            )
                    )
                    Spacer(
                        modifier = Modifier.fillMaxWidth().height(68.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFCFCFC),
                                        Color(0xFFFCFCFC)
                                    )
                                )
                            )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(top = 13.dp, bottom = 8.dp)
                    .navigationBarsPadding()
                    .shadow(
                        elevation = 40.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color(0x14000000),
                        spotColor = Color(0x14000000)
                    )
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFFFFF),
                                Color(0xFFFFFFFF),
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0x140F1419),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = onClick != null
                    ) {
                        showContinueData?.let { onClick?.invoke(it.bookmarkId) }
                    }
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(Res.drawable.ic_continue_reading_icon),
                        contentDescription = "continue_reading_desc".i18n(),
                        modifier = Modifier.size(14.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = showContinueData!!.title,
                        style = TextStyle(
                            fontSize = 15.sp,
                            color = Color(0xFF4d4d4d),
                            lineHeight = 21.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(16.dp)
                            .background(Color(0x3DCAA68E))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Image(
                        painter = painterResource(Res.drawable.ic_continue_reading_close),
                        contentDescription = "continue_reading_close_desc".i18n(),
                        modifier = Modifier
                            .size(12.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                visible = false
                            },
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}