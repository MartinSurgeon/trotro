package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val groupTag: String = "All Delegates",
    val isSelected: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sender_ids")
data class SenderIdEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderName: String,          // Up to 11 chars approved on SMSOnlineGH, e.g., "JOYCE AFI"
    val candidateOrOrgLabel: String, // e.g., "Joyce Afi Fianu - Women Organizer"
    val defaultDelegateGroup: String = "All", // Optional delegate group filter
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "organisation_profile")
data class OrganisationProfileEntity(
    @PrimaryKey val id: Int = 1,
    val organisationName: String = "JOYCE AFI FIANU",
    val roleOrSubtitle: String = "FOR WOMEN ORGANIZER",
    val slogan: String = "Together, we can work for change.",
    val activeSenderId: String = "JOYCE AFI",
    val customPhotoPath: String? = null,
    val primaryColorHex: String = "#0B4F26", // Campaign Dark Green
    val accentColorHex: String = "#D32F2F",  // Campaign Red
    val draftMessage: String = "Please vote for Joyce Afi Fianu as your Women Organizer. Together, we can work for change. Thank you.",
    val preferredSendMode: String = "SMSONLINEGH" // "SMSONLINEGH", "DIRECT_SIM", or "SMS_APP"
)

@Entity(tableName = "message_broadcasts")
data class MessageBroadcastEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderIdUsed: String = "JOYCE AFI",
    val candidateLabel: String = "JOYCE AFI FIANU",
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val totalRecipients: Int,
    val sendMode: String, // "SMSONLINEGH", "DIRECT_SIM", or "SMS_APP"
    val overallStatus: String, // "SENDING", "SUBMITTED_TO_GATEWAY", "SENT_TO_CARRIER", "DELIVERED", "PARTIAL", "FAILED", "OPENED_IN_SMS_APP"
    val statusDetail: String
)

@Entity(tableName = "recipient_deliveries")
data class RecipientDeliveryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val broadcastId: Long,
    val contactName: String,
    val phoneNumber: String,
    val status: String, // "SENDING", "SUBMITTED", "SENT", "DELIVERED", "FAILED", "OPENED_IN_APP"
    val errorDetail: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
