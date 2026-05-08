package com.periodtracker.ui.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.periodtracker.R
import com.periodtracker.databinding.FragmentCalendarBinding
import com.periodtracker.data.PeriodRecord
import com.periodtracker.viewmodel.PeriodViewModel
import com.periodtracker.viewmodel.PeriodViewModelFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: PeriodViewModel
    private lateinit var calendarAdapter: CalendarAdapter

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
    private val shortFormatter = DateTimeFormatter.ofPattern("MM月dd日")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            requireActivity(),
            PeriodViewModelFactory(requireActivity().application)
        )[PeriodViewModel::class.java]

        setupCalendar()
        setupButtons()
        observeViewModel()
        viewModel.refreshPredictions()
    }

    private fun setupCalendar() {
        calendarAdapter = CalendarAdapter { date ->
            viewModel.selectDate(date)
        }
        binding.calendarView.adapter = calendarAdapter
    }

    private fun setupButtons() {
        binding.btnPrevMonth.setOnClickListener {
            calendarAdapter.previousMonth()
        }

        binding.btnNextMonth.setOnClickListener {
            calendarAdapter.nextMonth()
        }

        binding.tvCurrentMonth.setOnClickListener {
            showDatePickerDialog()
        }

        binding.tvCurrentMonth.setOnLongClickListener {
            calendarAdapter.goToToday()
            viewModel.selectDate(LocalDate.now())
            true
        }

        binding.btnStartPeriod.setOnClickListener {
            showStartPeriodDialog()
        }

        binding.btnEndPeriod.setOnClickListener {
            viewModel.endCurrentPeriod()
        }
    }

    private fun observeViewModel() {
        viewModel.allRecords.observe(viewLifecycleOwner) { records ->
            updateCalendar(records)
            updateUI(records)
        }

        viewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            binding.tvSelectedDate.text = date.format(dateFormatter)
            calendarAdapter.setSelectedDate(date)
        }

        viewModel.recordForDate.observe(viewLifecycleOwner) { record ->
            updateDateDetails(record)
        }

        viewModel.nextPeriodDate.observe(viewLifecycleOwner) { date ->
            binding.tvNextPeriod.text = date?.format(shortFormatter) ?: "--"
        }

        viewModel.ovulationDate.observe(viewLifecycleOwner) { date ->
            binding.tvOvulation.text = date?.format(shortFormatter) ?: "--"
        }

        viewModel.fertileWindow.observe(viewLifecycleOwner) { window ->
            if (window != null) {
                binding.tvFertileWindow.text = 
                    "${window.first.format(shortFormatter)} - ${window.second.format(shortFormatter)}"
            } else {
                binding.tvFertileWindow.text = "--"
            }
        }
    }

    private fun updateCalendar(records: List<PeriodRecord>) {
        calendarAdapter.setRecords(records)
        calendarAdapter.setPredictions(
            viewModel.nextPeriodDate.value,
            viewModel.ovulationDate.value,
            viewModel.fertileWindow.value
        )
    }

    private fun updateUI(records: List<PeriodRecord>) {
        binding.tvCurrentMonth.text = calendarAdapter.getCurrentMonthDisplay()

        val hasActivePeriod = records.firstOrNull()?.endDate == null
        binding.btnStartPeriod.visibility = if (hasActivePeriod) View.GONE else View.VISIBLE
        binding.btnEndPeriod.visibility = if (hasActivePeriod) View.VISIBLE else View.GONE
    }

    private fun updateDateDetails(record: PeriodRecord?) {
        if (record != null) {
            updateRecordDetails(record)
        } else {
            updateNonRecordDetails()
        }
    }

    private fun updateRecordDetails(record: PeriodRecord) {
        val duration = record.getDurationDays()
        val symptoms = if (record.symptoms.isNotEmpty()) record.symptoms else "无"
        val notes = if (record.notes.isNotEmpty()) record.notes else "无"

        binding.tvDateStatus.text = "经期第${duration}天"
        binding.tvDateStatus.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.period_red)
        )

        val flowText = when (record.flowLevel) {
            1 -> "少量"
            2 -> "中等"
            3 -> "大量"
            else -> "未知"
        }

        binding.tvDateDetails.text = buildString {
            append("流量: $flowText\n")
            append("症状: $symptoms\n")
            append("备注: $notes")
        }
    }

    private fun updateNonRecordDetails() {
        val selectedDate = viewModel.selectedDate.value
        val nextPeriod = viewModel.nextPeriodDate.value
        val ovulation = viewModel.ovulationDate.value
        val fertile = viewModel.fertileWindow.value

        when {
            selectedDate == nextPeriod -> {
                binding.tvDateStatus.text = "预计月经开始"
                binding.tvDateStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.period_red)
                )
            }
            selectedDate == ovulation -> {
                binding.tvDateStatus.text = "排卵日"
                binding.tvDateStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.ovulation_purple)
                )
            }
            selectedDate != null && fertile != null && 
                selectedDate.isAfter(fertile.first.minusDays(1)) && 
                selectedDate.isBefore(fertile.second.plusDays(1)) -> {
                binding.tvDateStatus.text = "排卵期"
                binding.tvDateStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.fertile_green)
                )
            }
            else -> {
                binding.tvDateStatus.text = "非经期"
                binding.tvDateStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.gray)
                )
            }
        }

        binding.tvDateDetails.text = if (selectedDate != null && selectedDate.isBefore(LocalDate.now())) {
            "点击\"记录经期\"添加该日期的经期记录"
        } else {
            "该日期预计处于安全期"
        }
    }

    private fun showStartPeriodDialog() {
        val selectedDate = viewModel.selectedDate.value ?: LocalDate.now()
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("记录经期")
            .setMessage("确定在 ${selectedDate.format(dateFormatter)} 开始记录经期吗？")
            .setPositiveButton("确定") { _, _ ->
                viewModel.startPeriodToday()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDatePickerDialog() {
        val currentDate = calendarAdapter.getCurrentMonth()
        DatePickerDialog(
            requireContext(),
            { _, year, month, _ ->
                calendarAdapter.setMonth(year, month + 1)
            },
            currentDate.year,
            currentDate.monthValue - 1,
            1
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
