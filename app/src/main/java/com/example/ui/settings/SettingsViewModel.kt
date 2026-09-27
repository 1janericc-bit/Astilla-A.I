package com.example.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.entity.MemoryItemEntity
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val memoryDao = db.memoryDao()
    private val knowledgeDao = db.knowledgeDao()
    private val chatDao = db.chatDao()
    private val prefs = UserPreferencesRepository(application)

    val themeMode: StateFlow<ThemeMode> = prefs.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val memoryEnabled: StateFlow<Boolean> = prefs.memoryEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val offlineOnly: StateFlow<Boolean> = prefs.offlineOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val customApiKey: StateFlow<String> = prefs.customApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val memoryItems: StateFlow<List<MemoryItemEntity>> = memoryDao.getAllMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    fun dismissBanner() {
        _statusBanner.value = null
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            prefs.setThemeMode(mode)
        }
    }

    fun setMemoryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setMemoryEnabled(enabled)
            _statusBanner.value = if (enabled) "Active context memory enabled." else "Context memory paused."
        }
    }

    fun setOfflineOnly(offline: Boolean) {
        viewModelScope.launch {
            prefs.setOfflineOnly(offline)
            _statusBanner.value = if (offline) "Strictly Offline mode enabled." else "Hybrid Cloud assist enabled."
        }
    }

    fun setCustomApiKey(key: String) {
        viewModelScope.launch {
            prefs.setCustomApiKey(key)
            _statusBanner.value = "API Key configuration updated."
        }
    }

    fun addCustomMemory(key: String, value: String, category: String = "USER_PREFERENCE") {
        if (key.isBlank() || value.isBlank()) return
        viewModelScope.launch {
            memoryDao.insertMemory(
                MemoryItemEntity(
                    key = key.trim(),
                    value = value.trim(),
                    category = category
                )
            )
            _statusBanner.value = "Memory saved to active context."
        }
    }

    fun toggleMemoryActive(id: Long, currentActive: Boolean) {
        viewModelScope.launch {
            memoryDao.updateMemoryStatus(id, !currentActive)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            memoryDao.deleteMemoryById(id)
        }
    }

    fun eraseAllMemoryAndContext() {
        viewModelScope.launch {
            memoryDao.clearAllMemories()
            _statusBanner.value = "All active context and stored memories have been completely erased."
        }
    }

    fun eraseAllKnowledge() {
        viewModelScope.launch {
            knowledgeDao.clearAllKnowledge()
            _statusBanner.value = "All trained knowledge capsules have been wiped."
        }
    }
}
