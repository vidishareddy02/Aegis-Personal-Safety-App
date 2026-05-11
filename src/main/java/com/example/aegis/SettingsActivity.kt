package com.example.aegis

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.widget.EditText
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Switch
import android.widget.TextView
import android.widget.Button
import android.os.Build

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("AegisPrefs", MODE_PRIVATE)

        val contact1 = prefs.getString("contact1", "")
        val contact2 = prefs.getString("contact2", "")

        val permissionBtn = findViewById<Button>(R.id.requestPermissionBtn)

        val locationStatus = findViewById<TextView>(R.id.locationStatus)
        val smsStatus = findViewById<TextView>(R.id.smsStatus)
        val notificationStatus = findViewById<TextView>(R.id.notificationStatus)
        val micStatus = findViewById<TextView>(R.id.micStatus)   // ✅ ADDED

        val notificationGranted =
            if (Build.VERSION.SDK_INT >= 33) {
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

        notificationStatus.text =
            if (notificationGranted) "Notifications: Enabled"
            else "Notifications: Not granted"

        val locationGranted = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val smsGranted = checkSelfPermission(android.Manifest.permission.SEND_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val micGranted = checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED   // ✅ ADDED

        locationStatus.text = if (locationGranted) "Location: Enabled" else "Location: Not granted"
        smsStatus.text = if (smsGranted) "SMS: Enabled" else "SMS: Not granted"
        micStatus.text = if (micGranted) "Mic: Enabled" else "Mic: Not granted"   // ✅ ADDED

        val monitorSwitch = findViewById<Switch>(R.id.monitorSwitch)
        val soundSwitch = findViewById<Switch>(R.id.soundSwitch)
        val vibrationSwitch = findViewById<Switch>(R.id.vibrationSwitch)

        if (!prefs.contains("soundEnabled")) {

            prefs.edit()
                .putBoolean("monitorEnabled", true)
                .putBoolean("soundEnabled", false)
                .putBoolean("vibrationEnabled", false)
                .apply()
        }

        monitorSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("monitoring", isChecked).apply()
        }

        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("sound", isChecked).apply()
        }

        vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("vibration", isChecked).apply()
        }

        monitorSwitch.isChecked = prefs.getBoolean("monitoring", false)
        soundSwitch.isChecked = prefs.getBoolean("sound", false)
        vibrationSwitch.isChecked = prefs.getBoolean("vibration", false)

        val sharedPref = getSharedPreferences("AegisPrefs", MODE_PRIVATE)

        val savedName = sharedPref.getString("username", "")
        val savedPhone = sharedPref.getString("phone", "")
        val savedContact1 = sharedPref.getString("contact1", "")
        val savedContact2 = sharedPref.getString("contact2", "")

        findViewById<EditText>(R.id.nameInput).setText(savedName)
        findViewById<EditText>(R.id.phoneInput).setText(savedPhone)
        findViewById<EditText>(R.id.phone1).setText(savedContact1)
        findViewById<EditText>(R.id.phone2).setText(savedContact2)

        findViewById<android.widget.Button>(R.id.saveBtn).setOnClickListener {

            val name = findViewById<EditText>(R.id.nameInput).text.toString()
            val phone = findViewById<EditText>(R.id.phoneInput).text.toString()
            val num1 = findViewById<EditText>(R.id.phone1).text.toString()
            val num2 = findViewById<EditText>(R.id.phone2).text.toString()

            val sharedPref = getSharedPreferences("AegisPrefs", MODE_PRIVATE)
            val editor = sharedPref.edit()

            editor.putString("username", name)
            editor.putString("phone", phone)
            editor.putString("contact1", num1)
            editor.putString("contact2", num2)
            editor.putBoolean("monitoring", monitorSwitch.isChecked)
            editor.putBoolean("sound", soundSwitch.isChecked)
            editor.putBoolean("vibration", vibrationSwitch.isChecked)

            editor.apply()

            val db = FirebaseFirestore.getInstance()

            val userData = hashMapOf(
                "name" to name,
                "phone" to phone,
                "contact1" to num1,
                "contact2" to num2
            )

            db.collection("users")
                .document("user1")
                .set(userData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Saved to Firebase", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Firebase failed", Toast.LENGTH_SHORT).show()
                }

            Toast.makeText(this, "Saved successfully", Toast.LENGTH_SHORT).show()

            if (monitorSwitch.isChecked) {
                val intent = Intent(this, ForegroundService::class.java)
                startService(intent)
            } else {
                val intent = Intent(this, ForegroundService::class.java)
                stopService(intent)
            }
        }

        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)

        bottomNav.selectedItemId = R.id.nav_settings

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

                R.id.nav_settings -> true

                else -> false
            }
        }

        permissionBtn.setOnClickListener {

            val permissions = mutableListOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.SEND_SMS,
                android.Manifest.permission.RECORD_AUDIO   // ✅ ADDED
            )

            if (Build.VERSION.SDK_INT >= 33) {
                permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }

            requestPermissions(permissions.toTypedArray(), 1)
        }
    }

    override fun onResume() {
        super.onResume()

        val locationStatus = findViewById<TextView>(R.id.locationStatus)
        val smsStatus = findViewById<TextView>(R.id.smsStatus)
        val notificationStatus = findViewById<TextView>(R.id.notificationStatus)
        val micStatus = findViewById<TextView>(R.id.micStatus)   // ✅ ADDED

        val notificationGranted =
            if (Build.VERSION.SDK_INT >= 33) {
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

        notificationStatus.text =
            if (notificationGranted) "Notifications: Enabled"
            else "Notifications: Not granted"

        val locationGranted =
            checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED

        val smsGranted =
            checkSelfPermission(android.Manifest.permission.SEND_SMS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED

        val micGranted =
            checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED   // ✅ ADDED

        locationStatus.text = if (locationGranted) "Location: Enabled" else "Location: Not granted"
        smsStatus.text = if (smsGranted) "SMS: Enabled" else "SMS: Not granted"
        micStatus.text = if (micGranted) "Mic: Enabled" else "Mic: Not granted"   // ✅ ADDED
    }
}