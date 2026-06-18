package com.stickme.app.keyboard

import android.content.ClipDescription
import android.content.Context
import android.net.Uri
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import com.stickme.app.data.LocalStickerStorage
import com.stickme.app.data.Sticker

object StickerInsertManager {
    fun insertSticker(
        context: Context,
        sticker: Sticker,
        inputConnection: InputConnection?,
        editorInfo: EditorInfo?,
    ): Boolean {
        if (inputConnection == null || editorInfo == null) return false

        val storage = LocalStickerStorage(context)
        val file = storage.fileFor(sticker.stickerPath)
        if (!file.exists()) {
            inputConnection.commitText("[StickMe sticker missing]", 1)
            return false
        }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )

        context.grantUriPermission(editorInfo.packageName, uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val description = ClipDescription(sticker.name, arrayOf("image/png"))
        val inputContentInfo = InputContentInfoCompat(uri, description, null)

        val committed = InputConnectionCompat.commitContent(
            inputConnection,
            editorInfo,
            inputContentInfo,
            InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
            null,
        )

        return if (committed) {
            storage.markUsed(sticker)
            true
        } else {
            inputConnection.commitText("StickMe sticker ready: ${sticker.name}", 1)
            false
        }
    }
}
