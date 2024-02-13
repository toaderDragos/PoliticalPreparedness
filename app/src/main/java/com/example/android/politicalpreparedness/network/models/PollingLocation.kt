package com.example.android.politicalpreparedness.network.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

data class PollingLocation(
    val address: Address? = null,
    val notes: String? = null,
    val pollingHours: String? = null,
    val name: String? = null,
    val voterServices: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val sources: List<Source>? = null
)

@JsonClass(generateAdapter = true)
data class Source(
    @Json(name = "name") val name: String? = null,
    @Json(name = "official") val official: Boolean? = null
)