package com.example.aegis

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import android.os.Handler
import android.os.Looper
import android.widget.Toast

class EmergencyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency)
        val statusText = findViewById<TextView>(R.id.statusText)

        val success = intent.getBooleanExtra("status", true)

        if (!success) {
            statusText.text = "❌ No contacts found"
            statusText.setTextColor(resources.getColor(android.R.color.holo_red_dark))
        } else {
            statusText.text = "⏳ Sending SOS..."
            statusText.setTextColor(resources.getColor(android.R.color.holo_orange_dark))

            Handler(Looper.getMainLooper()).postDelayed({
                statusText.text = "✅ Alert sent successfully"
                statusText.setTextColor(resources.getColor(android.R.color.holo_green_dark))
            }, 2000)
        }
        val sosCircle = findViewById<View>(R.id.sosCircle)

        val pulse = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.scale_pulse)
        sosCircle.startAnimation(pulse)

        val cancelBtn = findViewById<Button>(R.id.cancelBtn)

        cancelBtn.setOnClickListener {

            sendSafeMessage()

            Toast.makeText(this, "SOS Cancelled. You are safe.", Toast.LENGTH_SHORT).show()

            finish()
        }

        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)

        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {

                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }

                R.id.nav_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                    true
                }

                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                    true
                }

                else -> false
            }
        }
    }

    fun sendSafeMessage() {

        val prefs = getSharedPreferences("AegisPrefs", MODE_PRIVATE)
        val contact1 = prefs.getString("contact1", "") ?: ""
        val contact2 = prefs.getString("contact2", "") ?: ""
        //val contacts = "9550965579"

        val message = "I am safe now. No emergency."

        val smsManager = android.telephony.SmsManager.getDefault()

        listOf(contact1, contact2).forEach { number ->
            if (number.isNotEmpty()) {
                try {
                    smsManager.sendTextMessage(number, null, message, null, null)
                    Toast.makeText(this, "Sent to $number", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Failed for $number", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}