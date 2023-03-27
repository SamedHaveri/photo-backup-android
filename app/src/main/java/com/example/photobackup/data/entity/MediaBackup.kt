package com.example.photobackup.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_backup")
data class MediaBackup(
    @ColumnInfo(name = "uri_id", )
    val uriId: Long,
    @ColumnInfo(name = "uri_path")
    val uriPath: String,
    @ColumnInfo(name = "absolute_path")
    val absolutePath: String,
    @ColumnInfo(name = "media_type")
    val mediaType: String,
    @ColumnInfo(name = "date_added")
    val dateAdded: Long,
    @ColumnInfo(name = "orientation")
    val orientation: Int,
    @ColumnInfo(name = "uploaded")
    var uploaded: Boolean,
    @ColumnInfo(name = "upload_tries")
    var uploadTries: Byte,
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
)
