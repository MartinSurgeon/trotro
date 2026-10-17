package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.OrganisationProfileEntity
import com.example.data.SenderIdEntity
import com.example.ui.theme.CampaignColorPresets
import com.example.ui.theme.parseHexColor
import com.example.viewmodel.CampaignUiState
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    uiState: CampaignUiState,
    onSendSmsClicked: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onPhotoPicked: (Uri) -> Unit,
    onResetPhoto: () -> Unit,
    onSelectSenderId: (SenderIdEntity) -> Unit,
    onAddSenderId: (senderName: String, candidateLabel: String, defaultGroup: String) -> Unit,
    onDeleteSenderId: (SenderIdEntity) -> Unit,
    onSaveCustomOrganisation: (
        name: String,
        role: String,
        slogan: String,
        primaryHex: String,
        accentHex: String,
        photoPath: String?
    ) -> Unit
) {
    var showCustomizeDialog by remember { mutableStateOf(false) }
    var showAddSenderDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onPhotoPicked(uri)
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val accentColor = MaterialTheme.colorScheme.secondary

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
            // 1. CAMPAIGN FLYER HERO CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("campaign_flyer_hero_card"),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = primaryColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor,
                                    primaryColor.copy(
                                        red = (primaryColor.red * 0.7f).coerceIn(0f, 1f),
                                        green = (primaryColor.green * 0.7f).coerceIn(0f, 1f),
                                        blue = (primaryColor.blue * 0.7f).coerceIn(0f, 1f)
                                    )
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Pill Row: Active Sender ID + Edit Flyer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "SENDER ID: ${uiState.activeSenderName}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { showCustomizeDialog = true }
                                    .testTag("customize_organisation_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = "Customize Organisation",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Edit Flyer",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Campaign Photo / Organisation Logo with Camera Badge
                        Box(
                            contentAlignment = Alignment.BottomEnd,
                            modifier = Modifier
                                .size(136.dp)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("organisation_photo_box")
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(4.dp, Color.White, CircleShape)
                                    .padding(4.dp)
                                    .border(2.dp, accentColor, CircleShape),
                                shape = CircleShape,
                                color = Color.White
                            ) {
                                val customPath = uiState.profile.customPhotoPath
                                val customFile = remember(customPath) {
                                    customPath?.let { File(it) }?.takeIf { it.exists() }
                                }
                                if (customFile != null) {
                                    AsyncImage(
                                        model = customFile,
                                        contentDescription = "${uiState.profile.organisationName} Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_campaign_flyer_art),
                                        contentDescription = "${uiState.profile.organisationName} Campaign Portrait",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = accentColor,
                                border = BorderStroke(2.dp, Color.White),
                                shadowElevation = 4.dp,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AddAPhoto,
                                        contentDescription = "Change Logo or Campaign Photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = uiState.profile.organisationName.uppercase(),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontSize = 26.sp,
                                lineHeight = 30.sp
                            ),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("organisation_name_text")
                        )

                        if (uiState.profile.roleOrSubtitle.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = accentColor,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.5.dp, Color.White)
                            ) {
                                Text(
                                    text = uiState.profile.roleOrSubtitle.uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                        }

                        if (uiState.profile.slogan.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "\"${uiState.profile.slogan}\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.95f),
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // BIG "SEND SMS" BUTTON
                        Button(
                            onClick = onSendSmsClicked,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .testTag("home_send_sms_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.White
                            ),
                            border = BorderStroke(2.dp, Color.White),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 6.dp,
                                pressedElevation = 2.dp
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "SEND SMS",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 22.sp,
                                    letterSpacing = 1.sp
                                ),
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            // 2. MULTIPLE CANDIDATE SENDER IDs CARD (SMSOnlineGH)
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_sender_ids_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                                tint = primaryColor
                            )
                            Column {
                                Text(
                                    text = "Candidate Sender IDs",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Switch Sender ID for different candidates & delegates",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = { showAddSenderDialog = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("home_add_sender_id_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add ID", fontWeight = FontWeight.Bold)
                        }
                    }

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
                                    color = if (isSelected) primaryColor else Color.Gray.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectSenderId(sender) }
                                    .testTag("sender_chip_${sender.senderName}")
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
                                            color = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (uiState.senderIds.size > 1) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove ${sender.senderName}",
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

            // 3. QUICK OVERVIEW & SIMPLE ACTION CARDS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Contacts Quick Card
                ElevatedCard(
                    onClick = onNavigateToContacts,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_contacts_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = primaryColor.copy(alpha = 0.12f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Contacts,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Delegates",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${uiState.contacts.size} Saved",
                            style = MaterialTheme.typography.headlineMedium,
                            color = primaryColor,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${uiState.selectedContacts.size} selected to receive SMS",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Add or Import CSV →",
                            style = MaterialTheme.typography.labelLarge,
                            color = accentColor
                        )
                    }
                }

                // Message History Quick Card
                ElevatedCard(
                    onClick = onNavigateToHistory,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_history_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = accentColor.copy(alpha = 0.12f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                text = "History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${uiState.broadcasts.size} Sent",
                            style = MaterialTheme.typography.headlineMedium,
                            color = primaryColor,
                            fontWeight = FontWeight.ExtraBold
                        )
                        val latestStatus = uiState.broadcasts.firstOrNull()?.let {
                            when (it.overallStatus) {
                                "DELIVERED" -> "Last: Delivered (${it.senderIdUsed})"
                                "SUBMITTED_TO_GATEWAY" -> "Last: Accepted by SMSOnlineGH"
                                "SENT_TO_CARRIER" -> "Last: Sent to carrier"
                                "OPENED_IN_SMS_APP" -> "Last: Opened in SMS app"
                                "FAILED" -> "Last: Failed"
                                else -> "Last: ${it.overallStatus}"
                            }
                        } ?: "No messages sent yet"
                        Text(
                            text = latestStatus,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "View Status →",
                            style = MaterialTheme.typography.labelLarge,
                            color = primaryColor
                        )
                    }
                }
            }

            // 4. CURRENT CAMPAIGN MESSAGE PREVIEW CARD
            ElevatedCard(
                onClick = onSendSmsClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_message_preview_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                imageVector = Icons.Default.Sms,
                                contentDescription = null,
                                tint = primaryColor
                            )
                            Text(
                                text = "Ready Message Draft",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(onClick = onSendSmsClicked) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit & Send")
                        }
                    }

                    Surface(
                        color = primaryColor.copy(alpha = 0.07f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.messageText.ifBlank { "Tap to write your SMS message..." },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // 5. CUSTOMIZE ORGANISATION & COLOURS BUTTON
            OutlinedButton(
                onClick = { showCustomizeDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("home_customize_org_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, primaryColor)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = primaryColor
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Change Organisation Name, Photo & Colours",
                    style = MaterialTheme.typography.titleMedium,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showCustomizeDialog) {
        CustomizeOrganisationDialog(
            currentProfile = uiState.profile,
            onDismiss = { showCustomizeDialog = false },
            onPickPhoto = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onResetPhoto = onResetPhoto,
            onSave = { name, role, slogan, primaryHex, accentHex, photoPath ->
                onSaveCustomOrganisation(name, role, slogan, primaryHex, accentHex, photoPath)
                showCustomizeDialog = false
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
fun AddSenderIdDialog(
    onDismiss: () -> Unit,
    onSave: (senderName: String, candidateLabel: String, defaultGroup: String) -> Unit
) {
    var senderName by remember { mutableStateOf("") }
    var candidateLabel by remember { mutableStateOf("") }
    var defaultGroup by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Candidate Sender ID",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add an approved SMSOnlineGH Sender ID (max 11 characters) for a candidate, portfolio, or group.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = senderName,
                    onValueChange = { if (it.length <= 11) senderName = it },
                    label = { Text("Sender ID (Max 11 chars) *") },
                    placeholder = { Text("e.g. JOYCE AFI or YOUTH WING") },
                    supportingText = { Text("${senderName.length}/11 characters") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_sender_id_input")
                )

                OutlinedTextField(
                    value = candidateLabel,
                    onValueChange = { candidateLabel = it },
                    label = { Text("Candidate / Portfolio Name") },
                    placeholder = { Text("e.g. Joyce Afi Fianu - Women Organizer") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_sender_candidate_input")
                )

                OutlinedTextField(
                    value = defaultGroup,
                    onValueChange = { defaultGroup = it },
                    label = { Text("Target Delegate Group (Optional)") },
                    placeholder = { Text("e.g. Women's Wing, Youth Delegates, All") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        senderName.trim(),
                        candidateLabel.trim(),
                        defaultGroup.trim().ifEmpty { "All" }
                    )
                },
                enabled = senderName.trim().isNotEmpty(),
                modifier = Modifier.testTag("confirm_save_sender_id_button")
            ) {
                Text("Save Sender ID")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomizeOrganisationDialog(
    currentProfile: OrganisationProfileEntity,
    onDismiss: () -> Unit,
    onPickPhoto: () -> Unit,
    onResetPhoto: () -> Unit,
    onSave: (
        name: String,
        role: String,
        slogan: String,
        primaryHex: String,
        accentHex: String,
        photoPath: String?
    ) -> Unit
) {
    var orgName by remember { mutableStateOf(currentProfile.organisationName) }
    var orgRole by remember { mutableStateOf(currentProfile.roleOrSubtitle) }
    var orgSlogan by remember { mutableStateOf(currentProfile.slogan) }
    var primaryHex by remember { mutableStateOf(currentProfile.primaryColorHex) }
    var accentHex by remember { mutableStateOf(currentProfile.accentColorHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Customize Organisation & Flyer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Set up this app for any candidate, political party, church, association, or community group.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = orgName,
                    onValueChange = { orgName = it },
                    label = { Text("Organisation or Candidate Name") },
                    placeholder = { Text("e.g. JOYCE AFI FIANU or GRACE CHAPEL") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("org_name_input")
                )

                OutlinedTextField(
                    value = orgRole,
                    onValueChange = { orgRole = it },
                    label = { Text("Subtitle / Position / Group") },
                    placeholder = { Text("e.g. FOR WOMEN ORGANIZER") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("org_role_input")
                )

                OutlinedTextField(
                    value = orgSlogan,
                    onValueChange = { orgSlogan = it },
                    label = { Text("Slogan or Motto") },
                    placeholder = { Text("e.g. Together, we can work for change.") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("org_slogan_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onPickPhoto,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Photo / Logo")
                    }

                    if (currentProfile.customPhotoPath != null) {
                        OutlinedButton(
                            onClick = onResetPhoto,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Photo",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Default")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose Colour Theme:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CampaignColorPresets.forEach { preset ->
                        val isSelected = primaryHex.equals(preset.primaryHex, ignoreCase = true) &&
                            accentHex.equals(preset.accentHex, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                preset.primaryColor.copy(alpha = 0.14f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) preset.primaryColor else Color.Gray.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    primaryHex = preset.primaryHex
                                    accentHex = preset.accentHex
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(preset.primaryColor)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(preset.accentColor)
                                )
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = preset.primaryColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = primaryHex,
                        onValueChange = { primaryHex = it },
                        label = { Text("Main Colour") },
                        singleLine = true,
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(primaryHex, Color(0xFF0B4F26)))
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = accentHex,
                        onValueChange = { accentHex = it },
                        label = { Text("Accent Colour") },
                        singleLine = true,
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(accentHex, Color(0xFFD32F2F)))
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        orgName,
                        orgRole,
                        orgSlogan,
                        primaryHex,
                        accentHex,
                        currentProfile.customPhotoPath
                    )
                },
                modifier = Modifier.testTag("save_org_customization_button")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
