package com.example.photobackup.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.photobackup.R
import com.example.photobackup.data.MediaDatabase
import com.example.photobackup.data.entity.MediaBackup
import com.example.photobackup.data.repository.MediaBackupRepository
import com.example.photobackup.other.Constants
import com.example.photobackup.util.MediaUploadUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import okio.BufferedSink
import okio.Okio
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.random.Random

class MediaUploadWorker(
    private val context: Context,
    private val workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("uploadWorker", "started Worker")
        val prefs = context.getSharedPreferences(R.string.prefs.toString(), 0)
        val mediaDatabase = MediaDatabase.getDatabase(context)
        val mediaBackupRepository = MediaBackupRepository(mediaDatabase.mediaBackup())

        Log.d("uploadWorker", "syncing database")
        MediaUploadUtil.syncDatabase(mediaBackupRepository, context.contentResolver)

        Log.d("uploadWorker", "getting media to upload")
        val mediasToUpload = mediaBackupRepository.getMediaToUpload() ?: return Result.success()

        startForegroundService()
        Log.d("uploadWorker", "trying to upload the media")
        try {
            for (mediaToUpload in mediasToUpload) {
                Log.d("uploadWorker", "uploading :" + mediaToUpload.absolutePath)
                val fileToUpload = File(mediaToUpload.absolutePath)
                val fileUri = fileToUpload.toUri()

                val contentResolver = context.contentResolver

                var contentType = contentResolver.getType(fileUri)
                val extension = MimeTypeMap.getFileExtensionFromUrl(fileToUpload.absolutePath)
                if (extension != null) {
                    contentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                }

                val fd = contentResolver.openAssetFileDescriptor(fileUri, "r")
                    ?: throw FileNotFoundException()

                if (!fileToUpload.exists()) {
                    Log.e("uploadWorker", "File does not exist:" + fileToUpload.absolutePath)
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
                val re = client.newCall(request).execute()
                if(re.isSuccessful){
                    Log.d("uploadWorker", "Upload successful")
                    mediaBackupRepository.setMediaAsUploaded(mediaToUpload.id)
                    fd.close()
                }else {
                    Log.d("uploadWorker", "Upload unsuccessful")
                    mediaBackupRepository.setMediaAsTriedButFailed(mediaToUpload.id)
                    fd.close()
                }
            }
            return Result.success()
        } catch (e: Exception) {
            Log.e("uploadWorker", "Exception" + e.stackTrace)
            return Result.failure()
        }

    }

    private suspend fun startForegroundService() {
        val channel = NotificationChannel(
            "media_upload",
            "Media Upload",
            NotificationManager.IMPORTANCE_HIGH,
        )
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
        setForeground(
            ForegroundInfo(
                Random.nextInt(),
                NotificationCompat.Builder(context, "media_upload")
                    .setSmallIcon(R.drawable.delete_24px)
                    .setContentTitle("Media Upload")
                    .setContentText("Uploading...")
                    .setOnlyAlertOnce(true)
                    .setOngoing(true)
                    .build()
            )
        )
    }

}