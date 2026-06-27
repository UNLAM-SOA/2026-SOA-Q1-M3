package com.example.thermoguard

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

/**
 * Pantalla de MODO: 3 botones (BAJO / MEDIO / ALTO).
 * Al tocar uno se publica el estado al ESP32 por MQTT (sin temperatura).
 */
class ModoActivity : AppCompatActivity() {

    private val TOPIC_MODO = Constants.TOPIC_SET_MODO

    private lateinit var btnBack: MaterialButton
    private lateinit var tvModoActual: TextView
    private lateinit var btnBajo: MaterialButton
    private lateinit var btnMedio: MaterialButton
    private lateinit var btnAlto: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modo)

        btnBack      = findViewById(R.id.btnBack)
        tvModoActual = findViewById(R.id.tvModoActual)
        btnBajo      = findViewById(R.id.btnBajo)
        btnMedio     = findViewById(R.id.btnMedio)
        btnAlto      = findViewById(R.id.btnAlto)

        btnBajo.setOnClickListener  { enviarModo(Constants.MSG_MODO_BAJO, getString(R.string.preset_bajo)) }
        btnMedio.setOnClickListener { enviarModo(Constants.MSG_MODO_MEDIO, getString(R.string.preset_medio)) }
        btnAlto.setOnClickListener  { enviarModo(Constants.MSG_MODO_ALTO, getString(R.string.preset_alto)) }

        btnBack.setOnClickListener { finish() }
    }

    private fun enviarModo(payload: String, label: String) {
        if (!MqttManager.isConnected()) {
            Toast.makeText(this, getString(R.string.error_not_connected), Toast.LENGTH_SHORT).show()
            return
        }
        MqttManager.mqttPublish(TOPIC_MODO, payload, 1)
        tvModoActual.text = getString(R.string.modo_actual_prefix, label)
        Toast.makeText(this, getString(R.string.toast_modo_enviado, label), Toast.LENGTH_SHORT).show()
    }
}
