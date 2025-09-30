package com.slax.reader.ui.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.bg_login
import slax_reader_client.composeapp.generated.resources.ic_google
import slax_reader_client.composeapp.generated.resources.ic_apple
import slax_reader_client.composeapp.generated.resources.ic_radio_disabled
import slax_reader_client.composeapp.generated.resources.ic_radio_enabled

@Composable
fun LoginView() {
    Column(modifier = Modifier.fillMaxSize().background(Color.White), verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(text = "欢迎来到 \nSlax Reader", style = TextStyle(
                fontSize = 27.sp,
                lineHeight = 40.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F1419)
            ), modifier = Modifier.padding(start = 40.dp, top = 84.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                // 背景图片
                Image(
                    painter = painterResource(Res.drawable.bg_login),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(375f / 314f),
                    contentScale = ContentScale.Fit
                )

                Text(
                    text = "Read Smarter\nConnect Deeper",
                    style = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xCC333333)
                    ),
                    modifier = Modifier.padding(start = 40.dp, top = 24.dp)
                )
            }

        }

        Column(modifier = Modifier.padding(bottom = 30.dp)) {
            MyRoundedButton(
                text = "Google 登录",
                drawableResource = Res.drawable.ic_google,
                onClick = {

                },
            )
            MyRoundedButton(
                modifier = Modifier.padding(top = 10.dp),
                text = "通过 Apple 登录",
                drawableResource = Res.drawable.ic_apple,
                onClick = {

                },
            )

            AgreementText(
                modifier = Modifier.padding(top = 30.dp),
                onPrivacyPolicyClick = {},
                onUserAgreementClick = {}
            )
        }
    }
}

@Composable
fun MyRoundedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalMargin: Dp = 40.dp,
    maxWidth: Dp = 295.dp,
    drawableResource: DrawableResource? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalMargin)
            .then(
                Modifier
                    .height(50.dp)
                    .widthIn(max = maxWidth)
            )
    ) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxSize(),
            shape = RoundedCornerShape(25.dp),
            border = BorderStroke(0.5.dp, Color(0x336A6E83)),
            contentPadding = PaddingValues(),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White
            )
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                drawableResource?.let {
                    Icon(painter = painterResource(drawableResource), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(24.dp) )
                }

                Text(text, style = TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0F1419)
                ), modifier = Modifier.padding(start = if (drawableResource != null) 10.dp else 0.dp))
            }
        }
    }
}

@Composable
fun AgreementText(
    modifier: Modifier = Modifier,
    onUserAgreementClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit
) {
    val annotatedString = buildAnnotatedString {
        append("阅读并同意")

        pushStringAnnotation(
            tag = "user_agreement",
            annotation = "user_agreement"
        )
        withStyle(
            style = SpanStyle(
                color = Color(0xff5490C2),
            )
        ) {
            append("《用户协议》")
        }
        pop()

        append("和")

        pushStringAnnotation(
            tag = "privacy_policy",
            annotation = "privacy_policy"
        )
        withStyle(
            style = SpanStyle(
                color = Color(0xff5490C2),
            )
        ) {
            append("《隐私政策》")
        }
        pop()
    }

    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.then(Modifier.fillMaxWidth())
    ) {
        val selected = remember { mutableStateOf(false) }

        IconButton(
            onClick = { selected.value = !selected.value },
            modifier = Modifier.size(12.dp)
        ) {
            Icon(
                painter = painterResource(
                    if (selected.value) Res.drawable.ic_radio_enabled else Res.drawable.ic_radio_disabled
                ),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(12.dp)
            )
        }

        BasicText(
            text = annotatedString,
            style = TextStyle(lineHeight = 16.5.sp, fontSize = 12.sp, color = Color(0xCC333333)),
            onTextLayout = { layoutResult.value = it },
            modifier = Modifier
                .padding(start = 6.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        layoutResult.value?.let { layout ->
                            val position = layout.getOffsetForPosition(offset)

                            annotatedString.getStringAnnotations(
                                tag = "user_agreement",
                                start = position,
                                end = position
                            ).firstOrNull()?.let {
                                onUserAgreementClick()
                            }

                            annotatedString.getStringAnnotations(
                                tag = "privacy_policy",
                                start = position,
                                end = position
                            ).firstOrNull()?.let {
                                onPrivacyPolicyClick()
                            }
                        }
                    }
                }
        )
    }
}