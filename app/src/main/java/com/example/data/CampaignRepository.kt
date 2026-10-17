package com.example.data

import android.content.Context
import android.net.Uri
import com.example.util.PhoneNumbers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class AddContactResult { ADDED, DUPLICATE, INVALID_NUMBER }

class CampaignRepository(
    private val dao: CampaignDao,
    private val appContext: Context
) {
    val allContacts: Flow<List<ContactEntity>> = dao.observeAllContacts()
    val allSenderIds: Flow<List<SenderIdEntity>> = dao.observeAllSenderIds()
    val organisationProfile: Flow<OrganisationProfileEntity?> = dao.observeOrganisationProfile()
    val allBroadcasts: Flow<List<MessageBroadcastEntity>> = dao.observeAllBroadcasts()
    val broadcastCounts: Flow<List<BroadcastCounts>> = dao.observeBroadcastCounts()

    // Serialises read-modify-write of the single profile row so concurrent edits
    // (typing a draft while changing send mode, say) never overwrite each other.
    private val profileMutex = Mutex()
    private val deliveryMutex = Mutex()

    fun observeDeliveriesForBroadcast(broadcastId: Long): Flow<List<RecipientDeliveryEntity>> {
        return dao.observeDeliveriesForBroadcast(broadcastId)
    }

    suspend fun getDeliveriesSnapshot(broadcastId: Long): List<RecipientDeliveryEntity> =
        dao.getDeliveriesForBroadcastSnapshot(broadcastId)

    suspend fun getBroadcast(broadcastId: Long): MessageBroadcastEntity? = dao.getBroadcastById(broadcastId)

    suspend fun ensureDefaultProfile(): OrganisationProfileEntity {
        recoverInterruptedSends()
        val existing = dao.getOrganisationProfileSnapshot()
        if (existing != null) return existing
        val defaultProfile = OrganisationProfileEntity()
        dao.saveOrganisationProfile(defaultProfile)
        return defaultProfile
    }

    /**
     * Messages still marked "sending" long after they started were interrupted (app killed,
     * phone restarted). Mark them failed so users can retry instead of waiting forever.
     */
    private suspend fun recoverInterruptedSends() {
        val cutoff = System.currentTimeMillis() - STALE_SENDING_MS
        val affected = dao.getBroadcastIdsWithStaleSending(cutoff)
        if (affected.isEmpty()) return
        dao.failStaleSending(cutoff, "Sending was interrupted. Tap \"Retry\" to send again.")
        affected.forEach { refreshBroadcastAggregateStatus(it) }
    }

    suspend fun updateProfile(transform: (OrganisationProfileEntity) -> OrganisationProfileEntity) {
        profileMutex.withLock {
            val current = dao.getOrganisationProfileSnapshot() ?: OrganisationProfileEntity()
            val updated = transform(current)
            if (updated != current) dao.saveOrganisationProfile(updated)
        }
    }

    suspend fun addSenderId(senderName: String, note: String): Long {
        val cleanSender = senderName.trim().take(11)
        val id = dao.insertSenderId(
            SenderIdEntity(
                senderName = cleanSender,
                candidateOrOrgLabel = note.trim(),
                isDefault = true
            )
        )
        dao.setDefaultSenderIdById(id)
        updateProfile { it.copy(activeSenderId = cleanSender) }
        return id
    }

    suspend fun selectActiveSenderId(senderIdEntity: SenderIdEntity) {
        dao.setDefaultSenderIdById(senderIdEntity.id)
        updateProfile { it.copy(activeSenderId = senderIdEntity.senderName) }
    }

    suspend fun deleteSenderId(senderIdEntity: SenderIdEntity) {
        dao.deleteSenderIdById(senderIdEntity.id)
        val remaining = dao.getAllSenderIdsSnapshot()
        val wasActive = remaining.none { it.isDefault }
        if (wasActive) {
            val next = remaining.firstOrNull()
            if (next != null) dao.setDefaultSenderIdById(next.id)
            updateProfile { it.copy(activeSenderId = next?.senderName.orEmpty()) }
        }
    }

    /** Copies the picked image into app storage and removes the previous one. */
    suspend fun replaceProfilePhoto(sourceUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val newPath = try {
            val destFile = File(appContext.filesDir, "profile_photo_${System.currentTimeMillis()}.jpg")
            val input = appContext.contentResolver.openInputStream(sourceUri) ?: return@withContext false
            input.use { stream -> FileOutputStream(destFile).use { stream.copyTo(it) } }
            destFile.absolutePath
        } catch (_: Exception) {
            return@withContext false
        }
        var oldPath: String? = null
        updateProfile {
            oldPath = it.customPhotoPath
            it.copy(customPhotoPath = newPath)
        }
        oldPath?.takeIf { it != newPath }?.let { runCatching { File(it).delete() } }
        true
    }

    suspend fun removeProfilePhoto() {
        var oldPath: String? = null
        updateProfile {
            oldPath = it.customPhotoPath
            it.copy(customPhotoPath = null)
        }
        withContext(Dispatchers.IO) { oldPath?.let { runCatching { File(it).delete() } } }
    }

    suspend fun addContact(name: String, phoneNumber: String, groupTag: String): AddContactResult {
        val normalized = PhoneNumbers.normalize(phoneNumber) ?: return AddContactResult.INVALID_NUMBER
        val key = PhoneNumbers.dedupeKey(normalized)
        if (dao.getAllContactsSnapshot().any { PhoneNumbers.dedupeKey(it.phoneNumber) == key }) {
            return AddContactResult.DUPLICATE
        }
        dao.insertContact(
            ContactEntity(
                name = name.trim(),
                phoneNumber = normalized,
                groupTag = groupTag.trim(),
                isSelected = true
            )
        )
        return AddContactResult.ADDED
    }

    /** How many of [candidates] are already saved (by phone number). */
    suspend fun countExisting(candidates: List<ContactEntity>): Int {
        val existing = dao.getAllContactsSnapshot().map { PhoneNumbers.dedupeKey(it.phoneNumber) }.toSet()
        return candidates.count { PhoneNumbers.dedupeKey(it.phoneNumber) in existing }
    }

    suspend fun importContacts(newContacts: List<ContactEntity>): Int {
        val existingKeys = dao.getAllContactsSnapshot()
            .map { PhoneNumbers.dedupeKey(it.phoneNumber) }
            .toMutableSet()

        val uniqueToInsert = newContacts.filter { contact ->
            existingKeys.add(PhoneNumbers.dedupeKey(contact.phoneNumber))
        }
        if (uniqueToInsert.isNotEmpty()) {
            dao.insertContacts(uniqueToInsert)
        }
        return uniqueToInsert.size
    }

    suspend fun updateContact(contact: ContactEntity) {
        dao.updateContact(contact)
    }

    suspend fun restoreContact(contact: ContactEntity) {
        dao.insertContact(contact)
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

    suspend fun setGroupSelected(groupTag: String, selected: Boolean) {
        dao.setGroupSelected(groupTag, selected)
    }

    suspend fun createBroadcastWithRecipients(
        senderIdUsed: String,
        candidateLabel: String,
        messageText: String,
        recipients: List<ContactEntity>,
        sendMode: String,
        initialOverallStatus: String,
        initialStatusDetail: String,
        initialRecipientStatus: String,
        initialRecipientDetail: String? = null
    ): Pair<Long, List<RecipientDeliveryEntity>> {
        val broadcastId = dao.insertBroadcast(
            MessageBroadcastEntity(
                senderIdUsed = senderIdUsed,
                candidateLabel = candidateLabel,
                messageText = messageText,
                timestamp = System.currentTimeMillis(),
                totalRecipients = recipients.size,
                sendMode = sendMode,
                overallStatus = initialOverallStatus,
                statusDetail = initialStatusDetail
            )
        )
        val deliveries = recipients.map { contact ->
            RecipientDeliveryEntity(
                broadcastId = broadcastId,
                contactName = contact.name,
                phoneNumber = contact.phoneNumber,
                status = initialRecipientStatus,
                errorDetail = initialRecipientDetail
            )
        }
        val ids = dao.insertRecipientDeliveries(deliveries)
        return broadcastId to deliveries.zip(ids) { delivery, id -> delivery.copy(id = id) }
    }

    suspend fun updateRecipientStatusAndRefreshBroadcast(
        deliveryId: Long,
        newStatus: String,
        errorDetail: String? = null
    ) {
        val broadcastId = deliveryMutex.withLock {
            val current = dao.getDeliveryById(deliveryId) ?: return
            // A long SMS is sent in parts and each part reports back. Never let a late
            // "sent" report hide a delivery confirmation or a failure of another part.
            val isDowngrade = newStatus == DeliveryStatus.SENT &&
                (current.status == DeliveryStatus.DELIVERED || current.status == DeliveryStatus.FAILED)
            if (isDowngrade) return
            dao.updateRecipientDeliveryStatus(deliveryId, newStatus, errorDetail)
            current.broadcastId
        }
        refreshBroadcastAggregateStatus(broadcastId)
    }

    suspend fun refreshBroadcastAggregateStatus(broadcastId: Long) {
        val broadcast = dao.getBroadcastById(broadcastId) ?: return
        val deliveries = dao.getDeliveriesForBroadcastSnapshot(broadcastId)
        if (deliveries.isEmpty()) return

        val total = deliveries.size
        val delivered = deliveries.count { it.status == DeliveryStatus.DELIVERED }
        val submitted = deliveries.count { it.status == DeliveryStatus.SUBMITTED }
        val sent = deliveries.count { it.status == DeliveryStatus.SENT } + submitted + delivered
        val failed = deliveries.count { it.status == DeliveryStatus.FAILED }
        val sending = deliveries.count { it.status == DeliveryStatus.SENDING }
        val openedInApp = deliveries.count { it.status == DeliveryStatus.OPENED_IN_APP }
        val people = if (total == 1) "1 person" else "$total people"

        val (overallStatus, detail) = when {
            openedInApp == total ->
                BroadcastStatus.OPENED_IN_SMS_APP to "Opened in your Messages app for $people. Press send there to finish."
            sending > 0 ->
                BroadcastStatus.SENDING to "Sending… ${total - sending} of $total done."
            failed == total -> {
                val reason = deliveries.firstNotNullOfOrNull { it.errorDetail?.takeIf(String::isNotBlank) }
                BroadcastStatus.FAILED to (reason ?: "The message could not be sent.")
            }
            failed > 0 ->
                BroadcastStatus.PARTIAL to "$failed of $total could not be sent. You can retry them."
            delivered == total ->
                BroadcastStatus.DELIVERED to "Delivered to all $people."
            submitted == total ->
                BroadcastStatus.SUBMITTED_TO_GATEWAY to "Accepted by SMS Online GH for $people. The networks are delivering it."
            sent == total ->
                BroadcastStatus.SENT_TO_CARRIER to if (delivered > 0) {
                    "Sent to $people. $delivered confirmed delivered so far."
                } else {
                    "Sent to $people. Waiting for delivery reports."
                }
            else -> BroadcastStatus.SENDING to "Processing $people…"
        }

        dao.updateBroadcast(broadcast.copy(overallStatus = overallStatus, statusDetail = detail))
    }

    suspend fun deleteBroadcast(broadcastId: Long) {
        dao.deleteDeliveriesForBroadcast(broadcastId)
        dao.deleteBroadcastById(broadcastId)
    }

    private companion object {
        const val STALE_SENDING_MS = 15 * 60 * 1000L
    }
}
