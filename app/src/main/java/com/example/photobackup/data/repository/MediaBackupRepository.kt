package com.example.photobackup.data.repository

import com.example.photobackup.data.dao.MediaBackupDao
import com.example.photobackup.data.entity.MediaBackup

class MediaBackupRepository(private val mediaBackupDao: MediaBackupDao) {
    fun getByMediaTypeAndUriId(mediaType: String, uriId: Int) =
        mediaBackupDao.getByMediaTypeAndUriId(mediaType, uriId)

    fun get(mediaType: String) = mediaBackupDao.get(mediaType)

    fun getLatestSyncedMedia() = mediaBackupDao.getLatestSyncedMedia()

    fun getMediaToUpload() = mediaBackupDao.getMediaTuUpload()

    fun existsMediaToUpload() = mediaBackupDao.existsMediaToUpload()

    fun isTableEmpty() = mediaBackupDao.isTableEmpty()

    fun deleteMediaByUri(uriId: Int, mediaType: String) =
        mediaBackupDao.deleteByUriId(uriId, mediaType)

    fun deleteMediaToUpload(id: Long) = mediaBackupDao.delete(id)

    fun insert(mediaBackup: MediaBackup) = mediaBackupDao.insert(mediaBackup)

    fun insertAll(mediaBackup: List<MediaBackup>) = mediaBackupDao.insertAll(mediaBackup)

    fun setMediaAsUploaded(id: Long) {
        val mediaToUpdate = mediaBackupDao.getById(id)
        mediaToUpdate.uploaded = true
        mediaBackupDao.update(mediaToUpdate)
    }

    fun setMediaAsTriedButFailed(id: Long) {
        val mediaToUpdate = mediaBackupDao.getById(id)
        mediaToUpdate.uploaded = false
        mediaToUpdate.uploadTries = (mediaToUpdate.uploadTries + 1).toByte()
        mediaBackupDao.update(mediaToUpdate)
    }
}