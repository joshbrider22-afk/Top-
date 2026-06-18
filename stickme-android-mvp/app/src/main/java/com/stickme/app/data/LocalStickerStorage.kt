package com.stickme.app.data

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.util.UUID

class LocalStickerStorage(private val context: Context) {
    private val root: File get() = context.filesDir
    private val stickersDir: File get() = File(root, "stickers")
    private val thumbnailsDir: File get() = File(root, "thumbnails")
    private val jsonFile: File get() = File(root, "stickers.json")

    init { ensureDirs() }

    fun ensureDirs() {
        stickersDir.mkdirs()
        thumbnailsDir.mkdirs()
        if (!jsonFile.exists()) jsonFile.writeText("[]")
    }

    fun load(): List<Sticker> = StickerJson.decode(jsonFile.readText()).sortedByDescending { it.createdAt }

    fun save(stickerBitmap: Bitmap, thumbnailBitmap: Bitmap, name: String = "Family Sticker"): Sticker {
        ensureDirs()
        val id = "sticker_${UUID.randomUUID()}"
        val stickerFile = File(stickersDir, "$id.png")
        val thumbFile = File(thumbnailsDir, "$id.png")
        stickerFile.outputStream().use { stickerBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        thumbFile.outputStream().use { thumbnailBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val sticker = Sticker(id, name, System.currentTimeMillis(), "stickers/${stickerFile.name}", "thumbnails/${thumbFile.name}")
        persist(listOf(sticker) + load())
        return sticker
    }

    fun delete(sticker: Sticker) {
        File(root, sticker.stickerPath).delete()
        File(root, sticker.thumbnailPath).delete()
        persist(load().filterNot { it.id == sticker.id })
    }

    fun markUsed(sticker: Sticker) {
        val updated = load().map { if (it.id == sticker.id) it.copy(lastUsedAt = System.currentTimeMillis()) else it }
            .sortedByDescending { it.lastUsedAt ?: it.createdAt }
        persist(updated)
    }

    fun fileFor(path: String): File = File(root, path)
    private fun persist(stickers: List<Sticker>) { jsonFile.writeText(StickerJson.encode(stickers)) }
}
