package com.example.thermoguard

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class BotonActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_boton)

        val btnBack = findViewById<MaterialButton>(R.id.btnBack)
        val btnEncender = findViewById<MaterialButton>(R.id.btnEncender)

        btnBack.setOnClickListener { finish() }

        btnEncender.setOnClickListener {
            if (!MqttManager.isConnected()) {
                Toast.makeText(this, getString(R.string.error_not_connected), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            MqttManager.mqttPublish(Constants.TOPIC_COMANDO_ALARMA, Constants.MSG_ALARMA_ON, 1)
            Toast.makeText(this, getString(R.string.toast_alarma_encendida), Toast.LENGTH_SHORT).show()
        }
    }
}
