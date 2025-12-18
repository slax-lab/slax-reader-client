package com.slax.reader.ui.inbox.compenents

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slax.reader.ui.inbox.InboxListViewModel
import com.slax.reader.utils.OpenInBrowser
import com.slax.reader.utils.i18n
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_xs_dialog_close

@Composable
fun ProcessingDialog(
    inboxView: InboxListViewModel,
) {
    var url by remember { mutableStateOf("") }
    var externalUrl by remember { mutableStateOf<String?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        inboxView.processingUrlEvent.collect { newUrl ->
            url = newUrl
            visible = true
            delay(100)
        }
    }

    LaunchedEffect(visible) {
        if (!visible) {
            url = ""
            delay(300)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        visible = false
                    }
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .imePadding(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(25.dp)
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.ic_xs_dialog_close),
                            contentDescription = "btn_close".i18n(),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(24.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    visible = false
                                }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "processing_message".i18n(),
                        modifier = Modifier.fillMaxWidth(),
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 22.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF333333)
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    val confirmButtonInteractionSource = remember { MutableInteractionSource() }
                    val isPressed by confirmButtonInteractionSource.collectIsPressedAsState()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(
                                color = if (isPressed) Color(0xFF14A68F) else Color(0xFF16b998),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(
                                interactionSource = confirmButtonInteractionSource,
                                indication = null
                            ) {
                                visible = false
                                externalUrl = url
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "processing_btn_read_original".i18n(),
                            style = TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 22.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
    
    if (externalUrl != null) {
        OpenInBrowser(externalUrl!!)
        externalUrl = null
    }
}
