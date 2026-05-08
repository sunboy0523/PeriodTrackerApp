package com.periodtracker.ui.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.periodtracker.R
import com.periodtracker.data.PeriodRecord
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class CalendarAdapter(
    private val onDateClick: (LocalDate) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.DateViewHolder>() {

    private var currentMonth: YearMonth = YearMonth.now()
    private var selectedDate: LocalDate = LocalDate.now()
    private var records: List<PeriodRecord> = emptyList()
    private var nextPeriod: LocalDate? = null
    private var ovulation: LocalDate? = null
    private var fertileWindow: Pair<LocalDate, LocalDate>? = null

    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy年MM月")

    companion object {
        const val DAYS_IN_WEEK = 7
        const val MAX_WEEKS = 6
        const val DEFAULT_PERIOD_DURATION = 5
    }

    inner class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDay: TextView = itemView.findViewById(R.id.tvDay)
        val indicator: View = itemView.findViewById(R.id.indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return DateViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        val startOfMonth = currentMonth.atDay(1)
        val dayOfWeek = startOfMonth.dayOfWeek.value % 7
        val firstVisibleDate = startOfMonth.minusDays(dayOfWeek.toLong())
        val date = firstVisibleDate.plusDays(position.toLong())

        val isCurrentMonth = date.month == currentMonth.month
        val isToday = date == LocalDate.now()
        val isSelected = date == selectedDate

        holder.tvDay.text = date.dayOfMonth.toString()

        // 设置背景颜色
        when {
            isInPeriod(date) -> {
                holder.itemView.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.period_red_light)
                )
            }
            isOvulationDate(date) -> {
                holder.itemView.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.ovulation_purple_light)
                )
            }
            isFertileWindow(date) -> {
                holder.itemView.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.fertile_green_light)
                )
            }
            isPredictedPeriod(date) -> {
                holder.itemView.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.predicted_pink)
                )
            }
            isCurrentMonth -> {
                holder.itemView.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.white)
                )
            }
            else -> {
                holder.itemView.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.gray_light)
                )
            }
        }

        // 设置文字颜色
        holder.tvDay.setTextColor(
            when {
                isToday -> ContextCompat.getColor(holder.itemView.context, R.color.today_blue)
                isInPeriod(date) -> ContextCompat.getColor(holder.itemView.context, R.color.period_red)
                isCurrentMonth -> ContextCompat.getColor(holder.itemView.context, R.color.black)
                else -> ContextCompat.getColor(holder.itemView.context, R.color.gray)
            }
        )

        // 选中标记
        holder.indicator.visibility = if (isSelected) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            val oldPosition = findPositionForDate(selectedDate)
            selectedDate = date
            onDateClick(date)
            if (oldPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(oldPosition)
            }
            notifyItemChanged(holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount(): Int = DAYS_IN_WEEK * MAX_WEEKS

    private fun getDateForPosition(position: Int): LocalDate {
        val startOfMonth = currentMonth.atDay(1)
        val dayOfWeek = startOfMonth.dayOfWeek.value % 7
        val firstVisibleDate = startOfMonth.minusDays(dayOfWeek.toLong())
        return firstVisibleDate.plusDays(position.toLong())
    }

    private fun findPositionForDate(date: LocalDate): Int {
        val startOfMonth = currentMonth.atDay(1)
        val dayOfWeek = startOfMonth.dayOfWeek.value % 7
        val firstVisibleDate = startOfMonth.minusDays(dayOfWeek.toLong())
        return java.time.temporal.ChronoUnit.DAYS.between(firstVisibleDate, date).toInt()
    }

    private fun isInPeriod(date: LocalDate): Boolean {
        return records.any { it.containsDate(date) }
    }

    private fun isOvulationDate(date: LocalDate): Boolean {
        return date == ovulation
    }

    private fun isFertileWindow(date: LocalDate): Boolean {
        val window = fertileWindow ?: return false
        return !date.isBefore(window.first) && !date.isAfter(window.second)
    }

    private fun isPredictedPeriod(date: LocalDate): Boolean {
        val next = nextPeriod ?: return false
        val endDate = next.plusDays(DEFAULT_PERIOD_DURATION.toLong() - 1)
        return !date.isBefore(next) && !date.isAfter(endDate)
    }

    fun setMonth(year: Int, month: Int) {
        currentMonth = YearMonth.of(year, month)
        notifyDataSetChanged()
    }

    fun getCurrentMonth(): YearMonth = currentMonth

    fun getCurrentMonthDisplay(): String = currentMonth.format(monthFormatter)

    fun previousMonth() {
        currentMonth = currentMonth.minusMonths(1)
        notifyDataSetChanged()
    }

    fun nextMonth() {
        currentMonth = currentMonth.plusMonths(1)
        notifyDataSetChanged()
    }

    fun goToToday() {
        currentMonth = YearMonth.now()
        selectedDate = LocalDate.now()
        notifyDataSetChanged()
    }

    fun setSelectedDate(date: LocalDate) {
        selectedDate = date
        notifyDataSetChanged()
    }

    fun setRecords(newRecords: List<PeriodRecord>) {
        records = newRecords
        notifyDataSetChanged()
    }

    fun setPredictions(next: LocalDate?, ovulationDate: LocalDate?, fertile: Pair<LocalDate, LocalDate>?) {
        nextPeriod = next
        ovulation = ovulationDate
        fertileWindow = fertile
        notifyDataSetChanged()
    }
}
