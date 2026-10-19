package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.LocalSnackbarHostState
import com.example.data.DeliveryStatus
import com.example.data.MessageBroadcastEntity
import com.example.data.RecipientDeliveryEntity
import com.example.data.SendMode
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.EmptyState
import com.example.ui.components.Outcome
import com.example.ui.components.PrimaryActionButton
import com.example.ui.components.ScreenTitleBar
import com.example.ui.components.SkeletonList
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusTone
import com.example.ui.components.broadcastStatusLabel
import com.example.ui.components.dateSection
import com.example.ui.components.deliveryStatusLabel
import com.example.ui.components.formatWhen
import com.example.ui.components.groupFailureReasons
import com.example.ui.components.outcomeOf
import com.example.ui.components.peopleCount
import com.example.ui.components.toneColor
import com.example.util.PhoneNumbers
import com.example.viewmodel.CampaignUiState
import com.example.viewmodel.HistoryFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

private fun sendModeLabel(mode: String): String = when (mode) {
    SendMode.SMS_APP -> "Messages app"
    SendMode.DIRECT_SIM -> "My SIM"
    else -> "SMS Online GH"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    uiState: CampaignUiState,
    observeDeliveries: (Long) -> Flow<List<RecipientDeliveryEntity>>,
    onOpenBroadcast: (Long?) -> Unit,
    onSetFilter: (HistoryFilter) -> Unit,
    onReuseMessage: (String) -> Unit,
    onRetryFailed: (Long) -> Unit,
    onMarkDone: (Long) -> Unit,
    onDeleteBroadcast: (Long) -> Unit,
    onNewMessage: () -> Unit,
    hasSendSmsPermission: () -> Boolean
) {
    val open = uiState.openBroadcastId?.let { id -> uiState.broadcasts.firstOrNull { it.id == id } }
    if (open != null) {
        BroadcastDetail(
            broadcast = open,
            outcome = outcomeOf(uiState.broadcastCounts[open.id], open.totalRecipients, open.sendMode),
            canMarkDone = uiState.attentionBroadcasts.any { it.id == open.id },
            isSending = uiState.isSending,
            observeDeliveries = observeDeliveries,
            onBack = { onOpenBroadcast(null) },
            onReuseMessage = { onReuseMessage(open.messageText) },
            onRetryFailed = { onRetryFailed(open.id) },
            onMarkDone = { onMarkDone(open.id) },
            onDelete = { onDeleteBroadcast(open.id) },
            hasSendSmsPermission = hasSendSmsPermission
        )
        return
    }

    val attention = uiState.attentionBroadcasts
    val filter = if (attention.isEmpty()) HistoryFilter.ALL else uiState.historyFilter
    val shown = if (filter == HistoryFilter.NEEDS_ATTENTION) attention else uiState.broadcasts
    val sections = remember(shown) { shown.groupBy { dateSection(it.timestamp) } }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenTitleBar(title = "Messages")
            if (uiState.broadcasts.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.Forum,
                    title = "No messages yet",
                    message = "Messages you send will show here, with who got them and who didn't.",
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    action = {
                        PrimaryActionButton(
                            text = "Write a message",
                            icon = Icons.AutoMirrored.Rounded.Send,
                            onClick = onNewMessage,
                            modifier = Modifier.widthIn(max = 320.dp).testTag("history_empty_send_sms_button")
                        )
                    }
                )
            } else {
                if (attention.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = filter == HistoryFilter.ALL,
                                onClick = { onSetFilter(HistoryFilter.ALL) },
                                label = { Text("All") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = filter == HistoryFilter.NEEDS_ATTENTION,
                                onClick = { onSetFilter(HistoryFilter.NEEDS_ATTENTION) },
                                label = { Text("Needs attention") },
                                trailingIcon = { Badge { Text(attention.size.toString()) } },
                                modifier = Modifier.testTag("filter_needs_attention")
                            )
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().testTag("history_lazy_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    sections.forEach { (title, items) ->
                        stickyHeader(key = "header_$title") {
                            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 4.dp)
                                )
                            }
                        }
                        items(items, key = { it.id }) { broadcast ->
                            BroadcastCard(
                                broadcast = broadcast,
                                outcome = outcomeOf(uiState.broadcastCounts[broadcast.id], broadcast.totalRecipients, broadcast.sendMode),
                                onClick = { onOpenBroadcast(broadcast.id) }
                            )
                        }
                    }
                }
            }
        }
        if (uiState.broadcasts.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = onNewMessage,
                icon = { Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null) },
                text = { Text("New message") },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            )
        }
    }
}

