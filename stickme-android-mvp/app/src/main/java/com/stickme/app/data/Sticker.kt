package com.stickme.app.data

data class Sticker(
    val id: String,
    val name: String,
    val createdAt: Long,
    val stickerPath: String,
    val thumbnailPath: String,
    val lastUsedAt: Long? = null,
    val isFavorite: Boolean = false
)
