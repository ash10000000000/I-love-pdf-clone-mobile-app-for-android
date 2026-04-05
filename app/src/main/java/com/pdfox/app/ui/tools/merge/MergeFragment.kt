package com.pdfox.app.ui.tools.merge

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pdfox.app.R
import com.pdfox.app.ui.tools.BaseToolFragment
import com.pdfox.app.ui.tools.ToolUiState
import com.pdfox.app.util.FileManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class MergeFragment : BaseToolFragment() {

    private val viewModel: MergeViewModel by viewModels()

    private lateinit var adapter: MergeFileAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = MergeFileAdapter(
            onItemMoved = { fromPos, toPos ->
                adapter.moveItem(fromPos, toPos)
            }
        )
        binding.rvSelectedFiles?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MergeFragment.adapter
            visibility = View.VISIBLE
        }

        val callback = MergeItemTouchHelperCallback(adapter)
        ItemTouchHelper(callback).attachToRecyclerView(binding.rvSelectedFiles)

        binding.tvSelectFile.text = "Select PDF files to merge"
        binding.tvDragDrop.text = "Tap to add multiple files"
    }

    override fun onFilesSelected(uris: List<Uri>) {
        adapter.submitFiles(uris)
        viewModel.setFiles(uris)
        binding.rvSelectedFiles?.visibility = View.VISIBLE
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
                        Timber.e(state.exception, "Merge failed: ${state.message}")
                    }
                }
            }
        }
    }

    private fun navigateToProcessing() {
        val orderedFiles = adapter.getOrderedFiles()
        if (orderedFiles.isEmpty()) return

        val uriString = orderedFiles.joinToString("|") { it.toString() }
        val bundle = Bundle().apply {
            putString("toolType", getToolId())
            putString("fileUris", uriString)
        }
        findNavController().navigate(R.id.toolOptionsFragment, bundle)
    }

    override fun getToolId() = "merge"
    override fun getAcceptedMimeType() = FileManager.MIME_PDF
    override fun isMultiFile() = true
    override fun getSupportedFormats() = listOf("PDF")
}
