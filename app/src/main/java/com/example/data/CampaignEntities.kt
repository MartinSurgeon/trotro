package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Default values below are Kotlin-only (not part of the Room schema), so changing them
// never requires a database migration.

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                // May be blank when only a number is known
    val phoneNumber: String,
    val groupTag: String = "",       // Blank = no group
    val isSelected: Boolean = true,  // Whether this contact is a recipient of the next message
    val createdAt: Long = System.currentTimeMillis()
) {
    val hasName: Boolean get() = name.isNotBlank()
}

@Entity(tableName = "sender_ids")
data class SenderIdEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderName: String,          // Up to 11 chars, must be approved on SMSOnlineGH
    val candidateOrOrgLabel: String, // Optional note shown under the sender name
    val defaultDelegateGroup: String = "All", // Legacy, no longer used by the UI
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "organisation_profile")
data class OrganisationProfileEntity(
    @PrimaryKey val id: Int = 1,
    val organisationName: String = "",
    val roleOrSubtitle: String = "",
    val slogan: String = "",
    val activeSenderId: String = "",
    val customPhotoPath: String? = null,
    val primaryColorHex: String = "#006B3F", // Ghana green
    val accentColorHex: String = "#FCD116",  // Ghana gold
    val draftMessage: String = "",
    val preferredSendMode: String = SendMode.DIRECT_SIM
)

@Entity(tableName = "message_broadcasts")
data class MessageBroadcastEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderIdUsed: String = "",
    val candidateLabel: String = "",
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val totalRecipients: Int,
    val sendMode: String, // See SendMode
    val overallStatus: String, // See BroadcastStatus
    val statusDetail: String
)

@Entity(tableName = "recipient_deliveries")
data class RecipientDeliveryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val broadcastId: Long,
    val contactName: String,
    val phoneNumber: String,
    val status: String, // See DeliveryStatus
    val errorDetail: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

/** Per-message delivery tally, computed by Room from recipient_deliveries. */
data class BroadcastCounts(
    val broadcastId: Long,
    val total: Int,
    val delivered: Int,
    val sent: Int,
    val failed: Int,
    val pending: Int,
    val openedInApp: Int
)

object SendMode {
    const val SMSONLINEGH = "SMSONLINEGH"
    const val DIRECT_SIM = "DIRECT_SIM"
    const val SMS_APP = "SMS_APP"
}

object BroadcastStatus {
    const val SENDING = "SENDING"
    const val SUBMITTED_TO_GATEWAY = "SUBMITTED_TO_GATEWAY"
    const val SENT_TO_CARRIER = "SENT_TO_CARRIER"
    const val DELIVERED = "DELIVERED"
    const val PARTIAL = "PARTIAL"
    const val FAILED = "FAILED"
    const val OPENED_IN_SMS_APP = "OPENED_IN_SMS_APP"
}

object DeliveryStatus {
    const val SENDING = "SENDING"
    const val SUBMITTED = "SUBMITTED"
    const val SENT = "SENT"
    const val DELIVERED = "DELIVERED"
    const val FAILED = "FAILED"
    const val OPENED_IN_APP = "OPENED_IN_APP"
}
