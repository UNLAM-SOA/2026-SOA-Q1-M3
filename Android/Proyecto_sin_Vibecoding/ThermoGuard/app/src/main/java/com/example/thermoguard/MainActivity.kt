package com.example.thermoguard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var etBrokerUrl: TextInputEditText
    private lateinit var etPort: TextInputEditText
    private lateinit var btnConectar: Button
    private lateinit var btnIrTermometro: Button
    private lateinit var btnIrActivity2: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etBrokerUrl     = findViewById(R.id.etBrokerUrl)
        etPort          = findViewById(R.id.etPort)
        btnConectar     = findViewById(R.id.btnConectar)
        btnIrTermometro = findViewById(R.id.btnIrTermometro)
        btnIrActivity2  = findViewById(R.id.btnIrActivity2)

        actualizarBotonConexion()

        // ── CONECTAR / DESCONECTAR ──────────────────────────────────
        btnConectar.setOnClickListener {

            if (MqttManager.isConnected()) {
                MqttManager.mqttDisconnect()
                // pequeño delay visual; el estado real se refleja en onResume / reintento
                btnConectar.postDelayed({ actualizarBotonConexion() }, 300)
                Toast.makeText(this, "Desconectando...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val broker = etBrokerUrl.text.toString().trim()
            val port = etPort.text.toString().trim().toIntOrNull()

            if (broker.isEmpty()) {
                etBrokerUrl.error = "Ingresá la URL del broker"
                return@setOnClickListener
            }
            if (port == null) {
                etPort.error = "Puerto inválido"
                return@setOnClickListener
            }

            btnConectar.isEnabled = false
            btnConectar.text = "Conectando..."

            // Listener de mensajes entrantes (del ESP32)
            MqttManager.onMessage = { _, _ ->
                runOnUiThread {
                    // En el futuro: actualizar UI con la temperatura/estado
                }
            }

            // Conexión ANÓNIMA (corre en Thread interno del Manager)
            MqttManager.mqttConnect(
                brokerAddr = broker,
                port = port,
                onConnected = {
                    runOnUiThread {
                        actualizarBotonConexion()
                        Toast.makeText(this, "✅ Conectado a $broker", Toast.LENGTH_SHORT).show()

                        MqttManager.mqttSubscribe("thermoguard/sensor/temperatura", 0)
                        MqttManager.mqttSubscribe("thermoguard/sensor/estado", 1)
                    }
                },
                onError = { errorMsg ->
                    runOnUiThread {
                        actualizarBotonConexion()
                        Toast.makeText(this, "❌ Error: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        // ── NAVEGACIÓN ──────────────────────────────────────────────
        btnIrTermometro.setOnClickListener {
            startActivity(Intent(this, ThermometerActivity::class.java))
        }

        btnIrActivity2.setOnClickListener {
            startActivity(Intent(this, SensorActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarBotonConexion()
    }

    private fun actualizarBotonConexion() {
        if (MqttManager.isConnected()) {
            btnConectar.text = "DESCONECTAR"
            btnConectar.setBackgroundColor(getColor(android.R.color.holo_red_light))
        } else {
            btnConectar.text = "CONECTAR"
            btnConectar.setBackgroundColor(getColor(R.color.ice_blue))
        }
        btnConectar.isEnabled = true
    }
}
