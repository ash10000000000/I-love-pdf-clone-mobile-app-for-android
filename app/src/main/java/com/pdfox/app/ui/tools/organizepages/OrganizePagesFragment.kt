package com.pdfox.app.ui.tools.organizepages

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.pdfox.app.R
import com.pdfox.app.databinding.FragmentOrganizePagesBinding
import com.pdfox.app.ui.tools.BaseToolFragment
import com.pdfox.app.ui.tools.ToolUiState
import com.pdfox.app.util.FileManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class OrganizePagesFragment : BaseToolFragment() {

    private val viewModel: OrganizePagesViewModel by viewModels()

    private var _binding: FragmentOrganizePagesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: OrganizePagesAdapter

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrganizePagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPageGrid()
        observeViewModel()
    }

    private fun setupPageGrid() {
        adapter = OrganizePagesAdapter(
            onPageRotated = { pageNumber, rotation ->
                viewModel.rotatePage(pageNumber, rotation)
            }
        )
        binding.rvPages.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = this@OrganizePagesFragment.adapter
        }

        val callback = OrganizeItemTouchHelperCallback(adapter)
        ItemTouchHelper(callback).attachToRecyclerView(binding.rvPages)
    }

    override fun onFileSelected(uri: Uri) {
        viewModel.setSelectedFile(uri)
        loadPages(uri)
    }

    private fun loadPages(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val pageCount = fileManager.getPageCount(uri)
            viewModel.setPageCount(pageCount)

            val pages = mutableListOf<OrganizePageItem>()
            for (i in 0 until pageCount) {
                pages.add(OrganizePageItem(i + 1, 0, null))
            }
            adapter.submitList(pages)
            binding.rvPages.visibility = View.VISIBLE
            binding.btnReorderAndSave.isEnabled = true
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
                        Timber.e(state.exception, "Organize pages failed: ${state.message}")
                    }
                }
            }
        }
    }

    private fun navigateToProcessing() {
        val uri = getSelectedUris().firstOrNull() ?: return
        val pageOrder = adapter.getPageOrder()
        val pageRotations = adapter.getPageRotations()

        val orderJson = pageOrder.joinToString(",") { it.toString() }
        val rotationsJson = pageRotations.entries.joinToString(",") { "${it.key}:${it.value}" }

        val bundle = Bundle().apply {
            putString("toolType", getToolId())
            putString("fileUris", uri.toString())
            putString("page_order", orderJson)
            putString("page_rotations", rotationsJson)
        }
        findNavController().navigate(R.id.toolOptionsFragment, bundle)
    }

    override fun getToolId() = "organize_pages"
    override fun getAcceptedMimeType() = FileManager.MIME_PDF
    override fun isMultiFile() = false
    override fun getSupportedFormats() = listOf("PDF")

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
