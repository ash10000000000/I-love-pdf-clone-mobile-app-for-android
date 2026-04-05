package com.pdfox.app.ui.processing

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.pdfox.app.R
import com.pdfox.app.data.db.RecentFile
import com.pdfox.app.data.repository.FileRepository
import com.pdfox.app.databinding.FragmentProcessingBinding
import com.pdfox.app.util.FileManager
import com.pdfox.app.util.Result
import com.pdfox.app.util.formatFileSize
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class ProcessingFragment : Fragment() {

    private var _binding: FragmentProcessingBinding? = null
    private val binding get() = _binding!!

    private val args: ProcessingFragmentArgs by navArgs()

    @Inject
    lateinit var fileManager: FileManager

    @Inject
    lateinit var fileRepository: FileRepository

    private var processingJob: Job? = null
    private var isCancelled = false

    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            showCancelConfirmation()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProcessingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAnimation()
        setupCancelButton()
        setupBackPressedCallback()
        startProcessing()
    }

    private fun setupAnimation() {
        try {
            binding.lottieAnimation.setAnimation(R.raw.processing)
            binding.lottieAnimation.playAnimation()
        } catch (e: Exception) {
            Timber.w(e, "Lottie animation resource not found, hiding animation")
            binding.lottieAnimation.visibility = View.GONE
        }
    }

    private fun setupCancelButton() {
        binding.btnCancel.setOnClickListener {
            showCancelConfirmation()
        }
    }

    private fun setupBackPressedCallback() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )
        onBackPressedCallback.isEnabled = true
    }

    private fun showCancelConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Cancel Processing?")
            .setMessage("Are you sure you want to cancel? Your progress will be lost.")
            .setPositiveButton("Cancel") { _, _ ->
                cancelProcessing()
                findNavController().navigateUp()
            }
            .setNegativeButton("Continue", null)
            .show()
    }

    private fun cancelProcessing() {
        isCancelled = true
        processingJob?.cancel(CancellationException("Processing cancelled by user"))
        Timber.d("Processing cancelled")
    }

    private fun startProcessing() {
        isCancelled = false
        binding.btnCancel.isEnabled = true

        processingJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val fileUris = args.fileUris
                    .split("|")
                    .filter { it.isNotBlank() }
                    .map { Uri.parse(it) }

                Timber.d("Starting processing for tool: ${args.toolType}, ${fileUris.size} files")

                val inputFiles = fileUris.mapIndexed { index, uri ->
                    if (isCancelled) return@launch
                    val fileName = fileManager.getFileNameFromUri(uri) ?: "input_$index.pdf"
                    withContext(Dispatchers.IO) {
                        fileManager.uriToFile(uri, fileName)
                    }
                }

                if (isCancelled) return@launch

                val outputFile = processWithTool(args.toolType, inputFiles)

                if (isCancelled) {
                    outputFile.delete()
                    return@launch
                }

                val inputFileName = fileManager.getFileNameFromUri(fileUris.first()) ?: "document"
                saveToRecent(outputFile, args.toolType, inputFileName)
                navigateToResult(outputFile, inputFileName)

            } catch (e: CancellationException) {
                Timber.d("Processing was cancelled: ${e.message}")
            } catch (e: Exception) {
                Timber.e(e, "Processing failed")
                showError(e.message ?: "Processing failed")
            }
        }
    }

    private suspend fun processWithTool(toolType: String, inputFiles: List<File>): File =
        withContext(Dispatchers.IO) {
            val outputFile = fileManager.createOutputFile(
                prefix = "PDFox_${toolType}",
                extension = getOutputExtension(toolType)
            )

            val toolProcessor = ToolProcessorFactory.create(toolType, fileManager)
            toolProcessor.process(inputFiles, outputFile)
        }

    private fun getOutputExtension(toolType: String): String {
        return when (toolType) {
            "pdf_to_word" -> "docx"
            "pdf_to_ppt" -> "pptx"
            "pdf_to_excel" -> "xlsx"
            "pdf_to_image" -> "zip"
            else -> "pdf"
        }
    }

    private suspend fun saveToRecent(outputFile: File, toolType: String, inputFileName: String) {
        try {
            val thumbnailPath = fileManager.createThumbnail(outputFile)
            val pageCount = if (outputFile.extension == "pdf") {
                fileManager.getPageCount(outputFile).takeIf { it > 0 } ?: 1
            } else 1
            val fileSize = fileManager.getFileSize(outputFile)

            val recentFile = RecentFile(
                inputFileName = inputFileName,
                inputFilePath = null,
                outputFileName = outputFile.name,
                outputFilePath = outputFile.absolutePath,
                toolUsed = toolType,
                timestamp = System.currentTimeMillis(),
                fileSizeBytes = fileSize,
                pageCount = pageCount,
                thumbnailPath = thumbnailPath,
                outputFormat = getOutputExtension(toolType).uppercase()
            )
            fileRepository.insertRecentFile(recentFile)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save to recent files")
        }
    }

    private fun navigateToResult(outputFile: File, inputFileName: String) {
        val toolName = getToolTitle(args.toolType)
        val action = ProcessingFragmentDirections.actionProcessingFragmentToResultFragment(
            outputFilePath = outputFile.absolutePath,
            toolName = toolName,
            inputFileName = inputFileName
        )
        findNavController().navigate(action)
    }

    private fun showError(message: String) {
        lifecycleScope.launch {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.error_occurred))
                .setMessage(message)
                .setPositiveButton(getString(R.string.retry)) { _, _ ->
                    startProcessing()
                }
                .setNegativeButton(getString(android.R.string.cancel)) { _, _ ->
                    findNavController().navigateUp()
                }
                .setCancelable(false)
                .show()
        }
    }

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

    override fun onResume() {
        super.onResume()
        try {
            binding.lottieAnimation.playAnimation()
        } catch (e: Exception) {
            // Animation not loaded
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            binding.lottieAnimation.pauseAnimation()
        } catch (e: Exception) {
            // Animation not loaded
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        processingJob?.cancel()
        onBackPressedCallback.isEnabled = false
        _binding = null
    }
}
