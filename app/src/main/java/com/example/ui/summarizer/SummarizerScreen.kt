package com.example.ui.summarizer

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.DocumentSummaryEntity
import com.example.data.model.SummaryMode
import com.example.ui.components.AstillaTopAppBar
import com.example.ui.components.copyToClipboard
import com.example.ui.theme.AstillaCyanPrimary
import com.example.ui.theme.AstillaEmeraldGreen
import com.example.ui.theme.AstillaVioletSecondary

@Composable
fun SummarizerScreen(
    viewModel: SummarizerViewModel,
    onOpenSupport: () -> Unit
) {
    val currentMode by viewModel.currentMode.collectAsStateWithLifecycle()
    val docTitle by viewModel.documentTitle.collectAsStateWithLifecycle()
    val docText by viewModel.documentText.collectAsStateWithLifecycle()
    val isSummarizing by viewModel.isSummarizing.collectAsStateWithLifecycle()
    val generatedSummary by viewModel.generatedSummary.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val savedSummaries by viewModel.savedSummaries.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Summarizer, 1: Saved Library
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadFromFileUri(uri)
        }
    }

    val wordCount = remember(docText) {
        if (docText.isBlank()) 0 else docText.trim().split("\\s+".toRegex()).size
    }

    Scaffold(
        topBar = {
            AstillaTopAppBar(
                title = "Book & Doc Summarizer",
                subtitle = "Deep Structured Analysis",
                isOffline = true,
                onOpenSupport = onOpenSupport
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Generate Summary", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Saved Library (${savedSummaries.size})", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Bookmarks, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            // Notification / Status banner
            AnimatedVisibility(visible = statusMessage != null) {
                Surface(
                    color = AstillaCyanPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = AstillaCyanPrimary)
                        Text(
                            text = statusMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.dismissStatus() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (activeTab == 0) {
                // Tab 0: Generator
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    // Mode Selector Chips
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Select Summary Style",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(SummaryMode.values()) { mode ->
                                    FilterChip(
                                        selected = currentMode == mode,
                                        onClick = { viewModel.currentMode.value = mode },
                                        label = { Text(mode.displayName) },
                                        leadingIcon = {
                                            if (currentMode == mode) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    )
                                }
                            }
                            Text(
                                text = currentMode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Input Form
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = docTitle,
                                    onValueChange = { viewModel.documentTitle.value = it },
                                    label = { Text("Book / Document Title") },
                                    placeholder = { Text("e.g. Atomic Habits, Project Architecture") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("summary_title_input"),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = docText,
                                    onValueChange = { viewModel.documentText.value = it },
                                    label = { Text("Book / Document Text") },
                                    placeholder = { Text("Paste document text, articles, or chapters to analyze...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 120.dp, max = 200.dp)
                                        .testTag("summary_text_input"),
                                    maxLines = 8
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$wordCount words loaded",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            filePickerLauncher.launch(
                                                arrayOf("application/pdf", "text/plain", "text/markdown", "text/csv")
                                            )
                                        },
                                        modifier = Modifier.testTag("load_doc_button")
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Import PDF / Doc")
                                    }
                                }

                                Button(
                                    onClick = { viewModel.generateSummary() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("generate_summary_button"),
                                    enabled = docText.isNotBlank() && !isSummarizing
                                ) {
                                    if (isSummarizing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Synthesizing Summary...")
                                    } else {
                                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Generate ${currentMode.displayName}")
                                    }
                                }
                            }
                        }
                    }

                    // Generated Result Section
                    if (generatedSummary != null) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("generated_summary_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Synthesized Analysis",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(AstillaEmeraldGreen.copy(alpha = 0.2f))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "COMPLETED",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = AstillaEmeraldGreen
                                            )
                                        }
                                    }

                                    SelectionContainer {
                                        Text(
                                            text = generatedSummary ?: "",
                                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                                        )
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                                    // Action buttons for summary
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = {
                                                copyToClipboard(context, generatedSummary ?: "", "Summary copied!")
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copy")
                                        }

                                        FilledTonalButton(
                                            onClick = {
                                                val sendIntent: Intent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, generatedSummary ?: "")
                                                    type = "text/plain"
                                                }
                                                val shareIntent = Intent.createChooser(sendIntent, "Share Summary")
                                                context.startActivity(shareIntent)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Share")
                                        }

                                        Button(
                                            onClick = { viewModel.saveCurrentSummary() },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Save")
                                        }
                                    }

                                    // Train button: one-click assimilate into Astilla's brain
                                    OutlinedButton(
                                        onClick = { viewModel.trainAstillaWithThisDoc() },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("🧠 Train Astilla A.I With This Entire Document")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Tab 1: Saved Library
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    if (savedSummaries.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Bookmarks,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "No Saved Summaries Yet",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Summaries you save will be permanently preserved here on your device.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(savedSummaries, key = { it.id }) { summary ->
                            SavedSummaryCard(
                                item = summary,
                                onDelete = { viewModel.deleteSavedSummary(summary.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavedSummaryCard(
    item: DocumentSummaryEntity,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Mode: ${item.summaryMode} • ${item.summaryWordCount} words summary",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.summaryText,
                style = MaterialTheme.typography.bodySmall,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(visible = expanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { copyToClipboard(context, item.summaryText, "Summary copied!") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy")
                    }
                    FilledTonalButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, item.summaryText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Summary"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }
                }
            }
        }
    }
}
