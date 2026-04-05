package com.pdfox.app.ui.tools.ppttopdf

import com.pdfox.app.ui.tools.BaseToolViewModel
import com.pdfox.app.util.Result
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import dagger.hilt.android.lifecycle.HiltViewModel
import org.apache.poi.xslf.usermodel.XMLSlideShow
import javax.inject.Inject
import java.io.File

@HiltViewModel
class PptToPdfViewModel @Inject constructor() : BaseToolViewModel() {
    override suspend fun executeToolAction(
        toolType: String,
        inputFiles: List<File>,
        options: Map<String, Any>
    ): Result<File> {
        return safeExecute {
            val inputFile = inputFiles.first()
            val outputFile = fileManager.createOutputFile("PDFox_from_ppt", "pdf")

            // Extract text from PPTX
            XMLSlideShow(java.io.FileInputStream(inputFile)).use { pptx ->
                PDDocument().use { doc ->
                    for (i in 0 until pptx.numberOfSlides) {
                        val slide = pptx.getSlide(i)
                        val text = slide.title ?: "Slide ${i + 1}"

                        val page = PDPage(PDRectangle.A4)
                        doc.addPage(page)

                        PDPageContentStream(doc, page).use { cs ->
                            cs.beginText()
                            cs.newLineAtOffset(50f, 750f)
                            cs.setFont(PDType1Font.HELVETICA_BOLD, 18f)
                            cs.showText(text)
                            cs.endText()
                        }
                    }
                    doc.save(outputFile)
                }
            }
            outputFile
        }
    }
}
