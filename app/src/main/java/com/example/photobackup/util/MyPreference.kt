package com.example.photobackup.util

import android.content.Context
import com.example.photobackup.R
import com.example.photobackup.models.auth.AuthDetails
import com.example.photobackup.models.auth.AuthResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MyPreference @Inject constructor(@ApplicationContext context : Context){
    private val prefs = context.getSharedPreferences(R.string.prefs.toString(),0)

    fun getStoredToken(): String {
        return prefs.getString(R.string.token_key.toString(), null)!!
    }
    fun setStoredToken(query: String) {
        prefs.edit().putString(R.string.token_key.toString(), query).apply()
    }

    fun setStoredAuthDetails(id:String, username:String, token:String, expDate:String){
        prefs.edit()
            .putString(R.string.token_key.toString(), token)
            .putString(R.string.username_key.toString(), username)
            .putInt(R.string.id_key.toString(), id.toInt())
            .putString(R.string.token_expiration_key.toString(), expDate).apply()
    }

    fun getStoredAuthDetails(): AuthDetails {
        val token = prefs.getString(R.string.token_key.toString(), "")
        val username = prefs.getString(R.string.username_key.toString(), "")
        val id = prefs.getInt(R.string.id_key.toString(), 0)
        val expDate = prefs.getString(R.string.token_expiration_key.toString(), "")
        return AuthDetails(token, id, username, expDate)
    }

    fun getStoredString(key:String) {
        prefs.getString(key, null)!!
    }

    fun setStoredString(key:String, value:String) {
        prefs.edit().putString(key, value).apply()
    }
}