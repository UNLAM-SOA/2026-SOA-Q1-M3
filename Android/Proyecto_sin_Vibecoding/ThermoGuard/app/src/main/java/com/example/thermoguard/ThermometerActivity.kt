package com.example.thermoguard

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.util.Locale

class ThermometerActivity : AppCompatActivity() {

    // ── Topics MQTT ──────────────────────────────────────────────
    private val TOPIC_FRIO     = Constants.TOPIC_SET_FRIO
    private val TOPIC_MEDIO    = Constants.TOPIC_SET_MEDIO
    private val TOPIC_CALIENTE = Constants.TOPIC_SET_CALIENTE

    private enum class Estado { FRIO, MEDIO, CALIENTE }

    // ── 3 rangos FIJOS e inalterables (no se solapan) ────────────
    private val FRIO_MIN     = Constants.FRIO_MIN
    private val FRIO_MAX     = Constants.FRIO_MAX
    private val MEDIO_MIN    = Constants.MEDIO_MIN
    private val MEDIO_MAX    = Constants.MEDIO_MAX
    private val CALIENTE_MIN = Constants.CALIENTE_MIN
    private val CALIENTE_MAX = Constants.CALIENTE_MAX

    // Valor actual de cada modo
    private var valFrio     = 8f
    private var valMedio    = 25f
    private var valCaliente = 42f

    private var modoActual = Estado.MEDIO

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

        btnBajo.text  = getString(R.string.label_frio)
        btnMedio.text = getString(R.string.preset_medio)
        btnAlto.text  = getString(R.string.label_caliente)

        // Refresco de UI
        thermometerView.onTemperatureChangeListener = { temp ->
            val color = colorDe(modoActual)
            tvTemperature.text = String.format(Locale.getDefault(), getString(R.string.temp_format), temp)
            tvTemperature.setTextColor(color)
            val modoStr = when(modoActual) {
                Estado.FRIO -> getString(R.string.label_frio)
                Estado.MEDIO -> getString(R.string.preset_medio)
                Estado.CALIENTE -> getString(R.string.label_caliente)
            }
            tvMode.text = getString(R.string.mode_format, modoStr)
            tvMode.setTextColor(color)
        }

        // Al arrastrar: guarda el valor
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

        btnEnviar.setOnClickListener {
            if (!MqttManager.isConnected()) {
                Toast.makeText(this, getString(R.string.error_not_connected), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val temp = thermometerView.getTemperature()
            val topic = when (modoActual) {
                Estado.FRIO     -> TOPIC_FRIO
                Estado.MEDIO    -> TOPIC_MEDIO
                Estado.CALIENTE -> TOPIC_CALIENTE
            }
            val modoStr = when(modoActual) {
                Estado.FRIO -> getString(R.string.label_frio)
                Estado.MEDIO -> getString(R.string.preset_medio)
                Estado.CALIENTE -> getString(R.string.label_caliente)
            }
            MqttManager.mqttPublish(topic, temp.toInt().toString(), 1)
            Toast.makeText(this, getString(R.string.toast_enviado_format, modoStr, temp.toInt()), Toast.LENGTH_SHORT).show()
        }

        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        aplicarModo(Estado.MEDIO, intro = true)
    }

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
        val color = colorDe(estado)
        if (intro) {
            thermometerView.setMode(min, max, valorClamped, color, animate = false)
            thermometerView.post { thermometerView.playIntro(valorClamped) }
        } else {
            thermometerView.setMode(min, max, valorClamped, color, animate = true)
            pulso()
        }
    }

    private fun colorDe(estado: Estado): Int = when (estado) {
        Estado.FRIO     -> ContextCompat.getColor(this, R.color.thermometer_frio)
        Estado.MEDIO    -> ContextCompat.getColor(this, R.color.thermometer_medio)
        Estado.CALIENTE -> ContextCompat.getColor(this, R.color.thermometer_caliente)
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
