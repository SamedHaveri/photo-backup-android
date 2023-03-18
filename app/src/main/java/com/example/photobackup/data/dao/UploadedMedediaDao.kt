package com.example.photobackup.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.photobackup.data.entity.UploadedMedia

@Dao
interface UploadedMedediaDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(uploadedMedia: UploadedMedia)

    @Query("SELECT CASE WHEN COUNT(m.id) > 0 THEN true ELSE false END FROM uploaded_media m WHERE m.uri_path = :path")
    fun isUriPathAlreadyAdded(path: String): Boolean
}