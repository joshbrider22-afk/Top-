package com.stickme.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.provider.Settings
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stickme.app.data.LocalStickerStorage
import com.stickme.app.processing.ThumbnailGenerator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val storage = remember { LocalStickerStorage(this) }
            val stickerCount = remember { mutableStateOf(storage.load().size) }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("StickMe", style = MaterialTheme.typography.headlineLarge)
                        Text("Private family stickers on your keyboard.")
                        Spacer(Modifier.height(16.dp))
                        Text("Saved stickers: ${stickerCount.value}")
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {
                            val sticker = DemoStickerFactory.create()
                            val thumb = ThumbnailGenerator.generate(sticker)
                            storage.save(sticker, thumb, "Family Sticker")
                            stickerCount.value = storage.load().size
                        }) {
                            Text("Create demo sticker")
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                        }) {
                            Text("Enable StickMe Keyboard")
                        }
                    }
                }
            }
        }
    }
}

private object DemoStickerFactory {
    fun create(): Bitmap {
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.TRANSPARENT)
        paint.color = Color.rgb(17, 17, 17)
        canvas.drawRoundRect(32f, 80f, 480f, 392f, 48f, 48f, paint)
        paint.color = Color.WHITE
        paint.textSize = 72f
        paint.textAlign = Paint.Align.CENTER
        paint.isFakeBoldText = true
        canvas.drawText("StickMe", 256f, 250f, paint)
        paint.textSize = 34f
        canvas.drawText("Family Sticker", 256f, 315f, paint)
        return bitmap
    }
}
