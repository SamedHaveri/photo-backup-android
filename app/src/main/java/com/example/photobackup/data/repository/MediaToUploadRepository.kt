package com.example.photobackup.data.repository

import com.example.photobackup.data.dao.MediaToUploadDao
import com.example.photobackup.data.entity.MediaToUpload

class MediaToUploadRepository(private val mediaToUploadDao: MediaToUploadDao) {
    suspend fun insert(mediaToUpload: MediaToUpload) = mediaToUploadDao.insert(mediaToUpload)
    fun getMediaToUpload(mediaType: String) = mediaToUploadDao.get(mediaType)
    fun deleteMediaToUpload(id: Int) = mediaToUploadDao.delete(id)
    fun deleteMediaByUri(uriId: Int, mediaType: String) = mediaToUploadDao.deleteByUriId(uriId, mediaType)
}