package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.LocalSnackbarHostState
import com.example.data.ContactEntity
import com.example.data.SendMode
import com.example.data.SenderIdEntity
import com.example.ui.components.Avatar
import com.example.ui.components.ScreenTitleBar
import com.example.ui.components.peopleCount
import com.example.ui.theme.DawuroTheme
import com.example.util.MessageTemplates
import com.example.util.PhoneNumbers
import com.example.util.SmsCounter
import com.example.viewmodel.CampaignUiState
import com.example.viewmodel.GroupInfo
import kotlinx.coroutines.launch

/** Above this many recipients, sending from a SIM triggers Android's "many SMS" prompt. */
private const val SIM_COMFORT_LIMIT = 30

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ComposeScreen(
    uiState: CampaignUiState,
    hasSendSmsPermission: () -> Boolean,
    isSimReady: () -> Boolean,
    onBack: () -> Unit,
    onMessageChanged: (String) -> Unit,
    onApplyTemplate: (String) -> Unit,
    onSendModeChanged: (String) -> Unit,
    onSelectSenderId: (SenderIdEntity) -> Unit,
    onToggleContact: (Long, Boolean) -> Unit,
    onSetAllSelected: (Boolean) -> Unit,
    onToggleGroup: (GroupInfo) -> Unit,
    onOpenContacts: () -> Unit,
    onOpenSettings: () -> Unit,
    onConfirmSend: () -> Unit
) {
    val context = LocalContext.current
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    var showRecipients by remember { mutableStateOf(false) }
    var showTemplates by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    // Re-check SIM and permission when the user comes back from system settings.
    var environmentTick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { environmentTick++ }
    val simReady = remember(environmentTick) { isSimReady() }

    // Local TextFieldValue so "Add their name" can insert at the cursor. It only takes text
    // from the ViewModel on external changes (template, undo), signalled by messageRevision.
    var field by remember { mutableStateOf(TextFieldValue(uiState.messageText, TextRange(uiState.messageText.length))) }
    LaunchedEffect(uiState.messageRevision) {
        if (field.text != uiState.messageText) {
            field = TextFieldValue(uiState.messageText, TextRange(uiState.messageText.length))
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        environmentTick++
        if (granted) {
            showConfirm = true
        } else {
            scope.launch {
                val result = snackbar.showSnackbar(
                    message = "Dawuro needs permission to send SMS from your SIM.",
                    actionLabel = "Open settings",
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    )
                }
            }
        }
    }

    val message = uiState.messageText
    val recipients = uiState.selectedContacts
    val mode = uiState.sendMode
    val canPersonalize = mode != SendMode.SMS_APP || recipients.size <= 1
    val longestName = recipients.maxByOrNull { it.name.length }?.name
    val worstCaseText = SmsCounter.simplifyPunctuation(
        MessageTemplates.personalize(message.trim(), if (canPersonalize) longestName else null)
    )
    val count = SmsCounter.count(worstCaseText)
    val specialChars = SmsCounter.unicodeCharacters(worstCaseText)
    val blanks = MessageTemplates.unfilledBlanks(message)

    val blockReason = when {
        uiState.isSending -> "Sending…"
        recipients.isEmpty() -> "Choose who to send to"
        message.isBlank() -> "Write your message"
        mode == SendMode.SMSONLINEGH && !uiState.gatewayConfigured -> "Set up SMS Online GH in Settings, or pick another way to send"
        mode == SendMode.SMSONLINEGH && uiState.activeSenderName.isBlank() -> "Add a sender name in Settings first"
        mode == SendMode.DIRECT_SIM && !simReady -> "No SIM card is ready. Pick another way to send"
        else -> null
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTitleBar(title = "New message", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- To ---
            ComposeCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) {
                            if (uiState.contacts.isEmpty()) onOpenContacts() else showRecipients = true
                        }
                        .padding(16.dp)
                        .testTag("compose_recipients_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.size(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("To", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = when {
                                uiState.contacts.isEmpty() -> "No contacts yet"
                                recipients.isEmpty() -> "No one selected"
                                uiState.allSelected -> "Everyone (${peopleCount(recipients.size)})"
                                else -> peopleCount(recipients.size)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = if (recipients.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        if (recipients.isNotEmpty() && !uiState.allSelected) {
                            Text(
                                text = recipients.take(3).joinToString(", ") { it.name.ifBlank { PhoneNumbers.format(it.phoneNumber) } } +
                                    if (recipients.size > 3) " +${recipients.size - 3} more" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    FilledTonalButton(
                        onClick = { if (uiState.contacts.isEmpty()) onOpenContacts() else showRecipients = true }
                    ) { Text(if (uiState.contacts.isEmpty()) "Add" else "Choose") }
                }
            }

            // --- Message ---
            ComposeCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = field,
                        onValueChange = {
                            field = it
                            if (it.text != uiState.messageText) onMessageChanged(it.text)
                        },
                        label = { Text("Message") },
                        placeholder = { Text("Type your message here…") },
                        minLines = 5,
                        maxLines = 12,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        trailingIcon = if (message.isNotEmpty()) {
                            {
                                IconButton(onClick = {
                                    field = TextFieldValue("")
                                    onMessageChanged("")
                                }) {
                                    Icon(Icons.Rounded.Clear, contentDescription = "Clear message")
                                }
                            }
                        } else null,
                        modifier = Modifier.fillMaxWidth().testTag("sms_message_input")
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                val token = MessageTemplates.NAME_TOKEN
                                val sel = field.selection
                                val newText = field.text.replaceRange(sel.min, sel.max, token)
                                field = TextFieldValue(newText, TextRange(sel.min + token.length))
                                onMessageChanged(newText)
                            },
                            label = { Text("Add their name") },
                            leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize)) },
                            modifier = Modifier.testTag("insert_name_chip")
                        )
                        AssistChip(
                            onClick = { showTemplates = true },
                            label = { Text("Templates") },
                            leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize)) },
                            modifier = Modifier.testTag("templates_chip")
                        )
                    }

                    Text(
                        text = if (message.isBlank()) {
                            "Tip: \"Add their name\" greets each person by name."
                        } else {
                            "${count.characters} characters · ${if (count.parts == 1) "1 SMS" else "${count.parts} SMS"} per person"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (specialChars.isNotEmpty()) {
                        Notice(
                            icon = Icons.Rounded.Info,
                            tone = DawuroTheme.statusColors.progress,
                            text = "Special letters (${specialChars.take(5).joinToString(" ")}) make each SMS hold 70 characters instead of 160."
                        )
                    }
                    if (blanks.isNotEmpty()) {
                        Notice(
                            icon = Icons.Rounded.WarningAmber,
                            tone = DawuroTheme.statusColors.warning,
                            text = "Fill in the blanks: ${blanks.distinct().take(4).joinToString(", ")}"
                        )
                    }
                    if (MessageTemplates.containsName(message) && recipients.isNotEmpty()) {
                        val sample = recipients.firstOrNull { it.hasName } ?: recipients.first()
                        PreviewBubble(
                            who = sample.name.ifBlank { PhoneNumbers.format(sample.phoneNumber) },
                            text = SmsCounter.simplifyPunctuation(
                                MessageTemplates.personalize(message.trim(), if (canPersonalize) sample.name else null)
                            )
                        )
                    }
                }
            }

            // --- How to send ---
            ComposeCard {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        "Send using",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    SendModeOption(
                        selected = mode == SendMode.DIRECT_SIM,
                        icon = Icons.Rounded.SimCard,
                        title = "My SIM card",
                        subtitle = if (simReady) {
                            "Uses your airtime or SMS bundle. People see your number. Best for up to $SIM_COMFORT_LIMIT people."
                        } else {
                            "No SIM card is ready on this phone."
                        },
                        warning = !simReady,
                        onSelect = { onSendModeChanged(SendMode.DIRECT_SIM) },
                        tag = "send_mode_sim"
                    )
                    SendModeOption(
                        selected = mode == SendMode.SMSONLINEGH,
                        icon = Icons.Rounded.Cloud,
                        title = "SMS Online GH",
                        subtitle = when {
                            !uiState.gatewayConfigured -> "Not set up. Send to big groups with your own name as sender."
                            uiState.senderIds.isEmpty() -> "Add a sender name in Settings."
                            else -> "People see \"${uiState.activeSenderName}\". Uses your SMS Online GH credit."
                        },
                        warning = mode == SendMode.SMSONLINEGH && (!uiState.gatewayConfigured || uiState.senderIds.isEmpty()),
                        onSelect = { onSendModeChanged(SendMode.SMSONLINEGH) },
                        tag = "send_mode_gateway"
                    ) {
                        if (mode == SendMode.SMSONLINEGH) {
                            if (!uiState.gatewayConfigured || uiState.senderIds.isEmpty()) {
                                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.padding(top = 8.dp)) {
                                    Text("Set up in Settings")
                                }
                            } else if (uiState.senderIds.size > 1) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    uiState.senderIds.forEach { sender ->
                                        FilterChip(
                                            selected = sender.senderName.equals(uiState.activeSenderName, ignoreCase = true),
                                            onClick = { onSelectSenderId(sender) },
                                            label = { Text(sender.senderName) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    SendModeOption(
                        selected = mode == SendMode.SMS_APP,
                        icon = Icons.Rounded.PhoneAndroid,
                        title = "My Messages app",
                        subtitle = "Opens your normal SMS app so you press send yourself. Names can't be added for each person.",
                        warning = false,
                        onSelect = { onSendModeChanged(SendMode.SMS_APP) },
                        tag = "send_mode_app"
                    )
                }
            }
            Spacer(Modifier.size(8.dp))
        }

        // --- Sticky send bar ---
        Surface(tonalElevation = 3.dp, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
            Box(contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (blockReason != null) {
                    Text(
                        blockReason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        if (mode == SendMode.DIRECT_SIM && !hasSendSmsPermission()) {
                            permissionLauncher.launch(Manifest.permission.SEND_SMS)
                        } else {
                            showConfirm = true
                        }
                    },
                    enabled = blockReason == null,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("review_and_send_sms_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null)
                    Spacer(Modifier.size(10.dp))
                    Text(
                        if (recipients.isEmpty()) "Send" else "Send to ${peopleCount(recipients.size)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            }
        }
    }

    if (showRecipients) {
        RecipientPickerSheet(
            uiState = uiState,
            onDismiss = { showRecipients = false },
            onToggleContact = onToggleContact,
            onSetAllSelected = onSetAllSelected,
            onToggleGroup = onToggleGroup
        )
    }

    if (showTemplates) {
        TemplatesSheet(
            onDismiss = { showTemplates = false },
            onPick = {
                showTemplates = false
                onApplyTemplate(it)
            }
        )
    }

    if (showConfirm) {
        val totalSms = count.parts * recipients.size
        ConfirmSendDialog(
            recipientCount = recipients.size,
            from = when (mode) {
                SendMode.SMSONLINEGH -> uiState.activeSenderName
                SendMode.SMS_APP -> "Your Messages app"
                else -> "Your SIM number"
            },
            costLine = when (mode) {
                SendMode.SMS_APP -> "You will press send in your Messages app."
                SendMode.SMSONLINEGH -> "About $totalSms SMS from your SMS Online GH credit."
                else -> "About $totalSms SMS from your airtime or bundle."
            },
            previewText = SmsCounter.simplifyPunctuation(
                MessageTemplates.personalize(
                    message.trim(),
                    if (canPersonalize) recipients.firstOrNull { it.hasName }?.name else null
                )
            ),
            blanks = blanks,
            bigSimBatch = mode == SendMode.DIRECT_SIM && recipients.size > SIM_COMFORT_LIMIT,
            onDismiss = { showConfirm = false },
            onConfirm = {
                showConfirm = false
                onConfirmSend()
            }
        )
    }
}

@Composable
private fun ComposeCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) { content() }
}

@Composable
private fun Notice(icon: ImageVector, tone: Color, text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = tone.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = tone, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(10.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PreviewBubble(who: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "What $who will see:",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
private fun SendModeOption(
    selected: Boolean,
    icon: ImageVector,
    title: String,
    subtitle: String,
    warning: Boolean,
    onSelect: () -> Unit,
    tag: String,
    extra: @Composable () -> Unit = {}
) {
    val border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
        border = border,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            RadioButton(selected = selected, onClick = null)
            Spacer(Modifier.size(8.dp))
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (warning) DawuroTheme.statusColors.warning else MaterialTheme.colorScheme.onSurfaceVariant
                )
                extra()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RecipientPickerSheet(
    uiState: CampaignUiState,
    onDismiss: () -> Unit,
    onToggleContact: (Long, Boolean) -> Unit,
    onSetAllSelected: (Boolean) -> Unit,
    onToggleGroup: (GroupInfo) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val visible = remember(uiState.contacts, query) {
        val q = query.trim().lowercase()
        val digits = q.filter { it.isDigit() }
        if (q.isEmpty()) uiState.contacts else uiState.contacts.filter {
            it.name.lowercase().contains(q) || it.groupTag.lowercase().contains(q) ||
                (digits.isNotEmpty() && it.phoneNumber.contains(digits))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.92f).navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Choose people", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${uiState.selectedContacts.size} of ${uiState.contacts.size} selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = { onSetAllSelected(!uiState.allSelected) },
                    modifier = Modifier.testTag("send_screen_select_all_checkbox")
                ) { Text(if (uiState.allSelected) "Clear all" else "Select all") }
            }

            if (uiState.groups.isNotEmpty()) {
                Text(
                    "Groups",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    uiState.groups.forEach { group ->
                        FilterChip(
                            selected = group.allSelected,
                            onClick = { onToggleGroup(group) },
                            label = { Text("${group.name} (${group.count})") }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 8.dp)) {
                items(visible, key = { it.id }) { contact ->
                    RecipientRow(contact = contact, onToggle = { onToggleContact(contact.id, !contact.isSelected) })
                }
            }

            HorizontalDivider()
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 52.dp)
            ) { Text("Done") }
        }
    }
}

@Composable
private fun RecipientRow(contact: ContactEntity, onToggle: () -> Unit) {
    val number = PhoneNumbers.format(contact.phoneNumber)
    ListItem(
        headlineContent = { Text(contact.name.ifBlank { number }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            val parts = buildList {
                if (contact.hasName) add(number)
                if (contact.groupTag.isNotBlank()) add(contact.groupTag)
            }
            if (parts.isNotEmpty()) Text(parts.joinToString(" · "), maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = { Avatar(name = contact.name, size = 40.dp) },
        trailingContent = { Checkbox(checked = contact.isSelected, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.selectable(selected = contact.isSelected, onClick = onToggle, role = Role.Checkbox)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplatesSheet(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Text(
            "Templates",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        Text(
            "Pick one, then fill in the [blanks].",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        LazyColumn(modifier = Modifier.navigationBarsPadding(), contentPadding = PaddingValues(vertical = 8.dp)) {
            items(MessageTemplates.all, key = { it.title }) { template ->
                ListItem(
                    headlineContent = { Text(template.title, fontWeight = FontWeight.SemiBold) },
                    supportingContent = {
                        Text(
                            template.text.replace(MessageTemplates.NAME_TOKEN, "Ama"),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onPick(template.text) }.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ConfirmSendDialog(
    recipientCount: Int,
    from: String,
    costLine: String,
    previewText: String,
    blanks: List<String>,
    bigSimBatch: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send to ${peopleCount(recipientCount)}?") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryLine("From", from, Modifier.testTag("confirm_dialog_sender_id"))
                SummaryLine("To", peopleCount(recipientCount), Modifier.testTag("confirm_dialog_recipient_count"))
                Text(costLine, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        previewText,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(12.dp).testTag("confirm_dialog_message_text")
                    )
                }
                if (blanks.isNotEmpty()) {
                    Notice(
                        icon = Icons.Rounded.WarningAmber,
                        tone = DawuroTheme.statusColors.warning,
                        text = "Your message still has blanks: ${blanks.distinct().take(3).joinToString(", ")}"
                    )
                }
                if (bigSimBatch) {
                    Notice(
                        icon = Icons.Rounded.Info,
                        tone = DawuroTheme.statusColors.progress,
                        text = "Your phone may ask to allow sending many SMS. Choose \"Always allow\" so all messages go out."
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, modifier = Modifier.testTag("confirm_send_sms_final_button")) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Send now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_send_sms_button")) { Text("Go back") }
        }
    )
}

@Composable
private fun SummaryLine(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 56.dp)
        )
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}
