package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File

/**
 * Result wrapper representing the state of media validation.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val details: Map<String, Any> = emptyMap()
) {
    companion object {
        fun valid(details: Map<String, Any> = emptyMap()) = ValidationResult(true, null, null, details)
        fun invalid(code: String, message: String, details: Map<String, Any> = emptyMap()) = 
            ValidationResult(false, code, message, details)
    }
}

/**
 * Unified verification layer for all sticker load, capture, processing, and storage operations.
 */
object MediaValidator {
    private const val TAG = "MediaValidator"

    // Guardrails for pixel dimensional boundaries
    private const val MIN_STICKER_DIMENSION = 32
    private const val MAX_STICKER_DIMENSION = 4096
    
    // Guardrail for maximum safe sticker file size (in bytes) to prevent custom IME keyboard crash (TransactionTooLargeException)
    // Most devices fail binder transactions around 1MB (1024 * 1024 bytes), so keeping file size under 3MB is a solid boundary.
    private const val MAX_SAFE_FILE_SIZE_BYTES = 3 * 1024 * 1024 // 3 MB

    /**
     * Inspects a Bitmap during the input supply or active creation phase to check for:
     * 1. Integrity (Null values, valid dimensions)
     * 2. Accessibility/Transparency (Empty cutout checks, opacity density, visible colors)
     */
    fun validateBitmap(bitmap: Bitmap?): ValidationResult {
        if (bitmap == null) {
            return ValidationResult.invalid("NULL_BITMAP", "Image source is invalid, empty, or missing.")
        }

        val width = bitmap.width
        val height = bitmap.height

        if (width <= 0 || height <= 0) {
            return ValidationResult.invalid(
                "ZERO_DIMENSIONS", 
                "Image dimension scale is defective or corrupted ($width x $height)."
            )
        }

        if (width < MIN_STICKER_DIMENSION || height < MIN_STICKER_DIMENSION) {
            return ValidationResult.invalid(
                "IMAGE_TOO_SMALL",
                "Sticker image size of $width x $height is too low. Resolution must be at least $MIN_STICKER_DIMENSION px for visual accessibility."
            )
        }

        if (width > MAX_STICKER_DIMENSION || height > MAX_STICKER_DIMENSION) {
            return ValidationResult.invalid(
                "IMAGE_TOO_LARGE",
                "Sticker image size of $width x $height exceeds our $MAX_STICKER_DIMENSION px memory thresholds (risk of OutOfMemoryException)."
            )
        }

        // Integrity & Accessibility check: Inspect transparent vs solid pixels
        val pixels = IntArray(width * height)
        try {
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing bitmap pixel array", e)
            return ValidationResult.invalid(
                "PIXEL_ACCESS_FAILED",
                "Failed to securely parse internal image pixel matrices. Image file may be corrupted."
            )
        }

        var transparentCount = 0
        var totalCount = pixels.size
        var totalLuminance = 0.0
        var opaqueCount = 0

        for (color in pixels) {
            val alpha = (color shr 24) and 0xFF
            if (alpha == 0) {
                transparentCount++
            } else {
                opaqueCount++
                // Calculate basic relative luminance to detect pure blinding white or pure pitch black (lack of detail)
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF
                val luminance = 0.299 * r + 0.587 * g + 0.114 * b
                totalLuminance += luminance
            }
        }

        val transparentPercentage = (transparentCount.toFloat() / totalCount) * 100
        val opaquePercentage = 100 - transparentPercentage

        // Check if image is 100% transparent (no physical content left after cutout erase)
        if (opaqueCount == 0) {
            return ValidationResult.invalid(
                "ENTIRELY_TRANSPARENT",
                "The image contains no visible content because it is completely transparent. Please check your cutout selection."
            )
        }

        // Accessibility warning/fails on extreme low visibility (too small visible component)
        if (opaquePercentage < 0.5) { // less than 0.5% visible
            return ValidationResult.invalid(
                "LOW_VISIBILITY_CONTENT",
                "The visible sticker part is less than 0.5% of the frame, making it visually inaccessible. Adjust editing thresholds."
            )
        }

        val averageLuminance = if (opaqueCount > 0) totalLuminance / opaqueCount else 0.0
        
        // Check if image has solid contrast variation or is a single solid color plane
        var isSingleColorPlane = true
        if (opaqueCount > 1) {
            val firstSampleColor = pixels.firstOrNull { ((it shr 24) and 0xFF) > 0 } ?: 0
            var diffCount = 0
            // sample 100 points
            val step = Math.max(1, totalCount / 100)
            for (i in 0 until totalCount step step) {
                val color = pixels[i]
                val alpha = (color shr 24) and 0xFF
                if (alpha > 0 && color != firstSampleColor) {
                    diffCount++
                    if (diffCount > 5) {
                        isSingleColorPlane = false
                        break
                    }
                }
            }
        } else {
            isSingleColorPlane = false
        }

        val details = mapOf(
            "width" to width,
            "height" to height,
            "total_pixels" to totalCount,
            "opaque_pixels" to opaqueCount,
            "transparent_percentage" to transparentPercentage,
            "average_luminance" to averageLuminance,
            "single_color_plane" to isSingleColorPlane
        )

        return ValidationResult.valid(details)
    }

