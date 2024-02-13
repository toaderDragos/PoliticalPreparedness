package com.example.android.politicalpreparedness.representative

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.android.politicalpreparedness.database.ElectionRepository
import com.example.android.politicalpreparedness.database.utils.Result
import com.example.android.politicalpreparedness.network.models.Address
import com.example.android.politicalpreparedness.representative.model.Representative
import kotlinx.coroutines.launch

class RepresentativeViewModel(app: Application, private val repository: ElectionRepository)
: AndroidViewModel(app) {

    val address = MutableLiveData<Address>()
    val state = MutableLiveData<String>()

    private val _representatives = MutableLiveData<List<Representative>>()
    val representatives: LiveData<List<Representative>> = _representatives

    /***  The following code will prove helpful in constructing a representative from the API. This code combines the two nodes of the RepresentativeResponse into a single official :
    Note: getRepresentatives in the above code represents the method used to fetch data from the API
    Note: _representatives in the above code represents the established mutable live data housing representatives  */

    fun addressHasZipAndState(): Boolean {
        println("dra - RepresentativeViewModel - addressHasZipAndState - address.value: ${address.value}")
        val address = address.value ?: return false
        // Assuming Address contains fields like zip, state, etc. Adjust according to your model.
        return address.zip.isNotBlank() && address.state.isNotBlank()
    }

    fun getRepresentativesFromAPI(address:Address){
        viewModelScope.launch {
            when (val result = repository.getRepresentatives(address)) {
                is Result.Success -> {
                    _representatives.value = result.data.offices.flatMap { office -> office.getRepresentatives(result.data.officials) }
                    // println("dra - RepresentativeViewModel - getRepresentativesFromAPI - _representatives.value: ${_representatives.value}")
                }
                is Result.Error -> {
                    _representatives.value = emptyList()
                }
            }
        }
    }

    // A livedata object to be handled individually for the state for the SPINNER
    fun setAddress(address: Address) {
        state.value = address.state
    }

}
