package com.example.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CampaignDatabase
import com.example.data.CampaignRepository
import com.example.data.ContactEntity
import com.example.data.MessageBroadcastEntity
import com.example.data.OrganisationProfileEntity
import com.example.data.RecipientDeliveryEntity
import com.example.data.SenderIdEntity
import com.example.sms.SmsSenderService
import com.example.util.CsvContactParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

enum class AppScreen {
    HOME,
    CONTACTS,
    SEND_SMS,
    HISTORY
}

data class CampaignUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val profile: OrganisationProfileEntity = OrganisationProfileEntity(),
    val senderIds: List<SenderIdEntity> = emptyList(),
    val contacts: List<ContactEntity> = emptyList(),
    val broadcasts: List<MessageBroadcastEntity> = emptyList(),
    val messageText: String = "Please vote for Joyce Afi Fianu as your Women Organizer. Together, we can work for change. Thank you.",
    val sendMode: String = "SMSONLINEGH", // "SMSONLINEGH", "DIRECT_SIM", "SMS_APP"
    val snackbarMessage: String? = null,
    val isSending: Boolean = false
) {
    val selectedContacts: List<ContactEntity>
        get() = contacts.filter { it.isSelected }

    val allSelected: Boolean
        get() = contacts.isNotEmpty() && contacts.all { it.isSelected }

    val activeSender: SenderIdEntity?
        get() = senderIds.firstOrNull {
            it.senderName.equals(profile.activeSenderId, ignoreCase = true)
        } ?: senderIds.firstOrNull { it.isDefault } ?: senderIds.firstOrNull()

    val activeSenderName: String
        get() = activeSender?.senderName ?: profile.activeSenderId.ifBlank { "JOYCE AFI" }

    val activeCandidateLabel: String
        get() = activeSender?.candidateOrOrgLabel ?: profile.organisationName

    val delegateGroups: List<String>
        get() = listOf("All") + contacts
            .map { it.groupTag.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .sorted()
}

class CampaignViewModel(
    private val repository: CampaignRepository,
    private val smsSenderService: SmsSenderService
) : ViewModel() {

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    private val _messageText = MutableStateFlow(
        "Please vote for Joyce Afi Fianu as your Women Organizer. Together, we can work for change. Thank you."
    )
    private val _sendMode = MutableStateFlow("SMSONLINEGH")
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private val _isSending = MutableStateFlow(false)
    private var messageInitializedFromProfile = false

    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    val uiState: StateFlow<CampaignUiState> = combine(
        _currentScreen,
        repository.organisationProfile,
        repository.allSenderIds,
        repository.allContacts,
        combine(
            repository.allBroadcasts,
            _messageText,
            _sendMode,
            _snackbarMessage,
            _isSending
        ) { broadcasts, msg, mode, snack, sending ->
            Quint(broadcasts, msg, mode, snack, sending)
        }
    ) { screen, profileEntity, senderIds, contacts, quint ->
        val resolvedProfile = profileEntity ?: OrganisationProfileEntity()
        if (!messageInitializedFromProfile && profileEntity != null && profileEntity.draftMessage.isNotBlank()) {
            messageInitializedFromProfile = true
            _messageText.value = profileEntity.draftMessage
            _sendMode.value = profileEntity.preferredSendMode
        }
        CampaignUiState(
            currentScreen = screen,
            profile = resolvedProfile,
            senderIds = senderIds,
            contacts = contacts,
            broadcasts = quint.first,
            messageText = quint.second,
            sendMode = quint.third,
            snackbarMessage = quint.fourth,
            isSending = quint.fifth
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CampaignUiState()
    )

    init {
        viewModelScope.launch {
            val profile = repository.ensureDefaultProfileAndSenderIds()
            if (!messageInitializedFromProfile) {
                messageInitializedFromProfile = true
                _messageText.value = profile.draftMessage
                _sendMode.value = profile.preferredSendMode
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun updateMessageText(newText: String) {
        _messageText.value = newText
        viewModelScope.launch {
            val current = uiState.value.profile
            repository.saveOrganisationProfile(current.copy(draftMessage = newText))
        }
    }

    fun updateSendMode(newMode: String) {
        _sendMode.value = newMode
        viewModelScope.launch {
            val current = uiState.value.profile
            repository.saveOrganisationProfile(current.copy(preferredSendMode = newMode))
        }
    }

    fun addSenderId(
        senderName: String,
        candidateOrOrgLabel: String,
        defaultDelegateGroup: String
    ) {
        val cleanSender = senderName.trim().take(11)
        if (cleanSender.isEmpty()) {
            showMessage("Please enter a Sender ID (up to 11 characters)")
            return
        }
        viewModelScope.launch {
            repository.addSenderId(
                senderName = cleanSender,
                candidateOrOrgLabel = candidateOrOrgLabel.trim().ifEmpty { cleanSender },
                defaultDelegateGroup = defaultDelegateGroup.trim().ifEmpty { "All" },
                makeActive = true
            )
            showMessage("Added & selected Sender ID '$cleanSender'")
        }
    }

    fun selectActiveSenderId(senderIdEntity: SenderIdEntity, filterDelegates: Boolean = true) {
        viewModelScope.launch {
            repository.selectActiveSenderId(senderIdEntity, filterGroupIfSpecific = filterDelegates)
            val groupNote = if (filterDelegates &&
                senderIdEntity.defaultDelegateGroup.isNotBlank() &&
                !senderIdEntity.defaultDelegateGroup.equals("All", ignoreCase = true)
            ) {
                " (${senderIdEntity.defaultDelegateGroup})"
            } else {
                ""
            }
            showMessage("Active Sender ID: ${senderIdEntity.senderName}$groupNote")
        }
    }

    fun deleteSenderId(senderIdEntity: SenderIdEntity) {
        if (uiState.value.senderIds.size <= 1) {
            showMessage("Keep at least one Sender ID")
            return
        }
        viewModelScope.launch {
            repository.deleteSenderId(senderIdEntity)
            showMessage("Removed Sender ID '${senderIdEntity.senderName}'")
        }
    }

    fun selectDelegatesByGroup(groupTag: String) {
        viewModelScope.launch {
            repository.selectContactsByGroup(groupTag)
            if (groupTag.equals("All", ignoreCase = true)) {
                showMessage("Selected all delegates")
            } else {
                showMessage("Selected delegates in '$groupTag'")
            }
        }
    }

    fun saveOrganisationCustomization(
        organisationName: String,
        roleOrSubtitle: String,
        slogan: String,
        primaryColorHex: String,
        accentColorHex: String,
        customPhotoPath: String?
    ) {
        viewModelScope.launch {
            val current = uiState.value.profile
            val updated = current.copy(
                organisationName = organisationName.trim().ifEmpty { "MY ORGANISATION" },
                roleOrSubtitle = roleOrSubtitle.trim(),
                slogan = slogan.trim(),
                primaryColorHex = primaryColorHex,
                accentColorHex = accentColorHex,
                customPhotoPath = customPhotoPath
            )
            repository.saveOrganisationProfile(updated)
            showMessage("Organisation profile & colours updated!")
        }
    }

    fun updateOrganisationPhotoFromUri(uri: Uri) {
        viewModelScope.launch {
            val savedPath = repository.copyPhotoToInternalStorage(uri)
            if (savedPath != null) {
                val current = uiState.value.profile
                repository.saveOrganisationProfile(current.copy(customPhotoPath = savedPath))
                showMessage("Campaign photo / logo updated!")
            } else {
                showMessage("Could not load selected image")
            }
        }
    }

    fun resetOrganisationPhotoToDefault() {
        viewModelScope.launch {
            val current = uiState.value.profile
            repository.saveOrganisationProfile(current.copy(customPhotoPath = null))
            showMessage("Restored default campaign flyer illustration")
        }
    }

    fun addManualContact(name: String, phone: String, groupTag: String = "All Delegates") {
        val sanitized = CsvContactParser.sanitizePhone(phone)
        if (!CsvContactParser.isValidPhone(sanitized)) {
            showMessage("Please enter a valid phone number (at least 6 digits)")
            return
        }
        val finalName = name.trim().ifEmpty { "Delegate $sanitized" }
        viewModelScope.launch {
            repository.addContact(finalName, sanitized, groupTag.ifBlank { "All Delegates" })
            showMessage("Added $finalName ($sanitized)")
        }
    }

    fun importContactFromSystemUri(context: Context, contactUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var contactId: String? = null
                var displayName = ""
                context.contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                        val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        if (idIndex >= 0) contactId = cursor.getString(idIndex)
                        if (nameIndex >= 0) displayName = cursor.getString(nameIndex) ?: ""
                    }
                }
                var phone: String? = null
                if (contactId != null) {
                    context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(contactId),
                        null
                    )?.use { pCursor ->
                        if (pCursor.moveToFirst()) {
                            val numIndex = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (numIndex >= 0) phone = pCursor.getString(numIndex)
                        }
                    }
                }
                val cleanPhone = phone?.let { CsvContactParser.sanitizePhone(it) } ?: ""
                if (CsvContactParser.isValidPhone(cleanPhone)) {
                    repository.addContact(
                        name = displayName.ifBlank { "Delegate $cleanPhone" },
                        phoneNumber = cleanPhone,
                        groupTag = "Phonebook"
                    )
                    showMessage("Added ${displayName.ifBlank { cleanPhone }}")
                } else {
                    showMessage("Selected contact does not have a valid phone number")
                }
            } catch (e: Exception) {
                showMessage("Could not read phonebook contact: make sure a phone number is saved")
            }
        }
    }

    fun importContactsFromCsvUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val rawText = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BufferedReader(InputStreamReader(stream)).readText()
                    }
                } catch (e: Exception) {
                    null
                }
            }
            if (rawText.isNullOrBlank()) {
                showMessage("Could not read the selected CSV file")
                return@launch
            }
            importContactsFromCsvString(rawText)
        }
    }

    fun importContactsFromCsvString(rawCsv: String) {
        viewModelScope.launch {
            val parseResult = CsvContactParser.parseCsv(rawCsv)
            if (parseResult.contacts.isEmpty()) {
                showMessage("No valid phone numbers found in CSV")
                return@launch
            }
            val insertedCount = repository.importContacts(parseResult.contacts)
            val duplicateCount = parseResult.contacts.size - insertedCount
            val msg = buildString {
                append("Imported $insertedCount delegate contact(s)")
                if (duplicateCount > 0) append(" ($duplicateCount duplicate(s) skipped)")
            }
            showMessage(msg)
        }
    }

    fun loadSampleCampaignCsv() {
        val sampleCsv = """
            Name,Phone Number,Delegate Group
            Abena Mensah,+233244102030,Women's Wing
            Akosua Ofori,+233502334455,Women's Wing
            Kofi Owusu,+233208112233,Youth Delegates
            Yaw Asante,+233249876543,Youth Delegates
            Ama Serwaa,+233541998877,Branch Executives
            Kwame Boateng,+233277445566,Branch Executives
        """.trimIndent()
        importContactsFromCsvString(sampleCsv)
    }

    fun toggleContactSelection(contactId: Long, selected: Boolean) {
        viewModelScope.launch {
            repository.toggleContactSelection(contactId, selected)
        }
    }

    fun setAllContactsSelected(selected: Boolean) {
        viewModelScope.launch {
            repository.setAllContactsSelected(selected)
        }
    }

    fun deleteContact(contactId: Long) {
        viewModelScope.launch {
            repository.deleteContact(contactId)
            showMessage("Contact removed")
        }
    }

    fun clearAllContacts() {
        viewModelScope.launch {
            repository.deleteAllContacts()
            showMessage("All contacts cleared")
        }
    }

    fun isSmsOnlineGhConfigured(): Boolean = smsSenderService.isSmsOnlineGhApiKeyConfigured()

    fun hasSendSmsPermission(): Boolean = smsSenderService.hasSendSmsPermission()

    fun isSimReady(): Boolean = smsSenderService.isSimReady()

    fun executeConfirmedSend(
        activityContext: Context,
        onCompleteNavigateToHistory: () -> Unit
    ) {
        val state = uiState.value
        val recipients = state.selectedContacts
        val message = state.messageText.trim()
        val senderId = state.activeSenderName
        val candidateLabel = state.activeCandidateLabel

        if (message.isEmpty()) {
            showMessage("Please write a message before sending")
            return
        }
        if (recipients.isEmpty()) {
            showMessage("Please select at least one recipient")
            return
        }

        viewModelScope.launch {
            _isSending.value = true
            try {
                when (state.sendMode) {
                    "SMS_APP" -> {
                        val (_, launched) = smsSenderService.sendViaSystemSmsApp(
                            activityContext = activityContext,
                            senderId = senderId,
                            candidateLabel = candidateLabel,
                            messageTemplate = message,
                            recipients = recipients
                        )
                        if (launched) {
                            showMessage("Opened device SMS app with ${recipients.size} recipient(s)")
                        } else {
                            showMessage("No default SMS app available on this device")
                        }
                    }
                    "DIRECT_SIM" -> {
                        smsSenderService.sendViaDirectSim(
                            senderId = senderId,
                            candidateLabel = candidateLabel,
                            messageTemplate = message,
                            recipients = recipients
                        )
                        if (smsSenderService.isSimReady()) {
                            showMessage("Sending SMS to ${recipients.size} recipient(s) via SIM...")
                        } else {
                            showMessage("Attempted SIM send — check History for SIM/carrier status")
                        }
                    }
                    else -> {
                        smsSenderService.sendViaSmsOnlineGh(
                            senderId = senderId,
                            candidateLabel = candidateLabel,
                            messageTemplate = message,
                            recipients = recipients
                        )
                        if (smsSenderService.isSmsOnlineGhApiKeyConfigured()) {
                            showMessage("Submitted ${recipients.size} SMS via SMSOnlineGH (Sender: $senderId)")
                        } else {
                            showMessage("Add SMSONLINEGH_API_KEY in AI Studio Secrets panel to send via SMSOnlineGH")
                        }
                    }
                }
                onCompleteNavigateToHistory()
            } finally {
                _isSending.value = false
            }
        }
    }

    fun observeDeliveriesForBroadcast(broadcastId: Long): Flow<List<RecipientDeliveryEntity>> {
        return repository.observeDeliveriesForBroadcast(broadcastId)
    }

    fun deleteBroadcast(broadcastId: Long) {
        viewModelScope.launch {
            repository.deleteBroadcast(broadcastId)
            showMessage("Message history item deleted")
        }
    }

    private data class Quint<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E
    )

    class Factory(private val appContext: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = CampaignDatabase.getInstance(appContext)
            val repo = CampaignRepository(db.campaignDao(), appContext)
            val smsService = SmsSenderService(appContext, repo)
            return CampaignViewModel(repo, smsService) as T
        }
    }
}
