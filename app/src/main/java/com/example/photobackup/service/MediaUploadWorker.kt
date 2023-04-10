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
        startForegroundService()
        Log.d("uploadWorker", "started as foreground service")
        val prefs = context.getSharedPreferences(R.string.prefs.toString(), 0)
        val mediaDatabase = MediaDatabase.getDatabase(context)
        val mediaBackupRepository = MediaBackupRepository(mediaDatabase.mediaBackup())
        Log.d("uploadWorker", "initializing variables")

        Log.d("uploadWorker", "syncing database")
        syncDatabase(mediaBackupRepository)

        Log.d("uploadWorker", "getting media to upload")
        val mediasToUpload = mediaBackupRepository.getMediaToUpload()

        Log.d("uploadWorker", "trying to upload the media")
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

                val fd = contentResolver.openAssetFileDescriptor(fileUri, "r")
                    ?: throw FileNotFoundException()

                if (!fileToUpload.exists()) {
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
            return Result.success()
        } catch (_: Exception) {
            Log.e("UploadMedia", "Exception")
            return Result.failure()
        }

        //start worker again (worker has content trigger)

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

    private fun syncDatabase(mediaBackupRepository: MediaBackupRepository) {
        val latestMediaBackup = mediaBackupRepository.getLatestSyncedMedia()
        val listOfMediaBackupToSave = ArrayList<MediaBackup>()
        Log.d("Job", "Starting Media Sync")
        //get mediaStore entries where date_added is older than the currently locally stored mediaData (mediaBackup)
        val selection = "${MediaStore.Images.Media.DATE_ADDED} > ?"
        val selectionArgs = arrayOf(latestMediaBackup.dateAdded.toString())
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val cursor = context.contentResolver.query(
            queryUri,
            MediaContentJob.PROJECTION,
            selection,
            selectionArgs,
            sortOrder
        )
        if (cursor != null && cursor.moveToFirst()) {
            do {
                val imageId =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val dateAdded =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED))
                val imagePath =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                val orientation =
                    cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION))

                //filter what media to insert in db
                //for now only add DCIM_DIR media //todo make user choosing custom
                if (!imagePath.startsWith(MediaContentJob.DCIM_DIR))
                    continue
                Log.d("Job", "mediaImg: $imagePath")

                val mediaToSave = MediaBackup(
                    imageId,
                    "",
                    imagePath,
                    Constants.IMAGE_TYPE,
                    dateAdded,
                    orientation,
                    false,
                    0)
                listOfMediaBackupToSave.add(mediaToSave)
            } while (cursor.moveToNext())
            cursor.close()
        }

        //MediaStore Video Query

        //get mediaStore entries where date_added is older than the currently locally stored mediaData (mediaBackup)
        val selectionVideo = "${MediaStore.Video.Media.DATE_ADDED} > ?"
        val selectionArgsVideo = arrayOf(latestMediaBackup.dateAdded.toString())
        val sortOrderVideo = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        val queryUriVideo = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val cursorVideo = context.contentResolver.query(
            queryUriVideo,
            MediaContentJob.PROJECTION_VIDEO,
            selectionVideo,
            selectionArgsVideo,
            sortOrderVideo
        )
        if (cursorVideo != null && cursorVideo.moveToFirst()) {
            do {
                val imageId =
                    cursorVideo.getLong(cursorVideo.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                val dateAdded =
                    cursorVideo.getLong(cursorVideo.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED))
                val path =
                    cursorVideo.getString(cursorVideo.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                //todo this requires elevated Android API :D -- for now we dont need orientation but mby later fix
//                val orientation =
//                    cursorVideo.getInt(cursorVideo.getColumnIndexOrThrow(MediaStore.Video.Media.ORIENTATION))

                //filter what media to insert in db
                //for now only add DCIM_DIR media //todo make user choosing custom
                if (!path.startsWith(MediaContentJob.DCIM_DIR))
                    continue

                Log.d("Job", "mediaVid: $path")

                val mediaToSave = MediaBackup(
                    imageId,
                    "",
                    path,
                    Constants.VIDEO_TYPE,
                    dateAdded,
                    0, //fixme static orientation here
                    false,
                    0)
                listOfMediaBackupToSave.add(mediaToSave)

            } while (cursorVideo.moveToNext())
            cursorVideo.close()
        }
        mediaBackupRepository.insertAll(listOfMediaBackupToSave)
    }

}