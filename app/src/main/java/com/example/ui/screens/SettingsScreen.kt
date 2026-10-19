package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.SenderIdEntity
import com.example.ui.components.Avatar
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.KenteStripe
import com.example.ui.components.ScreenTitleBar
import com.example.ui.theme.ColourPresets
import com.example.ui.theme.DawuroTheme
import com.example.ui.theme.contentColorFor
import com.example.viewmodel.CampaignUiState
import com.example.viewmodel.CampaignViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    uiState: CampaignUiState,
    onSaveProfile: (name: String, subtitle: String) -> Unit,
    onPickPhoto: (Uri) -> Unit,
    onRemovePhoto: () -> Unit,
    onSetColours: (primaryHex: String, accentHex: String) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onAddSenderId: (name: String, note: String) -> Boolean,
    onSelectSenderId: (SenderIdEntity) -> Unit,
    onDeleteSenderId: (SenderIdEntity) -> Unit,
    onClearAllContacts: () -> Unit
) {
    val profile = uiState.profile

    var showEditProfile by remember { mutableStateOf(false) }
    var showApiKey by remember { mutableStateOf(false) }
    var showAddSender by remember { mutableStateOf(false) }
    var senderToDelete by remember { mutableStateOf<SenderIdEntity?>(null) }
    var confirmClearContacts by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPickPhoto(uri)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTitleBar(title = "Settings")
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- Profile header ---
            Card(
                onClick = { showEditProfile = true },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth().testTag("profile_card")
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(
                        name = uiState.displayName,
                        photoPath = profile.customPhotoPath,
                        size = 64.dp,
                        background = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.size(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(uiState.displayName, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            profile.roleOrSubtitle.ifBlank { "Add a short description" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit profile")
                }
            }

            // --- Sending ---
            SettingsSection("Sending") {
                SettingsRow(
                    icon = Icons.Rounded.Cloud,
                    title = "SMS Online GH",
                    subtitle = when {
                        !uiState.gatewayConfigured -> "Not set up · for big groups with your own sender name"
                        uiState.usingBuildApiKey -> "Connected (built-in key)"
                        else -> "Connected (${uiState.savedApiKeyHint})"
                    },
                    trailing = {
                        if (uiState.gatewayConfigured) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = "Connected", tint = DawuroTheme.statusColors.success)
                        } else {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
                        }
                    },
                    onClick = { showApiKey = true },
                    tag = "api_key_row"
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("Sender names", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "The name people see instead of your number. It must be approved on your SMS Online GH account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                uiState.senderIds.forEach { sender ->
                    val active = sender.senderName.equals(uiState.activeSenderName, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .selectable(selected = active, onClick = { onSelectSenderId(sender) }, role = Role.RadioButton)
                            .padding(start = 8.dp)
                            .testTag("sender_chip_${sender.senderName}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = active, onClick = null)
                        Spacer(Modifier.size(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sender.senderName, style = MaterialTheme.typography.bodyLarge)
                            if (sender.candidateOrOrgLabel.isNotBlank() && sender.candidateOrOrgLabel != sender.senderName) {
                                Text(
                                    sender.candidateOrOrgLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { senderToDelete = sender }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove ${sender.senderName}")
                        }
                    }
                }
                TextButton(
                    onClick = { showAddSender = true },
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp).testTag("home_add_sender_id_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Add sender name")
                }
            }

            // --- Appearance ---
            SettingsSection("Appearance") {
                Text(
                    "App colour",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    ColourPresets.forEach { preset ->
                        val selected = profile.primaryColorHex.equals(preset.primaryHex, ignoreCase = true) &&
                            profile.accentColorHex.equals(preset.accentHex, ignoreCase = true)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(preset.primary)
                                    .then(
                                        if (selected) {
                                            Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface), CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .selectable(
                                        selected = selected,
                                        onClick = { onSetColours(preset.primaryHex, preset.accentHex) },
                                        role = Role.RadioButton
                                    )
                                    .semantics { contentDescription = preset.name },
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(preset.accent))
                                if (selected) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = contentColorFor(preset.primary),
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                            Text(
                                preset.name,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 4.dp).widthIn(max = 68.dp),
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            // --- Data ---
            if (uiState.contacts.isNotEmpty()) {
                SettingsSection("Your data") {
                    SettingsRow(
                        icon = Icons.Rounded.DeleteSweep,
                        title = "Delete all contacts",
                        subtitle = "${uiState.contacts.size} saved in Dawuro",
                        destructive = true,
                        onClick = { confirmClearContacts = true }
                    )
                }
            }

            // --- About ---
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                KenteStripe(modifier = Modifier.widthIn(max = 120.dp), height = 4.dp)
                Text("Dawuro ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleSmall)
                Text(
                    "One message, everyone hears.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showEditProfile) {
        EditProfileDialog(
            uiState = uiState,
            onPickPhoto = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onRemovePhoto = onRemovePhoto,
            onSave = { name, subtitle ->
                onSaveProfile(name, subtitle)
                showEditProfile = false
            },
            onDismiss = { showEditProfile = false }
        )
    }

    if (showApiKey) {
        ApiKeyDialog(
            uiState = uiState,
            onSave = {
                onSaveApiKey(it)
                showApiKey = false
            },
            onDismiss = { showApiKey = false }
        )
    }

    if (showAddSender) {
        AddSenderDialog(
            onDismiss = { showAddSender = false },
            onSave = { senderName, note -> if (onAddSenderId(senderName, note)) showAddSender = false }
        )
    }

    senderToDelete?.let { sender ->
        ConfirmDialog(
            title = "Remove \"${sender.senderName}\"?",
            message = "It will no longer be available when sending. You can add it again later.",
            confirmLabel = "Remove",
            destructive = true,
            onConfirm = { onDeleteSenderId(sender) },
            onDismiss = { senderToDelete = null }
        )
    }

    if (confirmClearContacts) {
        ConfirmDialog(
            title = "Delete all contacts?",
            message = "This removes all ${uiState.contacts.size} contacts from Dawuro. Your phone's own contacts are not affected. This cannot be undone.",
            confirmLabel = "Delete all",
            destructive = true,
            onConfirm = onClearAllContacts,
            onDismiss = { confirmClearContacts = false }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    destructive: Boolean = false,
    tag: String? = null
) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    ListItem(
        headlineContent = {
            Text(title, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null, tint = tint) },
        trailingContent = trailing,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick).then(if (tag != null) Modifier.testTag(tag) else Modifier)
    )
}

@Composable
private fun EditProfileDialog(
    uiState: CampaignUiState,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val profile = uiState.profile
    var name by rememberSaveable { mutableStateOf(profile.organisationName) }
    var subtitle by rememberSaveable { mutableStateOf(profile.roleOrSubtitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit profile") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(
                        name = name.ifBlank { uiState.displayName },
                        photoPath = profile.customPhotoPath,
                        size = 64.dp,
                        background = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.size(16.dp))
                    Column {
                        FilledTonalButton(onClick = onPickPhoto, modifier = Modifier.testTag("organisation_photo_box")) {
                            Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(if (profile.customPhotoPath == null) "Add photo" else "Change")
                        }
                        if (profile.customPhotoPath != null) {
                            TextButton(onClick = onRemovePhoto) { Text("Remove photo") }
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text("Name or group name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth().testTag("org_name_input")
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it.take(80) },
                    label = { Text("Short description (optional)") },
                    placeholder = { Text("e.g. Youth fellowship, Kumasi") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth().testTag("org_role_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, subtitle) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_org_customization_button")
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ApiKeyDialog(uiState: CampaignUiState, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var apiKey by rememberSaveable { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("SMS Online GH") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Send to large groups with your own name (like \"GRACECHAPEL\") instead of your number. You need an account and SMS credit from SMS Online GH.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (uiState.gatewayConfigured) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                        contentDescription = null,
                        tint = if (uiState.gatewayConfigured) DawuroTheme.statusColors.success else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        if (uiState.gatewayConfigured) "Connected" else "Not set up",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it.trim() },
                    label = { Text(if (uiState.savedApiKeyHint.isEmpty()) "Paste your API key" else "Replace API key") },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (showKey) "Hide key" else "Show key"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("api_key_input")
                )
                Row {
                    TextButton(onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://smsonlinegh.com")))
                        } catch (_: ActivityNotFoundException) {
                        }
                    }) {
                        Text("Get an account")
                        Spacer(Modifier.size(4.dp))
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    if (uiState.savedApiKeyHint.isNotEmpty()) {
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { onSave("") }) {
                            Text("Remove key", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(apiKey) }, enabled = apiKey.length >= 8) { Text("Save key") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun AddSenderDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var senderName by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    val clean = senderName.trim().replace(Regex("\\s+"), " ")
    val valid = CampaignViewModel.isValidSenderName(clean)
    val showError = senderName.isNotEmpty() && !valid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add sender name") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = senderName,
                    onValueChange = { if (it.length <= 11) senderName = it },
                    label = { Text("Sender name") },
                    placeholder = { Text("e.g. GRACECHAPEL") },
                    isError = showError,
                    supportingText = {
                        Text(if (showError) "Only letters, numbers and spaces" else "${senderName.length}/11")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth().testTag("new_sender_id_input")
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(60) },
                    label = { Text("Note for yourself (optional)") },
                    placeholder = { Text("e.g. For announcements") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_sender_candidate_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(clean, note) },
                enabled = valid,
                modifier = Modifier.testTag("confirm_save_sender_id_button")
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
