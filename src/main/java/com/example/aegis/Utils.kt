package com.example.aegis

import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast
import android.content.Context
import android.telephony.SmsManager

fun getLocation(context: Context, callback: (String) -> Unit) {

    val fusedLocationClient =
        com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)

    if (context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
        == android.content.pm.PackageManager.PERMISSION_GRANTED
    ) {

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->

            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude

                val link = "https://maps.google.com/?q=$lat,$lon"
                callback(link)
            } else {
                callback("Location unavailable (turn on GPS)")
            }
        }

    } else {
        callback("Permission not granted")
    }
}

fun getContacts(context: Context): List<String> {
    val sharedPref = context.getSharedPreferences("AegisPrefs", Context.MODE_PRIVATE)

    val c1 = sharedPref.getString("contact1", "") ?: ""
    val c2 = sharedPref.getString("contact2", "") ?: ""

    return listOf(c1, c2).filter { it.isNotEmpty() }
}

fun sendSOS(context: Context, location: String): Boolean {

    val contacts = getContacts(context)

    if (contacts.isEmpty()) {
        Toast.makeText(context, "No emergency contacts found", Toast.LENGTH_LONG).show()
        return false
    }

    val prefs = context.getSharedPreferences("AegisPrefs", Context.MODE_PRIVATE)
    val userName = prefs.getString("username", "Someone")

    val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    val message = "$userName needs help!\nTime: $currentTime\nLocation: $location"

    return try {
        val smsManager = SmsManager.getDefault()

        for (phoneNumber in contacts) {
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
        }

        Toast.makeText(context, "SOS Sent", Toast.LENGTH_SHORT).show()
        true

    } catch (e: Exception) {
        Toast.makeText(context, "SMS failed: ${e.message}", Toast.LENGTH_LONG).show()
        false
    }
}

fun shareLiveLocation(context: Context) {

    getLocation(context) { location ->

        val contacts = getContacts(context)

        if (contacts.isEmpty()) {
            Toast.makeText(context, "No contacts found", Toast.LENGTH_SHORT).show()
            return@getLocation
        }

        val message = "Live Location:\n$location"

        try {
            val smsManager = SmsManager.getDefault()

            for (phoneNumber in contacts) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            }

            Toast.makeText(context, "Location shared", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share location", Toast.LENGTH_LONG).show()
        }
    }
}
fun saveContactsToFirebase(context: Context, contact1: String, contact2: String, username: String) {

    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    val data = hashMapOf(
        "contact1" to contact1,
        "contact2" to contact2,
        "username" to username
    )

    db.collection("users")
        .document("user1")
        .set(data)
}
fun loadContactsFromFirebase(context: Context) {

    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    db.collection("users")
        .document("user1")
        .get()
        .addOnSuccessListener { document ->

            if (document != null) {

                val contact1 = document.getString("contact1") ?: ""
                val contact2 = document.getString("contact2") ?: ""
                val username = document.getString("username") ?: ""

                val sharedPref = context.getSharedPreferences("AegisPrefs", Context.MODE_PRIVATE)

                sharedPref.edit()
                    .putString("contact1", contact1)
                    .putString("contact2", contact2)
                    .putString("username", username)
                    .apply()
            }
        }
}
fun saveAlertToFirebase(location: String, success: Boolean, type: String) {

    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    val data = hashMapOf(
        "time" to System.currentTimeMillis(),
        "location" to location,
        "status" to if (success) "sent" else "failed",
        "triggerType" to type
    )

    db.collection("alerts")
        .add(data)
}
