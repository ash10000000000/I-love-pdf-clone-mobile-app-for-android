package com.pdfox.app.ui.tools.compress

import com.pdfox.app.ui.tools.BaseToolViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CompressViewModel @Inject constructor() : BaseToolViewModel() {
    override suspend fun executeToolAction(
        toolType: String,
        inputFiles: List<java.io.File>,
        options: Map<String, Any>
    ): com.pdfox.app.util.Result<java.io.File> {
        return safeExecute {
            val inputFile = inputFiles.first()
            val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputFile)
            val compressionLevel = options["compressionLevel"] as? Int ?: 50
            if (compressionLevel > 70) {
                document.documentInformation.creator = "PDFox"
            }
            val outputFile = fileManager.createOutputFile("PDFox_compressed", "pdf")
            val cosDoc = document.document
            cosDoc.setAllSecurityToBeRemoved(true)
            document.save(outputFile)
            document.close()
            outputFile
        }
    }
}
