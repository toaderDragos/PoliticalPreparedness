package com.example.android.politicalpreparedness.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.android.politicalpreparedness.network.models.Election

@Dao
interface ElectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveElection(election: Election)

    // Select all election query
    @Query("SELECT * FROM election_table")
    fun getSavedElections(): List<Election>

    // Select single election query // check if this is a string or int
    @Query("SELECT * FROM election_table WHERE id = :id")
    fun getElectionById(id: Int): Election?

    // Delete query
    @Query("DELETE FROM election_table WHERE id = :id")
    fun deleteElection(id: Int): Int

    // Clear query
    @Query("DELETE FROM election_table")
    fun deleteAllElections()

    // Save all downloaded elections
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveAllElections(toTypedArray: Array<Election>)

}