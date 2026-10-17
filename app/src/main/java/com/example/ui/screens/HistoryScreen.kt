package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MessageBroadcastEntity
import com.example.data.RecipientDeliveryEntity
import com.example.viewmodel.CampaignUiState
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    uiState: CampaignUiState,
    observeDeliveries: (Long) -> Flow<List<RecipientDeliveryEntity>>,
    onBackToHome: () -> Unit,
    onReuseMessage: (String) -> Unit,
    onDeleteBroadcast: (Long) -> Unit,
    onGoToSendSms: () -> Unit
) {
    BackHandler { onBackToHome() }

    val primaryColor = MaterialTheme.colorScheme.primary
    val accentColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (uiState.broadcasts.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = primaryColor.copy(alpha = 0.12f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No Messages Sent Yet",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "When candidates send SMS messages to delegates, each broadcast will appear here with the Sender ID, date, recipient count, and real SMSOnlineGH / carrier status.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onGoToSendSms,
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("history_empty_send_sms_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Write & Send Your First SMS", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("history_lazy_list"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(uiState.broadcasts, key = { it.id }) { broadcast ->
                        BroadcastHistoryCard(
                            broadcast = broadcast,
                            observeDeliveries = observeDeliveries,
                            primaryColor = primaryColor,
                            accentColor = accentColor,
                            onReuseMessage = { onReuseMessage(broadcast.messageText) },
                            onDelete = { onDeleteBroadcast(broadcast.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BroadcastHistoryCard(
    broadcast: MessageBroadcastEntity,
    observeDeliveries: (Long) -> Flow<List<RecipientDeliveryEntity>>,
    primaryColor: Color,
    accentColor: Color,
    onReuseMessage: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()) }
    val formattedDate = remember(broadcast.timestamp) {
        dateFormat.format(Date(broadcast.timestamp))
    }

    val (badgeText, badgeColor, badgeIcon) = when (broadcast.overallStatus) {
        "DELIVERED" -> Triple("Delivered", Color(0xFF1B5E20), Icons.Default.CheckCircle)
        "SUBMITTED_TO_GATEWAY" -> Triple("SMSOnlineGH Accepted", primaryColor, Icons.Default.CloudDone)
        "SENT_TO_CARRIER" -> Triple("Sent to Carrier", primaryColor, Icons.Default.CheckCircle)
        "OPENED_IN_SMS_APP" -> Triple("Opened in SMS App", Color(0xFF0D47A1), Icons.Default.OpenInNew)
        "PARTIAL" -> Triple("Partially Sent", Color(0xFFE65100), Icons.Default.Schedule)
        "FAILED" -> Triple("Send Failed", accentColor, Icons.Default.ErrorOutline)
        else -> Triple("Sending...", Color(0xFF6A1B9A), Icons.Default.Schedule)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_${broadcast.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Date + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelMedium,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Candidate Sender ID Badge Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Sender ID: ${broadcast.senderIdUsed}",
                            style = MaterialTheme.typography.labelMedium,
                            color = accentColor,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                if (broadcast.candidateLabel.isNotBlank()) {
                    Text(
                        text = broadcast.candidateLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Message Text Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = primaryColor.copy(alpha = 0.06f),
                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.18f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = broadcast.messageText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }

            // Recipients Count & Detailed Status Explanation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Delegates / Recipients: ${broadcast.totalRecipients}",
                    style = MaterialTheme.typography.titleMedium,
                    color = primaryColor,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = when (broadcast.sendMode) {
                        "SMS_APP" -> "Via Phone SMS App"
                        "DIRECT_SIM" -> "Via Direct SIM"
                        else -> "Via SMSOnlineGH"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = broadcast.statusDetail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { expanded = !expanded }
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (expanded) "Hide Recipients" else "Recipient Status")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onReuseMessage,
                        modifier = Modifier.testTag("reuse_message_button_${broadcast.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send Again")
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete history entry",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (expanded) {
                val deliveries by observeDeliveries(broadcast.id)
                    .collectAsStateWithLifecycle(initialValue = emptyList())

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        deliveries.forEach { d ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${d.contactName} (${d.phoneNumber})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (!d.errorDetail.isNullOrBlank()) {
                                        Text(
                                            text = d.errorDetail,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (d.status == "FAILED") accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                val statusLabel = when (d.status) {
                                    "DELIVERED" -> "Delivered"
                                    "SUBMITTED" -> "Accepted (HSHK_OK)"
                                    "SENT" -> "Sent to Carrier"
                                    "OPENED_IN_APP" -> "In SMS App"
                                    "FAILED" -> "Failed"
                                    else -> "Sending..."
                                }
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when (d.status) {
                                        "DELIVERED", "SUBMITTED", "SENT" -> primaryColor
                                        "FAILED" -> accentColor
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
