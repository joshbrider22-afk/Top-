package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun ProfileScreen(
    currentStickersCount: Int,
    favoriteStickersCount: Int,
    savedStickers: List<com.example.db.IndexedDbSticker> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // Preferences configuration
    val prefs = remember { context.getSharedPreferences("stickme_profile_prefs", Context.MODE_PRIVATE) }

    // Profile state
    var username by remember { mutableStateOf(prefs.getString("username", "StickerCreator") ?: "StickerCreator") }
    var bio by remember { mutableStateOf(prefs.getString("bio", "Sticker wizard creating awesome stickers with StickMe!") ?: "") }
    var avatarEmoji by remember { mutableStateOf(prefs.getString("avatar_emoji", "🤠") ?: "🤠") }
    var customAvatarPath by remember { mutableStateOf(prefs.getString("custom_avatar_path", "") ?: "") }

    // Settings state
    var hapticMode by remember { mutableStateOf(prefs.getString("haptic_mode", "Light") ?: "Light") }
    var qualityPreset by remember { mutableStateOf(prefs.getString("quality_preset", "HD PNG") ?: "HD PNG") }
    var autoSuggest by remember { mutableStateOf(prefs.getBoolean("auto_suggest", true)) }
    var themeAccent by remember { mutableStateOf(prefs.getString("theme_accent", "Crimson") ?: "Crimson") }

    // Active configuration indicators
    val isKeyboardActive = remember { isKeyboardEnabled(context) && isKeyboardSelected(context) }

    // Image picker launcher for custom avatar
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val avatarDir = File(context.filesDir, "avatars")
                        if (!avatarDir.exists()) avatarDir.mkdirs()
                        val destinationFile = File(avatarDir, "user_avatar_custom.png")
                        val outputStream = FileOutputStream(destinationFile)
                        inputStream.copyTo(outputStream)
                        inputStream.close()
                        outputStream.close()

                        customAvatarPath = destinationFile.absolutePath
                        prefs.edit().putString("custom_avatar_path", customAvatarPath).apply()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Toast feedback indicator helper
    var showSavedNotification by remember { mutableStateOf(false) }
    var savedNotificationText by remember { mutableStateOf("Profile and configuration updated successfully!") }

    LaunchedEffect(showSavedNotification) {
        if (showSavedNotification) {
            kotlinx.coroutines.delay(2000)
            showSavedNotification = false
        }
    }

    // Accent color mapper helper
    val accentColorValue = remember(themeAccent) {
        getAccentColor(themeAccent)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Floating Top-Of-Screen Toast for Saves
        AnimatedVisibility(
            visible = showSavedNotification,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1B4D20))
                    .border(1.dp, Color(0xFF4CAF50), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = savedNotificationText,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Section 1: Dynamic Avatar and Hero Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF141419), Color(0xFF09090C))
                    )
                )
                .border(1.dp, accentColorValue.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Circular Avatar Frame with beautiful Neon glow
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A1A22))
                            .border(2.5.dp, accentColorValue, CircleShape)
                            .clickable {
                                // Trigger selection of custom image
                                imagePickerLauncher.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (customAvatarPath.isNotEmpty() && File(customAvatarPath).exists()) {
                            // Render user custom image centered
                            androidx.compose.foundation.Image(
                                painter = coil.compose.rememberAsyncImagePainter(File(customAvatarPath)),
                                contentDescription = "User avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Text(avatarEmoji, fontSize = 54.sp)
                        }
                    }

                    // Floating Quick-Edit Badge overlay button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(accentColorValue)
                            .border(2.dp, Color.Black, CircleShape)
                            .clickable {
                                imagePickerLauncher.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Set Custom Avatar",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Inline quick Preset Emojis selector list
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Or choose a quick mascot emoji:",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val emojiSelection = listOf("🤠", "🤖", "🦄", "👽", "🧠", "🔥", "🐹")
                        emojiSelection.forEach { sel ->
                            val isSel = (avatarEmoji == sel) && customAvatarPath.isEmpty()
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(if (isSel) accentColorValue.copy(alpha = 0.2f) else Color.Transparent)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSel) accentColorValue else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        customAvatarPath = ""
                                        avatarEmoji = sel
                                        prefs
                                            .edit()
                                            .putString("avatar_emoji", sel)
                                            .putString("custom_avatar_path", "")
                                            .apply()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(sel, fontSize = 16.sp)
                            }
                        }
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.05f))

                // Gamer tag + Bio inputs
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Display Tag Handle") },
                        placeholder = { Text("@stitch_magician") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = accentColorValue)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = accentColorValue,
                            unfocusedBorderColor = Color(0xFF1E1E24),
                            focusedContainerColor = Color(0xFF0C0C0E),
                            unfocusedContainerColor = Color(0xFF0C0C0E)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input")
                    )

                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Bio / Sticker Philosophy") },
                        placeholder = { Text("Stickers make messages warmer...") },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = accentColorValue,
                            unfocusedBorderColor = Color(0xFF1E1E24),
                            focusedContainerColor = Color(0xFF0C0C0E),
                            unfocusedContainerColor = Color(0xFF0C0C0E)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bio_input")
                    )
                }

                // Explicit button to persist profile changes
                Button(
                    onClick = {
                        prefs.edit()
                            .putString("username", username)
                            .putString("bio", bio)
                            .apply()
                        showSavedNotification = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColorValue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("submit_button")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SAVE PROFILE DETAILS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section 2: Progress Metrics, Achievements & Badges (Interactive Level Progress Track)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF22222A), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131317)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏆 GAMIFIED PROGRESS TRACK",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )

                    // Active rank text badge overlay
                    val rankName = getRankTitle(currentStickersCount)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColorValue.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = rankName.uppercase(),
                            color = accentColorValue,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Circular Progress or Linear Progress showing level gauge
                // Let's count level steps. Formula: Level = (currentStickers / 3) + 1 capped at lvl 10
                val level = (currentStickersCount / 3) + 1
                val stickersTowardsNext = currentStickersCount % 3
                val levelProgress = stickersTowardsNext / 3.0f

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Level $level Sticker Summoner",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "$stickersTowardsNext/3 to Level ${level + 1}",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                    LinearProgressIndicator(
                        progress = { levelProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = accentColorValue,
                        trackColor = Color(0xFF22222A)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Stats rows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        weight = 1f,
                        icon = "🖼️",
                        metrics = "$currentStickersCount",
                        label = "Stickers Created"
                    )
                    StatCard(
                        weight = 1f,
                        icon = "❤️",
                        metrics = "$favoriteStickersCount",
                        label = "Favorites"
                    )
                    StatCard(
                        weight = 1f,
                        icon = "⌨️",
                        metrics = if (isKeyboardActive) "ON" else "OFF",
                        label = "Extension Status"
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "UNLOCKED BADGES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Gray,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                // List of unlockable credentials
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BadgeItem(
                        icon = "🥉",
                        title = "Initiate",
                        desc = "Profile Created",
                        isUnlocked = username.length > 3,
                        accentColor = accentColorValue,
                        modifier = Modifier.weight(1f)
                    )
                    BadgeItem(
                        icon = "🥈",
                        title = "Splitter",
                        desc = "1+ Stickers Created",
                        isUnlocked = currentStickersCount >= 1,
                        accentColor = accentColorValue,
                        modifier = Modifier.weight(1f)
                    )
                    BadgeItem(
                        icon = "🥇",
                        title = "Elite",
                        desc = "5+ Stickers Saved",
                        isUnlocked = currentStickersCount >= 5,
                        accentColor = accentColorValue,
                        modifier = Modifier.weight(1f)
                    )
                    BadgeItem(
                        icon = "👑",
                        title = "Keyboard Pro",
                        desc = "System keyboard ready",
                        isUnlocked = isKeyboardActive,
                        accentColor = accentColorValue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Section 2.5: Sticker Vault, Export & Share Platform
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF22222A), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131317)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📤 STICKER EXPORT & SHARE CENTRE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColorValue.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "OFFLINE VAULT",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColorValue
                        )
                    }
                }

                Text(
                    text = "Share customized transparent PNG stickers directly to WhatsApp, Telegram, Signal or save them safely into your device's photo gallery.",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    lineHeight = 14.sp
                )

                if (savedStickers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF18181F), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF22222A), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("No Stickers Created Yet 🧐", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Head to the 'Create' tab to split, erase backgrounds and summon custom high-quality stickers with magical caption templates!",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 12.sp
                            )
                        }
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(savedStickers.size) { index ->
                            val sticker = savedStickers[index]
                            Card(
                                modifier = Modifier
                                    .width(130.dp)
                                    .border(1.dp, Color(0xFF22222A), RoundedCornerShape(14.dp)),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B22))
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Image container
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(90.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF121216)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.foundation.Image(
                                            painter = coil.compose.rememberAsyncImagePainter(File(sticker.filePath)),
                                            contentDescription = sticker.caption,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                        )

                                        if (sticker.isFavorite) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0x992C1113))
                                                    .padding(3.dp)
                                            ) {
                                                Text("❤️", fontSize = 8.sp)
                                            }
                                        }

                                        if (sticker.category.isNotEmpty() && sticker.category != "uncategorized") {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .background(accentColorValue.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = sticker.category.uppercase(),
                                                    fontSize = 6.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    if (sticker.caption.isNotEmpty()) {
                                        Text(
                                            text = sticker.caption,
                                            color = Color.LightGray,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        Text(
                                            text = "Sticker #${index + 1}",
                                            color = Color.DarkGray,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    // Action buttons for single sticker
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Save directly to the phone's gallery option
                                        Button(
                                            onClick = {
                                                val success = saveStickerToGallery(context, sticker.filePath, sticker.caption)
                                                if (success) {
                                                    savedNotificationText = "💾 Saved to main photo album (/Pictures/StickMe)!"
                                                } else {
                                                    savedNotificationText = "❌ Failed to save sticker to photo album."
                                                }
                                                showSavedNotification = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(28.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Done,
                                                    contentDescription = "Save",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Text("SAVE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }

                                        // Share to messaging apps option
                                        Button(
                                            onClick = {
                                                shareSticker(context, sticker.filePath, sticker.caption)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = accentColorValue),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(28.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = "Share",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Text("SHARE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
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

        // Section 3: Advanced Input Customization Settings Drawer
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF22222A), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131317)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "⚙️ TYPING SYSTEM OPTIONS & STYLING",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )

                // Neon Accent Color Theme Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        value = "Custom Glowing Keyboard Accent Color:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val colorsList = listOf("Crimson", "Cyan", "Lime", "Gold")
                        colorsList.forEach { col ->
                            val colorVal = getAccentColor(col)
                            val isSel = themeAccent == col

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) colorVal.copy(alpha = 0.15f) else Color(0xFF1E1E24))
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isSel) colorVal else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        themeAccent = col
                                        prefs.edit().putString("theme_accent", col).apply()
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(colorVal)
                                    )
                                    Text(
                                        text = col.uppercase(),
                                        color = if (isSel) Color.White else Color.Gray,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.05f))

                // Vibration Haptics Selector Flow
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        value = "Haptic Vibration Strength feedback when tapping stickers:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val hapticOptions = listOf("None", "Light", "Strong")
                        hapticOptions.forEach { opt ->
                            val isSel = hapticMode == opt

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) accentColorValue.copy(alpha = 0.15f) else Color(0xFF1C1C22))
                                    .border(
                                        width = 1.dp,
                                        color = if (isSel) accentColorValue else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        hapticMode = opt
                                        prefs.edit().putString("haptic_mode", opt).apply()
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = opt.uppercase(),
                                    color = if (isSel) Color.White else Color.Gray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.05f))

                // Quality presets Selector Flow
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        value = "Sticker Compressor & Quality preset:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val qualityOptions = listOf("Standard", "HD PNG", "WebP")
                        qualityOptions.forEach { opt ->
                            val isSel = qualityPreset == opt

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) accentColorValue.copy(alpha = 0.15f) else Color(0xFF1C1C22))
                                    .border(
                                        width = 1.dp,
                                        color = if (isSel) accentColorValue else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        qualityPreset = opt
                                        prefs.edit().putString("quality_preset", opt).apply()
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = opt,
                                    color = if (isSel) Color.White else Color.Gray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.05f))

                // Auto Suggest Toggle row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Suggest stickers",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Analyze keyboard text on the fly to suggest matched cutouts",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = autoSuggest,
                        onCheckedChange = {
                            autoSuggest = it
                            prefs.edit().putBoolean("auto_suggest", it).apply()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentColorValue,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF1C1C22)
                        )
                    )
                }
            }
        }

        // Section 4: Store Legal, Privacy & Age Compliance Compass
        var isPrivacyExpanded by remember { mutableStateOf(false) }
        var isUgcExpanded by remember { mutableStateOf(false) }
        var isTermsExpanded by remember { mutableStateOf(false) }
        
        var isLegallyConsented by remember { mutableStateOf(prefs.getBoolean("legal_consent", false)) }
        var consentTimestamp by remember { mutableStateOf(prefs.getString("consent_timestamp", "") ?: "") }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF22222A), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131317)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🛡️ COMPLIANCE & PRIVACY COMPASS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Official Play Store developer declarations & agreements",
                            fontSize = 9.5.sp,
                            color = Color.Gray
                        )
                    }

                    // Compliance status badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isLegallyConsented) Color(0xFF1B4D20) else Color(0xFF2C1515))
                            .border(
                                1.dp, 
                                if (isLegallyConsented) Color(0xFF4CAF50) else Color(0xFFFF1F2D).copy(alpha = 0.5f), 
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isLegallyConsented) "COMPLIANT" else "PENDING ACTION",
                            color = if (isLegallyConsented) Color(0xFF81C784) else Color(0xFFFF8A80),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Interactive Expandable item 1: Data Safety & Keylogger protection
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0C0C0F))
                        .border(1.dp, Color(0xFF1E1E24), RoundedCornerShape(12.dp))
                        .clickable { isPrivacyExpanded = !isPrivacyExpanded }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Privacy",
                                tint = accentColorValue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Data Safety & Keyboard Warning",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Expand",
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(if (isPrivacyExpanded) 180f else 0f)
                        )
                    }

                    AnimatedVisibility(visible = isPrivacyExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Divider(color = Color.White.copy(alpha = 0.05f))
                            Text(
                                value = "🔒 Why does Android show a default warning when enabling StickMe?",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColorValue
                            )
                            Text(
                                text = "Android displays a standard, generic warning for ALL third-party keyboard extensions warning they 'might collect personal text'. Please be assured that StickMe is engineered with zero network-state tracking. Your typed keystrokes, personal messages, passwords, and custom sticker creations remain strictly isolated in the secure, sandboxed client-side cache of your local device. We initiate zero cloud uploads, ensuring 100% absolute privacy compliance.",
                                fontSize = 10.sp,
                                color = Color.LightGray,
                                lineHeight = 14.sp
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accentColorValue.copy(alpha = 0.05f))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Compliance Verified",
                                        tint = accentColorValue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Google Play Data Safety Rating: fully offline application.",
                                        fontSize = 9.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Interactive Expandable item 2: Age & UGC content guidelines
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0C0C0F))
                        .border(1.dp, Color(0xFF1E1E24), RoundedCornerShape(12.dp))
                        .clickable { isUgcExpanded = !isUgcExpanded }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "UGC Guidelines",
                                tint = accentColorValue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "UGC Moderation & Age Rating Policy",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Expand",
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(if (isUgcExpanded) 180f else 0f)
                        )
                    }

                    AnimatedVisibility(visible = isUgcExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Divider(color = Color.White.copy(alpha = 0.05f))
                            Text(
                                value = "🔞 Play Store Target Rating: PEGI 3 / Everyone",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColorValue
                            )
                            Text(
                                text = "Because StickMe features local Camera capture and Gallery photo-crop wizards, the final created sticker assets are classified as User Generated Content (UGC). To maintain continuous legal presence on the Google Play Store, you strictly agree as the designer and user that you will not craft stickers that deploy:\n\n" +
                                        "• 1. Extremely graphic violence, hate speech, or defamatory motifs.\n" +
                                        "• 2. Obscene, sexually explicit, or adult-themed illustrations.\n" +
                                        "• 3. Intellectual property patents unless you have direct design copyright.\n\n" +
                                        "Violation of these terms may result in local data clearance or application account termination.",
                                fontSize = 10.sp,
                                color = Color.LightGray,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                // Interactive Expandable item 3: Detailed Terms & Privacy document
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0C0C0F))
                        .border(1.dp, Color(0xFF1E1E24), RoundedCornerShape(12.dp))
                        .clickable { isTermsExpanded = !isTermsExpanded }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Legal Terms",
                                tint = accentColorValue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Legal Terms & Privacy Policy",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Expand",
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(if (isTermsExpanded) 180f else 0f)
                        )
                    }

                    AnimatedVisibility(visible = isTermsExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Divider(color = Color.White.copy(alpha = 0.05f))
                            Text(
                                value = "PRIVACY POLICY & AGREEMENT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Gray
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .background(Color(0xFF070709))
                                    .border(1.dp, Color(0xFF1E1E24), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = "StickMe Sticker Keyboard App complies with international legal frameworks including GDPR, CCPA, and Google Play Store Content Policies.\n\n" +
                                            "1. COLLECTION STATEMENT: StickMe does not request, log, scrape, or transmit personal details, contacts, phone parameters, location coordinates, or keystroke strings.\n\n" +
                                            "2. INFORMATION HANDLING: Photo files and custom cropped sticker graphics are stored using security-gated internal app directories and a heavily encrypted SQLite system (Room) situated on the local sandbox partition of your Android hardware.\n\n" +
                                            "3. THIRD PARTY SERVICES: No third-party network plugins, analytical scripts, tracking cookies, or advertising metrics are bundled. Keystroke vibrations are produced client-side using Android's native Vibrator services.\n\n" +
                                            "4. AMENDMENTS: These policies are continuously updated to preserve maximum compliance metrics for official store distributions.",
                                    fontSize = 9.5.sp,
                                    color = Color.Gray,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.05f))

                // Interactive Consent Checkbox Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isLegallyConsented) accentColorValue.copy(alpha = 0.04f) else Color.Transparent)
                        .border(
                            1.dp, 
                            if (isLegallyConsented) accentColorValue.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f), 
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            val targetState = !isLegallyConsented
                            isLegallyConsented = targetState
                            
                            val df = java.text.DateFormat.getDateTimeInstance()
                            val now = df.format(java.util.Date())
                            consentTimestamp = if (targetState) now else ""
                            
                            prefs.edit()
                                .putBoolean("legal_consent", targetState)
                                .putString("consent_timestamp", consentTimestamp)
                                .apply()
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isLegallyConsented,
                        onCheckedChange = { checked ->
                            isLegallyConsented = checked
                            val df = java.text.DateFormat.getDateTimeInstance()
                            val now = df.format(java.util.Date())
                            consentTimestamp = if (checked) now else ""
                            
                            prefs.edit()
                                .putBoolean("legal_consent", checked)
                                .putString("consent_timestamp", consentTimestamp)
                                .apply()
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = accentColorValue,
                            uncheckedColor = Color.Gray,
                            checkmarkColor = Color.Black
                        )
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Agree to play store compliance declarations",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isLegallyConsented && consentTimestamp.isNotEmpty()) 
                                "Declared active on: $consentTimestamp" 
                                else "Declares you understand the sandbox data privacy and custom UGC standards",
                            fontSize = 8.5.sp,
                            color = if (isLegallyConsented) accentColorValue else Color.Gray
                        )
                    }
                }
            }
        }

        // Section 5: Signature & Creator Branding (TOPSHELF-NZ Special)
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.stickme_logo_1781048421456),
                contentDescription = "Stick Me Logo Creator Signature",
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, Color(0xFFFF1F2D), RoundedCornerShape(16.dp))
            )

            Text(
                text = "EXPRESS ANYTHING.. STICK EVERYWHERE.",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "CREATED BY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Text(
                    text = "TOPSHELF-NZ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFF1F2D)
                )
            }
            Text(
                text = "Version 1.0.0 • Production Release ready",
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                color = Color.DarkGray
            )
        }
    }
}

