package com.example.aegis

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.hardware.SensorManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.widget.Toast
import android.content.Context
import android.app.PendingIntent
import android.content.IntentFilter
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

@Suppress("UnspecifiedRegisterReceiverFlag")
class ForegroundService : Service() {

    private var isSOSSent = false
    private var highRiskCount = 0
    private lateinit var sensorManager: SensorManager
    private var isMoving = false
    var isAutoTriggered = false

    private var recorder: android.media.MediaRecorder? = null
    private var audioFilePath: String = ""

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val sensorListener = object : SensorEventListener {

            override fun onSensorChanged(event: SensorEvent) {

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val acceleration = Math.sqrt((x * x + y * y + z * z).toDouble())

                isMoving = acceleration > 17
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            sensorListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        val filter = IntentFilter("START_RECORDING")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.registerReceiver(
                recordingReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            this.registerReceiver(
                recordingReceiver,
                filter
            )
        }

    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "AegisChannel",
                "Aegis Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )

            channel.enableVibration(true)
            channel.enableLights(true)

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val notification = NotificationCompat.Builder(this, "AegisChannel")
            .setContentTitle("Aegis Protection Active")
            .setContentText("Monitoring your safety in background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        startForeground(1, notification)
        startRiskMonitoring()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun triggerSOS() {
        startAudioRecording()

        val prefs = getSharedPreferences("AegisPrefs", MODE_PRIVATE)

        val soundEnabled = prefs.getBoolean("sound", false)
        val vibrationEnabled = prefs.getBoolean("vibration", false)

        if (soundEnabled) {
            try {
                val mediaPlayer = android.media.MediaPlayer.create(this, R.raw.alert_sound)
                mediaPlayer?.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (vibrationEnabled) {
            try {
                val vibrator = getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = android.os.VibrationEffect.createOneShot(
                        1000,
                        android.os.VibrationEffect.DEFAULT_AMPLITUDE
                    )
                    vibrator.vibrate(effect)
                } else {
                    vibrator.vibrate(1000)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        getLocation(this) { location ->

            if (location != null) {
                sendSOS(this, location)
            } else {
                getSafeLocation(this) { fallbackLocation ->
                    sendSOS(this, fallbackLocation)
                }
            }
        }
    }

    fun getSafeLocation(context: Context, onResult: (String) -> Unit) {

        if (context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {

            onResult("Location permission not granted")
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        fusedLocationClient.lastLocation.addOnSuccessListener { location: android.location.Location? ->

            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude
                val mapsLink = "https://maps.google.com/?q=$lat,$lon"
                onResult(mapsLink)
            } else {
                onResult("Location unavailable. Please check immediately.")
            }

        }.addOnFailureListener {
            onResult("Location unavailable. Please check immediately.")
        }
    }

    fun startAudioRecording() {
        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
        try {
            audioFilePath = "${externalCacheDir?.absolutePath}/evidence_${System.currentTimeMillis()}.3gp"

            recorder = android.media.MediaRecorder().apply {
                setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                setOutputFormat(android.media.MediaRecorder.OutputFormat.THREE_GPP)
                setOutputFile(audioFilePath)
                setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AMR_NB)
                prepare()
                start()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun stopAudioRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startRiskMonitoring() {

        val handler = android.os.Handler(android.os.Looper.getMainLooper())

        handler.postDelayed(object : Runnable {
            override fun run() {

                val hour = java.util.Calendar.getInstance()
                    .get(java.util.Calendar.HOUR_OF_DAY)

                val batteryIntent = registerReceiver(
                    null,
                    android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
                )

                val battery = batteryIntent?.getIntExtra(
                    android.os.BatteryManager.EXTRA_LEVEL, -1
                ) ?: -1

                getLocation(this@ForegroundService) { location ->

                    var isUnsafeLocation = false
                    //var isUnsafeLocation = true

                    if (location.contains("unavailable", ignoreCase = true)) {
                        isUnsafeLocation = true
                    }

                    if (location.isEmpty()) {
                        isUnsafeLocation = true
                    }

                    val risk = RiskManager().calculateRisk(
                        hour,
                        battery,
                        this@ForegroundService.isMoving,
                        isUnsafeLocation
                    )
                    //val risk = 10

                    if (risk >= 8 && !isAutoTriggered) {
                        isAutoTriggered = true

                        val intent = Intent(this@ForegroundService, ConfirmationActivity::class.java)

                        val pendingIntent = PendingIntent.getActivity(
                            this@ForegroundService,
                            0,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        val notification = NotificationCompat.Builder(this@ForegroundService, "AegisChannel")
                            .setContentTitle("🚨 High Risk Detected")
                            .setContentText("Tap to confirm your safety")
                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .build()

                        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        manager.notify(999, notification)

                        android.os.Handler(mainLooper).postDelayed({
                            isAutoTriggered = false
                        }, 30000)
                    }

                    handler.postDelayed(this, 5000)
                }
            }
        }, 5000)
    }
    private val recordingReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Toast.makeText(this@ForegroundService, "Receiver triggered", Toast.LENGTH_SHORT).show()
            if (intent?.action == "START_RECORDING") {
                startAudioRecording()
            }
        }
    }
}