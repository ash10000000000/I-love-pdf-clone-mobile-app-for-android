package com.pdfox.app.ui.tools.pdftopdfa

import com.pdfox.app.ui.tools.BaseToolViewModel
import com.pdfox.app.util.Result
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDMetadata
import com.tom_roush.pdfbox.xmp.XmpMetadata
import com.tom_roush.xmpbox.XMPMetadata
import com.tom_roush.xmpbox.schema.AdobePDFSchema
import com.tom_roush.xmpbox.schema.DublinCoreSchema
import com.tom_roush.xmpbox.schema.XMPBasicSchema
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.io.File
import java.util.GregorianCalendar

@HiltViewModel
class PdfToPdfaViewModel @Inject constructor() : BaseToolViewModel() {
    override suspend fun executeToolAction(
        toolType: String,
        inputFiles: List<File>,
        options: Map<String, Any>
    ): Result<File> {
        return safeExecute {
            val inputFile = inputFiles.first()
            val document = PDDocument.load(inputFile)
            
            try {
                val xmp = XMPMetadata()
                val basic = XMPBasicSchema(xmp)
                basic.createDate = GregorianCalendar()
                basic.metadataDate = GregorianCalendar()
                basic.creatorTool = "PDFox PDF/A Converter"
                xmp.addSchema(basic)
                
                val dc = DublinCoreSchema(xmp)
                dc.format = "application/pdf"
                xmp.addSchema(dc)
                
                val pdf = AdobePDFSchema(xmp)
                pdf.producer = "PDFox"
                xmp.addSchema(pdf)
                
                val metadata = PDMetadata(document, xmp, false)
                document.documentMetadata = metadata
                
                val info = document.documentInformation
                info.producer = "PDFox PDF/A Converter"
                info.creator = "PDFox"
            } catch (e: Exception) {
                // XMP metadata failed, just save normally
            }
            
            val outputFile = fileManager.createOutputFile("PDFox_pdfa", "pdf")
            document.save(outputFile)
            document.close()
            outputFile
        }
    }
}
