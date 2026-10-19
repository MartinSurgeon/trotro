package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContactPhone
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.LocalSnackbarHostState
import com.example.data.ContactEntity
import com.example.ui.components.Avatar
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.EmptyState
import com.example.ui.components.PrimaryActionButton
import com.example.ui.components.ScreenTitleBar
import com.example.util.PhoneNumbers
import com.example.viewmodel.CampaignUiState
import com.example.viewmodel.ImportPreview
import kotlinx.coroutines.launch

private val csvMimeTypes = arrayOf(
    "text/csv",
    "text/comma-separated-values",
    "text/plain",
    "application/csv",
    "application/vnd.ms-excel",
    "*/*"
)

/** A–Z sections like the phone's own contacts app; numbers-only contacts go under "#" at the end. */
private fun alphabetSections(contacts: List<ContactEntity>): List<Pair<String, List<ContactEntity>>> =
    contacts
        .groupBy { c -> c.name.trim().firstOrNull()?.takeIf { it.isLetter() }?.uppercase() ?: "#" }
        .toList()
        .sortedWith(compareBy({ it.first == "#" }, { it.first }))
        .map { (letter, members) -> letter to members.sortedBy { it.name.lowercase() } }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactsScreen(
    uiState: CampaignUiState,
    onAddContact: (name: String, phone: String, group: String) -> Unit,
    onUpdateContact: (ContactEntity, String, String, String) -> String?,
    onDeleteContact: (ContactEntity) -> Unit,
    onClearAllContacts: () -> Unit,
    onPickedPhonebookContact: (Uri) -> Unit,
    onPickedCsvFile: (Uri) -> Unit,
    onPastedCsv: (String) -> Unit,
    onConfirmImport: (groupName: String) -> Unit,
    onCancelImport: () -> Unit,
    onNewMessage: () -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var groupFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddOptions by remember { mutableStateOf(false) }
    var showAddForm by remember { mutableStateOf(false) }
    var showPaste by remember { mutableStateOf(false) }
    var showConfirmClear by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ContactEntity?>(null) }

    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onPickedCsvFile(uri)
    }
    // Picking a phone number (not a whole contact) grants temporary read access to that one
    // entry, so no contacts permission is needed.
    val phonebookLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.data?.let(onPickedPhonebookContact)
    }

    fun openPhonebook() {
        try {
            phonebookLauncher.launch(
                Intent(Intent.ACTION_PICK).setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE)
            )
        } catch (_: ActivityNotFoundException) {
            scope.launch { snackbar.showSnackbar("No contacts app found on this phone") }
        }
    }

    val groups = uiState.groups
    val activeFilter = groupFilter?.takeIf { f -> groups.any { it.name.equals(f, ignoreCase = true) } }
    val filtered = remember(uiState.contacts, searchQuery, activeFilter) {
        val q = searchQuery.trim().lowercase()
        val qDigits = q.filter { it.isDigit() }
        uiState.contacts.filter { c ->
            val inGroup = activeFilter == null || c.groupTag.trim().equals(activeFilter, ignoreCase = true)
            val matches = q.isEmpty() ||
                c.name.lowercase().contains(q) ||
                c.groupTag.lowercase().contains(q) ||
                (qDigits.isNotEmpty() && c.phoneNumber.contains(qDigits))
            inGroup && matches
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenTitleBar(
                title = "Contacts",
                actions = {
                    if (uiState.contacts.isNotEmpty()) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Delete all contacts") },
                                    leadingIcon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        showConfirmClear = true
                                    }
                                )
                            }
                        }
                    }
                }
            )

            if (uiState.contacts.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.GroupAdd,
                    title = "No contacts yet",
                    message = "Add the people you want to send messages to. You can pick them from your phone or import a list.",
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    action = {
                        PrimaryActionButton(
                            text = "Add contacts",
                            icon = Icons.Rounded.PersonAdd,
                            onClick = { showAddOptions = true },
                            modifier = Modifier.widthIn(max = 320.dp).testTag("add_contact_manual_button")
                        )
                    }
                )
            } else {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search name, number or group") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("contacts_search_input")
                )

                if (groups.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = activeFilter == null,
                                onClick = { groupFilter = null },
                                label = { Text("All (${uiState.contacts.size})") }
                            )
                        }
                        items(groups, key = { it.name.lowercase() }) { group ->
                            FilterChip(
                                selected = activeFilter.equals(group.name, ignoreCase = true),
                                onClick = { groupFilter = group.name },
                                label = { Text("${group.name} (${group.count})") }
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.size(8.dp))
                }

                Text(
                    text = when {
                        filtered.size == uiState.contacts.size -> "${uiState.contacts.size} contacts"
                        else -> "Showing ${filtered.size} of ${uiState.contacts.size}"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )

                if (filtered.isEmpty()) {
                    Text(
                        text = "No contacts match \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().testTag("contacts_lazy_list"),
                    // Room for the two stacked buttons so they never cover the last contact.
                    contentPadding = PaddingValues(bottom = 160.dp)
                ) {
                    alphabetSections(filtered).forEach { (letter, members) ->
                        stickyHeader(key = "letter_$letter") {
                            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    letter,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                                )
                            }
                        }
                        items(members, key = { it.id }) { contact ->
                            ContactRow(contact = contact, onClick = { editing = contact })
                        }
                    }
                }
            }
        }

        if (uiState.contacts.isNotEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = { showAddOptions = true },
                    icon = { Icon(Icons.Rounded.PersonAdd, contentDescription = null) },
                    text = { Text("Add") },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.testTag("add_contact_fab")
                )
                ExtendedFloatingActionButton(
                    onClick = onNewMessage,
                    icon = { Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null) },
                    text = { Text("New message") },
                    modifier = Modifier.testTag("contacts_proceed_to_send_button")
                )
            }
        }
    }

    if (showAddOptions) {
        AddContactsSheet(
            onDismiss = { showAddOptions = false },
            onTypeNumber = { showAddOptions = false; showAddForm = true },
            onPhonebook = { showAddOptions = false; openPhonebook() },
            onImportFile = { showAddOptions = false; csvLauncher.launch(csvMimeTypes) },
            onPaste = { showAddOptions = false; showPaste = true }
        )
    }

    if (showAddForm) {
        ContactFormDialog(
            title = "Add contact",
            initial = null,
            existingGroups = groups.map { it.name },
            onDismiss = { showAddForm = false },
            onSave = { name, phone, group ->
                onAddContact(name, phone, group)
                null
            }
        )
    }

    editing?.let { contact ->
        ContactFormDialog(
            title = "Edit contact",
            initial = contact,
            existingGroups = groups.map { it.name },
            onDismiss = { editing = null },
            onSave = { name, phone, group -> onUpdateContact(contact, name, phone, group) },
            onDelete = {
                editing = null
                onDeleteContact(contact)
            }
        )
    }

    if (showPaste) {
        PasteListDialog(
            onDismiss = { showPaste = false },
            onImport = { text ->
                showPaste = false
                onPastedCsv(text)
            }
        )
    }

    uiState.pendingImport?.let { preview ->
        ImportPreviewDialog(
            preview = preview,
            existingGroups = groups.map { it.name },
            onConfirm = onConfirmImport,
            onDismiss = onCancelImport
        )
    }

    if (showConfirmClear) {
        ConfirmDialog(
            title = "Delete all contacts?",
            message = "This removes all ${uiState.contacts.size} contacts from Dawuro. Your phone's own contacts are not affected. This cannot be undone.",
            confirmLabel = "Delete all",
            destructive = true,
            onConfirm = onClearAllContacts,
            onDismiss = { showConfirmClear = false }
        )
    }
}

