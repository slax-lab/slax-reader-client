package com.slax.reader.ui.setting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.slax.reader.const.DeleteAccountRoutes
import com.slax.reader.utils.LocaleString
import com.slax.reader.utils.i18n
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_sm_back

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    onBackClick: () -> Unit,
    navController: NavHostController
) {
    val coroutineScope = rememberCoroutineScope()
    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "setting_title".i18n(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0F1419)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_sm_back),
                            contentDescription = "btn_back".i18n(),
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F5F3)
                )
            )
        },
        containerColor = Color(0xFFF5F5F3)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 语言选择
                    SettingItem(
                        title = "setting_language".i18n(),
                        rightText = if (LocaleString.currentLocale == "zh") "language_chinese".i18n() else "language_english".i18n(),
                        onClick = {
                            showLanguageDialog = true
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = Color(0x1E000000)
                    )

                    // 删除账号
                    SettingItem(
                        title = "setting_delete_account".i18n(),
                        color = Color(0xFFF45454),
                        onClick = {
                            navController.navigate(DeleteAccountRoutes)
                        }
                    )
                }
            }
        }
    }

    // 语言选择对话框
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = Color.White,
            title = { Text("setting_language".i18n()) },
            text = {
                Column {
                    RadioButtonItem(
                        text = "language_chinese".i18n(),
                        selected = LocaleString.currentLocale == "zh",
                        onClick = {
                            coroutineScope.launch {
                                LocaleString.changeLocale("zh")
                            }
                            showLanguageDialog = false
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RadioButtonItem(
                        text = "language_english".i18n(),
                        selected = LocaleString.currentLocale == "en",
                        onClick = {
                            coroutineScope.launch {
                                LocaleString.changeLocale("en")
                            }
                            showLanguageDialog = false
                        }
                    )
                }
            },
            confirmButton = { }
        )
    }
}

@Composable
private fun RadioButtonItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.Gray)
            ) { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFF16b998)
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 16.sp,
            color = Color(0xFF0F1419)
        )
    }
}

@Composable
private fun SettingItem(
    title: String,
    color: Color = Color(0xFF0F1419),
    rightText: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.Gray)
            ) {
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = color
        )

        if (rightText != null) {
            Text(
                text = rightText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF999999)
            )
        }
    }
}