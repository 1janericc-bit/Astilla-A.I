package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory_items")
data class MemoryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val value: String,
    val category: String = "USER_PREFERENCE", // "FACT", "USER_PREFERENCE", "PROJECT_CONTEXT", "KNOWLEDGE"
    val timestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
