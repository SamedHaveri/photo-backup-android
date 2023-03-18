package com.example.photobackup.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "uploaded_media", indices = [Index(value = [ "uri_path" ], unique = true)])
data class UploadedMedia(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    @ColumnInfo(name = "uri_path")
    val uriPath: String,
//    @ColumnInfo(name = "datetime_added")
//    val dateTimeAdded: Date
)