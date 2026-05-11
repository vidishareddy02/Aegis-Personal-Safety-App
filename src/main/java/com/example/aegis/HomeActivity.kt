package com.example.aegis

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.Toast
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import com.google.android.gms.location.*
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    var isAutoTriggered = false
    val handler = Handler(Looper.getMainLooper())

    private var isMoving = false
    private var lastTriggerTime = 0L
    private var volumePressCount = 0
    private var lastPressTime = 0L

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val safeStatus = findViewById<TextView>(R.id.safeStatus)

        safeStatus.setOnClickListener {
            Toast.makeText(this, "Simulating Risk 🚨", Toast.LENGTH_SHORT).show()

            startActivity(Intent(this, ConfirmationActivity::class.java))
        }

        val intent = Intent(this, ForegroundService::class.java)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        val batteryText = findViewById<TextView>(R.id.batteryStatus)
        val locationText = findViewById<TextView>(R.id.locationStatus)
        val movementText = findViewById<TextView>(R.id.movementStatus)

        // Battery
        val batteryIntent = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val battery = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        batteryText.text = "$battery%"

        // Location setup
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000
        ).build()

        //  Real-time location updates
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return

                val speed = location.speed

                if (speed < 1.5) {
                    movementText.text = "Still"
                    isMoving = false
                } else {
                    movementText.text = "Walking"
                    isMoving = true
                }

                //  Convert to city
                val geocoder = android.location.Geocoder(this@HomeActivity)

                try {
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                    if (!addresses.isNullOrEmpty()) {
                        val city = addresses[0].locality ?: addresses[0].subAdminArea
                        val country = addresses[0].countryName

                        locationText.text = "$city, $country"
                    } else {
                        locationText.text = "Location unknown"
                    }

                } catch (e: Exception) {
                    locationText.text = "Location error"
                }

                //  Risk calculation
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val risk = RiskManager().calculateRisk(hour, battery, isMoving, false)

                if (risk >= 10 && !isAutoTriggered) {
                    isAutoTriggered = true
                    startActivity(Intent(this@HomeActivity, ConfirmationActivity::class.java))
                }
            }
        }

        // Start location updates
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } else {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
        }

        //startRiskMonitoring()

        // NAVBAR (FIXED)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {

                R.id.nav_home -> true

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

        val sosButton = findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.sosButton)

        var isHolding = false

        sosButton.setOnTouchListener { _, event ->

            when (event.action) {

                android.view.MotionEvent.ACTION_DOWN -> {
                    isHolding = true

                    // Change to red
                    sosButton.setBackgroundResource(R.drawable.sos_circle_red)

                    // Scale animation
                    sosButton.animate()
                        .scaleX(1.1f)
                        .scaleY(1.1f)
                        .setDuration(150)
                        .start()

                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isHolding) {
                            triggerSOS()
                        }
                    }, 2000) // hold for 2 sec
                }

                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    isHolding = false

                    // 🔄 Reset back
                    sosButton.setBackgroundResource(R.drawable.sos_circle)

                    sosButton.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .start()
                }
            }

            true
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        isAutoTriggered = false
    }

    fun startRiskMonitoring() {
        handler.postDelayed(object : Runnable {
            override fun run() {


                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                //val hour = 23
                val batteryIntent = registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
                val battery = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
                //val battery = 10
                //isMoving=true
                val riskManager = RiskManager()
                val risk = riskManager.calculateRisk(hour, battery, isMoving, false)

                val currentTime = System.currentTimeMillis()
                Toast.makeText(this@HomeActivity, "Risk = $risk", Toast.LENGTH_SHORT).show()

                if (risk >= 10 && !isAutoTriggered && isMoving &&
                    currentTime - lastTriggerTime > 30000) {

                    isAutoTriggered = true
                    lastTriggerTime = currentTime

                    val intent = Intent(this@HomeActivity, ConfirmationActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }

                handler.postDelayed(this, 5000)
            }
        }, 5000)
    }

    fun triggerSOS() {
        Toast.makeText(this, "SOS ACTIVATED 🚨", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, ConfirmationActivity::class.java))
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {

        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP) {

            val currentTime = System.currentTimeMillis()

            if (currentTime - lastPressTime < 2000) {
                volumePressCount++
            } else {
                volumePressCount = 1
            }

            lastPressTime = currentTime

            if (volumePressCount >= 3) {
                volumePressCount = 0
                triggerSOSFromVolume()
            }

            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun triggerSOSFromVolume() {
        Toast.makeText(this, "Emergency Triggered!", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, ConfirmationActivity::class.java)
        startActivity(intent)
    }
}