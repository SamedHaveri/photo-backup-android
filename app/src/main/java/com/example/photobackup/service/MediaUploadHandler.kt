package com.example.photobackup.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.example.photobackup.R
import com.example.photobackup.other.Constants
import com.example.photobackup.repository.MainRepository
import com.example.photobackup.util.MyPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.*
import java.io.File
import java.net.ConnectException
import javax.inject.Inject

class MediaUploadHandler(looper: Looper) : Handler(looper) {

//    @Inject
//    lateinit var repository: MainRepository
//    @Inject
//    lateinit var preferences: MyPreference
    @Inject
    @ApplicationContext
    lateinit var context: Context

    val prefs = context.getSharedPreferences(R.string.prefs.toString(),0)

    override fun handleMessage(msg: Message) {
        try {
            for (path in msg.data.getStringArrayList("filePaths")!!) {
                Log.d("PhotosContent", "uploading :" + path)
                val fileToUpload = File(path)
//                val requestBody = RequestBody.create(MultipartBody.FORM, fileToUpload) //todo "image/" is static .. no bueno
//                Log.d("PhotosContent", "RequestBody created")
//                val part =
//                    MultipartBody.Part.createFormData("file", fileToUpload.name, requestBody)
//                Log.d("PhotosContent", "PartFile created")


                Log.d("tokenUpload", prefs.getString(R.string.token_key.toString(), "")!!)


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


//                runBlocking {
//                    Log.d("PhotosContent", "runBlocking")
//                    launch {
//                        Log.d("PhotosContent", "lounching coroutine")
//                        delay(1000L)
//                        try {
//                            Log.d("PhotosContent", "uploading finally")
//                            repository.uploadMedia(preferences.getStoredToken(), part).let {
//                                if (it.code() == 200) {
//                                    Log.d("PhotosContent", "UPLAODED!")
//                                } else if (it.code() == 403) {
//                                    //need auth
//                                    Log.d("PhotosContent", "upload error 403")
//                                } else {
//                                    Log.d("PhotosContent", "upload error ?")
//                                }
//                            }
//                        } catch (ex: ConnectException) {
//                            Log.d("PhotosContent", "upload error conn")
//                        }
//                    }
//                }

            }
        } catch (e: InterruptedException) {
            // Restore interrupt status.
            Thread.currentThread().interrupt()
            Log.d("PhotosContent", "Thread error interrupt")
        }
    }
}