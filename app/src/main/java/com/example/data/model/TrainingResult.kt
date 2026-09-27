package com.example.data.model

data class TrainingResult(
    val title: String,
    val sourceName: String,
    val sourceType: String,
    val rawText: String,
    val coreSummary: String,
    val extractedKeyPoints: List<String>,
    val wordCount: Int,
    val acknowledgmentMessage: String
)
