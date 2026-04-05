package com.pdfox.app.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.pdfox.app.R
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val APP_FOLDER_NAME = "PDFox"
        const val MIME_PDF = "application/pdf"
        const val MIME_IMAGE = "image/*"
        const val MIME_WORD = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        const val MIME_EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        const val MIME_POWERPOINT = "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    }

    init {
        PDFBoxResourceLoader.init(context)
    }

    // Get the app's output directory
    fun getOutputDirectory(): File {
        val dir = File(context.getExternalFilesDir(null), APP_FOLDER_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    // Get cache directory for temporary files
    fun getCacheDirectory(): File {
        val dir = File(context.cacheDir, APP_FOLDER_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    // Create a unique output file path
    fun createOutputFile(prefix: String = "PDFox", extension: String = "pdf"): File {
        val dir = getOutputDirectory()
        val timestamp = System.currentTimeMillis()
        return File(dir, "${prefix}_$timestamp.$extension")
    }

    // Create a temp file path for processing
    fun createTempFile(prefix: String = "temp", extension: String = "pdf"): File {
        val dir = getCacheDirectory()
        val timestamp = System.currentTimeMillis()
        return File(dir, "${prefix}_$timestamp.$extension")
    }

    // Copy URI to File
    suspend fun uriToFile(uri: Uri, fileName: String? = null): File = withContext(Dispatchers.IO) {
        val tempFile = createTempFile("input", getFileExtension(uri) ?: "pdf")
        val resolvedName = fileName ?: getFileNameFromUri(uri)
        if (resolvedName != null) {
            val newName = File(tempFile.parent, resolvedName)
            if (tempFile.exists()) tempFile.delete()
            return@withContext copyUriToFile(uri, newName)
        } else {
            return@withContext copyUriToFile(uri, tempFile)
        }
    }

    private fun copyUriToFile(uri: Uri, destFile: File): File {
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        return destFile
    }

    // Copy file to Downloads folder
    suspend fun copyToDownloads(sourceFile: File): Uri? = withContext(Dispatchers.IO) {
        try {
            val fileName = sourceFile.name
            val mimeType = getMimeType(sourceFile)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, MediaStore.Downloads.DIR_NAME)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        FileInputStream(sourceFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(it, values, null, null)
                }
                uri
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                val destFile = File(downloadsDir, fileName)
                sourceFile.copyTo(destFile, overwrite = true)
                Uri.fromFile(destFile)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to copy to downloads: ${e.message}")
            null
        }
    }

    // Get file size from URI
    suspend fun getFileSize(uri: Uri): Long = withContext(Dispatchers.IO) {
        var size: Long = 0
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                size = pfd.statSize
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get file size")
        }
        size
    }

    // Get file size from File
    fun getFileSize(file: File): Long {
        return file.length()
    }

    // Get page count from PDF URI
    suspend fun getPageCount(uri: Uri): Int = withContext(Dispatchers.IO) {
        var count = 0
        var pfd: ParcelFileDescriptor? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r")
            val renderer = PdfRenderer(pfd!!)
            count = renderer.pageCount
            renderer.close()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get page count via PdfRenderer, trying PdfBox")
            count = getPageCountViaPdfBox(uri)
        } finally {
            pfd?.close()
        }
        count
    }

    private fun getPageCountViaPdfBox(uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val tempFile = createTempFile()
                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                val document = PDDocument.load(tempFile)
                val count = document.numberOfPages
                document.close()
                tempFile.delete()
                count
            } ?: 0
        } catch (e: Exception) {
            Timber.e(e, "Failed to get page count via PdfBox")
            0
        }
    }

    // Get page count from File
    fun getPageCount(file: File): Int {
        return try {
            val document = PDDocument.load(file)
            val count = document.numberOfPages
            document.close()
            count
        } catch (e: Exception) {
            Timber.e(e, "Failed to get page count")
            0
        }
    }

    // Create thumbnail from PDF first page
    suspend fun createThumbnail(uri: Uri, outputPath: File? = null): String? = withContext(Dispatchers.IO) {
        try {
            val thumbnailFile = outputPath ?: File(context.cacheDir, "thumb_${System.currentTimeMillis()}.png")
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val renderer = PdfRenderer(pfd)
                if (renderer.pageCount > 0) {
                    val page = renderer.openPage(0)
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    renderer.close()
                    
                    FileOutputStream(thumbnailFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 80, out)
                        bitmap.recycle()
                    }
                    return@withContext thumbnailFile.absolutePath
                }
                renderer.close()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to create thumbnail")
        }
        null
    }

    // Create thumbnail from File
    fun createThumbnail(file: File): String? {
        return try {
            val thumbnailFile = File(context.cacheDir, "thumb_${file.nameWithoutExtension}.png")
            val document = PDDocument.load(file)
            if (document.numberOfPages > 0) {
                // Use PdfRenderer for better thumbnails
                val uri = Uri.fromFile(file)
                return createThumbnail(uri, thumbnailFile)
            }
            document.close()
            null
        } catch (e: Exception) {
            Timber.e(e, "Failed to create thumbnail from file")
            null
        }
    }

    // Get file name from URI
    fun getFileNameFromUri(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    // Get file extension
    fun getFileExtension(uri: Uri): String? {
        val name = getFileNameFromUri(uri)
        return name?.substringAfterLast('.', "")
    }

    // Get MIME type
    fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "pdf" -> MIME_PDF
            "doc", "docx" -> MIME_WORD
            "xls", "xlsx" -> MIME_EXCEL
            "ppt", "pptx" -> MIME_POWERPOINT
            "jpg", "jpeg", "png", "bmp", "webp" -> MIME_IMAGE
            else -> "application/octet-stream"
        }
    }

    fun getMimeType(uri: Uri): String {
        return context.contentResolver.getType(uri) ?: "application/octet-stream"
    }

    // Delete file
    fun deleteFile(file: File): Boolean {
        return try {
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete file")
            false
        }
    }

    // Get supported PDF file types description
    fun getSupportedPdfTypes(): List<String> {
        return listOf("PDF")
    }

    fun getSupportedImageTypes(): List<String> {
        return listOf("JPG", "PNG", "BMP", "WEBP")
    }

    fun getSupportedOfficeTypes(): List<String> {
        return listOf("DOC", "DOCX", "PPT", "PPTX", "XLS", "XLSX")
    }

    // Cleanup temp files
    fun cleanupTempFiles() {
        try {
            val cacheDir = getCacheDirectory()
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.startsWith("temp")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to cleanup temp files")
        }
    }
}
