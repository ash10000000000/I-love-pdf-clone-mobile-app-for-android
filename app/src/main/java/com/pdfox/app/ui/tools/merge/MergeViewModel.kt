package com.pdfox.app.ui.tools.merge

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.pdfox.app.data.db.RecentFile
import com.pdfox.app.data.repository.FileRepository
import com.pdfox.app.ui.tools.BaseToolViewModel
import com.pdfox.app.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MergeViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : BaseToolViewModel() {

    private val _files = MutableStateFlow<List<Uri>>(emptyList())
    val files: StateFlow<List<Uri>> = _files.asStateFlow()

    fun setFiles(uris: List<Uri>) {
        _files.value = uris
    }

    fun mergeFiles(outputFile: File) {
        viewModelScope.launch {
            setState(com.pdfox.app.ui.tools.ToolUiState.Loading())
            try {
                val uriList = _files.value
                if (uriList.size < 2) {
                    setError("At least 2 PDF files are required to merge")
                    return@launch
                }

                val inputFiles = uriList.map { uri ->
                    val fileName = fileManager.getFileNameFromUri(uri) ?: "input.pdf"
                    fileManager.uriToFile(uri, fileName)
                }

                val merger = com.tom_roush.pdfbox.multipdf.PDFMergerUtility()
                merger.destinationFileName = outputFile.absolutePath
                inputFiles.forEach { file ->
                    merger.addSource(file.absolutePath)
                }
                merger.mergeDocuments(null)

                val firstFileName = fileManager.getFileNameFromUri(uriList.first()) ?: "merged"
                saveToRecent(outputFile, "merge", firstFileName)
                setState(com.pdfox.app.ui.tools.ToolUiState.Success(outputFile))
                Timber.d("Successfully merged ${inputFiles.size} files into ${outputFile.name}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to merge PDF files")
                setError("Failed to merge files: ${e.message}", e)
            }
        }
    }

    private suspend fun saveToRecent(outputFile: File, toolType: String, inputFileName: String) {
        try {
            val pageCount = fileManager.getPageCount(outputFile)
            val fileSize = fileManager.getFileSize(outputFile)
            val thumbnailPath = fileManager.createThumbnail(outputFile)

            val recentFile = RecentFile(
                inputFileName = inputFileName,
                inputFilePath = null,
                outputFileName = outputFile.name,
                outputFilePath = outputFile.absolutePath,
                toolUsed = toolType,
                timestamp = System.currentTimeMillis(),
                fileSizeBytes = fileSize,
                pageCount = pageCount.takeIf { it > 0 } ?: 1,
                thumbnailPath = thumbnailPath,
                outputFormat = "PDF"
            )
            fileRepository.insertRecentFile(recentFile)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save to recent files")
        }
    }

    override suspend fun executeToolAction(
        toolType: String,
        inputFiles: List<File>,
        options: Map<String, Any>
    ): Result<File> {
        val outputFile = fileManager.createOutputFile(prefix = "PDFox_merged", extension = "pdf")
        try {
            val merger = com.tom_roush.pdfbox.multipdf.PDFMergerUtility()
            merger.destinationFileName = outputFile.absolutePath
            inputFiles.forEach { file ->
                merger.addSource(file.absolutePath)
            }
            merger.mergeDocuments(null)
            return Result.Success(outputFile)
        } catch (e: Exception) {
            Timber.e(e, "Merge failed")
            return Result.Error("Failed to merge files: ${e.message}", e)
        }
    }
}
