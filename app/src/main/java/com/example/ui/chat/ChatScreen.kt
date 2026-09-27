package com.example.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ai.AstillaBrainEngine
import com.example.data.local.entity.ChatMessageEntity
import com.example.ui.components.AstillaTopAppBar
import com.example.ui.components.copyToClipboard
import com.example.ui.theme.AstillaAmberTertiary
import com.example.ui.theme.AstillaCyanPrimary
import com.example.ui.theme.AstillaEmeraldGreen
import com.example.ui.theme.AstillaVioletSecondary

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onOpenSupport: () -> Unit,
    onNavigateToSummarizer: () -> Unit,
    onNavigateToTraining: () -> Unit
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val trainingBanner by viewModel.trainingBanner.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Document file picker for instant training from chat
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.trainFromUri(uri)
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            AstillaTopAppBar(
                title = "Astilla A.I",
                subtitle = "Offline Assistant",
                isOffline = true,
                onOpenSupport = onOpenSupport,
                actions = {
                    IconButton(
                        onClick = { viewModel.clearChat() },
                        modifier = Modifier.testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteSweep,
                            contentDescription = "Clear Chat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // Enthusiastic Training notification banner
            AnimatedVisibility(visible = trainingBanner != null) {
                Surface(
                    color = AstillaEmeraldGreen.copy(alpha = 0.15f),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AstillaEmeraldGreen
                        )
                        Text(
                            text = trainingBanner ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.dismissTrainingBanner() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatMessageBubble(
                        message = message,
                        onCopy = {
                            copyToClipboard(context, message.content, "Message copied to clipboard")
                        },
                        onSupportClicked = onOpenSupport
                    )
                }

                if (isGenerating) {
                    item {
                        AssistantThinkingIndicator()
                    }
                }
            }

            // Suggestion Chips (when user is at bottom or starting)
            SuggestionChipsRow(
                onPromptSelected = { prompt ->
                    inputText = prompt
                    viewModel.sendMessage(prompt)
                    inputText = ""
                },
                onSummarizerSelected = onNavigateToSummarizer,
                onTrainSelected = onNavigateToTraining
            )

            // Input Bar
            ChatInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        val textToSend = inputText
                        inputText = ""
                        viewModel.sendMessage(textToSend)
                    }
                },
                onAttachDoc = {
                    docPickerLauncher.launch(
                        arrayOf("application/pdf", "text/plain", "text/markdown", "text/csv")
                    )
                },
                isGenerating = isGenerating
            )
        }
    }
}

@Composable
fun SuggestionChipsRow(
    onPromptSelected: (String) -> Unit,
    onSummarizerSelected: () -> Unit,
    onTrainSelected: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SuggestionChip(
                onClick = onTrainSelected,
                label = { Text("🧠 Train Astilla") },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = AstillaCyanPrimary.copy(alpha = 0.12f)
                ),
                icon = {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = AstillaCyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
        item {
            SuggestionChip(
                onClick = onSummarizerSelected,
                label = { Text("📚 Summarize Doc") },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = AstillaVioletSecondary.copy(alpha = 0.12f)
                ),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = AstillaVioletSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
        item {
            SuggestionChip(
                onClick = { onPromptSelected("How can I support Astilla Softwares?") },
                label = { Text("💝 Support Developer") },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = AstillaAmberTertiary.copy(alpha = 0.12f)
                ),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = AstillaAmberTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
        item {
            SuggestionChip(
                onClick = { onPromptSelected("What can you do offline?") },
                label = { Text("⚡ Offline Abilities") }
            )
        }
        item {
            SuggestionChip(
                onClick = { onPromptSelected("What do you remember about me?") },
                label = { Text("💾 Context Memory") }
            )
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessageEntity,
    onCopy: () -> Unit,
    onSupportClicked: () -> Unit
) {
    val isUser = message.role == "user"
    val context = LocalContext.current
    val hasSupportInfo = message.content.contains("09273352516") || message.content.contains("09193710317")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(if (isUser) "user_message_bubble" else "assistant_message_bubble"),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Label for Assistant
        if (!isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 6.dp, bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.astilla_app_logo_1790505394647),
                        contentDescription = "Astilla A.I",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Text(
                    text = "Astilla A.I",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (message.isOffline) "• Offline Brain" else "• Hybrid Cloud",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (message.attachedDocName != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = message.attachedDocName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                SelectionContainer {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = if (isUser) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                // If this is a support reply, provide quick payment buttons right in the bubble!
                if (hasSupportInfo) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                copyToClipboard(context, AstillaBrainEngine.MAYA_NUMBER, "Copied Maya Number: ${AstillaBrainEngine.MAYA_NUMBER}")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF))
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Maya: ${AstillaBrainEngine.MAYA_NUMBER}", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = {
                                copyToClipboard(context, AstillaBrainEngine.GCASH_NUMBER, "Copied GCash Number: ${AstillaBrainEngine.GCASH_NUMBER}")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007DFE))
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy GCash: ${AstillaBrainEngine.GCASH_NUMBER}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Bottom actions for bubble
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy message",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AssistantThinkingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Astilla A.I is analyzing context...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
        )
    }
}

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachDoc: () -> Unit,
    isGenerating: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = onAttachDoc,
                enabled = !isGenerating,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("attach_doc_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PostAdd,
                    contentDescription = "Train from Doc or PDF",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            TextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = {
                    Text(
                        "Ask, summarize, or train Astilla A.I...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                maxLines = 4
            )

            IconButton(
                onClick = onSend,
                enabled = text.isNotBlank() && !isGenerating,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (text.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .testTag("send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (text.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
