package com.pdfox.app.ui.tools.watermark

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
class WatermarkViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : BaseToolViewModel() {

    private val _selectedFile = MutableStateFlow<Uri?>(null)
    private val _text = MutableStateFlow("CONFIDENTIAL")
    private val _position = MutableStateFlow("diagonal")
    private val _opacity = MutableStateFlow(0.3f)
    private val _fontSize = MutableStateFlow(60)

    fun setSelectedFile(uri: Uri) {
        _selectedFile.value = uri
    }

    fun setText(text: String) {
        _text.value = text
    }

    fun setPosition(position: String) {
        _position.value = position
    }

    fun setOpacity(opacity: Float) {
        _opacity.value = opacity
    }

    fun getOpacity() = _opacity.value

    fun setFontSize(size: Int) {
        _fontSize.value = size
    }

    fun getFontSize() = _fontSize.value

    fun addWatermark(inputFile: File, outputFile: File, text: String, position: String, opacity: Float, fontSize: Int): File {
        val document = PDDocument.load(inputFile)

        try {
            val font = PDType1Font.HELVETICA_BOLD
            val fontSizeF = fontSize.toFloat()
            val textWidth = font.getStringWidth(text) / 1000 * fontSizeF
            val totalPages = document.numberOfPages

            for (i in 0 until totalPages) {
                val page = document.getPage(i)
                val mediaBox = page.mediaBox

                PDPageContentStream.appendContentStream(document, page, true).use { contentStream ->
                    when (position) {
                        "center" -> {
                            val x = (mediaBox.width - textWidth) / 2
                            val y = (mediaBox.height - fontSizeF) / 2
                            drawText(contentStream, text, font, fontSizeF, opacity, x, y)
                        }
                        "diagonal" -> {
                            val x = (mediaBox.width - textWidth) / 2
                            val y = (mediaBox.height - fontSizeF) / 2
                            contentStream.saveGraphicsState()
                            contentStream.transform(Matrix.getRotateInstance(-0.785398, x, y)) // -45 degrees
                            contentStream.beginText()
                            contentStream.setNonStrokingColor(0.5f, 0.5f, 0.5f)
                            contentStream.setNonStrokingAlphaConstant(opacity)
                            contentStream.setFont(font, fontSizeF)
                            contentStream.newLineAtOffset(0f, 0f)
                            contentStream.showText(text)
                            contentStream.endText()
                            contentStream.restoreGraphicsState()
                        }
                        "tiled" -> {
                            // Create tiled watermark pattern
                            val cols = 2
                            val rows = 3
                            val cellWidth = mediaBox.width / cols
                            val cellHeight = mediaBox.height / rows

                            for (row in 0 until rows) {
                                for (col in 0 until cols) {
                                    val x = col * cellWidth + (cellWidth - textWidth) / 2
                                    val y = row * cellHeight + (cellHeight - fontSizeF) / 2
                                    drawText(contentStream, text, font, fontSizeF, opacity, x, y)
                                }
                            }
                        }
                    }
                }
            }

            document.save(outputFile)
            Timber.d("Added watermark to PDF: ${outputFile.name}")
        } finally {
            document.close()
        }

        return outputFile
    }

    private fun drawText(
        contentStream: PDPageContentStream,
        text: String,
        font: PDType1Font,
        fontSize: Float,
        opacity: Float,
        x: Float,
        y: Float
    ) {
        contentStream.beginText()
        contentStream.setNonStrokingColor(0.5f, 0.5f, 0.5f)
        contentStream.setNonStrokingAlphaConstant(opacity)
        contentStream.setFont(font, fontSize)
        contentStream.newLineAtOffset(x, y)
        contentStream.showText(text)
        contentStream.endText()
    }

    override suspend fun executeToolAction(
        toolType: String,
        inputFiles: List<File>,
        options: Map<String, Any>
    ): Result<File> {
        return withContext(Dispatchers.IO) {
            safeExecuteSuspend {
                val inputFile = inputFiles.first()
                val text = options["watermark_text"] as? String ?: "CONFIDENTIAL"
                val position = options["position"] as? String ?: "diagonal"
                val opacity = options["opacity"] as? Float ?: 0.3f
                val fontSize = options["font_size"] as? Int ?: 60

                val outputFile = fileManager.createOutputFile(prefix = "PDFox_watermarked", extension = "pdf")
                addWatermark(inputFile, outputFile, text, position, opacity, fontSize)
            }
        }
    }
}
