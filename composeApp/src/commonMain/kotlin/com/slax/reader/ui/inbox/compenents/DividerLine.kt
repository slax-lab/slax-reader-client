package com.slax.reader.ui.inbox.compenents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@NonRestartableComposable
@Composable
fun DividerLine() {
    // println("[watch][UI] recomposition DividerLine")

    Spacer(modifier = Modifier.height(0.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .padding(horizontal = 24.dp)
            .background(Color(0x14333333))
    )
    Spacer(modifier = Modifier.height(0.dp))
}