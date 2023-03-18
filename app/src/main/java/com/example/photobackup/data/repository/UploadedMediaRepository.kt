package com.example.photobackup.data.repository

import com.example.photobackup.data.dao.UploadedMedediaDao
import com.example.photobackup.data.entity.UploadedMedia

class UploadedMediaRepository(private val uploadedMedediaDao: UploadedMedediaDao) {
    fun isUriPathAlreadyAdded(path: String) = uploadedMedediaDao.isUriPathAlreadyAdded(path)
    suspend fun insertUploadedMedia(uploadedMedia: UploadedMedia) = uploadedMedediaDao.insert(uploadedMedia)
}