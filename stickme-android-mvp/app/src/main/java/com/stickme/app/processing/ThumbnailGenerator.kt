package com.stickme.app.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

object ThumbnailGenerator {
    fun generate(source: Bitmap, size: Int = 160): Bitmap {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.TRANSPARENT)
        val scale = minOf(size.toFloat() / source.width, size.toFloat() / source.height)
        val w = source.width * scale
        val h = source.height * scale
        val left = (size - w) / 2f
        val top = (size - h) / 2f
        canvas.drawBitmap(source, null, RectF(left, top, left + w, top + h), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        return output
    }
}
