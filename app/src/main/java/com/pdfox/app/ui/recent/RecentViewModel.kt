package com.pdfox.app.ui.recent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfox.app.data.db.RecentFile
import com.pdfox.app.data.repository.FileRepository
import com.pdfox.app.util.FileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RecentViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val fileManager: FileManager
) : ViewModel() {

    private val _recentFiles = MutableStateFlow<List<RecentFile>>(emptyList())
    val recentFiles: StateFlow<List<RecentFile>> = _recentFiles.asStateFlow()

    private val _recentFilesFlow: Flow<List<RecentFile>> = fileRepository.recentFiles

    private var deletedFile: RecentFile? = null

    init {
        collectRecentFiles()
    }

    private fun collectRecentFiles() {
        viewModelScope.launch {
            _recentFilesFlow.collect { files ->
                _recentFiles.value = files
            }
        }
    }

    fun deleteFile(recentFile: RecentFile) {
        viewModelScope.launch {
            deletedFile = recentFile
            fileRepository.deleteRecentFile(recentFile)
            deleteOutputFile(recentFile.outputFilePath)
            deleteThumbnail(recentFile.thumbnailPath)
            Timber.d("Deleted recent file: ${recentFile.outputFileName}")
        }
    }

    fun restoreFile() {
        deletedFile?.let { file ->
            viewModelScope.launch {
                fileRepository.insertRecentFile(file)
                Timber.d("Restored recent file: ${file.outputFileName}")
            }
        }
    }

    fun clearDeletedFile() {
        deletedFile = null
    }

    fun deleteAllFiles() {
        viewModelScope.launch {
            val files = fileRepository.getRecentFilesList()
            files.forEach { file ->
                deleteOutputFile(file.outputFilePath)
                deleteThumbnail(file.thumbnailPath)
            }
            fileRepository.deleteAll()
            Timber.d("Deleted all recent files")
        }
    }

    private fun deleteOutputFile(filePath: String) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                fileManager.deleteFile(file)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete output file: $filePath")
        }
    }

    private fun deleteThumbnail(thumbnailPath: String?) {
        if (thumbnailPath == null) return
        try {
            val thumbnailFile = File(thumbnailPath)
            if (thumbnailFile.exists()) {
                fileManager.deleteFile(thumbnailFile)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete thumbnail: $thumbnailPath")
        }
    }
}
