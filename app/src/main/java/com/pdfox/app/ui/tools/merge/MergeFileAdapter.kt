package com.pdfox.app.ui.tools.merge

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pdfox.app.databinding.ItemMergeFileBinding

class MergeFileAdapter(
    private val onItemMoved: (Int, Int) -> Unit
) : RecyclerView.Adapter<MergeFileAdapter.MergeFileViewHolder>() {

    private val files = mutableListOf<Uri>()

    fun submitFiles(uris: List<Uri>) {
        files.clear()
        files.addAll(uris)
        notifyDataSetChanged()
    }

    fun getOrderedFiles(): List<Uri> = files.toList()

    fun moveItem(fromPos: Int, toPos: Int) {
        if (fromPos < 0 || toPos < 0 || fromPos >= files.size || toPos >= files.size) return
        val item = files.removeAt(fromPos)
        files.add(toPos, item)
        notifyItemMoved(fromPos, toPos)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MergeFileViewHolder {
        val binding = ItemMergeFileBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MergeFileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MergeFileViewHolder, position: Int) {
        holder.bind(files[position], position)
    }

    override fun getItemCount() = files.size

    inner class MergeFileViewHolder(
        private val binding: ItemMergeFileBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(uri: Uri, position: Int) {
            val path = uri.path ?: ""
            val fileName = path.substringAfterLast('/', "file_${position + 1}.pdf")
            binding.tvFileName.text = fileName
            binding.tvPosition.text = "${position + 1}"
        }
    }
}
