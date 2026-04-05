package com.pdfox.app.ui.tools.extractpages

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
class ExtractPagesViewModel @Inject constructor(
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

    fun extractPages(inputFile: File, outputFile: File, pagesToExtract: Set<Int>): File {
        val document = PDDocument.load(inputFile)
        val totalPages = document.numberOfPages

        try {
            val newDoc = PDDocument()
            val sortedPages = pagesToExtract.filter { it in 1..totalPages }.sorted()
            sortedPages.forEach { pageNum ->
                newDoc.addPage(document.getPage(pageNum - 1))
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
                val pagesToExtractStr = options["pages_to_extract"] as? String ?: ""
                val pagesToExtract = pagesToExtractStr.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .toSet()

                val outputFile = fileManager.createOutputFile(prefix = "PDFox_extracted", extension = "pdf")
                extractPages(inputFile, outputFile, pagesToExtract)
            }
        }
    }
}
