package com.example.photobackup.service

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.job.JobInfo
import android.app.job.JobInfo.TriggerContentUri
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.work.*
import com.example.photobackup.data.MediaDatabase
import com.example.photobackup.data.entity.MediaBackup
import com.example.photobackup.data.repository.MediaBackupRepository
import com.example.photobackup.models.parcelable.MediaBackupParcelable
import com.example.photobackup.other.Constants
import com.google.gson.Gson
import kotlin.streams.toList


/**
 * Example stub job to monitor when there is a change to photos in the media provider.
 */
@SuppressLint("SpecifyJobSchedulerIdRange")
class MediaContentJob : JobService() {
    var mRunningParams: JobParameters? = null

    override fun onStartJob(params: JobParameters): Boolean {
        mRunningParams = params
        Log.d("Job", "Job started")

        if(isMyServiceRunning(MediaUploadService::class.java)){
            Log.d("Job", "Upload not possible, uploadService is running")
            return true
        }

        // Did we trigger due to a content change?
        if (params.triggeredContentAuthorities != null) {
            Log.d("Job", "Auth triggered")
            val mediaDatabase = MediaDatabase.getDatabase(applicationContext)
            val mediaBackupRepository = MediaBackupRepository(mediaDatabase.mediaBackup())
            syncDatabase(mediaBackupRepository)
            val listOfMediaToUpload = mediaBackupRepository.getMediaToUpload()
            if (listOfMediaToUpload.isNotEmpty()) {
                Log.d("Job", "We have media to upload")
                //todo figure out dependency injection and implement here
                //todo optional (possible issue of user renaming the file before upload was successful "unlikely but possible")
                // throwing FileNotFound on upload
                // query in MediaStore and get absolutePaths (we have that in localDb but it could have changed) / also verify Dirs

                val mediaToUploadJson = Gson().toJson(listOfMediaToUpload)
                val data = Data.Builder()
                    .putString("data", mediaToUploadJson)
                    .build()

                val mediaUploadWorker = OneTimeWorkRequestBuilder<MediaUploadWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .setInputData(data)
                    .build()
                val workManager = WorkManager.getInstance(applicationContext)
                workManager.enqueueUniqueWork("media_upload", ExistingWorkPolicy.KEEP, mediaUploadWorker)
            } else {
                Log.d("upload", "synced but no files to upload")
            }
        } else {
            //no media triggered "wierd"
        }

        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        Log.d("PhotosContent", "Job Stopped (photos)")
        return false
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

        val cursor = contentResolver.query(
            queryUri,
            PROJECTION,
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
                if (!imagePath.startsWith(DCIM_DIR))
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

        val cursorVideo = contentResolver.query(
            queryUriVideo,
            PROJECTION_VIDEO,
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
                if (!path.startsWith(DCIM_DIR))
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

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    companion object {
        //in future we will support different folders to observe all in one job i guess ?
        const val jobId = 546863151; //todo make this better than just placing it here ?

        // The root URI of the media provider, to monitor for generic changes to its content.
        val MEDIA_URI = Uri.parse("content://" + MediaStore.AUTHORITY + "/")

        // Path segments for image-specific URIs in the provider.
        val EXTERNAL_PATH_SEGMENTS = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.pathSegments

        // The columns we want to retrieve about a particular image.
        val PROJECTION = arrayOf(
            MediaStore.Images.ImageColumns._ID,
            MediaStore.Images.ImageColumns.DATA,
            MediaStore.Images.ImageColumns.DATE_ADDED,
            MediaStore.Images.ImageColumns.ORIENTATION
        )
        val PROJECTION_VIDEO = arrayOf(
            MediaStore.Video.VideoColumns._ID,
            MediaStore.Video.VideoColumns.DATA,
            MediaStore.Video.VideoColumns.DATE_ADDED,
//            MediaStore.Video.VideoColumns.ORIENTATION
        )

        // This is the external storage directory where cameras place pictures.
        val DCIM_DIR = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM).path

        // A pre-built JobInfo we use for scheduling our job.
        var JOB_INFO: JobInfo? = null

        init {
            val builder = JobInfo.Builder(jobId,
                ComponentName("com.example.photobackup.service", MediaContentJob::class.java.name))
            // Look for specific changes to images in the provider.
            builder.addTriggerContentUri(TriggerContentUri(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS))
            builder.addTriggerContentUri(TriggerContentUri(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS))
            // Also look for general reports of changes in the overall provider.
//            builder.addTriggerContentUri(TriggerContentUri(MEDIA_URI, 0))
            JOB_INFO = builder.setRequiredNetworkType(NetworkType.CONNECTED.ordinal).build()
        }

        // Schedule this job, replace any existing one.
        fun scheduleJob(context: Context) {
            val builder = JobInfo.Builder(jobId,
                ComponentName(context, MediaContentJob::class.java.name))
            // Look for specific changes to images in the provider.
            builder.addTriggerContentUri(TriggerContentUri(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS))
            builder.addTriggerContentUri(TriggerContentUri(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS))
            // Also look for general reports of changes in the overall provider.
//            builder.addTriggerContentUri(TriggerContentUri(MEDIA_URI, 0))
            JOB_INFO = builder.setRequiredNetworkType(NetworkType.CONNECTED.ordinal).build()
            val js = context.getSystemService(JobScheduler::class.java)
            js.schedule(JOB_INFO!!)
            Log.i("PhotosContent", "JOB SCHEDULED!")
        }

        // Check whether this job is currently scheduled.
        fun isScheduled(context: Context): Boolean {
            val js = context.getSystemService(JobScheduler::class.java)
            val jobs = js.allPendingJobs ?: return false
            for (i in jobs.indices) {
                if (jobs[i].id == jobId) {
                    return true
                }
            }
            return false
        }

        // Cancel this job, if currently scheduled.
        fun cancelJob(context: Context) {
            val js = context.getSystemService(JobScheduler::class.java)
            js.cancel(jobId)
        }
    }
}