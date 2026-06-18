package com.stickme.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stickme.app.sticker.StickerStorage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StickMeHome() }
    }
}

@Composable
private fun StickMeHome() {
    val context = LocalContext.current
    var count by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf("Ready. Create, save, share, return.") }

    fun refresh() { count = StickerStorage.listStickers(context).size }
    LaunchedEffect(Unit) { refresh() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching { StickerStorage.importImage(context, uri) }
                .onSuccess {
                    status = "Sticker saved locally. Enable StickMe Keyboard to share."
                    refresh()
                }
                .onFailure { status = "Sticker save failed." }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF050505)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("stick me", color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        Text("Turn moments into stickers.", color = Color.White.copy(alpha = 0.78f))

        Button(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
            onClick = { picker.launch("image/*") }
        ) { Text("Create Sticker", fontWeight = FontWeight.Bold) }

        Button(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            onClick = { context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        ) { Text("Enable StickMe Keyboard", fontWeight = FontWeight.Bold) }

        Text(status, color = Color.White.copy(alpha = 0.84f))
        Text("My Stickers: $count", color = Color.White, fontWeight = FontWeight.Bold)
    }
}
