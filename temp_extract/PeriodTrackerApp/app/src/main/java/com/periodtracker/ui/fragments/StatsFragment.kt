package com.periodtracker.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.periodtracker.R
import com.periodtracker.databinding.FragmentStatsBinding
import com.periodtracker.viewmodel.PeriodViewModel
import com.periodtracker.viewmodel.PeriodViewModelFactory
import java.text.DecimalFormat

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: PeriodViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            requireActivity(),
            PeriodViewModelFactory(requireActivity().application)
        )[PeriodViewModel::class.java]

        setupCharts()
        observeViewModel()
    }

    private fun setupCharts() {
        // 设置周期长度趋势图
        setupLineChart(binding.chartCycleLength, "周期长度（天）")
        // 设置经期长度柱状图
        setupBarChart(binding.chartPeriodLength, "经期长度（天）")
    }

    private fun setupLineChart(chart: LineChart, label: String) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 20f
                axisMaximum = 45f
            }
            
            axisRight.isEnabled = false
            legend.isEnabled = true
        }
    }

    private fun setupBarChart(chart: BarChart, label: String) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                axisMaximum = 10f
            }
            
            axisRight.isEnabled = false
            legend.isEnabled = true
        }
    }

    private fun observeViewModel() {
        viewModel.statistics.observe(viewLifecycleOwner) { stats ->
            if (stats == null) return@observe

            updateSummaryCard(stats)
            updateCycleLengthChart(stats)
            updatePeriodLengthChart(stats)
        }
    }

    private fun updateSummaryCard(stats: com.periodtracker.data.CycleStatistics) {
        val df = DecimalFormat("#.0")

        // 平均周期
        binding.tvAvgCycleLength.text = stats.averageCycleLength?.let { "${df.format(it)}天" } ?: "--"
        
        // 平均经期
        binding.tvAvgPeriodLength.text = stats.averagePeriodLength?.let { "${df.format(it)}天" } ?: "--"
        
        // 周期规律性
        binding.tvCycleRegularity.text = stats.cycleVariation?.let { variation ->
            val regularity = when {
                variation < 2 -> "非常规律"
                variation < 4 -> "规律"
                variation < 7 -> "基本规律"
                else -> "不规律"
            }
            "$regularity (±${df.format(variation)}天)"
        } ?: "数据不足"

        // 记录数量
        binding.tvTotalRecords.text = "${stats.cycleLengths.size + 1}次"
    }

    private fun updateCycleLengthChart(stats: com.periodtracker.data.CycleStatistics) {
        if (stats.cycleLengths.isEmpty()) {
            binding.chartCycleLength.visibility = View.GONE
            binding.tvNoCycleData.visibility = View.VISIBLE
            return
        }

        binding.chartCycleLength.visibility = View.VISIBLE
        binding.tvNoCycleData.visibility = View.GONE

        val entries = stats.cycleLengths.mapIndexed { index, length ->
            Entry(index.toFloat(), length.toFloat())
        }

        val dataSet = LineDataSet(entries, "周期长度").apply {
            color = ContextCompat.getColor(requireContext(), R.color.primary_blue)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.primary_blue))
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
            mode = LineDataSet.Mode.LINEAR
        }

        binding.chartCycleLength.apply {
            data = LineData(dataSet)
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "第${value.toInt() + 1}次"
                }
            }
            invalidate()
        }
    }

    private fun updatePeriodLengthChart(stats: com.periodtracker.data.CycleStatistics) {
        if (stats.periodLengths.isEmpty()) {
            binding.chartPeriodLength.visibility = View.GONE
            binding.tvNoPeriodData.visibility = View.VISIBLE
            return
        }

        binding.chartPeriodLength.visibility = View.VISIBLE
        binding.tvNoPeriodData.visibility = View.GONE

        val entries = stats.periodLengths.mapIndexed { index, length ->
            BarEntry(index.toFloat(), length.toFloat())
        }

        val dataSet = BarDataSet(entries, "经期长度").apply {
            color = ContextCompat.getColor(requireContext(), R.color.period_red)
            valueTextSize = 10f
        }

        binding.chartPeriodLength.apply {
            data = BarData(dataSet)
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "第${value.toInt() + 1}次"
                }
            }
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
