package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.MessageBroadcastEntity
import com.example.ui.components.Avatar
import com.example.ui.components.KenteStripe
import com.example.ui.components.PrimaryActionButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.broadcastStatusLabel
import com.example.ui.components.formatWhen
import com.example.ui.components.peopleCount
import com.example.viewmodel.CampaignUiState
import java.util.Calendar

@Composable
fun HomeScreen(
    uiState: CampaignUiState,
    onNewMessage: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBroadcast: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HomeHeader(uiState = uiState, onOpenSettings = onOpenSettings)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PrimaryActionButton(
                text = "New message",
                icon = Icons.AutoMirrored.Rounded.Send,
                onClick = onNewMessage,
                modifier = Modifier.testTag("home_send_sms_button")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    value = uiState.contacts.size.toString(),
                    label = if (uiState.contacts.size == 1) "Contact" else "Contacts",
                    onClick = onOpenContacts,
                    modifier = Modifier.weight(1f).testTag("home_contacts_card")
                )
                StatTile(
                    value = uiState.groups.size.toString(),
                    label = if (uiState.groups.size == 1) "Group" else "Groups",
                    onClick = onOpenContacts,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    value = uiState.broadcasts.size.toString(),
                    label = "Sent",
                    onClick = onOpenHistory,
                    modifier = Modifier.weight(1f).testTag("home_history_card")
                )
            }

            if (uiState.contacts.isEmpty() || uiState.broadcasts.isEmpty()) {
                GettingStartedCard(
                    hasContacts = uiState.contacts.isNotEmpty(),
                    hasSent = uiState.broadcasts.isNotEmpty(),
                    onAddContacts = onOpenContacts,
                    onWriteMessage = onNewMessage
                )
            }

            if (uiState.broadcasts.isNotEmpty()) {
                SectionHeader(
                    title = "Recent messages",
                    trailing = { TextButton(onClick = onOpenHistory) { Text("See all") } }
                )
                uiState.broadcasts.take(3).forEach { broadcast ->
                    RecentMessageCard(broadcast = broadcast, onClick = { onOpenBroadcast(broadcast.id) })
                }
            }
        }
    }
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 0..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

@Composable
private fun HomeHeader(uiState: CampaignUiState, onOpenSettings: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(
                    name = uiState.displayName,
                    photoPath = uiState.profile.customPhotoPath,
                    size = 60.dp,
                    background = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onOpenSettings)
                        .semantics { contentDescription = "Change profile" }
                )
                Spacer(Modifier.size(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${greeting()},",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = uiState.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("organisation_name_text")
                    )
                    if (uiState.profile.roleOrSubtitle.isNotBlank()) {
                        Text(
                            text = uiState.profile.roleOrSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            KenteStripe()
        }
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GettingStartedCard(
    hasContacts: Boolean,
    hasSent: Boolean,
    onAddContacts: () -> Unit,
    onWriteMessage: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(
                text = "Get started in 2 steps",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            ChecklistRow(
                done = hasContacts,
                title = "Add the people you want to reach",
                subtitle = "From your phonebook, a CSV file, or type them in",
                onClick = onAddContacts
            )
            ChecklistRow(
                done = hasSent,
                title = "Write and send your first message",
                subtitle = "Use a ready-made template to save time",
                onClick = onWriteMessage
            )
        }
    }
}

@Composable
private fun ChecklistRow(done: Boolean, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 64.dp)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (done) "Done" else "Not done yet",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun RecentMessageCard(broadcast: MessageBroadcastEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${formatWhen(broadcast.timestamp)} · ${peopleCount(broadcast.totalRecipients)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                StatusPill(broadcastStatusLabel(broadcast.overallStatus))
            }
            Text(
                text = broadcast.messageText,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