    /**
     * Inspects a saved File on disk to guarantee that:
     * 1. The document has actual size (> 0 bytes) and exists.
     * 2. The document fits within IPC Binder transaction limits on the keyboard (preventing crashes).
     * 3. The internal bytes construct a valid PNG schema layout (can decode correctly).
     */
    fun validateStickerFile(file: File?): ValidationResult {
        if (file == null || !file.exists()) {
            return ValidationResult.invalid("FILE_NOT_FOUND", "Saved sticker file could not be loaded from storage.")
        }

        val length = file.length()
        if (length == 0L) {
            return ValidationResult.invalid("EMPTY_FILE", "Sticker file length is 0 bytes (corrupted serialization).")
        }

        if (length > MAX_SAFE_FILE_SIZE_BYTES) {
            val sizeMb = length.toFloat() / (1024 * 1024)
            return ValidationResult.invalid(
                "FILE_TOO_LARGE",
                "Saved sticker is too large (%.2f MB). It must be kept under 3.0 MB to avoid crashes over keyboard IPC channels.".format(sizeMb)
            )
        }

        // PNG Header Verification & Safe Bounds Pre-Decoding
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)

        val outMime = options.outMimeType
        if (outMime == null || !outMime.startsWith("image/")) {
            return ValidationResult.invalid(
                "INVALID_MIME_TYPE",
                "File media signature is invalid or not an image. Found mimetype: $outMime"
            )
        }

        val details = mapOf(
            "file_path" to file.absolutePath,
            "file_size_bytes" to length,
            "mime_type" to outMime,
            "width" to options.outWidth,
            "height" to options.outHeight
        )

        if (options.outWidth <= 0 || options.outHeight <= 0) {
            return ValidationResult.invalid(
                "DECODE_FAIL",
                "Internal image structure analysis failed. The physical PNG document is corrupted.",
                details
            )
        }

        return ValidationResult.valid(details)
    }

    /**
     * Helper to perform validations for loaded user URIs.
     */
    fun validateUri(context: Context, uri: Uri?): ValidationResult {
        if (uri == null) {
            return ValidationResult.invalid("NULL_URI", "No directory pointer or item selected.")
        }
        return try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd == null) {
                ValidationResult.invalid("UNREADABLE_URI", "Device permissions prevent reading this media document.")
            } else {
                val size = pfd.statSize
                pfd.close()
                if (size <= 0) {
                    ValidationResult.invalid("EMPTY_URI_STREAM", "The selected media source file contains absolutely 0 bytes.")
                } else {
                    ValidationResult.valid(mapOf("uri" to uri.toString(), "declared_size" to size))
                }
            }
        } catch (e: Exception) {
            ValidationResult.invalid("URI_VAL_EXCEPTION", "Permission error reading media source: ${e.message}")
        }
    }
}
