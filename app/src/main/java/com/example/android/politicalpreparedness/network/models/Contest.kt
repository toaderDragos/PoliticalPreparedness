package com.example.android.politicalpreparedness.network.models

data class Contest(
    val type: String,
    val office: String,
    val level: List<String>,
    val referendumTitle: String?
)
