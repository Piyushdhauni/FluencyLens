package com.example.fluencylens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LoginActivity : Activity() {

    private val client = OkHttpClient()

    // Use the same Flask server IP and port as MainActivity.kt
    private val serverBase = "http://10.171.145.31:5000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginBtn = findViewById<TextView>(R.id.loginBtn)

        findViewById<TextView>(R.id.registerLink).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        findViewById<TextView>(R.id.forgotPassword).setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        loginBtn.setOnClickListener {

            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginBtn.isEnabled = false
            loginBtn.text = "Signing in..."

            val json = JSONObject().apply {
                put("email", email)
                put("password", password)
            }

            val body = json.toString().toRequestBody(
                "application/json; charset=utf-8".toMediaType()
            )

            val request = Request.Builder()
                .url("$serverBase/login")
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        loginBtn.isEnabled = true
                        loginBtn.text = "Sign in"
                        Toast.makeText(
                            this@LoginActivity,
                            "Cannot connect to server: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseText = response.body?.string().orEmpty()
                    val success = response.isSuccessful

                    val responseJson = try {
                        JSONObject(responseText)
                    } catch (e: Exception) {
                        JSONObject()
                    }

                    val message = responseJson.optString(
                        if (success) "message" else "error",
                        "Unknown server response"
                    )

                    response.close()

                    runOnUiThread {
                        loginBtn.isEnabled = true
                        loginBtn.text = "Sign in"

                        if (success) {
                            Toast.makeText(
                                this@LoginActivity,
                                message,
                                Toast.LENGTH_SHORT
                            ).show()

                            val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                            intent.putExtra("USER_NAME", responseJson.optString("name"))
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            })
        }
    }
}
