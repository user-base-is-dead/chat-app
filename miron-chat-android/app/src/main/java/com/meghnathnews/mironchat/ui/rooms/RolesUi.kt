package com.meghnathnews.mironchat.ui.rooms

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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.meghnathnews.mironchat.R
import com.meghnathnews.mironchat.data.PERMISSIONS
import com.meghnathnews.mironchat.data.ROLE_COLORS
import com.meghnathnews.mironchat.data.ROLE_NAME_LIMIT
import com.meghnathnews.mironchat.data.Role
import com.meghnathnews.mironchat.data.RoleDraft
import com.meghnathnews.mironchat.ui.theme.BorderStrong
import com.meghnathnews.mironchat.ui.theme.Chroma
import com.meghnathnews.mironchat.ui.theme.Danger
import com.meghnathnews.mironchat.ui.theme.Ink
import com.meghnathnews.mironchat.ui.theme.InkMuted
import com.meghnathnews.mironchat.ui.theme.MutedIcon
import com.meghnathnews.mironchat.ui.theme.Panel
import com.meghnathnews.mironchat.ui.theme.SurfaceStrong
import com.meghnathnews.mironchat.ui.theme.roleColor

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
                    .clickable(onClick = onNew)
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
                    .clickable { onOpen(role) }
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
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Swatch(null, draft.color == null, readOnly) { onChange(draft.copy(color = null)) }
                ROLE_COLORS.forEach { color ->
                    Swatch(color, draft.color == color, readOnly) { onChange(draft.copy(color = color)) }
                }
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
            .clickable(enabled = !readOnly, onClick = onClick),
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
        Switch(
            checked = checked,
            enabled = !readOnly,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Chroma, checkedThumbColor = Color.White),
        )
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
                    .clickable(enabled = editable) { onToggle(role.id, !checked) }
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
