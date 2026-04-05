package com.pdfox.app.ui.processing

import com.pdfox.app.util.FileManager
import java.io.File

interface ToolProcessor {
    suspend fun process(inputFiles: List<File>, outputFile: File): File
}

object ToolProcessorFactory {
    fun create(toolType: String, fileManager: FileManager): ToolProcessor {
        return when (toolType) {
            "merge" -> MergeProcessor(fileManager)
            "split" -> SplitProcessor(fileManager)
            "compress" -> CompressProcessor(fileManager)
            "pdf_to_word" -> PdfToWordProcessor(fileManager)
            "pdf_to_ppt" -> PdfToPptProcessor(fileManager)
            "pdf_to_excel" -> PdfToExcelProcessor(fileManager)
            "pdf_to_image" -> PdfToImageProcessor(fileManager)
            "pdf_to_pdfa" -> PdfToPdfaProcessor(fileManager)
            "word_to_pdf" -> WordToPdfProcessor(fileManager)
            "ppt_to_pdf" -> PptToPdfProcessor(fileManager)
            "excel_to_pdf" -> ExcelToPdfProcessor(fileManager)
            "image_to_pdf" -> ImageToPdfProcessor(fileManager)
            "html_to_pdf" -> HtmlToPdfProcessor(fileManager)
            "protect" -> ProtectProcessor(fileManager)
            "unlock" -> UnlockProcessor(fileManager)
            "rotate" -> RotateProcessor(fileManager)
            "repair" -> RepairProcessor(fileManager)
            "remove_pages" -> RemovePagesProcessor(fileManager)
            "extract_pages" -> ExtractPagesProcessor(fileManager)
            "sign" -> SignProcessor(fileManager)
            "redact" -> RedactProcessor(fileManager)
            "page_numbers" -> PageNumbersProcessor(fileManager)
            "watermark" -> WatermarkProcessor(fileManager)
            "metadata" -> MetadataProcessor(fileManager)
            "organize_pages" -> OrganizePagesProcessor(fileManager)
            else -> throw IllegalArgumentException("Unknown tool type: $toolType")
        }
    }
}

// ═══════════════════════════════════════════
// ORGANIZE TOOLS
// ═══════════════════════════════════════════

class MergeProcessor(private val fileManager: FileManager) : ToolProcessor {
    override suspend fun process(inputFiles: List<File>, outputFile: File): File {
        val pdfMerger = com.tom_roush.pdfbox.multipdf.PDFMergerUtility()
        pdfMerger.destinationFileName = outputFile.absolutePath
        inputFiles.forEach { pdfMerger.addSource(it.absolutePath) }
        pdfMerger.mergeDocuments(null)
        return outputFile
    }
}

class SplitProcessor(private val fileManager: FileManager) : ToolProcessor {
    override suspend fun process(inputFiles: List<File>, outputFile: File): File {
        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputFiles.first())
        val splitter = com.tom_roush.pdfbox.multipdf.Splitter(1)
        val documents = splitter.split(document)
        if (documents.isNotEmpty()) {
            documents.first().save(outputFile)
            documents.forEach { it.close() }
        }
        document.close()
        return outputFile
    }
}

class RemovePagesProcessor(private val fileManager: FileManager) : ToolProcessor {
    override suspend fun process(inputFiles: List<File>, outputFile: File): File {
        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputFiles.first())
        // Remove last page as default behavior
        if (document.numberOfPages > 1) {
            document.removePage(document.numberOfPages - 1)
        }
        document.save(outputFile)
        document.close()
        return outputFile
    }
}

class ExtractPagesProcessor(private val fileManager: FileManager) : ToolProcessor {
    override suspend fun process(inputFiles: List<File>, outputFile: File): File {
        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputFiles.first())
        val newDoc = com.tom_roush.pdfbox.pdmodel.PDDocument()
        // Extract first page as default
        if (document.numberOfPages > 0) {
            newDoc.addPage(document.getPage(0))
        }
        newDoc.save(outputFile)
        newDoc.close()
        document.close()
        return outputFile
    }
}

class OrganizePagesProcessor(private val fileManager: FileManager) : ToolProcessor {
    override suspend fun process(inputFiles: List<File>, outputFile: File): File {
        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputFiles.first())
        document.save(outputFile)
        document.close()
        return outputFile
    }
}

// ═══════════════════════════════════════════
// OPTIMIZE TOOLS
// ═══════════════════════════════════════════

