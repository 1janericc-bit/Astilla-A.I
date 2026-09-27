package com.example.ui.training

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AstillaBrainEngine
import com.example.ai.PdfDocumentExtractor
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.KnowledgeCapsuleEntity
import com.example.data.model.TrainingResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrainingViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val knowledgeDao = db.knowledgeDao()
    private val chatDao = db.chatDao()

    val searchQuery = MutableStateFlow("")

    val knowledgeCapsules: StateFlow<List<KnowledgeCapsuleEntity>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                knowledgeDao.getAllKnowledgeCapsules()
            } else {
                knowledgeDao.searchKnowledge(query.trim())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isTraining = MutableStateFlow(false)
    val isTraining: StateFlow<Boolean> = _isTraining.asStateFlow()

    private val _latestResult = MutableStateFlow<TrainingResult?>(null)
    val latestResult: StateFlow<TrainingResult?> = _latestResult.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun dismissResult() {
        _latestResult.value = null
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun trainFromText(title: String, text: String, sourceType: String = "TEXT") {
        if (text.isBlank()) {
            _errorMessage.value = "Please provide some text or document content to train Astilla A.I."
            return
        }

        viewModelScope.launch {
            _isTraining.value = true
            try {
                val resolvedTitle = if (title.isNotBlank()) title else "Custom Input"
                val result = AstillaBrainEngine.trainOnContent(
                    title = resolvedTitle,
                    sourceName = "Direct Input",
                    sourceType = sourceType,
                    rawText = text
                )

                // Save to Room DB
                knowledgeDao.insertCapsule(
                    KnowledgeCapsuleEntity(
                        title = result.title,
                        sourceType = result.sourceType,
                        sourceName = result.sourceName,
                        rawText = result.rawText,
                        coreSummary = result.coreSummary,
                        extractedKeyPoints = result.extractedKeyPoints.joinToString("\n"),
                        wordCount = result.wordCount
                    )
                )

                // Record in chat as well
                chatDao.insertMessage(
                    ChatMessageEntity(
                        role = "assistant",
                        content = "${result.acknowledgmentMessage}\n\n" +
                                "I have assimilated this into my active context. You can ask me questions about **${result.title}** at any time!",
                        isOffline = true,
                        isTrainingAcknowledge = true
                    )
                )

                _latestResult.value = result
            } catch (e: Exception) {
                _errorMessage.value = "Failed to train: ${e.message}"
            } finally {
                _isTraining.value = false
            }
        }
    }

    fun trainFromFileUri(uri: Uri) {
        viewModelScope.launch {
            _isTraining.value = true
            try {
                val context = getApplication<Application>().applicationContext
                val extraction = PdfDocumentExtractor.extractFromUri(context, uri)

                if (extraction.isSuccess) {
                    val doc = extraction.getOrThrow()
                    val result = AstillaBrainEngine.trainOnContent(
                        title = doc.fileName,
                        sourceName = doc.fileName,
                        sourceType = if (doc.mimeType.contains("pdf")) "PDF" else "DOC",
                        rawText = doc.text
                    )

                    knowledgeDao.insertCapsule(
                        KnowledgeCapsuleEntity(
                            title = result.title,
                            sourceType = result.sourceType,
                            sourceName = result.sourceName,
                            rawText = result.rawText,
                            coreSummary = result.coreSummary,
                            extractedKeyPoints = result.extractedKeyPoints.joinToString("\n"),
                            wordCount = result.wordCount
                        )
                    )

                    chatDao.insertMessage(
                        ChatMessageEntity(
                            role = "assistant",
                            content = "${result.acknowledgmentMessage}\n\n" +
                                    "I have updated my local intelligence with ${result.wordCount} words from **${result.title}**.",
                            isOffline = true,
                            isTrainingAcknowledge = true
                        )
                    )

                    _latestResult.value = result
                } else {
                    _errorMessage.value = "Extraction failed: ${extraction.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load document: ${e.message}"
            } finally {
                _isTraining.value = false
            }
        }
    }

    fun deleteCapsule(id: Long) {
        viewModelScope.launch {
            knowledgeDao.deleteCapsuleById(id)
        }
    }

    fun clearAllKnowledge() {
        viewModelScope.launch {
            knowledgeDao.clearAllKnowledge()
        }
    }
}
