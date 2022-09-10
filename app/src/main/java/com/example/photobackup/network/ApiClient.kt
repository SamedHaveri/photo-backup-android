package com.example.photobackup.network

import com.example.photobackup.network.requestDTO.AuthenticationRequestDTO
import com.example.photobackup.network.responseDTO.ImageData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:8080/"
    private val gson: Gson = GsonBuilder().setDateFormat("dd-MM-yyyy").create();

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

}

interface ApiService {
//    @GET("authenticate")
//    fun authenticatead(@Query("page") page: String): Call<CharacterResponse>

    @POST("authenticate")
    fun authenticate(@Body auth: AuthenticationRequestDTO): Call<AuthenticationResponse>

    @GET("images")
    fun getImages(): Call<List<ImageData>>

    @GET("images/download/id{id}")
    fun downloadImage(@Path("id") id: Int, @Header("Authorization") token: String): Call<ResponseBody>

}