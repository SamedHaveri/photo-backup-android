package com.example.photobackup.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UploadedMedediaDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUploadedMedia(uploadedMedia: UploadedMedia)

    @Query("SELECT CASE WHEN COUNT(m.id) > 0 THEN true ELSE false END FROM uploaded_media m WHERE m.uri_path = :path")
    fun isUriPathAlreadyAdded(path: String): Boolean
}