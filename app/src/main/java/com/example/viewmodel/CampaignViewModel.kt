package com.example.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppGraph
import com.example.data.AddContactResult
import com.example.data.BroadcastCounts
import com.example.data.CampaignRepository
import com.example.data.ContactEntity
import com.example.data.MessageBroadcastEntity
import com.example.data.OrganisationProfileEntity
import com.example.data.RecipientDeliveryEntity
import com.example.data.SendMode
import com.example.data.SenderIdEntity
import com.example.data.SettingsStore
import com.example.sms.SmsSenderService
import com.example.util.CsvContactParser
import com.example.util.PhoneNumbers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

enum class AppScreen {
    HOME,
    CONTACTS,
    HISTORY,
    SETTINGS,
    COMPOSE
}

data class GroupInfo(val name: String, val count: Int, val selectedCount: Int) {
    val allSelected: Boolean get() = count > 0 && selectedCount == count
}

data class ImportPreview(
    val contacts: List<ContactEntity>,
    val alreadySaved: Int,
    val skippedRows: Int
) {
    val newCount: Int get() = contacts.size - alreadySaved
}

enum class HistoryFilter { ALL, NEEDS_ATTENTION }

/** One-off messages for the snackbar. [action] runs if the user taps [actionLabel]. */
data class UiEvent(
    val message: String,
    val actionLabel: String? = null,
    val action: (() -> Unit)? = null
)

data class CampaignUiState(
    val isLoaded: Boolean = false,
    val currentScreen: AppScreen = AppScreen.HOME,
    val showWelcome: Boolean = false,
    val profile: OrganisationProfileEntity = OrganisationProfileEntity(),
    val senderIds: List<SenderIdEntity> = emptyList(),
    val contacts: List<ContactEntity> = emptyList(),
    val broadcasts: List<MessageBroadcastEntity> = emptyList(),
    val broadcastCounts: Map<Long, BroadcastCounts> = emptyMap(),
    val messageText: String = "",
    val messageRevision: Int = 0,
    val sendMode: String = SendMode.DIRECT_SIM,
    val isSending: Boolean = false,
    val gatewayConfigured: Boolean = false,
    val usingBuildApiKey: Boolean = false,
    val savedApiKeyHint: String = "",
    val pendingImport: ImportPreview? = null,
    val openBroadcastId: Long? = null,
    val historyFilter: HistoryFilter = HistoryFilter.ALL,
    val openTemplatesOnCompose: Boolean = false,
    val dismissedAttentionIds: Set<Long> = emptySet()
) {
    /**
     * Messages where some people didn't get it, sending has finished, and the user hasn't
     * chosen "Mark as done". This is the one list users need to act on.
     */
    val attentionBroadcasts: List<MessageBroadcastEntity>
        get() = broadcasts.filter { b ->
            val c = broadcastCounts[b.id]
            c != null && c.failed > 0 && c.pending == 0 &&
                b.sendMode != SendMode.SMS_APP && b.id !in dismissedAttentionIds
        }

    /** Total people across [attentionBroadcasts] who didn't get their message. */
    val attentionPeople: Int
        get() = attentionBroadcasts.sumOf { broadcastCounts[it.id]?.failed ?: 0 }

    /** The message currently going out, if any, for a live progress card. */
    val sendingBroadcast: MessageBroadcastEntity?
        get() = broadcasts.firstOrNull { (broadcastCounts[it.id]?.pending ?: 0) > 0 }

    val selectedContacts: List<ContactEntity>
        get() = contacts.filter { it.isSelected }

    val allSelected: Boolean
        get() = contacts.isNotEmpty() && contacts.all { it.isSelected }

    val activeSender: SenderIdEntity?
        get() = senderIds.firstOrNull {
            it.senderName.equals(profile.activeSenderId, ignoreCase = true)
        } ?: senderIds.firstOrNull { it.isDefault } ?: senderIds.firstOrNull()

    val activeSenderName: String
        get() = activeSender?.senderName.orEmpty()

    val displayName: String
        get() = profile.organisationName.ifBlank { "My group" }

    val groups: List<GroupInfo>
        get() = contacts
            .filter { it.groupTag.isNotBlank() }
            .groupBy { it.groupTag.trim().lowercase() }
            .map { (_, members) ->
                GroupInfo(
                    name = members.first().groupTag.trim(),
                    count = members.size,
                    selectedCount = members.count { it.isSelected }
                )
            }
            .sortedBy { it.name.lowercase() }
}

