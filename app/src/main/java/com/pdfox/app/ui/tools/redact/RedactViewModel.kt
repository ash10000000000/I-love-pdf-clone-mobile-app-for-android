package com.pdfox.app.ui.tools.redact

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.pdfox.app.data.db.RecentFile
import com.pdfox.app.data.repository.FileRepository
import com.pdfox.app.ui.tools.BaseToolViewModel
import com.pdfox.app.util.Result
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.util.Matrix
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
class RedactViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : BaseToolViewModel() {

    private val _selectedFile = MutableStateFlow<Uri?>(null)
    private val _redactions = MutableStateFlow<List<RedactionArea>>(emptyList())

    data class RedactionArea(
        val pageNumber: Int,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
    )

    fun setSelectedFile(uri: Uri) {
        _selectedFile.value = uri
    }

    fun addRedaction(pageNumber: Int, x: Float, y: Float, width: Float, height: Float) {
        val current = _redactions.value.toMutableList()
        current.add(RedactionArea(pageNumber, x, y, width, height))
        _redactions.value = current
    }

    fun clearRedactions() {
        _redactions.value = emptyList()
    }

    fun getRedactions() = _redactions.value

    fun redactPdf(inputFile: File, outputFile: File, redactions: List<RedactionArea>): File {
        val document = PDDocument.load(inputFile)

        try {
            // Group redactions by page
            val redactionsByPage = redactions.groupBy { it.pageNumber }

            for ((pageNum, pageRedactions) in redactionsByPage) {
                if (pageNum < 1 || pageNum > document.numberOfPages) continue

                val page = document.getPage(pageNum - 1)

                PDPageContentStream.appendContentStream(document, page, true).use { contentStream ->
                    contentStream.setNonStrokingColor(0f, 0f, 0f) // Black

                    for (redaction in pageRedactions) {
                        // Draw filled black rectangle
                        contentStream.addRect(
                            redaction.x,
                            redaction.y,
                            redaction.width,
                            redaction.height
                        )
                    }
                    contentStream.fill()
                }
            }

            document.save(outputFile)
            Timber.d("Redacted ${redactions.size} areas from PDF: ${outputFile.name}")
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
                val redactions = _redactions.value

                val outputFile = fileManager.createOutputFile(prefix = "PDFox_redacted", extension = "pdf")
                redactPdf(inputFile, outputFile, redactions)
            }
        }
    }
}
