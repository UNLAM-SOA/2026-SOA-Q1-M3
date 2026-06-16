package com.example.thermoguard

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
 * Termómetro interactivo con RANGO acotado por modo.
 *
 *  • La escala visual siempre es minTemp..maxTemp (0..50).
 *  • El usuario solo puede moverse dentro de [rangeMin, rangeMax] (el modo activo).
 *  • setMode() fija el rango, el color y anima hasta el valor inicial (el medio del rango).
 */
class ThermometerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Escala visual completa
    val minTemp = 0f
    val maxTemp = 50f

    // Rango permitido del modo activo (por defecto, todo)
    private var rangeMin = 0f
    private var rangeMax = 50f

    private var temperature = 25f
    private var displayed = 25f

    private var accentColor = Color.parseColor("#4FC3F7")

    private var animator: ValueAnimator? = null

    /** Se dispara SIEMPRE (animación o gesto). Sirve para refrescar la UI. */
    var onTemperatureChangeListener: ((Float) -> Unit)? = null

    /** Se dispara SOLO cuando el usuario arrastra con el dedo. */
    var onUserDragListener: ((Float) -> Unit)? = null

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B2530")
    }
    private val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG)            // zona permitida
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
        if (isInEditMode) { temperature = 25f; displayed = 25f }
    }

    private fun ratioOf(t: Float) = (t - minTemp) / (maxTemp - minTemp)

    /** Color actual (el del modo activo). */
    fun currentColor(): Int = accentColor

    fun getTemperature(): Float = temperature

    /**
     * Configura un modo: acota el rango, fija el color y anima al valor inicial.
     */
    fun setMode(min: Float, max: Float, initial: Float, color: Int, animate: Boolean = true) {
        rangeMin = min
        rangeMax = max
        accentColor = color
        setTemperature(initial, animate)
    }

    fun setTemperature(temp: Float, animate: Boolean = true) {
        val target = temp.coerceIn(rangeMin, rangeMax)   // ← clamp al rango del modo
        temperature = target
        if (!animate) {
            displayed = target
            onTemperatureChangeListener?.invoke(displayed)
            invalidate()
            return
        }
        animator?.cancel()
        animator = ValueAnimator.ofFloat(displayed, target).apply {
            duration = 500
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                displayed = it.animatedValue as Float
                onTemperatureChangeListener?.invoke(displayed)
                invalidate()
            }
            start()
        }
    }

    /** Animación de entrada: llena desde rangeMin hasta el valor actual. */
    fun playIntro(to: Float = temperature) {
        displayed = rangeMin
        invalidate()
        post { setTemperature(to, animate = true) }
    }

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
        val cx = w * 0.46f
        tubeTop = h * 0.05f + tubeW / 2
        tubeBottom = h - bulbR * 1.7f
        val bulbCy = tubeBottom + bulbR * 0.95f

        val left = cx - tubeW / 2
        val right = cx + tubeW / 2
        val usable = tubeBottom - tubeTop

        // Riel + bulbo de fondo
        val track = RectF(left, tubeTop - tubeW / 2, right, tubeBottom)
        canvas.drawRoundRect(track, tubeW / 2, tubeW / 2, trackPaint)
        canvas.drawCircle(cx, bulbCy, bulbR, trackPaint)

        // Zona permitida (banda del modo activo), tenue
        bandPaint.color = (accentColor and 0x00FFFFFF) or 0x33000000   // ~20% alpha
        val yBandTop = tubeBottom - ratioOf(rangeMax) * usable
        val yBandBot = tubeBottom - ratioOf(rangeMin) * usable
        canvas.drawRoundRect(RectF(left, yBandTop, right, yBandBot), tubeW / 2, tubeW / 2, bandPaint)

        // Mercurio (color del modo)
        mercuryPaint.color = accentColor
        val mercTop = tubeBottom - ratioOf(displayed) * usable
        val inset = tubeW * 0.20f
        val mLeft = left + inset
        val mRight = right - inset
        val mW = mRight - mLeft
        if (mercTop < tubeBottom - mW / 2) {
            canvas.drawRoundRect(RectF(mLeft, mercTop, mRight, tubeBottom), mW / 2, mW / 2, mercuryPaint)
        }
        canvas.drawCircle(cx, bulbCy, bulbR * 0.80f, mercuryPaint)

        // Reflejo de vidrio
        val glass = RectF(left + tubeW * 0.20f, tubeTop, left + tubeW * 0.36f, tubeBottom - tubeW)
        canvas.drawRoundRect(glass, tubeW * 0.1f, tubeW * 0.1f, glassPaint)

        // Escala cada 10°
        labelPaint.textSize = h * 0.030f
        var t = minTemp.toInt()
        while (t <= maxTemp.toInt()) {
            val ty = tubeBottom - ratioOf(t.toFloat()) * usable
            canvas.drawLine(right + tubeW * 0.55f, ty, right + tubeW * 0.95f, ty, tickPaint)
            canvas.drawText("$t°", right + tubeW * 1.15f, ty + labelPaint.textSize * 0.35f, labelPaint)
            t += 10
        }

        // Perilla
        knobRingPaint.color = accentColor
        canvas.drawCircle(cx, mercTop, tubeW * 0.36f, knobPaint)
        canvas.drawCircle(cx, mercTop, tubeW * 0.36f, knobRingPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val usable = tubeBottom - tubeTop
                if (usable <= 0f) return true
                val y = event.y.coerceIn(tubeTop, tubeBottom)
                val r = (tubeBottom - y) / usable
                val rawTemp = minTemp + r * (maxTemp - minTemp)
                animator?.cancel()
                // ← clamp al rango del modo: no baja del min ni sube del max
                temperature = rawTemp.coerceIn(rangeMin, rangeMax)
                displayed = temperature
                onTemperatureChangeListener?.invoke(displayed)
                onUserDragListener?.invoke(displayed)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}