@Composable
private fun ContactRow(contact: ContactEntity, onClick: () -> Unit) {
    val formatted = PhoneNumbers.format(contact.phoneNumber)
    val network = PhoneNumbers.network(contact.phoneNumber)
    ListItem(
        headlineContent = {
            Text(
                text = contact.name.ifBlank { formatted },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            val parts = buildList {
                if (contact.hasName) add(formatted)
                network?.let(::add)
            }
            if (parts.isNotEmpty()) Text(parts.joinToString(" · "), maxLines = 1)
        },
        leadingContent = { Avatar(name = contact.name.ifBlank { "" }) },
        trailingContent = {
            if (contact.groupTag.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        text = contact.groupTag,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 120.dp).padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .testTag("contact_item_${contact.id}")
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddContactsSheet(
    onDismiss: () -> Unit,
    onTypeNumber: () -> Unit,
    onPhonebook: () -> Unit,
    onImportFile: () -> Unit,
    onPaste: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(
                "Add contacts",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            SheetOption(Icons.Rounded.ContactPhone, "Pick from my phone", "Choose someone saved on this phone", onPhonebook, "pick_phonebook_button")
            SheetOption(Icons.Rounded.Dialpad, "Type a number", "Enter a name and phone number", onTypeNumber, "type_number_button")
            SheetOption(Icons.Rounded.UploadFile, "Import a list (CSV file)", "From Excel or Google Sheets: save as CSV with Name, Number columns", onImportFile, "import_csv_file_button")
            SheetOption(Icons.Rounded.ContentPaste, "Paste a list", "Copy names and numbers from WhatsApp, notes or email", onPaste, "paste_csv_button")
        }
    }
}

@Composable
private fun SheetOption(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit, tag: String) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(10.dp).size(24.dp)
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp).testTag(tag)
    )
}

