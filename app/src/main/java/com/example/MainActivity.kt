package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ComposeScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.DawuroTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.CampaignViewModel

/** Lets any screen show a snackbar, including ones with an action like "Undo". */
val LocalSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }

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

            DawuroTheme(
                primaryColorHex = uiState.profile.primaryColorHex,
                accentColorHex = uiState.profile.accentColorHex
            ) {
                DawuroApp(viewModel = viewModel)
            }
        }
    }
}

private data class TabItem(
    val screen: AppScreen,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
    val testTag: String
)

private val tabs = listOf(
    TabItem(AppScreen.HOME, "Home", Icons.Rounded.Home, Icons.Outlined.Home, "nav_tab_home"),
    TabItem(AppScreen.CONTACTS, "Contacts", Icons.Rounded.People, Icons.Outlined.People, "nav_tab_contacts"),
    TabItem(AppScreen.HISTORY, "Messages", Icons.AutoMirrored.Rounded.Chat, Icons.AutoMirrored.Outlined.Chat, "nav_tab_history"),
    TabItem(AppScreen.SETTINGS, "Settings", Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_tab_settings")
)

@Composable
fun DawuroApp(viewModel: CampaignViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = event.actionLabel,
                withDismissAction = event.actionLabel == null,
                duration = if (event.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) event.action?.invoke()
        }
    }

    if (!uiState.isLoaded) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }
        return
    }

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        if (uiState.showWelcome) {
            WelcomeScreen(onContinue = viewModel::completeWelcome)
            return@CompositionLocalProvider
        }

        BackHandler(enabled = uiState.currentScreen != AppScreen.HOME || uiState.openBroadcastId != null) {
            viewModel.navigateBack()
        }

        val showTabs = uiState.currentScreen != AppScreen.COMPOSE

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showTabs) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            val selected = uiState.currentScreen == tab.screen
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.navigateTo(tab.screen) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.icon,
                                        contentDescription = null
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = uiState.currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen",
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            ) { screen ->
                ScreenContent(screen = screen, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun ScreenContent(screen: AppScreen, viewModel: CampaignViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    when (screen) {
        AppScreen.HOME -> HomeScreen(
            uiState = uiState,
            onNewMessage = viewModel::openCompose,
            onOpenContacts = { viewModel.navigateTo(AppScreen.CONTACTS) },
            onOpenHistory = { viewModel.navigateTo(AppScreen.HISTORY) },
            onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) },
            onOpenBroadcast = { id ->
                viewModel.navigateTo(AppScreen.HISTORY)
                viewModel.openBroadcast(id)
            }
        )

        AppScreen.CONTACTS -> ContactsScreen(
            uiState = uiState,
            onAddContact = viewModel::addManualContact,
            onUpdateContact = viewModel::updateContact,
            onDeleteContact = viewModel::deleteContact,
            onClearAllContacts = viewModel::clearAllContacts,
            onPickedPhonebookContact = { uri -> viewModel.importContactFromPhonebook(context, uri) },
            onPickedCsvFile = { uri -> viewModel.previewCsvFromUri(context, uri) },
            onPastedCsv = viewModel::previewCsvText,
            onConfirmImport = viewModel::confirmImport,
            onCancelImport = viewModel::cancelImport,
            onNewMessage = viewModel::openCompose
        )

        AppScreen.COMPOSE -> ComposeScreen(
            uiState = uiState,
            hasSendSmsPermission = viewModel::hasSendSmsPermission,
            isSimReady = viewModel::isSimReady,
            onBack = { viewModel.navigateBack() },
            onMessageChanged = viewModel::updateMessageText,
            onApplyTemplate = viewModel::applyTemplate,
            onSendModeChanged = viewModel::updateSendMode,
            onSelectSenderId = viewModel::selectActiveSenderId,
            onToggleContact = viewModel::toggleContactSelection,
            onSetAllSelected = viewModel::setAllContactsSelected,
            onToggleGroup = viewModel::toggleGroupSelection,
            onOpenContacts = { viewModel.navigateTo(AppScreen.CONTACTS) },
            onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) },
            onConfirmSend = { viewModel.executeConfirmedSend(context) }
        )

        AppScreen.HISTORY -> HistoryScreen(
            uiState = uiState,
            observeDeliveries = viewModel::observeDeliveriesForBroadcast,
            onOpenBroadcast = viewModel::openBroadcast,
            onReuseMessage = viewModel::reuseMessage,
            onRetryFailed = { id -> viewModel.retryFailed(id, context) },
            onDeleteBroadcast = viewModel::deleteBroadcast,
            onNewMessage = viewModel::openCompose,
            hasSendSmsPermission = viewModel::hasSendSmsPermission
        )

        AppScreen.SETTINGS -> SettingsScreen(
            uiState = uiState,
            onSaveProfile = viewModel::saveProfile,
            onPickPhoto = viewModel::updateProfilePhoto,
            onRemovePhoto = viewModel::removeProfilePhoto,
            onSetColours = viewModel::setColours,
            onSaveApiKey = viewModel::saveApiKey,
            onAddSenderId = viewModel::addSenderId,
            onSelectSenderId = viewModel::selectActiveSenderId,
            onDeleteSenderId = viewModel::deleteSenderId,
            onClearAllContacts = viewModel::clearAllContacts
        )
    }
}
