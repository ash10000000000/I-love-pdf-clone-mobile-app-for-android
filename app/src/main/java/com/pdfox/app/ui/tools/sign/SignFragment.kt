package com.pdfox.app.ui.tools.sign

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.pdfox.app.R
import com.pdfox.app.databinding.FragmentSignBinding
import com.pdfox.app.ui.tools.BaseToolFragment
import com.pdfox.app.ui.tools.ToolUiState
import com.pdfox.app.util.FileManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class SignFragment : BaseToolFragment() {

    private val viewModel: SignViewModel by viewModels()

    private var _binding: FragmentSignBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSignatureView()
        observeViewModel()
    }

    private fun setupSignatureView() {
        binding.btnClearSignature.setOnClickListener {
            binding.signatureView.clear()
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
                        Timber.e(state.exception, "Sign failed: ${state.message}")
                    }
                }
            }
        }
    }

    private fun navigateToProcessing() {
        val uri = getSelectedUris().firstOrNull() ?: return

        if (!binding.signatureView.hasSignature()) {
            Timber.w("No signature drawn")
            return
        }

        val pageNumber = binding.etPageNumber.text?.toString()?.toIntOrNull() ?: 1
        val signatureBitmap = binding.signatureView.getSignatureBitmap(300, 150)

        viewModel.setSignatureBitmap(signatureBitmap)
        viewModel.setPageNumber(pageNumber)

        val bundle = Bundle().apply {
            putString("toolType", getToolId())
            putString("fileUris", uri.toString())
            putInt("page_number", pageNumber)
        }
        findNavController().navigate(R.id.toolOptionsFragment, bundle)
    }

    override fun getToolId() = "sign"
    override fun getAcceptedMimeType() = FileManager.MIME_PDF
    override fun isMultiFile() = false
    override fun getSupportedFormats() = listOf("PDF")

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
