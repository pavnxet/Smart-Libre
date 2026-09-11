package com.github.libretube.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.ItemTimestampEntryBinding
import com.github.libretube.obj.ChapterCategory
import com.github.libretube.obj.TimestampItem

class TimestampsSidebarAdapter(
    private var allItems: List<TimestampItem> = emptyList(),
    private val onTimestampClicked: (TimestampItem) -> Unit
) : RecyclerView.Adapter<TimestampsSidebarAdapter.ViewHolder>() {

    private var currentFilter: ChapterCategory = ChapterCategory.ALL
    private var filteredItems: List<TimestampItem> = emptyList()

    class ViewHolder(val binding: ItemTimestampEntryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTimestampEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = filteredItems[position]
        holder.binding.tvTimestamp.text = item.timeFormatted
        holder.binding.tvNote.text = item.note

        when (item.category) {
            ChapterCategory.QUES -> {
                holder.binding.tvCategoryBadge.isVisible = true
                holder.binding.tvCategoryBadge.text = "❓ QUES"
                holder.binding.tvCategoryBadge.setTextColor(Color.parseColor("#FFD54F"))
            }
            ChapterCategory.ANS -> {
                holder.binding.tvCategoryBadge.isVisible = true
                holder.binding.tvCategoryBadge.text = "🎯 OPTION"
                holder.binding.tvCategoryBadge.setTextColor(Color.parseColor("#81C784"))
            }
            ChapterCategory.EXPLAIN -> {
                holder.binding.tvCategoryBadge.isVisible = true
                holder.binding.tvCategoryBadge.text = "💡 EXPLAIN"
                holder.binding.tvCategoryBadge.setTextColor(Color.parseColor("#64B5F6"))
            }
            else -> {
                holder.binding.tvCategoryBadge.isVisible = false
            }
        }

        holder.binding.root.setOnClickListener {
            onTimestampClicked(item)
        }
    }

    override fun getItemCount(): Int = filteredItems.size

    fun updateData(newItems: List<TimestampItem>) {
        allItems = newItems
        applyFilter(currentFilter)
    }

    fun applyFilter(filter: ChapterCategory) {
        currentFilter = filter
        filteredItems = if (filter == ChapterCategory.ALL) {
            allItems
        } else {
            allItems.filter { it.category == filter }
        }
        notifyDataSetChanged()
    }
}