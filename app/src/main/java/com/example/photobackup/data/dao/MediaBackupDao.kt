package com.example.photobackup.data.dao

import androidx.room.*
import com.example.photobackup.data.entity.MediaBackup

@Dao
interface MediaBackupDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(mediaBackup: MediaBackup)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(mediaBackup: List<MediaBackup>)

    @Update
    fun update(mediaBackup: MediaBackup)

    @Query("SELECT * FROM media_backup WHERE media_type = :mediaType")
    fun get(mediaType: String) : List<MediaBackup>

    @Query("SELECT * FROM media_backup WHERE id = :id")
    fun getById(id: Long) : MediaBackup

    @Query("SELECT * FROM media_backup WHERE media_type = :mediaType AND uri_id = :uriId")
    fun getByMediaTypeAndUriId(mediaType: String, uriId: Int) : MediaBackup

    @Query("SELECT * FROM media_backup ORDER BY date_added DESC LIMIT 1")
    fun getLatestSyncedMedia() : MediaBackup

    @Query("SELECT * FROM media_backup WHERE uploaded = 0 AND upload_tries < 3")
    fun getMediaTuUpload() : List<MediaBackup>

    @Query("DELETE FROM media_backup WHERE id = :id")
    fun delete(id: Long)

    @Query("SELECT CASE WHEN COUNT(m.id) = 0 THEN true ELSE false END FROM media_backup m")
    fun isTableEmpty() : Boolean

    @Query("DELETE FROM media_backup WHERE uri_id = :uriId AND media_type = :mediaType")
    fun deleteByUriId(uriId: Int, mediaType: String)

}