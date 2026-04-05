package com.pdfox.app.ui.tools.organizepages

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.pdfox.app.data.db.RecentFile
import com.pdfox.app.data.repository.FileRepository
import com.pdfox.app.ui.tools.BaseToolViewModel
import com.pdfox.app.util.Result
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
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
class OrganizePagesViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : BaseToolViewModel() {

    private val _selectedFile = MutableStateFlow<Uri?>(null)
    private val _pageCount = MutableStateFlow(0)

    val pageCount: StateFlow<Int> = _pageCount.asStateFlow()

    fun setSelectedFile(uri: Uri) {
        _selectedFile.value = uri
    }

    fun setPageCount(count: Int) {
        _pageCount.value = count
    }

    fun rotatePage(pageNumber: Int, rotation: Int) {
        // Rotation is tracked in the adapter
    }

    fun organizePages(
        inputFile: File,
        outputFile: File,
        pageOrder: List<Int>,
        pageRotations: Map<Int, Int>
    ): File {
        val document = PDDocument.load(inputFile)
        val totalPages = document.numberOfPages

        try {
            val newDoc = PDDocument()

            // Add pages in the specified order
            for (pageNum in pageOrder) {
                if (pageNum in 1..totalPages) {
                    val page = document.getPage(pageNum - 1)
                    // Apply rotation if specified
                    val rotationDelta = pageRotations[pageNum] ?: 0
                    if (rotationDelta != 0) {
                        page.rotation = (page.rotation + rotationDelta) % 360
                    }
                    newDoc.addPage(page)
                }
            }

            newDoc.save(outputFile)
            newDoc.close()
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
                val pageOrderStr = options["page_order"] as? String ?: ""
                val pageRotationsStr = options["page_rotations"] as? String ?: ""

                val pageOrder = pageOrderStr.split(",").mapNotNull { it.trim().toIntOrNull() }
                val pageRotations = mutableMapOf<Int, Int>()
                if (pageRotationsStr.isNotBlank()) {
                    pageRotationsStr.split(",").forEach { entry ->
                        val parts = entry.split(":")
                        if (parts.size == 2) {
                            val pageNum = parts[0].trim().toIntOrNull()
                            val rotation = parts[1].trim().toIntOrNull()
                            if (pageNum != null && rotation != null) {
                                pageRotations[pageNum] = rotation
                            }
                        }
                    }
                }

                val outputFile = fileManager.createOutputFile(prefix = "PDFox_organized", extension = "pdf")
                organizePages(inputFile, outputFile, pageOrder, pageRotations)
            }
        }
    }
}
