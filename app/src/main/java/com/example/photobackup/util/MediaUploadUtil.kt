package com.example.photobackup.util

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.work.*
import com.example.photobackup.data.MediaDatabase
import com.example.photobackup.data.entity.MediaBackup
import com.example.photobackup.data.repository.MediaBackupRepository
import com.example.photobackup.other.Constants
import com.example.photobackup.service.MediaContentJob
import com.example.photobackup.service.MediaUploadWorker

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
            workManager.enqueueUniqueWork("media_upload", ExistingWorkPolicy.KEEP, mediaUploadWorker)
        }
        suspend fun syncAndUploadIfNeeded(applicationContext: Context, workManager: WorkManager) {
            val mediaDatabase = MediaDatabase.getDatabase(applicationContext)
            val mediaBackupRepository = MediaBackupRepository(mediaDatabase.mediaBackup())
            syncDatabase(mediaBackupRepository, applicationContext.contentResolver)
            if(mediaBackupRepository.existsMediaToUpload()){
                enqueueUploadWorker(workManager)
            }
        }
    }
}