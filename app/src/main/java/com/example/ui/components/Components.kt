package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.BroadcastStatus
import com.example.data.DeliveryStatus
import com.example.ui.theme.DawuroTheme
import com.example.ui.theme.GhanaGold
import com.example.ui.theme.GhanaGreen
import com.example.ui.theme.GhanaRed
import com.example.ui.theme.KenteBlack
import java.io.File

private val avatarPalette = listOf(
    Color(0xFF00897B), Color(0xFF5E35B1), Color(0xFFD81B60), Color(0xFF3949AB),
    Color(0xFFF4511E), Color(0xFF6D4C41), Color(0xFF43A047), Color(0xFF8E24AA)
)

fun initialsOf(name: String): String =
    name.split(" ", "-")
        .filter { it.isNotBlank() && it.first().isLetter() }
        .take(2)
        .joinToString("") { it.first().uppercase() }

/** Round picture, or coloured initials when there is no photo. */
@Composable
fun Avatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    photoPath: String? = null,
    background: Color? = null
) {
    val photo = remember(photoPath) { photoPath?.let(::File)?.takeIf { it.exists() } }
    if (photo != null) {
        AsyncImage(
            model = photo,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape)
        )
        return
    }
    val color = background ?: avatarPalette[(name.hashCode() and 0x7fffffff) % avatarPalette.size]
    val initials = initialsOf(name)
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center
    ) {
        if (initials.isNotEmpty()) {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.38f).sp
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Sms,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}

/** A thin kente-inspired band in Ghana's colours, used as a quiet brand accent. */
@Composable
fun KenteStripe(modifier: Modifier = Modifier, height: Dp = 5.dp) {
    val colours = listOf(GhanaGreen, GhanaGold, GhanaRed, KenteBlack, GhanaGold, GhanaGreen, GhanaRed, GhanaGold)
    Row(modifier = modifier.fillMaxWidth().height(height)) {
        colours.forEach { c ->
            Box(modifier = Modifier.weight(1f).height(height).background(c))
        }
    }
}

enum class StatusTone { SUCCESS, PROGRESS, WARNING, ERROR, NEUTRAL }

data class StatusLabel(val text: String, val tone: StatusTone)

fun broadcastStatusLabel(status: String): StatusLabel = when (status) {
    BroadcastStatus.DELIVERED -> StatusLabel("Delivered", StatusTone.SUCCESS)
    BroadcastStatus.SUBMITTED_TO_GATEWAY, BroadcastStatus.SENT_TO_CARRIER -> StatusLabel("Sent", StatusTone.SUCCESS)
    BroadcastStatus.OPENED_IN_SMS_APP -> StatusLabel("In Messages app", StatusTone.NEUTRAL)
    BroadcastStatus.PARTIAL -> StatusLabel("Some not sent", StatusTone.WARNING)
    BroadcastStatus.FAILED -> StatusLabel("Not sent", StatusTone.ERROR)
    else -> StatusLabel("Sending…", StatusTone.PROGRESS)
}

fun deliveryStatusLabel(status: String): StatusLabel = when (status) {
    DeliveryStatus.DELIVERED -> StatusLabel("Delivered", StatusTone.SUCCESS)
    DeliveryStatus.SUBMITTED, DeliveryStatus.SENT -> StatusLabel("Sent", StatusTone.SUCCESS)
    DeliveryStatus.OPENED_IN_APP -> StatusLabel("In Messages app", StatusTone.NEUTRAL)
    DeliveryStatus.FAILED -> StatusLabel("Not sent", StatusTone.ERROR)
    else -> StatusLabel("Sending…", StatusTone.PROGRESS)
}

@Composable
fun toneColor(tone: StatusTone): Color {
    val colors = DawuroTheme.statusColors
    return when (tone) {
        StatusTone.SUCCESS -> colors.success
        StatusTone.PROGRESS -> colors.progress
        StatusTone.WARNING -> colors.warning
        StatusTone.ERROR -> colors.error
        StatusTone.NEUTRAL -> colors.neutral
    }
}

/** Status shown with colour, icon and words, so it never relies on colour alone. */
@Composable
fun StatusPill(label: StatusLabel, modifier: Modifier = Modifier) {
    val color = toneColor(label.tone)
    val icon = when (label.tone) {
        StatusTone.SUCCESS -> Icons.Rounded.CheckCircle
        StatusTone.ERROR -> Icons.Rounded.ErrorOutline
        StatusTone.WARNING -> Icons.Rounded.WarningAmber
        else -> Icons.Rounded.Schedule
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.13f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Text(label.text, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(88.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 360.dp)
        )
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

/** Large, full-width main action button. */
@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(10.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    dismissLabel: String = "Cancel"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = if (destructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel) } }
    )
}
