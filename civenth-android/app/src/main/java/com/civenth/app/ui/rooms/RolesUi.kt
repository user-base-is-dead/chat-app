package com.civenth.app.ui.rooms

import com.civenth.app.ui.theme.pressClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civenth.app.R
import com.civenth.app.data.PERMISSIONS
import com.civenth.app.data.ROLE_COLORS
import com.civenth.app.data.ROLE_NAME_LIMIT
import com.civenth.app.data.Role
import com.civenth.app.data.RoleDraft
import com.civenth.app.ui.theme.BorderStrong
import com.civenth.app.ui.theme.Chroma
import com.civenth.app.ui.theme.Danger
import com.civenth.app.ui.theme.Ink
import com.civenth.app.ui.theme.InkMuted
import com.civenth.app.ui.theme.MutedIcon
import com.civenth.app.ui.theme.Panel
import com.civenth.app.ui.theme.SurfaceStrong
import com.civenth.app.ui.theme.roleColor
import com.civenth.app.ui.theme.SmoothSwitch
import android.graphics.Color as AndroidColor
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
private fun RoleDot(color: String?) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(roleColor(color) ?: SurfaceStrong),
    )
}

@Composable
fun RoleListContent(roles: List<Role>, canCreate: Boolean, onOpen: (Role) -> Unit, onNew: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (canCreate) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, BorderStrong, RoundedCornerShape(14.dp))
                    .pressClickable(onClick = onNew)
                    .padding(12.dp),
            ) {
                Icon(painterResource(R.drawable.ic_plus), null, tint = Chroma, modifier = Modifier.size(20.dp))
                Text("New Role", color = Chroma, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (roles.isEmpty()) {
            Text("No roles yet", color = InkMuted, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(12.dp))
        }
        roles.forEach { role ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .pressClickable { onOpen(role) }
                    .padding(12.dp),
            ) {
                RoleDot(role.color)
                Text(
                    text = role.name,
                    color = Ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(if (role.memberCount == 1) "1 member" else "${role.memberCount} members", color = InkMuted, fontSize = 13.sp)
                Icon(painterResource(R.drawable.ic_chevron), null, tint = MutedIcon, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoleEditorContent(
    draft: RoleDraft,
    readOnly: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onChange: (RoleDraft) -> Unit,
    onMove: ((String) -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (readOnly) {
            Text("This role is at or above your highest role, so you can only view it.", color = InkMuted, fontSize = 13.5.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.alpha(if (readOnly) 0.7f else 1f)) {
            OutlinedTextField(
                value = draft.name,
                onValueChange = { onChange(draft.copy(name = it.take(ROLE_NAME_LIMIT))) },
                label = { Text("Role name") },
                singleLine = true,
                enabled = !readOnly,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Chroma,
                    unfocusedBorderColor = BorderStrong,
                    focusedLabelColor = Chroma,
                    cursorColor = Chroma,
                    focusedTextColor = Ink,
                    unfocusedTextColor = Ink,
                    disabledTextColor = Ink,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Colour", color = InkMuted, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            Text("Members with this role show their name in this colour. Pick one or choose your own with the last circle.", color = InkMuted, fontSize = 12.5.sp)
            var picking by remember { mutableStateOf(false) }
            val custom = draft.color?.startsWith("#") == true
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Swatch(null, draft.color == null, readOnly) { onChange(draft.copy(color = null)) }
                ROLE_COLORS.forEach { color ->
                    Swatch(color, draft.color == color, readOnly) { onChange(draft.copy(color = color)) }
                }
                CustomSwatch(if (custom) draft.color else null, readOnly) { picking = true }
            }
            if (picking) {
                ColorPickerDialog(
                    start = if (custom) draft.color.orEmpty() else "#3a8fe6",
                    onDismiss = { picking = false },
                    onPick = {
                        picking = false
                        onChange(draft.copy(color = it))
                    },
                )
            }
            ToggleRow("Show members separately", "Online members with this role get their own section in the member list", draft.separate, readOnly) {
                onChange(draft.copy(separate = it))
            }
            Text("Permissions", color = InkMuted, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            PERMISSIONS.forEach { permission ->
                ToggleRow(permission.label, permission.hint, draft.permissions.contains(permission.key), readOnly) { on ->
                    val next = if (on) draft.permissions + permission.key else draft.permissions - permission.key
                    onChange(draft.copy(permissions = PERMISSIONS.map { it.key }.filter { next.contains(it) }))
                }
            }
        }
        if (onMove != null && !readOnly) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoveButton("Move up", canMoveUp, Modifier.weight(1f)) { onMove("up") }
                MoveButton("Move down", canMoveDown, Modifier.weight(1f)) { onMove("down") }
            }
        }
        if (onDelete != null && !readOnly) {
            OutlinedButton(
                onClick = onDelete,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderStrong),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
            ) {
                Text("Delete Role", color = Danger, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun Swatch(color: String?, selected: Boolean, readOnly: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .border(2.dp, if (selected) Ink else Color.Transparent, CircleShape)
            .padding(3.dp)
            .clip(CircleShape)
            .background(roleColor(color) ?: Panel)
            .border(if (color == null) 1.dp else 0.dp, BorderStrong, CircleShape)
            .pressClickable(enabled = !readOnly, onClick = onClick),
    ) {
        if (color == null) Text("/", color = Danger, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ToggleRow(label: String, hint: String, checked: Boolean, readOnly: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, color = Ink, fontSize = 15.sp)
            Text(hint, color = InkMuted, fontSize = 12.5.sp)
        }
        SmoothSwitch(checked = checked, enabled = !readOnly, onChange = onChange)
    }
}

@Composable
private fun MoveButton(label: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderStrong),
        modifier = modifier.height(42.dp),
    ) {
        Text(label, color = if (enabled) Ink else InkMuted, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun MemberRolesContent(name: String, roles: List<Role>, selected: List<String>, isEditable: (Role) -> Boolean, onToggle: (String, Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Roles for $name", color = InkMuted, fontSize = 13.5.sp, modifier = Modifier.padding(bottom = 4.dp))
        if (roles.isEmpty()) Text("No roles yet", color = InkMuted, fontSize = 14.sp)
        roles.forEach { role ->
            val editable = isEditable(role)
            val checked = selected.contains(role.id)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (editable) 1f else 0.55f)
                    .clip(RoundedCornerShape(14.dp))
                    .pressClickable(enabled = editable) { onToggle(role.id, !checked) }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            ) {
                Checkbox(
                    checked = checked,
                    enabled = editable,
                    onCheckedChange = { onToggle(role.id, it) },
                    colors = CheckboxDefaults.colors(checkedColor = Chroma),
                )
                RoleDot(role.color)
                Text(role.name, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun CustomSwatch(color: String?, readOnly: Boolean, onClick: () -> Unit) {
    val rainbow = Brush.sweepGradient(
        listOf(Color(0xFFFF6B6B), Color(0xFFF5B841), Color(0xFF51CF66), Color(0xFF3BC9DB), Color(0xFF4263EB), Color(0xFFCC8CF0), Color(0xFFFF6B6B)),
    )
    val fill = roleColor(color)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .border(2.dp, if (fill != null) Ink else Color.Transparent, CircleShape)
            .padding(3.dp)
            .clip(CircleShape)
            .then(if (fill != null) Modifier.background(fill) else Modifier.background(rainbow))
            .pressClickable(enabled = !readOnly, onClick = onClick),
    ) {
        Text("+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

private fun hexOf(hsv: FloatArray): String =
    String.format("#%06x", AndroidColor.HSVToColor(hsv) and 0xFFFFFF)

@Composable
private fun ColorPickerDialog(start: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val initial = remember(start) {
        FloatArray(3).also { AndroidColor.colorToHSV(AndroidColor.parseColor(start), it) }
    }
    var hue by remember { mutableFloatStateOf(initial[0]) }
    var saturation by remember { mutableFloatStateOf(initial[1]) }
    var brightness by remember { mutableFloatStateOf(initial[2]) }
    val hex = hexOf(floatArrayOf(hue, saturation, brightness))
    val preview = roleColor(hex) ?: Chroma
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Custom colour", color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(preview),
                    )
                    Column {
                        Text("Role name", color = preview, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(hex.uppercase(), color = InkMuted, fontSize = 13.sp)
                    }
                }
                PickerSlider("Shade", hue, 0f..360f, Brush.horizontalGradient((0..6).map { Color.hsv(it * 60f, 1f, 1f) })) { hue = it }
                PickerSlider(
                    "Strength",
                    saturation,
                    0f..1f,
                    Brush.horizontalGradient(listOf(Color.hsv(hue, 0f, brightness), Color.hsv(hue, 1f, brightness))),
                ) { saturation = it }
                PickerSlider(
                    "Brightness",
                    brightness,
                    0.2f..1f,
                    Brush.horizontalGradient(listOf(Color.hsv(hue, saturation, 0.2f), Color.hsv(hue, saturation, 1f))),
                ) { brightness = it }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(hex) }) { Text("Use colour", color = Chroma, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = InkMuted) } },
    )
}

@Composable
private fun PickerSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, track: Brush, onChange: (Float) -> Unit) {
    Column {
        Text(label, color = InkMuted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(40.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(track),
            )
            Slider(
                value = value,
                onValueChange = onChange,
                valueRange = range,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                ),
            )
        }
    }
}
