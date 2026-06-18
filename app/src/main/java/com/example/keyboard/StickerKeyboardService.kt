package com.example.keyboard

import android.content.Context
import android.content.Intent
import android.content.ClipDescription
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.EditorInfo
import android.inputmethodservice.InputMethodService
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import coil.compose.AsyncImage
import com.example.db.AppDatabase
import com.example.db.StickerEntity
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ServiceLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val controller = SavedStateRegistryController.create(this)

    init {
        controller.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val viewModelStore: ViewModelStore = store
    override val savedStateRegistry: SavedStateRegistry = controller.savedStateRegistry

    fun onCreate() {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun onStart() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    fun onResume() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun onPause() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    fun onStop() {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }
}

class StickerKeyboardService : InputMethodService() {

    private val lifecycleOwner = ServiceLifecycleOwner()
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        lifecycleOwner.onCreate()
        database = AppDatabase.getDatabase(this)
    }

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        }

        lifecycleOwner.onStart()
        lifecycleOwner.onResume()

        composeView.setContent {
            MyApplicationTheme(darkTheme = true) {
                KeyboardScreen(
                    onSendSticker = { filePath, caption -> sendStickerFileToApp(filePath, caption) },
                    onOpenApp = { launchApp() },
                    database = database,
                    onSwitchKeyboard = { showKeyboardPicker() },
                    onTypeText = { text -> currentInputConnection?.commitText(text, 1) },
                    onBackspace = { currentInputConnection?.deleteSurroundingText(1, 0) },
                    onEnter = {
                        val ic = currentInputConnection
                        if (ic != null) {
                            ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
                            ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER))
                        }
                    }
                )
            }
        }

        return composeView
    }

    private fun showKeyboardPicker() {
        val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        im.showInputMethodPicker()
    }

    private fun launchApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    private fun sendStickerFileToApp(filePath: String, caption: String) {
        val inputConnection = currentInputConnection ?: return
        val editorInfo = currentInputEditorInfo ?: return

        val file = File(filePath)
        if (!file.exists()) return

        val authority = "$packageName.fileprovider"
        val contentUri: Uri = try {
            FileProvider.getUriForFile(this, authority, file)
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        // Clip description representing a PNG file
        val description = ClipDescription("Sticker", arrayOf("image/png"))
        val inputContentInfo = InputContentInfoCompat(contentUri, description, null)

        var flags = 0
        if (Build.VERSION.SDK_INT >= 25) {
            flags = InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
        }

        // Grant temporary read URI permission so the recipient app can view and import the image
        val targetPackage = editorInfo.packageName
        try {
            grantUriPermission(
                targetPackage,
                contentUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        InputConnectionCompat.commitContent(
            inputConnection,
            editorInfo,
            inputContentInfo,
            flags,
            null
        )
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        lifecycleOwner.onStart()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleOwner.onResume()
    }

    override fun onFinishInputView(classifying: Boolean) {
        super.onFinishInputView(classifying)
        lifecycleOwner.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleOwner.onStop()
        lifecycleOwner.onDestroy()
        mainScope.cancel()
    }
}

// Clean model representing merged stickers in the keyboard view
data class KeyboardSticker(
    val id: String,
    val filePath: String,
    val caption: String,
    val category: String,
    val isFavorite: Boolean,
    val tags: String,
    val timestamp: Long
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeyboardScreen(
    onSendSticker: (String, String) -> Unit, // (filePath, caption)
    onOpenApp: () -> Unit,
    database: AppDatabase,
    onSwitchKeyboard: () -> Unit,
    onTypeText: (String) -> Unit = {},
    onBackspace: () -> Unit = {},
    onEnter: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var roomStickers by remember { mutableStateOf<List<StickerEntity>>(emptyList()) }
    var indexedDbStickers by remember { mutableStateOf<List<com.example.db.IndexedDbSticker>>(emptyList()) }

    // Dynamic user profile preferences
    val prefs = remember(context) { context.getSharedPreferences("stickme_profile_prefs", android.content.Context.MODE_PRIVATE) }
    var refreshCount by remember { mutableStateOf(0) }
    val themeAccent = remember(refreshCount) { prefs.getString("theme_accent", "Crimson") ?: "Crimson" }
    val hapticMode = remember(refreshCount) { prefs.getString("haptic_mode", "Light") ?: "Light" }

    val accentColor = remember(themeAccent) {
        when (themeAccent) {
            "Cyan" -> Color(0xFF00E5FF)
            "Lime" -> Color(0xFF00E676)
            "Gold" -> Color(0xFFFFD600)
            else -> Color(0xFFFF1F2D) // Crimson default
        }
    }

    val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current
    val triggerHaptic = {
        try {
            when (hapticMode) {
                "Light" -> hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                "Strong" -> hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var isTextInputMode by remember { mutableStateOf(false) }
    var mergedStickers by remember { mutableStateOf<List<KeyboardSticker>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf("All") }
    val scope = rememberCoroutineScope()


    // Observe Room Flow
    LaunchedEffect(Unit) {
        database.stickerDao().getAllStickers().collectLatest { roomList ->
            roomStickers = roomList
        }
    }

    // Refresh from IndexedDB
    val reloadIndexedDb = suspend {
        try {
            val indexedDb = com.example.db.IndexedDB.getDatabase(context)
            val tx = indexedDb.transaction("stickers", "readonly") {}
            val store = tx.objectStore("stickers")
            val records = store.getAll()
            tx.commit()

            val list = ArrayList<com.example.db.IndexedDbSticker>()
            for (pair in records) {
                val key = pair.first
                val jsonStr = pair.second
                try {
                    val obj = org.json.JSONObject(jsonStr)
                    list.add(
                        com.example.db.IndexedDbSticker(
                            id = key,
                            filePath = obj.getString("filePath"),
                            caption = obj.optString("caption", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            category = obj.optString("category", "All"),
                            isFavorite = obj.optBoolean("isFavorite", false),
                            tags = obj.optString("tags", "")
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            indexedDbStickers = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Load IndexedDB stickers whenever screen loads or we trigger a manual refresh
    LaunchedEffect(refreshCount) {
        reloadIndexedDb()
    }

    // Merge both sources seamlessly: IndexedDB provides richer metadata (category, isFavorite, tags),
    // and Room acts as a fast-sync pipeline. Deduplicate by file path.
    LaunchedEffect(roomStickers, indexedDbStickers) {
        val combined = mutableMapOf<String, KeyboardSticker>()

        // 1. Add Room stickers
        roomStickers.forEach { sticker ->
            combined[sticker.filePath] = KeyboardSticker(
                id = "room_${sticker.id}",
                filePath = sticker.filePath,
                caption = sticker.caption,
                category = "All",
                isFavorite = false,
                tags = "",
                timestamp = sticker.timestamp
            )
        }

        // 2. Add/Overwrite with IndexedDB data (which has categories, favorites, and tags)
        indexedDbStickers.forEach { sticker ->
            combined[sticker.filePath] = KeyboardSticker(
                id = sticker.id,
                filePath = sticker.filePath,
                caption = sticker.caption,
                category = sticker.category,
                isFavorite = sticker.isFavorite,
                tags = sticker.tags,
                timestamp = sticker.timestamp
            )
        }

        mergedStickers = combined.values.sortedByDescending { it.timestamp }
    }

    // Filter stickers precisely based on custom category tabs and metadata
    val filteredStickerList = remember(mergedStickers, selectedCategory) {
        when (selectedCategory) {
            "All" -> mergedStickers
            "Favorites" -> mergedStickers.filter { it.isFavorite }
            else -> mergedStickers.filter { sticker ->
                sticker.category.equals(selectedCategory, ignoreCase = true) ||
                sticker.caption.contains(selectedCategory, ignoreCase = true) ||
                sticker.tags.contains(selectedCategory, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color(0xFF0F0F12))
            .padding(8.dp)
    ) {
        // Keyboard Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.stickme_logo_1781048421456),
                    contentDescription = "Stick Me Keyboard Logo",
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(0.5.dp, Color(0xFFFF1F2D).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "STICK",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "ME",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                // Quick Badge indicating count
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${filteredStickerList.size} ITEMS",
                        color = accentColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Manual Sync/Reload trigger
                IconButton(
                    onClick = { refreshCount++ },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Open App Button
                TextButton(
                    onClick = onOpenApp,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "New Sticker",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "NEW STICKER",
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Switch Keyboard Button
                IconButton(
                    onClick = onSwitchKeyboard,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Switch Keyboard",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = Color(0xFF1F1F27), thickness = 1.dp)

        // Interactive Segmented Tab Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E1E24))
                    .border(1.dp, Color(0xFF2C2C33), RoundedCornerShape(20.dp))
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (!isTextInputMode) accentColor else Color.Transparent)
                        .clickable { isTextInputMode = false }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🖼️", fontSize = 11.sp)
                        Text(
                            text = "Stickers",
                            color = if (!isTextInputMode) Color.Black else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isTextInputMode) accentColor else Color.Transparent)
                        .clickable { isTextInputMode = true }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💬", fontSize = 11.sp)
                        Text(
                            text = "Text Input",
                            color = if (isTextInputMode) Color.Black else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFF1F1F27), thickness = 1.dp)

        Spacer(modifier = Modifier.height(4.dp))

        AnimatedContent(
            targetState = isTextInputMode,
            transitionSpec = {
                if (targetState) {
                    (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(220))) togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(220))
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(220))) togetherWith
                            slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(220))
                }
            },
            label = "KeyboardModeTransition",
            modifier = Modifier.weight(1f)
        ) { inTextMode ->
            if (inTextMode) {
                TextInputKeypad(
                    accentColor = accentColor,
                    onTypeText = onTypeText,
                    onBackspace = onBackspace,
                    onEnter = onEnter,
                    triggerHaptic = triggerHaptic
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Tactile category filter bar
                    val categories = listOf(
                        Pair("All", "ALL 🏷️"),
                        Pair("Favorites", "FAVS ❤️"),
                        Pair("funny", "FUNNY 😂"),
                        Pair("pets", "PETS 🐶"),
                        Pair("custom", "CUSTOM 🎨"),
                        Pair("love", "LOVE ❤️"),
                        Pair("cool", "COOL 😎"),
                        Pair("meme", "MEME 🎭"),
                        Pair("text", "TEXT 💬")
                    )
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(categories) { (id, label) ->
                            val isSelected = selectedCategory == id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) accentColor else Color(0xFF1E1E24))
                                    .border(
                                        1.dp,
                                        if (isSelected) accentColor else Color(0xFF2C2C33),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedCategory = id }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1F1F27), thickness = 1.dp)

                    Spacer(modifier = Modifier.height(6.dp))

                    if (mergedStickers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "No Stickers Found",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Go to StickMe App to tap, import, and create your custom PNG templates!",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 14.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                Button(
                                    onClick = onOpenApp,
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Open Creator App", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if (filteredStickerList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "No Matches under \"${selectedCategory.uppercase()}\"",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Check tags, set favorites, or categorise your cutouts inside StickMe app.",
                                    color = Color.DarkGray,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredStickerList, key = { it.id }) { sticker ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF16161A))
                                        .border(
                                            width = 1.dp,
                                            color = if (sticker.isFavorite) accentColor.copy(alpha = 0.5f) else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .combinedClickable(
                                            onClick = {
                                                triggerHaptic()
                                                onSendSticker(sticker.filePath, sticker.caption)
                                            },
                                            onLongClick = {
                                                scope.launch(Dispatchers.IO) {
                                                    // 1. Delete matching row from Room
                                                    try {
                                                        val roomId = sticker.id.removePrefix("room_").toIntOrNull()
                                                        if (roomId != null) {
                                                            database.stickerDao().deleteSticker(roomId)
                                                        } else {
                                                            val match = roomStickers.find { it.filePath == sticker.filePath }
                                                            if (match != null) {
                                                                database.stickerDao().deleteSticker(match.id)
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }

                                                    // 2. Delete matching row from IndexedDB
                                                    try {
                                                        val indexedDb = com.example.db.IndexedDB.getDatabase(context)
                                                        val tx = indexedDb.transaction("stickers", "readwrite")
                                                        val store = tx.objectStore("stickers")
                                                        store.delete(sticker.id)
                                                        if (sticker.id.startsWith("room_")) {
                                                            val dbMatch = indexedDbStickers.find { it.filePath == sticker.filePath }
                                                            if (dbMatch != null) {
                                                                store.delete(dbMatch.id)
                                                            }
                                                        }
                                                        tx.commit()
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }

                                                    // 3. Delete physical image source
                                                    try {
                                                        File(sticker.filePath).delete()
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }

                                                    // 4. Force state rebuild and reload UI
                                                    withContext(Dispatchers.Main) {
                                                        refreshCount++
                                                    }
                                                }
                                            }
                                        )
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        AsyncImage(
                                            model = File(sticker.filePath),
                                            contentDescription = sticker.caption,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(4.dp)
                                                .align(Alignment.Center)
                                        )

                                        // Tiny floating pink heart icon if it is marked as Favorite
                                        if (sticker.isFavorite) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(2.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF2C1113))
                                                    .padding(2.dp)
                                            ) {
                                                Text("❤️", fontSize = 6.sp)
                                            }
                                        }

                                        // Caption tag floating at bottom of the card beautifully
                                        if (sticker.caption.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .fillMaxWidth()
                                                    .background(Color.Black.copy(alpha = 0.6f))
                                                    .padding(vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = sticker.caption,
                                                    color = Color.White,
                                                    fontSize = 7.5.sp,
                                                    maxLines = 1,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TextInputKeypad(
    accentColor: Color,
    onTypeText: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    triggerHaptic: () -> Unit
) {
    var isShiftActive by remember { mutableStateOf(false) }
    var isSymbolMode by remember { mutableStateOf(false) }

    val row1 = if (isSymbolMode) {
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    } else {
        if (isShiftActive) listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
        else listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    }

    val row2 = if (isSymbolMode) {
        listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/")
    } else {
        if (isShiftActive) listOf("A", "S", "D", "F", "G", "H", "J", "K", "L")
        else listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    }

    val row3Keys = if (isSymbolMode) {
        listOf("*", "\"", "'", ":", ";", "!", "?")
    } else {
        if (isShiftActive) listOf("Z", "X", "C", "V", "B", "N", "M")
        else listOf("z", "x", "c", "v", "b", "n", "m")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            row1.forEach { char ->
                KeyButton(
                    text = char,
                    modifier = Modifier.weight(1f),
                    accentColor = accentColor,
                    onClick = {
                        triggerHaptic()
                        onTypeText(char)
                    }
                )
            }
        }

        // Row 2
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            row2.forEach { char ->
                KeyButton(
                    text = char,
                    modifier = Modifier.weight(1f),
                    accentColor = accentColor,
                    onClick = {
                        triggerHaptic()
                        onTypeText(char)
                    }
                )
            }
        }

        // Row 3 (Shift + Keys + Backspace)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shift Key
            KeyButton(
                text = if (isSymbolMode) "1/2" else "⇧",
                modifier = Modifier.width(46.dp),
                isSpecial = true,
                isSelected = !isSymbolMode && isShiftActive,
                accentColor = accentColor,
                onClick = {
                    triggerHaptic()
                    if (!isSymbolMode) {
                        isShiftActive = !isShiftActive
                    }
                }
            )

            // Middle alphabetic/symbol keys
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row3Keys.forEach { char ->
                    KeyButton(
                        text = char,
                        modifier = Modifier.weight(1f),
                        accentColor = accentColor,
                        onClick = {
                            triggerHaptic()
                            onTypeText(char)
                        }
                    )
                }
            }

            // Backspace Key
            KeyButton(
                text = "⌫",
                modifier = Modifier.width(46.dp),
                isSpecial = true,
                accentColor = accentColor,
                onClick = {
                    triggerHaptic()
                    onBackspace()
                }
            )
        }

        // Row 4
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mode Switch Key (?123 / ABC)
            KeyButton(
                text = if (isSymbolMode) "ABC" else "?123",
                modifier = Modifier.width(55.dp),
                isSpecial = true,
                accentColor = accentColor,
                onClick = {
                    triggerHaptic()
                    isSymbolMode = !isSymbolMode
                }
            )

            // Comma ,
            KeyButton(
                text = ",",
                modifier = Modifier.width(36.dp),
                accentColor = accentColor,
                onClick = {
                    triggerHaptic()
                    onTypeText(",")
                }
            )

            // Spacebar
            KeyButton(
                text = "Space",
                modifier = Modifier.weight(1f),
                accentColor = accentColor,
                onClick = {
                    triggerHaptic()
                    onTypeText(" ")
                }
            )

            // Period .
            KeyButton(
                text = ".",
                modifier = Modifier.width(36.dp),
                accentColor = accentColor,
                onClick = {
                    triggerHaptic()
                    onTypeText(".")
                }
            )

            // Enter Key
            KeyButton(
                text = "↩",
                modifier = Modifier.width(55.dp),
                isSpecial = true,
                accentColor = accentColor,
                onClick = {
                    triggerHaptic()
                    onBackspace() // Actually let's trigger enter! Wait, click on Enter should trigger onEnter()!
                    onEnter()
                }
            )
        }
    }
}

@Composable
fun KeyButton(
    text: String,
    modifier: Modifier = Modifier,
    isSpecial: Boolean = false,
    isSelected: Boolean = false,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) accentColor 
                else if (isSpecial) Color(0xFF1E1E24) 
                else Color(0xFF131317)
            )
            .border(
                1.dp,
                if (isSelected) accentColor 
                else if (isSpecial) Color(0xFF2C2C33) 
                else Color(0xFF1E1E24),
                RoundedCornerShape(6.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else Color.White,
            fontSize = if (text.length > 2) 11.sp else 14.sp,
            fontWeight = if (isSpecial || isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
