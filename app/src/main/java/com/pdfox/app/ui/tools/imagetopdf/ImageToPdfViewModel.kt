package com.pdfox.app.ui.tools.imagetopdf

import android.graphics.BitmapFactory
import com.pdfox.app.ui.tools.BaseToolViewModel
import com.pdfox.app.util.Result
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.io.File

@HiltViewModel
class ImageToPdfViewModel @Inject constructor() : BaseToolViewModel() {
    override suspend fun executeToolAction(
        toolType: String,
        inputFiles: List<File>,
        options: Map<String, Any>
    ): Result<File> {
        return safeExecute {
            val outputFile = fileManager.createOutputFile("PDFox_from_images", "pdf")
            val pageSize = options["pageSize"] as? String ?: "A4"

            PDDocument().use { doc ->
                inputFiles.forEach { imageFile ->
                    val bitmapOptions = BitmapFactory.Options()
                    bitmapOptions.inJustDecodeBounds = true
                    BitmapFactory.decodeFile(imageFile.absolutePath, bitmapOptions)

                    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                    if (bitmap != null) {
                        val width = bitmap.width.toFloat()
                        val height = bitmap.height.toFloat()
                        val page = PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle(width, height))
                        doc.addPage(page)

                        val pdImage = LosslessFactory.createFromImage(doc, bitmap)
                        PDPageContentStream(doc, page).use { cs ->
                            cs.drawImage(pdImage, 0f, 0f, width, height)
                        }
                        bitmap.recycle()
                    }
                }
                doc.save(outputFile)
            }
            outputFile
        }
    }
}
