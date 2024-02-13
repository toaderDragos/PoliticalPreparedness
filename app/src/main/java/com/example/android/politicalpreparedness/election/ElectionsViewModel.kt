package com.example.android.politicalpreparedness.election

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.android.politicalpreparedness.database.ElectionDataSource
import com.example.android.politicalpreparedness.database.ElectionRepository
import com.example.android.politicalpreparedness.database.utils.Result
import com.example.android.politicalpreparedness.network.models.Election
import kotlinx.coroutines.launch

enum class ElectionApiStatus { LOADING, ERROR, DONE }

// Construct ViewModel with Factory and provide election datasource
class ElectionsViewModel(
    app: Application,
    private val dataSource: ElectionDataSource,
    private val repository: ElectionRepository)
    : AndroidViewModel(app) {

    //Create live data val for upcoming elections
    var upcomingElectionsList = MutableLiveData<List<Election>>()
    var savedElectionsList = MutableLiveData<List<Election>>()
    private val showNoData: MutableLiveData<Boolean> = MutableLiveData()

    private val _apiStatus = MutableLiveData<ElectionApiStatus>()
    val apiStatus: LiveData<ElectionApiStatus>
        get() = _apiStatus

    // The internal MutableLiveData that stores the status of the most recent request - used for showing loading signs or errors
    private val _status = MutableLiveData<ElectionApiStatus>()
    // The external immutable LiveData for the request status
    val status: LiveData<ElectionApiStatus>
        get() = _status

    // Information from the API and display it on screen
    fun getUpcomingElections() {
        viewModelScope.launch {
            try {
                _apiStatus.value = ElectionApiStatus.LOADING
                val elections = repository.refreshElections()
                upcomingElectionsList.value = elections
                _apiStatus.value = ElectionApiStatus.DONE
            } catch (e: Exception) {
                _apiStatus.value = ElectionApiStatus.ERROR
                upcomingElectionsList.value = listOf() // Consider setting to an empty list or handling the error appropriately
            }
        }
    }

    fun getSavedElections() {
        viewModelScope.launch {
            _apiStatus.value = ElectionApiStatus.LOADING
            when (val result = dataSource.getAllSavedElections()) {
                is Result.Success -> {
                    val dataList = ArrayList<Election>()
                    dataList.addAll(result.data.map { election ->
                        // map the Election data from the DB to the be ready to be displayed on the UI
                        Election(election.id, election.name, election.electionDay, election.division)
                    })
                    savedElectionsList.value = dataList
                    _apiStatus.value = ElectionApiStatus.DONE
                }
                is Result.Error -> {
                    savedElectionsList.value = emptyList() // Set an empty list or a specific error value
                    _apiStatus.value = ElectionApiStatus.ERROR
                }
            }
            // Check if no data has to be shown
            invalidateShowNoData()
        }
    }

    /**
     * Inform the user that there's not any data if the savedElectionsList is empty
     */
    private fun invalidateShowNoData() {
        showNoData.value = savedElectionsList.value == null || savedElectionsList.value!!.isEmpty()
    }

}


