package com.example.photobackup.di

import com.example.photobackup.api.ApiHelper
import com.example.photobackup.api.ApiHelperImpl
import com.example.photobackup.api.ApiService
import com.example.photobackup.other.Constants
import com.example.photobackup.service.MediaUploadExecutor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Named("BASE_URL")
    @Provides
    fun provideBaseUrl() = Constants.BASE_URL

    @Singleton
    @Provides
    fun provideOkHttpClient() = OkHttpClient
        .Builder()
        .build()

    @Singleton
    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient, @Named("BASE_URL") BASE_URL:String): Retrofit = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit) = retrofit.create(ApiService::class.java)

    @Provides
    @Singleton
    fun provideApiHelper(apiHelper: ApiHelperImpl): ApiHelper = apiHelper

    //todo figure out dependency injection :/
    @Provides
    @Singleton
    fun provideMediaUploadExecutor(mediaUploadExecutor: MediaUploadExecutor): MediaUploadExecutor = mediaUploadExecutor

}