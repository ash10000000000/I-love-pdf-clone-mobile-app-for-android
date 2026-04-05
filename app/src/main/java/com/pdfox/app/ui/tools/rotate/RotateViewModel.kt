package com.pdfox.app.ui.tools.rotate

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
class RotateViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : BaseToolViewModel() {

    private val _selectedFile = MutableStateFlow<Uri?>(null)
    private val _rotation = MutableStateFlow(90)
    private val _applyToAll = MutableStateFlow(true)
    private val _selectedPages = MutableStateFlow("")

    fun setSelectedFile(uri: Uri) {
        _selectedFile.value = uri
    }

    fun setRotation(degrees: Int) {
        _rotation.value = degrees
    }

    fun setApplyToAll(all: Boolean) {
        _applyToAll.value = all
    }

    fun setSelectedPages(pages: String) {
        _selectedPages.value = pages
    }

    fun rotatePdf(inputFile: File, outputFile: File, rotation: Int, allPages: Boolean, selectedPagesStr: String): File {
        val document = PDDocument.load(inputFile)

        try {
            val totalPages = document.numberOfPages
            val pagesToRotate = if (allPages) {
                (1..totalPages).toSet()
            } else {
                parsePageSelection(selectedPagesStr, totalPages)
            }

            for (pageNum in pagesToRotate) {
                if (pageNum in 1..totalPages) {
                    val page = document.getPage(pageNum - 1)
                    val currentRotation = page.rotation
                    val newRotation = (currentRotation + rotation + 360) % 360
                    page.rotation = newRotation
                }
            }

            document.save(outputFile)
            Timber.d("Rotated PDF by $rotation degrees: ${outputFile.name}")
        } finally {
            document.close()
        }

        return outputFile
    }

    private fun parsePageSelection(pagesStr: String, totalPages: Int): Set<Int> {
        val pages = mutableSetOf<Int>()
        val parts = pagesStr.split(",")

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

        return pages.ifEmpty { (1..totalPages).toSet() }
    }

    override suspend fun executeToolAction(
        toolType: String,
        inputFiles: List<File>,
        options: Map<String, Any>
    ): Result<File> {
        return withContext(Dispatchers.IO) {
            safeExecuteSuspend {
                val inputFile = inputFiles.first()
                val rotation = options["rotation"] as? Int ?: 90
                val allPages = options["all_pages"] as? Boolean ?: true
                val selectedPages = options["selected_pages"] as? String ?: ""

                val outputFile = fileManager.createOutputFile(prefix = "PDFox_rotated", extension = "pdf")
                rotatePdf(inputFile, outputFile, rotation, allPages, selectedPages)
            }
        }
    }
}
