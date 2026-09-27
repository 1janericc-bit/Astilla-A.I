package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "document_summaries")
data class DocumentSummaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val documentSnippet: String,
    val summaryMode: String, // "EXECUTIVE", "TAKEAWAYS", "CHAPTERS", "FLASHCARDS", "DEEP_DIVE"
    val summaryText: String,
    val originalWordCount: Int,
    val summaryWordCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)
