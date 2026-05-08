package com.periodtracker.data

import androidx.lifecycle.LiveData
import androidx.room.*
import java.time.LocalDate

@Dao
interface PeriodRecordDao {
    @Query("SELECT * FROM period_records ORDER BY startDate DESC")
    fun getAllRecords(): LiveData<List<PeriodRecord>>

    @Query("SELECT * FROM period_records WHERE id = :id")
    suspend fun getRecordById(id: String): PeriodRecord?

    @Query("SELECT * FROM period_records WHERE endDate IS NULL LIMIT 1")
    suspend fun getActivePeriod(): PeriodRecord?

    @Query("SELECT * FROM period_records WHERE :date >= startDate AND (endDate IS NULL OR :date <= endDate)")
    suspend fun getRecordForDate(date: LocalDate): PeriodRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: PeriodRecord)

    @Update
    suspend fun update(record: PeriodRecord)

    @Delete
    suspend fun delete(record: PeriodRecord)

    @Query("SELECT * FROM period_records ORDER BY startDate DESC")
    suspend fun getAllRecordsSync(): List<PeriodRecord>
}
