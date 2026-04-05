package com.pdfox.app.ui.tools.organizepages

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pdfox.app.databinding.ItemOrganizePageBinding

class OrganizePagesAdapter(
    private val onPageRotated: (Int, Int) -> Unit
) : ListAdapter<OrganizePageItem, OrganizePagesAdapter.OrganizePageViewHolder>(OrganizePageDiffCallback()),
    OrganizeItemTouchHelperCallback.DragListener {

    private val pages = mutableListOf<OrganizePageItem>()

    override fun submitList(list: List<OrganizePageItem>?) {
        list?.let { pages.clear(); pages.addAll(it) }
        super.submitList(list)
    }

    fun getPageOrder(): List<Int> = currentList.map { it.originalPageNumber }

    fun getPageRotations(): Map<Int, Int> = currentList.associate { it.originalPageNumber to it.rotation }

    fun moveItem(fromPos: Int, toPos: Int) {
        if (fromPos < 0 || toPos < 0 || fromPos >= currentList.size || toPos >= currentList.size) return
        val item = currentList.toMutableList().removeAt(fromPos)
        val mutableList = currentList.toMutableList()
        // This is handled by ItemTouchHelper
    }

    override fun onItemMoved(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                val temp = currentList[i]
                val next = currentList[i + 1]
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                val temp = currentList[i]
                val prev = currentList[i - 1]
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrganizePageViewHolder {
        val binding = ItemOrganizePageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OrganizePageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrganizePageViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class OrganizePageViewHolder(
        private val binding: ItemOrganizePageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(page: OrganizePageItem, position: Int) {
            binding.tvPageNumber.text = "${position + 1}"

            page.thumbnailPath?.let { path ->
                val bitmap = BitmapFactory.decodeFile(path)
                binding.ivThumbnail.setImageBitmap(bitmap)
            }

            binding.tvRotation.text = "${page.rotation}°"

            binding.btnRotate90.setOnClickListener {
                val newRotation = (page.rotation + 90) % 360
                page.rotation = newRotation
                binding.tvRotation.text = "${newRotation}°"
                onPageRotated(page.originalPageNumber, newRotation)
            }

            binding.btnRotate270.setOnClickListener {
                val newRotation = (page.rotation - 90 + 360) % 360
                page.rotation = newRotation
                binding.tvRotation.text = "${newRotation}°"
                onPageRotated(page.originalPageNumber, newRotation)
            }
        }
    }

    class OrganizePageDiffCallback : DiffUtil.ItemCallback<OrganizePageItem>() {
        override fun areItemsTheSame(oldItem: OrganizePageItem, newItem: OrganizePageItem) =
            oldItem.originalPageNumber == newItem.originalPageNumber

        override fun areContentsTheSame(oldItem: OrganizePageItem, newItem: OrganizePageItem) =
            oldItem == newItem
    }
}
