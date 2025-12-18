package com.slax.reader.ui.inbox.compenents

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slax.reader.utils.i18n
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.inbox_more


@Composable
fun InboxTitleRow() {
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    println("[watch][UI] recomposition InboxTitleRow")
    Row(
        modifier = Modifier
            .padding(start = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Row(
                modifier = Modifier
                    .alpha(if (isPressed) 0.5f else 1f)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        expanded = true
                    }
                    .height(20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "inbox_title".i18n(),
                    style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, color = Color(0xFF999999))
                )

                Image(
                    painter = painterResource(Res.drawable.inbox_more),
                    contentDescription = "Content Type",
                    modifier = Modifier
                        .size(8.dp),
                    contentScale = ContentScale.Fit
                )
            }

            DropdownMenu(
                modifier = Modifier.background(Color.White),
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("inbox_title".i18n()) },
                    modifier = Modifier.background(Color.White),
                    onClick = {
                        expanded = false
                    }
                )
            }
        }
    }
}
