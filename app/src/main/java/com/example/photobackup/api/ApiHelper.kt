package com.example.photobackup.api

import com.example.photobackup.models.auth.AuthRequest
import com.example.photobackup.models.auth.AuthResponse
import com.example.photobackup.models.imageDownload.ImageData
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response

interface ApiHelper {
    suspend fun authenticate(authRequest: AuthRequest): Response<AuthResponse>
    suspend fun getImages(authToken:String): Response<List<ImageData>>
    suspend fun uploadMedia(authToken: String, file : MultipartBody.Part): Response<ResponseBody>
    suspend fun deleteMedia(authToken: String, id: Int): Response<ResponseBody>
}