package com.pdfox.app.ui.tools.pdftoword

import com.pdfox.app.ui.tools.BaseToolViewModel
import com.pdfox.app.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import org.apache.poi.xwpf.usermodel.XWPFDocument
import javax.inject.Inject
import java.io.File

@HiltViewModel
class PdfToWordViewModel @Inject constructor() : BaseToolViewModel() {
    override suspend fun executeToolAction(
        toolType: String,
        inputFiles: List<File>,
        options: Map<String, Any>
    ): Result<File> {
        return safeExecute {
            val inputFile = inputFiles.first()
            val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputFile)
            val text = com.tom_roush.pdfbox.text.PDFTextStripper().getText(document)
            document.close()

            val outputFile = fileManager.createOutputFile("PDFox_word", "docx")
            XWPFDocument().use { doc ->
                val paragraph = doc.createParagraph()
                val run = paragraph.createRun()
                run.setText(text)
                outputFile.outputStream().use { doc.write(it) }
            }
            outputFile
        }
    }
}
