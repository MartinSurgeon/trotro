package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.LocalSnackbarHostState
import com.example.data.BroadcastCounts
import com.example.data.DeliveryStatus
import com.example.data.MessageBroadcastEntity
import com.example.data.RecipientDeliveryEntity
import com.example.data.SendMode
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.EmptyState
import com.example.ui.components.PrimaryActionButton
import com.example.ui.components.ScreenTitleBar
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusTone
import com.example.ui.components.broadcastStatusLabel
import com.example.ui.components.deliveryStatusLabel
import com.example.ui.components.formatWhen
import com.example.ui.components.peopleCount
import com.example.ui.components.toneColor
import com.example.util.PhoneNumbers
import com.example.viewmodel.CampaignUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

private fun sendModeLabel(mode: String): String = when (mode) {
    SendMode.SMS_APP -> "Messages app"
    SendMode.DIRECT_SIM -> "My SIM"
    else -> "SMS Online GH"
}

@Composable
fun HistoryScreen(
    uiState: CampaignUiState,
    observeDeliveries: (Long) -> Flow<List<RecipientDeliveryEntity>>,
    onOpenBroadcast: (Long?) -> Unit,
    onReuseMessage: (String) -> Unit,
    onRetryFailed: (Long) -> Unit,
    onDeleteBroadcast: (Long) -> Unit,
    onNewMessage: () -> Unit,
    hasSendSmsPermission: () -> Boolean
) {
    val open = uiState.openBroadcastId?.let { id -> uiState.broadcasts.firstOrNull { it.id == id } }
    if (open != null) {
        BroadcastDetail(
            broadcast = open,
            counts = uiState.broadcastCounts[open.id],
            isSending = uiState.isSending,
            observeDeliveries = observeDeliveries,
            onBack = { onOpenBroadcast(null) },
            onReuseMessage = { onReuseMessage(open.messageText) },
            onRetryFailed = { onRetryFailed(open.id) },
            onDelete = { onDeleteBroadcast(open.id) },
            hasSendSmsPermission = hasSendSmsPermission
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenTitleBar(title = "Messages")
            if (uiState.broadcasts.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.Forum,
                    title = "No messages yet",
                    message = "Messages you send will appear here, with who received them and who didn't.",
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
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().testTag("history_lazy_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.broadcasts, key = { it.id }) { broadcast ->
                        BroadcastCard(
                            broadcast = broadcast,
                            counts = uiState.broadcastCounts[broadcast.id],
                            onClick = { onOpenBroadcast(broadcast.id) }
                        )
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
private fun BroadcastCard(broadcast: MessageBroadcastEntity, counts: BroadcastCounts?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("history_item_${broadcast.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatWhen(broadcast.timestamp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                StatusPill(broadcastStatusLabel(broadcast.overallStatus))
            }
            Text(
                broadcast.messageText,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${peopleCount(broadcast.totalRecipients)} · ${sendModeLabel(broadcast.sendMode)}" +
                    if (broadcast.senderIdUsed.isNotBlank()) " · ${broadcast.senderIdUsed}" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (counts != null && broadcast.sendMode != SendMode.SMS_APP) {
                DeliveryProgress(counts)
            }
        }
    }
}

@Composable
private fun DeliveryProgress(counts: BroadcastCounts) {
    if (counts.total == 0) return
    val reached = counts.delivered + counts.sent
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LinearProgressIndicator(
            progress = { reached.toFloat() / counts.total },
            color = toneColor(if (counts.failed > 0) StatusTone.WARNING else StatusTone.SUCCESS),
            modifier = Modifier.fillMaxWidth()
        )
        val parts = buildList {
            if (counts.delivered > 0) add("${counts.delivered} delivered")
            if (counts.sent > 0) add("${counts.sent} sent")
            if (counts.pending > 0) add("${counts.pending} sending")
            if (counts.failed > 0) add("${counts.failed} not sent")
        }
        Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private enum class RecipientFilter(val label: String) { ALL("All"), FAILED("Not sent"), SENT("Sent"), DELIVERED("Delivered") }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BroadcastDetail(
    broadcast: MessageBroadcastEntity,
    counts: BroadcastCounts?,
    isSending: Boolean,
    observeDeliveries: (Long) -> Flow<List<RecipientDeliveryEntity>>,
    onBack: () -> Unit,
    onReuseMessage: () -> Unit,
    onRetryFailed: () -> Unit,
    onDelete: () -> Unit,
    hasSendSmsPermission: () -> Boolean
) {
    val deliveriesFlow = remember(broadcast.id) { observeDeliveries(broadcast.id) }
    val deliveries by deliveriesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var filter by rememberSaveable { mutableStateOf(RecipientFilter.ALL) }
    var confirmDelete by remember { mutableStateOf(false) }
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onRetryFailed() else scope.launch { snackbar.showSnackbar("Allow SMS permission to retry from your SIM") }
    }

    val failedCount = counts?.failed ?: deliveries.count { it.status == DeliveryStatus.FAILED }
    val visible = deliveries.filter {
        when (filter) {
            RecipientFilter.ALL -> true
            RecipientFilter.FAILED -> it.status == DeliveryStatus.FAILED
            RecipientFilter.SENT -> it.status == DeliveryStatus.SENT || it.status == DeliveryStatus.SUBMITTED
            RecipientFilter.DELIVERED -> it.status == DeliveryStatus.DELIVERED
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomEnd = 18.dp, bottomStart = 18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(broadcast.messageText, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(14.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${peopleCount(broadcast.totalRecipients)} · ${sendModeLabel(broadcast.sendMode)}" +
                                if (broadcast.senderIdUsed.isNotBlank()) " · ${broadcast.senderIdUsed}" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        StatusPill(broadcastStatusLabel(broadcast.overallStatus))
                    }
                    Text(broadcast.statusDetail, style = MaterialTheme.typography.bodyMedium)

                    if (counts != null && broadcast.sendMode != SendMode.SMS_APP) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CountTile("Delivered", counts.delivered, StatusTone.SUCCESS, Modifier.weight(1f))
                            CountTile("Sent", counts.sent, StatusTone.SUCCESS, Modifier.weight(1f))
                            CountTile("Not sent", counts.failed, StatusTone.ERROR, Modifier.weight(1f))
                            CountTile("Sending", counts.pending, StatusTone.PROGRESS, Modifier.weight(1f))
                        }
                        if (broadcast.sendMode == SendMode.DIRECT_SIM && counts.sent > 0 && counts.delivered == 0) {
                            Text(
                                "\"Sent\" means it left your phone. Some networks don't send delivery reports.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (failedCount > 0 && broadcast.sendMode != SendMode.SMS_APP) {
                        Button(
                            onClick = {
                                if (broadcast.sendMode == SendMode.DIRECT_SIM && !hasSendSmsPermission()) {
                                    permissionLauncher.launch(Manifest.permission.SEND_SMS)
                                } else {
                                    onRetryFailed()
                                }
                            },
                            enabled = !isSending,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("retry_failed_button")
                        ) {
                            Icon(Icons.Rounded.Replay, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Retry the $failedCount not sent")
                        }
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

                    if (deliveries.size > 1 && broadcast.sendMode != SendMode.SMS_APP) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RecipientFilter.entries.forEach { f ->
                                FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) })
                            }
                        }
                    }
                }
            }
            items(visible, key = { it.id }) { delivery ->
                DeliveryRow(delivery)
            }
            if (visible.isEmpty() && deliveries.isNotEmpty()) {
                item {
                    Text(
                        "No one in this list.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
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
private fun CountTile(label: String, value: Int, tone: StatusTone, modifier: Modifier = Modifier) {
    val color = toneColor(tone)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (value > 0) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                value.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = if (value > 0) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DeliveryRow(delivery: RecipientDeliveryEntity) {
    val number = PhoneNumbers.format(delivery.phoneNumber)
    val label = deliveryStatusLabel(delivery.status)
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
        trailingContent = { StatusPill(label) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
