package com.example.photobackup.service

data class UploadingMediaDto(
    val uriId: Int,
    val absolutePath: String,
    val uriPath: String
)