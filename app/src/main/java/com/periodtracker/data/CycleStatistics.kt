package com.periodtracker.data

data class CycleStatistics(
    val cycleLengths: List<Int> = emptyList(),
    val periodLengths: List<Int> = emptyList(),
    val averageCycleLength: Double? = null,
    val averagePeriodLength: Double? = null,
    val cycleVariation: Double? = null
)
