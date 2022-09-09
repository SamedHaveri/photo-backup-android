package com.example.photobackup.network

import com.example.photobackup.network.bodyModel.AuthenticationBody
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:8080/"
    //private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val gson: Gson = GsonBuilder().setDateFormat("dd/MM/yyyy HH:mm:ss").create();

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
//
//    @GET("authenticate")
//    fun authenticatead(@Query("page") page: String): Call<CharacterResponse>

    @POST("authenticate")
    fun authenticate(@Body auth: AuthenticationBody): Call<AuthenticationResponse>

}