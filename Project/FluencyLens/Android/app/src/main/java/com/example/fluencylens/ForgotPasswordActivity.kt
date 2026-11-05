package com.example.fluencylens

import android.app.Activity
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.util.Patterns

class ForgotPasswordActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val email = findViewById<EditText>(R.id.recoveryEmailInput)

        findViewById<TextView>(R.id.backToLogin).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.resetPasswordBtn).setOnClickListener {

            val userEmail = email.text.toString().trim()

            if (userEmail.isEmpty()) {
                email.error = "Enter your email"
            } else if (!Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
                email.error = "Enter a valid email"
            } else {
                Toast.makeText(
                    this,
                    "Password recovery backend is not connected yet.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
