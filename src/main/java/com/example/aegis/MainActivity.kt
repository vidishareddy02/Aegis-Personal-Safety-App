package com.example.aegis

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("AegisPrefs", MODE_PRIVATE)
        val name = prefs.getString("username", null)

        if (name != null) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val nameInput = findViewById<EditText>(R.id.nameInput)
        val phoneInput = findViewById<EditText>(R.id.phoneInput)
        val contact1Input = findViewById<EditText>(R.id.contact1Input)
        val contact2Input = findViewById<EditText>(R.id.contact2Input)
        val continueBtn = findViewById<Button>(R.id.continueBtn)


        continueBtn.setOnClickListener {

            //  Get input values
            val name = findViewById<EditText>(R.id.nameInput).text.toString()
            val phone = findViewById<EditText>(R.id.phoneInput).text.toString()
            val contact1 = findViewById<EditText>(R.id.contact1Input).text.toString()
            val contact2 = findViewById<EditText>(R.id.contact2Input).text.toString()

            //  Validate
            if (name.isEmpty() || phone.isEmpty() || contact1.isEmpty() || contact2.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // SAVE DATA
            val sharedPref = getSharedPreferences("AegisPrefs", MODE_PRIVATE)
            val editor = sharedPref.edit()

            editor.putString("username", name)
            editor.putString("phone", phone)
            editor.putString("contact1", contact1)
            editor.putString("contact2", contact2)
            editor.apply()

            // NOW check permissions
            val locationGranted = checkSelfPermission(
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            val smsGranted = checkSelfPermission(
                android.Manifest.permission.SEND_SMS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!locationGranted || !smsGranted) {
                Toast.makeText(this, "Please grant permissions first", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, SettingsActivity::class.java))
                return@setOnClickListener
            }

            // Go to home
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}