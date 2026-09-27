package com.example.ui.summarizer

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AstillaBrainEngine
import com.example.ai.PdfDocumentExtractor
import com.example.data.local.AppDatabase
import com.example.data.local.entity.DocumentSummaryEntity
import com.example.data.local.entity.KnowledgeCapsuleEntity
import com.example.data.model.SummaryMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SummarizerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val summaryDao = db.documentSummaryDao()
    private val knowledgeDao = db.knowledgeDao()

    val savedSummaries: StateFlow<List<DocumentSummaryEntity>> = summaryDao.getAllSummaries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentMode = MutableStateFlow(SummaryMode.EXECUTIVE)
    val documentTitle = MutableStateFlow("")
    val documentText = MutableStateFlow("")

    private val _isSummarizing = MutableStateFlow(false)
    val isSummarizing: StateFlow<Boolean> = _isSummarizing.asStateFlow()

    private val _generatedSummary = MutableStateFlow<String?>(null)
    val generatedSummary: StateFlow<String?> = _generatedSummary.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun dismissStatus() {
        _statusMessage.value = null
    }

    fun loadFromFileUri(uri: Uri) {
        viewModelScope.launch {
            _isSummarizing.value = true
            try {
                val context = getApplication<Application>().applicationContext
                val extraction = PdfDocumentExtractor.extractFromUri(context, uri)

                if (extraction.isSuccess) {
                    val doc = extraction.getOrThrow()
                    documentTitle.value = doc.fileName
                    documentText.value = doc.text
                    _statusMessage.value = "Loaded \"${doc.fileName}\" (${doc.wordCount} words ready to summarize)"
                } else {
                    _statusMessage.value = "Failed to read file: ${extraction.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error: ${e.message}"
            } finally {
                _isSummarizing.value = false
            }
        }
    }

    fun generateSummary() {
        val text = documentText.value.trim()
        if (text.isBlank()) {
            _statusMessage.value = "Please provide text or load a document first."
            return
        }

        viewModelScope.launch {
            _isSummarizing.value = true
            try {
                val title = if (documentTitle.value.isNotBlank()) documentTitle.value else "Document Analysis"
                val summary = AstillaBrainEngine.summarizeDocument(
                    text = text,
                    mode = currentMode.value,
                    title = title
                )
                _generatedSummary.value = summary
            } catch (e: Exception) {
                _statusMessage.value = "Summarization error: ${e.message}"
            } finally {
                _isSummarizing.value = false
            }
        }
    }

    fun saveCurrentSummary() {
        val summary = _generatedSummary.value ?: return
        viewModelScope.launch {
            val title = if (documentTitle.value.isNotBlank()) documentTitle.value else "Document Summary"
            val origWords = documentText.value.split("\\s+".toRegex()).size
            val sumWords = summary.split("\\s+".toRegex()).size

            summaryDao.insertSummary(
                DocumentSummaryEntity(
                    title = title,
                    documentSnippet = documentText.value.take(200),
                    summaryMode = currentMode.value.name,
                    summaryText = summary,
                    originalWordCount = origWords,
                    summaryWordCount = sumWords
                )
            )
            _statusMessage.value = "Summary saved to your library!"
        }
    }

    fun trainAstillaWithThisDoc() {
        val text = documentText.value.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            val title = if (documentTitle.value.isNotBlank()) documentTitle.value else "Summarized Book/Doc"
            val training = AstillaBrainEngine.trainOnContent(
                title = title,
                sourceName = "Summarizer Tool",
                sourceType = "DOC",
                rawText = text
            )

            knowledgeDao.insertCapsule(
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

            _statusMessage.value = "Got it! Astilla A.I has assimilated this entire document into neural memory."
        }
    }

    fun deleteSavedSummary(id: Long) {
        viewModelScope.launch {
            summaryDao.deleteSummaryById(id)
        }
    }
}
