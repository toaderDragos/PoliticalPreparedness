package com.example.android.politicalpreparedness.election

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.android.politicalpreparedness.R
import com.example.android.politicalpreparedness.database.ElectionRepository
import com.example.android.politicalpreparedness.network.models.Address
import com.example.android.politicalpreparedness.network.models.Election
import com.example.android.politicalpreparedness.network.models.VoterInfoResponse
import kotlinx.coroutines.launch
import com.example.android.politicalpreparedness.database.utils.Result

class VoterInfoViewModel(app: Application, private val repository: ElectionRepository)
    : AndroidViewModel(app) {

    val vaddress = MutableLiveData<Address>()
    private val isSaved = MutableLiveData<Boolean>()

    private val _followButtonText = MutableLiveData<String>()
    val followButtonText: LiveData<String> = _followButtonText

    // Add live data to hold voter info
    private val voterInfo = MutableLiveData<VoterInfoResponse>()

    private val _votingLocationFinderUrl = MutableLiveData<String?>()
    val votingLocationFinderUrl: LiveData<String?> = _votingLocationFinderUrl

    private val _ballotInfoUrl = MutableLiveData<String?>()
    val ballotInfoUrl: LiveData<String?> = _ballotInfoUrl

    // Api call to get voter info from API
    fun getVoterInfo(electionId: Int, voterAddress: Address) {
        viewModelScope.launch {
            val voterResult  = repository.getVoterInfo(electionId, voterAddress)
            if (voterResult is com.example.android.politicalpreparedness.database.utils.Result.Success) {
                voterInfo.value = voterResult.data

                _votingLocationFinderUrl.value = voterInfo.value?.state?.firstOrNull()?.electionAdministrationBody?.votingLocationFinderUrl
                _ballotInfoUrl.value = voterInfo.value?.state?.firstOrNull()?.electionAdministrationBody?.ballotInfoUrl

                println("dra - VoterInfoViewModel - getVoterInfo - voterInfo.value: ${voterInfo.value}")
            } else {
                Toast.makeText(getApplication(), "Error getting voter info, the app works exclusively in the USA!", Toast.LENGTH_LONG).show()
            }
        }
    }

    // If we have saved this election then we should show "Unfollow Election" else "Follow Election"
    fun isElectionSaved(electionId: Int): LiveData<Boolean> {
        viewModelScope.launch {
            val savedElection = repository.getElectionFromDB(electionId) is Result.Success
            isSaved.postValue(savedElection)
            _followButtonText.postValue(if (savedElection) "Unfollow Election" else "Follow Election")
        }
        return isSaved
    }

    private fun unfollowElection(election: Election) {
        viewModelScope.launch {
            repository.deleteElection(election.id)
            isSaved.postValue(false) // Update saved state after DB operation
            _followButtonText.postValue("Follow Election")
        }
    }

    private fun followElection(election: Election) {
        viewModelScope.launch {
            repository.saveElection(election)
            isSaved.postValue(true) // Update saved state after DB operation
            _followButtonText.postValue("Unfollow Election")
        }
    }

    fun toggleElectionFollowed(election: Election) {
        if (isSaved.value == true) {
            unfollowElection(election)
        } else {
            followElection(election)
        }
    }

    // if the app is launched only in USA then we don't need to show the voter info
    fun isCountryUSA(): Boolean {
        for (state in getApplication<Application>().resources.getStringArray(R.array.states)) {
            if (vaddress.value?.state == state) {
                return true
            }
        }
        return false
    }


}