package com.example.android.politicalpreparedness.database


import com.example.android.politicalpreparedness.network.models.Election
import com.example.android.politicalpreparedness.database.utils.Result

/**
 * Main entry point for accessing election data.
 */
interface ElectionDataSource {
    suspend fun getAllSavedElections(): Result<List<Election>>
    suspend fun getElectionFromDB(id: Int): Result<Election>
    suspend fun saveElection(election: Election)
    suspend fun deleteElection(id: Int)
    suspend fun deleteAllElections()
}
