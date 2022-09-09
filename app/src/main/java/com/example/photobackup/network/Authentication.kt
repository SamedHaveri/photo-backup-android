package com.example.photobackup.network

import com.squareup.moshi.Json
import java.time.LocalDateTime
import java.util.*

data class AuthenticationResponse(
    @Json(name = "token")
    val token: String,
    @Json(name = "id")
    val id: Int,
    @Json(name = "username")
    val username: String,
    @Json(name = "tokenExpiration")
    val tokenExpiration: Date,
)