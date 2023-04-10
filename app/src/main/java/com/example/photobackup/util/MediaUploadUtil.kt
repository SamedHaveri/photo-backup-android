package com.example.photobackup.util

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import android.provider.MediaStore.Video.Media
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import androidx.work.*
import com.example.photobackup.R
import com.example.photobackup.data.MediaDatabase
import com.example.photobackup.data.entity.MediaBackup
import com.example.photobackup.data.repository.MediaBackupRepository
import com.example.photobackup.other.Constants
import com.example.photobackup.service.MediaContentJob
import com.example.photobackup.service.MediaUploadWorker
import okhttp3.*
import okio.BufferedSink
import okio.Okio
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.time.Duration

class MediaUploadUtil {
    companion object {
        fun syncDatabase(
            mediaBackupRepository: MediaBackupRepository,
            contentResolver: ContentResolver,
        ) {
            val latestMediaBackup = mediaBackupRepository.getLatestSyncedMedia() ?: return
            val listOfMediaBackupToSave = ArrayList<MediaBackup>()
            Log.d("Sync", "Starting Media Sync")
            //get mediaStore entries where date_added is older than the currently locally stored mediaData (mediaBackup)
            val selection = "${MediaStore.Images.Media.DATE_ADDED} > ?"
            val selectionArgs = arrayOf(latestMediaBackup.dateAdded.toString())
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            val cursor = contentResolver.query(
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
                    Log.d("Sync", "mediaImg: $imagePath")

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

            val cursorVideo = contentResolver.query(
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

                    Log.d("Sync", "mediaVid: $path")

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
        fun enqueueUploadWorker(workManager: WorkManager) {
            val mediaUploadWorker = OneTimeWorkRequestBuilder<MediaUploadWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            workManager
                .enqueueUniqueWork("media_upload", ExistingWorkPolicy.KEEP, mediaUploadWorker)
        }
        suspend fun syncAndUploadIfNeeded(applicationContext: Context, workManager: WorkManager) {
            val mediaDatabase = MediaDatabase.getDatabase(applicationContext)
            val mediaBackupRepository = MediaBackupRepository(mediaDatabase.mediaBackup())
            syncDatabase(mediaBackupRepository, applicationContext.contentResolver)
            if(mediaBackupRepository.existsMediaToUpload()){
                enqueueUploadWorker(workManager)
            }
        }

        fun uploadMedias(context:Context){
            val prefs = context.getSharedPreferences(R.string.prefs.toString(), 0)
            val mediaDatabase = MediaDatabase.getDatabase(context)
            val mediaBackupRepository = MediaBackupRepository(mediaDatabase.mediaBackup())

            val mediasToUpload = mediaBackupRepository.getMediaToUpload() ?: return

            for (mediaToUpload in mediasToUpload) {

                try {
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
                        .readTimeout(Duration.ofHours(1))
                        .writeTimeout(Duration.ofHours(1))
                        .connectTimeout(Duration.ofHours(1))
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
                    if (re.isSuccessful) {
                        Log.d("uploadWorker", "Upload successful")
                        mediaBackupRepository.setMediaAsUploaded(mediaToUpload.id)
                        fd.close()
                    } else {
                        Log.d("uploadWorker", "Upload unsuccessful")
                        mediaBackupRepository.setMediaAsTriedButFailed(mediaToUpload.id)
                        fd.close()
                    }
                } catch (e: FileNotFoundException) {
                    //todo handle file not found
                    Log.e("uploadWorker", "Exception" + e.stackTraceToString())
                    mediaBackupRepository.setMediaAsTriedButFailed(mediaToUpload.id)
                } catch (e: Exception) {
                    Log.e("uploadWorker", "Exception" + e.stackTraceToString())
                }
            }
        }
    }
}