package com.example.thermoguard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * SERVICIO: MqttService
 * DESCRIPCIÓN: Servicio en primer plano que mantiene la conexión MQTT persistente.
 */
class MqttService : Service() {

    private val channelId = "MQTT_SERVICE_CHANNEL"
    private val alarmChannelId = "ALARM_CHANNEL"
    private val notificationId = 2002

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannels()
        startForegroundService()

        // Configurar el listener global para alertas
        MqttManager.addListener { topic, msg ->
            if (topic == Constants.TOPIC_SENSOR_ESTADO) {
                val status = msg.trim().removeSurrounding("\"").trim().uppercase()
                // Aceptamos tanto ALERTA como ALARMA por seguridad
                if (status == "ALERTA" || status == "ALARMA") {
                    triggerAlarmNotification()
                }
            }
        }

        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.iot_button_icon_151911)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Monitoreo de seguridad activo en segundo plano")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(notificationId, notification)
    }

    private fun triggerAlarmNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, alarmChannelId)
            .setSmallIcon(R.drawable.alarm_bell_icon_icons_com_68596)
            .setContentTitle(getString(R.string.status_hardware_alarm))
            .setContentText(getString(R.string.notification_alarm_content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sonido, Luz, Vibración por defecto
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1001, builder.build())

        // Refuerzo de vibración manual
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(1000)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            
            // Canal para el servicio persistente
            val serviceChannel = NotificationChannel(
                channelId,
                "Servicio de Monitoreo",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(serviceChannel)

            // Canal para las alertas críticas
            val alarmChannel = NotificationChannel(
                alarmChannelId,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_desc)
            }
            manager.createNotificationChannel(alarmChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MqttService", "Servicio destruido")
    }
}
