package com.example.thermoguard

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * Pantalla HUB de configuración. Se abre al conectar al broker.
 */
class ConfigActivity : AppCompatActivity() {

    private lateinit var tvEstado: TextView
    private lateinit var btnDesconectar: MaterialButton
    private lateinit var cardTermometro: MaterialCardView
    private lateinit var cardModo: MaterialCardView
    private lateinit var cardSensor: MaterialCardView
    private lateinit var cardBoton: MaterialCardView
    private lateinit var cardHistorial: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        tvEstado       = findViewById(R.id.tvEstado)
        btnDesconectar = findViewById(R.id.btnDesconectar)
        cardTermometro = findViewById(R.id.cardTermometro)
        cardModo       = findViewById(R.id.cardModo)
        cardSensor     = findViewById(R.id.cardSensor)
        cardBoton      = findViewById(R.id.cardBoton)
        cardHistorial  = findViewById(R.id.cardHistorial)

        cardTermometro.setOnClickListener {
            startActivity(Intent(this, ThermometerActivity::class.java))
        }
        cardModo.setOnClickListener {
            startActivity(Intent(this, ModoActivity::class.java))
        }
        cardSensor.setOnClickListener {
            startActivity(Intent(this, SensorActivity::class.java))
        }
        cardBoton.setOnClickListener {
            startActivity(Intent(this, BotonActivity::class.java))
        }
        cardHistorial.setOnClickListener {
            startActivity(Intent(this, HistorialActivity::class.java))
        }

        btnDesconectar.setOnClickListener {
            MqttManager.mqttDisconnect()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarEstado()
    }

    private fun actualizarEstado() {
        if (MqttManager.isConnected()) {
            tvEstado.text = getString(R.string.status_connected)
            tvEstado.setTextColor(getColor(R.color.status_green))
        } else {
            tvEstado.text = getString(R.string.status_disconnected)
            tvEstado.setTextColor(getColor(R.color.status_red))
            finish()
        }
    }
}
