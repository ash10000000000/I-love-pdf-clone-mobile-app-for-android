package com.pdfox.app.ui.tools.split

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.pdfox.app.data.db.RecentFile
import com.pdfox.app.data.repository.FileRepository
import com.pdfox.app.ui.tools.BaseToolViewModel
import com.pdfox.app.util.Result
import com.tom_roush.pdfbox.multipdf.Splitter
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
class SplitViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : BaseToolViewModel() {

    private val _selectedFile = MutableStateFlow<Uri?>(null)
    private val _pageRanges = MutableStateFlow("")

    val pageRanges: StateFlow<String> = _pageRanges.asStateFlow()

    fun setSelectedFile(uri: Uri) {
        _selectedFile.value = uri
    }

    fun setPageRanges(ranges: String) {
        _pageRanges.value = ranges
    }

    fun splitByRange(inputFile: File, outputDir: File, ranges: String): List<File> {
        val document = PDDocument.load(inputFile)
        val totalPages = document.numberOfPages
        val outputFiles = mutableListOf<File>()

        try {
            val pageList = parsePageRanges(ranges, totalPages)
            if (pageList.isEmpty()) {
                document.close()
                throw IllegalArgumentException("Invalid page ranges: $ranges")
            }

            val newDoc = PDDocument()
            for (pageNum in pageList) {
                if (pageNum >= 1 && pageNum <= totalPages) {
                    newDoc.addPage(document.getPage(pageNum - 1))
                }
            }

            val outputFile = File(outputDir, "PDFox_split_${System.currentTimeMillis()}.pdf")
            newDoc.save(outputFile)
            newDoc.close()
            outputFiles.add(outputFile)

        } finally {
            document.close()
        }

        return outputFiles
    }

    fun extractAllPages(inputFile: File, outputDir: File): List<File> {
        val splitter = Splitter(1)
        val document = PDDocument.load(inputFile)
        val outputFiles = mutableListOf<File>()

        try {
            val documents = splitter.split(document)
            documents.forEachIndexed { index, doc ->
                val outputFile = File(outputDir, "PDFox_page_${index + 1}_${System.currentTimeMillis()}.pdf")
                doc.save(outputFile)
                doc.close()
                outputFiles.add(outputFile)
            }
        } finally {
            document.close()
        }

        return outputFiles
    }

    fun splitToSingleFile(inputFile: File, outputFile: File, mode: String, ranges: String): File {
        return when (mode) {
            "range" -> {
                val document = PDDocument.load(inputFile)
                val totalPages = document.numberOfPages
                val pageList = parsePageRanges(ranges, totalPages)

                val newDoc = PDDocument()
                for (pageNum in pageList) {
                    if (pageNum >= 1 && pageNum <= totalPages) {
                        newDoc.addPage(document.getPage(pageNum - 1))
                    }
                }
                newDoc.save(outputFile)
                newDoc.close()
                document.close()
                outputFile
            }
            "extract" -> {
                val splitter = Splitter(1)
                val document = PDDocument.load(inputFile)
                val documents = splitter.split(document)
                if (documents.isNotEmpty()) {
                    documents.first().save(outputFile)
                    documents.forEach { it.close() }
                }
                document.close()
                outputFile
            }
            else -> throw IllegalArgumentException("Unknown split mode: $mode")
        }
    }

    private fun parsePageRanges(ranges: String, totalPages: Int): List<Int> {
        val pages = mutableSetOf<Int>()
        val parts = ranges.split(",")
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.contains("-")) {
                val rangeParts = trimmed.split("-")
                if (rangeParts.size == 2) {
                    val start = rangeParts[0].trim().toIntOrNull() ?: continue
                    val end = rangeParts[1].trim().toIntOrNull() ?: continue
                    for (i in start..end) {
                        if (i in 1..totalPages) pages.add(i)
                    }
                }
            } else {
                val page = trimmed.toIntOrNull()
                if (page != null && page in 1..totalPages) {
                    pages.add(page)
                }
            }
        }
        return pages.sorted()
    }

    override suspend fun executeToolAction(
        toolType: String,
        inputFiles: List<File>,
        options: Map<String, Any>
    ): Result<File> {
        return withContext(Dispatchers.IO) {
            safeExecuteSuspend {
                val inputFile = inputFiles.first()
                val mode = options["split_mode"] as? String ?: "range"
                val ranges = options["split_ranges"] as? String ?: "1"
                val outputFile = fileManager.createOutputFile(prefix = "PDFox_split", extension = "pdf")

                splitToSingleFile(inputFile, outputFile, mode, ranges)
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
