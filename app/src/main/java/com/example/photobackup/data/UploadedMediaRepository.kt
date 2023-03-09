package com.example.photobackup.data

class UploadedMediaRepository(private val uploadedMedediaDao: UploadedMedediaDao) {
    fun isUriPathAlreadyAdded(path: String) = uploadedMedediaDao.isUriPathAlreadyAdded(path)
    suspend fun insertUploadedMedia(uploadedMedia: UploadedMedia) = uploadedMedediaDao.insertUploadedMedia(uploadedMedia)
}