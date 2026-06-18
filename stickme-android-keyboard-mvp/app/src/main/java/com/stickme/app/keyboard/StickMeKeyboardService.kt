package com.stickme.app.keyboard

import android.content.ClipDescription
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import com.stickme.app.sticker.StickerStorage

class StickMeKeyboardService : android.inputmethodservice.InputMethodService() {
    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
            setBackgroundColor(0xFF050505.toInt())
        }

        val title = TextView(this).apply {
            text = "StickMe Keyboard"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 18f
            gravity = Gravity.CENTER_VERTICAL
        }
        root.addView(title, LinearLayout.LayoutParams.MATCH_PARENT, 56)

        val stickers = StickerStorage.listStickers(this)
        if (stickers.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No stickers yet. Open StickMe and create one first."
                setTextColor(0xCCFFFFFF.toInt())
                textSize = 15f
                gravity = Gravity.CENTER
            }
            root.addView(empty, LinearLayout.LayoutParams.MATCH_PARENT, 180)
            return root
        }

        val scroll = ScrollView(this)
        val grid = GridLayout(this).apply { columnCount = 4 }
        stickers.forEach { sticker ->
            val button = Button(this).apply {
                text = ""
                minWidth = 0
                minHeight = 0
                background = BitmapFactory.decodeFile(sticker.thumbnailFile.absolutePath)?.let { bitmap ->
                    BitmapDrawable(resources, bitmap)
                }
                setOnClickListener { commitSticker(sticker) }
            }
            grid.addView(button, GridLayout.LayoutParams().apply {
                width = 160
                height = 160
            })
        }
        scroll.addView(grid)
        root.addView(scroll, LinearLayout.LayoutParams.MATCH_PARENT, 420)
        return root
    }

    private fun commitSticker(sticker: StickerStorage.Sticker) {
        val inputConnection = currentInputConnection ?: return
        val editorInfo = currentInputEditorInfo ?: EditorInfo()
        val uri = StickerStorage.uriForSticker(this, sticker)
        val contentInfo = InputContentInfoCompat(uri, ClipDescription("StickMe sticker", arrayOf("image/png")), null)
        val inserted = InputConnectionCompat.commitContent(
            inputConnection,
            editorInfo,
            contentInfo,
            InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
            null
        )
        if (!inserted) {
            inputConnection.commitText("[StickMe sticker saved locally]", 1)
            Toast.makeText(this, "Fallback text sent.", Toast.LENGTH_SHORT).show()
        }
    }
}
