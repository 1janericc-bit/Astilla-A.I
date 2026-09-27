package com.example.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entity.MemoryItemEntity
import com.example.ui.components.AstillaTopAppBar
import com.example.ui.components.SupportDeveloperCard
import com.example.ui.theme.AstillaAmberTertiary
import com.example.ui.theme.AstillaCyanPrimary
import com.example.ui.theme.AstillaEmeraldGreen
import com.example.ui.theme.ThemeMode

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenSupport: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val memoryEnabled by viewModel.memoryEnabled.collectAsStateWithLifecycle()
    val offlineOnly by viewModel.offlineOnly.collectAsStateWithLifecycle()
    val customApiKey by viewModel.customApiKey.collectAsStateWithLifecycle()
    val memoryItems by viewModel.memoryItems.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    var showAddMemoryDialog by remember { mutableStateOf(false) }
    var showEraseMemoryConfirm by remember { mutableStateOf(false) }
    var showWipeKnowledgeConfirm by remember { mutableStateOf(false) }
    var keyInputText by remember { mutableStateOf(customApiKey) }

    LaunchedEffect(customApiKey) {
        keyInputText = customApiKey
    }

    Scaffold(
        topBar = {
            AstillaTopAppBar(
                title = "App Settings & Memory",
                subtitle = "Preferences & Storage",
                isOffline = true,
                onOpenSupport = onOpenSupport
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Status banner
            if (statusBanner != null) {
                item {
                    Surface(
                        color = AstillaCyanPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = AstillaCyanPrimary)
                            Text(
                                text = statusBanner ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.dismissBanner() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Section 1: Appearance & Theme Customization
            item {
                SettingsSectionCard(
                    title = "Theme & Customization",
                    icon = Icons.Default.Palette
                ) {
                    Text(
                        text = "Customize the software's appearance directly to match your visual preference.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionChip(
                            label = "System",
                            selected = themeMode == ThemeMode.SYSTEM,
                            icon = Icons.Default.BrightnessAuto,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) }
                        )
                        ThemeOptionChip(
                            label = "Dark",
                            selected = themeMode == ThemeMode.DARK,
                            icon = Icons.Default.DarkMode,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setThemeMode(ThemeMode.DARK) }
                        )
                        ThemeOptionChip(
                            label = "Light",
                            selected = themeMode == ThemeMode.LIGHT,
                            icon = Icons.Default.LightMode,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }
                        )
                    }
                }
            }

            // Section 2: Memory Management & Context
            item {
                SettingsSectionCard(
                    title = "Context Memory Management",
                    icon = Icons.Default.Psychology
                ) {
                    Surface(
                        color = AstillaAmberTertiary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = AstillaAmberTertiary, modifier = Modifier.size(20.dp))
                            Text(
                                text = "Astilla A.I maintains active context memory across conversations. All stored context and memory can be reset or erased anytime here.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Master Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active Context Memory",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (memoryEnabled) "Preserving conversation context & facts" else "Memory paused",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = memoryEnabled,
                            onCheckedChange = { viewModel.setMemoryEnabled(it) },
                            modifier = Modifier.testTag("memory_enabled_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Stored memory items
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Stored Memories (${memoryItems.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = { showAddMemoryDialog = true },
                            modifier = Modifier.testTag("add_memory_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Fact")
                        }
                    }

                    if (memoryItems.isEmpty()) {
                        Text(
                            text = "No custom memories stored yet. Astilla remembers facts as you chat, or you can add them manually above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            memoryItems.forEach { item ->
                                MemoryItemRow(
                                    item = item,
                                    onToggle = { viewModel.toggleMemoryActive(item.id, item.isActive) },
                                    onDelete = { viewModel.deleteMemory(item.id) }
                                )
                            }
                        }
                    }

                    // Reset buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEraseMemoryConfirm = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("erase_all_memory_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Erase Memory", maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = { showWipeKnowledgeConfirm = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("wipe_knowledge_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Wipe Knowledge", maxLines = 1)
                        }
                    }
                }
            }

            // Section 3: Offline & Engine Configuration
            item {
                SettingsSectionCard(
                    title = "Engine & Intelligence",
                    icon = Icons.Default.Memory
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Strictly Offline Mode",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Execute 100% on-device neural heuristics without cloud fallback",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = offlineOnly,
                            onCheckedChange = { viewModel.setOfflineOnly(it) }
                        )
                    }

                    OutlinedTextField(
                        value = keyInputText,
                        onValueChange = {
                            keyInputText = it
                            viewModel.setCustomApiKey(it)
                        },
                        label = { Text("Gemini Cloud API Key (Optional Hybrid Assist)") },
                        placeholder = { Text("AI Studio automatically provides or enter custom key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (keyInputText.isNotBlank()) {
                                Icon(Icons.Default.Check, contentDescription = "Active", tint = AstillaEmeraldGreen)
                            }
                        }
                    )
                }
            }

            // Section 4: Developer Support (Astilla Softwares)
            item {
                Text(
                    text = "Developer Support",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item {
                SupportDeveloperCard()
            }

            // Section 5: About Astilla Softwares
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.astilla_app_logo_1790505394647),
                            contentDescription = "Astilla A.I Logo",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Astilla A.I",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Astilla Softwares • v1.0.0",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Built for privacy, zero subscription fees, and complete offline autonomy.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog: Add Custom Memory
    if (showAddMemoryDialog) {
        var newKey by remember { mutableStateOf("") }
        var newValue by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddMemoryDialog = false },
            title = { Text("Add Memory Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Add a fact, preference, or project context for Astilla to remember:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        label = { Text("Key / Subject") },
                        placeholder = { Text("e.g., Name, Preferred Framework, Project") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        label = { Text("Value / Details") },
                        placeholder = { Text("e.g., Eric, Kotlin Compose, Astilla") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newKey.isNotBlank() && newValue.isNotBlank()) {
                            viewModel.addCustomMemory(newKey, newValue)
                            showAddMemoryDialog = false
                        }
                    },
                    enabled = newKey.isNotBlank() && newValue.isNotBlank()
                ) {
                    Text("Save Memory")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Confirm Erase All Memory
    if (showEraseMemoryConfirm) {
        AlertDialog(
            onDismissRequest = { showEraseMemoryConfirm = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Erase All Context Memory?") },
            text = {
                Text("This will immediately delete all stored personal facts, preferences, and active conversation context memory. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eraseAllMemoryAndContext()
                        showEraseMemoryConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Erase Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEraseMemoryConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Confirm Wipe Knowledge Base
    if (showWipeKnowledgeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeKnowledgeConfirm = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Wipe All Trained Knowledge?") },
            text = {
                Text("This will remove all documents, books, and articles trained into Astilla A.I. The assistant will revert to its baseline state.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eraseAllKnowledge()
                        showWipeKnowledgeConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Wipe Knowledge")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeKnowledgeConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
fun ThemeOptionChip(
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (selected) CardDefaults.outlinedCardBorder() else null
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MemoryItemRow(
    item: MemoryItemEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.key,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = item.value,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (item.isActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle",
                        tint = if (item.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
