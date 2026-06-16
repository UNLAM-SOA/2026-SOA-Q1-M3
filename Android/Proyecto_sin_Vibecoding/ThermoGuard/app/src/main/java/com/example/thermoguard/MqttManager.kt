package com.example.thermoguard

import android.util.Log
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

/**
 * Manager MQTT basado en el enfoque de la cátedra (UNLaM SOA):
 * usa MqttClient (cliente Java puro) en lugar de MqttAndroidClient.
 *
 * Diferencia clave: NO usa el servicio de Android de Paho (el que crashea en API 36).
 * Por eso TODO se ejecuta en un Thread aparte (connect/publish bloquean el hilo).
 */
object MqttManager {

    private const val TAG = "MQTT_THERMO"

    private var mqttClient: MqttClient? = null

    /** Listener para mensajes entrantes (lo setea la Activity). */
    var onMessage: ((String, String) -> Unit)? = null

    /**
     * Conexión ANÓNIMA. Mismos nombres de función que antes.
     * Se ejecuta en un Thread interno para no bloquear la UI.
     */
    fun mqttConnect(
        brokerAddr: String,
        port: Int,
        onConnected: () -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val serverUri = "tcp://$brokerAddr:$port"
                mqttClient = MqttClient(serverUri, MqttClient.generateClientId(), MemoryPersistence())

                mqttClient?.setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        Log.e(TAG, "Conexión perdida: ${cause?.message}")
                    }
                    override fun messageArrived(topic: String, message: MqttMessage) {
                        val payload = String(message.payload)
                        Log.d(TAG, "Recibido: $topic → $payload")
                        onMessage?.invoke(topic, payload)
                    }
                    override fun deliveryComplete(token: IMqttDeliveryToken?) { }
                })

                val options = MqttConnectOptions()
                options.isCleanSession = true
                options.connectionTimeout = 10
                options.keepAliveInterval = 60
                // ANONIMO: no se setea userName ni password

                mqttClient?.connect(options)

                Log.i(TAG, "Conectado a $serverUri")
                onConnected()

            } catch (e: Exception) {
                Log.e(TAG, "Error al conectar: ${e.message}", e)
                onError(e.message ?: "Error desconocido")
            }
        }.start()
    }

    fun mqttSubscribe(topic: String, qos: Int) {
        Thread {
            try {
                mqttClient?.subscribe(topic, qos)
                Log.d(TAG, "Suscripto a: $topic")
            } catch (e: Exception) {
                Log.e(TAG, "Error al suscribirse: ${e.message}", e)
            }
        }.start()
    }

    fun mqttPublish(topic: String, msg: String, qos: Int) {
        Thread {
            try {
                val message = MqttMessage(msg.toByteArray())
                message.qos = qos
                message.isRetained = false
                mqttClient?.publish(topic, message)
                Log.d(TAG, "Publicado en $topic: $msg")
            } catch (e: Exception) {
                Log.e(TAG, "Error al publicar: ${e.message}", e)
            }
        }.start()
    }

    fun mqttDisconnect() {
        Thread {
            try {
                if (mqttClient?.isConnected == true) {
                    mqttClient?.disconnect()
                }
                mqttClient?.close()
                mqttClient = null
                Log.d(TAG, "Desconectado")
            } catch (e: Exception) {
                Log.e(TAG, "Error al desconectar: ${e.message}", e)
            }
        }.start()
    }

    fun isConnected(): Boolean = mqttClient?.isConnected == true
}
