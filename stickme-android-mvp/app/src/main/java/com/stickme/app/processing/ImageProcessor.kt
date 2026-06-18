package com.stickme.app.processing

import android.graphics.Bitmap

object ImageProcessor {
    fun prepareSticker(source: Bitmap, maxDimension: Int = 1024): Bitmap {
        val ratio = minOf(maxDimension.toFloat() / source.width, maxDimension.toFloat() / source.height, 1f)
        val width = (source.width * ratio).toInt().coerceAtLeast(1)
        val height = (source.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, width, height, true)
    }
}
