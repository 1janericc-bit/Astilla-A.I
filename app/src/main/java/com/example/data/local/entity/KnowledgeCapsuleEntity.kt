package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_capsules")
data class KnowledgeCapsuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val sourceType: String, // "TEXT", "PDF", "DOC", "MANUAL"
    val sourceName: String,
    val rawText: String,
    val coreSummary: String,
    val extractedKeyPoints: String, // Newline or semicolon separated
    val wordCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)
