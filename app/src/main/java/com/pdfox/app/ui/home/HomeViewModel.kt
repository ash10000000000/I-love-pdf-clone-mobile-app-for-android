package com.pdfox.app.ui.home

import androidx.lifecycle.ViewModel
import com.pdfox.app.R
import com.pdfox.app.util.ToolCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class HomeUiState(
    val tools: List<ToolItem> = ALL_TOOLS,
    val filteredTools: List<ToolItem> = ALL_TOOLS,
    val searchQuery: String = "",
    val selectedCategory: ToolCategory? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun selectCategory(category: ToolCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        var filtered = state.tools

        if (state.selectedCategory != null) {
            filtered = filtered.filter { it.category == state.selectedCategory }
        }

        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.lowercase()
            filtered = filtered.filter {
                it.name.lowercase().contains(query) ||
                it.description.lowercase().contains(query)
            }
        }

        _uiState.value = state.copy(filteredTools = filtered)
    }

    companion object {
        val ALL_TOOLS = listOf(
            // Organize
            ToolItem("merge", "Merge PDF", "Combine multiple PDFs into one", android.R.drawable.ic_menu_add, ToolCategory.ORGANIZE, "mergeFragment"),
            ToolItem("split", "Split PDF", "Extract pages or split by range", android.R.drawable.ic_menu_cut, ToolCategory.ORGANIZE, "splitFragment"),
            ToolItem("remove_pages", "Remove Pages", "Delete unwanted pages from PDF", android.R.drawable.ic_menu_delete, ToolCategory.ORGANIZE, "removePagesFragment"),
            ToolItem("extract_pages", "Extract Pages", "Select and extract specific pages", android.R.drawable.ic_menu_upload, ToolCategory.ORGANIZE, "extractPagesFragment"),
            ToolItem("organize_pages", "Organize Pages", "Reorder and rotate pages", android.R.drawable.ic_menu_sort_by_size, ToolCategory.ORGANIZE, "organizePagesFragment"),
            
            // Optimize
            ToolItem("compress", "Compress PDF", "Reduce file size while keeping quality", android.R.drawable.ic_menu_zoom, ToolCategory.OPTIMIZE, "compressFragment"),
            
            // Convert FROM PDF
            ToolItem("pdf_to_word", "PDF to Word", "Convert PDF to DOCX", android.R.drawable.ic_menu_edit, ToolCategory.CONVERT, "pdfToWordFragment"),
            ToolItem("pdf_to_ppt", "PDF to PowerPoint", "Convert PDF to PPTX", android.R.drawable.ic_menu_slideshow, ToolCategory.CONVERT, "pdfToPptFragment"),
            ToolItem("pdf_to_excel", "PDF to Excel", "Convert PDF to XLSX", android.R.drawable.ic_menu_agenda, ToolCategory.CONVERT, "pdfToExcelFragment"),
            ToolItem("pdf_to_image", "PDF to Image", "Export pages as JPG/PNG", android.R.drawable.ic_menu_gallery, ToolCategory.CONVERT, "pdfToImageFragment"),
            ToolItem("pdf_to_pdfa", "PDF to PDF/A", "Convert to archival format", android.R.drawable.ic_menu_save, ToolCategory.CONVERT, "pdfToPdfaFragment"),
            
            // Convert TO PDF
            ToolItem("word_to_pdf", "Word to PDF", "Convert DOC/DOCX to PDF", android.R.drawable.ic_menu_edit, ToolCategory.CONVERT, "wordToPdfFragment"),
            ToolItem("ppt_to_pdf", "PowerPoint to PDF", "Convert PPT/PPTX to PDF", android.R.drawable.ic_menu_slideshow, ToolCategory.CONVERT, "pptToPdfFragment"),
            ToolItem("excel_to_pdf", "Excel to PDF", "Convert XLS/XLSX to PDF", android.R.drawable.ic_menu_agenda, ToolCategory.CONVERT, "excelToPdfFragment"),
            ToolItem("image_to_pdf", "Image to PDF", "Convert images to PDF", android.R.drawable.ic_menu_gallery, ToolCategory.CONVERT, "imageToPdfFragment"),
            ToolItem("html_to_pdf", "HTML to PDF", "Convert web pages to PDF", android.R.drawable.ic_menu_search, ToolCategory.CONVERT, "htmlToPdfFragment"),
            
            // Security
            ToolItem("protect", "Protect PDF", "Add password encryption", android.R.drawable.ic_lock_idle_lock, ToolCategory.SECURITY, "protectFragment"),
            ToolItem("unlock", "Unlock PDF", "Remove password protection", android.R.drawable.ic_lock_idle_unlock, ToolCategory.SECURITY, "unlockFragment"),
            ToolItem("sign", "Sign PDF", "Add your signature", android.R.drawable.ic_menu_edit, ToolCategory.SECURITY, "signFragment"),
            ToolItem("redact", "Redact PDF", "Remove sensitive information", android.R.drawable.ic_menu_close_clear_cancel, ToolCategory.SECURITY, "redactFragment"),
            
            // Edit
            ToolItem("rotate", "Rotate PDF", "Change page orientation", android.R.drawable.ic_menu_rotate, ToolCategory.EDIT, "rotateFragment"),
            ToolItem("page_numbers", "Add Page Numbers", "Number your pages", android.R.drawable.ic_menu_today, ToolCategory.EDIT, "pageNumbersFragment"),
            ToolItem("watermark", "Add Watermark", "Overlay text or image watermark", android.R.drawable.ic_menu_compass, ToolCategory.EDIT, "watermarkFragment"),
            ToolItem("metadata", "Edit Metadata", "Modify PDF properties", android.R.drawable.ic_menu_info_details, ToolCategory.EDIT, "metadataFragment"),
            ToolItem("repair", "Repair PDF", "Fix corrupted PDF files", android.R.drawable.ic_menu_tools, ToolCategory.EDIT, "repairFragment"),
        )
    }
}