class CompressProcessor(private val fileManager: FileManager) : ToolProcessor {
    override suspend fun process(inputFiles: List<File>, outputFile: File): File {
        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputFiles.first())
        document.documentInformation.creator = "PDFox"
        document.save(outputFile)
        document.close()
        return outputFile
    }
}

// ═══════════════════════════════════════════
// CONVERT FROM PDF TOOLS
// ═══════════════════════════════════════════

class PdfToWordProcessor(private val fileManager: FileManager) : ToolProcessor {
    override suspend fun process(inputFiles: List<File>, outputFile: File): File {
        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputFiles.first())
        val text = com.tom_roush.pdfbox.text.PDFTextStripper().getText(document)
        document.close()

        org.apache.poi.xwpf.usermodel.XWPFDocument().use { doc ->
            val paragraph = doc.createParagraph()
            val run = paragraph.createRun()
            run.setText(text)
            outputFile.outputStream().use { doc.write(it) }
        }
        return outputFile
    }
}

class PdfToPptProcessor(private val fileManager: FileManager) : ToolProcessor {
    override suspend fun process(inputFiles: List<File>, outputFile: File): File {
        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputFiles.first())
        val renderer = com.tom_roush.pdfbox.rendering.PDFRenderer(document)

        org.apache.poi.xslf.usermodel.XMLSlideShow().use { pptx ->
            for (i in 0 until document.numberOfPages) {
                val bitmap = renderer.renderImageWithDPI(i, 150, android.graphics.Bitmap.Config.ARGB_8888)
                val slide = pptx.createSlide()
                val imageBytes = run {
                    val baos = java.io.ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, baos)
                    baos.toByteArray()
                }
                val idx = pptx.addPicture(imageBytes, org.apache.poi.sl.usermodel.PictureData.PictureType.PNG)
                slide.createPicture(idx)
                bitmap.recycle()
            }
            outputFile.outputStream().use { pptx.write(it) }
        }
        document.close()
        return outputFile
    }
}

class PdfToExcelProcessor(private val fileManager: FileManager) : ToolProcessor {
    override suspend fun process(inputFiles: List<File>, outputFile: File): File {
        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputFiles.first())
        val text = com.tom_roush.pdfbox.text.PDFTextStripper().getText(document)
        document.close()

        org.apache.poi.xssf.usermodel.XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet("PDF Data")
            val lines = text.lines()
            lines.forEachIndexed { rowIndex, line ->
                val row = sheet.createRow(rowIndex)
                val cell = row.createCell(0)
                cell.setCellValue(line)
            }
            outputFile.outputStream().use { workbook.write(it) }
        }
        return outputFile
    }
}

class PdfToImageProcessor(private val fileManager: FileManager) : ToolProcessor {
    override suspend fun process(inputFiles: List<File>, outputFile: File): File {
        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputFiles.first())
        val renderer = com.tom_roush.pdfbox.rendering.PDFRenderer(document)
        val outputDir = fileManager.getCacheDirectory()
        val imageFiles = mutableListOf<File>()

        for (i in 0 until document.numberOfPages) {
            val bitmap = renderer.renderImageWithDPI(i, 150, android.graphics.Bitmap.Config.ARGB_8888)
            val imageFile = File(outputDir, "page_${i + 1}.png")
            imageFile.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            imageFiles.add(imageFile)
        }
        document.close()

        val zipFile = net.lingala.zip4j.ZipFile(outputFile)
        zipFile.addFiles(imageFiles)
        imageFiles.forEach { it.delete() }
        return outputFile
    }
}

class PdfToPdfaProcessor(private val fileManager: FileManager) : ToolProcessor {
    override suspend fun process(inputFiles: List<File>, outputFile: File): File {
        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputFiles.first())
        val info = document.documentInformation
        info.producer = "PDFox PDF/A Converter"
        info.creator = "PDFox"
        document.save(outputFile)
        document.close()
        return outputFile
    }
}

// ═══════════════════════════════════════════
// CONVERT TO PDF TOOLS
// ═══════════════════════════════════════════

class WordToPdfProcessor(private val fileManager: FileManager) : ToolProcessor {
    override suspend fun process(inputFiles: List<File>, outputFile: File): File {
        val textExtractor = org.apache.poi.xwpf.extractor.XWPFWordExtractor(
            org.apache.poi.xwpf.usermodel.XWPFDocument(java.io.FileInputStream(inputFiles.first()))
        )
        val text = textExtractor.text
        textExtractor.close()

        com.tom_roush.pdfbox.pdmodel.PDDocument().use { doc ->
            var page = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.A4)
            doc.addPage(page)

            com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, page).use { cs ->
                cs.beginText()
                cs.newLineAtOffset(50f, 750f)
                cs.setFont(com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12f)

                var yPos = 750f
                text.lines().forEach { line ->
                    if (yPos < 50f) {
                        cs.endText()
                        page = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.A4)
                        doc.addPage(page)
                        cs.beginText()
                        cs.setFont(com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12f)
                        yPos = 750f
                    }
                    cs.newLineAtOffset(0f, -15f)
                    cs.showText(line.take(80))
                    yPos -= 15f
                }
                cs.endText()
            }
            doc.save(outputFile)
        }
        return outputFile
    }
}

