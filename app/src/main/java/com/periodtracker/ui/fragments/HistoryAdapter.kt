package com.periodtracker.ui.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.periodtracker.R
import com.periodtracker.data.PeriodRecord
import java.time.format.DateTimeFormatter

class HistoryAdapter(
    private val onDeleteClick: (PeriodRecord) -> Unit,
    private val onEditClick: (PeriodRecord) -> Unit
) : ListAdapter<PeriodRecord, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDateRange: TextView = itemView.findViewById(R.id.tvDateRange)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvSymptoms: TextView = itemView.findViewById(R.id.tvSymptoms)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_record, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val record = getItem(position)
        
        // 日期范围
        val endDateStr = record.endDate?.format(dateFormatter) ?: "进行中"
        holder.tvDateRange.text = "${record.startDate.format(dateFormatter)} - $endDateStr"
        
        // 持续天数
        val days = record.getDurationDays()
        holder.tvDuration.text = "持续 ${days} 天"
        
        // 状态
        if (record.endDate == null) {
            holder.tvStatus.text = "进行中"
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_active)
            holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.white))
        } else {
            holder.tvStatus.text = "已结束"
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_completed)
            holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.gray_dark))
        }
        
        // 症状
        holder.tvSymptoms.text = if (record.symptoms.isNotEmpty()) {
            "症状: ${record.symptoms}"
        } else {
            "无症状记录"
        }
        
        // 按钮点击事件
        holder.btnEdit.setOnClickListener { onEditClick(record) }
        holder.btnDelete.setOnClickListener { onDeleteClick(record) }
    }
}

class HistoryDiffCallback : DiffUtil.ItemCallback<PeriodRecord>() {
    override fun areItemsTheSame(oldItem: PeriodRecord, newItem: PeriodRecord): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: PeriodRecord, newItem: PeriodRecord): Boolean {
        return oldItem == newItem
    }
}
