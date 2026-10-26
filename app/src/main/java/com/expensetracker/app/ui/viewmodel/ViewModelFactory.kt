package com.expensetracker.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.expensetracker.app.data.ai.GeminiManager
import com.expensetracker.app.data.repository.ExpenseRepository

class ViewModelFactory(
    private val repository: ExpenseRepository,
    private val geminiManager: GeminiManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository, geminiManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
