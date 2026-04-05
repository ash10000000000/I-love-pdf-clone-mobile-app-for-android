package com.pdfox.app.ui.tools.sign

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.pdfox.app.data.db.RecentFile
import com.pdfox.app.data.repository.FileRepository
import com.pdfox.app.ui.tools.BaseToolViewModel
import com.pdfox.app.util.Result
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SignViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : BaseToolViewModel() {

    private val _selectedFile = MutableStateFlow<Uri?>(null)
    private val _signatureBitmap = MutableStateFlow<Bitmap?>(null)
    private val _pageNumber = MutableStateFlow(1)

    fun setSelectedFile(uri: Uri) {
        _selectedFile.value = uri
    }

    fun setSignatureBitmap(bitmap: Bitmap) {
        _signatureBitmap.value = bitmap
    }

    fun setPageNumber(page: Int) {
        _pageNumber.value = page
    }

    fun signPdf(inputFile: File, outputFile: File, signatureBitmap: Bitmap, pageNumber: Int): File {
        val document = PDDocument.load(inputFile)

        try {
            val totalPages = document.numberOfPages
            val targetPage = if (pageNumber in 1..totalPages) pageNumber - 1 else 0
            val page = document.getPage(targetPage)

            // Convert bitmap to PDImage
            val pdImage = LosslessFactory.createFromImage(document, signatureBitmap)

            // Calculate position (bottom-right of page with margin)
            val margin = 50f
            val imageWidth = 150f
            val imageHeight = 75f
            val x = page.mediaBox.width - imageWidth - margin
            val y = margin

            // Add signature to page
            PDPageContentStream.appendContentStream(document, page, true).use { contentStream ->
                contentStream.drawImage(pdImage, x, y, imageWidth, imageHeight)
            }

            document.save(outputFile)
            Timber.d("Signed PDF on page $pageNumber: ${outputFile.name}")
        } finally {
            document.close()
        }

        return outputFile
    }

    override suspend fun executeToolAction(
        toolType: String,
        inputFiles: List<File>,
        options: Map<String, Any>
    ): Result<File> {
        return withContext(Dispatchers.IO) {
            safeExecuteSuspend {
                val inputFile = inputFiles.first()
                val pageNumber = options["page_number"] as? Int ?: 1
                val signatureBitmap = _signatureBitmap.value
                    ?: throw IllegalArgumentException("No signature provided")

                val outputFile = fileManager.createOutputFile(prefix = "PDFox_signed", extension = "pdf")
                signPdf(inputFile, outputFile, signatureBitmap, pageNumber)
            }
        }
    }
}
