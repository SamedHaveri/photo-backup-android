package com.example.photobackup.service

//class MediaUploadWorker(
//    private val context: Context,
//    private val workerParams: WorkerParameters,
//) : CoroutineWorker(context, workerParams) {
//
//    override suspend fun doWork(): Result {
//        Log.d("uploadWorker", "started Worker")
//        val mediaBackupRepository = MediaBackupRepository(MediaDatabase.getDatabase(applicationContext).mediaBackup())
//        MediaUploadUtil.syncDatabase(mediaBackupRepository, applicationContext.contentResolver)
//        MediaUploadUtil.uploadMedias(applicationContext)
//        startForegroundService()
//        return Result.success();
//    }
//
//    private suspend fun startForegroundService() {
//        val channel = NotificationChannel(
//            "media_upload",
//            "Media Upload",
//            NotificationManager.IMPORTANCE_HIGH,
//        )
//        val notificationManager = context.getSystemService(NotificationManager::class.java)
//        notificationManager.createNotificationChannel(channel)
//        setForeground(
//            ForegroundInfo(
//                Random.nextInt(),
//                NotificationCompat.Builder(context, "media_upload")
//                    .setSmallIcon(R.drawable.delete_24px)
//                    .setContentTitle("Media Upload")
//                    .setContentText("Uploading...")
//                    .setOnlyAlertOnce(true)
//                    .setOngoing(true)
//                    .build()
//            )
//        )
//    }
//
//}