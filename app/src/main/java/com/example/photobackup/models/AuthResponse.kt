package com.example.photobackup.models

data class AuthResponse(
    val token: String,
    val id: Int,
    val username: String,
    val tokenExpiration: String,
)