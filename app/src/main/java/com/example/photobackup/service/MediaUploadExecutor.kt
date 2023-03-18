package com.example.photobackup.service

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.photobackup.R
import com.example.photobackup.data.MediaDatabase
import com.example.photobackup.data.entity.MediaToUpload
import com.example.photobackup.data.entity.UploadedMedia
import com.example.photobackup.data.repository.MediaToUploadRepository
import com.example.photobackup.data.repository.UploadedMediaRepository
import com.example.photobackup.other.Constants
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.*
import java.io.File
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Singleton
import kotlin.math.log

@Singleton
class MediaUploadExecutor {

    fun executeUpload(mediasToUpload:ArrayList<MediaToUpload>, context : Context, mediaDatabase: MediaDatabase) {

        val prefs = context.getSharedPreferences(R.string.prefs.toString(),0)
        val mediaToUploadRepository = MediaToUploadRepository(mediaDatabase.mediaToUploadDao())
        val uploadedMediaRepository = UploadedMediaRepository(mediaDatabase.uploadedMediaDao())

        // Create an executor that executes tasks in the main thread.
        val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

        // Create an executor that executes tasks in a background thread.
        val backgroundExecutor = Executors.newSingleThreadScheduledExecutor()

        // Execute a task in the background thread.
        backgroundExecutor.execute {
            //background thread file upload

            try {
                for (mediaToUpload in mediasToUpload) {
                    Log.d("UploadMedia", "uploading :" + mediaToUpload.absolutePath)
                    val fileToUpload = File(mediaToUpload.absolutePath)
                    val client = OkHttpClient().newBuilder()
                        .build()
                    val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("file", fileToUpload.name,
                            RequestBody.create(MediaType.parse("application/octet-stream"),
                                fileToUpload))
                        .build()
                    val request = Request.Builder()
                        .url(Constants.BASE_URL+"media/upload")
                        .method("POST", body)
                        .addHeader("Authorization", prefs.getString(R.string.token_key.toString(), "")!!)
                        .build()
                    try {
                        val response = client.newCall(request).execute()
                        //todo if token expired use refresh token (when implemented) to get a new token set it and resend file
                        // Restore interrupt status.
                        Log.d("upload", "code:"+response.code())
                        if (!response.isSuccessful){
                            Log.d("upload", "Upload unsuccessful:"+response.code())
                            runBlocking { launch {
                                mediaToUploadRepository.insert(mediaToUpload)
                            } }
                            mediaToUploadRepository.deleteMediaByUri(mediaToUpload.uriId, mediaToUpload.mediaType)
                        }
                        else{
                            Log.d("upload", "Upload successful")
                            runBlocking { launch {
                                val uploadedMedia = UploadedMedia(0, mediaToUpload.uriPath)
                                uploadedMediaRepository.insertUploadedMedia(uploadedMedia)
                            } }
                            mediaToUploadRepository.deleteMediaByUri(mediaToUpload.uriId, mediaToUpload.mediaType)
                        }
                    } catch (_: IOException) {
                        Log.d("UploadMedia", "Connection Issue.. saving for later")
                        runBlocking {launch {
                            mediaToUploadRepository.insert(mediaToUpload)
                        }}
                    }
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                Log.d("UploadMedia", "Thread error interrupt")
            } catch (_: Exception) {
                Log.d("UploadMedia", "Exception")
            }


            // Update UI on the main thread
            mainExecutor.execute {
                // You code logic goes here.
            }
        }
    }
}