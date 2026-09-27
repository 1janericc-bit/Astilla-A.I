package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.DocumentSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentSummaryDao {
    @Query("SELECT * FROM document_summaries ORDER BY timestamp DESC")
    fun getAllSummaries(): Flow<List<DocumentSummaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: DocumentSummaryEntity): Long

    @Delete
    suspend fun deleteSummary(summary: DocumentSummaryEntity)

    @Query("DELETE FROM document_summaries WHERE id = :id")
    suspend fun deleteSummaryById(id: Long)

    @Query("DELETE FROM document_summaries")
    suspend fun clearAllSummaries()
}
