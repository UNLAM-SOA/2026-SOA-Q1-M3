package com.example.thermoguard

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.util.Locale

class ThermometerActivity : AppCompatActivity() {

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

        // Actualiza lectura + modo + color en cada cambio (incluye la animación)
        thermometerView.onTemperatureChangeListener = { temp ->
            val color = thermometerView.currentColor()
            tvTemperature.text = String.format(Locale.getDefault(), "%.1f°C", temp)
            tvTemperature.setTextColor(color)
            tvMode.text = modoPara(temp)
            tvMode.setTextColor(color)
        }

        // Presets con animación + un pulso sutil en la lectura
        btnBajo.setOnClickListener  { thermometerView.setTemperature(8f);  pulso() }
        btnMedio.setOnClickListener { thermometerView.setTemperature(25f); pulso() }
        btnAlto.setOnClickListener  { thermometerView.setTemperature(42f); pulso() }

        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        btnEnviar.setOnClickListener {
            val temp = thermometerView.getTemperature()
            val modo = modoPara(temp)
            // TODO Parte 2: publicar por MQTT (temp + modo)
            Toast.makeText(
                this,
                "Enviado: ${String.format(Locale.getDefault(), "%.1f°C", temp)}  •  $modo",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Animación de entrada (se llena desde 0)
        thermometerView.post { thermometerView.playIntro(22f) }
    }

    private fun modoPara(temp: Float): String {
        val r = (temp - thermometerView.minTemp) /
                (thermometerView.maxTemp - thermometerView.minTemp)
        return when {
            r < 0.33f -> "MODO BAJO"
            r < 0.66f -> "MODO MEDIO"
            else      -> "MODO ALTO"
        }
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
