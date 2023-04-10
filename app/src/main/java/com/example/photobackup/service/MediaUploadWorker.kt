package com.example.photobackup.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.photobackup.R
import com.example.photobackup.util.MediaUploadUtil
import kotlin.random.Random

class MediaUploadWorker(
    private val context: Context,
    private val workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("uploadWorker", "started Worker")
        startForegroundService()
        MediaUploadUtil.uploadMedias(applicationContext)
        return Result.success()
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