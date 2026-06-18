package com.example.ui

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GalleryPhoto(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateAdded: Long,
    val size: Long
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryPickerScreen(
    onDismiss: () -> Unit,
    onPhotoSelected: (Uri) -> Unit,
    onLaunchSystemPicker: () -> Unit
) {
    val context = LocalContext.current

    // Determine correct storage permission key based on Android build version
    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isLoading by remember { mutableStateOf(false) }
    var rawPhotoList by remember { mutableStateOf<List<GalleryPhoto>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPhotoPreview by remember { mutableStateOf<GalleryPhoto?>(null) }

    // Request permissions launcher
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Storage access is required to view your device photos", Toast.LENGTH_LONG).show()
        }
    }

    // Query media items from content provider on background thread
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            isLoading = true
            withContext(Dispatchers.IO) {
                val photos = mutableListOf<GalleryPhoto>()
                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media.SIZE
                )
                
                // Sort by date added descending (newest first)
                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

                try {
                    context.contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        null,
                        null,
                        sortOrder
                    )?.use { cursor ->
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                        val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idColumn)
                            val name = cursor.getString(nameColumn) ?: "photo_$id.jpg"
                            val date = cursor.getLong(dateColumn)
                            val size = cursor.getLong(sizeColumn)
                            val uri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id
                            )
                            photos.add(GalleryPhoto(id, uri, name, date, size))
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GalleryPicker", "Error querying MediaStore", e)
                }

                withContext(Dispatchers.Main) {
                    rawPhotoList = photos
                    isLoading = false
                }
            }
        }
    }

    // Dynamic Client-side search filtering
    val filteredPhotos = remember(rawPhotoList, searchQuery) {
        if (searchQuery.trim().isEmpty()) {
            rawPhotoList
        } else {
            rawPhotoList.filter {
                it.displayName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0E))
            .testTag("gallery_picker_overlay")
    ) {
        if (!hasPermission) {
            // Permission Block State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF1F2D).copy(alpha = 0.12f))
                        .border(1.5.dp, Color(0xFFFF1F2D), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🖼️", fontSize = 36.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "ACCESS YOUR PHOTO LIBRARY",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.2.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Allow StickMe to search and list photos directly from your phone's memory to instantly convert family snapshots, cute pets, or sketch drawings into high-quality custom stickers!",
                    fontSize = 13.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { requestPermissionLauncher.launch(storagePermission) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF1F2D),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(48.dp)
                ) {
                    Text("ALLOW STORAGE ACCESS", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = {
                        onLaunchSystemPicker()
                        onDismiss()
                    }
                ) {
                    Text(
                        text = "📂 USE SYSTEM FILE SELECTOR INSTEAD",
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                TextButton(onClick = onDismiss) {
                    Text("BACK TO STUDIO", color = Color.DarkGray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        } else {
            // Main Interactive Gallery
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                // Top Custom Header Control Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF1E1E24), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Gallery",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "DEVICE PHOTO LIBRARY",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "${rawPhotoList.size} Photos Found • Select to edit",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }

                    // Floating/Inline file system backup button
                    IconButton(
                        onClick = {
                            onLaunchSystemPicker()
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFF1F2D).copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, Color(0xFFFF1F2D).copy(alpha = 0.4f), CircleShape)
                    ) {
                        Text("📂", fontSize = 16.sp)
                    }
                }

                // Smooth Search Filtering Input Row
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search photos by keyword...", color = Color.Gray, fontSize = 13.sp) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search icon", tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF141419),
                        unfocusedContainerColor = Color(0xFF141419),
                        focusedBorderColor = Color(0xFFFF1F2D),
                        unfocusedBorderColor = Color(0xFF2C2C34),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFFF1F2D))
                    }
                } else if (filteredPhotos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "📷",
                                fontSize = 48.sp
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty()) "NO MATCHING PHOTO FOUND" else "YOUR PHOTO LIBRARY IS EMPTY",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Try searching for a different image name or clear query." else "Take some snaps with your device camera or tap the folder icon at the top-right to pick documents.",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Beautiful fluid Grid list of custom photo items
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 96.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredPhotos, key = { it.id }) { photo ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF141418))
                                    .border(
                                        width = if (selectedPhotoPreview?.id == photo.id) 2.5.dp else 1.dp,
                                        color = if (selectedPhotoPreview?.id == photo.id) Color(0xFFFF1F2D) else Color(0xFF22222A),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            onPhotoSelected(photo.uri)
                                        },
                                        onLongClick = {
                                            selectedPhotoPreview = photo
                                        }
                                    )
                            ) {
                                AsyncImage(
                                    model = photo.uri,
                                    contentDescription = photo.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Overlay metadata banner on photos
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                            )
                                        )
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        text = photo.displayName,
                                        color = Color.LightGray,
                                        fontSize = 8.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Overlay dialog for long-press high-res preview inspection
        selectedPhotoPreview?.let { preview ->
            AlertDialog(
                onDismissRequest = { selectedPhotoPreview = null },
                confirmButton = {
                    Button(
                        onClick = {
                            onPhotoSelected(preview.uri)
                            selectedPhotoPreview = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1F2D))
                    ) {
                        Text("SELECT & MAKE STICKER")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedPhotoPreview = null }) {
                        Text("CLOSE", color = Color.Gray)
                    }
                },
                title = {
                    Text(
                        text = preview.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color.DarkGray, RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = preview.uri,
                                contentDescription = "Preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Display file details
                        val dateString = java.text.DateFormat.getDateTimeInstance()
                            .format(java.util.Date(preview.dateAdded * 1000))
                        val kbSize = preview.size / 1024
                        Text(
                            text = "Captured: $dateString\nFile size: $kbSize KB",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                containerColor = Color(0xFF121216),
                titleContentColor = Color.White,
                textContentColor = Color.LightGray
            )
        }
    }
}
