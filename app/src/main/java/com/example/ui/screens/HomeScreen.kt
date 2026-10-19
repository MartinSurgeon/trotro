package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.MessageBroadcastEntity
import com.example.ui.components.Avatar
import com.example.ui.components.KenteStripe
import com.example.ui.components.PrimaryActionButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusTone
import com.example.ui.components.formatWhen
import com.example.ui.components.outcomeOf
import com.example.ui.components.peopleCount
import com.example.ui.components.toneColor
import com.example.viewmodel.CampaignUiState
import java.util.Calendar

@Composable
fun HomeScreen(
    uiState: CampaignUiState,
    onNewMessage: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenNeedsAttention: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBroadcast: (Long) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HomeTopBar(uiState = uiState, onOpenSettings = onOpenSettings)

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Only the single most important status is shown, to keep Home calm.
                val sending = uiState.sendingBroadcast
                when {
                    sending != null -> SendingCard(uiState, sending, onClick = { onOpenBroadcast(sending.id) })
                    uiState.attentionPeople > 0 -> AttentionCard(
                        people = uiState.attentionPeople,
                        messages = uiState.attentionBroadcasts.size,
                        onReview = onOpenNeedsAttention
                    )
                }

                PrimaryActionButton(
                    text = "New message",
                    icon = Icons.AutoMirrored.Rounded.Send,
                    onClick = onNewMessage,
                    modifier = Modifier.testTag("home_send_sms_button")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickAction(Icons.Rounded.People, "Contacts", onOpenContacts, Modifier.weight(1f).testTag("home_contacts_card"))
                    QuickAction(Icons.Rounded.AutoAwesome, "Templates", onOpenTemplates, Modifier.weight(1f))
                    QuickAction(Icons.AutoMirrored.Rounded.Chat, "Messages", onOpenHistory, Modifier.weight(1f).testTag("home_history_card"))
                    QuickAction(Icons.Rounded.Settings, "Settings", onOpenSettings, Modifier.weight(1f))
                }

                if (uiState.contacts.isEmpty() || uiState.broadcasts.isEmpty()) {
                    GettingStartedCard(
                        hasContacts = uiState.contacts.isNotEmpty(),
                        hasSent = uiState.broadcasts.isNotEmpty(),
                        onAddContacts = onOpenContacts,
                        onWriteMessage = onNewMessage
                    )
                } else {
                    MonthSummaryCard(uiState)
                }

                if (uiState.broadcasts.isNotEmpty()) {
                    Column {
                        SectionHeader(
                            title = "Recent messages",
                            trailing = { TextButton(onClick = onOpenHistory) { Text("See all") } }
                        )
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            uiState.broadcasts.take(4).forEachIndexed { index, broadcast ->
                                if (index > 0) HorizontalDivider(modifier = Modifier.padding(start = 64.dp))
                                RecentMessageRow(uiState, broadcast, onClick = { onOpenBroadcast(broadcast.id) })
                            }
                        }
                    }
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
private fun HomeTopBar(uiState: CampaignUiState, onOpenSettings: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                name = uiState.displayName,
                photoPath = uiState.profile.customPhotoPath,
                size = 52.dp,
                background = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onOpenSettings)
                    .semantics { contentDescription = "Your profile" }
            )
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    greeting(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    uiState.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("organisation_name_text")
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings")
            }
        }
        KenteStripe(height = 3.dp)
    }
}

@Composable
private fun SendingCard(uiState: CampaignUiState, broadcast: MessageBroadcastEntity, onClick: () -> Unit) {
    val outcome = outcomeOf(uiState.broadcastCounts[broadcast.id], broadcast.totalRecipients, broadcast.sendMode) ?: return
    val color = toneColor(StatusTone.PROGRESS)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = color)
                Spacer(Modifier.size(10.dp))
                Text(outcome.headline, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
            }
            LinearProgressIndicator(
                progress = { outcome.progress },
                color = color,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "You can keep using the app while it sends.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AttentionCard(people: Int, messages: Int, onReview: () -> Unit) {
    val color = toneColor(StatusTone.WARNING)
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth().testTag("home_attention_card")
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (people == 1) "1 person didn't get your message" else "$people people didn't get your messages",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    if (messages == 1) "In 1 message · you can try again" else "In $messages messages · you can try again",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.size(8.dp))
            Button(
                onClick = onReview,
                colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = MaterialTheme.colorScheme.surface)
            ) { Text("Review") }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null) }
        }
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun MonthSummaryCard(uiState: CampaignUiState) {
    val monthStart = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val thisMonth = uiState.broadcasts.filter { it.timestamp >= monthStart }
    val outcomes = thisMonth.mapNotNull { outcomeOf(uiState.broadcastCounts[it.id], it.totalRecipients, it.sendMode) }
    val reached = outcomes.sumOf { it.sent }
    val attempted = outcomes.sumOf { it.sent + it.failed }
    val rate = if (attempted == 0) null else (reached * 100) / attempted

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("This month", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(12.dp))
            Row {
                SummaryStat(thisMonth.size.toString(), if (thisMonth.size == 1) "message" else "messages", Modifier.weight(1f))
                SummaryStat(reached.toString(), "people reached", Modifier.weight(1f))
                SummaryStat(rate?.let { "$it%" } ?: "–", "sent OK", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RecentMessageRow(uiState: CampaignUiState, broadcast: MessageBroadcastEntity, onClick: () -> Unit) {
    val outcome = outcomeOf(uiState.broadcastCounts[broadcast.id], broadcast.totalRecipients, broadcast.sendMode)
    val tone = outcome?.tone ?: StatusTone.NEUTRAL
    val icon = when (tone) {
        StatusTone.SUCCESS -> Icons.Rounded.CheckCircle
        StatusTone.PROGRESS -> Icons.Rounded.Schedule
        StatusTone.WARNING -> Icons.Rounded.WarningAmber
        StatusTone.ERROR -> Icons.Rounded.ErrorOutline
        StatusTone.NEUTRAL -> Icons.AutoMirrored.Rounded.Send
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = toneColor(tone), modifier = Modifier.size(28.dp))
        Spacer(Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                broadcast.messageText,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${formatWhen(broadcast.timestamp)} · ${outcome?.headline ?: peopleCount(broadcast.totalRecipients)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
                subtitle = "From your phone, a list, or type them in",
                onClick = onAddContacts
            )
            ChecklistRow(
                done = hasSent,
                title = "Send your first message",
                subtitle = "Templates help you write it quickly",
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
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
        }
    }
}
