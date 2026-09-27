package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.KnowledgeCapsuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeDao {
    @Query("SELECT * FROM knowledge_capsules ORDER BY timestamp DESC")
    fun getAllKnowledgeCapsules(): Flow<List<KnowledgeCapsuleEntity>>

    @Query("SELECT * FROM knowledge_capsules ORDER BY timestamp DESC")
    suspend fun getAllKnowledgeCapsulesSnapshot(): List<KnowledgeCapsuleEntity>

    @Query("SELECT * FROM knowledge_capsules WHERE id = :id LIMIT 1")
    suspend fun getKnowledgeCapsuleById(id: Long): KnowledgeCapsuleEntity?

    @Query("SELECT * FROM knowledge_capsules WHERE title LIKE '%' || :query || '%' OR rawText LIKE '%' || :query || '%' OR coreSummary LIKE '%' || :query || '%'")
    fun searchKnowledge(query: String): Flow<List<KnowledgeCapsuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapsule(capsule: KnowledgeCapsuleEntity): Long

    @Delete
    suspend fun deleteCapsule(capsule: KnowledgeCapsuleEntity)

    @Query("DELETE FROM knowledge_capsules WHERE id = :id")
    suspend fun deleteCapsuleById(id: Long)

    @Query("DELETE FROM knowledge_capsules")
    suspend fun clearAllKnowledge()
}