@Composable
private fun outcomeIcon(tone: StatusTone) = when (tone) {
    StatusTone.SUCCESS -> Icons.Rounded.CheckCircle
    StatusTone.PROGRESS -> Icons.Rounded.Schedule
    StatusTone.WARNING -> Icons.Rounded.WarningAmber
    StatusTone.ERROR -> Icons.Rounded.ErrorOutline
    StatusTone.NEUTRAL -> Icons.AutoMirrored.Rounded.Send
}

@Composable
private fun BroadcastCard(broadcast: MessageBroadcastEntity, outcome: Outcome?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().testTag("history_item_${broadcast.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                broadcast.messageText,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (outcome != null) {
                    val color = toneColor(outcome.tone)
                    Icon(outcomeIcon(outcome.tone), contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(outcome.headline, style = MaterialTheme.typography.labelLarge, color = color)
                } else {
                    StatusPill(broadcastStatusLabel(broadcast.overallStatus))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "${formatWhen(broadcast.timestamp).substringAfter(", ")} · ${sendModeLabel(broadcast.sendMode)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (outcome != null && outcome.isSending) {
                LinearProgressIndicator(
                    progress = { outcome.progress },
                    color = toneColor(StatusTone.PROGRESS),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private enum class PeopleFilter(val label: String) { NOT_SENT("Didn't get it"), SENT("Sent"), ALL("Everyone") }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BroadcastDetail(
    broadcast: MessageBroadcastEntity,
    outcome: Outcome?,
    canMarkDone: Boolean,
    isSending: Boolean,
    observeDeliveries: (Long) -> Flow<List<RecipientDeliveryEntity>>,
    onBack: () -> Unit,
    onReuseMessage: () -> Unit,
    onRetryFailed: () -> Unit,
    onMarkDone: () -> Unit,
    onDelete: () -> Unit,
    hasSendSmsPermission: () -> Boolean
) {
    val deliveriesFlow = remember(broadcast.id) { observeDeliveries(broadcast.id) }
    // null until the first load, so a skeleton shows instead of an empty list.
    val deliveries by deliveriesFlow.collectAsStateWithLifecycle(initialValue = null)
    var confirmDelete by remember { mutableStateOf(false) }
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    val hasFailures = (outcome?.failed ?: 0) > 0
    var peopleFilter by rememberSaveable(broadcast.id) {
        mutableStateOf(if (hasFailures) PeopleFilter.NOT_SENT else PeopleFilter.ALL)
    }
    if (peopleFilter == PeopleFilter.NOT_SENT && !hasFailures) peopleFilter = PeopleFilter.ALL

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onRetryFailed() else scope.launch { snackbar.showSnackbar("Allow SMS permission to try again from your SIM") }
    }
    val retry = {
        if (broadcast.sendMode == SendMode.DIRECT_SIM && !hasSendSmsPermission()) {
            permissionLauncher.launch(Manifest.permission.SEND_SMS)
        } else {
            onRetryFailed()
        }
    }

    val list = deliveries
    val visible = list?.filter {
        when (peopleFilter) {
            PeopleFilter.ALL -> true
            PeopleFilter.NOT_SENT -> it.status == DeliveryStatus.FAILED
            PeopleFilter.SENT -> it.status in listOf(DeliveryStatus.SENT, DeliveryStatus.SUBMITTED, DeliveryStatus.DELIVERED)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTitleBar(
            title = formatWhen(broadcast.timestamp),
            onBack = onBack,
            actions = {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete from history")
                }
            }
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomEnd = 18.dp, bottomStart = 18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(broadcast.messageText, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(14.dp))
                    }

                    if (outcome != null) {
                        OutcomeSummary(outcome, broadcast)
                    } else {
                        Text(broadcast.statusDetail, style = MaterialTheme.typography.bodyMedium)
                    }

                    if (outcome != null && outcome.hasFailures) {
                        FailureCard(
                            failedCount = outcome.failed,
                            deliveries = list,
                            canRetry = !isSending,
                            canMarkDone = canMarkDone,
                            onRetry = retry,
                            onMarkDone = onMarkDone
                        )
                    }

                    OutlinedButton(
                        onClick = onReuseMessage,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("reuse_message_button_${broadcast.id}")
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Edit and send again")
                    }

                    if (broadcast.sendMode != SendMode.SMS_APP && broadcast.totalRecipients > 1) {
                        Text("People", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PeopleFilter.entries
                                .filter { it != PeopleFilter.NOT_SENT || hasFailures }
                                .forEach { f ->
                                    FilterChip(selected = peopleFilter == f, onClick = { peopleFilter = f }, label = { Text(f.label) })
                                }
                        }
                    }
                }
            }
            if (visible == null) {
                item { SkeletonList(rows = 4) }
            } else {
                items(visible, key = { it.id }) { delivery -> DeliveryRow(delivery) }
                if (visible.isEmpty() && list.isNotEmpty()) {
                    item {
                        Text(
                            "No one here.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete this message?",
            message = "It will be removed from your history. Messages already sent cannot be taken back.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun OutcomeSummary(outcome: Outcome, broadcast: MessageBroadcastEntity) {
    val color = toneColor(outcome.tone)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(outcomeIcon(outcome.tone), contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.size(12.dp))
            Column {
                Text(outcome.headline, style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("outcome_headline"))
                Text(
                    "${peopleCount(broadcast.totalRecipients)} · ${sendModeLabel(broadcast.sendMode)}" +
                        if (broadcast.senderIdUsed.isNotBlank()) " · from ${broadcast.senderIdUsed}" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        LinearProgressIndicator(
            progress = { if (outcome.total == 0) 0f else outcome.sent.toFloat() / outcome.total },
            color = toneColor(if (outcome.isSending) StatusTone.PROGRESS else StatusTone.SUCCESS),
            modifier = Modifier.fillMaxWidth()
        )
        if (outcome.isSending) {
            Text(
                "You can leave this screen. Sending continues in the background.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (broadcast.sendMode == SendMode.DIRECT_SIM && outcome.sent > 0) {
            Text(
                "\"Sent\" means it left your phone. Some networks don't confirm delivery.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FailureCard(
    failedCount: Int,
    deliveries: List<RecipientDeliveryEntity>?,
    canRetry: Boolean,
    canMarkDone: Boolean,
    onRetry: () -> Unit,
    onMarkDone: () -> Unit
) {
    val color = toneColor(StatusTone.WARNING)
    val reasons = deliveries?.let(::groupFailureReasons).orEmpty()
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
        modifier = Modifier.fillMaxWidth().testTag("failure_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                if (failedCount == 1) "Why 1 person didn't get it" else "Why $failedCount people didn't get it",
                style = MaterialTheme.typography.titleMedium
            )
            reasons.take(4).forEachIndexed { index, r ->
                if (index > 0) HorizontalDivider(color = color.copy(alpha = 0.2f))
                Row(verticalAlignment = Alignment.Top) {
                    Text(r.reason, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Spacer(Modifier.size(12.dp))
                    Text(r.count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
                }
            }
            if (reasons.size > 4) {
                Text(
                    "+ ${reasons.size - 4} other reasons",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onRetry,
                enabled = canRetry,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("retry_failed_button")
            ) {
                Icon(Icons.Rounded.Replay, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(if (failedCount == 1) "Try again for 1 person" else "Try again for $failedCount people")
            }
            if (canMarkDone) {
                TextButton(onClick = onMarkDone, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Icon(Icons.Rounded.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Mark as done")
                }
            }
        }
    }
}

@Composable
private fun DeliveryRow(delivery: RecipientDeliveryEntity) {
    val number = PhoneNumbers.format(delivery.phoneNumber)
    ListItem(
        headlineContent = { Text(delivery.contactName.ifBlank { number }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column {
                if (delivery.contactName.isNotBlank()) Text(number)
                if (delivery.status == DeliveryStatus.FAILED && !delivery.errorDetail.isNullOrBlank()) {
                    Text(delivery.errorDetail, color = toneColor(StatusTone.ERROR), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        trailingContent = { StatusPill(deliveryStatusLabel(delivery.status)) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
