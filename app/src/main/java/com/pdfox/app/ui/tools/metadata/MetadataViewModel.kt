package com.pdfox.app.ui.tools.metadata

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
class MetadataViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : BaseToolViewModel() {

    private val _selectedFile = MutableStateFlow<Uri?>(null)
    private val _metadata = MutableStateFlow(MetadataInfo())

    data class MetadataInfo(
        val title: String = "",
        val author: String = "",
        val subject: String = "",
        val keywords: String = "",
        val creator: String = ""
    )

    fun setSelectedFile(uri: Uri) {
        _selectedFile.value = uri
    }

    fun setMetadata(title: String, author: String, subject: String, keywords: String, creator: String) {
        _metadata.value = MetadataInfo(title, author, subject, keywords, creator)
    }

    fun updateMetadata(inputFile: File, outputFile: File, metadata: MetadataInfo): File {
        val document = PDDocument.load(inputFile)

        try {
            val info = document.documentInformation

            if (metadata.title.isNotBlank()) info.title = metadata.title
            if (metadata.author.isNotBlank()) info.author = metadata.author
            if (metadata.subject.isNotBlank()) info.subject = metadata.subject
            if (metadata.keywords.isNotBlank()) info.keywords = metadata.keywords
            if (metadata.creator.isNotBlank()) info.creator = metadata.creator

            document.save(outputFile)
            Timber.d("Updated metadata for PDF: ${outputFile.name}")
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
                val metadata = MetadataInfo(
                    title = options["title"] as? String ?: "",
                    author = options["author"] as? String ?: "",
                    subject = options["subject"] as? String ?: "",
                    keywords = options["keywords"] as? String ?: "",
                    creator = options["creator"] as? String ?: ""
                )

                val outputFile = fileManager.createOutputFile(prefix = "PDFox_metadata", extension = "pdf")
                updateMetadata(inputFile, outputFile, metadata)
            }
        }
    }
}