private data class LocalState(
    val initialized: Boolean = false,
    val screen: AppScreen = AppScreen.HOME,
    val composeReturnScreen: AppScreen = AppScreen.HOME,
    val messageText: String = "",
    val messageRevision: Int = 0,
    val sendMode: String = SendMode.DIRECT_SIM,
    val isSending: Boolean = false,
    val welcomeDone: Boolean = true,
    val gatewayConfigured: Boolean = false,
    val usingBuildApiKey: Boolean = false,
    val savedApiKeyHint: String = "",
    val pendingImport: ImportPreview? = null,
    val openBroadcastId: Long? = null,
    val historyFilter: HistoryFilter = HistoryFilter.ALL,
    val openTemplatesOnCompose: Boolean = false,
    val dismissedAttentionIds: Set<Long> = emptySet()
)

private data class DataState(
    val profile: OrganisationProfileEntity?,
    val senderIds: List<SenderIdEntity>,
    val contacts: List<ContactEntity>,
    val broadcasts: List<MessageBroadcastEntity>,
    val counts: List<BroadcastCounts>
)

class CampaignViewModel(
    private val repository: CampaignRepository,
    private val smsSenderService: SmsSenderService,
    private val settings: SettingsStore
) : ViewModel() {

    private val local = MutableStateFlow(LocalState())
    private val eventChannel = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = eventChannel.receiveAsFlow()

    private var draftSaveJob: Job? = null

    private val data = combine(
        repository.organisationProfile,
        repository.allSenderIds,
        repository.allContacts,
        repository.allBroadcasts,
        repository.broadcastCounts
    ) { profile, senders, contacts, broadcasts, counts ->
        DataState(profile, senders, contacts, broadcasts, counts)
    }

    val uiState: StateFlow<CampaignUiState> = combine(data, local) { d, l ->
        val profile = d.profile ?: OrganisationProfileEntity()
        CampaignUiState(
            isLoaded = l.initialized && d.profile != null,
            currentScreen = l.screen,
            showWelcome = !l.welcomeDone && profile.organisationName.isBlank(),
            profile = profile,
            senderIds = d.senderIds,
            contacts = d.contacts,
            broadcasts = d.broadcasts,
            broadcastCounts = d.counts.associateBy { it.broadcastId },
            messageText = l.messageText,
            messageRevision = l.messageRevision,
            sendMode = l.sendMode,
            isSending = l.isSending,
            gatewayConfigured = l.gatewayConfigured,
            usingBuildApiKey = l.usingBuildApiKey,
            savedApiKeyHint = l.savedApiKeyHint,
            pendingImport = l.pendingImport,
            openBroadcastId = l.openBroadcastId,
            historyFilter = l.historyFilter,
            openTemplatesOnCompose = l.openTemplatesOnCompose,
            dismissedAttentionIds = l.dismissedAttentionIds
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CampaignUiState()
    )

    init {
        refreshGatewayState()
        viewModelScope.launch {
            val profile = repository.ensureDefaultProfile()
            // Existing installs already have a name, so they skip the welcome screen.
            if (profile.organisationName.isNotBlank()) settings.hasCompletedWelcome = true
            local.update {
                it.copy(
                    initialized = true,
                    messageText = profile.draftMessage,
                    sendMode = profile.preferredSendMode,
                    welcomeDone = settings.hasCompletedWelcome,
                    dismissedAttentionIds = settings.dismissedAttentionIds
                )
            }
        }
    }

    private fun showMessage(msg: String, actionLabel: String? = null, action: (() -> Unit)? = null) {
        eventChannel.trySend(UiEvent(msg, actionLabel, action))
    }

    // --- Navigation ---

    fun navigateTo(screen: AppScreen) {
        if (screen == AppScreen.COMPOSE) {
            openCompose()
            return
        }
        local.update { it.copy(screen = screen, openBroadcastId = null) }
    }

    fun openCompose() {
        local.update {
            if (it.screen == AppScreen.COMPOSE) it
            else it.copy(screen = AppScreen.COMPOSE, composeReturnScreen = it.screen)
        }
    }

    /** Opens the composer with the template picker already showing. */
    fun openComposeWithTemplates() {
        local.update { it.copy(openTemplatesOnCompose = true) }
        openCompose()
    }

    fun consumeOpenTemplates() {
        local.update { it.copy(openTemplatesOnCompose = false) }
    }

    fun openHistory(filter: HistoryFilter) {
        local.update { it.copy(screen = AppScreen.HISTORY, openBroadcastId = null, historyFilter = filter) }
    }

    fun setHistoryFilter(filter: HistoryFilter) {
        local.update { it.copy(historyFilter = filter) }
    }

    /** "Mark as done": the user accepts that some people didn't get this message. */
    fun dismissAttention(broadcastId: Long) {
        val updated = local.value.dismissedAttentionIds + broadcastId
        settings.dismissedAttentionIds = updated
        local.update { it.copy(dismissedAttentionIds = updated) }
        showMessage("Marked as done", "Undo") { restoreAttention(broadcastId) }
    }

    private fun restoreAttention(broadcastId: Long) {
        val updated = local.value.dismissedAttentionIds - broadcastId
        settings.dismissedAttentionIds = updated
        local.update { it.copy(dismissedAttentionIds = updated) }
    }

    /** Handles the system back button. Returns false when the app should close. */
    fun navigateBack(): Boolean {
        val state = local.value
        when {
            state.screen == AppScreen.COMPOSE -> local.update { it.copy(screen = it.composeReturnScreen) }
            state.openBroadcastId != null -> local.update { it.copy(openBroadcastId = null) }
            state.screen != AppScreen.HOME -> local.update { it.copy(screen = AppScreen.HOME) }
            else -> return false
        }
        return true
    }

    fun openBroadcast(broadcastId: Long?) {
        local.update { it.copy(openBroadcastId = broadcastId) }
    }

    // --- Welcome & profile ---

    fun completeWelcome(name: String) {
        settings.hasCompletedWelcome = true
        viewModelScope.launch {
            repository.updateProfile { it.copy(organisationName = name.trim()) }
            local.update { it.copy(welcomeDone = true) }
        }
    }

    fun saveProfile(name: String, subtitle: String) {
        viewModelScope.launch {
            repository.updateProfile {
                it.copy(organisationName = name.trim(), roleOrSubtitle = subtitle.trim())
            }
            showMessage("Profile saved")
        }
    }

    fun setColours(primaryHex: String, accentHex: String) {
        viewModelScope.launch {
            repository.updateProfile { it.copy(primaryColorHex = primaryHex, accentColorHex = accentHex) }
        }
    }

    fun updateProfilePhoto(uri: Uri) {
        viewModelScope.launch {
            if (!repository.replaceProfilePhoto(uri)) showMessage("Could not open that picture. Try another one.")
        }
    }

    fun removeProfilePhoto() {
        viewModelScope.launch { repository.removeProfilePhoto() }
    }

    // --- Message draft ---

    fun updateMessageText(newText: String) {
        local.update { it.copy(messageText = newText) }
        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            delay(400)
            repository.updateProfile { it.copy(draftMessage = newText) }
        }
    }

    /**
     * Replaces the draft from outside the text field (template, undo, "send again"). Bumping
     * [CampaignUiState.messageRevision] tells the field to take the new text; ordinary typing
     * doesn't, so a late state update can never overwrite what the user just typed.
     */
    private fun replaceMessageText(newText: String) {
        updateMessageText(newText)
        local.update { it.copy(messageRevision = it.messageRevision + 1) }
    }

    fun applyTemplate(templateText: String) {
        val previous = local.value.messageText
        replaceMessageText(templateText)
        if (previous.isNotBlank() && previous != templateText) {
            showMessage("Template added. Fill in the [blanks].", "Undo") { replaceMessageText(previous) }
        }
    }

    fun updateSendMode(newMode: String) {
        local.update { it.copy(sendMode = newMode) }
        viewModelScope.launch {
            repository.updateProfile { it.copy(preferredSendMode = newMode) }
        }
    }

    // --- SMS Online GH setup ---

    private fun refreshGatewayState() {
        val userKey = settings.smsOnlineGhApiKey
        local.update {
            it.copy(
                gatewayConfigured = smsSenderService.isSmsOnlineGhApiKeyConfigured(),
                usingBuildApiKey = settings.isUsingBuildKey(),
                savedApiKeyHint = if (userKey.length > 4) "•••• ${userKey.takeLast(4)}" else ""
            )
        }
    }

    fun saveApiKey(key: String) {
        settings.smsOnlineGhApiKey = key
        refreshGatewayState()
        showMessage(if (key.isBlank()) "API key removed" else "API key saved")
    }

    fun addSenderId(senderName: String, note: String): Boolean {
        val clean = senderName.trim().replace(Regex("\\s+"), " ")
        if (!isValidSenderName(clean)) {
            showMessage("Sender names are 1 to 11 letters or numbers.")
            return false
        }
        if (uiState.value.senderIds.any { it.senderName.equals(clean, ignoreCase = true) }) {
            showMessage("\"$clean\" is already in your list")
            return false
        }
        viewModelScope.launch {
            repository.addSenderId(clean, note)
            showMessage("Sender name \"$clean\" added")
        }
        return true
    }

    fun selectActiveSenderId(senderIdEntity: SenderIdEntity) {
        viewModelScope.launch { repository.selectActiveSenderId(senderIdEntity) }
    }

    fun deleteSenderId(senderIdEntity: SenderIdEntity) {
        viewModelScope.launch {
            repository.deleteSenderId(senderIdEntity)
            showMessage("Removed \"${senderIdEntity.senderName}\"")
        }
    }

    // --- Contacts ---

    fun addManualContact(name: String, phone: String, groupTag: String) {
        viewModelScope.launch {
            val label = name.trim().ifBlank { PhoneNumbers.format(phone) }
            when (repository.addContact(name, phone, groupTag)) {
                AddContactResult.ADDED -> showMessage("$label added")
                AddContactResult.DUPLICATE -> showMessage("${PhoneNumbers.format(phone)} is already saved")
                AddContactResult.INVALID_NUMBER -> showMessage("That phone number doesn't look right")
            }
        }
    }

    /** Returns an error message, or null when the contact was saved. */
    fun updateContact(original: ContactEntity, name: String, phone: String, groupTag: String): String? {
        val normalized = PhoneNumbers.normalize(phone) ?: return "Enter a valid phone number"
        val key = PhoneNumbers.dedupeKey(normalized)
        val clash = uiState.value.contacts.any { it.id != original.id && PhoneNumbers.dedupeKey(it.phoneNumber) == key }
        if (clash) return "Another contact already has this number"
        viewModelScope.launch {
            repository.updateContact(
                original.copy(name = name.trim(), phoneNumber = normalized, groupTag = groupTag.trim())
            )
            showMessage("Contact saved")
        }
        return null
    }

    fun deleteContact(contact: ContactEntity) {
        viewModelScope.launch {
            repository.deleteContact(contact.id)
            val label = contact.name.ifBlank { PhoneNumbers.format(contact.phoneNumber) }
            showMessage("$label deleted", "Undo") {
                viewModelScope.launch { repository.restoreContact(contact) }
            }
        }
    }

    fun clearAllContacts() {
        viewModelScope.launch {
            repository.deleteAllContacts()
            showMessage("All contacts deleted")
        }
    }

    fun toggleContactSelection(contactId: Long, selected: Boolean) {
        viewModelScope.launch { repository.toggleContactSelection(contactId, selected) }
    }

    fun setAllContactsSelected(selected: Boolean) {
        viewModelScope.launch { repository.setAllContactsSelected(selected) }
    }

    /** Selects every member of the group, or deselects them all if they were all selected. */
    fun toggleGroupSelection(group: GroupInfo) {
        viewModelScope.launch { repository.setGroupSelected(group.name, !group.allSelected) }
    }

    /** Reads a phone number picked with ACTION_PICK on Phone.CONTENT_TYPE (no permission needed). */
    fun importContactFromPhonebook(context: Context, phoneUri: Uri) {
        viewModelScope.launch {
            val picked = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.query(
                        phoneUri,
                        arrayOf(
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                        ),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0).orEmpty() to cursor.getString(1).orEmpty() else null
                    }
                }.getOrNull()
            }
            if (picked == null) {
                showMessage("Could not read that contact")
                return@launch
            }
            addManualContact(picked.second, picked.first, "")
        }
    }

    fun previewCsvFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val rawText = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                    }
                }.getOrNull()
            }
            if (rawText.isNullOrBlank()) {
                showMessage("Could not read that file. Save it as CSV and try again.")
                return@launch
            }
            previewCsvText(rawText)
        }
    }

    fun previewCsvText(rawCsv: String) {
        viewModelScope.launch {
            val parsed = withContext(Dispatchers.Default) { CsvContactParser.parseCsv(rawCsv) }
            if (parsed.contacts.isEmpty()) {
                showMessage("No phone numbers found. Put one contact per line: Name, Number")
                return@launch
            }
            val existing = repository.countExisting(parsed.contacts)
            local.update { it.copy(pendingImport = ImportPreview(parsed.contacts, existing, parsed.skippedRows)) }
        }
    }

    fun confirmImport(groupName: String) {
        val preview = local.value.pendingImport ?: return
        local.update { it.copy(pendingImport = null) }
        viewModelScope.launch {
            val group = groupName.trim()
            val toInsert = if (group.isEmpty()) preview.contacts else preview.contacts.map {
                if (it.groupTag.isBlank()) it.copy(groupTag = group) else it
            }
            val inserted = repository.importContacts(toInsert)
            showMessage(if (inserted == 1) "1 contact added" else "$inserted contacts added")
        }
    }

    fun cancelImport() {
        local.update { it.copy(pendingImport = null) }
    }

    // --- Sending ---

    fun hasSendSmsPermission(): Boolean = smsSenderService.hasSendSmsPermission()

    fun isSimReady(): Boolean = smsSenderService.isSimReady()

    fun executeConfirmedSend(activityContext: Context) {
        val state = uiState.value
        val message = state.messageText.trim()
        val recipients = state.selectedContacts
        if (message.isEmpty() || recipients.isEmpty() || state.isSending) return
        dispatch(
            activityContext = activityContext,
            mode = state.sendMode,
            senderId = state.activeSenderName,
            label = state.displayName,
            message = message,
            recipients = recipients
        )
    }

    /** Sends the message again to only the people it failed for. */
    /**
     * Tries again for only the people a message failed for, updating that same message, so
     * once a problem is fixed it disappears from "Needs attention" on its own.
     */
    fun retryFailed(broadcastId: Long) {
        if (local.value.isSending) return
        local.update { it.copy(isSending = true) }
        restoreAttention(broadcastId)
        viewModelScope.launch {
            try {
                val broadcast = repository.getBroadcast(broadcastId) ?: return@launch
                val toRetry = repository.prepareRetry(broadcastId)
                if (toRetry.isEmpty()) return@launch
                showMessage("Trying again for ${if (toRetry.size == 1) "1 person" else "${toRetry.size} people"}…")
                smsSenderService.retry(broadcast, toRetry)
            } catch (e: Exception) {
                showMessage("Couldn't retry right now. Please try again.")
            } finally {
                local.update { it.copy(isSending = false) }
            }
        }
    }

    private fun dispatch(
        activityContext: Context,
        mode: String,
        senderId: String,
        label: String,
        message: String,
        recipients: List<ContactEntity>
    ) {
        local.update { it.copy(isSending = true) }
        // Jump to the message's progress page as soon as it exists, rather than making the
        // user wait on the compose screen while hundreds of messages go out.
        val onCreated: (Long) -> Unit = { id ->
            local.update { it.copy(screen = AppScreen.HISTORY, openBroadcastId = id) }
        }
        viewModelScope.launch {
            try {
                when (mode) {
                    SendMode.SMS_APP -> {
                        val (_, launched) = smsSenderService.sendViaSystemSmsApp(
                            activityContext, label, message, recipients, onCreated
                        )
                        if (!launched) showMessage("No Messages app was found on this phone")
                    }
                    SendMode.DIRECT_SIM -> smsSenderService.sendViaDirectSim(label, message, recipients, onCreated)
                    else -> smsSenderService.sendViaSmsOnlineGh(senderId, label, message, recipients, onCreated)
                }
            } catch (e: Exception) {
                showMessage("Something went wrong while sending. Check the message in History.")
            } finally {
                local.update { it.copy(isSending = false) }
            }
        }
    }

    fun observeDeliveriesForBroadcast(broadcastId: Long): Flow<List<RecipientDeliveryEntity>> {
        return repository.observeDeliveriesForBroadcast(broadcastId)
    }

    fun reuseMessage(text: String) {
        replaceMessageText(text)
        local.update { it.copy(openBroadcastId = null) }
        openCompose()
    }

    fun deleteBroadcast(broadcastId: Long) {
        local.update { if (it.openBroadcastId == broadcastId) it.copy(openBroadcastId = null) else it }
        viewModelScope.launch {
            repository.deleteBroadcast(broadcastId)
            showMessage("Message deleted from history")
        }
    }

    companion object {
        private val senderNameRegex = Regex("^[A-Za-z0-9 ]{1,11}$")

        fun isValidSenderName(name: String): Boolean =
            senderNameRegex.matches(name) && name.any { it.isLetterOrDigit() }
    }

    class Factory(private val appContext: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CampaignViewModel(
                AppGraph.repository(appContext),
                AppGraph.smsSender(appContext),
                AppGraph.settings(appContext)
            ) as T
        }
    }
}
