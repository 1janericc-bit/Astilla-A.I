package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.UserPreferencesRepository
import com.example.ui.chat.ChatScreen
import com.example.ui.chat.ChatViewModel
import com.example.ui.components.SupportAstillaDialog
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.summarizer.SummarizerScreen
import com.example.ui.summarizer.SummarizerViewModel
import com.example.ui.support.SupportDeveloperScreen
import com.example.ui.theme.AstillaAITheme
import com.example.ui.theme.ThemeMode
import com.example.ui.training.TrainingScreen
import com.example.ui.training.TrainingViewModel

enum class MainDestination(
    val route: String,
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    CHAT("chat", "Chat", Icons.Default.Chat, Icons.Outlined.Chat),
    TRAINING("training", "Training", Icons.Default.School, Icons.Outlined.School),
    SUMMARIZER("summarizer", "Summarizer", Icons.Default.MenuBook, Icons.Outlined.MenuBook),
    SETTINGS("settings", "Settings", Icons.Default.Settings, Icons.Outlined.Settings),
    SUPPORT("support", "Support", Icons.Default.VolunteerActivism, Icons.Outlined.VolunteerActivism)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val userPrefs = remember { UserPreferencesRepository(applicationContext) }
            val themeMode by userPrefs.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)

            AstillaAITheme(themeMode = themeMode) {
                AstillaAppMain()
            }
        }
    }
}

@Composable
fun AstillaAppMain() {
    var currentDestination by remember { mutableStateOf(MainDestination.CHAT) }
    var showSupportDialog by remember { mutableStateOf(false) }

    val chatViewModel: ChatViewModel = viewModel()
    val trainingViewModel: TrainingViewModel = viewModel()
    val summarizerViewModel: SummarizerViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    // Handle back button when not on Chat screen
    if (currentDestination != MainDestination.CHAT) {
        BackHandler {
            currentDestination = MainDestination.CHAT
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                listOf(
                    MainDestination.CHAT,
                    MainDestination.TRAINING,
                    MainDestination.SUMMARIZER,
                    MainDestination.SETTINGS,
                    MainDestination.SUPPORT
                ).forEach { destination ->
                    val isSelected = currentDestination == destination
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = destination.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text(destination.title, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("nav_item_${destination.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentDestination) {
                MainDestination.CHAT -> {
                    ChatScreen(
                        viewModel = chatViewModel,
                        onOpenSupport = { showSupportDialog = true },
                        onNavigateToSummarizer = { currentDestination = MainDestination.SUMMARIZER },
                        onNavigateToTraining = { currentDestination = MainDestination.TRAINING }
                    )
                }
                MainDestination.TRAINING -> {
                    TrainingScreen(
                        viewModel = trainingViewModel,
                        onOpenSupport = { showSupportDialog = true },
                        onAskInChat = { prompt ->
                            chatViewModel.sendMessage(prompt)
                            currentDestination = MainDestination.CHAT
                        }
                    )
                }
                MainDestination.SUMMARIZER -> {
                    SummarizerScreen(
                        viewModel = summarizerViewModel,
                        onOpenSupport = { showSupportDialog = true }
                    )
                }
                MainDestination.SETTINGS -> {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onOpenSupport = { showSupportDialog = true }
                    )
                }
                MainDestination.SUPPORT -> {
                    SupportDeveloperScreen(
                        onNavigateBack = { currentDestination = MainDestination.CHAT }
                    )
                }
            }
        }
    }

    if (showSupportDialog) {
        SupportAstillaDialog(
            onDismiss = { showSupportDialog = false }
        )
    }
}
