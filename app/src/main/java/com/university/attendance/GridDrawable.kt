package com.university.attendance

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

class GridDrawable : Drawable() {

    private val paint = Paint().apply {
        color = 0x0A3B6BF8.toInt()
        strokeWidth = 1f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val gridSize = 40f

    override fun draw(canvas: Canvas) {
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()

        var x = 0f
        while (x <= width) {
            canvas.drawLine(x, 0f, x, height, paint)
            x += gridSize
        }

        var y = 0f
        while (y <= height) {
            canvas.drawLine(0f, y, width, y, paint)
            y += gridSize
        }
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(cf: ColorFilter?) { paint.colorFilter = cf }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}