package com.example.photobackup.ui.login

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.photobackup.R
import com.example.photobackup.other.Status
import com.example.photobackup.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDateTime

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginViewModel by viewModels<LoginViewModel>()
        val loginButton = findViewById<Button>(R.id.loginButton)
        val usernameText = findViewById<EditText>(R.id.etUsername)
        val passwordText = findViewById<EditText>(R.id.etPassword)

        val authDetails = loginViewModel.authDetails
        try {
            val expDate = LocalDateTime.parse(authDetails.tokenExpiration)
            val nowDate = LocalDateTime.now();
            if (expDate.isAfter(nowDate) && authDetails.token != "") {
                val intent =
                    Intent(this@LoginActivity, MainActivity::class.java).apply {}
                startActivity(intent)
            }
        }catch (_:java.lang.RuntimeException){

        }

        loginViewModel.res.observe(this, Observer {
            when (it.status) {
                Status.SUCCESS -> {
                    val intent =
                        Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                }
                Status.LOADING -> {
//                    Toast.makeText(this@LoginActivity, "Loading", Toast.LENGTH_SHORT).show()
                }
                Status.ERROR -> {
                    Toast.makeText(this@LoginActivity, it.message, Toast.LENGTH_SHORT).show()
                }
            }
        })

        loginButton.setOnClickListener {
            when {
                TextUtils.isEmpty(usernameText.text.toString().trim { it <= ' ' }) -> {
                    Toast.makeText(this@LoginActivity, "Enter username", Toast.LENGTH_SHORT).show()
                }
                TextUtils.isEmpty(passwordText.text.toString().trim { it <= ' ' }) -> {
                    Toast.makeText(this@LoginActivity, "Enter password", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val username: String = usernameText.text.toString().trim { it <= ' ' }
                    val password: String = passwordText.text.toString().trim { it <= ' ' }
                    loginViewModel.authenticate(username, password)
                }
            }
        }
    }
}