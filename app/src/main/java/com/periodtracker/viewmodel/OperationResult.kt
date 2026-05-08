package com.periodtracker.viewmodel

sealed class OperationResult {
    data class Success(val message: String) : OperationResult()
    data class Error(val message: String) : OperationResult()
}
