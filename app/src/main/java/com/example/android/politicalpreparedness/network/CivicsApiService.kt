package com.example.android.politicalpreparedness.network

import DateJsonAdapter
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import com.example.android.politicalpreparedness.BuildConfig;
import com.example.android.politicalpreparedness.network.jsonadapter.ElectionAdapter
import com.example.android.politicalpreparedness.network.models.ElectionResponse
import com.example.android.politicalpreparedness.network.models.RepresentativeResponse
import com.example.android.politicalpreparedness.network.models.VoterInfoResponse

private const val BASE_URL = "https://www.googleapis.com/civicinfo/v2/"
private const val VOTER_INFO_URL = "voterinfo"
private const val ELECTIONS_URL = "elections"
private const val REPRESENTATIVE_INFO_BY_ADDRESS_URL = "representatives"
private const val REPRESENTATIVE_INFO_BY_DIVISION_URL = "representatives/ocdDivisionId"
const val apiKey = BuildConfig.API_KEY

    // Added custom adapter ElectionAdapter
    // Added adapter for Java Date
private val moshi = Moshi.Builder()
        .add(ElectionAdapter())
        .add(KotlinJsonAdapterFactory())
        .add(DateJsonAdapter())
        .build()

private val retrofit = Retrofit.Builder()
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .client(CivicsHttpClient.getClient())
        .baseUrl(BASE_URL)
        .build()

/**
 *  Documentation for the Google Civics API Service can be found at https://developers.google.com/civic-information/docs/v2
 */

interface CivicsApiService {
    // Elections API Call - maybe add a query parameter to search for current upcoming elections
    @GET(ELECTIONS_URL)
    suspend fun getElections(@Query("api_key") yourKey: String): ElectionResponse

    // Voterinfo API Call with address query (required)
    @GET(VOTER_INFO_URL)
    suspend fun getVoterInfo(@Query("address") address: String,
                             @Query("electionId") electionId: Int)
    : VoterInfoResponse

    // Representatives API Call
    @GET(REPRESENTATIVE_INFO_BY_ADDRESS_URL)
    suspend fun getRepresentatives(@Query("address") address: String): RepresentativeResponse

    // THis const should be concatenated with the division id - it is wrong now
    @GET(REPRESENTATIVE_INFO_BY_DIVISION_URL)
    suspend fun getRepresentativesByDivision(@Query("ocdDivisionId") ocdDivisionId: String, ): String
}

object CivicsApi {
    val retrofitService: CivicsApiService by lazy {
        retrofit.create(CivicsApiService::class.java)
    }
}