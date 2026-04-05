package com.pdfox.app.ui.tools.removepages

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.pdfox.app.data.db.RecentFile
import com.pdfox.app.data.repository.FileRepository
import com.pdfox.app.ui.tools.BaseToolViewModel
import com.pdfox.app.util.Result
import com.tom_roush.pdfbox.pdmodel.PDDocument
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RemovePagesViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : BaseToolViewModel() {

    private val _selectedFile = MutableStateFlow<Uri?>(null)
    private val _pageCount = MutableStateFlow(0)
    private val _selectedPages = MutableStateFlow<Set<Int>>(emptySet())

    val pageCount: StateFlow<Int> = _pageCount.asStateFlow()

    fun setSelectedFile(uri: Uri) {
        _selectedFile.value = uri
    }

    fun setPageCount(count: Int) {
        _pageCount.value = count
    }

    fun setSelectedPages(pages: Set<Int>) {
        _selectedPages.value = pages
    }

    fun getSelectedPages(): Set<Int> = _selectedPages.value

    fun removePages(inputFile: File, outputFile: File, pagesToRemove: Set<Int>): File {
        val document = PDDocument.load(inputFile)
        val totalPages = document.numberOfPages

        try {
            // Remove pages in reverse order to avoid index shifting
            val sortedPages = pagesToRemove.filter { it in 1..totalPages }.sortedDescending()
            sortedPages.forEach { pageNum ->
                document.removePage(pageNum - 1)
            }

            document.save(outputFile)
        } finally {
            document.close()
        }

        return outputFile
    }

    override suspend fun executeToolAction(
        toolType: String,
        inputFiles: List<File>,
        options: Map<String, Any>
    ): Result<File> {
        return withContext(Dispatchers.IO) {
            safeExecuteSuspend {
                val inputFile = inputFiles.first()
                val pagesToRemoveStr = options["pages_to_remove"] as? String ?: ""
                val pagesToRemove = pagesToRemoveStr.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .toSet()

                val outputFile = fileManager.createOutputFile(prefix = "PDFox_cleaned", extension = "pdf")
                removePages(inputFile, outputFile, pagesToRemove)
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
}
