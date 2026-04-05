package com.pdfox.app.ui.tools.htmltopdf

import com.pdfox.app.ui.tools.BaseToolFragment

class HtmlToPdfFragment : BaseToolFragment() {
    override fun getToolId(): String = "html_to_pdf"
    override fun getAcceptedMimeType(): String = "text/html"
    override fun isMultiFile(): Boolean = false
    override fun getSupportedFormats(): List<String> = listOf("HTML", "HTM")
}
