package com.example.tuneflow

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.animation.doOnEnd
import kotlin.random.Random
import androidx.core.graphics.withClip

// Specific class for animate sound_container (ic_mute)
class WaveCircleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paintDark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF444444.toInt()
    }

    private val paintLight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF858585.toInt()
    }

    private var level = 0.5f

    init {
        startWaveAnimation()
    }

    private fun startWaveAnimation() {
        val random = Random(System.currentTimeMillis())

        fun animateNext() {
            val minLevel = 0.25f
            val maxLevel = 0.75f
            val minStep = 0.1f // minimum distance between two step
            var target: Float
            do {
                // choose the next step away from the first to better simulate the music
                target = Random.nextFloat() * (maxLevel - minLevel) + minLevel
            } while (Math.abs(target - level) < minStep)


            val duration = random.nextLong(150, 500)

            val animator = ValueAnimator.ofFloat(level, target).apply {
                this.duration = duration
                // allows the movement speed to be randomly changed
                interpolator = if (random.nextBoolean())
                    android.view.animation.AccelerateDecelerateInterpolator()
                else
                    android.view.animation.LinearInterpolator()
                addUpdateListener {
                    level = it.animatedValue as Float
                    invalidate()
                }
                doOnEnd { animateNext() } // to infinity
            }
            animator.start()
        }

        animateNext()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val radius = width / 2f
        val centerX = width / 2f
        val centerY = height / 2f

        canvas.drawCircle(centerX, centerY, radius, paintDark)

        val yLevel = height * (1 - level)

        canvas.withClip(0f, yLevel, width.toFloat(), height.toFloat()) {
            drawCircle(centerX, centerY, radius, paintLight)
        }
    }
}
