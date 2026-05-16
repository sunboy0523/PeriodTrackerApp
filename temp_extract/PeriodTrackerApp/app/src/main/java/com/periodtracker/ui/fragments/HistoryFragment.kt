package com.periodtracker.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.periodtracker.R
import com.periodtracker.databinding.FragmentHistoryBinding
import com.periodtracker.data.PeriodRecord
import com.periodtracker.viewmodel.PeriodViewModel
import com.periodtracker.viewmodel.PeriodViewModelFactory
import java.time.format.DateTimeFormatter

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: PeriodViewModel
    private lateinit var historyAdapter: HistoryAdapter

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
    private val shortFormatter = DateTimeFormatter.ofPattern("MM月dd日")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            requireActivity(),
            PeriodViewModelFactory(requireActivity().application)
        )[PeriodViewModel::class.java]

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(
            onDeleteClick = { record ->
                showDeleteConfirmDialog(record)
            },
            onEditClick = { record ->
                // TODO: 编辑功能
            }
        )

        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.allRecords.observe(viewLifecycleOwner) { records ->
            if (records.isEmpty()) {
                binding.recyclerViewHistory.visibility = View.GONE
                binding.tvEmptyState.visibility = View.VISIBLE
            } else {
                binding.recyclerViewHistory.visibility = View.VISIBLE
                binding.tvEmptyState.visibility = View.GONE
                historyAdapter.submitList(records.sortedByDescending { it.startDate })
            }

            // 更新统计信息
            updateStatistics(records)
        }
    }

    private fun updateStatistics(records: List<PeriodRecord>) {
        if (records.isEmpty()) return

        val totalCycles = records.size
        val ongoingPeriod = records.firstOrNull { it.endDate == null }
        
        // 计算最近6个月的经期天数
        val last6Months = records.take(6).sumOf { it.getDurationDays() }
        
        // 平均周期长度（近似计算）
        val avgCycle = if (records.size >= 2) {
            val cycles = records.windowed(2) { pair ->
                val days = java.time.temporal.ChronoUnit.DAYS.between(
                    pair[1].startDate, pair[0].startDate
                )
                days.toInt()
            }.filter { it in 20..45 }
            if (cycles.isNotEmpty()) cycles.average() else 28.0
        } else 28.0

        // 更新UI
        binding.tvTotalCycles.text = totalCycles.toString()
        binding.tvAvgCycleDays.text = String.format("%.1f", avgCycle)
        
        ongoingPeriod?.let {
            binding.tvCurrentStatus.text = "进行中"
            binding.tvCurrentStatus.setTextColor(
                requireContext().getColor(R.color.period_red)
            )
        } ?: run {
            binding.tvCurrentStatus.text = "非经期"
            binding.tvCurrentStatus.setTextColor(
                requireContext().getColor(R.color.gray)
            )
        }

        val avgPeriod = records.filter { it.endDate != null }
            .map { it.getDurationDays() }
            .average()
            .takeIf { !it.isNaN() } ?: 5.0
        binding.tvAvgPeriodDays.text = String.format("%.1f", avgPeriod)
    }

    private fun showDeleteConfirmDialog(record: PeriodRecord) {
        val message = "确定要删除 ${record.startDate.format(dateFormatter)} 的经期记录吗？此操作无法撤销。"
        
        AlertDialog.Builder(requireContext())
            .setTitle("删除记录")
            .setMessage(message)
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteRecord(record)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
