package com.example.android.politicalpreparedness.database

import com.example.android.politicalpreparedness.network.models.Election

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Exception
import com.example.android.politicalpreparedness.database.utils.Result
import com.example.android.politicalpreparedness.network.CivicsApi
import com.example.android.politicalpreparedness.network.apiKey
import com.example.android.politicalpreparedness.network.models.Address
import com.example.android.politicalpreparedness.network.models.ElectionResponse
import com.example.android.politicalpreparedness.network.models.RepresentativeResponse
import com.example.android.politicalpreparedness.network.models.VoterInfoResponse

/**
 * Main entry point for accessing election data. Implements ElectionDataSource interface.
 * Created with dependency injection in mind.
 */
class ElectionRepository(
    private val electionDao: ElectionDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ElectionDataSource {

    /*** Get all elections saved in the database*/
    override suspend fun getAllSavedElections(): Result<List<Election>> = withContext(ioDispatcher) {
        return@withContext try {
            Result.Success(electionDao.getSavedElections())
        } catch (e: Exception) {
            Result.Error(e.localizedMessage)
        }
    }

    /*** Get a saved election by its id */
    override suspend fun getElectionFromDB(id: Int): Result<Election> = withContext(ioDispatcher) {
        try {
            val election = electionDao.getElectionById(id)
            if (election != null) {
                return@withContext Result.Success(election)
            } else {
                return@withContext Result.Error("Election not found!")
            }
        } catch (e: Exception) {
            return@withContext Result.Error(e.localizedMessage)
        }
    }

    /*** Save an election to the database */
    override suspend fun saveElection(election: Election) {
        withContext(ioDispatcher) { electionDao.saveElection(election) }
    }

    /** Delete an election from the database */
    override suspend fun deleteElection(id: Int) {
        withContext(ioDispatcher) { electionDao.deleteElection(id) }
    }

    /**  Clear all elections from the database */
    override suspend fun deleteAllElections() {
        withContext(ioDispatcher) { electionDao.deleteAllElections() }
    }

    /**  Get upcoming elections from Google civics API */
    suspend fun refreshElections(): List<Election>  = withContext(ioDispatcher) {
            try {
                val electionResponse: ElectionResponse = CivicsApi.retrofitService.getElections(apiKey)
                println("dra electionResponse: $electionResponse.elections")
                return@withContext electionResponse.elections
            } catch (e: Exception) {
                println("dra exception in fetching elections: $e.localizedMessage")
                e.printStackTrace()
                return@withContext emptyList()
            }
        }

    /** Get voter info from the API */
    suspend fun getVoterInfo(electionId: Int, voterAddress: Address): Result<VoterInfoResponse>
    = withContext(ioDispatcher) {
        try {
            val response = CivicsApi.retrofitService.getVoterInfo(voterAddress.city, electionId)
            return@withContext Result.Success(response)
        } catch (e: Exception) {
            println("Error fetching voter info: $e.localizedMessage")
            e.printStackTrace()
            return@withContext Result.Error(e.localizedMessage)
        }
    }

    /** Get representatives from the API */
    suspend fun getRepresentatives(address: Address): Result<RepresentativeResponse> = withContext(ioDispatcher) {
        try {
            val response = CivicsApi.retrofitService.getRepresentatives(address.toFormattedString())
            return@withContext Result.Success(response)
        } catch (e: Exception) {
            println("Error fetching representatives: $e.localizedMessage")
            e.printStackTrace()
            return@withContext Result.Error(e.localizedMessage)
        }

    }

}

