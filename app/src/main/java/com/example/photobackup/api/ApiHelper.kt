package com.example.photobackup.api

import com.example.photobackup.models.AuthRequest
import com.example.photobackup.models.AuthResponse
import retrofit2.Response

interface ApiHelper {
    suspend fun authenticate(authRequest: AuthRequest): Response<AuthResponse>
}