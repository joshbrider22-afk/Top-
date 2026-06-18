package com.example

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.example.db.AppDatabase
import com.example.db.StickerEntity
import com.example.network.GeminiClient
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.StickerGalleryPlaceholder
import com.example.ui.CameraCaptureScreen
import com.example.ui.GalleryPickerScreen
import com.example.ui.KeyboardSetupScreen
import com.example.ui.ProfileScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.BitSet
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = AppDatabase.getDatabase(this)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF0B0B0D)
                ) { innerPadding ->
                    StickerStudioScreen(
                        modifier = Modifier.padding(innerPadding),
                        database = database,
                        activityContext = this
                    )
                }
            }
        }
    }
}

@Composable
fun StickerStudioScreen(
    modifier: Modifier = Modifier,
    database: AppDatabase,
    activityContext: ComponentActivity
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Loaded image states
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var editingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var updateTrigger by remember { mutableStateOf(0) } // Forces recompositions on mutable bitmap edits

    // Undo stack
    val undoStack = remember { mutableStateListOf<Bitmap>() }

    // Editor settings
    var activeTool by remember { mutableStateOf("magic") } // "magic", "erase", "restore"
    var magicStrength by remember { mutableStateOf(32) }
    var brushSize by remember { mutableStateOf(40f) }
    var outlineSize by remember { mutableStateOf(8) }
    var outlineColor by remember { mutableStateOf(Color.White) }
    var showShadow by remember { mutableStateOf(true) }
    var captionText by remember { mutableStateOf("") }
    var backgroundTheme by remember { mutableStateOf("checker") } // "checker", "dark", "light", "chat"
    var creationCategoryTarget by remember { mutableStateOf("uncategorized") }
    var creationTagsInput by remember { mutableStateOf("") }
    var stickerToEdit by remember { mutableStateOf<com.example.db.IndexedDbSticker?>(null) }
    var touchPos by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    var preFilterBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showInAppCamera by remember { mutableStateOf(false) }
    var showInAppGallery by remember { mutableStateOf(false) }
    var showPhotoSourceChooser by remember { mutableStateOf(false) }
    var showCameraSourceChooser by remember { mutableStateOf(false) }

    // Previously created sticker items (using IndexedDB schema)
    var savedStickersList by remember { mutableStateOf<List<com.example.db.IndexedDbSticker>>(emptyList()) }

    // AI suggestion states
    var aiSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var loadingSuggestions by remember { mutableStateOf(false) }

    // UI helpers
    var activeTab by remember { mutableStateOf(0) } // 0: Editor, 1: My Stickers, 2: Keyboard Setup
    var showSuccessDialog by remember { mutableStateOf(false) }
    var busyWorking by remember { mutableStateOf(false) }

    // Interactive Profile/Settings configuration states
    val profilePrefs = remember { context.getSharedPreferences("stickme_profile_prefs", Context.MODE_PRIVATE) }
    var currentAccentName by remember { mutableStateOf(profilePrefs.getString("theme_accent", "Crimson") ?: "Crimson") }
    LaunchedEffect(activeTab) {
        currentAccentName = profilePrefs.getString("theme_accent", "Crimson") ?: "Crimson"
    }
    val currentAccentColor = remember(currentAccentName) {
        when (currentAccentName) {
            "Cyan" -> Color(0xFF00E5FF)
            "Lime" -> Color(0xFF00E676)
            "Gold" -> Color(0xFFFFD600)
            else -> Color(0xFFFF1F2D) // Crimson/Red fallback
        }
    }

    // IndexedDB Instance & Logs Terminal state
    val indexedDb = remember { com.example.db.IndexedDB.getDatabase(context) }
    var indexedDbLogs by remember { mutableStateOf<List<String>>(listOf("System: Connected to IndexedDB database 'stickme_indexed_db' [v1].")) }

    val addLog: (String) -> Unit = { message ->
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        indexedDbLogs = (listOf("[$timestamp] $message") + indexedDbLogs).take(40)
    }

    // Function to reload from IndexedDB
    val reloadFromIndexedDb = {
        scope.launch {
            try {
                addLog("TX: Starting READONLY transaction on 'stickers' objectStore...")
                val tx = indexedDb.transaction("stickers", "readonly") { status ->
                    addLog("TX Status: $status")
                }
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
                savedStickersList = list
                addLog("Database sync: Loaded ${list.size} stickers of keypath 'id' from 'stickers' store.")
            } catch (e: Exception) {
                addLog("Error reading IndexedDB: ${e.message}")
            }
        }
    }

    // Read stored stickers
    LaunchedEffect(Unit) {
        reloadFromIndexedDb()
    }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val uriCheck = com.example.utils.MediaValidator.validateUri(context, it)
            if (!uriCheck.isValid) {
                Toast.makeText(context, uriCheck.errorMessage ?: "Selected image file is corrupted or empty.", Toast.LENGTH_LONG).show()
                return@let
            }
            busyWorking = true
            scope.launch(Dispatchers.IO) {
                val loaded = loadBitmapFromUri(context, it)
                val bitmapCheck = com.example.utils.MediaValidator.validateBitmap(loaded)
                if (loaded != null && bitmapCheck.isValid) {
                    val configCopy = loaded.copy(Bitmap.Config.ARGB_8888, true)
                    val origCopy = loaded.copy(Bitmap.Config.ARGB_8888, false)
                    withContext(Dispatchers.Main) {
                        originalBitmap = origCopy
                        editingBitmap = configCopy
                        undoStack.clear()
                        aiSuggestions = emptyList() // clear previous suggestions
                        busyWorking = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, bitmapCheck.errorMessage ?: "Error validating image integrity.", Toast.LENGTH_LONG).show()
                        busyWorking = false
                    }
                }
            }
        }
    }

    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Take Picture Launcher (High Quality Photo)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = tempPhotoUri ?: return@rememberLauncherForActivityResult
            val uriCheck = com.example.utils.MediaValidator.validateUri(context, uri)
            if (!uriCheck.isValid) {
                Toast.makeText(context, uriCheck.errorMessage ?: "Snapped picture is corrupted or empty.", Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }
            busyWorking = true
            scope.launch(Dispatchers.IO) {
                val loaded = loadBitmapFromUri(context, uri)
                val bitmapCheck = com.example.utils.MediaValidator.validateBitmap(loaded)
                if (loaded != null && bitmapCheck.isValid) {
                    val configCopy = loaded.copy(Bitmap.Config.ARGB_8888, true)
                    val origCopy = loaded.copy(Bitmap.Config.ARGB_8888, false)
                    withContext(Dispatchers.Main) {
                        originalBitmap = origCopy
                        editingBitmap = configCopy
                        undoStack.clear()
                        aiSuggestions = emptyList()
                        busyWorking = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, bitmapCheck.errorMessage ?: "Captured photo failed image integrity checks.", Toast.LENGTH_LONG).show()
                        busyWorking = false
                    }
                }
            }
        }
    }

    // Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val tempFile = java.io.File(context.cacheDir, "temp_camera_photo.jpg")
                if (tempFile.exists()) tempFile.delete()
                tempFile.createNewFile()
                val authority = "${context.packageName}.fileprovider"
                val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, tempFile)
                tempPhotoUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to start camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    // Capture flow initiator
    val initiateCameraCapture = {
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val tempFile = java.io.File(context.cacheDir, "temp_camera_photo.jpg")
                if (tempFile.exists()) tempFile.delete()
                tempFile.createNewFile()
                val authority = "${context.packageName}.fileprovider"
                val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, tempFile)
                tempPhotoUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to start camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // App Branded Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AsyncImage(
                    model = R.drawable.stickme_logo_1781048421456,
                    contentDescription = "Stick Me Logo",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFFF1F2D), RoundedCornerShape(10.dp))
                )
                Column {
                    Row {
                        Text(
                            text = "STICK",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "ME",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFF1F2D),
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                    Text(
                        text = "TURN MOMENTS INTO STICKERS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = Color.Gray
                    )
                }
            }

            // Tab bar
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF16161A))
                    .padding(2.dp)
            ) {
                Text(
                    text = "STUDIO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activeTab == 0) Color.White else Color.Gray,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeTab == 0) currentAccentColor else Color.Transparent)
                        .clickable { activeTab = 0 }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
                Text(
                    text = "MY PACK",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activeTab == 1) Color.White else Color.Gray,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeTab == 1) currentAccentColor else Color.Transparent)
                        .clickable { activeTab = 1 }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
                Text(
                    text = "KEYBOARD",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activeTab == 2) Color.White else Color.Gray,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeTab == 2) currentAccentColor else Color.Transparent)
                        .clickable { activeTab = 2 }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
                Text(
                    text = "PROFILE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activeTab == 3) Color.White else Color.Gray,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeTab == 3) currentAccentColor else Color.Transparent)
                        .clickable { activeTab = 3 }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (activeTab == 0) {
            // STUDIO WORKSPACE

            // Dynamic interactive workflow progress HUD
            val currentProgressStep = if (editingBitmap == null) {
                1
            } else if (captionText.isNotEmpty() || outlineSize > 8 || showShadow) {
                3
            } else {
                2
            }
            CreationWorkflowStepper(
                editingBitmap = editingBitmap,
                currentStep = currentProgressStep,
                onAction = { action ->
                    when (action) {
                        "SOURCE" -> { showPhotoSourceChooser = true }
                        "CUTOUT" -> {
                            android.widget.Toast.makeText(
                                context,
                                "✂️ Use Magic Tap to instantly isolate your subject, or Manual Erase/Restore brush to refine details!",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                        "STYLIZE" -> {
                            android.widget.Toast.makeText(
                                context,
                                "🎨 Customize outline stroke width, back shadow depth, and enter fun sticker captions to make it pop!",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                        "NAV_KEYBOARD" -> { activeTab = 2 }
                    }
                }
            )

            // Sticker preview stage card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .border(1.dp, Color(0xFF2C2C33), RoundedCornerShape(20.dp))
                    .testTag("sticker_stage"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16161A))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Stage Background Renderer
                    when (backgroundTheme) {
                        "checker" -> {
                            val tileSize = 16.dp
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val sizePx = tileSize.toPx()
                                val horCells = (size.width / sizePx).toInt() + 1
                                val verCells = (size.height / sizePx).toInt() + 1
                                for (i in 0..horCells) {
                                    for (j in 0..verCells) {
                                        val color = if ((i + j) % 2 == 0) Color(0xFF1C1C21) else Color(0xFF101014)
                                        drawRect(
                                            color = color,
                                            topLeft = androidx.compose.ui.geometry.Offset(i * sizePx, j * sizePx),
                                            size = androidx.compose.ui.geometry.Size(sizePx, sizePx)
                                        )
                                    }
                                }
                            }
                        }
                        "dark" -> {
                            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1B2A)))
                        }
                        "light" -> {
                            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFEEF0F2)))
                        }
                        "chat" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF075E54)) // Green gradient placeholder
                            )
                        }
                    }

                    if (editingBitmap == null) {
                        // Empty State Placeholder
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            AsyncImage(
                                model = R.drawable.stickme_logo_1781048421456,
                                contentDescription = "Logo Empty State",
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.5.dp, Color(0xFFFF1F2D).copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "ADD A PHOTO",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "Choose a picture of a kid, pet, car — any subject.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { showPhotoSourceChooser = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                                colors = listOf(Color(0xFFFF2535), Color(0xFFB00610))
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .testTag("choose_photo_button")
                                ) {
                                    Text("📸 Choose Photo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { showCameraSourceChooser = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                                colors = listOf(Color(0xFF333333), Color(0xFF111111))
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .testTag("take_photo_button")
                                ) {
                                    Text("📷 Take Photo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .border(1.dp, Color(0xFF2C2C33), RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF101014))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF7b2ff7).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("✨", fontSize = 18.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Automatic BG Remover Active",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Our neural engine isolates people, pets, or objects to create transparent stickers instantly.",
                                            fontSize = 10.sp,
                                            color = Color.Gray,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Live Editor Interactive Canvas
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val containerWidth = constraints.maxWidth.toFloat()
                            val containerHeight = constraints.maxHeight.toFloat()

                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(activeTool, brushSize, editingBitmap, updateTrigger) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                val b = editingBitmap ?: return@detectDragGestures
                                                // Store state snapshot for Undo support
                                                val snapshot = b.copy(Bitmap.Config.ARGB_8888, true)
                                                undoStack.add(snapshot)
                                                preFilterBitmap = null
                                                touchPos = offset

                                                // Map viewport touch to pixel coordinates
                                                val ratio = Math.min(containerWidth / b.width, containerHeight / b.height)
                                                val scaleX = containerWidth / b.width
                                                val scaleY = containerHeight / b.height

                                                val leftOffset = (containerWidth - b.width * ratio) / 2
                                                val topOffset = (containerHeight - b.height * ratio) / 2

                                                val x = (offset.x - leftOffset) / ratio
                                                val y = (offset.y - topOffset) / ratio

                                                if (x >= 0 && x < b.width && y >= 0 && y < b.height) {
                                                    if (activeTool == "magic") {
                                                        // Run high-efficiency BFS paint color cutoff
                                                        busyWorking = true
                                                        scope.launch(Dispatchers.Default) {
                                                            findAndEraseSimilarPixels(b, x.toInt(), y.toInt(), magicStrength)
                                                            withContext(Dispatchers.Main) {
                                                                updateTrigger++
                                                                busyWorking = false
                                                            }
                                                        }
                                                    } else {
                                                        val finalBrushRadius = brushSize * (b.width.toFloat() / containerWidth)
                                                        performBrushStroke(b, originalBitmap ?: b, x, y, x, y, activeTool, finalBrushRadius)
                                                        updateTrigger++
                                                    }
                                                }
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                val b = editingBitmap ?: return@detectDragGestures
                                                preFilterBitmap = null
                                                touchPos = change.position

                                                if (activeTool != "magic") {
                                                    // Map viewport drag to pixel coordinates
                                                    val ratio = Math.min(containerWidth / b.width, containerHeight / b.height)
                                                    val leftOffset = (containerWidth - b.width * ratio) / 2
                                                    val topOffset = (containerHeight - b.height * ratio) / 2

                                                    val px = (change.position.x - leftOffset) / ratio
                                                    val py = (change.position.y - topOffset) / ratio

                                                    val prevPx = (change.previousPosition.x - leftOffset) / ratio
                                                    val prevPy = (change.previousPosition.y - topOffset) / ratio

                                                    val finalBrushRadius = brushSize * (b.width.toFloat() / containerWidth)
                                                    performBrushStroke(b, originalBitmap ?: b, prevPx, prevPy, px, py, activeTool, finalBrushRadius)
                                                    updateTrigger++
                                                }
                                            },
                                            onDragEnd = {
                                                touchPos = null
                                            },
                                            onDragCancel = {
                                                touchPos = null
                                            }
                                        )
                                    }
                            ) {
                                val b = editingBitmap ?: return@Canvas
                                // Resolve scaled offsets
                                val ratio = Math.min(size.width / b.width, size.height / b.height)
                                val dstWidth = b.width * ratio
                                val dstHeight = b.height * ratio

                                val left = (size.width - dstWidth) / 2
                                val top = (size.height - dstHeight) / 2

                                drawIntoCanvas { canvas ->
                                    val nativeCanvas = canvas.nativeCanvas
                                    val destRect = RectF(left, top, left + dstWidth, top + dstHeight)

                                    // 1. Render Drop Shadow
                                    if (showShadow) {
                                        val shadowPaint = Paint().apply {
                                            colorFilter = PorterDuffColorFilter(
                                                android.graphics.Color.argb(120, 0, 0, 0),
                                                PorterDuff.Mode.SRC_IN
                                            )
                                            isAntiAlias = true
                                        }
                                        val shadowShift = 8f * ratio
                                        val shadowDst = RectF(left, top + shadowShift, left + dstWidth, top + dstHeight + shadowShift)
                                        nativeCanvas.drawBitmap(b, null, shadowDst, shadowPaint)
                                    }

                                    // 2. Render Scaled Border Outline
                                    if (outlineSize > 0 && outlineColor != Color.Transparent) {
                                        val outlinePaint = Paint().apply {
                                            colorFilter = PorterDuffColorFilter(
                                                outlineColor.toArgb(),
                                                PorterDuff.Mode.SRC_IN
                                            )
                                            isAntiAlias = true
                                        }
                                        val outlinePxScaled = outlineSize * ratio
                                        for (i in 0 until 16) {
                                            val angle = (i * 2 * Math.PI) / 16
                                            val dx = (cos(angle) * outlinePxScaled).toFloat()
                                            val dy = (sin(angle) * outlinePxScaled).toFloat()

                                            val borderDst = RectF(
                                                left + dx,
                                                top + dy,
                                                left + dstWidth + dx,
                                                top + dstHeight + dy
                                            )
                                            nativeCanvas.drawBitmap(b, null, borderDst, outlinePaint)
                                        }
                                    }

                                    // 3. Render Sticker Image
                                    nativeCanvas.drawBitmap(b, null, destRect, null)

                                    // 4. Render Meme Header/Footer text
                                    if (captionText.trim().isNotEmpty()) {
                                        val fontHeight = dstHeight * 0.11f
                                        val textPaint = android.graphics.Paint().apply {
                                            textSize = fontHeight
                                            textAlign = android.graphics.Paint.Align.CENTER
                                            typeface = android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.BOLD)
                                            isAntiAlias = true
                                        }

                                        val textX = left + dstWidth / 2f
                                        val textY = top + dstHeight - fontHeight * 0.6f

                                        // Outline (Stroke)
                                        textPaint.style = android.graphics.Paint.Style.STROKE
                                        textPaint.strokeWidth = fontHeight * 0.22f
                                        textPaint.color = android.graphics.Color.BLACK
                                        nativeCanvas.drawText(captionText.uppercase(), textX, textY, textPaint)

                                        // Fill
                                        textPaint.style = android.graphics.Paint.Style.FILL
                                        textPaint.color = android.graphics.Color.WHITE
                                        nativeCanvas.drawText(captionText.uppercase(), textX, textY, textPaint)
                                    }
                                }
                            }

                            // Magnificent floating magnifier Loupe
                            if ((activeTool == "erase" || activeTool == "restore") && touchPos != null && editingBitmap != null) {
                                val tp = touchPos!!
                                val loupeSizeDp = 130.dp
                                val scaleFactor = 2.0f
                                val offsetAboveDp = (-70).dp

                                Box(
                                    modifier = Modifier
                                        .size(loupeSizeDp)
                                        .offset(
                                            x = with(androidx.compose.ui.platform.LocalDensity.current) { (tp.x - (130 / 2).dp.toPx()).toDp() },
                                            y = with(androidx.compose.ui.platform.LocalDensity.current) { (tp.y - (130 / 2).dp.toPx()).toDp() + offsetAboveDp }
                                        )
                                        .clip(CircleShape)
                                        .border(3.dp, Color.White, CircleShape)
                                        .border(4.dp, Color(0xFFFF1F2D).copy(alpha = 0.4f), CircleShape)
                                        .background(Color(0xFF101014))
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val b = editingBitmap ?: return@Canvas
                                        val ratio = Math.min(containerWidth / b.width, containerHeight / b.height)
                                        val dstWidth = b.width * ratio
                                        val dstHeight = b.height * ratio
                                        val left = (containerWidth - dstWidth) / 2
                                        val top = (containerHeight - dstHeight) / 2

                                        drawIntoCanvas { canvas ->
                                            val nativeCanvas = canvas.nativeCanvas
                                            nativeCanvas.save()

                                            // 1. Center around loupe center (W/2, H/2)
                                            nativeCanvas.translate(size.width / 2f, size.height / 2f)
                                            // 2. Scale up
                                            nativeCanvas.scale(scaleFactor, scaleFactor)
                                            // 3. Shift negative of touch coordinates
                                            nativeCanvas.translate(-tp.x, -tp.y)

                                            // Now we draw the exact same view as the canvas!
                                            // 1. Render Drop Shadow
                                            if (showShadow) {
                                                val shadowPaint = Paint().apply {
                                                    colorFilter = PorterDuffColorFilter(
                                                        android.graphics.Color.argb(120, 0, 0, 0),
                                                        PorterDuff.Mode.SRC_IN
                                                    )
                                                    isAntiAlias = true
                                                }
                                                val shadowShift = 8f * ratio
                                                val shadowDst = RectF(left, top + shadowShift, left + dstWidth, top + dstHeight + shadowShift)
                                                nativeCanvas.drawBitmap(b, null, shadowDst, shadowPaint)
                                            }

                                            // 2. Render Scaled Border Outline
                                            if (outlineSize > 0 && outlineColor != Color.Transparent) {
                                                val outlinePaint = Paint().apply {
                                                    colorFilter = PorterDuffColorFilter(
                                                        outlineColor.toArgb(),
                                                        PorterDuff.Mode.SRC_IN
                                                    )
                                                    isAntiAlias = true
                                                }
                                                val outlinePxScaled = outlineSize * ratio
                                                for (i in 0 until 16) {
                                                    val angle = (i * 2 * Math.PI) / 16
                                                    val dx = (cos(angle) * outlinePxScaled).toFloat()
                                                    val dy = (sin(angle) * outlinePxScaled).toFloat()

                                                    val borderDst = RectF(
                                                        left + dx,
                                                        top + dy,
                                                        left + dstWidth + dx,
                                                        top + dstHeight + dy
                                                    )
                                                    nativeCanvas.drawBitmap(b, null, borderDst, outlinePaint)
                                                }
                                            }

                                            // 3. Render Sticker Image
                                            val destRect = RectF(left, top, left + dstWidth, top + dstHeight)
                                            nativeCanvas.drawBitmap(b, null, destRect, null)

                                            nativeCanvas.restore()
                                        }

                                        // Draw a central tiny red target crosshair inside the glass for exact pixel feedback
                                        drawCircle(Color(0xFFFF1F2D), radius = 3.dp.toPx())
                                    }
                                }
                            }
                        }
                    }

                    // Loading overlay
                    if (busyWorking) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color(0xFFFF1F2D))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("WORKING...", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            }
                        }
                    }
                }
            }

            // Stage Background Swatches
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STAGING BG:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Gray,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(end = 6.dp)
                )

                listOf(
                    Triple("checker", "None", Color(0xFF1C1C21)),
                    Triple("dark", "Dark", Color(0xFF0D1B2A)),
                    Triple("light", "Light", Color(0xFFEEF0F2)),
                    Triple("chat", "Chat", Color(0xFF128C7E))
                ).forEach { (key, label, color) ->
                    val isSelected = backgroundTheme == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF211012) else Color(0xFF16161A))
                            .border(
                                width = 1.6.dp,
                                color = if (isSelected) Color(0xFFFF1F2D) else Color(0xFF2C2C33),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { backgroundTheme = key }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (key == "checker") {
                                // Draw a mini checkerboard box representation
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .border(0.5.dp, Color.Gray, RoundedCornerShape(2.dp))
                                        .background(Color.Gray)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .align(Alignment.TopStart)
                                            .background(Color.White)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .align(Alignment.BottomEnd)
                                            .background(Color.White)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(0.5.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color.Gray,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // SECTION 1: CUT OUT THE STICKER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF16161A), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF2C2C33), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF1F2D)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("1", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CUT IT OUT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Gray,
                        letterSpacing = 1.5.sp
                    )
                }

                // Brush Tools selectors
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().alpha(if (editingBitmap != null) 1f else 0.4f)
                ) {
                    listOf(
                        Triple("magic", "✨ Magic Tap", "t_magic_button"),
                        Triple("erase", "🧽 Manual Erase", "t_erase_button"),
                        Triple("restore", "↩︎ Restore", "t_restore_button")
                    ).forEach { (id, label, tag) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (activeTool == id) Color(0xFF2A1115) else Color(0xFF1C1C21))
                                .border(
                                    width = 1.dp,
                                    color = if (activeTool == id) Color(0xFFFF1F2D) else Color(0xFF2C2C33),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable(enabled = editingBitmap != null) { activeTool = id }
                                .padding(vertical = 11.dp)
                                .testTag(tag),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (activeTool == id) Color.White else Color.Gray,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Brush size / strength sliders
                if (activeTool == "magic") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("MAGIC STRENGTH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text(text = "$magicStrength", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Slider(
                        value = magicStrength.toFloat(),
                        onValueChange = { magicStrength = it.toInt() },
                        valueRange = 6f..120f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFF1F2D),
                            activeTrackColor = Color(0xFFFF1F2D),
                            inactiveTrackColor = Color(0xFF2C2C33)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("magic_strength_slider")
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("BRUSH RADIUS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text(text = "${brushSize.toInt()}px", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Slider(
                        value = brushSize,
                        onValueChange = { brushSize = it },
                        valueRange = 8f..120f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFF1F2D),
                            activeTrackColor = Color(0xFFFF1F2D),
                            inactiveTrackColor = Color(0xFF2C2C33)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("brush_radius_slider")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // AI Subject Extraction Button
                val aiGradient = if (editingBitmap != null) {
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(Color(0xFF7b2ff7), Color(0xFFf107a3))
                    )
                } else {
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(Color(0xFF33333A), Color(0xFF33333A))
                    )
                }

                Button(
                    onClick = {
                        val b = editingBitmap ?: return@Button
                        busyWorking = true
                        undoStack.add(b.copy(Bitmap.Config.ARGB_8888, true))
                        val inputImage = com.google.mlkit.vision.common.InputImage.fromBitmap(b, 0)
                        val options = com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions.Builder()
                            .enableForegroundBitmap()
                            .build()
                        val segmenter = com.google.mlkit.vision.segmentation.subject.SubjectSegmentation.getClient(options)
                        segmenter.process(inputImage)
                            .addOnSuccessListener { result ->
                                val fg = result.foregroundBitmap
                                if (fg != null) {
                                    editingBitmap = fg.copy(Bitmap.Config.ARGB_8888, true)
                                    updateTrigger++
                                } else {
                                    // Fallback to legacy clean if model returned null
                                    scope.launch(Dispatchers.Default) {
                                        autoCleanBorders(b, magicStrength)
                                        withContext(Dispatchers.Main) {
                                            updateTrigger++
                                        }
                                    }
                                }
                                busyWorking = false
                            }
                            .addOnFailureListener { e ->
                                android.util.Log.e("MainActivity", "AI background removal failed", e)
                                scope.launch(Dispatchers.Default) {
                                    autoCleanBorders(b, magicStrength)
                                    withContext(Dispatchers.Main) {
                                        updateTrigger++
                                        busyWorking = false
                                    }
                                }
                            }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        disabledContentColor = Color.Gray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(aiGradient, RoundedCornerShape(12.dp))
                        .testTag("ai_auto_cut_button"),
                    enabled = editingBitmap != null,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("✨ AI REMOVE BACKGROUND", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Automatically isolates the foreground subject and makes the background transparent.",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Sub action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Auto Clean Borders Button
                    Button(
                        onClick = {
                            val b = editingBitmap ?: return@Button
                            busyWorking = true
                            // Save current to Undo
                            undoStack.add(b.copy(Bitmap.Config.ARGB_8888, true))
                            scope.launch(Dispatchers.Default) {
                                autoCleanBorders(b, magicStrength)
                                withContext(Dispatchers.Main) {
                                    updateTrigger++
                                    busyWorking = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C21)),
                        modifier = Modifier.weight(1.5f).height(44.dp).testTag("auto_clean_button"),
                        enabled = editingBitmap != null,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("🪄 AUTO CLEAN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Undo Button
                    Button(
                        onClick = {
                            if (undoStack.isNotEmpty()) {
                                val popped = undoStack.removeAt(undoStack.lastIndex)
                                editingBitmap = popped
                                updateTrigger++
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C21)),
                        modifier = Modifier.weight(1f).height(44.dp).testTag("undo_button"),
                        enabled = undoStack.isNotEmpty(),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("UNDO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Revert All Button
                    Button(
                        onClick = {
                            originalBitmap?.let {
                                val b = editingBitmap ?: return@let
                                undoStack.add(b.copy(Bitmap.Config.ARGB_8888, true))
                                editingBitmap = it.copy(Bitmap.Config.ARGB_8888, true)
                                updateTrigger++
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C21)),
                        modifier = Modifier.weight(1f).height(44.dp).testTag("reset_button"),
                        enabled = originalBitmap != null,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RESET", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SECTION 2: MAKE IT POP
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF16161A), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF2C2C33), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF1F2D)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("2", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MAKE IT POP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Gray,
                        letterSpacing = 1.5.sp
                    )
                }

                // Outline Thickness Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("STICKER OUTLINE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text(text = "${outlineSize}px", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Slider(
                    value = outlineSize.toFloat(),
                    onValueChange = { outlineSize = it.toInt() },
                    valueRange = 0f..26f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFF1F2D),
                        activeTrackColor = Color(0xFFFF1F2D),
                        inactiveTrackColor = Color(0xFF2C2C33)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = editingBitmap != null
                )

                // Outline color Swatches
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val outlineColors = listOf(
                        Triple(Color.White, "White", "sw_white"),
                        Triple(Color.Black, "Black", "sw_black"),
                        Triple(Color(0xFFFF1F2D), "Neon Red", "sw_red"),
                        Triple(Color(0xFFFFFF00), "Neon Gold", "sw_yellow"),
                        Triple(Color(0xFF00FF00), "Glow Green", "sw_green"),
                        Triple(Color(0xFF00FFFF), "Neon Cyan", "sw_cyan"),
                        Triple(Color(0xFFFF00FF), "Magic Pink", "sw_pink"),
                        Triple(Color.Transparent, "No Border", "sw_none")
                    )
                    items(outlineColors) { (color, label, tag) ->
                        val isSelected = outlineColor == color
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF211012) else Color(0xFF1C1C21))
                                .border(
                                    width = 1.6.dp,
                                    color = if (isSelected) Color(0xFFFF1F2D) else Color(0xFF2C2C33),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable(enabled = editingBitmap != null) { outlineColor = color }
                                .padding(vertical = 10.dp, horizontal = 12.dp)
                                .testTag(tag),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (color == Color.Transparent) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, Color.Gray, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.5.dp)
                                                .background(Color(0xFFFF1F2D))
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) Color.White else Color.Gray,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF2C2C33), modifier = Modifier.padding(vertical = 4.dp))

                // Photo Presets Selection Section
                Text(
                    text = "AESTHETIC PHOTO PRESETS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
                )

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val filterPresets = listOf(
                        Pair("original", "Normal 🚫"),
                        Pair("grayscale", "Noir 🌚"),
                        Pair("sepia", "Sepia 🎞️"),
                        Pair("cyberpunk", "Cyber 🌌"),
                        Pair("warm", "Retro ☀️"),
                        Pair("cool", "Icy ❄️"),
                        Pair("invert", "Invert 🎭")
                    )
                    items(filterPresets) { (id, label) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1C1C21))
                                .border(1.dp, Color(0xFF2C2C33), RoundedCornerShape(10.dp))
                                .clickable(enabled = editingBitmap != null) {
                                    val currentB = editingBitmap ?: return@clickable
                                    busyWorking = true
                                    scope.launch(Dispatchers.Default) {
                                        // Save pre-filter undo state of the bitmap to undo stack
                                        undoStack.add(currentB.copy(Bitmap.Config.ARGB_8888, true))
                                        
                                        // Take snapshot for non-compounding filter application if null
                                        if (preFilterBitmap == null) {
                                            preFilterBitmap = currentB.copy(Bitmap.Config.ARGB_8888, true)
                                        }
                                        
                                        // Apply filter cleanly on top of the pre-filter base
                                        val filterBase = preFilterBitmap ?: currentB
                                        val filteredResult = applyFilterToBitmap(filterBase, id)
                                        
                                        withContext(Dispatchers.Main) {
                                            editingBitmap = filteredResult
                                            updateTrigger++
                                            busyWorking = false
                                        }
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.LightGray
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF2C2C33), modifier = Modifier.padding(vertical = 4.dp))

                // Drop Shadow Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("DROP SHADOW", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("Add real sticker 3D sense depth", fontSize = 9.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = showShadow,
                        onCheckedChange = { showShadow = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFFF1F2D),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF1C1C21)
                        ),
                        enabled = editingBitmap != null
                    )
                }

                HorizontalDivider(color = Color(0xFF2C2C33), modifier = Modifier.padding(vertical = 4.dp))

                // Optional caption text input
                Text(
                    text = "ADD WORDS (CAPTION)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
                )

                OutlinedTextField(
                    value = captionText,
                    onValueChange = { captionText = it },
                    placeholder = { Text("E.g. BEST BOY!", color = Color.Gray, fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1C1C21),
                        unfocusedContainerColor = Color(0xFF1C1C21),
                        focusedBorderColor = Color(0xFFFF1F2D),
                        unfocusedBorderColor = Color(0xFF2C2C33)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("caption_input_field"),
                    enabled = editingBitmap != null,
                    trailingIcon = {
                        if (captionText.isNotEmpty()) {
                            IconButton(onClick = { captionText = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = Color.Gray)
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Choose label / category
                Text(
                    text = "CATEGORIZE STICKER",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                val categoriesList = listOf("funny", "pets", "custom", "uncategorized")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categoriesList.forEach { cat ->
                        val isSelected = creationCategoryTarget == cat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFFF1F2D).copy(alpha = 0.15f) else Color(0xFF1C1C21))
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFFFF1F2D) else Color(0xFF2C2C33),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable(enabled = editingBitmap != null) { creationCategoryTarget = cat }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Optional Tags input
                Text(
                    text = "TAGS (COMMA SEPARATED)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = creationTagsInput,
                    onValueChange = { creationTagsInput = it },
                    placeholder = { Text("E.g. meme, dog, silly", color = Color.Gray, fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1C1C21),
                        unfocusedContainerColor = Color(0xFF1C1C21),
                        focusedBorderColor = Color(0xFFFF1F2D),
                        unfocusedBorderColor = Color(0xFF2C2C33)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("tags_input_field"),
                    enabled = editingBitmap != null,
                    trailingIcon = {
                        if (creationTagsInput.isNotEmpty()) {
                            IconButton(onClick = { creationTagsInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = Color.Gray)
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive AI Caption suggestion
                Button(
                    onClick = {
                        val bitmap = editingBitmap ?: return@Button
                        loadingSuggestions = true
                        scope.launch {
                            val results = GeminiClient.getCaptionsForImage(bitmap)
                            aiSuggestions = results
                            loadingSuggestions = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F1235)),
                    modifier = Modifier.fillMaxWidth().testTag("ai_caption_button"),
                    enabled = editingBitmap != null && !loadingSuggestions,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (loadingSuggestions) {
                        CircularProgressIndicator(color = Color(0xFFFF1F2D), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ANALYZING WITH AI...", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Text("✨ ASK AI FOR WITTY CAPTIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFE3E5))
                    }
                }

                // AI captions list
                if (aiSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "TAP TO APPLY CAPTION:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF1F2D)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(aiSuggestions) { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF221F29))
                                    .border(1.dp, Color(0xFF3F3555), RoundedCornerShape(8.dp))
                                    .clickable { captionText = tag }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tag,
                                    color = Color(0xFFFFC0C3),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            val saveGradient = if (editingBitmap != null) {
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFFFF2535), Color(0xFFB00610))
                )
            } else {
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFF33333A), Color(0xFF33333A))
                )
            }

            // CTA SAVE BUTTON
            Button(
                onClick = {
                    val eb = editingBitmap ?: return@Button
                    busyWorking = true
                    scope.launch {
                        exportAndSaveSticker(
                            context = context,
                            editedBitmap = eb,
                            outlinePx = outlineSize.toFloat(),
                            outlineColor = outlineColor,
                            showShadow = showShadow,
                            caption = captionText,
                            database = database,
                            onSuccess = { file ->
                                scope.launch(Dispatchers.IO) {
                                    val entity = StickerEntity(
                                        filePath = file.absolutePath,
                                        caption = captionText
                                    )
                                    database.stickerDao().insertSticker(entity)

                                    // Create unique IndexedDB Object Keypath
                                    val stickerId = "sticker_${System.currentTimeMillis()}"
                                    try {
                                        withContext(Dispatchers.Main) {
                                            addLog("TX: Initiating READWRITE transaction to objectStore 'stickers'...")
                                        }
                                        val tx = indexedDb.transaction("stickers", "readwrite") { status ->
                                            scope.launch(Dispatchers.Main) { addLog("TX Status: $status") }
                                        }
                                        val store = tx.objectStore("stickers")
                                        
                                        val json = org.json.JSONObject().apply {
                                            put("id", stickerId)
                                            put("filePath", file.absolutePath)
                                            put("caption", captionText)
                                            put("timestamp", System.currentTimeMillis())
                                            put("category", creationCategoryTarget)
                                            put("isFavorite", false)
                                            put("tags", creationTagsInput.trim())
                                        }
                                        store.put(stickerId, json.toString())
                                        tx.commit()
                                        
                                        withContext(Dispatchers.Main) {
                                            addLog("Doc Put Success: Stored sticker JSON under key '$stickerId'.")
                                            reloadFromIndexedDb()
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            addLog("IndexedDB put failed: ${e.message}")
                                        }
                                    }

                                    withContext(Dispatchers.Main) {
                                        busyWorking = false
                                        showSuccessDialog = true
                                    }
                                }
                            },
                            onFailure = { error ->
                                scope.launch(Dispatchers.Main) {
                                    busyWorking = false
                                    Toast.makeText(context, "Save aborted: $error", Toast.LENGTH_LONG).show()
                                    addLog("Aborted save: $error")
                                }
                            }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    disabledContentColor = Color.Gray
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .background(saveGradient, RoundedCornerShape(16.dp))
                    .testTag("save_sticker_button"),
                enabled = editingBitmap != null
            ) {
                Icon(Icons.Default.Done, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SAVE STICKER (PNG)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Saves a transparent PNG that you can paste straight into WhatsApp, Messages, or Instagram using the custom StickMe Keyboard.",
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        } else if (activeTab == 1) {
            // MY STICKERS / PACK VIEW WITH INDEXEDDB GALLERY CONFIG
            var searchQuery by remember { mutableStateOf("") }
            var selectedFilter by remember { mutableStateOf("All") } // "All", "Favorites"
            var showDbInspector by remember { mutableStateOf(false) }

            val filteredList = savedStickersList.filter {
                val matchesSearch = it.caption.contains(searchQuery, ignoreCase = true) || 
                                    it.tags.contains(searchQuery, ignoreCase = true) || 
                                    searchQuery.isEmpty()
                val matchesFilter = when (selectedFilter) {
                    "All" -> true
                    "Favorites" -> it.isFavorite
                    else -> {
                        it.category.equals(selectedFilter, ignoreCase = true) ||
                        it.tags.split(",").map { t -> t.trim().lowercase() }.contains(selectedFilter.lowercase())
                    }
                }
                matchesSearch && matchesFilter
            }

            // Interactive Title and Schema Inspector Console Toggle
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "YOUR STICKER BOOK",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showDbInspector = !showDbInspector }
                        .background(if (showDbInspector) Color(0xFFFF1F2D).copy(alpha = 0.2f) else Color(0xFF1E1E24))
                        .border(1.dp, if (showDbInspector) Color(0xFFFF1F2D) else Color(0xFF33333A), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "DB Inspector",
                        tint = if (showDbInspector) Color(0xFFFF1F2D) else Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "INDEXEDDB CONSOLE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showDbInspector) Color.White else Color.Gray
                    )
                }
            }

            // Live Web-style IndexedDB Inspector panel
            if (showDbInspector) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .border(1.dp, Color(0xFF33333A), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F12))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00FF66))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DATABASE active: [stickme_indexed_db]",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "ver: 1.0 (SQLite-backed)",
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "OBJECTSTORE: 'stickers' (keyPath: 'id') | records: ${savedStickersList.size}",
                            fontSize = 9.sp,
                            color = Color(0xFFFF1F2D),
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "LIVE TRANSACTION TIME-SERIES LOGS:",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(Color.Black, RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF222226), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                Column {
                                    indexedDbLogs.forEach { log ->
                                        Text(
                                            text = log,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (log.contains("Error") || log.contains("failed")) Color(0xFFFF334B) else Color(0xFFDCDCE0)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val tx = indexedDb.transaction("stickers", "readwrite") { status ->
                                                scope.launch(Dispatchers.Main) { addLog("TX Reset Status: $status") }
                                            }
                                            tx.objectStore("stickers").clear()
                                            tx.commit()
                                            
                                            // Wipe local cache stickers
                                            savedStickersList.forEach { File(it.filePath).delete() }
                                            database.openHelper.writableDatabase.delete("stickers", null, null)
                                            
                                            withContext(Dispatchers.Main) {
                                                addLog("TX: Database ObjectStore 'stickers' cleared completely.")
                                                reloadFromIndexedDb()
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                addLog("Clear error: ${e.message}")
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF221113)),
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("WIPE OBJECT STORE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF3B30))
                            }
                            
                            Button(
                                onClick = {
                                    addLog("System trigger: active db diagnostic test executed.")
                                    reloadFromIndexedDb()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E24)),
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("REFRESH DB CONTROLS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            // Search inputs
            SearchAndFilterBar(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                selectedFilter = selectedFilter,
                onFilterSelect = { selectedFilter = it }
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredList.isEmpty()) {
                if (savedStickersList.isEmpty()) {
                    StickerGalleryPlaceholder(
                        onNavigateToStudio = { activeTab = 0 }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "No Matches Logo",
                                tint = Color.Gray,
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = "NO MATCHES FOUND",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Try adjusting your search query or switching from '${selectedFilter.uppercase()}' to see all saved stickers.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Button(
                                onClick = {
                                    searchQuery = ""
                                    selectedFilter = "All"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E24)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Reset Filters", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(430.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clickable { stickerToEdit = item },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF16161A)),
                            border = BorderStroke(1.dp, Color(0xFF2C2C33))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(6.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        AsyncImage(
                                            model = File(item.filePath),
                                            contentDescription = item.caption,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )

                                        if (item.category.isNotEmpty() && item.category != "uncategorized") {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .background(Color(0xFFFF1F2D).copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = item.category.uppercase(),
                                                    fontSize = 7.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.White
                                                )
                                            }
                                        }

                                        if (item.isFavorite) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = "Starred Favorite",
                                                tint = Color(0xFFFF1F2D),
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(16.dp)
                                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                                    .padding(2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.caption.ifEmpty { "STICKER" },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                                        )

                                        IconButton(
                                            onClick = {
                                                scope.launch(Dispatchers.IO) {
                                                    val nextFav = !item.isFavorite
                                                    try {
                                                        withContext(Dispatchers.Main) {
                                                            addLog("TX: Requesting WRITE transaction to favorite key '${item.id}' -> $nextFav")
                                                        }
                                                        val tx = indexedDb.transaction("stickers", "readwrite") { status ->
                                                            scope.launch(Dispatchers.Main) { addLog("TX Status: $status") }
                                                        }
                                                        val store = tx.objectStore("stickers")
                                                        val json = org.json.JSONObject().apply {
                                                            put("id", item.id)
                                                            put("filePath", item.filePath)
                                                            put("caption", item.caption)
                                                            put("timestamp", item.timestamp)
                                                            put("category", item.category)
                                                            put("isFavorite", nextFav)
                                                            put("tags", item.tags)
                                                        }
                                                        store.put(item.id, json.toString())
                                                        tx.commit()
                                                        withContext(Dispatchers.Main) {
                                                            reloadFromIndexedDb()
                                                        }
                                                    } catch (e: Exception) {
                                                        withContext(Dispatchers.Main) {
                                                            addLog("Fail to fav item: ${e.message}")
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = "Favorite Toggle",
                                                tint = if (item.isFavorite) Color(0xFFFF1F2D) else Color.Gray,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                scope.launch(Dispatchers.IO) {
                                                    try {
                                                        withContext(Dispatchers.Main) {
                                                            addLog("TX: Requesting transaction to DELETE sticker key '${item.id}'")
                                                        }
                                                        val tx = indexedDb.transaction("stickers", "readwrite") { status ->
                                                            scope.launch(Dispatchers.Main) { addLog("TX Status: $status") }
                                                        }
                                                        tx.objectStore("stickers").delete(item.id)
                                                        tx.commit()
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }

                                                    try {
                                                        database.openHelper.writableDatabase.delete(
                                                            "stickers",
                                                            "filePath = ?",
                                                            arrayOf(item.filePath)
                                                        )
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }

                                                    try {
                                                        File(item.filePath).delete()
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }

                                                    withContext(Dispatchers.Main) {
                                                        reloadFromIndexedDb()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }


        } else if (activeTab == 2) {
            KeyboardSetupScreen()
        } else {
            ProfileScreen(
                currentStickersCount = savedStickersList.size,
                favoriteStickersCount = savedStickersList.count { it.isFavorite },
                savedStickers = savedStickersList
            )
        }

        // FOOTER DETAILS
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "STICK ME · CREATE → SAVE → SHARE · CREATED BY TOPSHELFNZ",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }

    // SUCCESS SAVING DIALOG
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = {
                Text(
                    text = "🎉 STICKER SAVED!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "This sticker cutout has been successfully compiled and stored in your custom pack.\n\nTo start sending this sticker in messages, enable the 'StickMe' keyboard extension in system language settings!",
                    fontSize = 13.sp,
                    color = Color.LightGray,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        val intent = Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS)
                        activityContext.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1F2D))
                ) {
                    Text("Go to Keyboard Settings", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("Not Now", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF16161A),
            titleContentColor = Color.White,
            textContentColor = Color.LightGray
        )
    }

    // EDIT STICKER DIALOG
    if (stickerToEdit != null) {
        val editingItem = stickerToEdit!!
        var editCaption by remember(editingItem.id) { mutableStateOf(editingItem.caption) }
        var editCategory by remember(editingItem.id) { mutableStateOf(editingItem.category) }
        var editTags by remember(editingItem.id) { mutableStateOf(editingItem.tags) }

        AlertDialog(
            onDismissRequest = { stickerToEdit = null },
            title = {
                Text(
                    text = "EDIT STICKER LABELS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(Color(0xFF16161A), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = File(editingItem.filePath),
                            contentDescription = null,
                            modifier = Modifier.fillMaxHeight().padding(8.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Caption
                    Column {
                        Text("CAPTION / TITLE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = editCaption,
                            onValueChange = { editCaption = it },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF1C1C21),
                                unfocusedContainerColor = Color(0xFF1C1C21),
                                focusedBorderColor = Color(0xFFFF1F2D),
                                unfocusedBorderColor = Color(0xFF2C2C33)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("edit_caption_field")
                        )
                    }

                    // Category
                    Column {
                        Text("CATEGORY LABEL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(6.dp))
                        val labelOptions = listOf("funny", "pets", "custom", "uncategorized")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            labelOptions.forEach { cat ->
                                val isSelected = editCategory == cat
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFFF1F2D).copy(alpha = 0.15f) else Color(0xFF1C1C21))
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFFFF1F2D) else Color(0xFF2C2C33),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { editCategory = cat }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cat.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    // Tags
                    Column {
                        Text("TAGS (COMMA SEPARATED)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = editTags,
                            onValueChange = { editTags = it },
                            placeholder = { Text("e.g. meme, stickers, cute", fontSize = 12.sp, color = Color.Gray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF1C1C21),
                                unfocusedContainerColor = Color(0xFF1C1C21),
                                focusedBorderColor = Color(0xFFFF1F2D),
                                unfocusedBorderColor = Color(0xFF2C2C33)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("edit_tags_field")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            try {
                                withContext(Dispatchers.Main) {
                                    addLog("TX: Starting WRITE transaction to EDIT sticker: ${editingItem.id}")
                                    busyWorking = true
                                }
                                val tx = indexedDb.transaction("stickers", "readwrite") { status ->
                                    scope.launch(Dispatchers.Main) { addLog("TX Status: $status") }
                                }
                                val store = tx.objectStore("stickers")
                                val json = org.json.JSONObject().apply {
                                    put("id", editingItem.id)
                                    put("filePath", editingItem.filePath)
                                    put("caption", editCaption)
                                    put("timestamp", editingItem.timestamp)
                                    put("category", editCategory)
                                    put("isFavorite", editingItem.isFavorite)
                                    put("tags", editTags.trim())
                                }
                                store.put(editingItem.id, json.toString())
                                tx.commit()
                                withContext(Dispatchers.Main) {
                                    addLog("IndexedDB Update Success: Modified label info of ${editingItem.id}")
                                    reloadFromIndexedDb()
                                    stickerToEdit = null
                                    busyWorking = false
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    addLog("Fail to edit item: ${e.message}")
                                    stickerToEdit = null
                                    busyWorking = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1F2D)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("SAVE CHANGES", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { stickerToEdit = null }) {
                    Text("CANCEL", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF101014),
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showPhotoSourceChooser) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceChooser = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📸", fontSize = 24.sp)
                    Text(
                        text = "CHOOSE PHOTO SOURCE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = "Select how you would like to load your sticker source image:",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )

                    // Card 1: System File picker (Direct, recommended)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoSourceChooser = false
                                imagePickerLauncher.launch("image/*")
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B22)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF1F2D).copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("📂", fontSize = 24.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "System Photo Picker",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "100% Reliable. Instantly uploads snapshots or existing digital art from any device directory.",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    // Card 2: Custom Local Album
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoSourceChooser = false
                                showInAppGallery = true
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B22)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C35)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("🖼️", fontSize = 24.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "App Photo Library Album",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Visual grid browser of your device MediaStore library. Requires storage access permission.",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoSourceChooser = false }) {
                    Text("CANCEL", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            containerColor = Color(0xFF101014),
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showCameraSourceChooser) {
        AlertDialog(
            onDismissRequest = { showCameraSourceChooser = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📷", fontSize = 24.sp)
                    Text(
                        text = "SELECT CAMERA SOURCE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = "Choose the camera mode to snap a new sticker photo:",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )

                    // Card 1: System Camera App (Recommended)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showCameraSourceChooser = false
                                initiateCameraCapture()
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B22)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF1F2D).copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("📱", fontSize = 24.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "System HDR Camera (No Failures)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Uses your device's native professional camera application with crisp auto-focus, zoom, and sensor optimization.",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    // Card 2: Custom Creator Viewport
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showCameraSourceChooser = false
                                showInAppCamera = true
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B22)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C35)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("⚡", fontSize = 24.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "In-App Creator Camera Guide",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Features an on-screen circular alignment grid to frame your sticker object precisely.",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCameraSourceChooser = false }) {
                    Text("CANCEL", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            containerColor = Color(0xFF101014),
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showInAppCamera) {
        CameraCaptureScreen(
            onDismiss = { showInAppCamera = false },
            onPhotoCaptured = { bitmap ->
                val bitmapCheck = com.example.utils.MediaValidator.validateBitmap(bitmap)
                if (bitmapCheck.isValid) {
                    val configCopy = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                    val origCopy = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                    originalBitmap = origCopy
                    editingBitmap = configCopy
                    undoStack.clear()
                    aiSuggestions = emptyList()
                    showInAppCamera = false
                } else {
                    Toast.makeText(context, bitmapCheck.errorMessage ?: "Captured photo failed validation checks.", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    if (showInAppGallery) {
        GalleryPickerScreen(
            onDismiss = { showInAppGallery = false },
            onPhotoSelected = { uri ->
                val uriCheck = com.example.utils.MediaValidator.validateUri(context, uri)
                if (!uriCheck.isValid) {
                    Toast.makeText(context, uriCheck.errorMessage ?: "Selected sticker gallery image is unreadable.", Toast.LENGTH_LONG).show()
                    return@GalleryPickerScreen
                }
                busyWorking = true
                scope.launch(Dispatchers.IO) {
                    val loaded = loadBitmapFromUri(context, uri)
                    val bitmapCheck = com.example.utils.MediaValidator.validateBitmap(loaded)
                    if (loaded != null && bitmapCheck.isValid) {
                        val configCopy = loaded.copy(Bitmap.Config.ARGB_8888, true)
                        val origCopy = loaded.copy(Bitmap.Config.ARGB_8888, false)
                        withContext(Dispatchers.Main) {
                            originalBitmap = origCopy
                            editingBitmap = configCopy
                            undoStack.clear()
                            aiSuggestions = emptyList()
                            busyWorking = false
                            showInAppGallery = false
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, bitmapCheck.errorMessage ?: "Loaded sticker library image failed checks.", Toast.LENGTH_LONG).show()
                            busyWorking = false
                            showInAppGallery = false
                        }
                    }
                }
            },
            onLaunchSystemPicker = {
                imagePickerLauncher.launch("image/*")
            }
        )
    }
}
}

// Loads bitmap safe from orientation and scales size keeping memory light
suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val original = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (original != null) {
            val maxDimension = 1000
            if (original.width > maxDimension || original.height > maxDimension) {
                val scale = maxDimension.toFloat() / Math.max(original.width, original.height)
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * scale).toInt(),
                    (original.height * scale).toInt(),
                    true
                )
            } else {
                original
            }
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Perform active manual brush editing (erase transparent or restore pixel values)
fun performBrushStroke(
    bitmap: Bitmap,
    original: Bitmap,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    tool: String,
    brushRadius: Float
) {
    val canvas = Canvas(bitmap)
    if (tool == "erase") {
        val paint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            style = Paint.Style.STROKE
            strokeWidth = brushRadius
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        canvas.drawLine(x1, y1, x2, y2, paint)
    } else if (tool == "restore") {
        val paint = Paint().apply {
            shader = BitmapShader(original, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            style = Paint.Style.STROKE
            strokeWidth = brushRadius
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        canvas.drawLine(x1, y1, x2, y2, paint)
    }
}

// Quick BFS floodfill cut out matching colors
fun findAndEraseSimilarPixels(
    bitmap: Bitmap,
    startX: Int,
    startY: Int,
    tolerance: Int
) {
    val w = bitmap.width
    val h = bitmap.height
    if (startX !in 0 until w || startY !in 0 until h) return

    val targetColor = bitmap.getPixel(startX, startY)
    if (ColorUtils.alpha(targetColor) == 0) return

    val targetR = ColorUtils.red(targetColor)
    val targetG = ColorUtils.green(targetColor)
    val targetB = ColorUtils.blue(targetColor)

    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

    val visited = BitSet(w * h)
    val queue = IntArray(w * h)
    var head = 0
    var tail = 0

    val startIndex = startY * w + startX
    queue[tail++] = startIndex
    visited.set(startIndex)

    val toleranceSq = tolerance * tolerance * 3

    while (head < tail) {
        val currIdx = queue[head++]
        val cx = currIdx % w
        val cy = currIdx / w

        pixels[currIdx] = 0 // Transparent

        val neighbors = arrayOf(
            cx - 1 to cy,
            cx + 1 to cy,
            cx to cy - 1,
            cx to cy + 1
        )

        for ((nx, ny) in neighbors) {
            if (nx in 0 until w && ny in 0 until h) {
                val nIdx = ny * w + nx
                if (!visited.get(nIdx)) {
                    val color = pixels[nIdx]
                    val alpha = (color shr 24) and 0xff
                    if (alpha != 0) {
                        val r = (color shr 16) and 0xff
                        val g = (color shr 8) and 0xff
                        val b = color and 0xff

                        val dr = r - targetR
                        val dg = g - targetG
                        val db = b - targetB

                        if (dr * dr + dg * dg + db * db <= toleranceSq) {
                            visited.set(nIdx)
                            queue[tail++] = nIdx
                        }
                    } else {
                        visited.set(nIdx)
                    }
                }
            }
        }
    }

    bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
}

// Auto clean background starting flood from all outer border pixels
fun autoCleanBorders(bitmap: Bitmap, tolerance: Int) {
    val w = bitmap.width
    val h = bitmap.height

    val step = Math.max(4, Math.floor(Math.min(w, h) / 40.0).toInt())

    // Seeds from top & bottom borders
    for (x in 0 until w step step) {
        findAndEraseSimilarPixels(bitmap, x, 0, tolerance)
        findAndEraseSimilarPixels(bitmap, x, h - 1, tolerance)
    }

    // Seeds from left & right borders
    for (y in 0 until h step step) {
        findAndEraseSimilarPixels(bitmap, 0, y, tolerance)
        findAndEraseSimilarPixels(bitmap, w - 1, y, tolerance)
    }

    // Corner seeds fallback
    findAndEraseSimilarPixels(bitmap, 0, 0, tolerance)
    findAndEraseSimilarPixels(bitmap, w - 1, 0, tolerance)
    findAndEraseSimilarPixels(bitmap, 0, h - 1, tolerance)
    findAndEraseSimilarPixels(bitmap, w - 1, h - 1, tolerance)
}

// Complete static rendering compile and local storage write
fun exportAndSaveSticker(
    context: Context,
    editedBitmap: Bitmap,
    outlinePx: Float,
    outlineColor: Color,
    showShadow: Boolean,
    caption: String,
    database: AppDatabase,
    onSuccess: (File) -> Unit,
    onFailure: (String) -> Unit
) {
    // 1. Pre-validation of input bitmap integrity & accessibility
    val bitmapCheck = com.example.utils.MediaValidator.validateBitmap(editedBitmap)
    if (!bitmapCheck.isValid) {
        onFailure(bitmapCheck.errorMessage ?: "Sticker image failed integrity checks.")
        return
    }

    // Compile sticker onto larger canvas with safe breathing padding
    val pad = (Math.max(editedBitmap.width, editedBitmap.height) * 0.08f).toInt()
    val finalW = editedBitmap.width + pad * 2
    val finalH = editedBitmap.height + pad * 2

    val result = Bitmap.createBitmap(finalW, finalH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)

    val left = pad.toFloat()
    val top = pad.toFloat()

    // 1. Draw outline blurred shadow
    if (showShadow) {
        val shadowPaint = Paint().apply {
            colorFilter = PorterDuffColorFilter(
                android.graphics.Color.argb(120, 0, 0, 0),
                PorterDuff.Mode.SRC_IN
            )
            isAntiAlias = true
        }
        val shadowY = pad * 0.22f
        canvas.drawBitmap(editedBitmap, left, top + shadowY, shadowPaint)
    }

    // 2. Draw solid sticker boundary outline
    if (outlinePx > 0 && outlineColor != Color.Transparent) {
        val outlinePaint = Paint().apply {
            colorFilter = PorterDuffColorFilter(
                outlineColor.toArgb(),
                PorterDuff.Mode.SRC_IN
            )
            isAntiAlias = true
        }
        for (i in 0 until 16) {
            val angle = (i * 2 * Math.PI) / 16
            val dx = (cos(angle) * outlinePx).toFloat()
            val dy = (sin(angle) * outlinePx).toFloat()
            canvas.drawBitmap(editedBitmap, left + dx, top + dy, outlinePaint)
        }
    }

    // 3. Draw foreground face
    canvas.drawBitmap(editedBitmap, left, top, null)

    // 4. Paint capitalized outline words string
    if (caption.trim().isNotEmpty()) {
        val fs = editedBitmap.height * 0.11f
        val textPaint = android.graphics.Paint().apply {
            textSize = fs
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.BOLD)
            isAntiAlias = true
        }

        val textX = left + editedBitmap.width / 2f
        val textY = top + editedBitmap.height - fs * 0.6f

        textPaint.style = android.graphics.Paint.Style.STROKE
        textPaint.strokeWidth = fs * 0.22f
        textPaint.color = android.graphics.Color.BLACK
        canvas.drawText(caption.uppercase(), textX, textY, textPaint)

        textPaint.style = android.graphics.Paint.Style.FILL
        textPaint.color = android.graphics.Color.WHITE
        canvas.drawText(caption.uppercase(), textX, textY, textPaint)
    }

    // Write PNG file structure to app local storage directory
    val folder = File(context.filesDir, "stickers")
    if (!folder.exists()) {
        folder.mkdirs()
    }
    val file = File(folder, "sticker_${System.currentTimeMillis()}.png")

    try {
        val stream = FileOutputStream(file)
        result.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.flush()
        stream.close()

        // 2. Post-serialization verification: check file integrity and size
        val fileCheck = com.example.utils.MediaValidator.validateStickerFile(file)
        if (fileCheck.isValid) {
            onSuccess(file)
        } else {
            // Remove corrupted file instantly to avoid dirty storage state
            if (file.exists()) {
                file.delete()
            }
            onFailure(fileCheck.errorMessage ?: "Sticker file failed post-render validation checks.")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        if (file.exists()) {
            file.delete()
        }
        onFailure("Failed writing image bytes to directory: ${e.message}")
    }
}

// Dynamic helper class for fast extraction of alpha values
object ColorUtils {
    fun alpha(color: Int): Int = (color shr 24) and 0xff
    fun red(color: Int): Int = (color shr 16) and 0xff
    fun green(color: Int): Int = (color shr 8) and 0xff
    fun blue(color: Int): Int = color and 0xff
}

fun applyFilterToBitmap(src: Bitmap, filterType: String): Bitmap {
    val dest = src.copy(Bitmap.Config.ARGB_8888, true)
    val width = dest.width
    val height = dest.height
    val pixels = IntArray(width * height)
    dest.getPixels(pixels, 0, width, 0, 0, width, height)

    for (i in pixels.indices) {
        val color = pixels[i]
        val a = (color shr 24) and 0xff
        if (a == 0) continue // Preserves absolute transparency

        val r = (color shr 16) and 0xff
        val g = (color shr 8) and 0xff
        val b = color and 0xff

        var newR = r
        var newG = g
        var newB = b

        when (filterType) {
            "grayscale" -> {
                val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)
                newR = gray
                newG = gray
                newB = gray
            }
            "sepia" -> {
                newR = (0.393 * r + 0.769 * g + 0.189 * b).toInt().coerceIn(0, 255)
                newG = (0.349 * r + 0.686 * g + 0.168 * b).toInt().coerceIn(0, 255)
                newB = (0.272 * r + 0.534 * g + 0.131 * b).toInt().coerceIn(0, 255)
            }
            "cyberpunk" -> {
                newR = (r * 1.25f).toInt().coerceIn(0, 255)
                newG = (g * 0.65f).toInt().coerceIn(0, 255)
                newB = (b * 1.45f).toInt().coerceIn(0, 255)
            }
            "warm" -> {
                newR = (r * 1.25f).toInt().coerceIn(0, 255)
                newG = (g * 1.05f).toInt().coerceIn(0, 255)
                newB = (b * 0.75f).toInt().coerceIn(0, 255)
            }
            "cool" -> {
                newR = (r * 0.75f).toInt().coerceIn(0, 255)
                newG = (g * 1.10f).toInt().coerceIn(0, 255)
                newB = (b * 1.35f).toInt().coerceIn(0, 255)
            }
            "invert" -> {
                newR = 255 - r
                newG = 255 - g
                newB = 255 - b
            }
        }
        pixels[i] = (a shl 24) or (newR shl 16) or (newG shl 8) or newB
    }

    dest.setPixels(pixels, 0, width, 0, 0, width, height)
    return dest
}

@Composable
fun SearchAndFilterBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedFilter: String,
    onFilterSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search stickers by caption or tag...", color = Color.Gray, fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray, modifier = Modifier.size(16.dp)) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.LightGray,
                focusedContainerColor = Color(0xFF16161A),
                unfocusedContainerColor = Color(0xFF16161A),
                focusedBorderColor = Color(0xFFFF1F2D),
                unfocusedBorderColor = Color(0xFF2C2C33)
            ),
            shape = RoundedCornerShape(10.dp)
        )

        // Filter chips Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val filters = listOf("All", "Favorites", "funny", "pets", "custom", "uncategorized")
            items(filters) { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onFilterSelect(filter) }
                        .background(if (isSelected) Color(0xFF211012) else Color(0xFF16161A))
                        .border(
                            width = 1.2.dp,
                            color = if (isSelected) Color(0xFFFF1F2D) else Color(0xFF2C2C33),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun CreationWorkflowStepper(
    editingBitmap: android.graphics.Bitmap?,
    currentStep: Int,
    onAction: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .border(1.dp, Color(0xFF22222A), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131317)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "⚡️ STICKER CREATION CYCLE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = when(currentStep) {
                        1 -> "STEP 1: SOURCING"
                        2 -> "STEP 2: SUBJECT CUTOUT"
                        3 -> "STEP 3: MAKE IT POP"
                        else -> "STEP 4: SAVED & READY"
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF1F2D)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val steps = listOf(
                    Triple(1, "Sourcing", "📸"),
                    Triple(2, "Cutout", "✂️"),
                    Triple(3, "Stylize", "🎨"),
                    Triple(4, "Ready!", "🚀")
                )

                steps.forEachIndexed { index, (stepNum, label, emoji) ->
                    val isActive = currentStep == stepNum
                    val isCompleted = currentStep > stepNum
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (stepNum == 1) onAction("SOURCE")
                                if (stepNum == 2) onAction("CUTOUT")
                                if (stepNum == 3) onAction("STYLIZE")
                                if (stepNum == 4) onAction("NAV_KEYBOARD")
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(
                                    if (isActive) Color(0xFFFF1F2D) 
                                    else if (isCompleted) Color(0xFF112B15) 
                                    else Color(0xFF16161A)
                                )
                                .border(
                                    width = 1.2.dp,
                                    color = if (isActive) Color(0xFFFF1F2D) 
                                            else if (isCompleted) Color(0xFF2E7D32) 
                                            else Color(0xFF2C2C33),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Text("✓", fontSize = 11.sp, color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                            } else {
                                Text(emoji, fontSize = 11.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = label,
                            fontSize = 9.sp,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isActive) Color.White else if (isCompleted) Color.Gray else Color.DarkGray
                        )
                    }

                    if (index < steps.size - 1) {
                        Box(
                            modifier = Modifier
                                .height(1.dp)
                                .weight(0.4f)
                                .background(
                                    if (currentStep > stepNum) Color(0xFF2E7D32) else Color(0xFF2C2C33)
                                )
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF18181F), RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "💡",
                        fontSize = 12.sp
                    )
                    Text(
                        text = when (currentStep) {
                            1 -> "Load a high-contrast portrait or photo of any subject (or snap one with Camera) to isolate it automatically!"
                            2 -> "Tap on the canvas to erase background instantly with Magic Tap, or use Manual Erase/Restore brush for fine-tuning."
                            3 -> "Configure an eye-catching Sticker Outline stroke, add a subtle back shadow overlay, and customize caption templates!"
                            else -> "Nice! Your transparent PNG sticker is saved to your custom offline pack and is ready to send via StickMe Keyboard!"
                        },
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

