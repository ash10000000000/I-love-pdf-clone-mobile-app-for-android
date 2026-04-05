package com.pdfox.app.ui.tools.protect

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.pdfox.app.data.db.RecentFile
import com.pdfox.app.data.repository.FileRepository
import com.pdfox.app.ui.tools.BaseToolViewModel
import com.pdfox.app.util.Result
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
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
class ProtectViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : BaseToolViewModel() {

    private val _selectedFile = MutableStateFlow<Uri?>(null)
    private val _password = MutableStateFlow("")
    private val _encryptionLevel = MutableStateFlow("AES256")

    fun setSelectedFile(uri: Uri) {
        _selectedFile.value = uri
    }

    fun setPassword(password: String) {
        _password.value = password
    }

    fun setEncryptionLevel(level: String) {
        _encryptionLevel.value = level
    }

    fun protectPdf(inputFile: File, outputFile: File, password: String, encryption: String): File {
        val document = PDDocument.load(inputFile)

        try {
            val accessPermission = AccessPermission()
            accessPermission.isReadOnly = false
            accessPermission.canPrint = true
            accessPermission.canExtractContent = false

            val policy = if (encryption == "AES256") {
                StandardProtectionPolicy(password, password, accessPermission).apply {
                    encryptionKeyLength = 256
                }
            } else {
                StandardProtectionPolicy(password, password, accessPermission).apply {
                    encryptionKeyLength = 128
                }
            }

            policy.isEncryptMetadata = true
            document.protect(policy)
            document.save(outputFile)

            Timber.d("Protected PDF with $encryption encryption: ${outputFile.name}")
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
                val password = options["password"] as? String ?: "default"
                val encryption = options["encryption"] as? String ?: "AES256"

                val outputFile = fileManager.createOutputFile(prefix = "PDFox_protected", extension = "pdf")
                protectPdf(inputFile, outputFile, password, encryption)
            }
        }
    }
}
