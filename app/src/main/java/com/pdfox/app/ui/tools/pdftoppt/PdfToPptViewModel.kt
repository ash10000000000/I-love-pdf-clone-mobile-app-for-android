package com.pdfox.app.ui.tools.pdftoppt

import com.pdfox.app.ui.tools.BaseToolViewModel
import com.pdfox.app.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import org.apache.poi.xslf.usermodel.XMLSlideShow
import javax.inject.Inject
import java.io.File

@HiltViewModel
class PdfToPptViewModel @Inject constructor() : BaseToolViewModel() {
    override suspend fun executeToolAction(
        toolType: String,
        inputFiles: List<File>,
        options: Map<String, Any>
    ): Result<File> {
        return safeExecute {
            val inputFile = inputFiles.first()
            val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputFile)
            val renderer = com.tom_roush.pdfbox.rendering.PDFRenderer(document)
            val outputFile = fileManager.createOutputFile("PDFox_ppt", "pptx")

            XMLSlideShow().use { pptx ->
                for (i in 0 until document.numberOfPages) {
                    val bitmap = renderer.renderImageWithDPI(i, 150, android.graphics.Bitmap.Config.ARGB_8888)
                    val slide = pptx.createSlide()
                    val imageType = org.apache.poi.sl.usermodel.PictureData.PictureType.PNG
                    val imageBytes = run {
                        val baos = java.io.ByteArrayOutputStream()
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, baos)
                        baos.toByteArray()
                    }
                    val idx = pptx.addPicture(imageBytes, imageType)
                    val ctShape = slide.createPicture(idx)
                    bitmap.recycle()
                }
                outputFile.outputStream().use { pptx.write(it) }
            }
            document.close()
            outputFile
        }
    }
}