class PptToPdfProcessor(private val fileManager: FileManager) : ToolProcessor {
    override suspend fun process(inputFiles: List<File>, outputFile: File): File {
        val pptx = org.apache.poi.xslf.usermodel.XMLSlideShow(java.io.FileInputStream(inputFiles.first()))

        com.tom_roush.pdfbox.pdmodel.PDDocument().use { doc ->
            for (i in 0 until pptx.numberOfSlides) {
                val slide = pptx.getSlide(i)
                val text = slide.title ?: "Slide ${i + 1}"

                val page = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.A4)
                doc.addPage(page)

                com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, page).use { cs ->
                    cs.beginText()
                    cs.newLineAtOffset(50f, 750f)
                    cs.setFont(com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 18f)
                    cs.showText(text)
                    cs.endText()
                }
            }
            pptx.close()
            doc.save(outputFile)
        }
        return outputFile
    }
}

class ExcelToPdfProcessor(private val fileManager: FileManager) : ToolProcessor {
    override suspend fun process(inputFiles: List<File>, outputFile: File): File {
        org.apache.poi.xssf.usermodel.XSSFWorkbook(java.io.FileInputStream(inputFiles.first())).use { workbook ->
            com.tom_roush.pdfbox.pdmodel.PDDocument().use { doc ->
                val sheet = workbook.getSheetAt(0)
                var page = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.A4)
                doc.addPage(page)
                var yPos = 750f

                com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, page).use { cs ->
                    cs.beginText()
                    cs.setFont(com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10f)
                    cs.newLineAtOffset(50f, yPos)

                    for (row in sheet) {
                        if (yPos < 50f) {
                            cs.endText()
                            page = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.A4)
                            doc.addPage(page)
                            cs.beginText()
                            cs.setFont(com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10f)
                            yPos = 750f
                            cs.newLineAtOffset(50f, yPos)
                        }
                        val rowText = (0 until row.physicalNumberOfCells).joinToString("\t") { cell ->
                            row.getCell(cell)?.toString() ?: ""
                        }
                        cs.showText(rowText)
                        cs.newLineAtOffset(0f, -12f)
                        yPos -= 12f
                    }
                    cs.endText()
                }
            }
        }
        return outputFile
    }
}

class ImageToPdfProcessor(private val fileManager: FileManager) : ToolProcessor {
    override suspend fun process(inputFiles: List<File>, outputFile: File): File {
        com.tom_roush.pdfbox.pdmodel.PDDocument().use { doc ->
            inputFiles.forEach { imageFile ->
                val bitmap = android.graphics.BitmapFactory.decodeFile(imageFile.absolutePath)
                if (bitmap != null) {
                    val width = bitmap.width.toFloat()
                    val height = bitmap.height.toFloat()
                    val page = com.tom_roush.pdfbox.pdmodel.PDPage(
                        com.tom_roush.pdfbox.pdmodel.common.PDRectangle(width, height)
                    )
                    doc.addPage(page)

                    val pdImage = com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(doc, bitmap)
                    com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, page).use { cs ->
                        cs.drawImage(pdImage, 0f, 0f, width, height)
                    }
                    bitmap.recycle()
                }
            }
            doc.save(outputFile)
        }
        return outputFile
    }
}

class HtmlToPdfProcessor(private val fileManager: FileManager) : ToolProcessor {
    override suspend fun process(inputFiles: List<File>, outputFile: File): File {
        val htmlContent = inputFiles.first().readText()
        val plainText = htmlContent.replace(Regex("<[^>]*>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        com.tom_roush.pdfbox.pdmodel.PDDocument().use { doc ->
            var page = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.A4)
            doc.addPage(page)

            com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, page).use { cs ->
                cs.beginText()
                cs.newLineAtOffset(50f, 750f)
                cs.setFont(com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12f)

                var yPos = 750f
                plainText.chunked(80).forEach { chunk ->
                    if (yPos < 50f) {
                        cs.endText()
                        page = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.A4)
                        doc.addPage(page)
                        cs.beginText()
                        cs.setFont(com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12f)
                        yPos = 750f
                    }
                    cs.newLineAtOffset(0f, -15f)
                    cs.showText(chunk)
                    yPos -= 15f
                }
                cs.endText()
            }
            doc.save(outputFile)
        }
        return outputFile
    }
}

