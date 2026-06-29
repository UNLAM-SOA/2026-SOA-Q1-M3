package com.example.thermoguard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * ACTIVIDAD: ConfigActivity
 * DESCRIPCIÓN: Dashboard principal. Visualiza el estado de la FSM del hardware
 * mediante icono + texto y permite la navegación modular.
 */
class ConfigActivity : AppCompatActivity() {

    private lateinit var tvStatusMqtt: TextView
    private lateinit var tvStatusHardware: TextView
    private var ivStatusHardware: ImageView? = null

    private val channelId = "ALARM_CHANNEL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        createNotificationChannel()
        checkNotificationPermission()

        tvStatusMqtt = findViewById(R.id.tvEstado)
        tvStatusHardware = findViewById(R.id.tvEstadoHardware)
        ivStatusHardware = findViewById(R.id.ivStatusHardware)


        // Navegación de módulos
        findViewById<MaterialCardView>(R.id.cardTermometro).setOnClickListener { startActivity(Intent(this, ThermometerActivity::class.java)) }
        findViewById<MaterialCardView>(R.id.cardModo).setOnClickListener { startActivity(Intent(this, ModoActivity::class.java)) }
        findViewById<MaterialCardView>(R.id.cardSensor).setOnClickListener { startActivity(Intent(this, SensorActivity::class.java)) }
        findViewById<MaterialCardView>(R.id.cardBoton).setOnClickListener { startActivity(Intent(this, BotonActivity::class.java)) }
        findViewById<MaterialCardView>(R.id.cardHistorial).setOnClickListener { startActivity(Intent(this, HistorialActivity::class.java)) }

        findViewById<MaterialButton>(R.id.btnDesconectar).setOnClickListener {
            MqttManager.mqttDisconnect()
            finish()
        }
    }

    /**
     * Cambia dinámicamente el icono y el texto del hardware según el estado de la FSM.
     */
    private fun updateHardwareStatus(status: String) {
        val colorRes: Int
        val desc: String
        val iconRes: Int

        when (status) {
            "ALERTA", "ALARMA" -> {
                desc = getString(R.string.status_hardware_alarm)
                colorRes = R.color.status_red
                iconRes = R.drawable.alarm_bell_icon_icons_com_68596
            }
            Constants.STATE_MONITOREANDO -> {
                desc = getString(R.string.status_hardware_monitoring)
                colorRes = R.color.status_green
                iconRes = R.drawable.smart_cctv_camera_monitoring_camera_protection_safety_icon_218664
            }
            Constants.STATE_IDLE -> {
                desc = getString(R.string.status_hardware_idle)
                colorRes = R.color.text_secondary
                iconRes = R.drawable.idle_4
            }
            else -> {
                Log.w("ConfigActivity", "Estado NO reconocido: [$status]")
                desc = status
                colorRes = R.color.text_sub
                iconRes = R.drawable.idle_4
            }
        }

        runOnUiThread {
            tvStatusHardware.text = desc
            tvStatusHardware.setTextColor(ContextCompat.getColor(this, colorRes))

            ivStatusHardware?.apply {
                setImageDrawable(ContextCompat.getDrawable(this@ConfigActivity, iconRes))
                setColorFilter(ContextCompat.getColor(this@ConfigActivity, colorRes))
                visibility = View.VISIBLE
            }

            if (status == Constants.STATE_ALARMA) {
                triggerAlarmActions()
            }
        }
    }

    /**
     * Dispara la vibración. La notificación ahora la maneja de forma centralizada el MqttService.
     */
    private fun triggerAlarmActions() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(1000)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private val mqttListener: (String, String) -> Unit = { topic, msg ->
        if (topic == Constants.TOPIC_SENSOR_ESTADO) {
            val limpio = msg.trim()
                .removeSurrounding("\"")
                .trim()
                .uppercase()
            updateHardwareStatus(limpio)
        }
    }

    override fun onResume() {
        super.onResume()
        syncMqttStatus()
        MqttManager.addListener(mqttListener)
    }

    override fun onPause() {
        super.onPause()
        MqttManager.removeListener(mqttListener)
    }

    private fun syncMqttStatus() {
        if (MqttManager.isConnected()) {
            tvStatusMqtt.text = getString(R.string.status_connected)
            tvStatusMqtt.setTextColor(ContextCompat.getColor(this, R.color.status_green))
        } else {
            finish()
        }
    }
}