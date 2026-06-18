package com.stickme.app.keyboard

import android.content.Context
import android.graphics.BitmapFactory
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.stickme.app.data.LocalStickerStorage
import com.stickme.app.data.Sticker

class StickMeKeyboardView(
    context: Context,
    private val onStickerTap: (Sticker) -> Unit,
) : LinearLayout(context) {
    private val storage = LocalStickerStorage(context)

    init {
        orientation = VERTICAL
        setPadding(16, 16, 16, 16)
        reload()
    }

    fun reload() {
        removeAllViews()
        val title = TextView(context).apply {
            text = "StickMe Keyboard"
            textSize = 16f
            gravity = Gravity.CENTER
        }
        addView(title, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val stickers = storage.load()
        if (stickers.isEmpty()) {
            val empty = TextView(context).apply {
                text = "Create a sticker in the StickMe app first."
                gravity = Gravity.CENTER
                textSize = 14f
            }
            addView(empty, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 120))
            return
        }

        val grid = LinearLayout(context).apply {
            orientation = VERTICAL
        }

        stickers.chunked(4).forEach { rowStickers ->
            val row = LinearLayout(context).apply { orientation = HORIZONTAL }
            rowStickers.forEach { sticker ->
                val file = storage.fileFor(sticker.thumbnailPath)
                val button = if (file.exists()) {
                    ImageButton(context).apply {
                        setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
                        setBackgroundColor(0x00000000)
                        contentDescription = sticker.name
                        setOnClickListener { onStickerTap(sticker) }
                    }
                } else {
                    Button(context).apply {
                        text = sticker.name.take(8)
                        setOnClickListener { onStickerTap(sticker) }
                    }
                }
                row.addView(button, LayoutParams(0, 120, 1f))
            }
            repeat(4 - rowStickers.size) {
                row.addView(TextView(context), LayoutParams(0, 120, 1f))
            }
            grid.addView(row, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        addView(grid, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
}
