package com.civenth.app.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civenth.app.data.PostSwitches
import com.civenth.app.ui.theme.BorderStrong
import com.civenth.app.ui.theme.Chroma
import com.civenth.app.ui.theme.Ink
import com.civenth.app.ui.theme.InkMuted
import com.civenth.app.ui.theme.Panel
import com.civenth.app.ui.theme.SmoothSwitch

const val POST_MAX_LENGTH = 4000

data class PostDraft(
    val text: String,
    val switches: PostSwitches,
    val messageId: String? = null,
)

@Composable
fun PostDialog(draft: PostDraft, onSubmit: (String, PostSwitches) -> Unit, onClose: () -> Unit) {
    var field by remember(draft) { mutableStateOf(TextFieldValue(draft.text, TextRange(draft.text.length))) }
    val text = field.text
    var switches by remember(draft) { mutableStateOf(draft.switches) }
    val editing = draft.messageId != null
    val ready = text.isNotBlank()
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Panel,
        title = { Text(if (editing) "Edit post" else "New broadcast", color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = field,
                    onValueChange = { if (it.text.length <= POST_MAX_LENGTH) field = it },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Chroma,
                        unfocusedBorderColor = BorderStrong,
                        cursorColor = Chroma,
                        focusedTextColor = Ink,
                        unfocusedTextColor = Ink,
                    ),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp, max = 220.dp),
                )
                SwitchRow(
                    label = "Comments",
                    on = switches.allowComments,
                    hint = if (switches.allowComments) "People can comment on this post" else "Nobody can comment on this post",
                ) { switches = switches.copy(allowComments = it) }
                SwitchRow(
                    label = "Reactions",
                    on = switches.allowReactions,
                    hint = if (switches.allowReactions) "People can react to this post" else "Nobody can react to this post",
                ) { switches = switches.copy(allowReactions = it) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = ready,
                onClick = {
                    onClose()
                    onSubmit(text.trim(), switches)
                },
            ) {
                Text(if (editing) "Save" else "Broadcast", color = if (ready) Chroma else InkMuted, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Cancel", color = Ink) }
        },
    )
}

@Composable
private fun SwitchRow(label: String, on: Boolean, hint: String, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text("$label ${if (on) "on" else "off"}", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(hint, color = InkMuted, fontSize = 13.sp)
        }
        SmoothSwitch(checked = on, onChange = onChange)
    }
}
