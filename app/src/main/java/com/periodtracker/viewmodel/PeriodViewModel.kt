package com.periodtracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.periodtracker.data.CycleStatistics
import com.periodtracker.data.PeriodRecord
import com.periodtracker.data.PeriodRecordDao
import com.periodtracker.data.AppDatabase
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.sqrt

class PeriodViewModel(application: Application) : AndroidViewModel(application) {

    private val dao: PeriodRecordDao = AppDatabase.getDatabase(application).periodRecordDao()

    val allRecords: LiveData<List<PeriodRecord>> = dao.getAllRecords()

    private val _selectedDate = MutableLiveData<LocalDate>(LocalDate.now())
    val selectedDate: LiveData<LocalDate> = _selectedDate

    private val _recordForDate = MutableLiveData<PeriodRecord?>()
    val recordForDate: LiveData<PeriodRecord?> = _recordForDate

    private val _nextPeriodDate = MutableLiveData<LocalDate?>()
    val nextPeriodDate: LiveData<LocalDate?> = _nextPeriodDate

    private val _ovulationDate = MutableLiveData<LocalDate?>()
    val ovulationDate: LiveData<LocalDate?> = _ovulationDate

    private val _fertileWindow = MutableLiveData<Pair<LocalDate, LocalDate>?>()
    val fertileWindow: LiveData<Pair<LocalDate, LocalDate>?> = _fertileWindow

    private val _operationResult = MutableLiveData<OperationResult?>()
    val operationResult: LiveData<OperationResult?> = _operationResult

    private val _statistics = MutableLiveData<CycleStatistics?>()
    val statistics: LiveData<CycleStatistics?> = _statistics

    companion object {
        private const val DEFAULT_CYCLE_LENGTH = 28
        private const val DEFAULT_PERIOD_DURATION = 5
        private const val OVULATION_DAY_OFFSET = 14
    }

    init {
        refreshPredictions()
    }

    fun refreshPredictions() {
        viewModelScope.launch {
            val records = dao.getAllRecordsSync()
            if (records.isNotEmpty()) {
                calculatePredictions(records)
                calculateStatistics(records)
            }
        }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        viewModelScope.launch {
            val record = dao.getRecordForDate(date)
            _recordForDate.postValue(record)
        }
    }

    fun startPeriodToday() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val existingRecord = dao.getActivePeriod()
            if (existingRecord != null) {
                _operationResult.value = OperationResult.Error("当前已有进行中的经期记录")
                return@launch
            }
            val newRecord = PeriodRecord(startDate = today)
            dao.insert(newRecord)
            selectDate(today)
            refreshPredictions()
            _operationResult.value = OperationResult.Success("经期记录已开始")
        }
    }

    fun endCurrentPeriod() {
        viewModelScope.launch {
            val activePeriod = dao.getActivePeriod()
            if (activePeriod == null) {
                _operationResult.value = OperationResult.Error("没有进行中的经期记录")
                return@launch
            }
            val updatedRecord = activePeriod.copy(endDate = LocalDate.now())
            dao.update(updatedRecord)
            refreshPredictions()
            _operationResult.value = OperationResult.Success("经期记录已结束")
        }
    }

    fun deleteRecord(record: PeriodRecord) {
        viewModelScope.launch {
            dao.delete(record)
            refreshPredictions()
            _operationResult.value = OperationResult.Success("记录已删除")
        }
    }

    fun clearOperationResult() {
        _operationResult.value = null
    }

    private fun calculatePredictions(records: List<PeriodRecord>) {
        val completedCycles = records.filter { it.endDate != null }
        if (completedCycles.isEmpty()) {
            _nextPeriodDate.postValue(null)
            _ovulationDate.postValue(null)
            _fertileWindow.postValue(null)
            return
        }

        val avgCycleLength = calculateAverageCycleLength(completedCycles)
        val nextPeriod = calculateNextPeriodDate(records, avgCycleLength)
        _nextPeriodDate.postValue(nextPeriod)

        nextPeriod?.let {
            val ovulation = it.minusDays(OVULATION_DAY_OFFSET.toLong())
            _ovulationDate.postValue(ovulation)

            val fertileStart = ovulation.minusDays(5)
            val fertileEnd = ovulation.plusDays(1)
            _fertileWindow.postValue(Pair(fertileStart, fertileEnd))
        }
    }

    private fun calculateNextPeriodDate(records: List<PeriodRecord>, avgCycleLength: Int): LocalDate? {
        val activePeriod = records.firstOrNull { it.endDate == null }
        if (activePeriod != null) {
            val avgPeriodLength = calculateAveragePeriodLength(records.filter { it.endDate != null })
            return activePeriod.startDate.plusDays((avgCycleLength - avgPeriodLength + DEFAULT_PERIOD_DURATION).toLong())
        }

        val lastCycle = records.firstOrNull { it.endDate != null } ?: return null
        return lastCycle.startDate.plusDays(avgCycleLength.toLong())
    }

    private fun calculateAverageCycleLength(records: List<PeriodRecord>): Int {
        if (records.size < 2) return DEFAULT_CYCLE_LENGTH

        val cycles = records.windowed(2) { pair ->
            ChronoUnit.DAYS.between(pair[1].startDate, pair[0].startDate).toInt()
        }.filter { it in 20..45 }

        return if (cycles.isNotEmpty()) cycles.average().toInt() else DEFAULT_CYCLE_LENGTH
    }

    private fun calculateAveragePeriodLength(records: List<PeriodRecord>): Int {
        if (records.isEmpty()) return DEFAULT_PERIOD_DURATION
        val lengths = records.map { it.getDurationDays() }.filter { it in 2..10 }
        return if (lengths.isNotEmpty()) lengths.average().toInt() else DEFAULT_PERIOD_DURATION
    }

    private fun calculateStatistics(records: List<PeriodRecord>) {
        val completedRecords = records.filter { it.endDate != null }
        if (completedRecords.isEmpty()) {
            _statistics.postValue(CycleStatistics())
            return
        }

        val cycleLengths = completedRecords.windowed(2) { pair ->
            ChronoUnit.DAYS.between(pair[1].startDate, pair[0].startDate).toInt()
        }.filter { it in 20..45 }

        val periodLengths = completedRecords.map { it.getDurationDays() }.filter { it in 2..10 }

        val avgCycle = if (cycleLengths.isNotEmpty()) cycleLengths.average() else null
        val avgPeriod = if (periodLengths.isNotEmpty()) periodLengths.average() else null
        val variation = if (cycleLengths.size >= 2) {
            val mean = cycleLengths.average()
            val variance = cycleLengths.map { (it - mean) * (it - mean) }.average()
            sqrt(variance)
        } else null

        _statistics.postValue(
            CycleStatistics(
                cycleLengths = cycleLengths,
                periodLengths = periodLengths,
                averageCycleLength = avgCycle,
                averagePeriodLength = avgPeriod,
                cycleVariation = variation
            )
        )
    }
}
