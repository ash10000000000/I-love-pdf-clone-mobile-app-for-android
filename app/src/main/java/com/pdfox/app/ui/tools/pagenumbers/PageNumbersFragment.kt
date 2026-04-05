package com.pdfox.app.ui.tools.pagenumbers

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.pdfox.app.R
import com.pdfox.app.databinding.FragmentPageNumbersBinding
import com.pdfox.app.ui.tools.BaseToolFragment
import com.pdfox.app.ui.tools.ToolUiState
import com.pdfox.app.util.FileManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class PageNumbersFragment : BaseToolFragment() {

    private val viewModel: PageNumbersViewModel by viewModels()

    private var _binding: FragmentPageNumbersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPageNumbersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupOptions()
        observeViewModel()
    }

    private fun setupOptions() {
        binding.sliderFontSize.addOnChangeListener { _, value, _ ->
            binding.tvFontSize.text = "${value.toInt()} pt"
            viewModel.setFontSize(value.toInt())
        }
        binding.sliderFontSize.value = 12f
        viewModel.setFontSize(12)
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
                        Timber.e(state.exception, "Page numbers failed: ${state.message}")
                    }
                }
            }
        }
    }

    private fun navigateToProcessing() {
        val uri = getSelectedUris().firstOrNull() ?: return

        val position = when (binding.rgPosition.checkedRadioButtonId) {
            R.id.rb_bottom_left -> "bottom_left"
            R.id.rb_bottom_right -> "bottom_right"
            R.id.rb_top_left -> "top_left"
            R.id.rb_top_right -> "top_right"
            R.id.rb_top_center -> "top_center"
            else -> "bottom_center"
        }

        val startNumber = binding.etStartNumber.text?.toString()?.toIntOrNull() ?: 1
        val fontSize = viewModel.getFontSize()

        viewModel.setPosition(position)
        viewModel.setStartNumber(startNumber)

        val bundle = Bundle().apply {
            putString("toolType", getToolId())
            putString("fileUris", uri.toString())
            putString("position", position)
            putInt("start_number", startNumber)
            putInt("font_size", fontSize)
        }
        findNavController().navigate(R.id.toolOptionsFragment, bundle)
    }

    override fun getToolId() = "page_numbers"
    override fun getAcceptedMimeType() = FileManager.MIME_PDF
    override fun isMultiFile() = false
    override fun getSupportedFormats() = listOf("PDF")

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
