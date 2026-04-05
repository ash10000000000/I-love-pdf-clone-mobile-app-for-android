package com.pdfox.app.ui.recent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.pdfox.app.R
import com.pdfox.app.data.db.RecentFile
import com.pdfox.app.databinding.FragmentRecentBinding
import com.pdfox.app.util.gone
import com.pdfox.app.util.show
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class RecentFragment : Fragment() {

    private var _binding: FragmentRecentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecentViewModel by viewModels()
    private lateinit var adapter: RecentFileAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeToDelete()
        observeState()
    }

    private fun setupRecyclerView() {
        adapter = RecentFileAdapter(
            onItemClick = { recentFile ->
                navigateToViewer(recentFile)
            },
            onThumbnailClick = { recentFile ->
                navigateToViewer(recentFile)
            }
        )

        binding.rvRecent.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecent.adapter = adapter
    }

    private fun setupSwipeToDelete() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val recentFile = adapter.currentList.getOrNull(position)

                if (recentFile != null) {
                    viewModel.deleteFile(recentFile)
                    showUndoSnackbar(recentFile)
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeCallback)
        itemTouchHelper.attachToRecyclerView(binding.rvRecent)
    }

    private fun showUndoSnackbar(recentFile: RecentFile) {
        val snackbar = Snackbar.make(
            binding.root,
            getString(R.string.file_deleted),
            Snackbar.LENGTH_LONG
        ).setAction(getString(R.string.undo)) {
            viewModel.restoreFile()
            viewModel.clearDeletedFile()
        }.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                if (event != DISMISS_EVENT_ACTION) {
                    viewModel.clearDeletedFile()
                }
            }
        })

        snackbar.show()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recentFiles.collectLatest { files ->
                adapter.submitList(files)
                updateEmptyState(files.isEmpty())
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.llEmptyState.show()
            binding.rvRecent.gone()
        } else {
            binding.llEmptyState.gone()
            binding.rvRecent.show()
        }
    }

    private fun navigateToViewer(recentFile: RecentFile) {
        val outputFile = java.io.File(recentFile.outputFilePath)
        if (outputFile.exists()) {
            val action = RecentFragmentDirections.actionRecentFragmentToPdfViewerFragment(
                filePath = outputFile.absolutePath
            )
            findNavController().navigate(action)
        } else {
            Timber.w("Output file not found: ${recentFile.outputFilePath}")
            Snackbar.make(
                binding.root,
                "File not found",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
