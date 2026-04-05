package com.pdfox.app.data.repository

import com.pdfox.app.data.db.RecentFile
import com.pdfox.app.data.db.RecentFileDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepository @Inject constructor(
    private val recentFileDao: RecentFileDao
) {
    val recentFiles: Flow<List<RecentFile>> = recentFileDao.getAllRecentFiles()

    suspend fun getRecentFilesList(): List<RecentFile> {
        return recentFileDao.getAllRecentFilesList()
    }

    suspend fun insertRecentFile(recentFile: RecentFile): Long {
        return recentFileDao.insertRecentFile(recentFile)
    }

    suspend fun deleteRecentFile(recentFile: RecentFile) {
        recentFileDao.deleteRecentFile(recentFile)
    }

    suspend fun deleteById(fileId: Long) {
        recentFileDao.deleteById(fileId)
    }

    suspend fun deleteAll() {
        recentFileDao.deleteAll()
    }

    suspend fun getById(fileId: Long): RecentFile? {
        return recentFileDao.getById(fileId)
    }
}
