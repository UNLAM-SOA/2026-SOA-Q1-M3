package com.example.thermoguard

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import kotlin.math.sqrt

/**
 * Activity 2 - Sensor del dispositivo (acelerómetro).
 * Al AGITAR el teléfono se publica por MQTT el comando para APAGAR la alarma.
 */
class SensorActivity : AppCompatActivity(), SensorEventListener {

    private val TOPIC_ALARMA = Constants.TOPIC_COMANDO_ALARMA

    // Umbral de agitación (subir = menos sensible, bajar = más sensible)
    private val UMBRAL_AGITACION = 13f

    // Tiempo mínimo entre agitaciones para no spamear (ms)
    private val COOLDOWN_MS = 1500L

    private lateinit var sensorManager: SensorManager
    private var acelerometro: Sensor? = null
    private var ultimaAgitacion = 0L

    private lateinit var tvInstruccion: TextView
    private lateinit var tvEstadoSensor: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        tvInstruccion  = findViewById(R.id.tvInstruccion)
        tvEstadoSensor = findViewById(R.id.tvEstadoSensor)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        acelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (acelerometro == null) {
            tvEstadoSensor.text = getString(R.string.sensor_no_acelerometro)
        } else {
            tvInstruccion.text = getString(R.string.instruccion_shake)
            tvEstadoSensor.text = getString(R.string.estado_reposo)
        }
    }

    // Registrar el listener solo cuando la pantalla está visible (ahorra batería)
    override fun onResume() {
        super.onResume()
        acelerometro?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Magnitud de la aceleración menos la gravedad (~9.8)
        val gFuerza = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH

        if (gFuerza > UMBRAL_AGITACION) {
            val ahora = System.currentTimeMillis()
            if (ahora - ultimaAgitacion > COOLDOWN_MS) {
                ultimaAgitacion = ahora
                onAgitacionDetectada()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }

    private fun onAgitacionDetectada() {
        tvEstadoSensor.text = getString(R.string.sensor_agitacion_detectada)

        if (!MqttManager.isConnected()) {
            Toast.makeText(this, getString(R.string.error_no_mqtt), Toast.LENGTH_SHORT).show()
            return
        }

        // Publica el comando para apagar la alarma
        MqttManager.mqttPublish(TOPIC_ALARMA, "OFF", 1)
        Toast.makeText(this, getString(R.string.sensor_alarma_apagada), Toast.LENGTH_SHORT).show()

        // Volver al estado de reposo después de un momento
        tvEstadoSensor.postDelayed({
            tvEstadoSensor.text = getString(R.string.estado_reposo)
        }, 1500)
    }
}
