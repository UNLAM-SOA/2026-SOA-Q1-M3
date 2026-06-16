package com.example.thermoguard

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * Pantalla HUB de configuración. Se abre al conectar al broker.
 * Desde acá se accede al termómetro, al modo y al sensor.
 * Muestra el estado de conexión y permite desconectar.
 */
class ConfigActivity : AppCompatActivity() {

    private lateinit var tvEstado: TextView
    private lateinit var btnDesconectar: MaterialButton
    private lateinit var cardTermometro: MaterialCardView
    private lateinit var cardModo: MaterialCardView
    private lateinit var cardSensor: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        tvEstado       = findViewById(R.id.tvEstado)
        btnDesconectar = findViewById(R.id.btnDesconectar)
        cardTermometro = findViewById(R.id.cardTermometro)
        cardModo       = findViewById(R.id.cardModo)
        cardSensor     = findViewById(R.id.cardSensor)

        cardTermometro.setOnClickListener {
            startActivity(Intent(this, ThermometerActivity::class.java))
        }
        cardModo.setOnClickListener {
            startActivity(Intent(this, ModoActivity::class.java))
        }
        cardSensor.setOnClickListener {
            startActivity(Intent(this, SensorActivity::class.java))
        }

        btnDesconectar.setOnClickListener {
            MqttManager.mqttDisconnect()
            finish() // vuelve a la pantalla de conexión (MainActivity)
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
            finish() // si se cae la conexión, volvemos a conectar
        }
    }
}
