package com.example.photobackup.api

import com.example.photobackup.models.AuthRequest
import com.example.photobackup.models.AuthResponse
import com.example.photobackup.models.ImageData
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("authenticate")
    fun authenticate(@Body auth: AuthRequest): Response<AuthResponse>

    @GET("images")
    fun getImages(): Call<List<ImageData>>

    @GET("images/download/id{id}")
    fun downloadImage(@Path("id") id: Int, @Header("Authorization") token: String): Response<ResponseBody>

}