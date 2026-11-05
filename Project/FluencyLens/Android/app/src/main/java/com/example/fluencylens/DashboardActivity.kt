package com.example.fluencylens

import android.view.View
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView

class DashboardActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val userName = intent.getStringExtra("USER_NAME") ?: "User"

        findViewById<TextView>(R.id.dashboardName).text =
            "Welcome back, $userName!"


        findViewById<View>(R.id.navHome).setOnClickListener {
            // Already on dashboard
        }

        findViewById<View>(R.id.navPractice).setOnClickListener {
            val practiceIntent = Intent(this, MainActivity::class.java)
            practiceIntent.putExtra("USER_NAME", userName)
            startActivity(practiceIntent)
        }

        findViewById<View>(R.id.navProgress).setOnClickListener {
            // Progress screen will be implemented next.
        }

        findViewById<View>(R.id.navAchievements).setOnClickListener {
            // Achievements screen will be implemented later.
        }

        findViewById<View>(R.id.navProfile).setOnClickListener {
            // Profile screen will be implemented later.
        }


        findViewById<TextView>(R.id.practiceCard).setOnClickListener {
            val practiceIntent = Intent(this, MainActivity::class.java)
            practiceIntent.putExtra("USER_NAME", userName)
            startActivity(practiceIntent)
        }

        findViewById<TextView>(R.id.interviewCard).setOnClickListener {
            // Interview preparation screen will be added next.
        }

        findViewById<TextView>(R.id.progressCard).setOnClickListener {
            // Progress screen will be added next.
        }
    }
}
