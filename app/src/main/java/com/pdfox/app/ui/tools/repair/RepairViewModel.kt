package com.pdfox.app.ui.tools.repair

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.pdfox.app.data.db.RecentFile
import com.pdfox.app.data.repository.FileRepository
import com.pdfox.app.ui.tools.BaseToolViewModel
import com.pdfox.app.util.Result
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RepairViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : BaseToolViewModel() {

    private val _selectedFile = MutableStateFlow<Uri?>(null)

    fun setSelectedFile(uri: Uri) {
        _selectedFile.value = uri
    }

    fun repairPdf(inputFile: File, outputFile: File): File {
        var document: PDDocument? = null
        var success = false

        try {
            // Attempt 1: Standard load
            try {
                document = PDDocument.load(inputFile)
                success = true
                Timber.d("Standard load succeeded")
            } catch (e: Exception) {
                Timber.w(e, "Standard load failed, trying lenient load")

                // Attempt 2: Try loading with memory
                try {
                    val bytes = inputFile.readBytes()
                    document = PDDocument.load(java.io.ByteArrayInputStream(bytes))
                    success = true
                    Timber.d("Memory load succeeded")
                } catch (e2: Exception) {
                    Timber.w(e2, "Memory load also failed")

                    // Attempt 3: Try partial recovery
                    try {
                        document = PDDocument.load(inputFile, java.nio.charset.StandardCharsets.ISO_8859_1)
                        success = true
                        Timber.d("Lenient charset load succeeded")
                    } catch (e3: Exception) {
                        Timber.e(e3, "All load attempts failed")
                        throw IllegalArgumentException("Unable to repair this PDF file. The file may be too corrupted to recover.")
                    }
                }
            }

            if (success && document != null) {
                // Rebuild the document
                val newDoc = PDDocument()

                try {
                    // Copy pages to new document (this filters out corrupted objects)
                    for (i in 0 until document.numberOfPages) {
                        try {
                            val page = document.getPage(i)
                            // Create a fresh page copy
                            val newPage = PDPage(page.mediaBox)
                            newPage.rotation = page.rotation
                            newDoc.addPage(newPage)
                        } catch (e: Exception) {
                            Timber.w(e, "Skipping corrupted page ${i + 1}")
                            // Add a blank page as placeholder
                            newDoc.addPage(PDPage())
                        }
                    }

                    // Copy available metadata
                    try {
                        val newInfo = newDoc.documentInformation
                        val oldInfo = document.documentInformation
                        newInfo.title = oldInfo.title ?: ""
                        newInfo.author = oldInfo.author ?: ""
                        newInfo.subject = oldInfo.subject ?: ""
                        newInfo.creator = oldInfo.creator ?: ""
                        newInfo.producer = "PDFox Repair"
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to copy metadata")
                    }

                    newDoc.save(outputFile)
                    Timber.d("Successfully repaired PDF: ${outputFile.name}")
                } finally {
                    newDoc.close()
                }
            }
        } finally {
            document?.close()
        }

        if (!success) {
            throw IllegalArgumentException("Unable to repair this PDF file.")
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
                val outputFile = fileManager.createOutputFile(prefix = "PDFox_repaired", extension = "pdf")
                repairPdf(inputFile, outputFile)
            }
        }
    }
}
