package com.pdfox.app.ui.tools.imagetopdf

import com.pdfox.app.ui.tools.BaseToolFragment

class ImageToPdfFragment : BaseToolFragment() {
    override fun getToolId(): String = "image_to_pdf"
    override fun getAcceptedMimeType(): String = "image/*"
    override fun isMultiFile(): Boolean = true
    override fun getSupportedFormats(): List<String> = listOf("JPG", "PNG", "BMP", "WEBP")
}
