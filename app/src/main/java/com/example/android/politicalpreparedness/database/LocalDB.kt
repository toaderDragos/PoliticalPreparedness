package com.example.android.politicalpreparedness.database

import android.content.Context
import androidx.room.Room

/*** Singleton class that is used to create a election database  */
object LocalDB {

    /**
     * static method that creates a election class and returns the DAO of the reminder
     */
    fun createElectionDao(context: Context): ElectionDao {
        return Room.databaseBuilder(
            context.applicationContext,
            ElectionDatabase::class.java, "election.db"
        ).build().electionDao
    }

}