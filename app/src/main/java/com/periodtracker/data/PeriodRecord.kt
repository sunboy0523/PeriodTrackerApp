package com.periodtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.util.UUID

@Entity(tableName = "period_records")
data class PeriodRecord(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val flowLevel: Int = 2,
    val symptoms: String = "",
    val notes: String = ""
) {
    fun containsDate(date: LocalDate): Boolean {
        return if (endDate != null) {
            !date.isBefore(startDate) && !date.isAfter(endDate)
        } else {
            date == startDate
        }
    }

    fun getDurationDays(): Int {
        return if (endDate != null) {
            java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        } else {
            java.time.temporal.ChronoUnit.DAYS.between(startDate, LocalDate.now()).toInt() + 1
        }
    }
}
