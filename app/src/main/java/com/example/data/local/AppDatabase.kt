package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ChatDao
import com.example.data.local.dao.DocumentSummaryDao
import com.example.data.local.dao.KnowledgeDao
import com.example.data.local.dao.MemoryDao
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.DocumentSummaryEntity
import com.example.data.local.entity.KnowledgeCapsuleEntity
import com.example.data.local.entity.MemoryItemEntity

@Database(
    entities = [
        ChatMessageEntity::class,
        KnowledgeCapsuleEntity::class,
        MemoryItemEntity::class,
        DocumentSummaryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun knowledgeDao(): KnowledgeDao
    abstract fun memoryDao(): MemoryDao
    abstract fun documentSummaryDao(): DocumentSummaryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "astilla_ai_database.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
