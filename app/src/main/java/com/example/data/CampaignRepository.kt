package com.example.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class CampaignRepository(
    private val dao: CampaignDao,
    private val appContext: Context
) {
    val allContacts: Flow<List<ContactEntity>> = dao.observeAllContacts()
    val allSenderIds: Flow<List<SenderIdEntity>> = dao.observeAllSenderIds()
    val organisationProfile: Flow<OrganisationProfileEntity?> = dao.observeOrganisationProfile()
    val allBroadcasts: Flow<List<MessageBroadcastEntity>> = dao.observeAllBroadcasts()

    fun observeDeliveriesForBroadcast(broadcastId: Long): Flow<List<RecipientDeliveryEntity>> {
        return dao.observeDeliveriesForBroadcast(broadcastId)
    }

    suspend fun ensureDefaultProfileAndSenderIds(): OrganisationProfileEntity {
        val existingSenders = dao.getAllSenderIdsSnapshot()
        if (existingSenders.isEmpty()) {
            dao.insertSenderIds(
                listOf(
                    SenderIdEntity(
                        senderName = "JOYCE AFI",
                        candidateOrOrgLabel = "Joyce Afi Fianu (Women Organizer)",
                        defaultDelegateGroup = "Women's Wing",
                        isDefault = true
                    ),
                    SenderIdEntity(
                        senderName = "YOUTH WING",
                        candidateOrOrgLabel = "Youth Organizer Campaign",
                        defaultDelegateGroup = "Youth Delegates",
                        isDefault = false
                    ),
                    SenderIdEntity(
                        senderName = "GEN SEC",
                        candidateOrOrgLabel = "General Secretary Campaign",
                        defaultDelegateGroup = "Branch Executives",
                        isDefault = false
                    ),
                    SenderIdEntity(
                        senderName = "CAMPAIGN HQ",
                        candidateOrOrgLabel = "All Candidates / General Delegates",
                        defaultDelegateGroup = "All",
                        isDefault = false
                    )
                )
            )
        }

        val existing = dao.getOrganisationProfileSnapshot()
        if (existing != null) return existing
        val defaultProfile = OrganisationProfileEntity()
        dao.saveOrganisationProfile(defaultProfile)
        return defaultProfile
    }

    suspend fun saveOrganisationProfile(profile: OrganisationProfileEntity) {
        dao.saveOrganisationProfile(profile)
    }

    suspend fun addSenderId(
        senderName: String,
        candidateOrOrgLabel: String,
        defaultDelegateGroup: String = "All",
        makeActive: Boolean = true
    ): Long {
        val cleanSender = senderName.trim().take(11)
        val newEntity = SenderIdEntity(
            senderName = cleanSender,
            candidateOrOrgLabel = candidateOrOrgLabel.trim().ifEmpty { cleanSender },
            defaultDelegateGroup = defaultDelegateGroup.trim().ifEmpty { "All" },
            isDefault = makeActive
        )
        val id = dao.insertSenderId(newEntity)
        if (makeActive) {
            dao.setDefaultSenderIdById(id)
            val currentProfile = dao.getOrganisationProfileSnapshot() ?: OrganisationProfileEntity()
            dao.saveOrganisationProfile(currentProfile.copy(activeSenderId = cleanSender))
        }
        return id
    }

    suspend fun selectActiveSenderId(senderIdEntity: SenderIdEntity, filterGroupIfSpecific: Boolean = false) {
        dao.setDefaultSenderIdById(senderIdEntity.id)
        val currentProfile = dao.getOrganisationProfileSnapshot() ?: OrganisationProfileEntity()
        dao.saveOrganisationProfile(
            currentProfile.copy(
                activeSenderId = senderIdEntity.senderName
            )
        )
        if (filterGroupIfSpecific &&
            senderIdEntity.defaultDelegateGroup.isNotBlank() &&
            !senderIdEntity.defaultDelegateGroup.equals("All", ignoreCase = true)
        ) {
            val all = dao.getAllContactsSnapshot()
            val matchingCount = all.count {
                it.groupTag.equals(senderIdEntity.defaultDelegateGroup, ignoreCase = true)
            }
            if (matchingCount > 0) {
                dao.selectContactsOnlyInGroup(senderIdEntity.defaultDelegateGroup)
            }
        }
    }

    suspend fun deleteSenderId(senderIdEntity: SenderIdEntity) {
        dao.deleteSenderIdById(senderIdEntity.id)
        val remaining = dao.getAllSenderIdsSnapshot()
        if (remaining.isNotEmpty() && remaining.none { it.isDefault }) {
            val first = remaining.first()
            dao.setDefaultSenderIdById(first.id)
            val currentProfile = dao.getOrganisationProfileSnapshot() ?: OrganisationProfileEntity()
            dao.saveOrganisationProfile(currentProfile.copy(activeSenderId = first.senderName))
        }
    }

    suspend fun copyPhotoToInternalStorage(sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = appContext.contentResolver.openInputStream(sourceUri) ?: return@withContext null
            val destFile = File(appContext.filesDir, "org_campaign_photo_${System.currentTimeMillis()}.jpg")
            FileOutputStream(destFile).use { output ->
                inputStream.use { input ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addContact(name: String, phoneNumber: String, groupTag: String = "All Delegates"): Long {
        return dao.insertContact(
            ContactEntity(
                name = name.trim(),
                phoneNumber = phoneNumber.trim(),
                groupTag = groupTag.trim().ifEmpty { "All Delegates" },
                isSelected = true
            )
        )
    }

    suspend fun importContacts(newContacts: List<ContactEntity>): Int {
        val existingPhones = dao.getAllContactsSnapshot()
            .map { normalizePhoneKey(it.phoneNumber) }
            .toSet()

        val uniqueToInsert = newContacts.filter { contact ->
            normalizePhoneKey(contact.phoneNumber) !in existingPhones
        }
        if (uniqueToInsert.isNotEmpty()) {
            dao.insertContacts(uniqueToInsert)
        }
        return uniqueToInsert.size
    }

    private fun normalizePhoneKey(phone: String): String {
        return phone.filter { it.isDigit() }.takeLast(9)
    }

    suspend fun updateContact(contact: ContactEntity) {
        dao.updateContact(contact)
    }

    suspend fun deleteContact(id: Long) {
        dao.deleteContactById(id)
    }

    suspend fun deleteAllContacts() {
        dao.deleteAllContacts()
    }

    suspend fun toggleContactSelection(id: Long, selected: Boolean) {
        dao.setContactSelected(id, selected)
    }

    suspend fun setAllContactsSelected(selected: Boolean) {
        dao.setAllContactsSelected(selected)
    }

    suspend fun selectContactsByGroup(groupTag: String) {
        if (groupTag.equals("All", ignoreCase = true)) {
            dao.setAllContactsSelected(true)
        } else {
            dao.selectContactsOnlyInGroup(groupTag)
        }
    }

    suspend fun createBroadcastWithRecipients(
        senderIdUsed: String,
        candidateLabel: String,
        messageText: String,
        recipients: List<ContactEntity>,
        sendMode: String,
        initialOverallStatus: String,
        initialStatusDetail: String,
        initialRecipientStatus: String
    ): Pair<Long, List<RecipientDeliveryEntity>> {
        val broadcastId = dao.insertBroadcast(
            MessageBroadcastEntity(
                senderIdUsed = senderIdUsed.ifBlank { "CAMPAIGN" },
                candidateLabel = candidateLabel,
                messageText = messageText,
                timestamp = System.currentTimeMillis(),
                totalRecipients = recipients.size,
                sendMode = sendMode,
                overallStatus = initialOverallStatus,
                statusDetail = initialStatusDetail
            )
        )
        val createdDeliveries = recipients.map { contact ->
            val delivery = RecipientDeliveryEntity(
                broadcastId = broadcastId,
                contactName = contact.name,
                phoneNumber = contact.phoneNumber,
                status = initialRecipientStatus,
                errorDetail = null
            )
            val id = dao.insertRecipientDelivery(delivery)
            delivery.copy(id = id)
        }
        return broadcastId to createdDeliveries
    }

    suspend fun updateRecipientStatusAndRefreshBroadcast(
        deliveryId: Long,
        newStatus: String,
        errorDetail: String? = null
    ) {
        dao.updateRecipientDeliveryStatus(deliveryId, newStatus, errorDetail)
        val delivery = dao.getDeliveryById(deliveryId) ?: return
        refreshBroadcastAggregateStatus(delivery.broadcastId)
    }

    suspend fun refreshBroadcastAggregateStatus(broadcastId: Long) {
        val broadcast = dao.getBroadcastById(broadcastId) ?: return
        val deliveries = dao.getDeliveriesForBroadcastSnapshot(broadcastId)
        if (deliveries.isEmpty()) return

        val total = deliveries.size
        val deliveredCount = deliveries.count { it.status == "DELIVERED" }
        val submittedCount = deliveries.count { it.status == "SUBMITTED" }
        val sentCount = deliveries.count { it.status == "SENT" || it.status == "DELIVERED" || it.status == "SUBMITTED" }
        val failedCount = deliveries.count { it.status == "FAILED" }
        val sendingCount = deliveries.count { it.status == "SENDING" }
        val openedInAppCount = deliveries.count { it.status == "OPENED_IN_APP" }

        val (overallStatus, detail) = when {
            openedInAppCount == total -> {
                "OPENED_IN_SMS_APP" to "Opened in device SMS app ($total recipients). Delivery status is managed by your SMS app."
            }
            sendingCount > 0 -> {
                "SENDING" to "Sending via ${broadcast.senderIdUsed}... ($sentCount/$total processed, $sendingCount pending)"
            }
            deliveredCount == total -> {
                "DELIVERED" to "Confirmed delivered to all $total recipients (Sender: ${broadcast.senderIdUsed})"
            }
            submittedCount == total -> {
                val statusNote = deliveries.firstOrNull()?.errorDetail
                val suffix = if (!statusNote.isNullOrBlank()) " [$statusNote]" else ""
                "SUBMITTED_TO_GATEWAY" to "Accepted by SMSOnlineGH (HSHK_OK) for all $total recipients under Sender ID '${broadcast.senderIdUsed}'$suffix. Carrier delivery is in progress."
            }
            failedCount == total -> {
                val firstReason = deliveries.firstOrNull { !it.errorDetail.isNullOrBlank() }?.errorDetail
                    ?: "Gateway or SIM error"
                "FAILED" to "Failed to send ($total/$total failed: $firstReason)"
            }
            sentCount == total && deliveredCount > 0 -> {
                "SENT_TO_CARRIER" to "Sent ($total/$total); $deliveredCount confirmed delivered so far"
            }
            sentCount == total -> {
                "SENT_TO_CARRIER" to "Sent to carrier ($total/$total). Delivery confirmation pending from network."
            }
            sentCount > 0 && failedCount > 0 -> {
                "PARTIAL" to "$sentCount submitted/sent, $failedCount failed (Sender: ${broadcast.senderIdUsed})"
            }
            else -> {
                "SENDING" to "Processing $total recipients..."
            }
        }

        dao.updateBroadcast(
            broadcast.copy(
                overallStatus = overallStatus,
                statusDetail = detail
            )
        )
    }

    suspend fun deleteBroadcast(broadcastId: Long) {
        dao.deleteDeliveriesForBroadcast(broadcastId)
        dao.deleteBroadcastById(broadcastId)
    }
}
