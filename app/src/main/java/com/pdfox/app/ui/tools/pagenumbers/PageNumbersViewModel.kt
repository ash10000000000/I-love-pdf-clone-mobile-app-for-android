package com.pdfox.app.ui.tools.pagenumbers

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.pdfox.app.data.db.RecentFile
import com.pdfox.app.data.repository.FileRepository
import com.pdfox.app.ui.tools.BaseToolViewModel
import com.pdfox.app.util.Result
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
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
class PageNumbersViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : BaseToolViewModel() {

    private val _selectedFile = MutableStateFlow<Uri?>(null)
    private val _position = MutableStateFlow("bottom_center")
    private val _startNumber = MutableStateFlow(1)
    private val _fontSize = MutableStateFlow(12)

    fun setSelectedFile(uri: Uri) {
        _selectedFile.value = uri
    }

    fun setPosition(position: String) {
        _position.value = position
    }

    fun setStartNumber(number: Int) {
        _startNumber.value = number
    }

    fun setFontSize(size: Int) {
        _fontSize.value = size
    }

    fun getFontSize() = _fontSize.value

    fun addPageNumbers(inputFile: File, outputFile: File, position: String, startNumber: Int, fontSize: Int): File {
        val document = PDDocument.load(inputFile)

        try {
            val totalPages = document.numberOfPages

            for (i in 0 until totalPages) {
                val page = document.getPage(i)
                val mediaBox = page.mediaBox
                val margin = 36f
                val pageNumber = startNumber + i
                val text = "$pageNumber"

                val font = PDType1Font.HELVETICA
                val fontSizeF = fontSize.toFloat()
                val textWidth = font.getStringWidth(text) / 1000 * fontSizeF
                val textHeight = fontSizeF * 1.2f

                val (x, y) = when (position) {
                    "bottom_left" -> margin to margin + textHeight
                    "bottom_right" -> mediaBox.width - margin - textWidth to margin + textHeight
                    "top_left" -> margin to mediaBox.height - margin
                    "top_right" -> mediaBox.width - margin - textWidth to mediaBox.height - margin
                    "top_center" -> (mediaBox.width - textWidth) / 2 to mediaBox.height - margin
                    else -> (mediaBox.width - textWidth) / 2 to margin + textHeight // bottom_center
                }

                PDPageContentStream.appendContentStream(document, page, true).use { contentStream ->
                    contentStream.beginText()
                    contentStream.setFont(font, fontSizeF)
                    contentStream.newLineAtOffset(x, y)
                    contentStream.showText(text)
                    contentStream.endText()
                }
            }

            document.save(outputFile)
            Timber.d("Added page numbers to PDF: ${outputFile.name}")
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
                val position = options["position"] as? String ?: "bottom_center"
                val startNumber = options["start_number"] as? Int ?: 1
                val fontSize = options["font_size"] as? Int ?: 12

                val outputFile = fileManager.createOutputFile(prefix = "PDFox_numbered", extension = "pdf")
                addPageNumbers(inputFile, outputFile, position, startNumber, fontSize)
            }
        }
    }
}
