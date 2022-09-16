package com.example.photobackup.api

import com.example.photobackup.models.auth.AuthRequest
import com.example.photobackup.models.auth.AuthResponse
import com.example.photobackup.models.imageDownload.ImageData
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("authenticate")
    suspend fun authenticate(@Body auth: AuthRequest): Response<AuthResponse>

    @GET("images")
    suspend fun getImages(): Call<List<ImageData>>

    @GET("images/download/id{id}")
    suspend fun downloadImage(@Path("id") id: Int, @Header("Authorization") token: String): Response<ResponseBody>

}