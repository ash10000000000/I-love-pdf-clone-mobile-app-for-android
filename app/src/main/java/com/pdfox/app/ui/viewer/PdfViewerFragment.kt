package com.pdfox.app.ui.viewer

import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.pdfox.app.R
import com.pdfox.app.databinding.FragmentPdfViewerBinding
import com.pdfox.app.util.FileManager
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class PdfViewerFragment : Fragment() {

    private var _binding: FragmentPdfViewerBinding? = null
    private val binding get() = _binding!!

    private val args: PdfViewerFragmentArgs by navArgs()

    @Inject
    lateinit var fileManager: FileManager

    private var pdfRenderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private lateinit var adapter: PdfPageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPdfViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        openPdfFile()
        setupViewPager()
        setupPageIndicator()
    }

    private fun setupToolbar() {
        val file = File(args.filePath)
        binding.toolbar.title = file.nameWithoutExtension
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.toolbar.inflateMenu(R.menu.pdf_viewer_menu)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_share -> {
                    shareFile()
                    true
                }
                R.id.action_download -> {
                    downloadFile()
                    true
                }
                else -> false
            }
        }
    }

    private fun openPdfFile() {
        val file = File(args.filePath)
        if (!file.exists()) {
            Timber.e("File not found: ${args.filePath}")
            findNavController().navigateUp()
            return
        }

        try {
            parcelFileDescriptor = ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            pdfRenderer = PdfRenderer(parcelFileDescriptor!!)
            Timber.d("Opened PDF with ${pdfRenderer?.pageCount} pages")
        } catch (e: Exception) {
            Timber.e(e, "Failed to open PDF file")
            findNavController().navigateUp()
        }
    }

    private fun setupViewPager() {
        val renderer = pdfRenderer ?: return
        adapter = PdfPageAdapter(renderer)
        binding.vpPages.adapter = adapter
        binding.vpPages.offscreenPageLimit = 2

        binding.vpPages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updatePageIndicator(position, renderer.pageCount)
            }
        })

        updatePageIndicator(0, renderer.pageCount)
    }

    private fun setupPageIndicator() {
        val renderer = pdfRenderer ?: return
        updatePageIndicator(0, renderer.pageCount)
    }

    private fun updatePageIndicator(currentPage: Int, totalPages: Int) {
        binding.tvPageIndicator.text = getString(R.string.pdf_viewer_page, currentPage + 1, totalPages)
    }

    private fun shareFile() {
        try {
            val file = File(args.filePath)
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )

            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(
                android.content.Intent.createChooser(shareIntent, "Share PDF file")
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to share file")
        }
    }

    private fun downloadFile() {
        val file = File(args.filePath)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val downloadsUri = fileManager.copyToDownloads(file)
                if (downloadsUri != null) {
                    Snackbar.make(
                        binding.root,
                        "File saved to Downloads",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to copy to downloads")
                Snackbar.make(
                    binding.root,
                    "Failed to save file",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun closeRenderer() {
        try {
            pdfRenderer?.close()
            parcelFileDescriptor?.close()
            pdfRenderer = null
            parcelFileDescriptor = null
        } catch (e: Exception) {
            Timber.e(e, "Error closing PDF renderer")
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        closeRenderer()
        _binding = null
    }
}
