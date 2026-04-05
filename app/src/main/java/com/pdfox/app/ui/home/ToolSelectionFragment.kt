package com.pdfox.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.pdfox.app.databinding.FragmentToolSelectionBinding
import androidx.navigation.fragment.navArgs

class ToolSelectionFragment : Fragment() {
    private var _binding: FragmentToolSelectionBinding? = null
    private val binding get() = _binding!!
    private val args: ToolSelectionFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToolSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val category = args.category
        // For now, just show the category name
        activity?.title = category ?: "All Tools"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
