package com.stickme.app.sticker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.UUID

object StickerStorage {
    private const val STICKER_DIR = "stickers"
    private const val THUMB_DIR = "thumbs"
    private const val THUMB_SIZE = 160

    data class Sticker(
        val id: String,
        val stickerFile: File,
        val thumbnailFile: File,
        val createdAtMillis: Long
    )

    fun importImage(context: Context, sourceUri: Uri): Sticker {
        val bitmap = context.contentResolver.openInputStream(sourceUri).use { input ->
            requireNotNull(input) { "Unable to open selected image." }
            BitmapFactory.decodeStream(input)
        } ?: error("Unable to decode selected image.")

        val id = "stickme_${Instant.now().toEpochMilli()}_${UUID.randomUUID().toString().take(8)}"
        val stickerFile = File(stickerDirectory(context), "$id.png")
        val thumbnailFile = File(thumbnailDirectory(context), "$id.png")

        FileOutputStream(stickerFile).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }

        val thumb = createThumbnail(bitmap, THUMB_SIZE)
        FileOutputStream(thumbnailFile).use { output ->
            thumb.compress(Bitmap.CompressFormat.PNG, 100, output)
        }

        if (thumb != bitmap) thumb.recycle()
        bitmap.recycle()

        return Sticker(id, stickerFile, thumbnailFile, stickerFile.lastModified())
    }

    fun listStickers(context: Context): List<Sticker> {
        val thumbs = thumbnailDirectory(context)
        return stickerDirectory(context)
            .listFiles { file -> file.isFile && file.extension.equals("png", ignoreCase = true) }
            .orEmpty()
            .map { stickerFile ->
                val thumbFile = File(thumbs, stickerFile.name)
                Sticker(
                    id = stickerFile.nameWithoutExtension,
                    stickerFile = stickerFile,
                    thumbnailFile = if (thumbFile.exists()) thumbFile else stickerFile,
                    createdAtMillis = stickerFile.lastModified()
                )
            }
            .sortedByDescending { it.createdAtMillis }
    }

    fun uriForSticker(context: Context, sticker: Sticker): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            sticker.stickerFile
        )
    }

    fun deleteSticker(sticker: Sticker) {
        sticker.stickerFile.delete()
        sticker.thumbnailFile.delete()
    }

    private fun stickerDirectory(context: Context): File = File(context.filesDir, STICKER_DIR).apply { mkdirs() }

    private fun thumbnailDirectory(context: Context): File = File(context.filesDir, THUMB_DIR).apply { mkdirs() }

    private fun createThumbnail(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap

        val scale = maxSize.toFloat() / maxOf(width, height).toFloat()
        val newWidth = (width * scale).toInt().coerceAtLeast(1)
        val newHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
