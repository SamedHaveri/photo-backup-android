package com.example.photobackup.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobInfo.TriggerContentUri
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.NetworkType
import com.example.photobackup.R
import com.example.photobackup.data.MediaDatabase
import com.example.photobackup.data.repository.MediaBackupRepository
import com.example.photobackup.other.Constants
import com.example.photobackup.util.MediaUploadUtil
import kotlin.random.Random


/**
 * Example stub job to monitor when there is a change to photos in the media provider.
 */
@SuppressLint("SpecifyJobSchedulerIdRange")
class MediaContentJob : JobService() {
    var mRunningParams: JobParameters? = null

    override fun onStartJob(params: JobParameters): Boolean {
        mRunningParams = params
        val mediaDatabase = MediaDatabase.getDatabase(applicationContext)
        val mediaBackupRepository = MediaBackupRepository(mediaDatabase.mediaBackup())
        Log.d("ContentJob", "ContentJob started")
        MediaUploadUtil.syncDatabase(mediaBackupRepository, contentResolver)

        // Did we trigger due to a content change? or do we have media to upload in db
        if (params.triggeredContentAuthorities != null || mediaBackupRepository.existsMediaToUpload()) {
            Log.d("ContentJob", "We have media to upload")
            startForegroundService()

            //upload
            val gfgThread = Thread {
                try {
                    MediaUploadUtil.uploadMedias(applicationContext)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            gfgThread.start()
        } else {
            Log.d("ContentJob", "synced but no files to upload")
        }

        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        Log.d("ContentJob", "Job Stopped (photos)")
        return false
    }

    private fun startForegroundService() {
        val channel = NotificationChannel(
            "media_upload",
            "Media Upload",
            NotificationManager.IMPORTANCE_HIGH,
        )
        val notificationManager =
            applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
        startForeground(
            Random.nextInt(),
            NotificationCompat.Builder(applicationContext, "media_upload")
                .setSmallIcon(R.drawable.delete_24px)
                .setContentTitle("Media Upload")
                .setContentText("Uploading...")
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .build())
    }

    companion object {
        //in future we will support different folders to observe all in one job i guess ?
        private const val jobId = Constants.mediaContentJobId

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
            Log.i("ContentJob", "JOB SCHEDULED!")
        }

        // Check whether this job is currently scheduled.
        fun isScheduled(context: Context): Boolean {
            val js = context.getSystemService(JobScheduler::class.java)
            val jobs = js.allPendingJobs
            for (i in jobs.indices) {
                if (jobs[i].id == jobId) {
                    Log.d("JobChecker", "Job is already scheduled ")
                    return true
                }
            }
            Log.d("JobChecker", "Job is NOT already scheduled ")
            return false
        }

        // Cancel this job, if currently scheduled.
        fun cancelJob(context: Context) {
            val js = context.getSystemService(JobScheduler::class.java)
            js.cancel(jobId)
        }
    }
}