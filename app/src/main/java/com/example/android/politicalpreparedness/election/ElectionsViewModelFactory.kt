package com.example.android.politicalpreparedness.election

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.android.politicalpreparedness.database.ElectionDataSource
import com.example.android.politicalpreparedness.database.ElectionRepository

class ElectionsViewModelFactory(
    private val app: Application,
    private val dataSource: ElectionDataSource,
    private val repository: ElectionRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ElectionsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ElectionsViewModel(app, dataSource, repository) as T
        }
        throw IllegalArgumentException("Unable to construct viewmodel")
    }
}
