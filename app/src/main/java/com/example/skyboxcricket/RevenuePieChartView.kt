package com.example.skyboxcricket

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class RevenuePieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.card_stroke)
    }

    private val oval = RectF()
    private var slices: List<Slice> = emptyList()
    private var animatedFraction = 1f

    init {
        val stroke = resources.displayMetrics.density * 22f
        slicePaint.strokeWidth = stroke
        trackPaint.strokeWidth = stroke
    }

    fun setData(total: Double, online: Double, offline: Double) {
        setSlices(
            total = total,
            values = listOf(
                online.toFloat().coerceAtLeast(0f) to ContextCompat.getColor(context, R.color.brand_blue),
                offline.toFloat().coerceAtLeast(0f) to ContextCompat.getColor(context, R.color.brand_navy)
            )
        )
    }

    fun setCustomPairData(
        firstValue: Double,
        secondValue: Double,
        firstColor: Int,
        secondColor: Int
    ) {
        setSlices(
            total = firstValue + secondValue,
            values = listOf(
                firstValue.toFloat().coerceAtLeast(0f) to firstColor,
                secondValue.toFloat().coerceAtLeast(0f) to secondColor
            )
        )
    }

    private fun setSlices(total: Double, values: List<Pair<Float, Int>>) {
        val safeTotal = total.coerceAtLeast(0.0)
        slices = if (safeTotal <= 0.0) {
            emptyList()
        } else {
            values.map { Slice(it.first, it.second) }
        }
        startAnimation()
    }

    private fun startAnimation() {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 900L
        animator.addUpdateListener {
            animatedFraction = it.animatedValue as Float
            invalidate()
        }
        animator.start()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val maxSize = (resources.displayMetrics.density * 280).toInt()
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val boundedHeight = if (heightMode == MeasureSpec.UNSPECIFIED) maxSize else heightSize
        val resolved = minOf(widthSize, boundedHeight, maxSize).coerceAtLeast(suggestedMinimumWidth)
        setMeasuredDimension(resolved, resolved)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val inset = slicePaint.strokeWidth / 2f + paddingLeft
        oval.set(
            inset,
            inset,
            width - inset - paddingRight,
            height - inset - paddingBottom
        )

        canvas.drawArc(oval, -90f, 360f, false, trackPaint)

        if (slices.isEmpty()) return

        val totalValue = slices.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)
        var startAngle = -90f
        slices.forEach { slice ->
            val sweep = (slice.value / totalValue) * 360f * animatedFraction
            slicePaint.color = slice.color
            canvas.drawArc(oval, startAngle, sweep, false, slicePaint)
            startAngle += (slice.value / totalValue) * 360f
        }
    }

    private data class Slice(
        val value: Float,
        val color: Int
    )
}
