package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val category: String = "عامة",
    val addedTimestamp: Long = System.currentTimeMillis()
)
