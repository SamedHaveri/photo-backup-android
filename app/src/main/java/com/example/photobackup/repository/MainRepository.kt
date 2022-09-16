package com.example.photobackup.repository

import com.example.photobackup.api.ApiHelper
import com.example.photobackup.models.auth.AuthRequest
import javax.inject.Inject

class MainRepository @Inject constructor(
    private val apiHelper: ApiHelper
) {
    suspend fun authenticate(authRequest: AuthRequest) = apiHelper.authenticate(authRequest)
}