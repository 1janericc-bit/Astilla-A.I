package com.example.ui.training

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.KnowledgeCapsuleEntity
import com.example.ui.components.AstillaTopAppBar
import com.example.ui.theme.*

@Composable
fun TrainingScreen(
    viewModel: TrainingViewModel,
    onOpenSupport: () -> Unit,
    onAskInChat: (String) -> Unit
) {
    val capsules by viewModel.knowledgeCapsules.collectAsStateWithLifecycle()
    val isTraining by viewModel.isTraining.collectAsStateWithLifecycle()
    val latestResult by viewModel.latestResult.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var inputTitle by remember { mutableStateOf("") }
    var inputText by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            viewModel.trainFromFileUri(uri)
        }
    }

    Scaffold(
        topBar = {
            AstillaTopAppBar(
                title = "Knowledge Base & Training",
                subtitle = "Active Neural Learning",
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
            // Header Explanation Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Interactive Training",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Astilla A.I grows smarter by learning directly from your documents, PDFs, and notes. Once trained, Astilla retains key insights in active context and references them in conversations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Enthusiastic Training Success Banner
            if (latestResult != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("training_success_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AstillaEmeraldGreen.copy(alpha = 0.15f)
                        ),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = AstillaEmeraldGreen
                                    )
                                    Text(
                                        text = "Training Complete!",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AstillaEmeraldGreen
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.dismissResult() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                                }
                            }

                            Text(
                                text = latestResult?.acknowledgmentMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (!latestResult?.extractedKeyPoints.isNullOrEmpty()) {
                                Text(
                                    text = "Extracted Insights (${latestResult?.wordCount} words analyzed):",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                latestResult?.extractedKeyPoints?.take(3)?.forEach { point ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(start = 4.dp)
                                    ) {
                                        Text("•", color = AstillaEmeraldGreen, fontWeight = FontWeight.Bold)
                                        Text(text = point, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val t = latestResult?.title ?: "the trained document"
                                    viewModel.dismissResult()
                                    onAskInChat("Tell me about $t based on what you learned.")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AstillaEmeraldGreen)
                            ) {
                                Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ask Astilla About This Document")
                            }
                        }
                    }
                }
            }

            // Error banner if any
            if (errorMessage != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text(errorMessage ?: "", color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            IconButton(onClick = { viewModel.dismissError() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Input Form Card
            item {
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
                        Text(
                            text = "Provide Training Input",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        // Title field
                        OutlinedTextField(
                            value = inputTitle,
                            onValueChange = { inputTitle = it },
                            label = { Text("Document / Topic Title (optional)") },
                            placeholder = { Text("e.g., Quantum Physics Chapter 1, Company Handbook") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("training_title_input"),
                            singleLine = true
                        )

                        // Document text field
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            label = { Text("Document Text / Notes") },
                            placeholder = { Text("Paste document text, research, or book chapters here...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 220.dp)
                                .testTag("training_text_input"),
                            maxLines = 10
                        )

                        // Action buttons row: File picker and Train
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    docPickerLauncher.launch(
                                        arrayOf("application/pdf", "text/plain", "text/markdown", "text/csv")
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("upload_file_training_button"),
                                enabled = !isTraining
                            ) {
                                Icon(
                                    imageVector = Icons.Default.UploadFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import PDF/Doc", maxLines = 1)
                            }

                            Button(
                                onClick = {
                                    viewModel.trainFromText(inputTitle, inputText)
                                    inputTitle = ""
                                    inputText = ""
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("train_button"),
                                enabled = inputText.isNotBlank() && !isTraining
                            ) {
                                if (isTraining) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Learning...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Train Astilla")
                                }
                            }
                        }
                    }
                }
            }

            // Knowledge Base Header & Search
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Stored Knowledge (${capsules.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (capsules.isNotEmpty()) {
                            Text(
                                text = "Persisted Offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (capsules.isNotEmpty() || searchQuery.isNotEmpty()) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.searchQuery.value = it },
                            placeholder = { Text("Search learned knowledge...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_knowledge_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Knowledge Capsules List
            if (capsules.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "No Trained Knowledge Yet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Paste text or import a PDF above to expand Astilla A.I's brain.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(capsules, key = { it.id }) { capsule ->
                    KnowledgeCapsuleCard(
                        capsule = capsule,
                        onDelete = { viewModel.deleteCapsule(capsule.id) },
                        onAskAbout = {
                            onAskInChat("Based on what you learned from \"${capsule.title}\", summarize the core takeaways.")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun KnowledgeCapsuleCard(
    capsule: KnowledgeCapsuleEntity,
    onDelete: () -> Unit,
    onAskAbout: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("capsule_card_${capsule.id}"),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (capsule.sourceType == "PDF") AstillaAmberTertiary.copy(alpha = 0.2f)
                                else AstillaCyanPrimary.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (capsule.sourceType == "PDF") Icons.Default.PictureAsPdf else Icons.Default.Article,
                            contentDescription = null,
                            tint = if (capsule.sourceType == "PDF") AstillaAmberTertiary else AstillaCyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = capsule.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${capsule.sourceType} • ${capsule.wordCount} words",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand"
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = capsule.coreSummary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (capsule.extractedKeyPoints.isNotBlank()) {
                        Text(
                            text = "Extracted Key Points:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        capsule.extractedKeyPoints.lines().forEach { pt ->
                            if (pt.isNotBlank()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Text("•", color = MaterialTheme.colorScheme.primary)
                                    Text(text = pt, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = onAskAbout,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ask Astilla About This")
                    }
                }
            }
        }
    }
}
