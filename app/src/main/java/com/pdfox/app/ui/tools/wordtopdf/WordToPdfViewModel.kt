package com.pdfox.app.ui.tools.wordtopdf

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
class WordToPdfViewModel @Inject constructor() : BaseToolViewModel() {
    override suspend fun executeToolAction(
        toolType: String,
        inputFiles: List<File>,
        options: Map<String, Any>
    ): Result<File> {
        return safeExecute {
            val inputFile = inputFiles.first()
            val outputFile = fileManager.createOutputFile("PDFox_from_word", "pdf")

            // Extract text from DOCX using Apache POI
            val textExtractor = org.apache.poi.xwpf.extractor.XWPFWordExtractor(
                org.apache.poi.xwpf.usermodel.XWPFDocument(
                    java.io.FileInputStream(inputFile)
                )
            )
            val text = textExtractor.text
            textExtractor.close()

            // Create PDF with the extracted text
            PDDocument().use { doc ->
                val page = PDPage(PDRectangle.A4)
                doc.addPage(page)

                PDPageContentStream(doc, page).use { contentStream ->
                    contentStream.beginText()
                    contentStream.newLineAtOffset(50f, 750f)
                    contentStream.setFont(PDType1Font.HELVETICA, 12f)

                    val lines = text.lines()
                    var yPos = 750f
                    for (line in lines) {
                        if (yPos < 50f) {
                            contentStream.endText()
                            val newPage = PDPage(PDRectangle.A4)
                            doc.addPage(newPage)
                            contentStream.beginText()
                            contentStream.setFont(PDType1Font.HELVETICA, 12f)
                            yPos = 750f
                        }
                        contentStream.newLineAtOffset(0f, -15f)
                        contentStream.showText(line.take(80))
                        yPos -= 15f
                    }
                    contentStream.endText()
                }

                doc.save(outputFile)
            }
            outputFile
        }
    }
}
