package com.example.photobackup

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.photobackup.network.ApiClient
import com.example.photobackup.network.AuthenticationResponse
import com.example.photobackup.network.requestDTO.AuthenticationRequestDTO
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDateTime

class LoginActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val sharedPref = this@LoginActivity.getSharedPreferences(
            getString(R.string.prefs), Context.MODE_PRIVATE
        ) ?: return
        val token = sharedPref.getString(getString(R.string.token_key), null)
        if (token != null) {
            val expirationDate =
                sharedPref.getString(getString(R.string.token_expiration_key), null)
            val expDate = LocalDateTime.parse(expirationDate)
            val nowDate = LocalDateTime.now();
            if (expDate.isAfter(nowDate)) {
                val intent =
                    Intent(this@LoginActivity, MainActivity::class.java).apply {}
                startActivity(intent)
                finish()
            }
        }

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
                    val authData = AuthenticationRequestDTO(username, password)

                    val client = ApiClient.apiService.authenticate(authData)

                    client.enqueue(object : Callback<AuthenticationResponse> {
                        override fun onResponse(
                            call: Call<AuthenticationResponse>,
                            response: Response<AuthenticationResponse>
                        ) {
                            Log.d("SUCCESS", "SUCCESS ACCESSING DB")
                            if (response.code() == 200) {
                                with(sharedPref.edit()) {
                                    putString(
                                        getString(R.string.token_key),
                                        "Bearer " + response.body()!!.token
                                    )
                                    putString(
                                        getString(R.string.username_key),
                                        response.body()!!.username
                                    )
                                    putInt(getString(R.string.id_key), response.body()!!.id)
                                    putString(
                                        getString(R.string.token_expiration_key),
                                        response.body()!!.tokenExpiration
                                    )
                                    apply()
                                }
                                val intent =
                                    Intent(this@LoginActivity, MainActivity::class.java).apply {}
                                startActivity(intent)
                                finish()
                            }
                            else if (response.code() == 403) {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Wrong Username or Password", Toast.LENGTH_SHORT
                                ).show()
                            }
                            else if (response.code() == 500){
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Server Error", Toast.LENGTH_SHORT
                                ).show()
                            }
                            else {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Unknown Error", Toast.LENGTH_SHORT
                                ).show()
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