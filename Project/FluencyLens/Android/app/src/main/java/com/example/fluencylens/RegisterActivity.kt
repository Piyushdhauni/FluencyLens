package com.example.fluencylens

import android.app.Activity
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class RegisterActivity : Activity() {

    private val client = OkHttpClient()

    // Use the same laptop IP address as MainActivity.
    private val serverBase = "http://10.171.145.31:5000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val name = findViewById<EditText>(R.id.nameInput)
        val email = findViewById<EditText>(R.id.registerEmailInput)
        val password = findViewById<EditText>(R.id.registerPasswordInput)
        val confirmPassword = findViewById<EditText>(R.id.confirmPasswordInput)

        val signInLink = findViewById<TextView>(R.id.signInLink)
        val createAccountBtn = findViewById<TextView>(R.id.createAccountBtn)

        signInLink.setOnClickListener {
            finish()
        }

        createAccountBtn.setOnClickListener {

            val userName = name.text.toString().trim()
            val userEmail = email.text.toString().trim()
            val userPassword = password.text.toString()
            val confirm = confirmPassword.text.toString()

            when {
                userName.isEmpty() -> {
                    name.error = "Enter your name"
                }

                userEmail.isEmpty() -> {
                    email.error = "Enter your email"
                }

                !android.util.Patterns.EMAIL_ADDRESS.matcher(userEmail).matches() -> {
                    email.error = "Enter a valid email"
                }

                userPassword.length < 8 -> {
                    password.error = "Password must contain at least 8 characters"
                }

                userPassword != confirm -> {
                    confirmPassword.error = "Passwords do not match"
                }

                else -> {
                    createAccountBtn.isEnabled = false
                    createAccountBtn.text = "Creating account..."

                    val json = JSONObject().apply {
                        put("name", userName)
                        put("email", userEmail)
                        put("password", userPassword)
                    }

                    val body = json.toString().toRequestBody(
                        "application/json; charset=utf-8".toMediaType()
                    )

                    val request = Request.Builder()
                        .url("$serverBase/register")
                        .post(body)
                        .build()

                    client.newCall(request).enqueue(object : okhttp3.Callback {

                        override fun onFailure(
                            call: okhttp3.Call,
                            e: java.io.IOException
                        ) {
                            runOnUiThread {
                                createAccountBtn.isEnabled = true
                                createAccountBtn.text = "Create account"

                                Toast.makeText(
                                    this@RegisterActivity,
                                    "Connection failed: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        override fun onResponse(
                            call: okhttp3.Call,
                            response: okhttp3.Response
                        ) {
                            val responseText = response.body?.string() ?: ""
                            val responseJson = try {
                                JSONObject(responseText)
                            } catch (e: Exception) {
                                JSONObject()
                            }

                            val message = if (responseJson.has("message")) {
                                responseJson.optString("message")
                            } else {
                                responseJson.optString("error", responseText)
                            }

                            val finalMessage = "HTTP ${response.code}: $message"

                            runOnUiThread {
                                createAccountBtn.isEnabled = true
                                createAccountBtn.text = "Create account"

                                if (response.isSuccessful) {
                                    Toast.makeText(
                                        this@RegisterActivity,
                                        message,
                                        Toast.LENGTH_LONG
                                    ).show()

                                    finish()
                                } else {
                                    Toast.makeText(
                                        this@RegisterActivity,
                                        message,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }

                            response.close()
                        }
                    })
                }
            }
        }
    }
}
