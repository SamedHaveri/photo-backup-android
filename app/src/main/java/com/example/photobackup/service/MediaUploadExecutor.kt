package com.example.photobackup.service

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.photobackup.R
import com.example.photobackup.other.Constants
import com.example.photobackup.repository.MainRepository
import com.example.photobackup.util.MyPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.*
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaUploadExecutor{

    fun executeUpload(paths: ArrayList<String>, context : Context) {

        val prefs = context.getSharedPreferences(R.string.prefs.toString(),0)


        // Create an executor that executes tasks in the main thread.
        val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

        // Create an executor that executes tasks in a background thread.
        val backgroundExecutor = Executors.newSingleThreadScheduledExecutor()

        // Execute a task in the background thread.
        backgroundExecutor.execute {
            //background thread file upload


            try {
                for (path in paths) {
                    Log.d("PhotosContent", "uploading :" + path)
                    val fileToUpload = File(path)

                    val client = OkHttpClient().newBuilder()
                        .build()
                    val mediaType = MediaType.parse("text/plain")
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
                    val response = client.newCall(request).execute()

                }
            } catch (e: InterruptedException) {
                //todo if resp code 403 use refresh token (when implemented) to get a new token set it and resend file
                // Restore interrupt status.
                Thread.currentThread().interrupt()
                Log.d("PhotosContent", "Thread error interrupt")
            }


            // Update UI on the main thread
            mainExecutor.execute {
                // You code logic goes here.
            }
        }
    }
}