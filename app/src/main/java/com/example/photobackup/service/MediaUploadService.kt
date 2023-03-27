package com.example.photobackup.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.photobackup.R
import com.example.photobackup.data.MediaDatabase
import com.example.photobackup.data.entity.MediaBackup
import com.example.photobackup.data.repository.MediaBackupRepository
import com.example.photobackup.models.parcelable.MediaBackupParcelable
import com.example.photobackup.models.parcelable.MediaBackupParcelableCnv
import com.example.photobackup.other.Constants
import com.example.photobackup.ui.main.photos.PhotosFragment
import okhttp3.*
import okio.BufferedSink
import okio.Okio
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class MediaUploadService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("UploadService", "STARTED SERVICE")

        if (intent == null || intent.extras == null) {
            //stop service
            stopSelf()
            return START_NOT_STICKY
        }

        val bundle = intent.extras
        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle?.getParcelableArrayList("data", MediaBackupParcelable::class.java)
        } else {
            bundle?.getParcelableArrayList("data")
        }
        if (data == null) {
            //stop service
            stopSelf()
            return START_NOT_STICKY
        }
        val convertedData = MediaBackupParcelableCnv.convert(data)

        val mediaDatabase = MediaDatabase.getDatabase(applicationContext)

        //todo make this with dependency injection

        executeUpload(convertedData, applicationContext, mediaDatabase)

        generateForegroundNotification()
        return START_NOT_STICKY
    }

    //Notififcation for ON-going
    private var iconNotification: Bitmap? = null
    private var notification: Notification? = null
    var mNotificationManager: NotificationManager? = null
    private val mNotificationId = 5476

    private fun generateForegroundNotification() {
        Log.d("UploadService", "generating notification")
        val intentMainLanding = Intent(this, PhotosFragment::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, intentMainLanding, PendingIntent.FLAG_IMMUTABLE)
        iconNotification = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
        if (mNotificationManager == null) {
            mNotificationManager =
                this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        assert(mNotificationManager != null)
        mNotificationManager?.createNotificationChannelGroup(
            NotificationChannelGroup("media_upload_group", "MediaUpload")
        )
        val notificationChannel =
            NotificationChannel("media_upload_channel", "Service Notifications",
                NotificationManager.IMPORTANCE_DEFAULT)
        notificationChannel.enableLights(false)
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_SECRET
        mNotificationManager?.createNotificationChannel(notificationChannel)
        val builder = NotificationCompat.Builder(this, "media_upload_channel")

        builder.setContentTitle("Media Uploading")
            .setTicker("Media is uploading")
            .setContentText("Device media is currently uploading") // swipe for more options
            .setSmallIcon(R.drawable.delete_24px)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setWhen(0)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
        if (iconNotification != null) {
            builder.setLargeIcon(Bitmap.createScaledBitmap(iconNotification!!, 128, 128, false))
        }
        builder.color = resources.getColor(R.color.primary)
        notification = builder.build()
        startForeground(mNotificationId, notification)

    }

    private fun executeUpload(
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
                stopSelf()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                Log.e("UploadMedia", "Thread error interrupt")
                stopSelf()
            } catch (_: Exception) {
                Log.e("UploadMedia", "Exception")
                stopSelf()
            }


            // Update UI on the main thread
            mainExecutor.execute {
                // You code logic goes here.
            }
        }
    }

}