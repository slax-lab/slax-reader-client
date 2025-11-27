package com.slax.reader.const.component

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun rememberDismissableVisibility(
    scope: CoroutineScope,
    animationDuration: Long = 300L,
    onDismissRequest: () -> Unit
): Pair<Boolean, () -> Unit> {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    val dismiss: () -> Unit = {
        scope.launch {
            visible = false
            delay(animationDuration)
            onDismissRequest()
        }
    }

    return visible to dismiss
}