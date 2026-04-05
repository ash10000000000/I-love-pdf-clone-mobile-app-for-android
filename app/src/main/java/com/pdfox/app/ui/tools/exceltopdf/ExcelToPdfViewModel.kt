package com.pdfox.app.ui.tools.exceltopdf

import com.pdfox.app.ui.tools.BaseToolViewModel
import com.pdfox.app.util.Result
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import dagger.hilt.android.lifecycle.HiltViewModel
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import javax.inject.Inject
import java.io.File

@HiltViewModel
class ExcelToPdfViewModel @Inject constructor() : BaseToolViewModel() {
    override suspend fun executeToolAction(
        toolType: String,
        inputFiles: List<File>,
        options: Map<String, Any>
    ): Result<File> {
        return safeExecute {
            val inputFile = inputFiles.first()
            val outputFile = fileManager.createOutputFile("PDFox_from_excel", "pdf")

            XSSFWorkbook(java.io.FileInputStream(inputFile)).use { workbook ->
                PDDocument().use { doc ->
                    val sheet = workbook.getSheetAt(0)
                    var yPos = 750f
                    var page = PDPage(PDRectangle.A4)
                    doc.addPage(page)

                    PDPageContentStream(doc, page).use { cs ->
                        cs.beginText()
                        cs.setFont(PDType1Font.HELVETICA, 10f)
                        cs.newLineAtOffset(50f, yPos)

                        for (row in sheet) {
                            if (yPos < 50f) {
                                cs.endText()
                                page = PDPage(PDRectangle.A4)
                                doc.addPage(page)
                                cs.beginText()
                                cs.setFont(PDType1Font.HELVETICA, 10f)
                                yPos = 750f
                                cs.newLineAtOffset(50f, yPos)
                            }
                            val rowText = (0 until row.physicalNumberOfCells).joinToString("\t") { cell ->
                                row.getCell(cell)?.toString() ?: ""
                            }
                            cs.showText(rowText)
                            cs.newLineAtOffset(0f, -12f)
                            yPos -= 12f
                        }
                        cs.endText()
                    }
                }
            }
            outputFile
        }
    }
}
