package com.pdfox.app.ui.tools.pdftoimage

import android.graphics.Bitmap
import com.pdfox.app.ui.tools.BaseToolViewModel
import com.pdfox.app.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import net.lingala.zip4j.ZipFile
import javax.inject.Inject
import java.io.File

@HiltViewModel
class PdfToImageViewModel @Inject constructor() : BaseToolViewModel() {
    override suspend fun executeToolAction(
        toolType: String,
        inputFiles: List<File>,
        options: Map<String, Any>
    ): Result<File> {
        return safeExecute {
            val inputFile = inputFiles.first()
            val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputFile)
            val renderer = com.tom_roush.pdfbox.rendering.PDFRenderer(document)
            val dpi = (options["dpi"] as? Int) ?: 150
            val format = (options["format"] as? String) ?: "png"
            val ext = if (format == "jpg") "jpg" else "png"
            val compressFormat = if (format == "jpg") Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
            val quality = if (format == "jpg") 90 else 100

            val outputDir = fileManager.getCacheDirectory()
            val imageFiles = mutableListOf<File>()

            for (i in 0 until document.numberOfPages) {
                val bitmap = renderer.renderImageWithDPI(i, dpi, Bitmap.Config.ARGB_8888)
                val imageFile = File(outputDir, "page_${i + 1}.$ext")
                imageFile.outputStream().use { bitmap.compress(compressFormat, quality, it) }
                bitmap.recycle()
                imageFiles.add(imageFile)
            }
            document.close()

            val outputFile = fileManager.createOutputFile("PDFox_images", "zip")
            val zipFile = ZipFile(outputFile)
            zipFile.addFiles(imageFiles)

            imageFiles.forEach { it.delete() }
            outputFile
        }
    }
}
