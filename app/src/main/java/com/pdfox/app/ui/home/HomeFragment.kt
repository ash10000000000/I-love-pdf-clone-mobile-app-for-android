package com.pdfox.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.pdfox.app.R
import com.pdfox.app.databinding.FragmentHomeBinding
import com.pdfox.app.util.ToolCategory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var toolAdapter: ToolAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupCategoryChips()
        setupSearch()
        observeState()
    }

    private fun setupRecyclerView() {
        toolAdapter = ToolAdapter { tool ->
            navigateToTool(tool)
        }
        binding.rvTools.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.rvTools.adapter = toolAdapter
    }

    private fun setupCategoryChips() {
        binding.chipGroupCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            val selectedCategory = when (checkedIds.firstOrNull()) {
                R.id.chip_all -> null
                R.id.chip_organize -> ToolCategory.ORGANIZE
                R.id.chip_optimize -> ToolCategory.OPTIMIZE
                R.id.chip_convert -> ToolCategory.CONVERT
                R.id.chip_security -> ToolCategory.SECURITY
                R.id.chip_edit -> ToolCategory.EDIT
                else -> null
            }
            viewModel.selectCategory(selectedCategory)
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.updateSearchQuery(s?.toString() ?: "")
            }
        })
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                toolAdapter.submitList(state.filteredTools)
            }
        }
    }

    private fun navigateToTool(tool: ToolItem) {
        val action = when (tool.id) {
            "merge" -> R.id.action_home_to_merge
            "split" -> R.id.action_home_to_split
            "remove_pages" -> R.id.action_home_to_remove_pages
            "extract_pages" -> R.id.action_home_to_extract_pages
            "organize_pages" -> R.id.action_home_to_organize_pages
            "compress" -> R.id.action_home_to_compress
            "pdf_to_word" -> R.id.action_home_to_pdf_to_word
            "pdf_to_ppt" -> R.id.action_home_to_pdf_to_ppt
            "pdf_to_excel" -> R.id.action_home_to_pdf_to_excel
            "pdf_to_image" -> R.id.action_home_to_pdf_to_image
            "pdf_to_pdfa" -> R.id.action_home_to_pdf_to_pdfa
            "word_to_pdf" -> R.id.action_home_to_word_to_pdf
            "ppt_to_pdf" -> R.id.action_home_to_ppt_to_pdf
            "excel_to_pdf" -> R.id.action_home_to_excel_to_pdf
            "image_to_pdf" -> R.id.action_home_to_image_to_pdf
            "html_to_pdf" -> R.id.action_home_to_html_to_pdf
            "protect" -> R.id.action_home_to_protect
            "unlock" -> R.id.action_home_to_unlock
            "sign" -> R.id.action_home_to_sign
            "redact" -> R.id.action_home_to_redact
            "rotate" -> R.id.action_home_to_rotate
            "page_numbers" -> R.id.action_home_to_page_numbers
            "watermark" -> R.id.action_home_to_watermark
            "metadata" -> R.id.action_home_to_metadata
            "repair" -> R.id.action_home_to_repair
            else -> return
        }
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
