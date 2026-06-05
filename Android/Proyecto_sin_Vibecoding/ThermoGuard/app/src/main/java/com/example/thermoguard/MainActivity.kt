package com.example.thermoguard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnIrTermometro).setOnClickListener {
            startActivity(Intent(this, ThermometerActivity::class.java))
        }

        findViewById<Button>(R.id.btnIrActivity2).setOnClickListener {
            startActivity(Intent(this, SensorActivity::class.java))
        }
    }
}
