package com.example.db

/**
 * Represents a sticker record inside our IndexedDB local schema object store.
 */
data class IndexedDbSticker(
    val id: String,
    val filePath: String,
    val caption: String,
    val timestamp: Long,
    val category: String = "All",
    val isFavorite: Boolean = false,
    val tags: String = ""
)
