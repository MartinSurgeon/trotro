package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.SenderIdEntity
import com.example.viewmodel.CampaignUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SendSmsScreen(
    uiState: CampaignUiState,
    isSmsOnlineGhConfigured: () -> Boolean,
    hasSendSmsPermission: () -> Boolean,
    isSimReady: () -> Boolean,
    onBackToHome: () -> Unit,
    onSelectSenderId: (SenderIdEntity) -> Unit,
    onAddSenderId: (senderName: String, candidateLabel: String, defaultGroup: String) -> Unit,
    onDeleteSenderId: (SenderIdEntity) -> Unit,
    onSelectDelegatesByGroup: (String) -> Unit,
    onMessageChanged: (String) -> Unit,
    onSendModeChanged: (String) -> Unit,
    onToggleContactSelection: (Long, Boolean) -> Unit,
    onSetAllContactsSelected: (Boolean) -> Unit,
    onNavigateToContacts: () -> Unit,
    onLoadSampleContacts: () -> Unit,
    onConfirmAndSend: () -> Unit,
    onPermissionDeniedMessage: (String) -> Unit
) {
    BackHandler { onBackToHome() }

    val primaryColor = MaterialTheme.colorScheme.primary
    val accentColor = MaterialTheme.colorScheme.secondary

    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showRecipientsExpanded by remember { mutableStateOf(true) }
    var showAddSenderDialog by remember { mutableStateOf(false) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showConfirmationDialog = true
        } else {
            onPermissionDeniedMessage(
                "SMS permission was denied. Use SMSOnlineGH or 'Open in Phone's SMS App'."
            )
        }
    }

    val charCount = uiState.messageText.length
    val smsSegments = if (charCount == 0) 0 else ((charCount - 1) / 160) + 1
    val apiKeyConfigured = isSmsOnlineGhConfigured()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // STEP 1: SELECT CANDIDATE SENDER ID (MULTIPLE CANDIDATES SUPPORT)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "1. Select Candidate Sender ID",
                                style = MaterialTheme.typography.titleLarge,
                                color = primaryColor,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        FilledTonalButton(
                            onClick = { showAddSenderDialog = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("send_screen_add_sender_id_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Sender ID", fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = "Different candidates can select their approved SMSOnlineGH Sender ID below to message their delegates:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.senderIds.forEach { sender ->
                            val isSelected = sender.senderName.equals(uiState.activeSenderName, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) primaryColor else Color.Gray.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectSenderId(sender) }
                                    .testTag("send_screen_sender_chip_${sender.senderName}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = sender.senderName,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = sender.candidateOrOrgLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isSelected) Color.White.copy(alpha = 0.88f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (uiState.senderIds.size > 1) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete ${sender.senderName}",
                                            tint = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { onDeleteSenderId(sender) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // STEP 2: WRITE MESSAGE CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "2. Write Your Message",
                                style = MaterialTheme.typography.titleLarge,
                                color = primaryColor,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        if (uiState.messageText.isNotEmpty()) {
                            TextButton(
                                onClick = { onMessageChanged("") },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear message",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear")
                            }
                        }
                    }

                    OutlinedTextField(
                        value = uiState.messageText,
                        onValueChange = onMessageChanged,
                        label = { Text("SMS Message (Sender: ${uiState.activeSenderName})") },
                        placeholder = {
                            Text(
                                "Please vote for Joyce Afi Fianu as your Women Organizer. Together, we can work for change. Thank you."
                            )
                        },
                        minLines = 4,
                        maxLines = 8,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sms_message_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tip: Use {name} to insert each delegate's name",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$charCount chars ($smsSegments SMS)",
                            style = MaterialTheme.typography.labelMedium,
                            color = primaryColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Quick Candidate & Group Templates:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                onMessageChanged(
                                    "Please vote for Joyce Afi Fianu as your Women Organizer. Together, we can work for change. Thank you."
                                )
                            },
                            label = { Text("Joyce Afi Fianu Flyer") }
                        )
                        AssistChip(
                            onClick = {
                                onMessageChanged(
                                    "Dear {name}, I humbly ask for your vote and support in the upcoming delegates election. Together we win!"
                                )
                            },
                            label = { Text("Delegates Appeal") }
                        )
                        AssistChip(
                            onClick = {
                                onMessageChanged(
                                    "Hello {name}, reminder that voting and accreditation starts at 8:00 AM tomorrow. Thank you for your steadfast support."
                                )
                            },
                            label = { Text("Election Day Reminder") }
                        )
                    }
                }
            }

            // STEP 3: SELECT RECIPIENTS / DELEGATES CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "3. Select Delegates / Recipients",
                                style = MaterialTheme.typography.titleLarge,
                                color = primaryColor,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        TextButton(onClick = onNavigateToContacts) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add / CSV")
                        }
                    }

                    if (uiState.contacts.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = accentColor.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "No delegate contacts in your list yet.",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = onNavigateToContacts,
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                                    ) {
                                        Text("Add or Import CSV")
                                    }
                                    FilledTonalButton(
                                        onClick = onLoadSampleContacts,
                                        modifier = Modifier.testTag("send_screen_load_sample_button")
                                    ) {
                                        Text("Load Sample Delegates")
                                    }
                                }
                            }
                        }
                    } else {
                        // Quick Delegate Group Filter Chips (for different candidates' delegates)
                        if (uiState.delegateGroups.size > 2) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Select by Delegate Group:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                uiState.delegateGroups.forEach { groupName ->
                                    AssistChip(
                                        onClick = { onSelectDelegatesByGroup(groupName) },
                                        label = { Text(groupName) }
                                    )
                                }
                            }
                        }

                        // Select All / Deselect All + Expand Toggle
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = primaryColor.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable {
                                            onSetAllContactsSelected(!uiState.allSelected)
                                        }
                                        .testTag("send_screen_select_all_checkbox")
                                ) {
                                    Checkbox(
                                        checked = uiState.allSelected,
                                        onCheckedChange = { onSetAllContactsSelected(it) },
                                        colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                                    )
                                    Text(
                                        text = "${uiState.selectedContacts.size} of ${uiState.contacts.size} Selected",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        onClick = { onSetAllContactsSelected(!uiState.allSelected) }
                                    ) {
                                        Text(
                                            text = if (uiState.allSelected) "Deselect All" else "Select All",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Icon(
                                        imageVector = if (showRecipientsExpanded) {
                                            Icons.Default.ExpandLess
                                        } else {
                                            Icons.Default.ExpandMore
                                        },
                                        contentDescription = "Toggle recipient list",
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable { showRecipientsExpanded = !showRecipientsExpanded }
                                            .padding(4.dp)
                                    )
                                }
                            }
                        }

                        if (showRecipientsExpanded) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                uiState.contacts.forEach { contact ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                onToggleContactSelection(contact.id, !contact.isSelected)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = contact.isSelected,
                                            onCheckedChange = {
                                                onToggleContactSelection(contact.id, it)
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = contact.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${contact.phoneNumber} • ${contact.groupTag}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // STEP 4: SENDING METHOD CARD (SMSOnlineGH v5 Default)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "4. SMS Service (SMSOnlineGH)",
                        style = MaterialTheme.typography.titleLarge,
                        color = primaryColor,
                        fontWeight = FontWeight.ExtraBold
                    )

                    // Option A: Official SMSOnlineGH v5 API (Default)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (uiState.sendMode == "SMSONLINEGH") {
                            primaryColor.copy(alpha = 0.09f)
                        } else {
                            Color.Transparent
                        },
                        border = BorderStroke(
                            1.5.dp,
                            if (uiState.sendMode == "SMSONLINEGH") primaryColor else Color.LightGray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSendModeChanged("SMSONLINEGH") }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.sendMode == "SMSONLINEGH",
                                onClick = { onSendModeChanged("SMSONLINEGH") }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (apiKeyConfigured) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = primaryColor
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SMSOnlineGH Gateway (dev.smsonlinegh.com)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (apiKeyConfigured) {
                                        "Ready • Sends using Sender ID '${uiState.activeSenderName}' via api.smsonlinegh.com/v5"
                                    } else {
                                        "Uses Sender ID '${uiState.activeSenderName}'. Set SMSONLINEGH_API_KEY in the AI Studio Secrets panel."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Option B: Direct SIM Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (uiState.sendMode == "DIRECT_SIM") {
                            primaryColor.copy(alpha = 0.09f)
                        } else {
                            Color.Transparent
                        },
                        border = BorderStroke(
                            1.dp,
                            if (uiState.sendMode == "DIRECT_SIM") primaryColor else Color.LightGray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSendModeChanged("DIRECT_SIM") }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.sendMode == "DIRECT_SIM",
                                onClick = { onSendModeChanged("DIRECT_SIM") }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.SimCard,
                                contentDescription = null,
                                tint = primaryColor
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Direct SIM Card SMS",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Sends directly via your phone SIM card.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Option C: Device SMS App
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (uiState.sendMode == "SMS_APP") {
                            primaryColor.copy(alpha = 0.09f)
                        } else {
                            Color.Transparent
                        },
                        border = BorderStroke(
                            1.dp,
                            if (uiState.sendMode == "SMS_APP") primaryColor else Color.LightGray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSendModeChanged("SMS_APP") }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.sendMode == "SMS_APP",
                                onClick = { onSendModeChanged("SMS_APP") }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = primaryColor
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Open in Phone's SMS App",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Opens your default Android Messages app pre-filled with recipients.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // STEP 5: BIG REVIEW & SEND BUTTON
            Button(
                onClick = {
                    if (uiState.sendMode == "DIRECT_SIM" && !hasSendSmsPermission()) {
                        smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                    } else {
                        showConfirmationDialog = true
                    }
                },
                enabled = uiState.messageText.isNotBlank() && uiState.selectedContacts.isNotEmpty() && !uiState.isSending,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .testTag("review_and_send_sms_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "SEND AS '${uiState.activeSenderName}' (${uiState.selectedContacts.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // CONFIRMATION DIALOG BEFORE SENDING
    if (showConfirmationDialog) {
        ConfirmSendSmsDialog(
            senderId = uiState.activeSenderName,
            candidateLabel = uiState.activeCandidateLabel,
            messageText = uiState.messageText.trim(),
            selectedRecipientsCount = uiState.selectedContacts.size,
            recipientNamesPreview = uiState.selectedContacts.take(5).joinToString(", ") {
                "${it.name} (${it.phoneNumber})"
            },
            sendMode = uiState.sendMode,
            apiKeyConfigured = apiKeyConfigured,
            simReady = isSimReady(),
            primaryColor = primaryColor,
            accentColor = accentColor,
            onDismiss = { showConfirmationDialog = false },
            onConfirmSend = {
                showConfirmationDialog = false
                onConfirmAndSend()
            }
        )
    }

    if (showAddSenderDialog) {
        AddSenderIdDialog(
            onDismiss = { showAddSenderDialog = false },
            onSave = { senderName, candidateLabel, defaultGroup ->
                onAddSenderId(senderName, candidateLabel, defaultGroup)
                showAddSenderDialog = false
            }
        )
    }
}

@Composable
fun ConfirmSendSmsDialog(
    senderId: String,
    candidateLabel: String,
    messageText: String,
    selectedRecipientsCount: Int,
    recipientNamesPreview: String,
    sendMode: String,
    apiKeyConfigured: Boolean,
    simReady: Boolean,
    primaryColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirmSend: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sms,
                    contentDescription = null,
                    tint = primaryColor
                )
                Text(
                    text = "Confirm Before Sending",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Please confirm the Sender ID, recipient count, and message before sending:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Active Candidate Sender ID Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Sender ID: $senderId",
                            style = MaterialTheme.typography.titleMedium,
                            color = accentColor,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.testTag("confirm_dialog_sender_id")
                        )
                        Text(
                            text = candidateLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Number of Selected Recipients Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = primaryColor.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Delegates Selected: $selectedRecipientsCount",
                            style = MaterialTheme.typography.titleMedium,
                            color = primaryColor,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.testTag("confirm_dialog_recipient_count")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = recipientNamesPreview + if (selectedRecipientsCount > 5) " ..." else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = "Message to Send:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = messageText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(12.dp)
                            .testTag("confirm_dialog_message_text")
                    )
                }

                val methodNote = when (sendMode) {
                    "SMS_APP" -> "Will open your phone's default SMS app with all $selectedRecipientsCount recipient(s) pre-filled."
                    "DIRECT_SIM" -> if (simReady) {
                        "Will send directly using your active SIM card."
                    } else {
                        "Note: No active SIM card was detected on this device."
                    }
                    else -> if (apiKeyConfigured) {
                        "Will submit via SMSOnlineGH v5 API (api.smsonlinegh.com) using Sender ID '$senderId' and verify HSHK_OK handshake."
                    } else {
                        "Note: SMSONLINEGH_API_KEY has not been set in the AI Studio Secrets panel yet. Add your key in the Secrets panel for live SMSOnlineGH delivery."
                    }
                }

                Text(
                    text = methodNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmSend,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("confirm_send_sms_final_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Confirm & Send",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_send_sms_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
