package com.example.thermoguard

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.button.MaterialButton

class SensorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }
}