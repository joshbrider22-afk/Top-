package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.example.utils.MediaValidator
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MediaValidatorTest {

    @Test
    fun testValidateBitmap_nullBitmap_returnsInvalid() {
        val result = MediaValidator.validateBitmap(null)
        assertFalse(result.isValid)
        assertEquals("NULL_BITMAP", result.errorCode)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun testValidateBitmap_tooSmall_returnsInvalid() {
        // Minimum width/height is 32px
        val smallBitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        val result = MediaValidator.validateBitmap(smallBitmap)
        
        assertFalse(result.isValid)
        assertEquals("IMAGE_TOO_SMALL", result.errorCode)
        assertTrue(result.errorMessage?.contains("too low") == true)
    }

    @Test
    fun testValidateBitmap_tooLarge_returnsInvalid() {
        // Maximum size is 4096px
        val largeBitmap = Bitmap.createBitmap(4100, 100, Bitmap.Config.ARGB_8888)
        val result = MediaValidator.validateBitmap(largeBitmap)
        
        assertFalse(result.isValid)
        assertEquals("IMAGE_TOO_LARGE", result.errorCode)
        assertTrue(result.errorMessage?.contains("exceeds") == true)
    }

    @Test
    fun testValidateBitmap_fullyTransparent_returnsInvalid() {
        // Create an empty, transparent 100x100 bitmap
        val emptyBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val result = MediaValidator.validateBitmap(emptyBitmap)
        
        assertFalse(result.isValid)
        assertEquals("ENTIRELY_TRANSPARENT", result.errorCode)
        assertTrue(result.errorMessage?.contains("completely transparent") == true)
    }

    @Test
    fun testValidateBitmap_validColored_returnsValid() {
        // Create a 100x100 bitmap and draw on it so it has solid, opaque pixels
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.RED) // entire bitmap is opaque red
        
        val result = MediaValidator.validateBitmap(bitmap)
        
        assertTrue(result.isValid)
        assertNull(result.errorCode)
        assertEquals(100, result.details["width"])
        assertEquals(100, result.details["height"])
        assertEquals(0.0, result.details["transparent_percentage"])
    }

    @Test
    fun testValidateBitmap_partialCutout_returnsValid() {
        // Create an image with both transparent and non-transparent pixels (a cutout sticker)
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        // Set the half to red and leave other half transparent
        for (x in 0 until 50) {
            for (y in 0 until 100) {
                bitmap.setPixel(x, y, Color.BLUE)
            }
        }
        
        val result = MediaValidator.validateBitmap(bitmap)
        
        assertTrue(result.isValid)
        assertNull(result.errorCode)
        
        val transPercent = result.details["transparent_percentage"] as Float
        assertEquals(50f, transPercent, 0.1f)
    }
}
