package com.pdfox.app.ui.recent

import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.pdfox.app.R
import com.pdfox.app.data.db.RecentFile
import com.pdfox.app.databinding.ItemRecentFileBinding
import com.pdfox.app.util.formatFileSize
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecentFileAdapter(
    private val onItemClick: (RecentFile) -> Unit,
    private val onThumbnailClick: (RecentFile) -> Unit
) : ListAdapter<RecentFile, RecentFileAdapter.RecentFileViewHolder>(RecentFileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentFileViewHolder {
        val binding = ItemRecentFileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecentFileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecentFileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecentFileViewHolder(
        private val binding: ItemRecentFileBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(recentFile: RecentFile) {
            binding.tvFilename.text = recentFile.outputFileName
            binding.tvFileSize.text = recentFile.fileSizeBytes.formatFileSize()
            binding.tvDate.text = formatTimestamp(recentFile.timestamp)
            binding.chipFormat.text = recentFile.outputFormat

            loadThumbnail(recentFile)

            binding.root.setOnClickListener {
                onItemClick(recentFile)
            }

            binding.ivThumbnail.setOnClickListener {
                onThumbnailClick(recentFile)
            }
        }

        private fun loadThumbnail(recentFile: RecentFile) {
            val thumbnailFile = recentFile.thumbnailPath?.let { File(it) }

            if (thumbnailFile != null && thumbnailFile.exists()) {
                binding.ivThumbnail.load(thumbnailFile) {
                    placeholder(android.R.drawable.ic_menu_save)
                    error(android.R.drawable.ic_menu_save)
                    crossfade(true)
                }
            } else {
                binding.ivThumbnail.setImageResource(android.R.drawable.ic_menu_save)
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            return dateFormat.format(Date(timestamp))
        }
    }

    class RecentFileDiffCallback : DiffUtil.ItemCallback<RecentFile>() {
        override fun areItemsTheSame(oldItem: RecentFile, newItem: RecentFile): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RecentFile, newItem: RecentFile): Boolean {
            return oldItem == newItem
        }
    }
}
