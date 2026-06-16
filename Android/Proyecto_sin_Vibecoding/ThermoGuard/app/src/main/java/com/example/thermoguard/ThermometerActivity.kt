package com.example.thermoguard

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.util.Locale

class ThermometerActivity : AppCompatActivity() {

    // ── Topics MQTT ──────────────────────────────────────────────
    private val TOPIC_FRIO     = "thermoguard/config/tempFrio"
    private val TOPIC_MEDIO    = "thermoguard/config/tempMedio"
    private val TOPIC_CALIENTE = "thermoguard/config/tempCaliente"

    private enum class Estado { FRIO, MEDIO, CALIENTE }

    // ── 3 rangos FIJOS e inalterables (no se solapan) ────────────
    // FRÍO: 0–16.6 · MEDIO: 16.6–33.3 · CALIENTE: 33.3–50
    private val FRIO_MIN     = 0f
    private val FRIO_MAX     = 16.6f
    private val MEDIO_MIN    = 16.6f
    private val MEDIO_MAX    = 33.3f
    private val CALIENTE_MIN = 33.3f
    private val CALIENTE_MAX = 50f

    // Valor actual de cada modo (lo que arrastrás dentro de su rango)
    private var valFrio     = 8f
    private var valMedio    = 25f
    private var valCaliente = 42f

    private var modoActual = Estado.MEDIO

    private val COLOR_FRIO     = Color.parseColor("#4FC3F7")
    private val COLOR_MEDIO    = Color.parseColor("#66BB6A")
    private val COLOR_CALIENTE = Color.parseColor("#EF5350")

    private lateinit var thermometerView: ThermometerView
    private lateinit var tvTemperature: TextView
    private lateinit var tvMode: TextView
    private lateinit var btnBack: MaterialButton
    private lateinit var btnEnviar: MaterialButton
    private lateinit var btnBajo: MaterialButton
    private lateinit var btnMedio: MaterialButton
    private lateinit var btnAlto: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thermometer)

        thermometerView = findViewById(R.id.thermometerView)
        tvTemperature   = findViewById(R.id.tvTemperature)
        tvMode          = findViewById(R.id.tvMode)
        btnBack         = findViewById(R.id.btnBack)
        btnEnviar       = findViewById(R.id.btnEnviar)
        btnBajo         = findViewById(R.id.btnBajo)
        btnMedio        = findViewById(R.id.btnMedio)
        btnAlto         = findViewById(R.id.btnAlto)

        btnBajo.text  = "FRÍO"
        btnMedio.text = "MEDIO"
        btnAlto.text  = "CALIENTE"

        // Refresco de UI (animación o gesto). No toca nada más.
        thermometerView.onTemperatureChangeListener = { temp ->
            val color = colorDe(modoActual)
            tvTemperature.text = String.format(Locale.getDefault(), "%.1f°C", temp)
            tvTemperature.setTextColor(color)
            tvMode.text = "MODO ${modoActual.name}"
            tvMode.setTextColor(color)
        }

        // Al arrastrar: guarda el valor del modo activo
        thermometerView.onUserDragListener = { temp ->
            when (modoActual) {
                Estado.FRIO     -> valFrio = temp
                Estado.MEDIO    -> valMedio = temp
                Estado.CALIENTE -> valCaliente = temp
            }
        }

        btnBajo.setOnClickListener  { aplicarModo(Estado.FRIO) }
        btnMedio.setOnClickListener { aplicarModo(Estado.MEDIO) }
        btnAlto.setOnClickListener  { aplicarModo(Estado.CALIENTE) }

        // ── ENVIAR: publica SOLO el valor del modo seleccionado ──
        btnEnviar.setOnClickListener {
            if (!MqttManager.isConnected()) {
                Toast.makeText(this, "⚠️ No estás conectado al MQTT", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val temp = thermometerView.getTemperature()
            val topic = when (modoActual) {
                Estado.FRIO     -> TOPIC_FRIO
                Estado.MEDIO    -> TOPIC_MEDIO
                Estado.CALIENTE -> TOPIC_CALIENTE
            }
            MqttManager.mqttPublish(topic, temp.toInt().toString(), 1)
            Toast.makeText(this, "✅ Enviado ${modoActual.name}: ${temp.toInt()}°C", Toast.LENGTH_SHORT).show()
        }

        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Estado inicial: MEDIO
        aplicarModo(Estado.MEDIO, intro = true)
    }

    /** Cada modo tiene su rango FIJO. Nunca cambian ni se solapan. */
    private fun aplicarModo(estado: Estado, intro: Boolean = false) {
        modoActual = estado

        val min: Float
        val max: Float
        val valor: Float
        when (estado) {
            Estado.FRIO     -> { min = FRIO_MIN;     max = FRIO_MAX;     valor = valFrio }
            Estado.MEDIO    -> { min = MEDIO_MIN;    max = MEDIO_MAX;    valor = valMedio }
            Estado.CALIENTE -> { min = CALIENTE_MIN; max = CALIENTE_MAX; valor = valCaliente }
        }

        val valorClamped = valor.coerceIn(min, max)

        if (intro) {
            thermometerView.setMode(min, max, valorClamped, colorDe(estado), animate = false)
            thermometerView.post { thermometerView.playIntro(valorClamped) }
        } else {
            thermometerView.setMode(min, max, valorClamped, colorDe(estado), animate = true)
            pulso()
        }
    }

    private fun colorDe(estado: Estado): Int = when (estado) {
        Estado.FRIO     -> COLOR_FRIO
        Estado.MEDIO    -> COLOR_MEDIO
        Estado.CALIENTE -> COLOR_CALIENTE
    }

    private fun pulso() {
        tvTemperature.animate()
            .scaleX(1.06f).scaleY(1.06f)
            .setDuration(110)
            .withEndAction {
                tvTemperature.animate().scaleX(1f).scaleY(1f).setDuration(110).start()
            }
            .start()
    }
}