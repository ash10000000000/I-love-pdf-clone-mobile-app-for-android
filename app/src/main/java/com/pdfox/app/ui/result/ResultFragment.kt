package com.pdfox.app.ui.result

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.pdfox.app.R
import com.pdfox.app.databinding.FragmentResultBinding
import com.pdfox.app.util.FileManager
import com.pdfox.app.util.formatFileSize
import com.pdfox.app.util.hide
import com.pdfox.app.util.show
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class ResultFragment : Fragment() {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    private val args: ResultFragmentArgs by navArgs()

    @Inject
    lateinit var fileManager: FileManager

    private var outputFile: File? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        outputFile = File(args.outputFilePath)
        setupSuccessAnimation()
        displayOutputInfo()
        setupButtons()
    }

    private fun setupSuccessAnimation() {
        try {
            binding.lottieSuccess.setAnimation(R.raw.success)
            binding.lottieSuccess.playAnimation()
        } catch (e: Exception) {
            Timber.w(e, "Lottie success animation resource not found")
            binding.lottieSuccess.visibility = View.GONE
        }
    }

    private fun displayOutputInfo() {
        binding.tvDone.text = getString(R.string.done)

        outputFile?.let { file ->
            binding.tvOutputFilename.text = file.name
            val fileSize = fileManager.getFileSize(file)
            binding.tvOutputSize.text = fileSize.formatFileSize()
        } ?: run {
            Timber.w("Output file not found at: ${args.outputFilePath}")
            binding.cardOutputInfo.hide()
        }
    }

    private fun setupButtons() {
        binding.btnDownload.setOnClickListener {
            downloadFile()
        }

        binding.btnShare.setOnClickListener {
            shareFile()
        }

        binding.btnPreview.setOnClickListener {
            previewFile()
        }

        binding.btnProcessAnother.setOnClickListener {
            navigateToHome()
        }
    }

    private fun downloadFile() {
        val file = outputFile ?: run {
            Timber.w("No output file to download")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val downloadsUri = fileManager.copyToDownloads(file)
                if (downloadsUri != null) {
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("File Saved")
                        .setMessage("File saved to Downloads folder")
                        .setPositiveButton("OK", null)
                        .show()
                    Timber.d("File saved to Downloads: $downloadsUri")
                } else {
                    showDownloadError()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to copy to downloads")
                showDownloadError()
            }
        }
    }

    private fun showDownloadError() {
        Snackbar.make(
            binding.root,
            "Failed to save file",
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun shareFile() {
        val file = outputFile ?: return

        try {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = fileManager.getMimeType(file)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_SUBJECT, "PDFox - ${args.toolName}")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Processed with PDFox using ${args.toolName}"
                )
            }

            startActivity(
                Intent.createChooser(shareIntent, "Share PDF file")
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to share file")
            Snackbar.make(
                binding.root,
                "Failed to share file",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun previewFile() {
        val file = outputFile ?: return

        val action = ResultFragmentDirections.actionResultFragmentToPdfViewerFragment(
            filePath = file.absolutePath
        )
        findNavController().navigate(action)
    }

    private fun navigateToHome() {
        findNavController().navigate(R.id.homeFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
