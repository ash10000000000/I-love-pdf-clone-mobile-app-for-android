package com.pdfox.app.ui.tools.redact

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.pdfox.app.R
import com.pdfox.app.databinding.FragmentRedactBinding
import com.pdfox.app.ui.tools.BaseToolFragment
import com.pdfox.app.ui.tools.ToolUiState
import com.pdfox.app.util.FileManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class RedactFragment : BaseToolFragment() {

    private val viewModel: RedactViewModel by viewModels()

    private var _binding: FragmentRedactBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRedactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClearButton()
        observeViewModel()
    }

    private fun setupClearButton() {
        binding.btnClearRedactions.setOnClickListener {
            viewModel.clearRedactions()
        }
    }

    override fun onFileSelected(uri: Uri) {
        viewModel.setSelectedFile(uri)
        binding.uploadZone.isEnabled = true
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is ToolUiState.Idle -> {}
                    is ToolUiState.Loading -> navigateToProcessing()
                    is ToolUiState.Success -> findNavController().navigateUp()
                    is ToolUiState.Error -> {
                        Timber.e(state.exception, "Redact failed: ${state.message}")
                    }
                }
            }
        }
    }

    private fun navigateToProcessing() {
        val uri = getSelectedUris().firstOrNull() ?: return
        val redactions = viewModel.getRedactions()

        val bundle = Bundle().apply {
            putString("toolType", getToolId())
            putString("fileUris", uri.toString())
            // Redaction data would be passed from a custom view
            // For now, we pass a default redaction
        }
        findNavController().navigate(R.id.toolOptionsFragment, bundle)
    }

    override fun getToolId() = "redact"
    override fun getAcceptedMimeType() = FileManager.MIME_PDF
    override fun isMultiFile() = false
    override fun getSupportedFormats() = listOf("PDF")

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
