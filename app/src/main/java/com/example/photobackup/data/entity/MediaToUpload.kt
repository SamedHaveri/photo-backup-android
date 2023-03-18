package com.example.photobackup.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "media_to_upload", indices = [Index(value = [ "uri_path" ], unique = true)])
data class MediaToUpload(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    @ColumnInfo(name = "uri_id", )
    val uriId: Int,
    @ColumnInfo(name = "uri_path")
    val uriPath: String,
    @ColumnInfo(name = "absolute_path")
    val absolutePath: String,
    @ColumnInfo(name = "media_type")
    val mediaType: String
)