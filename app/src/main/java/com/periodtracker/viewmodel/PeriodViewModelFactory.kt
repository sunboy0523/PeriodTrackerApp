package com.periodtracker.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PeriodViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PeriodViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PeriodViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
