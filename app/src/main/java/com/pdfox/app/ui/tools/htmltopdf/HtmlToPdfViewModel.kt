package com.pdfox.app.ui.tools.htmltopdf

import com.pdfox.app.ui.tools.BaseToolViewModel
import com.pdfox.app.util.Result
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.io.File

@HiltViewModel
class HtmlToPdfViewModel @Inject constructor() : BaseToolViewModel() {
    override suspend fun executeToolAction(
        toolType: String,
        inputFiles: List<File>,
        options: Map<String, Any>
    ): Result<File> {
        return safeExecute {
            val inputFile = inputFiles.first()
            val outputFile = fileManager.createOutputFile("PDFox_from_html", "pdf")

            // Read HTML content from file
            val htmlContent = inputFile.readText()

            // Create a basic PDF from HTML content
            // For a real implementation, you would use WebView's print adapter
            val document = PDDocument()
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)

            val contentStream = PDPageContentStream(document, page)
            contentStream.beginText()
            contentStream.newLineAtOffset(50f, 750f)
            contentStream.setFont(PDType1Font.HELVETICA, 12f)

            // Strip HTML tags and add text
            val plainText = htmlContent.replace(Regex("<[^>]*>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()

            var yPos = 750f
            plainText.chunked(80).forEach { chunk ->
                if (yPos < 50f) {
                    contentStream.endText()
                    contentStream.close()
                    val newPage = PDPage(PDRectangle.A4)
                    document.addPage(newPage)
                    contentStream.beginText()
                    contentStream.setFont(PDType1Font.HELVETICA, 12f)
                    yPos = 750f
                }
                contentStream.newLineAtOffset(0f, -15f)
                contentStream.showText(chunk)
                yPos -= 15f
            }
            contentStream.endText()
            contentStream.close()

            document.save(outputFile)
            document.close()
            outputFile
        }
    }
}
