package com.pdfox.app.ui.tools.rotate

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.pdfox.app.R
import com.pdfox.app.databinding.FragmentRotateBinding
import com.pdfox.app.ui.tools.BaseToolFragment
import com.pdfox.app.ui.tools.ToolUiState
import com.pdfox.app.util.FileManager
import com.pdfox.app.util.gone
import com.pdfox.app.util.show
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class RotateFragment : BaseToolFragment() {

    private val viewModel: RotateViewModel by viewModels()

    private var _binding: FragmentRotateBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRotateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupOptions()
        observeViewModel()
    }

    private fun setupOptions() {
        binding.rgPages.setOnCheckedChangeListener { _, checkedId ->
            binding.tilSelectedPages.visibility = if (checkedId == R.id.rb_selected) View.VISIBLE else View.GONE
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
                        Timber.e(state.exception, "Rotate failed: ${state.message}")
                    }
                }
            }
        }
    }

    private fun navigateToProcessing() {
        val uri = getSelectedUris().firstOrNull() ?: return

        val rotation = when (binding.rgRotation.checkedRadioButtonId) {
            R.id.rb_90_ccw -> -90
            R.id.rb_180 -> 180
            else -> 90
        }

        val allPages = binding.rgPages.checkedRadioButtonId == R.id.rb_all_pages
        val selectedPages = binding.etSelectedPages.text?.toString()?.trim() ?: ""

        viewModel.setRotation(rotation)
        viewModel.setApplyToAll(allPages)
        viewModel.setSelectedPages(selectedPages)

        val bundle = Bundle().apply {
            putString("toolType", getToolId())
            putString("fileUris", uri.toString())
            putInt("rotation", rotation)
            putBoolean("all_pages", allPages)
            putString("selected_pages", selectedPages)
        }
        findNavController().navigate(R.id.toolOptionsFragment, bundle)
    }

    override fun getToolId() = "rotate"
    override fun getAcceptedMimeType() = FileManager.MIME_PDF
    override fun isMultiFile() = false
    override fun getSupportedFormats() = listOf("PDF")

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
