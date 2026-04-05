package com.pdfox.app.ui.tools.extractpages

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pdfox.app.databinding.ItemPageThumbnailBinding

class PageThumbnailAdapter(
    private val onSelectionChanged: (Set<Int>) -> Unit
) : ListAdapter<PageThumbnail, PageThumbnailAdapter.PageViewHolder>(PageDiffCallback()) {

    private val selectedPages = mutableSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemPageThumbnailBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun selectAll() {
        selectedPages.clear()
        val updated = currentList.map {
            it.isSelected = true
            selectedPages.add(it.pageNumber)
            it
        }
        submitList(updated)
        onSelectionChanged(selectedPages.toSet())
    }

    fun deselectAll() {
        selectedPages.clear()
        val updated = currentList.map {
            it.isSelected = false
            it
        }
        submitList(updated)
        onSelectionChanged(selectedPages.toSet())
    }

    inner class PageViewHolder(
        private val binding: ItemPageThumbnailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(page: PageThumbnail) {
            binding.tvPageNumber?.text = "Page ${page.pageNumber}"
            binding.cbSelect.isChecked = page.isSelected
            binding.cbSelect.setOnCheckedChangeListener { _, isChecked ->
                val currentPage = currentList[adapterPosition]
                currentPage.isSelected = isChecked
                if (isChecked) {
                    selectedPages.add(currentPage.pageNumber)
                } else {
                    selectedPages.remove(currentPage.pageNumber)
                }
                onSelectionChanged(selectedPages.toSet())
            }
            binding.cardThumbnail.setOnClickListener {
                binding.cbSelect.isChecked = !binding.cbSelect.isChecked
            }
        }
    }

    class PageDiffCallback : DiffUtil.ItemCallback<PageThumbnail>() {
        override fun areItemsTheSame(oldItem: PageThumbnail, newItem: PageThumbnail) =
            oldItem.pageNumber == newItem.pageNumber

        override fun areContentsTheSame(oldItem: PageThumbnail, newItem: PageThumbnail) =
            oldItem == newItem
    }
}
