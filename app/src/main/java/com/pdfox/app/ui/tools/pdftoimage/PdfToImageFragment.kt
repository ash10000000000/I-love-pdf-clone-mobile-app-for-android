package com.pdfox.app.ui.tools.pdftoimage

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.pdfox.app.R
import com.pdfox.app.ui.tools.BaseToolFragment
import com.pdfox.app.ui.tools.ToolUiState
import com.pdfox.app.util.FileManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class PdfToImageFragment : BaseToolFragment() {

    private val viewModel: PdfToImageViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is ToolUiState.Idle -> {}
                    is ToolUiState.Loading -> navigateToProcessing()
                    is ToolUiState.Success -> findNavController().navigateUp()
                    is ToolUiState.Error -> {
                        Timber.e(state.exception, "PDF to Image failed: ${state.message}")
                    }
                }
            }
        }
    }

    private fun navigateToProcessing() {
        val uri = getSelectedUris().firstOrNull() ?: return
        val bundle = Bundle().apply {
            putString("toolType", getToolId())
            putString("fileUris", uri.toString())
        }
        findNavController().navigate(R.id.toolOptionsFragment, bundle)
    }

    override fun getToolId(): String = "pdf_to_image"
    override fun getAcceptedMimeType(): String = FileManager.MIME_PDF
    override fun isMultiFile(): Boolean = false
    override fun getSupportedFormats(): List<String> = listOf("PDF")
}
