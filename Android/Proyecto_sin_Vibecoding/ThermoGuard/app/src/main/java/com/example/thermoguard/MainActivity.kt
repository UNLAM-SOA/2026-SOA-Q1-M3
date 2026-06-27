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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etBrokerUrl = findViewById(R.id.etBrokerUrl)
        etPort      = findViewById(R.id.etPort)
        btnConectar = findViewById(R.id.btnConectar)

        // Pre-fill with default constants
        etBrokerUrl.setText(Constants.DEFAULT_BROKER)
        etPort.setText(Constants.DEFAULT_PORT)

        actualizarBotonConexion()

        btnConectar.setOnClickListener {

            if (MqttManager.isConnected()) {
                MqttManager.mqttDisconnect()
                btnConectar.postDelayed({ actualizarBotonConexion() }, 300)
                Toast.makeText(this, getString(R.string.toast_desconectando), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val broker = etBrokerUrl.text.toString().trim()
            val port = etPort.text.toString().trim().toIntOrNull()

            if (broker.isEmpty()) {
                etBrokerUrl.error = getString(R.string.error_broker_empty)
                return@setOnClickListener
            }
            if (port == null) {
                etPort.error = getString(R.string.error_port_invalid)
                return@setOnClickListener
            }

            btnConectar.isEnabled = false
            btnConectar.text = getString(R.string.status_connecting)

            MqttManager.onMessage = { _, _ -> }

            MqttManager.mqttConnect(
                brokerAddr = broker,
                port = port,
                onConnected = {
                    runOnUiThread {
                        actualizarBotonConexion()
                        Toast.makeText(this, getString(R.string.toast_connected_to, broker), Toast.LENGTH_SHORT).show()

                        MqttManager.mqttSubscribe(Constants.TOPIC_SENSOR_TEMP, 0)
                        MqttManager.mqttSubscribe(Constants.TOPIC_SENSOR_ESTADO, 1)

                        // Al conectar → ir a la pantalla de configuración
                        startActivity(Intent(this, ConfigActivity::class.java))
                    }
                },
                onError = { errorMsg ->
                    runOnUiThread {
                        actualizarBotonConexion()
                        Toast.makeText(this, getString(R.string.toast_error_prefix, errorMsg), Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarBotonConexion()
    }

    private fun actualizarBotonConexion() {
        if (MqttManager.isConnected()) {
            btnConectar.text = getString(R.string.btn_desconectar)
            btnConectar.setBackgroundColor(getColor(R.color.status_red))
        } else {
            btnConectar.text = getString(R.string.btn_conectar)
            btnConectar.setBackgroundColor(getColor(R.color.primary))
        }
        btnConectar.isEnabled = true
    }
}
