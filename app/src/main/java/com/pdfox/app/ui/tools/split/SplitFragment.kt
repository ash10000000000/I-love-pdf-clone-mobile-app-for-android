package com.pdfox.app.ui.tools.split

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.pdfox.app.R
import com.pdfox.app.databinding.FragmentSplitBinding
import com.pdfox.app.ui.tools.BaseToolFragment
import com.pdfox.app.ui.tools.ToolUiState
import com.pdfox.app.util.FileManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class SplitFragment : BaseToolFragment() {

    private val viewModel: SplitViewModel by viewModels()

    private var _binding: FragmentSplitBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSplitOptions()
        observeViewModel()
    }

    private fun setupSplitOptions() {
        binding.rgSplitMode.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_split_by_range -> {
                    binding.etPageRanges.visibility = View.VISIBLE
                    binding.tvRangeHint.visibility = View.VISIBLE
                }
                R.id.rb_extract_pages -> {
                    binding.etPageRanges.visibility = View.GONE
                    binding.tvRangeHint.visibility = View.GONE
                }
            }
        }

        binding.etPageRanges.doAfterTextChanged { text ->
            viewModel.setPageRanges(text?.toString() ?: "")
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
                    is ToolUiState.Loading -> {
                        navigateToProcessing()
                    }
                    is ToolUiState.Success -> {
                        findNavController().navigateUp()
                    }
                    is ToolUiState.Error -> {
                        Timber.e(state.exception, "Split failed: ${state.message}")
                    }
                }
            }
        }
    }

    private fun navigateToProcessing() {
        val uri = getSelectedUris().firstOrNull() ?: return
        val mode = if (binding.rgSplitMode.checkedRadioButtonId == R.id.rb_split_by_range) "range" else "extract"
        val ranges = binding.etPageRanges.text.toString()

        val uriString = uri.toString()
        val bundle = Bundle().apply {
            putString("toolType", getToolId())
            putString("fileUris", uriString)
            putString("split_mode", mode)
            putString("split_ranges", ranges)
        }
        findNavController().navigate(R.id.toolOptionsFragment, bundle)
    }

    override fun getToolId() = "split"
    override fun getAcceptedMimeType() = FileManager.MIME_PDF
    override fun isMultiFile() = false
    override fun getSupportedFormats() = listOf("PDF")

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
