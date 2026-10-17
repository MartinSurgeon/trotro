package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignDao {
    // --- Contacts ---
    @Query("SELECT * FROM contacts ORDER BY name ASC, id DESC")
    fun observeAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts ORDER BY name ASC, id DESC")
    suspend fun getAllContactsSnapshot(): List<ContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContactById(id: Long)

    @Query("DELETE FROM contacts")
    suspend fun deleteAllContacts()

    @Query("UPDATE contacts SET isSelected = :selected")
    suspend fun setAllContactsSelected(selected: Boolean)

    @Query("UPDATE contacts SET isSelected = :selected WHERE id = :id")
    suspend fun setContactSelected(id: Long, selected: Boolean)

    @Query("UPDATE contacts SET isSelected = CASE WHEN LOWER(groupTag) = LOWER(:groupTag) THEN 1 ELSE 0 END")
    suspend fun selectContactsOnlyInGroup(groupTag: String)

    @Query("UPDATE contacts SET isSelected = :selected WHERE LOWER(TRIM(groupTag)) = LOWER(TRIM(:groupTag))")
    suspend fun setGroupSelected(groupTag: String, selected: Boolean)

    @Query("UPDATE contacts SET groupTag = :newName WHERE LOWER(TRIM(groupTag)) = LOWER(TRIM(:oldName))")
    suspend fun renameGroup(oldName: String, newName: String)

    // --- Sender IDs (Multiple Candidates / Organisers) ---
    @Query("SELECT * FROM sender_ids ORDER BY isDefault DESC, senderName ASC")
    fun observeAllSenderIds(): Flow<List<SenderIdEntity>>

    @Query("SELECT * FROM sender_ids ORDER BY isDefault DESC, senderName ASC")
    suspend fun getAllSenderIdsSnapshot(): List<SenderIdEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSenderId(senderId: SenderIdEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSenderIds(senderIds: List<SenderIdEntity>)

    @Query("DELETE FROM sender_ids WHERE id = :id")
    suspend fun deleteSenderIdById(id: Long)

    @Query("UPDATE sender_ids SET isDefault = CASE WHEN id = :selectedId THEN 1 ELSE 0 END")
    suspend fun setDefaultSenderIdById(selectedId: Long)

    // --- Organisation Profile ---
    @Query("SELECT * FROM organisation_profile WHERE id = 1 LIMIT 1")
    fun observeOrganisationProfile(): Flow<OrganisationProfileEntity?>

    @Query("SELECT * FROM organisation_profile WHERE id = 1 LIMIT 1")
    suspend fun getOrganisationProfileSnapshot(): OrganisationProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOrganisationProfile(profile: OrganisationProfileEntity)

    // --- Message Broadcasts & Recipient Deliveries ---
    @Query("SELECT * FROM message_broadcasts ORDER BY timestamp DESC")
    fun observeAllBroadcasts(): Flow<List<MessageBroadcastEntity>>

    @Query("SELECT * FROM message_broadcasts WHERE id = :broadcastId LIMIT 1")
    suspend fun getBroadcastById(broadcastId: Long): MessageBroadcastEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBroadcast(broadcast: MessageBroadcastEntity): Long

    @Update
    suspend fun updateBroadcast(broadcast: MessageBroadcastEntity)

    @Query("DELETE FROM message_broadcasts WHERE id = :broadcastId")
    suspend fun deleteBroadcastById(broadcastId: Long)

    @Query("DELETE FROM recipient_deliveries WHERE broadcastId = :broadcastId")
    suspend fun deleteDeliveriesForBroadcast(broadcastId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipientDelivery(delivery: RecipientDeliveryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipientDeliveries(deliveries: List<RecipientDeliveryEntity>): List<Long>

    @Query(
        """
        SELECT broadcastId,
            COUNT(*) AS total,
            SUM(CASE WHEN status = 'DELIVERED' THEN 1 ELSE 0 END) AS delivered,
            SUM(CASE WHEN status IN ('SENT', 'SUBMITTED') THEN 1 ELSE 0 END) AS sent,
            SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failed,
            SUM(CASE WHEN status = 'SENDING' THEN 1 ELSE 0 END) AS pending,
            SUM(CASE WHEN status = 'OPENED_IN_APP' THEN 1 ELSE 0 END) AS openedInApp
        FROM recipient_deliveries
        GROUP BY broadcastId
        """
    )
    fun observeBroadcastCounts(): Flow<List<BroadcastCounts>>

    @Query("SELECT DISTINCT broadcastId FROM recipient_deliveries WHERE status = 'SENDING' AND updatedAt < :before")
    suspend fun getBroadcastIdsWithStaleSending(before: Long): List<Long>

    @Query(
        "UPDATE recipient_deliveries SET status = 'FAILED', errorDetail = :reason, updatedAt = :now " +
            "WHERE status = 'SENDING' AND updatedAt < :before"
    )
    suspend fun failStaleSending(before: Long, reason: String, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM recipient_deliveries WHERE broadcastId = :broadcastId ORDER BY contactName ASC")
    fun observeDeliveriesForBroadcast(broadcastId: Long): Flow<List<RecipientDeliveryEntity>>

    @Query("SELECT * FROM recipient_deliveries WHERE broadcastId = :broadcastId")
    suspend fun getDeliveriesForBroadcastSnapshot(broadcastId: Long): List<RecipientDeliveryEntity>

    @Query("SELECT * FROM recipient_deliveries WHERE id = :deliveryId LIMIT 1")
    suspend fun getDeliveryById(deliveryId: Long): RecipientDeliveryEntity?

    @Query("UPDATE recipient_deliveries SET status = :status, errorDetail = :errorDetail, updatedAt = :updatedAt WHERE id = :deliveryId")
    suspend fun updateRecipientDeliveryStatus(
        deliveryId: Long,
        status: String,
        errorDetail: String?,
        updatedAt: Long = System.currentTimeMillis()
    )
}
