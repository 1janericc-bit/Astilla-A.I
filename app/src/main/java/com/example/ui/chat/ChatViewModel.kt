package com.example.ui.chat

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AstillaBrainEngine
import com.example.ai.GeminiCloudService
import com.example.ai.PdfDocumentExtractor
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.KnowledgeCapsuleEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val chatDao = db.chatDao()
    private val knowledgeDao = db.knowledgeDao()
    private val memoryDao = db.memoryDao()
    private val prefs = UserPreferencesRepository(application)

    val messages: StateFlow<List<ChatMessageEntity>> = chatDao.getAllMessages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _trainingBanner = MutableStateFlow<String?>(null)
    val trainingBanner: StateFlow<String?> = _trainingBanner.asStateFlow()

    init {
        // Seed initial friendly message if chat is empty
        viewModelScope.launch {
            try {
                val existing = chatDao.getAllMessages().first()
                if (existing.isEmpty()) {
                    chatDao.insertMessage(
                        ChatMessageEntity(
                            role = "assistant",
                            content = "Hello! I am **Astilla A.I**, your free, intelligent, and offline-first personal assistant developed by **Astilla Softwares**.\n\n" +
                                    "I have out-of-the-box conversational intelligence and grow smarter through your training documents and context memory. How can I assist you today?",
                            isOffline = true
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error checking/seeding initial message", e)
            }
        }
    }

    fun dismissTrainingBanner() {
        _trainingBanner.value = null
    }

    fun clearChat() {
        viewModelScope.launch {
            chatDao.clearAllMessages()
            chatDao.insertMessage(
                ChatMessageEntity(
                    role = "assistant",
                    content = "Chat cleared. Astilla A.I is ready for a fresh conversation! All your trained knowledge and memory capsules remain preserved.",
                    isOffline = true
                )
            )
        }
    }

    fun sendMessage(userText: String, attachedDocName: String? = null) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || _isGenerating.value) return

        viewModelScope.launch {
            // Save user message
            chatDao.insertMessage(
                ChatMessageEntity(
                    role = "user",
                    content = trimmed,
                    attachedDocName = attachedDocName
                )
            )

            _isGenerating.value = true

            val isOfflineOnly = prefs.offlineOnly.first()
            val customApiKey = prefs.customApiKey.first()
            val isMemoryEnabled = prefs.memoryEnabled.first()
            val effectiveApiKey = GeminiCloudService.getEffectiveApiKey(customApiKey)

            // Check if hybrid cloud mode can be attempted
            var assistantReply: String? = null
            var usedOffline = true

            if (!isOfflineOnly && effectiveApiKey.isNotBlank()) {
                try {
                    // Collect recent context
                    val history = chatDao.getAllMessages().first().takeLast(6).map { it.role to it.content }
                    val activeMemories = if (isMemoryEnabled) memoryDao.getActiveMemoriesSnapshot() else emptyList()
                    val relevantCapsules = knowledgeDao.getAllKnowledgeCapsulesSnapshot()
                    val memoriesPrompt = if (activeMemories.isNotEmpty()) {
                        "\nActive User Memory: " + activeMemories.joinToString("; ") { "${it.key}: ${it.value}" }
                    } else ""
                    val knowledgePrompt = if (relevantCapsules.isNotEmpty()) {
                        "\nTrained Knowledge Capsules Available: " + relevantCapsules.take(3).joinToString("; ") { it.title + ": " + it.coreSummary.take(150) }
                    } else ""

                    val systemWithContext = AstillaBrainEngine.SYSTEM_PROMPT + memoriesPrompt + knowledgePrompt

                    val cloudResult = GeminiCloudService.generateResponse(
                        apiKey = effectiveApiKey,
                        prompt = trimmed,
                        systemInstruction = systemWithContext,
                        chatHistory = history
                    )

                    if (cloudResult.isSuccess) {
                        assistantReply = cloudResult.getOrNull()
                        usedOffline = false
                    }
                } catch (e: Exception) {
                    // Fall back cleanly to offline engine
                    assistantReply = null
                }
            }

            // Fallback to offline native engine
            if (assistantReply == null) {
                assistantReply = AstillaBrainEngine.generateOfflineResponse(
                    prompt = trimmed,
                    knowledgeDao = knowledgeDao,
                    memoryDao = memoryDao,
                    isMemoryEnabled = isMemoryEnabled
                )
                usedOffline = true
            }

            // Insert assistant response
            chatDao.insertMessage(
                ChatMessageEntity(
                    role = "assistant",
                    content = assistantReply,
                    isOffline = usedOffline
                )
            )

            _isGenerating.value = false
        }
    }

    fun trainFromUri(uri: Uri) {
        viewModelScope.launch {
            _isGenerating.value = true
            val context = getApplication<Application>().applicationContext
            val extractResult = PdfDocumentExtractor.extractFromUri(context, uri)

            if (extractResult.isSuccess) {
                val doc = extractResult.getOrThrow()
                val training = AstillaBrainEngine.trainOnContent(
                    title = doc.fileName,
                    sourceName = doc.fileName,
                    sourceType = if (doc.mimeType.contains("pdf")) "PDF" else "DOC",
                    rawText = doc.text
                )

                // Save to Room DB
                val capsuleId = knowledgeDao.insertCapsule(
                    KnowledgeCapsuleEntity(
                        title = training.title,
                        sourceType = training.sourceType,
                        sourceName = training.sourceName,
                        rawText = training.rawText,
                        coreSummary = training.coreSummary,
                        extractedKeyPoints = training.extractedKeyPoints.joinToString("\n"),
                        wordCount = training.wordCount
                    )
                )

                _trainingBanner.value = training.acknowledgmentMessage

                // Add to chat conversation
                chatDao.insertMessage(
                    ChatMessageEntity(
                        role = "user",
                        content = "I have uploaded a document to train you: \"${training.title}\"",
                        attachedDocName = doc.fileName
                    )
                )

                chatDao.insertMessage(
                    ChatMessageEntity(
                        role = "assistant",
                        content = "${training.acknowledgmentMessage}\n\n" +
                                "### Key Takeaways Extracted:\n" +
                                training.extractedKeyPoints.take(4).joinToString("\n") { "• $it" } +
                                "\n\nFeel free to ask me questions or request a full summary!",
                        isOffline = true,
                        isTrainingAcknowledge = true
                    )
                )
            } else {
                val errorMsg = extractResult.exceptionOrNull()?.message ?: "Unable to read document"
                chatDao.insertMessage(
                    ChatMessageEntity(
                        role = "assistant",
                        content = "Sorry, I encountered an issue reading that document: $errorMsg. Please make sure the file contains accessible text.",
                        isOffline = true
                    )
                )
            }
            _isGenerating.value = false
        }
    }
}
