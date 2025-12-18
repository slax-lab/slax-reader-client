package com.slax.reader.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.slax.reader.SlaxConfig
import com.slax.reader.utils.i18n
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.ic_app_logo
import slax_reader_client.composeapp.generated.resources.ic_sm_back

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_app_logo),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.padding(top = 44.dp).size(64.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Slax Reader",
                fontSize = 27.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 40.5.sp,
                color = Color(0xFF0F1419)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${"about_version".i18n()} ${SlaxConfig.APP_VERSION_NAME}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 21.sp,
                color = Color(0xFF999999)
            )
        }
    }
}