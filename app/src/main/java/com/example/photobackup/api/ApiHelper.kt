package com.example.photobackup.api

import com.example.photobackup.models.auth.AuthRequest
import com.example.photobackup.models.auth.AuthResponse
import com.example.photobackup.models.imageDownload.ImageData
import retrofit2.Response

interface ApiHelper {
    suspend fun authenticate(authRequest: AuthRequest): Response<AuthResponse>
    suspend fun getImages(authToken:String): Response<List<ImageData>>
}