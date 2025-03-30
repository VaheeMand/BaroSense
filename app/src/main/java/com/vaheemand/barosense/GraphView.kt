package com.vaheemand.barosense

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.util.*
import kotlin.math.max
import kotlin.math.min

class GraphView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val dataPoints = LinkedList<Float>()
    private val maxPoints = 100
    private val paint = Paint().apply {
        color = Color.parseColor("#4CAF50") // Зеленый цвет графика
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#BDBDBD") // Светло-серый цвет сетки
        strokeWidth = 1f
    }
    private val textPaint = Paint().apply {
        color = Color.parseColor("#BDBDBD") // Светло-серый цвет текста
        textSize = 24f
    }

    fun addDataPoint(value: Float) {
        dataPoints.add(value)
        if (dataPoints.size > maxPoints) {
            dataPoints.removeFirst()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val minValue = dataPoints.minOrNull() ?: 0f
        val maxValue = dataPoints.maxOrNull() ?: 0f
        val range = max(1f, maxValue - minValue)

        drawGrid(canvas, width, height, minValue, range)

        val step = width / (maxPoints - 1)
        var x = 0f

        for (i in 1 until dataPoints.size) {
            val y1 = height - ((dataPoints[i-1] - minValue) / range * height)
            val y2 = height - ((dataPoints[i] - minValue) / range * height)
            canvas.drawLine(x, y1, x + step, y2, paint)
            x += step
        }
    }

    private fun drawGrid(canvas: Canvas, width: Float, height: Float, minValue: Float, range: Float) {
        for (i in 0..10) {
            val y = height - (i * height / 10)
            canvas.drawLine(0f, y, width, y, gridPaint)
            val value = minValue + (10 - i) * range / 10
            canvas.drawText("%.1f".format(value), 10f, y - 10, textPaint)
        }
    }
}