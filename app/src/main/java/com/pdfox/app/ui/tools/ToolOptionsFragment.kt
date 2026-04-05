package com.pdfox.app.ui.tools

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.pdfox.app.R
import com.pdfox.app.databinding.FragmentToolOptionsBinding
import com.pdfox.app.util.FileManager
import com.pdfox.app.util.formatFileSize
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ToolOptionsFragment : Fragment() {

    private var _binding: FragmentToolOptionsBinding? = null
    private val binding get() = _binding!!

    private val args: ToolOptionsFragmentArgs by navArgs()

    @Inject
    lateinit var fileManager: FileManager

    private val selectedUris: MutableList<Uri> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToolOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parseFileUris()
        setupToolbar()
        displayFileSummary()
        setupProcessFab()
    }

    private fun parseFileUris() {
        args.fileUris.split("|").filter { it.isNotBlank() }.forEach { uriString ->
            try {
                Uri.parse(uriString).let { selectedUris.add(it) }
            } catch (e: Exception) {
                Timber.w(e, "Failed to parse URI: $uriString")
            }
        }
        Timber.d("Parsed ${selectedUris.size} URIs for tool: ${args.toolType}")
    }

    private fun setupToolbar() {
        val toolTitle = getToolTitle(args.toolType)
        binding.toolbar.title = toolTitle
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun displayFileSummary() {
        if (selectedUris.isEmpty()) {
            Timber.w("No files selected for tool options")
            findNavController().navigateUp()
            return
        }

        val firstUri = selectedUris.first()
        val fileName = fileManager.getFileNameFromUri(firstUri) ?: "document.pdf"
        binding.tvFilename.text = fileName

        if (selectedUris.size == 1) {
            viewLifecycleOwner.lifecycleScope.launch {
                val fileSize = fileManager.getFileSize(firstUri)
                val pageCount = fileManager.getPageCount(firstUri)
                val info = buildString {
                    append(fileSize.formatFileSize())
                    if (pageCount > 0) {
                        append(" - $pageCount pages")
                    }
                }
                binding.tvFileInfo.text = info
            }
        } else {
            binding.tvFileInfo.text = "${selectedUris.size} files selected"
        }
    }

    private fun setupProcessFab() {
        binding.fabProcess.setOnClickListener {
            navigateToProcessing()
        }
    }

    private fun navigateToProcessing() {
        val uriString = selectedUris.joinToString("|") { it.toString() }
        val optionsJson = collectOptionsAsJson()

        val action = ToolOptionsFragmentDirections.actionToolOptionsFragmentToProcessingFragment(
            toolType = args.toolType,
            fileUris = uriString,
            optionsJson = optionsJson.takeIf { it.isNotEmpty() }
        )
        findNavController().navigate(action)
    }

    /**
     * Override this method to collect tool-specific options as a JSON string.
     * Return an empty string if no options are needed.
     */
    protected open fun collectOptionsAsJson(): String = ""

    private fun getToolTitle(toolId: String): String {
        return when (toolId) {
            "merge" -> getString(R.string.tool_merge)
            "split" -> getString(R.string.tool_split)
            "remove_pages" -> getString(R.string.tool_remove_pages)
            "extract_pages" -> getString(R.string.tool_extract_pages)
            "organize_pages" -> getString(R.string.tool_organize_pages)
            "compress" -> getString(R.string.tool_compress)
            "pdf_to_word" -> getString(R.string.tool_pdf_to_word)
            "pdf_to_ppt" -> getString(R.string.tool_pdf_to_ppt)
            "pdf_to_excel" -> getString(R.string.tool_pdf_to_excel)
            "pdf_to_image" -> getString(R.string.tool_pdf_to_image)
            "pdf_to_pdfa" -> getString(R.string.tool_pdf_to_pdfa)
            "word_to_pdf" -> getString(R.string.tool_word_to_pdf)
            "ppt_to_pdf" -> getString(R.string.tool_ppt_to_pdf)
            "excel_to_pdf" -> getString(R.string.tool_excel_to_pdf)
            "image_to_pdf" -> getString(R.string.tool_image_to_pdf)
            "html_to_pdf" -> getString(R.string.tool_html_to_pdf)
            "protect" -> getString(R.string.tool_protect)
            "unlock" -> getString(R.string.tool_unlock)
            "sign" -> getString(R.string.tool_sign)
            "redact" -> getString(R.string.tool_redact)
            "rotate" -> getString(R.string.tool_rotate)
            "page_numbers" -> getString(R.string.tool_page_numbers)
            "watermark" -> getString(R.string.tool_watermark)
            "metadata" -> getString(R.string.tool_metadata)
            "repair" -> getString(R.string.tool_repair)
            else -> "Tool"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
