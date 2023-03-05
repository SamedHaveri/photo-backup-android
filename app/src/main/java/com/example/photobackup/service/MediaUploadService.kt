package com.example.photobackup.service

import android.R
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.*
import android.os.Process.THREAD_PRIORITY_BACKGROUND
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.photobackup.ui.main.photos.PhotosFragment


class MediaUploadService : Service() {

    private var serviceLooper: Looper? = null
    private var serviceHandler: ServiceHandler? = null

    // Handler that receives messages from the thread
    private inner class ServiceHandler(looper: Looper) : Handler(looper) {

        override fun handleMessage(msg: Message) {
            // Normally we would do some work here, like download a file.
            // For our sample, we just sleep for 5 seconds.
            try {
                for (path in msg.data.getStringArrayList("filePaths")!!){
                    Log.d("PhotosContent", "uploading :"+path)
                }
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
                // Restore interrupt status.
                Thread.currentThread().interrupt()
            }

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        createNotification();
        HandlerThread("ServiceStartArguments", THREAD_PRIORITY_BACKGROUND).apply {
            start()

            // Get the HandlerThread's Looper and use it for our Handler
            serviceLooper = looper
            serviceHandler = ServiceHandler(looper)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d("PhotosContent", "starting service for upload")

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        serviceHandler?.obtainMessage()?.also { msg ->
            msg.arg1 = startId
            msg.data = intent.extras
            serviceHandler?.sendMessage(msg)
        }

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onDestroy() {
        Log.d("PhotosContent", "stopped service for upload")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(){
        val pendingIntent: PendingIntent =
            Intent(this, PhotosFragment::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE)
            }

        val notification: Notification = Notification.Builder(this, "com.example.photobackup.service.MediaUploadService")
            .setContentTitle("title")
            .setContentText("ContentText")
            .setSmallIcon(R.drawable.bottom_bar)
            .setContentIntent(pendingIntent)
//            .setTicker(getText(R.string.ticker_text))
            .build()

// Notification ID cannot be 0.
        startForeground(1, notification)
    }

}