/**
 * Add/edit form. [onSave] returns an error to show, or null to close the dialog.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContactFormDialog(
    title: String,
    initial: ContactEntity?,
    existingGroups: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, group: String) -> String?,
    onDelete: (() -> Unit)? = null
) {
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var phone by rememberSaveable { mutableStateOf(initial?.phoneNumber?.let(PhoneNumbers::format).orEmpty()) }
    var group by rememberSaveable { mutableStateOf(initial?.groupTag.orEmpty()) }
    var triedSave by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    val normalized = PhoneNumbers.normalize(phone)
    val phoneError = when {
        normalized != null -> null
        phone.isBlank() -> if (triedSave) "Enter a phone number" else null
        triedSave || phone.count { it.isDigit() } >= 10 -> "Use 10 digits like 024 123 4567, or +country code"
        else -> null
    }
    val network = normalized?.let(PhoneNumbers::network)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth().testTag("manual_contact_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; saveError = null },
                    label = { Text("Phone number") },
                    placeholder = { Text("024 123 4567") },
                    singleLine = true,
                    isError = phoneError != null || saveError != null,
                    supportingText = {
                        Text(saveError ?: phoneError ?: network?.let { "$it number" } ?: " ")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("manual_contact_phone_input")
                )
                OutlinedTextField(
                    value = group,
                    onValueChange = { group = it },
                    label = { Text("Group (optional)") },
                    placeholder = { Text("e.g. Choir, Customers, Family") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                val suggestions = existingGroups.filter { !it.equals(group.trim(), ignoreCase = true) }
                if (suggestions.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.take(8).forEach { g ->
                            SuggestionChip(onClick = { group = g }, label = { Text(g) })
                        }
                    }
                }
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.size(8.dp))
                        Text("Delete contact", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    triedSave = true
                    if (normalized != null) {
                        saveError = onSave(name, normalized, group)
                        if (saveError == null) onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_add_contact_button")
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PasteListDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paste a list") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "One person per line. A name is optional.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Ama Serwaa, 0241234567\nKofi Mensah, 0201234567\n0551234567") },
                    minLines = 5,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth().testTag("paste_csv_textarea")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(text) },
                enabled = text.any { it.isDigit() },
                modifier = Modifier.testTag("confirm_paste_csv_button")
            ) { Text("Continue") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImportPreviewDialog(
    preview: ImportPreview,
    existingGroups: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var group by rememberSaveable { mutableStateOf("") }
    val hasGroupsInFile = preview.contacts.any { it.groupTag.isNotBlank() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (preview.newCount == 1) "Add 1 contact?" else "Add ${preview.newCount} contacts?")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val notes = buildList {
                    if (preview.alreadySaved > 0) add("${preview.alreadySaved} already saved (will be skipped)")
                    if (preview.skippedRows > 0) add("${preview.skippedRows} lines had no valid number")
                }
                notes.forEach {
                    Text("• $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        preview.contacts.take(4).forEach { c ->
                            val number = PhoneNumbers.format(c.phoneNumber)
                            Text(
                                text = if (c.hasName) "${c.name} · $number" else number,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (preview.contacts.size > 4) {
                            Text(
                                "and ${preview.contacts.size - 4} more…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = group,
                    onValueChange = { group = it },
                    label = { Text("Put them in a group (optional)") },
                    placeholder = { Text("e.g. Youth fellowship") },
                    supportingText = if (hasGroupsInFile) {
                        { Text("Rows that already have a group keep it") }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                if (existingGroups.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        existingGroups.take(6).forEach { g ->
                            SuggestionChip(onClick = { group = g }, label = { Text(g) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(group) }, enabled = preview.newCount > 0) {
                Text("Add contacts")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
