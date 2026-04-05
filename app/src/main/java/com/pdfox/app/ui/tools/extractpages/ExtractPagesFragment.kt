package com.pdfox.app.ui.tools.extractpages

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.pdfox.app.R
import com.pdfox.app.databinding.FragmentPageSelectionBinding
import com.pdfox.app.ui.tools.BaseToolFragment
import com.pdfox.app.ui.tools.ToolUiState
import com.pdfox.app.util.FileManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class ExtractPagesFragment : BaseToolFragment() {

    private val viewModel: ExtractPagesViewModel by viewModels()

    private var _binding: FragmentPageSelectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PageThumbnailAdapter

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPageSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPageGrid()
        setupActionButtons()
        observeViewModel()
    }

    private fun setupPageGrid() {
        adapter = PageThumbnailAdapter(
            onSelectionChanged = { selectedPages ->
                viewModel.setSelectedPages(selectedPages)
                binding.tvSelectedCount.text = "${selectedPages.size} pages selected for extraction"
            }
        )
        binding.rvPages.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = this@ExtractPagesFragment.adapter
        }
        binding.tvActionLabel.text = "Select pages to EXTRACT"
        binding.tvActionLabel.visibility = View.VISIBLE
    }

    private fun setupActionButtons() {
        binding.btnSelectAll.setOnClickListener {
            adapter.selectAll()
        }
        binding.btnDeselectAll.setOnClickListener {
            adapter.deselectAll()
        }
    }

    override fun onFileSelected(uri: Uri) {
        viewModel.setSelectedFile(uri)
        loadPageThumbnails(uri)
    }

    private fun loadPageThumbnails(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val pageCount = fileManager.getPageCount(uri)
            viewModel.setPageCount(pageCount)

            val thumbnails = mutableListOf<PageThumbnail>()
            for (i in 0 until pageCount) {
                thumbnails.add(PageThumbnail(i + 1, false, null))
            }
            adapter.submitList(thumbnails)
            binding.rvPages.visibility = View.VISIBLE
        }
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
                        Timber.e(state.exception, "Extract pages failed: ${state.message}")
                    }
                }
            }
        }
    }

    private fun navigateToProcessing() {
        val uri = getSelectedUris().firstOrNull() ?: return
        val pagesToExtract = viewModel.getSelectedPages().toList()
        val pagesJson = pagesToExtract.joinToString(",") { it.toString() }

        val bundle = Bundle().apply {
            putString("toolType", getToolId())
            putString("fileUris", uri.toString())
            putString("pages_to_extract", pagesJson)
        }
        findNavController().navigate(R.id.toolOptionsFragment, bundle)
    }

    override fun getToolId() = "extract_pages"
    override fun getAcceptedMimeType() = FileManager.MIME_PDF
    override fun isMultiFile() = false
    override fun getSupportedFormats() = listOf("PDF")

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
