package com.pdfox.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentFileDao {
    @Query("SELECT * FROM recent_files ORDER BY timestamp DESC LIMIT 20")
    fun getAllRecentFiles(): Flow<List<RecentFile>>

    @Query("SELECT * FROM recent_files ORDER BY timestamp DESC LIMIT 20")
    suspend fun getAllRecentFilesList(): List<RecentFile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentFile(recentFile: RecentFile): Long

    @Delete
    suspend fun deleteRecentFile(recentFile: RecentFile): Int

    @Query("DELETE FROM recent_files WHERE id = :fileId")
    suspend fun deleteById(fileId: Long): Int

    @Query("DELETE FROM recent_files")
    suspend fun deleteAll()

    @Query("SELECT * FROM recent_files WHERE id = :fileId LIMIT 1")
    suspend fun getById(fileId: Long): RecentFile?
}
