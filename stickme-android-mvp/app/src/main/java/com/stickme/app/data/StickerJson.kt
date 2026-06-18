package com.stickme.app.data

import org.json.JSONArray
import org.json.JSONObject

object StickerJson {
    fun encode(stickers: List<Sticker>): String {
        val array = JSONArray()
        stickers.forEach { sticker ->
            array.put(
                JSONObject()
                    .put("id", sticker.id)
                    .put("name", sticker.name)
                    .put("createdAt", sticker.createdAt)
                    .put("stickerPath", sticker.stickerPath)
                    .put("thumbnailPath", sticker.thumbnailPath)
                    .put("lastUsedAt", sticker.lastUsedAt)
                    .put("isFavorite", sticker.isFavorite)
            )
        }
        return array.toString(2)
    }

    fun decode(json: String): List<Sticker> {
        if (json.isBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val stickerPath = item.optString("stickerPath").takeIf { it.isNotBlank() } ?: continue
                    val thumbnailPath = item.optString("thumbnailPath").takeIf { it.isNotBlank() } ?: continue
                    add(
                        Sticker(
                            id = id,
                            name = item.optString("name", "Family Sticker"),
                            createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                            stickerPath = stickerPath,
                            thumbnailPath = thumbnailPath,
                            lastUsedAt = if (item.isNull("lastUsedAt")) null else item.optLong("lastUsedAt"),
                            isFavorite = item.optBoolean("isFavorite", false)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
