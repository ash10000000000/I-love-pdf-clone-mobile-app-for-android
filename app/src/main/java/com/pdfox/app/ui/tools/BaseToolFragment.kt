package com.pdfox.app.ui.tools

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pdfox.app.R
import com.pdfox.app.databinding.FragmentToolFileSelectionBinding
import com.pdfox.app.util.FileManager
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
abstract class BaseToolFragment : Fragment() {

    private var _binding: FragmentToolFileSelectionBinding? = null
    protected val binding get() = _binding!!

    @Inject
    lateinit var fileManager: FileManager

    private val selectedUris: MutableList<Uri> = mutableListOf()

    private val singleFilePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedUris.clear()
                selectedUris.add(uri)
                onFileSelected(uri)
                navigateToOptions()
            }
        }
    }

    private val multiFilePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val clipData = result.data?.clipData
            val uriList = mutableListOf<Uri>()

            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    clipData.getItemAt(i).uri?.let { uriList.add(it) }
                }
            } else {
                result.data?.data?.let { uriList.add(it) }
            }

            if (uriList.isNotEmpty()) {
                selectedUris.clear()
                selectedUris.addAll(uriList)
                onFilesSelected(uriList)
                navigateToOptions()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToolFileSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupUploadZone()
        setupSupportedFormatsChips()
    }

    private fun setupToolbar() {
        binding.toolbar.title = requireActivity().title
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupUploadZone() {
        binding.uploadZone.setOnClickListener {
            launchFilePicker()
        }

        binding.btnSelectFile.setOnClickListener {
            launchFilePicker()
        }

        binding.rvSelectedFiles?.hide()
    }

    private fun setupSupportedFormatsChips() {
        val formats = getSupportedFormats()
        binding.chipFormats.removeAllViews()

        formats.forEach { format ->
            val chip = com.google.android.material.chip.Chip(requireContext())
            chip.text = format
            chip.isCheckable = false
            chip.isClickable = false
            binding.chipFormats.addView(chip)
        }
    }

    private fun launchFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = getAcceptedMimeType()
            addCategory(Intent.CATEGORY_OPENABLE)
            if (isMultiFile()) {
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
        }
        Timber.d("Launching file picker with MIME type: ${getAcceptedMimeType()}, multiFile: ${isMultiFile()}")

        if (isMultiFile()) {
            multiFilePicker.launch(intent)
        } else {
            singleFilePicker.launch(intent)
        }
    }

    protected open fun onFileSelected(uri: Uri) {
        Timber.d("File selected: ${fileManager.getFileNameFromUri(uri)}")
    }

    protected open fun onFilesSelected(uris: List<Uri>) {
        Timber.d("${uris.size} files selected")
    }

    protected fun navigateToOptions() {
        val uriString = selectedUris.joinToString("|") { it.toString() }
        val direction = com.pdfox.app.R.id.toolOptionsFragment

        try {
            val bundle = Bundle().apply {
                putString("toolType", getToolId())
                putString("fileUris", uriString)
            }
            findNavController().navigate(direction, bundle)
        } catch (e: Exception) {
            Timber.e(e, "Failed to navigate to tool options")
        }
    }

    protected fun getSelectedUris(): List<Uri> = selectedUris.toList()

    // Abstract methods to implement by subclasses
    abstract fun getToolId(): String
    abstract fun getAcceptedMimeType(): String
    abstract fun isMultiFile(): Boolean
    abstract fun getSupportedFormats(): List<String>

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
