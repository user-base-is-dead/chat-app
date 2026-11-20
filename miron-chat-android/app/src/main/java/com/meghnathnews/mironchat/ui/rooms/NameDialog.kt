package com.meghnathnews.mironchat.ui.rooms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meghnathnews.mironchat.data.CHANNEL_NAME_LIMIT
import com.meghnathnews.mironchat.ui.theme.BorderStrong
import com.meghnathnews.mironchat.ui.theme.Chroma
import com.meghnathnews.mironchat.ui.theme.Ink
import com.meghnathnews.mironchat.ui.theme.InkMuted
import com.meghnathnews.mironchat.ui.theme.Panel

data class NameRequest(
    val title: String,
    val label: String,
    val initial: String = "",
    val clean: ((String) -> String)? = null,
    val onSubmit: (String) -> Unit,
)

@Composable
fun NameDialog(request: NameRequest, onClose: () -> Unit) {
    var value by remember(request) { mutableStateOf(request.initial) }
    val name = request.clean?.invoke(value) ?: value.trim()
    val ready = name.isNotEmpty() && name.codePointCount(0, name.length) <= CHANNEL_NAME_LIMIT
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Panel,
        title = { Text(request.title, color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.take(64) },
                    label = { Text(request.label) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Chroma,
                        unfocusedBorderColor = BorderStrong,
                        focusedLabelColor = Chroma,
                        cursorColor = Chroma,
                        focusedTextColor = Ink,
                        unfocusedTextColor = Ink,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = if (request.clean != null && name.isNotEmpty()) "Will be saved as #$name" else "Up to $CHANNEL_NAME_LIMIT characters",
                    color = InkMuted,
                    fontSize = 13.sp,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = ready,
                onClick = {
                    onClose()
                    request.onSubmit(name)
                },
            ) {
                Text("Save", color = if (ready) Chroma else InkMuted, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Cancel", color = Ink) }
        },
    )
}
