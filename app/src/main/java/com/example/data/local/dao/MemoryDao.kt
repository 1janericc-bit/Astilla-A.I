package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.MemoryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_items ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryItemEntity>>

    @Query("SELECT * FROM memory_items WHERE isActive = 1 ORDER BY timestamp DESC")
    suspend fun getActiveMemoriesSnapshot(): List<MemoryItemEntity>

    @Query("SELECT * FROM memory_items WHERE isActive = 1 ORDER BY timestamp DESC")
    fun getActiveMemories(): Flow<List<MemoryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryItemEntity): Long

    @Query("UPDATE memory_items SET isActive = :isActive WHERE id = :id")
    suspend fun updateMemoryStatus(id: Long, isActive: Boolean)

    @Delete
    suspend fun deleteMemory(memory: MemoryItemEntity)

    @Query("DELETE FROM memory_items WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("DELETE FROM memory_items")
    suspend fun clearAllMemories()
}
