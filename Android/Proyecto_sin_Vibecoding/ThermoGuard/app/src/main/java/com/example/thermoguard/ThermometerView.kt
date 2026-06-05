package com.example.thermoguard

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * Termómetro interactivo (Canvas + Paint).
 *
 *  • Arrastrá el dedo para fijar la temperatura (sigue al dedo en tiempo real).
 *  • setTemperature() anima el mercurio de forma suave (presets / intro).
 *  • El color del mercurio interpola de forma gradual: azul → cian → verde → ámbar → rojo.
 */
class ThermometerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    val minTemp = 0f
    val maxTemp = 50f

    private var temperature = 22f      // valor objetivo
    private var displayed = 22f        // valor realmente dibujado (animado)

    private var animator: ValueAnimator? = null
    private val argb = ArgbEvaluator()

    var onTemperatureChangeListener: ((Float) -> Unit)? = null

    // Paleta de interpolación del mercurio
    private val stops = intArrayOf(
        Color.parseColor("#4FC3F7"), // frío
        Color.parseColor("#4DD0E1"),
        Color.parseColor("#66BB6A"),
        Color.parseColor("#FFCA28"),
        Color.parseColor("#EF5350")  // caliente
    )

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B2530")
    }
    private val mercuryPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(28, 255, 255, 255)
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33404D"); strokeWidth = 3f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5B6B79"); textAlign = Paint.Align.LEFT
    }
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val knobRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 7f
    }

    init {
        if (isInEditMode) { temperature = 22f; displayed = 22f }
    }

    private fun ratioOf(t: Float) = (t - minTemp) / (maxTemp - minTemp)

    private fun colorFor(ratio: Float): Int {
        val r = ratio.coerceIn(0f, 1f)
        val seg = 1f / (stops.size - 1)
        val idx = minOf((r / seg).toInt(), stops.size - 2)
        val local = (r - idx * seg) / seg
        return argb.evaluate(local, stops[idx], stops[idx + 1]) as Int
    }

    /** Color actual del mercurio (para sincronizar la lectura grande). */
    fun currentColor(): Int = colorFor(ratioOf(displayed))

    // geometría compartida draw/touch
    private var tubeTop = 0f
    private var tubeBottom = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return

        val tubeW = w * 0.16f
        val bulbR = tubeW * 1.05f
        val cx = w * 0.46f               // tubo centrado, deja aire a la escala
        tubeTop = h * 0.05f + tubeW / 2
        tubeBottom = h - bulbR * 1.7f
        val bulbCy = tubeBottom + bulbR * 0.95f

        val left = cx - tubeW / 2
        val right = cx + tubeW / 2

        // Riel (tubo) + bulbo de fondo
        val track = RectF(left, tubeTop - tubeW / 2, right, tubeBottom)
        canvas.drawRoundRect(track, tubeW / 2, tubeW / 2, trackPaint)
        canvas.drawCircle(cx, bulbCy, bulbR, trackPaint)

        // Mercurio
        val ratio = ratioOf(displayed)
        val color = colorFor(ratio)
        mercuryPaint.color = color
        val usable = tubeBottom - tubeTop
        val mercTop = tubeBottom - ratio * usable
        val inset = tubeW * 0.20f
        val mLeft = left + inset
        val mRight = right - inset
        val mW = mRight - mLeft

        // Columna de mercurio (solo si hay algo que mostrar)
        if (mercTop < tubeBottom - mW / 2) {
            val mercRect = RectF(mLeft, mercTop, mRight, tubeBottom)
            canvas.drawRoundRect(mercRect, mW / 2, mW / 2, mercuryPaint)
        }
        // Bulbo siempre lleno (es el "depósito")
        canvas.drawCircle(cx, bulbCy, bulbR * 0.80f, mercuryPaint)

        // Reflejo de vidrio (sutil)
        val glass = RectF(left + tubeW * 0.20f, tubeTop, left + tubeW * 0.36f, tubeBottom - tubeW)
        canvas.drawRoundRect(glass, tubeW * 0.1f, tubeW * 0.1f, glassPaint)

        // Escala minimalista cada 10°
        labelPaint.textSize = h * 0.030f
        var t = minTemp.toInt()
        while (t <= maxTemp.toInt()) {
            val ty = tubeBottom - ratioOf(t.toFloat()) * usable
            canvas.drawLine(right + tubeW * 0.55f, ty, right + tubeW * 0.95f, ty, tickPaint)
            canvas.drawText("$t°", right + tubeW * 1.15f, ty + labelPaint.textSize * 0.35f, labelPaint)
            t += 10
        }

        // Perilla arrastrable en el nivel actual
        knobRingPaint.color = color
        canvas.drawCircle(cx, mercTop, tubeW * 0.36f, knobPaint)
        canvas.drawCircle(cx, mercTop, tubeW * 0.36f, knobRingPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                setFromY(event.y)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun setFromY(y: Float) {
        val usable = tubeBottom - tubeTop
        if (usable <= 0f) return
        animator?.cancel()
        val clampedY = y.coerceIn(tubeTop, tubeBottom)
        val r = (tubeBottom - clampedY) / usable
        temperature = (minTemp + r * (maxTemp - minTemp)).coerceIn(minTemp, maxTemp)
        displayed = temperature                       // sigue al dedo, sin lag
        onTemperatureChangeListener?.invoke(displayed)
        invalidate()
    }

    /** Cambia la temperatura con animación suave (presets). */
    fun setTemperature(temp: Float, animate: Boolean = true) {
        val target = temp.coerceIn(minTemp, maxTemp)
        temperature = target
        if (!animate) {
            displayed = target
            onTemperatureChangeListener?.invoke(displayed)
            invalidate()
            return
        }
        animator?.cancel()
        animator = ValueAnimator.ofFloat(displayed, target).apply {
            duration = 550
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                displayed = it.animatedValue as Float
                onTemperatureChangeListener?.invoke(displayed)
                invalidate()
            }
            start()
        }
    }

    /** Animación de entrada: llena desde 0 hasta el valor inicial. */
    fun playIntro(to: Float = temperature) {
        displayed = minTemp
        invalidate()
        post { setTemperature(to, animate = true) }
    }

    fun getTemperature(): Float = temperature
}
