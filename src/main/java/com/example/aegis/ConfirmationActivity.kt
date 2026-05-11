package com.example.aegis

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.view.animation.AnimationUtils
import android.content.Intent
import android.widget.Button

class ConfirmationActivity : AppCompatActivity() {

    private lateinit var countdownText: TextView
    private lateinit var btnSOSNow: Button
    private var timer: CountDownTimer? = null
    private var isSafe = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmation)

        checkAndRequestPermissions()

        window.decorView.alpha = 0f
        window.decorView.animate().alpha(1f).setDuration(300).start()

        countdownText = findViewById(R.id.countdownText)
        btnSOSNow = findViewById(R.id.btnSOSNow)

        val riskText = findViewById<TextView>(R.id.riskText)

        val batteryIntent = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val battery = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1

        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isNight = hour < 6 || hour > 18

        val risks = mutableListOf<String>()

        if (isNight) {
            risks.add("• Night time detected")
        } else {
            risks.add("• Day time")
        }

        if (battery <= 20) {
            risks.add("• Low battery")
        }

        riskText.text = risks.joinToString("\n")


        riskText.setTextColor(android.graphics.Color.RED)
        startCountdown()

        val root = findViewById<View>(android.R.id.content)

        root.setOnClickListener {
            isSafe = true
            timer?.cancel()
            Toast.makeText(this, "You're safe", Toast.LENGTH_SHORT).show()
            finish()
        }

        val pulse = AnimationUtils.loadAnimation(this, R.anim.scale_pulse)
        countdownText.startAnimation(pulse)

        btnSOSNow.setOnClickListener {

            isSafe = false
            timer?.cancel()

            Toast.makeText(this, "Sending SOS immediately!", Toast.LENGTH_SHORT).show()

            getLocation(this) { location ->

                val success = sendSOS(this, location)

                val intent = Intent(this, EmergencyActivity::class.java)
                intent.putExtra("status", success)

                startActivity(intent)
                finish()
            }
        }


    }

    private fun startCountdown() {
        timer = object : CountDownTimer(10000, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt()
                countdownText.text = seconds.toString()

                if (seconds <= 3) {
                    countdownText.setBackgroundResource(R.drawable.sos_circle_red)
                }
            }

            override fun onFinish() {
                if (!isSafe) {

                    AudioHelper.startRecording(this@ConfirmationActivity)

                    android.os.Handler().postDelayed({
                        AudioHelper.stopRecording()
                    }, 5000) // stops after 5 seconds

                    getLocation(this@ConfirmationActivity) { location ->

                        val success = sendSOS(this@ConfirmationActivity, location)

                        val intent = Intent(this@ConfirmationActivity, EmergencyActivity::class.java)
                        intent.putExtra("status", success)

                        startActivity(intent)
                        finish()
                    }
                }
            }
        }.start()
    }

    fun handleScreenTap(view: View) {
        isSafe = true
        timer?.cancel()
        Toast.makeText(this, "You're safe", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun checkAndRequestPermissions() {

        val permissions = arrayOf(
            android.Manifest.permission.SEND_SMS,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )

        val notGranted = permissions.filter {
            checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            requestPermissions(notGranted.toTypedArray(), 1)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1) {
            Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
        }
    }
}