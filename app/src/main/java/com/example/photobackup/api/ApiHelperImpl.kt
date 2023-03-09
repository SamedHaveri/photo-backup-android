package com.example.photobackup.api

import com.example.photobackup.models.auth.AuthRequest
import com.example.photobackup.models.auth.AuthResponse
import com.example.photobackup.models.imageDownload.ImageData
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject

class ApiHelperImpl @Inject constructor(
    private val apiService: ApiService
):ApiHelper{
    override suspend fun authenticate(authRequest: AuthRequest): Response<AuthResponse> = apiService.authenticate(authRequest)
    override suspend fun getImages(authToken:String): Response<List<ImageData>> = apiService.getImages(authToken)
    override suspend fun uploadMedia(authToken: String, file : MultipartBody.Part): Response<ResponseBody> = apiService.uploadMedia(authToken, file)
    override suspend fun deleteMedia(authToken: String, id: Int): Response<ResponseBody> = apiService.deleteMedia(authToken, id)
}