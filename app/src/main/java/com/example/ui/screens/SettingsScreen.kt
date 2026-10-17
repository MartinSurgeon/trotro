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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    val context = LocalContext.current
    val profile = uiState.profile

    var name by rememberSaveable(profile.organisationName) { mutableStateOf(profile.organisationName) }
    var subtitle by rememberSaveable(profile.roleOrSubtitle) { mutableStateOf(profile.roleOrSubtitle) }
    var apiKey by rememberSaveable { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var showAddSender by remember { mutableStateOf(false) }
    var senderToDelete by remember { mutableStateOf<SenderIdEntity?>(null) }
    var confirmClearContacts by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPickPhoto(uri)
    }
    val profileChanged = name.trim() != profile.organisationName || subtitle.trim() != profile.roleOrSubtitle

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTitleBar(title = "Settings")
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Profile ---
            SettingsCard(title = "Your profile") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(
                        name = uiState.displayName,
                        photoPath = profile.customPhotoPath,
                        size = 72.dp,
                        background = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.size(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilledTonalButton(
                            onClick = {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.testTag("organisation_photo_box")
                        ) {
                            Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(if (profile.customPhotoPath == null) "Add photo or logo" else "Change photo")
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
                    placeholder = { Text("e.g. Women's Fellowship, Kumasi") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth().testTag("org_role_input")
                )
                Button(
                    onClick = { onSaveProfile(name, subtitle) },
                    enabled = profileChanged && name.isNotBlank(),
                    modifier = Modifier.align(Alignment.End).testTag("save_org_customization_button")
                ) { Text("Save") }
            }

            // --- Colours ---
            SettingsCard(title = "App colour") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ColourPresets.forEach { preset ->
                        val selected = profile.primaryColorHex.equals(preset.primaryHex, ignoreCase = true) &&
                            profile.accentColorHex.equals(preset.accentHex, ignoreCase = true)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
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
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(preset.accent)
                                )
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
                                modifier = Modifier.padding(top = 4.dp).widthIn(max = 72.dp),
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            // --- SMS Online GH ---
            SettingsCard(title = "SMS Online GH (optional)") {
                Text(
                    "Send to large groups with your own name (like \"GRACE CHAPEL\") showing as the sender, instead of your phone number. You need an account and SMS credit from SMS Online GH.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val ok = uiState.gatewayConfigured
                    Icon(
                        if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                        contentDescription = null,
                        tint = if (ok) DawuroTheme.statusColors.success else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        when {
                            !ok -> "Not set up"
                            uiState.usingBuildApiKey -> "Connected (key built into this app)"
                            else -> "Connected (${uiState.savedApiKeyHint})"
                        },
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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
                    Spacer(Modifier.weight(1f))
                    if (uiState.savedApiKeyHint.isNotEmpty()) {
                        TextButton(onClick = { onSaveApiKey("") }) { Text("Remove") }
                    }
                    Button(
                        onClick = {
                            onSaveApiKey(apiKey)
                            apiKey = ""
                        },
                        enabled = apiKey.length >= 8
                    ) { Text("Save key") }
                }

                Text(
                    "Sender names",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    "Must match a sender name approved on your SMS Online GH account (up to 11 letters).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                uiState.senderIds.forEach { sender ->
                    val active = sender.senderName.equals(uiState.activeSenderName, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .selectable(selected = active, onClick = { onSelectSenderId(sender) }, role = Role.RadioButton)
                            .testTag("sender_chip_${sender.senderName}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = active, onClick = null)
                        Spacer(Modifier.size(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sender.senderName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
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
                OutlinedButton(
                    onClick = { showAddSender = true },
                    modifier = Modifier.testTag("home_add_sender_id_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Add sender name")
                }
            }

            // --- Data ---
            if (uiState.contacts.isNotEmpty()) {
                SettingsCard(title = "Your data") {
                    TextButton(onClick = { confirmClearContacts = true }) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.size(8.dp))
                        Text("Delete all ${uiState.contacts.size} contacts", color = MaterialTheme.colorScheme.error)
                    }
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
                    "One message, everyone hears. Made for Ghana.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
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
                    placeholder = { Text("e.g. For church announcements") },
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
