package com.pdfox.app.ui.tools.unlock

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.pdfox.app.data.db.RecentFile
import com.pdfox.app.data.repository.FileRepository
import com.pdfox.app.ui.tools.BaseToolViewModel
import com.pdfox.app.util.Result
import com.tom_roush.pdfbox.pdmodel.PDDocument
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
class UnlockViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : BaseToolViewModel() {

    private val _selectedFile = MutableStateFlow<Uri?>(null)
    private val _password = MutableStateFlow("")

    fun setSelectedFile(uri: Uri) {
        _selectedFile.value = uri
    }

    fun setPassword(password: String) {
        _password.value = password
    }

    fun unlockPdf(inputFile: File, outputFile: File, password: String): File {
        val document = try {
            PDDocument.load(inputFile, password)
        } catch (e: Exception) {
            throw IllegalArgumentException("Incorrect password or file is not encrypted")
        }

        try {
            // Check if document is actually encrypted
            if (!document.isEncrypted) {
                document.close()
                throw IllegalArgumentException("This PDF is not password protected")
            }

            // Remove all security
            document.setAllSecurityToBeRemoved(true)
            document.save(outputFile)

            Timber.d("Unlocked PDF: ${outputFile.name}")
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
                val password = options["password"] as? String ?: ""

                val outputFile = fileManager.createOutputFile(prefix = "PDFox_unlocked", extension = "pdf")
                unlockPdf(inputFile, outputFile, password)
            }
        }
    }
}
