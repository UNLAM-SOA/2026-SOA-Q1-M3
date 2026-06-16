package com.example.thermoguard

object Constants {
    // MQTT Topics
    const val TOPIC_SET_FRIO = "grupoM3/set/tempFrio"
    const val TOPIC_SET_MEDIO = "grupoM3/set/tempMedio"
    const val TOPIC_SET_CALIENTE = "grupoM3/set/tempCaliente"
    const val TOPIC_SET_MODO = "grupoM3/set/modo"
    const val TOPIC_SENSOR_TEMP = "grupoM3/sensor/temperatura"
    const val TOPIC_SENSOR_ESTADO = "grupoM3/sensor/estado"

    // Temperature Ranges
    const val FRIO_MIN = 0f
    const val FRIO_MAX = 16.6f
    const val MEDIO_MIN = 16.6f
    const val MEDIO_MAX = 33.3f
    const val CALIENTE_MIN = 33.3f
    const val CALIENTE_MAX = 50f
}
