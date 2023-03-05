package com.example.photobackup.repository

import com.example.photobackup.api.ApiHelper
import com.example.photobackup.models.auth.AuthRequest
import okhttp3.MultipartBody
import javax.inject.Inject

class MainRepository @Inject constructor(
    private val apiHelper: ApiHelper
) {
    suspend fun authenticate(authRequest: AuthRequest) = apiHelper.authenticate(authRequest)
    suspend fun getImages(authToken:String) = apiHelper.getImages(authToken)
    suspend fun uploadMedia(authToken: String, file : MultipartBody.Part) = apiHelper.uploadMedia(authToken, file)
}