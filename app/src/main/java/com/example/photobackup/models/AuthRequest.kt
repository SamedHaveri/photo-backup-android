package com.example.photobackup.models

import com.google.gson.annotations.SerializedName

class AuthRequest (
    @SerializedName("username")
    val username: String,
    @SerializedName("password")
    val password: String,
)

