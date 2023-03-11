package com.example.photobackup.service


import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobInfo.TriggerContentUri
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.Constraints
import androidx.work.NetworkType
import com.example.photobackup.data.MediaDatabase
import com.example.photobackup.data.UploadedMedediaDao
import com.example.photobackup.data.UploadedMedia
import com.example.photobackup.data.UploadedMediaRepository
import com.example.photobackup.di.AppModule
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import javax.inject.Inject


/**
 * Example stub job to monitor when there is a change to photos in the media provider.
 */
@SuppressLint("SpecifyJobSchedulerIdRange")
class VideosContentJob : JobService() {
    var mRunningParams: JobParameters? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartJob(params: JobParameters): Boolean {
        mRunningParams = params
        val absolutePaths = ArrayList<String>()
        // Instead of real work, we are going to build a string to show to the user.
        val sb = StringBuilder()
        Log.d("Job", "Job started (video)")

        // Did we trigger due to a content change?
        if (params.triggeredContentAuthorities != null) {
            var rescanNeeded = false
            if (params.triggeredContentUris != null) {
                // If we have details about which URIs changed, then iterate through them
                // and collect either the ids that were impacted or note that a generic
                // change has happened.
                val ids = ArrayList<String>()
                for (uri in params.triggeredContentUris!!) {
                    //todo find how to not run this on main thread and take away the allowOnMainThread setting in getDatabase
                    val uploadedMediaRepository = UploadedMediaRepository(MediaDatabase.getDatabase(applicationContext).uploadedMediaDao())
                    if(uploadedMediaRepository.isUriPathAlreadyAdded(uri.path!!)){
                        Log.d("FUCK U", "Almost uploaded twice ?")
                        continue
                    }else{
                        runBlocking { launch {
                                val uploadedMedia = UploadedMedia(0, uri.path!!)
                                uploadedMediaRepository.insertUploadedMedia(uploadedMedia);
                            }
                        }
                    }
                    val path = uri.pathSegments
                    if (path != null && path.size == EXTERNAL_PATH_SEGMENTS.size + 1) {
                        // This is a specific file.
                        ids.add(path[path.size - 1])
                    } else {
                        // Oops, there is some general change!
                        rescanNeeded = true
                    }
                }
                if (ids.size > 0) {
                    // If we found some ids that changed, we want to determine what they are.
                    // First, we do a query with content provider to ask about all of them.
                    val selection = StringBuilder()
                    for (i in ids.indices) {
                        if (selection.isNotEmpty()) {
                            selection.append(" OR ")
                        }
                        selection.append(MediaStore.Video.VideoColumns._ID)
                        selection.append("='")
                        selection.append(ids[i])
                        selection.append("'")
                    }
                    // Now we iterate through the query, looking at the filenames of
                    // the items to determine if they are ones we are interested in.
                    var cursor: Cursor? = null
                    var haveFiles = false
                    try {
                        cursor = contentResolver.query(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            PROJECTION, selection.toString(), null, null)
                        while (cursor!!.moveToNext()) {
                            // We only care about files in the DCIM directory.
                            val dir = cursor.getString(PROJECTION_DATA)
                            Log.d("All changed files", dir)
                            if (dir.startsWith(DCIM_DIR)) {
                                if (!haveFiles) {
                                    haveFiles = true
                                }
                                Log.d("filesToUpload", dir)
                                absolutePaths.add(dir) //store all files for upload
                                sb.append(cursor.getInt(PROJECTION_ID))
                                sb.append(": ")
                                sb.append(dir)
                                sb.append("\n")
                            }
                        }
                    } catch (e: SecurityException) {
                        sb.append("Error: no access to media!")
                    } finally {
                        cursor?.close()
                    }
                }
            } else {
                // We don't have any details about URIs (because too many changed at once),
                // so just note that we need to do a full rescan.
                rescanNeeded = true
            }
            if (rescanNeeded) {
                sb.append("Video rescan needed!")
            }
        } else {
            sb.append("(No video content)")
        }

        //todo figure out dependency injection and implement here
        MediaUploadExecutor().executeUpload(absolutePaths, applicationContext)

        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        Log.d("VideoContent", "Job Stopped (video)")
        return false
    }

    companion object {
        //in future we will support different folders to observe all in one job i guess ?
        const val jobId = 546863626; //todo make this better than just placing it here ?

        // The root URI of the media provider, to monitor for generic changes to its content.
        val MEDIA_URI = Uri.parse("content://" + MediaStore.AUTHORITY + "/")

        // Path segments for image-specific URIs in the provider.
        val EXTERNAL_PATH_SEGMENTS = MediaStore.Video.Media.EXTERNAL_CONTENT_URI.pathSegments

        // The columns we want to retrieve about a particular image.
        val PROJECTION = arrayOf(
            MediaStore.Video.VideoColumns._ID, MediaStore.Video.VideoColumns.DATA
        )
        const val PROJECTION_ID = 0
        const val PROJECTION_DATA = 1

        // This is the external storage directory where cameras place pictures.
        val DCIM_DIR = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM).path

        // A pre-built JobInfo we use for scheduling our job.
        var JOB_INFO: JobInfo? = null

        init {
            val builder = JobInfo.Builder(jobId,
                ComponentName("com.example.photobackup.service", VideosContentJob::class.java.name))
            // Look for specific changes to images in the provider.
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
                ComponentName(context, VideosContentJob::class.java.name))
            // Look for specific changes to images in the provider.
            builder.addTriggerContentUri(TriggerContentUri(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS))
            // Also look for general reports of changes in the overall provider.
//            builder.addTriggerContentUri(TriggerContentUri(MEDIA_URI, 0))
            JOB_INFO = builder.setRequiredNetworkType(NetworkType.CONNECTED.ordinal).build()
            val js = context.getSystemService(JobScheduler::class.java)
            js.schedule(JOB_INFO!!)
            Log.i("VideoContent", "JOB SCHEDULED!")
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