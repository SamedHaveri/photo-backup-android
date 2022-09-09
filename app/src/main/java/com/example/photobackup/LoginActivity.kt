package com.example.photobackup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.photobackup.network.ApiClient
import com.example.photobackup.network.AuthenticationResponse
import com.example.photobackup.network.bodyModel.AuthenticationBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginButton = findViewById<Button>(R.id.loginButton)
        val usernameText = findViewById<EditText>(R.id.etUsername)
        val passwordText = findViewById<EditText>(R.id.etPassword)

        loginButton.setOnClickListener {
            when {
                TextUtils.isEmpty(usernameText.text.toString().trim { it <= ' ' }) -> {
                    Toast.makeText(this@LoginActivity, "Enter username", Toast.LENGTH_SHORT).show()
                }
                TextUtils.isEmpty(passwordText.text.toString().trim { it <= ' ' }) -> {
                    Toast.makeText(this@LoginActivity, "Enter password", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val username = usernameText.text.toString().trim { it <= ' ' }
                    val password = passwordText.text.toString().trim { it <= ' ' }

                    Log.d("CUSTOM_INFO", "username: " + username)
                    Log.d("CUSTOM_INFO", "password: " + password)

                    val authData = AuthenticationBody(username, password)

                    val client = ApiClient.apiService.authenticate(authData)

                    client.enqueue(object : Callback<AuthenticationResponse> {
                        override fun onResponse(
                            call: Call<AuthenticationResponse>,
                            response: Response<AuthenticationResponse>
                        ) {
                            Log.d("SUCCESS", "SUCCESS ACCESSING DB")
                            if (response.code() != 200) {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Wrong Username or Password", Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                val sharedPref = this@LoginActivity.getSharedPreferences(
                                    getString(R.string.prefs), Context.MODE_PRIVATE
                                ) ?: return
                                with(sharedPref.edit()) {
                                    putString(
                                        getString(R.string.token_key),
                                        response.body()!!.token
                                    )
                                    putString(
                                        getString(R.string.username_key),
                                        response.body()!!.username
                                    )
                                    putInt(getString(R.string.id_key), response.body()!!.id)
                                    putString(
                                        getString(R.string.token_expiration_key),
                                        response.body()!!.tokenExpiration.toString()
                                    )
                                    apply()
                                }
                                val intent =
                                    Intent(this@LoginActivity, MainActivity::class.java).apply {}
                                startActivity(intent)
                            }
                        }
                        override fun onFailure(call: Call<AuthenticationResponse>, t: Throwable) {
                            Toast.makeText(
                                this@LoginActivity,
                                "Error API Connection", Toast.LENGTH_SHORT
                            ).show()
                            Log.d("ERROR", "ERROR GETTING AUTH DATA")
                            Log.d("ERROR", t.message.toString());
                        }

                    })

                }
            }
        }
    }
}