// ═══════════════════════════════════════════
// SECURITY TOOLS
// ═══════════════════════════════════════════

class ProtectProcessor(private val fileManager: FileManager) : ToolProcessor {
    override suspend fun process(inputFiles: List<File>, outputFile: File): File {
        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputFiles.first())
        val accessPermission = com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission()
        val policy = com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy(
            "1234", "1234", accessPermission
        )
        policy.isEncryptMetadata = false
        document.protect(policy)
        document.save(outputFile)
        document.close()
        return outputFile
    }
}

class UnlockProcessor(private val fileManager: FileManager) : ToolProcessor {
    override suspend fun process(inputFiles: List<File>, outputFile: File): File {
        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputFiles.first(), "1234")
        document.setAllSecurityToBeRemoved(true)
        document.save(outputFile)
        document.close()
        return outputFile
    }
}

class SignProcessor(private val fileManager: FileManager) : ToolProcessor {
    override suspend fun process(inputFiles: List<File>, outputFile: File): File {
        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputFiles.first())
        document.save(outputFile)
        document.close()
        return outputFile
    }
}

class RedactProcessor(private val fileManager: FileManager) : ToolProcessor {
    override suspend fun process(inputFiles: List<File>, outputFile: File): File {
        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputFiles.first())
        document.save(outputFile)
        document.close()
        return outputFile
    }
}

// ═══════════════════════════════════════════
// EDIT TOOLS
// ═══════════════════════════════════════════

class RotateProcessor(private val fileManager: FileManager) : ToolProcessor {
    override suspend fun process(inputFiles: List<File>, outputFile: File): File {
        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputFiles.first())
        for (i in 0 until document.numberOfPages) {
            val page = document.getPage(i)
            page.rotation = (page.rotation + 90) % 360
        }
        document.save(outputFile)
        document.close()
        return outputFile
    }
}

class PageNumbersProcessor(private val fileManager: FileManager) : ToolProcessor {
    override suspend fun process(inputFiles: List<File>, outputFile: File): File {
        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputFiles.first())
        for (i in 0 until document.numberOfPages) {
            val page = document.getPage(i)
            val mediaBox = page.mediaBox
            com.tom_roush.pdfbox.pdmodel.PDPageContentStream(document, page,
                com.tom_roush.pdfbox.pdmodel.PDPageContentStream.AppendMode.APPEND, true).use { cs ->
                cs.beginText()
                cs.setFont(com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10f)
                cs.newLineAtOffset(
                    mediaBox.width / 2 - 10f,
                    mediaBox.lowerLeftY + 20f
                )
                cs.showText("${i + 1}")
                cs.endText()
            }
        }
        document.save(outputFile)
        document.close()
        return outputFile
    }
}

class WatermarkProcessor(private val fileManager: FileManager) : ToolProcessor {
    override suspend fun process(inputFiles: List<File>, outputFile: File): File {
        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputFiles.first())
        for (i in 0 until document.numberOfPages) {
            val page = document.getPage(i)
            com.tom_roush.pdfbox.pdmodel.PDPageContentStream(document, page,
                com.tom_roush.pdfbox.pdmodel.PDPageContentStream.AppendMode.APPEND, true).use { cs ->
                cs.beginText()
                cs.setFont(com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 50f)
                cs.setNonStrokingColor(0.8f, 0.8f, 0.8f)
                val mediaBox = page.mediaBox
                cs.newLineAtOffset(mediaBox.width / 2 - 100f, mediaBox.height / 2)
                cs.showText("WATERMARK")
                cs.endText()
            }
        }
        document.save(outputFile)
        document.close()
        return outputFile
    }
}

class MetadataProcessor(private val fileManager: FileManager) : ToolProcessor {
    override suspend fun process(inputFiles: List<File>, outputFile: File): File {
        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputFiles.first())
        val info = document.documentInformation
        info.creator = "PDFox"
        info.producer = "PDFox PDF Editor"
        document.save(outputFile)
        document.close()
        return outputFile
    }
}

class RepairProcessor(private val fileManager: FileManager) : ToolProcessor {
    override suspend fun process(inputFiles: List<File>, outputFile: File): File {
        val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputFiles.first())
        document.save(outputFile)
        document.close()
        return outputFile
    }
}
