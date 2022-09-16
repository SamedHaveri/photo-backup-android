package com.example.photobackup.api

import com.example.photobackup.models.auth.AuthRequest
import com.example.photobackup.models.auth.AuthResponse
import retrofit2.Response
import javax.inject.Inject

class ApiHelperImpl @Inject constructor(
    private val apiService: ApiService
):ApiHelper{
    override suspend fun authenticate(authRequest: AuthRequest): Response<AuthResponse> = apiService.authenticate(authRequest)
}