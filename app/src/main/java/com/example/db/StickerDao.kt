package com.example.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StickerDao {
    @Query("SELECT * FROM stickers ORDER BY timestamp DESC")
    fun getAllStickers(): Flow<List<StickerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSticker(sticker: StickerEntity)

    @Query("DELETE FROM stickers WHERE id = :id")
    suspend fun deleteSticker(id: Int)

    @Query("SELECT * FROM stickers WHERE id = :id LIMIT 1")
    suspend fun getStickerById(id: Int): StickerEntity?
}
