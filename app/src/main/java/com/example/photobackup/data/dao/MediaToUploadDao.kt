package com.example.photobackup.data.dao

import androidx.room.*
import com.example.photobackup.data.entity.MediaToUpload

@Dao
interface MediaToUploadDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(mediaToUpload: MediaToUpload)

    @Query("SELECT * FROM media_to_upload where media_type = :mediaType")
    fun get(mediaType: String) : List<MediaToUpload>

    @Query("DELETE FROM media_to_upload where id = :id")
    fun delete(id: Int)

    @Query("DELETE FROM media_to_upload WHERE uri_id = :uriId AND media_type = :mediaType")
    fun deleteByUriId(uriId: Int, mediaType: String)

}