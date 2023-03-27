package com.example.photobackup.service

import android.content.Context
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.photobackup.R
import com.example.photobackup.data.MediaDatabase
import com.example.photobackup.data.entity.MediaBackup
import com.example.photobackup.data.repository.MediaBackupRepository
import com.example.photobackup.other.Constants
import okhttp3.*
import okio.BufferedSink
import okio.Okio
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Singleton


@Singleton
class MediaUploadExecutor {

    fun executeUpload(
        mediasToUpload: List<MediaBackup>,
        context: Context,
        mediaDatabase: MediaDatabase,
    ) {

        val prefs = context.getSharedPreferences(R.string.prefs.toString(), 0)
        val mediaBackupRepository = MediaBackupRepository(mediaDatabase.mediaBackup())

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
                    val fileUri = fileToUpload.toUri()

                    val contentResolver = context.contentResolver

                    var contentType = contentResolver.getType(fileUri)
                    val extension = MimeTypeMap.getFileExtensionFromUrl(fileToUpload.absolutePath)
                    if (extension != null) {
                        contentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                    }

                    val fd = contentResolver.openAssetFileDescriptor(fileUri, "r") ?: throw FileNotFoundException()

                    if(!fileToUpload.exists()){
                        Log.e("upload", "File does not exist:" + fileToUpload.absolutePath)
                        mediaBackupRepository.setMediaAsTriedButFailed(mediaToUpload.id)
                    }
                    val client = OkHttpClient().newBuilder()
                        .build()

                    val videoFile: RequestBody = object : RequestBody() {
                        override fun contentLength(): Long {
                            return fd.declaredLength
                        }

                        override fun contentType(): MediaType? {
                            return MediaType.parse(contentType)
                        }

                        @Throws(IOException::class)
                        override fun writeTo(sink: BufferedSink) {
                            fd.createInputStream()
                                .use { `is` -> sink.writeAll(Okio.buffer(Okio.source(`is`))) }
                        }
                    }
                    val requestBody: RequestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", fileToUpload.name, videoFile)
                        .build()
                    val request = Request.Builder()
                        .url(Constants.BASE_URL + "media/upload")
                        .method("POST", requestBody)
                        .addHeader("Authorization",
                            prefs.getString(R.string.token_key.toString(), "")!!)
                        .build()
                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            try {
                                fd.close()
                            } catch (ex: IOException) {
                                e.addSuppressed(ex)
                            }
                            Log.d("upload", "Upload unsuccessful:$e")
                            mediaBackupRepository.setMediaAsTriedButFailed(mediaToUpload.id)
                        }

                        @Throws(IOException::class)
                        override fun onResponse(call: Call, response: Response) {
                            Log.d("upload", "Upload successful")
                            mediaBackupRepository.setMediaAsUploaded(mediaToUpload.id)
                            fd.close()
                        }
                    })
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                Log.e("UploadMedia", "Thread error interrupt")
            } catch (_: Exception) {
                Log.e("UploadMedia", "Exception")
            }


            // Update UI on the main thread
            mainExecutor.execute {
                // You code logic goes here.
            }
        }
    }
}