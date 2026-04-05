package com.pdfox.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pdfox.app.databinding.ItemToolBinding
import com.pdfox.app.util.getCategoryColor
import com.pdfox.app.util.getCategoryColorWithAlpha

class ToolAdapter(
    private val onToolClick: (ToolItem) -> Unit
) : ListAdapter<ToolItem, ToolAdapter.ToolViewHolder>(ToolDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolViewHolder {
        val binding = ItemToolBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ToolViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ToolViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ToolViewHolder(
        private val binding: ItemToolBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tool: ToolItem) {
            binding.tvToolName.text = tool.name
            binding.tvToolDescription.text = tool.description
            binding.ivToolIcon.setImageResource(tool.iconRes)

            val color = getCategoryColor(binding.root.context, tool.category)
            val colorWithAlpha = getCategoryColorWithAlpha(binding.root.context, tool.category)

            binding.viewAccent.setBackgroundColor(color)
            binding.flIconBg.setBackgroundColor(colorWithAlpha)
            binding.ivToolIcon.setColorFilter(color)

            binding.cardTool.setOnClickListener {
                onToolClick(tool)
            }
        }
    }

    class ToolDiffCallback : DiffUtil.ItemCallback<ToolItem>() {
        override fun areItemsTheSame(oldItem: ToolItem, newItem: ToolItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ToolItem, newItem: ToolItem): Boolean {
            return oldItem == newItem
        }
    }
}