// Custom text utility proxy due to string mapping helper override
@Composable
fun Text(
    value: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = value,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        modifier = modifier
    )
}

@Composable
fun RowScope.StatCard(
    weight: Float,
    icon: String,
    metrics: String,
    label: String
) {
    Box(
        modifier = Modifier
            .weight(weight)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0C0C0F))
            .border(1.dp, Color(0xFF1E1E24), RoundedCornerShape(12.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(icon, fontSize = 20.sp)
            Text(
                text = metrics,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = label,
                color = Color.Gray,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
fun BadgeItem(
    icon: String,
    title: String,
    desc: String,
    isUnlocked: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isUnlocked) Color(0xFF0F0F12) else Color(0xFF0A0A0B))
            .border(
                width = 1.dp,
                color = if (isUnlocked) accentColor.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.03f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isUnlocked) accentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isUnlocked) icon else "🔒",
                    fontSize = 14.sp
                )
            }
            Text(
                text = title,
                color = if (isUnlocked) Color.White else Color.DarkGray,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Utility mapper to translate theme string value to Color instances
fun getAccentColor(name: String): Color {
    return when (name) {
        "Cyan" -> Color(0xFF00E5FF)
        "Lime" -> Color(0xFF00E676)
        "Gold" -> Color(0xFFFFD600)
        else -> Color(0xFFFF1F2D) // Crimson default
    }
}

fun getRankTitle(stickersCount: Int): String {
    return when {
        stickersCount == 0 -> "Uninitiated"
        stickersCount < 2 -> "Beginner Carver"
        stickersCount < 5 -> "Cutout Maestro"
        stickersCount < 10 -> "Sticker Legend"
        else -> "Supreme Overlord"
    }
}

fun saveStickerToGallery(context: Context, filePath: String, caption: String): Boolean {
    val file = File(filePath)
    if (!file.exists()) return false

    val resolver = context.contentResolver
    val contentValues = android.content.ContentValues().apply {
        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "StickMe_" + System.currentTimeMillis() + ".png")
        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/StickMe")
            put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }

    val imageUri = resolver.insert(
        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    )

    return if (imageUri != null) {
        try {
            resolver.openOutputStream(imageUri).use { outputStream ->
                if (outputStream != null) {
                    file.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                resolver.delete(imageUri, null, null)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            false
        }
    } else {
        false
    }
}

fun shareSticker(context: Context, filePath: String, caption: String) {
    try {
        val file = File(filePath)
        if (!file.exists()) return
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            if (caption.isNotEmpty()) {
                putExtra(android.content.Intent.EXTRA_TEXT, caption)
            }
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share Sticker via"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
