package com.example.photobackup.models.auth

data class AuthResponse(
    val token: String,
    val id: Int,
    val username: String,
    val tokenExpiration: String,
)