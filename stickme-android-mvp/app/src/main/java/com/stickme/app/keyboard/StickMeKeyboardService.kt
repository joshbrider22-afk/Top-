package com.stickme.app.keyboard

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo

class StickMeKeyboardService : InputMethodService() {
    private var keyboardView: StickMeKeyboardView? = null

    override fun onCreateInputView(): View {
        val view = StickMeKeyboardView(this) { sticker ->
            StickerInsertManager.insertSticker(
                context = this,
                sticker = sticker,
                inputConnection = currentInputConnection,
                editorInfo = currentInputEditorInfo
            )
        }
        keyboardView = view
        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        keyboardView?.reload()
    }
}
