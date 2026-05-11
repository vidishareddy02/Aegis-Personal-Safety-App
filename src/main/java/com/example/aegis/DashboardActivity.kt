package com.example.aegis

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.location.Geocoder
import java.util.Locale
import com.google.android.gms.location.LocationServices

class DashboardActivity : AppCompatActivity() {

    private var lastLocationTime: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val shareBtn = findViewById<Button>(R.id.shareBtn)
        val sosBtn = findViewById<Button>(R.id.sosBtn)

        // TextViews
        val batteryText = findViewById<TextView>(R.id.batteryText)
        val movementText = findViewById<TextView>(R.id.movementText)
        val riskText = findViewById<TextView>(R.id.riskText)
        val locationText = findViewById<TextView>(R.id.locationText)
        val updatedTimeText = findViewById<TextView>(R.id.updatedTimeText)

        //  BATTERY
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val battery = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        batteryText.text = "Battery: $battery%"

        //LOCATION
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            == android.content.pm.PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {

                    lastLocationTime = System.currentTimeMillis()

                    val geocoder = Geocoder(this, Locale.getDefault())

                    try {
                        val addresses = geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            1
                        )

                        if (!addresses.isNullOrEmpty()) {
                            val city = addresses[0].locality ?: "Unknown"
                            val country = addresses[0].countryName ?: ""
                            locationText.text = "$city, $country"
                        } else {
                            locationText.text = "Location unknown"
                        }

                    } catch (e: Exception) {
                        locationText.text = "Location error"
                    }

                    //  Updated time
                    updatedTimeText.text = "Updated just now"

                    // update every minute
                    updateTimeAgo(updatedTimeText)

                } else {
                    locationText.text = "Location not available"
                    updatedTimeText.text = ""
                }
            }

        } else {
            locationText.text = "Permission not granted"
        }

        //  MOVEMENT
        val movement = "Normal"
        movementText.text = "Movement: $movement"

        //  TIME + RISK
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isNight = hour < 6 || hour > 18

        val risks = mutableListOf<String>()

        if (isNight) risks.add("• Night time detected")
        if (battery in 0..20) risks.add("• Low battery")

        if (risks.isEmpty()) {
            riskText.text = "No major risks"
        } else {
            riskText.text = risks.joinToString("\n")
        }
        if (risks.isEmpty()) {
            riskText.setTextColor(android.graphics.Color.parseColor("#2E7D32")) // green
        } else {
            riskText.setTextColor(android.graphics.Color.parseColor("#C62828")) // red
        }



        shareBtn.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)

            it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).duration = 100
            }

            Toast.makeText(this, "Sharing your live location 📍", Toast.LENGTH_SHORT).show()
            shareLiveLocation(this)
        }

        sosBtn.setOnClickListener {
            val intent = Intent(this, ConfirmationActivity::class.java)
            startActivity(intent)
        }

        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)

        bottomNav.selectedItemId = R.id.nav_dashboard

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {

                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }

                R.id.nav_dashboard -> true

                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                    true
                }

                else -> false
            }
        }
    }

    //  Updates "Updated X min ago"
    private fun updateTimeAgo(textView: TextView) {
        val handler = android.os.Handler(mainLooper)

        handler.postDelayed(object : Runnable {
            override fun run() {
                val diff = (System.currentTimeMillis() - lastLocationTime) / 60000

                if (diff <= 0) {
                    textView.text = "Updated just now"
                } else {
                    textView.text = "Updated $diff min ago"
                }

                handler.postDelayed(this, 60000) // every 1 min
            }
        }, 60000)
    }
}