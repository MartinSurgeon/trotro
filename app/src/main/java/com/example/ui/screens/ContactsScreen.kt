package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ContactEntity
import com.example.viewmodel.CampaignUiState

@Composable
fun ContactsScreen(
    uiState: CampaignUiState,
    onBackToHome: () -> Unit,
    onAddManualContact: (name: String, phone: String, group: String) -> Unit,
    onImportCsvUri: (Uri) -> Unit,
    onImportCsvText: (String) -> Unit,
    onLoadSampleCsv: () -> Unit,
    onPickSystemContact: (Uri) -> Unit,
    onToggleContactSelection: (Long, Boolean) -> Unit,
    onSetAllContactsSelected: (Boolean) -> Unit,
    onDeleteContact: (Long) -> Unit,
    onClearAllContacts: () -> Unit,
    onProceedToSendSms: () -> Unit
) {
    BackHandler { onBackToHome() }

    val context = LocalContext.current
    var showAddManualDialog by remember { mutableStateOf(false) }
    var showPasteCsvDialog by remember { mutableStateOf(false) }
    var showConfirmClearAll by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val csvFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportCsvUri(uri)
        }
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            onPickSystemContact(uri)
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val accentColor = MaterialTheme.colorScheme.secondary

    val filteredContacts = remember(uiState.contacts, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.contacts
        } else {
            val q = searchQuery.trim().lowercase()
            uiState.contacts.filter {
                it.name.lowercase().contains(q) ||
                    it.phoneNumber.lowercase().contains(q) ||
                    it.groupTag.lowercase().contains(q)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Top Action Buttons: Add Manually & Import CSV
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = primaryColor
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Add or Import Contacts",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Add phone numbers one by one or import a CSV contact list.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showAddManualDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("add_contact_manual_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = primaryColor
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add Number",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = {
                                csvFileLauncher.launch(
                                    arrayOf(
                                        "text/csv",
                                        "text/comma-separated-values",
                                        "text/plain",
                                        "*/*"
                                    )
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("import_csv_file_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color.White)
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Import CSV",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    // Secondary helper row: Paste CSV / Phonebook / Load Sample
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPasteCsvDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("paste_csv_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste CSV Text", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { contactPickerLauncher.launch(null) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_phonebook_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContactPhone,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("From Phonebook", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.contacts.isNotEmpty()) {
                // Search & Select All Header Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search name or phone number...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contacts_search_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onSetAllContactsSelected(!uiState.allSelected)
                                }
                                .padding(end = 8.dp)
                                .testTag("contacts_select_all_toggle")
                        ) {
                            Checkbox(
                                checked = uiState.allSelected,
                                onCheckedChange = { checked ->
                                    onSetAllContactsSelected(checked)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                            )
                            Text(
                                text = if (uiState.allSelected) {
                                    "All Selected (${uiState.selectedContacts.size}/${uiState.contacts.size})"
                                } else {
                                    "Select All (${uiState.selectedContacts.size}/${uiState.contacts.size} selected)"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { showConfirmClearAll = true },
                            modifier = Modifier.testTag("clear_all_contacts_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear All Contacts",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Contacts List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("contacts_lazy_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 84.dp)
                ) {
                    items(filteredContacts, key = { it.id }) { contact ->
                        ContactItemRow(
                            contact = contact,
                            primaryColor = primaryColor,
                            onToggleSelected = { selected ->
                                onToggleContactSelection(contact.id, selected)
                            },
                            onDelete = { onDeleteContact(contact.id) }
                        )
                    }
                }
            } else {
                // Empty State with Quick Actions
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
                                    imageVector = Icons.Default.GroupAdd,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No Contacts Added Yet",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Add phone numbers manually, import a CSV file, or load a sample contact list to test sending SMS immediately.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        FilledTonalButton(
                            onClick = onLoadSampleCsv,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("load_sample_csv_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.DoneAll, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Load Sample Campaign Contacts (CSV)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Floating Bottom Bar when contacts exist
        if (uiState.contacts.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(16.dp),
                color = accentColor,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = onProceedToSendSms,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("contacts_proceed_to_send_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Write & Send SMS (${uiState.selectedContacts.size} Selected)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }

    if (showAddManualDialog) {
        AddManualContactDialog(
            onDismiss = { showAddManualDialog = false },
            onAdd = { name, phone, group ->
                onAddManualContact(name, phone, group)
                showAddManualDialog = false
            }
        )
    }

    if (showPasteCsvDialog) {
        PasteCsvContactsDialog(
            onDismiss = { showPasteCsvDialog = false },
            onLoadSample = {
                onLoadSampleCsv()
                showPasteCsvDialog = false
            },
            onImportCsvText = { csvText ->
                onImportCsvText(csvText)
                showPasteCsvDialog = false
            }
        )
    }

    if (showConfirmClearAll) {
        AlertDialog(
            onDismissRequest = { showConfirmClearAll = false },
            title = { Text("Clear All Contacts?") },
            text = {
                Text("Are you sure you want to remove all ${uiState.contacts.size} contacts from your list?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllContacts()
                        showConfirmClearAll = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearAll = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ContactItemRow(
    contact: ContactEntity,
    primaryColor: Color,
    onToggleSelected: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onToggleSelected(!contact.isSelected) }
            .testTag("contact_item_${contact.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (contact.isSelected) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        ),
        border = BorderStroke(
            width = if (contact.isSelected) 1.5.dp else 1.dp,
            color = if (contact.isSelected) primaryColor.copy(alpha = 0.45f) else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = contact.isSelected,
                onCheckedChange = { onToggleSelected(it) },
                colors = CheckboxDefaults.colors(checkedColor = primaryColor)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Initials Circle
            val initials = contact.name
                .split(" ")
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .take(2)
                .joinToString("")
                .ifEmpty { "#" }

            Surface(
                shape = CircleShape,
                color = primaryColor.copy(alpha = 0.14f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium,
                        color = primaryColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (contact.groupTag.isNotBlank() && contact.groupTag != "General") {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = primaryColor.copy(alpha = 0.1f),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = contact.groupTag,
                        style = MaterialTheme.typography.labelMedium,
                        color = primaryColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete ${contact.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AddManualContactDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, phone: String, group: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Contact Manually",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    placeholder = { Text("e.g. Abena Mensah") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_contact_name_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number *") },
                    placeholder = { Text("e.g. +233244102030 or 0244102030") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_contact_phone_input")
                )

                OutlinedTextField(
                    value = group,
                    onValueChange = { group = it },
                    label = { Text("Group / Branch (Optional)") },
                    placeholder = { Text("e.g. Women's Wing, Delegates, Choir") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, phone, group) },
                enabled = phone.count { it.isDigit() } >= 6,
                modifier = Modifier.testTag("confirm_add_contact_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Contact")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PasteCsvContactsDialog(
    onDismiss: () -> Unit,
    onLoadSample: () -> Unit,
    onImportCsvText: (String) -> Unit
) {
    var csvText by remember {
        mutableStateOf(
            "Abena Mensah, +233244102030\nKofi Owusu, +233208112233\nAma Serwaa, +233541998877"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Paste CSV Contacts",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Enter one contact per line in the format: Name, Phone Number",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = csvText,
                    onValueChange = { csvText = it },
                    label = { Text("CSV Lines (Name, Phone)") },
                    minLines = 5,
                    maxLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("paste_csv_textarea")
                )

                FilledTonalButton(
                    onClick = onLoadSample,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Or Load 6 Sample Campaign Contacts")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onImportCsvText(csvText) },
                enabled = csvText.isNotBlank(),
                modifier = Modifier.testTag("confirm_paste_csv_button")
            ) {
                Text("Import Contacts")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
