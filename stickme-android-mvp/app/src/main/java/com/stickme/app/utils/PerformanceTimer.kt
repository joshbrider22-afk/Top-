package com.stickme.app.utils

import android.util.Log

class PerformanceTimer(private val label: String) {
    private val start = System.nanoTime()
    fun stop() {
        val elapsed = (System.nanoTime() - start) / 1_000_000.0
        Log.d("StickMePerf", "$label: ${"%.2f".format(elapsed)}ms")
    }
}
