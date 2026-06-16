package com.example.thermoguard

object MqttConstants {
    // Publish Topics (Setters)
    const val TOPIC_SET_FRIO = "grupoM3/set/tempFrio"
    const val TOPIC_SET_MEDIO = "grupoM3/set/tempMedio"
    const val TOPIC_SET_CALIENTE = "grupoM3/set/tempCaliente"
    const val TOPIC_SET_MODO = "grupoM3/set/modo"

    // Subscribe Topics (Sensors)
    const val TOPIC_SENSOR_TEMP = "grupoM3/sensor/temperatura"
    const val TOPIC_SENSOR_ESTADO = "grupoM3/sensor/estado"
}
