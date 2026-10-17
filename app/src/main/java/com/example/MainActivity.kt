package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SendSmsScreen
import com.example.ui.theme.CampaignSmsTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.CampaignViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appContext = LocalContext.current.applicationContext
            val viewModel: CampaignViewModel = viewModel(
                factory = CampaignViewModel.Factory(appContext)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            CampaignSmsTheme(
                primaryColorHex = uiState.profile.primaryColorHex,
                accentColorHex = uiState.profile.accentColorHex
            ) {
                CampaignSmsAppContent(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignSmsAppContent(viewModel: CampaignViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val accentColor = MaterialTheme.colorScheme.secondary

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val titleText = when (uiState.currentScreen) {
                        AppScreen.HOME -> "${uiState.profile.organisationName} • ${uiState.activeSenderName}"
                        AppScreen.CONTACTS -> "Delegates & Contacts (${uiState.contacts.size})"
                        AppScreen.SEND_SMS -> "Send SMS (${uiState.activeSenderName})"
                        AppScreen.HISTORY -> "Sent Message History"
                    }
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = primaryColor,
                    indicatorColor = primaryColor,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )

                NavigationBarItem(
                    selected = uiState.currentScreen == AppScreen.HOME,
                    onClick = { viewModel.navigateTo(AppScreen.HOME) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentScreen == AppScreen.HOME) {
                                Icons.Default.Home
                            } else {
                                Icons.Outlined.Home
                            },
                            contentDescription = "Home"
                        )
                    },
                    label = {
                        Text(
                            text = "Home",
                            fontWeight = if (uiState.currentScreen == AppScreen.HOME) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = navItemColors,
                    modifier = Modifier.testTag("nav_tab_home")
                )

                NavigationBarItem(
                    selected = uiState.currentScreen == AppScreen.CONTACTS,
                    onClick = { viewModel.navigateTo(AppScreen.CONTACTS) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (uiState.contacts.isNotEmpty()) {
                                    Badge(
                                        containerColor = accentColor,
                                        contentColor = Color.White
                                    ) {
                                        Text("${uiState.contacts.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (uiState.currentScreen == AppScreen.CONTACTS) {
                                    Icons.Default.Contacts
                                } else {
                                    Icons.Outlined.Contacts
                                },
                                contentDescription = "Contacts"
                            )
                        }
                    },
                    label = {
                        Text(
                            text = "Contacts",
                            fontWeight = if (uiState.currentScreen == AppScreen.CONTACTS) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = navItemColors,
                    modifier = Modifier.testTag("nav_tab_contacts")
                )

                NavigationBarItem(
                    selected = uiState.currentScreen == AppScreen.SEND_SMS,
                    onClick = { viewModel.navigateTo(AppScreen.SEND_SMS) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentScreen == AppScreen.SEND_SMS) {
                                Icons.AutoMirrored.Filled.Send
                            } else {
                                Icons.Outlined.Sms
                            },
                            contentDescription = "Send SMS"
                        )
                    },
                    label = {
                        Text(
                            text = "Send SMS",
                            fontWeight = if (uiState.currentScreen == AppScreen.SEND_SMS) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = navItemColors,
                    modifier = Modifier.testTag("nav_tab_send_sms")
                )

                NavigationBarItem(
                    selected = uiState.currentScreen == AppScreen.HISTORY,
                    onClick = { viewModel.navigateTo(AppScreen.HISTORY) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (uiState.broadcasts.isNotEmpty()) {
                                    Badge(
                                        containerColor = primaryColor,
                                        contentColor = Color.White
                                    ) {
                                        Text("${uiState.broadcasts.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (uiState.currentScreen == AppScreen.HISTORY) {
                                    Icons.Default.History
                                } else {
                                    Icons.Outlined.History
                                },
                                contentDescription = "History"
                            )
                        }
                    },
                    label = {
                        Text(
                            text = "History",
                            fontWeight = if (uiState.currentScreen == AppScreen.HISTORY) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = navItemColors,
                    modifier = Modifier.testTag("nav_tab_history")
                )
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (uiState.currentScreen) {
                    AppScreen.HOME -> {
                        HomeScreen(
                            uiState = uiState,
                            onSendSmsClicked = { viewModel.navigateTo(AppScreen.SEND_SMS) },
                            onNavigateToContacts = { viewModel.navigateTo(AppScreen.CONTACTS) },
                            onNavigateToHistory = { viewModel.navigateTo(AppScreen.HISTORY) },
                            onPhotoPicked = { uri -> viewModel.updateOrganisationPhotoFromUri(uri) },
                            onResetPhoto = { viewModel.resetOrganisationPhotoToDefault() },
                            onSelectSenderId = { sender -> viewModel.selectActiveSenderId(sender) },
                            onAddSenderId = { senderName, candidateLabel, defaultGroup ->
                                viewModel.addSenderId(senderName, candidateLabel, defaultGroup)
                            },
                            onDeleteSenderId = { sender -> viewModel.deleteSenderId(sender) },
                            onSaveCustomOrganisation = { name, role, slogan, primaryHex, accentHex, photoPath ->
                                viewModel.saveOrganisationCustomization(
                                    name,
                                    role,
                                    slogan,
                                    primaryHex,
                                    accentHex,
                                    photoPath
                                )
                            }
                        )
                    }

                    AppScreen.CONTACTS -> {
                        ContactsScreen(
                            uiState = uiState,
                            onBackToHome = { viewModel.navigateTo(AppScreen.HOME) },
                            onAddManualContact = { name, phone, group ->
                                viewModel.addManualContact(name, phone, group)
                            },
                            onImportCsvUri = { uri ->
                                viewModel.importContactsFromCsvUri(context, uri)
                            },
                            onImportCsvText = { rawCsv ->
                                viewModel.importContactsFromCsvString(rawCsv)
                            },
                            onLoadSampleCsv = {
                                viewModel.loadSampleCampaignCsv()
                            },
                            onPickSystemContact = { uri ->
                                viewModel.importContactFromSystemUri(context, uri)
                            },
                            onToggleContactSelection = { id, selected ->
                                viewModel.toggleContactSelection(id, selected)
                            },
                            onSetAllContactsSelected = { selected ->
                                viewModel.setAllContactsSelected(selected)
                            },
                            onDeleteContact = { id ->
                                viewModel.deleteContact(id)
                            },
                            onClearAllContacts = {
                                viewModel.clearAllContacts()
                            },
                            onProceedToSendSms = {
                                viewModel.navigateTo(AppScreen.SEND_SMS)
                            }
                        )
                    }

                    AppScreen.SEND_SMS -> {
                        SendSmsScreen(
                            uiState = uiState,
                            isSmsOnlineGhConfigured = { viewModel.isSmsOnlineGhConfigured() },
                            hasSendSmsPermission = { viewModel.hasSendSmsPermission() },
                            isSimReady = { viewModel.isSimReady() },
                            onBackToHome = { viewModel.navigateTo(AppScreen.HOME) },
                            onSelectSenderId = { sender -> viewModel.selectActiveSenderId(sender) },
                            onAddSenderId = { senderName, candidateLabel, defaultGroup ->
                                viewModel.addSenderId(senderName, candidateLabel, defaultGroup)
                            },
                            onDeleteSenderId = { sender -> viewModel.deleteSenderId(sender) },
                            onSelectDelegatesByGroup = { groupTag ->
                                viewModel.selectDelegatesByGroup(groupTag)
                            },
                            onMessageChanged = { viewModel.updateMessageText(it) },
                            onSendModeChanged = { viewModel.updateSendMode(it) },
                            onToggleContactSelection = { id, selected ->
                                viewModel.toggleContactSelection(id, selected)
                            },
                            onSetAllContactsSelected = { selected ->
                                viewModel.setAllContactsSelected(selected)
                            },
                            onNavigateToContacts = { viewModel.navigateTo(AppScreen.CONTACTS) },
                            onLoadSampleContacts = { viewModel.loadSampleCampaignCsv() },
                            onConfirmAndSend = {
                                viewModel.executeConfirmedSend(
                                    activityContext = context,
                                    onCompleteNavigateToHistory = {
                                        viewModel.navigateTo(AppScreen.HISTORY)
                                    }
                                )
                            },
                            onPermissionDeniedMessage = { msg ->
                                viewModel.showMessage(msg)
                            }
                        )
                    }

                    AppScreen.HISTORY -> {
                        HistoryScreen(
                            uiState = uiState,
                            observeDeliveries = { broadcastId ->
                                viewModel.observeDeliveriesForBroadcast(broadcastId)
                            },
                            onBackToHome = { viewModel.navigateTo(AppScreen.HOME) },
                            onReuseMessage = { messageText ->
                                viewModel.updateMessageText(messageText)
                                viewModel.navigateTo(AppScreen.SEND_SMS)
                            },
                            onDeleteBroadcast = { broadcastId ->
                                viewModel.deleteBroadcast(broadcastId)
                            },
                            onGoToSendSms = {
                                viewModel.navigateTo(AppScreen.SEND_SMS)
                            }
                        )
                    }
                }
            }
        }
    }
}
