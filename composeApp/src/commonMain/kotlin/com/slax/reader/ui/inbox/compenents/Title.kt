package com.slax.reader.ui.inbox.compenents

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import slax_reader_client.composeapp.generated.resources.Res
import slax_reader_client.composeapp.generated.resources.inbox_more


@Composable
fun InboxTitleRow() {
    var expanded by remember { mutableStateOf(false) }
    // println("[watch][UI] recomposition InboxTitleRow")
    Box {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Inbox",
                fontSize = 16.sp,
                color = Color(0xFF999999)
            )

            Image(
                painter = painterResource(Res.drawable.inbox_more),
                contentDescription = "Content Type",
                modifier = Modifier
                    .size(12.dp)
                    .height(8.dp),
                contentScale = ContentScale.Fit
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Inbox") },
                onClick = {
                    expanded = false
                }
            )
        }
    }
}
